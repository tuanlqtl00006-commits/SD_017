import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// Form THÊM / SỬA nhân viên: lỗi hiện dưới từng ô, ngày sinh bắt buộc và từ đủ 18 tuổi, form sửa kiểm tra như form thêm.
const { svc, push, state } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), getVaiTro: vi.fn(), create: vi.fn(), update: vi.fn(), toggleActive: vi.fn() },
  push: vi.fn(),
  state: { params: {} },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: state.params }),
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/nhanVienService', () => ({ nhanVienService: svc }))
vi.mock('../src/services/diaChiHanhChinhService', () => {
  const tinh = [{ code: 1, name: 'Thành phố Hà Nội' }, { code: 33, name: 'Tỉnh Hưng Yên' }]
  const phuong = { 1: [{ code: 10, name: 'Phường Hồng Hà' }, { code: 11, name: 'Phường Hà Đông' }], 33: [{ code: 20, name: 'Phường Thái Bình' }] }
  return { API_DIA_CHI: 'x', diaChiHanhChinhService: { layTinhThanh: async () => tinh, layPhuongXa: async (m) => phuong[m] ?? [], xoaNho() {} } }
})

import NhanVienFormPage from '../src/components/nhanVien/NhanVienFormPage.vue'
import DateInput from '../src/components/common/DateInput.vue'
import { useToast } from '../src/composables/useToast'
import { HUONG_DAN_TUOI_NHAN_VIEN, LOI_CCCD, LOI_HO_TEN_KY_TU, LOI_SDT } from '../src/utils/kiemTra'
import { cacMucSelect2, datSelect2 } from './helpersSelect2'

const VAI_TRO = [{ id: 1, ten: 'Quản lý' }, { id: 2, ten: 'Nhân viên' }]
const NV_CO_SAN = [
  { id: 1, ma: 'NV01', hoTen: 'Nguyễn Hoàng Long', email: 'long@footstyle.vn', soDienThoai: '0912000001', canCuoc: '079203001234', idVaiTro: 1, vaiTro: 'Quản lý', hoatDong: true, gioiTinh: 'Nam', ngaySinh: '1990-01-01', ngayVaoLam: '2022-01-10', diaChi: 'x', diaChis: [] },
]
const NV_SUA = {
  id: 5, ma: 'NVA01', hoTen: 'Nguyễn Văn An', email: 'an@footstyle.vn', soDienThoai: '0987654321', canCuoc: '001099012345', idVaiTro: 2, vaiTro: 'Nhân viên', hoatDong: true, gioiTinh: 'Nam',
  ngaySinh: '1999-05-20', ngayVaoLam: '2024-01-01', diaChi: 'Số 3 Lý Bôn, Phường Thái Bình, Tỉnh Hưng Yên',
  diaChis: [{ id: 11, tinhThanh: 'Tỉnh Hưng Yên', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 3 Lý Bôn', macDinh: true }],
}

async function moForm(ds = NV_CO_SAN, vaiTro = VAI_TRO) {
  svc.getAll.mockResolvedValue(ds)
  svc.getVaiTro.mockResolvedValue(vaiTro)
  const w = mount(NhanVienFormPage, { attachTo: document.body })
  await flushPromises()
  return w
}
async function moFormSua(nv = NV_SUA) {
  state.params = { id: String(nv.id) }
  return moForm([...NV_CO_SAN, nv])
}

// Chỉ hiện là LỖI nếu phần tử có class ad-error (cùng id đó có lúc là dòng hướng dẫn ad-hint)
const loiO = (w, id) => {
  const p = w.find(`#${id}`)
  return p.exists() && p.classes().includes('ad-error') ? p.text() : ''
}
const goVaRoi = async (w, sel, v) => {
  const o = w.find(sel)
  await o.setValue(v)
  await o.trigger('blur')
}
const roiO = (w, sel) => w.find(sel).trigger('blur')
const submit = async (w) => {
  await w.find('form').trigger('submit')
  await flushPromises()
}
const nutTheoChu = (w, chu) => w.findAll('button').find((b) => b.text().includes(chu))

async function themDiaChi(w, tinh, phuong, chiTiet) {
  await nutTheoChu(w, 'Thêm địa chỉ').trigger('click')
  await flushPromises()
  await datSelect2(w, '#nv-dc-tinh', tinh)
  await flushPromises()
  await datSelect2(w, '#nv-dc-phuong', phuong)
  await w.find('#nv-dc-chi-tiet').setValue(chiTiet)
  await nutTheoChu(w, 'Lưu địa chỉ').trigger('click')
  await flushPromises()
}
async function dienHopLe(w, ghiDe = {}) {
  const v = { ten: 'Nguyễn Văn An', email: 'an@footstyle.vn', sdt: '0987654321', mk: 'Footstyle@123', ngaySinh: '20/05/1999', ...ghiDe }
  await w.find('#nv-ho-ten').setValue(v.ten)
  await w.find('#nv-email').setValue(v.email)
  await w.find('#nv-sdt').setValue(v.sdt)
  await w.find('#nv-mat-khau').setValue(v.mk)
  await w.find('#nv-ngay-sinh').setValue(v.ngaySinh)
  await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 15 ngõ 120 Trần Phú')
}

beforeEach(() => {
  // Cố định "hôm nay" = 15/10/2026 để các mốc tuổi 18 / 100 tính ra đúng từng ngày
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 15, 9, 0, 0))
  document.body.innerHTML = ''
  state.params = {}
  Object.values(svc).forEach((f) => f.mockReset())
  push.mockReset()
  useToast().toasts.splice(0)
})
afterEach(() => vi.useRealTimers())

describe('Form nhân viên: bố cục, ô chọn và hướng dẫn tuổi', () => {
  it('mở form trống chưa hiện lỗi nào; ngày sinh có dấu * và dòng hướng dẫn tuổi kèm căn cứ pháp luật', async () => {
    const w = await moForm()
    expect(w.findAll('.ad-error')).toHaveLength(0)
    expect(w.find('label[for=nv-ngay-sinh]').text()).toContain('*')
    const goiY = w.find('[data-testid=nv-huong-dan-tuoi]')
    expect(goiY.text()).toBe(HUONG_DAN_TUOI_NHAN_VIEN)
    expect(goiY.text()).toContain('đủ 18 tuổi')
    expect(goiY.text()).toContain('Bộ luật Lao động 2019')
    expect(goiY.text()).toContain('Điều 143')
    w.unmount()
  })

  it('không còn <select> thường; vai trò là Select2 chỉ có hai quyền Quản lý và Nhân viên', async () => {
    const w = await moForm()
    expect(w.findAll('select')).toHaveLength(0)
    expect(await cacMucSelect2(w, '#nv-vai-tro-form')).toEqual(['Quản lý', 'Nhân viên'])
    w.unmount()
  })

  it('lịch ngày sinh chỉ cho chọn người từ đủ 18 tuổi đến tối đa 100 tuổi; ngày vào làm không vượt hôm nay', async () => {
    const w = await moForm()
    const [ngaySinh, ngayVaoLam] = w.findAllComponents(DateInput)
    expect(ngaySinh.props('max')).toBe('2008-10-15')
    expect(ngaySinh.props('min')).toBe('1925-10-16')
    expect(ngayVaoLam.props('max')).toBe('2026-10-15')
    expect(ngayVaoLam.props('min')).toBeUndefined() // chưa có ngày sinh nên chưa giới hạn cận dưới
    await w.find('#nv-ngay-sinh').setValue('20/05/2000')
    expect(ngayVaoLam.props('min')).toBe('2018-05-20') // từ sinh nhật thứ 18
    w.unmount()
  })
})

describe('Form nhân viên: lỗi hiện dưới từng ô khi rời ô', () => {
  it('họ tên: để trống, quá ngắn, quá dài, có chữ số / ký tự lạ đều báo; sửa đúng thì lỗi mất ngay', async () => {
    const w = await moForm()
    const o = w.find('#nv-ho-ten')
    await roiO(w, '#nv-ho-ten')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('Nhập họ tên.')
    expect(o.classes()).toContain('is-invalid')
    expect(o.attributes('aria-invalid')).toBe('true')
    await o.setValue('A')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('Họ tên từ 2 đến 60 ký tự.')
    await o.setValue('A'.repeat(61))
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('Họ tên từ 2 đến 60 ký tự.')
    await o.setValue('An123')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe(LOI_HO_TEN_KY_TU)
    await o.setValue('An@#')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe(LOI_HO_TEN_KY_TU)
    await o.setValue('Nguyễn  Văn An')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('')
    expect(o.classes()).not.toContain('is-invalid')
    await o.setValue("Trần Thị O'Neil-Nguyễn")
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('')
    w.unmount()
  })

  it('số điện thoại: trống, sai đầu số / độ dài, trùng người khác; đúng thì hiện lại dòng hướng dẫn', async () => {
    const w = await moForm()
    await roiO(w, '#nv-sdt')
    expect(loiO(w, 'nv-sdt-msg')).toBe('Nhập số điện thoại.')
    for (const sai of ['0123456789', '0212345678', '091234567', '09123456789', '09123abcde']) {
      await w.find('#nv-sdt').setValue(sai)
      expect(loiO(w, 'nv-sdt-msg'), sai).toBe(LOI_SDT)
    }
    await w.find('#nv-sdt').setValue('0912000001') // trùng NV01
    expect(loiO(w, 'nv-sdt-msg')).toBe('Số điện thoại đã được sử dụng.')
    await w.find('#nv-sdt').setValue('0987654321')
    expect(loiO(w, 'nv-sdt-msg')).toBe('')
    expect(w.find('#nv-sdt-msg').classes()).toContain('ad-hint')
    w.unmount()
  })

  it('email: trống, sai định dạng, trùng (không phân biệt hoa thường); email đúng thì hết lỗi', async () => {
    const w = await moForm()
    await roiO(w, '#nv-email')
    expect(loiO(w, 'nv-email-msg')).toBe('Nhập email.')
    for (const sai of ['abc', 'abc@', '@footstyle.vn', 'a b@footstyle.vn', 'a@footstyle']) {
      await w.find('#nv-email').setValue(sai)
      expect(loiO(w, 'nv-email-msg'), sai).toBe('Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.')
    }
    await w.find('#nv-email').setValue('LONG@Footstyle.vn')
    expect(loiO(w, 'nv-email-msg')).toBe('Email đã được sử dụng.')
    await w.find('#nv-email').setValue('moi@footstyle.vn')
    expect(loiO(w, 'nv-email-msg')).toBe('')
    w.unmount()
  })

  it('căn cước: không bắt buộc, nhưng đã nhập thì phải đủ 12 chữ số và không trùng', async () => {
    const w = await moForm()
    await roiO(w, '#nv-cccd')
    expect(loiO(w, 'nv-cccd-msg')).toBe('') // bỏ trống là hợp lệ
    for (const sai of ['12345', '12345678901a', '1234567890123']) {
      await w.find('#nv-cccd').setValue(sai)
      expect(loiO(w, 'nv-cccd-msg'), sai).toBe(LOI_CCCD)
    }
    await w.find('#nv-cccd').setValue('079203001234')
    expect(loiO(w, 'nv-cccd-msg')).toBe('Số căn cước công dân đã được sử dụng.')
    await w.find('#nv-cccd').setValue('079203009999')
    expect(loiO(w, 'nv-cccd-msg')).toBe('')
    w.unmount()
  })

  it('mật khẩu: trống, ngắn, dài, thiếu chữ / số, có khoảng trắng; mật khẩu đủ mạnh thì hết lỗi', async () => {
    const w = await moForm()
    await roiO(w, '#nv-mat-khau')
    expect(loiO(w, 'nv-mat-khau-msg')).toBe('Nhập mật khẩu cho nhân viên mới.')
    const bang = [
      ['abc12', 'Mật khẩu từ 8 đến 50 ký tự.'],
      ['a1'.repeat(26), 'Mật khẩu từ 8 đến 50 ký tự.'],
      ['abcdefgh', 'Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.'],
      ['12345678', 'Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.'],
      ['abc 12345', 'Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.'],
    ]
    for (const [mk, loi] of bang) {
      await w.find('#nv-mat-khau').setValue(mk)
      expect(loiO(w, 'nv-mat-khau-msg'), mk).toBe(loi)
    }
    await w.find('#nv-mat-khau').setValue('Footstyle@123')
    expect(loiO(w, 'nv-mat-khau-msg')).toBe('')
    expect(w.find('#nv-mat-khau-msg').classes()).toContain('ad-hint')
    w.unmount()
  })

  it('ngày sinh BẮT BUỘC: để trống báo lỗi; chưa đủ 18 tuổi, ở tương lai, quá 100 tuổi đều báo đúng câu', async () => {
    const w = await moForm()
    await roiO(w, '#nv-ngay-sinh')
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhập ngày sinh.')
    expect(w.find('#nv-ngay-sinh').classes()).toContain('is-invalid')
    const bang = [
      ['01/01/2020', 'Nhân viên phải từ đủ 18 tuổi.'],
      ['16/10/2008', 'Nhân viên phải từ đủ 18 tuổi.'], // ngày mai mới tròn 18
      ['16/10/2026', 'Ngày sinh không được ở tương lai.'],
      ['15/10/1925', 'Ngày sinh không hợp lệ (quá 100 tuổi).'],
      ['01/01/1900', 'Ngày sinh không hợp lệ (quá 100 tuổi).'],
      ['15/10/2008', ''], // đúng hôm nay tròn 18 tuổi -> được
      ['15/10/1926', ''], // đúng 100 tuổi -> còn được
      ['20/05/1999', ''],
    ]
    for (const [ngay, loi] of bang) {
      await w.find('#nv-ngay-sinh').setValue(ngay)
      expect(loiO(w, 'nv-ngay-sinh-msg'), ngay).toBe(loi)
    }
    expect(w.find('#nv-ngay-sinh-msg').classes()).toContain('ad-hint') // hợp lệ thì quay lại dòng hướng dẫn
    w.unmount()
  })

  it('ngày vào làm: không được ở tương lai; không được sớm hơn sinh nhật thứ 18; bỏ trống thì lấy hôm nay', async () => {
    const w = await moForm()
    await w.find('#nv-ngay-sinh').setValue('20/05/2000')
    await w.find('#nv-ngay-vao-lam').setValue('16/10/2026')
    await roiO(w, '#nv-ngay-vao-lam')
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('Ngày vào làm không được ở tương lai.')
    await w.find('#nv-ngay-vao-lam').setValue('01/01/2018')
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ 18 tuổi).')
    await w.find('#nv-ngay-vao-lam').setValue('19/05/2018')
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ 18 tuổi).')
    await w.find('#nv-ngay-vao-lam').setValue('20/05/2018') // đúng ngày tròn 18 tuổi
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('')
    await w.find('#nv-ngay-vao-lam').setValue('')
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('')
    w.unmount()
  })

  it('ngày sinh sai thì không báo thêm lỗi ngày vào làm (chỉ một lỗi gốc)', async () => {
    const w = await moForm()
    await w.find('#nv-ngay-sinh').setValue('01/01/2020')
    await roiO(w, '#nv-ngay-sinh')
    await roiO(w, '#nv-ngay-vao-lam')
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhân viên phải từ đủ 18 tuổi.')
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('')
    w.unmount()
  })

  it('giới tính mặc định là Nam; đổi sang Nữ gửi đúng giá trị', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'NVT01', hoTen: 'Trần Thị Tú' })
    const w = await moForm()
    expect(w.find('input[name=gioiTinh]:checked').element.value).toBe('Nam')
    await dienHopLe(w, { ten: 'Trần Thị Tú' })
    await w.findAll('input[name=gioiTinh]')[1].setValue(true)
    await submit(w)
    expect(svc.create.mock.calls[0][0].gioiTinh).toBe('Nữ')
    w.unmount()
  })
})

describe('Form nhân viên: bấm lưu', () => {
  it('để trống rồi bấm tạo: hiện lỗi dưới TẤT CẢ các ô cùng lúc, con trỏ vào ô lỗi đầu tiên, không gọi API', async () => {
    const w = await moForm()
    await submit(w)
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('Nhập họ tên.')
    expect(loiO(w, 'nv-email-msg')).toBe('Nhập email.')
    expect(loiO(w, 'nv-mat-khau-msg')).toBe('Nhập mật khẩu cho nhân viên mới.')
    expect(loiO(w, 'nv-sdt-msg')).toBe('Nhập số điện thoại.')
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhập ngày sinh.')
    expect(loiO(w, 'nv-dia-chi-msg')).toBe('Thêm ít nhất một địa chỉ.')
    expect(document.activeElement.id).toBe('nv-ho-ten')
    expect(svc.create).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
    w.unmount()
  })

  it('chỉ thiếu ngày sinh thì vẫn không cho tạo; điền ngày sinh hợp lệ thì tạo được', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'NVA01', hoTen: 'Nguyễn Văn An' })
    const w = await moForm()
    await dienHopLe(w, { ngaySinh: '' })
    await submit(w)
    expect(svc.create).not.toHaveBeenCalled()
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhập ngày sinh.')
    expect(document.activeElement.id).toBe('nv-ngay-sinh')
    await w.find('#nv-ngay-sinh').setValue('20/05/1999')
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('') // sửa đúng là hết lỗi ngay, không phải bấm lại
    await submit(w)
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(svc.create.mock.calls[0][0].ngaySinh).toBe('1999-05-20')
    expect(push).toHaveBeenCalledWith('/nhan-vien')
    w.unmount()
  })

  it('nhân viên dưới 18 tuổi không được tạo', async () => {
    const w = await moForm()
    await dienHopLe(w, { ngaySinh: '16/10/2008' })
    await submit(w)
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhân viên phải từ đủ 18 tuổi.')
    expect(svc.create).not.toHaveBeenCalled()
    w.unmount()
  })

  it('email / số điện thoại / CCCD trùng nhân viên khác bị chặn ngay ở trình duyệt, không gọi API', async () => {
    const w = await moForm()
    await dienHopLe(w, { email: 'long@footstyle.vn', sdt: '0912000001' })
    await w.find('#nv-cccd').setValue('079203001234')
    await submit(w)
    expect(loiO(w, 'nv-email-msg')).toBe('Email đã được sử dụng.')
    expect(loiO(w, 'nv-sdt-msg')).toBe('Số điện thoại đã được sử dụng.')
    expect(loiO(w, 'nv-cccd-msg')).toBe('Số căn cước công dân đã được sử dụng.')
    expect(svc.create).not.toHaveBeenCalled()
    w.unmount()
  })

  it('lỗi theo ô từ máy chủ (409): hiện đúng dưới ô, giữ nguyên trang; sửa ô nào thì lỗi ô đó mất', async () => {
    const loi = Object.assign(new Error('Email đã được sử dụng.'), {
      status: 409,
      loiTheoTruong: { email: 'Email đã được sử dụng.', soDienThoai: 'Số điện thoại đã được sử dụng.' },
    })
    svc.create.mockRejectedValue(loi)
    const w = await moForm()
    await dienHopLe(w) // dữ liệu hợp lệ với danh sách đã tải; máy chủ báo trùng vì có người vừa thêm trước đó
    await submit(w)
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(push).not.toHaveBeenCalled()
    expect(loiO(w, 'nv-email-msg')).toBe('Email đã được sử dụng.')
    expect(loiO(w, 'nv-sdt-msg')).toBe('Số điện thoại đã được sử dụng.')
    expect(w.find('#nv-email').classes()).toContain('is-invalid')
    expect(useToast().toasts.at(-1).message).toBe('Email đã được sử dụng.')
    await w.find('#nv-email').setValue('an.moi@footstyle.vn')
    expect(loiO(w, 'nv-email-msg')).toBe('')
    expect(loiO(w, 'nv-sdt-msg')).toBe('Số điện thoại đã được sử dụng.') // ô chưa sửa vẫn giữ lỗi
    w.unmount()
  })

  it('lỗi máy chủ không có theo ô (mất mạng...) thì báo bằng thông báo chung và vẫn ở lại trang', async () => {
    svc.create.mockRejectedValue(new Error('Không kết nối được máy chủ.'))
    const w = await moForm()
    await dienHopLe(w)
    await submit(w)
    expect(useToast().toasts.at(-1).message).toBe('Không kết nối được máy chủ.')
    expect(w.findAll('.ad-error')).toHaveLength(0)
    expect(push).not.toHaveBeenCalled()
    w.unmount()
  })

  it('không có vai trò nào để chọn thì báo "Chọn vai trò." dưới ô vai trò', async () => {
    const w = await moForm(NV_CO_SAN, [])
    await dienHopLe(w)
    await submit(w)
    expect(loiO(w, 'nv-vai-tro-msg')).toBe('Chọn vai trò.')
    expect(svc.create).not.toHaveBeenCalled()
    w.unmount()
  })

  it('bấm Tạo hai lần liên tiếp chỉ gửi một lần', async () => {
    let xong
    svc.create.mockReturnValue(new Promise((r) => (xong = r)))
    const w = await moForm()
    await dienHopLe(w)
    w.find('form').trigger('submit')
    w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    xong({ id: 9, ma: 'NVA01', hoTen: 'Nguyễn Văn An' })
    await flushPromises()
    w.unmount()
  })
})

describe('Form nhân viên: ô địa chỉ báo lỗi dưới từng ô', () => {
  it('lưu địa chỉ khi chưa chọn gì: lỗi nằm dưới Tỉnh, Phường và Địa chỉ cụ thể; chọn / gõ đúng thì từng lỗi mất', async () => {
    const w = await moForm()
    await nutTheoChu(w, 'Thêm địa chỉ').trigger('click')
    await flushPromises()
    expect(w.find('.nv-dc-form').findAll('.ad-error')).toHaveLength(0) // mới mở: chưa báo lỗi
    await nutTheoChu(w, 'Lưu địa chỉ').trigger('click')
    expect(loiO(w, 'nv-dc-tinh-msg')).toBe('Chọn Tỉnh/Thành phố.')
    expect(loiO(w, 'nv-dc-phuong-msg')).toBe('Chọn Phường/Xã.')
    expect(loiO(w, 'nv-dc-chi-tiet-msg')).toBe('Nhập số nhà, tên đường...')
    await datSelect2(w, '#nv-dc-tinh', 'Thành phố Hà Nội')
    await flushPromises()
    expect(loiO(w, 'nv-dc-tinh-msg')).toBe('')
    expect(loiO(w, 'nv-dc-phuong-msg')).toBe('Chọn Phường/Xã.')
    await datSelect2(w, '#nv-dc-phuong', 'Phường Hà Đông')
    expect(loiO(w, 'nv-dc-phuong-msg')).toBe('')
    await w.find('#nv-dc-chi-tiet').setValue('   ')
    expect(loiO(w, 'nv-dc-chi-tiet-msg')).toBe('Nhập số nhà, tên đường...')
    await w.find('#nv-dc-chi-tiet').setValue('Số 15 ngõ 120 Trần Phú')
    expect(loiO(w, 'nv-dc-chi-tiet-msg')).toBe('')
    await nutTheoChu(w, 'Lưu địa chỉ').trigger('click')
    expect(w.findAll('.nv-dc')).toHaveLength(1)
    w.unmount()
  })

  it('địa chỉ cụ thể dài quá 255 ký tự thì báo lỗi', async () => {
    const w = await moForm()
    await nutTheoChu(w, 'Thêm địa chỉ').trigger('click')
    await flushPromises()
    await datSelect2(w, '#nv-dc-tinh', 'Thành phố Hà Nội')
    await flushPromises()
    await datSelect2(w, '#nv-dc-phuong', 'Phường Hà Đông')
    await w.find('#nv-dc-chi-tiet').setValue('x'.repeat(256))
    await nutTheoChu(w, 'Lưu địa chỉ').trigger('click')
    expect(loiO(w, 'nv-dc-chi-tiet-msg')).toBe('Địa chỉ cụ thể tối đa 255 ký tự.')
    expect(w.findAll('.nv-dc')).toHaveLength(0)
    w.unmount()
  })
})

describe('Form SỬA nhân viên: kiểm tra giống hệt form thêm', () => {
  it('không có ô mật khẩu; email và ngày vào làm bị khóa; ngày sinh vẫn có dấu * và hướng dẫn tuổi', async () => {
    const w = await moFormSua()
    expect(w.find('#nv-mat-khau').exists()).toBe(false)
    expect(w.find('#nv-email').attributes('readonly')).toBeDefined()
    expect(w.find('#nv-ngay-vao-lam').attributes('readonly')).toBeDefined()
    expect(w.find('label[for=nv-ngay-sinh]').text()).toContain('*')
    expect(w.find('[data-testid=nv-huong-dan-tuoi]').text()).toContain('Điều 143')
    expect(w.findAll('.ad-error')).toHaveLength(0)
    w.unmount()
  })

  it('họ tên, số điện thoại, căn cước, ngày sinh bị sửa sai thì báo dưới từng ô như khi thêm', async () => {
    const w = await moFormSua()
    await goVaRoi(w, '#nv-ho-ten', 'An 99')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe(LOI_HO_TEN_KY_TU)
    await goVaRoi(w, '#nv-ho-ten', '')
    expect(loiO(w, 'nv-ho-ten-msg')).toBe('Nhập họ tên.')
    await goVaRoi(w, '#nv-sdt', '0112345678')
    expect(loiO(w, 'nv-sdt-msg')).toBe(LOI_SDT)
    await goVaRoi(w, '#nv-sdt', '0912000001') // của NV01
    expect(loiO(w, 'nv-sdt-msg')).toBe('Số điện thoại đã được sử dụng.')
    await goVaRoi(w, '#nv-cccd', '079203001234') // của NV01
    expect(loiO(w, 'nv-cccd-msg')).toBe('Số căn cước công dân đã được sử dụng.')
    await goVaRoi(w, '#nv-cccd', '123')
    expect(loiO(w, 'nv-cccd-msg')).toBe(LOI_CCCD)
    await goVaRoi(w, '#nv-ngay-sinh', '')
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhập ngày sinh.')
    await goVaRoi(w, '#nv-ngay-sinh', '01/01/2015')
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhân viên phải từ đủ 18 tuổi.')
    w.unmount()
  })

  it('giữ nguyên số điện thoại / căn cước / email của chính mình thì KHÔNG bị báo trùng', async () => {
    svc.update.mockResolvedValue({})
    const w = await moFormSua()
    await goVaRoi(w, '#nv-sdt', '0987654321')
    await goVaRoi(w, '#nv-cccd', '001099012345')
    expect(loiO(w, 'nv-sdt-msg')).toBe('')
    expect(loiO(w, 'nv-cccd-msg')).toBe('')
    await submit(w)
    expect(svc.update).toHaveBeenCalledTimes(1)
    w.unmount()
  })

  it('có lỗi thì bấm Lưu thay đổi không gọi API và đưa con trỏ tới ô lỗi; sửa đúng rồi lưu được', async () => {
    svc.update.mockResolvedValue({})
    const w = await moFormSua()
    await w.find('#nv-sdt').setValue('123')
    await submit(w)
    expect(svc.update).not.toHaveBeenCalled()
    expect(loiO(w, 'nv-sdt-msg')).toBe(LOI_SDT)
    expect(document.activeElement.id).toBe('nv-sdt')
    await w.find('#nv-sdt').setValue('0933444555')
    await submit(w)
    expect(svc.update).toHaveBeenCalledTimes(1)
    const [id, p] = svc.update.mock.calls[0]
    expect(id).toBe(5)
    expect(p.soDienThoai).toBe('0933444555')
    expect(p.ngaySinh).toBe('1999-05-20')
    for (const k of ['email', 'matKhau', 'ngayVaoLam']) expect(p).not.toHaveProperty(k)
    expect(push).toHaveBeenCalledWith('/nhan-vien')
    w.unmount()
  })

  it('ngày vào làm đã khóa: sửa ngày sinh làm nhân viên chưa đủ 18 tuổi lúc vào làm thì lỗi nằm ở ô ngày sinh', async () => {
    const w = await moFormSua() // vào làm 01/01/2024
    await goVaRoi(w, '#nv-ngay-sinh', '01/06/2007') // đủ 18 tuổi từ 01/06/2025, sau ngày vào làm
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Ngày sinh không hợp lệ: nhân viên phải đủ 18 tuổi vào ngày vào làm (01/01/2024).')
    expect(loiO(w, 'nv-ngay-vao-lam-msg')).toBe('')
    await goVaRoi(w, '#nv-ngay-sinh', '01/01/2006') // vừa tròn 18 tuổi đúng ngày vào làm
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('')
    w.unmount()
  })

  it('nhân viên cũ chưa có ngày sinh: phải bổ sung ngày sinh mới lưu được', async () => {
    svc.update.mockResolvedValue({})
    const w = await moFormSua({ ...NV_SUA, ngaySinh: null })
    expect(w.find('#nv-ngay-sinh').element.value).toBe('')
    await submit(w)
    expect(svc.update).not.toHaveBeenCalled()
    expect(loiO(w, 'nv-ngay-sinh-msg')).toBe('Nhập ngày sinh.')
    await w.find('#nv-ngay-sinh').setValue('20/05/1999')
    await submit(w)
    expect(svc.update).toHaveBeenCalledTimes(1)
    w.unmount()
  })

  it('lỗi theo ô từ máy chủ khi sửa cũng hiện dưới đúng ô', async () => {
    svc.update.mockRejectedValue(Object.assign(new Error('Số điện thoại đã được sử dụng.'), { status: 409, loiTheoTruong: { soDienThoai: 'Số điện thoại đã được sử dụng.' } }))
    const w = await moFormSua()
    await w.find('#nv-sdt').setValue('0933444555')
    await submit(w)
    expect(loiO(w, 'nv-sdt-msg')).toBe('Số điện thoại đã được sử dụng.')
    expect(push).not.toHaveBeenCalled()
    w.unmount()
  })
})
