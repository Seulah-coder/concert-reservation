# 콘서트 예약 시스템 아키텍처 및 로직 분석 보고서

## 1. 시스템 아키텍처 개요 (System Architecture)

본 시스템은 대규모 트래픽을 처리해야 하는 콘서트 예약 서비스를 위한 고성능, 고가용성 아키텍처로 설계되었습니다.
핵심은 **"대기열 시스템의 Redis 이관"**과 **"트랜잭션 분리 및 동시성 제어"**입니다.

### 전체 기술 스택
- **Application**: Spring Boot 3.x
- **Database**: PostgreSQL (Core Business Data: Concert, Seat, Reservation, Payment)
- **Queue/Cache**: Redis (Waiting Queue, Active Token Management)
- **Concurrency**: Optimistic Locking (Balance), Pessimistic Locking (Seat Reservation)

---

## 2. End-to-End 전체 로직 분석

사용자의 진입부터 결제 완료까지의 데이터 흐름은 다음과 같습니다.

### Step 1: 대기열 토큰 발급 (Queue Token Issue)
- **User Action**: 유저가 접속을 시도합니다.
- **Logic**:
    1. `TokenController`가 요청을 받습니다.
    2. `RedisQueueRepository`의 `addToWaitingQueue`를 호출합니다.
    3. **Redis 동작**: `ZADD` 명령어를 사용하여 `waiting_queue` (Sorted Set)에 유저 ID를 추가합니다. Score는 현재 타임스탬프입니다.
    4. 대기 순번과 토큰 UUID를 담은 응답을 반환합니다.
- **Why Redis?**: DB에 `INSERT`하는 것보다 메모리 기반의 Redis `ZADD`가 압도적으로 빠르며, 수만 명의 동시 접속을 가볍게 처리합니다.

### Step 2: 대기열 활성화 (Queue Activation / Scheduling)
- **System Action**: 스케줄러(`QueueScheduler`)가 주기적으로(예: 1초마다) 실행됩니다.
- **Logic**:
    1. 서비스 가능한 최대 동시 접속자 수(N명)를 기준으로 여유 슬롯을 계산합니다.
    2. **Redis 동작**: `waiting_queue`에서 `ZRANGE`로 N명만큼 가져옵니다.
    3. 가져온 유저들을 `active_tokens` (Hash)로 이동시킵니다 (`active:token:{uuid}`).
    4. `waiting_queue`에서 해당 유저들을 제거합니다.
- **Optimization**: 대량의 키 이동 시 네트워크 오버헤드를 줄이기 위해 **Pipeline**을 사용하여 배치 처리합니다.

### Step 3: 인터셉터 인증 (Interceptor Verification)
- **User Action**: 유저가 `예약 가능 날짜 조회`, `좌석 예약`, `결제` 등을 요청합니다.
- **Logic**:
    1. `QueueTokenInterceptor`가 HTTP 헤더의 토큰을 가로챕니다.
    2. **Resilience Logic**:
        - 1차: `Redis`에 해당 토큰이 `Active` 상태인지 확인합니다.
        - 2차(Fallback): Redis 연결 실패 시, DB(`Queue` 테이블)를 조회하여 비상 상황에서도 서비스가 중단되지 않도록 합니다.
    3. 유효하지 않거나 대기 상태인 토큰이면 요청을 거부(401/403)합니다.

### Step 4: 좌석 예약 (Seat Reservation)
- **User Action**: 원하는 좌석을 선택하여 예약 요청을 보냅니다.
- **Logic**:
    1. Transaction 시작.
    2. 해당 날짜/좌석 번호에 대해 **Pessimistic Lock (비관적 락)**을 획득하거나, Unique Constraint를 통해 중복 예약을 방지합니다.
    3. 좌석 상태를 `TEMPORARY_RESERVED`로 변경하고 `Reservation` 레코드를 생성합니다.
    4. 임시 점유 시간(5분)이 설정됩니다.

### Step 5: 결제 및 예약 확정 (Payment & Confirm)
- **User Action**: 결제를 시도합니다.
- **Logic**:
    1. 유저의 잔액을 조회하고 차감합니다. (동시성 제어를 위해 **Optimistic Lock (@Version)** 사용).
    2. 결제 내역(`Payment`)을 저장합니다.
    3. 예약 상태를 `CONFIRMED`로 변경하고 좌석 상태를 `RESERVED`로 확정합니다.
    4. 결제 완료 이벤트를 발행하여(외부 시스템 연동 고려) `DataPlatform`에 전송합니다.
    5. **Token 만료**: 사용이 완료된 토큰을 Redis `active_tokens` 및 `waiting_queue`에서 삭제하여 다음 대기자를 위한 자리를 확보합니다.

---

## 3. Migration 분석: JPA에서 Redis로의 전환

초기 설계(JPA)에서 현재(Redis)로 아키텍처를 변경한 핵심 이유는 **성능(Performance)**과 **확장성(Scalability)**입니다.

### 3.1. 기존 JPA(RDB) 방식의 문제점
1. **Polling 부하**: 클라이언트가 "나 이제 들어갈 수 있어?"라고 묻는 Polling 요청이 수천 건씩 DB로 유입됩니다. 이는 정작 중요한 `예약/결제` 트랜잭션을 처리해야 할 DB 커넥션 풀을 고갈시킵니다.
2. **Locking Overhead**: 대기열 순서를 관리하기 위해 Table Lock이나 Row Lock이 빈번하게 발생하여 동시성 처리량이 급격히 저하됩니다.
3. **비효율적인 순서 관리**: RDB에서 수만 개의 행을 `ORDER BY timestamp`로 매번 정렬하여 조회하는 것은 매우 비싼 연산입니다.

### 3.2. Redis 도입 효과
1. **O(log N) 순서 보장**: Redis의 **Sorted Set** 자료구조는 데이터 삽입 시 자동으로 정렬되므로, `RANK` 조회나 범위 조회(`ZRANGE`)가 매우 빠릅니다.
2. **In-Memory Speed**: 디스크 I/O가 발생하는 DB와 달리 메모리에서 동작하므로 대기열 조회/진입 응답 속도가 ms 단위로 단축됩니다.
3. **TTL (Time-To-Live) 자동 만료**: 유저가 이탈하거나 비정상 종료된 경우, Redis의 TTL 기능을 사용해 자동으로 토큰을 만료시켜 별도의 배치 삭제 로직(Delete Job) 없이도 리소스를 관리할 수 있습니다.
4. **Active Token 검증 최적화**: `Keys` 스캔 대신 `Hash` 구조나 구체적인 Key 조회를 통해 O(1) 수준으로 현재 활성화 여부를 즉시 판단합니다.

### 3.3. 주요 리팩토링 포인트
- **Keys -> Scan**: Redis 싱글 스레드 블로킹을 유발하는 `keys` 명령어를 `scan`으로 변경하여 운영 안정성 확보.
- **N+1 문제 해결**: 활성 토큰 조회 시 Loop를 돌며 하나씩 조회하던 것을 `HGETALL` 등으로 한 번에 조회하거나 Pipeline 처리.
- **AOF (Append Only File)**: Redis가 다운되더라도 데이터 유실을 최소화하기 위해 AOF 옵션 활성화 (`appendonly yes`).

---

## 4. 성능 검증 결과 (Load Test Summary)

**테스트 시나리오 (LoadTest5)**:
- **사용자**: 10,000명 동시 접속 시뮬레이션
- **Flow**: 대기열 진입 -> 토큰 활성화 대기 -> 잔액 충전 -> 좌석 예약 -> 결제

**결과**:
- **Queue/Token**: 10,000명 전원 정상 발급 및 활성화 성공 (처리율 100%).
- **Payment**: 예약에 성공한 9,074건에 대해 **결제 성공률 100%**.
- **Data Integrity**: 총 충전 금액 = 잔액 + 총 결제 금액 정확히 일치.
- **Note**: 예약 성공률이 100%가 아닌 이유는 테스트 봇들이 랜덤하게 좌석을 고르면서 발생한 **충돌(Collision)** 때문이며, 시스템 오류가 아닙니다. 실제 유저라면 다른 좌석을 선택하여 성공했을 것입니다.

---

## 5. 결론 (Conclusion)

현재 시스템은 **Redis를 활용한 대기열 격리**를 통해 DB 부하를 원천 차단했습니다.
이로 인해 핵심 비즈니스 로직(예약/결제)은 DB의 자원을 온전히 사용할 수 있게 되어 **대규모 트래픽 상황에서도 안정적인 서비스가 가능함**을 입증했습니다.
또한, Redis 장애 시 DB를 조회하는 **Fallback 메커니즘**을 통해 SPoF(Single Point of Failure) 위험도 효과적으로 완화하였습니다.
