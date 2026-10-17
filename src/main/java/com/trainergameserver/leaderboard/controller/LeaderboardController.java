package com.trainergameserver.leaderboard.controller;

import com.trainergameserver.leaderboard.dto.LeaderboardResponse;
import com.trainergameserver.leaderboard.dto.MyRankResponse;
import com.trainergameserver.auth.security.AuthUser;
import com.trainergameserver.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "5. Leaderboard", description = "랭킹 조회")
@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Operation(summary = "리더보드 조회",
            description = "트레이너별 최고 기록을 처치 수 많은 순(같으면 먼저 달성한 순)으로 반환합니다. size는 1~100 (범위 밖이면 보정).")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public LeaderboardResponse getLeaderboard(@RequestParam(defaultValue = "10") int size) {
        return leaderboardService.getTop(size);
    }

    @Operation(summary = "내 순위 조회",
            description = "토큰의 트레이너 기록과 순위를 반환합니다. 상위 목록 밖이어도 몇 등인지 나오고, 기록이 없으면 ranked=false 입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    public MyRankResponse getMyRank(@Parameter(hidden = true) @AuthUser String username) {
        return leaderboardService.getMyRank(username);
    }
}
