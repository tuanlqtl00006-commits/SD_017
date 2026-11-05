import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'

// Cô review 04/11 + lỗi thấy trong video: trang thanh toán online
const { api, auth, diaChi } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn() },
  auth: { trangThai: null, khoiTao: async () => {} },
  diaChi: { getAll: vi.fn(), create: vi.fn() },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/composables/useAuth', () => ({ useAuth: () => auth }))
vi.mock('../src/services/khachDiaChiService', () => ({ khachDiaChiService: diaChi }))
vi.mock('../src/services/diaChiHanhChinhService', () => {
  const tinh = [{ code: 1, name: 'Thành phố Hà Nội' }, { code: 48, name: 'Thành phố Đà Nẵng' }]
  const phuong = { 1: [{ code: 10, name: 'Phường Cửa Nam' }], 48: [{ code: 20, name: 'Phường An Hải' }] }
  return { API_DIA_CHI: 'x', diaChiHanhChinhService: { layTinhThanh: async () => tinh, layPhuongXa: async (m) => phuong[m] ?? [], xoaNho() {} } }
})
vi.mock('../src/components/shop/VoucherPanel.vue', () => ({ default: { template: '<div />' } }))

import CheckoutView from '../src/views/client/CheckoutView.vue'
import { useCartStore } from '../src/stores/cart'

const body = () => new DOMWrapper(document.body)
const SO_DIA_CHI = [
  { id: 1, tenNguoiNhan: 'lê quốc tuấn', sdtNguoiNhan: '0379992712', tinhThanhPho: 'Thành phố Hà Nội', phuongXa: 'Phường Cửa Nam', diaChiCuThe: 'so,ngo', laDiaChiMacDinh: true },
  { id: 2, tenNguoiNhan: 'lê quốc tuấn', sdtNguoiNhan: '0379992712', tinhThanhPho: 'Thành phố Đà Nẵng', phuongXa: 'Phường An Hải', diaChiCuThe: 'Số 5', laDiaChiMacDinh: false },
]

async function moTrang({ dangNhap = false, tien = 480000, soLuong = 2 } = {}) {
  auth.trangThai = reactive({ nguoiDung: dangNhap ? { id: 9, vaiTro: 'KHACH_HANG', hoTen: 'lê quốc tuấn', sdt: '0379992712', email: 'a@gmail.com' } : null })
  const cart = useCartStore()
  cart.items.splice(0)
  cart.items.push({ bienTheId: 52, sanPhamTen: 'Ống cầu lông Victor Master No.7', giaBan: tien, soLuong, mauSac: 'Trắng' })
  const w = mount(CheckoutView, { attachTo: document.body })
  await flushPromises()
  return { w, cart }
}
const datHang = async (w) => {
  await w.findAll('button').find((b) => b.text().includes('ĐẶT HÀNG') || b.text().includes('Đặt hàng')).trigger('click')
  await flushPromises()
}

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  setActivePinia(createPinia())
  Object.values(api).forEach((f) => f.mockReset())
  Object.values(diaChi).forEach((f) => f.mockReset())
  diaChi.getAll.mockResolvedValue(SO_DIA_CHI)
})

describe('Thanh toán online', () => {
  it('phí ship theo tỉnh: Hà Nội miễn phí dù đơn nhỏ, tỉnh khác mất phí tùy tỉnh dù đơn lớn, chưa chọn tỉnh thì chưa tính', async () => {
    const { cart } = await moTrang({ tien: 480000, soLuong: 2 })
    expect(cart.chuaCoTinh).toBe(true)
    expect(cart.shippingFee).toBe(0)
    cart.tinhGiao = 'Thành phố Hà Nội'
    expect(cart.shippingFee).toBe(0)
    cart.tinhGiao = 'Thành phố Đà Nẵng'
    expect(cart.chuaCoTinh).toBe(false)
    expect(cart.shippingFee).toBe(40000)
    cart.items[0].soLuong = 30 // 14.400.000 ₫: đơn lớn vẫn mất phí ship ở tỉnh khác
    expect(cart.shippingFee).toBe(40000)
    cart.tinhGiao = 'Tỉnh Phú Thọ'
    expect(cart.shippingFee).toBe(25000)
  })

  it('khách chưa đăng nhập: chưa chọn Tỉnh/Thành, Phường/Xã thì không đặt được (đơn HD970 trong video)', async () => {
    const { w } = await moTrang()
    await w.find('input[placeholder="Ví dụ: Nguyễn Văn A"]').setValue('aaa')
    await w.find('input[placeholder="Ví dụ: 0912345678"]').setValue('0379998734')
    await w.find('input[placeholder="Ví dụ: Số 20 ngõ 56 Phố Nhổn"]').setValue('s23sssss')
    await datHang(w)
    expect(api.post.mock.calls.some((c) => c[0] === '/cua-hang/dat-hang')).toBe(false)
    expect(w.text()).toContain('Vui lòng chọn Tỉnh/Thành phố.')
    expect(w.text()).toContain('Vui lòng chọn Phường/Xã.')
  })

  it('báo lỗi tự mất khi đã nhập đúng (video: nhập "aaa" rồi mà vẫn đỏ "Vui lòng nhập họ và tên")', async () => {
    const { w } = await moTrang()
    await datHang(w)
    expect(w.text()).toContain('Vui lòng nhập họ và tên người nhận.')
    expect(w.text()).toContain('Vui lòng nhập số điện thoại.')
    await w.find('input[placeholder="Ví dụ: Nguyễn Văn A"]').setValue('aaa')
    await w.find('input[placeholder="Ví dụ: 0912345678"]').setValue('0379998734')
    await flushPromises()
    expect(w.text()).not.toContain('Vui lòng nhập họ và tên người nhận.')
    expect(w.text()).not.toContain('Vui lòng nhập số điện thoại.')
  })

  it('sổ địa chỉ gọn: một dòng địa chỉ mặc định + "Thay đổi" mở pop-up, chọn địa chỉ khác hoặc thêm địa chỉ mới', async () => {
    const { w } = await moTrang({ dangNhap: true })
    const gon = () => w.find('[data-testid="ck-dia-chi-gon"]')
    expect(gon().text()).toContain('so,ngo, Phường Cửa Nam, Thành phố Hà Nội')
    expect(gon().text()).toContain('Mặc định')
    expect(w.findAll('.dc-card')).toHaveLength(0) // không còn các thẻ địa chỉ to trên trang
    expect(w.find('#ck-dc-tinh').exists()).toBe(false)

    await w.get('[data-testid="ck-doi-dia-chi"]').trigger('click')
    await flushPromises()
    const the = body().findAll('.dc-card')
    expect(the).toHaveLength(3) // 2 địa chỉ đã lưu + "Thêm địa chỉ mới"
    await the[1].trigger('click')
    await flushPromises()
    expect(body().find('.dc-list').exists()).toBe(false) // chọn xong thì đóng pop-up
    expect(gon().text()).toContain('Số 5, Phường An Hải, Thành phố Đà Nẵng')

    await w.get('[data-testid="ck-doi-dia-chi"]').trigger('click')
    await flushPromises()
    await body().findAll('.dc-card')[2].trigger('click')
    await flushPromises()
    expect(gon().text()).toContain('Giao đến địa chỉ mới')
    expect(w.find('#ck-dc-tinh').exists()).toBe(true)
  })

  // Review 05/11: đồng bộ phần tiền - giỏ hàng lưu giá cũ trên máy, số trên trang thanh toán phải lấy theo máy chủ
  it('dùng số máy chủ tính: cập nhật giá cũ trong giỏ, có dòng giảm theo đợt, giá gạch, các dòng cộng trừ ra đúng tổng', async () => {
    api.post.mockImplementation(async (url) => (url === '/cua-hang/tinh-tien'
      ? { data: {
        dong: [{ idSanPhamChiTiet: 52, conBan: true, giaGoc: 600000, donGia: 480000, soLuong: 2, soLuongTon: 9 }],
        tongTienGoc: 1200000, tienGiamTheoDot: 240000, tongTien: 960000, maPhieu: null, soTienGiam: 0, loiPhieu: null, phiVanChuyen: null, thanhTien: 960000,
      } }
      : { data: {} }))
    const { w, cart } = await moTrang({ tien: 500000, soLuong: 2 }) // giá trong giỏ đã cũ (500.000)
    await new Promise((r) => setTimeout(r, 300))
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/cua-hang/tinh-tien', expect.objectContaining({ chiTiet: [{ idSanPhamChiTiet: 52, soLuong: 2 }], tinhThanh: null }))
    expect(cart.items[0].giaBan).toBe(480000) // giá cập nhật theo cửa hàng
    expect(w.find('[data-testid="co-doi-gia"]').exists()).toBe(true)
    const dong = (k) => w.find(`[data-dong="${k}"]`).text().replace(/\s+/g, ' ')
    expect(dong('tong-tien-hang')).toContain('1.200.000')
    expect(dong('giam-dot')).toContain('Giảm theo đợt')
    expect(dong('giam-dot')).toContain('-240.000')
    expect(dong('phieu')).toContain('Không dùng phiếu')
    // phí giao tính theo tỉnh (commit của Phương Minh): chưa chọn tỉnh thì chưa tính
    expect(dong('phi-van-chuyen')).toContain('Chọn Tỉnh/Thành phố để tính')
    expect(dong('tong-thanh-toan')).toContain('Tổng thanh toán')
    expect(dong('tong-thanh-toan')).toContain('960.000')
    expect(w.find('[data-testid="co-don-gia"]').text()).toContain('600.000')
  })
})
