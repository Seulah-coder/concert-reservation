@echo off
REM PostgreSQL 환경에서 애플리케이션 실행 (포트 충돌 자동 해결)
REM 작성일: 2026-02-05

echo ======================================
echo Concert Reservation System
echo PostgreSQL 환경 실행
echo ======================================
echo.

echo [1/4] 포트 8080 확인 중...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo 기존 프로세스 발견 (PID: %%a)
    echo 종료 중...
    taskkill /F /PID %%a >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo 종료 완료!
    goto :continue
)
echo 포트 8080 사용 가능

:continue
echo.
echo [2/4] Docker 컨테이너 상태 확인...
docker-compose ps | findstr "postgres.*healthy" >nul
if errorlevel 1 (
    echo PostgreSQL 컨테이너가 실행 중이 아닙니다.
    echo Docker 컨테이너를 시작합니다...
    docker-compose up -d
    timeout /t 5 /nobreak >nul
) else (
    echo PostgreSQL 정상 실행 중
)

echo.
echo [3/4] Redis 상태 확인...
docker-compose ps | findstr "redis.*healthy" >nul
if errorlevel 1 (
    echo Redis 컨테이너가 실행 중이 아닙니다.
    docker-compose restart redis
    timeout /t 3 /nobreak >nul
) else (
    echo Redis 정상 실행 중
)

echo.
echo [4/4] 애플리케이션 시작...
echo.
echo ======================================
echo Profile: postgres
echo Database: PostgreSQL (localhost:5432)
echo Redis: localhost:6379
echo API: http://localhost:8080
echo Swagger: http://localhost:8080/swagger-ui.html
echo ======================================
echo.

gradlew bootRun --args="--spring.profiles.active=postgres"
