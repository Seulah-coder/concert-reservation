package com.example.concert_reservation.domain.reservation.infrastructure;

import com.example.concert_reservation.domain.reservation.infrastructure.entity.ReservationEntity;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ReservationCoreStoreRepository.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("ReservationCoreStoreRepository 통합 테스트")
class ReservationCoreStoreRepositoryTest {
    
    @Autowired
    private ReservationCoreStoreRepository reservationCoreStoreRepository;
    
    @Autowired
    private ReservationJpaRepository reservationJpaRepository;
    
    @Test
    @DisplayName("예약을 저장할 수 있다")
    void save_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        // when
        Reservation saved = reservationCoreStoreRepository.save(reservation);
        
        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("user123");
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }
    
    @Test
    @DisplayName("사용자 ID로 예약 목록을 조회할 수 있다")
    void findByUserId_success() {
        // given
        String userId = "user123";
        reservationJpaRepository.save(new ReservationEntity(null, userId, 1L, 1L,
            new BigDecimal("50000"), "PENDING", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)));
        reservationJpaRepository.save(new ReservationEntity(null, userId, 2L, 1L,
            new BigDecimal("50000"), "PENDING", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)));
        reservationJpaRepository.save(new ReservationEntity(null, "other", 3L, 1L,
            new BigDecimal("50000"), "PENDING", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)));
        
        // when
        List<Reservation> result = reservationCoreStoreRepository.findByUserId(userId);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getUserId().equals(userId));
    }
    
    @Test
    @DisplayName("좌석 ID로 활성 예약을 조회할 수 있다")
    void findActiveBySeatId_success() {
        // given
        Long seatId = 1L;
        reservationJpaRepository.save(new ReservationEntity(null, "user123", seatId, 1L,
            new BigDecimal("50000"), "PENDING", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5)));
        
        // when
        Optional<Reservation> result = reservationCoreStoreRepository.findActiveBySeatId(seatId);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSeatId()).isEqualTo(seatId);
    }
    
    @Test
    @DisplayName("만료된 예약 목록을 조회할 수 있다")
    void findExpiredReservations_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        reservationJpaRepository.save(new ReservationEntity(null, "user1", 1L, 1L,
            new BigDecimal("50000"), "PENDING", now.minusMinutes(10), now.minusMinutes(5)));
        reservationJpaRepository.save(new ReservationEntity(null, "user2", 2L, 1L,
            new BigDecimal("50000"), "PENDING", now.minusMinutes(10), now.minusMinutes(3)));
        reservationJpaRepository.save(new ReservationEntity(null, "user3", 3L, 1L,
            new BigDecimal("50000"), "PENDING", now, now.plusMinutes(5))); // 만료 안됨
        
        // when
        List<Reservation> result = reservationCoreStoreRepository.findExpiredReservations(now);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getExpiresAt().isBefore(now));
    }
}
