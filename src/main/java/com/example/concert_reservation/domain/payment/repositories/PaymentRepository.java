package com.example.concert_reservation.domain.payment.repositories;

import com.example.concert_reservation.domain.payment.models.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByIdWithLock(Long id);
    Optional<Payment> findByReservationId(Long reservationId);
    List<Payment> findByUserId(String userId);
}
