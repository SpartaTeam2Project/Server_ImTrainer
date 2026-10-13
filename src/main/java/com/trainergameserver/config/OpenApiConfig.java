package com.trainergameserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    private static final String DESCRIPTION = """
            ## 공통 인증 규칙

            - `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`를 제외한 모든 API는
              헤더에 `Authorization: Bearer {accessToken}`을 붙여 호출합니다.
            - 인증이 필요한 API가 `401`을 반환하면 `POST /auth/refresh`로 토큰을 재발급받은 뒤 원래 요청을 재호출합니다.
            - refresh도 `401`이면 로그인 화면으로 이동합니다. **refresh 실패 후에는 재시도하지 않습니다.**

            ## 토큰 저장 규칙

            - 로그인 응답의 `accessToken`, `refreshToken`을 모두 기기의 보안 저장소에 저장합니다.
            - refresh 성공 시 응답의 두 토큰을 **모두 새 값으로 교체**합니다. 이전 refreshToken은 재사용할 수 없습니다.
            - 이미 사용한 refreshToken을 다시 보내면 탈취로 판단해 **그 로그인에서 발급된 토큰을 모두 폐기**합니다.
              (10초 안의 중복 요청은 401만 반환하고 폐기하지 않습니다.) refresh는 한 번에 하나만 호출하세요.
            - refresh 실패(`401`) 또는 로그아웃 시 저장된 토큰을 모두 삭제하고 로그인 화면으로 이동합니다.
            - 앱을 다시 실행하면 저장된 토큰으로 바로 `POST /master-data`를 호출합니다. 토큰이 없으면 로그인 화면으로 이동합니다.

            ## 게임 시작 흐름

            1. `POST /master-data` 호출
            2. `200` → 게임 화면으로 이동
            3. `401` → refresh 후 재호출, refresh 실패 시 로그인 화면으로 이동

            ## 플레이데이터 (3. Player)

            - 로그인 후 `POST /player/load`(본문 없음)로 받습니다. 신규 계정은 `intro.done=false`, 빈 목록, `pocket_dollar` 0입니다.
            - 쓰기 요청(인트로, 트레이닝 구매·초기화, 트레이너 선택)은 클라가 먼저 계산해 화면에 반영하고 결과를 보냅니다.
              서버는 같은 마스터데이터로 다시 계산합니다.
              - 맞으면 `200`과 갱신된 PlayerData 전체 → 그대로 저장
              - 다르면 `409`와 `{code, message, playerData}` → **playerData로 덮어쓰기(롤백)**
            - `409 MISMATCH`가 반복되면 마스터데이터가 바뀌었을 수 있으니 `POST /master-data`를 다시 호출합니다.

            | code | 의미 |
            | --- | --- |
            | `MISMATCH` | 클라 계산값(cost, refund, balanceAfter, toLevel, currencyId)이 서버와 다름 |
            | `NOT_ENOUGH_CURRENCY` | 잔액 부족 |
            | `REQUIREMENT_NOT_MET` | 트레이닝 해금 조건 미충족, 인트로에서 받을 수 없는 트레이너 |
            | `MAX_LEVEL` | 이미 최대 레벨 |
            | `NOT_OWNED` | 보유하지 않은 트레이너 선택 |
            | `INTRO_ALREADY_DONE` | 인트로를 이미 완료함 |
            | `UNKNOWN_ID` | 마스터데이터에 없는 트레이닝·포켓몬 id |

            ## 에러 응답 형식

            모든 에러는 `{ "code": "...", "message": "..." }` 형태로 내려가며, 화면에는 `message`를 그대로 표시하면 됩니다.
            입력값 오류가 여러 개여도 메시지는 하나만 내려가므로 길이·공백 검사는 클라이언트에서 먼저 하는 것을 권장합니다.

            | code | message | 발생 API |
            | --- | --- | --- |
            | `INVALID_REQUEST` | 아이디를 입력해 주세요. | signup, login |
            | `INVALID_REQUEST` | 아이디는 4~20자로 입력해 주세요. | signup |
            | `INVALID_REQUEST` | 비밀번호를 입력해 주세요. | signup, login |
            | `INVALID_REQUEST` | 비밀번호는 8~20자로 입력해 주세요. | signup |
            | `INVALID_REQUEST` | 리프레시 토큰을 입력해 주세요. | refresh, logout |
            | `INVALID_REQUEST` | 요청 본문(JSON) 형식이 올바르지 않습니다. | 본문이 있는 모든 API |
            | `DUPLICATE_USERNAME` | 이미 사용 중인 아이디입니다. | signup |
            | `UNAUTHORIZED` | 아이디 또는 비밀번호가 일치하지 않습니다. | login |
            | `TOO_MANY_REQUESTS` | 로그인 시도가 너무 많습니다. N분 후 다시 시도해 주세요. | login (429, `Retry-After` 헤더: 초) |
            | `UNAUTHORIZED` | 리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해 주세요. | refresh |
            | `UNAUTHORIZED` | 액세스 토큰이 없습니다. | logout, master-data, player, training |
            | `UNAUTHORIZED` | 액세스 토큰이 유효하지 않거나 만료되었습니다. | logout, master-data, player, training |

            ## 토큰 유효시간

            - 액세스 토큰: 30분
            - 리프레시 토큰: 14일 (한 번 사용하면 폐기되고 새 토큰으로 교체)

            ## 로그인 시도 제한

            - 같은 기기(IP)에서 같은 아이디로 5번 실패하면 15분 동안 `429`를 반환합니다. 같은 IP 전체로는 50번입니다.
            - `429`를 받으면 `message`를 표시하고, `Retry-After` 헤더(초)가 지나기 전에는 재시도하지 않습니다.
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("imtrainer API")
                        .version("v1")
                        .description(DESCRIPTION))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
