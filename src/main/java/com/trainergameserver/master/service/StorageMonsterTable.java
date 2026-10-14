package com.trainergameserver.master.service;

import com.trainergameserver.master.dto.StorageMonsterData;
import com.trainergameserver.master.dto.StorageMonsterTableData;
import com.trainergameserver.master.entity.StorageMonster;
import com.trainergameserver.master.repository.StorageMonsterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 보유 가능 포켓몬 (storage_monster.json → storage_monsters 테이블) */
@Component
public class StorageMonsterTable extends MasterTable<StorageMonsterTableData> {

    private final StorageMonsterRepository storageMonsterRepository;

    public StorageMonsterTable(StorageMonsterRepository storageMonsterRepository) {
        this.storageMonsterRepository = storageMonsterRepository;
    }

    @Override
    public String name() {
        return "storage_monster";
    }

    @Override
    public Class<StorageMonsterTableData> type() {
        return StorageMonsterTableData.class;
    }

    /** 보유할 수 있는 포켓몬 id인지 (플레이데이터 검증용) */
    public boolean exists(int monsterId) {
        return current().monsters().stream().anyMatch(m -> m.id() == monsterId);
    }

    @Override
    protected void validate(StorageMonsterTableData file) {
        requireValues(file, file.monsters());
        file.monsters().forEach(m -> requireValues(m, m.id(), m.name(), m.generation(), m.legendary()));
        byId(file);
    }

    @Override
    protected void sync(StorageMonsterTableData file) {
        Map<Integer, StorageMonsterData> fromFile = byId(file);
        Map<Integer, StorageMonster> inDb = storageMonsterRepository.findAll().stream()
                .collect(Collectors.toMap(StorageMonster::getId, Function.identity()));

        inDb.values().stream()
                .filter(monster -> !fromFile.containsKey(monster.getId()))
                .forEach(storageMonsterRepository::delete);

        fromFile.values().forEach(data -> {
            StorageMonster monster = inDb.get(data.id());
            if (monster == null) {
                monster = new StorageMonster(data.id());
            }
            monster.apply(data);
            storageMonsterRepository.save(monster);
        });
    }

    @Override
    protected StorageMonsterTableData load() {
        return new StorageMonsterTableData(storageMonsterRepository.findAll(Sort.by("id")).stream()
                .map(m -> new StorageMonsterData(m.getId(), m.getName(), m.getGeneration(), m.getLegendary()))
                .toList());
    }

    private Map<Integer, StorageMonsterData> byId(StorageMonsterTableData file) {
        return file.monsters().stream()
                .collect(Collectors.toMap(StorageMonsterData::id, Function.identity(), (a, b) -> {
                    throw duplicateId(a.id());
                }));
    }
}
