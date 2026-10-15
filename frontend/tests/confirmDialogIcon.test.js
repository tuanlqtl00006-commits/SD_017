import { beforeEach, describe, expect, it } from 'vitest'
import { DOMWrapper, mount } from '@vue/test-utils'
import ConfirmDialog from '../src/components/common/ConfirmDialog.vue'

const body = () => new DOMWrapper(document.body)
const mo = (props = {}) =>
  mount(ConfirmDialog, { attachTo: document.body, props: { title: 'Ngừng hoạt động?', message: 'Bạn chắc chứ?', confirmText: 'Đồng ý', ...props } })

beforeEach(() => {
  document.body.innerHTML = ''
})

describe('ConfirmDialog: giao diện cũ giữ nguyên khi không truyền icon', () => {
  it('có tiêu đề ở header, nội dung, nút Hủy / xác nhận; cỡ nhỏ; không có icon', () => {
    const w = mo()
    expect(body().find('.ad-modal-title').text()).toBe('Ngừng hoạt động?')
    expect(body().find('.ad-confirm-text').text()).toBe('Bạn chắc chứ?')
    expect(body().find('.modal-dialog').classes()).toContain('modal-sm')
    expect(body().find('.cf-dialog').exists()).toBe(false)
    expect(body().find('.cf-icon').exists()).toBe(false)
    const nut = body().findAll('.ad-modal-footer button').map((b) => b.text())
    expect(nut).toEqual(['Hủy', 'Đồng ý'])
    w.unmount()
  })
})

describe('ConfirmDialog: giao diện căn giữa khi truyền icon', () => {
  it.each([
    ['warning', 'cf-icon-warning'],
    ['success', 'cf-icon-success'],
  ])('icon %s: có biểu tượng tròn, tiêu đề + nội dung trong thân, cỡ vừa', (icon, lop) => {
    const w = mo({ icon })
    expect(body().find('.cf-dialog').exists()).toBe(true)
    expect(body().find(`.cf-icon.${lop}`).exists()).toBe(true)
    expect(body().find('.cf-title').text()).toBe('Ngừng hoạt động?')
    expect(body().find('.cf-text').text()).toBe('Bạn chắc chứ?')
    expect(body().find('.modal-dialog').classes()).not.toContain('modal-sm')
    expect(body().find('.ad-modal-title').exists()).toBe(false) // tiêu đề không lặp ở header
    w.unmount()
  })

  it('hộp thoại vẫn có tên cho trình đọc màn hình (aria-labelledby trỏ tới tiêu đề ẩn)', () => {
    const w = mo({ icon: 'warning' })
    const goc = body().find('[role=dialog]')
    const id = goc.attributes('aria-labelledby')
    expect(id).toBeTruthy()
    const ten = document.getElementById(id)
    expect(ten).not.toBeNull()
    expect(ten.textContent).toBe('Ngừng hoạt động?')
    w.unmount()
  })

  it('nút xác nhận / Hủy / đóng phát đúng sự kiện; đang tải thì khóa hai nút', async () => {
    const w = mo({ icon: 'success', variant: 'danger' })
    const nut = body().findAll('.ad-modal-footer button')
    expect(nut[1].classes()).toContain('ad-btn-danger')
    await nut[1].trigger('click')
    await nut[0].trigger('click')
    await body().find('.btn-close').trigger('click')
    expect(w.emitted('confirm')).toHaveLength(1)
    expect(w.emitted('cancel')).toHaveLength(2)
    await w.setProps({ loading: true })
    for (const b of body().findAll('.ad-modal-footer button')) expect(b.attributes('disabled')).toBeDefined()
    w.unmount()
  })
})
