import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi, THUOC_TINH } from './helpersDanhMuc'
import { laDanhMucVot } from '../src/utils/danhMuc'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))
beforeEach(() => { document.body.innerHTML = '' })

const DOTS = [{ id: 1, maDot: 'DGG008', tenDot: 'A', phanTramGiamDot: 10, ngayBatDau: '2026-10-01T00:00:00', ngayKetThuc: '2026-10-30T00:00:00', trangThai: 1 }, { id: 2, maDot: 'DGG007', tenDot: 'B', phanTramGiamDot: 5, ngayBatDau: '2026-09-01T00:00:00', ngayKetThuc: '2026-09-30T00:00:00', trangThai: 0 }]
async function montar(get) {
  mock = mockApi({ get })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/dot-giam-gia', component: { template: '<div/>' } }, { path: '/dot-giam-gia/them', component: { template: '<div/>' } }] })
  router.push('/dot-giam-gia/them'); await router.isReady()
  const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaFormPage.vue')).default
  const w = mount(Comp, { global: { plugins: [router] } }); await flushPromises(); return w
}
describe('Trang thêm đợt giảm giá', () => {
  it('mã lấy từ backend, ngắn (DGG009)', async () => {
    const w = await montar((url) => (url === '/dot-giam-gia' ? DOTS : url === '/dot-giam-gia/ma-moi' ? { ma: 'DGG009' } : undefined))
    expect(w.get('#dgg-ma').element.value).toBe('DGG009')
  })
  it('backend lỗi: dùng mã dự phòng DGG009 từ các mã đã có', async () => {
    const w = await montar((url) => { if (url === '/dot-giam-gia/ma-moi') throw new Error('x'); return url === '/dot-giam-gia' ? DOTS : undefined })
    expect(w.get('#dgg-ma').element.value).toBe('DGG009')
  })
})
describe('Chi tiết sản phẩm', () => {
  const mk = async (item) => {
    const Comp = (await import('../src/components/sanPham/SanPhamDetailModal.vue')).default
    mount(Comp, { props: { item, bienThes: [], thuocTinh: { ...THUOC_TINH, 'danh-muc': THUOC_TINH['danh-muc'] } }, attachTo: document.body }); await flushPromises()
    return document.body.textContent
  }
  const base = { id: 1, ma: 'SP1', ten: 'X', idDanhMuc: 1, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, hoatDong: true }
  it('vợt: hiện độ cứng, điểm cân bằng', async () => {
    const t = await mk({ ...base, idDoCung: 1, idDiemCanBang: 1 })
    expect(t).toContain('Độ cứng'); expect(t).toContain('Điểm cân bằng')
  })
  it('phụ kiện: ẩn độ cứng, điểm cân bằng', async () => {
    const t = await mk({ ...base, idDanhMuc: 5, idDoCung: 90, idDiemCanBang: 91 })
    expect(t).not.toContain('Độ cứng'); expect(t).not.toContain('Điểm cân bằng'); expect(t).not.toContain('Không áp dụng')
  })
})
