@echo off
REM H2 테스트 환경에서 애플리케이션 실행 (포트 충돌 자동 해결)
REM 작성일: 2026-02-05

echo ======================================
echo Concert Reservation System
echo H2 테스트 환경 실행
echo ======================================
echo.

echo [1/3] 포트 8080 확인 중...
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
echo [2/3] Redis 상태 확인...
docker-compose ps | findstr "redis.*healthy" >nul
if errorlevel 1 (
    echo Redis 컨테이너가 실행 중이 아닙니다.
    echo Redis를 시작합니다...
    docker-compose up -d redis
    timeout /t 3 /nobreak >nul
) else (
    echo Redis 정상 실행 중
)

echo.
echo [3/3] 애플리케이션 시작 (H2 모드)...
echo.
echo ======================================
echo Profile: default (H2)
echo Database: H2 In-Memory
echo Redis: localhost:6379
echo API: http://localhost:8080
echo Swagger: http://localhost:8080/swagger-ui.html
echo H2 Console: http://localhost:8080/h2-console
echo ======================================
echo.

gradlew bootRun
