package com.trainergameserver.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 트레이닝 레벨 하나 (training_levels 테이블의 행) */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingLevel {

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal value;

    @Column(nullable = false)
    private Integer cost;

    public TrainingLevel(Integer level, BigDecimal value, Integer cost) {
        this.level = level;
        this.value = value;
        this.cost = cost;
    }

    /** 소수 자릿수(0.15 vs 0.150)와 관계없이 같은 레벨인지 */
    public boolean sameAs(TrainingLevel other) {
        return level.equals(other.level) && cost.equals(other.cost) && value.compareTo(other.value) == 0;
    }
}
