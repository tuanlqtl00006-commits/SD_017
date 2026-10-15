import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { svc, xuat } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), getVaiTro: vi.fn(), toggleActive: vi.fn() },
  xuat: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/nhanVienService', () => ({ nhanVienService: svc }))
vi.mock('../src/utils/exportExcel', () => ({ exportExcel: xuat }))

import NhanVienManager from '../src/components/NhanVienManager.vue'
import { cacMucSelect2, datSelect2 } from './helpersSelect2'

const nv = (id, ma, hoTen, extra = {}) => ({
  id, ma, hoTen, email: `${ma.toLowerCase()}@footstyle.vn`, soDienThoai: `09120000${String(id).padStart(2, '0')}`, gioiTinh: 'Nam',
  idVaiTro: 2, vaiTro: 'Nhân viên', hoatDong: true, diaChi: 'Hà Nội', ngaySinh: '1999-01-01', ngayVaoLam: '2024-01-01', canCuoc: null, diaChis: [], ...extra,
})
const DS = [
  nv(3, 'LQT0001', 'Lê Quốc Tuấn', { canCuoc: '001099012345' }),
  nv(2, 'NV0002', 'Trần Thị Mai', { hoatDong: false }),
  nv(1, 'NV0001', 'Nguyễn Hoàng Long', { idVaiTro: 1, vaiTro: 'Quản lý' }),
]

beforeEach(() => {
  Object.values(svc).forEach((f) => f.mockReset())
  xuat.mockReset()
  svc.getAll.mockResolvedValue(DS)
  svc.getVaiTro.mockResolvedValue([{ id: 1, ten: 'Quản lý' }, { id: 2, ten: 'Nhân viên' }])
})

async function mo() {
  const w = mount(NhanVienManager, { global: { stubs: { RouterLink: true } } })
  await flushPromises()
  return w
}

describe('Danh sách nhân viên', () => {
  it('không còn cột Vị trí; số ô mỗi dòng khớp số cột tiêu đề', async () => {
    const w = await mo()
    const tieuDe = w.findAll('thead th').map((t) => t.text())
    expect(tieuDe).not.toContain('Vị trí')
    expect(tieuDe).toEqual(['STT', '', 'Mã NV', 'Họ tên', 'Email', 'Giới tính', 'SĐT', 'Địa chỉ', 'Vai trò', 'Trạng thái', 'Hành động'])
    const hang = w.findAll('tbody tr')
    expect(hang).toHaveLength(3)
    for (const h of hang) expect(h.findAll('td')).toHaveLength(tieuDe.length)
    w.unmount()
  })

  it('trạng thái hiện "Còn làm" / "Đã nghỉ" (không còn "Hoạt động" / "Ngưng hoạt động")', async () => {
    const w = await mo()
    const hang = w.findAll('tbody tr')
    expect(hang[0].text()).toContain('Còn làm')
    expect(hang[1].text()).toContain('Đã nghỉ')
    expect(w.text()).not.toContain('Ngưng hoạt động')
    expect(w.text()).not.toMatch(/Hoạt động(?! )/)
    const loc = await cacMucSelect2(w, '#nv-trang-thai')
    expect(loc).toEqual(['Tất cả trạng thái', 'Còn làm', 'Đã nghỉ'])
    w.unmount()
  })

  it('lọc theo trạng thái "Đã nghỉ" chỉ còn nhân viên đã nghỉ', async () => {
    const w = await mo()
    await datSelect2(w, '#nv-trang-thai', 'false')
    await flushPromises()
    expect(w.findAll('tbody .ad-code').map((c) => c.text())).toEqual(['NV0002'])
    w.unmount()
  })

  it('nút ở cột Hành động: "Cho nghỉ việc" với người còn làm, "Cho làm lại" với người đã nghỉ', async () => {
    const w = await mo()
    const hang = w.findAll('tbody tr')
    expect(hang[0].find('button[title="Cho nghỉ việc"]').exists()).toBe(true)
    expect(hang[1].find('button[title="Cho làm lại"]').exists()).toBe(true)
    await hang[0].find('button[title="Cho nghỉ việc"]').trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Cho nhân viên nghỉ việc?')
    w.unmount()
  })

  it('hiện mã theo tên (LQT0001) và các mã mẫu NV giữ nguyên', async () => {
    const w = await mo()
    const maHienThi = w.findAll('tbody .ad-code').map((c) => c.text())
    expect(maHienThi).toEqual(['LQT0001', 'NV0002', 'NV0001'])
    w.unmount()
  })

  it('tìm theo số CCCD (tên trường canCuoc) và theo mã', async () => {
    const w = await mo()
    const o = w.find('input[type="search"], input[type="text"]')
    await o.setValue('001099012345')
    await flushPromises()
    expect(w.findAll('tbody .ad-code').map((c) => c.text())).toEqual(['LQT0001'])
    await o.setValue('NV0001')
    await flushPromises()
    expect(w.findAll('tbody .ad-code').map((c) => c.text())).toEqual(['NV0001'])
    w.unmount()
  })

  it('file Excel không có cột Vị trí, có Số CCCD và trạng thái Còn làm / Đã nghỉ', async () => {
    const w = await mo()
    await w.findAll('button').find((b) => b.text().includes('Xuất Excel')).trigger('click')
    expect(xuat).toHaveBeenCalledTimes(1)
    const { columns, rows } = xuat.mock.calls[0][0]
    const khoa = columns.map((c) => c.key)
    expect(khoa).not.toContain('viTri')
    expect(khoa).toContain('canCuoc')
    expect(rows[0].canCuoc).toBe('001099012345')
    expect(rows[1].canCuoc).toBe('')
    expect(rows.map((r) => r.trangThai)).toEqual(['Còn làm', 'Đã nghỉ', 'Còn làm'])
    w.unmount()
  })
})
