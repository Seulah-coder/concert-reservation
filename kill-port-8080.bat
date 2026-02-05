@echo off
REM 포트 8080 해제 스크립트
REM 작성일: 2026-02-05

echo ======================================
echo 포트 8080 사용 중인 프로세스 종료
echo ======================================
echo.

echo [1/2] 포트 8080 사용 중인 프로세스 확인...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    set PID=%%a
    goto :found
)

echo 포트 8080을 사용하는 프로세스가 없습니다.
goto :end

:found
echo 발견: PID %PID%

echo.
echo [2/2] 프로세스 종료 중...
taskkill /F /PID %PID%

echo.
echo ======================================
echo 완료! 이제 애플리케이션을 실행하세요.
echo ======================================

:end
pause
