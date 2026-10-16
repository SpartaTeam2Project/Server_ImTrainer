package com.trainergameserver.master.service;

import com.trainergameserver.common.Sha256;
import com.trainergameserver.master.dto.MasterDataTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * 마스터데이터 동기화와 해시 비교.
 * <ol>
 *   <li>서버 시작 시 resources/masterdata/{테이블}.json을 DB에 반영합니다(파일이 원본, DB는 파일과 같게 맞춤).</li>
 *   <li>DB에서 다시 읽어 테이블별 JSON 문자열과 SHA-256 해시를 만들어 메모리에 둡니다.</li>
 *   <li>클라가 보낸 해시와 다른 테이블만 내려줍니다.</li>
 * </ol>
 * 같은 DB 데이터는 항상 같은 문자열이 되도록 필드 순서(@JsonPropertyOrder), 행 순서(id), 소수 표기(끝자리 0 제거)를 고정합니다.
 * 테이블 목록은 {@link MasterTable}을 상속한 빈들입니다.
 */
@Service
public class MasterDataService implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);

    private final List<MasterTable<?>> masterTables;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)       // 6E+1 대신 60
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)  // 클라가 필드를 추가하면 알 수 있게
            .build();

    private volatile Map<String, MasterDataTable> tables = Map.of();

    public MasterDataService(List<MasterTable<?>> masterTables, TransactionTemplate transactionTemplate) {
        // 응답 순서를 매번 같게 하려고 이름순으로 정렬
        this.masterTables = masterTables.stream().sorted(Comparator.comparing(MasterTable::name)).toList();
        this.transactionTemplate = transactionTemplate;
    }

    /** 웹 서버가 요청을 받기 전에 실행됩니다. 실패하면 서버가 뜨지 않습니다. */
    @Override
    public void afterSingletonsInstantiated() {
        Map<String, MasterDataTable> built = new LinkedHashMap<>();
        for (MasterTable<?> table : masterTables) {
            if (built.containsKey(table.name())) {
                throw new IllegalStateException("마스터데이터 테이블 이름이 중복됩니다: " + table.name());
            }
            built.put(table.name(), prepare(table));
        }
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

    private <T> MasterDataTable prepare(MasterTable<T> table) {
        T file = readFile(table.filePath(), table.type());
        table.validate(file);
        transactionTemplate.executeWithoutResult(status -> table.sync(file));

        T loaded = transactionTemplate.execute(status -> table.load());
        String json = jsonMapper.writeValueAsString(loaded);
        table.setCurrent(loaded);
        return new MasterDataTable(Sha256.hex(json), json);
    }

    private <T> T readFile(String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            T value = jsonMapper.readValue(in, type);
            if (value == null) {
                throw new IllegalStateException(path + "이(가) 비어 있습니다.");
            }
            return value;
        } catch (IOException e) {
            throw new UncheckedIOException(path + "을(를) 읽을 수 없습니다.", e);
        }
    }
}
