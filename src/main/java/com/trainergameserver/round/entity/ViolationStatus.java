package com.trainergameserver.round.entity;

/** 위반 기록 처리 상태 */
public enum ViolationStatus {
    /** 검토 전 */
    PENDING,
    /** 정상 플레이로 판단해 리더보드·포켓달러를 복구함 */
    RESTORED,
    /** 요청이 JSON으로 저장되기 전의 기록이라 자동 복구 불가 */
    LEGACY
}
