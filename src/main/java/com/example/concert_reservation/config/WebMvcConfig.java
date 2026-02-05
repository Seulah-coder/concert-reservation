package com.example.concert_reservation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정
 * Interceptor 등록 및 적용 경로 설정
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final QueueTokenInterceptor queueTokenInterceptor;
    private final HttpLoggingInterceptor httpLoggingInterceptor;
    
    public WebMvcConfig(QueueTokenInterceptor queueTokenInterceptor,
                       HttpLoggingInterceptor httpLoggingInterceptor) {
        this.queueTokenInterceptor = queueTokenInterceptor;
        this.httpLoggingInterceptor = httpLoggingInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // HTTP 로깅 인터셉터 (모든 요청에 대해 먼저 적용)
        registry.addInterceptor(httpLoggingInterceptor)
                .order(1)
                .addPathPatterns("/**");
        
        // 토큰 검증 인터셉터 (로깅 이후 적용)
        registry.addInterceptor(queueTokenInterceptor)
                .order(2)
                // 토큰 검증이 필요한 API 경로
                .addPathPatterns(
                    "/api/v1/reservations/**",  // 예약 생성, 취소
                    "/api/payments/**",          // 결제
                    "/api/refunds/**"            // 환불
                )
                // 토큰 검증에서 제외할 API 경로 (명시적 제외)
                .excludePathPatterns(
                    "/api/v1/queue/**",          // 대기열 관리 (토큰 발급, 상태 조회)
                    "/api/balance/**",           // 잔액 관리 (충전, 조회)
                    "/api/v1/concerts/**"        // 콘서트 조회 (날짜, 좌석)
                );
    }
}
