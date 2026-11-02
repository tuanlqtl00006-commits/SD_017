import { beforeEach, describe, expect, it, vi } from 'vitest'
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
  document.body.innerHTML = ''
  api.get.mockReset()
  api.post.mockReset()
  api.get.mockResolvedValue({ data: DU_LIEU })
})

describe('Thống kê so sánh', () => {
  it('tải theo tháng, hiện so với kỳ trước và cùng kỳ năm trước, biểu đồ tăng trưởng và sản phẩm', async () => {
    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/thong-ke/so-sanh', { params: { kieu: 'THANG', ky: expect.stringMatching(/^\d{4}-\d{2}$/) } })
    const the = w.findAll('[data-test="ss-chi-so"]')
    expect(the).toHaveLength(8)
    expect(w.find('svg[aria-label="Biểu đồ so sánh nhóm chỉ số doanh thu"]').exists()).toBe(true)
    expect(the[0].text()).toContain('1.450.000')
    expect(the[0].text()).toContain('▲ 20,8%')
    expect(the[0].text()).toContain('so với cùng kỳ Tháng 10/2025')
    expect(the[4].find('.ss-xau').text()).toContain('▲ 50%')
    expect(w.find('[data-test="ss-xu-huong"]').text()).toContain('+100%')
    expect(w.find('[data-test="ss-san-pham"]').text()).toContain('▲ 200%')
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

  it('tài khoản nào đang đăng nhập thì tự điền email của tài khoản đó và chỉ gửi về đúng email đó', async () => {
    api.get.mockResolvedValue({
      data: {
        ...DU_LIEU,
        emailNguoiDangNhap: 'long.nh@footstyle.vn',
      },
    })
    api.post.mockResolvedValue({ data: { daGui: true, giaLap: false, email: 'long.nh@footstyle.vn' } })

    const w = mount(ThongKeSoSanh, { attachTo: document.body })
    await flushPromises()
    await w.find('[data-test="ss-email"]').trigger('click')

    // Tự động điền đúng email của tài khoản đang đăng nhập
    expect(w.find('#ss-email-o').element.value).toBe('long.nh@footstyle.vn')

    // Thử đổi sang email người khác -> chặn gửi
    await w.find('#ss-email-o').setValue('nguoikhac@gmail.com')
    await w.find('.ss-email').trigger('submit')
    expect(w.text()).toContain('Chỉ được gửi báo cáo về email của tài khoản đang đăng nhập (long.nh@footstyle.vn).')
    expect(api.post).not.toHaveBeenCalled()

    // Gửi đúng về email của tài khoản đang đăng nhập -> thành công
    await w.find('#ss-email-o').setValue('long.nh@footstyle.vn')
    await w.find('.ss-email').trigger('submit')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith(
      '/thong-ke/so-sanh/gui-email',
      expect.objectContaining({
        email: 'long.nh@footstyle.vn',
      })
    )
    w.unmount()
  })
})
