import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import HoaDonScannerModal from '../src/components/hoaDon/HoaDonScannerModal.vue'
import HoaDonManager from '../src/components/HoaDonManager.vue'

import { quetMaTuAnh } from '../src/utils/qrHoaDon'

const body = () => new DOMWrapper(document.body)

let mockApi

vi.mock('../src/services/api', () => ({
  default: new Proxy({}, {
    get: (_, k) => (...a) => mockApi[k](...a),
  }),
}))

vi.mock('../src/utils/qrHoaDon', async () => {
  const actual = await vi.importActual('../src/utils/qrHoaDon')
  return {
    ...actual,
    quetMaTuAnh: vi.fn(),
  }
})

const MOCK_HD_CHI_TIET = {
  hoaDon: {
    id: 16,
    maHoaDon: 'HD16',
    loaiDon: 2,
    tenLoaiDon: 'Trực tuyến',
    trangThai: 1,
    ngayTao: '2026-10-08T14:39:33',
    tenNhanVien: 'Nguyễn Hoàng Long',
    tongTien: 3861000,
    soTienGiam: 0,
    phiVanChuyen: 30000,
    thanhTien: 3891000,
  },
}

describe('HoaDonScannerModal - Quét mã hóa đơn', () => {
  let router

  beforeEach(async () => {
    document.body.innerHTML = ''
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div/>' } },
        { path: '/hoa-don', component: { template: '<div/>' } },
        { path: '/hoa-don/:id', component: { template: '<div id="hd-detail"/>' } },
      ],
    })
    router.push('/hoa-don')
    await router.isReady()

    mockApi = {
      get: vi.fn(async (url, cfg) => {
        if (url === '/hoa-don/tra-cuu') {
          if (cfg?.params?.ma === 'HD16') return { data: MOCK_HD_CHI_TIET }
          const err = new Error('Không tìm thấy')
          err.response = { data: { message: 'Không tìm thấy hóa đơn có mã "HD_SAI".' } }
          throw err
        }
        if (url === '/hoa-don') {
          const maHoaDon = cfg?.params?.maHoaDon
          if (!maHoaDon || maHoaDon === 'HD16') return { data: { content: [MOCK_HD_CHI_TIET.hoaDon], totalElements: 1 } }
          return { data: { content: [], totalElements: 0 } }
        }
        if (url.startsWith('/hoa-don/')) {
          return { data: MOCK_HD_CHI_TIET }
        }
        return { data: {} }
      }),
    }
  })

  it('hiển thị 2 phương thức quét: Camera, Tải ảnh mã (không còn Máy quét / Nhập mã)', async () => {
    const w = mount(HoaDonScannerModal, {
      global: { plugins: [router] },
      attachTo: document.body,
    })
    await flushPromises()

    const tabs = body().findAll('.ad-segment-item').map((el) => el.text())
    expect(tabs.some((t) => t.includes('Camera'))).toBe(true)
    expect(tabs.some((t) => t.includes('Tải ảnh mã'))).toBe(true)
    expect(tabs.some((t) => t.includes('Máy quét'))).toBe(false)
    w.unmount()
  })

  it('tab Tải ảnh mã: tải ảnh có mã HD16 -> tìm thấy thì đóng modal và mở ngay chi tiết hóa đơn', async () => {
    vi.mocked(quetMaTuAnh).mockResolvedValue('HD16')
    const w = mount(HoaDonScannerModal, {
      global: { plugins: [router] },
      attachTo: document.body,
    })
    await flushPromises()

    const tabAnh = body().findAll('.ad-segment-item').find((el) => el.text().includes('Tải ảnh mã'))
    await tabAnh.trigger('click')
    await flushPromises()

    const input = body().get('#hd-file-qr')
    const file = new File(['dummy'], 'qr.png', { type: 'image/png' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await new Promise((r) => setTimeout(r, 50))
    await flushPromises()

    // Kiểm tra API được gọi
    expect(mockApi.get).toHaveBeenCalledWith('/hoa-don/tra-cuu', expect.objectContaining({
      params: { ma: 'HD16' },
    }))

    // Kiểm tra đã phát sự kiện đóng modal
    expect(w.emitted('close')).toBeTruthy()

    // Kiểm tra đã chuyển hướng thẳng tới trang chi tiết hóa đơn (/hoa-don/16)
    expect(router.currentRoute.value.path).toBe('/hoa-don/16')

    w.unmount()
  })

  it('báo lỗi khi không tìm thấy hóa đơn khớp mã từ ảnh', async () => {
    vi.mocked(quetMaTuAnh).mockResolvedValue('HD_SAI')
    const w = mount(HoaDonScannerModal, {
      global: { plugins: [router] },
      attachTo: document.body,
    })
    await flushPromises()

    const tabAnh = body().findAll('.ad-segment-item').find((el) => el.text().includes('Tải ảnh mã'))
    await tabAnh.trigger('click')
    await flushPromises()

    const input = body().get('#hd-file-qr')
    const file = new File(['dummy'], 'qr.png', { type: 'image/png' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    expect(body().text()).toContain('Không tìm thấy hóa đơn có mã')
    w.unmount()
  })
})

describe('HoaDonManager - Tích hợp nút quét mã hóa đơn', () => {
  let router

  beforeEach(async () => {
    document.body.innerHTML = ''
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div/>' } },
        { path: '/hoa-don', component: { template: '<div/>' } },
        { path: '/hoa-don/:id', component: { template: '<div/>' } },
      ],
    })
    router.push('/hoa-don')
    await router.isReady()

    mockApi = {
      get: vi.fn(async (url) => {
        if (url === '/hoa-don/dem-trang-thai') return { data: { tatCa: 1 } }
        if (url === '/hoa-don') {
          return { data: { content: [MOCK_HD_CHI_TIET.hoaDon], totalElements: 1 } }
        }
        return { data: {} }
      }),
    }
  })

  it('có nút "Quét mã hóa đơn" ở tiêu đề danh sách và nút icon quét trong ô nhập mã', async () => {
    const w = mount(HoaDonManager, {
      global: { plugins: [router] },
      attachTo: document.body,
    })
    await flushPromises()

    // 1. Nút "Quét mã hóa đơn" trên danh sách
    const btnQuetHeader = w.findAll('button').find((b) => b.text().includes('Quét mã hóa đơn'))
    expect(btnQuetHeader).toBeTruthy()

    // 2. Nút icon quét trong ô lọc
    const btnIconQuet = w.get('button[title="Quét mã hóa đơn"]')
    expect(btnIconQuet.exists()).toBe(true)

    // Bấm nút -> mở modal quét
    await btnQuetHeader.trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Quét mã hóa đơn')
    w.unmount()
  })
})
