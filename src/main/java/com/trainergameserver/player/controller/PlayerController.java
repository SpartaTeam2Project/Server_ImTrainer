package com.trainergameserver.player.controller;

import com.trainergameserver.auth.security.AuthUser;
import com.trainergameserver.common.ErrorResponse;
import com.trainergameserver.player.dto.IntroRequest;
import com.trainergameserver.player.dto.PlayerData;
import com.trainergameserver.player.dto.PlayerErrorResponse;
import com.trainergameserver.player.dto.TrainerSelectRequest;
import com.trainergameserver.player.dto.TrainingPurchaseRequest;
import com.trainergameserver.player.dto.TrainingResetRequest;
import com.trainergameserver.player.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 플레이데이터 조회와 쓰기 요청. 쓰기 요청은 클라가 먼저 계산해 화면에 반영하고, 서버가 같은 마스터데이터로 다시 검증합니다.
 * 성공하면 갱신된 PlayerData 전체를, 검증에 실패하면 409와 서버 기준 playerData를 돌려줍니다.
 */
@Tag(name = "3. Player", description = "플레이데이터 조회 / 인트로 / 트레이닝 구매·초기화 / 트레이너 선택")
@RestController
public class PlayerController {

    private static final String CONFLICT_DESCRIPTION = "서버 검증 실패 → 응답의 playerData로 덮어쓰기(롤백)";

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Operation(summary = "플레이데이터 조회",
            description = "로그인 직후 호출합니다. 본문 없음. 신규 계정이면 intro.done=false, 빈 목록, pocket_dollar 0을 돌려줍니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @PostMapping("/player/load")
    public PlayerData load(@Parameter(hidden = true) @AuthUser String username) {
        return playerService.load(username);
    }

    @Operation(summary = "인트로 완료",
            description = "계정당 한 번. 스타터 포켓몬은 storage_monster에 있는 id여야 합니다. "
                    + "trainer 중 unlockedByIntro=true이고 세대·성별이 맞는 트레이너를 모두 지급하며, trainerId는 그중 하나여야 합니다.")
    @ApiResponse(responseCode = "200", description = "성공, 갱신된 PlayerData")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = PlayerErrorResponse.class), examples = {
                    @ExampleObject(name = "이미 완료", value = "{\"code\": \"INTRO_ALREADY_DONE\", \"message\": \"이미 인트로를 완료했습니다.\", \"playerData\": {}}"),
                    @ExampleObject(name = "없는 포켓몬", value = "{\"code\": \"UNKNOWN_ID\", \"message\": \"선택할 수 없는 스타터 포켓몬입니다.\", \"playerData\": {}}"),
                    @ExampleObject(name = "받을 수 없는 트레이너", value = "{\"code\": \"REQUIREMENT_NOT_MET\", \"message\": \"선택한 세대·성별로 받을 수 없는 트레이너입니다.\", \"playerData\": {}}")}))
    @ApiResponse(responseCode = "400", description = "입력값 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/player/intro")
    public PlayerData intro(@Parameter(hidden = true) @AuthUser String username,
                            @Valid @RequestBody IntroRequest request) {
        return playerService.intro(username, request);
    }

    @Operation(summary = "트레이너 선택", description = "보유한 트레이너만 선택할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "성공, 갱신된 PlayerData")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = PlayerErrorResponse.class), examples = {
                    @ExampleObject(name = "미보유", value = "{\"code\": \"NOT_OWNED\", \"message\": \"보유하지 않은 트레이너입니다.\", \"playerData\": {}}")}))
    @PostMapping("/player/trainer/select")
    public PlayerData selectTrainer(@Parameter(hidden = true) @AuthUser String username,
                                    @Valid @RequestBody TrainerSelectRequest request) {
        return playerService.selectTrainer(username, request);
    }

    @Operation(summary = "트레이닝 구매",
            description = "현재 레벨에서 한 레벨 올립니다. 서버는 toLevel = 현재 레벨 + 1, 해금 조건, "
                    + "cost = levels[toLevel-1].cost, 잔액 ≥ cost, balanceAfter = 잔액 - cost를 검증합니다.")
    @ApiResponse(responseCode = "200", description = "성공, 갱신된 PlayerData")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = PlayerErrorResponse.class), examples = {
                    @ExampleObject(name = "잔액 부족", value = "{\"code\": \"NOT_ENOUGH_CURRENCY\", \"message\": \"재화가 부족합니다.\", \"playerData\": {}}"),
                    @ExampleObject(name = "해금 전", value = "{\"code\": \"REQUIREMENT_NOT_MET\", \"message\": \"아직 해금되지 않은 트레이닝입니다.\", \"playerData\": {}}"),
                    @ExampleObject(name = "최대 레벨", value = "{\"code\": \"MAX_LEVEL\", \"message\": \"이미 최대 레벨입니다.\", \"playerData\": {}}"),
                    @ExampleObject(name = "계산 불일치", value = "{\"code\": \"MISMATCH\", \"message\": \"비용이 서버와 다릅니다. (서버 기준 650)\", \"playerData\": {}}"),
                    @ExampleObject(name = "없는 트레이닝", value = "{\"code\": \"UNKNOWN_ID\", \"message\": \"존재하지 않는 트레이닝입니다.\", \"playerData\": {}}")}))
    @PostMapping("/training/purchase")
    public PlayerData purchaseTraining(@Parameter(hidden = true) @AuthUser String username,
                                       @Valid @RequestBody TrainingPurchaseRequest request) {
        return playerService.purchaseTraining(username, request);
    }

    @Operation(summary = "트레이닝 초기화",
            description = "산 레벨들의 비용을 전액 환불하고 모든 레벨을 0으로 되돌립니다. "
                    + "서버는 refund = 산 레벨 비용의 합, balanceAfter = 잔액 + refund를 검증합니다.")
    @ApiResponse(responseCode = "200", description = "성공, 갱신된 PlayerData")
    @ApiResponse(responseCode = "409", description = CONFLICT_DESCRIPTION,
            content = @Content(schema = @Schema(implementation = PlayerErrorResponse.class), examples = {
                    @ExampleObject(name = "계산 불일치", value = "{\"code\": \"MISMATCH\", \"message\": \"환불액이 서버와 다릅니다. (서버 기준 910)\", \"playerData\": {}}")}))
    @PostMapping("/training/reset")
    public PlayerData resetTrainings(@Parameter(hidden = true) @AuthUser String username,
                                     @Valid @RequestBody TrainingResetRequest request) {
        return playerService.resetTrainings(username, request);
    }
}
