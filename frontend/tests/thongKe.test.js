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
import { addDaysIso } from '../src/utils/format'
import {
  congNgay, donGanNhat, gomCot, kyTruoc, lamTronTran, locChiTietHoanThanh, locTheoKhoang, nhanTienGon, phanTramThayDoi,
  soNgay, theoDanhMuc, theoLoaiDon, theoThuongHieu, tinhKhoang, tongHop, tongHopUuDai, tongKetMocThoiGian, topSanPhamBanChay, xepHang,
} from '../src/utils/thongKe'

const hd = (id, ngay, tien, tt = 6, extra = {}) => ({
  id, maHoaDon: `HD${String(id).padStart(2, '0')}`, ngayTao: `${ngay}T09:30:00`, tongTien: tien, trangThai: tt,
  loaiDon: 'Tại quầy', tenKhachHang: null, tenNhanVien: 'Lê Quốc Tuấn', ...extra,
})

describe('Thống kê: khoảng ngày', () => {
  it('cộng ngày, đếm ngày tính cả hai đầu, qua tháng / năm', () => {
    expect(congNgay('2026-10-11', -29)).toBe('2026-09-12')
    expect(congNgay('2026-01-01', -1)).toBe('2025-12-31')
    expect(congNgay('2026-02-27', 2)).toBe('2026-03-01')
    expect(soNgay('2026-10-01', '2026-10-07')).toBe(7)
    expect(soNgay('2026-10-05', '2026-10-05')).toBe(1)
  })
  it('tinhKhoang: N ngày gần nhất (tính cả hôm nay), tất cả, tự chọn (đảo nếu nhập ngược)', () => {
    expect(tinhKhoang(7, '', '', '2026-10-11')).toEqual({ tu: '2026-10-05', den: '2026-10-11' })
    expect(tinhKhoang(30, '', '', '2026-10-11')).toEqual({ tu: '2026-09-12', den: '2026-10-11' })
    expect(tinhKhoang(0, '', '', '2026-10-11')).toEqual({ tu: '', den: '' })
    expect(tinhKhoang(-1, '2026-10-01', '2026-10-05', 'x')).toEqual({ tu: '2026-10-01', den: '2026-10-05' })
    expect(tinhKhoang(-1, '2026-10-09', '2026-10-02', 'x')).toEqual({ tu: '2026-10-02', den: '2026-10-09' })
    expect(tinhKhoang(-1, '2026-10-09', '', 'x')).toEqual({ tu: '2026-10-09', den: '' })
  })
  it('locTheoKhoang: lấy cả ngày đầu và ngày cuối, bỏ đơn không có ngày', () => {
    const ds = [hd(1, '2026-10-01', 1), hd(2, '2026-10-05', 1), hd(3, '2026-10-06', 1), { id: 4, ngayTao: null }]
    expect(locTheoKhoang(ds, '2026-10-01', '2026-10-05').map((h) => h.id)).toEqual([1, 2])
    expect(locTheoKhoang(ds, '', '').map((h) => h.id)).toEqual([1, 2, 3])
    expect(locTheoKhoang(null, '', '')).toEqual([])
  })
  it('kyTruoc: cùng độ dài, liền trước; thiếu một đầu thì không so sánh', () => {
    expect(kyTruoc('2026-10-05', '2026-10-11')).toEqual({ tu: '2026-09-28', den: '2026-10-04' })
    expect(kyTruoc('2026-10-01', '2026-10-01')).toEqual({ tu: '2026-09-30', den: '2026-09-30' })
    expect(kyTruoc('', '2026-10-11')).toBeNull()
    expect(kyTruoc('2026-10-01', '')).toBeNull()
  })
})

describe('Thống kê: tổng hợp và so sánh', () => {
  const ds = [hd(1, '2026-10-01', 1000000), hd(2, '2026-10-02', 3000000), hd(3, '2026-10-02', 500000, 0), hd(4, '2026-10-03', 700000, 2)]
  it('tongHop: doanh thu chỉ tính đơn hoàn thành, tỷ lệ hoàn thành / hủy', () => {
    const t = tongHop(ds)
    expect(t).toMatchObject({ tongDon: 4, hoanThanh: 2, huy: 1, doanhThu: 4000000, trungBinh: 2000000 })
    expect(t.tyLeHoanThanh).toBe(50)
    expect(t.tyLeHuy).toBe(25)
  })
  it('tongHop: rỗng không chia cho 0', () => {
    expect(tongHop([])).toMatchObject({ tongDon: 0, doanhThu: 0, trungBinh: 0, tyLeHoanThanh: 0, tyLeHuy: 0 })
    expect(tongHop(undefined).tongDon).toBe(0)
  })
  it('phanTramThayDoi', () => {
    expect(phanTramThayDoi(150, 100)).toBe(50)
    expect(phanTramThayDoi(50, 100)).toBe(-50)
    expect(phanTramThayDoi(0, 0)).toBe(0)
    expect(phanTramThayDoi(10, 0)).toBeNull()
  })
})

describe('Thống kê: biểu đồ cột', () => {
  it('<= 31 ngày: cột theo NGÀY, đủ mọi ngày kể cả ngày không có đơn; đơn hủy / chưa xong không tính', () => {
    const ds = [hd(1, '2026-10-01', 1000000), hd(2, '2026-10-03', 2000000), hd(3, '2026-10-03', 500000), hd(4, '2026-10-02', 9e6, 0), hd(5, '2026-10-02', 9e6, 3)]
    const { donVi, cot } = gomCot(ds, '2026-10-01', '2026-10-05', '2026-10-05')
    expect(donVi).toBe('ngày')
    expect(cot.map((c) => c.v)).toEqual([1000000, 0, 2500000, 0, 0])
    expect(cot.map((c) => c.so)).toEqual([1, 0, 2, 0, 0])
    expect(cot[0].nhan).toBe('01/10')
  })
  it('đúng 31 ngày vẫn theo ngày, 32 ngày chuyển sang TUẦN', () => {
    expect(gomCot([], '2026-10-01', '2026-10-31', 'x').donVi).toBe('ngày')
    expect(gomCot([], '2026-10-01', '2026-11-01', 'x').donVi).toBe('tuần')
  })
  it('90 ngày: theo TUẦN, tổng các cột bằng tổng doanh thu, đơn nằm đúng tuần', () => {
    const ds = [hd(1, '2026-08-14', 100), hd(2, '2026-08-20', 200), hd(3, '2026-10-11', 400)]
    const { donVi, cot } = gomCot(ds, '2026-08-14', '2026-10-11', '2026-10-11')
    expect(donVi).toBe('tuần')
    expect(cot.length).toBe(Math.ceil(soNgay('2026-08-14', '2026-10-11') / 7))
    expect(cot.reduce((s, c) => s + c.v, 0)).toBe(700)
    expect(cot[0].v).toBe(300) // 14/08 - 20/08 cùng tuần đầu
    expect(cot.at(-1).v).toBe(400)
    expect(cot.at(-1).den).toBe('2026-10-11') // tuần cuối bị cắt ở ngày cuối khoảng
  })
  it('"Tất cả" dài hơn nửa năm: theo THÁNG, từ tháng có đơn đầu tiên tới nay', () => {
    const ds = [hd(1, '2025-12-20', 100), hd(2, '2026-03-05', 200), hd(3, '2026-10-02', 300)]
    const { donVi, cot } = gomCot(ds, '', '', '2026-10-11')
    expect(donVi).toBe('tháng')
    expect(cot[0].nhan).toBe('12/2025')
    expect(cot.at(-1).nhan).toBe('10/2026')
    expect(cot.length).toBe(11)
    expect(cot.reduce((s, c) => s + c.v, 0)).toBe(600)
  })
  it('không có đơn nào: vẫn có khung (từ hôm nay) và không lỗi', () => {
    expect(gomCot([], '', '', '2026-10-11').cot.length).toBe(1)
    expect(gomCot([], '2026-10-09', '2026-10-01', 'x').cot).toEqual([])
  })
  it('lamTronTran và nhanTienGon', () => {
    expect(lamTronTran(0)).toBe(1)
    expect(lamTronTran(850000)).toBe(1000000)
    expect(lamTronTran(1200000)).toBe(2000000)
    expect(lamTronTran(3400000)).toBe(5000000)
    expect(lamTronTran(7000000)).toBe(10000000)
    expect(nhanTienGon(0)).toBe('0')
    expect(nhanTienGon(500000)).toBe('500k')
    expect(nhanTienGon(2500000)).toBe('2,5tr')
    expect(nhanTienGon(1200000000)).toBe('1,2 tỷ')
  })
})

describe('Thống kê: xếp hạng', () => {
  const ds = [
    hd(1, '2026-10-01', 1000000, 6, { tenNhanVien: 'An', tenKhachHang: 'Khách A' }),
    hd(2, '2026-10-02', 3000000, 6, { tenNhanVien: 'Bình', tenKhachHang: 'Khách B' }),
    hd(3, '2026-10-03', 500000, 6, { tenNhanVien: 'An', tenKhachHang: 'Khách A' }),
    hd(4, '2026-10-04', 9000000, 0, { tenNhanVien: 'Bình' }), // hủy: không tính
    hd(5, '2026-10-05', 800000, 6, { tenNhanVien: ' ', tenKhachHang: null }), // không tên: bỏ qua
  ]
  it('xepHang theo doanh thu giảm dần, gom đúng số đơn, bỏ đơn hủy / không tên', () => {
    expect(xepHang(ds, (h) => h.tenNhanVien)).toEqual([{ ten: 'Bình', so: 1, v: 3000000 }, { ten: 'An', so: 2, v: 1500000 }])
    expect(xepHang(ds, (h) => h.tenKhachHang)).toEqual([{ ten: 'Khách B', so: 1, v: 3000000 }, { ten: 'Khách A', so: 2, v: 1500000 }])
  })
  it('giới hạn số dòng', () => {
    expect(xepHang(ds, (h) => h.tenNhanVien, 1).length).toBe(1)
  })
  it('theoLoaiDon: tỷ trọng cộng lại 100%', () => {
    const r = theoLoaiDon([hd(1, '2026-10-01', 300, 6, { loaiDon: 'Online' }), hd(2, '2026-10-01', 100, 6), hd(3, '2026-10-01', 999, 0)])
    expect(r.map((x) => x.ten)).toEqual(['Online', 'Tại quầy'])
    expect(r.map((x) => x.pct)).toEqual([75, 25])
  })
  it('donGanNhat: mới nhất trước, đồng ngày thì id lớn trước', () => {
    const r = donGanNhat([hd(1, '2026-10-01', 1), hd(2, '2026-10-03', 1), hd(3, '2026-10-03', 1)], 2)
    expect(r.map((h) => h.id)).toEqual([3, 2])
  })
})

/* ---------- Trang Thống kê ---------- */
const homNay = addDaysIso(0)
const ngay = (n) => addDaysIso(-n)

async function moTrang(content) {
  api.get.mockResolvedValue({ data: { content } })
  const w = mount(ThongKe, { attachTo: document.body })
  await flushPromises()
  return w
}
const kpiText = (w) => w.findAll('[data-test="tk-kpi"]').map((e) => e.text())

beforeEach(() => {
  document.body.innerHTML = ''
  api.get.mockReset()
})

describe('Trang Thống kê', () => {
  it('hiện 4 chỉ số, doanh thu chỉ tính đơn hoàn thành trong 30 ngày', async () => {
    const w = await moTrang([
      hd(1, ngay(1), 2000000), hd(2, ngay(2), 1000000), hd(3, ngay(3), 5000000, 0), hd(4, ngay(60), 7000000),
    ])
    const k = kpiText(w)
    expect(k).toHaveLength(4)
    expect(k[0]).toContain('Doanh thu thực tế')
    expect(k[0]).toContain('3.000.000')
    expect(k[1]).toContain('Doanh thu dự kiến')
    expect(k[1]).toContain('3.000.000')
    expect(k[2]).toMatch(/Đơn hoàn thành\s*2\/3/)
    expect(k[3]).toMatch(/Đơn đã hủy\s*1/)
    w.unmount()
  })

  it('so sánh với kỳ trước: tăng thì ▲ xanh, đơn hủy tăng thì tô đỏ', async () => {
    const w = await moTrang([
      hd(1, ngay(2), 3000000), hd(2, ngay(3), 1000000, 0), hd(3, ngay(4), 1000000, 0), // kỳ này: 3tr, 2 đơn hủy
      hd(4, ngay(40), 2000000), hd(5, ngay(41), 1000000, 0), // kỳ trước (30 ngày liền trước): 2tr, 1 đơn hủy
    ])
    const [dt, , , huy] = w.findAll('[data-test="tk-kpi"]')
    expect(dt.find('.tk-so').text()).toContain('▲ 50%')
    expect(dt.find('.tk-so').classes()).toContain('tk-tot')
    expect(huy.find('.tk-so').text()).toContain('▲ 100%')
    expect(huy.find('.tk-so').classes()).toContain('tk-xau')
    w.unmount()
  })

  it('"Tất cả" không có kỳ trước nên không hiện so sánh', async () => {
    const w = await moTrang([hd(1, ngay(2), 3000000)])
    await w.findAll('button').find((b) => b.text() === 'Tất cả').trigger('click')
    expect(w.findAll('.tk-so')).toHaveLength(0)
    expect(w.get('[data-test="tk-mo-ta"]').text()).toContain('Toàn bộ thời gian')
    w.unmount()
  })

  it('đổi sang 7 ngày thì chỉ còn đơn trong 7 ngày', async () => {
    const w = await moTrang([hd(1, ngay(2), 1000000), hd(2, ngay(20), 4000000)])
    expect(kpiText(w)[0]).toContain('5.000.000')
    await w.findAll('button').find((b) => b.text() === '7 ngày').trigger('click')
    expect(kpiText(w)[0]).toContain('1.000.000')
    expect(kpiText(w)[0]).not.toContain('5.000.000')
    w.unmount()
  })

  it('Tự chọn: có ô từ ngày / đến ngày và lọc đúng khoảng', async () => {
    const w = await moTrang([hd(1, ngay(5), 1000000), hd(2, ngay(10), 4000000)])
    expect(w.find('[data-test="tk-tu-chon"]').exists()).toBe(false)
    await w.findAll('button').find((b) => b.text() === 'Tự chọn').trigger('click')
    await w.get('#tk-tu').setValue(ngay(12))
    await w.get('#tk-den').setValue(ngay(8))
    expect(kpiText(w)[0]).toContain('4.000.000')
    expect(kpiText(w)[0]).not.toContain('5.000.000')
    // nhập ngược (từ > đến) thì tự đảo, không bị rỗng
    await w.get('#tk-tu').setValue(ngay(1))
    await w.get('#tk-den').setValue(ngay(6))
    expect(kpiText(w)[0]).toContain('1.000.000')
    w.unmount()
  })

  it('doanh thu dự kiến gồm đơn đang xử lý / đang giao, bỏ đơn hủy và hóa đơn chờ tại quầy; có Hôm nay, bỏ bảng đơn gần đây', async () => {
    const w = await moTrang([
      hd(1, ngay(0), 1000000, 6),
      hd(2, ngay(0), 500000, 4, { loaiDon: 'Trực tuyến', doanhThu: 470000 }),
      hd(3, ngay(1), 800000, 0),
      hd(4, ngay(0), 900000, 1), // hóa đơn chờ tại quầy (chưa thanh toán)
    ])
    const k = kpiText(w)
    expect(k[0]).toContain('1.000.000')
    expect(k[1]).toContain('1.470.000')
    expect(k[1]).toContain('Còn 470.000')
    expect(k[2]).toMatch(/Đơn hoàn thành\s*1\/3/)
    await w.findAll('button').find((b) => b.text() === 'Hôm nay').trigger('click')
    expect(kpiText(w)[2]).toMatch(/Đơn hoàn thành\s*1\/2/)
    expect(w.find('[data-test="tk-gan-day"]').exists()).toBe(false)
    expect(w.find('[data-test="tk-xuat-excel"]').exists()).toBe(true)
    expect(w.find('[data-test="tk-so-sanh"]').attributes('href')).toBe('/thong-ke/so-sanh')
    w.unmount()
  })

  it('top nhân viên / khách hàng', async () => {
    const w = await moTrang([
      hd(1, ngay(1), 1000000, 6, { tenNhanVien: 'An', tenKhachHang: 'Khách A' }),
      hd(2, ngay(1), 4000000, 6, { tenNhanVien: 'Bình', tenKhachHang: 'Khách B' }),
    ])
    expect(w.get('[data-test="tk-top-nv"] li').text()).toContain('Bình')
    expect(w.get('[data-test="tk-top-kh"] li').text()).toContain('Khách B')
    w.unmount()
  })

  it('chưa có dữ liệu: các khối báo trống, không lỗi, số liệu về 0', async () => {
    const w = await moTrang([])
    expect(w.text()).toContain('Chưa có đơn hoàn thành trong khoảng này.')
    expect(kpiText(w)[0]).toContain('0 ₫')
    w.unmount()
  })

  it('biểu đồ: 30 ngày có 30 cột; 90 ngày gom theo tuần', async () => {
    const w = await moTrang([hd(1, ngay(2), 1000000), hd(2, ngay(50), 1000000)])
    expect(w.text()).toContain('Doanh thu theo ngày')
    expect(w.findAll('svg rect')).toHaveLength(30)
    await w.findAll('button').find((b) => b.text() === '90 ngày').trigger('click')
    expect(w.text()).toContain('Doanh thu theo tuần')
    expect(w.findAll('svg rect')).toHaveLength(Math.ceil(90 / 7))
    w.unmount()
  })

  it('lỗi tải: báo lỗi và vẫn hiển thị trang', async () => {
    api.get.mockRejectedValue(new Error('Mất kết nối'))
    const w = mount(ThongKe, { attachTo: document.body })
    await flushPromises()
    expect(w.text()).toContain('Tổng quan cửa hàng')
    w.unmount()
  })

  it('validate ngày tự chọn + đối soát dòng tiền + lọc tìm kiếm SP và tồn kho từ /thong-ke/tong-quan', async () => {
    const dsHd = [
      hd(10, ngay(1), 500000, 6, { maHoaDon: 'HD10', tenKhachHang: 'Trần Bình' }),
      hd(11, ngay(1), 800000, 3, { maHoaDon: 'HD11', tenKhachHang: 'Lê Hoa', loaiDon: 'Trực tuyến' }),
      hd(12, ngay(1), 400000, 0, { maHoaDon: 'HD12', tenKhachHang: 'Vũ Nam' }),
    ]
    const tongQuanDb = {
      chiTietHoaDon: [
        { idHoaDon: 10, maHoaDon: 'HD10', ngayTao: `${ngay(1)}T10:00:00`, trangThaiHoaDon: 6, idSanPham: 1, maSanPham: 'SP1', tenSanPham: 'Bóng Futsal Nike', tenThuongHieu: 'Nike', tenDanhMuc: 'Futsal', idBienThe: 1, maBienThe: 'BT1', soLuong: 1, thanhTien: 500000, soLuongTon: 0 },
        { idHoaDon: 10, maHoaDon: 'HD10', ngayTao: `${ngay(1)}T10:00:00`, trangThaiHoaDon: 6, idSanPham: 2, maSanPham: 'SP2', tenSanPham: 'Bóng Adidas Pro', tenThuongHieu: 'Adidas', tenDanhMuc: 'Sân cỏ', idBienThe: 2, maBienThe: 'BT2', soLuong: 2, thanhTien: 900000, soLuongTon: 4 },
      ],
      uuDaiHoaDon: [
        { idHoaDon: 10, maHoaDon: 'HD10', ngayTao: `${ngay(1)}T10:00:00`, trangThai: 6, soTienGiam: 900000, phiVanChuyen: 25000 },
      ],
      tonKhoCanhBao: [
        { idBienThe: 1, maBienThe: 'BT1', idSanPham: 1, tenSanPham: 'Bóng Futsal Nike', soLuongTon: 0, giaBan: 500000, giaSauGiam: 500000, phanTramGiam: 0 },
        { idBienThe: 2, maBienThe: 'BT2', idSanPham: 2, tenSanPham: 'Bóng Adidas Pro', soLuongTon: 4, giaBan: 450000, giaSauGiam: 405000, phanTramGiam: 10 },
      ],
      heThong: {
        tongSanPhamHoatDong: 8, tongBienTheHoatDong: 24, tongTonKho: 320, soBienTheSapHet: 2, soBienTheHetHang: 1,
        tongKhachHangHoatDong: 15, tongNhanVienHoatDong: 4, soDotDangDienRa: 1, soDotSapDienRa: 1, soPhieuDangHoatDong: 2,
        dotDangChay: [{ id: 9, maDot: 'DGG09', tenDot: 'Siêu Sale Tháng 10', phanTramGiamDot: 15, trangThaiText: 'Đang diễn ra' }],
        phieuDangChay: [],
      },
    }
    api.get.mockImplementation((url) => {
      if (url === '/thong-ke/tong-quan') return Promise.resolve({ data: tongQuanDb })
      if (url === '/thong-ke') return Promise.resolve({ data: {} })
      return Promise.resolve({ data: { content: dsHd } })
    })

    const w = mount(ThongKe, { attachTo: document.body })
    await flushPromises()

    expect(w.get('[data-test="tk-thuc-thu"]').text()).toContain('25.000')
    expect(w.get('[data-test="tk-doi-soat"]').text()).toContain('800.000')
    expect(w.get('[data-test="tk-doi-soat"]').text()).toContain('400.000')
    expect(w.get('[data-test="tk-he-thong"]').text()).toContain('8 sản phẩm · 24 biến thể')

    await w.get('[data-test="tk-tim-sp"]').setValue('futsal')
    expect(w.findAll('[data-test="tk-top-sp"] tbody tr')).toHaveLength(1)
    expect(w.get('[data-test="tk-top-sp"] tbody').text()).toContain('Bóng Futsal Nike')

    await w.get('[data-test="tk-loc-het-hang"]').trigger('click')
    expect(w.findAll('[data-test="tk-ton-kho"] tbody tr')).toHaveLength(1)

    await w.findAll('button').find((b) => b.text() === 'Tự chọn').trigger('click')
    await w.get('#tk-tu').setValue(ngay(1))
    await w.get('#tk-den').setValue(ngay(5))
    expect(w.find('[data-test="tk-canh-bao-ngay"]').exists()).toBe(true)
    await w.get('[data-test="tk-chuan-hoa-ngay"]').trigger('click')
    expect(w.find('[data-test="tk-canh-bao-ngay"]').exists()).toBe(false)
    w.unmount()
  })
})
