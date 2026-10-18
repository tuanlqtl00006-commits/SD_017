// Theo lời cô (GĐ2): ảnh bắt buộc mới lưu sản phẩm; "Thêm nhanh" ngay trong ô Select2; 2 x 2 x 2 = 8 biến thể.
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi, themAnhChung } from './helpersDanhMuc'
import { chuSelect2, datSelect2, giaTriSelect2, themNhanhSelect2, timTrongSelect2 } from './helpersSelect2'
import { useToast } from '../src/composables/useToast'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))
beforeEach(() => { document.body.innerHTML = ''; useToast().toasts.splice(0) })

const t = (id, ma, ten, nhom) => ({ id, ma, ten, hoatDong: true, ...(nhom ? { nhom } : {}) })
const DU_LIEU = {
  'danh-muc': [t(1, 'DM01', 'Vợt cầu lông'), t(5, 'DM05', 'Phụ kiện')],
  'thuong-hieu': [t(1, 'TH01', 'Yonex')],
  'xuat-xu': [t(1, 'XX01', 'Nhật Bản')],
  'chat-lieu': [t(1, 'CL01', 'Carbon', ['VOT', 'PHU_KIEN'])],
  'do-cung': [t(1, 'DC01', 'Cứng'), t(90, 'DC00', 'Không áp dụng')],
  'diem-can-bang': [t(1, 'CB01', 'Nặng đầu'), t(91, 'CB00', 'Không áp dụng')],
  'mau-sac': [t(1, 'MS01', 'Đen'), t(2, 'MS02', 'Đỏ')],
  'trong-luong': [t(1, 'TL01', '4U (80-84g)'), t(2, 'TL02', '3U (85-89g)'), t(92, 'TL00', 'Không áp dụng')],
  'chu-vi': [t(1, 'CV01', 'G5'), t(2, 'CV02', 'G6'), t(93, 'CV00', 'Không áp dụng')],
}

async function montar(extra = {}) {
  mock = mockApi({
    get: (url) => { const m = /^\/thuoc-tinh\/(.+)$/.exec(url); return m ? DU_LIEU[m[1]].map((x) => ({ ...x })) : undefined },
    ...extra,
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
const coHopXacNhan = () => document.body.textContent.includes('Bạn có muốn lưu sản phẩm không?')

async function dienCoBan(w, danhMuc) {
  await datSelect2(w, '#sp-idDanhMuc', danhMuc)
  await w.get('#sp-ten').setValue('Cuốn cán Yonex')
  await datSelect2(w, '#sp-idThuongHieu', 1)
  await datSelect2(w, '#sp-idXuatXu', 1)
  await datSelect2(w, '#sp-idChatLieu', 1)
}
async function taoHaiMau(w) {
  const chips = w.findAll('.ad-gen-chip')
  await chips[0].trigger('click'); await chips[1].trigger('click')
  await bam(w, 'Tạo biến thể')
  await w.get('#bt-sl').setValue('5'); await w.get('#bt-gia').setValue('50000'); await bam(w, 'Áp dụng')
}
async function chonAnhMau(w, idMau, ten) {
  const input = w.get(`#sp-anh-mau-${idMau}`)
  Object.defineProperty(input.element, 'files', { value: [new File(['x'], ten, { type: 'image/png' })], configurable: true })
  await input.trigger('change'); await flushPromises()
}

describe('Ảnh sản phẩm là tùy chọn theo yêu cầu người dùng', () => {
  it('khung ảnh không có dấu * bắt buộc; chưa có ảnh thì bấm lưu vẫn hỏi xác nhận và lưu thành công (ảnh rỗng)', async () => {
    const { w } = await montar(); await dienCoBan(w, 5)
    expect(w.get('#sp-anh-chung').text()).not.toContain('*')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.find('[data-testid=loi-anh]').exists()).toBe(false)
    expect(coHopXacNhan()).toBe(true)
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    expect(mock.calls.post.length).toBe(1)
    expect(mock.calls.post[0][1].anhChinh).toBe('')
  })

  it('thêm 1 ảnh chung thì gửi anhChinh khi lưu', async () => {
    const { w } = await montar(); await dienCoBan(w, 5)
    await themAnhChung(w)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.find('[data-testid=loi-anh]').exists()).toBe(false)
    expect(coHopXacNhan()).toBe(true)
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    expect(mock.calls.post.length).toBe(1)
    expect(mock.calls.post[0][1].anhChinh).toBe('/api/uploads/anh-1.png')
  })

  it('có biến thể 2 màu mà chỉ màu Đen có ảnh riêng: vẫn lưu thành công (không bắt buộc đủ ảnh các màu)', async () => {
    const { w } = await montar(); await dienCoBan(w, 5); await taoHaiMau(w)
    await chonAnhMau(w, 1, 'den.png')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.find('[data-testid=loi-anh]').exists()).toBe(false)
    expect(coHopXacNhan()).toBe(true)
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    expect(mock.calls.post.length).toBe(1)
    expect(mock.calls.post[0][1].anhTheoMau).toEqual([{ idMauSac: 1, anh: ['/api/uploads/anh-1.png'] }])
  })

  it('mỗi màu đều có ảnh riêng thì gửi anhTheoMau cho cả 2 màu', async () => {
    const { w } = await montar(); await dienCoBan(w, 5); await taoHaiMau(w)
    await chonAnhMau(w, 1, 'den.png'); await chonAnhMau(w, 2, 'do.png')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.find('[data-testid=loi-anh]').exists()).toBe(false)
    expect(coHopXacNhan()).toBe(true)
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    const body = mock.calls.post[0][1]
    expect(body.anhChinh).toBe('')
    expect(body.anhTheoMau).toEqual([{ idMauSac: 1, anh: ['/api/uploads/anh-1.png'] }, { idMauSac: 2, anh: ['/api/uploads/anh-2.png'] }])
  })

  it('bỏ ảnh vừa thêm thì vẫn lưu bình thường mà không có ảnh', async () => {
    const { w } = await montar(); await dienCoBan(w, 5)
    await themAnhChung(w)
    await w.get('button[aria-label="Bỏ ảnh 1"]').trigger('click')
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.find('[data-testid=loi-anh]').exists()).toBe(false)
    expect(coHopXacNhan()).toBe(true)
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    expect(mock.calls.post.length).toBe(1)
    expect(mock.calls.post[0][1].anhChinh).toBe('')
  })
})

describe('Biến thể = màu x trọng lượng x chu vi (cô: "2 x 2 x 2 = 8 thì dưới này phải có 8 biến thể")', () => {
  it('vợt 2 màu x 2 trọng lượng x 2 chu vi: hiện công thức, tạo đúng 8 dòng chia 2 nhóm màu, ghi tổng cộng 8 biến thể và gửi 8 biến thể', async () => {
    const { w } = await montar(); await dienCoBan(w, 1)
    await datSelect2(w, '#sp-idDoCung', 1); await datSelect2(w, '#sp-idDiemCanBang', 1)
    const hang = w.findAll('.ad-gen-row')
    expect(hang.length).toBe(3)
    for (const h of hang) {
      const chips = h.findAll('.ad-gen-chip')
      expect(chips.length).toBe(2) // "Không áp dụng" không có để chọn
      for (const c of chips) await c.trigger('click')
    }
    expect(w.get('[data-testid=du-kien]').text()).toContain('2 màu × 2 trọng lượng × 2 chu vi = 8 biến thể')
    await bam(w, 'Tạo biến thể')
    expect(w.findAll('tbody tr').length).toBe(8)
    const nhom = w.findAll('.ad-gen-group')
    expect(nhom.length).toBe(2)
    expect(nhom.map((g) => g.findAll('tbody tr').length)).toEqual([4, 4])
    expect(w.get('[data-testid=tong-bien-the]').text()).toContain('Tổng cộng 8 biến thể')
    expect(w.get('[data-testid=tong-bien-the]').text()).toContain('2 màu × 2 trọng lượng × 2 chu vi')

    await w.get('#bt-sl').setValue('3'); await w.get('#bt-gia').setValue('2500000'); await bam(w, 'Áp dụng')
    await themAnhChung(w)
    await w.get('form').trigger('submit'); await flushPromises()
    ;[...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu')).click(); await flushPromises()
    const body = mock.calls.post[0][1]
    expect(body.bienThes.length).toBe(8)
    const khoa = new Set(body.bienThes.map((b) => `${b.idMauSac}-${b.idTrongLuong}-${b.idChuVi}`))
    expect(khoa.size).toBe(8) // 8 tổ hợp khác nhau
  })

  it('bỏ tích 1 dòng thì tổng đếm theo số dòng còn lại', async () => {
    const { w } = await montar(); await dienCoBan(w, 5); await taoHaiMau(w)
    expect(w.get('[data-testid=tong-bien-the]').text()).toContain('Tổng cộng 2 biến thể')
  })
})

describe('"Thêm nhanh" ngay trong ô chọn (cô: "chưa có thêm nhanh")', () => {
  const taoMoi = (slug, ten, id = 50) => ({ id, ma: `${slug.toUpperCase().slice(0, 2)}${id}`, ten, hoatDong: true })

  it.each([
    ['thuong-hieu', '#sp-idThuongHieu', 'Li-Ning'],
    ['xuat-xu', '#sp-idXuatXu', 'Trung Quốc'],
    ['danh-muc', '#sp-idDanhMuc', 'Giày cầu lông'],
  ])('%s: gõ tên chưa có -> Thêm nhanh -> gọi API tạo, ô tự chọn mục mới', async (slug, sel, ten) => {
    const { w } = await montar({ post: (url, body) => (url === `/thuoc-tinh/${slug}` ? taoMoi(slug, body.ten) : undefined) })
    await themNhanhSelect2(w, sel, ten)
    const goi = mock.calls.post.find((c) => c[0] === `/thuoc-tinh/${slug}`)
    expect(goi).toBeTruthy()
    expect(goi[1]).toEqual({ ten })
    expect(giaTriSelect2(w, sel)).toBe('50')
    expect(chuSelect2(w, sel)).toBe(ten)
    expect(useToast().toasts.at(-1).message).toContain(ten)
  })

  it('chất liệu: khóa tới khi chọn danh mục; thêm nhanh gắn đúng nhóm của danh mục (vợt -> VOT) rồi tự chọn', async () => {
    const { w } = await montar({ post: (url, body) => (url === '/thuoc-tinh/chat-lieu' ? { ...taoMoi('chat-lieu', body.ten), nhom: body.nhom } : undefined) })
    expect(w.get('#sp-idChatLieu').attributes('disabled')).toBeDefined()
    await datSelect2(w, '#sp-idDanhMuc', 1)
    await themNhanhSelect2(w, '#sp-idChatLieu', 'Kevlar')
    const goi = mock.calls.post.find((c) => c[0] === '/thuoc-tinh/chat-lieu')
    expect(goi[1]).toEqual({ ten: 'Kevlar', nhom: ['VOT'] })
    expect(chuSelect2(w, '#sp-idChatLieu')).toBe('Kevlar')
  })

  it('độ cứng và điểm cân bằng (vợt) cũng thêm nhanh được', async () => {
    const { w } = await montar({ post: (url, body) => (/^\/thuoc-tinh\/(do-cung|diem-can-bang)$/.test(url) ? taoMoi('x', body.ten) : undefined) })
    await datSelect2(w, '#sp-idDanhMuc', 1)
    await themNhanhSelect2(w, '#sp-idDoCung', 'Siêu cứng')
    await themNhanhSelect2(w, '#sp-idDiemCanBang', 'Cân bằng')
    expect(mock.calls.post.map((c) => c[0])).toEqual(['/thuoc-tinh/do-cung', '/thuoc-tinh/diem-can-bang'])
    expect(chuSelect2(w, '#sp-idDoCung')).toBe('Siêu cứng')
    expect(chuSelect2(w, '#sp-idDiemCanBang')).toBe('Cân bằng')
  })

  it('màu sắc / trọng lượng / chu vi: thêm nhanh ở phần tạo biến thể, mục mới được tích sẵn', async () => {
    const { w } = await montar({ post: (url, body) => (/^\/thuoc-tinh\/(mau-sac|trong-luong|chu-vi)$/.test(url) ? taoMoi('x', body.ten, 70) : undefined) })
    await datSelect2(w, '#sp-idDanhMuc', 1)
    await themNhanhSelect2(w, '#sp-them-mau-sac', 'Xanh ngọc')
    await themNhanhSelect2(w, '#sp-them-trong-luong', '2U (90-94g)')
    await themNhanhSelect2(w, '#sp-them-chu-vi', 'G4')
    expect(mock.calls.post.map((c) => c[0])).toEqual(['/thuoc-tinh/mau-sac', '/thuoc-tinh/trong-luong', '/thuoc-tinh/chu-vi'])
    const hang = w.findAll('.ad-gen-row')
    for (const [i, ten] of ['Xanh ngọc', '2U (90-94g)', 'G4'].entries()) {
      const chip = hang[i].findAll('.ad-gen-chip').find((c) => c.text().includes(ten))
      expect(chip, `không có chip ${ten}`).toBeTruthy()
      expect(chip.attributes('aria-pressed')).toBe('true')
    }
  })

  it('tên đã có (kể cả khác hoa thường / dấu) thì không hiện Thêm nhanh', async () => {
    const { w } = await montar()
    await timTrongSelect2(w, '#sp-idThuongHieu', 'yonex')
    expect(document.body.querySelector('[data-testid=select2-them-nhanh]')).toBeNull()
    await timTrongSelect2(w, '#sp-idXuatXu', 'nhat ban')
    expect(w.find('[data-testid=select2-them-nhanh]').exists()).toBe(false)
  })

  it('server báo lỗi (vd trùng tên đã ngưng): báo bằng thông báo, ô giữ nguyên chưa chọn', async () => {
    const { w } = await montar({ post: () => { throw new Error('Thương hiệu đã tồn tại.') } })
    await themNhanhSelect2(w, '#sp-idThuongHieu', 'Victor')
    expect(useToast().toasts.at(-1).message).toContain('Thương hiệu đã tồn tại')
    expect(giaTriSelect2(w, '#sp-idThuongHieu')).toBe('')
  })
})
