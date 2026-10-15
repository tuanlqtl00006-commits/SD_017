import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { svc, api, push, xuat } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), toggleActive: vi.fn() },
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  push: vi.fn(),
  xuat: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/khachHangService', () => ({ khachHangService: svc }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/utils/exportExcel', () => ({ exportExcel: xuat }))
vi.mock('../src/services/diaChiHanhChinhService', () => ({
  API_DIA_CHI: 'x',
  diaChiHanhChinhService: { layTinhThanh: async () => [], layPhuongXa: async () => [], xoaNho() {} },
}))

import KhachHangManager from '../src/components/KhachHangManager.vue'
import { useToast } from '../src/composables/useToast'
import { cacMucSelect2, datSelect2 } from './helpersSelect2'

const kh = (id, ma, hoTen, extra = {}) => ({
  id,
  maKhachHang: ma,
  hoTen,
  sdt: `09010000${String(id).padStart(2, '0')}`,
  email: `${ma.toLowerCase()}@gmail.com`,
  gioiTinh: 1,
  trangThai: 1,
  ngaySinh: '1995-03-12',
  diaChiList: [],
  ...extra,
})
const taoDS = () => [
  kh(1, 'KH01', 'Nguyễn Thị Lan', {
    gioiTinh: 0,
    diaChiList: [{ id: 1, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: 'Quận Thanh Xuân', phuongXa: 'Phường Thượng Đình', diaChiCuThe: '12 Nguyễn Trãi', laDiaChiMacDinh: true }],
  }),
  kh(2, 'KH02', 'Trần Văn Hùng', { trangThai: 0 }),
  kh(3, 'KH03', 'Lê Minh Tuấn', { diaChiList: [{ id: 2, tenNguoiNhan: 'Tuấn', sdtNguoiNhan: '0901000003', tinhThanhPho: 'Thành phố Đà Nẵng', phuongXa: 'Phường Hải Châu 1', diaChiCuThe: '8 Trần Phú', laDiaChiMacDinh: false }] }),
]

let DS = taoDS()

beforeEach(() => {
  DS = taoDS() // dữ liệu mới cho mỗi test: component đổi trạng thái ngay trên object nhận về
  document.body.innerHTML = ''
  Object.values(svc).forEach((f) => f.mockReset())
  Object.values(api).forEach((f) => f.mockReset())
  push.mockReset()
  xuat.mockReset()
  svc.getAll.mockResolvedValue(DS)
})

async function mo(ds = DS) {
  svc.getAll.mockResolvedValue(ds)
  const w = mount(KhachHangManager, { attachTo: document.body, global: { stubs: { RouterLink: true } } })
  await flushPromises()
  return w
}
const maTrongBang = (w) => w.findAll('tbody .ad-code').map((c) => c.text())
const nhieuKhach = (n) => Array.from({ length: n }, (_, i) => kh(i + 1, `KH${String(i + 1).padStart(2, '0')}`, `Khách số ${String.fromCharCode(65 + (i % 26))}${i}`))

describe('Danh sách khách hàng: bố cục chuẩn của trang quản lý', () => {
  it('dùng khung .ad-page, bộ lọc có nhãn, nút Đặt lại bộ lọc / Xuất Excel / Thêm khách hàng; không còn <select> thường', async () => {
    const w = await mo()
    expect(w.find('.ad-page').exists()).toBe(true)
    expect(w.findAll('select')).toHaveLength(0)
    expect(w.findAll('.s2-trigger').map((s) => s.attributes('id'))).toEqual(['kh-loc-gioi-tinh', 'kh-loc-trang-thai'])
    for (const chu of ['Tìm kiếm', 'Giới tính', 'Trạng thái', 'Đặt lại bộ lọc', 'Xuất Excel', 'Thêm khách hàng']) expect(w.text()).toContain(chu)
    expect(w.html()).not.toContain('bg-opacity') // không còn kiểu Bootstrap rời rạc của bản cũ
    w.unmount()
  })

  it('đủ cột và số ô mỗi dòng khớp số cột tiêu đề', async () => {
    const w = await mo()
    const tieuDe = w.findAll('thead th').map((t) => t.text())
    expect(tieuDe).toEqual(['STT', '', 'Mã KH', 'Họ tên', 'Email', 'SĐT', 'Giới tính', 'Tên người nhận', 'Địa chỉ', 'Trạng thái', 'Hành động'])
    for (const h of w.findAll('tbody tr')) expect(h.findAll('td')).toHaveLength(tieuDe.length)
    expect(w.text()).toContain('3 bản ghi hiển thị')
    w.unmount()
  })

  it('địa chỉ mặc định ghép một dòng; khách chưa có địa chỉ ghi rõ; giới tính và trạng thái hiện chữ', async () => {
    const w = await mo()
    const hang = w.findAll('tbody tr')
    expect(hang[0].text()).toContain('12 Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Thành phố Hà Nội')
    expect(hang[0].text()).toContain('Nữ')
    expect(hang[1].text()).toContain('Chưa cập nhật địa chỉ')
    expect(hang[1].text()).toContain('Ngừng hoạt động')
    expect(hang[1].text()).toContain('Nam')
    expect(hang[2].text()).toContain('8 Trần Phú, Phường Hải Châu 1, Thành phố Đà Nẵng') // không có địa chỉ mặc định -> lấy địa chỉ đầu tiên
    expect(hang[0].find('.ad-pill-green').exists()).toBe(true)
    expect(hang[1].find('.ad-pill-red').exists()).toBe(true)
    w.unmount()
  })
})

describe('Danh sách khách hàng: ảnh đại diện và tên người nhận (gộp từ bản của nhóm)', () => {
  it('cột Tên người nhận lấy theo địa chỉ mặc định (không có mặc định thì địa chỉ đầu), chưa có địa chỉ thì ghi Chưa cập nhật', async () => {
    const w = await mo()
    const hang = w.findAll('tbody tr')
    const nhan = (h) => h.findAll('td')[7].text()
    expect(nhan(hang[0])).toBe('Lan')
    expect(nhan(hang[1])).toBe('Chưa cập nhật')
    expect(nhan(hang[2])).toBe('Tuấn')
    w.unmount()
  })

  it('có ảnh đại diện thì hiện ảnh; chưa có thì hiện chữ cái đầu; ảnh hỏng thì quay về chữ cái đầu', async () => {
    const ds = taoDS()
    ds[0].anhDaiDien = '/api/uploads/abc.png'
    const w = await mo(ds)
    const hang = w.findAll('tbody tr')
    const img = hang[0].find('.ad-avatar img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/api/uploads/abc.png')
    expect(hang[1].find('.ad-avatar img').exists()).toBe(false)
    expect(hang[1].find('.ad-avatar').text()).not.toBe('')
    await img.trigger('error')
    expect(w.findAll('tbody tr')[0].find('.ad-avatar img').exists()).toBe(false)
    expect(w.findAll('tbody tr')[0].find('.ad-avatar').text()).not.toBe('')
    w.unmount()
  })

  it('xuất Excel có thêm cột Tên người nhận', async () => {
    const w = await mo()
    await w.findAll('button').find((b) => b.text().includes('Xuất Excel')).trigger('click')
    const opt = xuat.mock.calls[0][0]
    expect(opt.columns.map((c) => c.header)).toContain('Tên người nhận')
    expect(opt.rows.map((r) => r.nguoiNhan)).toEqual(['Lan', '', 'Tuấn'])
    w.unmount()
  })
})

describe('Danh sách khách hàng: lọc (Select2) và tìm kiếm', () => {
  it('bộ lọc trạng thái và giới tính là Select2 có đủ mục', async () => {
    const w = await mo()
    expect(await cacMucSelect2(w, '#kh-loc-trang-thai')).toEqual(['Tất cả trạng thái', 'Hoạt động', 'Ngừng hoạt động'])
    expect(await cacMucSelect2(w, '#kh-loc-gioi-tinh')).toEqual(['Tất cả giới tính', 'Nam', 'Nữ'])
    w.unmount()
  })

  it('lọc theo trạng thái, theo giới tính; đặt lại bộ lọc trả về đủ', async () => {
    const w = await mo()
    await datSelect2(w, '#kh-loc-trang-thai', '0')
    expect(maTrongBang(w)).toEqual(['KH02'])
    await datSelect2(w, '#kh-loc-trang-thai', '')
    await datSelect2(w, '#kh-loc-gioi-tinh', '0')
    expect(maTrongBang(w)).toEqual(['KH01'])
    await w.findAll('button').find((b) => b.text().includes('Đặt lại bộ lọc')).trigger('click')
    expect(maTrongBang(w)).toEqual(['KH01', 'KH02', 'KH03'])
    w.unmount()
  })

  it('tìm theo tên không dấu, theo SĐT, theo email và theo mã; không có thì báo không tìm thấy', async () => {
    const w = await mo()
    const tim = async (v) => {
      await w.find('#kh-keyword').setValue(v)
      return maTrongBang(w)
    }
    expect(await tim('hung')).toEqual(['KH02'])
    expect(await tim('nguyen thi')).toEqual(['KH01'])
    expect(await tim('0901000003')).toEqual(['KH03'])
    expect(await tim('kh03@gmail')).toEqual(['KH03'])
    expect(await tim('KH01')).toEqual(['KH01'])
    expect(await tim('khong co ai')).toEqual([])
    expect(w.text()).toContain('Không tìm thấy khách hàng')
    w.unmount()
  })
})

describe('Danh sách khách hàng: phân trang rút gọn', () => {
  const nutTrang = (w) => w.findAll('.ad-page-btn, .ad-page-gap').map((b) => b.text()).filter(Boolean)

  it('5 dòng mỗi trang; nhiều trang thì hiện dạng 1 2 … N, bấm trang để chuyển', async () => {
    const w = await mo(nhieuKhach(60))
    expect(w.findAll('tbody tr')).toHaveLength(5)
    expect(w.text()).toContain('60 bản ghi hiển thị')
    expect(nutTrang(w)).toEqual(['1', '2', '…', '12'])
    expect(w.find('select').exists()).toBe(false) // không có ô chọn số dòng
    await w.findAll('.ad-page-btn').find((b) => b.text() === '2').trigger('click')
    expect(maTrongBang(w)).toEqual(['KH06', 'KH07', 'KH08', 'KH09', 'KH10'])
    expect(nutTrang(w)).toEqual(['1', '2', '3', '…', '12'])
    await w.findAll('.ad-page-btn').find((b) => b.text() === '12').trigger('click')
    expect(maTrongBang(w)).toEqual(['KH56', 'KH57', 'KH58', 'KH59', 'KH60'])
    expect(nutTrang(w)).toEqual(['1', '2', '…', '11', '12'])
    w.unmount()
  })

  it('lọc làm ít trang đi thì về trang 1; ít dữ liệu thì chỉ có một nút trang', async () => {
    const w = await mo(nhieuKhach(60))
    await w.findAll('.ad-page-btn').find((b) => b.text() === '12').trigger('click')
    await w.find('#kh-keyword').setValue('KH01')
    expect(maTrongBang(w)).toEqual(['KH01'])
    expect(nutTrang(w)).toEqual(['1'])
    w.unmount()
  })
})

describe('Danh sách khách hàng: hành động', () => {
  it('nút Thêm khách hàng và nút sửa chuyển sang trang riêng', async () => {
    const w = await mo()
    await w.findAll('button').find((b) => b.text().includes('Thêm khách hàng')).trigger('click')
    expect(push).toHaveBeenLastCalledWith('/khach-hang/them')
    await w.findAll('tbody tr')[1].find('button[title="Xem / sửa khách hàng"]').trigger('click')
    expect(push).toHaveBeenLastCalledWith('/khach-hang/2/sua')
    w.unmount()
  })

  it('nút bật/tắt hỏi xác nhận rồi đổi trạng thái ngay tại dòng', async () => {
    svc.toggleActive.mockResolvedValue({ ...DS[0], trangThai: 0 })
    const w = await mo()
    const dong = () => w.findAll('tbody tr')[0]
    expect(dong().find('button[title="Ngừng hoạt động"]').exists()).toBe(true)
    await dong().find('button[title="Ngừng hoạt động"]').trigger('click')
    expect(document.body.textContent).toContain('Bạn có chắc chắn muốn ngừng hoạt động khách hàng Nguyễn Thị Lan?')
    expect(svc.toggleActive).not.toHaveBeenCalled()
    ;[...document.body.querySelectorAll('button.ad-btn-danger')].find((b) => b.textContent.includes('Ngừng hoạt động')).click()
    await flushPromises()
    expect(svc.toggleActive).toHaveBeenCalledWith(1)
    expect(dong().text()).toContain('Ngừng hoạt động')
    expect(dong().find('button[title="Kích hoạt"]').exists()).toBe(true)
    expect(useToast().toasts.at(-1).message).toBe('Đã ngừng hoạt động khách hàng.')
    w.unmount()
  })

  it('đổi trạng thái lỗi thì báo lỗi, không đổi dòng', async () => {
    svc.toggleActive.mockRejectedValue(new Error('Không thể đổi trạng thái.'))
    const w = await mo()
    await w.findAll('tbody tr')[0].find('button[title="Ngừng hoạt động"]').trigger('click')
    ;[...document.body.querySelectorAll('button.ad-btn-danger')].find((b) => b.textContent.includes('Ngừng hoạt động')).click()
    await flushPromises()
    expect(useToast().toasts.at(-1).message).toBe('Không thể đổi trạng thái.')
    expect(w.findAll('tbody tr')[0].text()).toContain('Hoạt động')
    w.unmount()
  })

  it('nút địa chỉ mở cửa sổ quản lý địa chỉ của đúng khách', async () => {
    api.get.mockResolvedValue({ data: [] })
    const w = await mo()
    await w.findAll('tbody tr')[2].find('button[title="Quản lý địa chỉ"]').trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/dia-chi/khach-hang/3')
    expect(new DOMWrapper(document.body).text()).toContain('Địa chỉ của Lê Minh Tuấn')
    w.unmount()
  })

  it('xuất Excel đúng theo danh sách đang lọc; không có dữ liệu thì báo lỗi', async () => {
    const w = await mo()
    await datSelect2(w, '#kh-loc-trang-thai', '1')
    await w.findAll('button').find((b) => b.text().includes('Xuất Excel')).trigger('click')
    expect(xuat).toHaveBeenCalledTimes(1)
    const opt = xuat.mock.calls[0][0]
    expect(opt.rows.map((r) => r.ma)).toEqual(['KH01', 'KH03'])
    expect(opt.rows[0]).toMatchObject({ ten: 'Nguyễn Thị Lan', sdt: '0901000001', gioiTinh: 'Nữ', ngaySinh: '12/03/1995', trangThai: 'Hoạt động' })
    expect(opt.columns.map((c) => c.header)).toContain('Mã khách hàng')
    await w.find('#kh-keyword').setValue('không có ai')
    await w.findAll('button').find((b) => b.text().includes('Xuất Excel')).trigger('click')
    expect(xuat).toHaveBeenCalledTimes(1)
    expect(useToast().toasts.at(-1).message).toBe('Không có khách hàng nào để xuất.')
    w.unmount()
  })

  it('tải danh sách lỗi thì báo lỗi, không vỡ trang', async () => {
    svc.getAll.mockRejectedValue(new Error('Không kết nối được máy chủ.'))
    const w = mount(KhachHangManager, { attachTo: document.body, global: { stubs: { RouterLink: true } } })
    await flushPromises()
    expect(useToast().toasts.at(-1).message).toBe('Không kết nối được máy chủ.')
    expect(w.text()).toContain('Không tìm thấy khách hàng')
    w.unmount()
  })
})
