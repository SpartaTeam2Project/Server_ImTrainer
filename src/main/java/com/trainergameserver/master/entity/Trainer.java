package com.trainergameserver.master.entity;

import com.trainergameserver.master.dto.TrainerData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 트레이너 마스터데이터. 값은 resources/masterdata/trainer.json에서 반영됩니다. */
@Entity
@Table(name = "trainers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trainer {

    @Id
    private Integer id;

    /** JSON의 key (클라 에셋 이름). key는 MySQL 예약어라 컬럼명을 trainer_key로 둡니다. */
    @Column(name = "trainer_key", nullable = false, length = 100)
    private String trainerKey;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer generation;

    /** 0 남자, 1 여자 */
    @Column(nullable = false)
    private Integer gender;

    /** 인트로에서 세대·성별이 맞으면 바로 지급되는지 */
    @Column(nullable = false)
    private Boolean unlockedByIntro;

    public Trainer(Integer id) {
        this.id = id;
    }

    public void apply(TrainerData data) {
        this.trainerKey = data.key();
        this.name = data.name();
        this.generation = data.generation();
        this.gender = data.gender();
        this.unlockedByIntro = data.unlockedByIntro();
    }
}
