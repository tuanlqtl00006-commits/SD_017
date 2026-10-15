// Ô chọn ngày dùng chung (DateInput): luôn hiện ngày/tháng/năm (13/10/2026) bất kể ngôn ngữ trình duyệt,
// v-model vẫn là 'yyyy-mm-dd'. Gõ tay tự chèn '/', lịch tiếng Việt, min / max, Hôm nay, Xóa.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import DateInput from '../src/components/common/DateInput.vue'
import { dinhDangKhiGo, isoSangVn, oLich, vnSangIso } from '../src/utils/ngay'

afterEach(() => {
  vi.useRealTimers()
  document.body.innerHTML = ''
})

describe('Chuyển đổi ngày', () => {
  it('yyyy-mm-dd <-> dd/mm/yyyy', () => {
    expect(isoSangVn('2026-10-13')).toBe('13/10/2026')
    expect(isoSangVn('')).toBe('')
    expect(isoSangVn(null)).toBe('')
    expect(isoSangVn('13/10/2026')).toBe('')
    expect(isoSangVn('2004-04-12T00:00:00')).toBe('12/04/2004') // API trả kèm giờ vẫn hiện được
    expect(isoSangVn('2026-10-131')).toBe('')
    expect(vnSangIso('13/10/2026')).toBe('2026-10-13')
    expect(vnSangIso('1/2/2026')).toBe('2026-02-01')
    expect(vnSangIso('13-10-2026')).toBe('2026-10-13')
    expect(vnSangIso('2026-10-13')).toBe('2026-10-13')
    expect(vnSangIso('')).toBe('')
  })

  it('ngày không có thật hoặc gõ chưa xong thì không nhận', () => {
    expect(vnSangIso('31/02/2026')).toBeNull()
    expect(vnSangIso('29/02/2026')).toBeNull()
    expect(vnSangIso('29/02/2028')).toBe('2028-02-29') // năm nhuận
    expect(vnSangIso('13/13/2026')).toBeNull()
    expect(vnSangIso('00/10/2026')).toBeNull()
    expect(vnSangIso('13/10/26')).toBeNull()
    expect(vnSangIso('13/10')).toBeNull()
    expect(vnSangIso('abc')).toBeNull()
  })

  it('định dạng dần khi gõ', () => {
    expect(dinhDangKhiGo('1')).toBe('1')
    expect(dinhDangKhiGo('13')).toBe('13')
    expect(dinhDangKhiGo('131')).toBe('13/1')
    expect(dinhDangKhiGo('1310')).toBe('13/10')
    expect(dinhDangKhiGo('13102026')).toBe('13/10/2026')
    expect(dinhDangKhiGo('131020269999')).toBe('13/10/2026')
    expect(dinhDangKhiGo('13/')).toBe('13/')
    expect(dinhDangKhiGo('1/')).toBe('01/')
    expect(dinhDangKhiGo('1/2/2026')).toBe('01/02/2026')
    expect(dinhDangKhiGo('13/10/2026')).toBe('13/10/2026')
    expect(dinhDangKhiGo('13/102')).toBe('13/10/2')
    expect(dinhDangKhiGo('2026-10-13')).toBe('13/10/2026')
    expect(dinhDangKhiGo('ab12')).toBe('12')
    expect(dinhDangKhiGo('')).toBe('')
  })

  it('lưới lịch 6 tuần bắt đầu thứ Hai', () => {
    const o = oLich(2026, 9) // tháng 10/2026: ngày 1 là thứ Năm
    expect(o).toHaveLength(42)
    expect(o[0]).toMatchObject({ iso: '2026-09-28', trongThang: false })
    expect(o[3]).toMatchObject({ iso: '2026-10-01', ngay: 1, trongThang: true })
    expect(o.filter((x) => x.trongThang)).toHaveLength(31)
  })
})

function gan(props = {}, attrs = {}) {
  return mount(DateInput, { props: { modelValue: '', ...props }, attrs, attachTo: document.body })
}
const daPhat = (w) => (w.emitted('update:modelValue') || []).map((x) => x[0])

describe('Ô chọn ngày', () => {
  it('hiện dd/mm/yyyy, id / class / aria đặt lên ô nhập', () => {
    const w = gan({ modelValue: '2026-10-13', id: 'hd-tu-ngay' }, { class: 'form-control ad-control', 'aria-invalid': 'true' })
    const o = w.get('input')
    expect(o.element.value).toBe('13/10/2026')
    expect(o.attributes('id')).toBe('hd-tu-ngay')
    expect(o.attributes('type')).toBe('text')
    expect(o.classes()).toEqual(expect.arrayContaining(['form-control', 'ad-control', 'di-input']))
    expect(o.attributes('aria-invalid')).toBe('true')
    expect(o.attributes('placeholder')).toBe('dd/mm/yyyy')
    w.unmount()
  })

  it('gõ đủ ngày mới đổi giá trị; gõ dở rồi rời ô thì trả lại ngày cũ', async () => {
    const w = gan({ modelValue: '2026-10-13' })
    const o = w.get('input')
    await o.setValue('0110')
    expect(o.element.value).toBe('01/10')
    expect(daPhat(w)).toEqual([])
    await o.setValue('01102026')
    expect(o.element.value).toBe('01/10/2026')
    expect(daPhat(w)).toEqual(['2026-10-01'])
    await w.setProps({ modelValue: '2026-10-01' })
    await o.setValue('31/02/2026') // không có ngày 31/02
    expect(daPhat(w)).toEqual(['2026-10-01'])
    await o.trigger('blur')
    expect(o.element.value).toBe('01/10/2026')
    w.unmount()
  })

  it('xóa trống thì giá trị là rỗng; dán yyyy-mm-dd cũng nhận', async () => {
    const w = gan({ modelValue: '2026-10-13' })
    await w.get('input').setValue('')
    expect(daPhat(w)).toEqual([''])
    await w.get('input').setValue('2026-12-25')
    expect(w.get('input').element.value).toBe('25/12/2026')
    expect(daPhat(w).at(-1)).toBe('2026-12-25')
    w.unmount()
  })

  it('giá trị đổi từ ngoài (vd Đặt lại bộ lọc) thì ô hiện theo', async () => {
    const w = gan({ modelValue: '2026-10-13' })
    await w.setProps({ modelValue: '2026-01-05' })
    expect(w.get('input').element.value).toBe('05/01/2026')
    await w.setProps({ modelValue: '' })
    expect(w.get('input').element.value).toBe('')
    w.unmount()
  })

  it('bấm vào ô mở lịch tiếng Việt: tháng / năm đang chọn, thứ Hai đầu tuần, ngày đang chọn được tô', async () => {
    const w = gan({ modelValue: '2026-10-13' })
    await w.get('input').trigger('click')
    const lich = w.get('[data-testid=lich-chon-ngay]')
    const [thang, nam] = lich.findAll('select')
    expect(thang.element.value).toBe('9')
    expect(thang.find('option:checked').text()).toBe('Tháng 10')
    expect(nam.element.value).toBe('2026')
    expect(lich.findAll('.di-week span').map((s) => s.text())).toEqual(['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'])
    expect(lich.get('.di-day.is-chon').attributes('data-iso')).toBe('2026-10-13')
    expect(lich.text()).toContain('Hôm nay')
    expect(lich.text()).toContain('Xóa')
    w.unmount()
  })

  it('chọn một ngày trên lịch thì đổi giá trị, hiện dd/mm/yyyy và đóng lịch', async () => {
    const w = gan({ modelValue: '2026-10-13' })
    await w.get('.di-btn').trigger('click')
    await w.get('[data-iso="2026-10-20"]').trigger('click')
    expect(daPhat(w)).toEqual(['2026-10-20'])
    expect(w.get('input').element.value).toBe('20/10/2026')
    expect(w.find('[data-testid=lich-chon-ngay]').exists()).toBe(false)
    w.unmount()
  })

  it('chuyển tháng bằng nút và bằng ô chọn năm', async () => {
    const w = gan({ modelValue: '2026-01-15' })
    await w.get('input').trigger('click')
    await w.get('button[aria-label="Tháng trước"]').trigger('click')
    const [thang, nam] = w.findAll('.di-sel')
    expect(thang.element.value).toBe('11')
    expect(nam.element.value).toBe('2025')
    await w.get('button[aria-label="Tháng sau"]').trigger('click')
    await w.get('button[aria-label="Tháng sau"]').trigger('click')
    expect(w.findAll('.di-sel')[0].element.value).toBe('1')
    await nam.setValue('1999')
    expect(w.find('[data-iso="1999-02-01"]').exists()).toBe(true)
    w.unmount()
  })

  it('ngày ngoài min / max bị khóa (giống Từ ngày không được sau Đến ngày)', async () => {
    const w = gan({ modelValue: '2026-10-13', min: '2026-10-10', max: '2026-10-20' })
    await w.get('input').trigger('click')
    expect(w.get('[data-iso="2026-10-09"]').attributes('disabled')).toBeDefined()
    expect(w.get('[data-iso="2026-10-21"]').attributes('disabled')).toBeDefined()
    expect(w.get('[data-iso="2026-10-10"]').attributes('disabled')).toBeUndefined()
    await w.get('[data-iso="2026-10-21"]').trigger('click')
    expect(daPhat(w)).toEqual([])
    w.unmount()
  })

  it('Hôm nay lấy ngày hiện tại theo giờ máy; Xóa làm trống', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 13, 21, 30))
    const w = gan({ modelValue: '' })
    await w.get('input').trigger('click')
    expect(w.findAll('.di-sel')[0].element.value).toBe('9') // ô trống: mở ở tháng hiện tại
    expect(w.get('.di-day.is-homnay').attributes('data-iso')).toBe('2026-10-13')
    await w.findAll('.di-link').find((b) => b.text() === 'Hôm nay').trigger('click')
    expect(daPhat(w)).toEqual(['2026-10-13'])
    expect(w.get('input').element.value).toBe('13/10/2026')
    await w.setProps({ modelValue: '2026-10-13' })
    await w.get('input').trigger('click')
    await w.findAll('.di-link').find((b) => b.text() === 'Xóa').trigger('click')
    expect(daPhat(w).at(-1)).toBe('')
    expect(w.get('input').element.value).toBe('')
    w.unmount()
  })

  it('Hôm nay bị khóa khi hôm nay nằm ngoài khoảng cho phép (vd ngày sinh có max)', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 13, 9, 0))
    const w = gan({ modelValue: '', max: '2008-10-13' })
    await w.get('input').trigger('click')
    expect(w.findAll('.di-link').find((b) => b.text() === 'Hôm nay').attributes('disabled')).toBeDefined()
    expect(w.findAll('.di-sel')[1].element.value).toBe('2008') // mở ở ngày gần nhất được phép
    w.unmount()
  })

  it('bàn phím: mũi tên xuống mở lịch, mũi tên di chuyển ngày, Enter chọn, Esc đóng', async () => {
    const w = gan({ modelValue: '2026-10-31' })
    await w.get('input').trigger('keydown', { key: 'ArrowDown' })
    await nextTick(); await nextTick()
    expect(document.activeElement?.getAttribute('data-iso')).toBe('2026-10-31')
    await w.get('.di-grid').trigger('keydown', { key: 'ArrowRight' }) // sang tháng 11
    await nextTick(); await nextTick()
    expect(w.findAll('.di-sel')[0].element.value).toBe('10')
    expect(document.activeElement?.getAttribute('data-iso')).toBe('2026-11-01')
    await w.get('.di-grid').trigger('keydown', { key: 'ArrowUp' })
    await nextTick(); await nextTick()
    expect(document.activeElement?.getAttribute('data-iso')).toBe('2026-10-25')
    document.activeElement.click(); await nextTick()
    expect(daPhat(w)).toEqual(['2026-10-25'])
    await w.get('input').trigger('click')
    await w.get('[data-testid=lich-chon-ngay]').trigger('keydown', { key: 'Escape' })
    expect(w.find('[data-testid=lich-chon-ngay]').exists()).toBe(false)
    w.unmount()
  })

  it('bấm ra ngoài thì đóng lịch', async () => {
    const w = gan({ modelValue: '2026-10-13' })
    await w.get('input').trigger('click')
    expect(w.find('[data-testid=lich-chon-ngay]').exists()).toBe(true)
    document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
    await nextTick()
    expect(w.find('[data-testid=lich-chon-ngay]').exists()).toBe(false)
    w.unmount()
  })

  it('ô bị khóa / chỉ đọc (vd Ngày vào làm khi sửa nhân viên) thì không mở lịch', async () => {
    const w = gan({ modelValue: '2024-03-01', readonly: true })
    await w.get('input').trigger('click')
    await w.get('input').trigger('keydown', { key: 'ArrowDown' })
    expect(w.find('[data-testid=lich-chon-ngay]').exists()).toBe(false)
    expect(w.get('input').attributes('readonly')).toBeDefined()
    expect(w.get('.di-btn').attributes('disabled')).toBeDefined()
    await w.setProps({ readonly: false, disabled: true })
    await w.get('.di-btn').trigger('click')
    expect(w.find('[data-testid=lich-chon-ngay]').exists()).toBe(false)
    w.unmount()
  })
})
