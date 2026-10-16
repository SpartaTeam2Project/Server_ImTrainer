package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/** 트레이닝 한 레벨. cost는 그 레벨을 살 때 드는 pocket_dollar */
@JsonPropertyOrder({"level", "value", "cost"})
public record TrainingLevelData(Integer level, BigDecimal value, Integer cost) {
}
