# Spring Events → Kafka 마이그레이션 가이드

## 📊 왜 단계적 마이그레이션인가?

### Phase 1: Spring Events (현재)
```
[UseCase] → [ApplicationEventPublisher]
                ↓ (메모리)
           [@TransactionalEventListener]
                ↓
           [DataPlatformClient]
```

**장점:**
- 구현 간단 (Spring 기본 기능)
- 외부 의존성 없음
- 빠른 MVP 출시

**단점:**
- 단일 서버만 처리
- 서버 재시작 시 이벤트 유실
- 확장성 제한

---

### Phase 2: Kafka (확장)
```
[UseCase] → [KafkaTemplate]
                ↓ (Kafka Cluster)
           [Consumer Group]
                ↓
       [DataPlatform / Notification / Analytics]
```

**장점:**
- 분산 처리 (여러 서버)
- 이벤트 영속화 (재처리 가능)
- 수평 확장
- 다중 Consumer

**단점:**
- 복잡도 증가
- Kafka 운영 필요
- 비용 증가

---

## 🎯 언제 Kafka로 전환할까?

| 지표 | Spring Events | Kafka 필요 |
|------|--------------|-----------|
| **이벤트 처리량** | < 1,000건/초 | > 10,000건/초 |
| **서버 대수** | 1-3대 | 5대 이상 |
| **이벤트 유실 허용** | 가능 | 불가능 |
| **다중 Consumer** | 불필요 | 필요 (분석, 알림 등) |
| **재처리 필요** | 없음 | 있음 |

**권장 전환 시점:**
- DAU > 50만
- 결제 > 1만건/일
- 마이크로서비스 분리 필요

---

## 🔄 단계적 마이그레이션 전략

### Step 1: 추상화 레이어 추가 (현재)

```java
// src/main/java/.../domain/payment/events/EventPublisher.java
public interface EventPublisher {
    void publish(Object event);
}

// Spring Events 구현
@Component
@Primary
public class SpringEventPublisher implements EventPublisher {
    
    private final ApplicationEventPublisher publisher;
    
    @Override
    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}

// UseCase에서 사용
@Service
public class ProcessPaymentUseCase {
    
    private final EventPublisher eventPublisher;  // 인터페이스 의존
    
    public void execute(...) {
        Payment payment = processPayment();
        
        PaymentCompletedEvent event = ...;
        eventPublisher.publish(event);  // 구현체 교체 가능
    }
}
```

---

### Step 2: Kafka 구현체 추가

```java
// Kafka 구현 (처음엔 비활성화)
@Component
@Profile("kafka")  // kafka 프로파일에서만 활성화
public class KafkaEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(Object event) {
        if (event instanceof PaymentCompletedEvent paymentEvent) {
            kafkaTemplate.send("payment.completed", 
                paymentEvent.userId(), 
                paymentEvent);
        }
    }
}
```

---

### Step 3: 이중 발행 (전환 기간)

```java
@Component
@Profile("migration")
public class DualEventPublisher implements EventPublisher {
    
    private final ApplicationEventPublisher springPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(Object event) {
        // 1. Spring Events (기존 리스너 처리)
        springPublisher.publishEvent(event);
        
        // 2. Kafka로도 발행 (새로운 Consumer 처리)
        sendToKafka(event);
        
        // 검증 기간: 두 시스템 동시 실행하며 결과 비교
    }
}
```

---

### Step 4: Kafka 완전 전환

```java
@Component
@Primary
public class KafkaEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(Object event) {
        // Kafka만 사용
        kafkaTemplate.send(getTopic(event), getKey(event), event);
    }
}
```

---

## 💻 Kafka 구현 상세

### 1. 의존성 추가

```gradle
// build.gradle
dependencies {
    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'
    
    // JSON 직렬화
    implementation 'com.fasterxml.jackson.core:jackson-databind'
}
```

---

### 2. Kafka 설정

```java
// config/KafkaProducerConfig.java
@Configuration
@EnableKafka
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 신뢰성 설정
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 복제본 확인
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // 순서 보장
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3
      compression-type: gzip
```

---

### 3. Producer 구현

```java
// infrastructure/kafka/PaymentEventProducer.java
@Component
public class PaymentEventProducer implements EventPublisher {
    
    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(Object event) {
        if (event instanceof PaymentCompletedEvent paymentEvent) {
            sendPaymentCompletedEvent(paymentEvent);
        }
    }
    
    private void sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        // userId를 파티션 키로 사용 (같은 사용자 이벤트는 순서 보장)
        kafkaTemplate.send(
            PAYMENT_COMPLETED_TOPIC,
            event.userId(),  // key
            event            // value
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka 전송 실패: paymentId={}", event.paymentId(), ex);
                // 실패 처리: DB에 저장 → 배치로 재전송
            } else {
                log.debug("Kafka 전송 성공: paymentId={}, offset={}", 
                    event.paymentId(), result.getRecordMetadata().offset());
            }
        });
    }
}
```

---

### 4. Consumer 구현

```java
// infrastructure/kafka/PaymentEventConsumer.java
@Component
@Slf4j
public class PaymentEventConsumer {
    
    private final DataPlatformClient dataPlatformClient;
    
    /**
     * 결제 완료 이벤트 소비
     * 
     * Consumer Group: data-platform-writer
     * - 여러 인스턴스가 파티션 분산 처리
     * - 한 파티션은 한 Consumer만 처리 (순서 보장)
     */
    @KafkaListener(
        topics = "payment.completed",
        groupId = "data-platform-writer",
        concurrency = "3"  // 3개 스레드로 병렬 처리
    )
    public void consumePaymentCompleted(
        @Payload PaymentCompletedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("이벤트 수신: paymentId={}, partition={}, offset={}", 
            event.paymentId(), partition, offset);
        
        try {
            // 외부 API 호출
            dataPlatformClient.sendOrderData(
                event.paymentId(),
                event.reservationId(),
                event.userId(),
                event.amount(),
                event.paidAt(),
                event.concertTitle(),
                event.seatNumber()
            );
            
            log.info("처리 완료: paymentId={}", event.paymentId());
            
        } catch (Exception e) {
            log.error("처리 실패: paymentId={}", event.paymentId(), e);
            // 재처리 또는 DLQ(Dead Letter Queue)로 전송
            throw e;
        }
    }
}
```

---

### 5. 다중 Consumer 구성

```java
// 데이터 플랫폼 전송
@KafkaListener(topics = "payment.completed", groupId = "data-platform-writer")
public void sendToDataPlatform(PaymentCompletedEvent event) {
    dataPlatformClient.send(event);
}

// 알림 발송 (별도 Consumer Group)
@KafkaListener(topics = "payment.completed", groupId = "notification-sender")
public void sendNotification(PaymentCompletedEvent event) {
    notificationService.send(event.userId(), "결제가 완료되었습니다");
}

// 통계 집계 (별도 Consumer Group)
@KafkaListener(topics = "payment.completed", groupId = "analytics-processor")
public void updateAnalytics(PaymentCompletedEvent event) {
    analyticsService.recordPayment(event);
}
```

**장점:**
- 3개의 독립적인 Consumer
- 서로 영향 없음
- 각자 속도로 처리

---

## 📊 성능 비교

### Spring Events

```
처리량: 1,000 이벤트/초
서버 1대: 1,000/초
서버 5대: 1,000/초 (분산 안 됨)
```

### Kafka

```
처리량: 100,000 이벤트/초
파티션 10개: 10,000/초 × 10 = 100,000/초
Consumer 30개: 파티션별 3개씩 처리
```

---

## 🛡️ 신뢰성 비교

### Spring Events

| 시나리오 | 결과 |
|---------|------|
| 서버 재시작 | ❌ 이벤트 유실 |
| 리스너 실패 | ❌ 재처리 불가 |
| 네트워크 장애 | ❌ 유실 |

### Kafka

| 시나리오 | 결과 |
|---------|------|
| 서버 재시작 | ✅ Offset 기억, 재개 |
| Consumer 실패 | ✅ 재처리 가능 |
| 네트워크 장애 | ✅ 복제본 유지 |

---

## 💰 비용 비교

### Spring Events
```
추가 비용: $0
운영 복잡도: 낮음
```

### Kafka (AWS MSK)
```
3 브로커: $600/월
운영 복잡도: 높음
학습 곡선: 가파름
```

---

## 🎯 마이그레이션 타임라인

### Week 1-2: 추상화 레이어

```java
// EventPublisher 인터페이스 도입
// 기존 코드 리팩토링
// 테스트 작성
```

### Week 3-4: Kafka 환경 구축

```bash
# Docker Compose로 로컬 테스트
docker-compose up kafka zookeeper

# Topic 생성
kafka-topics --create --topic payment.completed \
  --partitions 10 \
  --replication-factor 3
```

### Week 5-6: 이중 발행 (검증)

```java
// Spring Events + Kafka 동시 실행
// 결과 비교
// 성능 측정
```

### Week 7-8: Kafka 완전 전환

```java
// Spring Events 리스너 제거
// Kafka Consumer만 사용
// 모니터링 강화
```

---

## ✅ 체크리스트

### Kafka 도입 전 확인

- [ ] 이벤트 처리량 > 10,000/초
- [ ] 서버 5대 이상
- [ ] 이벤트 유실 불가
- [ ] 재처리 필요
- [ ] 다중 Consumer 필요
- [ ] 운영 팀 준비 완료
- [ ] 예산 확보 ($600+/월)

### 하나라도 No면 → Spring Events 유지

---

## 💡 결론

### 현재 (DAU < 50만)
```
✅ Spring Events 사용
- 간단하고 효과적
- 비용 $0
- 빠른 개발
```

### 미래 (DAU > 50만)
```
✅ Kafka 전환
- 확장성 확보
- 신뢰성 향상
- 다중 시스템 연계
```

**핵심: 지금은 Spring Events로 시작하고, 필요할 때 Kafka로 전환하세요!** 🚀
