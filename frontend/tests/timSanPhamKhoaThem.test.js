import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// Góp ý 21/11: nhân viên chưa vào ca thì nút Thêm trong bảng chọn sản phẩm bị khóa (kể cả bảng đang mở sẵn), Enter / quét mã cũng không thêm.
const { api } = vi.hoisted(() => ({ api: { get: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))
import TimSanPham from '../src/components/banHang/TimSanPham.vue'

const SP = { id: 9, tenSanPham: 'Yonex Astrox 99 Pro', maSpct: 'SP01-DEN-4U-G5', donGia: 3861000, giaGoc: 4290000, tonKho: 5 }

describe('TimSanPham khi chưa vào ca', () => {
  it('khoaThem: nút Thêm bị khóa, bấm / Enter / quét mã không phát sự kiện chọn', async () => {
    api.get.mockImplementation(async (url) => (url.includes('/ban-hang/san-pham/ma/') ? { data: SP } : { data: { items: [SP], tongSo: 1, trang: 0 } }))
    const w = mount(TimSanPham, { props: { trongHop: false, khoaThem: true } })
    await flushPromises()
    const nut = w.find('.bh-nut-them')
    expect(nut.exists()).toBe(true)
    expect(nut.attributes('disabled')).toBeDefined()
    await nut.trigger('click')
    await w.find('.bh-o-sl').trigger('keydown', { key: 'Enter' })
    await w.find('.bh-o-ma input').setValue('SP01-DEN-4U-G5')
    await w.find('.bh-o-ma').trigger('submit')
    await flushPromises()
    expect(w.emitted('chon')).toBeUndefined()

    await w.setProps({ khoaThem: false })
    expect(w.find('.bh-nut-them').attributes('disabled')).toBeUndefined()
    await w.find('.bh-nut-them').trigger('click')
    expect(w.emitted('chon')).toHaveLength(1)
  })
})
