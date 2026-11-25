import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// Góp ý 25/11: nút "Màn hình khách hàng" ở quầy mở cửa sổ riêng cho khách xem hóa đơn đang bán, cập nhật ngay.
import ManHinhKhachView from '../src/views/ManHinhKhachView.vue'
import { guiManHinhKhach, KHOA_MAN_KHACH } from '../src/utils/manHinhKhach'

const banChup = (ghiDe = {}) => ({
  tenCuaHang: 'FootStyle',
  maHoaDon: 'HD1001',
  tenKhach: 'Nguyễn Văn A',
  dong: [
    { id: 1, ten: 'Yonex Astrox 99 Pro', phanLoai: 'Đỏ · 4U', anh: '/anh/vot.jpg', soLuong: 2, donGia: 3861000, giaGoc: 4290000, phanTramGiam: 10 },
  ],
  phieu: { ma: 'VCH20', thongTin: 'Giảm 20% · Tối đa 500.000 ₫ · Đơn từ 2.000.000 ₫', giam: 500000 },
  tienDon: [
    { khoa: 'tong-tien-hang', nhan: 'Tổng tiền hàng', so: 8580000, dau: '' },
    { khoa: 'giam-dot', nhan: 'Giảm giá', so: 858000, dau: '-' },
    { khoa: 'phieu', nhan: 'Phiếu giảm giá', so: 500000, dau: '-' },
    { khoa: 'tong-thanh-toan', nhan: 'Tổng thanh toán', so: 7222000, dau: '' },
  ],
  tongThanhToan: 7222000,
  phuongThuc: 'Tiền mặt',
  ...ghiDe,
})

beforeEach(() => localStorage.clear())
afterEach(() => vi.restoreAllMocks())

describe('Màn hình khách hàng', () => {
  it('mở cửa sổ là có ngay hóa đơn đang bán: mã hóa đơn, sản phẩm (ảnh, tên, số lượng, giá), phiếu, các dòng tiền, tổng', async () => {
    guiManHinhKhach(banChup())
    const w = mount(ManHinhKhachView)
    await flushPromises()
    expect(w.get('[data-testid="mk-ma"]').text()).toContain('HD1001')
    const dong = w.get('[data-testid="mk-dong"]')
    expect(dong.find('img').attributes('src')).toBe('/anh/vot.jpg')
    expect(dong.text()).toContain('Yonex Astrox 99 Pro')
    expect(dong.text()).toContain('2 × 3.861.000 ₫')
    expect(dong.text()).toContain('-10%')
    expect(w.text()).toContain('1 sản phẩm · 2 món')
    expect(dong.text()).toContain('7.722.000 ₫')
    expect(w.get('[data-testid="mk-phieu"]').text()).toContain('VCH20')
    expect(w.get('[data-testid="mk-phieu"]').text()).toContain('Giảm 20%')
    expect(w.text()).toContain('-858.000 ₫')
    expect(w.text()).toContain('-500.000 ₫')
    expect(w.get('[data-testid="mk-tong"]').text()).toBe('7.222.000 ₫')
    expect(w.get('[data-testid="mk-phuong-thuc"]').text()).toContain('Tiền mặt')
    w.unmount()
  })

  it('nhân viên đổi số lượng / chuyển hóa đơn chờ: màn khách cập nhật theo (sự kiện storage giữa hai cửa sổ)', async () => {
    guiManHinhKhach(banChup())
    const w = mount(ManHinhKhachView)
    await flushPromises()
    const moi = banChup({ maHoaDon: 'HD1002', dong: [{ id: 9, ten: 'Lining Axforce 80', phanLoai: '', anh: '', soLuong: 1, donGia: 2500000, giaGoc: 2500000 }], phieu: null, tongThanhToan: 2500000 })
    window.dispatchEvent(new StorageEvent('storage', { key: KHOA_MAN_KHACH, newValue: JSON.stringify(moi) }))
    await flushPromises()
    expect(w.get('[data-testid="mk-ma"]').text()).toContain('HD1002')
    expect(w.findAll('[data-testid="mk-dong"]')).toHaveLength(1)
    expect(w.text()).toContain('Lining Axforce 80')
    expect(w.find('[data-testid="mk-phieu"]').exists()).toBe(false)
    expect(w.get('[data-testid="mk-tong"]').text()).toBe('2.500.000 ₫')
    w.unmount()
  })

  it('chưa có hóa đơn: hiện lời chào', async () => {
    const w = mount(ManHinhKhachView)
    await flushPromises()
    expect(w.text()).toContain('Xin chào quý khách')
    w.unmount()
  })
})
