import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi, THUOC_TINH, SAN_PHAM } from './helpersDanhMuc'
import { layTen, layLuaChon } from '../src/utils/text'
import { laDanhMucVot, idKhongApDung, laKhongApDung } from '../src/utils/danhMuc'
import { taoMaDot } from '../src/constants/dotGiamGia'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))
beforeEach(() => { mock = mockApi(); document.body.innerHTML = '' })

describe('utils', () => {
  it('laDanhMucVot', () => {
    expect(laDanhMucVot('Vợt cầu lông')).toBe(true)
    expect(laDanhMucVot('Vợt cầu lông người mới')).toBe(true)
    for (const t of ['Phụ kiện', 'Túi - balo', 'Quần áo thể thao', 'Quả cầu lông', '', null, undefined]) expect(laDanhMucVot(t)).toBe(false)
  })
  it('Không áp dụng: ẩn khỏi ô chọn, hiển thị —', () => {
    const ds = THUOC_TINH['do-cung']
    expect(layLuaChon(ds, null).map((x) => x.id)).toEqual([1])
    expect(layLuaChon(ds, 90).map((x) => x.id)).toEqual([1])
    expect(layTen(ds, 90)).toBe('—'); expect(layTen(ds, 1)).toBe('Cứng')
    expect(idKhongApDung(ds)).toBe(90); expect(idKhongApDung([])).toBe(null)
    expect(laKhongApDung({ ten: 'KHÔNG ÁP DỤNG' })).toBe(true)
  })
  it('taoMaDot: ngắn, theo số thứ tự', () => {
    expect(taoMaDot(['DGG001', 'DGG008'])).toBe('DGG009')
    expect(taoMaDot([])).toBe('DGG001')
    expect(taoMaDot(['DGG008', 'ABC', 'dgg012'])).toBe('DGG013')
    expect(taoMaDot(['DGG008']).length).toBe(6)
  })
})

describe('Modal biến thể', () => {
  const montar = async (props = {}) => {
    const Comp = (await import('../src/components/sanPham/BienTheFormModal.vue')).default
    const options = (slug, cur) => layLuaChon(THUOC_TINH[slug], cur)
    const laVotFn = (id) => laDanhMucVot(THUOC_TINH['danh-muc'].find((d) => d.id === SAN_PHAM.find((p) => p.id === id).idDanhMuc).ten)
    const idKadFn = (slug) => idKhongApDung(THUOC_TINH[slug])
    const w = mount(Comp, { props: { sanPhams: SAN_PHAM, all: [], options, laVotFn, idKadFn, ...props }, attachTo: document.body })
    await flushPromises(); return w
  }
  const q = (sel) => document.body.querySelector(sel)
  const set = async (sel, v) => { const e = q(sel); e.value = String(v); e.dispatchEvent(new Event('change')); e.dispatchEvent(new Event('input')); await flushPromises() }
  it('sản phẩm vợt: có trọng lượng + chu vi, bắt buộc chọn', async () => {
    const w = await montar()
    await set('#bt-san-pham', 1)
    expect(q('#bt-tl')).toBeTruthy(); expect(q('#bt-cv')).toBeTruthy()
    await set('#bt-mau', 1); await set('#bt-gia', 100000)
    q('#bt-form').dispatchEvent(new Event('submit')); await flushPromises()
    expect(w.emitted('save')).toBeFalsy()
    expect(document.body.textContent).toContain('Chọn trọng lượng.')
    w.unmount()
  })
  it('sản phẩm phụ kiện: ẩn trọng lượng + chu vi, lưu gửi "Không áp dụng"', async () => {
    const w = await montar()
    await set('#bt-san-pham', 2)
    expect(q('#bt-tl')).toBeFalsy(); expect(q('#bt-cv')).toBeFalsy()
    await set('#bt-mau', 1); await set('#bt-gia', 50000)
    q('#bt-form').dispatchEvent(new Event('submit')); await flushPromises()
    const ev = w.emitted('save'); expect(ev).toBeTruthy()
    expect(ev[0][0]).toMatchObject({ idSanPham: 2, idMauSac: 1, idTrongLuong: 92, idChuVi: 93, giaBan: 50000 })
    expect(ev[0][0].ma).toBe('SP002-DEN')
    w.unmount()
  })
  it('phụ kiện: trùng màu với biến thể đã có thì vẫn nhận ra là trùng bộ', async () => {
    const all = [{ id: 5, ma: 'SP002-DO', idSanPham: 2, idMauSac: 2, idTrongLuong: 92, idChuVi: 93 }, { id: 6, ma: 'SP002-DEN', idSanPham: 2, idMauSac: 1, idTrongLuong: 92, idChuVi: 93 }]
    const w = await montar({ all, item: { id: 5, ma: 'SP002-DO', idSanPham: 2, idMauSac: 2, idTrongLuong: 92, idChuVi: 93, giaBan: 1000, soLuongTon: 1 } })
    await set('#bt-mau', 1)
    q('#bt-form').dispatchEvent(new Event('submit')); await flushPromises()
    expect(document.body.textContent).toContain('Sản phẩm đã có biến thể với màu sắc này.')
    w.unmount()
  })
  it('sửa biến thể phụ kiện đổi sang sản phẩm vợt: bắt chọn lại trọng lượng', async () => {
    const w = await montar({ item: { id: 5, ma: 'SP002-DEN', idSanPham: 2, idMauSac: 1, idTrongLuong: 92, idChuVi: 93, giaBan: 50000, soLuongTon: 1 } })
    expect(q('#bt-tl')).toBeFalsy()
    await set('#bt-san-pham', 1)
    expect(q('#bt-tl')).toBeTruthy(); expect(q('#bt-tl').value).toBe('')
    q('#bt-form').dispatchEvent(new Event('submit')); await flushPromises()
    expect(w.emitted('save')).toBeFalsy()
    w.unmount()
  })
})

describe('Hóa đơn: cột SĐT KH', () => {
  it('hiện SĐT, thiếu thì hiện —', async () => {
    mock = mockApi({ get: (url) => {
      if (url === '/hoa-don') return { content: [
        { id: 1, maHoaDon: 'HD1', tenKhachHang: 'Khách vãng lai', sdtKhachHang: null, maNhanVien: 'NV1', tongTien: 1000, loaiDon: 'Tại quầy', ngayTao: '2026-10-08T10:00:00', trangThai: 1 },
        { id: 2, maHoaDon: 'HD2', tenKhachHang: 'Vũ Khánh Linh', sdtKhachHang: '0901000006', maNhanVien: 'NV9', tongTien: 2000, loaiDon: 'Tại quầy', ngayTao: '2026-10-03T10:00:00', trangThai: 6 },
      ], totalElements: 2 }
      if (url === '/hoa-don/dem-trang-thai') return { tatCa: 2 }
    } })
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div/>' } }, { path: '/hoa-don/:id', component: { template: '<div/>' } }] })
    router.push('/'); await router.isReady()
    const Comp = (await import('../src/components/HoaDonManager.vue')).default
    const w = mount(Comp, { global: { plugins: [router] } }); await flushPromises()
    const rows = w.findAll('tbody tr')
    expect(rows.length).toBe(2)
    const header = w.findAll('thead th').map((t) => t.text()); const i = header.indexOf('SĐT KH'); expect(i).toBeGreaterThan(-1)
    expect(rows[0].findAll('td')[i].text()).toBe('—')
    expect(rows[1].findAll('td')[i].text()).toBe('0901000006')
  })
})

describe('Trang thuộc tính ẩn mục Không áp dụng', () => {
  it('danh sách độ cứng không có dòng Không áp dụng', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/san-pham/do-cung', meta: { attr: 'do-cung' }, component: { template: '<div/>' } }] })
    router.push('/san-pham/do-cung'); await router.isReady()
    const Comp = (await import('../src/components/ThuocTinhManager.vue')).default
    const w = mount(Comp, { global: { plugins: [router] } }); await flushPromises()
    expect(w.text()).toContain('Cứng')
    expect(w.text()).not.toContain('Không áp dụng')
  })
})
