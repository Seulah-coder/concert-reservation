@echo off
REM PostgreSQL + Redis 환경 시작 스크립트
REM 작성일: 2026-02-05

echo ======================================
echo Concert Reservation System
echo PostgreSQL + Redis 환경 시작
echo ======================================
echo.

echo [1/3] Docker 컨테이너 시작 중...
docker-compose up -d

echo.
echo [2/3] 컨테이너 상태 확인 중...
timeout /t 5 /nobreak > nul
docker-compose ps

echo.
echo [3/3] PostgreSQL 연결 테스트...
docker exec concert-reservation-postgres pg_isready -U concert_user -d concert_reservation

echo.
echo ======================================
echo 환경 구축 완료!
echo ======================================
echo.
echo [PostgreSQL 정보]
echo   Host:     localhost
echo   Port:     5432
echo   Database: concert_reservation
echo   Username: concert_user
echo   Password: concert_pass
echo.
echo [Redis 정보]
echo   Host:     localhost
echo   Port:     6379
echo.
echo [다음 단계]
echo   1. IntelliJ에서 Run Configuration 생성
echo   2. Active profiles: postgres
echo   3. 애플리케이션 실행
echo.
echo 또는 명령어로 실행:
echo   gradlew bootRun --args='--spring.profiles.active=postgres'
echo.
echo 상세 가이드: report\POSTGRESQL_SETUP_GUIDE.md
echo ======================================
pause
