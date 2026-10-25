import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import ClientLayout from '../layouts/ClientLayout.vue'
import { THUOC_TINH_LIST } from '../constants/thuocTinh'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { useGiaoCa } from '../composables/useGiaoCa'
import { duongDanQuayLaiAnToan } from '../services/phien'

// meta.roles: vai trò được vào trang ('quan-ly' | 'nhan-vien'); meta.congKhai: trang không cần đăng nhập.
const QL = ['quan-ly']
const NV = ['nhan-vien', 'quan-ly']

// meta.title / meta.parent: hiển thị trên header (kiểu "Quản lý giảm giá / Phiếu giảm giá").
const routes = [
  // Link riêng vào trang quản trị: /admin (nếu đã đăng nhập thì vào trang chủ nghiệp vụ, chưa thì vào /dang-nhap)
  {
    path: '/admin',
    name: 'admin',
    redirect: () => {
      const auth = useAuth()
      return auth.daDangNhap.value ? auth.trangChu() : '/dang-nhap'
    },
  },
  {
    path: '/admin/dang-nhap',
    redirect: '/dang-nhap',
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      // Mở link gốc trang web (/) chuyển thẳng sang Cửa hàng online (/shop) riêng biệt, không vào trang đăng nhập
      {
        path: '',
        name: 'trang-chu-goc',
        redirect: '/shop',
        meta: { congKhai: true },
      },
      {
        path: 'thong-ke',
        name: 'thong-ke',
        component: () => import('../components/ThongKe.vue'),
        meta: { title: 'Thống kê', roles: QL },
      },
      {
        path: 'thong-ke/so-sanh',
        name: 'thong-ke-so-sanh',
        component: () => import('../components/thongKe/ThongKeSoSanh.vue'),
        meta: { parent: 'Thống kê', parentTo: '/thong-ke', title: 'So sánh doanh thu', roles: QL },
      },
      {
        path: 'hoa-don',
        name: 'hoa-don',
        component: () => import('../components/HoaDonManager.vue'),
        meta: { title: 'Hóa đơn', roles: NV },
      },
      {
        path: 'hoa-don/:id',
        name: 'hoa-don-chi-tiet',
        component: () => import('../components/HoaDonChiTiet.vue'),
        meta: { parent: 'Hóa đơn', parentTo: '/hoa-don', title: 'Chi tiết hóa đơn', roles: NV },
      },
      {
        path: 'san-pham',
        name: 'san-pham',
        component: () => import('../components/SanPhamManager.vue'),
        meta: { parent: 'Quản lý sản phẩm', title: 'Sản phẩm', roles: QL },
      },
      // Thêm / sửa sản phẩm là một trang riêng (có nút "Quay lại danh sách"), giống video
      {
        path: 'san-pham/them',
        name: 'san-pham-them',
        component: () => import('../components/sanPham/SanPhamFormPage.vue'),
        meta: { parent: 'Sản phẩm', parentTo: '/san-pham', title: 'Thêm sản phẩm', roles: QL },
      },
      {
        path: 'san-pham/:id(\\d+)/sua',
        name: 'san-pham-sua',
        component: () => import('../components/sanPham/SanPhamFormPage.vue'),
        meta: { parent: 'Sản phẩm', parentTo: '/san-pham', title: 'Chỉnh sửa sản phẩm', roles: QL },
      },
      {
        path: 'san-pham/bien-the',
        name: 'bien-the-san-pham',
        component: () => import('../components/BienTheManager.vue'),
        meta: { parent: 'Quản lý sản phẩm', title: 'Biến thể sản phẩm', roles: QL },
      },
      // 9 bảng thuộc tính (danh mục, thương hiệu, xuất xứ, chất liệu, độ cứng, điểm cân bằng, màu sắc, trọng lượng, chu vi)
      // dùng chung một trang; loại thuộc tính lấy từ meta.attr.
      ...THUOC_TINH_LIST.map((t) => ({
        path: `san-pham/${t.slug}`,
        name: `thuoc-tinh-${t.slug}`,
        component: () => import('../components/ThuocTinhManager.vue'),
        meta: { parent: 'Danh sách thuộc tính', title: t.label, attr: t.slug, roles: QL },
      })),
      {
        path: 'khach-hang',
        name: 'khach-hang',
        component: () => import('../components/KhachHangManager.vue'),
        meta: { title: 'Khách hàng', roles: NV },
      },
      // Thêm / sửa khách hàng là trang riêng (có nút "Quay lại"); form sửa kiểm tra dữ liệu y hệt form thêm
      {
        path: 'khach-hang/them',
        name: 'khach-hang-them',
        component: () => import('../components/khachHang/KhachHangFormPage.vue'),
        meta: { parent: 'Khách hàng', parentTo: '/khach-hang', title: 'Thêm khách hàng', roles: NV },
      },
      {
        path: 'khach-hang/:id(\\d+)/sua',
        name: 'khach-hang-sua',
        component: () => import('../components/khachHang/KhachHangFormPage.vue'),
        meta: { parent: 'Khách hàng', parentTo: '/khach-hang', title: 'Chỉnh sửa khách hàng', roles: NV },
      },
      {
        path: 'dot-giam-gia',
        name: 'dot-giam-gia',
        component: () => import('../components/DotGiamGiaManager.vue'),
        meta: { parent: 'Quản lý giảm giá', title: 'Đợt giảm giá', roles: QL },
      },
      {
        path: 'dot-giam-gia/them',
        name: 'dot-giam-gia-them',
        component: () => import('../components/dotGiamGia/DotGiamGiaFormPage.vue'),
        meta: { parent: 'Đợt giảm giá', parentTo: '/dot-giam-gia', title: 'Thêm đợt giảm giá', roles: QL },
      },
      {
        path: 'dot-giam-gia/:id(\\d+)',
        name: 'dot-giam-gia-chi-tiet',
        component: () => import('../components/dotGiamGia/DotGiamGiaFormPage.vue'),
        meta: { parent: 'Đợt giảm giá', parentTo: '/dot-giam-gia', title: 'Chi tiết đợt giảm giá', roles: QL },
      },
      {
        path: 'phieu-giam-gia',
        name: 'phieu-giam-gia',
        component: () => import('../components/PhieuGiamGiaManager.vue'),
        meta: { parent: 'Quản lý giảm giá', title: 'Phiếu giảm giá', roles: QL },
      },
      {
        path: 'phieu-giam-gia/them',
        name: 'phieu-giam-gia-them',
        component: () => import('../components/phieuGiamGia/PhieuGiamGiaFormPage.vue'),
        meta: { parent: 'Phiếu giảm giá', parentTo: '/phieu-giam-gia', title: 'Thêm phiếu giảm giá', roles: QL },
      },
      {
        path: 'phieu-giam-gia/:id(\\d+)/sua',
        name: 'phieu-giam-gia-sua',
        component: () => import('../components/phieuGiamGia/PhieuGiamGiaFormPage.vue'),
        meta: { parent: 'Phiếu giảm giá', parentTo: '/phieu-giam-gia', title: 'Chỉnh sửa phiếu giảm giá', roles: QL },
      },
      {
        path: 'nhan-vien',
        name: 'nhan-vien',
        component: () => import('../components/NhanVienManager.vue'),
        meta: { title: 'Nhân viên', roles: QL },
      },
      // Thêm nhân viên và xem / sửa chi tiết là trang riêng, giống video
      {
        path: 'nhan-vien/them',
        name: 'nhan-vien-them',
        component: () => import('../components/nhanVien/NhanVienFormPage.vue'),
        meta: { parent: 'Nhân viên', parentTo: '/nhan-vien', title: 'Thêm nhân viên', roles: QL },
      },
      {
        path: 'nhan-vien/:id(\\d+)',
        name: 'nhan-vien-chi-tiet',
        component: () => import('../components/nhanVien/NhanVienFormPage.vue'),
        meta: { parent: 'Nhân viên', parentTo: '/nhan-vien', title: 'Chi tiết nhân viên', roles: QL },
      },
      // ===== Giai đoạn 3 =====
      {
        path: 'ban-hang',
        name: 'ban-hang',
        component: () => import('../components/banHang/BanHangPage.vue'),
        meta: { title: 'Bán hàng tại quầy', roles: NV },
      },
      // ===== Ca làm việc / giao ca (nhận ca, giao ca ở nút "Ca làm việc" trên header) =====
      {
        path: 'giao-ca',
        name: 'giao-ca',
        component: () => import('../components/giaoCa/GiaoCaLichSuPage.vue'),
        meta: { parent: 'Ca làm việc', title: 'Lịch sử giao ca', roles: NV },
      },
      {
        path: 'lich-cua-toi',
        name: 'lich-cua-toi',
        component: () => import('../components/lichLamViec/LichCuaToiPage.vue'),
        meta: { parent: 'Ca làm việc', title: 'Lịch làm của tôi', roles: NV },
      },
      {
        path: 'lich-lam-viec',
        name: 'lich-lam-viec',
        component: () => import('../components/lichLamViec/LichLamViecPage.vue'),
        meta: { parent: 'Ca làm việc', title: 'Xếp lịch làm việc', roles: QL },
      },
      // ===== Trò chuyện: hỗ trợ khách (trợ lý ảo chuyển sang) + chat nội bộ nhân viên =====
      {
        path: 'ho-tro-truc-tuyen',
        name: 'ho-tro-truc-tuyen',
        component: () => import('../components/chat/HoTroTrucTuyenPage.vue'),
        meta: { parent: 'Trò chuyện', title: 'Hỗ trợ khách hàng', roles: NV },
      },
      {
        path: 'chat-noi-bo',
        name: 'chat-noi-bo',
        component: () => import('../components/chat/ChatNoiBoPage.vue'),
        meta: { parent: 'Trò chuyện', title: 'Chat nội bộ', roles: NV },
      },
    ],
  },
  {
    path: '/ho-tro',
    name: 'ho-tro-khach',
    component: () => import('../components/chat/HoTroKhachPage.vue'),
    meta: { congKhai: true, title: 'Hỗ trợ trực tuyến - FootStyle' },
  },
  // Bán hàng online cho khách (Giao diện chuẩn nhóm)
  {
    path: '/shop',
    component: ClientLayout,
    meta: { congKhai: true },
    children: [
      {
        path: '',
        name: 'shop-home',
        component: () => import('../views/client/HomeView.vue'),
        meta: { congKhai: true, title: 'Trang chủ - FootStyle' },
      },
      {
        path: 'san-pham',
        name: 'shop-catalog',
        component: () => import('../views/client/ProductCatalogView.vue'),
        meta: { congKhai: true, title: 'Tất cả sản phẩm - FootStyle' },
      },
      {
        path: 'san-pham/:id(\\d+)',
        name: 'shop-product-detail',
        component: () => import('../views/client/ProductDetailView.vue'),
        meta: { congKhai: true, title: 'Chi tiết sản phẩm - FootStyle' },
      },
      {
        path: 'gio-hang',
        name: 'shop-cart',
        component: () => import('../views/client/CartView.vue'),
        meta: { congKhai: true, title: 'Giỏ hàng của bạn' },
      },
      {
        path: 'thanh-toan',
        name: 'shop-checkout',
        component: () => import('../views/client/CheckoutView.vue'),
        meta: { congKhai: true, title: 'Thanh toán đơn hàng' },
      },
      {
        path: 'dat-hang-thanh-cong',
        name: 'shop-order-success',
        component: () => import('../views/client/OrderSuccessView.vue'),
        meta: { congKhai: true, title: 'Đặt hàng thành công' },
      },
      {
        path: 'tra-cuu',
        name: 'shop-order-lookup',
        component: () => import('../views/client/OrderLookupView.vue'),
        meta: { congKhai: true, title: 'Tra cứu đơn hàng' },
      },
      {
        path: 'dang-nhap',
        name: 'shop-dang-nhap',
        component: () => import('../views/client/KhachDangNhapView.vue'),
        meta: { congKhai: true, title: 'Đăng nhập khách hàng - FootStyle' },
      },
      {
        path: 'goi-y-ai',
        name: 'shop-goi-y-ai',
        component: () => import('../views/client/GoiYAiView.vue'),
        meta: { congKhai: true, title: 'Gợi ý AI từ hình ảnh - FootStyle' },
      },
      {
        path: 'tai-khoan',
        name: 'shop-account',
        component: () => import('../views/client/AccountView.vue'),
        meta: { congKhai: true, title: 'Tài khoản cá nhân' },
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
  {
    path: '/gio-hang',
    redirect: '/shop/gio-hang',
    meta: { congKhai: true },
  },
  {
    path: '/thanh-toan',
    redirect: '/shop/thanh-toan',
    meta: { congKhai: true },
  },
  // Vào ca: trang riêng (không có menu), nhân viên chưa vào ca thì chỉ ở trang này
  {
    path: '/vao-ca',
    name: 'vao-ca',
    component: () => import('../components/giaoCa/VaoCaPage.vue'),
    meta: { title: 'Vào ca làm việc', roles: NV, khongCanVaoCa: true },
  },
  // Trang không cần đăng nhập
  {
    path: '/dang-nhap',
    name: 'dang-nhap',
    component: () => import('../components/auth/DangNhapPage.vue'),
    meta: { congKhai: true, title: 'Đăng nhập hệ thống' },
  },
  // Chat của khách mở riêng một trang (trợ lý ảo trả lời trước, cần thì gặp nhân viên); trang shop cũng có nút chat nổi
  {
    path: '/ho-tro',
    name: 'ho-tro-khach',
    component: () => import('../components/chat/HoTroKhachPage.vue'),
    meta: { congKhai: true, title: 'Hỗ trợ trực tuyến - FootStyle' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => {
      const auth = useAuth()
      return auth.daDangNhap.value ? auth.trangChu() : '/shop'
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * Kiểm tra trước khi vào trang (giai đoạn 3):
 *  - trang công khai (đăng nhập, chat của khách): vào thẳng; đã đăng nhập mà mở trang đăng nhập thì chuyển vào trong;
 *  - chưa đăng nhập: về /dang-nhap?quayLai=trang-đang-mở;
 *  - không đủ quyền (vd nhân viên mở /thong-ke): báo lỗi và về trang mở đầu của vai trò.
 */
export async function kiemTraTruyCap(to) {
  const auth = useAuth()
  await auth.khoiTao()
  if (to.meta?.congKhai || to.matched?.some((record) => record.meta?.congKhai) || to.path?.startsWith('/shop')) {
    if (to.name === 'dang-nhap' && auth.daDangNhap.value) {
      return duongDanQuayLaiAnToan(to.query.quayLai) || auth.trangChu()
    }
    return true
  }
  if (!auth.daDangNhap.value) {
    return { path: '/dang-nhap', query: to.fullPath && to.fullPath !== '/' ? { quayLai: to.fullPath } : {} }
  }
  const roles = to.meta.roles
  if (roles && !roles.includes(auth.vaiTro.value)) {
    useToast().error('Tài khoản của bạn không có quyền vào trang này.')
    return auth.trangChu()
  }
  // Nhân viên bắt buộc vào ca trước khi làm bất cứ việc gì (backend cũng chặn API bằng mã CHUA_NHAN_CA)
  if (auth.vaiTro.value === 'nhan-vien' && !to.meta?.khongCanVaoCa) {
    const giaoCa = useGiaoCa()
    if (!giaoCa.trangThai.daTai) await giaoCa.taiLai()
    if (giaoCa.trangThai.daTai && !giaoCa.dangTrongCa.value) {
      return { path: '/vao-ca', query: to.fullPath && to.fullPath !== '/' ? { quayLai: to.fullPath } : {} }
    }
  }
  return true
}

router.beforeEach(kiemTraTruyCap)

export default router
