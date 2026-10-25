import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))

import SuaDonModal from '../src/components/hoaDon/SuaDonModal.vue'

const body = () => new DOMWrapper(document.body)
const DATA = {
  hoaDon: { id: 5, maHoaDon: 'HD05', thanhTien: 530000 },
  giaoHang: { hoTenNguoiNhan: 'Nguyễn Thị Lan', sdtNguoiNhan: '0901000001', diaChi: 'Số 15 Trần Phú, Hà Đông, Hà Nội' },
  sanPham: [{ id: 11, idBienThe: 1, tenSanPham: 'Ống cầu Thành Công', maSpct: 'SP07-TRANG', donGia: 250000, soLuong: 2 }],
  daThanhToan: 530000,
}

beforeEach(() => {
  document.body.innerHTML = ''
  for (const f of [api.get, api.post, api.put]) f.mockReset()
})

describe('Sửa đơn online (case khó)', () => {
  it('bắt buộc lý do; thêm sản phẩm mới gửi idChiTiet null, dòng cũ giữ idChiTiet', async () => {
    api.get.mockResolvedValue({ data: [{ id: 2, tenSanPham: 'Vợt Yonex', ma: 'SP01-DEN', donGia: 1200000, tonKho: 3 }] })
    api.put.mockResolvedValue({ data: { ...DATA, ketQuaSua: { moTa: 'Phụ phí 1.170.000 ₫: khách trả thêm khi nhận hàng' } } })
    const w = mount(SuaDonModal, { props: { data: DATA }, attachTo: document.body })
    await body().find('[data-testid="sd-luu"]').trigger('click')
    await body().find('#sua-don-form').trigger('submit')
    await flushPromises()
    expect(body().text()).toContain('Vui lòng ghi lý do chỉnh sửa')
    expect(api.put).not.toHaveBeenCalled()

    await body().find('input[type="search"]').setValue('vot')
    await new Promise((r) => setTimeout(r, 350))
    await flushPromises()
    await body().find('.sd-goi-y button').trigger('click')
    expect(body().text()).toContain('Thêm mới · giá hiện tại')
    expect(body().text()).toContain('Miễn phí')
    await body().find('#sd-ly-do').setValue('Khách gọi thêm 1 vợt')
    await body().find('#sua-don-form').trigger('submit')
    await flushPromises()
    expect(api.put).toHaveBeenCalledWith('/hoa-don/5/sua-don', expect.objectContaining({
      lyDo: 'Khách gọi thêm 1 vợt',
      items: [{ idChiTiet: 11, idBienThe: 1, soLuong: 2 }, { idChiTiet: null, idBienThe: 2, soLuong: 1 }],
    }))
    expect(w.emitted('saved')).toHaveLength(1)
    w.unmount()
  })

  it('bỏ hết sản phẩm thì không cho lưu', async () => {
    const w = mount(SuaDonModal, { props: { data: DATA }, attachTo: document.body })
    await body().find('.ad-icon-btn.is-danger').trigger('click')
    await body().find('#sd-ly-do').setValue('Khách bỏ hết')
    await body().find('#sua-don-form').trigger('submit')
    await flushPromises()
    expect(body().text()).toContain('Đơn phải còn ít nhất 1 sản phẩm')
    expect(api.put).not.toHaveBeenCalled()
    w.unmount()
  })
})
