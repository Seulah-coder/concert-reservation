package com.example.concert_reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * 
 * 목적:
 * - @Async 어노테이션 활성화
 * - 이벤트 리스너 비동기 실행
 * - 외부 API 호출 시 메인 스레드 블로킹 방지
 * 
 * 스레드 풀 설정:
 * - Core: 5개 (기본 유지)
 * - Max: 20개 (피크 시 확장)
 * - Queue: 100개 (대기열)
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수
        executor.setCorePoolSize(5);
        
        // 최대 스레드 수
        executor.setMaxPoolSize(20);
        
        // 큐 용량 (대기 작업)
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("async-event-");
        
        // 종료 시 대기 중인 작업 완료
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}
