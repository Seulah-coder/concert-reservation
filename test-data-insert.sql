-- PostgreSQL 테스트 데이터 생성 스크립트
-- 작성일: 2026-02-05
-- 목적: 실제 사용자 데이터로 전체 플로우 테스트

-- 1. 사용자 잔액 생성
INSERT INTO balance (user_id, amount, created_at, updated_at)
VALUES
    ('testuser001', 200000.00, NOW(), NOW()),
    ('testuser002', 150000.00, NOW(), NOW()),
    ('testuser003', 100000.00, NOW(), NOW());

-- 2. 좌석 예약 (아이유 콘서트)
INSERT INTO reservations (user_id, concert_date_id, seat_id, status, price, reserved_at, expires_at, created_at, updated_at)
VALUES
   ('testuser001', 1, 1, 'CONFIRMED', 50000.00, NOW(), NOW() + INTERVAL '5 minutes', NOW(), NOW()),
    ('testuser002', 1, 2, 'PENDING', 50000.00, NOW(), NOW() + INTERVAL '5 minutes', NOW(), NOW()),
    ('testuser003', 1, 3, 'PENDING', 50000.00, NOW(), NOW() + INTERVAL '5 minutes', NOW(), NOW());

-- 3. 좌석 상태 업데이트
UPDATE seats SET status = 'RESERVED', updated_at = NOW() WHERE id IN (1, 2, 3);

-- 4. 결제 데이터 생성 (testuser001만 결제 완료)
INSERT INTO payments (user_id, reservation_id, amount, status, paid_at, created_at)
VALUES
    ('testuser001', 1, 50000.00, 'COMPLETED', NOW(), NOW());

-- 5. 조회 쿼리
SELECT 
    '=== 사용자 잔액 ===' as 섹션;
    
SELECT 
    user_id as "사용자ID",
    amount as "잔액",
    created_at as "생성일"
FROM balance
ORDER BY user_id;

SELECT 
    '=== 예약 현황 ===' as 섹션;

SELECT 
    r.id as "예약ID",
    r.user_id as "사용자ID",
    c.concert_name as "콘서트",
    s.seat_number as "좌석번호",
    r.status as "상태",
    r.price as "가격",
    r.reserved_at as "예약시간"
FROM reservations r
JOIN concert_dates c ON r.concert_date_id = c.id
JOIN seats s ON r.seat_id = s.id
ORDER BY r.id;

SELECT 
    '=== 결제 현황 ===' as 섹션;

SELECT 
    p.id as "결제ID",
    p.user_id as "사용자ID",
    p.reservation_id as "예약ID",
    p.amount as "결제금액",
    p.status as "상태",
    p.paid_at as "결제시간"
FROM payments p
ORDER BY p.id;

SELECT 
    '=== 좌석 상태 ===' as 섹션;

SELECT 
    s.id as "좌석ID",
    c.concert_name as "콘서트",
    s.seat_number as "좌석번호",
    s.status as "상태",
    s.price as "가격"
FROM seats s
JOIN concert_dates c ON s.concert_date_id = c.id
WHERE s.id <= 10
ORDER BY s.id;

SELECT 
    '=== 통계 ===' as 섹션;

SELECT 
    '예약' as "구분",
    status as "상태",
    COUNT(*) as "건수"
FROM reservations
GROUP BY status;

SELECT 
    '좌석' as "구분",
    status as "상태",
    COUNT(*) as "건수"
FROM seats
GROUP BY status;
