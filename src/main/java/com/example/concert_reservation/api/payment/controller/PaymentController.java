package com.example.concert_reservation.api.payment.controller;

import com.example.concert_reservation.api.payment.dto.PaymentResponse;
import com.example.concert_reservation.api.payment.dto.ProcessPaymentRequest;
import com.example.concert_reservation.api.payment.usecase.ProcessPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "결제 처리 API - 예약에 대한 결제 완료")
public class PaymentController {
    
    private final ProcessPaymentUseCase processPaymentUseCase;
    
    public PaymentController(ProcessPaymentUseCase processPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
    }
    
    @Operation(
        summary = "결제 처리",
        description = """
            예약에 대한 결제를 처리합니다.
            - 예약 생성 후 5분 이내에 결제해야 합니다.
            - 사용자의 잔액에서 좌석 가격만큼 차감됩니다.
            - 결제 완료 시 좌석이 최종 판매 상태로 변경됩니다.
            - 본인의 예약만 결제할 수 있습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "결제 성공",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (만료된 예약, 권한 없음, 잔액 부족 등)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "예약을 찾을 수 없음",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "결제 요청 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = ProcessPaymentRequest.class))
        )
        @Valid @RequestBody ProcessPaymentRequest request
    ) {
        PaymentResponse response = processPaymentUseCase.execute(request.reservationId(), request.userId());
        return ResponseEntity.ok(response);
    }
}
