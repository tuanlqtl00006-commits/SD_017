// Các "ví dụ" trên form Phiếu giảm giá ("Ví dụ với phiếu này") và form Đợt giảm giá ("Ví dụ chạy thử: hai đợt cùng áp cho một
// biến thể") phải ra ĐÚNG theo quy tắc của cửa hàng: đợt giảm trước (giá từng biến thể, làm tròn tới đồng), phiếu giảm sau trên
// tổng tiền đã trừ đợt (% làm tròn xuống, không quá "giảm tối đa", tiền không quá tổng đơn, đơn tối thiểu xét sau đợt), 1 đơn 1 phiếu.
// Giá sau đợt và cách gộp nhiều đợt dùng chung số với backend (LichGiamGiaService.giaSauGiam, GiamGiaChongDot.gop -
// xem ViDuGiamGiaTrenFormTest.java).
import { describe, expect, it } from 'vitest'
import { tinhGiamPhieu, viDuDonHang } from '../src/utils/tinhPhieu'
import { formatPhanTram, gopPhanTram, tinhGiaSauGiam } from '../src/utils/giamGia'

// [tiền hàng, % đợt, loại phiếu, giá trị, giảm tối đa, đơn tối thiểu] -> [tiền sau đợt, đủ điều kiện, phiếu giảm, khách trả]
const BANG_VI_DU = [
  [2_000_000, 10, 'PHAN_TRAM', 10, 200_000, 1_500_000, 1_800_000, true, 180_000, 1_620_000], // ví dụ mặc định trên form phiếu
  [1_600_000, 10, 'PHAN_TRAM', 10, 200_000, 1_500_000, 1_440_000, false, 0, 1_440_000], // đơn tối thiểu xét SAU đợt
  [3_000_000, 15, 'PHAN_TRAM', 10, 200_000, 1_500_000, 2_550_000, true, 200_000, 2_350_000], // chạm trần
  [3_000_000, 25, 'TIEN_MAT', 300_000, null, 2_000_000, 2_250_000, true, 300_000, 1_950_000], // 10% + 15% cộng dồn
  [3_000_000, 12.5, 'PHAN_TRAM', 15, 0, 0, 2_625_000, true, 393_750, 2_231_250], // trung bình 12,5%, không trần
  [1_250_005, 10, 'PHAN_TRAM', 15, null, 0, 1_125_005, true, 168_750, 956_255], // .5 làm tròn lên, % phiếu làm tròn xuống
  [999_990, 0, 'PHAN_TRAM', 15, null, 0, 999_990, true, 149_998, 849_992],
  [250_000, 0, 'TIEN_MAT', 300_000, null, 0, 250_000, true, 250_000, 0], // không giảm quá tổng đơn
  [2_000_000, 100, 'PHAN_TRAM', 10, null, 0, 0, true, 0, 0],
  [5_000, 39.67, 'TIEN_MAT', 1_000, null, 3_000, 3_017, true, 1_000, 2_017], // trung bình 3 đợt 39,67%
  [1_050, 1, 'PHAN_TRAM', 50, null, 0, 1_040, true, 520, 520],
  [2_000_000, 10, 'PHAN_TRAM', 120, null, 0, 1_800_000, true, 1_800_000, 0], // % phiếu quá 100 vẫn chỉ 100%
]

/** Viết lại quy tắc theo kiểu backend Java (số nguyên long, chia nguyên = làm tròn xuống) để đối chiếu với utils của frontend. */
const quyTac = {
  giaSauDot: (giaBan, pt) => (pt <= 0 ? giaBan : pt >= 100 ? 0 : Math.round((giaBan * (100 - pt)) / 100)),
  soTienGiam: (p, tong) => {
    const t = Math.max(0, tong)
    const giaTri = p.giaTri ?? 0
    if (giaTri <= 0) return 0
    if (p.loaiGiam === 'PHAN_TRAM') {
      let giam = Math.trunc((t * Math.min(100, giaTri)) / 100)
      if (p.giamToiDa != null && p.giamToiDa > 0) giam = Math.min(giam, p.giamToiDa)
      return giam
    }
    return Math.min(giaTri, t)
  },
  duDon: (p, tong) => tong >= (p.donToiThieu ?? 0),
  gop: (pts, cheDo) => {
    if (!pts.length) return 0
    const tong = pts.reduce((a, b) => a + b, 0)
    if (cheDo === 'TRUNG_BINH') return Math.round((tong / pts.length) * 100.0) / 100.0
    if (cheDo === 'CONG_DON') return Math.min(100, tong)
    return Math.max(0, ...pts)
  },
}

// Sinh số ngẫu nhiên có hạt giống cố định (chạy lại luôn ra cùng các ca)
function taoNgauNhien(hat) {
  let s = hat >>> 0
  return (n) => {
    s = (s + 0x6d2b79f5) >>> 0
    let t = s
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return Math.floor((((t ^ (t >>> 14)) >>> 0) / 4294967296) * n)
  }
}

describe('Bảng ví dụ tính tiền (đợt giảm trước, phiếu giảm sau, mỗi đơn 1 phiếu)', () => {
  it.each(BANG_VI_DU)('tiền hàng %d ₫ | đợt giảm %s | phiếu %s %d | tối đa %s | đơn từ %d', (tien, pt, loai, giaTri, toiDa, donMin, sauDot, du, giam, khachTra) => {
    const v = viDuDonHang({ loaiGiam: loai, giaTri, giamToiDa: toiDa, donToiThieu: donMin }, tien, pt)
    expect(v.sauDot).toBe(sauDot)
    expect(v.giamDot + v.sauDot).toBe(tien)
    expect(v.phieu.duDieuKien).toBe(du)
    expect(v.phieu.giam).toBe(giam)
    expect(v.khachTra).toBe(khachTra)
  })

  it('chưa đủ đơn tối thiểu: báo rõ đơn tối thiểu và số tiền còn thiếu', () => {
    const v = viDuDonHang({ loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200_000, donToiThieu: 1_500_000 }, 1_600_000, 10)
    expect(v.phieu.lyDo).toBe('Chưa đủ điều kiện: đơn tối thiểu 1.500.000 ₫ (còn thiếu 60.000 ₫).')
    expect(tinhGiamPhieu({ loaiGiam: 'PHAN_TRAM', giaTri: 10, donToiThieu: 1_500_000 }, 1_500_000).duDieuKien).toBe(true) // đúng bằng thì được
  })
})

describe('Ví dụ trên form phiếu = đúng quy tắc tính tiền (30.000 ca ngẫu nhiên)', () => {
  it('tiền sau đợt, tiền phiếu giảm, khách trả trùng từng đồng với quy tắc', () => {
    const r = taoNgauNhien(20261016)
    const giaNgauNhien = () => [1000 * r(20001), r(5_000_001), 500 * r(10001), 1_250_005, 1_050, 5_000, 999_990][r(7)]
    const ptNgauNhien = () => {
      const pts = Array.from({ length: 1 + r(4) }, () => 1 + r(60))
      return r(4) === 0 ? r(101) : quyTac.gop(pts, ['MAX', 'CONG_DON', 'TRUNG_BINH'][r(3)])
    }
    for (let i = 0; i < 30000; i++) {
      const phieu = r(2)
        ? { loaiGiam: 'PHAN_TRAM', giaTri: 1 + r(100), giamToiDa: [null, 0, 1000 * (1 + r(1000))][r(3)], donToiThieu: r(3) ? 1000 * r(5001) : 0 }
        : { loaiGiam: 'TIEN_MAT', giaTri: 1000 * (1 + r(3000)), giamToiDa: null, donToiThieu: r(3) ? 1000 * r(5001) : 0 }
      const tien = giaNgauNhien()
      const pt = ptNgauNhien()
      const v = viDuDonHang(phieu, tien, pt)
      const sauDot = quyTac.giaSauDot(tien, pt)
      const du = quyTac.duDon(phieu, sauDot)
      const giam = du ? quyTac.soTienGiam(phieu, sauDot) : 0
      const ca = JSON.stringify({ phieu, tien, pt })
      expect(v.sauDot, ca).toBe(sauDot)
      expect(v.sauDot, ca).toBe(tinhGiaSauGiam(tien, pt)) // = giá sau giảm hiện ở danh sách sản phẩm / biến thể / lịch giảm giá
      expect(v.phieu.duDieuKien, ca).toBe(du)
      expect(v.phieu.giam, ca).toBe(giam)
      expect(v.khachTra, ca).toBe(sauDot - giam)
      // bất biến: không âm, phiếu không giảm quá tiền hàng, không vượt trần
      expect(v.khachTra).toBeGreaterThanOrEqual(0)
      expect(v.phieu.giam).toBeLessThanOrEqual(v.sauDot)
      if (phieu.loaiGiam === 'PHAN_TRAM' && phieu.giamToiDa > 0) expect(v.phieu.giam).toBeLessThanOrEqual(phieu.giamToiDa)
    }
  })

  it('% đợt ngoài 0 - 100 hoặc bỏ trống được kẹp lại, % lẻ giữ 2 chữ số', () => {
    const p = { loaiGiam: 'TIEN_MAT', giaTri: 50_000 }
    expect(viDuDonHang(p, 1_000_000, 150)).toMatchObject({ phanTramDot: 100, sauDot: 0, khachTra: 0 })
    expect(viDuDonHang(p, 1_000_000, -5)).toMatchObject({ phanTramDot: 0, sauDot: 1_000_000, khachTra: 950_000 })
    expect(viDuDonHang(p, 1_000_000, '')).toMatchObject({ phanTramDot: 0, sauDot: 1_000_000 })
    expect(viDuDonHang(p, 1_000_000, 12.345).phanTramDot).toBe(12.35)
    expect(formatPhanTram(viDuDonHang(p, 1_000_000, 12.5).phanTramDot)).toBe('12,5%')
  })
})

describe('Ví dụ hai đợt chồng nhau trên form đợt giảm giá = cách gộp của backend (GiamGiaChongDot.gop)', () => {
  it('giá 3.000.000, đợt A 10%, đợt B 15%: cao nhất 2.550.000 / cộng dồn 2.250.000 / trung bình 2.625.000', () => {
    const ket = Object.fromEntries(['MAX', 'CONG_DON', 'TRUNG_BINH'].map((k) => [k, tinhGiaSauGiam(3_000_000, gopPhanTram([10, 15], k))]))
    expect(ket).toEqual({ MAX: 2_550_000, CONG_DON: 2_250_000, TRUNG_BINH: 2_625_000 })
  })

  it('gộp % trùng với GiamGiaChongDot.gop ở mọi tổ hợp 1 - 4 đợt (cộng dồn chặn 100%, trung bình 2 chữ số)', () => {
    const r = taoNgauNhien(7)
    for (let i = 0; i < 20000; i++) {
      const pts = Array.from({ length: 1 + r(4) }, () => r(101))
      for (const k of ['MAX', 'CONG_DON', 'TRUNG_BINH']) expect(gopPhanTram(pts, k), `${k} ${pts}`).toBe(quyTac.gop(pts, k))
    }
    expect(gopPhanTram([70, 60], 'CONG_DON')).toBe(100)
    expect(gopPhanTram([39, 40, 40], 'TRUNG_BINH')).toBe(39.67)
    expect(tinhGiaSauGiam(5_000, 39.67)).toBe(3_017) // backend lịch giảm giá cũng ra 3.017 (đã sửa cho khớp)
  })
})
