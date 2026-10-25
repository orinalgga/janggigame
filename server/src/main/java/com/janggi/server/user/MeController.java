package com.janggi.server.user;

import com.janggi.server.auth.CurrentUser;
import com.janggi.server.auth.OAuthProviders;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    /**
     * nicknameChosen이 false면 아직 임시 닉네임(장기꾼+번호)이라 웹이 닉네임을 정하라고 묻는다.
     * boardTheme, pieceTheme은 장착한 테마의 상점 상품 id.
     */
    public record MeDto(long id, String nickname, boolean nicknameChosen, int rating, String boardTheme,
            String pieceTheme) {

        static MeDto of(User u) {
            return new MeDto(u.getId(), u.getNickname(), u.isNicknameChosen(), u.getRating(), u.getBoardTheme(),
                    u.getPieceTheme());
        }
    }

    public record NicknameRequest(String nickname) {
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
                .map(u -> ResponseEntity.ok(MeDto.of(u)))
                .orElse(ResponseEntity.status(401).build());
    }

    @PutMapping("/api/me/nickname")
    public MeDto nickname(@RequestBody NicknameRequest request) {
        return MeDto.of(users.changeNickname(CurrentUser.id(), request.nickname()));
    }

    /** 같은 닉네임을 거의 동시에 정한 경우. 유일 인덱스가 막는다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> taken(DataIntegrityViolationException e) {
        return Map.of("error", "이미 누가 쓰고 있는 닉네임이에요");
    }

    @ExceptionHandler(NicknameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badNickname(NicknameException e) {
        return Map.of("error", e.getMessage());
    }

    @GetMapping("/api/auth/providers")
    public AuthProvidersDto providers() {
        return new AuthProvidersDto(providers);
    }
}
