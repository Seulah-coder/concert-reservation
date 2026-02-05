package com.example.concert_reservation.api.payment.usecase;

import com.example.concert_reservation.api.payment.dto.PaymentResponse;
import com.example.concert_reservation.domain.payment.components.PaymentProcessor;
import com.example.concert_reservation.domain.payment.events.PaymentCompletedEvent;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.repositories.ReservationStoreRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 처리 UseCase (이벤트 기반)
 * 
 * 설계 원칙:
 * 1. 트랜잭션 범위 최소화
 *    - 핵심 비즈니스 로직만 트랜잭션에 포함
 *    - 외부 API는 이벤트로 분리
 * 
 * 2. 이벤트 발행 (트랜잭션 커밋 후 처리)
 *    - Spring Events: 로컬 이벤트 버스 (초기 단계)
 *    - Kafka: 분산 이벤트 스트림 (확장 단계)
 * 
 * 3. Kafka 마이그레이션 준비
 *    - ApplicationEventPublisher → KafkaTemplate으로 교체 가능
 *    - 이벤트 구조는 동일하게 유지
 */
@Service
public class ProcessPaymentUseCase {
    
    private final PaymentProcessor paymentProcessor;
    private final ReservationStoreRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public ProcessPaymentUseCase(
        PaymentProcessor paymentProcessor,
        ReservationStoreRepository reservationRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.paymentProcessor = paymentProcessor;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 결제 처리
     * 
     * 흐름:
     * 1. [트랜잭션] 핵심 결제 로직 (80ms)
     * 2. [트랜잭션 커밋]
     * 3. [이벤트 발행] PaymentCompletedEvent
     * 4. [리스너 실행] 외부 API 호출 (비동기)
     * 
     * @param reservationId 예약 ID
     * @param userId 결제 요청 사용자 ID (예약자)
     * @return 결제 정보
     */
    @Transactional
    public PaymentResponse execute(Long reservationId, String userId) {
        // 1. 핵심 결제 처리 (트랜잭션 내)
        Payment payment = paymentProcessor.processPayment(reservationId, userId);
        
        // 2. 이벤트 발행 (트랜잭션 커밋 후 리스너 실행)
        publishPaymentCompletedEvent(payment);
        
        return PaymentResponse.from(payment);
    }
    
    /**
     * 결제 완료 이벤트 발행
     * 
     * 현재: Spring Events (ApplicationEventPublisher)
     * 나중: Kafka (KafkaTemplate) ← 코드 변경 최소화
     * 
     * 이벤트 리스너에서 처리:
     * - 데이터 플랫폼 전송
     * - 알림 발송
     * - 통계 집계
     */
    private void publishPaymentCompletedEvent(Payment payment) {
        // 예약 정보 조회 (읽기 전용, 트랜잭션 내)
        Reservation reservation = reservationRepository
            .findById(payment.getReservationId())
            .orElseThrow();
        
        // 이벤트 생성
        PaymentCompletedEvent event = PaymentCompletedEvent.of(
            payment.getId(),
            reservation.getId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getPaidAt(),
            "Concert Title",  // TODO: Concert 정보 추가
            "Seat Info"       // TODO: Seat 정보 추가
        );
        
        // Spring Events로 발행 (나중에 Kafka로 교체)
        eventPublisher.publishEvent(event);
    }
}
