package com.imtrainer.round.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MonsterBall(@NotNull @PositiveOrZero Long earned,
                          @NotNull @PositiveOrZero Long spent,
                          @NotNull @PositiveOrZero Long sold,
                          @NotNull @PositiveOrZero Long current) { }
