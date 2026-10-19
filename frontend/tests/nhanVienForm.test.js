import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

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
import { cacMucSelect2, datSelect2, giaTriSelect2 } from './helpersSelect2'

const QR = '001099012345|123456789|NGUYỄN VĂN AN|20051999|Nam|Thôn Cầu, Xã Đông Quý, Huyện Tiền Hải, Tỉnh Thái Bình|15032021'
const VAI_TRO = [{ id: 1, ten: 'Quản lý' }, { id: 2, ten: 'Nhân viên' }]
const NV_CO_SAN = [
  { id: 1, ma: 'NV01', hoTen: 'Nguyễn Hoàng Long', email: 'long@footstyle.vn', soDienThoai: '0912000001', canCuoc: '079203001234', idVaiTro: 1, vaiTro: 'Quản lý', hoatDong: true, gioiTinh: 'Nam', ngayVaoLam: '2022-01-10', diaChi: 'x', diaChis: [] },
]

// Ô nhập thường thì setValue; ô chọn Select2 (có data-gia-tri) thì chọn mục có value tương ứng
const set = async (w, sel, v) => {
  const el = w.find(sel)
  if (el.attributes('data-gia-tri') !== undefined) {
    await datSelect2(w, sel, v)
    return
  }
  await el.setValue(v)
}
const dom = (sel) => document.body.querySelector(sel)

async function moForm(ds = NV_CO_SAN) {
  svc.getAll.mockResolvedValue(ds)
  svc.getVaiTro.mockResolvedValue(VAI_TRO)
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
  it('mã nhân viên = chữ cái đầu của họ tên + số thứ tự, hiện ngay khi nhập tên', async () => {
    const w = await moForm()
    expect(w.find('#nv-ma').element.value).toBe('')
    await set(w, '#nv-ho-ten', 'Lê Quốc Tuấn')
    expect(w.find('#nv-ma').element.value).toBe('LQT01')
    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    expect(w.find('#nv-ma').element.value).toBe('NVA01')
    await set(w, '#nv-ho-ten', 'Trần Thị Mai Lan')
    expect(w.find('#nv-ma').element.value).toBe('TTML01')
    w.unmount()
  })

  it('thẻ hồ sơ bên trái hiển thị đầy đủ thông tin xem trước khi nhập', async () => {
    const w = await moForm()
    expect(w.find('[data-testid=nv-preview-ma]').text()).toBe('—')
    expect(w.find('[data-testid=nv-preview-ten]').text()).toBe('—')

    await set(w, '#nv-ho-ten', 'Lê Quốc Tuấn')
    expect(w.find('[data-testid=nv-preview-ten]').text()).toBe('Lê Quốc Tuấn')
    expect(w.find('[data-testid=nv-preview-ma]').text()).toBe('LQT01')

    await set(w, '#nv-email', 'tuan.lq@footstyle.vn')
    expect(w.find('[data-testid=nv-preview-email]').text()).toBe('tuan.lq@footstyle.vn')

    await set(w, '#nv-sdt', '0912345678')
    expect(w.find('[data-testid=nv-preview-sdt]').text()).toBe('0912345678')

    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 15 ngõ 120 Trần Phú')
    expect(w.find('[data-testid=nv-preview-dia-chi]').text()).toContain('Số 15 ngõ 120 Trần Phú')
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
    expect(await cacMucSelect2(w, '#nv-dc-phuong')).toEqual(['Phường Hồng Hà', 'Phường Hà Đông'])
    await set(w, '#nv-dc-phuong', 'Phường Hà Đông')
    await set(w, '#nv-dc-tinh', 'Tỉnh Hưng Yên')
    await flushPromises()
    expect(giaTriSelect2(w, '#nv-dc-phuong')).toBe('')
    expect(await cacMucSelect2(w, '#nv-dc-phuong')).toEqual(['Phường Thái Bình'])
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

  it('chưa có địa chỉ thì không cho tạo', async () => {
    const w = await moForm()
    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    await set(w, '#nv-email', 'an@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Thêm ít nhất một địa chỉ.')
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

  it('tạo thành công: gửi nhiều địa chỉ (đúng 1 chính), không gửi mã, rồi về danh sách', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'NVA01', hoTen: 'Nguyễn Văn An' })
    const w = await moForm()
    await set(w, '#nv-ho-ten', 'Nguyễn  Văn An ')
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
    expect(p.canCuoc).toBeNull()
    expect(p.email).toBe('an@footstyle.vn')
    expect(p.ngaySinh).toBe('1999-05-20') // ô hiện 20/05/1999 nhưng gửi đi dạng yyyy-mm-dd
    expect(p).not.toHaveProperty('ma')
    expect(p).not.toHaveProperty('diaChi')
    expect(p.diaChis).toEqual([
      { id: null, tinhThanh: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', diaChiCuThe: 'Số 15 ngõ 120 Trần Phú', macDinh: false },
      { id: null, tinhThanh: 'Tỉnh Hưng Yên', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 3 Lý Bôn', macDinh: true },
    ])
    expect(push).toHaveBeenCalledWith('/nhan-vien')
    expect(useToast().toasts.at(-1).message).toContain('NVA01') // báo mã chính thức do hệ thống cấp
    w.unmount()
  })

  it('lỗi từ backend thì ở lại trang và báo lỗi', async () => {
    svc.create.mockRejectedValue(new Error('Email đã được sử dụng.'))
    const w = await moForm()
    await set(w, '#nv-ho-ten', 'Nguyễn Văn An')
    await set(w, '#nv-email', 'an@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await set(w, '#nv-ngay-sinh', '1999-05-20')
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 1')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(push).not.toHaveBeenCalled()
    expect(useToast().toasts.at(-1).message).toBe('Email đã được sử dụng.')
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

  it('quét QR hợp lệ -> điền họ tên, số CCCD, giới tính, ngày sinh, mã dự kiến và TỰ THÊM địa chỉ trên thẻ', async () => {
    const w = await moForm()
    await quet(w, QR)
    expect(dom('.modal')).toBeNull() // modal tự đóng
    expect(w.find('#nv-ho-ten').element.value).toBe('Nguyễn Văn An')
    expect(w.find('[data-testid=cccd-da-quet]').text()).toContain('001099012345')
    expect(w.find('#nv-ngay-sinh').element.value).toBe('20/05/1999') // ô ngày hiện dd/mm/yyyy, giá trị gửi đi vẫn là 1999-05-20
    expect(w.find('#nv-ma').element.value).toBe('NVA01')
    // địa chỉ trên thẻ đã nằm sẵn trong danh sách địa chỉ (không cần bấm nút gợi ý)
    expect(w.findAll('.nv-dc')).toHaveLength(1)
    expect(w.find('.nv-dc-form').exists()).toBe(false)
    w.unmount()
  })

  it('thẻ có ngày sinh chưa đủ 18 tuổi / số CCCD trùng: lỗi hiện ngay dưới ô hoặc thông báo', async () => {
    const w = await moForm()
    await quet(w, '079203001234|123456789|TRẦN MINH KHOA|20052015|Nam|Số 1, Phường Hà Đông, Thành phố Hà Nội|15032021')
    expect(w.find('#nv-ngay-sinh').element.value).toBe('20/05/2015')
    await w.find('form').trigger('submit')
    await flushPromises()
    const loi = (id) => (w.find(`#${id}`).classes().includes('ad-error') ? w.find(`#${id}`).text() : '')
    expect(loi('nv-ngay-sinh-msg')).toBe('Nhân viên phải từ đủ 18 tuổi.')
    expect(useToast().toasts.map((x) => x.message).join(' | ')).toContain('Số CCCD này đã thuộc nhân viên')
    expect(w.find('#nv-ho-ten').classes()).not.toContain('is-invalid')
    w.unmount()
  })

  it('địa chỉ thẻ ghi tỉnh đã sáp nhập (Thái Bình) -> tự đổi sang tỉnh mới (Hưng Yên); phường cũ giữ lại để chọn lại, quận/huyện cũ nằm ở địa chỉ cụ thể', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'NVA01', hoTen: 'Nguyễn Văn An' })
    const w = await moForm()
    await quet(w, QR)
    const dc = w.find('.nv-dc').text()
    expect(dc).toContain('Tỉnh Hưng Yên')
    expect(dc).not.toContain('Thái Bình')
    expect(dc).toContain('Xã Đông Quý')
    expect(dc).toContain('Thôn Cầu, Huyện Tiền Hải')
    expect(w.find('.nv-dc input[type=radio]').element.checked).toBe(true) // địa chỉ đầu tiên là địa chỉ chính
    expect(useToast().toasts.at(-1).message).toContain('tỉnh đã sáp nhập')
    // điền nốt các ô còn thiếu rồi lưu: địa chỉ được gửi lên đúng
    await set(w, '#nv-email', 'an@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(svc.create.mock.calls[0][0].diaChis).toEqual([
      { id: null, tinhThanh: 'Tỉnh Hưng Yên', phuongXa: 'Xã Đông Quý', diaChiCuThe: 'Thôn Cầu, Huyện Tiền Hải', macDinh: true },
    ])
    w.unmount()
  })

  it('địa chỉ thẻ khớp đơn vị mới (Hà Nội / Phường Hà Đông) -> dùng đúng tên mới, bỏ Quận/Huyện cũ', async () => {
    const w = await moForm()
    await quet(w, QR.replace(/\|[^|]*\|15032021$/, '|Số 15 ngõ 120 Trần Phú, Phường Hà Đông, Quận Hà Đông, Thành phố Hà Nội|15032021'))
    const dc = w.find('.nv-dc').text()
    expect(dc).toContain('Thành phố Hà Nội')
    expect(dc).toContain('Phường Hà Đông')
    expect(dc).toContain('Số 15 ngõ 120 Trần Phú')
    expect(dc).not.toContain('Quận Hà Đông')
    expect(useToast().toasts.at(-1).message).toContain('theo đơn vị hành chính mới')
    w.unmount()
  })

  it('quét lại cùng một thẻ không thêm địa chỉ trùng', async () => {
    const w = await moForm()
    await quet(w, QR)
    await quet(w, QR)
    expect(w.findAll('.nv-dc')).toHaveLength(1)
    expect(useToast().toasts.at(-1).message).toContain('đã có trong danh sách địa chỉ')
    w.unmount()
  })

  it('thẻ thiếu số nhà / đường -> mở sẵn ô nhập địa chỉ với Tỉnh, Phường đã điền', async () => {
    const w = await moForm()
    await quet(w, QR.replace(/\|[^|]*\|15032021$/, '|Phường Hà Đông, Thành phố Hà Nội|15032021'))
    expect(w.findAll('.nv-dc')).toHaveLength(0)
    expect(w.find('.nv-dc-form').exists()).toBe(true)
    expect(giaTriSelect2(w, '#nv-dc-tinh')).toBe('Thành phố Hà Nội')
    expect(giaTriSelect2(w, '#nv-dc-phuong')).toBe('Phường Hà Đông')
    expect(w.find('#nv-dc-chi-tiet').element.value).toBe('')
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
    const w = await moForm([{ ...NV_CO_SAN[0], canCuoc: '001099012345' }])
    await quet(w, QR)
    expect(useToast().toasts.at(-1).message).toContain('đã thuộc nhân viên Nguyễn Hoàng Long')
    w.unmount()
  })
})

describe('Sửa nhân viên', () => {
  const NV = {
    id: 5, ma: 'NVA01', hoTen: 'Nguyễn Văn An', email: 'an@footstyle.vn', soDienThoai: '0987654321', canCuoc: '001099012345', idVaiTro: 2, vaiTro: 'Nhân viên', hoatDong: true, gioiTinh: 'Nam',
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
    expect(w.find('#nv-ma').element.value).toBe('NVA01')
    expect(w.find('[data-testid=cccd-da-quet]').text()).toContain('001099012345')
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

describe('Đã bỏ vị trí làm việc', () => {
  it('form thêm nhân viên không còn mục Vị trí làm việc; payload không gửi idViTri', async () => {
    svc.create.mockResolvedValue({ id: 9, ma: 'NVT01', hoTen: 'Lê Quốc Tuấn' })
    const w = await moForm()
    expect(w.text()).not.toContain('Vị trí làm việc')
    expect(w.find('#nv-vi-tri-moi').exists()).toBe(false)
    await set(w, '#nv-ho-ten', 'Lê Quốc Tuấn')
    await set(w, '#nv-email', 'tuan@footstyle.vn')
    await set(w, '#nv-sdt', '0987654321')
    await set(w, '#nv-mat-khau', 'Footstyle@123')
    await set(w, '#nv-ngay-sinh', '1999-05-20')
    await themDiaChi(w, 'Thành phố Hà Nội', 'Phường Hà Đông', 'Số 1')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(svc.create.mock.calls[0][0]).not.toHaveProperty('idViTri')
    w.unmount()
  })
})

describe('Trạng thái nhân viên: Còn làm / Đã nghỉ', () => {
  it('trang chi tiết: nhân viên còn làm hiện nút "Cho nghỉ việc", đã nghỉ hiện "Cho làm lại"', async () => {
    state.params = { id: '1' }
    let w = await moForm()
    expect(w.text()).toContain('Còn làm')
    expect(w.text()).toContain('Cho nghỉ việc')
    expect(w.text()).not.toContain('Khóa tài khoản')
    w.unmount()
    document.body.innerHTML = ''
    w = await moForm([{ ...NV_CO_SAN[0], hoatDong: false }])
    expect(w.text()).toContain('Đã nghỉ')
    expect(w.text()).toContain('Cho làm lại')
    expect(w.text()).not.toContain('Mở khóa')
    w.unmount()
  })
})
