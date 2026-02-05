package com.example.concert_reservation.domain.balance.infrastructure;

import com.example.concert_reservation.domain.balance.models.Balance;
import com.example.concert_reservation.domain.balance.repositories.BalanceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BalanceStoreRepository implements BalanceRepository {
    
    private final BalanceJpaRepository balanceJpaRepository;
    
    public BalanceStoreRepository(BalanceJpaRepository balanceJpaRepository) {
        this.balanceJpaRepository = balanceJpaRepository;
    }
    
    @Override
    public Optional<Balance> findByUserId(String userId) {
        return balanceJpaRepository.findByUserId(userId)
            .map(BalanceEntity::toDomain);
    }
    
    @Override
    public Optional<Balance> findByUserIdWithLock(String userId) {
        return balanceJpaRepository.findByUserIdWithLock(userId)
            .map(BalanceEntity::toDomain);
    }
    
    @Override
    public Balance save(Balance balance) {
        if (balance.getId() == null) {
            // 새 엔티티 생성
            BalanceEntity entity = BalanceEntity.from(balance);
            BalanceEntity saved = balanceJpaRepository.save(entity);
            return saved.toDomain();
        } else {
            // 기존 엔티티 업데이트
            BalanceEntity entity = balanceJpaRepository.findById(balance.getId())
                .orElseThrow(() -> new IllegalArgumentException("잔액 엔티티를 찾을 수 없습니다: " + balance.getId()));
            entity.updateFrom(balance);
            BalanceEntity saved = balanceJpaRepository.save(entity);
            return saved.toDomain();
        }
    }
}
