import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { PNG } from 'pngjs'
import jsQR from 'jsqr'
import { docMaQrCccd, ngayDdMMyyyyThanhIso, vietHoaTen } from '../src/utils/cccd'
import { taoMaTheoTen, tienToMaNhanVien } from '../src/constants/nhanVien'
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
  it('mã QR không có ô CMND cũ: họ tên nằm ngay sau số CCCD', () => {
    const r = docMaQrCccd('079203001234|TRẦN THỊ MAI|01012003|Nữ|Quận 1, TP.HCM|01012021')
    expect(r.hoTen).toBe('Trần Thị Mai')
    expect(r.ngaySinh).toBe('2003-01-01')
    expect(r.gioiTinh).toBe('Nữ')
    expect(r.diaChi).toBe('Quận 1, TP.HCM')
    expect(r.ngayCap).toBe('2021-01-01')
    expect(r.soCmnd).toBe('')
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

describe('Mã nhân viên theo họ tên đầy đủ (khớp MaNhanVienUtil ở backend)', () => {
  const ca = [
    ['Nguyễn Văn An', 'AnNV'],
    ['trần thị mai lan', 'LanTTM'],
    ['Đặng Văn Đức', 'DucDV'],
    ['Lan', 'Lan'],
    ['  Lê   Quốc  Bảo  ', 'BaoLQ'],
    ['ĐỖ THÙY TRANG', 'TrangDT'],
    ['', ''],
    ['   ', ''],
  ]
  it.each(ca)('tiền tố của "%s" là "%s"', (ten, tienTo) => {
    expect(tienToMaNhanVien(ten)).toBe(tienTo)
  })
  it('đánh số thứ tự không trùng', () => {
    expect(taoMaTheoTen('Nguyễn Văn An', ['NV0001', 'NV0002'])).toBe('AnNV01')
    expect(taoMaTheoTen('Ngô Văn An', ['AnNV01', 'AnNV02', 'NV0001'])).toBe('AnNV03')
    expect(taoMaTheoTen('Nguyễn Văn An', ['annv02'])).toBe('AnNV03')
    expect(taoMaTheoTen('Nguyễn Văn An', ['AnNVX05'])).toBe('AnNV01')
    expect(taoMaTheoTen('123', [])).toBe('NV01')
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
