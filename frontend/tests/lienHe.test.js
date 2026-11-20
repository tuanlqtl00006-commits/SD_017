import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

// Trang Liên hệ (20/11, góp ý 19/11 [12:48–13:54]): bỏ những gì cửa hàng không có (đổi trả, email không có hộp thư thật),
// thêm dòng "Quy định cửa hàng" trong "Có thể bạn đang cần", bấm vào mở hộp thoại quy định.
const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))

import { nhanCaiDatVanChuyen } from '../src/services/caiDatVanChuyen'
import ContactView from '../src/views/client/ContactView.vue'

const RouterLinkGia = { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' }
const gan = () => mount(ContactView, { attachTo: document.body, global: { components: { RouterLink: RouterLinkGia } } })

beforeEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
  api.get.mockResolvedValue({ data: {} })
})

describe('Trang Liên hệ', () => {
  it('chỉ còn kênh có thật: hotline, chat, trụ sở; không còn đổi trả / email cskh', async () => {
    const w = gan()
    await flushPromises()
    expect(w.find('[data-testid="lh-hotline"]').text()).toContain('0977 508 430')
    expect(w.find('[data-testid="lh-chat"]').exists()).toBe(true)
    expect(w.find('[data-testid="lh-tru-so"]').text()).toContain('123 Đường Cầu Diễn')
    expect(w.text()).not.toContain('cskh@footstyle.vn')
    expect(w.text().toLowerCase()).not.toContain('đổi trả')
    expect(w.text().toLowerCase()).not.toContain('bảo hành')
    expect(w.text().toLowerCase()).not.toContain('chính hãng')
    expect(w.html()).not.toContain('doi-tra')
  })

  it('"Quy định cửa hàng" là một dòng trong "Có thể bạn đang cần", bấm mở hộp thoại; phí giao theo cài đặt của quản lý', async () => {
    nhanCaiDatVanChuyen({ mienPhiBat: true, mienPhiTu: 2000000, phiGiaoHang: 25000, tinhTheoKm: true, kmCoBan: 10, buocKm: 50, phiMoiBuoc: 1000 })
    const w = gan()
    await flushPromises()
    const body = new DOMWrapper(document.body)
    const hop = () => body.find('[data-testid="lh-quy-dinh"]')
    expect(hop().exists()).toBe(false)
    expect(w.find('.lh-lien-ket').text()).toContain('Quy định cửa hàng')

    await w.find('[data-testid="lh-mo-quy-dinh"]').trigger('click')
    expect(hop().exists()).toBe(true)
    expect(body.find('.ad-modal-title').text()).toBe('Quy định cửa hàng')
    const qd = hop().text()
    expect(qd).toContain('5 phút')
    expect(qd).toContain('Chờ xác nhận')
    expect(qd).toContain('5 lần')
    const giao = body.find('[data-testid="lh-qd-giao-hang"]').text()
    expect(giao).toMatch(/2\.000\.000/)
    expect(giao).toMatch(/25\.000.*10 km đầu.*1\.000.*50 km/)

    await body.findAll('.modal button').find((b) => b.text() === 'Đã hiểu').trigger('click')
    expect(hop().exists()).toBe(false)
  })
})

describe('Chân trang web khách', () => {
  // Đọc mã nguồn như menuCaTroChuyen.test.js (layout kéo theo nhiều phần, chỉ cần kiểm nội dung chữ của chân trang)
  const src = readFileSync(resolve(__dirname, '../src/layouts/ClientLayout.vue'), 'utf8')
  const chanTrang = src.slice(src.indexOf('<footer'), src.indexOf('</footer>'))

  it('chỉ còn những gì dự án có thật (20/11)', () => {
    expect(chanTrang).toContain('Trụ sở: 123 Đường Cầu Diễn')
    expect(chanTrang).toContain('0977508430')
    expect(chanTrang).toContain('#giao-hang')
    expect(chanTrang).toContain('#bao-mat')
    expect(chanTrang).toContain('/shop/tra-cuu')
    expect(chanTrang).toContain('Giao Hàng Nhanh')
    for (const khongCo of ['cskh@footstyle.vn', 'bảo hành', 'Câu hỏi thường gặp', 'nhuong-quyen', 'chính hãng', 'toàn quốc', 'theo nơi nhận', '#doi-tra']) {
      expect(chanTrang).not.toContain(khongCo)
    }
  })
})
