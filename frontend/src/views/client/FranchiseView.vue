<script setup>
import { ref } from 'vue'

const sent = ref(false)
const franchiseForm = ref({
  hoTen: '',
  sdt: '',
  email: '',
  khuVuc: '',
  vonDuKien: '500 triệu - 1 tỷ',
  ghiChu: '',
})

function submitFranchise() {
  sent.value = true
}
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Chính sách nhượng quyền</li>
      </ol>
    </nav>

    <!-- Hero Header -->
    <div class="p-4 p-md-5 rounded-4 text-white text-center mb-5" style="background: linear-gradient(135deg, #1e3a8a 0%, #0d9488 100%);">
      <span class="badge bg-warning text-dark fw-bold px-3 py-1 rounded-pill mb-2">HỢP TÁC KINH DOANH 2026</span>
      <h2 class="display-6 fw-black text-uppercase mb-3" style="font-weight: 900;">
        CHÍNH SÁCH NHƯỢNG QUYỀN SHOP CẦU LÔNG FOOTSTYLE
      </h2>
      <p class="lead small opacity-90 mx-auto" style="max-width: 700px;">
        Mô hình kinh doanh thể thao cầu lông siêu lợi nhuận, thu hồi vốn nhanh, đồng hành cùng thương hiệu phân phối thiết bị cầu lông uy tín hàng đầu Việt Nam.
      </p>
    </div>

    <!-- 4 Lợi thế nhượng quyền -->
    <div class="row g-4 mb-5">
      <div class="col-lg-3 col-sm-6">
        <div class="p-4 bg-white rounded-4 shadow-sm border text-center h-100">
          <div class="display-4 text-primary mb-3"><i class="bi bi-shield-check"></i></div>
          <h5 class="fw-bold text-dark mb-2">Nguồn Hàng Chính Hãng 100%</h5>
          <p class="small text-secondary mb-0">Nhập trực tiếp từ các hãng Yonex, Victor, Lining với chiết khấu đại lý cấp 1 cao nhất thị trường.</p>
        </div>
      </div>

      <div class="col-lg-3 col-sm-6">
        <div class="p-4 bg-white rounded-4 shadow-sm border text-center h-100">
          <div class="display-4 text-success mb-3"><i class="bi bi-shop-window"></i></div>
          <h5 class="fw-bold text-dark mb-2">Hỗ Trợ Thiết Kế & Setup Showroom</h5>
          <p class="small text-secondary mb-0">Cung cấp trọn gói bản vẽ 3D, hệ giá kệ trưng bày, máy căng vợt điện tử cao cấp và biển hiệu nhận diện đồng bộ.</p>
        </div>
      </div>

      <div class="col-lg-3 col-sm-6">
        <div class="p-4 bg-white rounded-4 shadow-sm border text-center h-100">
          <div class="display-4 text-warning mb-3"><i class="bi bi-mortarboard-fill"></i></div>
          <h5 class="fw-bold text-dark mb-2">Đào Tạo Thợ Căng Vợt & Bán Hàng</h5>
          <p class="small text-secondary mb-0">Đào tạo nhân viên kỹ thuật đan vợt 4 nút chuẩn thế giới, chuyển giao phần mềm quản lý kho và bán hàng POS hiện đại.</p>
        </div>
      </div>

      <div class="col-lg-3 col-sm-6">
        <div class="p-4 bg-white rounded-4 shadow-sm border text-center h-100">
          <div class="display-4 text-danger mb-3"><i class="bi bi-megaphone-fill"></i></div>
          <h5 class="fw-bold text-dark mb-2">Marketing & Đẩy Đơn Toàn Khu Vực</h5>
          <p class="small text-secondary mb-0">FootStyle hỗ trợ chạy quảng cáo khai trương, tài trợ giải đấu phong trào tại địa phương và định tuyến khách hàng lân cận.</p>
        </div>
      </div>
    </div>

    <!-- Quy trình 4 bước -->
    <div class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white mb-5">
      <h4 class="fw-bold text-primary mb-4 text-center text-uppercase">Quy Trình Gia Nhập Hệ Thống Nhượng Quyền</h4>
      <div class="row g-4 text-center">
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <span class="badge bg-primary fs-6 rounded-circle p-2 px-3 mb-2">1</span>
            <h6 class="fw-bold text-dark mb-1">Đăng Ký Tư Vấn</h6>
            <small class="text-secondary">Điền thông tin vào form liên hệ bên dưới để nhận hồ sơ năng lực và bảng phân tích doanh thu chi tiết.</small>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <span class="badge bg-primary fs-6 rounded-circle p-2 px-3 mb-2">2</span>
            <h6 class="fw-bold text-dark mb-1">Khảo Sát Mặt Bằng</h6>
            <small class="text-secondary">Đội ngũ chuyên gia FootStyle trực tiếp khảo sát vị trí, lưu lượng người chơi cầu lông và mật độ sân cầu khu vực.</small>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <span class="badge bg-primary fs-6 rounded-circle p-2 px-3 mb-2">3</span>
            <h6 class="fw-bold text-dark mb-1">Ký Hợp Đồng & Thi Công</h6>
            <small class="text-secondary">Hoàn thiện thủ tục pháp lý, thi công bảng hiệu, setup nội thất và nhập kho lô hàng đầu tiên trong 15-20 ngày.</small>
          </div>
        </div>
        <div class="col-md-3">
          <div class="p-3 bg-light rounded-3 h-100">
            <span class="badge bg-primary fs-6 rounded-circle p-2 px-3 mb-2">4</span>
            <h6 class="fw-bold text-dark mb-1">Khai Trương & Bán Hàng</h6>
            <small class="text-secondary">Chạy chiến dịch truyền thông khai trương tưng bừng với sự hỗ trợ của đội ngũ chuyên gia FootStyle.</small>
          </div>
        </div>
      </div>
    </div>

    <!-- Form Đăng Ký Hợp Tác -->
    <div class="row justify-content-center">
      <div class="col-lg-8">
        <div class="card border-0 shadow-lg rounded-4 p-4 p-md-5 bg-white">
          <h4 class="fw-bold text-dark text-center mb-2">Đăng Ký Nhận Hồ Sơ Nhượng Quyền</h4>
          <p class="text-muted small text-center mb-4">Chúng tôi sẽ bảo mật thông tin và cử chuyên viên liên hệ giải đáp trong vòng 24 giờ</p>

          <div v-if="sent" class="alert alert-success rounded-4 p-4 text-center">
            <i class="bi bi-check-circle-fill display-4 text-success d-block mb-2"></i>
            <h5 class="fw-bold text-dark">Gửi thông tin thành công!</h5>
            <p class="small text-secondary mb-0">Ban Phát Triển Hệ Thống FootStyle sẽ chủ động liên hệ qua số điện thoại của bạn để trao đổi chi tiết.</p>
          </div>

          <form v-else @submit.prevent="submitFranchise">
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold">Họ và tên đối tác *</label>
                <input type="text" v-model="franchiseForm.hoTen" class="form-control" placeholder="Nguyễn Văn A" required />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Số điện thoại liên hệ *</label>
                <input type="tel" v-model="franchiseForm.sdt" class="form-control" placeholder="0987654321" required />
              </div>
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold">Email</label>
                <input type="email" v-model="franchiseForm.email" class="form-control" placeholder="doitac@gmail.com" />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Khu vực dự kiến mở quán *</label>
                <input type="text" v-model="franchiseForm.khuVuc" class="form-control" placeholder="Tỉnh / Thành phố dự kiến mở" required />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Vốn đầu tư dự kiến</label>
              <select v-model="franchiseForm.vonDuKien" class="form-select">
                <option value="300 triệu - 500 triệu">300 triệu - 500 triệu</option>
                <option value="500 triệu - 1 tỷ">500 triệu - 1 tỷ</option>
                <option value="Trên 1 tỷ">Trên 1 tỷ</option>
              </select>
            </div>

            <div class="mb-4">
              <label class="form-label small fw-bold">Ghi chú thêm (nếu có)</label>
              <textarea v-model="franchiseForm.ghiChu" class="form-control" rows="3" placeholder="Đã có sẵn mặt bằng hoặc mong muốn thời gian triển khai..."></textarea>
            </div>

            <button type="submit" class="btn btn-primary btn-lg w-100 rounded-pill fw-bold">
              GỬI YÊU CẦU TƯ VẤN NHƯỢNG QUYỀN
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
