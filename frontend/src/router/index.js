import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { THUOC_TINH_LIST } from '../constants/thuocTinh'

// meta.title / meta.parent: hiển thị trên header (kiểu "Quản lý giảm giá / Phiếu giảm giá").
// Trang hóa đơn có thẻ tiêu đề riêng nên không khai báo meta.
const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/hoa-don',
    children: [
      {
        path: 'hoa-don',
        name: 'hoa-don',
        component: () => import('../components/HoaDonManager.vue'),
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
        meta: { parent: 'Quản lý sản phẩm', title: t.label, attr: t.slug },
      })),
      {
        path: 'khach-hang',
        name: 'khach-hang',
        component: () => import('../components/KhachHangManager.vue'),
        meta: { title: 'Khách hàng' },
      },
      // Thêm khách hàng là một trang riêng (có nút "Quay lại")
      {
        path: 'khach-hang/them',
        name: 'khach-hang-them',
        component: () => import('../components/KhachHangAdd.vue'),
        meta: { parent: 'Khách hàng', parentTo: '/khach-hang', title: 'Thêm khách hàng' },
      },
      {
        path: 'dot-giam-gia',
        name: 'dot-giam-gia',
        component: () => import('../components/DotGiamGiaManager.vue'),
        meta: { parent: 'Quản lý giảm giá', title: 'Đợt giảm giá' },
      },
      {
        path: 'phieu-giam-gia',
        name: 'phieu-giam-gia',
        component: () => import('../components/PhieuGiamGiaManager.vue'),
        meta: { parent: 'Quản lý giảm giá', title: 'Phiếu giảm giá' },
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
  { path: '/:pathMatch(.*)*', redirect: '/hoa-don' },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
