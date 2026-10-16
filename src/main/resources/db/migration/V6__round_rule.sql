-- 라운드 결과 검증 기준값 (round_rule.json → round_rule 테이블, 행은 id=1 하나뿐)
-- 값은 서버 시작 시 resources/masterdata/round_rule.json에서 채워집니다.
CREATE TABLE round_rule (
    id                    INT            NOT NULL,
    stage_duration_sec    DECIMAL(10, 3) NOT NULL,
    time_tolerance_sec    DECIMAL(10, 3) NOT NULL,
    max_kill_per_sec      DECIMAL(10, 3) NOT NULL,
    max_exp_per_kill      DECIMAL(10, 3) NOT NULL,
    max_ball_per_kill     DECIMAL(10, 3) NOT NULL,
    max_exp_multiplier    DECIMAL(10, 3) NOT NULL,
    max_ball_multiplier   DECIMAL(10, 3) NOT NULL,
    rival_spawn_sec       DECIMAL(10, 3) NOT NULL,
    rival_min_kill_sec    DECIMAL(10, 3) NOT NULL,
    rival_exp             DECIMAL(10, 3) NOT NULL,
    rival_ball            DECIMAL(10, 3) NOT NULL,
    champion_spawn_sec    DECIMAL(10, 3) NOT NULL,
    champion_min_kill_sec DECIMAL(10, 3) NOT NULL,
    champion_exp          DECIMAL(10, 3) NOT NULL,
    champion_ball         DECIMAL(10, 3) NOT NULL,
    PRIMARY KEY (id)
);
