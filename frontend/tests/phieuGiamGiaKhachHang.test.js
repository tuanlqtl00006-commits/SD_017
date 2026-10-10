import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// Bảng "Danh sách khách hàng nhận phiếu" trong form phiếu giảm giá cá nhân
const KHACH = [
  { id: 1, ma: 'KH001', hoTen: 'Nguyễn Thu Thảo', soDienThoai: '0912345671', email: 'thao@gmail.com', ngaySinh: '2000-01-15', soDonDaMua: 68, lanMuaGanNhat: '2026-08-12T17:07:30', daDung: false },
  { id: 2, ma: 'KH002', hoTen: 'Lê Hoài An', soDienThoai: '0912345672', email: 'an@gmail.com', ngaySinh: '1998-05-20', soDonDaMua: 0, lanMuaGanNhat: null, daDung: false },
  { id: 3, ma: 'KH003', hoTen: 'Trịnh Xuân Bách', soDienThoai: '0934633210', email: 'bach@gmail.com', ngaySinh: null, soDonDaMua: 0, lanMuaGanNhat: null, daDung: false },
]
vi.mock('../src/services/api', () => ({
  default: { get: async (url) => ({ data: url === '/phieu-giam-gia/khach-hang' ? KHACH : [] }) },
}))
beforeEach(() => { document.body.innerHTML = '' })

async function moForm() {
  const Comp = (await import('../src/components/phieuGiamGia/PhieuGiamGiaFormModal.vue')).default
  const item = {
    id: 9, ma: 'VCH836504', ten: 'Ưu đãi', hinhThuc: 'CA_NHAN', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000,
    donToiThieu: 500000, soLuong: 1, soLuongDaDung: 0, ngayBatDau: '2026-10-12', ngayKetThuc: '2026-10-15',
    khachHangs: [{ id: 2, daDung: false }],
  }
  const w = mount(Comp, { props: { item, all: [] }, attachTo: document.body })
  await flushPromises()
  return w
}
const bang = () => document.querySelector('.pgg-kh-table')
const dongKhach = () => [...bang().querySelectorAll('tbody tr')]

describe('Bảng khách hàng nhận phiếu cá nhân', () => {
  it('có đủ các cột như thiết kế', async () => {
    await moForm()
    const cot = [...bang().querySelectorAll('thead th')].map((th) => th.textContent.trim()).filter(Boolean)
    expect(cot).toEqual(['Mã KH', 'Tên khách hàng', 'Ngày sinh', 'Số điện thoại', 'Email', 'Đã mua', 'Gần nhất'])
  })

  it('hiện ngày sinh, số đơn và lần mua gần nhất đúng định dạng', async () => {
    await moForm()
    const thao = dongKhach()[0].textContent
    expect(thao).toContain('KH001')
    expect(thao).toContain('15/01/2000')
    expect(thao).toContain('68 đơn')
    expect(thao).toContain('12/08/2026 17:07')
    const bach = dongKhach()[2].textContent
    expect(bach).toContain('0 đơn')
    expect(bach).toContain('---')
  })

  it('đếm "Đã chọn", bấm vào dòng để chọn, ô tiêu đề chọn / bỏ chọn tất cả', async () => {
    await moForm()
    const badge = () => document.querySelector('.pgg-kh-badge').textContent
    expect(badge()).toContain('Đã chọn 1')
    dongKhach()[0].click(); await flushPromises()
    expect(badge()).toContain('Đã chọn 2')
    const chonTatCa = bang().querySelector('thead input[type=checkbox]')
    chonTatCa.click(); await flushPromises()
    expect(badge()).toContain('Đã chọn 3')
    chonTatCa.click(); await flushPromises()
    expect(badge()).toContain('Đã chọn 0')
  })

  it('tìm theo mã, tên hoặc SĐT', async () => {
    await moForm()
    const o = document.querySelector('.pgg-kh-search input')
    o.value = '0934633210'; o.dispatchEvent(new Event('input')); await flushPromises()
    expect(dongKhach()).toHaveLength(1)
    expect(dongKhach()[0].textContent).toContain('Trịnh Xuân Bách')
  })
})
