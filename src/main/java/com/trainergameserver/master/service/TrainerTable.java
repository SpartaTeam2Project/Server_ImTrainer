package com.trainergameserver.master.service;

import com.trainergameserver.master.dto.TrainerData;
import com.trainergameserver.master.dto.TrainerTableData;
import com.trainergameserver.master.entity.Trainer;
import com.trainergameserver.master.repository.TrainerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 트레이너 (trainer.json → trainers 테이블) */
@Component
public class TrainerTable extends MasterTable<TrainerTableData> {

    private final TrainerRepository trainerRepository;

    public TrainerTable(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Override
    public String name() {
        return "trainer";
    }

    @Override
    public Class<TrainerTableData> type() {
        return TrainerTableData.class;
    }

    /** 인트로에서 이 세대·성별을 고르면 지급되는 트레이너 id (unlockedByIntro = true) */
    public List<Integer> introTrainerIds(int generation, int gender) {
        return current().trainers().stream()
                .filter(t -> t.unlockedByIntro() && t.generation() == generation && t.gender() == gender)
                .map(TrainerData::id)
                .toList();
    }

    @Override
    protected void validate(TrainerTableData file) {
        requireValues(file, file.trainers());
        for (TrainerData t : file.trainers()) {
            requireValues(t, t.id(), t.key(), t.name(), t.generation(), t.gender(), t.unlockedByIntro());
            if (t.gender() != 0 && t.gender() != 1) {
                throw new IllegalStateException(filePath() + ": gender는 0 또는 1이어야 합니다. " + t);
            }
        }
        byId(file);
    }

    @Override
    protected void sync(TrainerTableData file) {
        Map<Integer, TrainerData> fromFile = byId(file);
        Map<Integer, Trainer> inDb = trainerRepository.findAll().stream()
                .collect(Collectors.toMap(Trainer::getId, Function.identity()));

        inDb.values().stream()
                .filter(trainer -> !fromFile.containsKey(trainer.getId()))
                .forEach(trainerRepository::delete);

        fromFile.values().forEach(data -> {
            Trainer trainer = inDb.get(data.id());
            if (trainer == null) {
                trainer = new Trainer(data.id());
            }
            trainer.apply(data);
            trainerRepository.save(trainer);
        });
    }

    @Override
    protected TrainerTableData load() {
        return new TrainerTableData(trainerRepository.findAll(Sort.by("id")).stream()
                .map(t -> new TrainerData(t.getId(), t.getTrainerKey(), t.getName(), t.getGeneration(),
                        t.getGender(), t.getUnlockedByIntro()))
                .toList());
    }

    private Map<Integer, TrainerData> byId(TrainerTableData file) {
        return file.trainers().stream()
                .collect(Collectors.toMap(TrainerData::id, Function.identity(), (a, b) -> {
                    throw duplicateId(a.id());
                }));
    }
}
