-- 마스터데이터 4개 추가 (player_base, training, storage_monster, trainer).
-- 값은 서버 시작 시 resources/masterdata/*.json에서 채워집니다. 여기서는 테이블만 만듭니다.
-- key, row, column은 MySQL 예약어라 컬럼명을 바꿨습니다 (JSON 필드명은 그대로).

-- 플레이어 기본 스탯. 행은 id=1 하나뿐
CREATE TABLE player_base (
    id                          INT            NOT NULL,
    move_speed                  DECIMAL(10, 3) NOT NULL,
    damage_multiplier           DECIMAL(10, 3) NOT NULL,
    magnet_radius               DECIMAL(10, 3) NOT NULL,
    xp_multiplier               DECIMAL(10, 3) NOT NULL,
    hp_regen                    DECIMAL(10, 3) NOT NULL,
    cooldown_multiplier         DECIMAL(10, 3) NOT NULL,
    projectile_speed_multiplier DECIMAL(10, 3) NOT NULL,
    projectile_multiplier       DECIMAL(10, 3) NOT NULL,
    size_multiplier             DECIMAL(10, 3) NOT NULL,
    duration_multiplier         DECIMAL(10, 3) NOT NULL,
    invincible_seconds          DECIMAL(10, 3) NOT NULL,
    max_health                  DECIMAL(10, 3) NOT NULL,
    revive_health_ratio         DECIMAL(10, 3) NOT NULL,
    revive_invincible_seconds   DECIMAL(10, 3) NOT NULL,
    base_required_xp            DECIMAL(10, 3) NOT NULL,
    required_growth             DECIMAL(10, 3) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE trainings (
    id                      INT         NOT NULL,
    training_key            VARCHAR(50) NOT NULL,
    title                   VARCHAR(50) NOT NULL,
    category                INT         NOT NULL,
    row_pos                 INT         NOT NULL,
    column_pos              INT         NOT NULL,
    value_display           INT         NOT NULL,
    required_category       INT         NOT NULL,
    required_category_level INT         NOT NULL,
    required_training_id    INT         NOT NULL,
    required_training_level INT         NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE training_levels (
    training_id INT            NOT NULL,
    level       INT            NOT NULL,
    value       DECIMAL(10, 3) NOT NULL,
    cost        INT            NOT NULL,
    PRIMARY KEY (training_id, level),
    CONSTRAINT fk_training_levels_training FOREIGN KEY (training_id) REFERENCES trainings (id)
);

CREATE TABLE storage_monsters (
    id         INT         NOT NULL,
    name       VARCHAR(50) NOT NULL,
    generation INT         NOT NULL,
    legendary  BIT(1)      NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE trainers (
    id                INT          NOT NULL,
    trainer_key       VARCHAR(100) NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    generation        INT          NOT NULL,
    gender            INT          NOT NULL,
    unlocked_by_intro BIT(1)       NOT NULL,
    PRIMARY KEY (id)
);
