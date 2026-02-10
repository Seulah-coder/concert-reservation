package com.example.concert_reservation.domain.queue.infrastructure;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Redis 기반 QueueStoreRepository 구현체 (Adapter)
 * QueueTokenInterceptor에서 사용할 수 있도록 RedisQueueRepository를 래핑
 */
@Repository
@Primary
public class RedisQueueStoreRepository implements QueueStoreRepository {
    
    private final RedisQueueRepository redisQueueRepository;
    
    public RedisQueueStoreRepository(RedisQueueRepository redisQueueRepository) {
        this.redisQueueRepository = redisQueueRepository;
    }
    
    @Override
    public UserQueue save(UserQueue userQueue) {
        // Redis에 저장 (addToWaitingQueue 사용)
        return redisQueueRepository.addToWaitingQueue(userQueue.getUserId());
    }
    
    @Override
    public Optional<UserQueue> findByToken(QueueToken token) {
        // Redis에서 토큰 조회
        return redisQueueRepository.findByToken(token);
    }
    
    @Override
    public List<UserQueue> findByUserId(String userId) {
        // Redis에서 사용자 ID로 조회 (구현 필요시)
        throw new UnsupportedOperationException("Redis에서는 미지원");
    }
    
    @Override
    public long countByStatus(QueueStatus status) {
        // Redis에서 상태별 카운트
        if (status == QueueStatus.WAITING) {
            return redisQueueRepository.getWaitingQueueSize();
        } else if (status == QueueStatus.ACTIVE) {
            return redisQueueRepository.getActiveQueueSize();
        }
        return 0;
    }
    
    @Override
    public List<UserQueue> findByStatusOrderByEnteredAt(QueueStatus status, int limit) {
        // Redis에서 상태별 정렬 조회 (구현 필요시)
        throw new UnsupportedOperationException("Redis에서는 미지원");
    }
    
    @Override
    public boolean existsByUserIdAndStatus(String userId, QueueStatus status) {
        // Redis에서 존재 여부 확인
        if (status == QueueStatus.ACTIVE) {
            return redisQueueRepository.hasActiveQueue(userId);
        } else if (status == QueueStatus.WAITING) {
            return redisQueueRepository.hasWaitingQueue(userId);
        }
        return false;
    }
    
    @Override
    public long getNextQueueNumber() {
        // Redis에서 다음 대기 번호 조회
        return redisQueueRepository.getWaitingQueueSize() + 1;
    }
    
    @Override
    public long countByStatusAndQueueNumberLessThan(QueueStatus status, Long queueNumber) {
        // Redis에서 특정 번호보다 작은 대기열 카운트 (구현 필요시)
        throw new UnsupportedOperationException("Redis에서는 미지원");
    }
}
