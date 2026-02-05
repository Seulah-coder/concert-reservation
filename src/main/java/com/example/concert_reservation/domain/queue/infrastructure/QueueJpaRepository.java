package com.example.concert_reservation.domain.queue.infrastructure;

import com.example.concert_reservation.domain.queue.infrastructure.entity.UserQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 대기열 JPA Repository
 * Spring Data JPA를 사용한 데이터베이스 접근
 */
public interface QueueJpaRepository extends JpaRepository<UserQueueEntity, Long> {
    
    /**
     * 토큰으로 대기열 조회
     */
    Optional<UserQueueEntity> findByToken(String token);
    
    /**
     * 사용자 ID로 대기열 조회
     */
    List<UserQueueEntity> findByUserId(String userId);
    
    /**
     * 특정 상태의 대기열 개수 조회
     */
    long countByStatus(String status);
    
    /**
     * 특정 상태의 대기열 조회 (입장 시간 오름차순, 제한)
     */
    List<UserQueueEntity> findByStatusOrderByEnteredAtAsc(String status);
    
    /**
     * 사용자 ID와 상태로 대기열 존재 여부 확인
     */
    boolean existsByUserIdAndStatus(String userId, String status);
    
    /**
     * 최대 대기 번호 조회
     */
    @Query("SELECT COALESCE(MAX(q.queueNumber), 0) FROM UserQueueEntity q")
    Long findMaxQueueNumber();
    
    /**
     * 특정 상태이면서 대기 번호가 지정된 번호보다 작은 대기열 개수 조회
     */
    long countByStatusAndQueueNumberLessThan(String status, Long queueNumber);
}
