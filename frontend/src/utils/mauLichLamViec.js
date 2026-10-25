// File Excel lịch làm việc: cùng một bộ cột cho "Tải file mẫu", "Xuất Excel" và "Nhập Excel"
// (backend LichLamViecImportService.Cot nhận các tiêu đề này), nên xuất lịch tuần ra, sửa ngày / ca rồi nhập lại được.
import { exportExcel } from './exportExcel'
import { formatDate } from './format'

export const TOI_DA_DONG_NHAP_LICH = 500

export const COT_LICH = [
  { header: 'Mã nhân viên (*)', key: 'maNhanVien', width: 16, text: true },
  { header: 'Họ tên', key: 'hoTen', width: 26, text: true },
  { header: 'Ngày làm (*)', key: 'ngayLam', width: 14, text: true },
  { header: 'Ca làm (*)', key: 'ca', width: 14, text: true },
  { header: 'Giờ', key: 'gio', width: 14, text: true },
  { header: 'Ghi chú', key: 'ghiChu', width: 30, text: true },
]

const gioCa = (c) => (c?.gioBatDau && c?.gioKetThuc ? `${c.gioBatDau} - ${c.gioKetThuc}` : '')

/** Dòng Excel từ lịch đã xếp (API /lich-lam-viec), sắp theo ngày rồi giờ bắt đầu. */
export function dongTuLich(lich) {
  return [...(lich || [])]
    .sort((a, b) => a.ngayLam.localeCompare(b.ngayLam) || String(a.gioBatDau).localeCompare(String(b.gioBatDau)) || a.hoTen.localeCompare(b.hoTen, 'vi'))
    .map((l) => ({ maNhanVien: l.maNhanVien, hoTen: l.hoTen, ngayLam: formatDate(l.ngayLam), ca: l.tenCa, gio: gioCa(l), ghiChu: l.ghiChu || '' }))
}

/**
 * Dòng ví dụ cho file mẫu: tối đa 3 nhân viên đầu, mỗi người một ca đang dùng (xoay vòng), vào thứ 2 của tuần (hoặc hôm nay nếu thứ 2 đã qua).
 * Dùng dữ liệu thật để file mẫu nhập được ngay.
 */
export function dongMauLich({ nhanViens = [], cas = [], thu2, homNay }) {
  const dangDung = cas.filter((c) => c.hoatDong)
  if (!nhanViens.length || !dangDung.length || !thu2) return []
  const ngay = homNay && thu2 < homNay ? homNay : thu2
  return nhanViens.slice(0, 3).map((nv, i) => {
    const ca = dangDung[i % dangDung.length]
    return { maNhanVien: nv.maNhanVien, hoTen: nv.hoTen, ngayLam: formatDate(ngay), ca: ca.tenCa, gio: gioCa(ca), ghiChu: '' }
  })
}

export function taiFileMauLich(thamSo) {
  exportExcel({ filename: 'mau_nhap_lich_lam_viec.xlsx', sheetName: 'Lịch làm việc', columns: COT_LICH, rows: dongMauLich(thamSo) })
}

export function xuatLichTuan(lich, thu2) {
  exportExcel({ filename: `lich_lam_viec_${thu2}.xlsx`, sheetName: 'Lịch làm việc', columns: COT_LICH, rows: dongTuLich(lich) })
}
