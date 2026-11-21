import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
// Hộp xác nhận dùng chung (21/11): các test ở đây kiểm luồng sau khi người dùng đã bấm Xác nhận; hộp xác nhận kiểm riêng ở xacNhanChung.test.js
vi.mock('../src/composables/useXacNhan', () => ({ hoiXacNhan: vi.fn(async () => true) }))
import { flushPromises, mount } from '@vue/test-utils'

// Góp ý 14/11 (video 22:22 [04:52–08:06]): mã QR chuyển khoản có hạn 5 phút, đếm ngược ngay dưới;
// quá hạn chưa nhận tiền thì đơn tạm treo, ẩn QR, khách bấm đổi sang thanh toán khi nhận hàng (COD).
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

  it('hết 5 phút chưa có tiền: ẩn QR, báo đơn treo; bấm đổi sang COD thì gọi máy chủ kèm SĐT và lưu lại trên máy', async () => {
    const w = gan()
    await flushPromises()
    vi.advanceTimersByTime(5 * 60_000)
    await flushPromises()
    expect(w.find('[data-testid="osv-het-han"]').exists()).toBe(true)
    expect(w.find('img[alt="Mã VietQR thanh toán"]').exists()).toBe(false)
    expect(w.text()).toContain('Đơn treo - chưa thanh toán')
    await w.find('[data-testid="osv-doi-cod"]').trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/cua-hang/don-hang/HD777/doi-sang-cod', { sdt: '0901000001' })
    expect(w.find('[data-testid="osv-da-doi-cod"]').exists()).toBe(true)
    expect(docDonHang()[0].phuongThucThanhToan).toBe('COD')
    w.unmount()
  })
})
