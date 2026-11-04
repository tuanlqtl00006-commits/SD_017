import { describe, expect, it } from 'vitest'
import { PHI_MAC_DINH, nhanRaTinh, phiTheoDiaChi, phiVanChuyenTheoTinh } from '../src/utils/phiVanChuyenTinh'

describe('Phí vận chuyển theo tỉnh', () => {
  it('Hà Nội miễn phí, mọi cách ghi tên', () => {
    for (const t of ['Hà Nội', 'Thành phố Hà Nội', 'TP. Hà Nội', 'ha noi']) expect(phiVanChuyenTheoTinh(t)).toBe(0)
  })

  it('tỉnh khác mất phí, mỗi tỉnh một giá', () => {
    expect(phiVanChuyenTheoTinh('Tỉnh Bắc Ninh')).toBe(25000)
    expect(phiVanChuyenTheoTinh('Tỉnh Quảng Ninh')).toBe(30000)
    expect(phiVanChuyenTheoTinh('Tỉnh Nghệ An')).toBe(35000)
    expect(phiVanChuyenTheoTinh('Thành phố Đà Nẵng')).toBe(40000)
    expect(phiVanChuyenTheoTinh('Tỉnh Lâm Đồng')).toBe(45000)
    expect(phiVanChuyenTheoTinh('Thành phố Đồng Nai')).toBe(50000)
    expect(phiVanChuyenTheoTinh('Thành phố Cần Thơ')).toBe(55000)
  })

  it('chưa chọn tỉnh thì null; tên lạ thì phí mặc định', () => {
    expect(phiVanChuyenTheoTinh('')).toBeNull()
    expect(phiVanChuyenTheoTinh(null)).toBeNull()
    expect(phiVanChuyenTheoTinh('Tỉnh Không Có')).toBe(PHI_MAC_DINH)
  })

  it('phí theo địa chỉ ghi liền: lấy tỉnh ở cuối, không nhận ra thì null', () => {
    expect(phiTheoDiaChi('Số 15 Trần Phú, Phường Hà Đông, Thành phố Hà Nội')).toBe(0)
    expect(phiTheoDiaChi('xcv, Phường Bảo Vinh, Thành phố Đồng Nai')).toBe(50000)
    expect(phiTheoDiaChi('Số 15 Trần Phú, khu phố 3')).toBeNull()
    expect(phiTheoDiaChi('')).toBeNull()
  })

  it('phủ đủ 34 tỉnh/thành sau sáp nhập', () => {
    const ds = ['Hà Nội', 'Bắc Ninh', 'Hưng Yên', 'Phú Thọ', 'Thái Nguyên', 'Hải Phòng', 'Ninh Bình', 'Quảng Ninh', 'Lào Cai', 'Tuyên Quang', 'Lạng Sơn',
      'Cao Bằng', 'Điện Biên', 'Lai Châu', 'Sơn La', 'Thanh Hóa', 'Nghệ An', 'Hà Tĩnh', 'Quảng Trị', 'Huế', 'Đà Nẵng', 'Quảng Ngãi', 'Gia Lai',
      'Khánh Hòa', 'Đắk Lắk', 'Lâm Đồng', 'Hồ Chí Minh', 'Đồng Nai', 'Tây Ninh', 'Cần Thơ', 'Vĩnh Long', 'Đồng Tháp', 'Cà Mau', 'An Giang']
    expect(ds).toHaveLength(34)
    for (const t of ds) expect(nhanRaTinh(t), t).toBe(true)
  })
})
