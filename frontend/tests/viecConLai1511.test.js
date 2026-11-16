import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

// Việc còn lại 15/11 (VIEC_CON_LAI_1511.md):
//  - ô mã giảm giá ở trang thanh toán hiện mã đang áp (cô review 14/11 [26:19]);
//  - tab Đơn tại quầy ghi rõ hình thức mua (R1 [11:32]);
//  - màn quầy KHÔNG hiện số "chờ đơn online" (thêm 15/11 theo case khó R1, góp ý 16/11 bảo bỏ).
let mockApi
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mockApi[k](...a) }) }))
vi.mock('../src/services/phien', () => ({ layPhien: () => null }))

import VoucherPanel from '../src/components/shop/VoucherPanel.vue'
import TimSanPham from '../src/components/banHang/TimSanPham.vue'
import { useCartStore } from '../src/stores/cart'
import { useToast } from '../src/composables/useToast'

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  sessionStorage.clear()
  setActivePinia(createPinia())
  mockApi = { get: vi.fn(async () => ({ data: [] })), post: vi.fn(async () => ({ data: null })) }
})

describe('Ô mã giảm giá hiện mã đang áp', () => {
  it('đã áp mã thì hiện "Đang áp dụng mã ..." ngay trên ô nhập, bấm X để bỏ', async () => {
    const cart = useCartStore()
    cart.items.push({ bienTheId: 1, sanPhamTen: 'Vợt', giaBan: 1_000_000, giaGoc: 1_000_000, soLuong: 1, soLuongTon: 5 })
    cart.setVoucher({ ma: 'GIAM50K', ten: 'Giảm 50k', loaiGiam: 2, giaTriGiam: 50_000, donToiThieu: 0 })
    const w = mount(VoucherPanel, { attachTo: document.body })
    await flushPromises()
    const chip = w.find('[data-testid="vp-dang-ap"]')
    expect(chip.text()).toContain('Đang áp dụng mã GIAM50K')
    expect(chip.text()).toContain('50.000')
    await chip.find('button').trigger('click')
    expect(cart.appliedVoucher).toBeNull()
    expect(w.find('[data-testid="vp-dang-ap"]').exists()).toBe(false)
    w.unmount()
  })
})

describe('Tab Đơn tại quầy: cột Hình thức mua', () => {
  it('đơn mua trực tiếp và đơn quầy có giao hàng ghi khác nhau; tab online vẫn là Loại đơn', async () => {
    mockApi.get = vi.fn(async (url) => {
      if (url === '/hoa-don') {
        return { data: { content: [
          { id: 1, maHoaDon: 'HD001', loaiDon: 'Tại quầy', hinhThucMua: 'Mua tại quầy', trangThai: 6, tongTien: 100, ngayTao: '2026-11-15T09:00:00' },
          { id: 2, maHoaDon: 'HD002', loaiDon: 'Tại quầy', hinhThucMua: 'Giao tận nơi', trangThai: 3, tongTien: 200, ngayTao: '2026-11-15T09:00:00' },
        ], totalElements: 2 } }
      }
      return { data: {} }
    })
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/hoa-don', component: { template: '<div/>' } }, { path: '/hoa-don/:id', component: { template: '<div/>' } }] })
    router.push('/hoa-don')
    await router.isReady()
    const HoaDonManager = (await import('../src/components/HoaDonManager.vue')).default
    const w = mount(HoaDonManager, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    expect(w.text()).toContain('Hình thức mua')
    const o = w.findAll('[data-testid="hd-hinh-thuc"]').map((x) => x.text())
    expect(o).toEqual(['Mua tại quầy', 'Giao tận nơi'])
    w.unmount()
  })
})

describe('Màn quầy: không hiện số "chờ đơn online" (góp ý 16/11 [04:20]: bỏ hết)', () => {
  const SP = { id: 52, tenSanPham: 'Vợt Yonex Astrox 99', maSpct: 'SP15-XD-3U', tonKho: 20, giaBan: 4_000_000, giaGoc: 4_000_000, anh: '' }

  it('dưới tồn kho chỉ có số tồn, không gọi API chờ đơn online; nhập quá tồn kho vẫn báo "Chỉ còn N sản phẩm trong kho"', async () => {
    mockApi.get = vi.fn(async (url) => {
      if (url === '/ban-hang/san-pham/loc') return { data: { items: [SP], tongSo: 1, trang: 0, soTrang: 1 } }
      return { data: {} }
    })
    const w = mount(TimSanPham, { props: { trongHop: true }, attachTo: document.body })
    await flushPromises()
    expect(mockApi.get.mock.calls.map((c) => c[0])).not.toContain('/ban-hang/san-pham/dang-cho-online')
    expect(w.text()).not.toContain('chờ đơn online')
    const o = w.find('input[aria-label="Số lượng Vợt Yonex Astrox 99"]')
    await o.setValue('21')
    await o.trigger('change')
    await w.find('.bh-nut-them').trigger('click')
    expect(w.emitted('chon')).toBeUndefined()
    expect(w.text()).toContain('Chỉ còn 20 sản phẩm trong kho.')
    await o.setValue('5')
    await o.trigger('change')
    await w.find('.bh-nut-them').trigger('click')
    expect(w.emitted('chon')?.[0]?.[2]).toBe(5)
    expect(useToast().toasts.length).toBe(0)
    w.unmount()
  })
})
