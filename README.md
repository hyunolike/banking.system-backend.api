# 🏦 banking.system-backend.api

> 사용자 · 계좌 · 거래를 담당하는 뱅킹 REST API 서버

<br>

## 📌 프로젝트 개요

두 개의 서버로 구성된 뱅킹 시스템의 **API 서버**입니다.
회원가입부터 계좌 개설, 입출금, 이체, 거래내역 조회까지의 흐름을 담당합니다.

```
┌────────────────────────┐         ┌────────────────────────┐
│   scheduler server     │         │      api server        │
│        :8181           │────────▶│        :8182           │──────▶ Oracle
│  현황 스냅샷 주기 수집   │ 내부 API │  인증 · 계좌 · 거래     │
└────────────────────────┘   키    └────────────────────────┘
```

| | |
|---|---|
| **스케줄러 서버** | [banking.system-backend.scheduler](https://github.com/hyunolike/banking.system-backend.scheduler) |
| **포트** | `8182` |
| **인증** | JWT (HS256) + BCrypt |

<br>

## 🛠 기술 스택

| 구분 | 기술 |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.0.3 |
| **Web** | Spring Web MVC, Bean Validation |
| **Persistence** | Spring Data JPA, Hibernate |
| **Security** | Spring Security, JJWT 0.11.5, BCrypt |
| **Database** | Oracle (운영) · H2 (로컬 · 테스트) |
| **Build** | Gradle 7.6.1 |
| **Test** | JUnit 5, AssertJ, MockMvc, Spring Security Test |
| **CI/CD** | GitHub Actions → NCP |

<br>

## 🚀 기능 목록

### 인증 · 회원

- [x] 회원가입
  - [x] 이메일 중복 검사 (대소문자 · 공백 정규화)
  - [x] 동시 가입 경합은 UNIQUE 제약으로 최종 차단
  - [x] 비밀번호 BCrypt 해싱 — 평문은 어디에도 남기지 않는다
- [x] 로그인
  - [x] 액세스 토큰(JWT) 발급
  - [x] 이메일 존재 여부를 응답으로 구분할 수 없게 처리
  - [x] 연속 실패 5회 시 계정 15분 잠금
  - [x] 로그인 성공 시 실패 카운터 초기화
- [x] 내 정보 조회 — 토큰 주인 본인만
- [x] 비밀번호 변경 — 현재 비밀번호 확인, 동일 비밀번호 재사용 차단
- [x] 비밀번호 재설정
  - [x] 운영자가 일회용 토큰 발급 (내부 API)
  - [x] 토큰 원문은 발급 응답에서만 노출, DB 에는 SHA-256 해시만 저장
  - [x] 1회용 · 재발급 시 이전 토큰 즉시 무효 · TTL 30분
  - [x] 재설정 시 계정 잠금도 함께 해제

### 계좌

- [x] 계좌 개설 — 최초 입금액은 원장에 기록
- [x] 내 계좌 목록 / 상세 조회
- [x] 입금 · 출금
  - [x] 0 이하 금액 거절
  - [x] 소수점 4자리 초과 금액은 반올림하지 않고 거절
  - [x] 잔액 부족 시 출금 거절
- [x] 계좌 이체
  - [x] 본인 소유 계좌에서만 출금
  - [x] 동일 계좌 간 이체 차단
  - [x] 비관적 락으로 동시 이체 시 갱신 손실 방지
  - [x] 계좌번호 오름차순 락 획득으로 데드락 회피
- [x] 거래내역 조회 (페이징) — 입금 / 출금 / 이체입금 / 이체출금

### 이체 즐겨찾기

- [x] 즐겨찾기 등록 / 목록 / 삭제
- [x] 본인 소유 항목만 삭제 가능

### 운영 · 내부

- [x] 시스템 현황 스냅샷 — 가입자 수 · 계좌 수 · 총 잔액 (스케줄러 전용)
- [x] 재설정 토큰 발급 (운영자 전용)
- [x] 내부 API 는 `X-Internal-Api-Key` 로 분리 인증 — 사용자 토큰으로 접근 불가

<br>

## 🎯 설계 원칙

- **한 테이블에는 엔티티 하나만 둔다.** 조회 결과는 엔티티가 아니라 DTO 로 내보낸다.
- **잔액 변경은 전부 `Account` 의 도메인 메서드를 통한다.** 서비스가 잔액 필드를 직접 만지지 않는다.
- **애그리거트는 ID 로만 참조한다.** 계좌는 `account.account_id` 로 사용자를 가리킬 뿐 객체 참조를 갖지 않는다.
- **금액은 `BigDecimal(19, 4)`.** 부동소수 오차가 들어올 자리를 두지 않는다.
- **스키마는 SQL 스크립트로만 관리한다.** `ddl-auto: none`.
- **자격증명은 저장소에 두지 않는다.** 전부 환경변수로 주입한다.

<br>

## 🏗 아키텍처

```
com.banking_system.api_server
├── common
│   ├── config       SecurityConfig (필터체인 · CORS · PasswordEncoder)
│   ├── security     JwtTokenProvider, JwtAuthenticationFilter,
│   │                InternalApiKeyFilter, 정책 프로퍼티
│   └── error        ErrorCode, BusinessException, GlobalExceptionHandler
├── user
│   ├── command      domain(User, Password, PasswordResetToken)
│   │                application(UserService, LoginAttemptService)
│   └── ui           AuthController, UserController, InternalUserController
├── account
│   ├── command      domain(Account, AccountTransaction)
│   │                application(AccountService)
│   └── ui           AccountController
├── friend
│   ├── command      domain(Friend) / application(FriendService)
│   └── ui           FriendController
└── stats
    ├── query        StatsQueryService, SystemSnapshot
    └── ui           InternalStatsController   ← 스케줄러 전용
```

### 접근 통제

| 경로 | 인증 방식 |
|---|---|
| `/api/auth/**` | 없음 (회원가입 · 로그인 · 비밀번호 재설정) |
| `/api/internal/**` | `X-Internal-Api-Key` 헤더 |
| 그 외 전부 | `Authorization: Bearer <JWT>` |

### 도메인 모델

```
member ──1:N──▶ account ──1:N──▶ account_tx
  │                                (거래 원장)
  ├──1:N──▶ friend
  └──1:N──▶ password_reset_token
```

<br>

## 📋 API 명세

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `POST` | `/api/auth/signup` | – | 회원가입 |
| `POST` | `/api/auth/login` | – | 로그인, 액세스 토큰 발급 |
| `POST` | `/api/auth/password-reset` | – | 일회용 토큰으로 비밀번호 재설정 |
| `GET` | `/api/users/me` | Bearer | 내 정보 |
| `PATCH` | `/api/users/me/password` | Bearer | 비밀번호 변경 |
| `POST` | `/api/accounts` | Bearer | 계좌 개설 |
| `GET` | `/api/accounts` | Bearer | 내 계좌 목록 |
| `GET` | `/api/accounts/{no}` | Bearer | 계좌 상세 |
| `POST` | `/api/accounts/{no}/deposits` | Bearer | 입금 |
| `POST` | `/api/accounts/{no}/withdrawals` | Bearer | 출금 |
| `POST` | `/api/accounts/{no}/transfers` | Bearer | 이체 |
| `GET` | `/api/accounts/{no}/transactions` | Bearer | 거래내역 (`page`, `size`) |
| `GET` `POST` `DELETE` | `/api/friends` · `/{id}` | Bearer | 이체 즐겨찾기 |
| `GET` | `/api/internal/stats/snapshot` | 내부 키 | 현황 스냅샷 |
| `POST` | `/api/internal/users/password-reset-tokens` | 내부 키 | 재설정 토큰 발급 |

### 에러 응답

모든 에러는 같은 형태입니다. 클라이언트는 HTTP 상태가 아니라 `code` 로 분기합니다.

```json
{
  "code": "A003",
  "message": "잔액이 부족합니다.",
  "errors": [],
  "timestamp": "2024-05-01T10:15:30"
}
```

| 코드 | 상황 | 코드 | 상황 |
|---|---|---|---|
| `C001` | 요청 값 검증 실패 | `A001` | 존재하지 않는 계좌 |
| `C002` | 인증 필요 | `A002` | 본인 소유 계좌 아님 |
| `C003` | 접근 권한 없음 | `A003` | 잔액 부족 |
| `U002` | 이메일 중복 | `A004` | 잘못된 거래 금액 |
| `U003` | 로그인 실패 | `A005` | 동일 계좌 이체 |
| `U004` | 계정 잠김 | `U005` | 재설정 토큰 무효 |

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

<br>

## 💡 기술적 고민과 해결 과정

### 1. 인증 없이 열려 있던 전 테이블 CRUD

**문제** — `spring-boot-starter-data-rest` 와 `@RepositoryRestResource` 조합이
`/api/userDatas`, `/api/accountDatas` 를 **인증 없이 GET / POST / PATCH / DELETE 전부** 열고 있었습니다.
누구나 남의 계좌 잔액을 PATCH 할 수 있는 상태였고, 응답 JSON 에 평문 비밀번호까지 실려 나갔습니다.

**해결** — Spring Data REST 를 걷어내고 명시적 컨트롤러로 대체했습니다.
Spring Security 필터체인을 세워 기본 정책을 "인증 필요"로 두고, 공개 경로만 열었습니다.
소유권 검사는 서비스가 아니라 `Account.requireOwner()` 안에 넣어, 어느 경로로 들어와도 같은 규칙이 적용되게 했습니다.

### 2. 한 테이블에 엔티티가 두 벌

**문제** — `User`/`UserData`, `Account`/`AccountData` 가 각각 같은 테이블에 매핑돼 있었습니다.
게다가 매핑이 스키마와 맞지 않았습니다.

```java
// account_number 는 account 의 PK 인데 FK 처럼 매핑돼 있었다
@OneToMany @JoinColumn(name = "account_number")
private List<Account> accounts;

// 스키마에 존재하지 않는 컬럼
@Column(name = "account_name") private String name;
```

`ddl-auto: none` 이라 기동 시엔 통과하고 **조회 시점에 터지는** 형태였습니다.

**해결** — 테이블당 엔티티를 하나로 줄이고, 조회는 DTO 로 내보내는 방식으로 정리했습니다.
애그리거트 간에는 객체 참조 대신 ID 참조를 쓰기로 정해 잘못된 연관관계가 생길 자리를 없앴습니다.

### 3. 동시 이체에서 돈이 생기고 사라지는 문제

**문제** — 이체 로직에 락이 없으면 두 요청이 같은 잔액을 읽고 각자 계산한 값을 쓰면서 갱신이 유실됩니다.
뱅킹 도메인에서는 곧바로 정합성 사고입니다.

**해결** — 잔액 변경 경로만 `SELECT ... FOR UPDATE` 로 행을 잠그고,
**두 계좌를 잠글 때 항상 계좌번호가 작은 쪽부터** 잠그도록 순서를 고정했습니다.
A→B 와 B→A 가 동시에 들어와도 락 획득 순서가 같아 데드락이 나지 않습니다.

```java
List<Long> lockOrder = Stream.of(fromAccountNumber, toAccountNumber).sorted().toList();
Account first  = getAccountForUpdate(lockOrder.get(0));
Account second = getAccountForUpdate(lockOrder.get(1));
```

**검증** — 테스트를 쓰는 것으로 끝내지 않고, **락을 제거하면 실제로 깨지는지** 확인했습니다.

| 시나리오 | 락 있음 | 락 제거 시 |
|---|---|---|
| 잔액 300 에 8건 동시 출금 | 3건만 성공, 잔액 0 | ❌ 8건 전부 성공 |
| 동시 이체 80건 | 잔액 정확히 0 | ❌ 6,700원 남음 |
| 양방향 교차 이체 | 총액 10,000 보존 | ❌ **11,400으로 증가** |

락을 빼면 없던 돈이 생깁니다. 테스트가 실제로 락을 검증하고 있다는 근거입니다.

### 4. 로그인 실패 횟수가 계속 0으로 돌아오던 문제

**문제** — 로그인 실패를 기록한 직후 `LOGIN_FAILED` 예외를 던지는데,
같은 트랜잭션 안에 두면 예외로 롤백되면서 **방금 올린 실패 횟수까지 함께 사라집니다.**
아무리 틀려도 계정이 잠기지 않습니다.

**해결** — 실패 기록을 `REQUIRES_NEW` 전파 속성의 별도 트랜잭션으로 분리했습니다.
바깥 트랜잭션이 롤백돼도 실패 카운터는 독립적으로 커밋됩니다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordFailure(Long userId) { ... }
```

테스트에서 카운터가 `1 → 2 → (잠금과 함께) 0` 으로 움직이는지 직접 단언합니다.

### 5. 평문 비밀번호를 BCrypt 로 옮길 때의 딜레마

**문제** — 기존 비밀번호는 평문(`VARCHAR(20)`)으로 저장돼 있었습니다.
BCrypt 는 단방향이라 **기존 값을 해시로 변환할 방법이 없습니다.**
그렇다고 평문을 그대로 둘 수도 없습니다.

**해결** — 마이그레이션에서 기존 값을 로그인 불가 값으로 바꿔 평문을 제거하되,
**복구 경로를 함께 제공**했습니다. 메일 발송 수단이 없어 운영자 매개 방식으로 설계했습니다.

```
운영자 ──POST /api/internal/users/password-reset-tokens──▶ 일회용 토큰 발급
       ──본인 확인 후 별도 경로로 전달──▶ 사용자
사용자 ──POST /api/auth/password-reset──▶ 재설정 + 잠금 해제
```

토큰 원문은 발급 응답에서만 볼 수 있고 DB 에는 SHA-256 해시만 남습니다.
DB 가 유출돼도 토큰을 되돌릴 수 없습니다.

> 재설정 경로 없이 마이그레이션만 적용하면 기존 사용자가 **영구 잠깁니다.**
> `V2` 와 `V3` 를 반드시 함께 적용해야 하는 이유입니다.

### 6. 암호화했지만 암호화되지 않았던 설정

**문제** — `application.yml` 의 DB 접속 정보가 Jasypt `ENC(...)` 로 감싸여 있었지만,
**복호화 키가 소스에 그대로 하드코딩**돼 있었습니다.

```java
final String key = "hyunho";   // JasyptConfig.java
```

게다가 `main()` 이 기동할 때마다 평문 호스트 · 계정 · 비밀번호를 콘솔에 출력하고 있었습니다.
키와 암호문이 같은 저장소에 있으면 암호화한 의미가 없습니다.

**해결** — Jasypt 를 걷어내고 자격증명을 전부 환경변수로 옮겼습니다.
기동 시 출력 코드도 삭제했습니다. 다만 **git 히스토리에 남은 자격증명은 코드 수정으로 지워지지 않으므로,
해당 계정의 비밀번호 교체가 선행돼야 합니다.**

<br>

## ⚙️ 실행 방법

### 로컬 (H2 인메모리 — 환경변수 불필요)

```bash
cd api_server
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Docker Compose (Oracle 포함)

```bash
cat > .env <<'ENV'
ORACLE_PASSWORD=change-me
DB_USERNAME=hyunho
DB_PASSWORD=change-me
JWT_SECRET=<openssl rand -base64 48>
INTERNAL_API_KEY=<openssl rand -hex 32>
ENV

docker compose up -d --build
```

Oracle 컨테이너가 healthy 가 된 뒤 api 가 올라옵니다. 최초 1회 `schema.sql` 실행이 필요합니다.

### 환경변수

자격증명은 **저장소에 두지 않습니다.**

| 변수 | 필수 | 설명 |
|---|:---:|---|
| `DB_URL` | ✅ | `jdbc:oracle:thin:@호스트:1521:xe` |
| `DB_USERNAME` | ✅ | DB 계정 |
| `DB_PASSWORD` | ✅ | DB 비밀번호 |
| `JWT_SECRET` | ✅ | HS256 서명 키, **32바이트 이상** |
| `INTERNAL_API_KEY` | ✅ | 스케줄러 서버와 공유하는 내부 API 키 |
| `JWT_EXPIRATION` | | 토큰 만료 (기본 `1h`) |
| `LOGIN_MAX_ATTEMPTS` | | 연속 실패 허용 횟수 (기본 `5`) |
| `LOGIN_LOCK_DURATION` | | 잠금 시간 (기본 `15m`) |
| `PASSWORD_RESET_TTL` | | 재설정 토큰 유효 기간 (기본 `30m`) |
| `CORS_ALLOWED_ORIGINS` | | 쉼표 구분 오리진. **비우면 교차 출처 요청 전면 차단** |
| `SERVER_PORT` | | 기본 `8182` |
| `LOG_LEVEL` | | 기본 `info` |

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -hex 32      # INTERNAL_API_KEY
```

> ⚠️ 계정 잠금은 **계정 단위**라, 남의 이메일로 일부러 실패시켜 잠그는 공격이 가능합니다.
> `LOGIN_MAX_ATTEMPTS` 를 지나치게 낮게 잡지 마세요.

<br>

## 🗄 데이터베이스

| 파일 | 용도 |
|---|---|
| `db/init/01-create-schema-owner.sql` | 스키마 소유 계정 생성 (DBA 1회 실행) |
| `schema.sql` | 신규 설치용 전체 DDL |
| `db/migration/V2__security_and_ledger.sql` | 기존 DB 업그레이드 (보안 · 원장) |
| `db/migration/V3__password_reset_and_login_lock.sql` | 재설정 토큰 · 로그인 잠금 |

`ddl-auto` 는 `none` 입니다. DDL 은 위 스크립트로만 적용합니다.

> ⚠️ **마이그레이션 적용 순서**
> 1. 서비스 중단 또는 읽기 전용 전환
> 2. `V2` → `V3` **연속 적용** (V3 가 V2 로 잠긴 사용자의 유일한 복구 경로)
> 3. 전체 사용자에게 비밀번호 재설정 안내
> 4. 신규 애플리케이션 배포
>
> 복구 절차는 `V3` 스크립트 하단 주석에 `curl` 예시까지 정리해 두었습니다.

<br>

## 🧪 테스트

```bash
cd api_server && ./gradlew test
```

외부 Oracle 없이 H2 로 동작합니다. 총 **38건**.

| 테스트 | 건수 | 검증 내용 |
|---|:---:|---|
| `AccountTest` | 6 | 입출금 · 잔액 부족 · 금액 스케일 · 소유권 |
| `AccountServiceTest` | 6 | 이체 · 롤백 · 원장 기록 · 타인 계좌 차단 |
| `AccountConcurrencyTest` | 3 | 동시 이체 갱신 손실 · 데드락 · 음수 잔액 |
| `ApiSecurityTest` | 9 | 인증 · 인가 · CORS · 내부 API 키 · 검증 에러 |
| `LoginLockTest` | 4 | 실패 카운터 · 계정 잠금 · 재설정 시 해제 |
| `PasswordFlowTest` | 7 | 비밀번호 변경 · 재설정 토큰 1회성 · 재발급 무효화 |
| `PasswordTest` | 2 | BCrypt 해싱 · 평문 미노출 |

<br>

## 🚢 배포

`develop` 에 push 되면 GitHub Actions 가 빌드 → 테스트 → NCP 배포를 수행합니다.
**Pull Request 에서는 빌드 · 테스트만 돌고 배포는 실행되지 않습니다.**

```
/home/<user>/api_server/
├── run.sh          # CI 가 jar 와 함께 업로드
├── .env            # 환경변수 (chmod 600, 저장소에 없음)
└── api_server-*.jar
```

필요한 Secrets: `NCP_HOST` · `NCP_USERNAME` · `NCP_PASS` · `NCP_PORT`

> 💡 `NCP_PASS` 대신 SSH 키(`NCP_SSH_KEY`)가 안전합니다.
> 기존 배포 설정을 깨뜨리지 않으려고 비밀번호 인증을 유지해 두었습니다.

<br>

## 📝 남은 과제

- [ ] 리프레시 토큰 · 로그아웃 (현재 토큰 강제 무효화 불가)
- [ ] API 문서 자동화 (Swagger / Spring REST Docs)
- [ ] Flyway 도입 — 현재 마이그레이션은 수동 실행
- [ ] Oracle 실환경 검증 — 드라이버 · 방언 · 마이그레이션 SQL 은 H2 로만 확인
- [ ] Docker 이미지 빌드 검증
- [ ] 배포 SSH 키 인증 전환
