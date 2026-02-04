package com.example.concert_reservation.domain.queue.repositories;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;

import java.util.List;
import java.util.Optional;

/**
 * 대기열 저장소 인터페이스 (도메인 계층)
 * 순수 비즈니스 로직만 정의하며, 구현은 infrastructure 계층에서 담당
 */
public interface QueueStoreRepository {
    
    /**
     * 대기열 정보 저장
     * @param userQueue 저장할 대기열 정보
     * @return 저장된 대기열 정보 (ID 포함)
     */
    UserQueue save(UserQueue userQueue);
    
    /**
     * 토큰으로 대기열 조회
     * @param token 조회할 토큰
     * @return 대기열 정보 (Optional)
     */
    Optional<UserQueue> findByToken(QueueToken token);
    
    /**
     * 사용자 ID로 대기열 조회
     * @param userId 사용자 ID
     * @return 대기열 정보 리스트
     */
    List<UserQueue> findByUserId(String userId);
    
    /**
     * 특정 상태의 대기열 개수 조회
     * @param status 조회할 상태
     * @return 해당 상태의 대기열 개수
     */
    long countByStatus(QueueStatus status);
    
    /**
     * 특정 상태의 대기열 조회 (정렬: 입장 시간 오름차순)
     * @param status 조회할 상태
     * @param limit 조회 개수 제한
     * @return 대기열 정보 리스트
     */
    List<UserQueue> findByStatusOrderByEnteredAt(QueueStatus status, int limit);
    
    /**
     * 특정 사용자의 활성 대기열 존재 여부 확인
     * @param userId 사용자 ID
     * @return 활성 대기열이 있으면 true
     */
    boolean existsByUserIdAndStatus(String userId, QueueStatus status);
    
    /**
     * 다음 대기 번호 조회 (현재 최대 번호 + 1)
     * @return 다음 대기 번호
     */
    long getNextQueueNumber();
    
    /**
     * 특정 상태이면서 대기 번호가 지정된 번호보다 작은 대기열 개수 조회
     * @param status 조회할 상태
     * @param queueNumber 기준 대기 번호
     * @return 조건을 만족하는 대기열 개수
     */
    long countByStatusAndQueueNumberLessThan(QueueStatus status, Long queueNumber);
}
