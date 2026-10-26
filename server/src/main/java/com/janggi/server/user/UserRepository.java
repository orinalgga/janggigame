package com.janggi.server.user;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameIgnoreCaseAndIdNot(String nickname, long id);

    boolean existsByIdAndBannedAtIsNotNull(long id);

    /** 대국 초대: 닉네임은 대소문자만 다른 것도 같은 닉네임으로 본다. */
    Optional<User> findByNicknameIgnoreCase(String nickname);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime from);

    /** 관리자 회원 검색: 번호가 같거나, 닉네임이나 연결된 메일에 q가 들어 있는 회원. */
    @Query("""
            select u from User u
            where :q = '' or u.id = :id
               or lower(u.nickname) like lower(concat('%', :q, '%'))
               or exists (select a from AuthIdentity a where a.user = u
                          and lower(a.email) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("q") String q, @Param("id") long id, Pageable pageable);
}
