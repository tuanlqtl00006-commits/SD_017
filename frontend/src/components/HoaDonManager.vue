<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">
    <Sidebar />

feature/quan-ly-hoa-don

    <div class="bg-white border-end d-flex flex-column" style="width: 320px;">


      <div class="p-3 text-center mb-3 d-flex align-items-center justify-content-center" style="height: 130px;">
        <img src="/logo.png" alt="FootStyle" class="img-fluid" style="width: 95%; object-fit: contain; transform: scale(1.1);">
      </div>


<!-- Menu List -->
<ul class="nav flex-column fw-medium px-2 sidebar-menu" style="font-size: 0.95rem; gap: 0.2rem;">

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link">
            <i class="bi bi-grid icon"></i> Thống kê
          </a>
        </li>

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link">
            <i class="bi bi-shop icon"></i> Bán hàng tại quầy
          </a>
        </li>

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link active">
            <i class="bi bi-receipt icon"></i> Quản lý hóa đơn
          </a>
        </li>

        <!-- Quản lý sản phẩm (Đang mở) -->
        <li class="nav-item">
          <a href="#menuSanPham" data-bs-toggle="collapse" class="nav-link sidebar-link d-flex justify-content-between align-items-center" aria-expanded="true">
            <div><i class="bi bi-box-seam icon"></i> Quản lý sản phẩm</div>
            <i class="bi bi-chevron-down chevron"></i>
          </a>
          <div class="collapse show" id="menuSanPham">
            <ul class="nav flex-column mt-1 submenu">
              <li class="nav-item">
                <a href="#" class="nav-link sidebar-link"><i class="bi bi-box icon"></i> Sản phẩm</a>
              </li>
              <li class="nav-item">
                <a href="#" class="nav-link sidebar-link"><i class="bi bi-layers icon"></i> Biến thể sản phẩm</a>
              </li>
            </ul>
          </div>
        </li>

        <!-- Danh sách thuộc tính (Đang đóng) -->
        <li class="nav-item">
          <a href="#menuThuocTinh" data-bs-toggle="collapse" class="nav-link sidebar-link d-flex justify-content-between align-items-center collapsed" aria-expanded="false">
            <div><i class="bi bi-hexagon icon"></i> Danh sách thuộc tính</div>
            <i class="bi bi-chevron-down chevron"></i>
          </a>
          <div class="collapse" id="menuThuocTinh"></div>
        </li>

        <!-- Quản lý giảm giá (Đang mở) -->
        <li class="nav-item">
          <a href="#menuGiamGia" data-bs-toggle="collapse" class="nav-link sidebar-link d-flex justify-content-between align-items-center" aria-expanded="true">
            <div><i class="bi bi-percent icon"></i> Quản lý giảm giá</div>
            <i class="bi bi-chevron-down chevron"></i>
          </a>
          <div class="collapse show" id="menuGiamGia">
            <ul class="nav flex-column mt-1 submenu">
              <li class="nav-item">
                <a href="#" class="nav-link sidebar-link"><i class="bi bi-ticket-detailed icon"></i> Phiếu giảm giá</a>
              </li>
              <li class="nav-item">
                <a href="#" class="nav-link sidebar-link"><i class="bi bi-tag icon"></i> Đợt giảm giá</a>
              </li>
            </ul>
          </div>
        </li>

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link">
            <i class="bi bi-people icon"></i> Quản lý khách hàng
          </a>
        </li>

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link">
            <i class="bi bi-chat-dots icon"></i> Hỗ trợ trực tuyến
          </a>
        </li>

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link">
            <i class="bi bi-star icon"></i> Quản lý đánh giá
          </a>
        </li>

        <li class="nav-item">
          <a href="#" class="nav-link sidebar-link">
            <i class="bi bi-person icon"></i> Quản lý nhân viên
          </a>
        </li>

        <!-- Quản lý lịch làm (Đang đóng) -->
        <li class="nav-item mb-4">
          <a href="#menuLichLam" data-bs-toggle="collapse" class="nav-link sidebar-link d-flex justify-content-between align-items-center collapsed" aria-expanded="false">
            <div><i class="bi bi-calendar3 icon"></i> Quản lý lịch làm</div>
            <i class="bi bi-chevron-down chevron"></i>
          </a>
          <div class="collapse" id="menuLichLam"></div>
        </li>

      </ul>
    </div>


    <div class="flex-grow-1 d-flex flex-column">
      <Header />


      <header class="bg-white border-bottom p-3 d-flex justify-content-end align-items-center gap-3">
        <button class="btn btn-light border rounded-circle p-2 lh-1 text-muted"><i class="bi bi-moon"></i></button>
        <button class="btn btn-light border rounded-circle p-2 lh-1 text-muted dropdown-toggle text-decoration-none" data-bs-toggle="dropdown">
          <i class="bi bi-person"></i>
        </button>
      </header>
    <div class="flex-grow-1 d-flex flex-column">
      <Header />

      <main class="p-4">

        <div class="bg-white border rounded-3 shadow-sm py-3 px-4 mb-4">
          <h5 class="fw-bold text-primary m-0">Quản lý hóa đơn</h5>
        </div>


        <div class="card border-0 shadow-sm rounded-4 mb-4">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-3 d-flex align-items-center"><i class="bi bi-funnel text-muted me-2 fs-5"></i> Bộ Lọc</h6>
            <div class="row g-3">
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Mã hóa đơn</label>
                <input type="text" class="form-control" placeholder="Nhập mã hóa đơn">
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Ngày Bắt Đầu</label>
                <input type="date" class="form-control text-muted" value="2025-02-20">
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Ngày Kết Thúc</label>
                <input type="date" class="form-control text-muted">
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Loại Đơn</label>
                <select class="form-select text-muted">
                  <option selected>Loại Đơn</option>
                  <option value="1">Tại cửa hàng</option>
                  <option value="2">Online</option>
                </select>
              </div>
            </div>
            <div class="d-flex justify-content-end gap-3 mt-4">
              <button class="btn btn-primary px-4 shadow-sm">Tìm kiếm</button>
              <button class="btn btn-danger px-4 shadow-sm">Làm mới</button>
              <button class="btn btn-success px-4 shadow-sm"><i class="bi bi-file-earmark-arrow-down me-1"></i> Xuất File</button>
            </div>
          </div>
        </div>


        <div class="card rounded-3 shadow-sm bg-white" style="border: 2px solid #5b8deb;">
          <div class="card-body p-0">


            <div class="d-flex align-items-center px-4 pt-4 pb-3">
               <div class="bg-secondary bg-opacity-25 text-primary rounded-3 d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px;">
                 <i class="bi bi-file-earmark-text fs-4 text-secondary"></i>
               </div>
               <h4 class="fw-bold m-0 text-dark" style="font-size: 1.25rem;">Danh Sách Hóa Đơn</h4>
            </div>


            <ul class="nav custom-tabs d-flex justify-content-between flex-nowrap overflow-auto px-4 w-100" style="border-bottom: 1px solid #e9ecef;">
              <li class="nav-item">
                <a class="nav-link active text-center px-2" href="#">Tất Cả</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" href="#">Chờ Xác Nhận</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" href="#">Đã Xác Nhận</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" href="#">Chờ Vận Chuyển</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" href="#">Vận Chuyển</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" href="#">Đã Hoàn Thành</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" href="#">Hủy</a>
              </li>
            </ul>


            <div class="table-responsive">
              <table class="table align-middle m-0 text-start">
                <thead style="background-color: #f4f6f9;">
                  <tr>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 80px;">STT</th>
                    <th class="py-3 fw-bold text-dark border-0">Mã Hóa Đơn</th>
                    <th class="py-3 fw-bold text-dark border-0">Tên Khách Hàng</th>
                    <th class="py-3 fw-bold text-dark border-0">Tên Nhân Viên</th>
                    <th class="py-3 fw-bold text-dark border-0">Tổng Tiền</th>
                    <th class="py-3 fw-bold text-dark border-0">Ngày Tạo</th>
                    <th class="py-3 fw-bold text-dark border-0">Loại Đơn</th>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 120px;">Hành Động</th>
                  </tr>
                </thead>
                <tbody class="text-dark">
                  <tr>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">1</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">HD0037</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">No name</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">Admin 1</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">2432424</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">06/03/2025<br>08:00:00</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">Tại cửa hàng</td>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <a href="#" class="text-secondary"><i class="bi bi-eye fs-5"></i></a>
                    </td>
                  </tr>
                  <tr>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">2</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">HD328</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">No name</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">Admin 2</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">32423</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">08/03/2025<br>20:45:00</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">Online</td>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <a href="#" class="text-secondary"><i class="bi bi-eye fs-5"></i></a>
                    </td>
                  </tr>
                  <tr>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">3</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">HD0028</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">No name</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">Admin 3</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">222222</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">20/03/2025<br>19:00:00</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">Online</td>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <a href="#" class="text-secondary"><i class="bi bi-eye fs-5"></i></a>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>


            <div class="d-flex justify-content-end px-4 py-4 mt-2">
              <nav>
                <ul class="pagination custom-pagination m-0 gap-2">
                  <li class="page-item disabled">
                    <a class="page-link rounded-2 bg-white" href="#"><i class="bi bi-chevron-left"></i></a>
                  </li>
                  <li class="page-item active">
                    <a class="page-link rounded-2 bg-white text-dark shadow-sm" style="border: 1px solid #dee2e6;" href="#">1</a>
                  </li>
                  <li class="page-item">
                    <a class="page-link rounded-2 bg-white" href="#"><i class="bi bi-chevron-right"></i></a>
                  </li>
                </ul>
              </nav>
            </div>

          </div>
        </div>

      </main>
    </div>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>

<style scoped>
/* Tùy chỉnh Menu Sidebar chung */
.sidebar-menu .sidebar-link {
color: #495057;
padding: 0.7rem 1rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  text-decoration: none;
  transition: all 0.2s ease-in-out;
}

.sidebar-menu .sidebar-link .icon {
  font-size: 1.15rem;
  margin-right: 0.8rem;
  color: #6c757d;
  width: 24px;
  text-align: center;
}

/* Hover: Đổi sang màu xanh dương */
.sidebar-menu .sidebar-link:hover:not(.active) {
  background-color: #f8f9fa;
  color: #0d6efd;
}

.sidebar-menu .sidebar-link:hover:not(.active) .icon,
.sidebar-menu .sidebar-link:hover:not(.active) .chevron {
  color: #0d6efd;
}

/* Trạng thái Active (Nền xanh dương, chữ trắng) */
.sidebar-menu .sidebar-link.active {
  background-color: #0d6efd !important;
  color: white !important;
  font-weight: 600;
  box-shadow: 0 0.125rem 0.25rem rgba(13, 110, 253, 0.2);
}

.sidebar-menu .sidebar-link.active .icon {
  color: white !important;
}

/* Submenu (Menu con) */
.submenu .sidebar-link {
  padding: 0.6rem 1rem 0.6rem 2.8rem; /* Thụt lề vào */
  font-size: 0.9rem;
  color: #6c757d;
}

.submenu .sidebar-link .icon {
  font-size: 1rem;
  margin-right: 0.6rem;
  color: #adb5bd;
}

/* Mũi tên Dropdown */
.sidebar-menu .sidebar-link .chevron {
  transition: transform 0.3s ease;
  font-size: 0.75rem;
  color: #6c757d;
}

/* Khi dropdown mở: Xoay mũi tên & đổi màu xanh dương */
.sidebar-menu .sidebar-link:not(.collapsed) .chevron {
transform: rotate(180deg);
  color: #0d6efd;
}
/* Form Input */
.form-control::placeholder {
  color: #ced4da;
}

/* Tùy chỉnh Tabs giống hình */
.custom-tabs .nav-link {
  color: #858796;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 1rem 0.5rem;
  font-weight: 500;
  font-size: 0.95rem;
}
.custom-tabs .nav-link.active {
  color: #0d6efd !important;
  border-bottom: 2px solid #0d6efd;
  font-weight: 600;
}
.custom-tabs .nav-link:hover {
  color: #0d6efd;
}

/* Tùy chỉnh Pagination giống hình */
.custom-pagination .page-link {
  color: #495057;
  border: 1px solid #e9ecef;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}
.custom-pagination .page-item.disabled .page-link {
  color: #adb5bd;
  background-color: #fff;
}
</style>
