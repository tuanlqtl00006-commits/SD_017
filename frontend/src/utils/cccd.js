// Đọc mã QR trên thẻ Căn cước công dân (CCCD gắn chip).
// Nội dung mã QR là một chuỗi ngăn cách bằng dấu "|":
//   Số CCCD | Số CMND cũ | Họ và tên | Ngày sinh (ddMMyyyy) | Giới tính | Nơi thường trú | Ngày cấp (ddMMyyyy)
// Ví dụ: 001099012345|123456789|Nguyễn Văn An|20051999|Nam|Số 3, Phường Hà Đông, Hà Nội|15032021

/** 'ddMMyyyy' -> 'yyyy-mm-dd' (rỗng nếu không đúng định dạng / ngày không có thật). */
export function ngayDdMMyyyyThanhIso(chuoi = '') {
  const m = /^(\d{2})(\d{2})(\d{4})$/.exec(String(chuoi).trim())
  if (!m) return ''
  const [, d, mo, y] = m
  const date = new Date(Number(y), Number(mo) - 1, Number(d))
  if (date.getFullYear() !== Number(y) || date.getMonth() !== Number(mo) - 1 || date.getDate() !== Number(d)) return ''
  return `${y}-${mo}-${d}`
}

/** 'NGUYỄN VĂN AN' -> 'Nguyễn Văn An' (trên thẻ tên viết hoa toàn bộ). */
export function vietHoaTen(ten = '') {
  return String(ten)
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => w.charAt(0).toLocaleUpperCase('vi') + w.slice(1).toLocaleLowerCase('vi'))
    .join(' ')
}

/**
 * Tách nội dung mã QR của CCCD. Ném Error (tiếng Việt) nếu không phải mã QR của căn cước công dân.
 * Trả về { soCccd, soCmnd, hoTen, ngaySinh ('yyyy-mm-dd' hoặc ''), gioiTinh ('Nam' | 'Nữ' | ''), diaChi, ngayCap }.
 */
export function docMaQrCccd(noiDung) {
  const text = String(noiDung ?? '').replace(/^\uFEFF/, '').trim()
  if (!text) throw new Error('Chưa có nội dung mã QR.')
  const phan = text.split('|').map((x) => x.trim())
  if (phan.length < 5 || !/^\d{12}$/.test(phan[0])) {
    throw new Error('Đây không phải mã QR trên căn cước công dân (cần số CCCD 12 chữ số).')
  }
  // Có ô CMND cũ (9 số hoặc để trống) thì họ tên ở vị trí 2; mã QR không có ô này thì họ tên ở vị trí 1
  const coCmnd = phan[1] === '' || /^\d{9}$/.test(phan[1])
  const i = coCmnd ? 2 : 1
  const hoTen = vietHoaTen(phan[i] || '')
  if (!hoTen || /^\d+$/.test(hoTen)) throw new Error('Không đọc được họ tên trong mã QR.')
  const gt = (phan[i + 2] || '').toLowerCase()
  return {
    soCccd: phan[0],
    soCmnd: coCmnd && /^\d{9}$/.test(phan[1]) ? phan[1] : '',
    hoTen,
    ngaySinh: ngayDdMMyyyyThanhIso(phan[i + 1]),
    gioiTinh: gt === 'nam' ? 'Nam' : gt === 'nữ' || gt === 'nu' ? 'Nữ' : '',
    diaChi: phan[i + 3] || '',
    ngayCap: ngayDdMMyyyyThanhIso(phan[i + 4]),
  }
}

/** File ảnh -> nội dung mã QR (quét từ ảnh chụp thẻ / ảnh mã QR). Trả về null nếu không thấy mã QR. */
export async function quetQrTuAnh(file, jsQR) {
  const bitmap = await taiAnh(file)
  // Thử nhiều cỡ ảnh: ảnh chụp cả tấm thẻ thì mã QR nhỏ, ảnh lớn quá thì chậm
  for (const canh of [1600, 1000, 640, 2400]) {
    const ti = Math.min(1, canh / Math.max(bitmap.width, bitmap.height))
    const w = Math.max(1, Math.round(bitmap.width * ti))
    const h = Math.max(1, Math.round(bitmap.height * ti))
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    ctx.drawImage(bitmap, 0, 0, w, h)
    const kq = jsQR(ctx.getImageData(0, 0, w, h).data, w, h, { inversionAttempts: 'attemptBoth' })
    if (kq?.data) return kq.data
  }
  return null
}

function taiAnh(file) {
  if (typeof createImageBitmap === 'function') return createImageBitmap(file)
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Không đọc được file ảnh.'))
    }
    img.src = url
  })
}
