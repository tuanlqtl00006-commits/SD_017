import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useCart } from '../src/composables/useCart'
import CuaHangHomePage from '../src/components/cuaHang/CuaHangHomePage.vue'
import CuaHangThanhToanPage from '../src/components/cuaHang/CuaHangThanhToanPage.vue'

// Mock global fetch
const mockFetch = vi.fn()
global.fetch = mockFetch

describe('Bán Hàng Online Chuẩn ShopVNB / FootStyle', () => {
  let router

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    const cart = useCart()
    cart.lamTrong()

    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/shop', component: CuaHangHomePage },
        { path: '/thanh-toan', component: CuaHangThanhToanPage },
      ],
    })
  })

  it('useCart: thêm món, tính tổng tiền, tăng giảm và xóa món trong giỏ', () => {
    const cart = useCart()
    expect(cart.items.value.length).toBe(0)

    const sp = { id: 1, tenSanPham: 'Vợt Yonex Astrox 99 Pro', anhChinh: '/images/san-pham/sp01_astrox99_den.svg' }
    const bt = { id: 101, maSpct: 'SP01-DEN-3U', tenMau: 'Đen', trongLuong: '3U', chuVi: 'G5', giaBan: 3500000, giaBanGoc: 3900000, soLuongTon: 10 }

    cart.them(sp, bt, 1)
    expect(cart.items.value.length).toBe(1)
    expect(cart.tongSoLuong.value).toBe(1)
    expect(cart.tongTien.value).toBe(3500000)

    // Thêm tiếp cùng biến thể
    cart.them(sp, bt, 2)
    expect(cart.items.value[0].soLuong).toBe(3)
    expect(cart.tongSoLuong.value).toBe(3)
    expect(cart.tongTien.value).toBe(10500000)

    // Sửa số lượng
    cart.suaSoLuong(101, 2)
    expect(cart.items.value[0].soLuong).toBe(2)

    // Xóa món
    cart.xoa(101)
    expect(cart.items.value.length).toBe(0)
    expect(cart.tongTien.value).toBe(0)
  })

  it('CuaHangHomePage: hiển thị banner VNB, danh mục cầu lông và danh sách sản phẩm', async () => {
    mockFetch.mockImplementation(async (url) => {
      if (url.includes('/api/cua-hang/danh-muc-va-thuong-hieu')) {
        return {
          ok: true,
          json: async () => ({
            danhMuc: [{ id: 1, ten: 'Vợt cầu lông' }, { id: 2, ten: 'Giày cầu lông' }],
            thuongHieu: [{ id: 1, ten: 'Yonex' }, { id: 2, ten: 'Lining' }],
          }),
        }
      }
      if (url.includes('/api/cua-hang/khuyen-mai')) {
        return {
          ok: true,
          json: async () => [
            {
              id: 1,
              tenSanPham: 'Vợt Yonex Astrox 99 Pro',
              tenThuongHieu: 'Yonex',
              anhChinh: '/images/san-pham/sp01_astrox99_den.svg',
              giaBanMin: 3500000,
              giaGocMin: 3900000,
              phanTramGiamMax: 10,
            },
          ],
        }
      }
      if (url.includes('/api/cua-hang/san-pham')) {
        return {
          ok: true,
          json: async () => [
            {
              id: 1,
              maSanPham: 'SP01',
              tenSanPham: 'Vợt Yonex Astrox 99 Pro',
              tenDanhMuc: 'Vợt cầu lông',
              tenThuongHieu: 'Yonex',
              anhChinh: '/images/san-pham/sp01_astrox99_den.svg',
              giaBanMin: 3500000,
              giaGocMin: 3900000,
              phanTramGiamMax: 10,
              bienThe: [
                { id: 101, maSpct: 'SP01-DEN-3U', tenMau: 'Đen', trongLuong: '3U', chuVi: 'G5', giaBan: 3500000, soLuongTon: 5 },
              ],
            },
          ],
        }
      }
      return { ok: false }
    })

    const wrapper = mount(CuaHangHomePage, {
      global: { plugins: [router] },
    })

    // Đợi fetch hoàn tất
    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // Kiểm tra các phần tử thương hiệu FootStyle / VNB
    expect(wrapper.text()).toContain('SIÊU SALE CẦU LÔNG FOOTSTYLE')
    expect(wrapper.text()).toContain('100% CHÍNH HÃNG')
    expect(wrapper.text()).toContain('CĂNG CƯỚC CHUẨN')
    expect(wrapper.text()).toContain('DANH SÁCH SẢN PHẨM CẦU LÔNG')
    expect(wrapper.text()).toContain('Vợt Yonex Astrox 99 Pro')
  })

  it('CuaHangThanhToanPage: kiểm tra validate họ tên, SĐT 10 số và địa chỉ', async () => {
    mockFetch.mockImplementation(async (url) => {
      if (url.includes('/api/cua-hang/phieu-giam-gia')) {
        return { ok: true, json: async () => [] }
      }
      return { ok: false }
    })

    const cart = useCart()
    cart.them(
      { id: 1, tenSanPham: 'Vợt Astrox' },
      { id: 101, maSpct: 'SP01-3U', giaBan: 2000000, soLuongTon: 5 },
      1,
    )

    const wrapper = mount(CuaHangThanhToanPage, {
      global: { plugins: [router] },
    })

    await wrapper.vm.$nextTick()

    // Bấm đặt hàng khi chưa nhập thông tin -> báo lỗi
    const btnDat = wrapper.find('button.vnb-btn-hero')
    expect(btnDat.exists()).toBe(true)
    await btnDat.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Vui lòng nhập họ và tên')
    expect(wrapper.text()).toContain('Vui lòng nhập số điện thoại')
  })
})
