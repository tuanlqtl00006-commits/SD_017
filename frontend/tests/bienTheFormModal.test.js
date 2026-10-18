import { beforeEach, describe, it, expect } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import BienTheFormModal from '../src/components/sanPham/BienTheFormModal.vue'
import { datSelect2 } from './helpersSelect2'

const body = () => new DOMWrapper(document.body)

beforeEach(() => {
  document.body.innerHTML = ''
})

describe('BienTheFormModal - Kiểm tra dữ liệu và touch tracking', () => {
  const sanPhams = [
    { id: 1, ma: 'SP01', ten: 'Vợt Astrox 99', hoatDong: true },
    { id: 2, ma: 'SP02', ten: 'Bao vợt Pro', hoatDong: true },
    { id: 3, ma: 'SP03', ten: 'Vợt cũ ngưng bán', hoatDong: false },
  ]

  const optionsFn = (slug) => {
    if (slug === 'mau-sac') return [{ id: 10, ten: 'Đen', hoatDong: true }, { id: 11, ten: 'Đỏ', hoatDong: true }]
    if (slug === 'trong-luong') return [{ id: 20, ten: '4U', hoatDong: true }, { id: 99, ten: 'Không áp dụng', hoatDong: true }]
    if (slug === 'chu-vi') return [{ id: 30, ten: 'G5', hoatDong: true }, { id: 98, ten: 'Không áp dụng', hoatDong: true }]
    return []
  }

  const laVotFn = (idSp) => idSp === 1 || idSp === 3
  const idKadFn = (slug) => (slug === 'trong-luong' ? 99 : 98)

  const allVariants = [
    { id: 101, idSanPham: 1, idMauSac: 10, idTrongLuong: 20, idChuVi: 30, ma: 'SP01-DEN-4U-G5', giaBan: 2000000, soLuongTon: 10 },
    { id: 102, idSanPham: 2, idMauSac: 10, idTrongLuong: 99, idChuVi: 98, ma: 'SP02-DEN', giaBan: 500000, soLuongTon: 5 },
  ]

  it('hiển thị đầy đủ các trường khi tạo mới biến thể cho vợt', async () => {
    const w = mount(BienTheFormModal, {
      props: {
        all: allVariants,
        sanPhams,
        options: optionsFn,
        laVotFn,
        idKadFn,
      },
      attachTo: document.body,
    })
    await flushPromises()

    expect(body().find('#bt-san-pham').exists()).toBe(true)
    expect(body().find('#bt-mau').exists()).toBe(true)
    expect(body().find('#bt-tl').exists()).toBe(true)
    expect(body().find('#bt-cv').exists()).toBe(true)
    expect(body().find('#bt-ma').exists()).toBe(true)
    expect(body().find('#bt-gia').exists()).toBe(true)
    expect(body().find('#bt-ton').exists()).toBe(true)
    w.unmount()
  })

  it('ẩn trọng lượng và chu vi khi chọn sản phẩm không phải vợt', async () => {
    const w = mount(BienTheFormModal, {
      props: {
        all: allVariants,
        sanPhams,
        options: optionsFn,
        laVotFn,
        idKadFn,
      },
      attachTo: document.body,
    })
    await flushPromises()

    await datSelect2(body(), '#bt-san-pham', 2)
    await flushPromises()

    expect(body().find('#bt-tl').exists()).toBe(false)
    expect(body().find('#bt-cv').exists()).toBe(false)
    w.unmount()
  })

  it('báo lỗi khi bấm Lưu mà chưa nhập gì', async () => {
    const w = mount(BienTheFormModal, {
      props: {
        all: allVariants,
        sanPhams,
        options: optionsFn,
        laVotFn,
        idKadFn,
      },
      attachTo: document.body,
    })
    await flushPromises()

    await body().find('form').trigger('submit')
    await flushPromises()

    expect(body().text()).toContain('Chọn sản phẩm.')
    expect(body().text()).toContain('Chọn màu sắc.')
    expect(body().text()).toContain('Nhập giá bán.')
    expect(w.emitted('save')).toBeFalsy()
    w.unmount()
  })

  it('touch tracking: blur ô nào thì hiện lỗi ngay ô đó, sửa đúng thì lỗi biến mất', async () => {
    const w = mount(BienTheFormModal, {
      props: {
        all: allVariants,
        sanPhams,
        options: optionsFn,
        laVotFn,
        idKadFn,
      },
      attachTo: document.body,
    })
    await flushPromises()

    // Chưa submit và chưa blur: không có lỗi
    expect(body().find('.ad-error').exists()).toBe(false)

    // Blur ô mã khi mã rỗng
    const maInput = body().find('#bt-ma')
    await maInput.setValue('')
    await maInput.trigger('blur')
    await flushPromises()

    expect(maInput.classes()).toContain('is-invalid')
    expect(body().text()).toContain('Nhập mã biến thể.')

    // Nhập mã đúng định dạng -> lỗi biến mất ngay
    await maInput.setValue('SP01-MOI')
    await flushPromises()
    expect(maInput.classes()).not.toContain('is-invalid')
    expect(body().text()).not.toContain('Nhập mã biến thể.')

    // Blur ô tồn kho với số âm
    const tonInput = body().find('#bt-ton')
    await tonInput.setValue(-5)
    await tonInput.trigger('blur')
    await flushPromises()
    expect(tonInput.classes()).toContain('is-invalid')
    expect(body().text()).toContain('Số lượng tồn là số nguyên từ 0 trở lên.')

    // Sửa tồn kho hợp lệ -> lỗi biến mất
    await tonInput.setValue(10)
    await flushPromises()
    expect(tonInput.classes()).not.toContain('is-invalid')
    w.unmount()
  })

  it('kiểm tra chặn sản phẩm ngưng hoạt động', async () => {
    const w = mount(BienTheFormModal, {
      props: {
        all: allVariants,
        sanPhams, // có SP03 ngưng hoạt động
        options: optionsFn,
        laVotFn,
        idKadFn,
      },
      attachTo: document.body,
    })
    await flushPromises()

    w.vm.form.idSanPham = 3
    w.vm.cham('idSanPham')
    await flushPromises()

    expect(body().text()).toContain('Sản phẩm đã ngưng hoạt động, không thể thêm biến thể mới.')
    w.unmount()
  })

  it('kiểm tra trùng tổ hợp khi sửa', async () => {
    // Sửa biến thể 101 của SP01
    const w = mount(BienTheFormModal, {
      props: {
        item: allVariants[0],
        all: allVariants,
        sanPhams,
        options: optionsFn,
        laVotFn,
        idKadFn,
      },
      attachTo: document.body,
    })
    await flushPromises()

    // Đổi sang SP02 (không phải vợt), đổi màu sắc thành 10 (trùng biến thể 102 đã có của SP02)
    w.vm.form.idSanPham = 2
    w.vm.form.idMauSac = 10
    w.vm.cham('idMauSac')
    await flushPromises()

    expect(body().text()).toContain('Sản phẩm đã có biến thể với màu sắc này.')
    w.unmount()
  })

  it('ánh xạ lỗi từ server (serverError prop) vào đúng trường giao diện', async () => {
    const w = mount(BienTheFormModal, {
      props: {
        all: allVariants,
        sanPhams,
        options: optionsFn,
        laVotFn,
        idKadFn,
        serverError: '',
      },
      attachTo: document.body,
    })
    await flushPromises()

    await w.setProps({ serverError: 'Mã biến thể đã tồn tại.' })
    await flushPromises()
    expect(body().find('#bt-ma').classes()).toContain('is-invalid')
    expect(body().text()).toContain('Mã biến thể đã tồn tại.')

    await w.setProps({ serverError: 'Giá bán tối đa 1.000.000.000 ₫.' })
    await flushPromises()
    expect(body().text()).toContain('Giá bán tối đa 1.000.000.000 ₫.')
    w.unmount()
  })
})
