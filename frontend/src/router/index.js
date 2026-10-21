import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import ClientLayout from '../layouts/ClientLayout.vue'
import { THUOC_TINH_LIST } from '../constants/thuocTinh'

// meta.title / meta.parent: hiển thị trên header (kiểu "Quản lý giảm giá / Phiếu giảm giá").
const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/hoa-don',
    children: [
      {
        path: 'thong-ke',
        name: 'thong-ke',
        component: () => import('../components/ThongKe.vue'),
        meta: { title: 'Thống kê' },
      },
      {
        path: 'hoa-don',
        name: 'hoa-don',
        component: () => import('../components/HoaDonManager.vue'),
        meta: { title: 'Hóa đơn' },
      },
      {
        path: 'hoa-don/:id',
        name: 'hoa-don-chi-tiet',
        component: () => import('../components/HoaDonChiTiet.vue'),
        meta: { parent: 'Hóa đơn', parentTo: '/hoa-don', title: 'Chi tiết hóa đơn' },
      },
      {
        path: 'san-pham',
        name: 'san-pham',
        component: () => import('../components/SanPhamManager.vue'),
        meta: { parent: 'Quản lý sản phẩm', title: 'Sản phẩm' },
      },
      // Thêm / sửa sản phẩm là một trang riêng (có nút "Quay lại danh sách"), giống video
      {
        path: 'san-pham/them',
        name: 'san-pham-them',
        component: () => import('../components/sanPham/SanPhamFormPage.vue'),
        meta: { parent: 'Sản phẩm', parentTo: '/san-pham', title: 'Thêm sản phẩm' },
      },
      {
        path: 'san-pham/:id(\\d+)/sua',
        name: 'san-pham-sua',
        component: () => import('../components/sanPham/SanPhamFormPage.vue'),
        meta: { parent: 'Sản phẩm', parentTo: '/san-pham', title: 'Chỉnh sửa sản phẩm' },
      },
      {
        path: 'san-pham/bien-the',
        name: 'bien-the-san-pham',
        component: () => import('../components/BienTheManager.vue'),
        meta: { parent: 'Quản lý sản phẩm', title: 'Biến thể sản phẩm' },
      },
      // 9 bảng thuộc tính (danh mục, thương hiệu, xuất xứ, chất liệu, độ cứng, điểm cân bằng, màu sắc, trọng lượng, chu vi)
      // dùng chung một trang; loại thuộc tính lấy từ meta.attr.
      ...THUOC_TINH_LIST.map((t) => ({
        path: `san-pham/${t.slug}`,
        name: `thuoc-tinh-${t.slug}`,
        component: () => import('../components/ThuocTinhManager.vue'),
        meta: { parent: 'Danh sách thuộc tính', title: t.label, attr: t.slug },
      })),
      {
        path: 'khach-hang',
        name: 'khach-hang',
        component: () => import('../components/KhachHangManager.vue'),
        meta: { title: 'Khách hàng' },
      },
      // Thêm / sửa khách hàng là trang riêng (có nút "Quay lại"); form sửa kiểm tra dữ liệu y hệt form thêm
      {
        path: 'khach-hang/them',
        name: 'khach-hang-them',
        component: () => import('../components/khachHang/KhachHangFormPage.vue'),
        meta: { parent: 'Khách hàng', parentTo: '/khach-hang', title: 'Thêm khách hàng' },
      },
      {
        path: 'khach-hang/:id(\\d+)/sua',
        name: 'khach-hang-sua',
        component: () => import('../components/khachHang/KhachHangFormPage.vue'),
        meta: { parent: 'Khách hàng', parentTo: '/khach-hang', title: 'Chỉnh sửa khách hàng' },
      },
      {
        path: 'dot-giam-gia',
        name: 'dot-giam-gia',
        component: () => import('../components/DotGiamGiaManager.vue'),
        meta: { parent: 'Quản lý giảm giá', title: 'Đợt giảm giá' },
      },
      {
        path: 'dot-giam-gia/them',
        name: 'dot-giam-gia-them',
        component: () => import('../components/dotGiamGia/DotGiamGiaFormPage.vue'),
        meta: { parent: 'Đợt giảm giá', parentTo: '/dot-giam-gia', title: 'Thêm đợt giảm giá' },
      },
      {
        path: 'dot-giam-gia/:id(\\d+)',
        name: 'dot-giam-gia-chi-tiet',
        component: () => import('../components/dotGiamGia/DotGiamGiaFormPage.vue'),
        meta: { parent: 'Đợt giảm giá', parentTo: '/dot-giam-gia', title: 'Chi tiết đợt giảm giá' },
      },
      {
        path: 'phieu-giam-gia',
        name: 'phieu-giam-gia',
        component: () => import('../components/PhieuGiamGiaManager.vue'),
        meta: { parent: 'Quản lý giảm giá', title: 'Phiếu giảm giá' },
      },
      {
        path: 'phieu-giam-gia/them',
        name: 'phieu-giam-gia-them',
        component: () => import('../components/phieuGiamGia/PhieuGiamGiaFormPage.vue'),
        meta: { parent: 'Phiếu giảm giá', parentTo: '/phieu-giam-gia', title: 'Thêm phiếu giảm giá' },
      },
      {
        path: 'phieu-giam-gia/:id(\\d+)/sua',
        name: 'phieu-giam-gia-sua',
        component: () => import('../components/phieuGiamGia/PhieuGiamGiaFormPage.vue'),
        meta: { parent: 'Phiếu giảm giá', parentTo: '/phieu-giam-gia', title: 'Chỉnh sửa phiếu giảm giá' },
      },
      {
        path: 'nhan-vien',
        name: 'nhan-vien',
        component: () => import('../components/NhanVienManager.vue'),
        meta: { title: 'Nhân viên' },
      },
      // Thêm nhân viên và xem / sửa chi tiết là trang riêng, giống video
      {
        path: 'nhan-vien/them',
        name: 'nhan-vien-them',
        component: () => import('../components/nhanVien/NhanVienFormPage.vue'),
        meta: { parent: 'Nhân viên', parentTo: '/nhan-vien', title: 'Thêm nhân viên' },
      },
      {
        path: 'nhan-vien/:id(\\d+)',
        name: 'nhan-vien-chi-tiet',
        component: () => import('../components/nhanVien/NhanVienFormPage.vue'),
        meta: { parent: 'Nhân viên', parentTo: '/nhan-vien', title: 'Chi tiết nhân viên' },
      },
    ],
  },
  {
    path: '/shop',
    component: ClientLayout,
    children: [
      {
        path: '',
        name: 'shop-home',
        component: () => import('../views/client/HomeView.vue'),
        meta: { title: 'Trang chủ - FootStyle' },
      },
      {
        path: 'san-pham',
        name: 'shop-catalog',
        component: () => import('../views/client/ProductCatalogView.vue'),
        meta: { title: 'Tất cả sản phẩm - FootStyle' },
      },
      {
        path: 'san-pham/:id(\\d+)',
        name: 'shop-product-detail',
        component: () => import('../views/client/ProductDetailView.vue'),
        meta: { title: 'Chi tiết sản phẩm - FootStyle' },
      },
      {
        path: 'gio-hang',
        name: 'shop-cart',
        component: () => import('../views/client/CartView.vue'),
        meta: { title: 'Giỏ hàng của bạn' },
      },
      {
        path: 'thanh-toan',
        name: 'shop-checkout',
        component: () => import('../views/client/CheckoutView.vue'),
        meta: { title: 'Thanh toán đơn hàng' },
      },
      {
        path: 'dat-hang-thanh-cong',
        name: 'shop-order-success',
        component: () => import('../views/client/OrderSuccessView.vue'),
        meta: { title: 'Đặt hàng thành công' },
      },
      {
        path: 'tra-cuu',
        name: 'shop-order-lookup',
        component: () => import('../views/client/OrderLookupView.vue'),
        meta: { title: 'Tra cứu đơn hàng' },
      },
      {
        path: 'tai-khoan',
        name: 'shop-account',
        component: () => import('../views/client/AccountView.vue'),
        meta: { title: 'Tài khoản cá nhân' },
      },
      {
        path: 'tin-tuc',
        name: 'shop-news',
        component: () => import('../views/client/NewsView.vue'),
        meta: { title: 'Tin tức & Cẩm nang - FootStyle' },
      },
      {
        path: 'tuyen-dung',
        name: 'shop-recruitment',
        component: () => import('../views/client/RecruitmentView.vue'),
        meta: { title: 'Tuyển dụng - FootStyle' },
      },
      {
        path: 'nhuong-quyen',
        name: 'shop-franchise',
        component: () => import('../views/client/FranchiseView.vue'),
        meta: { title: 'Chính sách nhượng quyền - FootStyle' },
      },
      {
        path: 'huong-dan',
        name: 'shop-guide',
        component: () => import('../views/client/GuideView.vue'),
        meta: { title: 'Hướng dẫn & Hỗ trợ - FootStyle' },
      },
      {
        path: 'gioi-thieu',
        name: 'shop-about',
        component: () => import('../views/client/AboutView.vue'),
        meta: { title: 'Giới thiệu về FootStyle' },
      },
      {
        path: 'lien-he',
        name: 'shop-contact',
        component: () => import('../views/client/ContactView.vue'),
        meta: { title: 'Liên hệ & Cửa hàng - FootStyle' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/hoa-don' },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
