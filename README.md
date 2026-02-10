# 콘서트 예약 시스템 (Concert Reservation System)

대규모 트래픽 환경에서 안정적으로 동작하는 콘서트 좌석 예약 시스템입니다.  
대기열 관리, 동시성 제어, 이벤트 기반 아키텍처를 중심으로 설계되었습니다.

---

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Framework | Spring Boot | 3.5.10 |
| Language | Java | 17 |
| Database | PostgreSQL (운영) / H2 (테스트) | 16-alpine |
| Cache & Queue | Redis | 7.2-alpine |
| Build | Gradle | 8.14.4 |
| API 문서 | SpringDoc OpenAPI (Swagger UI) | 2.7.0 |
| 컨테이너 | Docker Compose | - |
| 기타 | Spring Retry, Spring AOP, Lettuce Pool | - |

---

## 프로젝트 구조

```
src/main/java/com/example/concert_reservation/
├── api/                          # Presentation Layer (Controller, UseCase, DTO)
│   ├── balance/                  # 잔액 충전/조회
│   ├── concert/                  # 콘서트 날짜/좌석 조회
│   ├── payment/                  # 결제 처리
│   ├── queue/                    # 대기열 토큰 발급/조회
│   ├── refund/                   # 환불 처리
│   └── reservation/              # 좌석 예약
├── config/                       # 설정 (Interceptor, WebMvc, OpenAPI)
├── domain/                       # Domain Layer (Model, Component, Repository)
│   ├── balance/                  # 잔액 도메인
│   ├── concert/                  # 콘서트/좌석 도메인
│   ├── payment/                  # 결제 도메인 + 이벤트
│   ├── queue/                    # 대기열 도메인 (Redis + JPA 이중 구현)
│   ├── refund/                   # 환불 도메인
│   └── reservation/              # 예약 도메인
└── support/                      # 횡단 관심사
    ├── common/                   # 공통 유틸리티
    ├── exception/                # 글로벌 예외 처리
    └── external/                 # 외부 연동 (DataPlatform, EventListener)
```

---

## 변경 이력 (Phase별)

### Phase 1: 기본 기능 구현

**초기 아키텍처**: Spring Boot + H2 + JPA

- 6개 도메인 설계 (Concert, Seat, Reservation, Payment, Balance, Queue)
- 8개 REST API 구현 (토큰 발급, 대기번호 조회, 날짜/좌석 조회, 예약, 결제, 충전, 환불)
- 단위 테스트 294개 작성 (100% 통과)
- Swagger UI를 통한 API 문서 자동 생성
- `QueueTokenInterceptor`를 통한 비인가 API 접근 차단

### Phase 2: 동시성 제어 도입

**문제**: 다수의 사용자가 동시에 같은 좌석을 예약하면 이중 예약이 발생  
**해결**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` 비관적 락 적용

- 좌석 예약: `SeatJpaRepository.findByIdWithLock()` — SELECT FOR UPDATE
- 잔액 관리: `BalanceJpaRepository.findByUserIdWithLock()` — SELECT FOR UPDATE
- 동시성 통합 테스트 작성: 10/100/1,000명 동시 접근 검증

### Phase 3: 대기열 시스템 Redis 이관

**문제**: JPA 기반 대기열의 한계
- 수만 명의 폴링 요청이 DB 커넥션 풀을 고갈시킴
- `ORDER BY timestamp` 정렬 비용이 매우 높음
- 만료 토큰 삭제를 위한 별도 배치 작업 필요

**해결**: Redis 자료구조로 대기열 전체 이관

| 자료구조 | Key 패턴 | 용도 |
|----------|---------|------|
| Sorted Set | `queue:waiting` | 대기열 순서 관리 (Score = 진입 시각) |
| Hash | `queue:active:{token}` | 활성 토큰 상태 저장 |
| Hash | `queue:token:{token}` | 토큰 메타데이터 (userId, status 등) |
| String | `user:active:{userId}` | 유저→활성 토큰 역방향 매핑 (O(1) 조회) |
| String | `user:waiting:{userId}` | 유저→대기 토큰 역방향 매핑 |

**최적화 포인트**:
- `KEYS` → `SCAN` 변경: Redis 싱글 스레드 블로킹 방지
- `Pipeline` 사용: 토큰 추가/활성화 시 네트워크 왕복 최소화
- `HGETALL` 사용: N+1 조회 문제 해결
- `TTL` 자동 만료: 활성 토큰 5분, 대기 토큰 30분 — Redis가 자동 정리

### Phase 4: 좌석 조회 캐싱 (Spring Cache + Redis)

**문제**: 인기 콘서트의 좌석 조회 요청이 대량 발생 → 매번 DB를 조회하면 불필요한 부하  
**해결**: Spring Cache Abstraction(`@Cacheable` / `@CacheEvict`) + `RedisCacheManager`(저장소)

- **구조**: `@Cacheable` 어노테이션(스프링 캐시 추상화) → `RedisCacheManager`(구현체) → Redis(저장소)
- **조회 캐싱**: `SeatManager.getSeatsByConcert()` → `@Cacheable(value = "seats", key = "#concertDateId")`
- **캐시 무효화**: 좌석 상태가 변경되는 모든 연산(예약/판매/해제)에 `@CacheEvict` 적용
- **TTL**: 10분 (`RedisConfig.cacheManager()`에서 `entryTtl(Duration.ofMinutes(10))` 설정)
- **직렬화**: `GenericJackson2JsonRedisSerializer`로 JSON 형태로 Redis에 저장

```java
// SeatManager.java — 캐싱 적용
@Cacheable(value = "seats", key = "#concertDateId")             // 조회 시 캐시 히트
public List<Seat> getSeatsByConcert(Long concertDateId) { ... }

@CacheEvict(value = "seats", key = "#seat.concertDateId")       // 예약 시 캐시 무효화
public Seat reserveSeat(Seat seat) { ... }

@CacheEvict(value = "seats", key = "#seat.concertDateId")       // 결제 시 캐시 무효화
public Seat sellSeat(Seat seat) { ... }

@CacheEvict(value = "seats", key = "#seat.concertDateId")       // 취소 시 캐시 무효화
public Seat releaseSeat(Seat seat) { ... }
```

**설계 의도**: 좌석 목록 조회는 읽기 비중이 압도적으로 높고, 상태 변경은 상대적으로 드물기 때문에 Cache-Aside 패턴이 효과적입니다. 상태가 변경될 때만 `@CacheEvict`로 해당 콘서트의 캐시를 삭제하면, 다음 조회 시 DB에서 최신 데이터를 가져와 다시 캐싱합니다.

**왜 로컬 캐시(Caffeine)가 아닌 Redis인가?**

이 시스템은 대규모 트래픽 처리를 위해 다중 인스턴스 스케일 아웃을 전제로 합니다. 로컬 캐시는 JVM 메모리에 저장되므로 인스턴스마다 독립된 캐시가 존재합니다. Instance A에서 좌석을 예약하여 `@CacheEvict`가 실행되어도 Instance B, C의 캐시에는 여전히 예약 전 데이터가 남아 있어 사용자에게 잘못된 정보를 제공하게 됩니다.

```
로컬 캐시 문제:
[Instance A] 좌석 예약 → @CacheEvict → A의 캐시만 삭제
[Instance B] 좌석 조회 → B의 캐시에 "AVAILABLE" 잔존 → 잘못된 정보 제공

Redis 캐시 해결:
[Instance A] 좌석 예약 → @CacheEvict → Redis 캐시 삭제 (중앙 저장소)
[Instance B] 좌석 조회 → Redis 캐시 미스 → DB 조회 → 최신 데이터 반환
```

Redis는 외부 중앙 저장소이므로 어느 인스턴스에서 `@CacheEvict`를 실행하든 전체에 즉시 반영됩니다. 또한 대기열 시스템이 이미 Redis를 사용하고 있어 추가 인프라 비용 없이 캐시 저장소를 확보할 수 있었고, Redis가 기본 제공하는 TTL로 만료 처리도 별도 구현 없이 해결됩니다.

### Phase 5: Redis 안정성 확보

**문제**: Redis 단일 장애점(SPoF) 위험

**해결 (2가지 방어 계층)**:
1. **AOF 영속화**: `docker-compose.yml`에서 `--appendonly yes` 설정 → Redis 재시작 시 데이터 복원
2. **Interceptor 재설계**: `QueueTokenInterceptor`가 기존 DB가 아닌 Redis에서 토큰 검증하도록 변경

### Phase 6: 이벤트 기반 결제 처리

**구현**: 결제 완료 시 외부 시스템 연동을 이벤트 기반으로 분리

```java
// ProcessPaymentUseCase.java — 결제 트랜잭션 내부
Payment payment = paymentProcessor.processPayment(...);
eventPublisher.publishEvent(new PaymentCompletedEvent(...));
```

```java
// DataPlatformEventListener.java — 트랜잭션 커밋 후 비동기 실행
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
public void handlePaymentCompleted(PaymentCompletedEvent event) { ... }
```

**설계 의도**:
- `AFTER_COMMIT`: 결제 트랜잭션이 확정된 후에만 외부 호출 → 정합성 보장
- `@Async`: 외부 API 응답 대기 없이 즉시 사용자 응답 반환
- `@Retryable`: 외부 시스템 장애 시 지수 백오프 자동 재시도 (2초 → 4초 → 8초)
- 외부 호출 실패가 결제 트랜잭션에 영향을 주지 않음

### Phase 7: 중복 환불 방지 강화

**문제**: `RefundProcessor`가 결제 정보를 일반 `findById()`로 조회 → 동시 환불 요청 시 두 스레드 모두 통과 가능

**해결 (2중 방어)**:
1. **비관적 락 추가**: `PaymentJpaRepository.findByIdWithLock()` 메서드 신설 → `RefundProcessor`에서 사용
2. **DB Unique Constraint**: `refunds` 테이블의 `payment_id` 컬럼에 유니크 제약 추가 → 최종 안전장치

---

## 전체 플로우 (End-to-End)

```
[유저 접속] → [대기열 토큰 발급] → [스케줄러가 활성화] → [좌석 조회]
     → [좌석 예약 (5분 임시점유)] → [잔액 충전] → [결제] → [토큰 만료]
```

### Step 1. 대기열 토큰 발급
- `POST /api/v1/queue/token` → Redis `ZADD queue:waiting` (Sorted Set)
- UUID 기반 토큰 발급, 대기 순번 반환

### Step 2. 토큰 활성화 (스케줄러)
- `QueueActivationScheduler`가 **10초마다** 실행
- 대기열에서 **최대 3,000명**을 꺼내 Active 상태로 전환
- 처리량: 분당 최대 18,000명 활성화 가능

### Step 3. API 접근 인증 (Interceptor)
- `QueueTokenInterceptor`가 HTTP 헤더 `X-Queue-Token`을 확인
- Redis에서 토큰 `ACTIVE` 상태 검증
- 보호 대상 경로: `/api/v1/reservations/**`, `/api/payments/**`, `/api/refunds/**`

### Step 4. 좌석 예약
- `@Transactional` + `@Lock(PESSIMISTIC_WRITE)`로 좌석 Row Lock 획득
- 도메인 모델이 `AVAILABLE → RESERVED` 상태 전이만 허용
- 5분 타임아웃 설정 (미결제 시 자동 해제)

### Step 5. 결제 및 확정
- 단일 트랜잭션 내에서 순차 실행:
  1. 잔액 차감 (비관적 락)
  2. 결제 내역 저장
  3. 예약 상태 `CONFIRMED`로 변경
  4. 좌석 상태 `SOLD`로 확정
- 커밋 후 `PaymentCompletedEvent` 발행 → 외부 시스템 비동기 알림

---

## 동시성 제어 상세

### 왜 비관적 락(Pessimistic Lock)을 선택했는가

콘서트 예약의 특성상 **"인기 좌석에 수천 명이 동시 접근"**하는 상황이 빈번합니다.
낙관적 락(Optimistic Lock)을 사용하면 거의 모든 요청이 충돌하여 재시도가 폭발적으로 증가하므로,
DB 레벨에서 선착순 진입을 보장하는 비관적 락이 적합합니다.

### 적용 영역별 상세

#### 1. 좌석 예약 — 이중 예약 방지

```java
// SeatJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM SeatEntity s WHERE s.id = :id")
Optional<SeatEntity> findByIdWithLock(@Param("id") Long id);
```

```java
// ReserveSeatUseCase.java — 트랜잭션 내부
@Transactional
public ReservationResponse execute(ReserveSeatRequest request) {
    Seat seat = seatManager.getSeatByIdWithLock(seatId);        // SELECT FOR UPDATE
    if (reservationManager.hasActiveReservation(seat.getId())) { // 이미 예약된 좌석 체크
        throw new IllegalStateException("이미 예약된 좌석입니다");
    }
    seat.reserve();  // AVAILABLE → RESERVED 상태 검증
    ...
}
```

**동작 원리**: 첫 번째 스레드가 Row Lock을 획득하면, 나머지 스레드는 해당 트랜잭션이 커밋될 때까지 대기합니다. 커밋 후 두 번째 스레드가 진입하지만 좌석이 이미 `RESERVED` 상태이므로 예외가 발생합니다.

#### 2. 잔액 관리 — 마이너스 잔액 방지

```java
// BalanceJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM BalanceEntity b WHERE b.userId = :userId")
Optional<BalanceEntity> findByUserIdWithLock(@Param("userId") String userId);
```

```java
// Balance.java — 도메인 모델 내 검증
public void use(BigDecimal useAmount) {
    if (this.amount.compareTo(useAmount) < 0) {
        throw new IllegalStateException("잔액이 부족합니다");
    }
    this.amount = this.amount.subtract(useAmount);
}
```

**동작 원리**: 충전/사용/환불 세 가지 연산 모두 `findByUserIdWithLock()`으로 시작합니다. 동시에 같은 유저의 잔액을 변경하는 요청이 들어와도 순차 처리됩니다.

**신규 유저 동시 충전 대응**: 두 스레드가 동시에 새 잔액 레코드를 생성하면 `DataIntegrityViolationException`이 발생합니다. 이를 catch하여 비관적 락 재조회로 폴백합니다.

#### 3. 환불 — 중복 환불 방지

```java
// PaymentJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PaymentEntity p WHERE p.id = :id")
Optional<PaymentEntity> findByIdWithLock(@Param("id") Long id);
```

**2중 방어 계층**:
- **Application**: 결제 Row를 비관적 락으로 잠근 뒤 기존 환불 여부 조회
- **Database**: `refunds.payment_id`에 Unique Constraint 설정 → 극단적 Race Condition에서도 DB가 차단

---

## Redis 대기열: JPA에서 이관한 이유

### 기존 JPA 방식의 병목

```
[유저 1만 명 폴링] → DB SELECT (ORDER BY timestamp) → 커넥션 풀 고갈
                                                    → 예약/결제 트랜잭션 지연
```

1. **커넥션 풀 경쟁**: 대기열 확인 폴링이 예약/결제 트랜잭션과 같은 DB 커넥션 풀을 공유
2. **정렬 비용**: 수만 행을 `ORDER BY`로 정렬하여 순번을 계산하는 것은 비용이 높음
3. **만료 처리**: 별도 배치를 돌려 만료 토큰을 `DELETE`해야 하는 운영 부담

### Redis 이관 후 개선

```
[유저 1만 명 폴링] → Redis ZSCORE/ZRANK (O(log N)) → 즉시 응답
[예약/결제]        → DB 커넥션 온전히 확보           → 안정적 처리
```

1. **Sorted Set**: `ZADD`/`ZRANK`가 O(log N)으로 순번 조회 → DB `ORDER BY` 대비 압도적 속도
2. **TTL 자동 만료**: 활성 토큰에 5분 TTL 설정 → 별도 만료 배치 불필요
3. **DB 부하 격리**: 대기열 트래픽이 DB에 전혀 유입되지 않아 핵심 비즈니스 로직(예약/결제)의 처리량 보장
4. **Pipeline**: 토큰 생성 시 5개 Redis 명령을 1회 네트워크 왕복으로 처리

### Redis 최적화 요약

| 개선 전 | 개선 후 | 효과 |
|---------|---------|------|
| `KEYS queue:active:*` | `SCAN` (커서 기반) | Redis 싱글 스레드 블로킹 방지 |
| 개별 `HGET` × N회 | `HGETALL` 1회 | N+1 조회 문제 해결 |
| 명령 개별 실행 | `Pipeline` 배치 처리 | 네트워크 왕복 횟수 감소 |
| 수동 삭제 배치 | `TTL` 자동 만료 | 운영 부담 제거 |

---

## 테스트 전략 및 결과

### 테스트 구성

| 분류 | 테스트 수 | 목적 |
|------|----------|------|
| 단위 테스트 | 294개 | 도메인 모델, 컴포넌트, UseCase 단위 검증 |
| 통합 테스트 | 8개 | 전체 예약/결제/환불 플로우 검증 |
| 동시성 테스트 | 6개 | 비관적 락 동작 검증 |
| 부하 테스트 | 6개 | 대규모 트래픽 시뮬레이션 |

### 동시성 검증 테스트 (ConcurrencyIntegrationTest)

| 시나리오 | 동시 스레드 | 대상 | 검증 결과 |
|----------|-----------|------|----------|
| 10명 → 1좌석 | 10 | 좌석 예약 | 정확히 1명 성공, 9명 실패 |
| 100명 → 1좌석 | 100 | 좌석 예약 | 정확히 1명 성공, 99명 실패 |
| 1,000명 → 1좌석 | 1,000 | 좌석 예약 | 정확히 1명 성공, 999명 실패 |
| 100명 → 10좌석 | 100 | 좌석 예약 | 정확히 10명 성공 (좌석당 1명) |

### 잔액 동시성 검증 테스트 (ConcurrencyControlIntegrationTest)

| 시나리오 | 동시 스레드 | 대상 | 검증 결과 |
|----------|-----------|------|----------|
| 동시 잔액 사용 | 10 | 10,000원, 각 1,500원 사용 | 6건 성공, 4건 실패, 잔액 = 1,000원 |
| 충전 + 사용 혼재 | 10 | 5스레드 충전 + 5스레드 사용 | 최종 잔액 정확 일치 |

### 부하 테스트 (LoadTest5 - Full Flow)

10,000명 동시 접속, 전체 플로우(대기열 → 충전 → 예약 → 결제) 시뮬레이션:

| 항목 | 결과 |
|------|------|
| 대기열 진입 | 10,000명 전원 성공 (100%) |
| 토큰 활성화 | 10,000건 활성화 (100%) |
| 잔액 충전 | 10,000명 × 100,000원 충전 완료 |
| 좌석 예약 | 약 9,074건 성공 (랜덤 좌석 충돌로 일부 실패 — 시스템 오류가 아닌 테스트 특성) |
| 결제 처리 | 예약 성공 건 전체 결제 완료 (100%) |
| 데이터 정합성 | 총 충전 금액 = 잔액 + 결제 총액 (정확 일치) |

**예약 성공률이 100%가 아닌 이유**: 테스트 봇이 50,000개 좌석 중 랜덤으로 선택하면서 "생일 문제(Birthday Problem)"와 같은 확률적 충돌이 발생합니다. 실제 사용자라면 다른 좌석을 선택하여 예약에 성공합니다.

---

## API 엔드포인트

| HTTP | 경로 | 설명 | 토큰 필요 |
|------|------|------|----------|
| POST | `/api/v1/queue/token` | 대기열 토큰 발급 | X |
| GET | `/api/v1/queue/status` | 대기 순번 조회 (폴링) | X |
| GET | `/api/v1/concerts/dates` | 예약 가능 날짜 조회 | X |
| GET | `/api/v1/concerts/{id}/seats` | 좌석 목록 조회 | X |
| POST | `/api/v1/reservations` | 좌석 예약 (5분 임시점유) | O |
| DELETE | `/api/v1/reservations/{id}` | 예약 취소 | O |
| POST | `/api/payments` | 결제 처리 | O |
| POST | `/api/refunds` | 환불 요청 | O |
| POST | `/api/balance/charge` | 잔액 충전 | X |
| GET | `/api/balance/{userId}` | 잔액 조회 | X |

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 실행 방법

### 사전 조건
- Java 17+
- Docker & Docker Compose

### 인프라 실행
```bash
docker-compose up -d
```

### 애플리케이션 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 동시성 테스트만
./gradlew test --tests "ConcurrencyIntegrationTest"

# 부하 테스트
./gradlew test --tests "LoadTest5_FullFlowWithPaymentTest"
```

---

## 설정

### Docker Compose 구성

| 서비스 | 이미지 | 주요 설정 |
|--------|--------|----------|
| Redis | `redis:7.2-alpine` | AOF 영속화 (`--appendonly yes`), `--maxclients 20000` |
| PostgreSQL | `postgres:16-alpine` | UTF-8, Asia/Seoul 타임존 |

### Redis 커넥션 풀 (Lettuce)

```properties
spring.data.redis.lettuce.pool.max-active=2000
spring.data.redis.lettuce.pool.max-idle=1000
spring.data.redis.lettuce.pool.min-idle=100
spring.data.redis.lettuce.pool.max-wait=3000ms
```

---

## 아키텍처 결정 사유 요약

| 결정 | 이유 |
|------|------|
| 비관적 락 (좌석/잔액) | 인기 좌석에 경합이 집중되므로 낙관적 락 시 재시도 폭발. DB 레벨 락으로 선착순 보장 |
| Redis 대기열 | DB 폴링 부하 격리. Sorted Set은 O(log N) 순번 조회, TTL로 자동 만료 |
| AOF 영속화 | Redis 재시작 시 대기열 데이터 보존 |
| 이벤트 기반 결제 후처리 | 외부 API 장애가 결제 트랜잭션을 롤백시키지 않도록 분리 |
| AFTER_COMMIT | 커밋 확정 후에만 외부 알림 → 미결제 데이터가 외부로 전송되는 상황 방지 |
| 환불 Unique Constraint | 비관적 락 + DB 유니크 제약 2중 방어로 중복 환불 완벽 차단 |
| Pipeline/SCAN | Redis 싱글 스레드 특성 고려: KEYS 대신 SCAN, 개별 명령 대신 Pipeline |
| 좌석 캐싱 (Cache-Aside) | 읽기 비중이 높은 좌석 조회를 Redis에 캐싱, 상태 변경 시에만 무효화하여 DB 부하 절감 |