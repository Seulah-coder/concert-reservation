package com.example.concert_reservation.integration;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.concert.repositories.SeatStoreRepository;
import com.example.concert_reservation.domain.payment.components.PaymentProcessor;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.sql.init.mode=never"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("결제 통합 테스트 - 예약자만 결제 가능한 전체 플로우")
class PaymentIntegrationTest {
    
    @Autowired
    private SeatManager seatManager;
    
    @Autowired
    private SeatStoreRepository seatStoreRepository;
    
    @Autowired
    private ReservationManager reservationManager;
    
    @Autowired
    private BalanceManager balanceManager;
    
    @Autowired
    private PaymentProcessor paymentProcessor;
    
    private Seat createAndSaveSeat(Integer seatNumber) {
        Seat seat = Seat.create(1L, seatNumber, new BigDecimal("50000"));
        return seatStoreRepository.save(seat);
    }
    
    @Test
    @Transactional
    @DisplayName("전체 플로우: 좌석 예약 → 잔액 충전 → 결제 → 좌석 확정")
    void fullPaymentFlow_success() {
        // given: 사용자와 좌석 준비
        String userId = "user123";
        Seat seat = createAndSaveSeat(101);
        BigDecimal seatPrice = new BigDecimal("50000");
        
        // 1. 좌석 예약
        Reservation reservation = reservationManager.reserveSeat(userId, seat.getId());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        
        // 2. 잔액 충전
        balanceManager.chargeBalance(userId, new BigDecimal("100000"));
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        
        // when: 결제 처리
        Payment payment = paymentProcessor.processPayment(reservation.getId(), userId);
        
        // then: 결제 완료 확인
        assertThat(payment).isNotNull();
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getAmount()).isEqualByComparingTo(seatPrice);
        
        // 잔액 차감 확인
        Balance updatedBalance = balanceManager.getBalance(userId);
        assertThat(updatedBalance.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        
        // 예약 확정 확인
        Reservation confirmedReservation = reservationManager.getReservation(reservation.getId()).get();
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        
        // 좌석 상태 확인
        Seat updatedSeat = seatManager.getSeatByIdWithLock(seat.getId());
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
    
    @Test
    @Transactional
    @DisplayName("다른 사용자는 타인의 예약을 결제할 수 없다")
    void paymentByDifferentUser_fails() {
        // given: 사용자A가 좌석 예약
        String userA = "userA";
        String userB = "userB";
        Seat seat = createAndSaveSeat(102);
        
        Reservation reservation = reservationManager.reserveSeat(userA, seat.getId());
        
        // 사용자B의 잔액 충전
        balanceManager.chargeBalance(userB, new BigDecimal("100000"));
        
        // when & then: 사용자B가 사용자A의 예약을 결제하려고 시도
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservation.getId(), userB))
            .isInstanceOf(DomainForbiddenException.class)
            .hasMessageContaining("본인의 예약만 결제할 수 있습니다");
        
        // 사용자B의 잔액은 차감되지 않음
        Balance balanceB = balanceManager.getBalance(userB);
        assertThat(balanceB.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        
        // 예약은 여전히 PENDING 상태
        Reservation stillReserved = reservationManager.getReservation(reservation.getId()).get();
        assertThat(stillReserved.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }
    
    @Test
    @Transactional
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void paymentWithInsufficientBalance_fails() {
        // given: 좌석 예약 (50000원)
        String userId = "user123";
        Seat seat = createAndSaveSeat(103);
        
        Reservation reservation = reservationManager.reserveSeat(userId, seat.getId());
        
        // 부족한 잔액 충전 (30000원만)
        balanceManager.chargeBalance(userId, new BigDecimal("30000"));
        
        // when & then: 결제 시도 실패
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservation.getId(), userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("잔액이 부족합니다");
        
        // 예약은 여전히 PENDING 상태
        Reservation stillReserved = reservationManager.getReservation(reservation.getId()).get();
        assertThat(stillReserved.getStatus()).isEqualTo(ReservationStatus.PENDING);
        
        // 잔액은 차감되지 않음
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }
    
    @Test
    @Transactional
    @DisplayName("이미 결제된 예약은 중복 결제할 수 없다")
    void duplicatePayment_fails() {
        // given: 좌석 예약 및 충분한 잔액
        String userId = "user123";
        Seat seat = createAndSaveSeat(104);
        
        Reservation reservation = reservationManager.reserveSeat(userId, seat.getId());
        balanceManager.chargeBalance(userId, new BigDecimal("200000"));
        
        // 첫 번째 결제 성공
        Payment firstPayment = paymentProcessor.processPayment(reservation.getId(), userId);
        assertThat(firstPayment).isNotNull();
        
        // when & then: 동일한 예약에 대해 두 번째 결제 시도
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservation.getId(), userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("예약 상태가 올바르지 않습니다"); // CONFIRMED 상태라서 결제 불가
        
        // 잔액은 한 번만 차감됨 (200000 - 50000 = 150000)
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
    }
    
    @Test
    @Transactional
    @DisplayName("취소된 예약은 결제할 수 없다")
    void paymentForCancelledReservation_fails() {
        // given: 좌석 예약 후 취소
        String userId = "user123";
        Seat seat = createAndSaveSeat(105);
        
        Reservation reservation = reservationManager.reserveSeat(userId, seat.getId());
        reservationManager.cancelReservation(reservation.getId(), userId);
        
        // 잔액 충전
        balanceManager.chargeBalance(userId, new BigDecimal("100000"));
        
        // when & then: 취소된 예약 결제 시도
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservation.getId(), userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("예약 상태가 올바르지 않습니다");
        
        // 잔액은 차감되지 않음
        Balance balance = balanceManager.getBalance(userId);
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
    }
    
    @Test
    @Transactional
    @DisplayName("여러 사용자가 각자의 예약을 독립적으로 결제할 수 있다")
    void multipleUsersPaymentFlow_success() {
        // given: 3명의 사용자가 각각 좌석 예약
        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";
        
        Seat seat1 = createAndSaveSeat(106);
        Seat seat2 = createAndSaveSeat(107);
        Seat seat3 = createAndSaveSeat(108);
        
        Reservation reservation1 = reservationManager.reserveSeat(user1, seat1.getId());
        Reservation reservation2 = reservationManager.reserveSeat(user2, seat2.getId());
        Reservation reservation3 = reservationManager.reserveSeat(user3, seat3.getId());
        
        // 각 사용자 잔액 충전
        balanceManager.chargeBalance(user1, new BigDecimal("100000"));
        balanceManager.chargeBalance(user2, new BigDecimal("100000"));
        balanceManager.chargeBalance(user3, new BigDecimal("100000"));
        
        // when: 각 사용자가 자신의 예약 결제
        Payment payment1 = paymentProcessor.processPayment(reservation1.getId(), user1);
        Payment payment2 = paymentProcessor.processPayment(reservation2.getId(), user2);
        Payment payment3 = paymentProcessor.processPayment(reservation3.getId(), user3);
        
        // then: 모든 결제 성공
        assertThat(payment1.getUserId()).isEqualTo(user1);
        assertThat(payment2.getUserId()).isEqualTo(user2);
        assertThat(payment3.getUserId()).isEqualTo(user3);
        
        // 모든 예약이 확정 상태
        assertThat(reservationManager.getReservation(reservation1.getId()).get().getStatus())
            .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservationManager.getReservation(reservation2.getId()).get().getStatus())
            .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservationManager.getReservation(reservation3.getId()).get().getStatus())
            .isEqualTo(ReservationStatus.CONFIRMED);
        
        // 각 사용자의 잔액이 정확히 차감됨 (모두 50000원 좌석)
        assertThat(balanceManager.getBalance(user1).getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(balanceManager.getBalance(user2).getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(balanceManager.getBalance(user3).getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
    }
}
