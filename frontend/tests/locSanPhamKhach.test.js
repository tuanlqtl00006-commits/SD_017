import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Trang Sản phẩm của khách (review 14/11 + làm đẹp 15/11): khoảng giá tự nhập, "Xóa tất cả" bỏ hết lọc,
// điện thoại: bộ lọc ẩn sau nút "Bộ lọc" có đếm số tiêu chí đang bật.
const { cuaHang, api, route, replace, push } = vi.hoisted(() => ({
  cuaHang: { getDanhSachSanPham: vi.fn(), getDanhMucVaThuongHieu: vi.fn() },
  api: { get: vi.fn() },
  route: { query: {}, params: {} },
  replace: vi.fn(),
  push: vi.fn(),
}))
vi.mock('../src/services/cuaHangService', () => ({ cuaHangService: cuaHang }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/composables/useAutoRefresh', () => ({ useAutoRefresh: () => {} }))
vi.mock('vue-router', async (goc) => ({ ...(await goc()), useRoute: () => route, useRouter: () => ({ replace, push }) }))

import ProductCatalogView from '../src/views/client/ProductCatalogView.vue'

const SP = [
  { id: 1, ten: 'Vợt 800k', idDanhMuc: 1, idThuongHieu: 1, brandName: 'Yonex', anhChinh: '/a.webp', minPrice: 800_000, bienThe: [] },
  { id: 2, ten: 'Vợt 1tr2', idDanhMuc: 1, idThuongHieu: 1, brandName: 'Yonex', anhChinh: '/b.webp', minPrice: 1_200_000, bienThe: [] },
  { id: 3, ten: 'Vợt 2tr5', idDanhMuc: 1, idThuongHieu: 3, brandName: 'Victor', anhChinh: '/c.webp', minPrice: 2_500_000, bienThe: [] },
]

async function moTrang() {
  cuaHang.getDanhSachSanPham.mockResolvedValue(SP)
  cuaHang.getDanhMucVaThuongHieu.mockResolvedValue({
    danhMuc: [{ id: 1, ten: 'Vợt cầu lông' }],
    thuongHieu: [{ id: 1, ten: 'Yonex' }, { id: 3, ten: 'Victor' }],
    thuocTinh: {},
  })
  api.get.mockResolvedValue({ data: [] })
  const w = mount(ProductCatalogView, { attachTo: document.body, global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
  await flushPromises()
  return w
}
const tieuDe = (w) => w.findAll('h4').map((h) => h.text()).find((x) => x.includes('sản phẩm)')) || ''

beforeEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
  setActivePinia(createPinia())
  route.query = {}
})

describe('Lọc sản phẩm ở trang khách', () => {
  it('khoảng giá tự nhập: "từ" lớn hơn "đến" thì báo lỗi, không lọc; nhập đúng thì chỉ còn sản phẩm trong khoảng; Bỏ thì về đủ', async () => {
    const w = await moTrang()
    expect(tieuDe(w)).toContain('3 sản phẩm')
    await w.find('#loc-gia-tu').setValue('2000000')
    await w.find('#loc-gia-den').setValue('1000000')
    await w.find('[data-testid="ap-dung-khoang-gia"]').trigger('click')
    expect(w.find('[data-testid="loi-khoang-gia"]').text()).toContain('nhỏ hơn hoặc bằng')
    expect(tieuDe(w)).toContain('3 sản phẩm')

    await w.find('#loc-gia-tu').setValue('1000000')
    await w.find('#loc-gia-den').setValue('2000000')
    await w.find('[data-testid="ap-dung-khoang-gia"]').trigger('click')
    expect(w.find('[data-testid="loi-khoang-gia"]').exists()).toBe(false)
    expect(tieuDe(w)).toContain('1 sản phẩm')
    expect(w.text()).toContain('Vợt 1tr2')
    expect(w.text()).not.toContain('Vợt 800k')
    expect(w.find('.catalog-nut-loc-dem').text()).toBe('1') // nút "Bộ lọc" (điện thoại) đếm 1 tiêu chí

    const nutBo = w.findAll('[data-testid="khoang-gia"] button').find((b) => b.text() === 'Bỏ')
    await nutBo.trigger('click')
    expect(tieuDe(w)).toContain('3 sản phẩm')
    expect(w.find('.catalog-nut-loc-dem').exists()).toBe(false)
    w.unmount()
  })

  it('"Xóa tất cả" bỏ cả khoảng giá, thương hiệu và lọc đang giảm giá trên đường dẫn', async () => {
    route.query = { khuyenMai: '1' }
    const w = await moTrang()
    await w.find('#loc-gia-tu').setValue('1000000')
    await w.find('[data-testid="ap-dung-khoang-gia"]').trigger('click')
    const nutXoa = w.findAll('button').find((b) => b.text() === 'Xóa tất cả')
    await nutXoa.trigger('click')
    expect(w.find('#loc-gia-tu').element.value).toBe('')
    expect(replace).toHaveBeenCalledWith({ path: '/shop/san-pham', query: {} })
    w.unmount()
  })

  it('nút "Bộ lọc" mở / đóng ngăn kéo bộ lọc, nút cuối ngăn kéo ghi số sản phẩm đang khớp', async () => {
    const w = await moTrang()
    const cot = w.find('[data-testid="cot-bo-loc"]')
    expect(cot.classes()).not.toContain('is-mo')
    await w.find('[data-testid="nut-bo-loc"]').trigger('click')
    expect(cot.classes()).toContain('is-mo')
    const nutXem = w.find('.catalog-loc-chan button')
    expect(nutXem.text()).toBe('Xem 3 sản phẩm')
    await nutXem.trigger('click')
    expect(cot.classes()).not.toContain('is-mo')
    w.unmount()
  })
})
