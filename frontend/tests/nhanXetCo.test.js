import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import QRCode from 'qrcode'
import MoneyInput from '../src/components/common/MoneyInput.vue'

describe('Ô nhập tiền (theo nhận xét của cô: giá bán phải có định dạng)', () => {
  it('hiển thị dấu chấm ngăn cách hàng nghìn và phát ra số', async () => {
    const w = mount(MoneyInput, { props: { modelValue: 1500000 } })
    expect(w.get('input').element.value).toBe('1.500.000')
    await w.get('input').setValue('2.750.000đ')
    expect(w.emitted('update:modelValue').at(-1)).toEqual([2750000])
    await w.get('input').setValue('')
    expect(w.emitted('update:modelValue').at(-1)).toEqual([''])
  })
  it('để trống khi chưa có giá', () => {
    const w = mount(MoneyInput, { props: { modelValue: '' } })
    expect(w.get('input').element.value).toBe('')
  })
})

describe('Mã QR hóa đơn', () => {
  it('tạo được ảnh QR từ mã hóa đơn', async () => {
    const url = await QRCode.toDataURL('HD01', { width: 240, margin: 1 })
    expect(url.startsWith('data:image/png;base64,')).toBe(true)
  })
})

/* ----- Đợt giảm giá: chọn sản phẩm / biến thể gọn hơn (nhận xét của cô) ----- */
import { beforeEach, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi } from './helpersDanhMuc'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))
beforeEach(() => { document.body.innerHTML = '' })

const BIEN_THE = [
  { id: 11, ma: 'SP001-DEN', idSanPham: 1, idMauSac: 1, idTrongLuong: 1, idChuVi: 1, giaBan: 3000000, soLuongTon: 5, hoatDong: true },
  { id: 12, ma: 'SP001-DO', idSanPham: 1, idMauSac: 2, idTrongLuong: 1, idChuVi: 1, giaBan: 3200000, soLuongTon: 5, hoatDong: true },
]
async function montarDot() {
  mock = mockApi({ get: (url) => (url === '/bien-the-san-pham' ? BIEN_THE : url === '/dot-giam-gia/ma-moi' ? { ma: 'DGG01' } : undefined) })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/dot-giam-gia', component: { template: '<div/>' } }, { path: '/dot-giam-gia/them', component: { template: '<div/>' } }] })
  router.push('/dot-giam-gia/them'); await router.isReady()
  const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaFormPage.vue')).default
  const w = mount(Comp, { global: { plugins: [router] } }); await flushPromises(); return w
}

describe('Đợt giảm giá: chọn sản phẩm / biến thể', () => {
  it('chỉ còn một ô tìm kiếm (không còn 3 ô lọc + nút Tìm kiếm)', async () => {
    const w = await montarDot()
    expect(w.findAll('.dgg-search select').length).toBe(0)
    expect(w.find('.dgg-search input[type=text]').exists()).toBe(true)
  })
  it('tích sản phẩm thì chọn mọi biến thể; bảng dưới mỗi dòng một biến thể và xóa được từng dòng', async () => {
    // Sửa 12/10 theo task: bảng "đã chọn" là MỖI DÒNG MỘT BIẾN THỂ (trước đây gom theo sản phẩm thành các nhãn)
    const w = await montarDot()
    await w.get('.dgg-table tbody input[type=checkbox]').setValue(true); await flushPromises()
    expect(w.text()).toContain('Đã chọn 2/2 biến thể')
    expect(w.text()).toContain('1 sản phẩm · 2 biến thể')
    expect(w.findAll('.dgg-chosen-row').length).toBe(2)
    await w.get('.dgg-chosen-row button[aria-label^="Xóa biến thể"]').trigger('click'); await flushPromises()
    expect(w.text()).toContain('1 sản phẩm · 1 biến thể')
    expect(w.findAll('.dgg-chosen-row').length).toBe(1)
  })
  it('bấm nút + hoặc dòng sản phẩm thì đẩy biến thể xuống bảng dưới (không xổ dòng con ở bảng trên)', async () => {
    const w = await montarDot()
    await w.get('.dgg-expand').trigger('click'); await flushPromises()
    expect(w.find('.dgg-sub-row').exists()).toBe(false)
    expect(w.text()).toContain('1 sản phẩm · 2 biến thể')
    expect(w.findAll('.dgg-chosen-row').length).toBe(2)
  })
})
