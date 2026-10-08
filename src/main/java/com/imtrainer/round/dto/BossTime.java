package com.imtrainer.round.dto;
import jakarta.validation.constraints.PositiveOrZero;

public record BossTime(
        @PositiveOrZero Double rivalSpawn,
        @PositiveOrZero Double rivalKill,
        @PositiveOrZero Double championSpawn,
        @PositiveOrZero Double championKill
) { }
