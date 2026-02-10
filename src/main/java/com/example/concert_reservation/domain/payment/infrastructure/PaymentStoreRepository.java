package com.example.concert_reservation.domain.payment.infrastructure;

import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.repositories.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentStoreRepository implements PaymentRepository {
    
    private final PaymentJpaRepository paymentJpaRepository;
    
    public PaymentStoreRepository(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }
    
    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.from(payment);
        PaymentEntity saved = paymentJpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id)
            .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByIdWithLock(Long id) {
        return paymentJpaRepository.findByIdWithLock(id)
            .map(PaymentEntity::toDomain);
    }
    
    @Override
    public Optional<Payment> findByReservationId(Long reservationId) {
        return paymentJpaRepository.findByReservationId(reservationId)
            .map(PaymentEntity::toDomain);
    }
    
    @Override
    public List<Payment> findByUserId(String userId) {
        return paymentJpaRepository.findByUserId(userId)
            .stream()
            .map(PaymentEntity::toDomain)
            .toList();
    }
}
