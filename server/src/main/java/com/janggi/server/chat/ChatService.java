package com.janggi.server.chat;

import com.janggi.core.Engine;
import com.janggi.core.GameResult;
import com.janggi.core.Side;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.NotYourGameException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 대국 채팅. 온라인 대국은 두 사람의 말을 /topic/games/{id}/chat으로 알리고,
 * AI 대국은 사람이 말을 걸면 AI가 답하고 수를 둘 때 가끔 한마디 한다.
 * 채팅은 메모리에만 두어 서버가 다시 켜지면 사라진다.
 */
@Service
public class ChatService {

    public static final int MAX_LENGTH = 200;
    private static final int KEEP = 100;

    private final Map<String, Log> logs = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messaging;
    private final ClaudeChat claude;
    private final int maxClaudeReplies;
    /** 한 사람이 이 간격보다 빨리 보내면 거절한다. */
    private final long minIntervalMs;
    private final Random random = new Random();

    public ChatService(SimpMessagingTemplate messaging, ClaudeChat claude,
            @Value("${janggi.ai-chat.max-replies-per-game:40}") int maxClaudeReplies,
            @Value("${janggi.chat.min-interval-ms:500}") long minIntervalMs) {
        this.messaging = messaging;
        this.claude = claude;
        this.maxClaudeReplies = maxClaudeReplies;
        this.minIntervalMs = minIntervalMs;
    }

    public List<ChatMessage> history(String gameId) {
        Log log = logs.get(gameId);
        return log == null ? List.of() : log.all();
    }

    /**
     * 온라인 대국이면 참가자의 말을 두 사람에게 알리고, AI 대국이면 사람의 말에 AI가 답한다.
     * 이번에 새로 생긴 말을 돌려준다.
     */
    public List<ChatMessage> post(GameDto game, Long userId, String nickname, String text) {
        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            throw new ChatException("보낼 말을 입력하세요");
        }
        if (clean.codePointCount(0, clean.length()) > MAX_LENGTH) {
            throw new ChatException("채팅은 " + MAX_LENGTH + "자까지 보낼 수 있습니다");
        }
        if (game.online()) {
            return List.of(postOnline(game, userId, clean));
        }
        if (game.aiSide() == null) {
            throw new ChatException("같은 화면 대국에는 채팅이 없습니다");
        }
        return postToAi(game, nickname, clean);
    }

    private ChatMessage postOnline(GameDto game, Long userId, String text) {
        Side side = userId == null ? null
                : game.cho() != null && userId.equals(game.cho().id()) ? Side.CHO
                : game.han() != null && userId.equals(game.han().id()) ? Side.HAN
                : null;
        if (side == null) {
            throw new NotYourGameException();
        }
        GameDto.PlayerDto me = side == Side.CHO ? game.cho() : game.han();
        ChatMessage m = log(game.id()).add(side, me.nickname(), false, text, true);
        messaging.convertAndSend("/topic/games/" + game.id() + "/chat", m);
        return m;
    }

    private List<ChatMessage> postToAi(GameDto game, String nickname, String text) {
        Side ai = game.aiSide();
        Persona persona = Persona.of(game.aiLevel());
        Log log = log(game.id());
        ChatMessage mine = log.add(opposite(ai), nickname == null ? "나" : nickname, false, text, true);
        String answer = null;
        if (claude.enabled() && log.claimClaudeReply(maxClaudeReplies)) {
            answer = claude.reply(systemPrompt(game, persona), turns(log.all())).orElse(null);
        }
        if (answer == null) {
            answer = CannedReplies.answer(persona, text, random);
        }
        ChatMessage reply = log.add(ai, persona.displayName(), true, truncate(answer), false);
        return List.of(mine, reply);
    }

    /** AI 대국을 시작할 때 AI가 먼저 인사한다. */
    public void greet(String gameId, Side ai, Engine.Level level) {
        Persona persona = Persona.of(level);
        log(gameId).add(ai, persona.displayName(), true, persona.greeting(), false);
    }

    /**
     * AI 대국에서 사람이 둔 수와 AI가 이어 둔 수를 보고 AI가 한마디 할지 정한다. 할 말이 없으면 null.
     */
    public ChatMessage commentOnMove(GameDto before, GameDto after) {
        Side ai = after.aiSide();
        if (ai == null || after.online()) {
            return null;
        }
        Persona persona = Persona.of(after.aiLevel());
        String line = MoveComments.line(persona, ai, before, after, random);
        return line == null ? null : log(after.id()).add(ai, persona.displayName(), true, line, false);
    }

    private Log log(String gameId) {
        return logs.computeIfAbsent(gameId, k -> new Log(minIntervalMs));
    }

    static String systemPrompt(GameDto g, Persona persona) {
        Side ai = g.aiSide();
        String turn = g.result() != GameResult.ONGOING ? "대국이 끝났다(" + result(g, ai) + ")"
                : g.sideToMove() == ai ? "네(AI) 차례" : "사용자 차례";
        return """
                너는 장기 앱의 AI 대국 상대 "%s"(이)다. 성격: %s
                지금 사용자와 장기를 두면서 채팅으로 이야기하고 있다. 사용자는 %s, 너는 %s 진영이다.
                대국 상황: %s%s. 점수 초 %.1f, 한 %.1f.
                남은 알: 초 %s / 한 %s

                지킬 것:
                - 한국어 존댓말로 한두 문장, 80자 안쪽으로 짧게 답한다.
                - 실제 수는 엔진이 따로 두므로 다음 수를 약속하거나 구체적인 수를 가르쳐 주지 않는다.
                - 장기와 이 대국 이야기를 중심으로, 성격에 맞게 친근하게 대화한다.
                - 마크다운이나 목록 없이 채팅하듯 쓴다.
                """.formatted(persona.displayName(), persona.character(), sideName(opposite(ai)), sideName(ai),
                turn, g.check() && g.result() == GameResult.ONGOING ? ", 장군이 걸려 있다" : "",
                g.choScore(), g.hanScore(), material(g, Side.CHO), material(g, Side.HAN));
    }

    /** 오래된 것부터, 사람의 말로 시작하고 같은 쪽 말은 합친다. */
    static List<ClaudeChat.Turn> turns(List<ChatMessage> messages) {
        List<ClaudeChat.Turn> turns = new ArrayList<>();
        for (ChatMessage m : messages.subList(Math.max(0, messages.size() - 16), messages.size())) {
            if (turns.isEmpty() && m.ai()) {
                continue;
            }
            int last = turns.size() - 1;
            if (last >= 0 && turns.get(last).ai() == m.ai()) {
                turns.set(last, new ClaudeChat.Turn(m.ai(), turns.get(last).text() + "\n" + m.text()));
            } else {
                turns.add(new ClaudeChat.Turn(m.ai(), m.text()));
            }
        }
        return turns;
    }

    private static String material(GameDto g, Side side) {
        Map<String, Integer> count = new HashMap<>();
        for (GameDto.PieceDto p : g.pieces()) {
            if (p.side().equals(side.name())) {
                count.merge(MoveComments.korean(p.type(), side), 1, Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String name : List.of("차", "포", "마", "상", "사", "졸", "병")) {
            Integer n = count.get(name);
            if (n != null) {
                sb.append(sb.isEmpty() ? "" : " ").append(name).append(n);
            }
        }
        return sb.toString();
    }

    private static String result(GameDto g, Side ai) {
        if (g.result() == GameResult.DRAW) {
            return "무승부";
        }
        Side winner = g.result() == GameResult.CHO_WIN ? Side.CHO : Side.HAN;
        return winner == ai ? "네가 이겼다" : "사용자가 이겼다";
    }

    static Side opposite(Side side) {
        return side == Side.CHO ? Side.HAN : Side.CHO;
    }

    private static String sideName(Side side) {
        return side == Side.CHO ? "초(초록)" : "한(빨강)";
    }

    private static String truncate(String text) {
        String t = text.strip();
        return t.codePointCount(0, t.length()) <= MAX_LENGTH ? t
                : t.substring(0, t.offsetByCodePoints(0, MAX_LENGTH - 1)) + "…";
    }

    /** 대국 하나의 채팅. 최근 KEEP개만 남긴다. */
    private static final class Log {

        private final long minIntervalMs;
        private final Deque<ChatMessage> messages = new ArrayDeque<>();
        private final Map<Side, Long> lastHumanAt = new EnumMap<>(Side.class);
        private long nextId = 1;
        private int claudeReplies;

        Log(long minIntervalMs) {
            this.minIntervalMs = minIntervalMs;
        }

        synchronized ChatMessage add(Side side, String nickname, boolean ai, String text, boolean human) {
            long now = System.currentTimeMillis();
            if (human) {
                Long last = lastHumanAt.get(side);
                if (last != null && now - last < minIntervalMs) {
                    throw new ChatException("조금 천천히 보내 주세요");
                }
                lastHumanAt.put(side, now);
            }
            ChatMessage m = new ChatMessage(nextId++, side.name(), Objects.requireNonNullElse(nickname, ""), ai, text,
                    now);
            messages.addLast(m);
            while (messages.size() > KEEP) {
                messages.removeFirst();
            }
            return m;
        }

        synchronized List<ChatMessage> all() {
            return List.copyOf(messages);
        }

        synchronized boolean claimClaudeReply(int max) {
            if (claudeReplies >= max) {
                return false;
            }
            claudeReplies++;
            return true;
        }
    }
}
