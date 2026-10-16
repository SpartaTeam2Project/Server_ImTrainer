# imtrainer-server

ImTrainer 게임 서버입니다. 회원 인증(JWT), 마스터데이터(몬스터·트레이닝 등 고정 데이터) 동기화, 플레이데이터 저장과 구매 검증을 담당합니다.

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
├── auth              # 인증
│   ├── controller    # AuthController (/auth/*)
│   ├── dto           # 로그인, 가입, 토큰 요청/응답
│   ├── entity        # User, RefreshToken
│   ├── repository
│   ├── security      # JWT 발급/검증, 인증 인터셉터, @AuthUser, 로그인 시도 제한
│   └── service       # AuthService, 만료 토큰 정리
├── master            # 마스터데이터
│   ├── controller    # MasterDataController (/master-data)
│   ├── dto           # 테이블별 JSON 구조 (MonsterData, TrainingData 등)
│   ├── entity        # Monster, PlayerBase, Training, StorageMonster, Trainer
│   ├── repository
│   └── service       # MasterDataService, 테이블별 동기화(MasterTable 상속: MonsterTable 등)
├── player            # 플레이데이터
│   ├── controller    # PlayerController (/player/*, /training/*)
│   ├── dto           # PlayerData, 쓰기 요청, 409 응답
│   ├── entity        # Player
│   ├── repository
│   └── service       # PlayerService (조회, 구매 검증), CurrencyService (재화 지급·차감)
├── common            # ErrorCode, ApiException, 글로벌 예외 처리, SHA-256
└── config            # Swagger, 비밀번호 암호화, 인터셉터·CORS 등록, 스케줄링
src/main/resources
├── application*.properties   # 공통 / local / dev / prod 설정
├── db/migration/V*.sql       # Flyway DB 스키마 (버전별)
├── masterdata/*.json         # 마스터데이터 원본 (테이블 이름 = 파일 이름)
└── messages.properties       # 에러·검증 메시지 문구
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
- 로그에 `마스터데이터 ... 준비 완료`(테이블 5개), `Started TrainerGameServerApplication`이 나오면 정상입니다.

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
| `CLIENT_IP_HEADER` | X | CloudFront·ALB·Nginx 뒤에 둘 때 `X-Forwarded-For`. 직접 받는 동안에는 비워 둠 |

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
| V3 | 마스터데이터 테이블 (`player_base`, `trainings`, `training_levels`, `storage_monsters`, `trainers`) |
| V4 | 플레이데이터 (`players`, `player_trainings`, `player_currencies`, `player_trainers`, `player_monsters`) |

## API

`/auth/signup`, `/auth/login`, `/auth/refresh`를 제외한 모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/auth/signup` | 회원가입 (아이디 4~20자, 비밀번호 8~20자) |
| POST | `/auth/login` | 로그인, accessToken·refreshToken 발급 |
| POST | `/auth/refresh` | 토큰 재발급. 사용한 refreshToken은 다시 쓸 수 없고 새 토큰 쌍 발급 |
| POST | `/auth/logout` | refreshToken 폐기 |
| POST | `/master-data` | 마스터데이터 해시 비교 후 바뀐 테이블만 반환 |
| POST | `/player/load` | 플레이데이터 조회 (본문 없음) |
| POST | `/player/intro` | 인트로 완료 (계정당 1회) |
| POST | `/player/trainer/select` | 트레이너 선택 |
| POST | `/training/purchase` | 트레이닝 한 레벨 구매 |
| POST | `/training/reset` | 트레이닝 초기화 (전액 환불) |

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
- 횟수는 서버 메모리에 저장합니다. 재시작하면 초기화되고, 서버를 여러 대로 늘리면 서버별로 셉니다.
- 클라이언트 IP는 `ClientIpResolver`가 정합니다. `CLIENT_IP_HEADER`가 비어 있으면 접속한 IP, `X-Forwarded-For`로 두면 그 헤더의 마지막 값(프록시가 붙인 실제 IP)을 씁니다. 앞쪽 값은 클라이언트가 위조할 수 있어 쓰지 않습니다.

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

모든 에러는 `{ "code": "...", "message": "..." }` 형식입니다. 입력값 오류가 여러 개면 요청 필드 순서(예: 아이디 → 비밀번호), 같은 필드는 미입력 → 길이 → 범위 순으로 하나만 내려갑니다.

| code | HTTP | 상황 |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | 입력값 오류, JSON 형식 오류 |
| `UNAUTHORIZED` | 401 | 토큰 없음/만료, 로그인 실패, refresh 실패 |
| `DUPLICATE_USERNAME` | 409 | 아이디 중복 |
| `MISMATCH` 등 | 409 | 플레이데이터 검증 실패. 응답에 `playerData` 포함 ([플레이데이터](#플레이데이터) 참고) |
| `TOO_MANY_REQUESTS` | 429 | 로그인 시도 제한. `Retry-After` 헤더(초) 뒤에 재시도 |
| `NOT_FOUND` / `METHOD_NOT_ALLOWED` / `UNSUPPORTED_MEDIA_TYPE` | 404 / 405 / 415 | 잘못된 경로, 메서드, Content-Type |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

**코드와 메시지 관리**

- 에러 코드와 HTTP 상태: `common/ErrorCode.java` (enum). 응답의 `code`는 이 이름 그대로입니다.
- 메시지 문구: `src/main/resources/messages.properties`. 코드에는 문구를 쓰지 않고 키만 씁니다.
  - 서비스 예외: `throw new ApiException(ErrorCode.MAX_LEVEL)` → `error.MAX_LEVEL`
  - 상세가 있는 예외: `new ApiException(ErrorCode.MISMATCH, "cost", 650)` → `error.MISMATCH.cost`의 `{0}`에 650
  - 요청 DTO 검증: `@NotBlank(message = "{validation.username.required}")`
- 문구만 바꿀 때는 `messages.properties`만 고치면 됩니다. 새 에러는 `ErrorCode`에 추가하고 `error.{코드}` 메시지를 넣습니다. (메시지가 없는 코드가 있으면 `ErrorMessageTest`가 실패)
- 다국어가 필요하면 `messages_en.properties`처럼 추가하면 요청의 `Accept-Language`에 따라 바뀝니다.

## 마스터데이터

### 서버 동작

1. 서버 시작 시 `resources/masterdata/*.json`을 DB 테이블에 반영합니다. (파일이 원본, DB는 파일 내용을 따라감)
2. DB에서 다시 읽어 테이블별 JSON 문자열과 SHA-256 해시를 만들어 메모리에 둡니다.
3. 클라이언트가 보낸 해시와 다른 테이블만 내려줍니다.

| 테이블 이름 (= 파일, 요청 key) | DB 테이블 | 내용 |
| --- | --- | --- |
| `monster` | `monsters` | 스테이지 몬스터 |
| `player_base` | `player_base` (행 1개) | 플레이어 기본 스탯 |
| `training` | `trainings`, `training_levels` | 트레이닝과 레벨별 값·비용 |
| `storage_monster` | `storage_monsters` | 보유 가능 포켓몬 |
| `trainer` | `trainers` | 트레이너 |

- JSON 필드 `key`, `row`, `column`은 MySQL 예약어라 DB 컬럼명만 `training_key`, `row_pos`, `column_pos`, `trainer_key`로 바꿨습니다. 내려가는 JSON은 파일과 같습니다.
- 소수는 끝자리 0을 뺀 형태로 내려갑니다. (파일의 `1.0` → `1`, `2.50` → `2.5`) 값은 같습니다.
- 응답의 테이블 순서는 이름순입니다.

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

- 값 수정: `masterdata/*.json`을 교체하고 재배포합니다. DB를 직접 고쳐도 재시작하면 파일 내용으로 덮어써집니다.
- 소수 값은 소수점 셋째 자리까지만 허용합니다. (DB `DECIMAL(10,3)`, 넘으면 서버 시작 실패)
- 파일에 없는 필드가 있거나, 필수 값이 비었거나, id가 중복되면 서버가 시작되지 않습니다. (training은 level이 1부터 빈칸 없이 이어져야 함)
- 새 테이블 추가: `masterdata/{이름}.json`, DTO, 엔티티, 리포지토리, `MasterTable`을 상속한 `@Component`(예: `TrainerTable`), 테이블을 만드는 Flyway 마이그레이션(`V{번호}__*.sql`)이 필요합니다. `MasterDataService`는 고칠 필요 없습니다.

## 플레이데이터

계정마다 다른 진행 상태(닉네임, 인트로 선택, 트레이닝 레벨, 재화, 보유 트레이너·포켓몬)입니다.
`players` 행은 계정이 처음 `/player/*`, `/training/*`를 호출할 때 만들어집니다. (기존 계정도 같음)

### 응답 `PlayerData`

```json
{
  "nickname": "레드",
  "intro": { "done": true, "gender": 0, "generation": 1, "starterMonsterId": 1 },
  "selectedTrainerId": 1,
  "trainings": [ { "trainingId": 6, "level": 2 } ],
  "currencies": [ { "currencyId": "pocket_dollar", "amount": 1200 } ],
  "ownedTrainerIds": [1],
  "ownedMonsterIds": [1]
}
```

- 신규 계정: `nickname` = 가입 아이디, `intro.done=false`(나머지 0), `selectedTrainerId=0`, `trainings`·`ownedTrainerIds`·`ownedMonsterIds`는 빈 배열, `currencies`는 `[{"currencyId":"pocket_dollar","amount":0}]`
- `currencies`에는 신규 계정이어도 **항상 `pocket_dollar`가 들어 있습니다.** (빈 배열로 오지 않음)
- 목록은 모두 id 오름차순. `trainings`에는 산 트레이닝(레벨 1 이상)만 들어갑니다.

### 쓰기 요청 (클라 계산 → 서버 검증)

| 경로 | 본문 | 서버 검증 |
| --- | --- | --- |
| `POST /player/intro` | `{nickname, gender, generation, starterMonsterId, trainerId}` | 인트로 1회만. `starterMonsterId`가 `storage_monster`에 있는지. `unlockedByIntro=true`이고 세대·성별이 맞는 트레이너를 모두 지급하고, `trainerId`는 그중 하나여야 함 (선택됨) |
| `POST /training/purchase` | `{trainingId, toLevel, cost, currencyId, balanceAfter}` | `currencyId`=`pocket_dollar`, 최대 레벨 아님, `toLevel`=현재+1, 해금 조건, `cost`=`levels[toLevel-1].cost`, 잔액 ≥ cost, `balanceAfter`=잔액−cost |
| `POST /training/reset` | `{refund, balanceAfter}` | `refund`=산 레벨 비용의 합(현재 마스터데이터 기준), `balanceAfter`=잔액+refund |
| `POST /player/trainer/select` | `{trainerId}` | 보유한 트레이너인지 |

- 성공 `200`: 갱신된 `PlayerData` 전체
- 검증 실패 `409`: `{code, message, playerData}`. 클라는 `playerData`로 덮어씁니다(롤백). 서버 데이터는 바뀌지 않습니다.
- 입력 형식 오류 `400 INVALID_REQUEST`: 필수 값 누락 등. `playerData` 없음
- 해금 조건: `requiredCategoryLevel`이 0이거나 `requiredCategory`의 카테고리 레벨(그 카테고리 트레이닝 레벨 합) ≥ `requiredCategoryLevel`, 그리고 `requiredTrainingId`가 -1이거나 그 트레이닝 레벨 ≥ `requiredTrainingLevel`
- 같은 계정의 쓰기 요청은 `players` 행 잠금(`SELECT ... FOR UPDATE`)으로 하나씩 처리합니다. 동시에 두 번 사도 잔액이 꼬이지 않습니다.

| code (409) | 의미 |
| --- | --- |
| `MISMATCH` | 클라 계산값(cost, refund, balanceAfter, toLevel, currencyId)이 서버와 다름. 반복되면 `/master-data`를 다시 받기 |
| `NOT_ENOUGH_CURRENCY` | 잔액 부족 |
| `REQUIREMENT_NOT_MET` | 트레이닝 해금 조건 미충족, 인트로에서 받을 수 없는 트레이너 |
| `MAX_LEVEL` | 이미 최대 레벨 |
| `NOT_OWNED` | 보유하지 않은 트레이너 선택 |
| `INTRO_ALREADY_DONE` | 인트로를 이미 완료함 |
| `UNKNOWN_ID` | 마스터데이터에 없는 트레이닝·포켓몬 id |

### 재화 지급·차감 (`CurrencyService`)

재화를 바꾸는 코드는 모두 `player/service/CurrencyService`를 거칩니다. 스테이지 결과처럼 재화를 주는 기능은 이걸 호출합니다.

```java
@Transactional   // 호출하는 쪽에서 트랜잭션을 열어야 함 (없으면 IllegalTransactionStateException)
public void saveStageResult(String username, StageResultRequest request) {
    Long userId = userRepository.findByUsername(username).orElseThrow().getId();  // username은 컨트롤러의 @AuthUser
    // ... runId 중복 확인, 결과 저장 ...
    long balance = currencyService.grant(userId, CurrencyService.POCKET_DOLLAR, deposited);
}
```

| 메서드 | 동작 |
| --- | --- |
| `grant(userId, currencyId, amount)` | 잔액 += amount, 변경 후 잔액 반환. 플레이데이터가 없는 계정이면 먼저 만듦 |
| `spend(userId, currencyId, amount)` | 잔액 < amount면 409 `NOT_ENOUGH_CURRENCY`(playerData 포함), 아니면 잔액 -= amount 후 반환 |
| `balance(userId, currencyId)` | 현재 잔액 (잠그지 않음) |

- `grant`, `spend`는 `Propagation.MANDATORY`입니다. 재화 변경과 그 원인(결과 저장, 레벨 변경)을 한 트랜잭션으로 묶어 한쪽만 저장되지 않게 하기 위해서입니다. 예외가 나면 둘 다 롤백됩니다.
- 사용자는 토큰으로 식별합니다. 요청 본문에 계정 id를 받지 않고, 컨트롤러의 `@AuthUser String username`으로 userId를 찾아 넘깁니다.
- 잠금은 트레이닝 구매와 같은 `players` 행 잠금입니다. 같은 계정의 구매·지급이 동시에 와도 순서대로 처리됩니다. 재화 행을 따로 잠그지 않습니다(잠금이 두 종류면 데드락 위험).
- amount는 0 이상만 받습니다.

### 아직 없는 것

- **재화 획득**: 스테이지 결과 API(위 `CurrencyService.grant` 사용 예정)가 아직 없어 `pocket_dollar`를 얻을 방법이 없습니다. 테스트할 때는 그 계정으로 `/player/load`를 한 번 호출한 뒤(`players` 행 생성) DB `player_currencies`에 직접 넣습니다.
  ```sql
  INSERT INTO player_currencies (user_id, currency_id, amount)
  SELECT id, 'pocket_dollar', 10000 FROM users WHERE username = '아이디'
  ON DUPLICATE KEY UPDATE amount = 10000;
  ```
- 스타터 외 포켓몬 해금, 트레이너 해금 규칙 (명세 미정)

## 배포 (AWS)

| 구성 | 사용 |
| --- | --- |
| 서버 | EC2 (ARM), Docker 컨테이너로 실행 |
| DB | RDS (MySQL 8) |
| 이미지 저장소 | ECR |
| 환경 변수 | SSM Parameter Store `/imtrainer/dev/*` (목록은 `deploy/app.env.example`) |
| 배포 명령 | SSM Run Command (SSH 사용 안 함) |
| CI/CD | GitHub Actions (`.github/workflows/deploy.yml`) |
| AWS 인증 | GitHub OIDC로 IAM Role 수임 (Access Key 사용 안 함) |

```
git push (YJ_2) → 테스트(MySQL) → bootJar → Docker 이미지(amd64·arm64) → ECR push
→ SSM으로 EC2에 배포 명령 → Parameter Store 값으로 컨테이너 교체 → 헬스 체크(/actuator/health)
```

- 배포는 `YJ_2` 브랜치 push에서만 실행됩니다. `dev`로 PR을 열면 테스트만 실행합니다.
- GitHub Secrets: `AWS_ROLE_ARN`, `ECR_REPOSITORY`, `EC2_INSTANCE_ID`
- 서버 앞에 HTTPS용 프록시(CloudFront, ALB, Nginx 등)를 두면 Parameter Store에 `CLIENT_IP_HEADER=X-Forwarded-For`를 추가합니다. 없으면 로그인 시도 제한이 모든 사용자를 프록시 IP 하나로 보고 함께 차단합니다.

## 라운드 / 리더보드

진행 중인 라운드의 시작 시각은 **Redis**(`round:{username}`, TTL 2시간)에, 리더보드는 MySQL `leaderboard` 테이블(V5)에 저장합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/rounds/start` | 본문 없음. 서버 시각을 시작 시간으로 기록. 진행 중인 라운드가 있으면 포기 처리 후 새로 시작 (201) |
| POST | `/rounds/end` | 결과 검증 후 리더보드 기록 (200). 성공/실패와 관계없이 라운드 종료 |
| GET | `/leaderboard?size=10` | 트레이너별 최고 기록 랭킹 (size 1~100) |

| code | HTTP | 상황 |
| --- | --- | --- |
| `ROUND_NOT_FOUND` | 404 | 진행 중인 라운드가 없거나 만료(2시간) |
| `INVALID_TIME` / `INVALID_BOSS` / `INVALID_KILL` / `INVALID_EXP` / `INVALID_LEVEL` / `INVALID_MONSTERBALL` / `INVALID_ENTRY` | 400 | 결과 검증 실패 (메시지는 모두 동일) |

### 환경 변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `REDIS_HOST` | 배포 시 O | 기본값 `localhost`. 컨테이너 안에서 localhost는 컨테이너 자신이므로 배포 환경에서는 반드시 지정 |
| `REDIS_PORT` | X | 기본값 `6379` |

로컬 실행: `docker run -d --name imtrainer-redis -p 6379:6379 redis:7`

> Redis에 연결할 수 없으면 `/actuator/health`가 DOWN(503)이 되어 배포 헬스 체크가 실패합니다. 배포 전에 Redis를 준비해 주세요.
