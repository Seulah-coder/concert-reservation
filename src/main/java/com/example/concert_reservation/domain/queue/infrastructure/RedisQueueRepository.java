package com.example.concert_reservation.domain.queue.infrastructure;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Redis 기반 대기열 저장소
 * 
 * Redis 자료구조:
 * 1. Waiting Queue: Sorted Set
 *    - Key: "queue:waiting"
 *    - Score: 진입 시간 (timestamp)
 *    - Member: token
 * 
 * 2. Active Queue: Hash
 *    - Key: "queue:active:{token}"
 *    - Fields: userId, enteredAt, expiredAt, queueNumber
 * 
 * 3. Token Metadata: Hash
 *    - Key: "queue:token:{token}"
 *    - Fields: userId, status, enteredAt, expiredAt, queueNumber
 */
@Repository
public class RedisQueueRepository {
    
    private static final String WAITING_KEY = "queue:waiting";
    private static final String ACTIVE_KEY_PREFIX = "queue:active:";
    private static final String TOKEN_KEY_PREFIX = "queue:token:";
    private static final String USER_ACTIVE_KEY_PREFIX = "user:active:";
    private static final String USER_WAITING_KEY_PREFIX = "user:waiting:";
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisQueueRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 대기열에 토큰 추가 (Sorted Set)
     * Pipeline으로 원자성 보장, TTL 설정으로 메모리 누수 방지
     */
    public UserQueue addToWaitingQueue(String userId) {
        QueueToken token = QueueToken.generate();
        LocalDateTime now = LocalDateTime.now();
        long score = now.toEpochSecond(ZoneOffset.UTC);
        String tokenKey = TOKEN_KEY_PREFIX + token.getValue();
        long waitingTtlSeconds = java.util.concurrent.TimeUnit.MINUTES.toSeconds(30);
        
        // Pipeline으로 모든 명령 원자적 실행
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            // Waiting Queue에 추가
            connection.zAdd(WAITING_KEY.getBytes(), score, token.getValue().getBytes());
            
            // Token Metadata 저장
            connection.hSet(tokenKey.getBytes(), "userId".getBytes(), userId.getBytes());
            connection.hSet(tokenKey.getBytes(), "status".getBytes(), QueueStatus.WAITING.name().getBytes());
            connection.hSet(tokenKey.getBytes(), "enteredAt".getBytes(), now.toString().getBytes());
            
            // Token Metadata TTL 설정 (30분 - 대기 상태 최대 시간)
            connection.expire(tokenKey.getBytes(), waitingTtlSeconds);
            
            return null;
        });
        
        // 대기 번호 계산 (Pipeline 외부에서 계산)
        long queueNumber = getWaitingPosition(token.getValue()) + 1;
        redisTemplate.opsForHash().put(tokenKey, "queueNumber", String.valueOf(queueNumber));
        
        // userId → token 역매핑 (TTL 30분 설정으로 메모리 누수 방지)
        redisTemplate.opsForValue().set(
            USER_WAITING_KEY_PREFIX + userId, 
            token.getValue(),
            30,  // 30분 후 자동 삭제 (Active 5분 + 버퍼)
            java.util.concurrent.TimeUnit.MINUTES
        );
        
        // UserQueue 객체 생성
        return UserQueue.of(
            null,
            token,
            userId,
            queueNumber,
            QueueStatus.WAITING,
            now,
            null
        );
    }
    
    /**
     * Waiting Queue에서 현재 위치 조회
     */
    public long getWaitingPosition(String token) {
        Long rank = redisTemplate.opsForZSet().rank(WAITING_KEY, token);
        return rank != null ? rank : -1;
    }
    
    /**
     * Waiting Queue 크기 조회
     */
    public long getWaitingQueueSize() {
        Long size = redisTemplate.opsForZSet().size(WAITING_KEY);
        return size != null ? size : 0;
    }
    
    /**
     * Active Queue 크기 조회
     * SCAN 사용 (KEYS 대신 - 프로덕션 안전, 비블로킹)
     */
    public long getActiveQueueSize() {
        Long count = redisTemplate.execute(
            (org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
                long c = 0;
                Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                        .match(ACTIVE_KEY_PREFIX + "*")
                        .count(100)
                        .build());
                while (cursor.hasNext()) {
                    cursor.next();
                    c++;
                }
                return c;
            });
        return count != null ? count : 0;
    }
    
    /**
     * Waiting Queue에서 N개의 토큰을 Active로 전환
     * Pipeline으로 원자성 보장
     */
    public List<String> activateTokens(int count) {
        // Waiting Queue에서 상위 N개 가져오기 (Score 오름차순)
        Set<String> tokens = redisTemplate.opsForZSet().range(WAITING_KEY, 0, count - 1);
        
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        
        List<String> activatedTokens = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        List<String> tokenKeys = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(5); // 5분 후 만료
        long ttlSeconds = java.util.concurrent.TimeUnit.MINUTES.toSeconds(5);
        
        for (String token : tokens) {
            // Token Metadata 조회
            String tokenKey = TOKEN_KEY_PREFIX + token;
            String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
            
            if (userId != null) {
                activatedTokens.add(token);
                userIds.add(userId);
                tokenKeys.add(tokenKey);
            }
        }

        if (activatedTokens.isEmpty()) {
            return List.of();
        }

        // Pipeline으로 활성화 작업 일괄 실행 (연결 소모 최소화)
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (int i = 0; i < activatedTokens.size(); i++) {
                String token = activatedTokens.get(i);
                String userId = userIds.get(i);
                String tokenKey = tokenKeys.get(i);
                String activeKey = ACTIVE_KEY_PREFIX + token;
                
                // Active Queue에 추가
                connection.hSet(activeKey.getBytes(), "userId".getBytes(), userId.getBytes());
                connection.hSet(activeKey.getBytes(), "activatedAt".getBytes(), now.toString().getBytes());
                connection.hSet(activeKey.getBytes(), "expiredAt".getBytes(), expiredAt.toString().getBytes());
                
                // Token Metadata 업데이트
                connection.hSet(tokenKey.getBytes(), "status".getBytes(), QueueStatus.ACTIVE.name().getBytes());
                connection.hSet(tokenKey.getBytes(), "expiredAt".getBytes(), expiredAt.toString().getBytes());
                
                // Waiting Queue에서 제거
                connection.zRem(WAITING_KEY.getBytes(), token.getBytes());
                
                // 역매핑 Waiting 제거
                connection.del((USER_WAITING_KEY_PREFIX + userId).getBytes());

                // Active 역매핑 추가 (TTL 5분)
                connection.setEx((USER_ACTIVE_KEY_PREFIX + userId).getBytes(), ttlSeconds, token.getBytes());
                
                // Active Hash에 TTL 설정 (5분 후 Redis가 자동 만료)
                connection.expire(activeKey.getBytes(), ttlSeconds);
                
                // Token Metadata에 TTL 설정 (5분 - Active 상태 수명과 동일)
                connection.expire(tokenKey.getBytes(), ttlSeconds);
            }
            return null;
        });
        
        return activatedTokens;
    }
    
    /**
     * 토큰으로 UserQueue 조회
     * HGETALL 1회 호출로 N+1 문제 해결 (기존 HGET 5회 → 1회)
     */
    public Optional<UserQueue> findByToken(QueueToken token) {
        String tokenKey = TOKEN_KEY_PREFIX + token.getValue();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(tokenKey);
        
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        
        String userId = (String) entries.get("userId");
        if (userId == null) {
            return Optional.empty();
        }
        
        String statusStr = (String) entries.get("status");
        String enteredAtStr = (String) entries.get("enteredAt");
        String expiredAtStr = (String) entries.get("expiredAt");
        String queueNumberStr = (String) entries.get("queueNumber");
        
        QueueStatus status = QueueStatus.valueOf(statusStr);
        LocalDateTime enteredAt = LocalDateTime.parse(enteredAtStr);
        LocalDateTime expiredAt = expiredAtStr != null ? LocalDateTime.parse(expiredAtStr) : null;
        Long queueNumber = queueNumberStr != null ? Long.parseLong(queueNumberStr) : null;
        
        // UserQueue 재구성 (기존 token 사용)
        return Optional.of(UserQueue.of(
            null,  // Redis는 ID 미사용
            token,  // 기존 token 사용 (중요!)
            userId,
            queueNumber != null ? queueNumber : 0L,
            status,
            enteredAt,
            expiredAt
        ));
    }
    
    /**
     * 토큰이 Active 상태인지 확인
     * TTL 기반 - Active Hash에 TTL이 설정되어 있으므로 키 존재 여부만 확인 (O(1))
     */
    public boolean isActiveToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACTIVE_KEY_PREFIX + token));
    }
    
    /**
     * 사용자가 Active 토큰을 보유하고 있는지 확인
     * O(1) - 유저당 토큰 1개 제약 활용
     */
    public boolean hasActiveQueue(String userId) {
        Boolean exists = redisTemplate.hasKey(USER_ACTIVE_KEY_PREFIX + userId);
        return exists != null && exists;
    }
    
    /**
     * 사용자가 Waiting 토큰을 보유하고 있는지 확인
     * O(1) - 유저당 토큰 1개 제약 활용
     */
    public boolean hasWaitingQueue(String userId) {
        Boolean exists = redisTemplate.hasKey(USER_WAITING_KEY_PREFIX + userId);
        return exists != null && exists;
    }
    
    /**
     * 앞에 대기 중인 인원 수 계산 (ZSET ZRANK 기반)
     * 
     * Sorted Set의 ZRANK는 0-based → rank 자체가 앞에 있는 사람 수와 동일.
     * 중간 토큰이 활성화/만료되어 제거되면 rank가 자동으로 갱신되므로
     * 항상 정확한 대기 인원을 반환한다.
     * 
     * @param tokenValue 조회할 토큰 값
     * @return 앞에 대기 중인 인원 수 (0-based rank)
     */
    public long countWaitingAheadByToken(String tokenValue) {
        Long rank = redisTemplate.opsForZSet().rank(WAITING_KEY, tokenValue);
        return rank != null ? rank : 0;
    }
    
    /**
     * 만료된 Active 토큰 제거
     * SCAN 사용 (KEYS 대신 - 비블로킹), Pipeline 일괄 삭제
     * Note: Active Hash에 TTL이 설정되어 대부분 자동 만료됨. 이 메서드는 안전망 역할.
     */
    public int removeExpiredActiveTokens() {
        // SCAN으로 Active 키 수집 (비블로킹)
        List<String> activeKeys = new ArrayList<>();
        redisTemplate.execute(
            (org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                        .match(ACTIVE_KEY_PREFIX + "*")
                        .count(100)
                        .build());
                while (cursor.hasNext()) {
                    activeKeys.add(new String(cursor.next()));
                }
                return null;
            });
        
        if (activeKeys.isEmpty()) {
            return 0;
        }
        
        // 만료된 토큰 필터링
        List<String> expiredTokens = new ArrayList<>();
        List<String> expiredUserIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (String key : activeKeys) {
            String expiredAtStr = (String) redisTemplate.opsForHash().get(key, "expiredAt");
            if (expiredAtStr != null) {
                LocalDateTime expiredAt = LocalDateTime.parse(expiredAtStr);
                if (now.isAfter(expiredAt)) {
                    String token = key.replace(ACTIVE_KEY_PREFIX, "");
                    expiredTokens.add(token);
                    
                    String tokenKey = TOKEN_KEY_PREFIX + token;
                    String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
                    if (userId != null) {
                        expiredUserIds.add(userId);
                    }
                }
            }
        }
        
        if (expiredTokens.isEmpty()) {
            return 0;
        }
        
        // Pipeline으로 일괄 삭제
        redisTemplate.executePipelined(
            (org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (String token : expiredTokens) {
                    connection.del((ACTIVE_KEY_PREFIX + token).getBytes());
                    connection.del((TOKEN_KEY_PREFIX + token).getBytes());
                }
                for (String userId : expiredUserIds) {
                    connection.del((USER_ACTIVE_KEY_PREFIX + userId).getBytes());
                }
                return null;
            });
        
        return expiredTokens.size();
    }
    
    /**
     * 토큰 제거 (예약 완료 시)
     * Pipeline으로 일괄 삭제 (기존 개별 DELETE 4~6회 → Pipeline 1회)
     */
    public void removeToken(String token) {
        // Token에서 userId 조회 (Pipeline 외부 - 조건부 삭제에 필요)
        String tokenKey = TOKEN_KEY_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
        
        // Pipeline으로 관련 키 일괄 삭제
        redisTemplate.executePipelined(
            (org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                connection.del((ACTIVE_KEY_PREFIX + token).getBytes());
                connection.del(tokenKey.getBytes());
                connection.zRem(WAITING_KEY.getBytes(), token.getBytes());
                
                if (userId != null) {
                    connection.del((USER_ACTIVE_KEY_PREFIX + userId).getBytes());
                    connection.del((USER_WAITING_KEY_PREFIX + userId).getBytes());
                }
                return null;
            });
    }
}
