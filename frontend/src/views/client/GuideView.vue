<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentTab = ref('mua-hang') // 'mua-hang' | 'chon-vot' | 'size-giay' | 'bao-hanh'

function syncTab() {
  if (route.query.tab) {
    currentTab.value = String(route.query.tab)
  }
}

onMounted(() => {
  syncTab()
})

watch(() => route.query.tab, () => {
  syncTab()
})
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Hướng dẫn & Chính sách</li>
      </ol>
    </nav>

    <!-- Header -->
    <div class="vnb-section-title-wrap mb-4">
      <h2 class="vnb-section-title">Trung Tâm Hướng Dẫn & Hỗ Trợ Khách Hàng</h2>
      <div class="vnb-title-underline"></div>
      <p class="text-muted small mt-2">Tất cả thông tin mua hàng, hướng dẫn chọn dụng cụ và quy định bảo hành chính hãng</p>
    </div>

    <!-- Thanh Navigation Tabs Hướng Dẫn -->
    <div class="vnb-cat-tabs mb-4 rounded-3 border">
      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'mua-hang' }"
        @click="currentTab = 'mua-hang'"
      >
        <i class="bi bi-cart-check me-1"></i> Mua Hàng & Thanh Toán
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
        :class="{ active: currentTab === 'size-giay' }"
        @click="currentTab = 'size-giay'"
      >
        <i class="bi bi-rulers me-1"></i> Bảng Size Giày Chuẩn
      </button>
      <button
        class="vnb-cat-tab"
        :class="{ active: currentTab === 'bao-hanh' }"
        @click="currentTab = 'bao-hanh'"
      >
        <i class="bi bi-shield-check me-1"></i> Chính Sách Bảo Hành & Đổi Trả
      </button>
    </div>

    <!-- TAB 1: MUA HÀNG & THANH TOÁN -->
    <div v-if="currentTab === 'mua-hang'" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-4">Quy Trình Mua Hàng Online Tại FootStyle</h4>
      
      <div class="row g-4 mb-4">
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <h6 class="fw-bold text-dark">Bước 1: Chọn Sản Phẩm</h6>
            <p class="small text-secondary mb-0">Tìm kiếm sản phẩm mong muốn qua thanh tìm kiếm hoặc danh mục, chọn màu sắc, trọng lượng (3U/4U), chu vi cán (G5) rồi bấm "Thêm vào giỏ".</p>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <h6 class="fw-bold text-dark">Bước 2: Điền Thông Tin Nhận Hàng</h6>
            <p class="small text-secondary mb-0">Vào Giỏ hàng bấm "Thanh toán", điền chính xác họ tên, số điện thoại (10 số) và địa chỉ nhận hàng (Tỉnh/Thành phố, Phường/Xã).</p>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <h6 class="fw-bold text-dark">Bước 3: Chọn Phương Thức Thanh Toán</h6>
            <p class="small text-secondary mb-0">Hỗ trợ 2 phương thức tiện lợi: Thanh toán tiền mặt khi nhận hàng (COD) hoặc Chuyển khoản quét mã VietQR tự động.</p>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <h6 class="fw-bold text-dark">Bước 4: Nhận Hàng & Kiểm Tra</h6>
            <p class="small text-secondary mb-0">Đơn hàng được vận chuyển hỏa tốc từ 1-3 ngày. Quý khách được đồng kiểm hàng hóa trước khi thanh toán cho nhân viên bưu tá.</p>
          </div>
        </div>
      </div>

      <div class="p-3 bg-primary-subtle rounded-3 text-dark small border-start border-4 border-primary">
        <strong>Chính sách giao hàng:</strong> Miễn phí vận chuyển toàn quốc cho tất cả đơn hàng từ <strong>500.000₫</strong>. Với các đơn hàng dưới 500.000₫, phí ship đồng giá 30.000₫.
      </div>
    </div>

    <!-- TAB 2: HƯỚNG DẪN CHỌN VỢT -->
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

    <!-- TAB 3: BẢNG SIZE GIÀY CHUẨN -->
    <div v-else-if="currentTab === 'size-giay'" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-3">Bảng Quy Đổi Size Giày Cầu Lông (Yonex, Lining, Victor)</h4>
      <p class="small text-secondary mb-4">
        <em>Mẹo chọn size:</em> Đo chiều dài bàn chân từ gót chân đến đầu ngón chân dài nhất (cm), sau đó <strong>cộng thêm 0.5cm - 1.0cm</strong> để đi tất vớ thể thao dày và chống cấn ngón chân khi dậm nhảy!
      </p>

      <div class="table-responsive">
        <table class="table table-bordered table-striped text-center align-middle small">
          <thead class="table-primary">
            <tr>
              <th>Chiều dài bàn chân (cm)</th>
              <th>Size EU (Việt Nam)</th>
              <th>Size US (Nam)</th>
              <th>Size UK</th>
              <th>Khuyến nghị</th>
            </tr>
          </thead>
          <tbody>
            <tr><td>24.0 cm</td><td><strong>38</strong></td><td>6.0</td><td>5.5</td><td>Chân nữ hoặc chân nam nhỏ</td></tr>
            <tr><td>24.5 cm</td><td><strong>39</strong></td><td>6.5</td><td>6.0</td><td>Phù hợp chân 23.5 - 24.0 cm</td></tr>
            <tr><td>25.0 cm</td><td><strong>40</strong></td><td>7.5</td><td>7.0</td><td>Phù hợp chân 24.0 - 24.5 cm</td></tr>
            <tr><td>25.5 cm</td><td><strong>41</strong></td><td>8.0</td><td>7.5</td><td>Size phổ biến nam</td></tr>
            <tr><td>26.0 cm</td><td><strong>42</strong></td><td>8.5</td><td>8.0</td><td>Size phổ biến nam</td></tr>
            <tr><td>26.5 cm</td><td><strong>43</strong></td><td>9.5</td><td>9.0</td><td>Phù hợp chân 25.5 - 26.0 cm</td></tr>
            <tr><td>27.0 cm</td><td><strong>44</strong></td><td>10.0</td><td>9.5</td><td>Chân to, mu bàn chân dày</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- TAB 4: BẢO HÀNH & ĐỔI TRẢ -->
    <div v-else class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-3">Chính Sách Bảo Hành & Đổi Trả Sản Phẩm</h4>

      <div class="mb-4">
        <h6 class="fw-bold text-dark"><i class="bi bi-arrow-repeat text-danger me-1"></i> Chính Sách Đổi Mới 1-1 Trong 7 Ngày:</h6>
        <ul class="small text-secondary ps-3">
          <li>Áp dụng cho mọi sản phẩm còn nguyên tem mác, chưa qua sử dụng trên sân.</li>
          <li>Đổi size giày, đổi mẫu áo nếu không vừa kích thước hoàn toàn miễn phí.</li>
          <li>Sản phẩm lỗi do nhà sản xuất (bung keo, xước sơn ban đầu, lỗi may mặc) đổi mới ngay lập tức.</li>
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
