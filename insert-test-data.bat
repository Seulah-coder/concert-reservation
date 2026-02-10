@echo off
REM PostgreSQL 테스트 데이터 생성 및 확인 스크립트
REM 작성일: 2026-02-05

echo ======================================
echo PostgreSQL 테스트 데이터 생성
echo ======================================
echo.

echo [1/2] 테스트 데이터 삽입 중...
docker exec -i concert-reservation-postgres psql -U concert_user -d concert_reservation < test-data-insert.sql

echo.
echo ======================================
echo 데이터 생성 완료!
echo ======================================
echo.

echo [추가 조회 옵션]
echo   1. 전체 예약 목록 보기
echo   2. 결제 상세 내역
echo   3. 좌석 예약 현황
echo   4. 사용자별 통계
echo.

pause
