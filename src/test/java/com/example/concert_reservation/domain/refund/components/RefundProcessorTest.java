package com.example.concert_reservation.domain.refund.components;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.repositories.PaymentRepository;
import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.repositories.RefundRepository;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import com.example.concert_reservation.support.exception.DomainNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundProcessorTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationManager reservationManager;

    @Mock
    private BalanceManager balanceManager;

    private RefundProcessor refundProcessor;

    @BeforeEach
    void setUp() {
        refundProcessor = new RefundProcessor(
            refundRepository,
            paymentRepository,
            reservationManager,
            balanceManager
        );
    }

    @Test
    void should_throw_domain_not_found_exception_when_payment_not_exists() {
        // Given
        Long paymentId = 999L;
        String userId = "user1";
        String reason = "Customer requested";

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refundProcessor.processRefund(paymentId, userId, reason))
            .isInstanceOf(DomainNotFoundException.class)
            .hasMessageContaining("결제를 찾을 수 없습니다");
    }

    @Test
    void should_throw_forbidden_exception_when_non_owner_requests_refund() {
        // Given
        Long paymentId = 1L;
        String userId = "user2";
        String ownerUserId = "user1";
        String reason = "Customer requested";

        Payment payment = Payment.create(1L, ownerUserId, BigDecimal.TEN);

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));

        // When & Then
        assertThatThrownBy(() -> refundProcessor.processRefund(paymentId, userId, reason))
            .isInstanceOf(DomainForbiddenException.class)
            .hasMessageContaining("본인의 결제만 환불할 수 있습니다");
    }

    @Test
    void should_throw_conflict_exception_when_already_refunded() {
        // Given
        Long paymentId = 1L;
        String userId = "user1";
        String reason = "Customer requested";

        Payment payment = Payment.create(1L, userId, BigDecimal.TEN);

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.of(Refund.create(paymentId, 1L, userId, BigDecimal.TEN, "already refunded")));

        // When & Then
        assertThatThrownBy(() -> refundProcessor.processRefund(paymentId, userId, reason))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("이미 환불된 결제입니다");
    }

    @Test
    void should_throw_not_found_exception_when_reservation_not_exists() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        String reason = "Customer requested";

        Payment payment = Payment.create(reservationId, userId, BigDecimal.TEN);

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.empty());
        when(reservationManager.getReservation(reservationId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refundProcessor.processRefund(paymentId, userId, reason))
            .isInstanceOf(DomainNotFoundException.class)
            .hasMessageContaining("예약을 찾을 수 없습니다");
    }

    @Test
    void should_throw_conflict_exception_when_reservation_not_confirmed() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        String reason = "Customer requested";

        Payment payment = Payment.create(reservationId, userId, BigDecimal.TEN);
        Reservation reservation = Reservation.create(userId, 1L, 1L, BigDecimal.TEN);

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.empty());
        when(reservationManager.getReservation(reservationId))
            .thenReturn(Optional.of(reservation));

        // When & Then
        assertThatThrownBy(() -> refundProcessor.processRefund(paymentId, userId, reason))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("확정된 예약만 환불할 수 있습니다");
    }

    @Test
    void should_throw_conflict_exception_when_balance_recovery_fails() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        String reason = "Customer requested";
        BigDecimal amount = new BigDecimal("50000");

        Payment payment = Payment.create(reservationId, userId, amount);
        Reservation reservation = Reservation.of(
            reservationId, userId, 1L, 1L, amount, ReservationStatus.CONFIRMED, null, null
        );

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.empty());
        when(reservationManager.getReservation(reservationId))
            .thenReturn(Optional.of(reservation));
        doThrow(new IllegalStateException("Balance overflow"))
            .when(balanceManager).chargeBalance(userId, amount);

        // When & Then
        assertThatThrownBy(() -> refundProcessor.processRefund(paymentId, userId, reason))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("잔액 복구에 실패했습니다");
    }

    @Test
    void should_successfully_process_refund() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        String reason = "Customer requested";
        BigDecimal amount = new BigDecimal("50000");

        Payment payment = Payment.create(reservationId, userId, amount);
        Reservation reservation = Reservation.of(
            reservationId, userId, 1L, 1L, amount, ReservationStatus.CONFIRMED, null, null
        );

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.empty());
        when(reservationManager.getReservation(reservationId))
            .thenReturn(Optional.of(reservation));
        when(refundRepository.save(any(Refund.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Refund refund = refundProcessor.processRefund(paymentId, userId, reason);

        // Then
        assertThat(refund).isNotNull();
        assertThat(refund.getPaymentId()).isEqualTo(paymentId);
        assertThat(refund.getUserId()).isEqualTo(userId);
        assertThat(refund.getAmount()).isEqualTo(amount);
        assertThat(refund.isApproved()).isTrue();

        verify(balanceManager).chargeBalance(userId, amount);
        verify(reservationManager).cancelReservation(any(Reservation.class));
        verify(refundRepository).save(any(Refund.class));
    }

    @Test
    void should_update_reservation_status_to_cancelled() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        String reason = "Customer requested";
        BigDecimal amount = new BigDecimal("50000");

        Payment payment = Payment.create(reservationId, userId, amount);
        Reservation reservation = Reservation.of(
            reservationId, userId, 1L, 1L, amount, ReservationStatus.CONFIRMED, null, null
        );

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.empty());
        when(reservationManager.getReservation(reservationId))
            .thenReturn(Optional.of(reservation));
        when(refundRepository.save(any(Refund.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        refundProcessor.processRefund(paymentId, userId, reason);

        // Then
        verify(reservationManager).cancelReservation(any(Reservation.class));
    }

    @Test
    void should_refund_balance_to_user() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        String reason = "Customer requested";
        BigDecimal amount = new BigDecimal("50000");

        Payment payment = Payment.create(reservationId, userId, amount);
        Reservation reservation = Reservation.of(
            reservationId, userId, 1L, 1L, amount, ReservationStatus.CONFIRMED, null, null
        );

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
            .thenReturn(Optional.empty());
        when(reservationManager.getReservation(reservationId))
            .thenReturn(Optional.of(reservation));
        when(refundRepository.save(any(Refund.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        refundProcessor.processRefund(paymentId, userId, reason);

        // Then
        verify(balanceManager).chargeBalance(userId, amount);
    }
}
