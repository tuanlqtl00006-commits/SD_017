<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCart } from '../../composables/useCart'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const route = useRoute()
const cart = useCart()
const toast = useToast()

const danhSachSanPham = ref([])
const danhMucList = ref([])
const thuongHieuList = ref([])
const flashSaleList = ref([])
const dangTai = ref(false)

// Bộ lọc
const idDanhMucChon = ref(route.query.dm ? Number(route.query.dm) : null)
const idThuongHieuChon = ref(null)
const mucGiaChon = ref('tat-ca')
const sapXepChon = ref('moi-nhat')
const tuKhoa = ref(route.query.q || '')

// Modal chi tiết sản phẩm
const moChiTiet = ref(false)
const spChon = ref(null)
const btChon = ref(null)
const mauChonId = ref(null)
const trongLuongChon = ref('')
const chuViChon = ref('')
const soLuongMua = ref(1)

// Countdown Flash sale
const countdown = ref({ h: 3, m: 45, s: 20 })
setInterval(() => {
  if (countdown.value.s > 0) {
    countdown.value.s--
  } else if (countdown.value.m > 0) {
    countdown.value.m--
    countdown.value.s = 59
  } else if (countdown.value.h > 0) {
    countdown.value.h--
    countdown.value.m = 59
    countdown.value.s = 59
  }
}, 1000)

async function taiDanhMucVaThuongHieu() {
  try {
    const res = await fetch('/api/cua-hang/danh-muc-va-thuong-hieu')
    if (res.ok) {
      const data = await res.json()
      danhMucList.value = data.danhMuc || []
      thuongHieuList.value = data.thuongHieu || []
    }
  } catch (e) {
    // ignore
  }
}

async function taiSanPham() {
  dangTai.value = true
  try {
    let url = '/api/cua-hang/san-pham?'
    if (idDanhMucChon.value) url += `idDanhMuc=${idDanhMucChon.value}&`
    if (idThuongHieuChon.value) url += `idThuongHieu=${idThuongHieuChon.value}&`
    if (tuKhoa.value) url += `tuKhoa=${encodeURIComponent(tuKhoa.value)}&`

    if (mucGiaChon.value === 'duoi-1tr') url += 'giaDen=1000000&'
    else if (mucGiaChon.value === '1tr-2tr') url += 'giaTu=1000000&giaDen=2000000&'
    else if (mucGiaChon.value === '2tr-3tr') url += 'giaTu=2000000&giaDen=3000000&'
    else if (mucGiaChon.value === 'tren-3tr') url += 'giaTu=3000000&'

    if (sapXepChon.value) url += `sapXep=${sapXepChon.value}&`

    const res = await fetch(url)
    if (res.ok) {
      danhSachSanPham.value = await res.json()
    }
  } catch (e) {
    console.error(e)
  } finally {
    dangTai.value = false
  }
}

async function taiFlashSale() {
  try {
    const res = await fetch('/api/cua-hang/khuyen-mai')
    if (res.ok) {
      flashSaleList.value = await res.json()
    }
  } catch (e) {
    // ignore
  }
}

watch(() => route.query, (q) => {
  if (q.dm) idDanhMucChon.value = Number(q.dm)
  else idDanhMucChon.value = null
  if (q.q !== undefined) tuKhoa.value = q.q
  if (q.sale) sapXepChon.value = 'giam-nhieu'
  taiSanPham()
})

onMounted(() => {
  taiDanhMucVaThuongHieu()
  taiSanPham()
  taiFlashSale()
})

function formatTien(v) {
  if (!v) return '0 ₫'
  return Number(v).toLocaleString('vi-VN') + ' ₫'
}

function moXemChiTiet(sp) {
  spChon.value = sp
  soLuongMua.value = 1
  if (sp.bienThe && sp.bienThe.length > 0) {
    // Lấy biến thể đầu tiên
    btChon.value = sp.bienThe[0]
    mauChonId.value = sp.bienThe[0].idMauSac
    trongLuongChon.value = sp.bienThe[0].trongLuong
    chuViChon.value = sp.bienThe[0].chuVi
  }
  moChiTiet.value = true
}

function chonMau(idMau) {
  mauChonId.value = idMau
  timBienThePhuHop()
}

function chonTrongLuong(tl) {
  trongLuongChon.value = tl
  timBienThePhuHop()
}

function timBienThePhuHop() {
  if (!spChon.value || !spChon.value.bienThe) return
  const match = spChon.value.bienThe.find(
    (b) =>
      (!mauChonId.value || b.idMauSac === mauChonId.value) &&
      (!trongLuongChon.value || b.trongLuong === trongLuongChon.value),
  )
  if (match) {
    btChon.value = match
  } else {
    // Fallback sang màu đó
    const theoMau = spChon.value.bienThe.find((b) => b.idMauSac === mauChonId.value)
    if (theoMau) btChon.value = theoMau
  }
}

// Danh sách các màu duy nhất của sp đang xem
const dsMauCuaSp = computed(() => {
  if (!spChon.value || !spChon.value.bienThe) return []
  const map = new Map()
  for (const b of spChon.value.bienThe) {
    if (b.idMauSac && !map.has(b.idMauSac)) {
      map.set(b.idMauSac, { id: b.idMauSac, ten: b.tenMau })
    }
  }
  return Array.from(map.values())
})

// Danh sách các trọng lượng duy nhất của sp đang xem
const dsTrongLuongCuaSp = computed(() => {
  if (!spChon.value || !spChon.value.bienThe) return []
  const set = new Set()
  for (const b of spChon.value.bienThe) {
    if (b.trongLuong) set.add(b.trongLuong)
  }
  return Array.from(set)
})

function themGioHangNhanh(sp, e) {
  if (e) e.stopPropagation()
  if (!sp.bienThe || sp.bienThe.length === 0) return
  const bt = sp.bienThe[0]
  cart.them(sp, bt, 1)
  toast.success(`Đã thêm "${sp.tenSanPham}" vào giỏ hàng!`)
}

function themVaoGioTuModal() {
  if (!spChon.value || !btChon.value) return
  cart.them(spChon.value, btChon.value, soLuongMua.value)
  toast.success(`Đã thêm ${soLuongMua.value} "${spChon.value.tenSanPham}" vào giỏ hàng!`)
  moChiTiet.value = false
}

function muaNgay() {
  if (!spChon.value || !btChon.value) return
  cart.them(spChon.value, btChon.value, soLuongMua.value)
  moChiTiet.value = false
  router.push('/thanh-toan')
}
</script>

<template>
  <div class="container">
    <!-- Hero Banner Carousel Thể Thao Đỉnh Cao -->
    <div class="vnb-hero-banner mb-4 rounded-4 overflow-hidden shadow-sm">
      <div class="row g-0 align-items-center vnb-hero-gradient p-4 p-md-5 text-white">
        <div class="col-lg-7">
          <div class="badge bg-primary text-white px-3 py-1 fw-bold mb-3 text-uppercase">
            🔥 SIÊU SALE CẦU LÔNG FOOTSTYLE · GIẢM ĐẾN 20%
          </div>
          <h1 class="display-5 fw-black text-white lh-1 mb-3">
            ĐỈNH CAO TỐC ĐỘ<br><span class="vnb-text-highlight">CHÍNH XÁC TỪNG CÚ ĐẬP</span>
          </h1>
          <p class="lead text-white-50 mb-4" style="max-width: 520px;">
            Hệ thống phân phối vợt cầu lông Yonex, Victor, Lining chính hãng. Căng cước điện tử chuẩn thi đấu quốc tế, giao hàng hỏa tốc trong 2h.
          </p>
          <div class="d-flex flex-wrap gap-3">
            <button class="btn vnb-btn-hero btn-lg px-4 fw-bold" @click="chonDanhMuc('vot', 1)">
              🏸 Mua Vợt Ngay
            </button>
            <button class="btn btn-outline-light btn-lg px-4" @click="sapXepChon = 'giam-nhieu'; taiSanPham()">
              <i class="bi bi-tag-fill me-1" style="color: #38bdf8;"></i> Săn Flash Sale
            </button>
          </div>
        </div>
        <div class="col-lg-5 text-center mt-4 mt-lg-0">
          <div class="vnb-hero-racket-wrap position-relative">
            <img
              src="/images/san-pham/sp01_astrox99_do.svg"
              alt="Yonex Astrox 99 Pro"
              class="img-fluid vnb-floating-racket"
              style="max-height: 290px; filter: drop-shadow(0 15px 25px rgba(0,0,0,0.6));"
            />
            <div class="vnb-hero-badge-float">
              <div class="fw-black text-info fs-4">-20%</div>
              <small class="fw-bold">YONEX ASTROX</small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Thanh 4 Cam Kết Vàng FootStyle -->
    <div class="row g-3 mb-4 text-center">
      <div class="col-6 col-lg-3">
        <div class="card h-100 border-0 shadow-sm rounded-3 p-3 vnb-trust-card">
          <i class="bi bi-patch-check-fill fs-2 text-primary mb-2"></i>
          <h6 class="fw-bold mb-1">100% CHÍNH HÃNG</h6>
          <small class="text-muted">Đền 200% nếu phát hiện hàng giả, hàng nhái</small>
        </div>
      </div>
      <div class="col-6 col-lg-3">
        <div class="card h-100 border-0 shadow-sm rounded-3 p-3 vnb-trust-card">
          <i class="bi bi-lightning-charge-fill fs-2 text-primary mb-2"></i>
          <h6 class="fw-bold mb-1">CĂNG CƯỚC CHUẨN</h6>
          <small class="text-muted">Máy điện tử Yonex chính xác từng lbs</small>
        </div>
      </div>
      <div class="col-6 col-lg-3">
        <div class="card h-100 border-0 shadow-sm rounded-3 p-3 vnb-trust-card">
          <i class="bi bi-truck fs-2 text-primary mb-2"></i>
          <h6 class="fw-bold mb-1">GIAO HÀNG HỎA TỐC</h6>
          <small class="text-muted">Nội thành 2h, toàn quốc 2-3 ngày nhận hàng</small>
        </div>
      </div>
      <div class="col-6 col-lg-3">
        <div class="card h-100 border-0 shadow-sm rounded-3 p-3 vnb-trust-card">
          <i class="bi bi-arrow-repeat fs-2 text-primary mb-2"></i>
          <h6 class="fw-bold mb-1">ĐỔI TRẢ 7 NGÀY</h6>
          <small class="text-muted">Đổi mới miễn phí khi có lỗi kỹ thuật từ NSX</small>
        </div>
      </div>
    </div>

    <!-- Khối Flash Sale Giảm Giá Sốc (Nếu có) -->
    <div v-if="flashSaleList.length > 0" class="vnb-flash-section p-4 rounded-4 mb-4 shadow-sm text-white">
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
        <div class="d-flex align-items-center gap-2">
          <span class="vnb-flash-icon"><i class="bi bi-lightning-fill"></i></span>
          <h4 class="fw-black mb-0 text-white">FLASH SALE SIÊU SỐC</h4>
          <span class="badge bg-danger ms-2">HOT DEALS</span>
        </div>
        <!-- Countdown -->
        <div class="d-flex align-items-center gap-1 small mt-2 mt-sm-0">
          <span class="text-white-50 me-1">Kết thúc trong:</span>
          <span class="vnb-timer-box">{{ String(countdown.h).padStart(2, '0') }}</span>:
          <span class="vnb-timer-box">{{ String(countdown.m).padStart(2, '0') }}</span>:
          <span class="vnb-timer-box">{{ String(countdown.s).padStart(2, '0') }}</span>
        </div>
      </div>

      <div class="row g-3">
        <div v-for="sp in flashSaleList.slice(0, 4)" :key="'fs-' + sp.id" class="col-6 col-lg-3">
          <div class="card h-100 border-0 rounded-3 text-dark p-2 vnb-fs-card" @click="moXemChiTiet(sp)">
            <div class="position-relative text-center p-2">
              <span class="position-absolute top-0 start-0 badge bg-danger fw-bold">-{{ sp.phanTramGiamMax }}%</span>
              <img :src="sp.anhChinh" :alt="sp.tenSanPham" class="img-fluid" style="height: 140px; object-fit: contain;">
            </div>
            <div class="card-body p-2 d-flex flex-column justify-content-between">
              <div>
                <small class="text-muted text-uppercase fw-bold">{{ sp.tenThuongHieu }}</small>
                <div class="fw-bold text-truncate" :title="sp.tenSanPham">{{ sp.tenSanPham }}</div>
              </div>
              <div class="mt-2">
                <div class="fw-bold text-danger fs-6">{{ formatTien(sp.giaBanMin) }}</div>
                <div class="small text-muted text-decoration-line-through">{{ formatTien(sp.giaGocMin) }}</div>
                <div class="progress mt-2" style="height: 6px;">
                  <div class="progress-bar bg-danger" role="progressbar" style="width: 75%;"></div>
                </div>
                <small class="text-danger fw-semibold" style="font-size: 0.7rem;">Đang bán chạy 🔥</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Thanh Bộ Lọc & Tìm Kiếm Đa Năng -->
    <div class="card border-0 shadow-sm rounded-4 p-3 mb-4 bg-white">
      <div class="row g-3 align-items-center">
        <!-- Danh mục -->
        <div class="col-md-3">
          <label class="form-label small fw-bold text-muted mb-1">Danh mục sản phẩm</label>
          <select v-model="idDanhMucChon" class="form-select form-select-sm" @change="taiSanPham">
            <option :value="null">-- Tất cả danh mục --</option>
            <option v-for="dm in danhMucList" :key="dm.id" :value="dm.id">{{ dm.ten }}</option>
          </select>
        </div>

        <!-- Thương hiệu -->
        <div class="col-md-3">
          <label class="form-label small fw-bold text-muted mb-1">Thương hiệu</label>
          <select v-model="idThuongHieuChon" class="form-select form-select-sm" @change="taiSanPham">
            <option :value="null">-- Tất cả thương hiệu --</option>
            <option v-for="th in thuongHieuList" :key="th.id" :value="th.id">{{ th.ten }}</option>
          </select>
        </div>

        <!-- Mức giá -->
        <div class="col-md-3">
          <label class="form-label small fw-bold text-muted mb-1">Mức giá</label>
          <select v-model="mucGiaChon" class="form-select form-select-sm" @change="taiSanPham">
            <option value="tat-ca">Tất cả mức giá</option>
            <option value="duoi-1tr">Dưới 1.000.000 ₫</option>
            <option value="1tr-2tr">Từ 1.000.000 - 2.000.000 ₫</option>
            <option value="2tr-3tr">Từ 2.000.000 - 3.000.000 ₫</option>
            <option value="tren-3tr">Trên 3.000.000 ₫</option>
          </select>
        </div>

        <!-- Sắp xếp -->
        <div class="col-md-3">
          <label class="form-label small fw-bold text-muted mb-1">Sắp xếp theo</label>
          <select v-model="sapXepChon" class="form-select form-select-sm" @change="taiSanPham">
            <option value="moi-nhat">Mới nhất</option>
            <option value="gia-tang">Giá tăng dần</option>
            <option value="gia-giam">Giá giảm dần</option>
            <option value="giam-nhieu">Khuyến mãi nhiều nhất</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Danh Sách Sản Phẩm Grid (Chuẩn ShopVNB Card) -->
    <div class="mb-5">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="fw-bold mb-0 text-dark d-flex align-items-center gap-2">
          <span class="vnb-title-bar"></span>
          DANH SÁCH SẢN PHẨM CẦU LÔNG
          <small class="text-muted fs-6 fw-normal">({{ danhSachSanPham.length }} sản phẩm)</small>
        </h4>
      </div>

      <!-- Loading State -->
      <div v-if="dangTai" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="text-muted mt-2">Đang tải danh sách sản phẩm FootStyle...</p>
      </div>

      <!-- Empty State -->
      <div v-else-if="danhSachSanPham.length === 0" class="card border-0 shadow-sm rounded-4 p-5 text-center">
        <i class="bi bi-inbox fs-1 text-muted mb-3"></i>
        <h5>Không tìm thấy sản phẩm phù hợp</h5>
        <p class="text-muted">Vui lòng thử tìm kiếm với từ khóa khác hoặc điều chỉnh lại bộ lọc.</p>
        <div>
          <button class="btn btn-outline-secondary btn-sm" @click="idDanhMucChon = null; idThuongHieuChon = null; mucGiaChon = 'tat-ca'; tuKhoa = ''; taiSanPham()">
            Xóa bộ lọc
          </button>
        </div>
      </div>

      <!-- Product Grid -->
      <div v-else class="row g-3 g-md-4">
        <div v-for="sp in danhSachSanPham" :key="sp.id" class="col-6 col-md-4 col-lg-3">
          <div class="card h-100 border-0 rounded-4 shadow-sm vnb-product-card" @click="moXemChiTiet(sp)">
            <!-- Card Image + Badges -->
            <div class="position-relative text-center p-3 vnb-card-img-wrap">
              <!-- Discount Badge -->
              <span v-if="sp.phanTramGiamMax > 0" class="position-absolute top-0 start-0 m-2 badge bg-danger fw-bold shadow-sm">
                -{{ sp.phanTramGiamMax }}%
              </span>
              <!-- Authentic Badge -->
              <span class="position-absolute top-0 end-0 m-2 badge bg-dark text-white fw-semibold small">
                Chính hãng
              </span>
              <!-- Image -->
              <img
                :src="sp.anhChinh"
                :alt="sp.tenSanPham"
                class="img-fluid vnb-card-img"
                style="height: 180px; object-fit: contain;"
              />
            </div>

            <!-- Card Body -->
            <div class="card-body p-3 d-flex flex-column justify-content-between">
              <div>
                <div class="d-flex justify-content-between align-items-center mb-1">
                  <span class="badge bg-light text-muted border text-uppercase" style="font-size: 0.68rem;">{{ sp.tenThuongHieu }}</span>
                  <span class="badge bg-light text-muted border" style="font-size: 0.68rem;">{{ sp.tenDanhMuc }}</span>
                </div>
                <h6 class="fw-bold text-dark text-truncate-2 mb-2 vnb-card-title" :title="sp.tenSanPham">
                  {{ sp.tenSanPham }}
                </h6>

                <!-- Thông số vợt nhanh (nếu là vợt) -->
                <div v-if="sp.diemCanBang || sp.doCung" class="small text-muted mb-2 lh-sm" style="font-size: 0.75rem;">
                  <span v-if="sp.diemCanBang"><i class="bi bi-circle-fill text-primary me-1" style="font-size: 6px;"></i>{{ sp.diemCanBang }}</span>
                  <span v-if="sp.doCung" class="ms-2"><i class="bi bi-circle-fill text-primary me-1" style="font-size: 6px;"></i>{{ sp.doCung }}</span>
                </div>
              </div>

              <!-- Price & Quick Add Button -->
              <div class="pt-2 border-top d-flex justify-content-between align-items-end mt-2">
                <div>
                  <div class="fw-black text-danger fs-5 lh-1">
                    {{ formatTien(sp.giaBanMin) }}
                  </div>
                  <div v-if="sp.giaGocMin > sp.giaBanMin" class="small text-muted text-decoration-line-through mt-1">
                    {{ formatTien(sp.giaGocMin) }}
                  </div>
                </div>
                <button
                  type="button"
                  class="btn vnb-btn-add-cart rounded-circle shadow-sm"
                  title="Thêm nhanh vào giỏ"
                  @click="themGioHangNhanh(sp, $event)"
                >
                  <i class="bi bi-cart-plus"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Chi Tiết Sản Phẩm & Đặt Mua Nhanh -->
    <div v-if="moChiTiet && spChon" class="modal d-block" tabindex="-1" style="background: rgba(0,0,0,0.6); z-index: 1055;">
      <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content rounded-4 border-0 shadow-lg overflow-hidden">
          <div class="modal-header border-bottom py-3 px-4 bg-light">
            <h5 class="modal-title fw-bold text-dark">
              Chi Tiết Sản Phẩm Cầu Lông
            </h5>
            <button type="button" class="btn-close" @click="moChiTiet = false"></button>
          </div>
          <div class="modal-body p-4">
            <div class="row g-4">
              <!-- Cột Trái: Ảnh sản phẩm lớn + danh sách ảnh nhỏ -->
              <div class="col-lg-5 text-center">
                <div class="p-3 bg-light rounded-4 border mb-3 position-relative">
                  <span v-if="spChon.phanTramGiamMax > 0" class="position-absolute top-0 start-0 m-3 badge bg-danger fw-bold fs-6">
                    -{{ spChon.phanTramGiamMax }}%
                  </span>
                  <img
                    :src="btChon?.anh || spChon.anhChinh"
                    :alt="spChon.tenSanPham"
                    class="img-fluid"
                    style="max-height: 320px; object-fit: contain;"
                  />
                </div>
                <!-- Gallery ảnh nhỏ -->
                <div v-if="spChon.danhSachAnh && spChon.danhSachAnh.length > 1" class="d-flex justify-content-center gap-2 flex-wrap">
                  <img
                    v-for="(anhUrl, idx) in spChon.danhSachAnh"
                    :key="idx"
                    :src="anhUrl"
                    class="rounded-3 border p-1"
                    style="width: 56px; height: 56px; object-fit: contain; cursor: pointer;"
                    :class="{ 'border-primary border-2': (btChon?.anh || spChon.anhChinh) === anhUrl }"
                    @click="spChon.anhChinh = anhUrl"
                  />
                </div>
              </div>

              <!-- Cột Phải: Thông tin chi tiết + Chọn biến thể & Mua -->
              <div class="col-lg-7">
                <div class="badge bg-primary text-white px-2 py-1 mb-2 fw-bold text-uppercase">
                  {{ spChon.tenThuongHieu }} · {{ spChon.tenDanhMuc }}
                </div>
                <h3 class="fw-bold text-dark mb-2">{{ spChon.tenSanPham }}</h3>
                <div class="small text-muted mb-3">Mã sản phẩm: <strong>{{ spChon.maSanPham }}</strong></div>

                <!-- Giá bán -->
                <div class="p-3 bg-light rounded-3 d-flex align-items-baseline gap-3 mb-3">
                  <span class="display-6 fw-black text-danger">
                    {{ formatTien(btChon?.giaBan || spChon.giaBanMin) }}
                  </span>
                  <span v-if="(btChon?.giaBanGoc || spChon.giaGocMin) > (btChon?.giaBan || spChon.giaBanMin)" class="text-muted text-decoration-line-through fs-5">
                    {{ formatTien(btChon?.giaBanGoc || spChon.giaGocMin) }}
                  </span>
                  <span v-if="(btChon?.phanTramGiam || spChon.phanTramGiamMax) > 0" class="badge bg-danger">
                    TIẾT KIỆM {{ btChon?.phanTramGiam || spChon.phanTramGiamMax }}%
                  </span>
                </div>

                <!-- Chọn Màu Sắc -->
                <div v-if="dsMauCuaSp.length > 0" class="mb-3">
                  <label class="form-label fw-bold small text-muted mb-1">Màu sắc:</label>
                  <div class="d-flex flex-wrap gap-2">
                    <button
                      v-for="m in dsMauCuaSp"
                      :key="m.id"
                      type="button"
                      class="btn btn-sm"
                      :class="mauChonId === m.id ? 'vnb-chip-active' : 'btn-outline-secondary'"
                      @click="chonMau(m.id)"
                    >
                      {{ m.ten }}
                    </button>
                  </div>
                </div>

                <!-- Chọn Trọng Lượng (3U, 4U...) -->
                <div v-if="dsTrongLuongCuaSp.length > 0" class="mb-3">
                  <label class="form-label fw-bold small text-muted mb-1">Trọng lượng & Chu vi cán:</label>
                  <div class="d-flex flex-wrap gap-2">
                    <button
                      v-for="tl in dsTrongLuongCuaSp"
                      :key="tl"
                      type="button"
                      class="btn btn-sm"
                      :class="trongLuongChon === tl ? 'vnb-chip-active' : 'btn-outline-secondary'"
                      @click="chonTrongLuong(tl)"
                    >
                      {{ tl }}
                    </button>
                  </div>
                </div>

                <!-- Thông số kỹ thuật bảng (Specs) -->
                <div class="table-responsive mb-3">
                  <table class="table table-sm table-bordered bg-white small mb-0">
                    <tbody>
                      <tr v-if="spChon.doCung"><td class="text-muted" style="width: 140px;">Độ cứng đũa:</td><td class="fw-semibold">{{ spChon.doCung }}</td></tr>
                      <tr v-if="spChon.diemCanBang"><td class="text-muted">Điểm cân bằng:</td><td class="fw-semibold">{{ spChon.diemCanBang }}</td></tr>
                      <tr v-if="spChon.chatLieu"><td class="text-muted">Chất liệu:</td><td class="fw-semibold">{{ spChon.chatLieu }}</td></tr>
                      <tr><td class="text-muted">Tình trạng tồn kho:</td><td class="text-success fw-bold">{{ btChon?.soLuongTon > 0 ? `Còn hàng (${btChon.soLuongTon} sản phẩm)` : 'Hết hàng' }}</td></tr>
                    </tbody>
                  </table>
                </div>

                <!-- Chọn số lượng & Nút Mua -->
                <div class="d-flex align-items-center gap-3 pt-3 border-top">
                  <div class="input-group" style="width: 130px;">
                    <button class="btn btn-outline-secondary" type="button" @click="soLuongMua = Math.max(1, soLuongMua - 1)">-</button>
                    <input v-model.number="soLuongMua" type="number" class="form-control text-center" min="1" :max="btChon?.soLuongTon || 999" />
                    <button class="btn btn-outline-secondary" type="button" @click="soLuongMua = Math.min(btChon?.soLuongTon || 999, soLuongMua + 1)">+</button>
                  </div>

                  <button
                    type="button"
                    class="btn btn-outline-primary fw-bold px-4 flex-grow-1"
                    :disabled="!btChon || btChon.soLuongTon <= 0"
                    @click="themVaoGioTuModal"
                  >
                    <i class="bi bi-cart-plus me-1"></i> Thêm Vào Giỏ
                  </button>

                  <button
                    type="button"
                    class="btn vnb-btn-hero fw-bold px-4 flex-grow-1"
                    :disabled="!btChon || btChon.soLuongTon <= 0"
                    @click="muaNgay"
                  >
                    <i class="bi bi-bag-check-fill me-1"></i> Mua Ngay
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
.fw-black {
  font-weight: 900;
}
.text-truncate-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.vnb-hero-banner {
  background: #060c18;
}
.vnb-hero-gradient {
  background: linear-gradient(135deg, #0a192f 0%, #0d2a4d 50%, #0977ec 100%);
}
.vnb-text-highlight {
  color: #38bdf8;
  text-shadow: 0 0 20px rgba(56, 189, 248, 0.6);
}
.vnb-btn-hero {
  background: #0977ec;
  color: #fff;
  border: none;
  transition: all 0.2s;
}
.vnb-btn-hero:hover {
  background: #075fc0;
  color: #fff;
}
.vnb-floating-racket {
  animation: floatRacket 3s ease-in-out infinite;
}
@keyframes floatRacket {
  0%, 100% { transform: translateY(0) rotate(-5deg); }
  50% { transform: translateY(-10px) rotate(0deg); }
}
.vnb-hero-badge-float {
  position: absolute;
  bottom: 0;
  right: 20px;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 12px;
}

/* Trust Cards */
.vnb-trust-card {
  transition: transform 0.2s;
}
.vnb-trust-card:hover {
  transform: translateY(-4px);
}

/* Flash Sale Section */
.vnb-flash-section {
  background: linear-gradient(135deg, #091a35 0%, #1e1b4b 50%, #0977ec 100%);
}
.vnb-flash-icon {
  width: 36px;
  height: 36px;
  background: #38bdf8;
  color: #042449;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}
.vnb-timer-box {
  background: rgba(0, 0, 0, 0.5);
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: bold;
}
.vnb-fs-card {
  transition: transform 0.2s;
  cursor: pointer;
}
.vnb-fs-card:hover {
  transform: translateY(-4px);
}

/* Title Bar */
.vnb-title-bar {
  display: inline-block;
  width: 6px;
  height: 24px;
  background: #0977ec;
  border-radius: 4px;
}

/* Product Card */
.vnb-product-card {
  transition: all 0.25s ease;
  cursor: pointer;
  background: #fff;
  border: 1px solid #eef1f5;
}
.vnb-product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1) !important;
  border-color: #c9def9;
}
.vnb-card-img-wrap {
  background: #f8fafc;
  border-top-left-radius: inherit;
  border-top-right-radius: inherit;
}
.vnb-card-img {
  transition: transform 0.3s ease;
}
.vnb-product-card:hover .vnb-card-img {
  transform: scale(1.08);
}
.vnb-card-title {
  font-size: 0.95rem;
  line-height: 1.35;
  transition: color 0.15s;
}
.vnb-product-card:hover .vnb-card-title {
  color: #0977ec !important;
}

.vnb-btn-add-cart {
  width: 38px;
  height: 38px;
  background: #e7f1fd;
  color: #0977ec;
  border: 1px solid #c9def9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  transition: all 0.2s;
}
.vnb-btn-add-cart:hover {
  background: #0977ec;
  color: #fff;
  border-color: #0977ec;
}

.vnb-chip-active {
  background: #0977ec !important;
  color: #fff !important;
  border-color: #0977ec !important;
  font-weight: 600;
}
</style>
