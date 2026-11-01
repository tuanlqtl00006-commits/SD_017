import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const { api, state } = vi.hoisted(() => ({
  api: { get: vi.fn(), post: vi.fn() },
  state: { path: '/hoa-don' },
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: state.path, fullPath: state.path, meta: {}, params: {}, query: {} }),
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}))
vi.mock('../src/services/api', () => ({ default: api }))

import MainLayout from '../src/layouts/MainLayout.vue'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'
import { _datLaiGiaoCa } from '../src/composables/useGiaoCa'
import { xoaPhien } from '../src/services/phien'

// MainLayout dùng <RouterLink> / <RouterView> đăng ký toàn cục (app.use(router)) nên trong test phải stub
const STUBS = {
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
  RouterView: { template: '<div />' },
}
let gan = []
const ganVao = (c) => {
  const w = mount(c, { attachTo: document.body, global: { stubs: STUBS } })
  gan.push(w)
  return w
}

function moLayout(nguoiDung) {
  useAuth().trangThai.nguoiDung = nguoiDung
  api.get.mockImplementation(async (url) => {
    if (url === '/giao-ca/hien-tai') return { data: { dangMo: null, tongKet: null, lichHomNay: [] } }
    return { data: [] }
  })
  return ganVao(MainLayout)
}
const nhom = (w, ten) => w.findAll('button.ad-link').find((b) => b.text().includes(ten))

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  sessionStorage.clear()
  xoaPhien()
  _datLaiAuth()
  _datLaiGiaoCa()
  state.path = '/hoa-don'
  api.get.mockReset()
  api.post.mockReset()
})
afterEach(() => {
  gan.forEach((w) => w.unmount())
  gan = []
})

describe('Menu quản trị: Ca làm việc (đã bỏ chat nội bộ, chat hỗ trợ khách, trợ lý AI)', () => {
  it('nhân viên thấy lịch của mình, lịch sử giao ca nhưng không thấy Xếp lịch làm việc', async () => {
    const w = moLayout({ id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' })
    await flushPromises()
    await nhom(w, 'Ca làm việc').trigger('click')
    const links = w.findAll('a').map((a) => a.attributes('href'))
    expect(links).toEqual(expect.arrayContaining(['/lich-cua-toi', '/giao-ca']))
    expect(links).not.toContain('/lich-lam-viec')
    expect(links).not.toContain('/thong-ke')
  })

  it('quản lý thấy thêm Xếp lịch làm việc', async () => {
    const w = moLayout({ id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' })
    await flushPromises()
    await nhom(w, 'Ca làm việc').trigger('click')
    expect(w.findAll('a').map((a) => a.attributes('href'))).toContain('/lich-lam-viec')
  })

  it('không còn nhóm Trò chuyện, nút Trợ lý AI, chuông tin nhắn; không gọi API đếm tin chưa đọc', async () => {
    for (const nguoiDung of [{ id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }, { id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' }]) {
      const w = moLayout(nguoiDung)
      await flushPromises()
      expect(nhom(w, 'Trò chuyện')).toBeUndefined()
      const links = w.findAll('a').map((a) => a.attributes('href'))
      expect(links).not.toContain('/ho-tro-truc-tuyen')
      expect(links).not.toContain('/chat-noi-bo')
      expect(w.find('header').text()).not.toContain('Trợ lý AI')
      expect(w.find('header .bi-bell').exists()).toBe(false)
      expect(api.get).not.toHaveBeenCalledWith('/chat/chua-doc')
    }
  })

  it('header có nút Ca làm việc (nhận ca / giao ca)', async () => {
    const w = moLayout({ id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' })
    await flushPromises()
    expect(w.find('header').text()).toContain('Ca làm việc')
    expect(api.get).toHaveBeenCalledWith('/giao-ca/hien-tai')
  })
})

describe('Trang bán hàng online', () => {
  it('không còn bong bóng chat hỗ trợ khách (chat AI)', () => {
    const src = readFileSync(resolve(__dirname, '../src/layouts/ClientLayout.vue'), 'utf8')
    expect(src).not.toContain('NutChatKhach')
  })
})
