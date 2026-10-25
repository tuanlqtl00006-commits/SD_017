<script setup>
// Trang đăng nhập QUẢN TRỊ: quản lý và nhân viên dùng chung một trang, vào trong thì phân quyền theo vai trò.
// Khách hàng đăng nhập ở trang riêng của web bán hàng (/shop/dang-nhap) theo yêu cầu của cô.
// Thanh chọn tài khoản mẫu 1-click chỉ có khi chạy `npm run dev`.
// Tài khoản = email, số điện thoại hoặc mã nhân viên. Sai 5 lần liên tiếp bị khóa 5 phút (backend bảo vệ chống brute-force).
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import { duongDanQuayLaiAnToan } from '../../services/phien'
import { useAuth } from '../../composables/useAuth'
import { useLoiForm } from '../../composables/useLoiForm'
import { EMAIL } from '../../utils/kiemTra'

const router = useRouter()
const route = useRoute()
const auth = useAuth()

const che = ref('dang-nhap') // 'dang-nhap' | 'quen'
// Máy bán tại quầy dùng chung nên mặc định KHÔNG ghi nhớ đăng nhập
const f = reactive({ taiKhoan: '', matKhau: '', nho: false })
const hienMatKhau = ref(false)
const dangGui = ref(false)
const loiChung = ref('')
const laDev = import.meta.env.DEV
// Bản build có import.meta.env.DEV = false nên mật khẩu mẫu bị loại khỏi mã gửi cho trình duyệt
const MAT_KHAU_MAU = import.meta.env.DEV ? 'Footstyle@123' : null
const oMatKhau = ref(null)
const oEmail = ref(null)

// Có lỗi theo ô: về ô lỗi đầu tiên; sai mật khẩu (lỗi chung, ô mật khẩu đã xóa): về ô mật khẩu để gõ lại
function veOLoi() {
  if (Object.keys(loi.value).length) focusLoiDau('#dn-form')
  else nextTick(() => oMatKhau.value?.focus())
}

onMounted(() => {
  if (typeof document !== 'undefined') {
    document.body.classList.add('dn-body-no-zoom')
  }
})
onUnmounted(() => {
  if (typeof document !== 'undefined') {
    document.body.classList.remove('dn-body-no-zoom')
  }
})

const { loi, cham, sua, batDauGui, datLoiMayChu, focusLoiDau, datLai } = useLoiForm(() => {
  if (che.value !== 'dang-nhap') return {}
  const e = {}
  if (!f.taiKhoan.trim()) e.taiKhoan = 'Vui lòng nhập email, số điện thoại hoặc mã nhân viên.'
  if (!f.matKhau) e.matKhau = 'Vui lòng nhập mật khẩu.'
  return e
})

const lyDoHetPhien = computed(() => (route.query.hetPhien ? String(route.query.hetPhien) : ''))

/* ----- Hiệu ứng: rung thẻ khi sai, báo thành công ngắn trước khi chuyển trang ----- */
const lac = ref(false)
const thanhCong = ref(false)
const coChuyenDong = () => typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: no-preference)').matches
function rungThe() {
  lac.value = false
  requestAnimationFrame(() => (lac.value = true))
}

async function dangNhap() {
  loiChung.value = ''
  if (dangGui.value) return
  if (!batDauGui()) {
    veOLoi()
    return
  }
  dangGui.value = true
  try {
    await auth.dangNhap({ taiKhoan: f.taiKhoan.trim(), matKhau: f.matKhau, nho: f.nho })
    const quayLai = duongDanQuayLaiAnToan(route.query.quayLai)
    thanhCong.value = true
    if (coChuyenDong()) await new Promise((r) => setTimeout(r, 450))
    await router.replace(quayLai || auth.trangChu())
  } catch (e) {
    f.matKhau = ''
    if (!datLoiMayChu(e)) {
      // Lỗi chung (sai tài khoản / mật khẩu): chỉ hiện ô báo phía trên, ô mật khẩu vừa xóa không báo "Vui lòng nhập" ngay
      datLai()
      loiChung.value = e.message || 'Đăng nhập không thành công.'
    }
    rungThe()
    veOLoi()
  } finally {
    dangGui.value = false
  }
}

/* ----- Quên mật khẩu ----- */
const email = ref('')
const loiEmail = ref('')
const thongBaoQuen = ref('')
async function guiQuenMatKhau() {
  loiEmail.value = ''
  thongBaoQuen.value = ''
  const e = email.value.trim()
  if (!e) loiEmail.value = 'Vui lòng nhập email tài khoản.'
  else if (!EMAIL.test(e)) loiEmail.value = 'Email chưa đúng định dạng, ví dụ: ten@gmail.com.'
  if (loiEmail.value) nextTick(() => oEmail.value?.focus())
  if (loiEmail.value || dangGui.value) return
  dangGui.value = true
  try {
    const res = await api.post('/auth/quen-mat-khau', { email: e })
    thongBaoQuen.value = res.data?.message || 'Đã gửi mật khẩu tạm đến email của bạn.'
  } catch (err) {
    loiEmail.value = err.loiTheoTruong?.email || err.message || 'Không gửi được yêu cầu.'
    nextTick(() => oEmail.value?.focus())
  } finally {
    dangGui.value = false
  }
}

function moQuen() {
  che.value = 'quen'
  email.value = f.taiKhoan.includes('@') ? f.taiKhoan.trim() : ''
  thongBaoQuen.value = ''
  loiEmail.value = ''
}
function veDangNhap() {
  che.value = 'dang-nhap'
  datLai()
  loiChung.value = ''
}

function dienMau(tk) {
  if (!MAT_KHAU_MAU) return
  f.taiKhoan = tk
  f.matKhau = MAT_KHAU_MAU
  sua('taiKhoan')
  sua('matKhau')
}
</script>

<template>
  <div class="dn-trang">
    <!-- Nền: sân cầu lông mờ, quầng sáng, quả cầu bay (chỉ để trang trí) -->
    <div class="dn-nen" aria-hidden="true">
      <span class="dn-quang dn-quang-1"></span>
      <span class="dn-quang dn-quang-2"></span>
      <span class="dn-quang dn-quang-3"></span>
      <svg class="dn-san" viewBox="-20 -20 650 1380" fill="none">
        <g stroke="currentColor" stroke-width="4">
          <rect x="0" y="0" width="610" height="1340" />
          <line x1="46" y1="0" x2="46" y2="1340" />
          <line x1="564" y1="0" x2="564" y2="1340" />
          <line x1="0" y1="76" x2="610" y2="76" />
          <line x1="0" y1="1264" x2="610" y2="1264" />
          <line x1="0" y1="472" x2="610" y2="472" />
          <line x1="0" y1="868" x2="610" y2="868" />
          <line x1="305" y1="0" x2="305" y2="472" />
          <line x1="305" y1="868" x2="305" y2="1340" />
        </g>
        <line x1="-20" y1="670" x2="630" y2="670" stroke="currentColor" stroke-width="8" />
      </svg>
      <svg v-for="n in 3" :key="n" class="dn-qua-cau" :class="`is-${n}`" viewBox="0 0 64 96">
        <path d="M14 8 L50 8 L42 62 L22 62 Z" fill="rgba(255,255,255,0.9)" />
        <path d="M20 8 L27 62 M32 8 L32 62 M44 8 L37 62 M16 26 H48 M18 44 H46" stroke="rgba(9,119,236,0.35)" stroke-width="2" />
        <rect x="20" y="60" width="24" height="8" rx="2" fill="#38bdf8" />
        <path d="M20 68 Q32 92 44 68 Z" fill="#f8fafc" />
      </svg>
    </div>

    <header class="dn-dau dn-vao" style="--tre: 0ms">
      <span class="dn-thuong-hieu"><i class="bi bi-trophy-fill" aria-hidden="true"></i> FootStyle Badminton</span>
      <button type="button" class="dn-btn-shop" title="Đến cửa hàng trực tuyến FootStyle" @click="router.push('/shop')">
        <i class="bi bi-shop" aria-hidden="true"></i> Cửa hàng online <i class="bi bi-arrow-up-right" aria-hidden="true"></i>
      </button>
    </header>

    <main class="dn-giua">
      <div class="dn-the" :class="{ 'is-lac': lac }" @animationend.self="lac = false">
        <div class="dn-the-dau dn-vao" style="--tre: 40ms">
          <img src="/logo.png" alt="FootStyle" class="dn-logo" />
        </div>
        <Transition name="dn-che" mode="out-in">
          <div v-if="che === 'dang-nhap'" key="dang-nhap">
            <span class="dn-nhan-tren dn-vao" style="--tre: 60ms"><i class="bi bi-shield-lock" aria-hidden="true"></i> Trang quản trị</span>
            <h1 class="dn-tieu-de dn-vao" style="--tre: 100ms">Chào mừng trở lại</h1>
            <p class="dn-phu dn-vao" style="--tre: 140ms">Đăng nhập bằng tài khoản nhân viên. Hệ thống tự mở đúng chức năng theo vai trò của bạn.</p>

            <div v-if="lyDoHetPhien" class="dn-thong-bao is-warn" role="status">
              <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i> {{ lyDoHetPhien }}
            </div>
            <Transition name="dn-tb">
              <div v-if="loiChung" class="dn-thong-bao is-error" role="alert">
                <i class="bi bi-x-circle-fill" aria-hidden="true"></i> {{ loiChung }}
              </div>
            </Transition>

            <form id="dn-form" class="dn-form dn-vao" style="--tre: 200ms" novalidate @submit.prevent="dangNhap">
              <div class="dn-o-nhap">
                <label class="dn-label" for="dn-tai-khoan">Tài khoản <span class="dn-bat-buoc">*</span></label>
                <div class="dn-input-wrap">
                  <i class="bi bi-person dn-input-icon" aria-hidden="true"></i>
                  <input
                    id="dn-tai-khoan"
                    v-model="f.taiKhoan"
                    type="text"
                    name="username"
                    class="dn-input"
                    :class="{ 'is-invalid': loi.taiKhoan }"
                    autocomplete="username"
                    autocapitalize="none"
                    spellcheck="false"
                    required
                    :aria-invalid="!!loi.taiKhoan"
                    :aria-describedby="loi.taiKhoan ? 'dn-tai-khoan-loi' : undefined"
                    placeholder="Mã NV, email hoặc SĐT (vd: NV01)"
                    maxlength="255"
                    autofocus
                    @input="sua('taiKhoan')"
                    @blur="cham('taiKhoan')"
                  />
                </div>
                <p v-if="loi.taiKhoan" id="dn-tai-khoan-loi" class="dn-error"><i class="bi bi-exclamation-circle" aria-hidden="true"></i> {{ loi.taiKhoan }}</p>
              </div>

              <div class="dn-o-nhap">
                <div class="dn-label-hang">
                  <label class="dn-label" for="dn-mat-khau">Mật khẩu <span class="dn-bat-buoc">*</span></label>
                  <button type="button" class="dn-lien-ket" @click="moQuen">Quên mật khẩu?</button>
                </div>
                <div class="dn-input-wrap">
                  <i class="bi bi-lock dn-input-icon" aria-hidden="true"></i>
                  <input
                    id="dn-mat-khau"
                    ref="oMatKhau"
                    v-model="f.matKhau"
                    :type="hienMatKhau ? 'text' : 'password'"
                    name="password"
                    class="dn-input"
                    :class="{ 'is-invalid': loi.matKhau }"
                    autocomplete="current-password"
                    required
                    :aria-invalid="!!loi.matKhau"
                    :aria-describedby="loi.matKhau ? 'dn-mat-khau-loi' : undefined"
                    placeholder="Nhập mật khẩu"
                    maxlength="100"
                    @input="sua('matKhau')"
                    @blur="cham('matKhau')"
                  />
                  <button
                    type="button"
                    class="dn-mat"
                    :aria-label="hienMatKhau ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    :title="hienMatKhau ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    @click="hienMatKhau = !hienMatKhau"
                  >
                    <i class="bi" :class="hienMatKhau ? 'bi-eye-slash' : 'bi-eye'" aria-hidden="true"></i>
                  </button>
                </div>
                <p v-if="loi.matKhau" id="dn-mat-khau-loi" class="dn-error"><i class="bi bi-exclamation-circle" aria-hidden="true"></i> {{ loi.matKhau }}</p>
              </div>

              <label class="dn-nho">
                <input v-model="f.nho" type="checkbox" name="remember" />
                <span>Ghi nhớ đăng nhập trên máy này</span>
              </label>

              <button type="submit" class="dn-nut" :class="{ 'is-xong': thanhCong }" :disabled="dangGui" aria-live="polite">
                <template v-if="thanhCong">
                  <i class="bi bi-check-circle-fill dn-nut-tick" aria-hidden="true"></i>
                  <span>Đăng nhập thành công</span>
                </template>
                <template v-else-if="dangGui">
                  <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                  <span>Đang đăng nhập…</span>
                </template>
                <template v-else>
                  <span>Đăng nhập</span>
                  <i class="bi bi-arrow-right dn-nut-mui" aria-hidden="true"></i>
                </template>
              </button>
            </form>

            <p class="dn-khach dn-vao" style="--tre: 260ms">
              <i class="bi bi-bag-heart" aria-hidden="true"></i> Bạn là khách mua hàng?
              <button type="button" class="dn-lien-ket" data-testid="dn-sang-khach" @click="router.push('/shop/dang-nhap')">Đăng nhập cửa hàng online</button>
            </p>

            <!-- Tài khoản kiểm thử 1-click (cho Hội đồng chấm) - chỉ khi chạy npm run dev -->
            <div v-if="laDev" class="dn-mau dn-vao" style="--tre: 280ms" data-testid="dn-tai-khoan-mau">
              <span class="dn-mau-tieu-de"><i class="bi bi-lightning-charge-fill" aria-hidden="true"></i> Tài khoản demo (bấm để điền)</span>
              <div class="dn-mau-nut-group">
                <button type="button" class="dn-mau-nut" @click="dienMau('NV01')">
                  <span class="dn-mau-avt is-ql">QL</span><span><b>NV01</b><small>Quản lý</small></span>
                </button>
                <button type="button" class="dn-mau-nut" @click="dienMau('NV03')">
                  <span class="dn-mau-avt">NV</span><span><b>NV03</b><small>Nhân viên</small></span>
                </button>
                <button type="button" class="dn-mau-nut" @click="dienMau('NV04')">
                  <span class="dn-mau-avt">NV</span><span><b>NV04</b><small>Nhân viên</small></span>
                </button>
              </div>
            </div>
          </div>

          <div v-else key="quen">
            <button type="button" class="dn-lien-ket dn-quay-lai" @click="veDangNhap">
              <i class="bi bi-arrow-left" aria-hidden="true"></i> Quay lại đăng nhập
            </button>
            <span class="dn-nhan-tren"><i class="bi bi-key" aria-hidden="true"></i> Khôi phục tài khoản</span>
            <h1 class="dn-tieu-de">Quên mật khẩu?</h1>
            <p class="dn-phu">Nhập email của tài khoản, hệ thống sẽ gửi mật khẩu tạm qua hòm thư. Đăng nhập xong hãy đổi mật khẩu ngay.</p>

            <Transition name="dn-tb">
              <div v-if="thongBaoQuen" class="dn-thong-bao is-ok" role="status">
                <i class="bi bi-check-circle-fill" aria-hidden="true"></i> {{ thongBaoQuen }}
              </div>
            </Transition>

            <form class="dn-form" novalidate @submit.prevent="guiQuenMatKhau">
              <div class="dn-o-nhap">
                <label class="dn-label" for="dn-email">Email đăng ký <span class="dn-bat-buoc">*</span></label>
                <div class="dn-input-wrap">
                  <i class="bi bi-envelope dn-input-icon" aria-hidden="true"></i>
                  <input
                    id="dn-email"
                    ref="oEmail"
                    v-model="email"
                    type="email"
                    name="email"
                    class="dn-input"
                    :class="{ 'is-invalid': loiEmail }"
                    autocomplete="email"
                    required
                    :aria-invalid="!!loiEmail"
                    :aria-describedby="loiEmail ? 'dn-email-loi' : undefined"
                    placeholder="vd: nhanvien@gmail.com"
                    maxlength="255"
                    @input="loiEmail = ''"
                  />
                </div>
                <p v-if="loiEmail" id="dn-email-loi" class="dn-error"><i class="bi bi-exclamation-circle" aria-hidden="true"></i> {{ loiEmail }}</p>
              </div>
              <button type="submit" class="dn-nut" :disabled="dangGui">
                <span v-if="dangGui" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <span>Gửi mật khẩu tạm</span>
              </button>
            </form>
          </div>
        </Transition>
      </div>

      <ul class="dn-tinh-nang dn-vao" style="--tre: 320ms" aria-label="Tính năng chính">
        <li><i class="bi bi-upc-scan" aria-hidden="true"></i> Bán tại quầy</li>
        <li><i class="bi bi-arrow-left-right" aria-hidden="true"></i> Giao ca</li>
        <li><i class="bi bi-truck" aria-hidden="true"></i> Đơn online</li>
        <li><i class="bi bi-graph-up-arrow" aria-hidden="true"></i> Thống kê</li>
      </ul>
    </main>

    <footer class="dn-chan">
      <span class="dn-bao-mat"><i class="bi bi-shield-check" aria-hidden="true"></i> Kết nối được mã hóa · Khóa 5 phút khi sai 5 lần</span>
      <span>© 2026 FootStyle Badminton</span>
    </footer>
  </div>
</template>

<style scoped>
/* Một khối đăng nhập ở giữa trên nền sân cầu lông xanh (không chia đôi màn hình) */
.dn-trang {
  --xanh: #0977ec;
  --xanh-dam: #0657b8;
  --xanh-nhat: #eaf3fe;
  --chu: #0f1b2d;
  --chu-phu: #5b6b80;
  --vien: #d9e1ec;
  --de: cubic-bezier(0.2, 0.8, 0.2, 1);
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
  padding: 1.25rem clamp(1rem, 3vw, 2.5rem);
  background: linear-gradient(155deg, #061634 0%, #0a3274 45%, #0b5fd0 80%, #1c8cf0 100%);
  color: var(--chu);
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
}
/* Trang này tự đặt cỡ / màu chữ cho tiêu đề (không theo h1, h2 chung của style.css) */
.dn-trang :is(h1, h2) {
  margin: 0;
  letter-spacing: -0.02em;
  font-weight: 800;
}

/* ----- Nền trang trí ----- */
.dn-nen {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.dn-quang {
  position: absolute;
  border-radius: 50%;
}
.dn-quang-1 {
  width: 620px;
  height: 620px;
  top: -240px;
  right: -180px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.42), transparent 65%);
}
.dn-quang-2 {
  width: 560px;
  height: 560px;
  bottom: -260px;
  left: -200px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.38), transparent 65%);
}
.dn-quang-3 {
  width: 420px;
  height: 420px;
  top: 30%;
  left: 50%;
  transform: translateX(-50%);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1), transparent 70%);
}
.dn-san {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(720px, 90vw);
  color: rgba(255, 255, 255, 0.06);
  transform: translate(-50%, -50%) rotate(-62deg);
}
.dn-qua-cau {
  position: absolute;
  width: 44px;
  opacity: 0.85;
  filter: drop-shadow(0 10px 18px rgba(0, 0, 0, 0.3));
}
.dn-qua-cau.is-1 {
  top: 16%;
  left: 12%;
  transform: rotate(-28deg);
  animation: dn-bay 9s ease-in-out infinite;
}
.dn-qua-cau.is-2 {
  bottom: 14%;
  right: 13%;
  width: 56px;
  transform: rotate(32deg);
  animation: dn-bay 11s ease-in-out 1.5s infinite;
}
.dn-qua-cau.is-3 {
  top: 22%;
  right: 22%;
  width: 30px;
  opacity: 0.55;
  transform: rotate(12deg);
  animation: dn-bay 10s ease-in-out 3s infinite;
}

/* ----- Thanh trên, khối giữa, chân trang ----- */
.dn-dau {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.dn-thuong-hieu {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: #e0ecff;
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: 0.01em;
}
.dn-thuong-hieu .bi {
  color: #fde047;
}
.dn-btn-shop {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border-radius: 999px;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: background-color 0.15s ease, transform 0.15s ease;
}
.dn-btn-shop:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}
.dn-giua {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 1rem 0;
}
.dn-the {
  width: 100%;
  max-width: 490px;
  padding: 1.6rem 2rem 1.4rem;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 30px 70px rgba(2, 10, 30, 0.45), 0 2px 0 rgba(255, 255, 255, 0.5) inset;
}
.dn-the-dau {
  display: flex;
  justify-content: center;
  margin-bottom: 0.9rem;
}
.dn-logo {
  height: 54px;
  width: auto;
  object-fit: contain;
}
.dn-the .dn-tieu-de,
.dn-the .dn-phu {
  text-align: center;
}
.dn-the .dn-nhan-tren {
  display: flex;
  width: fit-content;
  margin-inline: auto;
}
.dn-tinh-nang {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
.dn-tinh-nang li {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.8rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #dbeafe;
  font-size: 0.82rem;
  font-weight: 600;
  backdrop-filter: blur(6px);
}
.dn-tinh-nang .bi {
  color: #7dd3fc;
}
.dn-the.is-lac {
  animation: dn-lac 0.42s ease;
}
.dn-nhan-tren {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  background: var(--xanh-nhat);
  color: var(--xanh-dam);
  font-size: 0.78rem;
  font-weight: 700;
  margin-bottom: 0.7rem;
}
.dn-tieu-de {
  font-size: clamp(1.7rem, 2.2vw, 2rem);
  line-height: 1.15;
  color: var(--chu);
}
.dn-phu {
  margin: 0.45rem 0 1.25rem;
  color: var(--chu-phu);
  font-size: 0.93rem;
  line-height: 1.55;
}
.dn-thong-bao {
  display: flex;
  gap: 0.55rem;
  align-items: flex-start;
  padding: 0.75rem 0.9rem;
  border-radius: 12px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
  line-height: 1.45;
}
.dn-thong-bao.is-error {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}
.dn-thong-bao.is-warn {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}
.dn-thong-bao.is-ok {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}
.dn-form {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.dn-label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: #24344a;
}
.dn-bat-buoc {
  color: #dc2626;
}
.dn-label-hang {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.dn-input-wrap {
  position: relative;
}
.dn-input-icon {
  position: absolute;
  left: 0.95rem;
  top: 50%;
  transform: translateY(-50%);
  color: #8a99ad;
  font-size: 1.05rem;
  pointer-events: none;
  transition: color 0.15s ease;
}
.dn-input {
  width: 100%;
  height: 48px;
  padding: 0 2.9rem 0 2.75rem;
  border: 1.5px solid var(--vien);
  border-radius: 14px;
  background: #f8fafc;
  color: var(--chu);
  font-size: 1rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}
.dn-input::placeholder {
  color: #9aa8ba;
}
.dn-input:hover {
  border-color: #b9c6d8;
}
.dn-input:focus {
  outline: none;
  background: #fff;
  border-color: var(--xanh);
  box-shadow: 0 0 0 4px rgba(9, 119, 236, 0.14);
}
.dn-input-wrap:focus-within .dn-input-icon {
  color: var(--xanh);
}
.dn-input.is-invalid {
  border-color: #dc2626;
  background: #fffafa;
}
.dn-input.is-invalid:focus {
  box-shadow: 0 0 0 4px rgba(220, 38, 38, 0.12);
}
.dn-mat {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}
.dn-mat:hover {
  background: #eef3f9;
  color: var(--chu);
}
.dn-error {
  display: flex;
  gap: 0.35rem;
  margin: 0.4rem 0 0;
  font-size: 0.84rem;
  color: #dc2626;
}
.dn-lien-ket {
  border: 0;
  background: none;
  padding: 0;
  color: var(--xanh);
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
}
.dn-lien-ket:hover {
  color: var(--xanh-dam);
  text-decoration: underline;
}
.dn-lien-ket:focus-visible,
.dn-btn-shop:focus-visible,
.dn-mau-nut:focus-visible,
.dn-mat:focus-visible {
  outline: 3px solid rgba(9, 119, 236, 0.35);
  outline-offset: 2px;
}
.dn-nho {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  font-size: 0.9rem;
  color: #475569;
  cursor: pointer;
  user-select: none;
}
.dn-nho input {
  appearance: none;
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1.5px solid #cbd5e1;
  background-color: #ffffff;
  color-scheme: light;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  transition: all 0.15s ease-in-out;
}
.dn-nho input:hover {
  border-color: var(--xanh);
}
.dn-nho input:checked {
  background-color: var(--xanh);
  border-color: var(--xanh);
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20'%3e%3cpath fill='none' stroke='%23fff' stroke-linecap='round' stroke-linejoin='round' stroke-width='3' d='M6 10l3 3l6-6'/%3e%3c/svg%3e");
  background-position: center;
  background-repeat: no-repeat;
  background-size: 12px 12px;
}
.dn-nut {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  height: 50px;
  margin-top: 0.2rem;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, #1a8cff 0%, var(--xanh) 45%, var(--xanh-dam) 100%);
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(9, 119, 236, 0.28);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;
}
.dn-nut:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(9, 119, 236, 0.34);
  filter: brightness(1.04);
}
.dn-nut:active:not(:disabled) {
  transform: translateY(0);
}
.dn-nut:focus-visible {
  outline: 3px solid rgba(9, 119, 236, 0.4);
  outline-offset: 3px;
}
.dn-nut:disabled {
  cursor: progress;
  opacity: 0.85;
}
.dn-nut .dn-nut-mui {
  transition: transform 0.2s var(--de);
}
.dn-nut:hover .dn-nut-mui {
  transform: translateX(4px);
}
.dn-nut.is-xong {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 10px 24px rgba(22, 163, 74, 0.3);
}
.dn-nut-tick {
  animation: dn-bat 0.35s var(--de);
}
.dn-chia {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 1.1rem 0 0.7rem;
  color: #94a3b8;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.dn-chia::before,
.dn-chia::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e5ebf3;
}
.dn-khach {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  margin: 1rem 0 0;
  color: var(--chu-phu);
  font-size: 0.9rem;
}
.dn-mau {
  margin-top: 1rem;
  padding: 0.75rem;
  border: 1px dashed #c9d6e8;
  border-radius: 14px;
  background: #f8fbff;
}
.dn-mau-tieu-de {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.65rem;
  font-size: 0.78rem;
  font-weight: 700;
  color: #475569;
}
.dn-mau-tieu-de .bi {
  color: #f59e0b;
}
.dn-mau-nut-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}
.dn-mau-nut {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.55rem;
  border: 1px solid var(--vien);
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.dn-mau-nut:hover {
  border-color: var(--xanh);
  box-shadow: 0 6px 16px rgba(9, 119, 236, 0.12);
  transform: translateY(-1px);
}
.dn-mau-nut b {
  display: block;
  font-size: 0.85rem;
  color: var(--chu);
}
.dn-mau-nut small {
  display: block;
  font-size: 0.72rem;
  white-space: nowrap;
  color: var(--chu-phu);
}
.dn-mau-avt {
  flex: none;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: var(--xanh-nhat);
  color: var(--xanh-dam);
  font-size: 0.68rem;
  font-weight: 800;
}
.dn-mau-avt.is-ql {
  background: #fef3c7;
  color: #b45309;
}
.dn-quay-lai {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 1.25rem;
}
.dn-chan {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.5rem 1rem;
  color: rgba(219, 234, 254, 0.7);
  font-size: 0.8rem;
}
.dn-bao-mat {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: #86efac;
  font-weight: 600;
}

/* ================= CHUYỂN ĐỘNG ================= */
.dn-vao {
  animation: dn-len 0.6s var(--de) both;
  animation-delay: var(--tre, 0ms);
}
.dn-che-enter-active,
.dn-che-leave-active {
  transition: opacity 0.22s ease, transform 0.22s var(--de);
}
.dn-che-enter-from {
  opacity: 0;
  transform: translateX(14px);
}
.dn-che-leave-to {
  opacity: 0;
  transform: translateX(-14px);
}
.dn-tb-enter-active,
.dn-tb-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.dn-tb-enter-from,
.dn-tb-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
@keyframes dn-len {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
@keyframes dn-lac {
  0%,
  100% {
    transform: translateX(0);
  }
  20%,
  60% {
    transform: translateX(-8px);
  }
  40%,
  80% {
    transform: translateX(8px);
  }
}
@keyframes dn-bat {
  from {
    transform: scale(0.4);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
/* Quả cầu bay lơ lửng (giữ góc nghiêng riêng của từng quả qua translate) */
@keyframes dn-bay {
  0%,
  100% {
    translate: 0 0;
  }
  50% {
    translate: 10px -18px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .dn-vao,
  .dn-qua-cau,
  .dn-the.is-lac,
  .dn-nut-tick {
    animation: none !important;
  }
  .dn-che-enter-active,
  .dn-che-leave-active,
  .dn-tb-enter-active,
  .dn-tb-leave-active {
    transition: none;
  }
}

/* ================= MÀN HÌNH NHỎ ================= */
@media (max-width: 640px) {
  .dn-trang {
    padding: 0.9rem;
  }
  .dn-the {
    padding: 1.5rem 1.25rem 1.25rem;
    border-radius: 20px;
  }
  .dn-logo {
    height: 52px;
  }
  .dn-thuong-hieu {
    font-size: 0.85rem;
  }
  .dn-btn-shop {
    padding: 0.45rem 0.75rem;
    font-size: 0.8rem;
  }
  .dn-qua-cau,
  .dn-tinh-nang {
    display: none;
  }
  .dn-chan {
    justify-content: center;
    text-align: center;
  }
}
/* Màn hình thấp (laptop 768px, trình duyệt phóng to): thu gọn để ít phải kéo */
@media (max-height: 820px) {
  .dn-the .dn-nhan-tren {
    display: none;
  }
  .dn-the {
    padding-top: 1.2rem;
  }
  .dn-the-dau {
    margin-bottom: 0.5rem;
  }
  .dn-logo {
    height: 44px;
  }
  .dn-phu {
    margin-bottom: 0.9rem;
  }
  .dn-giua {
    padding: 0.5rem 0;
  }
}
@media (max-height: 980px) and (min-width: 641px) {
  .dn-tinh-nang {
    display: none;
  }
}
@media (max-width: 420px) {
  .dn-mau-nut-group {
    grid-template-columns: 1fr;
  }
}
</style>
