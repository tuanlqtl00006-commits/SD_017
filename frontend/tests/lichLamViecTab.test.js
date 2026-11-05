import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { dv, state } = vi.hoisted(() => ({
  dv: { dsCa: vi.fn(), nhanVien: vi.fn(), lich: vi.fn(), lichCuaToi: vi.fn() },
  state: { query: {} },
}))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: state.query }) }))
vi.mock('../src/services/lichLamViecService', () => ({ lichLamViecService: dv }))

import LichLamViecPage from '../src/components/lichLamViec/LichLamViecPage.vue'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'

let w
function mo(vaiTro, query = {}) {
  useAuth().trangThai.nguoiDung = { id: 1, hoTen: 'Nguyễn Hoàng Long', vaiTro }
  state.query = query
  w = mount(LichLamViecPage, { attachTo: document.body })
  return w
}
const nhanTab = () => w.findAll('.ll-tab .ad-segment-item').map((b) => b.text())

beforeEach(() => {
  document.body.innerHTML = ''
  _datLaiAuth()
  for (const f of Object.values(dv)) f.mockReset()
  dv.dsCa.mockResolvedValue([])
  dv.nhanVien.mockResolvedValue([])
  dv.lich.mockResolvedValue([])
  dv.lichCuaToi.mockResolvedValue([])
})
afterEach(() => w?.unmount())

describe('Lịch làm việc: gộp Lịch ca làm thành một tab', () => {
  it('quản lý thấy 3 tab Lịch tuần, Ca làm việc, Lịch ca làm; mặc định mở Lịch tuần', async () => {
    mo('QUAN_LY')
    await flushPromises()
    expect(nhanTab()).toEqual(['Lịch tuần', 'Ca làm việc', 'Lịch ca làm'])
    expect(dv.lich).toHaveBeenCalled()
    expect(dv.lichCuaToi).not.toHaveBeenCalled()
    await w.find('[data-testid="ll-tab-cua-toi"]').trigger('click')
    await flushPromises()
    expect(dv.lichCuaToi).toHaveBeenCalled()
  })

  it('link cũ ?tab=cua-toi mở thẳng tab Lịch ca làm', async () => {
    mo('QUAN_LY', { tab: 'cua-toi' })
    await flushPromises()
    expect(w.find('[data-testid="ll-tab-cua-toi"]').classes()).toContain('active')
    expect(dv.lichCuaToi).toHaveBeenCalled()
  })

  it('nhân viên chỉ thấy lịch ca làm của mình, không có tab xếp lịch, không gọi API của quản lý', async () => {
    mo('NHAN_VIEN')
    await flushPromises()
    expect(w.find('.ll-tab').exists()).toBe(false)
    expect(dv.lichCuaToi).toHaveBeenCalled()
    expect(dv.dsCa).not.toHaveBeenCalled()
    expect(dv.nhanVien).not.toHaveBeenCalled()
    expect(dv.lich).not.toHaveBeenCalled()
  })
})
