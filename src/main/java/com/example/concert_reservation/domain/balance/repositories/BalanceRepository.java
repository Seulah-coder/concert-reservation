package com.example.concert_reservation.domain.balance.repositories;

import com.example.concert_reservation.domain.balance.models.Balance;

import java.util.Optional;

/**
 * 잔액 저장소 인터페이스
 * 도메인 레이어 - 구현은 infrastructure 레이어에서
 */
public interface BalanceRepository {
    
    /**
     * 사용자 ID로 잔액 조회
     * @param userId 사용자 ID
     * @return 잔액 (없으면 empty)
     */
    Optional<Balance> findByUserId(String userId);
    
    /**
     * 잔액 저장
     * @param balance 저장할 잔액
     * @return 저장된 잔액
     */
    Balance save(Balance balance);
}
