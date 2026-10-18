package com.trainergameserver.round.service;

import com.trainergameserver.auth.entity.User;
import com.trainergameserver.auth.repository.UserRepository;
import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.common.UnauthorizedException;
import com.trainergameserver.leaderboard.service.LeaderboardService;
import com.trainergameserver.master.service.PlayerBaseTable;
import com.trainergameserver.master.dto.TrainingData;
import com.trainergameserver.master.service.StorageMonsterTable;
import com.trainergameserver.master.service.TrainingTable;
import com.trainergameserver.master.service.TrainerTable;
import com.trainergameserver.player.entity.Player;
import com.trainergameserver.player.repository.PlayerRepository;
import com.trainergameserver.player.service.CurrencyService;
import com.trainergameserver.player.service.PlayerService;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundEndResponse;
import com.trainergameserver.round.entity.RoundViolation;
import com.trainergameserver.round.repository.RoundViolationRepository;
import com.trainergameserver.round.rule.RoundRuleProperties;
import com.trainergameserver.round.validation.RoundValidationContext;
import com.trainergameserver.round.validation.RoundValidationException;
import com.trainergameserver.round.validation.RoundValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 진행 중인 라운드의 시작 시각은 Redis(round:{username})에 둡니다.
 * 종료 요청 때 꺼내면서 삭제하므로, 같은 라운드 결과를 두 번 제출할 수 없습니다.
 */
@Service
public class RoundService {

    private static final Logger log = LoggerFactory.getLogger(RoundService.class);

    private static final String KEY_PREFIX = "round:";
    /** 종료 요청이 오지 않은 라운드(튕김, 강제 종료)를 치우는 청소용 시간. 게임을 멈추고 오래 자리를 비우는 경우까지 고려해 24시간 */
    /** 트레이닝 마스터데이터의 포켓달러 보너스 키 (training.json id 5) */
    private static final String SHARD_BONUS_KEY = "ShardBonus";

    private static final Duration ROUND_TTL = Duration.ofHours(24);
    private final PlayerService playerService;
    private final TrainerTable trainerTable;
    private final StorageMonsterTable storageMonsterTable;
    private final RoundViolationRepository roundViolationRepository;
    private final TrainingTable trainingTable;

    private final StringRedisTemplate redisTemplate;
    private final RoundValidator roundValidator;
    private final LeaderboardService leaderboardService;
    private final CurrencyService currencyService;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final RoundRuleProperties roundRule;
    private final PlayerBaseTable playerBaseTable;

    public RoundService(PlayerService playerService, StringRedisTemplate redisTemplate, RoundValidator roundValidator,
                        LeaderboardService leaderboardService, CurrencyService currencyService,
                        UserRepository userRepository, PlayerRepository playerRepository,
                        RoundRuleProperties roundRule, PlayerBaseTable playerBaseTable,
                        TrainerTable trainerTable, StorageMonsterTable storageMonsterTable,
                        RoundViolationRepository roundViolationRepository, TrainingTable trainingTable) {
        this.trainingTable = trainingTable;
        this.roundViolationRepository = roundViolationRepository;
        this.playerService = playerService;
        this.trainerTable = trainerTable;
        this.storageMonsterTable = storageMonsterTable;
        this.redisTemplate = redisTemplate;
        this.roundValidator = roundValidator;
        this.leaderboardService = leaderboardService;
        this.currencyService = currencyService;
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
        this.roundRule = roundRule;
        this.playerBaseTable = playerBaseTable;
    }

    /** 진행 중인 라운드가 있으면 포기로 보고 덮어쓴다 (새 런 시작 = 이전 런 종료) */
    public void startRound(String username) {
        String key = KEY_PREFIX + username;
        String startedAt = String.valueOf(Instant.now().toEpochMilli());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            log.info("이전 라운드 포기 처리: {}", username);
        }
        redisTemplate.opsForValue().set(key, startedAt, ROUND_TTL);
    }

    /**
     * 해금 검증 → 해금 반영 → 결과 검증 → 리더보드 기록 → pocket_dollar 지급(주운 양 × (1 + ShardBonus), CLEAR/DEAD 모두).
     * 결과 검증에 실패해도 해금과 위반 기록은 저장됩니다(noRollbackFor). 해금 검증에 실패하면 위반 기록만 저장됩니다.
     * 리더보드 기록과 재화 지급은 한 트랜잭션이라 하나가 실패하면 둘 다 취소됩니다.
     * Redis 키는 트랜잭션과 별개로 먼저 지우므로, 검증에 실패해도 라운드는 종료됩니다.
     */
    @Transactional(noRollbackFor = RoundValidationException.class)
    public RoundEndResponse endRound(String username, RoundEndRequest request) {
        // 꺼내면서 삭제 (Redis 6.2+)
        String startedAt = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + username);
        if (startedAt == null) {
            throw new ApiException(ErrorCode.ROUND_NOT_FOUND);
        }
        double serverElapsedSec = (Instant.now().toEpochMilli() - Long.parseLong(startedAt)) / 1000.0;

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(UnauthorizedException.INVALID_TOKEN));
        playerRepository.createIfAbsent(user.getId(), user.getUsername());
        // 재화 지급과 같은 행 잠금. 같은 계정의 트레이닝 구매와 동시에 와도 하나씩 처리된다
        Player player = playerRepository.findForUpdate(user.getId()).orElseThrow();

        RoundValidationContext ctx = new RoundValidationContext(serverElapsedSec,
                roundRule, playerBaseTable.current(), Set.copyOf(player.getOwnedMonsterIds()),
                knownTrainerIds(), knownMonsterIds());
        // 1) 해금 검증: 확실한 검사라 실패하면 해금도 하지 않는다
        try {
            roundValidator.validateUnlocks(request, ctx);
        } catch (RoundValidationException e) {
            recordViolation(user.getId(), username, e, false, serverElapsedSec, request);
            throw e;
        }

        // 2) 해금 반영: CLEAR/DEAD, 결과 검증 성공 여부와 상관없이
        player.unlock(request.unlockedTrainerIds(), request.unlockedMonsterIds());
        if (!request.unlockedTrainerIds().isEmpty() || !request.unlockedMonsterIds().isEmpty()) {
            log.info("라운드 해금 user={} trainers={} monsters={}",
                    username, request.unlockedTrainerIds(), request.unlockedMonsterIds());
        }

        // 3) 결과 검증: 기준값 때문에 정상 플레이도 걸릴 수 있어, 실패해도 해금은 유지(noRollbackFor)하고
        //    리더보드·재화만 주지 않는다. 위반 기록은 DB에 남긴다
        try {
            roundValidator.validateResult(request, ctx);
        } catch (RoundValidationException e) {
            recordViolation(user.getId(), username, e, true, serverElapsedSec, request);
            throw e;
        }

        // 4) 결과 검증 통과 시에만
        leaderboardService.record(username, request, LocalDateTime.now());

        // 포켓달러: CLEAR/DEAD 모두, 주운 양 × (1 + ShardBonus)
        long reward = rewardOf(request.pocketDollar(), shardBonus(player));
        if (reward > 0) {
            currencyService.grant(user.getId(), CurrencyService.POCKET_DOLLAR, reward);
        }
        return new RoundEndResponse(reward, playerService.load(username));
    }

    /** 검증 실패를 로그와 DB(round_violations)에 남긴다. 응답은 공통 문구로 나간다 */
    private void recordViolation(Long userId, String username, RoundValidationException e, boolean unlockApplied,
                                 double serverElapsedSec, RoundEndRequest request) {
        log.warn("라운드 검증 실패 user={} code={} reason=[{}] unlockApplied={} serverElapsed={}s request={}",
                username, e.getCode(), e.getReason(), unlockApplied, "%.1f".formatted(serverElapsedSec), request);
        roundViolationRepository.save(RoundViolation.of(userId, e.getCode().name(), e.getReason(), unlockApplied,
                serverElapsedSec, request.toString(), LocalDateTime.now()));
    }

    private Set<Integer> knownTrainerIds() {
        return trainerTable.current().trainers().stream().map(t -> t.id()).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Integer> knownMonsterIds() {
        return storageMonsterTable.current().monsters().stream().map(m -> m.id()).collect(Collectors.toUnmodifiableSet());
    }

    /** 트레이닝 ShardBonus(부적 금화)의 현재 레벨 값. 안 샀으면 0 (예: 레벨 1 → 0.1 = +10%) */
    private BigDecimal shardBonus(Player player) {
        return trainingTable.current().trainings().stream()
                .filter(t -> SHARD_BONUS_KEY.equals(t.key()))
                .findFirst()
                .map(t -> levelValue(t, player.trainingLevel(t.id())))
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal levelValue(TrainingData training, int level) {
        if (level <= 0 || level > training.levels().size()) {
            return BigDecimal.ZERO;
        }
        return training.levels().get(level - 1).value();
    }

    /**
     * 지급 규칙: 주운 포켓달러 × (1 + ShardBonus), 0.5 이상 올림. CLEAR/DEAD 모두 지급.
     * 소수 오차가 없도록 BigDecimal로 계산합니다. 클라는 응답의 reward 를 그대로 표시합니다.
     */
    static long rewardOf(long pocketDollar, BigDecimal shardBonus) {
        return BigDecimal.valueOf(pocketDollar)
                .multiply(BigDecimal.ONE.add(shardBonus))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
