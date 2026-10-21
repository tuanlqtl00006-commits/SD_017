<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('ALL') // 'ALL' | 'VOT' | 'GIAY' | 'KY_THUAT' | 'KHUYEN_MAI'

const posts = ref([
  {
    id: 1,
    title: 'Bí quyết chọn vợt cầu lông chuẩn theo lối đánh Công - Thủ toàn diện',
    category: 'VOT',
    categoryName: 'TƯ VẤN VỢT',
    date: '20/10/2026',
    author: 'HLV FootStyle',
    views: 1420,
    summary:
      'Lối chơi công thủ toàn diện đòi hỏi cây vợt có điểm cân bằng vừa phải (290-295mm), thân vợt có độ dẻo trung bình (Medium Flex) để linh hoạt chuyển đổi giữa smash và phòng thủ phản tạt.',
    content: `
      <p>Chọn đúng cây vợt là bước khởi đầu quan trọng nhất cho bất kỳ lông thủ nào. Dưới đây là 3 yếu tố cốt lõi bạn cần nắm rõ:</p>
      <h5>1. Trọng lượng vợt (Thông số U)</h5>
      <p>Phần lớn người chơi phong trào Việt Nam thích hợp nhất với trọng lượng <strong>4U (80-84g)</strong>. Cổ tay khỏe hoặc người chơi chuyên nghiệp thích smash uy lực có thể chọn <strong>3U (85-89g)</strong>. Người mới chơi, nữ giới hoặc người có lực cổ tay vừa phải nên ưu tiên 4U hoặc 5U.</p>
      <h5>2. Điểm cân bằng (Balance Point)</h5>
      <ul>
        <li><strong>Vợt nhẹ đầu (Dưới 285mm):</strong> Thủ cầu nhanh, phản tạt linh hoạt, thích hợp đánh đôi trên lưới.</li>
        <li><strong>Vợt cân bằng (290 - 295mm):</strong> Công thủ toàn diện, kiểm soát cầu tốt.</li>
        <li><strong>Vợt nặng đầu (Trên 300mm):</strong> Tấn công uy lực, smash cắm sân mạnh mẽ.</li>
      </ul>
      <h5>3. Độ cứng thân vợt (Shaft Flexibility)</h5>
      <p>Thân vợt càng dẻo càng trợ lực tốt cho người chơi lực tay trung bình. Thân vợt cứng (Stiff) đòi hỏi lực phát từ cổ tay dứt khoát, bù lại cho độ chính xác cực cao.</p>
    `,
  },
  {
    id: 2,
    title: 'Top 5 đôi giày cầu lông bảo vệ cổ chân và giảm chấn tốt nhất 2026',
    category: 'GIAY',
    categoryName: 'TƯ VẤN GIÀY',
    date: '18/10/2026',
    author: 'FootStyle Lab',
    views: 980,
    summary:
      'Khám phá công nghệ đệm Power Cushion+ và thanh chống xoắn TPU cao cấp giúp ngăn ngừa chấn thương lật sơ mi cổ chân khi bật nhảy tiếp đất.',
    content: `
      <p>Trong môn cầu lông, đôi giày quan trọng không kém cây vợt vì chịu toàn bộ xung lực từ các bước dậm nhảy, đổi hướng đột ngột:</p>
      <h5>1. Yonex Power Cushion Eclipsion Z3</h5>
      <p>Dòng giày cao cấp nhất về khả năng giữ ổn định bàn chân, công nghệ Power Cushion+ độc quyền giảm sốc đến 28% và tăng lực bật nảy 62%.</p>
      <h5>2. Yonex SHB 65Z3</h5>
      <p>Đôi giày toàn diện được huyền thoại Kento Momota và đông đảo tuyển thủ thế giới tin dùng nhờ trọng lượng nhẹ và độ ôm chân tuyệt hảo.</p>
      <h5>3. Lining Ranger Pro 2026</h5>
      <p>Đế cao su kép chống mài mòn, bám sân cực tốt cả trên sàn thảm PVC lẫn sàn gỗ.</p>
    `,
  },
  {
    id: 3,
    title: 'Bao lâu nên căng cước vợt một lần và nên căng bao nhiêu kg là chuẩn?',
    category: 'KY_THUAT',
    categoryName: 'KỸ THUẬT ĐAN CƯỚC',
    date: '15/10/2026',
    author: 'Chuyên Gia Cước FootStyle',
    views: 2150,
    summary:
      'Người mới chơi nên căng từ 9.5kg - 10.5kg (21-23 lbs). Căng quá số kg so với lực cổ tay dễ gây đau bả vai và giảm độ nảy của quả cầu.',
    content: `
      <p>Cước cầu lông sau một thời gian chơi sẽ bị chùng giãn cơ học, làm giảm độ nảy và độ cảm giác cầu:</p>
      <h5>Mức căng khuyến nghị theo trình độ:</h5>
      <ul>
        <li><strong>Người mới chơi, học sinh - sinh viên:</strong> 9.5kg - 10.5kg (21 - 23 lbs).</li>
        <li><strong>Người chơi phong trào khá (từ 1-3 năm):</strong> 10.5kg - 11.5kg (23 - 25.5 lbs).</li>
        <li><strong>VĐV chuyên nghiệp, bán chuyên:</strong> 12kg - 13.5kg (26.5 - 30 lbs).</li>
      </ul>
      <p>Khuyến nghị nên thay cước sau mỗi 2 - 3 tháng chơi đều đặn, ngay cả khi cước chưa đứt để đảm bảo cảm giác cầu tối ưu.</p>
    `,
  },
  {
    id: 4,
    title: 'Đại tiệc siêu sale Flash Sale tuần lễ tri ân khách hàng FootStyle',
    category: 'KHUYEN_MAI',
    categoryName: 'TIN KHUYẾN MÃI',
    date: '12/10/2026',
    author: 'Ban Truyền Thông',
    views: 3100,
    summary:
      'Giảm giá đến 50% cho toàn bộ phụ kiện cầu lông, tặng ngay cước Yonex BG65 khi mua vợt bất kỳ và tặng voucher 50K cho đơn hàng từ 500K.',
    content: `
      <p>Hệ thống cửa hàng FootStyle trên toàn quốc hân hạnh mang đến chương trình ưu đãi lớn nhất mùa giải với hàng loạt deal sốc:</p>
      <ul>
        <li>Vợt Yonex, Lining, Victor giảm giá 15% - 40%.</li>
        <li>Tặng 1 cước BG65 chính hãng + miễn phí công căng 4 nút tiêu chuẩn Yonex.</li>
        <li>Freeship toàn quốc cho đơn hàng từ 500.000₫.</li>
      </ul>
    `,
  },
])

const filteredPosts = computed(() => {
  if (activeTab.value === 'ALL') return posts.value
  return posts.value.filter((p) => p.category === activeTab.value)
})

const selectedPost = ref(null)
function openPostDetail(post) {
  selectedPost.value = post
  window.scrollTo({ top: 300, behavior: 'smooth' })
}
function backToList() {
  selectedPost.value = null
}
</script>

<template>
  <div class="vnb-container py-4">
    <!-- Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-4">
      <ol class="breadcrumb small">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Tin tức & Cẩm nang</li>
      </ol>
    </nav>

    <!-- Header -->
    <div class="vnb-section-title-wrap mb-4">
      <h2 class="vnb-section-title">Tin Tức & Cẩm Nang Cầu Lông</h2>
      <div class="vnb-title-underline"></div>
      <p class="text-muted small mt-2">Tổng hợp kiến thức trang bị, kỹ thuật chơi cầu và các chương trình ưu đãi mới nhất từ FootStyle</p>
    </div>

    <!-- CHI TIẾT BÀI VIẾT (Nếu đang chọn đọc) -->
    <div v-if="selectedPost" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 mb-5 bg-white">
      <button class="btn btn-outline-primary btn-sm rounded-pill mb-4 align-self-start fw-bold" @click="backToList">
        <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách bài viết
      </button>

      <span class="badge bg-primary px-3 py-1 rounded-pill align-self-start mb-2 fw-bold">
        {{ selectedPost.categoryName }}
      </span>

      <h2 class="fw-black text-dark mb-3">{{ selectedPost.title }}</h2>

      <div class="d-flex align-items-center gap-4 text-muted small pb-3 mb-4 border-bottom">
        <span><i class="bi bi-person-fill text-primary me-1"></i> {{ selectedPost.author }}</span>
        <span><i class="bi bi-calendar3 text-primary me-1"></i> {{ selectedPost.date }}</span>
        <span><i class="bi bi-eye-fill text-primary me-1"></i> {{ selectedPost.views }} lượt đọc</span>
      </div>

      <div class="lead text-secondary mb-4 p-3 bg-light rounded-3 border-start border-4 border-primary">
        {{ selectedPost.summary }}
      </div>

      <div class="post-content lh-lg text-dark mb-4" v-html="selectedPost.content"></div>

      <div class="pt-4 border-top d-flex justify-content-between align-items-center">
        <button class="btn btn-outline-secondary btn-sm rounded-pill" @click="backToList">
          ← Bài viết khác
        </button>
        <RouterLink to="/shop/san-pham" class="btn btn-primary btn-sm rounded-pill fw-bold px-3">
          Khám phá sản phẩm liên quan →
        </RouterLink>
      </div>
    </div>

    <!-- DANH SÁCH BÀI VIẾT -->
    <div v-else>
      <!-- Tabs Lọc -->
      <div class="vnb-cat-tabs mb-4 rounded-3 border">
        <button
          class="vnb-cat-tab"
          :class="{ active: activeTab === 'ALL' }"
          @click="activeTab = 'ALL'"
        >
          Tất cả tin tức
        </button>
        <button
          class="vnb-cat-tab"
          :class="{ active: activeTab === 'VOT' }"
          @click="activeTab = 'VOT'"
        >
          Tư vấn chọn vợt
        </button>
        <button
          class="vnb-cat-tab"
          :class="{ active: activeTab === 'GIAY' }"
          @click="activeTab = 'GIAY'"
        >
          Tư vấn chọn giày
        </button>
        <button
          class="vnb-cat-tab"
          :class="{ active: activeTab === 'KY_THUAT' }"
          @click="activeTab = 'KY_THUAT'"
        >
          Kỹ thuật & Căng cước
        </button>
        <button
          class="vnb-cat-tab"
          :class="{ active: activeTab === 'KHUYEN_MAI' }"
          @click="activeTab = 'KHUYEN_MAI'"
        >
          Tin khuyến mãi
        </button>
      </div>

      <!-- Lưới bài viết -->
      <div class="row g-4">
        <div
          v-for="post in filteredPosts"
          :key="post.id"
          class="col-lg-6"
        >
          <div
            class="card h-100 border-0 shadow-sm rounded-4 overflow-hidden p-3 p-md-4 cursor-pointer post-item-card bg-white"
            @click="openPostDetail(post)"
          >
            <div class="d-flex align-items-center justify-content-between mb-2">
              <span class="badge bg-primary-subtle text-primary fw-bold px-3 py-1 rounded-pill small">
                {{ post.categoryName }}
              </span>
              <small class="text-muted"><i class="bi bi-clock me-1"></i>{{ post.date }}</small>
            </div>

            <h5 class="fw-bold text-dark my-2 post-title-hover">
              {{ post.title }}
            </h5>

            <p class="text-secondary small mb-3 flex-grow-1">
              {{ post.summary }}
            </p>

            <div class="d-flex align-items-center justify-content-between pt-2 border-top border-light small text-muted">
              <span><i class="bi bi-person me-1"></i>{{ post.author }}</span>
              <span class="text-primary fw-bold">Xem chi tiết <i class="bi bi-arrow-right ms-1"></i></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}

.post-item-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.post-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08) !important;
}

.post-title-hover {
  transition: color 0.15s;
}

.post-item-card:hover .post-title-hover {
  color: #1d4ed8 !important;
}
</style>
