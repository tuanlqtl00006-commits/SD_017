// Tài khoản nhận chuyển khoản tại quầy (để hiện mã QR VietQR đúng số tiền).
// Cấu hình trong file frontend/.env (tạo mới nếu chưa có) rồi chạy lại npm run dev:
//   VITE_NGAN_HANG_BIN=970422            mã BIN ngân hàng (vd MB Bank 970422, Vietcombank 970436, Techcombank 970407)
//   VITE_NGAN_HANG_STK=0123456789        số tài khoản nhận tiền
//   VITE_NGAN_HANG_CHU_TK=CONG TY FOOTSTYLE   tên chủ tài khoản (không dấu)
//   VITE_NGAN_HANG_TEN=MB Bank           tên ngân hàng hiển thị
// Chưa cấu hình thì KHÔNG hiện mã QR (tránh khách chuyển nhầm vào tài khoản mẫu), nhân viên vẫn nhập mã giao dịch như bình thường.
const env = import.meta.env ?? {}

export const NGAN_HANG = {
  bin: String(env.VITE_NGAN_HANG_BIN ?? '').trim(),
  soTaiKhoan: String(env.VITE_NGAN_HANG_STK ?? '').trim(),
  chuTaiKhoan: String(env.VITE_NGAN_HANG_CHU_TK ?? '').trim(),
  tenNganHang: String(env.VITE_NGAN_HANG_TEN ?? '').trim(),
}

export const daCauHinhNganHang = (nh = NGAN_HANG) => /^\d{6}$/.test(nh.bin) && /^[0-9A-Za-z]{4,19}$/.test(nh.soTaiKhoan)

export const PHUONG_THUC = [
  { ma: 'TIEN_MAT', ten: 'Tiền mặt', icon: 'bi-cash-coin' },
  { ma: 'CHUYEN_KHOAN', ten: 'Chuyển khoản', icon: 'bi-qr-code' },
  { ma: 'KET_HOP', ten: 'Kết hợp', icon: 'bi-wallet2' },
]
