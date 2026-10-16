package com.trainergameserver.player.service;

import com.trainergameserver.auth.entity.User;
import com.trainergameserver.auth.repository.UserRepository;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.common.UnauthorizedException;
import com.trainergameserver.master.dto.TrainingData;
import com.trainergameserver.master.service.StorageMonsterTable;
import com.trainergameserver.master.service.TrainerTable;
import com.trainergameserver.master.service.TrainingTable;
import com.trainergameserver.player.dto.IntroRequest;
import com.trainergameserver.player.dto.PlayerData;
import com.trainergameserver.player.dto.TrainerSelectRequest;
import com.trainergameserver.player.dto.TrainingPurchaseRequest;
import com.trainergameserver.player.dto.TrainingResetRequest;
import com.trainergameserver.player.entity.Player;
import com.trainergameserver.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 플레이데이터 조회와 쓰기 요청 검증.
 * <p>
 * 쓰기 요청은 클라가 먼저 계산한 결과를 받아, 서버가 같은 마스터데이터로 다시 계산해 비교합니다.
 * 맞으면 저장하고 갱신된 PlayerData를, 다르면 409와 함께 서버 기준 PlayerData를 돌려줍니다(클라 롤백용).
 * 같은 계정의 쓰기 요청은 players 행 잠금으로 하나씩 처리합니다.
 */
@Service
@Transactional
public class PlayerService {

    private static final String POCKET_DOLLAR = CurrencyService.POCKET_DOLLAR;

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final TrainingTable trainingTable;
    private final TrainerTable trainerTable;
    private final StorageMonsterTable storageMonsterTable;
    private final CurrencyService currencyService;

    public PlayerService(PlayerRepository playerRepository, UserRepository userRepository,
                         TrainingTable trainingTable, TrainerTable trainerTable,
                         StorageMonsterTable storageMonsterTable, CurrencyService currencyService) {
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.trainingTable = trainingTable;
        this.trainerTable = trainerTable;
        this.storageMonsterTable = storageMonsterTable;
        this.currencyService = currencyService;
    }

    public PlayerData load(String username) {
        Long userId = ensurePlayer(username);
        Player player = playerRepository.findById(userId).orElseThrow();
        return toData(player);
    }

    /** 인트로 완료: 닉네임, 성별·세대, 스타터 포켓몬, 인트로 지급 트레이너. 계정당 한 번만 가능합니다. */
    public PlayerData intro(String username, IntroRequest request) {
        Player player = lockPlayer(username);
        if (player.getIntroDone()) {
            throw conflict(ErrorCode.INTRO_ALREADY_DONE, null, player);
        }
        if (!storageMonsterTable.exists(request.starterMonsterId())) {
            throw conflict(ErrorCode.UNKNOWN_ID, "starter", player);
        }
        List<Integer> granted = trainerTable.introTrainerIds(request.generation(), request.gender());
        if (!granted.contains(request.trainerId())) {
            throw conflict(ErrorCode.REQUIREMENT_NOT_MET, "intro-trainer", player);
        }

        player.completeIntro(request.nickname().strip(), request.gender(), request.generation(),
                request.starterMonsterId(), granted, request.trainerId());
        return toData(player);
    }

    /** 트레이닝 한 레벨 구매. 검증 순서: 트레이닝 존재 → 재화 → 최대 레벨 → toLevel → 해금 조건 → 비용 → 잔액 → 구매 후 잔액 */
    public PlayerData purchaseTraining(String username, TrainingPurchaseRequest request) {
        Player player = lockPlayer(username);
        TrainingData training = trainingTable.find(request.trainingId())
                .orElseThrow(() -> conflict(ErrorCode.UNKNOWN_ID, "training", player));
        if (!POCKET_DOLLAR.equals(request.currencyId())) {
            throw conflict(ErrorCode.MISMATCH, "currency", player);
        }

        int currentLevel = player.trainingLevel(training.id());
        if (currentLevel >= training.maxLevel()) {
            throw conflict(ErrorCode.MAX_LEVEL, null, player);
        }
        int nextLevel = currentLevel + 1;
        if (request.toLevel() != nextLevel) {
            throw conflict(ErrorCode.MISMATCH, "to-level", player, nextLevel);
        }
        if (!isUnlocked(player, training)) {
            throw conflict(ErrorCode.REQUIREMENT_NOT_MET, "training", player);
        }
        long cost = training.costOf(nextLevel);
        if (request.cost() != cost) {
            throw conflict(ErrorCode.MISMATCH, "cost", player, cost);
        }
        long balance = player.balance(POCKET_DOLLAR);
        if (balance < cost) {
            throw conflict(ErrorCode.NOT_ENOUGH_CURRENCY, null, player);
        }
        if (request.balanceAfter() != balance - cost) {
            throw conflict(ErrorCode.MISMATCH, "balance-after", player, balance - cost);
        }

        currencyService.spend(player.getUserId(), POCKET_DOLLAR, cost);
        player.setTrainingLevel(training.id(), nextLevel);
        return toData(player);
    }

    /** 트레이닝 초기화: 산 레벨들의 비용을 전액 환불하고 모든 레벨을 0으로 되돌립니다. */
    public PlayerData resetTrainings(String username, TrainingResetRequest request) {
        Player player = lockPlayer(username);
        long refund = refundOf(player);
        if (request.refund() != refund) {
            throw conflict(ErrorCode.MISMATCH, "refund", player, refund);
        }
        long balance = player.balance(POCKET_DOLLAR);
        if (request.balanceAfter() != balance + refund) {
            throw conflict(ErrorCode.MISMATCH, "reset-balance-after", player, balance + refund);
        }

        currencyService.grant(player.getUserId(), POCKET_DOLLAR, refund);
        player.resetTrainings();
        return toData(player);
    }

    public PlayerData selectTrainer(String username, TrainerSelectRequest request) {
        Player player = lockPlayer(username);
        if (!player.getOwnedTrainerIds().contains(request.trainerId())) {
            throw conflict(ErrorCode.NOT_OWNED, "trainer", player);
        }
        player.selectTrainer(request.trainerId());
        return toData(player);
    }

    /**
     * 해금 조건 (클라 TrainingManager와 같음). 둘 다 만족해야 합니다.
     * <ul>
     *   <li>requiredCategoryLevel이 0이거나, requiredCategory의 카테고리 레벨 ≥ requiredCategoryLevel</li>
     *   <li>requiredTrainingId가 -1이거나, 그 트레이닝의 현재 레벨 ≥ requiredTrainingLevel</li>
     * </ul>
     */
    private boolean isUnlocked(Player player, TrainingData training) {
        boolean categoryOk = training.requiredCategoryLevel() == 0
                || categoryLevel(player, training.requiredCategory()) >= training.requiredCategoryLevel();
        boolean trainingOk = training.requiredTrainingId() == -1
                || player.trainingLevel(training.requiredTrainingId()) >= training.requiredTrainingLevel();
        return categoryOk && trainingOk;
    }

    /** 카테고리 레벨 = 그 카테고리에 속한 트레이닝들의 현재 레벨 합 */
    private int categoryLevel(Player player, int category) {
        return trainingTable.current().trainings().stream()
                .filter(t -> t.category() == category)
                .mapToInt(t -> player.trainingLevel(t.id()))
                .sum();
    }

    /** 산 레벨들의 비용 합. 마스터데이터에서 사라진 트레이닝은 환불할 수 없어 0으로 봅니다. */
    private long refundOf(Player player) {
        long refund = 0;
        for (Map.Entry<Integer, Integer> entry : player.getTrainings().entrySet()) {
            TrainingData training = trainingTable.find(entry.getKey()).orElse(null);
            if (training == null) {
                continue;
            }
            int boughtLevel = Math.min(entry.getValue(), training.maxLevel());
            for (int level = 1; level <= boughtLevel; level++) {
                refund += training.costOf(level);
            }
        }
        return refund;
    }

    /** 행이 없으면 만들고 잠근 뒤 돌려줍니다. */
    private Player lockPlayer(String username) {
        Long userId = ensurePlayer(username);
        return playerRepository.findForUpdate(userId).orElseThrow();
    }

    private Long ensurePlayer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(UnauthorizedException.INVALID_TOKEN));
        playerRepository.createIfAbsent(user.getId(), user.getUsername());
        return user.getId();
    }

    /** detail은 messages.properties의 error.{code}.{detail} (없으면 error.{code}), args는 메시지의 {0}, {1} */
    private PlayerConflictException conflict(ErrorCode code, String detail, Player player, Object... args) {
        return new PlayerConflictException(code, detail, toData(player), args);
    }

    /** 항상 같은 순서로 내려가도록 id 오름차순 정렬. pocket_dollar는 0이어도 포함합니다. */
    static PlayerData toData(Player player) {
        List<PlayerData.TrainingLevel> trainings = player.getTrainings().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PlayerData.TrainingLevel(e.getKey(), e.getValue()))
                .toList();

        Map<String, Long> balances = new java.util.TreeMap<>(player.getCurrencies());
        balances.putIfAbsent(POCKET_DOLLAR, 0L);
        List<PlayerData.Currency> currencies = balances.entrySet().stream()
                .map(e -> new PlayerData.Currency(e.getKey(), e.getValue()))
                .toList();

        return new PlayerData(
                player.getNickname(),
                new PlayerData.Intro(player.getIntroDone(), player.getIntroGender(),
                        player.getIntroGeneration(), player.getStarterMonsterId()),
                player.getSelectedTrainerId(),
                trainings,
                currencies,
                player.getOwnedTrainerIds().stream().sorted(Comparator.naturalOrder()).toList(),
                player.getOwnedMonsterIds().stream().sorted(Comparator.naturalOrder()).toList());
    }
}
