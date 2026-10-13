package com.trainergameserver.service;

import com.trainergameserver.dto.MasterDataTable;
import com.trainergameserver.dto.MonsterData;
import com.trainergameserver.dto.MonsterTableData;
import com.trainergameserver.entity.Monster;
import com.trainergameserver.repository.MonsterRepository;
import com.trainergameserver.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 마스터데이터 동기화와 해시 비교.
 * <ol>
 *   <li>서버 시작 시 resources/masterdata/*.json을 DB에 반영합니다(파일이 원본, DB는 파일과 같게 맞춤).</li>
 *   <li>DB에서 다시 읽어 테이블별 JSON 문자열과 SHA-256 해시를 만들어 메모리에 둡니다.</li>
 *   <li>클라가 보낸 해시와 다른 테이블만 내려줍니다.</li>
 * </ol>
 * 같은 DB 데이터는 항상 같은 문자열이 되도록 필드 순서(@JsonPropertyOrder), 행 순서(id), 소수 표기(끝자리 0 제거)를 고정합니다.
 */
@Service
public class MasterDataService implements SmartInitializingSingleton {

    public static final String MONSTER = "monster";

    private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);
    private static final String MONSTER_FILE = "masterdata/monster.json";
    /** DB 컬럼이 DECIMAL(10,3)이라 소수점 셋째 자리까지만 저장됩니다. 넘으면 조용히 반올림되지 않도록 시작 시 실패시킵니다. */
    private static final int MAX_SCALE = 3;

    private final MonsterRepository monsterRepository;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)       // 6E+1 대신 60
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)  // 클라가 필드를 추가하면 알 수 있게
            .build();

    private volatile Map<String, MasterDataTable> tables = Map.of();

    public MasterDataService(MonsterRepository monsterRepository, TransactionTemplate transactionTemplate) {
        this.monsterRepository = monsterRepository;
        this.transactionTemplate = transactionTemplate;
    }

    /** 웹 서버가 요청을 받기 전에 실행됩니다. 실패하면 서버가 뜨지 않습니다. */
    @Override
    public void afterSingletonsInstantiated() {
        MonsterTableData file = readFile(MONSTER_FILE, MonsterTableData.class);
        transactionTemplate.executeWithoutResult(status -> syncMonsters(file));

        String monsterJson = transactionTemplate.execute(status -> jsonMapper.writeValueAsString(loadMonsterTable()));
        Map<String, MasterDataTable> built = new LinkedHashMap<>();
        built.put(MONSTER, new MasterDataTable(TokenHasher.sha256(monsterJson), monsterJson));
        tables = Collections.unmodifiableMap(built);

        built.forEach((name, table) -> log.info("마스터데이터 {} 준비 완료 (hash={})", name, table.hash()));
    }

    /** 클라가 보낸 해시(테이블 이름 → 해시)와 다른 테이블만 돌려줍니다. 보내지 않은 테이블은 전부 내려갑니다. */
    public Map<String, MasterDataTable> findChanged(Map<String, String> clientHashes) {
        Map<String, MasterDataTable> changed = new LinkedHashMap<>();
        tables.forEach((name, table) -> {
            if (!table.hash().equals(clientHashes.get(name))) {
                changed.put(name, table);
            }
        });
        return changed;
    }

    private void syncMonsters(MonsterTableData file) {
        Map<Integer, MonsterData> fromFile = file.monsters().stream()
                .peek(this::validate)
                .collect(Collectors.toMap(MonsterData::id, Function.identity(), (a, b) -> {
                    throw new IllegalStateException(MONSTER_FILE + ": 몬스터 id가 중복됩니다. id=" + a.id());
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

    private MonsterTableData loadMonsterTable() {
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

    private void validate(MonsterData data) {
        if (data.id() == null || data.name() == null || data.maxHealth() == null || data.contactDamage() == null
                || data.attackKind() == null) {
            throw new IllegalStateException(MONSTER_FILE + ": 비어 있는 값이 있습니다. " + data);
        }
        for (BigDecimal value : Arrays.asList(data.moveSpeed(), data.attackRange(), data.attackInterval(),
                data.projectileSpeed(), data.hitRadius(), data.attackDistance())) {
            if (value == null) {
                throw new IllegalStateException(MONSTER_FILE + ": 비어 있는 값이 있습니다. " + data);
            }
            if (value.stripTrailingZeros().scale() > MAX_SCALE) {
                throw new IllegalStateException(MONSTER_FILE + ": 소수점 " + MAX_SCALE + "자리를 넘는 값이 있습니다. " + data);
            }
        }
    }

    /** DB DECIMAL(10,3)에서 읽은 0.700, 6.000을 0.7, 6으로 */
    private static BigDecimal plain(BigDecimal value) {
        return value.stripTrailingZeros();
    }

    private <T> T readFile(String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return jsonMapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(path + "을(를) 읽을 수 없습니다.", e);
        }
    }
}
