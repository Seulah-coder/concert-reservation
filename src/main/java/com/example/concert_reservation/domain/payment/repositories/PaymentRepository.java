package com.example.concert_reservation.domain.payment.repositories;

import com.example.concert_reservation.domain.payment.models.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByReservationId(Long reservationId);
}
