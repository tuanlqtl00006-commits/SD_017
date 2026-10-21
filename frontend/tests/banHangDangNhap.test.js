import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { api, replace, push, state } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn() },
  replace: vi.fn(),
  push: vi.fn(),
  state: { query: {} },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {}, query: state.query }),
  useRouter: () => ({ replace, push }),
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))

import BanHangPage from '../src/components/banHang/BanHangPage.vue'
import DangNhapPage from '../src/components/auth/DangNhapPage.vue'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'
import { _datLaiGiaoCa } from '../src/composables/useGiaoCa'
import { layPhien, xoaPhien } from '../src/services/phien'

const body = () => new DOMWrapper(document.body)
const loi = (message, code, extra = {}) => Object.assign(new Error(message), { code, ...extra })
let gan = []
const ganVao = (c) => {
  const w = mount(c, { attachTo: document.body })
  gan.push(w)
  return w
}

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  sessionStorage.clear()
  xoaPhien()
  _datLaiAuth()
  _datLaiGiaoCa()
  state.query = {}
  for (const f of [api.get, api.post, replace, push]) f.mockReset()
})
afterEach(() => {
  gan.forEach((w) => w.unmount())
  gan = []
})

/* ====================== Đăng nhập ====================== */

describe('Trang đăng nhập', () => {
  it('bỏ trống: báo lỗi dưới từng ô, không gọi máy chủ', async () => {
    const w = ganVao(DangNhapPage)
    await w.find('#dn-form').trigger('submit')
    expect(w.text()).toContain('Vui lòng nhập email, số điện thoại hoặc mã nhân viên.')
    expect(w.text()).toContain('Vui lòng nhập mật khẩu.')
    expect(api.post).not.toHaveBeenCalled()
  })

  it('đúng tài khoản: lưu phiên, vào trang chủ theo vai trò (hoặc trang đang mở dở)', async () => {
    api.post.mockResolvedValue({ data: { token: 't.k', hetHan: '2099-01-01T00:00:00', nguoiDung: { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' } } })
    const w = ganVao(DangNhapPage)
    await w.find('#dn-tai-khoan').setValue(' NV03 ')
    await w.find('#dn-mat-khau').setValue('Footstyle@123')
    await w.find('#dn-form').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/auth/dang-nhap', { taiKhoan: 'NV03', matKhau: 'Footstyle@123' })
    expect(layPhien().token).toBe('t.k')
    expect(useAuth().trangThai.nguoiDung.hoTen).toBe('Lê Quốc Bảo')
    expect(replace).toHaveBeenCalledWith('/ban-hang')

    state.query = { quayLai: '/hoa-don/5' }
    const w2 = ganVao(DangNhapPage)
    await w2.find('#dn-tai-khoan').setValue('NV03')
    await w2.find('#dn-mat-khau').setValue('Footstyle@123')
    await w2.find('#dn-form').trigger('submit')
    await flushPromises()
    expect(replace).toHaveBeenLastCalledWith('/hoa-don/5')
  })

  it('sai mật khẩu: báo lỗi chung, xóa ô mật khẩu', async () => {
    api.post.mockRejectedValue(loi('Tài khoản hoặc mật khẩu không đúng. Còn 2 lần thử trước khi bị khóa 5 phút.', 'SAI_TAI_KHOAN'))
    const w = ganVao(DangNhapPage)
    await w.find('#dn-tai-khoan').setValue('NV03')
    await w.find('#dn-mat-khau').setValue('sai')
    await w.find('#dn-form').trigger('submit')
    await flushPromises()
    expect(w.find('[role=alert]').text()).toContain('Còn 2 lần thử')
    expect(w.find('#dn-mat-khau').element.value).toBe('')
    expect(replace).not.toHaveBeenCalled()
  })
})

/* ====================== Bán tại quầy ====================== */

const SP = { id: 1, ma: 'SP01-DEN-4U-G5', tenSanPham: 'Yonex Astrox 99 Pro', thuongHieu: 'Yonex', phanLoai: 'Đen · 4U · G5', anh: null, giaGoc: 4290000, phanTramGiam: 10, donGia: 3861000, tonKho: 5 }
const CA = { id: 7, maGiaoCa: 'GC07', thoiGianNhanCa: '2026-10-16T07:00:00', tienDauCa: 2000000 }

function moBanHang({ dangMo = CA } = {}) {
  useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
  api.get.mockImplementation(async (url) => {
    if (url === '/giao-ca/hien-tai') return { data: { dangMo, tongKet: dangMo ? { soHoaDon: 0, doanhThu: 0 } : null, lichHomNay: [] } }
    if (url === '/ban-hang/san-pham') return { data: [SP] }
    if (url === '/ban-hang/phieu-giam-gia') return { data: [] }
    return { data: [] }
  })
  return ganVao(BanHangPage)
}

describe('Bán tại quầy', () => {
  it('chọn sản phẩm, nhập tiền khách đưa, xác nhận thì gửi đúng giỏ hàng lên máy chủ', async () => {
    const w = moBanHang()
    await flushPromises()
    await w.find('.bh-sp').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Yonex Astrox 99 Pro')
    expect(w.find('.bh-nut-tt').text()).toContain('3.861.000')
    await w.find('#bh-tien-mat').setValue('4000000')
    await flushPromises()
    expect(w.text()).toContain('Tiền thừa trả khách')
    expect(w.text()).toContain('139.000')

    api.post.mockResolvedValue({ data: { id: 9, maHoaDon: 'HD09', thanhTien: 3861000, tienThua: 139000, maGiaoCa: 'GC07' } })
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    const nut = body().findAll('.modal button').find((b) => b.text() === 'Thanh toán')
    await nut.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/ban-hang/thanh-toan', expect.objectContaining({
      idKhachHang: null, maPhieu: null, items: [{ idBienThe: 1, soLuong: 1, donGia: 3861000 }], tienMat: 4000000, chuyenKhoan: 0,
    }))
    expect(body().text()).toContain('Đã thanh toán HD09')
    expect(body().text()).toContain('Trả lại khách')
  })

  it('chưa nhận ca: không cho thanh toán, nhắc nhận ca', async () => {
    const w = moBanHang({ dangMo: null })
    await flushPromises()
    await w.find('.bh-sp').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Bạn chưa nhận ca.')
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(api.post).not.toHaveBeenCalled()
  })

  it('giá thay đổi lúc thanh toán: cập nhật giá mới trong hóa đơn', async () => {
    const w = moBanHang()
    await flushPromises()
    await w.find('.bh-sp').trigger('click')
    await w.find('#bh-tien-mat').setValue('5000000')
    await flushPromises()
    api.post.mockRejectedValue(loi('Giá sản phẩm vừa thay đổi.', 'GIA_THAY_DOI', { chiTiet: { items: [{ idBienThe: 1, donGiaCu: 3861000, donGiaMoi: 4290000 }] } }))
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await body().findAll('.modal button').find((b) => b.text() === 'Thanh toán').trigger('click')
    await flushPromises()
    expect(w.find('.bh-nut-tt').text()).toContain('4.290.000')
  })
})
