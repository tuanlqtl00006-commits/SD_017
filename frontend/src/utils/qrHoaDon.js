// Tiện ích hỗ trợ quét mã hóa đơn (QR code và mã vạch)
// Hỗ trợ cả 3 hình thức: camera trực tiếp, file ảnh và máy quét cầm tay.

/**
 * Trích xuất mã hóa đơn từ chuỗi quét được.
 * Xử lý linh hoạt các trường hợp:
 * - Mã thuần túy: "HD16", "HD0001", "16"
 * - Đường dẫn URL: "http://localhost:5173/hoa-don/16" -> "16"
 * - URL có tham số: "http://.../hoa-don?ma=HD16" -> "HD16"
 * - Chuỗi có tiền tố: "Mã hóa đơn: HD16" -> "HD16"
 */
export function trichXuatMaHoaDon(chuoi = '') {
  if (!chuoi) return ''
  let text = String(chuoi).replace(/^\uFEFF/, '').trim()
  if (!text) return ''

  // 1. Nếu là đường dẫn URL
  if (text.includes('http://') || text.includes('https://') || text.startsWith('/')) {
    try {
      const url = new URL(text, 'http://localhost')
      // Tham số ?ma=... hoặc ?maHoaDon=...
      const maParam = url.searchParams.get('ma') || url.searchParams.get('maHoaDon')
      if (maParam) return maParam.trim()

      // Khớp đường dẫn dạng /hoa-don/:id hoặc /hoa-don/:ma
      const mPath = /\/hoa-don\/([a-zA-Z0-9_-]+)/i.exec(url.pathname)
      if (mPath && mPath[1]) return mPath[1].trim()
    } catch {
      // Bỏ qua nếu không parse được URL
    }
  }

  // 2. Nếu có tiền tố như "Mã HD: HD16", "Mã: HD16", "Mã hóa đơn: HD16"
  const mPrefix = /(?:mã(?:\s+hóa\s+đơn|\s+hd)?[:\s]+)([a-zA-Z0-9_-]+)/i.exec(text)
  if (mPrefix && mPrefix[1]) return mPrefix[1].trim()

  // 3. Nếu chứa định dạng mã HD (vd: HD12, HD001, HD_20261020_01)
  const mHd = /\b(HD[a-zA-Z0-9_-]*)\b/i.exec(text)
  if (mHd && mHd[1]) return mHd[1].trim()

  return text
}

/**
 * Quét mã QR / mã vạch từ file ảnh:
 * Thử native BarcodeDetector trước (quét được cả mã vạch 1D & QR),
 * nếu không có hoặc không thấy thì fallback sang jsQR.
 */
export async function quetMaTuAnh(file, jsQR) {
  const bitmap = await taiAnh(file)

  // Cách 1: Native BarcodeDetector (nếu trình duyệt hỗ trợ)
  if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
    try {
      const detector = new window.BarcodeDetector({
        formats: ['qr_code', 'code_128', 'code_39', 'ean_13', 'ean_8', 'upc_a', 'upc_e', 'codabar', 'itf'],
      })
      const barcodes = await detector.detect(bitmap)
      if (barcodes && barcodes.length > 0 && barcodes[0].rawValue) {
        return barcodes[0].rawValue
      }
    } catch {
      // Tiếp tục fallback jsQR nếu BarcodeDetector lỗi hoặc chưa hỗ trợ format
    }
  }

  // Cách 2: jsQR với nhiều kích thước ảnh để tăng khả năng nhận diện
  if (jsQR) {
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

