<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sanPhamService, bienTheService } from '../../services/sanPhamService'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { formatMoney } from '../../utils/format'

const router = useRouter()

const loading = ref(true)
const products = ref([])
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }

// Tab được chọn trong phần "Sản phẩm mới"
const selectedCatTab = ref('ALL') // 'ALL' | 'VOT' | 'GIAY' | 'AO' | 'VAY' | 'QUAN'

// Chỉ số slide hiện tại cho carousel sản phẩm mới
const currentSlide = ref(0)
const itemsPerSlide = ref(5)

onMounted(async () => {
  try {
    loading.value = true
    const [spRes, btRes, dggRes] = await Promise.all([
      sanPhamService.getAll(),
      bienTheService.getAll(),
      dotGiamGiaService.getApDungHomNay().catch(() => ({})),
    ])

    products.value = spRes || []
    variants.value = btRes || []
    discounts.value = dggRes || {}
  } catch (err) {
    console.error('Lỗi khi tải trang chủ:', err)
  } finally {
    loading.value = false
  }
})

// Tính giá và khuyến mãi
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

      return {
        ...sp,
        minPrice: minPrice === Infinity ? 529000 : minPrice,
        originalPrice: originalPrice === Infinity ? 690000 : originalPrice,
        maxDiscountPercent,
        hasDiscount: maxDiscountPercent > 0,
      }
    })
})

// Lọc sản phẩm theo tab đang bấm
const tabFilteredProducts = computed(() => {
  if (selectedCatTab.value === 'ALL') return enrichedProducts.value
  if (selectedCatTab.value === 'VOT') {
    return enrichedProducts.value.filter(
      (p) => p.ten?.toLowerCase().includes('vợt') || !p.idDanhMuc || p.idDanhMuc === 1
    )
  }
  if (selectedCatTab.value === 'GIAY') {
    return enrichedProducts.value.filter((p) => p.ten?.toLowerCase().includes('giày'))
  }
  if (selectedCatTab.value === 'AO') {
    return enrichedProducts.value.filter((p) => p.ten?.toLowerCase().includes('áo'))
  }
  if (selectedCatTab.value === 'VAY') {
    return enrichedProducts.value.filter((p) => p.ten?.toLowerCase().includes('váy'))
  }
  if (selectedCatTab.value === 'QUAN') {
    return enrichedProducts.value.filter((p) => p.ten?.toLowerCase().includes('quần'))
  }
  return enrichedProducts.value
})

// Sản phẩm hiển thị trong khung carousel
const carouselDisplayList = computed(() => {
  const list = tabFilteredProducts.value
  if (list.length === 0) return []
  const start = currentSlide.value * itemsPerSlide.value
  return list.slice(start, start + itemsPerSlide.value)
})

function prevSlide() {
  if (currentSlide.value > 0) {
    currentSlide.value--
  } else {
    const maxPage = Math.ceil(tabFilteredProducts.value.length / itemsPerSlide.value) - 1
    currentSlide.value = Math.max(0, maxPage)
  }
}

function nextSlide() {
  const maxPage = Math.ceil(tabFilteredProducts.value.length / itemsPerSlide.value) - 1
  if (currentSlide.value < maxPage) {
    currentSlide.value++
  } else {
    currentSlide.value = 0
  }
}

function goToDetail(id) {
  router.push(`/shop/san-pham/${id}`)
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <div class="home-view">
    <!-- 1. HERO SLIDER BANNER (Theo ảnh 2) -->
    <section class="vnb-hero-banner mb-4">
      <div class="position-relative overflow-hidden" style="background: linear-gradient(135deg, #70b4d4 0%, #3182ce 100%); min-height: 400px;">
        <div class="vnb-container py-5">
          <div class="row align-items-center g-4">
            <div class="col-lg-6 text-white">
              <span class="badge bg-warning text-dark px-3 py-1 fw-bold rounded-pill mb-2">NEW ARRIVAL</span>
              <h1 class="display-4 fw-black text-uppercase fst-italic mb-2" style="font-weight: 900; letter-spacing: -1px;">
                NEW ECLIPSION
              </h1>
              <h5 class="fw-bold text-uppercase text-light mb-3">
                CONFIDENCE. FROM EVERY DIRECTION. TO EVERY DIRECTION.
              </h5>
              <p class="lead opacity-90 small mb-4">
                Công nghệ Power Cushion+ độc quyền hỗ trợ bật nhảy tối đa, giảm chấn thương hoàn hảo cho từng bước di chuyển trên sân cầu.
              </p>
              <RouterLink to="/shop/san-pham" class="btn btn-warning btn-lg fw-bold px-4 py-2 rounded-pill text-dark shadow">
                KHÁM PHÁ NGAY <i class="bi bi-arrow-right ms-1"></i>
              </RouterLink>
            </div>
            <div class="col-lg-6 text-center">
              <img
                src="/logo.png"
                alt="Banner Eclipsion"
                class="img-fluid rounded-4 shadow-lg p-3 bg-white"
                style="max-height: 320px; object-fit: contain;"
              />
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="vnb-container">
      <!-- 2. 4 KHỐI CAM KẾT DỊCH VỤ (Theo ảnh 3) -->
      <section class="mb-5">
        <div class="row g-3">
          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-truck"></i>
              </div>
              <div>
                <div class="vnb-service-title">Vận chuyển <span class="highlight">TOÀN QUỐC</span></div>
                <div class="vnb-service-desc">Thanh toán khi nhận hàng</div>
              </div>
            </div>
          </div>

          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-hand-thumbs-up"></i>
              </div>
              <div>
                <div class="vnb-service-title">Bảo đảm <span class="highlight">CHẤT LƯỢNG</span></div>
                <div class="vnb-service-desc">Sản phẩm bảo đảm chất lượng.</div>
              </div>
            </div>
          </div>

          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-credit-card"></i>
              </div>
              <div>
                <div class="vnb-service-title">Tiến hành <span class="highlight">THANH TOÁN</span></div>
                <div class="vnb-service-desc">Với nhiều PHƯƠNG THỨC</div>
              </div>
            </div>
          </div>

          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-arrow-repeat"></i>
              </div>
              <div>
                <div class="vnb-service-title">Đổi sản phẩm <span class="highlight">MỚI</span></div>
                <div class="vnb-service-desc">nếu sản phẩm lỗi</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 3. PHẦN SẢN PHẨM MỚI (Theo ảnh 3) -->
      <section class="mb-5">
        <!-- Tiêu đề có gạch cam dưới -->
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Sản phẩm mới</h2>
          <div class="vnb-title-underline"></div>
        </div>

        <!-- Các tab danh mục: Tất cả, Vợt Cầu Lông, Giày Cầu Lông... -->
        <div class="vnb-cat-tabs">
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'ALL' }"
            @click="selectedCatTab = 'ALL'; currentSlide = 0"
          >
            Tất cả
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'VOT' }"
            @click="selectedCatTab = 'VOT'; currentSlide = 0"
          >
            Vợt Cầu Lông
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'GIAY' }"
            @click="selectedCatTab = 'GIAY'; currentSlide = 0"
          >
            Giày Cầu Lông
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'AO' }"
            @click="selectedCatTab = 'AO'; currentSlide = 0"
          >
            Áo Cầu Lông
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'VAY' }"
            @click="selectedCatTab = 'VAY'; currentSlide = 0"
          >
            Váy cầu lông
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'QUAN' }"
            @click="selectedCatTab = 'QUAN'; currentSlide = 0"
          >
            Quần Cầu Lông
          </button>
        </div>

        <!-- Khung viền vàng/cam chứa sản phẩm và mũi tên điều hướng -->
        <div class="vnb-carousel-frame">
          <!-- Nút Prev -->
          <button
            type="button"
            class="vnb-arrow-btn vnb-arrow-prev"
            @click="prevSlide"
            aria-label="Xem sản phẩm trước"
          >
            <i class="bi bi-chevron-left"></i>
          </button>

          <!-- Nút Next -->
          <button
            type="button"
            class="vnb-arrow-btn vnb-arrow-next"
            @click="nextSlide"
            aria-label="Xem sản phẩm kế tiếp"
          >
            <i class="bi bi-chevron-right"></i>
          </button>

          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-warning" role="status"></div>
            <p class="small text-muted mt-2">Đang tải sản phẩm mới...</p>
          </div>

          <div v-else-if="carouselDisplayList.length === 0" class="text-center py-5 text-muted small">
            Không có sản phẩm nào trong danh mục này.
          </div>

          <!-- Lưới 5 sản phẩm/slide theo ảnh 3 -->
          <div v-else class="row g-0">
            <div
              v-for="item in carouselDisplayList"
              :key="item.id"
              class="col-xl-2 col-lg-3 col-md-4 col-6"
              style="flex: 1 0 20%; max-width: 20%;"
            >
              <div class="vnb-product-item" @click="goToDetail(item.id)">
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
      </section>

      <!-- 4. PHẦN SALE OFF (Theo ảnh 4) -->
      <section class="mb-5">
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Sale off</h2>
          <div class="vnb-title-underline"></div>
        </div>

        <!-- 3 Banner khuyến mãi hàng ngang -->
        <div class="row g-3">
          <div class="col-md-4">
            <div
              class="rounded-3 p-4 text-white text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); min-height: 140px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-warning text-dark fw-bold px-2 py-1 mb-1" style="font-size: 0.7rem;">SALE OFF</span>
              <h5 class="fw-black text-uppercase mb-1" style="font-weight: 900;">VỢT CẦU LÔNG</h5>
              <div class="small text-warning fw-bold">GIẢM 20% - 70%</div>
            </div>
          </div>

          <div class="col-md-4">
            <div
              class="rounded-3 p-4 text-white text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #18181b 0%, #dc2626 100%); min-height: 140px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-white text-danger fw-bold px-2 py-1 mb-1" style="font-size: 0.7rem;">DISCOUNT</span>
              <h5 class="fw-black text-uppercase mb-1" style="font-weight: 900;">GIẢM GIÁ GIÀY</h5>
              <div class="small text-white opacity-90 fw-bold">OFF LIMITED TIME</div>
            </div>
          </div>

          <div class="col-md-4">
            <div
              class="rounded-3 p-4 text-dark text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #fef3c7 0%, #fed7aa 100%); min-height: 140px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-danger text-white fw-bold px-2 py-1 mb-1" style="font-size: 0.7rem;">SALE OFF</span>
              <h5 class="fw-black text-uppercase mb-1 text-danger" style="font-weight: 900;">ÁO CẦU LÔNG</h5>
              <div class="small text-dark fw-bold">20% - 70% OFF</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 5. PHẦN SẢN PHẨM CẦU LÔNG (4 Card danh mục có dải nơ cam chéo - Theo ảnh 4) -->
      <section class="mb-5">
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Sản phẩm cầu lông</h2>
          <div class="vnb-title-underline"></div>
        </div>

        <div class="row g-4">
          <!-- Card 1: VỢT CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-crosshair display-1 text-white opacity-75"></i>
              </div>
              <div class="vnb-cat-ribbon">VỢT CẦU LÔNG</div>
            </div>
          </div>

          <!-- Card 2: GIÀY CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-lightning-charge display-1 text-white opacity-75"></i>
              </div>
              <div class="vnb-cat-ribbon">GIÀY CẦU LÔNG</div>
            </div>
          </div>

          <!-- Card 3: ÁO CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-tshirt display-1 text-white opacity-75"></i>
              </div>
              <div class="vnb-cat-ribbon">ÁO CẦU LÔNG</div>
            </div>
          </div>

          <!-- Card 4: VÁY CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-star display-1 text-white opacity-75"></i>
              </div>
              <div class="vnb-cat-ribbon">VÁY CẦU LÔNG</div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Nút Cuộn Lên Đầu Trang (Back to top màu cam vuông - Theo góc phải ảnh 3 & 4) -->
    <button
      type="button"
      class="vnb-back-to-top"
      @click="scrollToTop"
      aria-label="Cuộn lên đầu trang"
    >
      <i class="bi bi-chevron-up"></i>
    </button>
  </div>
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
</style>
