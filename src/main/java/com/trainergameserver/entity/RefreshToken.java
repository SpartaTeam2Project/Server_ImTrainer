package com.trainergameserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 토큰 원문이 아닌 SHA-256 해시(hex 64자). DB가 유출돼도 토큰을 바로 쓸 수 없게 합니다. 컬럼명은 기존 스키마 호환을 위해 token 유지. */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String tokenHash;

    /** 로그인 한 번에서 refresh로 이어지는 토큰 묶음. 재사용이 감지되면 같은 family를 모두 폐기합니다. */
    @Column(nullable = false, length = 36)
    private String familyId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** refresh에 사용한 시각. 사용한 토큰은 만료될 때까지 남겨 두고 재사용을 감지하는 데 씁니다. */
    private LocalDateTime usedAt;

    public RefreshToken(Long userId, String tokenHash, String familyId, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }
}
