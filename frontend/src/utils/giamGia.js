// Tính giảm giá của biến thể khi nằm trong NHIỀU đợt giảm giá (đợt chồng nhau về thời gian). Thời gian tính tới từng GIÂY
// (đợt có ngày GIỜ bắt đầu - kết thúc), cùng cách chia đoạn với backend (GiamGiaChongDot / LichGiamGiaService).
//
// Ví dụ (đúng kịch bản kiểm thử):
//   Đợt A: 10%, 1/10 08:00 - 10/10 17:00, biến thể SPCT1, SPCT5
//   Đợt B: 15%, 4/10 12:00 - 20/10 23:59, biến thể SPCT1, SPCT3, SPCT4
//   => SPCT1: 1/10 08:00 - 4/10 11:59 giảm 10%
//             4/10 12:00 - 10/10 17:00 CHỒNG NHAU: cao nhất 15% | cộng dồn 25% | trung bình 12,5%
//             10/10 17:01 - 20/10 23:59 giảm 15%
//      SPCT5: 1/10 08:00 - 10/10 17:00 giảm 10%        SPCT3, SPCT4: 4/10 12:00 - 20/10 23:59 giảm 15%
//
// Cách tính khi chồng nhau do backend quyết định (app.giam-gia.che-do-chong-dot):
//   'MAX'        = lấy mức cao nhất (mặc định)
//   'CONG_DON'   = cộng dồn các đợt (tối đa 100%)
//   'TRUNG_BINH' = trung bình cộng các đợt đang chồng nhau
import { bayGioDayDu, mocBatDau, mocKetThuc } from './ngayGio'

const lamTron = (x) => Math.round(x * 100) / 100

// 'yyyy-MM-ddTHH:mm:ss' (giờ máy, không múi giờ) <-> số giây; tính trên UTC để không dính giờ mùa hè
const sangGiay = (iso) => {
  const m = String(iso ?? '').match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/)
  return m ? Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6]) / 1000 : NaN
}
const veIso = (giay) => new Date(giay * 1000).toISOString().slice(0, 19)

/**
 * Giá sau giảm của một biến thể: làm tròn tới đồng (vd 2.500.000 giảm 15% -> 2.125.000; giảm 12,5% -> 2.187.500).
 * Dùng chung cho danh sách sản phẩm / biến thể, trang soạn đợt giảm giá (khớp cách backend tính trong LichGiamGiaService).
 */
export function tinhGiaSauGiam(giaBan, phanTram) {
  const gia = Number(giaBan) || 0
  const pct = Number(phanTram) || 0
  if (pct <= 0) return gia
  if (pct >= 100) return 0
  return Math.round((gia * (100 - pct)) / 100)
}

/** 12.5 -> '12,5%', 15 -> '15%' */
export function formatPhanTram(x) {
  if (x === null || x === undefined) return ''
  return `${String(lamTron(x)).replace('.', ',')}%`
}

/** Gộp % của các đợt đang chồng nhau thành một mức giảm theo cách đã chọn (cùng quy tắc GiamGiaChongDot.gop ở backend). */
export function gopPhanTram(phanTrams, cheDo = 'MAX') {
  if (!phanTrams?.length) return 0
  const tong = phanTrams.reduce((a, b) => a + b, 0)
  if (cheDo === 'TRUNG_BINH') return lamTron(tong / phanTrams.length)
  if (cheDo === 'CONG_DON') return Math.min(100, tong)
  return Math.max(...phanTrams)
}

const cungTapDot = (a, b) => {
  if (a.length !== b.length) return false
  const ids = new Set(a.map((x) => x.idDot))
  return b.every((x) => ids.has(x.idDot))
}

/**
 * Gộp các đợt của MỘT biến thể thành các đoạn thời gian liên tiếp, mỗi đoạn có một mức giảm.
 * @param {Array<{idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc}>} muc  các đợt chứa biến thể này
 *        (ngayBatDau / ngayKetThuc: 'yyyy-MM-ddTHH:mm[:ss]' hoặc chỉ ngày; kết thúc tính tới hết phút / hết ngày)
 * @param {'MAX'|'CONG_DON'|'TRUNG_BINH'} cheDo
 * @param {{batDau?: string, ketThuc?: string}} [gioiHan] chỉ lấy phần nằm trong khoảng này (vd khoảng thời gian của đợt đang soạn)
 * @returns {Array<{tuLuc, denLuc, cacDot, caoNhat, congDon, trungBinh, apDung, chongNhau}>} theo thứ tự thời gian
 *          (tuLuc / denLuc: 'yyyy-MM-ddTHH:mm:ss', đều tính)
 */
export function lichGiamGia(muc, cheDo = 'MAX', gioiHan = null) {
  const ds = []
  for (const m of muc || []) {
    const bd = sangGiay(mocBatDau(m.ngayBatDau))
    const kt = sangGiay(mocKetThuc(m.ngayKetThuc))
    if (Number.isNaN(bd) || Number.isNaN(kt) || kt < bd) continue
    ds.push({ m, bd, kt })
  }
  if (!ds.length) return []

  const moc = new Set()
  for (const { bd, kt } of ds) {
    moc.add(bd)
    moc.add(kt + 1) // kết thúc tính tới hết giây kết thúc
  }
  const mocs = [...moc].sort((a, b) => a - b)

  const gh = gioiHan ? { tu: sangGiay(mocBatDau(gioiHan.batDau)), den: sangGiay(mocKetThuc(gioiHan.ketThuc)) } : null
  const ketQua = []
  for (let i = 0; i < mocs.length - 1; i++) {
    const tu = mocs[i]
    const den = mocs[i + 1] - 1
    const cacDot = ds.filter((x) => x.bd <= tu && x.kt >= den).map((x) => x.m)
    if (!cacDot.length) continue // khoảng trống: không đợt nào giảm
    const phanTrams = cacDot.map((m) => Number(m.phanTram) || 0)
    const tong = phanTrams.reduce((a, b) => a + b, 0)
    const doan = {
      tuLuc: veIso(tu),
      denLuc: veIso(den),
      cacDot,
      caoNhat: Math.max(...phanTrams),
      congDon: Math.min(100, tong),
      trungBinh: lamTron(tong / phanTrams.length),
      apDung: gopPhanTram(phanTrams, cheDo),
      chongNhau: cacDot.length > 1,
    }
    // Đoạn liền kề có cùng các đợt thì gộp lại cho gọn
    const truoc = ketQua[ketQua.length - 1]
    if (truoc && sangGiay(truoc.denLuc) + 1 === tu && cungTapDot(truoc.cacDot, cacDot)) {
      ketQua[ketQua.length - 1] = { ...doan, tuLuc: truoc.tuLuc }
    } else {
      ketQua.push(doan)
    }
  }
  if (!gh || Number.isNaN(gh.tu) || Number.isNaN(gh.den)) return ketQua

  // Cắt theo khoảng giới hạn
  const cat = []
  for (const k of ketQua) {
    const tu = Math.max(sangGiay(k.tuLuc), gh.tu)
    const den = Math.min(sangGiay(k.denLuc), gh.den)
    if (tu > den) continue
    cat.push({ ...k, tuLuc: veIso(tu), denLuc: veIso(den) })
  }
  return cat
}

/** Mức giảm áp dụng tại một thời điểm (0 nếu không đợt nào). `thoiDiem`: 'yyyy-MM-ddTHH:mm[:ss]', mặc định là bây giờ. */
export function phanTramTaiThoiDiem(muc, cheDo = 'MAX', thoiDiem = bayGioDayDu()) {
  const t = sangGiay(mocBatDau(thoiDiem))
  const khoang = lichGiamGia(muc, cheDo).find((k) => sangGiay(k.tuLuc) <= t && t <= sangGiay(k.denLuc))
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
