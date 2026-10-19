import { describe, it, expect } from 'vitest'
import { trichXuatMaHoaDon } from '../src/utils/qrHoaDon'

describe('qrHoaDon - trichXuatMaHoaDon', () => {
  it('giữ nguyên mã hóa đơn dạng chuỗi thuần túy', () => {
    expect(trichXuatMaHoaDon('HD16')).toBe('HD16')
    expect(trichXuatMaHoaDon('hd001')).toBe('hd001')
    expect(trichXuatMaHoaDon('12345')).toBe('12345')
  })

  it('loại bỏ ký tự khoảng trắng thừa và BOM', () => {
    expect(trichXuatMaHoaDon('\uFEFF  HD99  ')).toBe('HD99')
  })

  it('trích xuất mã từ đường dẫn URL chi tiết hóa đơn', () => {
    expect(trichXuatMaHoaDon('http://localhost:5173/hoa-don/16')).toBe('16')
    expect(trichXuatMaHoaDon('https://footstyle.vn/hoa-don/HD100')).toBe('HD100')
    expect(trichXuatMaHoaDon('/hoa-don/HD25')).toBe('HD25')
  })

  it('trích xuất mã từ URL có tham số truy vấn', () => {
    expect(trichXuatMaHoaDon('http://localhost:5173/hoa-don?ma=HD18')).toBe('HD18')
    expect(trichXuatMaHoaDon('http://localhost:5173/hoa-don?maHoaDon=HD22&tab=1')).toBe('HD22')
  })

  it('trích xuất mã từ chuỗi có tiền tố ghi chú', () => {
    expect(trichXuatMaHoaDon('Mã HD: HD16')).toBe('HD16')
    expect(trichXuatMaHoaDon('Mã: HD88')).toBe('HD88')
    expect(trichXuatMaHoaDon('Mã hóa đơn: HD77')).toBe('HD77')
  })

  it('tìm thấy mã hóa đơn có tiền tố HD trong đoạn văn bản', () => {
    expect(trichXuatMaHoaDon('Đơn hàng HD99 của khách')).toBe('HD99')
  })

  it('trả về rỗng khi chuỗi rỗng hoặc không hợp lệ', () => {
    expect(trichXuatMaHoaDon('')).toBe('')
    expect(trichXuatMaHoaDon(null)).toBe('')
    expect(trichXuatMaHoaDon(undefined)).toBe('')
  })
})
