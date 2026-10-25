import { computed, reactive } from 'vue'
import api from '../services/api'
import { layPhien, luuPhien, xoaPhien } from '../services/phien'
import { datLaiGiaoCa } from './useGiaoCa'

// Trạng thái đăng nhập dùng chung toàn trang (một bản duy nhất, giống useToast).
//  - nguoiDung: { id, ma, hoTen, email, vaiTro: 'QUAN_LY' | 'NHAN_VIEN', tenVaiTro } hoặc null
//  - batDangNhap: backend có bật đăng nhập không (app.auth.bat); false thì dùng luôn quyền quản lý, không cần đăng nhập
const trangThai = reactive({
  nguoiDung: layPhien()?.nguoiDung ?? null,
  batDangNhap: null,
  daKhoiTao: false,
})
let dangKhoiTao = null

const daDangNhap = computed(() => !!trangThai.nguoiDung)
const laQuanLy = computed(() => trangThai.nguoiDung?.vaiTro === 'QUAN_LY')
const laKhachHang = computed(() => trangThai.nguoiDung?.vaiTro === 'KHACH_HANG')
// Vai trò theo cách gọi của menu / router: 'quan-ly' | 'nhan-vien' | 'khach-hang'
// (khách hàng đăng nhập ở trang bán online không được vào trang quản trị)
const vaiTro = computed(() => (laQuanLy.value ? 'quan-ly' : laKhachHang.value ? 'khach-hang' : 'nhan-vien'))

/** Hỏi backend có bật đăng nhập không + xác nhận lại phiên đang lưu (tài khoản bị ngừng / đổi mật khẩu ở máy khác thì phải đăng nhập lại). */
async function khoiTao() {
  if (trangThai.daKhoiTao) return
  if (!dangKhoiTao) {
    dangKhoiTao = (async () => {
      try {
        const cauHinh = await api.get('/auth/cau-hinh')
        trangThai.batDangNhap = cauHinh.data?.batDangNhap !== false
        if (!trangThai.batDangNhap || layPhien()) {
          const toi = await api.get('/auth/toi')
          trangThai.nguoiDung = toi.data
          if (trangThai.batDangNhap) luuPhien({ ...layPhien(), nguoiDung: toi.data })
        }
      } catch (e) {
        // 401: phiên cũ không còn dùng được. Mất kết nối: giữ phiên đang lưu để giao diện vẫn mở được (API sẽ tự báo lỗi).
        if (e?.status === 401) {
          xoaPhien()
          trangThai.nguoiDung = null
        }
        if (trangThai.batDangNhap === null) trangThai.batDangNhap = true
      } finally {
        trangThai.daKhoiTao = true
        dangKhoiTao = null
      }
    })()
  }
  await dangKhoiTao
}

/** Đăng nhập; lỗi (sai mật khẩu, bị khóa tạm...) ném lên cho form hiển thị. */
async function dangNhap({ taiKhoan, matKhau, nho = true }) {
  const res = await api.post('/auth/dang-nhap', { taiKhoan, matKhau })
  ghiPhien(res.data, nho)
  return res.data.nguoiDung
}

/** Đăng nhập khách hàng (bán hàng online). */
async function dangNhapKhachHang({ taiKhoan, matKhau, nho = true }) {
  const res = await api.post('/auth/khach-hang/dang-nhap', { taiKhoan, matKhau })
  ghiPhien(res.data, nho)
  return res.data.nguoiDung
}

/** Lưu phiên mới (sau đăng nhập hoặc đổi mật khẩu - backend cấp token mới). */
function ghiPhien(kq, nho = layPhien()?.nho ?? true) {
  if (trangThai.nguoiDung?.id !== kq.nguoiDung?.id || trangThai.nguoiDung?.vaiTro !== kq.nguoiDung?.vaiTro) datLaiGiaoCa() // đổi tài khoản: tải lại ca của người mới
  luuPhien({ token: kq.token, hetHan: kq.hetHan, nguoiDung: kq.nguoiDung, nho })
  trangThai.nguoiDung = kq.nguoiDung
  trangThai.batDangNhap = true
  trangThai.daKhoiTao = true
}

/** Đăng xuất (token chỉ lưu ở trình duyệt nên xóa là xong). */
function dangXuat() {
  xoaPhien()
  trangThai.nguoiDung = null
  datLaiGiaoCa()
}

/** Trang mở đầu sau khi đăng nhập theo vai trò. */
function trangChu() {
  if (trangThai.nguoiDung?.vaiTro === 'KHACH_HANG') return '/shop'
  return laQuanLy.value ? '/thong-ke' : '/ban-hang'
}

/** Tên viết tắt cho ô avatar (Nguyễn Hoàng Long -> NL). */
const vietTat = computed(() => {
  const ten = (trangThai.nguoiDung?.hoTen || '').trim().split(/\s+/).filter(Boolean)
  if (!ten.length) return 'AD'
  return ((ten[0][0] || '') + (ten.length > 1 ? ten[ten.length - 1][0] : '')).toUpperCase()
})

export function useAuth() {
  return {
    trangThai,
    daDangNhap,
    laQuanLy,
    laKhachHang,
    vaiTro,
    vietTat,
    khoiTao,
    dangNhap,
    dangNhapKhachHang,
    ghiPhien,
    dangXuat,
    trangChu,
  }
}

/** Chỉ dùng trong test: đưa về trạng thái ban đầu. */
export function _datLaiAuth() {
  trangThai.nguoiDung = layPhien()?.nguoiDung ?? null
  trangThai.batDangNhap = null
  trangThai.daKhoiTao = false
  dangKhoiTao = null
}
