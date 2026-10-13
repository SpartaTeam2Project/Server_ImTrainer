package com.imtrainer.leaderboard.dto;

import java.util.List;

public record LeaderboardResponse(List<LeaderboardEntry> rankings) {
}
