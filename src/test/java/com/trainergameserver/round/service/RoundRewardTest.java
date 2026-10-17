package com.trainergameserver.round.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 포켓달러 지급: 주운 양 × (1 + ShardBonus), 0.5 이상 올림 */
class RoundRewardTest {

    @Test
    @DisplayName("ShardBonus 를 안 샀으면 주운 만큼 지급")
    void noBonus() {
        assertThat(RoundService.rewardOf(1000, BigDecimal.ZERO)).isEqualTo(1000);
    }

    @Test
    @DisplayName("ShardBonus 0.2 면 1.2배")
    void withBonus() {
        assertThat(RoundService.rewardOf(1000, new BigDecimal("0.2"))).isEqualTo(1200);
    }

    @Test
    @DisplayName("소수점은 0.5 이상 올림 (15 × 1.1 = 16.5 → 17). float 오차 없이 계산")
    void roundHalfUp() {
        assertThat(RoundService.rewardOf(15, new BigDecimal("0.1"))).isEqualTo(17);
        assertThat(RoundService.rewardOf(14, new BigDecimal("0.1"))).isEqualTo(15);   // 15.4 → 15
    }

    @Test
    @DisplayName("주운 게 없으면 0")
    void zero() {
        assertThat(RoundService.rewardOf(0, new BigDecimal("0.5"))).isZero();
    }
}
