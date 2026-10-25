package com.janggi.server.user;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    /** 가입하자마자 쓰는 임시 닉네임의 앞부분. 뒤에 회원 번호가 붙는다. */
    static final String TEMP_PREFIX = "장기꾼";
    private static final int MIN_NICKNAME = 2;
    private static final int MAX_NICKNAME = 12;
    private static final Pattern ALLOWED = Pattern.compile("[가-힣a-zA-Z0-9_]+");
    private static final Pattern TEMP = Pattern.compile(TEMP_PREFIX + "\\d+");
    private static final Set<String> RESERVED = Set.of("운영자", "관리자", "admin", "administrator", "system", "ai");

    private final UserRepository users;
    private final AuthIdentityRepository identities;

    public UserService(UserRepository users, AuthIdentityRepository identities) {
        this.users = users;
        this.identities = identities;
    }

    /**
     * 처음 들어온 소셜 계정이면 회원을 새로 만들고, 아니면 연결된 회원을 돌려준다.
     * 소셜 계정의 이름은 남에게 보이지 않도록 쓰지 않고, 임시 닉네임(장기꾼+번호)을 준다.
     */
    @Transactional
    public User loginWith(String provider, String providerUid, String email) {
        return identities.findByProviderAndProviderUid(provider, providerUid)
                .map(AuthIdentity::getUser)
                .orElseGet(() -> {
                    // 번호는 저장해야 정해지므로, 겹치지 않는 이름으로 먼저 저장한 뒤 바꾼다.
                    User user = users.saveAndFlush(new User(UUID.randomUUID().toString(), OffsetDateTime.now()));
                    user.rename(TEMP_PREFIX + user.getId(), false);
                    identities.save(new AuthIdentity(user, provider, providerUid, email));
                    return user;
                });
    }

    /** 2~12자의 한글, 영문, 숫자, 밑줄. 다른 회원과 겹치면 안 된다(대소문자 무시). */
    @Transactional
    public User changeNickname(long userId, String requested) {
        String nickname = requested == null ? "" : requested.strip();
        int length = nickname.codePointCount(0, nickname.length());
        if (length < MIN_NICKNAME || length > MAX_NICKNAME) {
            throw new NicknameException("닉네임은 " + MIN_NICKNAME + "~" + MAX_NICKNAME + "자로 정해 주세요");
        }
        if (!ALLOWED.matcher(nickname).matches()) {
            throw new NicknameException("한글, 영문, 숫자, 밑줄(_)만 쓸 수 있어요");
        }
        if (TEMP.matcher(nickname).matches() || RESERVED.contains(nickname.toLowerCase(Locale.ROOT))) {
            throw new NicknameException("쓸 수 없는 닉네임이에요");
        }
        if (users.existsByNicknameIgnoreCaseAndIdNot(nickname, userId)) {
            throw new NicknameException("이미 누가 쓰고 있는 닉네임이에요");
        }
        User user = users.findById(userId).orElseThrow(() -> new NicknameException("회원을 찾을 수 없습니다"));
        user.rename(nickname, true);
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> find(long id) {
        return users.findById(id);
    }
}
