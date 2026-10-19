import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi, themAnhChung } from './helpersDanhMuc'
import { datSelect2, giaTriSelect2 } from './helpersSelect2'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

async function montar(path = '/san-pham/them') {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/san-pham', component: { template: '<div/>' } },
    { path: '/san-pham/them', component: { template: '<div/>' } },
    { path: '/san-pham/:id/sua', component: { template: '<div/>' } },
  ] })
  router.push(path); await router.isReady()
  const Comp = (await import('../src/components/sanPham/SanPhamFormPage.vue')).default
  const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
  await flushPromises()
  return { w, router }
}
const chonDanhMuc = async (w, id) => { await datSelect2(w, '#sp-idDanhMuc', id); await flushPromises() }
const nhan = (w) => w.findAll('label').map((l) => l.text())

beforeEach(() => { mock = mockApi(); document.body.innerHTML = '' })

describe('Form sản phẩm theo danh mục', () => {
  it('mới vào trang thêm: hiện đủ thuộc tính; khi chọn danh mục khác thì lọc theo danh mục đó', async () => {
    const { w } = await montar()
    expect(w.find('#sp-idDoCung').exists()).toBe(true)
    expect(w.find('#sp-idDiemCanBang').exists()).toBe(true)
    expect(w.findAll('.ad-gen-row').length).toBe(3)
  })
  it('chọn Vợt cầu lông: hiện độ cứng, điểm cân bằng, màu + trọng lượng + chu vi; không có "Không áp dụng" để chọn', async () => {
    const { w } = await montar()
    await chonDanhMuc(w, 1)
    expect(w.find('#sp-idDoCung').exists()).toBe(true)
    expect(w.find('#sp-idDiemCanBang').exists()).toBe(true)
    expect(w.findAll('.ad-gen-row').length).toBe(3)
    expect(w.text()).not.toContain('Không áp dụng')
  })
  it('chọn Vợt cầu lông người mới cũng là vợt', async () => {
    const { w } = await montar(); await chonDanhMuc(w, 2)
    expect(w.find('#sp-idDoCung').exists()).toBe(true)
  })
  for (const [id, ten] of [[5, 'Phụ kiện'], [4, 'Túi - balo'], [3, 'Quần áo thể thao'], [6, 'Quả cầu lông']]) {
    it(`chọn ${ten}: ẩn độ cứng, điểm cân bằng; biến thể chỉ có màu sắc`, async () => {
      const { w } = await montar(); await chonDanhMuc(w, id)
      expect(w.find('#sp-idDoCung').exists()).toBe(false)
      expect(w.find('#sp-idDiemCanBang').exists()).toBe(false)
      const rows = w.findAll('.ad-gen-row')
      expect(rows.length).toBe(1)
      expect(rows[0].text()).toContain('Màu sắc')
      expect(w.text()).not.toContain('Trọng lượng')
      expect(w.text()).not.toContain('Chu vi cán')
    })
  }
  it('Phụ kiện: lưu được khi chưa chọn độ cứng, gửi "Không áp dụng" + biến thể chỉ màu', async () => {
    const { w, router } = await montar(); await chonDanhMuc(w, 5)
    await w.get('#sp-ten').setValue('Cuốn cán Yonex')
    await datSelect2(w, '#sp-idThuongHieu', 1)
    await datSelect2(w, '#sp-idXuatXu', 1)
    await datSelect2(w, '#sp-idChatLieu', 1)
    // chọn 2 màu rồi tạo biến thể
    const chips = w.findAll('.ad-gen-chip'); expect(chips.length).toBe(2)
    await chips[0].trigger('click'); await chips[1].trigger('click')
    await w.findAll('button').find((b) => b.text().trim() === 'Tạo biến thể').trigger('click'); await flushPromises()
    expect(w.findAll('tbody tr').length).toBe(2)
    // Mặc định nhóm theo màu như 2 video mẫu (sửa 12/10: trước đây là một bảng chung): 2 màu -> 2 nhóm, mỗi nhóm 1 dòng
    expect(w.findAll('.ad-gen-group').length).toBe(2)
    expect(w.find('thead').text()).not.toContain('Trọng lượng')
    // Chuyển sang "Bảng chung": một bảng phẳng có cột Màu sắc
    await w.findAll('input[name=bt-che-do]').find((i) => i.element.value === 'chung').setValue(true)
    expect(w.find('.ad-gen-group').exists()).toBe(false)
    expect(w.findAll('tbody tr').length).toBe(2)
    expect(w.find('thead').text()).toContain('Màu sắc')
    expect(w.find('thead').text()).not.toContain('Trọng lượng')
    for (const input of w.findAll('tbody input[type=number], tbody input[inputmode=numeric]')) await input.setValue(input.attributes('aria-label') === 'Giá bán' ? '50000' : '5')
    await themAnhChung(w) // ảnh là bắt buộc
    await w.get('form').trigger('submit'); await flushPromises()
    expect(document.body.textContent).toContain('Bạn có muốn lưu sản phẩm không?')
    const nutLuu = [...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu'))
    nutLuu.click(); await flushPromises()
    expect(mock.calls.post.length).toBe(1)
    const body = mock.calls.post[0][1]
    expect(body).toMatchObject({ idDanhMuc: 5, idDoCung: 90, idDiemCanBang: 91 })
    expect(body.bienThes.length).toBe(2)
    expect(body.bienThes.every((b) => b.idTrongLuong === 92 && b.idChuVi === 93)).toBe(true)
    expect(router.currentRoute.value.path).toBe('/san-pham')
  })
  it('Vợt: vẫn bắt buộc độ cứng / điểm cân bằng', async () => {
    const { w } = await montar(); await chonDanhMuc(w, 1)
    await w.get('#sp-ten').setValue('Vợt A'); await datSelect2(w, '#sp-idThuongHieu', 1); await datSelect2(w, '#sp-idXuatXu', 1); await datSelect2(w, '#sp-idChatLieu', 1)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.text()).toContain('Chọn độ cứng.')
    expect(w.text()).toContain('Chọn điểm cân bằng.')
    expect(mock.calls.post.length).toBe(0)
  })
  it('Vợt: tạo biến thể cần cả 3 nhóm; đủ thì ra tổ hợp màu x trọng lượng x chu vi', async () => {
    const { w } = await montar(); await chonDanhMuc(w, 1)
    const btn = () => w.findAll('button').find((b) => b.text().trim() === 'Tạo biến thể')
    const rows = w.findAll('.ad-gen-row')
    await rows[0].findAll('.ad-gen-chip')[0].trigger('click')
    await btn().trigger('click'); await flushPromises()
    expect(w.findAll('tbody tr').length).toBe(0) // thiếu trọng lượng, chu vi
    await rows[1].findAll('.ad-gen-chip')[0].trigger('click'); await rows[2].findAll('.ad-gen-chip')[0].trigger('click')
    await btn().trigger('click'); await flushPromises()
    expect(w.findAll('tbody tr').length).toBe(1)
    expect(w.find('thead').text()).toContain('Trọng lượng')
  })
  it('đổi từ Vợt sang Phụ kiện: biến thể đã tạo bị xóa, độ cứng ẩn; đổi lại Vợt thì phải chọn lại', async () => {
    const { w } = await montar(); await chonDanhMuc(w, 1)
    const rows = w.findAll('.ad-gen-row')
    for (const r of rows) await r.findAll('.ad-gen-chip')[0].trigger('click')
    await w.findAll('button').find((b) => b.text().trim() === 'Tạo biến thể').trigger('click'); await flushPromises()
    expect(w.findAll('tbody tr').length).toBe(1)
    await chonDanhMuc(w, 5)
    expect(w.findAll('tbody tr').length).toBe(0)
    expect(w.find('#sp-idDoCung').exists()).toBe(false)
    await chonDanhMuc(w, 1)
    expect(w.find('#sp-idDoCung').exists()).toBe(true)
    expect(giaTriSelect2(w, '#sp-idDoCung')).toBe('') // không giữ "Không áp dụng"
  })
  it('sửa sản phẩm phụ kiện: không hiện độ cứng; lưu giữ "Không áp dụng"', async () => {
    mock = mockApi({ get: (url) => (url === '/san-pham/2' ? { id: 2, ma: 'SP002', ten: 'Grip Yonex', idDanhMuc: 5, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 90, idDiemCanBang: 91, anhChinh: '/api/uploads/grip.png', anhPhu: [], moTa: '', hoatDong: true } : undefined) })
    const { w } = await montar('/san-pham/2/sua')
    expect(w.find('#sp-idDoCung').exists()).toBe(false)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(mock.calls.put.length).toBe(1)
    expect(mock.calls.put[0][1]).toMatchObject({ idDanhMuc: 5, idDoCung: 90, idDiemCanBang: 91 })
  })
  it('sửa sản phẩm vợt: vẫn hiện độ cứng với giá trị cũ', async () => {
    mock = mockApi({ get: (url) => (url === '/san-pham/1' ? { id: 1, ma: 'SP001', ten: 'Astrox 99', idDanhMuc: 1, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 1, idDiemCanBang: 1, anhChinh: '/api/uploads/astrox.png', anhPhu: [], moTa: '', hoatDong: true } : undefined) })
    const { w } = await montar('/san-pham/1/sua')
    expect(giaTriSelect2(w, '#sp-idDoCung')).toBe('1')
    expect(giaTriSelect2(w, '#sp-idDiemCanBang')).toBe('1')
  })
  it('thiếu mục "Không áp dụng" trong CSDL: báo lỗi rõ ràng, không gửi API', async () => {
    const { THUOC_TINH } = await import('./helpersDanhMuc')
    const bak = THUOC_TINH['do-cung']; THUOC_TINH['do-cung'] = bak.filter((x) => x.id !== 90)
    try {
      const { w } = await montar(); await chonDanhMuc(w, 5)
      await w.get('#sp-ten').setValue('X'); await datSelect2(w, '#sp-idThuongHieu', 1); await datSelect2(w, '#sp-idXuatXu', 1); await datSelect2(w, '#sp-idChatLieu', 1)
      await w.get('form').trigger('submit'); await flushPromises()
      expect(w.text()).toContain('chưa có mục')
      expect(mock.calls.post.length).toBe(0)
    } finally { THUOC_TINH['do-cung'] = bak }
  })
  it('thêm sản phẩm trùng tên sản phẩm đã có: báo lỗi "Tên sản phẩm đã tồn tại.", không gửi API', async () => {
    const { w } = await montar('/san-pham/them')
    await w.get('#sp-ten').setValue('  astrox   99  ')
    await w.get('#sp-ten').trigger('blur')
    expect(w.find('#sp-ten-msg').text()).toBe('Tên sản phẩm đã tồn tại.')
    expect(w.get('#sp-ten').classes()).toContain('is-invalid')
    await w.get('form').trigger('submit')
    await flushPromises()
    expect(mock.calls.post.length).toBe(0)
  })
  it('sửa sản phẩm: đổi sang tên sản phẩm khác thì báo lỗi, giữ tên của chính nó thì hợp lệ', async () => {
    mock = mockApi({ get: (url) => (url === '/san-pham/2' ? { id: 2, ma: 'SP002', ten: 'Grip Yonex', idDanhMuc: 5, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 90, idDiemCanBang: 91, anhChinh: '/api/uploads/grip.png', anhPhu: [], moTa: '', hoatDong: true } : undefined) })
    const { w } = await montar('/san-pham/2/sua')
    // Đổi sang tên SP001 (Astrox 99)
    await w.get('#sp-ten').setValue('Astrox 99')
    await w.get('#sp-ten').trigger('blur')
    expect(w.find('#sp-ten-msg').text()).toBe('Tên sản phẩm đã tồn tại.')
    // Đổi lại tên cũ của chính nó (Grip Yonex)
    await w.get('#sp-ten').setValue('Grip Yonex')
    await w.get('#sp-ten').trigger('blur')
    expect(w.find('#sp-ten-msg').exists()).toBe(false)
  })
})
