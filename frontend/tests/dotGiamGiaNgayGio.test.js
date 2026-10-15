// Đợt giảm giá làm lại theo lời cô (video review): NGÀY GIỜ bắt đầu / kết thúc, trạng thái theo thời gian + "Trạng thái dự kiến",
// lỗi hiện dưới từng ô (thêm và sửa như nhau), CẢNH BÁO TRÙNG ĐỢT và demo "cộng dồn hay lấy mức cao nhất".
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi } from './helpersDanhMuc'
import { datSelect2 } from './helpersSelect2'
import { TRANG_THAI, daKetThuc, tinhTrangThai } from '../src/constants/dotGiamGia'
import { gopPhanTram, lichGiamGia, phanTramTaiThoiDiem } from '../src/utils/giamGia'
import { useToast } from '../src/composables/useToast'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

beforeEach(() => {
  document.body.innerHTML = ''
  useToast().toasts.splice(0)
})
afterEach(() => vi.useRealTimers())

// "Bây giờ" của các ca có kiểm tra quá khứ: 15/10/2026 10:00:00
function datGio(y = 2026, m = 10, d = 15, h = 10, mi = 0, s = 0) {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(y, m - 1, d, h, mi, s))
}

const dotMau = (bd, kt, over = {}) => ({ trangThai: 1, ngayBatDau: bd, ngayKetThuc: kt, ...over })

/* =================== Hàm thuần: trạng thái theo ngày giờ =================== */
describe('Trạng thái đợt giảm giá tính theo NGÀY GIỜ', () => {
  const d = dotMau('2099-10-04T18:00:00', '2099-10-04T20:30:59')

  it('chưa tới giờ bắt đầu = Sắp diễn ra; tới đúng giờ bắt đầu = Đang diễn ra (cùng một ngày)', () => {
    expect(tinhTrangThai(d, '2099-10-04T17:59:59')).toBe('SAP_DIEN_RA')
    expect(tinhTrangThai(d, '2099-10-04T18:00:00')).toBe('DANG_DIEN_RA')
  })

  it('giờ kết thúc tính tới hết phút: 20:30:59 còn diễn ra, 20:31:00 đã kết thúc', () => {
    expect(tinhTrangThai(d, '2099-10-04T20:30:59')).toBe('DANG_DIEN_RA')
    expect(tinhTrangThai(d, '2099-10-04T20:31:00')).toBe('DA_KET_THUC')
    expect(daKetThuc(d, '2099-10-04T20:30:59')).toBe(false)
    expect(daKetThuc(d, '2099-10-04T20:31:00')).toBe(true)
  })

  it('công tắc tắt thì luôn "Ngừng hoạt động" (kể cả đã quá giờ kết thúc)', () => {
    expect(tinhTrangThai({ ...d, trangThai: 0 }, '2099-10-04T19:00:00')).toBe('NGUNG_HOAT_DONG')
    expect(tinhTrangThai({ ...d, trangThai: 0 }, '2099-12-01T00:00:00')).toBe('NGUNG_HOAT_DONG')
  })

  it('đợt cũ lưu theo ngày (00:00:00 - 23:59:59) vẫn chạy đúng', () => {
    const cu = dotMau('2026-10-04T00:00:00', '2026-10-11T23:59:59')
    expect(tinhTrangThai(cu, '2026-10-03')).toBe('SAP_DIEN_RA')
    expect(tinhTrangThai(cu, '2026-10-11T23:59:59')).toBe('DANG_DIEN_RA')
    expect(tinhTrangThai(cu, '2026-10-12')).toBe('DA_KET_THUC')
  })

  it('đủ 4 nhãn trạng thái', () => {
    expect(Object.keys(TRANG_THAI)).toEqual(['DANG_DIEN_RA', 'SAP_DIEN_RA', 'DA_KET_THUC', 'NGUNG_HOAT_DONG'])
  })
})

/* =================== Hàm thuần: đợt chồng nhau (cao nhất / cộng dồn / trung bình) =================== */
describe('Lịch giảm giá khi đợt chồng nhau (ví dụ của cô: 10% và 15%)', () => {
  // Đợt A 10%: 1/10 08:00 - 10/10 17:00; đợt B 15%: 4/10 12:00 - 20/10 23:59 (cùng biến thể SPCT1)
  const A = { idDot: 1, maDot: 'DGG01', tenDot: 'A', phanTram: 10, ngayBatDau: '2099-10-01T08:00', ngayKetThuc: '2099-10-10T17:00' }
  const B = { idDot: 2, maDot: 'DGG02', tenDot: 'B', phanTram: 15, ngayBatDau: '2099-10-04T12:00', ngayKetThuc: '2099-10-20T23:59' }

  it('chia đúng 3 đoạn theo giờ: chỉ A | A và B chồng nhau | chỉ B', () => {
    const l = lichGiamGia([A, B], 'MAX')
    expect(l.map((k) => [k.tuLuc, k.denLuc, k.cacDot.map((x) => x.maDot), k.chongNhau])).toEqual([
      ['2099-10-01T08:00:00', '2099-10-04T11:59:59', ['DGG01'], false],
      ['2099-10-04T12:00:00', '2099-10-10T17:00:59', ['DGG01', 'DGG02'], true],
      ['2099-10-10T17:01:00', '2099-10-20T23:59:59', ['DGG02'], false],
    ])
  })

  it('đoạn chồng nhau: cao nhất 15%, cộng dồn 25%, trung bình cộng 12,5%; mức áp dụng theo cách đã chọn', () => {
    const chong = (cheDo) => lichGiamGia([A, B], cheDo)[1]
    expect(chong('MAX')).toMatchObject({ caoNhat: 15, congDon: 25, trungBinh: 12.5, apDung: 15 })
    expect(chong('CONG_DON')).toMatchObject({ caoNhat: 15, congDon: 25, trungBinh: 12.5, apDung: 25 })
    expect(chong('TRUNG_BINH')).toMatchObject({ caoNhat: 15, congDon: 25, trungBinh: 12.5, apDung: 12.5 })
    // đoạn không chồng nhau thì cách nào cũng cho đúng % của đợt
    expect(lichGiamGia([A, B], 'CONG_DON').map((k) => k.apDung)).toEqual([10, 25, 15])
  })

  it('cộng dồn không bao giờ vượt 100%', () => {
    const l = lichGiamGia([{ ...A, phanTram: 70 }, { ...B, phanTram: 60 }], 'CONG_DON')
    expect(l[1]).toMatchObject({ congDon: 100, apDung: 100 })
    expect(gopPhanTram([70, 60], 'CONG_DON')).toBe(100)
  })

  it('gopPhanTram: rỗng = 0; một đợt thì cách nào cũng bằng chính nó', () => {
    expect(gopPhanTram([], 'MAX')).toBe(0)
    for (const cheDo of ['MAX', 'CONG_DON', 'TRUNG_BINH']) expect(gopPhanTram([20], cheDo)).toBe(20)
    expect(gopPhanTram([10, 15, 20], 'TRUNG_BINH')).toBe(15)
    expect(gopPhanTram([10, 15, 20], 'MAX')).toBe(20)
  })

  it('hai đợt cách xa nhau: khoảng trống không có đoạn nào', () => {
    const l = lichGiamGia([A, { ...B, ngayBatDau: '2099-11-01T00:00', ngayKetThuc: '2099-11-05T12:00' }], 'MAX')
    expect(l.map((k) => k.cacDot.length)).toEqual([1, 1])
    expect(l[0].denLuc).toBe('2099-10-10T17:00:59')
    expect(l[1].tuLuc).toBe('2099-11-01T00:00:00')
  })

  it('giới hạn theo khoảng thời gian của đợt đang soạn: cắt các đoạn nằm ngoài', () => {
    const l = lichGiamGia([A, B], 'MAX', { batDau: '2099-10-05T00:00', ketThuc: '2099-10-06T23:59' })
    expect(l).toHaveLength(1)
    expect(l[0]).toMatchObject({ tuLuc: '2099-10-05T00:00:00', denLuc: '2099-10-06T23:59:59', chongNhau: true, apDung: 15 })
  })

  it('mức giảm tại một thời điểm: trước giờ bắt đầu 0, trong đoạn chồng nhau theo cách đã chọn, sau khi kết thúc 0', () => {
    const tai = (t, cheDo = 'MAX') => phanTramTaiThoiDiem([A, B], cheDo, t)
    expect(tai('2099-10-01T07:59:59')).toBe(0)
    expect(tai('2099-10-01T08:00:00')).toBe(10)
    expect(tai('2099-10-04T12:00:00')).toBe(15)
    expect(tai('2099-10-04T12:00:00', 'CONG_DON')).toBe(25)
    expect(tai('2099-10-10T17:00:59', 'TRUNG_BINH')).toBe(12.5)
    expect(tai('2099-10-10T17:01:00')).toBe(15)
    expect(tai('2099-10-21T00:00:00')).toBe(0)
  })

  it('dữ liệu đợt cũ chỉ có ngày vẫn tính được (bắt đầu 00:00, kết thúc hết ngày)', () => {
    const l = lichGiamGia([{ idDot: 1, maDot: 'X', phanTram: 5, ngayBatDau: '2099-10-01', ngayKetThuc: '2099-10-03' }], 'MAX')
    expect(l).toEqual([expect.objectContaining({ tuLuc: '2099-10-01T00:00:00', denLuc: '2099-10-03T23:59:59', apDung: 5 })])
  })
})

/* =================== Trang thêm / sửa đợt giảm giá =================== */
const BIEN_THE = [
  { id: 11, ma: 'SP001-DEN', idSanPham: 1, idMauSac: 1, idTrongLuong: 1, idChuVi: 1, giaBan: 3000000, soLuongTon: 5, hoatDong: true },
  { id: 12, ma: 'SP001-DO', idSanPham: 1, idMauSac: 2, idTrongLuong: 1, idChuVi: 1, giaBan: 3200000, soLuongTon: 5, hoatDong: true },
  { id: 21, ma: 'SP002-DEN', idSanPham: 2, idMauSac: 1, idTrongLuong: 92, idChuVi: 93, giaBan: 150000, soLuongTon: 5, hoatDong: true },
]

/** Dựng backend giả: `du` ghi đè từng URL (giá trị hoặc hàm). */
async function mo({ duongDan = '/dot-giam-gia/them', du = {} } = {}) {
  const macDinh = {
    '/bien-the-san-pham': BIEN_THE,
    '/dot-giam-gia/ma-moi': { ma: 'DGG09' },
    '/dot-giam-gia': [],
  }
  mock = mockApi({
    get: (url) => {
      const nguon = url in du ? du[url] : macDinh[url]
      return typeof nguon === 'function' ? nguon(url) : nguon
    },
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/dot-giam-gia', component: { template: '<div/>' } },
      { path: '/dot-giam-gia/them', component: { template: '<div/>' } },
      { path: '/dot-giam-gia/:id', component: { template: '<div/>' } },
    ],
  })
  router.push(duongDan)
  await router.isReady()
  const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaFormPage.vue')).default
  const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
  await flushPromises()
  return { w, router }
}
const giaTri = (w, sel) => w.get(sel).element.value
const dongSanPham = (w, ten) => w.findAll('.dgg-table tbody tr').find((tr) => tr.text().includes(ten))
const chonSanPham = async (w, ten) => {
  await dongSanPham(w, ten).get('input[type=checkbox]').setValue(true)
  await flushPromises()
}
async function datNgayGio(w, id, ngay, gio) {
  if (ngay) await w.get(`#${id}`).setValue(ngay)
  if (gio) await w.get(`#${id}-gio`).setValue(gio)
}
const duKien = (w) => w.get('[data-testid=dgg-trang-thai-du-kien]').text()
const loiO = (w, id) => w.find(`#${id}-msg`).text()
const xacNhanLuu = async (w) => {
  await w.get('form').trigger('submit')
  await flushPromises()
  ;[...document.body.querySelectorAll('button')].find((b) => /Xác nhận|Đồng ý|Có/.test(b.textContent))?.click()
  await flushPromises()
}

describe('Form đợt giảm giá: ngày giờ + trạng thái dự kiến', () => {
  it('thêm mới: mặc định bắt đầu = bây giờ (ngày + giờ phút), kết thúc để trống, chưa có trạng thái dự kiến', async () => {
    datGio()
    const { w } = await mo()
    expect(giaTri(w, '#dgg-tu-ngay')).toBe('15/10/2026')
    expect(giaTri(w, '#dgg-tu-ngay-gio')).toBe('10:00')
    expect(giaTri(w, '#dgg-den-ngay')).toBe('')
    expect(w.text()).toContain('Ngày giờ bắt đầu')
    expect(w.text()).toContain('Ngày giờ kết thúc')
    expect(duKien(w)).toContain('Chọn ngày giờ bắt đầu và kết thúc hợp lệ')
    w.unmount()
  })

  it('"Trạng thái dự kiến" cập nhật ngay khi chọn giờ: bắt đầu bây giờ = Đang diễn ra; bắt đầu về sau = Sắp diễn ra (kèm đếm ngược)', async () => {
    datGio()
    const { w } = await mo()
    await datNgayGio(w, 'dgg-den-ngay', '2099-12-31', '17:00')
    expect(w.get('[data-testid=dgg-trang-thai-nhan]').text()).toBe('Đang diễn ra')
    expect(duKien(w)).toContain('kết thúc lúc 31/12/2099 17:00')
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-20', '08:30')
    expect(w.get('[data-testid=dgg-trang-thai-nhan]').text()).toBe('Sắp diễn ra')
    expect(duKien(w)).toContain('Bắt đầu lúc 20/10/2099 08:30, còn')
    w.unmount()
  })

  it('kết thúc trước hoặc cùng phút với bắt đầu: báo ngay dưới ô kết thúc, không có trạng thái dự kiến', async () => {
    datGio()
    const { w } = await mo()
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-20', '08:30')
    await datNgayGio(w, 'dgg-den-ngay', '2099-10-20', '07:00')
    expect(loiO(w, 'dgg-den-ngay')).toBe('Ngày giờ kết thúc phải sau ngày giờ bắt đầu.')
    expect(duKien(w)).toContain('Chọn ngày giờ bắt đầu và kết thúc hợp lệ')
    await datNgayGio(w, null, null)
    await w.get('#dgg-den-ngay-gio').setValue('08:30') // cùng phút với bắt đầu vẫn sai
    expect(loiO(w, 'dgg-den-ngay')).toBe('Ngày giờ kết thúc phải sau ngày giờ bắt đầu.')
    await w.get('#dgg-den-ngay-gio').setValue('08:31') // hơn 1 phút thì hợp lệ
    expect(w.find('#dgg-den-ngay-msg').exists()).toBe(false)
    w.unmount()
  })

  it('bắt đầu ở quá khứ bị chặn, nhưng cho lệch tối đa 5 phút (bấm "Bây giờ" rồi lưu không bị báo lỗi)', async () => {
    datGio() // 10:00
    const { w } = await mo()
    await datNgayGio(w, 'dgg-den-ngay', '2099-12-31', '17:00')
    await w.get('#dgg-tu-ngay-gio').setValue('09:00')
    expect(loiO(w, 'dgg-tu-ngay')).toBe('Ngày giờ bắt đầu không được ở quá khứ.')
    await w.get('#dgg-tu-ngay-gio').setValue('09:56')
    expect(w.find('#dgg-tu-ngay-msg').exists()).toBe(false)
    await w.get('#dgg-tu-ngay-gio').setValue('09:54') // quá 5 phút
    expect(loiO(w, 'dgg-tu-ngay')).toBe('Ngày giờ bắt đầu không được ở quá khứ.')
    w.unmount()
  })

  it('kết thúc ở quá khứ (dù bắt đầu còn trong dung sai): báo lỗi ở ô kết thúc, không báo ở ô bắt đầu', async () => {
    datGio() // 10:00
    const { w } = await mo()
    await w.get('#dgg-tu-ngay-gio').setValue('09:57')
    await datNgayGio(w, 'dgg-den-ngay', '2026-10-15', '09:58')
    expect(loiO(w, 'dgg-den-ngay')).toBe('Ngày giờ kết thúc không được ở quá khứ.')
    expect(w.find('#dgg-tu-ngay-msg').exists()).toBe(false)
    w.unmount()
  })

  it('bấm lưu form trống: hiện lỗi dưới TỪNG ô cần nhập, không gọi API lưu', async () => {
    datGio()
    const { w } = await mo()
    await w.get('form').trigger('submit')
    await flushPromises()
    expect(loiO(w, 'dgg-ten')).toBe('Vui lòng nhập tên đợt giảm giá.')
    expect(loiO(w, 'dgg-gia-tri')).toBe('Vui lòng nhập giá trị giảm.')
    expect(loiO(w, 'dgg-den-ngay')).toBe('Chọn ngày giờ kết thúc.')
    expect(w.text()).toContain('Vui lòng chọn ít nhất một sản phẩm / biến thể áp dụng.')
    expect(w.find('#dgg-tu-ngay-msg').exists()).toBe(false) // bắt đầu đã có giá trị mặc định
    expect(mock.calls.post.length).toBe(0)
    w.unmount()
  })

  it('chưa đụng tới ô nào thì chưa hiện lỗi (mở form sạch sẽ)', async () => {
    datGio()
    const { w } = await mo()
    expect(w.findAll('.ad-error').length).toBe(0)
    expect(w.findAll('.is-invalid').length).toBe(0)
    w.unmount()
  })

  it.each([
    ['0', 'Giá trị giảm phải là số nguyên từ 1 đến 100.'],
    ['101', 'Giá trị giảm phải là số nguyên từ 1 đến 100.'],
    ['-5', 'Giá trị giảm phải là số nguyên từ 1 đến 100.'],
    ['12.5', 'Giá trị giảm phải là số nguyên từ 1 đến 100.'],
    ['', 'Vui lòng nhập giá trị giảm.'],
  ])('giá trị giảm "%s" bị chặn: %s', async (nhap, loi) => {
    datGio()
    const { w } = await mo()
    await w.get('#dgg-gia-tri').setValue(nhap)
    await w.get('#dgg-gia-tri').trigger('blur')
    expect(loiO(w, 'dgg-gia-tri')).toBe(loi)
    w.unmount()
  })

  it.each(['1', '50', '100'])('giá trị giảm %s%% hợp lệ', async (nhap) => {
    datGio()
    const { w } = await mo()
    await w.get('#dgg-gia-tri').setValue(nhap)
    expect(w.find('#dgg-gia-tri-msg').exists()).toBe(false)
    w.unmount()
  })

  it('tên quá 255 ký tự và mô tả quá 500 ký tự: báo ngay dưới ô (mô tả không còn chỉ hiện thông báo nổi)', async () => {
    datGio()
    const { w } = await mo()
    await w.get('#dgg-ten').setValue('A'.repeat(256))
    expect(loiO(w, 'dgg-ten')).toBe('Tên đợt giảm giá tối đa 255 ký tự.')
    await w.get('#dgg-mo-ta').setValue('B'.repeat(501))
    expect(loiO(w, 'dgg-mo-ta')).toBe('Mô tả tối đa 500 ký tự.')
    await w.get('#dgg-mo-ta').setValue('B'.repeat(10))
    expect(w.text()).toContain('10/500 ký tự')
    w.unmount()
  })

  it('lưu đủ thông tin: gửi ngày GIỜ (bắt đầu :00, kết thúc mặc định 23:59), mã lấy từ hệ thống, đúng danh sách biến thể', async () => {
    datGio()
    const { w, router } = await mo()
    await chonSanPham(w, 'Astrox 99')
    await w.get('#dgg-ten').setValue('  Sale cuối năm  ')
    await w.get('#dgg-gia-tri').setValue('15')
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-20', '08:30')
    await datNgayGio(w, 'dgg-den-ngay', '2099-10-31')
    await xacNhanLuu(w)
    const luu = mock.calls.post.find((c) => c[0] === '/dot-giam-gia')
    expect(luu[1]).toMatchObject({
      maDot: 'DGG09',
      tenDot: 'Sale cuối năm',
      phanTramGiamDot: 15,
      ngayBatDau: '2099-10-20T08:30:00',
      ngayKetThuc: '2099-10-31T23:59:00',
      idBienThe: [11, 12],
    })
    expect(router.currentRoute.value.path).toBe('/dot-giam-gia')
    w.unmount()
  })
})

describe('Form SỬA đợt giảm giá: cùng bộ luật như thêm, thêm quy tắc "đã bắt đầu thì khóa giờ bắt đầu"', () => {
  const DOT = (over = {}) => ({
    id: 5, maDot: 'DGG05', tenDot: 'Sale tháng 10', phanTramGiamDot: 10, moTa: 'Mô tả cũ', trangThai: 1,
    ngayBatDau: '2026-10-10T08:00:00', ngayKetThuc: '2026-10-20T17:00:59', ...over,
  })
  const moSua = (dot) => mo({
    duongDan: '/dot-giam-gia/5',
    du: { '/dot-giam-gia/5': dot, '/dot-giam-gia-chi-tiet/campaign/5': [{ id: 1, idSanPhamChiTiet: 11, phanTramGiamBienThe: 10, trangThai: 1 }] },
  })

  it('đợt ĐANG diễn ra: nạp đúng dữ liệu, khóa ô ngày giờ bắt đầu (có ghi chú), ô kết thúc vẫn sửa được, mở form chưa báo lỗi', async () => {
    datGio()
    const { w } = await moSua(DOT())
    expect(w.get('[data-testid=dgg-trang-thai-hien-tai]').text()).toBe('Đang diễn ra')
    expect(giaTri(w, '#dgg-ten')).toBe('Sale tháng 10')
    expect(giaTri(w, '#dgg-tu-ngay')).toBe('10/10/2026')
    expect(giaTri(w, '#dgg-tu-ngay-gio')).toBe('08:00')
    expect(giaTri(w, '#dgg-den-ngay')).toBe('20/10/2026')
    expect(giaTri(w, '#dgg-den-ngay-gio')).toBe('17:00')
    expect(w.get('#dgg-tu-ngay').attributes('disabled')).toBeDefined()
    expect(w.get('#dgg-tu-ngay-gio').attributes('disabled')).toBeDefined()
    expect(w.get('#dgg-den-ngay').attributes('disabled')).toBeUndefined()
    expect(w.get('[data-testid=dgg-khoa-bat-dau]').text()).toBe('Đợt đã bắt đầu nên không đổi được ngày giờ bắt đầu.')
    expect(w.findAll('.ad-error').length).toBe(0)
    w.unmount()
  })

  it('đợt đang diễn ra: giữ nguyên ngày giờ rồi lưu được; gửi lại đúng mốc bắt đầu cũ', async () => {
    datGio()
    const { w } = await moSua(DOT())
    await w.get('#dgg-ten').setValue('Sale tháng 10 (gia hạn)')
    await xacNhanLuu(w)
    const luu = mock.calls.put.find((c) => c[0] === '/dot-giam-gia/5')
    expect(luu[1]).toMatchObject({ tenDot: 'Sale tháng 10 (gia hạn)', ngayBatDau: '2026-10-10T08:00:00', ngayKetThuc: '2026-10-20T17:00:00', idBienThe: [11] })
    w.unmount()
  })

  it('đợt đang diễn ra: dời giờ kết thúc về quá khứ thì báo lỗi; kéo dài ra thì hợp lệ', async () => {
    datGio()
    const { w } = await moSua(DOT())
    await datNgayGio(w, 'dgg-den-ngay', '2026-10-14', '09:00')
    expect(loiO(w, 'dgg-den-ngay')).toBe('Ngày giờ kết thúc mới không được ở quá khứ.')
    await datNgayGio(w, 'dgg-den-ngay', '2026-11-30', '12:00')
    expect(w.find('#dgg-den-ngay-msg').exists()).toBe(false)
    expect(duKien(w)).toContain('Đang giảm giá')
    w.unmount()
  })

  it('đợt SẮP diễn ra: sửa được cả ngày giờ bắt đầu, nhưng không được dời về quá khứ', async () => {
    datGio()
    const { w } = await moSua(DOT({ ngayBatDau: '2026-10-18T08:00:00', ngayKetThuc: '2026-10-25T17:00:59' }))
    expect(w.get('[data-testid=dgg-trang-thai-hien-tai]').text()).toBe('Sắp diễn ra')
    expect(w.get('#dgg-tu-ngay').attributes('disabled')).toBeUndefined()
    expect(w.find('[data-testid=dgg-khoa-bat-dau]').exists()).toBe(false)
    await datNgayGio(w, 'dgg-tu-ngay', '2026-10-14', '08:00')
    expect(loiO(w, 'dgg-tu-ngay')).toBe('Ngày giờ bắt đầu mới không được ở quá khứ.')
    await datNgayGio(w, 'dgg-tu-ngay', '2026-10-19', '09:15')
    expect(w.find('#dgg-tu-ngay-msg').exists()).toBe(false)
    expect(duKien(w)).toContain('Sắp diễn ra')
    w.unmount()
  })

  it('đợt ĐÃ kết thúc: vẫn sửa được tên / mô tả (giữ nguyên ngày giờ không bị báo quá khứ); đổi kết thúc sang quá khứ khác thì báo lỗi', async () => {
    datGio()
    const { w } = await moSua(DOT({ ngayBatDau: '2026-09-01T08:00:00', ngayKetThuc: '2026-09-10T17:00:59' }))
    expect(w.get('[data-testid=dgg-trang-thai-hien-tai]').text()).toBe('Đã kết thúc')
    await w.get('#dgg-ten').setValue('Sale tháng 9')
    expect(w.findAll('.ad-error').length).toBe(0)
    await datNgayGio(w, 'dgg-den-ngay', '2026-09-12', '10:00')
    expect(loiO(w, 'dgg-den-ngay')).toBe('Ngày giờ kết thúc mới không được ở quá khứ.')
    w.unmount()
  })

  it('đợt đang NGỪNG hoạt động (công tắc tắt): hiện "Ngừng hoạt động" và trạng thái dự kiến cũng vậy', async () => {
    datGio()
    const { w } = await moSua(DOT({ trangThai: 0 }))
    expect(w.get('[data-testid=dgg-trang-thai-hien-tai]').text()).toBe('Ngừng hoạt động')
    expect(w.get('[data-testid=dgg-trang-thai-nhan]').text()).toBe('Ngừng hoạt động')
    expect(duKien(w)).toContain('Công tắc của đợt đang tắt')
    w.unmount()
  })

  it('đợt cũ lưu theo ngày (00:00:00 - 23:59:59) vẫn mở sửa được, hiện 00:00 và 23:59', async () => {
    datGio()
    const { w } = await moSua(DOT({ ngayBatDau: '2026-10-10T00:00:00', ngayKetThuc: '2026-10-20T23:59:59' }))
    expect(giaTri(w, '#dgg-tu-ngay-gio')).toBe('00:00')
    expect(giaTri(w, '#dgg-den-ngay-gio')).toBe('23:59')
    w.unmount()
  })
})

/* =================== Thẻ "Khi nhiều đợt chồng nhau" =================== */
describe('Thẻ trùng đợt (DotGiamGiaChongDotCard): cảnh báo + cách tính + demo', () => {
  const BT = (id, ma, giaBan, ten = 'Vợt A') => ({ id, ma, giaBan, sp: { ten }, mau: 'Đen', tl: '4U', cv: 'G5' })
  const BIEN_THES = [BT(1, 'SPCT1', 1000000), BT(3, 'SPCT3', 2000000, 'Vợt B')]
  const DOT_MOI = { idDot: 0, maDot: 'DGG09', tenDot: 'Đợt mới', phanTram: 15, ngayBatDau: '2099-10-04T12:00', ngayKetThuc: '2099-10-20T23:59' }
  const KHAC = (over = {}) => ({ idBienThe: 1, idDot: 7, maDot: 'DGG07', tenDot: 'Đợt A', phanTram: 10, ngayBatDau: '2099-10-01T08:00:00', ngayKetThuc: '2099-10-10T17:00:59', ...over })
  const montar = async (props) => {
    const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaChongDotCard.vue')).default
    return mount(Comp, { props: { dot: DOT_MOI, bienThes: BIEN_THES, mucKhac: [KHAC()], cheDo: 'MAX', ...props }, attachTo: document.body })
  }
  const hang = (w, idBt) => w.findAll(`[data-testid=dgg-lich-${idBt}] li`)

  it('có đợt khác trùng thời gian: cảnh báo liệt kê đợt đó, số biến thể trùng và cách tính đang dùng', async () => {
    const w = await montar()
    const canhBao = w.get('[data-testid=dgg-canh-bao-trung]')
    expect(canhBao.text()).toContain('Có 1 đợt giảm giá khác trùng thời gian với đợt này trên 1 biến thể đã chọn')
    expect(canhBao.get('[data-testid=dgg-dot-trung-7]').text()).toContain('DGG07')
    expect(canhBao.get('[data-testid=dgg-dot-trung-7]').text()).toContain('giảm 10%')
    expect(canhBao.get('[data-testid=dgg-dot-trung-7]').text()).toContain('01/10/2099 08:00 → 10/10/2099 17:00')
    expect(canhBao.get('[data-testid=dgg-canh-bao-che-do]').text()).toBe('Lấy mức cao nhất')
    expect(w.find('[data-testid=dgg-khong-trung]').exists()).toBe(false)
    w.unmount()
  })

  it('lịch của biến thể trùng: chỉ phần nằm trong đợt đang soạn, đoạn chồng nhau hiện đủ 3 cách và giá sau giảm theo cách đang dùng (cao nhất)', async () => {
    const w = await montar()
    const h = hang(w, 1)
    expect(h).toHaveLength(2) // đoạn 01/10-04/10 nằm ngoài đợt đang soạn nên không hiện
    expect(h[0].attributes('data-chong-nhau')).toBe('true')
    expect(h[0].text()).toContain('04/10/2099 12:00 → 10/10/2099 17:00')
    expect(h[0].text()).toContain('DGG07 (10%) + Đợt này (15%)')
    expect(h[0].get('[data-che-do=MAX]').text()).toBe('Cao nhất 15%')
    expect(h[0].get('[data-che-do=CONG_DON]').text()).toBe('Cộng dồn 25%')
    expect(h[0].get('[data-che-do=TRUNG_BINH]').text()).toBe('Trung bình 12,5%')
    expect(h[0].get('[data-che-do=MAX]').classes()).toContain('on')
    expect(h[0].get('[data-che-do=CONG_DON]').classes()).not.toContain('on')
    expect(h[0].get('.dgg-lich-gia').text()).toBe('850.000 ₫') // 1.000.000 giảm 15%
    // đoạn sau chỉ còn đợt đang soạn
    expect(h[1].attributes('data-chong-nhau')).toBe('false')
    expect(h[1].text()).toContain('15%')
    w.unmount()
  })

  it('đổi cách tính sang CỘNG DỒN: nhãn, ô tô xanh và giá sau giảm đổi theo (25% -> 750.000 ₫)', async () => {
    const w = await montar({ cheDo: 'CONG_DON' })
    expect(w.get('[data-testid=dgg-che-do-ten]').text()).toBe('Cộng dồn')
    const h = hang(w, 1)[0]
    expect(h.get('[data-che-do=CONG_DON]').classes()).toContain('on')
    expect(h.get('[data-che-do=MAX]').classes()).not.toContain('on')
    expect(h.get('.dgg-lich-gia').text()).toBe('750.000 ₫')
    w.unmount()
  })

  it('đổi cách tính sang TRUNG BÌNH CỘNG: 12,5% -> 875.000 ₫', async () => {
    const w = await montar({ cheDo: 'TRUNG_BINH' })
    expect(w.get('[data-testid=dgg-che-do-ten]').text()).toBe('Trung bình cộng')
    const h = hang(w, 1)[0]
    expect(h.get('[data-che-do=TRUNG_BINH]').classes()).toContain('on')
    expect(h.get('.dgg-lich-gia').text()).toBe('875.000 ₫')
    w.unmount()
  })

  it('mô tả cách tính lấy từ backend nếu có, không thì dùng câu mặc định', async () => {
    const w1 = await montar({ moTaCheDo: 'Câu mô tả riêng từ backend.' })
    expect(w1.get('[data-testid=dgg-che-do]').text()).toContain('Câu mô tả riêng từ backend.')
    w1.unmount()
    const w2 = await montar({ cheDo: 'CONG_DON' })
    expect(w2.get('[data-testid=dgg-che-do]').text()).toContain('CỘNG DỒN mức giảm')
    w2.unmount()
  })

  it('đợt khác chạy TRƯỚC khi đợt này bắt đầu (không chạm nhau): không cảnh báo, báo rõ không trùng', async () => {
    const w = await montar({ mucKhac: [KHAC({ ngayBatDau: '2099-09-01T00:00:00', ngayKetThuc: '2099-10-04T11:59:59' })] })
    expect(w.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(false)
    expect(w.get('[data-testid=dgg-khong-trung]').text()).toContain('Không có biến thể nào trùng thời gian')
    w.unmount()
  })

  it('đợt khác kết thúc đúng 1 giây trước giờ bắt đầu thì không trùng; chạm nhau 1 giây thì trùng', async () => {
    const khongTrung = await montar({ mucKhac: [KHAC({ ngayBatDau: '2099-10-01T00:00:00', ngayKetThuc: '2099-10-04T11:59:59' })] })
    expect(khongTrung.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(false)
    khongTrung.unmount()
    const trung = await montar({ mucKhac: [KHAC({ ngayBatDau: '2099-10-01T00:00:00', ngayKetThuc: '2099-10-04T12:00:00' })] })
    expect(trung.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(true)
    trung.unmount()
  })

  it('đợt khác nằm trên biến thể khác (không được chọn): không tính là trùng', async () => {
    const w = await montar({ mucKhac: [KHAC({ idBienThe: 99 })] })
    expect(w.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(false)
    expect(w.find('[data-testid=dgg-khong-trung]').exists()).toBe(true)
    w.unmount()
  })

  it('chính đợt đang sửa nằm trong danh sách đợt khác thì bị loại, không tự cảnh báo trùng với mình', async () => {
    const w = await montar({ dot: { ...DOT_MOI, idDot: 7 }, mucKhac: [KHAC({ idDot: 7 })] })
    expect(w.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(false)
    w.unmount()
  })

  it('cùng mức giảm và cùng thời gian với đợt khác: gắn nhãn "Cùng mức giảm và cùng thời gian"', async () => {
    const w = await montar({ mucKhac: [KHAC({ phanTram: 15, ngayBatDau: '2099-10-04T12:00:00', ngayKetThuc: '2099-10-20T23:59:59' })] })
    expect(w.get('[data-testid=dgg-giong-het]').text()).toBe('Cùng mức giảm và cùng thời gian')
    w.unmount()
  })

  it('nhiều đợt khác cùng trùng: liệt kê từng đợt và đếm số biến thể bị trùng của mỗi đợt', async () => {
    const w = await montar({
      mucKhac: [KHAC(), KHAC({ idBienThe: 3, phanTram: 20 }), KHAC({ idBienThe: 3, idDot: 8, maDot: 'DGG08', tenDot: 'Đợt C', phanTram: 5, ngayBatDau: '2099-10-08T00:00:00', ngayKetThuc: '2099-10-09T00:00:00' })],
    })
    expect(w.get('[data-testid=dgg-canh-bao-trung]').text()).toContain('Có 2 đợt giảm giá khác trùng thời gian với đợt này trên 2 biến thể')
    expect(w.get('[data-testid=dgg-dot-trung-7]').text()).toContain('trùng 2 biến thể')
    expect(w.get('[data-testid=dgg-dot-trung-8]').text()).toContain('trùng 1 biến thể')
    // SPCT3 nằm trong 3 đợt: đoạn 08/10 00:00 - 09/10 00:00 có cả ba đợt chồng nhau (20% + 5% + 15%)
    const doanBa = hang(w, 3).find((li) => li.text().includes('DGG07') && li.text().includes('DGG08'))
    expect(doanBa).toBeTruthy()
    expect(doanBa.get('[data-che-do=CONG_DON]').text()).toBe('Cộng dồn 40%') // 20 + 5 + 15
    w.unmount()
  })

  it('chưa nhập đủ thông tin đợt (dot = null) hoặc chưa chọn biến thể: chỉ nhắc, không cảnh báo', async () => {
    const w1 = await montar({ dot: null })
    expect(w1.find('[data-testid=dgg-trung-chua-du]').exists()).toBe(true)
    expect(w1.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(false)
    w1.unmount()
    const w2 = await montar({ bienThes: [] })
    expect(w2.find('[data-testid=dgg-trung-chua-chon]').exists()).toBe(true)
    w2.unmount()
  })

  it('quy tắc luôn hiện: đợt giảm trước, phiếu tính sau; mỗi đơn 1 phiếu, không cộng dồn phiếu', async () => {
    const w = await montar({ dot: null })
    expect(w.text()).toContain('Phiếu giảm giá tính sau đợt')
    expect(w.text()).toContain('mỗi đơn chỉ dùng 1 phiếu')
    expect(w.text()).toContain('app.giam-gia.che-do-chong-dot')
    w.unmount()
  })

  it('xem thêm / thu gọn khi có hơn 6 biến thể trùng', async () => {
    const nhieu = Array.from({ length: 8 }, (_, i) => BT(100 + i, `SPCT${100 + i}`, 1000000))
    const w = await montar({ bienThes: nhieu, mucKhac: nhieu.map((b) => KHAC({ idBienThe: b.id })) })
    expect(w.findAll('.dgg-lich-item')).toHaveLength(6)
    const nut = w.findAll('button').find((b) => b.text().includes('Xem thêm 2 biến thể'))
    expect(nut).toBeTruthy()
    await nut.trigger('click')
    expect(w.findAll('.dgg-lich-item')).toHaveLength(8)
    await w.findAll('button').find((b) => b.text() === 'Thu gọn').trigger('click')
    expect(w.findAll('.dgg-lich-item')).toHaveLength(6)
    w.unmount()
  })
})

describe('Ví dụ chạy thử trong thẻ trùng đợt (demo cộng dồn / cao nhất / trung bình)', () => {
  const montar = async (cheDo = 'MAX') => {
    const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaChongDotCard.vue')).default
    return mount(Comp, { props: { dot: null, bienThes: [], mucKhac: [], cheDo }, attachTo: document.body })
  }
  const o = (w, khoa) => w.get(`[data-testid=dgg-vd-${khoa}]`)

  it('mặc định giá 3.000.000 ₫, đợt A 10%, đợt B 15%: cao nhất 15% / cộng dồn 25% / trung bình 12,5%', async () => {
    const w = await montar()
    expect(o(w, 'MAX').text()).toContain('15%')
    expect(o(w, 'MAX').text()).toContain('2.550.000 ₫')
    expect(o(w, 'CONG_DON').text()).toContain('25%')
    expect(o(w, 'CONG_DON').text()).toContain('2.250.000 ₫')
    expect(o(w, 'TRUNG_BINH').text()).toContain('12,5%')
    expect(o(w, 'TRUNG_BINH').text()).toContain('2.625.000 ₫')
    w.unmount()
  })

  it('cách đang dùng được tô xanh + nhãn "đang dùng" (chỉ một ô)', async () => {
    for (const [cheDo, khac1, khac2] of [['MAX', 'CONG_DON', 'TRUNG_BINH'], ['CONG_DON', 'MAX', 'TRUNG_BINH'], ['TRUNG_BINH', 'MAX', 'CONG_DON']]) {
      const w = await montar(cheDo)
      expect(o(w, cheDo).classes()).toContain('is-dung')
      expect(o(w, cheDo).text()).toContain('đang dùng')
      for (const k of [khac1, khac2]) {
        expect(o(w, k).classes()).not.toContain('is-dung')
        expect(o(w, k).text()).not.toContain('đang dùng')
      }
      w.unmount()
    }
  })

  it('sửa giá và % của hai đợt thì kết quả đổi theo ngay; cộng dồn không vượt 100%', async () => {
    const w = await montar('CONG_DON')
    await w.get('#dgg-vd-dot-a').setValue('70')
    await w.get('#dgg-vd-dot-b').setValue('60')
    expect(o(w, 'MAX').text()).toContain('70%')
    expect(o(w, 'CONG_DON').text()).toContain('100%')
    expect(o(w, 'CONG_DON').text()).toContain('0 ₫')
    expect(o(w, 'TRUNG_BINH').text()).toContain('65%')
    await w.get('#dgg-vd-gia').setValue('1.000.000')
    expect(o(w, 'MAX').text()).toContain('300.000 ₫')
    w.unmount()
  })

  it('nhập % ngoài khoảng (âm, trên 100, bỏ trống) thì kẹp về 0 - 100, không hiện số vô nghĩa', async () => {
    const w = await montar()
    await w.get('#dgg-vd-dot-a').setValue('150')
    await w.get('#dgg-vd-dot-b').setValue('-20')
    expect(o(w, 'MAX').text()).toContain('100%')
    expect(o(w, 'CONG_DON').text()).toContain('100%')
    await w.get('#dgg-vd-dot-a').setValue('')
    expect(o(w, 'MAX').text()).toContain('0%')
    w.unmount()
  })
})

/* =================== Form + thẻ trùng đợt ghép lại =================== */
describe('Form đợt giảm giá cảnh báo trùng đợt thật theo biến thể đã chọn và ngày giờ đang nhập', () => {
  const KHAC = (over = {}) => ({ idBienThe: 11, idDot: 7, maDot: 'DGG07', tenDot: 'Yonex Festival', phanTram: 10, ngayBatDau: '2099-10-01T08:00:00', ngayKetThuc: '2099-10-10T17:00:59', ...over })
  const dung = (cheDo = 'MAX', muc = [KHAC()]) => ({
    '/dot-giam-gia/ap-dung': { chinhSach: cheDo, muc },
    '/dot-giam-gia/che-do': { cheDo, moTa: `Mô tả ${cheDo} từ backend.` },
  })

  it('chưa chọn biến thể / chưa nhập đủ: chỉ nhắc, rồi chọn biến thể + nhập % + giờ trùng thì cảnh báo hiện ra, đổi giờ không trùng thì mất', async () => {
    datGio()
    const { w } = await mo({ du: dung() })
    expect(w.find('[data-testid=dgg-trung-chua-du]').exists()).toBe(true)
    await chonSanPham(w, 'Astrox 99')
    await w.get('#dgg-gia-tri').setValue('15')
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-04', '12:00')
    await datNgayGio(w, 'dgg-den-ngay', '2099-10-20', '23:59')
    const canhBao = w.get('[data-testid=dgg-canh-bao-trung]')
    expect(canhBao.text()).toContain('DGG07')
    expect(canhBao.text()).toContain('Yonex Festival')
    expect(w.get('[data-testid=dgg-che-do-ten]').text()).toBe('Lấy mức cao nhất')
    expect(w.get('[data-testid=dgg-che-do]').text()).toContain('Mô tả MAX từ backend.')
    // dời đợt này sang sau khi đợt kia kết thúc -> hết trùng
    await datNgayGio(w, 'dgg-tu-ngay', '2099-11-01', '00:00')
    await datNgayGio(w, 'dgg-den-ngay', '2099-11-30', '23:59')
    expect(w.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(false)
    expect(w.find('[data-testid=dgg-khong-trung]').exists()).toBe(true)
    w.unmount()
  })

  it('backend cấu hình CỘNG DỒN: thẻ hiện "Cộng dồn" và đoạn chồng nhau áp dụng 25%', async () => {
    datGio()
    const { w } = await mo({ du: dung('CONG_DON') })
    await chonSanPham(w, 'Astrox 99')
    await w.get('#dgg-gia-tri').setValue('15')
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-04', '12:00')
    await datNgayGio(w, 'dgg-den-ngay', '2099-10-20', '23:59')
    expect(w.get('[data-testid=dgg-che-do-ten]').text()).toBe('Cộng dồn')
    const dong = w.get('[data-testid=dgg-lich-11]').findAll('li')[0]
    expect(dong.get('[data-che-do=CONG_DON]').classes()).toContain('on')
    expect(dong.get('.dgg-lich-gia').text()).toBe('2.250.000 ₫') // 3.000.000 giảm 25%
    w.unmount()
  })

  it('backend cũ chưa có API cách tính: dùng "chinhSach" của API áp dụng; API áp dụng lỗi thì trang vẫn mở bình thường', async () => {
    datGio()
    const { w } = await mo({ du: { '/dot-giam-gia/ap-dung': { chinhSach: 'TRUNG_BINH', muc: [KHAC()] }, '/dot-giam-gia/che-do': () => { throw new Error('404') } } })
    await chonSanPham(w, 'Astrox 99')
    await w.get('#dgg-gia-tri').setValue('15')
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-04', '12:00')
    await datNgayGio(w, 'dgg-den-ngay', '2099-10-20', '23:59')
    expect(w.get('[data-testid=dgg-che-do-ten]').text()).toBe('Trung bình cộng')
    w.unmount()
    const { w: w2 } = await mo({ du: { '/dot-giam-gia/ap-dung': () => { throw new Error('x') } } })
    expect(w2.find('#dgg-ten').exists()).toBe(true)
    expect(w2.find('[data-testid=dgg-che-do-ten]').text()).toBe('Lấy mức cao nhất')
    w2.unmount()
  })

  it('cảnh báo trùng đợt chỉ là cảnh báo: vẫn lưu được', async () => {
    datGio()
    const { w } = await mo({ du: dung() })
    await chonSanPham(w, 'Astrox 99')
    await w.get('#dgg-ten').setValue('Đợt trùng')
    await w.get('#dgg-gia-tri').setValue('15')
    await datNgayGio(w, 'dgg-tu-ngay', '2099-10-04', '12:00')
    await datNgayGio(w, 'dgg-den-ngay', '2099-10-20', '23:59')
    expect(w.find('[data-testid=dgg-canh-bao-trung]').exists()).toBe(true)
    await xacNhanLuu(w)
    expect(mock.calls.post.some((c) => c[0] === '/dot-giam-gia')).toBe(true)
    w.unmount()
  })
})

/* =================== Danh sách đợt =================== */
describe('Danh sách đợt giảm giá: ngày giờ + trạng thái theo giờ', () => {
  const DOT = (id, ma, bd, kt, trangThai = 1) => ({ id, maDot: ma, tenDot: 'Đợt ' + ma, phanTramGiamDot: 12, ngayBatDau: bd, ngayKetThuc: kt, moTa: '', trangThai })
  async function moDs(ds) {
    mock = mockApi({ get: (url) => (url === '/dot-giam-gia' ? ds : undefined) })
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<div/>' } }] })
    router.push('/dot-giam-gia'); await router.isReady()
    const Comp = (await import('../src/components/DotGiamGiaManager.vue')).default
    const w = mount(Comp, { global: { plugins: [router] }, attachTo: document.body })
    await flushPromises()
    return w
  }
  const dong = (w, ma) => w.findAll('tbody tr').find((tr) => tr.text().includes(ma))
  const DS = () => [
    DOT(1, 'DGG001', '2026-10-15T18:00:00', '2026-10-15T20:30:59'), // tối nay
    DOT(2, 'DGG002', '2026-10-15T08:00:00', '2026-10-15T09:59:59'), // sáng nay (đã xong lúc 10:00)
    DOT(3, 'DGG003', '2026-10-14T00:00:00', '2026-10-16T23:59:59'), // đang chạy
    DOT(4, 'DGG004', '2026-10-14T00:00:00', '2026-10-16T23:59:59', 0), // đang chạy nhưng công tắc tắt
  ]

  it('cột Ngày giờ bắt đầu / kết thúc hiện dd/mm/yyyy HH:mm', async () => {
    datGio() // 10:00
    const w = await moDs(DS())
    const dau = w.findAll('thead th').map((t) => t.text())
    expect(dau).toContain('Ngày giờ bắt đầu')
    expect(dau).toContain('Ngày giờ kết thúc')
    expect(dong(w, 'DGG001').text()).toContain('15/10/2026 18:00')
    expect(dong(w, 'DGG001').text()).toContain('15/10/2026 20:30')
    w.unmount()
  })

  it('10:00 sáng: đợt 18:00 tối nay "Sắp diễn ra", đợt 08:00-09:59 "Đã kết thúc", đợt nhiều ngày "Đang diễn ra", công tắc tắt "Ngừng hoạt động"', async () => {
    datGio(2026, 10, 15, 10, 0, 0)
    const w = await moDs(DS())
    expect(dong(w, 'DGG001').text()).toContain('Sắp diễn ra')
    expect(dong(w, 'DGG002').text()).toContain('Đã kết thúc')
    expect(dong(w, 'DGG003').text()).toContain('Đang diễn ra')
    expect(dong(w, 'DGG004').text()).toContain('Ngừng hoạt động')
    w.unmount()
  })

  it('18:30 tối: đợt 18:00-20:30 đã "Đang diễn ra" (cùng danh sách, chỉ đổi giờ)', async () => {
    datGio(2026, 10, 15, 18, 30, 0)
    const w = await moDs(DS())
    expect(dong(w, 'DGG001').text()).toContain('Đang diễn ra')
    w.unmount()
  })

  it('20:31 tối: đợt kết thúc 20:30 đã "Đã kết thúc" và công tắc bị khóa; lọc theo trạng thái dùng đúng giờ', async () => {
    datGio(2026, 10, 15, 20, 31, 0)
    const w = await moDs(DS())
    expect(dong(w, 'DGG001').text()).toContain('Đã kết thúc')
    const sw = dong(w, 'DGG001').find('button[role="switch"]')
    expect(sw.attributes('disabled')).toBeDefined()
    expect(sw.attributes('aria-checked')).toBe('false')
    await datSelect2(w, '#dgg-trang-thai', 'DA_KET_THUC')
    const ma = w.findAll('tbody tr').map((tr) => tr.find('.ad-code').text())
    expect(ma.sort()).toEqual(['DGG001', 'DGG002'])
    w.unmount()
  })
})

/* =================== Cửa sổ "Sản phẩm áp dụng" =================== */
describe('Cửa sổ Sản phẩm áp dụng: ghi chú theo giờ + cảnh báo trùng đợt khi chọn biến thể', () => {
  const CAMPAIGN = { id: 1, maDot: 'DGG01', tenDot: 'Đợt 1', phanTramGiamDot: 12, trangThai: 1, ngayBatDau: '2099-10-04T12:00:00', ngayKetThuc: '2099-10-20T23:59:59' }
  const BT = [
    { id: 1, ma: 'SP001-BT1', giaBan: 1000000, hoatDong: true },
    { id: 2, ma: 'SP001-BT2', giaBan: 2000000, hoatDong: true },
  ]
  const KHAC = (over = {}) => ({ idBienThe: 2, idDot: 7, maDot: 'DGG07', tenDot: 'Đợt 7', phanTram: 30, ngayBatDau: '2099-10-01T08:00:00', ngayKetThuc: '2099-10-10T17:00:59', ...over })
  async function moModal({ campaign = CAMPAIGN, muc = [KHAC()], cheDo } = {}) {
    mock = mockApi({
      get: (url) => {
        if (url === '/bien-the-san-pham') return BT
        if (url === '/dot-giam-gia-chi-tiet/campaign/1') return []
        if (url === '/dot-giam-gia/ap-dung') return { chinhSach: 'MAX', muc }
        if (url === '/dot-giam-gia/che-do') return cheDo ?? []
        return undefined
      },
    })
    const Modal = (await import('../src/components/dotGiamGia/DotGiamGiaChiTietModal.vue')).default
    const w = mount(Modal, { props: { campaign }, attachTo: document.body })
    await flushPromises()
    return w
  }

  it('đợt sắp diễn ra: ghi chú nêu ngày GIỜ bắt đầu', async () => {
    datGio()
    const w = await moModal()
    expect(w.get('[data-testid=ghi-chu-dot]').text()).toContain('từ 04/10/2099 12:00')
    w.unmount()
  })

  it('chọn biến thể đang nằm trong đợt khác trùng thời gian: cảnh báo + kết quả theo cách tính (cao nhất = 30%)', async () => {
    datGio()
    const w = await moModal()
    expect(w.find('[data-testid=ct-canh-bao-trung]').exists()).toBe(false) // chưa chọn biến thể
    await datSelect2(w, '#dgg-ct-bien-the', 2)
    const cb = w.get('[data-testid=ct-canh-bao-trung]')
    expect(cb.text()).toContain('DGG07 giảm 30%')
    expect(cb.text()).toContain('01/10/2099 08:00 → 10/10/2099 17:00')
    expect(w.get('[data-testid=ct-ket-qua-trung]').text()).toContain('lấy mức cao nhất')
    expect(w.get('[data-testid=ct-ket-qua-trung]').text()).toContain('30%') // đợt này 12% vs đợt kia 30%
    await w.find('input[type=number]').setValue(50)
    expect(w.get('[data-testid=ct-ket-qua-trung]').text()).toContain('50%')
    w.unmount()
  })

  it('cộng dồn: kết quả = tổng hai đợt (12% + 30% = 42%)', async () => {
    datGio()
    const w = await moModal({ cheDo: { cheDo: 'CONG_DON', moTa: '...' } })
    await datSelect2(w, '#dgg-ct-bien-the', 2)
    expect(w.get('[data-testid=ct-ket-qua-trung]').text()).toContain('cộng dồn')
    expect(w.get('[data-testid=ct-ket-qua-trung]').text()).toContain('42%')
    w.unmount()
  })

  it('biến thể không nằm đợt nào khác, hoặc đợt kia không trùng thời gian: không cảnh báo', async () => {
    datGio()
    const w = await moModal()
    await datSelect2(w, '#dgg-ct-bien-the', 1)
    expect(w.find('[data-testid=ct-canh-bao-trung]').exists()).toBe(false)
    w.unmount()
    const w2 = await moModal({ muc: [KHAC({ ngayBatDau: '2099-09-01T00:00:00', ngayKetThuc: '2099-10-04T11:59:59' })] })
    await datSelect2(w2, '#dgg-ct-bien-the', 2)
    expect(w2.find('[data-testid=ct-canh-bao-trung]').exists()).toBe(false)
    w2.unmount()
  })
})

/* =================== Lịch giảm giá đã lưu (backend tính) =================== */
describe('Lịch giảm giá theo ngày giờ của đợt đã lưu', () => {
  const LICH = [{
    idBienThe: 1, maSpct: 'SPCT1', tenSanPham: 'Vợt A', giaBan: 1000000,
    doan: [
      { tuLuc: '2099-10-01T08:00:00', denLuc: '2099-10-04T11:59:59', phanTramApDung: 10, phanTramCaoNhat: 10, phanTramCongDon: 10, phanTramTrungBinh: 10, cacDot: [{ idDot: 1, maDot: 'DGG01', tenDot: 'A', phanTram: 10 }], giaSauGiam: 900000 },
      { tuLuc: '2099-10-04T12:00:00', denLuc: '2099-10-10T17:00:59', phanTramApDung: 25, phanTramCaoNhat: 15, phanTramCongDon: 25, phanTramTrungBinh: 12.5, cacDot: [{ idDot: 1, maDot: 'DGG01', tenDot: 'A', phanTram: 10 }, { idDot: 2, maDot: 'DGG02', tenDot: 'B', phanTram: 15 }], giaSauGiam: 750000 },
    ],
  }]
  async function moLich(cheDo) {
    mock = mockApi({ get: (url) => (url === '/dot-giam-gia/1/lich-giam-gia' ? LICH : url === '/dot-giam-gia/che-do' ? cheDo : undefined) })
    const Comp = (await import('../src/components/dotGiamGia/DotGiamGiaLichCard.vue')).default
    const w = mount(Comp, { props: { idDot: 1 }, attachTo: document.body })
    await flushPromises()
    return w
  }

  it('hiện từ lúc / đến lúc có giờ, đoạn chồng nhau liệt kê cả ba mức và tô xanh mức đang dùng (cộng dồn)', async () => {
    const w = await moLich({ cheDo: 'CONG_DON', moTa: 'Cộng dồn nhé.' })
    const dong = w.findAll('tbody tr')
    expect(dong).toHaveLength(2)
    expect(dong[0].text()).toContain('01/10/2099 08:00')
    expect(dong[0].text()).toContain('04/10/2099 11:59')
    expect(dong[1].text()).toContain('04/10/2099 12:00')
    expect(dong[1].text()).toContain('10/10/2099 17:00')
    expect(dong[1].text()).toContain('chồng 2 đợt')
    expect(dong[1].get('[data-che-do=MAX]').text()).toBe('Cao nhất 15%')
    expect(dong[1].get('[data-che-do=CONG_DON]').text()).toBe('Cộng dồn 25%')
    expect(dong[1].get('[data-che-do=TRUNG_BINH]').text()).toBe('Trung bình 12,5%')
    expect(dong[1].get('[data-che-do=CONG_DON]').classes()).toContain('on')
    expect(dong[1].get('[data-che-do=MAX]').classes()).not.toContain('on')
    expect(dong[1].text()).toContain('750.000 ₫')
    expect(w.get('[data-testid=lich-che-do]').text()).toContain('Cộng dồn')
    expect(w.get('[data-testid=lich-che-do]').text()).toContain('Cộng dồn nhé.')
    w.unmount()
  })

  it('backend chưa có API cách tính: vẫn hiện lịch, mặc định coi là "Lấy mức cao nhất"', async () => {
    const w = await moLich([])
    expect(w.findAll('tbody tr')).toHaveLength(2)
    expect(w.get('[data-testid=lich-che-do]').text()).toContain('Lấy mức cao nhất')
    w.unmount()
  })
})
