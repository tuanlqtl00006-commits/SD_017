import { describe, it, expect, beforeEach } from 'vitest'
import { dongBoTrangThaiDon } from '../src/utils/donHangLocal'

// Đơn online lưu trên trình duyệt lúc đặt; trang Tài khoản đồng bộ với backend để hiện đúng trạng thái và món hàng.
describe('Đơn hàng của khách: tên sản phẩm', () => {
  beforeEach(() => localStorage.clear())

  it('đơn lưu trên máy thiếu tên sản phẩm: lấy món hàng theo hóa đơn thật của backend', async () => {
    const ds = [{ maHoaDon: 'HD1940', trangThai: 0, trangThaiText: 'Chờ xác nhận', items: [{ bienTheId: 21, soLuong: 1, giaBan: 2392000 }] }]
    const kq = await dongBoTrangThaiDon(ds, async () => ({
      trangThai: 4,
      chiTiet: [{ idSanPham: 21, maSpct: 'SP21-XD-4U-G5', tenSanPham: 'Vợt cầu lông Mizuno Acrospeed 8', mauSac: 'Xanh dương', trongLuong: '4U (80-84g)', chuVi: 'G5', soLuong: 1, donGia: 2392000 }],
    }))
    expect(kq[0].trangThaiText).toBe('Đang giao hàng')
    // Review 05/11: "Đơn hàng của tôi" hiện mã sản phẩm của từng dòng
    expect(kq[0].items).toEqual([
      { sanPhamId: 21, sanPhamTen: 'Vợt cầu lông Mizuno Acrospeed 8', maSpct: 'SP21-XD-4U-G5', mauSac: 'Xanh dương', trongLuong: '4U (80-84g)', chuVi: 'G5', soLuong: 1, giaBan: 2392000 },
    ])
  })

  it('backend không trả chi tiết hoặc mất kết nối: giữ nguyên món hàng đang lưu', async () => {
    const items = [{ sanPhamTen: 'Ống cầu lông Victor NCS Max', soLuong: 2, giaBan: 410000 }]
    const kq1 = await dongBoTrangThaiDon([{ maHoaDon: 'HD1', trangThai: 0, trangThaiText: 'Chờ xác nhận', items }], async () => ({ trangThai: 1 }))
    expect(kq1[0].items).toBe(items)
    const kq2 = await dongBoTrangThaiDon([{ maHoaDon: 'HD2', trangThai: 0, trangThaiText: 'Chờ xác nhận', items }], async () => {
      throw new Error('mất kết nối')
    })
    expect(kq2[0].items).toBe(items)
  })
})
