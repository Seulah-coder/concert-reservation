package com.example.concert_reservation.api.queue.usecase;

import com.example.concert_reservation.api.queue.dto.IssueTokenRequest;
import com.example.concert_reservation.api.queue.dto.IssueTokenResponse;
import com.example.concert_reservation.domain.queue.components.QueueActivationScheduler;
import com.example.concert_reservation.domain.queue.components.QueueValidator;
import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기열 토큰 발급 유스케이스 (Redis 기반)
 * 
 * 비즈니스 흐름:
 * 1. 사용자가 이미 활성/대기 중인 토큰이 있는지 확인
 * 2. 없으면 Redis Sorted Set에 추가 (WAITING 상태)
 * 3. 대기 순번 자동 부여
 * 4. 예상 대기 시간 계산
 */
@Service
public class IssueQueueTokenUseCase {
    
    private final RedisQueueRepository redisQueueRepository;
    private final QueueValidator queueValidator;
    
    public IssueQueueTokenUseCase(RedisQueueRepository redisQueueRepository,
                                  QueueValidator queueValidator) {
        this.redisQueueRepository = redisQueueRepository;
        this.queueValidator = queueValidator;
    }
    
    /**
     * 대기열 토큰 발급
     * 
     * @param request 토큰 발급 요청 (userId 포함)
     * @return 발급된 토큰 정보
     * @throws IllegalStateException 사용자가 이미 활성/대기 중인 토큰을 보유한 경우
     */
    @Transactional
    public IssueTokenResponse execute(IssueTokenRequest request) {
        String userId = request.getUserId();
        
        // 1. 중복 토큰 체크
        if (queueValidator.hasActiveQueue(userId)) {
            throw new IllegalStateException("이미 활성 상태의 토큰이 존재합니다");
        }
        
        if (queueValidator.hasWaitingQueue(userId)) {
            throw new IllegalStateException("이미 대기 중인 토큰이 존재합니다");
        }
        
        // 2. Redis Waiting Queue에 추가
        UserQueue newQueue = redisQueueRepository.addToWaitingQueue(userId);
        
        // 3. 예상 대기 시간 계산
        String estimatedWaitTime = QueueActivationScheduler.getEstimatedWaitTimeString(
            newQueue.getQueueNumber()
        );
        
        // 4. 응답 생성
        return new IssueTokenResponse(
            newQueue.getToken().getValue(),
            newQueue.getUserId(),
            newQueue.getQueueNumber(),
            newQueue.getStatus().name(),
            estimatedWaitTime,
            newQueue.getEnteredAt()
        );
    }
}
        
        // 6. 응답 DTO 변환
        return new IssueTokenResponse(
            savedQueue.getToken().getValue(),
            savedQueue.getQueueNumber(),
            savedQueue.getStatus().name(),
            savedQueue.getEnteredAt()
        );
    }
}
