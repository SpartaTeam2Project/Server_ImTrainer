package com.trainergameserver.master.dto;

import java.util.Map;

/**
 * 마스터데이터 요청. hashes는 테이블 이름 → 클라가 저장된 data 문자열로 계산한 해시.
 * 처음 실행이라 저장된 데이터가 없으면 {"hashes": {}}를 보냅니다.
 */
public record MasterDataRequest(Map<String, String> hashes) {

    public Map<String, String> hashesOrEmpty() {
        return hashes == null ? Map.of() : hashes;
    }
}
