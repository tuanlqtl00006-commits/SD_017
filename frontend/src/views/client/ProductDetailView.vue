<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cuaHangService } from '../../services/cuaHangService'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const loading = ref(true)
const product = ref(null)
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }
const activeImage = ref('')

// Thuộc tính để hiển thị
const colors = ref([])
const weights = ref([])
const grips = ref([])

// Lựa chọn hiện tại của khách
const selectedColorId = ref(null)
const selectedWeightId = ref(null)
const selectedGripId = ref(null)
const quantity = ref(1)

// Thông báo toast nhanh
const toastMsg = ref('')
const showToast = ref(false)

function notify(msg) {
  toastMsg.value = msg
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 2500)
}

async function loadProductDetail(id) {
  if (!id) {
    router.push('/shop/san-pham')
    return
  }

  try {
    loading.value = true
    const res = await cuaHangService.getChiTietSanPham(id)
    if (res && res.product) {
      product.value = res.product
      activeImage.value = res.product.anhChinh || (res.product.danhSachAnh && res.product.danhSachAnh[0]) || ''
      variants.value = res.variants || []
      discounts.value = res.discounts || {}
      colors.value = res.colors || []
      weights.value = res.weights || []
      grips.value = res.grips || []

      // Mặc định chọn biến thể đầu tiên còn hàng hoặc biến thể đầu tiên
      if (variants.value.length > 0) {
        const firstAvailable = variants.value.find((v) => (v.soLuongTon || 0) > 0) || variants.value[0]
        selectedColorId.value = firstAvailable.idMauSac
        selectedWeightId.value = firstAvailable.idTrongLuong
        selectedGripId.value = firstAvailable.idChuVi
      }
    } else {
      product.value = null
    }
  } catch (err) {
    console.error('Lỗi khi tải chi tiết sản phẩm:', err)
    product.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProductDetail(Number(route.params.id))
})

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadProductDetail(Number(newId))
    }
  }
)

// Các màu sắc khả dụng của sản phẩm này (chỉ lấy từ biến thể thật)
const availableColors = computed(() => {
  const ids = [...new Set(variants.value.map((v) => v.idMauSac).filter(Boolean))]
  if (colors.value.length > 0) {
    return colors.value.filter((c) => ids.includes(c.id))
  }
  return ids.map((id) => {
    const v = variants.value.find((item) => item.idMauSac === id)
    return { id, ten: v?.tenMau || `Màu ${id}` }
  })
})

// Các trọng lượng / kích thước khả dụng (chỉ lấy từ biến thể thật)
const availableWeights = computed(() => {
  const ids = [...new Set(variants.value.map((v) => v.idTrongLuong).filter(Boolean))]
  if (weights.value.length > 0) {
    return weights.value.filter((w) => ids.includes(w.id))
  }
  return ids.map((id) => {
    const v = variants.value.find((item) => item.idTrongLuong === id)
    return { id, ten: v?.trongLuong || `Size ${id}` }
  })
})

// Các chu vi cán khả dụng (chỉ lấy từ biến thể thật)
const availableGrips = computed(() => {
  const ids = [...new Set(variants.value.map((v) => v.idChuVi).filter(Boolean))]
  if (grips.value.length > 0) {
    return grips.value.filter((g) => ids.includes(g.id))
  }
  return ids.map((id) => {
    const v = variants.value.find((item) => item.idChuVi === id)
    return { id, ten: v?.chuVi || `Cán ${id}` }
  })
})

// Biến thể khớp với bộ chọn hiện tại
const currentVariant = computed(() => {
  if (!variants.value.length) return null
  const match = variants.value.find(
    (v) =>
      (!selectedColorId.value || v.idMauSac === selectedColorId.value) &&
      (!selectedWeightId.value || v.idTrongLuong === selectedWeightId.value) &&
      (!selectedGripId.value || v.idChuVi === selectedGripId.value)
  )
  return match || variants.value[0]
})

// Giá gốc và giá sau giảm của biến thể đang chọn
const currentPricing = computed(() => {
  if (!currentVariant.value) {
    return { original: 0, sale: 0, discountPercent: 0 }
  }
  const original = Number(currentVariant.value.giaBan) || 0
  const ptGiam = Number(discounts.value[currentVariant.value.id]) || Number(currentVariant.value.phanTramGiam) || 0
  const sale = currentVariant.value.giaSauGiam || (ptGiam > 0 ? original * (1 - ptGiam / 100) : original)
  return {
    original,
    sale,
    discountPercent: ptGiam,
  }
})

// Kiểm tra loại sản phẩm là Giày hoặc Quần áo
const isFootwearOrApparel = computed(() => {
  const name = product.value?.ten?.toLowerCase() || ''
  const cat = product.value?.categoryName?.toLowerCase() || ''
  return (
    name.includes('giày') ||
    name.includes('áo') ||
    name.includes('váy') ||
    name.includes('quần') ||
    cat.includes('giay') ||
    cat.includes('ao') ||
    cat.includes('vay') ||
    cat.includes('quan')
  )
})

// Tên thuộc tính để hiển thị
const brandName = computed(() => product.value?.brandName || 'Chính hãng')
const originName = computed(() => product.value?.originName || '—')
const materialName = computed(() => product.value?.materialName || '—')
const flexName = computed(() => product.value?.flexName || '—')
const balanceName = computed(() => product.value?.balanceName || '—')

function changeQuantity(delta) {
  const maxStock = currentVariant.value?.soLuongTon ?? 99
  const newQty = quantity.value + delta
  if (newQty >= 1 && newQty <= maxStock) {
    quantity.value = newQty
  }
}

function addToCart() {
  if (!currentVariant.value) {
    notify('Vui lòng chọn đầy đủ phiên bản sản phẩm.')
    return
  }
  if ((currentVariant.value.soLuongTon || 0) < 1) {
    notify('Phiên bản này hiện đã hết hàng!')
    return
  }

  const colorObj = availableColors.value.find((c) => c.id === currentVariant.value.idMauSac)
  const weightObj = availableWeights.value.find((w) => w.id === currentVariant.value.idTrongLuong)
  const gripObj = availableGrips.value.find((g) => g.id === currentVariant.value.idChuVi)

  cartStore.addItem({
    sanPhamId: product.value.id,
    sanPhamTen: product.value.ten,
    sanPhamMa: product.value.ma || `SP${product.value.id}`,
    bienTheId: currentVariant.value.id,
    bienTheMa: currentVariant.value.ma || currentVariant.value.maSpct || `${product.value.ma}-BT${currentVariant.value.id}`,
    mauSac: currentVariant.value.tenMau || colorObj?.ten || '',
    trongLuong: currentVariant.value.trongLuong || weightObj?.ten || '',
    chuVi: currentVariant.value.chuVi || gripObj?.ten || '',
    giaGoc: currentPricing.value.original,
    giaBan: currentPricing.value.sale,
    anh: activeImage.value || product.value.anhChinh || '/logo.png',
    soLuong: quantity.value,
    soLuongTon: currentVariant.value.soLuongTon || 99,
  })

  notify(`Đã thêm "${product.value.ten}" vào giỏ hàng!`)
}

function buyNow() {
  addToCart()
  router.push('/shop/thanh-toan')
}
</script>

<template>
  <div class="product-detail-view py-4">
    <div class="container">
      <!-- Toast Thông báo nhanh -->
      <div
        v-if="showToast"
        class="position-fixed bottom-0 end-0 p-3"
        style="z-index: 1080;"
      >
        <div class="toast show align-items-center text-white bg-dark border-0 shadow-lg px-3 py-2 rounded-3">
          <div class="d-flex align-items-center justify-content-between">
            <div class="toast-body d-flex align-items-center gap-2 p-0">
              <i class="bi bi-check-circle-fill text-success fs-5"></i>
              <span>{{ toastMsg }}</span>
            </div>
            <button type="button" class="btn-close btn-close-white ms-3 m-auto" @click="showToast = false"></button>
          </div>
        </div>
      </div>

      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-4">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item"><RouterLink to="/shop/san-pham">Sản phẩm</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">{{ product?.ten || 'Chi tiết sản phẩm' }}</li>
        </ol>
      </nav>

      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="text-muted mt-2">Đang tải thông tin sản phẩm...</p>
      </div>

      <div v-else-if="!product" class="text-center py-5 card border-0 shadow-sm rounded-4 p-5">
        <div class="mb-3">
          <i class="bi bi-exclamation-circle text-warning display-4"></i>
        </div>
        <h4 class="fw-bold text-dark">Không tìm thấy sản phẩm này</h4>
        <p class="text-muted">Sản phẩm có thể đã ngừng kinh doanh hoặc đường dẫn không đúng.</p>
        <div>
          <RouterLink to="/shop/san-pham" class="btn btn-primary px-4 py-2 rounded-pill">Quay lại danh sách sản phẩm</RouterLink>
        </div>
      </div>

      <div v-else>
        <!-- Thông tin sản phẩm chính -->
        <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
          <div class="row g-4">
            <!-- Cột Thư viện ảnh -->
            <div class="col-lg-5 col-md-6">
              <div class="product-gallery text-center p-4 bg-light rounded-4 position-relative d-flex align-items-center justify-content-center" style="min-height: 380px;">
                <span
                  v-if="currentPricing.discountPercent > 0"
                  class="product-badge-sale"
                  style="font-size: 0.9rem;"
                >
                  -{{ currentPricing.discountPercent }}%
                </span>
                <img
                  :src="activeImage || product.anhChinh || '/logo.png'"
                  :alt="product.ten"
                  class="img-fluid"
                  style="max-height: 350px; max-width: 100%; object-fit: contain; transition: transform 0.3s ease;"
                />
              </div>

              <!-- Thumbnails gallery -->
              <div
                v-if="product.danhSachAnh && product.danhSachAnh.length > 1"
                class="d-flex flex-wrap justify-content-center gap-2 mt-3"
              >
                <div
                  v-for="(img, idx) in product.danhSachAnh"
                  :key="idx"
                  class="thumbnail-box"
                  :class="{ active: activeImage === img }"
                  @click="activeImage = img"
                >
                  <img :src="img" class="w-100 h-100 object-fit-contain" />
                </div>
              </div>
            </div>

            <!-- Cột Chi tiết & Chọn biến thể -->
            <div class="col-lg-7 col-md-6">
              <div class="product-info-wrap">
                <span class="badge bg-primary-subtle text-primary fw-bold text-uppercase px-2 py-1 mb-2">
                  {{ brandName }}
                </span>
                <h1 class="h3 fw-bold text-dark mb-2">{{ product.ten }}</h1>
                <div class="small text-muted mb-3">Mã sản phẩm: <strong class="text-dark">{{ product.ma }}</strong></div>

                <!-- Khối Giá bán -->
                <div class="p-3 rounded-3 mb-4" style="background-color: #f8fafc; border: 1px dashed #cbd5e1;">
                  <div v-if="currentPricing.sale > 0" class="d-flex align-items-baseline gap-3">
                    <span class="fs-2 fw-bold text-danger">{{ formatMoney(currentPricing.sale) }}</span>
                    <span
                      v-if="currentPricing.original > currentPricing.sale"
                      class="text-decoration-line-through text-muted fs-5"
                    >
                      {{ formatMoney(currentPricing.original) }}
                    </span>
                    <span
                      v-if="currentPricing.discountPercent > 0"
                      class="badge bg-danger text-white rounded-pill px-2 py-1"
                    >
                      Tiết kiệm {{ currentPricing.discountPercent }}%
                    </span>
                  </div>
                  <div v-else class="fs-4 fw-bold text-muted">
                    Liên hệ cửa hàng
                  </div>
                  <div class="small text-success mt-1">
                    <i class="bi bi-shield-check me-1"></i> Giá đã bao gồm thuế VAT & Bảo hành chính hãng
                  </div>
                </div>

                <!-- Cảnh báo nếu chưa có biến thể thật nào -->
                <div v-if="variants.length === 0" class="alert alert-warning py-3 mb-4 rounded-3 small">
                  <i class="bi bi-exclamation-triangle-fill me-1"></i> Sản phẩm này hiện chưa có phiên bản / biến thể nào trong kho.
                </div>

                <!-- Chọn Màu Sắc -->
                <div class="mb-3" v-if="availableColors.length > 0">
                  <label class="form-label fw-bold small text-muted text-uppercase mb-2">
                    Màu sắc:
                  </label>
                  <div class="d-flex flex-wrap gap-2">
                    <button
                      type="button"
                      v-for="c in availableColors"
                      :key="c.id"
                      class="variant-pill"
                      :class="{ active: selectedColorId === c.id }"
                      @click="selectedColorId = c.id"
                    >
                      {{ c.ten }}
                    </button>
                  </div>
                </div>

                <!-- Chọn Trọng Lượng / Size -->
                <div class="mb-3" v-if="availableWeights.length > 0">
                  <label class="form-label fw-bold small text-muted text-uppercase mb-2">
                    {{ isFootwearOrApparel ? 'Kích thước / Size:' : 'Trọng lượng:' }}
                  </label>
                  <div class="d-flex flex-wrap gap-2">
                    <button
                      type="button"
                      v-for="w in availableWeights"
                      :key="w.id"
                      class="variant-pill"
                      :class="{ active: selectedWeightId === w.id }"
                      @click="selectedWeightId = w.id"
                    >
                      {{ w.ten }}
                    </button>
                  </div>
                </div>

                <!-- Chọn Chu Vi Cán -->
                <div class="mb-4" v-if="availableGrips.length > 0 && !isFootwearOrApparel">
                  <label class="form-label fw-bold small text-muted text-uppercase mb-2">
                    Chu vi cán cầm:
                  </label>
                  <div class="d-flex flex-wrap gap-2">
                    <button
                      type="button"
                      v-for="g in availableGrips"
                      :key="g.id"
                      class="variant-pill"
                      :class="{ active: selectedGripId === g.id }"
                      @click="selectedGripId = g.id"
                    >
                      {{ g.ten }}
                    </button>
                  </div>
                </div>

                <!-- Số lượng tồn kho & Chọn số lượng -->
                <div class="d-flex align-items-center gap-4 mb-4">
                  <div class="d-flex align-items-center">
                    <button
                      type="button"
                      class="btn btn-outline-secondary btn-sm px-3"
                      @click="changeQuantity(-1)"
                      :disabled="quantity <= 1"
                    >
                      -
                    </button>
                    <span class="px-3 fw-bold fs-6">{{ quantity }}</span>
                    <button
                      type="button"
                      class="btn btn-outline-secondary btn-sm px-3"
                      @click="changeQuantity(1)"
                      :disabled="quantity >= (currentVariant?.soLuongTon ?? 99)"
                    >
                      +
                    </button>
                  </div>

                  <div class="small">
                    <span v-if="(currentVariant?.soLuongTon || 0) > 0" class="text-success fw-semibold">
                      <i class="bi bi-check-circle me-1"></i> Còn {{ currentVariant.soLuongTon }} sản phẩm trong kho
                    </span>
                    <span v-else class="text-danger fw-semibold">
                      <i class="bi bi-x-circle me-1"></i> Tạm hết hàng phiên bản này
                    </span>
                  </div>
                </div>

                <!-- Nút Hành động Mua hàng -->
                <div class="d-flex flex-wrap gap-3">
                  <button
                    type="button"
                    class="btn btn-outline-danger btn-lg px-4 fw-semibold rounded-pill d-flex align-items-center gap-2"
                    :disabled="variants.length === 0 || !currentVariant || (currentVariant?.soLuongTon || 0) < 1"
                    @click="addToCart"
                  >
                    <i class="bi bi-cart-plus fs-5"></i> Thêm vào giỏ
                  </button>

                  <button
                    type="button"
                    class="btn btn-lg px-5 fw-bold rounded-pill shadow-sm text-white"
                    style="background-color: #ea580c;"
                    :disabled="variants.length === 0 || !currentVariant || (currentVariant?.soLuongTon || 0) < 1"
                    @click="buyNow"
                  >
                    Mua ngay
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Thông số kỹ thuật & Mô tả chi tiết -->
        <div class="row g-4">
          <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 p-4 h-100">
              <h5 class="fw-bold mb-3 d-flex align-items-center gap-2 text-dark">
                <i class="bi bi-sliders text-primary"></i> Thông số kỹ thuật
              </h5>
              <table class="table table-striped table-borderless small mb-0">
                <tbody>
                  <tr>
                    <th class="text-muted w-40">Thương hiệu</th>
                    <td class="fw-semibold">{{ brandName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">Xuất xứ</th>
                    <td>{{ originName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">{{ isFootwearOrApparel ? 'Chất liệu' : 'Chất liệu thân vợt' }}</th>
                    <td>{{ materialName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">{{ isFootwearOrApparel ? 'Đặc tính' : 'Độ cứng thân' }}</th>
                    <td>{{ flexName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">{{ isFootwearOrApparel ? 'Kiểu dáng / Đế' : 'Điểm cân bằng' }}</th>
                    <td>{{ balanceName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">Mã sản phẩm</th>
                    <td>{{ product.ma }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 p-4 h-100">
              <h5 class="fw-bold mb-3 d-flex align-items-center gap-2 text-dark">
                <i class="bi bi-info-circle text-primary"></i> Mô tả chi tiết
              </h5>
              <div class="text-muted" style="line-height: 1.7; white-space: pre-line;">
                {{ product.moTa || 'Dòng sản phẩm thể thao chính hãng chất lượng cao phân phối độc quyền tại FootStyle. Thiết kế hiện đại, trợ lực tốt phù hợp cho cả người mới chơi lẫn vận động viên chuyên nghiệp.' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-detail-view {
  background-color: #f8fafc;
  min-height: calc(100vh - 200px);
}
.product-gallery {
  background: #ffffff;
  border: 1px solid #f1f5f9;
}
.product-badge-sale {
  position: absolute;
  top: 15px;
  left: 15px;
  background-color: #ef4444;
  color: #fff;
  font-weight: 700;
  border-radius: 999px;
  padding: 4px 10px;
  box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.3);
}
.variant-pill {
  padding: 8px 16px;
  font-size: 0.9rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #fff;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}
.variant-pill:hover {
  border-color: #ea580c;
  color: #ea580c;
}
.variant-pill.active {
  border-color: #ea580c;
  background-color: #fff7ed;
  color: #c2410c;
  font-weight: 700;
  box-shadow: 0 0 0 2px rgba(234, 88, 12, 0.2);
}
.thumbnail-box {
  width: 68px;
  height: 68px;
  cursor: pointer;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.2s ease;
  padding: 4px;
  background: #fff;
}
.thumbnail-box:hover,
.thumbnail-box.active {
  border-color: #ea580c;
  box-shadow: 0 2px 8px rgba(234, 88, 12, 0.2);
}
</style>
