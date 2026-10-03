<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">

    
    <div class="bg-white border-end d-flex flex-column" style="width: 280px; flex-shrink: 0;">
      
      <div class="p-3 text-center mb-1 d-flex align-items-center justify-content-center" style="height: 100px;">
        <img src="/logo.png" alt="FootStyle" class="img-fluid" style="width: 80%; object-fit: contain;">
      </div>


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

        
        <li class="nav-item">
          <a href="#menuThuocTinh" data-bs-toggle="collapse" class="nav-link sidebar-link d-flex justify-content-between align-items-center collapsed" aria-expanded="false">
            <div><i class="bi bi-hexagon icon"></i> Danh sách thuộc tính</div>
            <i class="bi bi-chevron-down chevron"></i>
          </a>
          <div class="collapse" id="menuThuocTinh"></div>
        </li>

        
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
        
        
        <li class="nav-item mb-4">
          <a href="#menuLichLam" data-bs-toggle="collapse" class="nav-link sidebar-link d-flex justify-content-between align-items-center collapsed" aria-expanded="false">
            <div><i class="bi bi-calendar3 icon"></i> Quản lý lịch làm</div>
            <i class="bi bi-chevron-down chevron"></i>
          </a>
          <div class="collapse" id="menuLichLam"></div>
        </li>
        
      </ul>
    </div>


    
    <div class="flex-grow-1 d-flex flex-column overflow-hidden">

      
      <header class="bg-white border-bottom px-4 py-3 d-flex justify-content-between align-items-center z-1">
        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-light border-0 d-lg-none"><i class="bi bi-list fs-4"></i></button>
          <div class="fw-medium text-muted fs-6">
            Hóa đơn <span class="mx-1">/</span> <span class="text-dark fw-bold">Chi tiết hóa đơn</span>
          </div>
        </div>

        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-outline-primary btn-sm rounded-pill px-3 fw-medium"><i class="bi bi-clock me-1"></i> Ca làm việc</button>
          <button class="btn btn-light rounded-circle p-2 text-muted shadow-sm"><i class="bi bi-moon"></i></button>
          <div class="position-relative">
            <button class="btn btn-light rounded-circle p-2 text-muted shadow-sm"><i class="bi bi-bell"></i></button>
            <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">4</span>
          </div>
          <div class="d-flex align-items-center gap-2 ms-2 cursor-pointer">
            <img src="https://ui-avatars.com/api/?name=Admin&background=0D6EFD&color=fff" class="rounded-circle shadow-sm" width="35" height="35" alt="Avatar">
            <span class="fw-medium text-dark d-none d-md-block">Admin</span>
          </div>
        </div>
      </header>

      
      <main class="p-4 overflow-auto">

        
        <div class="row g-4 mb-4">
          
          <div class="col-xl-8">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              <div class="card-body p-4 d-flex flex-column">
                <div class="d-flex justify-content-between align-items-center mb-4">
                  <h6 class="fw-bold m-0 d-flex align-items-center"><i class="bi bi-file-earmark-check text-primary me-2"></i> Trạng Thái Đơn Hàng</h6>
                  <span class="badge bg-success bg-opacity-10 text-success px-3 py-2 rounded-pill fw-semibold">Đã xác nhận</span>
                </div>

                
                <div class="timeline-container my-4 flex-grow-1">
                  <div class="timeline-line"></div>
                  <div class="d-flex justify-content-between position-relative z-1">

                    
                    <div class="timeline-step active">
                      <div class="timeline-icon bg-primary text-white"><i class="bi bi-hourglass-split"></i></div>
                      <div class="timeline-label mt-2 fw-bold text-dark">Chờ Xác Nhận</div>
                      <div class="timeline-time text-muted small">17:05:25<br>10/05/2026<br>ADMIN: TA</div>
                    </div>

                    
                    <div class="timeline-step active">
                      <div class="timeline-icon bg-primary text-white"><i class="bi bi-clipboard-check"></i></div>
                      <div class="timeline-label mt-2 fw-bold text-dark">Đã Xác Nhận</div>
                      <div class="timeline-time text-muted small">17:08:03<br>10/05/2026<br>ADMIN: TA</div>
                    </div>

                    
                    <div class="timeline-step">
                      <div class="timeline-icon bg-white text-muted border"><i class="bi bi-box-seam"></i></div>
                      <div class="timeline-label mt-2 text-muted fw-medium">Chờ Lấy Hàng</div>
                    </div>

                  </div>
                </div>

                
                <div class="d-flex justify-content-end gap-3 mt-3">
                  <button class="btn btn-outline-danger px-4 rounded-pill fw-medium"><i class="bi bi-x-circle me-1"></i> Hủy Đơn Hàng</button>
                  <button class="btn btn-primary px-4 rounded-pill fw-medium"><i class="bi bi-clock-history me-1"></i> Lịch Sử Thao Tác</button>
                </div>
              </div>
            </div>
          </div>

          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              <div class="card-body p-4">
                <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-wallet2 text-primary me-2"></i> Tổng Kết Thanh Toán</h6>

                <div class="d-flex justify-content-between mb-3 text-muted">
                  <span>Tổng Tiền Hàng</span>
                  <span class="text-dark fw-medium">3.032.000 ₫</span>
                </div>
                <div class="d-flex justify-content-between mb-3 text-muted">
                  <span>Phiếu Giảm Giá <span class="badge bg-primary bg-opacity-10 text-primary ms-1">VCH65FFTO</span></span>
                  <span class="text-success fw-medium">- 1.500.000 ₫</span>
                </div>
                <div class="d-flex justify-content-between mb-3 text-muted border-bottom pb-3">
                  <span>Phí Vận Chuyển <span class="badge bg-warning text-dark ms-1">GHN</span></span>
                  <span class="text-dark fw-medium">+ 31.000 ₫</span>
                </div>
                <div class="d-flex justify-content-between align-items-center mt-3">
                  <span class="fw-bold text-dark fs-5">Tổng Tiền</span>
                  <span class="fw-bold text-primary fs-4">1.563.000 ₫</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        
        <div class="row g-4 mb-4">
          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              <div class="card-body p-4">
                <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-person-lines-fill text-primary me-2"></i> Thông Tin Khách Hàng</h6>

                <div class="row mb-3">
                  <div class="col-4 text-muted small">Tên Khách Hàng</div>
                  <div class="col-8 text-end fw-medium text-dark">nguyễn van a</div>
                </div>
                <div class="row mb-3">
                  <div class="col-4 text-muted small">Số Điện Thoại</div>
                  <div class="col-8 text-end fw-medium text-dark">0383854485</div>
                </div>
                <div class="row">
                  <div class="col-4 text-muted small">Email</div>
                  <div class="col-8 text-end fw-medium text-dark text-break">sđf@gmail.com</div>
                </div>
              </div>
            </div>
          </div>

          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              <div class="card-body p-4">
                <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-geo-alt text-primary me-2"></i> Thông Tin Giao Hàng</h6>

                <div class="row mb-3">
                  <div class="col-3 text-muted small">Địa Chỉ</div>
                  <div class="col-9 text-end fw-medium text-dark">ưiuefhweiufhwfehe</div>
                </div>
                <div class="row mb-3">
                  <div class="col-4 text-muted small">Loại Đơn</div>
                  <div class="col-8 text-end fw-medium text-dark">Giao hàng</div>
                </div>
                <div class="row">
                  <div class="col-4 text-muted small">Ghi Chú</div>
                  <div class="col-8 text-end fw-medium text-muted fst-italic">Không có</div>
                </div>
              </div>
            </div>
          </div>

          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              <div class="card-body p-4 d-flex flex-column">
                <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-credit-card text-primary me-2"></i> Lịch Sử Thanh Toán</h6>

                <div class="d-flex justify-content-between align-items-center mb-4 flex-grow-1">
                  <div>
                    <div class="fw-bold text-dark">Chuyển khoản</div>
                    <div class="small text-muted">Thanh toán đơn hàng</div>
                    <div class="small text-muted" style="font-size: 0.75rem;">17:08:03 10/05/2026</div>
                  </div>
                  <div class="text-end">
                    <div class="badge bg-success bg-opacity-10 text-success mb-1">Đã thanh toán</div>
                    <div class="fw-bold text-dark">1.563.000 ₫</div>
                  </div>
                </div>

                <div class="d-flex flex-column gap-2 mt-auto">
                  <button class="btn btn-outline-primary rounded-3 fw-medium py-2"><i class="bi bi-printer me-1"></i> In Hóa Đơn</button>
                  <button class="btn btn-primary rounded-3 fw-medium py-2"><i class="bi bi-pencil-square me-1"></i> Chỉnh Sửa Đơn Hàng</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        
        <div class="card border-0 shadow-sm rounded-4 mb-4">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-box text-primary me-2"></i> Danh Sách Sản Phẩm (1)</h6>

            
            <div class="table-responsive">
              <table class="table align-middle">
                <thead class="table-light">
                <tr>
                  <th class="py-3 text-muted fw-semibold border-0 rounded-start">Sản phẩm</th>
                  <th class="py-3 text-muted fw-semibold border-0 text-center">Số lượng</th>
                  <th class="py-3 text-muted fw-semibold border-0 text-end">Đơn giá</th>
                  <th class="py-3 text-muted fw-semibold border-0 text-end rounded-end">Thành tiền</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                  <td class="py-3 border-0">
                    <div class="d-flex align-items-center gap-3">
                      <div class="bg-light rounded-3 d-flex align-items-center justify-content-center" style="width: 60px; height: 60px;">
                        <i class="bi bi-image text-muted fs-4"></i>
                      </div>
                      <div>
                        <div class="fw-bold text-dark">vợt cầu lông</div>
                        <div class="small text-muted">Màu: Trắng | Size: 42</div>
                      </div>
                    </div>
                  </td>
                  <td class="py-3 border-0 text-center fw-medium text-dark">1</td>
                  <td class="py-3 border-0 text-end fw-medium text-dark">3.032.000 ₫</td>
                  <td class="py-3 border-0 text-end fw-bold text-primary">3.032.000 ₫</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </main>
    </div>
  </div>
</template>

<style scoped>

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


.sidebar-menu .sidebar-link:hover:not(.active) {
  background-color: #f8f9fa;
  color: #0d6efd; 
}

.sidebar-menu .sidebar-link:hover:not(.active) .icon,
.sidebar-menu .sidebar-link:hover:not(.active) .chevron {
  color: #0d6efd;
}


.sidebar-menu .sidebar-link.active {
  background-color: #0d6efd !important; 
  color: white !important; 
  font-weight: 600;
  box-shadow: 0 0.125rem 0.25rem rgba(13, 110, 253, 0.2); 
}

.sidebar-menu .sidebar-link.active .icon {
  color: white !important;
}


.submenu .sidebar-link {
  padding: 0.6rem 1rem 0.6rem 2.8rem; 
  font-size: 0.9rem;
  color: #6c757d;
}

.submenu .sidebar-link .icon {
  font-size: 1rem;
  margin-right: 0.6rem;
  color: #adb5bd;
}


.sidebar-menu .sidebar-link .chevron {
  transition: transform 0.3s ease;
  font-size: 0.75rem;
  color: #6c757d;
}


.sidebar-menu .sidebar-link:not(.collapsed) .chevron {
transform: rotate(180deg);
  color: #0d6efd; 
}


.timeline-container {
  position: relative;
  padding: 0 20px;
}

.timeline-line {
  position: absolute;
  top: 25px; 
  left: 10%;
  right: 10%;
  height: 3px;
  background-color: #e9ecef;
  z-index: 0;
}
.timeline-step {
  position: relative;
  z-index: 1;
  text-align: center;
  width: 120px; 
}
.timeline-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  font-size: 1.4rem;
  transition: all 0.3s ease;
}

.timeline-step.active .timeline-icon {
  box-shadow: 0 0 0 5px rgba(13, 110, 253, 0.15);
}
</style>