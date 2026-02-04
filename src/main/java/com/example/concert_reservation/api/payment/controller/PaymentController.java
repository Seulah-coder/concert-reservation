package com.example.concert_reservation.api.payment.controller;

import com.example.concert_reservation.api.payment.dto.PaymentResponse;
import com.example.concert_reservation.api.payment.dto.ProcessPaymentRequest;
import com.example.concert_reservation.api.payment.usecase.ProcessPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API 컨트롤러
 * 예약자만 본인의 예약에 대해 결제 가능
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final ProcessPaymentUseCase processPaymentUseCase;
    
    public PaymentController(ProcessPaymentUseCase processPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
    }
    
    /**
     * 결제 처리 API
     * POST /api/payments
     * 
     * @param request 결제 요청 (reservationId, userId)
     * @return 결제 정보
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = processPaymentUseCase.execute(request.reservationId(), request.userId());
        return ResponseEntity.ok(response);
    }
}
