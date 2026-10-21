<script setup>
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Giới thiệu về FootStyle</li>
      </ol>
    </nav>

    <!-- Hero Header -->
    <div class="p-4 p-md-5 rounded-4 text-white text-center mb-5" style="background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);">
      <span class="badge bg-warning text-dark fw-bold px-3 py-1 rounded-pill mb-2">HỆ THỐNG CỬA HÀNG CẦU LÔNG TOÀN QUỐC</span>
      <h2 class="display-6 fw-black text-uppercase mb-3" style="font-weight: 900;">
        VỀ CHÚNG TÔI - FOOTSTYLE BADMINTON
      </h2>
      <p class="lead small opacity-90 mx-auto" style="max-width: 720px;">
        Đồng hành cùng hàng triệu lông thủ Việt Nam từ năm 2018. Chúng tôi tự hào là đại lý phân phối chính thức các thương hiệu vợt, giày và trang bị thể thao cầu lông hàng đầu thế giới.
      </p>
    </div>

    <!-- 3 Cột Sứ Mệnh & Giá Trị -->
    <div class="row g-4 mb-5">
      <div class="col-md-4">
        <div class="card border-0 shadow-sm rounded-4 p-4 text-center h-100 bg-white">
          <div class="display-4 text-primary mb-3"><i class="bi bi-bullseye"></i></div>
          <h5 class="fw-bold mb-2">Sứ Mệnh Của Chúng Tôi</h5>
          <p class="small text-secondary mb-0">
            Mang đến cho người chơi phong trào và VĐV chuyên nghiệp các trang thiết bị thể thao đạt chuẩn thi đấu quốc tế với mức giá dễ tiếp cận nhất.
          </p>
        </div>
      </div>

      <div class="col-md-4">
        <div class="card border-0 shadow-sm rounded-4 p-4 text-center h-100 bg-white">
          <div class="display-4 text-success mb-3"><i class="bi bi-patch-check-fill"></i></div>
          <h5 class="fw-bold mb-2">Cam Kết 100% Chính Hãng</h5>
          <p class="small text-secondary mb-0">
            Tất cả sản phẩm tại FootStyle đều có hóa đơn VAT, tem chống giả cào mã chính hãng từ nhà phân phối Yonex Sunrise, Lining, Victor.
          </p>
        </div>
      </div>

      <div class="col-md-4">
        <div class="card border-0 shadow-sm rounded-4 p-4 text-center h-100 bg-white">
          <div class="display-4 text-warning mb-3"><i class="bi bi-tools"></i></div>
          <h5 class="fw-bold mb-2">Dịch Vụ Căng Vợt Chuẩn Hãng</h5>
          <p class="small text-secondary mb-0">
            Hệ thống máy căng điện tử hiện đại và kỹ thuật viên đan vợt 4 nút theo quy chuẩn khắt khe của giải đấu thế giới BWF.
          </p>
        </div>
      </div>
    </div>

    <!-- Thông Tin Hệ Thống & Con Số Biết Nói -->
    <div class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <div class="row g-4 text-center">
        <div class="col-md-3 col-6">
          <div class="display-5 fw-black text-primary mb-1" style="font-weight: 900;">10+</div>
          <div class="small fw-bold text-dark text-uppercase">Cửa Hàng Toàn Quốc</div>
        </div>
        <div class="col-md-3 col-6">
          <div class="display-5 fw-black text-danger mb-1" style="font-weight: 900;">50.000+</div>
          <div class="small fw-bold text-dark text-uppercase">Khách Hàng Thân Thiết</div>
        </div>
        <div class="col-md-3 col-6">
          <div class="display-5 fw-black text-success mb-1" style="font-weight: 900;">100%</div>
          <div class="small fw-bold text-dark text-uppercase">Chính Hãng Bảo Hành</div>
        </div>
        <div class="col-md-3 col-6">
          <div class="display-5 fw-black text-warning mb-1" style="font-weight: 900;">24/7</div>
          <div class="small fw-bold text-dark text-uppercase">Tư Vấn Tận Tâm</div>
        </div>
      </div>
    </div>

    <!-- CTA -->
    <div class="text-center">
      <RouterLink to="/shop/san-pham" class="btn btn-primary btn-lg rounded-pill px-5 fw-bold shadow">
        KHÁM PHÁ CỬA HÀNG NGAY <i class="bi bi-arrow-right ms-1"></i>
      </RouterLink>
    </div>
  </div>
</template>
