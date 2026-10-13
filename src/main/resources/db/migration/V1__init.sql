-- 초기 스키마. Flyway 도입 전 Hibernate(ddl-auto=update)가 만들던 테이블과 같은 구조입니다.
-- 이미 테이블이 있는 DB(dev RDS, 로컬)는 baseline-on-migrate로 이 파일을 건너뛰고 V2부터 적용됩니다.

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(20)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token)
);

CREATE TABLE monsters (
    id              INT            NOT NULL,
    stage_id        VARCHAR(50)    NOT NULL,
    name            VARCHAR(50)    NOT NULL,
    max_health      INT            NOT NULL,
    contact_damage  INT            NOT NULL,
    move_speed      DECIMAL(10, 3) NOT NULL,
    attack_kind     VARCHAR(30)    NOT NULL,
    attack_range    DECIMAL(10, 3) NOT NULL,
    attack_interval DECIMAL(10, 3) NOT NULL,
    projectile_speed DECIMAL(10, 3) NOT NULL,
    hit_radius      DECIMAL(10, 3) NOT NULL,
    attack_distance DECIMAL(10, 3) NOT NULL,
    PRIMARY KEY (id)
);
