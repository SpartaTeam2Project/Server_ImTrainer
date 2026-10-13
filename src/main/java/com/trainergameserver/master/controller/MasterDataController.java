package com.trainergameserver.master.controller;

import com.trainergameserver.common.ErrorResponse;
import com.trainergameserver.master.dto.MasterDataRequest;
import com.trainergameserver.master.dto.MasterDataResponse;
import com.trainergameserver.master.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. MasterData", description = "게임 시작 시 호출")
@RestController
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @Operation(summary = "마스터데이터 조회",
            description = "게임 시작 시 호출합니다. 200이면 게임 화면으로 이동, "
                    + "401이면 refresh 후 재호출하고, refresh도 실패하면 로그인 화면으로 이동합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공 → 게임 화면으로 이동")
    @ApiResponse(responseCode = "401", description = "액세스 토큰 없음/만료 → refresh 후 재호출",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "토큰 없음", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"액세스 토큰이 없습니다.\"}"),
                    @ExampleObject(name = "토큰 만료/위조", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"액세스 토큰이 유효하지 않거나 만료되었습니다.\"}")}))
    @PostMapping("/master-data")
    public MasterDataResponse getMasterData(@RequestBody MasterDataRequest request) {
        // 액세스 토큰 검증은 AuthInterceptor에서 처리합니다.
        // 본문 예: {"hashes": {"monster": "{hash}"}}
        return new MasterDataResponse(masterDataService.findChanged(request.hashesOrEmpty()));
    }
}
