-- PostgreSQL 상세 데이터 조회 스크립트
-- 작성일: 2026-02-05

\echo '======================================='
\echo '  콘서트 예약 시스템 - 데이터 현황'
\echo '======================================='
\echo ''

-- 1. 콘서트 목록
\echo '1. 콘서트 목록'
\echo '---------------------------------------'
SELECT 
    id,
    concert_name as "콘서트명",
    concert_date as "공연일",
    total_seats as "전체좌석",
    available_seats as "잔여좌석",
    (total_seats - available_seats) as "예약/판매"
FROM concert_dates
ORDER BY concert_date
LIMIT 5;

\echo ''

-- 2. 사용자별 예약 및 결제 현황
\echo '2. 사용자별 현황'
\echo '---------------------------------------'
SELECT 
    b.user_id as "사용자ID",
    b.amount as "잔액",
    COUNT(DISTINCT r.id) as "예약수",
    COUNT(DISTINCT p.id) as "결제수",
    COALESCE(SUM(p.amount), 0) as "결제총액"
FROM balance b
LEFT JOIN reservations r ON b.user_id = r.user_id
LEFT JOIN payments p ON b.user_id = p.user_id AND p.status = 'COMPLETED'
GROUP BY b.user_id, b.amount
ORDER BY b.user_id;

\echo ''

-- 3. 예약 상태별 통계
\echo '3. 예약 상태별 통계'
\echo '---------------------------------------'
SELECT 
    status as "상태",
    COUNT(*) as "건수",
    SUM(price) as "총금액"
FROM reservations
GROUP BY status
ORDER BY 
    CASE status
        WHEN 'CONFIRMED' THEN 1
        WHEN 'PENDING' THEN 2
        WHEN 'CANCELLED' THEN 3
        WHEN 'EXPIRED' THEN 4
    END;

\echo ''

-- 4. 좌석 예약 현황 (각 콘서트별)
\echo '4. 콘서트별 좌석 예약 현황'
\echo '---------------------------------------'
SELECT 
    c.concert_name as "콘서트",
    COUNT(CASE WHEN s.status = 'AVAILABLE' THEN 1 END) as "판매가능",
    COUNT(CASE WHEN s.status = 'RESERVED' THEN 1 END) as "예약완료",
    COUNT(*) as "전체좌석"
FROM concert_dates c
LEFT JOIN seats s ON c.id = s.concert_date_id
GROUP BY c.id, c.concert_name
ORDER BY c.concert_date
LIMIT 5;

\echo ''

-- 5. 최근 활동 (예약, 결제)
\echo '5. 최근 활동 내역'
\echo '---------------------------------------'
SELECT 
    '예약' as "유형",
    r.user_id as "사용자",
    c.concert_name as "콘서트",
    s.seat_number as "좌석",
    r.status as "상태",
    r.created_at as "시간"
FROM reservations r
JOIN concert_dates c ON r.concert_date_id = c.id
JOIN seats s ON r.seat_id = s.id
ORDER BY r.created_at DESC
LIMIT 10;

\echo ''

SELECT 
    '결제' as "유형",
    p.user_id as "사용자",
    '-' as "콘서트",
    '-' as "좌석",
    p.status as "상태",
    p.paid_at as "시간"
FROM payments p
ORDER BY p.paid_at DESC
LIMIT 10;

\echo ''

-- 6. 데이터베이스 통계
\echo '6. 데이터베이스 통계'
\echo '---------------------------------------'
SELECT 
    '콘서트' as "테이블",
    COUNT(*) as "레코드수"
FROM concert_dates
UNION ALL
SELECT 
    '좌석',
    COUNT(*)
FROM seats
UNION ALL
SELECT 
    '예약',
    COUNT(*)
FROM reservations
UNION ALL
SELECT 
    '결제',
    COUNT(*)
FROM payments
UNION ALL
SELECT 
    '잔액',
    COUNT(*)
FROM balance
UNION ALL
SELECT 
    '환불',
    COUNT(*)
FROM refunds;

\echo ''
\echo '======================================='
\echo '  조회 완료'
\echo '======================================='
