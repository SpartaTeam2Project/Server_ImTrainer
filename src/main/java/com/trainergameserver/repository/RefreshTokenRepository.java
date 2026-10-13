package com.trainergameserver.repository;

import com.trainergameserver.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 만료되지 않은 토큰을 지우고 지운 건수를 돌려줍니다.
     * 같은 토큰으로 동시에 요청이 들어와도 DB 행 잠금 덕분에 한 요청만 1을 받고 나머지는 0을 받습니다.
     */
    @Modifying
    @Query("delete from RefreshToken t where t.tokenHash = :tokenHash and t.expiresAt > :now")
    int deleteValid(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken t where t.tokenHash = :tokenHash and t.userId = :userId")
    int deleteByTokenHashAndUserId(@Param("tokenHash") String tokenHash, @Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
