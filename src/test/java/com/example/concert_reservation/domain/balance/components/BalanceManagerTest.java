package com.example.concert_reservation.domain.balance.components;

import com.example.concert_reservation.domain.balance.models.Balance;
import com.example.concert_reservation.domain.balance.repositories.BalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceManager 컴포넌트 테스트")
class BalanceManagerTest {
    
    @Mock
    private BalanceRepository balanceRepository;
    
    @InjectMocks
    private BalanceManager balanceManager;
    
    @Test
    @DisplayName("잔액 조회 - 존재하는 경우")
    void getOrCreateBalance_existing() {
        // given
        String userId = "user123";
        Balance existingBalance = Balance.create(userId);
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(existingBalance));
        
        // when
        Balance result = balanceManager.getOrCreateBalance(userId);
        
        // then
        assertThat(result).isEqualTo(existingBalance);
        verify(balanceRepository).findByUserId(userId);
        verify(balanceRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("잔액 조회 - 없으면 새로 생성")
    void getOrCreateBalance_notExisting_createsNew() {
        // given
        String userId = "user123";
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(balanceRepository.save(any(Balance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // when
        Balance result = balanceManager.getOrCreateBalance(userId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(balanceRepository).findByUserId(userId);
        verify(balanceRepository).save(any(Balance.class));
    }
    
    @Test
    @DisplayName("잔액 조회 - 없으면 예외 발생")
    void getBalance_notExisting_throwsException() {
        // given
        String userId = "user123";
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> balanceManager.getBalance(userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("잔액이 존재하지 않습니다");
    }
    
    @Test
    @DisplayName("잔액 충전 - 기존 잔액이 있는 경우")
    void chargeBalance_existing() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        given(balanceRepository.save(any(Balance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // when
        Balance result = balanceManager.chargeBalance(userId, new BigDecimal("10000"));
        
        // then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        verify(balanceRepository).findByUserId(userId);
        verify(balanceRepository).save(any(Balance.class));
    }
    
    @Test
    @DisplayName("잔액 충전 - 잔액이 없으면 생성 후 충전")
    void chargeBalance_notExisting_createsAndCharges() {
        // given
        String userId = "user123";
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(balanceRepository.save(any(Balance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // when
        Balance result = balanceManager.chargeBalance(userId, new BigDecimal("10000"));
        
        // then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        verify(balanceRepository, times(2)).save(any(Balance.class)); // 생성 + 충전
    }
    
    @Test
    @DisplayName("잔액 사용 - 성공")
    void useBalance_success() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        balance.charge(new BigDecimal("50000"));
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        given(balanceRepository.save(any(Balance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // when
        Balance result = balanceManager.useBalance(userId, new BigDecimal("30000"));
        
        // then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        verify(balanceRepository).findByUserId(userId);
        verify(balanceRepository).save(any(Balance.class));
    }
    
    @Test
    @DisplayName("잔액 사용 - 잔액 부족 시 예외")
    void useBalance_insufficientBalance_throwsException() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        balance.charge(new BigDecimal("10000"));
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        
        // when & then
        assertThatThrownBy(() -> balanceManager.useBalance(userId, new BigDecimal("20000")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("잔액이 부족합니다");
        
        verify(balanceRepository).findByUserId(userId);
        verify(balanceRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("잔액 사용 - 잔액이 없으면 예외")
    void useBalance_notExisting_throwsException() {
        // given
        String userId = "user123";
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> balanceManager.useBalance(userId, new BigDecimal("10000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("잔액이 존재하지 않습니다");
    }
    
    @Test
    @DisplayName("잔액 환불 - 성공")
    void refundBalance_success() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        balance.charge(new BigDecimal("50000"));
        balance.use(new BigDecimal("30000"));
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        given(balanceRepository.save(any(Balance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // when
        Balance result = balanceManager.refundBalance(userId, new BigDecimal("15000"));
        
        // then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("35000"));
        verify(balanceRepository).findByUserId(userId);
        verify(balanceRepository).save(any(Balance.class));
    }
    
    @Test
    @DisplayName("충분한 잔액 확인 - 충분한 경우")
    void hasSufficientBalance_sufficient() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        balance.charge(new BigDecimal("50000"));
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        
        // when
        boolean result = balanceManager.hasSufficientBalance(userId, new BigDecimal("30000"));
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("충분한 잔액 확인 - 부족한 경우")
    void hasSufficientBalance_insufficient() {
        // given
        String userId = "user123";
        Balance balance = Balance.create(userId);
        balance.charge(new BigDecimal("10000"));
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        
        // when
        boolean result = balanceManager.hasSufficientBalance(userId, new BigDecimal("20000"));
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("충분한 잔액 확인 - 잔액이 없는 경우")
    void hasSufficientBalance_notExisting() {
        // given
        String userId = "user123";
        given(balanceRepository.findByUserId(userId)).willReturn(Optional.empty());
        
        // when
        boolean result = balanceManager.hasSufficientBalance(userId, new BigDecimal("10000"));
        
        // then
        assertThat(result).isFalse();
    }
}
