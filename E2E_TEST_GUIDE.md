# 완전한 E2E 시나리오 테스트 가이드

## 🎯 시나리오 개요
고객이 **대기열 진입**부터 **결제 완료 → 환불**까지의 전체 비즈니스 플로우를 검증합니다.

---

## 📋 3가지 테스트 방법

### 1️⃣ **통합 테스트 실행** (자동화 - 추천)

```bash
# 방법 1: 배치 스크립트 실행
.\run-e2e-test.bat

# 방법 2: Gradle 직접 실행
.\gradlew.bat test --tests "CompleteE2EScenarioTest"

# 테스트 결과 확인
# build\reports\tests\test\index.html
```

**테스트 시나리오:**
- ✅ 대기열 진입 → 토큰 활성화
- ✅ 잔액 충전 → 좌석 예약
- ✅ 결제 처리 → 예약 확정
- ✅ 환불 처리 → 좌석 복구
- ✅ 실패 시나리오 (잔액 부족, 중복 예약)

---

### 2️⃣ **API 수동 테스트** (curl 기반)

```bash
# 전체 시나리오 자동 실행
.\test-full-scenario.bat
```

**수동 실행 (단계별):**

```bash
# 1. 대기열 진입 (토큰 발급)
curl -X POST http://localhost:8080/api/v1/queue/token \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"testuser\"}"
  
# 응답: {"token":"xxx-xxx-xxx","status":"WAITING","queueNumber":1}

# 2. 토큰 활성화 대기 (10-15초 후)
curl -X GET http://localhost:8080/api/v1/queue/status \
  -H "X-Queue-Token: {받은토큰}"
  
# 응답: {"status":"ACTIVE"} 확인

# 3. 콘서트 조회
curl -X GET http://localhost:8080/api/v1/concerts/dates \
  -H "X-Queue-Token: {토큰}"

# 4. 좌석 조회
curl -X GET http://localhost:8080/api/v1/concerts/1/seats \
  -H "X-Queue-Token: {토큰}"

# 5. 잔액 충전
curl -X POST http://localhost:8080/api/balance/charge \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"testuser\",\"amount\":100000}"

# 6. 좌석 예약
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -H "X-Queue-Token: {토큰}" \
  -d "{\"userId\":\"testuser\",\"concertDateId\":1,\"seatId\":4}"
  
# 응답: {"reservationId":1,"status":"PENDING"}

# 7. 결제 처리
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-Queue-Token: {토큰}" \
  -d "{\"userId\":\"testuser\",\"reservationId\":1}"
  
# 응답: {"id":1,"status":"COMPLETED","amount":50000}

# 8. 환불 처리
curl -X POST http://localhost:8080/api/refunds \
  -H "Content-Type: application/json" \
  -H "X-Queue-Token: {토큰}" \
  -d "{\"paymentId\":1,\"userId\":\"testuser\",\"reason\":\"고객 변심\"}"
  
# 응답: {"id":1,"status":"APPROVED","amount":50000}
```

---

### 3️⃣ **Swagger UI 수동 테스트** (웹 브라우저)

**접속:** http://localhost:8080/swagger-ui.html

#### 📝 단계별 실행 순서:

**1. Queue - 대기열 진입**
```
POST /api/v1/queue/token
Body: {"userId":"swagger_test"}
→ token 값 복사
```

**2. Queue - 대기열 상태 확인** (10초 후)
```
GET /api/v1/queue/status
Header: X-Queue-Token: {복사한토큰}
→ status가 "ACTIVE"인지 확인
```

**3. Balance - 잔액 충전**
```
POST /api/balance/charge
Body: {"userId":"swagger_test","amount":100000}
```

**4. Concerts - 콘서트 조회**
```
GET /api/v1/concerts/dates
Header: X-Queue-Token: {토큰}
→ concertDateId 확인 (예: 1)
```

**5. Concerts - 좌석 조회**
```
GET /api/v1/concerts/{concertDateId}/seats
Header: X-Queue-Token: {토큰}
Path: concertDateId=1
→ 예약 가능한 seatId 확인 (예: 4)
```

**6. Reservations - 좌석 예약**
```
POST /api/v1/reservations
Header: X-Queue-Token: {토큰}
Body: {"userId":"swagger_test","concertDateId":1,"seatId":4}
→ reservationId 확인
```

**7. Payments - 결제 처리**
```
POST /api/payments
Header: X-Queue-Token: {토큰}
Body: {"userId":"swagger_test","reservationId":1}
→ paymentId 확인
```

**8. Refunds - 환불 처리**
```
POST /api/refunds
Header: X-Queue-Token: {토큰}
Body: {"paymentId":1,"userId":"swagger_test","reason":"테스트"}
```

**9. Balance - 최종 잔액 확인**
```
GET /api/balance/{userId}
Path: userId=swagger_test
→ 100,000원 복구 확인
```

---

## 🔍 PostgreSQL 데이터 검증

```bash
# 데이터 확인
.\check-postgres-data.bat

# 또는 직접 쿼리
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation

-- 예약 현황 조회
SELECT r.id, r.user_id, r.status, s.seat_number, p.status AS payment_status
FROM reservations r
JOIN seats s ON r.seat_id = s.id
LEFT JOIN payments p ON r.id = p.reservation_id
ORDER BY r.created_at DESC
LIMIT 10;

-- 잔액 조회
SELECT * FROM balance WHERE user_id = 'testuser';

-- 환불 조회
SELECT * FROM refunds ORDER BY created_at DESC LIMIT 5;
```

---

## 📊 검증 체크리스트

### ✅ 대기열 단계
- [ ] 토큰 발급 성공
- [ ] 대기 순번 부여
- [ ] WAITING → ACTIVE 상태 전환 (10-15초 후)
- [ ] Redis에 토큰 저장 확인

### ✅ 예약 단계
- [ ] 잔액 충전 성공 (PostgreSQL balance 테이블)
- [ ] 콘서트/좌석 조회 성공
- [ ] 좌석 예약 생성 (PENDING 상태)
- [ ] 좌석 상태: AVAILABLE → RESERVED

### ✅ 결제 단계
- [ ] 결제 처리 성공 (payments 테이블)
- [ ] 잔액 차감 (100,000 → 50,000원)
- [ ] 예약 상태: PENDING → CONFIRMED
- [ ] 결제 금액: 50,000원

### ✅ 환불 단계
- [ ] 환불 처리 성공 (refunds 테이블)
- [ ] 잔액 복구 (50,000 → 100,000원)
- [ ] 예약 상태: CONFIRMED → CANCELLED
- [ ] 좌석 상태: RESERVED → AVAILABLE

---

## 🚨 예상 에러 시나리오

### 1. 토큰 미활성화
```json
{
  "status": 401,
  "message": "대기열 토큰이 활성 상태가 아닙니다. 현재 상태: WAITING"
}
```
→ **해결:** 10-15초 대기 후 다시 시도

### 2. 잔액 부족
```json
{
  "status": 400,
  "message": "잔액이 부족합니다. 현재 잔액: 30000, 필요 금액: 50000"
}
```
→ **해결:** 잔액 충전 API 호출

### 3. 중복 예약
```json
{
  "status": 409,
  "message": "이미 예약된 좌석입니다. 좌석 ID: 4"
}
```
→ **해결:** 다른 좌석 ID로 시도

### 4. 토큰 만료
```json
{
  "status": 401,
  "message": "유효하지 않은 토큰입니다"
}
```
→ **해결:** 새로운 토큰 발급

---

## 📈 성능 테스트

동시 사용자 테스트:
```bash
# 10명의 사용자가 동시에 예약 시도
for i in {1..10}; do
  (curl -X POST http://localhost:8080/api/v1/reservations \
    -H "Content-Type: application/json" \
    -H "X-Queue-Token: ${TOKEN[$i]}" \
    -d "{\"userId\":\"user$i\",\"concertDateId\":1,\"seatId\":$i}" &)
done
wait
```

---

## 🎯 테스트 완료 기준

- ✅ 모든 API 응답 코드 200/201
- ✅ PostgreSQL에 데이터 정상 저장
- ✅ Redis 토큰 관리 정상 동작
- ✅ 잔액/좌석/예약 상태 일관성 유지
- ✅ 환불 후 모든 데이터 원복 확인

---

## 📞 문제 해결

**애플리케이션 로그 확인:**
```bash
# 실시간 로그 확인
tail -f logs/application.log

# PostgreSQL 연결 확인
docker exec concert-reservation-postgres pg_isready

# Redis 연결 확인
docker exec concert-reservation-redis redis-cli ping
```

**포트 충돌 해결:**
```bash
.\kill-port-8080.bat
```

**데이터 초기화:**
```sql
-- PostgreSQL 데이터 삭제
TRUNCATE TABLE refunds, payments, reservations, seats, concert_dates, balance CASCADE;
```

---

## 📚 참고 자료

- API 문서: http://localhost:8080/swagger-ui.html
- 테스트 리포트: `build\reports\tests\test\index.html`
- PostgreSQL 연결: `localhost:5432` (concert_user/concert_pass)
- Redis 연결: `localhost:6379`
