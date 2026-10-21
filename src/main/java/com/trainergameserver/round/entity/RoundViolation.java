package com.trainergameserver.round.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 라운드 결과 검증 실패 기록 (V7__round_violations.sql).
 * 응답에는 공통 문구만 나가므로, 어떤 기준에 왜 걸렸는지는 여기에 남깁니다.
 * 나중에 계정 제재(밴) 기준을 정하거나 이상 탐지를 할 때 이 테이블을 봅니다.
 * 정상 플레이로 판단되면 RoundRestoreService 로 리더보드·포켓달러를 복구합니다 (복구 컬럼은 V8).
 */
@Entity
@Table(name = "round_violations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoundViolation {

    private static final int REASON_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** ErrorCode 이름 (INVALID_TIME, INVALID_UNLOCK ...) */
    @Column(nullable = false, length = 30)
    private String code;

    /** 로그용 상세 이유 (예: "라이벌 처치 소요 4.2초 < 최소 15초") */
    @Column(nullable = false, length = REASON_MAX)
    private String reason;

    /** 해금이 반영됐는지 (결과 검증 실패면 true, 해금 검증 실패면 false) */
    @Column(nullable = false)
    private Boolean unlockApplied;

    @Column(nullable = false, columnDefinition = "DOUBLE")
    private Double serverElapsedSec;

    /** 클라이언트가 보낸 요청 전체 (JSON, 복구할 때 다시 읽음) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String request;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ddl-auto=validate: MySQL 에서 Hibernate 가 enum 컬럼을 기대할 수 있어 VARCHAR 로 명시
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private ViolationStatus status;

    /** 검증을 통과했다면 받았을 포켓달러 (위반 당시 ShardBonus 기준) */
    private Long expectedReward;

    private LocalDateTime restoredAt;

    public static RoundViolation of(Long userId, String code, String reason, boolean unlockApplied,
                                    double serverElapsedSec, String request, long expectedReward,
                                    LocalDateTime now) {
        RoundViolation v = new RoundViolation();
        v.userId = userId;
        v.code = code;
        v.reason = reason.length() > REASON_MAX ? reason.substring(0, REASON_MAX) : reason;
        v.unlockApplied = unlockApplied;
        v.serverElapsedSec = serverElapsedSec;
        v.request = request;
        v.createdAt = now;
        v.status = ViolationStatus.PENDING;
        v.expectedReward = expectedReward;
        return v;
    }

    /** 복구 완료로 표시. 두 번 복구되지 않도록 PENDING 일 때만 가능 */
    public void markRestored(LocalDateTime now) {
        if (status != ViolationStatus.PENDING) {
            throw new IllegalStateException("복구할 수 없는 상태: " + status);
        }
        this.status = ViolationStatus.RESTORED;
        this.restoredAt = now;
    }
}
