import { ref } from 'vue'

/*
 * Kênh báo thay đổi dữ liệu (SSE GET /api/su-kien, backend SuKienService): ghi dữ liệu ở đâu (bán quầy, đặt online, đổi trạng thái đơn,
 * sửa sản phẩm / giảm giá...) thì màn liên quan ở mọi tab, mọi máy tự tải lại ngay, không cần F5.
 *  - Tin chỉ có { chuDe: ['hoa-don', 'ton-kho'...], nguon }; màn nhận tin tự gọi API của nó để lấy dữ liệu mới.
 *  - Mỗi trình duyệt giữ MỘT kết nối dù mở nhiều tab (trình duyệt chỉ cho 6 kết nối tới một máy chủ, mỗi tab một kết nối thì
 *    mở nhiều tab sẽ treo): tab giữ khóa (Web Locks) mở kết nối rồi chuyển tin sang các tab khác qua BroadcastChannel;
 *    tab đó đóng thì tab khác tự nhận khóa và nối thay.
 *  - Tab vừa ghi (nguon = TAB_ID, gửi qua header X-Tab-Id trong services/api.js) bỏ qua tin của chính nó vì màn đã tự cập nhật.
 *  - Mất kết nối (backend tắt / khởi động lại) thì tự nối lại; nối lại được thì báo chủ đề TAT_CA để mọi màn tải lại một lần (có thể đã lỡ tin).
 * Tên chủ đề khớp backend ChuDeSuKien.
 */

export const CHU_DE = Object.freeze({
  HOA_DON: 'hoa-don',
  TON_KHO: 'ton-kho',
  SAN_PHAM: 'san-pham',
  GIAM_GIA: 'giam-gia',
  PHIEU_GIAM_GIA: 'phieu-giam-gia',
  KHACH_HANG: 'khach-hang',
  NHAN_VIEN: 'nhan-vien',
  GIAO_CA: 'giao-ca',
  LICH_LAM_VIEC: 'lich-lam-viec',
  CAI_DAT: 'cai-dat',
})
/** Chủ đề đặc biệt: vừa nối lại sau khi mất kết nối, mọi màn đang nghe đều tải lại. */
export const TAT_CA = '*'
export const TIEU_DE_TAB = 'X-Tab-Id'
export const TAB_ID = taoMaTab()

/** Kết nối (của tab này hoặc tab đang giữ kết nối chung) có đang sống không. */
export const dangKetNoi = ref(false)

const DUONG_DAN = '/api/su-kien'
const TEN_KHOA = 'footstyle-su-kien'
const TEN_KENH = 'footstyle-su-kien'
const NOI_LAI_TOI_DA_MS = 5_000
/** Mở kết nối mà chừng này chưa nhận "san-sang" (yêu cầu treo ở proxy khi backend đang tắt / bật) thì bỏ, nối lại. */
const CHO_SAN_SANG_MS = 10_000
/** Máy chủ gửi "ping" 20 giây một lần; im lặng quá chừng này coi như kết nối đã chết, nối lại. */
const IM_LANG_TOI_DA_MS = 45_000

const nguoiNghe = new Set()
let daBatDau = false
let kenh = null
let nguon = null // EventSource (chỉ ở tab giữ kết nối)
let henNoiLai = null
let henCanh = null
let choNoiLaiMs = 1000
let daMoLanNao = false

function taoMaTab() {
  try {
    if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  } catch {
    // trang mở bằng http://IP (không phải localhost) không có randomUUID
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/** Chuyển tin tới các màn đang nghe đúng chủ đề (bỏ tin do chính tab này ghi). Xuất ra để kiểm thử. */
export function nhanTin(tin) {
  if (!tin || !Array.isArray(tin.chuDe)) return
  if (tin.nguon && tin.nguon === TAB_ID) return
  const tatCa = tin.chuDe.includes(TAT_CA)
  for (const n of [...nguoiNghe]) {
    if (tatCa || !n.chuDe || tin.chuDe.some((c) => n.chuDe.has(c))) {
      try {
        n.cb(tin)
      } catch (e) {
        console.error('Lỗi khi xử lý tin thay đổi dữ liệu:', e)
      }
    }
  }
}

/** Nghe thay đổi của các chủ đề (rỗng / null = mọi chủ đề). Trả về hàm hủy nghe. */
export function nghe(chuDe, cb) {
  const n = { chuDe: chuDe?.length ? new Set(chuDe) : null, cb }
  nguoiNghe.add(n)
  batDau()
  return () => nguoiNghe.delete(n)
}

function batDau() {
  if (daBatDau || typeof window === 'undefined' || typeof window.EventSource === 'undefined') return
  daBatDau = true
  // Web Locks chỉ có ở trang an toàn (localhost / https); mở bằng http://IP (điện thoại cùng Wi-Fi) thì mỗi tab một kết nối
  if (typeof BroadcastChannel === 'undefined' || !globalThis.navigator?.locks?.request) {
    moKetNoi()
    return
  }
  kenh = new BroadcastChannel(TEN_KENH)
  kenh.onmessage = (e) => {
    const d = e.data || {}
    if (d.loai === 'tin') nhanTin(d.tin)
    else if (d.loai === 'trang-thai') dangKetNoi.value = !!d.song
    else if (d.loai === 'hoi' && nguon) kenh.postMessage({ loai: 'trang-thai', song: dangKetNoi.value })
  }
  kenh.postMessage({ loai: 'hoi' }) // tab mới mở: hỏi tab đang giữ kết nối xem kết nối có sống không
  // Chờ tới lượt giữ khóa; giữ tới khi đóng tab (Promise không bao giờ xong), trình duyệt tự nhả khóa khi tab đóng
  navigator.locks.request(TEN_KHOA, () => new Promise(() => moKetNoi())).catch(() => {
    kenh?.close()
    kenh = null
    moKetNoi()
  })
}

function chuyenTin(tin) {
  nhanTin(tin)
  kenh?.postMessage({ loai: 'tin', tin })
}

function baoTrangThai(song) {
  dangKetNoi.value = song
  kenh?.postMessage({ loai: 'trang-thai', song })
}

/** Hẹn giờ canh: quá `ms` mà kết nối không có tin gì thì đóng và nối lại. */
function canhGac(es, ms) {
  clearTimeout(henCanh)
  henCanh = setTimeout(() => {
    if (nguon !== es) return
    baoTrangThai(false)
    moKetNoi()
  }, ms)
}

function moKetNoi() {
  clearTimeout(henNoiLai)
  clearTimeout(henCanh)
  nguon?.close()
  const es = new EventSource(DUONG_DAN)
  nguon = es
  canhGac(es, CHO_SAN_SANG_MS)
  es.addEventListener('san-sang', () => {
    const noiLai = daMoLanNao
    daMoLanNao = true
    choNoiLaiMs = 1000
    canhGac(es, IM_LANG_TOI_DA_MS)
    baoTrangThai(true)
    if (noiLai) chuyenTin({ chuDe: [TAT_CA] })
  })
  es.addEventListener('ping', () => canhGac(es, IM_LANG_TOI_DA_MS))
  es.addEventListener('thay-doi', (e) => {
    canhGac(es, IM_LANG_TOI_DA_MS)
    try {
      chuyenTin(JSON.parse(e.data))
    } catch {
      // tin hỏng: bỏ qua
    }
  })
  es.onerror = () => {
    if (nguon !== es) return
    baoTrangThai(false)
    if (es.readyState === 2) {
      // Máy chủ trả lỗi (backend tắt, proxy 502) thì trình duyệt KHÔNG tự nối lại: tự hẹn, chờ tăng dần tới 5 giây
      clearTimeout(henCanh)
      es.close()
      henNoiLai = setTimeout(moKetNoi, choNoiLaiMs)
      choNoiLaiMs = Math.min(choNoiLaiMs * 2, NOI_LAI_TOI_DA_MS)
    } else {
      // Trình duyệt đang tự nối lại: nếu yêu cầu đó treo thì hẹn giờ canh sẽ bỏ và nối lại
      canhGac(es, CHO_SAN_SANG_MS)
    }
  }
}
