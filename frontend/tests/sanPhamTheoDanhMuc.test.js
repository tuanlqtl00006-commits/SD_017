import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi } from './helpersDanhMuc'

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
const chonDanhMuc = async (w, id) => { await w.get('#sp-idDanhMuc').setValue(id); await flushPromises() }
const nhan = (w) => w.findAll('label').map((l) => l.text())

beforeEach(() => { mock = mockApi(); document.body.innerHTML = '' })

describe('Form sản phẩm theo danh mục', () => {
  it('chưa chọn danh mục: không hiện độ cứng / điểm cân bằng, biến thể chưa có thuộc tính', async () => {
    const { w } = await montar()
    expect(w.find('#sp-idDoCung').exists()).toBe(false)
    expect(w.find('#sp-idDiemCanBang').exists()).toBe(false)
    expect(w.findAll('.ad-gen-row').length).toBe(0)
    expect(w.text()).toContain('Chọn danh mục ở trên')
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
    await w.get('#sp-idThuongHieu').setValue(1)
    await w.get('#sp-idXuatXu').setValue(1)
    await w.get('#sp-idChatLieu').setValue(1)
    // chọn 2 màu rồi tạo biến thể
    const chips = w.findAll('.ad-gen-chip'); expect(chips.length).toBe(2)
    await chips[0].trigger('click'); await chips[1].trigger('click')
    await w.findAll('button').find((b) => b.text().includes('Tạo biến thể tự động')).trigger('click'); await flushPromises()
    expect(w.findAll('tbody tr').length).toBe(2)
    expect(w.find('thead').text()).not.toContain('Trọng lượng')
    for (const input of w.findAll('tbody input[type=number]')) await input.setValue(input.attributes('aria-label') === 'Giá bán' ? '50000' : '5')
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
    await w.get('#sp-ten').setValue('Vợt A'); await w.get('#sp-idThuongHieu').setValue(1); await w.get('#sp-idXuatXu').setValue(1); await w.get('#sp-idChatLieu').setValue(1)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.text()).toContain('Chọn độ cứng.')
    expect(w.text()).toContain('Chọn điểm cân bằng.')
    expect(mock.calls.post.length).toBe(0)
  })
  it('Vợt: tạo biến thể cần cả 3 nhóm; đủ thì ra tổ hợp màu x trọng lượng x chu vi', async () => {
    const { w } = await montar(); await chonDanhMuc(w, 1)
    const btn = () => w.findAll('button').find((b) => b.text().includes('Tạo biến thể tự động'))
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
    await w.findAll('button').find((b) => b.text().includes('Tạo biến thể tự động')).trigger('click'); await flushPromises()
    expect(w.findAll('tbody tr').length).toBe(1)
    await chonDanhMuc(w, 5)
    expect(w.findAll('tbody tr').length).toBe(0)
    expect(w.find('#sp-idDoCung').exists()).toBe(false)
    await chonDanhMuc(w, 1)
    expect(w.find('#sp-idDoCung').exists()).toBe(true)
    expect(w.get('#sp-idDoCung').element.value).toBe('') // không giữ "Không áp dụng"
  })
  it('sửa sản phẩm phụ kiện: không hiện độ cứng; lưu giữ "Không áp dụng"', async () => {
    mock = mockApi({ get: (url) => (url === '/san-pham/2' ? { id: 2, ma: 'SP002', ten: 'Grip Yonex', idDanhMuc: 5, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 90, idDiemCanBang: 91, anhChinh: '', anhPhu: [], moTa: '', hoatDong: true } : undefined) })
    const { w } = await montar('/san-pham/2/sua')
    expect(w.find('#sp-idDoCung').exists()).toBe(false)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(mock.calls.put.length).toBe(1)
    expect(mock.calls.put[0][1]).toMatchObject({ idDanhMuc: 5, idDoCung: 90, idDiemCanBang: 91 })
  })
  it('sửa sản phẩm vợt: vẫn hiện độ cứng với giá trị cũ', async () => {
    mock = mockApi({ get: (url) => (url === '/san-pham/1' ? { id: 1, ma: 'SP001', ten: 'Astrox 99', idDanhMuc: 1, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 1, idDiemCanBang: 1, anhChinh: '', anhPhu: [], moTa: '', hoatDong: true } : undefined) })
    const { w } = await montar('/san-pham/1/sua')
    expect(w.get('#sp-idDoCung').element.value).toBe('1')
    expect(w.get('#sp-idDiemCanBang').element.value).toBe('1')
  })
  it('thiếu mục "Không áp dụng" trong CSDL: báo lỗi rõ ràng, không gửi API', async () => {
    const { THUOC_TINH } = await import('./helpersDanhMuc')
    const bak = THUOC_TINH['do-cung']; THUOC_TINH['do-cung'] = bak.filter((x) => x.id !== 90)
    try {
      const { w } = await montar(); await chonDanhMuc(w, 5)
      await w.get('#sp-ten').setValue('X'); await w.get('#sp-idThuongHieu').setValue(1); await w.get('#sp-idXuatXu').setValue(1); await w.get('#sp-idChatLieu').setValue(1)
      await w.get('form').trigger('submit'); await flushPromises()
      expect(w.text()).toContain('chưa có mục')
      expect(mock.calls.post.length).toBe(0)
    } finally { THUOC_TINH['do-cung'] = bak }
  })
})
