import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { tinhTrangThai, daKetThuc } from '../src/constants/dotGiamGia'
import { useToast } from '../src/composables/useToast'

// api giả: mọi lời gọi đi qua mock.api (đặt lại ở mỗi test)
let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

beforeEach(() => {
  document.body.innerHTML = ''
  useToast().toasts.splice(0)
})
afterEach(() => vi.useRealTimers())

function gia(y, m, d) {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(y, m - 1, d, 10, 0, 0))
}

/** Đợt 4/10 -> 11/10/2026. trangThai: 1 = công tắc bật, 0 = tắt. */
const dot = (over = {}) => ({
  id: 1, maDot: 'DGG004', tenDot: 'Tuần lễ vàng', phanTramGiamDot: 12,
  ngayBatDau: '2026-10-04T00:00:00', ngayKetThuc: '2026-10-11T23:59:59', moTa: '', trangThai: 1, ...over,
})

describe('Trạng thái đợt giảm giá (hàm thuần)', () => {
  it('đã quá ngày kết thúc: công tắc bật = "Đã kết thúc", công tắc tắt = "Ngừng hoạt động"', () => {
    expect(daKetThuc(dot(), '2026-10-12')).toBe(true)
    expect(daKetThuc(dot(), '2026-10-11')).toBe(false)
    expect(tinhTrangThai(dot({ trangThai: 1 }), '2026-10-12')).toBe('DA_KET_THUC')
    expect(tinhTrangThai(dot({ trangThai: 0 }), '2026-10-12')).toBe('NGUNG_HOAT_DONG')
  })
  it('còn hạn: sắp diễn ra / đang diễn ra / ngừng hoạt động', () => {
    expect(tinhTrangThai(dot(), '2026-10-03')).toBe('SAP_DIEN_RA')
    expect(tinhTrangThai(dot(), '2026-10-08')).toBe('DANG_DIEN_RA')
    expect(tinhTrangThai(dot({ trangThai: 0 }), '2026-10-08')).toBe('NGUNG_HOAT_DONG')
  })
})

describe('Danh sách đợt giảm giá: công tắc tắt và khóa ở đợt đã kết thúc', () => {
  async function montarDanhSach(ngay, ds) {
    gia(...ngay)
    mock = { api: { get: vi.fn(async () => ({ data: ds })) } }
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<div/>' } }] })
    router.push('/dot-giam-gia'); await router.isReady()
    const Comp = (await import('../src/components/DotGiamGiaManager.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const dong = (w, ma) => w.findAll('tbody tr').find((tr) => tr.text().includes(ma))
  const DS = () => [dot({ id: 1, maDot: 'DGG007' }), dot({ id: 2, maDot: 'DGG006', trangThai: 0 })]

  it('đợt đã kết thúc: công tắc TẮT và bị khóa (cả khi dữ liệu còn bật); nhãn vẫn là "Đã kết thúc" / "Ngừng hoạt động"', async () => {
    const w = await montarDanhSach([2026, 10, 12], DS())
    const bat = dong(w, 'DGG007')
    const tat = dong(w, 'DGG006')
    const swBat = bat.find('button[role="switch"]')
    const swTat = tat.find('button[role="switch"]')
    expect(swBat.exists()).toBe(true)
    expect(swTat.exists()).toBe(true)
    expect(swBat.attributes('aria-checked')).toBe('false')
    expect(swBat.classes()).not.toContain('is-on')
    expect(swBat.attributes('disabled')).toBeDefined()
    expect(swBat.attributes('title')).toContain('Đã kết thúc')
    expect(bat.text()).toContain('Đã kết thúc')
    expect(swTat.attributes('aria-checked')).toBe('false')
    expect(swTat.attributes('disabled')).toBeDefined()
    expect(tat.text()).toContain('Ngừng hoạt động')
    expect(tat.text()).not.toContain('Đã kết thúc')
    w.unmount()
  })

  it('bấm công tắc ở đợt đã kết thúc: không hiện hộp xác nhận, không gọi API', async () => {
    const w = await montarDanhSach([2026, 10, 12], DS())
    mock.api.put = vi.fn(async () => ({ data: {} }))
    await dong(w, 'DGG007').find('button[role="switch"]').trigger('click')
    await flushPromises()
    expect(document.body.querySelector('.modal, [role="dialog"]')).toBeNull()
    expect(mock.api.put).not.toHaveBeenCalled()
    expect(dong(w, 'DGG007').text()).toContain('Đã kết thúc')
    w.unmount()
  })

  it('đợt chưa kết thúc: công tắc vẫn dùng bình thường (ngày 11/10 là ngày cuối nên còn BẬT)', async () => {
    const w = await montarDanhSach([2026, 10, 11], [dot({ id: 1, maDot: 'DGG007' }), dot({ id: 2, maDot: 'DGG006', trangThai: 0 })])
    const sw = dong(w, 'DGG007').find('button[role="switch"]')
    expect(sw.attributes('aria-checked')).toBe('true')
    expect(sw.attributes('disabled')).toBeUndefined()
    expect(dong(w, 'DGG007').text()).toContain('Đang diễn ra')
    expect(dong(w, 'DGG006').find('button[role="switch"]').attributes('aria-checked')).toBe('false')
    expect(dong(w, 'DGG006').find('button[role="switch"]').attributes('disabled')).toBeUndefined()
    mock.api.put = vi.fn(async () => ({ data: dot({ id: 1, maDot: 'DGG007', trangThai: 0 }) }))
    mock.api.get = vi.fn(async () => ({ data: [dot({ id: 1, maDot: 'DGG007', trangThai: 0 }), dot({ id: 2, maDot: 'DGG006', trangThai: 0 })] }))
    await sw.trigger('click'); await flushPromises()
    const nut = [...document.body.querySelectorAll('.modal button, [role="dialog"] button')].find((b) => /ngừng/i.test(b.textContent))
    expect(nut, 'hộp xác nhận hiện ra').toBeTruthy()
    nut.click(); await flushPromises()
    expect(mock.api.put.mock.calls[0][0]).toBe('/dot-giam-gia/1/trang-thai')
    expect(dong(w, 'DGG007').text()).toContain('Ngừng hoạt động')
    w.unmount()
  })
})
