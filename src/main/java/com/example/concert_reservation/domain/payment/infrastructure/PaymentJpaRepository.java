package com.example.concert_reservation.domain.payment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByReservationId(Long reservationId);
    List<PaymentEntity> findByUserId(String userId);
}
