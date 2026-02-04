package com.example.concert_reservation.domain.balance.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BalanceJpaRepository extends JpaRepository<BalanceEntity, Long> {
    Optional<BalanceEntity> findByUserId(String userId);
}
