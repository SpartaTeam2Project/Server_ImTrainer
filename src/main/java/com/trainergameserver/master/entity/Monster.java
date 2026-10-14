package com.trainergameserver.master.entity;

import com.trainergameserver.master.dto.MonsterData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 몬스터 마스터데이터. 값은 resources/masterdata/monster.json에서 서버 시작 시 반영됩니다. */
@Entity
@Table(name = "monsters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Monster {

    @Id
    private Integer id;

    @Column(nullable = false, length = 50)
    private String stageId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer maxHealth;

    @Column(nullable = false)
    private Integer contactDamage;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal moveSpeed;

    @Column(nullable = false, length = 30)
    private String attackKind;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal attackRange;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal attackInterval;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal projectileSpeed;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal hitRadius;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal attackDistance;

    public Monster(Integer id) {
        this.id = id;
    }

    public void apply(String stageId, MonsterData data) {
        this.stageId = stageId;
        this.name = data.name();
        this.maxHealth = data.maxHealth();
        this.contactDamage = data.contactDamage();
        this.moveSpeed = data.moveSpeed();
        this.attackKind = data.attackKind();
        this.attackRange = data.attackRange();
        this.attackInterval = data.attackInterval();
        this.projectileSpeed = data.projectileSpeed();
        this.hitRadius = data.hitRadius();
        this.attackDistance = data.attackDistance();
    }
}
