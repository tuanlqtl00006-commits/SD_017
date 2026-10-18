import { beforeEach, describe, it, expect } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import ThuocTinhFormModal from '../src/components/thuocTinh/ThuocTinhFormModal.vue'
import { THUOC_TINH } from '../src/constants/thuocTinh'

const body = () => new DOMWrapper(document.body)

beforeEach(() => {
  document.body.innerHTML = ''
})

describe('ThuocTinhFormModal - Kiểm tra dữ liệu và touch tracking', () => {
  const allCategories = [
    { id: 1, ma: 'DM01', ten: 'Vợt cầu lông', hoatDong: true },
    { id: 2, ma: 'DM02', ten: 'Quần áo thể thao', hoatDong: true },
    { id: 3, ma: 'DM03', ten: 'Túi - balo', hoatDong: true },
  ]

  it('hiển thị mã tự động và ô nhập tên danh mục', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        all: allCategories,
      },
      attachTo: document.body,
    })
    await flushPromises()

    expect(body().find('#tt-ma').element.value).toBe('DM04')
    expect(body().find('#tt-ten').exists()).toBe(true)
    w.unmount()
  })

  it('báo lỗi khi submit tên rỗng', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        all: allCategories,
      },
      attachTo: document.body,
    })
    await flushPromises()

    await body().find('form').trigger('submit')
    await flushPromises()

    expect(body().text()).toContain('Nhập tên danh mục.')
    expect(w.emitted('save')).toBeFalsy()
    w.unmount()
  })

  it('touch tracking: blur ô tên khi rỗng thì hiện lỗi ngay, nhập tên hợp lệ thì biến mất', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        all: allCategories,
      },
      attachTo: document.body,
    })
    await flushPromises()

    expect(body().find('.ad-error').exists()).toBe(false)

    const input = body().find('#tt-ten')
    await input.trigger('blur')
    await flushPromises()

    expect(input.classes()).toContain('is-invalid')
    expect(body().text()).toContain('Nhập tên danh mục.')

    await input.setValue('Giày cầu lông')
    await flushPromises()

    expect(input.classes()).not.toContain('is-invalid')
    expect(body().text()).not.toContain('Nhập tên danh mục.')
    w.unmount()
  })

  it('báo lỗi khi tên vượt quá 100 ký tự', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        all: allCategories,
      },
      attachTo: document.body,
    })
    await flushPromises()

    const input = body().find('#tt-ten')
    await input.setValue('a'.repeat(101))
    await input.trigger('blur')
    await flushPromises()

    expect(body().text()).toContain('Tên tối đa 100 ký tự.')
    w.unmount()
  })

  it('báo lỗi trùng tên (không phân biệt hoa thường)', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        all: allCategories,
      },
      attachTo: document.body,
    })
    await flushPromises()

    const input = body().find('#tt-ten')
    await input.setValue('  vợt CẦU LÔNG  ')
    await input.trigger('blur')
    await flushPromises()

    expect(body().text()).toContain('Tên danh mục đã tồn tại.')
    w.unmount()
  })

  it('khi sửa chính nó thì cho phép giữ nguyên tên', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        item: allCategories[0],
        all: allCategories,
      },
      attachTo: document.body,
    })
    await flushPromises()

    await body().find('form').trigger('submit')
    await flushPromises()

    expect(w.emitted('save')).toBeTruthy()
    expect(w.emitted('save')[0][0]).toEqual({ ten: 'Vợt cầu lông' })
    w.unmount()
  })

  it('hiển thị serverError truyền từ component cha', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['danh-muc'],
        item: allCategories[0],
        all: allCategories,
        serverError: '',
      },
      attachTo: document.body,
    })
    await flushPromises()

    await w.setProps({
      serverError: 'Danh mục đang có sản phẩm, không thể đổi tên làm danh mục chuyển giữa loại vợt và loại khác.',
    })
    await flushPromises()

    expect(body().find('#tt-ten').classes()).toContain('is-invalid')
    expect(body().text()).toContain('Danh mục đang có sản phẩm, không thể đổi tên làm danh mục chuyển giữa loại vợt và loại khác.')
    w.unmount()
  })

  it('chất liệu: bắt buộc chọn ít nhất một danh mục áp dụng', async () => {
    const w = mount(ThuocTinhFormModal, {
      props: {
        cfg: THUOC_TINH['chat-lieu'],
        all: [],
      },
      attachTo: document.body,
    })
    await flushPromises()

    await body().find('#tt-ten').setValue('Titanium')
    await body().find('form').trigger('submit')
    await flushPromises()

    expect(body().text()).toContain('Chọn ít nhất một danh mục sử dụng chất liệu này.')
    expect(w.emitted('save')).toBeFalsy()
    w.unmount()
  })
})
