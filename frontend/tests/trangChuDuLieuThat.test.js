import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Trang chủ cửa hàng (làm đẹp 15/11): mọi con số lấy từ dữ liệu thật, không còn sao 5.0, "100+ lượt mua",
// banner "ASTROX 100ZZ", khối "GIẢM TỪ 20% ĐẾN 50%" hay thương hiệu KAWASAKI không có trong cửa hàng.
const { api, cuaHang, push } = vi.hoisted(() => ({
  api: { get: vi.fn() },
  cuaHang: { getDanhSachSanPham: vi.fn(), getDanhMucVaThuongHieu: vi.fn() },
  push: vi.fn(),
}))
const RouterLinkGia = { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' }
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ query: {}, params: {} }),
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/cuaHangService', () => ({ cuaHangService: cuaHang }))
vi.mock('../src/composables/useAuth', () => ({ useAuth: () => ({ daDangNhap: { value: false }, laKhachHang: { value: false } }) }))
vi.mock('../src/composables/useAutoRefresh', () => ({ useAutoRefresh: () => {} }))

import HomeView from '../src/views/client/HomeView.vue'

// Dữ liệu như cuaHangService.formatSanPhamCuaHang trả về
const SP = [
  { id: 1, ten: 'Vợt A', tenSanPham: 'Vợt A', idDanhMuc: 1, anhChinh: '/images/a.webp', minPrice: 1_000_000, bienThe: [{ id: 11, giaBan: 1_000_000, phanTramGiam: 0 }] },
  { id: 2, ten: 'Vợt B người mới', tenSanPham: 'Vợt B người mới', idDanhMuc: 2, anhChinh: '/images/b.webp', minPrice: 600_000, bienThe: [{ id: 21, giaBan: 600_000, phanTramGiam: 0 }] },
  { id: 3, ten: 'Vợt C đang giảm', tenSanPham: 'Vợt C đang giảm', idDanhMuc: 1, anhChinh: '/images/c.webp', minPrice: 850_000, originalPrice: 1_000_000, maxDiscountPercent: 15, hasDiscount: true, bienThe: [{ id: 31, giaBan: 1_000_000, phanTramGiam: 15 }] },
]
const DM = {
  danhMuc: [{ id: 1, ten: 'Vợt cầu lông' }, { id: 2, ten: 'Vợt cầu lông người mới' }],
  thuongHieu: [{ id: 1, ten: 'Yonex', trangThai: 1 }, { id: 3, ten: 'Victor', trangThai: 1 }, { id: 9, ten: 'Hãng đã ngừng', trangThai: 0 }],
}
const DOT = [
  { id: 44, tenDot: 'Tuần lễ vợt Mizuno', phanTramGiamMax: 10, soSanPham: 3, ngayKetThuc: '2026-11-24T23:59:00', idSanPham: [1] },
  { id: 47, tenDot: 'Sale giữa tháng', phanTramGiamMax: 15, soSanPham: 5, ngayKetThuc: '2026-11-27T23:59:00', idSanPham: [3] },
]

function gia({ banChay = [{ idSanPham: 3, daBan: 10 }, { idSanPham: 1, daBan: 6 }, { idSanPham: 99, daBan: 4 }], dot = DOT } = {}) {
  cuaHang.getDanhSachSanPham.mockResolvedValue(SP)
  cuaHang.getDanhMucVaThuongHieu.mockResolvedValue(DM)
  api.get.mockImplementation(async (url) => {
    if (url === '/cua-hang/ban-chay') return { data: { soNgay: 30, ds: banChay } }
    if (url === '/cua-hang/dot-giam-gia') return { data: dot }
    return { data: [] }
  })
}

async function moTrang() {
  const w = mount(HomeView, { global: { components: { RouterLink: RouterLinkGia } } })
  await flushPromises()
  return w
}

beforeEach(() => {
  vi.clearAllMocks()
  setActivePinia(createPinia())
})

describe('Trang chủ dùng dữ liệu thật', () => {
  it('bán chạy: số đã bán thật theo 30 ngày, đúng thứ tự máy chủ, bỏ sản phẩm không còn bán; không có sao, không có "lượt mua"', async () => {
    gia()
    const w = await moTrang()
    const khoi = w.find('[data-testid="home-ban-chay"]')
    expect(khoi.exists()).toBe(true)
    expect(khoi.text()).toContain('30 ngày gần nhất')
    const daBan = khoi.findAll('[data-testid="the-san-pham-nhan"]').map((x) => x.text())
    expect(daBan).toEqual(['Đã bán 10', 'Đã bán 6']) // sản phẩm 99 không còn trong cửa hàng: bỏ
    expect(khoi.text().indexOf('Vợt C đang giảm')).toBeLessThan(khoi.text().indexOf('Vợt A'))
    expect(w.html()).not.toContain('bi-star-fill')
    expect(w.text()).not.toContain('lượt mua')
    expect(w.text()).not.toContain('5.0')
    expect(w.text()).not.toContain('đã bán)') // câu cũ "(100+ đã bán)" ở khối sản phẩm mới
    w.unmount()
  })

  it('chưa bán được gì thì ẩn khối bán chạy; không có đợt giảm giá thì ẩn khối đợt', async () => {
    gia({ banChay: [], dot: [] })
    const w = await moTrang()
    expect(w.find('[data-testid="home-ban-chay"]').exists()).toBe(false)
    expect(w.find('[data-testid="home-dot-giam-gia"]').exists()).toBe(false)
    w.unmount()
  })

  it('đợt giảm giá đang chạy: thẻ có mức giảm, số sản phẩm, ngày kết thúc, bấm vào mở đúng sản phẩm của đợt', async () => {
    gia()
    const w = await moTrang()
    const the = w.findAll('[data-testid="home-the-dot"]')
    expect(the).toHaveLength(2)
    expect(the[1].text()).toContain('15%')
    expect(the[1].text()).toContain('5 sản phẩm')
    expect(the[1].text()).toContain('27/11/2026')
    expect(JSON.parse(the[1].attributes('data-to'))).toEqual({ path: '/shop/san-pham', query: { dot: 47 } })
    expect(w.text()).not.toContain('GIẢM TỪ 20% ĐẾN 50%')
    w.unmount()
  })

  it('banner dùng ảnh sản phẩm thật, có slide ưu đãi theo mức giảm thật; thương hiệu lấy từ dữ liệu, bỏ hãng đã ngừng', async () => {
    gia()
    const w = await moTrang()
    const banner = w.find('[data-testid="home-banner"]')
    expect(banner.find('img').attributes('src')).toBe('/images/a.webp')
    expect(banner.text()).toContain('3 MẪU VỢT ĐANG BÁN')
    expect(banner.text()).toContain('Yonex, Victor')
    expect(w.text()).not.toContain('ASTROX 100ZZ')
    // slide cuối: giảm đến 15% (mức giảm cao nhất thật), ảnh của sản phẩm đang giảm nhiều nhất
    await banner.find('.banner-prev').trigger('click')
    expect(banner.text()).toContain('GIẢM ĐẾN 15%')
    expect(banner.find('img').attributes('src')).toBe('/images/c.webp')

    const hang = w.findAll('[data-testid="home-thuong-hieu"]').map((x) => x.find('h5').text())
    expect(hang).toEqual(['YONEX', 'VICTOR'])
    expect(w.text()).not.toContain('KAWASAKI')
    await w.findAll('[data-testid="home-thuong-hieu"]')[1].trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/shop/san-pham', query: { thuongHieu: 3 } })
    w.unmount()
  })
})
