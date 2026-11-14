import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'

// Review 14/11: màn Cài đặt free ship (xem mức đang áp dụng, bấm Chỉnh sửa mới nhập), trang Ưu đãi riêng,
// và câu nhắc "Mua thêm ... để được miễn phí vận chuyển" ở giỏ hàng.
const { api, auth } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
  auth: { laKhach: false },
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/composables/useAuth', () => ({ useAuth: () => ({ laKhachHang: ref(auth.laKhach) }) }))

import CaiDatFreeShipPage from '../src/components/caiDat/CaiDatFreeShipPage.vue'
import UuDaiView from '../src/views/client/UuDaiView.vue'
import { useCartStore } from '../src/stores/cart'
import { nhanCaiDatVanChuyen } from '../src/services/caiDatVanChuyen'

const body = () => new DOMWrapper(document.body)
const RouterLinkGia = { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' }

beforeEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
  localStorage.clear()
  setActivePinia(createPinia())
  auth.laKhach = false
  nhanCaiDatVanChuyen({ mienPhiBat: false, mienPhiTu: 0 })
})

describe('Cài đặt free ship', () => {
  async function moMan(caiDat = { mienPhiBat: true, mienPhiTu: 2_000_000, capNhatLuc: null }) {
    api.get.mockResolvedValue({ data: caiDat })
    const w = mount(CaiDatFreeShipPage, { attachTo: document.body })
    await flushPromises()
    return w
  }

  it('mở màn chỉ xem mức đang áp dụng; bấm Chỉnh sửa mới hiện ô nhập, Hủy thì về lại mức cũ', async () => {
    const w = await moMan()
    expect(api.get).toHaveBeenCalledWith('/cai-dat/van-chuyen')
    expect(w.find('[data-testid="cd-trang-thai"]').text()).toBe('Đang bật')
    expect(w.find('[data-testid="cd-nguong-hien-tai"]').text()).toContain('2.000.000')
    expect(w.find('[data-testid="cd-cap-nhat"]').text()).toContain('Chưa chỉnh sửa')
    expect(w.find('#cd-nguong').exists()).toBe(false)

    await w.find('[data-testid="cd-sua"]').trigger('click')
    expect(w.find('#cd-nguong').element.value).toBe('2.000.000')
    expect(w.find('[data-testid="cd-luu"]').attributes('disabled')).toBeDefined() // chưa đổi gì thì chưa lưu được
    await w.find('#cd-nguong').setValue('5000000')
    expect(w.find('[data-testid="cd-vi-du"]').text()).toContain('5.000.000') // ví dụ tính theo mức đang nhập
    await w.find('[data-testid="cd-huy"]').trigger('click')
    expect(w.find('#cd-nguong').exists()).toBe(false)
    expect(w.find('[data-testid="cd-nguong-hien-tai"]').text()).toContain('2.000.000')
    expect(api.put).not.toHaveBeenCalled()
    w.unmount()
  })

  it('để trống / nhập 0 thì báo lỗi dưới ô và không hỏi lưu; nhập đúng thì hỏi lại rồi mới lưu, khối trên đổi theo', async () => {
    const w = await moMan()
    await w.find('[data-testid="cd-sua"]').trigger('click')
    await w.find('#cd-nguong').setValue('')
    await w.find('[data-testid="cd-form"]').trigger('submit')
    expect(w.find('[data-testid="cd-loi-nguong"]').text()).toBe('Vui lòng nhập giá trị đơn tối thiểu để được miễn phí vận chuyển.')
    await w.find('#cd-nguong').setValue('0')
    expect(w.find('[data-testid="cd-loi-nguong"]').text()).toBe('Giá trị đơn tối thiểu phải lớn hơn 0.')
    await w.find('[data-testid="cd-form"]').trigger('submit')
    expect(body().text()).not.toContain('Lưu cài đặt miễn phí vận chuyển?')

    await w.find('#cd-nguong').setValue('1000000')
    await w.find('[data-testid="cd-form"]').trigger('submit')
    await flushPromises()
    expect(body().text()).toContain('Lưu cài đặt miễn phí vận chuyển?')
    expect(body().text()).toContain('1.000.000')
    expect(api.put).not.toHaveBeenCalled() // chưa bấm xác nhận thì chưa ghi

    api.put.mockResolvedValue({ data: { mienPhiBat: true, mienPhiTu: 1_000_000, capNhatLuc: '2026-11-14T16:30:00' } })
    const nutLuu = body().findAll('button').find((b) => b.text() === 'Lưu')
    await nutLuu.trigger('click')
    await flushPromises()
    expect(api.put).toHaveBeenCalledWith('/cai-dat/van-chuyen', { mienPhiBat: true, mienPhiTu: 1_000_000 })
    expect(w.find('#cd-nguong').exists()).toBe(false) // lưu xong về lại chế độ xem
    expect(w.find('[data-testid="cd-nguong-hien-tai"]').text()).toContain('1.000.000')
    expect(w.find('[data-testid="cd-cap-nhat"]').text()).not.toContain('Chưa chỉnh sửa')
    w.unmount()
  })
})

describe('Trang Ưu đãi', () => {
  const PHIEU = [
    { id: 1, maPhieu: 'VCH10', tenPhieu: 'Giảm 10% vợt Yonex', hinhThucGiam: 1, giaTriGiam: 10, giamToiDa: 200000, donToiThieu: 1500000, ngayKetThuc: '2026-11-18T23:59:59' },
    { id: 2, maPhieu: 'VCH30K', tenPhieu: 'Giảm 30K', hinhThucGiam: 2, giaTriGiam: 30000, giamToiDa: null, donToiThieu: 0, ngayKetThuc: '2026-11-28T23:59:59' },
  ]
  const DOT = [{ id: 7, maDot: 'DGG7', tenDot: 'Sale giữa tháng', moTa: null, ngayBatDau: '2026-11-10T00:00:00', ngayKetThuc: '2026-11-20T23:59:59', phanTramGiamMax: 15, soSanPham: 4, idSanPham: [1, 2, 3, 4] }]

  function gia({ phieu = PHIEU, dot = DOT } = {}) {
    api.get.mockImplementation(async (url) => {
      if (url === '/cua-hang/phieu-giam-gia') return { data: phieu }
      if (url === '/cua-hang/dot-giam-gia') return { data: dot }
      if (url === '/khach/phieu-giam-gia') {
        return {
          data: [
            { ma: 'RIENG5', ten: 'Tặng riêng 5%', caNhan: true, loaiGiam: 1, giaTriGiam: 5, giamToiDa: null, donToiThieu: 0, ngayKetThuc: '2026-12-01T00:00:00', dungDuoc: true },
            { ma: 'HETHAN', ten: 'Đã hết hạn', caNhan: false, loaiGiam: 2, giaTriGiam: 10000, donToiThieu: 0, ngayKetThuc: '2026-11-01T00:00:00', dungDuoc: false, lyDo: 'Phiếu đã hết hạn.' },
          ],
        }
      }
      return { data: [] }
    })
  }
  const moTrang = async () => {
    const w = mount(UuDaiView, { attachTo: document.body, global: { components: { RouterLink: RouterLinkGia } } })
    await flushPromises()
    return w
  }

  it('khách chưa đăng nhập: thẻ phiếu công khai (mức giảm, điều kiện, hạn) và thẻ đợt đang chạy, bấm đợt mở đúng sản phẩm của đợt', async () => {
    gia()
    const w = await moTrang()
    const phieu = w.findAll('[data-testid="ud-the-phieu"]')
    expect(phieu).toHaveLength(2)
    expect(phieu[0].text()).toContain('10%')
    expect(phieu[0].text()).toContain('Giảm tối đa 200.000')
    expect(phieu[0].text()).toContain('Đơn từ 1.500.000')
    expect(phieu[0].text()).toContain('VCH10')
    expect(phieu[1].text()).toContain('Không yêu cầu giá trị đơn tối thiểu')
    const dot = w.findAll('[data-testid="ud-the-dot"]')
    expect(dot).toHaveLength(1)
    expect(dot[0].text()).toContain('Sale giữa tháng')
    expect(dot[0].text()).toContain('15%')
    expect(dot[0].text()).toContain('4 sản phẩm')
    expect(JSON.parse(dot[0].attributes('data-to'))).toEqual({ path: '/shop/san-pham', query: { dot: 7 } })
    w.unmount()
  })

  it('khách đã đăng nhập: thêm phiếu tặng riêng (xếp trước), không hiện phiếu hết hạn; chưa có ưu đãi thì ghi rõ', async () => {
    auth.laKhach = true
    gia()
    const w = await moTrang()
    const phieu = w.findAll('[data-testid="ud-the-phieu"]')
    expect(phieu).toHaveLength(1)
    expect(phieu[0].text()).toContain('Dành riêng cho bạn')
    expect(w.text()).not.toContain('HETHAN')
    w.unmount()

    auth.laKhach = false
    gia({ phieu: [], dot: [] })
    const trong = await moTrang()
    expect(trong.text()).toContain('Hiện chưa có phiếu giảm giá nào đang áp dụng.')
    expect(trong.text()).toContain('Hiện chưa có đợt giảm giá nào đang diễn ra.')
    trong.unmount()
  })

  it('bấm Sao chép mã thì chép đúng mã và nút báo đã sao chép', async () => {
    gia()
    const ghi = vi.fn().mockResolvedValue()
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: ghi }, configurable: true })
    const w = await moTrang()
    await w.find('button[data-ma="VCH30K"]').trigger('click')
    await flushPromises()
    expect(ghi).toHaveBeenCalledWith('VCH30K')
    expect(w.find('button[data-ma="VCH30K"]').text()).toBe('Đã sao chép')
    expect(w.find('button[data-ma="VCH10"]').text()).toBe('Sao chép mã')
    w.unmount()
  })
})

describe('Giỏ hàng: nhắc mua thêm để được miễn phí vận chuyển', () => {
  const dong = { bienTheId: 62, sanPhamId: 30, ten: 'Vợt', giaBan: 1_250_000, giaGoc: 1_250_000, soLuong: 1, soLuongTon: 27 }

  it('chưa đạt ngưỡng thì nhắc số còn thiếu; đạt rồi, hoặc giao Hà Nội (phí vốn bằng 0) thì không nhắc', () => {
    nhanCaiDatVanChuyen({ mienPhiBat: true, mienPhiTu: 2_000_000 })
    const cart = useCartStore()
    cart.addItem({ ...dong })
    expect(cart.conThieuMienPhiShip).toBe(750_000) // chưa chọn tỉnh
    cart.tinhGiao = 'Thành phố Đà Nẵng'
    expect(cart.shippingFee).toBe(40_000)
    expect(cart.conThieuMienPhiShip).toBe(750_000)
    cart.tinhGiao = 'Thành phố Hà Nội'
    expect(cart.shippingFee).toBe(0)
    expect(cart.conThieuMienPhiShip).toBe(0) // đã miễn phí theo tỉnh: không nhắc mua thêm
    cart.tinhGiao = 'Thành phố Đà Nẵng'
    cart.updateQuantity(62, 2)
    expect(cart.mienPhiShip).toBe(true)
    expect(cart.shippingFee).toBe(0)
    expect(cart.conThieuMienPhiShip).toBe(0)
  })

  it('cửa hàng tắt miễn phí theo giá trị đơn thì không nhắc', () => {
    nhanCaiDatVanChuyen({ mienPhiBat: false, mienPhiTu: 2_000_000 })
    const cart = useCartStore()
    cart.addItem({ ...dong })
    cart.tinhGiao = 'Thành phố Đà Nẵng'
    expect(cart.conThieuMienPhiShip).toBe(0)
  })
})
