import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { api, replace, push, state } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
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
import KhachDangNhapView from '../src/views/client/KhachDangNhapView.vue'
import VaoCaPage from '../src/components/giaoCa/VaoCaPage.vue'
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
  for (const f of [api.get, api.post, api.put, api.delete, replace, push]) f.mockReset()
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
    expect(w.text()).not.toContain('Vui lòng nhập mật khẩu.')
    expect(replace).not.toHaveBeenCalled()
    expect(document.activeElement).toBe(w.find('#dn-mat-khau').element)
  })

  it('lỗi gắn với đúng ô (aria-invalid, aria-describedby) và con trỏ về ô lỗi đầu tiên', async () => {
    const w = ganVao(DangNhapPage)
    await w.find('#dn-mat-khau').setValue('abc')
    await w.find('#dn-form').trigger('submit')
    await flushPromises()
    const tk = w.find('#dn-tai-khoan')
    expect(tk.attributes('aria-invalid')).toBe('true')
    expect(tk.attributes('aria-describedby')).toBe('dn-tai-khoan-loi')
    expect(w.find('#dn-tai-khoan-loi').text()).toBe('Vui lòng nhập email, số điện thoại hoặc mã nhân viên.')
    expect(w.find('#dn-mat-khau').attributes('aria-invalid')).toBe('false')
    expect(w.find('#dn-mat-khau').attributes('aria-describedby')).toBeUndefined()
    expect(document.activeElement).toBe(tk.element)
  })

  it('chuẩn form: name / autocomplete cố định, "Ghi nhớ" mặc định không tích', () => {
    const w = ganVao(DangNhapPage)
    expect(w.find('#dn-tai-khoan').attributes()).toMatchObject({ name: 'username', autocomplete: 'username' })
    expect(w.find('#dn-mat-khau').attributes()).toMatchObject({ name: 'password', autocomplete: 'current-password' })
    expect(w.find('input[name=remember]').element.checked).toBe(false)
  })

  it('trang quản trị chỉ cho nhân viên / quản lý; khách hàng sang trang đăng nhập riêng', async () => {
    const w = ganVao(DangNhapPage)
    expect(w.find('[role=tablist]').exists()).toBe(false)
    expect(w.text()).not.toContain('Thu ngân')
    await w.find('[data-testid=dn-sang-khach]').trigger('click')
    expect(push).toHaveBeenCalledWith('/shop/dang-nhap')
  })

  it('trang đăng nhập khách: gọi API đăng nhập khách hàng rồi về cửa hàng', async () => {
    api.post.mockResolvedValue({ data: { token: 'k.h', hetHan: '2099-01-01T00:00:00', nguoiDung: { id: 5, hoTen: 'Khách A', vaiTro: 'KHACH_HANG' } } })
    const w = ganVao(KhachDangNhapView)
    await w.find('#kh-form').trigger('submit')
    expect(w.text()).toContain('Vui lòng nhập email hoặc số điện thoại.')
    expect(api.post).not.toHaveBeenCalled()
    await w.find('#kh-tai-khoan').setValue('khach@gmail.com')
    await w.find('#kh-mat-khau').setValue('123456')
    await w.find('#kh-form').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/auth/khach-hang/dang-nhap', { taiKhoan: 'khach@gmail.com', matKhau: '123456' })
    expect(replace).toHaveBeenCalledWith('/shop')
  })

  it('đăng ký khách gọi API đăng ký công khai (không cần đăng nhập), không gọi API quản lý khách hàng', async () => {
    api.post.mockResolvedValue({ data: { maKhachHang: 'KH30', hoTen: 'Lê Quốc Tuấn', sdt: '0379992712' } })
    const w = ganVao(KhachDangNhapView)
    const tab = w.findAll('button').find((b) => b.text().includes('Đăng ký'))
    await tab.trigger('click')
    const o = w.findAll('form').at(-1).findAll('input')
    await o[0].setValue('Lê Quốc Tuấn')
    await o[1].setValue('0379992712')
    await o[3].setValue('abc123')
    await o[4].setValue('abc123')
    await w.findAll('form').at(-1).trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/auth/khach-hang/dang-ky', { hoTen: 'Lê Quốc Tuấn', sdt: '0379992712', email: undefined, matKhau: 'abc123', xacNhanMatKhau: 'abc123' })
    expect(api.post).not.toHaveBeenCalledWith('/khach-hang', expect.anything())
  })

  it('trang đăng nhập không hiện khối tài khoản demo, ô tài khoản / mật khẩu để trống', () => {
    const w = ganVao(DangNhapPage)
    expect(w.text()).not.toContain('Tài khoản demo')
    expect(w.find('#dn-tai-khoan').element.value).toBe('')
    expect(w.find('#dn-mat-khau').element.value).toBe('')
  })
})

/* ====================== Bán tại quầy ====================== */

const SP = { id: 1, ma: 'SP01-DEN-4U-G5', tenSanPham: 'Yonex Astrox 99 Pro', thuongHieu: 'Yonex', phanLoai: 'Đen · 4U · G5', anh: null, giaGoc: 4290000, phanTramGiam: 10, donGia: 3861000, tonKho: 5 }
const CA = { id: 7, maGiaoCa: 'GC07', thoiGianNhanCa: '2026-10-16T07:00:00', tienDauCa: 2000000 }

/**
 * Máy chủ giả cho hóa đơn chờ lưu ở CSDL: tạo hóa đơn, thêm sản phẩm (trừ "kho" của SP), đổi số lượng, xóa dòng, hủy.
 * Trả về đúng dạng DTO của HoaDonChoService.
 */
function mayChuGia({ dangMo = CA, phieus = () => [], tonKho = 5 } = {}) {
  const may = { ds: [], ton: tonKho, idCt: 1, thanhToan: null, loiThanhToan: null }
  const dto = (h) => {
    const items = h.dong.map((d) => ({
      ...SP, id: d.id, idBienThe: SP.id, ten: SP.tenSanPham, donGia: d.donGia, giaHienTai: d.giaHienTai ?? d.donGia,
      giaDoi: (d.giaHienTai ?? d.donGia) !== d.donGia, soLuong: d.soLuong, tonKho: may.ton, thanhTien: d.donGia * d.soLuong,
    }))
    return {
      id: h.id, maHoaDon: h.ma, khach: h.khach, items, tongTienHang: items.reduce((s, d) => s + d.thanhTien, 0),
      soLuong: items.reduce((s, d) => s + d.soLuong, 0), coGiaDoi: items.some((d) => d.giaDoi),
    }
  }
  const tim = (url) => may.ds.find((h) => url.startsWith(`/ban-hang/hoa-don-cho/${h.id}`))
  useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
  api.get.mockImplementation(async (url) => {
    if (url === '/giao-ca/hien-tai') return { data: { dangMo, tongKet: dangMo ? { soHoaDon: 0, doanhThu: 0 } : null, lichHomNay: [] } }
    if (url === '/ban-hang/san-pham/loc') {
      return { data: { items: may.ton > 0 ? [{ ...SP, tonKho: may.ton }] : [], tongSo: may.ton > 0 ? 1 : 0, trang: 0, boLoc: {} } }
    }
    if (url === '/ban-hang/hoa-don-cho') return { data: may.ds.map(dto) }
    if (url === '/ban-hang/phieu-giam-gia') return { data: phieus() }
    return { data: [] }
  })
  api.post.mockImplementation(async (url, body) => {
    if (url === '/ban-hang/hoa-don-cho') {
      const h = { id: 100 + may.ds.length, ma: `HD${100 + may.ds.length}`, khach: null, dong: [] }
      may.ds.push(h)
      return { data: dto(h) }
    }
    const h = tim(url)
    if (url.endsWith('/san-pham')) {
      if (body.soLuong > may.ton) throw loi(`Sản phẩm ${SP.ma} chỉ còn ${may.ton} trong kho.`, 'HET_HANG')
      may.ton -= body.soLuong
      const d = h.dong.find((x) => x.donGia === SP.donGia)
      if (d) d.soLuong += body.soLuong
      else h.dong.push({ id: may.idCt++, donGia: SP.donGia, soLuong: body.soLuong })
      return { data: dto(h) }
    }
    if (url.endsWith('/cap-nhat-gia')) {
      h.dong.forEach((d) => (d.donGia = d.giaHienTai ?? d.donGia))
      return { data: dto(h) }
    }
    if (url.endsWith('/thanh-toan')) {
      may.thanhToan = body
      if (may.loiThanhToan) throw may.loiThanhToan
      may.ds = may.ds.filter((x) => x !== h)
      return { data: { id: 9, maHoaDon: h.ma, thanhTien: 3861000, tienThua: (body.tienMat || 0) - 3861000, maGiaoCa: 'GC07', giaoHang: !!body.giaoHang } }
    }
    return { data: {} }
  })
  api.put.mockImplementation(async (url, body) => {
    const h = tim(url)
    if (url.endsWith('/khach-hang')) {
      h.khach = body.idKhachHang ? { id: body.idKhachHang, hoTen: 'Nguyễn Thị Lan', sdt: '0901000001' } : null
    } else {
      const d = h.dong.find((x) => url.endsWith(`/chi-tiet/${x.id}`))
      may.ton -= body.soLuong - d.soLuong
      d.soLuong = body.soLuong
    }
    return { data: dto(h) }
  })
  api.delete.mockImplementation(async (url) => {
    const h = tim(url)
    const d = h?.dong.find((x) => url.endsWith(`/chi-tiet/${x.id}`))
    if (d) {
      may.ton += d.soLuong
      h.dong = h.dong.filter((x) => x !== d)
      return { data: dto(h) }
    }
    h.dong.forEach((x) => (may.ton += x.soLuong))
    may.ds = may.ds.filter((x) => x !== h)
    return { data: null }
  })
  return may
}

async function moBanHang(tuyChon) {
  const may = mayChuGia(tuyChon)
  const w = ganVao(BanHangPage)
  await flushPromises()
  // Giống các nhóm demo: bấm "Chọn sản phẩm" để mở hộp thoại bảng sản phẩm
  await w.find('[data-testid="bh-chon-sp"]').trigger('click')
  await flushPromises()
  return { w, may }
}
// Bảng chọn sản phẩm nằm trong hộp thoại (Teleport ra body)
const nutThem = () => body().find('.bh-nut-them')
const nutXacNhan = () => body().findAll('.modal button').find((b) => b.text() === 'Thanh toán')

describe('Bán tại quầy (hóa đơn chờ lưu ở CSDL)', () => {
  it('thêm sản phẩm: tự tạo hóa đơn chờ trên máy chủ, trừ kho ngay; thanh toán gửi đúng hóa đơn', async () => {
    const { w, may } = await moBanHang()
    expect(body().findAll('.bh-dong-chon')).toHaveLength(1)
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/ban-hang/hoa-don-cho')
    expect(api.post).toHaveBeenCalledWith('/ban-hang/hoa-don-cho/100/san-pham', { idBienThe: 1, soLuong: 1 })
    expect(may.ton).toBe(4)
    expect(w.text()).toContain('HD100')
    expect(w.find('.bh-nut-tt').text()).toContain('3.861.000')
    await w.find('#bh-tien-mat').setValue('4000000')
    await flushPromises()
    expect(w.text()).toContain('Tiền thừa trả khách')
    expect(w.text()).toContain('139.000')

    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toEqual(expect.objectContaining({ maPhieu: null, tienMat: 4000000, chuyenKhoan: 0, giaoHang: null }))
    expect(body().text()).toContain('Đã thanh toán HD100')
    expect(body().text()).toContain('Trả lại khách')
  })

  it('nhập số lượng lớn hơn tồn kho trong bảng chọn: báo ngay, không gửi', async () => {
    const { w } = await moBanHang()
    await body().find('.bh-o-sl').setValue('9')
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(body().text()).toContain('Chỉ còn 5 sản phẩm trong kho.')
    expect(api.post).not.toHaveBeenCalled()
  })

  it('bỏ sản phẩm / hủy hóa đơn chờ: trả hàng về kho; sản phẩm hết hàng biến mất khỏi bảng', async () => {
    const { w, may } = await moBanHang({ tonKho: 1 })
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(may.ton).toBe(0)
    expect(body().findAll('.bh-dong-chon')).toHaveLength(0)
    await w.find('.bh-gio .ad-icon-btn.is-danger').trigger('click')
    await flushPromises()
    expect(may.ton).toBe(1)
    expect(body().findAll('.bh-dong-chon')).toHaveLength(1)
    await w.find('.bh-tab-xoa').trigger('click')
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/ban-hang/hoa-don-cho/100')
    expect(may.ds).toHaveLength(0)
  })

  it('chưa vào ca: không tạo hóa đơn, nhắc vào ca', async () => {
    const { w } = await moBanHang({ dangMo: null })
    expect(w.text()).toContain('Bạn chưa vào ca.')
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(api.post).not.toHaveBeenCalled()
  })

  it('giá đổi sau khi thêm: máy chủ báo khi thanh toán, hiện giá mới và chỉ thanh toán được sau "Cập nhật giá"', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    may.ds[0].dong[0].giaHienTai = 4290000
    may.loiThanhToan = loi('Giá của 1 sản phẩm trong hóa đơn đã thay đổi.', 'GIA_THAY_DOI')
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Giá mới 4.290.000')
    expect(w.find('[data-testid="bh-cap-nhat-gia"]').exists()).toBe(true)
    may.loiThanhToan = null
    await w.find('[data-testid="bh-cap-nhat-gia"]').trigger('click')
    await flushPromises()
    expect(w.find('.bh-nut-tt').text()).toContain('4.290.000')
    expect(w.find('[data-testid="bh-cap-nhat-gia"]').exists()).toBe(false)
  })

  it('giao hàng: khách lẻ nhập một địa chỉ, cộng phí ship khi đơn dưới 1 triệu, gửi kèm người nhận', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await w.find('[data-testid="bh-giao-hang"]').setValue(true)
    await flushPromises()
    expect(w.text()).toContain('Phí giao hàng')
    expect(w.text()).toContain('Miễn phí')
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Vui lòng nhập tên người nhận.')
    await w.find('#bh-nn-ten').setValue('Trần Văn Hùng')
    await w.find('#bh-nn-sdt').setValue('0901000002')
    await w.find('#bh-nn-dc').setValue('Số 1 Trần Phú, Hà Đông, Hà Nội')
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan.giaoHang).toEqual({ hoTen: 'Trần Văn Hùng', sdt: '0901000002', diaChi: 'Số 1 Trần Phú, Hà Đông, Hà Nội' })
    expect(body().text()).toContain('Chờ giao hàng')
  })

  it('ô phiếu dạng thẻ: tự áp phiếu tốt nhất, phiếu chưa đủ điều kiện bị khóa kèm lý do, bỏ phiếu được', async () => {
    const PHIEUS = [
      { id: 1, ma: 'GIAM50K', ten: 'Giảm 50k', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 },
      { id: 2, ma: 'VIP10', ten: 'Khách VIP', loaiGiam: 'PHAN_TRAM', giaTri: 10, donToiThieu: 5000000, apDungDuoc: false, lyDo: 'Đơn tối thiểu 5.000.000 ₫ (còn thiếu 1.139.000 ₫).', soTienGiam: 0 },
    ]
    const { w } = await moBanHang({ phieus: () => PHIEUS })
    await nutThem().trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    const the = w.find('#bh-phieu')
    expect(the.text()).toContain('GIAM50K')
    expect(the.text()).toContain('Tự áp phiếu tốt nhất')
    expect(the.text()).toContain('-50.000')
    await the.trigger('click')
    const muc = w.findAll('.bh-phieu-muc')
    expect(muc).toHaveLength(3)
    expect(muc[1].text()).toContain('Tốt nhất')
    expect(muc[2].attributes('disabled')).toBeDefined()
    expect(muc[2].text()).toContain('còn thiếu 1.139.000')
    await muc[0].trigger('click')
    expect(w.find('.bh-phieu-ds').exists()).toBe(false)
    expect(w.find('#bh-phieu').text()).toContain('Không dùng phiếu')
    expect(w.find('.bh-nut-tt').text()).toContain('3.861.000')
  })

  it('phiếu bị sửa mức giảm lúc đang chờ: gửi mức giảm đang hiện, máy chủ báo thì tải lại phiếu và hiện số tiền mới', async () => {
    let giam = 50000
    const { w, may } = await moBanHang({ phieus: () => [{ ma: 'GIAM50K', loaiGiam: 'SO_TIEN', giaTri: giam, apDungDuoc: true, soTienGiam: giam }] })
    await nutThem(w).trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.find('.bh-nut-tt').text()).toContain('3.811.000')

    giam = 30000
    may.loiThanhToan = loi('Phiếu GIAM50K vừa được cập nhật: mức giảm đổi từ 50.000 ₫ thành 30.000 ₫.', 'PHIEU_THAY_DOI')
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toEqual(expect.objectContaining({ maPhieu: 'GIAM50K', soTienGiam: 50000 }))
    expect(w.find('.bh-nut-tt').text()).toContain('3.831.000')
  })
})

/* ====================== Vào ca (bắt buộc) ====================== */

describe('Trang vào ca', () => {
  const CA_TRUOC = {
    dangMo: null,
    goiYTienDauCa: 1000000,
    nguoiBanGiao: 'Phạm Minh Anh',
    thoiGianBanGiao: '2026-10-24T12:00:00',
    maCaTruoc: 'GC07',
    tienMatCaTruoc: 2500000,
    chuyenKhoanCaTruoc: 1500000,
    lichHomNay: [{ id: 21, tenCa: 'Ca chiều', gioBatDau: '12:00', gioKetThuc: '17:00' }],
  }
  function moTrang(hienTai = CA_TRUOC) {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockImplementation(async (url) => (url === '/giao-ca/hien-tai' ? { data: hienTai } : { data: [] }))
    return ganVao(VaoCaPage)
  }

  it('hiện tiền mặt + chuyển khoản ca trước, bắt buộc nhập cả hai; lệch thì phải ghi lý do', async () => {
    const w = moTrang()
    await flushPromises()
    expect(w.find('[data-testid="vc-tien-mat-truoc"]').text()).toContain('1.000.000')
    expect(w.find('[data-testid="vc-ck-truoc"]').text()).toContain('1.500.000')
    await w.find('#vc-form').trigger('submit')
    expect(w.text()).toContain('Vui lòng nhập tiền mặt đầu ca.')
    expect(w.text()).toContain('Vui lòng nhập số tiền chuyển khoản của ca trước.')
    expect(api.post).not.toHaveBeenCalled()
    await w.find('#vc-tien-mat').setValue('1000000')
    await w.find('#vc-chuyen-khoan').setValue('1200000')
    await w.find('#vc-form').trigger('submit')
    expect(w.text()).toContain('Chuyển khoản ca trước khác số hệ thống ghi nhận')
    expect(api.post).not.toHaveBeenCalled()
  })

  it('"Dùng số tiền này" điền đúng số ca trước, bắt đầu ca xong vào trang định mở', async () => {
    state.query = { quayLai: '/hoa-don' }
    const w = moTrang()
    await flushPromises()
    await w.find('[data-testid="vc-dung-so"]').trigger('click')
    api.post.mockResolvedValue({ data: { maGiaoCa: 'GC08', ngoaiLich: false, tenCa: 'Ca chiều' } })
    await w.find('#vc-form').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/giao-ca/nhan-ca', { tienDauCa: 1000000, chuyenKhoanCaTruoc: 1500000, ghiChu: '', idLichLamViec: null })
    expect(replace).toHaveBeenCalledWith('/hoa-don')
  })

  it('đã trong ca thì không ở lại trang vào ca', async () => {
    moTrang({ ...CA_TRUOC, dangMo: { id: 5, maGiaoCa: 'GC05' } })
    await flushPromises()
    expect(replace).toHaveBeenCalledWith('/ban-hang')
  })
})
