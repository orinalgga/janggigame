package com.janggi.server.invite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.janggi.core.Formation;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameService;
import com.janggi.server.user.User;
import com.janggi.server.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

class InviteServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final GameService games = mock(GameService.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final SimpUserRegistry online = mock(SimpUserRegistry.class);
    private final AtomicReference<Instant> clock = new AtomicReference<>(Instant.parse("2026-10-26T00:00:00Z"));
    private final InviteService invites = new InviteService(users, games, messaging, online, clock::get);

    private User alice;
    private User bob;

    private User user(long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getNickname()).thenReturn(nickname);
        when(u.getRating()).thenReturn(1200);
        when(users.findById(id)).thenReturn(Optional.of(u));
        when(users.findByNicknameIgnoreCase(nickname)).thenReturn(Optional.of(u));
        return u;
    }

    @BeforeEach
    void setUp() {
        alice = user(1, "앨리스");
        bob = user(2, "밥");
        when(online.getUser("2")).thenReturn(mock(SimpUser.class));
        GameDto game = mock(GameDto.class);
        when(game.id()).thenReturn("g1");
        when(games.createOnline(anyLong(), any(), anyLong(), any())).thenReturn(game);
    }

    @Test
    void invitedPlayerGetsNoticeAndAcceptStartsGame() {
        InviteService.InviteDto sent = invites.invite(1, " 밥 ", Formation.MA_SANG_MA_SANG);
        assertEquals(2L, sent.toId());
        verify(messaging).convertAndSendToUser(eq("2"), eq(InviteService.DESTINATION), any(InviteService.InviteEvent.class));

        assertEquals("g1", invites.accept(sent.id(), 2, Formation.SANG_MA_SANG_MA).gameId());
        ArgumentCaptor<InviteService.InviteEvent> event = ArgumentCaptor.forClass(InviteService.InviteEvent.class);
        verify(messaging).convertAndSendToUser(eq("1"), eq(InviteService.DESTINATION), event.capture());
        assertEquals(InviteService.EventType.ACCEPTED, event.getValue().type());
        assertEquals("g1", event.getValue().gameId());
        // 한 번 받은 초대는 끝난다.
        assertThrows(InviteException.class, () -> invites.accept(sent.id(), 2, null));
    }

    @Test
    void cannotInviteSelfOrOfflineOrUnknown() {
        assertThrows(InviteException.class, () -> invites.invite(1, "앨리스", null));
        when(online.getUser("2")).thenReturn(null);
        assertThrows(InviteException.class, () -> invites.invite(1, "밥", null));
        assertThrows(InviteException.class, () -> invites.invite(1, "없는사람", null));
    }

    @Test
    void onlyInviteeCanAccept() {
        user(3, "캐럴");
        InviteService.InviteDto sent = invites.invite(1, "밥", null);
        assertThrows(InviteException.class, () -> invites.accept(sent.id(), 3, null));
        assertThrows(InviteException.class, () -> invites.accept(sent.id(), 1, null));
    }

    @Test
    void expiredInviteCannotBeAccepted() {
        InviteService.InviteDto sent = invites.invite(1, "밥", null);
        clock.set(clock.get().plus(InviteService.DIRECT_TTL));
        assertThrows(InviteException.class, () -> invites.accept(sent.id(), 2, null));
        verify(games, never()).createOnline(anyLong(), any(), anyLong(), any());
    }

    @Test
    void linkInviteCanBeTakenByAnyoneButTheHost() {
        InviteService.InviteDto link = invites.link(1, null);
        assertNull(link.toId());
        assertThrows(InviteException.class, () -> invites.accept(link.id(), 1, null));
        // 링크 초대는 거절해도 남는다.
        invites.decline(link.id(), 2);
        assertEquals("g1", invites.accept(link.id(), 2, null).gameId());
    }

    @Test
    void declineAndCancelNotifyTheOtherSide() {
        InviteService.InviteDto first = invites.invite(1, "밥", null);
        invites.decline(first.id(), 2);
        assertThrows(InviteException.class, () -> invites.get(first.id()));

        InviteService.InviteDto second = invites.invite(1, "밥", null);
        invites.cancel(second.id(), 1);
        assertThrows(InviteException.class, () -> invites.get(second.id()));

        ArgumentCaptor<InviteService.InviteEvent> toAlice = ArgumentCaptor.forClass(InviteService.InviteEvent.class);
        verify(messaging).convertAndSendToUser(eq("1"), eq(InviteService.DESTINATION), toAlice.capture());
        assertEquals(InviteService.EventType.DECLINED, toAlice.getValue().type());
    }
}
