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
        @Size(min = 1, max = 6, message = "{validation.round.pokemonEntry.size}")
        List<@NotNull(message = "{validation.round.pokemonEntry.required}")
        @Positive(message = "{validation.round.pokemonEntry.positive}") Integer> pokemonEntry,

        @NotNull(message = "{validation.round.bossTime.required}")
        @Valid
        BossTime bossTime,

        // 이번 라운드에서 해금한 트레이너·포켓몬. 없으면 빈 배열. CLEAR/DEAD 상관없이 검증 통과 시 보유 목록에 추가
        @NotNull(message = "{validation.round.unlockedTrainerIds.required}")
        @Size(max = 6, message = "{validation.round.unlockedTrainerIds.size}")
        List<@NotNull(message = "{validation.round.unlockedTrainerIds.required}")
        @Positive(message = "{validation.round.unlocked.positive}") Integer> unlockedTrainerIds,

        @NotNull(message = "{validation.round.unlockedMonsterIds.required}")
        @Size(max = 6, message = "{validation.round.unlockedMonsterIds.size}")
        List<@NotNull(message = "{validation.round.unlockedMonsterIds.required}")
        @Positive(message = "{validation.round.unlocked.positive}") Integer> unlockedMonsterIds
) {}
