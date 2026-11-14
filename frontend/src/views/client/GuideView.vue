<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { dungCaiDatVanChuyen } from '../../services/caiDatVanChuyen'
import { NHOM_PHI } from '../../utils/phiVanChuyenTinh'
import { formatMoney } from '../../utils/format'

const route = useRoute()
// Chính sách giao hàng hiện theo cài đặt thật: ngưỡng miễn phí vận chuyển của quản lý + bảng phí theo tỉnh
const caiDatShip = dungCaiDatVanChuyen()
const cacMucPhi = NHOM_PHI.map(([phi]) => phi).filter((phi) => phi > 0)
const phiThapNhat = Math.min(...cacMucPhi)
const phiCaoNhat = Math.max(...cacMucPhi)
const currentTab = ref('thanh-toan') // 'thanh-toan' | 'mua-hang' | 'chon-vot' | 'cong-nghe' | 'bao-hanh'

function syncTab() {
  if (route.query.tab) {
    currentTab.value = String(route.query.tab)
  }
}

onMounted(() => {
  syncTab()
})

watch(
  () => route.query.tab,
  () => {
    syncTab()
  },
)
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Hướng dẫn & Hỗ trợ</li>
      </ol>
    </nav>

    <!-- Header -->
    <div class="vnb-section-title-wrap mb-4">
      <h2 class="vnb-section-title">Trung Tâm Hướng Dẫn & Hỗ Trợ Khách Hàng</h2>
      <div class="vnb-title-underline"></div>
      <p class="text-muted small mt-2">Tất cả thông tin mua hàng, thanh toán, công nghệ sản phẩm và hướng dẫn chọn dụng cụ chuẩn xác</p>
    </div>

    <!-- Thanh Navigation Tabs Hướng Dẫn -->
    <div class="vnb-cat-tabs mb-4 rounded-3 border">
      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'thanh-toan' }"
        @click="currentTab = 'thanh-toan'"
      >
        <i class="bi bi-credit-card me-1"></i> Hướng Dẫn Thanh Toán
      </button>
      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'mua-hang' }"
        @click="currentTab = 'mua-hang'"
      >
        <i class="bi bi-cart-check me-1"></i> Hướng Dẫn Mua Hàng
      </button>
      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'chon-vot' }"
        @click="currentTab = 'chon-vot'"
      >
        <i class="bi bi-crosshair me-1"></i> Hướng Dẫn Chọn Vợt
      </button>
      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'cong-nghe' }"
        @click="currentTab = 'cong-nghe'"
      >
        <i class="bi bi-cpu me-1"></i> Công Nghệ Sản Phẩm
      </button>

      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'bao-hanh' }"
        @click="currentTab = 'bao-hanh'"
      >
        <i class="bi bi-shield-check me-1"></i> Bảo Hành & Đổi Trả
      </button>
    </div>

    <!-- 1. TAB HƯỚNG DẪN THANH TOÁN -->
    <div v-if="currentTab === 'thanh-toan'" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-4">Các Phương Thức Thanh Toán Tại FootStyle</h4>

      <div class="row g-4 mb-4">
        <!-- COD -->
        <div class="col-md-6">
          <div class="p-4 bg-light rounded-3 h-100 border">
            <div class="d-flex align-items-center gap-2 mb-2">
              <i class="bi bi-cash-stack text-success fs-3"></i>
              <h5 class="fw-bold text-dark mb-0">1. Thanh Toán Khi Nhận Hàng (COD)</h5>
            </div>
            <p class="small text-secondary mb-3">
              Quý khách nhận hàng tại nhà, kiểm tra bao bì và sản phẩm cùng nhân viên giao hàng (đồng kiểm) trước khi tiến hành thanh toán tiền mặt trực tiếp cho bưu tá.
            </p>
            <ul class="small text-muted ps-3 mb-0">
              <li>Áp dụng cho mọi tỉnh thành trên toàn quốc.</li>
              <li>An toàn 100%, không cần tài khoản ngân hàng.</li>
            </ul>
          </div>
        </div>

        <!-- Chuyển khoản QR -->
        <div class="col-md-6">
          <div class="p-4 bg-light rounded-3 h-100 border">
            <div class="d-flex align-items-center gap-2 mb-2">
              <i class="bi bi-qr-code-scan text-primary fs-3"></i>
              <h5 class="fw-bold text-dark mb-0">2. Chuyển Khoản Ngân Hàng (VietQR Tự Động)</h5>
            </div>
            <p class="small text-secondary mb-3">
              Quét mã VietQR chuẩn Napas bằng bất kỳ ứng dụng ngân hàng nào (Vietcombank, MB, Techcombank, VPBank, MoMo,...). Hệ thống tự điền số tài khoản, số tiền và nội dung đơn hàng.
            </p>
            <ul class="small text-muted ps-3 mb-0">
              <li>Xác nhận thanh toán tự động trong 30 giây.</li>
              <li>Miễn phí hoàn toàn phí chuyển khoản liên ngân hàng.</li>
            </ul>
          </div>
        </div>

        <!-- Quẹt thẻ tại quầy -->
        <div class="col-md-6">
          <div class="p-4 bg-light rounded-3 h-100 border">
            <div class="d-flex align-items-center gap-2 mb-2">
              <i class="bi bi-credit-card-2-front text-danger fs-3"></i>
              <h5 class="fw-bold text-dark mb-0">3. Quẹt Thẻ POS Tại Showroom</h5>
            </div>
            <p class="small text-secondary mb-3">
              Hỗ trợ quẹt thẻ Visa, MasterCard, JCB và thẻ ATM nội địa Napas tại tất cả các chi nhánh FootStyle trên toàn quốc.
            </p>
            <ul class="small text-muted ps-3 mb-0">
              <li>Hỗ trợ thanh toán không tiếp xúc (Contactless, Apple Pay, Google Pay).</li>
              <li>Không thu phụ phí quẹt thẻ.</li>
            </ul>
          </div>
        </div>

        <!-- Ví điện tử -->
        <div class="col-md-6">
          <div class="p-4 bg-light rounded-3 h-100 border">
            <div class="d-flex align-items-center gap-2 mb-2">
              <i class="bi bi-wallet2 text-warning fs-3"></i>
              <h5 class="fw-bold text-dark mb-0">4. Ví Điện Tử & Cổng Thanh Toán</h5>
            </div>
            <p class="small text-secondary mb-3">
              Thanh toán dễ dàng qua ví MoMo, VNPay, ZaloPay, ShopeePay với nhiều chương trình hoàn tiền và mã giảm giá độc quyền.
            </p>
            <ul class="small text-muted ps-3 mb-0">
              <li>Tiện lợi, thao tác nhanh chỉ trong 1 chạm trên điện thoại.</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="p-3 bg-primary-subtle rounded-3 text-dark small border-start border-4 border-primary">
        <strong>Lưu ý bảo mật:</strong> FootStyle không bao giờ yêu cầu khách hàng cung cấp mã OTP ngân hàng hoặc mật khẩu cá nhân. Mọi thắc mắc về thanh toán vui lòng liên hệ Hotline: <strong>0977508430</strong>.
      </div>
    </div>

    <!-- 2. TAB HƯỚNG DẪN MUA HÀNG -->
    <div v-else-if="currentTab === 'mua-hang'" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-4">Quy Trình Mua Hàng Online 4 Bước Đơn Giản</h4>

      <div class="row g-4 mb-4">
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100 border">
            <h6 class="fw-bold text-dark">Bước 1: Chọn Sản Phẩm</h6>
            <p class="small text-secondary mb-0">Duyệt tìm sản phẩm qua thanh tìm kiếm hoặc danh mục, chọn phiên bản màu sắc, trọng lượng (3U/4U), chu vi cán (G5) rồi bấm "Thêm vào giỏ".</p>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100 border">
            <h6 class="fw-bold text-dark">Bước 2: Điền Thông Tin Nhận Hàng</h6>
            <p class="small text-secondary mb-0">Vào Giỏ hàng bấm "Thanh toán", điền chính xác họ tên, số điện thoại (10 số) và địa chỉ nhận hàng (Tỉnh/Thành phố, Phường/Xã).</p>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100 border">
            <h6 class="fw-bold text-dark">Bước 3: Chọn Phương Thức Thanh Toán</h6>
            <p class="small text-secondary mb-0">Lựa chọn hình thức thanh toán COD hoặc Chuyển khoản VietQR tự động, nhập mã giảm giá (nếu có) rồi bấm "Đặt hàng".</p>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100 border">
            <h6 class="fw-bold text-dark">Bước 4: Nhận Hàng & Kiểm Tra</h6>
            <p class="small text-secondary mb-0">Đơn hàng được vận chuyển hỏa tốc từ 1-3 ngày. Quý khách được đồng kiểm hàng hóa trước khi thanh toán cho nhân viên bưu tá.</p>
          </div>
        </div>
      </div>

      <div class="p-3 bg-primary-subtle rounded-3 text-dark small border-start border-4 border-primary">
        <!-- Viết theo đúng cách hệ thống tính: ngưỡng miễn phí do quản lý cài đặt, phí theo tỉnh lấy từ bảng phí dùng chung -->
        <strong>Chính sách giao hàng:</strong>
        <template v-if="caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0">
          Miễn phí vận chuyển toàn quốc cho đơn có tiền hàng từ <strong>{{ formatMoney(caiDatShip.mienPhiTu) }}</strong>.
        </template>
        Giao tại Hà Nội miễn phí; các tỉnh / thành khác phí từ {{ formatMoney(phiThapNhat) }} đến {{ formatMoney(phiCaoNhat) }} tùy tỉnh nhận.
      </div>
    </div>

    <!-- 3. TAB HƯỚNG DẪN CHỌN VỢT -->
    <div v-else-if="currentTab === 'chon-vot'" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-4">Cẩm Nang Lựa Chọn Vợt Cầu Lông Cho Lông Thủ</h4>

      <div class="row g-4 mb-4">
        <div class="col-md-4">
          <div class="card h-100 border p-3 rounded-3 bg-light">
            <h6 class="fw-bold text-danger"><i class="bi bi-fire me-1"></i> Lối Chơi Tấn Công (Smash Uy Lực)</h6>
            <p class="small text-secondary mb-2">Đặc điểm: Thích đập cầu ghi điểm, làm chủ nhịp đấu bằng những pha đập cầu cắm sân.</p>
            <ul class="small text-dark mb-0 ps-3">
              <li>Điểm cân bằng: <strong>Nặng đầu (298 - 310mm)</strong></li>
              <li>Trọng lượng: 3U hoặc 4U</li>
              <li>Thân vợt: Cứng (Stiff) hoặc Trung bình</li>
              <li>Dòng gợi ý: <em>Yonex Astrox 88D / 100ZZ, Victor Thruster Ryuga, Lining Axforce 90</em></li>
            </ul>
          </div>
        </div>

        <div class="col-md-4">
          <div class="card h-100 border p-3 rounded-3 bg-light">
            <h6 class="fw-bold text-primary"><i class="bi bi-shield-shaded me-1"></i> Lối Chơi Công Thủ Toàn Diện</h6>
            <p class="small text-secondary mb-2">Đặc điểm: Điều cầu, bỏ nhỏ, thủ cầu chắc chắn và phản tạt bất ngờ chuyển trạng thái.</p>
            <ul class="small text-dark mb-0 ps-3">
              <li>Điểm cân bằng: <strong>Cân bằng (290 - 295mm)</strong></li>
              <li>Trọng lượng: 4U (80-84g)</li>
              <li>Thân vợt: Dẻo vừa (Medium)</li>
              <li>Dòng gợi ý: <em>Yonex Arcsaber 11 Pro, Lining Halbertec 8000, Victor Brave Sword 12</em></li>
            </ul>
          </div>
        </div>

        <div class="col-md-4">
          <div class="card h-100 border p-3 rounded-3 bg-light">
            <h6 class="fw-bold text-success"><i class="bi bi-lightning-charge-fill me-1"></i> Lối Chơi Tốc Độ / Phản Tạt Lưới</h6>
            <p class="small text-secondary mb-2">Đặc điểm: Đánh đôi tốc độ, tạt cầu chụp lưới nhanh chớp nhoáng.</p>
            <ul class="small text-dark mb-0 ps-3">
              <li>Điểm cân bằng: <strong>Nhẹ đầu (280 - 288mm)</strong></li>
              <li>Trọng lượng: 4U hoặc 5U (siêu nhẹ)</li>
              <li>Thân vợt: Dẻo (Flexible)</li>
              <li>Dòng gợi ý: <em>Yonex Nanoflare 800 / 1000Z, Lining Windstorm, Victor Auraspeed</em></li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. TAB CÔNG NGHỆ SẢN PHẨM -->
    <div v-else-if="currentTab === 'cong-nghe'" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-4">Các Công Nghệ Đột Phá Trên Sản Phẩm Cầu Lông</h4>

      <div class="row g-4 mb-4">
        <div class="col-md-6">
          <div class="p-3 bg-light rounded-3 border h-100">
            <h6 class="fw-bold text-primary mb-1">1. Khung Vợt Vuông ISOMETRIC™</h6>
            <p class="small text-secondary mb-0">
              Thiết kế đầu vợt hình vuông độc quyền của Yonex giúp mở rộng vùng điểm ngọt (Sweet Spot) lên thêm 7%, mang lại cảm giác tiếp xúc cầu chắc tay ngay cả khi đánh lệch tâm.
            </p>
          </div>
        </div>

        <div class="col-md-6">
          <div class="p-3 bg-light rounded-3 border h-100">
            <h6 class="fw-bold text-primary mb-1">2. Vật Liệu Carbon Namd & 2G-Namd</h6>
            <p class="small text-secondary mb-0">
              Các sợi carbon nano gắn trực tiếp vào ống nhựa giúp thân vợt linh hoạt uốn cong khi vung vợt và tích lũy năng lượng, sau đó bung mạnh tạo lực smash cầu cắm sân uy lực.
            </p>
          </div>
        </div>

        <div class="col-md-6">
          <div class="p-3 bg-light rounded-3 border h-100">
            <h6 class="fw-bold text-primary mb-1">3. Đũa Vợt Siêu Mỏng Super Slim Shaft</h6>
            <p class="small text-secondary mb-0">
              Thân vợt được thu gọn tối đa đường kính giúp xé gió nhanh hơn, giảm sức cản không khí, tăng tốc độ vung vợt và độ xoay linh hoạt của cổ tay trong các pha cầu tốc độ cao.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. TAB BẢO HÀNH & ĐỔI TRẢ -->
    <div v-else class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-3">Chính Sách Bảo Hành & Đổi Trả Sản Phẩm</h4>

      <div class="mb-4">
        <h6 class="fw-bold text-dark"><i class="bi bi-arrow-repeat text-primary me-1"></i> Chính Sách Đổi Mới 1-1 Trong 7 Ngày:</h6>
        <ul class="small text-secondary ps-3">
          <li>Áp dụng cho mọi sản phẩm còn nguyên tem mác, chưa qua sử dụng trên sân.</li>
          <li>Sản phẩm lỗi do nhà sản xuất (bung keo, xước sơn ban đầu) đổi mới ngay lập tức.</li>
        </ul>
      </div>

      <div class="mb-4">
        <h6 class="fw-bold text-dark"><i class="bi bi-shield-check text-primary me-1"></i> Quy Định Bảo Hành Vợt Cầu Lông:</h6>
        <ul class="small text-secondary ps-3">
          <li>Vợt chính hãng được bảo hành theo đúng chính sách của hãng (Yonex 3 tháng, Victor 3 tháng, Lining 3 tháng).</li>
          <li>Điều kiện bảo hành: Vợt bị gãy nứt tự nhiên do lỗi cấu trúc carbon, còn nguyên tem nhà phân phối, không có vết va chạm cạch vợt với vợt khác.</li>
          <li>Thời gian xử lý bảo hành từ hãng: 7 - 14 ngày làm việc.</li>
        </ul>
      </div>

      <div class="p-3 bg-light rounded-3 text-secondary small">
        Quý khách cần hỗ trợ bảo hành vui lòng mang sản phẩm đến bất kỳ showroom FootStyle hoặc gọi Hotline: <strong>0977508430</strong>.
      </div>
    </div>
  </div>
</template>
