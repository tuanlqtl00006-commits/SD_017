<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useCart } from '../composables/useCart'
import KhungChatKhach from '../components/chat/KhungChatKhach.vue'

const router = useRouter()
const auth = useAuth()
const cart = useCart()

const tuKhoaTimKiem = ref('')
const moChatBox = ref(false)
const moTraCuuModal = ref(false)
const maTraCuu = ref('')
const sdtTraCuu = ref('')
const ketQuaTraCuu = ref(null)
const dangTraCuu = ref(false)
const loiTraCuu = ref('')

function timKiem() {
  const kw = tuKhoaTimKiem.value.trim()
  router.push({ path: '/shop', query: kw ? { q: kw } : {} })
}

function chonDanhMuc(slug, id) {
  router.push({ path: '/shop', query: id ? { dm: id } : {} })
}

function chonFlashSale() {
  router.push({ path: '/shop', query: { sale: '1' } })
}

async function traCuu() {
  if (!maTraCuu.value.trim()) {
    loiTraCuu.value = 'Vui lòng nhập mã đơn hàng (vd: HD01).'
    return
  }
  dangTraCuu.value = true
  loiTraCuu.value = ''
  ketQuaTraCuu.value = null
  try {
    const res = await fetch(`/api/cua-hang/tra-cuu?maHoaDon=${encodeURIComponent(maTraCuu.value.trim())}&sdt=${encodeURIComponent(sdtTraCuu.value.trim())}`)
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Không tìm thấy thông tin đơn hàng.')
    }
    ketQuaTraCuu.value = await res.json()
  } catch (e) {
    loiTraCuu.value = e.message || 'Lỗi tra cứu đơn hàng.'
  } finally {
    dangTraCuu.value = false
  }
}

function formatTien(v) {
  if (!v) return '0 ₫'
  return Number(v).toLocaleString('vi-VN') + ' ₫'
}
</script>

<template>
  <div class="vnb-storefront">
    <!-- Header chính: Logo + Search Bar + Quick Actions -->
    <header class="vnb-main-header bg-white border-bottom py-3 sticky-top shadow-sm">
      <div class="container d-flex align-items-center justify-content-between gap-3">
        <!-- Logo thương hiệu -->
        <RouterLink to="/shop" class="d-flex align-items-center gap-2 text-decoration-none">
          <img src="/logo.png" alt="FootStyle Badminton" style="height: 48px; object-fit: contain;">
          <div class="d-none d-sm-block">
            <div class="fw-bold fs-5 text-dark lh-1" style="letter-spacing: -0.5px;">
              FOOTSTYLE <span class="vnb-badge-brand">BADMINTON</span>
            </div>
            <small class="text-muted" style="font-size: 0.72rem;">HỆ THỐNG CẦU LÔNG CHÍNH HÃNG VNB & FS</small>
          </div>
        </RouterLink>

        <!-- Search Bar phong cách ShopVNB to bản -->
        <div class="vnb-search-wrap flex-grow-1 mx-lg-4" style="max-width: 580px;">
          <form class="position-relative d-flex" @submit.prevent="timKiem">
            <input
              v-model="tuKhoaTimKiem"
              type="text"
              class="form-control form-control-lg vnb-search-input"
              placeholder="Tìm vợt Yonex Astrox, Victor, giày cầu lông, cước căng..."
            />
            <button type="submit" class="btn vnb-btn-search px-4">
              <i class="bi bi-search me-1"></i> <span class="d-none d-sm-inline">Tìm kiếm</span>
            </button>
          </form>
        </div>

        <!-- Quick Actions: Tra cứu, Chat, Giỏ hàng, Tài khoản -->
        <div class="d-flex align-items-center gap-2 gap-md-3">
          <!-- Nút sang POS / Quản trị -->
          <RouterLink
            to="/ban-hang"
            class="btn btn-outline-secondary btn-sm d-none d-lg-flex align-items-center gap-1 vnb-action-btn"
            title="Hệ thống quản trị / Bán hàng tại quầy"
          >
            <i class="bi bi-speedometer2 text-primary fs-6"></i>
            <span class="small">Quản trị</span>
          </RouterLink>

          <!-- Nút tra cứu đơn -->
          <button
            type="button"
            class="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1 d-none d-md-flex vnb-action-btn"
            @click="moTraCuuModal = true"
          >
            <i class="bi bi-search text-primary fs-6"></i>
            <span class="small">Tra cứu đơn</span>
          </button>

          <!-- Nút giỏ hàng có badge -->
          <RouterLink to="/gio-hang" class="btn vnb-cart-btn position-relative d-flex align-items-center gap-2 px-3 py-2">
            <i class="bi bi-cart3 fs-5"></i>
            <div class="text-start d-none d-sm-block lh-1">
              <div class="small fw-semibold">Giỏ hàng</div>
              <small class="text-white-50" style="font-size: 0.7rem;">{{ formatTien(cart.tongTien.value) }}</small>
            </div>
            <span v-if="cart.tongSoLuong.value" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-white">
              {{ cart.tongSoLuong.value }}
            </span>
          </RouterLink>

          <!-- Auth Account -->
          <template v-if="auth.daDangNhap.value">
            <div class="d-flex align-items-center gap-2 ps-2 border-start">
              <div class="vnb-user-avatar">{{ auth.vietTat.value }}</div>
              <div class="d-none d-lg-block text-start lh-1">
                <div class="small fw-bold text-dark text-truncate" style="max-width: 110px;">{{ auth.trangThai.nguoiDung?.hoTen }}</div>
                <small class="text-muted" style="font-size: 0.72rem;">{{ auth.trangThai.nguoiDung?.tenVaiTro }}</small>
              </div>
            </div>
          </template>
          <template v-else>
            <RouterLink to="/dang-nhap" class="btn btn-outline-dark btn-sm d-flex align-items-center gap-1 vnb-login-btn">
              <i class="bi bi-person-circle"></i>
              <span class="d-none d-md-inline">Đăng nhập</span>
            </RouterLink>
          </template>
        </div>
      </div>
    </header>

    <!-- Megamenu Thể Thao Ngang Chuẩn ShopVNB -->
    <nav class="vnb-nav-bar bg-dark text-white">
      <div class="container d-flex flex-wrap align-items-center">
        <ul class="nav vnb-nav-list">
          <li class="nav-item">
            <RouterLink to="/shop" class="nav-link vnb-nav-link text-white fw-bold active">
              <i class="bi bi-grid-fill me-1 text-info"></i> TẤT CẢ SẢN PHẨM
            </RouterLink>
          </li>
          <li class="nav-item">
            <a href="#" class="nav-link vnb-nav-link text-white" @click.prevent="chonDanhMuc('vot-cau-long', 1)">
              🏸 VỢT CẦU LÔNG
            </a>
          </li>
          <li class="nav-item">
            <a href="#" class="nav-link vnb-nav-link text-white" @click.prevent="chonDanhMuc('giay-cau-long', 2)">
              👟 GIÀY CẦU LÔNG
            </a>
          </li>
          <li class="nav-item">
            <a href="#" class="nav-link vnb-nav-link text-white" @click.prevent="chonDanhMuc('ao-quan', 3)">
              👕 QUẦN ÁO THỂ THAO
            </a>
          </li>
          <li class="nav-item">
            <a href="#" class="nav-link vnb-nav-link text-white" @click.prevent="chonDanhMuc('balo-tui', 4)">
              🎒 BALO & TÚI VỢT
            </a>
          </li>
          <li class="nav-item">
            <a href="#" class="nav-link vnb-nav-link text-white" @click.prevent="chonDanhMuc('phu-kien', 5)">
              🎾 PHỤ KIỆN & CƯỚC
            </a>
          </li>
          <li class="nav-item ms-lg-auto">
            <a href="#" class="nav-link vnb-nav-link text-white fw-bold d-flex align-items-center gap-1" @click.prevent="chonFlashSale">
              <span class="vnb-badge-flash">HOT</span> FLASH SALE SIÊU SỐC
            </a>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Main Content Area -->
    <main class="vnb-content-body min-vh-100 bg-light py-4">
      <RouterView />
    </main>

    <!-- Floating Chat AI Widget Button -->
    <div class="vnb-floating-chat">
      <button
        type="button"
        class="btn vnb-chat-trigger shadow-lg d-flex align-items-center gap-2"
        :class="{ 'is-active': moChatBox }"
        @click="moChatBox = !moChatBox"
      >
        <i class="bi bi-chat-dots-fill fs-5"></i>
        <span class="fw-bold d-none d-sm-inline">Tư vấn Cầu Lông AI</span>
        <span class="vnb-chat-pulse"></span>
      </button>

      <!-- Popup Chat Box -->
      <div v-if="moChatBox" class="vnb-chat-popup shadow-lg border rounded-4 overflow-hidden">
        <div class="vnb-chat-header d-flex justify-content-between align-items-center p-3 text-white">
          <div class="d-flex align-items-center gap-2">
            <div class="vnb-bot-avatar"><i class="bi bi-robot"></i></div>
            <div>
              <div class="fw-bold small lh-1">Trợ lý Cầu Lông AI · FootStyle</div>
              <small class="text-white-50" style="font-size: 0.68rem;">Tư vấn căng cước, chọn vợt & giải đáp tức thì</small>
            </div>
          </div>
          <button type="button" class="btn btn-sm btn-link text-white p-0" @click="moChatBox = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="vnb-chat-body" style="height: 480px;">
          <KhungChatKhach />
        </div>
      </div>
    </div>

    <!-- Modal Tra Cứu Đơn Hàng -->
    <div v-if="moTraCuuModal" class="modal d-block" tabindex="-1" style="background: rgba(0,0,0,0.6); z-index: 1060;">
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content rounded-4 border-0 shadow-lg">
          <div class="modal-header bg-dark text-white rounded-top-4">
            <h5 class="modal-title fw-bold d-flex align-items-center gap-2">
              <i class="bi bi-truck text-info"></i> Tra Cứu Tình Trạng Đơn Hàng Trực Tuyến
            </h5>
            <button type="button" class="btn-close btn-close-white" @click="moTraCuuModal = false"></button>
          </div>
          <div class="modal-body p-4">
            <form class="row g-3" @submit.prevent="traCuu">
              <div class="col-md-7">
                <label class="form-label fw-semibold small">Mã đơn hàng <span class="text-danger">*</span></label>
                <div class="input-group">
                  <span class="input-group-text"><i class="bi bi-receipt"></i></span>
                  <input v-model="maTraCuu" type="text" class="form-control" placeholder="VD: HD01 hoặc HD16" required />
                </div>
              </div>
              <div class="col-md-5">
                <label class="form-label fw-semibold small">Số điện thoại nhận hàng (tùy chọn)</label>
                <div class="input-group">
                  <span class="input-group-text"><i class="bi bi-telephone"></i></span>
                  <input v-model="sdtTraCuu" type="text" class="form-control" placeholder="VD: 0987654321" />
                </div>
              </div>
              <div class="col-12 text-end">
                <button type="submit" class="btn vnb-btn-search px-4 fw-bold" :disabled="dangTraCuu">
                  <span v-if="dangTraCuu" class="spinner-border spinner-border-sm me-1"></span>
                  <i v-else class="bi bi-search me-1"></i> Kiểm Tra Tiến Độ
                </button>
              </div>
            </form>

            <div v-if="loiTraCuu" class="alert alert-danger mt-3 small">
              <i class="bi bi-exclamation-triangle-fill me-1"></i> {{ loiTraCuu }}
            </div>

            <!-- Kết quả tra cứu chi tiết -->
            <div v-if="ketQuaTraCuu" class="mt-4 border rounded-3 p-3 bg-light">
              <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                <div>
                  <h6 class="fw-bold mb-0 text-dark">Đơn hàng #{{ ketQuaTraCuu.maHoaDon }}</h6>
                  <small class="text-muted">Ngày đặt: {{ new Date(ketQuaTraCuu.ngayTao).toLocaleString('vi-VN') }}</small>
                </div>
                <span class="badge bg-warning text-dark px-3 py-2 fs-6 fw-bold">
                  {{ ketQuaTraCuu.tenTrangThai }}
                </span>
              </div>

              <div class="row g-2 small mb-3">
                <div class="col-sm-6">
                  <div><strong>Người nhận:</strong> {{ ketQuaTraCuu.hoTenNguoiNhan }}</div>
                  <div><strong>SĐT:</strong> {{ ketQuaTraCuu.sdtNguoiNhan }}</div>
                </div>
                <div class="col-sm-6">
                  <div><strong>Địa chỉ:</strong> {{ ketQuaTraCuu.diaChiGiaoHang }}</div>
                  <div><strong>Vận chuyển:</strong> {{ ketQuaTraCuu.donViVanChuyen || 'Giao Hàng Nhanh' }}</div>
                </div>
              </div>

              <div class="table-responsive">
                <table class="table table-sm table-bordered bg-white mb-2">
                  <thead class="table-secondary small">
                    <tr>
                      <th>Sản phẩm</th>
                      <th>Màu / Quy cách</th>
                      <th class="text-center">Số lượng</th>
                      <th class="text-end">Đơn giá</th>
                      <th class="text-end">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody class="small">
                    <tr v-for="(m, idx) in ketQuaTraCuu.chiTiet" :key="idx">
                      <td class="fw-semibold">{{ m.tenSanPham }}</td>
                      <td>{{ m.mauSac }} · {{ m.trongLuong }} · {{ m.chuVi }}</td>
                      <td class="text-center">{{ m.soLuong }}</td>
                      <td class="text-end">{{ formatTien(m.donGia) }}</td>
                      <td class="text-end fw-bold">{{ formatTien(m.thanhTien) }}</td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colspan="4" class="text-end fw-bold">Tổng thanh toán:</td>
                      <td class="text-end text-danger fw-bold fs-6">{{ formatTien(ketQuaTraCuu.thanhTien) }}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer Chuẩn Hệ Thống Shop Cầu Lông Lớn -->
    <footer class="vnb-footer bg-dark text-white pt-5 pb-3">
      <div class="container">
        <div class="row g-4 pb-4 border-bottom border-secondary">
          <div class="col-lg-4 col-md-6">
            <h5 class="fw-bold text-warning mb-3">HỆ THỐNG CẦU LÔNG FOOTSTYLE</h5>
            <p class="small text-white-50">
              Chuyên phân phối vợt cầu lông Yonex, Victor, Lining chính hãng hàng đầu Việt Nam. Dịch vụ căng cước máy điện tử chính xác, tư vấn chuẩn vận động viên.
            </p>
            <div class="small text-white-50">
              <div><i class="bi bi-geo-alt-fill text-warning me-2"></i> Showroom: Số 12 Cầu Giấy, Hà Nội</div>
              <div><i class="bi bi-telephone-fill text-warning me-2"></i> Hotline đặt hàng: 1900 6886</div>
              <div><i class="bi bi-envelope-fill text-warning me-2"></i> Email: lienhe@footstyle.vn</div>
            </div>
          </div>
          <div class="col-lg-2 col-md-6">
            <h6 class="fw-bold text-white mb-3">DANH MỤC HOT</h6>
            <ul class="list-unstyled small text-white-50 lh-lg">
              <li><a href="#" class="text-white-50 text-decoration-none" @click.prevent="chonDanhMuc('vot', 1)">Vợt Cầu Lông Yonex</a></li>
              <li><a href="#" class="text-white-50 text-decoration-none" @click.prevent="chonDanhMuc('vot', 1)">Vợt Lining Axforce</a></li>
              <li><a href="#" class="text-white-50 text-decoration-none" @click.prevent="chonDanhMuc('giay', 2)">Giày Cầu Lông Yonex</a></li>
              <li><a href="#" class="text-white-50 text-decoration-none" @click.prevent="chonDanhMuc('ao', 3)">Áo Quần Thi Đấu</a></li>
              <li><a href="#" class="text-white-50 text-decoration-none" @click.prevent="chonDanhMuc('phu-kien', 5)">Cước Căng Yonex BG65</a></li>
            </ul>
          </div>
          <div class="col-lg-3 col-md-6">
            <h6 class="fw-bold text-white mb-3">CHÍNH SÁCH BÁN HÀNG</h6>
            <ul class="list-unstyled small text-white-50 lh-lg">
              <li><i class="bi bi-check2-circle text-warning me-1"></i> Đổi trả hàng trong 7 ngày</li>
              <li><i class="bi bi-check2-circle text-warning me-1"></i> Bảo hành khung vợt 3 tháng</li>
              <li><i class="bi bi-check2-circle text-warning me-1"></i> Giao hàng hỏa tốc 2h nội thành</li>
              <li><i class="bi bi-check2-circle text-warning me-1"></i> Tặng kèm bao vợt + quấn cán</li>
            </ul>
          </div>
          <div class="col-lg-3 col-md-6">
            <h6 class="fw-bold text-white mb-3">PHƯƠNG THỨC THANH TOÁN</h6>
            <div class="d-flex flex-wrap gap-2 mb-3">
              <span class="badge bg-secondary p-2"><i class="bi bi-cash-stack me-1"></i> COD Nhận Hàng</span>
              <span class="badge bg-warning text-dark p-2"><i class="bi bi-qr-code-scan me-1"></i> Chuyển Khoản VietQR</span>
            </div>
            <h6 class="fw-bold text-white mb-2">ĐỐI TÁC VẬN CHUYỂN</h6>
            <div class="d-flex gap-2">
              <span class="badge bg-light text-dark p-1">Giao Hàng Nhanh</span>
              <span class="badge bg-light text-dark p-1">Viettel Post</span>
            </div>
          </div>
        </div>

        <div class="pt-3 d-flex flex-wrap justify-content-between align-items-center small text-white-50">
          <div>© 2026 FOOTSTYLE BADMINTON STORE - GIẢI PHÁP BÁN HÀNG CẦU LÔNG CHUYÊN NGHIỆP.</div>
          <div>Bảo mật 256-bit SSL · Kết nối CSDL SQL Server</div>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.vnb-storefront {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #1a1a1a;
}

.vnb-badge-brand {
  background: #0977ec;
  color: #fff;
  font-size: 0.65rem;
  padding: 2px 6px;
  border-radius: 4px;
  vertical-align: middle;
}

.vnb-search-input {
  border-radius: 999px 0 0 999px;
  border: 2px solid #0977ec;
  font-size: 0.95rem;
}
.vnb-search-input:focus {
  box-shadow: none;
  border-color: #075fc0;
}
.vnb-btn-search {
  background: #0977ec;
  color: #fff;
  border: 2px solid #0977ec;
  border-radius: 0 999px 999px 0;
  font-weight: 600;
  transition: all 0.2s;
}
.vnb-btn-search:hover {
  background: #075fc0;
  border-color: #075fc0;
  color: #fff;
}

.vnb-cart-btn {
  background: #0977ec;
  color: #fff;
  border-radius: 999px;
  border: none;
  transition: all 0.2s;
}
.vnb-cart-btn:hover {
  background: #075fc0;
  color: #fff;
}

.vnb-nav-bar {
  background: #181d26 !important;
}
.vnb-nav-link {
  font-size: 0.88rem;
  padding: 0.75rem 1rem !important;
  transition: all 0.15s;
}
.vnb-nav-link:hover {
  background: #0977ec;
  color: #fff !important;
}
.vnb-badge-flash {
  background: #ef4444;
  color: #fff;
  font-size: 0.65rem;
  padding: 2px 6px;
  border-radius: 4px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.vnb-user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #0977ec;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.85rem;
}

/* Floating Chat Button */
.vnb-floating-chat {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1050;
}
.vnb-chat-trigger {
  background: #0977ec;
  color: #fff;
  border-radius: 999px;
  padding: 10px 20px;
  border: 2px solid #fff;
  transition: all 0.25s;
}
.vnb-chat-trigger:hover, .vnb-chat-trigger.is-active {
  background: #075fc0;
  color: #fff;
  transform: translateY(-2px);
}
.vnb-chat-pulse {
  width: 10px;
  height: 10px;
  background: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 2px #fff;
}
.vnb-chat-popup {
  position: absolute;
  bottom: 60px;
  right: 0;
  width: 380px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(0,0,0,0.25) !important;
}
.vnb-chat-header {
  background: #181d26;
}
.vnb-bot-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #0977ec;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
