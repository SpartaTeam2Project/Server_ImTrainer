package com.trainergameserver.leaderboard.dto;

import java.util.List;

/** Unity JsonUtility 는 최상위 배열을 파싱하지 못하므로 객체로 감싼다 */
public record LeaderboardResponse(List<LeaderboardEntry> rankings) {
}
