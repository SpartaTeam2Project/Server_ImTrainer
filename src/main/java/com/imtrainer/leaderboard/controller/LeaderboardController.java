package com.imtrainer.leaderboard.controller;

import com.imtrainer.leaderboard.dto.LeaderboardResponse;
import com.imtrainer.leaderboard.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<LeaderboardResponse> getLeaderboard(@RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaderboardService.getTop(size));
    }
}

