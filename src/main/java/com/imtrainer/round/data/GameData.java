package com.imtrainer.round.data;

import org.springframework.stereotype.Component;

/**
 * 검증에 사용하는 게임 데이터 규칙입니다.
 * 추후에 실제 밸런스에 맞춰서 수정할 예정.
 * 일단 예시로 작성해 놓은 형태입니다.
 */
@Component
public class GameData {

    public static final double STAGE_DURATION_SEC = 1800;   // 클리어 생존 시간
    public static final double TIME_TOLERANCE_SEC = 3;      // 네트워크/프레임 오차 허용

    public static final double MAX_KILL_PER_SEC = 20;       // 초당 최대 처치 수
    public static final long MAX_EXP_PER_KILL = 30;         // 야생 포켓몬 1마리 최대 경험치
    public static final long MAX_BALL_PER_KILL = 3;         // 야생 포켓몬 1마리 최대 몬스터볼
    public static final double MAX_EXP_MULTIPLIER = 2.0;    // 업그레이드로 가능한 최대 경험치 배율
    public static final double MAX_BALL_MULTIPLIER = 2.0;   // 업그레이드로 가능한 최대 재화 배율

    // 보스: 출현 예정 시간, 최소 처치 소요 시간(체력 / 이론상 최대 DPS), 보상
    public record BossData(double spawnTime, double minKillSec, long exp, long ball) {}

    public static final BossData RIVAL = new BossData(600, 15, 500, 50);
    public static final BossData CHAMPION = new BossData(1200, 25, 1500, 150);

    // 도감번호 (몇번부터 몇번까지 있는지 아직 몰라서 그냥 일전에 시트에 써놓은 1세대 번호만 확인했습니다.)
    private static final int MIN_POKEMON_ID = 1;
    private static final int MAX_POKEMON_ID = 151;

    // 레벨 테이블: index = 레벨, 값 = 해당 레벨에 필요한 누적 경험치
    private final long[] levelTable = buildLevelTable(100);

    public boolean pokemonExists(int pokemonId) {
        return pokemonId >= MIN_POKEMON_ID && pokemonId <= MAX_POKEMON_ID;
    }

    public int levelOf(long totalExp) {
        int level = 1;
        while (level + 1 < levelTable.length && totalExp >= levelTable[level + 1]) {
            level++;
        }
        return level;
    }

    private static long[] buildLevelTable(int maxLevel) {
        long[] table = new long[maxLevel + 1];
        for (int lv = 2; lv <= maxLevel; lv++) {
            table[lv] = table[lv - 1] + lv * 50L;
        }
        return table;
    }
}
