package com.example.concert_reservation.domain.balance.infrastructure;

import com.example.concert_reservation.domain.balance.models.Balance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.sql.init.mode=never"
})
@Import(BalanceStoreRepository.class)
@DisplayName("Balance 인프라 계층 테스트")
class BalanceStoreRepositoryTest {
    
    @Autowired
    private BalanceStoreRepository balanceStoreRepository;
    
    @Autowired
    private BalanceJpaRepository balanceJpaRepository;
    
    @Test
    @DisplayName("잔액을 저장할 수 있다")
    void save_newBalance_success() {
        // given
        Balance balance = Balance.create("user123");
        
        // when
        Balance saved = balanceStoreRepository.save(balance);
        
        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("user123");
        assertThat(saved.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("userId로 잔액을 조회할 수 있다")
    void findByUserId_exists_returnsBalance() {
        // given
        Balance balance = Balance.create("user123");
        balanceStoreRepository.save(balance);
        
        // when
        Optional<Balance> result = balanceStoreRepository.findByUserId("user123");
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user123");
    }
    
    @Test
    @DisplayName("존재하지 않는 userId는 Empty를 반환한다")
    void findByUserId_notExists_returnsEmpty() {
        // when
        Optional<Balance> result = balanceStoreRepository.findByUserId("nonexistent");
        
        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("기존 잔액을 업데이트할 수 있다")
    void save_existingBalance_updates() {
        // given
        Balance balance = Balance.create("user123");
        Balance saved = balanceStoreRepository.save(balance);
        
        // 잔액 충전
        saved.charge(new BigDecimal("50000"));
        
        // when
        Balance updated = balanceStoreRepository.save(saved);
        
        // then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
    }
    
    @Test
    @DisplayName("동일한 userId로 중복 저장 시 예외가 발생한다")
    void save_duplicateUserId_throwsException() {
        // given
        Balance balance1 = Balance.create("user123");
        balanceStoreRepository.save(balance1);
        
        // when & then
        Balance balance2 = Balance.create("user123");
        assertThatThrownBy(() -> balanceStoreRepository.save(balance2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("여러 잔액을 저장하고 개별 조회할 수 있다")
    void save_multipleBalances_canQueryIndividually() {
        // given
        Balance balance1 = Balance.create("user1");
        Balance balance2 = Balance.create("user2");
        Balance balance3 = Balance.create("user3");
        
        // when
        balanceStoreRepository.save(balance1);
        balanceStoreRepository.save(balance2);
        balanceStoreRepository.save(balance3);
        
        // then
        assertThat(balanceStoreRepository.findByUserId("user1")).isPresent();
        assertThat(balanceStoreRepository.findByUserId("user2")).isPresent();
        assertThat(balanceStoreRepository.findByUserId("user3")).isPresent();
    }
    
    @Test
    @DisplayName("충전 후 저장하면 금액이 올바르게 업데이트된다")
    void save_afterCharge_updatesAmount() {
        // given
        Balance balance = Balance.create("user123");
        Balance saved = balanceStoreRepository.save(balance);
        
        // when
        saved.charge(new BigDecimal("10000"));
        saved.charge(new BigDecimal("20000"));
        Balance updated = balanceStoreRepository.save(saved);
        
        // then
        Balance reloaded = balanceStoreRepository.findByUserId("user123").get();
        assertThat(reloaded.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }
    
    @Test
    @DisplayName("사용 후 저장하면 금액이 올바르게 차감된다")
    void save_afterUse_deductsAmount() {
        // given
        Balance balance = Balance.create("user123");
        balance.charge(new BigDecimal("50000"));
        Balance saved = balanceStoreRepository.save(balance);
        
        // when
        saved.use(new BigDecimal("20000"));
        Balance updated = balanceStoreRepository.save(saved);
        
        // then
        Balance reloaded = balanceStoreRepository.findByUserId("user123").get();
        assertThat(reloaded.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }
}
