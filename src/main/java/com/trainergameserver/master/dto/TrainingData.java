package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/** training.json의 트레이닝 하나. 필드명·순서는 클라가 준 JSON 그대로입니다. */
@JsonPropertyOrder({"id", "key", "title", "category", "row", "column", "valueDisplay", "requiredCategory",
        "requiredCategoryLevel", "requiredTrainingId", "requiredTrainingLevel", "levels"})
public record TrainingData(
        Integer id,
        String key,
        String title,
        Integer category,
        Integer row,
        Integer column,
        Integer valueDisplay,
        Integer requiredCategory,
        Integer requiredCategoryLevel,
        Integer requiredTrainingId,
        Integer requiredTrainingLevel,
        List<TrainingLevelData> levels) {

    /** 최대 레벨 (= 레벨 개수) */
    @JsonIgnore
    public int maxLevel() {
        return levels.size();
    }

    /** 레벨 L(1부터)의 구매 비용 */
    @JsonIgnore
    public int costOf(int level) {
        return levels.get(level - 1).cost();
    }
}
