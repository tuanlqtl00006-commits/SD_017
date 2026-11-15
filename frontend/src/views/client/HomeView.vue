<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { cuaHangService } from '../../services/cuaHangService'
import { useCartStore } from '../../stores/cart'
import { useAuth } from '../../composables/useAuth'
import { formatMoney, formatNgay } from '../../utils/format'
import { useAutoRefresh } from '../../composables/useAutoRefresh'
import api from '../../services/api'
import TheSanPham from '../../components/shop/TheSanPham.vue'

const router = useRouter()
const cartStore = useCartStore()
const auth = useAuth()

const loading = ref(true)
const products = ref([])
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }

// 1. BANNER: dựng từ dữ liệu thật (ảnh sản phẩm đang bán, mức giảm đang chạy). Trước đây có banner "ASTROX 100ZZ KURENAI"
// và khung logo trắng: sản phẩm đó không có trong cửa hàng, ảnh không phải sản phẩm.
const currentBannerIndex = ref(0)
const bannerList = computed(() => {
  const ds = enrichedProducts.value.filter((p) => p.anhChinh && p.anhChinh !== '/logo.png')
  const anh = (p) => p?.anhChinh || '/logo.png'
  const dauTien = ds[0]
  const nguoiMoi = ds.find((p) => String(p.idDanhMuc) === '2') || ds[1] || dauTien
  const giamNhieu = [...ds].sort((a, b) => (b.maxDiscountPercent || 0) - (a.maxDiscountPercent || 0))[0]
  const giamMax = Math.round(giamNhieu?.maxDiscountPercent || 0)
  const tenHang = thuongHieu.value.map((t) => t.ten).join(', ')
  const slides = [
    {
      tag: 'FOOTSTYLE',
      title: 'VỢT CẦU LÔNG CHÍNH HÃNG',
      subtitle: `${ds.length || ''} MẪU VỢT ĐANG BÁN`.trim(),
      desc: tenHang ? `Vợt cầu lông chính hãng từ ${tenHang}. Chọn theo trọng lượng, chu vi cán và điểm cân bằng hợp với lối chơi của bạn.` : 'Vợt cầu lông chính hãng, chọn theo trọng lượng, chu vi cán và điểm cân bằng.',
      btnText: 'MUA SẮM NGAY',
      btnLink: '/shop/san-pham',
      bgGradient: 'linear-gradient(135deg, #0b1b3f 0%, #1e3a8a 100%)',
      anh: anh(dauTien),
      tenAnh: dauTien?.ten,
    },
    {
      tag: 'DANH MỤC',
      title: 'VỢT CHO NGƯỜI MỚI',
      subtitle: 'NHẸ, DỄ ĐÁNH, GIÁ VỪA TÚI',
      desc: 'Các mẫu vợt trong danh mục Vợt cầu lông người mới, phù hợp người mới tập và học sinh.',
      btnText: 'XEM DANH MỤC',
      btnLink: '/shop/san-pham?danhMuc=2',
      bgGradient: 'linear-gradient(135deg, #062a3a 0%, #0e5a73 100%)',
      anh: anh(nguoiMoi),
      tenAnh: nguoiMoi?.ten,
    },
  ]
  if (giamMax > 0) {
    slides.push({
      tag: 'ƯU ĐÃI',
      title: `GIẢM ĐẾN ${giamMax}%`,
      subtitle: 'CÁC ĐỢT GIẢM GIÁ ĐANG DIỄN RA',
      desc: 'Giá đã giảm sẵn, không cần nhập mã. Xem thêm phiếu giảm giá đang áp dụng ở trang Ưu đãi.',
      btnText: 'XEM ƯU ĐÃI',
      btnLink: '/shop/uu-dai',
      bgGradient: 'linear-gradient(135deg, #3b0a1c 0%, #9f1239 100%)',
      anh: anh(giamNhieu),
      tenAnh: giamNhieu?.ten,
    })
  }
  return slides
})
const bannerHienTai = computed(() => bannerList.value[currentBannerIndex.value] || bannerList.value[0])

let bannerTimer = null
function nextBanner() {
  currentBannerIndex.value = (currentBannerIndex.value + 1) % bannerList.value.length
}
function prevBanner() {
  currentBannerIndex.value =
    (currentBannerIndex.value - 1 + bannerList.value.length) % bannerList.value.length
}
function setBanner(index) {
  currentBannerIndex.value = index
}

// 2. TOAST THÔNG BÁO THÊM GIỎ HÀNG
const toastVisible = ref(false)
const toastText = ref('')
let toastTimer = null

function showNotification(msg) {
  toastText.value = msg
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 3000)
}

// 3. DANH MỤC TABS Ở "SẢN PHẨM MỚI" (Dữ liệu thật từ CSDL)
const homeCategories = ref([
  { id: 'ALL', ten: 'Tất cả' },
])
const selectedCatTab = ref('ALL')
const currentSlide = ref(0)

// 4. BÁN CHẠY: số thật từ GET /cua-hang/ban-chay (đơn "Đã hoàn thành" trong 30 ngày gần nhất, giống màn Thống kê).
// Trước đây là số bịa "100+ lượt mua" và 5 sao: hệ thống không có dữ liệu đánh giá nên bỏ hẳn phần sao.
const banChay = ref({ soNgay: 30, ds: [] })
const bestSellerList = computed(() => {
  const theoId = new Map(enrichedProducts.value.map((p) => [String(p.id), p]))
  return (banChay.value.ds || [])
    .map((b) => ({ b, p: theoId.get(String(b.idSanPham)) }))
    .filter((x) => x.p)
    .map(({ b, p }, idx) => ({
      rank: idx + 1,
      sp: p,
      id: p.id,
      ten: p.ten,
      gia: p.minPrice,
      giaGoc: p.originalPrice || p.minPrice,
      daBan: Number(b.daBan) || 0,
      anh: p.anhChinh || '/logo.png',
    }))
})

// 5. THƯƠNG HIỆU: lấy từ danh sách thương hiệu đang bán (trước ghi cứng, có cả KAWASAKI không có trong cửa hàng)
const thuongHieu = ref([])
const brandLogos = computed(() => thuongHieu.value.map((t) => ({ id: t.id, name: String(t.ten || '').toUpperCase() })))

// 6. ĐỢT GIẢM GIÁ ĐANG CHẠY (thay khối ảnh tĩnh "GIẢM TỪ 20% ĐẾN 50% - TẶNG CƯỚC BG65" không có thật)
const dotGiamGia = ref([])
const dotNoiBat = computed(() => dotGiamGia.value.slice(0, 3))

async function taiBanChayVaUuDai() {
  const [bc, dot] = await Promise.allSettled([api.get('/cua-hang/ban-chay'), api.get('/cua-hang/dot-giam-gia')])
  if (bc.status === 'fulfilled' && bc.value?.data) banChay.value = bc.value.data
  if (dot.status === 'fulfilled' && Array.isArray(dot.value?.data)) dotGiamGia.value = dot.value.data
}

onMounted(async () => {
  bannerTimer = setInterval(nextBanner, 5000)

  taiBanChayVaUuDai()
  try {
    loading.value = true
    const [spRes, btRes, dggRes, dmThRes] = await Promise.all([
      cuaHangService.getDanhSachSanPham().catch(() => []),
      Promise.resolve([]),
      Promise.resolve({}),
      cuaHangService.getDanhMucVaThuongHieu().catch(() => null),
    ])

    products.value = spRes || []
    variants.value = btRes || []
    discounts.value = dggRes || {}

    thuongHieu.value = (dmThRes?.thuongHieu || []).filter((t) => t.trangThai !== 0 && t.hoatDong !== false)
    if (dmThRes?.danhMuc?.length > 0) {
      homeCategories.value = [
        { id: 'ALL', ten: 'Tất cả' },
        ...dmThRes.danhMuc
          .filter((dm) => dm.trangThai !== 0 && dm.hoatDong !== false)
          .map((dm) => ({ id: dm.id, ten: dm.ten })),
      ]
    }
  } catch (err) {
    console.error('Lỗi khi tải trang chủ:', err)
  } finally {
    loading.value = false
  }
})

// Giá / tồn kho / sản phẩm mới cập nhật ngầm
async function taiNgam() {
  if (loading.value) return
  const ds = await cuaHangService.getDanhSachSanPham()
  if (Array.isArray(ds) && ds.length) products.value = ds
  await taiBanChayVaUuDai()
}
useAutoRefresh(taiNgam, 3000, { chuKyMs: 30_000, chuDe: ['san-pham', 'ton-kho', 'giam-gia', 'hoa-don'] })

onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer)
  if (toastTimer) clearTimeout(toastTimer)
})

// Tính giá và khuyến mãi từ dữ liệu thật
const enrichedProducts = computed(() => {
  if (!products.value || products.value.length === 0) return []

  return products.value
    .filter((p) => p.hoatDong !== 0 && p.hoatDong !== false)
    .map((sp) => {
      let minPrice = sp.minPrice
      let originalPrice = sp.originalPrice
      let maxDiscountPercent = sp.maxDiscountPercent || 0

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

      const ten = sp.ten || sp.tenSanPham || 'Sản phẩm'

      return {
        ...sp,
        id: sp.id,
        ten,
        ma: sp.ma || sp.maSanPham || '',
        anhChinh: sp.anhChinh || (sp.danhSachAnh && sp.danhSachAnh[0]) || '/logo.png',
        minPrice: Number(minPrice) || 0,
        originalPrice: Number(originalPrice) || 0,
        maxDiscountPercent,
        hasDiscount: maxDiscountPercent > 0,
        isNew: true,
        idDanhMuc: sp.idDanhMuc,
        tenDanhMuc: sp.tenDanhMuc || '',
      }
    })
})

// Lọc sản phẩm theo tab danh mục
const tabFilteredProducts = computed(() => {
  if (selectedCatTab.value === 'ALL') return enrichedProducts.value
  return enrichedProducts.value.filter((p) => {
    if (p.idDanhMuc != null && String(p.idDanhMuc) === String(selectedCatTab.value)) return true
    if (String(selectedCatTab.value) === '1' && (p.idDanhMuc === 1 || p.ten?.toLowerCase().includes('vợt'))) return true
    if (String(selectedCatTab.value) === '2' && (p.idDanhMuc === 2 || p.ten?.toLowerCase().includes('mới'))) return true
    return false
  })
})

// Sản phẩm nổi bật (trái) và danh sách phụ (phải)
const spmFeatured = computed(() => {
  const list = tabFilteredProducts.value
  if (list.length === 0) return null
  return list[currentSlide.value] || list[0]
})

const spmSideList = computed(() => {
  const list = tabFilteredProducts.value
  if (list.length === 0) return []
  return list.slice(0, 5)
})

function selectFeaturedProduct(product) {
  const idx = tabFilteredProducts.value.findIndex((p) => p.id === product.id)
  if (idx !== -1) {
    currentSlide.value = idx
  }
}

function prevSlide() {
  if (tabFilteredProducts.value.length === 0) return
  if (currentSlide.value > 0) {
    currentSlide.value--
  } else {
    currentSlide.value = Math.max(0, tabFilteredProducts.value.length - 1)
  }
}

function nextSlide() {
  if (tabFilteredProducts.value.length === 0) return
  if (currentSlide.value < tabFilteredProducts.value.length - 1) {
    currentSlide.value++
  } else {
    currentSlide.value = 0
  }
}

function goToDetail(id) {
  if (!id) return
  router.push(`/shop/san-pham/${id}`)
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// Thêm nhanh vào giỏ hàng
function quickAddToCart(item, event) {
  if (event) event.stopPropagation()
  if (!item) return

  const bt = (item.bienThe || [])
    .filter((b) => Number(b.soLuongTon) > 0)
    .sort((a, b) => Number(a.giaBan) - Number(b.giaBan))[0]
  if (!bt) {
    goToDetail(item.id)
    return
  }

  cartStore.addItem({
    sanPhamId: item.id,
    sanPhamTen: item.ten,
    sanPhamMa: item.ma || '',
    bienTheId: bt.id,
    bienTheMa: bt.maSpct || '',
    mauSac: bt.tenMau || '',
    trongLuong: bt.trongLuong || '',
    chuVi: bt.chuVi || '',
    giaGoc: Number(bt.giaBanGoc) || Number(bt.giaBan) || 0,
    giaBan: Number(bt.giaBan) || 0,
    anh: item.anhChinh || '/logo.png',
    soLuong: 1,
    soLuongTon: Number(bt.soLuongTon),
  })

  showNotification(`Đã thêm "${item.ten}" vào giỏ hàng!`)
}
</script>

<template>
  <div class="home-view">
    <!-- Thông báo nổi khi thêm giỏ -->
    <transition name="fade">
      <div v-if="toastVisible" class="vnb-floating-toast shadow-lg">
        <div class="d-flex align-items-center gap-2">
          <i class="bi bi-check-circle-fill text-success fs-5"></i>
          <div>
            <div class="fw-bold text-dark small">{{ toastText }}</div>
            <RouterLink to="/shop/gio-hang" class="text-primary text-decoration-none fw-semibold" style="font-size: 0.75rem;">
              Xem giỏ hàng & thanh toán →
            </RouterLink>
          </div>
        </div>
      </div>
    </transition>

    <!-- 1. BANNER: dữ liệu thật, ảnh sản phẩm đang bán -->
    <section class="hm-hero" data-testid="home-banner">
      <div class="vnb-container">
        <div class="hm-hero-khung" :style="{ '--hm-nen': bannerHienTai.bgGradient }">
          <div class="hm-hero-chu">
            <span class="hm-hero-tag">{{ bannerHienTai.tag }}</span>
            <h1 class="hm-hero-tieu-de">{{ bannerHienTai.title }}</h1>
            <p class="hm-hero-phu">{{ bannerHienTai.subtitle }}</p>
            <p class="hm-hero-mo-ta">{{ bannerHienTai.desc }}</p>
            <div class="hm-hero-nut">
              <RouterLink :to="bannerHienTai.btnLink" class="hm-nut hm-nut-trang">
                {{ bannerHienTai.btnText }} <i class="bi bi-arrow-right"></i>
              </RouterLink>
              <RouterLink to="/shop/san-pham" class="hm-nut hm-nut-vien">Xem tất cả sản phẩm</RouterLink>
            </div>
            <div class="hm-hero-cham">
              <button
                v-for="(b, idx) in bannerList"
                :key="idx"
                type="button"
                class="hm-cham"
                :class="{ active: currentBannerIndex === idx }"
                :aria-label="`Banner ${idx + 1}`"
                @click="setBanner(idx)"
              ></button>
            </div>
          </div>
          <div class="hm-hero-anh">
            <div class="hm-hero-vong"></div>
            <img :src="bannerHienTai.anh" :alt="bannerHienTai.tenAnh || 'Vợt cầu lông FootStyle'" />
            <span v-if="bannerHienTai.tenAnh" class="hm-hero-ten-anh">{{ bannerHienTai.tenAnh }}</span>
          </div>
          <button type="button" class="hm-mui-ten banner-prev" aria-label="Banner trước" @click="prevBanner"><i class="bi bi-chevron-left"></i></button>
          <button type="button" class="hm-mui-ten banner-next" aria-label="Banner sau" @click="nextBanner"><i class="bi bi-chevron-right"></i></button>
        </div>

        <!-- Cam kết: theo đúng chính sách thật của cửa hàng -->
        <div class="hm-cam-ket">
          <div class="hm-cam-ket-o"><i class="bi bi-patch-check"></i><div><strong>Chính hãng 100%</strong><span>Vợt từ {{ thuongHieu.length }} thương hiệu</span></div></div>
          <div class="hm-cam-ket-o"><i class="bi bi-truck"></i><div><strong>Giao Hàng Nhanh</strong><span>Toàn quốc, kiểm tra hàng khi nhận</span></div></div>
          <div class="hm-cam-ket-o"><i class="bi bi-arrow-repeat"></i><div><strong>Đổi trả 7 ngày</strong><span>Lỗi do nhà sản xuất</span></div></div>
          <div class="hm-cam-ket-o"><i class="bi bi-shield-lock"></i><div><strong>Thanh toán an toàn</strong><span>COD hoặc chuyển khoản VietQR</span></div></div>
        </div>
      </div>
    </section>

    <div class="vnb-container hm-noi-dung">
      <!-- 2. BÁN CHẠY -->
      <section v-if="bestSellerList.length" class="hm-muc" data-testid="home-ban-chay">
        <header class="hm-muc-dau">
          <div>
            <h2 class="hm-muc-ten">Bán chạy nhất</h2>
            <p class="hm-muc-phu">Theo số lượng đã bán trong {{ banChay.soNgay }} ngày gần nhất</p>
          </div>
          <RouterLink to="/shop/san-pham" class="hm-xem-tat-ca">Xem tất cả <i class="bi bi-arrow-right"></i></RouterLink>
        </header>
        <div class="hm-luoi hm-luoi-4">
          <TheSanPham
            v-for="item in bestSellerList"
            :key="item.id"
            :sp="item.sp"
            :hang="item.rank"
            :nhan="`Đã bán ${item.daBan}`"
          />
        </div>
      </section>

      <!-- 3. ĐỢT GIẢM GIÁ ĐANG DIỄN RA -->
      <section v-if="dotNoiBat.length" class="hm-muc" data-testid="home-dot-giam-gia">
        <header class="hm-muc-dau">
          <div>
            <h2 class="hm-muc-ten">Đang giảm giá</h2>
            <p class="hm-muc-phu">Giá đã giảm sẵn, không cần nhập mã</p>
          </div>
          <RouterLink to="/shop/uu-dai" class="hm-xem-tat-ca">Tất cả ưu đãi <i class="bi bi-arrow-right"></i></RouterLink>
        </header>
        <div class="hm-luoi hm-luoi-3">
          <RouterLink
            v-for="(d, idx) in dotNoiBat"
            :key="d.id"
            :to="{ path: '/shop/san-pham', query: { dot: d.id } }"
            class="hm-dot"
            :class="`hm-dot-${idx % 3}`"
            data-testid="home-the-dot"
          >
            <span class="hm-dot-nhan">Giảm đến</span>
            <strong class="hm-dot-muc">{{ d.phanTramGiamMax }}%</strong>
            <span class="hm-dot-ten">{{ d.tenDot }}</span>
            <span class="hm-dot-phu">{{ d.soSanPham }} sản phẩm · đến {{ formatNgay(d.ngayKetThuc) }}</span>
            <span class="hm-dot-xem">Xem sản phẩm <i class="bi bi-arrow-right"></i></span>
          </RouterLink>
        </div>
      </section>

      <!-- 4. SẢN PHẨM -->
      <section class="hm-muc">
        <header class="hm-muc-dau">
          <div>
            <h2 class="hm-muc-ten">Khám phá vợt</h2>
            <p class="hm-muc-phu">Chọn theo danh mục</p>
          </div>
          <div class="hm-tab">
            <button
              v-for="cat in homeCategories"
              :key="cat.id"
              type="button"
              class="hm-tab-nut"
              :class="{ active: String(selectedCatTab) === String(cat.id) }"
              @click="selectedCatTab = cat.id"
            >
              {{ cat.ten }}
            </button>
          </div>
        </header>
        <div v-if="loading" class="hm-trong">Đang tải sản phẩm…</div>
        <div v-else-if="tabFilteredProducts.length === 0" class="hm-trong">Không có sản phẩm nào trong danh mục này.</div>
        <template v-else>
          <div class="hm-luoi hm-luoi-4" data-testid="home-kham-pha">
            <TheSanPham v-for="p in tabFilteredProducts.slice(0, 8)" :key="p.id" :sp="p" />
          </div>
          <div class="text-center mt-4">
            <RouterLink
              :to="selectedCatTab === 'ALL' ? '/shop/san-pham' : { path: '/shop/san-pham', query: { danhMuc: selectedCatTab } }"
              class="hm-nut hm-nut-dam"
            >
              Xem tất cả {{ tabFilteredProducts.length }} sản phẩm <i class="bi bi-arrow-right"></i>
            </RouterLink>
          </div>
        </template>
      </section>

      <!-- 5. DANH MỤC -->
      <section class="hm-muc">
        <header class="hm-muc-dau">
          <div>
            <h2 class="hm-muc-ten">Danh mục</h2>
            <p class="hm-muc-phu">Vợt cầu lông chính hãng cho mọi trình độ</p>
          </div>
        </header>
        <div class="hm-luoi hm-luoi-2">
          <RouterLink to="/shop/san-pham?danhMuc=1" class="hm-dm hm-dm-1">
            <div>
              <span class="hm-dm-nhan">Danh mục</span>
              <h3>Vợt cầu lông</h3>
              <p>Tấn công, phòng thủ, công thủ toàn diện</p>
              <span class="hm-dm-xem">Mua ngay <i class="bi bi-arrow-right"></i></span>
            </div>
            <img src="/images/san-pham/sp13_lining_halbertec_9000.webp" alt="Vợt cầu lông" loading="lazy" />
          </RouterLink>
          <RouterLink to="/shop/san-pham?danhMuc=2" class="hm-dm hm-dm-2">
            <div>
              <span class="hm-dm-nhan">Danh mục</span>
              <h3>Vợt cho người mới</h3>
              <p>Nhẹ, dễ đánh, giá vừa túi</p>
              <span class="hm-dm-xem">Mua ngay <i class="bi bi-arrow-right"></i></span>
            </div>
            <img src="/images/san-pham/sp25_yonex_astrox_10.webp" alt="Vợt cầu lông người mới" loading="lazy" />
          </RouterLink>
        </div>
      </section>

      <!-- 6. THƯƠNG HIỆU -->
      <section v-if="brandLogos.length" class="hm-muc hm-muc-cuoi">
        <header class="hm-muc-dau hm-muc-dau-giua">
          <div>
            <h2 class="hm-muc-ten">Thương hiệu</h2>
            <p class="hm-muc-phu">Các thương hiệu vợt đang bán tại FootStyle</p>
          </div>
        </header>
        <div class="hm-hang">
          <button
            v-for="brand in brandLogos"
            :key="brand.id"
            type="button"
            class="hm-hang-o"
            data-testid="home-thuong-hieu"
            @click="router.push({ path: '/shop/san-pham', query: { thuongHieu: brand.id } })"
          >
            <h5>{{ brand.name }}</h5>
            <span>Xem sản phẩm</span>
          </button>
        </div>
      </section>
    </div>

    <button type="button" class="vnb-back-to-top" aria-label="Cuộn lên đầu trang" @click="scrollToTop">
      <i class="bi bi-chevron-up"></i>
    </button>
  </div>
</template>

<style scoped>
.home-view {
  padding-bottom: 24px;
}

/* ---------- Banner ---------- */
.hm-hero {
  padding-top: 20px;
}
.hm-hero-khung {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  align-items: center;
  gap: 32px;
  min-height: 460px;
  padding: 56px 72px;
  overflow: hidden;
  color: #fff;
  border-radius: 28px;
  background: radial-gradient(circle at 78% 40%, rgba(96, 165, 250, 0.45), transparent 55%), var(--hm-nen, #0b1b3f);
  transition: background 0.5s ease;
}
.hm-hero-tag {
  display: inline-block;
  margin-bottom: 18px;
  padding: 5px 14px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
}
.hm-hero-tieu-de {
  margin: 0 0 10px;
  font-size: clamp(2.2rem, 4.2vw, 3.6rem);
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: -0.02em;
  color: #fff;
  text-transform: uppercase;
}
.hm-hero-phu {
  margin: 0 0 14px;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #bfdbfe;
  text-transform: uppercase;
}
.hm-hero-mo-ta {
  max-width: 520px;
  margin: 0 0 28px;
  font-size: 1rem;
  line-height: 1.65;
  color: #dbeafe;
}
.hm-hero-nut {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.hm-nut {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 13px 26px;
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
  border-radius: 999px;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}
.hm-nut:hover {
  transform: translateY(-1px);
}
.hm-nut-trang {
  color: #0b1b3f;
  background: #fff;
}
.hm-nut-trang:hover {
  color: #0b1b3f;
  background: #e0e7ff;
}
.hm-nut-vien {
  color: #fff;
  border: 1.5px solid rgba(255, 255, 255, 0.55);
}
.hm-nut-vien:hover {
  color: #0b1b3f;
  background: #fff;
}
.hm-nut-dam {
  color: #fff;
  background: #0f172a;
}
.hm-nut-dam:hover {
  color: #fff;
  background: #1d4ed8;
}
.hm-hero-cham {
  display: flex;
  gap: 8px;
  margin-top: 36px;
}
.hm-cham {
  width: 10px;
  height: 10px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.35);
  transition: width 0.3s ease, background 0.3s ease;
}
.hm-cham.active {
  width: 28px;
  background: #fff;
}
.hm-hero-anh {
  position: relative;
  display: grid;
  place-items: center;
  height: 100%;
  min-height: 340px;
}
.hm-hero-vong {
  position: absolute;
  width: min(100%, 380px);
  aspect-ratio: 1 / 1;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 30px 60px rgba(2, 6, 23, 0.35);
}
.hm-hero-anh img {
  position: relative;
  width: min(80%, 300px);
  aspect-ratio: 1 / 1;
  object-fit: contain;
  mix-blend-mode: multiply;
}
.hm-hero-ten-anh {
  position: absolute;
  bottom: 6px;
  max-width: 90%;
  padding: 7px 16px;
  overflow: hidden;
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #0f172a;
  background: #fff;
  border-radius: 999px;
  box-shadow: 0 8px 20px rgba(2, 6, 23, 0.2);
}
.hm-mui-ten {
  position: absolute;
  top: 50%;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  transform: translateY(-50%);
  transition: background 0.2s ease;
}
.hm-mui-ten:hover {
  background: rgba(255, 255, 255, 0.28);
}
.banner-prev {
  left: 18px;
}
.banner-next {
  right: 18px;
}

/* Cam kết */
.hm-cam-ket {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.hm-cam-ket-o {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
}
.hm-cam-ket-o .bi {
  flex: 0 0 44px;
  height: 44px;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  color: #1d4ed8;
  background: #eef4ff;
  border-radius: 12px;
}
.hm-cam-ket-o strong {
  display: block;
  font-size: 0.92rem;
  color: #0f172a;
}
.hm-cam-ket-o span {
  font-size: 0.8rem;
  color: #64748b;
}

/* ---------- Mục chung ---------- */
.hm-noi-dung {
  padding-top: 8px;
}
.hm-muc {
  margin-top: 56px;
}
.hm-muc-dau {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px 24px;
  margin-bottom: 22px;
}
.hm-muc-dau-giua {
  justify-content: center;
  text-align: center;
}
.hm-muc-ten {
  margin: 0;
  font-size: clamp(1.5rem, 2.4vw, 2rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
}
.hm-muc-phu {
  margin: 4px 0 0;
  font-size: 0.92rem;
  color: #64748b;
}
.hm-xem-tat-ca {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.92rem;
  font-weight: 700;
  color: #0f172a;
  text-decoration: none;
}
.hm-xem-tat-ca:hover {
  color: #1d4ed8;
}
.hm-luoi {
  display: grid;
  gap: 18px;
}
.hm-luoi-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.hm-luoi-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.hm-luoi-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.hm-trong {
  padding: 48px 16px;
  text-align: center;
  color: #64748b;
  background: #fff;
  border: 1px dashed #e2e8f0;
  border-radius: 16px;
}

/* Tab danh mục */
.hm-tab {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 5px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 999px;
}
.hm-tab-nut {
  padding: 8px 16px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #475569;
  background: transparent;
  border: 0;
  border-radius: 999px;
}
.hm-tab-nut.active {
  color: #fff;
  background: #0f172a;
}

/* Đợt giảm giá */
.hm-dot {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 210px;
  padding: 24px 26px;
  overflow: hidden;
  color: #fff;
  text-decoration: none;
  border-radius: 22px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.hm-dot::after {
  content: '';
  position: absolute;
  right: -40px;
  bottom: -60px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.hm-dot:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.18);
}
.hm-dot-0 {
  background: linear-gradient(135deg, #e11d48, #9f1239);
}
.hm-dot-1 {
  background: linear-gradient(135deg, #1d4ed8, #1e3a8a);
}
.hm-dot-2 {
  background: linear-gradient(135deg, #0f172a, #334155);
}
.hm-dot-nhan {
  font-size: 0.8rem;
  font-weight: 600;
  opacity: 0.85;
}
.hm-dot-muc {
  font-size: 3rem;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.03em;
}
.hm-dot-ten {
  margin-top: 12px;
  font-size: 1.05rem;
  font-weight: 700;
}
.hm-dot-phu {
  margin-top: 2px;
  font-size: 0.84rem;
  opacity: 0.85;
}
.hm-dot-xem {
  margin-top: auto;
  padding-top: 14px;
  font-size: 0.88rem;
  font-weight: 700;
}

/* Danh mục */
.hm-dm {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 240px;
  padding: 32px 36px;
  overflow: hidden;
  color: #0f172a;
  text-decoration: none;
  border-radius: 24px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.hm-dm:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
}
.hm-dm-1 {
  background: linear-gradient(135deg, #e0e7ff, #f1f5f9);
}
.hm-dm-2 {
  background: linear-gradient(135deg, #dcfce7, #f1f5f9);
}
.hm-dm-nhan {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #64748b;
}
.hm-dm h3 {
  margin: 6px 0 6px;
  font-size: 1.6rem;
  font-weight: 800;
}
.hm-dm p {
  margin: 0 0 18px;
  color: #475569;
}
.hm-dm-xem {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 0.88rem;
  font-weight: 700;
  color: #fff;
  background: #0f172a;
  border-radius: 999px;
}
.hm-dm img {
  width: 42%;
  max-height: 200px;
  object-fit: contain;
  mix-blend-mode: multiply;
  transform: rotate(-12deg);
  transition: transform 0.35s ease;
}
.hm-dm:hover img {
  transform: rotate(-6deg) scale(1.05);
}

/* Thương hiệu */
.hm-hang {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.hm-hang-o {
  padding: 22px 12px;
  text-align: center;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
  transition: border-color 0.2s ease, transform 0.2s ease;
}
.hm-hang-o:hover {
  transform: translateY(-2px);
  border-color: #c7d2fe;
}
.hm-hang-o h5 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #0f172a;
}
.hm-hang-o span {
  font-size: 0.78rem;
  color: #64748b;
}
.hm-muc-cuoi {
  margin-bottom: 24px;
}

/* Thông báo nổi + nút lên đầu */
.vnb-floating-toast {
  position: fixed;
  top: 96px;
  right: 24px;
  z-index: 2000;
  padding: 14px 18px;
  background: #fff;
  border-radius: 14px;
}
.vnb-back-to-top {
  position: fixed;
  right: 24px;
  bottom: 96px;
  z-index: 1000;
  width: 42px;
  height: 42px;
  color: #fff;
  border: 0;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ---------- Màn hình nhỏ ---------- */
@media (max-width: 1199px) {
  .hm-luoi-4 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .hm-cam-ket {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 991px) {
  .hm-hero-khung {
    grid-template-columns: minmax(0, 1fr);
    padding: 40px 56px 32px;
  }
  .hm-hero-anh {
    min-height: 260px;
  }
  .hm-luoi-3 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 767px) {
  .hm-luoi-4,
  .hm-luoi-3,
  .hm-luoi-2 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
  .hm-luoi-2 {
    grid-template-columns: minmax(0, 1fr);
  }
  .hm-muc {
    margin-top: 40px;
  }
}
@media (max-width: 575px) {
  .hm-hero {
    padding-top: 12px;
  }
  .hm-hero-khung {
    padding: 28px 22px 24px;
    border-radius: 22px;
  }
  .hm-mui-ten {
    display: none;
  }
  .hm-cam-ket {
    grid-template-columns: minmax(0, 1fr);
  }
  .hm-luoi-3 {
    grid-template-columns: minmax(0, 1fr);
  }
  .hm-dm {
    padding: 24px;
    min-height: 200px;
  }
  .hm-dm h3 {
    font-size: 1.3rem;
  }
}
</style>
