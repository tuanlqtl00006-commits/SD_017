// Hóa đơn (cô: "chưa có QR", "gộp thành một cục -> tách ra từng cái"): QR ở trang chi tiết + bản in, bố cục tách khối, Xuất Excel mở được.
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { buildXlsx } from '../src/utils/exportExcel'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock[k](...a) }) }))

const CHI_TIET = {
  hoaDon: { id: 1, maHoaDon: 'HD16', loaiDon: 2, tenLoaiDon: 'Trực tuyến', trangThai: 1, ngayTao: '2026-10-08T14:39:33', tenNhanVien: 'Nguyễn Hoàng Long', phuongThucThanhToan: 'Tiền mặt', tongTien: 3861000, soTienGiam: 0, phiVanChuyen: 30000, thanhTien: 3891000, donViVanChuyen: 'GHN' },
  khachHang: { ten: 'Vũ Khánh Linh', sdt: '0901000006', email: 'linh@mail.vn' },
  giaoHang: { hoTenNguoiNhan: 'Vũ Khánh Linh', sdtNguoiNhan: '0901000006', diaChi: 'Số 1, Phường Hàng Bạc, Hà Nội' },
  sanPham: [{ id: 1, maSpct: 'SP01-DEN-4U-G5', tenSanPham: 'Astrox 99', mauSac: 'Đen', trongLuong: '4U', chuVi: 'G5', soLuong: 1, donGia: 3861000, giaGoc: 3861000, phanTramGiam: 0, thanhTien: 3861000, hinhAnh: '/api/uploads/den.jpg' }],
  thanhToan: [],
  lichSu: [{ id: 1, trangThai: 1, hanhDong: 'Tạo đơn hàng', thoiGian: '2026-10-08T14:39:33', nguoiThaoTac: 'NV01' }],
  daThanhToan: 0,
}

async function montarChiTiet() {
  mock = { get: vi.fn(async () => ({ data: CHI_TIET })) }
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/hoa-don', component: { template: '<div/>' } }, { path: '/hoa-don/:id', component: { template: '<div/>' } }] })
  router.push('/hoa-don/1'); await router.isReady()
  const Comp = (await import('../src/components/HoaDonChiTiet.vue')).default
  const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
  await flushPromises(); await new Promise((r) => setTimeout(r, 30)); await flushPromises()
  return w
}

beforeEach(() => { document.body.innerHTML = '' })

describe('Chi tiết hóa đơn', () => {
  it('có mã QR chứa mã hóa đơn và các khối riêng (trạng thái, thông tin đơn, khách hàng, giao hàng, thanh toán, sản phẩm)', async () => {
    const w = await montarChiTiet()
    const qr = w.get('img[alt="Mã QR hóa đơn HD16"]')
    expect(qr.attributes('src').startsWith('data:image/png;base64,')).toBe(true)
    const tieuDe = w.findAll('h2.ad-card-title').map((h) => h.text())
    for (const t of ['Trạng thái đơn hàng', 'Thông tin đơn hàng', 'Mã QR hóa đơn', 'Thông tin khách hàng', 'Thông tin giao hàng', 'Lịch sử thanh toán']) {
      expect(tieuDe).toContain(t)
    }
    expect(tieuDe.some((t) => t.startsWith('Danh sách sản phẩm'))).toBe(true)
    expect(w.findAll('section.ad-card').length).toBeGreaterThanOrEqual(7)
  })

  it('bản in có QR ở góc trên và tách thành từng phần', async () => {
    const w = await montarChiTiet()
    let html = ''
    const cuaSo = { document: { write: (x) => { html += x }, close: () => {} } }
    const open = vi.spyOn(window, 'open').mockReturnValue(cuaSo)
    await w.findAll('button').find((b) => b.text().includes('In hóa đơn')).trigger('click')
    expect(open).toHaveBeenCalled()
    expect(html).toContain('class="qr"')
    expect(html).toContain('src="data:image/png;base64,')
    for (const t of ['Thông tin đơn hàng', 'Khách hàng', 'Giao hàng', 'Sản phẩm', 'Tổng tiền']) expect(html).toContain(`<h2>${t}</h2>`)
    open.mockRestore()
  })
})

describe('Xuất Excel danh sách hóa đơn', () => {
  it('file .xlsx đúng cấu trúc (zip có workbook, sheet, styles), cột tiền là SỐ có định dạng #,##0', async () => {
    const blob = buildXlsx({
      sheetName: 'Hóa đơn',
      columns: [{ header: 'Mã HD', key: 'ma' }, { header: 'Tổng tiền TT (₫)', key: 'tien', money: true }],
      rows: [{ ma: 'HD16', tien: 3861000 }],
    })
    const bytes = new Uint8Array(await blob.arrayBuffer())
    expect([...bytes.slice(0, 4)]).toEqual([0x50, 0x4b, 0x03, 0x04]) // chữ ký zip "PK"
    const text = new TextDecoder().decode(bytes)
    for (const f of ['[Content_Types].xml', 'xl/workbook.xml', 'xl/worksheets/sheet1.xml', 'xl/styles.xml']) expect(text).toContain(f)
    expect(text).toContain('<c r="B2" s="2"><v>3861000</v></c>')
    expect(text).toContain('numFmtId="3"')
    expect(text).toContain('Tổng tiền TT (₫)')
  })
})
