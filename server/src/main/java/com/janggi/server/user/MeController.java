package com.janggi.server.user;

import com.janggi.server.auth.CurrentUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import com.janggi.server.auth.OAuthProviders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    /** boardTheme, pieceTheme은 장착한 테마의 상점 상품 id. */
    public record MeDto(long id, String nickname, int rating, String boardTheme, String pieceTheme) {
    }

    /** 웹이 로그인 버튼을 보여 줄 공급자 목록. */
    public record AuthProvidersDto(List<String> providers) {
    }

    private final UserService users;
    private final List<String> providers;

    public MeController(UserService users, OAuthProviders providers) {
        this.users = users;
        this.providers = providers.names();
    }

    @GetMapping("/api/me")
    public ResponseEntity<MeDto> me() {
        Long id = CurrentUser.id();
        if (id == null) {
            return ResponseEntity.status(401).build();
        }
        return users.find(id)
                .map(u -> ResponseEntity.ok(new MeDto(u.getId(), u.getNickname(), u.getRating(),
                        u.getBoardTheme(), u.getPieceTheme())))
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/api/auth/providers")
    public AuthProvidersDto providers() {
        return new AuthProvidersDto(providers);
    }
}
