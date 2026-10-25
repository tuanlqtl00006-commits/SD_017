<script setup>
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'

// Danh sách tin tuyển dụng thực tế bám sát 100% hình ảnh mẫu
const allJobs = ref([
  {
    id: 1,
    title: '🔥🔥 Tuyển Dụng FootStyle SPORT 🔥🔥',
    viTri: 'Nhân viên Bán Hàng',
    mucLuong: 'Thỏa Thuận (8.000.000đ - 12.000.000đ)',
    diaDiem: 'Số 33 Đốc Ngữ, Ba Đình, TP. Hà Nội',
    donVi: 'FootStyle Ba Đình',
    ngayDang: '21/10/2026',
    image: 'https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?w=700&auto=format&fit=crop&q=80',
    hinhThuc: 'Toàn thời gian (Xoay ca 8 tiếng)',
    moTa: 'Tư vấn bán hàng các sản phẩm vợt, giày và phụ kiện cầu lông chính hãng cho khách ghé showroom. Sắp xếp, kiểm kê hàng hóa trên quầy kệ gọn gàng, bắt mắt.',
    yeuCau: [
      'Nam/Nữ từ đủ 18 đến 28 tuổi.',
      'Yêu thích hoặc có hiểu biết cơ bản về môn cầu lông là một lợi thế lớn.',
      'Giao tiếp niềm nở, trung thực, có tinh thần trách nhiệm với công việc.',
    ],
    quyenLoi: [
      'Lương cứng + Thưởng hoa hồng doanh số không giới hạn.',
      'Được mua đồ thể thao với giá ưu đãi nhân viên (giảm 30%).',
      'Được hỗ trợ sân cầu lông tập luyện hàng tuần.',
    ],
  },
  {
    id: 2,
    title: '🔥🔥 Tuyển Dụng FootStyle SPORT 🔥🔥',
    viTri: 'Nhân viên bán hàng',
    mucLuong: 'Thỏa thuận (8.000.000đ - 11.500.000đ)',
    diaDiem: '92 Nguyễn Hữu Thọ - Định Công - Hà Nội',
    donVi: 'FootStyle Hoàng Mai',
    ngayDang: '21/10/2026',
    image: 'https://images.unsplash.com/photo-1528698827591-e19ccd7bc23d?w=700&auto=format&fit=crop&q=80',
    hinhThuc: 'Toàn thời gian hoặc Bán thời gian linh hoạt',
    moTa: 'Đón tiếp và hỗ trợ khách hàng trải nghiệm sản phẩm vợt Yonex, Lining, Victor. Hướng dẫn khách thanh toán và giải đáp thắc mắc về chính sách bảo hành.',
    yeuCau: [
      'Nhanh nhẹn, thân thiện, kỹ năng giao tiếp tốt.',
      'Không yêu cầu kinh nghiệm, được đào tạo kiến thức sản phẩm từ đầu.',
      'Ưu tiên ứng viên có thể làm việc vào các ngày cuối tuần hoặc ca tối.',
    ],
    quyenLoi: [
      'Chế độ thưởng tháng 13, thưởng các ngày lễ tết đầy đủ.',
      'Môi trường làm việc năng động, đồng nghiệp trẻ trung đam mê thể thao.',
    ],
  },
  {
    id: 3,
    title: 'FootStyle Thanh Trì Tuyển Dụng',
    viTri: 'Nhân viên bán hàng',
    mucLuong: '8.000.000đ - 12.000.000đ / tháng',
    diaDiem: 'Số 36 đường 70 Yên Xá Tân Triều Thanh Trì Hà Nội (Gần viện 103 Hà Đông)',
    donVi: 'FootStyle Thanh Trì',
    ngayDang: '20/10/2026',
    image: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=700&auto=format&fit=crop&q=80',
    hinhThuc: 'Toàn thời gian',
    moTa: 'Tư vấn bán lẻ trực tiếp tại cửa hàng mới khai trương, hỗ trợ đóng gói và bàn giao đơn hàng online cho đơn vị vận chuyển GHN/GHTK.',
    yeuCau: [
      'Nhiệt tình, có trách nhiệm và chịu khó học hỏi.',
      'Có khả năng gắn bó lâu dài cùng sự phát triển của chi nhánh.',
    ],
    quyenLoi: [
      'Thưởng doanh số chi nhánh vượt chỉ tiêu.',
      'Được đào tạo nâng cao lên vị trí Cửa hàng phó sau 6 tháng.',
    ],
  },
  {
    id: 4,
    title: 'FootStyle Cầu Giấy Tuyển Dụng Nhân Viên Bán Hàng',
    viTri: 'Nhân viên tư vấn bán hàng',
    mucLuong: '8.500.000đ - 13.000.000đ / tháng',
    diaDiem: '112 Cầu Giấy, P. Quan Hoa, Q. Cầu Giấy, Hà Nội',
    donVi: 'FootStyle Cầu Giấy',
    ngayDang: '19/10/2026',
    image: 'https://images.unsplash.com/photo-1567401893414-76b7b1e5a7a5?w=700&auto=format&fit=crop&q=80',
    hinhThuc: 'Toàn thời gian (Ca sáng / Ca chiều tối)',
    moTa: 'Phụ trách khu vực trưng bày vợt cầu lông và giày chuyên dụng. Tư vấn kỹ thuật chọn vợt phù hợp với cổ tay và lối chơi cho khách hàng.',
    yeuCau: [
      'Biết chơi cầu lông hoặc thường xuyên xem thi đấu cầu lông.',
      'Ngoại hình sáng sủa, tác phong nhanh nhẹn, chuyên nghiệp.',
    ],
    quyenLoi: [
      'Lương thưởng hấp dẫn, xét tăng lương định kỳ 6 tháng/lần.',
      'Hỗ trợ ăn ca và phụ cấp gửi xe miễn phí.',
    ],
  },
  {
    id: 5,
    title: 'Tuyển Dụng Kỹ Thuật Viên Đan Vợt Cầu Lông Chuyên Nghiệp',
    viTri: 'Kỹ thuật viên đan cước vợt',
    mucLuong: 'Lương trên 10 Triệu / Tháng (10.000.000đ - 16.000.000đ)',
    diaDiem: 'Hệ thống Showroom FootStyle Hà Nội & TP.HCM',
    donVi: 'FootStyle Kỹ Thuật',
    ngayDang: '18/10/2026',
    image: 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=700&auto=format&fit=crop&q=80',
    hinhThuc: 'Toàn thời gian',
    moTa: 'Sử dụng máy đan cước điện tử hiện đại, thực hiện đan cước vợt 4 nút / 2 nút chuẩn quốc tế Yonex/Victor. Kiểm tra gen vợt và thay gen bảo vệ khung.',
    yeuCau: [
      'Có kinh nghiệm căng cước vợt tối thiểu từ 3 - 6 tháng.',
      'Cẩn thận, tỉ mỉ, không làm xước khung hoặc biến dạng mặt vợt.',
    ],
    quyenLoi: [
      'Thu nhập cao gồm lương cứng + phụ cấp đan cước theo cây.',
      'Được đào tạo cấp chứng chỉ kỹ thuật viên căng cước chuẩn quốc tế.',
    ],
  },
  {
    id: 6,
    title: 'Tuyển Dụng Cửa Hàng Trưởng / Quản Lý Ca Showroom',
    viTri: 'Cửa hàng trưởng / Quản lý ca',
    mucLuong: '12.000.000đ - 18.000.000đ / tháng',
    diaDiem: 'Số 85 Nguyễn Văn Linh, Q. Hải Châu, TP. Đà Nẵng',
    donVi: 'FootStyle Đà Nẵng',
    ngayDang: '16/10/2026',
    image: 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=700&auto=format&fit=crop&q=80',
    hinhThuc: 'Toàn thời gian',
    moTa: 'Quản lý toàn diện hoạt động của showroom: phân ca nhân viên, quản lý tồn kho sản phẩm, theo dõi doanh số và chăm sóc khách hàng VIP.',
    yeuCau: [
      'Có ít nhất 1 năm kinh nghiệm quản lý cửa hàng bán lẻ/thời trang/thể thao.',
      'Kỹ năng lãnh đạo, xử lý tình huống linh hoạt và giải quyết khiếu nại tốt.',
    ],
    quyenLoi: [
      'Lương cứng thỏa thuận + % Doanh số toàn cửa hàng.',
      'Đóng bảo hiểm BHXH, BHYT đầy đủ theo quy định của Nhà nước.',
    ],
  },
])

// Tìm kiếm theo từ khóa
const searchInput = ref('')
const activeKeyword = ref('')

const filteredJobs = computed(() => {
  if (!activeKeyword.value.trim()) return allJobs.value
  const kw = activeKeyword.value.trim().toLowerCase()
  return allJobs.value.filter(
    (j) =>
      j.title.toLowerCase().includes(kw) ||
      j.viTri.toLowerCase().includes(kw) ||
      j.diaDiem.toLowerCase().includes(kw) ||
      j.donVi.toLowerCase().includes(kw),
  )
})

function handleSearch() {
  activeKeyword.value = searchInput.value
}

function clearSearch() {
  searchInput.value = ''
  activeKeyword.value = ''
}

// Modal ứng tuyển & xem chi tiết
const selectedJob = ref(null)
const showModal = ref(false)
const appliedSuccess = ref(false)
const applyForm = ref({
  hoTen: '',
  sdt: '',
  email: '',
  ghiChu: '',
})

function openApplyModal(job) {
  selectedJob.value = job
  appliedSuccess.value = false
  applyForm.value = {
    hoTen: '',
    sdt: '',
    email: '',
    ghiChu: '',
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function submitApplication() {
  appliedSuccess.value = true
  setTimeout(() => {
    // Tự động đóng modal sau 2.5s khi nộp xong
    showModal.value = false
  }, 2500)
}

function onImageError(e) {
  e.target.src =
    'data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22600%22%20height%3D%22400%22%20viewBox%3D%220%200%20600%20400%22%3E%3Crect%20fill%3D%22%23e2e8f0%22%20width%3D%22600%22%20height%3D%22400%22%2F%3E%3Ctext%20fill%3D%22%231d4ed8%22%20font-family%3D%22sans-serif%22%20font-size%3D%2222%22%20font-weight%3D%22bold%22%20x%3D%2250%25%22%20y%3D%2250%25%22%20text-anchor%3D%22middle%22%3EFootStyle%20Tuy%E1%BB%83n%20D%E1%BB%A5ng%3C%2Ftext%3E%3C%2Fsvg%3E'
}
</script>

<template>
  <div class="vnb-container py-3">
    <!-- Breadcrumb nhẹ nhàng -->
    <nav aria-label="breadcrumb" class="mb-2">
      <ol class="breadcrumb small py-1 mb-2">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none text-muted">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active text-dark fw-semibold" aria-current="page">Tuyển dụng</li>
      </ol>
    </nav>

    <!-- 1. Tiêu đề chính dạng: Tuyển dụng (6 tin) -->
    <h1 class="td-page-title mb-3">
      Tuyển dụng ({{ filteredJobs.length }} tin)
    </h1>

    <!-- 2. Thanh tìm kiếm từ khóa nhỏ gọn -->
    <div class="td-search-wrap d-flex align-items-center gap-2 mb-4">
      <div class="input-group td-input-group">
        <input
          type="text"
          v-model="searchInput"
          class="form-control td-search-input"
          placeholder="Nhập từ khóa ..."
          @keyup.enter="handleSearch"
        />
        <button class="btn td-search-btn" type="button" @click="handleSearch">
          <i class="bi bi-search me-1"></i> Tìm kiếm
        </button>
      </div>

      <button
        v-if="activeKeyword"
        class="btn btn-outline-secondary btn-sm flex-shrink-0"
        type="button"
        @click="clearSearch"
        title="Xóa bộ lọc tìm kiếm"
      >
        <i class="bi bi-x-circle me-1"></i> Xóa tìm
      </button>
    </div>

    <!-- Khi không tìm thấy bài viết -->
    <div v-if="filteredJobs.length === 0" class="card border-0 p-5 text-center bg-white rounded-3 shadow-sm text-muted my-4">
      <i class="bi bi-briefcase display-4 text-secondary mb-2"></i>
      <h5 class="fw-bold">Chưa tìm thấy tin tuyển dụng phù hợp</h5>
      <p class="small mb-3">Vui lòng thử tìm kiếm với vị trí hoặc chi nhánh khác.</p>
      <div>
        <button class="btn btn-primary btn-sm rounded-pill px-3" @click="clearSearch">
          Xem tất cả tin tuyển dụng
        </button>
      </div>
    </div>

    <!-- 3. Lưới danh sách tin tuyển dụng (3 cột x 2 hàng chuẩn 100% mẫu) -->
    <div v-else class="row g-3 g-xl-4 td-grid mb-5">
      <div
        v-for="job in filteredJobs"
        :key="job.id"
        class="col-lg-4 col-md-6 d-flex"
      >
        <div class="td-card w-100" @click="openApplyModal(job)">
          <!-- Ảnh thẻ tin tuyển dụng -->
          <div class="td-card-img-wrap">
            <img
              :src="job.image"
              :alt="job.title"
              class="td-card-img"
              loading="lazy"
              @error="onImageError"
            />
          </div>

          <!-- Nội dung chi tiết tin tuyển dụng -->
          <div class="td-card-body d-flex flex-column">
            <!-- Tiêu đề tin -->
            <h2 class="td-card-title" :title="job.title">
              {{ job.title }}
            </h2>

            <!-- Thông tin chi tiết: Vị trí, Lương, Địa điểm, Đơn vị -->
            <ul class="td-info-list list-unstyled m-0 p-0 flex-grow-1">
              <li class="td-info-item">
                <i class="bi bi-briefcase-fill td-icon text-muted" aria-hidden="true"></i>
                <span class="td-label">Vị trí:</span>
                <span class="td-val text-dark fw-medium">{{ job.viTri }}</span>
              </li>
              <li class="td-info-item">
                <i class="bi bi-cash-stack td-icon text-muted" aria-hidden="true"></i>
                <span class="td-label">Mức lương:</span>
                <span class="td-val text-success fw-bold">{{ job.mucLuong }}</span>
              </li>
              <li class="td-info-item">
                <i class="bi bi-geo-alt-fill td-icon text-danger" aria-hidden="true"></i>
                <span class="td-label">Địa điểm:</span>
                <span class="td-val text-dark">{{ job.diaDiem }}</span>
              </li>
              <li class="td-info-item">
                <i class="bi bi-building td-icon text-muted" aria-hidden="true"></i>
                <span class="td-label">Đơn vị:</span>
                <span class="td-val text-dark">{{ job.donVi }}</span>
              </li>
            </ul>

            <!-- Đáy thẻ: Ngày đăng bên trái + Nút Ứng tuyển xanh dương bên phải -->
            <div class="td-card-footer d-flex align-items-center justify-content-between pt-3 mt-2 border-top">
              <span class="td-date text-muted small">{{ job.ngayDang }}</span>
              <button
                type="button"
                class="btn td-btn-apply"
                @click.stop="openApplyModal(job)"
              >
                Ứng tuyển
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. MODAL CHI TIẾT & FORM ỨNG TUYỂN NHANH -->
    <div
      v-if="showModal && selectedJob"
      class="modal fade show d-block td-modal-backdrop"
      tabindex="-1"
      role="dialog"
      aria-modal="true"
      @click.self="closeModal"
    >
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg rounded-3 overflow-hidden">
          <div class="modal-header bg-light border-bottom px-4 py-3">
            <h5 class="modal-title fw-bold text-primary mb-0">
              {{ selectedJob.title }}
            </h5>
            <button
              type="button"
              class="btn-close"
              aria-label="Đóng"
              @click="closeModal"
            ></button>
          </div>

          <div class="modal-body p-4">
            <!-- Thông báo nộp thành công -->
            <div v-if="appliedSuccess" class="alert alert-success rounded-3 p-4 text-center">
              <i class="bi bi-check-circle-fill text-success display-4 d-block mb-2"></i>
              <h5 class="fw-bold text-success">Nộp hồ sơ thành công!</h5>
              <p class="mb-0 text-secondary">
                Cảm ơn bạn đã ứng tuyển vào <strong>{{ selectedJob.donVi }}</strong>. Phòng Nhân sự của FootStyle sẽ liên hệ với bạn trong vòng 24 giờ tới.
              </p>
            </div>

            <!-- Nội dung chi tiết & form -->
            <div v-else class="row g-4">
              <div class="col-lg-7 border-end-lg">
                <h6 class="fw-bold text-dark mb-2">Thông tin công việc</h6>
                <p class="small text-secondary mb-2"><strong>Vị trí:</strong> {{ selectedJob.viTri }}</p>
                <p class="small text-secondary mb-2"><strong>Mức lương:</strong> <span class="text-success fw-bold">{{ selectedJob.mucLuong }}</span></p>
                <p class="small text-secondary mb-2"><strong>Địa điểm làm việc:</strong> {{ selectedJob.diaDiem }}</p>
                <p class="small text-secondary mb-3"><strong>Hình thức:</strong> {{ selectedJob.hinhThuc }}</p>

                <h6 class="fw-bold text-dark mt-3 mb-2">Mô tả công việc</h6>
                <p class="small text-secondary mb-3">{{ selectedJob.moTa }}</p>

                <h6 class="fw-bold text-dark mt-3 mb-2">Yêu cầu công việc</h6>
                <ul class="small text-secondary ps-3 mb-3">
                  <li v-for="(req, idx) in selectedJob.yeuCau" :key="idx">{{ req }}</li>
                </ul>

                <h6 class="fw-bold text-dark mt-3 mb-2">Quyền lợi & Đãi ngộ</h6>
                <ul class="small text-secondary ps-3 mb-0">
                  <li v-for="(bene, idx) in selectedJob.quyenLoi" :key="idx">{{ bene }}</li>
                </ul>
              </div>

              <!-- Form ứng tuyển -->
              <div class="col-lg-5">
                <h6 class="fw-bold text-dark mb-3">Gửi hồ sơ ứng tuyển ngay</h6>
                <form @submit.prevent="submitApplication">
                  <div class="mb-2">
                    <label class="form-label small fw-medium mb-1">Họ và tên <span class="text-danger">*</span></label>
                    <input
                      type="text"
                      v-model="applyForm.hoTen"
                      class="form-control form-control-sm"
                      placeholder="Ví dụ: Nguyễn Văn An"
                      required
                    />
                  </div>

                  <div class="mb-2">
                    <label class="form-label small fw-medium mb-1">Số điện thoại <span class="text-danger">*</span></label>
                    <input
                      type="tel"
                      v-model="applyForm.sdt"
                      class="form-control form-control-sm"
                      placeholder="0987654321"
                      pattern="[0-9]{10}"
                      title="Vui lòng nhập số điện thoại gồm 10 chữ số"
                      required
                    />
                  </div>

                  <div class="mb-2">
                    <label class="form-label small fw-medium mb-1">Email <span class="text-danger">*</span></label>
                    <input
                      type="email"
                      v-model="applyForm.email"
                      class="form-control form-control-sm"
                      placeholder="email@example.com"
                      required
                    />
                  </div>

                  <div class="mb-3">
                    <label class="form-label small fw-medium mb-1">Kinh nghiệm / Lời nhắn</label>
                    <textarea
                      v-model="applyForm.ghiChu"
                      class="form-control form-control-sm"
                      rows="3"
                      placeholder="Chia sẻ ngắn về kinh nghiệm hoặc vị trí ca làm việc bạn mong muốn..."
                    ></textarea>
                  </div>

                  <button type="submit" class="btn btn-primary w-100 fw-bold py-2 rounded-2">
                    NỘP HỒ SƠ ỨNG TUYỂN
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================================
   STYLE TRANG TUYỂN DỤNG CHUẨN MẪU SHOPVNB
   Tone màu chủ đạo: Xanh dương FootStyle (#1d4ed8)
   ========================================================================== */

/* 1. Tiêu đề trang */
.td-page-title {
  font-size: 1.65rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.2px;
}

/* 2. Thanh tìm kiếm */
.td-search-wrap {
  background: transparent;
}

.td-input-group {
  max-width: 380px;
}

.td-search-input {
  font-size: 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 4px 0 0 4px;
  padding: 6px 12px;
  height: 36px;
  background-color: #ffffff;
}

.td-search-input:focus {
  border-color: #1d4ed8;
  box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15);
}

.td-search-btn {
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-left: none;
  border-radius: 0 4px 4px 0;
  font-size: 0.85rem;
  font-weight: 500;
  color: #334155;
  padding: 0 14px;
  height: 36px;
  transition: all 0.15s ease;
}

.td-search-btn:hover {
  background-color: #1d4ed8;
  color: #ffffff;
  border-color: #1d4ed8;
}

/* 3. Thẻ Card tin tuyển dụng */
.td-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.td-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

.td-card-img-wrap {
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background-color: #f1f5f9;
}

.td-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.td-card:hover .td-card-img {
  transform: scale(1.04);
}

.td-card-body {
  padding: 14px 16px 14px;
  flex: 1;
}

.td-card-title {
  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1.35;
  color: #1e293b;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.15s ease;
}

.td-card:hover .td-card-title {
  color: #1d4ed8;
}

/* Danh sách thông tin: Vị trí, Lương, Địa điểm, Đơn vị */
.td-info-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.81rem;
}

.td-info-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.4;
}

.td-icon {
  font-size: 0.85rem;
  margin-top: 1px;
  flex-shrink: 0;
}

.td-label {
  color: #64748b;
  flex-shrink: 0;
}

.td-val {
  word-break: break-word;
}

/* Đáy thẻ */
.td-card-footer {
  border-color: #f1f5f9 !important;
}

.td-date {
  font-size: 0.78rem;
  color: #64748b;
}

.td-btn-apply {
  background-color: #1d4ed8;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 5px 16px;
  border-radius: 4px;
  border: none;
  transition: all 0.15s ease;
}

.td-btn-apply:hover {
  background-color: #1e40af;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(29, 78, 216, 0.35);
}

/* 4. Modal */
.td-modal-backdrop {
  background-color: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(2px);
}

@media (min-width: 992px) {
  .border-end-lg {
    border-right: 1px solid #e2e8f0;
  }
}
</style>
