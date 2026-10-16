package com.trainergameserver.leaderboard.dto;

import com.trainergameserver.leaderboard.entity.LeaderboardRecord;

/** 리더보드 조회 쿼리 결과 한 줄 (기록 + 플레이데이터의 닉네임). 응답으로는 LeaderboardEntry로 바꿔 내려갑니다. */
public record LeaderboardRow(LeaderboardRecord record, String nickname, Boolean introDone) {
}
