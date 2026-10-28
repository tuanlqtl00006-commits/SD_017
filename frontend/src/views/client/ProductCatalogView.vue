<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sanPhamService, bienTheService } from '../../services/sanPhamService'
import { cuaHangService } from '../../services/cuaHangService'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { thuocTinhService } from '../../services/thuocTinhService'
import { formatMoney } from '../../utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const products = ref([])
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }
const brands = ref([])

// ===== DANH MỤC HIỆN TẠI =====
const currentCategory = ref('vot-cau-long') // default
const pillsScrollRef = ref(null)

function scrollCategories(offset) {
  if (pillsScrollRef.value) {
    pillsScrollRef.value.scrollBy({ left: offset, behavior: 'smooth' })
  }
}

// Hỗ trợ cuộn chuột lăn trực tiếp khi rê chuột vào thanh danh mục
function onPillsWheel(e) {
  if (pillsScrollRef.value) {
    pillsScrollRef.value.scrollLeft += e.deltaY || e.deltaX
  }
}

let isDragging = false
let startX = 0
let scrollLeftStart = 0

function onMouseDown(e) {
  if (!pillsScrollRef.value) return
  isDragging = true
  startX = e.pageX - pillsScrollRef.value.offsetLeft
  scrollLeftStart = pillsScrollRef.value.scrollLeft
}

function onMouseMove(e) {
  if (!isDragging || !pillsScrollRef.value) return
  e.preventDefault()
  const x = e.pageX - pillsScrollRef.value.offsetLeft
  const walk = (x - startX) * 1.5
  pillsScrollRef.value.scrollLeft = scrollLeftStart - walk
}

function onMouseUp() {
  isDragging = false
}

function getCategoryIcon(name) {
  const n = (name || '').toLowerCase()
  if (n.includes('vợt')) return 'bi-record-circle'
  if (n.includes('quần') || n.includes('áo') || n.includes('thể thao')) return 'bi-person-fill'
  if (n.includes('túi') || n.includes('balo')) return 'bi-bag-fill'
  if (n.includes('phụ kiện')) return 'bi-tools'
  if (n.includes('cầu')) return 'bi-circle'
  if (n.includes('giày')) return 'bi-shield-shaded'
  return 'bi-tag-fill'
}

// Danh mục thật từ CSDL (loại bỏ hoàn toàn các danh mục ảo và Pickleball)
const categories = ref([
  { key: 'all', id: null, title: 'Tất cả sản phẩm', icon: 'bi-grid-fill' },
  { key: '1', id: 1, title: 'Vợt cầu lông', icon: 'bi-record-circle' },
  { key: '2', id: 2, title: 'Vợt cầu lông người mới', icon: 'bi-record-circle' },
  { key: '3', id: 3, title: 'Quần áo thể thao', icon: 'bi-person-fill' },
  { key: '4', id: 4, title: 'Túi - balo', icon: 'bi-bag-fill' },
  { key: '5', id: 5, title: 'Phụ kiện', icon: 'bi-tools' },
  { key: '6', id: 6, title: 'Quả cầu lông', icon: 'bi-circle' },
])

const currentCategoryTitle = computed(() => {
  const cat = categories.value.find(
    (c) => String(c.key) === String(currentCategory.value) || String(c.id) === String(currentCategory.value)
  )
  return cat ? cat.title : 'Sản phẩm'
})

const currentBanner = computed(() => {
  const activeCat = categories.value.find(
    (c) => String(c.key) === String(currentCategory.value) || String(c.id) === String(currentCategory.value)
  )
  const title = activeCat && activeCat.key !== 'all' ? activeCat.title.toUpperCase() : 'TẤT CẢ SẢN PHẨM'
  return {
    badge: 'FOOTSTYLE STORE',
    title: title,
    highlight: 'CHÍNH HÃNG',
    tag: 'DỤNG CỤ, PHỤ KIỆN & THỜI TRANG CẦU LÔNG CHUYÊN NGHIỆP',
    priceText: 'Cam kết chất lượng 100%',
    bg: 'linear-gradient(90deg, #18181b 0%, #b91c1c 45%, #18181b 100%)',
  }
})

// ===== BỘ LỌC CHUNG =====
const selectedPrices = ref([])
const selectedBrands = ref([])

// ===== BỘ LỌC THEO THUỘC TÍNH (lấy từ danh sách thuộc tính thật trong CSDL) =====
const ATTR_DEFS = [
  { key: 'xuatXu', title: 'XUẤT XỨ', type: 'xuat-xu', values: (p) => [p.xuatXu] },
  { key: 'chatLieu', title: 'CHẤT LIỆU', type: 'chat-lieu', values: (p) => [p.chatLieu] },
  { key: 'doCung', title: 'ĐỘ CỨNG', type: 'do-cung', values: (p) => [p.doCung] },
  { key: 'diemCanBang', title: 'ĐIỂM CÂN BẰNG', type: 'diem-can-bang', values: (p) => [p.diemCanBang] },
  { key: 'mauSac', title: 'MÀU SẮC', type: 'mau-sac', values: (p) => (p.bienThe || []).map((b) => b.tenMau) },
  { key: 'trongLuong', title: 'TRỌNG LƯỢNG', type: 'trong-luong', values: (p) => (p.bienThe || []).map((b) => b.trongLuong) },
  { key: 'chuVi', title: 'CHU VI CÁN', type: 'chu-vi', values: (p) => (p.bienThe || []).map((b) => b.chuVi) },
]

const attrSelected = ref(Object.fromEntries(ATTR_DEFS.map((g) => [g.key, []])))
const attrOpen = ref({})

// Sản phẩm có thuộc về danh mục đang chọn không ('all' = tất cả)
function khopDanhMuc(p) {
  const cur = currentCategory.value
  if (!cur || cur === 'all') return true
  if (p.idDanhMuc != null && String(p.idDanhMuc) === String(cur)) return true
  if (p.category && String(p.category) === String(cur)) return true
  const catObj = categories.value.find((c) => String(c.key) === String(cur) || String(c.id) === String(cur))
  return !!(catObj && p.tenDanhMuc && catObj.title && p.tenDanhMuc.toLowerCase() === catObj.title.toLowerCase())
}

// Mỗi loại danh mục có bộ lọc thuộc tính RIÊNG (vợt khác balo khác quần áo...)
function loaiDanhMuc(ten) {
  const t = (ten || '').toLowerCase()
  if (t.includes('quả')) return 'qua-cau'
  if (t.includes('vợt')) return 'vot'
  if (t.includes('quần') || t.includes('áo') || t.includes('thể thao')) return 'quan-ao'
  if (t.includes('túi') || t.includes('balo')) return 'tui-balo'
  if (t.includes('phụ kiện')) return 'phu-kien'
  return 'khac'
}

const ATTR_THEO_LOAI = {
  vot: ['xuatXu', 'chatLieu', 'doCung', 'diemCanBang', 'mauSac', 'trongLuong', 'chuVi'],
  'quan-ao': ['chatLieu', 'mauSac', 'xuatXu'],
  'tui-balo': ['mauSac', 'chatLieu', 'xuatXu'],
  'phu-kien': ['mauSac', 'chatLieu'],
  'qua-cau': ['chatLieu', 'xuatXu'],
  khac: ['xuatXu', 'chatLieu', 'mauSac'],
}

// Danh sách thuộc tính công khai từ backend (để danh mục chưa có sản phẩm vẫn có đủ bộ lọc)
const attrOptions = ref({})

// Chất liệu có thể gắn theo nhóm danh mục (VOT, QUAN_AO, TUI_BALO, PHU_KIEN); trống = dùng cho mọi danh mục
const NHOM_THEO_LOAI = { vot: 'VOT', 'quan-ao': 'QUAN_AO', 'tui-balo': 'TUI_BALO', 'phu-kien': 'PHU_KIEN' }

function tuDanhSachThuocTinh(key, loai) {
  const ds = attrOptions.value[key] || []
  if (key !== 'chatLieu') return ds
  const nhom = NHOM_THEO_LOAI[loai]
  return ds
    .filter((c) => !c.nhom || !nhom || c.nhom.split(',').map((x) => x.trim()).includes(nhom))
    .map((c) => c.ten)
}

// Nhóm lọc hợp với danh mục đang xem: giá trị lấy từ danh sách thuộc tính + từ các sản phẩm của danh mục
const attributeGroups = computed(() => {
  const loai = loaiDanhMuc(currentCategoryTitle.value)
  const cho = ATTR_THEO_LOAI[loai] || []
  const trongDanhMuc = products.value.filter(khopDanhMuc)
  return cho
    .map((key) => ATTR_DEFS.find((g) => g.key === key))
    .filter(Boolean)
    .map((g) => ({
      key: g.key,
      title: g.title,
      options: [...new Set([...tuDanhSachThuocTinh(g.key, loai), ...trongDanhMuc.flatMap((p) => g.values(p))].filter(Boolean))],
    }))
    .filter((g) => g.options.length > 0)
})

// Đổi danh mục thì bỏ các lựa chọn thuộc tính cũ (không còn áp dụng)
watch(currentCategory, () => {
  for (const g of ATTR_DEFS) attrSelected.value[g.key] = []
})

function toggleAttr(key) {
  attrOpen.value[key] = attrOpen.value[key] === false
}

// Trạng thái toggle từng section
const sectionOpen = ref({
  gia: true,
  thuongHieu: true,
})

function toggleSection(key) {
  sectionOpen.value[key] = !sectionOpen.value[key]
}

function clearAllFilters() {
  selectedPrices.value = []
  selectedBrands.value = []
  for (const g of ATTR_DEFS) attrSelected.value[g.key] = []
}

const sortBy = ref('default')
const searchKeyword = ref('')

function getProductCategory(sp) {
  if (sp.idDanhMuc != null) return String(sp.idDanhMuc)
  if (sp.category) return String(sp.category)
  return '1'
}

const FALLBACK_BRANDS = [
  { id: 1, ten: 'Yonex' },
  { id: 2, ten: 'Lining' },
  { id: 3, ten: 'Victor' },
  { id: 4, ten: 'Mizuno' },
  { id: 5, ten: 'Kumpoo' },
]

onMounted(async () => {
  try {
    loading.value = true
    const [spRes, btRes, dggRes, dmThRes] = await Promise.all([
      cuaHangService.getDanhSachSanPham(),
      bienTheService.getAll().catch(() => []),
      dotGiamGiaService.getApDungHomNay().catch(() => ({})),
      cuaHangService.getDanhMucVaThuongHieu().catch(() => null),
    ])

    products.value = spRes || []
    variants.value = btRes || []
    discounts.value = dggRes || {}

    if (dmThRes?.danhMuc?.length > 0) {
      categories.value = [
        { key: 'all', id: null, title: 'Tất cả sản phẩm', icon: 'bi-grid-fill' },
        ...dmThRes.danhMuc.map((dm) => ({
          key: String(dm.id),
          id: dm.id,
          title: dm.ten,
          icon: getCategoryIcon(dm.ten),
        })),
      ]
    }

    attrOptions.value = dmThRes?.thuocTinh || {}

    if (dmThRes?.thuongHieu?.length > 0) {
      brands.value = dmThRes.thuongHieu
    } else {
      brands.value = FALLBACK_BRANDS
    }

    syncFromUrl()
  } catch (err) {
    console.error('Lỗi khi tải sản phẩm:', err)
    brands.value = FALLBACK_BRANDS
    syncFromUrl()
  } finally {
    loading.value = false
  }
})

function selectCategory(catKey) {
  currentCategory.value = catKey
  clearAllFilters()
  if (catKey === 'all') {
    router.push({ path: '/shop/san-pham' })
  } else {
    router.push({ path: '/shop/san-pham', query: { danhMuc: catKey } })
  }
}

function syncFromUrl() {
  if (route.query.danhMuc) {
    currentCategory.value = String(route.query.danhMuc)
  } else if (route.query.khuyenMai === '1' || route.query.sale === '1') {
    currentCategory.value = 'all'
  } else {
    currentCategory.value = 'all'
  }

  if (route.query.search) {
    searchKeyword.value = String(route.query.search)
  } else {
    searchKeyword.value = ''
  }

  if (route.query.thuongHieu) {
    selectedBrands.value = [String(route.query.thuongHieu)]
  } else {
    selectedBrands.value = []
  }
}

watch(
  () => route.query,
  () => {
    syncFromUrl()
  }
)

const enrichedProducts = computed(() => {
  return products.value
    .filter((p) => p.hoatDong !== 0 && p.hoatDong !== false)
    .map((sp) => {
      let minPrice = sp.minPrice
      let originalPrice = sp.originalPrice
      let maxDiscountPercent = sp.maxDiscountPercent || 0

      // Nếu sản phẩm chưa có giá tính trước, tính từ biến thể thật
      if (minPrice === undefined || minPrice === null || minPrice === 0) {
        const spVariants = (sp.bienThe && sp.bienThe.length > 0)
          ? sp.bienThe
          : variants.value.filter(
              (v) => v.idSanPham === sp.id && v.hoatDong !== 0 && v.hoatDong !== false
            )

        if (spVariants.length > 0) {
          let min = Infinity
          let orig = Infinity
          for (const v of spVariants) {
            const giaGoc = Number(v.giaBan || v.giaBanGoc) || 0
            const ptGiam = Number(discounts.value[v.id]) || Number(v.phanTramGiam) || 0
            const giaSauGiam = ptGiam > 0 ? giaGoc * (1 - ptGiam / 100) : giaGoc

            if (giaSauGiam > 0 && giaSauGiam < min) {
              min = giaSauGiam
              orig = giaGoc
            }
            if (ptGiam > maxDiscountPercent) {
              maxDiscountPercent = ptGiam
            }
          }
          minPrice = min === Infinity ? 0 : min
          originalPrice = orig === Infinity ? 0 : orig
        } else {
          minPrice = 0
          originalPrice = 0
        }
      }

      const brand = brands.value.find((b) => b.id === sp.idThuongHieu)

      return {
        ...sp,
        id: sp.id,
        ten: sp.ten || sp.tenSanPham || 'Sản phẩm',
        ma: sp.ma || sp.maSanPham || '',
        category: getProductCategory(sp),
        brandName: sp.brandName || sp.tenThuongHieu || brand?.ten || '',
        anhChinh: sp.anhChinh || (sp.danhSachAnh && sp.danhSachAnh[0]) || '',
        minPrice: Number(minPrice) || 0,
        originalPrice: Number(originalPrice) || 0,
        maxDiscountPercent,
        hasDiscount: maxDiscountPercent > 0,
      }
    })
})

const filteredProducts = computed(() => {
  let list = enrichedProducts.value

  // 0. Lọc Khuyến mãi (SALE OFF)
  if (route.query.khuyenMai === '1' || route.query.sale === '1') {
    list = list.filter((p) => p.hasDiscount)
  }

  // 1. Lọc theo Danh mục (nếu không chọn 'all')
  list = list.filter(khopDanhMuc)
  // 2. Tìm kiếm từ khóa
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim()
    list = list.filter(
      (p) =>
        p.ten?.toLowerCase().includes(kw) ||
        p.ma?.toLowerCase().includes(kw) ||
        p.brandName?.toLowerCase().includes(kw)
    )
  }

  // 3. Lọc Thương hiệu
  if (selectedBrands.value.length > 0) {
    list = list.filter((p) => selectedBrands.value.includes(String(p.idThuongHieu)))
  }

  // 4. Lọc Mức giá
  if (selectedPrices.value.length > 0) {
    list = list.filter((p) => {
      return selectedPrices.value.some((range) => {
        if (range === 'under-500k') return p.minPrice < 500000
        if (range === '500k-1m') return p.minPrice >= 500000 && p.minPrice <= 1000000
        if (range === '1m-2m') return p.minPrice >= 1000000 && p.minPrice <= 2000000
        if (range === '2m-3m') return p.minPrice >= 2000000 && p.minPrice <= 3000000
        if (range === 'above-3m') return p.minPrice > 3000000
        return true
      })
    })
  }

  // 4b. Lọc theo thuộc tính (màu sắc, trọng lượng, chu vi, chất liệu, độ cứng, điểm cân bằng)
  for (const g of ATTR_DEFS) {
    const chon = attrSelected.value[g.key] || []
    if (chon.length > 0) {
      list = list.filter((p) => g.values(p).some((v) => chon.includes(v)))
    }
  }

  // 5. Sắp xếp
  if (sortBy.value === 'price-asc') {
    list = [...list].sort((a, b) => a.minPrice - b.minPrice)
  } else if (sortBy.value === 'price-desc') {
    list = [...list].sort((a, b) => b.minPrice - a.minPrice)
  } else if (sortBy.value === 'name-asc') {
    list = [...list].sort((a, b) => (a.ten || '').localeCompare(b.ten || ''))
  }

  return list
})

function goToDetail(id) {
  router.push(`/shop/san-pham/${id}`)
}
</script>

<template>
  <div class="product-catalog-view py-3 bg-white">
    <div class="vnb-container">

      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb small mb-0">
          <li class="breadcrumb-item">
            <RouterLink to="/shop" class="text-decoration-none text-muted">Trang chủ</RouterLink>
          </li>
          <li class="breadcrumb-item">
            <RouterLink to="/shop/san-pham" class="text-decoration-none text-muted" @click="selectCategory('all')">Sản phẩm</RouterLink>
          </li>
          <li class="breadcrumb-item active text-dark fw-bold" aria-current="page">
            {{ currentCategoryTitle }}
          </li>
        </ol>
      </nav>

      <!-- THANH CHỌN DANH MỤC NHANH (CATEGORY TABS VỚI NÚT CUỘN & LĂN CHUỘT) -->
      <div class="vnb-cat-pills-bar mb-4">
        <!-- Nút mũi tên cuộn sang trái -->
        <button
          type="button"
          class="vnb-cat-scroll-arrow"
          @click="scrollCategories(-260)"
          title="Cuộn sang trái"
        >
          <i class="bi bi-chevron-left"></i>
        </button>

        <div
          ref="pillsScrollRef"
          class="vnb-cat-pills-scroll"
          @wheel.prevent="onPillsWheel"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
          @mouseleave="onMouseUp"
        >
          <button
            v-for="cat in categories"
            :key="cat.key"
            type="button"
            class="vnb-cat-pill-btn"
            :class="{ active: String(currentCategory) === String(cat.key) || (cat.id && String(currentCategory) === String(cat.id)) }"
            @click="selectCategory(cat.key)"
          >
            <i :class="['bi', cat.icon, 'me-1']"></i>
            {{ cat.title }}
          </button>
        </div>

        <!-- Nút mũi tên cuộn sang phải -->
        <button
          type="button"
          class="vnb-cat-scroll-arrow"
          @click="scrollCategories(260)"
          title="Cuộn sang phải"
        >
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>

      <div class="row g-4">
        <!-- ============================================== -->
        <!-- CỘT BỘ LỌC BÊN TRÁI (THAY ĐỔI ĐỘNG THEO DANH MỤC) -->
        <!-- ============================================== -->
        <div class="col-lg-3 col-md-4">
          <div class="vnb-filter-panel shadow-sm">

            <!-- Header Panel có nút Xóa lọc -->
            <div class="p-3 border-bottom d-flex align-items-center justify-content-between bg-light">
              <span class="fw-bold text-dark text-uppercase small">
                <i class="bi bi-funnel-fill text-primary me-1"></i> Bộ lọc
              </span>
              <button
                type="button"
                class="btn btn-link btn-sm p-0 text-decoration-none text-danger small fw-semibold"
                @click="clearAllFilters"
                title="Bỏ toàn bộ tiêu chí lọc"
              >
                Xóa tất cả
              </button>
            </div>

            <!-- 1. CHỌN MỨC GIÁ (Chung cho tất cả) -->
            <div class="vnb-filter-section">
              <div class="vnb-filter-section-title" @click="toggleSection('gia')">
                CHỌN MỨC GIÁ
                <i class="bi" :class="sectionOpen.gia ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
              </div>
              <ul v-show="sectionOpen.gia" class="vnb-filter-list">
                <li class="vnb-filter-item">
                  <label>
                    <input type="checkbox" value="under-500k" v-model="selectedPrices" />
                    <span>Giá dưới 500.000đ</span>
                  </label>
                </li>
                <li class="vnb-filter-item">
                  <label>
                    <input type="checkbox" value="500k-1m" v-model="selectedPrices" />
                    <span>500.000đ - 1 triệu</span>
                  </label>
                </li>
                <li class="vnb-filter-item">
                  <label>
                    <input type="checkbox" value="1m-2m" v-model="selectedPrices" />
                    <span>1 - 2 triệu</span>
                  </label>
                </li>
                <li class="vnb-filter-item">
                  <label>
                    <input type="checkbox" value="2m-3m" v-model="selectedPrices" />
                    <span>2 - 3 triệu</span>
                  </label>
                </li>
                <li class="vnb-filter-item">
                  <label>
                    <input type="checkbox" value="above-3m" v-model="selectedPrices" />
                    <span>Giá trên 3 triệu</span>
                  </label>
                </li>
              </ul>
            </div>

            <!-- 2. THƯƠNG HIỆU (Chung cho tất cả) -->
            <div class="vnb-filter-section">
              <div class="vnb-filter-section-title" @click="toggleSection('thuongHieu')">
                THƯƠNG HIỆU
                <i class="bi" :class="sectionOpen.thuongHieu ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
              </div>
              <ul v-show="sectionOpen.thuongHieu" class="vnb-filter-list vnb-filter-list-scroll">
                <li v-for="b in brands" :key="b.id" class="vnb-filter-item">
                  <label>
                    <input type="checkbox" :value="String(b.id)" v-model="selectedBrands" />
                    <span>{{ b.ten }}</span>
                  </label>
                </li>
              </ul>
            </div>

            <!-- 3. CÁC THUỘC TÍNH (lấy từ danh sách thuộc tính trong CSDL) -->
            <div v-for="g in attributeGroups" :key="g.key" class="vnb-filter-section">
              <div class="vnb-filter-section-title" @click="toggleAttr(g.key)">
                {{ g.title }}
                <i class="bi" :class="attrOpen[g.key] !== false ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
              </div>
              <ul v-show="attrOpen[g.key] !== false" class="vnb-filter-list vnb-filter-list-scroll">
                <li v-for="opt in g.options" :key="opt" class="vnb-filter-item">
                  <label>
                    <input type="checkbox" :value="opt" v-model="attrSelected[g.key]" />
                    <span>{{ opt }}</span>
                  </label>
                </li>
              </ul>
            </div>

          </div>
        </div>

        <!-- ============================================== -->
        <!-- CỘT NỘI DUNG SẢN PHẨM BÊN PHẢI                 -->
        <!-- ============================================== -->
        <div class="col-lg-9 col-md-8">

          <!-- Dynamic Banner cho từng Danh mục -->
          <div
            class="rounded-3 p-4 mb-4 text-white position-relative overflow-hidden shadow-sm"
            :style="{ background: currentBanner.bg, minHeight: '190px', display: 'flex', alignItems: 'center' }"
          >
            <div class="row align-items-center w-100">
              <div class="col-md-8">
                <span class="badge bg-dark text-white mb-2 px-2 py-1 border border-secondary">
                  {{ currentBanner.badge }}
                </span>
                <h2
                  class="display-6 fw-black text-uppercase mb-1"
                  style="font-weight: 900; color: #ffffff; text-shadow: 2px 2px 4px rgba(0,0,0,0.6);"
                >
                  {{ currentBanner.title }} <span class="text-warning">{{ currentBanner.highlight }}</span>
                </h2>
                <div class="badge bg-light text-dark fw-bold px-3 py-2 rounded-pill mb-2">
                  {{ currentBanner.tag }}
                </div>
                <div class="fs-5 fw-bold text-white">
                  {{ currentBanner.priceText }}
                </div>
              </div>
              <div class="col-md-4 text-center d-none d-md-block">
                <img
                  src="/logo.png"
                  alt="FootStyle"
                  class="img-fluid p-2 bg-white rounded-3 shadow"
                  style="max-height: 120px; object-fit: contain;"
                />
              </div>
            </div>
          </div>

          <!-- Thanh tiêu đề danh mục & Sắp xếp -->
          <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
            <h4 class="fw-bold mb-0 text-uppercase" style="font-size: 1.15rem; color: #1e293b;">
              {{ currentCategoryTitle }}
              <span class="text-muted fw-normal" style="font-size: 0.85rem;">({{ filteredProducts.length }} sản phẩm)</span>
            </h4>

            <div class="d-flex align-items-center gap-2">
              <span class="small text-muted d-none d-sm-inline">⇅ Sắp xếp:</span>
              <select v-model="sortBy" class="form-select form-select-sm" style="width: auto;">
                <option value="default">Mặc định</option>
                <option value="price-asc">Giá tăng dần</option>
                <option value="price-desc">Giá giảm dần</option>
                <option value="name-asc">Tên A-Z</option>
              </select>
            </div>
          </div>

          <!-- Trạng thái tải -->
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="small text-muted mt-2">Đang tải danh sách sản phẩm...</p>
          </div>

          <!-- Khi không tìm thấy sản phẩm -->
          <div v-else-if="filteredProducts.length === 0" class="card border-0 p-5 text-center text-muted">
            <i class="bi bi-inbox display-4 mb-2"></i>
            <h5>Không tìm thấy sản phẩm nào</h5>
            <p class="small">Thử chọn danh mục khác hoặc bỏ bớt tiêu chí lọc.</p>
            <div class="mt-2">
              <button class="btn btn-outline-primary btn-sm rounded-pill px-3" @click="clearAllFilters">
                Xóa tất cả bộ lọc
              </button>
            </div>
          </div>

          <!-- Lưới sản phẩm Cards -->
          <div v-else class="row g-3">
            <div
              v-for="item in filteredProducts"
              :key="item.id"
              class="col-xl-3 col-lg-4 col-sm-6"
            >
              <div class="vnb-product-item" @click="goToDetail(item.id)">
                <!-- Khung ảnh -->
                <div class="vnb-img-box">
                  <span v-if="item.hasDiscount" class="vnb-badge-discount">-{{ item.maxDiscountPercent }}%</span>
                  <span class="vnb-badge-premium">Chính hãng</span>

                  <!-- Placeholder SVG khi không có ảnh thật -->
                  <div v-if="!item.anhChinh" class="vnb-img-placeholder">
                    <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" width="60" height="60">
                      <rect width="80" height="80" rx="10" fill="#f1f5f9"/>
                      <circle cx="40" cy="36" r="16" stroke="#94a3b8" stroke-width="2"/>
                      <path d="M40 52V68M34 68H46" stroke="#94a3b8" stroke-width="2" stroke-linecap="round"/>
                      <circle cx="40" cy="36" r="8" fill="#e2e8f0"/>
                    </svg>
                    <div class="vnb-img-placeholder-brand">{{ item.brandName }}</div>
                  </div>
                  <img v-else :src="item.anhChinh" :alt="item.ten" class="vnb-product-img" />
                </div>

                <!-- Thông tin sản phẩm -->
                <div class="vnb-product-info">
                  <div class="vnb-product-brand">{{ item.brandName }}</div>
                  <div class="vnb-product-name" :title="item.ten">{{ item.ten }}</div>
                  <div class="vnb-product-price-row">
                    <span v-if="item.minPrice > 0" class="vnb-product-price">{{ formatMoney(item.minPrice) }}</span>
                    <span v-else class="vnb-product-price text-muted" style="font-size: 0.95rem;">Liên hệ</span>
                    <span v-if="item.originalPrice > item.minPrice && item.minPrice > 0" class="vnb-product-old-price">
                      {{ formatMoney(item.originalPrice) }}
                    </span>
                  </div>
                  <button class="vnb-product-btn-cart" type="button" @click.stop="goToDetail(item.id)">
                    <i class="bi bi-cart-plus me-1"></i> Xem chi tiết
                  </button>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Thanh Category Pills cuộn ngang */
.vnb-cat-pills-bar {
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 6px 8px;
  gap: 6px;
}

/* Nút mũi tên tròn trái / phải */
.vnb-cat-scroll-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #1e293b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.18s ease;
  font-size: 0.85rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.vnb-cat-scroll-arrow:hover {
  background: #1d4ed8;
  color: #ffffff;
  border-color: #1d4ed8;
  transform: scale(1.08);
}

.vnb-cat-pills-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE và Edge cũ */
  scroll-behavior: smooth;
  flex: 1;
  padding: 2px 0;
  cursor: grab;
}

.vnb-cat-pills-scroll:active {
  cursor: grabbing;
}

/* Ẩn thanh cuộn xám thô trên Chrome, Safari, Edge */
.vnb-cat-pills-scroll::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.vnb-cat-pill-btn {
  display: inline-flex;
  align-items: center;
  padding: 7px 15px;
  border-radius: 99px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  user-select: none;
  flex-shrink: 0;
}

.vnb-cat-pill-btn:hover {
  border-color: #1d4ed8;
  color: #1d4ed8;
  background: #eff6ff;
  transform: translateY(-1px);
}

.vnb-cat-pill-btn.active {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(29, 78, 216, 0.3);
}

.fw-black {
  font-weight: 900;
}
</style>
