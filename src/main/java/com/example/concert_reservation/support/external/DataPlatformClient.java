package com.example.concert_reservation.support.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터 플랫폼 클라이언트
 * 
 * 역할: 외부 데이터 플랫폼으로 주문 정보 전송
 * - HTTP POST 요청
 * - JSON 형식
 * - 타임아웃 설정
 * 
 * 주의:
 * - 절대 트랜잭션 내부에서 호출 금지
 * - 이벤트 리스너에서만 호출
 * - 실패 시 재시도 정책 적용
 */
@Component
public class DataPlatformClient {
    
    private static final Logger log = LoggerFactory.getLogger(DataPlatformClient.class);
    
    private final RestTemplate restTemplate;
    private final String dataPlatformUrl;
    
    public DataPlatformClient(
        RestTemplate restTemplate,
        @Value("${external.data-platform.url:http://localhost:9090/api/orders}") String dataPlatformUrl
    ) {
        this.restTemplate = restTemplate;
        this.dataPlatformUrl = dataPlatformUrl;
    }
    
    /**
     * 주문 데이터 전송
     * 
     * @param paymentId 결제 ID
     * @param reservationId 예약 ID
     * @param userId 사용자 ID
     * @param amount 결제 금액
     * @param paidAt 결제 일시
     * @param concertTitle 콘서트 제목
     * @param seatNumber 좌석 번호
     */
    public void sendOrderData(
        Long paymentId,
        Long reservationId,
        String userId,
        BigDecimal amount,
        LocalDateTime paidAt,
        String concertTitle,
        String seatNumber
    ) {
        // 요청 페이로드 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("reservationId", reservationId);
        payload.put("userId", userId);
        payload.put("amount", amount.longValue());
        payload.put("amountDecimal", amount.toString());
        payload.put("paidAt", paidAt.toString());
        payload.put("concertTitle", concertTitle);
        payload.put("seatNumber", seatNumber);
        payload.put("timestamp", LocalDateTime.now().toString());
        
        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Name", "concert-reservation");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        // 외부 API 호출 (타임아웃: 5초)
        try {
            String response = restTemplate.postForObject(
                dataPlatformUrl,
                request,
                String.class
            );
            
            log.info("데이터 플랫폼 응답: {}", response);
            
        } catch (Exception e) {
            log.error("데이터 플랫폼 호출 실패: url={}, payload={}", 
                dataPlatformUrl, payload, e);
            throw new ExternalApiException("데이터 플랫폼 전송 실패", e);
        }
    }
    
    /**
     * 외부 API 예외
     */
    public static class ExternalApiException extends RuntimeException {
        public ExternalApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
