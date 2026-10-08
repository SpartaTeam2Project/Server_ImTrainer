package com.imtrainer.round.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record RoundEndRequest(@NotBlank String trainerId,
                              @NotNull RoundResult result,
                              @NotNull @PositiveOrZero Double gameTime,
                              @NotNull @Positive Integer level,
                              @NotNull @PositiveOrZero Long totalExp,
                              @NotNull @Valid MonsterBall monsterBall,
                              @NotNull @PositiveOrZero Integer killCount,
                              @NotNull @Size(min = 1, max = 3) List<@NotNull @Positive Integer> pokemonEntry,
                              @NotNull @Valid BossTime bossTime) {
}
