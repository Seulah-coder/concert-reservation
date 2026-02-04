package com.example.concert_reservation.domain.refund.infrastructure;

import com.example.concert_reservation.domain.refund.infrastructure.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 환불 JPA 저장소
 */
@Repository
public interface RefundJpaRepository extends JpaRepository<RefundEntity, Long> {
    /**
     * 결제 ID로 환불 조회
     */
    Optional<RefundEntity> findByPaymentId(Long paymentId);

    /**
     * 예약 ID로 환불 조회
     */
    Optional<RefundEntity> findByReservationId(Long reservationId);
}
