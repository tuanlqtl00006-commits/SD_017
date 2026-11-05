import { describe, expect, it } from 'vitest'
import { NHAN_TIEN, dongTienDon, tienGiamTheoDotCuaDong } from '../src/utils/tienDon'

// Review 05/11: các màn chưa đồng bộ phần tiền (đợt giảm, phiếu, phí ship). Mọi màn dùng chung hàm này.
describe('Các dòng tiền của đơn (dùng chung mọi màn)', () => {
  it('đủ 5 dòng, đúng tên và thứ tự; Tổng tiền hàng theo giá gốc, các dòng cộng trừ ra đúng tổng', () => {
    // ví dụ màn bán quầy trong video: 2 áo 450.000 giảm đợt 35% -> 585.000, phiếu -146.250, khách trả 438.750
    const t = dongTienDon({ tienHangSauDot: 585000, giamTheoDot: 315000, giamPhieu: 146250, maPhieu: 'VCHE7KL4B', coGiaoHang: true, phiVanChuyen: 0, tongThanhToan: 438750 })
    expect(t.dong.map((d) => d.nhan)).toEqual([NHAN_TIEN.tongTienHang, NHAN_TIEN.giamDot, NHAN_TIEN.phieu, NHAN_TIEN.phiVanChuyen, NHAN_TIEN.tongThanhToan])
    expect(t.dong[0].so).toBe(900000)
    expect(t.dong[2].ma).toBe('VCHE7KL4B')
    expect(t.tong).toBe(438750)
    expect(t.khop).toBe(true)
  })

  it('không giảm theo đợt thì không có dòng đó; đơn tại quầy không có dòng phí vận chuyển', () => {
    const t = dongTienDon({ tienHangSauDot: 234000, giamTheoDot: 0, giamPhieu: 0, coGiaoHang: false, tongThanhToan: 234000 })
    expect(t.dong.map((d) => d.khoa)).toEqual(['tong-tien-hang', 'phieu', 'tong-thanh-toan'])
    expect(t.khop).toBe(true)
  })

  it('chưa chọn tỉnh: phí vận chuyển chưa tính (null), không bị coi là miễn phí', () => {
    const t = dongTienDon({ tienHangSauDot: 500000, coGiaoHang: true, phiVanChuyen: null })
    expect(t.dong.find((d) => d.khoa === 'phi-van-chuyen').so).toBeNull()
  })

  it('giảm theo đợt không bao giờ âm (giá tăng sau khi bán không thành giảm âm)', () => {
    const t = dongTienDon({ tienHangSauDot: 270000, giamTheoDot: -10000, tongThanhToan: 270000 })
    expect(t.dong.some((d) => d.khoa === 'giam-dot')).toBe(false)
    expect(t.dong[0].so).toBe(270000)
    expect(tienGiamTheoDotCuaDong([{ giaGoc: 260000, donGia: 270000, soLuong: 1 }, { giaGoc: 140000, donGia: 112000, soLuong: 2 }])).toBe(56000)
  })

  it('tổng truyền vào lệch với các dòng thì báo không khớp (để test các màn bắt được lỗi trừ 2 lần)', () => {
    const t = dongTienDon({ tienHangSauDot: 585000, giamTheoDot: 315000, giamPhieu: 0, tongThanhToan: 270000 })
    expect(t.khop).toBe(false)
  })
})
