# Docker로 Redis 실행하기

## 📋 사전 준비

### 1. Docker Desktop 설치 확인

**Windows:**
1. Docker Desktop 다운로드: https://www.docker.com/products/docker-desktop
2. 설치 후 재시작
3. 확인:
```bash
docker --version
docker-compose --version
```

## 🚀 Redis 실행 방법

### 방법 1: Docker Compose 사용 (권장)

**1) 프로젝트 루트에서 실행:**
```bash
# Redis 컨테이너 시작 (백그라운드)
docker-compose up -d

# 로그 확인
docker-compose logs redis

# 상태 확인
docker-compose ps
```

**2) Redis 연결 테스트:**
```bash
# Redis CLI 접속
docker exec -it concert-reservation-redis redis-cli

# Redis 명령어 테스트
> PING
PONG

> SET test "Hello Redis"
OK

> GET test
"Hello Redis"

> exit
```

**3) 중지 및 제거:**
```bash
# 중지
docker-compose stop

# 중지 및 컨테이너 제거
docker-compose down

# 볼륨까지 모두 제거 (데이터 삭제)
docker-compose down -v
```

---

### 방법 2: Docker 명령어 직접 사용

```bash
# Redis 컨테이너 실행
docker run -d \
  --name concert-redis \
  -p 6379:6379 \
  -v redis-data:/data \
  redis:7.2-alpine redis-server --appendonly yes

# 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs concert-redis

# Redis CLI 접속
docker exec -it concert-redis redis-cli

# 중지
docker stop concert-redis

# 제거
docker rm concert-redis
```

---

## 🔌 애플리케이션 연결

Redis가 실행되면 Spring Boot 애플리케이션이 자동으로 연결됩니다.

**application.properties 설정 (이미 완료):**
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**연결 확인:**
```bash
# 애플리케이션 실행
./gradlew bootRun

# 로그에서 확인
# "Lettuce connection initialized" 메시지 확인
```

---

## 🧪 테스트 시나리오

### 1. 대기열 토큰 발급
```bash
curl -X POST http://localhost:8080/api/v1/queue/token \
  -H "Content-Type: application/json" \
  -d '{"userId": "testUser001"}'
```

**예상 응답:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "testUser001",
  "queueNumber": 1,
  "status": "WAITING",
  "estimatedWaitTime": "0분 2초",
  "enteredAt": "2024-12-25T10:30:00"
}
```

### 2. Redis에서 확인
```bash
docker exec -it concert-reservation-redis redis-cli

# Waiting Queue 확인
> ZCARD queue:waiting
(integer) 1

# Token Metadata 확인
> KEYS queue:token:*
1) "queue:token:550e8400-e29b-41d4-a716-446655440000"

> HGETALL queue:token:550e8400-e29b-41d4-a716-446655440000
1) "userId"
2) "testUser001"
3) "status"
4) "WAITING"
5) "enteredAt"
6) "2024-12-25T10:30:00"
```

### 3. 10초 후 Active 확인 (스케줄러 동작)
```bash
# Active Queue 확인
> KEYS queue:active:*
1) "queue:active:550e8400-e29b-41d4-a716-446655440000"

# Waiting Queue에서 제거 확인
> ZCARD queue:waiting
(integer) 0
```

---

## 📊 모니터링

### Redis 실시간 모니터링
```bash
# Redis CLI에서 모니터링
docker exec -it concert-reservation-redis redis-cli MONITOR

# 또는
docker exec -it concert-reservation-redis redis-cli
> MONITOR
```

### 메모리 사용량 확인
```bash
docker exec -it concert-reservation-redis redis-cli INFO memory
```

### 키 통계
```bash
docker exec -it concert-reservation-redis redis-cli INFO keyspace
```

---

## 🔧 트러블슈팅

### 문제 1: 포트 6379가 이미 사용 중
```
Error: Bind for 0.0.0.0:6379 failed: port is already allocated
```

**해결:**
```bash
# 다른 Redis 프로세스 확인
netstat -ano | findstr :6379

# 포트 변경 (docker-compose.yml)
ports:
  - "6380:6379"

# application.properties 수정
spring.data.redis.port=6380
```

### 문제 2: Docker Desktop이 실행되지 않음
```
Error: Cannot connect to the Docker daemon
```

**해결:**
1. Docker Desktop 실행
2. 작업 관리자에서 "Docker Desktop" 프로세스 확인

### 문제 3: 애플리케이션 연결 실패
```
Error: Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException
```

**해결:**
```bash
# Redis 컨테이너 상태 확인
docker ps | grep redis

# Redis 재시작
docker-compose restart redis

# 연결 테스트
docker exec -it concert-reservation-redis redis-cli PING
```

### 문제 4: 데이터가 사라짐
**원인:** 볼륨 마운트가 안 되어 있어 컨테이너 재시작 시 데이터 손실

**해결:** docker-compose.yml에 volumes 설정 (이미 완료됨)
```yaml
volumes:
  - redis-data:/data
```

---

## 🎯 다음 단계

1. ✅ Docker Redis 실행
2. ✅ 애플리케이션 연결
3. 🔄 대기열 토큰 발급 테스트
4. 🔄 10초 후 활성화 확인
5. 🔄 좌석 조회 캐싱 테스트
6. 🔄 성능 테스트 (선택)

---

## 💡 추가 팁

### Redis GUI 도구 (선택)
- **Redis Commander**: 웹 기반 GUI
```bash
docker run -d \
  --name redis-commander \
  --env REDIS_HOSTS=local:concert-reservation-redis:6379 \
  --network container:concert-reservation-redis \
  -p 8081:8081 \
  rediscommander/redis-commander:latest
```
접속: http://localhost:8081

### Redis 데이터 백업
```bash
# 현재 데이터 백업
docker exec concert-reservation-redis redis-cli SAVE

# 백업 파일 복사
docker cp concert-reservation-redis:/data/dump.rdb ./backup/
```

### 프로덕션 설정 (참고)
```yaml
# docker-compose.prod.yml
services:
  redis:
    image: redis:7.2-alpine
    command: >
      redis-server
      --appendonly yes
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
      --requirepass yourStrongPassword
```
