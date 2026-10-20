// File Excel MẪU để nhập hàng loạt nhân viên (nút "Nhập Excel" ở trang Nhân viên).
// Tiêu đề cột phải được backend nhận ra (NhanVienImportService.Cot): backend bỏ dấu, bỏ "(*)" rồi so với các tên chấp nhận,
// nên thứ tự cột không quan trọng; cột có (*) là bắt buộc. tests/nhanVienImport.test.js đối chiếu danh sách này với file Java.
import { exportExcel } from './exportExcel'
import { formatDate, todayIso } from './format'

/** Mỗi lần nhập tối đa bấy nhiêu nhân viên (khớp NhanVienImportService.TOI_DA_DONG). */
export const TOI_DA_DONG_NHAP = 100
export const TEN_FILE_MAU = 'mau_nhap_nhan_vien.xlsx'

// Cả file mẫu để dạng văn bản (text): giữ số 0 đầu của số điện thoại / căn cước, và ngày dd/mm/yyyy không bị Excel đổi theo kiểu tháng/ngày
export const COT_MAU = [
  { header: 'Họ và tên (*)', key: 'hoTen', width: 26, text: true },
  { header: 'Giới tính (*)', key: 'gioiTinh', width: 12, text: true },
  { header: 'Ngày sinh (*)', key: 'ngaySinh', width: 14, text: true },
  { header: 'Số điện thoại (*)', key: 'soDienThoai', width: 17, text: true },
  { header: 'Email (*)', key: 'email', width: 30, text: true },
  { header: 'Số căn cước công dân', key: 'canCuoc', width: 22, text: true },
  { header: 'Ngày vào làm', key: 'ngayVaoLam', width: 14, text: true },
  { header: 'Vai trò', key: 'vaiTro', width: 14, text: true },
  { header: 'Địa chỉ cụ thể (*)', key: 'diaChiCuThe', width: 30, text: true },
  { header: 'Phường/Xã (*)', key: 'phuongXa', width: 22, text: true },
  { header: 'Tỉnh/Thành phố (*)', key: 'tinhThanh', width: 22, text: true },
]

/** Hai dòng ví dụ (hợp lệ, điền đầy đủ tất cả các cột, email @gmail.com, địa chỉ chuẩn): người dùng xóa hoặc ghi đè trước khi nhập thật. */
export function dongMau(homNay = todayIso()) {
  return [
    {
      hoTen: 'Nguyễn Văn Mẫu', gioiTinh: 'Nam', ngaySinh: '25/12/2000', soDienThoai: '0900000001', email: 'nguyenvanmau.footstyle@gmail.com',
      canCuoc: '001200012345', ngayVaoLam: formatDate(homNay), vaiTro: 'Nhân viên', diaChiCuThe: 'Số 12 Nguyễn Trãi', phuongXa: 'Phường Thượng Đình', tinhThanh: 'Thành phố Hà Nội',
    },
    {
      hoTen: 'Trần Thị Ví Dụ', gioiTinh: 'Nữ', ngaySinh: '03/04/1998', soDienThoai: '0900000002', email: 'tranthividu.footstyle@gmail.com',
      canCuoc: '001200098765', ngayVaoLam: formatDate(homNay), vaiTro: 'Quản lý', diaChiCuThe: 'Số 8 Lê Lợi', phuongXa: 'Phường Lam Sơn', tinhThanh: 'Tỉnh Hưng Yên',
    },
  ]
}

export function taiFileMau() {
  exportExcel({ filename: TEN_FILE_MAU, sheetName: 'Nhân viên', columns: COT_MAU, rows: dongMau() })
}

/** Cột của file danh sách tài khoản vừa tạo (có mật khẩu tạm) để quản lý lưu / gửi cho nhân viên. */
export const COT_TAI_KHOAN = [
  { header: 'Mã NV', key: 'ma', width: 12, text: true },
  { header: 'Họ tên', key: 'hoTen', width: 28, text: true },
  { header: 'Email đăng nhập', key: 'email', width: 32, text: true },
  { header: 'Vai trò', key: 'vaiTro', width: 14, text: true },
  { header: 'Mật khẩu tạm', key: 'matKhauTam', width: 20, text: true },
]
