import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))

import ChonKhach from '../src/components/banHang/ChonKhach.vue'

const body = () => new DOMWrapper(document.body)
let w

beforeEach(() => {
  document.body.innerHTML = ''
  api.get.mockReset()
  api.post.mockReset()
  api.get.mockResolvedValue({ data: [] })
})
afterEach(() => w?.unmount())

describe('Bán tại quầy: thêm nhanh khách', () => {
  it('SĐT chỉ nhận chữ số, tối đa 10 số; sai định dạng thì báo lỗi, không gọi lưu', async () => {
    w = mount(ChonKhach, { attachTo: document.body })
    await w.find('button[aria-label="Thêm khách mới"]').trigger('click')
    await flushPromises()
    const o = body().find('#khn-sdt')
    await o.setValue('09a12 345-6789')
    expect(o.element.value).toBe('0912345678')
    await o.setValue('912345678')
    await o.trigger('blur')
    expect(body().text()).toContain('Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.')
    await o.setValue('0912345678')
    expect(body().text()).not.toContain('Số điện thoại gồm 10 chữ số')
  })
})
