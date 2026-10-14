package com.trainergameserver.round.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 라운드 안에서만 쓰는 재화. current = earned - spent + sold 여야 한다 */
public record MonsterBall(
        @NotNull(message = "{validation.round.monsterBall.required}") @PositiveOrZero(message = "{validation.round.monsterBall.negative}") Long earned,
        @NotNull(message = "{validation.round.monsterBall.required}") @PositiveOrZero(message = "{validation.round.monsterBall.negative}") Long spent,
        @NotNull(message = "{validation.round.monsterBall.required}") @PositiveOrZero(message = "{validation.round.monsterBall.negative}") Long sold,
        @NotNull(message = "{validation.round.monsterBall.required}") @PositiveOrZero(message = "{validation.round.monsterBall.negative}") Long current
) {}
