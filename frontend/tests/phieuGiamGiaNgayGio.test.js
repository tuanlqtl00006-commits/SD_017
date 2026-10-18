// Phiếu giảm giá theo lời cô: ngày GIỜ + "Trạng thái dự kiến", ô Số lượng chỉ ở phiếu công khai, bảng chọn khách có Tổng đơn /
// Mua gần nhất / Ngày sinh / Giới tính + bộ lọc, quy tắc chồng ưu đãi (ví dụ tính tiền + cảnh báo phiếu trùng), lỗi dưới từng ô.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useToast } from '../src/composables/useToast'
import {
  bayGioDayDu,
  cungPhut,
  dinhDangKhoangPhut,
  dinhDangNgayGio,
  giaoNhau,
  kiemTraKhoangThoiGian,
  mocBatDau,
  mocKetThuc,
  soPhutGiua,
  viTriTheoMoc,
} from '../src/utils/ngayGio'
import { ghiChuSinhNhat, locKhach, ngayToiSinhNhat, tenGioiTinh } from '../src/utils/khachChonPhieu'
import { tinhGiamPhieu, viDuDonHang } from '../src/utils/tinhPhieu'
import { chonSelect2, datSelect2, giaTriSelect2 } from './helpersSelect2'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

/** Giả "bây giờ" (chỉ đổi Date, giữ nguyên bộ hẹn giờ). */
const gia = (y, m, d, h = 10, mi = 0, s = 0) => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(y, m - 1, d, h, mi, s))
}

beforeEach(() => {
  document.body.innerHTML = ''
  useToast().toasts.splice(0)
  gia(2026, 10, 15, 10, 0, 0) // 15/10/2026 10:00:00
})
afterEach(() => vi.useRealTimers())

/* ======================= Quy tắc ngày giờ (hàm thuần) ======================= */

describe('Ngày giờ: mốc, vị trí, kiểm tra khoảng thời gian (khớp ThoiGianPhut ở backend)', () => {
  it('mốc bắt đầu thiếu giây = :00, mốc kết thúc thiếu giây = :59 (hết phút), chỉ có ngày = đầu / cuối ngày', () => {
    expect(mocBatDau('2026-10-15T08:30')).toBe('2026-10-15T08:30:00')
    expect(mocKetThuc('2026-10-15T08:30')).toBe('2026-10-15T08:30:59')
    expect(mocKetThuc('2026-10-15T08:30:07')).toBe('2026-10-15T08:30:07')
    expect(mocBatDau('2026-10-15')).toBe('2026-10-15T00:00:00')
    expect(mocKetThuc('2026-10-15')).toBe('2026-10-15T23:59:59')
    expect(mocBatDau('')).toBe('')
    expect(mocKetThuc(null)).toBe('')
  })

  it('"bây giờ" so với khoảng: sắp / đang / kết thúc, chính xác tới giây', () => {
    const bd = '2026-10-15T08:00:00'
    const kt = '2026-10-15T09:00:59'
    expect(viTriTheoMoc(bd, kt, '2026-10-15T07:59:59')).toBe('SAP')
    expect(viTriTheoMoc(bd, kt, '2026-10-15T08:00:00')).toBe('DANG')
    expect(viTriTheoMoc(bd, kt, '2026-10-15T09:00:59')).toBe('DANG')
    expect(viTriTheoMoc(bd, kt, '2026-10-15T09:01:00')).toBe('KET_THUC')
    expect(bayGioDayDu()).toBe('2026-10-15T10:00:00')
  })

  it('cùng phút / khoảng cách phút / giao nhau / định dạng thời lượng', () => {
    expect(cungPhut('2026-10-15T08:00:00', '2026-10-15T08:00')).toBe(true)
    expect(cungPhut('2026-10-15T08:00', '2026-10-15T08:01')).toBe(false)
    expect(soPhutGiua('2026-10-15T08:00', '2026-10-15T09:30')).toBe(90)
    expect(dinhDangKhoangPhut(45)).toBe('45 phút')
    expect(dinhDangKhoangPhut(90)).toBe('1 giờ 30 phút')
    expect(dinhDangKhoangPhut(2 * 1440 + 3 * 60 + 20)).toBe('2 ngày 3 giờ')
    expect(dinhDangKhoangPhut(0)).toBe('dưới 1 phút')
    expect(giaoNhau({ batDau: '2026-10-01T00:00', ketThuc: '2026-10-10T17:00' }, { batDau: '2026-10-10T17:00', ketThuc: '2026-10-20T00:00' })).toBe(true)
    expect(giaoNhau({ batDau: '2026-10-01T00:00', ketThuc: '2026-10-10T17:00' }, { batDau: '2026-10-10T17:01', ketThuc: '2026-10-20T00:00' })).toBe(false)
    expect(dinhDangNgayGio('2026-10-15T08:05:00')).toBe('15/10/2026 08:05')
  })

  const kt = (o) => kiemTraKhoangThoiGian({ bayGio: '2026-10-15T10:00:00', ...o })

  it('thiếu ngày giờ: báo từng ô', () => {
    expect(kt({ batDau: '', ketThuc: '' })).toEqual({ batDau: 'Chọn ngày giờ bắt đầu.', ketThuc: 'Chọn ngày giờ kết thúc.' })
    expect(kt({ batDau: '2026-10-15T10:00', ketThuc: '' })).toEqual({ ketThuc: 'Chọn ngày giờ kết thúc.' })
  })

  it('kết thúc phải sau bắt đầu (cùng phút cũng sai)', () => {
    expect(kt({ batDau: '2026-10-16T10:00', ketThuc: '2026-10-16T10:00' }).ketThuc).toBe('Ngày giờ kết thúc phải sau ngày giờ bắt đầu.')
    expect(kt({ batDau: '2026-10-16T10:00', ketThuc: '2026-10-16T09:00' }).ketThuc).toBe('Ngày giờ kết thúc phải sau ngày giờ bắt đầu.')
    expect(kt({ batDau: '2026-10-16T10:00', ketThuc: '2026-10-16T10:01' })).toEqual({})
  })

  it('tạo mới: bắt đầu cho lệch quá khứ tối đa 5 phút, kết thúc không ở quá khứ', () => {
    expect(kt({ batDau: '2026-10-15T09:55', ketThuc: '2026-10-16T10:00' })).toEqual({}) // đúng 5 phút trước
    expect(kt({ batDau: '2026-10-15T09:54', ketThuc: '2026-10-16T10:00' }).batDau).toBe('Ngày giờ bắt đầu không được ở quá khứ.')
    expect(kt({ batDau: '2026-10-14T10:00', ketThuc: '2026-10-14T12:00' })).toEqual({
      batDau: 'Ngày giờ bắt đầu không được ở quá khứ.',
      ketThuc: 'Ngày giờ kết thúc không được ở quá khứ.',
    })
  })

  it('sửa phiếu đã bắt đầu: không đổi được giờ bắt đầu; giờ kết thúc mới không được ở quá khứ', () => {
    const cu = { batDauCu: '2026-10-10T08:00:00', ketThucCu: '2026-10-20T23:59:59', taoMoi: false }
    expect(kt({ ...cu, batDau: '2026-10-10T08:00:00', ketThuc: '2026-10-20T23:59:59' })).toEqual({}) // giữ nguyên
    expect(kt({ ...cu, batDau: '2026-10-11T08:00', ketThuc: '2026-10-20T23:59' }).batDau).toBe('Phiếu đã bắt đầu nên không đổi được ngày giờ bắt đầu.')
    expect(kt({ ...cu, ten: 'Đợt', batDau: '2026-10-11T08:00', ketThuc: '2026-10-20T23:59' }).batDau).toBe('Đợt đã bắt đầu nên không đổi được ngày giờ bắt đầu.')
    expect(kt({ ...cu, batDau: '2026-10-10T08:00', ketThuc: '2026-10-12T08:00' }).ketThuc).toBe('Ngày giờ kết thúc mới không được ở quá khứ.')
    expect(kt({ ...cu, batDau: '2026-10-10T08:00', ketThuc: '2026-10-25T08:00' })).toEqual({}) // kéo dài thì được
  })

  it('sửa phiếu chưa bắt đầu: đổi được giờ bắt đầu nhưng không về quá khứ', () => {
    const cu = { batDauCu: '2026-10-20T08:00:00', ketThucCu: '2026-10-25T23:59:59', taoMoi: false }
    expect(kt({ ...cu, batDau: '2026-10-18T08:00', ketThuc: '2026-10-25T23:59' })).toEqual({})
    expect(kt({ ...cu, batDau: '2026-10-14T08:00', ketThuc: '2026-10-25T23:59' }).batDau).toBe('Ngày giờ bắt đầu mới không được ở quá khứ.')
  })
})

/* ======================= Lọc khách (hàm thuần) ======================= */

const HOM_NAY = new Date(2026, 9, 15) // 15/10/2026
const KHACH = [
  { id: 1, ma: 'KH01', hoTen: 'An', soDienThoai: '0901', email: 'an@mail.vn', gioiTinh: 1, ngaySinh: '2000-10-17', tongDon: 3, muaGanNhat: '2026-10-10T09:30:00' },
  { id: 2, ma: 'KH02', hoTen: 'Bình', soDienThoai: '0902', email: 'binh@mail.vn', gioiTinh: 0, ngaySinh: '1995-03-02', tongDon: 0, muaGanNhat: null },
  { id: 3, ma: 'KH03', hoTen: 'Chi', soDienThoai: '0903', email: '', gioiTinh: 0, ngaySinh: '1999-10-15', tongDon: 7, muaGanNhat: '2026-09-01T10:00:00' },
  { id: 4, ma: 'KH04', hoTen: 'Dũng', soDienThoai: '0904', email: 'dung@mail.vn', gioiTinh: 1, ngaySinh: null, tongDon: 1, muaGanNhat: '2026-07-20T18:00:00' },
  { id: 5, ma: 'KH05', hoTen: 'Em', soDienThoai: '0905', email: 'em@mail.vn', gioiTinh: null, ngaySinh: '2001-11-20', tongDon: 2, muaGanNhat: '2026-10-14T08:00:00' },
]
const ma = (ds) => ds.map((k) => k.ma)

describe('Lọc khách nhận phiếu cá nhân (hàm thuần)', () => {
  it('giới tính: Nam / Nữ / chưa khai báo', () => {
    expect(tenGioiTinh(1)).toBe('Nam')
    expect(tenGioiTinh(0)).toBe('Nữ')
    expect(tenGioiTinh(null)).toBe('')
    expect(ma(locKhach(KHACH, { gioiTinh: '1' }, HOM_NAY))).toEqual(['KH01', 'KH04'])
    expect(ma(locKhach(KHACH, { gioiTinh: '0' }, HOM_NAY))).toEqual(['KH02', 'KH03'])
    expect(ma(locKhach(KHACH, { gioiTinh: 'chua' }, HOM_NAY))).toEqual(['KH05'])
  })

  it('tháng sinh', () => {
    expect(ma(locKhach(KHACH, { thangSinh: '10' }, HOM_NAY))).toEqual(['KH01', 'KH03'])
    expect(ma(locKhach(KHACH, { thangSinh: '3' }, HOM_NAY))).toEqual(['KH02'])
    expect(ma(locKhach(KHACH, { thangSinh: '12' }, HOM_NAY))).toEqual([])
  })

  it('sắp sinh nhật trong 7 / 30 ngày tới (hôm nay tính là 0 ngày; không có ngày sinh thì loại)', () => {
    expect(ngayToiSinhNhat('1999-10-15', HOM_NAY)).toBe(0)
    expect(ngayToiSinhNhat('2000-10-17', HOM_NAY)).toBe(2)
    expect(ngayToiSinhNhat('2001-11-20', HOM_NAY)).toBe(36)
    expect(ngayToiSinhNhat('1995-10-14', HOM_NAY)).toBe(364) // vừa qua hôm qua -> sang năm
    expect(ngayToiSinhNhat(null, HOM_NAY)).toBeNull()
    expect(ma(locKhach(KHACH, { sinhNhat: '7' }, HOM_NAY))).toEqual(['KH01', 'KH03'])
    expect(ma(locKhach(KHACH, { sinhNhat: '30' }, HOM_NAY))).toEqual(['KH01', 'KH03'])
    expect(ghiChuSinhNhat('1999-10-15', HOM_NAY)).toBe('Hôm nay sinh nhật')
    expect(ghiChuSinhNhat('2000-10-17', HOM_NAY)).toBe('Sinh nhật sau 2 ngày')
    expect(ghiChuSinhNhat('2001-11-20', HOM_NAY)).toBe('')
  })

  it('sinh ngày 29/2: năm không nhuận tính vào 28/2', () => {
    expect(ngayToiSinhNhat('2000-02-29', new Date(2027, 1, 27))).toBe(1) // 28/2/2027
    expect(ngayToiSinhNhat('2000-02-29', new Date(2028, 1, 28))).toBe(1) // 29/2/2028 (nhuận)
  })

  it('số đơn đã hoàn thành', () => {
    expect(ma(locKhach(KHACH, { soDon: '0-0' }, HOM_NAY))).toEqual(['KH02'])
    expect(ma(locKhach(KHACH, { soDon: '1-2' }, HOM_NAY))).toEqual(['KH04', 'KH05'])
    expect(ma(locKhach(KHACH, { soDon: '3-5' }, HOM_NAY))).toEqual(['KH01'])
    expect(ma(locKhach(KHACH, { soDon: '6-' }, HOM_NAY))).toEqual(['KH03'])
  })

  it('ngày mua gần nhất (khách chưa mua bị loại khi lọc theo ngày)', () => {
    expect(ma(locKhach(KHACH, { muaTu: '2026-10-01' }, HOM_NAY))).toEqual(['KH01', 'KH05'])
    expect(ma(locKhach(KHACH, { muaTu: '2026-07-01', muaDen: '2026-09-30' }, HOM_NAY))).toEqual(['KH03', 'KH04'])
    expect(ma(locKhach(KHACH, { muaDen: '2026-10-10' }, HOM_NAY))).toEqual(['KH01', 'KH03', 'KH04'])
  })

  it('kết hợp nhiều điều kiện + từ khóa không phân biệt hoa thường / dấu', () => {
    expect(ma(locKhach(KHACH, { gioiTinh: '0', thangSinh: '10' }, HOM_NAY))).toEqual(['KH03'])
    expect(ma(locKhach(KHACH, { tuKhoa: 'DUNG' }, HOM_NAY))).toEqual(['KH04'])
    expect(ma(locKhach(KHACH, { tuKhoa: 'binh@', gioiTinh: '1' }, HOM_NAY))).toEqual([])
    expect(ma(locKhach(KHACH, {}, HOM_NAY))).toHaveLength(5)
  })
})

/* ======================= Tính tiền phiếu + đợt (ví dụ trên form) ======================= */

describe('Tính phiếu giảm giá trên đơn (đợt giảm trước, phiếu giảm sau, không cộng dồn phiếu)', () => {
  const p10 = { loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000, donToiThieu: 1500000 }
  it('giảm % có trần, đơn tối thiểu xét trên tiền SAU đợt', () => {
    expect(tinhGiamPhieu(p10, 1800000)).toEqual({ duDieuKien: true, giam: 180000, lyDo: '' })
    expect(tinhGiamPhieu(p10, 3000000).giam).toBe(200000) // chạm trần
    const du = viDuDonHang(p10, 2000000, 10)
    expect(du).toMatchObject({ tienHang: 2000000, giamDot: 200000, sauDot: 1800000, khachTra: 1620000 })
    expect(du.phieu.giam).toBe(180000)
    const chuaDu = viDuDonHang(p10, 1600000, 10) // sau đợt 1.440.000 < 1.500.000
    expect(chuaDu.phieu.duDieuKien).toBe(false)
    expect(chuaDu.phieu.lyDo).toBe('Chưa đủ điều kiện: đơn tối thiểu 1.500.000 ₫ (còn thiếu 60.000 ₫).')
    expect(chuaDu.khachTra).toBe(1440000)
  })
  it('giảm tiền không vượt tổng đơn; % tròn xuống; giá trị 0 thì không giảm', () => {
    expect(tinhGiamPhieu({ loaiGiam: 'TIEN_MAT', giaTri: 50000, donToiThieu: 0 }, 30000).giam).toBe(30000)
    expect(tinhGiamPhieu({ loaiGiam: 'PHAN_TRAM', giaTri: 10 }, 1234567).giam).toBe(123456)
    expect(tinhGiamPhieu({ loaiGiam: 'PHAN_TRAM', giaTri: 0 }, 100000).giam).toBe(0)
  })
})

/* ======================= Danh sách: hiện ngày giờ + trạng thái theo giờ ======================= */

const phieu = (over = {}) => ({
  id: 1, ma: 'VCH0001', ten: 'Tri ân', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000,
  donToiThieu: 500000, soLuong: 30, soLuongDaDung: 0, gioiHanMoiKhach: 1, ngayBatDau: '2026-10-15T08:00:00', ngayKetThuc: '2026-10-15T17:00:59',
  hoatDong: true, khachHangs: [], ...over,
})

describe('Danh sách phiếu: cột ngày giờ + trạng thái đổi theo giờ', () => {
  async function montarDanhSach(ds) {
    mock = { api: { get: vi.fn(async () => ({ data: ds })) } }
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<div/>' } }] })
    router.push('/phieu-giam-gia'); await router.isReady()
    const Comp = (await import('../src/components/PhieuGiamGiaManager.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const dong = (w, maPhieu) => w.findAll('tbody tr').find((tr) => tr.text().includes(maPhieu))

  it('cột Ngày giờ bắt đầu / kết thúc hiện dd/MM/yyyy HH:mm', async () => {
    const w = await montarDanhSach([phieu({ ma: 'VCHGIO001' })])
    expect(w.text()).toContain('Ngày giờ bắt đầu')
    expect(w.text()).toContain('Ngày giờ kết thúc')
    expect(dong(w, 'VCHGIO001').get('[data-testid=pgg-ngay-bd]').text()).toBe('15/10/2026 08:00')
    expect(dong(w, 'VCHGIO001').get('[data-testid=pgg-ngay-kt]').text()).toBe('15/10/2026 17:00')
    w.unmount()
  })

  it('10:00 ngày 15/10: phiếu 08:00-09:30 đã kết thúc, 09:00-11:00 đang hoạt động, 11:00-12:00 sắp diễn ra', async () => {
    const w = await montarDanhSach([
      phieu({ id: 1, ma: 'VCHXONG01', ngayBatDau: '2026-10-15T08:00:00', ngayKetThuc: '2026-10-15T09:30:59' }),
      phieu({ id: 2, ma: 'VCHDANG02', ngayBatDau: '2026-10-15T09:00:00', ngayKetThuc: '2026-10-15T11:00:59' }),
      phieu({ id: 3, ma: 'VCHSAPD03', ngayBatDau: '2026-10-15T11:00:00', ngayKetThuc: '2026-10-15T12:00:59' }),
    ])
    expect(dong(w, 'VCHXONG01').text()).toContain('Đã kết thúc')
    expect(dong(w, 'VCHDANG02').text()).toContain('Đang hoạt động')
    expect(dong(w, 'VCHSAPD03').text()).toContain('Sắp diễn ra')
    // phiếu đã kết thúc: công tắc tắt + khóa
    const sw = dong(w, 'VCHXONG01').get('button[role="switch"]')
    expect(sw.attributes('aria-checked')).toBe('false')
    expect(sw.attributes('disabled')).toBeDefined()
    w.unmount()
  })

  it('đồng hồ chạy: tới 11:01 thì phiếu 09:00-11:00 tự chuyển sang "Đã kết thúc" (không cần tải lại)', async () => {
    vi.useRealTimers()
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
    vi.setSystemTime(new Date(2026, 9, 15, 10, 59, 40))
    const w = await montarDanhSach([phieu({ id: 2, ma: 'VCHDANG02', ngayBatDau: '2026-10-15T09:00:00', ngayKetThuc: '2026-10-15T11:00:59' })])
    expect(dong(w, 'VCHDANG02').text()).toContain('Đang hoạt động')
    vi.setSystemTime(new Date(2026, 9, 15, 11, 1, 5))
    vi.advanceTimersByTime(30000)
    await flushPromises()
    expect(dong(w, 'VCHDANG02').text()).toContain('Đã kết thúc')
    w.unmount()
  })

  it('bộ lọc ngày bắt đầu / kết thúc vẫn đúng khi dữ liệu có giờ', async () => {
    const w = await montarDanhSach([
      phieu({ id: 1, ma: 'VCHNGAY015', ngayBatDau: '2026-10-15T08:00:00', ngayKetThuc: '2026-10-20T17:00:59' }),
      phieu({ id: 2, ma: 'VCHNGAY030', ngayBatDau: '2026-10-30T08:00:00', ngayKetThuc: '2026-11-05T17:00:59' }),
    ])
    await w.get('#pgg-tu-ngay').setValue('2026-10-20')
    expect(dong(w, 'VCHNGAY015')).toBeUndefined()
    expect(dong(w, 'VCHNGAY030')).toBeDefined()
    await w.get('#pgg-tu-ngay').setValue('')
    await w.get('#pgg-den-ngay').setValue('2026-10-20')
    expect(dong(w, 'VCHNGAY015')).toBeDefined()
    expect(dong(w, 'VCHNGAY030')).toBeUndefined()
    w.unmount()
  })
})

/* ======================= Form phiếu giảm giá ======================= */

function apiForm({ khach = KHACH, cheDo = { cheDo: 'MAX', moTa: 'Lấy mức giảm CAO NHẤT trong các đợt đang chồng nhau (không cộng dồn).' } } = {}) {
  return {
    api: {
      get: vi.fn(async (url) => {
        if (url === '/phieu-giam-gia/khach-hang') return { data: khach }
        if (url === '/phieu-giam-gia/trang-thai-mail') return { data: { daCauHinh: true, dangNhap: 'THANH_CONG', lyDo: '' } }
        if (url === '/dot-giam-gia/che-do') return { data: cheDo }
        return { data: [] }
      }),
    },
  }
}
async function montarForm({ item = null, all = [], api = apiForm() } = {}) {
  mock = api
  const Comp = (await import('../src/components/phieuGiamGia/PhieuGiamGiaFormModal.vue')).default
  const w = mount(Comp, { props: { item, all }, attachTo: document.body })
  await flushPromises()
  return w
}
const nhapNgay = async (w, id, iso, gio) => {
  await w.get(`#${id}`).setValue(iso)
  if (gio !== undefined) await w.get(`#${id}-gio`).setValue(gio)
  await flushPromises()
}
const khungCua = (w, selector) => w.get(selector).element.closest('[class*="col-md-"]').textContent
const chonCaNhan = async (w) => {
  await w.findAll('input[name=hinhThuc]').find((i) => i.element.value === 'CA_NHAN').setValue(true)
  await flushPromises()
}
const dongKhach = (w) => w.findAll('[data-testid=pgg-khach-dong]')
const maKhachHienThi = (w) => dongKhach(w).map((d) => d.find('.ad-code').text())

describe('Form phiếu: bố cục + ngày giờ + trạng thái dự kiến', () => {
  it('có hai thẻ "Thông tin chung" và "Đối tượng áp dụng"; mặc định Công khai, bắt đầu = bây giờ, kết thúc để trống', async () => {
    const w = await montarForm()
    expect(w.get('[data-testid=pgg-thong-tin-chung]').text()).toContain('Thêm mới phiếu giảm giá')
    expect(w.get('[data-testid=pgg-thong-tin-chung]').text()).toContain('Thông tin chung')
    expect(w.get('[data-testid=pgg-doi-tuong]').text()).toContain('Đối tượng áp dụng')
    expect(w.get('[data-testid=pgg-doi-tuong]').text()).toContain('Tất cả (Công khai)')
    expect(w.get('[data-testid=pgg-doi-tuong]').text()).toContain('Nhóm chỉ định (Cá nhân)')
    expect(w.get('input[name=hinhThuc]:checked').element.value).toBe('CONG_KHAI')
    expect(w.get('#pgg-ngay-bd').element.value).toBe('15/10/2026')
    expect(w.get('#pgg-ngay-bd-gio').element.value).toBe('10:00')
    expect(w.get('#pgg-ngay-kt').element.value).toBe('')
    expect(w.find('#pgg-so-luong').element.value).toBe('')
    w.unmount()
  })

  it('chưa nhập gì thì chưa hiện lỗi; bấm lưu thì lỗi hiện NGAY DƯỚI TỪNG Ô', async () => {
    const w = await montarForm()
    expect(w.findAll('.ad-error')).toHaveLength(0)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(khungCua(w, '#pgg-ten')).toContain('Nhập tên phiếu.')
    expect(khungCua(w, '#pgg-gia-tri')).toContain('Nhập giá trị giảm.')
    expect(khungCua(w, '#pgg-so-luong')).toContain('Nhập số lượng phiếu phát hành.')
    expect(khungCua(w, '#pgg-ngay-kt')).toContain('Chọn ngày giờ kết thúc.')
    expect(w.emitted('save')).toBeUndefined()
    // mỗi lỗi chỉ nằm ở ô của nó
    expect(khungCua(w, '#pgg-ten')).not.toContain('Nhập giá trị giảm.')
    w.unmount()
  })

  it('lỗi hiện ngay khi chạm vào ô (chưa cần bấm lưu) và mất ngay khi sửa đúng', async () => {
    const w = await montarForm()
    await w.get('#pgg-gia-tri').setValue('150')
    expect(khungCua(w, '#pgg-gia-tri')).toContain('Phần trăm giảm từ 1 đến 100.')
    await w.get('#pgg-gia-tri').setValue('12.5')
    expect(khungCua(w, '#pgg-gia-tri')).toContain('Phần trăm giảm phải là số nguyên từ 1 đến 100.')
    await w.get('#pgg-gia-tri').setValue('15')
    expect(khungCua(w, '#pgg-gia-tri')).not.toContain('Phần trăm giảm')
    // ô khác chưa đụng tới thì chưa báo
    expect(khungCua(w, '#pgg-ten')).not.toContain('Nhập tên phiếu.')
    w.unmount()
  })

  it('Trạng thái dự kiến đổi ngay theo ngày giờ: Đang hoạt động / Sắp diễn ra / Đã kết thúc', async () => {
    const w = await montarForm()
    const nhan = () => w.get('[data-testid=pgg-trang-thai-du-kien]').text()
    expect(nhan()).toContain('Chọn ngày giờ bắt đầu và kết thúc hợp lệ')
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-15', '18:00') // bắt đầu 10:00 (bây giờ), kết thúc 18:00 -> đang chạy
    expect(w.get('[data-testid=pgg-trang-thai-nhan]').text()).toBe('Đang hoạt động')
    expect(nhan()).toContain('kết thúc lúc 15/10/2026 18:00')
    expect(nhan()).toContain('8 giờ')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-16', '08:00')
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-17', '08:00')
    expect(w.get('[data-testid=pgg-trang-thai-nhan]').text()).toBe('Sắp diễn ra')
    expect(nhan()).toContain('Bắt đầu lúc 16/10/2026 08:00')
    expect(nhan()).toContain('22 giờ')
    w.unmount()
  })

  it('tạo mới: bắt đầu ở quá khứ (quá 5 phút) / kết thúc ở quá khứ / kết thúc trước bắt đầu đều báo lỗi dưới ô tương ứng', async () => {
    const w = await montarForm()
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-14', '09:00')
    expect(khungCua(w, '#pgg-ngay-bd')).toContain('Ngày giờ bắt đầu không được ở quá khứ.')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-15', '09:56') // trong 5 phút dung sai
    expect(khungCua(w, '#pgg-ngay-bd')).not.toContain('quá khứ')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-15', '09:54')
    expect(khungCua(w, '#pgg-ngay-bd')).toContain('Ngày giờ bắt đầu không được ở quá khứ.')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-15', '10:00')
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-15', '09:00')
    expect(khungCua(w, '#pgg-ngay-kt')).toContain('Ngày giờ kết thúc phải sau ngày giờ bắt đầu.')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-10', '08:00')
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-12', '08:00')
    expect(khungCua(w, '#pgg-ngay-kt')).toContain('Ngày giờ kết thúc không được ở quá khứ.')
    w.unmount()
  })

  it('điền form lâu: bắt đầu còn là giờ mặc định cũ thì bấm lưu tự lấy lại "bây giờ", không bị báo quá khứ', async () => {
    const w = await montarForm()
    await w.get('#pgg-ten').setValue('Tri ân')
    await w.get('#pgg-gia-tri').setValue('10')
    await w.get('#pgg-so-luong').setValue('50')
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-16', '23:59')
    vi.setSystemTime(new Date(2026, 9, 15, 10, 40, 5)) // 40 phút sau
    await w.get('form').trigger('submit'); await flushPromises()
    const body = w.emitted('save')?.[0]?.[0]
    expect(body, 'phải lưu được, không báo lỗi quá khứ').toBeTruthy()
    expect(body.ngayBatDau).toBe('2026-10-15T10:40:00')
    expect(body.ngayKetThuc).toBe('2026-10-16T23:59:00')
    w.unmount()
  })

  it('đã tự chọn giờ bắt đầu thì KHÔNG bị đổi: quá khứ vẫn báo lỗi khi lưu', async () => {
    const w = await montarForm()
    await w.get('#pgg-ten').setValue('Tri ân')
    await w.get('#pgg-gia-tri').setValue('10')
    await w.get('#pgg-so-luong').setValue('50')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-15', '09:00')
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-16', '23:59')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.emitted('save')).toBeUndefined()
    expect(khungCua(w, '#pgg-ngay-bd')).toContain('Ngày giờ bắt đầu không được ở quá khứ.')
    w.unmount()
  })

  it('giới hạn giá trị: giảm tiền ≤ 100 triệu, giảm tối đa ≤ 100 triệu, đơn tối thiểu ≤ 1 tỷ, số lượng ≤ 1.000.000', async () => {
    const w = await montarForm()
    await w.get('#pgg-gia-tri').setValue('10')
    await w.get('#pgg-giam-toi-da').setValue('100000001')
    expect(khungCua(w, '#pgg-giam-toi-da')).toContain('Giảm tối đa không quá 100.000.000 ₫.')
    await w.get('#pgg-don-toi-thieu').setValue('1000000001')
    expect(khungCua(w, '#pgg-don-toi-thieu')).toContain('Đơn tối thiểu không quá 1.000.000.000 ₫.')
    await w.get('#pgg-so-luong').setValue('1000001')
    expect(khungCua(w, '#pgg-so-luong')).toContain('Số lượng tối đa 1.000.000 phiếu.')
    await w.get('#pgg-so-luong').setValue('0')
    expect(khungCua(w, '#pgg-so-luong')).toContain('Số lượng là số nguyên từ 1 trở lên.')
    await w.get('#pgg-so-luong').setValue('5')
    await w.get('#pgg-gioi-han').setValue('6')
    expect(khungCua(w, '#pgg-gioi-han')).toContain('Giới hạn mỗi khách không được lớn hơn số lượng phát hành.')
    await datSelect2(w, '#pgg-loai-giam', 'TIEN_MAT')
    await w.get('#pgg-gia-tri').setValue('100000001')
    expect(khungCua(w, '#pgg-gia-tri')).toContain('Số tiền giảm tối đa 100.000.000 ₫.')
    w.unmount()
  })

  it('trùng mã phiếu: báo ngay dưới ô Mã khi gõ mã đã có; nút tạo mã khác sinh mã mới hợp lệ', async () => {
    const w = await montarForm({ all: [phieu({ ma: 'VCHDATON1' })] })
    await w.get('#pgg-ma').setValue('vchdaton1')
    expect(khungCua(w, '#pgg-ma')).toContain('Mã phiếu đã tồn tại.')
    await w.get('#pgg-ma').setValue('ab')
    expect(khungCua(w, '#pgg-ma')).toContain('Mã gồm 4-20 ký tự')
    await w.get('button[aria-label="Tạo mã khác"]').trigger('click')
    expect(w.get('#pgg-ma').element.value).toMatch(/^VCH[A-Z0-9]{6}$/)
    expect(khungCua(w, '#pgg-ma')).not.toContain('Mã phiếu đã tồn tại.')
    w.unmount()
  })

  it('đổi loại giảm: giá trị cũ bị xóa (10% khác 10 ₫), giảm tối đa mất luôn khi giảm theo số tiền', async () => {
    const w = await montarForm()
    await w.get('#pgg-gia-tri').setValue('10')
    await w.get('#pgg-giam-toi-da').setValue('200000')
    await datSelect2(w, '#pgg-loai-giam', 'TIEN_MAT')
    expect(giaTriSelect2(w, '#pgg-loai-giam')).toBe('TIEN_MAT')
    expect(w.get('#pgg-gia-tri').element.value).toBe('')
    expect(khungCua(w, '#pgg-gia-tri')).not.toContain('Nhập giá trị giảm.') // vừa đổi loại thì chưa báo lỗi
    expect(w.find('#pgg-giam-toi-da').exists()).toBe(false)
    await datSelect2(w, '#pgg-loai-giam', 'PHAN_TRAM')
    expect(w.find('#pgg-giam-toi-da').exists()).toBe(true)
    w.unmount()
  })
})

describe('Form SỬA phiếu: kiểm tra y như form thêm + khóa giờ bắt đầu khi đã chạy', () => {
  const dangChay = phieu({ id: 7, ma: 'VCHDANGCH', ten: 'Đang chạy', ngayBatDau: '2026-10-10T08:00:00', ngayKetThuc: '2026-10-20T23:59:59', soLuong: 30, soLuongDaDung: 4 })

  it('phiếu đã bắt đầu: ô ngày giờ bắt đầu bị khóa kèm ghi chú; ô kết thúc vẫn sửa được', async () => {
    const w = await montarForm({ item: dangChay, all: [dangChay] })
    expect(w.get('#pgg-ngay-bd').attributes('disabled')).toBeDefined()
    expect(w.get('#pgg-ngay-bd-gio').attributes('disabled')).toBeDefined()
    expect(w.get('[data-testid=pgg-khoa-bat-dau]').text()).toContain('Phiếu đã bắt đầu nên không đổi được ngày giờ bắt đầu.')
    expect(w.get('#pgg-ngay-kt').attributes('disabled')).toBeUndefined()
    expect(w.get('[data-testid=pgg-trang-thai-nhan]').text()).toBe('Đang hoạt động')
    expect(w.get('#pgg-ma').attributes('readonly')).toBeDefined()
    w.unmount()
  })

  it('dời kết thúc về quá khứ bị báo; kéo dài thì lưu được và gửi đúng ngày giờ', async () => {
    const w = await montarForm({ item: dangChay, all: [dangChay] })
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-12', '08:00')
    expect(khungCua(w, '#pgg-ngay-kt')).toContain('Ngày giờ kết thúc mới không được ở quá khứ.')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.emitted('save')).toBeUndefined()
    await nhapNgay(w, 'pgg-ngay-kt', '2026-10-25', '18:30')
    await w.get('form').trigger('submit'); await flushPromises()
    const body = w.emitted('save')[0][0]
    expect(body.ngayBatDau).toBe('2026-10-10T08:00:00') // giữ nguyên giờ bắt đầu cũ (kể cả giây)
    expect(body.ngayKetThuc).toBe('2026-10-25T18:30:00')
    expect(body.ma).toBe('VCHDANGCH')
    expect(body.soLuong).toBe(30)
    w.unmount()
  })

  it('số lượng không nhỏ hơn số phiếu đã dùng (giống backend)', async () => {
    const w = await montarForm({ item: dangChay, all: [dangChay] })
    await w.get('#pgg-so-luong').setValue('3')
    expect(khungCua(w, '#pgg-so-luong')).toContain('Số lượng không được nhỏ hơn số phiếu đã dùng (4).')
    w.unmount()
  })

  it('form sửa báo lỗi giống hệt form thêm (tên trống, % sai, số lượng trống)', async () => {
    const w = await montarForm({ item: dangChay, all: [dangChay] })
    await w.get('#pgg-ten').setValue('')
    await w.get('#pgg-gia-tri').setValue('0')
    await w.get('#pgg-so-luong').setValue('')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(khungCua(w, '#pgg-ten')).toContain('Nhập tên phiếu.')
    expect(khungCua(w, '#pgg-gia-tri')).toContain('Phần trăm giảm từ 1 đến 100.')
    expect(khungCua(w, '#pgg-so-luong')).toContain('Nhập số lượng phiếu phát hành.')
    expect(w.emitted('save')).toBeUndefined()
    w.unmount()
  })

  it('phiếu sắp diễn ra: đổi giờ bắt đầu được nhưng không về quá khứ; không khóa ô', async () => {
    const sap = phieu({ id: 8, ma: 'VCHSAPDR1', ngayBatDau: '2026-10-20T08:00:00', ngayKetThuc: '2026-10-25T23:59:59' })
    const w = await montarForm({ item: sap, all: [sap] })
    expect(w.get('#pgg-ngay-bd').attributes('disabled')).toBeUndefined()
    expect(w.find('[data-testid=pgg-khoa-bat-dau]').exists()).toBe(false)
    expect(w.get('[data-testid=pgg-trang-thai-nhan]').text()).toBe('Sắp diễn ra')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-14', '08:00')
    expect(khungCua(w, '#pgg-ngay-bd')).toContain('Ngày giờ bắt đầu mới không được ở quá khứ.')
    await nhapNgay(w, 'pgg-ngay-bd', '2026-10-18', '08:00')
    expect(khungCua(w, '#pgg-ngay-bd')).not.toContain('quá khứ')
    w.unmount()
  })

  it('phiếu đang tắt công tắc: trạng thái dự kiến là "Ngưng hoạt động"', async () => {
    const tat = phieu({ id: 9, ma: 'VCHTATCT1', hoatDong: false, ngayBatDau: '2026-10-20T08:00:00', ngayKetThuc: '2026-10-25T23:59:59' })
    const w = await montarForm({ item: tat, all: [tat] })
    expect(w.get('[data-testid=pgg-trang-thai-nhan]').text()).toBe('Ngưng hoạt động')
    w.unmount()
  })

  it('phiếu công khai đã có người dùng: không đổi được sang Cá nhân', async () => {
    const w = await montarForm({ item: dangChay, all: [dangChay] })
    for (const r of w.findAll('input[name=hinhThuc]')) expect(r.attributes('disabled')).toBeDefined()
    expect(w.text()).toContain('Phiếu đã có người dùng nên không đổi được đối tượng áp dụng.')
    w.unmount()
  })
})

describe('Form phiếu: đã bỏ ghi chú và gợi ý theo yêu cầu người dùng', () => {
  it('không còn thẻ ghi chú quy tắc chồng ưu đãi, ví dụ tính tiền và cảnh báo trùng phiếu', async () => {
    const w = await montarForm()
    expect(w.find('[data-testid=pgg-quy-tac]').exists()).toBe(false)
    expect(w.find('[data-testid=pgg-vi-du]').exists()).toBe(false)
    expect(w.find('[data-testid=pgg-phieu-trung]').exists()).toBe(false)
    w.unmount()
  })
})

describe('Form phiếu cá nhân: bảng khách có Tổng đơn / Mua gần nhất / Ngày sinh / Giới tính + bộ lọc', () => {
  async function montarCaNhan(opts = {}) {
    const w = await montarForm({ api: apiForm({ khach: KHACH }), ...opts })
    if (!opts.item) await chonCaNhan(w)
    return w
  }

  it('hiện Tổng đơn, Mua gần nhất, Ngày sinh, Giới tính của từng khách', async () => {
    const w = await montarCaNhan()
    const an = dongKhach(w).find((d) => d.text().includes('KH01'))
    expect(an.get('[data-testid=pgg-tong-don]').text()).toBe('3 đơn')
    expect(an.get('[data-testid=pgg-mua-gan-nhat]').text()).toBe('10/10/2026 09:30')
    expect(an.text()).toContain('17/10/2000')
    expect(an.text()).toContain('Nam')
    const binh = dongKhach(w).find((d) => d.text().includes('KH02'))
    expect(binh.get('[data-testid=pgg-tong-don]').text()).toBe('0 đơn')
    expect(binh.get('[data-testid=pgg-mua-gan-nhat]').text()).toBe('Chưa mua')
    expect(binh.text()).toContain('Nữ')
    const dung = dongKhach(w).find((d) => d.text().includes('KH04'))
    expect(dung.text()).toContain('---') // chưa có ngày sinh
    w.unmount()
  })

  it('sắp sinh nhật có ghi chú nhỏ cạnh ngày sinh', async () => {
    const w = await montarCaNhan()
    const chi = dongKhach(w).find((d) => d.text().includes('KH03'))
    expect(chi.get('[data-testid=pgg-sinh-nhat]').text()).toContain('Hôm nay sinh nhật')
    const an = dongKhach(w).find((d) => d.text().includes('KH01'))
    expect(an.get('[data-testid=pgg-sinh-nhat]').text()).toContain('Sinh nhật sau 2 ngày')
    expect(dongKhach(w).find((d) => d.text().includes('KH05')).find('[data-testid=pgg-sinh-nhat]').exists()).toBe(false)
    w.unmount()
  })

  it('lọc theo giới tính (Select2)', async () => {
    const w = await montarCaNhan()
    await datSelect2(w, '#pgg-khach-gioi-tinh', '0')
    expect(maKhachHienThi(w)).toEqual(['KH02', 'KH03'])
    expect(w.get('[data-testid=pgg-khach-dem]').text()).toBe('Hiển thị 2 / 5 khách hàng')
    await datSelect2(w, '#pgg-khach-gioi-tinh', '1')
    expect(maKhachHienThi(w)).toEqual(['KH01', 'KH04'])
    w.unmount()
  })

  it('lọc theo tháng sinh và sắp sinh nhật', async () => {
    const w = await montarCaNhan()
    await datSelect2(w, '#pgg-khach-thang-sinh', '10')
    expect(maKhachHienThi(w)).toEqual(['KH01', 'KH03'])
    await datSelect2(w, '#pgg-khach-thang-sinh', '')
    await datSelect2(w, '#pgg-khach-sinh-nhat', '7')
    expect(maKhachHienThi(w)).toEqual(['KH01', 'KH03'])
    await chonSelect2(w, '#pgg-khach-sinh-nhat', 'Sinh nhật trong 30 ngày tới')
    expect(maKhachHienThi(w)).toEqual(['KH01', 'KH03'])
    w.unmount()
  })

  it('lọc theo số đơn và ngày mua gần nhất', async () => {
    const w = await montarCaNhan()
    await datSelect2(w, '#pgg-khach-so-don', '0-0')
    expect(maKhachHienThi(w)).toEqual(['KH02'])
    await datSelect2(w, '#pgg-khach-so-don', '1-2')
    expect(maKhachHienThi(w)).toEqual(['KH04', 'KH05'])
    await datSelect2(w, '#pgg-khach-so-don', '')
    await w.get('#pgg-khach-mua-tu').setValue('2026-10-01'); await flushPromises()
    expect(maKhachHienThi(w)).toEqual(['KH01', 'KH05'])
    await w.get('#pgg-khach-mua-den').setValue('2026-10-10'); await flushPromises()
    expect(maKhachHienThi(w)).toEqual(['KH01'])
    w.unmount()
  })

  it('không có khách phù hợp: có dòng thông báo; "Đặt lại lọc" trả lại đủ danh sách', async () => {
    const w = await montarCaNhan()
    await datSelect2(w, '#pgg-khach-gioi-tinh', '0')
    await datSelect2(w, '#pgg-khach-so-don', '1-2')
    expect(dongKhach(w)).toHaveLength(0)
    expect(w.get('[data-testid=pgg-bang-khach]').text()).toContain('Không có khách hàng nào phù hợp bộ lọc.')
    await w.get('[data-testid=pgg-dat-lai-loc]').trigger('click'); await flushPromises()
    expect(dongKhach(w)).toHaveLength(5)
    expect(w.find('[data-testid=pgg-dat-lai-loc]').exists()).toBe(false)
    w.unmount()
  })

  it('chọn tất cả / bỏ chọn chỉ tác động lên KẾT QUẢ ĐANG LỌC, lựa chọn ngoài bộ lọc được giữ', async () => {
    const w = await montarCaNhan()
    await dongKhach(w)[0].get('input[type=checkbox]').setValue(true) // KH01
    await datSelect2(w, '#pgg-khach-gioi-tinh', '0')
    await w.get('[data-testid=pgg-chon-tat-ca]').trigger('click'); await flushPromises()
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 3 khách hàng') // KH01 + KH02 + KH03
    await w.get('[data-testid=pgg-bo-chon]').trigger('click'); await flushPromises()
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 1 khách hàng') // KH01 còn nguyên
    await datSelect2(w, '#pgg-khach-gioi-tinh', '')
    const co = dongKhach(w).map((d) => d.get('input[type=checkbox]').element.checked)
    expect(co).toEqual([true, false, false, false, false])
    w.unmount()
  })

  it('ô tích đầu bảng: chọn / bỏ chọn cả kết quả lọc, trạng thái "một phần" khi chọn lẻ', async () => {
    const w = await montarCaNhan()
    const dau = () => w.get('[data-testid=pgg-chon-dau-bang]')
    await dau().setValue(true)
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 5 khách hàng')
    await dau().setValue(false)
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 0 khách hàng')
    await dongKhach(w)[1].get('input[type=checkbox]').setValue(true)
    await flushPromises()
    expect(dau().element.indeterminate).toBe(true)
    w.unmount()
  })

  it('bấm vào dòng cũng chọn / bỏ chọn khách', async () => {
    const w = await montarCaNhan()
    await dongKhach(w)[2].trigger('click')
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 1 khách hàng')
    await dongKhach(w)[2].trigger('click')
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 0 khách hàng')
    w.unmount()
  })

  it('sửa phiếu cá nhân: khách đã dùng bị khóa (không bỏ chọn được kể cả khi "Bỏ chọn kết quả lọc"), khách không còn hoạt động vẫn hiện', async () => {
    const item = phieu({
      id: 20, ma: 'VCHCANHAN2', hinhThuc: 'CA_NHAN', soLuong: 2, soLuongDaDung: 1, gioiHanMoiKhach: 1,
      ngayBatDau: '2026-10-20T08:00:00', ngayKetThuc: '2026-10-30T23:59:59',
      khachHangs: [
        { id: 1, ma: 'KH01', hoTen: 'An', daDung: true, tongDon: 3, muaGanNhat: '2026-10-10T09:30:00' },
        { id: 99, ma: 'KH99', hoTen: 'Khách cũ đã nghỉ', daDung: false, tongDon: 1, muaGanNhat: '2026-01-01T10:00:00' },
      ],
    })
    const w = await montarForm({ item, all: [item], api: apiForm({ khach: KHACH }) })
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 2 khách hàng')
    expect(maKhachHienThi(w)).toContain('KH99') // không có trong danh sách đang hoạt động nhưng vẫn hiện
    const an = dongKhach(w).find((d) => d.text().includes('KH01'))
    expect(an.get('input[type=checkbox]').attributes('disabled')).toBeDefined()
    expect(an.get('input[type=checkbox]').element.checked).toBe(true)
    expect(an.text()).toContain('Đã dùng')
    await w.get('[data-testid=pgg-bo-chon]').trigger('click'); await flushPromises()
    expect(w.get('[data-testid=pgg-da-chon]').text()).toBe('Đã chọn 1 khách hàng') // chỉ KH01 (đã dùng) còn
    expect(w.find('#pgg-so-luong').exists()).toBe(false)
    w.unmount()
  })
})

describe('Chi tiết phiếu: ngày giờ + thống kê khách', () => {
  it('hiện ngày giờ áp dụng và tổng đơn / mua gần nhất của khách được tặng', async () => {
    const Comp = (await import('../src/components/phieuGiamGia/PhieuGiamGiaDetailModal.vue')).default
    const item = { ...phieu({ hinhThuc: 'CA_NHAN', khachHangs: [{ id: 1, ma: 'KH01', hoTen: 'An', daDung: false, tongDon: 3, muaGanNhat: '2026-10-10T09:30:00' }] }), trangThai: 'DANG_HOAT_DONG' }
    const w = mount(Comp, { props: { item }, attachTo: document.body })
    expect(document.body.querySelector('[data-testid=pgg-ct-thoi-gian]').textContent).toBe('15/10/2026 08:00 - 15/10/2026 17:00')
    expect(document.body.textContent).toContain('3 đơn · mua gần nhất 10/10/2026 09:30')
    w.unmount()
  })
})
