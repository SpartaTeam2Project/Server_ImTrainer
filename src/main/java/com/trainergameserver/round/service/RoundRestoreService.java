package com.trainergameserver.round.service;

import com.trainergameserver.auth.entity.User;
import com.trainergameserver.auth.repository.UserRepository;
import com.trainergameserver.leaderboard.service.LeaderboardService;
import com.trainergameserver.player.entity.Player;
import com.trainergameserver.player.repository.PlayerRepository;
import com.trainergameserver.player.service.CurrencyService;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import com.trainergameserver.round.entity.RoundViolation;
import com.trainergameserver.round.entity.ViolationStatus;
import com.trainergameserver.round.repository.RoundViolationRepository;
import com.trainergameserver.round.validation.RoundValidationException;
import com.trainergameserver.round.validation.RoundValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 위반 기록 복구: 결과 검증에 걸렸지만 정상 플레이로 판단된 판의 리더보드 기록과 포켓달러를 되살립니다.
 * HTTP API 가 없고, 서버 안에서 RoundRestoreRunner 로만 실행합니다 (관리자 권한이 생기기 전까지).
 *
 * 순서: 위반 기록 확인 → 현재 기준값으로 다시 검증 → (apply 일 때) 리더보드·포켓달러·복구 표시를 한 트랜잭션으로.
 * 해금은 위반 당시 이미 저장됐으므로(unlock_applied=1) 다시 하지 않습니다.
 */
@Service
public class RoundRestoreService {

    private final RoundViolationRepository roundViolationRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final RoundValidator roundValidator;
    private final RoundService roundService;
    private final LeaderboardService leaderboardService;
    private final CurrencyService currencyService;

    public RoundRestoreService(RoundViolationRepository roundViolationRepository, UserRepository userRepository,
                               PlayerRepository playerRepository, RoundValidator roundValidator,
                               RoundService roundService, LeaderboardService leaderboardService,
                               CurrencyService currencyService) {
        this.roundViolationRepository = roundViolationRepository;
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
        this.roundValidator = roundValidator;
        this.roundService = roundService;
        this.leaderboardService = leaderboardService;
        this.currencyService = currencyService;
    }

    /**
     * @param apply false 면 미리보기만 (아무것도 바꾸지 않음)
     * @param force 현재 기준으로도 검증에 실패하는 판을 그래도 복구할지 (사람이 직접 확인한 경우)
     */
    @Transactional
    public RestoreReport restore(long violationId, boolean apply, boolean force) {
        RoundViolation v = roundViolationRepository.findById(violationId).orElse(null);
        if (v == null) {
            return RestoreReport.skipped(violationId, "위반 기록이 없음");
        }
        if (v.getStatus() != ViolationStatus.PENDING) {
            return RestoreReport.skipped(violationId, "상태가 " + v.getStatus() + " (PENDING 만 복구)");
        }
        if (!Boolean.TRUE.equals(v.getUnlockApplied())) {
            return RestoreReport.skipped(violationId, "해금 검증 실패(" + v.getCode() + ")는 잘못된 요청이라 복구 대상 아님");
        }

        RoundEndRequest request;
        try {
            request = RoundService.JSON.readValue(v.getRequest(), RoundEndRequest.class);
        } catch (Exception e) {
            return RestoreReport.skipped(violationId, "요청을 읽을 수 없음 (JSON 아님)");
        }

        User user = userRepository.findById(v.getUserId()).orElse(null);
        if (user == null) {
            return RestoreReport.skipped(violationId, "계정이 없음");
        }
        // 라운드 종료·트레이닝 구매와 같은 행 잠금: 복구 중에 이 계정의 요청이 와도 섞이지 않음
        Player player = playerRepository.findForUpdate(user.getId()).orElseThrow();

        // 서버 경과 시간은 위반 당시 값, 기준값은 지금 값으로 다시 검증 (기준을 고친 뒤 정상 플레이를 골라내는 용도)
        String recheck;
        boolean passesNow;
        try {
            roundValidator.validateResult(request, roundService.contextFor(player, v.getServerElapsedSec()));
            passesNow = true;
            recheck = "현재 기준으로 통과";
        } catch (RoundValidationException e) {
            passesNow = false;
            recheck = "현재 기준으로도 실패: " + e.getCode() + " " + e.getReason();
        }

        boolean clear = request.result() == RoundResult.CLEAR;
        long reward = v.getExpectedReward() != null ? v.getExpectedReward() : roundService.rewardFor(player, request);

        if (!apply) {
            return new RestoreReport(violationId, user.getUsername(), v.getCode(), recheck, clear, reward, "미리보기 (변경 없음)");
        }
        if (!passesNow && !force) {
            return new RestoreReport(violationId, user.getUsername(), v.getCode(), recheck, clear, reward,
                    "건너뜀: 현재 기준으로도 실패 (확인했다면 force=true)");
        }

        // 리더보드·포켓달러·복구 표시를 한 트랜잭션으로. 하나라도 실패하면 전부 취소
        if (clear) {
            // 원래 플레이한 시각으로 기록해야 동점 순위(먼저 달성한 순)가 공정함
            leaderboardService.record(user.getUsername(), request, v.getCreatedAt());
        }
        if (reward > 0) {
            currencyService.grant(user.getId(), CurrencyService.POCKET_DOLLAR, reward);
        }
        v.markRestored(LocalDateTime.now());
        return new RestoreReport(violationId, user.getUsername(), v.getCode(), recheck, clear, reward, "복구 완료");
    }

    public record RestoreReport(long violationId, String username, String code, String recheck,
                                boolean clear, long reward, String outcome) {
        static RestoreReport skipped(long id, String why) {
            return new RestoreReport(id, "-", "-", "-", false, 0, "건너뜀: " + why);
        }

        @Override
        public String toString() {
            return "#%d user=%s code=%s | %s | 리더보드=%s 포켓달러=%d | %s"
                    .formatted(violationId, username, code, recheck, clear ? "기록" : "없음(DEAD)", reward, outcome);
        }
    }
}
