import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  CCCD,
  EMAIL,
  HO_TEN,
  HUONG_DAN_TUOI_KHACH,
  HUONG_DAN_TUOI_NHAN_VIEN,
  LOI_CCCD,
  LOI_EMAIL,
  LOI_HO_TEN_KY_TU,
  LOI_SDT,
  SDT,
  TUOI_KHACH_TOI_THIEU,
  TUOI_NHAN_VIEN_TOI_THIEU,
  TUOI_TOI_DA,
  chuanHoaHoTen,
  congNam,
  khoaEmail,
  loiCccd,
  loiEmail,
  loiHoTen,
  loiMatKhau,
  loiNgaySinh,
  loiSdt,
  ngaySinhMuonNhat,
  ngaySinhSomNhat,
  sinhMatKhauTam,
  timBanTrung,
  tuoiTai,
  vaoLamKhiDuTuoi,
} from '../src/utils/kiemTra'

const cong = (iso, n) => {
  const d = new Date(`${iso}T00:00:00`)
  d.setDate(d.getDate() + n)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

describe('Số điện thoại: 10 số, bắt đầu bằng 0', () => {
  it('10 chữ số bắt đầu bằng 0 đều hợp lệ', () => {
    for (const dau of ['01', '02', '03', '04', '05', '06', '07', '08', '09']) expect(loiSdt(`${dau}12345678`)).toBe('')
  })
  it('không bắt đầu bằng 0 bị chặn', () => {
    for (const sai of ['1912345678', '9912345678', '2123456789', '8812345678']) expect(loiSdt(sai)).toBe(LOI_SDT)
  })
  it('sai độ dài / có chữ / có dấu cách / +84 bị chặn', () => {
    for (const sai of ['091234567', '09123456789', '09123abc45', '0912 345 678', '+84912345678', '091-234-5678']) expect(loiSdt(sai)).toBe(LOI_SDT)
  })
  it('để trống báo thiếu (câu thiếu đổi được); khoảng trắng hai đầu được bỏ', () => {
    expect(loiSdt('')).toBe('Vui lòng nhập số điện thoại.')
    expect(loiSdt('   ', 'Nhập số điện thoại.')).toBe('Nhập số điện thoại.')
    expect(loiSdt(null)).toBe('Vui lòng nhập số điện thoại.')
    expect(loiSdt(' 0912345678 ')).toBe('')
  })
})

describe('Email', () => {
  it('đúng định dạng', () => {
    for (const ok of ['an@mail.vn', 'an.nguyen+shop@sub.mail.com', 'AN@Mail.VN', ' an@mail.vn ']) expect(loiEmail(ok)).toBe('')
  })
  it('sai định dạng', () => {
    for (const sai of ['an', 'an@', 'an@mail', 'an @mail.vn', '@mail.vn', 'an@mail.v', 'an@@mail.vn']) expect(loiEmail(sai)).toBe(LOI_EMAIL)
  })
  it('trống và quá dài', () => {
    expect(loiEmail('')).toBe('Vui lòng nhập email.')
    expect(loiEmail('   ', { thieu: 'Nhập email.' })).toBe('Nhập email.')
    expect(loiEmail(`${'a'.repeat(250)}@mail.vn`)).toBe(LOI_EMAIL)
    expect(loiEmail('an', { dinhDang: 'Sai rồi' })).toBe('Sai rồi')
  })
  it('so trùng không phân biệt hoa thường', () => {
    expect(khoaEmail(' An@Mail.VN ')).toBe('an@mail.vn')
  })
})

describe('Họ tên: chỉ chữ cái', () => {
  it('tên hợp lệ (có dấu, nháy đơn, gạch ngang, dấu chấm, khoảng trắng thừa)', () => {
    for (const ok of ['Nguyễn Thị Ánh Tuyết', "Lê O'Brien-Smith Jr.", 'Đặng Văn Ơn', "Lê O’Brien", '  Trần   Bình  ']) expect(loiHoTen(ok)).toBe('')
  })
  it('có chữ số / ký tự đặc biệt / bắt đầu bằng dấu bị chặn', () => {
    for (const sai of ['An 123', 'Nguyễn@An', '12345', 'An_Văn', '<b>An</b>', '.An', '-An', 'An#']) expect(loiHoTen(sai)).toBe(LOI_HO_TEN_KY_TU)
  })
  it('trống / quá ngắn / quá dài', () => {
    expect(loiHoTen('')).toBe('Vui lòng nhập họ tên.')
    expect(loiHoTen('   ', { thieu: 'Nhập họ tên.' })).toBe('Nhập họ tên.')
    expect(loiHoTen('A')).toBe('Họ tên từ 2 đến 100 ký tự.')
    expect(loiHoTen('A'.repeat(101))).toBe('Họ tên từ 2 đến 100 ký tự.')
    expect(loiHoTen('A'.repeat(100))).toBe('')
    expect(loiHoTen('A'.repeat(61), { toiDa: 60 })).toBe('Họ tên từ 2 đến 60 ký tự.')
  })
  it('chuẩn hóa khoảng trắng', () => {
    expect(chuanHoaHoTen('  Nguyễn   Thị  Ánh ')).toBe('Nguyễn Thị Ánh')
  })
})

describe('Số căn cước và mật khẩu', () => {
  it('CCCD không bắt buộc, đã nhập thì đúng 12 chữ số', () => {
    expect(loiCccd('')).toBe('')
    expect(loiCccd('  ')).toBe('')
    expect(loiCccd('001099012345')).toBe('')
    for (const sai of ['12345', '00109901234', '0010990123456', '00109901234a', '001 099 012 345']) expect(loiCccd(sai)).toBe(LOI_CCCD)
  })
  it('mật khẩu: 8-50 ký tự, có chữ và số, không khoảng trắng', () => {
    expect(loiMatKhau('')).toBe('Nhập mật khẩu cho nhân viên mới.')
    expect(loiMatKhau('Ab1')).toBe('Mật khẩu từ 8 đến 50 ký tự.')
    expect(loiMatKhau('a1'.repeat(26))).toBe('Mật khẩu từ 8 đến 50 ký tự.')
    for (const sai of ['abcdefgh', '12345678', 'abcd 1234', '!!!!!!!!!']) expect(loiMatKhau(sai)).toBe('Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.')
    for (const ok of ['Footstyle@123', 'abcd1234', 'Mật1khẩu']) expect(loiMatKhau(ok)).toBe('')
  })
  it('sinhMatKhauTam: 10 ký tự, luôn có chữ hoa, thường, số, hợp lệ luật mật khẩu', () => {
    for (let i = 0; i < 50; i++) {
      const mk = sinhMatKhauTam()
      expect(mk).toHaveLength(10)
      expect(mk).toMatch(/[A-Z]/)
      expect(mk).toMatch(/[a-z]/)
      expect(mk).toMatch(/\d/)
      expect(loiMatKhau(mk)).toBe('')
    }
  })
})

describe('Ngày sinh và tuổi tối thiểu (khách 15, nhân viên 18)', () => {
  it('hằng số tuổi', () => {
    expect([TUOI_KHACH_TOI_THIEU, TUOI_NHAN_VIEN_TOI_THIEU, TUOI_TOI_DA]).toEqual([15, 18, 100])
  })
  it('tuổi tròn: tính đúng ngày sinh nhật, kể cả 29/02', () => {
    expect(tuoiTai('2000-05-20', '2026-05-19')).toBe(25)
    expect(tuoiTai('2000-05-20', '2026-05-20')).toBe(26)
    expect(tuoiTai('2000-05-20', '2026-12-31')).toBe(26)
    expect(tuoiTai('2000-02-29', '2023-02-28')).toBe(22)
    expect(tuoiTai('2000-02-29', '2023-03-01')).toBe(23)
    expect(tuoiTai('khong-phai-ngay', '2026-01-01')).toBeNull()
  })
  it('khách hàng: đúng 15 tuổi hôm nay được, thiếu 1 ngày bị chặn', () => {
    const homNay = '2026-10-15'
    expect(loiNgaySinh('2011-10-15', 15, 'Khách hàng', homNay)).toBe('')
    expect(loiNgaySinh('2011-10-16', 15, 'Khách hàng', homNay)).toBe('Khách hàng phải từ đủ 15 tuổi.')
    expect(loiNgaySinh('2016-01-01', 15, 'Khách hàng', homNay)).toBe('Khách hàng phải từ đủ 15 tuổi.')
  })
  it('nhân viên: đúng 18 tuổi hôm nay được, thiếu 1 ngày bị chặn', () => {
    const homNay = '2026-10-15'
    expect(loiNgaySinh('2008-10-15', 18, 'Nhân viên', homNay)).toBe('')
    expect(loiNgaySinh('2008-10-16', 18, 'Nhân viên', homNay)).toBe('Nhân viên phải từ đủ 18 tuổi.')
  })
  it('tương lai và quá 100 tuổi', () => {
    const homNay = '2026-10-15'
    expect(loiNgaySinh('2026-10-16', 15, 'Khách hàng', homNay)).toBe('Ngày sinh không được ở tương lai.')
    expect(loiNgaySinh('2026-10-15', 15, 'Khách hàng', homNay)).toBe('Khách hàng phải từ đủ 15 tuổi.')
    expect(loiNgaySinh('1926-10-15', 15, 'Khách hàng', homNay)).toBe('') // đúng 100 tuổi
    expect(loiNgaySinh('1926-10-14', 15, 'Khách hàng', homNay)).toBe('') // 100 tuổi + 1 ngày: vẫn là 100
    expect(loiNgaySinh('1925-10-15', 15, 'Khách hàng', homNay)).toBe('Ngày sinh không hợp lệ (quá 100 tuổi).')
    expect(loiNgaySinh('1900-01-01', 15, 'Khách hàng', homNay)).toBe('Ngày sinh không hợp lệ (quá 100 tuổi).')
  })
  it('ngày trống thì không báo lỗi (nơi gọi tự xét bắt buộc)', () => {
    expect(loiNgaySinh('', 15, 'Khách hàng')).toBe('')
    expect(loiNgaySinh(null, 18, 'Nhân viên')).toBe('')
  })
  it('ngày giới hạn trên lịch khớp đúng luật kiểm tra (nhiều "hôm nay" khác nhau, kể cả 29/02)', () => {
    for (const homNay of ['2026-10-15', '2028-02-29', '2027-03-01', '2024-02-28', '2031-12-31', '2026-01-01']) {
      for (const tuoi of [15, 18]) {
        const muonNhat = ngaySinhMuonNhat(tuoi, homNay)
        expect(loiNgaySinh(muonNhat, tuoi, 'X', homNay), `${homNay}: ${muonNhat} phải hợp lệ`).toBe('')
        expect(loiNgaySinh(cong(muonNhat, 1), tuoi, 'X', homNay), `${homNay}: sau ${muonNhat} 1 ngày phải bị chặn`).not.toBe('')
      }
      const somNhat = ngaySinhSomNhat(homNay)
      expect(loiNgaySinh(somNhat, 15, 'X', homNay), `${homNay}: ${somNhat} phải hợp lệ`).toBe('')
      expect(loiNgaySinh(cong(somNhat, -1), 15, 'X', homNay), `${homNay}: trước ${somNhat} 1 ngày phải bị chặn`).toContain('quá 100 tuổi')
    }
  })
  it('dòng hướng dẫn trên form nêu căn cứ pháp lý', () => {
    expect(HUONG_DAN_TUOI_KHACH).toContain('Bộ luật Dân sự 2015')
    expect(HUONG_DAN_TUOI_KHACH).toContain('Điều 21')
    expect(HUONG_DAN_TUOI_KHACH).toContain('15 tuổi')
    expect(HUONG_DAN_TUOI_NHAN_VIEN).toContain('Bộ luật Lao động 2019')
    expect(HUONG_DAN_TUOI_NHAN_VIEN).toContain('Điều 143')
    expect(HUONG_DAN_TUOI_NHAN_VIEN).toContain('18 tuổi')
  })
})

describe('Cộng năm và mốc vào làm của nhân viên', () => {
  it('congNam giống LocalDate.plusYears của Java: 29/02 sang năm không nhuận lùi về 28/02', () => {
    expect(congNam('2000-05-20', 18)).toBe('2018-05-20')
    expect(congNam('2008-02-29', 18)).toBe('2026-02-28')
    expect(congNam('2008-02-29', 16)).toBe('2024-02-29') // năm đích nhuận thì giữ nguyên
    expect(congNam('2000-12-31', 1)).toBe('2001-12-31')
    expect(congNam('2000-05-20T10:00:00', 18)).toBe('2018-05-20') // API có thể trả kèm giờ
    expect(congNam('', 18)).toBe('')
    expect(congNam(null, 18)).toBe('')
    expect(congNam('không phải ngày', 18)).toBe('')
  })

  it('vaoLamKhiDuTuoi: vào làm đúng ngày tròn 18 tuổi là được, sớm hơn một ngày thì không', () => {
    expect(vaoLamKhiDuTuoi('2000-05-20', '2018-05-20')).toBe(true)
    expect(vaoLamKhiDuTuoi('2000-05-20', '2018-05-19')).toBe(false)
    expect(vaoLamKhiDuTuoi('2000-05-20', '2026-10-15')).toBe(true)
    expect(vaoLamKhiDuTuoi('2008-02-29', '2026-02-28')).toBe(true) // giống Java: ngày vào làm không sớm hơn plusYears(18) = 28/02
    expect(vaoLamKhiDuTuoi('2008-02-29', '2026-02-27')).toBe(false)
    expect(vaoLamKhiDuTuoi('2000-05-20', '2005-01-01', 10)).toBe(false) // mốc tuổi đổi được
  })

  it('thiếu ngày sinh hoặc ngày vào làm thì không chặn (nơi gọi tự xét bắt buộc)', () => {
    expect(vaoLamKhiDuTuoi('', '2020-01-01')).toBe(true)
    expect(vaoLamKhiDuTuoi('2000-05-20', '')).toBe(true)
    expect(vaoLamKhiDuTuoi(null, null)).toBe(true)
  })
})

describe('Tìm bản ghi trùng', () => {
  const ds = [
    { id: 1, sdt: '0912345678', email: 'An@Mail.vn' },
    { id: 2, sdt: '0987654321', email: 'binh@mail.vn' },
  ]
  it('trùng khác hoa thường; không tính chính mình; ô trống không bao giờ trùng', () => {
    expect(timBanTrung(ds, 'an@MAIL.VN', (x) => x.email, khoaEmail)?.id).toBe(1)
    expect(timBanTrung(ds, 'an@mail.vn', (x) => x.email, khoaEmail, 1)).toBeNull()
    expect(timBanTrung(ds, ' 0987654321 ', (x) => x.sdt, (v) => String(v ?? '').trim())?.id).toBe(2)
    expect(timBanTrung(ds, '', (x) => x.email, khoaEmail)).toBeNull()
    expect(timBanTrung(null, 'a@b.vn', (x) => x.email, khoaEmail)).toBeNull()
  })
})

/* ===== Khớp với backend: đọc thẳng QuyTacDuLieu.java và các Service ===== */
const goc = resolve(process.cwd(), '../backend/src/main/java/com/footstyle/demo/service')
const coBackend = existsSync(resolve(goc, 'QuyTacDuLieu.java'))
const doc = (ten) => readFileSync(resolve(goc, ten), 'utf-8')
// Chuỗi trong mã Java ("...") -> chuỗi thật (bỏ gạch chéo kép)
const chuoiJava = (literal) => JSON.parse(`"${literal}"`)

describe.skipIf(!coBackend)('Khớp từng quy tắc với backend (QuyTacDuLieu.java)', () => {
  const java = coBackend ? doc('QuyTacDuLieu.java') : ''
  const mau = (ten) => {
    const m = new RegExp(`${ten}\\s*=\\s*Pattern\\.compile\\("((?:[^"\\\\]|\\\\.)*)"\\)`).exec(java)
    expect(m, `không thấy ${ten} trong QuyTacDuLieu.java`).toBeTruthy()
    return chuoiJava(m[1])
  }
  const chuoi = (ten) => {
    const m = new RegExp(`String\\s+${ten}\\s*=\\s*"((?:[^"\\\\]|\\\\.)*)"`).exec(java)
    expect(m, `không thấy ${ten} trong QuyTacDuLieu.java`).toBeTruthy()
    return chuoiJava(m[1])
  }
  const so = (ten) => Number(new RegExp(`int\\s+${ten}\\s*=\\s*(\\d+)`).exec(java)?.[1])

  it('biểu thức SĐT, email, họ tên, CCCD giống hệt', () => {
    expect(SDT.source).toBe(mau('SDT'))
    expect(EMAIL.source).toBe(mau('EMAIL'))
    expect(HO_TEN.source).toBe(mau('HO_TEN'))
    expect(CCCD.source).toBe(mau('CCCD'))
  })
  it('câu báo lỗi giống hệt', () => {
    expect(LOI_SDT).toBe(chuoi('LOI_SDT'))
    expect(LOI_EMAIL).toBe(chuoi('LOI_EMAIL'))
    expect(LOI_HO_TEN_KY_TU).toBe(chuoi('LOI_HO_TEN_KY_TU'))
  })
  it('mốc tuổi giống hệt', () => {
    expect(TUOI_KHACH_TOI_THIEU).toBe(so('TUOI_KHACH_TOI_THIEU'))
    expect(TUOI_NHAN_VIEN_TOI_THIEU).toBe(so('TUOI_NHAN_VIEN_TOI_THIEU'))
    expect(TUOI_TOI_DA).toBe(so('TUOI_TOI_DA'))
  })
  it('câu báo lỗi ngày sinh có trong cả hai bản', () => {
    for (const manh of ['Ngày sinh không được ở tương lai.', ' phải từ đủ ', 'Ngày sinh không hợp lệ (quá ']) expect(java).toContain(manh)
    expect(loiNgaySinh('2099-01-01', 15, 'Khách hàng', '2026-10-15')).toBe('Ngày sinh không được ở tương lai.')
  })
  it('các câu báo lỗi của form khách hàng / nhân viên có đúng chữ trong backend', () => {
    const kh = doc('KhachHangService.java')
    const nv = doc('NhanVienService.java')
    const formKh = readFileSync(resolve(process.cwd(), 'src/components/khachHang/KhachHangFormPage.vue'), 'utf-8')
    for (const cau of [
      'Vui lòng nhập họ tên khách hàng.',
      'Chọn Tỉnh/Thành phố.',
      'Chọn Phường/Xã.',
      'Nhập địa chỉ cụ thể (số nhà, đường...).',
      'Địa chỉ cụ thể tối đa 255 ký tự.',
      'Giới tính không hợp lệ.',
      'Trạng thái không hợp lệ.',
    ]) {
      expect(kh, `backend thiếu: ${cau}`).toContain(cau)
      expect(formKh, `form khách hàng thiếu: ${cau}`).toContain(cau)
    }
    // Hai chuỗi mặc định của các hàm loiXxx phải trùng backend
    expect(kh).toContain('Vui lòng nhập số điện thoại.')
    expect(kh).toContain('Vui lòng nhập email.')
    expect(kh).toContain('Họ tên từ 2 đến 100 ký tự.')
    expect(kh).toContain('Số điện thoại đã tồn tại.')
    expect(kh).toContain('Email đã tồn tại.')
    for (const cau of ['Nhập mật khẩu cho nhân viên mới.', 'Mật khẩu từ 8 đến 50 ký tự.', 'Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.', LOI_CCCD]) {
      expect(nv, `backend thiếu: ${cau}`).toContain(cau)
    }
    expect(loiMatKhau('')).toBe('Nhập mật khẩu cho nhân viên mới.')
  })

  it('các câu báo lỗi của form nhân viên có đúng chữ trong backend NhanVienService (và form dùng đúng những câu đó)', () => {
    const nv = doc('NhanVienService.java')
    const form = readFileSync(resolve(process.cwd(), 'src/components/nhanVien/NhanVienFormPage.vue'), 'utf-8')
    for (const cau of [
      'Nhập họ tên.',
      'Nhập email.',
      'Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.',
      'Email đã được sử dụng.',
      'Số căn cước công dân đã được sử dụng.',
      'Nhập số điện thoại.',
      'Số điện thoại đã được sử dụng.',
      'Chọn giới tính.',
      'Giới tính không hợp lệ.',
      'Nhập ngày sinh.',
      'Ngày vào làm không được ở tương lai.',
      'Chọn vai trò.',
    ]) {
      expect(nv, `backend thiếu: ${cau}`).toContain(cau)
      expect(form, `form nhân viên thiếu: ${cau}`).toContain(cau)
    }
    // Hai câu có số tuổi được ghép trong form: đối chiếu với bản đã thay số
    expect(nv).toContain(`Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi).`)
    expect(nv).toContain(`Ngày sinh không hợp lệ: nhân viên phải đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi vào ngày vào làm (`)
    expect(form).toContain('Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi).')
    expect(form).toContain('Ngày sinh không hợp lệ: nhân viên phải đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi vào ngày vào làm (')
  })
})
