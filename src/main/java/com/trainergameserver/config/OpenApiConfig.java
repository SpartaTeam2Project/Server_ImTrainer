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
            - refresh 실패(`401`) 또는 로그아웃 시 저장된 토큰을 모두 삭제하고 로그인 화면으로 이동합니다.
            - 앱을 다시 실행하면 저장된 토큰으로 바로 `GET /master-data`를 호출합니다. 토큰이 없으면 로그인 화면으로 이동합니다.

            ## 게임 시작 흐름

            1. `GET /master-data` 호출
            2. `200` → 게임 화면으로 이동
            3. `401` → refresh 후 재호출, refresh 실패 시 로그인 화면으로 이동

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
            | `UNAUTHORIZED` | 리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해 주세요. | refresh |
            | `UNAUTHORIZED` | 액세스 토큰이 없습니다. | logout, master-data |
            | `UNAUTHORIZED` | 액세스 토큰이 유효하지 않거나 만료되었습니다. | logout, master-data |

            ## 토큰 유효시간

            - 액세스 토큰: 30분
            - 리프레시 토큰: 14일 (한 번 사용하면 폐기되고 새 토큰으로 교체)
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
