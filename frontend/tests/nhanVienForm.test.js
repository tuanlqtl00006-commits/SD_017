import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { svc, push, state } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), getVaiTro: vi.fn(), getViTri: vi.fn(), themViTri: vi.fn(), create: vi.fn(), update: vi.fn(), toggleActive: vi.fn() },
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
  return {
    API_DIA_CHI: 'x',
    diaChiHanhChinhService: {
      layTinhThanh: async () => tinh,
      layPhuongXa: async (ma) => phuong[ma] ?? [],
      xoaNho() {},
    },
  }
})

import NhanVienFormPage from '../src/components/nhanVien/NhanVienFormPage.vue'
import { useToast } from '../src/composables/useToast'

const QR = '001099012345|123456789|NGUYỄN VĂN AN|20051999|Nam|Thôn Cầu, Xã Đông Quý, Huyện Tiền Hải, Tỉnh Thái Bình|15032021'
const VAI_TRO = [{ id: 1, ten: 'Quản lý' }, { id: 2, ten: 'Nhân viên' }]
const VI_TRI = [{ id: 1, ten: 'Bán hàng tại quầy' }, { id: 2, ten: 'Thu ngân' }, { id: 3, ten: 'Kho' }]
const NV_CO_SAN = [
  { id: 1, ma: 'NV0001', hoTen: 'Nguyễn Hoàng Long', email: 'long@footstyle.vn', soDienThoai: '0912000001', cccd: '079203001234', idVaiTro: 1, vaiTro: 'Quản lý', hoatDong: true, gioiTinh: 'Nam', ngayVaoLam: '2022-01-10', diaChi: 'x', diaChis: [] },
]

const set = async (w, sel, v) => {
  await w.find(sel).setValue(v)
}
const dom = (sel) => document.body.querySelector(sel)

async function moForm(ds = NV_CO_SAN) {
  svc.getAll.mockResolvedValue(ds)
  svc.getVaiTro.mockResolvedValue(VAI_TRO)
  svc.getViTri.mockResolvedValue(VI_TRI.map((v) => ({ ...v })))
  const w = mount(NhanVienFormPage, { attachTo: document.body })
  await flushPromises()
  return w
}

async function themDiaChi(w, tinh, phuong, chiTiet) {
  const nut = w.findAll('button').find((b) => b.text().includes('Thêm địa chỉ'))
  await nut.trigger('click')
  await flushPromises()
  await set(w, '#nv-dc-tinh', tinh)
  await flushPromises()
  await set(w, '#nv-dc-phuong', phuong)
  await set(w, '#nv-dc-chi-tiet', chiTiet)
  await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
  await flushPromises()
}

beforeEach(() => {
  document.body.innerHTML = ''
  state.params = {}
  Object.values(svc).forEach((f) => f.mockReset())
  push.mockReset()
  useToast().toasts.splice(0)
})

describe('Thêm nhân viên', () => {
  it('mã nhân viên tạo theo họ tên đầy đủ, hiện ngay khi nhập tên', async () => {
    const w = await moForm()
    expect(w.find('#nv-ma').element.value).toBe('')
    expect(w.find('#nv-ma').attributes('placeholder')).toMatch(/Tự tạo sau khi nhập họ tên/)
    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    expect(w.find('#nv-ma').element.value).toBe('AnNV01')
    await set(w, '#nv-ho-ten', 'Trần Thị Mai Lan')
    expect(w.find('#nv-ma').element.value).toBe('LanTTM01')
    w.unmount()
  })

  it('mã không trùng với nhân viên đã có cùng tiền tố', async () => {
    const w = await moForm([...NV_CO_SAN, { ...NV_CO_SAN[0], id: 2, ma: 'AnNV01', email: 'a@x.vn', soDienThoai: '0911111111', cccd: null }])
    await set(w, '#nv-ho-ten', 'Ngô Văn An')
    expect(w.find('#nv-ma').element.value).toBe('AnNV02')
    w.unmount()
  })

  it('không có nút xóa địa chỉ; thêm nhiều địa chỉ và chọn địa chỉ chính', async () => {
    const w = await moForm()
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 15 ngõ 120 Trần Phú')
    await themDiaChi(w, 'Tỉnh Hưng Yên', 'Phường Thái Bình', 'Số 3 Lý Bôn')
    const the = w.findAll('.nv-dc')
    expect(the).toHaveLength(2)
    expect(the[0].text()).toContain('Số 15 ngõ 120 Trần Phú, Phường Hà Đông, Thành phố Hà Nội')
    expect(the[0].text()).toContain('Địa chỉ chính') // địa chỉ đầu tiên tự là địa chỉ chính
    expect(the[1].text()).not.toContain('Địa chỉ chính')
    // không có bất kỳ nút xóa nào trong thẻ địa chỉ
    const card = w.find('#nv-dia-chi-card')
    expect(card.findAll('button').some((b) => /xóa|xoá/i.test(b.text() + (b.attributes('title') ?? '') + (b.attributes('aria-label') ?? '')))).toBe(false)
    expect(card.find('.bi-trash').exists()).toBe(false)
    // chọn địa chỉ đã thêm làm địa chỉ chính
    await the[1].find('input[type=radio]').setValue(true)
    const sau = w.findAll('.nv-dc')
    expect(sau[1].text()).toContain('Địa chỉ chính')
    expect(sau[0].text()).not.toContain('Địa chỉ chính')
    w.unmount()
  })

  it('đổi tỉnh thì phường/xã bị xóa và phải chọn lại; chỉ liệt kê phường của tỉnh đó (không có quận/huyện)', async () => {
    const w = await moForm()
    await w.findAll('button').find((b) => b.text().includes('Thêm địa chỉ')).trigger('click')
    await flushPromises()
    expect(w.text()).not.toContain('Quận/Huyện')
    await set(w, '#nv-dc-tinh', 'Thành phố Hà Nội')
    await flushPromises()
    expect(w.findAll('#nv-dc-phuong option').map((o) => o.text())).toEqual(['Chọn Phường/Xã', 'Phường Hồng Hà', 'Phường Hà Đông'])
    await set(w, '#nv-dc-phuong', 'Phường Hà Đông')
    await set(w, '#nv-dc-tinh', 'Tỉnh Hưng Yên')
    await flushPromises()
    expect(w.find('#nv-dc-phuong').element.value).toBe('')
    expect(w.findAll('#nv-dc-phuong option').map((o) => o.text())).toEqual(['Chọn Phường/Xã', 'Phường Thái Bình'])
    w.unmount()
  })

  it('địa chỉ thiếu tỉnh / phường / địa chỉ cụ thể thì báo lỗi, không thêm vào danh sách', async () => {
    const w = await moForm()
    await w.findAll('button').find((b) => b.text().includes('Thêm địa chỉ')).trigger('click')
    await flushPromises()
    await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
    expect(w.find('.nv-dc-form').text()).toContain('Chọn Tỉnh/Thành phố.')
    expect(w.find('.nv-dc-form').text()).toContain('Nhập số nhà, tên đường')
    expect(w.findAll('.nv-dc')).toHaveLength(0)
    w.unmount()
  })

  it('chưa có địa chỉ thì không cho tạo; số CCCD sai/trùng bị báo lỗi', async () => {
    const w = await moForm()
    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    await set(w, '#nv-cccd', '12345')
    await set(w, '#nv-email', 'an@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Số căn cước công dân gồm đúng 12 chữ số.')
    expect(w.text()).toContain('Thêm ít nhất một địa chỉ.')
    await set(w, '#nv-cccd', '079203001234') // trùng với NV0001
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Số căn cước công dân đã được sử dụng.')
    expect(svc.create).not.toHaveBeenCalled()
    w.unmount()
  })

  it('đang mở ô nhập địa chỉ mà bấm tạo thì nhắc lưu/hủy trước', async () => {
    const w = await moForm()
    await w.findAll('button').find((b) => b.text().includes('Thêm địa chỉ')).trigger('click')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Bấm “Lưu địa chỉ” hoặc “Hủy”')
    w.unmount()
  })

  it('tạo thành công: gửi CCCD + nhiều địa chỉ (đúng 1 chính), không gửi mã, rồi về danh sách', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'AnNV01', hoTen: 'Nguyễn Văn An' })
    const w = await moForm()
    await set(w, '#nv-ho-ten', 'Nguyễn  Văn An ')
    await set(w, '#nv-cccd', '001099012345')
    await set(w, '#nv-email', 'An@Footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await set(w, '#nv-ngay-sinh', '1999-05-20')
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 15 ngõ 120 Trần Phú')
    await themDiaChi(w, 'Tỉnh Hưng Yên', 'Phường Thái Bình', 'Số 3 Lý Bôn')
    await w.findAll('.nv-dc')[1].find('input[type=radio]').setValue(true) // chọn địa chỉ thứ 2 làm chính
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    const p = svc.create.mock.calls[0][0]
    expect(p.hoTen).toBe('Nguyễn Văn An')
    expect(p.cccd).toBe('001099012345')
    expect(p.email).toBe('an@footstyle.vn')
    expect(p).not.toHaveProperty('ma')
    expect(p).not.toHaveProperty('diaChi')
    expect(p.diaChis).toEqual([
      { id: null, tinhThanh: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', diaChiCuThe: 'Số 15 ngõ 120 Trần Phú', macDinh: false },
      { id: null, tinhThanh: 'Tỉnh Hưng Yên', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 3 Lý Bôn', macDinh: true },
    ])
    expect(push).toHaveBeenCalledWith('/nhan-vien')
    expect(useToast().toasts.at(-1).message).toContain('AnNV01') // báo mã chính thức do hệ thống cấp
    w.unmount()
  })

  it('lỗi từ backend thì ở lại trang và báo lỗi', async () => {
    svc.create.mockRejectedValue(new Error('Email đã được sử dụng.'))
    const w = await moForm()
    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    await set(w, '#nv-email', 'an@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 1')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(push).not.toHaveBeenCalled()
    expect(useToast().toasts.at(-1).message).toBe('Email đã được sử dụng.')
    w.unmount()
  })
})

describe('Vị trí làm việc', () => {
  const chip = (w, ten) => w.findAll('.nv-chip').find((c) => c.text().includes(ten))

  it('hiện các vị trí đã thêm, chọn nhiều vị trí và gửi idViTri khi tạo', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'AnNV01', hoTen: 'Nguyễn Văn An' })
    const w = await moForm()
    expect(w.findAll('.nv-chip').map((c) => c.text())).toEqual(['Bán hàng tại quầy', 'Thu ngân', 'Kho'])
    await chip(w, 'Thu ngân').trigger('click')
    await chip(w, 'Kho').trigger('click')
    expect(chip(w, 'Thu ngân').classes()).toContain('active')
    expect(w.text()).toContain('Đã chọn 2 vị trí')
    await chip(w, 'Kho').trigger('click') // bấm lại để bỏ chọn
    expect(w.text()).toContain('Đã chọn 1 vị trí')
    await chip(w, 'Kho').trigger('click')

    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    await set(w, '#nv-email', 'an@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 1')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(svc.create.mock.calls[0][0].idViTri).toEqual([2, 3])
    w.unmount()
  })

  it('thêm vị trí mới: gọi API rồi tự chọn; không có nút xóa vị trí', async () => {
    svc.themViTri.mockResolvedValue({ id: 4, ten: 'Chăm sóc khách hàng' })
    const w = await moForm()
    await set(w, '#nv-vi-tri-moi', 'Chăm sóc khách hàng')
    await w.findAll('button').find((b) => b.text().includes('Thêm') && b.classes().includes('flex-shrink-0')).trigger('click')
    await flushPromises()
    expect(svc.themViTri).toHaveBeenCalledWith('Chăm sóc khách hàng')
    expect(chip(w, 'Chăm sóc khách hàng').classes()).toContain('active')
    expect(w.find('.bi-trash').exists()).toBe(false)
    w.unmount()
  })

  it('gõ lại tên vị trí đã có thì chỉ chọn lại, không gọi API thêm', async () => {
    const w = await moForm()
    await set(w, '#nv-vi-tri-moi', 'kho')
    await w.findAll('button').find((b) => b.text().includes('Thêm') && b.classes().includes('flex-shrink-0')).trigger('click')
    await flushPromises()
    expect(svc.themViTri).not.toHaveBeenCalled()
    expect(chip(w, 'Kho').classes()).toContain('active')
    w.unmount()
  })

  it('sửa nhân viên: các vị trí đang có được chọn sẵn', async () => {
    state.params = { id: '1' }
    const w = await moForm([{ ...NV_CO_SAN[0], viTri: [{ id: 1, ten: 'Bán hàng tại quầy' }, { id: 3, ten: 'Kho' }] }])
    expect(chip(w, 'Bán hàng tại quầy').classes()).toContain('active')
    expect(chip(w, 'Thu ngân').classes()).not.toContain('active')
    expect(chip(w, 'Kho').classes()).toContain('active')
    w.unmount()
  })
})

describe('Quét căn cước công dân', () => {
  async function quet(w, noiDung) {
    await w.findAll('button').find((b) => b.text().includes('Quét căn cước')).trigger('click')
    await flushPromises()
    expect(dom('.modal')).toBeTruthy()
    // mở camera thất bại trong jsdom (không có thiết bị) -> chuyển sang "Máy quét"
    const tab = [...document.body.querySelectorAll('.ad-segment-item')].find((l) => l.textContent.includes('Máy quét'))
    tab.querySelector('input').click()
    await flushPromises()
    const o = dom('#cccd-dan')
    o.value = noiDung
    o.dispatchEvent(new Event('input'))
    o.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))
    await flushPromises()
  }

  it('quét QR hợp lệ -> điền họ tên, số CCCD, giới tính, ngày sinh, mã dự kiến và mở ô nhập địa chỉ kèm gợi ý', async () => {
    const w = await moForm()
    await quet(w, QR)
    expect(dom('.modal')).toBeNull() // modal tự đóng
    expect(w.find('#nv-ho-ten').element.value).toBe('Nguyễn Văn An')
    expect(w.find('#nv-cccd').element.value).toBe('001099012345')
    expect(w.find('#nv-ngay-sinh').element.value).toBe('1999-05-20')
    expect(w.find('#nv-ma').element.value).toBe('AnNV01')
    expect(w.find('.nv-dc-form').exists()).toBe(true)
    expect(w.find('.nv-dc-form').text()).toContain('Địa chỉ trên căn cước')
    await w.find('.nv-dc-form .btn-link').trigger('click')
    expect(w.find('#nv-dc-chi-tiet').element.value).toContain('Tiền Hải')
    w.unmount()
  })

  it('mã QR không phải căn cước -> báo lỗi, không điền gì, modal vẫn mở', async () => {
    const w = await moForm()
    await quet(w, 'https://example.com')
    expect(dom('.modal')).toBeTruthy()
    expect(dom('.modal').textContent).toContain('không phải mã QR trên căn cước')
    expect(w.find('#nv-ho-ten').element.value).toBe('')
    w.unmount()
  })

  it('quét ra số CCCD đã thuộc nhân viên khác -> báo trùng', async () => {
    const w = await moForm([{ ...NV_CO_SAN[0], cccd: '001099012345' }])
    await quet(w, QR)
    expect(useToast().toasts.at(-1).message).toContain('đã thuộc nhân viên Nguyễn Hoàng Long')
    w.unmount()
  })
})

describe('Sửa nhân viên', () => {
  const NV = {
    id: 5, ma: 'AnNV01', hoTen: 'Nguyễn Văn An', email: 'an@footstyle.vn', soDienThoai: '0987654321', cccd: '001099012345', idVaiTro: 2, vaiTro: 'Nhân viên', hoatDong: true, gioiTinh: 'Nam',
    ngaySinh: '1999-05-20', ngayVaoLam: '2024-01-01', diaChi: 'Số 3 Lý Bôn, Phường Thái Bình, Tỉnh Hưng Yên',
    diaChis: [
      { id: 11, tinhThanh: 'Tỉnh Hưng Yên', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 3 Lý Bôn', macDinh: true },
      { id: 12, tinhThanh: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', diaChiCuThe: 'Số 15 Trần Phú', macDinh: false },
    ],
  }

  it('hiện đủ địa chỉ, không có nút quét căn cước, mã/email khóa; đổi địa chỉ chính và thêm địa chỉ mới rồi lưu', async () => {
    state.params = { id: '5' }
    svc.update.mockResolvedValue({})
    const w = await moForm([...NV_CO_SAN, NV])
    expect(w.find('#nv-ho-ten').element.value).toBe('Nguyễn Văn An')
    expect(w.find('#nv-ma').element.value).toBe('AnNV01')
    expect(w.find('#nv-cccd').element.value).toBe('001099012345')
    expect(w.findAll('button').some((b) => b.text().includes('Quét căn cước'))).toBe(false)
    expect(w.findAll('.nv-dc')).toHaveLength(2)
    expect(w.findAll('.nv-dc')[0].text()).toContain('Địa chỉ chính')

    await w.findAll('.nv-dc')[1].find('input[type=radio]').setValue(true)
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hồng Hà', 'Số 9 Bạch Đằng')
    expect(w.findAll('.nv-dc')).toHaveLength(3)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.update).toHaveBeenCalledTimes(1)
    const [id, p] = svc.update.mock.calls[0]
    expect(id).toBe(5)
    expect(p).not.toHaveProperty('email')
    expect(p).not.toHaveProperty('matKhau')
    expect(p.diaChis.map((d) => [d.id, d.macDinh])).toEqual([[11, false], [12, true], [null, false]])
    w.unmount()
  })

  it('nhân viên cũ chỉ có chuỗi địa chỉ: vẫn hiện và lưu lại được', async () => {
    state.params = { id: '5' }
    svc.update.mockResolvedValue({})
    const w = await moForm([{ ...NV, diaChis: [] , diaChi: 'Số 8 đường Lê Văn Lương, quận Thanh Xuân, Hà Nội' }])
    expect(w.findAll('.nv-dc')).toHaveLength(1)
    expect(w.find('.nv-dc').text()).toContain('Số 8 đường Lê Văn Lương, quận Thanh Xuân, Hà Nội')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.update).toHaveBeenCalled()
    expect(svc.update.mock.calls[0][1].diaChis).toEqual([{ id: null, tinhThanh: null, phuongXa: null, diaChiCuThe: 'Số 8 đường Lê Văn Lương, quận Thanh Xuân, Hà Nội', macDinh: true }])
    w.unmount()
  })
})
