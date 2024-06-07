package io.cloudtype.Demo.mypage.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntity, Long> {
    boolean existsByAccessToken(String accessToken);
    boolean existsByRefreshToken(String refreshToken);
}

