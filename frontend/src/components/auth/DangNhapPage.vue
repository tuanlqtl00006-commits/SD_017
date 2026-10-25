<script setup>
// Trang đăng nhập phong cách thương hiệu thể thao lớn (FootStyle Enterprise).
// Hỗ trợ đăng nhập nhân viên/quản lý và khách hàng. Tích hợp thanh chọn tài khoản mẫu 1-click cho ban giám khảo/người chấm thi.
// Tài khoản = email, số điện thoại hoặc mã nhân viên. Sai 5 lần liên tiếp bị khóa 5 phút (backend bảo vệ chống brute-force).
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
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
const vaiTroTab = ref('nhan-vien') // 'nhan-vien' | 'khach-hang'
const f = reactive({ taiKhoan: '', matKhau: '', nho: true })
const hienMatKhau = ref(false)
const dangGui = ref(false)
const loiChung = ref('')
const laDev = import.meta.env.DEV

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

const { loi, cham, sua, batDauGui, datLoiMayChu, datLai } = useLoiForm(() => {
  if (che.value !== 'dang-nhap') return {}
  const e = {}
  if (!f.taiKhoan.trim()) e.taiKhoan = 'Vui lòng nhập email, số điện thoại hoặc mã nhân viên.'
  if (!f.matKhau) e.matKhau = 'Vui lòng nhập mật khẩu.'
  return e
})

const lyDoHetPhien = computed(() => (route.query.hetPhien ? String(route.query.hetPhien) : ''))

async function dangNhap() {
  loiChung.value = ''
  if (dangGui.value || !batDauGui()) return
  dangGui.value = true
  try {
    if (vaiTroTab.value === 'khach-hang') {
      await auth.dangNhapKhachHang({ taiKhoan: f.taiKhoan.trim(), matKhau: f.matKhau, nho: f.nho })
    } else {
      await auth.dangNhap({ taiKhoan: f.taiKhoan.trim(), matKhau: f.matKhau, nho: f.nho })
    }
    const quayLai = duongDanQuayLaiAnToan(route.query.quayLai)
    await router.replace(quayLai || auth.trangChu())
  } catch (e) {
    if (!datLoiMayChu(e)) loiChung.value = e.message || 'Đăng nhập không thành công.'
    f.matKhau = ''
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
  if (loiEmail.value || dangGui.value) return
  dangGui.value = true
  try {
    const res = await api.post('/auth/quen-mat-khau', { email: e })
    thongBaoQuen.value = res.data?.message || 'Đã gửi mật khẩu tạm đến email của bạn.'
  } catch (err) {
    loiEmail.value = err.loiTheoTruong?.email || err.message || 'Không gửi được yêu cầu.'
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

function dienMau(tk, role = 'nhan-vien') {
  vaiTroTab.value = role
  f.taiKhoan = tk
  f.matKhau = role === 'khach-hang' ? '123456' : 'Footstyle@123'
  sua('taiKhoan')
  sua('matKhau')
}
</script>

<template>
  <div class="dn-trang">
    <!-- CỘT TRÁI: Giới thiệu đề tài / Đồ án tốt nghiệp -->
    <section class="dn-trai" aria-hidden="true">
      <div class="dn-trai-nen">
        <!-- Vòng hào quang gradient & lưới đồ họa thể thao -->
        <div class="dn-aura dn-aura-1"></div>
        <div class="dn-aura dn-aura-2"></div>
        <div class="dn-luoi-nen"></div>
      </div>

      <div class="dn-trai-noi-dung">
        <!-- Brand Header Top -->
        <header class="dn-trai-dau">
          <div class="dn-logo-hop">
            <img src="/logo.png" alt="FootStyle" class="dn-logo" />
          </div>
          <span class="dn-trai-huy-hieu">
            <i class="bi bi-mortarboard-fill me-1 text-primary"></i>
            ĐỒ ÁN TỐT NGHIỆP CNTT · 2026
          </span>
        </header>

        <!-- Slogan & Intro Nghiêm Túc -->
        <div class="dn-trai-giua">
          <div class="dn-tagline">HỆ THỐNG QUẢN LÝ BÁN HÀNG CỬA HÀNG THỂ THAO</div>
          <h2 class="dn-khau-hieu">
            Quản trị & Bán lẻ <br />
            <span class="dn-chu-nhan">FootStyle Badminton</span>
          </h2>
          <p class="dn-mo-ta">
            Phần mềm quản lý bán lẻ đa kênh toàn diện: Bán hàng tại quầy (POS), bán hàng trực tuyến (Online Storefront), quản lý thuộc tính sản phẩm thể thao chuyên sâu và tổng hợp báo cáo kinh doanh.
          </p>

          <!-- 3 Phân hệ nghiệp vụ chính của Đồ Án -->
          <div class="dn-cards-grid">
            <div class="dn-card-item">
              <div class="dn-card-icon"><i class="bi bi-box-seam-fill"></i></div>
              <div class="dn-card-text">
                <strong>Phân hệ Sản phẩm & Biến thể</strong>
                <span>Quản lý vợt cầu lông, giày, phụ kiện theo thuộc tính kỹ thuật chuyên sâu và mã QR.</span>
              </div>
            </div>

            <div class="dn-card-item">
              <div class="dn-card-icon"><i class="bi bi-shop"></i></div>
              <div class="dn-card-text">
                <strong>Phân hệ Bán hàng tại quầy (POS)</strong>
                <span>Bán tại quầy nhiều giỏ chờ, quét mã vạch, tự động áp mã giảm giá và thanh toán VietQR.</span>
              </div>
            </div>

            <div class="dn-card-item">
              <div class="dn-card-icon"><i class="bi bi-globe-americas"></i></div>
              <div class="dn-card-text">
                <strong>Phân hệ Bán hàng Trực tuyến (Storefront)</strong>
                <span>Website mua sắm trực tuyến, đồng bộ giỏ hàng, đặt hàng giao tận nơi và tra cứu đơn hàng.</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer Đồ Án / Metrics -->
        <footer class="dn-trai-day">
          <div class="dn-stats-row">
            <div class="dn-stat">
              <span class="dn-stat-so">03</span>
              <span class="dn-stat-chu">Phân hệ nghiệp vụ</span>
            </div>
            <div class="dn-stat-ngan"></div>
            <div class="dn-stat">
              <span class="dn-stat-so">100%</span>
              <span class="dn-stat-chu">Quy trình chuẩn hóa</span>
            </div>
            <div class="dn-stat-ngan"></div>
            <div class="dn-stat">
              <span class="dn-stat-so">Full-Stack</span>
              <span class="dn-stat-chu">Spring Boot & Vue.js</span>
            </div>
          </div>
          <div class="dn-danh-gia">
            <i class="bi bi-award-fill text-primary me-1"></i>
            <span>Hội đồng Đánh giá Đồ án Tốt nghiệp Chuyên ngành Công nghệ Thông tin</span>
          </div>
        </footer>
      </div>
    </section>

    <!-- CỘT PHẢI: Form Đăng Nhập Trang Nhã Chuẩn Doanh Nghiệp -->
    <section class="dn-phai">
      <div class="dn-the">
        <!-- Header card -->
        <div class="dn-the-dau">
          <div class="dn-logo-nho-wrap">
            <img src="/logo.png" alt="FootStyle" class="dn-logo-nho" />
          </div>
          <span class="dn-the-badge"><i class="bi bi-shield-check"></i> Xác thực bảo mật</span>
          <button
            type="button"
            class="dn-btn-shop"
            title="Đến gian hàng trực tuyến FootStyle Badminton"
            @click="router.push('/shop')"
          >
            <i class="bi bi-shop"></i> Cửa hàng Online <i class="bi bi-arrow-right"></i>
          </button>
        </div>

        <template v-if="che === 'dang-nhap'">
          <div class="dn-tieu-de-khoi">
            <h1 class="dn-tieu-de">Đăng nhập</h1>
            <p class="dn-phu">Vui lòng đăng nhập tài khoản để truy cập hệ thống quản trị.</p>
          </div>

          <!-- Role Switcher Tabs -->
          <div class="dn-tabs" role="tablist">
            <button
              type="button"
              class="dn-tab"
              :class="{ 'is-active': vaiTroTab === 'nhan-vien' }"
              role="tab"
              :aria-selected="vaiTroTab === 'nhan-vien'"
              @click="vaiTroTab = 'nhan-vien'"
            >
              <i class="bi bi-person-badge-fill me-1"></i> Nhân viên & Quản lý
            </button>
            <button
              type="button"
              class="dn-tab"
              :class="{ 'is-active': vaiTroTab === 'khach-hang' }"
              role="tab"
              :aria-selected="vaiTroTab === 'khach-hang'"
              @click="vaiTroTab = 'khach-hang'"
            >
              <i class="bi bi-person-circle me-1"></i> Khách mua hàng
            </button>
          </div>

          <!-- Alerts -->
          <div v-if="lyDoHetPhien" class="dn-thong-bao is-warn" role="status">
            <i class="bi bi-exclamation-triangle-fill me-2"></i> {{ lyDoHetPhien }}
          </div>
          <div v-if="loiChung" class="dn-thong-bao is-error" role="alert">
            <i class="bi bi-x-circle-fill me-2"></i> {{ loiChung }}
          </div>

          <!-- Main Login Form -->
          <form id="dn-form" class="dn-form" novalidate @submit.prevent="dangNhap">
            <!-- Account Input -->
            <div class="dn-o-nhap">
              <label class="ad-label dn-label" for="dn-tai-khoan">
                {{ vaiTroTab === 'nhan-vien' ? 'Tài khoản nhân viên' : 'Tài khoản khách hàng' }}
                <span class="ad-required">*</span>
              </label>
              <div class="dn-input-wrap">
                <span class="dn-input-icon"><i class="bi bi-person"></i></span>
                <input
                  id="dn-tai-khoan"
                  v-model="f.taiKhoan"
                  type="text"
                  class="form-control ad-control dn-input"
                  :class="{ 'is-invalid': loi.taiKhoan }"
                  autocomplete="username"
                  :placeholder="vaiTroTab === 'nhan-vien' ? 'Mã NV, email hoặc SĐT (vd: NV01)' : 'Email hoặc số điện thoại đăng ký'"
                  maxlength="255"
                  autofocus
                  @input="sua('taiKhoan')"
                  @blur="cham('taiKhoan')"
                />
              </div>
              <p v-if="loi.taiKhoan" class="ad-error dn-error">{{ loi.taiKhoan }}</p>
            </div>

            <!-- Password Input -->
            <div class="dn-o-nhap mt-3">
              <label class="ad-label dn-label" for="dn-mat-khau">
                Mật khẩu <span class="ad-required">*</span>
              </label>
              <div class="dn-input-wrap">
                <span class="dn-input-icon"><i class="bi bi-lock"></i></span>
                <input
                  id="dn-mat-khau"
                  v-model="f.matKhau"
                  :type="hienMatKhau ? 'text' : 'password'"
                  class="form-control ad-control dn-input"
                  :class="{ 'is-invalid': loi.matKhau }"
                  autocomplete="current-password"
                  placeholder="Nhập mật khẩu của bạn"
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
              <p v-if="loi.matKhau" class="ad-error dn-error">{{ loi.matKhau }}</p>
            </div>

            <!-- Options Row -->
            <div class="dn-hang">
              <label class="form-check dn-nho">
                <input v-model="f.nho" type="checkbox" class="form-check-input" />
                <span class="form-check-label">Ghi nhớ đăng nhập</span>
              </label>
              <button type="button" class="dn-lien-ket" @click="moQuen">Quên mật khẩu?</button>
            </div>

            <!-- Submit Button -->
            <button type="submit" class="ad-btn ad-btn-primary dn-nut" :disabled="dangGui">
              <span v-if="dangGui" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
              <span>Đăng nhập hệ thống</span>
              <i v-if="!dangGui" class="bi bi-arrow-right ms-2" aria-hidden="true"></i>
            </button>
          </form>

          <!-- 1-Click Quick Demo Bar (Dành cho Hội đồng chấm thi) -->
          <div class="dn-mau">
            <div class="dn-mau-tieu-de">
              <i class="bi bi-mortarboard me-1 text-primary"></i>
              <span>Tài khoản kiểm thử đồ án (1-Click):</span>
            </div>
            <div class="dn-mau-nut-group">
              <button type="button" class="dn-mau-nut" @click="dienMau('NV01', 'nhan-vien')">
                <span class="dn-badge-role">Admin</span>
                <b>NV01</b> · Quản lý
              </button>
              <!-- NV02 trong dữ liệu mẫu là Quản lý nên nút nhân viên thứ hai dùng NV04 -->
              <button type="button" class="dn-mau-nut" @click="dienMau('NV04', 'nhan-vien')">
                <span class="dn-badge-role is-nv">Thu ngân</span>
                <b>NV04</b> · Nhân viên
              </button>
              <button type="button" class="dn-mau-nut" @click="dienMau('NV03', 'nhan-vien')">
                <span class="dn-badge-role is-nv">Bán hàng</span>
                <b>NV03</b> · Nhân viên
              </button>
            </div>
          </div>
        </template>

        <!-- Form Quên Mật Khẩu -->
        <template v-else>
          <div class="dn-tieu-de-khoi">
            <h1 class="dn-tieu-de">Khôi phục mật khẩu</h1>
            <p class="dn-phu">Nhập email của tài khoản, hệ thống sẽ gửi mật khẩu tạm an toàn qua hòm thư.</p>
          </div>

          <div v-if="thongBaoQuen" class="dn-thong-bao is-ok" role="status">
            <i class="bi bi-check-circle-fill me-2"></i> {{ thongBaoQuen }}
          </div>

          <form novalidate @submit.prevent="guiQuenMatKhau">
            <div class="dn-o-nhap">
              <label class="ad-label dn-label" for="dn-email">Email đăng ký <span class="ad-required">*</span></label>
              <div class="dn-input-wrap">
                <span class="dn-input-icon"><i class="bi bi-envelope"></i></span>
                <input
                  id="dn-email"
                  v-model="email"
                  type="email"
                  class="form-control ad-control dn-input"
                  :class="{ 'is-invalid': loiEmail }"
                  autocomplete="email"
                  placeholder="vd: nhanvien@footstyle.vn"
                  maxlength="255"
                  @input="loiEmail = ''"
                />
              </div>
              <p v-if="loiEmail" class="ad-error dn-error">{{ loiEmail }}</p>
            </div>

            <button type="submit" class="ad-btn ad-btn-primary dn-nut mt-3" :disabled="dangGui">
              <span v-if="dangGui" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
              Gửi mật khẩu tạm
            </button>
          </form>

          <button type="button" class="dn-lien-ket dn-quay-lai" @click="veDangNhap">
            <i class="bi bi-arrow-left me-1"></i> Quay lại đăng nhập
          </button>
        </template>

        <!-- Compliance & Security Footer -->
        <div class="dn-the-chan">
          <div class="dn-ban-quyen">Đồ Án Tốt Nghiệp: Hệ Thống Bán Hàng Thể Thao FootStyle © 2026</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dn-trang {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: #f8fafc;
  font-family: inherit;
}

/* =========================================================
   CỘT TRÁI: GIỚI THIỆU ĐỀ TÀI / ĐỒ ÁN TỐT NGHIỆP
   ========================================================= */
.dn-trai {
  position: relative;
  overflow: hidden;
  display: flex;
  background: #18242f;
  color: #fff;
  padding: 2rem 3.25rem;
  height: 100vh;
  max-height: 100vh;
}

.dn-trai-nen {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.dn-aura {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}
.dn-aura-1 {
  width: 500px;
  height: 500px;
  top: -100px;
  left: -100px;
  background: radial-gradient(circle, #0977ec 0%, rgba(9, 119, 236, 0) 70%);
  opacity: 0.32;
}
.dn-aura-2 {
  width: 520px;
  height: 520px;
  bottom: -120px;
  right: -80px;
  background: radial-gradient(circle, #0977ec 0%, rgba(9, 119, 236, 0) 70%);
  opacity: 0.2;
}
.dn-luoi-nen {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

.dn-trai-noi-dung {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  max-width: 580px;
  height: 100%;
}

/* Header */
.dn-trai-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.dn-logo-hop {
  background: #ffffff;
  padding: 0.4rem 0.85rem;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  display: inline-flex;
}
.dn-logo {
  height: 34px;
  width: auto;
  object-fit: contain;
}
.dn-trai-huy-hieu {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(9, 119, 236, 0.15);
  border: 1px solid rgba(9, 119, 236, 0.35);
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #38bdf8;
  backdrop-filter: blur(8px);
}

/* Mid Section */
.dn-trai-giua {
  margin: 0.85rem 0;
}
.dn-tagline {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: #38bdf8;
  margin-bottom: 0.35rem;
  text-transform: uppercase;
}
.dn-khau-hieu {
  font-size: 1.95rem;
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 0.65rem;
  color: #ffffff;
  letter-spacing: -0.4px;
}
.dn-chu-nhan {
  color: #38bdf8;
}
.dn-mo-ta {
  font-size: 0.86rem;
  line-height: 1.48;
  color: #cbd5e1;
  margin: 0 0 0.9rem;
  max-width: 520px;
}

/* Cards Grid */
.dn-cards-grid {
  display: grid;
  gap: 0.6rem;
}
.dn-card-item {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.6rem 0.85rem;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  backdrop-filter: blur(12px);
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}
.dn-card-item:hover {
  transform: translateX(4px);
  background: rgba(9, 119, 236, 0.12);
  border-color: #0977ec;
}
.dn-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  display: inline-grid;
  place-items: center;
  font-size: 1.05rem;
  flex-shrink: 0;
  background: rgba(9, 119, 236, 0.18);
  color: #38bdf8;
  border: 1px solid rgba(9, 119, 236, 0.3);
}
.dn-card-text {
  display: flex;
  flex-direction: column;
}
.dn-card-text strong {
  font-size: 0.86rem;
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 0.1rem;
}
.dn-card-text span {
  font-size: 0.76rem;
  color: #94a3b8;
  line-height: 1.3;
}

/* Bottom Stats */
.dn-trai-day {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 0.9rem;
}
.dn-stats-row {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  margin-bottom: 0.6rem;
}
.dn-stat {
  display: flex;
  flex-direction: column;
}
.dn-stat-so {
  font-size: 1.35rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.5px;
}
.dn-stat-chu {
  font-size: 0.72rem;
  color: #94a3b8;
}
.dn-stat-ngan {
  width: 1px;
  height: 28px;
  background: rgba(255, 255, 255, 0.12);
}
.dn-danh-gia {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.76rem;
  color: #cbd5e1;
}

/* =========================================================
   CỘT PHẢI: FORM ĐĂNG NHẬP SANG TRỌNG
   ========================================================= */
.dn-phai {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem 2rem;
  height: 100vh;
  max-height: 100vh;
  overflow-y: auto;
  background: radial-gradient(circle at 80% 20%, rgba(9, 119, 236, 0.04) 0%, #f8fafc 70%);
}
.dn-the {
  width: 100%;
  max-width: 460px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 1.35rem 1.85rem;
  box-shadow: 0 16px 36px -12px rgba(15, 23, 42, 0.07), 0 0 0 1px rgba(9, 119, 236, 0.05);
}

.dn-the-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.dn-logo-nho-wrap {
  display: none;
}
.dn-logo-nho {
  height: 32px;
  width: auto;
}
.dn-the-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: #eff6ff;
  color: #0977ec;
  padding: 0.28rem 0.65rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 600;
  border: 1px solid #dbeafe;
}
.dn-btn-shop {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #0977ec;
  border-radius: 999px;
  padding: 0.28rem 0.75rem;
  font-size: 0.76rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.dn-btn-shop:hover {
  background: #0977ec;
  color: #ffffff;
  border-color: #0977ec;
}

.dn-tieu-de-khoi {
  margin-bottom: 0.95rem;
}
.dn-tieu-de {
  font-size: 1.55rem;
  font-weight: 800;
  color: #18242f;
  letter-spacing: -0.4px;
  margin: 0 0 0.2rem;
}
.dn-phu {
  font-size: 0.84rem;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

/* Role Tabs */
.dn-tabs {
  display: flex;
  background: #f1f5f9;
  padding: 0.25rem;
  border-radius: 10px;
  margin-bottom: 1rem;
  gap: 0.25rem;
}
.dn-tab {
  flex: 1;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.42rem 0.6rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.dn-tab.is-active {
  background: #ffffff;
  color: #0977ec;
  box-shadow: 0 2px 8px rgba(9, 119, 236, 0.15);
  border: 1px solid #bfdbfe;
  font-weight: 700;
}

/* Alert Boxes */
.dn-thong-bao {
  border-radius: 10px;
  padding: 0.6rem 0.8rem;
  margin-bottom: 0.9rem;
  font-size: 0.82rem;
  display: flex;
  align-items: center;
  line-height: 1.35;
}
.dn-thong-bao.is-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}
.dn-thong-bao.is-warn {
  background: #fffbeb;
  border: 1px solid #fef3c7;
  color: #b45309;
}
.dn-thong-bao.is-ok {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
}

/* Form Controls */
.dn-form {
  display: flex;
  flex-direction: column;
}
.dn-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #18242f;
  margin-bottom: 0.3rem;
  display: block;
}
.dn-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.dn-input-icon {
  position: absolute;
  left: 0.85rem;
  color: #94a3b8;
  font-size: 1rem;
  pointer-events: none;
  z-index: 2;
}
.dn-input {
  height: 42px;
  padding-left: 2.4rem !important;
  padding-right: 0.85rem;
  border-radius: 9px;
  font-size: 0.86rem;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #18242f;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.dn-input:focus {
  border-color: #0977ec;
  box-shadow: 0 0 0 3px rgba(9, 119, 236, 0.16);
}
.dn-mat {
  position: absolute;
  right: 0.65rem;
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 1.05rem;
  padding: 0.3rem;
  cursor: pointer;
  border-radius: 6px;
  display: inline-grid;
  place-items: center;
}
.dn-mat:hover {
  color: #18242f;
}
.dn-error {
  margin-top: 0.25rem;
  font-size: 0.78rem;
  color: #dc2626;
}

/* Options */
.dn-hang {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0.75rem 0 0.95rem;
}
.dn-nho {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.8rem;
  color: #475569;
  cursor: pointer;
  margin: 0;
}
.dn-nho .form-check-input {
  margin: 0;
  cursor: pointer;
  width: 1rem;
  height: 1rem;
}
.dn-lien-ket {
  border: 0;
  background: none;
  color: #0977ec;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0;
  cursor: pointer;
  transition: color 0.15s ease;
}
.dn-lien-ket:hover {
  text-decoration: underline;
  color: #075fc0;
}

/* Submit Button */
.dn-nut {
  width: 100%;
  height: 42px;
  border-radius: 9px;
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: 0.2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0977ec 0%, #075fc0 100%);
  border: 0;
  box-shadow: 0 6px 18px rgba(9, 119, 236, 0.28);
  transition: all 0.2s ease;
  color: #fff;
  cursor: pointer;
}
.dn-nut:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(9, 119, 236, 0.36);
  background: linear-gradient(135deg, #0f83f7 0%, #0654ab 100%);
}
.dn-nut:active:not(:disabled) {
  transform: translateY(1px);
}

/* Quick Demo Bar */
.dn-mau {
  margin-top: 1rem;
  padding-top: 0.85rem;
  border-top: 1px dashed #cbd5e1;
}
.dn-mau-tieu-de {
  display: flex;
  align-items: center;
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  margin-bottom: 0.5rem;
}
.dn-mau-nut-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.dn-mau-nut {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #18242f;
  border-radius: 8px;
  padding: 0.32rem 0.65rem;
  font-size: 0.76rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}
.dn-mau-nut:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #0977ec;
  transform: translateY(-1px);
}
.dn-badge-role {
  background: #e7f1fd;
  color: #0977ec;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.12rem 0.38rem;
  border-radius: 5px;
}
.dn-badge-role.is-nv {
  background: #e2e8f0;
  color: #475569;
}

/* Quên mật khẩu quay lại */
.dn-quay-lai {
  display: inline-flex;
  align-items: center;
  margin-top: 1rem;
  font-size: 0.84rem;
}

/* Footer card */
.dn-the-chan {
  margin-top: 1.15rem;
  padding-top: 0.75rem;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}
.dn-ban-quyen {
  font-size: 0.72rem;
  color: #94a3b8;
}

/* =========================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================= */
@media (max-width: 1080px) {
  .dn-trang {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }
  .dn-trai {
    display: none;
  }
  .dn-phai {
    height: auto;
    min-height: 100vh;
    padding: 2rem 1rem;
  }
  .dn-logo-nho-wrap {
    display: block;
  }
  .dn-the {
    max-width: 440px;
    padding: 1.75rem 1.5rem;
  }
}
</style>
