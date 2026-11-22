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
vi.mock('../src/services/diaChiHanhChinhService', () => ({
  API_DIA_CHI: 'x',
  diaChiHanhChinhService: {
    layTinhThanh: async () => [{ code: 1, name: 'Thành phố Hà Nội' }, { code: 25, name: 'Tỉnh Phú Thọ' }],
    layPhuongXa: async () => [{ code: 9, name: 'Phường Hà Đông' }],
    xoaNho() {},
  },
}))

import BanHangPage from '../src/components/banHang/BanHangPage.vue'
import { nhanTin } from '../src/services/suKien'
import DiaChiHanhChinhSelect from '../src/components/common/DiaChiHanhChinhSelect.vue'
import { caiDatVanChuyen, nhanCaiDatVanChuyen } from '../src/services/caiDatVanChuyen'
import { phiVanChuyenTheoTinh } from '../src/utils/phiVanChuyenTinh'
import DangNhapPage from '../src/components/auth/DangNhapPage.vue'
import KhachDangNhapView from '../src/views/client/KhachDangNhapView.vue'
import VaoCaPage from '../src/components/giaoCa/VaoCaPage.vue'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'
import { _datLaiGiaoCa } from '../src/composables/useGiaoCa'
import { useToast } from '../src/composables/useToast'
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
  Object.assign(caiDatVanChuyen, { mienPhiBat: false, mienPhiTu: 0, phiGiaoHang: 0, daTai: false })
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

  it('trang quản trị chỉ cho nhân viên / quản lý: không có nút sang cửa hàng online / đăng nhập khách', async () => {
    const w = ganVao(DangNhapPage)
    expect(w.find('[role=tablist]').exists()).toBe(false)
    expect(w.text()).not.toContain('Thu ngân')
    expect(w.text()).not.toContain('Cửa hàng online')
    expect(w.text()).not.toContain('Đăng nhập cửa hàng online')
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
  const may = { ds: [], ton: tonKho, idCt: 1, thanhToan: null, loiThanhToan: null, ck: null, gia: SP.donGia }
  const dto = (h) => {
    const items = h.dong.map((d) => ({
      ...SP, id: d.id, idBienThe: SP.id, ten: SP.tenSanPham, donGia: d.donGia, giaHienTai: may.gia,
      giaDoi: may.gia !== d.donGia, soLuong: d.soLuong, tonKho: may.ton, thanhTien: d.donGia * d.soLuong,
    }))
    return {
      id: h.id, maHoaDon: h.ma, khach: h.khach, items, tongTienHang: items.reduce((s, d) => s + d.thanhTien, 0),
      soLuong: items.reduce((s, d) => s + d.soLuong, 0), coGiaDoi: items.some((d) => d.giaDoi),
    }
  }
  const tim = (url) => may.ds.find((h) => url.startsWith(`/ban-hang/hoa-don-cho/${h.id}`))
  useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
  api.get.mockImplementation(async (url, cfg) => {
    // Phí giao hàng máy chủ tính (API bản đồ); máy chủ giả trả theo bảng tỉnh như khi không có bản đồ
    if (url === '/cua-hang/phi-van-chuyen') return { data: { phiVanChuyen: phiVanChuyenTheoTinh(cfg?.params?.tinhThanh, cfg?.params?.phuongXa), soKm: null, nguon: 'BANG_TINH' } }
    if (url === '/giao-ca/hien-tai') return { data: { dangMo, tongKet: dangMo ? { soHoaDon: 0, doanhThu: 0 } : null, lichHomNay: [] } }
    if (url === '/ban-hang/san-pham/loc') {
      return { data: { items: may.ton > 0 ? [{ ...SP, tonKho: may.ton }] : [], tongSo: may.ton > 0 ? 1 : 0, trang: 0, boLoc: {} } }
    }
    if (url === '/ban-hang/hoa-don-cho') return { data: may.ds.map(dto) }
    if (url === '/ban-hang/phieu-giam-gia') return { data: phieus() }
    if (url.endsWith('/chuyen-khoan')) return { data: may.ck }
    if (url === '/cua-hang/thong-tin-thanh-toan') return { data: { nganHang: 'MB', soTaiKhoan: '0123456789', tenChuTaiKhoan: 'FOOTSTYLE DEMO' } }
    // Trang chi tiết hóa đơn vừa thanh toán (chuyển tới ngay sau khi trả tiền) và cửa sổ tự in đều đọc lại qua GET này
    if (url === '/hoa-don/9') {
      return {
        data: {
          hoaDon: { id: 9, maHoaDon: 'HD100' },
          khachHang: { maKhachHang: null, khachLe: true, ten: 'Khách vãng lai', sdt: null, email: null },
          giaoHang: { hoTenNguoiNhan: null, sdtNguoiNhan: null, diaChi: null },
          sanPham: [],
          daThanhToan: 3861000,
        },
      }
    }
    return { data: [] }
  })
  api.post.mockImplementation(async (url, body) => {
    if (url === '/ban-hang/hoa-don-cho') {
      const h = { id: 100 + may.ds.length, ma: `HD${100 + may.ds.length}`, khach: null, dong: [] }
      may.ds.push(h)
      return { data: dto(h) }
    }
    const h = tim(url)
    // QR chuyển khoản giả lập: null = backend tắt giả lập (dùng VietQR)
    if (url.endsWith('/chuyen-khoan')) return { data: may.ck }
    if (url.endsWith('/san-pham')) {
      if (body.soLuong > may.ton) throw loi(`Sản phẩm ${SP.ma} chỉ còn ${may.ton} trong kho.`, 'HET_HANG')
      const d = h.dong.find((x) => x.donGia === may.gia)
      const khac = h.dong.find((x) => x.donGia !== may.gia)
      if (!d && khac && !body.xacNhanGiaMoi) {
        throw loi('Sản phẩm đã đổi giá.', 'GIA_THAY_DOI', {
          chiTiet: { idBienThe: SP.id, ma: SP.ma, ten: SP.tenSanPham, donGiaCu: khac.donGia, donGiaMoi: may.gia, soLuong: body.soLuong },
        })
      }
      may.ton -= body.soLuong
      if (d) d.soLuong += body.soLuong
      else h.dong.push({ id: may.idCt++, donGia: may.gia, soLuong: body.soLuong })
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
      if (body.soLuong > d.soLuong && d.donGia !== may.gia) {
        throw loi('Sản phẩm đã đổi giá.', 'GIA_THAY_DOI', {
          chiTiet: { idBienThe: SP.id, ma: SP.ma, ten: SP.tenSanPham, donGiaCu: d.donGia, donGiaMoi: may.gia, soLuong: body.soLuong - d.soLuong },
        })
      }
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

async function moBanHang(tuyChon = {}) {
  const may = mayChuGia(tuyChon)
  const w = ganVao(BanHangPage)
  await flushPromises()
  // Nhân viên tự bấm "Thêm hóa đơn chờ" (không còn tự tạo khi chọn sản phẩm)
  if (tuyChon.taoHoaDon !== false) {
    await w.find('[data-testid="bh-them-hd"]').trigger('click')
    await flushPromises()
  }
  // Giống các nhóm demo: bấm "Chọn sản phẩm" để mở hộp thoại bảng sản phẩm
  await w.find('[data-testid="bh-chon-sp"]').trigger('click')
  await flushPromises()
  return { w, may }
}
// Bảng chọn sản phẩm nằm trong hộp thoại (Teleport ra body)
const nutThem = () => body().find('.bh-nut-them')
const nutXacNhan = () => body().findAll('.modal button').find((b) => b.text() === 'Thanh toán')

function cuaSoInGia() {
  const cuaSo = { document: { write: vi.fn(), close: vi.fn() }, close: vi.fn() }
  const open = vi.spyOn(window, 'open').mockReturnValue(cuaSo)
  return { cuaSo, open }
}

describe('Bán tại quầy (hóa đơn chờ lưu ở CSDL)', () => {
  afterEach(() => vi.restoreAllMocks())

  it('chưa có hóa đơn chờ: bấm "Thêm" sản phẩm thì tự tạo hóa đơn rồi thêm vào', async () => {
    const { w, may } = await moBanHang({ taoHoaDon: false })
    expect(w.text()).not.toContain('Chọn sản phẩm là tự tạo hóa đơn mới.') // review: bỏ bớt chữ hướng dẫn nhỏ
    expect(w.text()).not.toContain('F2: chọn sản phẩm')
    expect(w.text()).not.toContain('Điền sẵn đúng số tiền cần trả')
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/ban-hang/hoa-don-cho')
    expect(api.post).toHaveBeenCalledWith('/ban-hang/hoa-don-cho/100/san-pham', { idBienThe: 1, soLuong: 1 })
    expect(may.ds).toHaveLength(1)
    expect(useToast().toasts.some((t) => t.message.includes('Chưa có hóa đơn chờ'))).toBe(false)
  })

  it('thêm hóa đơn chờ rồi thêm sản phẩm: trừ kho ngay; thanh toán gửi đúng hóa đơn', async () => {
    const { w, may } = await moBanHang()
    expect(body().findAll('.bh-dong-chon')).toHaveLength(1)
    expect(body().find('.bh-dong-chon [data-testid="bh-giam-anh"]').text()).toBe('-10%') // % giảm nằm trên ảnh
    expect(body().findAll('.bh-dong-chon .bh-chip').map((c) => c.text())).toEqual(['Đen', '4U', 'G5'])
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

    const { cuaSo } = cuaSoInGia()
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toEqual(expect.objectContaining({ maPhieu: null, tienMat: 4000000, chuyenKhoan: 0, giaoHang: null }))
    // Thanh toán xong: không hỏi lại, tự in hóa đơn và chuyển thẳng sang trang chi tiết
    expect(body().text()).not.toContain('Đã thanh toán HD100')
    expect(push).toHaveBeenCalledWith('/hoa-don/9')
    await new Promise((r) => setTimeout(r, 50)) // tạo mã QR để in thật sự chạy bất đồng bộ qua nhiều tick, 1 lần flushPromises chưa đủ
    expect(cuaSo.document.write).toHaveBeenCalledTimes(1)
    expect(cuaSo.document.write.mock.calls[0][0]).toContain('HD100')
    expect(cuaSo.document.close).toHaveBeenCalledTimes(1)
  })

  it('trình duyệt chặn cửa sổ bật lên: vẫn thanh toán và chuyển trang được, chỉ báo lỗi không in được', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    vi.spyOn(window, 'open').mockReturnValue(null)
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toBeTruthy()
    expect(push).toHaveBeenCalledWith('/hoa-don/9')
    vi.restoreAllMocks()
  })

  it('nhập số lượng lớn hơn tồn kho trong bảng chọn: tự đưa về tối đa (góp ý 21/11), bấm Thêm lần nữa thì thêm đúng số tối đa', async () => {
    const { w } = await moBanHang()
    const o = () => body().find('.bh-o-sl')
    await o().setValue('9') // gõ xong rời ô (sự kiện change) là đưa về tối đa ngay
    expect(o().element.value).toBe('5')
    expect(body().text()).toContain('Chỉ còn 5 sản phẩm trong kho, đã đưa về 5.')
    expect(api.post.mock.calls.some((c) => c[0].endsWith('/san-pham'))).toBe(false)

    await o().setValue('0')
    expect(o().element.value).toBe('1')
    expect(body().text()).toContain('Số lượng tối thiểu là 1, đã đưa về 1.')

    await o().setValue('9')
    expect(o().element.value).toBe('5')
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(expect.stringMatching(/\/san-pham$/), expect.objectContaining({ soLuong: 5 }))
  })

  it('gõ số lượng ở dòng giỏ quá tồn kho: ô tự đưa về tối đa và gửi đúng số tối đa (góp ý 21/11)', async () => {
    const { w } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    const o = w.find('.bh-gio input[type="number"]')
    await o.setValue('101')
    await flushPromises()
    expect(o.element.value).toBe('5')
    expect(api.put).toHaveBeenCalledWith(expect.stringContaining('/chi-tiet/'), expect.objectContaining({ soLuong: 5 }))
    expect(api.put.mock.calls.some((c) => c[1]?.soLuong === 101)).toBe(false)
  })

  it('bỏ sản phẩm / hủy hóa đơn chờ: trả hàng về kho; sản phẩm hết hàng biến mất khỏi bảng', async () => {
    const { w, may } = await moBanHang({ tonKho: 1 })
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(may.ton).toBe(0)
    expect(body().findAll('.bh-dong-chon')).toHaveLength(0)
    // Bấm thùng rác chỉ hỏi lại: chưa xóa gì; Hủy thì giữ nguyên, xác nhận "Bỏ" mới trả hàng về kho
    const nutTrongHop = (ten) => body().findAll('.modal button').find((b) => b.text() === ten)
    await w.find('.bh-gio .ad-icon-btn.is-danger').trigger('click')
    await flushPromises()
    expect(body().text()).toContain('Bỏ Yonex Astrox 99 Pro (Đen · 4U · G5), số lượng 1, khỏi HD100? Sản phẩm sẽ được trả lại kho.')
    expect(api.delete).not.toHaveBeenCalled()
    await nutTrongHop('Hủy').trigger('click')
    await flushPromises()
    expect(api.delete).not.toHaveBeenCalled()
    expect(may.ton).toBe(0)
    await w.find('.bh-gio .ad-icon-btn.is-danger').trigger('click')
    await flushPromises()
    await nutTrongHop('Bỏ').trigger('click')
    await flushPromises()
    expect(may.ton).toBe(1)
    expect(body().findAll('.bh-dong-chon')).toHaveLength(1)
    expect(w.text()).not.toContain('Hủy hóa đơn') // chỉ hủy bằng dấu X trên tab
    await w.find('.bh-tab-xoa').trigger('click')
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/ban-hang/hoa-don-cho/100')
    expect(may.ds).toHaveLength(0)
  })

  it('chưa vào ca: khóa nút Thêm hóa đơn chờ, Chọn sản phẩm, quét camera, nút Thêm trong bảng chọn (góp ý 21/11); không tạo hóa đơn', async () => {
    const { w } = await moBanHang({ dangMo: null })
    expect(w.text()).toContain('Bạn chưa vào ca nên mới chỉ xem được.')
    expect(w.find('[data-testid="bh-them-hd"]').attributes('disabled')).toBeDefined()
    expect(w.find('[data-testid="bh-chon-sp"]').attributes('disabled')).toBeDefined()
    expect(w.find('button[title="Bạn chưa vào ca"] .bi-camera').exists()).toBe(true)
    // nút Chọn sản phẩm bị khóa nên bảng chọn không mở; phím tắt F2 cũng không mở
    expect(nutThem(w).exists()).toBe(false)
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F2' }))
    await flushPromises()
    expect(nutThem(w).exists()).toBe(false)
    expect(api.post).not.toHaveBeenCalled()
  })

  // Góp ý 16/11 [00:00–00:44]: dòng đã thêm giữ giá cũ; thêm lại khi giá đổi thì popup, OK mới tách dòng giá mới
  it('giá / đợt giảm giá đổi: dòng cũ giữ giá cũ, không tự đổi; thêm lại thì popup, Hủy không thêm, OK tách dòng giá mới; đổi phiếu không tải lại hóa đơn', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await body().find('[data-testid="bh-dong-chon-sp"]').trigger('click')
    await flushPromises()

    // Tin đổi phiếu: màn quầy không tải lại hóa đơn chờ
    const soLanTai = () => api.get.mock.calls.filter((c) => c[0] === '/ban-hang/hoa-don-cho').length
    const truoc = soLanTai()
    nhanTin({ chuDe: ['phieu-giam-gia'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(soLanTai()).toBe(truoc)

    may.gia = 4290000 // đợt giảm giá kết thúc
    nhanTin({ chuDe: ['giam-gia', 'san-pham'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.text()).not.toContain('Giá mới')
    expect(w.find('.bh-nut-tt').text()).toContain('3.861.000') // dòng cũ giữ giá cũ
    expect(w.text()).not.toContain('Cập nhật giá')
    expect(w.find('[data-testid="bh-dong-giam"]').exists()).toBe(true) // dòng cũ giữ hiển thị giảm giá cũ
    expect(w.find('.bh-gio').text()).toContain('Kho còn') // một dòng thì vẫn hiện tồn kho

    // Thêm lại: popup, Hủy thì không thêm
    await w.find('[data-testid="bh-chon-sp"]').trigger('click')
    await flushPromises()
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(body().find('[data-testid="bh-hoi-gia"]').text()).toMatch(/3\.861\.000.*4\.290\.000/)
    await body().find('[data-testid="bh-hoi-gia-huy"]').trigger('click')
    await flushPromises()
    expect(body().find('[data-testid="bh-hoi-gia"]').exists()).toBe(false)
    expect(may.ds[0].dong).toHaveLength(1)

    // Thêm lại, OK: dòng mới giá mới, dòng cũ giữ giá cũ
    await nutThem(w).trigger('click')
    await flushPromises()
    await body().find('[data-testid="bh-hoi-gia-ok"]').trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(expect.stringMatching(/\/san-pham$/), { idBienThe: SP.id, soLuong: 1, xacNhanGiaMoi: true })
    expect(may.ds[0].dong.map((d) => d.donGia)).toEqual([3861000, 4290000])
    expect(w.find('.bh-nut-tt').text()).toContain('8.151.000')
    // Hai dòng cùng sản phẩm khác đợt: bỏ chữ "Kho còn N" (dòng nào cũng lặp cùng một số)
    expect(w.find('.bh-gio').text()).not.toContain('Kho còn')
  })

  it('giá đổi khi đang lên đơn: dòng cũ ẩn nút +, chữ đỏ giá cũ → giá mới (góp ý 21/11); gõ tăng số lượng thì popup, OK thì phần thêm thành dòng mới giá mới', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await body().find('[data-testid="bh-dong-chon-sp"]').trigger('click')
    await flushPromises()
    const nutCong = () => w.find('button[aria-label="Tăng số lượng Yonex Astrox 99 Pro"]')
    expect(nutCong().exists()).toBe(true)
    expect(w.find('[data-testid="bh-gia-doi"]').exists()).toBe(false)
    may.gia = 4290000
    nhanTin({ chuDe: ['san-pham'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(nutCong().exists()).toBe(false)
    const chuDo = w.find('[data-testid="bh-gia-doi"]')
    expect(chuDo.classes()).toContain('text-danger')
    // Chỉ ghi số tiền, không có chữ "Giá đã cập nhật"; giỏ chưa có dòng giá mới nên tạm hiện ở dòng giá cũ
    expect(chuDo.text()).toBe('3.861.000 ₫ → 4.290.000 ₫')
    await w.find('.bh-gio input[type="number"]').setValue('2')
    await flushPromises()
    expect(body().find('[data-testid="bh-hoi-gia"]').exists()).toBe(true)
    await body().find('[data-testid="bh-hoi-gia-ok"]').trigger('click')
    await flushPromises()
    expect(may.ds[0].dong.map((d) => [d.donGia, d.soLuong])).toEqual([[3861000, 1], [4290000, 1]])
    // Đã có dòng giá mới: chữ đỏ chuyển xuống dòng 2 (dòng giá mới), dòng 1 không còn
    const dong = w.findAll('.bh-gio tbody tr')
    expect(dong[0].find('[data-testid="bh-gia-doi"]').exists()).toBe(false)
    expect(dong[1].find('[data-testid="bh-gia-doi"]').text()).toBe('3.861.000 ₫ → 4.290.000 ₫')
    expect(w.text()).not.toContain('Giá đã cập nhật')
    // Giá đổi lần nữa: dòng mới (dòng 3) ghi đủ cả hai mức giá cũ, hai dòng cũ không có chữ đỏ
    may.gia = 4000000
    nhanTin({ chuDe: ['san-pham'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    await w.findAll('.bh-gio input[type="number"]')[1].setValue('2')
    await flushPromises()
    await body().find('[data-testid="bh-hoi-gia-ok"]').trigger('click')
    await flushPromises()
    expect(may.ds[0].dong.map((d) => [d.donGia, d.soLuong])).toEqual([[3861000, 1], [4290000, 1], [4000000, 1]])
    const dong3 = w.findAll('.bh-gio tbody tr')
    expect(dong3.map((r) => r.find('[data-testid="bh-gia-doi"]').exists())).toEqual([false, false, true])
    expect(dong3[2].find('[data-testid="bh-gia-doi"]').text()).toBe('3.861.000 ₫, 4.290.000 ₫ → 4.000.000 ₫')
  })

  it('dòng giữ giá cũ vẫn thanh toán được ngay, không bị chặn vì giá đổi', async () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    may.gia = 4290000
    nhanTin({ chuDe: ['san-pham'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.find('.bh-nut-tt').attributes('disabled')).toBeUndefined()
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    // Góp ý 19/11: bấm Thanh toán không hỏi "Đợt giảm giá đã thay đổi", vào thẳng hộp xác nhận
    expect(body().text()).not.toContain('Đợt giảm giá đã thay đổi')
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.ds).toHaveLength(0)
    expect(api.post).not.toHaveBeenCalledWith(expect.stringContaining('/cap-nhat-gia'))
  })

  it('dòng trong giỏ có nhãn giảm giá -% như bảng chọn', async () => {
    const { w } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await body().find('[data-testid="bh-dong-chon-sp"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-testid="bh-dong-giam"]').text()).toBe('-10%')
  })

  it('tải ngầm khi con trỏ ở ô ghi chú: chữ đang gõ giữ nguyên; đang gõ ô số lượng thì chờ gõ xong', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await body().find('[data-testid="bh-dong-chon-sp"]').trigger('click')
    await flushPromises()

    const oSoLuong = w.find('input[type="number"]')
    oSoLuong.element.focus()
    may.gia = 4290000
    nhanTin({ chuDe: ['san-pham'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 200))
    await flushPromises()
    expect(w.text()).not.toContain('Giá mới 4.290.000')

    const oGhiChu = w.find('#bh-ghi-chu')
    await oGhiChu.setValue('Giao buổi chiều')
    oGhiChu.element.focus()
    await new Promise((r) => setTimeout(r, 1100)) // đợi lượt kiểm tra lại của useAutoRefresh
    await flushPromises()
    expect(w.text()).not.toContain('Giá mới')
    expect(oGhiChu.element.value).toBe('Giao buổi chiều')
    expect(document.activeElement).toBe(oGhiChu.element)
  })

  it('đang tải ngầm mà nhân viên vừa thêm hàng: bỏ kết quả đã cũ, không mất dòng vừa thêm', async () => {
    const { w } = await moBanHang()
    await body().find('[data-testid="bh-dong-chon-sp"]').trigger('click')
    await flushPromises()
    const goc = api.get.getMockImplementation()
    let traKetQuaCu = null
    api.get.mockImplementation((url, ...k) => {
      if (url !== '/ban-hang/hoa-don-cho' || traKetQuaCu) return goc(url, ...k)
      const cu = goc(url, ...k) // chụp danh sách lúc chưa thêm hàng
      return new Promise((r) => {
        traKetQuaCu = () => cu.then(r)
      })
    })
    nhanTin({ chuDe: ['hoa-don'], nguon: 'may-khac' })
    await new Promise((r) => setTimeout(r, 200))
    await flushPromises()
    expect(traKetQuaCu).toBeTypeOf('function')

    await w.find('[data-testid="bh-chon-sp"]').trigger('click')
    await flushPromises()
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(w.find('input[type="number"]').exists()).toBe(true)

    traKetQuaCu()
    await flushPromises()
    await new Promise((r) => setTimeout(r))
    await flushPromises()
    expect(w.find('input[type="number"]').exists()).toBe(true)
  })

  it('giao hàng: thẻ riêng ở cột trái, chọn Tỉnh/Phường, phí tính theo tỉnh, gửi kèm người nhận', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    expect(w.find('[data-testid="bh-khoi-giao"]').exists()).toBe(false)
    await w.find('[data-testid="bh-giao-hang"]').setValue(true)
    await flushPromises()
    const khoi = w.find('[data-testid="bh-khoi-giao"]')
    expect(khoi.exists()).toBe(true)
    expect(w.find('.bh-trai [data-testid="bh-khoi-giao"]').exists()).toBe(true)
    expect(w.find('#bh-phi-ship').attributes('placeholder')).toBe('Chưa tính phí') // chưa chọn tỉnh thì chưa có phí
    expect(khoi.text()).not.toContain('Phí giao hàng') // phí chỉ hiện ở khối thanh toán bên phải
    expect(w.text()).not.toContain('Vui lòng nhập đủ thông tin người nhận') // không nhắc sẵn dưới nút Thanh toán
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Vui lòng nhập tên người nhận.')
    expect(w.text()).toContain('Vui lòng chọn Tỉnh/Thành phố.')
    await w.find('#bh-nn-ten').setValue('Trần Văn Hùng')
    await w.find('#bh-nn-sdt').setValue('0901000002')
    const chonDc = w.findComponent(DiaChiHanhChinhSelect)
    chonDc.vm.$emit('update:tinhThanh', 'Thành phố Hà Nội')
    chonDc.vm.$emit('update:phuongXa', 'Phường Hà Đông')
    await w.find('#bh-nn-dc').setValue('Số 1 Trần Phú')
    await flushPromises()
    expect(w.find('#bh-phi-ship').attributes('placeholder')).toBe('25.000 ₫') // Hà Nội, phường: 25.000
    expect(w.find('#bh-phi-ship').element.value).toBe('25.000') // phí tự tính hiện sẵn trong ô, chưa phải số nhân viên gõ tay
    const { cuaSo } = cuaSoInGia()
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan.giaoHang).toEqual({ hoTen: 'Trần Văn Hùng', sdt: '0901000002', diaChi: 'Số 1 Trần Phú', tinhThanh: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', phiVanChuyen: null })
    // Thanh toán xong đơn giao hàng cũng tự in và chuyển sang trang chi tiết luôn, không hỏi lại
    expect(push).toHaveBeenCalledWith('/hoa-don/9')
    // tạo mã QR để in chạy bất đồng bộ qua nhiều tick, máy chậm thì lâu hơn nên chờ tới khi in xong
    await vi.waitFor(() => expect(cuaSo.document.write).toHaveBeenCalledTimes(1))
  })

  it('đơn đạt ngưỡng miễn phí vận chuyển (Cài đặt free ship): ô phí thành chữ "Miễn phí", không cộng phí; hạ ngưỡng xuống dưới thì lại tính theo tỉnh', async () => {
    const { w } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await w.find('[data-testid="bh-giao-hang"]').setValue(true)
    await flushPromises()
    const chonDc = w.findComponent(DiaChiHanhChinhSelect)
    chonDc.vm.$emit('update:tinhThanh', 'Thành phố Hồ Chí Minh')
    chonDc.vm.$emit('update:phuongXa', 'Phường Bến Nghé')
    await flushPromises()
    const tong = () => w.find('.bh-nut-tt').text()
    try {
      // Đơn 3.861.000, ngưỡng 5.000.000: chưa đạt nên vẫn là ô nhập, phí theo tỉnh Hồ Chí Minh là 50.000
      nhanCaiDatVanChuyen({ mienPhiBat: true, mienPhiTu: 5_000_000 })
      await flushPromises()
      expect(w.find('[data-testid="bh-mien-phi-ship"]').exists()).toBe(false)
      expect(w.find('#bh-phi-ship').element.value).toBe('50.000')
      expect(tong()).toContain('3.911.000')
      // Quản lý hạ ngưỡng còn 3.000.000: đơn đạt ngưỡng, không còn ô nhập, không cộng phí
      nhanCaiDatVanChuyen({ mienPhiBat: true, mienPhiTu: 3_000_000 })
      await flushPromises()
      expect(w.find('#bh-phi-ship').exists()).toBe(false)
      expect(w.find('[data-testid="bh-mien-phi-ship"]').text()).toBe('Miễn phí')
      expect(tong()).toContain('3.861.000')
      // Tắt miễn phí theo giá trị đơn: về lại phí theo tỉnh
      nhanCaiDatVanChuyen({ mienPhiBat: false, mienPhiTu: 3_000_000 })
      await flushPromises()
      expect(w.find('#bh-phi-ship').element.value).toBe('50.000')
      expect(tong()).toContain('3.911.000')
    } finally {
      nhanCaiDatVanChuyen({ mienPhiBat: false, mienPhiTu: 0 })
    }
  })

  it('phí vận chuyển là một ô nhập: tự điền theo tỉnh, sửa được, đổi tỉnh thì tính lại; hộp xác nhận ghi đủ thông tin', async () => {
    const { w, may } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await w.find('[data-testid="bh-giao-hang"]').setValue(true)
    await flushPromises()
    const oPhi = () => w.find('#bh-phi-ship')
    const tong = () => w.find('.bh-nut-tt').text()
    expect(w.find('[data-testid="bh-phi-ship"] button').exists()).toBe(false)
    await w.find('#bh-nn-ten').setValue('Trần Văn Hùng')
    await w.find('#bh-nn-sdt').setValue('0901000002')
    await w.find('#bh-nn-dc').setValue('Số 1 Trần Phú')
    const chonDc = w.findComponent(DiaChiHanhChinhSelect)
    chonDc.vm.$emit('update:tinhThanh', 'Thành phố Hồ Chí Minh')
    chonDc.vm.$emit('update:phuongXa', 'Phường Bến Nghé')
    await flushPromises()
    expect(oPhi().element.value).toBe('50.000')
    expect(tong()).toContain('3.911.000')

    await oPhi().setValue('70000')
    await flushPromises()
    expect(oPhi().element.value).toBe('70.000')
    expect(tong()).toContain('3.931.000')
    // Xóa trống ô: về phí theo tỉnh, ô vẫn trống để gõ số mới
    await oPhi().setValue('')
    await flushPromises()
    expect(oPhi().element.value).toBe('')
    expect(oPhi().attributes('placeholder')).toBe('50.000 ₫')
    expect(tong()).toContain('3.911.000')
    await oPhi().setValue('70000')
    await flushPromises()
    // Đổi tỉnh: bỏ số đã nhập, tính lại theo tỉnh mới
    chonDc.vm.$emit('update:tinhThanh', 'Tỉnh Phú Thọ')
    chonDc.vm.$emit('update:phuongXa', 'Phường Việt Trì')
    await flushPromises()
    expect(oPhi().element.value).toBe('25.000')
    await oPhi().setValue('40000')
    await flushPromises()
    expect(oPhi().element.value).toBe('40.000')
    expect(tong()).toContain('3.901.000')

    await w.find('#bh-ghi-chu').setValue('Giao giờ hành chính')
    const { cuaSo } = cuaSoInGia()
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    const noiDung = body().find('.bh-xn-modal').text().replace(/\s+/g, ' ')
    for (const chu of ['HD100', 'Khách lẻ', 'Trần Văn Hùng · 0901000002', 'Số 1 Trần Phú, Phường Việt Trì, Tỉnh Phú Thọ', 'Giao giờ hành chính',
      'Yonex Astrox 99 Pro', 'SP01-DEN-4U-G5 · Đen · 4U · G5', '1 × 3.861.000', 'Phí vận chuyển', '40.000', 'Tiền mặt', '3.901.000']) expect(noiDung).toContain(chu)
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan.giaoHang.phiVanChuyen).toBe(40000)
    expect(may.thanhToan.tienMat).toBe(3901000)
    await vi.waitFor(() => expect(cuaSo.document.write).toHaveBeenCalledTimes(1))
  })

  it('giao hàng tại quầy: có nút Địa chỉ khách hàng mở sổ địa chỉ (chọn cũ hoặc thêm mới); chưa chọn khách thành viên thì báo nhắc; chọn xong tự điền thông tin', async () => {
    nhanCaiDatVanChuyen({ mienPhiBat: false, mienPhiTu: 0, phiGiaoHang: 25_000 })
    const { w } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await w.find('[data-testid="bh-giao-hang"]').setValue(true)
    await flushPromises()

    const nutCu = w.find('[data-testid="bh-btn-dc-cu"]')
    expect(nutCu.exists()).toBe(true)

    // Khách lẻ: bấm nút thì báo nhắc chọn khách hàng thành viên trước
    await nutCu.trigger('click')
    await flushPromises()
    expect(useToast().toasts.at(-1)).toMatchObject({ type: 'error', message: expect.stringContaining('chọn khách hàng thành viên') })
    expect(w.vm.modalDiaChiKhach.open).toBe(false)

    // Chọn khách thành viên
    w.vm.hd.khach = { id: 7, hoTen: 'Lê Minh Tuấn', sdt: '0901000003' }
    await flushPromises()

    // Bấm nút "Địa chỉ khách hàng" mở modal với cờ moThemMoi = false
    await nutCu.trigger('click')
    await flushPromises()
    expect(w.vm.modalDiaChiKhach.open).toBe(true)
    expect(w.vm.modalDiaChiKhach.moThemMoi).toBe(false)

    // Áp dụng địa chỉ từ modal
    w.vm.apDungDiaChiGiao({
      id: 1,
      tenNguoiNhan: 'Lê Quốc Tuấn',
      sdtNguoiNhan: '0379992712',
      tinhThanhPho: 'Tỉnh Phú Thọ',
      phuongXa: 'Phường Vĩnh Phúc',
      diaChiCuThe: 'Số 18 Đầm Vạc',
    })
    await flushPromises()

    expect(w.vm.hd.p.nguoiNhan.hoTen).toBe('Lê Quốc Tuấn')
    expect(w.vm.hd.p.nguoiNhan.sdt).toBe('0379992712')
    expect(w.vm.hd.p.nguoiNhan.tinhThanh).toBe('Tỉnh Phú Thọ')
    expect(w.vm.hd.p.nguoiNhan.phuongXa).toBe('Phường Vĩnh Phúc')
    expect(w.vm.hd.p.nguoiNhan.diaChiCuThe).toBe('Số 18 Đầm Vạc')
    // Phí ship tự tính theo tỉnh Phú Thọ (25.000)
    expect(w.find('#bh-phi-ship').element.value).toBe('25.000')
    expect(useToast().toasts.at(-1)).toMatchObject({ type: 'success', message: 'Đã áp dụng địa chỉ giao hàng.' })

    // Đang có địa chỉ giao hàng, sau đó đổi về Khách lẻ -> thông tin giao hàng tự động bị xóa trắng
    expect(w.vm.hd.p.nguoiNhan.hoTen).toBe('Lê Quốc Tuấn')

    // Bỏ khách về Khách lẻ
    await w.vm.chonKhach(null)
    await flushPromises()
    expect(w.vm.hd.p.nguoiNhan.hoTen).toBe('')
    expect(w.vm.hd.p.nguoiNhan.sdt).toBe('')
    expect(w.vm.hd.p.nguoiNhan.tinhThanh).toBe('')
    expect(w.vm.hd.p.nguoiNhan.phuongXa).toBe('')
    expect(w.vm.hd.p.nguoiNhan.diaChiCuThe).toBe('')
  })

  it('SĐT người nhận: chỉ nhận chữ số, tối đa 10 số; rời ô là báo lỗi nếu chưa đúng 10 số bắt đầu bằng 0', async () => {
    const { w } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    await w.find('[data-testid="bh-giao-hang"]').setValue(true)
    await flushPromises()
    const o = w.find('#bh-nn-sdt')
    await o.setValue('a09.01-000')
    expect(o.element.value).toBe('0901000')
    expect(w.text()).not.toContain('Số điện thoại gồm 10 chữ số')
    await o.trigger('blur')
    expect(w.text()).toContain('Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.')
    await o.setValue('1901000002')
    expect(w.text()).toContain('Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.')
    await o.setValue('090100000299')
    expect(o.element.value).toBe('0901000002')
    expect(w.text()).not.toContain('Số điện thoại gồm 10 chữ số')
  })

  it('tiền mặt khách đưa tự điền đúng số khách cần trả; nhân viên sửa thì giữ số đã sửa và tính tiền thừa', async () => {
    const { w } = await moBanHang()
    await nutThem(w).trigger('click')
    await flushPromises()
    const o = w.find('#bh-tien-mat')
    expect(o.element.value).toBe('3.861.000')
    await o.setValue('4000000')
    await flushPromises()
    expect(w.find('.bh-thua').text()).toContain('139.000')
    await w.find('#bh-pt-CHUYEN_KHOAN').setValue(true)
    await w.find('#bh-pt-TIEN_MAT').setValue(true)
    await flushPromises()
    expect(w.find('#bh-tien-mat').element.value).toBe('3.861.000') // đổi sang chuyển khoản rồi quay lại thì điền lại đúng số
  })

  it('chuyển khoản: QR giả lập như online; khách xác nhận trên ngân hàng giả thì tự thanh toán kèm mã giao dịch', async () => {
    const { w, may } = await moBanHang()
    may.ck = { maHoaDon: 'HD100', soTien: 3861000, daNhan: false, maGiaoDich: null, diaChiLan: null }
    await nutThem(w).trigger('click')
    await flushPromises()
    const { open } = cuaSoInGia()
    await w.find('#bh-pt-CHUYEN_KHOAN').setValue(true)
    // Chờ theo kết quả, không canh giờ cứng: chạy song song trên máy bận thì mã QR và lần hỏi 3 giây có thể đến muộn
    await vi.waitFor(() => expect(w.find('[data-testid="bh-qr-gia-lap"] .bh-qr-anh img').exists()).toBe(true), { timeout: 8000, interval: 50 })
    expect(api.post).toHaveBeenCalledWith('/ban-hang/hoa-don-cho/100/chuyen-khoan', { soTien: 3861000 })
    expect(w.find('[data-testid="bh-qr-gia-lap"]').text()).toContain('Đang chờ khách chuyển khoản')
    // Review: ô chuyển khoản ghi đủ thông tin, mã QR to hơn (210px), bấm để phóng to
    const ttCk = w.find('[data-testid="bh-qr-thong-tin"]').text()
    for (const chu of ['Ngân hàngMB', 'Số tài khoản0123456789', 'Chủ tài khoảnFOOTSTYLE DEMO', 'Số tiền3.861.000', 'Nội dungHD100']) expect(ttCk.replace(/\s+/g, '')).toContain(chu.replace(/\s+/g, ''))
    // Đủ thông tin sản phẩm ngay trong khung chuyển khoản: tên, mã, phân loại, số lượng, đơn giá, thành tiền
    const spCk = w.find('[data-testid="bh-qr-san-pham"]').text().replace(/\s+/g, '')
    for (const chu of ['Yonex Astrox 99 Pro', 'SP01-DEN-4U-G5', 'Đen · 4U · G5', '1 × 3.861.000', '3.861.000']) expect(spCk).toContain(chu.replace(/\s+/g, ''))
    expect(w.find('.bh-qr-anh img').attributes('width')).toBe('210')
    await w.find('.bh-qr-anh').trigger('click')
    expect(body().find('[data-testid="bh-qr-to"] img').attributes('width')).toBe('340')
    expect(body().find('[data-testid="bh-qr-to"] .bh-qr-sp').text()).toContain('Yonex Astrox 99 Pro')
    expect(may.thanhToan).toBeNull()
    // khách bấm "Xác nhận chuyển tiền" trên trang ngân hàng giả
    may.ck = { ...may.ck, daNhan: true, maGiaoDich: 'GIALAP-HD100-1' }
    await vi.waitFor(() => expect(may.thanhToan).toEqual(expect.objectContaining({ tienMat: 0, chuyenKhoan: 3861000, maGiaoDich: 'GIALAP-HD100-1' })), { timeout: 12000, interval: 100 })
    // Khách quét mã xong là tự in: không có cú bấm nên không mở cửa sổ mới (trình duyệt sẽ chặn), in qua khung ẩn trong trang
    expect(open).not.toHaveBeenCalled()
    const khungIn = () => document.querySelector('iframe[aria-hidden="true"]')
    await vi.waitFor(() => expect(khungIn()?.contentDocument.body.textContent).toContain('HD100'), { timeout: 5000 })
    khungIn().remove()
  }, 30000)

  it('phiếu: luôn tự áp phiếu tốt nhất, không cho chọn; ghi chú mua thêm bao nhiêu tiền / sản phẩm thì được phiếu mới', async () => {
    const PHIEUS = [
      { id: 1, ma: 'GIAM50K', ten: 'Giảm 50k', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 },
      {
        id: 2, ma: 'VIP10', ten: 'Khách VIP', loaiGiam: 'PHAN_TRAM', giaTri: 10, donToiThieu: 5000000, apDungDuoc: false,
        lyDo: 'Đơn tối thiểu 5.000.000 ₫ (còn thiếu 1.139.000 ₫).', soTienGiam: 0, canMuaThem: 1139000, giamKhiDu: 500000,
      },
    ]
    const { w } = await moBanHang({ phieus: () => PHIEUS })
    await nutThem().trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.find('.bh-o-phieu').exists()).toBe(false)
    const apDung = () => w.find('[data-testid="bh-phieu-ap-dung"]')
    expect(apDung().text()).toContain('Đang áp dụng phiếu GIAM50K (giảm 50.000')
    expect(apDung().text()).toContain('Tốt nhất')
    expect(w.find('.bh-tien-giam').text()).toContain('-50.000')
    // Dòng phiếu chỉ để xem: không bấm được, không mũi tên, bấm hay Enter cũng không mở pop-up
    const the = w.find('#bh-phieu')
    expect(the.attributes('role')).toBeUndefined()
    expect(the.attributes('tabindex')).toBeUndefined()
    expect(the.find('.bi-chevron-right').exists()).toBe(false)
    await the.trigger('click')
    await the.trigger('keydown.enter')
    await flushPromises()
    expect(body().text()).not.toContain('Chọn phiếu giảm giá')
    expect(apDung().text()).toContain('Đang áp dụng phiếu GIAM50K')
    // Ghi chú: thiếu 1.139.000 ₫, giỏ có giá 3.861.000 ₫ nên thêm 1 sản phẩm là đủ
    const ghiChu = w.find('[data-testid="bh-goi-y-ngan"]').text().replace(/\s+/g, ' ')
    expect(ghiChu).toContain('Mua thêm 1.139.000 ₫ (thêm 1 sản phẩm Yonex Astrox 99 Pro là đủ) để dùng phiếu VIP10 (giảm 500.000 ₫)')
  })

  it('phiếu: có phiếu mới giảm nhiều hơn thì không tự đổi, bỏ nhãn Tốt nhất, hiện nút nhỏ "Đổi phiếu tốt nhất"; bấm thì đổi', async () => {
    let ds = [{ id: 1, ma: 'GIAM50K', ten: 'Giảm 50k', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 }]
    const { w } = await moBanHang({ phieus: () => ds })
    await nutThem().trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    const apDung = () => w.find('[data-testid="bh-phieu-ap-dung"]')
    const nutDoi = () => w.find('[data-testid="bh-doi-phieu-tot"]')
    expect(apDung().text()).toContain('Tốt nhất')
    expect(nutDoi().exists()).toBe(false)
    // Quản lý vừa tạo phiếu giảm 200k (máy chủ xếp lên đầu); giỏ đổi nên tải lại phiếu
    ds = [{ id: 2, ma: 'GIAM200K', ten: 'Giảm 200k', loaiGiam: 'SO_TIEN', giaTri: 200000, apDungDuoc: true, soTienGiam: 200000 }, ...ds]
    await nutThem().trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(apDung().text()).toContain('Đang áp dụng phiếu GIAM50K')
    expect(apDung().text()).not.toContain('Tốt nhất')
    // Nút nhỏ ở mép khung: chỉ icon, rê chuột / đọc màn hình mới thấy chữ
    expect(nutDoi().text()).toBe('')
    expect(nutDoi().find('.bi-stars').exists()).toBe(true)
    expect(nutDoi().attributes('title')).toBe('Đổi phiếu tốt nhất: GIAM200K (giảm 200.000 ₫)')
    expect(nutDoi().attributes('aria-label')).toBe('Đổi phiếu tốt nhất GIAM200K')
    await nutDoi().trigger('click')
    await flushPromises()
    expect(apDung().text()).toContain('Đang áp dụng phiếu GIAM200K (giảm 200.000')
    expect(apDung().text()).toContain('Tốt nhất')
    expect(nutDoi().exists()).toBe(false)
    expect(w.find('.bh-tien-giam').text()).toContain('-200.000')
  })

  it('phiếu: đã kịch mức giảm cao nhất thì không hiện ghi chú', async () => {
    const { w } = await moBanHang({
      phieus: () => [{ id: 1, ma: 'GIAM50K', ten: 'Giảm 50k', loaiGiam: 'TIEN_MAT', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 }],
    })
    await nutThem().trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.find('[data-testid="bh-phieu-ap-dung"]').text()).toContain('Đang áp dụng phiếu GIAM50K')
    expect(w.find('[data-testid="bh-goi-y-ngan"]').exists()).toBe(false)
  })

  // Cô test case khó 14/11 [00:42–02:57]: phiếu đổi giữa chừng thì bấm Thanh toán phải HỎI, không tự đổi sau lưng nhân viên
  const hopPhieu = () => new DOMWrapper(document.body).find('[data-testid="bh-hoi-phieu"]')
  const nutTrongHopPhieu = (chu) => hopPhieu().findAll('button').find((b) => b.text().includes(chu))

  it('phiếu bị sửa mức giảm lúc đang chờ: bấm Thanh toán thì báo mức giảm mới, đồng ý mới mở xác nhận với số tiền mới', async () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    let giam = 50000
    const { w, may } = await moBanHang({ phieus: () => [{ ma: 'GIAM50K', loaiGiam: 'SO_TIEN', giaTri: giam, apDungDuoc: true, soTienGiam: giam }] })
    await nutThem(w).trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.find('.bh-nut-tt').text()).toContain('3.811.000')

    giam = 30000
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(hopPhieu().text()).toContain('Phiếu GIAM50K vừa đổi mức giảm')
    expect(may.thanhToan).toBeFalsy()
    await nutTrongHopPhieu('Tiếp tục thanh toán').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toEqual(expect.objectContaining({ maPhieu: 'GIAM50K', soTienGiam: 30000 }))
  })

  it('vừa có phiếu tốt hơn: hỏi áp dụng không; chọn giữ phiếu cũ thì thanh toán bằng phiếu cũ', async () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    let ds = [{ ma: 'GIAM50K', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 }]
    const { w, may } = await moBanHang({ phieus: () => ds })
    await nutThem(w).trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    ds = [{ ma: 'GIAM90K', loaiGiam: 'SO_TIEN', giaTri: 90000, apDungDuoc: true, soTienGiam: 90000 }, ...ds]
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(hopPhieu().text()).toContain('Vừa có phiếu GIAM90K')
    await nutTrongHopPhieu('Giữ phiếu GIAM50K').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toEqual(expect.objectContaining({ maPhieu: 'GIAM50K', soTienGiam: 50000 }))
  })

  it('góp ý 21/11: có phiếu tốt hơn khi đang lên đơn thì KHÔNG tự đổi, đơn giữ phiếu cũ; bấm Thanh toán mới hỏi, chọn Áp dụng thì đổi', async () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    let ds = [{ ma: 'GIAM50K', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 }]
    const { w, may } = await moBanHang({ phieus: () => ds })
    await nutThem(w).trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.text()).toContain('GIAM50K')
    ds = [{ ma: 'GIAM90K', loaiGiam: 'SO_TIEN', giaTri: 90000, apDungDuoc: true, soTienGiam: 90000 }, ...ds]
    await nutThem(w).trigger('click') // giỏ đổi -> tải lại danh sách phiếu (đã có GIAM90K)
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.text()).toContain('GIAM50K')
    expect(w.text()).not.toContain('GIAM90K')
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(hopPhieu().text()).toContain('Vừa có phiếu GIAM90K')
    await nutTrongHopPhieu('Áp dụng GIAM90K').trigger('click')
    await flushPromises()
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan).toEqual(expect.objectContaining({ maPhieu: 'GIAM90K', soTienGiam: 90000 }))
  })

  it('góp ý 21/11: phiếu đang áp bị hủy / không còn đủ điều kiện thì không tự thay, báo đỏ; bấm Thanh toán hỏi áp phiếu khác hoặc không dùng phiếu', async () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    let ds = [{ ma: 'GIAM50K', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: true, soTienGiam: 50000 }]
    const { w, may } = await moBanHang({ phieus: () => ds })
    await nutThem(w).trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    ds = [
      { ma: 'GIAM30K', loaiGiam: 'SO_TIEN', giaTri: 30000, apDungDuoc: true, soTienGiam: 30000 },
      { ma: 'GIAM50K', loaiGiam: 'SO_TIEN', giaTri: 50000, apDungDuoc: false, soTienGiam: 0, lyDo: 'Phiếu đang ngừng hoạt động.' },
    ]
    await nutThem(w).trigger('click')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.text()).toContain('Phiếu đang ngừng hoạt động.')
    expect(w.text()).not.toContain('GIAM30K') // không tự thay sang phiếu khác
    await w.find('.bh-nut-tt').trigger('click')
    await flushPromises()
    expect(hopPhieu().text()).toContain('Phiếu GIAM50K không còn dùng được')
    expect(hopPhieu().text()).toContain('GIAM30K')
    await nutTrongHopPhieu('Thanh toán không dùng phiếu').trigger('click')
    await flushPromises()
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    expect(w.text()).not.toContain('GIAM30K') // đã chọn không dùng phiếu: không tự áp lại
    await nutXacNhan().trigger('click')
    await flushPromises()
    expect(may.thanhToan.maPhieu || '').toBe('')
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

  it('tự điền tiền mặt + chuyển khoản ca trước; sửa khác hệ thống thì phải ghi lý do, xóa trống thì báo thiếu', async () => {
    const w = moTrang()
    await flushPromises()
    expect(w.find('[data-testid="vc-tien-mat-truoc"]').text()).toContain('1.000.000')
    expect(w.find('[data-testid="vc-ck-truoc"]').text()).toContain('1.500.000')
    expect(w.find('[data-testid="vc-dung-so"]').exists()).toBe(false) // không còn nút "Dùng số tiền này"
    await w.find('#vc-chuyen-khoan').setValue('1200000')
    await w.find('#vc-form').trigger('submit')
    expect(w.text()).toContain('Chuyển khoản ca trước khác số hệ thống ghi nhận')
    await w.find('#vc-tien-mat').setValue('')
    await w.find('#vc-form').trigger('submit')
    expect(w.text()).toContain('Vui lòng nhập tiền mặt đầu ca.')
    expect(api.post).not.toHaveBeenCalled()
  })

  it('thông báo ca cần nhận hôm nay, nhận được từ 15 phút trước giờ bắt đầu', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 2, 11, 30))
    const w = moTrang()
    await flushPromises()
    const tb = w.find('[data-testid="vc-ca-can-nhan"]')
    expect(tb.text()).toContain('Ca chiều')
    expect(tb.text()).toContain('nhận ca từ 11:45')
    expect(tb.text()).toContain('chưa đến giờ nhận ca')
    vi.useRealTimers()
  })

  it('số ca trước đã điền sẵn: bắt đầu ca xong vào trang định mở', async () => {
    state.query = { quayLai: '/hoa-don' }
    const w = moTrang()
    await flushPromises()
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
