package com.example.concert_reservation.api.balance.usecase;

import com.example.concert_reservation.api.balance.dto.BalanceResponse;
import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBalanceUseCase {
    
    private final BalanceManager balanceManager;
    
    public GetBalanceUseCase(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }
    
    @Transactional(readOnly = true)
    public BalanceResponse execute(String userId) {
        Balance balance = balanceManager.getOrCreateBalance(userId);
        return BalanceResponse.from(balance);
    }
}
