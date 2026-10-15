// Ngày GIỜ (không chỉ ngày) cho phiếu giảm giá / đợt giảm giá theo lời cô: bắt đầu và kết thúc phải có cả giờ.
// Giá trị trong form và gửi API: 'yyyy-MM-ddTHH:mm' (có thể kèm ':ss'), giờ máy, không múi giờ.
import { isoSangVn, vnSangIso } from './ngay'
import { toIso } from './format'

const pad = (n) => String(n).padStart(2, '0')

/** 'yyyy-MM-ddTHH:mm[:ss]' -> { ngay: 'yyyy-MM-dd', gio: 'HH:mm' } (thiếu phần nào thì phần đó là ''). */
export function tachNgayGio(v) {
  const m = String(v ?? '').match(/^(\d{4}-\d{2}-\d{2})(?:[T ](\d{2}):(\d{2}))?/)
  return m ? { ngay: m[1], gio: m[2] !== undefined ? `${m[2]}:${m[3]}` : '' } : { ngay: '', gio: '' }
}

/** 'HH:mm' có thật (00:00 – 23:59) không? */
export const gioHopLe = (s) => /^([01]\d|2[0-3]):[0-5]\d$/.test(String(s ?? ''))

/** Ghép ngày + giờ thành 'yyyy-MM-ddTHH:mm' ('' nếu chưa có ngày; thiếu giờ thì lấy gioMacDinh). */
export function gopNgayGio(ngay, gio, gioMacDinh = '00:00') {
  if (!ngay) return ''
  return `${ngay}T${gioHopLe(gio) ? gio : gioMacDinh}`
}

/** 'yyyy-MM-ddTHH:mm:ss' -> 'dd/MM/yyyy HH:mm' (chỉ có ngày thì chỉ hiện ngày; sai / trống -> ''). */
export function dinhDangNgayGio(v) {
  const { ngay, gio } = tachNgayGio(v)
  if (!ngay) return ''
  return gio ? `${isoSangVn(ngay)} ${gio}` : isoSangVn(ngay)
}

/** Bây giờ dạng 'yyyy-MM-ddTHH:mm' theo giờ máy. */
export function bayGioIso(d = new Date()) {
  return `${toIso(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 'yyyy-MM-ddTHH:mm' -> 'yyyy-MM-ddTHH:mm:ss' để gửi API. Chỉ có ngày thì lấy gioMacDinh ('00:00' hoặc '23:59'). */
export function ngayGioGuiApi(v, gioMacDinh = '00:00') {
  const { ngay, gio } = tachNgayGio(v)
  if (!ngay) return null
  const g = gioHopLe(gio) ? gio : gioMacDinh
  const giay = String(v).match(/T\d{2}:\d{2}:(\d{2})/)?.[1] ?? '00'
  return `${ngay}T${g}:${giay}`
}

/** Định dạng dần khi gõ giờ: '1430' -> '14:30', '9' -> '09:' (khi không đang xóa). */
export function dinhDangGioKhiGo(raw, dangXoa = false) {
  const s = String(raw ?? '')
  const so = s.replace(/\D/g, '').slice(0, 4)
  if (so.length === 0) return ''
  if (so.length === 1) {
    if (dangXoa) return so
    return s.includes(':') || Number(so) > 2 ? `0${so}:` : so
  }
  if (so.length === 2) return dangXoa ? so : `${so}:`
  return `${so.slice(0, 2)}:${so.slice(2)}`
}

/** Hai chuỗi ngày giờ (cùng định dạng ISO không múi giờ) so sánh được trực tiếp: <0 nếu a trước b. */
export function soSanhNgayGio(a, b) {
  const x = ngayGioGuiApi(a) ?? ''
  const y = ngayGioGuiApi(b) ?? ''
  return x < y ? -1 : x > y ? 1 : 0
}

/** Gõ tay "dd/mm/yyyy HH:mm" (dán từ nơi khác) -> 'yyyy-MM-ddTHH:mm'; không đọc được -> null. */
export function vnNgayGioSangIso(text) {
  const m = String(text ?? '').trim().match(/^(\S+)\s+(\d{1,2}:\d{2})$/)
  if (!m) return null
  const ngay = vnSangIso(m[1])
  const [h, mi] = m[2].split(':')
  const gio = `${pad(Number(h))}:${mi}`
  return ngay && gioHopLe(gio) ? `${ngay}T${gio}` : null
}

/* ----- Mốc thời gian đầy đủ giây + quy tắc kiểm tra (khớp ThoiGianPhut ở backend) ----- */

/** Số phút cho phép giờ bắt đầu của phiếu / đợt MỚI lệch về quá khứ (để bấm "Bây giờ" rồi lưu không bị báo lỗi). */
export const DUNG_SAI_PHUT = 5

/** Bây giờ đầy đủ giây 'yyyy-MM-ddTHH:mm:ss' theo giờ máy (để so với mốc backend trả về). */
export function bayGioDayDu(d = new Date()) {
  return `${toIso(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const giayCua = (v) => String(v ?? '').match(/T\d{2}:\d{2}:(\d{2})/)?.[1]

/** Mốc BẮT ĐẦU đầy đủ giây: thiếu giây -> ':00'; chỉ có ngày -> 00:00:00; sai / trống -> ''. */
export function mocBatDau(v) {
  const { ngay, gio } = tachNgayGio(v)
  if (!ngay) return ''
  return `${ngay}T${gio || '00:00'}:${giayCua(v) ?? '00'}`
}

/** Mốc KẾT THÚC đầy đủ giây: tính tới hết phút nên thiếu giây -> ':59'; chỉ có ngày -> 23:59:59; sai / trống -> ''. */
export function mocKetThuc(v) {
  const { ngay, gio } = tachNgayGio(v)
  if (!ngay) return ''
  if (!gio) return `${ngay}T23:59:59`
  return `${ngay}T${gio}:${giayCua(v) ?? '59'}`
}

const moc = (v) => {
  const t = tachNgayGio(v)
  return t.ngay && t.gio ? `${t.ngay}T${t.gio}` : '' // mốc tới phút
}

/** Hai mốc có cùng phút không (trống chỉ bằng trống). */
export const cungPhut = (a, b) => moc(a) === moc(b)

/** Trừ n phút khỏi 'yyyy-MM-ddTHH:mm:ss' (giờ máy) -> cùng định dạng. */
function truPhut(iso, n) {
  const m = String(iso).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/)
  const d = new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5] - n, +m[6])
  return bayGioDayDu(d)
}

/**
 * Vị trí của "bây giờ" so với khoảng [batDau, ketThuc]: 'SAP' (chưa tới giờ bắt đầu), 'DANG' (đang trong khoảng),
 * 'KET_THUC' (quá giờ kết thúc). Mốc chỉ có ngày thì bắt đầu 00:00:00, kết thúc 23:59:59. Thiếu mốc -> 'DANG'.
 */
export function viTriTheoMoc(batDau, ketThuc, bayGio = bayGioDayDu()) {
  const now = mocBatDau(bayGio) // bayGio chỉ có ngày thì tính từ 00:00
  const bd = mocBatDau(batDau)
  const kt = mocKetThuc(ketThuc)
  if (kt && now > kt) return 'KET_THUC'
  if (bd && now < bd) return 'SAP'
  return 'DANG'
}

/**
 * Kiểm tra khoảng thời gian của phiếu / đợt giảm giá (cùng quy tắc ThoiGianPhut.kiemTra ở backend), trả về lỗi THEO TỪNG Ô:
 * { batDau?: string, ketThuc?: string } (rỗng = hợp lệ).
 *  - bắt buộc cả hai; kết thúc phải sau bắt đầu;
 *  - tạo mới: bắt đầu không ở quá khứ (cho lệch DUNG_SAI_PHUT phút), kết thúc không ở quá khứ;
 *  - sửa: đã bắt đầu thì không đổi giờ bắt đầu; giờ mới (bắt đầu / kết thúc) không được ở quá khứ.
 * @param {string} ten 'Phiếu' | 'Đợt' (đầu câu "… đã bắt đầu nên không đổi được ngày giờ bắt đầu.")
 */
export function kiemTraKhoangThoiGian({ batDau, ketThuc, batDauCu = null, ketThucCu = null, taoMoi = true, ten = 'Phiếu', bayGio = bayGioDayDu() }) {
  const loi = {}
  const bd = mocBatDau(batDau)
  const kt = mocKetThuc(ketThuc)
  if (!bd) loi.batDau = 'Chọn ngày giờ bắt đầu.'
  if (!kt) loi.ketThuc = 'Chọn ngày giờ kết thúc.'
  // So tới PHÚT (kết thúc cùng phút với bắt đầu cũng sai); lỗi của ô nào hiện ở ô đó, không cần điền đủ cả hai mới thấy
  if (bd && kt && kt.slice(0, 16) <= bd.slice(0, 16)) loi.ketThuc = 'Ngày giờ kết thúc phải sau ngày giờ bắt đầu.'
  const sanDuoi = truPhut(bayGio, DUNG_SAI_PHUT)
  if (taoMoi) {
    if (bd && bd < sanDuoi) loi.batDau = 'Ngày giờ bắt đầu không được ở quá khứ.'
    if (kt && !loi.ketThuc && kt < bayGio) loi.ketThuc = 'Ngày giờ kết thúc không được ở quá khứ.'
    return loi
  }
  if (bd) {
    const daBatDau = !!batDauCu && mocBatDau(batDauCu) <= bayGio
    const doiBatDau = !cungPhut(batDau, batDauCu)
    if (doiBatDau && daBatDau) loi.batDau = `${ten} đã bắt đầu nên không đổi được ngày giờ bắt đầu.`
    else if (doiBatDau && bd < sanDuoi) loi.batDau = 'Ngày giờ bắt đầu mới không được ở quá khứ.'
  }
  if (kt && !loi.ketThuc && !cungPhut(ketThuc, ketThucCu) && kt < bayGio) loi.ketThuc = 'Ngày giờ kết thúc mới không được ở quá khứ.'
  return loi
}

/** Khoảng thời gian của hai phiếu / đợt có giao nhau không (mốc kết thúc tính tới hết phút). */
export function giaoNhau(a, b) {
  const [aBd, aKt, bBd, bKt] = [mocBatDau(a.batDau), mocKetThuc(a.ketThuc), mocBatDau(b.batDau), mocKetThuc(b.ketThuc)]
  if (!aBd || !aKt || !bBd || !bKt) return false
  return aBd <= bKt && bBd <= aKt
}

/** "2 ngày 3 giờ", "45 phút" ... từ số phút (làm tròn xuống; < 1 phút -> "dưới 1 phút"). */
export function dinhDangKhoangPhut(phut) {
  const p = Math.max(0, Math.floor(phut))
  if (p < 1) return 'dưới 1 phút'
  const ngay = Math.floor(p / 1440)
  const gio = Math.floor((p % 1440) / 60)
  const ph = p % 60
  const phan = []
  if (ngay) phan.push(`${ngay} ngày`)
  if (gio) phan.push(`${gio} giờ`)
  if (ph && !ngay) phan.push(`${ph} phút`)
  return phan.join(' ')
}

/** Số phút từ mốc a tới mốc b ('yyyy-MM-ddTHH:mm[:ss]'); âm nếu b trước a. */
export function soPhutGiua(a, b) {
  const t = (v) => {
    const m = String(mocBatDau(v)).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/)
    return m ? new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6]).getTime() : NaN
  }
  return (t(b) - t(a)) / 60000
}
