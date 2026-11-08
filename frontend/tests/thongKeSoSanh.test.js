import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {}, query: {} }),
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}))
vi.mock('../src/services/api', () => ({ default: api }))

import ThongKeSoSanh from '../src/components/thongKe/ThongKeSoSanh.vue'
import { buildXlsxNhieuSheet } from '../src/utils/exportExcel'

const chiSo = (nhan, dt) => ({
  ky: { ma: nhan, nhan, tuNgay: '2026-10-01', denNgay: '2026-10-31' },
  doanhThuThucTe: dt, doanhThuDuKien: dt + 300000, soDon: 4, soHoanThanh: 2, soHuy: 1, giaTriTrungBinh: dt / 2, soSanPhamBan: 3, soKhachMua: 1,
})
const DU_LIEU = {
  kieu: 'THANG',
  ky: { ma: '2026-10', nhan: 'Tháng 10/2026', tuNgay: '2026-10-01', denNgay: '2026-10-31' },
  hienTai: chiSo('Tháng 10/2026', 1450000),
  kyTruoc: chiSo('Tháng 9/2026', 1200000),
  cungKyNamTruoc: chiSo('Tháng 10/2025', 725000),
  tangSoVoiKyTruoc: { doanhThuThucTe: 20.8, doanhThuDuKien: 16.7, soDon: 0, soHoanThanh: 0, soHuy: 0, giaTriTrungBinh: 20.8, soSanPhamBan: 0, soKhachMua: 0 },
  tangSoVoiCungKy: { doanhThuThucTe: 100, doanhThuDuKien: 70.1, soDon: 0, soHoanThanh: 0, soHuy: 50, giaTriTrungBinh: 100, soSanPhamBan: 0, soKhachMua: 0 },
  xuHuong: Array.from({ length: 12 }, (_, i) => ({ nhan: `T${i + 1}`, ma: `2026-${String(i + 1).padStart(2, '0')}`, doanhThu: i === 9 ? 1450000 : 0, doanhThuNamTruoc: i === 9 ? 725000 : 0, soDon: 0, tangTruong: i === 9 ? 100 : 0 })),
  chiTietKy: [{ nhan: '1/10', ma: '2026-10-01', doanhThu: 0, doanhThuDuKien: 0, soDon: 0, doanhThuNamTruoc: 0 }],
  sanPham: [{ maSanPham: 'SP01', tenSanPham: 'Vợt Yonex Astrox 99 Pro', soLuong: 3, soLuongKyTruoc: 1, doanhThu: 1450000, tangTruong: 200 }],
}

beforeEach(() => {
  // Cố định "hôm nay" là 05/11/2026 để kỳ mặc định là Tháng 10/2026 và Tháng 11/2026
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 5, 10, 0, 0))
  document.body.innerHTML = ''
  api.get.mockReset()
  api.post.mockReset()
  api.get.mockResolvedValue({ data: DU_LIEU })
})

afterEach(() => {
  vi.useRealTimers()
})

const sanPhamThang10 = [
  { maSanPham: 'SP01', tenSanPham: 'Vợt Yonex Astrox 99 Pro', thuongHieu: 'Yonex', soLuong: 3, doanhThu: 900000, hoaDonIds: [1, 2], khachHangIds: [7] },
  { maSanPham: 'SP02', tenSanPham: 'Vợt Lining Axforce 80', thuongHieu: 'Lining', soLuong: 1, doanhThu: 550000, hoaDonIds: [2], khachHangIds: [7] },
]

describe('Thống kê so sánh', () => {
  it('so sánh kỳ mới nhất với kỳ liền trước: thẻ tóm tắt, biểu đồ, bảng chỉ số và sản phẩm', async () => {
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/thong-ke/so-sanh', { params: { kieu: 'THANG', ky: '2026-11' } })
    expect(w.find('[data-test="ss-dang-so-sanh"]').text()).toContain('Đang so sánh Tháng 11/2026 với Tháng 10/2026')
    expect(w.find('[data-test="ss-luu-y-ky"]').text()).toContain('Tháng 11/2026 chưa kết thúc (đã qua 5/30 ngày)')

    // Không còn cộng dồn "tổng doanh thu" / "doanh thu dự kiến" của nhiều kỳ
    expect(w.text()).not.toContain('Doanh thu dự kiến')
    expect(w.text()).not.toContain('Tổng doanh thu kỳ')

    const the = w.findAll('[data-test="ss-kpi"]')
    expect(the).toHaveLength(6)
    expect(the[0].text()).toContain('Doanh thu')
    expect(the[0].text()).toContain('▼ 100%')
    expect(the[0].text()).toContain('Tháng 10/2026: 1.450.000')

    const dong = w.findAll('[data-test="ss-chi-so"]')
    expect(dong).toHaveLength(9)
    expect(dong[0].text()).toContain('1.450.000')
    expect(dong[0].text()).toContain('-1.450.000')
    expect(dong[0].find('.ss-trend-pill').classes()).toContain('is-xau')
    // Đơn hủy giảm là tốt
    expect(dong[7].text()).toContain('Đơn hủy')
    expect(dong[7].find('.ss-trend-pill').classes()).toContain('is-tot')
    // Tỷ lệ hủy 1/4 đơn = 25%, giảm 25 điểm %
    expect(dong[8].text()).toContain('25%')
    expect(dong[8].text()).toContain('-25 điểm %')

    // Không lặp khối phân tích thừa bên dưới KPI (đã đủ số ở thẻ KPI và bảng chỉ số)
    expect(w.find('[data-test="ss-vi-sao"]').exists()).toBe(false)

    expect(w.find('svg[aria-label="Biểu đồ so sánh nhóm chỉ số doanh thu"]').exists()).toBe(true)
    expect(w.find('[data-test="ss-xu-huong"]').text()).toContain('+100%')
    expect(w.find('[data-test="ss-san-pham"]').text()).toContain('↓ Giảm (-100%)')
    w.unmount()
  })

  it('lọc theo thương hiệu: đếm đúng số đơn có sản phẩm khớp lọc, ẩn chỉ số tính theo cả hóa đơn và gửi email đúng bộ lọc', async () => {
    api.get.mockResolvedValue({
      data: { ...DU_LIEU, hienTai: { ...chiSo('Tháng 10/2026', 1450000), sanPhamTrongKy: sanPhamThang10 } },
    })
    api.post.mockResolvedValue({ data: { daGui: true, giaLap: false, email: 'ql@gmail.com' } })
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()

    const bangTh = w.find('[data-test="ss-thuong-hieu"]').text()
    expect(bangTh).toContain('Yonex')
    expect(bangTh).toContain('Lining')

    await w.find('.ss-brand-select').setValue('Yonex')
    await flushPromises()

    const dong = w.findAll('[data-test="ss-chi-so"]')
    expect(dong).toHaveLength(5)
    expect(dong[0].text()).toContain('900.000')
    expect(dong[1].text()).toContain('Đơn hoàn thành')
    expect(dong[1].findAll('td')[1].text()).toBe('2')
    expect(w.find('[data-test="ss-thuong-hieu"]').text()).not.toContain('Lining')

    await w.find('[data-test="ss-email"]').trigger('click')
    await w.find('#ss-email-o').setValue('ql@gmail.com')
    await w.find('.ss-email').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(
      '/thong-ke/so-sanh/gui-email',
      expect.objectContaining({ thuongHieu: 'Yonex', dsKy: ['2026-10', '2026-11'] })
    )
    w.unmount()
  })

  it('đổi sang quý / năm thì gọi lại đúng kỳ', async () => {
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    await w.findAll('button').find((b) => b.text() === 'Theo quý').trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenLastCalledWith('/thong-ke/so-sanh', { params: { kieu: 'QUY', ky: expect.stringMatching(/^\d{4}-Q[1-4]$/) } })
    await w.findAll('button').find((b) => b.text() === 'Theo năm').trigger('click')
    await flushPromises()
    expect(api.get).toHaveBeenLastCalledWith('/thong-ke/so-sanh', { params: { kieu: 'NAM', ky: expect.stringMatching(/^\d{4}$/) } })
    w.unmount()
  })

  it('gửi email: kiểm tra email rồi gửi đúng kỳ', async () => {
    api.post.mockResolvedValue({ data: { daGui: false, giaLap: true, email: 'ql@gmail.com' } })
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    await w.find('[data-test="ss-email"]').trigger('click')
    await w.find('#ss-email-o').setValue('sai-email')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('Email không hợp lệ.')
    expect(api.post).not.toHaveBeenCalled()
    await w.find('#ss-email-o').setValue('ql@gmail.com')
    await w.find('.ss-email').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/thong-ke/so-sanh/gui-email', expect.objectContaining({ kieu: 'THANG', email: 'ql@gmail.com' }))
    w.unmount()
  })

  it('file Excel nhiều sheet có đủ sheet và tên không trùng', async () => {
    const blob = buildXlsxNhieuSheet([
      { sheetName: 'Tổng hợp', columns: [{ header: 'A', key: 'a' }], rows: [{ a: 1 }] },
      { sheetName: 'Tổng hợp', columns: [{ header: 'B', key: 'b' }], rows: [{ b: 'x' }] },
    ])
    const text = new TextDecoder().decode(new Uint8Array(await blob.arrayBuffer()))
    expect(text).toContain('xl/worksheets/sheet2.xml')
    expect(text).toContain('<sheet name="Tổng hợp" sheetId="1"')
    expect(text).toContain('<sheet name="Tổng hợp 2" sheetId="2"')
  })

  it('cảnh báo khi chọn kỳ trùng hoặc kỳ tương lai và cho phép đặt về kỳ hiện tại', async () => {
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    const selectsThang = w.findAll('.ss-period-select--month')
    expect(selectsThang.length).toBeGreaterThanOrEqual(2)
    // Chọn trùng tháng giữa Kỳ 1 và Kỳ 2
    const valKy2 = selectsThang[1].element.value
    await selectsThang[0].setValue(valKy2)
    await flushPromises()
    expect(w.find('[data-test="ss-canh-bao-ky"]').exists()).toBe(true)
    expect(w.find('[data-test="ss-canh-bao-ky"]').text()).toContain('Đang chọn trùng')
    // Bấm nút Về kỳ hiện tại để chuẩn hóa lại các kỳ
    await w.find('[data-test="ss-ve-hien-tai"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-test="ss-canh-bao-ky"]').exists()).toBe(false)
    w.unmount()
  })

  it('gửi email đơn giản: kiểm tra đầy đủ các case khó (dấu chấm đầu/cuối, trùng email, quá 5 email, mã độc trong ghi chú)', async () => {
    api.post.mockResolvedValue({ data: { daGui: true, giaLap: false, email: 'ql1@gmail.com, ql2@gmail.com' } })
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    await w.find('[data-test="ss-email"]').trigger('click')

    // Case 1: Để trống hoặc toàn khoảng trắng
    await w.find('#ss-email-o').setValue('   ')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('Vui lòng nhập địa chỉ email nhận báo cáo.')

    // Case 2: Dấu chấm ở đầu/cuối phần tên hoặc dấu gạch ngang đầu tên miền
    for (const sai of ['.abc@gmail.com', 'abc.@gmail.com', 'abc@-gmail.com', 'a..b@gmail.com']) {
      await w.find('#ss-email-o').setValue(sai)
      await w.find('.ss-email').trigger('submit')
      expect(w.text()).toContain('Email không hợp lệ.')
    }

    // Case 3: Nhập trùng email (không phân biệt hoa thường)
    await w.find('#ss-email-o').setValue('ql1@gmail.com, QL1@gmail.com')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('Có địa chỉ email bị trùng lặp trong danh sách nhận.')

    // Case 4: Quá 5 địa chỉ email -> báo lỗi
    await w.find('#ss-email-o').setValue('a1@g.co, a2@g.co, a3@g.co, a4@g.co, a5@g.co, a6@g.co')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('Chỉ hỗ trợ gửi tối đa 5 địa chỉ email mỗi lần.')

    // Case 5: Ghi chú chứa mã script độc hại
    await w.find('#ss-email-o').setValue('ql1@gmail.com')
    await w.find('#ss-email-note').setValue('<script>alert(1)</script>')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('Ghi chú không được chứa mã HTML hoặc kịch bản.')
    expect(api.post).not.toHaveBeenCalled()

    // Case 6: Gửi báo cáo hợp lệ tới 2 email kèm ghi chú ngắn
    await w.find('#ss-email-o').setValue('ql1@gmail.com, ql2@gmail.com')
    await w.find('#ss-email-note').setValue('Đề xuất nhập thêm dòng Nike Air')
    await w.find('.ss-email').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(
      '/thong-ke/so-sanh/gui-email',
      expect.objectContaining({
        kieu: 'THANG',
        email: 'ql1@gmail.com, ql2@gmail.com',
        ghiChu: 'Đề xuất nhập thêm dòng Nike Air',
      })
    )
    w.unmount()
  })

  it('tự động điền sẵn email tài khoản đang đăng nhập nhưng vẫn cho phép gửi tới email bất kỳ (chỉ chặn nhân viên đã nghỉ việc)', async () => {
    api.get.mockResolvedValue({
      data: {
        ...DU_LIEU,
        emailNguoiDangNhap: 'long.nh@footstyle.vn',
        emailNgungHoatDong: ['nghiviec@footstyle.vn'],
      },
    })
    api.post.mockResolvedValue({ data: { daGui: true, giaLap: false, email: 'nguoikhac@gmail.com' } })

    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    await w.find('[data-test="ss-email"]').trigger('click')

    // Tự động điền đúng email của tài khoản đang đăng nhập để tiện bấm gửi ngay
    expect(w.find('#ss-email-o').element.value).toBe('long.nh@footstyle.vn')

    // Chặn gửi cho nhân viên đã nghỉ việc
    await w.find('#ss-email-o').setValue('nghiviec@footstyle.vn')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('đã ngừng làm việc tại cửa hàng')
    expect(api.post).not.toHaveBeenCalled()

    // Cho phép nhập trực tiếp bất kỳ email Gmail nào mà không cần tạo nhân viên mới hay đăng nhập lại
    await w.find('#ss-email-o').setValue('nguoikhac@gmail.com')
    await w.find('.ss-email').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(
      '/thong-ke/so-sanh/gui-email',
      expect.objectContaining({
        email: 'nguoikhac@gmail.com',
      })
    )
    w.unmount()
  })

  it('kiểm tra validate khoảng ngày tùy chọn (Từ ngày > Đến ngày, quá 366 ngày) và xóa kỳ so sánh', async () => {
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()

    // Xóa bớt 1 kỳ khi đang có 2 kỳ -> còn 1 kỳ và khóa nút xóa cuối cùng
    const nutXoa = w.findAll('.ss-remove-period-btn')
    expect(nutXoa).toHaveLength(2)
    expect(nutXoa[0].attributes('disabled')).toBeUndefined()
    await nutXoa[0].trigger('click')
    await flushPromises()
    const nutXoaConLai = w.findAll('.ss-remove-period-btn')
    expect(nutXoaConLai).toHaveLength(1)
    expect(nutXoaConLai[0].attributes('disabled')).toBeDefined()

    // Chuyển sang Tùy chọn khoảng ngày
    await w.findAll('button').find((b) => b.text() === 'Tùy chọn khoảng ngày').trigger('click')
    await flushPromises()

    const oNgay = w.findAll('.ss-period-date')
    expect(oNgay.length).toBeGreaterThanOrEqual(4)

    // Đặt Từ ngày > Đến ngày
    await oNgay[0].setValue('2025-05-20')
    await oNgay[1].setValue('2025-05-10')
    await flushPromises()
    expect(w.find('[data-test="ss-canh-bao-ky"]').exists()).toBe(true)
    expect(w.find('[data-test="ss-canh-bao-ky"]').text()).toContain('không được lớn hơn')

    // Đặt khoảng ngày vượt quá 366 ngày
    await oNgay[0].setValue('2024-01-01')
    await oNgay[1].setValue('2025-06-01')
    await flushPromises()
    expect(w.find('[data-test="ss-canh-bao-ky"]').exists()).toBe(true)
    expect(w.find('[data-test="ss-canh-bao-ky"]').text()).toContain('tối đa 366 ngày')

    w.unmount()
  })
})
