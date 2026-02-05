package com.example.concert_reservation.domain.balance.components;

import com.example.concert_reservation.domain.balance.models.Balance;
import com.example.concert_reservation.domain.balance.repositories.BalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 잔액 관리 비즈니스 로직
 * 도메인 레이어의 컴포넌트
 */
@Component
public class BalanceManager {
    
    private static final Logger log = LoggerFactory.getLogger(BalanceManager.class);
    
    private final BalanceRepository balanceRepository;
    
    public BalanceManager(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }
    
    /**
     * 사용자 잔액 조회 (없으면 새로 생성)
     * @param userId 사용자 ID
     * @return 사용자 잔액
     */
    public Balance getOrCreateBalance(String userId) {
        return balanceRepository.findByUserId(userId)
            .orElseGet(() -> {
                Balance newBalance = Balance.create(userId);
                return balanceRepository.save(newBalance);
            });
    }
    
    /**
     * 사용자 잔액 조회 (없으면 예외)
     * @param userId 사용자 ID
     * @return 사용자 잔액
     * @throws IllegalArgumentException 잔액이 존재하지 않는 경우
     */
    public Balance getBalance(String userId) {
        return balanceRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("잔액이 존재하지 않습니다. 사용자 ID: " + userId));
    }
    
    /**
     * 잔액 충전
     * @param userId 사용자 ID
     * @param amount 충전 금액
     * @return 충전된 잔액
     */
    public Balance chargeBalance(String userId, BigDecimal amount) {
        log.info("잔액 충전 시작 - userId: {}, amount: {}", userId, amount);
        Balance balance = getOrCreateBalance(userId);
        balance.charge(amount);
        Balance saved = balanceRepository.save(balance);
        log.info("잔액 충전 완료 - userId: {}, newBalance: {}", userId, saved.getAmount());
        return saved;
    }
    
    /**
     * 잔액 사용
     * @param userId 사용자 ID
     * @param amount 사용 금액
     * @return 사용 후 잔액
     * @throws IllegalStateException 잔액이 부족한 경우
     */
    public Balance useBalance(String userId, BigDecimal amount) {
        log.info("잔액 사용 시작 - userId: {}, amount: {}", userId, amount);
        Balance balance = getBalance(userId);
        balance.use(amount);
        Balance saved = balanceRepository.save(balance);
        log.info("잔액 사용 완료 - userId: {}, newBalance: {}", userId, saved.getAmount());
        return saved;
    }
    
    /**
     * 잔액 환불
     * @param userId 사용자 ID
     * @param amount 환불 금액
     * @return 환불 후 잔액
     */
    public Balance refundBalance(String userId, BigDecimal amount) {
        Balance balance = getBalance(userId);
        balance.refund(amount);
        return balanceRepository.save(balance);
    }
    
    /**
     * 잔액이 충분한지 확인
     * @param userId 사용자 ID
     * @param requiredAmount 필요한 금액
     * @return 잔액이 충분하면 true
     */
    public boolean hasSufficientBalance(String userId, BigDecimal requiredAmount) {
        return balanceRepository.findByUserId(userId)
            .map(balance -> balance.hasSufficientBalance(requiredAmount))
            .orElse(false);
    }
}
