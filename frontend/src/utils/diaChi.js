// Hàm phụ cho địa chỉ hành chính Việt Nam (sau sáp nhập 07/2025: chỉ còn Tỉnh/Thành phố + Phường/Xã, không còn Quận/Huyện).

/** Bỏ dấu, chữ thường, bỏ tiền tố "Thành phố"/"Tỉnh" để so khớp tên (vd 'Thành phố Hà Nội' ~ 'Hà Nội'). */
export function chuanHoaTenDonVi(ten = '') {
  return String(ten)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/^(thanh pho|tinh)\s+/, '')
    .replace(/\s+/g, ' ')
    .trim()
}

/** Ghép địa chỉ thành một dòng, bỏ phần trống: 'Số 1, Phường A, Thành phố B'. Cũ có Quận/Huyện thì vẫn hiện. */
export function ghepDiaChi(diaChi) {
  const { diaChiCuThe, phuongXa, quanHuyen, tinhThanh, tinhThanhPho } = diaChi ?? {}
  return [diaChiCuThe, phuongXa, quanHuyen, tinhThanh ?? tinhThanhPho]
    .map((x) => (x == null ? '' : String(x).trim()))
    .filter(Boolean)
    .join(', ')
}

/* ====== Địa chỉ ghi trên thẻ CCCD (đơn vị hành chính CŨ) -> địa chỉ mới (sau sáp nhập 07/2025) ====== */

const TIEN_TO_QUAN = /^(quận|huyện|thị xã|thành phố|tp\.?|q\.|h\.|tx\.)\s*/i
const TIEN_TO_PHUONG = /^(phường|xã|thị trấn|p\.|x\.|tt\.)\s*/i

/**
 * Tách chuỗi "Nơi thường trú" trên thẻ CCCD: các phần cách nhau bằng dấu phẩy, đọc từ cuối lên:
 *   Tỉnh/Thành phố <- [Quận/Huyện/Thị xã] <- [Phường/Xã/Thị trấn] <- phần còn lại là địa chỉ cụ thể (số nhà, đường, thôn...).
 * 'Số 3, Tổ 5, Phường Văn Quán, Quận Hà Đông, Thành phố Hà Nội'
 *   -> { chiTiet: 'Số 3, Tổ 5', phuongXa: 'Phường Văn Quán', quanHuyen: 'Quận Hà Đông', tinhThanh: 'Thành phố Hà Nội' }
 * Chỉ có 1 phần (không có dấu phẩy) thì coi cả chuỗi là địa chỉ cụ thể.
 */
export function phanTichDiaChiCccd(chuoi = '') {
  const phan = String(chuoi ?? '').split(',').map((x) => x.trim()).filter(Boolean)
  const kq = { chiTiet: '', phuongXa: '', quanHuyen: '', tinhThanh: '' }
  if (phan.length <= 1) {
    kq.chiTiet = phan[0] ?? ''
    return kq
  }
  kq.tinhThanh = phan.pop()
  if (phan.length > 1 && TIEN_TO_QUAN.test(phan[phan.length - 1])) kq.quanHuyen = phan.pop()
  if (phan.length > 1 && TIEN_TO_PHUONG.test(phan[phan.length - 1])) kq.phuongXa = phan.pop()
  else if (phan.length === 1 && TIEN_TO_PHUONG.test(phan[0])) kq.phuongXa = phan.pop() // chỉ còn phường/xã, không có số nhà
  kq.chiTiet = phan.join(', ')
  return kq
}

/** Khóa so khớp tên tỉnh: bỏ dấu, bỏ tiền tố (Tỉnh / Thành phố / TP.), bỏ mọi khoảng trắng và dấu câu. 'Bà Rịa - Vũng Tàu' ~ 'bariavungtau'. */
const khoaTinh = (ten = '') =>
  chuanHoaTenDonVi(String(ten).replace(/^\s*tp\.?\s+/i, ''))
    .replace(/[^a-z0-9]/g, '')

/**
 * Tỉnh/Thành phố CŨ (ghi trên thẻ CCCD) -> tỉnh/thành phố MỚI sau sáp nhập 01/07/2025 (63 -> 34 đơn vị).
 * Chỉ liệt kê các tỉnh bị sáp nhập hoặc đổi tên; tỉnh giữ nguyên (Hà Nội, Cao Bằng, Nghệ An...) khớp thẳng theo tên.
 * Khóa và giá trị đều ở dạng khoaTinh().
 */
export const TINH_CU_SANG_MOI = {
  hagiang: 'tuyenquang',
  yenbai: 'laocai',
  backan: 'thainguyen',
  vinhphuc: 'phutho',
  hoabinh: 'phutho',
  bacgiang: 'bacninh',
  thaibinh: 'hungyen',
  haiduong: 'haiphong',
  hanam: 'ninhbinh',
  namdinh: 'ninhbinh',
  quangbinh: 'quangtri',
  quangnam: 'danang',
  kontum: 'quangngai',
  binhdinh: 'gialai',
  ninhthuan: 'khanhhoa',
  daknong: 'lamdong',
  binhthuan: 'lamdong',
  phuyen: 'daklak',
  binhduong: 'hochiminh',
  bariavungtau: 'hochiminh',
  binhphuoc: 'dongnai',
  longan: 'tayninh',
  soctrang: 'cantho',
  haugiang: 'cantho',
  bentre: 'vinhlong',
  travinh: 'vinhlong',
  tiengiang: 'dongthap',
  baclieu: 'camau',
  kiengiang: 'angiang',
  thuathienhue: 'hue',
}

const boTienToPhuong = (ten) => chuanHoaTenDonVi(String(ten).replace(TIEN_TO_PHUONG, ''))

/**
 * Đổi địa chỉ trên thẻ CCCD (cũ, có Quận/Huyện) thành địa chỉ MỚI (Tỉnh/Thành phố -> Phường/Xã) để điền sẵn vào form:
 *  - Tỉnh/Thành phố và Phường/Xã được đối chiếu với danh sách hành chính mới (diaChiHanhChinhService), khớp thì dùng đúng tên mới.
 *  - Không khớp (đơn vị đã sáp nhập / đổi tên) hoặc không có mạng thì GIỮ NGUYÊN chữ trên thẻ (form sẽ nhắc "địa chỉ cũ", chọn lại sau),
 *    kèm tên Quận/Huyện cũ ở cuối địa chỉ cụ thể để người dùng biết đường chọn.
 * Tỉnh bị sáp nhập thì tự đổi sang tỉnh mới (bảng TINH_CU_SANG_MOI). Phường/Xã không đổi tự động được vì nhiều phường cũ gộp thành phường mới.
 * Trả về { tinhThanh, phuongXa, diaChiCuThe, khopTinh, khopPhuong, daDoiTinh }.
 */
export async function diaChiMoiTuCccd(chuoi, dichVu) {
  const p = phanTichDiaChiCccd(chuoi)
  let tinhThanh = p.tinhThanh
  let phuongXa = p.phuongXa
  let khopTinh = false
  let khopPhuong = false
  let daDoiTinh = false
  try {
    if (p.tinhThanh) {
      const dsTinh = await dichVu.layTinhThanh()
      const khoaCu = khoaTinh(p.tinhThanh)
      const khoaMoi = TINH_CU_SANG_MOI[khoaCu] ?? khoaCu // tỉnh bị sáp nhập thì đổi sang tỉnh mới (Thái Bình -> Hưng Yên)
      const tinh = dsTinh.find((t) => khoaTinh(t.name) === khoaMoi)
      if (tinh) {
        tinhThanh = tinh.name
        khopTinh = true
        daDoiTinh = khoaMoi !== khoaCu
        if (p.phuongXa) {
          const dsPhuong = await dichVu.layPhuongXa(tinh.code)
          const khoa = chuanHoaTenDonVi(p.phuongXa)
          let w = dsPhuong.find((x) => chuanHoaTenDonVi(x.name) === khoa)
          if (!w) {
            const khoaTen = boTienToPhuong(p.phuongXa) // 'Thị trấn Xuân Mai' ~ 'Xã Xuân Mai' (chỉ nhận khi duy nhất)
            const trung = dsPhuong.filter((x) => boTienToPhuong(x.name) === khoaTen)
            if (khoaTen && trung.length === 1) w = trung[0]
          }
          if (w) {
            phuongXa = w.name
            khopPhuong = true
          }
        }
      }
    }
  } catch {
    // không tải được danh sách (mất mạng): giữ nguyên chữ trên thẻ
  }
  const phanChiTiet = [p.chiTiet]
  if (p.quanHuyen && !khopPhuong) phanChiTiet.push(p.quanHuyen)
  return { tinhThanh, phuongXa, diaChiCuThe: phanChiTiet.filter(Boolean).join(', '), khopTinh, khopPhuong, daDoiTinh }
}
