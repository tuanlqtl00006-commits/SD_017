<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sanPhamService, bienTheService } from '../../services/sanPhamService'
import { cuaHangService } from '../../services/cuaHangService'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { thuocTinhService } from '../../services/thuocTinhService'
import { formatMoney } from '../../utils/format'
import MoneyInput from '../../components/common/MoneyInput.vue'
import TheSanPham from '../../components/shop/TheSanPham.vue'
import api from '../../services/api'
import { useAutoRefresh } from '../../composables/useAutoRefresh'

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

// Danh mục thật từ CSDL
const categories = ref([
  { key: 'all', id: null, title: 'Tất cả sản phẩm', icon: 'bi-grid-fill' },
])

// Trang SALE OFF: chỉ hiện sản phẩm đang giảm giá. Cần tiêu đề / banner riêng, nếu không nhìn giống hệt "Tất cả sản phẩm"
// (nhất là khi mọi sản phẩm đều đang giảm giá thì danh sách không thay đổi gì khi bấm SALE OFF).
const laTrangSale = computed(() => route.query.khuyenMai === '1' || route.query.sale === '1')

// Ô lọc "Đang giảm giá" ở cột bộ lọc: bật / tắt bằng tham số ?khuyenMai=1 (giữ nguyên các tham số khác)
const chiDangGiam = computed({
  get: () => laTrangSale.value,
  set: (bat) => {
    const q = { ...route.query }
    delete q.sale
    if (bat) q.khuyenMai = '1'
    else delete q.khuyenMai
    router.replace({ path: '/shop/san-pham', query: q })
  },
})

// Ảnh trên banner: tối đa 3 ảnh sản phẩm thật của danh sách đang xem (trước là khung logo trắng)
const anhBanner = computed(() => {
  const ds = []
  for (const p of filteredProducts.value) {
    if (p.anhChinh && !ds.includes(p.anhChinh)) ds.push(p.anhChinh)
    if (ds.length === 3) break
  }
  return ds
})

// Xem sản phẩm của một đợt giảm giá (bấm từ trang Ưu đãi: ?dot=<id>)
const dotDangChay = ref([])
const trangThaiDot = ref('chua-tai') // 'chua-tai' | 'dang-tai' | 'xong' | 'loi'
// Chỉ khi máy chủ đã trả danh sách đợt đang chạy mà không có đợt này thì mới coi là đợt đã kết thúc (khongCon).
// Đang tải hoặc tải lỗi thì chưa biết: không lọc danh sách, không báo "đã kết thúc".
const dotDangXem = computed(() => {
  const id = route.query.dot
  if (!id) return null
  const dot = dotDangChay.value.find((d) => String(d.id) === String(id))
  if (dot) return dot
  if (trangThaiDot.value === 'xong') return { id, tenDot: '', idSanPham: [], khongCon: true }
  return { id, tenDot: '', idSanPham: null, chuaBiet: true, loi: trangThaiDot.value === 'loi' }
})
let lanTaiDot = 0
async function taiDotDangChay() {
  if (!route.query.dot) return
  const lan = ++lanTaiDot
  if (trangThaiDot.value !== 'xong') trangThaiDot.value = 'dang-tai'
  try {
    const ds = (await api.get('/cua-hang/dot-giam-gia')).data
    if (lan !== lanTaiDot) return
    dotDangChay.value = Array.isArray(ds) ? ds : []
    trangThaiDot.value = 'xong'
  } catch {
    if (lan !== lanTaiDot) return
    // lần tải trước đã có dữ liệu thì giữ, chưa có thì báo không tải được
    if (trangThaiDot.value !== 'xong') trangThaiDot.value = 'loi'
  }
}
function boLocDot() {
  const q = { ...route.query }
  delete q.dot
  router.replace({ path: '/shop/san-pham', query: q })
}

const currentCategoryTitle = computed(() => {
  const cat = categories.value.find(
    (c) => String(c.key) === String(currentCategory.value) || String(c.id) === String(currentCategory.value)
  )
  const ten = cat ? cat.title : 'Sản phẩm'
  if (dotDangXem.value) return dotDangXem.value.tenDot ? `Đợt giảm giá: ${dotDangXem.value.tenDot}` : 'Đợt giảm giá'
  if (!laTrangSale.value) return ten
  return cat && cat.key !== 'all' ? `Sale off - ${ten}` : 'Sale off - Sản phẩm đang giảm giá'
})

const currentBanner = computed(() => {
  if (dotDangXem.value && !dotDangXem.value.khongCon && !dotDangXem.value.chuaBiet) {
    const d = dotDangXem.value
    return {
      badge: 'Đợt giảm giá',
      title: d.tenDot,
      highlight: d.phanTramGiamMax > 0 ? `Giảm đến ${d.phanTramGiamMax}%` : '',
      tag: `${d.soSanPham} sản phẩm trong đợt`,
      priceText: 'Giá đã được giảm sẵn, không cần nhập mã',
      bg: 'radial-gradient(120% 140% at 85% 20%, #be123c 0%, #3b0a1c 70%)',
      mauNhan: '#fda4af',
    }
  }
  if (laTrangSale.value) {
    const maxGiam = Math.round(Math.max(0, ...enrichedProducts.value.map((p) => p.maxDiscountPercent || 0)))
    return {
      badge: 'Sale off',
      title: 'Sản phẩm đang',
      highlight: maxGiam > 0 ? `giảm đến ${maxGiam}%` : 'giảm giá',
      tag: 'Vợt cầu lông chính hãng',
      priceText: 'Giá đã được áp dụng ưu đãi',
      bg: 'radial-gradient(120% 140% at 85% 20%, #be123c 0%, #3b0a1c 70%)',
      mauNhan: '#fda4af',
    }
  }
  const activeCat = categories.value.find(
    (c) => String(c.key) === String(currentCategory.value) || String(c.id) === String(currentCategory.value)
  )
  const title = activeCat && activeCat.key !== 'all' ? activeCat.title : 'Tất cả sản phẩm'
  return {
    badge: 'FootStyle Store',
    title: title,
    highlight: 'chính hãng',
    tag: 'Cửa hàng vợt cầu lông',
    priceText: 'Cam kết chất lượng 100%',
    bg: 'radial-gradient(120% 140% at 85% 20%, #2547b8 0%, #0b1b3f 70%)', // cùng tông xanh đậm với banner trang chủ
    mauNhan: '#93c5fd',
  }
})

// ===== BỘ LỌC CHUNG =====
const selectedPrices = ref([])
// Khoảng giá tự nhập (cô review 14/11: "tôi muốn custom khoảng giá"). Bấm Áp dụng mới lọc; từ > đến thì báo lỗi, không lọc.
const giaTuNhap = ref('')
const giaDenNhap = ref('')
const khoangGia = ref({ tu: null, den: null })
const loiKhoangGia = ref('')
function apDungKhoangGia() {
  const tu = giaTuNhap.value === '' ? null : Number(giaTuNhap.value)
  const den = giaDenNhap.value === '' ? null : Number(giaDenNhap.value)
  if (tu !== null && den !== null && tu > den) {
    loiKhoangGia.value = 'Giá "từ" phải nhỏ hơn hoặc bằng giá "đến".'
    return
  }
  loiKhoangGia.value = ''
  khoangGia.value = { tu, den }
}
function boKhoangGia() {
  giaTuNhap.value = ''
  giaDenNhap.value = ''
  loiKhoangGia.value = ''
  khoangGia.value = { tu: null, den: null }
}
const coKhoangGia = computed(() => khoangGia.value.tu !== null || khoangGia.value.den !== null)

// Điện thoại: cột bộ lọc ẩn sau nút "Bộ lọc", bấm mới mở (trước đây bộ lọc dài nằm trên danh sách sản phẩm)
const moBoLoc = ref(false)
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
      title: g.title.charAt(0) + g.title.slice(1).toLowerCase(), // "XUẤT XỨ" → "Xuất xứ"
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
  boKhoangGia()
}

// Số tiêu chí lọc đang bật (hiện trên nút "Bộ lọc" ở điện thoại)
const soBoLocDangBat = computed(
  () =>
    selectedPrices.value.length +
    selectedBrands.value.length +
    ATTR_DEFS.reduce((n, g) => n + (attrSelected.value[g.key]?.length || 0), 0) +
    (coKhoangGia.value ? 1 : 0) +
    (laTrangSale.value ? 1 : 0),
)

// Nút "Xóa tất cả" / "Xóa tất cả bộ lọc": bỏ cả lọc "Đang giảm giá" và lọc theo đợt (nằm trên đường dẫn), giữ danh mục và từ khóa đang xem
function xoaTatCaBoLoc() {
  clearAllFilters()
  if (route.query.khuyenMai || route.query.sale || route.query.dot) {
    const q = { ...route.query }
    delete q.khuyenMai
    delete q.sale
    delete q.dot
    router.replace({ path: '/shop/san-pham', query: q })
  }
}

const sortBy = ref('default')

// Vào SALE OFF thì mặc định xếp giá thấp -> cao để khách thấy ngay các deal rẻ nhất; khách vẫn tự đổi cách sắp xếp được.
// Rời SALE OFF thì trả về mặc định (chỉ khi cách sắp xếp đó do trang tự đặt, không đè lựa chọn của khách).
let sortTuDat = false
watch(
  laTrangSale,
  (sale) => {
    if (sale) {
      sortBy.value = 'price-asc'
      sortTuDat = true
    } else if (sortTuDat) {
      sortBy.value = 'default'
      sortTuDat = false
    }
  },
  { immediate: true }
)
watch(sortBy, (v) => {
  if (v !== 'price-asc') sortTuDat = false
})
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
  taiDotDangChay()
  try {
    loading.value = true
    const [spRes, dmThRes] = await Promise.all([
      cuaHangService.getDanhSachSanPham(),
      cuaHangService.getDanhMucVaThuongHieu().catch(() => null),
    ])

    products.value = spRes || []
    variants.value = []
    discounts.value = {}

    if (dmThRes?.danhMuc?.length > 0) {
      categories.value = [
        { key: 'all', id: null, title: 'Tất cả sản phẩm', icon: 'bi-grid-fill' },
        ...dmThRes.danhMuc
          .filter((dm) => dm.trangThai !== 0 && dm.hoatDong !== false)
          .map((dm) => ({
            key: String(dm.id),
            id: dm.id,
            title: dm.ten,
            icon: getCategoryIcon(dm.ten),
          })),
      ]
    }

    attrOptions.value = dmThRes?.thuocTinh || {}

    if (dmThRes?.thuongHieu?.length > 0) {
      brands.value = dmThRes.thuongHieu.filter((th) => th.trangThai !== 0 && th.hoatDong !== false)
    } else {
      brands.value = []
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

// Giá / tồn kho / sản phẩm mới đổi ở quầy hoặc trang quản trị: cập nhật danh sách ngay, giữ nguyên bộ lọc đang chọn
async function taiNgam() {
  if (loading.value) return
  const ds = await cuaHangService.getDanhSachSanPham()
  if (Array.isArray(ds) && ds.length) products.value = ds
}
useAutoRefresh(taiNgam, 3000, { chuKyMs: 30_000, chuDe: ['san-pham', 'ton-kho', 'giam-gia'] })

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
watch(() => route.query.dot, taiDotDangChay)

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
  // 0b. Chỉ sản phẩm của đợt giảm giá đang xem (đợt đã kết thúc thì không còn sản phẩm nào; chưa tải xong đợt thì chưa lọc)
  if (dotDangXem.value && Array.isArray(dotDangXem.value.idSanPham)) {
    const ids = new Set(dotDangXem.value.idSanPham.map(String))
    list = list.filter((p) => ids.has(String(p.id)))
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
  // 4a. Khoảng giá tự nhập
  if (coKhoangGia.value) {
    const { tu, den } = khoangGia.value
    list = list.filter((p) => (tu === null || p.minPrice >= tu) && (den === null || p.minPrice <= den))
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
  <div class="product-catalog-view pc-trang">
    <div class="vnb-container">

      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="pc-duong-dan">
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

      <!-- Banner đầu trang theo danh mục / đợt giảm giá -->
      <section class="pc-banner" :style="{ background: currentBanner.bg }">
        <div class="pc-banner-chu">
          <span class="pc-banner-nhan">{{ currentBanner.badge }}</span>
          <h1 class="pc-banner-tieu-de">
            {{ currentBanner.title }} <span :style="{ color: currentBanner.mauNhan }">{{ currentBanner.highlight }}</span>
          </h1>
          <p class="pc-banner-phu">{{ currentBanner.tag }} · {{ currentBanner.priceText }}</p>
        </div>
        <div v-if="anhBanner.length" class="pc-banner-anh d-none d-md-flex" aria-hidden="true">
          <span v-for="(a, i) in anhBanner" :key="a" class="pc-banner-o" :style="{ zIndex: 3 - i }"><img :src="a" alt="" /></span>
        </div>
      </section>

      <!-- THANH CHỌN DANH MỤC NHANH (CATEGORY TABS VỚI NÚT CUỘN & LĂN CHUỘT) -->
      <div class="vnb-cat-pills-bar pc-danh-muc">
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
        <div class="col-lg-3 col-md-4 catalog-cot-loc" :class="{ 'is-mo': moBoLoc }" data-testid="cot-bo-loc">
          <div class="catalog-nen-loc" @click="moBoLoc = false"></div>
          <div class="vnb-filter-panel shadow-sm">

            <!-- Header Panel có nút Xóa lọc -->
            <div class="pc-loc-dau">
              <span class="pc-loc-tieu-de">
                <i class="bi bi-sliders2"></i> Bộ lọc
              </span>
              <span class="d-flex align-items-center gap-3">
                <button
                  type="button"
                  class="pc-loc-xoa"
                  @click="xoaTatCaBoLoc"
                  title="Bỏ toàn bộ tiêu chí lọc"
                >
                  Xóa tất cả
                </button>
                <button type="button" class="btn-close d-md-none" aria-label="Đóng bộ lọc" @click="moBoLoc = false"></button>
              </span>
            </div>

            <!-- 0. KHUYẾN MÃI: chỉ xem sản phẩm đang giảm giá -->
            <div class="vnb-filter-section">
              <div class="vnb-filter-section-title">Khuyến mãi</div>
              <ul class="vnb-filter-list">
                <li class="vnb-filter-item">
                  <label>
                    <input type="checkbox" v-model="chiDangGiam" data-testid="loc-dang-giam" />
                    <span>Đang giảm giá</span>
                  </label>
                </li>
              </ul>
            </div>

            <!-- 1. CHỌN MỨC GIÁ (Chung cho tất cả) -->
            <div class="vnb-filter-section">
              <div class="vnb-filter-section-title" @click="toggleSection('gia')">
                Mức giá
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
              <!-- Khoảng giá tự nhập -->
              <div v-show="sectionOpen.gia" class="catalog-khoang-gia" data-testid="khoang-gia">
                <div class="catalog-khoang-gia-o">
                  <MoneyInput id="loc-gia-tu" small :model-value="giaTuNhap" placeholder="Từ" aria-label="Giá từ" @update:model-value="(v) => (giaTuNhap = v)" />
                  <span class="text-muted">–</span>
                  <MoneyInput id="loc-gia-den" small :model-value="giaDenNhap" placeholder="Đến" aria-label="Giá đến" @update:model-value="(v) => (giaDenNhap = v)" />
                </div>
                <p v-if="loiKhoangGia" class="catalog-khoang-gia-loi" data-testid="loi-khoang-gia">{{ loiKhoangGia }}</p>
                <div class="d-flex gap-2">
                  <button type="button" class="btn btn-primary btn-sm flex-grow-1" data-testid="ap-dung-khoang-gia" @click="apDungKhoangGia">Áp dụng</button>
                  <button v-if="coKhoangGia" type="button" class="btn btn-outline-secondary btn-sm" @click="boKhoangGia">Bỏ</button>
                </div>
              </div>
            </div>

            <!-- 2. THƯƠNG HIỆU (Chung cho tất cả) -->
            <div class="vnb-filter-section">
              <div class="vnb-filter-section-title" @click="toggleSection('thuongHieu')">
                Thương hiệu
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

            <!-- Điện thoại: nút xem kết quả, đóng bộ lọc -->
            <div class="catalog-loc-chan d-md-none">
              <button type="button" class="btn btn-primary w-100 fw-bold" @click="moBoLoc = false">
                Xem {{ filteredProducts.length }} sản phẩm
              </button>
            </div>
          </div>
        </div>

        <!-- ============================================== -->
        <!-- CỘT NỘI DUNG SẢN PHẨM BÊN PHẢI                 -->
        <!-- ============================================== -->
        <div class="col-lg-9 col-md-8">
          <!-- Điện thoại: nút mở bộ lọc -->
          <button type="button" class="btn catalog-nut-loc d-md-none mb-3" data-testid="nut-bo-loc" @click="moBoLoc = true">
            <i class="bi bi-funnel"></i> Bộ lọc
            <span v-if="soBoLocDangBat" class="catalog-nut-loc-dem">{{ soBoLocDangBat }}</span>
          </button>

          <!-- Đang xem sản phẩm của một đợt giảm giá (bấm từ trang Ưu đãi) -->
          <div v-if="dotDangXem" class="catalog-dot-bar mb-3" data-testid="loc-dot">
            <span v-if="dotDangXem.khongCon"><i class="bi bi-info-circle me-1"></i> Đợt giảm giá này đã kết thúc hoặc không còn sản phẩm đang bán.</span>
            <span v-else-if="dotDangXem.chuaBiet && dotDangXem.loi"><i class="bi bi-exclamation-triangle me-1"></i> Không tải được thông tin đợt giảm giá, đang hiện tất cả sản phẩm.</span>
            <span v-else-if="dotDangXem.chuaBiet"><i class="bi bi-hourglass-split me-1"></i> Đang tải đợt giảm giá…</span>
            <span v-else><i class="bi bi-tags me-1"></i> Đang xem sản phẩm của đợt <strong>{{ dotDangXem.tenDot }}</strong></span>
            <span class="d-flex gap-3">
              <RouterLink to="/shop/uu-dai" class="catalog-dot-link">Về trang Ưu đãi</RouterLink>
              <button type="button" class="catalog-dot-link" @click="boLocDot">Xem tất cả sản phẩm</button>
            </span>
          </div>

          <!-- Thanh tiêu đề danh mục & Sắp xếp -->
          <div class="pc-thanh-cong-cu">
            <h4 class="pc-dem">
              {{ currentCategoryTitle }}
              <span>({{ filteredProducts.length }} sản phẩm)</span>
            </h4>

            <div class="pc-sap-xep">
              <label for="pc-sap-xep" class="d-none d-sm-inline">Sắp xếp</label>
              <select id="pc-sap-xep" v-model="sortBy" class="form-select form-select-sm">
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
          <div v-else-if="filteredProducts.length === 0" class="pc-trong">
            <i class="bi bi-search"></i>
            <h5>Không tìm thấy sản phẩm nào</h5>
            <p class="small">Thử chọn danh mục khác hoặc bỏ bớt tiêu chí lọc.</p>
            <div class="mt-2">
              <button class="btn btn-outline-primary btn-sm rounded-pill px-3" @click="xoaTatCaBoLoc">
                Xóa tất cả bộ lọc
              </button>
            </div>
          </div>

          <!-- Lưới sản phẩm Cards -->
          <div v-else class="pc-luoi">
            <TheSanPham v-for="item in filteredProducts" :key="item.id" :sp="item" />
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Khoảng giá tự nhập */
.catalog-khoang-gia {
  padding: 4px 16px 14px;
}
.catalog-khoang-gia-o {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.catalog-khoang-gia-o :deep(.money-input) {
  flex: 1;
  min-width: 0;
}
.catalog-khoang-gia-loi {
  margin: 0 0 8px;
  font-size: 0.8rem;
  color: #dc2626;
}

/* Điện thoại: bộ lọc mở dạng ngăn kéo từ trái, nền mờ phía sau */
.catalog-nut-loc {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  font-weight: 700;
  color: #1d4ed8;
  background: #fff;
  border: 1.5px solid #bfdbfe;
  border-radius: 999px;
}
.catalog-nut-loc-dem {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 0.75rem;
  line-height: 20px;
  text-align: center;
  color: #fff;
  background: #1d4ed8;
  border-radius: 999px;
}
.catalog-nen-loc,
.catalog-loc-chan {
  display: none;
}
@media (max-width: 767px) {
  .catalog-cot-loc {
    position: fixed;
    inset: 0;
    z-index: 1050;
    width: 100%;
    max-width: none;
    padding: 0;
    visibility: hidden;
    pointer-events: none;
  }
  .catalog-cot-loc.is-mo {
    visibility: visible;
    pointer-events: auto;
  }
  .catalog-nen-loc {
    display: block;
    position: absolute;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    opacity: 0;
    transition: opacity 0.2s ease;
  }
  .catalog-cot-loc.is-mo .catalog-nen-loc {
    opacity: 1;
  }
  .catalog-cot-loc .vnb-filter-panel {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    width: min(86%, 340px);
    overflow-y: auto;
    border-radius: 0 16px 16px 0;
    background: #fff;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
  }
  .catalog-cot-loc.is-mo .vnb-filter-panel {
    transform: translateX(0);
  }
  .catalog-loc-chan {
    display: block;
    position: sticky;
    bottom: 0;
    padding: 12px 16px;
    background: #fff;
    border-top: 1px solid #e2e8f0;
  }
}

.catalog-dot-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  padding: 0.65rem 1rem;
  font-size: 0.9rem;
  color: var(--vnb-dark);
  background: var(--vnb-orange-light);
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}
.catalog-dot-link {
  padding: 0;
  border: 0;
  background: none;
  font-weight: 600;
  color: var(--vnb-orange);
  text-decoration: none;
}
.catalog-dot-link:hover {
  text-decoration: underline;
}

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

/* ===== Giao diện mới 15/11 ===== */
.pc-trang {
  padding: 18px 0 64px;
  background: var(--ft-bg, #f6f7f9);
}
.pc-duong-dan {
  margin-bottom: 14px;
}
.pc-duong-dan :deep(.breadcrumb-item.active) {
  font-weight: 600 !important;
}
.pc-banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 190px;
  margin-bottom: 18px;
  padding: 32px 40px;
  overflow: hidden;
  color: #fff;
  border-radius: 24px;
}
.pc-banner::after {
  content: '';
  position: absolute;
  right: -80px;
  bottom: -160px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  pointer-events: none;
}
.pc-banner-chu {
  position: relative;
  z-index: 1;
  max-width: 640px;
}
.pc-banner-nhan {
  display: inline-block;
  margin-bottom: 12px;
  padding: 4px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
}
.pc-banner-tieu-de {
  margin: 0 0 8px;
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: #fff;
}
.pc-banner-phu {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.75);
}
.pc-banner-anh {
  position: relative;
  z-index: 1;
  align-items: center;
  padding-right: 8px;
}
.pc-banner-o {
  position: relative;
  display: grid;
  place-items: center;
  width: 110px;
  height: 140px;
  margin-left: -28px;
  padding: 12px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.28);
}
.pc-banner-o:nth-child(2) {
  transform: translateY(12px) rotate(5deg);
}
.pc-banner-o:nth-child(3) {
  transform: translateY(-8px) rotate(-5deg);
}
.pc-banner-o img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.pc-danh-muc {
  margin-bottom: 22px;
}
.pc-loc-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--ft-line-2, #eef0f4);
}
.pc-loc-tieu-de {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
}
.pc-loc-xoa {
  padding: 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: #e11d48;
  background: none;
  border: 0;
}
.pc-loc-xoa:hover {
  text-decoration: underline;
}
.pc-thanh-cong-cu {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 10px 10px 18px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 16px;
}
.pc-dem {
  min-width: 0;
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
}
.pc-dem span {
  margin-left: 4px;
  font-size: 0.88rem;
  font-weight: 400;
  color: #64748b;
}
.pc-sap-xep {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}
.pc-sap-xep label {
  font-size: 0.85rem;
  color: #64748b;
}
.pc-sap-xep .form-select {
  width: auto;
  padding-left: 14px;
  font-weight: 600;
  border-color: var(--ft-line-2, #eef0f4);
  border-radius: 999px;
  background-color: #f8fafc;
}
.pc-luoi {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.pc-trong {
  padding: 56px 20px;
  text-align: center;
  color: #64748b;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 20px;
}
.pc-trong > i {
  display: inline-grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin-bottom: 12px;
  font-size: 1.6rem;
  color: var(--ft-primary, #1d4ed8);
  background: #eff6ff;
  border-radius: 50%;
}
.pc-trong h5 {
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
}
@media (max-width: 1199px) {
  .pc-luoi {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 991px) {
  .pc-luoi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 767px) {
  .pc-banner {
    min-height: 0;
    padding: 24px 22px;
    border-radius: 20px;
  }
  .pc-luoi {
    gap: 10px;
  }
  .pc-thanh-cong-cu {
    padding: 8px 8px 8px 14px;
  }
  .pc-dem {
    font-size: 0.95rem;
  }
}
</style>
