import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

// Nhập nhân viên hàng loạt từ Excel: hộp thoại (kiểm tra cả file -> bảng lỗi theo dòng/cột -> xem trước -> nhập -> tiến độ mail),
// file mẫu khớp backend, nút "Nhập Excel" ở trang Nhân viên.
const { svc, xuat, push } = vi.hoisted(() => ({
  svc: { getAll: vi.fn(), getVaiTro: vi.fn(), toggleActive: vi.fn(), importExcel: vi.fn(), ketQuaMailImport: vi.fn() },
  xuat: vi.fn(),
  push: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../src/services/nhanVienService', () => ({ nhanVienService: svc }))
vi.mock('../src/utils/exportExcel', async (importOriginal) => ({ ...(await importOriginal()), exportExcel: xuat }))

import NhanVienImportModal from '../src/components/nhanVien/NhanVienImportModal.vue'
import NhanVienManager from '../src/components/NhanVienManager.vue'
import { useToast } from '../src/composables/useToast'
import { buildXlsx } from '../src/utils/exportExcel'
import { HUONG_DAN_TUOI_NHAN_VIEN } from '../src/utils/kiemTra'
import { COT_MAU, COT_TAI_KHOAN, TEN_FILE_MAU, TOI_DA_DONG_NHAP, dongMau } from '../src/utils/mauNhapNhanVien'

const q = (sel) => document.body.querySelector(sel)
const qa = (sel) => [...document.body.querySelectorAll(sel)]
const chu = (sel) => q(sel)?.textContent.replace(/\s+/g, ' ').trim() ?? ''
const bam = async (sel) => {
  const nut = q(sel)
  expect(nut, `không thấy nút ${sel}`).toBeTruthy()
  nut.click()
  await flushPromises()
}

async function chonFile(ten = 'nhan-vien.xlsx', dungLuong = 2048) {
  const o = q('#nv-import-file')
  const f = new File([new Uint8Array(dungLuong)], ten, { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  Object.defineProperty(o, 'files', { value: [f], configurable: true })
  o.dispatchEvent(new Event('change'))
  await flushPromises()
  return f
}

async function moModal(props = {}) {
  const w = mount(NhanVienImportModal, { props: { chuKyMailMs: 5, ...props }, attachTo: document.body })
  await flushPromises()
  return w
}

const loiFile422 = (loi, tongDong = 10) =>
  Object.assign(new Error(`File có ${loi.length} lỗi. Cả file bị từ chối, chưa có nhân viên nào được thêm.`), {
    status: 422,
    code: 'FILE_KHONG_HOP_LE',
    chiTiet: { loi, tongDong, soDongLoi: new Set(loi.map((l) => l.dong)).size },
  })
const XEM_TRUOC = {
  daLuu: false, soNhanVien: 2, maLo: null, soMail: 0, mailDaCauHinh: true, taiKhoan: [],
  xemTruoc: [
    { dong: 2, ma: 'NVM01', hoTen: 'Nguyễn Văn Mẫu', email: 'nguyen.van.mau@example.com', vaiTro: 'Nhân viên' },
    { dong: 3, ma: 'TTVD01', hoTen: 'Trần Thị Ví Dụ', email: 'tran.thi.vi.du@example.com', vaiTro: 'Quản lý' },
  ],
}
const DA_NHAP = {
  daLuu: true, soNhanVien: 2, xemTruoc: [], maLo: 'NVIMP-1760000000000', soMail: 2, mailDaCauHinh: true,
  taiKhoan: [
    { ma: 'NVM01', hoTen: 'Nguyễn Văn Mẫu', email: 'nguyen.van.mau@example.com', vaiTro: 'Nhân viên', matKhauTam: 'Kx7mP2qZ9vLd' },
    { ma: 'TTVD01', hoTen: 'Trần Thị Ví Dụ', email: 'tran.thi.vi.du@example.com', vaiTro: 'Quản lý', matKhauTam: 'Rt4nW8cH3yBs' },
  ],
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 15, 9, 0, 0))
  document.body.innerHTML = ''
  Object.values(svc).forEach((f) => f.mockReset())
  xuat.mockReset()
  push.mockReset()
  useToast().toasts.splice(0)
})
afterEach(() => vi.useRealTimers())

describe('Nhập Excel: bước chọn file', () => {
  it('có tiêu đề, 3 bước, nút tải file mẫu, và nêu rõ các quy tắc cô yêu cầu', async () => {
    const w = await moModal()
    expect(chu('.ad-modal-title')).toBe('Nhập nhân viên từ Excel')
    expect(qa('.nv-imp-steps li').map((l) => l.textContent.replace(/\s+/g, ' ').trim())).toEqual(['1 Chọn file', '2 Kiểm tra', '3 Hoàn tất'])
    const quyTac = chu('[data-testid=nv-import-quy-tac]')
    expect(quyTac).toContain('Sai dù chỉ một dòng thì cả file bị từ chối')
    expect(quyTac).toContain('chưa có nhân viên nào được thêm')
    expect(quyTac).toContain(`Tối đa ${TOI_DA_DONG_NHAP} nhân viên mỗi file`)
    expect(quyTac).toContain('không được trùng')
    expect(quyTac).toContain('gửi mail tài khoản')
    expect(quyTac).toContain(HUONG_DAN_TUOI_NHAN_VIEN) // tuổi tối thiểu kèm căn cứ pháp luật
    expect(quyTac).toContain('Điều 143')
    expect(q('[data-testid=nv-import-kiem-tra]').disabled).toBe(true) // chưa chọn file
    w.unmount()
  })

  it('nút tải file mẫu xuất đúng cột và hai dòng ví dụ', async () => {
    const w = await moModal()
    await bam('[data-testid=nv-import-tai-mau]')
    expect(xuat).toHaveBeenCalledTimes(1)
    const o = xuat.mock.calls[0][0]
    expect(o.filename).toBe(TEN_FILE_MAU)
    expect(o.columns).toEqual(COT_MAU)
    expect(o.rows).toHaveLength(2)
    expect(o.rows[0]).toMatchObject({ hoTen: 'Nguyễn Văn Mẫu', soDienThoai: '0900000001', ngaySinh: '25/12/2000' })
    expect(o.rows[1].ngayVaoLam).toBe('15/10/2026') // hôm nay, dạng dd/mm/yyyy
    w.unmount()
  })

  it('chọn file không phải .xlsx (csv, xls) hoặc quá lớn thì báo ngay, không cho kiểm tra', async () => {
    const w = await moModal()
    await chonFile('nhan-vien.csv')
    expect(chu('[data-testid=nv-import-loi-chung]')).toContain('Chỉ nhận file Excel định dạng .xlsx')
    expect(q('[data-testid=nv-import-kiem-tra]').disabled).toBe(true)
    await chonFile('nhan-vien.xls')
    expect(chu('[data-testid=nv-import-loi-chung]')).toContain('.xlsx')
    await chonFile('lon.xlsx', 5 * 1024 * 1024 + 1)
    expect(chu('[data-testid=nv-import-loi-chung]')).toContain('File quá lớn')
    expect(q('[data-testid=nv-import-kiem-tra]').disabled).toBe(true)
    expect(svc.importExcel).not.toHaveBeenCalled()
    await chonFile('NHAN-VIEN.XLSX') // viết hoa đuôi vẫn nhận
    expect(q('[data-testid=nv-import-loi-chung]')).toBeNull()
    expect(chu('[data-testid=nv-import-ten-file]')).toContain('NHAN-VIEN.XLSX')
    expect(q('[data-testid=nv-import-kiem-tra]').disabled).toBe(false)
    w.unmount()
  })

  it('Hủy đóng hộp thoại', async () => {
    const w = await moModal()
    qa('.ad-modal-footer button').find((b) => b.textContent.trim() === 'Hủy').click()
    expect(w.emitted('close')).toHaveLength(1)
    w.unmount()
  })
})

describe('Nhập Excel: file sai thì CẢ FILE bị từ chối', () => {
  const LOI = [
    { dong: 3, truong: 'email', cot: 'Email', thongBao: 'Email đã được sử dụng.' },
    { dong: 3, truong: 'soDienThoai', cot: 'Số điện thoại', thongBao: 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.' },
    { dong: 7, truong: 'ngaySinh', cot: 'Ngày sinh', thongBao: 'Nhân viên phải từ đủ 18 tuổi.' },
  ]

  it('hiện bảng lỗi theo dòng - cột - nội dung, báo rõ chưa thêm ai; chỉ gọi kiểm tra, không bao giờ lưu', async () => {
    svc.importExcel.mockRejectedValue(loiFile422(LOI))
    const w = await moModal()
    const f = await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    expect(svc.importExcel).toHaveBeenCalledTimes(1)
    expect(svc.importExcel).toHaveBeenCalledWith(f, { chiKiemTra: true })
    const phan = chu('[data-testid=nv-import-loi]')
    expect(phan).toContain('Cả file bị từ chối - chưa có nhân viên nào được thêm.')
    expect(phan).toContain('Có 3 lỗi ở 2/10 dòng')
    expect(qa('[data-testid=nv-import-loi] tbody tr').map((tr) => [...tr.children].map((td) => td.textContent.trim()))).toEqual([
      ['3', 'Email', 'Email đã được sử dụng.'],
      ['3', 'Số điện thoại', 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.'],
      ['7', 'Ngày sinh', 'Nhân viên phải từ đủ 18 tuổi.'],
    ])
    expect(qa('[data-testid=nv-import-loi] thead th').map((th) => th.textContent.trim())).toEqual(['Dòng', 'Cột', 'Lỗi'])
    expect(q('[data-testid=nv-import-nhap]')).toBeNull() // không có nút nhập
    expect(q('[data-testid=nv-import-xem]')).toBeNull()
    expect(w.emitted('imported')).toBeUndefined()
    w.unmount()
  })

  it('bấm "Chọn file khác" quay về bước chọn, ô file được làm sạch, phải chọn lại mới kiểm tra được', async () => {
    svc.importExcel.mockRejectedValueOnce(loiFile422(LOI))
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-chon-lai]')
    expect(q('[data-testid=nv-import-chon]')).toBeTruthy()
    expect(q('[data-testid=nv-import-loi]')).toBeNull()
    expect(q('#nv-import-file').value).toBe('')
    expect(q('[data-testid=nv-import-kiem-tra]').disabled).toBe(true)
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC)
    await chonFile('da-sua.xlsx')
    await bam('[data-testid=nv-import-kiem-tra]')
    expect(q('[data-testid=nv-import-xem]')).toBeTruthy()
    expect(svc.importExcel).toHaveBeenCalledTimes(2)
    w.unmount()
  })

  it('lỗi chung (thiếu cột, quá 100 dòng, mất mạng) thì báo một câu ở bước chọn, không có bảng lỗi', async () => {
    svc.importExcel.mockRejectedValue(Object.assign(new Error('File thiếu cột bắt buộc: Email. Hãy dùng đúng file mẫu của hệ thống.'), { status: 400, code: null, chiTiet: null }))
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    expect(chu('[data-testid=nv-import-loi-chung]')).toContain('File thiếu cột bắt buộc: Email')
    expect(q('[data-testid=nv-import-loi]')).toBeNull()
    expect(q('[data-testid=nv-import-chon]')).toBeTruthy()
    expect(q('[data-testid=nv-import-kiem-tra]').disabled).toBe(false) // thử lại được
    w.unmount()
  })

  it('mã lỗi file nhưng thiếu danh sách dòng thì vẫn báo câu của máy chủ (không vỡ giao diện)', async () => {
    svc.importExcel.mockRejectedValue(Object.assign(new Error('File không hợp lệ.'), { status: 422, code: 'FILE_KHONG_HOP_LE', chiTiet: null }))
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    expect(chu('[data-testid=nv-import-loi-chung]')).toBe('File không hợp lệ.')
    w.unmount()
  })
})

describe('Nhập Excel: file hợp lệ -> xem trước -> nhập', () => {
  it('xem trước liệt kê từng nhân viên với mã dự kiến; nút nhập ghi rõ số người', async () => {
    svc.importExcel.mockResolvedValue(XEM_TRUOC)
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    expect(chu('[data-testid=nv-import-xem]')).toContain('File hợp lệ. Sẽ thêm 2 nhân viên')
    expect(qa('[data-testid=nv-import-xem] tbody tr').map((tr) => [...tr.children].map((td) => td.textContent.trim()))).toEqual([
      ['2', 'NVM01', 'Nguyễn Văn Mẫu', 'nguyen.van.mau@example.com', 'Nhân viên'],
      ['3', 'TTVD01', 'Trần Thị Ví Dụ', 'tran.thi.vi.du@example.com', 'Quản lý'],
    ])
    expect(chu('[data-testid=nv-import-nhap]')).toBe('Nhập 2 nhân viên vào hệ thống')
    expect(q('[data-testid=nv-import-canh-bao-mail]')).toBeNull()
    expect(svc.importExcel).toHaveBeenCalledTimes(1) // mới chỉ kiểm tra, chưa lưu
    w.unmount()
  })

  it('chưa cấu hình Gmail thì cảnh báo ngay từ bước xem trước', async () => {
    svc.importExcel.mockResolvedValue({ ...XEM_TRUOC, mailDaCauHinh: false })
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    expect(chu('[data-testid=nv-import-canh-bao-mail]')).toContain('chưa cấu hình Gmail')
    w.unmount()
  })

  it('bấm nhập: gửi đúng file với chiKiemTra=false, báo thành công, phát sự kiện imported, hiện tài khoản vừa tạo (mật khẩu tạm được che)', async () => {
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC).mockResolvedValueOnce(DA_NHAP)
    svc.ketQuaMailImport.mockResolvedValue({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    const w = await moModal()
    const f = await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    expect(svc.importExcel).toHaveBeenCalledTimes(2)
    expect(svc.importExcel).toHaveBeenLastCalledWith(f, { chiKiemTra: false })
    expect(chu('[data-testid=nv-import-xong]')).toContain('Đã thêm 2 nhân viên.')
    expect(w.emitted('imported')).toHaveLength(1)
    expect(w.emitted('imported')[0][0].soNhanVien).toBe(2)
    expect(useToast().toasts.at(-1).message).toBe('Đã thêm 2 nhân viên từ file Excel.')
    expect(qa('[data-testid=nv-import-xong] tbody tr')).toHaveLength(2)
    expect(qa('[data-testid=nv-import-mat-khau]').map((e) => e.textContent)).toEqual(['••••••••••', '••••••••••'])
    expect(document.body.textContent).not.toContain('Kx7mP2qZ9vLd')
    await bam('[data-testid=nv-import-hien-mat-khau]')
    expect(qa('[data-testid=nv-import-mat-khau]').map((e) => e.textContent)).toEqual(['Kx7mP2qZ9vLd', 'Rt4nW8cH3yBs'])
    await bam('[data-testid=nv-import-hien-mat-khau]')
    expect(document.body.textContent).not.toContain('Kx7mP2qZ9vLd')
    w.unmount()
  })

  it('tải danh sách tài khoản (kèm mật khẩu tạm) ra file Excel', async () => {
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC).mockResolvedValueOnce(DA_NHAP)
    svc.ketQuaMailImport.mockResolvedValue({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    await bam('[data-testid=nv-import-tai-tai-khoan]')
    expect(xuat).toHaveBeenCalledTimes(1)
    const o = xuat.mock.calls[0][0]
    expect(o.filename).toBe('tai_khoan_nhan_vien_moi_2026-10-15.xlsx')
    expect(o.columns).toEqual(COT_TAI_KHOAN)
    expect(o.rows).toEqual(DA_NHAP.taiKhoan)
    w.unmount()
  })

  it('có người vừa thêm trùng email trong lúc xem trước: máy chủ từ chối khi nhập thật -> hiện bảng lỗi, không báo thành công', async () => {
    svc.importExcel
      .mockResolvedValueOnce(XEM_TRUOC)
      .mockRejectedValueOnce(loiFile422([{ dong: 2, truong: 'email', cot: 'Email', thongBao: 'Email đã được sử dụng.' }], 2))
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    expect(q('[data-testid=nv-import-loi]')).toBeTruthy()
    expect(q('[data-testid=nv-import-xong]')).toBeNull()
    expect(w.emitted('imported')).toBeUndefined()
    expect(useToast().toasts.filter((t) => t.message.includes('Đã thêm'))).toHaveLength(0)
    w.unmount()
  })

  it('mất kết nối khi nhập thật: ở lại bước xem trước, báo lỗi, bấm nhập lại được', async () => {
    svc.importExcel
      .mockResolvedValueOnce(XEM_TRUOC)
      .mockRejectedValueOnce(Object.assign(new Error('Không kết nối được máy chủ.'), { status: null, code: null, chiTiet: null }))
      .mockResolvedValueOnce(DA_NHAP)
    svc.ketQuaMailImport.mockResolvedValue({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    expect(q('[data-testid=nv-import-xem]')).toBeTruthy()
    expect(chu('[data-testid=nv-import-loi-chung]')).toBe('Không kết nối được máy chủ.')
    expect(q('[data-testid=nv-import-nhap]').disabled).toBe(false)
    await bam('[data-testid=nv-import-nhap]')
    expect(q('[data-testid=nv-import-xong]')).toBeTruthy()
    expect(w.emitted('imported')).toHaveLength(1)
    w.unmount()
  })

  it('đang xử lý thì nút bị khóa, bấm nhiều lần chỉ gửi một lần', async () => {
    let xong
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC).mockReturnValueOnce(new Promise((r) => (xong = r)))
    svc.ketQuaMailImport.mockResolvedValue({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    q('[data-testid=nv-import-nhap]').click()
    q('[data-testid=nv-import-nhap]').click()
    await flushPromises()
    expect(svc.importExcel).toHaveBeenCalledTimes(2) // 1 lần kiểm tra + đúng 1 lần nhập
    expect(q('[data-testid=nv-import-nhap]').disabled).toBe(true)
    xong(DA_NHAP)
    await flushPromises()
    expect(q('[data-testid=nv-import-xong]')).toBeTruthy()
    w.unmount()
  })
})

describe('Nhập Excel: tiến độ gửi mail tài khoản', () => {
  async function nhapXong(ketQuaMail, cauHinh = DA_NHAP) {
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC).mockResolvedValueOnce(cauHinh)
    if (Array.isArray(ketQuaMail)) ketQuaMail.forEach((k) => svc.ketQuaMailImport.mockResolvedValueOnce(k))
    else svc.ketQuaMailImport.mockResolvedValue(ketQuaMail)
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    return w
  }
  const mailChu = () => chu('[data-testid=nv-import-mail]')

  it('hỏi lại theo mã lô cho tới khi gửi xong rồi hiện "Đã gửi 2/2 mail"', async () => {
    const w = await nhapXong([
      { tong: 2, daGui: 0, loi: 0, giaLap: 0, xong: false, lyDo: '' },
      { tong: 2, daGui: 1, loi: 0, giaLap: 0, xong: false, lyDo: '' },
      { tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' },
    ])
    expect(mailChu()).toContain('Đang gửi mail tài khoản')
    await vi.waitFor(() => expect(mailChu()).toContain('Đã gửi 2/2 mail thông báo tài khoản.'), { timeout: 2000 })
    expect(svc.ketQuaMailImport).toHaveBeenCalledWith('NVIMP-1760000000000')
    expect(svc.ketQuaMailImport).toHaveBeenCalledTimes(3)
    await new Promise((r) => setTimeout(r, 40))
    expect(svc.ketQuaMailImport).toHaveBeenCalledTimes(3) // xong là dừng hỏi
    w.unmount()
  })

  it('có thư lỗi thì nêu số lượng và lý do, kèm hướng xử lý', async () => {
    const w = await nhapXong({ tong: 2, daGui: 1, loi: 1, giaLap: 0, xong: true, lyDo: 'Sai mật khẩu ứng dụng Gmail' })
    await vi.waitFor(() => expect(mailChu()).toContain('Gửi mail lỗi 1/2: Sai mật khẩu ứng dụng Gmail'), { timeout: 2000 })
    expect(mailChu()).toContain('gửi mật khẩu tạm')
    w.unmount()
  })

  it('chưa cấu hình Gmail (mail giả lập) thì nói rõ không gửi mail thật', async () => {
    const w = await nhapXong({ tong: 2, daGui: 0, loi: 0, giaLap: 2, xong: true, lyDo: '' }, { ...DA_NHAP, mailDaCauHinh: false })
    expect(mailChu()).toContain('không gửi mail thật')
    w.unmount()
  })

  it('mất kết nối thoáng qua khi hỏi tiến độ thì thử lại, không vỡ', async () => {
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC).mockResolvedValueOnce(DA_NHAP)
    svc.ketQuaMailImport.mockRejectedValueOnce(new Error('mạng')).mockResolvedValue({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    const w = await moModal()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    await vi.waitFor(() => expect(mailChu()).toContain('Đã gửi 2/2'), { timeout: 2000 })
    w.unmount()
  })

  it('đóng hộp thoại thì dừng hỏi tiến độ mail', async () => {
    const w = await nhapXong({ tong: 2, daGui: 0, loi: 0, giaLap: 0, xong: false, lyDo: '' })
    await vi.waitFor(() => expect(svc.ketQuaMailImport.mock.calls.length).toBeGreaterThanOrEqual(2), { timeout: 2000 })
    w.unmount()
    await new Promise((r) => setTimeout(r, 30))
    const soLan = svc.ketQuaMailImport.mock.calls.length
    await new Promise((r) => setTimeout(r, 60))
    expect(svc.ketQuaMailImport.mock.calls.length).toBe(soLan)
  })

  it('nút Đóng ở bước hoàn tất phát sự kiện close', async () => {
    const w = await nhapXong({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    await bam('[data-testid=nv-import-dong]')
    expect(w.emitted('close')).toHaveLength(1)
    w.unmount()
  })
})

describe('Trang Nhân viên: nút Nhập Excel', () => {
  const DS = [{ id: 1, ma: 'NV01', hoTen: 'Nguyễn Hoàng Long', email: 'long@footstyle.vn', soDienThoai: '0912000001', canCuoc: null, idVaiTro: 1, vaiTro: 'Quản lý', hoatDong: true, gioiTinh: 'Nam', ngayVaoLam: '2022-01-10', diaChi: 'x', diaChis: [] }]

  it('mở hộp thoại nhập; nhập xong thì danh sách tự tải lại; đóng thì hộp thoại biến mất', async () => {
    svc.getAll.mockResolvedValue(DS)
    svc.getVaiTro.mockResolvedValue([{ id: 1, ten: 'Quản lý' }, { id: 2, ten: 'Nhân viên' }])
    svc.importExcel.mockResolvedValueOnce(XEM_TRUOC).mockResolvedValueOnce(DA_NHAP)
    svc.ketQuaMailImport.mockResolvedValue({ tong: 2, daGui: 2, loi: 0, giaLap: 0, xong: true, lyDo: '' })
    const w = mount(NhanVienManager, { attachTo: document.body, global: { stubs: { RouterLink: true } } })
    await flushPromises()
    expect(q('.ad-modal')).toBeNull()
    const nut = w.find('[data-testid=nv-nhap-excel]')
    expect(nut.text()).toContain('Nhập Excel')
    const soLanTaiBanDau = svc.getAll.mock.calls.length
    await nut.trigger('click')
    await flushPromises()
    expect(q('.ad-modal')).toBeTruthy()
    await chonFile()
    await bam('[data-testid=nv-import-kiem-tra]')
    await bam('[data-testid=nv-import-nhap]')
    expect(svc.getAll.mock.calls.length).toBeGreaterThan(soLanTaiBanDau) // làm mới danh sách để thấy người mới
    await bam('[data-testid=nv-import-dong]')
    expect(q('.ad-modal')).toBeNull()
    w.unmount()
  })
})

/* ===== File mẫu phải khớp backend (NhanVienImportService.Cot) ===== */
const javaFile = resolve(process.cwd(), '../backend/src/main/java/com/footstyle/demo/service/NhanVienImportService.java')
const coJava = existsSync(javaFile)

// Giống NhanVienImportService.chuanHoaTen: bỏ dấu, chữ thường, chỉ giữ chữ / số, gộp khoảng trắng; và bỏ chữ "bắt buộc"
const chuanHoaTen = (s) =>
  String(s)
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
    .replace(/\bbat buoc\b/g, '')
    .replace(/\s+/g, ' ')
    .trim()

function docCotJava() {
  const nguon = readFileSync(javaFile, 'utf-8')
  const kieu = /^\s*[A-Z_]+\("(\w+)",\s*"([^"]+)",\s*(true|false)((?:,\s*"[^"]+")*)\)[,;]/gm
  const ds = []
  let m
  while ((m = kieu.exec(nguon))) {
    ds.push({ truong: m[1], tieuDe: m[2], batBuoc: m[3] === 'true', tenKhac: [...m[4].matchAll(/"([^"]+)"/g)].map((x) => x[1]) })
  }
  return { nguon, ds }
}

describe.skipIf(!coJava)('File mẫu khớp backend', () => {
  it('đọc được đủ 11 cột trong file Java (kiểm tra chính bộ đọc của test)', () => {
    expect(docCotJava().ds).toHaveLength(11)
  })

  it('mỗi tiêu đề trong file mẫu được backend nhận ra; cột bắt buộc có dấu (*) và ngược lại', () => {
    const { ds } = docCotJava()
    for (const c of COT_MAU) {
      const ten = chuanHoaTen(c.header)
      const cotJava = ds.find((d) => d.tenKhac.includes(ten))
      expect(cotJava, `backend không nhận ra tiêu đề "${c.header}" (chuẩn hóa: "${ten}")`).toBeTruthy()
      expect(c.key, `khóa cột "${c.header}"`).toBe(cotJava.truong === 'idVaiTro' ? 'vaiTro' : cotJava.truong)
      expect(c.header.includes('(*)'), `dấu (*) của "${c.header}"`).toBe(cotJava.batBuoc)
    }
  })

  it('file mẫu có đủ mọi cột của backend, mỗi cột đúng một lần', () => {
    const { ds } = docCotJava()
    for (const d of ds) {
      const soLan = COT_MAU.filter((c) => d.tenKhac.includes(chuanHoaTen(c.header))).length
      expect(soLan, `cột "${d.tieuDe}"`).toBe(1)
    }
    expect(COT_MAU).toHaveLength(ds.length)
  })

  it('tiêu đề trong file mẫu giống đúng tiêu đề backend ghi trong báo lỗi (khác nhau chỉ ở dấu (*))', () => {
    const { ds } = docCotJava()
    for (const c of COT_MAU) {
      const ten = chuanHoaTen(c.header)
      const cotJava = ds.find((d) => d.tenKhac.includes(ten))
      expect(c.header.replace(/\s*\(\*\)/, '')).toBe(cotJava.tieuDe)
    }
  })

  it('số dòng tối đa mỗi file giống backend', () => {
    expect(docCotJava().nguon).toContain(`TOI_DA_DONG = ${TOI_DA_DONG_NHAP};`)
  })

  it('hai dòng ví dụ điền đủ các cột bắt buộc', () => {
    const batBuoc = COT_MAU.filter((c) => c.header.includes('(*)')).map((c) => c.key)
    for (const dong of dongMau('2026-10-15')) for (const k of batBuoc) expect(dong[k], `${k} của dòng mẫu`).toBeTruthy()
  })
})

describe('File Excel mẫu: cột văn bản', () => {
  it('cả cột có định dạng văn bản (@) nên Excel giữ số 0 đầu của SĐT / căn cước và ngày dd/mm/yyyy', async () => {
    const blob = buildXlsx({ sheetName: 'Nhân viên', columns: COT_MAU, rows: dongMau('2026-10-15') })
    const text = new TextDecoder().decode(new Uint8Array(await blob.arrayBuffer()))
    expect(text).toContain('numFmtId="49"')
    expect(text).toContain('<cellXfs count="4">')
    expect(text).toContain('<col min="4" max="4" width="17" customWidth="1" style="3"/>') // cột Số điện thoại
    expect(text).toContain('<c r="D2" s="3" t="inlineStr"><is><t xml:space="preserve">0900000001</t></is></c>') // giữ nguyên 0900000001
    expect(text).toContain('<c r="C2" s="3" t="inlineStr"><is><t xml:space="preserve">25/12/2000</t></is></c>')
    expect(text).toContain('<c r="F2" s="3" t="inlineStr"><is><t xml:space="preserve">001200012345</t></is></c>')
  })

  it('cột không đánh dấu text vẫn như cũ (không đổi các file xuất khác)', async () => {
    const blob = buildXlsx({ columns: [{ header: 'Mã', key: 'ma' }, { header: 'Tiền', key: 'tien', money: true }], rows: [{ ma: 'HD1', tien: 5000 }] })
    const text = new TextDecoder().decode(new Uint8Array(await blob.arrayBuffer()))
    expect(text).toContain('<c r="A2" t="inlineStr">')
    expect(text).toContain('<c r="B2" s="2"><v>5000</v></c>')
    expect(text).not.toContain('style="3"')
  })
})
