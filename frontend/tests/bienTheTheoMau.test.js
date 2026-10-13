// Biến thể nhóm theo màu ở trang Thêm sản phẩm (theo nhận xét của cô + 2 video mẫu):
// đúng tích số (4 x 3 x 2 = 24), nhóm theo màu, mặc định + Áp dụng, ảnh theo màu, định dạng giá, báo lỗi rõ.
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi, THUOC_TINH } from './helpersDanhMuc'
import {
  CHE_DO_HIEN_THI_MAC_DINH,
  apDungMacDinh,
  congThucSoBienThe,
  kiemTraBienThe,
  loiGiaBan,
  loiSoLuong,
  nhomTheoMau,
  payloadAnhTheoMau,
  payloadBienThe,
  taoBienThe,
} from '../src/utils/bienThe'
import { mauHex } from '../src/utils/mau'
import { useToast } from '../src/composables/useToast'
const thongBao = () => useToast().toasts.map((x) => x.message).join(' | ')

/* ===================== Hàm thuần ===================== */

describe('Tạo biến thể: đúng tích số và nhóm theo màu', () => {
  it('vợt 4 màu x 3 trọng lượng x 2 chu vi = 24 biến thể, không trùng, đúng thứ tự Màu -> Trọng lượng -> Chu vi', () => {
    const rows = taoBienThe([1, 2, 3, 4], [10, 11, 12], [20, 21])
    expect(rows.length).toBe(24)
    expect(new Set(rows.map((r) => r.key)).size).toBe(24)
    expect(rows.slice(0, 3).map((r) => [r.idMauSac, r.idTrongLuong, r.idChuVi])).toEqual([[1, 10, 20], [1, 10, 21], [1, 11, 20]])
    expect(rows.every((r) => r.chon)).toBe(true)
    // đếm lại theo từng màu: mỗi màu 3 x 2 = 6 dòng
    const nhom = nhomTheoMau(rows, [1, 2, 3, 4])
    expect(nhom.map((g) => g.idMauSac)).toEqual([1, 2, 3, 4])
    expect(nhom.map((g) => g.rows.length)).toEqual([6, 6, 6, 6])
    expect(nhom.reduce((s, g) => s + g.rows.length, 0)).toBe(24)
  })
  it('ví dụ của cô 3 x 3 x 1 = 9 và công thức hiển thị để tự đếm', () => {
    expect(taoBienThe([1, 2, 3], [10, 11, 12], [20]).length).toBe(9)
    expect(congThucSoBienThe(4, 3, 2, true)).toEqual({ so: 24, congThuc: '4 màu × 3 trọng lượng × 2 chu vi = 24 biến thể' })
    expect(congThucSoBienThe(3, 0, 0, false)).toEqual({ so: 3, congThuc: '3 màu = 3 biến thể' })
  })
  it('danh mục không phải vợt: chỉ theo màu (trọng lượng, chu vi = "Không áp dụng")', () => {
    const rows = taoBienThe([1, 2, 3], [92], [93])
    expect(rows.length).toBe(3)
    expect(nhomTheoMau(rows, [1, 2, 3]).map((g) => g.rows.length)).toEqual([1, 1, 1])
    expect(rows.every((r) => r.idTrongLuong === 92 && r.idChuVi === 93)).toBe(true)
  })
  it('tạo lại giữ số lượng / giá đã nhập; chọn trùng chỉ tạo 1 dòng; màu theo thứ tự đã chọn', () => {
    const cu = taoBienThe([1], [10], [20])
    cu[0].soLuong = 7; cu[0].giaBan = 1500000
    const moi = taoBienThe([2, 1, 1], [10], [20], cu, { soLuong: 3, giaBan: 900000 })
    expect(moi.length).toBe(2)
    expect(moi.find((r) => r.idMauSac === 1)).toMatchObject({ soLuong: 7, giaBan: 1500000 })
    expect(moi.find((r) => r.idMauSac === 2)).toMatchObject({ soLuong: 3, giaBan: 900000 })
    expect(nhomTheoMau(moi, [2, 1]).map((g) => g.idMauSac)).toEqual([2, 1])
  })
  it('mặc định hiển thị nhóm theo màu như 2 video mẫu', () => {
    expect(CHE_DO_HIEN_THI_MAC_DINH).toBe('mau')
    expect(mauHex('Đen')).toBe('#1f2937')
    expect(mauHex('Xanh dương')).toBe('#0977ec')
    expect(mauHex('Màu lạ')).toBe('#cbd5e1')
  })
})

describe('Số lượng / giá mặc định + Áp dụng', () => {
  it('chỉ áp cho các dòng đang tích; ô mặc định trống thì giữ cột đó', () => {
    const rows = taoBienThe([1, 2], [10], [20, 21])
    rows[1].chon = false
    rows[1].giaBan = 111000
    const kq = apDungMacDinh(rows, { soLuong: 25, giaBan: 2500000 })
    expect(kq).toMatchObject({ ok: true, soDong: 3 })
    expect(rows.filter((r) => r.chon).every((r) => r.soLuong === 25 && r.giaBan === 2500000)).toBe(true)
    expect(rows[1].giaBan).toBe(111000)
    apDungMacDinh(rows, { soLuong: '', giaBan: 3000000 })
    expect(rows[0]).toMatchObject({ soLuong: 25, giaBan: 3000000 })
  })
  it('báo lỗi rõ: chưa nhập gì, giá 0, số lượng âm / lẻ, chưa tích dòng nào', () => {
    const rows = taoBienThe([1], [10], [20])
    expect(apDungMacDinh(rows, { soLuong: '', giaBan: '' }).loi).toContain('Nhập số lượng mặc định hoặc giá bán mặc định')
    expect(apDungMacDinh(rows, { giaBan: 0 }).loi).toContain('Giá bán phải lớn hơn 0')
    expect(apDungMacDinh(rows, { soLuong: -1 }).loi).toContain('số nguyên từ 0')
    expect(apDungMacDinh(rows, { soLuong: 1.5 }).loi).toContain('số nguyên từ 0')
    rows[0].chon = false
    expect(apDungMacDinh(rows, { soLuong: 5 }).loi).toContain('Chưa có biến thể nào được tích chọn')
    expect(rows[0].soLuong).toBe('')
  })
})

describe('Kiểm tra giá bán / số lượng của từng biến thể', () => {
  it('giá trống, 0, dưới 1.000 ₫ đều báo lỗi; số lượng phải là số nguyên từ 0', () => {
    expect(loiGiaBan('')).toBe('Nhập giá bán.')
    expect(loiGiaBan(0)).toBe('Giá bán phải lớn hơn 0 ₫.')
    expect(loiGiaBan(999)).toBe('Giá bán tối thiểu 1.000 ₫.')
    expect(loiGiaBan(1500000)).toBeNull()
    expect(loiGiaBan(2_000_000_000)).toContain('tối đa')
    expect(loiSoLuong('')).toBe('Nhập số lượng.')
    expect(loiSoLuong(0)).toBeNull()
    expect(loiSoLuong(2.5)).toContain('số nguyên')
  })
  it('chỉ kiểm tra dòng đang tích, đếm số dòng lỗi', () => {
    const rows = taoBienThe([1, 2], [10], [20])
    rows[0].soLuong = 5; rows[0].giaBan = 0
    rows[1].chon = false
    const kq = kiemTraBienThe(rows)
    expect(kq.soLoi).toBe(1)
    expect(kq.loiTheoKey[rows[0].key]).toEqual({ soLuong: null, giaBan: 'Giá bán phải lớn hơn 0 ₫.' })
    expect(kq.thongBao).toContain('Có 1 biến thể chưa hợp lệ')
  })
  it('dữ liệu gửi lên: chỉ dòng đang tích; ảnh theo màu chỉ gửi màu được chọn và có ảnh', () => {
    const rows = taoBienThe([1, 2], [10], [20], [], { soLuong: 4, giaBan: 50000 })
    rows[1].chon = false
    expect(payloadBienThe(rows)).toEqual([{ idMauSac: 1, idTrongLuong: 10, idChuVi: 20, giaBan: 50000, soLuongTon: 4 }])
    const anh = { 1: ['/api/uploads/den.jpg'], 2: ['/api/uploads/do.jpg'], 3: [] }
    expect(payloadAnhTheoMau(anh, [1, 3])).toEqual([{ idMauSac: 1, anh: ['/api/uploads/den.jpg'] }])
    expect(payloadAnhTheoMau(anh, [1, 3], false)).toEqual([{ idMauSac: 1, anh: ['/api/uploads/den.jpg'] }, { idMauSac: 3, anh: [] }])
  })
})

/* ===================== Trang Thêm sản phẩm ===================== */

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

const t = (id, ma, ten) => ({ id, ma, ten, hoatDong: true })
const NHIEU = {
  'mau-sac': [t(1, 'MS01', 'Đen'), t(2, 'MS02', 'Đỏ'), t(3, 'MS03', 'Trắng'), t(4, 'MS04', 'Vàng')],
  'trong-luong': [t(1, 'TL01', '3U (85-89g)'), t(2, 'TL02', '4U (80-84g)'), t(3, 'TL03', '5U (75-79g)'), t(92, 'TL00', 'Không áp dụng')],
  'chu-vi': [t(1, 'CV01', 'G5'), t(2, 'CV02', 'G6'), t(93, 'CV00', 'Không áp dụng')],
}

async function montar() {
  mock = mockApi({ get: (url) => { const m = /^\/thuoc-tinh\/(.+)$/.exec(url); return m && NHIEU[m[1]] ? NHIEU[m[1]] : undefined } })
  let soAnh = 0
  mock.api.post = vi.fn(async (url, body) => {
    mock.calls.post.push([url, body])
    if (url === '/hinh-anh/upload') return { data: { duongDan: `/api/uploads/anh-${++soAnh}.png` } }
    return { data: { id: 99 } }
  })
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/san-pham', component: { template: '<div/>' } },
    { path: '/san-pham/them', component: { template: '<div/>' } },
  ] })
  router.push('/san-pham/them'); await router.isReady()
  const Comp = (await import('../src/components/sanPham/SanPhamFormPage.vue')).default
  const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
  await flushPromises()
  return { w, router }
}
const bam = async (w, text) => { await w.findAll('button').find((b) => b.text().trim() === text).trigger('click'); await flushPromises() }

async function taoVot24(w) {
  await w.get('#sp-idDanhMuc').setValue(1); await flushPromises()
  const rows = w.findAll('.ad-gen-row')
  for (const c of rows[0].findAll('.ad-gen-chip')) await c.trigger('click') // 4 màu
  for (const c of rows[1].findAll('.ad-gen-chip')) await c.trigger('click') // 3 trọng lượng
  for (const c of rows[2].findAll('.ad-gen-chip')) await c.trigger('click') // 2 chu vi
  await bam(w, 'Tạo biến thể')
}

async function dienThongTin(w) {
  await w.get('#sp-ten').setValue('Astrox 100ZZ')
  for (const id of ['#sp-idThuongHieu', '#sp-idXuatXu', '#sp-idChatLieu', '#sp-idDoCung', '#sp-idDiemCanBang']) await w.get(id).setValue(1)
}

beforeEach(() => { document.body.innerHTML = '' })

describe('Trang Thêm sản phẩm: biến thể nhóm theo màu', () => {
  it('vợt: hiện công thức 4 × 3 × 2 = 24, tạo đủ 24 dòng chia 4 nhóm màu, mỗi nhóm 6 dòng có tên + chấm màu', async () => {
    const { w } = await montar()
    await w.get('#sp-idDanhMuc').setValue(1); await flushPromises()
    const rows = w.findAll('.ad-gen-row')
    expect(rows[1].findAll('.ad-gen-chip').length).toBe(3) // không có "Không áp dụng"
    for (const r of rows) for (const c of r.findAll('.ad-gen-chip')) await c.trigger('click')
    expect(w.get('[data-testid=du-kien]').text()).toContain('4 màu × 3 trọng lượng × 2 chu vi = 24 biến thể')
    await bam(w, 'Tạo biến thể')
    expect(w.findAll('tbody tr').length).toBe(24)
    const groups = w.findAll('.ad-gen-group')
    expect(groups.length).toBe(4)
    expect(groups.map((g) => g.findAll('tbody tr').length)).toEqual([6, 6, 6, 6])
    expect(groups.map((g) => g.get('.bt-group-head strong').text())).toEqual(['Đen', 'Đỏ', 'Trắng', 'Vàng'])
    expect(groups[0].find('.bt-dot').exists()).toBe(true)
    expect(groups[0].find('thead').text()).toContain('Trọng lượng')
    expect(groups[0].find('thead').text()).toContain('Chu vi')
    expect(w.text()).toContain('Chọn tất cả biến thể (24/24)')
  })

  it('bỏ tích cả nhóm màu, xóa từng dòng, chuyển sang Bảng chung vẫn đếm đúng', async () => {
    const { w } = await montar(); await taoVot24(w)
    const g0 = w.findAll('.ad-gen-group')[0]
    await g0.get('.bt-group-head input[type=checkbox]').setValue(false)
    expect(w.text()).toContain('Chọn tất cả biến thể (18/24)')
    await w.findAll('.ad-gen-group')[1].findAll('tbody tr')[0].get('button').trigger('click')
    expect(w.findAll('tbody tr').length).toBe(23)
    expect(w.findAll('.ad-gen-group')[1].findAll('tbody tr').length).toBe(5)
    await w.findAll('input[name=bt-che-do]').find((i) => i.element.value === 'chung').setValue(true)
    expect(w.findAll('.ad-gen-group').length).toBe(0)
    expect(w.findAll('tbody tr').length).toBe(23)
    expect(w.find('thead').text()).toContain('Màu sắc')
    await w.get('#bt-all').setValue(true)
    expect(w.text()).toContain('Chọn tất cả biến thể (23/23)')
  })

  it('số lượng / giá mặc định + Áp dụng: giá hiện dạng tiền 2.500.000, chỉ áp cho dòng đang tích', async () => {
    const { w } = await montar(); await taoVot24(w)
    await w.findAll('.ad-gen-group')[3].get('.bt-group-head input[type=checkbox]').setValue(false) // bỏ màu Vàng
    await w.get('#bt-sl').setValue('25')
    await w.get('#bt-gia').setValue('2500000')
    expect(w.get('#bt-gia').element.value).toBe('2.500.000')
    await bam(w, 'Áp dụng')
    const gia = w.findAll('.ad-gen-group')[0].findAll('input[aria-label="Giá bán"]')
    expect(gia.every((i) => i.element.value === '2.500.000')).toBe(true)
    const giaVang = w.findAll('.ad-gen-group')[3].findAll('input[aria-label="Giá bán"]')
    expect(giaVang.every((i) => i.element.value === '')).toBe(true)
    expect(thongBao()).toContain('Đã áp dụng cho 18 biến thể đang chọn')
  })

  it('giá 0 / để trống: báo lỗi rõ, viền đỏ, không gửi API', async () => {
    const { w } = await montar(); await taoVot24(w); await dienThongTin(w)
    await w.get('#bt-sl').setValue('5'); await w.get('#bt-gia').setValue('2000000'); await bam(w, 'Áp dụng')
    await w.findAll('.ad-gen-group')[0].findAll('input[aria-label="Giá bán"]')[0].setValue('0')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.text()).toContain('Có 1 biến thể chưa hợp lệ')
    expect(w.findAll('.ad-gen-group')[0].find('input[aria-label="Giá bán"].is-invalid').exists()).toBe(true)
    expect(mock.calls.post.length).toBe(0)
  })

  it('ảnh theo màu: mỗi màu một ô "Ảnh sản phẩm màu ...", tải ảnh lên thì gửi kèm anhTheoMau khi lưu', async () => {
    const { w, router } = await montar(); await taoVot24(w); await dienThongTin(w)
    const the = w.get('[data-testid=anh-theo-mau]')
    expect(the.findAll('.bt-anh-card').length).toBe(4)
    expect(the.text()).toContain('Ảnh sản phẩm màu Đen')
    expect(the.text()).toContain('Áp dụng cho 6 biến thể cùng màu')
    expect(the.text()).toContain('Nhóm màu này chưa có ảnh')
    // chọn 2 ảnh cho màu Đen
    const input = w.get('#sp-anh-mau-1')
    Object.defineProperty(input.element, 'files', { value: [new File(['a'], 'den1.png', { type: 'image/png' }), new File(['b'], 'den2.png', { type: 'image/png' })], configurable: true })
    await input.trigger('change'); await flushPromises()
    expect(w.findAll('.bt-anh-card')[0].findAll('img').length).toBe(2)
    expect(w.findAll('.bt-anh-card')[0].text()).toContain('Ảnh chính')
    // file sai định dạng: báo lỗi ngay, không tải
    const input2 = w.get('#sp-anh-mau-2')
    Object.defineProperty(input2.element, 'files', { value: [new File(['x'], 'a.pdf', { type: 'application/pdf' })], configurable: true })
    await input2.trigger('change'); await flushPromises()
    expect(w.findAll('.bt-anh-card')[1].text()).toContain('chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF')

    await w.get('#bt-sl').setValue('5'); await w.get('#bt-gia').setValue('3200000'); await bam(w, 'Áp dụng')
    await w.get('form').trigger('submit'); await flushPromises()
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    const luu = mock.calls.post.find((c) => c[0] === '/san-pham')
    expect(luu).toBeTruthy()
    expect(luu[1].bienThes.length).toBe(24)
    expect(luu[1].bienThes.every((b) => b.giaBan === 3200000 && b.soLuongTon === 5)).toBe(true)
    expect(luu[1].anhTheoMau).toEqual([{ idMauSac: 1, anh: ['/api/uploads/anh-1.png', '/api/uploads/anh-2.png'] }])
    expect(router.currentRoute.value.path).toBe('/san-pham')
  })

  it('danh mục không phải vợt: chỉ chọn màu, mỗi màu 1 dòng, trọng lượng / chu vi gửi "Không áp dụng"', async () => {
    const { w } = await montar()
    await w.get('#sp-idDanhMuc').setValue(5); await flushPromises()
    expect(w.findAll('.ad-gen-row').length).toBe(1)
    for (const c of w.findAll('.ad-gen-chip').slice(0, 3)) await c.trigger('click')
    expect(w.get('[data-testid=du-kien]').text()).toContain('3 màu = 3 biến thể')
    await bam(w, 'Tạo biến thể')
    expect(w.findAll('.ad-gen-group').length).toBe(3)
    expect(w.findAll('tbody tr').length).toBe(3)
    expect(w.find('thead').text()).not.toContain('Trọng lượng')
  })

  it('đã tạo biến thể mà bỏ tích hết: báo lỗi, không lưu', async () => {
    const { w } = await montar(); await taoVot24(w); await dienThongTin(w)
    await w.get('#bt-all').setValue(false)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.text()).toContain('Chưa tích chọn biến thể nào')
    expect(mock.calls.post.length).toBe(0)
  })
})

describe('Trang Sửa sản phẩm: ảnh theo màu', () => {
  it('hiện ô ảnh cho các màu sản phẩm đã có biến thể; lưu gửi anhTheoMau của các màu đó', async () => {
    mock = mockApi({ get: (url) => {
      if (url === '/san-pham/1') return { id: 1, ma: 'SP001', ten: 'Astrox 99', idDanhMuc: 1, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 1, idDiemCanBang: 1, anhChinh: '', anhPhu: [], moTa: '', hoatDong: true, anhTheoMau: [{ idMauSac: 1, anh: ['/api/uploads/den.jpg'] }] }
      if (url === '/bien-the-san-pham') return [{ id: 5, idSanPham: 1, idMauSac: 1 }, { id: 6, idSanPham: 1, idMauSac: 2 }, { id: 7, idSanPham: 2, idMauSac: 1 }]
      return undefined
    } })
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/san-pham', component: { template: '<div/>' } }, { path: '/san-pham/:id/sua', component: { template: '<div/>' } }] })
    router.push('/san-pham/1/sua'); await router.isReady()
    const Comp = (await import('../src/components/sanPham/SanPhamFormPage.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body }); await flushPromises()
    const cards = w.findAll('.bt-anh-card')
    expect(cards.length).toBe(2)
    expect(cards[0].find('img').attributes('src')).toBe('/api/uploads/den.jpg')
    // bỏ ảnh màu Đen rồi lưu -> gửi danh sách rỗng cho màu Đen để backend bỏ ảnh
    await cards[0].get('button[aria-label^="Bỏ ảnh 1"]').trigger('click')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(mock.calls.put[0][1].anhTheoMau).toEqual([{ idMauSac: 1, anh: [] }, { idMauSac: 2, anh: [] }])
    expect(THUOC_TINH['mau-sac'].length).toBe(2) // dữ liệu mẫu dùng chung không bị đổi
  })
})
