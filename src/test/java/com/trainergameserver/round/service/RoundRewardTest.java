package com.trainergameserver.round.service;

import com.trainergameserver.round.dto.BossTime;
import com.trainergameserver.round.dto.MonsterBall;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundRewardTest {

    @Test
    @DisplayName("CLEAR면 남은 몬스터볼(current)만큼 지급")
    void clearGivesRemainingBalls() {
        assertThat(RoundService.rewardOf(request(RoundResult.CLEAR, 500))).isEqualTo(500);
    }

    @Test
    @DisplayName("DEAD면 지급하지 않음")
    void deadGivesNothing() {
        assertThat(RoundService.rewardOf(request(RoundResult.DEAD, 500))).isZero();
    }

    private static RoundEndRequest request(RoundResult result, long current) {
        return new RoundEndRequest(result, 1800.5, 1, 0L,
                new MonsterBall(current, 0L, 0L, current), 0, List.of(1), new BossTime(null, null, null, null));
    }
}
