package com.trainergameserver.controller;

import com.trainergameserver.common.ErrorResponse;
import com.trainergameserver.dto.LoginRequest;
import com.trainergameserver.dto.RefreshRequest;
import com.trainergameserver.dto.SignupRequest;
import com.trainergameserver.dto.TokenResponse;
import com.trainergameserver.security.AuthUser;
import com.trainergameserver.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Auth", description = "회원가입 / 로그인 / 토큰 재발급 / 로그아웃")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "회원가입", description = "인증 불필요. 아이디는 중복될 수 없습니다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "가입 성공 → 로그인 화면으로 이동")
    @ApiResponse(responseCode = "400", description = "입력값 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "아이디 미입력", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"아이디를 입력해 주세요.\"}"),
                    @ExampleObject(name = "아이디 길이", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"아이디는 4~20자로 입력해 주세요.\"}"),
                    @ExampleObject(name = "비밀번호 미입력", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"비밀번호를 입력해 주세요.\"}"),
                    @ExampleObject(name = "비밀번호 길이", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"비밀번호는 8~20자로 입력해 주세요.\"}"),
                    @ExampleObject(name = "JSON 형식 오류", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"요청 본문(JSON) 형식이 올바르지 않습니다.\"}")}))
    @ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "아이디 중복", value = "{\"code\": \"DUPLICATE_USERNAME\", \"message\": \"이미 사용 중인 아이디입니다.\"}")}))
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "로그인", description = "인증 불필요. 응답의 두 토큰을 저장해 두고 이후 API 호출에 사용합니다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "로그인 성공, 토큰 발급")
    @ApiResponse(responseCode = "400", description = "입력값 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "아이디 미입력", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"아이디를 입력해 주세요.\"}"),
                    @ExampleObject(name = "비밀번호 미입력", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"비밀번호를 입력해 주세요.\"}"),
                    @ExampleObject(name = "JSON 형식 오류", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"요청 본문(JSON) 형식이 올바르지 않습니다.\"}")}))
    @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "로그인 실패", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"아이디 또는 비밀번호가 일치하지 않습니다.\"}")}))
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "액세스 토큰 재발급",
            description = "인증 불필요. 다른 API가 401을 반환했을 때 호출합니다. "
                    + "성공 시 두 토큰을 모두 새 값으로 교체하세요. 사용한 리프레시 토큰은 즉시 폐기되어 재사용할 수 없습니다. "
                    + "실패(401) 시 재시도하지 말고 로그인 화면으로 이동합니다.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "재발급 성공 → 원래 요청 재호출")
    @ApiResponse(responseCode = "400", description = "입력값 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "토큰 미입력", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"리프레시 토큰을 입력해 주세요.\"}"),
                    @ExampleObject(name = "JSON 형식 오류", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"요청 본문(JSON) 형식이 올바르지 않습니다.\"}")}))
    @ApiResponse(responseCode = "401", description = "리프레시 토큰 만료/폐기/위조 → 로그인 화면으로 이동",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "재발급 실패", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해 주세요.\"}")}))
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기합니다. 클라이언트는 저장된 토큰을 모두 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "토큰 미입력", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"리프레시 토큰을 입력해 주세요.\"}"),
                    @ExampleObject(name = "JSON 형식 오류", value = "{\"code\": \"INVALID_REQUEST\", \"message\": \"요청 본문(JSON) 형식이 올바르지 않습니다.\"}")}))
    @ApiResponse(responseCode = "401", description = "액세스 토큰 없음/만료 → refresh 후 재호출",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "토큰 없음", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"액세스 토큰이 없습니다.\"}"),
                    @ExampleObject(name = "토큰 만료/위조", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"액세스 토큰이 유효하지 않거나 만료되었습니다.\"}")}))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @AuthUser String username,
            @Valid @RequestBody RefreshRequest request) {
        authService.logout(username, request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
