// Quy tắc kiểm tra dữ liệu DÙNG CHUNG cho Khách hàng và Nhân viên (form thêm, form sửa).
// Đây là bản sao của backend/.../service/QuyTacDuLieu.java: hai bản phải khớp nhau từng câu báo lỗi
// (tests/kiemTra.test.js đọc thẳng file Java để kiểm tra điều này). Backend vẫn kiểm tra lại, nên sửa ở đây mà quên
// sửa backend thì chỉ làm lỗi hiện muộn hơn (sau khi bấm lưu), không làm lọt dữ liệu sai.
//
// CĂN CỨ ĐỘ TUỔI (ghi cả trên form và trong CAN_CU_PHAP_LY_VA_QUY_TAC.md):
//  - Khách hàng từ đủ 15 tuổi: Bộ luật Dân sự 2015, Điều 21 khoản 4 - người từ đủ 15 đến chưa đủ 18 tuổi tự mình xác lập,
//    thực hiện giao dịch dân sự (như mua bán hàng thông thường) mà không cần người đại diện đồng ý.
//    Từ đủ 6 đến chưa đủ 15 tuổi phải được người đại diện theo pháp luật đồng ý (khoản 3, trừ nhu cầu sinh hoạt hằng ngày phù hợp lứa tuổi);
//    cửa hàng không kiểm chứng được sự đồng ý đó khi tạo hồ sơ nên lấy mốc 15 (luật không cấm trẻ nhỏ mua hàng, đây là quy định nội bộ).
//  - Nhân viên từ đủ 18 tuổi: Bộ luật Lao động 2019, Điều 143 khoản 1 - "lao động chưa thành niên là người lao động chưa đủ 18 tuổi";
//    loại lao động này bị hạn chế công việc / nơi làm việc (Điều 143 khoản 2-3, Điều 147), thời giờ làm việc (Điều 146), có nguyên tắc
//    sử dụng riêng (Điều 144). Luật không cấm hẳn người 15-17 tuổi đi làm; cửa hàng chọn mốc 18 để không thuộc diện đó (quy định nội bộ).
//  - Tuổi tối đa 100 KHÔNG phải quy định pháp luật, chỉ để chặn gõ nhầm năm sinh (1900 thay vì 2000).
import { todayIso } from './format'

export const TUOI_KHACH_TOI_THIEU = 15
export const TUOI_NHAN_VIEN_TOI_THIEU = 18
export const TUOI_TOI_DA = 100

/** Số điện thoại: 10 chữ số, bắt đầu bằng 0. */
export const SDT = /^0\d{9}$/
export const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
/** Họ tên: bắt đầu bằng chữ cái, chỉ gồm chữ cái (có dấu), khoảng trắng, dấu chấm, nháy đơn, gạch ngang - không có chữ số. */
export const HO_TEN = /^\p{L}[\p{L} .'’-]*$/u
export const CCCD = /^\d{12}$/

export const LOI_SDT = 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.'
export const LOI_EMAIL = 'Email chưa đúng định dạng, ví dụ: ten@gmail.com.'
export const LOI_HO_TEN_KY_TU = 'Họ tên chỉ gồm chữ cái và khoảng trắng, không có chữ số hay ký tự đặc biệt.'
export const LOI_CCCD = 'Số căn cước công dân gồm đúng 12 chữ số.'
export const LOI_DIA_CHI_CHI_TIET_CHU_SO = 'Địa chỉ cụ thể phải có chữ hoặc số (ví dụ: 12 Trần Phú).'

// Mỗi khách hàng lưu tối đa bấy nhiêu địa chỉ giao hàng (cùng giá trị với backend DiaChiKhachHangController.SO_DIA_CHI_MAX)
export const SO_DIA_CHI_MAX = 10

/** Dòng hướng dẫn đặt dưới ô Ngày sinh (có căn cứ pháp lý). */
export const HUONG_DAN_TUOI_KHACH = `Khách hàng phải từ đủ ${TUOI_KHACH_TOI_THIEU} tuổi (Bộ luật Dân sự 2015, Điều 21 khoản 4: từ đủ 15 tuổi tự xác lập giao dịch mua bán thông thường). Có thể bỏ trống.`
export const HUONG_DAN_TUOI_NHAN_VIEN = `Nhân viên phải từ đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi (Bộ luật Lao động 2019, Điều 143: dưới 18 tuổi là lao động chưa thành niên).`

/** null hoặc chỉ có khoảng trắng -> '' ; ngược lại bỏ khoảng trắng đầu / cuối. */
export function cat(s) {
  return s === null || s === undefined ? '' : String(s).trim()
}

/** Họ tên: bỏ khoảng trắng thừa ('  Nguyễn   An ' -> 'Nguyễn An'). */
export function chuanHoaHoTen(s) {
  return cat(s).replace(/\s+/g, ' ')
}

/** Số tuổi tròn của người sinh ngày `ngaySinh` ('yyyy-mm-dd') tại ngày `homNay`. null nếu ngày sinh không đọc được. */
export function tuoiTai(ngaySinh, homNay = todayIso()) {
  const a = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(ngaySinh ?? ''))
  const b = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(homNay ?? ''))
  if (!a || !b) return null
  let tuoi = Number(b[1]) - Number(a[1])
  if (Number(b[2]) < Number(a[2]) || (Number(b[2]) === Number(a[2]) && Number(b[3]) < Number(a[3]))) tuoi--
  return tuoi
}

/**
 * Kiểm tra ngày sinh ('yyyy-mm-dd'). Trả về câu báo lỗi hoặc '' nếu hợp lệ; ngày trống -> '' (nơi gọi tự xét "bắt buộc").
 * doiTuong: 'Khách hàng' / 'Nhân viên' (đưa vào câu báo lỗi).
 */
export function loiNgaySinh(ngaySinh, tuoiToiThieu, doiTuong, homNay = todayIso()) {
  const ngay = String(ngaySinh ?? '').slice(0, 10)
  if (!ngay) return ''
  if (ngay > homNay) return 'Ngày sinh không được ở tương lai.'
  const tuoi = tuoiTai(ngay, homNay)
  if (tuoi === null) return 'Ngày sinh không hợp lệ.'
  if (tuoi < tuoiToiThieu) return `${doiTuong} phải từ đủ ${tuoiToiThieu} tuổi.`
  if (tuoi > TUOI_TOI_DA) return `Ngày sinh không hợp lệ (quá ${TUOI_TOI_DA} tuổi).`
  return ''
}

/** Ngày sinh muộn nhất còn đủ tuổi (hôm nay trừ `tuoi` năm) - để khóa các ngày không chọn được trên lịch. */
export function ngaySinhMuonNhat(tuoi, homNay = todayIso()) {
  const [y, m, d] = homNay.split('-').map(Number)
  // 29/02 trừ về năm không nhuận -> lùi về 28/02 (đủ tuổi vào đúng ngày 01/03, nên ngày muộn nhất được chọn là 28/02)
  const ngayCuoi = new Date(y - tuoi, m, 0).getDate()
  return `${y - tuoi}-${String(m).padStart(2, '0')}-${String(Math.min(d, ngayCuoi)).padStart(2, '0')}`
}

/** Ngày sinh sớm nhất còn hợp lệ (chưa quá TUOI_TOI_DA). */
export function ngaySinhSomNhat(homNay = todayIso()) {
  const [y, m, d] = homNay.split('-').map(Number)
  // Sinh đúng hôm nay của 101 năm trước là đã 101 tuổi (quá hạn); sinh sau đó 1 ngày vẫn là 100 tuổi -> sớm nhất = (hôm nay - 101 năm) + 1 ngày.
  const nam = y - TUOI_TOI_DA - 1
  const moc = new Date(nam, m - 1, Math.min(d, new Date(nam, m, 0).getDate()) + 1)
  return `${moc.getFullYear()}-${String(moc.getMonth() + 1).padStart(2, '0')}-${String(moc.getDate()).padStart(2, '0')}`
}

/** Cộng `soNam` năm vào ngày 'yyyy-mm-dd'; 29/02 sang năm không nhuận thì lùi về 28/02 (giống LocalDate.plusYears bên Java). '' nếu ngày không đọc được. */
export function congNam(ngay, soNam) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(ngay ?? ''))
  if (!m) return ''
  const nam = Number(m[1]) + soNam
  const ngayCuoiThang = new Date(nam, Number(m[2]), 0).getDate()
  return `${String(nam).padStart(4, '0')}-${m[2]}-${String(Math.min(Number(m[3]), ngayCuoiThang)).padStart(2, '0')}`
}

/** Nhân viên chỉ được vào làm từ ngày đủ 18 tuổi trở đi: true nếu `ngayVaoLam` KHÔNG sớm hơn sinh nhật thứ 18. */
export function vaoLamKhiDuTuoi(ngaySinh, ngayVaoLam, tuoi = TUOI_NHAN_VIEN_TOI_THIEU) {
  const moc = congNam(ngaySinh, tuoi)
  return !moc || !ngayVaoLam || String(ngayVaoLam).slice(0, 10) >= moc
}

/* ===== Từng ô: trả về câu báo lỗi hoặc '' ===== */

/** Họ tên. thieu = câu báo khi để trống; toiDa = số ký tự tối đa; boDoDai = không kiểm tra giới hạn độ dài. */
export function loiHoTen(giaTri, { thieu = 'Vui lòng nhập họ tên.', toiDa = 100, boDoDai = false } = {}) {
  const ten = chuanHoaHoTen(giaTri)
  if (!ten) return thieu
  if (!boDoDai && (ten.length < 2 || ten.length > toiDa)) return `Họ tên từ 2 đến ${toiDa} ký tự.`
  if (!HO_TEN.test(ten)) return LOI_HO_TEN_KY_TU
  return ''
}

export function loiSdt(giaTri, thieu = 'Vui lòng nhập số điện thoại.') {
  const sdt = cat(giaTri)
  if (!sdt) return thieu
  if (!SDT.test(sdt)) return LOI_SDT
  return ''
}

export function loiEmail(giaTri, { thieu = 'Vui lòng nhập email.', dinhDang = LOI_EMAIL } = {}) {
  const email = cat(giaTri)
  if (!email) return thieu
  if (email.length > 255 || !EMAIL.test(email)) return dinhDang
  return ''
}

/** Số căn cước (không bắt buộc): để trống thì hợp lệ, đã nhập thì phải đủ 12 chữ số. */
/** Địa chỉ cụ thể (số nhà, tên đường...): bắt buộc, tối đa 255 ký tự và phải có ít nhất một chữ cái hoặc chữ số (không chỉ toàn dấu như "---"). */
export function loiDiaChiCuThe(giaTri) {
  const v = cat(giaTri)
  if (!v) return 'Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).'
  if (v.length > 255) return 'Địa chỉ cụ thể tối đa 255 ký tự.'
  if (!/[\p{L}\p{Nd}]/u.test(v)) return LOI_DIA_CHI_CHI_TIET_CHU_SO
  return ''
}

export function loiCccd(giaTri) {
  const so = cat(giaTri)
  if (!so) return ''
  return CCCD.test(so) ? '' : LOI_CCCD
}

/** Mật khẩu nhân viên mới: 8 - 50 ký tự, có cả chữ và số, không khoảng trắng. */
export function loiMatKhau(giaTri, thieu = 'Nhập mật khẩu cho nhân viên mới.') {
  const mk = giaTri === null || giaTri === undefined ? '' : String(giaTri)
  if (!mk) return thieu
  if (mk.length < 8 || mk.length > 50) return 'Mật khẩu từ 8 đến 50 ký tự.'
  if (!/\p{L}/u.test(mk) || !/\d/.test(mk) || /\s/.test(mk)) return 'Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.'
  return ''
}

/**
 * Sinh mật khẩu tạm ngẫu nhiên 10 ký tự: có chữ hoa, chữ thường và chữ số,
 * bỏ các ký tự dễ nhìn nhầm (0/O, 1/l/I) để nhân viên gõ lại từ mail không bị sai.
 * Khớp hoàn toàn với MatKhauTam.java của backend.
 */
export function sinhMatKhauTam() {
  const HOA = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
  const THUONG = 'abcdefghijkmnpqrstuvwxyz'
  const SO = '23456789'
  const TAT_CA = HOA + THUONG + SO
  const lay = (s) => s.charAt(Math.floor(Math.random() * s.length))
  const kq = [lay(HOA), lay(THUONG), lay(SO)]
  for (let i = 3; i < 10; i++) kq.push(lay(TAT_CA))
  for (let i = kq.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const t = kq[i]
    kq[i] = kq[j]
    kq[j] = t
  }
  return kq.join('')
}

/** Chuẩn hóa để so trùng: bỏ khoảng trắng, email không phân biệt hoa thường. */
export const khoaEmail = (v) => cat(v).toLowerCase()
export const khoaSdt = (v) => cat(v)

/**
 * Tìm bản ghi KHÁC (id khác `idBoQua`) đang dùng cùng giá trị. ds: danh sách đã tải; layGiaTri: (bản ghi) => giá trị; chuanHoa: hàm đồng nhất khóa.
 * Trả về bản ghi trùng hoặc null. Dùng để báo "đã tồn tại" ngay khi gõ, trước khi bấm lưu (backend vẫn kiểm tra lại).
 */
export function timBanTrung(ds, giaTri, layGiaTri, chuanHoa, idBoQua = null) {
  const khoa = chuanHoa(giaTri)
  if (!khoa) return null
  return (ds ?? []).find((x) => x.id !== idBoQua && chuanHoa(layGiaTri(x)) === khoa) ?? null
}
