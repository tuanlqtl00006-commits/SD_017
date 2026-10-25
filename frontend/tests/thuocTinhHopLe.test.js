import { beforeEach, describe, expect, it } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import ThuocTinhFormModal from '../src/components/thuocTinh/ThuocTinhFormModal.vue'
import { THUOC_TINH } from '../src/constants/thuocTinh'
import { chuanHoaThuocTinh, loiThuocTinh } from '../src/utils/thuocTinhHopLe'

const body = () => new DOMWrapper(document.body)

beforeEach(() => {
  document.body.innerHTML = ''
})

describe('Giá trị thuộc tính đúng chuẩn (cô: không nhập size / màu linh tinh)', () => {
  it('màu sắc chỉ gồm chữ', () => {
    expect(loiThuocTinh('mau-sac', 'Xanh dương')).toBe('')
    expect(loiThuocTinh('mau-sac', 'Trắng - Đỏ')).toBe('')
    expect(loiThuocTinh('mau-sac', 'Hồng 45')).toContain('chỉ gồm chữ')
    expect(loiThuocTinh('mau-sac', 'Đỏ!!')).not.toBe('')
  })

  it('trọng lượng chuẩn U và khoảng gram đúng chuẩn; chu vi G1 - G6', () => {
    expect(chuanHoaThuocTinh('trong-luong', ' 4u ( 80 - 84 g ) ')).toBe('4U (80-84g)')
    expect(loiThuocTinh('trong-luong', '3U')).toBe('')
    expect(loiThuocTinh('trong-luong', '4U (85-89g)')).toContain('4U (80-84g)')
    expect(loiThuocTinh('trong-luong', '9U')).not.toBe('')
    expect(loiThuocTinh('trong-luong', 'Không áp dụng')).toBe('')
    expect(chuanHoaThuocTinh('chu-vi', 'g5')).toBe('G5')
    expect(loiThuocTinh('chu-vi', 'G7')).not.toBe('')
    expect(loiThuocTinh('chu-vi', 'XL')).not.toBe('')
  })

  it('các bảng khác: phải có chữ, không ký tự lạ', () => {
    expect(loiThuocTinh('thuong-hieu', 'Li-Ning')).toBe('')
    expect(loiThuocTinh('chat-lieu', 'Polyester 100%')).toBe('')
    expect(loiThuocTinh('thuong-hieu', '12345')).toContain('phải có chữ')
    expect(loiThuocTinh('xuat-xu', 'VN-01')).not.toBe('')
    expect(loiThuocTinh('do-cung', 'Cứng 2')).not.toBe('')
  })

  it('form báo lỗi ngay khi nhập màu có số, gửi tên đã chuẩn hóa cho trọng lượng', async () => {
    const w = mount(ThuocTinhFormModal, { props: { cfg: THUOC_TINH['mau-sac'], all: [] }, attachTo: document.body })
    await body().find('#tt-ten').setValue('Hồng 45')
    await body().find('#tt-form').trigger('submit')
    await flushPromises()
    expect(body().text()).toContain('Tên màu chỉ gồm chữ')
    expect(w.emitted('save')).toBeUndefined()
    w.unmount()

    const w2 = mount(ThuocTinhFormModal, { props: { cfg: THUOC_TINH['trong-luong'], all: [] }, attachTo: document.body })
    await body().find('#tt-ten').setValue('5u (75-79 g)')
    await body().find('#tt-form').trigger('submit')
    await flushPromises()
    expect(w2.emitted('save')[0][0]).toEqual({ ten: '5U (75-79g)' })
    w2.unmount()
  })
})
