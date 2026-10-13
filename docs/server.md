# imtrainer-server

ImTrainer 게임 서버입니다. 회원 인증(JWT)과 마스터데이터(몬스터 등 고정 데이터) 동기화를 담당합니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어 / 프레임워크 | Java 21, Spring Boot 4.1.1 |
| DB | MySQL 8 (로컬: Docker, 배포: AWS RDS), Spring Data JPA, Flyway |
| 인증 | JWT (jjwt 0.12.6), BCrypt (spring-security-crypto) |
| 문서 | springdoc-openapi (Swagger UI) |
| 배포 | Docker, GitHub Actions, AWS ECR, EC2 |

## 폴더 구조

```
src/main/java/com/trainergameserver
├── common        # 공통 예외, 에러 응답, 글로벌 예외 처리
├── config        # Swagger, 비밀번호 암호화, 인터셉터 등록, 스케줄링
├── controller    # AuthController, MasterDataController
├── dto           # 요청/응답 객체
├── entity        # User, RefreshToken, Monster
├── repository    # JPA 리포지토리
├── security      # JWT 발급/검증, 인증 인터셉터, @AuthUser
└── service       # AuthService, MasterDataService, 만료 토큰 정리
src/main/resources
├── application*.properties   # 공통 / local / dev / prod 설정
├── db/migration/V*.sql       # Flyway DB 스키마 (버전별)
└── masterdata/monster.json   # 몬스터 마스터데이터 원본
```

## 실행 방법 (로컬)

### 환경 변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `JWT_SECRET` | O | Base64로 인코딩한 32바이트 이상 랜덤 값 |
| `DB_PASSWORD` | X | local 기본값은 `root` |
| `CORS_ALLOWED_ORIGINS` | X | 웹 빌드가 호출할 출처, 쉼표로 여러 개 (예: `http://localhost:5173`). 비우면 CORS 비활성화 |

`JWT_SECRET` 만들기 (PowerShell):

```powershell
$b = New-Object byte[] 64; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

### 방법 1. IntelliJ + Docker MySQL

```powershell
docker run -d --name imtrainer-mysql -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=game `
  mysql:8.4 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

IntelliJ Run Configuration의 Environment variables에 `JWT_SECRET=...`을 넣고 `TrainerGameServerApplication`을 실행합니다. 프로필을 지정하지 않으면 `local`로 실행됩니다.

### 방법 2. Docker Compose (서버 + MySQL)

```powershell
copy .env.example .env          # .env에 JWT_SECRET 값 입력
.\gradlew.bat bootJar
docker compose up --build
```

compose의 MySQL은 기존 컨테이너와 겹치지 않도록 `3307` 포트로 열립니다.

### 확인

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 로그에 `마스터데이터 monster 준비 완료`, `Started TrainerGameServerApplication`이 나오면 정상입니다.

## 프로필

| 프로필 | DB | 비고 |
| --- | --- | --- |
| `local` | localhost:3306 (`root` / `root`) | 기본 프로필, SQL 로그 출력 |
| `dev` | AWS RDS (`DB_USERNAME`, `DB_PASSWORD`, `DB_URL`은 생략 시 dev RDS) | Swagger 사용 |
| `prod` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 필수 (기본값 없음) | Swagger 비활성화 |

모든 프로필에서 테이블은 Flyway가 만들고, Hibernate는 `ddl-auto=validate`로 엔티티와 맞는지만 검사합니다.

### 서버 환경 변수 (dev, prod 공통)

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | O | `dev` 또는 `prod` |
| `JWT_SECRET` | O | 환경마다 다른 값 사용 (dev 토큰으로 prod에 접근하지 못하게) |
| `DB_URL` | prod만 | `jdbc:mysql://<host>:3306/game?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME`, `DB_PASSWORD` | O | DB 계정 |
| `CORS_ALLOWED_ORIGINS` | X | 웹 빌드 출처 (예: `https://game.example.com`) |
| `FORWARD_HEADERS_STRATEGY` | X | HTTPS 프록시(Nginx, ALB) 뒤에 둘 때 `native`. 직접 받는 동안에는 비워 둠 |

## DB 스키마 (Flyway)

- 스키마 변경은 `src/main/resources/db/migration`에 `V{번호}__{설명}.sql` 파일을 추가합니다. 서버가 시작할 때 아직 적용하지 않은 파일을 순서대로 실행합니다.
- **이미 적용된 파일은 절대 수정하지 않습니다.** (체크섬이 달라져 서버가 시작되지 않음) 바꿀 내용은 새 번호로 추가합니다.
- 엔티티를 고치면 같은 내용의 마이그레이션도 함께 추가해야 합니다. 빠뜨리면 `validate`에서 서버 시작이 실패합니다.
- Flyway 도입 전부터 테이블이 있던 DB(dev RDS, 기존 로컬 DB)는 `baseline-on-migrate`로 V1(초기 스키마)을 건너뛰고 V2부터 적용합니다. 빈 DB는 V1부터 모두 실행합니다.
- 적용 이력은 `flyway_schema_history` 테이블에서 확인할 수 있습니다.

| 버전 | 내용 |
| --- | --- |
| V1 | 초기 스키마 (`users`, `refresh_tokens`, `monsters`) |
| V2 | 리프레시 토큰 재사용 감지 (`family_id`, `used_at` 컬럼) |

## API

`/auth/signup`, `/auth/login`, `/auth/refresh`를 제외한 모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/auth/signup` | 회원가입 (아이디 4~20자, 비밀번호 8~20자) |
| POST | `/auth/login` | 로그인, accessToken·refreshToken 발급 |
| POST | `/auth/refresh` | 토큰 재발급. 사용한 refreshToken은 다시 쓸 수 없고 새 토큰 쌍 발급 |
| POST | `/auth/logout` | refreshToken 폐기 |
| POST | `/master-data` | 마스터데이터 해시 비교 후 바뀐 테이블만 반환 |

- 토큰 유효시간: access 30분, refresh 14일
- 같은 refreshToken으로 동시에 요청하면 하나만 성공하고 나머지는 401
- refreshToken은 DB에 SHA-256 해시로 저장, 만료 토큰은 매일 04:00 삭제

### 리프레시 토큰 재사용 감지

- 로그인 한 번에서 refresh로 이어지는 토큰들을 하나의 family로 묶습니다. 기기(로그인)마다 family가 다릅니다.
- 사용한 refreshToken은 바로 지우지 않고 `used_at`을 기록해 만료될 때까지 남겨 둡니다.
- 사용한 토큰이 **10초 안에** 다시 오면 동시 요청으로 보고 401만 반환합니다. (`jwt.refresh-reuse-grace-seconds`)
- **10초가 지난 뒤** 다시 오면 탈취로 보고 그 family의 토큰을 모두 폐기합니다. 공격자와 사용자 모두 다시 로그인해야 하고, 다른 기기의 로그인은 유지됩니다. 서버 로그에 `리프레시 토큰 재사용 감지`가 남습니다.
- 로그아웃하면 그 기기의 family 전체를 폐기합니다.
- 클라이언트는 refresh를 한 번에 하나만 호출해야 합니다. (여러 요청이 동시에 401을 받아도 refresh는 한 번만 하고 나머지는 결과를 기다림)

### 로그인 시도 제한

| 기준 | 한도 | 차단 시간 |
| --- | --- | --- |
| 같은 IP + 같은 아이디 | 5번 실패 | 15분 |
| 같은 IP 전체 | 50번 실패 | 15분 |

- 차단 중에는 비밀번호가 맞아도 `429 TOO_MANY_REQUESTS`와 `Retry-After`(초) 헤더를 반환합니다.
- 로그인에 성공하면 그 IP + 아이디의 실패 횟수는 초기화됩니다.
- 아이디만 기준으로 막으면 남의 계정을 일부러 잠글 수 있어 IP와 묶었습니다.
- 횟수는 서버 메모리에 저장합니다. 재시작하면 초기화되고, 서버를 여러 대로 늘리면 서버별로 셉니다. (그때는 Redis 등으로 옮겨야 함)
- 클라이언트 IP는 `request.getRemoteAddr()` 기준입니다. HTTPS 프록시 뒤에 두면 `FORWARD_HEADERS_STRATEGY=native`로 실제 IP를 받아야 모든 사용자가 같은 IP로 묶이지 않습니다.

### CORS (웹 빌드)

브라우저에서 실행되는 게임(Unity WebGL 빌드)이 다른 주소의 서버를 호출해도 되는지 정하는 설정입니다.
브라우저는 게임 페이지와 주소가 다른 서버를 호출하기 전에 허용 여부를 먼저 묻고, 서버가 허용하지 않으면 요청을 막습니다.

예: 게임 페이지 `https://imtrainer.itch.io` → API 서버 `https://api.imtrainer.com` (주소가 달라 허용 필요)

**설정**: 게임 페이지 주소를 `CORS_ALLOWED_ORIGINS`에 넣습니다. 여러 개면 쉼표로 구분합니다. dev 서버는 Parameter Store `/imtrainer/dev/CORS_ALLOWED_ORIGINS`에 추가합니다.

```
CORS_ALLOWED_ORIGINS=https://imtrainer.itch.io,http://localhost:8000
```

**비워 두면** 기존과 같이 동작합니다. 웹 빌드를 쓰지 않으면 비워 둡니다.

| 호출하는 곳 | 비워 두면 |
| --- | --- |
| Unity 에디터, PC(.exe), 모바일 빌드 | 정상 동작 (브라우저가 아니라 CORS 검사를 하지 않음) |
| Postman, curl, 테스트 코드 | 정상 동작 |
| Swagger UI (`/swagger-ui`) | 정상 동작 (서버와 같은 주소) |
| WebGL 빌드 (다른 주소에서 브라우저로 실행) | 브라우저가 막음. 콘솔에 `CORS policy` 에러, 게임에서는 네트워크 오류로 보임 |

**세부 설정**

- 허용 메서드 `GET`, `POST`, `OPTIONS`, 허용 헤더 `Authorization`, `Content-Type`
- 토큰은 쿠키가 아니라 `Authorization` 헤더로 보내므로 credentials(쿠키 전송)는 허용하지 않습니다.
- `https://*.example.com`처럼 와일드카드 패턴도 쓸 수 있습니다.
- 응답 헤더 중 `Retry-After`(로그인 시도 제한)는 웹 빌드에서도 읽을 수 있게 열어 두었습니다.

### 에러 응답

모든 에러는 `{ "code": "...", "message": "..." }` 형식입니다. 입력값 오류가 여러 개면 아이디 → 비밀번호, 미입력 → 길이 순으로 하나만 내려갑니다.

| code | HTTP | 상황 |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | 입력값 오류, JSON 형식 오류 |
| `UNAUTHORIZED` | 401 | 토큰 없음/만료, 로그인 실패, refresh 실패 |
| `DUPLICATE_USERNAME` | 409 | 아이디 중복 |
| `TOO_MANY_REQUESTS` | 429 | 로그인 시도 제한. `Retry-After` 헤더(초) 뒤에 재시도 |
| `NOT_FOUND` / `METHOD_NOT_ALLOWED` / `UNSUPPORTED_MEDIA_TYPE` | 404 / 405 / 415 | 잘못된 경로, 메서드, Content-Type |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

## 마스터데이터

### 서버 동작

1. 서버 시작 시 `resources/masterdata/monster.json`을 DB `monsters` 테이블에 반영합니다. (파일이 원본, DB는 파일 내용을 따라감)
2. DB에서 다시 읽어 테이블별 JSON 문자열과 SHA-256 해시를 만들어 메모리에 둡니다.
3. 클라이언트가 보낸 해시와 다른 테이블만 내려줍니다.

### 요청 / 응답

```json
POST /master-data
{ "hashes": { "monster": "a02691a9..." } }
```

```json
{ "tables": { "monster": { "hash": "a02691a9...", "data": "{\"stageId\":\"PrototypeStage\",\"monsters\":[...]}" } } }
```

- 처음 실행이면 `{"hashes": {}}`를 보냅니다.
- `tables`가 비어 있으면 모든 테이블이 최신입니다.

### 클라이언트 규칙

1. 게임 시작마다 로컬에 저장된 `data` 문자열로 SHA-256(소문자 hex)을 계산해 보냅니다. 해시는 따로 저장하지 않습니다.
2. 응답에 테이블이 있으면 `data` 문자열을 받은 그대로 저장합니다. (다시 직렬화하면 해시가 달라짐)
3. 저장된 `data` 문자열을 파싱해서 게임에서 사용합니다. 파싱 결과를 따로 저장해 쓰지 않습니다.

### 데이터 수정 / 추가

- 값 수정: `monster.json`을 교체하고 재배포합니다. DB를 직접 고쳐도 재시작하면 파일 내용으로 덮어써집니다.
- 소수 값은 소수점 셋째 자리까지만 허용합니다. (DB `DECIMAL(10,3)`, 넘으면 서버 시작 실패)
- 새 테이블 추가: 엔티티, 리포지토리, DTO, `MasterDataService` 등록과 테이블을 만드는 Flyway 마이그레이션(`V{번호}__*.sql`)이 필요합니다.

## CI/CD

`.github/workflows/deploy.yml`

```
git push (YJ_2) → 빌드(bootJar) → Docker 이미지 생성 → AWS ECR push → SSM으로 EC2에 배포 명령 → 컨테이너 교체
```

- 현재는 `YJ_2` 브랜치 push에서만 실행됩니다.
- 팀 합의 후 `on.push.branches`에 `dev`를 추가하면 dev push에서도 같은 흐름으로 배포됩니다.
- 이미지는 `linux/amd64`, `linux/arm64` 두 아키텍처용으로 만듭니다. (dev EC2가 ARM(aarch64) 인스턴스)
- 배포 단계는 EC2에서 IAM Role로 계정 번호를 조회해 이미지 주소를 만듭니다. (이미지 주소에 Secret이 포함돼 job 사이로 넘길 수 없기 때문)
- CI는 job 안에서 MySQL을 띄워 `./gradlew test bootJar`로 테스트를 실행합니다. 테스트가 실패하면 이미지 업로드와 배포를 하지 않습니다.

### GitHub Secrets

| 이름 | 용도 |
| --- | --- |
| `AWS_ROLE_ARN` | GitHub Actions가 OIDC로 수임할 IAM Role ARN (`arn:aws:iam::<계정>:role/imtrainer-github-actions`) |
| `ECR_REPOSITORY` | ECR 저장소 이름 |
| `EC2_INSTANCE_ID` | 배포할 EC2 인스턴스 ID (`i-...`) |

SSH 키는 사용하지 않습니다. 배포 명령은 AWS SSM(Run Command)으로 전송합니다.

### AWS 인증 (GitHub OIDC)

GitHub Actions는 장기 Access Key 대신 GitHub OIDC로 IAM Role을 수임해 임시 자격 증명(1시간)을 받습니다. 키가 유출되거나 교체할 일이 없습니다.

1. **OIDC 공급자 등록** (계정당 1회): IAM → 자격 증명 공급자 → 공급자 추가
   - 유형: OpenID Connect
   - 공급자 URL: `https://token.actions.githubusercontent.com`
   - 대상(Audience): `sts.amazonaws.com`
2. **IAM Role 생성** (`imtrainer-github-actions`), 신뢰 정책:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<계정>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:SpartaTeam2Project@332745443/Server_ImTrainer@1382631345:ref:refs/heads/YJ_2"
      }
    }
  }]
}
```

   - 이 저장소는 GitHub가 `sub`에 조직·저장소 ID를 함께 넣는 형식(`조직@조직ID/저장소@저장소ID`)을 씁니다. `repo:SpartaTeam2Project/Server_ImTrainer:...`처럼 이름만 쓰면 `Not authorized to perform sts:AssumeRoleWithWebIdentity`로 실패합니다.
   - `sub` 조건으로 이 저장소의 `YJ_2` 브랜치 push에서만 Role을 쓸 수 있습니다. dev 배포를 추가하면 `StringEquals`를 `StringLike`로 바꾸지 말고 `sub` 값을 배열로 두어 `repo:SpartaTeam2Project@332745443/Server_ImTrainer@1382631345:ref:refs/heads/dev`를 추가합니다.
   - PR 워크플로는 AWS를 쓰지 않으므로 신뢰 정책에 넣지 않습니다.
3. **Role 권한 정책**:
   - `AmazonEC2ContainerRegistryPowerUser` (ECR push) 또는 해당 저장소로 범위를 좁힌 정책
   - SSM 명령 전송: `ssm:SendCommand`, `ssm:GetCommandInvocation`
4. GitHub 저장소 Settings → Secrets에 `AWS_ROLE_ARN`을 등록합니다.
5. 첫 배포가 성공하면 기존 IAM 사용자의 Access Key를 비활성화·삭제하고, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` Secret도 지웁니다.

### EC2 준비

- Docker, AWS CLI 설치 (`sudo dnf install -y docker`, `sudo systemctl enable --now docker`)
- EC2 IAM Role에 `AmazonEC2ContainerRegistryReadOnly`, `AmazonSSMManagedInstanceCore` 정책 연결
- Systems Manager → Fleet Manager에서 인스턴스가 Online인지 확인 (Amazon Linux 2023은 SSM Agent 기본 설치)
- 환경변수는 Parameter Store `/imtrainer/dev/*` (목록은 `deploy/app.env.example` 참고). EC2 역할에 `ssm:GetParametersByPath` 권한 필요
- 기존 jar 실행용 systemd 서비스는 `sudo systemctl disable --now imtrainer`로 중지 (8080 포트 비우기)
- 배포에 SSH(22) 포트는 필요 없음
- 서버는 EC2 인스턴스가 ARM(aarch64)이므로 arm64 이미지로 실행됨

## HTTPS

현재는 컨테이너가 8080을 HTTP로 직접 엽니다. 토큰과 비밀번호가 암호화되지 않으므로 외부 공개 전에 HTTPS를 붙여야 합니다. 공개 인증서를 받으려면 **도메인이 필요**합니다.

| 방식 | 비용 | 특징 |
| --- | --- | --- |
| ALB + ACM 인증서 | ALB 시간당 요금 (월 약 2~3만 원) | 인증서 자동 갱신, 서버 여러 대로 늘리기 쉬움 |
| EC2에 Nginx/Caddy + Let's Encrypt | 무료 | 인증서 자동 갱신(Caddy), EC2 한 대일 때 간단 |

어느 쪽이든 붙인 뒤에는:

1. Parameter Store에 `FORWARD_HEADERS_STRATEGY=native` 추가 (로그인 제한이 실제 클라이언트 IP를 쓰도록)
2. EC2 보안 그룹에서 8080 외부 접근을 막고 443만 열기
3. 클라이언트의 서버 주소를 `https://`로 변경
