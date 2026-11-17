import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('qrcode', () => ({ default: { toDataURL: async () => 'data:image/png;base64,mockqr' } }))

const maDonRoute = vi.hoisted(() => ({ value: 'HD87' }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { ma: maDonRoute.value } }),
  useRouter: () => ({ push: vi.fn() }),
}))

import GiaLapNganHangView from '../src/views/GiaLapNganHangView.vue'
import OrderSuccessView from '../src/views/client/OrderSuccessView.vue'
import HoaDonNoiDung from '../src/components/hoaDon/HoaDonNoiDung.vue'
import { ghiDonHang } from '../src/utils/donHangLocal'

describe('Yêu cầu hết hàng: ở điện thoại chỉ để chuyển tiền, web hiện thông báo hết hàng', () => {
  beforeEach(() => {
    localStorage.clear()
    api.get.mockReset()
    api.post.mockReset()
    maDonRoute.value = 'HD87'
  })

  describe('Giao diện điện thoại (GiaLapNganHangView)', () => {
    it('khi đơn bị hết hàng, điện thoại vẫn là app ngân hàng chỉ để chuyển tiền (không hiện cảnh báo chặn)', async () => {
      api.get.mockResolvedValueOnce({
        data: {
          maHoaDon: 'HD87',
          thanhTien: 4_000_000,
          conLai: 4_000_000,
          daThanhToanDu: false,
          laChuyenKhoan: true,
          daHuy: false,
          hetHang: true,
          lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: soau (SP15-VANG-4U-G6, kho còn 0, đơn cần 100)',
        },
      })

      const w = mount(GiaLapNganHangView)
      await flushPromises()

      // Không hiển thị màn cảnh báo chặn "Sản phẩm đã hết hàng" hay nút "Thử chuyển khoản"
      expect(w.text()).not.toContain('Thử chuyển khoản (Kiểm tra gửi mail hoàn tiền)')
      // Hiển thị giao diện chuyển tiền bình thường của ngân hàng
      expect(w.text()).toContain('Bạn sắp chuyển')
      expect(w.text()).toContain('4.000.000')
      expect(w.text()).toContain('FootStyle Sports')
      expect(w.text()).toContain('HD87')
      expect(w.text()).toContain('Xác nhận chuyển tiền')

      // Bấm chuyển tiền thành công
      api.post.mockResolvedValueOnce({
        data: {
          maHoaDon: 'HD87',
          thanhTien: 4_000_000,
          conLai: 0,
          daThanhToanDu: true,
          laChuyenKhoan: true,
          daHuy: false,
          hetHang: true,
        },
      })

      const nut = w.find('button.gl-btn')
      expect(nut.exists()).toBe(true)
      await nut.trigger('click')
      await flushPromises()

      expect(api.post).toHaveBeenCalledWith('/cua-hang/thanh-toan-gia-lap/HD87/xac-nhan')
      expect(w.text()).toContain('Chuyển tiền thành công')
      expect(w.text()).toContain('Màn hình đặt hàng sẽ tự cập nhật')
      w.unmount()
    })
  })

  describe('Giao diện web (OrderSuccessView)', () => {
    it('khi đơn hết hàng và chưa thanh toán: web hiện thông báo hết hàng và không quét QR', async () => {
      ghiDonHang([{ maHoaDon: 'HD87', phuongThucThanhToan: 'QR', tongThanhToan: 4_000_000, sdtKhachHang: '0901234567' }])
      api.get.mockImplementation(async (url) => {
        if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) {
          return {
            data: {
              maHoaDon: 'HD87',
              thanhTien: 4_000_000,
              conLai: 4_000_000,
              daThanhToanDu: false,
              hetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: soau (kho còn 0, đơn cần 100)',
            },
          }
        }
        if (url === '/cua-hang/tra-cuu') {
          return {
            data: {
              trangThai: 1,
              daThanhToanDu: false,
              hetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: soau (kho còn 0, đơn cần 100)',
            },
          }
        }
        return { data: {} }
      })

      const w = mount(OrderSuccessView, {
        global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
      })
      await flushPromises()

      expect(w.find('[data-testid="osv-het-hang-chua-tra"]').exists()).toBe(true)
      expect(w.text()).toContain('Sản phẩm trong đơn đã hết hàng')
      expect(w.text()).toContain('Vui lòng KHÔNG chuyển khoản cho đơn hàng này')
      expect(w.text()).toContain('soau (kho còn 0, đơn cần 100)')
      // Không hiện khung quét mã QR
      expect(w.find('img[alt="Mã QR thanh toán giả lập"]').exists()).toBe(false)
      w.unmount()
    })

    it('khi đơn hết hàng sau khi đã chuyển khoản: web hiện thông báo đã nhận thanh toán nhưng sản phẩm hết hàng', async () => {
      ghiDonHang([{ maHoaDon: 'HD87', phuongThucThanhToan: 'QR', tongThanhToan: 4_000_000, sdtKhachHang: '0901234567' }])
      api.get.mockImplementation(async (url) => {
        if (url.startsWith('/cua-hang/thanh-toan-gia-lap/')) {
          return {
            data: {
              maHoaDon: 'HD87',
              thanhTien: 4_000_000,
              conLai: 0,
              daThanhToanDu: true,
              hetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: soau (kho còn 0, đơn cần 100)',
            },
          }
        }
        if (url === '/cua-hang/tra-cuu') {
          return {
            data: {
              trangThai: 1,
              daThanhToanDu: true,
              hetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: soau (kho còn 0, đơn cần 100)',
            },
          }
        }
        return { data: {} }
      })

      const w = mount(OrderSuccessView, {
        global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
      })
      await flushPromises()

      expect(w.find('[data-testid="osv-da-huy-het-hang"]').exists()).toBe(true)
      expect(w.text()).toContain('Đã ghi nhận thanh toán nhưng sản phẩm vừa hết hàng')
      expect(w.text()).toContain('hoàn tiền 100%')
      expect(w.text()).toContain('0901234567')
      w.unmount()
    })
  })

  describe('Xem chi tiết đơn trên web (HoaDonNoiDung)', () => {
    it('chế độ khách: đơn hết hàng chưa thanh toán thì hiện cảnh báo và ẩn nút Tiếp tục thanh toán', () => {
      const w = mount(HoaDonNoiDung, {
        props: {
          cheDoKhach: true,
          data: {
            hoaDon: {
              maHoaDon: 'HD87',
              trangThai: 1,
              idPhuongThucThanhToan: 2,
              phuongThucThanhToan: 'Chuyển khoản QR',
              thanhTien: 4_000_000,
              hetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: soau (kho còn 0)',
            },
            daThanhToan: 0,
            hetHang: true,
          },
        },
      })

      expect(w.text()).toContain('Sản phẩm trong đơn hàng đã hết hàng do đơn hàng khác vừa thanh toán trước')
      expect(w.text()).toContain('Vui lòng không tiếp tục chuyển khoản cho đơn hàng này')
      expect(w.find('[data-testid="hd-tiep-tuc-thanh-toan"]').exists()).toBe(false)
      w.unmount()
    })

    it('chế độ khách: đơn hết hàng đã thanh toán thì hiện thông tin hỗ trợ hoàn tiền hoặc đổi sản phẩm', () => {
      const w = mount(HoaDonNoiDung, {
        props: {
          cheDoKhach: true,
          data: {
            hoaDon: {
              maHoaDon: 'HD87',
              trangThai: 1,
              idPhuongThucThanhToan: 2,
              phuongThucThanhToan: 'Chuyển khoản QR',
              thanhTien: 4_000_000,
              hetHang: true,
            },
            daThanhToan: 4_000_000,
            hetHang: true,
          },
        },
      })

      expect(w.text()).toContain('Đã nhận thanh toán 4.000.000')
      expect(w.text()).toContain('sản phẩm trong đơn vừa hết hàng tồn kho')
      expect(w.text()).toContain('hỗ trợ đổi sang sản phẩm khác hoặc hoàn tiền 100%')
      w.unmount()
    })

    it('chế độ nhân viên quản trị: đơn hết hàng chưa thanh toán hiện cảnh báo không đủ tồn kho và hướng dẫn báo khách', () => {
      const w = mount(HoaDonNoiDung, {
        props: {
          cheDoKhach: false,
          data: {
            hoaDon: {
              maHoaDon: 'HD97',
              trangThai: 0,
              idPhuongThucThanhToan: 1,
              thanhTien: 4_000_000,
              hetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: SP15-VANG-4U-G6 (kho còn 0, đơn cần 100)',
            },
            daThanhToan: 0,
            hetHang: true,
            duocBaoHetHang: true,
          },
        },
      })

      expect(w.text()).toContain('Sản phẩm trong đơn hàng đã hết hàng')
      expect(w.text()).toContain('SP15-VANG-4U-G6 (kho còn 0, đơn cần 100)')
      w.unmount()
    })

    it('bỏ bớt chữ rườm rà, không hiện alert info chiếm chỗ trên trang', () => {
      const w = mount(HoaDonNoiDung, {
        props: {
          cheDoKhach: false,
          data: {
            hoaDon: {
              maHoaDon: 'HD97',
              trangThai: 0,
              daBaoHetHang: true,
              lyDoHetHang: 'Sản phẩm trong đơn hàng đã hết hàng: SP15 (kho còn 0, đơn cần 10)',
            },
            daBaoHetHang: true,
            hetHang: true,
            daThanhToan: 0,
          },
        },
      })

      expect(w.text()).toContain('Sản phẩm trong đơn hàng đã hết hàng: SP15')
      expect(w.text()).not.toContain('Đơn vẫn giữ ở trạng thái Chờ xác nhận. Hãy bấm nút')
      expect(w.find('.alert-info').exists()).toBe(false)
      w.unmount()
    })
  })
})
