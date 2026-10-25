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
})
