<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ToastHost from '../components/common/ToastHost.vue'
import { THUOC_TINH_LIST } from '../constants/thuocTinh'

const route = useRoute()

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
  { key: 'ban-hang', label: 'Bán hàng tại quầy', icon: 'bi-shop', to: null, roles: NV },
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
  // Chưa có trong sơ đồ use case nên tạm ẩn khỏi menu (bỏ chú thích nếu muốn hiện lại / vẽ thêm vào sơ đồ):
  // { key: 'ho-tro', label: 'Hỗ trợ trực tuyến', icon: 'bi-chat-dots', to: null, roles: NV },
  // { key: 'danh-gia', label: 'Quản lý đánh giá', icon: 'bi-star', to: null, roles: NV },
  // { key: 'lich-lam', label: 'Quản lý lịch làm', icon: 'bi-calendar3', children: [], roles: QL },
]

// Vai trò đang dùng (chưa có đăng nhập nên chọn ở header, nhớ lại sau khi tải lại trang)
const readLS = (k, d) => { try { return localStorage.getItem(k) ?? d } catch { return d } }
const writeLS = (k, v) => { try { localStorage.setItem(k, v) } catch { /* bỏ qua */ } }
const role = ref(readLS('fs-role', 'quan-ly'))
watch(role, (v) => writeLS('fs-role', v))
const menu = computed(() => menuAll.filter((m) => m.roles.includes(role.value)))

// Mục menu sáng cả khi đang ở trang con (vd /hoa-don/1 vẫn sáng "Quản lý hóa đơn")
const isActive = (to, exact = false) => !!to && (route.path === to || (!exact && route.path.startsWith(to + '/')))
// Phóng to / thu nhỏ phần nội dung bên phải (mặc định 75%). Thanh bên trái tự co giãn theo cho vừa mắt:
// luôn lớn hơn nội dung ~10% nhưng chỉ trong khoảng 80% - 100% để menu không quá bé / quá to.
const ZOOM_DEFAULT = 75, ZOOM_MIN = 50, ZOOM_MAX = 125, ZOOM_STEP = 5
const zoomPct = ref(Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, Number(readLS('fs-zoom', String(ZOOM_DEFAULT))) || ZOOM_DEFAULT)))
const applyZoom = () => {
  const z = zoomPct.value / 100
  const root = document.documentElement.style
  root.setProperty('--ui-zoom', String(z))
  root.setProperty('--ui-side-zoom', String(Math.min(1, Math.max(0.8, z + 0.1))))
}
applyZoom()
watch(zoomPct, (v) => { writeLS('fs-zoom', String(v)); applyZoom() })
const zoomBy = (d) => { zoomPct.value = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, zoomPct.value + d)) }

// Nhóm có menu con mặc định đóng cho gọn; bấm vào nhóm để mở/đóng.
const open = reactive({ 'san-pham': false, 'thuoc-tinh': false, 'giam-gia': false })
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
                <i class="bi bi-chevron-down chevron" :class="{ up: open[item.key] }" aria-hidden="true"></i>
              </button>
              <ul v-if="open[item.key] && item.children.length" class="nav flex-column mt-1 ad-sub">
                <li v-for="c in item.children" :key="c.label" class="nav-item">
                  <RouterLink v-if="c.to" :to="c.to" class="nav-link side-link ad-link ad-sublink" :class="{ active: isActive(c.to, c.exact) }">
                    <i class="bi icon" :class="c.icon" aria-hidden="true"></i>{{ c.label }}
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
      <header class="bg-white border-bottom px-4 py-3 d-flex flex-wrap align-items-center gap-3">
        <div class="me-auto d-flex align-items-baseline gap-2">
          <template v-if="pageTitle">
            <RouterLink v-if="pageParent && pageParentTo" :to="pageParentTo" class="ad-crumb-parent text-decoration-none">{{ pageParent }}</RouterLink>
            <span v-else-if="pageParent" class="ad-crumb-parent">{{ pageParent }}</span>
            <span v-if="pageParent" class="ad-crumb-sep" aria-hidden="true">/</span>
            <h1 class="ad-page-title">{{ pageTitle }}</h1>
          </template>
        </div>

        <!-- Phóng to / thu nhỏ phần nội dung -->
        <div class="btn-group btn-group-sm" role="group" aria-label="Phóng to thu nhỏ nội dung">
          <button type="button" class="btn btn-outline-secondary" :disabled="zoomPct <= ZOOM_MIN" aria-label="Thu nhỏ" @click="zoomBy(-ZOOM_STEP)"><i class="bi bi-dash-lg"></i></button>
          <button type="button" class="btn btn-outline-secondary ad-zoom-val" :title="`Về ${ZOOM_DEFAULT}%`" @click="zoomPct = ZOOM_DEFAULT">{{ zoomPct }}%</button>
          <button type="button" class="btn btn-outline-secondary" :disabled="zoomPct >= ZOOM_MAX" aria-label="Phóng to" @click="zoomBy(ZOOM_STEP)"><i class="bi bi-plus-lg"></i></button>
        </div>
        <!-- Chọn vai trò để xem menu theo use case (chưa có đăng nhập) -->
        <select v-model="role" class="form-select form-select-sm ad-role" aria-label="Vai trò">
          <option value="quan-ly">Quản Lý</option>
          <option value="nhan-vien">Nhân Viên</option>
        </select>
        <button class="btn btn-light btn-sm border rounded-3 px-3 fw-medium d-flex align-items-center gap-2">
          <i class="bi bi-arrow-left-right text-secondary"></i> Ca làm việc <span class="ad-live-dot" aria-hidden="true"></span>
        </button>
        <button class="btn btn-light rounded-circle p-2 lh-1 text-muted" aria-label="Đổi giao diện sáng/tối">
          <i class="bi bi-moon"></i>
        </button>
        <div class="position-relative">
          <button class="btn btn-light rounded-circle p-2 lh-1 text-muted" aria-label="Thông báo">
            <i class="bi bi-bell"></i>
          </button>
          <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">4</span>
        </div>
        <div class="d-flex align-items-center gap-2 ms-1">
          <span class="rounded-circle bg-primary text-white fw-semibold d-flex align-items-center justify-content-center" style="width: 35px; height: 35px; font-size: 0.8rem;">AD</span>
          <span class="fw-medium text-dark d-none d-md-block">Admin</span>
        </div>
      </header>

      <main class="p-4">
        <RouterView :key="route.fullPath" />
      </main>
    </div>

    <ToastHost />
  </div>
</template>

<style scoped>
/* Thanh bên có độ phóng to riêng (--ui-side-zoom): body đã zoom --ui-zoom nên ở đây nhân thêm tỉ lệ bù, chiều cao chia cho hệ số hiệu dụng để luôn phủ hết màn hình */
.ad-aside { position: sticky; top: 0; zoom: calc(var(--ui-side-zoom) / var(--ui-zoom)); height: calc(100vh / var(--ui-side-zoom)); overflow-y: auto; overflow-x: hidden; width: 300px; }
.ad-logo { height: 100px; }
.ad-role { width: auto; }
.ad-zoom-val { min-width: 3.4rem; }
.ad-link { white-space: nowrap; display: flex; align-items: center; padding: 0.7rem 1rem; border-radius: 0.5rem; text-align: left; }
.ad-link .icon { font-size: 1.15rem; margin-right: 0.8rem; width: 24px; text-align: center; }
.ad-link.justify-content-between .icon { margin-right: 0.8rem; }
.ad-link.justify-content-between > span { display: flex; align-items: center; }
.chevron { font-size: 0.75rem; transition: transform 0.2s ease; }
.chevron.up { transform: rotate(180deg); }
.ad-sublink { padding: 0.6rem 1rem 0.6rem 2.8rem; font-size: 0.9rem; color: #6c757d; }
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
