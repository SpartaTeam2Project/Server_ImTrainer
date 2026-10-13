package com.trainergameserver.master.entity;

import com.trainergameserver.master.dto.StorageMonsterData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 보유 가능 포켓몬 마스터데이터. 값은 resources/masterdata/storage_monster.json에서 반영됩니다. */
@Entity
@Table(name = "storage_monsters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageMonster {

    /** 도감번호 */
    @Id
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer generation;

    @Column(nullable = false)
    private Boolean legendary;

    public StorageMonster(Integer id) {
        this.id = id;
    }

    public void apply(StorageMonsterData data) {
        this.name = data.name();
        this.generation = data.generation();
        this.legendary = data.legendary();
    }
}
