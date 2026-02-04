package com.example.concert_reservation.api.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 환불 요청 DTO
 */
public class ProcessRefundRequest {

    @NotNull(message = "결제 ID는 필수입니다")
    private Long paymentId;

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "환불 사유는 필수입니다")
    private String reason;

    public ProcessRefundRequest() {}

    public ProcessRefundRequest(Long paymentId, String userId, String reason) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.reason = reason;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
