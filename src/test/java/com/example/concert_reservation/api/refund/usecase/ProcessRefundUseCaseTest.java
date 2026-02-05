package com.example.concert_reservation.api.refund.usecase;

import com.example.concert_reservation.api.refund.dto.ProcessRefundRequest;
import com.example.concert_reservation.api.refund.dto.RefundResponse;
import com.example.concert_reservation.domain.refund.components.RefundProcessor;
import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.models.RefundStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import com.example.concert_reservation.support.exception.DomainNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessRefundUseCaseTest {

    @Mock
    private RefundProcessor refundProcessor;

    private ProcessRefundUseCase processRefundUseCase;

    @BeforeEach
    void setUp() {
        processRefundUseCase = new ProcessRefundUseCase(refundProcessor);
    }

    @Test
    void should_execute_refund_successfully() {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "Customer requested");
        Refund refund = Refund.create(1L, 1L, "user1", BigDecimal.TEN, "Customer requested");
        refund.approve();

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenReturn(refund);

        // When
        RefundResponse response = processRefundUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("user1");
        
        // Verify processor was called with correct parameters
        verify(refundProcessor).processRefund(1L, "user1", "Customer requested");
    }

    @Test
    void should_return_refund_response_with_approved_status() {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "reason");
        Refund refund = Refund.create(1L, 1L, "user1", new BigDecimal("50000"), "reason");
        refund.approve();

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenReturn(refund);

        // When
        RefundResponse response = processRefundUseCase.execute(request);

        // Then
        assertThat(response.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(response.getReason()).isEqualTo("reason");
    }

    @Test
    void should_map_refund_to_response_dto() {
        // Given
        Long paymentId = 1L;
        Long reservationId = 2L;
        String userId = "user1";
        BigDecimal amount = new BigDecimal("75000");
        String reason = "Customer requested";

        ProcessRefundRequest request = new ProcessRefundRequest(paymentId, userId, reason);
        Refund refund = Refund.create(paymentId, reservationId, userId, amount, reason);
        refund.approve();

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenReturn(refund);

        // When
        RefundResponse response = processRefundUseCase.execute(request);

        // Then
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAmount()).isEqualTo(amount);
        assertThat(response.getReason()).isEqualTo(reason);
    }

    @Test
    void should_propagate_payment_not_found_exception() {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(999L, "user1", "reason");

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenThrow(new DomainNotFoundException("결제를 찾을 수 없습니다: 999"));

        // When & Then
        assertThatThrownBy(() -> processRefundUseCase.execute(request))
            .isInstanceOf(DomainNotFoundException.class)
            .hasMessageContaining("결제를 찾을 수 없습니다");
    }

    @Test
    void should_propagate_non_owner_refund_exception() {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "different-user", "reason");

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenThrow(new DomainForbiddenException("본인의 결제만 환불할 수 있습니다"));

        // When & Then
        assertThatThrownBy(() -> processRefundUseCase.execute(request))
            .isInstanceOf(DomainForbiddenException.class)
            .hasMessageContaining("본인의 결제만 환불할 수 있습니다");
    }

    @Test
    void should_propagate_duplicate_refund_exception() {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "reason");

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenThrow(new DomainConflictException("이미 환불된 결제입니다"));

        // When & Then
        assertThatThrownBy(() -> processRefundUseCase.execute(request))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("이미 환불된 결제입니다");
    }

    @Test
    void should_propagate_invalid_reservation_status_exception() {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "reason");

        when(refundProcessor.processRefund(anyLong(), anyString(), anyString()))
            .thenThrow(new DomainConflictException("확정된 예약만 환불할 수 있습니다. 현재 상태: PENDING"));

        // When & Then
        assertThatThrownBy(() -> processRefundUseCase.execute(request))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("확정된 예약만 환불할 수 있습니다");
    }
}
