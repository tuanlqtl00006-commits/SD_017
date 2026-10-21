import { beforeEach, describe, expect, it, vi } from 'vitest'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))

import {
  capNhatGia, chuoiVietQR, crc16, docHoaDonCho, doiSoLuong, ghiHoaDonCho, goiYTienKhachDua, noiDungChuyenKhoan, soHoaDonCho,
  taoHoaDonMoi, themVaoGio, tienGiamDot, tinhThanhToan, tongSoLuong, tongTienHang, xoaKhoiGio,
} from '../src/utils/banHang'
import { duongDanQuayLaiAnToan, layPhien, layToken, luuPhien, xoaPhien } from '../src/services/phien'
import { daCauHinhNganHang } from '../src/constants/thanhToan'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'
import { kiemTraTruyCap } from '../src/router'

const SP = { id: 1, ma: 'SP01-DEN-4U-G5', tenSanPham: 'Yonex Astrox 99 Pro', phanLoai: 'Đen · 4U · G5', giaGoc: 4290000, phanTramGiam: 10, donGia: 3861000, tonKho: 3 }

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  xoaPhien()
  api.get.mockReset()
  api.post.mockReset()
})

/* ====================== Bán hàng tại quầy ====================== */

describe('Bán tại quầy: giỏ hàng', () => {
  it('thêm, cộng dồn, chặn vượt tồn kho / hết hàng', () => {
    let { gio, loi } = themVaoGio([], SP)
    expect(loi).toBe('')
    expect(gio).toHaveLength(1)
    ;({ gio, loi } = themVaoGio(gio, SP, 2))
    expect(gio[0].soLuong).toBe(3)
    const vuot = themVaoGio(gio, SP)
    expect(vuot.loi).toBe('Yonex Astrox 99 Pro chỉ còn 3 sản phẩm trong kho.')
    expect(vuot.gio).toBe(gio)
    expect(themVaoGio([], { ...SP, tonKho: 0 }).loi).toContain('đã hết hàng')
    expect(tongSoLuong(gio)).toBe(3)
    expect(tongTienHang(gio)).toBe(3 * 3861000)
    expect(tienGiamDot(gio)).toBe(3 * 429000)
  })

  it('đổi số lượng: tối thiểu 1, tối đa tồn kho; xóa dòng; cập nhật giá mới từ máy chủ', () => {
    const { gio } = themVaoGio([], SP)
    expect(doiSoLuong(gio, 1, 0)).toMatchObject({ loi: 'Số lượng tối thiểu là 1. Muốn bỏ sản phẩm hãy bấm nút xóa.' })
    expect(doiSoLuong(gio, 1, 0).gio[0].soLuong).toBe(1)
    expect(doiSoLuong(gio, 1, 10).gio[0].soLuong).toBe(3)
    expect(doiSoLuong(gio, 1, 'abc').gio[0].soLuong).toBe(1)
    expect(doiSoLuong(gio, 99, 2).gio).toBe(gio)
    expect(xoaKhoiGio(gio, 1)).toEqual([])
    expect(capNhatGia(gio, [{ idBienThe: 1, donGiaMoi: 4290000 }])[0].donGia).toBe(4290000)
  })

  it('hóa đơn chờ lưu theo từng nhân viên, đánh số nhỏ nhất còn trống', () => {
    const a = taoHoaDonMoi([])
    const b = taoHoaDonMoi([a])
    expect([a.so, b.so]).toEqual([1, 2])
    expect(taoHoaDonMoi([b]).so).toBe(1)
    expect(a.ten).toBe('Hóa đơn 1')
    ghiHoaDonCho(3, { ds: [{ ...a, gio: [{ idBienThe: 1 }] }, b], dangChon: a.id })
    expect(docHoaDonCho(3).dangChon).toBe(a.id)
    expect(soHoaDonCho(3)).toBe(1) // chỉ đếm hóa đơn có hàng
    expect(docHoaDonCho(4)).toEqual({ ds: [], dangChon: null })
    localStorage.setItem('fs_hoa_don_cho_5', '{hỏng')
    expect(docHoaDonCho(5)).toEqual({ ds: [], dangChon: null })
  })
})

describe('Bán tại quầy: thanh toán', () => {
  it('tiền mặt: tiền thừa / thiếu', () => {
    expect(tinhThanhToan({ tongTien: 1000000, giam: 100000, tienMat: 1000000 })).toMatchObject({ phaiTra: 900000, tienThua: 100000, loi: '' })
    expect(tinhThanhToan({ tongTien: 1000000, tienMat: '' }).loi).toBe('Nhập tiền khách đưa.')
    expect(tinhThanhToan({ tongTien: 1000000, tienMat: 500000 })).toMatchObject({ conThieu: 500000, loi: 'Khách trả chưa đủ.', tienThua: 0 })
  })

  it('chuyển khoản trả đúng số phải trả; kết hợp: chuyển một phần, còn lại tiền mặt', () => {
    expect(tinhThanhToan({ tongTien: 2000000, phuongThuc: 'CHUYEN_KHOAN', tienMat: 999 })).toMatchObject({ chuyenKhoan: 2000000, tienMat: 0, loi: '' })
    expect(tinhThanhToan({ tongTien: 2000000, phuongThuc: 'KET_HOP', chuyenKhoan: 1500000, tienMat: 600000 }))
      .toMatchObject({ tienThua: 100000, loi: '' })
    expect(tinhThanhToan({ tongTien: 2000000, phuongThuc: 'KET_HOP', chuyenKhoan: 2500000, tienMat: 0 }).loi)
      .toBe('Tiền chuyển khoản không được lớn hơn số tiền phải trả.')
  })

  it('gợi ý tiền khách đưa: đúng số + làm tròn lên, không trùng, tối đa 5', () => {
    expect(goiYTienKhachDua(3861000)).toEqual([3861000, 3870000, 3900000, 4000000])
    expect(goiYTienKhachDua(500000)).toEqual([500000, 600000])
    expect(goiYTienKhachDua(0)).toEqual([])
  })

  it('CRC-16/CCITT-FALSE và chuỗi VietQR hợp lệ', () => {
    expect(crc16('123456789')).toBe('29B1')
    const qr = chuoiVietQR({ bin: '970436', soTaiKhoan: '0123456789', soTien: 3861000, noiDung: 'Thanh toán HĐ #HD12!' })
    expect(qr.startsWith('000201010212')).toBe(true)
    expect(qr).toContain('0010A000000727')
    expect(qr).toContain('0006970436')
    expect(qr).toContain('01100123456789')
    expect(qr).toContain('5303704')
    expect(qr).toContain('54073861000')
    expect(qr).toContain('5802VN')
    expect(qr).toContain('0818Thanh toan HD HD12')
    expect(qr.slice(-8, -4)).toBe('6304')
    expect(crc16(qr.slice(0, -4))).toBe(qr.slice(-4))
    // không có số tiền: QR tĩnh (11)
    expect(chuoiVietQR({ bin: '970436', soTaiKhoan: '0123456789' }).startsWith('000201010211')).toBe(true)
    expect(noiDungChuyenKhoan('Đơn hàng của Nguyễn Văn An rất dài quá 25 ký tự')).toHaveLength(25)
    expect(daCauHinhNganHang({ bin: '970436', soTaiKhoan: '0123456789' })).toBe(true)
    expect(daCauHinhNganHang({ bin: '', soTaiKhoan: '' })).toBe(false)
  })
})

/* ====================== Đăng nhập / phân quyền ====================== */

describe('Phiên đăng nhập', () => {
  it('ghi nhớ -> localStorage, không ghi nhớ -> sessionStorage; hết hạn thì bỏ', () => {
    luuPhien({ token: 'a.b', hetHan: '2099-01-01T00:00:00', nguoiDung: { id: 1 }, nho: true })
    expect(localStorage.getItem('fs_phien')).toContain('a.b')
    expect(layToken()).toBe('a.b')
    luuPhien({ token: 'c.d', hetHan: '2099-01-01T00:00:00', nguoiDung: { id: 1 }, nho: false })
    expect(localStorage.getItem('fs_phien')).toBeNull()
    expect(sessionStorage.getItem('fs_phien')).toContain('c.d')
    luuPhien({ token: 'e.f', hetHan: '2000-01-01T00:00:00', nguoiDung: { id: 1 } })
    expect(layPhien()).toBeNull()
    expect(localStorage.getItem('fs_phien')).toBeNull()
  })

  it('chỉ quay lại đường dẫn trong trang', () => {
    expect(duongDanQuayLaiAnToan('/hoa-don/5')).toBe('/hoa-don/5')
    expect(duongDanQuayLaiAnToan('//web-la.com')).toBeNull()
    expect(duongDanQuayLaiAnToan('/\\web-la.com')).toBeNull()
    expect(duongDanQuayLaiAnToan('https://web-la.com')).toBeNull()
    expect(duongDanQuayLaiAnToan('/dang-nhap')).toBeNull()
    expect(duongDanQuayLaiAnToan(undefined)).toBeNull()
  })
})

describe('Kiểm tra truy cập trang', () => {
  const NV = { id: 3, ma: 'NV03', hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
  const QL = { id: 1, ma: 'NV01', hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' }
  function dangNhapLa(nd) {
    luuPhien({ token: 'x.y', hetHan: '2099-01-01T00:00:00', nguoiDung: nd, nho: true })
    api.get.mockImplementation(async (url) => (url === '/auth/cau-hinh' ? { data: { batDangNhap: true } } : { data: nd }))
    _datLaiAuth()
  }
  const trang = (fullPath, meta, name) => ({ fullPath, meta, name, query: {} })

  it('chưa đăng nhập: về trang đăng nhập kèm đường dẫn quay lại; trang công khai vào thẳng', async () => {
    api.get.mockResolvedValue({ data: { batDangNhap: true } })
    _datLaiAuth()
    expect(await kiemTraTruyCap(trang('/ban-hang', { roles: ['quan-ly', 'nhan-vien'] }))).toEqual({ path: '/dang-nhap', query: { quayLai: '/ban-hang' } })
    expect(await kiemTraTruyCap(trang('/shop', { congKhai: true }, 'shop-home'))).toBe(true)
    expect(await kiemTraTruyCap(trang('/dang-nhap', { congKhai: true }, 'dang-nhap'))).toBe(true)
  })

  it('nhân viên không vào được trang của quản lý; đã đăng nhập mở trang đăng nhập thì vào trang chủ theo vai trò', async () => {
    dangNhapLa(NV)
    expect(await kiemTraTruyCap(trang('/thong-ke', { roles: ['quan-ly'] }))).toBe('/ban-hang')
    expect(await kiemTraTruyCap(trang('/ban-hang', { roles: ['quan-ly', 'nhan-vien'] }))).toBe(true)
    expect(await kiemTraTruyCap(trang('/dang-nhap', { congKhai: true }, 'dang-nhap'))).toBe('/ban-hang')
    dangNhapLa(QL)
    expect(await kiemTraTruyCap(trang('/thong-ke', { roles: ['quan-ly'] }))).toBe(true)
    expect(useAuth().trangChu()).toBe('/thong-ke')
  })
})
