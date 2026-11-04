import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { api, replace, push } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn() },
  replace: vi.fn(),
  push: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {}, query: {}, fullPath: '/ban-hang' }),
  useRouter: () => ({ replace, push }),
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))

import VaoCaPage from '../src/components/giaoCa/VaoCaPage.vue'
import GiaoCaLichSuPage from '../src/components/giaoCa/GiaoCaLichSuPage.vue'
import GiaoCaNut from '../src/components/giaoCa/GiaoCaNut.vue'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'
import { _datBayGio, _datLaiGiaoCa, useGiaoCa } from '../src/composables/useGiaoCa'
import { useToast } from '../src/composables/useToast'

const body = () => new DOMWrapper(document.body)
const loi = (message, code, extra = {}) => Object.assign(new Error(message), { code, ...extra })
let gan = []
const ganVao = (c) => {
  const w = mount(c, { attachTo: document.body })
  gan.push(w)
  return w
}

beforeEach(() => {
  document.body.innerHTML = ''
  _datLaiAuth()
  _datLaiGiaoCa()
  useToast().toasts.splice(0)
  for (const f of [api.get, api.post, replace, push]) f.mockReset()
})
afterEach(() => {
  gan.forEach((w) => w.unmount())
  gan = []
})

const CA_LONG = { id: 142, maGiaoCa: 'GC140', idNhanVien: 1, tenNhanVien: 'Nguyễn Hoàng Long', thoiGianNhanCa: '2026-11-02T14:33:46' }

describe('Một két tiền: chỉ một ca mở cùng lúc', () => {
  it('trang Vào ca báo ca của người khác chưa giao và khóa nút Bắt đầu ca', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockResolvedValue({ data: { dangMo: null, tongKet: null, goiYTienDauCa: null, lichHomNay: [], caKhacDangMo: CA_LONG } })
    const w = ganVao(VaoCaPage)
    await flushPromises()
    const bao = w.find('[data-testid="vc-ca-khac"]')
    expect(bao.text()).toContain('Ca GC140 của Nguyễn Hoàng Long')
    expect(bao.text()).toContain('đóng ca hộ')
    expect(w.find('[data-testid="vc-bat-dau"]').attributes('disabled')).toBeDefined()
  })

  it('máy chủ chặn vì ca khác vừa mở: tải lại và báo lỗi, không chuyển trang', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    let lan = 0
    api.get.mockImplementation(async () => ({ data: { dangMo: null, lichHomNay: [], caKhacDangMo: lan++ ? CA_LONG : null } }))
    api.post.mockRejectedValue(loi('Ca GC140 của Nguyễn Hoàng Long chưa giao.', 'CA_KHAC_DANG_MO'))
    const w = ganVao(VaoCaPage)
    await flushPromises()
    await w.find('#vc-tien-mat').setValue('500000')
    await w.find('#vc-form').trigger('submit')
    await flushPromises()
    expect(useToast().toasts.map((t) => t.message).join(' ')).toContain('GC140')
    expect(w.find('[data-testid="vc-ca-khac"]').exists()).toBe(true)
    expect(replace).not.toHaveBeenCalled()
  })
})

describe('Quản lý đóng ca hộ', () => {
  const CHI_TIET = {
    ...CA_LONG,
    id: 150,
    maGiaoCa: 'GC141',
    idNhanVien: 3,
    tenNhanVien: 'Lê Quốc Bảo',
    maNhanVien: 'NV03',
    dangTrongCa: true,
    tienDauCa: 1000000,
    tongKet: { tienMatHeThong: 1250000, tienMat: 250000, chuyenKhoan: 0 },
    hoaDons: [],
  }
  function moTrang() {
    useAuth().trangThai.nguoiDung = { id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' }
    api.get.mockImplementation(async (url) => {
      if (url === '/giao-ca') return { data: [{ ...CHI_TIET }] }
      if (url === '/giao-ca/150') return { data: { ...CHI_TIET } }
      return { data: [] }
    })
    return ganVao(GiaoCaLichSuPage)
  }

  it('điền sẵn tiền hệ thống; còn hóa đơn chờ thì cho hủy rồi đóng hộ được', async () => {
    const w = moTrang()
    await flushPromises()
    await w.find('.gcl-dong').trigger('click')
    await flushPromises()
    await body().find('[data-testid="gcl-mo-dong-ho"]').trigger('click')
    expect(body().find('#gcl-tien-thuc-te').element.value).toBe('1.250.000')
    expect(body().find('#gcl-ban-giao').element.value).toBe('1.000.000')
    api.post.mockRejectedValueOnce(loi('Còn 2 hóa đơn chờ chưa thanh toán.', 'CON_HOA_DON_CHO', { chiTiet: { soHoaDonCho: 2 } }))
    await body().find('[data-testid="gcl-dong-ho"]').trigger('submit')
    await flushPromises()
    const huy = body().find('[data-testid="gcl-huy-hd-cho"]')
    expect(huy.text()).toContain('Hủy 2 hóa đơn chờ')
    api.post.mockResolvedValueOnce({ data: { soHoaDonDaHuy: 2 } })
    await huy.trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/giao-ca/150/huy-hoa-don-cho')
    api.post.mockResolvedValueOnce({ data: {} })
    await body().find('[data-testid="gcl-dong-ho"]').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenLastCalledWith('/giao-ca/150/dong-ho', { tienMatThucTe: 1250000, tienBanGiao: 1000000, ghiChu: null })
  })

  it('nhân viên không thấy nút Đóng ca hộ', async () => {
    useAuth().trangThai.nguoiDung = { id: 4, hoTen: 'Phạm Minh Anh', vaiTro: 'NHAN_VIEN' }
    api.get.mockImplementation(async (url) => (url === '/giao-ca/150' ? { data: { ...CHI_TIET } } : { data: [{ ...CHI_TIET }] }))
    const w = ganVao(GiaoCaLichSuPage)
    await flushPromises()
    await w.find('.gcl-dong').trigger('click')
    await flushPromises()
    expect(body().find('[data-testid="gcl-mo-dong-ho"]').exists()).toBe(false)
  })
})

describe('Nhắc giao ca khi quá giờ kết thúc ca', () => {
  const dangMo = { maGiaoCa: 'GC141', tenCa: 'Ca sáng', gioCa: '07:00 - 12:00', ngoaiLich: false, thoiGianNhanCa: '2026-11-04T07:16:40' }

  it('trong giờ ca thì không nhắc; quá 12:00 thì nút Ca làm việc báo "Quá giờ"', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockResolvedValue({ data: { dangMo, tongKet: {}, lichHomNay: [] } })
    _datBayGio(new Date('2026-11-04T11:30:00').getTime())
    const w = ganVao(GiaoCaNut)
    await flushPromises()
    expect(w.text()).not.toContain('Quá giờ')
    _datBayGio(new Date('2026-11-04T12:35:00').getTime())
    await flushPromises()
    expect(w.text()).toContain('Quá giờ')
    expect(useGiaoCa().quaGioCa.value).toEqual({ tenCa: 'Ca sáng', ketThuc: '12:00', phut: 35, caCu: false })
  })

  it('ca ngoài lịch không có giờ kết thúc nên không nhắc trong ngày', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockResolvedValue({ data: { dangMo: { ...dangMo, ngoaiLich: true, gioCa: null }, tongKet: {}, lichHomNay: [] } })
    _datBayGio(new Date('2026-11-04T23:00:00').getTime())
    const w = ganVao(GiaoCaNut)
    await flushPromises()
    expect(w.text()).not.toContain('Quá giờ')
  })
})

describe('Hộp Giao ca: nút tự điền theo hệ thống', () => {
  it('điền tiền mặt thực tế = số hệ thống, để lại cho ca sau = tiền đầu ca; chênh lệch Khớp', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockImplementation(async (url) => {
      if (url === '/giao-ca/hien-tai') {
        return {
          data: {
            dangMo: { maGiaoCa: 'GC141', tenCa: 'Ca sáng', gioCa: '07:00 - 12:00', ngoaiLich: false, thoiGianNhanCa: '2026-11-04T07:16:40', tienDauCa: 1000000 },
            tongKet: { soHoaDon: 3, doanhThu: 900000, tienMat: 600000, chuyenKhoan: 300000, tienDauCa: 1000000, tienMatHeThong: 1600000 },
            lichHomNay: [],
          },
        }
      }
      return { data: [] }
    })
    const w = ganVao(GiaoCaNut)
    await flushPromises()
    await w.find('button[data-testid="gc-nut"]').trigger('click')
    await flushPromises()
    await body().find('[data-testid="gc-tu-dien"]').trigger('click')
    expect(body().find('#gc-thuc-te').element.value).toBe('1.600.000')
    expect(body().find('#gc-ban-giao').element.value).toBe('1.000.000')
    expect(body().find('#gc-ket-form').text()).toContain('Khớp')
  })
})

describe('Trang Vào ca: ca đã giao và ngoài lịch', () => {
  const CA_SANG = { id: 18, tenCa: 'Ca sáng', gioBatDau: '07:00', gioKetThuc: '12:00' }
  const CA_TOI = { id: 19, tenCa: 'Ca tối', gioBatDau: '17:00', gioKetThuc: '22:00' }
  afterEach(() => vi.useRealTimers())

  it('mọi ca trong lịch hôm nay đã giao: ca bị khóa, hiện lý do và khóa nút Bắt đầu ca', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockResolvedValue({ data: { dangMo: null, lichHomNay: [{ ...CA_SANG, daGiao: true }] } })
    const w = ganVao(VaoCaPage)
    await flushPromises()
    expect(w.find('[data-testid="vc-het-ca"]').text()).toContain('nhờ quản lý xếp thêm lịch')
    expect(w.find('[data-testid="vc-bat-dau"]').attributes('disabled')).toBeDefined()
    expect(w.find('[data-testid="vc-ca-can-nhan"]').exists()).toBe(false)
  })

  it('còn ca chưa giao: ca đã giao bị khóa, ca còn lại vẫn nhận được và được nhắc đúng', async () => {
    // Cố định 10:00 để Ca tối (17:00 - 22:00) luôn còn là ca cần nhận, không phụ thuộc giờ máy lúc chạy test
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 4, 10, 0))
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockResolvedValue({ data: { dangMo: null, lichHomNay: [{ ...CA_SANG, daGiao: true }, { ...CA_TOI, daGiao: false }] } })
    const w = ganVao(VaoCaPage)
    await flushPromises()
    expect(w.find('[data-testid="vc-het-ca"]').exists()).toBe(false)
    expect(w.find('[data-testid="vc-ca-da-giao"]').text()).toContain('đã giao')
    expect(w.findAll('input[type="radio"]').filter((i) => i.attributes('disabled') !== undefined)).toHaveLength(1)
    expect(w.find('[data-testid="vc-bat-dau"]').attributes('disabled')).toBeUndefined()
    expect(w.find('[data-testid="vc-ca-can-nhan"]').text()).toContain('Ca tối')
  })

  it('máy chủ chặn vào ngoài lịch: giữ câu báo trên màn hình, không chuyển trang', async () => {
    useAuth().trangThai.nguoiDung = { id: 5, hoTen: 'Vũ Hoàng Nam', vaiTro: 'NHAN_VIEN' }
    api.get.mockResolvedValue({ data: { dangMo: null, lichHomNay: [{ ...CA_TOI, daGiao: false }] } })
    api.post.mockRejectedValue(loi('Hôm nay bạn có lịch Ca tối 17:00 - 22:00. Chỉ nhận ca được từ 15 phút trước giờ bắt đầu.', 'NGOAI_LICH_BI_CHAN'))
    const w = ganVao(VaoCaPage)
    await flushPromises()
    await w.find('#vc-tien-mat').setValue('500000')
    await w.find('#vc-form').trigger('submit')
    await flushPromises()
    expect(w.find('[data-testid="vc-chan-ca"]').text()).toContain('Ca tối 17:00 - 22:00')
    expect(replace).not.toHaveBeenCalled()
  })
})

describe('Tự cập nhật nhanh', () => {
  afterEach(() => vi.useRealTimers())

  it('Lịch sử giao ca: 10 giây tự làm mới, ca mới hiện ra, không hiện "Đang tải", dùng bộ lọc đã áp dụng', async () => {
    vi.useFakeTimers()
    useAuth().trangThai.nguoiDung = { id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' }
    const ca = (id, ma, dang) => ({ id, maGiaoCa: ma, tenNhanVien: 'Lê Quốc Bảo', maNhanVien: 'NV03', dangTrongCa: dang, tienDauCa: 1000000 })
    let dsCa = [ca(1, 'GC141', true)]
    api.get.mockImplementation(async (url) => (url === '/giao-ca' ? { data: dsCa.map((c) => ({ ...c })) } : { data: [] }))
    const w = ganVao(GiaoCaLichSuPage)
    await vi.advanceTimersByTimeAsync(50)
    expect(w.findAll('.gcl-dong')).toHaveLength(1)
    expect(w.find('[data-testid="gcl-cap-nhat-luc"]').exists()).toBe(true)
    const soLanGoi = api.get.mock.calls.filter((c) => c[0] === '/giao-ca').length
    // người khác vừa giao ca, có ca mới
    dsCa = [ca(2, 'GC142', true), ca(1, 'GC141', false)]
    await vi.advanceTimersByTimeAsync(10_000)
    expect(w.findAll('.gcl-dong')).toHaveLength(2)
    expect(w.text()).not.toContain('Đang tải dữ liệu')
    expect(api.get.mock.calls.filter((c) => c[0] === '/giao-ca').length).toBe(soLanGoi + 1)
    // rời trang thì dừng hẹn giờ
    w.unmount()
    gan = gan.filter((x) => x !== w)
    await vi.advanceTimersByTimeAsync(30_000)
    expect(api.get.mock.calls.filter((c) => c[0] === '/giao-ca').length).toBe(soLanGoi + 1)
  })

  it('trang Vào ca: đang chờ ca trước giao thì tự kiểm tra lại, giao xong thì mở khóa và điền sẵn tiền bàn giao', async () => {
    vi.useFakeTimers()
    useAuth().trangThai.nguoiDung = { id: 5, hoTen: 'Vũ Hoàng Nam', vaiTro: 'NHAN_VIEN' }
    let giaoXong = false
    api.get.mockImplementation(async () => ({
      data: giaoXong
        ? { dangMo: null, lichHomNay: [], caKhacDangMo: null, goiYTienDauCa: 1000000, chuyenKhoanCaTruoc: 250000, maCaTruoc: 'GC141', nguoiBanGiao: 'Lê Quốc Bảo' }
        : { dangMo: null, lichHomNay: [], caKhacDangMo: CA_LONG },
    }))
    const w = ganVao(VaoCaPage)
    await vi.advanceTimersByTimeAsync(50)
    expect(w.find('[data-testid="vc-ca-khac"]').exists()).toBe(true)
    giaoXong = true
    await vi.advanceTimersByTimeAsync(10_000)
    expect(w.find('[data-testid="vc-ca-khac"]').exists()).toBe(false)
    expect(w.find('[data-testid="vc-bat-dau"]').attributes('disabled')).toBeUndefined()
    expect(w.find('#vc-tien-mat').element.value).toBe('1.000.000')
    expect(useToast().toasts.map((t) => t.message).join(' ')).toContain('đã giao. Bạn vào ca được rồi')
  })
})

// Review: lịch sử giao ca cho biết nhân viên giữ ca đến hết rồi ca tiếp theo đến ai nhận
describe('Lịch sử giao ca: ca sau đến ai', () => {
  it('ca đã giao hiện người nhận ca tiếp theo; ca đang mở ghi "Đang giữ ca"; chi tiết ca cũng có', async () => {
    useAuth().trangThai.nguoiDung = { id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' }
    const caBao = {
      id: 141, maGiaoCa: 'GC141', idNhanVien: 3, tenNhanVien: 'Lê Quốc Bảo', maNhanVien: 'NV03', dangTrongCa: false,
      thoiGianNhanCa: '2026-11-04T07:16:40', thoiGianKetCa: '2026-11-04T12:00:00', tienDauCa: 1000000, hoaDons: [],
      caSau: { id: 142, maGiaoCa: 'GC142', tenNhanVien: 'Phạm Minh Anh', maNhanVien: 'NV04', thoiGianNhanCa: '2026-11-04T12:05:00' },
    }
    const caAnh = { id: 142, maGiaoCa: 'GC142', idNhanVien: 4, tenNhanVien: 'Phạm Minh Anh', maNhanVien: 'NV04', dangTrongCa: true, thoiGianNhanCa: '2026-11-04T12:05:00', tienDauCa: 1000000, caSau: null }
    api.get.mockImplementation(async (url) => {
      if (url === '/giao-ca') return { data: [caAnh, caBao] }
      if (url === '/giao-ca/141') return { data: caBao }
      return { data: [] }
    })
    const w = ganVao(GiaoCaLichSuPage)
    await flushPromises()
    const o = w.findAll('[data-testid="gcl-ca-sau"]')
    expect(o[0].text()).toBe('Đang giữ ca')
    expect(o[1].text()).toContain('Phạm Minh Anh')
    expect(o[1].text()).toContain('GC142')
    await w.findAll('.gcl-dong')[1].trigger('click')
    await flushPromises()
    expect(body().text()).toContain('Phạm Minh Anh (NV04) · GC142')
  })
})
