package com.janggi.server.invite;

import com.janggi.core.Formation;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameService;
import com.janggi.server.user.Grade;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

/**
 * 특정 상대와 두는 대국 초대. 닉네임으로 부르면 상대에게 바로 알림이 가고, 링크로 부르면 링크를 연 회원 누구나 받을 수 있다.
 * 받는 사람이 수락하면 진영을 무작위로 정해 온라인 대국(레이팅 반영)을 연다. 매칭 대기열처럼 서버 한 대 기준이라 메모리에 둔다.
 */
@Service
public class InviteService {

    /** 닉네임 초대는 상대가 바로 답할 수 있을 만큼만 기다린다. */
    static final Duration DIRECT_TTL = Duration.ofSeconds(60);
    /** 링크 초대는 메신저로 보내고 상대가 열 때까지 걸리니 넉넉히 둔다. */
    static final Duration LINK_TTL = Duration.ofMinutes(30);

    /** 초대 알림은 STOMP /user/queue/invite로 간다. */
    public static final String DESTINATION = "/queue/invite";

    public enum EventType { INVITED, ACCEPTED, DECLINED, CANCELLED }

    /** toId가 없으면 링크 초대다. expiresAt은 epoch 밀리초. */
    public record InviteDto(String id, long fromId, String fromNickname, String fromGrade, Long toId,
            String toNickname, long expiresAt) {
    }

    /** ACCEPTED면 gameId가 있다. */
    public record InviteEvent(EventType type, InviteDto invite, String gameId) {
    }

    public record AcceptResult(String gameId) {
    }

    private record Invite(String id, User from, Formation formation, User to, Instant expiresAt) {
        boolean isFor(long userId) {
            return to == null ? from.getId() != userId : to.getId() == userId;
        }
    }

    private final Map<String, Invite> invites = new HashMap<>();
    private final UserRepository users;
    private final GameService games;
    private final SimpMessagingTemplate messaging;
    private final SimpUserRegistry online;
    private final Supplier<Instant> now;
    private final Random random = new Random();

    @Autowired
    public InviteService(UserRepository users, GameService games, SimpMessagingTemplate messaging,
            SimpUserRegistry online) {
        this(users, games, messaging, online, Instant::now);
    }

    InviteService(UserRepository users, GameService games, SimpMessagingTemplate messaging, SimpUserRegistry online,
            Supplier<Instant> now) {
        this.users = users;
        this.games = games;
        this.messaging = messaging;
        this.online = online;
        this.now = now;
    }

    /** 닉네임으로 상대를 부른다. 상대가 지금 접속해 있어야 알림을 받을 수 있다. */
    public InviteDto invite(long fromId, String nickname, Formation formation) {
        String name = nickname == null ? "" : nickname.trim();
        if (name.isEmpty()) {
            throw new InviteException("부를 상대의 닉네임을 적어 주세요");
        }
        User to = users.findByNicknameIgnoreCase(name)
                .orElseThrow(() -> new InviteException("'" + name + "' 닉네임의 회원이 없어요"));
        if (to.getId() == fromId) {
            throw new InviteException("나 자신은 부를 수 없어요");
        }
        if (to.isBanned()) {
            throw new InviteException("이용이 정지된 회원이라 부를 수 없어요");
        }
        if (online.getUser(to.getId().toString()) == null) {
            throw new InviteException(to.getNickname() + " 님은 지금 접속해 있지 않아요");
        }
        Invite invite = create(fromId, formation, to, DIRECT_TTL);
        InviteDto dto = dto(invite);
        send(to.getId(), new InviteEvent(EventType.INVITED, dto, null));
        return dto;
    }

    /** 누구나 받을 수 있는 링크 초대를 만든다. */
    public InviteDto link(long fromId, Formation formation) {
        return dto(create(fromId, formation, null, LINK_TTL));
    }

    public InviteDto get(String id) {
        synchronized (this) {
            return dto(live(id));
        }
    }

    /** 받은 사람이 수락하면 바로 대국을 연다. 초대한 사람에게는 STOMP로 대국 번호를 알린다. */
    public AcceptResult accept(String id, long userId, Formation formation) {
        Invite invite;
        synchronized (this) {
            invite = live(id);
            if (!invite.isFor(userId)) {
                throw new InviteException(invite.from().getId() == userId
                        ? "내가 보낸 초대예요. 상대가 받기를 기다려 주세요"
                        : "다른 회원에게 온 초대예요");
            }
            invites.remove(id);
        }
        long fromId = invite.from().getId();
        Formation mine = formation == null ? Formation.MA_SANG_MA_SANG : formation;
        GameDto game = random.nextBoolean()
                ? games.createOnline(fromId, invite.formation(), userId, mine)
                : games.createOnline(userId, mine, fromId, invite.formation());
        send(fromId, new InviteEvent(EventType.ACCEPTED, dto(invite), game.id()));
        return new AcceptResult(game.id());
    }

    /** 받은 사람이 거절한다. 링크 초대는 거절해도 남아서 다른 사람이 받을 수 있다. */
    public void decline(String id, long userId) {
        Invite invite;
        synchronized (this) {
            invite = invites.get(id);
            if (invite == null || invite.to() == null || invite.to().getId() != userId) {
                return;
            }
            invites.remove(id);
        }
        send(invite.from().getId(), new InviteEvent(EventType.DECLINED, dto(invite), null));
    }

    /** 보낸 사람이 거둔다. 닉네임 초대였으면 받은 사람 화면의 초대 창도 닫힌다. */
    public void cancel(String id, long userId) {
        Invite invite;
        synchronized (this) {
            invite = invites.get(id);
            if (invite == null || invite.from().getId() != userId) {
                return;
            }
            invites.remove(id);
        }
        if (invite.to() != null) {
            send(invite.to().getId(), new InviteEvent(EventType.CANCELLED, dto(invite), null));
        }
    }

    /** 연결이 끊긴 회원이 보낸 닉네임 초대는 거둔다. 링크 초대는 창을 닫았다 와도 쓸 수 있게 남긴다. */
    public void disconnected(long userId) {
        List<Invite> gone = new ArrayList<>();
        synchronized (this) {
            invites.values().removeIf(i -> {
                boolean mine = i.from().getId() == userId && i.to() != null;
                if (mine) {
                    gone.add(i);
                }
                return mine;
            });
        }
        for (Invite i : gone) {
            send(i.to().getId(), new InviteEvent(EventType.CANCELLED, dto(i), null));
        }
    }

    private Invite create(long fromId, Formation formation, User to, Duration ttl) {
        User from = users.findById(fromId).orElseThrow(() -> new InviteException("회원을 찾을 수 없습니다"));
        Formation f = formation == null ? Formation.MA_SANG_MA_SANG : formation;
        Instant at = now.get();
        Invite invite = new Invite(UUID.randomUUID().toString(), from, f, to, at.plus(ttl));
        synchronized (this) {
            invites.values().removeIf(i -> !i.expiresAt().isAfter(at));
            invites.put(invite.id(), invite);
        }
        return invite;
    }

    /** 아직 받을 수 있는 초대. 시간이 지났으면 지우고 알린다. */
    private Invite live(String id) {
        Invite invite = invites.get(id);
        if (invite != null && !invite.expiresAt().isAfter(now.get())) {
            invites.remove(id);
            invite = null;
        }
        if (invite == null) {
            throw new InviteException("이미 끝났거나 시간이 지난 초대예요");
        }
        return invite;
    }

    private void send(long userId, InviteEvent event) {
        messaging.convertAndSendToUser(Long.toString(userId), DESTINATION, event);
    }

    private static InviteDto dto(Invite i) {
        User f = i.from();
        return new InviteDto(i.id(), f.getId(), f.getNickname(), Grade.name(f.getRating()),
                i.to() == null ? null : i.to().getId(), i.to() == null ? null : i.to().getNickname(),
                i.expiresAt().toEpochMilli());
    }
}
