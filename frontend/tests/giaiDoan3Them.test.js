import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

// GĐ3 bổ sung theo video mẫu: Xuất / Nhập Excel lịch làm việc, Trợ lý AI cho nhân viên, Gợi ý AI từ ảnh trên web khách.
const { lichSvc, troLySvc, xuat } = vi.hoisted(() => ({
  lichSvc: { importExcel: vi.fn() },
  troLySvc: { chao: vi.fn(), hoi: vi.fn() },
  xuat: vi.fn(),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ['to'], template: '<a :href="to" @click="$emit(\'click\')"><slot /></a>' },
}))
vi.mock('../src/services/lichLamViecService', () => ({ lichLamViecService: lichSvc }))
vi.mock('../src/services/troLyService', () => ({ troLyService: troLySvc }))
vi.mock('../src/utils/exportExcel', async (importOriginal) => ({ ...(await importOriginal()), exportExcel: xuat }))

import LichImportModal from '../src/components/lichLamViec/LichImportModal.vue'
import TroLyNoiBo from '../src/components/troLy/TroLyNoiBo.vue'
import { COT_LICH, dongMauLich, dongTuLich, taiFileMauLich, xuatLichTuan } from '../src/utils/mauLichLamViec'
import { docDacTrung, doGiongBieuDo, doKhopMau, mauTheoTen, tenMauGanNhat, xepHangGoiY } from '../src/utils/goiYAnh'

const q = (sel) => document.body.querySelector(sel)
const qa = (sel) => [...document.body.querySelectorAll(sel)]
const chu = (sel) => q(sel)?.textContent.replace(/\s+/g, ' ').trim() ?? ''
const oBang = (tr) => [...tr.querySelectorAll('td')].map((td) => td.textContent.replace(/\s+/g, ' ').trim()).join(' | ')
const RouterLinkGia = { props: ['to'], emits: ['click'], template: '<a :href="to" @click.prevent="$emit(\'click\')"><slot /></a>' }

afterEach(() => {
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

/* ====================== Excel lịch làm việc ====================== */

const NHAN_VIENS = [
  { id: 3, maNhanVien: 'NV03', hoTen: 'Lê Quốc Bảo' },
  { id: 4, maNhanVien: 'NV04', hoTen: 'Phạm Minh Anh' },
]
const CAS = [
  { id: 1, tenCa: 'Ca sáng', gioBatDau: '07:00', gioKetThuc: '12:00', hoatDong: true },
  { id: 2, tenCa: 'Ca đêm', gioBatDau: '22:00', gioKetThuc: '23:00', hoatDong: false },
  { id: 3, tenCa: 'Ca chiều', gioBatDau: '12:00', gioKetThuc: '17:00', hoatDong: true },
]

describe('File Excel lịch làm việc', () => {
  it('tiêu đề cột khớp với backend LichLamViecImportService', () => {
    const java = readFileSync(resolve(__dirname, '../../backend/src/main/java/com/footstyle/demo/service/LichLamViecImportService.java'), 'utf8')
    for (const c of COT_LICH.filter((c) => c.key !== 'gio')) expect(java).toContain(`"${c.header}"`)
  })

  it('xuất lịch tuần: sắp theo ngày, giờ; ngày dd/mm/yyyy', () => {
    const dong = dongTuLich([
      { maNhanVien: 'NV04', hoTen: 'Phạm Minh Anh', ngayLam: '2026-10-20', tenCa: 'Ca chiều', gioBatDau: '12:00', gioKetThuc: '17:00' },
      { maNhanVien: 'NV03', hoTen: 'Lê Quốc Bảo', ngayLam: '2026-10-20', tenCa: 'Ca sáng', gioBatDau: '07:00', gioKetThuc: '12:00', ghiChu: 'Mở cửa' },
      { maNhanVien: 'NV03', hoTen: 'Lê Quốc Bảo', ngayLam: '2026-10-19', tenCa: 'Ca chiều', gioBatDau: '12:00', gioKetThuc: '17:00' },
    ])
    expect(dong.map((d) => `${d.ngayLam} ${d.ca}`)).toEqual(['19/10/2026 Ca chiều', '20/10/2026 Ca sáng', '20/10/2026 Ca chiều'])
    expect(dong[1]).toEqual({ maNhanVien: 'NV03', hoTen: 'Lê Quốc Bảo', ngayLam: '20/10/2026', ca: 'Ca sáng', gio: '07:00 - 12:00', ghiChu: 'Mở cửa' })
    xuatLichTuan([], '2026-10-19')
    expect(xuat).toHaveBeenCalledWith(expect.objectContaining({ filename: 'lich_lam_viec_2026-10-19.xlsx', columns: COT_LICH }))
  })

  it('file mẫu dùng nhân viên / ca thật đang dùng, ngày không nằm trong quá khứ', () => {
    const tuanTruocDaQua = dongMauLich({ nhanViens: NHAN_VIENS, cas: CAS, thu2: '2026-10-19', homNay: '2026-10-23' })
    expect(tuanTruocDaQua.map((d) => [d.maNhanVien, d.ca, d.ngayLam])).toEqual([['NV03', 'Ca sáng', '23/10/2026'], ['NV04', 'Ca chiều', '23/10/2026']])
    expect(dongMauLich({ nhanViens: NHAN_VIENS, cas: CAS, thu2: '2026-10-26', homNay: '2026-10-23' })[0].ngayLam).toBe('26/10/2026')
    expect(dongMauLich({ nhanViens: [], cas: CAS, thu2: '2026-10-26' })).toEqual([])
    taiFileMauLich({ nhanViens: NHAN_VIENS, cas: CAS, thu2: '2026-10-26', homNay: '2026-10-23' })
    expect(xuat.mock.calls[0][0].filename).toBe('mau_nhap_lich_lam_viec.xlsx')
    expect(xuat.mock.calls[0][0].rows).toHaveLength(2)
  })
})

describe('Hộp thoại nhập Excel lịch', () => {
  async function moVaChonFile() {
    mount(LichImportModal, { props: { nhanViens: NHAN_VIENS, cas: CAS, thu2: '2026-10-26', homNay: '2026-10-23' }, attachTo: document.body })
    await flushPromises()
    const o = q('[data-testid="ll-import-file"]')
    const f = new File([new Uint8Array(100)], 'lich.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    Object.defineProperty(o, 'files', { value: [f], configurable: true })
    o.dispatchEvent(new Event('change'))
    await flushPromises()
    return f
  }

  it('chỉ nhận .xlsx; quy tắc liệt kê ca đang dùng', async () => {
    mount(LichImportModal, { props: { nhanViens: NHAN_VIENS, cas: CAS, thu2: '2026-10-26', homNay: '2026-10-23' }, attachTo: document.body })
    await flushPromises()
    expect(chu('.ll-imp-rules')).toContain('Ca sáng, Ca chiều')
    expect(chu('.ll-imp-rules')).not.toContain('Ca đêm')
    const o = q('[data-testid="ll-import-file"]')
    Object.defineProperty(o, 'files', { value: [new File(['x'], 'lich.csv')], configurable: true })
    o.dispatchEvent(new Event('change'))
    await flushPromises()
    expect(chu('[data-testid="ll-import-loi-chung"]')).toBe('Chỉ nhận file Excel định dạng .xlsx.')
    expect(q('[data-testid="ll-import-kiem-tra"]').disabled).toBe(true)
  })

  it('file sai: bảng lỗi theo dòng / cột, chưa nhập gì', async () => {
    lichSvc.importExcel.mockRejectedValue(Object.assign(new Error('File có 2 lỗi'), {
      code: 'FILE_KHONG_HOP_LE',
      chiTiet: { tongDong: 3, soDongLoi: 2, loi: [
        { dong: 3, cot: 'Mã nhân viên (*)', thongBao: 'Không có nhân viên mã NV99.' },
        { dong: 4, cot: 'Ca làm (*)', thongBao: 'Chồng giờ với Ca sáng ở dòng 2.' },
      ] },
    }))
    await moVaChonFile()
    q('[data-testid="ll-import-kiem-tra"]').click()
    await flushPromises()
    expect(lichSvc.importExcel).toHaveBeenCalledWith(expect.any(File), { chiKiemTra: true })
    expect(chu('[data-testid="ll-import-loi"] .alert')).toContain('Có 2 lỗi ở 2/3 dòng')
    expect(qa('[data-testid="ll-import-loi"] tbody tr').map(oBang))
      .toEqual(['3 | Mã nhân viên (*) | Không có nhân viên mã NV99.', '4 | Ca làm (*) | Chồng giờ với Ca sáng ở dòng 2.'])
    q('[data-testid="ll-import-chon-lai"]').click()
    await flushPromises()
    expect(q('[data-testid="ll-import-chon"]')).toBeTruthy()
  })

  it('file đúng: xem trước rồi nhập, báo cho trang tải lại lịch', async () => {
    const xem = { daLuu: false, soLich: 1, xemTruoc: [{ dong: 2, maNhanVien: 'NV03', hoTen: 'Lê Quốc Bảo', ngayLam: '2026-10-26', tenCa: 'Ca sáng', gio: '07:00 - 12:00' }] }
    lichSvc.importExcel.mockResolvedValueOnce(xem).mockResolvedValueOnce({ ...xem, daLuu: true })
    const w = mount(LichImportModal, { props: { nhanViens: NHAN_VIENS, cas: CAS, thu2: '2026-10-26', homNay: '2026-10-23' }, attachTo: document.body })
    await flushPromises()
    const o = q('[data-testid="ll-import-file"]')
    Object.defineProperty(o, 'files', { value: [new File([new Uint8Array(10)], 'lich.xlsx')], configurable: true })
    o.dispatchEvent(new Event('change'))
    await flushPromises()
    q('[data-testid="ll-import-kiem-tra"]').click()
    await flushPromises()
    expect(qa('[data-testid="ll-import-xem"] tbody tr').map(oBang)).toEqual(['2 | Lê Quốc Bảo NV03 | 26/10/2026 | Ca sáng 07:00 - 12:00'])
    q('[data-testid="ll-import-nhap"]').click()
    await flushPromises()
    expect(lichSvc.importExcel).toHaveBeenLastCalledWith(expect.any(File), { chiKiemTra: false })
    expect(chu('[data-testid="ll-import-xong"] .alert')).toContain('Đã xếp 1 ca')
    expect(w.emitted('imported')).toHaveLength(1)
  })
})

/* ====================== Trợ lý AI cho nhân viên ====================== */

describe('Trợ lý AI nội bộ', () => {
  it('mở khung -> lời chào; hỏi -> trả lời kèm gợi ý và nút đi tới trang', async () => {
    troLySvc.chao.mockResolvedValue({ noiDung: 'Chào Long!', goiY: ['Doanh thu hôm nay'], lienKet: [] })
    troLySvc.hoi.mockResolvedValue({ noiDung: 'Doanh thu hôm nay: 2.000.000 ₫', goiY: [], lienKet: [{ nhan: 'Xem thống kê', duongDan: '/thong-ke' }] })
    mount(TroLyNoiBo, { attachTo: document.body, global: { components: { RouterLink: RouterLinkGia } } })
    q('[data-testid="tro-ly-nut"]').click()
    await flushPromises()
    expect(chu('.tl-tin')).toBe('Chào Long!')
    q('.tl-goi-y .tl-chip').click()
    await flushPromises()
    expect(troLySvc.hoi).toHaveBeenCalledWith('Doanh thu hôm nay')
    expect(qa('.tl-bong').map((b) => b.textContent.trim())).toEqual(['Chào Long!', 'Doanh thu hôm nay', 'Doanh thu hôm nay: 2.000.000 ₫'])
    const lk = q('.tl-lien-ket a')
    expect(lk.getAttribute('href')).toBe('/thong-ke')
    lk.click()
    await flushPromises()
    expect(q('[data-testid="tro-ly-khung"]')).toBeNull()
    q('[data-testid="tro-ly-nut"]').click()
    await flushPromises()
    expect(troLySvc.chao).toHaveBeenCalledTimes(1)
    expect(qa('.tl-bong')).toHaveLength(3)
  })

  it('Enter để gửi, không gửi câu rỗng; lỗi máy chủ hiện trong khung', async () => {
    troLySvc.chao.mockResolvedValue({ noiDung: 'Chào!', goiY: [], lienKet: [] })
    troLySvc.hoi.mockRejectedValue(new Error('Bạn cần đăng nhập.'))
    mount(TroLyNoiBo, { attachTo: document.body, global: { components: { RouterLink: RouterLinkGia } } })
    q('[data-testid="tro-ly-nut"]').click()
    await flushPromises()
    const o = q('.tl-soan textarea')
    o.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))
    await flushPromises()
    expect(troLySvc.hoi).not.toHaveBeenCalled()
    o.value = 'tồn kho astrox'
    o.dispatchEvent(new Event('input'))
    o.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))
    await flushPromises()
    expect(troLySvc.hoi).toHaveBeenCalledWith('tồn kho astrox')
    expect(q('.tl-bong.is-loi').textContent).toBe('Bạn cần đăng nhập.')
  })
})

/* ====================== Gợi ý AI từ ảnh ====================== */

/** Ảnh giả rong x cao: nền `nen`, ô vuông giữa màu `mau` chiếm khoảng 1/2 cạnh. */
function anh(mau, nen = [255, 255, 255], rong = 20, cao = 20) {
  const d = new Uint8ClampedArray(rong * cao * 4)
  for (let y = 0; y < cao; y++) {
    for (let x = 0; x < rong; x++) {
      const giua = x >= rong / 4 && x < (rong * 3) / 4 && y >= cao / 4 && y < (cao * 3) / 4
      const c = giua ? mau : nen
      d.set([...c, 255], (y * rong + x) * 4)
    }
  }
  return d
}

describe('Gợi ý AI từ ảnh', () => {
  it('tên màu phân loại -> màu chuẩn; màu RGB -> tên gần nhất', () => {
    expect(mauTheoTen('Đỏ')?.ten).toBe('Đỏ')
    expect(mauTheoTen('Xanh navy')?.ten).toBe('Xanh dương')
    expect(mauTheoTen('Xanh lá')?.ten).toBe('Xanh lá')
    expect(mauTheoTen('Đen nhám')?.ten).toBe('Đen')
    expect(mauTheoTen('Không áp dụng')).toBeNull()
    expect(tenMauGanNhat([200, 30, 40]).ten).toBe('Đỏ')
    expect(tenMauGanNhat([10, 10, 10]).ten).toBe('Đen')
    expect(tenMauGanNhat([30, 80, 200]).ten).toBe('Xanh dương')
  })

  it('tách nền trắng, nhận đúng màu sản phẩm', () => {
    const dt = docDacTrung(anh([210, 35, 42]), 20, 20)
    expect(dt.mauNoiBat[0].ten).toBe('Đỏ')
    expect(dt.mauNoiBat[0].tiLe).toBeCloseTo(1)
    expect(dt.soDiem).toBe(100)
    expect(dt.bieuDo.reduce((s, v) => s + v, 0)).toBeCloseTo(1)
    expect(docDacTrung(anh([255, 255, 255]), 20, 20)).toBeNull()
  })

  it('ảnh trong suốt (SVG sản phẩm) bỏ phần trong suốt', () => {
    const d = anh([25, 25, 28])
    for (let i = 0; i < d.length; i += 4) if (d[i] === 255) d[i + 3] = 0
    expect(docDacTrung(d, 20, 20).mauNoiBat.map((m) => m.ten)).toEqual(['Đen'])
  })

  it('xếp hạng: ảnh và màu phân loại giống nhất lên đầu, bỏ sản phẩm hết hàng', () => {
    const khach = docDacTrung(anh([205, 40, 45]), 20, 20)
    const dacTrungSp = { 1: docDacTrung(anh([210, 35, 42]), 20, 20), 2: docDacTrung(anh([30, 90, 210]), 20, 20), 3: null, 4: docDacTrung(anh([210, 35, 42]), 20, 20) }
    const sps = [
      { id: 1, tenSanPham: 'Vợt đỏ', bienThe: [{ tenMau: 'Đỏ', soLuongTon: 3 }] },
      { id: 2, tenSanPham: 'Vợt xanh', bienThe: [{ tenMau: 'Xanh', soLuongTon: 5 }] },
      { id: 3, tenSanPham: 'Áo không có ảnh', bienThe: [{ tenMau: 'Đỏ', soLuongTon: 2 }, { tenMau: 'Trắng', soLuongTon: 1 }] },
      { id: 4, tenSanPham: 'Vợt đỏ hết hàng', bienThe: [{ tenMau: 'Đỏ', soLuongTon: 0 }] },
    ]
    const kq = xepHangGoiY(khach, sps, dacTrungSp)
    expect(kq.map((k) => k.sp.id)).toEqual([1, 3])
    expect(kq[0].doGiong).toBeGreaterThan(80)
    expect(kq[0].mauKhop).toBe('Đỏ')
    expect(xepHangGoiY(null, sps, dacTrungSp)).toEqual([])
    expect(doGiongBieuDo(khach.bieuDo, dacTrungSp[2].bieuDo)).toBe(0)
    expect(doKhopMau(khach.mauNoiBat, ['Trắng', 'Đỏ']).ten).toBe('Đỏ')
  })
})
