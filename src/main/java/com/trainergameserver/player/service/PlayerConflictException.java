package com.trainergameserver.player.service;

import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.player.dto.PlayerData;

/**
 * 플레이데이터 쓰기 요청이 서버 검증에 실패한 경우 (409).
 * 응답에 서버 기준 playerData를 담아, 클라가 그 값으로 덮어쓰게(롤백) 합니다.
 */
public class PlayerConflictException extends ApiException {

    private final PlayerData playerData;

    public PlayerConflictException(ErrorCode code, String detail, PlayerData playerData, Object... args) {
        super(code, detail, args);
        this.playerData = playerData;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }
}
