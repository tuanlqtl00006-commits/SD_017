import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
// Hộp xác nhận dùng chung (21/11): các test ở đây kiểm luồng sau khi người dùng đã bấm Xác nhận; hộp xác nhận kiểm riêng ở xacNhanChung.test.js
vi.mock('../src/composables/useXacNhan', () => ({ hoiXacNhan: vi.fn(async () => true) }))
import { flushPromises, mount } from '@vue/test-utils'

// Góp ý 14/11 (video 22:22 [04:52–08:06]): mã QR chuyển khoản có hạn 5 phút, đếm ngược ngay dưới.
// 24/11 (Task 4): hết hạn chưa nhận tiền thì đơn TỰ HỦY - không gia hạn, không mã QR mới, không gửi lại link.
const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { ma: 'HD777' } }) }))
vi.mock('qrcode', () => ({ default: { toDataURL: async () => 'data:image/png;base64,AA' } }))

import { docDonHang, ghiDonHang } from '../src/utils/donHangLocal'
import OrderSuccessView from '../src/views/client/OrderSuccessView.vue'

const LUC_DAT = new Date('2026-11-15T10:00:00')

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval', 'setTimeout'] })
  vi.setSystemTime(LUC_DAT)
  localStorage.clear()
  ghiDonHang([{ maHoaDon: 'HD777', ngayTao: LUC_DAT.toISOString(), sdtKhachHang: '0901000001', phuongThucThanhToan: 'QR', tongThanhToan: 1_000_000 }])
  api.get.mockReset()
  api.post.mockReset()
  api.get.mockImplementation(async (url) => {
    if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) throw new Error('tắt giả lập')
    if (url === '/cua-hang/thong-tin-thanh-toan') return { data: { nganHang: 'MB', soTaiKhoan: '0123456789', tenChuTaiKhoan: 'FOOTSTYLE DEMO' } }
    if (url === '/cua-hang/tra-cuu') return { data: { trangThai: 1, daThanhToanDu: false, idPhuongThucThanhToan: 2 } }
    return { data: {} }
  })
  api.post.mockResolvedValue({ data: { trangThai: 1, idPhuongThucThanhToan: 1 } })
})
afterEach(() => vi.useRealTimers())

const gan = () => mount(OrderSuccessView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })

describe('Mã QR chuyển khoản có hạn 5 phút', () => {
  it('đang trong hạn: hiện QR và đồng hồ đếm ngược', async () => {
    const w = gan()
    await flushPromises()
    expect(w.find('[data-testid="osv-dem-nguoc"]').text()).toContain('05:00')
    vi.advanceTimersByTime(61_000)
    await flushPromises()
    expect(w.find('[data-testid="osv-dem-nguoc"]').text()).toContain('03:59')
    expect(w.find('img[alt="Mã VietQR thanh toán"]').exists()).toBe(true)
    w.unmount()
  })

  it('đồng hồ về 00:00 chưa có tiền: dừng hỏi máy chủ, ẩn QR, badge "Đã hủy" và báo tự hủy; không có nút chuyển khoản lại / gửi link', async () => {
    const w = gan()
    await flushPromises()
    vi.advanceTimersByTime(5 * 60_000)
    await flushPromises()
    expect(w.find('img[alt="Mã VietQR thanh toán"]').exists()).toBe(false)
    expect(w.find('[data-testid="osv-dem-nguoc"]').exists()).toBe(false)
    expect(w.find('.badge').text()).toBe('Đã hủy')
    expect(w.get('[data-testid="osv-da-huy"]').text()).toContain('đã tự động hủy do quá thời hạn thanh toán 5 phút')
    expect(w.find('[data-testid="osv-chuyen-khoan-lai"]').exists()).toBe(false)
    expect(w.text()).not.toContain('gửi link')
    expect(api.post).not.toHaveBeenCalled()
    // Đã dừng hỏi máy chủ
    const soLanHoi = api.get.mock.calls.filter(([u]) => u === '/cua-hang/tra-cuu').length
    vi.advanceTimersByTime(20_000)
    await flushPromises()
    expect(api.get.mock.calls.filter(([u]) => u === '/cua-hang/tra-cuu').length).toBe(soLanHoi)
    w.unmount()
  })

  it('cửa hàng đã đổi đơn sang COD (nhân viên đổi giúp): trang báo đơn trả tiền khi nhận hàng', async () => {
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) throw new Error('tắt giả lập')
      if (url === '/cua-hang/thong-tin-thanh-toan') return { data: { nganHang: 'MB', soTaiKhoan: '0123456789', tenChuTaiKhoan: 'X' } }
      if (url === '/cua-hang/tra-cuu') return { data: { trangThai: 1, daThanhToanDu: false, idPhuongThucThanhToan: 1 } }
      return { data: {} }
    })
    const w = gan()
    await flushPromises()
    expect(w.find('[data-testid="osv-da-doi-cod"]').exists()).toBe(true)
    expect(docDonHang()[0].maHoaDon).toBe('HD777')
    w.unmount()
  })

  it('F5 trang: đếm ngược theo hạn máy chủ trả về (lúc đặt + 5 phút), không đếm lại từ đầu', async () => {
    vi.setSystemTime(new Date('2026-11-15T10:02:00')) // mở lại trang sau 2 phút
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) throw new Error('tắt giả lập')
      if (url === '/cua-hang/thong-tin-thanh-toan') return { data: { nganHang: 'MB', soTaiKhoan: '0123456789', tenChuTaiKhoan: 'X' } }
      if (url === '/cua-hang/tra-cuu') return { data: { trangThai: 1, daThanhToanDu: false, idPhuongThucThanhToan: 2, hanChuyenKhoan: '2026-11-15T10:05:00' } }
      return { data: {} }
    })
    const w = gan()
    await flushPromises()
    expect(w.find('[data-testid="osv-dem-nguoc"]').text()).toContain('03:00')
    w.unmount()
  })

  it('máy chủ đã tự hủy đơn quá hạn (tuHuyQuaHan): trang báo đã tự động hủy, ẩn QR', async () => {
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) throw new Error('tắt giả lập')
      if (url === '/cua-hang/tra-cuu') return { data: { trangThai: 0, daThanhToanDu: false, idPhuongThucThanhToan: 2, tuHuyQuaHan: true } }
      return { data: {} }
    })
    const w = gan()
    await flushPromises()
    expect(w.find('img[alt="Mã VietQR thanh toán"]').exists()).toBe(false)
    expect(w.find('.badge').text()).toBe('Đã hủy')
    expect(w.text()).toContain('Đơn hàng đã tự động hủy do quá thời hạn thanh toán 5 phút.')
    w.unmount()
  })

  it('cửa hàng hủy đơn: trang báo đơn đã hủy, không nhầm thành hết hàng', async () => {
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) throw new Error('tắt giả lập')
      if (url === '/cua-hang/tra-cuu') return { data: { trangThai: 0, daThanhToanDu: false, idPhuongThucThanhToan: 2 } }
      return { data: {} }
    })
    const w = gan()
    await flushPromises()
    expect(w.find('[data-testid="osv-da-huy"]').exists()).toBe(true)
    expect(w.text()).toContain('Đơn hàng đã hủy')
    expect(w.find('[data-testid="osv-het-hang-chua-tra"]').exists()).toBe(false)
    w.unmount()
  })
})
