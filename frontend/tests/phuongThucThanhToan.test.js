import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { ma: 'HD888' } }),
  useRouter: () => ({ push: vi.fn() }),
}))
vi.mock('qrcode', () => ({ default: { toDataURL: async () => 'data:image/png;base64,AA' } }))
vi.mock('../src/composables/useXacNhan', () => ({ hoiXacNhan: vi.fn(async () => true) }))

import { docDonHang, ghiDonHang } from '../src/utils/donHangLocal'
import { useCartStore } from '../src/stores/cart'
import CheckoutView from '../src/views/client/CheckoutView.vue'
import OrderSuccessView from '../src/views/client/OrderSuccessView.vue'

describe('Hình thức thanh toán: COD, VietQR, MoMo, VNPay', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    api.get.mockReset()
    api.post.mockReset()
  })

  it('CheckoutView hiển thị đầy đủ 4 hình thức thanh toán: COD, VietQR, MoMo, VNPay', async () => {
    const cart = useCartStore()
    cart.items = [{ bienTheId: 52, sanPhamId: 1, sanPhamTen: 'Vợt Yonex', giaBan: 1000000, giaGoc: 1000000, soLuong: 1, soLuongTon: 10 }]
    api.get.mockImplementation(async (url) => {
      if (url === '/cua-hang/bao-gia') return { data: { tongTien: 1000000, thanhTien: 1000000 } }
      if (url === '/khach/phieu-giam-gia') return { data: [] }
      return { data: {} }
    })
    const w = mount(CheckoutView, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
          VoucherPanel: { template: '<div></div>' },
          BaseModal: { template: '<div><slot /><slot name="footer" /></div>' },
        },
      },
    })
    await flushPromises()

    const radios = w.findAll('input[name="payment"]')
    expect(radios).toHaveLength(4)
    const values = radios.map((r) => r.attributes('value'))
    expect(values).toContain('COD')
    expect(values).toContain('QR')
    expect(values).toContain('MOMO')
    expect(values).toContain('VNPAY')

    const text = w.text()
    expect(text).toContain('Thanh toán khi nhận hàng (COD)')
    expect(text).toContain('Chuyển khoản VietQR Ngân Hàng')
    expect(text).toContain('Ví điện tử MoMo')
    expect(text).toContain('Cổng thanh toán VNPay')
    w.unmount()
  })

  it('Bấm chọn MoMo / VNPay / VietQR: gửi đúng idPhuongThucThanhToan lên API đặt hàng', async () => {
    const cart = useCartStore()
    cart.items = [{ bienTheId: 52, sanPhamId: 1, sanPhamTen: 'Vợt Yonex', giaBan: 1000000, giaGoc: 1000000, soLuong: 1, soLuongTon: 10 }]
    api.get.mockImplementation(async (url) => {
      if (url === '/cua-hang/bao-gia') return { data: { tongTien: 1000000, thanhTien: 1000000 } }
      return { data: {} }
    })
    api.post.mockImplementation(async (url) => {
      if (url === '/cua-hang/tinh-tien') return { data: { tongTien: 1000000, thanhTien: 1000000, dong: [] } }
      if (url === '/cua-hang/dat-hang') return { data: { maHoaDon: 'HD888', tongTien: 1000000, soTienGiam: 0, phiVanChuyen: 0, thanhTien: 1000000 } }
      return { data: {} }
    })

    const w = mount(CheckoutView, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
          VoucherPanel: { template: '<div></div>' },
          BaseModal: { template: '<div><slot /><slot name="footer" /></div>' },
        },
      },
    })
    await flushPromises()

    // Chọn MoMo
    const momoRadio = w.find('input[name="payment"][value="MOMO"]')
    await momoRadio.setChecked()

    // Nhập thông tin bắt buộc
    w.vm.form.hoTen = 'Nguyễn Văn A'
    w.vm.form.sdt = '0901234567'
    w.vm.form.email = 'a@gmail.com'
    w.vm.form.diaChiCuThe = '123 Đường Láng'
    w.vm.form.tinhThanh = 'Thành phố Hà Nội'
    w.vm.form.phuongXa = 'Phường Láng Thượng'

    // Bấm nút đặt hàng trên giao diện
    await w.find('.co-submit').trigger('click')
    await flushPromises()

    // Bấm xác nhận trên modal
    const nutDongY = w.find('[data-testid="xn-dat-hang-dong-y"]')
    if (nutDongY.exists()) {
      await nutDongY.trigger('click')
      await flushPromises()
    }

    expect(api.post).toHaveBeenCalledWith(
      '/cua-hang/dat-hang',
      expect.objectContaining({
        idPhuongThucThanhToan: 6,
      })
    )
    w.unmount()
  })

  it('OrderSuccessView: đối với các hình thức quét mã (VietQR, MoMo, VNPay), hệ thống tự động kiểm tra trạng thái và KHÔNG có nút bấm hoàn thành thanh toán thủ công', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval', 'setTimeout'] })
    const LUC_DAT = new Date('2026-11-22T10:00:00')
    vi.setSystemTime(LUC_DAT)

    // Đơn MoMo
    ghiDonHang([{
      maHoaDon: 'HD888',
      ngayTao: LUC_DAT.toISOString(),
      sdtKhachHang: '0901234567',
      phuongThucThanhToan: 'MOMO',
      tongThanhToan: 500000,
    }])

    let daTraTien = false
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) {
        return { data: { diaChiLan: '192.168.1.10', daThanhToanDu: daTraTien } }
      }
      if (url === '/cua-hang/tra-cuu') {
        return { data: { maHoaDon: 'HD888', trangThai: 1, daThanhToanDu: daTraTien, idPhuongThucThanhToan: 6 } }
      }
      return { data: {} }
    })

    const w = mount(OrderSuccessView, {
      global: {
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })
    await flushPromises()

    // 1. Phải hiện tiêu đề quét mã
    expect(w.text()).toContain('Quét mã Ví điện tử MoMo để thanh toán')

    // 2. Phải có chỉ báo tự động kiểm tra trạng thái
    const dangCho = w.find('[data-testid="osv-dang-cho"]')
    expect(dangCho.exists()).toBe(true)
    expect(dangCho.text()).toContain('tự động kiểm tra')

    // 3. KHÔNG có nút hoàn tất / xác nhận thanh toán thủ công trên màn hình khách
    expect(w.text()).not.toMatch(/Tôi đã thanh toán|Hoàn tất thanh toán|Xác nhận đã thanh toán/i)

    // 4. Khi tiền vào (ngân hàng / ví xác nhận ngầm), polling tự động chuyển sang Thanh toán thành công
    daTraTien = true
    vi.advanceTimersByTime(4000)
    await flushPromises()

    expect(w.text()).toContain('Thanh toán thành công!')
    expect(w.text()).toContain('Hệ thống đã nhận được')
    w.unmount()
    vi.useRealTimers()
  })

  it('OrderSuccessView: hình thức VNPay cũng tự động kiểm tra và chuyển trạng thái thành công', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval', 'setTimeout'] })
    const LUC_DAT = new Date('2026-11-22T10:00:00')
    vi.setSystemTime(LUC_DAT)

    ghiDonHang([{
      maHoaDon: 'HD888',
      ngayTao: LUC_DAT.toISOString(),
      sdtKhachHang: '0901234567',
      phuongThucThanhToan: 'VNPAY',
      tongThanhToan: 750000,
    }])

    let daTraTien = false
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) {
        return { data: { diaChiLan: '192.168.1.10', daThanhToanDu: daTraTien } }
      }
      if (url === '/cua-hang/tra-cuu') {
        return { data: { maHoaDon: 'HD888', trangThai: 1, daThanhToanDu: daTraTien, idPhuongThucThanhToan: 4 } }
      }
      return { data: {} }
    })

    const w = mount(OrderSuccessView, {
      global: {
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })
    await flushPromises()

    expect(w.text()).toContain('Quét mã Cổng thanh toán VNPay để thanh toán')
    expect(w.find('[data-testid="osv-dang-cho"]').exists()).toBe(true)

    // Tự động nhận tiền qua polling 4s
    daTraTien = true
    vi.advanceTimersByTime(4000)
    await flushPromises()

    expect(w.text()).toContain('Thanh toán thành công!')
    w.unmount()
    vi.useRealTimers()
  })
})
