import { describe, expect, it } from 'vitest'
import { conThieuDeMienPhi, duocMienPhiVanChuyen, phiGiaoHangCaiDat, phiSauKhiSuaDon, phiVanChuyenDon } from '../src/utils/phiVanChuyen'

// Phí vận chuyển = một mức phí giao hàng quản lý cài + miễn phí theo giá trị đơn (Cài đặt → Cài đặt free ship).
// Góp ý 16/11: bỏ bảng phí viết cứng theo tỉnh. Cùng luật với backend CaiDatVanChuyenService / HoaDonSuaService.phiSauKhiSua.
const BAT = { mienPhiBat: true, mienPhiTu: 2_000_000, phiGiaoHang: 25_000 }
const TAT = { mienPhiBat: false, mienPhiTu: 2_000_000, phiGiaoHang: 25_000 }

describe('Phí giao hàng quản lý cài', () => {
  it('chưa đạt ngưỡng thì thu phí cài đặt, đạt thì 0; chưa tải cài đặt thì 0', () => {
    expect(phiGiaoHangCaiDat(BAT)).toBe(25_000)
    expect(phiVanChuyenDon(BAT, 1_999_999)).toBe(25_000)
    expect(phiVanChuyenDon(BAT, 2_000_000)).toBe(0)
    expect(phiVanChuyenDon(TAT, 160_000_000)).toBe(25_000)
    expect(phiGiaoHangCaiDat(null)).toBe(0)
    expect(phiGiaoHangCaiDat({ phiGiaoHang: -5 })).toBe(0)
  })
})

describe('Miễn phí vận chuyển theo giá trị đơn', () => {
  it('đạt ngưỡng thì miễn phí, thiếu 1 đồng thì chưa', () => {
    expect(duocMienPhiVanChuyen(BAT, 1_999_999)).toBe(false)
    expect(duocMienPhiVanChuyen(BAT, 2_000_000)).toBe(true)
    expect(duocMienPhiVanChuyen(BAT, 160_000_000)).toBe(true)
  })

  it('đang tắt, chưa tải cài đặt, giỏ trống thì không miễn phí', () => {
    expect(duocMienPhiVanChuyen(TAT, 160_000_000)).toBe(false)
    expect(duocMienPhiVanChuyen(null, 160_000_000)).toBe(false)
    expect(duocMienPhiVanChuyen({ mienPhiBat: true, mienPhiTu: 0 }, 160_000_000)).toBe(false)
    expect(duocMienPhiVanChuyen(BAT, 0)).toBe(false)
  })

  it('còn thiếu bao nhiêu để được miễn phí', () => {
    expect(conThieuDeMienPhi(BAT, 1_500_000)).toBe(500_000)
    expect(conThieuDeMienPhi(BAT, 2_000_000)).toBe(0)
    expect(conThieuDeMienPhi(BAT, 3_000_000)).toBe(0)
    expect(conThieuDeMienPhi(TAT, 1_500_000)).toBe(0)
    expect(conThieuDeMienPhi(null, 1_500_000)).toBe(0)
  })
})

describe('Phí giao hàng sau khi sửa đơn (giống máy chủ)', () => {
  const caiDat = { mienPhiBat: true, mienPhiTu: 700_000, phiGiaoHang: 40_000 }
  const phi = (o) => phiSauKhiSuaDon({ caiDat, ...o })

  it('không đổi tiền hàng thì giữ phí của đơn (đổi địa chỉ không đổi phí)', () => {
    expect(phi({ tienHang: 900_000, tienHangCu: 900_000, phiCu: 30_000 })).toBe(30_000)
    expect(phi({ tienHang: 500_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(30_000)
  })

  it('tiền hàng mới đạt ngưỡng thì miễn phí', () => {
    expect(phi({ tienHang: 750_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(0)
  })

  it('bớt hàng làm đơn tụt dưới ngưỡng (trước được miễn phí) thì thu phí quản lý đang cài', () => {
    expect(phi({ tienHang: 500_000, tienHangCu: 900_000, phiCu: 0 })).toBe(40_000)
  })

  it('trước và sau đều dưới ngưỡng thì giữ phí nhân viên đã nhập', () => {
    expect(phi({ tienHang: 400_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(30_000)
  })

  it('cửa hàng tắt miễn phí theo giá trị đơn thì như cũ', () => {
    expect(phiSauKhiSuaDon({ caiDat: { mienPhiBat: false, mienPhiTu: 700_000, phiGiaoHang: 40_000 }, tienHang: 900_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(30_000)
  })
})
