import { describe, expect, it } from 'vitest'
import { PHI_MAC_DINH, nhanRaTinh, phiTheoDiaChi, phiVanChuyenTheoTinh } from '../src/utils/phiVanChuyenTinh'

describe('Phí vận chuyển theo tỉnh', () => {
  // Góp ý 15/11 (video 22:22 [14:39–15:55]): Hà Nội không miễn phí; phường (nội thành) 25.000, xã (ngoại thành) 30.000
  it('Hà Nội: phường 25.000, xã 30.000, chưa biết phường/xã thì như phường; mọi cách ghi tên', () => {
    for (const t of ['Hà Nội', 'Thành phố Hà Nội', 'TP. Hà Nội', 'ha noi']) expect(phiVanChuyenTheoTinh(t)).toBe(25000)
    expect(phiVanChuyenTheoTinh('Thành phố Hà Nội', 'Phường Hà Đông')).toBe(25000)
    expect(phiVanChuyenTheoTinh('Thành phố Hà Nội', 'Xã Sóc Sơn')).toBe(30000)
    expect(phiVanChuyenTheoTinh('Tỉnh Bắc Ninh', 'Xã Yên Phong')).toBe(25000) // tỉnh khác không phân biệt
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
    expect(phiTheoDiaChi('Số 15 Trần Phú, Phường Hà Đông, Thành phố Hà Nội')).toBe(25000)
    expect(phiTheoDiaChi('Thôn 2, Xã Sóc Sơn, Thành phố Hà Nội')).toBe(30000)
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
