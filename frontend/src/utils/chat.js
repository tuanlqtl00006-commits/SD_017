// Hàm dùng chung cho chat hỗ trợ khách + chat nội bộ (không phụ thuộc giao diện, có test riêng).

/** Ai gửi tin (khớp TinNhan.java). */
export const NGUOI_GUI = Object.freeze({ KHACH: 1, NHAN_VIEN: 2, TRO_LY_AO: 3, HE_THONG: 4 })

/** Trạng thái cuộc chat hỗ trợ khách (khớp HoiThoai.java). */
export const CHE_DO = Object.freeze({ TRO_LY_AO: 1, CHO_NHAN_VIEN: 2, NHAN_VIEN_HO_TRO: 3, DA_DONG: 4 })

const TEN_CHE_DO = {
  [CHE_DO.TRO_LY_AO]: { nhan: 'Trợ lý ảo', pill: 'ad-pill-purple', icon: 'bi-robot' },
  [CHE_DO.CHO_NHAN_VIEN]: { nhan: 'Chờ nhân viên', pill: 'ad-pill-orange', icon: 'bi-hourglass-split' },
  [CHE_DO.NHAN_VIEN_HO_TRO]: { nhan: 'Đang hỗ trợ', pill: 'ad-pill-blue', icon: 'bi-headset' },
  [CHE_DO.DA_DONG]: { nhan: 'Đã kết thúc', pill: 'ad-pill-gray', icon: 'bi-check2-circle' },
}

export function moTaCheDo(cheDo) {
  return TEN_CHE_DO[cheDo] ?? TEN_CHE_DO[CHE_DO.TRO_LY_AO]
}

/** Độ dài tối đa một tin (khớp TinNhan.DO_DAI_TOI_DA). */
export const DO_DAI_TOI_DA = 1000

/** Gộp tin mới vào danh sách đang có: bỏ trùng theo id, xếp theo id tăng dần. */
export function gopTin(cu = [], moi = []) {
  if (!moi?.length) return cu
  const theoId = new Map()
  for (const t of cu) theoId.set(t.id, t)
  for (const t of moi) theoId.set(t.id, t)
  return [...theoId.values()].sort((a, b) => a.id - b.id)
}

/** Id tin lớn nhất (mốc "sauId" cho lần hỏi lại), chưa có tin -> 0. */
export function idCuoi(ds = []) {
  return ds.reduce((m, t) => (t.id > m ? t.id : m), 0)
}

const pad = (n) => String(n).padStart(2, '0')

function docNgay(v) {
  if (!v) return null
  const d = v instanceof Date ? v : new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

function cungNgay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

/** Giờ ngắn gọn cho danh sách hội thoại: hôm nay '09:05', hôm qua 'Hôm qua', trong năm '16/10', năm khác '16/10/2025'. */
export function gioNgan(v, bayGio = new Date()) {
  const d = docNgay(v)
  if (!d) return ''
  if (cungNgay(d, bayGio)) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const homQua = new Date(bayGio)
  homQua.setDate(homQua.getDate() - 1)
  if (cungNgay(d, homQua)) return 'Hôm qua'
  return d.getFullYear() === bayGio.getFullYear() ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}` : `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

/** Nhãn ngày chia đoạn trong khung chat: 'Hôm nay', 'Hôm qua', 'Thứ 6, 16/10/2026'. */
export function nhanNgay(v, bayGio = new Date()) {
  const d = docNgay(v)
  if (!d) return ''
  if (cungNgay(d, bayGio)) return 'Hôm nay'
  const homQua = new Date(bayGio)
  homQua.setDate(homQua.getDate() - 1)
  if (cungNgay(d, homQua)) return 'Hôm qua'
  const thu = d.getDay() === 0 ? 'Chủ nhật' : `Thứ ${d.getDay() + 1}`
  return `${thu}, ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

export function gioPhut(v) {
  const d = docNgay(v)
  return d ? `${pad(d.getHours())}:${pad(d.getMinutes())}` : ''
}

/**
 * Chuẩn bị danh sách để vẽ: chèn dòng ngày khi sang ngày mới; đánh dấu `noiTiep` khi tin liền trước cùng người gửi
 * trong vòng 5 phút (giao diện không lặp lại tên / ảnh đại diện). laCuaToi(t) quyết định tin nằm bên phải.
 */
export function chuanBiHienThi(ds = [], laCuaToi = () => false, bayGio = new Date()) {
  const kq = []
  let ngayTruoc = ''
  let truoc = null
  for (const t of ds) {
    const d = docNgay(t.thoiGian)
    const nhan = d ? nhanNgay(d, bayGio) : ''
    if (nhan && nhan !== ngayTruoc) {
      kq.push({ loai: 'ngay', khoa: `ngay-${t.id}`, nhan })
      ngayTruoc = nhan
      truoc = null
    }
    if (t.loaiNguoiGui === NGUOI_GUI.HE_THONG) {
      kq.push({ loai: 'he-thong', khoa: `t-${t.id}`, tin: t })
      truoc = null
      continue
    }
    const cungNguoi = truoc && truoc.loaiNguoiGui === t.loaiNguoiGui && (truoc.idNhanVien ?? null) === (t.idNhanVien ?? null)
      && truoc.tenNguoiGui === t.tenNguoiGui
    const ganNhau = truoc && d && docNgay(truoc.thoiGian) && d - docNgay(truoc.thoiGian) <= 5 * 60 * 1000
    kq.push({ loai: 'tin', khoa: `t-${t.id}`, tin: t, cuaToi: !!laCuaToi(t), noiTiep: !!(cungNguoi && ganNhau) })
    truoc = t
  }
  return kq
}

/** Lọc danh sách hội thoại theo từ khóa (tên, SĐT, tiêu đề, tin cuối), không phân biệt dấu. */
export function locHoiThoai(ds = [], tuKhoa = '', boDau = (s) => s) {
  const k = boDau(String(tuKhoa ?? '').trim())
  if (!k) return ds
  return ds.filter((h) => boDau([h.tenKhach, h.sdtKhach, h.tieuDe, h.tinCuoi?.noiDung].filter(Boolean).join(' ')).includes(k))
}

/** Xem trước tin cuối trong danh sách: "Bạn: ...", "Trợ lý: ...", "Lan: ..." */
export function xemTruocTinCuoi(t, idToi = null, nhom = false) {
  if (!t) return 'Chưa có tin nhắn'
  const nd = t.noiDung ?? ''
  if (t.loaiNguoiGui === NGUOI_GUI.HE_THONG) return nd
  if (t.loaiNguoiGui === NGUOI_GUI.NHAN_VIEN && idToi !== null && t.idNhanVien === idToi) return `Bạn: ${nd}`
  if (t.loaiNguoiGui === NGUOI_GUI.TRO_LY_AO) return `Trợ lý: ${nd}`
  if (nhom && t.tenNguoiGui) return `${t.tenNguoiGui.split(' ').slice(-1)[0]}: ${nd}`
  return nd
}

/* ---------- phiên chat của khách (lưu trong trình duyệt để tải lại trang vẫn tiếp tục) ---------- */

const KHOA_PHIEN_KHACH = 'fs_ho_tro_khach'

export function docPhienKhach() {
  try {
    const p = JSON.parse(localStorage.getItem(KHOA_PHIEN_KHACH) || 'null')
    return p && Number.isInteger(p.id) && typeof p.ma === 'string' && p.ma ? p : null
  } catch {
    return null
  }
}

export function luuPhienKhach(p) {
  try {
    localStorage.setItem(KHOA_PHIEN_KHACH, JSON.stringify({ id: p.id, ma: p.ma, hoTen: p.hoTen ?? '', sdt: p.sdt ?? '' }))
  } catch {
    // trình duyệt chặn lưu trữ: vẫn chat được, chỉ là tải lại trang sẽ mất cuộc chat
  }
}

export function xoaPhienKhach() {
  try {
    const cu = docPhienKhach()
    localStorage.removeItem(KHOA_PHIEN_KHACH)
    // giữ lại tên + SĐT để lần sau khỏi gõ lại
    if (cu) localStorage.setItem(`${KHOA_PHIEN_KHACH}_nguoi`, JSON.stringify({ hoTen: cu.hoTen, sdt: cu.sdt }))
  } catch {
    // bỏ qua
  }
}

export function docNguoiKhachCu() {
  try {
    const p = JSON.parse(localStorage.getItem(`${KHOA_PHIEN_KHACH}_nguoi`) || 'null')
    return { hoTen: p?.hoTen ?? '', sdt: p?.sdt ?? '' }
  } catch {
    return { hoTen: '', sdt: '' }
  }
}

/** Trả lời nhanh của nhân viên hỗ trợ. */
export const TRA_LOI_NHANH = [
  'Dạ em chào anh/chị, em có thể hỗ trợ gì cho mình ạ?',
  'Anh/chị cho em xin mã đơn hàng để em kiểm tra giúp mình nhé.',
  'Dạ sản phẩm này bên em còn hàng ạ, anh/chị muốn đặt màu và trọng lượng nào ạ?',
  'Anh/chị đợi em một chút để em kiểm tra kho nhé.',
  'Cảm ơn anh/chị đã liên hệ FootStyle, chúc anh/chị một ngày tốt lành!',
]
