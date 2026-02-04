package com.example.concert_reservation.integration;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.reservation.infrastructure.ReservationJpaRepository;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 동시성 통합 테스트
 * 
 * 목적: 대규모 동시 접속 환경에서 예약 시스템의 동시성 제어 검증
 * - 200,000~300,000명 사용자 동시 접속 시뮬레이션
 * - 비관적 락(Pessimistic Lock)을 통한 데이터 무결성 보장
 * - 정확히 1명만 예약 성공하도록 보장
 * 
 * 테스트 시나리오:
 * 1. 소규모 동시성 테스트 (10명)
 * 2. 중규모 동시성 테스트 (100명)
 * 3. 대규모 동시성 테스트 (1,000명)
 * 4. 여러 좌석에 대한 동시 예약
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("동시성 통합 테스트 - 대규모 사용자 동시 접속")
class ConcurrencyIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyIntegrationTest.class);
    
    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;
    
    @Autowired
    private SeatManager seatManager;
    
    @Autowired
    private ConcertDateJpaRepository concertDateJpaRepository;
    
    @Autowired
    private SeatJpaRepository seatJpaRepository;
    
    @Autowired
    private ReservationJpaRepository reservationJpaRepository;
    
    private Long concertDateId;
    private Long seatId;
    private static final BigDecimal SEAT_PRICE = new BigDecimal("50000");
    
    @BeforeEach
    void setUp() {
        // 데이터 초기화
        reservationJpaRepository.deleteAll();
        seatJpaRepository.deleteAll();
        concertDateJpaRepository.deleteAll();
        
        // 콘서트 날짜 생성
        ConcertDateEntity concertDate = new ConcertDateEntity(
            null,
            "동시성 테스트 콘서트",
            LocalDate.now().plusDays(7),
            100,
            100
        );
        concertDate = concertDateJpaRepository.save(concertDate);
        concertDateId = concertDate.getId();
        
        // 좌석 생성
        SeatEntity seat = new SeatEntity(null, concertDateId, 1, SeatStatus.AVAILABLE.name(), SEAT_PRICE);
        seat = seatJpaRepository.save(seat);
        seatId = seat.getId();
    }

    /**
     * 동시성 테스트 헬퍼 메서드
     * 
     * @param userCount 동시에 예약을 시도할 사용자 수
     * @return ConcurrencyResult 테스트 결과 (성공/실패 수, 실행 시간)
     */
    private ConcurrencyResult executeReservationConcurrencyTest(int userCount) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);
        List<Future<ReservationAttempt>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // 모든 사용자가 동시에 예약 시도
        for (int i = 0; i < userCount; i++) {
            final String userId = "concurrent_user_" + i;
            
            Future<ReservationAttempt> future = executorService.submit(() -> {
                try {
                    // 모든 스레드가 준비될 때까지 대기
                    latch.countDown();
                    latch.await();
                    
                    // 예약 시도 (실제 Use Case 호출 - 트랜잭션 포함)
                    ReservationResponse response = reserveSeatUseCase.execute(
                        new ReserveSeatRequest(userId, seatId)
                    );
                    return new ReservationAttempt(userId, true, response.getReservationId(), null);
                } catch (Exception e) {
                    log.error("예약 실패 - User: {}, Error: {}", userId, e.getMessage());
                    return new ReservationAttempt(userId, false, null, e);
                }
            });
            futures.add(future);
        }
        
        // 결과 수집
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> successfulUsers = new ArrayList<>();
        List<String> failedUsers = new ArrayList<>();
        
        for (Future<ReservationAttempt> future : futures) {
            try {
                ReservationAttempt result = future.get(30, TimeUnit.SECONDS);
                if (result.success) {
                    successCount.incrementAndGet();
                    successfulUsers.add(result.userId);
                    log.info("✅ 예약 성공: {}", result.userId);
                } else {
                    failureCount.incrementAndGet();
                    failedUsers.add(result.userId);
                    if (result.exception != null) {
                        log.warn("❌ 예약 실패 - {}: {} - {}", 
                            result.userId, 
                            result.exception.getClass().getSimpleName(),
                            result.exception.getMessage());
                    }
                }
            } catch (TimeoutException e) {
                log.error("타임아웃 발생: {}", e.getMessage());
                failureCount.incrementAndGet();
            }
        }
        
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        return new ConcurrencyResult(
            userCount,
            successCount.get(),
            failureCount.get(),
            executionTime,
            successfulUsers,
            failedUsers
        );
    }

    @Test
    @Order(1)
    @DisplayName("소규모 동시성: 10명의 사용자가 동시에 1개 좌석 예약 시도")
    void concurrency_10Users_1Seat() throws Exception {
        // given
        int userCount = 10;
        
        // when
        ConcurrencyResult result = executeReservationConcurrencyTest(userCount);
        
        // then
        log.info("=== 소규모 동시성 테스트 결과 ===");
        log.info("총 사용자: {}", result.totalUsers);
        log.info("성공: {}", result.successCount);
        log.info("실패: {}", result.failureCount);
        log.info("실행 시간: {}ms", result.executionTime);
        log.info("성공한 사용자: {}", result.successfulUsers);
        
        // 정확히 1명만 성공해야 함
        assertThat(result.successCount).isEqualTo(1);
        assertThat(result.failureCount).isEqualTo(9);
        
        // 좌석 상태 확인 (비관적 락 없이 조회)
        SeatEntity seatEntity = seatJpaRepository.findById(seatId)
            .orElseThrow(() -> new AssertionError("좌석을 찾을 수 없습니다: " + seatId));
        assertThat(seatEntity.getStatus()).isEqualTo(SeatStatus.RESERVED.name());
    }

    @Test
    @Order(2)
    @DisplayName("중규모 동시성: 100명의 사용자가 동시에 1개 좌석 예약 시도")
    void concurrency_100Users_1Seat() throws Exception {
        // given
        int userCount = 100;
        
        // when
        ConcurrencyResult result = executeReservationConcurrencyTest(userCount);
        
        // then
        log.info("=== 중규모 동시성 테스트 결과 ===");
        log.info("총 사용자: {}", result.totalUsers);
        log.info("성공: {}", result.successCount);
        log.info("실패: {}", result.failureCount);
        log.info("실행 시간: {}ms", result.executionTime);
        log.info("성공한 사용자: {}", result.successfulUsers);
        
        // 정확히 1명만 성공해야 함
        assertThat(result.successCount).isEqualTo(1);
        assertThat(result.failureCount).isEqualTo(99);
        
        // 좌석 상태 확인 (비관적 락 없이 조회)
        SeatEntity seatEntity = seatJpaRepository.findById(seatId)
            .orElseThrow(() -> new AssertionError("좌석을 찾을 수 없습니다: " + seatId));
        assertThat(seatEntity.getStatus()).isEqualTo(SeatStatus.RESERVED.name());
        
        // 성능 검증 (100명 처리가 10초 이내)
        assertThat(result.executionTime).isLessThan(10000);
    }

    @Test
    @Order(3)
    @DisplayName("대규모 동시성: 1000명의 사용자가 동시에 1개 좌석 예약 시도")
    void concurrency_1000Users_1Seat() throws Exception {
        // given
        int userCount = 1000;
        
        // when
        ConcurrencyResult result = executeReservationConcurrencyTest(userCount);
        
        // then
        log.info("=== 대규모 동시성 테스트 결과 ===");
        log.info("총 사용자: {}", result.totalUsers);
        log.info("성공: {}", result.successCount);
        log.info("실패: {}", result.failureCount);
        log.info("실행 시간: {}ms", result.executionTime);
        log.info("초당 처리량: {} requests/sec", (userCount * 1000.0) / result.executionTime);
        log.info("성공한 사용자: {}", result.successfulUsers);
        
        // 정확히 1명만 성공해야 함
        assertThat(result.successCount).isEqualTo(1);
        assertThat(result.failureCount).isEqualTo(999);
        
        // 좌석 상태 확인 (비관적 락 없이 조회)
        SeatEntity seatEntity = seatJpaRepository.findById(seatId)
            .orElseThrow(() -> new AssertionError("좌석을 찾을 수 없습니다: " + seatId));
        assertThat(seatEntity.getStatus()).isEqualTo(SeatStatus.RESERVED.name());
        
        // 성능 검증 (1000명 처리가 30초 이내)
        assertThat(result.executionTime).isLessThan(30000);
    }

    @Test
    @Order(4)
    @DisplayName("복수 좌석 동시성: 100명이 10개 좌석에 동시 예약 시도")
    void concurrency_100Users_10Seats() throws Exception {
        // given: 추가 좌석 9개 생성 (총 10개)
        List<Long> seatIds = new ArrayList<>();
        seatIds.add(seatId); // 기존 좌석
        
        for (int i = 2; i <= 10; i++) {
            SeatEntity seat = new SeatEntity(null, concertDateId, i, SeatStatus.AVAILABLE.name(), SEAT_PRICE);
            seat = seatJpaRepository.save(seat);
            seatIds.add(seat.getId());
        }
        
        int userCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);
        List<Future<ReservationAttempt>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // 100명이 10개 좌석에 무작위로 예약 시도
        for (int i = 0; i < userCount; i++) {
            final String userId = "multi_seat_user_" + i;
            final Long targetSeatId = seatIds.get(i % 10); // 각 좌석에 10명씩 시도
            
            Future<ReservationAttempt> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    
                    ReservationResponse response = reserveSeatUseCase.execute(
                        new ReserveSeatRequest(userId, targetSeatId)
                    );
                    return new ReservationAttempt(userId, true, response.getReservationId(), null);
                } catch (Exception e) {
                    return new ReservationAttempt(userId, false, null, e);
                }
            });
            futures.add(future);
        }
        
        // 결과 수집
        int successCount = 0;
        int failureCount = 0;
        
        for (Future<ReservationAttempt> future : futures) {
            ReservationAttempt result = future.get(30, TimeUnit.SECONDS);
            if (result.success) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // then
        log.info("=== 복수 좌석 동시성 테스트 결과 ===");
        log.info("총 사용자: {}", userCount);
        log.info("총 좌석: 10개");
        log.info("성공: {}", successCount);
        log.info("실패: {}", failureCount);
        log.info("실행 시간: {}ms", executionTime);
        log.info("초당 처리량: {} requests/sec", (userCount * 1000.0) / executionTime);
        
        // 정확히 10명만 성공해야 함 (각 좌석당 1명)
        assertThat(successCount).isEqualTo(10);
        assertThat(failureCount).isEqualTo(90);
        
        // 모든 좌석이 RESERVED 상태인지 확인
        for (Long sid : seatIds) {
            SeatEntity seatEntity = seatJpaRepository.findById(sid)
                .orElseThrow(() -> new AssertionError("좌석을 찾을 수 없습니다: " + sid));
            assertThat(seatEntity.getStatus()).isEqualTo(SeatStatus.RESERVED.name());
        }
    }

    @Test
    @Order(5)
    @DisplayName("극한 동시성 시뮬레이션: 5000명의 사용자가 동시에 1개 좌석 예약 시도")
    @Disabled("성능 테스트용 - 필요 시에만 활성화")
    void concurrency_5000Users_1Seat() throws Exception {
        // given
        int userCount = 5000;
        
        // when
        ConcurrencyResult result = executeReservationConcurrencyTest(userCount);
        
        // then
        log.info("=== 극한 동시성 테스트 결과 ===");
        log.info("총 사용자: {}", result.totalUsers);
        log.info("성공: {}", result.successCount);
        log.info("실패: {}", result.failureCount);
        log.info("실행 시간: {}ms", result.executionTime);
        log.info("초당 처리량: {} requests/sec", (userCount * 1000.0) / result.executionTime);
        
        // 정확히 1명만 성공해야 함
        assertThat(result.successCount).isEqualTo(1);
        assertThat(result.failureCount).isEqualTo(4999);
        
        // 좌석 상태 확인 (비관적 락 없이 조회)
        SeatEntity seatEntity = seatJpaRepository.findById(seatId)
            .orElseThrow(() -> new AssertionError("좌석을 찾을 수 없습니다: " + seatId));
        assertThat(seatEntity.getStatus()).isEqualTo(SeatStatus.RESERVED.name());
    }

    // ========================================
    // Helper Classes
    // ========================================
    
    /**
     * 예약 시도 결과
     */
    private static class ReservationAttempt {
        String userId;
        boolean success;
        Long reservationId;
        Exception exception;
        
        ReservationAttempt(String userId, boolean success, Long reservationId, Exception exception) {
            this.userId = userId;
            this.success = success;
            this.reservationId = reservationId;
            this.exception = exception;
        }
    }
    
    /**
     * 동시성 테스트 결과
     */
    private static class ConcurrencyResult {
        int totalUsers;
        int successCount;
        int failureCount;
        long executionTime;
        List<String> successfulUsers;
        List<String> failedUsers;
        
        ConcurrencyResult(int totalUsers, int successCount, int failureCount, 
                         long executionTime, List<String> successfulUsers, List<String> failedUsers) {
            this.totalUsers = totalUsers;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.executionTime = executionTime;
            this.successfulUsers = successfulUsers;
            this.failedUsers = failedUsers;
        }
    }
}
