import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useAutoRefresh } from '../src/composables/useAutoRefresh'
import { useSuKien } from '../src/composables/useSuKien'
import { TAB_ID, TAT_CA, TIEU_DE_TAB, dangKetNoi, nghe, nhanTin } from '../src/services/suKien'
import api from '../src/services/api'

// Đồng bộ dữ liệu: ghi ở đâu thì màn liên quan ở tab / máy khác tự tải lại NGAY (không chờ 10 giây, không cần F5).
function gan(reload, tuyChon) {
  const C = defineComponent({
    setup() {
      useAutoRefresh(reload, 3000, tuyChon)
      return () => h('div', [h('input', { id: 'o-nhap' })])
    },
  })
  return mount(C, { attachTo: document.body })
}
const tin = (...chuDe) => ({ chuDe, nguon: 'tab-khac' })

beforeEach(() => {
  vi.useFakeTimers()
  document.body.innerHTML = ''
  dangKetNoi.value = false
})
afterEach(() => {
  vi.useRealTimers()
})

describe('Kênh báo thay đổi (services/suKien)', () => {
  it('chỉ báo màn nghe đúng chủ đề; tin do chính tab này ghi thì bỏ; TAT_CA (vừa nối lại) báo mọi màn', () => {
    const donHang = vi.fn()
    const sanPham = vi.fn()
    const huy1 = nghe(['hoa-don'], donHang)
    const huy2 = nghe(['san-pham', 'ton-kho'], sanPham)
    nhanTin(tin('ton-kho'))
    expect(donHang).not.toHaveBeenCalled()
    expect(sanPham).toHaveBeenCalledTimes(1)
    nhanTin({ chuDe: ['hoa-don'], nguon: TAB_ID })
    expect(donHang).not.toHaveBeenCalled()
    nhanTin({ chuDe: [TAT_CA] })
    expect(donHang).toHaveBeenCalledTimes(1)
    expect(sanPham).toHaveBeenCalledTimes(2)
    huy1()
    huy2()
    nhanTin(tin('hoa-don', 'ton-kho'))
    expect(donHang).toHaveBeenCalledTimes(1)
    nhanTin(null)
    nhanTin({ chuDe: 'hoa-don' }) // tin hỏng: bỏ qua, không lỗi
  })

  it('mọi yêu cầu API gửi kèm mã tab để backend ghi nguồn của tin', async () => {
    const cauHinh = await api.interceptors.request.handlers[0].fulfilled({ headers: {} })
    expect(cauHinh.headers[TIEU_DE_TAB]).toBe(TAB_ID)
    expect(TAB_ID).toMatch(/^[A-Za-z0-9-]{1,64}$/) // backend chỉ nhận chữ, số, gạch nối
  })

  it('lỗi proxy 502 / 504 khi backend tắt hiển thị thông báo tiếng Việt rõ cổng 8081', async () => {
    const xuLyLoi = api.interceptors.response.handlers[0].rejected
    await expect(xuLyLoi({ response: { status: 502, data: '' }, message: 'Request failed with status code 502' })).rejects.toMatchObject({
      status: 502,
      message: 'Không kết nối được máy chủ. Hãy kiểm tra backend (cổng 8081) đã chạy chưa.',
    })
    await expect(xuLyLoi({ response: { status: 504 }, message: 'Request failed with status code 504' })).rejects.toMatchObject({
      status: 504,
      message: 'Không kết nối được máy chủ. Hãy kiểm tra backend (cổng 8081) đã chạy chưa.',
    })
    await expect(xuLyLoi({ response: { status: 400, data: { message: 'Kỳ không hợp lệ' } }, message: 'Bad Request' })).rejects.toMatchObject({
      status: 400,
      message: 'Kỳ không hợp lệ',
    })
  })
})

describe('Màn tự tải lại khi có tin (useAutoRefresh chuDe)', () => {
  it('có tin đúng chủ đề thì tải ngay (sau 150ms gộp tin), không chờ chu kỳ 10 giây; tin khác chủ đề thì thôi', async () => {
    const reload = vi.fn()
    const w = gan(reload, { chuDe: ['hoa-don'] })
    nhanTin(tin('san-pham'))
    await vi.advanceTimersByTimeAsync(400)
    expect(reload).not.toHaveBeenCalled()
    nhanTin(tin('hoa-don', 'ton-kho'))
    await vi.advanceTimersByTimeAsync(149)
    expect(reload).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(1)
    expect(reload).toHaveBeenCalledTimes(1)
    w.unmount()
    nhanTin(tin('hoa-don'))
    await vi.advanceTimersByTimeAsync(1000)
    expect(reload).toHaveBeenCalledTimes(1) // rời màn thì thôi nghe
  })

  it('nhiều tin sát nhau (một thao tác gọi vài API) chỉ tải một lần', async () => {
    const reload = vi.fn()
    const w = gan(reload, { chuDe: ['hoa-don'] })
    for (let i = 0; i < 5; i++) {
      nhanTin(tin('hoa-don'))
      await vi.advanceTimersByTimeAsync(100)
    }
    await vi.advanceTimersByTimeAsync(300)
    expect(reload).toHaveBeenCalledTimes(1)
    w.unmount()
  })

  it('đang gõ / mở hộp thoại / form chưa cho tải thì chờ, xong việc là tải ngay (không mất tin)', async () => {
    let choPhep = true
    const reload = vi.fn()
    const w = gan(reload, { chuDe: ['hoa-don'], chuKyMs: 0, coTheTai: () => choPhep })
    w.get('#o-nhap').element.focus()
    nhanTin(tin('hoa-don'))
    await vi.advanceTimersByTimeAsync(5000)
    expect(reload).not.toHaveBeenCalled()
    w.get('#o-nhap').element.blur()
    await vi.advanceTimersByTimeAsync(1000)
    expect(reload).toHaveBeenCalledTimes(1)

    choPhep = false
    nhanTin(tin('hoa-don'))
    await vi.advanceTimersByTimeAsync(5000)
    expect(reload).toHaveBeenCalledTimes(1)
    choPhep = true
    await vi.advanceTimersByTimeAsync(1000)
    expect(reload).toHaveBeenCalledTimes(2)
    w.unmount()
  })

  it('có tin mới trong lúc đang tải thì tải thêm một lần sau khi xong (kết quả cuối luôn mới nhất)', async () => {
    const xong = []
    const reload = vi.fn(() => new Promise((r) => xong.push(r)))
    const w = gan(reload, { chuDe: ['ton-kho'], chuKyMs: 0 })
    nhanTin(tin('ton-kho'))
    await vi.advanceTimersByTimeAsync(300)
    expect(reload).toHaveBeenCalledTimes(1)
    nhanTin(tin('ton-kho'))
    nhanTin(tin('ton-kho'))
    await vi.advanceTimersByTimeAsync(300)
    expect(reload).toHaveBeenCalledTimes(1) // không gọi chồng
    xong[0]()
    await vi.advanceTimersByTimeAsync(0)
    expect(reload).toHaveBeenCalledTimes(2)
    xong[1]()
    await vi.advanceTimersByTimeAsync(2000)
    expect(reload).toHaveBeenCalledTimes(2)
    w.unmount()
  })

  it('kênh đang sống thì chỉ hỏi lại định kỳ 60 giây để phòng sót; mất kênh thì quay về 10 giây', async () => {
    const reload = vi.fn()
    const w = gan(reload, { chuDe: ['hoa-don'] })
    dangKetNoi.value = true
    await vi.advanceTimersByTimeAsync(50_000)
    expect(reload).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(1)
    dangKetNoi.value = false
    await vi.advanceTimersByTimeAsync(10_000)
    expect(reload).toHaveBeenCalledTimes(2)
    w.unmount()
  })

  it('màn không khai báo chủ đề thì giữ nguyên cách cũ (không nghe tin)', async () => {
    const reload = vi.fn()
    const w = gan(reload)
    nhanTin(tin('hoa-don'))
    await vi.advanceTimersByTimeAsync(1000)
    expect(reload).not.toHaveBeenCalled()
    w.unmount()
  })
})

describe('useSuKien (bảng chọn sản phẩm tại quầy, nút ca làm việc)', () => {
  it('gọi ngay cả khi đang mở hộp thoại, gộp tin sát nhau', async () => {
    const cb = vi.fn()
    const C = defineComponent({
      setup() {
        useSuKien(['ton-kho'], cb)
        return () => h('div', { 'aria-modal': 'true' })
      },
    })
    const w = mount(C, { attachTo: document.body })
    nhanTin(tin('ton-kho'))
    nhanTin(tin('ton-kho'))
    await vi.advanceTimersByTimeAsync(300)
    expect(cb).toHaveBeenCalledTimes(1)
    w.unmount()
    nhanTin(tin('ton-kho'))
    await vi.advanceTimersByTimeAsync(300)
    expect(cb).toHaveBeenCalledTimes(1)
  })
})
