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
@DisplayName("ChargeBalanceUseCase 테스트")
class ChargeBalanceUseCaseTest {
    
    @Mock
    private BalanceManager balanceManager;
    
    @InjectMocks
    private ChargeBalanceUseCase chargeBalanceUseCase;
    
    @Test
    @DisplayName("잔액 충전에 성공한다")
    void execute_success() {
        // given
        String userId = "user123";
        BigDecimal amount = new BigDecimal("50000");
        Balance balance = Balance.create(userId);
        balance.charge(amount);
        
        given(balanceManager.chargeBalance(userId, amount)).willReturn(balance);
        
        // when
        BalanceResponse response = chargeBalanceUseCase.execute(userId, amount);
        
        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.amount()).isEqualByComparingTo(amount);
        verify(balanceManager).chargeBalance(userId, amount);
    }
    
    @Test
    @DisplayName("여러 번 충전에 성공한다")
    void execute_multipleTimes_success() {
        // given
        String userId = "user123";
        BigDecimal firstAmount = new BigDecimal("30000");
        BigDecimal secondAmount = new BigDecimal("20000");
        
        Balance balance1 = Balance.create(userId);
        balance1.charge(firstAmount);
        
        Balance balance2 = Balance.create(userId);
        balance2.charge(firstAmount);
        balance2.charge(secondAmount);
        
        given(balanceManager.chargeBalance(userId, firstAmount)).willReturn(balance1);
        given(balanceManager.chargeBalance(userId, secondAmount)).willReturn(balance2);
        
        // when
        BalanceResponse response1 = chargeBalanceUseCase.execute(userId, firstAmount);
        BalanceResponse response2 = chargeBalanceUseCase.execute(userId, secondAmount);
        
        // then
        assertThat(response1.amount()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(response2.amount()).isEqualByComparingTo(new BigDecimal("50000"));
    }
}
