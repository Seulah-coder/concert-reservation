package com.example.concert_reservation.domain.balance.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BalanceJpaRepository extends JpaRepository<BalanceEntity, Long> {
    Optional<BalanceEntity> findByUserId(String userId);
    
    /**
     * 사용자 ID로 잔액 조회 (비관적 락 적용)
     * 동시성 문제 방지를 위해 PESSIMISTIC_WRITE 락 사용
     * @param userId 사용자 ID
     * @return 잔액 (Lock이 걸린 상태)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BalanceEntity b WHERE b.userId = :userId")
    Optional<BalanceEntity> findByUserIdWithLock(@Param("userId") String userId);
}
