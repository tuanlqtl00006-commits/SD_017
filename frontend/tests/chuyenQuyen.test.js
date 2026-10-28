// Case khó bán hàng tại quầy "chuyển quyền nhân viên": quản lý đổi vai trò của người đang làm việc.
// Backend gửi vai trò hiện tại trong header X-Vai-Tro của mọi phản hồi; giao diện đổi menu, báo cho người dùng
// và rời trang không còn quyền vào (thành nhân viên mà chưa vào ca thì về trang Vào ca).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import api from '../src/services/api'
import { baoVaiTro, datXuLyDoiVaiTro, layPhien, xoaPhien } from '../src/services/phien'
import { _datLaiAuth, useAuth } from '../src/composables/useAuth'
import { _datLaiGiaoCa } from '../src/composables/useGiaoCa'
import { useToast } from '../src/composables/useToast'
import { apDungVaiTroMoi } from '../src/router'

const NV = { id: 3, ma: 'NV03', hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN', tenVaiTro: 'Nhân viên' }
const QL = { id: 1, ma: 'NV01', hoTen: 'Nguyễn Hoàng Long', vaiTro: 'QUAN_LY', tenVaiTro: 'Quản lý' }
const toast = useToast()

// Máy chủ giả cho axios: trả về theo url, kèm header vai trò
let mayChu
const adapterGoc = api.defaults.adapter
function traVe(config, status, data, vaiTro) {
  const response = { data, status, statusText: '', headers: vaiTro ? { 'x-vai-tro': vaiTro } : {}, config, request: {} }
  if (status >= 200 && status < 300) return Promise.resolve(response)
  return Promise.reject(Object.assign(new Error(`Request failed with status code ${status}`), { response, config, isAxiosError: true }))
}

function dangNhap(nguoiDung) {
  useAuth().ghiPhien({ token: 't.k', hetHan: '2099-01-01T00:00:00', nguoiDung }, true)
}

const trang = (fullPath, roles, extra = {}) => ({ fullPath, path: fullPath, name: fullPath.slice(1), meta: { roles, ...extra }, query: {} })

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  xoaPhien()
  _datLaiAuth()
  _datLaiGiaoCa()
  toast.toasts.splice(0)
  mayChu = vi.fn((config) => traVe(config, 200, {}, null))
  api.defaults.adapter = (config) => mayChu(config)
})
afterEach(() => {
  api.defaults.adapter = adapterGoc
  datXuLyDoiVaiTro(null)
})

describe('Phát hiện vai trò bị đổi qua header X-Vai-Tro', () => {
  it('chỉ báo khi vai trò khác vai trò đang lưu; khách hàng / chưa đăng nhập / không có header thì bỏ qua', () => {
    const xuLy = vi.fn()
    datXuLyDoiVaiTro(xuLy)
    baoVaiTro('QUAN_LY')
    expect(xuLy).not.toHaveBeenCalled() // chưa đăng nhập
    dangNhap(NV)
    baoVaiTro('NHAN_VIEN')
    baoVaiTro(undefined)
    expect(xuLy).not.toHaveBeenCalled()
    baoVaiTro('QUAN_LY')
    expect(xuLy).toHaveBeenCalledWith('QUAN_LY', 'NHAN_VIEN')
    dangNhap({ id: 5, hoTen: 'Khách', vaiTro: 'KHACH_HANG' })
    xuLy.mockClear()
    baoVaiTro('QUAN_LY')
    expect(xuLy).not.toHaveBeenCalled()
  })

  it('đọc header ở cả phản hồi thành công lẫn bị chặn (403 không đủ quyền sau khi bị hạ quyền)', async () => {
    const xuLy = vi.fn()
    datXuLyDoiVaiTro(xuLy)
    dangNhap(QL)
    mayChu.mockImplementationOnce((c) => traVe(c, 200, [], 'QUAN_LY'))
    await api.get('/hoa-don')
    expect(xuLy).not.toHaveBeenCalled()
    mayChu.mockImplementationOnce((c) => traVe(c, 403, { message: 'Chức năng này chỉ dành cho Quản lý.', code: 'KHONG_DU_QUYEN' }, 'NHAN_VIEN'))
    await expect(api.get('/thong-ke/tong-quan')).rejects.toMatchObject({ status: 403, code: 'KHONG_DU_QUYEN' })
    expect(xuLy).toHaveBeenCalledWith('NHAN_VIEN', 'QUAN_LY')
  })
})

describe('Áp dụng vai trò mới cho trang đang mở', () => {
  it('nhân viên đang bán được lên quản lý: giữ phiên, menu quản lý hiện ra, ở lại trang bán hàng', async () => {
    dangNhap(NV)
    const chuyenTrang = vi.fn()
    await apDungVaiTroMoi('QUAN_LY', trang('/ban-hang', ['quan-ly', 'nhan-vien']), chuyenTrang)
    const auth = useAuth()
    expect(auth.laQuanLy.value).toBe(true)
    expect(auth.trangThai.nguoiDung).toMatchObject({ id: 3, vaiTro: 'QUAN_LY', tenVaiTro: 'Quản lý' })
    expect(layPhien()).toMatchObject({ token: 't.k', nguoiDung: { vaiTro: 'QUAN_LY' } }) // tải lại trang vẫn đúng vai trò mới
    expect(chuyenTrang).not.toHaveBeenCalled()
    expect(toast.toasts.at(-1)).toMatchObject({ type: 'success', title: 'Quyền đã thay đổi' })
  })

  it('quản lý bị hạ xuống nhân viên khi đang ở trang chỉ quản lý (thống kê): rời sang trang bán hàng', async () => {
    dangNhap(QL)
    const chuyenTrang = vi.fn()
    await apDungVaiTroMoi('NHAN_VIEN', trang('/thong-ke', ['quan-ly']), chuyenTrang)
    expect(useAuth().laQuanLy.value).toBe(false)
    expect(chuyenTrang).toHaveBeenCalledWith('/ban-hang')
    expect(toast.toasts.at(-1).type).toBe('error')
    expect(toast.toasts.at(-1).message).toContain('quyền Nhân viên')
  })

  it('quản lý bị hạ xuống nhân viên khi đang bán tại quầy mà chưa vào ca: về trang Vào ca rồi quay lại', async () => {
    dangNhap(QL)
    mayChu.mockImplementation((c) => traVe(c, 200, c.url === '/giao-ca/hien-tai' ? { dangMo: null, tongKet: null, lichHomNay: [] } : {}, 'NHAN_VIEN'))
    const chuyenTrang = vi.fn()
    await apDungVaiTroMoi('NHAN_VIEN', trang('/ban-hang', ['quan-ly', 'nhan-vien']), chuyenTrang)
    expect(chuyenTrang).toHaveBeenCalledWith({ path: '/vao-ca', query: { quayLai: '/ban-hang' } })
  })

  it('bị hạ xuống nhân viên nhưng đang trong ca: ở lại bán hàng, hóa đơn chờ không mất', async () => {
    dangNhap(QL)
    mayChu.mockImplementation((c) => traVe(c, 200, c.url === '/giao-ca/hien-tai' ? { dangMo: { id: 7 }, tongKet: null, lichHomNay: [] } : {}, 'NHAN_VIEN'))
    const chuyenTrang = vi.fn()
    await apDungVaiTroMoi('NHAN_VIEN', trang('/ban-hang', ['quan-ly', 'nhan-vien']), chuyenTrang)
    expect(chuyenTrang).not.toHaveBeenCalled()
  })
})
