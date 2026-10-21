<script setup>
import { ref } from 'vue'

const sent = ref(false)
const contactForm = ref({
  hoTen: '',
  sdt: '',
  email: '',
  chuDe: 'Tư vấn chọn vợt / giày',
  noiDung: '',
})

const branches = [
  {
    name: 'FootStyle Super Center Hà Nội',
    address: '123 Đường Cầu Diễn, P. Phúc Diễn, Q. Bắc Từ Liêm, Hà Nội',
    phone: '0977 508 430',
    time: '8:00 - 21:30 (Mở cửa tất cả các ngày)',
  },
  {
    name: 'FootStyle Chi Nhánh Cầu Giấy',
    address: '45 Đường Xuân Thủy, P. Dịch Vọng Hậu, Q. Cầu Giấy, Hà Nội',
    phone: '0968 123 456',
    time: '8:00 - 21:30 (Mở cửa tất cả các ngày)',
  },
  {
    name: 'FootStyle Showroom Quận 10 - TP.HCM',
    address: '280 Đường 3 Tháng 2, Phường 12, Quận 10, TP. Hồ Chí Minh',
    phone: '0909 888 999',
    time: '8:00 - 21:30 (Mở cửa tất cả các ngày)',
  },
  {
    name: 'FootStyle Chi Nhánh Đà Nẵng',
    address: '56 Đường Nguyễn Văn Linh, P. Nam Dương, Q. Hải Châu, Đà Nẵng',
    phone: '0912 333 444',
    time: '8:00 - 21:00 (Mở cửa tất cả các ngày)',
  },
]

function handleSendContact() {
  sent.value = true
}
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Liên hệ & Hệ thống cửa hàng</li>
      </ol>
    </nav>

    <!-- Header -->
    <div class="vnb-section-title-wrap mb-4">
      <h2 class="vnb-section-title">Liên Hệ & Hệ Thống Showroom FootStyle</h2>
      <div class="vnb-title-underline"></div>
      <p class="text-muted small mt-2">Hân hạnh phục vụ và giải đáp mọi thắc mắc của quý khách hàng</p>
    </div>

    <div class="row g-4 mb-5">
      <!-- Cột Trái: Thông Tin & Form Gửi Tin Nhắn -->
      <div class="col-lg-6">
        <div class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white h-100">
          <h4 class="fw-bold text-dark mb-4">Gửi Tin Nhắn Cho Chúng Tôi</h4>

          <div v-if="sent" class="alert alert-success rounded-4 p-4 text-center">
            <i class="bi bi-check-circle-fill display-4 text-success d-block mb-2"></i>
            <h5 class="fw-bold text-dark">Tin nhắn đã được gửi!</h5>
            <p class="small text-secondary mb-0">Cảm ơn bạn đã liên hệ với FootStyle. Chúng tôi sẽ phản hồi lại bạn sớm nhất có thể.</p>
          </div>

          <form v-else @submit.prevent="handleSendContact">
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold">Họ và tên *</label>
                <input type="text" v-model="contactForm.hoTen" class="form-control" placeholder="Nguyễn Văn A" required />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Số điện thoại *</label>
                <input type="tel" v-model="contactForm.sdt" class="form-control" placeholder="0977508430" required />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Email</label>
              <input type="email" v-model="contactForm.email" class="form-control" placeholder="email@gmail.com" />
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Chủ đề cần hỗ trợ</label>
              <select v-model="contactForm.chuDe" class="form-select">
                <option value="Tư vấn chọn vợt / giày">Tư vấn chọn vợt / giày cầu lông</option>
                <option value="Hỏi về tiến độ đơn hàng">Hỏi về tiến độ đơn hàng</option>
                <option value="Hỗ trợ bảo hành / đổi trả">Hỗ trợ bảo hành / đổi trả</option>
                <option value="Hợp tác nhượng quyền / đại lý">Hợp tác nhượng quyền / đại lý</option>
                <option value="Ý kiến đóng góp khác">Ý kiến đóng góp khác</option>
              </select>
            </div>

            <div class="mb-4">
              <label class="form-label small fw-bold">Nội dung tin nhắn *</label>
              <textarea v-model="contactForm.noiDung" class="form-control" rows="4" placeholder="Nhập câu hỏi hoặc yêu cầu cần giải đáp..." required></textarea>
            </div>

            <button type="submit" class="btn btn-primary btn-lg w-100 rounded-pill fw-bold">
              GỬI TIN NHẮN NGAY
            </button>
          </form>
        </div>
      </div>

      <!-- Cột Phải: Thông Tin Hotline Trụ Sở -->
      <div class="col-lg-6">
        <div class="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white h-100 d-flex flex-column justify-content-between">
          <div>
            <h4 class="fw-bold text-dark mb-4">Thông Tin Trụ Sở Chính</h4>

            <div class="d-flex align-items-start gap-3 mb-4">
              <div class="p-3 bg-primary-subtle text-primary rounded-circle">
                <i class="bi bi-geo-alt-fill fs-4"></i>
              </div>
              <div>
                <h6 class="fw-bold text-dark mb-1">Địa chỉ trụ sở:</h6>
                <p class="text-secondary small mb-0">123 Đường Cầu Diễn, Phường Phúc Diễn, Quận Bắc Từ Liêm, Hà Nội</p>
              </div>
            </div>

            <div class="d-flex align-items-start gap-3 mb-4">
              <div class="p-3 bg-danger-subtle text-danger rounded-circle">
                <i class="bi bi-telephone-fill fs-4"></i>
              </div>
              <div>
                <h6 class="fw-bold text-dark mb-1">Hotline tư vấn & đặt hàng:</h6>
                <p class="text-secondary small mb-0">
                  <a href="tel:0977508430" class="text-danger fw-bold text-decoration-none fs-6">0977 508 430</a> (8:00 - 21:30 hàng ngày)
                </p>
              </div>
            </div>

            <div class="d-flex align-items-start gap-3 mb-4">
              <div class="p-3 bg-success-subtle text-success rounded-circle">
                <i class="bi bi-envelope-fill fs-4"></i>
              </div>
              <div>
                <h6 class="fw-bold text-dark mb-1">Hòm thư điện tử:</h6>
                <p class="text-secondary small mb-0">cskh@footstyle.vn (Hỗ trợ 24/7)</p>
              </div>
            </div>
          </div>

          <div class="p-3 bg-light rounded-3 text-secondary small border-start border-4 border-primary">
            Quý khách có thể ghé trực tiếp bất kỳ cửa hàng FootStyle trong hệ thống để trải nghiệm vợt, thử giày và đan cước lấy ngay trong 20 phút!
          </div>
        </div>
      </div>
    </div>

    <!-- Hệ Thống Chi Nhánh Cửa Hàng -->
    <div class="mb-5">
      <h4 class="fw-bold text-dark mb-4 text-center text-uppercase">Hệ Thống Cửa Hàng Showroom Toàn Quốc</h4>
      <div class="row g-4">
        <div
          v-for="(store, idx) in branches"
          :key="idx"
          class="col-md-6"
        >
          <div class="card border-0 shadow-sm rounded-4 p-4 bg-white h-100">
            <h5 class="fw-bold text-primary mb-2">
              <i class="bi bi-shop me-2"></i>{{ store.name }}
            </h5>
            <p class="small text-secondary mb-2">
              <i class="bi bi-geo-alt-fill text-danger me-1"></i>{{ store.address }}
            </p>
            <p class="small text-secondary mb-2">
              <i class="bi bi-telephone-fill text-success me-1"></i>Hotline: <strong>{{ store.phone }}</strong>
            </p>
            <p class="small text-muted mb-0">
              <i class="bi bi-clock-fill text-warning me-1"></i>Giờ mở cửa: {{ store.time }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
