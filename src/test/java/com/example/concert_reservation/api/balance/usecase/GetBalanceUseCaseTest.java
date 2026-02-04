package com.example.concert_reservation.api.balance.usecase;

import com.example.concert_reservation.api.balance.dto.BalanceResponse;
import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetBalanceUseCase 테스트")
class GetBalanceUseCaseTest {
    
    @Mock
    private BalanceManager balanceManager;
    
    @InjectMocks
    private GetBalanceUseCase getBalanceUseCase;
    
    @Test
    @DisplayName("잔액 조회에 성공한다")
    void execute_success() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        balance.charge(new BigDecimal("50000"));
        
        given(balanceManager.getOrCreateBalance(userId)).willReturn(balance);
        
        // when
        BalanceResponse response = getBalanceUseCase.execute(userId);
        
        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50000"));
        verify(balanceManager).getOrCreateBalance(userId);
    }
    
    @Test
    @DisplayName("잔액이 없을 때 조회하면 0원 잔액이 생성된다")
    void execute_notExists_createsZeroBalance() {
        // given
        String userId = "newUser";
        Balance balance = Balance.create(userId);
        
        given(balanceManager.getOrCreateBalance(userId)).willReturn(balance);
        
        // when
        BalanceResponse response = getBalanceUseCase.execute(userId);
        
        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(balanceManager).getOrCreateBalance(userId);
    }
}
