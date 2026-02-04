package com.example.concert_reservation.domain.queue.infrastructure;

import com.example.concert_reservation.domain.queue.infrastructure.entity.UserQueueEntity;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * QueueCoreStoreRepository 통합 테스트
 */
@DataJpaTest
@Import(QueueCoreStoreRepository.class)
@ActiveProfiles("test")
@DisplayName("QueueCoreStoreRepository 통합 테스트")
class QueueCoreStoreRepositoryTest {
    
    @Autowired
    private QueueCoreStoreRepository repository;
    
    @Autowired
    private QueueJpaRepository jpaRepository;
    
    @Test
    @DisplayName("대기열을 저장할 수 있다")
    void save_success() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        
        // when
        UserQueue saved = repository.save(queue);
        
        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("user123");
        assertThat(saved.getQueueNumber()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(QueueStatus.WAITING);
    }
    
    @Test
    @DisplayName("토큰으로 대기열을 조회할 수 있다")
    void findByToken_success() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        UserQueue saved = repository.save(queue);
        
        // when
        Optional<UserQueue> found = repository.findByToken(saved.getToken());
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user123");
        assertThat(found.get().getToken()).isEqualTo(saved.getToken());
    }
    
    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 시 빈 Optional을 반환한다")
    void findByToken_notFound_returnsEmpty() {
        // given
        QueueToken nonExistentToken = QueueToken.generate();
        
        // when
        Optional<UserQueue> found = repository.findByToken(nonExistentToken);
        
        // then
        assertThat(found).isEmpty();
    }
    
    @Test
    @DisplayName("사용자 ID로 대기열을 조회할 수 있다")
    void findByUserId_success() {
        // given
        String userId = "user123";
        UserQueue queue1 = UserQueue.create(userId, 1L);
        UserQueue queue2 = UserQueue.create(userId, 2L);
        repository.save(queue1);
        repository.save(queue2);
        
        // when
        List<UserQueue> found = repository.findByUserId(userId);
        
        // then
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(q -> q.getUserId().equals(userId));
    }
    
    @Test
    @DisplayName("특정 상태의 대기열 개수를 조회할 수 있다")
    void countByStatus_success() {
        // given
        repository.save(UserQueue.create("user1", 1L));
        repository.save(UserQueue.create("user2", 2L));
        repository.save(UserQueue.create("user3", 3L));
        
        // when
        long count = repository.countByStatus(QueueStatus.WAITING);
        
        // then
        assertThat(count).isEqualTo(3L);
    }
    
    @Test
    @DisplayName("특정 상태의 대기열을 입장 시간 순으로 조회할 수 있다")
    void findByStatusOrderByEnteredAt_success() {
        // given
        UserQueue queue1 = UserQueue.create("user1", 1L);
        UserQueue queue2 = UserQueue.create("user2", 2L);
        UserQueue queue3 = UserQueue.create("user3", 3L);
        repository.save(queue1);
        repository.save(queue2);
        repository.save(queue3);
        
        // when
        List<UserQueue> found = repository.findByStatusOrderByEnteredAt(QueueStatus.WAITING, 2);
        
        // then
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getEnteredAt()).isBeforeOrEqualTo(found.get(1).getEnteredAt());
    }
    
    @Test
    @DisplayName("사용자 ID와 상태로 대기열 존재 여부를 확인할 수 있다")
    void existsByUserIdAndStatus_exists_returnsTrue() {
        // given
        String userId = "user123";
        UserQueue queue = UserQueue.create(userId, 1L);
        repository.save(queue);
        
        // when
        boolean exists = repository.existsByUserIdAndStatus(userId, QueueStatus.WAITING);
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("사용자 ID와 상태로 대기열이 없으면 false를 반환한다")
    void existsByUserIdAndStatus_notExists_returnsFalse() {
        // given
        String userId = "user123";
        
        // when
        boolean exists = repository.existsByUserIdAndStatus(userId, QueueStatus.ACTIVE);
        
        // then
        assertThat(exists).isFalse();
    }
    
    @Test
    @DisplayName("다음 대기 번호를 조회할 수 있다")
    void getNextQueueNumber_success() {
        // given
        repository.save(UserQueue.create("user1", 5L));
        repository.save(UserQueue.create("user2", 10L));
        
        // when
        long nextNumber = repository.getNextQueueNumber();
        
        // then
        assertThat(nextNumber).isEqualTo(11L);
    }
    
    @Test
    @DisplayName("대기열이 없을 때 다음 대기 번호는 1이다")
    void getNextQueueNumber_emptyQueue_returnsOne() {
        // when
        long nextNumber = repository.getNextQueueNumber();
        
        // then
        assertThat(nextNumber).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("대기열 상태를 변경하고 저장할 수 있다")
    void save_updateStatus_success() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        UserQueue saved = repository.save(queue);
        
        // when
        saved.activate(30);
        UserQueue updated = repository.save(saved);
        
        // then
        assertThat(updated.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(updated.getExpiredAt()).isNotNull();
        
        // DB에서 다시 조회하여 확인
        Optional<UserQueue> found = repository.findByToken(saved.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(QueueStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("특정 상태이면서 대기 번호가 작은 대기열 개수를 조회할 수 있다")
    void countByStatusAndQueueNumberLessThan_success() {
        // given
        repository.save(UserQueue.create("user1", 1L));   // WAITING
        repository.save(UserQueue.create("user2", 5L));   // WAITING
        repository.save(UserQueue.create("user3", 10L));  // WAITING
        repository.save(UserQueue.create("user4", 15L));  // WAITING
        
        // when
        long count = repository.countByStatusAndQueueNumberLessThan(QueueStatus.WAITING, 10L);
        
        // then
        assertThat(count).isEqualTo(2L);  // 1번, 5번만 10보다 작음
    }
    
    @Test
    @DisplayName("대기 번호 1보다 작은 대기열은 0개다")
    void countByStatusAndQueueNumberLessThan_firstInLine_returnsZero() {
        // given
        repository.save(UserQueue.create("user1", 1L));
        repository.save(UserQueue.create("user2", 5L));
        
        // when
        long count = repository.countByStatusAndQueueNumberLessThan(QueueStatus.WAITING, 1L);
        
        // then
        assertThat(count).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("다른 상태의 대기열은 카운트하지 않는다")
    void countByStatusAndQueueNumberLessThan_onlyCountsSpecifiedStatus() {
        // given
        UserQueue queue1 = repository.save(UserQueue.create("user1", 1L));  // WAITING
        UserQueue queue2 = repository.save(UserQueue.create("user2", 5L));  // WAITING
        
        // user2를 ACTIVE로 변경
        queue2.activate(30);
        repository.save(queue2);
        
        // when
        long count = repository.countByStatusAndQueueNumberLessThan(QueueStatus.WAITING, 10L);
        
        // then
        assertThat(count).isEqualTo(1L);  // WAITING 상태인 1번만 카운트
    }
}
