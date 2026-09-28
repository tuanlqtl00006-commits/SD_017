<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f6f9;">
    <div class="d-flex flex-column flex-shrink-0 text-white" style="width: 260px; background-color: #1a202c;">
      <div class="bg-white rounded p-2 mx-3">
        <img src="/logo.png" alt="FootStyle" class="img-fluid w-100" style="object-fit: contain;">
      </div>
      <ul class="nav nav-pills flex-column mb-auto p-3 gap-2">
        <li class="nav-item"><a href="#" class="nav-link text-white text-opacity-75"><i class="bi bi-bar-chart me-2"></i> Thống Kê</a></li>
        <li><a href="#" class="nav-link text-white text-opacity-75"><i class="bi bi-box me-2"></i> Quản Lý Sản Phẩm</a></li>
        <li><a href="#" class="nav-link text-white text-opacity-75"><i class="bi bi-person me-2"></i> Quản Lý Nhân Viên</a></li>
        <li><a href="#" class="nav-link text-white text-opacity-75"><i class="bi bi-people me-2"></i> Quản Lý Khách Hàng</a></li>
        <li><a href="#" class="nav-link active fw-bold shadow-sm" style="background-color: #0d6efd;"><i class="bi bi-receipt me-2"></i> Quản Lý Hóa Đơn</a></li>
      </ul>
      <div class="p-3 border-top border-secondary border-opacity-25 d-flex align-items-center">
        <img src="https://github.com/mdo.png" width="40" height="40" class="rounded-circle me-3">
        <div>
          <div class="fw-bold fs-6">Admin</div>
          <div class="text-muted small">Quản trị viên</div>
        </div>
      </div>
    </div>

    <div class="flex-grow-1 d-flex flex-column">

      <header class="d-flex justify-content-between align-items-center p-3 bg-white border-bottom">
        <h5 class="m-0 fw-bold d-flex align-items-center">
          <i class="bi bi-list fs-4 me-2 text-muted"></i> Chi Tiết Hóa Đơn
        </h5>
        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-light border btn-sm text-muted rounded-pill px-3"><i class="bi bi-clock"></i> Ca làm việc <i class="bi bi-chevron-down ms-1"></i></button>
          <button class="btn btn-light border btn-sm text-muted rounded-circle"><i class="bi bi-gear"></i></button>
          <button class="btn btn-light border btn-sm text-muted rounded-circle"><i class="bi bi-bell"></i></button>
          <div class="d-flex align-items-center border-start ps-3">
            <span class="me-2 text-muted">Admin</span>
            <img src="https://github.com/mdo.png" width="32" class="rounded-circle">
          </div>
        </div>
      </header>

      <main class="p-4 overflow-auto">

        <div class="d-flex justify-content-between align-items-end mb-4">
          <div>
            <nav aria-label="breadcrumb">
              <ol class="breadcrumb mb-1 small">
                <li class="breadcrumb-item"><a href="#" class="text-decoration-none text-primary">Quản lý hóa đơn</a></li>
                <li class="breadcrumb-item active text-muted" aria-current="page">HD20260118027</li>
              </ol>
            </nav>
            <div class="d-flex align-items-center gap-3 mt-2">
              <h4 class="fw-bold m-0">Hóa đơn #HD20260118027</h4>
              <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-2">ĐANG GIAO HÀNG</span>
              <span class="text-muted small">Tạo lúc 09:42 - 18/01/2026</span>
            </div>
          </div>
          <div class="d-flex gap-2">
            <button class="btn btn-white border shadow-sm text-muted"><i class="bi bi-printer me-1"></i> In hóa đơn</button>
            <button class="btn btn-white border shadow-sm text-muted"><i class="bi bi-download me-1"></i> Xuất PDF</button>
            <button class="btn btn-primary shadow-sm"><i class="bi bi-arrow-repeat me-1"></i> Cập nhật trạng thái</button>
          </div>
        </div>

        <div class="card border-0 shadow-sm rounded-4 mb-4">
          <div class="card-body p-4">
            <div class="d-flex justify-content-between mb-4">
              <h6 class="fw-bold m-0">Trạng thái hóa đơn</h6>
              <small class="text-muted">Cập nhật gần nhất: 14:20 - 19/01/2026</small>
            </div>

            <div class="position-relative m-4">
              <div class="progress position-absolute top-50 start-0 w-100 translate-middle-y" style="height: 3px; z-index: 1;">
                <div class="progress-bar bg-success" style="width: 66%"></div>
              </div>

              <div class="d-flex justify-content-between position-relative" style="z-index: 2;">
                <div class="text-center bg-white px-2">
                  <div class="rounded-circle bg-success text-white d-flex align-items-center justify-content-center mx-auto mb-2 shadow-sm" style="width:40px; height:40px;"><i class="bi bi-check-lg fs-5"></i></div>
                  <div class="fw-bold small text-dark">Đã xác nhận</div>
                  <div class="text-muted" style="font-size: 0.75rem;">18/01 - 09:45</div>
                </div>
                <div class="text-center bg-white px-2">
                  <div class="rounded-circle bg-success text-white d-flex align-items-center justify-content-center mx-auto mb-2 shadow-sm" style="width:40px; height:40px;"><i class="bi bi-box-seam fs-5"></i></div>
                  <div class="fw-bold small text-dark">Đã đóng gói</div>
                  <div class="text-muted" style="font-size: 0.75rem;">18/01 - 16:30</div>
                </div>
                <div class="text-center bg-white px-2">
                  <div class="rounded-circle bg-white border border-2 border-primary text-primary d-flex align-items-center justify-content-center mx-auto mb-2 shadow-sm" style="width:40px; height:40px;"><i class="bi bi-truck fs-5"></i></div>
                  <div class="fw-bold small text-primary">Đang giao hàng</div>
                  <div class="text-muted" style="font-size: 0.75rem;">19/01 - 14:20</div>
                </div>
                <div class="text-center bg-white px-2">
                  <div class="rounded-circle bg-light border text-muted d-flex align-items-center justify-content-center mx-auto mb-2" style="width:40px; height:40px;"><i class="bi bi-check2-circle fs-5"></i></div>
                  <div class="fw-bold small text-muted">Hoàn thành</div>
                  <div class="text-muted" style="font-size: 0.75rem;">Dự kiến 20/01</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="row g-4">
          <div class="col-xl-8 col-lg-7 d-flex flex-column gap-4">

            <div class="card border-0 shadow-sm rounded-4">
              <div class="card-body p-0">
                <div class="p-4 border-bottom d-flex justify-content-between align-items-center">
                  <h6 class="fw-bold m-0"><i class="bi bi-bag text-primary me-2"></i> Sản phẩm trong hóa đơn</h6>
                  <span class="text-muted small">3 sản phẩm</span>
                </div>
                <div class="table-responsive">
                  <table class="table table-borderless align-middle m-0">
                    <thead class="bg-light text-muted small border-bottom">
                    <tr>
                      <th class="ps-4 py-3 fw-normal">SẢN PHẨM</th>
                      <th class="py-3 fw-normal text-end">ĐƠN GIÁ</th>
                      <th class="py-3 fw-normal text-center">SL</th>
                      <th class="pe-4 py-3 fw-normal text-end">THÀNH TIỀN</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr class="border-bottom">
                      <td class="ps-4 py-3">
                        <div class="d-flex align-items-center gap-3">
                          <div class="bg-light rounded p-2" style="width: 50px; height: 50px;"><img src="https://via.placeholder.com/50" class="img-fluid mix-blend-multiply"></div>
                          <div>
                            <div class="fw-bold text-dark">Vợt cầu lông Yonex Astrox 100 ZZ</div>
                            <div class="text-muted small">Trắng / Navy • YX-AST-100ZZ</div>
                          </div>
                        </div>
                      </td>
                      <td class="py-3 text-end fw-medium text-dark">6.490.000đ</td>
                      <td class="py-3 text-center">1</td>
                      <td class="pe-4 py-3 text-end fw-bold text-dark">6.490.000đ</td>
                    </tr>
                    <tr class="border-bottom">
                      <td class="ps-4 py-3">
                        <div class="d-flex align-items-center gap-3">
                          <div class="bg-light rounded p-2" style="width: 50px; height: 50px;"><img src="https://via.placeholder.com/50" class="img-fluid mix-blend-multiply"></div>
                          <div>
                            <div class="fw-bold text-dark">Vợt cầu lông Li-Ning Axforce 90 Dragon</div>
                            <div class="text-muted small">Đen đỏ / 4U • LN-AXF-90D</div>
                          </div>
                        </div>
                      </td>
                      <td class="py-3 text-end fw-medium text-dark">4.290.000đ</td>
                      <td class="py-3 text-center">1</td>
                      <td class="pe-4 py-3 text-end fw-bold text-dark">4.290.000đ</td>
                    </tr>
                    <tr>
                      <td class="ps-4 py-3">
                        <div class="d-flex align-items-center gap-3">
                          <div class="bg-light rounded p-2" style="width: 50px; height: 50px;"><img src="https://via.placeholder.com/50" class="img-fluid mix-blend-multiply"></div>
                          <div>
                            <div class="fw-bold text-dark">Vợt cầu lông Victor Thruster F Falcon</div>
                            <div class="text-muted small">Đen cam / 4U • VC-TFF-4U</div>
                          </div>
                        </div>
                      </td>
                      <td class="py-3 text-end fw-medium text-dark">3.980.000đ</td>
                      <td class="py-3 text-center">1</td>
                      <td class="pe-4 py-3 text-end fw-bold text-dark">3.980.000đ</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4">
              <div class="card-body p-4">
                <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3">
                  <h6 class="fw-bold m-0"><i class="bi bi-clock-history text-primary me-2"></i> Lịch sử thanh toán</h6>
                  <span class="text-muted small">Đã thanh toán</span>
                </div>
                <div class="d-flex justify-content-between align-items-center">
                  <div class="d-flex align-items-center gap-3">
                    <div class="fw-bold text-dark">18/01/2026 - 09:43<br><span class="text-muted fw-normal small">MOMO-98241570</span></div>
                    <div class="d-flex align-items-center bg-light rounded px-3 py-2 border">
                      <span class="text-danger fw-bold me-2">M</span> <span class="fw-medium">Ví MoMo</span>
                    </div>
                  </div>
                  <div class="d-flex align-items-center gap-3">
                    <span class="fw-bold text-dark fs-6">7.570.000đ</span>
                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-2">THÀNH CÔNG</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4">
              <div class="card-body p-4">
                <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-4">
                  <h6 class="fw-bold m-0"><i class="bi bi-list-ul text-primary me-2"></i> Nhật ký xử lý</h6>
                  <span class="text-muted small">Theo thời gian thực</span>
                </div>

                <div class="position-relative ms-3 border-start border-2 border-primary border-opacity-25 pb-3">
                  <div class="position-relative mb-4 ms-4">
                    <div class="position-absolute bg-primary text-white rounded-circle d-flex align-items-center justify-content-center shadow" style="width: 30px; height: 30px; left: -40px; top: 0;"><i class="bi bi-truck small"></i></div>
                    <div class="d-flex justify-content-between">
                      <div>
                        <div class="fw-bold text-dark">Bàn giao cho đơn vị vận chuyển</div>
                        <div class="text-muted small">Nhân viên Nguyễn Quốc Huy - Kho Cầu Giấy</div>
                      </div>
                      <div class="text-muted small">19/01 - 14:20</div>
                    </div>
                  </div>
                  <div class="position-relative mb-4 ms-4">
                    <div class="position-absolute bg-success-subtle text-success rounded-circle d-flex align-items-center justify-content-center" style="width: 30px; height: 30px; left: -40px; top: 0;"><i class="bi bi-check-lg small"></i></div>
                    <div class="d-flex justify-content-between">
                      <div>
                        <div class="fw-bold text-dark">Đã đóng gói và kiểm tra sản phẩm</div>
                        <div class="text-muted small">Kiện hàng 3 sản phẩm - 2,4 kg</div>
                      </div>
                      <div class="text-muted small">18/01 - 16:30</div>
                    </div>
                  </div>
                  <div class="position-relative ms-4">
                    <div class="position-absolute bg-success-subtle text-success rounded-circle d-flex align-items-center justify-content-center" style="width: 30px; height: 30px; left: -40px; top: 0;"><i class="bi bi-check-lg small"></i></div>
                    <div class="d-flex justify-content-between">
                      <div>
                        <div class="fw-bold text-dark">Xác nhận đơn hàng</div>
                        <div class="text-muted small">Tự động xác nhận sau khi thanh toán</div>
                      </div>
                      <div class="text-muted small">18/01 - 09:45</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <div class="col-xl-4 col-lg-5 d-flex flex-column gap-4">

            <div class="card border-0 shadow-sm rounded-4">
              <div class="card-body p-4">
                <h6 class="fw-bold mb-4"><i class="bi bi-person text-primary me-2"></i> Khách hàng & giao hàng</h6>

                <div class="d-flex align-items-center mb-4">
                  <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 fs-5" style="width: 50px; height: 50px;">MA</div>
                  <div>
                    <div class="fw-bold text-dark d-flex align-items-center">
                      Nguyễn Minh Anh <span class="badge bg-warning-subtle text-warning border border-warning-subtle ms-2 rounded-pill" style="font-size: 0.65rem;">GOLD</span>
                    </div>
                    <div class="text-muted small">0983 354 455 • minhanh@gmail.com</div>
                  </div>
                </div>

                <div class="d-flex mb-3">
                  <div class="text-muted me-2" style="width: 20px;"><i class="bi bi-geo-alt"></i></div>
                  <div>
                    <div class="text-muted small mb-1">Địa chỉ nhận</div>
                    <div class="text-dark small fw-medium">24 Trần Thái Tông, Cầu Giấy,<br>Hà Nội</div>
                  </div>
                </div>

                <div class="d-flex mb-3">
                  <div class="text-muted me-2" style="width: 20px;"><i class="bi bi-truck"></i></div>
                  <div class="w-100 d-flex justify-content-between">
                    <span class="text-muted small">Đơn vị vận chuyển</span>
                    <span class="text-dark small fw-medium">Giao Hàng Tiết Kiệm</span>
                  </div>
                </div>

                <div class="d-flex mb-3">
                  <div class="text-muted me-2" style="width: 20px;"><i class="bi bi-upc-scan"></i></div>
                  <div class="w-100 d-flex justify-content-between">
                    <span class="text-muted small">Mã vận đơn</span>
                    <span class="text-dark small fw-medium">GHTK.HN.186450392</span>
                  </div>
                </div>

                <div class="d-flex">
                  <div class="text-muted me-2" style="width: 20px;"><i class="bi bi-chat-left-text"></i></div>
                  <div class="w-100 d-flex justify-content-between">
                    <span class="text-muted small">Ghi chú</span>
                    <span class="text-dark small fw-medium">Gọi trước khi giao hàng</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4">
              <div class="card-body p-4">
                <h6 class="fw-bold mb-4"><i class="bi bi-receipt text-primary me-2"></i> Tổng thanh toán</h6>

                <div class="d-flex justify-content-between mb-2">
                  <span class="text-muted small">Tạm tính</span>
                  <span class="text-dark fw-medium small">14.760.000đ</span>
                </div>
                <div class="d-flex justify-content-between mb-2">
                  <span class="text-muted small">Giảm giá • <span class="text-dark">RACKET500</span></span>
                  <span class="text-success fw-medium small">-500.000đ</span>
                </div>
                <div class="d-flex justify-content-between mb-4">
                  <span class="text-muted small">Phí vận chuyển</span>
                  <span class="text-dark fw-medium small">30.000đ</span>
                </div>

                <hr class="text-muted border-opacity-50">

                <div class="d-flex justify-content-between align-items-center mb-4">
                  <span class="fw-bold text-dark">Khách đã thanh toán</span>
                  <span class="fs-4 fw-bold text-primary">14.290.000đ</span>
                </div>

                <div class="alert alert-success bg-success-subtle border-success-subtle text-success py-2 text-center small fw-bold mb-4 rounded-3">
                  <i class="bi bi-check-circle me-1"></i> ĐÃ THANH TOÁN QUA MOMO
                </div>

                <div class="d-flex gap-2">
                  <button class="btn btn-danger bg-danger-subtle text-danger border-danger-subtle w-100 fw-medium"><i class="bi bi-x-circle me-1"></i> Hủy hóa đơn</button>
                  <button class="btn btn-light border text-muted px-3"><i class="bi bi-telephone"></i></button>
                </div>
              </div>
            </div>

          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.btn-white {
  background-color: #fff;
}
.mix-blend-multiply {
  mix-blend-mode: multiply;
}
</style>