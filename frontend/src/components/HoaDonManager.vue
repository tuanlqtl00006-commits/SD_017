<template>
  <!-- Chỉ còn phần nội dung trang; thanh bên + header nằm trong layouts/MainLayout.vue -->
  <div>
    
    <div class="hd-title bg-white border rounded-3 shadow-sm py-3 px-4 mb-4">
      <h5 class="fw-bold text-primary m-0">Quản lý hóa đơn</h5>
    </div>

    
    <div class="hd-filter card border-0 shadow-sm rounded-4 mb-4">
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

    
    <div class="hd-table card rounded-3 shadow-sm bg-white" style="border: 2px solid #5b8deb;">
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
                  <RouterLink to="/hoa-don/1" class="text-secondary" aria-label="Xem chi tiết"><i class="bi bi-eye fs-5"></i></RouterLink>
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
                  <RouterLink to="/hoa-don/2" class="text-secondary" aria-label="Xem chi tiết"><i class="bi bi-eye fs-5"></i></RouterLink>
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
                  <RouterLink to="/hoa-don/3" class="text-secondary" aria-label="Xem chi tiết"><i class="bi bi-eye fs-5"></i></RouterLink>
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

  </div>
</template>

<style scoped>
/* Bảng hóa đơn lớn hơn: chữ to, hàng cao, tiêu đề đậm */
.hd-table table { font-size: 1.05rem; }
.hd-table thead th { font-size: 1.05rem; }
.hd-table tbody td { padding-block: 1.6rem !important; }
/* Màn hình thấp/zoom 100%: thu gọn thẻ tiêu đề + bộ lọc để nhường chỗ cho bảng */
@media (max-height: 820px), (max-width: 1600px) {
  .hd-title { padding-block: 0.6rem !important; margin-bottom: 0.75rem !important; }
  .hd-filter { margin-bottom: 0.75rem !important; }
  .hd-filter .card-body { padding: 1rem 1.25rem !important; }
  .hd-filter .row { --bs-gutter-y: 0.5rem; }
  .hd-filter .mt-4 { margin-top: 0.75rem !important; }
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
  color: var(--fs-primary) !important;
  border-bottom: 2px solid var(--fs-primary);
  font-weight: 600;
}
.custom-tabs .nav-link:hover {
  color: var(--fs-primary);
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
