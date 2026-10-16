package com.trainergameserver.round.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    /** 클라이언트가 보낸 요청 전체 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String request;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static RoundViolation of(Long userId, String code, String reason, boolean unlockApplied,
                                    double serverElapsedSec, String request, LocalDateTime now) {
        RoundViolation v = new RoundViolation();
        v.userId = userId;
        v.code = code;
        v.reason = reason.length() > REASON_MAX ? reason.substring(0, REASON_MAX) : reason;
        v.unlockApplied = unlockApplied;
        v.serverElapsedSec = serverElapsedSec;
        v.request = request;
        v.createdAt = now;
        return v;
    }
}
