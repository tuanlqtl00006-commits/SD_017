import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useAutoRefresh } from '../src/composables/useAutoRefresh'

// Review 05/11: dữ liệu tự cập nhật nhanh, không cần F5 - nhưng không được đè lên việc người dùng đang làm.
function gan(reload, tuyChon) {
  const C = defineComponent({
    setup() {
      useAutoRefresh(reload, 3000, tuyChon)
      return () => h('div', [h('input', { id: 'o-nhap' })])
    },
  })
  return mount(C, { attachTo: document.body })
}

beforeEach(() => {
  vi.useFakeTimers()
  document.body.innerHTML = ''
})
afterEach(() => {
  vi.useRealTimers()
})

describe('Tự cập nhật dữ liệu (useAutoRefresh)', () => {
  it('tải ngầm 10 giây một lần', async () => {
    const reload = vi.fn()
    const w = gan(reload)
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(2)
    w.unmount()
    await vi.advanceTimersByTimeAsync(30_000)
    expect(reload).toHaveBeenCalledTimes(2) // rời trang thì thôi
  })

  it('không tải khi đang mở hộp thoại hoặc con trỏ đang ở ô nhập', async () => {
    const reload = vi.fn()
    const w = gan(reload)
    const hop = document.createElement('div')
    hop.setAttribute('aria-modal', 'true')
    document.body.appendChild(hop)
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).not.toHaveBeenCalled()
    hop.remove()
    w.get('#o-nhap').element.focus()
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).not.toHaveBeenCalled()
    w.get('#o-nhap').element.blur()
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(1)
    w.unmount()
  })

  it('màn chỉ ra ô nào mới phải chờ (oChanTai): con trỏ ở ô khác vẫn tải, ở đúng ô đó thì chờ', async () => {
    const reload = vi.fn()
    let chan = false
    const w = gan(reload, { oChanTai: () => chan })
    w.get('#o-nhap').element.focus()
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(1)
    chan = true
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(1)
    w.unmount()
  })

  it('màn báo chưa tải được (vd form đang mở) thì bỏ qua; lần trước chưa xong thì không gọi chồng', async () => {
    let choPhep = false
    let xong
    const reload = vi.fn(() => new Promise((r) => { xong = r }))
    const w = gan(reload, { coTheTai: () => choPhep })
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).not.toHaveBeenCalled()
    choPhep = true
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(20_000) // lần 1 chưa xong
    expect(reload).toHaveBeenCalledTimes(1)
    xong()
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(2)
    w.unmount()
  })

  it('tải lỗi thì không văng lỗi ra ngoài, lần sau vẫn thử lại; chuKyMs = 0 thì tắt tải định kỳ', async () => {
    const loi = vi.fn(() => Promise.reject(new Error('mất mạng')))
    const w = gan(loi)
    await vi.advanceTimersByTimeAsync(20_000)
    expect(loi).toHaveBeenCalledTimes(2)
    w.unmount()
    const tat = vi.fn()
    const w2 = gan(tat, { chuKyMs: 0 })
    await vi.advanceTimersByTimeAsync(60_000)
    expect(tat).not.toHaveBeenCalled()
    w2.unmount()
  })
})
