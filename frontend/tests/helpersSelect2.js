// Thao tác trên ô chọn Select2 (components/common/Select2.vue) trong test: mở danh sách, đọc các mục, chọn theo nhãn.
import { flushPromises } from '@vue/test-utils'
import { expect } from 'vitest'

/** Phần tử bao quanh ô Select2 có nút bấm khớp `selector` (vd '#bt-f-sp'). */
export function khungSelect2(w, selector) {
  const nut = w.find(selector)
  expect(nut.exists(), `không thấy ô chọn ${selector}`).toBe(true)
  return nut.element.closest('.s2-wrap')
}

/** Chữ đang hiện trên nút (mục đã chọn hoặc placeholder). */
export const chuSelect2 = (w, selector) => w.find(selector).text().trim()

/** Giá trị (value) đang được chọn, dạng chữ; '' nếu chưa chọn. Thay cho select.element.value của <select> cũ. */
export const giaTriSelect2 = (w, selector) => {
  const nut = w.find(selector)
  expect(nut.exists(), `không thấy ô chọn ${selector}`).toBe(true)
  return nut.attributes('data-gia-tri') ?? ''
}

/** Mở danh sách (nếu chưa mở) và trả về nhãn các mục đang hiện (không tính dòng "Thêm nhanh"). */
export async function cacMucSelect2(w, selector) {
  const khung = khungSelect2(w, selector)
  if (!khung.querySelector('.s2-pop')) {
    w.find(selector).element.click()
    await flushPromises()
  }
  return [...khung.querySelectorAll('.s2-opt:not(.s2-create) .s2-opt-label')].map((e) => e.textContent.trim())
}

/** Gõ vào ô tìm kiếm của danh sách đang mở. */
export async function timTrongSelect2(w, selector, chu) {
  const khung = khungSelect2(w, selector)
  if (!khung.querySelector('.s2-pop')) {
    w.find(selector).element.click()
    await flushPromises()
  }
  const o = khung.querySelector('.s2-search-input')
  o.value = chu
  o.dispatchEvent(new Event('input', { bubbles: true }))
  await flushPromises()
}

/** Mở danh sách và chọn mục có nhãn đúng bằng `nhan` (không có thì lỗi, liệt kê các mục có). */
export async function chonSelect2(w, selector, nhan) {
  const khung = khungSelect2(w, selector)
  if (!khung.querySelector('.s2-pop')) {
    w.find(selector).element.click()
    await flushPromises()
  }
  const cacMuc = [...khung.querySelectorAll('.s2-opt:not(.s2-create)')]
  const muc = cacMuc.find((e) => e.querySelector('.s2-opt-label')?.textContent.trim() === nhan)
  expect(muc, `không có mục "${nhan}" trong ${selector}; hiện có: ${cacMuc.map((e) => e.textContent.trim()).join(' | ')}`).toBeTruthy()
  muc.click()
  await flushPromises()
}

/** Bấm dòng "Thêm nhanh" (phải gõ tên vào ô tìm kiếm trước). */
export async function themNhanhSelect2(w, selector, ten) {
  await timTrongSelect2(w, selector, ten)
  const dong = khungSelect2(w, selector).querySelector('[data-testid=select2-them-nhanh]')
  expect(dong, `không có dòng Thêm nhanh cho "${ten}" trong ${selector}`).toBeTruthy()
  dong.click()
  await flushPromises()
}

/** Chọn mục theo GIÁ TRỊ (value) thay cho select.setValue(value) của <select> cũ; giá trị so sánh dạng chữ ("1" = 1). */
export async function datSelect2(w, selector, giaTri) {
  const khung = khungSelect2(w, selector)
  if (!khung.querySelector('.s2-pop')) {
    w.find(selector).element.click()
    await flushPromises()
  }
  const cacMuc = [...khung.querySelectorAll('.s2-opt:not(.s2-create)')]
  const muc = cacMuc.find((e) => e.getAttribute('data-value') === String(giaTri))
  expect(muc, `không có mục value=${giaTri} trong ${selector}; hiện có: ${cacMuc.map((e) => `${e.getAttribute('data-value')}=${e.textContent.trim()}`).join(' | ')}`).toBeTruthy()
  muc.click()
  await flushPromises()
}
