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
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisQueueRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 대기열에 토큰 추가 (Sorted Set)
     */
    public UserQueue addToWaitingQueue(String userId) {
        QueueToken token = QueueToken.generate();
        LocalDateTime now = LocalDateTime.now();
        long score = now.toEpochSecond(ZoneOffset.UTC);
        
        // Waiting Queue에 추가
        redisTemplate.opsForZSet().add(WAITING_KEY, token.getValue(), score);
        
        // Token Metadata 저장
        String tokenKey = TOKEN_KEY_PREFIX + token.getValue();
        redisTemplate.opsForHash().put(tokenKey, "userId", userId);
        redisTemplate.opsForHash().put(tokenKey, "status", QueueStatus.WAITING.name());
        redisTemplate.opsForHash().put(tokenKey, "enteredAt", now.toString());
        redisTemplate.opsForHash().put(tokenKey, "queueNumber", String.valueOf(getWaitingPosition(token.getValue()) + 1));
        
        return UserQueue.create(userId, getWaitingPosition(token.getValue()) + 1);
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
     */
    public List<String> activateTokens(int count) {
        // Waiting Queue에서 상위 N개 가져오기 (Score 오름차순)
        Set<String> tokens = redisTemplate.opsForZSet().range(WAITING_KEY, 0, count - 1);
        
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        
        List<String> activatedTokens = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(30); // 30분 후 만료
        
        for (String token : tokens) {
            // Token Metadata 조회
            String tokenKey = TOKEN_KEY_PREFIX + token;
            String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
            
            if (userId != null) {
                // Active Queue에 추가
                String activeKey = ACTIVE_KEY_PREFIX + token;
                redisTemplate.opsForHash().put(activeKey, "userId", userId);
                redisTemplate.opsForHash().put(activeKey, "activatedAt", now.toString());
                redisTemplate.opsForHash().put(activeKey, "expiredAt", expiredAt.toString());
                
                // Token Metadata 업데이트
                redisTemplate.opsForHash().put(tokenKey, "status", QueueStatus.ACTIVE.name());
                redisTemplate.opsForHash().put(tokenKey, "expiredAt", expiredAt.toString());
                
                // Waiting Queue에서 제거
                redisTemplate.opsForZSet().remove(WAITING_KEY, token);
                
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
        
        // UserQueue 재구성
        if (status == QueueStatus.WAITING && queueNumber != null) {
            UserQueue queue = UserQueue.create(userId, queueNumber);
            return Optional.of(queue);
        } else if (status == QueueStatus.ACTIVE && expiredAt != null) {
            UserQueue queue = UserQueue.create(userId, 0L);
            queue.activate(30); // 30분 유효
            return Optional.of(queue);
        }
        
        return Optional.empty();
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
     */
    public boolean hasActiveQueue(String userId) {
        Set<String> keys = redisTemplate.keys(ACTIVE_KEY_PREFIX + "*");
        
        if (keys == null) {
            return false;
        }
        
        for (String key : keys) {
            String storedUserId = (String) redisTemplate.opsForHash().get(key, "userId");
            if (userId.equals(storedUserId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 사용자가 Waiting 토큰을 보유하고 있는지 확인
     */
    public boolean hasWaitingQueue(String userId) {
        Set<String> tokens = redisTemplate.opsForZSet().range(WAITING_KEY, 0, -1);
        
        if (tokens == null) {
            return false;
        }
        
        for (String token : tokens) {
            String tokenKey = TOKEN_KEY_PREFIX + token;
            String storedUserId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
            if (userId.equals(storedUserId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 앞에 대기 중인 인원 수 계산
     */
    public long countWaitingAhead(Long queueNumber) {
        return queueNumber - 1;
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
                    // Active Queue에서 제거
                    redisTemplate.delete(key);
                    
                    // Token Metadata에서도 제거
                    String token = key.replace(ACTIVE_KEY_PREFIX, "");
                    redisTemplate.delete(TOKEN_KEY_PREFIX + token);
                    
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
        // Active Queue에서 제거
        redisTemplate.delete(ACTIVE_KEY_PREFIX + token);
        
        // Token Metadata에서 제거
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        
        // Waiting Queue에서도 제거 (혹시 있다면)
        redisTemplate.opsForZSet().remove(WAITING_KEY, token);
    }
}
