import { describe, expect, it } from 'vitest'
import { goiYPhieuMoi } from '../src/utils/banHang'

// Đúng các phiếu công khai đang chạy trong CSDL mẫu (điều kiện và mức giảm)
const E7 = { ma: 'VCHE7KL4B', loaiGiam: 'PHAN_TRAM', giaTri: 25, giamToiDa: 250000, donToiThieu: 300000 }
const FS500 = { ma: 'VCHFS500K', loaiGiam: 'TIEN_MAT', giaTri: 30000, giamToiDa: null, donToiThieu: 500000 }
const SN26 = { ma: 'VCHSN26FS', loaiGiam: 'PHAN_TRAM', giaTri: 15, giamToiDa: 300000, donToiThieu: 1000000 }
const QX2 = { ma: 'VCH7QX2YN', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000, donToiThieu: 1500000 }
const TAT_CA = [E7, FS500, SN26, QX2]

// Dựng đúng danh sách máy chủ trả về cho một tổng tiền: đủ điều kiện hoặc chỉ thiếu đơn tối thiểu
const danhSach = (tong, phieus = TAT_CA) =>
  phieus.map((p) => (tong >= p.donToiThieu ? { ...p, apDungDuoc: true } : { ...p, apDungDuoc: false, canMuaThem: p.donToiThieu - tong }))
const dang = (ds, ma) => ds.find((p) => p.ma === ma) ?? null
const dong = (donGia, tonKho = 10, soLuong = 1, ten = 'Vợt A') => ({ ten, donGia, tonKho, soLuong })

describe('goiYPhieuMoi: mua thêm bao nhiêu thì được phiếu mới', () => {
  it('chưa có phiếu: gợi ý mức đơn tối thiểu gần nhất, quy đổi ra số sản phẩm trong giỏ', () => {
    const tong = 120000
    expect(goiYPhieuMoi(danhSach(tong), tong, [dong(120000)], null)).toEqual({
      ma: 'VCHE7KL4B', canThem: 180000, giam: 75000, sanPham: { ten: 'Vợt A', soLuong: 2, donGia: 120000 },
    })
  })

  it('đơn đang được phiếu % tăng dần: phiếu ngưỡng cao hơn nhưng giảm ít hơn thì không gợi ý ở ngưỡng đó', () => {
    // 900k: E7 giảm 225k. Tới 1 triệu, SN26 mới giảm 150k trong khi E7 đã 250k, hệ thống vẫn áp E7 nên không được gợi ý mốc 1 triệu
    const tong = 900000
    const kq = goiYPhieuMoi(danhSach(tong), tong, [dong(900000)], dang(danhSach(tong), 'VCHE7KL4B'))
    expect(kq.ma).toBe('VCHSN26FS')
    expect(kq.canThem).toBe(766674) // 1.666.674 ₫ là mức đầu tiên SN26 (15%) giảm hơn trần 250k của E7
    expect(kq.giam).toBe(250001)
    expect(kq.sanPham.soLuong).toBe(1)
  })

  it('đã là phiếu giảm nhiều nhất (như ảnh: VCHSN26FS giảm 300k): không gợi ý gì', () => {
    const tong = 2100000
    expect(goiYPhieuMoi(danhSach(tong), tong, [dong(2100000)], dang(danhSach(tong), 'VCHSN26FS'))).toBeNull()
  })

  it('phiếu bị chặn vì lý do khác (hết lượt...) thì không gợi ý', () => {
    const ds = [{ ...SN26, apDungDuoc: false, lyDo: 'Phiếu đã hết lượt sử dụng.' }]
    expect(goiYPhieuMoi(ds, 200000, [dong(200000)], null)).toBeNull()
  })

  it('chọn dòng cần ít sản phẩm nhất, cùng số lượng thì lấy dòng rẻ hơn', () => {
    const tong = 200000
    const ds = danhSach(tong, [E7])
    const gio = [dong(50000, 10, 1, 'Quấn cán'), dong(150000, 10, 1, 'Vợt rẻ'), dong(400000, 10, 1, 'Vợt đắt')]
    expect(goiYPhieuMoi(ds, tong, gio, null).sanPham.ten).toBe('Vợt rẻ')
  })

  it('không đủ tồn kho thì bỏ dòng đó; không dòng nào đủ thì chỉ báo số tiền', () => {
    const tong = 200000
    const ds = danhSach(tong, [E7])
    expect(goiYPhieuMoi(ds, tong, [dong(50000, 1, 1, 'Quấn cán'), dong(100000, 1, 1, 'Vợt')], null).sanPham.ten).toBe('Vợt')
    const kq = goiYPhieuMoi(ds, tong, [dong(50000, 1), dong(150000, 0)], null)
    expect(kq.canThem).toBe(100000)
    expect(kq.sanPham).toBeNull()
  })

  it('thêm đúng số sản phẩm đó mà phiếu khác giảm nhiều hơn thì không ghi số sản phẩm', () => {
    // Cần 100k để đạt A; thêm 1 sản phẩm 900k thì tổng 1 triệu, phiếu B giảm nhiều hơn A
    const A = { ma: 'A', loaiGiam: 'TIEN_MAT', giaTri: 10000, donToiThieu: 200000, apDungDuoc: false, canMuaThem: 100000 }
    const B = { ma: 'B', loaiGiam: 'TIEN_MAT', giaTri: 90000, donToiThieu: 1000000, apDungDuoc: false, canMuaThem: 900000 }
    const kq = goiYPhieuMoi([A, B], 100000, [dong(900000)], null)
    expect(kq.ma).toBe('A')
    expect(kq.sanPham).toBeNull()
  })

  it('giỏ trống, chưa có phiếu nào: không gợi ý', () => {
    expect(goiYPhieuMoi([], 0, [], null)).toBeNull()
  })
})
