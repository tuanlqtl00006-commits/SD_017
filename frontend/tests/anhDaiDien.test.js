import { describe, expect, it } from 'vitest'
import { loiFileAnhDaiDien } from '../src/utils/anhDaiDien'

const file = (type, size = 10) => {
  const f = new File(['x'], 'a', { type })
  Object.defineProperty(f, 'size', { value: size })
  return f
}

describe('loiFileAnhDaiDien', () => {
  it('nhận JPG, PNG, WEBP, GIF', () => {
    for (const t of ['image/jpeg', 'image/png', 'image/webp', 'image/gif']) expect(loiFileAnhDaiDien(file(t))).toBe('')
  })
  it('từ chối định dạng khác (pdf, svg, bmp, rỗng)', () => {
    for (const t of ['application/pdf', 'image/svg+xml', 'image/bmp', '']) expect(loiFileAnhDaiDien(file(t))).toBe('Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.')
  })
  it('đúng 5 MB còn được, hơn 5 MB bị chặn', () => {
    expect(loiFileAnhDaiDien(file('image/png', 5 * 1024 * 1024))).toBe('')
    expect(loiFileAnhDaiDien(file('image/png', 5 * 1024 * 1024 + 1))).toBe('Ảnh lớn hơn 5 MB.')
  })
  it('không có file thì báo chưa chọn', () => {
    expect(loiFileAnhDaiDien(null)).toBe('Chưa chọn ảnh.')
  })
})
