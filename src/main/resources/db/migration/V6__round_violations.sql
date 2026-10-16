-- 라운드 결과 검증 실패 기록. 응답에는 공통 문구만 나가므로 상세 이유와 제출값을 여기에 남깁니다.
-- 계정 제재(밴) 기준, 이상 탐지에 사용합니다.
CREATE TABLE round_violations (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    code               VARCHAR(30)  NOT NULL,
    reason             VARCHAR(500) NOT NULL,
    unlock_applied     BIT(1)       NOT NULL,
    server_elapsed_sec DOUBLE       NOT NULL,
    request            TEXT         NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_round_violations_user (user_id, created_at)
);
