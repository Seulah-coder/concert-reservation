package com.example.concert_reservation.domain.payment.components;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.repositories.PaymentRepository;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import com.example.concert_reservation.support.exception.DomainNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessor 단위 테스트 - 예약자만 결제 가능")
class PaymentProcessorTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private ReservationManager reservationManager;
    
    @Mock
    private BalanceManager balanceManager;
    
    @InjectMocks
    private PaymentProcessor paymentProcessor;
    
    @Test
    @DisplayName("예약자 본인이 PENDING 상태의 예약을 결제할 수 있다")
    void processPayment_validReservation_success() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        BigDecimal price = new BigDecimal("50000");
        
        Reservation reservation = Reservation.create(userId, 1L, 1L, price);
        Payment payment = Payment.create(reservationId, userId, price);
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(reservation));
        given(paymentRepository.findByReservationId(reservationId)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);
        
        // when
        Payment result = paymentProcessor.processPayment(reservationId, userId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualByComparingTo(price);
        
        verify(balanceManager).useBalance(userId, price);
        verify(reservationManager).confirmReservation(reservationId);
        verify(paymentRepository).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("다른 사용자는 타인의 예약을 결제할 수 없다")
    void processPayment_differentUser_throwsException() {
        // given
        Long reservationId = 1L;
        String reservationOwner = "user123";
        String paymentAttemptUser = "hacker456";
        
        Reservation reservation = Reservation.create(reservationOwner, 1L, 1L, new BigDecimal("50000"));
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(reservation));
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, paymentAttemptUser))
            .isInstanceOf(DomainForbiddenException.class)
            .hasMessageContaining("본인의 예약만 결제할 수 있습니다");
        
        verify(balanceManager, never()).useBalance(anyString(), any());
        verify(reservationManager, never()).confirmReservation(anyLong());
        verify(paymentRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("CONFIRMED 상태의 예약은 결제할 수 없다")
    void processPayment_alreadyConfirmed_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        
        Reservation reservation = Reservation.create(userId, 1L, 1L, new BigDecimal("50000"));
        // 이미 확정된 상태로 변경
        Reservation confirmedReservation = Reservation.of(
            1L, userId, 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CONFIRMED,
            null,
            null
        );
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(confirmedReservation));
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("예약 상태가 올바르지 않습니다");
        
        verify(balanceManager, never()).useBalance(anyString(), any());
        verify(reservationManager, never()).confirmReservation(anyLong());
    }
    
    @Test
    @DisplayName("EXPIRED 상태의 예약은 결제할 수 없다")
    void processPayment_expired_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        
        Reservation expiredReservation = Reservation.of(
            1L, userId, 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.EXPIRED,
            null, null
        );
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(expiredReservation));
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("예약 상태가 올바르지 않습니다");
    }
    
    @Test
    @DisplayName("CANCELLED 상태의 예약은 결제할 수 없다")
    void processPayment_cancelled_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        
        Reservation cancelledReservation = Reservation.of(
            1L, userId, 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CANCELLED,
            null, null
        );
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(cancelledReservation));
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("예약 상태가 올바르지 않습니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 예약은 결제할 수 없다")
    void processPayment_notFound_throwsException() {
        // given
        Long reservationId = 999L;
        String userId = "user123";
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, userId))
            .isInstanceOf(DomainNotFoundException.class)
            .hasMessageContaining("예약을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("이미 결제된 예약은 중복 결제할 수 없다")
    void processPayment_alreadyPaid_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        BigDecimal price = new BigDecimal("50000");
        
        Reservation reservation = Reservation.create(userId, 1L, 1L, price);
        Payment existingPayment = Payment.create(reservationId, userId, price);
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(reservation));
        given(paymentRepository.findByReservationId(reservationId)).willReturn(Optional.of(existingPayment));
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("이미 결제된 예약입니다");
        
        verify(balanceManager, never()).useBalance(anyString(), any());
    }
    
    @Test
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void processPayment_insufficientBalance_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        BigDecimal price = new BigDecimal("50000");
        
        Reservation reservation = Reservation.create(userId, 1L, 1L, price);
        
        given(reservationManager.getReservation(reservationId)).willReturn(Optional.of(reservation));
        given(paymentRepository.findByReservationId(reservationId)).willReturn(Optional.empty());
        willThrow(new IllegalStateException("잔액이 부족합니다"))
            .given(balanceManager).useBalance(userId, price);
        
        // when & then
        assertThatThrownBy(() -> paymentProcessor.processPayment(reservationId, userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("잔액이 부족합니다");
        
        verify(reservationManager, never()).confirmReservation(anyLong());
        verify(paymentRepository, never()).save(any());
    }
}
