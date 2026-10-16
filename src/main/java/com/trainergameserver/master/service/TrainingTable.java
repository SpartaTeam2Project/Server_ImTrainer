package com.trainergameserver.master.service;

import com.trainergameserver.master.dto.TrainingData;
import com.trainergameserver.master.dto.TrainingLevelData;
import com.trainergameserver.master.dto.TrainingTableData;
import com.trainergameserver.master.entity.Training;
import com.trainergameserver.master.repository.TrainingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 트레이닝 (training.json → trainings, training_levels 테이블) */
@Component
public class TrainingTable extends MasterTable<TrainingTableData> {

    /** 카테고리 번호. 0 공격, 1 방어, 2 성장, 3 특수 */
    private static final int MIN_CATEGORY = 0;
    private static final int MAX_CATEGORY = 3;

    private final TrainingRepository trainingRepository;

    public TrainingTable(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    @Override
    public String name() {
        return "training";
    }

    @Override
    public Class<TrainingTableData> type() {
        return TrainingTableData.class;
    }

    /** 구매 검증에서 쓰는 트레이닝 조회 */
    public Optional<TrainingData> find(int trainingId) {
        return current().trainings().stream().filter(t -> t.id() == trainingId).findFirst();
    }

    @Override
    protected void validate(TrainingTableData file) {
        requireValues(file, file.trainings());
        for (TrainingData t : file.trainings()) {
            requireValues(t, t.id(), t.key(), t.title(), t.category(), t.row(), t.column(), t.valueDisplay(),
                    t.requiredCategory(), t.requiredCategoryLevel(), t.requiredTrainingId(),
                    t.requiredTrainingLevel(), t.levels());
            if (t.category() < MIN_CATEGORY || t.category() > MAX_CATEGORY
                    || t.requiredCategory() < MIN_CATEGORY || t.requiredCategory() > MAX_CATEGORY) {
                throw new IllegalStateException(filePath() + ": category는 0~3이어야 합니다. " + t);
            }
            if (t.levels().isEmpty()) {
                throw new IllegalStateException(filePath() + ": levels가 비어 있습니다. id=" + t.id());
            }
            // 구매 검증이 levels[L-1]로 비용을 찾으므로 level은 1부터 빈칸 없이 이어져야 합니다.
            for (int i = 0; i < t.levels().size(); i++) {
                TrainingLevelData level = t.levels().get(i);
                requireValues(t, level.level(), level.cost());
                requireScale(t, level.value());
                if (level.level() != i + 1 || level.cost() < 0) {
                    throw new IllegalStateException(filePath() + ": level은 1부터 차례로, cost는 0 이상이어야 합니다. id=" + t.id());
                }
            }
        }
        Map<Integer, TrainingData> byId = byId(file);
        for (TrainingData t : file.trainings()) {
            if (t.requiredTrainingId() != -1 && !byId.containsKey(t.requiredTrainingId())) {
                throw new IllegalStateException(filePath() + ": requiredTrainingId가 없는 트레이닝입니다. id=" + t.id());
            }
        }
    }

    @Override
    protected void sync(TrainingTableData file) {
        Map<Integer, TrainingData> fromFile = byId(file);
        Map<Integer, Training> inDb = trainingRepository.findAll().stream()
                .collect(Collectors.toMap(Training::getId, Function.identity()));

        inDb.values().stream()
                .filter(training -> !fromFile.containsKey(training.getId()))
                .forEach(trainingRepository::delete);

        fromFile.values().forEach(data -> {
            Training training = inDb.get(data.id());
            if (training == null) {
                training = new Training(data.id());
            }
            training.apply(data);
            trainingRepository.save(training);
        });
    }

    @Override
    protected TrainingTableData load() {
        List<TrainingData> rows = trainingRepository.findAll(Sort.by("id")).stream()
                .map(t -> new TrainingData(
                        t.getId(), t.getTrainingKey(), t.getTitle(), t.getCategory(), t.getRowPos(),
                        t.getColumnPos(), t.getValueDisplay(), t.getRequiredCategory(),
                        t.getRequiredCategoryLevel(), t.getRequiredTrainingId(), t.getRequiredTrainingLevel(),
                        t.getLevels().stream()
                                .map(l -> new TrainingLevelData(l.getLevel(), plain(l.getValue()), l.getCost()))
                                .toList()))
                .toList();
        return new TrainingTableData(rows);
    }

    private Map<Integer, TrainingData> byId(TrainingTableData file) {
        return file.trainings().stream()
                .collect(Collectors.toMap(TrainingData::id, Function.identity(), (a, b) -> {
                    throw duplicateId(a.id());
                }));
    }
}
