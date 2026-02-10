@echo off
REM PostgreSQL 데이터 확인 스크립트
REM 작성일: 2026-02-05

echo ======================================
echo PostgreSQL 데이터 확인
echo ======================================
echo.

echo [테이블 목록]
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation -c "\dt"

echo.
echo [콘서트 데이터 (상위 5개)]
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation -c "SELECT id, concert_name, concert_date, total_seats, available_seats FROM concert_dates LIMIT 5;"

echo.
echo [좌석 데이터 (상위 10개)]
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation -c "SELECT id, concert_date_id, seat_number, status, price FROM seats LIMIT 10;"

echo.
echo [예약 현황]
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation -c "SELECT status, COUNT(*) as count FROM reservations GROUP BY status;"

echo.
echo [좌석 상태 현황]
docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation -c "SELECT status, COUNT(*) as count FROM seats GROUP BY status;"

echo.
echo ======================================
echo PostgreSQL 직접 접속:
echo   docker exec -it concert-reservation-postgres psql -U concert_user -d concert_reservation
echo ======================================
pause
