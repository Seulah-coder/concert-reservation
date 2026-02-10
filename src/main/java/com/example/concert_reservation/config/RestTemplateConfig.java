package com.example.concert_reservation.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 * 
 * 외부 API 호출 설정:
 * - 연결 타임아웃: 3초
 * - 읽기 타임아웃: 5초
 * - 재시도는 @Retryable에서 처리
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .requestFactory(() -> {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(3000);  // 연결 타임아웃: 3초
                factory.setReadTimeout(5000);     // 읽기 타임아웃: 5초
                return factory;
            })
            .build();
    }
}
