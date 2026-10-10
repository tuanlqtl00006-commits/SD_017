import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'

const { api, push } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  push: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {} }),
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/diaChiHanhChinhService', () => {
  const tinh = [{ code: 1, name: 'Thành phố Hà Nội' }, { code: 33, name: 'Tỉnh Hưng Yên' }]
  const phuong = { 1: [{ code: 10, name: 'Phường Hà Đông' }], 33: [{ code: 20, name: 'Phường Thái Bình' }] }
  return { API_DIA_CHI: 'x', diaChiHanhChinhService: { layTinhThanh: async () => tinh, layPhuongXa: async (m) => phuong[m] ?? [], xoaNho() {} } }
})

import KhachHangAdd from '../src/components/KhachHangAdd.vue'
import KhachHangAddressModal from '../src/components/KhachHangAddressModal.vue'
import DiaChiHanhChinhSelect from '../src/components/common/DiaChiHanhChinhSelect.vue'
import HoaDonManager from '../src/components/HoaDonManager.vue'

beforeEach(() => {
  document.body.innerHTML = ''
  Object.values(api).forEach((f) => f.mockReset())
  push.mockReset()
})

describe('Không còn dùng API địa chỉ cũ (63 tỉnh + quận/huyện)', () => {
  const duyet = (dir) => readdirSync(dir).flatMap((f) => {
    const p = join(dir, f)
    return statSync(p).isDirectory() ? duyet(p) : /\.(vue|js)$/.test(f) ? [p] : []
  })
  it('không file nào trong src còn gọi esgoo.net', () => {
    const con = duyet(resolve(process.cwd(), 'src')).filter((p) => readFileSync(p, 'utf-8').includes('esgoo'))
    expect(con).toEqual([])
  })
})

describe('Thêm khách hàng: địa chỉ sau sáp nhập', () => {
  async function dien(w) {
    await w.find('input[placeholder="Nhập họ và tên"]').setValue('Nguyễn Thị Lan')
    await w.find('input[placeholder="Nhập email"]').setValue('lan@gmail.com')
    await w.find('input[placeholder="Nhập số điện thoại"]').setValue('0901000001')
    await w.find('input[placeholder="Số nhà, đường..."]').setValue('Số 12 Nguyễn Trãi')
  }
  it('chỉ có Tỉnh/Thành phố và Phường/Xã, không có Quận/Huyện', async () => {
    const w = mount(KhachHangAdd)
    await flushPromises()
    expect(w.text()).toContain('Tỉnh/Thành phố')
    expect(w.text()).toContain('Phường/Xã')
    expect(w.text()).not.toContain('Quận/Huyện')
    expect(w.findAll('#kh-add-tinh option').map((o) => o.text())).toEqual(['Chọn Tỉnh/Thành phố', 'Thành phố Hà Nội', 'Tỉnh Hưng Yên'])
    w.unmount()
  })
  it('thiếu phường/xã thì không cho tạo', async () => {
    const w = mount(KhachHangAdd)
    await flushPromises()
    await dien(w)
    await w.find('#kh-add-tinh').setValue('Thành phố Hà Nội')
    await flushPromises()
    await w.find('button.btn-primary').trigger('click')
    expect(api.post).not.toHaveBeenCalled()
    expect(w.text()).toContain('Địa chỉ giao hàng')
    expect(w.find('.alert-danger').exists()).toBe(true)
    w.unmount()
  })
  it('đủ thông tin: gửi tỉnh + phường/xã mới, không gửi quận/huyện', async () => {
    api.post.mockResolvedValue({ data: {} })
    const w = mount(KhachHangAdd)
    await flushPromises()
    await dien(w)
    await w.find('#kh-add-tinh').setValue('Tỉnh Hưng Yên')
    await flushPromises()
    await w.find('#kh-add-phuong').setValue('Phường Thái Bình')
    await w.find('button.btn-primary').trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledTimes(1)
    const [url, p] = api.post.mock.calls[0]
    expect(url).toBe('/khach-hang')
    expect(p).toMatchObject({ tinhThanh: 'Tỉnh Hưng Yên', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 12 Nguyễn Trãi' })
    expect(p).not.toHaveProperty('quanHuyen')
    expect(push).toHaveBeenCalledWith('/khach-hang')
    w.unmount()
  })
})

describe('Ô chọn Tỉnh/Phường xử lý địa chỉ cũ (trước sáp nhập)', () => {
  it('tỉnh cũ không còn trong danh sách mới vẫn hiện, kèm nhắc chọn lại; phường bị khóa', async () => {
    const w = mount(DiaChiHanhChinhSelect, { props: { tinhThanh: 'Tỉnh Thái Bình', phuongXa: 'Xã Đông Quý' } })
    await flushPromises()
    expect(w.find('#dc-tinh').element.value).toBe('Tỉnh Thái Bình')
    expect(w.findAll('#dc-tinh option').map((o) => o.text())).toContain('Tỉnh Thái Bình (địa chỉ cũ)')
    expect(w.text()).toContain('đơn vị hành chính cũ')
    expect(w.find('#dc-phuong').element.disabled).toBe(true)
    w.unmount()
  })
  it('tỉnh vẫn còn, phường cũ đã sáp nhập: phường cũ vẫn hiện để chọn lại', async () => {
    const w = mount(DiaChiHanhChinhSelect, { props: { tinhThanh: 'Thành phố Hà Nội', phuongXa: 'Phường Thượng Đình' } })
    await flushPromises()
    expect(w.findAll('#dc-phuong option').map((o) => o.text())).toEqual(['Chọn Phường/Xã', 'Phường Thượng Đình (địa chỉ cũ)', 'Phường Hà Đông'])
    w.unmount()
  })
  it('chọn tỉnh phát ra sự kiện cập nhật và xóa phường/xã', async () => {
    const w = mount(DiaChiHanhChinhSelect, { props: { tinhThanh: '', phuongXa: 'Phường X' } })
    await flushPromises()
    await w.find('#dc-tinh').setValue('Tỉnh Hưng Yên')
    expect(w.emitted('update:tinhThanh')[0]).toEqual(['Tỉnh Hưng Yên'])
    expect(w.emitted('update:phuongXa')[0]).toEqual([''])
    w.unmount()
  })
})

describe('Địa chỉ khách hàng (modal)', () => {
  it('mở form thêm địa chỉ: 2 cấp Tỉnh → Phường, khách hàng vẫn có nút Xóa/Sửa như cũ', async () => {
    api.get.mockResolvedValue({ data: [
      { id: 1, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: 'Quận Thanh Xuân', phuongXa: 'Phường Thượng Đình', diaChiCuThe: '12 Nguyễn Trãi', laDiaChiMacDinh: true },
      { id: 2, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901', tinhThanhPho: 'Tỉnh Hưng Yên', quanHuyen: '', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 3', laDiaChiMacDinh: false },
    ] })
    const w = mount(KhachHangAddressModal, { props: { customer: { id: 7, hoTen: 'Nguyễn Thị Lan', sdt: '0901000001' } }, attachTo: document.body })
    await flushPromises()
    // địa chỉ cũ (có quận/huyện) vẫn hiện đủ; địa chỉ mới không có dấu phẩy thừa
    expect(w.text()).toContain('12 Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Thành phố Hà Nội')
    expect(w.text()).toContain('Số 3, Phường Thái Bình, Tỉnh Hưng Yên')
    expect(w.text()).not.toMatch(/,\s*,/)
    await w.findAll('button').find((b) => b.text().includes('Thêm địa chỉ mới')).trigger('click')
    await flushPromises()
    expect(w.text()).not.toContain('Quận/Huyện')
    expect(w.find('#kh-dc-tinh').exists()).toBe(true)
    expect(w.find('#kh-dc-phuong').exists()).toBe(true)
    // lưu địa chỉ mới: chỉ tỉnh + phường/xã, quận/huyện để trống
    api.post.mockResolvedValue({ data: {} })
    await w.find('#kh-dc-tinh').setValue('Thành phố Hà Nội')
    await flushPromises()
    await w.find('#kh-dc-phuong').setValue('Phường Hà Đông')
    await w.findAll('input.form-control').at(-1).setValue('Số 9 Bạch Đằng') // ô "Địa chỉ cụ thể"
    await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledTimes(1)
    expect(api.post.mock.calls[0][0]).toBe('/dia-chi/khach-hang/7')
    expect(api.post.mock.calls[0][1]).toMatchObject({ tinhThanhPho: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', quanHuyen: '', diaChiCuThe: 'Số 9 Bạch Đằng' })
    w.unmount()
  })

  it('sửa địa chỉ cũ mà không đổi tỉnh/phường thì giữ nguyên quận/huyện cũ; đổi sang đơn vị mới thì bỏ quận/huyện', async () => {
    const cu = { id: 1, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: 'Quận Thanh Xuân', phuongXa: 'Phường Thượng Đình', diaChiCuThe: '12 Nguyễn Trãi', laDiaChiMacDinh: true }
    api.get.mockResolvedValue({ data: [cu] })
    api.put.mockResolvedValue({ data: {} })
    const w = mount(KhachHangAddressModal, { props: { customer: { id: 7, hoTen: 'Lan', sdt: '0901000001' } }, attachTo: document.body })
    await flushPromises()
    await w.findAll('button').find((b) => b.text() === 'Sửa').trigger('click')
    await flushPromises()
    await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
    await flushPromises()
    expect(api.put.mock.calls[0][1].quanHuyen).toBe('Quận Thanh Xuân') // không đổi gì -> giữ nguyên
    await w.findAll('button').find((b) => b.text() === 'Sửa').trigger('click')
    await flushPromises()
    await w.find('#kh-dc-phuong').setValue('Phường Hà Đông') // chọn phường mới theo danh sách sau sáp nhập
    await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
    await flushPromises()
    expect(api.put.mock.calls[1][1]).toMatchObject({ phuongXa: 'Phường Hà Đông', quanHuyen: '' })
    w.unmount()
  })
})

describe('Hóa đơn: trang danh sách theo thiết kế', () => {
  const HD = (id) => ({ id, maHoaDon: 'HD' + String(id).padStart(4, '0'), maNhanVien: 'LongNH01', tenKhachHang: 'Nguyễn Thị Lan', sdtKhachHang: '0901000001', tongTien: 1500000, loaiDon: 'Trực tuyến', ngayTao: '2026-10-01T09:00:00', trangThai: 3 })
  const DEM = { tatCa: 12, 0: 1, 1: 2, 2: 1, 3: 3, 4: 1, 5: 1, 6: 2, 7: 1 }
  function dungApi() {
    api.get.mockImplementation(async (url) => {
      if (url === '/hoa-don/dem-trang-thai') return { data: DEM }
      return { data: { content: [HD(1), HD(2)], totalElements: 12, totalPages: 3 } }
    })
  }

  it('đủ cột, tên trạng thái mới, số hóa đơn trên từng tab và tab Hoàn tiền', async () => {
    dungApi()
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    const cot = w.findAll('thead th').map((th) => th.text())
    expect(cot).toEqual(['STT', 'Mã HD', 'Mã NV', 'Tên KH', 'SĐT KH', 'Tổng tiền TT', 'Loại đơn', 'Ngày tạo', 'Trạng thái', 'Hành động'])
    const tab = w.findAll('.ad-status-tab').map((t) => t.text())
    expect(tab).toEqual(['Tất cả 12', 'Chờ xác nhận 2', 'Đã xác nhận 1', 'Chờ giao hàng 3', 'Đang giao hàng 1', 'Đã giao hàng 1', 'Đã hoàn thành 2', 'Đã hủy 1', 'Hoàn tiền 1'])
    expect(w.text()).toContain('Đặt lại bộ lọc')
    expect(w.text()).toContain('Xuất Excel')
    expect(w.text()).toContain('LongNH01')
    expect(w.text()).toContain('0901000001')
    w.unmount()
  })

  it('mỗi trang 5 dòng cố định, không có ô chọn số dòng', async () => {
    dungApi()
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    expect(w.find('.ad-pager select').exists()).toBe(false)
    const goiDs = api.get.mock.calls.find((c) => c[0] === '/hoa-don')
    expect(goiDs[1].params).toMatchObject({ page: 0, size: 5, trangThai: null })
    w.unmount()
  })

  it('bấm tab "Đã hủy" thì gọi API với trangThai = 0', async () => {
    dungApi()
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    api.get.mockClear()
    dungApi()
    await w.findAll('.ad-status-tab').find((t) => t.text().startsWith('Đã hủy')).trigger('click')
    await flushPromises()
    const goiDs = api.get.mock.calls.filter((c) => c[0] === '/hoa-don').at(-1)
    expect(goiDs[1].params.trangThai).toBe(0)
    w.unmount()
  })
})

describe('Bản gộp không còn mã trùng chức năng', () => {
  const duyet = (dir) => readdirSync(dir).flatMap((f) => {
    const p = join(dir, f)
    return statSync(p).isDirectory() ? duyet(p) : /\.(vue|js)$/.test(f) ? [p] : []
  })
  it('không còn tham chiếu tên cũ của các nhánh (canCuoc, CccdScanModal, useDiaChiVN)', () => {
    const noi = duyet(resolve(process.cwd(), 'src')).map((p) => [p, readFileSync(p, 'utf-8')])
    for (const tu of ['canCuoc', 'CccdScanModal', 'useDiaChiVN', 'docQrCanCuoc']) {
      expect(noi.filter(([, s]) => s.includes(tu)).map(([p]) => p)).toEqual([])
    }
  })
})
