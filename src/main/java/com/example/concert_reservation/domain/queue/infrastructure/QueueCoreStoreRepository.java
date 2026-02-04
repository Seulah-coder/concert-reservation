package com.example.concert_reservation.domain.queue.infrastructure;

import com.example.concert_reservation.domain.queue.infrastructure.entity.UserQueueEntity;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 대기열 저장소 구현체
 * 도메인 모델(UserQueue)과 JPA 엔티티(UserQueueEntity) 간의 변환을 담당
 */
@Repository
public class QueueCoreStoreRepository implements QueueStoreRepository {
    
    private final QueueJpaRepository jpaRepository;
    
    public QueueCoreStoreRepository(QueueJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    @Transactional
    public UserQueue save(UserQueue userQueue) {
        UserQueueEntity entity = toEntity(userQueue);
        UserQueueEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserQueue> findByToken(QueueToken token) {
        return jpaRepository.findByToken(token.getValue())
            .map(this::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserQueue> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByStatus(QueueStatus status) {
        return jpaRepository.countByStatus(status.name());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserQueue> findByStatusOrderByEnteredAt(QueueStatus status, int limit) {
        return jpaRepository.findByStatusOrderByEnteredAtAsc(status.name()).stream()
            .limit(limit)
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndStatus(String userId, QueueStatus status) {
        return jpaRepository.existsByUserIdAndStatus(userId, status.name());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getNextQueueNumber() {
        Long maxQueueNumber = jpaRepository.findMaxQueueNumber();
        return (maxQueueNumber != null ? maxQueueNumber : 0L) + 1;
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByStatusAndQueueNumberLessThan(QueueStatus status, Long queueNumber) {
        return jpaRepository.countByStatusAndQueueNumberLessThan(status.name(), queueNumber);
    }
    
    /**
     * 도메인 모델 → JPA 엔티티 변환
     */
    private UserQueueEntity toEntity(UserQueue domain) {
        return UserQueueEntity.of(
            domain.getId(),
            domain.getToken().getValue(),
            domain.getUserId(),
            domain.getQueueNumber(),
            domain.getStatus().name(),
            domain.getEnteredAt(),
            domain.getExpiredAt()
        );
    }
    
    /**
     * JPA 엔티티 → 도메인 모델 변환
     */
    private UserQueue toDomain(UserQueueEntity entity) {
        return UserQueue.of(
            entity.getId(),
            QueueToken.of(entity.getToken()),
            entity.getUserId(),
            entity.getQueueNumber(),
            QueueStatus.valueOf(entity.getStatus()),
            entity.getEnteredAt(),
            entity.getExpiredAt()
        );
    }
}
