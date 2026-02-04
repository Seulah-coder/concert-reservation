package com.example.concert_reservation.api.queue.usecase;

import com.example.concert_reservation.api.queue.dto.IssueTokenRequest;
import com.example.concert_reservation.api.queue.dto.IssueTokenResponse;
import com.example.concert_reservation.domain.queue.components.QueueValidator;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기열 토큰 발급 유스케이스
 * 
 * 비즈니스 흐름:
 * 1. 사용자가 이미 활성/대기 중인 토큰이 있는지 확인
 * 2. 없으면 새로운 대기열 생성 (WAITING 상태)
 * 3. 대기 번호 자동 부여
 * 4. 토큰 반환
 */
@Service
public class IssueQueueTokenUseCase {
    
    private final QueueStoreRepository queueStoreRepository;
    private final QueueValidator queueValidator;
    
    public IssueQueueTokenUseCase(QueueStoreRepository queueStoreRepository,
                                  QueueValidator queueValidator) {
        this.queueStoreRepository = queueStoreRepository;
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
        
        // 1. 중복 토큰 체크 - 이미 활성 토큰이 있으면 발급 불가
        if (queueValidator.hasActiveQueue(userId)) {
            throw new IllegalStateException("이미 활성 상태의 토큰이 존재합니다");
        }
        
        // 2. 대기 중인 토큰도 체크
        if (queueValidator.hasWaitingQueue(userId)) {
            throw new IllegalStateException("이미 대기 중인 토큰이 존재합니다");
        }
        
        // 3. 다음 대기 번호 조회
        long nextQueueNumber = queueStoreRepository.getNextQueueNumber();
        
        // 4. 새로운 대기열 생성 (WAITING 상태)
        UserQueue newQueue = UserQueue.create(userId, nextQueueNumber);
        
        // 5. 저장
        UserQueue savedQueue = queueStoreRepository.save(newQueue);
        
        // 6. 응답 DTO 변환
        return new IssueTokenResponse(
            savedQueue.getToken().getValue(),
            savedQueue.getQueueNumber(),
            savedQueue.getStatus().name(),
            savedQueue.getEnteredAt()
        );
    }
}
