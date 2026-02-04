package com.example.concert_reservation.api.refund.usecase;

import com.example.concert_reservation.api.refund.dto.ProcessRefundRequest;
import com.example.concert_reservation.api.refund.dto.RefundResponse;
import com.example.concert_reservation.domain.refund.components.RefundProcessor;
import com.example.concert_reservation.domain.refund.models.Refund;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 처리 유스케이스
 */
@Service
public class ProcessRefundUseCase {

    private final RefundProcessor refundProcessor;

    public ProcessRefundUseCase(RefundProcessor refundProcessor) {
        this.refundProcessor = refundProcessor;
    }

    /**
     * 결제에 대한 환불 처리
     *
     * @param request 환불 요청
     * @return 환불 응답
     */
    @Transactional
    public RefundResponse execute(@Valid ProcessRefundRequest request) {
        Refund refund = refundProcessor.processRefund(
            request.getPaymentId(),
            request.getUserId(),
            request.getReason()
        );
        return RefundResponse.from(refund);
    }
}
