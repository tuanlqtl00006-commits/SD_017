// Đợt giảm giá (cô: "làm lại phần chọn"): chọn gọn, thẻ Tổng kết, bảng biến thể đã chọn có ảnh + giá sau giảm + lọc + xóa từng dòng.
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi } from './helpersDanhMuc'
import { tinhGiaSauGiam } from '../src/utils/giamGia'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

const TL = [{ id: 1, ma: 'TL01', ten: '4U (80-84g)', hoatDong: true }, { id: 2, ma: 'TL02', ten: '3U (85-89g)', hoatDong: true }, { id: 92, ma: 'TL00', ten: 'Không áp dụng', hoatDong: true }]
const bt = (id, idSanPham, idMauSac, idTrongLuong, idChuVi, giaBan, hinhAnh = '') => ({ id, ma: `SP0${idSanPham}-${id}`, idSanPham, idMauSac, idTrongLuong, idChuVi, giaBan, soLuongTon: 5, hoatDong: true, hinhAnh })
const BIEN_THE = [
  bt(11, 1, 1, 1, 1, 3000000, '/api/uploads/den.jpg'),
  bt(12, 1, 1, 2, 1, 3000000, '/api/uploads/den.jpg'),
  bt(13, 1, 2, 1, 1, 3200000, '/api/uploads/do.jpg'),
  bt(14, 1, 2, 2, 1, 3200000),
  bt(21, 2, 1, 92, 93, 150000),
]

async function montar() {
  mock = mockApi({ get: (url) => (url === '/bien-the-san-pham' ? BIEN_THE : url === '/thuoc-tinh/trong-luong' ? TL : url === '/dot-giam-gia/ma-moi' ? { ma: 'DGG09' } : undefined) })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/dot-giam-gia', component: { template: '<div/>' } }, { path: '/dot-giam-gia/them', component: { template: '<div/>' } }] })
  router.push('/dot-giam-gia/them'); await router.isReady()
  const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaFormPage.vue')).default
  const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
  await flushPromises()
  return { w, router }
}
const dongSanPham = (w, ten) => w.findAll('.dgg-table tbody tr').find((tr) => tr.text().includes(ten))
const tongKet = (w) => w.get('[data-testid=tong-ket]').text()

beforeEach(() => { document.body.innerHTML = '' })

describe('Giá sau giảm (dùng chung cho danh sách sản phẩm, biến thể, đợt giảm giá)', () => {
  it('làm tròn tới đồng, 0% giữ giá, 100% về 0', () => {
    expect(tinhGiaSauGiam(2500000, 15)).toBe(2125000)
    expect(tinhGiaSauGiam(2500000, 12.5)).toBe(2187500)
    expect(tinhGiaSauGiam(3333333, 10)).toBe(3000000)
    expect(tinhGiaSauGiam(1000000, 0)).toBe(1000000)
    expect(tinhGiaSauGiam(1000000, 100)).toBe(0)
  })
})

describe('Thêm đợt giảm giá: tổng kết + bảng biến thể đã chọn', () => {
  it('tích 1 sản phẩm thì chọn hết 4 biến thể; tổng kết hiện mức giảm, số sản phẩm, số biến thể, ngày bắt đầu / kết thúc', async () => {
    const { w } = await montar()
    expect(w.findAll('.dgg-search select').length).toBe(0) // chỉ một ô tìm kiếm
    await dongSanPham(w, 'Astrox 99').get('input[type=checkbox]').setValue(true); await flushPromises()
    expect(w.findAll('.dgg-chosen-row').length).toBe(4)
    await w.get('#dgg-ten').setValue('Sale vợt')
    await w.get('#dgg-gia-tri').setValue('15')
    await w.get('#dgg-tu-ngay').setValue('2026-10-20')
    await w.get('#dgg-den-ngay').setValue('2026-10-29')
    const tk = tongKet(w)
    expect(tk).toContain('Mức giảm15%')
    expect(tk).toContain('Sản phẩm1')
    expect(tk).toContain('Biến thể4')
    expect(tk).toContain('Ngày bắt đầu20/10/2026')
    expect(tk).toContain('Ngày kết thúc29/10/2026')
    expect(tk).toContain('10 ngày')
    expect(tk).toContain('tổng giá bán 12.400.000 ₫ → sau giảm 10.540.000 ₫')
  })

  it('mỗi dòng có ảnh theo màu, tên sản phẩm, biến thể (màu · trọng lượng · chu vi), giá bán và giá sau giảm đúng', async () => {
    const { w } = await montar()
    await dongSanPham(w, 'Astrox 99').get('input[type=checkbox]').setValue(true); await flushPromises()
    await w.get('#dgg-gia-tri').setValue('15')
    const dong = w.findAll('.dgg-chosen-row').find((r) => r.text().includes('SP01-13'))
    expect(dong.get('img').attributes('src')).toBe('/api/uploads/do.jpg')
    expect(dong.text()).toContain('Astrox 99')
    expect(dong.text()).toContain('Đỏ · 4U (80-84g) · G5')
    expect(dong.text()).toContain('3.200.000 ₫')
    expect(dong.text()).toContain('2.720.000 ₫')
    // chưa có ảnh màu -> ô ảnh trống, không lỗi
    expect(w.findAll('.dgg-chosen-row').find((r) => r.text().includes('SP01-14')).find('.dgg-thumb-empty').exists()).toBe(true)
  })

  it('lọc theo màu / trọng lượng, gỡ cả nhóm đang lọc, xóa từng dòng', async () => {
    const { w } = await montar()
    await dongSanPham(w, 'Astrox 99').get('input[type=checkbox]').setValue(true); await flushPromises()
    const [mau, tl] = w.get('[data-testid=loc-da-chon]').findAll('select')
    await mau.setValue('2') // Đỏ
    expect(w.findAll('.dgg-chosen-row').length).toBe(2)
    await tl.setValue('2') // 3U
    expect(w.findAll('.dgg-chosen-row').length).toBe(1)
    await tl.setValue('')
    await w.findAll('button').find((b) => b.text().includes('Gỡ 2 biến thể đang lọc')).trigger('click'); await flushPromises()
    expect(w.text()).toContain('1 sản phẩm · 2 biến thể')
    expect(w.findAll('.dgg-chosen-row').length).toBe(2) // màu Đỏ hết biến thể -> bộ lọc màu tự bỏ
    await w.findAll('.dgg-chosen-row')[0].get('button[aria-label^="Xóa biến thể"]').trigger('click'); await flushPromises()
    expect(w.text()).toContain('1 sản phẩm · 1 biến thể')
    expect(dongSanPham(w, 'Astrox 99').text()).toContain('Đã chọn 1/4 biến thể')
  })

  it('bấm + rồi bấm nhãn để thêm từng biến thể của sản phẩm khác; lưu gửi đúng danh sách biến thể', async () => {
    const { w, router } = await montar()
    await dongSanPham(w, 'Grip Yonex').get('.dgg-expand').trigger('click')
    await w.findAll('.dgg-vchip').find((c) => c.text().includes('Đen')).trigger('click'); await flushPromises()
    expect(w.text()).toContain('1 sản phẩm · 1 biến thể')
    expect(w.findAll('.dgg-chosen-row')[0].text()).toContain('Đen') // phụ kiện: không hiện "Không áp dụng"
    expect(w.findAll('.dgg-chosen-row')[0].text()).not.toContain('Không áp dụng')
    await w.get('#dgg-ten').setValue('Sale phụ kiện'); await w.get('#dgg-gia-tri').setValue('10'); await w.get('#dgg-den-ngay').setValue('2099-12-31')
    await w.get('form').trigger('submit'); await flushPromises()
    ;[...document.body.querySelectorAll('button')].find((b) => /Xác nhận|Đồng ý|Có/.test(b.textContent))?.click(); await flushPromises()
    const luu = mock.calls.post.find((c) => c[0] === '/dot-giam-gia')
    expect(luu[1]).toMatchObject({ maDot: 'DGG09', phanTramGiamDot: 10, idBienThe: [21] })
    expect(router.currentRoute.value.path).toBe('/dot-giam-gia')
  })

  it('chưa chọn biến thể nào: báo lỗi, không lưu', async () => {
    const { w } = await montar()
    await w.get('#dgg-ten').setValue('X'); await w.get('#dgg-gia-tri').setValue('10'); await w.get('#dgg-den-ngay').setValue('2099-12-31')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.text()).toContain('Vui lòng chọn ít nhất một sản phẩm / biến thể áp dụng.')
    expect(mock.calls.post.length).toBe(0)
  })
})
