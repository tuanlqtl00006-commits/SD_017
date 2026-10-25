<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ToastHost from '../components/common/ToastHost.vue'
import DoiMatKhauModal from '../components/auth/DoiMatKhauModal.vue'
import GiaoCaNut from '../components/giaoCa/GiaoCaNut.vue'
import { THUOC_TINH_LIST } from '../constants/thuocTinh'
import { useAuth } from '../composables/useAuth'
import { useChatChuaDoc } from '../composables/useChatChuaDoc'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const chuaDoc = useChatChuaDoc()

// Breadcrumb trên header lấy từ meta của route (xem router/index.js)
const pageTitle = computed(() => route.meta.title || '')
const pageParent = computed(() => route.meta.parent || '')
const pageParentTo = computed(() => route.meta.parentTo || '')

// to: null => chưa có trang, vẫn hiển thị nhưng chưa chuyển trang được.
// children => mục cha có menu con (bấm để mở/đóng).
// Vai trò theo sơ đồ use case: Quản Lý kế thừa Nhân Viên nên thấy đủ mọi chức năng.
// roles: ['nhan-vien'] => cả Nhân Viên và Quản Lý; ['quan-ly'] => chỉ Quản Lý.
const NV = ['nhan-vien', 'quan-ly']
const QL = ['quan-ly']
const menuAll = [
  { key: 'thong-ke', label: 'Thống kê', icon: 'bi-grid', to: '/thong-ke', roles: QL },
  { key: 'ban-hang', label: 'Bán hàng tại quầy', icon: 'bi-shop', to: '/ban-hang', roles: NV },
  { key: 'hoa-don', label: 'Quản lý hóa đơn', icon: 'bi-receipt', to: '/hoa-don', roles: NV },
  {
    key: 'san-pham', label: 'Quản lý sản phẩm', icon: 'bi-box-seam', roles: QL,
    children: [
      { label: 'Sản phẩm', icon: 'bi-box', to: '/san-pham', exact: true },
      { label: 'Biến thể sản phẩm', icon: 'bi-layers', to: '/san-pham/bien-the' },
    ],
  },
  {
    // 9 bảng thuộc tính (danh_muc, thuong_hieu, xuat_xu, chat_lieu, do_cung, diem_can_bang, mau_sac, trong_luong, chu_vi)
    key: 'thuoc-tinh', label: 'Danh sách thuộc tính', icon: 'bi-box-seam', roles: QL,
    children: THUOC_TINH_LIST.map((t) => ({ label: t.label, icon: t.icon, to: `/san-pham/${t.slug}` })),
  },
  {
    key: 'giam-gia', label: 'Quản lý giảm giá', icon: 'bi-percent', roles: QL,
    children: [
      { label: 'Phiếu giảm giá', icon: 'bi-ticket-detailed', to: '/phieu-giam-gia' },
      { label: 'Đợt giảm giá', icon: 'bi-tag', to: '/dot-giam-gia' },
    ],
  },
  { key: 'khach-hang', label: 'Quản lý khách hàng', icon: 'bi-people', to: '/khach-hang', roles: NV },
  { key: 'nhan-vien', label: 'Quản lý nhân viên', icon: 'bi-person', to: '/nhan-vien', roles: QL },
  {
    // Nhận ca / giao ca bấm nút "Ca làm việc" trên header; ở đây là lịch và lịch sử
    key: 'ca-lam', label: 'Ca làm việc', icon: 'bi-calendar-week', roles: NV,
    children: [
      { label: 'Lịch làm của tôi', icon: 'bi-calendar-check', to: '/lich-cua-toi' },
      { label: 'Xếp lịch làm việc', icon: 'bi-calendar3', to: '/lich-lam-viec', roles: QL },
      { label: 'Lịch sử giao ca', icon: 'bi-arrow-left-right', to: '/giao-ca' },
    ],
  },
  {
    // badge: khóa trong useChatChuaDoc().dem (hoTro = cuộc chat khách cần xử lý, noiBo = tin nội bộ chưa đọc)
    key: 'tro-chuyen', label: 'Trò chuyện', icon: 'bi-chat-dots', roles: NV,
    children: [
      { label: 'Hỗ trợ khách hàng', icon: 'bi-headset', to: '/ho-tro-truc-tuyen', badge: 'hoTro' },
      { label: 'Chat nội bộ', icon: 'bi-people-fill', to: '/chat-noi-bo', badge: 'noiBo' },
    ],
  },
]

// Số trên menu (tối đa "99+"); mục cha cộng số của các mục con để thấy ngay khi nhóm đang đóng
const soBadge = (khoa) => (khoa ? chuaDoc.dem[khoa] || 0 : 0)
const nhanBadge = (n) => (n > 99 ? '99+' : String(n))
const tongBadge = (item) => (item.children || []).reduce((s, c) => s + soBadge(c.badge), 0)

// Vai trò lấy từ tài khoản đang đăng nhập; menu con cũng có thể giới hạn vai trò riêng
const role = computed(() => auth.vaiTro.value)
const menu = computed(() =>
  menuAll
    .filter((m) => m.roles.includes(role.value))
    .map((m) => (m.children ? { ...m, children: m.children.filter((c) => !c.roles || c.roles.includes(role.value)) } : m)),
)

/* ----- Tài khoản (góc phải header) ----- */
const moTaiKhoan = ref(false)
const moDoiMatKhau = ref(false)
const taiKhoanRef = ref(null)
function dongKhiBamNgoai(e) {
  if (moTaiKhoan.value && taiKhoanRef.value && !taiKhoanRef.value.contains(e.target)) moTaiKhoan.value = false
}
onMounted(() => {
  document.addEventListener('mousedown', dongKhiBamNgoai)
  chuaDoc.batDau()
})
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', dongKhiBamNgoai)
  chuaDoc.dung()
})
function dangXuat() {
  moTaiKhoan.value = false
  auth.dangXuat()
  router.replace('/dang-nhap')
}

// Mục menu sáng cả khi đang ở trang con (vd /hoa-don/1 vẫn sáng "Quản lý hóa đơn")
const isActive = (to, exact = false) => !!to && (route.path === to || (!exact && route.path.startsWith(to + '/')))
// Nội dung luôn hiển thị ở 75% (không còn ô phóng to / thu nhỏ ở header). Thanh bên trái lớn hơn nội dung ~10% cho vừa mắt.
const ZOOM = 0.75
{
  const root = document.documentElement.style
  root.setProperty('--ui-zoom', String(ZOOM))
  root.setProperty('--ui-side-zoom', String(Math.min(1, Math.max(0.8, ZOOM + 0.1))))
}

// Nhóm có menu con mặc định đóng cho gọn; bấm vào nhóm để mở/đóng.
const open = reactive({ 'san-pham': false, 'thuoc-tinh': false, 'giam-gia': false, 'ca-lam': false, 'tro-chuyen': false })
for (const g of menuAll) {
  if (g.children?.some((c) => isActive(c.to, c.exact))) open[g.key] = true
}
const toggle = (key) => { open[key] = !open[key] }
</script>

<template>
  <div class="d-flex" style="min-height: calc(100vh / var(--ui-zoom)); background-color: #f3f6fb;">
    <!-- Thanh bên -->
    <aside class="bg-white border-end d-flex flex-column flex-shrink-0 ad-aside">
      <div class="ad-logo p-3 text-center mb-1 d-flex align-items-center justify-content-center">
        <img src="/logo.png" alt="FootStyle" class="img-fluid" style="width: 80%; object-fit: contain;">
      </div>

      <nav aria-label="Menu chính" class="pb-4">
        <ul class="nav flex-column fw-medium px-2 m-0" style="font-size: 0.9rem; gap: 0.2rem;">
          <li v-for="item in menu" :key="item.key" class="nav-item">
            <!-- Mục có menu con -->
            <template v-if="item.children">
              <button
                type="button"
                class="nav-link side-link ad-link w-100 border-0 justify-content-between"
                :aria-expanded="!!open[item.key]"
                @click="toggle(item.key)"
              >
                <span><i class="bi icon" :class="item.icon" aria-hidden="true"></i>{{ item.label }}</span>
                <span class="d-flex align-items-center gap-2">
                  <span v-if="!open[item.key] && tongBadge(item)" class="ad-menu-badge" :aria-label="`${tongBadge(item)} mục chưa xem`">{{ nhanBadge(tongBadge(item)) }}</span>
                  <i class="bi bi-chevron-down chevron" :class="{ up: open[item.key] }" aria-hidden="true"></i>
                </span>
              </button>
              <ul v-if="open[item.key] && item.children.length" class="nav flex-column mt-1 ad-sub">
                <li v-for="c in item.children" :key="c.label" class="nav-item">
                  <RouterLink v-if="c.to" :to="c.to" class="nav-link side-link ad-link ad-sublink" :class="{ active: isActive(c.to, c.exact) }">
                    <i class="bi icon" :class="c.icon" aria-hidden="true"></i>{{ c.label }}
                    <span v-if="soBadge(c.badge)" class="ad-menu-badge" :aria-label="`${soBadge(c.badge)} mục chưa xem`">{{ nhanBadge(soBadge(c.badge)) }}</span>
                  </RouterLink>
                  <a v-else href="#" class="nav-link side-link ad-link ad-sublink" @click.prevent>
                    <i class="bi icon" :class="c.icon" aria-hidden="true"></i>{{ c.label }}
                  </a>
                </li>
              </ul>
            </template>
            <!-- Mục thường -->
            <RouterLink v-else-if="item.to" :to="item.to" class="nav-link side-link ad-link" :class="{ active: isActive(item.to) }">
              <i class="bi icon" :class="item.icon" aria-hidden="true"></i>{{ item.label }}
            </RouterLink>
            <a v-else href="#" class="nav-link side-link ad-link" @click.prevent>
              <i class="bi icon" :class="item.icon" aria-hidden="true"></i>{{ item.label }}
            </a>
          </li>
        </ul>
      </nav>
    </aside>

    <!-- Khu vực nội dung -->
    <div class="flex-grow-1 d-flex flex-column" style="min-width: 0;">
      <!-- Header gọn theo nhận xét của cô: chỉ còn tiêu đề trang + Ca làm việc, Thông báo, Tài khoản
           (đã bỏ ô phóng to 75%, ô chọn vai trò "Quản lý" và nút sáng/tối chưa dùng) -->
      <header class="bg-white border-bottom px-4 py-3 d-flex flex-wrap align-items-center gap-3">
        <div class="me-auto d-flex align-items-baseline gap-2">
          <template v-if="pageTitle">
            <RouterLink v-if="pageParent && pageParentTo" :to="pageParentTo" class="ad-crumb-parent text-decoration-none">{{ pageParent }}</RouterLink>
            <span v-else-if="pageParent" class="ad-crumb-parent">{{ pageParent }}</span>
            <span v-if="pageParent" class="ad-crumb-sep" aria-hidden="true">/</span>
            <h1 class="ad-page-title">{{ pageTitle }}</h1>
          </template>
        </div>

        <GiaoCaNut v-if="auth.daDangNhap.value && !auth.laKhachHang.value" />

        <div ref="taiKhoanRef" class="position-relative ms-1">
          <button type="button" class="ad-tk-nut" :aria-expanded="moTaiKhoan" aria-haspopup="menu" @click="moTaiKhoan = !moTaiKhoan">
            <span class="ad-tk-avatar">{{ auth.vietTat.value }}</span>
            <span class="ad-tk-ten d-none d-md-flex">
              <span class="fw-medium text-dark">{{ auth.trangThai.nguoiDung?.hoTen || 'Tài khoản' }}</span>
              <small class="text-muted">{{ auth.trangThai.nguoiDung?.tenVaiTro }}</small>
            </span>
            <i class="bi bi-chevron-down small text-muted" aria-hidden="true"></i>
          </button>
          <div v-if="moTaiKhoan" class="ad-tk-menu" role="menu">
            <div class="ad-tk-dau">
              <div class="fw-semibold">{{ auth.trangThai.nguoiDung?.hoTen }}</div>
              <div class="small text-muted">{{ auth.trangThai.nguoiDung?.ma }} · {{ auth.trangThai.nguoiDung?.tenVaiTro }}</div>
              <div v-if="auth.trangThai.nguoiDung?.email" class="small text-muted text-truncate">{{ auth.trangThai.nguoiDung.email }}</div>
            </div>
            <template v-if="auth.trangThai.batDangNhap !== false">
              <button type="button" class="ad-tk-muc" role="menuitem" @click="moDoiMatKhau = true; moTaiKhoan = false">
                <i class="bi bi-key" aria-hidden="true"></i> Đổi mật khẩu
              </button>
              <button type="button" class="ad-tk-muc is-danger" role="menuitem" @click="dangXuat">
                <i class="bi bi-box-arrow-right" aria-hidden="true"></i> Đăng xuất
              </button>
            </template>
            <div v-else class="ad-tk-muc text-muted small">Đăng nhập đang tắt (app.auth.bat=false)</div>
          </div>
        </div>
      </header>

      <main class="p-4">
        <RouterView :key="route.fullPath" />
      </main>
    </div>

    <ToastHost />
    <DoiMatKhauModal v-if="moDoiMatKhau" @close="moDoiMatKhau = false" />
  </div>
</template>

<style scoped>
/* Thanh bên có độ phóng to riêng (--ui-side-zoom): body đã zoom --ui-zoom nên ở đây nhân thêm tỉ lệ bù, chiều cao chia cho hệ số hiệu dụng để luôn phủ hết màn hình */
.ad-aside { position: sticky; top: 0; zoom: calc(var(--ui-side-zoom) / var(--ui-zoom)); height: calc(100vh / var(--ui-side-zoom)); overflow-y: auto; overflow-x: hidden; width: 300px; }
.ad-logo { height: 100px; }
.ad-link { white-space: nowrap; display: flex; align-items: center; padding: 0.7rem 1rem; border-radius: 0.5rem; text-align: left; }
.ad-link .icon { font-size: 1.15rem; margin-right: 0.8rem; width: 24px; text-align: center; }
.ad-link.justify-content-between .icon { margin-right: 0.8rem; }
.ad-link.justify-content-between > span { display: flex; align-items: center; }
.chevron { font-size: 0.75rem; transition: transform 0.2s ease; }
.chevron.up { transform: rotate(180deg); }
.ad-sublink { padding: 0.6rem 1rem 0.6rem 2.8rem; font-size: 0.9rem; color: #6c757d; }
.ad-menu-badge { margin-left: auto; min-width: 1.4rem; padding: 0 0.4rem; border-radius: 999px; background: #e5484d; color: #fff; font-size: 0.72rem; font-weight: 700; line-height: 1.35rem; text-align: center; }
.ad-tk-nut { display: flex; align-items: center; gap: 0.6rem; border: 0; background: transparent; padding: 0.2rem 0.4rem; border-radius: 10px; }
.ad-tk-nut:hover { background: var(--fs-hover-bg, #e7f1fd); }
.ad-tk-avatar { width: 35px; height: 35px; border-radius: 50%; background: var(--fs-primary, #0977ec); color: #fff; font-weight: 600; font-size: 0.8rem; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ad-tk-ten { flex-direction: column; align-items: flex-start; line-height: 1.15; text-align: left; }
.ad-tk-menu { position: absolute; right: 0; top: calc(100% + 0.4rem); z-index: 1050; min-width: 240px; background: #fff; border: 1px solid var(--fs-line, #e6eaf0); border-radius: 12px; box-shadow: 0 12px 28px rgba(24, 36, 47, 0.12); padding: 0.4rem; }
.ad-tk-dau { padding: 0.6rem 0.75rem; border-bottom: 1px solid var(--fs-line-soft, #eef1f5); margin-bottom: 0.3rem; }
.ad-tk-muc { display: flex; width: 100%; align-items: center; gap: 0.6rem; border: 0; background: transparent; padding: 0.55rem 0.75rem; border-radius: 8px; text-align: left; color: var(--fs-text, #18242f); }
.ad-tk-muc:hover { background: var(--fs-hover-bg, #e7f1fd); }
.ad-tk-muc.is-danger { color: #c62828; }
.ad-sublink .icon { font-size: 1rem; margin-right: 0.6rem; width: 20px; }

/* Màn hình thấp (zoom 100% trên laptop): thu gọn khoảng cách để thấy ĐỦ các chức năng mà không phải cuộn */
@media (max-height: 820px) {
  .ad-logo { height: 76px; padding-block: 0.5rem !important; }
  .ad-link { padding: 0.45rem 0.85rem; font-size: 0.92rem; }
  .ad-link .icon { font-size: 1.05rem; margin-right: 0.7rem; }
  .ad-sublink { padding: 0.38rem 1rem 0.38rem 2.6rem; font-size: 0.86rem; }
  .ad-sub { margin-top: 0 !important; }
}
</style>
