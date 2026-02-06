package com.example.concert_reservation.integration;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.payment.components.PaymentProcessor;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.refund.components.RefundProcessor;
import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.models.RefundStatus;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.infrastructure.ReservationJpaRepository;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 완전한 E2E 시나리오 테스트
 * 
 * 테스트 시나리오:
 * 1. 대기열 진입 (토큰 발급)
 * 2. 대기열 활성화 대기
 * 3. 콘서트 조회
 * 4. 좌석 조회
 * 5. 잔액 충전
 * 6. 좌석 예약
 * 7. 결제 처리
 * 8. 예약 확정
 * 9. 환불 처리
 * 10. 좌석 복구
 * 
 * 이 테스트는 고객이 대기열에 진입하는 시점부터
 * 최종 환불까지의 전체 비즈니스 플로우를 검증합니다.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.profiles.active=test",
    "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("완전한 E2E 시나리오: 대기열 진입 → 결제 → 환불")
class CompleteE2EScenarioTest {

    @Autowired
    private RedisQueueRepository queueRepository;
    
    @Autowired
    private BalanceManager balanceManager;
    
    @Autowired
    private SeatManager seatManager;
    
    @Autowired
    private ReservationManager reservationManager;
    
    @Autowired
    private PaymentProcessor paymentProcessor;
    
    @Autowired
    private RefundProcessor refundProcessor;
    
    @Autowired
    private ConcertDateJpaRepository concertDateRepository;
    
    @Autowired
    private SeatJpaRepository seatRepository;
    
    @Autowired
    private ReservationJpaRepository reservationRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    private Long concertDateId;
    private Long seatId;
    private static final BigDecimal SEAT_PRICE = new BigDecimal("50000");
    private static final String TEST_USER_ID = "e2e_test_user";

    @BeforeEach
    void setUp() {
        // 데이터 정리
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        
        // 콘서트 및 좌석 생성
        ConcertDateEntity concert = new ConcertDateEntity(
            null,
            "아이유 콘서트",
            LocalDate.now().plusDays(30),
            50,
            50
        );
        concert = concertDateRepository.save(concert);
        concertDateId = concert.getId();
        
        SeatEntity seat = new SeatEntity(
            null, 
            concertDateId, 
            1, 
            SeatStatus.AVAILABLE.name(), 
            SEAT_PRICE
        );
        seat = seatRepository.save(seat);
        seatId = seat.getId();
    }

    @Test
    @Transactional
    @DisplayName("✅ 완전한 E2E 시나리오: 대기열 → 예약 → 결제 → 환불")
    void completeE2EScenario_fromQueueToRefund_success() throws InterruptedException {
        
        // ========================================
        // STEP 1: 대기열 진입 (토큰 발급)
        // ========================================
        System.out.println("\n[STEP 1] 대기열 진입: 토큰 발급");
        UserQueue userQueue = queueRepository.addToWaitingQueue(TEST_USER_ID);
        
        assertThat(userQueue).isNotNull();
        assertThat(userQueue.getToken()).isNotNull();
        assertThat(userQueue.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(userQueue.getQueueNumber()).isGreaterThan(0);
        
        String token = userQueue.getToken().getValue();
        System.out.println("✅ 토큰 발급 성공: " + token.substring(0, 8) + "...");
        System.out.println("   대기 순번: " + userQueue.getQueueNumber());
        
        // ========================================
        // STEP 2: 대기열 활성화 (자동 활성화 시뮬레이션)
        // ========================================
        System.out.println("\n[STEP 2] 대기열 활성화 대기중...");
        
        // 실제 환경에서는 QueueActivationScheduler가 10초마다 자동 활성화
        // 테스트에서는 수동으로 활성화
        queueRepository.activateTokens(1);
        Thread.sleep(100); // Redis 동기화 대기
        
        Optional<UserQueue> activatedQueue = queueRepository.findByToken(userQueue.getToken());
        assertThat(activatedQueue).isPresent();
        assertThat(activatedQueue.get().getStatus()).isEqualTo(QueueStatus.ACTIVE);
        System.out.println("✅ 토큰 활성화 완료");
        System.out.println("   상태: WAITING → ACTIVE");
        
        // ========================================
        // STEP 3: 콘서트 및 좌석 정보 조회
        // ========================================
        System.out.println("\n[STEP 3] 콘서트 정보 조회");
        
        ConcertDateEntity concert = concertDateRepository.findById(concertDateId).get();
        assertThat(concert.getAvailableSeats()).isEqualTo(50);
        System.out.println("✅ 콘서트: " + concert.getConcertName());
        System.out.println("   공연일: " + concert.getConcertDate());
        System.out.println("   잔여 좌석: " + concert.getAvailableSeats() + "/50");
        
        Seat seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        System.out.println("   좌석 " + seat.getSeatNumber() + "번: " + seat.getStatus() + " (" + seat.getPrice() + "원)");
        
        // ========================================
        // STEP 4: 잔액 충전
        // ========================================
        System.out.println("\n[STEP 4] 잔액 충전");
        BigDecimal chargeAmount = new BigDecimal("100000");
        balanceManager.chargeBalance(TEST_USER_ID, chargeAmount);
        
        Balance balance = balanceManager.getBalance(TEST_USER_ID);
        assertThat(balance.getAmount()).isEqualByComparingTo(chargeAmount);
        System.out.println("✅ 충전 완료: " + chargeAmount + "원");
        System.out.println("   현재 잔액: " + balance.getAmount() + "원");
        
        // ========================================
        // STEP 5: 좌석 예약
        // ========================================
        System.out.println("\n[STEP 5] 좌석 예약");
        
        Reservation reservation = reservationManager.reserveSeat(TEST_USER_ID, seatId);
        assertThat(reservation).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getPrice()).isEqualByComparingTo(SEAT_PRICE);
        
        // 좌석 상태 확인
        Seat reservedSeat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(reservedSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        System.out.println("✅ 예약 생성 성공");
        System.out.println("   예약 ID: " + reservation.getId());
        System.out.println("   상태: " + reservation.getStatus());
        System.out.println("   좌석 상태: AVAILABLE → RESERVED");
        System.out.println("   예약 만료: " + reservation.getExpiresAt());
        
        // ========================================
        // STEP 6: 결제 처리
        // ========================================
        System.out.println("\n[STEP 6] 결제 처리");
        
        Payment payment = paymentProcessor.processPayment(reservation.getId(), TEST_USER_ID);
        assertThat(payment).isNotNull();
        assertThat(payment.getAmount()).isEqualByComparingTo(SEAT_PRICE);
        
        // 잔액 차감 확인
        Balance afterPayment = balanceManager.getBalance(TEST_USER_ID);
        BigDecimal expectedBalance = chargeAmount.subtract(SEAT_PRICE);
        assertThat(afterPayment.getAmount()).isEqualByComparingTo(expectedBalance);
        
        // 예약 상태 확인
        Reservation confirmedReservation = reservationManager.getReservation(reservation.getId()).get();
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        
        System.out.println("✅ 결제 완료");
        System.out.println("   결제 ID: " + payment.getId());
        System.out.println("   결제 금액: " + payment.getAmount() + "원");
        System.out.println("   잔여 잔액: " + afterPayment.getAmount() + "원");
        System.out.println("   예약 상태: PENDING → CONFIRMED");
        
        // ========================================
        // STEP 7: 환불 처리
        // ========================================
        System.out.println("\n[STEP 7] 환불 처리");
        
        Refund refund = refundProcessor.processRefund(payment.getId(), TEST_USER_ID, "고객 변심");
        assertThat(refund).isNotNull();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(refund.getAmount()).isEqualByComparingTo(SEAT_PRICE);
        
        // 잔액 복구 확인
        Balance afterRefund = balanceManager.getBalance(TEST_USER_ID);
        assertThat(afterRefund.getAmount()).isEqualByComparingTo(chargeAmount);
        
        // 예약 취소 확인
        Reservation cancelledReservation = reservationManager.getReservation(reservation.getId()).get();
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        
        // 좌석 복구 확인
        Seat releasedSeat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        
        System.out.println("✅ 환불 완료");
        System.out.println("   환불 ID: " + refund.getId());
        System.out.println("   환불 금액: " + refund.getAmount() + "원");
        System.out.println("   복구 잔액: " + afterRefund.getAmount() + "원");
        System.out.println("   예약 상태: CONFIRMED → CANCELLED");
        System.out.println("   좌석 상태: RESERVED → AVAILABLE");
        
        // ========================================
        // 최종 검증
        // ========================================
        System.out.println("\n[최종 검증]");
        System.out.println("✅ 전체 시나리오 성공!");
        System.out.println("   - 대기열 진입 → 활성화: OK");
        System.out.println("   - 잔액 충전 → 결제 → 환불: OK");
        System.out.println("   - 좌석 예약 → 확정 → 복구: OK");
        System.out.println("   - 데이터 정합성: OK");
    }

    @Test
    @Transactional
    @DisplayName("❌ 실패 시나리오: 잔액 부족으로 결제 실패")
    void failureScenario_insufficientBalance_paymentFails() {
        System.out.println("\n[실패 시나리오] 잔액 부족");
        
        // 대기열 진입 및 활성화
        UserQueue userQueue = queueRepository.addToWaitingQueue(TEST_USER_ID);
        queueRepository.activateTokens(1);
        
        // 부족한 잔액 충전 (좌석 가격: 50,000원)
        BigDecimal insufficientAmount = new BigDecimal("30000");
        balanceManager.chargeBalance(TEST_USER_ID, insufficientAmount);
        System.out.println("잔액: " + insufficientAmount + "원 (부족)");
        
        // 좌석 예약
        Reservation reservation = reservationManager.reserveSeat(TEST_USER_ID, seatId);
        System.out.println("예약 생성: " + reservation.getId());
        
        // 결제 시도 (실패 예상)
        try {
            paymentProcessor.processPayment(reservation.getId(), TEST_USER_ID);
            Assertions.fail("잔액 부족으로 결제가 실패해야 합니다");
        } catch (Exception e) {
            System.out.println("✅ 예상대로 결제 실패: " + e.getMessage());
            assertThat(e.getMessage()).contains("잔액이 부족합니다");
        }
        
        // 예약은 여전히 PENDING 상태
        Reservation stillPending = reservationManager.getReservation(reservation.getId()).get();
        assertThat(stillPending.getStatus()).isEqualTo(ReservationStatus.PENDING);
        System.out.println("예약 상태: " + stillPending.getStatus() + " (유지)");
    }

    @Test
    @Transactional
    @DisplayName("❌ 실패 시나리오: 중복 예약 시도")
    void failureScenario_duplicateReservation_fails() {
        System.out.println("\n[실패 시나리오] 중복 예약");
        
        // 첫 번째 사용자 예약
        String user1 = "user1";
        balanceManager.chargeBalance(user1, new BigDecimal("100000"));
        
        UserQueue queue1 = queueRepository.addToWaitingQueue(user1);
        queueRepository.activateTokens(1);
        
        Reservation reservation1 = reservationManager.reserveSeat(user1, seatId);
        System.out.println("✅ 첫 번째 사용자 예약 성공: " + reservation1.getId());
        
        // 첫 번째 예약을 DB에 플러시하여 두 번째 예약 시도 시 좌석 상태를 확인할 수 있게 함
        entityManager.flush();
        entityManager.clear();
        
        // 두 번째 사용자가 같은 좌석 예약 시도
        String user2 = "user2";
        balanceManager.chargeBalance(user2, new BigDecimal("100000"));
        
        UserQueue queue2 = queueRepository.addToWaitingQueue(user2);
        queueRepository.activateTokens(1);
        
        try {
            reservationManager.reserveSeat(user2, seatId);
            Assertions.fail("이미 예약된 좌석이므로 실패해야 합니다");
        } catch (Exception e) {
            System.out.println("✅ 예상대로 중복 예약 실패: " + e.getMessage());
            assertThat(e.getMessage()).contains("이미 예약된 좌석");
        }
    }
}
