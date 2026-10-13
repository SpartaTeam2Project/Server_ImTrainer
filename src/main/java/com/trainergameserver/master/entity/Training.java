package com.trainergameserver.master.entity;

import com.trainergameserver.master.dto.TrainingData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 트레이닝 마스터데이터. 값은 resources/masterdata/training.json에서 서버 시작 시 반영됩니다. */
@Entity
@Table(name = "trainings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Training {

    @Id
    private Integer id;

    /** JSON의 key. key는 MySQL 예약어라 컬럼명을 training_key로 둡니다. */
    @Column(name = "training_key", nullable = false, length = 50)
    private String trainingKey;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false)
    private Integer category;

    /** JSON의 row (MySQL 예약어라 row_pos) */
    @Column(name = "row_pos", nullable = false)
    private Integer rowPos;

    /** JSON의 column (MySQL 예약어라 column_pos) */
    @Column(name = "column_pos", nullable = false)
    private Integer columnPos;

    @Column(nullable = false)
    private Integer valueDisplay;

    @Column(nullable = false)
    private Integer requiredCategory;

    @Column(nullable = false)
    private Integer requiredCategoryLevel;

    @Column(nullable = false)
    private Integer requiredTrainingId;

    @Column(nullable = false)
    private Integer requiredTrainingLevel;

    @ElementCollection
    @CollectionTable(name = "training_levels", joinColumns = @JoinColumn(name = "training_id"))
    @OrderBy("level ASC")
    private List<TrainingLevel> levels = new ArrayList<>();

    public Training(Integer id) {
        this.id = id;
    }

    public void apply(TrainingData data) {
        this.trainingKey = data.key();
        this.title = data.title();
        this.category = data.category();
        this.rowPos = data.row();
        this.columnPos = data.column();
        this.valueDisplay = data.valueDisplay();
        this.requiredCategory = data.requiredCategory();
        this.requiredCategoryLevel = data.requiredCategoryLevel();
        this.requiredTrainingId = data.requiredTrainingId();
        this.requiredTrainingLevel = data.requiredTrainingLevel();

        List<TrainingLevel> fromFile = data.levels().stream()
                .map(l -> new TrainingLevel(l.level(), l.value(), l.cost()))
                .toList();
        // 값이 같으면 컬렉션을 건드리지 않아 매 시작마다 DELETE/INSERT가 나가지 않게 합니다.
        if (!sameLevels(fromFile)) {
            levels.clear();
            levels.addAll(fromFile);
        }
    }

    private boolean sameLevels(List<TrainingLevel> other) {
        if (levels.size() != other.size()) {
            return false;
        }
        for (int i = 0; i < levels.size(); i++) {
            if (!levels.get(i).sameAs(other.get(i))) {
                return false;
            }
        }
        return true;
    }
}
