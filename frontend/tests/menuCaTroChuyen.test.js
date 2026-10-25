import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

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
import NutChatKhach from '../src/components/chat/NutChatKhach.vue'
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

function moLayout(nguoiDung, chuaDoc = { hoTro: 0, noiBo: 0 }) {
  useAuth().trangThai.nguoiDung = nguoiDung
  api.get.mockImplementation(async (url) => {
    if (url === '/chat/chua-doc') return { data: chuaDoc }
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

describe('Menu quản trị: Ca làm việc + Trò chuyện', () => {
  it('nhân viên thấy lịch của mình, lịch sử giao ca, hỗ trợ khách, chat nội bộ nhưng không thấy Xếp lịch làm việc', async () => {
    const w = moLayout({ id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' })
    await flushPromises()
    await nhom(w, 'Ca làm việc').trigger('click')
    await nhom(w, 'Trò chuyện').trigger('click')
    const links = w.findAll('a').map((a) => a.attributes('href'))
    expect(links).toEqual(expect.arrayContaining(['/lich-cua-toi', '/giao-ca', '/ho-tro-truc-tuyen', '/chat-noi-bo']))
    expect(links).not.toContain('/lich-lam-viec')
    expect(links).not.toContain('/thong-ke')
  })

  it('quản lý thấy thêm Xếp lịch làm việc', async () => {
    const w = moLayout({ id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY' })
    await flushPromises()
    await nhom(w, 'Ca làm việc').trigger('click')
    expect(w.findAll('a').map((a) => a.attributes('href'))).toContain('/lich-lam-viec')
  })

  it('số tin chưa đọc: nhóm đang đóng hiện tổng (tối đa 99+), mở nhóm thì hiện riêng từng mục', async () => {
    const w = moLayout({ id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }, { hoTro: 3, noiBo: 120 })
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/chat/chua-doc')
    expect(nhom(w, 'Trò chuyện').find('.ad-menu-badge').text()).toBe('99+')
    await nhom(w, 'Trò chuyện').trigger('click')
    expect(nhom(w, 'Trò chuyện').find('.ad-menu-badge').exists()).toBe(false)
    const hoTro = w.findAll('a').find((a) => a.attributes('href') === '/ho-tro-truc-tuyen')
    const noiBo = w.findAll('a').find((a) => a.attributes('href') === '/chat-noi-bo')
    expect(hoTro.find('.ad-menu-badge').text()).toBe('3')
    expect(noiBo.find('.ad-menu-badge').text()).toBe('99+')
  })

  it('header có nút Ca làm việc (nhận ca / giao ca)', async () => {
    const w = moLayout({ id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' })
    await flushPromises()
    expect(w.find('header').text()).toContain('Ca làm việc')
    expect(api.get).toHaveBeenCalledWith('/giao-ca/hien-tai')
  })
})

describe('Nút chat trợ lý ảo trên trang bán hàng online', () => {
  it('bấm để mở / đóng khung chat của khách', async () => {
    const w = ganVao(NutChatKhach)
    expect(w.find('.nck-khung').exists()).toBe(false)
    await w.find('.nck-nut').trigger('click')
    expect(w.find('.nck-khung').exists()).toBe(true)
    expect(w.text()).toContain('Bắt đầu trò chuyện')
    await w.find('.nck-nut').trigger('click')
    expect(w.find('.nck-khung').exists()).toBe(false)
  })
})
