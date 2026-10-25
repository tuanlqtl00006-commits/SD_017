// "Gợi ý AI từ ảnh" trên web khách: phân tích ảnh NGAY TRÊN TRÌNH DUYỆT (không gửi ảnh lên máy chủ, không gọi dịch vụ AI ngoài).
// 1. Tách nền: màu viền ảnh coi là nền (ảnh chụp sản phẩm trên nền trắng / trong suốt), bỏ các điểm gần màu nền.
// 2. Đặc trưng ảnh: biểu đồ màu 4x4x4 (64 ô) đã chuẩn hóa + vài màu nổi bật kèm tỉ lệ.
// 3. Xếp hạng sản phẩm: độ giống biểu đồ màu với ảnh sản phẩm + độ khớp màu nổi bật với màu các phân loại đang bán.

const SO_MUC = 4
const BUOC = 256 / SO_MUC

export const MAU_CO_TEN = [
  { ten: 'Đen', tu: ['den'], rgb: [25, 25, 28] },
  { ten: 'Trắng', tu: ['trang'], rgb: [242, 242, 240] },
  { ten: 'Xám', tu: ['xam', 'ghi'], rgb: [128, 128, 132] },
  { ten: 'Bạc', tu: ['bac'], rgb: [190, 192, 196] },
  { ten: 'Đỏ', tu: ['do'], rgb: [210, 35, 42] },
  { ten: 'Cam', tu: ['cam'], rgb: [245, 120, 30] },
  { ten: 'Vàng', tu: ['vang'], rgb: [240, 200, 30] },
  { ten: 'Xanh lá', tu: ['xanh la', 'xanh luc', 'xanh reu'], rgb: [40, 160, 70] },
  { ten: 'Xanh dương', tu: ['xanh duong', 'xanh', 'xanh navy', 'xanh bien', 'navy'], rgb: [35, 90, 210] },
  { ten: 'Xanh ngọc', tu: ['xanh ngoc', 'ngoc'], rgb: [20, 170, 170] },
  { ten: 'Tím', tu: ['tim'], rgb: [130, 60, 190] },
  { ten: 'Hồng', tu: ['hong'], rgb: [235, 90, 160] },
  { ten: 'Nâu', tu: ['nau'], rgb: [120, 75, 40] },
]

export function boDau(s) {
  return String(s ?? '')
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
}

/** Màu có tên ứng với tên màu phân loại ("Đỏ đen" -> Đỏ; "Xanh navy" -> Xanh dương). Không biết thì null. */
export function mauTheoTen(tenMau) {
  const t = boDau(tenMau)
  if (!t) return null
  let tot = null
  for (const m of MAU_CO_TEN) {
    for (const tu of m.tu) {
      if ((t === tu || t.startsWith(tu + ' ')) && (!tot || tu.length > tot.do)) tot = { mau: m, do: tu.length }
    }
  }
  return tot?.mau ?? null
}

function khoangCach(a, b) {
  // RGB có trọng số gần với cảm nhận của mắt (đỏ-lục quan trọng hơn lam)
  const dr = a[0] - b[0]
  const dg = a[1] - b[1]
  const db = a[2] - b[2]
  return Math.sqrt(2 * dr * dr + 4 * dg * dg + 3 * db * db) / 3
}

/** Tên màu gần nhất với một màu RGB. */
export function tenMauGanNhat(rgb) {
  let tot = MAU_CO_TEN[0]
  let min = Infinity
  for (const m of MAU_CO_TEN) {
    const d = khoangCach(rgb, m.rgb)
    if (d < min) {
      min = d
      tot = m
    }
  }
  return tot
}

/**
 * Đặc trưng màu của ảnh từ mảng điểm RGBA (ImageData.data), kích thước rong x cao.
 * Trả về { bieuDo: number[64] (tổng = 1), mauNoiBat: [{ ten, rgb, tiLe }], soDiem } hoặc null nếu ảnh gần như chỉ có nền.
 */
export function docDacTrung(data, rong, cao) {
  const nen = mauNen(data, rong, cao)
  const bieuDo = new Array(SO_MUC ** 3).fill(0)
  const tong = bieuDo.map(() => [0, 0, 0])
  let soDiem = 0
  for (let i = 0; i < rong * cao; i++) {
    const r = data[i * 4]
    const g = data[i * 4 + 1]
    const b = data[i * 4 + 2]
    if (data[i * 4 + 3] < 128) continue
    if (nen && khoangCach([r, g, b], nen) < 28) continue
    const o = Math.floor(r / BUOC) * SO_MUC * SO_MUC + Math.floor(g / BUOC) * SO_MUC + Math.floor(b / BUOC)
    bieuDo[o]++
    tong[o][0] += r
    tong[o][1] += g
    tong[o][2] += b
    soDiem++
  }
  if (soDiem < Math.max(16, rong * cao * 0.02)) return null
  const gopTheoTen = new Map()
  bieuDo.forEach((n, o) => {
    if (!n) return
    const rgb = tong[o].map((v) => Math.round(v / n))
    const m = tenMauGanNhat(rgb)
    const cu = gopTheoTen.get(m.ten) ?? { ten: m.ten, rgb: m.rgb, tiLe: 0 }
    cu.tiLe += n / soDiem
    gopTheoTen.set(m.ten, cu)
  })
  const mauNoiBat = [...gopTheoTen.values()].filter((m) => m.tiLe >= 0.08).sort((a, b) => b.tiLe - a.tiLe).slice(0, 3)
  return { bieuDo: bieuDo.map((n) => n / soDiem), mauNoiBat, soDiem }
}

/** Màu nền = màu chiếm đa số ở viền ảnh (null nếu viền trong suốt hoặc lẫn lộn, vd ảnh chụp cận cảnh). */
function mauNen(data, rong, cao) {
  const dem = new Map()
  let tongVien = 0
  const xet = (x, y) => {
    const i = (y * rong + x) * 4
    tongVien++
    if (data[i + 3] < 128) return
    const khoa = [data[i], data[i + 1], data[i + 2]].map((v) => Math.floor(v / 32)).join(',')
    const cu = dem.get(khoa) ?? { n: 0, r: 0, g: 0, b: 0 }
    cu.n++
    cu.r += data[i]
    cu.g += data[i + 1]
    cu.b += data[i + 2]
    dem.set(khoa, cu)
  }
  for (let x = 0; x < rong; x++) {
    xet(x, 0)
    xet(x, cao - 1)
  }
  for (let y = 1; y < cao - 1; y++) {
    xet(0, y)
    xet(rong - 1, y)
  }
  let tot = null
  for (const v of dem.values()) if (!tot || v.n > tot.n) tot = v
  if (!tot || tot.n < tongVien * 0.5) return null
  return [tot.r / tot.n, tot.g / tot.n, tot.b / tot.n]
}

/** Độ giống hai biểu đồ màu (giao nhau, 0..1). */
export function doGiongBieuDo(a, b) {
  if (!a || !b) return 0
  let s = 0
  for (let i = 0; i < a.length; i++) s += Math.min(a[i], b[i])
  return s
}

/** Độ khớp giữa màu nổi bật của ảnh khách và danh sách tên màu phân loại (0..1) + tên phân loại khớp nhất. */
export function doKhopMau(mauNoiBat, tenMauPhanLoai) {
  let tot = { diem: 0, ten: null }
  for (const ten of tenMauPhanLoai || []) {
    const m = mauTheoTen(ten)
    if (!m) continue
    let diem = 0
    for (const nb of mauNoiBat || []) {
      const gan = Math.max(0, 1 - khoangCach(nb.rgb, m.rgb) / 120)
      diem += gan * nb.tiLe
    }
    if (diem > tot.diem) tot = { diem, ten }
  }
  return { diem: Math.min(1, tot.diem), ten: tot.ten }
}

/**
 * Xếp hạng sản phẩm giống ảnh của khách.
 * sanPhams: [{ id, tenSanPham, bienThe: [{ tenMau, soLuongTon }], ... }]; dacTrungSanPham: { [id]: dacTrung | null }.
 * Chỉ lấy sản phẩm còn hàng, độ giống >= nguong; trả về [{ sp, doGiong (0..100), mauKhop }].
 */
export function xepHangGoiY(dacTrungKhach, sanPhams, dacTrungSanPham, { soLuong = 8, nguong = 25 } = {}) {
  if (!dacTrungKhach) return []
  const kq = []
  for (const sp of sanPhams || []) {
    const conHang = (sp.bienThe || []).filter((b) => (b.soLuongTon ?? 0) > 0)
    if (!conHang.length) continue
    const anh = doGiongBieuDo(dacTrungKhach.bieuDo, dacTrungSanPham?.[sp.id]?.bieuDo)
    const mau = doKhopMau(dacTrungKhach.mauNoiBat, [...new Set(conHang.map((b) => b.tenMau).filter(Boolean))])
    const coAnh = !!dacTrungSanPham?.[sp.id]
    const diem = coAnh ? 0.55 * anh + 0.45 * mau.diem : mau.diem
    const doGiong = Math.round(diem * 100)
    if (doGiong >= nguong) kq.push({ sp, doGiong, mauKhop: mau.diem >= 0.3 ? mau.ten : null })
  }
  return kq.sort((a, b) => b.doGiong - a.doGiong).slice(0, soLuong)
}
