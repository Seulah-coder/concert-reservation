package com.example.concert_reservation.api.payment.usecase;

import com.example.concert_reservation.api.payment.dto.PaymentResponse;
import com.example.concert_reservation.domain.payment.components.PaymentProcessor;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.models.PaymentStatus;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.domain.reservation.repositories.ReservationStoreRepository;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import com.example.concert_reservation.support.exception.DomainNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 처리 UseCase 테스트")
class ProcessPaymentUseCaseTest {
    
    @Mock
    private PaymentProcessor paymentProcessor;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private ReservationStoreRepository reservationRepository;
    
    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;
    
    @Test
    @DisplayName("결제 처리 성공")
    void execute_success() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        BigDecimal amount = new BigDecimal("50000");
        
        Payment expectedPayment = Payment.of(
            1L, reservationId, userId, amount,
            PaymentStatus.COMPLETED,
            LocalDateTime.now(), LocalDateTime.now()
        );
        
        Reservation mockReservation = Reservation.of(
            reservationId, userId, 1L, 10L,
            amount,  // price
            ReservationStatus.CONFIRMED,
            LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)
        );
        
        given(paymentProcessor.processPayment(reservationId, userId))
            .willReturn(expectedPayment);
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(mockReservation));
        
        // when
        PaymentResponse response = processPaymentUseCase.execute(reservationId, userId);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.reservationId()).isEqualTo(reservationId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.amount()).isEqualByComparingTo(amount);
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        
        verify(paymentProcessor).processPayment(reservationId, userId);
        verify(reservationRepository).findById(reservationId);
        // eventPublisher는 단위 테스트에서 검증 제외 (통합 테스트에서 검증)
    }
    
    @Test
    @DisplayName("예약이 없으면 결제 실패")
    void execute_reservationNotFound_throwsException() {
        // given
        Long reservationId = 999L;
        String userId = "user123";
        
        given(paymentProcessor.processPayment(reservationId, userId))
            .willThrow(new DomainNotFoundException("예약을 찾을 수 없습니다"));
        
        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(reservationId, userId))
            .isInstanceOf(DomainNotFoundException.class)
            .hasMessageContaining("예약을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("다른 사용자의 예약은 결제할 수 없다")
    void execute_differentUser_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "hacker";
        
        given(paymentProcessor.processPayment(reservationId, userId))
            .willThrow(new DomainForbiddenException("본인의 예약만 결제할 수 있습니다"));
        
        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(reservationId, userId))
            .isInstanceOf(DomainForbiddenException.class)
            .hasMessageContaining("본인의 예약만 결제할 수 있습니다");
    }
    
    @Test
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void execute_insufficientBalance_throwsException() {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        
        given(paymentProcessor.processPayment(reservationId, userId))
            .willThrow(new DomainConflictException("잔액이 부족합니다"));
        
        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(reservationId, userId))
            .isInstanceOf(DomainConflictException.class)
            .hasMessageContaining("잔액이 부족합니다");
    }
}
