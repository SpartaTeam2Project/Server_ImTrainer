-- 계정별 플레이데이터. user_id = users.id
-- players 행은 처음 /player/* 를 호출할 때 만들어집니다 (기존 계정도 같음).

CREATE TABLE players (
    user_id             BIGINT      NOT NULL,
    nickname            VARCHAR(20) NOT NULL,
    intro_done          BIT(1)      NOT NULL,
    intro_gender        INT         NOT NULL,
    intro_generation    INT         NOT NULL,
    starter_monster_id  INT         NOT NULL,
    selected_trainer_id INT         NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_players_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 산 트레이닝. level = 산 레벨 수 (0이면 행 없음)
CREATE TABLE player_trainings (
    user_id     BIGINT NOT NULL,
    training_id INT    NOT NULL,
    level       INT    NOT NULL,
    PRIMARY KEY (user_id, training_id),
    CONSTRAINT fk_player_trainings_player FOREIGN KEY (user_id) REFERENCES players (user_id)
);

-- 스테이지가 끝나도 남는 재화 (지금은 pocket_dollar 하나)
CREATE TABLE player_currencies (
    user_id     BIGINT      NOT NULL,
    currency_id VARCHAR(30) NOT NULL,
    amount      BIGINT      NOT NULL,
    PRIMARY KEY (user_id, currency_id),
    CONSTRAINT fk_player_currencies_player FOREIGN KEY (user_id) REFERENCES players (user_id)
);

CREATE TABLE player_trainers (
    user_id    BIGINT NOT NULL,
    trainer_id INT    NOT NULL,
    PRIMARY KEY (user_id, trainer_id),
    CONSTRAINT fk_player_trainers_player FOREIGN KEY (user_id) REFERENCES players (user_id)
);

CREATE TABLE player_monsters (
    user_id    BIGINT NOT NULL,
    monster_id INT    NOT NULL,
    PRIMARY KEY (user_id, monster_id),
    CONSTRAINT fk_player_monsters_player FOREIGN KEY (user_id) REFERENCES players (user_id)
);
