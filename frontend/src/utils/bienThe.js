// Logic tạo biến thể ở trang THÊM sản phẩm (tách riêng để kiểm thử).
//
// Mỗi dòng biến thể: { key, idMauSac, idTrongLuong, idChuVi, soLuong, giaBan, chon }
//  - Vợt: số biến thể = số màu × số trọng lượng × số chu vi (vd 4 × 3 × 2 = 24).
//  - Danh mục khác: chỉ theo màu (trọng lượng, chu vi = mục "Không áp dụng") -> số biến thể = số màu.
// Hiển thị mặc định NHÓM THEO MÀU như 2 video mẫu (mỗi màu một bảng + phần ảnh riêng của màu đó);
// có nút chuyển sang "Bảng chung" (một bảng phẳng). Muốn mặc định là bảng chung thì đổi hằng số bên dưới thành 'chung'.
export const CHE_DO_HIEN_THI_MAC_DINH = 'mau'

export const GIA_TOI_THIEU = 1000
export const GIA_TOI_DA = 1_000_000_000
export const SO_LUONG_TOI_DA = 1_000_000
export const TOI_DA_ANH_MOI_MAU = 6 // giống backend (SanPhamService.TOI_DA_ANH_MOI_MAU)

/**
 * Sinh tổ hợp biến thể theo đúng thứ tự Màu -> Trọng lượng -> Chu vi.
 * cu: các dòng đã có (giữ lại số lượng / giá / ô tích khi bấm "Tạo biến thể" lần nữa).
 * macDinh: số lượng / giá mặc định cho dòng MỚI.
 */
export function taoBienThe(dsMau = [], dsTl = [], dsCv = [], cu = [], macDinh = {}) {
  const daCo = new Map(cu.map((r) => [r.key, r]))
  const ketQua = []
  const daThay = new Set()
  for (const m of dsMau) for (const t of dsTl) for (const c of dsCv) {
    const key = `${m}-${t}-${c}`
    if (daThay.has(key)) continue // chọn trùng thì chỉ tạo 1 dòng
    daThay.add(key)
    ketQua.push(
      daCo.get(key) ?? {
        key,
        idMauSac: m,
        idTrongLuong: t,
        idChuVi: c,
        soLuong: macDinh.soLuong ?? '',
        giaBan: macDinh.giaBan ?? '',
        chon: true,
      },
    )
  }
  return ketQua
}

/** Số biến thể sẽ tạo và công thức để người dùng tự đếm: { so: 24, congThuc: '4 màu × 3 trọng lượng × 2 chu vi = 24' } */
export function congThucSoBienThe(soMau, soTl, soCv, laVot) {
  if (!laVot) return { so: soMau, congThuc: `${soMau} màu = ${soMau} biến thể` }
  const so = soMau * soTl * soCv
  return { so, congThuc: `${soMau} màu × ${soTl} trọng lượng × ${soCv} chu vi = ${so} biến thể` }
}

/**
 * Nhóm các dòng theo màu: [{ idMauSac, rows: [...] }].
 * Thứ tự màu theo thuTuMau (thứ tự người dùng chọn), màu không có trong thuTuMau xếp sau theo lần xuất hiện.
 */
export function nhomTheoMau(rows = [], thuTuMau = []) {
  const nhom = new Map()
  for (const r of rows) {
    if (!nhom.has(r.idMauSac)) nhom.set(r.idMauSac, [])
    nhom.get(r.idMauSac).push(r)
  }
  const ketQua = []
  for (const id of thuTuMau) if (nhom.has(id)) ketQua.push({ idMauSac: id, rows: nhom.get(id) })
  for (const [id, ds] of nhom) if (!thuTuMau.includes(id)) ketQua.push({ idMauSac: id, rows: ds })
  return ketQua
}

const trong = (v) => v === '' || v === null || v === undefined

/** Lỗi của ô "số lượng" (null nếu hợp lệ). */
export function loiSoLuong(v) {
  if (trong(v)) return 'Nhập số lượng.'
  const n = Number(v)
  if (!Number.isInteger(n) || n < 0) return 'Số lượng là số nguyên từ 0.'
  if (n > SO_LUONG_TOI_DA) return 'Số lượng tối đa 1.000.000.'
  return null
}

/** Lỗi của ô "giá bán" (null nếu hợp lệ). Giá 0 / để trống đều báo lỗi rõ. */
export function loiGiaBan(v) {
  if (trong(v)) return 'Nhập giá bán.'
  const n = Number(v)
  if (Number.isNaN(n) || n <= 0) return 'Giá bán phải lớn hơn 0 ₫.'
  if (n < GIA_TOI_THIEU) return 'Giá bán tối thiểu 1.000 ₫.'
  if (n > GIA_TOI_DA) return 'Giá bán tối đa 1.000.000.000 ₫.'
  return null
}

/** Kiểm tra mọi dòng ĐANG TÍCH: { soLoi, loiTheoKey: { key: { soLuong, giaBan } }, thongBao } */
export function kiemTraBienThe(rows = []) {
  const loiTheoKey = {}
  let soLoi = 0
  for (const r of rows) {
    if (!r.chon) continue
    const l = { soLuong: loiSoLuong(r.soLuong), giaBan: loiGiaBan(r.giaBan) }
    if (l.soLuong || l.giaBan) {
      loiTheoKey[r.key] = l
      soLoi++
    }
  }
  const thongBao = soLoi
    ? `Có ${soLoi} biến thể chưa hợp lệ: số lượng là số nguyên từ 0, giá bán tối thiểu 1.000 ₫ (ô bị viền đỏ).`
    : ''
  return { soLoi, loiTheoKey, thongBao }
}

/**
 * Áp số lượng / giá mặc định cho các dòng đang tích. Trả về { ok, soDong, loi }.
 * Ô mặc định để trống thì không đổi cột đó; giá trị mặc định sai thì không áp gì và báo lỗi.
 */
export function apDungMacDinh(rows = [], macDinh = {}) {
  const coSl = !trong(macDinh.soLuong)
  const coGia = !trong(macDinh.giaBan)
  if (!coSl && !coGia) return { ok: false, soDong: 0, loi: 'Nhập số lượng mặc định hoặc giá bán mặc định rồi bấm Áp dụng.' }
  if (coSl && loiSoLuong(macDinh.soLuong)) return { ok: false, soDong: 0, loi: `Số lượng mặc định: ${loiSoLuong(macDinh.soLuong)}` }
  if (coGia && loiGiaBan(macDinh.giaBan)) return { ok: false, soDong: 0, loi: `Giá bán mặc định: ${loiGiaBan(macDinh.giaBan)}` }
  const chon = rows.filter((r) => r.chon)
  if (!chon.length) return { ok: false, soDong: 0, loi: 'Chưa có biến thể nào được tích chọn.' }
  for (const r of chon) {
    if (coSl) r.soLuong = Number(macDinh.soLuong)
    if (coGia) r.giaBan = Number(macDinh.giaBan)
  }
  return { ok: true, soDong: chon.length, loi: '' }
}

/**
 * Ảnh theo màu gửi lên backend: [{ idMauSac, anh: [url...] }] (anh[0] = ảnh chính của màu).
 * anhTheoMau: { idMàu: [url...] }; chiMau: chỉ gửi các màu này (màu còn biến thể được tích). boQuaRong: không gửi màu chưa có ảnh.
 */
export function payloadAnhTheoMau(anhTheoMau = {}, chiMau = null, boQuaRong = true) {
  const ketQua = []
  for (const [k, ds] of Object.entries(anhTheoMau)) {
    const idMauSac = Number(k)
    if (chiMau && !chiMau.includes(idMauSac)) continue
    const anh = (ds || []).filter(Boolean)
    if (boQuaRong && !anh.length) continue
    ketQua.push({ idMauSac, anh })
  }
  return ketQua
}

/** Dữ liệu biến thể gửi kèm khi thêm sản phẩm (chỉ dòng đang tích). */
export function payloadBienThe(rows = []) {
  return rows
    .filter((r) => r.chon)
    .map((r) => ({
      idMauSac: r.idMauSac,
      idTrongLuong: r.idTrongLuong,
      idChuVi: r.idChuVi,
      giaBan: Number(r.giaBan),
      soLuongTon: Number(r.soLuong),
    }))
}

/**
 * Ảnh sản phẩm là BẮT BUỘC (cô: "ảnh đang chưa có mà tại sao vẫn thành công"): phải có ít nhất 1 ảnh chung của sản phẩm,
 * hoặc mọi màu đang dùng đều có ảnh riêng (ảnh của màu áp dụng cho mọi biến thể cùng màu).
 * mauCanAnh: id các màu đang có biến thể (thêm mới: màu của dòng đang tích; sửa: màu sản phẩm đã có biến thể).
 * @returns {{ ok: boolean, mauThieu: number[] }} mauThieu: các màu chưa có ảnh riêng (rỗng nếu không có màu nào)
 */
export function kiemTraAnhBatBuoc({ anhChung = [], anhTheoMau = {}, mauCanAnh = [] } = {}) {
  const dem = (ds) => (ds || []).filter(Boolean).length
  const coChung = dem(anhChung) > 0
  const mauThieu = mauCanAnh.filter((m) => dem(anhTheoMau[m]) === 0)
  const moiMauCoAnh = mauCanAnh.length > 0 && mauThieu.length === 0
  return { ok: coChung || moiMauCoAnh, mauThieu }
}
