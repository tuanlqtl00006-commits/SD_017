// Phiếu giảm giá cá nhân (nhận xét của cô: "Cá nhân chưa thấy gửi mail", "chưa định dạng"):
// số lượng tự bằng số khách chọn, ô tiền có định dạng, cảnh báo cấu hình Gmail, báo rõ kết quả gửi 3 loại mail.
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { useToast } from '../src/composables/useToast'
import { cauBaoKetQuaMail, theoDoiKetQuaMail } from '../src/services/phieuGiamGiaService'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

const KHACH = [
  { id: 1, ma: 'KH01', hoTen: 'An', soDienThoai: '0901', email: 'an@mail.vn' },
  { id: 2, ma: 'KH02', hoTen: 'Bình', soDienThoai: '0902', email: 'binh@mail.vn' },
  { id: 3, ma: 'KH03', hoTen: 'Chi', soDienThoai: '0903', email: '' },
]

function api(trangThaiMail = { daCauHinh: true, dangNhap: 'THANH_CONG', lyDo: '' }) {
  return {
    api: {
      get: vi.fn(async (url) => {
        if (url === '/phieu-giam-gia/khach-hang') return { data: KHACH }
        if (url === '/phieu-giam-gia/trang-thai-mail') return { data: trangThaiMail }
        return { data: [] }
      }),
    },
  }
}

async function montarForm(item = null) {
  const Comp = (await import('../src/components/phieuGiamGia/PhieuGiamGiaFormModal.vue')).default
  const w = mount(Comp, { props: { item, all: [], asPage: true }, attachTo: document.body })
  await flushPromises()
  return w
}
const chonCaNhan = async (w) => {
  await w.findAll('input[name=hinhThuc]').find((i) => i.element.value === 'CA_NHAN').setValue(true)
  await flushPromises()
}

beforeEach(() => {
  document.body.innerHTML = ''
  useToast().toasts.splice(0)
})

describe('Form phiếu cá nhân', () => {
  it('số lượng tự bằng số khách đã chọn (chọn 3 -> 3, bỏ 1 -> 2), ô số lượng chỉ đọc', async () => {
    mock = api()
    const w = await montarForm()
    await chonCaNhan(w)
    const boxes = w.findAll('.pgg-picker-item input[type=checkbox]')
    expect(boxes.length).toBe(3)
    for (const b of boxes) await b.setValue(true)
    expect(w.get('#pgg-so-luong').element.value).toBe('3')
    expect(w.get('#pgg-so-luong').attributes('readonly')).toBeDefined()
    await boxes[0].setValue(false)
    expect(w.get('#pgg-so-luong').element.value).toBe('2')
    expect(w.text()).toContain('Đã chọn 2')
  })

  it('khách chưa có email hiện nhãn "Chưa có email" và câu báo trước số mail sẽ gửi', async () => {
    mock = api()
    const w = await montarForm()
    await chonCaNhan(w)
    expect(w.text()).toContain('an@mail.vn')
    expect(w.findAll('.pgg-picker-item')[2].text()).toContain('Chưa có email')
    for (const b of w.findAll('.pgg-picker-item input[type=checkbox]')) await b.setValue(true)
    expect(w.get('[data-testid=pgg-mail-note]').text()).toContain('gửi mail tặng phiếu cho 2 khách đã chọn (1 khách chưa có email')
    expect(w.get('[data-testid=pgg-mail-status]').text()).toContain('Gmail đã sẵn sàng')
  })

  it('chưa cấu hình Gmail / đăng nhập lỗi: cảnh báo rõ ngay trên form trước khi lưu', async () => {
    mock = api({ daCauHinh: false, dangNhap: 'CHUA_CAU_HINH', lyDo: '' })
    let w = await montarForm()
    await chonCaNhan(w)
    expect(w.get('[data-testid=pgg-mail-status]').text()).toContain('Chưa cấu hình Gmail')
    w.unmount()
    mock = api({ daCauHinh: true, dangNhap: 'THAT_BAI', lyDo: '535 Username and Password not accepted' })
    w = await montarForm()
    await chonCaNhan(w)
    expect(w.get('[data-testid=pgg-mail-status]').text()).toContain('535 Username and Password not accepted')
    expect(w.get('[data-testid=pgg-mail-status]').classes()).toContain('is-error')
  })

  it('ô tiền có định dạng 1.500.000 (giảm tối đa, đơn tối thiểu, giảm theo số tiền) và gửi đi số', async () => {
    mock = api()
    const w = await montarForm()
    await w.get('#pgg-ten').setValue('Tri ân')
    await w.get('#pgg-gia-tri').setValue('10')
    await w.get('#pgg-giam-toi-da').setValue('200000')
    await w.get('#pgg-don-toi-thieu').setValue('1500000')
    expect(w.get('#pgg-giam-toi-da').element.value).toBe('200.000')
    expect(w.get('#pgg-don-toi-thieu').element.value).toBe('1.500.000')
    await w.get('#pgg-ngay-kt').setValue('2099-12-31')
    await w.get('form').trigger('submit'); await flushPromises()
    const body = w.emitted('save')[0][0]
    expect(body).toMatchObject({ giaTri: 10, giamToiDa: 200000, donToiThieu: 1500000, loaiGiam: 'PHAN_TRAM' })
    // đổi sang giảm theo số tiền: ô giá trị thành ô tiền, giảm tối đa bị khóa
    await w.findAll('input[name=loaiGiam]').find((i) => i.element.value === 'TIEN_MAT').setValue(true)
    await w.get('#pgg-gia-tri').setValue('50000')
    expect(w.get('#pgg-gia-tri').element.value).toBe('50.000')
    expect(w.get('#pgg-giam-toi-da').attributes('disabled')).toBeDefined()
  })

  it('kiểm tra dữ liệu: % ngoài 1-100, tiền < 1.000, đơn tối thiểu < số tiền giảm, ngày kết thúc trước ngày bắt đầu, chưa chọn khách', async () => {
    mock = api()
    const w = await montarForm()
    await w.get('#pgg-ten').setValue('X')
    await w.get('#pgg-gia-tri').setValue('150')
    await w.get('#pgg-ngay-bd').setValue('2099-05-10')
    await w.get('#pgg-ngay-kt').setValue('2099-05-01')
    await chonCaNhan(w)
    await w.get('form').trigger('submit'); await flushPromises()
    expect(w.text()).toContain('Phần trăm giảm từ 1 đến 100.')
    expect(w.text()).toContain('Ngày kết thúc phải sau hoặc cùng ngày bắt đầu.')
    expect(w.text()).toContain('Chọn ít nhất một khách hàng để tặng phiếu.')
    await w.findAll('input[name=loaiGiam]').find((i) => i.element.value === 'TIEN_MAT').setValue(true)
    await w.get('#pgg-gia-tri').setValue('500')
    await flushPromises()
    expect(w.text()).toContain('Số tiền giảm tối thiểu 1.000 ₫.')
    await w.get('#pgg-gia-tri').setValue('100000')
    await w.get('#pgg-don-toi-thieu').setValue('50000')
    await flushPromises()
    expect(w.text()).toContain('Đơn tối thiểu phải lớn hơn hoặc bằng số tiền giảm.')
    expect(w.emitted('save')).toBeUndefined()
  })

  it('sửa phiếu đã bắt đầu: báo trước là lưu sẽ không gửi mail', async () => {
    mock = api()
    const item = { id: 1, ma: 'VCH0001', ten: 'Tri ân', hinhThuc: 'CA_NHAN', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: null, donToiThieu: 0, soLuong: 1, soLuongDaDung: 0, ngayBatDau: '2020-01-01', ngayKetThuc: '2099-01-01', hoatDong: true, khachHangs: [{ id: 1, ma: 'KH01', hoTen: 'An' }] }
    const w = await montarForm(item)
    expect(w.text()).toContain('Phiếu đã bắt đầu nên lưu thay đổi sẽ không gửi mail')
  })
})

describe('Báo kết quả gửi mail sau khi lưu (mail mới / cập nhật / hủy gửi ở nền)', () => {
  const cho = () => Promise.resolve()
  it('hỏi lại tới khi xong; gửi hết thì báo "Đã gửi 3/3 mail"', async () => {
    const lan = [{ tong: 3, daGui: 1, loi: 0, giaLap: 0, xong: false }, { tong: 3, daGui: 3, loi: 0, giaLap: 0, xong: true }]
    mock = { api: { get: vi.fn(async () => ({ data: lan.shift() })) } }
    const kq = await theoDoiKetQuaMail(9, { cho })
    expect(kq.trangThai).toBe('DA_GUI')
    expect(mock.api.get).toHaveBeenCalledWith('/phieu-giam-gia/9/ket-qua-mail')
    expect(cauBaoKetQuaMail(kq, 'VCH01')).toEqual({ loai: 'success', noiDung: 'Đã gửi 3/3 mail phiếu VCH01 thành công.' })
  })
  it('gửi lỗi (sai mật khẩu ứng dụng / mất mạng): báo ĐỎ kèm lý do thật, nói rõ phiếu vẫn đã lưu', async () => {
    mock = { api: { get: vi.fn(async () => ({ data: { tong: 2, daGui: 0, loi: 2, giaLap: 0, xong: true, lyDo: '535 sai mật khẩu' } })) } }
    const kq = await theoDoiKetQuaMail(9, { cho })
    const bao = cauBaoKetQuaMail(kq)
    expect(bao.loai).toBe('error')
    expect(bao.tieuDe).toContain('phiếu vẫn đã lưu')
    expect(bao.noiDung).toContain('2/2 mail gửi KHÔNG thành công')
    expect(bao.noiDung).toContain('535 sai mật khẩu')
  })
  it('chưa cấu hình: báo mail chưa gửi; backend không trả lời: không báo gì thêm', async () => {
    mock = { api: { get: vi.fn(async () => ({ data: { tong: 1, daGui: 0, loi: 0, giaLap: 1, xong: true } })) } }
    expect(cauBaoKetQuaMail(await theoDoiKetQuaMail(9, { cho })).tieuDe).toBe('Mail chưa gửi')
    mock = { api: { get: vi.fn(async () => { throw new Error('mạng') }) } }
    const kq = await theoDoiKetQuaMail(9, { cho })
    expect(kq.trangThai).toBe('KHONG_RO')
    expect(cauBaoKetQuaMail(kq)).toBeNull()
  })
  it('quá thời gian chờ mà chưa xong: báo số mail đã gửi, phần còn lại đang gửi', async () => {
    mock = { api: { get: vi.fn(async () => ({ data: { tong: 5, daGui: 2, loi: 0, giaLap: 0, xong: false } })) } }
    const kq = await theoDoiKetQuaMail(9, { cho, lan: 3 })
    expect(mock.api.get).toHaveBeenCalledTimes(3)
    expect(cauBaoKetQuaMail(kq).noiDung).toContain('Đã gửi 2/5 mail')
  })
})
