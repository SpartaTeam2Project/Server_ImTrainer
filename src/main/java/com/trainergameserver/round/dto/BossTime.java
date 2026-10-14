package com.trainergameserver.round.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** 보스 출현/처치 시간 (초, gameTime 기준). 도달하지 못했거나 처치하지 못한 경우 null */
public record BossTime(
        @PositiveOrZero(message = "{validation.round.bossTime.negative}") Double rivalSpawn,
        @PositiveOrZero(message = "{validation.round.bossTime.negative}") Double rivalKill,
        @PositiveOrZero(message = "{validation.round.bossTime.negative}") Double championSpawn,
        @PositiveOrZero(message = "{validation.round.bossTime.negative}") Double championKill
) {}
