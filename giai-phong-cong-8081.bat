@echo off
chcp 65001 >nul
echo Dang tim chuong trinh dang chiem cong 8081 (backend cu chua tat)...
set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081 ^| findstr LISTENING') do (
  set FOUND=1
  echo Tat tien trinh PID %%a
  taskkill /PID %%a /F
)
if "%FOUND%"=="0" echo Khong co chuong trinh nao dang dung cong 8081. Ban co the chay BackendApplication binh thuong.
echo.
echo Xong. Bay gio hay chay lai BackendApplication trong IntelliJ.
pause
