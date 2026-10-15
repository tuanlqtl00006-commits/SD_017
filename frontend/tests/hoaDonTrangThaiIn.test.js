// Hóa đơn theo lời cô (video review GĐ2): bảng trạng thái khớp backend, nhận ra đơn vị vận chuyển (GHN / GHTK), bản in/PDF hóa đơn.
import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  TAB_TRANG_THAI, TRANG_THAI_HOA_DON, TT, laTrangThaiCuoi, nutChuyen, trangThaiOf,
} from '../src/constants/hoaDon'
import { nhanDienVanChuyen } from '../src/utils/vanChuyen'
import { esc, htmlVanChuyen, moTaMucGiam, nhanGiamGia, taoHtmlHoaDon } from '../src/utils/hoaDonIn'

// Đọc mã nguồn backend (thư mục backend nằm cạnh frontend) để chắc chắn bảng trạng thái hai bên không lệch nhau.
const docJava = (...phan) => readFileSync(resolve(process.cwd(), '..', 'backend/src/main/java/com/footstyle/demo', ...phan), 'utf-8')

/* ===== Bảng trạng thái: giao diện khớp backend ===== */

describe('Trạng thái hóa đơn: khớp backend', () => {
  it('mã trạng thái giống hệt các hằng số trong HoaDon.java', () => {
    const java = docJava('entity', 'HoaDon.java')
    const tuJava = {}
    for (const m of java.matchAll(/public static final int ([A-Z_]+) = (\d+);/g)) tuJava[m[1]] = Number(m[2])
    for (const ten of ['DA_HUY', 'CHO_XAC_NHAN', 'DA_XAC_NHAN', 'CHO_LAY_HANG', 'DANG_GIAO', 'DA_GIAO', 'HOAN_THANH', 'HOAN_TIEN', 'GIAO_THAT_BAI']) {
      expect(TT[ten], ten).toBe(tuJava[ten])
    }
    expect(Object.keys(TT)).toHaveLength(9)
  })

  it('tên hiển thị giống hệt bảng tên trong HoaDonLuongTrangThai.java (cũng là chữ ghi vào lịch sử hóa đơn)', () => {
    const java = docJava('service', 'HoaDonLuongTrangThai.java')
    const tenJava = {}
    for (const m of java.matchAll(/ten\.put\(HoaDon\.([A-Z_]+), "([^"]+)"\);/g)) tenJava[TT[m[1]]] = m[2]
    expect(Object.keys(tenJava)).toHaveLength(9)
    for (const [ma, ten] of Object.entries(tenJava)) expect(TRANG_THAI_HOA_DON[ma].label, `mã ${ma}`).toBe(ten)
  })

  it('trạng thái cuối (Đã hủy, Đã hoàn thành, Hoàn tiền) giống backend; các trạng thái khác chưa khóa', () => {
    const java = docJava('service', 'HoaDonLuongTrangThai.java')
    const khoi = /static boolean laTrangThaiCuoi\(int trangThai\) \{\s*return ([^;]+);/.exec(java)[1]
    const cuoiJava = [...khoi.matchAll(/HoaDon\.([A-Z_]+)/g)].map((m) => TT[m[1]]).sort()
    const cuoiGiaoDien = Object.values(TT).filter(laTrangThaiCuoi).sort()
    expect(cuoiGiaoDien).toEqual(cuoiJava)
    expect(cuoiGiaoDien).toEqual([TT.DA_HUY, TT.HOAN_THANH, TT.HOAN_TIEN].sort())
    for (const dangXuLy of [1, 2, 3, 4, 5, 8]) expect(laTrangThaiCuoi(dangXuLy), `mã ${dangXuLy}`).toBe(false)
  })

  it('mỗi trạng thái có đúng một tab (trừ "Tất cả"); "Giao hàng thất bại" nằm ngay sau "Đang giao hàng"', () => {
    const giaTri = TAB_TRANG_THAI.map((t) => t.value)
    expect(giaTri[0]).toBe(null)
    expect(giaTri.slice(1).sort()).toEqual(Object.values(TT).sort())
    const nhan = TAB_TRANG_THAI.map((t) => t.label)
    expect(nhan.indexOf('Giao hàng thất bại')).toBe(nhan.indexOf('Đang giao hàng') + 1)
    for (const t of TAB_TRANG_THAI.slice(1)) expect(t.label, `tab ${t.value}`).toBe(TRANG_THAI_HOA_DON[t.value].label)
  })

  it('"Giao hàng thất bại" có nhãn màu cam riêng; mã lạ thì "Không xác định"', () => {
    expect(trangThaiOf(TT.GIAO_THAT_BAI)).toEqual({ label: 'Giao hàng thất bại', pill: 'ad-pill-orange' })
    expect(trangThaiOf(99).label).toBe('Không xác định')
    expect(trangThaiOf(undefined).label).toBe('Không xác định')
    expect(readFileSync(resolve(process.cwd(), 'src/assets/admin.css'), 'utf-8')).toContain('.ad-pill-orange')
  })
})

describe('Nút đổi trạng thái', () => {
  it('Hủy đơn là nút nguy hiểm; Giao hàng thất bại là nút cảnh báo; bước đi tiếp là nút chính', () => {
    expect(nutChuyen(2, 1, TT.DA_HUY)).toMatchObject({ text: 'Hủy đơn', kieu: 'nguyhiem' })
    expect(nutChuyen(2, 4, TT.GIAO_THAT_BAI)).toMatchObject({ text: 'Giao hàng thất bại', kieu: 'canhbao' })
    expect(nutChuyen(2, 1, TT.DA_XAC_NHAN)).toMatchObject({ text: 'Xác nhận đơn', kieu: 'chinh' })
    expect(nutChuyen(2, 2, TT.CHO_LAY_HANG).text).toBe('Chuẩn bị hàng')
    expect(nutChuyen(2, 3, TT.DANG_GIAO).text).toBe('Giao cho vận chuyển')
    expect(nutChuyen(2, 4, TT.DA_GIAO).text).toBe('Xác nhận đã giao')
    expect(nutChuyen(2, 5, TT.HOAN_THANH).text).toBe('Hoàn thành đơn')
  })

  it('từ Giao hàng thất bại: về Đang giao hàng là "Giao lại"; đơn tại quầy hoàn thành là "Xác nhận thanh toán"', () => {
    expect(nutChuyen(2, TT.GIAO_THAT_BAI, TT.DANG_GIAO)).toMatchObject({ text: 'Giao lại', kieu: 'chinh' })
    expect(nutChuyen(1, 1, TT.HOAN_THANH).text).toBe('Xác nhận thanh toán')
    expect(nutChuyen(2, 5, TT.HOAN_THANH).text).toBe('Hoàn thành đơn')
  })
})

/* ===== Đơn vị vận chuyển ===== */

describe('Nhận ra đơn vị vận chuyển', () => {
  it('Giao Hàng Nhanh / GHN, Giao Hàng Tiết Kiệm / GHTK, Viettel Post (không phân biệt hoa thường, dấu)', () => {
    for (const ten of ['Giao Hàng Nhanh', 'giao hang nhanh', 'GHN', 'ghn express', 'GIAO HÀNG NHANH']) {
      expect(nhanDienVanChuyen(ten), ten).toMatchObject({ ma: 'GHN', ten: 'Giao Hàng Nhanh', logo: '/van-chuyen/ghn.png' })
    }
    for (const ten of ['Giao Hàng Tiết Kiệm', 'giao hang tiet kiem', 'GHTK']) {
      expect(nhanDienVanChuyen(ten), ten).toMatchObject({ ma: 'GHTK', ten: 'Giao Hàng Tiết Kiệm', logo: '/van-chuyen/ghtk.png' })
    }
    for (const ten of ['Viettel Post', 'ViettelPost', 'VTP']) expect(nhanDienVanChuyen(ten), ten).toMatchObject({ ma: 'VTP', logo: '/van-chuyen/vtp.png' })
  })

  it('GHN không bị nhận nhầm là GHTK và ngược lại; "ghn" nằm giữa từ khác không tính', () => {
    expect(nhanDienVanChuyen('GHTK').ma).toBe('GHTK')
    expect(nhanDienVanChuyen('Giao Hàng Tiết Kiệm').ma).toBe('GHTK')
    expect(nhanDienVanChuyen('Nghnguyen Express').ma).toBe('')
  })

  it('đơn vị lạ giữ nguyên tên, không logo; để trống thì null', () => {
    expect(nhanDienVanChuyen('  J&T   Express ')).toEqual({ ma: '', ten: 'J&T Express', logo: null })
    expect(nhanDienVanChuyen('')).toBeNull()
    expect(nhanDienVanChuyen(null)).toBeNull()
    expect(nhanDienVanChuyen(undefined)).toBeNull()
  })

  it('thư mục logo có hướng dẫn bỏ file ghn.png, ghtk.png vào (không tự vẽ logo hãng)', () => {
    const readme = readFileSync(resolve(process.cwd(), 'public/van-chuyen/README.txt'), 'utf-8')
    expect(readme).toContain('ghn.png')
    expect(readme).toContain('ghtk.png')
  })
})

/* ===== Mã phiếu + % giảm ===== */

describe('Mô tả mức giảm của phiếu trên hóa đơn', () => {
  it('phiếu phần trăm: "giảm 10%"; có mức tối đa thì ghi kèm', () => {
    expect(moTaMucGiam({ maPhieuGiamGia: 'VCH7QX2YN', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10 })).toBe('giảm 10%')
    expect(moTaMucGiam({ maPhieuGiamGia: 'VCH7QX2YN', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10, giamToiDaPhieuGiamGia: 100000 })).toBe('giảm 10% (tối đa 100.000 ₫)')
  })
  it('phiếu số tiền: "giảm 50.000 ₫"', () => {
    expect(moTaMucGiam({ maPhieuGiamGia: 'VCHTT50KH', loaiPhieuGiamGia: 2, giaTriPhieuGiamGia: 50000 })).toBe('giảm 50.000 ₫')
  })
  it('không dùng phiếu hoặc thiếu giá trị: không có mô tả', () => {
    expect(moTaMucGiam({})).toBe('')
    expect(moTaMucGiam({ maPhieuGiamGia: null, loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10 })).toBe('')
    expect(moTaMucGiam({ maPhieuGiamGia: 'X', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 0 })).toBe('')
  })
  it('nhãn dòng giảm giá ghi mã + mức giảm', () => {
    expect(nhanGiamGia({ maPhieuGiamGia: 'VCH7QX2YN', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10 })).toBe('Giảm giá (mã VCH7QX2YN – giảm 10%)')
    expect(nhanGiamGia({ maPhieuGiamGia: 'VCH7QX2YN' })).toBe('Giảm giá (mã VCH7QX2YN)')
    expect(nhanGiamGia({ maPhieuGiamGia: 'VCH7QX2YN', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10, giamToiDaPhieuGiamGia: 100000 })).toBe('Giảm giá (mã VCH7QX2YN – giảm 10%, tối đa 100.000 ₫)')
    expect(nhanGiamGia({ maPhieuGiamGia: 'VCHTT50KH', loaiPhieuGiamGia: 2, giaTriPhieuGiamGia: 50000 })).toBe('Giảm giá (mã VCHTT50KH – giảm 50.000 ₫)')
    expect(nhanGiamGia({})).toBe('Giảm giá')
  })
})

/* ===== Bản in / PDF ===== */

const GIAO_HANG = {
  hd: {
    id: 3, maHoaDon: 'HD03', loaiDon: 2, tenLoaiDon: 'Trực tuyến', trangThai: 2, ngayTao: '2026-10-08T14:39:33', tenNhanVien: 'Nguyễn Hoàng Long',
    phuongThucThanhToan: 'Thanh toán khi nhận hàng (COD)', tongTien: 1000000, soTienGiam: 100000, phiVanChuyen: 30000, thanhTien: 930000,
    donViVanChuyen: 'Giao Hàng Nhanh', maPhieuGiamGia: 'VCH7QX2YN', tenPhieuGiamGia: 'Giảm 10%', loaiPhieuGiamGia: 1, giaTriPhieuGiamGia: 10, giamToiDaPhieuGiamGia: 200000,
  },
  khach: { ten: 'Nguyễn Thị Lan', sdt: '0901000001', email: 'lan@mail.vn', khachLe: false },
  giao: { hoTenNguoiNhan: 'Nguyễn Thị Lan', sdtNguoiNhan: '0901000001', diaChi: 'Số 12 ngõ 45 Nguyễn Chí Thanh, Hà Nội' },
  sanPham: [
    { id: 1, maSpct: 'SP02-XDG-3U-G5', tenSanPham: 'Astrox 99', mauSac: 'Xanh dương', trongLuong: '3U', chuVi: 'G5', soLuong: 1, donGia: 900000, giaGoc: 1000000, phanTramGiam: 10, thanhTien: 900000 },
    { id: 2, maSpct: 'SP06-DEN-4U-G5', tenSanPham: 'Cước Yonex', mauSac: 'Đen', trongLuong: null, chuVi: null, soLuong: 1, donGia: 100000, giaGoc: 100000, phanTramGiam: 0, thanhTien: 100000 },
  ],
  daThanhToan: 0,
  qrUrl: 'data:image/png;base64,AAAA',
  goc: 'http://localhost:5173',
}
const TAI_QUAY = {
  ...GIAO_HANG,
  hd: { ...GIAO_HANG.hd, id: 1, maHoaDon: 'HD01', loaiDon: 1, tenLoaiDon: 'Tại quầy', trangThai: 6, phiVanChuyen: 0, donViVanChuyen: null, maPhieuGiamGia: null, tenPhieuGiamGia: null, loaiPhieuGiamGia: null, giaTriPhieuGiamGia: null, soTienGiam: 0, thanhTien: 1000000 },
  khach: { ten: 'Khách vãng lai', sdt: null, email: null, khachLe: true },
  giao: { hoTenNguoiNhan: null, sdtNguoiNhan: null, diaChi: null },
  daThanhToan: 1000000,
}

describe('Bản in hóa đơn (PDF)', () => {
  it('KHÔNG còn khối "Lịch sử thanh toán" (cô: "phần này không cần in ra trong hóa đơn")', () => {
    const html = taoHtmlHoaDon({ ...GIAO_HANG, daThanhToan: 930000 })
    expect(html).not.toContain('Lịch sử thanh toán')
    expect(html).not.toContain('Chờ xử lý')
    expect(html).not.toMatch(/<th>Phương thức<\/th>/)
  })

  it('giữ các phần: thông tin đơn, khách hàng, giao hàng, sản phẩm, tổng tiền và mã QR', () => {
    const html = taoHtmlHoaDon(GIAO_HANG)
    for (const t of ['Thông tin đơn hàng', 'Khách hàng', 'Giao hàng', 'Sản phẩm', 'Tổng tiền']) expect(html).toContain(`<h2>${t}</h2>`)
    expect(html).toContain('class="qr"')
    expect(html).toContain('src="data:image/png;base64,AAAA"')
    expect(html).toContain('HÓA ĐƠN BÁN HÀNG')
    expect(html).toContain('window.print()')
  })

  it('ghi mã phiếu đã dùng và giảm bao nhiêu % (cô: "sử dụng mã nào, giảm bao nhiêu phần trăm in vào đây")', () => {
    const html = taoHtmlHoaDon(GIAO_HANG)
    expect(html).toContain('Giảm giá (mã VCH7QX2YN – giảm 10%, tối đa 200.000 ₫)')
    expect(html).toContain('- 100.000 ₫')
  })

  it('phiếu giảm theo số tiền thì ghi số tiền; không dùng phiếu thì chỉ ghi "Giảm giá"', () => {
    const soTien = taoHtmlHoaDon({ ...GIAO_HANG, hd: { ...GIAO_HANG.hd, loaiPhieuGiamGia: 2, giaTriPhieuGiamGia: 50000, giamToiDaPhieuGiamGia: null, soTienGiam: 50000 } })
    expect(soTien).toContain('Giảm giá (mã VCH7QX2YN – giảm 50.000 ₫)')
    const khong = taoHtmlHoaDon(TAI_QUAY)
    expect(khong).toContain('<span>Giảm giá</span>')
    expect(khong).not.toContain('(mã ')
  })

  it('đơn giao hàng in phí vận chuyển và logo đơn vị vận chuyển (có nhãn chữ dự phòng khi chưa có file logo)', () => {
    const html = taoHtmlHoaDon(GIAO_HANG)
    expect(html).toContain('<span>Phí vận chuyển</span><span>30.000 ₫</span>')
    expect(html).toContain('<img class="vc-logo" src="http://localhost:5173/van-chuyen/ghn.png"')
    expect(html).toContain('onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'inline-block\'"')
    expect(html).toContain('<span class="vc-badge" style="display:none">GHN</span>')
    expect(html).toContain('Giao Hàng Nhanh')
    expect(html).toContain('<b>Vận chuyển:</b>')
  })

  it('đơn vị vận chuyển là GHTK thì in logo ghtk; đơn vị lạ chỉ in nhãn chữ; chưa ghi thì dấu —', () => {
    const ghtk = taoHtmlHoaDon({ ...GIAO_HANG, hd: { ...GIAO_HANG.hd, donViVanChuyen: 'GHTK' } })
    expect(ghtk).toContain('/van-chuyen/ghtk.png')
    expect(ghtk).toContain('Giao Hàng Tiết Kiệm')
    const la = taoHtmlHoaDon({ ...GIAO_HANG, hd: { ...GIAO_HANG.hd, donViVanChuyen: 'J&T Express' } })
    expect(la).not.toContain('<img class="vc-logo"')
    expect(la).toContain('<span class="vc-badge">J&amp;T Express</span>')
    const chuaCo = taoHtmlHoaDon({ ...GIAO_HANG, hd: { ...GIAO_HANG.hd, donViVanChuyen: '' } })
    expect(chuaCo).toContain('<b>Vận chuyển:</b> —')
    expect(htmlVanChuyen('')).toBe('—')
  })

  it('đơn tại quầy: không in phí vận chuyển, không đơn vị vận chuyển; ghi nhận hàng tại cửa hàng (cô: "tại quầy sao lại có phí vận chuyển")', () => {
    const html = taoHtmlHoaDon(TAI_QUAY)
    expect(html).not.toContain('Phí vận chuyển')
    expect(html).not.toContain('Vận chuyển:')
    expect(html).not.toContain('<img class="vc-logo"') // (tên class vẫn có trong phần CSS nên chỉ kiểm tra thẻ ảnh)
    expect(html).not.toContain('class="vc-badge"')
    expect(html).toContain('Nhận hàng tại cửa hàng')
    expect(html).not.toContain('Người nhận')
  })

  it('khách lẻ: chỉ in "Khách vãng lai", không số điện thoại, không email (cô: "khách lẻ tại sao có số điện thoại")', () => {
    const html = taoHtmlHoaDon(TAI_QUAY)
    expect(html).toContain('<b>Tên:</b> Khách vãng lai')
    expect(html).not.toContain('<b>SĐT:</b>')
    expect(html).not.toContain('<b>Email:</b>')
    // khách có hồ sơ thì vẫn in đủ
    const co = taoHtmlHoaDon(GIAO_HANG)
    expect(co).toContain('<b>SĐT:</b> 0901000001')
    expect(co).toContain('<b>Email:</b> lan@mail.vn')
  })

  it('dòng sản phẩm có đợt giảm giá hiện giá gốc gạch ngang và -x%', () => {
    const html = taoHtmlHoaDon(GIAO_HANG)
    expect(html).toContain('<s>1.000.000 ₫</s><br>900.000 ₫ <span class="gg">-10%</span>')
    // dòng không giảm giá thì chỉ có đơn giá
    expect(html).toContain('<td class="r">100.000 ₫</td>')
  })

  it('dòng "Còn phải trả" chỉ có khi còn nợ và đơn chưa hủy', () => {
    expect(taoHtmlHoaDon({ ...GIAO_HANG, daThanhToan: 0 })).toContain('Còn phải trả')
    expect(taoHtmlHoaDon({ ...GIAO_HANG, daThanhToan: 930000 })).not.toContain('Còn phải trả')
    expect(taoHtmlHoaDon({ ...GIAO_HANG, hd: { ...GIAO_HANG.hd, trangThai: 0 }, daThanhToan: 0 })).not.toContain('Còn phải trả')
  })

  it('trạng thái "Giao hàng thất bại" in đúng nhãn', () => {
    expect(taoHtmlHoaDon({ ...GIAO_HANG, hd: { ...GIAO_HANG.hd, trangThai: TT.GIAO_THAT_BAI } })).toContain('<b>Trạng thái:</b> Giao hàng thất bại')
  })

  it('thoát ký tự HTML trong dữ liệu khách nhập (không chèn được thẻ vào bản in)', () => {
    const html = taoHtmlHoaDon({
      ...GIAO_HANG,
      hd: { ...GIAO_HANG.hd, ghiChu: '<script>alert(1)</script>' },
      khach: { ...GIAO_HANG.khach, ten: '<img src=x onerror=alert(1)>' },
      giao: { ...GIAO_HANG.giao, diaChi: 'Số 1 "ngõ" & <b>ngách</b>' },
    })
    expect(html).not.toContain('<script>alert(1)')
    expect(html).not.toContain('<img src=x')
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
    expect(html).toContain('Số 1 &quot;ngõ&quot; &amp; &lt;b&gt;ngách&lt;/b&gt;')
    expect(esc(null)).toBe('')
  })

  it('chạy được với dữ liệu trống (không lỗi, không "undefined")', () => {
    const html = taoHtmlHoaDon()
    expect(html).toContain('HÓA ĐƠN BÁN HÀNG')
    expect(html).not.toContain('undefined')
    expect(html).not.toContain('NaN')
  })
})
