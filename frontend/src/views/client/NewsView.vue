<script setup>
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'

// Danh mục tin tức theo đúng sidebar trong ảnh mẫu
const categories = ref([
  {
    id: 'tong-hop',
    name: 'Thông Tin Tổng Hợp Cầu Lông',
    hasChildren: true,
    children: ['Review sân cầu lông', 'Địa điểm tập luyện', 'Cộng đồng cầu lông'],
  },
  { id: 'clb-nhom', name: 'Câu Lạc Bộ - Nhóm Cầu Lông', hasChildren: false },
  { id: 'thay-day', name: 'Thầy Dạy Đánh Cầu Lông', hasChildren: false },
  {
    id: 'tin-footstyle',
    name: 'Tin tức FootStyle Sports',
    hasChildren: true,
    children: ['Khai trương chi nhánh', 'Khuyến mãi sự kiện', 'Hoạt động nội bộ'],
  },
  { id: 'chinh-sach', name: 'Chính sách', hasChildren: false },
  { id: 'tin-tennis', name: 'Tin Tennis', hasChildren: false },
  { id: 'san-tennis', name: 'Sân Tennis', hasChildren: false },
  {
    id: 'kien-thuc-tennis',
    name: 'Kiến thức tennis',
    hasChildren: true,
    children: ['Kỹ thuật giao bóng', 'Chọn vợt tennis', 'Lối đánh hiện đại'],
  },
  { id: 'hieu-dung', name: 'Hiểu đúng về giày & dụng cụ', hasChildren: false },
  { id: 'song-khoe', name: 'Sống khoẻ cùng FootStyle Sports', hasChildren: false },
  { id: 'san-pickleball', name: 'Sân pickleball', hasChildren: false },
  { id: 'kien-thuc-pickleball', name: 'Kiến thức pickleball', hasChildren: false },
  { id: 'luat-pickleball', name: 'Luật pickleball', hasChildren: false },
  { id: 'ky-thuat-pickleball', name: 'Kỹ thuật đánh trong pickleball', hasChildren: false },
  { id: 'huong-dan', name: 'Hướng dẫn', hasChildren: false },
  { id: 'cong-nghe', name: 'Công nghệ', hasChildren: false },
])

const openSubmenu = ref({ 'tong-hop': true, 'tin-footstyle': false, 'kien-thuc-tennis': false })
function toggleSubmenu(catId) {
  openSubmenu.value[catId] = !openSubmenu.value[catId]
}

const selectedCategory = ref('tong-hop')
const selectedSubcategory = ref('')
const searchKeyword = ref('')
const activeKeyword = ref('')

const selectedCategoryTitle = computed(() => {
  if (selectedSubcategory.value) return selectedSubcategory.value.toUpperCase()
  const cat = categories.value.find((c) => c.id === selectedCategory.value)
  return cat ? cat.name.toUpperCase() : 'THÔNG TIN TỔNG HỢP CẦU LÔNG'
})

// Dữ liệu bài viết thực tế bám sát 100% nội dung và giao diện trong ảnh chụp màn hình
const allArticles = ref([
  {
    id: 1,
    title: 'Review Chi Tiết Sân Cầu Lông Hồng Lĩnh Tại Thủ Dầu Một, Bình Dương',
    categoryId: 'tong-hop',
    subCategory: 'Review sân cầu lông',
    date: '23-10-2026 09:27',
    author: 'Admin FootStyle',
    views: 1850,
    image: 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=700&auto=format&fit=crop&q=80',
    summary:
      'Sân cầu lông Hồng Lĩnh là một trong những địa điểm dành cho người yêu thích cầu lông tại khu vực Thủ Dầu Một. Sân sở hữu mặt thảm PVC chuẩn thi đấu, hệ thống đèn chiếu sáng chống chói mắt hiện đại cùng dịch vụ tiện ích đầy đủ.',
    content: `
      <p>Sân cầu lông Hồng Lĩnh tại Thủ Dầu Một, Bình Dương từ lâu đã trở thành điểm hẹn quen thuộc của đông đảo các tay vợt phong trào và bán chuyên trong khu vực. Với không gian rộng rãi, thoáng mát cùng cơ sở vật chất được đầu tư bài bản, đây là một trong những sân cầu lông chất lượng hàng đầu hiện nay.</p>
      <h5>1. Quy mô và chất lượng mặt sân</h5>
      <p>Sân gồm cụm 6 sân tiêu chuẩn được trải thảm PVC đạt chuẩn thi đấu cầu lông quốc tế. Độ bám của thảm rất tốt, hỗ trợ tối đa cho các pha di chuyển đổi hướng đột ngột và giảm chấn thương hiệu quả cho khớp gối và cổ chân của người chơi.</p>
      <h5>2. Hệ thống chiếu sáng và độ thoáng khí</h5>
      <p>Hệ thống đèn LED cao cấp được lắp đặt hai bên dọc biên sân ở độ cao hợp lý, đảm bảo ánh sáng trải đều toàn bộ mặt sân mà không gây chói lóa khi ngước nhìn cầu ở các pha smash hay lốp cầu cao sâu. Trần nhà cao trên 9m tạo không gian vô cùng thoáng mát, không bị hầm bí ngay cả trong những ngày thời tiết oi bức.</p>
      <h5>3. Dịch vụ tiện ích đi kèm</h5>
      <p>Khu vực căng-tin phục vụ đa dạng các loại nước uống bù khoáng, nước tăng lực và đồ ăn nhẹ. Có phòng thay đồ, nhà vệ sinh sạch sẽ, bãi đỗ xe ô tô và xe máy rộng rãi có bảo vệ trông coi 24/7. Ngoài ra, sân còn có dịch vụ căng cước vợt lấy ngay và bán phụ kiện cầu lông chính hãng.</p>
    `,
  },
  {
    id: 2,
    title: 'Tìm Hiểu Chi Tiết Chất Lượng Và Dịch Vụ Tiện Ích Của Sân Cầu Lông Đông Hòa',
    categoryId: 'tong-hop',
    subCategory: 'Review sân cầu lông',
    date: '22-10-2026 11:43',
    author: 'Minh Cường',
    views: 1420,
    image: 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=700&auto=format&fit=crop&q=80',
    summary:
      'Sân cầu lông Đông Hòa là địa điểm được nhiều người chơi lựa chọn để tập luyện và giao lưu tại TP.HCM. Mặt sân đạt chuẩn êm ái, trần cao thoáng đãng, mức giá thuê hợp lý phù hợp cho cả học sinh sinh viên và người đi làm.',
    content: `
      <p>Nếu bạn đang tìm kiếm một cụm sân cầu lông chất lượng, giá cả phải chăng tại khu vực TP.HCM thì sân cầu lông Đông Hòa chắc chắn là một gợi ý không thể bỏ qua. Đây là địa điểm lý tưởng để tổ chức các buổi giao lưu nội bộ, giải phong trào cũng như tập luyện nâng cao kỹ thuật.</p>
      <h5>1. Thiết kế mặt sân và không gian</h5>
      <p>Cụm sân có tổng cộng 8 thảm đấu chất lượng cao với khoảng cách giữa các sân rộng rãi, đảm bảo sự an toàn cho các pha cứu cầu ở cuối biên. Tông màu thảm xanh lá cây kết hợp viền xanh dương mang lại cảm giác dễ chịu cho mắt nhìn trong suốt thời gian dài vận động.</p>
      <h5>2. Mức giá thuê và giờ mở cửa</h5>
      <p>Sân mở cửa liên tục từ 05:00 sáng đến 23:00 đêm tất cả các ngày trong tuần. Mức giá thuê dao động từ 60.000đ - 110.000đ/giờ tùy theo khung giờ cố định hoặc vãng lai, cực kỳ hợp lý cho học sinh, sinh viên và nhân viên văn phòng.</p>
    `,
  },
  {
    id: 3,
    title: 'Vợt Cầu Lông Bằng Sắt Có Dùng Được Không? Những Ai Phù Hợp Để Sử Dụng?',
    categoryId: 'tong-hop',
    subCategory: 'Review sân cầu lông',
    date: '22-10-2026 10:24',
    author: 'HLV FootStyle',
    views: 2980,
    image: 'https://images.unsplash.com/photo-1613918431703-aa6321a0077c?w=700&auto=format&fit=crop&q=80',
    summary:
      'Vợt cầu lông bằng sắt là từ khóa được nhiều người tìm kiếm khi muốn chọn một cây vợt giá rẻ, có độ bền cao. Tuy nhiên loại vợt này có trọng lượng khá nặng và độ đàn hồi kém, phù hợp nhất cho tập cổ tay hoặc giải trí gia đình.',
    content: `
      <p>Vợt cầu lông bằng sắt (hoặc hợp kim nhôm - thép) thường có giá thành rất rẻ chỉ từ vài chục đến hơn một trăm ngàn đồng một cặp. Nhưng liệu loại vợt này có thực sự phù hợp để chơi cầu lông lâu dài hay không?</p>
      <h5>1. Ưu điểm của vợt bằng sắt</h5>
      <ul>
        <li>Giá thành cực rẻ, dễ dàng tiếp cận với bất kỳ ai.</li>
        <li>Khung vợt rất cứng, chịu va đập tốt, không sợ bị nứt gãy khi cạch vợt xuống sàn bê tông.</li>
        <li>Có thể dùng làm bài tập bổ trợ lực cổ tay nhờ trọng lượng nặng vượt trội.</li>
      </ul>
      <h5>2. Nhược điểm và đối tượng nên dùng</h5>
      <p>Do trọng lượng nặng (thường trên 95g - 110g), khớp nối chữ T bằng kim loại thô và độ dẻo thân vợt hầu như không có, người chơi đánh lâu rất dễ bị mỏi bả vai và viêm khớp cổ tay. Loại vợt này chỉ thích hợp đánh giải trí gia đình ngoài công viên hoặc dùng làm tạ tay bổ trợ.</p>
    `,
  },
  {
    id: 4,
    title: 'Sân Cầu Lông Nhân Trần: Review Giá Thuê Và Dịch Vụ Tiện Ích Tại TP.HCM',
    categoryId: 'tong-hop',
    subCategory: 'Review sân cầu lông',
    date: '22-10-2026 09:31',
    author: 'Quốc Bảo',
    views: 1150,
    image: 'https://images.unsplash.com/photo-1599586120429-48281b6f0ece?w=700&auto=format&fit=crop&q=80',
    summary:
      'Sân cầu lông Nhân Trần là địa điểm đáng trải nghiệm với dàn thảm thi đấu chuẩn quốc tế, không gian thoáng mát và bãi gửi xe rộng rãi. Phù hợp cho các hội nhóm giao lưu và các câu lạc bộ phong trào thường xuyên tập luyện.',
    content: `
      <p>Sân cầu lông Nhân Trần nổi tiếng với cơ sở vật chất mới tinh, hệ thống chiếu sáng đạt chuẩn không gây bóng đổ và dịch vụ chăm sóc khách hàng cực kỳ chu đáo. Sân có đầy đủ quạt thông gió công suất lớn giúp duy trì nhiệt độ thoải mái suốt cả buổi đánh cầu.</p>
    `,
  },
  {
    id: 5,
    title: 'Khám Phá Chi Tiết Sân Cầu Lông Long Bình Từ A-Z Tại Thanh Hóa',
    categoryId: 'tong-hop',
    subCategory: 'Review sân cầu lông',
    date: '21-10-2026 15:16',
    author: 'Admin FootStyle',
    views: 940,
    image: 'https://images.unsplash.com/photo-1544698310-74ea9d1c8258?w=700&auto=format&fit=crop&q=80',
    summary:
      'Sân cầu lông Long Bình nằm tại vị trí thuận lợi, mặt sân thảm đệm 5 lớp êm ái bảo vệ tối ưu khớp chân người chơi. Sân có huấn luyện viên trực tiếp kèm cặp cho người mới bắt đầu và hỗ trợ tổ chức giải đấu chuyên nghiệp.',
    content: `
      <p>Sân cầu lông Long Bình là một trong những trung tâm đào tạo và sinh hoạt cầu lông sôi động nhất tại Thanh Hóa. Với mặt sàn đệm êm ái chống trơn trượt cùng dàn đèn chiếu sáng hiện đại, sân đáp ứng hoàn hảo cho mọi nhu cầu tập luyện từ nghiệp dư tới thi đấu chính thức.</p>
    `,
  },
  {
    id: 6,
    title: 'Rộn Ràng Khai Trương FootStyle Thạnh Trị CN2: Đại Tiệc Ưu Đãi, Giảm Giá Sốc',
    categoryId: 'tin-footstyle',
    subCategory: 'Khai trương chi nhánh',
    date: '19-10-2026 15:41',
    author: 'Ban Truyền Thông',
    views: 3410,
    image: 'https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?w=700&auto=format&fit=crop&q=80',
    summary:
      'FootStyle vừa chính thức mở rộng thêm một cột mốc mới với chi nhánh FootStyle Thạnh Trị CN2. Hàng loạt ưu đãi hấp dẫn: đan cước vợt miễn phí, giảm giá đến 50% toàn bộ giày và vợt cầu lông chính hãng cùng quà tặng độc quyền.',
    content: `
      <p>Sự kiện khai trương FootStyle Thạnh Trị CN2 đã thu hút hàng ngàn lông thủ đến trải nghiệm mua sắm và giao lưu. Trong tuần lễ khai trương, khách hàng được miễn phí 100% công đan cước, giảm giá từ 20% đến 50% cho tất cả các thương hiệu vợt cầu lông nổi tiếng như Yonex, Victor, Lining.</p>
    `,
  },
  {
    id: 7,
    title: 'Top 5 Câu Lạc Bộ Cầu Lông Phong Trào Nổi Tiếng Nhất Tại Hà Nội',
    categoryId: 'clb-nhom',
    subCategory: '',
    date: '18-10-2026 14:20',
    author: 'Tuấn Long',
    views: 1240,
    image: 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=700&auto=format&fit=crop&q=80',
    summary:
      'Danh sách các CLB cầu lông sinh hoạt đều đặn các buổi tối trong tuần, tinh thần giao lưu nhiệt tình, phân chia trình độ rõ ràng giúp bạn dễ dàng hòa nhập và nâng cao trình độ thi đấu.',
    content: `<p>Tham gia câu lạc bộ cầu lông là cách nhanh nhất để rèn luyện sức khỏe và mở rộng mối quan hệ bạn bè có cùng đam mê thể thao.</p>`,
  },
  {
    id: 8,
    title: 'Danh Sách Huấn Luyện Viên Cầu Lông Uy Tín Cho Người Mới Bắt Đầu Và Trẻ Em',
    categoryId: 'thay-day',
    subCategory: '',
    date: '17-10-2026 10:15',
    author: 'FootStyle Lab',
    views: 1110,
    image: 'https://images.unsplash.com/photo-1544698310-74ea9d1c8258?w=700&auto=format&fit=crop&q=80',
    summary:
      'Tổng hợp các thầy dạy cầu lông có chứng chỉ BWF, phương pháp truyền đạt dễ hiểu, chỉnh sửa bộ chân và cách phát lực cổ tay chuẩn chỉ ngay từ những buổi học đầu tiên.',
    content: `<p>Học đúng kỹ thuật từ đầu sẽ giúp bạn phòng tránh các chấn thương khớp vai, cổ tay và tiến bộ vượt bậc sau thời gian ngắn.</p>`,
  },
  {
    id: 9,
    title: 'Luật Giao Cầu Lông Đôi Và Đơn Cập Nhật Mới Nhất Chuẩn Quốc Tế BWF',
    categoryId: 'huong-dan',
    subCategory: '',
    date: '16-10-2026 16:30',
    author: 'Trọng Tài Quốc Gia',
    views: 2850,
    image: 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=700&auto=format&fit=crop&q=80',
    summary:
      'Quy định về chiều cao quả cầu khi giao không quá 1.15m tính từ mặt đất, luật ô giao cầu chẵn lẻ và các lỗi giao cầu thường gặp nhất trong các giải đấu phong trào.',
    content: `<p>Nắm vững luật thi đấu cầu lông giúp bạn tự tin hơn trong các pha xử lý trên sân và tránh bị mất điểm đáng tiếc trong các trận cầu căng thẳng.</p>`,
  },
])

// Lọc bài viết theo danh mục & từ khóa
const filteredArticles = computed(() => {
  let list = allArticles.value

  // Lọc theo danh mục chính
  if (selectedCategory.value) {
    list = list.filter((a) => a.categoryId === selectedCategory.value)
  }

  // Lọc theo danh mục con nếu có
  if (selectedSubcategory.value) {
    list = list.filter((a) => a.subCategory === selectedSubcategory.value)
  }

  // Lọc theo từ khóa tìm kiếm
  if (activeKeyword.value.trim()) {
    const kw = activeKeyword.value.trim().toLowerCase()
    list = list.filter(
      (a) =>
        a.title.toLowerCase().includes(kw) ||
        a.summary.toLowerCase().includes(kw) ||
        a.author.toLowerCase().includes(kw),
    )
  }

  return list
})

// Phân trang
const currentPage = ref(1)
const pageSize = 6
const totalPages = computed(() => Math.ceil(filteredArticles.value.length / pageSize) || 1)
const pagedArticles = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredArticles.value.slice(start, start + pageSize)
})

function chooseCategory(catId, sub = '') {
  selectedCategory.value = catId
  selectedSubcategory.value = sub
  currentPage.value = 1
  selectedArticle.value = null
  window.scrollTo({ top: 120, behavior: 'smooth' })
}

function handleSearch() {
  activeKeyword.value = searchKeyword.value
  currentPage.value = 1
}

function clearSearch() {
  searchKeyword.value = ''
  activeKeyword.value = ''
  currentPage.value = 1
}

// Xem chi tiết bài viết
const selectedArticle = ref(null)
function openPostDetail(article) {
  selectedArticle.value = article
  window.scrollTo({ top: 120, behavior: 'smooth' })
}
function backToList() {
  selectedArticle.value = null
  window.scrollTo({ top: 120, behavior: 'smooth' })
}

// Fallback ảnh khi không tải được
function onImageError(e) {
  e.target.src =
    'data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22600%22%20height%3D%22400%22%20viewBox%3D%220%200%20600%20400%22%3E%3Crect%20fill%3D%22%23e2e8f0%22%20width%3D%22600%22%20height%3D%22400%22%2F%3E%3Ctext%20fill%3D%22%2364748b%22%20font-family%3D%22sans-serif%22%20font-size%3D%2224%22%20font-weight%3D%22bold%22%20x%3D%2250%25%22%20y%3D%2250%25%22%20text-anchor%3D%22middle%22%3EFootStyle%20Badminton%3C%2Ftext%3E%3C%2Fsvg%3E'
}
</script>

<template>
  <div class="vnb-container py-3">
    <!-- Breadcrumb nhẹ nhàng -->
    <nav aria-label="breadcrumb" class="mb-2">
      <ol class="breadcrumb small py-1 mb-2">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none text-muted">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item"><RouterLink to="/shop/tin-tuc" class="text-decoration-none text-muted" @click="backToList">Tin tức</RouterLink></li>
        <li class="breadcrumb-item active text-dark fw-semibold" aria-current="page">
          {{ selectedArticle ? selectedArticle.title : selectedCategoryTitle }}
        </li>
      </ol>
    </nav>

    <div class="row g-4">
      <!-- CỘT TRÁI: NỘI DUNG CHÍNH (Chiếm ~75% theo mẫu ShopVNB) -->
      <main class="col-lg-9 col-md-8">
        <!-- Khi đang xem CHI TIẾT BÀI VIẾT -->
        <article v-if="selectedArticle" class="news-detail-box bg-white p-4 p-md-5 rounded-3 border shadow-sm">
          <button class="btn btn-outline-secondary btn-sm rounded-pill mb-3" @click="backToList">
            <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách tin tức
          </button>

          <h1 class="news-detail-title fw-bold mb-3 text-dark">
            {{ selectedArticle.title }}
          </h1>

          <div class="d-flex flex-wrap align-items-center gap-3 text-muted small pb-3 mb-4 border-bottom">
            <span class="news-date-badge">{{ selectedArticle.date }}</span>
            <span><i class="bi bi-person-fill text-primary me-1"></i> Đăng bởi: <strong>{{ selectedArticle.author }}</strong></span>
            <span><i class="bi bi-eye-fill text-primary me-1"></i> {{ selectedArticle.views }} lượt xem</span>
          </div>

          <div class="news-detail-lead p-3 rounded-2 bg-light border-start border-4 border-primary mb-4 fw-medium text-secondary">
            {{ selectedArticle.summary }}
          </div>

          <div class="news-detail-thumb-box mb-4 text-center">
            <img :src="selectedArticle.image" :alt="selectedArticle.title" class="img-fluid rounded-3 shadow-sm news-detail-img" @error="onImageError" />
          </div>

          <div class="news-detail-content lh-lg text-dark mb-5" v-html="selectedArticle.content"></div>

          <!-- Nút chia sẻ và quay lại -->
          <div class="pt-4 border-top d-flex justify-content-between align-items-center flex-wrap gap-2">
            <button class="btn btn-secondary btn-sm px-3 rounded-pill" @click="backToList">
              <i class="bi bi-chevron-left me-1"></i> Xem các tin khác
            </button>
            <RouterLink to="/shop/san-pham" class="btn btn-primary btn-sm px-3 rounded-pill fw-bold">
              Xem sản phẩm cầu lông tại cửa hàng <i class="bi bi-arrow-right ms-1"></i>
            </RouterLink>
          </div>
        </article>

        <!-- Khi xem DANH SÁCH BÀI VIẾT -->
        <section v-else>
          <!-- Khung tìm kiếm từ khóa -->
          <div class="news-search-bar d-flex align-items-center gap-2 mb-4">
            <label for="news-kw-input" class="news-search-label flex-shrink-0">Từ khóa:</label>
            <div class="input-group news-input-group">
              <input
                id="news-kw-input"
                type="text"
                v-model="searchKeyword"
                class="form-control news-search-input"
                placeholder="Nhập tiêu đề hoặc nội dung cần tìm..."
                @keyup.enter="handleSearch"
              />
              <button class="btn news-btn-search" type="button" @click="handleSearch">
                Tìm kiếm
              </button>
            </div>
            <button v-if="activeKeyword" class="btn btn-outline-secondary btn-sm flex-shrink-0" type="button" @click="clearSearch" title="Xóa bộ lọc tìm kiếm">
              <i class="bi bi-x-circle me-1"></i> Xóa tìm
            </button>
          </div>

          <!-- Trạng thái khi không có bài viết nào -->
          <div v-if="pagedArticles.length === 0" class="card border-0 p-5 text-center bg-white rounded-3 shadow-sm text-muted my-4">
            <i class="bi bi-newspaper display-4 text-secondary mb-2"></i>
            <h5 class="fw-bold">Chưa tìm thấy bài viết phù hợp</h5>
            <p class="small mb-3">Vui lòng thử tìm với từ khóa khác hoặc chọn danh mục khác ở cột bên phải.</p>
            <div>
              <button class="btn btn-primary btn-sm rounded-pill px-3" @click="clearSearch(); chooseCategory('tong-hop')">
                Xem tất cả bài viết
              </button>
            </div>
          </div>

          <!-- 3. Lưới bài viết (3 cột x 2 hàng theo đúng ảnh chụp màn hình) -->
          <div v-else class="row g-3 g-xl-4 news-grid">
            <div
              v-for="post in pagedArticles"
              :key="post.id"
              class="col-lg-4 col-sm-6 d-flex"
            >
              <div class="news-card w-100" @click="openPostDetail(post)">
                <!-- Khung ảnh bài viết -->
                <div class="news-card-img-wrap">
                  <img
                    :src="post.image"
                    :alt="post.title"
                    class="news-card-img"
                    loading="lazy"
                    @error="onImageError"
                  />
                </div>

                <!-- Thân card bài viết -->
                <div class="news-card-body d-flex flex-column">
                  <h2 class="news-card-title" :title="post.title">
                    {{ post.title }}
                  </h2>

                  <!-- Badge ngày giờ màu cam chuẩn 100% mẫu -->
                  <div class="news-card-meta mb-2">
                    <span class="news-date-badge">{{ post.date }}</span>
                  </div>

                  <!-- Tóm tắt ngắn 3 dòng có ... -->
                  <p class="news-card-snippet flex-grow-1">
                    {{ post.summary }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- 4. Phân trang nếu có nhiều bài viết -->
          <nav v-if="totalPages > 1" aria-label="Phân trang tin tức" class="d-flex justify-content-center mt-4">
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <button class="page-link" @click="currentPage--" aria-label="Trang trước">&laquo;</button>
              </li>
              <li
                v-for="page in totalPages"
                :key="page"
                class="page-item"
                :class="{ active: currentPage === page }"
              >
                <button class="page-link" @click="currentPage = page">{{ page }}</button>
              </li>
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <button class="page-link" @click="currentPage++" aria-label="Trang sau">&raquo;</button>
              </li>
            </ul>
          </nav>
        </section>
      </main>

      <!-- CỘT PHẢI: SIDEBAR (Chiếm ~25% theo mẫu ShopVNB) -->
      <aside class="col-lg-3 col-md-4">
        <!-- Khối 1: DANH MỤC TIN TỨC -->
        <div class="news-sidebar-box bg-white rounded-2 border shadow-sm mb-4">
          <div class="news-sidebar-header">
            <h2 class="news-sidebar-title mb-0">DANH MỤC TIN TỨC</h2>
          </div>
          <ul class="news-category-list list-unstyled m-0 p-0">
            <li
              v-for="cat in categories"
              :key="cat.id"
              class="news-category-item"
              :class="{ active: selectedCategory === cat.id && !selectedSubcategory }"
            >
              <div class="d-flex align-items-center justify-content-between news-category-row">
                <span
                  class="news-category-name flex-grow-1"
                  @click="chooseCategory(cat.id)"
                >
                  {{ cat.name }}
                </span>
                <button
                  v-if="cat.hasChildren"
                  class="news-toggle-btn"
                  type="button"
                  :aria-label="openSubmenu[cat.id] ? 'Thu gọn' : 'Mở rộng'"
                  @click.stop="toggleSubmenu(cat.id)"
                >
                  <i class="bi" :class="openSubmenu[cat.id] ? 'bi-dash' : 'bi-plus'"></i>
                </button>
              </div>

              <!-- Menu con nếu có -->
              <ul v-if="cat.hasChildren && openSubmenu[cat.id]" class="news-subcategory-list list-unstyled ps-3 pe-2 py-1">
                <li
                  v-for="sub in cat.children"
                  :key="sub"
                  class="news-subcategory-item py-1"
                  :class="{ active: selectedCategory === cat.id && selectedSubcategory === sub }"
                  @click="chooseCategory(cat.id, sub)"
                >
                  <i class="bi bi-chevron-right small text-muted me-1"></i>
                  <span>{{ sub }}</span>
                </li>
              </ul>
            </li>
          </ul>
        </div>

        <!-- Khối 2: DANH MỤC SẢN PHẨM NHANH (Bổ trợ theo ShopVNB) -->
        <div class="news-sidebar-box bg-white rounded-2 border shadow-sm">
          <div class="news-sidebar-header">
            <h2 class="news-sidebar-title mb-0">DANH MỤC SẢN PHẨM</h2>
          </div>
          <ul class="list-unstyled p-2 m-0 news-quick-links small">
            <li class="py-2 px-2 border-bottom">
              <RouterLink to="/shop/san-pham" class="text-decoration-none text-dark d-flex align-items-center justify-content-between">
                <span>🏸 Vợt cầu lông chính hãng</span>
                <i class="bi bi-chevron-right text-muted small"></i>
              </RouterLink>
            </li>
            <li class="py-2 px-2 border-bottom">
              <RouterLink to="/shop/san-pham" class="text-decoration-none text-dark d-flex align-items-center justify-content-between">
                <span>👟 Giày cầu lông chuyên dụng</span>
                <i class="bi bi-chevron-right text-muted small"></i>
              </RouterLink>
            </li>
            <li class="py-2 px-2 border-bottom">
              <RouterLink to="/shop/san-pham" class="text-decoration-none text-dark d-flex align-items-center justify-content-between">
                <span>👕 Quần áo thể thao cầu lông</span>
                <i class="bi bi-chevron-right text-muted small"></i>
              </RouterLink>
            </li>
            <li class="py-2 px-2 border-bottom">
              <RouterLink to="/shop/san-pham" class="text-decoration-none text-dark d-flex align-items-center justify-content-between">
                <span>🎒 Balo & Túi đựng vợt</span>
                <i class="bi bi-chevron-right text-muted small"></i>
              </RouterLink>
            </li>
            <li class="py-2 px-2">
              <RouterLink to="/shop/san-pham" class="text-decoration-none text-dark d-flex align-items-center justify-content-between">
                <span>🏓 Vợt & Phụ kiện Pickleball</span>
                <i class="bi bi-chevron-right text-muted small"></i>
              </RouterLink>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================================
   STYLE TRANG TIN TỨC CHUẨN MẪU SHOPVNB.COM
   ========================================================================== */

/* 1. Thanh tìm kiếm từ khóa */
.news-search-bar {
  background: #ffffff;
  padding: 6px 0;
}

.news-search-label {
  font-size: 0.88rem;
  font-weight: 600;
  color: #1e293b;
}

.news-input-group {
  max-width: 480px;
}

.news-search-input {
  font-size: 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 4px 0 0 4px;
  padding: 6px 12px;
  height: 36px;
}

.news-search-input:focus {
  border-color: #1d4ed8;
  box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.15);
}

.news-btn-search {
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-left: none;
  border-radius: 0 4px 4px 0;
  font-size: 0.85rem;
  font-weight: 500;
  color: #334155;
  padding: 0 16px;
  height: 36px;
  transition: all 0.15s ease;
}

.news-btn-search:hover {
  background-color: #1d4ed8;
  color: #ffffff;
  border-color: #1d4ed8;
}

/* 3. Lưới bài viết News Cards */
.news-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.news-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

.news-card-img-wrap {
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background-color: #f1f5f9;
}

.news-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.news-card:hover .news-card-img {
  transform: scale(1.05);
}

.news-card-body {
  padding: 12px 14px 14px;
  flex: 1;
}

.news-card-title {
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.35;
  color: #1e293b;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.15s ease;
}

.news-card:hover .news-card-title {
  color: #1d4ed8;
}

/* Badge ngày giờ màu xanh dương chuẩn tone thương hiệu FootStyle */
.news-card-meta {
  margin-bottom: 6px;
}

.news-date-badge {
  display: inline-block;
  background-color: #1d4ed8;
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 99px;
  letter-spacing: 0.2px;
}

.news-card-snippet {
  font-size: 0.8rem;
  line-height: 1.45;
  color: #4b5563;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 4. Sidebar Danh Mục Tin Tức */
.news-sidebar-box {
  background: #ffffff;
}

.news-sidebar-header {
  padding: 10px 14px;
  border-bottom: 2px solid #1d4ed8;
  background-color: #fafafa;
}

.news-sidebar-title {
  font-size: 0.88rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.news-category-list {
  padding: 4px 0;
}

.news-category-item {
  border-bottom: 1px dashed #f1f5f9;
  transition: background-color 0.12s ease;
}

.news-category-item:last-child {
  border-bottom: none;
}

.news-category-row {
  padding: 8px 14px;
  cursor: pointer;
  user-select: none;
}

.news-category-row:hover {
  background-color: #f8fafc;
}

.news-category-name {
  font-size: 0.83rem;
  color: #334155;
  font-weight: 500;
  transition: color 0.12s ease;
}

.news-category-row:hover .news-category-name {
  color: #1d4ed8;
  font-weight: 600;
}

.news-category-item.active .news-category-name {
  color: #1d4ed8;
  font-weight: 700;
}

.news-toggle-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 1rem;
  font-weight: bold;
  padding: 0 4px;
  cursor: pointer;
  line-height: 1;
  transition: color 0.15s ease;
}

.news-toggle-btn:hover {
  color: #1d4ed8;
}

.news-subcategory-item {
  font-size: 0.79rem;
  color: #64748b;
  cursor: pointer;
  transition: color 0.12s ease;
}

.news-subcategory-item:hover,
.news-subcategory-item.active {
  color: #1d4ed8;
  font-weight: 600;
}

.news-quick-links a:hover {
  color: #1d4ed8 !important;
}

/* 5. Khung chi tiết bài viết */
.news-detail-box {
  background: #ffffff;
}

.news-detail-title {
  font-size: 1.6rem;
  line-height: 1.3;
}

.news-detail-img {
  max-height: 440px;
  width: 100%;
  object-fit: cover;
}

.news-detail-content {
  font-size: 0.95rem;
}

.news-detail-content p {
  margin-bottom: 1rem;
}

.news-detail-content h5 {
  font-weight: 700;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  color: #1e293b;
}

@media (max-width: 768px) {
  .news-detail-title {
    font-size: 1.3rem;
  }
}
</style>
