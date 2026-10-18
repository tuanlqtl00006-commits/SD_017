import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { svc, api, push, state } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), getById: vi.fn(), create: vi.fn(), update: vi.fn(), toggleActive: vi.fn() },
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  push: vi.fn(),
  state: { params: {} },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: state.params }),
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/khachHangService', () => ({ khachHangService: svc }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/diaChiHanhChinhService', () => {
  const tinh = [{ code: 1, name: 'Thành phố Hà Nội' }, { code: 33, name: 'Tỉnh Hưng Yên' }]
  const phuong = { 1: [{ code: 10, name: 'Phường Hà Đông' }], 33: [{ code: 20, name: 'Phường Thái Bình' }] }
  return { API_DIA_CHI: 'x', diaChiHanhChinhService: { layTinhThanh: async () => tinh, layPhuongXa: async (m) => phuong[m] ?? [], xoaNho() {} } }
})

import KhachHangFormPage from '../src/components/khachHang/KhachHangFormPage.vue'
import { useToast } from '../src/composables/useToast'
import { LOI_EMAIL, LOI_HO_TEN_KY_TU, LOI_SDT } from '../src/utils/kiemTra'
import { cacMucSelect2, chuSelect2, datSelect2, giaTriSelect2 } from './helpersSelect2'

const KHACH_CO_SAN = [
  { id: 1, maKhachHang: 'KH01', hoTen: 'Nguyễn Thị Lan', sdt: '0901000001', email: 'lan.nt@gmail.com', gioiTinh: 0, trangThai: 1, ngaySinh: '1995-03-12', diaChiList: [] },
  { id: 2, maKhachHang: 'KH02', hoTen: 'Trần Văn Hùng', sdt: '0901000002', email: 'hung.tv@gmail.com', gioiTinh: 1, trangThai: 1, ngaySinh: null, diaChiList: [] },
]

const body = () => new DOMWrapper(document.body)
const loiO = (w, id) => w.find(`#${id}`).text()
const coLoi = (w, id) => w.find(`#${id}`).exists()
const nutLuu = (w) => w.find('button[type=submit]')

async function moForm(ds = KHACH_CO_SAN) {
  svc.getAll.mockResolvedValue(ds)
  const w = mount(KhachHangFormPage, { attachTo: document.body })
  await flushPromises()
  return w
}

async function dienHopLe(w, ghiDe = {}) {
  const v = { ten: 'Nguyễn Thị Mai', sdt: '0912345678', email: 'Mai@Gmail.com', chiTiet: 'Số 12 Nguyễn Trãi', ...ghiDe }
  await w.find('#kh-ho-ten').setValue(v.ten)
  await w.find('#kh-sdt').setValue(v.sdt)
  await w.find('#kh-email').setValue(v.email)
  await datSelect2(w, '#kh-dia-chi-tinh', 'Tỉnh Hưng Yên')
  await flushPromises()
  await datSelect2(w, '#kh-dia-chi-phuong', 'Phường Thái Bình')
  await w.find('#kh-dia-chi-cu-the').setValue(v.chiTiet)
}

beforeEach(() => {
  document.body.innerHTML = ''
  state.params = {}
  Object.values(svc).forEach((f) => f.mockReset())
  Object.values(api).forEach((f) => f.mockReset())
  push.mockReset()
})
afterEach(() => vi.useRealTimers())

describe('Thêm khách hàng: bố cục và ô chọn', () => {
  it('dùng radio cho giới tính, Select2 cho tỉnh và phường (không còn <select> thường); chỉ có Tỉnh/Thành phố và Phường/Xã, không có Quận/Huyện', async () => {
    const w = await moForm()
    expect(w.findAll('select')).toHaveLength(0)
    expect(w.findAll('.s2-trigger').map((s) => s.attributes('id'))).toEqual(['kh-dia-chi-tinh', 'kh-dia-chi-phuong'])
    expect(w.text()).toContain('Tỉnh/Thành phố')
    expect(w.text()).toContain('Phường/Xã')
    expect(w.text()).not.toContain('Quận/Huyện')
    expect(await cacMucSelect2(w, '#kh-dia-chi-tinh')).toEqual(['Thành phố Hà Nội', 'Tỉnh Hưng Yên'])
    expect(w.find('#kh-gioi-tinh-nam').element.checked).toBe(true)
    expect(w.find('#kh-gioi-tinh-nu').element.checked).toBe(false)
    w.unmount()
  })

  it('chưa nhập gì thì không hiện dòng nhắc hay lỗi dưới các ô', async () => {
    const w = await moForm()
    expect(w.findAll('.ad-error')).toHaveLength(0)
    expect(coLoi(w, 'kh-sdt-msg')).toBe(false)
    expect(coLoi(w, 'kh-ngay-sinh-msg')).toBe(false)
    expect(w.text()).toContain('Thêm khách hàng mới')
    w.unmount()
  })

  it('có nút Hủy (về danh sách) và Tạo khách hàng', async () => {
    const w = await moForm()
    expect(nutLuu(w).text()).toContain('Tạo khách hàng')
    await w.findAll('button').find((b) => b.text() === 'Hủy').trigger('click')
    expect(push).toHaveBeenCalledWith('/khach-hang')
    w.unmount()
  })
})

describe('Thêm khách hàng: lỗi hiện dưới từng ô', () => {
  it('bấm tạo khi để trống: báo lỗi ở TỪNG ô bắt buộc, không gọi API, con trỏ nhảy tới ô lỗi đầu tiên', async () => {
    const w = await moForm()
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).not.toHaveBeenCalled()
    expect(loiO(w, 'kh-ho-ten-msg')).toBe('Vui lòng nhập họ tên khách hàng.')
    expect(loiO(w, 'kh-sdt-msg')).toBe('Vui lòng nhập số điện thoại.')
    expect(loiO(w, 'kh-email-msg')).toBe('Vui lòng nhập email.')
    expect(loiO(w, 'kh-dia-chi-tinh-msg')).toBe('Chọn Tỉnh/Thành phố.')
    expect(loiO(w, 'kh-dia-chi-phuong-msg')).toBe('Chọn Phường/Xã.')
    expect(loiO(w, 'kh-dia-chi-cu-the-msg')).toBe('Nhập địa chỉ cụ thể (số nhà, đường...).')
    expect(w.find('#kh-ho-ten').classes()).toContain('is-invalid')
    expect(w.find('#kh-ho-ten').attributes('aria-invalid')).toBe('true')
    expect(document.activeElement?.id).toBe('kh-ho-ten')
    // ô Ngày sinh để trống là hợp lệ (không bắt buộc)
    expect(coLoi(w, 'kh-ngay-sinh-msg')).toBe(false)
    w.unmount()
  })

  it('lỗi của ô chỉ hiện khi đã chạm vào ô đó: rời ô SĐT mới báo lỗi SĐT, ô email chưa chạm thì chưa báo', async () => {
    const w = await moForm()
    await w.find('#kh-sdt').setValue('123')
    await w.find('#kh-sdt').trigger('blur')
    expect(loiO(w, 'kh-sdt-msg')).toBe(LOI_SDT)
    expect(coLoi(w, 'kh-email-msg')).toBe(false)
    w.unmount()
  })

  it('SĐT: đầu số 01/02/04/06 bị chặn, có chữ / thiếu số / thừa số bị chặn; 03, 05, 07, 08, 09 hợp lệ', async () => {
    const w = await moForm()
    for (const sai of ['0112345678', '0212345678', '0412345678', '0612345678', '09123abc45', '091234567', '09123456789', '0912 345 678']) {
      await w.find('#kh-sdt').setValue(sai)
      expect(loiO(w, 'kh-sdt-msg'), sai).toBe(LOI_SDT)
      expect(w.find('#kh-sdt').classes()).toContain('is-invalid')
    }
    for (const dau of ['03', '05', '07', '08', '09']) {
      await w.find('#kh-sdt').setValue(`${dau}12345678`)
      expect(coLoi(w, 'kh-sdt-msg'), dau).toBe(false)
      expect(w.find('#kh-sdt').classes()).not.toContain('is-invalid')
    }
    w.unmount()
  })

  it('email: sai định dạng báo lỗi, đúng thì hết lỗi', async () => {
    const w = await moForm()
    for (const sai of ['an', 'an@', 'an@mail', 'an@mail.v', '@mail.vn', 'an @mail.vn']) {
      await w.find('#kh-email').setValue(sai)
      expect(loiO(w, 'kh-email-msg'), sai).toBe(LOI_EMAIL)
    }
    await w.find('#kh-email').setValue('an@mail.vn')
    expect(coLoi(w, 'kh-email-msg')).toBe(false)
    w.unmount()
  })

  it('họ tên có chữ số / ký tự đặc biệt bị chặn, quá dài bị chặn', async () => {
    const w = await moForm()
    for (const sai of ['An 123', 'Nguyễn@An', '12345']) {
      await w.find('#kh-ho-ten').setValue(sai)
      expect(loiO(w, 'kh-ho-ten-msg'), sai).toBe(LOI_HO_TEN_KY_TU)
    }
    await w.find('#kh-ho-ten').setValue('A')
    expect(loiO(w, 'kh-ho-ten-msg')).toBe('Họ tên từ 2 đến 100 ký tự.')
    await w.find('#kh-ho-ten').setValue('Nguyễn Thị Ánh Tuyết')
    expect(coLoi(w, 'kh-ho-ten-msg')).toBe(false)
    w.unmount()
  })

  it('trùng SĐT / email với khách đã có: báo ngay khi gõ, nêu rõ khách nào; email trùng không phân biệt hoa thường', async () => {
    const w = await moForm()
    await w.find('#kh-sdt').setValue('0901000001')
    expect(loiO(w, 'kh-sdt-msg')).toBe('Số điện thoại đã tồn tại (khách hàng Nguyễn Thị Lan).')
    await w.find('#kh-email').setValue('HUNG.TV@gmail.com')
    expect(loiO(w, 'kh-email-msg')).toBe('Email đã tồn tại (khách hàng Trần Văn Hùng).')
    await w.find('#kh-sdt').setValue('0901000009')
    await w.find('#kh-email').setValue('moi@gmail.com')
    expect(coLoi(w, 'kh-email-msg')).toBe(false)
    expect(coLoi(w, 'kh-sdt-msg')).toBe(false)
    w.unmount()
  })

  it('tuổi: đủ 15 tuổi đúng hôm nay thì được, thiếu một ngày bị chặn; tương lai và quá 100 tuổi bị chặn', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 15, 9, 0, 0)) // 15/10/2026
    const w = await moForm()
    const dat = async (v) => {
      await w.find('#kh-ngay-sinh').setValue(v)
      await w.find('#kh-ngay-sinh').trigger('blur')
    }
    await dat('15/10/2011') // đúng 15 tuổi hôm nay
    expect(coLoi(w, 'kh-ngay-sinh-msg')).toBe(false)
    await dat('16/10/2011') // thiếu đúng một ngày
    expect(loiO(w, 'kh-ngay-sinh-msg')).toBe('Khách hàng phải từ đủ 15 tuổi.')
    expect(w.find('#kh-ngay-sinh').classes()).toContain('is-invalid')
    await dat('01/01/2020')
    expect(loiO(w, 'kh-ngay-sinh-msg')).toBe('Khách hàng phải từ đủ 15 tuổi.')
    await dat('16/10/2026')
    expect(loiO(w, 'kh-ngay-sinh-msg')).toBe('Ngày sinh không được ở tương lai.')
    await dat('15/10/1925')
    expect(loiO(w, 'kh-ngay-sinh-msg')).toBe('Ngày sinh không hợp lệ (quá 100 tuổi).')
    await dat('20/05/1999')
    expect(coLoi(w, 'kh-ngay-sinh-msg')).toBe(false)
    w.unmount()
  })

  it('sửa lại ô lỗi cho đúng thì lỗi biến mất ngay, không cần bấm lưu lại', async () => {
    const w = await moForm()
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(loiO(w, 'kh-email-msg')).toBe('Vui lòng nhập email.')
    await w.find('#kh-email').setValue('mai@gmail.com')
    expect(coLoi(w, 'kh-email-msg')).toBe(false)
    expect(loiO(w, 'kh-sdt-msg')).toBe('Vui lòng nhập số điện thoại.') // ô khác vẫn còn lỗi
    w.unmount()
  })
})

describe('Thêm khách hàng: gửi lên backend', () => {
  it('đủ thông tin: gửi dữ liệu đã chuẩn hóa (không có quận/huyện), báo thành công và về danh sách', async () => {
    svc.create.mockResolvedValue({ id: 9 })
    const w = await moForm()
    await dienHopLe(w, { ten: '  Nguyễn   Thị  Mai ', email: ' Mai@Gmail.com ' })
    await w.find('#kh-gioi-tinh-nu').setValue(true)
    await w.find('#kh-ngay-sinh').setValue('20/05/1999')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    const p = svc.create.mock.calls[0][0]
    expect(p).toEqual({
      hoTen: 'Nguyễn Thị Mai',
      sdt: '0912345678',
      email: 'mai@gmail.com',
      ngaySinh: '1999-05-20',
      gioiTinh: 0,
      trangThai: 1,
      tenNguoiNhan: 'Nguyễn Thị Mai',
      sdtNguoiNhan: '0912345678',
      tinhThanh: 'Tỉnh Hưng Yên',
      phuongXa: 'Phường Thái Bình',
      diaChiCuThe: 'Số 12 Nguyễn Trãi',
    })
    expect(p).not.toHaveProperty('quanHuyen')
    expect(push).toHaveBeenCalledWith('/khach-hang')
    expect(useToast().toasts.at(-1).message).toBe('Đã tạo khách hàng mới.')
    w.unmount()
  })

  it('không nhập ngày sinh thì gửi null', async () => {
    svc.create.mockResolvedValue({ id: 9 })
    const w = await moForm()
    await dienHopLe(w)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create.mock.calls[0][0].ngaySinh).toBeNull()
    w.unmount()
  })

  it('không bấm lưu hai lần liên tiếp khi đang gửi', async () => {
    let xong
    svc.create.mockImplementation(() => new Promise((r) => (xong = r)))
    const w = await moForm()
    await dienHopLe(w)
    await w.find('form').trigger('submit')
    await w.find('form').trigger('submit')
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(nutLuu(w).attributes('disabled')).toBeDefined()
    xong({ id: 1 })
    await flushPromises()
    w.unmount()
  })
})

describe('Thêm khách hàng: lỗi từ backend gắn xuống đúng ô', () => {
  const loiMayChu = (msg, loiTheoTruong, status = 409) => Object.assign(new Error(msg), { status, loiTheoTruong })

  it('409 trùng email: lỗi nằm dưới ô email (không phải thông báo chung), ở lại trang; sửa email thì lỗi mất', async () => {
    svc.create.mockRejectedValue(loiMayChu('Email đã tồn tại.', { email: 'Email đã tồn tại.' }))
    const w = await moForm()
    await dienHopLe(w, { email: 'moi@gmail.com' }) // chưa có trong danh sách đã tải (người khác vừa tạo)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(push).not.toHaveBeenCalled()
    expect(loiO(w, 'kh-email-msg')).toBe('Email đã tồn tại.')
    expect(w.find('#kh-email').classes()).toContain('is-invalid')
    expect(document.activeElement?.id).toBe('kh-email')
    await w.find('#kh-email').setValue('moi2@gmail.com')
    expect(coLoi(w, 'kh-email-msg')).toBe(false)
    w.unmount()
  })

  it('400 nhiều ô cùng lúc: mỗi lỗi hiện đúng ô của nó (SĐT, ngày sinh)', async () => {
    svc.create.mockRejectedValue(loiMayChu('Số điện thoại gồm 10 chữ số.', { sdt: LOI_SDT, ngaySinh: 'Khách hàng phải từ đủ 15 tuổi.' }, 400))
    const w = await moForm()
    await dienHopLe(w)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(loiO(w, 'kh-sdt-msg')).toBe(LOI_SDT)
    expect(loiO(w, 'kh-ngay-sinh-msg')).toBe('Khách hàng phải từ đủ 15 tuổi.')
    w.unmount()
  })

  it('lỗi không có thông tin theo ô (mất mạng, 500) thì báo bằng thông báo chung', async () => {
    svc.create.mockRejectedValue(Object.assign(new Error('Không kết nối được máy chủ.'), { status: null, loiTheoTruong: null }))
    const w = await moForm()
    await dienHopLe(w)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(useToast().toasts.at(-1).message).toBe('Không kết nối được máy chủ.')
    expect(push).not.toHaveBeenCalled()
    w.unmount()
  })
})

describe('Sửa khách hàng: kiểm tra y hệt form thêm', () => {
  const KH = { id: 2, maKhachHang: 'KH02', hoTen: 'Trần Văn Hùng', sdt: '0901000002', email: 'hung.tv@gmail.com', gioiTinh: 1, trangThai: 1, ngaySinh: '1992-07-25', diaChiList: [{ id: 5, tenNguoiNhan: 'Hùng', sdtNguoiNhan: '0901000002', tinhThanhPho: 'Thành phố Hồ Chí Minh', phuongXa: 'Phường Bến Thành', diaChiCuThe: '45 Lê Lợi', laDiaChiMacDinh: true }] }
  async function moSua(khach = KH) {
    state.params = { id: '2' }
    svc.getById.mockResolvedValue(khach)
    return moForm()
  }

  it('nạp đúng dữ liệu; hiện mã, trạng thái (Select2); không có ô địa chỉ mới mà có nút Quản lý địa chỉ', async () => {
    const w = await moSua()
    expect(svc.getById).toHaveBeenCalledWith(2)
    expect(w.find('#kh-ho-ten').element.value).toBe('Trần Văn Hùng')
    expect(w.find('#kh-sdt').element.value).toBe('0901000002')
    expect(w.find('#kh-email').element.value).toBe('hung.tv@gmail.com')
    expect(w.find('#kh-ngay-sinh').element.value).toBe('25/07/1992')
    expect(w.find('#kh-gioi-tinh-nam').element.checked).toBe(true)
    expect(giaTriSelect2(w, '#kh-trang-thai')).toBe('1')
    expect(w.find('.ad-code').text()).toContain('KH02')
    expect(w.find('#kh-dia-chi-tinh').exists()).toBe(false)
    expect(w.text()).toContain('45 Lê Lợi, Phường Bến Thành, Thành phố Hồ Chí Minh')
    expect(w.findAll('button').some((b) => b.text().includes('Quản lý địa chỉ'))).toBe(true)
    expect(w.findAll('select')).toHaveLength(0)
    expect(nutLuu(w).text()).toContain('Lưu thay đổi')
    w.unmount()
  })

  it('xóa hết các ô bắt buộc rồi lưu: báo lỗi từng ô như form thêm, không gọi API', async () => {
    const w = await moSua()
    await w.find('#kh-ho-ten').setValue('')
    await w.find('#kh-sdt').setValue('')
    await w.find('#kh-email').setValue('')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.update).not.toHaveBeenCalled()
    expect(loiO(w, 'kh-ho-ten-msg')).toBe('Vui lòng nhập họ tên khách hàng.')
    expect(loiO(w, 'kh-sdt-msg')).toBe('Vui lòng nhập số điện thoại.')
    expect(loiO(w, 'kh-email-msg')).toBe('Vui lòng nhập email.')
    w.unmount()
  })

  it('SĐT sai đầu số / email sai / tên có số / tuổi dưới 15 đều bị chặn khi sửa', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 15, 9, 0, 0))
    const w = await moSua()
    await w.find('#kh-sdt').setValue('0212345678')
    await w.find('#kh-email').setValue('hung@')
    await w.find('#kh-ho-ten').setValue('Hùng 2')
    await w.find('#kh-ngay-sinh').setValue('01/01/2020')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.update).not.toHaveBeenCalled()
    expect(loiO(w, 'kh-sdt-msg')).toBe(LOI_SDT)
    expect(loiO(w, 'kh-email-msg')).toBe(LOI_EMAIL)
    expect(loiO(w, 'kh-ho-ten-msg')).toBe(LOI_HO_TEN_KY_TU)
    expect(loiO(w, 'kh-ngay-sinh-msg')).toBe('Khách hàng phải từ đủ 15 tuổi.')
    w.unmount()
  })

  it('giữ nguyên SĐT / email của chính mình thì KHÔNG báo trùng; đổi sang của người khác thì báo trùng', async () => {
    const w = await moSua()
    await w.find('#kh-sdt').setValue('0901000002')
    await w.find('#kh-email').setValue('HUNG.TV@gmail.com')
    expect(coLoi(w, 'kh-sdt-msg')).toBe(false)
    expect(coLoi(w, 'kh-email-msg')).toBe(false)
    await w.find('#kh-sdt').setValue('0901000001') // của Nguyễn Thị Lan
    expect(loiO(w, 'kh-sdt-msg')).toBe('Số điện thoại đã tồn tại (khách hàng Nguyễn Thị Lan).')
    await w.find('#kh-email').setValue('lan.nt@gmail.com')
    expect(loiO(w, 'kh-email-msg')).toBe('Email đã tồn tại (khách hàng Nguyễn Thị Lan).')
    w.unmount()
  })

  it('lưu thành công: gọi cập nhật đúng id với dữ liệu chuẩn hóa (không gửi địa chỉ), đổi được trạng thái và giới tính', async () => {
    svc.update.mockResolvedValue({})
    const w = await moSua()
    await w.find('#kh-ho-ten').setValue('  Trần  Văn  Hùng Anh ')
    await w.find('#kh-gioi-tinh-nu').setValue(true)
    await datSelect2(w, '#kh-trang-thai', '0')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.update).toHaveBeenCalledTimes(1)
    const [id, p] = svc.update.mock.calls[0]
    expect(id).toBe(2)
    expect(p).toEqual({ hoTen: 'Trần Văn Hùng Anh', sdt: '0901000002', email: 'hung.tv@gmail.com', ngaySinh: '1992-07-25', gioiTinh: 0, trangThai: 0 })
    expect(push).toHaveBeenCalledWith('/khach-hang')
    expect(useToast().toasts.at(-1).message).toBe('Đã cập nhật khách hàng.')
    w.unmount()
  })

  it('xóa ngày sinh được (không bắt buộc) -> gửi null; backend báo trùng email (409) thì hiện dưới ô email', async () => {
    svc.update.mockRejectedValue(Object.assign(new Error('Email đã tồn tại.'), { status: 409, loiTheoTruong: { email: 'Email đã tồn tại.' } }))
    const w = await moSua()
    await w.find('#kh-ngay-sinh').setValue('')
    await w.find('#kh-email').setValue('moi@gmail.com')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.update.mock.calls[0][1].ngaySinh).toBeNull()
    expect(loiO(w, 'kh-email-msg')).toBe('Email đã tồn tại.')
    expect(push).not.toHaveBeenCalled()
    w.unmount()
  })

  it('nút Quản lý địa chỉ mở cửa sổ địa chỉ của đúng khách hàng', async () => {
    api.get.mockResolvedValue({ data: KH.diaChiList })
    const w = await moSua()
    await w.findAll('button').find((b) => b.text().includes('Quản lý địa chỉ')).trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/dia-chi/khach-hang/2')
    expect(body().text()).toContain('Địa chỉ của Trần Văn Hùng')
    w.unmount()
  })

  it('khách không tồn tại (đã bị xóa): báo lỗi và quay về danh sách', async () => {
    state.params = { id: '99' }
    svc.getById.mockRejectedValue(new Error('Không tìm thấy khách hàng.'))
    svc.getAll.mockResolvedValue(KHACH_CO_SAN)
    const w = mount(KhachHangFormPage, { attachTo: document.body })
    await flushPromises()
    expect(useToast().toasts.at(-1).message).toBe('Không tìm thấy khách hàng.')
    expect(push).toHaveBeenCalledWith('/khach-hang')
    w.unmount()
  })
})

