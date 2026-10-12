package com.trainergameserver.dto;

/**
 * 테이블 하나의 마스터데이터.
 * data는 테이블 JSON을 문자열로 담은 것이고, hash는 그 문자열(UTF-8)의 SHA-256 소문자 hex입니다.
 * 클라는 data 문자열을 그대로 저장하고 hash를 다음 요청 때 보냅니다.
 */
public record MasterDataTable(String hash, String data) {
}
