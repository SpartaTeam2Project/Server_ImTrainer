package com.trainergameserver.player.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 계정별 플레이데이터. /player/load 응답이자, 쓰기 요청의 성공 응답과 409 응답의 playerData입니다. */
@Schema(description = "플레이데이터 (서버 기준 전체 값)")
@JsonPropertyOrder({"nickname", "intro", "selectedTrainerId", "trainings", "currencies", "ownedTrainerIds", "ownedMonsterIds"})
public record PlayerData(
        @Schema(description = "닉네임", example = "레드") String nickname,
        Intro intro,
        @Schema(description = "현재 고른 트레이너. 없으면 0", example = "1") int selectedTrainerId,
        @Schema(description = "산 트레이닝만 (trainingId 오름차순)") List<TrainingLevel> trainings,
        @Schema(description = "재화. 지금은 pocket_dollar 하나이며 항상 포함 (신규 계정은 amount 0)") List<Currency> currencies,
        @Schema(description = "보유 트레이너 id (오름차순)", example = "[1]") List<Integer> ownedTrainerIds,
        @Schema(description = "보유 포켓몬 id (오름차순, 스타터 포함)", example = "[1]") List<Integer> ownedMonsterIds) {

    @Schema(description = "인트로 선택. done이 false면 나머지는 0")
    @JsonPropertyOrder({"done", "gender", "generation", "starterMonsterId"})
    public record Intro(boolean done, int gender, int generation, int starterMonsterId) {
    }

    @JsonPropertyOrder({"trainingId", "level"})
    public record TrainingLevel(int trainingId, int level) {
    }

    @JsonPropertyOrder({"currencyId", "amount"})
    public record Currency(String currencyId, long amount) {
    }
}
