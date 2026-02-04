package com.example.concert_reservation.domain.balance.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Balance 도메인 모델 테스트")
class BalanceTest {
    
    @Test
    @DisplayName("새로운 잔액을 생성할 수 있다 (초기 잔액 0원)")
    void create_success() {
        // when
        Balance balance = Balance.create("user123");
        
        // then
        assertThat(balance).isNotNull();
        assertThat(balance.getId()).isNull(); // 아직 저장 전
        assertThat(balance.getUserId()).isEqualTo("user123");
        assertThat(balance.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getCreatedAt()).isNotNull();
        assertThat(balance.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("사용자 ID 없이 잔액을 생성하면 예외가 발생한다")
    void create_withNullUserId_throwsException() {
        // when & then
        assertThatThrownBy(() -> Balance.create(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("빈 사용자 ID로 잔액을 생성하면 예외가 발생한다")
    void create_withEmptyUserId_throwsException() {
        // when & then
        assertThatThrownBy(() -> Balance.create("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("잔액을 충전할 수 있다")
    void charge_success() throws InterruptedException {
        // given
        Balance balance = Balance.create("user123");
        BigDecimal initialAmount = balance.getAmount();
        LocalDateTime initialUpdatedAt = balance.getUpdatedAt();
        
        Thread.sleep(10); // updatedAt 변경 확인을 위한 시간 간격
        
        // when
        balance.charge(new BigDecimal("10000"));
        
        // then
        assertThat(balance.getAmount()).isEqualByComparingTo(initialAmount.add(new BigDecimal("10000")));
        assertThat(balance.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
    
    @Test
    @DisplayName("여러 번 충전할 수 있다")
    void charge_multiple_success() {
        // given
        Balance balance = Balance.create("user123");
        
        // when
        balance.charge(new BigDecimal("10000"));
        balance.charge(new BigDecimal("20000"));
        balance.charge(new BigDecimal("5000"));
        
        // then
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("35000"));
    }
    
    @Test
    @DisplayName("0원 이하로 충전하면 예외가 발생한다")
    void charge_withZeroOrNegative_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        
        // when & then
        assertThatThrownBy(() -> balance.charge(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("충전 금액은 0보다 커야 합니다");
        
        assertThatThrownBy(() -> balance.charge(new BigDecimal("-1000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("충전 금액은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("null 금액으로 충전하면 예외가 발생한다")
    void charge_withNull_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        
        // when & then
        assertThatThrownBy(() -> balance.charge(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("충전 금액은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("잔액을 사용할 수 있다")
    void use_success() throws InterruptedException {
        // given
        Balance balance = Balance.create("user123");
        balance.charge(new BigDecimal("50000"));
        BigDecimal beforeAmount = balance.getAmount();
        LocalDateTime beforeUpdatedAt = balance.getUpdatedAt();
        
        Thread.sleep(10); // updatedAt 변경 확인을 위한 시간 간격
        
        // when
        balance.use(new BigDecimal("20000"));
        
        // then
        assertThat(balance.getAmount()).isEqualByComparingTo(beforeAmount.subtract(new BigDecimal("20000")));
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(balance.getUpdatedAt()).isAfter(beforeUpdatedAt);
    }
    
    @Test
    @DisplayName("잔액이 부족하면 사용할 수 없다")
    void use_insufficientBalance_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        balance.charge(new BigDecimal("10000"));
        
        // when & then
        assertThatThrownBy(() -> balance.use(new BigDecimal("20000")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("잔액이 부족합니다")
            .hasMessageContaining("현재 잔액: 10000")
            .hasMessageContaining("사용 금액: 20000");
        
        // 잔액은 변경되지 않음
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }
    
    @Test
    @DisplayName("0원 이하로 사용하면 예외가 발생한다")
    void use_withZeroOrNegative_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        balance.charge(new BigDecimal("10000"));
        
        // when & then
        assertThatThrownBy(() -> balance.use(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용 금액은 0보다 커야 합니다");
        
        assertThatThrownBy(() -> balance.use(new BigDecimal("-1000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용 금액은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("null 금액으로 사용하면 예외가 발생한다")
    void use_withNull_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        
        // when & then
        assertThatThrownBy(() -> balance.use(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용 금액은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("잔액을 환불할 수 있다")
    void refund_success() throws InterruptedException {
        // given
        Balance balance = Balance.create("user123");
        balance.charge(new BigDecimal("50000"));
        balance.use(new BigDecimal("30000"));
        BigDecimal beforeAmount = balance.getAmount();
        LocalDateTime beforeUpdatedAt = balance.getUpdatedAt();
        
        Thread.sleep(10); // updatedAt 변경 확인을 위한 시간 간격
        
        // when
        balance.refund(new BigDecimal("15000"));
        
        // then
        assertThat(balance.getAmount()).isEqualByComparingTo(beforeAmount.add(new BigDecimal("15000")));
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(balance.getUpdatedAt()).isAfter(beforeUpdatedAt);
    }
    
    @Test
    @DisplayName("0원 이하로 환불하면 예외가 발생한다")
    void refund_withZeroOrNegative_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        
        // when & then
        assertThatThrownBy(() -> balance.refund(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("환불 금액은 0보다 커야 합니다");
        
        assertThatThrownBy(() -> balance.refund(new BigDecimal("-1000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("환불 금액은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("null 금액으로 환불하면 예외가 발생한다")
    void refund_withNull_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        
        // when & then
        assertThatThrownBy(() -> balance.refund(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("환불 금액은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("충분한 잔액이 있는지 확인할 수 있다")
    void hasSufficientBalance_success() {
        // given
        Balance balance = Balance.create("user123");
        balance.charge(new BigDecimal("50000"));
        
        // when & then
        assertThat(balance.hasSufficientBalance(new BigDecimal("30000"))).isTrue();
        assertThat(balance.hasSufficientBalance(new BigDecimal("50000"))).isTrue();
        assertThat(balance.hasSufficientBalance(new BigDecimal("60000"))).isFalse();
        assertThat(balance.hasSufficientBalance(BigDecimal.ZERO)).isTrue();
    }
    
    @Test
    @DisplayName("null 금액으로 잔액 확인하면 예외가 발생한다")
    void hasSufficientBalance_withNull_throwsException() {
        // given
        Balance balance = Balance.create("user123");
        
        // when & then
        assertThatThrownBy(() -> balance.hasSufficientBalance(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("필요 금액은 필수입니다");
    }
    
    @Test
    @DisplayName("복합 시나리오: 충전 → 사용 → 환불")
    void complexScenario_chargeUseRefund() {
        // given
        Balance balance = Balance.create("user123");
        
        // when
        balance.charge(new BigDecimal("100000"));  // 충전 100,000원
        balance.use(new BigDecimal("30000"));       // 사용 30,000원
        balance.use(new BigDecimal("20000"));       // 사용 20,000원
        balance.refund(new BigDecimal("10000"));    // 환불 10,000원
        
        // then
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("60000"));
    }
    
    @Test
    @DisplayName("of 메서드로 기존 잔액을 재구성할 수 있다")
    void of_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // when
        Balance balance = Balance.of(
            1L, 
            "user123", 
            new BigDecimal("50000"),
            now.minusDays(1),
            now,
            0L
        );
        
        // then
        assertThat(balance.getId()).isEqualTo(1L);
        assertThat(balance.getUserId()).isEqualTo("user123");
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(balance.getCreatedAt()).isEqualTo(now.minusDays(1));
        assertThat(balance.getUpdatedAt()).isEqualTo(now);
        assertThat(balance.getVersion()).isEqualTo(0L);
    }
}
