package com.example.concert_reservation.domain.refund.models;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RefundTest {

    @Test
    void should_create_refund_with_pending_status() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        BigDecimal amount = new BigDecimal("50000");
        String reason = "Customer requested";

        // When
        Refund refund = Refund.create(paymentId, reservationId, userId, amount, reason);

        // Then
        assertThat(refund.getPaymentId()).isEqualTo(paymentId);
        assertThat(refund.getReservationId()).isEqualTo(reservationId);
        assertThat(refund.getUserId()).isEqualTo(userId);
        assertThat(refund.getAmount()).isEqualTo(amount);
        assertThat(refund.getReason()).isEqualTo(reason);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
    }

    @Test
    void should_approve_refund() {
        // Given
        Refund refund = Refund.create(1L, 1L, "user1", BigDecimal.TEN, "reason");

        // When
        refund.approve();

        // Then
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(refund.isApproved()).isTrue();
    }

    @Test
    void should_reject_refund() {
        // Given
        Refund refund = Refund.create(1L, 1L, "user1", BigDecimal.TEN, "reason");

        // When
        refund.reject();

        // Then
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(refund.isPending()).isFalse();
    }

    @Test
    void should_fail_refund() {
        // Given
        Refund refund = Refund.create(1L, 1L, "user1", BigDecimal.TEN, "reason");

        // When
        refund.fail();

        // Then
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
    }

    @Test
    void should_restore_refund_from_database() {
        // Given
        Long id = 1L;
        Long paymentId = 1L;
        Long reservationId = 1L;
        String userId = "user1";
        BigDecimal amount = new BigDecimal("50000");
        String reason = "reason";
        RefundStatus status = RefundStatus.APPROVED;

        // When
        Refund refund = Refund.of(id, paymentId, reservationId, userId, amount, reason, status, null, null);

        // Then
        assertThat(refund.getId()).isEqualTo(id);
        assertThat(refund.getStatus()).isEqualTo(status);
        assertThat(refund.getAmount()).isEqualTo(amount);
    }
}
