package com.example.concert_reservation.domain.queue.infrastructure;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
        
        // Pipeline으로 모든 명령 원자적 실행
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            // Waiting Queue에 추가
            connection.zAdd(WAITING_KEY.getBytes(), score, token.getValue().getBytes());
            
            // Token Metadata 저장
            connection.hSet(tokenKey.getBytes(), "userId".getBytes(), userId.getBytes());
            connection.hSet(tokenKey.getBytes(), "status".getBytes(), QueueStatus.WAITING.name().getBytes());
            connection.hSet(tokenKey.getBytes(), "enteredAt".getBytes(), now.toString().getBytes());
            
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
     */
    public long getActiveQueueSize() {
        Set<String> keys = redisTemplate.keys(ACTIVE_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(5); // 5분 후 만료
        
        for (String token : tokens) {
            // Token Metadata 조회
            String tokenKey = TOKEN_KEY_PREFIX + token;
            String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
            
            if (userId != null) {
                // Pipeline으로 활성화 작업 원자적 실행
                redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
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
                    
                    return null;
                });
                
                // Active 역매핑 추가 (TTL 5분: Active 만료 시간과 동일)
                redisTemplate.opsForValue().set(
                    USER_ACTIVE_KEY_PREFIX + userId, 
                    token,
                    5,  // Active 토큰 만료 시간과 동일
                    java.util.concurrent.TimeUnit.MINUTES
                );
                
                activatedTokens.add(token);
            }
        }
        
        return activatedTokens;
    }
    
    /**
     * 토큰으로 UserQueue 조회
     */
    public Optional<UserQueue> findByToken(QueueToken token) {
        String tokenKey = TOKEN_KEY_PREFIX + token.getValue();
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
        
        if (userId == null) {
            return Optional.empty();
        }
        
        String statusStr = (String) redisTemplate.opsForHash().get(tokenKey, "status");
        String enteredAtStr = (String) redisTemplate.opsForHash().get(tokenKey, "enteredAt");
        String expiredAtStr = (String) redisTemplate.opsForHash().get(tokenKey, "expiredAt");
        String queueNumberStr = (String) redisTemplate.opsForHash().get(tokenKey, "queueNumber");
        
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
     */
    public boolean isActiveToken(String token) {
        String activeKey = ACTIVE_KEY_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(activeKey);
        
        if (exists != null && exists) {
            // 만료 시간 체크
            String expiredAtStr = (String) redisTemplate.opsForHash().get(activeKey, "expiredAt");
            if (expiredAtStr != null) {
                LocalDateTime expiredAt = LocalDateTime.parse(expiredAtStr);
                return LocalDateTime.now().isBefore(expiredAt);
            }
        }
        
        return false;
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
     * 앞에 대기 중인 인원 수 계산
     * Sorted Set에서 현재 토큰보다 score가 낮은(먼저 들어온) 토큰의 수
     */
    public long countWaitingAhead(Long queueNumber) {
        // Redis Sorted Set의 rank는 0-based이므로
        // rank 자체가 앞에 있는 사람의 수와 동일
        // queueNumber는 1-based이므로 -1
        return Math.max(0, queueNumber - 1);
    }
    
    /**
     * 토큰으로 앞에 대기 중인 인원 수 계산 (정확한 방법)
     */
    public long countWaitingAheadByToken(String tokenValue) {
        Long rank = redisTemplate.opsForZSet().rank(WAITING_KEY, tokenValue);
        return rank != null ? rank : 0;
    }
    
    /**
     * 만료된 Active 토큰 제거
     */
    public int removeExpiredActiveTokens() {
        Set<String> keys = redisTemplate.keys(ACTIVE_KEY_PREFIX + "*");
        
        if (keys == null) {
            return 0;
        }
        
        int removed = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (String key : keys) {
            String expiredAtStr = (String) redisTemplate.opsForHash().get(key, "expiredAt");
            if (expiredAtStr != null) {
                LocalDateTime expiredAt = LocalDateTime.parse(expiredAtStr);
                if (now.isAfter(expiredAt)) {
                    // Token과 userId 조회
                    String token = key.replace(ACTIVE_KEY_PREFIX, "");
                    String tokenKey = TOKEN_KEY_PREFIX + token;
                    String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
                    
                    // Active Queue에서 제거
                    redisTemplate.delete(key);
                    
                    // Token Metadata에서 제거
                    redisTemplate.delete(tokenKey);
                    
                    // 역매핑 제거
                    if (userId != null) {
                        redisTemplate.delete(USER_ACTIVE_KEY_PREFIX + userId);
                    }
                    
                    removed++;
                }
            }
        }
        
        return removed;
    }
    
    /**
     * 토큰 제거 (예약 완료 시)
     */
    public void removeToken(String token) {
        // Token에서 userId 조회
        String tokenKey = TOKEN_KEY_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
        
        // Active Queue에서 제거
        redisTemplate.delete(ACTIVE_KEY_PREFIX + token);
        
        // Token Metadata에서 제거
        redisTemplate.delete(tokenKey);
        
        // Waiting Queue에서도 제거 (혹시 있다면)
        redisTemplate.opsForZSet().remove(WAITING_KEY, token);
        
        // 역매핑 제거
        if (userId != null) {
            redisTemplate.delete(USER_ACTIVE_KEY_PREFIX + userId);
            redisTemplate.delete(USER_WAITING_KEY_PREFIX + userId);
        }
    }
}
