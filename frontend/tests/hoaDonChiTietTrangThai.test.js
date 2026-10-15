// Trang chi tiết hóa đơn theo lời cô (video review GĐ2):
//  - nút đổi trạng thái do backend quyết định (hoaDon.chuyenDuoc); "Đã hoàn thành" / "Đã hủy" khóa, không còn nút nào;
//  - "Giao hàng thất bại" (báo thất bại bắt buộc lý do, giao lại, hủy sau thất bại) hiện trên dòng thời gian;
//  - chuyển sang "Đã xác nhận" thì tự mở bản in hóa đơn (PDF);
//  - đơn tại quầy không có phí / đơn vị vận chuyển, khách lẻ không hiện số điện thoại, hiện mã phiếu + % giảm, logo GHN/GHTK.
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useToast } from '../src/composables/useToast'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock[k](...a) }) }))

/* ===== Dữ liệu mẫu (đúng hình dạng GET /api/hoa-don/{id} của backend) ===== */

const NUT = (trangThai, ten, batBuocLyDo = false) => ({ trangThai, ten, batBuocLyDo })
const HUY = NUT(0, 'Đã hủy', true)
const THAT_BAI = NUT(8, 'Giao hàng thất bại', true)
// Bảng "đang ở đâu thì chuyển được sang đâu" y hệt HoaDonLuongTrangThai.chuyenDuoc (các test Java đã khóa luật này)
const CHUYEN = {
  1: [NUT(2, 'Đã xác nhận'), HUY],
  2: [NUT(3, 'Chờ giao hàng'), HUY],
  3: [NUT(4, 'Đang giao hàng'), HUY],
  4: [NUT(5, 'Đã giao hàng'), THAT_BAI],
  5: [NUT(6, 'Đã hoàn thành')],
  8: [NUT(4, 'Đang giao hàng'), HUY],
  6: [],
  0: [],
}
const TEN = { 0: 'Đã hủy', 1: 'Chờ xác nhận', 2: 'Đã xác nhận', 3: 'Chờ giao hàng', 4: 'Đang giao hàng', 5: 'Đã giao hàng', 6: 'Đã hoàn thành', 7: 'Hoàn tiền', 8: 'Giao hàng thất bại' }

const ls = (id, trangThai, thoiGian, moTa = null, nguoiThaoTac = 'Quản lý') => ({ id, trangThai, hanhDong: TEN[trangThai], thoiGian, nguoiThaoTac, moTa })
const TAO = ls(1, 1, '2026-10-08T09:00:00', null, 'NV01')
const XAC_NHAN = ls(2, 2, '2026-10-08T09:30:00')
const CHO_GIAO = ls(3, 3, '2026-10-08T10:00:00')
const DANG_GIAO = ls(4, 4, '2026-10-08T11:00:00')
const THAT_BAI_LS = ls(5, 8, '2026-10-09T15:00:00', 'Khách không nghe máy 3 lần')
const GIAO_LAI = ls(6, 4, '2026-10-10T08:00:00', 'Giao lại sau khi giao hàng thất bại')

const PHIEU = { maPhieuGiamGia: 'VCH7QX2YN', tenPhieuGiamGia: 'Giảm 10% tháng 10', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10, giamToiDaPhieuGiamGia: 200000 }

function donGiaoHang(trangThai, lichSu = [TAO], ghiDe = {}) {
  return {
    hoaDon: {
      id: 1, maHoaDon: 'HD03', loaiDon: 2, tenLoaiDon: 'Trực tuyến', trangThai, tenTrangThai: TEN[trangThai], ngayTao: '2026-10-08T09:00:00',
      tenNhanVien: 'Nguyễn Hoàng Long', phuongThucThanhToan: 'Thanh toán khi nhận hàng (COD)',
      tongTien: 1000000, soTienGiam: 0, phiVanChuyen: 30000, thanhTien: 1030000, donViVanChuyen: 'Giao Hàng Nhanh',
      maPhieuGiamGia: null, tenPhieuGiamGia: null, loaiPhieuGiamGia: null, giaTriPhieuGiamGia: null, giamToiDaPhieuGiamGia: null,
      laTrangThaiCuoi: trangThai === 0 || trangThai === 6 || trangThai === 7, chuyenDuoc: CHUYEN[trangThai] ?? [],
      ...ghiDe,
    },
    khachHang: { ten: 'Nguyễn Thị Lan', sdt: '0901000001', email: 'lan@mail.vn', khachLe: false },
    giaoHang: { hoTenNguoiNhan: 'Nguyễn Thị Lan', sdtNguoiNhan: '0901000001', diaChi: 'Số 12 ngõ 45 Nguyễn Chí Thanh, Hà Nội' },
    sanPham: [{ id: 1, maSpct: 'SP02-XDG-3U-G5', tenSanPham: 'Astrox 99', mauSac: 'Xanh dương', trongLuong: '3U', chuVi: 'G5', soLuong: 1, donGia: 1000000, giaGoc: 1000000, phanTramGiam: 0, thanhTien: 1000000, tenDanhMuc: 'Vợt' }],
    thanhToan: [],
    lichSu,
    daThanhToan: 0,
  }
}

function donTaiQuay(trangThai = 6, ghiDe = {}) {
  const goc = donGiaoHang(trangThai, [TAO, ls(2, 6, '2026-10-08T09:05:00')], {
    id: 1, maHoaDon: 'HD01', loaiDon: 1, tenLoaiDon: 'Tại quầy', phiVanChuyen: 0, donViVanChuyen: null, thanhTien: 1000000,
    chuyenDuoc: trangThai === 6 ? [] : [NUT(6, 'Đã hoàn thành'), HUY], ...ghiDe,
  })
  return {
    ...goc,
    khachHang: { ten: 'Khách vãng lai', sdt: null, email: null, khachLe: true },
    giaoHang: { hoTenNguoiNhan: null, sdtNguoiNhan: null, diaChi: null },
    daThanhToan: trangThai === 6 ? 1000000 : 0,
  }
}

/* ===== Hỗ trợ ===== */

const dangMo = []
const toast = useToast()

// Chờ trang chi tiết tải xong và mã QR (tạo bất đồng bộ, tốn CPU nên máy chậm lâu hơn) hiện ra.
// Không chờ một khoảng cố định (30ms) vì máy yếu / đang bận sẽ chưa kịp và test báo lỗi oan.
async function choTaiXong(w) {
  await flushPromises()
  try {
    await vi.waitFor(() => expect(w.find('.hd-qr img').exists()).toBe(true), { timeout: 3000 })
  } catch { /* không tạo được QR (vd. dữ liệu thiếu mã hóa đơn) hoặc trang lỗi: để từng test tự kiểm tra */ }
  await flushPromises()
}

async function montar(chiTiet, put = vi.fn(async () => ({ data: chiTiet }))) {
  mock = { get: vi.fn(async () => ({ data: chiTiet })), put }
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/hoa-don', component: { template: '<div/>' } }, { path: '/hoa-don/:id', component: { template: '<div/>' } }] })
  router.push('/hoa-don/1'); await router.isReady()
  const Comp = (await import('../src/components/HoaDonChiTiet.vue')).default
  const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
  dangMo.push(w)
  await choTaiXong(w)
  return w
}

const nutDoiTrangThai = (w) => w.findAll('[data-chuyen]').map((b) => b.text())
const cacBuoc = (w) => w.findAll('li.hd-step').map((li) => Number(li.attributes('data-buoc')))
const buoc = (w, ma) => w.get(`li.hd-step[data-buoc="${ma}"]`)
const bamNut = async (w, chu) => {
  const nut = w.findAll('button').find((b) => b.text().includes(chu))
  expect(nut, `nút "${chu}"`).toBeTruthy()
  await nut.trigger('click')
  await flushPromises()
}
// Hộp thoại nằm ngoài component (Teleport vào body)
const hopThoai = () => document.body.querySelector('[role="dialog"]')
const nutHopThoai = (chu) => [...document.body.querySelectorAll('.modal-footer button')].find((b) => b.textContent.trim() === chu)
async function bamXacNhan(chu) {
  const nut = nutHopThoai(chu)
  expect(nut, `nút "${chu}" trong hộp thoại`).toBeTruthy()
  nut.click()
  await flushPromises()
}
async function nhapGhiChu(noiDung) {
  const o = document.getElementById('hd-ghi-chu')
  o.value = noiDung
  o.dispatchEvent(new Event('input'))
  await flushPromises()
}
const loiHopThoai = () => hopThoai()?.querySelector('.ad-error')?.textContent.trim()

function cuaSoInGia() {
  const cuaSo = { document: { write: vi.fn(), close: vi.fn() }, close: vi.fn() }
  const open = vi.spyOn(window, 'open').mockReturnValue(cuaSo)
  return { cuaSo, open }
}

beforeEach(() => { document.body.innerHTML = ''; toast.toasts.splice(0) })
afterEach(() => {
  while (dangMo.length) dangMo.pop().unmount()
  vi.restoreAllMocks()
})

/* ===== 1. Nút theo trạng thái ===== */

describe('Nút đổi trạng thái do backend quyết định', () => {
  it('Chờ xác nhận: "Hủy đơn" và "Xác nhận đơn"; không có thông báo khóa', async () => {
    const w = await montar(donGiaoHang(1))
    expect(nutDoiTrangThai(w)).toEqual(['Hủy đơn', 'Xác nhận đơn'])
    expect(w.find('.hd-khoa').exists()).toBe(false)
  })

  it('Đang giao hàng: có "Giao hàng thất bại" và "Xác nhận đã giao", KHÔNG có "Hủy đơn" (đã giao cho vận chuyển thì không hủy)', async () => {
    const w = await montar(donGiaoHang(4, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO]))
    expect(nutDoiTrangThai(w)).toEqual(['Giao hàng thất bại', 'Xác nhận đã giao'])
    expect(w.text()).not.toContain('Hủy đơn')
  })

  it('Giao hàng thất bại: chỉ còn "Giao lại" và "Hủy đơn"', async () => {
    const w = await montar(donGiaoHang(8, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS]))
    expect(nutDoiTrangThai(w)).toEqual(['Hủy đơn', 'Giao lại'])
  })

  it('Đã giao hàng: chỉ "Hoàn thành đơn"', async () => {
    const w = await montar(donGiaoHang(5, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, ls(5, 5, '2026-10-09T10:00:00')]))
    expect(nutDoiTrangThai(w)).toEqual(['Hoàn thành đơn'])
  })

  it('đơn tại quầy chưa thanh toán: "Hủy đơn" và "Xác nhận thanh toán"', async () => {
    const w = await montar(donTaiQuay(1))
    expect(nutDoiTrangThai(w)).toEqual(['Hủy đơn', 'Xác nhận thanh toán'])
  })

  it('"Đã hoàn thành" là trạng thái cuối: KHÔNG còn nút đổi trạng thái, có thông báo khóa, vẫn xem được lịch sử (cô: "đã hoàn thành rồi sao còn chuyển trạng thái")', async () => {
    const w = await montar(donGiaoHang(6, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, ls(5, 5, '2026-10-09T10:00:00'), ls(6, 6, '2026-10-09T10:05:00')]))
    expect(w.findAll('[data-chuyen]')).toHaveLength(0)
    const khoa = w.get('.hd-khoa')
    expect(khoa.text()).toContain('đã hoàn thành')
    expect(khoa.text()).toContain('không thể đổi trạng thái nữa')
    expect(w.findAll('button').some((b) => b.text().includes('Lịch sử thao tác'))).toBe(true)
    for (const chu of ['Hủy đơn', 'Xác nhận', 'Giao lại', 'Hoàn thành đơn']) expect(w.findAll('button').some((b) => b.text() === chu), chu).toBe(false)
  })

  it('"Đã hủy" cũng khóa hẳn', async () => {
    const w = await montar(donGiaoHang(0, [TAO, ls(2, 0, '2026-10-08T09:10:00', 'Khách đổi ý')]))
    expect(w.findAll('[data-chuyen]')).toHaveLength(0)
    expect(w.get('.hd-khoa').text()).toContain('đã hủy')
  })

  it('backend cũ chưa trả laTrangThaiCuoi thì giao diện tự suy ra từ mã trạng thái (Đã hủy, Đã hoàn thành, Hoàn tiền)', async () => {
    for (const ma of [0, 6, 7]) {
      const chiTiet = donGiaoHang(ma)
      delete chiTiet.hoaDon.laTrangThaiCuoi
      const w = await montar(chiTiet)
      expect(w.find('.hd-khoa').exists(), `mã ${ma}`).toBe(true)
      expect(w.findAll('[data-chuyen]'), `mã ${ma}`).toHaveLength(0)
    }
  })

  it('nút do API trả về là gì thì vẽ đúng thế (không tự thêm nút nào)', async () => {
    const chiTiet = donGiaoHang(1)
    chiTiet.hoaDon.chuyenDuoc = [NUT(2, 'Đã xác nhận')]
    const w = await montar(chiTiet)
    expect(nutDoiTrangThai(w)).toEqual(['Xác nhận đơn'])
  })
})

/* ===== 2. Hộp thoại xác nhận ===== */

describe('Hủy đơn / báo giao hàng thất bại / giao lại', () => {
  it('Hủy đơn bắt buộc nhập lý do: để trống thì báo lỗi ngay trong hộp thoại và KHÔNG gọi API', async () => {
    const put = vi.fn()
    const w = await montar(donGiaoHang(1), put)
    await bamNut(w, 'Hủy đơn')
    expect(hopThoai().textContent).toContain('Hủy đơn hàng')
    expect(document.body.querySelector('label[for="hd-ghi-chu"]').textContent).toContain('Lý do hủy')
    expect(document.body.querySelector('label[for="hd-ghi-chu"] .ad-required')).toBeTruthy()
    await bamXacNhan('Hủy đơn')
    expect(loiHopThoai()).toBe('Vui lòng nhập lý do hủy đơn.')
    expect(put).not.toHaveBeenCalled()
    await nhapGhiChu('   ')
    await bamXacNhan('Hủy đơn')
    expect(loiHopThoai()).toBe('Vui lòng nhập lý do hủy đơn.')
    expect(put).not.toHaveBeenCalled()
  })

  it('Hủy đơn có lý do: gọi API đúng dữ liệu, đóng hộp thoại, trang chuyển sang "Đã hủy" và khóa', async () => {
    const daHuy = donGiaoHang(0, [TAO, ls(2, 0, '2026-10-08T09:10:00', 'Khách đổi ý')])
    const put = vi.fn(async () => ({ data: daHuy }))
    const w = await montar(donGiaoHang(1), put)
    await bamNut(w, 'Hủy đơn')
    await nhapGhiChu('  Khách đổi ý  ')
    await bamXacNhan('Hủy đơn')
    expect(put).toHaveBeenCalledTimes(1)
    expect(put).toHaveBeenCalledWith('/hoa-don/1/trang-thai', { trangThaiMoi: 0, ghiChu: 'Khách đổi ý', nguoiThaoTac: 'Quản lý' })
    expect(hopThoai()).toBeNull()
    expect(w.findAll('[data-chuyen]')).toHaveLength(0)
    expect(w.find('.hd-khoa').exists()).toBe(true)
    expect(cacBuoc(w)).toEqual([1, 0])
    expect(toast.toasts.some((t) => t.type === 'success' && t.message === 'Đã hủy hóa đơn HD03')).toBe(true)
  })

  it('Báo "Giao hàng thất bại" cũng bắt buộc lý do (tiêu đề, nhãn và nút riêng)', async () => {
    const put = vi.fn()
    const w = await montar(donGiaoHang(4, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO]), put)
    await bamNut(w, 'Giao hàng thất bại')
    expect(hopThoai().querySelector('.ad-modal-title').textContent).toBe('Báo giao hàng thất bại')
    expect(document.body.querySelector('label[for="hd-ghi-chu"]').textContent).toContain('Lý do giao hàng thất bại')
    expect(hopThoai().textContent).toContain('thất bại')
    expect(hopThoai().textContent).not.toContain('số lượng hàng của đơn sẽ được cộng lại')
    await bamXacNhan('Ghi nhận thất bại')
    expect(loiHopThoai()).toBe('Vui lòng nhập lý do giao hàng thất bại.')
    expect(put).not.toHaveBeenCalled()
  })

  it('báo thất bại có lý do: gửi trạng thái 8, dòng thời gian hiện nhánh "Giao hàng thất bại" (màu cam, kèm lý do) và đổi nút', async () => {
    const thatBai = donGiaoHang(8, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS])
    const put = vi.fn(async () => ({ data: thatBai }))
    const w = await montar(donGiaoHang(4, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO]), put)
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 5, 6])
    await bamNut(w, 'Giao hàng thất bại')
    await nhapGhiChu('Khách không nghe máy 3 lần')
    await bamXacNhan('Ghi nhận thất bại')
    expect(put).toHaveBeenCalledWith('/hoa-don/1/trang-thai', { trangThaiMoi: 8, ghiChu: 'Khách không nghe máy 3 lần', nguoiThaoTac: 'Quản lý' })
    expect(hopThoai()).toBeNull()
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 8, 5, 6])
    expect(buoc(w, 8).classes()).toEqual(expect.arrayContaining(['is-fail', 'is-latest']))
    expect(buoc(w, 8).text()).toContain('Khách không nghe máy 3 lần')
    expect(buoc(w, 8).text()).toContain('09/10/2026')
    expect(w.get('.hd-card-head .ad-pill').classes()).toContain('ad-pill-orange')
    expect(w.get('.hd-card-head .ad-pill').text()).toBe('Giao hàng thất bại')
    expect(nutDoiTrangThai(w)).toEqual(['Hủy đơn', 'Giao lại'])
    expect(toast.toasts.some((t) => t.message.includes('đã ghi nhận giao hàng thất bại'))).toBe(true)
  })

  it('"Giao lại" không bắt buộc lý do; gửi trạng thái 4 và quay về "Đang giao hàng"', async () => {
    const dangGiao = donGiaoHang(4, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS, GIAO_LAI])
    const put = vi.fn(async () => ({ data: dangGiao }))
    const w = await montar(donGiaoHang(8, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS]), put)
    await bamNut(w, 'Giao lại')
    expect(hopThoai().textContent).toContain('Giao lại hóa đơn')
    expect(document.body.querySelector('label[for="hd-ghi-chu"] .ad-required')).toBeNull()
    await bamXacNhan('Xác nhận')
    expect(put).toHaveBeenCalledWith('/hoa-don/1/trang-thai', { trangThaiMoi: 4, ghiChu: '', nguoiThaoTac: 'Quản lý' })
    expect(hopThoai()).toBeNull()
    expect(nutDoiTrangThai(w)).toEqual(['Giao hàng thất bại', 'Xác nhận đã giao'])
  })

  it('hủy sau khi giao thất bại cũng cần lý do, rồi khóa đơn', async () => {
    const huy = donGiaoHang(0, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS, ls(6, 0, '2026-10-10T09:00:00', 'Khách bỏ đơn')])
    const put = vi.fn(async () => ({ data: huy }))
    const w = await montar(donGiaoHang(8, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS]), put)
    await bamNut(w, 'Hủy đơn')
    await bamXacNhan('Hủy đơn')
    expect(loiHopThoai()).toBe('Vui lòng nhập lý do hủy đơn.')
    await nhapGhiChu('Khách bỏ đơn')
    await bamXacNhan('Hủy đơn')
    expect(put).toHaveBeenCalledWith('/hoa-don/1/trang-thai', { trangThaiMoi: 0, ghiChu: 'Khách bỏ đơn', nguoiThaoTac: 'Quản lý' })
    expect(w.find('.hd-khoa').exists()).toBe(true)
  })

  it('máy chủ từ chối (vd bấm trùng): lỗi hiện trong hộp thoại, hộp thoại vẫn mở, trạng thái trên trang không đổi', async () => {
    const put = vi.fn(async () => { throw new Error('Hóa đơn đã ở trạng thái "Đã xác nhận" rồi. Vui lòng tải lại trang.') })
    const w = await montar(donGiaoHang(1), put)
    await bamNut(w, 'Xác nhận đơn')
    const { cuaSo } = cuaSoInGia()
    await bamXacNhan('Xác nhận')
    expect(loiHopThoai()).toBe('Hóa đơn đã ở trạng thái "Đã xác nhận" rồi. Vui lòng tải lại trang.')
    expect(hopThoai()).not.toBeNull()
    expect(w.get('.hd-card-head .ad-pill').text()).toBe('Chờ xác nhận')
    expect(cuaSo.close).toHaveBeenCalledTimes(1) // cửa sổ in đã mở trước đó phải được đóng lại
  })

  it('ghi chú quá 500 ký tự bị chặn trước khi gọi API', async () => {
    const put = vi.fn()
    const w = await montar(donGiaoHang(1), put)
    await bamNut(w, 'Hủy đơn')
    await nhapGhiChu('a'.repeat(501))
    await bamXacNhan('Hủy đơn')
    expect(loiHopThoai()).toBe('Ghi chú tối đa 500 ký tự.')
    expect(put).not.toHaveBeenCalled()
    expect(document.getElementById('hd-ghi-chu').getAttribute('maxlength')).toBe('500')
  })

  it('gõ lại vào ô lý do thì lỗi tự biến mất', async () => {
    const w = await montar(donGiaoHang(1), vi.fn())
    await bamNut(w, 'Hủy đơn')
    await bamXacNhan('Hủy đơn')
    expect(loiHopThoai()).toBeTruthy()
    await nhapGhiChu('Khách đổi ý')
    expect(loiHopThoai()).toBeUndefined()
  })
})

/* ===== 3. Tự in PDF khi "Đã xác nhận" ===== */

describe('Chuyển sang "Đã xác nhận" thì tự mở bản in hóa đơn (PDF)', () => {
  it('hộp thoại báo trước sẽ tự mở bản in; các bước khác không có dòng này', async () => {
    const w = await montar(donGiaoHang(1))
    await bamNut(w, 'Xác nhận đơn')
    expect(hopThoai().querySelector('.hd-in-tu-dong').textContent).toContain('Lưu dưới dạng PDF')
    document.body.querySelector('.btn-close').click()
    await flushPromises()
    const w2 = await montar(donGiaoHang(2, [TAO, XAC_NHAN]))
    await bamNut(w2, 'Chuẩn bị hàng')
    expect(hopThoai().querySelector('.hd-in-tu-dong')).toBeNull()
  })

  it('cửa sổ in được mở NGAY lúc bấm (trước khi máy chủ trả lời, để trình duyệt không chặn), nội dung ghi vào sau khi thành công', async () => {
    const daXacNhan = donGiaoHang(2, [TAO, XAC_NHAN])
    let traLoi
    const put = vi.fn(() => new Promise((r) => { traLoi = r }))
    const w = await montar(donGiaoHang(1), put)
    const { cuaSo, open } = cuaSoInGia()
    await bamNut(w, 'Xác nhận đơn')
    await bamXacNhan('Xác nhận')
    expect(put).toHaveBeenCalledWith('/hoa-don/1/trang-thai', { trangThaiMoi: 2, ghiChu: '', nguoiThaoTac: 'Quản lý' })
    expect(open).toHaveBeenCalledTimes(1)
    expect(open.mock.calls[0][0]).toBe('')
    expect(open.mock.calls[0][1]).toBe('_blank')
    expect(cuaSo.document.write).not.toHaveBeenCalled() // máy chủ chưa trả lời nên chưa có gì để in
    traLoi({ data: daXacNhan })
    await flushPromises()
    expect(cuaSo.document.write).toHaveBeenCalledTimes(1)
    expect(cuaSo.document.close).toHaveBeenCalledTimes(1)
    expect(cuaSo.close).not.toHaveBeenCalled()
    const html = cuaSo.document.write.mock.calls[0][0]
    expect(html).toContain('HÓA ĐƠN BÁN HÀNG')
    expect(html).toContain('<b>Trạng thái:</b> Đã xác nhận') // in theo dữ liệu MỚI sau khi xác nhận
    expect(html).toContain('window.print()')
    expect(toast.toasts.some((t) => t.type === 'success' && t.message.includes('Lưu dưới dạng PDF'))).toBe(true)
  })

  it('máy chủ báo lỗi thì cửa sổ in đã mở bị đóng lại và không in gì', async () => {
    const put = vi.fn(async () => { throw new Error('Không cập nhật được') })
    const w = await montar(donGiaoHang(1), put)
    const { cuaSo } = cuaSoInGia()
    await bamNut(w, 'Xác nhận đơn')
    await bamXacNhan('Xác nhận')
    expect(cuaSo.close).toHaveBeenCalledTimes(1)
    expect(cuaSo.document.write).not.toHaveBeenCalled()
    expect(loiHopThoai()).toBe('Không cập nhật được')
  })

  it('trình duyệt chặn cửa sổ bật lên: vẫn xác nhận đơn được và có thông báo hướng dẫn bấm "In hóa đơn"', async () => {
    const daXacNhan = donGiaoHang(2, [TAO, XAC_NHAN])
    const put = vi.fn(async () => ({ data: daXacNhan }))
    const w = await montar(donGiaoHang(1), put)
    vi.spyOn(window, 'open').mockReturnValue(null)
    await bamNut(w, 'Xác nhận đơn')
    await bamXacNhan('Xác nhận')
    expect(put).toHaveBeenCalledTimes(1)
    expect(hopThoai()).toBeNull()
    expect(w.get('.hd-card-head .ad-pill').text()).toBe('Đã xác nhận')
    const loi = toast.toasts.find((t) => t.type === 'error')
    expect(loi.message).toContain('chặn cửa sổ in')
    expect(loi.message).toContain('In hóa đơn')
  })

  it('các bước khác (chuẩn bị hàng, giao cho vận chuyển, hủy...) KHÔNG tự in', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    const w = await montar(donGiaoHang(2, [TAO, XAC_NHAN]), vi.fn(async () => ({ data: donGiaoHang(3, [TAO, XAC_NHAN, CHO_GIAO]) })))
    await bamNut(w, 'Chuẩn bị hàng')
    await bamXacNhan('Xác nhận')
    expect(open).not.toHaveBeenCalled()
    expect(toast.toasts.some((t) => t.type === 'error')).toBe(false)
  })

  it('nút "In hóa đơn": bản in KHÔNG có khối "Lịch sử thanh toán" dù trang có lịch sử thanh toán; có mã phiếu + % giảm, phí vận chuyển, logo GHN', async () => {
    const chiTiet = donGiaoHang(2, [TAO, XAC_NHAN], { ...PHIEU, soTienGiam: 100000, thanhTien: 930000 })
    chiTiet.thanhToan = [{ id: 1, phuongThuc: 'Chuyển khoản', trangThai: 1, moTa: 'Đặt cọc', thoiGian: '2026-10-08T09:10:00', soTien: 200000, maGiaoDich: 'GD1' }]
    chiTiet.daThanhToan = 200000
    const w = await montar(chiTiet)
    expect(w.findAll('h2.ad-card-title').map((h) => h.text())).toContain('Lịch sử thanh toán') // trên màn hình vẫn xem được
    const { cuaSo } = cuaSoInGia()
    await bamNut(w, 'In hóa đơn')
    const html = cuaSo.document.write.mock.calls[0][0]
    expect(html).not.toContain('Lịch sử thanh toán')
    expect(html).not.toContain('Chuyển khoản')
    expect(html).toContain('Giảm giá (mã VCH7QX2YN – giảm 10%, tối đa 200.000 ₫)')
    expect(html).toContain('<span>Phí vận chuyển</span><span>30.000 ₫</span>')
    expect(html).toContain('/van-chuyen/ghn.png')
    expect(html).toContain('class="qr"')
    expect(cuaSo.document.close).toHaveBeenCalled()
  })

  it('"In hóa đơn" bị chặn cửa sổ thì báo lỗi, không văng', async () => {
    const w = await montar(donGiaoHang(2, [TAO, XAC_NHAN]))
    vi.spyOn(window, 'open').mockReturnValue(null)
    await bamNut(w, 'In hóa đơn')
    expect(toast.toasts.some((t) => t.type === 'error' && t.message.includes('chặn cửa sổ in'))).toBe(true)
  })
})

/* ===== 4. Dòng thời gian ===== */

describe('Dòng thời gian', () => {
  it('giao hàng thất bại: bước màu cam nằm ngay sau "Đang giao hàng", bước sau chưa đi', async () => {
    const w = await montar(donGiaoHang(8, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS]))
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 8, 5, 6])
    for (const ma of [1, 2, 3, 4]) expect(buoc(w, ma).classes(), `bước ${ma}`).toContain('is-done')
    expect(buoc(w, 8).classes()).toEqual(expect.arrayContaining(['is-fail', 'is-latest']))
    for (const ma of [5, 6]) expect(buoc(w, ma).classes(), `bước ${ma}`).toContain('is-todo')
    expect(buoc(w, 8).text()).toContain('Giao hàng thất bại')
  })

  it('giao lại sau thất bại: đơn lại "Đang giao hàng"; bước thất bại vẫn lưu dấu nhưng không còn là bước mới nhất; giờ "Đang giao" là lần đầu bàn giao', async () => {
    const w = await montar(donGiaoHang(4, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS, GIAO_LAI]))
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 8, 5, 6])
    expect(buoc(w, 4).classes()).toContain('is-current')
    expect(buoc(w, 8).classes()).toContain('is-fail')
    expect(buoc(w, 8).classes()).not.toContain('is-latest')
    expect(buoc(w, 4).text()).toContain('11:00:00') // lần đầu bàn giao, không phải giờ giao lại 08:00:00
    expect(buoc(w, 4).text()).not.toContain('08:00:00')
    expect(buoc(w, 8).text()).toContain('Khách không nghe máy 3 lần')
  })

  it('thất bại rồi giao lại và hoàn thành: mọi bước đã xong, vẫn còn dấu vết thất bại', async () => {
    const hoanThanh = donGiaoHang(6, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS, GIAO_LAI, ls(7, 5, '2026-10-10T14:00:00'), ls(8, 6, '2026-10-10T14:05:00')])
    const w = await montar(hoanThanh)
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 8, 5, 6])
    for (const ma of [1, 2, 3, 4, 5, 6]) expect(buoc(w, ma).classes(), `bước ${ma}`).toContain('is-done')
    expect(buoc(w, 8).classes()).toContain('is-fail')
    expect(w.find('.hd-khoa').exists()).toBe(true)
  })

  it('hủy sau khi giao thất bại: ..., Đang giao hàng, Giao hàng thất bại, Đã hủy (kèm lý do hủy)', async () => {
    const w = await montar(donGiaoHang(0, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS, ls(6, 0, '2026-10-10T09:00:00', 'Khách bỏ đơn')]))
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 8, 0])
    expect(buoc(w, 0).classes()).toContain('is-cancel')
    expect(buoc(w, 0).text()).toContain('Khách bỏ đơn')
    expect(buoc(w, 8).classes()).toContain('is-fail')
  })

  it('đơn hủy sớm (chưa xác nhận) chỉ còn 2 bước: Chờ xác nhận -> Đã hủy', async () => {
    const w = await montar(donGiaoHang(0, [TAO, ls(2, 0, '2026-10-08T09:10:00', 'Đặt nhầm')]))
    expect(cacBuoc(w)).toEqual([1, 0])
    expect(buoc(w, 0).text()).toContain('Đặt nhầm')
  })

  it('đơn thường chưa từng thất bại thì không có bước màu cam', async () => {
    const w = await montar(donGiaoHang(4, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO]))
    expect(cacBuoc(w)).toEqual([1, 2, 3, 4, 5, 6])
    expect(w.find('.hd-step.is-fail').exists()).toBe(false)
    expect(buoc(w, 4).classes()).toContain('is-current')
  })

  it('đơn tại quầy chỉ có 2 bước: Tạo đơn hàng -> Đã hoàn thành', async () => {
    const w = await montar(donTaiQuay(6))
    expect(cacBuoc(w)).toEqual([1, 6])
    expect(w.findAll('li.hd-step').map((li) => li.get('.hd-step-label').text())).toEqual(['Tạo đơn hàng', 'Đã hoàn thành'])
  })

  it('lịch sử thao tác: nhãn "Giao hàng thất bại" màu cam và hiện lý do', async () => {
    const w = await montar(donGiaoHang(8, [TAO, XAC_NHAN, CHO_GIAO, DANG_GIAO, THAT_BAI_LS]))
    await bamNut(w, 'Lịch sử thao tác')
    const dong = [...hopThoai().querySelectorAll('tbody tr')]
    expect(dong).toHaveLength(5)
    const dongThatBai = dong[0] // mới nhất lên đầu
    expect(dongThatBai.textContent).toContain('Giao hàng thất bại')
    expect(dongThatBai.textContent).toContain('Khách không nghe máy 3 lần')
    expect(dongThatBai.querySelector('.ad-pill').classList.contains('ad-pill-orange')).toBe(true)
  })
})

/* ===== 5. Đơn tại quầy, khách lẻ, vận chuyển, mã phiếu ===== */

describe('Đơn tại quầy và khách lẻ', () => {
  it('đơn tại quầy: không có phí vận chuyển, không có đơn vị vận chuyển, ghi "Nhận hàng tại cửa hàng"', async () => {
    const w = await montar(donTaiQuay(6))
    expect(w.find('[data-dong="phi-van-chuyen"]').exists()).toBe(false)
    expect(w.text()).not.toContain('Phí vận chuyển')
    const nhan = w.findAll('dt').map((d) => d.text())
    expect(nhan).not.toContain('Vận chuyển')
    expect(nhan).not.toContain('Người nhận')
    expect(nhan).not.toContain('Địa chỉ')
    expect(w.text()).toContain('Nhận hàng tại cửa hàng')
    expect(w.find('.vc-logo').exists()).toBe(false)
  })

  it('khách lẻ: chỉ hiện "Khách vãng lai", KHÔNG có dòng số điện thoại / email (cô: "khách lẻ tại sao có số điện thoại")', async () => {
    const w = await montar(donTaiQuay(6))
    const nhan = w.findAll('dt').map((d) => d.text())
    expect(nhan).toContain('Tên khách hàng')
    expect(nhan).not.toContain('Số điện thoại')
    expect(nhan).not.toContain('Email')
    expect(nhan).not.toContain('SĐT nhận')
    expect(w.text()).toContain('Khách vãng lai')
    expect(w.text()).toContain('Khách lẻ, không lưu thông tin liên hệ')
  })

  it('khách có hồ sơ vẫn hiện đủ số điện thoại và email', async () => {
    const w = await montar(donGiaoHang(1))
    const nhan = w.findAll('dt').map((d) => d.text())
    expect(nhan).toEqual(expect.arrayContaining(['Số điện thoại', 'Email']))
    expect(w.text()).toContain('0901000001')
    expect(w.text()).toContain('lan@mail.vn')
    expect(w.text()).not.toContain('Khách lẻ, không lưu')
  })

  it('đơn giao hàng: có dòng phí vận chuyển và người nhận / địa chỉ', async () => {
    const w = await montar(donGiaoHang(1))
    expect(w.get('[data-dong="phi-van-chuyen"]').text()).toContain('30.000')
    expect(w.findAll('dt').map((d) => d.text())).toEqual(expect.arrayContaining(['Người nhận', 'Địa chỉ', 'Vận chuyển']))
  })
})

describe('Đơn vị vận chuyển (logo GHN / GHTK)', () => {
  it('Giao Hàng Nhanh hiện logo GHN + tên đầy đủ', async () => {
    const w = await montar(donGiaoHang(1))
    const logo = w.get('img.vc-logo')
    expect(logo.attributes('src')).toBe('/van-chuyen/ghn.png')
    expect(logo.attributes('alt')).toBe('Logo GHN')
    expect(w.get('.vc-ten').text()).toBe('Giao Hàng Nhanh')
  })

  it('đơn vị ghi "GHTK" hiện logo GHTK', async () => {
    const w = await montar(donGiaoHang(1, [TAO], { donViVanChuyen: 'GHTK' }))
    expect(w.get('img.vc-logo').attributes('src')).toBe('/van-chuyen/ghtk.png')
    expect(w.get('.vc-ten').text()).toBe('Giao Hàng Tiết Kiệm')
  })

  it('chưa có file logo (ảnh lỗi) thì tự chuyển sang nhãn chữ GHN, không vỡ giao diện', async () => {
    const w = await montar(donGiaoHang(1))
    await w.get('img.vc-logo').trigger('error')
    expect(w.find('img.vc-logo').exists()).toBe(false)
    expect(w.get('.vc-badge').text()).toBe('GHN')
    expect(w.get('.vc-ten').text()).toBe('Giao Hàng Nhanh')
  })

  it('đơn vị khác chỉ hiện nhãn chữ bằng đúng tên đó; chưa ghi đơn vị thì hiện dấu —', async () => {
    const la = await montar(donGiaoHang(1, [TAO], { donViVanChuyen: 'J&T Express' }))
    expect(la.find('img.vc-logo').exists()).toBe(false)
    expect(la.get('.vc-badge').text()).toBe('J&T Express')
    const chua = await montar(donGiaoHang(1, [TAO], { donViVanChuyen: null }))
    expect(chua.find('.vc').exists()).toBe(false)
    const dd = chua.findAll('dt').find((d) => d.text() === 'Vận chuyển').element.nextElementSibling
    expect(dd.textContent.trim()).toBe('—')
  })
})

describe('Phiếu giảm giá trên hóa đơn (cô: "mã nào, giảm bao nhiêu %")', () => {
  it('ghi rõ mã phiếu và mức giảm ở dòng giảm giá và ở khối thông tin đơn', async () => {
    const w = await montar(donGiaoHang(1, [TAO], { ...PHIEU, soTienGiam: 100000, thanhTien: 930000 }))
    const dong = w.get('[data-dong="giam-gia"]')
    expect(dong.text()).toContain('VCH7QX2YN')
    expect(dong.text()).toContain('giảm 10% (tối đa 200.000 ₫)')
    expect(dong.text()).toContain('- 100.000')
    const phieu = w.get('.hd-phieu')
    expect(phieu.text()).toContain('VCH7QX2YN')
    expect(phieu.text()).toContain('giảm 10%')
  })

  it('phiếu giảm theo số tiền ghi "giảm 50.000 ₫"', async () => {
    const w = await montar(donGiaoHang(1, [TAO], { ...PHIEU, loaiPhieuGiamGia: 2, giaTriPhieuGiamGia: 50000, giamToiDaPhieuGiamGia: null, soTienGiam: 50000 }))
    expect(w.get('[data-dong="giam-gia"]').text()).toContain('giảm 50.000 ₫')
  })

  it('không dùng phiếu thì ghi "Không dùng phiếu" và dòng giảm giá không có mã', async () => {
    const w = await montar(donGiaoHang(1))
    expect(w.text()).toContain('Không dùng phiếu')
    expect(w.find('.hd-phieu').exists()).toBe(false)
    expect(w.find('[data-dong="giam-gia"] .hd-voucher').exists()).toBe(false)
  })
})

describe('Tổng tiền', () => {
  it('đơn đã hủy không hiện "Còn phải trả"; đơn đang xử lý hiện', async () => {
    const huy = await montar(donGiaoHang(0, [TAO, ls(2, 0, '2026-10-08T09:10:00', 'Đặt nhầm')]))
    expect(huy.text()).not.toContain('Còn phải trả')
    const dangXuLy = await montar(donGiaoHang(1))
    expect(dangXuLy.text()).toContain('Còn phải trả')
  })
})
