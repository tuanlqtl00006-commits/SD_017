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

const CATEGORIES = [
  { key: 'all', title: 'Tất cả sản phẩm', icon: 'bi-grid-fill' },
  { key: 'vot-cau-long', title: 'Vợt Cầu Lông', icon: 'bi-record-circle' },
  { key: 'giay-cau-long', title: 'Giày Cầu Lông', icon: 'bi-shield-shaded' },
  { key: 'ao-cau-long', title: 'Áo Cầu Lông', icon: 'bi-person-fill' },
  { key: 'vay-cau-long', title: 'Váy Cầu Lông', icon: 'bi-person-heart' },
  { key: 'quan-cau-long', title: 'Quần Cầu Lông', icon: 'bi-layout-split' },
  { key: 'tui-vot-cau-long', title: 'Túi Vợt Cầu Lông', icon: 'bi-bag-fill' },
  { key: 'balo-cau-long', title: 'Balo Cầu Lông', icon: 'bi-backpack-fill' },
  { key: 'phu-kien-cau-long', title: 'Phụ Kiện Cầu Lông', icon: 'bi-tools' },
  { key: 'vot-pickleball', title: 'Vợt Pickleball', icon: 'bi-circle' },
  { key: 'giay-pickleball', title: 'Giày Pickleball', icon: 'bi-app-indicator' },
]

const CATEGORY_BANNERS = {
  'vot-cau-long': {
    badge: 'R SERIES',
    title: 'VỢT CẦU LÔNG',
    highlight: 'TARO & YONEX',
    tag: 'PHÙ HỢP CHO MỌI TRÌNH ĐỘ',
    priceText: 'Giá chỉ từ 399K',
    bg: 'linear-gradient(90deg, #18181b 0%, #b91c1c 45%, #18181b 100%)',
  },
  'giay-cau-long': {
    badge: 'POWER CUSHION',
    title: 'GIÀY CẦU LÔNG',
    highlight: 'CHÍNH HÃNG',
    tag: 'BẢO VỆ CỔ CHÂN & ÊM ÁI TỐI ĐA',
    priceText: 'Giá chỉ từ 690K',
    bg: 'linear-gradient(90deg, #0f172a 0%, #1e40af 50%, #0f172a 100%)',
  },
  'ao-cau-long': {
    badge: 'DRY-FIT 2026',
    title: 'ÁO CẦU LÔNG',
    highlight: 'THOÁNG KHÍ',
    tag: 'THẤM HÚT MỒ HÔI CO GIÃN 4 CHIỀU',
    priceText: 'Giá chỉ từ 180K',
    bg: 'linear-gradient(90deg, #1e1b4b 0%, #6d28d9 50%, #1e1b4b 100%)',
  },
  'vay-cau-long': {
    badge: 'FASHION SPORT',
    title: 'VÁY CẦU LÔNG',
    highlight: 'NỮ TÍNH',
    tag: 'TÔN DÁNG CÓ QUẦN BẢO HỘ TRONG',
    priceText: 'Giá chỉ từ 199K',
    bg: 'linear-gradient(90deg, #831843 0%, #be185d 50%, #831843 100%)',
  },
  'quan-cau-long': {
    badge: 'SPORT PRO',
    title: 'QUẦN CẦU LÔNG',
    highlight: 'NĂNG ĐỘNG',
    tag: 'TÚI KHÓA TIỆN LỢI SIÊU NHẸ',
    priceText: 'Giá chỉ từ 150K',
    bg: 'linear-gradient(90deg, #14532d 0%, #15803d 50%, #14532d 100%)',
  },
  'tui-vot-cau-long': {
    badge: 'THERMOGUARD',
    title: 'TÚI VỢT CẦU LÔNG',
    highlight: 'CHỐNG NHIỆT',
    tag: 'BẢO VỆ CƯỚC & KHUNG VỢT HOÀN HẢO',
    priceText: 'Giá chỉ từ 980K',
    bg: 'linear-gradient(90deg, #7c2d12 0%, #ea580c 50%, #7c2d12 100%)',
  },
  'balo-cau-long': {
    badge: 'ERGONOMIC',
    title: 'BALO CẦU LÔNG',
    highlight: 'ĐA NĂNG',
    tag: 'CÓ NGĂN ĐỰNG GIÀY RIÊNG & LAPTOP',
    priceText: 'Giá chỉ từ 790K',
    bg: 'linear-gradient(90deg, #1e293b 0%, #334155 50%, #1e293b 100%)',
  },
  'phu-kien-cau-long': {
    badge: 'ESSENTIALS',
    title: 'PHỤ KIỆN CẦU LÔNG',
    highlight: 'TOÀN DIỆN',
    tag: 'CƯỚC CĂNG, QUẤN CÁN, VỚ, BĂNG CHẶN',
    priceText: 'Giá chỉ từ 50K',
    bg: 'linear-gradient(90deg, #374151 0%, #4b5563 50%, #374151 100%)',
  },
  'vot-pickleball': {
    badge: 'USAPA APPROVED',
    title: 'VỢT PICKLEBALL',
    highlight: 'CARBON T700',
    tag: 'CÔNG NGHỆ THẾ HỆ MỚI SWEETSPOT CỰC RỘNG',
    priceText: 'Giá chỉ từ 1.890K',
    bg: 'linear-gradient(90deg, #042f2e 0%, #0f766e 50%, #042f2e 100%)',
  },
  'giay-pickleball': {
    badge: 'OUTDOOR COURT',
    title: 'GIÀY PICKLEBALL',
    highlight: 'BÁM SÂN',
    tag: 'ĐẾ CAO SU CHỐNG MÀI MÒN SÂN CỨNG',
    priceText: 'Giá chỉ từ 1.450K',
    bg: 'linear-gradient(90deg, #172554 0%, #1d4ed8 50%, #172554 100%)',
  },
  'all': {
    badge: 'FOOTSTYLE STORE',
    title: 'TỔNG KHO CẦU LÔNG & PICKLEBALL',
    highlight: 'CHÍNH HÃNG',
    tag: 'ĐẦY ĐỦ DỤNG CỤ, PHỤ KIỆN & THỜI TRANG',
    priceText: 'Ưu đãi lên tới 30%',
    bg: 'linear-gradient(90deg, #1e293b 0%, #1d4ed8 50%, #1e293b 100%)',
  },
}

const currentBanner = computed(() => {
  return CATEGORY_BANNERS[currentCategory.value] || CATEGORY_BANNERS['all']
})

const currentCategoryTitle = computed(() => {
  const cat = CATEGORIES.find((c) => c.key === currentCategory.value)
  return cat ? cat.title : 'Sản phẩm'
})

// ===== BỘ LỌC CHUNG =====
const selectedPrices = ref([])
const selectedBrands = ref([])
const branchSearch = ref('')
const selectedBranches = ref([])

// ===== BỘ LỌC RIÊNG: VỢT CẦU LÔNG =====
const selectedChieuDaiVot = ref([])
const selectedChieuDaiCanVot = ref([])
const selectedSwingweight = ref([])
const selectedTrongLuong = ref([])
const selectedDiemCanBang = ref([])
const selectedDoCung = ref([])
const selectedPhongCach = ref([])
const selectedNoiDung = ref([])
const selectedTrinhDo = ref([])

// ===== BỘ LỌC RIÊNG: GIÀY CẦU LÔNG & GIÀY PICKLEBALL =====
const selectedSizeGiay = ref([])
const selectedFormChan = ref([])
const selectedDoiTuongGiay = ref([])
const selectedMauSac = ref([])
const selectedLoaiDe = ref([])

// ===== BỘ LỌC RIÊNG: ÁO, VÁY, QUẦN =====
const selectedSizeTrangPhuc = ref([])
const selectedGioiTinh = ref([])
const selectedChatLieu = ref([])
const selectedKieuDangVay = ref([])
const selectedLoaiQuan = ref([])

// ===== BỘ LỌC RIÊNG: TÚI & BALO =====
const selectedSoNgan = ref([])
const selectedSucChua = ref([])
const selectedKieuTui = ref([])

// ===== BỘ LỌC RIÊNG: PHỤ KIỆN =====
const selectedLoaiPhuKien = ref([])

// ===== BỘ LỌC RIÊNG: VỢT PICKLEBALL =====
const selectedDoDayLoi = ref([])
const selectedTrongLuongPB = ref([])
const selectedBeMatPB = ref([])
const selectedHinhDangPB = ref([])

// ===== DATA OPTIONS CỦA TỪNG BỘ LỌC =====
const branches = [
  'VNB Super Center',
  'VNB PREMIUM Quận 1',
  'VNB PREMIUM Quận 7',
  'VNB PREMIUM Quận 11',
  'VNB PREMIUM Bình Thạnh',
  'VNB Quận 10 - TP.HCM',
  'VNB Cầu Giấy - Hà Nội',
  'VNB Bắc Từ Liêm - Hà Nội',
  'VNB Hải Châu - Đà Nẵng',
  'VNB Ninh Kiều - Cần Thơ',
]

// Vợt options (theo đúng 4 ảnh mẫu)
const chieuDaiVotOptions = ['665 mm', '670 mm', '675 mm']
const chieuDaiCanVotOptions = ['200 mm', '205 mm', '210 mm']
const swingweightOptions = ['Dưới 82 kg/cm2', '82-84 kg/cm2', '84-86 kg/cm2', '86-88 kg/cm2', 'Trên 88 kg/cm2']
const trongLuongOptions = ['3U: 85 - 89g', '4U: 80 - 84g', '5U: 75 - 79g', 'F: 70 - 74g', '2F: 65 - 69g']
const diemCanBangOptions = ['Nhẹ Đầu', 'Cân Bằng', 'Hơi Nặng Đầu', 'Nặng Đầu', 'Siêu Nặng Đầu']
const doCungOptions = ['Dẻo', 'Trung Bình', 'Cứng', 'Siêu Cứng']
const phongCachOptions = ['Tấn Công', 'Công Thủ Toàn Diện', 'Phản Tạt, Phòng Thủ']
const noiDungOptions = ['Đánh Đơn', 'Đánh Đôi', 'Cả Đơn và Đôi']
const trinhDoOptions = ['Mới Chơi', 'Trung Bình', 'Khá Tốt']

// Giày options
const sizeGiayOptions = ['36', '37', '38', '39', '40', '41', '42', '43', '44', '45']
const formChanOptions = ['Chân Thon', 'Chân Tiêu Chuẩn', 'Chân Bè (Wide)', 'Chân Siêu Bè (Extra Wide)']
const doiTuongGiayOptions = ['Giày Nam', 'Giày Nữ', 'Giày Trẻ Em', 'Unisex']
const mauSacOptions = ['Trắng', 'Đen', 'Đỏ', 'Xanh Dương', 'Vàng', 'Cam', 'Hồng', 'Tím']
const loaiDeOptions = ['Sân ngoài trời (Hard Court)', 'Sân đa năng trong nhà (Indoor Court)']

// Trang phục options
const sizeTrangPhucOptions = ['XS', 'S', 'M', 'L', 'XL', 'XXL', '3XL']
const gioiTinhOptions = ['Nam', 'Nữ', 'Trẻ Em', 'Unisex']
const chatLieuOptions = ['Thun lạnh 4 chiều', 'Vải mè thể thao thoáng khí', 'Polyester dệt kim']
const kieuDangVayOptions = ['Váy xòe xếp ly', 'Váy suông thể thao', 'Có quần lót bảo hộ trong']
const loaiQuanOptions = ['Quần short ngắn', 'Quần đùi lót lưới', 'Quần dài thể thao']

// Túi & Balo options
const soNganOptions = ['2 ngăn lớn', '3 ngăn lớn', 'Có ngăn cách nhiệt', 'Có ngăn đựng giày riêng']
const sucChuaTuiOptions = ['3 - 6 cây vợt', '6 - 9 cây vợt', '9 - 12 cây vợt']
const kieuTuiOptions = ['Túi chữ nhật', 'Túi du lịch thể thao', 'Túi xách tay']
const sucChuaBaloOptions = ['1 - 2 cây vợt', '2 - 3 cây vợt', 'Đựng được laptop', 'Có ngăn giày dưới đáy']

// Phụ kiện options
const loaiPhuKienOptions = [
  'Cước căng vợt cầu lông',
  'Quấn cán vợt',
  'Vớ / Tất thể thao',
  'Băng chặn mồ hôi cổ tay',
  'Bó gối / Cổ chân',
  'Quả cầu lông',
  'Móc khóa / Phụ kiện',
]

// Vợt Pickleball options
const doDayLoiOptions = ['13 mm (Tốc độ & Smash)', '14 mm (Cân bằng toàn diện)', '16 mm (Kiểm soát êm ái)']
const trongLuongPBOptions = ['Nhẹ (< 7.8 oz)', 'Trung bình (7.8 - 8.3 oz)', 'Nặng (> 8.3 oz)']
const beMatPBOptions = ['Raw Carbon T700', 'Carbon Fiber', 'Fiberglass (Sợi thủy tinh)']
const hinhDangPBOptions = ['Tiêu chuẩn (Standard)', 'Thân dài (Elongated)']

// Trạng thái toggle từng section
const sectionOpen = ref({
  gia: true,
  thuongHieu: true,
  chiNhanh: true,
  chieuDaiVot: true,
  chieuDaiCan: true,
  swingweight: true,
  trongLuong: true,
  diemCanBang: true,
  doCung: true,
  phongCach: true,
  noiDung: true,
  trinhDo: true,
  sizeGiay: true,
  formChan: true,
  doiTuongGiay: true,
  mauSac: true,
  loaiDe: true,
  sizeTrangPhuc: true,
  gioiTinh: true,
  chatLieu: true,
  kieuDangVay: true,
  loaiQuan: true,
  soNgan: true,
  sucChua: true,
  kieuTui: true,
  loaiPhuKien: true,
  doDayLoi: true,
  trongLuongPB: true,
  beMatPB: true,
  hinhDangPB: true,
})

function toggleSection(key) {
  sectionOpen.value[key] = !sectionOpen.value[key]
}

function clearAllFilters() {
  selectedPrices.value = []
  selectedBrands.value = []
  selectedBranches.value = []
  selectedChieuDaiVot.value = []
  selectedChieuDaiCanVot.value = []
  selectedSwingweight.value = []
  selectedTrongLuong.value = []
  selectedDiemCanBang.value = []
  selectedDoCung.value = []
  selectedPhongCach.value = []
  selectedNoiDung.value = []
  selectedTrinhDo.value = []
  selectedSizeGiay.value = []
  selectedFormChan.value = []
  selectedDoiTuongGiay.value = []
  selectedMauSac.value = []
  selectedLoaiDe.value = []
  selectedSizeTrangPhuc.value = []
  selectedGioiTinh.value = []
  selectedChatLieu.value = []
  selectedKieuDangVay.value = []
  selectedLoaiQuan.value = []
  selectedSoNgan.value = []
  selectedSucChua.value = []
  selectedKieuTui.value = []
  selectedLoaiPhuKien.value = []
  selectedDoDayLoi.value = []
  selectedTrongLuongPB.value = []
  selectedBeMatPB.value = []
  selectedHinhDangPB.value = []
}

const sortBy = ref('default')
const searchKeyword = ref('')

// ===== DỮ LIỆU MẪU ĐẦY ĐỦ THEO 10 DANH MỤC =====
const FALLBACK_PRODUCTS = [
  // 1. VỢT CẦU LÔNG (category: vot-cau-long)
  { id: 101, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Yonex Astrox 99 Pro', ma: 'YNX-AX99P', idThuongHieu: 1, hoatDong: 1, minPrice: 3290000, originalPrice: 3890000, maxDiscountPercent: 15, hasDiscount: true, brandName: 'Yonex' },
  { id: 102, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Victor Thruster K 9900', ma: 'VCT-TK9900', idThuongHieu: 2, hoatDong: 1, minPrice: 2750000, originalPrice: 2750000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Victor' },
  { id: 103, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Lining Axforce 80', ma: 'LN-AX80', idThuongHieu: 3, hoatDong: 1, minPrice: 3450000, originalPrice: 3900000, maxDiscountPercent: 12, hasDiscount: true, brandName: 'Lining' },
  { id: 104, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Yonex Nanoflare 700', ma: 'YNX-NF700', idThuongHieu: 1, hoatDong: 1, minPrice: 2490000, originalPrice: 2490000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Yonex' },
  { id: 105, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Victor Auraspeed 90K', ma: 'VCT-AS90K', idThuongHieu: 2, hoatDong: 1, minPrice: 3150000, originalPrice: 3500000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Victor' },
  { id: 106, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Kawasaki Master 800', ma: 'KWS-M800', idThuongHieu: 4, hoatDong: 1, minPrice: 890000, originalPrice: 1090000, maxDiscountPercent: 18, hasDiscount: true, brandName: 'Kawasaki' },
  { id: 107, category: 'vot-cau-long', ten: 'Vợt Cầu Lông Apacs Lethal 9 Pro', ma: 'APC-LT9P', idThuongHieu: 5, hoatDong: 1, minPrice: 1490000, originalPrice: 1490000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Apacs' },
  { id: 108, category: 'vot-cau-long', ten: 'Vợt Cầu Lông TARO R-Series Nhẹ Đầu', ma: 'TARO-R1', idThuongHieu: 8, hoatDong: 1, minPrice: 399000, originalPrice: 550000, maxDiscountPercent: 27, hasDiscount: true, brandName: 'Taro' },

  // 2. GIÀY CẦU LÔNG (category: giay-cau-long)
  { id: 201, category: 'giay-cau-long', ten: 'Giày Cầu Lông Yonex Power Cushion 65Z3', ma: 'YNX-PC65Z3', idThuongHieu: 1, hoatDong: 1, minPrice: 2890000, originalPrice: 3200000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Yonex' },
  { id: 202, category: 'giay-cau-long', ten: 'Giày Cầu Lông Victor A970 Ace Thi Đấu', ma: 'VCT-A970', idThuongHieu: 2, hoatDong: 1, minPrice: 1990000, originalPrice: 1990000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Victor' },
  { id: 203, category: 'giay-cau-long', ten: 'Giày Cầu Lông Lining Saga Lite Bền Bỉ', ma: 'LN-SLITE', idThuongHieu: 3, hoatDong: 1, minPrice: 1290000, originalPrice: 1450000, maxDiscountPercent: 11, hasDiscount: true, brandName: 'Lining' },
  { id: 204, category: 'giay-cau-long', ten: 'Giày Cầu Lông Kawasaki K086 Chống Lật', ma: 'KWS-K086', idThuongHieu: 4, hoatDong: 1, minPrice: 850000, originalPrice: 950000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Kawasaki' },
  { id: 205, category: 'giay-cau-long', ten: 'Giày Cầu Lông Kumpoo KH-G08 Siêu Êm', ma: 'KP-G08', idThuongHieu: 9, hoatDong: 1, minPrice: 690000, originalPrice: 790000, maxDiscountPercent: 12, hasDiscount: true, brandName: 'Kumpoo' },

  // 3. ÁO CẦU LÔNG (category: ao-cau-long)
  { id: 301, category: 'ao-cau-long', ten: 'Áo Cầu Lông Yonex Tournament 20541 Nam', ma: 'YNX-20541', idThuongHieu: 1, hoatDong: 1, minPrice: 450000, originalPrice: 520000, maxDiscountPercent: 13, hasDiscount: true, brandName: 'Yonex' },
  { id: 302, category: 'ao-cau-long', ten: 'Áo Cầu Lông Victor T-30013 Mát Lạnh', ma: 'VCT-T30013', idThuongHieu: 2, hoatDong: 1, minPrice: 380000, originalPrice: 380000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Victor' },
  { id: 303, category: 'ao-cau-long', ten: 'Áo Cầu Lông Lining Thi Đấu Quốc Gia', ma: 'LN-ATD', idThuongHieu: 3, hoatDong: 1, minPrice: 420000, originalPrice: 490000, maxDiscountPercent: 14, hasDiscount: true, brandName: 'Lining' },
  { id: 304, category: 'ao-cau-long', ten: 'Áo Cầu Lông VNB Sports Co Giãn 4 Chiều', ma: 'VNB-A01', idThuongHieu: 10, hoatDong: 1, minPrice: 180000, originalPrice: 220000, maxDiscountPercent: 18, hasDiscount: true, brandName: 'VNB' },

  // 4. VÁY CẦU LÔNG (category: vay-cau-long)
  { id: 401, category: 'vay-cau-long', ten: 'Váy Cầu Lông Yonex Nữ Xếp Ly 26038', ma: 'YNX-26038', idThuongHieu: 1, hoatDong: 1, minPrice: 390000, originalPrice: 450000, maxDiscountPercent: 13, hasDiscount: true, brandName: 'Yonex' },
  { id: 402, category: 'vay-cau-long', ten: 'Váy Cầu Lông Lining Có Quần Trong Thể Thao', ma: 'LN-VAY01', idThuongHieu: 3, hoatDong: 1, minPrice: 350000, originalPrice: 390000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Lining' },
  { id: 403, category: 'vay-cau-long', ten: 'Váy Cầu Lông Victec Thể Thao Năng Động', ma: 'VT-VAY02', idThuongHieu: 11, hoatDong: 1, minPrice: 220000, originalPrice: 220000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Victec' },
  { id: 404, category: 'vay-cau-long', ten: 'Váy Cầu Lông Taro Thể Thao Xinh Xắn', ma: 'TARO-V01', idThuongHieu: 8, hoatDong: 1, minPrice: 199000, originalPrice: 250000, maxDiscountPercent: 20, hasDiscount: true, brandName: 'Taro' },

  // 5. QUẦN CẦU LÔNG (category: quan-cau-long)
  { id: 501, category: 'quan-cau-long', ten: 'Quần Short Cầu Lông Yonex Túi Khóa', ma: 'YNX-Q01', idThuongHieu: 1, hoatDong: 1, minPrice: 290000, originalPrice: 350000, maxDiscountPercent: 17, hasDiscount: true, brandName: 'Yonex' },
  { id: 502, category: 'quan-cau-long', ten: 'Quần Cầu Lông Lining Chống Tĩnh Điện', ma: 'LN-Q02', idThuongHieu: 3, hoatDong: 1, minPrice: 320000, originalPrice: 320000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Lining' },
  { id: 503, category: 'quan-cau-long', ten: 'Quần Cầu Lông VNB Co Giãn Thể Thao', ma: 'VNB-Q03', idThuongHieu: 10, hoatDong: 1, minPrice: 150000, originalPrice: 180000, maxDiscountPercent: 16, hasDiscount: true, brandName: 'VNB' },
  { id: 504, category: 'quan-cau-long', ten: 'Quần Cầu Lông Victor Thoáng Khí Nam Nữ', ma: 'VCT-Q04', idThuongHieu: 2, hoatDong: 1, minPrice: 270000, originalPrice: 300000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Victor' },

  // 6. TÚI VỢT CẦU LÔNG (category: tui-vot-cau-long)
  { id: 601, category: 'tui-vot-cau-long', ten: 'Túi Vợt Cầu Lông Yonex Pro 9 Cây BA92229', ma: 'YNX-BA92229', idThuongHieu: 1, hoatDong: 1, minPrice: 1690000, originalPrice: 1990000, maxDiscountPercent: 15, hasDiscount: true, brandName: 'Yonex' },
  { id: 602, category: 'tui-vot-cau-long', ten: 'Túi Vợt Victor Supreme 6 Cây Chống Nước', ma: 'VCT-SUP6', idThuongHieu: 2, hoatDong: 1, minPrice: 1190000, originalPrice: 1190000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Victor' },
  { id: 603, category: 'tui-vot-cau-long', ten: 'Túi Vợt Cầu Lông Lining 3 Ngăn Cách Nhiệt', ma: 'LN-T3N', idThuongHieu: 3, hoatDong: 1, minPrice: 980000, originalPrice: 1150000, maxDiscountPercent: 14, hasDiscount: true, brandName: 'Lining' },

  // 7. BALO CẦU LÔNG (category: balo-cau-long)
  { id: 701, category: 'balo-cau-long', ten: 'Balo Cầu Lông Yonex BA232 Đựng Vợt & Giày', ma: 'YNX-BA232', idThuongHieu: 1, hoatDong: 1, minPrice: 990000, originalPrice: 1290000, maxDiscountPercent: 23, hasDiscount: true, brandName: 'Yonex' },
  { id: 702, category: 'balo-cau-long', ten: 'Balo Cầu Lông Victor BR9011 Chống Mỏi', ma: 'VCT-BR9011', idThuongHieu: 2, hoatDong: 1, minPrice: 850000, originalPrice: 850000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Victor' },
  { id: 703, category: 'balo-cau-long', ten: 'Balo Cầu Lông Lining Đa Năng Có Ngăn Laptop', ma: 'LN-BL01', idThuongHieu: 3, hoatDong: 1, minPrice: 790000, originalPrice: 890000, maxDiscountPercent: 11, hasDiscount: true, brandName: 'Lining' },

  // 8. PHỤ KIỆN CẦU LÔNG (category: phu-kien-cau-long)
  { id: 801, category: 'phu-kien-cau-long', ten: 'Cước Cầu Lông Yonex BG65 Ti Nảy Vang', ma: 'YNX-BG65TI', idThuongHieu: 1, hoatDong: 1, minPrice: 125000, originalPrice: 140000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Yonex' },
  { id: 802, category: 'phu-kien-cau-long', ten: 'Cước Cầu Lông Lining No.1 Trợ Lực Đỉnh Cao', ma: 'LN-NO1', idThuongHieu: 3, hoatDong: 1, minPrice: 110000, originalPrice: 110000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Lining' },
  { id: 803, category: 'phu-kien-cau-long', ten: 'Hộp 3 Quấn Cán Vợt Cầu Lông Yonex AC102EX', ma: 'YNX-AC102', idThuongHieu: 1, hoatDong: 1, minPrice: 95000, originalPrice: 110000, maxDiscountPercent: 13, hasDiscount: true, brandName: 'Yonex' },
  { id: 804, category: 'phu-kien-cau-long', ten: 'Hộp 12 Quả Cầu Lông Thành Công 77 Tốc Độ', ma: 'TC-77', idThuongHieu: 12, hoatDong: 1, minPrice: 230000, originalPrice: 250000, maxDiscountPercent: 8, hasDiscount: true, brandName: 'Thành Công' },
  { id: 805, category: 'phu-kien-cau-long', ten: 'Vớ Cầu Lông Yonex Cổ Cao Chống Trượt', ma: 'YNX-VO01', idThuongHieu: 1, hoatDong: 1, minPrice: 50000, originalPrice: 65000, maxDiscountPercent: 23, hasDiscount: true, brandName: 'Yonex' },

  // 9. VỢT PICKLEBALL (category: vot-pickleball)
  { id: 901, category: 'vot-pickleball', ten: 'Vợt Pickleball Joola Ben Johns Perseus CFS 16', ma: 'JL-BJ16', idThuongHieu: 13, hoatDong: 1, minPrice: 4890000, originalPrice: 5300000, maxDiscountPercent: 8, hasDiscount: true, brandName: 'Joola' },
  { id: 902, category: 'vot-pickleball', ten: 'Vợt Pickleball Head Radical Elite Trợ Lực', ma: 'HD-RE', idThuongHieu: 14, hoatDong: 1, minPrice: 1890000, originalPrice: 2100000, maxDiscountPercent: 10, hasDiscount: true, brandName: 'Head' },
  { id: 903, category: 'vot-pickleball', ten: 'Vợt Pickleball Franklin Carbon T700 Thi Đấu', ma: 'FK-T700', idThuongHieu: 15, hoatDong: 1, minPrice: 2450000, originalPrice: 2450000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Franklin' },
  { id: 904, category: 'vot-pickleball', ten: 'Vợt Pickleball Selkirk Vanguard Power Air', ma: 'SK-VG', idThuongHieu: 16, hoatDong: 1, minPrice: 3990000, originalPrice: 4500000, maxDiscountPercent: 11, hasDiscount: true, brandName: 'Selkirk' },

  // 10. GIÀY PICKLEBALL (category: giay-pickleball)
  { id: 1001, category: 'giay-pickleball', ten: 'Giày Pickleball Asics Gel Resolution 9', ma: 'AS-GEL9', idThuongHieu: 17, hoatDong: 1, minPrice: 3290000, originalPrice: 3600000, maxDiscountPercent: 8, hasDiscount: true, brandName: 'Asics' },
  { id: 1002, category: 'giay-pickleball', ten: 'Giày Pickleball Jogarbola P-Drive Chuyên Sân Cứng', ma: 'JG-PDRIVE', idThuongHieu: 18, hoatDong: 1, minPrice: 1450000, originalPrice: 1650000, maxDiscountPercent: 12, hasDiscount: true, brandName: 'Jogarbola' },
  { id: 1003, category: 'giay-pickleball', ten: 'Giày Pickleball Babolat Jet Tere All Court', ma: 'BB-JT', idThuongHieu: 19, hoatDong: 1, minPrice: 2190000, originalPrice: 2190000, maxDiscountPercent: 0, hasDiscount: false, brandName: 'Babolat' },
]

const FALLBACK_BRANDS = [
  { id: 1, ten: 'Yonex' },
  { id: 2, ten: 'Victor' },
  { id: 3, ten: 'Lining' },
  { id: 4, ten: 'Kawasaki' },
  { id: 5, ten: 'Apacs' },
  { id: 6, ten: 'Mizuno' },
  { id: 7, ten: 'Forza' },
  { id: 8, ten: 'Taro' },
  { id: 9, ten: 'Kumpoo' },
  { id: 10, ten: 'VNB' },
  { id: 11, ten: 'Victec' },
  { id: 12, ten: 'Thành Công' },
  { id: 13, ten: 'Joola' },
  { id: 14, ten: 'Head' },
  { id: 15, ten: 'Franklin' },
  { id: 16, ten: 'Selkirk' },
  { id: 17, ten: 'Asics' },
  { id: 18, ten: 'Jogarbola' },
  { id: 19, ten: 'Babolat' },
]

onMounted(async () => {
  try {
    loading.value = true
    const [spRes, btRes, dggRes, brandRes] = await Promise.all([
      sanPhamService.getAll(),
      bienTheService.getAll(),
      dotGiamGiaService.getApDungHomNay().catch(() => ({})),
      thuocTinhService.getAll('thuong-hieu').catch(() => []),
    ])

    products.value = spRes && spRes.length > 0 ? spRes : []
    variants.value = btRes || []
    discounts.value = dggRes || {}
    brands.value = brandRes && brandRes.length > 0 ? brandRes : FALLBACK_BRANDS

    syncFromUrl()
  } catch (err) {
    console.warn('Backend chưa sẵn sàng, dùng dữ liệu mẫu:', err)
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
  } else {
    currentCategory.value = 'vot-cau-long'
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
  const rawList = products.value.length > 0 ? products.value : FALLBACK_PRODUCTS

  return rawList
    .filter((p) => p.hoatDong !== 0 && p.hoatDong !== false)
    .map((sp) => {
      if (sp.minPrice !== undefined && sp.brandName !== undefined) {
        return sp
      }

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
        category: sp.category || 'vot-cau-long',
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

  // 1. Lọc theo Danh mục (nếu không chọn 'all')
  if (currentCategory.value && currentCategory.value !== 'all') {
    list = list.filter((p) => p.category === currentCategory.value)
  }

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
            v-for="cat in CATEGORIES"
            :key="cat.key"
            type="button"
            class="vnb-cat-pill-btn"
            :class="{ active: currentCategory === cat.key }"
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

            <!-- 3. CHI NHÁNH (Chung cho tất cả) -->
            <div class="vnb-filter-section">
              <div class="vnb-filter-section-title" @click="toggleSection('chiNhanh')">
                CHI NHÁNH
                <i class="bi" :class="sectionOpen.chiNhanh ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
              </div>
              <div v-show="sectionOpen.chiNhanh">
                <div class="vnb-filter-search-wrap">
                  <input
                    type="text"
                    class="vnb-filter-search"
                    placeholder="Tìm chi nhánh"
                    v-model="branchSearch"
                  />
                  <button class="vnb-filter-search-btn" type="button">
                    <i class="bi bi-search"></i>
                  </button>
                </div>
                <ul class="vnb-filter-list vnb-filter-list-scroll">
                  <li v-for="br in filteredBranches" :key="br" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="br" v-model="selectedBranches" />
                      <span>{{ br }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </div>

            <!-- ============================================== -->
            <!-- BỘ LỌC RIÊNG CHO: VỢT CẦU LÔNG                -->
            <!-- ============================================== -->
            <template v-if="currentCategory === 'vot-cau-long' || currentCategory === 'all'">
              <!-- Chiều dài vợt -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('chieuDaiVot')">
                  CHIỀU DÀI VỢT
                  <i class="bi" :class="sectionOpen.chieuDaiVot ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.chieuDaiVot" class="vnb-filter-list">
                  <li v-for="opt in chieuDaiVotOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedChieuDaiVot" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Chiều dài cán vợt -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('chieuDaiCan')">
                  CHIỀU DÀI CÁN VỢT
                  <i class="bi" :class="sectionOpen.chieuDaiCan ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.chieuDaiCan" class="vnb-filter-list">
                  <li v-for="opt in chieuDaiCanVotOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedChieuDaiCanVot" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Swingweight -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('swingweight')">
                  SWINGWEIGHT
                  <i class="bi" :class="sectionOpen.swingweight ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.swingweight" class="vnb-filter-list">
                  <li v-for="opt in swingweightOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedSwingweight" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Trọng lượng -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('trongLuong')">
                  TRỌNG LƯỢNG
                  <i class="bi" :class="sectionOpen.trongLuong ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.trongLuong" class="vnb-filter-list">
                  <li v-for="opt in trongLuongOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedTrongLuong" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Điểm cân bằng -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('diemCanBang')">
                  ĐIỂM CÂN BẰNG
                  <i class="bi" :class="sectionOpen.diemCanBang ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.diemCanBang" class="vnb-filter-list">
                  <li v-for="opt in diemCanBangOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedDiemCanBang" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Độ cứng đũa -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('doCung')">
                  ĐỘ CỨNG ĐŨA
                  <i class="bi" :class="sectionOpen.doCung ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.doCung" class="vnb-filter-list">
                  <li v-for="opt in doCungOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedDoCung" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Phong cách chơi -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('phongCach')">
                  PHONG CÁCH CHƠI
                  <i class="bi" :class="sectionOpen.phongCach ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.phongCach" class="vnb-filter-list">
                  <li v-for="opt in phongCachOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedPhongCach" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Nội dung chơi -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('noiDung')">
                  NỘI DUNG CHƠI
                  <i class="bi" :class="sectionOpen.noiDung ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.noiDung" class="vnb-filter-list">
                  <li v-for="opt in noiDungOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedNoiDung" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Trình độ chơi -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('trinhDo')">
                  TRÌNH ĐỘ CHƠI
                  <i class="bi" :class="sectionOpen.trinhDo ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.trinhDo" class="vnb-filter-list">
                  <li v-for="opt in trinhDoOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedTrinhDo" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </template>

            <!-- ============================================== -->
            <!-- BỘ LỌC RIÊNG CHO: GIÀY CẦU LÔNG & PICKLEBALL   -->
            <!-- ============================================== -->
            <template v-if="currentCategory === 'giay-cau-long' || currentCategory === 'giay-pickleball'">
              <!-- Size giày -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('sizeGiay')">
                  SIZE GIÀY
                  <i class="bi" :class="sectionOpen.sizeGiay ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <div v-show="sectionOpen.sizeGiay" class="d-flex flex-wrap gap-1 pt-1">
                  <button
                    v-for="s in sizeGiayOptions"
                    :key="s"
                    type="button"
                    class="btn btn-sm"
                    :class="selectedSizeGiay.includes(s) ? 'btn-primary' : 'btn-outline-secondary'"
                    style="min-width: 42px; font-size: 0.78rem;"
                    @click="selectedSizeGiay.includes(s) ? selectedSizeGiay = selectedSizeGiay.filter(x => x !== s) : selectedSizeGiay.push(s)"
                  >
                    {{ s }}
                  </button>
                </div>
              </div>

              <!-- Form chân -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('formChan')">
                  FORM CHÂN
                  <i class="bi" :class="sectionOpen.formChan ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.formChan" class="vnb-filter-list">
                  <li v-for="opt in formChanOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedFormChan" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Đối tượng -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('doiTuongGiay')">
                  ĐỐI TƯỢNG SỬ DỤNG
                  <i class="bi" :class="sectionOpen.doiTuongGiay ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.doiTuongGiay" class="vnb-filter-list">
                  <li v-for="opt in doiTuongGiayOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedDoiTuongGiay" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Loại đế sân -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('loaiDe')">
                  LOẠI ĐẾ & MẶT SÂN
                  <i class="bi" :class="sectionOpen.loaiDe ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.loaiDe" class="vnb-filter-list">
                  <li v-for="opt in loaiDeOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedLoaiDe" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </template>

            <!-- ============================================== -->
            <!-- BỘ LỌC RIÊNG CHO: TRANG PHỤC (ÁO, VÁY, QUẦN)   -->
            <!-- ============================================== -->
            <template v-if="currentCategory === 'ao-cau-long' || currentCategory === 'vay-cau-long' || currentCategory === 'quan-cau-long'">
              <!-- Size -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('sizeTrangPhuc')">
                  SIZE TRANG PHỤC
                  <i class="bi" :class="sectionOpen.sizeTrangPhuc ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <div v-show="sectionOpen.sizeTrangPhuc" class="d-flex flex-wrap gap-1 pt-1">
                  <button
                    v-for="s in sizeTrangPhucOptions"
                    :key="s"
                    type="button"
                    class="btn btn-sm"
                    :class="selectedSizeTrangPhuc.includes(s) ? 'btn-primary' : 'btn-outline-secondary'"
                    style="min-width: 44px; font-size: 0.78rem;"
                    @click="selectedSizeTrangPhuc.includes(s) ? selectedSizeTrangPhuc = selectedSizeTrangPhuc.filter(x => x !== s) : selectedSizeTrangPhuc.push(s)"
                  >
                    {{ s }}
                  </button>
                </div>
              </div>

              <!-- Giới tính -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('gioiTinh')">
                  DÀNH CHO
                  <i class="bi" :class="sectionOpen.gioiTinh ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.gioiTinh" class="vnb-filter-list">
                  <li v-for="opt in gioiTinhOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedGioiTinh" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Chất liệu -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('chatLieu')">
                  CHẤT LIỆU VẢI
                  <i class="bi" :class="sectionOpen.chatLieu ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.chatLieu" class="vnb-filter-list">
                  <li v-for="opt in chatLieuOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedChatLieu" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Riêng cho Váy -->
              <div v-if="currentCategory === 'vay-cau-long'" class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('kieuDangVay')">
                  KIỂU DÁNG VÁY
                  <i class="bi" :class="sectionOpen.kieuDangVay ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.kieuDangVay" class="vnb-filter-list">
                  <li v-for="opt in kieuDangVayOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedKieuDangVay" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Riêng cho Quần -->
              <div v-if="currentCategory === 'quan-cau-long'" class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('loaiQuan')">
                  LOẠI QUẦN
                  <i class="bi" :class="sectionOpen.loaiQuan ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.loaiQuan" class="vnb-filter-list">
                  <li v-for="opt in loaiQuanOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedLoaiQuan" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </template>

            <!-- ============================================== -->
            <!-- BỘ LỌC RIÊNG CHO: TÚI VỢT & BALO               -->
            <!-- ============================================== -->
            <template v-if="currentCategory === 'tui-vot-cau-long' || currentCategory === 'balo-cau-long'">
              <!-- Số ngăn / Sức chứa -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('soNgan')">
                  SỐ NGĂN & TÍNH NĂNG
                  <i class="bi" :class="sectionOpen.soNgan ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.soNgan" class="vnb-filter-list">
                  <li v-for="opt in soNganOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedSoNgan" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Sức chứa số vợt -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('sucChua')">
                  SỨC CHỨA CÂY VỢT
                  <i class="bi" :class="sectionOpen.sucChua ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.sucChua" class="vnb-filter-list">
                  <li
                    v-for="opt in currentCategory === 'tui-vot-cau-long' ? sucChuaTuiOptions : sucChuaBaloOptions"
                    :key="opt"
                    class="vnb-filter-item"
                  >
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedSucChua" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </template>

            <!-- ============================================== -->
            <!-- BỘ LỌC RIÊNG CHO: PHỤ KIỆN                     -->
            <!-- ============================================== -->
            <template v-if="currentCategory === 'phu-kien-cau-long'">
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('loaiPhuKien')">
                  LOẠI PHỤ KIỆN
                  <i class="bi" :class="sectionOpen.loaiPhuKien ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.loaiPhuKien" class="vnb-filter-list">
                  <li v-for="opt in loaiPhuKienOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedLoaiPhuKien" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </template>

            <!-- ============================================== -->
            <!-- BỘ LỌC RIÊNG CHO: VỢT PICKLEBALL               -->
            <!-- ============================================== -->
            <template v-if="currentCategory === 'vot-pickleball'">
              <!-- Độ dày lõi -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('doDayLoi')">
                  ĐỘ DÀY LÕI VỢT
                  <i class="bi" :class="sectionOpen.doDayLoi ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.doDayLoi" class="vnb-filter-list">
                  <li v-for="opt in doDayLoiOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedDoDayLoi" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Trọng lượng -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('trongLuongPB')">
                  TRỌNG LƯỢNG
                  <i class="bi" :class="sectionOpen.trongLuongPB ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.trongLuongPB" class="vnb-filter-list">
                  <li v-for="opt in trongLuongPBOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedTrongLuongPB" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Bề mặt -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('beMatPB')">
                  BỀ MẶT CÔNG NGHỆ
                  <i class="bi" :class="sectionOpen.beMatPB ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.beMatPB" class="vnb-filter-list">
                  <li v-for="opt in beMatPBOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedBeMatPB" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>

              <!-- Hình dáng thân -->
              <div class="vnb-filter-section">
                <div class="vnb-filter-section-title" @click="toggleSection('hinhDangPB')">
                  DÁNG THÂN VỢT
                  <i class="bi" :class="sectionOpen.hinhDangPB ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
                </div>
                <ul v-show="sectionOpen.hinhDangPB" class="vnb-filter-list">
                  <li v-for="opt in hinhDangPBOptions" :key="opt" class="vnb-filter-item">
                    <label>
                      <input type="checkbox" :value="opt" v-model="selectedHinhDangPB" />
                      <span>{{ opt }}</span>
                    </label>
                  </li>
                </ul>
              </div>
            </template>

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
                    <span class="vnb-product-price">{{ formatMoney(item.minPrice) }}</span>
                    <span v-if="item.originalPrice > item.minPrice" class="vnb-product-old-price">
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
