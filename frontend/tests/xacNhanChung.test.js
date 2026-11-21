import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

// Góp ý 21/11: mọi thao tác thêm / sửa / xóa / gửi phải có hộp xác nhận (Có / Không). Hộp dùng chung hoiXacNhan
// (composables/useXacNhan.js) hiện ConfirmDialog sẵn có; bấm Hủy thì KHÔNG gửi gì lên máy chủ.
const { svc, api, push } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), getById: vi.fn(), create: vi.fn(), update: vi.fn(), toggleActive: vi.fn() },
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  push: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: {} }),
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/khachHangService', () => ({ khachHangService: svc }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/diaChiHanhChinhService', () => {
  const tinh = [{ code: 33, name: 'Tỉnh Hưng Yên' }]
  const phuong = { 33: [{ code: 20, name: 'Phường Thái Bình' }] }
  return { API_DIA_CHI: 'x', diaChiHanhChinhService: { layTinhThanh: async () => tinh, layPhuongXa: async (m) => phuong[m] ?? [], xoaNho() {} } }
})

import { hoiXacNhan } from '../src/composables/useXacNhan'
import KhachHangFormPage from '../src/components/khachHang/KhachHangFormPage.vue'
import { datSelect2 } from './helpersSelect2'

const body = () => new DOMWrapper(document.body)
const hop = () => body().find('.modal-content')
const nut = (chu) => body().findAll('.modal-footer button').find((b) => b.text() === chu)

beforeEach(() => {
  document.body.innerHTML = ''
  Object.values(svc).forEach((f) => f.mockReset())
  push.mockReset()
})

describe('hoiXacNhan (hộp xác nhận dùng chung)', () => {
  it('hiện tiêu đề, câu hỏi, nút Hủy + nút xác nhận; bấm xác nhận trả về true, bấm Hủy trả về false', async () => {
    const lan1 = hoiXacNhan({ title: 'Thêm khách hàng', message: 'Bạn có chắc muốn thêm?', confirmText: 'Thêm khách hàng' })
    await flushPromises()
    expect(hop().text()).toContain('Thêm khách hàng')
    expect(hop().text()).toContain('Bạn có chắc muốn thêm?')
    expect(body().findAll('.modal-footer button').map((b) => b.text())).toEqual(['Hủy', 'Thêm khách hàng'])
    await nut('Thêm khách hàng').trigger('click')
    expect(await lan1).toBe(true)
    await flushPromises()
    expect(hop().exists()).toBe(false)

    const lan2 = hoiXacNhan({ title: 'Xóa', message: 'Xóa thật không?', confirmText: 'Xóa', variant: 'danger' })
    await flushPromises()
    expect(nut('Xóa').classes()).toContain('ad-btn-danger')
    await nut('Hủy').trigger('click')
    expect(await lan2).toBe(false)
  })
})

describe('Form khách hàng: bấm Lưu phải hỏi xác nhận', () => {
  async function moVaDien() {
    svc.getAll.mockResolvedValue([])
    const w = mount(KhachHangFormPage, { attachTo: document.body })
    await flushPromises()
    await w.find('#kh-ho-ten').setValue('Nguyễn Thị Mai')
    await w.find('#kh-sdt').setValue('0912345678')
    await w.find('#kh-email').setValue('mai@gmail.com')
    await datSelect2(w, '#kh-dia-chi-tinh', 'Tỉnh Hưng Yên')
    await flushPromises()
    await datSelect2(w, '#kh-dia-chi-phuong', 'Phường Thái Bình')
    await w.find('#kh-dia-chi-cu-the').setValue('Số 12 Nguyễn Trãi')
    return w
  }

  it('bấm Hủy trong hộp xác nhận thì không gửi; bấm "Thêm khách hàng" mới tạo', async () => {
    const w = await moVaDien()
    await w.find('button[type=submit]').trigger('click')
    await flushPromises()
    expect(hop().text()).toContain('Bạn có chắc muốn thêm khách hàng Nguyễn Thị Mai?')
    await nut('Hủy').trigger('click')
    await flushPromises()
    expect(svc.create).not.toHaveBeenCalled()

    svc.create.mockResolvedValue({ id: 9 })
    await w.find('button[type=submit]').trigger('click')
    await flushPromises()
    await nut('Thêm khách hàng').trigger('click')
    await flushPromises()
    expect(svc.create).toHaveBeenCalledTimes(1)
    expect(svc.create.mock.calls[0][0]).toMatchObject({ hoTen: 'Nguyễn Thị Mai', sdt: '0912345678' })
  })
})
