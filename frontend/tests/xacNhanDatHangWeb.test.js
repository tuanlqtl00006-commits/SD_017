import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'

const { api, auth, diaChi, toast } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn() },
  auth: { trangThai: null, khoiTao: async () => {} },
  diaChi: { getAll: vi.fn(), create: vi.fn() },
  toast: { success: vi.fn(), error: vi.fn() },
}))

const pushMock = vi.hoisted(() => vi.fn())
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: pushMock }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/phien', () => ({ layPhien: () => ({ nguoiDung: { id: 10, vaiTro: 'KHACH_HANG' } }) }))
vi.mock('../src/composables/useAuth', () => ({ useAuth: () => auth }))
vi.mock('../src/composables/useToast', () => ({ useToast: () => toast }))
vi.mock('../src/services/khachDiaChiService', () => ({ khachDiaChiService: diaChi }))
vi.mock('../src/components/shop/VoucherPanel.vue', () => ({ default: { template: '<div />' } }))

import CheckoutView from '../src/views/client/CheckoutView.vue'
import { useCartStore } from '../src/stores/cart'

const body = () => new DOMWrapper(document.body)

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  setActivePinia(createPinia())
  Object.values(api).forEach((f) => f.mockReset())
  Object.values(diaChi).forEach((f) => f.mockReset())
  Object.values(toast).forEach((f) => f.mockReset())
  pushMock.mockReset()

  auth.trangThai = reactive({
    nguoiDung: { id: 10, vaiTro: 'KHACH_HANG', hoTen: 'Nguyễn Văn A', sdt: '0901234567', email: 'vana@gmail.com' },
  })
  diaChi.getAll.mockResolvedValue([
    {
      id: 1,
      tenNguoiNhan: 'Nguyễn Văn A',
      sdtNguoiNhan: '0901234567',
      tinhThanhPho: 'Thành phố Hà Nội',
      phuongXa: 'Phường Dịch Vọng Hậu',
      diaChiCuThe: 'Số 10 Phạm Hùng',
      laDiaChiMacDinh: true,
    },
  ])

  api.get.mockImplementation(async (url) => {
    if (url === '/khach/phieu-giam-gia') return { data: [] }
    return { data: [] }
  })
  api.post.mockImplementation(async (url) => {
    if (url === '/cua-hang/tinh-tien') {
      return {
        data: {
          dong: [{ idSanPhamChiTiet: 101, conBan: true, giaGoc: 2000000, donGia: 2000000, soLuong: 2, soLuongTon: 10 }],
          tongTienGoc: 4000000,
          tienGiamTheoDot: 0,
          tongTien: 4000000,
          maPhieu: null,
          soTienGiam: 0,
          loiPhieu: null,
          phiVanChuyen: 0,
          thanhTien: 4000000,
        },
      }
    }
    if (url === '/cua-hang/dat-hang') {
      return {
        data: {
          maHoaDon: 'HD88',
          tongTien: 4000000,
          soTienGiam: 0,
          phiVanChuyen: 0,
          thanhTien: 4000000,
        },
      }
    }
    return { data: {} }
  })
})

describe('Thông báo xác nhận khi đặt hàng ở web', () => {
  it('bấm Đặt hàng: hiện hộp thoại xác nhận với đầy đủ thông tin đơn hàng; bấm Hủy thì không đặt', async () => {
    const cart = useCartStore()
    cart.items.splice(0)
    cart.items.push({ bienTheId: 101, sanPhamTen: 'Vợt cầu lông Yonex', giaBan: 2000000, giaGoc: 2000000, soLuong: 2, soLuongTon: 10 })

    const w = mount(CheckoutView, { attachTo: document.body })
    await new Promise((r) => setTimeout(r, 200))
    await flushPromises()

    // Bấm nút Đặt hàng trên trang web
    const nutDat = w.findAll('button').find((b) => b.text().includes('Đặt hàng'))
    await nutDat.trigger('click')
    await flushPromises()

    // Kiểm tra hộp thoại xác nhận xuất hiện
    const modalXn = body().find('[data-testid="xac-nhan-dat-hang"]')
    expect(modalXn.exists()).toBe(true)
    expect(modalXn.text()).toContain('Xác nhận thông tin đặt hàng')
    expect(modalXn.text()).toContain('Nguyễn Văn A · 0901234567')
    expect(modalXn.text()).toContain('Số 10 Phạm Hùng, Phường Dịch Vọng Hậu, Thành phố Hà Nội')
    expect(modalXn.text()).toContain('2 sản phẩm')
    expect(modalXn.text()).toContain('4.000.000')

    // Bấm Hủy trong hộp thoại
    const nutHuy = body().findAll('button').find((b) => b.text().trim() === 'Hủy')
    expect(nutHuy?.exists()).toBe(true)
    await nutHuy.trigger('click')
    await flushPromises()

    // Hộp thoại đóng lại, không gọi API đặt hàng
    expect(body().find('[data-testid="xac-nhan-dat-hang"]').exists()).toBe(false)
    expect(api.post.mock.calls.some((c) => c[0] === '/cua-hang/dat-hang')).toBe(false)
    w.unmount()
  })

  it('bấm Xác nhận đặt hàng trong hộp thoại: gửi đơn, hiện toast thông báo thành công và chuyển trang', async () => {
    const cart = useCartStore()
    cart.items.splice(0)
    cart.items.push({ bienTheId: 101, sanPhamTen: 'Vợt cầu lông Yonex', giaBan: 2000000, giaGoc: 2000000, soLuong: 2, soLuongTon: 10 })

    const w = mount(CheckoutView, { attachTo: document.body })
    await new Promise((r) => setTimeout(r, 200))
    await flushPromises()

    // Bấm nút Đặt hàng
    const nutDat = w.findAll('button').find((b) => b.text().includes('Đặt hàng'))
    await nutDat.trigger('click')
    await flushPromises()

    // Bấm Xác nhận đặt hàng trong hộp thoại xác nhận
    const nutDongY = body().find('[data-testid="xn-dat-hang-dong-y"]')
    expect(nutDongY.exists()).toBe(true)
    await nutDongY.trigger('click')
    await flushPromises()

    // Kiểm tra gọi API đặt hàng
    expect(api.post.mock.calls.some((c) => c[0] === '/cua-hang/dat-hang')).toBe(true)
    // Kiểm tra hiện toast thông báo thành công
    expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('HD88'), expect.any(String))
    w.unmount()
  })
})
