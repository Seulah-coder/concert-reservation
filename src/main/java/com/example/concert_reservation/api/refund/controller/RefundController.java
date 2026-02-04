package com.example.concert_reservation.api.refund.controller;

import com.example.concert_reservation.api.refund.dto.ProcessRefundRequest;
import com.example.concert_reservation.api.refund.dto.RefundResponse;
import com.example.concert_reservation.api.refund.usecase.ProcessRefundUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 환불 API 컨트롤러
 */
@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final ProcessRefundUseCase processRefundUseCase;

    public RefundController(ProcessRefundUseCase processRefundUseCase) {
        this.processRefundUseCase = processRefundUseCase;
    }

    /**
     * 환불 처리
     * POST /api/refunds
     *
     * @param request 환불 요청
     * @return 환불 응답
     */
    @PostMapping
    public ResponseEntity<RefundResponse> processRefund(@Valid @RequestBody ProcessRefundRequest request) {
        RefundResponse response = processRefundUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
