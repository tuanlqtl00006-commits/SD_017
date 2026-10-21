// Logic tính số liệu cho trang Thống kê (thuần hàm, không đụng tới giao diện nên test được).
// Dữ liệu vào là danh sách hóa đơn từ GET /api/hoa-don: { maHoaDon, tenKhachHang, tenNhanVien, loaiDon, tongTien, ngayTao, trangThai }.
// Doanh thu chỉ tính đơn đã hoàn thành (trangThai = 6); trangThai = 0 là đơn đã hủy.

import { toIso } from './format'

export const TT_HOAN_THANH = 6
export const TT_HUY = 0

/** Các mốc nhanh. v = số ngày gần nhất; 0 = tất cả; -1 = tự chọn từ ngày - đến ngày. */
export const KHOANG_NHANH = [
  { v: 7, t: '7 ngày' },
  { v: 30, t: '30 ngày' },
  { v: 90, t: '90 ngày' },
  { v: 0, t: 'Tất cả' },
  { v: -1, t: 'Tự chọn' },
]

/* ----- Ngày tháng (theo giờ máy, định dạng yyyy-mm-dd) ----- */

/** 'yyyy-mm-dd' -> Date lúc 12:00 giờ máy (tránh lệch ngày khi đổi giờ). */
export function isoSangNgay(iso) {
  const [y, m, d] = String(iso).slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d, 12)
}

export function congNgay(iso, n) {
  const d = isoSangNgay(iso)
  d.setDate(d.getDate() + n)
  return toIso(d)
}

/** Số ngày tính cả hai đầu: (2026-10-01, 2026-10-07) -> 7. */
export function soNgay(tu, den) {
  return Math.round((isoSangNgay(den) - isoSangNgay(tu)) / 86400000) + 1
}

export const ngayCua = (h) => String(h?.ngayTao ?? '').slice(0, 10)
const tien = (h) => Number(h?.tongTien || 0)

/**
 * Khoảng ngày cần xem -> { tu, den } ('yyyy-mm-dd'; '' nghĩa là không giới hạn phía đó).
 * khoang > 0: N ngày gần nhất tính cả hôm nay. 0: tất cả. -1: tự chọn (nếu nhập ngược thì tự đảo lại).
 */
export function tinhKhoang(khoang, tuChon, denChon, homNay) {
  if (khoang > 0) return { tu: congNgay(homNay, -(khoang - 1)), den: homNay }
  if (khoang === -1) {
    let tu = tuChon || ''
    let den = denChon || ''
    if (tu && den && tu > den) [tu, den] = [den, tu]
    return { tu, den }
  }
  return { tu: '', den: '' }
}

export function locTheoKhoang(hoaDon, tu, den) {
  return (hoaDon ?? []).filter((h) => {
    const n = ngayCua(h)
    if (!n) return false
    return (!tu || n >= tu) && (!den || n <= den)
  })
}

/** Kỳ liền trước có cùng độ dài (để so sánh). Không xác định được (thiếu tu hoặc den) thì trả null. */
export function kyTruoc(tu, den) {
  if (!tu || !den) return null
  const n = soNgay(tu, den)
  return { tu: congNgay(tu, -n), den: congNgay(tu, -1) }
}

/* ----- Tổng hợp ----- */

export function tongHop(ds) {
  const list = ds ?? []
  const xong = list.filter((h) => h.trangThai === TT_HOAN_THANH)
  const huy = list.filter((h) => h.trangThai === TT_HUY)
  const doanhThu = xong.reduce((s, h) => s + tien(h), 0)
  return {
    tongDon: list.length,
    hoanThanh: xong.length,
    huy: huy.length,
    doanhThu,
    trungBinh: xong.length ? Math.round(doanhThu / xong.length) : 0,
    tyLeHoanThanh: list.length ? (xong.length / list.length) * 100 : 0,
    tyLeHuy: list.length ? (huy.length / list.length) * 100 : 0,
  }
}

/** % thay đổi so với kỳ trước. Kỳ trước = 0 mà kỳ này > 0 thì không tính được (null); cả hai = 0 thì 0. */
export function phanTramThayDoi(nay, truoc) {
  if (!truoc) return nay ? null : 0
  return ((nay - truoc) / truoc) * 100
}

/* ----- Biểu đồ cột doanh thu ----- */

/**
 * Gom doanh thu (đơn hoàn thành) thành các cột: <= 31 ngày thì theo NGÀY, <= 182 ngày theo TUẦN (7 ngày), dài hơn theo THÁNG.
 * tu / den rỗng (xem "Tất cả") thì lấy từ ngày đầu tiên có đơn tới hôm nay.
 * Trả về { donVi: 'ngày' | 'tuần' | 'tháng', cot: [{ k, nhan, v, so }] }.
 */
export function gomCot(hoaDon, tu, den, homNay) {
  const xong = (hoaDon ?? []).filter((h) => h.trangThai === TT_HOAN_THANH && ngayCua(h))
  const ngay = xong.map(ngayCua).sort()
  const dau = tu || ngay[0] || homNay
  const cuoi = den || homNay
  if (dau > cuoi) return { donVi: 'ngày', cot: [] }
  const n = soNgay(dau, cuoi)
  const donVi = n <= 31 ? 'ngày' : n <= 182 ? 'tuần' : 'tháng'

  const cot = []
  if (donVi === 'ngày' || donVi === 'tuần') {
    const buoc = donVi === 'ngày' ? 1 : 7
    for (let s = dau; s <= cuoi; s = congNgay(s, buoc)) {
      const e = congNgay(s, buoc - 1)
      cot.push({ k: s, tu: s, den: e > cuoi ? cuoi : e, nhan: `${s.slice(8)}/${s.slice(5, 7)}`, v: 0, so: 0 })
    }
  } else {
    let y = Number(dau.slice(0, 4))
    let m = Number(dau.slice(5, 7))
    const yc = Number(cuoi.slice(0, 4))
    const mc = Number(cuoi.slice(5, 7))
    while (y < yc || (y === yc && m <= mc)) {
      const mm = String(m).padStart(2, '0')
      cot.push({ k: `${y}-${mm}`, tu: `${y}-${mm}-01`, den: `${y}-${mm}-31`, nhan: `${mm}/${y}`, v: 0, so: 0 })
      if (++m > 12) { m = 1; y++ }
    }
  }
  for (const h of xong) {
    const n2 = ngayCua(h)
    if (n2 < dau || n2 > cuoi) continue
    const c = cot.find((x) => n2 >= x.tu && n2 <= x.den)
    if (c) { c.v += tien(h); c.so++ }
  }
  return { donVi, cot }
}

/* ----- Xếp hạng ----- */

/**
 * Top theo doanh thu (đơn hoàn thành), nhóm theo khóa. layTen(h) trả về tên nhóm; rỗng thì bỏ qua.
 * Trả về [{ ten, so, v }] giảm dần theo doanh thu, tối đa gioiHan dòng.
 */
export function xepHang(hoaDon, layTen, gioiHan = 5) {
  const m = new Map()
  for (const h of hoaDon ?? []) {
    if (h.trangThai !== TT_HOAN_THANH) continue
    const ten = String(layTen(h) ?? '').trim()
    if (!ten) continue
    const r = m.get(ten) ?? { ten, so: 0, v: 0 }
    r.so++
    r.v += tien(h)
    m.set(ten, r)
  }
  return [...m.values()].sort((a, b) => b.v - a.v || b.so - a.so || a.ten.localeCompare(b.ten, 'vi')).slice(0, gioiHan)
}

/** Doanh thu theo loại đơn (đơn hoàn thành), kèm tỷ trọng %. */
export function theoLoaiDon(hoaDon) {
  const m = {}
  for (const h of hoaDon ?? []) {
    if (h.trangThai !== TT_HOAN_THANH) continue
    const k = h.loaiDon || 'Khác'
    m[k] = (m[k] || 0) + tien(h)
  }
  const tong = Object.values(m).reduce((a, b) => a + b, 0)
  return Object.entries(m).map(([ten, v]) => ({ ten, v, pct: tong ? (v / tong) * 100 : 0 })).sort((a, b) => b.v - a.v)
}

/** Các đơn mới nhất (theo ngày tạo giảm dần, rồi id giảm dần). */
export function donGanNhat(hoaDon, gioiHan = 8) {
  return [...(hoaDon ?? [])]
    .sort((a, b) => String(b.ngayTao).localeCompare(String(a.ngayTao)) || Number(b.id || 0) - Number(a.id || 0))
    .slice(0, gioiHan)
}

/** Làm tròn trục tung "đẹp": 1,2,5 x 10^n. 0 -> 1. */
export function lamTronTran(max) {
  if (!(max > 0)) return 1
  const mu = 10 ** Math.floor(Math.log10(max))
  const f = max / mu
  const b = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10
  return b * mu
}

/** Nhãn tiền gọn cho trục: 2500000 -> '2,5tr', 1200000000 -> '1,2 tỷ', 500000 -> '500k'. */
export function nhanTienGon(v) {
  const so = (x) => String(Math.round(x * 10) / 10).replace('.', ',')
  if (!v) return '0'
  if (v >= 1e9) return `${so(v / 1e9)} tỷ`
  if (v >= 1e6) return `${so(v / 1e6)}tr`
  if (v >= 1e3) return `${so(v / 1e3)}k`
  return String(v)
}

/* ---------- Giai đoạn 3: dữ liệu từ GET /api/thong-ke ---------- */

/** Khoảng ngày gửi lên máy chủ: "Tất cả" (không giới hạn) đổi thành 1 năm gần nhất vì máy chủ chỉ cho xem tối đa 1 năm. */
export function khoangGuiMayChu(tu, den, homNay) {
  const d = den || homNay
  let t = tu || congNgay(d, -365)
  if (soNgay(t, d) > 367) t = congNgay(d, -365)
  return { tuNgay: t, denNgay: d, catBot: !tu || t !== tu }
}

/** Phần trăm của một phần trong tổng (0 khi tổng = 0), làm tròn 1 chữ số. */
export function phanTram(v, tong) {
  const t = Number(tong) || 0
  return t > 0 ? Math.round(((Number(v) || 0) / t) * 1000) / 10 : 0
}

/** 570 phút -> '9,5 giờ'; 45 -> '45 phút'; 0 -> '0 giờ'. */
export function gioLam(phut) {
  const p = Number(phut) || 0
  if (p > 0 && p < 60) return `${p} phút`
  const g = Math.round((p / 60) * 10) / 10
  return `${String(g).replace('.', ',')} giờ`
}

/** Tỷ lệ đi làm đúng giờ trên các ca đã làm (null nếu chưa làm ca nào). */
export function tyLeDungGio(r) {
  const lam = Number(r?.soCaDaLam) || 0
  return lam ? phanTram(lam - (Number(r?.soLanDiMuon) || 0), lam) : null
}

/** Tỷ lệ có mặt theo lịch: đã làm / (đã làm + vắng); chưa có ca nào đã qua thì null. */
export function tyLeCoMat(r) {
  const lam = Number(r?.soCaDaLam) || 0
  const vang = Number(r?.soCaVang) || 0
  return lam + vang ? phanTram(lam, lam + vang) : null
}

/** Cắt 24 khung giờ còn đoạn có đơn (luôn hiện ít nhất 8h - 21h) để biểu đồ giờ không bị trống hai đầu. */
export function khungGioHienThi(theoGio = []) {
  const coDon = theoGio.filter((g) => Number(g.soDon) > 0).map((g) => g.gio)
  const dau = Math.min(8, ...coDon)
  const cuoi = Math.max(21, ...coDon)
  return theoGio.filter((g) => g.gio >= dau && g.gio <= cuoi)
}
