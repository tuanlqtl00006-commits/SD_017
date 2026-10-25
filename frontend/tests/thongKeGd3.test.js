import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn() } }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {} }),
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))

import ThongKe from '../src/components/ThongKe.vue'
import { todayIso } from '../src/utils/format'
import { congNgay, gioLam, khoangGuiMayChu, khungGioHienThi, phanTram, tyLeCoMat, tyLeDungGio } from '../src/utils/thongKe'

const GD3 = {
  tuNgay: '2026-09-17', denNgay: '2026-10-16', nguongSapHet: 5, soSapHet: 2, soHetHang: 1,
  sanPhamBanChay: [
    { idSanPham: 1, maSanPham: 'SP01', tenSanPham: 'Yonex Astrox 99 Pro', thuongHieu: 'Yonex', soLuong: 12, doanhThu: 46332000, soDon: 10, phanLoaiBanChay: 'Đen · 4U · G5' },
    { idSanPham: 7, maSanPham: 'SP07', tenSanPham: 'Ống cầu Thành Công', thuongHieu: 'Thành Công', soLuong: 9, doanhThu: 2250000, soDon: 6, phanLoaiBanChay: 'Trắng' },
  ],
  sapHetHang: [
    { id: 5, ma: 'SP01-VANG-4U-G6', idSanPham: 1, tenSanPham: 'Yonex Astrox 99 Pro', phanLoai: 'Vàng · 4U · G6', tonKho: 0 },
    { id: 9, ma: 'SP03-DEN-4U-G5', idSanPham: 3, tenSanPham: 'Victor Thruster K 9900', phanLoai: 'Đen · 4U · G5', tonKho: 4 },
  ],
  theoPhuongThuc: [{ ma: 'PT01', ten: 'Tiền mặt', soTien: 3000000, soDon: 3 }, { ma: 'PT02', ten: 'Chuyển khoản ngân hàng', soTien: 1000000, soDon: 1 }],
  theoGio: Array.from({ length: 24 }, (_, gio) => ({ gio, soDon: gio === 19 ? 6 : gio === 10 ? 2 : 0, doanhThu: gio === 19 ? 9000000 : 0 })),
  theoCa: [{ tenCa: 'Ca sáng', gio: '07:00 - 12:00', soDon: 2, doanhThu: 3000000 }, { tenCa: 'Ca tối', gio: '17:00 - 22:00', soDon: 6, doanhThu: 9000000 }],
  chuyenCan: [{ idNhanVien: 3, maNhanVien: 'NV03', hoTen: 'Lê Quốc Bảo', soCaLich: 10, soCaDaLam: 8, soCaVang: 2, soCaChuaToi: 0, soLanDiMuon: 2, tongPhutMuon: 55, soCaNgoaiLich: 1, soPhutLam: 2430 }],
  giaoCa: { soCaDaGiao: 12, soCaLech: 1, tongThieu: 50000, tongThua: 0, caLech: [{ id: 3, maGiaoCa: 'GC03', tenNhanVien: 'Lê Quốc Bảo', thoiGianKetCa: '2026-10-15T12:05:00', chenhLech: -50000, ghiChu: 'Thối nhầm' }] },
}

function moTrang(gd3 = GD3) {
  api.get.mockImplementation(async (url) => {
    if (url === '/thong-ke') return { data: gd3 }
    return { data: { content: [] } }
  })
  return mount(ThongKe, { attachTo: document.body })
}

beforeEach(() => {
  document.body.innerHTML = ''
  api.get.mockReset()
})

describe('utils thống kê giai đoạn 3', () => {
  it('khoảng gửi máy chủ: "Tất cả" đổi thành 1 năm gần nhất', () => {
    expect(khoangGuiMayChu('2026-10-01', '2026-10-16', '2026-10-16')).toEqual({ tuNgay: '2026-10-01', denNgay: '2026-10-16', catBot: false })
    expect(khoangGuiMayChu('', '', '2026-10-16')).toEqual({ tuNgay: '2025-10-16', denNgay: '2026-10-16', catBot: true })
    expect(khoangGuiMayChu('2020-01-01', '2026-10-16', '2026-10-16').catBot).toBe(true)
    expect(khoangGuiMayChu('', '2026-05-01', '2026-10-16').denNgay).toBe('2026-05-01')
  })

  it('phần trăm, giờ làm, tỷ lệ có mặt / đúng giờ', () => {
    expect(phanTram(1, 3)).toBe(33.3)
    expect(phanTram(5, 0)).toBe(0)
    expect(gioLam(570)).toBe('9,5 giờ')
    expect(gioLam(45)).toBe('45 phút')
    expect(gioLam(0)).toBe('0 giờ')
    expect(tyLeDungGio({ soCaDaLam: 8, soLanDiMuon: 2 })).toBe(75)
    expect(tyLeDungGio({ soCaDaLam: 0 })).toBeNull()
    expect(tyLeCoMat({ soCaDaLam: 8, soCaVang: 2 })).toBe(80)
    expect(tyLeCoMat({ soCaDaLam: 0, soCaVang: 0 })).toBeNull()
  })

  it('khung giờ hiển thị: tối thiểu 8h - 21h, mở rộng nếu có đơn ngoài khoảng đó', () => {
    const gio = Array.from({ length: 24 }, (_, g) => ({ gio: g, soDon: 0 }))
    expect(khungGioHienThi(gio).map((g) => g.gio)).toEqual(Array.from({ length: 14 }, (_, i) => i + 8))
    gio[6].soDon = 1
    gio[23].soDon = 2
    const kq = khungGioHienThi(gio)
    expect(kq[0].gio).toBe(6)
    expect(kq[kq.length - 1].gio).toBe(23)
  })
})

describe('Trang thống kê: phần giai đoạn 3', () => {
  it('gọi /thong-ke theo khoảng 30 ngày và hiện đủ các mục', async () => {
    const w = moTrang()
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/thong-ke', { params: { tuNgay: congNgay(todayIso(), -29), denNgay: todayIso() } })
    const banChay = w.find('[data-test="tk-ban-chay"]').text()
    expect(banChay).toContain('Yonex Astrox 99 Pro')
    expect(banChay).toContain('Đen · 4U · G5')
    expect(banChay).toContain('46.332.000')
    const sapHet = w.find('[data-test="tk-sap-het"]').text()
    expect(sapHet).toContain('2 phân loại còn ≤ 5')
    expect(sapHet).toContain('1 hết hàng')
    expect(sapHet).toContain('Hết hàng')
    expect(sapHet).toContain('Còn 4')
    expect(w.find('[data-test="tk-phuong-thuc"]').text()).toContain('Tiền mặt')
    expect(w.find('[data-test="tk-phuong-thuc"]').text()).toContain('75%')
    expect(w.find('[data-test="tk-theo-ca"]').text()).toContain('Ca tối (17:00 - 22:00)')
    expect(w.find('[data-test="tk-theo-gio"]').text()).toContain('Đông nhất 19h - 20h (6 đơn)')
    const cc = w.find('[data-test="tk-chuyen-can"]').text()
    expect(cc).toContain('Lê Quốc Bảo')
    expect(cc).toContain('55 phút')
    expect(cc).toContain('40,5 giờ')
    expect(cc).toContain('80%')
    expect(cc).toContain('75%')
    const gc = w.find('[data-test="tk-giao-ca"]').text()
    expect(gc).toContain('GC03 · Lê Quốc Bảo')
    expect(gc).toContain('Thối nhầm')
    expect(gc).toContain('-50.000')
    w.unmount()
  })

  it('đổi khoảng thời gian thì tải lại; chọn "Tất cả" thì gửi 1 năm gần nhất và ghi chú', async () => {
    const w = moTrang()
    await flushPromises()
    api.get.mockClear()
    const nut = w.findAll('button').find((b) => b.text() === 'Tất cả')
    await nut.trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/thong-ke', { params: { tuNgay: congNgay(todayIso(), -365), denNgay: todayIso() } })
    expect(w.text()).toContain('Các mục bên dưới tính trong 1 năm gần nhất')
    w.unmount()
  })

  it('không có dữ liệu / lỗi tải: báo trong từng mục, trang vẫn hiện', async () => {
    let w = moTrang({})
    await flushPromises()
    expect(w.find('[data-test="tk-ban-chay"]').text()).toContain('Chưa bán được sản phẩm nào')
    expect(w.find('[data-test="tk-sap-het"]').text()).toContain('Không có sản phẩm nào sắp hết')
    expect(w.find('[data-test="tk-giao-ca"]').text()).toContain('Không có ca nào lệch tiền')
    w.unmount()
    api.get.mockImplementation(async (url) => {
      if (url === '/thong-ke') throw new Error('Bạn không có quyền xem thống kê.')
      return { data: { content: [] } }
    })
    w = mount(ThongKe, { attachTo: document.body })
    await flushPromises()
    expect(w.find('[data-test="tk-loi-gd3"]').text()).toBe('Bạn không có quyền xem thống kê.')
    expect(w.text()).toContain('Tổng quan cửa hàng')
    w.unmount()
  })
})
