// Tính giảm giá của biến thể khi nằm trong NHIỀU đợt giảm giá (đợt chồng nhau về thời gian).
//
// Ví dụ (đúng kịch bản kiểm thử):
//   Đợt A: 10%, 1/10 - 10/10, biến thể SPCT1, SPCT5
//   Đợt B: 15%, 4/10 - 20/10, biến thể SPCT1, SPCT3, SPCT4
//   => SPCT1: 1/10-3/10 giảm 10% | 4/10-10/10 CHỒNG NHAU: cao nhất 15% / trung bình cộng 12,5% | 11/10-20/10 giảm 15%
//      SPCT5: 1/10-10/10 giảm 10%        SPCT3, SPCT4: 4/10-20/10 giảm 15%
//
// Cách tính khi chồng nhau do backend quyết định (app.giam-gia.chinh-sach-chong-lap):
//   'MAX'        = lấy mức cao nhất
//   'TRUNG_BINH' = trung bình cộng các đợt đang chồng nhau
import { todayIso } from './format'

const MOT_NGAY = 86400000

// 'yyyy-mm-dd' <-> số ngày (tránh lỗi múi giờ / giờ mùa hè khi cộng trừ ngày)
const soNgay = (iso) => {
  const [y, m, d] = String(iso).slice(0, 10).split('-').map(Number)
  return Math.round(Date.UTC(y, m - 1, d) / MOT_NGAY)
}
const veNgay = (n) => new Date(n * MOT_NGAY).toISOString().slice(0, 10)

const lamTron = (x) => Math.round(x * 100) / 100

/** 12.5 -> '12,5%', 15 -> '15%' */
export function formatPhanTram(x) {
  if (x === null || x === undefined) return ''
  return `${String(lamTron(x)).replace('.', ',')}%`
}

/**
 * Gộp các đợt của MỘT biến thể thành các khoảng ngày liên tiếp, mỗi khoảng có một mức giảm.
 * @param {Array<{idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc}>} muc  các đợt chứa biến thể này
 * @param {'MAX'|'TRUNG_BINH'} chinhSach
 * @returns {Array<{tuNgay, denNgay, cacDot, caoNhat, trungBinh, apDung, chongNhau}>} theo thứ tự thời gian
 */
export function lichGiamGia(muc, chinhSach = 'MAX') {
  const ds = (muc || []).filter((m) => m.ngayBatDau && m.ngayKetThuc)
  if (!ds.length) return []
  const moc = new Set()
  for (const m of ds) {
    moc.add(soNgay(m.ngayBatDau))
    moc.add(soNgay(m.ngayKetThuc) + 1) // ngày kết thúc tính đến hết ngày
  }
  const mocs = [...moc].sort((a, b) => a - b)
  const ketQua = []
  for (let i = 0; i < mocs.length - 1; i++) {
    const tu = mocs[i]
    const den = mocs[i + 1] - 1
    const cacDot = ds.filter((m) => soNgay(m.ngayBatDau) <= tu && soNgay(m.ngayKetThuc) >= den)
    if (!cacDot.length) continue // khoảng trống: không đợt nào giảm
    const phanTrams = cacDot.map((m) => Number(m.phanTram) || 0)
    const caoNhat = Math.max(...phanTrams)
    const trungBinh = lamTron(phanTrams.reduce((a, b) => a + b, 0) / phanTrams.length)
    ketQua.push({
      tuNgay: veNgay(tu),
      denNgay: veNgay(den),
      cacDot,
      caoNhat,
      trungBinh,
      apDung: chinhSach === 'TRUNG_BINH' ? trungBinh : caoNhat,
      chongNhau: cacDot.length > 1,
    })
  }
  return ketQua
}

/** Mức giảm áp dụng tại một ngày (0 nếu không đợt nào). */
export function phanTramTaiNgay(muc, chinhSach = 'MAX', ngay = todayIso()) {
  const khoang = lichGiamGia(muc, chinhSach).find((k) => k.tuNgay <= ngay && ngay <= k.denNgay)
  return khoang ? khoang.apDung : 0
}

/** Nhóm danh sách mức giảm (từ API /dot-giam-gia/ap-dung) theo id biến thể. */
export function nhomTheoBienThe(muc) {
  const map = new Map()
  for (const m of muc || []) {
    if (!map.has(m.idBienThe)) map.set(m.idBienThe, [])
    map.get(m.idBienThe).push(m)
  }
  return map
}
