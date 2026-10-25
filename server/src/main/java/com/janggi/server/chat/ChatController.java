package com.janggi.server.chat;

import com.janggi.server.auth.CurrentUser;
import com.janggi.server.game.GameDto;
import com.janggi.server.game.GameNotFoundException;
import com.janggi.server.game.GameService;
import com.janggi.server.game.NotYourGameException;
import com.janggi.server.user.User;
import com.janggi.server.user.UserService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{id}/chat")
public class ChatController {

    private final ChatService chat;
    private final GameService games;
    private final UserService users;

    public ChatController(ChatService chat, GameService games, UserService users) {
        this.chat = chat;
        this.games = games;
        this.users = users;
    }

    @GetMapping
    public List<ChatMessage> history(@PathVariable String id) {
        games.get(id);
        return chat.history(id);
    }

    /** 이번에 새로 생긴 말을 돌려준다. AI 대국이면 내 말과 AI의 답 두 줄이다. */
    @PostMapping
    public List<ChatMessage> post(@PathVariable String id, @RequestBody ChatRequest request) {
        GameDto game = games.get(id);
        Long userId = CurrentUser.id();
        String nickname = userId == null ? null : users.find(userId).map(User::getNickname).orElse(null);
        return chat.post(game, userId, nickname, request.text());
    }

    public record ChatRequest(String text) {
    }

    @ExceptionHandler(ChatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badChat(ChatException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(NotYourGameException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> notYours(NotYourGameException e) {
        return Map.of("error", "이 대국의 참가자만 채팅할 수 있습니다");
    }

    @ExceptionHandler(GameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(GameNotFoundException e) {
        return Map.of("error", e.getMessage());
    }
}
