package com.trainergameserver.master.service;

import com.trainergameserver.master.dto.MonsterData;
import com.trainergameserver.master.dto.MonsterTableData;
import com.trainergameserver.master.entity.Monster;
import com.trainergameserver.master.repository.MonsterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 스테이지 몬스터 (monster.json → monsters 테이블) */
@Component
public class MonsterTable extends MasterTable<MonsterTableData> {

    private final MonsterRepository monsterRepository;

    public MonsterTable(MonsterRepository monsterRepository) {
        this.monsterRepository = monsterRepository;
    }

    @Override
    public String name() {
        return "monster";
    }

    @Override
    public Class<MonsterTableData> type() {
        return MonsterTableData.class;
    }

    @Override
    protected void validate(MonsterTableData file) {
        requireValues(file, file.stageId(), file.monsters());
        for (MonsterData data : file.monsters()) {
            requireValues(data, data.id(), data.name(), data.maxHealth(), data.contactDamage(), data.attackKind());
            requireScale(data, data.moveSpeed(), data.attackRange(), data.attackInterval(),
                    data.projectileSpeed(), data.hitRadius(), data.attackDistance());
        }
    }

    @Override
    protected void sync(MonsterTableData file) {
        Map<Integer, MonsterData> fromFile = file.monsters().stream()
                .collect(Collectors.toMap(MonsterData::id, Function.identity(), (a, b) -> {
                    throw duplicateId(a.id());
                }));

        Map<Integer, Monster> inDb = monsterRepository.findAll().stream()
                .collect(Collectors.toMap(Monster::getId, Function.identity()));

        // 파일에서 빠진 몬스터는 DB에서도 삭제
        inDb.values().stream()
                .filter(monster -> !fromFile.containsKey(monster.getId()))
                .forEach(monsterRepository::delete);

        // 추가/수정 (값이 같으면 Hibernate가 UPDATE를 보내지 않음)
        fromFile.values().forEach(data -> {
            Monster monster = inDb.get(data.id());
            if (monster == null) {
                monster = new Monster(data.id());
            }
            monster.apply(file.stageId(), data);
            monsterRepository.save(monster);
        });
    }

    @Override
    protected MonsterTableData load() {
        List<Monster> monsters = monsterRepository.findAll(Sort.by("id"));
        Set<String> stageIds = monsters.stream().map(Monster::getStageId).collect(Collectors.toSet());
        if (stageIds.size() > 1) {
            throw new IllegalStateException("monsters 테이블에 stageId가 여러 개입니다: " + stageIds);
        }
        String stageId = stageIds.stream().findFirst().orElse(null);
        List<MonsterData> rows = monsters.stream()
                .map(m -> new MonsterData(
                        m.getId(), m.getName(), m.getMaxHealth(), m.getContactDamage(),
                        plain(m.getMoveSpeed()), m.getAttackKind(), plain(m.getAttackRange()),
                        plain(m.getAttackInterval()), plain(m.getProjectileSpeed()),
                        plain(m.getHitRadius()), plain(m.getAttackDistance())))
                .toList();
        return new MonsterTableData(stageId, rows);
    }
}
