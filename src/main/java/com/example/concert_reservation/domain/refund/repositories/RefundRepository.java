package com.example.concert_reservation.domain.refund.repositories;

import com.example.concert_reservation.domain.refund.models.Refund;
import java.util.Optional;

/**
 * 환불 저장소 인터페이스
 */
public interface RefundRepository {
    /**
     * 환불 정보 저장
     */
    Refund save(Refund refund);

    /**
     * 환불 ID로 조회
     */
    Optional<Refund> findById(Long id);

    /**
     * 결제 ID로 환불 조회
     */
    Optional<Refund> findByPaymentId(Long paymentId);

    /**
     * 예약 ID로 환불 조회
     */
    Optional<Refund> findByReservationId(Long reservationId);
}
