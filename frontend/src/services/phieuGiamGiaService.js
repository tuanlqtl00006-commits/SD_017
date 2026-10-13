import api from './api'

// Gọi API backend: /api/phieu-giam-gia (bảng phieu_giam_gia + phieu_giam_gia_khach_hang).
// Dữ liệu trả về có dạng:
//   { id, ma, ten, hinhThuc: 'CONG_KHAI'|'CA_NHAN', loaiGiam: 'PHAN_TRAM'|'TIEN_MAT', giaTri, giamToiDa, donToiThieu,
//     soLuong, soLuongDaDung, gioiHanMoiKhach, ngayBatDau, ngayKetThuc ('yyyy-mm-dd'), hoatDong, khachHangs: [...] }
// Danh sách (getAll) không kèm khachHangs; dùng getById để lấy danh sách khách được tặng của phiếu cá nhân.

export const phieuGiamGiaService = {
  async getAll() {
    return (await api.get('/phieu-giam-gia')).data
  },

  async getById(id) {
    return (await api.get(`/phieu-giam-gia/${id}`)).data
  },

  /** Khách hàng có thể chọn khi tặng phiếu cá nhân: [{ id, ma, hoTen, soDienThoai, email }] */
  async getKhachHang() {
    return (await api.get('/phieu-giam-gia/khach-hang')).data
  },

  async create(payload) {
    return (await api.post('/phieu-giam-gia', payload)).data
  },

  async update(id, payload) {
    return (await api.put(`/phieu-giam-gia/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/phieu-giam-gia/${id}/trang-thai`)).data
  },

  /** Trạng thái gửi mail: { daCauHinh, dangNhap: 'CHUA_CAU_HINH'|'DANG_KIEM_TRA'|'THANH_CONG'|'THAT_BAI', lyDo } */
  async trangThaiMail() {
    return (await api.get('/phieu-giam-gia/trang-thai-mail')).data
  },

  /** Kết quả gửi mail của lần lưu phiếu gần nhất: { tong, daGui, loi, giaLap, xong, lyDo } */
  async ketQuaMail(id) {
    return (await api.get(`/phieu-giam-gia/${id}/ket-qua-mail`)).data
  },
}

/**
 * Sau khi lưu phiếu cá nhân, mail gửi ở luồng nền của backend. Hàm này hỏi lại kết quả vài lần (mỗi `cach` ms, tối đa `lan` lần)
 * cho tới khi mọi thư có kết quả, rồi trả về { trangThai: 'DA_GUI' | 'LOI' | 'CHUA_CAU_HINH' | 'CHUA_XONG', ... }.
 * Không ném lỗi: backend không trả lời được thì trả về trangThai 'KHONG_RO'.
 */
export async function theoDoiKetQuaMail(id, { lan = 20, cach = 1500, cho = (ms) => new Promise((r) => setTimeout(r, ms)) } = {}) {
  let kq = null
  for (let i = 0; i < lan; i++) {
    await cho(cach)
    try {
      kq = await phieuGiamGiaService.ketQuaMail(id)
    } catch {
      return { trangThai: 'KHONG_RO' }
    }
    if (kq?.xong) break
  }
  if (!kq) return { trangThai: 'KHONG_RO' }
  if (!kq.xong) return { ...kq, trangThai: 'CHUA_XONG' }
  if (kq.loi > 0) return { ...kq, trangThai: 'LOI' }
  if (kq.giaLap > 0 && !kq.daGui) return { ...kq, trangThai: 'CHUA_CAU_HINH' }
  return { ...kq, trangThai: 'DA_GUI' }
}

/** Câu thông báo cho kết quả theo dõi ở trên (null: không cần báo gì). */
export function cauBaoKetQuaMail(kq, maPhieu = '') {
  const ten = maPhieu ? ` phiếu ${maPhieu}` : ''
  switch (kq?.trangThai) {
    case 'DA_GUI':
      return { loai: 'success', noiDung: `Đã gửi ${kq.daGui}/${kq.tong} mail${ten} thành công.` }
    case 'LOI':
      return {
        loai: 'error',
        tieuDe: 'Gửi mail lỗi (phiếu vẫn đã lưu)',
        noiDung: `${kq.loi}/${kq.tong} mail${ten} gửi KHÔNG thành công${kq.daGui ? `, ${kq.daGui} mail đã gửi` : ''}. Lý do: ${kq.lyDo || 'xem log backend'}`,
      }
    case 'CHUA_CAU_HINH':
      return { loai: 'error', tieuDe: 'Mail chưa gửi', noiDung: 'Chưa cấu hình Gmail nên mail chưa được gửi thật (phiếu vẫn đã lưu).' }
    case 'CHUA_XONG':
      return { loai: 'success', noiDung: `Đã gửi ${kq.daGui}/${kq.tong} mail${ten}, các mail còn lại vẫn đang gửi ở nền.` }
    default:
      return null
  }
}
