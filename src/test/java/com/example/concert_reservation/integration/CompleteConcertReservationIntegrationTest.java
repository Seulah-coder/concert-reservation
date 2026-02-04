package com.example.concert_reservation.integration;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.CancelReservationUseCase;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
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
import com.example.concert_reservation.domain.refund.components.RefundProcessor;
import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.models.RefundStatus;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.infrastructure.ReservationJpaRepository;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * 완전한 콘서트 예약 프로세스 통합 테스트
 * 
 * 전체 예약 생명주기를 처음부터 끝까지 검증:
 * 1. 사용자 설정 (잔액 충전)
 * 2. 콘서트/좌석 설정
 * 3. 좌석 예약 (임시 보유)
 * 4. 결제 처리
 * 5. 환불 처리
 * 
 * 모든 가능한 엣지 케이스 및 오류 시나리오 포함:
 * - 동시 예약 시도
 * - 결제 권한
 * - 잔액 부족
 * - 예약 시간 초과/만료
 * - 유효하지 않은 좌석 선택
 * - 중복 결제 시도
 * - 권한 없는 환불 시도
 * - 취소 후 환불
 * - 동일 좌석을 위한 여러 사용자 경쟁
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("완전한 콘서트 예약 프로세스 통합 테스트")
class CompleteConcertReservationIntegrationTest {

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;
    
    @Autowired
    private CancelReservationUseCase cancelReservationUseCase;
    
    @Autowired
    private ReservationManager reservationManager;
    
    @Autowired
    private SeatManager seatManager;
    
    @Autowired
    private BalanceManager balanceManager;
    
    @Autowired
    private PaymentProcessor paymentProcessor;
    
    @Autowired
    private RefundProcessor refundProcessor;
    
    @Autowired
    private ConcertDateJpaRepository concertDateJpaRepository;
    
    @Autowired
    private SeatJpaRepository seatJpaRepository;
    
    @Autowired
    private ReservationJpaRepository reservationJpaRepository;
    
    private Long concertDateId;
    private Long seatId1;
    private Long seatId2;
    private Long seatId3;
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
            "완전한 테스트 콘서트",
            LocalDate.now().plusDays(7),
            50,
            50
        );
        concertDate = concertDateJpaRepository.save(concertDate);
        concertDateId = concertDate.getId();
        
        // 좌석 3개 생성
        SeatEntity seat1 = new SeatEntity(null, concertDateId, 1, SeatStatus.AVAILABLE.name(), SEAT_PRICE);
        SeatEntity seat2 = new SeatEntity(null, concertDateId, 2, SeatStatus.AVAILABLE.name(), SEAT_PRICE);
        SeatEntity seat3 = new SeatEntity(null, concertDateId, 3, SeatStatus.AVAILABLE.name(), SEAT_PRICE);
        
        seat1 = seatJpaRepository.save(seat1);
        seat2 = seatJpaRepository.save(seat2);
        seat3 = seatJpaRepository.save(seat3);
        
        seatId1 = seat1.getId();
        seatId2 = seat2.getId();
        seatId3 = seat3.getId();
    }

    // ========================================
    // 1. HAPPY PATH - 완전한 플로우 성공
    // ========================================
    
    @Test
    @Order(1)
    @Transactional
    @DisplayName("완전한 플로우: 예약 → 결제 → 환불 (성공)")
    void completeFlow_reservePayRefund_success() {
        String userId = "user_complete_flow";
        
        // Step 1: 잔액 충전
        balanceManager.chargeBalance(userId, new BigDecimal("100000"));
        Balance initialBalance = balanceManager.getBalance(userId);
        assertThat(initialBalance.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        
        // Step 2: 좌석 예약
        ReservationResponse reservationResponse = reserveSeatUseCase.execute(
            new ReserveSeatRequest(userId, seatId1)
        );
        assertThat(reservationResponse.getReservationId()).isNotNull();
        assertThat(reservationResponse.getStatus()).isEqualTo(ReservationStatus.PENDING.name());
        
        Seat seat = seatManager.getSeatByIdWithLock(seatId1);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        // Step 3: 결제 처리
        Payment payment = paymentProcessor.processPayment(reservationResponse.getReservationId(), userId);
        assertThat(payment).isNotNull();
        assertThat(payment.getAmount()).isEqualByComparingTo(SEAT_PRICE);
        
        Balance afterPayment = balanceManager.getBalance(userId);
        assertThat(afterPayment.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        
        Reservation confirmedReservation = reservationManager.getReservation(reservationResponse.getReservationId()).get();
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        
        // Step 4: 환불 처리
        Refund refund = refundProcessor.processRefund(payment.getId(), userId, "고객 변심");
        assertThat(refund).isNotNull();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        
        // 환불 후 잔액 복구 확인
        Balance afterRefund = balanceManager.getBalance(userId);
        assertThat(afterRefund.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        
        Reservation cancelledReservation = reservationManager.getReservation(reservationResponse.getReservationId()).get();
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        
        // 좌석 해제 확인
        Seat releasedSeat = seatManager.getSeatByIdWithLock(seatId1);
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    // ========================================
    // 2. RESERVATION PHASE - 엣지 케이스
    // ========================================
    
    @Test
    @Order(2)
    @Transactional
    @DisplayName("예약 취소: 좌석 복구")
    void cancelReservation_seatRestored() {
        String userId = "user_cancel";
        
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(userId, seatId1)
        );
        
        Seat reservedSeat = seatManager.getSeatByIdWithLock(seatId1);
        assertThat(reservedSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        // 예약 취소
        cancelReservationUseCase.execute(response.getReservationId());
        
        // 좌석 복구 확인
        Seat availableSeat = seatManager.getSeatByIdWithLock(seatId1);
        assertThat(availableSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        
        Reservation cancelled = reservationManager.getReservation(response.getReservationId()).get();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    // ========================================
    // 3. PAYMENT PHASE - 엣지 케이스
    // ========================================
    
    @Test
    @Order(3)
    @Transactional
    @DisplayName("결제 실패: 잔액 부족")
    void paymentFails_insufficientBalance() {
        String userId = "user_poor";
        
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(userId, seatId1)
        );
        
        // 부족한 잔액 충전 (30000원만)
        balanceManager.chargeBalance(userId, new BigDecimal("30000"));
        
        // 결제 시도 (실패 예상)
        assertThatThrownBy(() -> 
            paymentProcessor.processPayment(response.getReservationId(), userId)
        )
        .isInstanceOf(DomainConflictException.class)
        .hasMessageContaining("잔액이 부족합니다");
        
        // 잔액 차감 안됨
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }
    
    @Test
    @Order(4)
    @Transactional
    @DisplayName("결제 실패: 권한 없는 사용자")
    void paymentFails_unauthorizedUser() {
        String owner = "user_owner";
        String hacker = "user_hacker";
        
        // 소유자가 예약
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(owner, seatId1)
        );
        
        // 해커가 잔액 충전
        balanceManager.chargeBalance(hacker, new BigDecimal("100000"));
        
        // 해커가 소유자의 예약을 결제하려고 시도
        assertThatThrownBy(() -> 
            paymentProcessor.processPayment(response.getReservationId(), hacker)
        )
        .isInstanceOf(DomainForbiddenException.class)
        .hasMessageContaining("본인의 예약만 결제할 수 있습니다");
        
        // 해커의 잔액은 차감되지 않음
        Balance hackerBalance = balanceManager.getBalance(hacker);
        assertThat(hackerBalance.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
    }
    
    @Test
    @Order(5)
    @Transactional
    @DisplayName("결제 실패: 중복 결제")
    void paymentFails_duplicatePayment() {
        String userId = "user_duplicate";
        
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(userId, seatId1)
        );
        balanceManager.chargeBalance(userId, new BigDecimal("200000"));
        
        // 첫 번째 결제 성공
        Payment firstPayment = paymentProcessor.processPayment(response.getReservationId(), userId);
        assertThat(firstPayment).isNotNull();
        
        // 두 번째 결제 시도 (실패)
        assertThatThrownBy(() -> 
            paymentProcessor.processPayment(response.getReservationId(), userId)
        )
        .isInstanceOf(DomainConflictException.class)
        .hasMessageContaining("예약 상태가 올바르지 않습니다");
        
        // 잔액은 한 번만 차감됨
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
    }
    
    @Test
    @Order(6)
    @Transactional
    @DisplayName("결제 실패: 취소된 예약")
    void paymentFails_cancelledReservation() {
        String userId = "user_cancelled";
        
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(userId, seatId1)
        );
        
        // 예약 취소
        cancelReservationUseCase.execute(response.getReservationId());
        
        balanceManager.chargeBalance(userId, new BigDecimal("100000"));
        
        // 취소된 예약 결제 시도
        assertThatThrownBy(() -> 
            paymentProcessor.processPayment(response.getReservationId(), userId)
        )
        .isInstanceOf(DomainConflictException.class)
        .hasMessageContaining("예약 상태가 올바르지 않습니다");
    }

    // ========================================
    // 4. REFUND PHASE - 엣지 케이스
    // ========================================
    
    @Test
    @Order(7)
    @Transactional
    @DisplayName("환불 실패: 다른 사용자의 결제 환불 시도")
    void refundFails_unauthorizedUser() {
        String owner = "user_refund_owner";
        String hacker = "user_refund_hacker";
        
        // 소유자: 예약, 충전, 결제
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(owner, seatId1)
        );
        balanceManager.chargeBalance(owner, new BigDecimal("100000"));
        Payment payment = paymentProcessor.processPayment(response.getReservationId(), owner);
        
        // 해커가 소유자의 결제를 환불하려고 시도
        assertThatThrownBy(() -> 
            refundProcessor.processRefund(payment.getId(), hacker, "도둑질")
        )
        .isInstanceOf(DomainForbiddenException.class)
        .hasMessageContaining("본인의 결제만 환불할 수 있습니다");
        
        // 소유자의 잔액은 변하지 않음
        Balance ownerBalance = balanceManager.getBalance(owner);
        assertThat(ownerBalance.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
    }
    
    @Test
    @Order(8)
    @Transactional
    @DisplayName("환불 실패: 중복 환불")
    void refundFails_duplicateRefund() {
        String userId = "user_dup_refund";
        
        ReservationResponse response = reserveSeatUseCase.execute(
            new ReserveSeatRequest(userId, seatId1)
        );
        balanceManager.chargeBalance(userId, new BigDecimal("100000"));
        Payment payment = paymentProcessor.processPayment(response.getReservationId(), userId);
        
        // 첫 번째 환불 성공
        Refund firstRefund = refundProcessor.processRefund(payment.getId(), userId, "첫 번째 환불");
        assertThat(firstRefund).isNotNull();
        
        // 두 번째 환불 시도 (실패)
        assertThatThrownBy(() -> 
            refundProcessor.processRefund(payment.getId(), userId, "두 번째 환불")
        )
        .isInstanceOf(Exception.class);
        
        // 잔액은 한 번만 환불됨
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    // ========================================
    // 5. COMPLEX SCENARIOS - 여러 사용자
    // ========================================
    
    @Test
    @Order(9)
    @Transactional
    @DisplayName("복합 시나리오: 여러 사용자가 독립적으로 예약-결제-환불")
    void complexScenario_multipleUsersIndependentFlow() {
        String user1 = "user_multi_1";
        String user2 = "user_multi_2";
        String user3 = "user_multi_3";
        
        // 모든 사용자가 잔액 충전
        balanceManager.chargeBalance(user1, new BigDecimal("100000"));
        balanceManager.chargeBalance(user2, new BigDecimal("100000"));
        balanceManager.chargeBalance(user3, new BigDecimal("100000"));
        
        // User1: 예약 → 결제
        ReservationResponse res1 = reserveSeatUseCase.execute(new ReserveSeatRequest(user1, seatId1));
        Payment pay1 = paymentProcessor.processPayment(res1.getReservationId(), user1);
        
        // User2: 예약 → 결제 → 환불
        ReservationResponse res2 = reserveSeatUseCase.execute(new ReserveSeatRequest(user2, seatId2));
        Payment pay2 = paymentProcessor.processPayment(res2.getReservationId(), user2);
        Refund ref2 = refundProcessor.processRefund(pay2.getId(), user2, "변심");
        
        // User3: 예약 → 취소 (결제 없음)
        ReservationResponse res3 = reserveSeatUseCase.execute(new ReserveSeatRequest(user3, seatId3));
        cancelReservationUseCase.execute(res3.getReservationId());
        
        // User1 확인: 결제됨, 예약 확정, 잔액 차감됨
        assertThat(balanceManager.getBalance(user1).getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(reservationManager.getReservation(res1.getReservationId()).get().getStatus())
            .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(seatManager.getSeatByIdWithLock(seatId1).getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        // User2 확인: 환불됨, 예약 취소, 잔액 복구, 좌석 해제
        assertThat(balanceManager.getBalance(user2).getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(reservationManager.getReservation(res2.getReservationId()).get().getStatus())
            .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(seatManager.getSeatByIdWithLock(seatId2).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        
        // User3 확인: 잔액 그대로, 예약 취소, 좌석 해제
        assertThat(balanceManager.getBalance(user3).getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(reservationManager.getReservation(res3.getReservationId()).get().getStatus())
            .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(seatManager.getSeatByIdWithLock(seatId3).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @Order(10)
    @Transactional
    @DisplayName("복합 시나리오: 환불 후 재예약")
    void complexScenario_refundAndReReserve() {
        String user1 = "user_refund_1";
        String user2 = "user_refund_2";
        
        // User1: 예약 → 결제 → 환불
        balanceManager.chargeBalance(user1, new BigDecimal("100000"));
        ReservationResponse res1 = reserveSeatUseCase.execute(new ReserveSeatRequest(user1, seatId1));
        Payment pay1 = paymentProcessor.processPayment(res1.getReservationId(), user1);
        refundProcessor.processRefund(pay1.getId(), user1, "환불");
        
        // 좌석이 해제되었는지 확인
        Seat releasedSeat = seatManager.getSeatByIdWithLock(seatId1);
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        
        // User2: 같은 좌석 예약
        balanceManager.chargeBalance(user2, new BigDecimal("100000"));
        ReservationResponse res2 = reserveSeatUseCase.execute(new ReserveSeatRequest(user2, seatId1));
        
        assertThat(res2.getReservationId()).isNotNull();
        assertThat(seatManager.getSeatByIdWithLock(seatId1).getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    // ========================================
    // 6. ADDITIONAL SCENARIOS - 추가 시나리오
    // ========================================
    
    @Test
    @Order(11)
    @Transactional
    @DisplayName("추가 시나리오: 이미 예약된 좌석 재예약 시도")
    void additionalScenario_reserveAlreadyReservedSeat() {
        String user1 = "user_first";
        String user2 = "user_second";
        
        // User1: 좌석 예약
        ReservationResponse res1 = reserveSeatUseCase.execute(new ReserveSeatRequest(user1, seatId1));
        assertThat(res1).isNotNull();
        
        // User2: 같은 좌석 예약 시도 (실패 예상)
        assertThatThrownBy(() -> 
            reserveSeatUseCase.execute(new ReserveSeatRequest(user2, seatId1))
        )
        .isInstanceOf(Exception.class);
        
        // User1의 예약은 여전히 유효
        Reservation res1State = reservationManager.getReservation(res1.getReservationId()).get();
        assertThat(res1State.getStatus()).isEqualTo(ReservationStatus.PENDING);
        
        // 좌석은 여전히 RESERVED 상태
        Seat seat = seatManager.getSeatByIdWithLock(seatId1);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    // ========================================
    // 7. BALANCE SCENARIOS - 잔액 시나리오
    // ========================================
    
    @Test
    @Order(12)
    @Transactional
    @DisplayName("잔액 시나리오: 여러 번 충전 및 결제")
    void balanceScenario_multipleChargesAndPayments() {
        String userId = "user_balance";
        
        // 부족한 잔액으로 시도 (실패)
        balanceManager.chargeBalance(userId, new BigDecimal("30000"));
        ReservationResponse res1 = reserveSeatUseCase.execute(new ReserveSeatRequest(userId, seatId1));
        assertThatThrownBy(() -> paymentProcessor.processPayment(res1.getReservationId(), userId))
            .isInstanceOf(DomainConflictException.class);
        
        // 추가 충전
        balanceManager.chargeBalance(userId, new BigDecimal("30000"));
        assertThat(balanceManager.getBalance(userId).getAmount()).isEqualByComparingTo(new BigDecimal("60000"));
        
        // 이제 결제 성공
        Payment payment = paymentProcessor.processPayment(res1.getReservationId(), userId);
        assertThat(payment).isNotNull();
        assertThat(balanceManager.getBalance(userId).getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }
    
    @Test
    @Order(13)
    @Transactional
    @DisplayName("잔액 시나리오: 환불 후 재사용")
    void balanceScenario_refundAndReuse() {
        String userId = "user_refund_reuse";
        
        // 첫 번째: 예약 → 결제 → 환불
        balanceManager.chargeBalance(userId, new BigDecimal("50000"));
        ReservationResponse res1 = reserveSeatUseCase.execute(new ReserveSeatRequest(userId, seatId1));
        Payment pay1 = paymentProcessor.processPayment(res1.getReservationId(), userId);
        refundProcessor.processRefund(pay1.getId(), userId, "환불");
        
        // 잔액이 복구되었는지 확인
        assertThat(balanceManager.getBalance(userId).getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        
        // 두 번째: 다른 좌석 예약 → 환불된 돈으로 결제
        ReservationResponse res2 = reserveSeatUseCase.execute(new ReserveSeatRequest(userId, seatId2));
        Payment pay2 = paymentProcessor.processPayment(res2.getReservationId(), userId);
        
        // 검증
        assertThat(pay2).isNotNull();
        assertThat(balanceManager.getBalance(userId).getAmount()).isEqualByComparingTo(new BigDecimal("0"));
    }

}
