// Thành phần dùng chung theo lời cô: ô chọn Select2 (tìm kiếm + "Thêm nhanh"), ô ngày giờ, phân trang rút gọn "1 2 … N".
import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import Select2 from '../src/components/common/Select2.vue'
import DateTimeInput from '../src/components/common/DateTimeInput.vue'
import BasePagination from '../src/components/common/BasePagination.vue'
import { danhSachTrang } from '../src/utils/phanTrang'
import {
  dinhDangGioKhiGo,
  dinhDangNgayGio,
  gioHopLe,
  gopNgayGio,
  ngayGioGuiApi,
  soSanhNgayGio,
  tachNgayGio,
  vnNgayGioSangIso,
} from '../src/utils/ngayGio'
import { cacMucSelect2, chonSelect2, chuSelect2, themNhanhSelect2, timTrongSelect2 } from './helpersSelect2'

afterEach(() => {
  document.body.innerHTML = ''
})

describe('Phân trang rút gọn', () => {
  it('ít trang thì hiện hết', () => {
    expect(danhSachTrang(1, 1)).toEqual([1])
    expect(danhSachTrang(2, 5)).toEqual([1, 2, 3, 4, 5])
    expect(danhSachTrang(4, 7)).toEqual([1, 2, 3, 4, 5, 6, 7])
  })

  it('nhiều trang: 1 2 … số cuối (đúng lời cô)', () => {
    expect(danhSachTrang(1, 20)).toEqual([1, 2, '…', 20])
    expect(danhSachTrang(2, 20)).toEqual([1, 2, 3, '…', 20])
    expect(danhSachTrang(3, 20)).toEqual([1, 2, 3, 4, '…', 20])
    expect(danhSachTrang(10, 20)).toEqual([1, 2, '…', 9, 10, 11, '…', 20])
    expect(danhSachTrang(18, 20)).toEqual([1, 2, '…', 17, 18, 19, 20])
    expect(danhSachTrang(20, 20)).toEqual([1, 2, '…', 19, 20])
  })

  it('chỉ thiếu đúng một trang thì hiện luôn trang đó thay vì dấu ba chấm', () => {
    expect(danhSachTrang(5, 8)).toEqual([1, 2, 3, 4, 5, 6, 7, 8])
    expect(danhSachTrang(1, 4)).toEqual([1, 2, 3, 4])
  })

  it('đầu vào lạ không làm hỏng', () => {
    expect(danhSachTrang(0, 0)).toEqual([1])
    expect(danhSachTrang(99, 3)).toEqual([1, 2, 3])
    expect(danhSachTrang(NaN, 10)).toEqual([1, 2, '…', 10])
  })

  it('BasePagination: 200 dòng / 10 = 20 trang chỉ vẽ vài nút, không còn « »', () => {
    const w = mount(BasePagination, { props: { page: 1, pageSize: 10, total: 200 } })
    const nut = w.findAll('.ad-page-btn').map((b) => b.attributes('aria-label'))
    expect(nut).toEqual(['Trang trước', 'Trang 1', 'Trang 2', 'Trang 20', 'Trang sau'])
    expect(w.find('.ad-page-gap').text()).toBe('…')
    expect(w.find('[aria-label="Trang đầu"]').exists()).toBe(false)
    expect(w.find('[aria-label="Trang cuối"]').exists()).toBe(false)
    expect(w.find('[aria-label="Trang trước"]').attributes('disabled')).toBeDefined()
  })

  it('bấm số trang / mũi tên thì phát update:page', async () => {
    const w = mount(BasePagination, { props: { page: 10, pageSize: 10, total: 200 } })
    await w.find('[aria-label="Trang 20"]').trigger('click')
    await w.find('[aria-label="Trang sau"]').trigger('click')
    await w.find('[aria-label="Trang trước"]').trigger('click')
    await w.find('[aria-label="Trang 10"]').trigger('click') // trang đang xem thì không phát
    expect(w.emitted('update:page')).toEqual([[20], [11], [9]])
    expect(w.find('[aria-current=page]').text()).toBe('10')
  })

  it('trang cuối: nút "Trang sau" khóa', () => {
    const w = mount(BasePagination, { props: { page: 3, pageSize: 10, total: 25 } })
    expect(w.find('[aria-label="Trang sau"]').attributes('disabled')).toBeDefined()
  })
})

describe('Select2', () => {
  const DS = [
    { value: 1, label: 'Yonex' },
    { value: 2, label: 'Li-Ning' },
    { value: 3, label: 'Nhật Bản' },
  ]
  function dung(props = {}, handlers = {}) {
    return mount(
      defineComponent({
        components: { Select2 },
        setup() {
          const v = ref(props.initial ?? '')
          return { v, DS, props, handlers }
        },
        template: `<Select2 id="s" v-model="v" :options="props.options ?? DS" v-bind="props.attrs" @create="handlers.create?.($event)" @change="handlers.change?.($event)" />`,
      }),
      { attachTo: document.body },
    )
  }

  it('chưa chọn thì hiện placeholder, bấm vào mở danh sách có ô tìm kiếm', async () => {
    const w = dung({ attrs: { placeholder: 'Chọn thương hiệu' } })
    expect(chuSelect2(w, '#s')).toBe('Chọn thương hiệu')
    expect(w.find('.s2-pop').exists()).toBe(false)
    expect(await cacMucSelect2(w, '#s')).toEqual(['Yonex', 'Li-Ning', 'Nhật Bản'])
    expect(w.find('.s2-search-input').exists()).toBe(true)
    expect(w.find('#s').attributes('aria-expanded')).toBe('true')
  })

  it('chọn một mục: cập nhật v-model giữ nguyên kiểu số, hiện nhãn, đóng danh sách, phát change', async () => {
    const thayDoi = []
    const w = dung({}, { change: (x) => thayDoi.push(x) })
    await chonSelect2(w, '#s', 'Li-Ning')
    expect(w.vm.v).toBe(2)
    expect(chuSelect2(w, '#s')).toBe('Li-Ning')
    expect(w.find('.s2-pop').exists()).toBe(false)
    expect(thayDoi).toEqual([2])
  })

  it('tìm kiếm không phân biệt hoa thường và dấu', async () => {
    const w = dung()
    await timTrongSelect2(w, '#s', 'nhat ban')
    expect(await cacMucSelect2(w, '#s')).toEqual(['Nhật Bản'])
    await timTrongSelect2(w, '#s', 'LI')
    expect(await cacMucSelect2(w, '#s')).toEqual(['Li-Ning'])
    await timTrongSelect2(w, '#s', 'zzz')
    expect(w.find('.s2-empty').text()).toBe('Không có kết quả')
  })

  it('bàn phím: mũi tên xuống + Enter chọn, Esc đóng', async () => {
    const w = dung()
    w.find('#s').element.click()
    await flushPromises()
    const o = w.find('.s2-search-input')
    await o.trigger('keydown', { key: 'ArrowDown' })
    await o.trigger('keydown', { key: 'Enter' })
    expect(w.vm.v).toBe(2)
    w.find('#s').element.click()
    await flushPromises()
    expect(w.find('.s2-pop').exists()).toBe(true)
    await w.find('.s2-search-input').trigger('keydown', { key: 'Escape' })
    expect(w.find('.s2-pop').exists()).toBe(false)
    expect(w.vm.v).toBe(2)
  })

  it('mục đang chọn được đánh dấu và danh sách mở ở đúng mục đó', async () => {
    const w = dung({ initial: 3 })
    expect(chuSelect2(w, '#s')).toBe('Nhật Bản')
    await cacMucSelect2(w, '#s')
    expect(w.find('.s2-opt.is-chon').text()).toContain('Nhật Bản')
    expect(w.find('.s2-opt.is-active').text()).toContain('Nhật Bản')
  })

  it('"Thêm nhanh": gõ tên chưa có thì hiện dòng thêm nhanh và phát create kèm tên', async () => {
    const tao = []
    const w = dung({ attrs: { allowCreate: true } }, { create: (t) => tao.push(t) })
    await timTrongSelect2(w, '#s', '  Victor  ')
    expect(w.find('[data-testid=select2-them-nhanh]').text()).toContain('Thêm nhanh “Victor”')
    await themNhanhSelect2(w, '#s', 'Victor')
    expect(tao).toEqual(['Victor'])
    expect(w.find('.s2-pop').exists()).toBe(false)
  })

  it('"Thêm nhanh" không hiện khi đã có mục trùng tên (kể cả khác hoa thường / dấu) hoặc chưa gõ gì', async () => {
    const w = dung({ attrs: { allowCreate: true } })
    await cacMucSelect2(w, '#s')
    expect(w.find('[data-testid=select2-them-nhanh]').exists()).toBe(false)
    await timTrongSelect2(w, '#s', 'yonex')
    expect(w.find('[data-testid=select2-them-nhanh]').exists()).toBe(false)
    await timTrongSelect2(w, '#s', 'nhat ban')
    expect(w.find('[data-testid=select2-them-nhanh]').exists()).toBe(false)
    await timTrongSelect2(w, '#s', 'Yone')
    expect(w.find('[data-testid=select2-them-nhanh]').exists()).toBe(true) // gần giống nhưng chưa có đúng tên
  })

  it('Enter khi không có mục khớp và đang bật thêm nhanh thì tạo mới', async () => {
    const tao = []
    const w = dung({ attrs: { allowCreate: true } }, { create: (t) => tao.push(t) })
    await timTrongSelect2(w, '#s', 'Babolat')
    await w.find('.s2-search-input').trigger('keydown', { key: 'Enter' })
    expect(tao).toEqual(['Babolat'])
  })

  it('không bật allowCreate thì không bao giờ có dòng thêm nhanh', async () => {
    const w = dung()
    await timTrongSelect2(w, '#s', 'Babolat')
    expect(w.find('[data-testid=select2-them-nhanh]').exists()).toBe(false)
  })

  it('có thể bỏ chọn khi clearable', async () => {
    const w = dung({ initial: 1, attrs: { clearable: true, placeholder: 'Chọn…' } })
    expect(w.find('.s2-clear').exists()).toBe(true)
    await w.find('.s2-clear').trigger('click')
    expect(w.vm.v).toBe('')
    expect(chuSelect2(w, '#s')).toBe('Chọn…')
    expect(w.find('.s2-clear').exists()).toBe(false)
  })

  it('bị khóa thì không mở được; invalid thì có viền lỗi', async () => {
    const w = dung({ attrs: { disabled: true, invalid: true } })
    expect(w.find('#s').attributes('disabled')).toBeDefined()
    expect(w.find('#s').classes()).toContain('is-invalid')
    await w.find('#s').trigger('click')
    expect(w.find('.s2-pop').exists()).toBe(false)
  })

  it('mục có value rỗng ("Tất cả") được hiện như một lựa chọn bình thường', async () => {
    const w = dung({ options: [{ value: '', label: 'Tất cả trạng thái' }, { value: 1, label: 'Hoạt động' }] })
    expect(chuSelect2(w, '#s')).toBe('Tất cả trạng thái')
    await chonSelect2(w, '#s', 'Hoạt động')
    expect(w.vm.v).toBe(1)
    await chonSelect2(w, '#s', 'Tất cả trạng thái')
    expect(w.vm.v).toBe('')
  })

  it('mục bị khóa (disabled) không chọn được', async () => {
    const w = dung({ options: [{ value: 1, label: 'A', disabled: true }, { value: 2, label: 'B' }] })
    await chonSelect2(w, '#s', 'A')
    expect(w.vm.v).toBe('')
    expect(w.find('.s2-pop').exists()).toBe(true)
  })
})

describe('Ngày giờ', () => {
  it('tách / ghép / định dạng', () => {
    expect(tachNgayGio('2026-10-15T08:30:00')).toEqual({ ngay: '2026-10-15', gio: '08:30' })
    expect(tachNgayGio('2026-10-15')).toEqual({ ngay: '2026-10-15', gio: '' })
    expect(tachNgayGio('')).toEqual({ ngay: '', gio: '' })
    expect(gopNgayGio('2026-10-15', '08:30')).toBe('2026-10-15T08:30')
    expect(gopNgayGio('2026-10-15', '', '23:59')).toBe('2026-10-15T23:59')
    expect(gopNgayGio('', '08:30')).toBe('')
    expect(dinhDangNgayGio('2026-10-15T08:30:00')).toBe('15/10/2026 08:30')
    expect(dinhDangNgayGio('2026-10-15')).toBe('15/10/2026')
    expect(dinhDangNgayGio(null)).toBe('')
  })

  it('gửi API luôn có giây; chỉ có ngày thì lấy giờ mặc định', () => {
    expect(ngayGioGuiApi('2026-10-15T08:30')).toBe('2026-10-15T08:30:00')
    expect(ngayGioGuiApi('2026-10-15T23:59:59')).toBe('2026-10-15T23:59:59')
    expect(ngayGioGuiApi('2026-10-15', '23:59')).toBe('2026-10-15T23:59:00')
    expect(ngayGioGuiApi('')).toBeNull()
  })

  it('giờ hợp lệ 00:00 – 23:59', () => {
    expect(gioHopLe('00:00')).toBe(true)
    expect(gioHopLe('23:59')).toBe(true)
    expect(gioHopLe('24:00')).toBe(false)
    expect(gioHopLe('12:60')).toBe(false)
    expect(gioHopLe('9:30')).toBe(false)
    expect(gioHopLe('')).toBe(false)
  })

  it('gõ giờ tự chèn dấu hai chấm', () => {
    expect(dinhDangGioKhiGo('1')).toBe('1')
    expect(dinhDangGioKhiGo('9')).toBe('09:')
    expect(dinhDangGioKhiGo('14')).toBe('14:')
    expect(dinhDangGioKhiGo('143')).toBe('14:3')
    expect(dinhDangGioKhiGo('1430')).toBe('14:30')
    expect(dinhDangGioKhiGo('14301')).toBe('14:30')
    expect(dinhDangGioKhiGo('14:', true)).toBe('14') // đang xóa thì không tự chèn lại dấu
    expect(dinhDangGioKhiGo('1:')).toBe('01:')
    expect(dinhDangGioKhiGo('ab')).toBe('')
  })

  it('so sánh hai mốc giờ', () => {
    expect(soSanhNgayGio('2026-10-15T08:00', '2026-10-15T08:01')).toBe(-1)
    expect(soSanhNgayGio('2026-10-15T08:00', '2026-10-15T08:00:00')).toBe(0)
    expect(soSanhNgayGio('2026-10-16T00:00', '2026-10-15T23:59')).toBe(1)
  })

  it('dán dd/mm/yyyy HH:mm', () => {
    expect(vnNgayGioSangIso('15/10/2026 8:05')).toBe('2026-10-15T08:05')
    expect(vnNgayGioSangIso('31/02/2026 08:00')).toBeNull()
    expect(vnNgayGioSangIso('15/10/2026 25:00')).toBeNull()
    expect(vnNgayGioSangIso('15/10/2026')).toBeNull()
  })

  function dung(init = '', props = {}) {
    return mount(
      defineComponent({
        components: { DateTimeInput },
        setup() {
          const v = ref(init)
          return { v, props }
        },
        template: `<DateTimeInput id="t" v-model="v" v-bind="props" />`,
      }),
      { attachTo: document.body },
    )
  }
  const gonNgay = async (w, chu) => {
    const o = w.find('#t')
    o.element.value = chu
    await o.trigger('input')
  }
  const gonGio = async (w, chu, inputType = 'insertText') => {
    const o = w.find('[data-testid=o-gio]')
    o.element.value = chu
    o.element.dispatchEvent(new InputEvent('input', { bubbles: true, inputType }))
    await flushPromises()
  }

  it('hiện ngày và giờ đang có', () => {
    const w = dung('2026-10-15T08:30')
    expect(w.find('#t').element.value).toBe('15/10/2026')
    expect(w.find('[data-testid=o-gio]').element.value).toBe('08:30')
  })

  it('chọn ngày mà chưa có giờ thì lấy giờ mặc định (bắt đầu 00:00, kết thúc 23:59)', async () => {
    const bd = dung('', { defaultTime: '00:00' })
    await gonNgay(bd, '16/10/2026')
    expect(bd.vm.v).toBe('2026-10-16T00:00')
    expect(bd.find('[data-testid=o-gio]').element.value).toBe('00:00')
    const kt = dung('', { defaultTime: '23:59' })
    await gonNgay(kt, '16/10/2026')
    expect(kt.vm.v).toBe('2026-10-16T23:59')
  })

  it('gõ giờ hợp lệ thì cập nhật, gõ dở thì chưa đổi và trả lại khi rời ô', async () => {
    const w = dung('2026-10-15T08:30')
    await gonGio(w, '1745')
    expect(w.vm.v).toBe('2026-10-15T17:45')
    expect(w.find('[data-testid=o-gio]').element.value).toBe('17:45')
    await gonGio(w, '9')
    expect(w.vm.v).toBe('2026-10-15T17:45') // chưa gõ xong
    await w.find('[data-testid=o-gio]').trigger('blur')
    expect(w.find('[data-testid=o-gio]').element.value).toBe('17:45')
    await gonGio(w, '2575') // 25:75 không có thật
    expect(w.vm.v).toBe('2026-10-15T17:45')
  })

  it('xóa ngày thì xóa cả giờ; đổi ngày giữ nguyên giờ đã chọn', async () => {
    const w = dung('2026-10-15T08:30')
    await gonNgay(w, '20/10/2026')
    expect(w.vm.v).toBe('2026-10-20T08:30')
    await gonNgay(w, '')
    expect(w.vm.v).toBe('')
    expect(w.find('[data-testid=o-gio]').element.value).toBe('')
  })

  it('nút "Bây giờ" trong bảng chọn giờ đặt đúng giờ máy (làm tròn tới phút), không hiện nút Bây giờ bên ngoài', async () => {
    const w = dung('')
    expect(w.find('[data-testid=nut-bay-gio]').exists()).toBe(false)
    await w.find('[data-testid=o-gio]').trigger('click')
    await w.find('[data-testid=nut-bay-gio]').trigger('click')
    expect(w.vm.v).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/)
    const gio = new Date()
    expect(w.vm.v.slice(0, 10)).toBe(`${gio.getFullYear()}-${String(gio.getMonth() + 1).padStart(2, '0')}-${String(gio.getDate()).padStart(2, '0')}`)
  })

  it('khóa thì không mở được bảng chọn giờ và hai ô không sửa được', async () => {
    const w = dung('2026-10-15T08:30', { disabled: true })
    await w.find('[data-testid=o-gio]').trigger('click')
    expect(w.find('[data-testid=bang-chon-gio]').exists()).toBe(false)
    expect(w.find('[data-testid=o-gio]').attributes('disabled')).toBeDefined()
    expect(w.find('#t').attributes('disabled')).toBeDefined()
  })

  it('bấm vào ô giờ mở bảng chọn Giờ : Phút tự động, chọn giờ / phút / mốc nhanh cập nhật ngay', async () => {
    const w = dung('2026-10-15T08:30')
    expect(w.find('[data-testid=bang-chon-gio]').exists()).toBe(false)
    await w.find('[data-testid=o-gio]').trigger('click')
    const bang = w.get('[data-testid=bang-chon-gio]')
    expect(bang.get('[data-gio="08"]').classes()).toContain('is-chon')
    expect(bang.get('[data-phut="30"]').classes()).toContain('is-chon')
    await bang.get('[data-gio="14"]').trigger('click')
    expect(w.vm.v).toBe('2026-10-15T14:30')
    await bang.get('[data-phut="45"]').trigger('click')
    expect(w.vm.v).toBe('2026-10-15T14:45')
    await bang.get('[data-moc="23:59"]').trigger('click')
    expect(w.vm.v).toBe('2026-10-15T23:59')
    expect(w.find('[data-testid=bang-chon-gio]').exists()).toBe(false)
    w.unmount()
  })
})
