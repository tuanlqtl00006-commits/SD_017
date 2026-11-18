<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cuaHangService } from '../../services/cuaHangService'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'
import { useAutoRefresh } from '../../composables/useAutoRefresh'
import TheSanPham from '../../components/shop/TheSanPham.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const loading = ref(true)
const product = ref(null)
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }
const activeImage = ref('')
// Ảnh nhỏ dưới ảnh chính: dữ liệu có thể lặp một đường dẫn nhiều lần, chỉ hiện mỗi ảnh một lần
const anhNho = computed(() => [...new Set((product.value?.danhSachAnh || []).filter(Boolean))])

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
const toastType = ref('success') // 'success' | 'warning' | 'error'
const showToast = ref(false)
let toastTimer = null

function notify(msg, type = 'success') {
  toastMsg.value = msg
  toastType.value = type
  showToast.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
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

// Sản phẩm tương tự: cùng danh mục, cùng hãng xếp trước, tối đa 4; lỗi tải thì ẩn khối
const tatCaSanPham = ref([])
async function taiSanPhamTuongTu() {
  try {
    const ds = await cuaHangService.getDanhSachSanPham()
    tatCaSanPham.value = Array.isArray(ds) ? ds : []
  } catch {
    tatCaSanPham.value = []
  }
}
const sanPhamTuongTu = computed(() => {
  const p = product.value
  if (!p) return []
  return tatCaSanPham.value
    .filter((x) => x && x.id !== p.id && (!p.idDanhMuc || x.idDanhMuc === p.idDanhMuc))
    .sort((a, b) => Number(b.idThuongHieu === p.idThuongHieu) - Number(a.idThuongHieu === p.idThuongHieu))
    .slice(0, 4)
})

onMounted(() => {
  taiSanPhamTuongTu()
  loadProductDetail(Number(route.params.id))
})

// Tồn kho / giá / ảnh đổi ở quầy hoặc trang quản trị: cập nhật ngay, giữ nguyên màu - trọng lượng - chu vi - số lượng khách đang chọn
async function taiNgam() {
  const id = Number(route.params.id)
  if (!id || !product.value || loading.value) return
  const res = await cuaHangService.getChiTietSanPham(id)
  if (Number(route.params.id) !== id || !res?.product) return
  product.value = res.product
  variants.value = res.variants || []
  discounts.value = res.discounts || {}
  colors.value = res.colors || []
  weights.value = res.weights || []
  grips.value = res.grips || []
  const anh = [res.product.anhChinh, ...(res.product.danhSachAnh || [])]
  if (!anh.includes(activeImage.value)) activeImage.value = res.product.anhChinh || res.product.danhSachAnh?.[0] || ''
  dongBoLuaChon()
}

// Biến thể đang chọn không còn (vừa bị ngừng bán / xóa): chuyển lựa chọn sang một biến thể có thật (ưu tiên còn hàng),
// để các nút đang tô luôn khớp với giá, tồn kho và món sẽ thêm vào giỏ.
function dongBoLuaChon() {
  if (!variants.value.length) return
  const khop = variants.value.some(
    (v) => v.idMauSac === selectedColorId.value && v.idTrongLuong === selectedWeightId.value && v.idChuVi === selectedGripId.value,
  )
  if (khop) return
  const thay = variants.value.find((v) => (Number(v.soLuongTon) || 0) > 0) || variants.value[0]
  selectedColorId.value = thay.idMauSac
  selectedWeightId.value = thay.idTrongLuong
  selectedGripId.value = thay.idChuVi
}
useAutoRefresh(taiNgam, 3000, { chuKyMs: 30_000, chuDe: ['san-pham', 'ton-kho', 'giam-gia'] })

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
  if (!variants.value || variants.value.length === 0) return null
  const match = variants.value.find(
    (v) =>
      (!selectedColorId.value || v.idMauSac === selectedColorId.value) &&
      (!selectedWeightId.value || v.idTrongLuong === selectedWeightId.value) &&
      (!selectedGripId.value || v.idChuVi === selectedGripId.value)
  )
  return match || variants.value[0]
})

/* ----- Chọn biến thể (màu / trọng lượng / chu vi cán) -----
 * Bấm một nút thì giữ nguyên các lựa chọn còn lại nếu có biến thể khớp; không có thì chuyển sang biến thể gần nhất
 * (ưu tiên còn hàng). Nút của phiên bản hết hàng được làm mờ nhưng vẫn bấm xem được. */
const CAC_THUOC_TINH = ['idMauSac', 'idTrongLuong', 'idChuVi']

function luaChonHienTai() {
  return { idMauSac: selectedColorId.value, idTrongLuong: selectedWeightId.value, idChuVi: selectedGripId.value }
}

function chonThuocTinh(khoa, id) {
  const dang = luaChonHienTai()
  const ungVien = variants.value.filter((v) => v[khoa] === id)
  if (!ungVien.length) return
  const khac = CAC_THUOC_TINH.filter((k) => k !== khoa)
  const diem = (v) => khac.reduce((s, k) => s + (v[k] === dang[k] ? 1 : 0), 0)
  const conHang = (v) => ((Number(v.soLuongTon) || 0) > 0 ? 1 : 0)
  const tot = [...ungVien].sort((a, b) => diem(b) - diem(a) || conHang(b) - conHang(a))[0]
  selectedColorId.value = tot.idMauSac
  selectedWeightId.value = tot.idTrongLuong
  selectedGripId.value = tot.idChuVi
}

// Nút này hết hàng? (xét các biến thể cùng giá trị và khớp với các lựa chọn còn lại; không có biến thể khớp thì xét tất cả cùng giá trị)
function nutHetHang(khoa, id) {
  const dang = luaChonHienTai()
  const cungGiaTri = variants.value.filter((v) => v[khoa] === id)
  const khac = CAC_THUOC_TINH.filter((k) => k !== khoa)
  const phuHop = cungGiaTri.filter((v) => khac.every((k) => v[k] === dang[k]))
  const xet = phuHop.length ? phuHop : cungGiaTri
  return !xet.some((v) => (Number(v.soLuongTon) || 0) > 0)
}

// Đổi sang biến thể ít hàng hơn (hoặc tồn vừa giảm): số lượng đang chọn không được vượt tồn của biến thể hiện tại
watch(
  () => Number(currentVariant.value?.soLuongTon) || 0,
  (ton) => {
    if (quantity.value > Math.max(1, ton)) quantity.value = Math.max(1, ton)
  },
)

// Giá gốc và giá sau giảm của biến thể đang chọn
const currentPricing = computed(() => {
  if (!currentVariant.value) {
    const prices = variants.value.map((v) => Number(v.giaBan) || 0).filter((p) => p > 0)
    const minPrice = prices.length ? Math.min(...prices) : Number(product.value?.gia) || 0
    return { original: minPrice, sale: minPrice, discountPercent: 0 }
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

function onQuantityInput(event) {
  const maxStock = currentVariant.value?.soLuongTon ?? 99
  let val = parseInt(event.target.value)
  if (isNaN(val) || val < 1) val = 1
  if (val > maxStock) val = maxStock
  quantity.value = val
  event.target.value = val
}

function addToCart() {
  if (!currentVariant.value) {
    notify('Vui lòng chọn đầy đủ phiên bản sản phẩm.', 'warning')
    return false
  }
  if ((currentVariant.value.soLuongTon || 0) < 1) {
    notify('Phiên bản này hiện đã hết hàng!', 'error')
    return false
  }

  const colorObj = availableColors.value.find((c) => c.id === currentVariant.value.idMauSac)
  const weightObj = availableWeights.value.find((w) => w.id === currentVariant.value.idTrongLuong)
  const gripObj = availableGrips.value.find((g) => g.id === currentVariant.value.idChuVi)

  const result = cartStore.addItem({
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

  if (result.status === 'maxed') {
    notify(`Giỏ hàng đã đạt tối đa ${result.maxStock} sản phẩm cho phiên bản này!`, 'error')
    return false
  } else if (result.status === 'partial') {
    notify(`Chỉ thêm được ${result.actualAdded} sản phẩm (đã đạt tối đa ${result.maxStock} trong kho)`, 'warning')
  } else {
    notify(`Đã thêm "${product.value.ten}" vào giỏ hàng!`)
  }
  return true
}

function buyNow() {
  if (addToCart()) {
    router.push('/shop/thanh-toan')
  }
}
</script>

<template>
  <div class="product-detail-view">
    <div class="vnb-container">
      <!-- Toast Thông báo nhanh -->
      <Transition name="toast-slide">
        <div
          v-if="showToast"
          class="toast-notification"
          :class="`toast-${toastType}`"
        >
          <div class="toast-icon">
            <i v-if="toastType === 'success'" class="bi bi-check-circle-fill"></i>
            <i v-else-if="toastType === 'warning'" class="bi bi-exclamation-triangle-fill"></i>
            <i v-else class="bi bi-x-circle-fill"></i>
          </div>
          <div class="toast-content">
            <div class="toast-title">
              {{ toastType === 'success' ? 'Thành công' : toastType === 'warning' ? 'Lưu ý' : 'Thông báo' }}
            </div>
            <div class="toast-message">{{ toastMsg }}</div>
          </div>
          <button class="toast-close" @click="showToast = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
      </Transition>

      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="pd-duong-dan">
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

      <div v-else-if="!product" class="pd-trong">
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
        <div class="pd-chinh">
          <div class="pd-luoi">
            <!-- Cột Thư viện ảnh -->
            <div class="pd-cot-anh">
              <div class="product-gallery">
                <span
                  v-if="currentPricing.discountPercent > 0"
                  class="product-badge-sale"
                  style="font-size: 0.9rem;"
                >
                  -{{ currentPricing.discountPercent }}%
                </span>
                <img :src="activeImage || product.anhChinh || '/logo.png'" :alt="product.ten" class="pd-anh-chinh" />
              </div>

              <!-- Thumbnails gallery -->
              <div
                v-if="anhNho.length > 1"
                class="pd-anh-nho"
              >
                <div
                  v-for="(img, idx) in anhNho"
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
            <div class="pd-cot-thong-tin">
              <div class="product-info-wrap">
                <div class="pd-hang">{{ brandName }}</div>
                <h1 class="pd-ten">{{ product.ten }}</h1>
                <div class="pd-ma">Mã sản phẩm: <strong>{{ product.ma }}</strong></div>

                <!-- Khối Giá bán -->
                <div class="pd-khoi-gia">
                  <div v-if="currentPricing.sale > 0" class="pd-gia-dong">
                    <span class="pd-gia" :class="{ 'is-giam': currentPricing.original > currentPricing.sale }">{{ formatMoney(currentPricing.sale) }}</span>
                    <span
                      v-if="currentPricing.original > currentPricing.sale"
                      class="pd-gia-goc"
                    >
                      {{ formatMoney(currentPricing.original) }}
                    </span>
                    <span
                      v-if="currentPricing.discountPercent > 0"
                      class="pd-tiet-kiem"
                    >
                      Tiết kiệm {{ currentPricing.discountPercent }}%
                    </span>
                  </div>
                  <div v-else class="fs-4 fw-bold text-muted">
                    Liên hệ cửa hàng
                  </div>
                </div>

                <!-- Cảnh báo nếu chưa có biến thể thật nào -->
                <div v-if="variants.length === 0" class="alert alert-warning py-3 mb-4 rounded-3 small">
                  <i class="bi bi-exclamation-triangle-fill me-1"></i> Sản phẩm này hiện chưa có phiên bản / biến thể nào trong kho.
                </div>

                <!-- Chọn Màu Sắc -->
                <div class="pd-nhom-chon" v-if="availableColors.length > 0">
                  <label class="pd-nhan-chon">Màu sắc</label>
                  <div class="pd-cac-nut">
                    <button
                      type="button"
                      v-for="c in availableColors"
                      :key="c.id"
                      class="variant-pill"
                      data-testid="chon-mau"
                      :class="{ active: selectedColorId === c.id, 'het-hang': nutHetHang('idMauSac', c.id) }"
                      :title="nutHetHang('idMauSac', c.id) ? 'Hết hàng' : ''"
                      @click="chonThuocTinh('idMauSac', c.id)"
                    >
                      {{ c.ten }}
                    </button>
                  </div>
                </div>

                <!-- Chọn Trọng Lượng / Size -->
                <div class="pd-nhom-chon" v-if="availableWeights.length > 0">
                  <label class="pd-nhan-chon">{{ isFootwearOrApparel ? 'Kích thước / Size' : 'Trọng lượng' }}</label>
                  <div class="pd-cac-nut">
                    <button
                      type="button"
                      v-for="w in availableWeights"
                      :key="w.id"
                      class="variant-pill"
                      data-testid="chon-trong-luong"
                      :class="{ active: selectedWeightId === w.id, 'het-hang': nutHetHang('idTrongLuong', w.id) }"
                      :title="nutHetHang('idTrongLuong', w.id) ? 'Hết hàng' : ''"
                      @click="chonThuocTinh('idTrongLuong', w.id)"
                    >
                      {{ w.ten }}
                    </button>
                  </div>
                </div>

                <!-- Chọn Chu Vi Cán -->
                <div class="pd-nhom-chon" v-if="availableGrips.length > 0 && !isFootwearOrApparel">
                  <label class="pd-nhan-chon">Chu vi cán cầm</label>
                  <div class="pd-cac-nut">
                    <button
                      type="button"
                      v-for="g in availableGrips"
                      :key="g.id"
                      class="variant-pill"
                      data-testid="chon-chu-vi"
                      :class="{ active: selectedGripId === g.id, 'het-hang': nutHetHang('idChuVi', g.id) }"
                      :title="nutHetHang('idChuVi', g.id) ? 'Hết hàng' : ''"
                      @click="chonThuocTinh('idChuVi', g.id)"
                    >
                      {{ g.ten }}
                    </button>
                  </div>
                </div>

                <!-- Số lượng tồn kho & Chọn số lượng -->
                <div class="pd-so-luong-dong">
                  <div class="pd-so-luong">
                    <button
                      type="button"
                      class="pd-so-luong-nut"
                      aria-label="Giảm số lượng"
                      @click="changeQuantity(-1)"
                      :disabled="quantity <= 1"
                    >
                      <i class="bi bi-dash-lg"></i>
                    </button>
                    <input
                      type="number"
                      class="pd-so-luong-o no-spinner"
                      aria-label="Số lượng"
                      :value="quantity"
                      :min="1"
                      :max="currentVariant?.soLuongTon ?? 99"
                      @change="onQuantityInput"
                      @keydown.enter="onQuantityInput($event); $event.target.blur()"
                    />
                    <button
                      type="button"
                      class="pd-so-luong-nut"
                      aria-label="Tăng số lượng"
                      @click="changeQuantity(1)"
                      :disabled="quantity >= (currentVariant?.soLuongTon ?? 99)"
                    >
                      <i class="bi bi-plus-lg"></i>
                    </button>
                  </div>

                  <div class="small">
                    <template v-if="currentVariant">
                      <span v-if="(currentVariant.soLuongTon || 0) > 0" class="text-success fw-semibold">
                        <i class="bi bi-check-circle me-1"></i> Còn {{ currentVariant.soLuongTon }} sản phẩm trong kho
                      </span>
                      <span v-else class="text-danger fw-semibold">
                        <i class="bi bi-x-circle me-1"></i> Tạm hết hàng phiên bản này
                      </span>
                    </template>
                    <span v-else class="text-muted fw-semibold">
                      <i class="bi bi-info-circle me-1"></i> Vui lòng chọn đầy đủ phiên bản
                    </span>
                  </div>
                </div>

                <!-- Nút Hành động Mua hàng -->
                <div class="pd-nut-dong">
                  <button
                    type="button"
                    class="btn pd-nut-gio"
                    :disabled="variants.length === 0 || !currentVariant || (currentVariant?.soLuongTon || 0) < 1"
                    @click="addToCart"
                  >
                    <i class="bi bi-bag-plus"></i> Thêm vào giỏ
                  </button>

                  <button
                    type="button"
                    class="btn pd-nut-mua"
                    :disabled="variants.length === 0 || !currentVariant || (currentVariant?.soLuongTon || 0) < 1"
                    @click="buyNow"
                  >
                    Mua ngay
                  </button>
                </div>

                <!-- Cam kết: đúng chính sách ở trang Chính sách -->
                <ul class="pd-cam-ket">
                  <li><i class="bi bi-truck"></i><span><strong>Giao Hàng Nhanh</strong><RouterLink to="/shop/chinh-sach#giao-hang">Toàn quốc, xem bảng phí</RouterLink></span></li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- Thông số kỹ thuật & Mô tả chi tiết -->
        <div class="pd-duoi">
          <div class="pd-the">
              <h2 class="pd-the-tieu-de">Thông số kỹ thuật</h2>
              <table class="pd-bang">
                <tbody>
                  <tr>
                    <th>Thương hiệu</th>
                    <td class="fw-semibold">{{ brandName }}</td>
                  </tr>
                  <tr>
                    <th>Xuất xứ</th>
                    <td>{{ originName }}</td>
                  </tr>
                  <tr>
                    <th>{{ isFootwearOrApparel ? 'Chất liệu' : 'Chất liệu thân vợt' }}</th>
                    <td>{{ materialName }}</td>
                  </tr>
                  <tr>
                    <th>{{ isFootwearOrApparel ? 'Đặc tính' : 'Độ cứng thân' }}</th>
                    <td>{{ flexName }}</td>
                  </tr>
                  <tr>
                    <th>{{ isFootwearOrApparel ? 'Kiểu dáng / Đế' : 'Điểm cân bằng' }}</th>
                    <td>{{ balanceName }}</td>
                  </tr>
                  <tr>
                    <th>Mã sản phẩm</th>
                    <td>{{ product.ma }}</td>
                  </tr>
                </tbody>
              </table>
          </div>

          <div class="pd-the">
              <h2 class="pd-the-tieu-de">Mô tả sản phẩm</h2>
              <div class="pd-mo-ta">
                {{ product.moTa || 'Chưa có mô tả cho sản phẩm này.' }}
              </div>
          </div>
        </div>

        <!-- Sản phẩm tương tự -->
        <section v-if="sanPhamTuongTu.length" class="pd-tuong-tu" data-testid="pd-tuong-tu">
          <div class="pd-tuong-tu-dau">
            <h2>Sản phẩm tương tự</h2>
            <RouterLink to="/shop/san-pham">Xem tất cả <i class="bi bi-arrow-right"></i></RouterLink>
          </div>
          <div class="pd-tuong-tu-luoi">
            <TheSanPham v-for="sp in sanPhamTuongTu" :key="sp.id" :sp="sp" />
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== Toast Notification ===== */
.toast-notification {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 300px;
  max-width: 400px;
  padding: 14px 16px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(4px);
  border-left: 4px solid;
}

.toast-success {
  background: #ffffff;
  border-color: #22c55e;
  color: #15803d;
}
.toast-warning {
  background: #ffffff;
  border-color: #f59e0b;
  color: #b45309;
}
.toast-error {
  background: #ffffff;
  border-color: #ef4444;
  color: #b91c1c;
}

.toast-icon {
  font-size: 22px;
  flex-shrink: 0;
}
.toast-success .toast-icon { color: #22c55e; }
.toast-warning .toast-icon { color: #f59e0b; }
.toast-error   .toast-icon { color: #ef4444; }

.toast-content {
  flex: 1;
}
.toast-title {
  font-weight: 700;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 2px;
}
.toast-message {
  font-size: 14px;
  color: #374151;
  line-height: 1.4;
}

.toast-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  font-size: 12px;
  padding: 2px;
  flex-shrink: 0;
  line-height: 1;
  transition: color 0.2s;
}
.toast-close:hover { color: #374151; }

/* Animation */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(60px);
}
.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(60px);
}

/* Ẩn mũi tên spinner của input number */
.no-spinner::-webkit-outer-spin-button,
.no-spinner::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.no-spinner {
  -moz-appearance: textfield;
  appearance: textfield;
}

.product-detail-view {
  min-height: calc(100vh - 200px);
  padding: 18px 0 72px;
  background: var(--ft-bg, #f6f7f9);
}
.pd-duong-dan {
  margin-bottom: 18px;
}
.pd-trong {
  padding: 56px 20px;
  text-align: center;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.pd-chinh {
  margin-bottom: 24px;
  padding: 28px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 28px;
}
.pd-luoi {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  gap: 44px;
  align-items: start;
}
.pd-cot-anh {
  position: sticky;
  top: 120px;
}
.product-gallery {
  position: relative;
  display: grid;
  place-items: center;
  aspect-ratio: 1 / 1;
  padding: 36px;
  background: #f3f4f6;
  border-radius: 22px;
  overflow: hidden;
}
.pd-anh-chinh {
  width: 100%;
  height: 100%;
  object-fit: contain;
  mix-blend-mode: multiply;
  transition: transform 0.4s ease;
}
.product-gallery:hover .pd-anh-chinh {
  transform: scale(1.05);
}
.product-badge-sale {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 1;
  padding: 5px 12px;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  background: #e11d48;
  border-radius: 999px;
}
.pd-anh-nho {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}
.thumbnail-box {
  width: 76px;
  height: 76px;
  padding: 6px;
  cursor: pointer;
  background: #f3f4f6;
  border: 2px solid transparent;
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.2s ease;
}
.thumbnail-box img {
  mix-blend-mode: multiply;
}
.thumbnail-box:hover {
  border-color: #cbd5e1;
}
.thumbnail-box.active {
  border-color: var(--ft-ink, #0f172a);
}
.pd-hang {
  margin-bottom: 8px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ft-primary, #1d4ed8);
}
.pd-ten {
  margin: 0 0 8px;
  font-size: clamp(1.5rem, 2.4vw, 2rem);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ft-ink, #0f172a);
}
.pd-ma {
  font-size: 0.88rem;
  color: #64748b;
}
.pd-ma strong {
  color: var(--ft-ink-2, #334155);
}
.pd-khoi-gia {
  margin: 20px 0 24px;
  padding: 20px 0;
  border-top: 1px solid var(--ft-line-2, #eef0f4);
  border-bottom: 1px solid var(--ft-line-2, #eef0f4);
}
.pd-gia-dong {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
}
.pd-gia {
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ft-ink, #0f172a);
}
.pd-gia.is-giam {
  color: #e11d48;
}
.pd-gia-goc {
  font-size: 1.1rem;
  color: #94a3b8;
  text-decoration: line-through;
}
.pd-tiet-kiem {
  padding: 4px 10px;
  font-size: 0.8rem;
  font-weight: 700;
  color: #be123c;
  background: #ffe4e6;
  border-radius: 999px;
}
.pd-nhom-chon {
  margin-bottom: 20px;
}
.pd-nhan-chon {
  display: block;
  margin-bottom: 10px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
}
.pd-cac-nut {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.variant-pill {
  min-width: 64px;
  padding: 10px 18px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ft-ink-2, #334155);
  cursor: pointer;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  transition: border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}
.variant-pill:hover:not(:disabled) {
  border-color: var(--ft-ink, #0f172a);
}
.variant-pill.active {
  color: #fff;
  background: var(--ft-ink, #0f172a);
  border-color: var(--ft-ink, #0f172a);
}
/* Phiên bản hết hàng: nét đứt, làm mờ nhưng vẫn bấm xem được (giá, tồn kho của phiên bản đó) */
.variant-pill.het-hang {
  color: #94a3b8;
  text-decoration: line-through;
  background: #f8fafc;
  border: 1.5px dashed #cbd5e1;
}
.variant-pill.het-hang.active {
  color: #64748b;
  background: #f1f5f9;
  border: 1.5px dashed var(--ft-ink, #0f172a);
}
.pd-so-luong-dong {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  margin: 26px 0 22px;
  font-size: 0.9rem;
}
.pd-so-luong {
  display: inline-flex;
  align-items: center;
  padding: 4px;
  background: #f1f5f9;
  border-radius: 999px;
}
.pd-so-luong-nut {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  color: var(--ft-ink, #0f172a);
  background: #fff;
  border: 0;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}
.pd-so-luong-nut:disabled {
  color: #cbd5e1;
  box-shadow: none;
}
.pd-so-luong-o {
  width: 56px;
  font-size: 1rem;
  font-weight: 700;
  text-align: center;
  color: var(--ft-ink, #0f172a);
  background: transparent;
  border: 0;
  outline: none;
}
.pd-nut-dong {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.pd-nut-gio,
.pd-nut-mua {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 54px;
  font-size: 1rem;
  font-weight: 700;
  border-radius: 999px;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.15s ease;
}
.pd-nut-gio {
  color: var(--ft-ink, #0f172a);
  background: #fff;
  border: 1.5px solid var(--ft-ink, #0f172a);
}
.pd-nut-gio:hover:not(:disabled) {
  color: #fff;
  background: var(--ft-ink, #0f172a);
}
.pd-nut-mua {
  color: #fff;
  background: var(--ft-primary, #1d4ed8);
  border: 1.5px solid var(--ft-primary, #1d4ed8);
  box-shadow: 0 10px 24px rgba(29, 78, 216, 0.25);
}
.pd-nut-mua:hover:not(:disabled) {
  color: #fff;
  background: #1e40af;
  border-color: #1e40af;
}
.pd-nut-gio:active:not(:disabled),
.pd-nut-mua:active:not(:disabled) {
  transform: scale(0.98);
}
.pd-nut-gio:disabled,
.pd-nut-mua:disabled {
  opacity: 0.45;
  box-shadow: none;
}
.pd-cam-ket {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
}
.pd-cam-ket li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  font-size: 0.8rem;
  background: #f8fafc;
  border-radius: 14px;
}
.pd-cam-ket i {
  font-size: 1.15rem;
  color: var(--ft-primary, #1d4ed8);
}
.pd-cam-ket span {
  display: flex;
  flex-direction: column;
  min-width: 0;
  color: #64748b;
}
.pd-cam-ket strong {
  font-size: 0.85rem;
  color: var(--ft-ink, #0f172a);
}
.pd-cam-ket a {
  color: #64748b;
  text-decoration: none;
}
.pd-cam-ket a:hover {
  color: var(--ft-primary, #1d4ed8);
  text-decoration: underline;
}
.pd-duoi {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}
.pd-the {
  padding: 26px 28px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.pd-the-tieu-de {
  margin: 0 0 16px;
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--ft-ink, #0f172a);
}
.pd-bang {
  width: 100%;
  font-size: 0.92rem;
  border-collapse: collapse;
}
.pd-bang th,
.pd-bang td {
  padding: 12px 0;
  border-bottom: 1px solid var(--ft-line-2, #eef0f4);
}
.pd-bang tr:last-child th,
.pd-bang tr:last-child td {
  border-bottom: 0;
}
.pd-bang th {
  width: 42%;
  font-weight: 500;
  color: #64748b;
}
.pd-bang td {
  font-weight: 600;
  color: var(--ft-ink, #0f172a);
}
.pd-mo-ta {
  line-height: 1.75;
  white-space: pre-line;
  color: var(--ft-ink-2, #334155);
}
.pd-tuong-tu {
  margin-top: 48px;
}
.pd-tuong-tu-dau {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
}
.pd-tuong-tu-dau h2 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ft-ink, #0f172a);
}
.pd-tuong-tu-dau a {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ft-ink, #0f172a);
  text-decoration: none;
}
.pd-tuong-tu-luoi {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
@media (max-width: 991px) {
  .pd-luoi {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
  }
  .pd-cot-anh {
    position: static;
  }
  .pd-duoi {
    grid-template-columns: minmax(0, 1fr);
  }
  .pd-tuong-tu-luoi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 575px) {
  .pd-chinh {
    padding: 14px;
    border-radius: 20px;
  }
  .product-gallery {
    padding: 20px;
  }
  .pd-gia {
    font-size: 1.6rem;
  }
  .pd-cam-ket {
    grid-template-columns: minmax(0, 1fr);
  }
  .pd-the {
    padding: 20px;
  }
  .pd-tuong-tu-luoi {
    gap: 10px;
  }
}
</style>
