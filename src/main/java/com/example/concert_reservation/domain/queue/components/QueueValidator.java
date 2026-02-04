package com.example.concert_reservation.domain.queue.components;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 대기열 검증 컴포넌트 (도메인 서비스)
 * 대기열 관련 비즈니스 검증 로직을 담당
 */
@Component
public class QueueValidator {
    
    private final QueueStoreRepository queueStoreRepository;
    
    public QueueValidator(QueueStoreRepository queueStoreRepository) {
        this.queueStoreRepository = queueStoreRepository;
    }
    
    /**
     * 토큰이 활성 상태인지 검증
     * @param token 검증할 토큰
     * @return 활성 상태이면 true
     */
    public boolean isActiveToken(QueueToken token) {
        Optional<UserQueue> queueOpt = queueStoreRepository.findByToken(token);
        
        if (queueOpt.isEmpty()) {
            return false;
        }
        
        UserQueue queue = queueOpt.get();
        return queue.isActive();
    }
    
    /**
     * 사용자가 이미 활성 대기열을 보유하고 있는지 확인
     * @param userId 사용자 ID
     * @return 활성 대기열이 있으면 true
     */
    public boolean hasActiveQueue(String userId) {
        return queueStoreRepository.existsByUserIdAndStatus(userId, QueueStatus.ACTIVE);
    }
    
    /**
     * 사용자가 이미 대기 중인 대기열을 보유하고 있는지 확인
     * @param userId 사용자 ID
     * @return 대기 중인 대기열이 있으면 true
     */
    public boolean hasWaitingQueue(String userId) {
        return queueStoreRepository.existsByUserIdAndStatus(userId, QueueStatus.WAITING);
    }
    
    /**
     * 토큰으로 대기열 조회 및 검증
     * @param token 조회할 토큰
     * @return 유효한 대기열 정보
     * @throws IllegalArgumentException 토큰이 유효하지 않은 경우
     */
    public UserQueue validateAndGetQueue(QueueToken token) {
        return queueStoreRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다"));
    }
    
    /**
     * 활성 토큰인지 검증 (예외 발생)
     * @param token 검증할 토큰
     * @throws IllegalStateException 토큰이 활성 상태가 아닌 경우
     */
    public void validateActiveToken(QueueToken token) {
        UserQueue queue = validateAndGetQueue(token);
        
        if (!queue.isActive()) {
            throw new IllegalStateException(
                "토큰이 활성 상태가 아닙니다. 현재 상태: " + queue.getStatus()
            );
        }
    }
    
    /**
     * 대기열 앞에 있는 사람 수 조회
     * @param queueNumber 조회할 대기 번호
     * @return 앞에 대기 중인 사람 수 (WAITING 상태이면서 queueNumber가 더 작은 사람들)
     */
    public long countWaitingAhead(Long queueNumber) {
        // 실제로 대기 중이면서 현재 번호보다 앞선 사람들을 카운트
        return queueStoreRepository.countByStatusAndQueueNumberLessThan(
            QueueStatus.WAITING, 
            queueNumber
        );
    }
}
