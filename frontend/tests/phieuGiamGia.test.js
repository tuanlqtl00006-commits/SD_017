import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { tinhTrangThai, isExpired } from '../src/constants/phieuGiamGia'
import { useToast } from '../src/composables/useToast'

// api giả: mọi lời gọi đi qua mock.api (đặt lại ở mỗi test)
let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

beforeEach(() => {
  document.body.innerHTML = ''
  useToast().toasts.splice(0)
})
afterEach(() => vi.useRealTimers())

/** Giả "hôm nay" (chỉ đổi Date, giữ nguyên bộ hẹn giờ) để kiểm tra theo ngày cụ thể. */
function gia(y, m, d) {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(y, m - 1, d, 10, 0, 0))
}

/** Phiếu 9/10 -> 15/10 giống ví dụ của cô. */
const phieu = (over = {}) => ({
  id: 1, ma: 'VCH0001', ten: 'Tri ân', hinhThuc: 'CA_NHAN', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000,
  donToiThieu: 500000, soLuong: 3, soLuongDaDung: 0, gioiHanMoiKhach: 1, ngayBatDau: '2026-10-09T00:00:00', ngayKetThuc: '2026-10-15T23:59:59',
  hoatDong: true, khachHangs: [], ...over,
})

describe('Quy tắc công tắc theo khoảng ngày giờ (hàm thuần)', () => {
  it('quá giờ kết thúc: hết ngày 15/10 (23:59:59) thì từ 16/10 là đã hết hạn (công tắc tắt và khóa)', () => {
    expect(isExpired(phieu(), '2026-10-09T00:00:00')).toBe(false)
    expect(isExpired(phieu(), '2026-10-14T12:00:00')).toBe(false)
    expect(isExpired(phieu(), '2026-10-15T23:59:59')).toBe(false)
    expect(isExpired(phieu(), '2026-10-16T00:00:00')).toBe(true)
  })
  it('phiếu cũ lưu theo ngày (không có giờ) vẫn chạy đúng: bắt đầu 00:00, kết thúc hết ngày', () => {
    const cu = phieu({ ngayBatDau: '2026-10-09', ngayKetThuc: '2026-10-15' })
    expect(isExpired(cu, '2026-10-15')).toBe(false)
    expect(isExpired(cu, '2026-10-16')).toBe(true)
    expect(tinhTrangThai(cu, '2026-10-08')).toBe('SAP_DIEN_RA')
    expect(tinhTrangThai(cu, '2026-10-09')).toBe('DANG_HOAT_DONG')
  })
  it('từ 16/10: công tắc bật = "Đã kết thúc", công tắc tắt = "Ngưng hoạt động"', () => {
    expect(tinhTrangThai(phieu({ hoatDong: true }), '2026-10-16T00:00:00')).toBe('DA_KET_THUC')
    expect(tinhTrangThai(phieu({ hoatDong: true }), '2027-01-01T00:00:00')).toBe('DA_KET_THUC')
    expect(tinhTrangThai(phieu({ hoatDong: false }), '2026-10-16T00:00:00')).toBe('NGUNG_HOAT_DONG')
    expect(tinhTrangThai(phieu({ hoatDong: false }), '2027-01-01T00:00:00')).toBe('NGUNG_HOAT_DONG')
  })
  it('trước 16/10: sắp diễn ra / đang hoạt động / ngưng hoạt động', () => {
    expect(tinhTrangThai(phieu(), '2026-10-08T23:59:59')).toBe('SAP_DIEN_RA')
    expect(tinhTrangThai(phieu(), '2026-10-09T00:00:00')).toBe('DANG_HOAT_DONG')
    expect(tinhTrangThai(phieu(), '2026-10-15T12:00:00')).toBe('DANG_HOAT_DONG')
    expect(tinhTrangThai(phieu({ hoatDong: false }), '2026-10-12T12:00:00')).toBe('NGUNG_HOAT_DONG')
  })
  it('tính tới từng giờ phút: bắt đầu 08:00 / kết thúc 17:00 (hết 17:00:59)', () => {
    const p = phieu({ ngayBatDau: '2026-10-15T08:00:00', ngayKetThuc: '2026-10-15T17:00:59' })
    expect(tinhTrangThai(p, '2026-10-15T07:59:59')).toBe('SAP_DIEN_RA')
    expect(tinhTrangThai(p, '2026-10-15T08:00:00')).toBe('DANG_HOAT_DONG')
    expect(tinhTrangThai(p, '2026-10-15T17:00:59')).toBe('DANG_HOAT_DONG')
    expect(tinhTrangThai(p, '2026-10-15T17:01:00')).toBe('DA_KET_THUC')
    expect(isExpired(p, '2026-10-15T17:00:59')).toBe(false)
    expect(isExpired(p, '2026-10-15T17:01:00')).toBe(true)
  })
})

describe('Danh sách phiếu giảm giá: công tắc và trạng thái', () => {
  async function montarDanhSach(ngay) {
    gia(...ngay)
    mock = { api: { get: vi.fn(async () => ({ data: [phieu({ id: 1, ma: 'VCHBAT01', hoatDong: true }), phieu({ id: 2, ma: 'VCHTAT02', hoatDong: false })] })) } }
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<div/>' } }] })
    router.push('/phieu-giam-gia'); await router.isReady()
    const Comp = (await import('../src/components/PhieuGiamGiaManager.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const dong = (w, ma) => w.findAll('tbody tr').find((tr) => tr.text().includes(ma))

  it('ngày 15/10 (ngày kết thúc): vẫn còn công tắc ở cả hai dòng, chưa phải đã kết thúc', async () => {
    const w = await montarDanhSach([2026, 10, 15])
    expect(dong(w, 'VCHBAT01').find('button[role="switch"]').exists()).toBe(true)
    expect(dong(w, 'VCHTAT02').find('button[role="switch"]').exists()).toBe(true)
    expect(dong(w, 'VCHBAT01').text()).toContain('Đang hoạt động')
    expect(dong(w, 'VCHTAT02').text()).toContain('Ngưng hoạt động')
    w.unmount()
  })

  it('ngày 16/10 (đã kết thúc): công tắc TẮT và bị khóa ở cả hai dòng; nhãn giữ nguyên', async () => {
    const w = await montarDanhSach([2026, 10, 16])
    for (const ma of ['VCHBAT01', 'VCHTAT02']) {
      const sw = dong(w, ma).find('button[role="switch"]')
      expect(sw.exists(), `công tắc ${ma}`).toBe(true)
      expect(sw.attributes('aria-checked'), `${ma} phải tắt`).toBe('false')
      expect(sw.classes()).not.toContain('is-on')
      expect(sw.attributes('disabled'), `${ma} phải khóa`).toBeDefined()
      expect(sw.attributes('title'), `${ma} có chú thích`).toContain('Đã kết thúc')
    }
    expect(dong(w, 'VCHBAT01').text()).toContain('Đã kết thúc')
    expect(dong(w, 'VCHBAT01').text()).not.toContain('Đang hoạt động')
    expect(dong(w, 'VCHTAT02').text()).toContain('Ngưng hoạt động')
    expect(dong(w, 'VCHTAT02').text()).not.toContain('Đã kết thúc')
    w.unmount()
  })

  it('ngày 15/10 (ngày cuối): công tắc dòng đang bật vẫn BẬT và dùng được', async () => {
    const w = await montarDanhSach([2026, 10, 15])
    const sw = dong(w, 'VCHBAT01').find('button[role="switch"]')
    expect(sw.attributes('aria-checked')).toBe('true')
    expect(sw.attributes('disabled')).toBeUndefined()
    expect(dong(w, 'VCHTAT02').find('button[role="switch"]').attributes('aria-checked')).toBe('false')
    w.unmount()
  })

  it('đã kết thúc: bấm công tắc không hiện hộp xác nhận và không gọi API', async () => {
    const w = await montarDanhSach([2026, 10, 16])
    mock.api.put = vi.fn(async () => ({ data: {} }))
    await dong(w, 'VCHBAT01').find('button[role="switch"]').trigger('click')
    await flushPromises()
    expect(document.body.textContent).not.toContain('Ẩn phiếu giảm giá?')
    expect(mock.api.put).not.toHaveBeenCalled()
    expect(dong(w, 'VCHBAT01').text()).toContain('Đã kết thúc')
    w.unmount()
  })

  it('còn hạn: bấm công tắc hiện hộp xác nhận "Ẩn phiếu" rồi gọi API đổi trạng thái', async () => {
    const w = await montarDanhSach([2026, 10, 12])
    mock.api.put = vi.fn(async () => ({ data: phieu({ id: 1, ma: 'VCHBAT01', hoatDong: false }) }))
    mock.api.get = vi.fn(async () => ({ data: [phieu({ id: 1, ma: 'VCHBAT01', hoatDong: false }), phieu({ id: 2, ma: 'VCHTAT02', hoatDong: false })] }))
    await dong(w, 'VCHBAT01').find('button[role="switch"]').trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Ẩn phiếu giảm giá?')
    const nut = [...document.body.querySelectorAll('button')].find((b) => b.textContent.trim() === 'Ẩn phiếu')
    nut.click()
    await flushPromises()
    expect(mock.api.put).toHaveBeenCalledTimes(1)
    expect(mock.api.put.mock.calls[0][0]).toBe('/phieu-giam-gia/1/trang-thai')
    expect(dong(w, 'VCHBAT01').text()).toContain('Ngưng hoạt động')
    w.unmount()
  })

  it('ngày 8/10 (chưa tới ngày bắt đầu): còn công tắc, trạng thái "Sắp diễn ra"', async () => {
    const w = await montarDanhSach([2026, 10, 8])
    expect(dong(w, 'VCHBAT01').find('button[role="switch"]').exists()).toBe(true)
    expect(dong(w, 'VCHBAT01').text()).toContain('Sắp diễn ra')
    w.unmount()
  })
})

describe('Lưu phiếu giảm giá: thông báo kèm kết quả gửi mail', () => {
  const payload = { ma: 'VCH0001', ten: 'Tri ân', hinhThuc: 'CA_NHAN', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000, donToiThieu: 500000, soLuong: 3, gioiHanMoiKhach: null, ngayBatDau: '2026-10-09T00:00:00', ngayKetThuc: '2026-10-15T23:59:00', khachHangIds: [2, 4, 5] }

  async function montarForm(path, { get = {}, post, put } = {}) {
    mock = {
      api: {
        get: vi.fn(async (url) => ({ data: url in get ? get[url] : [] })),
        post: vi.fn(post || (async () => ({ data: { id: 9 } }))),
        put: vi.fn(put || (async () => ({ data: { id: 1 } }))),
      },
    }
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/phieu-giam-gia', component: { template: '<div/>' } },
        { path: '/phieu-giam-gia/them', component: { template: '<div/>' } },
        { path: '/phieu-giam-gia/:id(\\d+)/sua', component: { template: '<div/>' } },
      ],
    })
    router.push(path); await router.isReady()
    const Comp = (await import('../src/components/phieuGiamGia/PhieuGiamGiaFormPage.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    return { w, router }
  }
  const thongBao = () => useToast().toasts.map((t) => t.message)

  it('tạo phiếu cá nhân: thông báo có câu ketQuaMail từ backend', async () => {
    const { w, router } = await montarForm('/phieu-giam-gia/them', {
      post: async () => ({ data: { id: 9, ketQuaMail: 'Đang gửi 3 mail phiếu mới.' } }),
    })
    await w.findComponent({ name: 'PhieuGiamGiaFormModal' }).vm.$emit('save', payload)
    await flushPromises()
    expect(mock.api.post).toHaveBeenCalledTimes(1)
    expect(mock.api.post.mock.calls[0][0]).toBe('/phieu-giam-gia')
    expect(mock.api.post.mock.calls[0][1].khachHangIds).toEqual([2, 4, 5])
    expect(thongBao()).toEqual(['Tạo phiếu giảm giá thành công. Đang gửi 3 mail phiếu mới.'])
    expect(router.currentRoute.value.path).toBe('/phieu-giam-gia')
    w.unmount()
  })

  it('sửa phiếu: thông báo có câu ketQuaMail (mới + cập nhật + hủy)', async () => {
    const { w, router } = await montarForm('/phieu-giam-gia/1/sua', {
      get: { '/phieu-giam-gia/1': phieu({ khachHangs: [2, 4, 5].map((id) => ({ id, ma: `KH${id}`, hoTen: `Khách ${id}` })) }) },
      put: async () => ({ data: { id: 1, ketQuaMail: 'Đang gửi 2 mail phiếu mới, 1 mail cập nhật, 2 mail hủy phiếu.' } }),
    })
    await w.findComponent({ name: 'PhieuGiamGiaFormModal' }).vm.$emit('save', { ...payload, khachHangIds: [1, 3, 5], giaTri: 15 })
    await flushPromises()
    expect(mock.api.put).toHaveBeenCalledTimes(1)
    expect(mock.api.put.mock.calls[0][0]).toBe('/phieu-giam-gia/1')
    expect(thongBao()).toEqual(['Đã lưu thay đổi phiếu giảm giá. Đang gửi 2 mail phiếu mới, 1 mail cập nhật, 2 mail hủy phiếu.'])
    expect(router.currentRoute.value.path).toBe('/phieu-giam-gia')
    w.unmount()
  })

  it('chưa cấu hình Gmail: phiếu vẫn lưu nhưng thông báo ĐỎ nói rõ mail CHƯA gửi (không báo "đã gửi")', async () => {
    const msg = 'Chưa gửi được mail thật vì chưa cấu hình Gmail (điền email + mật khẩu ứng dụng vào backend/src/main/resources/mail.properties rồi chạy lại backend).'
    const { w, router } = await montarForm('/phieu-giam-gia/them', { post: async () => ({ data: { id: 9, ketQuaMail: msg } }) })
    await w.findComponent({ name: 'PhieuGiamGiaFormModal' }).vm.$emit('save', payload)
    await flushPromises()
    const t = useToast().toasts
    expect(t).toHaveLength(1)
    expect(t[0].type).toBe('error')
    expect(t[0].title).toBe('Phiếu đã lưu nhưng mail chưa gửi')
    expect(t[0].message).toContain('Tạo phiếu giảm giá thành công.')
    expect(t[0].message).toContain('chưa cấu hình Gmail')
    expect(router.currentRoute.value.path).toBe('/phieu-giam-gia')
    w.unmount()
  })

  it('không có mail nào (ketQuaMail = null): thông báo chỉ có câu lưu thành công, không thừa chữ', async () => {
    const { w } = await montarForm('/phieu-giam-gia/them', { post: async () => ({ data: { id: 9, ketQuaMail: null } }) })
    await w.findComponent({ name: 'PhieuGiamGiaFormModal' }).vm.$emit('save', { ...payload, hinhThuc: 'CONG_KHAI', khachHangIds: [] })
    await flushPromises()
    expect(thongBao()).toEqual(['Tạo phiếu giảm giá thành công.'])
    w.unmount()
  })

  it('backend báo lỗi: hiện lỗi, ở lại trang và không có thông báo thành công', async () => {
    const { w, router } = await montarForm('/phieu-giam-gia/them', {
      post: async () => { throw new Error('Mã phiếu đã tồn tại.') },
    })
    await w.findComponent({ name: 'PhieuGiamGiaFormModal' }).vm.$emit('save', payload)
    await flushPromises()
    expect(thongBao()).toEqual(['Mã phiếu đã tồn tại.'])
    expect(useToast().toasts[0].type).toBe('error')
    expect(router.currentRoute.value.path).toBe('/phieu-giam-gia/them')
    w.unmount()
  })
})
