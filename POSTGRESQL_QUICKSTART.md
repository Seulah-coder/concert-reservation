# PostgreSQL 환경 구축 완료! 🎉

## ✅ 현재 상태

```
✅ PostgreSQL 컨테이너: 실행 중 (healthy)
✅ Redis 컨테이너: 실행 중 (healthy)
✅ 데이터베이스 연결: 정상
```

---

## 🚀 빠른 시작 가이드

### 1단계: Docker 컨테이너 시작 (완료!)

```bash
# 이미 실행되었습니다!
docker-compose up -d
```

**현재 실행 중인 컨테이너**:
- `concert-reservation-postgres` (Port: 5432)
- `concert-reservation-redis` (Port: 6379)

---

### 2단계: 애플리케이션 실행

#### 방법 A: IntelliJ IDEA (추천)

1. **Edit Configurations** 열기
2. **Add New Configuration** → **Spring Boot**
3. 설정:
   ```
   Name: Concert Reservation (PostgreSQL)
   Active profiles: postgres
   ```
4. **Run** 클릭 (▶️)

#### 방법 B: Gradle 명령어

```bash
.\gradlew bootRun --args='--spring.profiles.active=postgres'
```

#### 방법 C: 편리한 배치 스크립트 (추천! ⭐)

```bash
# PostgreSQL 환경으로 애플리케이션 실행 (포트 충돌 자동 해결)
run-postgres.bat

# H2 환경으로 애플리케이션 실행
run-h2.bat

# 포트 8080 충돌 해결만
kill-port-8080.bat

# PostgreSQL/Redis 컨테이너만 시작
start-postgres.bat

# 데이터 확인
check-postgres-data.bat
```

**run-postgres.bat** 특징:
- ✅ 포트 8080 충돌 자동 해결
- ✅ Docker 컨테이너 자동 시작
- ✅ PostgreSQL + Redis 상태 확인
- ✅ 애플리케이션 자동 실행

---

## 📊 데이터 확인

### PostgreSQL 접속

```bash
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation
```

### 유용한 SQL 쿼리

```sql
-- 테이블 목록
\dt

-- 콘서트 데이터 (10개)
SELECT * FROM concert_dates;

-- 좌석 데이터 (아이유 콘서트)
SELECT * FROM seats WHERE concert_date_id = 1 LIMIT 10;

-- 예약 현황
SELECT status, COUNT(*) FROM reservations GROUP BY status;

-- 좌석 상태 현황
SELECT status, COUNT(*) FROM seats GROUP BY status;
```

---

## 🧪 API 테스트

### 1. Swagger UI 접속

애플리케이션 실행 후:
```
http://localhost:8080/swagger-ui.html
```

### 2. API 테스트 시나리오

```bash
# 대기열 토큰 발급
curl -X POST http://localhost:8080/api/queue/token -H "X-User-Id: user001"

# 잔액 충전
curl -X POST http://localhost:8080/api/balance/charge \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user001" \
  -d '{"amount": 100000}'

# 좌석 예약
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user001" \
  -H "X-Queue-Token: {발급받은토큰}" \
  -d '{"concertDateId": 1, "seatId": 1}'

# 결제
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user001" \
  -d '{"reservationId": 1}'
```

---

## 📁 설정 파일

### PostgreSQL 연결 정보

**파일**: `src/main/resources/application-postgres.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/concert_reservation
spring.datasource.username=concert_user
spring.datasource.password=concert_pass
```

### 초기 데이터

**파일**: `src/main/resources/data.sql`

- 콘서트 10개
- 좌석 500개 (각 콘서트당 50개)

---

## 🛠️ 유용한 명령어

### Docker 관리

```bash
# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs postgres
docker-compose logs redis

# 컨테이너 재시작
docker-compose restart postgres

# 컨테이너 중지
docker-compose stop

# 컨테이너 및 데이터 삭제 (초기화)
docker-compose down -v
```

### PostgreSQL 관리

```bash
# 데이터베이스 접속
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation

# 데이터 백업
docker exec concert-reservation-postgres pg_dump -U concert_user concert_reservation > backup.sql

# 데이터 복원
docker exec -i concert-reservation-postgres psql -U concert_user -d concert_reservation < backup.sql
```

---

## 🔍 트러블슈팅

### 문제: 포트 8080 충돌 (가장 흔한 문제!)

**에러 메시지**:
```
Web server failed to start. Port 8080 was already in use.
```

**해결 방법**:

#### 방법 1: 자동 해결 스크립트 (가장 빠름!)
```bash
kill-port-8080.bat
```

#### 방법 2: 수동으로 프로세스 종료
```bash
# 포트 사용 중인 프로세스 찾기
netstat -ano | findstr :8080

# 결과: PID 확인 (예: 39808)
taskkill /F /PID 39808
```

#### 방법 3: run-postgres.bat 사용 (자동으로 해결)
```bash
run-postgres.bat  # 포트 충돌을 자동으로 해결하고 실행
```

---

### 문제: 포트 충돌 (5432 already in use)

**해결**:
```bash
# 기존 서비스 확인
netstat -ano | findstr 5432

# 다른 PostgreSQL 중지 또는 docker-compose.yml의 포트 변경
```

### 문제: 애플리케이션 연결 실패

**확인**:
```bash
# 1. PostgreSQL 상태
docker-compose ps

# 2. Health check
docker inspect concert-reservation-postgres | findstr Health

# 3. 재시작
docker-compose restart postgres
```

### 문제: data.sql이 실행 안 됨

**확인**:
```properties
# application-postgres.properties
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
```

---

## 📚 상세 가이드

더 자세한 내용은 다음 문서를 참고하세요:

- **[POSTGRESQL_SETUP_GUIDE.md](./report/POSTGRESQL_SETUP_GUIDE.md)** - 전체 구축 가이드
- **[INTEGRATION_TEST_REPORT_2026-02-05.md](./report/INTEGRATION_TEST_REPORT_2026-02-05.md)** - 테스트 보고서

---

## 🎯 다음 단계

1. ✅ Docker 컨테이너 시작 (완료!)
2. ▶️ **애플리케이션 실행** (profile: postgres)
3. 🧪 **API 테스트** (Swagger UI 또는 curl)
4. 📊 **PostgreSQL에서 데이터 확인**
5. 🔁 **다양한 시나리오 테스트**

---

**준비 완료!** 이제 PostgreSQL과 실제 데이터로 테스트를 시작하세요! 🚀
