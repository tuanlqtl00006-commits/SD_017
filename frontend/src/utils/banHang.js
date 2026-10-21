// Hàm phụ cho Bán hàng tại quầy: hóa đơn chờ lưu trên trình duyệt, giỏ hàng, tiền khách đưa, mã QR VietQR.
// Backend luôn tính lại giá / phiếu / tồn kho khi thanh toán; các hàm ở đây chỉ để hiển thị và chặn sớm lỗi thường gặp.

export const SO_HOA_DON_CHO_TOI_DA = 5
export const SO_LUONG_TOI_DA = 999

/* ===================== Hóa đơn chờ (localStorage, theo từng nhân viên) ===================== */

const khoa = (idNhanVien) => `fs_hoa_don_cho_${idNhanVien ?? 'chung'}`

export function docHoaDonCho(idNhanVien) {
  try {
    const s = window.localStorage.getItem(khoa(idNhanVien))
    const d = s ? JSON.parse(s) : null
    if (d && Array.isArray(d.ds)) return d
  } catch {
    // dữ liệu hỏng hoặc trình duyệt chặn bộ nhớ: bắt đầu lại
  }
  return { ds: [], dangChon: null }
}

export function ghiHoaDonCho(idNhanVien, duLieu) {
  try {
    window.localStorage.setItem(khoa(idNhanVien), JSON.stringify(duLieu))
  } catch {
    // không lưu được thì hóa đơn chờ chỉ còn tới khi tải lại trang
  }
}

/** Số hóa đơn chờ đang có sản phẩm (cảnh báo khi giao ca). */
export function soHoaDonCho(idNhanVien) {
  return docHoaDonCho(idNhanVien).ds.filter((h) => h.gio?.length).length
}

/** Hóa đơn chờ mới, đánh số nhỏ nhất chưa dùng (Hóa đơn 1, 2...). */
export function taoHoaDonMoi(ds = []) {
  const daDung = new Set(ds.map((h) => h.so))
  let so = 1
  while (daDung.has(so)) so++
  return {
    id: `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`,
    so,
    ten: `Hóa đơn ${so}`,
    khach: null,
    gio: [],
    maPhieu: '',
    tuDongPhieu: true, // true: tự chọn phiếu giảm nhiều nhất; false: nhân viên đã tự chọn / bỏ phiếu
    ghiChu: '',
    phuongThuc: 'TIEN_MAT',
    tienMat: '',
    chuyenKhoan: '',
    maGiaoDich: '',
    taoLuc: new Date().toISOString(),
  }
}

/* ===================== Giỏ hàng ===================== */

/** Thêm sản phẩm (kết quả GET /ban-hang/san-pham) vào giỏ: { gio, loi }. Không cho vượt tồn kho / 999. */
export function themVaoGio(gio, sp, soLuong = 1) {
  const sl = Math.max(1, Math.floor(Number(soLuong) || 1))
  const ton = Number(sp.tonKho) || 0
  const co = gio.find((d) => d.idBienThe === sp.id)
  const moi = (co ? co.soLuong : 0) + sl
  if (ton <= 0) return { gio, loi: `${sp.tenSanPham} (${sp.phanLoai || sp.ma}) đã hết hàng.` }
  if (moi > ton) return { gio, loi: `${sp.tenSanPham} chỉ còn ${ton} sản phẩm trong kho.` }
  if (moi > SO_LUONG_TOI_DA) return { gio, loi: `Mỗi sản phẩm tối đa ${SO_LUONG_TOI_DA}.` }
  if (co) return { gio: gio.map((d) => (d.idBienThe === sp.id ? { ...d, soLuong: moi, tonKho: ton } : d)), loi: '' }
  return {
    gio: [
      ...gio,
      {
        idBienThe: sp.id,
        ma: sp.ma,
        ten: sp.tenSanPham,
        phanLoai: sp.phanLoai,
        anh: sp.anh,
        giaGoc: Number(sp.giaGoc) || 0,
        phanTramGiam: Number(sp.phanTramGiam) || 0,
        donGia: Number(sp.donGia) || 0,
        soLuong: sl,
        tonKho: ton,
      },
    ],
    loi: '',
  }
}

/** Đổi số lượng một dòng: { gio, loi }. Số lượng < 1 giữ 1 (xóa dòng bằng nút xóa). */
export function doiSoLuong(gio, idBienThe, soLuong) {
  const d = gio.find((x) => x.idBienThe === idBienThe)
  if (!d) return { gio, loi: '' }
  let sl = Math.floor(Number(soLuong))
  let loi = ''
  if (!Number.isFinite(sl) || sl < 1) {
    sl = 1
    loi = 'Số lượng tối thiểu là 1. Muốn bỏ sản phẩm hãy bấm nút xóa.'
  }
  if (sl > d.tonKho) {
    sl = d.tonKho
    loi = `${d.ten} chỉ còn ${d.tonKho} sản phẩm trong kho.`
  }
  if (sl > SO_LUONG_TOI_DA) {
    sl = SO_LUONG_TOI_DA
    loi = `Mỗi sản phẩm tối đa ${SO_LUONG_TOI_DA}.`
  }
  return { gio: gio.map((x) => (x.idBienThe === idBienThe ? { ...x, soLuong: sl } : x)), loi }
}

export const xoaKhoiGio = (gio, idBienThe) => gio.filter((d) => d.idBienThe !== idBienThe)

export const tongSoLuong = (gio) => gio.reduce((s, d) => s + d.soLuong, 0)
export const tongTienHang = (gio) => gio.reduce((s, d) => s + d.donGia * d.soLuong, 0)
/** Tiền đã giảm nhờ đợt giảm giá (giá gốc - giá bán) để hiện cho khách thấy. */
export const tienGiamDot = (gio) => gio.reduce((s, d) => s + Math.max(0, d.giaGoc - d.donGia) * d.soLuong, 0)

/** Cập nhật giá trong giỏ theo backend báo (lỗi GIA_THAY_DOI: [{ idBienThe, donGiaMoi }]). */
export function capNhatGia(gio, items = []) {
  const moi = Object.fromEntries(items.map((x) => [x.idBienThe, Number(x.donGiaMoi)]))
  return gio.map((d) => (d.idBienThe in moi ? { ...d, donGia: moi[d.idBienThe] } : d))
}

/* ===================== Tiền khách trả ===================== */

const so = (v) => (v === '' || v === null || v === undefined ? 0 : Math.max(0, Math.round(Number(v) || 0)))

/**
 * Theo phương thức: TIEN_MAT (chỉ tiền mặt), CHUYEN_KHOAN (chuyển đúng số phải trả), KET_HOP (một phần chuyển khoản, còn lại tiền mặt).
 * Trả về { phaiTra, tienMat, chuyenKhoan, tienThua, conThieu, loi }.
 */
export function tinhThanhToan({ tongTien, giam = 0, phuongThuc = 'TIEN_MAT', tienMat, chuyenKhoan }) {
  const phaiTra = Math.max(0, so(tongTien) - so(giam))
  let mat = so(tienMat)
  let ck = so(chuyenKhoan)
  if (phuongThuc === 'TIEN_MAT') ck = 0
  if (phuongThuc === 'CHUYEN_KHOAN') {
    ck = phaiTra
    mat = 0
  }
  let loi = ''
  if (ck > phaiTra) loi = 'Tiền chuyển khoản không được lớn hơn số tiền phải trả.'
  const conThieu = Math.max(0, phaiTra - mat - ck)
  if (!loi && conThieu > 0) loi = phuongThuc === 'TIEN_MAT' && mat === 0 ? 'Nhập tiền khách đưa.' : 'Khách trả chưa đủ.'
  const tienThua = loi ? 0 : mat - (phaiTra - ck)
  return { phaiTra, tienMat: mat, chuyenKhoan: ck, tienThua: Math.max(0, tienThua), conThieu, loi }
}

/** Gợi ý tiền khách đưa: đúng số + làm tròn lên 10k / 50k / 100k / 200k / 500k (bỏ trùng), tối đa 5 nút. */
export function goiYTienKhachDua(phaiTra) {
  const p = so(phaiTra)
  if (p <= 0) return []
  const ds = [p]
  for (const buoc of [10_000, 50_000, 100_000, 200_000, 500_000]) ds.push(Math.ceil(p / buoc) * buoc)
  return [...new Set(ds)].sort((a, b) => a - b).slice(0, 5)
}

/* ===================== Mã QR chuyển khoản (chuẩn VietQR / EMVCo) ===================== */

const tlv = (id, giaTri) => {
  const v = String(giaTri)
  return `${id}${String(v.length).padStart(2, '0')}${v}`
}

/** CRC-16/CCITT-FALSE (đa thức 0x1021, khởi đầu 0xFFFF) - mã kiểm tra cuối chuỗi VietQR. */
export function crc16(chuoi) {
  let crc = 0xffff
  for (const b of new TextEncoder().encode(chuoi)) {
    crc ^= b << 8
    for (let i = 0; i < 8; i++) crc = crc & 0x8000 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff
  }
  return crc.toString(16).toUpperCase().padStart(4, '0')
}

/** Bỏ dấu, chỉ giữ chữ / số / khoảng trắng, tối đa 25 ký tự (nội dung chuyển khoản ngân hàng chấp nhận). */
export function noiDungChuyenKhoan(s) {
  return String(s ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^0-9A-Za-z ]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 25)
}

/** Chuỗi VietQR (quét bằng app ngân hàng sẽ điền sẵn tài khoản, số tiền, nội dung). */
export function chuoiVietQR({ bin, soTaiKhoan, soTien, noiDung }) {
  const nguoiNhan = tlv('00', bin) + tlv('01', soTaiKhoan)
  const taiKhoan = tlv('00', 'A000000727') + tlv('01', nguoiNhan) + tlv('02', 'QRIBFTTA')
  let s = tlv('00', '01') + tlv('01', soTien ? '12' : '11') + tlv('38', taiKhoan) + tlv('53', '704')
  if (soTien) s += tlv('54', String(Math.round(soTien)))
  s += tlv('58', 'VN')
  const nd = noiDungChuyenKhoan(noiDung)
  if (nd) s += tlv('62', tlv('08', nd))
  s += '6304'
  return s + crc16(s)
}
