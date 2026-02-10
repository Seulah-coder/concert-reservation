package com.example.concert_reservation.domain.queue.components;

import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 대기열 검증 컴포넌트 (도메인 서비스)
 * Redis 기반 대기열 관리
 */
@Component
public class QueueValidator {
    
    private final RedisQueueRepository redisQueueRepository;
    
    public QueueValidator(RedisQueueRepository redisQueueRepository) {
        this.redisQueueRepository = redisQueueRepository;
    }
    
    /**
     * 토큰이 활성 상태인지 검증
     * @param token 검증할 토큰
     * @return 활성 상태이면 true
     */
    public boolean isActiveToken(QueueToken token) {
        return redisQueueRepository.isActiveToken(token.getValue());
    }
    
    /**
     * 사용자가 이미 활성 대기열을 보유하고 있는지 확인
     * @param userId 사용자 ID
     * @return 활성 대기열이 있으면 true
     */
    public boolean hasActiveQueue(String userId) {
        return redisQueueRepository.hasActiveQueue(userId);
    }
    
    /**
     * 사용자가 이미 대기 중인 대기열을 보유하고 있는지 확인
     * @param userId 사용자 ID
     * @return 대기 중인 대기열이 있으면 true
     */
    public boolean hasWaitingQueue(String userId) {
        return redisQueueRepository.hasWaitingQueue(userId);
    }
    
    /**
     * 토큰으로 대기열 조회 및 검증
     * @param token 조회할 토큰
     * @return 유효한 대기열 정보
     * @throws IllegalArgumentException 토큰이 유효하지 않은 경우
     */
    public UserQueue validateAndGetQueue(QueueToken token) {
        return redisQueueRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다"));
    }
    
    /**
     * 활성 토큰인지 검증 (예외 발생)
     * @param token 검증할 토큰
     * @throws IllegalStateException 토큰이 활성 상태가 아닌 경우
     */
    public void validateActiveToken(QueueToken token) {
        if (!isActiveToken(token)) {
            throw new IllegalStateException("토큰이 활성 상태가 아닙니다");
        }
    }
    
    /**
     * 대기열 앞에 있는 사람 수 조회
     * Redis ZSET rank를 사용하여 갭(gap)을 올바르게 처리
     * @param token 조회할 토큰
     * @return 앞에 대기 중인 사람 수
     */
    public long countWaitingAhead(QueueToken token) {
        return redisQueueRepository.countWaitingAhead(token.getValue());
    }
}
