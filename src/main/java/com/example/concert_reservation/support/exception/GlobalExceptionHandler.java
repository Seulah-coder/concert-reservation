package com.example.concert_reservation.support.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 전역 예외 처리기
 * REST API의 모든 예외를 일관된 형식으로 처리하고 적절한 로깅 수행
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * BaseException 계열 예외 처리
     * LogLevel에 따라 적절한 로그 레벨로 기록
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();
        
        // LogLevel에 따른 로깅
        switch (e.getLogLevel()) {
            case ERROR -> log.error("[{}] {} - data: {}", 
                errorCode.getCode(), e.getMessage(), e.getData(), e);
            case WARN -> log.warn("[{}] {} - data: {}", 
                errorCode.getCode(), e.getMessage(), e.getData());
            case INFO -> log.info("[{}] {} - data: {}", 
                errorCode.getCode(), e.getMessage(), e.getData());
        }
        
        ErrorResponse error = new ErrorResponse(
            errorCode.getHttpStatus().value(),
            e.getMessage(),
            LocalDateTime.now(),
            Map.of("code", errorCode.getCode())
        );
        
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 잘못된 요청 (비즈니스 로직 위반)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * 잘못된 인자 (유효성 검증 실패)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Bean Validation 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "입력값 검증에 실패했습니다",
            LocalDateTime.now(),
            errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 리소스 없음 (도메인 조회 실패)
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException ex) {
        log.warn("NoSuchElementException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * 기타 예외 (Unhandled Exception)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("UnhandledException: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "서버 내부 오류가 발생했습니다",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
