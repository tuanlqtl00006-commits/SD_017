#!/usr/bin/env bash
# ===================================================================
#  Chay ca project FootStyle bang 1 lenh (macOS / Linux), tuong duong chay-du-an.bat cua Windows:
#    ./chay-du-an.sh        (neu bao "Permission denied" thi chay:  bash chay-du-an.sh)
#  - Backend chay nen, ghi log vao backend/backend.log, khong tat khi bam Ctrl+C.
#  - Cho backend san sang roi moi chay frontend (npm install + npm run dev) ngay trong terminal nay.
#  - Backend dang chay san thi dung luon, khong mo them ban thu 2.
#  - Truoc khi chay se kiem tra Node.js / Java / SQL Server (scripts/kiem-tra-moi-truong.cjs).
#    Muon bo qua buoc nay: FOOTSTYLE_BO_QUA_KIEM_TRA=1 ./chay-du-an.sh
#  Thong bao viet khong dau cho giong ban Windows.
# ===================================================================
GOC="$(cd "$(dirname "$0")" && pwd)"
CONG="${SERVER_PORT:-8081}"
API="http://localhost:${CONG}/api/phieu-giam-gia/trang-thai-mail"
LOG="$GOC/backend/backend.log"
KIEM_TRA="$GOC/scripts/kiem-tra-moi-truong.cjs"

if ! command -v npm >/dev/null 2>&1 || ! command -v node >/dev/null 2>&1; then
  echo "[FootStyle] Chua cai Node.js (khong tim thay lenh node / npm). Cai Node.js LTS tai https://nodejs.org roi chay lai."
  exit 1
fi

# Cho backend tra loi API, toi da $1 giay; $2 (neu co) la PID backend vua mo, chet som thi khoi cho nua. 0 = da chay
kiem_tra_backend() {
  local i=0
  while [ "$i" -lt "$1" ]; do
    if node -e 'fetch(process.argv[1],{signal:AbortSignal.timeout(2000)}).then(r=>process.exit(r.ok?0:1),()=>process.exit(1))' "$API" >/dev/null 2>&1; then
      return 0
    fi
    if [ -n "${2:-}" ] && ! kill -0 "$2" 2>/dev/null; then
      return 2
    fi
    sleep 1
    i=$((i + 1))
  done
  return 1
}

chay_frontend() {
  cd "$GOC/frontend" || exit 1
  echo "[FootStyle] npm install..."
  if ! npm install; then
    echo "[FootStyle] npm install loi, xem thong bao phia tren."
    exit 1
  fi
  echo "[FootStyle] npm run dev (mo http://localhost:5173 tren trinh duyet)..."
  exec npm run dev
}

if kiem_tra_backend 1; then
  echo "[FootStyle] Backend da chay san o cong ${CONG}, dung luon."
  # Chi con frontend can chay nen chi kiem tra Node.js.
  node "$KIEM_TRA" --chi-node || exit 1
  chay_frontend
fi

# Backend chua chay: kiem tra Node.js, JDK 17 tro len va SQL Server truoc khi mo backend. Loi thi dung lai va noi cach sua.
node "$KIEM_TRA" || exit 1

echo "[FootStyle] Mo backend chay nen (cong ${CONG}), log o backend/backend.log ..."
# "sh ./mvnw" de chay duoc ca khi file mvnw mat quyen thuc thi (giai nen bang cong cu khac).
( cd "$GOC/backend" && exec nohup sh ./mvnw spring-boot:run >"$LOG" 2>&1 ) &
PID_BACKEND=$!
echo "[FootStyle] Dang cho backend khoi dong (lan dau co the mat vai phut de tai thu vien)..."
kiem_tra_backend 300 "$PID_BACKEND"
KQ=$?
if [ "$KQ" -eq 0 ]; then
  echo "[FootStyle] Backend da san sang. Muon tat backend: kill ${PID_BACKEND}"
elif [ "$KQ" -eq 2 ]; then
  echo "[FootStyle] Backend tat ngay khi vua mo. 20 dong cuoi cua backend/backend.log:"
  tail -n 20 "$LOG" 2>/dev/null
  echo "[FootStyle] (thuong gap: chua bat SQL Server, sai mat khau trong backend/src/main/resources/application.properties)."
  echo "[FootStyle] Van chay frontend, giao dien se bao loi tai du lieu cho toi khi backend chay."
else
  echo "[FootStyle] Sau 5 phut backend van chua chay. Xem loi trong backend/backend.log (tien trinh dang chay nen: kill ${PID_BACKEND})."
  echo "[FootStyle] (thuong gap: chua bat SQL Server, sai mat khau trong backend/src/main/resources/application.properties)."
  echo "[FootStyle] Van chay frontend, giao dien se bao loi tai du lieu cho toi khi backend chay."
fi
chay_frontend
