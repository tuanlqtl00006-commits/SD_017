import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'

// Làm đẹp trang khách 15/11 + việc cô review 14/11: đơn của khách mở trong "Đơn hàng của tôi" (không sang trang Tra cứu),
// màn chính sách cửa hàng theo đúng cách hệ thống tính, bộ lọc giá tự nhập + bộ lọc dạng ngăn kéo trên điện thoại.
const { api, route, push, replace } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  route: { query: {}, params: {}, hash: '', fullPath: '/' },
  push: vi.fn(),
  replace: vi.fn(),
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('vue-router', async (goc) => ({
  ...(await goc()),
  useRoute: () => route,
  useRouter: () => ({ push, replace }),
}))

import { useAuth, _datLaiAuth } from '../src/composables/useAuth'
import { ghiDonHang } from '../src/utils/donHangLocal'
import { nhanCaiDatVanChuyen } from '../src/services/caiDatVanChuyen'
import AccountView from '../src/views/client/AccountView.vue'
import ChinhSachView from '../src/views/client/ChinhSachView.vue'

const RouterLinkGia = { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' }
const gan = (c) => mount(c, { attachTo: document.body, global: { components: { RouterLink: RouterLinkGia } } })

beforeEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
  localStorage.clear()
  setActivePinia(createPinia())
  _datLaiAuth()
  Object.assign(route, reactive({ query: {}, params: {}, hash: '' }))
  route.query = {}
})

describe('Đơn hàng của tôi: xem chi tiết ngay trong trang tài khoản', () => {
  function dangNhapKhach() {
    const auth = useAuth()
    auth.trangThai.daKhoiTao = true
    auth.trangThai.nguoiDung = { id: 8, hoTen: 'Khách A', vaiTro: 'KHACH_HANG' }
    ghiDonHang([{ maHoaDon: 'HD900', trangThai: 0, trangThaiText: 'Chờ xác nhận', items: [], tongTien: 1000 }])
  }
  const CHI_TIET = {
    hoaDon: { id: 9, maHoaDon: 'HD900', loaiDon: 2, tenLoaiDon: 'Trực tuyến', trangThai: 1, ngayTao: '2026-11-14T10:00:00', tongTien: 1000, soTienGiam: 0, phiVanChuyen: 0, thanhTien: 1000 },
    khachHang: { ten: 'Khách A', sdt: '0901000001' },
    giaoHang: { hoTenNguoiNhan: 'Khách A', sdtNguoiNhan: '0901000001', diaChi: 'Số 1, Phường Cửa Nam, Thành phố Hà Nội' },
    sanPham: [],
    lichSu: [],
    thanhToan: [],
  }

  it('nút "Chi tiết đơn hàng" ở lại trang tài khoản (?tab=orders&don=), không sang /shop/tra-cuu', async () => {
    dangNhapKhach()
    route.query = { tab: 'orders' }
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/khach/don-hang/')) return { data: { trangThai: 1, chiTiet: [], chiTietDon: CHI_TIET } }
      return { data: url === '/khach/tai-khoan' ? {} : [] }
    })
    const w = gan(AccountView)
    await flushPromises()
    const nut = w.find('[data-testid="ac-xem-don"]')
    expect(nut.exists()).toBe(true)
    expect(w.html()).not.toContain('/shop/tra-cuu')
    await nut.trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/shop/tai-khoan', query: { tab: 'orders', don: 'HD900' } })
    w.unmount()
  })

  it('mở trang với ?don=HD900: hiện chi tiết đơn bằng khung chung, có nút về danh sách', async () => {
    dangNhapKhach()
    route.query = { tab: 'orders', don: 'HD900' }
    api.get.mockImplementation(async (url) => {
      if (url === '/khach/don-hang/HD900') return { data: { trangThai: 1, chiTiet: [], chiTietDon: CHI_TIET } }
      if (url.startsWith('/khach/don-hang/')) return { data: { trangThai: 1, chiTiet: [] } }
      return { data: url === '/khach/tai-khoan' ? {} : [] }
    })
    const w = gan(AccountView)
    await flushPromises()
    const khung = w.find('[data-testid="ac-chi-tiet-don"]')
    expect(khung.exists()).toBe(true)
    expect(khung.text()).toContain('Đơn hàng HD900')
    expect(khung.text()).toContain('Phường Cửa Nam')
    await w.find('[data-testid="ac-ve-danh-sach"]').trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/shop/tai-khoan', query: { tab: 'orders' } })
    w.unmount()
  })
  it('khách tự sửa thông tin giao hàng 1 lần: có nút khi máy chủ cho phép, lưu thì PUT /khach/don-hang/{ma}/sua-giao-hang', async () => {
    dangNhapKhach()
    route.query = { tab: 'orders', don: 'HD900' }
    const choPhep = { ...CHI_TIET, hoaDon: { ...CHI_TIET.hoaDon, duocSuaGiaoHang: true } }
    const daSua = { ...CHI_TIET, hoaDon: { ...CHI_TIET.hoaDon, duocSuaGiaoHang: false, daSuaGiaoHang: true } }
    api.get.mockImplementation(async (url) => {
      if (url === '/khach/don-hang/HD900') return { data: { trangThai: 1, chiTiet: [], chiTietDon: choPhep } }
      if (url.startsWith('/khach/don-hang/')) return { data: { trangThai: 1, chiTiet: [] } }
      return { data: url === '/khach/tai-khoan' ? {} : [] }
    })
    api.put.mockResolvedValue({ data: { chiTietDon: daSua } })
    const w = gan(AccountView)
    await flushPromises()
    await w.get('[data-testid="ac-sua-giao-hang"] button').trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Ghi chú cho cửa hàng (không bắt buộc)')
    document.body.querySelector('[data-testid="sgh-luu"]').click()
    await flushPromises()
    expect(api.put).toHaveBeenCalledWith('/khach/don-hang/HD900/sua-giao-hang', expect.objectContaining({ phuongXa: 'Phường Cửa Nam', tinhThanh: 'Thành phố Hà Nội' }))
    expect(w.find('[data-testid="ac-sua-giao-hang"]').exists()).toBe(false)
    expect(w.find('[data-testid="ac-da-doi-giao"]').text()).toContain('một lần')
    w.unmount()
  })

  it('đơn đã được nhân viên sửa (daChinhSua hoặc daSuaGiaoHang): khách không được sửa nữa, hiện thông báo đã đổi 1 lần', async () => {
    dangNhapKhach()
    route.query = { tab: 'orders', don: 'HD900' }
    const daSuaVoiNhanVien = {
      ...CHI_TIET,
      hoaDon: { ...CHI_TIET.hoaDon, duocSuaGiaoHang: false, daSuaGiaoHang: true, daChinhSua: true },
    }
    api.get.mockImplementation(async (url) => {
      if (url === '/khach/don-hang/HD900') return { data: { trangThai: 1, chiTiet: [], chiTietDon: daSuaVoiNhanVien } }
      return { data: url === '/khach/tai-khoan' ? {} : [] }
    })
    const w = gan(AccountView)
    await flushPromises()
    expect(w.find('[data-testid="ac-sua-giao-hang"]').exists()).toBe(false)
    const thongBao = w.find('[data-testid="ac-da-doi-giao"]')
    expect(thongBao.exists()).toBe(true)
    expect(thongBao.text()).toContain('một lần')
    w.unmount()
  })

  it('hồ sơ cá nhân lên đầu tiên: menu sidebar theo thứ tự Hồ sơ cá nhân -> Theo dõi đơn hàng -> Sổ địa chỉ; mặc định mở tab Hồ sơ cá nhân', async () => {
    dangNhapKhach()
    api.get.mockImplementation(async (url) => {
      if (url === '/khach/tai-khoan') return { data: { hoTen: 'Khách A', sdt: '0901000001' } }
      return { data: [] }
    })
    const w = gan(AccountView)
    await flushPromises()
    const nutNav = w.findAll('.nav button')
    expect(nutNav).toHaveLength(3)
    expect(nutNav[0].text()).toContain('Hồ sơ cá nhân')
    expect(nutNav[1].text()).toContain('Theo dõi đơn hàng')
    expect(nutNav[2].text()).toContain('Sổ địa chỉ nhận hàng')
    expect(nutNav[0].classes()).toContain('btn-primary')
    expect(w.text()).toContain('Thông tin tài khoản')
    w.unmount()
  })
})

describe('Màn chính sách cửa hàng', () => {
  // Góp ý 16/11: bỏ bảng phí theo tỉnh, ghi đúng phí giao hàng quản lý cài
  it('ghi đúng ngưỡng miễn phí vận chuyển và phí giao hàng đang cài đặt, không còn bảng phí theo tỉnh', async () => {
    api.get.mockResolvedValue({ data: { mienPhiBat: true, mienPhiTu: 2_000_000, phiGiaoHang: 30_000 } })
    nhanCaiDatVanChuyen({ mienPhiBat: true, mienPhiTu: 2_000_000, phiGiaoHang: 30_000 })
    const w = gan(ChinhSachView)
    await flushPromises()
    expect(w.find('[data-testid="cs-mien-phi"]').text()).toContain('2.000.000')
    const phi = w.find('[data-testid="cs-phi-giao"]').text()
    expect(phi).toContain('30.000')
    expect(phi).toContain('như nhau ở mọi tỉnh')
    expect(w.find('[data-testid="cs-bang-phi"]').exists()).toBe(false)
    expect(w.text()).not.toContain('giao tới phường')
    for (const muc of ['Giao hàng', 'Thanh toán', 'Đổi trả & bảo hành', 'Đặt hàng & hủy đơn', 'Bảo mật thông tin']) expect(w.text()).toContain(muc)
    expect(w.text()).toContain('Giao Hàng Nhanh')
    // không ghi các phương thức hệ thống không có
    expect(w.text()).not.toMatch(/MoMo|ZaloPay|quẹt thẻ/i)
    w.unmount()

    nhanCaiDatVanChuyen({ mienPhiBat: false, mienPhiTu: 2_000_000 })
    const tat = gan(ChinhSachView)
    await flushPromises()
    expect(tat.find('[data-testid="cs-mien-phi"]').exists()).toBe(false)
    tat.unmount()
  })
})
