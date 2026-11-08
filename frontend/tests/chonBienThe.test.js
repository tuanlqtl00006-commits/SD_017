import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Trang chi tiết sản phẩm (review 05/11, thầy bấm ra): khách online phải chọn được trọng lượng 3U/4U và chu vi cán,
// giá - tồn đổi theo đúng biến thể, thêm giỏ đúng biến thể đang chọn.
const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {}, params: { id: '1' } }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  createRouter: () => ({ beforeEach() {}, afterEach() {} }),
  createWebHistory: () => ({}),
}))
vi.mock('../src/services/api', () => ({ default: api }))

import ProductDetailView from '../src/views/client/ProductDetailView.vue'
import { useCartStore } from '../src/stores/cart'

// Dữ liệu đúng dạng backend trả về (GET /cua-hang/san-pham/1)
const VOT = {
  id: 1, maSanPham: 'SP01', tenSanPham: 'Vợt Yonex Astrox', tenDanhMuc: 'Vợt cầu lông',
  anhChinh: '/den.png', danhSachAnh: ['/den.png'],
  bienThe: [
    { id: 11, maSpct: 'V-DEN-3U-G5', idMauSac: 1, tenMau: 'Đen', idTrongLuong: 31, trongLuong: '3U', idChuVi: 41, chuVi: 'G5', giaBan: 2000000, giaBanGoc: 2000000, soLuongTon: 5 },
    { id: 12, maSpct: 'V-DEN-4U-G5', idMauSac: 1, tenMau: 'Đen', idTrongLuong: 32, trongLuong: '4U', idChuVi: 41, chuVi: 'G5', giaBan: 2200000, giaBanGoc: 2200000, soLuongTon: 0 },
    { id: 13, maSpct: 'V-DO-3U-G5', idMauSac: 2, tenMau: 'Đỏ', idTrongLuong: 31, trongLuong: '3U', idChuVi: 41, chuVi: 'G5', giaBan: 1800000, giaBanGoc: 2000000, phanTramGiam: 10, soLuongTon: 3 },
    { id: 14, maSpct: 'V-DEN-3U-G6', idMauSac: 1, tenMau: 'Đen', idTrongLuong: 31, trongLuong: '3U', idChuVi: 42, chuVi: 'G6', giaBan: 2100000, giaBanGoc: 2100000, soLuongTon: 2 },
  ],
}
// Như "tesst" SP55: 2 biến thể cùng màu, khác trọng lượng và khác giá
const HAI_TRONG_LUONG = {
  id: 1, maSanPham: 'SP55', tenSanPham: 'tesst', tenDanhMuc: 'Vợt cầu lông', anhChinh: '/v.png', danhSachAnh: ['/v.png'],
  bienThe: [
    { id: 108, maSpct: 'SP55-VANG-4U-G5', idMauSac: 5, tenMau: 'Vàng', idTrongLuong: 32, trongLuong: '4U (80-84g)', idChuVi: 41, chuVi: 'G5', giaBan: 1000000, giaBanGoc: 1000000, soLuongTon: 3 },
    { id: 109, maSpct: 'SP55-VANG-3U-G5', idMauSac: 5, tenMau: 'Vàng', idTrongLuong: 31, trongLuong: '3U (85-89g)', idChuVi: 41, chuVi: 'G5', giaBan: 1200000, giaBanGoc: 1200000, soLuongTon: 4 },
  ],
}
const BALO = {
  id: 1, maSanPham: 'SP70', tenSanPham: 'Balo Yonex', tenDanhMuc: 'Túi - balo', anhChinh: '/b.png', danhSachAnh: ['/b.png'],
  bienThe: [
    { id: 21, maSpct: 'B-DEN', idMauSac: 1, tenMau: 'Đen', idTrongLuong: 9, trongLuong: 'Không áp dụng', idChuVi: 8, chuVi: 'Không áp dụng', giaBan: 500000, giaBanGoc: 500000, soLuongTon: 4 },
  ],
}

async function moTrang(sanPham = VOT) {
  api.get.mockResolvedValue({ data: sanPham })
  const w = mount(ProductDetailView, { global: { stubs: { RouterLink: { props: ['to'], template: '<a><slot /></a>' } } } })
  await flushPromises()
  return w
}
const nut = (w, ten) => w.findAll(`[data-testid="${ten}"]`)
const nutChu = (w, chu) => w.findAll('button').find((b) => b.text().includes(chu))

beforeEach(() => {
  try { localStorage.clear() } catch { /* không có localStorage */ }
  setActivePinia(createPinia())
  api.get.mockReset()
})

describe('Chọn biến thể ở trang chi tiết', () => {
  it('hiện đủ nút trọng lượng 3U/4U, chu vi cán và màu lấy từ biến thể thật', async () => {
    const w = await moTrang()
    expect(nut(w, 'chon-trong-luong').map((b) => b.text())).toEqual(['3U', '4U'])
    expect(nut(w, 'chon-chu-vi').map((b) => b.text())).toEqual(['G5', 'G6'])
    expect(nut(w, 'chon-mau').map((b) => b.text())).toEqual(['Đen', 'Đỏ'])
    w.unmount()
  })

  it('như "tesst": bấm 3U ra giá 1.200.000, tồn 4; thêm giỏ đúng biến thể 3U', async () => {
    const w = await moTrang(HAI_TRONG_LUONG)
    expect(w.text()).toContain('1.000.000')
    expect(w.text()).toContain('Còn 3 sản phẩm trong kho')
    await nut(w, 'chon-trong-luong')[1].trigger('click')
    expect(nut(w, 'chon-trong-luong')[1].classes()).toContain('active')
    expect(w.text()).toContain('1.200.000')
    expect(w.text()).toContain('Còn 4 sản phẩm trong kho')
    await nutChu(w, 'Thêm vào giỏ').trigger('click')
    const dong = useCartStore().items[0]
    expect(dong.bienTheId).toBe(109)
    expect(dong.trongLuong).toBe('3U (85-89g)')
    expect(dong.giaBan).toBe(1200000)
    w.unmount()
  })

  it('biến thể hết hàng: nút làm mờ, bấm xem được, khóa nút mua', async () => {
    const w = await moTrang()
    expect(nut(w, 'chon-trong-luong')[1].classes()).toContain('het-hang')
    expect(nut(w, 'chon-trong-luong')[0].classes()).not.toContain('het-hang')
    await nut(w, 'chon-trong-luong')[1].trigger('click')
    expect(w.text()).toContain('2.200.000')
    expect(w.text()).toContain('Tạm hết hàng phiên bản này')
    expect(nutChu(w, 'Thêm vào giỏ').attributes('disabled')).toBeDefined()
    await nut(w, 'chon-trong-luong')[0].trigger('click')
    expect(nutChu(w, 'Thêm vào giỏ').attributes('disabled')).toBeUndefined()
    w.unmount()
  })

  it('bấm chu vi G6: giá và tồn đổi theo biến thể G6', async () => {
    const w = await moTrang()
    await nut(w, 'chon-chu-vi')[1].trigger('click')
    expect(w.text()).toContain('2.100.000')
    expect(w.text()).toContain('Còn 2 sản phẩm trong kho')
    w.unmount()
  })

  it('bấm trọng lượng không có ở màu đang chọn: chuyển sang biến thể có thật, nút khớp với giá đang hiện', async () => {
    const w = await moTrang()
    await nut(w, 'chon-mau')[1].trigger('click') // Đỏ: chỉ có 3U
    await nut(w, 'chon-trong-luong')[1].trigger('click') // 4U chỉ có ở Đen
    expect(nut(w, 'chon-mau')[0].classes()).toContain('active')
    expect(nut(w, 'chon-trong-luong')[1].classes()).toContain('active')
    expect(w.text()).toContain('2.200.000')
    w.unmount()
  })

  it('đổi sang biến thể ít hàng hơn: số lượng đang chọn tự về đúng số tồn', async () => {
    const w = await moTrang()
    for (let i = 0; i < 4; i++) await nutChu(w, '+').trigger('click') // Đen 3U G5: chọn 5
    expect(w.text()).toContain('5')
    await nut(w, 'chon-chu-vi')[1].trigger('click') // G6 chỉ còn 2
    await nutChu(w, 'Thêm vào giỏ').trigger('click')
    expect(useCartStore().items[0].soLuong).toBe(2)
    w.unmount()
  })

  it('balo: "Không áp dụng" không thành nút chọn, không ghi vào giỏ', async () => {
    const w = await moTrang(BALO)
    expect(nut(w, 'chon-trong-luong')).toHaveLength(0)
    expect(nut(w, 'chon-chu-vi')).toHaveLength(0)
    expect(w.text()).not.toContain('Không áp dụng')
    await nutChu(w, 'Thêm vào giỏ').trigger('click')
    const dong = useCartStore().items[0]
    expect(dong.bienTheId).toBe(21)
    expect(dong.trongLuong).toBe('')
    expect(dong.chuVi).toBe('')
    w.unmount()
  })
})
