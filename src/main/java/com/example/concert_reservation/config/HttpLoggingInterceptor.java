package com.example.concert_reservation.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP 요청/응답 로깅 인터셉터
 * 모든 HTTP 요청과 응답을 로깅하여 API 호출 추적 및 모니터링 제공
 */
@Component
public class HttpLoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
    private static final String REQUEST_ID_ATTR = "requestId";
    private static final String START_TIME_ATTR = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 요청 ID 생성 (추적용)
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(REQUEST_ID_ATTR, requestId);
        
        // 시작 시간 기록
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);
        
        // 요청 정보 로깅
        String headers = Collections.list(request.getHeaderNames())
            .stream()
            .filter(this::isLoggableHeader)
            .map(name -> name + "=" + request.getHeader(name))
            .collect(Collectors.joining(", "));
        
        log.info("[{}] HTTP Request: {} {} | Headers: {{{}}} | Params: {}", 
            requestId,
            request.getMethod(),
            request.getRequestURI(),
            headers,
            request.getQueryString() != null ? request.getQueryString() : "none");
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            // 응답 정보 로깅 (상태 코드에 따라 레벨 구분)
            if (status >= 500) {
                log.error("[{}] HTTP Response: {} {} | Status: {} | Duration: {}ms | Error: {}", 
                    requestId, request.getMethod(), request.getRequestURI(), 
                    status, duration, ex != null ? ex.getMessage() : "none");
            } else if (status >= 400) {
                log.warn("[{}] HTTP Response: {} {} | Status: {} | Duration: {}ms", 
                    requestId, request.getMethod(), request.getRequestURI(), 
                    status, duration);
            } else {
                log.info("[{}] HTTP Response: {} {} | Status: {} | Duration: {}ms", 
                    requestId, request.getMethod(), request.getRequestURI(), 
                    status, duration);
            }
        }
    }
    
    /**
     * 로깅 가능한 헤더인지 확인 (민감 정보 제외)
     */
    private boolean isLoggableHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        // Authorization, Cookie 등 민감한 헤더는 제외
        return !lowerName.contains("authorization") 
            && !lowerName.contains("cookie")
            && !lowerName.contains("token");
    }
}
