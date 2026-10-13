import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { PNG } from 'pngjs'
import jsQR from 'jsqr'
import { docMaQrCccd, ngayDdMMyyyyThanhIso, vietHoaTen } from '../src/utils/cccd'
import { taoMaTheoTen, tienToMaNhanVien } from '../src/constants/nhanVien'
import { taoMa } from '../src/constants/thuocTinh'
import { dinhDangMa, rutGonMa, rutGonMaBienThe, soThuTu } from '../src/utils/ma'
import { chuanHoaTenDonVi, ghepDiaChi } from '../src/utils/diaChi'

const QR_HOP_LE = '001099012345|123456789|NGUYỄN VĂN AN|20051999|Nam|Số 3 ngõ 12, Thôn Cầu, Xã Đông Quý, Huyện Tiền Hải, Tỉnh Thái Bình|15032021'

describe('Đọc mã QR căn cước công dân', () => {
  it('tách đủ thông tin', () => {
    const r = docMaQrCccd(QR_HOP_LE)
    expect(r.soCccd).toBe('001099012345')
    expect(r.soCmnd).toBe('123456789')
    expect(r.hoTen).toBe('Nguyễn Văn An')
    expect(r.ngaySinh).toBe('1999-05-20')
    expect(r.gioiTinh).toBe('Nam')
    expect(r.diaChi).toContain('Tiền Hải')
    expect(r.ngayCap).toBe('2021-03-15')
  })
  it('giới tính Nữ, thiếu CMND cũ, có khoảng trắng thừa', () => {
    const r = docMaQrCccd(' 079203001234||TRẦN  THỊ  MAI|01012003|Nữ|Quận 1, TP.HCM|01012021 \n')
    expect(r.gioiTinh).toBe('Nữ')
    expect(r.soCmnd).toBe('')
    expect(r.hoTen).toBe('Trần Thị Mai')
    expect(r.ngaySinh).toBe('2003-01-01')
  })
  it('thẻ bỏ hẳn ô CMND cũ (họ tên ở vị trí thứ 2) vẫn đọc đúng', () => {
    const r = docMaQrCccd('079203001234|TRẦN THỊ MAI|01012003|Nữ|Quận 1, TP.HCM|01012021')
    expect(r.hoTen).toBe('Trần Thị Mai')
    expect(r.ngaySinh).toBe('2003-01-01')
    expect(r.gioiTinh).toBe('Nữ')
    expect(r.soCmnd).toBe('')
    expect(r.diaChi).toBe('Quận 1, TP.HCM')
    expect(r.ngayCap).toBe('2021-01-01')
  })
  it('ngày sinh không có thật -> rỗng', () => {
    expect(ngayDdMMyyyyThanhIso('31022000')).toBe('')
    expect(ngayDdMMyyyyThanhIso('abc')).toBe('')
    expect(docMaQrCccd('001099012345||A B|99|Nam|x|y').ngaySinh).toBe('')
  })
  it('báo lỗi tiếng Việt khi không phải QR căn cước', () => {
    expect(() => docMaQrCccd('https://example.com')).toThrow(/không phải mã QR/)
    expect(() => docMaQrCccd('12345|a|b|c|d')).toThrow(/12 chữ số/)
    expect(() => docMaQrCccd('')).toThrow(/Chưa có nội dung/)
    expect(() => docMaQrCccd('001099012345|x')).toThrow(/không phải mã QR/)
  })
  it('viết hoa tên chuẩn tiếng Việt', () => {
    expect(vietHoaTen('ĐẶNG VĂN ĐỨC')).toBe('Đặng Văn Đức')
    expect(vietHoaTen('  lê   quốc bảo ')).toBe('Lê Quốc Bảo')
  })

  // Ảnh QR thật (tạo bằng thư viện qrcode) -> giải mã bằng jsQR -> tách thông tin
  const giaiMa = (file) => {
    const png = PNG.sync.read(readFileSync(resolve(process.cwd(), 'tests/fixtures', file)))
    return jsQR(new Uint8ClampedArray(png.data), png.width, png.height, { inversionAttempts: 'attemptBoth' })
  }
  it('ảnh QR căn cước thật -> giải mã được và đọc đúng thông tin', () => {
    const kq = giaiMa('cccd-qr-mau.png')
    expect(kq?.data).toBe(QR_HOP_LE)
    expect(docMaQrCccd(kq.data).hoTen).toBe('Nguyễn Văn An')
  })
  it('ảnh thẻ có QR nhỏ trên nền lớn -> vẫn giải mã được', () => {
    expect(giaiMa('cccd-the-nho.png')?.data).toBe(QR_HOP_LE)
  })
  it('QR khác (không phải căn cước) -> giải mã được nhưng bị từ chối', () => {
    const kq = giaiMa('qr-khac.png')
    expect(kq?.data).toContain('example.com')
    expect(() => docMaQrCccd(kq.data)).toThrow(/không phải mã QR/)
  })
})

describe('Mã nhân viên: chữ cái đầu của họ tên + số thứ tự tối thiểu 2 chữ số (khớp MaNhanVienUtil ở backend)', () => {
  const ca = [
    ['Lê Quốc Tuấn', 'LQT'],
    ['Nguyễn Văn An', 'NVA'],
    ['trần thị mai lan', 'TTML'],
    ['Đặng Văn Đức', 'DVD'],
    ['Lan', 'L'],
    ['  Lê   Quốc  Bảo  ', 'LQB'],
    ['ĐỖ THÙY TRANG', 'DTT'],
    ['', ''],
    ['   ', ''],
    ['123 ???', ''],
  ]
  it.each(ca)('tiền tố của "%s" là "%s"', (ten, tienTo) => {
    expect(tienToMaNhanVien(ten)).toBe(tienTo)
  })
  it('Lê Quốc Tuấn -> LQT01, người trùng chữ cái đầu -> LQT02', () => {
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['NV01', 'NV02'])).toBe('LQT01')
    expect(taoMaTheoTen('Lê Quang Tú', ['LQT01', 'NV01'])).toBe('LQT02')
    expect(taoMaTheoTen('Lê Quốc Tuấn', [])).toBe('LQT01')
    expect(taoMaTheoTen('Nguyễn Văn An', [])).toBe('NVA01')
    expect(taoMaTheoTen('Trần Thị Mai Lan', [])).toBe('TTML01')
    expect(taoMaTheoTen('Lan', [])).toBe('L01')
  })
  it('đánh số thứ tự không trùng, không phân biệt hoa thường, bỏ qua mã khác dạng', () => {
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQT01', 'LQT03'])).toBe('LQT04')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['lqt02'])).toBe('LQT03')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQTA01'])).toBe('LQT01')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQTX05', 'LQT', null, undefined])).toBe('LQT01')
  })
  it('mã mẫu NV01..NV10 giữ nguyên; người có chữ cái đầu N-V đánh số tiếp, không trùng mã mẫu', () => {
    expect(taoMaTheoTen('Ngô Vinh', ['NV01', 'NV02', 'NV10'])).toBe('NV11')
  })
  it('đọc được cả mã cũ nhiều số 0 (LQT0001, NV0010) nhưng mã mới luôn gọn', () => {
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQT0001'])).toBe('LQT02')
    expect(taoMaTheoTen('Ngô Vinh', ['NV0001', 'NV0010'])).toBe('NV11')
  })
  it('tên không có chữ cái thì dùng tiền tố dự phòng NV; dưới 10 có 1 số 0, quá 99 viết thẳng', () => {
    expect(taoMaTheoTen('123', [])).toBe('NV01')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQT08'])).toBe('LQT09')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQT09'])).toBe('LQT10')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQT99'])).toBe('LQT100')
    expect(taoMaTheoTen('Lê Quốc Tuấn', ['LQT999'])).toBe('LQT1000')
  })
})

describe('Mã tự cấp dùng chung: tiền tố + số thứ tự tối thiểu 2 chữ số (HD01 ... HD99, HD100)', () => {
  it('soThuTu / dinhDangMa: không đệm dài, quá 99 thì viết thẳng', () => {
    expect(soThuTu(1)).toBe('01')
    expect(soThuTu(9)).toBe('09')
    expect(soThuTu(10)).toBe('10')
    expect(soThuTu(99)).toBe('99')
    expect(soThuTu(100)).toBe('100')
    expect(dinhDangMa('HD', 1)).toBe('HD01')
    expect(dinhDangMa('HD', 18)).toBe('HD18')
    expect(dinhDangMa('HD', 100)).toBe('HD100')
    expect(dinhDangMa('HD', 1000)).toBe('HD1000')
    expect(dinhDangMa('DGG', 9)).toBe('DGG09')
  })
  it('rutGonMa: HD000001 -> HD01, NV0010 -> NV10, DC000 -> DC00; mã đã gọn / mã khác dạng giữ nguyên', () => {
    expect(rutGonMa('HD000001')).toBe('HD01')
    expect(rutGonMa('HD000100')).toBe('HD100')
    expect(rutGonMa('NV0010')).toBe('NV10')
    expect(rutGonMa('SP001')).toBe('SP01')
    expect(rutGonMa('DGG009')).toBe('DGG09')
    expect(rutGonMa('DC000')).toBe('DC00')
    expect(rutGonMa('HD01')).toBe('HD01')
    expect(rutGonMa('HD100')).toBe('HD100')
    expect(rutGonMa(rutGonMa('HD000001'))).toBe('HD01') // chạy lại không đổi thêm
    expect(rutGonMa('VCH7QX2YN')).toBe('VCH7QX2YN') // mã phiếu giảm giá nhập tay
    expect(rutGonMa('SP001-DEN-4U-G5')).toBe('SP001-DEN-4U-G5')
    expect(rutGonMa(null)).toBe(null)
  })
  it('rutGonMaBienThe: chỉ rút gọn phần mã sản phẩm ở đầu', () => {
    expect(rutGonMaBienThe('SP001-DEN-4U-G5')).toBe('SP01-DEN-4U-G5')
    expect(rutGonMaBienThe('SP012-DEN-4U-G5-2')).toBe('SP12-DEN-4U-G5-2')
    expect(rutGonMaBienThe('SP01-DEN-4U-G5')).toBe('SP01-DEN-4U-G5')
  })
  it('mã thuộc tính (DM, TH, CB...) cũng gọn: DM05 -> DM06, DM99 -> DM100, đọc được mã cũ DM001', () => {
    expect(taoMa('DM', [])).toBe('DM01')
    expect(taoMa('DM', ['DM05'])).toBe('DM06')
    expect(taoMa('DM', ['DM99'])).toBe('DM100')
    expect(taoMa('DM', ['DM001', 'DM003'])).toBe('DM04')
  })
})

describe('Địa chỉ hành chính', () => {
  it('so khớp tên đơn vị, bỏ dấu và tiền tố', () => {
    expect(chuanHoaTenDonVi('Thành phố Hà Nội')).toBe('ha noi')
    expect(chuanHoaTenDonVi('Tỉnh Hưng Yên')).toBe('hung yen')
    expect(chuanHoaTenDonVi('Thành phố Đà Nẵng')).toBe('da nang')
  })
  it('ghép địa chỉ, bỏ phần trống (không còn quận/huyện)', () => {
    expect(ghepDiaChi({ diaChiCuThe: 'Số 1', phuongXa: 'Phường A', tinhThanh: 'Thành phố B' })).toBe('Số 1, Phường A, Thành phố B')
    expect(ghepDiaChi({ diaChiCuThe: 'Số 1', phuongXa: 'Phường A', quanHuyen: '', tinhThanhPho: 'Thành phố B' })).toBe('Số 1, Phường A, Thành phố B')
    expect(ghepDiaChi({ diaChiCuThe: 'Số 1', phuongXa: 'Phường A', quanHuyen: 'Quận C', tinhThanhPho: 'TP B' })).toBe('Số 1, Phường A, Quận C, TP B')
    expect(ghepDiaChi(null)).toBe('')
  })
})
