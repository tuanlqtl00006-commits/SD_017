import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('../src/services/diaChiHanhChinhService', () => ({
  API_DIA_CHI: 'x',
  diaChiHanhChinhService: { layTinhThanh: async () => [], layPhuongXa: async () => [], xoaNho() {} },
}))

import KhachHangAddressModal from '../src/components/KhachHangAddressModal.vue'
import { useToast } from '../src/composables/useToast'
import { LOI_DIA_CHI_CHI_TIET_CHU_SO, SO_DIA_CHI_MAX, loiDiaChiCuThe } from '../src/utils/kiemTra'

const KHACH = { id: 7, hoTen: 'Lê Minh Tuấn', sdt: '0901000003' }
const dc = (id, macDinh = false, extra = {}) => ({
  id,
  tenNguoiNhan: `Người ${id}`,
  sdtNguoiNhan: '0901000003',
  tinhThanhPho: 'Thành phố Hà Nội',
  phuongXa: 'Phường Hà Đông',
  quanHuyen: '',
  diaChiCuThe: `${id} Trần Phú`,
  laDiaChiMacDinh: macDinh,
  ...extra,
})

async function mo(ds) {
  api.get.mockResolvedValue({ data: ds })
  const w = mount(KhachHangAddressModal, { attachTo: document.body, props: { customer: KHACH } })
  await flushPromises()
  return w
}
const body = () => new DOMWrapper(document.body)
const nutThem = () => [...document.body.querySelectorAll('button.kh-dc-them')][0]

beforeEach(() => {
  document.body.innerHTML = ''
  Object.values(api).forEach((f) => f.mockReset())
  useToast().toasts.splice(0)
})

describe('Luật "địa chỉ cụ thể phải có chữ hoặc số"', () => {
  it('trống / quá dài / chỉ toàn dấu đều báo lỗi, có chữ hoặc số thì hợp lệ', () => {
    expect(loiDiaChiCuThe('')).toBe('Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).')
    expect(loiDiaChiCuThe('   ')).toContain('Vui lòng nhập')
    expect(loiDiaChiCuThe('x'.repeat(256))).toBe('Địa chỉ cụ thể tối đa 255 ký tự.')
    for (const sai of ['---', '...', '@#$%', '- . -']) expect(loiDiaChiCuThe(sai), sai).toBe(LOI_DIA_CHI_CHI_TIET_CHU_SO)
    for (const dung of ['12', 'Ngõ 5', 'Số 1/2 Trần Phú', 'Thôn Đoài']) expect(loiDiaChiCuThe(dung), dung).toBe('')
  })

  it('cùng giới hạn 10 địa chỉ với backend', () => {
    expect(SO_DIA_CHI_MAX).toBe(10)
  })

  it('form địa chỉ báo lỗi ngay dưới ô khi địa chỉ cụ thể chỉ có dấu', async () => {
    const w = await mo([dc(1, true)])
    nutThem().click()
    await flushPromises()
    const o = document.body.querySelector('#kh-dc-chi-tiet') || document.body.querySelector('input[placeholder*="Số nhà"]')
    expect(o, 'không thấy ô địa chỉ cụ thể').toBeTruthy()
    o.value = '---'
    o.dispatchEvent(new Event('input'))
    o.dispatchEvent(new Event('blur'))
    await flushPromises()
    expect(body().text()).toContain(LOI_DIA_CHI_CHI_TIET_CHU_SO)
    w.unmount()
  })
})

describe('Cửa sổ địa chỉ: nút "Đặt làm địa chỉ mặc định"', () => {
  it('chỉ địa chỉ không phải mặc định mới có nút; bấm gọi API riêng /mac-dinh rồi tải lại danh sách', async () => {
    const w = await mo([dc(1, true), dc(2, false)])
    const nut = [...document.body.querySelectorAll('button[title="Đặt làm địa chỉ mặc định"]')]
    expect(nut).toHaveLength(1)
    expect(nut[0].getAttribute('aria-label')).toContain('Người 2')

    api.put.mockResolvedValue({ data: {} })
    api.get.mockResolvedValue({ data: [dc(1, false), dc(2, true)] })
    nut[0].click()
    await flushPromises()
    expect(api.put).toHaveBeenCalledTimes(1)
    expect(api.put).toHaveBeenCalledWith('/dia-chi/2/mac-dinh')
    expect(useToast().toasts.at(-1).message).toBe('Đã đặt làm địa chỉ mặc định.')
    expect(w.emitted('address-updated')).toHaveLength(1)
    // sau khi tải lại: địa chỉ 2 lên đầu và đang là mặc định, địa chỉ 1 mới có nút
    const tieuDe = [...document.body.querySelectorAll('.kh-dc-item-ten')].map((e) => e.textContent.replace(/\s+/g, ' ').trim())
    expect(tieuDe).toEqual(['Người 2 Mặc định', 'Người 1'])
    expect(document.body.querySelectorAll('button[title="Đặt làm địa chỉ mặc định"]')).toHaveLength(1)
    w.unmount()
  })

  it('API lỗi thì báo lỗi và không phát sự kiện cập nhật', async () => {
    const w = await mo([dc(1, true), dc(2, false)])
    api.put.mockRejectedValue(new Error('Không tìm thấy địa chỉ.'))
    document.body.querySelector('button[title="Đặt làm địa chỉ mặc định"]').click()
    await flushPromises()
    expect(useToast().toasts.at(-1)).toMatchObject({ type: 'error', message: 'Không tìm thấy địa chỉ.' })
    expect(w.emitted('address-updated')).toBeUndefined()
    w.unmount()
  })
})

describe('Cửa sổ địa chỉ: tối đa 10 địa chỉ', () => {
  it('còn dưới 10 địa chỉ thì thêm được và không có lời nhắc', async () => {
    const ds = Array.from({ length: 9 }, (_, i) => dc(i + 1, i === 0))
    const w = await mo(ds)
    expect(nutThem().disabled).toBe(false)
    expect(body().text()).not.toContain('đã có đủ 10 địa chỉ')
    w.unmount()
  })

  it('đủ 10 địa chỉ thì khóa nút Thêm và nhắc xóa bớt; vẫn sửa / xóa được', async () => {
    const ds = Array.from({ length: 10 }, (_, i) => dc(i + 1, i === 0))
    const w = await mo(ds)
    expect(nutThem().disabled).toBe(true)
    expect(body().text()).toContain('Khách hàng đã có đủ 10 địa chỉ')
    nutThem().click() // bị khóa: không mở form
    await flushPromises()
    expect(document.body.querySelector('#kh-dc-form')).toBeNull()
    expect(document.body.querySelectorAll('button[title="Sửa địa chỉ"]')).toHaveLength(10)
    expect(document.body.querySelectorAll('button[title="Xóa địa chỉ"]')).toHaveLength(10)
    w.unmount()
  })
})

describe('Cửa sổ địa chỉ: lỗi trùng địa chỉ từ máy chủ', () => {
  async function moFormDaDien(ds) {
    const w = await mo(ds)
    nutThem().click()
    await flushPromises()
    const form = document.body.querySelector('#kh-dc-form')
    expect(form).toBeTruthy()
    expect(form.querySelector('#kh-dc-ten').value).toBe('Lê Minh Tuấn') // tự lấy tên + SĐT của khách
    expect(form.querySelector('#kh-dc-sdt').value).toBe('0901000003')
    // Tỉnh / phường là ô chọn dữ liệu từ API (đã giả rỗng ở đây) nên gán thẳng giá trị vào form
    Object.assign(w.vm.f, { tinhThanhPho: 'Thành phố Hà Nội', phuongXa: 'Phường Hà Đông', diaChiCuThe: '1 Trần Phú' })
    await flushPromises()
    return { w, form }
  }

  it('thêm địa chỉ trùng: hiện đúng câu 409 của backend, form vẫn mở, không phát sự kiện cập nhật', async () => {
    const { w, form } = await moFormDaDien([dc(1, true)])
    api.post.mockRejectedValue(Object.assign(new Error('Địa chỉ này đã có trong sổ địa chỉ của khách hàng.'), { status: 409 }))
    form.dispatchEvent(new Event('submit'))
    await flushPromises()
    expect(api.post).toHaveBeenCalledTimes(1)
    expect(api.post.mock.calls[0][0]).toBe('/dia-chi/khach-hang/7')
    expect(useToast().toasts.at(-1)).toMatchObject({ type: 'error', message: 'Địa chỉ này đã có trong sổ địa chỉ của khách hàng.' })
    expect(document.body.querySelector('#kh-dc-form')).toBeTruthy()
    expect(w.emitted('address-updated')).toBeUndefined()
    w.unmount()
  })

  it('địa chỉ hợp lệ không trùng: gửi đúng dữ liệu, đóng form và tải lại danh sách', async () => {
    const { w, form } = await moFormDaDien([dc(1, true)])
    api.post.mockResolvedValue({ data: dc(2) })
    form.dispatchEvent(new Event('submit'))
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/dia-chi/khach-hang/7', expect.objectContaining({
      tenNguoiNhan: 'Lê Minh Tuấn',
      sdtNguoiNhan: '0901000003',
      tinhThanhPho: 'Thành phố Hà Nội',
      phuongXa: 'Phường Hà Đông',
      diaChiCuThe: '1 Trần Phú',
    }))
    expect(useToast().toasts.at(-1).message).toBe('Đã thêm địa chỉ mới.')
    expect(document.body.querySelector('#kh-dc-form')).toBeNull()
    expect(w.emitted('address-updated')).toHaveLength(1)
    w.unmount()
  })

  it('địa chỉ cụ thể chỉ có dấu: chặn ngay ở form, không gọi API', async () => {
    const { w, form } = await moFormDaDien([dc(1, true)])
    w.vm.f.diaChiCuThe = '---'
    form.dispatchEvent(new Event('submit'))
    await flushPromises()
    expect(api.post).not.toHaveBeenCalled()
    expect(document.body.querySelector('#kh-dc-chi-tiet-msg').textContent).toBe(LOI_DIA_CHI_CHI_TIET_CHU_SO)
    w.unmount()
  })
})
