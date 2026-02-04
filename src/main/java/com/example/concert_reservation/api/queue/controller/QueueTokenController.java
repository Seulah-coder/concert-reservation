package com.example.concert_reservation.api.queue.controller;

import com.example.concert_reservation.api.queue.dto.IssueTokenRequest;
import com.example.concert_reservation.api.queue.dto.IssueTokenResponse;
import com.example.concert_reservation.api.queue.dto.QueueStatusResponse;
import com.example.concert_reservation.api.queue.usecase.GetQueueStatusUseCase;
import com.example.concert_reservation.api.queue.usecase.IssueQueueTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/queue")
@Tag(name = "Queue", description = "대기열 관리 API - 토큰 발급 및 대기 상태 조회")
public class QueueTokenController {
    
    private final IssueQueueTokenUseCase issueQueueTokenUseCase;
    private final GetQueueStatusUseCase getQueueStatusUseCase;
    
    public QueueTokenController(IssueQueueTokenUseCase issueQueueTokenUseCase,
                                GetQueueStatusUseCase getQueueStatusUseCase) {
        this.issueQueueTokenUseCase = issueQueueTokenUseCase;
        this.getQueueStatusUseCase = getQueueStatusUseCase;
    }
    
    @Operation(
        summary = "대기열 토큰 발급",
        description = """
            대기열 진입을 위한 토큰을 발급합니다.
            - 피크 시간대에 시스템 부하를 제어하기 위해 사용됩니다.
            - 발급된 토큰으로 대기열 상태를 폴링할 수 있습니다.
            - ACTIVE 상태가 되면 예약/결제를 진행할 수 있습니다.
            - 토큰은 30분간 유효합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "토큰 발급 성공",
            content = @Content(schema = @Schema(implementation = IssueTokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (유효하지 않은 사용자 ID)",
            content = @Content
        )
    })
    @PostMapping("/token")
    public ResponseEntity<IssueTokenResponse> issueToken(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "토큰 발급 요청 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = IssueTokenRequest.class))
        )
        @Valid @RequestBody IssueTokenRequest request
    ) {
        IssueTokenResponse response = issueQueueTokenUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "대기열 상태 조회",
        description = """
            토큰의 현재 대기열 상태를 조회합니다.
            - 클라이언트는 이 API를 주기적으로 폴링해야 합니다 (권장: 5-10초 간격).
            - 대기 순서(queuePosition)와 예상 대기 시간을 확인할 수 있습니다.
            - 상태가 ACTIVE가 되면 예약을 진행할 수 있습니다.
            - 상태가 EXPIRED인 경우 새로운 토큰을 발급받아야 합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "상태 조회 성공",
            content = @Content(schema = @Schema(implementation = QueueStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "유효하지 않은 토큰",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "토큰을 찾을 수 없음",
            content = @Content
        )
    })
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
        @Parameter(
            description = "대기열 토큰 (토큰 발급 시 받은 값)",
            example = "abc123def456",
            required = true
        )
        @RequestHeader("X-Queue-Token") String token
    ) {
        QueueStatusResponse response = getQueueStatusUseCase.execute(token);
        return ResponseEntity.ok(response);
    }
}
