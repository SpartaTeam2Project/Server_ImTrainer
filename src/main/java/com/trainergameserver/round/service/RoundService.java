package com.trainergameserver.round.service;

import com.trainergameserver.auth.entity.User;
import com.trainergameserver.auth.repository.UserRepository;
import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.common.UnauthorizedException;
import com.trainergameserver.leaderboard.service.LeaderboardService;
import com.trainergameserver.master.service.PlayerBaseTable;
import com.trainergameserver.master.service.RoundRuleTable;
import com.trainergameserver.master.service.StorageMonsterTable;
import com.trainergameserver.master.service.TrainerTable;
import com.trainergameserver.player.entity.Player;
import com.trainergameserver.player.repository.PlayerRepository;
import com.trainergameserver.player.service.CurrencyService;
import com.trainergameserver.player.service.PlayerService;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundEndResponse;
import com.trainergameserver.round.dto.RoundResult;
import com.trainergameserver.round.entity.RoundViolation;
import com.trainergameserver.round.repository.RoundViolationRepository;
import com.trainergameserver.round.validation.RoundValidationContext;
import com.trainergameserver.round.validation.RoundValidationException;
import com.trainergameserver.round.validation.RoundValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    /** 종료 요청이 오지 않은 라운드(튕김, 강제 종료)를 치우는 청소용 시간. 정상 플레이(30분 + 일시정지)보다 넉넉하게 */
    private static final Duration ROUND_TTL = Duration.ofHours(2);
    private final PlayerService playerService;
    private final TrainerTable trainerTable;
    private final StorageMonsterTable storageMonsterTable;
    private final RoundViolationRepository roundViolationRepository;

    private final StringRedisTemplate redisTemplate;
    private final RoundValidator roundValidator;
    private final LeaderboardService leaderboardService;
    private final CurrencyService currencyService;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final RoundRuleTable roundRuleTable;
    private final PlayerBaseTable playerBaseTable;

    public RoundService(PlayerService playerService, StringRedisTemplate redisTemplate, RoundValidator roundValidator,
                        LeaderboardService leaderboardService, CurrencyService currencyService,
                        UserRepository userRepository, PlayerRepository playerRepository,
                        RoundRuleTable roundRuleTable, PlayerBaseTable playerBaseTable,
                        TrainerTable trainerTable, StorageMonsterTable storageMonsterTable,
                        RoundViolationRepository roundViolationRepository) {
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
        this.roundRuleTable = roundRuleTable;
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
     * 해금 검증 → 해금 반영 → 결과 검증 → 리더보드 기록 → CLEAR면 남은 몬스터볼만큼 pocket_dollar 지급.
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
                roundRuleTable.current(), playerBaseTable.current(), Set.copyOf(player.getOwnedMonsterIds()),
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

        long reward = rewardOf(request);
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

    /** 지급 규칙: CLEAR면 남은 몬스터볼(current)을 1:1로, DEAD면 0 */
    static long rewardOf(RoundEndRequest request) {
        return request.result() == RoundResult.CLEAR ? request.monsterBall().current() : 0;
    }
}
