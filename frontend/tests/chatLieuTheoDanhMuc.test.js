import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi, THUOC_TINH, themAnhChung } from './helpersDanhMuc'
import { nhomDanhMuc, chatLieuHopDanhMuc } from '../src/utils/danhMuc'
import { cacMucSelect2, chuSelect2, datSelect2, giaTriSelect2 } from './helpersSelect2'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))
beforeEach(() => { document.body.innerHTML = '' })

// Chất liệu giống dữ liệu thật sau khi gắn nhóm danh mục (database/SD_17_2_du_lieu.sql)
const t = (id, ma, ten, nhom) => ({ id, ma, ten, hoatDong: true, nhom })
const CHAT_LIEU = [
  t(1, 'CL001', 'Carbon', ['VOT']), t(2, 'CL002', 'Graphite', ['VOT']), t(3, 'CL003', 'Nhôm', ['VOT']), t(4, 'CL004', 'Titanium', ['VOT']),
  t(5, 'CL005', 'Da PU', ['TUI_BALO', 'PHU_KIEN']), t(6, 'CL006', 'Cotton', ['QUAN_AO', 'PHU_KIEN']), t(7, 'CL007', 'Polyester', ['QUAN_AO', 'TUI_BALO', 'PHU_KIEN']),
  t(8, 'CL008', 'Thun lạnh', ['QUAN_AO']), t(13, 'CL013', 'Lông ngỗng', ['QUA_CAU']), t(20, 'CL020', 'Chất liệu cũ chưa gắn nhóm', []),
]

describe('nhomDanhMuc / chatLieuHopDanhMuc', () => {
  it('nhận ra nhóm từ tên danh mục', () => {
    expect(nhomDanhMuc('Vợt cầu lông')).toBe('VOT')
    expect(nhomDanhMuc('Vợt cầu lông người mới')).toBe('VOT')
    expect(nhomDanhMuc('Quần áo thể thao')).toBe('QUAN_AO')
    expect(nhomDanhMuc('Túi - balo')).toBe('TUI_BALO')
    expect(nhomDanhMuc('Phụ kiện')).toBe('PHU_KIEN')
    expect(nhomDanhMuc('Quả cầu lông')).toBe('QUA_CAU')
    expect(nhomDanhMuc('Giày')).toBe(null)
    expect(nhomDanhMuc('')).toBe(null)
    expect(nhomDanhMuc(null)).toBe(null)
  })
  it('chất liệu hợp danh mục; danh mục lạ hoặc chất liệu chưa gắn nhóm thì không bị lọc', () => {
    expect(chatLieuHopDanhMuc(CHAT_LIEU[2], 'Quần áo thể thao')).toBe(false) // Nhôm không dùng cho quần áo
    expect(chatLieuHopDanhMuc(CHAT_LIEU[5], 'Quần áo thể thao')).toBe(true)
    expect(chatLieuHopDanhMuc(CHAT_LIEU[2], 'Vợt cầu lông')).toBe(true)
    expect(chatLieuHopDanhMuc(CHAT_LIEU[2], 'Giày')).toBe(true)
    expect(chatLieuHopDanhMuc(CHAT_LIEU[9], 'Quần áo thể thao')).toBe(true)
    expect(chatLieuHopDanhMuc({ ten: 'x' }, 'Phụ kiện')).toBe(true)
  })
})

describe('Form sản phẩm: ô Chất liệu theo danh mục', () => {
  async function montar(path = '/san-pham/them', sanPham) {
    mock = mockApi({
      get: (url) => {
        if (url === '/thuoc-tinh/chat-lieu') return CHAT_LIEU
        if (sanPham && url === `/san-pham/${sanPham.id}`) return sanPham
      },
    })
    const router = createRouter({ history: createMemoryHistory(), routes: [
      { path: '/san-pham', component: { template: '<div/>' } },
      { path: '/san-pham/them', component: { template: '<div/>' } },
      { path: '/san-pham/:id/sua', component: { template: '<div/>' } },
    ] })
    router.push(path); await router.isReady()
    const Comp = (await import('../src/components/sanPham/SanPhamFormPage.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const chonDanhMuc = async (w, id) => { await datSelect2(w, '#sp-idDanhMuc', id); await flushPromises() }
  // Các mục đang có trong ô Chất liệu (ô bị khóa thì không mở được danh sách => rỗng)
  const dsChatLieu = async (w) => {
    if (w.get('#sp-idChatLieu').attributes('disabled') !== undefined) return []
    const ds = await cacMucSelect2(w, '#sp-idChatLieu')
    w.get('#sp-idChatLieu').element.click() // đóng danh sách
    await flushPromises()
    return ds
  }

  it('chưa chọn danh mục: ô Chất liệu bị khóa, không có lựa chọn nào', async () => {
    const w = await montar()
    expect(w.get('#sp-idChatLieu').attributes('disabled')).toBeDefined()
    expect(chuSelect2(w, '#sp-idChatLieu')).toContain('Chọn danh mục trước')
    expect(await dsChatLieu(w)).toEqual([])
  })

  it('Quần áo thể thao: chỉ có Cotton, Polyester, Thun lạnh (và chất liệu chưa gắn nhóm) - KHÔNG có Nhôm / Titanium / Carbon', async () => {
    const w = await montar()
    await chonDanhMuc(w, 3)
    expect(w.get('#sp-idChatLieu').attributes('disabled')).toBeUndefined()
    const ds = await dsChatLieu(w)
    expect(ds).toEqual(expect.arrayContaining(['Cotton', 'Polyester', 'Thun lạnh']))
    for (const sai of ['Nhôm', 'Titanium', 'Carbon', 'Graphite', 'Da PU', 'Lông ngỗng']) expect(ds).not.toContain(sai)
  })

  it.each([
    [1, ['Carbon', 'Graphite', 'Nhôm', 'Titanium']],
    [2, ['Carbon', 'Graphite', 'Nhôm', 'Titanium']],
    [4, ['Da PU', 'Polyester']],
    [5, ['Da PU', 'Cotton', 'Polyester']],
    [6, ['Lông ngỗng']],
  ])('danh mục id %i: đúng bộ chất liệu', async (id, mong) => {
    const w = await montar()
    await chonDanhMuc(w, id)
    const ds = (await dsChatLieu(w)).filter((x) => x !== 'Chất liệu cũ chưa gắn nhóm')
    expect(ds.sort()).toEqual([...mong].sort())
  })

  it('đổi danh mục: chất liệu đã chọn không còn hợp thì bị bỏ chọn (bắt chọn lại); còn hợp thì giữ', async () => {
    const w = await montar()
    await chonDanhMuc(w, 1)
    await datSelect2(w, '#sp-idChatLieu', 3) // Nhôm (khung vợt)
    expect(giaTriSelect2(w, '#sp-idChatLieu')).toBe('3')
    await chonDanhMuc(w, 3) // sang Quần áo thể thao
    expect(giaTriSelect2(w, '#sp-idChatLieu')).toBe('')
    await datSelect2(w, '#sp-idChatLieu', 7) // Polyester
    await chonDanhMuc(w, 5) // sang Phụ kiện: Polyester vẫn dùng được
    expect(giaTriSelect2(w, '#sp-idChatLieu')).toBe('7')
  })

  it('lưu quần áo với chất liệu hợp danh mục: gửi đúng idChatLieu', async () => {
    const w = await montar()
    await chonDanhMuc(w, 3)
    await w.get('#sp-ten').setValue('Áo Yonex'); await datSelect2(w, '#sp-idThuongHieu', 1); await datSelect2(w, '#sp-idXuatXu', 1)
    await datSelect2(w, '#sp-idChatLieu', 8)
    await themAnhChung(w)
    await w.get('form').trigger('submit'); await flushPromises()
    const nut = [...document.body.querySelectorAll('button')].find((b) => b.textContent.includes('Xác nhận lưu'))
    nut.click(); await flushPromises()
    expect(mock.calls.post[0][1]).toMatchObject({ idDanhMuc: 3, idChatLieu: 8 })
  })

  it('sửa sản phẩm cũ giữ danh mục: chất liệu cũ (dù lệch nhóm) vẫn còn trong ô để không mất dữ liệu', async () => {
    const sp = { id: 9, ma: 'SP009', ten: 'Áo cũ', idDanhMuc: 3, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 3, idDoCung: 90, idDiemCanBang: 91, hoatDong: true, anhPhu: [] }
    const w = await montar('/san-pham/9/sua', sp)
    expect(giaTriSelect2(w, '#sp-idChatLieu')).toBe('3')
    expect(await dsChatLieu(w)).toContain('Nhôm')
    await chonDanhMuc(w, 5) // đổi sang danh mục khác: phải chọn lại chất liệu hợp danh mục mới
    expect(await dsChatLieu(w)).not.toContain('Nhôm')
    expect(giaTriSelect2(w, '#sp-idChatLieu')).toBe('')
    await chonDanhMuc(w, 3) // quay về đúng danh mục gốc thì tổ hợp cũ được phép giữ (cùng luật với backend)
    expect(await dsChatLieu(w)).toContain('Nhôm')
  })
})

describe('Quản lý chất liệu: chọn danh mục sử dụng', () => {
  const montarModal = async (props) => {
    const Comp = (await import('../src/components/thuocTinh/ThuocTinhFormModal.vue')).default
    const cfg = { slug: 'chat-lieu', label: 'Chất liệu', prefix: 'CL', example: 'Carbon' }
    const w = mount(Comp, { props: { cfg, all: [], ...props }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const q = (s) => document.body.querySelector(s)
  const nhom = () => [...document.body.querySelectorAll('[aria-label="Danh mục sử dụng chất liệu"] button')]

  it('thêm chất liệu: phải chọn ít nhất một danh mục, rồi gửi ten + nhom', async () => {
    const w = await montarModal()
    expect(nhom().map((b) => b.textContent.trim())).toEqual(['Vợt cầu lông', 'Quần áo thể thao', 'Túi - balo', 'Phụ kiện', 'Quả cầu lông'])
    q('#tt-ten').value = 'Cotton'; q('#tt-ten').dispatchEvent(new Event('input'))
    q('#tt-form').dispatchEvent(new Event('submit')); await flushPromises()
    expect(w.emitted('save')).toBeFalsy()
    expect(document.body.textContent).toContain('Chọn ít nhất một danh mục')
    nhom()[1].click(); nhom()[3].click(); await flushPromises()
    q('#tt-form').dispatchEvent(new Event('submit')); await flushPromises()
    expect(w.emitted('save')[0][0]).toEqual({ ten: 'Cotton', nhom: ['QUAN_AO', 'PHU_KIEN'] })
    w.unmount()
  })

  it('sửa chất liệu: tích sẵn nhóm hiện có; chất liệu cũ chưa gắn nhóm thì tích sẵn tất cả', async () => {
    let w = await montarModal({ item: { id: 6, ma: 'CL006', ten: 'Cotton', hoatDong: true, nhom: ['QUAN_AO'] } })
    expect(nhom().map((b) => b.getAttribute('aria-pressed'))).toEqual(['false', 'true', 'false', 'false', 'false'])
    w.unmount(); document.body.innerHTML = ''
    w = await montarModal({ item: { id: 1, ma: 'CL001', ten: 'Carbon', hoatDong: true, nhom: [] } })
    expect(nhom().every((b) => b.getAttribute('aria-pressed') === 'true')).toBe(true)
    w.unmount()
  })

  it('thuộc tính khác (vd màu sắc) không có phần chọn danh mục và chỉ gửi ten', async () => {
    const Comp = (await import('../src/components/thuocTinh/ThuocTinhFormModal.vue')).default
    const w = mount(Comp, { props: { cfg: { slug: 'mau-sac', label: 'Màu sắc', prefix: 'MS', example: 'Đen' }, all: [] }, attachTo: document.body })
    await flushPromises()
    expect(nhom().length).toBe(0)
    q('#tt-ten').value = 'Hồng'; q('#tt-ten').dispatchEvent(new Event('input'))
    q('#tt-form').dispatchEvent(new Event('submit')); await flushPromises()
    expect(w.emitted('save')[0][0]).toEqual({ ten: 'Hồng' })
    w.unmount()
  })
})
