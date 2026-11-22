import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// Cô review 14/11 [20:24–23:17] phần chat / trợ lý ảo:
//  - gợi ý sản phẩm trả về từng THẺ (ảnh, tên, giá), bấm thẻ sang trang chi tiết;
//  - "tư vấn về đơn hàng đã có": mở danh sách đơn của khách để chọn;
//  - phiên có nhân viên kết thúc thì khách đánh giá (mức độ, đã giải quyết chưa).
const { khach, api, push, cuaHang } = vi.hoisted(() => ({
  khach: { batDau: vi.fn(), xem: vi.fn(), gui: vi.fn(), gapNhanVien: vi.fn(), ketThuc: vi.fn(), danhGia: vi.fn() },
  api: { get: vi.fn(), post: vi.fn() },
  push: vi.fn(),
  cuaHang: { getDanhSachSanPham: vi.fn() },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/shop', params: {}, query: {} }),
  useRouter: () => ({ push }),
}))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/chatService', () => ({ hoTroKhachApi: khach, hoTroApi: {}, chatNoiBoApi: {} }))
vi.mock('../src/services/cuaHangService', () => ({ cuaHangService: cuaHang }))

import KhungChatKhach from '../src/components/chat/KhungChatKhach.vue'
import { xoaKhoSanPhamChat } from '../src/components/chat/khoSanPhamChat'
import { luuPhienKhach, tachLienKet, tachTheSanPham, xemTruocTinCuoi } from '../src/utils/chat'

const tn = (id, loai, noiDung) => ({ id, loaiNguoiGui: loai, noiDung, tenNguoiGui: loai === 3 ? 'Trợ lý ảo' : 'Lan', idNhanVien: null, thoiGian: '2026-11-15T09:00:00' })
const SP = [
  { id: 12, ten: 'Vợt Yonex Nanoflare 700', anhChinh: '/a.webp', minPrice: 2_601_000, originalPrice: 2_890_000, maxDiscountPercent: 10 },
  { id: 15, ten: 'Vợt Lining Axforce 20', anhChinh: '/b.webp', minPrice: 950_000, originalPrice: 950_000, maxDiscountPercent: 0 },
]

let gan = []
function moKhung() {
  const w = mount(KhungChatKhach, { attachTo: document.body })
  gan.push(w)
  return w
}

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  xoaKhoSanPhamChat()
  for (const f of [...Object.values(khach), ...Object.values(api), push, cuaHang.getDanhSachSanPham]) f.mockReset()
  cuaHang.getDanhSachSanPham.mockResolvedValue(SP)
  luuPhienKhach({ id: 7, ma: 'MA', hoTen: 'Lan', sdt: '0901000001' })
})
afterEach(() => {
  gan.forEach((w) => w.unmount())
  gan = []
})

describe('Tách thẻ sản phẩm khỏi nội dung tin', () => {
  it('dòng cuối "[[SP:..]]" thành danh sách id; tin thường giữ nguyên; xem trước tin cuối không lộ dòng đánh dấu', () => {
    expect(tachTheSanPham('Có 2 sản phẩm:\nBấm vào thẻ.\n[[SP:12,15]]')).toEqual({ chu: 'Có 2 sản phẩm:\nBấm vào thẻ.', sanPham: [12, 15] })
    expect(tachTheSanPham('Xin chào [[SP:1]] ở giữa câu')).toEqual({ chu: 'Xin chào [[SP:1]] ở giữa câu', sanPham: [] })
    // Nhân viên dán đường dẫn trang sản phẩm: không hiện URL thô, đổi thành thẻ sản phẩm (góp ý 22/11)
    expect(tachTheSanPham('http://localhost:5173/shop/san-pham/71')).toEqual({ chu: '', sanPham: [71] })
    expect(tachTheSanPham('Mẫu này ạ:\nhttps://footstyle.vn/shop/san-pham/71?mau=Do\n/shop/san-pham/9 cũng được')).toEqual({ chu: 'Mẫu này ạ:\ncũng được', sanPham: [71, 9] })
    expect(tachTheSanPham('Như trên [[SP:5]]\n[[SP:71]]')).toEqual({ chu: 'Như trên [[SP:5]]', sanPham: [71] })
    expect(tachTheSanPham('xem http://localhost:5173/shop/san-pham/71\n[[SP:71,8]]')).toEqual({ chu: 'xem', sanPham: [71, 8] })
    // Đường dẫn khác giữ nguyên trong chữ, khung chat vẽ thành liên kết bấm được
    expect(tachTheSanPham('Chính sách: http://localhost:5173/shop/chinh-sach')).toEqual({ chu: 'Chính sách: http://localhost:5173/shop/chinh-sach', sanPham: [] })
    expect(tachLienKet('Xem https://a.vn/x?y=1. nhé')).toEqual([{ chu: 'Xem ' }, { url: 'https://a.vn/x?y=1' }, { chu: '. nhé' }])
    expect(tachLienKet('không có link')).toEqual([{ chu: 'không có link' }])
    expect(xemTruocTinCuoi(tn(1, 3, 'Gợi ý:\n[[SP:12]]'))).toBe('Trợ lý: Gợi ý:')
  })
})

describe('Thẻ sản phẩm trong khung chat', () => {
  it('trợ lý gợi ý: hiện thẻ ảnh, tên, giá (giá giảm + giá gốc); bấm thẻ sang trang chi tiết', async () => {
    khach.xem.mockResolvedValue({ id: 7, cheDo: 1, tinNhan: [tn(1, 3, 'Có 2 sản phẩm phù hợp:\nBấm vào thẻ để xem chi tiết.\n[[SP:12,15]]')], goiY: [] })
    const w = moKhung()
    await flushPromises()
    expect(w.text()).not.toContain('[[SP')
    const the = w.findAll('.tspc-the')
    expect(the).toHaveLength(2)
    expect(the[0].text()).toContain('Vợt Yonex Nanoflare 700')
    expect(the[0].text()).toContain('2.601.000')
    expect(the[0].text()).toContain('2.890.000')
    expect(the[0].text()).toContain('-10%')
    expect(the[0].find('img').attributes('src')).toBe('/a.webp')
    await the[1].trigger('click')
    expect(push).toHaveBeenCalledWith('/shop/san-pham/15')
    expect(cuaHang.getDanhSachSanPham).toHaveBeenCalledTimes(1)
  })
})

describe('Tư vấn về đơn hàng đã đặt', () => {
  it('bấm gợi ý: mở danh sách đơn theo SĐT của cuộc chat; chọn đơn thì gửi "Tư vấn về đơn HD..."', async () => {
    khach.xem.mockResolvedValue({ id: 7, cheDo: 1, tinNhan: [tn(1, 3, 'Xin chào')], goiY: ['Tư vấn đơn hàng đã đặt', 'Gặp nhân viên'] })
    api.get.mockResolvedValue({ data: { sdt: '0901000001', don: [
      { maHoaDon: 'HD000987', tenTrangThai: 'Đã xác nhận', loaiDon: 'Trực tuyến', thanhTien: 1_385_400 },
      { maHoaDon: 'HD000984', tenTrangThai: 'Chờ xác nhận', loaiDon: 'Trực tuyến', thanhTien: 632_200 },
    ] } })
    khach.gui.mockResolvedValue({ id: 7, cheDo: 1, tinNhan: [tn(2, 1, 'Tư vấn về đơn HD000987'), tn(3, 3, 'Đơn HD000987 - Trạng thái: Đã xác nhận')], goiY: [] })
    const w = moKhung()
    await flushPromises()
    await w.findAll('.kck-chip').find((b) => b.text() === 'Tư vấn đơn hàng đã đặt').trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/cua-hang/tra-cuu-sdt', { params: { sdt: '0901000001' } })
    expect(khach.gui).not.toHaveBeenCalled()
    const don = w.findAll('.kck-don')
    expect(don).toHaveLength(2)
    expect(don[0].text()).toContain('HD000987')
    expect(don[0].text()).toContain('1.385.400')
    await don[0].trigger('click')
    await flushPromises()
    expect(khach.gui).toHaveBeenCalledWith(7, 'MA', 'Tư vấn về đơn HD000987')
    expect(w.find('[data-testid="kck-chon-don"]').exists()).toBe(false)
  })

  it('nút đơn hàng ở đầu khung dùng được cả khi đang chat với nhân viên; chưa có đơn thì báo rõ', async () => {
    khach.xem.mockResolvedValue({ id: 7, cheDo: 3, tenNhanVien: 'Long', tinNhan: [tn(1, 4, 'Nhân viên Long đã tham gia')], goiY: [] })
    api.get.mockRejectedValue(Object.assign(new Error('Không tìm thấy'), { status: 404 }))
    const w = moKhung()
    await flushPromises()
    await w.find('[data-testid="kck-nut-don"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-testid="kck-chon-don"]').text()).toContain('Chưa có đơn hàng nào với số 0901000001')
  })
})

describe('Đánh giá sau phiên có nhân viên', () => {
  it('đã kết thúc + máy chủ mời đánh giá: chọn mức độ và đã giải quyết chưa mới gửi được; gửi xong cảm ơn', async () => {
    khach.xem.mockResolvedValue({ id: 7, cheDo: 4, canDanhGia: true, daDanhGia: false, tinNhan: [tn(1, 4, 'Nhân viên Long đã kết thúc cuộc trò chuyện.')], goiY: [] })
    khach.danhGia.mockResolvedValue({ id: 7, cheDo: 4, canDanhGia: false, daDanhGia: true, tinNhan: [tn(2, 4, 'Khách đánh giá: Tốt · Đã giải quyết yêu cầu')], goiY: [] })
    const w = moKhung()
    await flushPromises()
    const khoi = w.find('[data-testid="kck-danh-gia"]')
    expect(khoi.exists()).toBe(true)
    await w.find('[data-testid="kck-gui-danh-gia"]').trigger('click')
    expect(khach.danhGia).not.toHaveBeenCalled()
    expect(w.text()).toContain('Bạn chọn mức độ hài lòng')
    await khoi.findAll('.kck-dg-nut').find((b) => b.text().includes('Tốt')).trigger('click')
    await khoi.findAll('.kck-dg-nut').find((b) => b.text() === 'Đã giải quyết').trigger('click')
    await khoi.find('textarea').setValue('Nhân viên nhiệt tình')
    await w.find('[data-testid="kck-gui-danh-gia"]').trigger('click')
    await flushPromises()
    expect(khach.danhGia).toHaveBeenCalledWith(7, 'MA', 'TOT', true, 'Nhân viên nhiệt tình')
    expect(w.find('[data-testid="kck-danh-gia"]').exists()).toBe(false)
    expect(w.find('[data-testid="kck-da-danh-gia"]').text()).toContain('Cảm ơn bạn đã đánh giá')
  })

  it('chỉ chat với trợ lý ảo (máy chủ không mời đánh giá) thì không hiện form', async () => {
    khach.xem.mockResolvedValue({ id: 7, cheDo: 4, canDanhGia: false, daDanhGia: false, tinNhan: [tn(1, 4, 'Khách đã kết thúc cuộc trò chuyện.')], goiY: [] })
    const w = moKhung()
    await flushPromises()
    expect(w.find('[data-testid="kck-danh-gia"]').exists()).toBe(false)
    expect(w.find('.kck-ket-thuc').exists()).toBe(true)
  })
})
