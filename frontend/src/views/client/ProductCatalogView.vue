<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sanPhamService, bienTheService } from '../../services/sanPhamService'
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

// Bộ lọc theo giao diện mẫu
const selectedPrices = ref([]) // array of price range keys
const selectedBrands = ref([]) // array of brand ids
const branchSearch = ref('')
const selectedBranches = ref([])

const branches = [
  'VNB Super Center',
  'VNB Quận 10 - TP.HCM',
  'VNB Cầu Giấy - Hà Nội',
  'VNB Bắc Từ Liêm - Hà Nội',
  'VNB Hải Châu - Đà Nẵng',
  'VNB Ninh Kiều - Cần Thơ',
]

const sortBy = ref('default')
const searchKeyword = ref('')

onMounted(async () => {
  try {
    loading.value = true
    const [spRes, btRes, dggRes, brandRes] = await Promise.all([
      sanPhamService.getAll(),
      bienTheService.getAll(),
      dotGiamGiaService.getApDungHomNay().catch(() => ({})),
      thuocTinhService.getAll('thuong-hieu').catch(() => []),
    ])

    products.value = spRes || []
    variants.value = btRes || []
    discounts.value = dggRes || {}
    brands.value = brandRes || []

    syncFromUrl()
  } catch (err) {
    console.error('Lỗi khi tải trang catalog:', err)
  } finally {
    loading.value = false
  }
})

function syncFromUrl() {
  if (route.query.search) {
    searchKeyword.value = String(route.query.search)
  }
  if (route.query.thuongHieu) {
    selectedBrands.value = [String(route.query.thuongHieu)]
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
      const spVariants = variants.value.filter(
        (v) => v.idSanPham === sp.id && v.hoatDong !== 0
      )

      let minPrice = Infinity
      let originalPrice = Infinity
      let maxDiscountPercent = 0

      for (const v of spVariants) {
        const giaGoc = Number(v.giaBan) || 0
        const ptGiam = Number(discounts.value[v.id]) || 0
        const giaSauGiam = ptGiam > 0 ? giaGoc * (1 - ptGiam / 100) : giaGoc

        if (giaSauGiam < minPrice) {
          minPrice = giaSauGiam
          originalPrice = giaGoc
        }
        if (ptGiam > maxDiscountPercent) {
          maxDiscountPercent = ptGiam
        }
      }

      const brand = brands.value.find((b) => b.id === sp.idThuongHieu)

      return {
        ...sp,
        brandName: brand?.ten || 'VNB',
        minPrice: minPrice === Infinity ? 529000 : minPrice,
        originalPrice: originalPrice === Infinity ? 690000 : originalPrice,
        maxDiscountPercent,
        hasDiscount: maxDiscountPercent > 0,
      }
    })
})

const filteredBranches = computed(() => {
  if (!branchSearch.value.trim()) return branches
  return branches.filter((b) =>
    b.toLowerCase().includes(branchSearch.value.trim().toLowerCase())
  )
})

const filteredProducts = computed(() => {
  let list = enrichedProducts.value

  // Tìm kiếm
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim()
    list = list.filter(
      (p) =>
        p.ten?.toLowerCase().includes(kw) ||
        p.ma?.toLowerCase().includes(kw) ||
        p.brandName?.toLowerCase().includes(kw)
    )
  }

  // Lọc thương hiệu
  if (selectedBrands.value.length > 0) {
    list = list.filter((p) => selectedBrands.value.includes(String(p.idThuongHieu)))
  }

  // Lọc khoảng giá
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

  // Sắp xếp
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
      <!-- Breadcrumb (Theo ảnh 1) -->
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb small mb-0">
          <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none text-muted">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active text-dark fw-bold" aria-current="page">Vợt Cầu Lông</li>
        </ol>
      </nav>

      <div class="row g-4">
        <!-- CỘT BỘ LỌC BÊN TRÁI (Theo đúng ảnh 1) -->
        <div class="col-lg-3 col-md-4">
          <div class="border p-3 rounded-2 bg-white">
            <!-- 1. CHỌN MỨC GIÁ -->
            <div class="mb-4">
              <div class="vnb-filter-title">CHỌN MỨC GIÁ</div>
              <ul class="vnb-filter-list">
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

            <!-- 2. THƯƠNG HIỆU -->
            <div class="mb-4">
              <div class="vnb-filter-title">THƯƠNG HIỆU</div>
              <ul class="vnb-filter-list">
                <li v-for="b in brands" :key="b.id" class="vnb-filter-item">
                  <label>
                    <input type="checkbox" :value="String(b.id)" v-model="selectedBrands" />
                    <span>{{ b.ten }}</span>
                  </label>
                </li>
                <li v-if="brands.length === 0" class="vnb-filter-item text-muted small">
                  Đang tải thương hiệu...
                </li>
              </ul>
            </div>

            <!-- 3. CHI NHÁNH -->
            <div class="mb-2">
              <div class="vnb-filter-title">CHI NHÁNH</div>
              <div class="input-group input-group-sm mb-2">
                <input
                  type="text"
                  class="form-control"
                  placeholder="Tìm chi nhánh"
                  v-model="branchSearch"
                />
                <button class="btn btn-outline-secondary" type="button">
                  <i class="bi bi-search text-warning"></i>
                </button>
              </div>
              <ul class="vnb-filter-list" style="max-height: 160px;">
                <li v-for="br in filteredBranches" :key="br" class="vnb-filter-item">
                  <label>
                    <input type="checkbox" :value="br" v-model="selectedBranches" />
                    <span>{{ br }}</span>
                  </label>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- CỘT NỘI DUNG SẢN PHẨM BÊN PHẢI (Theo ảnh 1) -->
        <div class="col-lg-9 col-md-8">
          <!-- Banner Vợt Cầu Lông TARO (Theo ảnh 1) -->
          <div
            class="rounded-3 p-4 mb-4 text-white position-relative overflow-hidden shadow-sm"
            style="background: linear-gradient(90deg, #18181b 0%, #b91c1c 45%, #18181b 100%); min-height: 200px; display: flex; align-items: center;"
          >
            <div class="row align-items-center w-100">
              <div class="col-md-7">
                <span class="badge bg-dark text-white mb-2 px-2 py-1 border border-secondary">R SERIES</span>
                <h2 class="display-6 fw-black text-uppercase mb-1" style="font-weight: 900; color: #ffffff; text-shadow: 2px 2px 4px rgba(0,0,0,0.6);">
                  VỢT CẦU LÔNG <span class="text-info">TARO</span>
                </h2>
                <div class="badge bg-primary-subtle text-primary fw-bold px-3 py-2 rounded-pill mb-2">
                  PHÙ HỢP CHO NGƯỜI MỚI CHƠI
                </div>
                <div class="fs-5 fw-bold text-warning">
                  Giá chỉ từ <span class="fs-4 text-white">399K</span>
                </div>
              </div>
              <div class="col-md-5 text-center">
                <img
                  src="/logo.png"
                  alt="Vợt TARO"
                  class="img-fluid p-2 bg-white rounded-3"
                  style="max-height: 140px; object-fit: contain;"
                />
              </div>
            </div>
          </div>

          <!-- Thanh tiêu đề danh mục & Sắp xếp -->
          <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
            <h4 class="fw-bold mb-0 text-uppercase" style="font-size: 1.15rem; color: #1e293b;">
              VỢT CẦU LÔNG
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
            <div class="spinner-border text-warning" role="status"></div>
            <p class="small text-muted mt-2">Đang tải danh sách sản phẩm...</p>
          </div>

          <div v-else-if="filteredProducts.length === 0" class="card border-0 p-5 text-center text-muted">
            <i class="bi bi-inbox display-4 mb-2"></i>
            <p>Không tìm thấy sản phẩm nào phù hợp với bộ lọc.</p>
          </div>

          <!-- Lưới sản phẩm (Cards theo ảnh 1) -->
          <div v-else class="row g-3">
            <div
              v-for="item in filteredProducts"
              :key="item.id"
              class="col-xl-3 col-lg-4 col-sm-6"
            >
              <div class="vnb-product-item border rounded-2 p-2" @click="goToDetail(item.id)">
                <div class="vnb-img-box">
                  <span v-if="item.hasDiscount" class="vnb-badge-discount">-{{ item.maxDiscountPercent }}%</span>
                  <span class="vnb-badge-premium">Premium</span>
                  <img :src="item.anhChinh || '/logo.png'" :alt="item.ten" class="vnb-product-img" />
                </div>
                <div class="vnb-product-name" :title="item.ten">{{ item.ten }}</div>
                <div>
                  <span class="vnb-product-price">{{ formatMoney(item.minPrice) }}</span>
                  <span v-if="item.originalPrice > item.minPrice" class="vnb-product-old-price">
                    {{ formatMoney(item.originalPrice) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
