import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'

// Cô review buổi 7 (9:15–9:31): khách chưa đăng nhập không được xem đơn (họ tên, SĐT, địa chỉ) của nhau chỉ bằng mã đơn
const { api, auth, route } = vi.hoisted(() => ({
  api: { get: vi.fn() },
  auth: { trangThai: null, khoiTao: async () => {} },
  route: { query: {} },
}))
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ push: vi.fn() }), RouterLink: { template: '<a><slot /></a>' } }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/composables/useAuth', () => ({ useAuth: () => auth }))

import OrderLookupView from '../src/views/client/OrderLookupView.vue'

const DON = { maHoaDon: 'HD967', trangThai: 1, tenTrangThai: 'Chờ xác nhận', hoTenNguoiNhan: 'tuan134', sdtNguoiNhan: '0379992712', diaChiGiaoHang: 'Đồng Nai', chiTiet: [], tongTien: 468000, thanhTien: 468000 }
const loi404 = () => Object.assign(new Error('Không tìm thấy'), { status: 404 })

async function tra(w, ma, sdt = '') {
  await w.find('input[aria-label="Mã đơn hàng"]').setValue(ma)
  await w.find('[data-testid="ol-sdt"]').setValue(sdt)
  await w.find('form').trigger('submit')
  await flushPromises()
}

beforeEach(() => {
  api.get.mockReset()
  auth.trangThai = reactive({ nguoiDung: null })
  route.query = {}
  localStorage.clear()
})

describe('Tra cứu đơn hàng', () => {
  it('khách chưa đăng nhập: chỉ nhập mã đơn thì không gọi máy chủ, bắt nhập SĐT đặt hàng', async () => {
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967')
    expect(api.get).not.toHaveBeenCalled()
    expect(w.text()).toContain('Vui lòng nhập số điện thoại đặt hàng')
    expect(w.text()).not.toContain('tuan134')
  })

  it('mở từ đường dẫn ?ma= khi chưa đăng nhập: chỉ điền mã, chưa tra', async () => {
    route.query = { ma: 'HD967' }
    const w = mount(OrderLookupView)
    await flushPromises()
    expect(w.find('input[aria-label="Mã đơn hàng"]').element.value).toBe('HD967')
    expect(api.get).not.toHaveBeenCalled()
  })

  it('đúng mã + SĐT thì xem được; sai SĐT báo không tìm thấy', async () => {
    api.get.mockImplementation((url, { params }) => (params.sdt === '0379992712' ? Promise.resolve({ data: DON }) : Promise.reject(loi404())))
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967', '0999999999')
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu', { params: { maHoaDon: 'HD967', sdt: '0999999999' } })
    expect(w.text()).toContain('với số điện thoại này')
    expect(w.text()).not.toContain('tuan134')
    await tra(w, 'HD967', '0379992712')
    expect(w.text()).toContain('tuan134')
  })

  it('đơn lưu trên máy (máy dùng chung): chỉ dùng khi mất kết nối và phải khớp SĐT', async () => {
    localStorage.setItem('footstyle_orders_khach', JSON.stringify([{ maHoaDon: 'HD967', tenKhachHang: 'tuan134', sdtKhachHang: '0379992712', items: [] }]))
    api.get.mockRejectedValue(loi404())
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967', '0379992712')
    expect(w.text()).not.toContain('tuan134') // máy chủ đã trả lời "không tìm thấy" thì không lấy bản trên máy
    api.get.mockRejectedValue(Object.assign(new Error('Không kết nối được máy chủ.'), { status: null }))
    await tra(w, 'HD967', '0999999999')
    expect(w.text()).not.toContain('tuan134')
    await tra(w, 'HD967', '0379992712')
    expect(w.text()).toContain('tuan134')
  })

  it('khách đã đăng nhập: xem đơn của mình không cần SĐT', async () => {
    auth.trangThai = reactive({ nguoiDung: { vaiTro: 'KHACH_HANG' } })
    api.get.mockResolvedValue({ data: DON })
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967')
    expect(api.get).toHaveBeenCalledWith('/khach/don-hang/HD967')
    expect(w.text()).toContain('tuan134')
  })
})

// Cô review 04/11 (05:57–06:26): màn đơn hàng của khách phải y hệt màn Chi tiết hóa đơn admin, trạng thái nhảy đúng bước và gợi ý bước sau
describe('Màn đơn hàng của khách đồng bộ với Chi tiết hóa đơn admin', () => {
  const CHI_TIET = {
    hoaDon: {
      maHoaDon: 'HD971', loaiDon: 2, tenLoaiDon: 'Trực tuyến', trangThai: 2, ngayTao: '2026-11-04T20:52:51',
      tongTien: 3634000, soTienGiam: 250000, phiVanChuyen: 0, thanhTien: 3384000, donViVanChuyen: 'GHN',
      maPhieuGiamGia: 'VCHE7KL4B', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 25,
    },
    khachHang: { ten: 'lê quốc tuấn', sdt: '0379992712', email: 'tuan@gmail.com' },
    giaoHang: { hoTenNguoiNhan: 'lê quốc tuấn', sdtNguoiNhan: '0379992712', diaChi: 'so,ngo, Phường An Hải, Thành phố Đà Nẵng' },
    sanPham: [{ id: 1, maSpct: 'SP53-TRG', tenSanPham: 'Ống cầu lông nhựa Victor NCS', mauSac: 'Trắng', soLuong: 1, donGia: 234000, thanhTien: 234000 }],
    thanhToan: [],
    lichSu: [
      { trangThai: 1, thoiGian: '2026-11-04T20:52:51' },
      { trangThai: 2, thoiGian: '2026-11-04T20:53:43' },
    ],
    daThanhToan: 0,
  }

  it('vẽ đủ các khối như màn admin, trạng thái sáng tới "Đã xác nhận" và gợi ý bước sau', async () => {
    auth.trangThai = reactive({ nguoiDung: { vaiTro: 'KHACH_HANG' } })
    api.get.mockResolvedValue({ data: { ...DON, maHoaDon: 'HD971', chiTietDon: CHI_TIET } })
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD971')
    const text = w.text()
    for (const khoi of ['Trạng thái đơn hàng', 'Tổng Kết Thanh Toán', 'Thông tin khách hàng', 'Thông tin giao hàng', 'Lịch sử thanh toán', 'Danh sách sản phẩm (1)']) {
      expect(text).toContain(khoi)
    }
    expect(text).toContain('SP53-TRG')
    expect(text).toContain('VCHE7KL4B')
    const buoc = w.findAll('.hd-step')
    expect(buoc.map((b) => b.attributes('data-buoc'))).toEqual(['1', '2', '3'])
    expect(buoc[1].classes()).toContain('is-current')
    expect(buoc[2].classes()).toContain('is-next')
    expect(w.find('[data-testid="hd-goi-y-buoc-sau"]').text()).toContain('Chờ giao hàng')
    expect(w.find('[data-chuyen]').exists()).toBe(false) // khách không có nút đổi trạng thái
  })

  it('đơn đã hủy / đã hoàn thành thì không gợi ý bước sau', async () => {
    auth.trangThai = reactive({ nguoiDung: { vaiTro: 'KHACH_HANG' } })
    for (const trangThai of [0, 6]) {
      const ct = { ...CHI_TIET, hoaDon: { ...CHI_TIET.hoaDon, trangThai } }
      api.get.mockResolvedValue({ data: { ...DON, chiTietDon: ct } })
      const w = mount(OrderLookupView)
      await flushPromises()
      await tra(w, 'HD971')
      expect(w.find('.hd-step.is-next').exists()).toBe(false)
      expect(w.find('[data-testid="hd-goi-y-buoc-sau"]').exists()).toBe(false)
    }
  })
})
