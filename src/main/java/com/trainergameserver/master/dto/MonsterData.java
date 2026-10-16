package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/** monster.json의 몬스터 한 마리. 필드명·순서는 클라가 준 JSON 그대로입니다. */
@JsonPropertyOrder({"id", "name", "maxHealth", "contactDamage", "moveSpeed", "attackKind",
        "attackRange", "attackInterval", "ProjectileSpeed", "HitRadius", "attackDistance"})
public record MonsterData(
        Integer id,
        String name,
        Integer maxHealth,
        Integer contactDamage,
        BigDecimal moveSpeed,
        String attackKind,
        BigDecimal attackRange,
        BigDecimal attackInterval,
        @JsonProperty("ProjectileSpeed") BigDecimal projectileSpeed,
        @JsonProperty("HitRadius") BigDecimal hitRadius,
        BigDecimal attackDistance) {
}
