import { describe, expect, it } from 'vitest'
import { PHI_MAC_DINH, conThieuDeMienPhi, duocMienPhiVanChuyen, phiSauKhiSuaDon } from '../src/utils/phiVanChuyenTinh'

// Miễn phí vận chuyển theo giá trị đơn (Cài đặt → Cài đặt free ship). Cùng luật với backend CaiDatVanChuyenService / HoaDonSuaService.phiSauKhiSua.
const BAT = { mienPhiBat: true, mienPhiTu: 2_000_000 }
const TAT = { mienPhiBat: false, mienPhiTu: 2_000_000 }

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
  const caiDat = { mienPhiBat: true, mienPhiTu: 700_000 }
  const haNoi = 'Số 15 Trần Phú, Hà Đông, Hà Nội'
  const hcm = 'Số 8 Lê Duẩn, Phường Bến Nghé, Thành phố Hồ Chí Minh'
  const phi = (o) => phiSauKhiSuaDon({ caiDat, ...o })

  it('không đổi địa chỉ, không đổi tiền hàng thì giữ phí của đơn', () => {
    expect(phi({ diaChiMoi: haNoi, diaChiCu: haNoi, tienHang: 900_000, tienHangCu: 900_000, phiCu: 30_000 })).toBe(30_000)
    expect(phi({ diaChiMoi: `  ${haNoi} `, diaChiCu: haNoi, tienHang: 900_000, tienHangCu: 900_000, phiCu: 30_000 })).toBe(30_000)
  })

  it('tiền hàng mới đạt ngưỡng thì miễn phí', () => {
    expect(phi({ diaChiMoi: haNoi, diaChiCu: haNoi, tienHang: 750_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(0)
    expect(phi({ diaChiMoi: hcm, diaChiCu: haNoi, tienHang: 900_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(0)
  })

  it('đổi địa chỉ mà chưa đạt ngưỡng thì theo tỉnh mới; không nhận ra tỉnh thì giữ phí cũ', () => {
    expect(phi({ diaChiMoi: hcm, diaChiCu: haNoi, tienHang: 500_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(50_000)
    expect(phi({ diaChiMoi: 'Số 15 Trần Phú, khu phố 3', diaChiCu: haNoi, tienHang: 500_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(30_000)
  })

  it('bớt hàng làm đơn tụt dưới ngưỡng thì tính lại theo tỉnh', () => {
    expect(phi({ diaChiMoi: hcm, diaChiCu: hcm, tienHang: 500_000, tienHangCu: 900_000, phiCu: 0 })).toBe(50_000)
    expect(phi({ diaChiMoi: haNoi, diaChiCu: haNoi, tienHang: 500_000, tienHangCu: 900_000, phiCu: 0 })).toBe(0)
    const la = 'Số 15 Trần Phú, khu phố 3'
    expect(phi({ diaChiMoi: la, diaChiCu: la, tienHang: 500_000, tienHangCu: 900_000, phiCu: 0 })).toBe(PHI_MAC_DINH)
  })

  it('trước và sau đều dưới ngưỡng thì giữ phí nhân viên đã nhập', () => {
    expect(phi({ diaChiMoi: hcm, diaChiCu: hcm, tienHang: 400_000, tienHangCu: 500_000, phiCu: 30_000 })).toBe(30_000)
  })

  it('cửa hàng tắt miễn phí theo giá trị đơn thì như cũ', () => {
    const o = { diaChiMoi: haNoi, diaChiCu: haNoi, tienHang: 900_000, tienHangCu: 500_000, phiCu: 30_000 }
    expect(phiSauKhiSuaDon({ caiDat: { mienPhiBat: false, mienPhiTu: 700_000 }, ...o })).toBe(30_000)
  })
})
