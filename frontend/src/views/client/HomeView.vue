<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { sanPhamService, bienTheService } from '../../services/sanPhamService'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'

const router = useRouter()
const cartStore = useCartStore()

const loading = ref(true)
const products = ref([])
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }

// 1. DỮ LIỆU BANNER SLIDER
const currentBannerIndex = ref(0)
const bannerList = [
  {
    tag: 'NEW ARRIVAL 2026',
    title: 'NEW ECLIPSION Z3',
    subtitle: 'CONFIDENCE. FROM EVERY DIRECTION. TO EVERY DIRECTION.',
    desc: 'Công nghệ Power Cushion+ độc quyền hỗ trợ bật nhảy tối đa, giảm chấn thương hoàn hảo cho từng bước di chuyển trên sân cầu.',
    btnText: 'KHÁM PHÁ NGAY',
    btnLink: '/shop/san-pham',
    bgGradient: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 50%, #38bdf8 100%)',
    badgeClass: 'bg-warning text-dark',
  },
  {
    tag: 'SIÊU PHẨM TẤN CÔNG',
    title: 'ASTROX 100ZZ KURENAI',
    subtitle: 'HYPER SLIM SHAFT - SMASH HỦY DIỆT SÂN CẦU',
    desc: 'Vũ khí tối thượng của số 1 thế giới Viktor Axelsen. Điểm cân bằng nặng đầu, đũa siêu mỏng tạo ra những cú đập cầu cắm sân không thể cản phá.',
    btnText: 'MUA NGAY - TẶNG CƯỚC BG65',
    btnLink: '/shop/san-pham?search=Astrox',
    bgGradient: 'linear-gradient(135deg, #831843 0%, #be123c 50%, #f43f5e 100%)',
    badgeClass: 'bg-light text-danger',
  },
  {
    tag: 'BỘ SƯU TẬP FOOTSTYLE PRO',
    title: 'SPEED & POWER 2026',
    subtitle: 'TRANG PHỤC & PHỤ KIỆN CHÍNH HÃNG TIÊU CHUẨN THI ĐẤU',
    desc: 'Chất liệu vải Cool-Dry thoáng khí kháng khuẩn, giày công nghệ chống lật cổ chân và các phụ kiện hỗ trợ tối đa cho lông thủ.',
    btnText: 'XEM BỘ SƯU TẬP',
    btnLink: '/shop/san-pham?khuyenMai=1',
    bgGradient: 'linear-gradient(135deg, #064e3b 0%, #059669 50%, #10b981 100%)',
    badgeClass: 'bg-warning text-dark',
  },
]

let bannerTimer = null
function nextBanner() {
  currentBannerIndex.value = (currentBannerIndex.value + 1) % bannerList.length
}
function prevBanner() {
  currentBannerIndex.value =
    (currentBannerIndex.value - 1 + bannerList.length) % bannerList.length
}
function setBanner(index) {
  currentBannerIndex.value = index
}

// 2. ĐẾM NGƯỢC FLASH SALE (Thời gian thực)
const countdownHours = ref('04')
const countdownMinutes = ref('38')
const countdownSeconds = ref('52')
let countdownTimer = null

function updateCountdown() {
  const now = new Date()
  const endOfDay = new Date()
  endOfDay.setHours(23, 59, 59, 999)
  const diff = Math.max(0, endOfDay - now)
  const h = Math.floor(diff / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)
  countdownHours.value = String(h).padStart(2, '0')
  countdownMinutes.value = String(m).padStart(2, '0')
  countdownSeconds.value = String(s).padStart(2, '0')
}

// 3. TOAST THÔNG BÁO THÊM GIỎ HÀNG
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

// 4. DANH MỤC TABS Ở "SẢN PHẨM MỚI"
const selectedCatTab = ref('ALL')
const currentSlide = ref(0)
const itemsPerSlide = ref(5)

// 5. DANH SÁCH SẢN PHẨM MẪU CHUẨN CHẤT LƯỢNG CAO (Fallback nếu backend rỗng hoặc chưa kết nối)
const fallbackProducts = [
  {
    id: 101,
    ma: 'VOT-YONEX-100ZZ',
    ten: 'Vợt Cầu Lông Yonex Astrox 100ZZ Kurenai Đỏ',
    danhMuc: 'VOT',
    thuongHieu: 'Yonex',
    minPrice: 4150000,
    originalPrice: 4790000,
    maxDiscountPercent: 13,
    hasDiscount: true,
    rating: 5.0,
    soldCount: 348,
    isNew: true,
    isHot: true,
    anhChinh: '/logo.png',
  },
  {
    id: 102,
    ma: 'GIAY-YONEX-ECLIP3',
    ten: 'Giày Cầu Lông Yonex Power Cushion Eclipsion Z3 Men',
    danhMuc: 'GIAY',
    thuongHieu: 'Yonex',
    minPrice: 2750000,
    originalPrice: 3250000,
    maxDiscountPercent: 15,
    hasDiscount: true,
    rating: 4.9,
    soldCount: 420,
    isNew: true,
    isHot: true,
    anhChinh: '/logo.png',
  },
  {
    id: 103,
    ma: 'VOT-VICTOR-RYUGA2',
    ten: 'Vợt Cầu Lông Victor Thruster Ryuga II Pro Hắc Long',
    danhMuc: 'VOT',
    thuongHieu: 'Victor',
    minPrice: 3490000,
    originalPrice: 4100000,
    maxDiscountPercent: 15,
    hasDiscount: true,
    rating: 4.9,
    soldCount: 290,
    isNew: true,
    isHot: false,
    anhChinh: '/logo.png',
  },
  {
    id: 104,
    ma: 'GIAY-YONEX-65Z3',
    ten: 'Giày Cầu Lông Yonex SHB 65Z3 C90 Trắng Đỏ Kento Momota',
    danhMuc: 'GIAY',
    thuongHieu: 'Yonex',
    minPrice: 2690000,
    originalPrice: 3100000,
    maxDiscountPercent: 13,
    hasDiscount: true,
    rating: 5.0,
    soldCount: 512,
    isNew: false,
    isHot: true,
    anhChinh: '/logo.png',
  },
  {
    id: 105,
    ma: 'AO-FOOTSTYLE-TD01',
    ten: 'Áo Cầu Lông FootStyle Tournament Cool-Dry Xanh Biển',
    danhMuc: 'AO',
    thuongHieu: 'FootStyle',
    minPrice: 220000,
    originalPrice: 290000,
    maxDiscountPercent: 24,
    hasDiscount: true,
    rating: 4.8,
    soldCount: 780,
    isNew: true,
    isHot: true,
    anhChinh: '/logo.png',
  },
  {
    id: 106,
    ma: 'VOT-LINING-AX90',
    ten: 'Vợt Cầu Lông Lining Axforce 90 Max Tiger Đen Cam',
    danhMuc: 'VOT',
    thuongHieu: 'Lining',
    minPrice: 4290000,
    originalPrice: 4890000,
    maxDiscountPercent: 12,
    hasDiscount: true,
    rating: 4.9,
    soldCount: 215,
    isNew: true,
    isHot: false,
    anhChinh: '/logo.png',
  },
  {
    id: 107,
    ma: 'GIAY-LINING-RANGER',
    ten: 'Giày Cầu Lông Lining Ranger Pro 2026 Xanh Đen',
    danhMuc: 'GIAY',
    thuongHieu: 'Lining',
    minPrice: 1850000,
    originalPrice: 2300000,
    maxDiscountPercent: 20,
    hasDiscount: true,
    rating: 4.8,
    soldCount: 310,
    isNew: false,
    isHot: true,
    anhChinh: '/logo.png',
  },
  {
    id: 108,
    ma: 'VAY-FOOTSTYLE-ACT',
    ten: 'Váy Cầu Lông Nữ FootStyle Active Trắng Viền Đen',
    danhMuc: 'VAY',
    thuongHieu: 'FootStyle',
    minPrice: 250000,
    originalPrice: 350000,
    maxDiscountPercent: 28,
    hasDiscount: true,
    rating: 4.9,
    soldCount: 460,
    isNew: true,
    isHot: false,
    anhChinh: '/logo.png',
  },
  {
    id: 109,
    ma: 'QUAN-FOOTSTYLE-CL',
    ten: 'Quần Cầu Lông Nam Co Giãn Thoáng Khí FootStyle Đen',
    danhMuc: 'QUAN',
    thuongHieu: 'FootStyle',
    minPrice: 180000,
    originalPrice: 250000,
    maxDiscountPercent: 28,
    hasDiscount: true,
    rating: 4.7,
    soldCount: 650,
    isNew: false,
    isHot: true,
    anhChinh: '/logo.png',
  },
  {
    id: 110,
    ma: 'PK-CUOC-BG65',
    ten: 'Cước Cầu Lông Yonex BG65 Titanium Nảy Trợ Lực',
    danhMuc: 'PHU_KIEN',
    thuongHieu: 'Yonex',
    minPrice: 160000,
    originalPrice: 190000,
    maxDiscountPercent: 16,
    hasDiscount: true,
    rating: 5.0,
    soldCount: 1890,
    isNew: false,
    isHot: true,
    anhChinh: '/logo.png',
  },
]

// 6. FLASH SALE ITEMS
const flashSaleList = [
  {
    id: 201,
    ten: 'Vợt Cầu Lông Yonex Astrox 88D Pro Gen 3',
    giaFlash: 3950000,
    giaGoc: 4690000,
    giam: 16,
    daBan: 28,
    tongKho: 35,
    anh: '/logo.png',
  },
  {
    id: 202,
    ten: 'Giày Cầu Lông FootStyle Ultra Cushion 2026',
    giaFlash: 990000,
    giaGoc: 1650000,
    giam: 40,
    daBan: 46,
    tongKho: 50,
    anh: '/logo.png',
  },
  {
    id: 203,
    ten: 'Túi Vợt Cầu Lông Yonex Pro 6 Cây 2 Ngăn Lớn',
    giaFlash: 750000,
    giaGoc: 1200000,
    giam: 38,
    daBan: 19,
    tongKho: 25,
    anh: '/logo.png',
  },
  {
    id: 204,
    ten: 'Combo 5 Quấn Cán Vợt FootStyle Siêu Bám & Thấm Hút',
    giaFlash: 85000,
    giaGoc: 150000,
    giam: 43,
    daBan: 89,
    tongKho: 100,
    anh: '/logo.png',
  },
]

// 7. BEST SELLERS LIST (Top bán chạy)
const bestSellerList = [
  {
    rank: 1,
    id: 104,
    ten: 'Giày Cầu Lông Yonex SHB 65Z3 C90',
    gia: 2690000,
    giaGoc: 3100000,
    daBan: '1.4k+',
    danhGia: '5.0',
    anh: '/logo.png',
  },
  {
    rank: 2,
    id: 101,
    ten: 'Vợt Cầu Lông Yonex Astrox 100ZZ Kurenai',
    gia: 4150000,
    giaGoc: 4790000,
    daBan: '1.2k+',
    danhGia: '5.0',
    anh: '/logo.png',
  },
  {
    rank: 3,
    id: 110,
    ten: 'Cước Cầu Lông Yonex BG65 Ti Trợ Lực',
    gia: 160000,
    giaGoc: 190000,
    daBan: '2.8k+',
    danhGia: '5.0',
    anh: '/logo.png',
  },
  {
    rank: 4,
    id: 105,
    ten: 'Áo Cầu Lông FootStyle Tournament Cool-Dry',
    gia: 220000,
    giaGoc: 290000,
    daBan: '950+',
    danhGia: '4.8',
    anh: '/logo.png',
  },
]

// 8. TIN TỨC & CẨM NANG CẦU LÔNG
const badmintonNews = [
  {
    id: 1,
    title: 'Bí quyết chọn vợt cầu lông chuẩn theo lối đánh Công - Thủ toàn diện',
    date: '20/10/2026',
    author: 'HLV FootStyle',
    desc: 'Lối chơi công thủ toàn diện đòi hỏi cây vợt có điểm cân bằng vừa phải (290-295mm), đũa vợt có độ dẻo trung bình để linh hoạt chuyển đổi giữa smash và thủ cầu.',
    tag: 'CẨM NANG CHỌN VỢT',
  },
  {
    id: 2,
    title: 'Top 5 đôi giày cầu lông bảo vệ cổ chân và giảm chấn tốt nhất 2026',
    date: '18/10/2026',
    author: 'FootStyle Lab',
    desc: 'Khám phá công nghệ đệm Power Cushion+ và thanh chống xoắn TPU cao cấp giúp ngăn ngừa chấn thương lật cổ chân phổ biến khi bật nhảy đập cầu.',
    tag: 'TƯ VẤN GIÀY',
  },
  {
    id: 3,
    title: 'Bao lâu nên căng cước vợt một lần và mức căng bao nhiêu kg là hợp lý?',
    date: '15/10/2026',
    author: 'Chuyên gia Đan Cước',
    desc: 'Người mới chơi nên căng từ 9.5kg - 10.5kg (21-23 lbs) để trợ lực tốt nhất. Khám phá cách nhận biết cước đã bị giãn chùng và mất lực đẩy.',
    tag: 'KỸ THUẬT CĂNG CƯỚC',
  },
]

// 9. ĐÁNH GIÁ KHÁCH HÀNG (Feedback)
const customerReviews = [
  {
    id: 1,
    name: 'Nguyễn Văn An',
    role: 'CLB Cầu Lông Cầu Giấy - Hà Nội',
    rating: 5,
    comment:
      'Mình mua cây Astrox 100ZZ tại FootStyle, shop đan cước BG65 11kg rất chuẩn tay. Đập cầu đầm và cắm sân cực kỳ. Đóng gói cẩn thận 2 lớp chống sốc.',
  },
  {
    id: 2,
    name: 'Trần Thị Thu Thảo',
    role: 'VĐV Phong Trào - TP.HCM',
    rating: 5,
    comment:
      'Đôi giày Yonex Eclipsion Z3 form ôm chân, đế bám sân gỗ rất tốt. Giao hàng hỏa tốc trong 2 tiếng tại nội thành, nhân viên tư vấn chọn size rất chuẩn!',
  },
  {
    id: 3,
    name: 'Lê Hoàng Long',
    role: 'CLB Cầu Lông Sông Hàn - Đà Nẵng',
    rating: 5,
    comment:
      'Hàng chính hãng 100%, có tem cào chống giả và bảo hành điện tử rõ ràng. Rất an tâm khi mua online tại FootStyle. Sẽ tiếp tục ủng hộ shop!',
  },
]

// 10. THƯƠNG HIỆU ĐỐI TÁC
const brandLogos = [
  { name: 'YONEX', slogan: 'Far beyond ordinary' },
  { name: 'LINING', slogan: 'Anything is possible' },
  { name: 'VICTOR', slogan: 'Ready to win' },
  { name: 'MIZUNO', slogan: 'Reach beyond' },
  { name: 'KUMPOO', slogan: 'Speed & Power' },
  { name: 'KAWASAKI', slogan: 'Pro Badminton' },
]

// NEWSLETTER
const newsletterEmail = ref('')
function subscribeNewsletter() {
  if (!newsletterEmail.value || !newsletterEmail.value.includes('@')) {
    showNotification('Vui lòng nhập địa chỉ email hợp lệ!')
    return
  }
  showNotification('Đăng ký thành công! Mã giảm giá FOOTSTYLE50K đã được gửi vào hòm thư.')
  newsletterEmail.value = ''
}

onMounted(async () => {
  // Chạy đồng hồ flash sale
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)

  // Chạy tự động chuyển banner
  bannerTimer = setInterval(nextBanner, 5000)

  try {
    loading.value = true
    const [spRes, btRes, dggRes] = await Promise.all([
      sanPhamService.getAll().catch(() => []),
      bienTheService.getAll().catch(() => []),
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

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (bannerTimer) clearInterval(bannerTimer)
  if (toastTimer) clearTimeout(toastTimer)
})

// Tính giá và khuyến mãi gộp dữ liệu thật và fallback
const enrichedProducts = computed(() => {
  let list = []
  if (products.value && products.value.length > 0) {
    list = products.value
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
          rating: 4.9,
          soldCount: 150,
          isNew: true,
          danhMuc: sp.ten?.toLowerCase().includes('giày')
            ? 'GIAY'
            : sp.ten?.toLowerCase().includes('áo')
            ? 'AO'
            : sp.ten?.toLowerCase().includes('váy')
            ? 'VAY'
            : sp.ten?.toLowerCase().includes('quần')
            ? 'QUAN'
            : 'VOT',
        }
      })
  }

  // Nếu dữ liệu backend ít hơn 5 sản phẩm thì gộp thêm fallback để giao diện luôn đầy đặn
  if (list.length < 5) {
    return [...list, ...fallbackProducts]
  }
  return list
})

// Lọc sản phẩm theo tab đang bấm
const tabFilteredProducts = computed(() => {
  if (selectedCatTab.value === 'ALL') return enrichedProducts.value
  return enrichedProducts.value.filter((p) => {
    if (selectedCatTab.value === 'VOT') {
      return (
        p.danhMuc === 'VOT' ||
        p.ten?.toLowerCase().includes('vợt') ||
        p.idDanhMuc === 1
      )
    }
    if (selectedCatTab.value === 'GIAY') {
      return p.danhMuc === 'GIAY' || p.ten?.toLowerCase().includes('giày')
    }
    if (selectedCatTab.value === 'AO') {
      return p.danhMuc === 'AO' || p.ten?.toLowerCase().includes('áo')
    }
    if (selectedCatTab.value === 'VAY') {
      return p.danhMuc === 'VAY' || p.ten?.toLowerCase().includes('váy')
    }
    if (selectedCatTab.value === 'QUAN') {
      return p.danhMuc === 'QUAN' || p.ten?.toLowerCase().includes('quần')
    }
    if (selectedCatTab.value === 'PHU_KIEN') {
      return (
        p.danhMuc === 'PHU_KIEN' ||
        p.ten?.toLowerCase().includes('cước') ||
        p.ten?.toLowerCase().includes('balo') ||
        p.ten?.toLowerCase().includes('túi')
      )
    }
    return true
  })
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
    const maxPage =
      Math.ceil(tabFilteredProducts.value.length / itemsPerSlide.value) - 1
    currentSlide.value = Math.max(0, maxPage)
  }
}

function nextSlide() {
  const maxPage =
    Math.ceil(tabFilteredProducts.value.length / itemsPerSlide.value) - 1
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

// Thêm nhanh vào giỏ hàng
function quickAddToCart(item, event) {
  if (event) event.stopPropagation()

  cartStore.addItem({
    bienTheId: item.id,
    sanPhamId: item.id,
    tenSanPham: item.ten,
    tenBienThe: item.ten,
    giaBan: item.minPrice || item.giaFlash || item.gia || 500000,
    giaGoc: item.originalPrice || item.giaGoc || 600000,
    anh: item.anhChinh || item.anh || '/logo.png',
    soLuong: 1,
    soLuongTon: 99,
  })

  showNotification(`Đã thêm "${item.ten}" vào giỏ hàng!`)
}
</script>

<template>
  <div class="home-view">
    <!-- TOAST NOTIFICATION NỔI -->
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

    <!-- 1. HERO SLIDER BANNER (Tự động chạy, có nút điều hướng & dots) -->
    <section class="vnb-hero-banner mb-4">
      <div
        class="position-relative overflow-hidden transition-bg"
        :style="{ background: bannerList[currentBannerIndex].bgGradient, minHeight: '420px' }"
      >
        <div class="vnb-container py-5">
          <div class="row align-items-center g-4">
            <div class="col-lg-7 text-white">
              <span
                class="badge px-3 py-1 fw-bold rounded-pill mb-3"
                :class="bannerList[currentBannerIndex].badgeClass"
              >
                {{ bannerList[currentBannerIndex].tag }}
              </span>
              <h1 class="display-4 fw-black text-uppercase fst-italic mb-2" style="font-weight: 900; letter-spacing: -1px;">
                {{ bannerList[currentBannerIndex].title }}
              </h1>
              <h5 class="fw-bold text-uppercase text-light mb-3 opacity-90">
                {{ bannerList[currentBannerIndex].subtitle }}
              </h5>
              <p class="lead opacity-90 small mb-4" style="max-width: 620px;">
                {{ bannerList[currentBannerIndex].desc }}
              </p>
              <div class="d-flex gap-3 flex-wrap">
                <RouterLink
                  :to="bannerList[currentBannerIndex].btnLink"
                  class="btn btn-warning btn-lg fw-bold px-4 py-2 rounded-pill text-dark shadow"
                >
                  {{ bannerList[currentBannerIndex].btnText }} <i class="bi bi-arrow-right ms-1"></i>
                </RouterLink>
                <RouterLink
                  to="/shop/san-pham"
                  class="btn btn-outline-light btn-lg fw-bold px-4 py-2 rounded-pill shadow-sm"
                >
                  XEM TẤT CẢ SẢN PHẨM
                </RouterLink>
              </div>
            </div>
            <div class="col-lg-5 text-center">
              <div class="hero-image-wrapper p-3">
                <img
                  src="/logo.png"
                  alt="Banner FootStyle"
                  class="img-fluid rounded-4 shadow-lg p-3 bg-white hero-banner-img"
                  style="max-height: 280px; object-fit: contain;"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Mũi tên chuyển Banner -->
        <button
          type="button"
          class="banner-nav-btn banner-prev"
          @click="prevBanner"
          aria-label="Banner trước"
        >
          <i class="bi bi-chevron-left"></i>
        </button>
        <button
          type="button"
          class="banner-nav-btn banner-next"
          @click="nextBanner"
          aria-label="Banner sau"
        >
          <i class="bi bi-chevron-right"></i>
        </button>

        <!-- Dots chỉ báo vị trí Slide -->
        <div class="banner-dots">
          <span
            v-for="(banner, idx) in bannerList"
            :key="idx"
            class="banner-dot"
            :class="{ active: currentBannerIndex === idx }"
            @click="setBanner(idx)"
          ></span>
        </div>
      </div>
    </section>

    <div class="vnb-container">
      <!-- 2. 4 KHỐI CAM KẾT DỊCH VỤ -->
      <section class="mb-5">
        <div class="row g-3">
          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-truck"></i>
              </div>
              <div>
                <div class="vnb-service-title">Vận chuyển <span class="highlight">TOÀN QUỐC</span></div>
                <div class="vnb-service-desc">Miễn phí giao hàng từ 500.000₫</div>
              </div>
            </div>
          </div>

          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-patch-check-fill"></i>
              </div>
              <div>
                <div class="vnb-service-title">Bảo đảm <span class="highlight">CHÍNH HÃNG</span></div>
                <div class="vnb-service-desc">Cam kết 100% chính hãng, đền x10 nếu giả</div>
              </div>
            </div>
          </div>

          <div class="col-lg-3 col-sm-6">
            <div class="vnb-service-box">
              <div class="vnb-service-icon-wrap">
                <i class="bi bi-credit-card-2-front-fill"></i>
              </div>
              <div>
                <div class="vnb-service-title">Tiến hành <span class="highlight">THANH TOÁN</span></div>
                <div class="vnb-service-desc">COD tận nơi hoặc VietQR Banking</div>
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
                <div class="vnb-service-desc">Lỗi 1 đổi 1 nhanh chóng trong 7 ngày</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 3. FLASH SALE - GIỜ VÀNG GIÁ SỐC (Có đồng hồ đếm ngược trực tiếp) -->
      <section class="mb-5 flash-sale-box p-3 p-md-4 rounded-4 text-white position-relative shadow">
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 border-bottom border-light border-opacity-25 pb-3">
          <div class="d-flex align-items-center gap-2">
            <span class="flash-icon-badge">⚡</span>
            <div>
              <h3 class="fw-black text-uppercase m-0" style="letter-spacing: 0.5px; font-weight: 900;">
                GIỜ VÀNG GIÁ SỐC - FLASH SALE
              </h3>
              <small class="text-warning fw-bold">ƯU ĐÃI CÓ HẠN - SỐ LƯỢNG GIỚI HẠN</small>
            </div>
          </div>

          <!-- Đồng hồ đếm ngược -->
          <div class="d-flex align-items-center gap-2">
            <span class="small fw-semibold text-uppercase text-light d-none d-sm-inline">Kết thúc sau:</span>
            <div class="countdown-digit">{{ countdownHours }}</div>
            <span class="fw-bold">:</span>
            <div class="countdown-digit">{{ countdownMinutes }}</div>
            <span class="fw-bold">:</span>
            <div class="countdown-digit">{{ countdownSeconds }}</div>
          </div>
        </div>

        <!-- Lưới sản phẩm Flash Sale -->
        <div class="row g-3">
          <div
            v-for="deal in flashSaleList"
            :key="deal.id"
            class="col-xl-3 col-md-6"
          >
            <div class="flash-item-card p-3 rounded-3 bg-white text-dark h-100 d-flex flex-column justify-content-between position-relative">
              <span class="flash-badge-discount">-{{ deal.giam }}%</span>
              <div class="text-center py-2">
                <img :src="deal.anh" :alt="deal.ten" class="img-fluid flash-img mb-2" />
                <h6 class="fw-bold text-truncate-2 small mb-2" :title="deal.ten" style="height: 2.4rem;">
                  {{ deal.ten }}
                </h6>
                <div class="d-flex align-items-center justify-content-center gap-2 mb-2">
                  <span class="fw-bold text-danger fs-6">{{ formatMoney(deal.giaFlash) }}</span>
                  <span class="text-muted text-decoration-line-through small">{{ formatMoney(deal.giaGoc) }}</span>
                </div>

                <!-- Thanh tiến độ đã bán -->
                <div class="progress flash-progress mb-2" style="height: 14px;">
                  <div
                    class="progress-bar progress-bar-striped progress-bar-animated bg-danger"
                    role="progressbar"
                    :style="{ width: `${(deal.daBan / deal.tongKho) * 100}%` }"
                  ></div>
                  <small class="flash-progress-text">Đã bán {{ deal.daBan }}/{{ deal.tongKho }}</small>
                </div>
              </div>

              <button
                type="button"
                class="btn btn-danger btn-sm w-100 fw-bold rounded-pill shadow-sm"
                @click="quickAddToCart(deal, $event)"
              >
                <i class="bi bi-cart-plus me-1"></i> MUA NGAY
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 4. PHẦN SẢN PHẨM MỚI (Tabs + Carousel Frame) -->
      <section class="mb-5">
        <!-- Tiêu đề có gạch xanh dưới -->
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Sản Phẩm Mới</h2>
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
            Váy Cầu Lông
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'QUAN' }"
            @click="selectedCatTab = 'QUAN'; currentSlide = 0"
          >
            Quần Cầu Lông
          </button>
          <button
            class="vnb-cat-tab"
            :class="{ active: selectedCatTab === 'PHU_KIEN' }"
            @click="selectedCatTab = 'PHU_KIEN'; currentSlide = 0"
          >
            Phụ Kiện & Cước
          </button>
        </div>

        <!-- Khung viền xanh dương chứa sản phẩm và mũi tên điều hướng -->
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
            <div class="spinner-border text-primary" role="status"></div>
            <p class="small text-muted mt-2">Đang tải sản phẩm mới...</p>
          </div>

          <div v-else-if="carouselDisplayList.length === 0" class="text-center py-5 text-muted small">
            Không có sản phẩm nào trong danh mục này.
          </div>

          <!-- Lưới 5 sản phẩm/slide -->
          <div v-else class="row g-0">
            <div
              v-for="item in carouselDisplayList"
              :key="item.id"
              class="col-xl-2 col-lg-3 col-md-4 col-6 product-col-flex"
            >
              <div class="vnb-product-item" @click="goToDetail(item.id)">
                <div class="vnb-img-box">
                  <span v-if="item.hasDiscount" class="vnb-badge-discount">-{{ item.maxDiscountPercent }}%</span>
                  <span class="vnb-badge-premium">Hot</span>
                  <img :src="item.anhChinh || '/logo.png'" :alt="item.ten" class="vnb-product-img" />
                </div>
                <div class="vnb-product-name" :title="item.ten">{{ item.ten }}</div>
                
                <!-- Sao đánh giá -->
                <div class="d-flex align-items-center justify-content-center gap-1 mb-1 text-warning" style="font-size: 0.75rem;">
                  <i class="bi bi-star-fill"></i>
                  <i class="bi bi-star-fill"></i>
                  <i class="bi bi-star-fill"></i>
                  <i class="bi bi-star-fill"></i>
                  <i class="bi bi-star-fill"></i>
                  <span class="text-muted ms-1 small">({{ item.soldCount || 100 }}+)</span>
                </div>

                <div class="mb-2">
                  <span class="vnb-product-price">{{ formatMoney(item.minPrice) }}</span>
                  <span v-if="item.originalPrice > item.minPrice" class="vnb-product-old-price">
                    {{ formatMoney(item.originalPrice) }}
                  </span>
                </div>

                <button
                  type="button"
                  class="btn btn-outline-primary btn-sm rounded-pill mt-auto w-100 quick-add-btn"
                  @click="quickAddToCart(item, $event)"
                >
                  <i class="bi bi-cart-plus me-1"></i> Thêm vào giỏ
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 5. PHẦN SALE OFF (3 Banner khuyến mãi hàng ngang) -->
      <section class="mb-5">
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Chương Trình Khuyến Mãi</h2>
          <div class="vnb-title-underline"></div>
        </div>

        <div class="row g-3">
          <div class="col-md-4">
            <div
              class="promo-banner-card rounded-4 p-4 text-white text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-warning text-dark fw-bold px-2 py-1 mb-2" style="font-size: 0.75rem;">SALE OFF</span>
              <h5 class="fw-black text-uppercase mb-1" style="font-weight: 900;">VỢT CẦU LÔNG CHÍNH HÃNG</h5>
              <div class="text-warning fw-bold small">GIẢM TỪ 20% ĐẾN 50% - TẶNG CƯỚC BG65</div>
            </div>
          </div>

          <div class="col-md-4">
            <div
              class="promo-banner-card rounded-4 p-4 text-white text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #18181b 0%, #dc2626 100%); min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-white text-danger fw-bold px-2 py-1 mb-2" style="font-size: 0.75rem;">DISCOUNT</span>
              <h5 class="fw-black text-uppercase mb-1" style="font-weight: 900;">GIÀY CẦU LÔNG CAO CẤP</h5>
              <div class="text-white opacity-90 fw-bold small">GIẢM SỐC CÔNG NGHỆ POWER CUSHION+</div>
            </div>
          </div>

          <div class="col-md-4">
            <div
              class="promo-banner-card rounded-4 p-4 text-dark text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #fef3c7 0%, #fed7aa 100%); min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-danger text-white fw-bold px-2 py-1 mb-2" style="font-size: 0.75rem;">COMBO HOT</span>
              <h5 class="fw-black text-uppercase mb-1 text-danger" style="font-weight: 900;">QUẦN ÁO THỂ THAO</h5>
              <div class="text-dark fw-bold small">MUA 2 ÁO TẶNG 1 QUẤN CÁN FOOTSTYLE</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 6. PHẦN SẢN PHẨM BÁN CHẠY NHẤT (Best Sellers Ranking) -->
      <section class="mb-5">
        <div class="vnb-section-title-wrap">
          <div class="d-inline-flex align-items-center gap-2">
            <h2 class="vnb-section-title mb-0">Sản Phẩm Bán Chạy Nhất</h2>
            <span class="badge bg-danger rounded-pill fw-bold px-2 py-1">HOT 🔥</span>
          </div>
          <div class="vnb-title-underline"></div>
        </div>

        <div class="row g-3">
          <div
            v-for="item in bestSellerList"
            :key="item.id"
            class="col-xl-3 col-md-6"
          >
            <div class="bestseller-card p-3 rounded-4 bg-white shadow-sm h-100 d-flex flex-column justify-content-between position-relative">
              <!-- Rank badge -->
              <div
                class="rank-badge"
                :class="{
                  'rank-1': item.rank === 1,
                  'rank-2': item.rank === 2,
                  'rank-3': item.rank === 3,
                  'rank-other': item.rank > 3,
                }"
              >
                TOP {{ item.rank }}
              </div>

              <div class="text-center pt-2">
                <img :src="item.anh" :alt="item.ten" class="bestseller-img mb-2" />
                <h6 class="fw-bold text-truncate-2 small mb-1" :title="item.ten" style="height: 2.3rem;">
                  {{ item.ten }}
                </h6>
                <div class="text-warning small mb-1">
                  <i class="bi bi-star-fill me-1"></i>{{ item.danhGia }} ({{ item.daBan }} đã bán)
                </div>
                <div class="mb-3">
                  <span class="fw-bold text-danger fs-6">{{ formatMoney(item.gia) }}</span>
                  <span class="text-muted text-decoration-line-through small ms-2">{{ formatMoney(item.giaGoc) }}</span>
                </div>
              </div>

              <div class="d-flex gap-2">
                <button
                  type="button"
                  class="btn btn-primary btn-sm flex-grow-1 rounded-pill fw-bold"
                  @click="quickAddToCart(item, $event)"
                >
                  <i class="bi bi-cart-plus me-1"></i> Mua ngay
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary btn-sm rounded-pill px-3"
                  @click="goToDetail(item.id)"
                  title="Xem chi tiết"
                >
                  <i class="bi bi-eye"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 7. PHẦN DANH MỤC CẦU LÔNG (4 Card danh mục có dải nơ chéo) -->
      <section class="mb-5">
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Danh Mục Nổi Bật</h2>
          <div class="vnb-title-underline"></div>
        </div>

        <div class="row g-4">
          <!-- Card 1: VỢT CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-crosshair display-3 text-white opacity-75 mb-2 d-block"></i>
                <div class="small text-white-50 fw-semibold">Yonex, Lining, Victor</div>
              </div>
              <div class="vnb-cat-ribbon">VỢT CẦU LÔNG</div>
            </div>
          </div>

          <!-- Card 2: GIÀY CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-lightning-charge display-3 text-white opacity-75 mb-2 d-block"></i>
                <div class="small text-white-50 fw-semibold">Bật nhảy, giảm chấn</div>
              </div>
              <div class="vnb-cat-ribbon">GIÀY CẦU LÔNG</div>
            </div>
          </div>

          <!-- Card 3: ÁO & VÁY CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-tsunami display-3 text-white opacity-75 mb-2 d-block"></i>
                <div class="small text-white-50 fw-semibold">Thoáng khí Cool-Dry</div>
              </div>
              <div class="vnb-cat-ribbon">QUẦN ÁO THỂ THAO</div>
            </div>
          </div>

          <!-- Card 4: PHỤ KIỆN & CƯỚC -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card" @click="router.push('/shop/san-pham')">
              <div class="p-3 text-center">
                <i class="bi bi-backpack display-3 text-white opacity-75 mb-2 d-block"></i>
                <div class="small text-white-50 fw-semibold">Cước đan, túi, quấn cán</div>
              </div>
              <div class="vnb-cat-ribbon">PHỤ KIỆN & CƯỚC</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 8. THƯƠNG HIỆU HÀNG ĐẦU (Brand Partners) -->
      <section class="mb-5 p-4 rounded-4 bg-white shadow-sm border border-light">
        <div class="text-center mb-4">
          <h4 class="fw-bold text-uppercase text-primary mb-1">Thương Hiệu Đồng Hành</h4>
          <p class="small text-muted mb-0">Đại lý ủy quyền phân phối thiết bị cầu lông chính hãng 100%</p>
        </div>

        <div class="row g-3 justify-content-center align-items-center text-center">
          <div
            v-for="brand in brandLogos"
            :key="brand.name"
            class="col-lg-2 col-md-4 col-6"
          >
            <div
              class="brand-badge-box p-3 rounded-3 cursor-pointer"
              @click="router.push(`/shop/san-pham?search=${brand.name}`)"
            >
              <h5 class="fw-black text-dark m-0 brand-text" style="font-weight: 900; letter-spacing: 1px;">
                {{ brand.name }}
              </h5>
              <small class="text-muted" style="font-size: 0.7rem;">{{ brand.slogan }}</small>
            </div>
          </div>
        </div>
      </section>

      <!-- 9. TIN TỨC & CẨM NANG CẦU LÔNG (Blog & Guides) -->
      <section class="mb-5">
        <div class="vnb-section-title-wrap">
          <h2 class="vnb-section-title">Cẩm Nang & Tin Tức Cầu Lông</h2>
          <div class="vnb-title-underline"></div>
        </div>

        <div class="row g-4">
          <div
            v-for="post in badmintonNews"
            :key="post.id"
            class="col-lg-4 col-md-6"
          >
            <div class="card h-100 border-0 rounded-4 shadow-sm overflow-hidden news-card cursor-pointer">
              <div class="news-img-box position-relative">
                <div class="news-tag-badge">{{ post.tag }}</div>
                <div class="news-fallback-art d-flex align-items-center justify-content-center text-white">
                  <i class="bi bi-newspaper display-4 opacity-75"></i>
                </div>
              </div>
              <div class="card-body p-4 d-flex flex-column justify-content-between">
                <div>
                  <div class="d-flex align-items-center gap-3 small text-muted mb-2">
                    <span><i class="bi bi-calendar3 me-1"></i> {{ post.date }}</span>
                    <span><i class="bi bi-person me-1"></i> {{ post.author }}</span>
                  </div>
                  <h6 class="fw-bold text-dark news-title mb-2">
                    {{ post.title }}
                  </h6>
                  <p class="small text-secondary mb-3">
                    {{ post.desc }}
                  </p>
                </div>
                <div class="text-primary fw-bold small">
                  Đọc tiếp cẩm nang <i class="bi bi-arrow-right ms-1"></i>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 10. Ý KIẾN KHÁCH HÀNG (Customer Feedback / Testimonials) -->
      <section class="mb-5 p-4 rounded-4" style="background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);">
        <div class="text-center mb-4">
          <h4 class="fw-bold text-uppercase text-dark mb-1">Cộng Đồng Lông Thủ Nói Gì Về FootStyle</h4>
          <p class="small text-muted mb-0">Hơn 50.000 khách hàng tin tưởng và lựa chọn đồng hành trên mọi sân cầu</p>
        </div>

        <div class="row g-3">
          <div
            v-for="rv in customerReviews"
            :key="rv.id"
            class="col-md-4"
          >
            <div class="p-4 rounded-4 bg-white shadow-sm h-100 d-flex flex-column justify-content-between review-card">
              <div>
                <div class="d-flex align-items-center gap-1 text-warning mb-2">
                  <i v-for="s in rv.rating" :key="s" class="bi bi-star-fill"></i>
                </div>
                <p class="small text-secondary fst-italic mb-3">
                  "{{ rv.comment }}"
                </p>
              </div>
              <div class="d-flex align-items-center gap-3 pt-2 border-top border-light">
                <div class="review-avatar">
                  {{ rv.name.charAt(0) }}
                </div>
                <div>
                  <div class="fw-bold text-dark small">{{ rv.name }}</div>
                  <div class="text-muted" style="font-size: 0.72rem;">{{ rv.role }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 11. ĐĂNG KÝ NHẬN BẢN TIN & MÃ GIẢM GIÁ 50.000₫ -->
      <section class="mb-5 newsletter-box p-4 p-md-5 rounded-4 text-white shadow-lg text-center position-relative overflow-hidden">
        <div class="position-relative" style="z-index: 2; max-width: 680px; margin: 0 auto;">
          <span class="badge bg-warning text-dark fw-bold px-3 py-1 rounded-pill mb-2">ƯU ĐÃI ĐẶC QUYỀN</span>
          <h3 class="fw-black text-uppercase mb-2" style="font-weight: 900;">
            ĐĂNG KÝ NHẬN VOUCHER 50.000₫
          </h3>
          <p class="small text-light opacity-90 mb-4">
            Nhận ngay mã giảm giá độc quyền cho đơn hàng đầu tiên, cùng các thông tin ra mắt vợt mới và chương trình Flash Sale hàng tuần.
          </p>

          <form @submit.prevent="subscribeNewsletter" class="d-flex gap-2 flex-column flex-sm-row justify-content-center">
            <input
              type="email"
              v-model="newsletterEmail"
              class="form-control form-control-lg rounded-pill px-4 border-0"
              placeholder="Nhập email của bạn (ví dụ: longthuviet@gmail.com)..."
              required
            />
            <button
              type="submit"
              class="btn btn-warning btn-lg fw-bold rounded-pill px-4 text-dark flex-shrink-0 shadow"
            >
              ĐĂNG KÝ NGAY
            </button>
          </form>
        </div>
      </section>
    </div>

    <!-- Nút Cuộn Lên Đầu Trang -->
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

.transition-bg {
  transition: background 0.6s ease-in-out;
}

/* Banner Nav Buttons */
.banner-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}

.banner-nav-btn:hover {
  background: white;
  color: #1d4ed8;
}

.banner-prev {
  left: 16px;
}

.banner-next {
  right: 16px;
}

/* Banner Dots */
.banner-dots {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 10;
}

.banner-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.25s;
}

.banner-dot.active {
  background: #fbbf24;
  width: 28px;
  border-radius: 9999px;
}

/* Flash Sale */
.flash-sale-box {
  background: linear-gradient(135deg, #b91c1c 0%, #dc2626 50%, #ea580c 100%);
}

.flash-icon-badge {
  font-size: 2rem;
  display: inline-block;
  animation: pulse 1s infinite alternate;
}

@keyframes pulse {
  0% { transform: scale(1); }
  100% { transform: scale(1.2); }
}

.countdown-digit {
  background: #18181b;
  color: #facc15;
  font-weight: 900;
  font-size: 1.2rem;
  padding: 4px 10px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.flash-item-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.flash-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.flash-badge-discount {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: #dc2626;
  color: white;
  font-weight: 900;
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 9999px;
}

.flash-img {
  height: 140px;
  object-fit: contain;
}

.flash-progress {
  position: relative;
  background-color: #fee2e2;
  border-radius: 9999px;
  overflow: hidden;
}

.flash-progress-text {
  position: absolute;
  width: 100%;
  text-align: center;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.65rem;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
}

/* Product grid item */
.product-col-flex {
  flex: 1 0 20%;
  max-width: 20%;
}

@media (max-width: 1200px) {
  .product-col-flex {
    flex: 1 0 25%;
    max-width: 25%;
  }
}

@media (max-width: 992px) {
  .product-col-flex {
    flex: 1 0 33.333%;
    max-width: 33.333%;
  }
}

@media (max-width: 576px) {
  .product-col-flex {
    flex: 1 0 50%;
    max-width: 50%;
  }
}

.text-truncate-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.quick-add-btn {
  font-size: 0.75rem;
  font-weight: 700;
  transition: all 0.15s;
}

.quick-add-btn:hover {
  transform: scale(1.02);
}

/* Best Seller */
.bestseller-card {
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #f1f5f9;
}

.bestseller-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
}

.bestseller-img {
  height: 160px;
  object-fit: contain;
}

.rank-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 0.72rem;
  font-weight: 900;
  padding: 3px 8px;
  border-radius: 6px;
  color: white;
}

.rank-1 {
  background: linear-gradient(135deg, #eab308 0%, #ca8a04 100%);
  box-shadow: 0 2px 6px rgba(202, 138, 4, 0.4);
}

.rank-2 {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
}

.rank-3 {
  background: linear-gradient(135deg, #b45309 0%, #78350f 100%);
}

.rank-other {
  background-color: #1d4ed8;
}

/* Brand badge */
.brand-badge-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.brand-badge-box:hover {
  background: #eff6ff;
  border-color: #1d4ed8;
  transform: translateY(-2px);
}

.brand-text {
  transition: color 0.2s;
}

.brand-badge-box:hover .brand-text {
  color: #1d4ed8 !important;
}

/* News */
.news-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.news-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.news-img-box {
  height: 180px;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
}

.news-tag-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #facc15;
  font-size: 0.7rem;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 4px;
}

.news-fallback-art {
  height: 100%;
}

.news-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 2.6rem;
}

/* Testimonial avatar */
.review-card {
  transition: transform 0.2s;
}

.review-card:hover {
  transform: translateY(-3px);
}

.review-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #1d4ed8;
  color: white;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* Newsletter */
.newsletter-box {
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
}

/* Floating Toast */
.vnb-floating-toast {
  position: fixed;
  top: 24px;
  right: 24px;
  background: #ffffff;
  border-left: 5px solid #16a34a;
  border-radius: 10px;
  padding: 14px 20px;
  z-index: 1050;
  min-width: 280px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
