# banking.system-backend.api

🏦 뱅킹 서버 api

사용자 · 계좌 · 거래를 담당하는 REST API 서버입니다.
스케줄러 서버([banking.system-backend.scheduler](https://github.com/hyunolike/banking.system-backend.scheduler))가
내부 API 로 현황 스냅샷을 수집해 갑니다.

| 항목 | 값 |
|---|---|
| 언어 / 런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.0.3 (Web, Data JPA, Security, Validation) |
| DB | Oracle (운영) / H2 (테스트·로컬) |
| 인증 | JWT (HS256) + BCrypt |
| 포트 | 8182 |

---

## 1. 아키텍처

```
com.banking_system.api_server
├── common
│   ├── config      SecurityConfig
│   ├── security    JwtTokenProvider, JwtAuthenticationFilter, InternalApiKeyFilter
│   └── error       ErrorCode, BusinessException, GlobalExceptionHandler
├── user
│   ├── command     domain(User, Password) / application(UserService)
│   └── ui          AuthController, UserController
├── account
│   ├── command     domain(Account, AccountTransaction) / application(AccountService)
│   └── ui          AccountController
├── friend
│   ├── command     domain(Friend) / application(FriendService)
│   └── ui          FriendController
└── stats
    ├── query       StatsQueryService, SystemSnapshot
    └── ui          InternalStatsController   ← 스케줄러 전용
```

- 도메인별 `command` / `query` 분리를 유지하되, **한 테이블에는 엔티티를 하나만** 둡니다.
  조회 결과는 엔티티가 아니라 DTO 로 나갑니다.
- 잔액 변경은 전부 `Account` 의 도메인 메서드(`deposit` / `withdraw`)를 통해서만 일어납니다.
- 이체는 계좌번호가 작은 쪽부터 잠그는 **비관적 락**으로 처리해 데드락을 피합니다.
- 모든 잔액/금액은 `BigDecimal(19, 4)` 입니다.

## 2. API

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | – | 회원가입 |
| POST | `/api/auth/login` | – | 로그인, 액세스 토큰 발급 |
| GET | `/api/users/me` | Bearer | 내 정보 |
| POST | `/api/accounts` | Bearer | 계좌 개설 |
| GET | `/api/accounts` | Bearer | 내 계좌 목록 |
| GET | `/api/accounts/{no}` | Bearer | 계좌 상세 |
| POST | `/api/accounts/{no}/deposits` | Bearer | 입금 |
| POST | `/api/accounts/{no}/withdrawals` | Bearer | 출금 |
| POST | `/api/accounts/{no}/transfers` | Bearer | 이체 |
| GET | `/api/accounts/{no}/transactions` | Bearer | 거래내역 (`page`, `size`) |
| GET | `/api/friends` · POST · DELETE `/{id}` | Bearer | 이체 즐겨찾기 |
| GET | `/api/internal/stats/snapshot` | `X-Internal-Api-Key` | 현황 스냅샷 (스케줄러 전용) |

에러 응답은 모두 같은 형태입니다.

```json
{
  "code": "A003",
  "message": "잔액이 부족합니다.",
  "errors": [],
  "timestamp": "2024-05-01T10:15:30"
}
```

### 사용 예

```bash
# 회원가입
curl -X POST localhost:8182/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"name":"장현호","email":"hyunho@example.com","password":"password1234"}'

# 로그인 → accessToken
TOKEN=$(curl -s -X POST localhost:8182/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"hyunho@example.com","password":"password1234"}' | jq -r .accessToken)

# 계좌 개설 후 이체
curl -X POST localhost:8182/api/accounts -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"월급통장","initialBalance":100000}'

curl -X POST localhost:8182/api/accounts/1/transfers -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"toAccountNumber":2,"amount":5000,"description":"용돈"}'
```

## 3. 설정 (환경변수)

자격증명은 **저장소에 두지 않습니다.** 배포 환경의 `.env` 또는 환경변수로 주입합니다.

| 변수 | 필수 | 설명 |
|---|:---:|---|
| `DB_URL` | ✅ | `jdbc:oracle:thin:@호스트:1521:xe` |
| `DB_USERNAME` | ✅ | DB 계정 |
| `DB_PASSWORD` | ✅ | DB 비밀번호 |
| `JWT_SECRET` | ✅ | HS256 서명 키, **32바이트 이상** |
| `INTERNAL_API_KEY` | ✅ | 스케줄러 서버와 공유하는 내부 API 키 |
| `JWT_EXPIRATION` | | 토큰 만료 (기본 `1h`) |
| `SERVER_PORT` | | 기본 `8182` |
| `LOG_LEVEL` | | 기본 `info` |

키 생성 예시:

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -hex 32      # INTERNAL_API_KEY
```

## 4. 로컬 실행 / 테스트

```bash
cd api_server

# H2 인메모리로 바로 기동 (환경변수 불필요)
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트 (H2, 외부 DB 불필요)
./gradlew test
```

## 5. DB 스키마

| 파일 | 용도 |
|---|---|
| `src/main/resources/db/init/01-create-schema-owner.sql` | 스키마 소유 계정 생성 (DBA 1회 실행) |
| `src/main/resources/schema.sql` | 신규 설치용 전체 DDL |
| `src/main/resources/db/migration/V2__security_and_ledger.sql` | 기존 DB 업그레이드용 |

`spring.jpa.hibernate.ddl-auto` 는 `none` 입니다. DDL 은 위 스크립트로만 적용합니다.

> ⚠️ **V2 마이그레이션 주의**
> 기존 비밀번호는 평문으로 저장돼 있었고 BCrypt 로 역변환할 수 없습니다.
> 마이그레이션 스크립트가 기존 해시를 로그인 불가 값으로 바꾸므로,
> 적용 전에 전체 사용자에게 비밀번호 재설정을 안내해야 합니다.

## 6. 배포

`develop` 에 push 되면 GitHub Actions 가 빌드 → 테스트 → NCP 서버 배포까지 수행합니다.
Pull Request 에서는 빌드·테스트만 돌고 **배포는 실행되지 않습니다.**

서버 준비 사항:

```
/home/<user>/api_server/
├── run.sh          # CI 가 jar 와 함께 업로드
├── .env            # 위 환경변수 (chmod 600, 저장소에 없음)
└── api_server-*.jar
```

필요한 GitHub Secrets: `NCP_HOST`, `NCP_USERNAME`, `NCP_PASS`, `NCP_PORT`

> 💡 `NCP_PASS` 대신 SSH 키(`NCP_SSH_KEY`)를 쓰는 편이 안전합니다.
> 기존 배포 설정을 깨뜨리지 않으려고 비밀번호 인증을 유지해 두었습니다.
