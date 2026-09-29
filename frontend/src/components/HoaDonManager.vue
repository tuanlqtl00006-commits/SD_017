<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">

    <!-- SIDEBAR -->
    <div class="bg-white border-end d-flex flex-column" style="width: 260px;">
      <div class="p-3 text-center border-bottom border-dashed">
        <img src="/logo.png" alt="FootStyle" class="img-fluid px-2" style="max-height: 90px; object-fit: contain;">
      </div>
      <ul class="nav flex-column py-3 fw-medium text-secondary">
        <li class="nav-item">
          <a href="#" class="nav-link text-secondary px-4 py-3"><i class="bi bi-grid me-2"></i> Tổng quan</a>
        </li>
        <li class="nav-item">
          <!-- Active state: Nền xanh nhạt, viền phải xanh đậm -->
          <a href="#" class="nav-link active px-4 py-3 text-primary" style="background-color: #e6f0ff; border-right: 4px solid #0d6efd;">
            <i class="bi bi-bag-check me-2"></i> Quản Lý Hóa Đơn
          </a>
        </li>
        <li class="nav-item"><a href="#" class="nav-link text-secondary px-4 py-3"><i class="bi bi-cart me-2"></i> Bán hàng tại quầy</a></li>
        <li class="nav-item d-flex justify-content-between align-items-center px-4 py-3 text-secondary" style="cursor: pointer;">
          <div><i class="bi bi-box-seam me-2"></i> Quản Lý Sản Phẩm</div> <i class="bi bi-chevron-down small"></i>
        </li>
        <li class="nav-item d-flex justify-content-between align-items-center px-4 py-3 text-secondary" style="cursor: pointer;">
          <div><i class="bi bi-people me-2"></i> Quản Lý Tài Khoản</div> <i class="bi bi-chevron-down small"></i>
        </li>
        <li class="nav-item d-flex justify-content-between align-items-center px-4 py-3 text-secondary" style="cursor: pointer;">
          <div><i class="bi bi-tag me-2"></i> Khuyến Mãi</div> <i class="bi bi-chevron-down small"></i>
        </li>
      </ul>
    </div>

    <!-- MAIN CONTENT -->
    <div class="flex-grow-1 d-flex flex-column">

      <!-- HEADER -->
      <header class="bg-white border-bottom p-3 d-flex justify-content-end align-items-center gap-3">
        <button class="btn btn-light border rounded-circle p-2 lh-1 text-muted"><i class="bi bi-moon"></i></button>
        <button class="btn btn-light border rounded-circle p-2 lh-1 text-muted dropdown-toggle text-decoration-none" data-bs-toggle="dropdown">
          <i class="bi bi-person"></i>
        </button>
      </header>

      <main class="p-4">
        <h5 class="fw-bold text-primary mb-4">Quản lý hóa đơn</h5>

        <!-- BỘ LỌC -->
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

        <!-- BẢNG DANH SÁCH -->
        <div class="card rounded-4 shadow-sm" style="border: 1.5px solid #8cb8ff;">
          <div class="card-body p-0">
            <!-- Tiêu đề bảng -->
            <div class="p-4 pb-2 border-bottom">
              <h6 class="fw-bold m-0 d-flex align-items-center">
                <div class="bg-primary bg-opacity-10 text-primary rounded p-2 me-3 lh-1"><i class="bi bi-file-earmark-text"></i></div>
                Danh Sách Hóa Đơn
              </h6>
            </div>

            <!-- Tabs Trạng thái -->
            <ul class="nav nav-underline flex-nowrap overflow-auto px-3 pt-2 text-muted small fw-medium" style="border-bottom: 1px solid #dee2e6;">
              <li class="nav-item"><a class="nav-link active text-primary" href="#">Tất Cả</a></li>
              <li class="nav-item"><a class="nav-link text-muted" href="#">Chờ Xác Nhận</a></li>
              <li class="nav-item"><a class="nav-link text-muted" href="#">Đã Xác Nhận</a></li>
              <li class="nav-item"><a class="nav-link text-muted" href="#">Chờ Vận Chuyển</a></li>
              <li class="nav-item"><a class="nav-link text-muted" href="#">Vận Chuyển</a></li>
              <li class="nav-item"><a class="nav-link text-muted" href="#">Đã Hoàn Thành</a></li>
              <li class="nav-item"><a class="nav-link text-muted" href="#">Hủy</a></li>
            </ul>

            <!-- Bảng dữ liệu -->
            <div class="table-responsive">
              <table class="table table-hover align-middle m-0 text-center">
                <thead class="text-dark small" style="background-color: #f8f9fa;">
                <tr>
                  <th class="py-3 fw-medium border-0">STT</th>
                  <th class="py-3 fw-medium border-0">Mã Hóa Đơn</th>
                  <th class="py-3 fw-medium border-0">Tên Khách Hàng</th>
                  <th class="py-3 fw-medium border-0">Tên Nhân Viên</th>
                  <th class="py-3 fw-medium border-0">Tổng Tiền</th>
                  <th class="py-3 fw-medium border-0">Ngày Tạo</th>
                  <th class="py-3 fw-medium border-0">Loại Đơn</th>
                  <th class="py-3 fw-medium border-0">Hành Động</th>
                </tr>
                </thead>
                <tbody class="text-secondary small">
                <tr>
                  <td class="py-3">1</td>
                  <td>HD0037</td>
                  <td>No name</td>
                  <td>Admin 1</td>
                  <td>2432424</td>
                  <td>06/03/2025<br>08:00:00</td>
                  <td>Tại cửa hàng</td>
                  <td><button class="btn btn-sm btn-light border text-muted rounded-circle"><i class="bi bi-eye"></i></button></td>
                </tr>
                <tr>
                  <td class="py-3">2</td>
                  <td>HD328</td>
                  <td>No name</td>
                  <td>Admin 2</td>
                  <td>32423</td>
                  <td>08/03/2025<br>20:45:00</td>
                  <td>Online</td>
                  <td><button class="btn btn-sm btn-light border text-muted rounded-circle"><i class="bi bi-eye"></i></button></td>
                </tr>
                <tr>
                  <td class="py-3 border-bottom-0">3</td>
                  <td class="border-bottom-0">HD0028</td>
                  <td class="border-bottom-0">No name</td>
                  <td class="border-bottom-0">Admin 3</td>
                  <td class="border-bottom-0">222222</td>
                  <td class="border-bottom-0">20/03/2025<br>19:00:00</td>
                  <td class="border-bottom-0">Online</td>
                  <td class="border-bottom-0"><button class="btn btn-sm btn-light border text-muted rounded-circle"><i class="bi bi-eye"></i></button></td>
                </tr>
                </tbody>
              </table>
            </div>

            <!-- Phân trang -->
            <div class="p-3 d-flex justify-content-end border-top">
              <nav>
                <ul class="pagination pagination-sm m-0 gap-2">
                  <li class="page-item disabled"><a class="page-link rounded text-muted" href="#"><i class="bi bi-chevron-left"></i></a></li>
                  <li class="page-item active"><a class="page-link rounded bg-white text-dark border shadow-sm" href="#">1</a></li>
                  <li class="page-item"><a class="page-link rounded text-muted" href="#"><i class="bi bi-chevron-right"></i></a></li>
                </ul>
              </nav>
            </div>

          </div>
        </div>

      </main>
    </div>
  </div>
</template>

<style scoped>
/* Tuỳ chỉnh gạch chân cho Tabs */
.nav-underline .nav-link {
  padding-bottom: 0.75rem;
  border-bottom-width: 2px;
}
.nav-underline .nav-link.active {
  border-bottom-color: #0d6efd;
  font-weight: 600;
}
.nav-underline .nav-link:hover {
  color: #0d6efd !important;
}

/* Custom placeholder input */
.form-control::placeholder {
  color: #ced4da;
}
</style>