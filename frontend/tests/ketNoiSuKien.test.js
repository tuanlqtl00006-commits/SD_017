import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// Kênh báo thay đổi phải tự hồi phục: backend tắt / khởi động lại, proxy treo yêu cầu, kết nối chết im lặng.
class EventSourceGia {
  static ds = []
  constructor(url) {
    this.url = url
    this.readyState = 0
    this.nghe = {}
    this.daDong = false
    EventSourceGia.ds.push(this)
  }
  addEventListener(ten, fn) {
    ;(this.nghe[ten] ||= []).push(fn)
  }
  phat(ten, data = '{}') {
    if (ten === 'san-sang') this.readyState = 1
    for (const fn of this.nghe[ten] || []) fn({ data })
  }
  loi(dong) {
    this.readyState = dong ? 2 : 0
    this.onerror?.()
  }
  close() {
    this.daDong = true
    this.readyState = 2
  }
}
const moiNhat = () => EventSourceGia.ds[EventSourceGia.ds.length - 1]

let suKien
beforeEach(async () => {
  vi.useFakeTimers()
  vi.resetModules()
  EventSourceGia.ds = []
  vi.stubGlobal('EventSource', EventSourceGia)
  suKien = await import('../src/services/suKien')
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('Kết nối kênh báo thay đổi', () => {
  it('mở một kết nối khi có màn nghe đầu tiên; tin thay-doi tới đúng màn', () => {
    const cb = vi.fn()
    suKien.nghe(['hoa-don'], cb)
    suKien.nghe(['san-pham'], vi.fn())
    expect(EventSourceGia.ds).toHaveLength(1)
    expect(moiNhat().url).toBe('/api/su-kien')
    moiNhat().phat('san-sang')
    expect(suKien.dangKetNoi.value).toBe(true)
    moiNhat().phat('thay-doi', JSON.stringify({ chuDe: ['hoa-don'], nguon: 'may-khac' }))
    expect(cb).toHaveBeenCalledTimes(1)
  })

  it('backend tắt (proxy 502): tự nối lại sau 1, 2, 4 rồi tối đa 5 giây; nối lại được thì mọi màn tải lại một lần', async () => {
    const cb = vi.fn()
    suKien.nghe(['hoa-don'], cb)
    moiNhat().phat('san-sang')
    moiNhat().loi(true)
    expect(suKien.dangKetNoi.value).toBe(false)
    await vi.advanceTimersByTimeAsync(1000)
    expect(EventSourceGia.ds).toHaveLength(2)
    moiNhat().loi(true)
    await vi.advanceTimersByTimeAsync(2000)
    expect(EventSourceGia.ds).toHaveLength(3)
    moiNhat().loi(true)
    await vi.advanceTimersByTimeAsync(4000)
    moiNhat().loi(true)
    await vi.advanceTimersByTimeAsync(4999)
    expect(EventSourceGia.ds).toHaveLength(4)
    await vi.advanceTimersByTimeAsync(1)
    expect(EventSourceGia.ds).toHaveLength(5)
    expect(cb).not.toHaveBeenCalled()
    moiNhat().phat('san-sang')
    expect(cb).toHaveBeenCalledWith({ chuDe: [suKien.TAT_CA] })
  })

  it('yêu cầu nối treo (10 giây chưa san-sang) thì bỏ, mở kết nối mới', async () => {
    suKien.nghe(['hoa-don'], vi.fn())
    await vi.advanceTimersByTimeAsync(9999)
    expect(EventSourceGia.ds).toHaveLength(1)
    await vi.advanceTimersByTimeAsync(1)
    expect(EventSourceGia.ds[0].daDong).toBe(true)
    expect(EventSourceGia.ds).toHaveLength(2)
    // trình duyệt tự nối lại (CONNECTING) mà treo: cũng bị bỏ sau 10 giây
    moiNhat().phat('san-sang')
    moiNhat().loi(false)
    await vi.advanceTimersByTimeAsync(10_000)
    expect(EventSourceGia.ds).toHaveLength(3)
  })

  it('đang sống: ping 20 giây giữ kết nối; im lặng quá 45 giây coi như chết, nối lại', async () => {
    suKien.nghe(['hoa-don'], vi.fn())
    moiNhat().phat('san-sang')
    for (let i = 0; i < 5; i++) {
      await vi.advanceTimersByTimeAsync(20_000)
      moiNhat().phat('ping', '3')
    }
    expect(EventSourceGia.ds).toHaveLength(1)
    await vi.advanceTimersByTimeAsync(45_000)
    expect(EventSourceGia.ds[0].daDong).toBe(true)
    expect(EventSourceGia.ds).toHaveLength(2)
    expect(suKien.dangKetNoi.value).toBe(false)
  })
})
