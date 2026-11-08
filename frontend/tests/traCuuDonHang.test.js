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


describe('Tra cứu đơn hàng theo số điện thoại', () => {
  const DS = { sdt: '0379992712', don: [
    { maHoaDon: 'HD967', ngayTao: '2026-11-04T07:36:00', loaiDon: 'Trực tuyến', trangThai: 1, tenTrangThai: 'Chờ xác nhận', thanhTien: 468000 },
    { maHoaDon: 'HD950', ngayTao: '2026-11-01T09:00:00', loaiDon: 'Tại quầy', trangThai: 6, tenTrangThai: 'Đã hoàn thành', thanhTien: 250000 },
  ] }
  // chỉ điền ô số điện thoại (ô mã đơn để trống) rồi bấm Tra cứu
  async function chuyenSdt(w, so) {
    await w.find('input[aria-label="Mã đơn hàng"]').setValue('')
    await w.find('[data-testid="ol-sdt"]').setValue(so)
    await w.find('form').trigger('submit')
    await flushPromises()
  }

  it('hai ô xếp hai dòng (mã đơn, số điện thoại), một nút Tra cứu; chỉ nhập số điện thoại thì không bắt nhập mã đơn', async () => {
    api.get.mockResolvedValue({ data: DS })
    const w = mount(OrderLookupView)
    await flushPromises()
    expect(w.findAll('form input')).toHaveLength(2)
    expect(w.findAll('form button[type="submit"]')).toHaveLength(1)
    expect(w.find('[data-testid="ol-tab-sdt"]').exists()).toBe(false)
    await chuyenSdt(w, '0379992712')
    expect(w.text()).not.toContain('Vui lòng nhập mã đơn hàng')
    expect(w.find('[data-testid="ol-ds-don"]').exists()).toBe(true)
  })

  it('để trống cả hai ô thì báo nhập mã đơn hoặc số điện thoại, không gọi máy chủ', async () => {
    const w = mount(OrderLookupView)
    await flushPromises()
    await w.find('form').trigger('submit')
    expect(api.get).not.toHaveBeenCalled()
    expect(w.text()).toContain('Vui lòng nhập mã đơn hàng hoặc số điện thoại')
  })

  it('nhập số điện thoại đúng thì ra danh sách đơn gọn, chưa lộ họ tên / địa chỉ; bấm "Xem chi tiết" mới mở đơn', async () => {
    api.get.mockImplementation((url, cfg) => {
      if (url === '/cua-hang/tra-cuu-sdt') return Promise.resolve({ data: DS })
      if (url === '/cua-hang/tra-cuu' && cfg.params.sdt === '0379992712' && cfg.params.maHoaDon === 'HD967') return Promise.resolve({ data: DON })
      return Promise.reject(loi404())
    })
    const w = mount(OrderLookupView)
    await flushPromises()
    await chuyenSdt(w, '0379992712')
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu-sdt', { params: { sdt: '0379992712' } })
    const bang = w.get('[data-testid="ol-ds-don"]').text()
    for (const chu of ['HD967', 'HD950', 'Chờ xác nhận', 'Đã hoàn thành', '468.000', '250.000']) expect(bang).toContain(chu)
    expect(w.text()).not.toContain('tuan134')
    await w.findAll('[data-testid="ol-xem-don"]')[0].trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu', { params: { maHoaDon: 'HD967', sdt: '0379992712' } })
    expect(w.text()).toContain('tuan134')
  })

  it('số điện thoại sai định dạng thì báo ngay, không gọi máy chủ; số không có đơn thì báo không tìm thấy', async () => {
    const w = mount(OrderLookupView)
    await flushPromises()
    await chuyenSdt(w, '1234')
    expect(api.get).not.toHaveBeenCalled()
    expect(w.find('[role="alert"]').exists()).toBe(true)
    api.get.mockRejectedValue(Object.assign(new Error('Không tìm thấy đơn hàng nào với số điện thoại này.'), { status: 404 }))
    await chuyenSdt(w, '0999999999')
    expect(w.text()).toContain('Không tìm thấy đơn hàng nào')
    expect(w.find('[data-testid="ol-ds-don"]').exists()).toBe(false)
  })

  it('nhập thêm mã đơn rồi tra thì xem đúng đơn đó và bỏ danh sách cũ', async () => {
    api.get.mockImplementation((url) => (url === '/cua-hang/tra-cuu-sdt' ? Promise.resolve({ data: DS }) : Promise.resolve({ data: DON })))
    const w = mount(OrderLookupView)
    await flushPromises()
    await chuyenSdt(w, '0379992712')
    expect(w.find('[data-testid="ol-ds-don"]').exists()).toBe(true)
    await tra(w, 'HD967', '0379992712')
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu', { params: { maHoaDon: 'HD967', sdt: '0379992712' } })
    expect(w.find('[data-testid="ol-ds-don"]').exists()).toBe(false)
    expect(w.text()).toContain('tuan134')
  })
})

describe('Tra cứu đơn hàng', () => {
  const DON_CHE = { ...DON, hoTenNguoiNhan: 'Đã ẩn', sdtNguoiNhan: 'Đã ẩn', diaChiGiaoHang: 'Đã ẩn', daChe: true, chiTietDon: {
    daChe: true,
    hoaDon: { maHoaDon: 'HD967', loaiDon: 2, trangThai: 1, tongTien: 468000, thanhTien: 468000 },
    khachHang: { ten: 'Đã ẩn', sdt: 'Đã ẩn', email: 'Đã ẩn' },
    giaoHang: { hoTenNguoiNhan: 'Đã ẩn', sdtNguoiNhan: 'Đã ẩn', diaChi: 'Đã ẩn' },
    sanPham: [{ id: 1, maSpct: 'SP53-TRG', tenSanPham: 'Vợt Yonex', soLuong: 1, donGia: 468000, thanhTien: 468000 }],
    thanhToan: [], lichSu: [{ trangThai: 1, thoiGian: '2026-11-04T20:52:51' }], daThanhToan: 0,
  } }

  it('chưa đăng nhập, chỉ nhập mã đơn: tra được ngay, không bắt SĐT, thông tin người mua bị che và có lời nhắc', async () => {
    api.get.mockResolvedValue({ data: DON_CHE })
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967')
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu', { params: { maHoaDon: 'HD967' } })
    expect(w.text()).not.toContain('Vui lòng nhập số điện thoại đặt hàng')
    expect(w.text()).toContain('Vợt Yonex')
    expect(w.text()).toContain('Đã ẩn')
    expect(w.find('[data-testid="ol-da-che"]').exists()).toBe(true)
  })

  it('chỉ nhập mã đơn mà không có đơn đó: báo không tìm thấy, không nhắc tới số điện thoại', async () => {
    api.get.mockRejectedValue(loi404())
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD999')
    expect(w.text()).toContain('Không tìm thấy đơn hàng "HD999"')
    expect(w.text()).not.toContain('với số điện thoại này')
  })

  it('có nhập số điện thoại thì vẫn phải đúng định dạng, không gọi máy chủ', async () => {
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967', '1234')
    expect(api.get).not.toHaveBeenCalled()
    expect(w.find('[role="alert"]').exists()).toBe(true)
  })

  it('mở từ đường dẫn ?ma= khi chưa đăng nhập: tự tra bằng mã, thông tin bị che', async () => {
    route.query = { ma: 'HD967' }
    api.get.mockResolvedValue({ data: DON_CHE })
    const w = mount(OrderLookupView)
    await flushPromises()
    expect(w.find('input[aria-label="Mã đơn hàng"]').element.value).toBe('HD967')
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu', { params: { maHoaDon: 'HD967' } })
    expect(w.find('[data-testid="ol-da-che"]').exists()).toBe(true)
  })

  it('đã đăng nhập, mã đơn không thuộc tài khoản: chuyển sang tra công khai, thông tin bị che', async () => {
    auth.trangThai = reactive({ nguoiDung: { vaiTro: 'KHACH_HANG' } })
    api.get.mockImplementation((url) => (url.startsWith('/khach/') ? Promise.reject(loi404()) : Promise.resolve({ data: DON_CHE })))
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD967')
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu', { params: { maHoaDon: 'HD967' } })
    expect(w.find('[data-testid="ol-da-che"]').exists()).toBe(true)
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

  // Review 05/11: đồng bộ phần tiền - chi tiết đơn có dòng giảm theo đợt, tên dòng giống mọi màn
  it('phần tiền: Tổng tiền hàng theo giá gốc, có Giảm theo đợt, Phiếu giảm giá, Phí vận chuyển, Tổng thanh toán', async () => {
    auth.trangThai = reactive({ nguoiDung: { vaiTro: 'KHACH_HANG' } })
    const ct = { ...CHI_TIET, hoaDon: { ...CHI_TIET.hoaDon, tienGiamTheoDot: 600000 } }
    api.get.mockResolvedValue({ data: { ...DON, chiTietDon: ct } })
    const w = mount(OrderLookupView)
    await flushPromises()
    await tra(w, 'HD971')
    const dong = (k) => w.get(`[data-dong="${k}"]`).text()
    expect(dong('tong-tien-hang')).toContain('Tổng tiền hàng')
    expect(dong('tong-tien-hang')).toContain('4.234.000') // 3.634.000 sau đợt + 600.000 giảm theo đợt
    expect(dong('giam-dot')).toContain('Giảm theo đợt')
    expect(dong('giam-dot')).toContain('600.000')
    expect(dong('giam-gia')).toContain('Phiếu giảm giá')
    expect(dong('phi-van-chuyen')).toContain('Miễn phí')
    expect(dong('tong-thanh-toan')).toContain('Tổng thanh toán')
    expect(dong('tong-thanh-toan')).toContain('3.384.000')
  })
})
