package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** trainer.json의 트레이너 하나. gender는 0 남자, 1 여자 */
@JsonPropertyOrder({"id", "key", "name", "generation", "gender", "unlockedByIntro"})
public record TrainerData(Integer id, String key, String name, Integer generation, Integer gender,
                          Boolean unlockedByIntro) {
}
