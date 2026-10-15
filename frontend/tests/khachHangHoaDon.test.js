import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
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

import KhachHangAddressModal from '../src/components/KhachHangAddressModal.vue'
import DiaChiHanhChinhSelect from '../src/components/common/DiaChiHanhChinhSelect.vue'
import HoaDonManager from '../src/components/HoaDonManager.vue'
import { cacMucSelect2, chuSelect2, datSelect2, giaTriSelect2 } from './helpersSelect2'

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

describe('Ô chọn Tỉnh/Phường xử lý địa chỉ cũ (trước sáp nhập)', () => {
  it('tỉnh cũ không còn trong danh sách mới vẫn hiện, kèm nhắc chọn lại; phường bị khóa', async () => {
    const w = mount(DiaChiHanhChinhSelect, { props: { tinhThanh: 'Tỉnh Thái Bình', phuongXa: 'Xã Đông Quý' } })
    await flushPromises()
    expect(giaTriSelect2(w, '#dc-tinh')).toBe('Tỉnh Thái Bình')
    expect(chuSelect2(w, '#dc-tinh')).toBe('Tỉnh Thái Bình (địa chỉ cũ)')
    expect(await cacMucSelect2(w, '#dc-tinh')).toContain('Tỉnh Thái Bình (địa chỉ cũ)')
    expect(w.text()).toContain('đơn vị hành chính cũ')
    expect(w.find('#dc-phuong').element.disabled).toBe(true)
    w.unmount()
  })
  it('tỉnh vẫn còn, phường cũ đã sáp nhập: phường cũ vẫn hiện để chọn lại', async () => {
    const w = mount(DiaChiHanhChinhSelect, { props: { tinhThanh: 'Thành phố Hà Nội', phuongXa: 'Phường Thượng Đình' } })
    await flushPromises()
    expect(await cacMucSelect2(w, '#dc-phuong')).toEqual(['Phường Thượng Đình (địa chỉ cũ)', 'Phường Hà Đông'])
    w.unmount()
  })
  it('chọn tỉnh phát ra sự kiện cập nhật và xóa phường/xã', async () => {
    const w = mount(DiaChiHanhChinhSelect, { props: { tinhThanh: '', phuongXa: 'Phường X' } })
    await flushPromises()
    await datSelect2(w, '#dc-tinh', 'Tỉnh Hưng Yên')
    expect(w.emitted('update:tinhThanh')[0]).toEqual(['Tỉnh Hưng Yên'])
    expect(w.emitted('update:phuongXa')[0]).toEqual([''])
    w.unmount()
  })
})

// Cửa sổ địa chỉ dùng BaseModal (Teleport ra <body>): thao tác qua DOMWrapper của document.body
const body = () => new DOMWrapper(document.body)
const nutTheoTitle = (title) => body().findAll('button').find((b) => b.attributes('title') === title)
const nutTheoChu = (chu) => body().findAll('button').find((b) => b.text().includes(chu))
const mocModal = async (ds, customer = { id: 7, hoTen: 'Nguyễn Thị Lan', sdt: '0901000001' }) => {
  api.get.mockResolvedValue({ data: ds })
  const w = mount(KhachHangAddressModal, { props: { customer }, attachTo: document.body })
  await flushPromises()
  return w
}

describe('Địa chỉ khách hàng (modal)', () => {
  const DS = [
    { id: 1, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: 'Quận Thanh Xuân', phuongXa: 'Phường Thượng Đình', diaChiCuThe: '12 Nguyễn Trãi', laDiaChiMacDinh: true },
    { id: 2, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Tỉnh Hưng Yên', quanHuyen: '', phuongXa: 'Phường Thái Bình', diaChiCuThe: 'Số 3', laDiaChiMacDinh: false },
  ]

  it('mở form thêm địa chỉ: 2 cấp Tỉnh → Phường, khách hàng vẫn có nút Xóa/Sửa như cũ', async () => {
    const w = await mocModal(DS)
    const chu = document.body.textContent
    // địa chỉ cũ (có quận/huyện) vẫn hiện đủ; địa chỉ mới không có dấu phẩy thừa
    expect(chu).toContain('12 Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Thành phố Hà Nội')
    expect(chu).toContain('Số 3, Phường Thái Bình, Tỉnh Hưng Yên')
    expect(chu).not.toMatch(/,\s*,/)
    await nutTheoChu('Thêm địa chỉ mới').trigger('click')
    await flushPromises()
    expect(document.body.textContent).not.toContain('Quận/Huyện')
    expect(body().find('#kh-dc-tinh').exists()).toBe(true)
    expect(body().find('#kh-dc-phuong').exists()).toBe(true)
    // tên + SĐT người nhận lấy sẵn theo khách hàng
    expect(body().find('#kh-dc-ten').element.value).toBe('Nguyễn Thị Lan')
    expect(body().find('#kh-dc-sdt').element.value).toBe('0901000001')
    // lưu địa chỉ mới: chỉ tỉnh + phường/xã, quận/huyện để trống
    api.post.mockResolvedValue({ data: {} })
    await datSelect2(body(), '#kh-dc-tinh', 'Thành phố Hà Nội')
    await flushPromises()
    await datSelect2(body(), '#kh-dc-phuong', 'Phường Hà Đông')
    await body().find('#kh-dc-chi-tiet').setValue('Số 9 Bạch Đằng')
    await nutTheoChu('Lưu địa chỉ').trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledTimes(1)
    expect(api.post.mock.calls[0][0]).toBe('/dia-chi/khach-hang/7')
    expect(api.post.mock.calls[0][1]).toMatchObject({ tinhThanhPho: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', quanHuyen: '', diaChiCuThe: 'Số 9 Bạch Đằng' })
    w.unmount()
  })

  it('sửa địa chỉ cũ mà không đổi tỉnh/phường thì giữ nguyên quận/huyện cũ; đổi sang đơn vị mới thì bỏ quận/huyện', async () => {
    const cu = { id: 1, tenNguoiNhan: 'Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: 'Quận Thanh Xuân', phuongXa: 'Phường Thượng Đình', diaChiCuThe: '12 Nguyễn Trãi', laDiaChiMacDinh: true }
    api.put.mockResolvedValue({ data: {} })
    const w = await mocModal([cu])
    await nutTheoTitle('Sửa địa chỉ').trigger('click')
    await flushPromises()
    await nutTheoChu('Lưu địa chỉ').trigger('click')
    await flushPromises()
    expect(api.put.mock.calls[0][1].quanHuyen).toBe('Quận Thanh Xuân') // không đổi gì -> giữ nguyên
    await nutTheoTitle('Sửa địa chỉ').trigger('click')
    await flushPromises()
    await datSelect2(body(), '#kh-dc-phuong', 'Phường Hà Đông') // chọn phường mới theo danh sách sau sáp nhập
    await nutTheoChu('Lưu địa chỉ').trigger('click')
    await flushPromises()
    expect(api.put.mock.calls[1][1]).toMatchObject({ phuongXa: 'Phường Hà Đông', quanHuyen: '' })
    w.unmount()
  })

  it('lỗi hiện ngay dưới từng ô: thiếu tên / SĐT sai đầu số / chưa chọn tỉnh, phường / thiếu địa chỉ cụ thể; không gọi API', async () => {
    const w = await mocModal([])
    await nutTheoChu('Thêm địa chỉ mới').trigger('click')
    await flushPromises()
    await body().find('#kh-dc-ten').setValue('')
    await body().find('#kh-dc-sdt').setValue('0212345678') // đầu số 02 không phải di động
    await nutTheoChu('Lưu địa chỉ').trigger('click')
    await flushPromises()
    const loi = (id) => body().find(`#${id}`).text()
    expect(loi('kh-dc-ten-msg')).toBe('Vui lòng nhập tên người nhận.')
    expect(loi('kh-dc-sdt-msg')).toContain('bắt đầu bằng 03, 05, 07, 08 hoặc 09')
    expect(loi('kh-dc-tinh-msg')).toBe('Vui lòng chọn Tỉnh/Thành phố.')
    expect(loi('kh-dc-phuong-msg')).toBe('Vui lòng chọn Phường/Xã.')
    expect(loi('kh-dc-chi-tiet-msg')).toContain('địa chỉ cụ thể')
    expect(body().find('#kh-dc-sdt').classes()).toContain('is-invalid')
    expect(api.post).not.toHaveBeenCalled()
    w.unmount()
  })

  it('tên người nhận có chữ số bị báo lỗi; sửa lại đúng thì lỗi ô đó biến mất ngay', async () => {
    const w = await mocModal([])
    await nutTheoChu('Thêm địa chỉ mới').trigger('click')
    await flushPromises()
    await body().find('#kh-dc-ten').setValue('Lan 123')
    await body().find('#kh-dc-ten').trigger('blur')
    expect(body().find('#kh-dc-ten-msg').text()).toContain('chỉ gồm chữ cái')
    await body().find('#kh-dc-ten').setValue('Lan Anh')
    expect(body().find('#kh-dc-ten-msg').exists()).toBe(false)
    w.unmount()
  })

  it('backend trả lỗi theo ô (400) thì gắn xuống đúng ô, không hiện thông báo chung', async () => {
    const w = await mocModal([])
    await nutTheoChu('Thêm địa chỉ mới').trigger('click')
    await flushPromises()
    await datSelect2(body(), '#kh-dc-tinh', 'Thành phố Hà Nội')
    await flushPromises()
    await datSelect2(body(), '#kh-dc-phuong', 'Phường Hà Đông')
    await body().find('#kh-dc-chi-tiet').setValue('Số 9 Bạch Đằng')
    api.post.mockRejectedValue(Object.assign(new Error('Họ tên từ 2 đến 100 ký tự.'), { loiTheoTruong: { tenNguoiNhan: 'Họ tên từ 2 đến 100 ký tự.' } }))
    await nutTheoChu('Lưu địa chỉ').trigger('click')
    await flushPromises()
    expect(body().find('#kh-dc-ten-msg').text()).toBe('Họ tên từ 2 đến 100 ký tự.')
    w.unmount()
  })
})

describe('Cửa sổ địa chỉ khách hàng: nút dạng icon', () => {
  const DC = { id: 1, tenNguoiNhan: 'Nguyễn Thị Lan', sdtNguoiNhan: '0901000001', tinhThanhPho: 'Thành phố Hà Nội', quanHuyen: '', phuongXa: 'Phường Hà Đông', diaChiCuThe: 'Số 12 Nguyễn Trãi', laDiaChiMacDinh: true }
  const mo = () => mocModal([DC])

  it('không còn chữ Xóa / Sửa trên thẻ địa chỉ, có 3 icon: Xem chi tiết, Sửa, Xóa', async () => {
    const w = await mo()
    const chu = body().findAll('button').map((b) => b.text())
    expect(chu).not.toContain('Xóa')
    expect(chu).not.toContain('Sửa')
    expect(nutTheoTitle('Xem chi tiết').find('i.bi-eye').exists()).toBe(true)
    expect(nutTheoTitle('Sửa địa chỉ').find('i.bi-pencil-square').exists()).toBe(true)
    expect(nutTheoTitle('Xóa địa chỉ').find('i.bi-trash').exists()).toBe(true)
    w.unmount()
  })

  it('bấm icon Xem: hiện chi tiết chỉ đọc (không có ô nhập), Quay lại về danh sách', async () => {
    const w = await mo()
    await nutTheoTitle('Xem chi tiết').trigger('click')
    const xem = body().find('[data-testid="xem-dia-chi"]')
    expect(xem.exists()).toBe(true)
    expect(xem.text()).toContain('Nguyễn Thị Lan')
    expect(xem.text()).toContain('0901000001')
    expect(xem.text()).toContain('Phường Hà Đông')
    expect(xem.text()).toContain('Số 12 Nguyễn Trãi, Phường Hà Đông, Thành phố Hà Nội')
    expect(xem.find('input').exists()).toBe(false)
    await xem.findAll('button').find((b) => b.text() === 'Quay lại').trigger('click')
    expect(body().find('[data-testid="xem-dia-chi"]').exists()).toBe(false)
    expect(nutTheoTitle('Xem chi tiết').exists()).toBe(true)
    w.unmount()
  })

  it('icon Xóa vẫn hỏi xác nhận rồi gọi API xóa', async () => {
    const w = await mo()
    api.delete.mockResolvedValue({})
    await nutTheoTitle('Xóa địa chỉ').trigger('click')
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
  const DEM = { tatCa: 12, 0: 1, 1: 3, 2: 0, 3: 2, 4: 1, 5: 1, 6: 3, 7: 1, 8: 2 }
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
    // chỉ còn ô chọn Loại đơn (Select2), không có <select> nào dành cho số dòng mỗi trang
    expect(w.findAll('select')).toHaveLength(0)
    expect(w.findAll('.s2-trigger').map((s) => s.attributes('id'))).toEqual(['hd-loai'])
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
  const DEM = { tatCa: 12, 0: 1, 1: 2, 2: 1, 3: 3, 4: 1, 5: 1, 6: 2, 7: 1, 8: 2 }

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

  it('10 tab trạng thái, mỗi tab có SỐ hóa đơn, có tab Giao hàng thất bại (cô: "giao hàng thất bại chưa thể hiện") và tab Hoàn tiền', async () => {
    moApi()
    const w = await moTrang()
    const tabs = w.findAll('.ad-status-tab')
    expect(tabs.map((t) => t.text().replace(/\s+/g, ' '))).toEqual([
      'Tất cả 12', 'Chờ xác nhận 2', 'Đã xác nhận 1', 'Chờ giao hàng 3', 'Đang giao hàng 1', 'Giao hàng thất bại 2', 'Đã giao hàng 1', 'Đã hoàn thành 2', 'Đã hủy 1', 'Hoàn tiền 1',
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

  it('bấm tab "Giao hàng thất bại" gọi API với trangThai = 8; dòng đơn thất bại hiện nhãn cam', async () => {
    moApi()
    const w = await moTrang()
    api.get.mockClear()
    moApi([{ ...HD[1], id: 3, maHoaDon: 'HD003', trangThai: 8 }], 1)
    await w.findAll('.ad-status-tab').find((t) => t.text().includes('Giao hàng thất bại')).trigger('click')
    await flushPromises()
    expect(goiDanhSach().at(-1)[1].params).toMatchObject({ page: 0, trangThai: 8 })
    const nhan = w.find('tbody .ad-pill')
    expect(nhan.text()).toBe('Giao hàng thất bại')
    expect(nhan.classes()).toContain('ad-pill-orange')
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

  it('Từ ngày / Đến ngày mặc định là ngày hiện tại; Đặt lại bộ lọc đưa về hôm nay; xóa trống thì xem mọi ngày', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 13, 9, 0, 0)) // 13/10/2026 theo giờ máy
    try {
      moApi()
      const w = await moTrang()
      expect(w.find('#hd-tu-ngay').element.value).toBe('13/10/2026')
      expect(w.find('#hd-den-ngay').element.value).toBe('13/10/2026')
      expect(goiDanhSach()[0][1].params).toMatchObject({ tuNgay: '2026-10-13', denNgay: '2026-10-13' })
      const dem = api.get.mock.calls.find((c) => c[0] === '/hoa-don/dem-trang-thai')[1].params
      expect(dem).toMatchObject({ tuNgay: '2026-10-13', denNgay: '2026-10-13' }) // số trên tab cũng theo hôm nay

      await w.find('#hd-tu-ngay').setValue('01/10/2026'); await flushPromises()
      expect(goiDanhSach().at(-1)[1].params.tuNgay).toBe('2026-10-01')
      await w.findAll('button').find((b) => b.text().includes('Đặt lại bộ lọc')).trigger('click'); await flushPromises()
      expect(w.find('#hd-tu-ngay').element.value).toBe('13/10/2026')
      expect(goiDanhSach().at(-1)[1].params).toMatchObject({ tuNgay: '2026-10-13', denNgay: '2026-10-13' })

      await w.find('#hd-tu-ngay').setValue(''); await w.find('#hd-den-ngay').setValue(''); await flushPromises()
      expect(goiDanhSach().at(-1)[1].params).toMatchObject({ tuNgay: null, denNgay: null })
      w.unmount()
    } finally {
      vi.useRealTimers()
    }
  })

  it('ngày hiện tại cập nhật liên tục: để trang mở qua 0h thì Từ ngày / Đến ngày tự sang ngày mới và tải lại', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
    vi.setSystemTime(new Date(2026, 9, 13, 23, 59, 40))
    try {
      moApi()
      const w = await moTrang()
      expect(w.find('#hd-tu-ngay').element.value).toBe('13/10/2026')
      const truoc = goiDanhSach().length
      vi.advanceTimersByTime(30 * 1000) // 23:59:40 -> 00:00:10 ngày 14/10
      await flushPromises()
      expect(w.find('#hd-tu-ngay').element.value).toBe('14/10/2026')
      expect(w.find('#hd-den-ngay').element.value).toBe('14/10/2026')
      expect(goiDanhSach().length).toBe(truoc + 1)
      expect(goiDanhSach().at(-1)[1].params).toMatchObject({ tuNgay: '2026-10-14', denNgay: '2026-10-14', page: 0 })
      // cùng ngày thì không tải lại thừa
      vi.advanceTimersByTime(5 * 60 * 1000); await flushPromises()
      expect(goiDanhSach().length).toBe(truoc + 1)
      w.unmount()
    } finally {
      vi.useRealTimers()
    }
  })

  it('qua ngày mới: ô người dùng đã tự chọn giữ nguyên, ô đang là hôm nay mới nhảy; xóa trống thì không tự điền lại', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
    vi.setSystemTime(new Date(2026, 9, 13, 23, 59, 40))
    try {
      moApi()
      const w = await moTrang()
      await w.find('#hd-tu-ngay').setValue('01/10/2026'); await flushPromises()
      vi.advanceTimersByTime(30 * 1000); await flushPromises()
      expect(w.find('#hd-tu-ngay').element.value).toBe('01/10/2026')
      expect(w.find('#hd-den-ngay').element.value).toBe('14/10/2026')
      expect(goiDanhSach().at(-1)[1].params).toMatchObject({ tuNgay: '2026-10-01', denNgay: '2026-10-14' })

      await w.find('#hd-tu-ngay').setValue(''); await w.find('#hd-den-ngay').setValue(''); await flushPromises()
      const truoc = goiDanhSach().length
      vi.setSystemTime(new Date(2026, 9, 15, 8, 0, 0))
      vi.advanceTimersByTime(30 * 1000); await flushPromises()
      expect(w.find('#hd-tu-ngay').element.value).toBe('')
      expect(w.find('#hd-den-ngay').element.value).toBe('')
      expect(goiDanhSach().length).toBe(truoc)
      // Đặt lại bộ lọc lấy đúng ngày hiện tại lúc bấm
      await w.findAll('button').find((b) => b.text().includes('Đặt lại bộ lọc')).trigger('click'); await flushPromises()
      expect(w.find('#hd-tu-ngay').element.value).toBe('15/10/2026')
      expect(w.find('#hd-den-ngay').element.value).toBe('15/10/2026')
      w.unmount()
    } finally {
      vi.useRealTimers()
    }
  })

  it('máy ngủ qua đêm rồi quay lại tab: kiểm tra ngày ngay khi quay lại, không chờ đồng hồ', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
    vi.setSystemTime(new Date(2026, 9, 13, 22, 0, 0))
    try {
      moApi()
      const w = await moTrang()
      const truoc = goiDanhSach().length
      vi.setSystemTime(new Date(2026, 9, 14, 7, 30, 0)) // đồng hồ không kịp chạy (máy ngủ)
      window.dispatchEvent(new Event('focus')); await flushPromises()
      expect(w.find('#hd-tu-ngay').element.value).toBe('14/10/2026')
      expect(w.find('#hd-den-ngay').element.value).toBe('14/10/2026')
      expect(goiDanhSach().length).toBe(truoc + 1) // chỉ tải một lần (không gọi thêm load lặng lẽ)
      expect(goiDanhSach().at(-1)[1].params).toMatchObject({ tuNgay: '2026-10-14', denNgay: '2026-10-14' })
      w.unmount()
    } finally {
      vi.useRealTimers()
    }
  })

  it('rời trang thì dừng đồng hồ kiểm tra ngày (qua 0h không gọi API nữa)', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
    vi.setSystemTime(new Date(2026, 9, 13, 23, 59, 40))
    try {
      moApi()
      const w = await moTrang()
      w.unmount()
      const truoc = api.get.mock.calls.length
      expect(vi.getTimerCount()).toBe(0)
      vi.advanceTimersByTime(60 * 1000); await flushPromises()
      expect(api.get.mock.calls.length).toBe(truoc)
    } finally {
      vi.useRealTimers()
    }
  })

  it('đổi loại đơn thì tự lọc, không cần bấm nút tìm', async () => {
    moApi()
    const w = await moTrang()
    await datSelect2(w, '#hd-loai', '2')
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
