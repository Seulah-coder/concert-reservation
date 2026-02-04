package com.example.concert_reservation.api.refund.controller;

import com.example.concert_reservation.api.refund.dto.ProcessRefundRequest;
import com.example.concert_reservation.api.refund.dto.RefundResponse;
import com.example.concert_reservation.api.refund.usecase.ProcessRefundUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
@Tag(name = "Refunds", description = "환불 처리 API - 결제 완료된 예약에 대한 환불")
public class RefundController {

    private final ProcessRefundUseCase processRefundUseCase;

    public RefundController(ProcessRefundUseCase processRefundUseCase) {
        this.processRefundUseCase = processRefundUseCase;
    }

    @Operation(
        summary = "환불 처리",
        description = """
            결제 완료된 예약에 대해 환불을 처리합니다.
            - 결제 금액이 사용자 잔액으로 반환됩니다.
            - 좌석이 다시 예약 가능한 상태로 변경됩니다.
            - 예약 상태가 CANCELLED로 변경됩니다.
            - 본인의 결제만 환불할 수 있습니다.
            - 이미 환불된 결제는 다시 환불할 수 없습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "환불 성공",
            content = @Content(schema = @Schema(implementation = RefundResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (권한 없음, 이미 환불됨 등)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "결제를 찾을 수 없음",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<RefundResponse> processRefund(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "환불 요청 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = ProcessRefundRequest.class))
        )
        @Valid @RequestBody ProcessRefundRequest request
    ) {
        RefundResponse response = processRefundUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
