package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** storage_monster.json의 보유 가능 포켓몬 하나. id는 도감번호 */
@JsonPropertyOrder({"id", "name", "generation", "legendary"})
public record StorageMonsterData(Integer id, String name, Integer generation, Boolean legendary) {
}
