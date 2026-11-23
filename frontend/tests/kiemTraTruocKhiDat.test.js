import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'

// Cô test case khó 14/11 [13:04–14:57] + nhóm góp ý: bấm Đặt hàng thì kiểm tra lại phiếu / giá, KHÔNG tự đổi sau lưng khách.
// Có phiếu tốt hơn -> hỏi có muốn áp dụng không (không thì đặt như cũ); phiếu đang dùng hết hiệu lực -> hỏi đặt không dùng phiếu.
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
vi.mock('../src/services/phien', () => ({ layPhien: () => ({ nguoiDung: { id: 9, vaiTro: 'KHACH_HANG' } }) }))
vi.mock('../src/composables/useAuth', () => ({ useAuth: () => auth }))
vi.mock('../src/services/khachDiaChiService', () => ({ khachDiaChiService: diaChi }))
vi.mock('../src/components/shop/VoucherPanel.vue', () => ({ default: { template: '<div />' } }))

import CheckoutView from '../src/views/client/CheckoutView.vue'
import { useCartStore } from '../src/stores/cart'

const body = () => new DOMWrapper(document.body)
const PHIEU_200K = { ma: 'VCH200', ten: 'Giảm 200k', loaiGiam: 2, giaTriGiam: 200000, giamToiDa: null, donToiThieu: 0, dungDuoc: true, soTienGiam: 200000 }

function giaMayChu({ phieu = [], loiPhieu = () => null, mucGiam = () => 200000 } = {}) {
  api.get.mockImplementation(async (url) => (url === '/khach/phieu-giam-gia' ? { data: phieu } : { data: [] }))
  api.post.mockImplementation(async (url, b) => {
    if (url === '/cua-hang/tinh-tien') {
      const loi = b.maPhieuGiamGia ? loiPhieu(b.maPhieuGiamGia) : null
      const giam = b.maPhieuGiamGia && !loi ? mucGiam() : 0
      return { data: {
        dong: [{ idSanPhamChiTiet: 52, conBan: true, giaGoc: 1000000, donGia: 1000000, soLuong: 1, soLuongTon: 9 }],
        tongTienGoc: 1000000, tienGiamTheoDot: 0, tongTien: 1000000, maPhieu: giam ? b.maPhieuGiamGia : null,
        soTienGiam: giam, chotPhieu: giam ? `ky-${giam}` : null, loiPhieu: loi, phiVanChuyen: 0, thanhTien: 1000000 - giam,
      } }
    }
    if (url === '/cua-hang/dat-hang') return { data: { maHoaDon: 'HD999', tongTien: 1000000, soTienGiam: 0, phiVanChuyen: 0, thanhTien: 1000000 } }
    return { data: {} }
  })
}

async function moTrang(phieuDangDung = null) {
  auth.trangThai = reactive({ nguoiDung: { id: 9, vaiTro: 'KHACH_HANG', hoTen: 'lê quốc tuấn', sdt: '0379992712', email: 'a@gmail.com' } })
  const cart = useCartStore()
  cart.items.splice(0)
  cart.items.push({ bienTheId: 52, sanPhamTen: 'Vợt test', giaBan: 1000000, giaGoc: 1000000, soLuong: 1, soLuongTon: 9 })
  if (phieuDangDung) cart.setVoucher(phieuDangDung)
  const w = mount(CheckoutView, { attachTo: document.body })
  await new Promise((r) => setTimeout(r, 300))
  await flushPromises()
  return { w, cart }
}
const bamDatHang = async (w) => {
  await w.findAll('button').find((b) => b.text().includes('Đặt hàng')).trigger('click')
  await flushPromises()
  const nutXn = body().find('[data-testid="xn-dat-hang-dong-y"]')
  if (nutXn.exists()) {
    await nutXn.trigger('click')
    await flushPromises()
  }
}
const lenhDat = () => api.post.mock.calls.filter((c) => c[0] === '/cua-hang/dat-hang')
const hop = () => body().find('[data-testid="hoi-truoc-khi-dat"]')
const nutTrongHop = (chu) => hop().findAll('button').find((b) => b.text().includes(chu))

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  setActivePinia(createPinia())
  Object.values(api).forEach((f) => f.mockReset())
  Object.values(diaChi).forEach((f) => f.mockReset())
  diaChi.getAll.mockResolvedValue([
    { id: 1, tenNguoiNhan: 'lê quốc tuấn', sdtNguoiNhan: '0379992712', tinhThanhPho: 'Thành phố Hà Nội', phuongXa: 'Phường Cửa Nam', diaChiCuThe: 'so 1', laDiaChiMacDinh: true },
  ])
})

describe('Kiểm tra lại phiếu / giá khi bấm Đặt hàng', () => {
  it('không có gì thay đổi: đặt luôn, không hỏi', async () => {
    giaMayChu()
    const { w } = await moTrang()
    await bamDatHang(w)
    expect(hop().exists()).toBe(false)
    expect(lenhDat()).toHaveLength(1)
  })

  it('có phiếu tốt hơn khi chưa dùng phiếu: hỏi trước; chọn "Không" thì đặt không dùng phiếu', async () => {
    giaMayChu({ phieu: [PHIEU_200K] })
    const { w, cart } = await moTrang()
    await bamDatHang(w)
    expect(lenhDat()).toHaveLength(0)
    expect(hop().text()).toContain('VCH200')
    expect(hop().text()).toContain('Bạn có muốn áp dụng không?')
    await nutTrongHop('Không').trigger('click')
    await flushPromises()
    expect(lenhDat()).toHaveLength(1)
    expect(lenhDat()[0][1].maPhieuGiamGia).toBeNull()
    expect(cart.appliedVoucher).toBeNull()
  })

  it('có phiếu tốt hơn: chọn "Áp dụng" thì gửi đơn kèm phiếu đó', async () => {
    giaMayChu({ phieu: [PHIEU_200K] })
    const { w } = await moTrang()
    await bamDatHang(w)
    await nutTrongHop('Áp dụng VCH200').trigger('click')
    await flushPromises()
    expect(lenhDat()).toHaveLength(1)
    expect(lenhDat()[0][1].maPhieuGiamGia).toBe('VCH200')
  })

  // Góp ý 23/11 (Task 2.1-2.3): chỉ 2 nút Áp dụng / Hủy như bên quầy
  it('phiếu đang dùng hết hiệu lực, không có phiếu thay: "Hủy" gỡ phiếu, ở lại trang; "Áp dụng" đặt không dùng phiếu', async () => {
    giaMayChu({ loiPhieu: (ma) => (ma === 'HETHAN' ? 'Phiếu đã hết hạn sử dụng.' : null) })
    const { w, cart } = await moTrang({ ma: 'HETHAN', ten: 'Phiếu cũ', loaiGiam: 2, giaTriGiam: 200000, donToiThieu: 0 })
    await bamDatHang(w)
    expect(hop().text()).toContain('Phiếu HETHAN không còn dùng được')
    expect(hop().text()).toContain('Phiếu đã hết hạn sử dụng.')
    expect(hop().findAll('button').map((b) => b.text())).toEqual(['Áp dụng', 'Hủy'])
    await nutTrongHop('Hủy').trigger('click')
    await flushPromises()
    expect(hop().exists()).toBe(false)
    expect(lenhDat()).toHaveLength(0)
    expect(cart.appliedVoucher).toBeNull()
    expect(w.findAll('button').find((b) => b.text().includes('Đặt hàng')).attributes('disabled')).toBeUndefined()

    await bamDatHang(w)
    expect(lenhDat()).toHaveLength(1)
    expect(lenhDat()[0][1].maPhieuGiamGia).toBeNull()
  })

  it('phiếu hết hiệu lực, không có phiếu thay: "Áp dụng" bỏ phiếu rồi đặt', async () => {
    giaMayChu({ loiPhieu: (ma) => (ma === 'HETHAN' ? 'Phiếu đã hết hạn sử dụng.' : null) })
    const { w } = await moTrang({ ma: 'HETHAN', ten: 'Phiếu cũ', loaiGiam: 2, giaTriGiam: 200000, donToiThieu: 0 })
    await bamDatHang(w)
    await nutTrongHop('Áp dụng').trigger('click')
    await flushPromises()
    expect(lenhDat()).toHaveLength(1)
    expect(lenhDat()[0][1].maPhieuGiamGia).toBeNull()
  })

  it('phiếu hết hiệu lực mà có phiếu khác: "Hủy" gỡ phiếu cũ, không đặt, không tự áp phiếu khác', async () => {
    giaMayChu({ phieu: [PHIEU_200K], loiPhieu: (ma) => (ma === 'HETHAN' ? 'Phiếu đã hết hạn sử dụng.' : null) })
    const { w, cart } = await moTrang({ ma: 'HETHAN', ten: 'Phiếu cũ', loaiGiam: 2, giaTriGiam: 200000, donToiThieu: 0 })
    await bamDatHang(w)
    await nutTrongHop('Hủy').trigger('click')
    await flushPromises()
    expect(lenhDat()).toHaveLength(0)
    expect(cart.appliedVoucher).toBeNull()
  })

  it('phiếu hết hiệu lực mà có phiếu khác: "Áp dụng" lấy phiếu mới rồi đặt', async () => {
    giaMayChu({ phieu: [PHIEU_200K], loiPhieu: (ma) => (ma === 'HETHAN' ? 'Phiếu đã hết hạn sử dụng.' : null) })
    const { w } = await moTrang({ ma: 'HETHAN', ten: 'Phiếu cũ', loaiGiam: 2, giaTriGiam: 200000, donToiThieu: 0 })
    await bamDatHang(w)
    expect(hop().findAll('button').map((b) => b.text())).toEqual(['Áp dụng', 'Hủy'])
    await nutTrongHop('Áp dụng').trigger('click')
    await flushPromises()
    expect(lenhDat()).toHaveLength(1)
    expect(lenhDat()[0][1].maPhieuGiamGia).toBe('VCH200')
  })

  // Góp ý 19/11 (thay cho 17/11 [19:31] "Hủy = bỏ phiếu"): phiếu bị sửa khi khách đang thanh toán thì hỏi lại như bên quầy, Hủy giữ nguyên
  async function phieuBiSua() {
    let giam = 200000
    const phieuSua = { ma: 'VCH200', ten: 'Giảm 100k', loaiGiam: 2, giaTriGiam: 100000, giamToiDa: null, donToiThieu: 0, dungDuoc: true, soTienGiam: 100000 }
    giaMayChu({ phieu: [phieuSua], mucGiam: () => giam })
    const kq = await moTrang({ ma: 'VCH200', ten: 'Giảm 200k', loaiGiam: 2, giaTriGiam: 200000, donToiThieu: 0 })
    expect(kq.cart.discountAmount).toBe(200000)
    giam = 100000 // quản lý sửa phiếu 200k -> 100k
    await kq.cart.taiBaoGia()
    expect(kq.cart.discountAmount).toBe(200000) // trang không tự đổi số
    expect(kq.cart.finalTotal).toBe(800000)
    await bamDatHang(kq.w)
    expect(lenhDat()).toHaveLength(0)
    expect(hop().text()).toContain('Phiếu VCH200 vừa đổi mức giảm')
    expect(hop().text()).toContain('200.000')
    expect(hop().text()).toContain('100.000')
    expect(hop().findAll('button').map((b) => b.text())).toEqual(['Áp dụng', 'Hủy'])
    return kq
  }

  it('phiếu bị sửa mức giảm: trang giữ số đã thấy, hỏi lại; bấm Hủy thì ở lại trang, chưa đặt (bỏ "giữ nguyên mức cũ")', async () => {
    const { w } = await phieuBiSua()
    await nutTrongHop('Hủy').trigger('click')
    await flushPromises()
    expect(hop().exists()).toBe(false)
    expect(lenhDat()).toHaveLength(0)
    expect(w.findAll('button').find((b) => b.text().includes('Đặt hàng')).attributes('disabled')).toBeUndefined()
  })

  it('phiếu bị sửa mức giảm: bấm Áp dụng thì đặt theo mức mới', async () => {
    await phieuBiSua()
    await nutTrongHop('Áp dụng').trigger('click')
    await flushPromises()
    expect(lenhDat()).toHaveLength(1)
    expect(lenhDat()[0][1]).toMatchObject({ maPhieuGiamGia: 'VCH200', soTienGiamDaThay: 100000, giuNguyenGiamCu: null, chotPhieu: 'ky-100000' })
  })

  it('bắt buộc nhập email nhận thông báo đơn hàng cho cả khách đăng nhập và không đăng nhập', async () => {
    giaMayChu()
    const { w } = await moTrang()
    // Xóa email
    await w.find('input[type="email"]').setValue('')
    await w.findAll('button').find((b) => b.text().includes('Đặt hàng')).trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Vui lòng nhập email nhận thông báo đơn hàng.')
    expect(lenhDat()).toHaveLength(0)
  })
})
