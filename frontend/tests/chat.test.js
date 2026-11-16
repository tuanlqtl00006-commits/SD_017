import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { khach, hoTro, noiBo, api } = vi.hoisted(() => ({
  khach: { batDau: vi.fn(), xem: vi.fn(), gui: vi.fn(), gapNhanVien: vi.fn(), ketThuc: vi.fn() },
  hoTro: { danhSach: vi.fn(), xem: vi.fn(), nhan: vi.fn(), gui: vi.fn(), dong: vi.fn() },
  noiBo: { danhSach: vi.fn(), nhanVien: vi.fn(), moRieng: vi.fn(), taoNhom: vi.fn(), xem: vi.fn(), gui: vi.fn() },
  api: { get: vi.fn(), post: vi.fn() },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {}, query: {} }),
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/chatService', () => ({ hoTroKhachApi: khach, hoTroApi: hoTro, chatNoiBoApi: noiBo }))

import KhungChatKhach from '../src/components/chat/KhungChatKhach.vue'
import OSoanTin from '../src/components/chat/OSoanTin.vue'
import HoTroTrucTuyenPage from '../src/components/chat/HoTroTrucTuyenPage.vue'
import ChatNoiBoPage from '../src/components/chat/ChatNoiBoPage.vue'
import { useAuth } from '../src/composables/useAuth'
import { LOI_SDT } from '../src/utils/kiemTra'
import {
  CHE_DO, chuanBiHienThi, docNguoiKhachCu, docPhienKhach, gioNgan, gopTin, idCuoi, locHoiThoai, luuPhienKhach, moTaCheDo,
  nhanNgay, NGUOI_GUI, xemTruocTinCuoi, xoaPhienKhach,
} from '../src/utils/chat'
import { normalizeText } from '../src/utils/text'

const tn = (id, loai, noiDung, extra = {}) => ({ id, loaiNguoiGui: loai, noiDung, tenNguoiGui: null, idNhanVien: null, thoiGian: '2026-10-16T09:00:00', ...extra })
const body = () => new DOMWrapper(document.body)
const loi = (msg, code, loiTheoTruong = null) => Object.assign(new Error(msg), { code, loiTheoTruong })

let gan = []
function ganVao(c, props = {}) {
  const w = mount(c, { props, attachTo: document.body })
  gan.push(w)
  return w
}

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  for (const nhom of [khach, hoTro, noiBo, api]) Object.values(nhom).forEach((f) => f.mockReset())
  api.get.mockResolvedValue({ data: { hoTro: 0, noiBo: 0 } })
})
afterEach(() => {
  gan.forEach((w) => w.unmount())
  gan = []
})

/* ====================== hàm dùng chung ====================== */

describe('utils/chat', () => {
  it('gộp tin mới: bỏ trùng theo id, xếp tăng dần; id cuối làm mốc hỏi lại', () => {
    const cu = [tn(1, 1, 'a'), tn(3, 3, 'c')]
    const moi = gopTin(cu, [tn(2, 1, 'b'), tn(3, 3, 'c sửa')])
    expect(moi.map((t) => t.id)).toEqual([1, 2, 3])
    expect(moi[2].noiDung).toBe('c sửa')
    expect(gopTin(cu, [])).toBe(cu)
    expect(idCuoi(moi)).toBe(3)
    expect(idCuoi([])).toBe(0)
  })

  it('giờ ngắn và nhãn ngày', () => {
    const now = new Date(2026, 9, 16, 15, 0)
    expect(gioNgan('2026-10-16T09:05:00', now)).toBe('09:05')
    expect(gioNgan('2026-10-15T21:00:00', now)).toBe('Hôm qua')
    expect(gioNgan('2026-09-02T08:00:00', now)).toBe('02/09')
    expect(gioNgan('2025-12-31T08:00:00', now)).toBe('31/12/2025')
    expect(gioNgan(null, now)).toBe('')
    expect(nhanNgay('2026-10-16T01:00:00', now)).toBe('Hôm nay')
    expect(nhanNgay('2026-10-15T01:00:00', now)).toBe('Hôm qua')
    expect(nhanNgay('2026-10-11T01:00:00', now)).toBe('Chủ nhật, 11/10/2026')
    expect(nhanNgay('2026-10-14T01:00:00', now)).toBe('Thứ 4, 14/10/2026')
  })

  it('chuẩn bị hiển thị: chia ngày, tin hệ thống, tin nối tiếp cùng người trong 5 phút', () => {
    const now = new Date(2026, 9, 16, 15, 0)
    const ds = [
      tn(1, NGUOI_GUI.KHACH, 'hôm qua', { tenNguoiGui: 'Lan', thoiGian: '2026-10-15T20:00:00' }),
      tn(2, NGUOI_GUI.KHACH, 'chào', { tenNguoiGui: 'Lan', thoiGian: '2026-10-16T09:00:00' }),
      tn(3, NGUOI_GUI.KHACH, 'shop ơi', { tenNguoiGui: 'Lan', thoiGian: '2026-10-16T09:03:00' }),
      tn(4, NGUOI_GUI.KHACH, 'alo', { tenNguoiGui: 'Lan', thoiGian: '2026-10-16T09:20:00' }),
      tn(5, NGUOI_GUI.HE_THONG, 'Nhân viên đã tham gia', { thoiGian: '2026-10-16T09:21:00' }),
      tn(6, NGUOI_GUI.NHAN_VIEN, 'Dạ em chào chị', { idNhanVien: 3, tenNguoiGui: 'Bảo', thoiGian: '2026-10-16T09:21:30' }),
    ]
    const kq = chuanBiHienThi(ds, (t) => t.idNhanVien === 3, now)
    expect(kq.map((d) => d.loai)).toEqual(['ngay', 'tin', 'ngay', 'tin', 'tin', 'tin', 'he-thong', 'tin'])
    expect(kq[0].nhan).toBe('Hôm qua')
    expect(kq[2].nhan).toBe('Hôm nay')
    expect(kq[3].noiTiep).toBe(false) // sang ngày mới
    expect(kq[4].noiTiep).toBe(true) // cùng Lan, cách 3 phút
    expect(kq[5].noiTiep).toBe(false) // cách 17 phút
    expect(kq[7].cuaToi).toBe(true)
    expect(kq[7].noiTiep).toBe(false) // sau tin hệ thống
  })

  it('lọc hội thoại không phân biệt dấu; xem trước tin cuối', () => {
    const ds = [{ tenKhach: 'Nguyễn Thị Lan', sdtKhach: '0901000001' }, { tieuDe: 'Ca tối', tinCuoi: { noiDung: 'Đổi ca nhé' } }]
    expect(locHoiThoai(ds, 'lan', normalizeText)).toHaveLength(1)
    expect(locHoiThoai(ds, '0901', normalizeText)).toHaveLength(1)
    expect(locHoiThoai(ds, 'doi ca', normalizeText)).toEqual([ds[1]])
    expect(locHoiThoai(ds, '  ', normalizeText)).toBe(ds)
    expect(xemTruocTinCuoi(null)).toBe('Chưa có tin nhắn')
    expect(xemTruocTinCuoi(tn(1, NGUOI_GUI.NHAN_VIEN, 'ok', { idNhanVien: 3 }), 3)).toBe('Bạn: ok')
    expect(xemTruocTinCuoi(tn(1, NGUOI_GUI.TRO_LY_AO, 'xin chào'))).toBe('Trợ lý: xin chào')
    expect(xemTruocTinCuoi(tn(1, NGUOI_GUI.NHAN_VIEN, 'ok', { idNhanVien: 4, tenNguoiGui: 'Phạm Minh Anh' }), 3, true)).toBe('Anh: ok')
    expect(moTaCheDo(CHE_DO.CHO_NHAN_VIEN).nhan).toBe('Chờ nhân viên')
    expect(moTaCheDo(99).nhan).toBe('Trợ lý ảo')
  })

  it('phiên chat của khách lưu trong trình duyệt; xóa phiên vẫn nhớ tên + SĐT', () => {
    expect(docPhienKhach()).toBeNull()
    luuPhienKhach({ id: 5, ma: 'abc', hoTen: 'Lan', sdt: '0901000001' })
    expect(docPhienKhach()).toEqual({ id: 5, ma: 'abc', hoTen: 'Lan', sdt: '0901000001' })
    xoaPhienKhach()
    expect(docPhienKhach()).toBeNull()
    expect(docNguoiKhachCu()).toEqual({ hoTen: 'Lan', sdt: '0901000001' })
    localStorage.setItem('fs_ho_tro_khach', '{hỏng')
    expect(docPhienKhach()).toBeNull()
    localStorage.setItem('fs_ho_tro_khach', JSON.stringify({ id: '5', ma: 'x' }))
    expect(docPhienKhach()).toBeNull()
  })
})

/* ====================== ô soạn tin ====================== */

describe('OSoanTin', () => {
  it('Enter gửi (đã bỏ khoảng trắng), Shift + Enter không gửi', async () => {
    const w = ganVao(OSoanTin, { modelValue: '  chào shop  ', 'onUpdate:modelValue': (v) => w.setProps({ modelValue: v }) })
    await w.find('textarea').trigger('keydown', { key: 'Enter', shiftKey: true })
    expect(w.emitted('gui')).toBeUndefined()
    await w.find('textarea').trigger('keydown', { key: 'Enter' })
    expect(w.emitted('gui')[0]).toEqual(['chào shop'])
  })

  it('quá 1000 ký tự: báo lỗi, khóa nút gửi; rỗng cũng không gửi được', async () => {
    const w = ganVao(OSoanTin, { modelValue: 'a'.repeat(1001) })
    expect(w.text()).toContain('Tin nhắn tối đa 1000 ký tự (đang có 1001)')
    expect(w.find('button').attributes('disabled')).toBeDefined()
    await w.setProps({ modelValue: '   ' })
    expect(w.find('button').attributes('disabled')).toBeDefined()
    await w.setProps({ modelValue: 'ok' })
    expect(w.find('button').attributes('disabled')).toBeUndefined()
  })

  it('bị khóa thì hiện lý do thay cho ô nhập', () => {
    const w = ganVao(OSoanTin, { lyDoKhoa: 'Cuộc trò chuyện đã kết thúc.' })
    expect(w.find('textarea').exists()).toBe(false)
    expect(w.text()).toContain('Cuộc trò chuyện đã kết thúc.')
  })
})

/* ====================== khung chat của khách ====================== */

const CHAO = tn(1, NGUOI_GUI.TRO_LY_AO, 'Xin chào Lan! Mình là trợ lý ảo', { tenNguoiGui: 'Trợ lý ảo' })
const GOI_Y = ['Vợt dưới 3 triệu', 'Gặp nhân viên']

describe('KhungChatKhach', () => {
  it('chưa có cuộc chat: báo lỗi dưới từng ô, không gọi máy chủ', async () => {
    const w = ganVao(KhungChatKhach)
    await w.find('form').trigger('submit')
    expect(w.find('#kck-ten-loi').text()).toBe('Hãy nhập họ tên.')
    expect(w.find('#kck-sdt-loi').text()).toContain('Hãy nhập số điện thoại')
    await w.find('#kck-ten').setValue('Lan')
    await w.find('#kck-sdt').setValue('012345')
    expect(w.find('#kck-ten-loi').exists()).toBe(false)
    expect(w.find('#kck-sdt-loi').text()).toBe(LOI_SDT)
    expect(khach.batDau).not.toHaveBeenCalled()
  })

  it('bắt đầu -> lời chào + gợi ý, lưu phiên; gửi tin thì trợ lý trả lời', async () => {
    khach.batDau.mockResolvedValue({ id: 7, ma: 'MA-BI-MAT', cheDo: 1, tinNhan: [CHAO], goiY: GOI_Y })
    const w = ganVao(KhungChatKhach)
    await w.find('#kck-ten').setValue('  nguyễn   lan ')
    await w.find('#kck-sdt').setValue('0901 000 001')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(khach.batDau).toHaveBeenCalledWith('nguyễn lan', '0901000001')
    expect(docPhienKhach()).toEqual({ id: 7, ma: 'MA-BI-MAT', hoTen: 'nguyễn lan', sdt: '0901000001' })
    expect(w.text()).toContain('Xin chào Lan!')
    expect(w.findAll('.kck-chip').map((b) => b.text())).toEqual(GOI_Y)

    khach.gui.mockResolvedValue({ id: 7, cheDo: 1, tinNhan: [tn(2, 1, 'vợt yonex', { tenNguoiGui: 'Lan' }), tn(3, 3, 'Sản phẩm còn hàng…')], goiY: ['Gặp nhân viên'] })
    await w.find('.kck-chip').trigger('click')
    await flushPromises()
    expect(khach.gui).toHaveBeenCalledWith(7, 'MA-BI-MAT', 'Vợt dưới 3 triệu')
    expect(w.text()).toContain('Sản phẩm còn hàng…')
    expect(w.findAll('.ktn-dong.is-toi')).toHaveLength(1)

    // Bấm gợi ý "Gặp nhân viên" -> chuyển nhân viên (không gửi như câu hỏi)
    khach.gapNhanVien.mockResolvedValue({ id: 7, cheDo: 2, tinNhan: [tn(4, 4, 'Đang kết nối với nhân viên.')], goiY: [] })
    await w.find('.kck-chip').trigger('click')
    await flushPromises()
    expect(khach.gapNhanVien).toHaveBeenCalledWith(7, 'MA-BI-MAT')
    expect(w.text()).toContain('Đang chờ nhân viên…')
    expect(w.find('.kck-chip').exists()).toBe(false)
  })

  it('tải lại trang: mở tiếp cuộc chat cũ; cuộc chat không còn thì quay về form, giữ tên + SĐT', async () => {
    luuPhienKhach({ id: 7, ma: 'MA', hoTen: 'Lan', sdt: '0901000001' })
    khach.xem.mockResolvedValue({ id: 7, cheDo: 3, tenNhanVien: 'Lê Quốc Bảo', tinNhan: [CHAO, tn(2, 2, 'Em Bảo đây ạ', { tenNguoiGui: 'Lê Quốc Bảo' })], goiY: [] })
    let w = ganVao(KhungChatKhach)
    await flushPromises()
    expect(khach.xem).toHaveBeenCalledWith(7, 'MA')
    expect(w.text()).toContain('Nhân viên Lê Quốc Bảo')
    expect(w.text()).toContain('Em Bảo đây ạ')
    w.unmount()
    gan = []

    khach.xem.mockRejectedValue(loi('Không tìm thấy cuộc trò chuyện.', 'KHONG_TIM_THAY_HOI_THOAI'))
    w = ganVao(KhungChatKhach)
    await flushPromises()
    expect(docPhienKhach()).toBeNull()
    expect(w.find('#kck-ten').element.value).toBe('Lan')
    expect(w.find('#kck-sdt').element.value).toBe('0901000001')
    expect(w.text()).toContain('Cuộc trò chuyện cũ không còn')
  })

  it('gửi lỗi: trả lại nội dung vào ô; cuộc chat đã kết thúc thì hiện nút trò chuyện mới', async () => {
    luuPhienKhach({ id: 7, ma: 'MA', hoTen: 'Lan', sdt: '0901000001' })
    khach.xem.mockResolvedValue({ id: 7, cheDo: 2, tinNhan: [CHAO], goiY: [] })
    const w = ganVao(KhungChatKhach)
    await flushPromises()
    khach.gui.mockRejectedValueOnce(loi('Bạn gửi tin nhanh quá', 'QUA_NHIEU_YEU_CAU'))
    await w.find('textarea').setValue('alo shop')
    await w.find('textarea').trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(w.text()).toContain('Bạn gửi tin nhanh quá')
    expect(w.find('textarea').element.value).toBe('alo shop')

    khach.gui.mockRejectedValueOnce(loi('Cuộc trò chuyện đã kết thúc.', 'DA_KET_THUC'))
    khach.xem.mockResolvedValue({ id: 7, cheDo: 4, tinNhan: [tn(9, 4, 'Nhân viên đã kết thúc cuộc trò chuyện.')], goiY: [] })
    await w.find('textarea').trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(w.text()).toContain('Cuộc trò chuyện đã kết thúc.')
    expect(w.find('textarea').exists()).toBe(false)
    await w.find('.kck-ket-thuc button').trigger('click')
    expect(docPhienKhach()).toBeNull()
    expect(w.find('#kck-ten').exists()).toBe(true)
  })

  it('kết thúc cuộc chat phải xác nhận', async () => {
    luuPhienKhach({ id: 7, ma: 'MA', hoTen: 'Lan', sdt: '0901000001' })
    khach.xem.mockResolvedValue({ id: 7, cheDo: 1, tinNhan: [CHAO], goiY: GOI_Y })
    const w = ganVao(KhungChatKhach)
    await flushPromises()
    await w.find('[aria-label="Kết thúc trò chuyện"]').trigger('click')
    expect(khach.ketThuc).not.toHaveBeenCalled()
    khach.ketThuc.mockResolvedValue({ id: 7, cheDo: 4, tinNhan: [tn(2, 4, 'Khách đã kết thúc cuộc trò chuyện.')], goiY: [] })
    const nut = body().findAll('.modal button').find((b) => b.text() === 'Kết thúc')
    await nut.trigger('click')
    await flushPromises()
    expect(khach.ketThuc).toHaveBeenCalledWith(7, 'MA')
    expect(w.text()).toContain('Khách đã kết thúc cuộc trò chuyện.')
  })
})

/* ====================== trang hỗ trợ của nhân viên ====================== */

const HT = (id, cheDo, extra = {}) => ({
  id, tenKhach: `Khách ${id}`, sdtKhach: '0901000001', cheDo, idNhanVienPhuTrach: null, tenNhanVienPhuTrach: null,
  ngayCapNhat: '2026-10-16T09:00:00', tinCuoi: tn(10 + id, 1, `Tin của khách ${id}`), chuaDoc: 0, ...extra,
})

describe('HoTroTrucTuyenPage', () => {
  function dangNhap(vaiTro = 'NHAN_VIEN', id = 3) {
    useAuth().trangThai.nguoiDung = { id, hoTen: id === 3 ? 'Lê Quốc Bảo' : 'Nguyễn Hoàng Long', vaiTro }
  }

  it('mặc định lọc "Chờ nhân viên", hiện số đếm và tin chưa đọc', async () => {
    dangNhap()
    hoTro.danhSach.mockResolvedValue({ hoiThoai: [HT(1, 2, { chuaDoc: 3 })], dem: { tatCa: 4, cho: 1, cuaToi: 2, troLy: 1, daDong: 0 } })
    const w = ganVao(HoTroTrucTuyenPage)
    await flushPromises()
    expect(hoTro.danhSach).toHaveBeenCalledWith('cho')
    expect(w.find('.htt-loc-nut.active').text()).toContain('Chờ nhân viên')
    expect(w.find('.htt-loc-dem.is-gap').text()).toBe('1')
    expect(w.find('.htt-so').text()).toBe('3')
    expect(w.text()).toContain('Chọn một cuộc trò chuyện')
  })

  it('mở cuộc đang chờ: có nút Nhận hỗ trợ; trả lời thì gửi lên máy chủ', async () => {
    dangNhap()
    hoTro.danhSach.mockResolvedValue({ hoiThoai: [HT(1, 2, { chuaDoc: 2 })], dem: { cho: 1 } })
    hoTro.xem.mockResolvedValue({ ...HT(1, 2), khachHang: null, tinNhan: [tn(1, 1, 'shop ơi', { tenNguoiGui: 'Khách 1' })] })
    const w = ganVao(HoTroTrucTuyenPage)
    await flushPromises()
    await w.find('.htt-muc').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('shop ơi')
    expect(w.text()).toContain('Khách chưa có tài khoản')
    expect(w.findAll('button').some((b) => b.text().includes('Nhận hỗ trợ'))).toBe(true)
    expect(w.find('.htt-so').exists()).toBe(false) // đã đọc
    hoTro.gui.mockResolvedValue(tn(2, 2, 'Dạ em chào chị', { idNhanVien: 3, tenNguoiGui: 'Lê Quốc Bảo' }))
    await w.find('textarea').setValue('Dạ em chào chị')
    await w.find('textarea').trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(hoTro.gui).toHaveBeenCalledWith(1, 'Dạ em chào chị')
    expect(w.find('.ktn-dong.is-toi').text()).toContain('Dạ em chào chị')
  })

  it('cuộc do nhân viên khác hỗ trợ: nhân viên bị khóa ô nhập, quản lý được tiếp quản', async () => {
    const cuocKhac = HT(1, 3, { idNhanVienPhuTrach: 4, tenNhanVienPhuTrach: 'Phạm Minh Anh' })
    hoTro.danhSach.mockResolvedValue({ hoiThoai: [cuocKhac], dem: {} })
    hoTro.xem.mockResolvedValue({ ...cuocKhac, tinNhan: [] })
    dangNhap()
    let w = ganVao(HoTroTrucTuyenPage)
    await flushPromises()
    await w.find('.htt-muc').trigger('click')
    await flushPromises()
    expect(w.find('textarea').exists()).toBe(false)
    expect(w.text()).toContain('Đang do Phạm Minh Anh hỗ trợ.')
    expect(w.findAll('button').some((b) => b.text().includes('Nhận hỗ trợ'))).toBe(false)
    expect(w.findAll('button').some((b) => b.text().includes('Kết thúc'))).toBe(false)
    w.unmount()
    gan = []

    dangNhap('QUAN_LY', 1)
    w = ganVao(HoTroTrucTuyenPage)
    await flushPromises()
    await w.find('.htt-muc').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Bấm "Nhận hỗ trợ" để tiếp quản.')
    hoTro.nhan.mockResolvedValue({ ...cuocKhac, idNhanVienPhuTrach: 1, tenNhanVienPhuTrach: 'Nguyễn Hoàng Long', tinNhan: [tn(5, 4, 'Nhân viên Nguyễn Hoàng Long đã tiếp quản')] })
    await w.findAll('button').find((b) => b.text().includes('Nhận hỗ trợ')).trigger('click')
    await flushPromises()
    expect(hoTro.nhan).toHaveBeenCalledWith(1)
    expect(w.find('textarea').exists()).toBe(true)
  })
})

/* ====================== chat nội bộ ====================== */

describe('ChatNoiBoPage', () => {
  const NHOM = { id: 1, loai: 3, nhomChung: true, tieuDe: 'Cả cửa hàng', thanhVien: [{ id: 3, hoTen: 'Lê Quốc Bảo' }, { id: 4, hoTen: 'Phạm Minh Anh' }], tinCuoi: null, chuaDoc: 2 }

  it('tự mở nhóm chung, tin của người khác hiện tên, tin của mình bên phải', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    noiBo.danhSach.mockResolvedValue([NHOM])
    noiBo.xem.mockResolvedValue({ ...NHOM, tinNhan: [tn(1, 2, 'Chào cả nhà', { idNhanVien: 4, tenNguoiGui: 'Phạm Minh Anh' }), tn(2, 2, 'Chào chị', { idNhanVien: 3, tenNguoiGui: 'Lê Quốc Bảo' })] })
    const w = ganVao(ChatNoiBoPage)
    await flushPromises()
    expect(noiBo.xem).toHaveBeenCalledWith(1)
    expect(w.find('.cnb-chat-ten').text()).toContain('2 thành viên: Bạn, Anh')
    expect(w.find('.ktn-ten').text()).toBe('Phạm Minh Anh')
    expect(w.find('.ktn-dong.is-toi').text()).toContain('Chào chị')
    expect(w.find('.cnb-so').exists()).toBe(false)
  })

  it('tạo nhóm: báo lỗi dưới từng ô, chọn thành viên rồi tạo', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    noiBo.danhSach.mockResolvedValue([])
    noiBo.nhanVien.mockResolvedValue([{ id: 4, ma: 'NV04', hoTen: 'Phạm Minh Anh', vaiTro: 'Nhân viên' }, { id: 1, ma: 'NV01', hoTen: 'Nguyễn Hoàng Long', vaiTro: 'Quản lý' }])
    const w = ganVao(ChatNoiBoPage)
    await flushPromises()
    await w.findAll('button').find((b) => b.text().includes('Tạo nhóm')).trigger('click')
    await flushPromises()
    await body().find('#cnb-form-nhom').trigger('submit')
    expect(body().text()).toContain('Hãy đặt tên nhóm.')
    expect(body().text()).toContain('Hãy chọn ít nhất 1 nhân viên khác.')
    expect(noiBo.taoNhom).not.toHaveBeenCalled()
    await body().find('#cnb-ten-nhom').setValue(' Ca tối ')
    await body().findAll('.modal input[type=checkbox]')[0].setValue(true)
    noiBo.taoNhom.mockResolvedValue({ id: 9, loai: 3, tieuDe: 'Ca tối', thanhVien: [], tinNhan: [tn(1, 4, 'Lê Quốc Bảo đã tạo nhóm "Ca tối".')] })
    await body().find('#cnb-form-nhom').trigger('submit')
    await flushPromises()
    expect(noiBo.taoNhom).toHaveBeenCalledWith('Ca tối', [4])
    expect(w.text()).toContain('đã tạo nhóm "Ca tối"')
  })
})
