<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sanPhamService, bienTheService } from '../../services/sanPhamService'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { thuocTinhService } from '../../services/thuocTinhService'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const loading = ref(true)
const product = ref(null)
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }

// Thuộc tính để hiển thị tên thay vì id
const colors = ref([])
const weights = ref([])
const grips = ref([])
const brands = ref([])
const origins = ref([])
const materials = ref([])
const flexibilities = ref([])
const balancePoints = ref([])

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

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    router.push('/shop/san-pham')
    return
  }

  try {
    loading.value = true
    const [sp, allVariants, dggRes, msRes, tlRes, cvRes, thRes, xxRes, clRes, dcRes, cbRes] =
      await Promise.all([
        sanPhamService.getById(id),
        bienTheService.getAll(),
        dotGiamGiaService.getApDungHomNay().catch(() => ({})),
        thuocTinhService.getAll('mau-sac').catch(() => []),
        thuocTinhService.getAll('trong-luong').catch(() => []),
        thuocTinhService.getAll('chu-vi').catch(() => []),
        thuocTinhService.getAll('thuong-hieu').catch(() => []),
        thuocTinhService.getAll('xuat-xu').catch(() => []),
        thuocTinhService.getAll('chat-lieu').catch(() => []),
        thuocTinhService.getAll('do-cung').catch(() => []),
        thuocTinhService.getAll('diem-can-bang').catch(() => []),
      ])

    product.value = sp
    variants.value = (allVariants || []).filter((v) => v.idSanPham === id && v.hoatDong !== 0)
    discounts.value = dggRes || {}

    colors.value = msRes || []
    weights.value = tlRes || []
    grips.value = cvRes || []
    brands.value = thRes || []
    origins.value = xxRes || []
    materials.value = clRes || []
    flexibilities.value = dcRes || []
    balancePoints.value = cbRes || []

    // Mặc định chọn biến thể đầu tiên còn hàng hoặc biến thể đầu tiên
    if (variants.value.length > 0) {
      const firstAvailable = variants.value.find((v) => (v.soLuongTon || 0) > 0) || variants.value[0]
      selectedColorId.value = firstAvailable.idMauSac
      selectedWeightId.value = firstAvailable.idTrongLuong
      selectedGripId.value = firstAvailable.idChuVi
    }
  } catch (err) {
    console.error('Lỗi khi tải chi tiết sản phẩm:', err)
  } finally {
    loading.value = false
  }
})

// Các màu sắc khả dụng của sản phẩm này
const availableColors = computed(() => {
  const ids = [...new Set(variants.value.map((v) => v.idMauSac))]
  return colors.value.filter((c) => ids.includes(c.id))
})

// Các trọng lượng khả dụng
const availableWeights = computed(() => {
  const ids = [...new Set(variants.value.map((v) => v.idTrongLuong))]
  return weights.value.filter((w) => ids.includes(w.id))
})

// Các chu vi cán khả dụng
const availableGrips = computed(() => {
  const ids = [...new Set(variants.value.map((v) => v.idChuVi))]
  return grips.value.filter((g) => ids.includes(g.id))
})

// Biến thể khớp với bộ chọn hiện tại
const currentVariant = computed(() => {
  if (!variants.value.length) return null
  return variants.value.find(
    (v) =>
      (!selectedColorId.value || v.idMauSac === selectedColorId.value) &&
      (!selectedWeightId.value || v.idTrongLuong === selectedWeightId.value) &&
      (!selectedGripId.value || v.idChuVi === selectedGripId.value)
  )
})

// Giá gốc và giá sau giảm của biến thể đang chọn
const currentPricing = computed(() => {
  if (!currentVariant.value) {
    return { original: 0, sale: 0, discountPercent: 0 }
  }
  const original = Number(currentVariant.value.giaBan) || 0
  const ptGiam = Number(discounts.value[currentVariant.value.id]) || 0
  const sale = ptGiam > 0 ? original * (1 - ptGiam / 100) : original
  return {
    original,
    sale,
    discountPercent: ptGiam,
  }
})

// Tên thuộc tính để hiển thị
const brandName = computed(() => brands.value.find((b) => b.id === product.value?.idThuongHieu)?.ten || '—')
const originName = computed(() => origins.value.find((o) => o.id === product.value?.idXuatXu)?.ten || '—')
const materialName = computed(() => materials.value.find((m) => m.id === product.value?.idChatLieu)?.ten || '—')
const flexName = computed(() => flexibilities.value.find((f) => f.id === product.value?.idDoCung)?.ten || '—')
const balanceName = computed(() => balancePoints.value.find((b) => b.id === product.value?.idDiemCanBang)?.ten || '—')

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

  const colorObj = colors.value.find((c) => c.id === currentVariant.value.idMauSac)
  const weightObj = weights.value.find((w) => w.id === currentVariant.value.idTrongLuong)
  const gripObj = grips.value.find((g) => g.id === currentVariant.value.idChuVi)

  cartStore.addItem({
    sanPhamId: product.value.id,
    sanPhamTen: product.value.ten,
    sanPhamMa: product.value.ma,
    bienTheId: currentVariant.value.id,
    bienTheMa: currentVariant.value.ma,
    mauSac: colorObj?.ten || '',
    trongLuong: weightObj?.ten || '',
    chuVi: gripObj?.ten || '',
    giaGoc: currentPricing.value.original,
    giaBan: currentPricing.value.sale,
    anh: product.value.anhChinh || '/logo.png',
    soLuong: quantity.value,
    soLuongTon: currentVariant.value.soLuongTon,
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
        <div class="toast show align-items-center text-white bg-dark border-0 shadow-lg px-2 py-1">
          <div class="d-flex">
            <div class="toast-body d-flex align-items-center gap-2">
              <i class="bi bi-check-circle-fill text-success fs-5"></i>
              <span>{{ toastMsg }}</span>
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="showToast = false"></button>
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

      <div v-else-if="!product" class="text-center py-5">
        <h5>Không tìm thấy sản phẩm này</h5>
        <RouterLink to="/shop/san-pham" class="btn btn-primary mt-3">Quay lại danh sách</RouterLink>
      </div>

      <div v-else>
        <!-- Thông tin sản phẩm chính -->
        <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
          <div class="row g-4">
            <!-- Cột Thư viện ảnh -->
            <div class="col-lg-5 col-md-6">
              <div class="product-gallery text-center p-3 bg-light rounded-4 position-relative">
                <span
                  v-if="currentPricing.discountPercent > 0"
                  class="product-badge-sale"
                  style="font-size: 0.9rem;"
                >
                  -{{ currentPricing.discountPercent }}%
                </span>
                <img
                  :src="product.anhChinh || '/logo.png'"
                  :alt="product.ten"
                  class="img-fluid"
                  style="max-height: 380px; object-fit: contain;"
                />
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
                  <div class="d-flex align-items-baseline gap-3">
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
                  <div class="small text-success mt-1">
                    <i class="bi bi-shield-check me-1"></i> Giá đã bao gồm thuế VAT & Bảo hành chính hãng
                  </div>
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

                <!-- Chọn Trọng Lượng -->
                <div class="mb-3" v-if="availableWeights.length > 0">
                  <label class="form-label fw-bold small text-muted text-uppercase mb-2">
                    Trọng lượng:
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
                <div class="mb-4" v-if="availableGrips.length > 0">
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
                    :disabled="(currentVariant?.soLuongTon || 0) < 1"
                    @click="addToCart"
                  >
                    <i class="bi bi-cart-plus fs-5"></i> Thêm vào giỏ
                  </button>

                  <button
                    type="button"
                    class="btn btn-lg px-5 fw-bold rounded-pill shadow-sm text-white"
                    style="background-color: #ea580c;"
                    :disabled="(currentVariant?.soLuongTon || 0) < 1"
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
                    <th class="text-muted">Chất liệu thân vợt</th>
                    <td>{{ materialName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">Độ cứng thân</th>
                    <td>{{ flexName }}</td>
                  </tr>
                  <tr>
                    <th class="text-muted">Điểm cân bằng</th>
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
                {{ product.moTa || 'Dòng sản phẩm vợt cầu lông chính hãng chất lượng cao phân phối độc quyền tại FootStyle. Thiết kế hiện đại, trợ lực tốt phù hợp cho cả người mới chơi lẫn vận động viên chuyên nghiệp.' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
