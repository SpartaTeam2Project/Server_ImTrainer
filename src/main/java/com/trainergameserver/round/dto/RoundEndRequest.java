package com.trainergameserver.round.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 라운드 종료 요청. 트레이너는 액세스 토큰(@AuthUser)으로 식별하므로 본문에 계정 id를 받지 않는다. */
public record RoundEndRequest(
        @NotNull(message = "{validation.round.result.required}")
        RoundResult result,

        @NotNull(message = "{validation.round.gameTime.required}")
        @PositiveOrZero(message = "{validation.round.gameTime.negative}")
        Double gameTime,

        @NotNull(message = "{validation.round.level.required}")
        @Positive(message = "{validation.round.level.positive}")
        Integer level,

        @NotNull(message = "{validation.round.totalExp.required}")
        @PositiveOrZero(message = "{validation.round.totalExp.negative}")
        Long totalExp,

        @NotNull(message = "{validation.round.monsterBall.required}")
        @Valid
        MonsterBall monsterBall,

        @NotNull(message = "{validation.round.killCount.required}")
        @PositiveOrZero(message = "{validation.round.killCount.negative}")
        Integer killCount,

        @NotNull(message = "{validation.round.pokemonEntry.required}")
        @Size(min = 1, max = 3, message = "{validation.round.pokemonEntry.size}")
        List<@NotNull(message = "{validation.round.pokemonEntry.required}")
             @Positive(message = "{validation.round.pokemonEntry.positive}") Integer> pokemonEntry,

        @NotNull(message = "{validation.round.bossTime.required}")
        @Valid
        BossTime bossTime
) {}
