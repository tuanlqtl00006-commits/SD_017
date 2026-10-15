import { describe, expect, it } from 'vitest'
import {
  chuanHoaEmail,
  chuanHoaKhoangTrang,
  chuanHoaSdt,
  gioiHanNgaySinh,
  kiemTraDiaChiCuThe,
  kiemTraEmail,
  kiemTraHoTen,
  kiemTraNgaySinh,
  kiemTraPhuongXa,
  kiemTraSdt,
  kiemTraTinhThanh,
} from '../src/utils/khachHangValidate'

// Ngày cố định để test không phụ thuộc hôm nay là ngày nào
const HOM_NAY = new Date(2026, 9, 15) // 15/10/2026

describe('Họ tên', () => {
  it('hợp lệ, kể cả tiếng Việt có dấu', () => {
    expect(kiemTraHoTen('Nguyễn Văn An')).toBe('')
    expect(kiemTraHoTen('Lê')).toBe('') // đúng 2 ký tự
    expect(kiemTraHoTen('Đặng Thị Ánh Nguyệt')).toBe('')
    expect(kiemTraHoTen('A'.repeat(100))).toBe('') // biên trên
  })
  it('nhiều khoảng trắng thừa vẫn hợp lệ và được gộp lại', () => {
    expect(kiemTraHoTen('   Nguyễn    Văn   An  ')).toBe('')
    expect(chuanHoaKhoangTrang('   Nguyễn    Văn \t An  ')).toBe('Nguyễn Văn An')
  })
  it('chữ có dấu dạng tổ hợp (copy từ Word / Mac) được đưa về dạng chuẩn', () => {
    expect(chuanHoaKhoangTrang('Nguye\u0302\u0303n')).toBe('Nguyễn')
    expect(kiemTraHoTen('Nguye\u0302\u0303n Van An')).toBe('')
  })
  it('trống hoặc toàn khoảng trắng', () => {
    expect(kiemTraHoTen('')).toMatch('Vui lòng nhập họ tên')
    expect(kiemTraHoTen('     ')).toMatch('Vui lòng nhập họ tên')
    expect(kiemTraHoTen(null)).toMatch('Vui lòng nhập họ tên')
  })
  it('quá ngắn / quá dài', () => {
    expect(kiemTraHoTen('A')).toMatch('từ 2 đến 100')
    expect(kiemTraHoTen('A'.repeat(101))).toMatch('từ 2 đến 100')
  })
  it('chứa số hoặc ký tự đặc biệt', () => {
    for (const s of ['Nguyen123', 'Nguyễn @n', '<script>', 'Nguyễn_Văn', 'An.']) {
      expect(kiemTraHoTen(s)).toMatch('chỉ được chứa chữ cái')
    }
  })
  it('thông báo dùng đúng tên ô', () => {
    expect(kiemTraHoTen('', 'Tên người nhận')).toBe('Vui lòng nhập tên người nhận.')
  })
})

describe('Số điện thoại', () => {
  it('đủ các đầu số di động', () => {
    for (const s of ['0312345678', '0512345678', '0712345678', '0812345678', '0912345678']) {
      expect(kiemTraSdt(s)).toBe('')
    }
  })
  it('tự bỏ khoảng trắng, dấu chấm, gạch ngang; đổi +84 / 84 thành 0', () => {
    expect(chuanHoaSdt(' 090 123 4567 ')).toBe('0901234567')
    expect(chuanHoaSdt('090.123.4567')).toBe('0901234567')
    expect(chuanHoaSdt('090-123-4567')).toBe('0901234567')
    expect(chuanHoaSdt('+84 90 123 4567')).toBe('0901234567')
    expect(chuanHoaSdt('84901234567')).toBe('0901234567')
    expect(kiemTraSdt('+84901234567')).toBe('')
  })
  it('không hợp lệ', () => {
    expect(kiemTraSdt('')).toMatch('Vui lòng nhập')
    for (const s of ['0123456789', '0212345678', '0000000000', '090123456', '09012345678', '09o1234567', '9012345678']) {
      expect(kiemTraSdt(s)).toMatch('không hợp lệ')
    }
  })
})

describe('Email', () => {
  it('hợp lệ', () => {
    for (const s of ['an.nguyen@gmail.com', 'AN@Gmail.COM', 'a_b+tag@sv.fpt.edu.vn', 'x@my-shop.vn']) {
      expect(kiemTraEmail(s)).toBe('')
    }
  })
  it('lưu chữ thường để so trùng không phân biệt hoa thường', () => {
    expect(chuanHoaEmail('  AN@Gmail.COM ')).toBe('an@gmail.com')
  })
  it('bắt buộc', () => {
    expect(kiemTraEmail('')).toBe('Vui lòng nhập email.')
    expect(kiemTraEmail('   ')).toBe('Vui lòng nhập email.')
  })
  it('sai định dạng', () => {
    const sai = ['abc', 'abc@', '@gmail.com', 'a@b', 'a@b.c', 'a@@gmail.com', 'a b@gmail.com', 'a..b@gmail.com',
      '.a@gmail.com', 'a.@gmail.com', 'a@gmail..com', 'a@-gmail.com', 'a@gmail.com.', 'nguyễn@gmail.com']
    for (const s of sai) expect(kiemTraEmail(s), s).toMatch('không đúng định dạng')
  })
  it('quá 100 ký tự', () => {
    expect(kiemTraEmail('a'.repeat(95) + '@g.com')).toMatch('tối đa 100')
  })
})

describe('Ngày sinh', () => {
  it('không bắt buộc', () => {
    expect(kiemTraNgaySinh('', HOM_NAY)).toBe('')
  })
  it('biên tuổi 10 và 100', () => {
    expect(kiemTraNgaySinh('2016-10-15', HOM_NAY)).toBe('') // tròn 10 tuổi hôm nay
    expect(kiemTraNgaySinh('2016-10-16', HOM_NAY)).toMatch('từ 10 tuổi') // thiếu 1 ngày
    expect(kiemTraNgaySinh('1926-10-15', HOM_NAY)).toBe('') // đúng 100 tuổi
    expect(kiemTraNgaySinh('1925-10-16', HOM_NAY)).toBe('') // 100 tuổi 364 ngày
    expect(kiemTraNgaySinh('1925-10-15', HOM_NAY)).toMatch('không quá 100') // tròn 101
    expect(kiemTraNgaySinh('1800-01-01', HOM_NAY)).toMatch('không quá 100')
  })
  it('tương lai, hôm nay', () => {
    expect(kiemTraNgaySinh('2026-10-16', HOM_NAY)).toMatch('tương lai')
    expect(kiemTraNgaySinh('2026-10-15', HOM_NAY)).toMatch('từ 10 tuổi')
  })
  it('ngày không tồn tại / năm quá 4 chữ số', () => {
    expect(kiemTraNgaySinh('2001-02-30', HOM_NAY)).toBe('Ngày sinh không tồn tại.')
    expect(kiemTraNgaySinh('2003-02-29', HOM_NAY)).toBe('Ngày sinh không tồn tại.') // 2003 không nhuận
    expect(kiemTraNgaySinh('02001-01-01', HOM_NAY)).toMatch('năm phải đủ 4 chữ số')
  })
  it('sinh 29/02 năm nhuận', () => {
    expect(kiemTraNgaySinh('2016-02-29', new Date(2026, 1, 28))).toMatch('từ 10 tuổi')
    expect(kiemTraNgaySinh('2016-02-29', new Date(2026, 2, 1))).toBe('')
  })
  it('giới hạn ô chọn ngày khớp với khoảng tuổi', () => {
    const { min, max } = gioiHanNgaySinh(HOM_NAY)
    expect(max).toBe('2016-10-15')
    expect(min).toBe('1925-10-16')
    expect(kiemTraNgaySinh(max, HOM_NAY)).toBe('')
    expect(kiemTraNgaySinh(min, HOM_NAY)).toBe('')
  })
})

describe('Địa chỉ', () => {
  it('tỉnh / phường bắt buộc', () => {
    expect(kiemTraTinhThanh('')).toBe('Vui lòng chọn Tỉnh/Thành phố.')
    expect(kiemTraPhuongXa('  ')).toBe('Vui lòng chọn Phường/Xã.')
    expect(kiemTraTinhThanh('Thành phố Hà Nội')).toBe('')
  })
  it('địa chỉ cụ thể', () => {
    expect(kiemTraDiaChiCuThe('Số 12 ngõ 5 Cầu Giấy')).toBe('')
    expect(kiemTraDiaChiCuThe('12/3A')).toBe('') // đúng 5 ký tự
    expect(kiemTraDiaChiCuThe('     ')).toMatch('Vui lòng nhập địa chỉ cụ thể')
    expect(kiemTraDiaChiCuThe('aaa')).toMatch('từ 5 đến 255')
    expect(kiemTraDiaChiCuThe('x'.repeat(256))).toMatch('từ 5 đến 255')
    expect(kiemTraDiaChiCuThe('..., ---')).toMatch('phải có chữ hoặc số')
  })
})
