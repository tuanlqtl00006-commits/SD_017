<script setup>
import { ref } from 'vue'

const applied = ref(false)
const applyForm = ref({
  hoTen: '',
  sdt: '',
  email: '',
  viTri: 'Nhân viên bán hàng Showroom',
  ghiChu: '',
})

const jobs = [
  {
    id: 1,
    title: 'Nhân Viên Tư Vấn & Bán Hàng Showroom',
    location: 'Hà Nội & TP.HCM',
    salary: '8.000.000đ - 12.000.000đ / tháng (Lương cứng + Hoa hồng)',
    type: 'Toàn thời gian (Xoay ca)',
    desc: 'Đón tiếp khách hàng, tư vấn chọn vợt, giày và trang phục cầu lông phù hợp với lối chơi. Quản lý quầy kệ hàng hóa sạch sẽ, bắt mắt.',
    requirements: [
      'Độ tuổi từ 18 - 28 tuổi.',
      'Yêu thích hoặc có kiến thức cơ bản về bộ môn cầu lông là một lợi thế lớn.',
      'Giao tiếp niềm nở, nhiệt tình, có tinh thần trách nhiệm.',
    ],
  },
  {
    id: 2,
    title: 'Kỹ Thuật Viên Đan Vợt Cầu Lông Chuyên Nghiệp',
    location: 'Hà Nội & TP.HCM & Đà Nẵng',
    salary: '10.000.000đ - 15.000.000đ / tháng',
    type: 'Toàn thời gian',
    desc: 'Sử dụng máy căng vợt điện tử Yonex/Victor, căng vợt 4 nút / 2 nút đúng tiêu chuẩn quốc tế, tư vấn mức căng và loại cước cho lông thủ.',
    requirements: [
      'Có kinh nghiệm đan cước vợt tối thiểu 6 tháng.',
      'Cẩn thận, tỉ mỉ, không làm xước khung hoặc sập gen vợt.',
      'Được đào tạo nâng cao chứng chỉ căng vợt chuẩn Yonex.',
    ],
  },
  {
    id: 3,
    title: 'Chuyên Viên Marketing & Quản Trị Fanpage Cầu Lông',
    location: 'Hà Nội (Văn phòng trụ sở)',
    salary: '11.000.000đ - 16.000.000đ / tháng',
    type: 'Toàn thời gian (Thứ 2 - Thứ 6)',
    desc: 'Lên ý tưởng quay video review vợt, giày, tổ chức các giải đấu phong trào và quản lý kênh truyền thông TikTok, Fanpage của FootStyle.',
    requirements: [
      'Có kinh nghiệm làm content/social media từ 1 năm.',
      'Am hiểu cộng đồng cầu lông phong trào Việt Nam.',
      'Biết quay chụp, dựng video ngắn cơ bản là lợi thế.',
    ],
  },
]

function handleSubmit() {
  applied.value = true
}
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Tuyển dụng</li>
      </ol>
    </nav>

    <!-- Header Banner -->
    <div class="p-4 p-md-5 rounded-4 text-white text-center mb-5" style="background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);">
      <span class="badge bg-warning text-dark fw-bold px-3 py-1 rounded-pill mb-2">GIA NHẬP ĐỘI NGŨ FOOTSTYLE</span>
      <h2 class="display-6 fw-black text-uppercase mb-3" style="font-weight: 900;">
        CÙNG FOOTSTYLE LAN TỎA NIỀM ĐAM MÊ CẦU LÔNG
      </h2>
      <p class="lead small opacity-90 mx-auto" style="max-width: 650px;">
        Môi trường làm việc trẻ trung, năng động, thỏa sức thể hiện tình yêu với trái cầu lông cùng chế độ đãi ngộ và cơ hội phát triển nghề nghiệp rộng mở.
      </p>
    </div>

    <!-- Phúc lợi -->
    <div class="row g-3 mb-5">
      <div class="col-md-3 col-6">
        <div class="p-3 bg-white rounded-3 shadow-sm text-center h-100 border">
          <i class="bi bi-cash-coin display-5 text-primary mb-2 d-block"></i>
          <h6 class="fw-bold mb-1">Thu Nhập Hấp Dẫn</h6>
          <small class="text-muted">Lương cứng + Thưởng doanh số không giới hạn</small>
        </div>
      </div>
      <div class="col-md-3 col-6">
        <div class="p-3 bg-white rounded-3 shadow-sm text-center h-100 border">
          <i class="bi bi-tag-fill display-5 text-success mb-2 d-block"></i>
          <h6 class="fw-bold mb-1">Ưu Đãi Nội Bộ</h6>
          <small class="text-muted">Giảm 30% khi mua đồ thể thao & đan vợt miễn phí</small>
        </div>
      </div>
      <div class="col-md-3 col-6">
        <div class="p-3 bg-white rounded-3 shadow-sm text-center h-100 border">
          <i class="bi bi-trophy-fill display-5 text-warning mb-2 d-block"></i>
          <h6 class="fw-bold mb-1">Giao Lưu & Tập Luyện</h6>
          <small class="text-muted">Được hỗ trợ sân bãi và tham gia CLB cầu lông hàng tuần</small>
        </div>
      </div>
      <div class="col-md-3 col-6">
        <div class="p-3 bg-white rounded-3 shadow-sm text-center h-100 border">
          <i class="bi bi-award-fill display-5 text-danger mb-2 d-block"></i>
          <h6 class="fw-bold mb-1">Đào Tạo Chuẩn Hãng</h6>
          <small class="text-muted">Cấp chứng chỉ kỹ thuật đan vợt Yonex / Victor</small>
        </div>
      </div>
    </div>

    <!-- Danh sách vị trí tuyển dụng -->
    <div class="row g-4 mb-5">
      <div class="col-lg-8">
        <h4 class="fw-bold text-dark mb-4">Các Vị Trí Đang Tuyển Dụng</h4>
        <div class="d-flex flex-column gap-3">
          <div
            v-for="job in jobs"
            :key="job.id"
            class="card border-0 shadow-sm rounded-4 p-4 bg-white"
          >
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
              <h5 class="fw-bold text-primary mb-0">{{ job.title }}</h5>
              <span class="badge bg-danger-subtle text-danger fw-bold px-3 py-1 rounded-pill small">
                {{ job.salary }}
              </span>
            </div>

            <div class="d-flex gap-3 text-muted small mb-3">
              <span><i class="bi bi-geo-alt-fill text-danger me-1"></i>{{ job.location }}</span>
              <span><i class="bi bi-clock-fill text-primary me-1"></i>{{ job.type }}</span>
            </div>

            <p class="small text-secondary mb-3">{{ job.desc }}</p>

            <div class="small mb-3">
              <strong>Yêu cầu cơ bản:</strong>
              <ul class="mb-0 mt-1 ps-3 text-secondary">
                <li v-for="(req, i) in job.requirements" :key="i">{{ req }}</li>
              </ul>
            </div>

            <a
              href="#form-ung-tuyen"
              class="btn btn-outline-primary btn-sm rounded-pill fw-bold align-self-start px-3"
              @click="applyForm.viTri = job.title"
            >
              Ứng tuyển vị trí này <i class="bi bi-arrow-right ms-1"></i>
            </a>
          </div>
        </div>
      </div>

      <!-- Form Nộp Hồ Sơ Ứng Tuyển Nhanh -->
      <div class="col-lg-4" id="form-ung-tuyen">
        <div class="card border-0 shadow-sm rounded-4 p-4 bg-white sticky-top" style="top: 20px;">
          <h5 class="fw-bold text-dark mb-3">Ứng Tuyển Trực Tuyến</h5>

          <div v-if="applied" class="alert alert-success rounded-3 small">
            <i class="bi bi-check-circle-fill me-1"></i>
            Cảm ơn bạn! FootStyle đã nhận được hồ sơ của bạn. Bộ phận nhân sự sẽ liên hệ phỏng vấn trong 24h tới.
          </div>

          <form v-else @submit.prevent="handleSubmit">
            <div class="mb-3">
              <label class="form-label small fw-bold">Họ và tên *</label>
              <input
                type="text"
                v-model="applyForm.hoTen"
                class="form-control form-control-sm"
                placeholder="Ví dụ: Nguyễn Văn Nam"
                required
              />
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Số điện thoại *</label>
              <input
                type="tel"
                v-model="applyForm.sdt"
                class="form-control form-control-sm"
                placeholder="0987654321"
                required
              />
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Email</label>
              <input
                type="email"
                v-model="applyForm.email"
                class="form-control form-control-sm"
                placeholder="nam.nguyen@gmail.com"
              />
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Vị trí muốn ứng tuyển</label>
              <select v-model="applyForm.viTri" class="form-select form-select-sm">
                <option v-for="job in jobs" :key="job.id" :value="job.title">{{ job.title }}</option>
              </select>
            </div>

            <div class="mb-3">
              <label class="form-label small fw-bold">Giới thiệu ngắn / Kinh nghiệm</label>
              <textarea
                v-model="applyForm.ghiChu"
                class="form-control form-control-sm"
                rows="3"
                placeholder="Chia sẻ về sở thích cầu lông hoặc kinh nghiệm của bạn..."
              ></textarea>
            </div>

            <button type="submit" class="btn btn-primary w-100 rounded-pill fw-bold">
              GỬI HỒ SƠ ỨNG TUYỂN
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
