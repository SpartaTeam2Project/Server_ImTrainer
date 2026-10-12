# imtrainer-server

ImTrainer 게임 서버입니다. 회원 인증(JWT)과 마스터데이터(몬스터 등 고정 데이터) 동기화를 담당합니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어 / 프레임워크 | Java 21, Spring Boot 4.1.1 |
| DB | MySQL 8 (로컬: Docker, 배포: AWS RDS), Spring Data JPA |
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
└── masterdata/monster.json   # 몬스터 마스터데이터 원본
```

## 실행 방법 (로컬)

### 환경 변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `JWT_SECRET` | O | Base64로 인코딩한 32바이트 이상 랜덤 값 |
| `DB_PASSWORD` | X | local 기본값은 `root` |

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
| `local` | localhost:3306 (`root` / `root`) | 기본 프로필, `ddl-auto=update` |
| `dev` | AWS RDS (`DB_USERNAME`, `DB_PASSWORD`) | `ddl-auto=update` |
| `prod` | 환경 변수로 지정 (`SPRING_DATASOURCE_URL` 등) | `ddl-auto=validate`, Swagger 비활성화 |

prod는 테이블을 자동으로 만들지 않으므로 `users`, `refresh_tokens`, `monsters` 테이블을 미리 생성해야 합니다.

## API

`/auth/signup`, `/auth/login`, `/auth/refresh`를 제외한 모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/auth/signup` | 회원가입 (아이디 4~20자, 비밀번호 8~20자) |
| POST | `/auth/login` | 로그인, accessToken·refreshToken 발급 |
| POST | `/auth/refresh` | 토큰 재발급. 사용한 refreshToken은 폐기되고 새 토큰 쌍 발급 |
| POST | `/auth/logout` | refreshToken 폐기 |
| POST | `/master-data` | 마스터데이터 해시 비교 후 바뀐 테이블만 반환 |

- 토큰 유효시간: access 30분, refresh 14일
- 같은 refreshToken으로 동시에 요청하면 하나만 성공하고 나머지는 401
- refreshToken은 DB에 SHA-256 해시로 저장, 만료 토큰은 매일 04:00 삭제

### 에러 응답

모든 에러는 `{ "code": "...", "message": "..." }` 형식입니다. 입력값 오류가 여러 개면 아이디 → 비밀번호, 미입력 → 길이 순으로 하나만 내려갑니다.

| code | HTTP | 상황 |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | 입력값 오류, JSON 형식 오류 |
| `UNAUTHORIZED` | 401 | 토큰 없음/만료, 로그인 실패, refresh 실패 |
| `DUPLICATE_USERNAME` | 409 | 아이디 중복 |
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
- 새 테이블 추가: 엔티티, 리포지토리, DTO, `MasterDataService` 등록이 필요합니다.

## CI/CD

`.github/workflows/deploy.yml`

```
git push (YJ_2) → 빌드(bootJar) → Docker 이미지 생성 → AWS ECR push → SSM으로 EC2에 배포 명령 → 컨테이너 교체
```

- 현재는 `YJ_2` 브랜치 push에서만 실행됩니다.
- 팀 합의 후 `on.push.branches`에 `dev`를 추가하면 dev push에서도 같은 흐름으로 배포됩니다.
- 이미지는 `linux/amd64`, `linux/arm64` 두 아키텍처용으로 만듭니다. (dev EC2가 ARM(aarch64) 인스턴스)
- 배포 단계는 EC2에서 IAM Role로 계정 번호를 조회해 이미지 주소를 만듭니다. (이미지 주소에 Secret이 포함돼 job 사이로 넘길 수 없기 때문)
- 테스트(`contextLoads`)는 DB와 `JWT_SECRET`이 필요해 CI에서 제외(`-x test`)했습니다.

### GitHub Secrets

| 이름 | 용도 |
| --- | --- |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | ECR push, SSM 명령 전송용 IAM 사용자 키 |
| `ECR_REPOSITORY` | ECR 저장소 이름 |
| `EC2_INSTANCE_ID` | 배포할 EC2 인스턴스 ID (`i-...`) |

SSH 키는 사용하지 않습니다. 배포 명령은 AWS SSM(Run Command)으로 전송합니다.

### IAM 사용자 권한 (GitHub Actions용)

- `AmazonEC2ContainerRegistryPowerUser` (ECR push)
- SSM 명령 전송: `ssm:SendCommand`, `ssm:GetCommandInvocation`

### EC2 준비

- Docker, AWS CLI 설치 (`sudo dnf install -y docker`, `sudo systemctl enable --now docker`)
- EC2 IAM Role에 `AmazonEC2ContainerRegistryReadOnly`, `AmazonSSMManagedInstanceCore` 정책 연결
- Systems Manager → Fleet Manager에서 인스턴스가 Online인지 확인 (Amazon Linux 2023은 SSM Agent 기본 설치)
- 환경변수 파일 `/etc/imtrainer/env` (root 소유, `chmod 600`, 형식은 `deploy/app.env.example` 참고, 값에 따옴표 금지)
- 기존 jar 실행용 systemd 서비스는 `sudo systemctl disable --now imtrainer`로 중지 (8080 포트 비우기)
- 배포에 SSH(22) 포트는 필요 없음
- 서버는 EC2 인스턴스가 ARM(aarch64)이므로 arm64 이미지로 실행됨
