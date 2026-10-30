<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import api from '../../services/api'
import { docDonHang } from '../../utils/donHangLocal'
import { formatMoney } from '../../utils/format'

const route = useRoute()
const maHoaDon = computed(() => route.query.ma || 'HD0000')

// Đơn chọn chuyển khoản VietQR: hiện mã QR (tự điền số tiền + nội dung chuyển khoản = mã đơn)
const donLuu = computed(() => docDonHang().find((o) => o.maHoaDon === maHoaDon.value) || null)
const thanhToanQR = computed(() => donLuu.value?.phuongThucThanhToan === 'QR')
const soTien = computed(() => Math.round(Number(donLuu.value?.tongThanhToan) || 0))
const nganHang = ref(null)
const qrLoi = ref(false)
const daThanhToan = ref(false)
const choThanhToan = computed(() => thanhToanQR.value && !daThanhToan.value)
let timer = null
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

// Hỏi backend mỗi 4 giây xem ngân hàng đã báo tiền vào chưa
async function kiemTraThanhToan() {
  try {
    const d = (await api.get('/cua-hang/tra-cuu', { params: { maHoaDon: maHoaDon.value } })).data
    if (d?.daThanhToanDu) {
      daThanhToan.value = true
      clearInterval(timer)
    }
  } catch {
    // mất kết nối tạm thời: lần sau thử lại
  }
}
onBeforeUnmount(() => clearInterval(timer))

onMounted(async () => {
  if (!thanhToanQR.value) return
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
                :class="choThanhToan ? 'bg-warning-subtle text-warning' : 'bg-success-subtle text-success'"
                style="width: 80px; height: 80px;"
              >
                <i class="bi display-4" :class="choThanhToan ? 'bi-hourglass-split' : 'bi-check-lg'"></i>
              </div>
            </div>

            <h3 class="fw-bold text-dark mb-2">{{ choThanhToan ? 'Đơn hàng đang chờ thanh toán' : daThanhToan ? 'Thanh toán thành công!' : 'Đặt Hàng Thành Công!' }}</h3>
            <p class="text-muted mb-4">
              <template v-if="choThanhToan">
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
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted small">Trạng thái:</span>
                <span class="badge bg-warning text-dark">{{ choThanhToan ? 'Chờ thanh toán' : 'Chờ xác nhận' }}</span>
              </div>
              <div class="d-flex justify-content-between align-items-center">
                <span class="text-muted small">Thời gian giao dự kiến:</span>
                <span class="fw-semibold">2 - 3 ngày làm việc</span>
              </div>
            </div>

            <!-- Thanh toán chuyển khoản VietQR -->
            <div v-if="daThanhToan" class="alert alert-success text-start mb-4"><i class="bi bi-check-circle-fill me-2"></i>Hệ thống đã nhận được <strong>{{ formatMoney(soTien) }}</strong> cho đơn này. Cửa hàng sẽ sớm xác nhận và chuẩn bị giao hàng.</div>
            <div v-else-if="thanhToanQR" class="osv-qr mb-4">
              <div class="osv-qr-title"><i class="bi bi-qr-code-scan me-2"></i>Quét mã để thanh toán</div>
              <template v-if="giaLap">
                <img :src="qrGiaLap" alt="Mã QR thanh toán giả lập" class="osv-qr-img" />
                <div class="osv-qr-info">
                  <div class="osv-row"><span>Số tiền</span><strong class="text-danger">{{ formatMoney(soTien) }}</strong></div>
                  <div class="osv-row"><span>Nội dung</span><strong class="text-primary">{{ maHoaDon }}</strong></div>
                </div>
                <p class="osv-qr-note">Dùng điện thoại quét mã bằng <strong>app Camera / Google Lens</strong> (không quét bằng Zalo) để mở trang thanh toán, rồi bấm <strong>Xác nhận chuyển tiền</strong>.</p>
                <p class="osv-qr-note">Hoặc nhập tay vào trình duyệt điện thoại: <code class="osv-link">{{ linkGiaLap }}</code></p>
                <p v-if="laLocalhost && !lanIp" class="osv-qr-note">
                  Không dò được IP mạng của máy tính: hãy mở web bằng địa chỉ IP (ví dụ <code>http://192.168.1.5:5173</code>) hoặc dùng nút bên dưới ngay trên máy này.
                </p>
                <a :href="linkTrenMayNay" target="_blank" rel="noopener" class="btn btn-outline-primary btn-sm rounded-pill px-3 mt-2">
                  <i class="bi bi-box-arrow-up-right me-1"></i>Mở trang thanh toán trên máy này
                </a>
                <div class="osv-wait"><span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Đang chờ xác nhận chuyển tiền, trang sẽ tự cập nhật...</div>
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
              <RouterLink
                :to="{ path: '/shop/tra-cuu', query: { ma: maHoaDon } }"
                class="btn btn-outline-primary rounded-pill px-4 fw-semibold"
              >
                <i class="bi bi-search me-1"></i> Tra cứu đơn hàng này
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
</style>
