package com.trainergameserver.round.controller;

import com.trainergameserver.auth.security.AuthUser;
import com.trainergameserver.common.ErrorResponse;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundEndResponse;
import com.trainergameserver.round.service.RoundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 라운드 시작/종료. 시작 시각은 서버가 기록하고(Redis), 종료 시 서버 경과 시간과 게임 데이터로 결과를 검증합니다.
 */
@Tag(name = "4. Round", description = "라운드 시작 / 종료(결과 검증)")
@RestController
@RequestMapping("/rounds")
public class RoundController {

    private final RoundService roundService;

    public RoundController(RoundService roundService) {
        this.roundService = roundService;
    }

    @Operation(summary = "라운드 시작",
            description = "본문 없음. 서버가 요청을 받은 시각을 시작 시간으로 기록합니다. "
                    + "진행 중인 라운드가 있으면 이전 라운드는 기록 없이 종료(포기)되고 새로 시작합니다. "
                    + "201을 받은 시점부터 클라이언트가 플레이 시간을 측정합니다.")
    @ApiResponse(responseCode = "201", description = "시작 완료, 본문 없음")
    @PostMapping("/start")
    public ResponseEntity<Void> startRound(@Parameter(hidden = true) @AuthUser String username) {
        roundService.startRound(username);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "라운드 종료",
            description = "결과를 검증하고, 통과하면 리더보드에 기록합니다. CLEAR면 남은 몬스터볼(current)만큼 pocket_dollar를 지급합니다. "
                    + "검증 성공 여부와 관계없이 진행 중인 라운드는 종료됩니다.")
    @ApiResponse(responseCode = "200", description = "검증 성공. 지급량과 갱신된 playerData")
    @ApiResponse(responseCode = "400", description = "입력값 오류(INVALID_REQUEST) 또는 검증 실패(INVALID_TIME, INVALID_BOSS ...)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "진행 중인 라운드 없음(ROUND_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/end")
    public RoundEndResponse endRound(@Parameter(hidden = true) @AuthUser String username,
                                     @Valid @RequestBody RoundEndRequest request) {
        return roundService.endRound(username, request);
    }
}
