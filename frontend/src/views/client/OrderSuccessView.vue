<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import api from '../../services/api'
import { docDonHang, ghiDonHang } from '../../utils/donHangLocal'
import { formatMoney } from '../../utils/format'
import { layPhien } from '../../services/phien'

const route = useRoute()
const maHoaDon = computed(() => route.query.ma || 'HD0000')
// Khách đã đăng nhập xem đơn trong "Đơn hàng của tôi"; khách vãng lai mới sang trang Tra cứu (cô review 14/11)
const laKhachDangNhap = computed(() => layPhien()?.nguoiDung?.vaiTro === 'KHACH_HANG')
const linkXemDon = computed(() =>
  laKhachDangNhap.value
    ? { path: '/shop/tai-khoan', query: { tab: 'orders', don: maHoaDon.value } }
    : { path: '/shop/tra-cuu', query: { ma: maHoaDon.value } },
)

// Đơn chọn chuyển khoản VietQR: hiện mã QR (tự điền số tiền + nội dung chuyển khoản = mã đơn)
const donLuu = computed(() => docDonHang().find((o) => o.maHoaDon === maHoaDon.value) || null)
const donBackend = ref(null)
const thanhToanQR = computed(() => {
  if (donLuu.value) return donLuu.value.phuongThucThanhToan === 'QR'
  return Number(donBackend.value?.idPhuongThucThanhToan) === 2
})
const soTien = computed(() => {
  if (donLuu.value?.tongThanhToan != null) return Math.round(Number(donLuu.value.tongThanhToan) || 0)
  return Math.round(Number(donBackend.value?.thanhTien) || 0)
})
const nganHang = ref(null)
const qrLoi = ref(false)
const daThanhToan = ref(false)
// Góp ý 14/11 (video 22:22 [04:52–08:06]): mã QR có hạn 5 phút; quá hạn chưa nhận tiền thì đơn tạm treo, khách đổi được sang COD
const HAN_PHUT = 5
const batDau = ref(donLuu.value?.ngayTao ? new Date(donLuu.value.ngayTao).getTime() : Date.now())
const bayGio = ref(Date.now())
const conLaiMs = computed(() => Math.max(0, batDau.value + HAN_PHUT * 60000 - bayGio.value))
const hetHan = computed(() => conLaiMs.value === 0)
const conLaiText = computed(() => {
  const giay = Math.ceil(conLaiMs.value / 1000)
  return `${String(Math.floor(giay / 60)).padStart(2, '0')}:${String(giay % 60).padStart(2, '0')}`
})
const daDoiCod = ref(false)
const dangDoiCod = ref(false)
const loiDoiCod = ref('')
const choThanhToan = computed(() => thanhToanQR.value && !daThanhToan.value && !daDoiCod.value)
let timer = null
let dongHo = null

async function doiSangCod() {
  if (dangDoiCod.value) return
  dangDoiCod.value = true
  loiDoiCod.value = ''
  try {
    await api.post(`/cua-hang/don-hang/${encodeURIComponent(maHoaDon.value)}/doi-sang-cod`, { sdt: donLuu.value?.sdtKhachHang })
    daDoiCod.value = true
    clearInterval(timer)
    clearInterval(dongHo)
    ghiDonHang(docDonHang().map((o) => (o.maHoaDon === maHoaDon.value ? { ...o, phuongThucThanhToan: 'COD' } : o)))
  } catch (e) {
    loiDoiCod.value = e?.message || 'Không đổi được cách thanh toán. Vui lòng liên hệ cửa hàng.'
  } finally {
    dangDoiCod.value = false
  }
}
const daSaoChep = ref('')
// Chế độ giả lập (demo đồ án): QR trỏ tới trang "ngân hàng giả", bấm xác nhận là coi như đã chuyển tiền
const giaLap = ref(false)
const qrGiaLap = ref('')
const lanIp = ref('')
const laLocalhost = ['localhost', '127.0.0.1'].includes(window.location.hostname)
// Mở web bằng localhost thì điện thoại không vào được -> thay bằng IP mạng LAN của máy tính (backend tự dò)
const linkGiaLap = computed(() => {
  const goc = laLocalhost && lanIp.value
    ? `${window.location.protocol}//${lanIp.value}${window.location.port ? `:${window.location.port}` : ''}`
    : window.location.origin
  return `${goc}/thanh-toan-gia-lap?ma=${encodeURIComponent(maHoaDon.value)}`
})
// Link để mở ngay trên máy này (localhost luôn vào được)
const linkTrenMayNay = computed(() => `${window.location.origin}/thanh-toan-gia-lap?ma=${encodeURIComponent(maHoaDon.value)}`)

const qrUrl = computed(() => {
  const nh = nganHang.value
  if (!nh) return ''
  const q = new URLSearchParams({
    amount: String(soTien.value),
    addInfo: maHoaDon.value,
    accountName: nh.tenChuTaiKhoan || '',
  })
  return `https://img.vietqr.io/image/${encodeURIComponent(nh.nganHang)}-${encodeURIComponent(nh.soTaiKhoan)}-compact2.png?${q}`
})

async function saoChep(giaTri, nhan) {
  try {
    await navigator.clipboard.writeText(String(giaTri))
    daSaoChep.value = nhan
    setTimeout(() => (daSaoChep.value = ''), 1500)
  } catch {
    // trình duyệt chặn clipboard: bỏ qua
  }
}

const daHuyHetHang = ref(false)

// Hỏi backend mỗi 4 giây xem ngân hàng đã báo tiền vào chưa
async function kiemTraThanhToan() {
  try {
<<<<<<< HEAD
    // Tra cứu công khai bắt buộc SĐT đặt hàng (lấy từ đơn vừa lưu trên máy)
    const d = (await api.get('/cua-hang/tra-cuu', { params: { maHoaDon: maHoaDon.value, sdt: donLuu.value?.sdtKhachHang } })).data
    if (d?.hanChuyenKhoan) batDau.value = new Date(d.hanChuyenKhoan).getTime() - HAN_PHUT * 60000 // mốc theo máy chủ
    if (d?.idPhuongThucThanhToan === 1) {
      daDoiCod.value = true // đã đổi sang COD (ở tab khác / lần trước)
      clearInterval(timer)
    } else if (d?.daThanhToanDu) {
      daThanhToan.value = true
      clearInterval(timer)
    } else if (d?.trangThai === 0 || d?.trangThai === 7) {
      // Đơn bị hủy do hết hàng hoặc đang hoàn tiền
      daHuyHetHang.value = true
=======
    const params = { maHoaDon: maHoaDon.value }
    if (donLuu.value?.sdtKhachHang) params.sdt = donLuu.value.sdtKhachHang
    const d = (await api.get('/cua-hang/tra-cuu', { params })).data
    if (d?.daThanhToanDu) {
>>>>>>> 42dfc10 (sửa bán hàng online)
      daThanhToan.value = true
      clearInterval(timer)
    }
  } catch {
    // mất kết nối tạm thời: lần sau thử lại
  }
}
onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(dongHo)
})

onMounted(async () => {
  if (!donLuu.value && maHoaDon.value) {
    try {
      donBackend.value = (await api.get('/cua-hang/tra-cuu', { params: { maHoaDon: maHoaDon.value } })).data
    } catch {
      // bỏ qua
    }
  }
  if (!thanhToanQR.value) return
  dongHo = setInterval(() => (bayGio.value = Date.now()), 1000)
  kiemTraThanhToan()
  timer = setInterval(kiemTraThanhToan, 4000)
  try {
    // Backend bật thanh toán giả lập -> dùng QR giả; tắt (404) -> dùng VietQR thật bên dưới
    const r = await api.get(`/cua-hang/thanh-toan-gia-lap/${encodeURIComponent(maHoaDon.value)}`)
    lanIp.value = r.data?.diaChiLan || ''
    giaLap.value = true
    qrGiaLap.value = await QRCode.toDataURL(linkGiaLap.value, { width: 280, margin: 1 })
    return
  } catch {
    // không bật giả lập
  }
  try {
    nganHang.value = (await api.get('/cua-hang/thong-tin-thanh-toan')).data
  } catch {
    qrLoi.value = true
  }
})
</script>

<template>
  <div class="order-success-view py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-7 col-md-9 text-center">
          <div class="card border-0 shadow-sm rounded-4 p-5">
            <div class="mb-4">
              <div
                class="rounded-circle d-inline-flex align-items-center justify-content-center"
                :class="daHuyHetHang ? 'bg-danger-subtle text-danger' : choThanhToan ? 'bg-warning-subtle text-warning' : 'bg-success-subtle text-success'"
                style="width: 80px; height: 80px;"
              >
                <i class="bi display-4" :class="daHuyHetHang ? 'bi-exclamation-triangle' : choThanhToan ? 'bi-hourglass-split' : 'bi-check-lg'"></i>
              </div>
            </div>

            <h3 class="fw-bold text-dark mb-2">{{ daHuyHetHang ? 'Sản phẩm đã hết hàng' : choThanhToan ? (hetHan ? 'Đơn hàng đang tạm treo' : 'Đơn hàng đang chờ thanh toán') : daThanhToan ? 'Thanh toán thành công!' : 'Đặt Hàng Thành Công!' }}</h3>
            <p class="text-muted mb-4">
              <template v-if="daHuyHetHang">
                Hệ thống đã nhận thanh toán, tuy nhiên sản phẩm trong đơn vừa hết hàng do đơn hàng khác thanh toán trước.
              </template>
              <template v-else-if="choThanhToan">
                Đơn hàng của bạn chỉ được xử lý <strong>sau khi cửa hàng nhận được tiền</strong>. Vui lòng quét mã QR bên dưới để thanh toán.
              </template>
              <template v-else>
                Cảm ơn bạn đã tin tưởng và mua sắm tại <strong>FootStyle Sports</strong>.
              </template>
            </p>

            <div class="alert alert-light border p-3 rounded-3 mb-4 text-start">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted small">Mã đơn hàng:</span>
                <strong class="fs-5 text-primary">{{ maHoaDon }}</strong>
              </div>
              <div class="d-flex justify-content-between align-items-center">
                <span class="text-muted small">Trạng thái:</span>
                <span class="badge" :class="daHuyHetHang ? 'bg-danger text-white' : choThanhToan ? 'bg-warning text-dark' : 'bg-primary text-white'">{{ daHuyHetHang ? 'Đã hủy do hết hàng' : choThanhToan ? (hetHan ? 'Đơn treo - chưa thanh toán' : 'Chờ thanh toán') : 'Chờ xác nhận' }}</span>
              </div>
<<<<<<< HEAD
              <div class="d-flex justify-content-between align-items-center">
                <span class="text-muted small">Thời gian giao dự kiến:</span>
                <span class="fw-semibold">{{ daHuyHetHang ? 'Đang chờ xử lý' : '2 - 3 ngày làm việc' }}</span>
              </div>
=======
>>>>>>> 42dfc10 (sửa bán hàng online)
            </div>

            <!-- Thanh toán chuyển khoản VietQR -->
            <div v-if="daHuyHetHang" class="alert alert-warning text-start mb-4">
              <div class="fw-bold mb-1"><i class="bi bi-exclamation-triangle-fill text-warning me-2"></i>Đã ghi nhận thanh toán nhưng sản phẩm vừa hết hàng</div>
              <p class="mb-0 small text-muted">
                Hệ thống đã nhận được số tiền <strong>{{ formatMoney(soTien) }}</strong> của bạn. Tuy nhiên, sản phẩm trong đơn vừa hết hàng tồn kho do đơn hàng khác thanh toán trước. Nhân viên tư vấn của FootStyle sẽ liên hệ qua số điện thoại <strong>{{ donLuu?.sdtKhachHang }}</strong> để hỗ trợ hoàn tiền 100% hoặc đổi sang sản phẩm khác tương đương.
              </p>
            </div>
            <div v-else-if="daThanhToan" class="alert alert-success text-start mb-4"><i class="bi bi-check-circle-fill me-2"></i>Hệ thống đã nhận được <strong>{{ formatMoney(soTien) }}</strong> cho đơn này. Cửa hàng sẽ sớm xác nhận và chuẩn bị giao hàng.</div>
            <div v-else-if="daDoiCod" class="alert alert-success text-start mb-4" data-testid="osv-da-doi-cod">
              <i class="bi bi-check-circle-fill me-2"></i>Đơn đã chuyển sang <strong>thanh toán khi nhận hàng (COD)</strong>. Bạn trả {{ formatMoney(soTien) }} cho nhân viên giao hàng.
            </div>
            <div v-else-if="thanhToanQR && hetHan" class="osv-treo mb-4" data-testid="osv-het-han">
              <div class="fw-bold mb-1"><i class="bi bi-hourglass-bottom me-2"></i>Đã quá {{ HAN_PHUT }} phút, cửa hàng chưa nhận được tiền</div>
              <p class="small mb-3">Đơn đang tạm treo. Nếu bạn đã chuyển khoản, vui lòng chờ hệ thống cập nhật. Chưa chuyển thì có thể đổi sang trả tiền khi nhận hàng.</p>
              <button type="button" class="btn btn-primary rounded-pill px-4 fw-semibold" :disabled="dangDoiCod" data-testid="osv-doi-cod" @click="doiSangCod">
                <span v-if="dangDoiCod" class="spinner-border spinner-border-sm me-1"></span> Đổi sang thanh toán khi nhận hàng (COD)
              </button>
              <p v-if="loiDoiCod" class="text-danger small mt-2 mb-0">{{ loiDoiCod }}</p>
            </div>
            <div v-else-if="thanhToanQR" class="osv-qr mb-4">
              <div class="osv-dem" data-testid="osv-dem-nguoc"><i class="bi bi-clock me-1"></i>Mã QR còn hiệu lực <strong>{{ conLaiText }}</strong></div>
              <div class="osv-qr-title"><i class="bi bi-qr-code-scan me-2"></i>Quét mã để thanh toán</div>
              <template v-if="giaLap">
                <img :src="qrGiaLap" alt="Mã QR thanh toán giả lập" class="osv-qr-img" />
                <div class="osv-qr-info">
                  <div class="osv-row"><span>Số tiền</span><strong class="text-danger">{{ formatMoney(soTien) }}</strong></div>
                  <div class="osv-row"><span>Nội dung</span><strong class="text-primary">{{ maHoaDon }}</strong></div>
                </div>
                <div class="mt-3 text-center">
                  <a :href="linkTrenMayNay" target="_blank" rel="noopener noreferrer" class="btn btn-outline-primary btn-sm rounded-pill px-3 fw-bold">
                    <i class="bi bi-box-arrow-up-right me-1"></i> Mở trang xác nhận chuyển tiền (Demo)
                  </a>
                </div>
              </template>
              <div v-else-if="qrLoi" class="small text-danger py-3">
                Không tải được thông tin thanh toán. Vui lòng liên hệ cửa hàng để được hướng dẫn chuyển khoản.
              </div>
              <template v-else-if="nganHang">
                <img :src="qrUrl" alt="Mã VietQR thanh toán" class="osv-qr-img" />
                <div class="osv-qr-info">
                  <div class="osv-row">
                    <span>Ngân hàng</span><strong>{{ nganHang.nganHang }}</strong>
                  </div>
                  <div class="osv-row">
                    <span>Số tài khoản</span>
                    <strong>{{ nganHang.soTaiKhoan }}</strong>
                    <button type="button" class="osv-copy" @click="saoChep(nganHang.soTaiKhoan, 'stk')">
                      {{ daSaoChep === 'stk' ? 'Đã chép' : 'Chép' }}
                    </button>
                  </div>
                  <div class="osv-row">
                    <span>Chủ tài khoản</span><strong>{{ nganHang.tenChuTaiKhoan }}</strong>
                  </div>
                  <div class="osv-row">
                    <span>Số tiền</span><strong class="text-danger">{{ formatMoney(soTien) }}</strong>
                    <button type="button" class="osv-copy" @click="saoChep(soTien, 'tien')">
                      {{ daSaoChep === 'tien' ? 'Đã chép' : 'Chép' }}
                    </button>
                  </div>
                  <div class="osv-row">
                    <span>Nội dung</span><strong class="text-primary">{{ maHoaDon }}</strong>
                    <button type="button" class="osv-copy" @click="saoChep(maHoaDon, 'nd')">
                      {{ daSaoChep === 'nd' ? 'Đã chép' : 'Chép' }}
                    </button>
                  </div>
                </div>
                <p class="osv-qr-note">Mở app ngân hàng, quét mã QR; số tiền và nội dung sẽ tự điền. Cửa hàng xác nhận đơn sau khi nhận được tiền.</p>
              </template>
              <div v-else class="small text-muted py-3">Đang tải mã QR...</div>
              <div v-if="nganHang && !qrLoi" class="osv-wait"><span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Đang chờ hệ thống nhận tiền, trang sẽ tự cập nhật...</div>
            </div>
            <p class="small text-muted mb-4">
              Nhân viên tư vấn của FootStyle sẽ liên hệ qua số điện thoại của bạn để xác nhận đơn hàng trước khi gửi cho đơn vị vận chuyển.
            </p>

            <div class="d-flex flex-wrap justify-content-center gap-3">
              <RouterLink :to="linkXemDon" class="btn btn-outline-primary rounded-pill px-4 fw-semibold" data-testid="osv-xem-don">
                <i class="bi bi-search me-1"></i> {{ laKhachDangNhap ? 'Xem đơn trong Đơn hàng của tôi' : 'Tra cứu đơn hàng này' }}
              </RouterLink>

              <RouterLink
                to="/shop/san-pham"
                class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm"
              >
                <i class="bi bi-arrow-left me-1"></i> Tiếp tục mua sắm
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.osv-qr {
  border: 1px solid #e5e8ee;
  border-radius: 16px;
  padding: 1rem 1.1rem 0.9rem;
  background: #fff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}
.osv-qr-title {
  font-weight: 700;
  color: #0d6efd;
  margin-bottom: 0.6rem;
}
.osv-qr-img {
  width: 100%;
  max-width: 300px;
  height: auto;
  border-radius: 12px;
  border: 1px solid #eef0f4;
}
.osv-qr-info {
  margin-top: 0.8rem;
  text-align: left;
  font-size: 0.88rem;
}
.osv-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.35rem 0;
  border-top: 1px dashed #eef0f4;
}
.osv-row span {
  flex: 0 0 105px;
  color: #8a94a6;
}
.osv-row strong {
  flex: 1;
  word-break: break-all;
}
.osv-copy {
  border: 1px solid #cfe0ff;
  background: #eef4ff;
  color: #0d6efd;
  font-size: 0.75rem;
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
}
.osv-link {
  word-break: break-all;
  user-select: all;
}
.osv-wait {
  margin-top: 0.6rem;
  font-size: 0.82rem;
  color: #a56a00;
}
.osv-qr-note {
  margin: 0.6rem 0 0;
  font-size: 0.76rem;
  color: #9aa3b2;
}
.osv-dem {
  display: inline-block;
  margin-bottom: 0.6rem;
  padding: 0.25rem 0.8rem;
  font-size: 0.85rem;
  color: #a56a00;
  background: #fffbeb;
  border-radius: 999px;
}
.osv-treo {
  padding: 1rem 1.1rem;
  text-align: left;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 16px;
}
</style>
