package com.trainergameserver.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/** monster.json 전체 구조. 파일을 읽을 때와 클라에 내려줄 때 같은 형태를 씁니다. */
@JsonPropertyOrder({"stageId", "monsters"})
public record MonsterTableData(String stageId, List<MonsterData> monsters) {
}
