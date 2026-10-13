package com.trainergameserver.master.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/** 클라가 보낸 해시와 다른 테이블만 담깁니다. 비어 있으면 모든 테이블이 최신입니다. key는 테이블 이름(monster 등). */
@Schema(description = "마스터데이터 응답")
public record MasterDataResponse(Map<String, MasterDataTable> tables) {
}
