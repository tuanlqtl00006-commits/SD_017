@echo off
chcp 65001 >nul
setlocal
REM ===================================================================
REM  Chay ca project FootStyle bang 1 lenh (Windows):
REM    .\chay-du-an.bat      (go trong terminal o thu muc goc project, hoac bam dup file nay)
REM  - Backend chay o CUA SO RIENG "FootStyle - Backend" (dong cua so do = tat backend).
REM  - Cho backend san sang roi moi chay frontend (npm install + npm run dev) ngay trong terminal nay.
REM  - Bam Ctrl+C o day chi tat frontend, backend van chay.
REM  - Backend dang chay san thi dung luon, khong mo them ban thu 2.
REM  - Truoc khi chay se kiem tra Node.js / Java / SQL Server (scripts\kiem-tra-moi-truong.cjs) va noi ro neu thieu hoac qua cu.
REM    Muon bo qua buoc nay: set FOOTSTYLE_BO_QUA_KIEM_TRA=1 truoc khi chay.
REM ===================================================================
set "GOC=%~dp0"
set "CONG=8081"
if defined SERVER_PORT set "CONG=%SERVER_PORT%"
set "API=http://localhost:%CONG%/api/phieu-giam-gia/trang-thai-mail"

where npm >nul 2>&1
if errorlevel 1 (
  echo [FootStyle] Chua cai Node.js ^(khong tim thay lenh npm^). Cai Node.js LTS tai https://nodejs.org roi chay lai.
  pause
  exit /b 1
)

call :kiemTraBackend 1
if not errorlevel 1 (
  echo [FootStyle] Backend da chay san o cong %CONG%, dung luon.
  REM Chi con frontend can chay nen chi kiem tra Node.js, phai tu 20.19 hoac 22.12 tro len theo yeu cau cua Vite 8.
  node "%GOC%scripts\kiem-tra-moi-truong.cjs" --chi-node
  if errorlevel 1 (
    pause
    exit /b 1
  )
  goto frontend
)

REM Backend chua chay: kiem tra Node.js, JDK 17 tro len va SQL Server truoc khi mo backend. Loi thi dung lai va noi cach sua.
node "%GOC%scripts\kiem-tra-moi-truong.cjs"
if errorlevel 1 (
  pause
  exit /b 1
)

echo [FootStyle] Mo backend o cua so rieng "FootStyle - Backend" ^(cong %CONG%^)...
start "FootStyle - Backend" /D "%GOC%backend" cmd /k "chcp 65001 >nul & mvnw.cmd spring-boot:run"
echo [FootStyle] Dang cho backend khoi dong ^(lan dau co the mat vai phut de tai thu vien^)...
call :kiemTraBackend 300
if errorlevel 1 (
  echo [FootStyle] Sau 5 phut backend van chua chay. Xem loi trong cua so "FootStyle - Backend"
  echo [FootStyle] ^(thuong gap: chua bat SQL Server, sai mat khau trong backend\src\main\resources\application.properties^).
  echo [FootStyle] Van chay frontend, giao dien se bao loi tai du lieu cho toi khi backend chay.
) else (
  echo [FootStyle] Backend da san sang.
)

:frontend
cd /d "%GOC%frontend"
echo [FootStyle] npm install...
call npm install
if errorlevel 1 (
  echo [FootStyle] npm install loi, xem thong bao phia tren.
  pause
  exit /b 1
)
echo [FootStyle] npm run dev ^(mo http://localhost:5173 tren trinh duyet^)...
call npm run dev
exit /b 0

REM ---- Cho backend tra loi API, toi da %1 giay. errorlevel 0 = da chay ----
:kiemTraBackend
powershell -NoProfile -Command "$u='%API%'; for($i=0; $i -lt %1; $i++){ try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri $u | Out-Null; exit 0 } catch { Start-Sleep -Seconds 1 } }; exit 1"
exit /b %errorlevel%
