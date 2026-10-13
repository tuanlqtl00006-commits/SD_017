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
    await w.findAll('button').find((b) => b.attributes('title') === 'Sửa địa chỉ').trigger('click')
    await flushPromises()
    await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
    await flushPromises()
    expect(api.put.mock.calls[0][1].quanHuyen).toBe('Quận Thanh Xuân') // không đổi gì -> giữ nguyên
    await w.findAll('button').find((b) => b.attributes('title') === 'Sửa địa chỉ').trigger('click')
    await flushPromises()
    await w.find('#kh-dc-phuong').setValue('Phường Hà Đông') // chọn phường mới theo danh sách sau sáp nhập
    await w.findAll('button').find((b) => b.text().includes('Lưu địa chỉ')).trigger('click')
    await flushPromises()
    expect(api.put.mock.calls[1][1]).toMatchObject({ phuongXa: 'Phường Hà Đông', quanHuyen: '' })
    w.unmount()
  })
})

describe('Cửa sổ địa chỉ khách hàng: nút dạng icon', () => {
  const DC = { id: 1, tenNguoiNhan: 'Nguyễn Thị Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: '', phuongXa: 'Phường Hà Đông', diaChiCuThe: 'Số 12 Nguyễn Trãi', laDiaChiMacDinh: true }
  const mo = async () => {
    api.get.mockResolvedValue({ data: [DC] })
    const w = mount(KhachHangAddressModal, { props: { customer: { id: 7, hoTen: 'Nguyễn Thị Lan', sdt: '0901000001' } }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const nut = (w, title) => w.findAll('button').find((b) => b.attributes('title') === title)

  it('không còn chữ Xóa / Sửa trên thẻ địa chỉ, có 3 icon: Xem chi tiết, Sửa, Xóa', async () => {
    const w = await mo()
    const chu = w.findAll('button').map((b) => b.text())
    expect(chu).not.toContain('Xóa')
    expect(chu).not.toContain('Sửa')
    expect(nut(w, 'Xem chi tiết').find('i.bi-eye').exists()).toBe(true)
    expect(nut(w, 'Sửa địa chỉ').find('i.bi-pencil-square').exists()).toBe(true)
    expect(nut(w, 'Xóa địa chỉ').find('i.bi-trash').exists()).toBe(true)
    w.unmount()
  })

  it('bấm icon Xem: hiện chi tiết chỉ đọc (không có ô nhập), Quay lại về danh sách', async () => {
    const w = await mo()
    await nut(w, 'Xem chi tiết').trigger('click')
    const xem = w.find('[data-testid="xem-dia-chi"]')
    expect(xem.exists()).toBe(true)
    expect(xem.text()).toContain('Nguyễn Thị Lan')
    expect(xem.text()).toContain('0901000001')
    expect(xem.text()).toContain('Phường Hà Đông')
    expect(xem.text()).toContain('Số 12 Nguyễn Trãi, Phường Hà Đông, Thành phố Hà Nội')
    expect(w.find('input').exists()).toBe(false)
    await w.findAll('button').find((b) => b.text() === 'Quay lại').trigger('click')
    expect(w.find('[data-testid="xem-dia-chi"]').exists()).toBe(false)
    expect(nut(w, 'Xem chi tiết').exists()).toBe(true)
    w.unmount()
  })

  it('icon Xóa vẫn hỏi xác nhận rồi gọi API xóa', async () => {
    const w = await mo()
    api.delete.mockResolvedValue({})
    await nut(w, 'Xóa địa chỉ').trigger('click')
    expect(api.delete).not.toHaveBeenCalled()
    const xacNhan = [...document.body.querySelectorAll('button.ad-btn-danger')].find((x) => x.textContent.trim() === 'Xóa') // hộp thoại xác nhận render ra document.body
    expect(xacNhan).toBeTruthy()
    xacNhan.click()
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/dia-chi/1')
    w.unmount()
  })
})

describe('Hóa đơn: bố cục chuẩn của trang quản lý, tab trạng thái có số, 5 dòng mỗi trang', () => {
  const DEM = { tatCa: 12, 0: 1, 1: 3, 2: 0, 3: 2, 4: 1, 5: 1, 6: 3, 7: 1 }
  const traLoi = () => api.get.mockImplementation(async (url) => {
    if (url === '/hoa-don/dem-trang-thai') return { data: DEM }
    return { data: { content: [], totalElements: 0, totalPages: 0 } }
  })

  it('dùng bố cục chuẩn: bộ lọc một hàng, đủ nhãn, không còn kiểu thu gọn riêng', async () => {
    traLoi()
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    const html = w.html()
    expect(html).not.toContain('hd-compact')
    expect(html).toContain('ad-filter-grid--hd')
    expect(w.find('.ad-page').exists()).toBe(true)
    expect(w.text()).toContain('Đặt lại bộ lọc')
    expect(w.text()).toContain('Xuất Excel')
    expect(w.text()).toContain('Loại đơn')
    expect(w.text()).toContain('Mã NV')
    expect(w.text()).toContain('SĐT KH')
    w.unmount()
  })

  it('tab trạng thái hiện số hóa đơn, có tab Hoàn tiền; phân trang cố định 5 dòng và không có ô chọn số dòng', async () => {
    traLoi()
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    const tabs = w.findAll('.ad-status-tab').map((t) => t.text().replace(/\s+/g, ' '))
    expect(tabs[0]).toBe('Tất cả 12')
    expect(tabs).toContain('Chờ xác nhận 3')
    expect(tabs).toContain('Hoàn tiền 1')
    expect(api.get.mock.calls.find((c) => c[0] === '/hoa-don')[1].params.size).toBe(5)
    // chỉ còn ô chọn Loại đơn, không có <select> nào dành cho số dòng mỗi trang
    expect(w.findAll('select').map((s) => s.attributes('id'))).toEqual(['hd-loai'])
    w.unmount()
  })

  it('bấm tab "Đã hủy" thì tải lại danh sách với trangThai = 0', async () => {
    traLoi()
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    await w.findAll('.ad-status-tab').find((t) => t.text().includes('Đã hủy')).trigger('click')
    await flushPromises()
    const lanCuoi = api.get.mock.calls.filter((c) => c[0] === '/hoa-don').at(-1)[1].params
    expect(lanCuoi.trangThai).toBe(0)
    w.unmount()
  })

  it('CSS của trang hóa đơn không ép lại cỡ chữ / chiều cao ô / bảng của trang chung', () => {
    const src = readFileSync(resolve(process.cwd(), 'src/components/HoaDonManager.vue'), 'utf-8')
    const style = src.slice(src.indexOf('<style'))
    expect(style).not.toMatch(/font-size:\s*0\.8[0-9]rem;[\s\S]*\.ad-table/)
    expect(style).not.toContain('hd-compact')
    expect(style).not.toMatch(/height:\s*3\dpx/)
  })
})

describe('Trang hóa đơn (thiết kế mới)', () => {
  const HD = [
    { id: 1, maHoaDon: 'HD001', maNhanVien: 'LQT0001', tenKhachHang: 'Nguyễn Thị Lan', sdtKhachHang: '0901000001', tongTien: 1500000, loaiDon: 'Tại quầy', ngayTao: '2026-10-09T08:00:00', trangThai: 6 },
    { id: 2, maHoaDon: 'HD002', maNhanVien: null, tenKhachHang: null, sdtKhachHang: null, tongTien: 250000, loaiDon: 'Trực tuyến', ngayTao: '2026-10-10T09:30:00', trangThai: 3 },
  ]
  const DEM = { tatCa: 12, 0: 1, 1: 2, 2: 1, 3: 3, 4: 1, 5: 1, 6: 2, 7: 1 }

  function moApi(content = HD, total = content.length) {
    api.get.mockImplementation(async (url) => {
      if (url === '/hoa-don/dem-trang-thai') return { data: DEM }
      if (url === '/hoa-don') return { data: { content, totalElements: total, totalPages: Math.ceil(total / 5) } }
      throw new Error('API không mong đợi: ' + url)
    })
  }
  const goiDanhSach = () => api.get.mock.calls.filter((c) => c[0] === '/hoa-don')
  async function moTrang() {
    const w = mount(HoaDonManager, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    return w
  }

  it('đủ cột theo thiết kế: Mã NV, SĐT KH, Loại đơn, Trạng thái; có Đặt lại bộ lọc và Xuất Excel', async () => {
    moApi()
    const w = await moTrang()
    expect(w.findAll('thead th').map((t) => t.text())).toEqual(['STT', 'Mã HD', 'Mã NV', 'Tên KH', 'SĐT KH', 'Tổng tiền TT', 'Loại đơn', 'Ngày tạo', 'Trạng thái', 'Hành động'])
    expect(w.text()).toContain('Đặt lại bộ lọc')
    expect(w.text()).toContain('Xuất Excel')
    expect(w.text()).toContain('Loại đơn')
    expect(w.find('.ad-page').exists()).toBe(true)
    w.unmount()
  })

  it('hiện mã nhân viên, SĐT khách; khách trống thì ghi Khách vãng lai và dấu —', async () => {
    moApi()
    const w = await moTrang()
    const dong = w.findAll('tbody tr')
    expect(dong[0].text()).toContain('HD001')
    expect(dong[0].text()).toContain('LQT0001')
    expect(dong[0].text()).toContain('Nguyễn Thị Lan')
    expect(dong[0].text()).toContain('0901000001')
    expect(dong[0].text()).toContain('Đã hoàn thành')
    expect(dong[1].text()).toContain('Khách vãng lai')
    expect(dong[1].text()).toContain('Chờ giao hàng')
    expect(dong[1].text()).toContain('Trực tuyến')
    expect(dong[1].text()).toContain('—')
    w.unmount()
  })

  it('9 tab trạng thái, mỗi tab có SỐ hóa đơn, có tab Hoàn tiền', async () => {
    moApi()
    const w = await moTrang()
    const tabs = w.findAll('.ad-status-tab')
    expect(tabs.map((t) => t.text().replace(/\s+/g, ' '))).toEqual([
      'Tất cả 12', 'Chờ xác nhận 2', 'Đã xác nhận 1', 'Chờ giao hàng 3', 'Đang giao hàng 1', 'Đã giao hàng 1', 'Đã hoàn thành 2', 'Đã hủy 1', 'Hoàn tiền 1',
    ])
    expect(tabs[0].classes()).toContain('active')
    w.unmount()
  })

  it('5 dòng mỗi trang cố định, không có ô chọn số dòng', async () => {
    moApi(HD, 12)
    const w = await moTrang()
    expect(goiDanhSach()[0][1].params).toMatchObject({ page: 0, size: 5, trangThai: null })
    expect(w.find('.ad-pagination select, select.ad-page-size').exists()).toBe(false)
    const phanTrang = w.findComponent({ name: 'BasePagination' })
    expect(phanTrang.exists()).toBe(true)
    expect(phanTrang.find('select').exists()).toBe(false)
    w.unmount()
  })

  it('bấm tab "Đã hủy" thì gọi API với trangThai = 0 và về trang 1', async () => {
    moApi()
    const w = await moTrang()
    api.get.mockClear()
    moApi([HD[1]], 1)
    await w.findAll('.ad-status-tab').find((t) => t.text().includes('Đã hủy')).trigger('click')
    await flushPromises()
    const lanCuoi = goiDanhSach().at(-1)[1].params
    expect(lanCuoi).toMatchObject({ page: 0, trangThai: 0 })
    expect(w.findAll('.ad-status-tab').find((t) => t.text().includes('Đã hủy')).classes()).toContain('active')
    w.unmount()
  })

  it('bấm tab "Hoàn tiền" gọi API với trangThai = 7; Đặt lại bộ lọc xóa mã hóa đơn đã gõ', async () => {
    moApi()
    const w = await moTrang()
    await w.findAll('.ad-status-tab').find((t) => t.text().includes('Hoàn tiền')).trigger('click')
    await flushPromises()
    expect(goiDanhSach().at(-1)[1].params.trangThai).toBe(7)
    await w.find('#hd-ma').setValue('HD00')
    expect(w.find('#hd-ma').element.value).toBe('HD00')
    await w.findAll('button').find((b) => b.text().includes('Đặt lại bộ lọc')).trigger('click')
    expect(w.find('#hd-ma').element.value).toBe('')
    w.unmount()
  })

  it('đổi loại đơn thì tự lọc, không cần bấm nút tìm', async () => {
    moApi()
    const w = await moTrang()
    await w.find('#hd-loai').setValue('2')
    await flushPromises()
    expect(goiDanhSach().at(-1)[1].params.loaiDon).toBe('2')
    w.unmount()
  })

  it('CSS trang hóa đơn không ép lại cỡ chữ / chiều cao ô / bảng của trang chung', () => {
    const src = readFileSync(resolve(process.cwd(), 'src/components/HoaDonManager.vue'), 'utf-8')
    const style = src.slice(src.indexOf('<style'))
    expect(style).not.toMatch(/font-size:\s*0\.8[0-9]rem;[\s\S]*\.ad-table/)
    expect(style).not.toContain('hd-compact')
    expect(style).not.toMatch(/height:\s*3\dpx/)
  })
})
