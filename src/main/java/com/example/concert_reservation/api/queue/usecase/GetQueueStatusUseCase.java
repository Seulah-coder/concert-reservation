package com.example.concert_reservation.api.queue.usecase;

import com.example.concert_reservation.api.queue.dto.QueueStatusResponse;
import com.example.concert_reservation.domain.queue.components.QueueActivationScheduler;
import com.example.concert_reservation.domain.queue.components.QueueValidator;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기열 상태 조회 유스케이스 (Redis 기반)
 * 
 * 비즈니스 흐름:
 * 1. 토큰으로 대기열 조회
 * 2. 대기 중이면 앞에 대기자 수 계산
 * 3. 예상 대기 시간 계산
 * 4. 상태 정보 반환 (폴링용)
 */
@Service
public class GetQueueStatusUseCase {
    
    private final QueueValidator queueValidator;
    
    public GetQueueStatusUseCase(QueueValidator queueValidator) {
        this.queueValidator = queueValidator;
    }
    
    /**
     * 대기열 상태 조회 (폴링용) - Redis 기반
     * 
     * @param tokenValue 조회할 토큰 값
     * @return 대기열 상태 정보 (대기 인원, 예상 대기 시간 등)
     * @throws IllegalArgumentException 토큰이 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public QueueStatusResponse execute(String tokenValue) {
        // 1. 토큰 검증 및 대기열 조회
        QueueToken token = QueueToken.of(tokenValue);
        UserQueue queue = queueValidator.validateAndGetQueue(token);
        
        // 2. 앞에 대기 중인 인원 수 계산 (WAITING 상태일 때만)
        Long waitingAhead = 0L;
        String estimatedWaitTime = "0분 0초";
        
        if (queue.isWaiting()) {
            waitingAhead = queueValidator.countWaitingAheadByToken(queue.getToken().getValue());
            estimatedWaitTime = QueueActivationScheduler.getEstimatedWaitTimeString(waitingAhead);
        }
        
        // 3. 응답 DTO 변환
        return new QueueStatusResponse(
            queue.getToken().getValue(),
            queue.getUserId(),
            queue.getQueueNumber(),
            queue.getStatus().name(),
            waitingAhead,
            estimatedWaitTime,
            queue.getEnteredAt(),
            queue.getExpiredAt()
        );
    }
}
