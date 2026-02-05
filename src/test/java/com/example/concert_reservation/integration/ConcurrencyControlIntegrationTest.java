package com.example.concert_reservation.integration;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 제어 테스트
 * 비관적 락을 이용한 Race Condition 방지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("동시성 제어 통합 테스트")
public class ConcurrencyControlIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(ConcurrencyControlIntegrationTest.class);
    
    @Autowired
    private BalanceManager balanceManager;
    
    private static final int THREAD_COUNT = 10;
    
    @Test
    @DisplayName("한 사용자가 동시에 여러 요청으로 잔액 사용 시도 - 비관적 락으로 순차 처리")
    void concurrentBalanceUsage_shouldPreventRaceCondition() throws InterruptedException {
        // Given: 사용자 잔액 10,000원
        String userId = "user-concurrent-balance-" + System.currentTimeMillis();
        balanceManager.chargeBalance(userId, new BigDecimal("10000"));
        
        // When: 한 사용자가 동시에 10번 각각 1,500원씩 사용 요청 (총 15,000원 시도)
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    balanceManager.useBalance(userId, new BigDecimal("1500"));
                    successCount.incrementAndGet();
                    log.info("잔액 사용 성공");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.info("잔액 부족으로 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        // Then: 6번만 성공하고 4번은 실패해야 함 (비관적 락으로 순차 처리)
        log.info("성공: {}번, 실패: {}번", successCount.get(), failCount.get());
        assertThat(successCount.get()).isEqualTo(6); // 10,000 / 1,500 = 6번 성공
        assertThat(failCount.get()).isEqualTo(4);
        
        // 최종 잔액: 10,000 - (1,500 * 6) = 1,000원
        Balance finalBalance = balanceManager.getBalance(userId);
        assertThat(finalBalance.getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
    }
    
    @Test
    @DisplayName("한 사용자가 동시에 충전과 사용 요청 - 비관적 락으로 순차 처리")
    void concurrentChargeAndUse_shouldMaintainConsistency() throws InterruptedException {
        // Given: 사용자 잔액 5,000원
        String userId = "user-charge-use-" + System.currentTimeMillis();
        balanceManager.chargeBalance(userId, new BigDecimal("5000"));
        
        // When: 한 사용자가 5번 충전(+3,000원), 5번 사용(-2,000원) 동시 요청
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger chargeCount = new AtomicInteger(0);
        AtomicInteger useCount = new AtomicInteger(0);
        
        // 충전 스레드 5개
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> {
                try {
                    balanceManager.chargeBalance(userId, new BigDecimal("3000"));
                    chargeCount.incrementAndGet();
                    log.info("충전 완료");
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 사용 스레드 5개
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> {
                try {
                    balanceManager.useBalance(userId, new BigDecimal("2000"));
                    useCount.incrementAndGet();
                    log.info("사용 완료");
                } catch (Exception e) {
                    log.info("사용 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        // Then: 최종 잔액 계산
        // 초기: 5,000
        // 충전: +3,000 * 5 = +15,000
        // 사용: -2,000 * useCount
        Balance finalBalance = balanceManager.getBalance(userId);
        
        log.info("충전 횟수: {}, 사용 횟수: {}, 최종 잔액: {}", 
            chargeCount.get(), useCount.get(), finalBalance.getAmount());
        
        // 비관적 락이 정상 작동하면 데이터 무결성 유지
        BigDecimal expectedBalance = new BigDecimal("5000")
            .add(new BigDecimal("3000").multiply(new BigDecimal(chargeCount.get())))
            .subtract(new BigDecimal("2000").multiply(new BigDecimal(useCount.get())));
        
        assertThat(finalBalance.getAmount()).isEqualByComparingTo(expectedBalance);
        assertThat(chargeCount.get()).isEqualTo(5); // 모든 충전은 성공
        assertThat(finalBalance.getAmount().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0); // 잔액은 음수가 아님
    }
}
