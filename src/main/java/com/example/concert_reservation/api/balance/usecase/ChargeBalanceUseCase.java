package com.example.concert_reservation.api.balance.usecase;

import com.example.concert_reservation.api.balance.dto.BalanceResponse;
import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ChargeBalanceUseCase {
    
    private final BalanceManager balanceManager;
    
    public ChargeBalanceUseCase(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }
    
    @Transactional
    public BalanceResponse execute(String userId, BigDecimal amount) {
        Balance balance = balanceManager.chargeBalance(userId, amount);
        return BalanceResponse.from(balance);
    }
}
