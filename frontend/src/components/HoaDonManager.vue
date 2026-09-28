<template>

  <div class="d-flex vh-100" style="background-color: #f4f7f6;">


    <div class="d-flex flex-column flex-shrink-0 p-3 text-white" style="width: 260px; background-color: #0d1b2a;">
      <a href="/" class="d-flex align-items-center mb-3 mb-md-0 me-md-auto text-white text-decoration-none">
        <span class="fs-4 fw-bold fst-italic text-primary bg-white px-2 py-1 rounded">FootStyle</span>
      </a>
      <hr>
      <ul class="nav nav-pills flex-column mb-auto gap-2">
        <li class="nav-item">
          <a href="#" class="nav-link text-white">Thống Kê</a>
        </li>
        <li>

          <a href="#" class="nav-link active bg-primary text-white fw-bold">Quản Lý Hoá Đơn</a>
        </li>
        <li><a href="#" class="nav-link text-white">Quản Lý Nhân Viên</a></li>
        <li><a href="#" class="nav-link text-white">Quản Lý Khách Hàng</a></li>
        <li><a href="#" class="nav-link text-white">Quản Lý Phiếu Giảm Giá</a></li>
      </ul>
      <hr>
      <div class="d-flex align-items-center">
        <img src="https://github.com/mdo.png" alt="" width="32" height="32" class="rounded-circle me-2">
        <strong>Admin</strong>
      </div>
    </div>


    <div class="flex-grow-1 p-4 overflow-auto">


      <div class="d-flex justify-content-between align-items-center mb-4">
        <h3 class="fw-bold m-0">Quản Lý Hoá Đơn</h3>
        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-primary">+ Thêm hoá đơn</button>
          <div class="d-flex align-items-center">
            <span class="me-2 fw-bold">Hải Long</span>
            <img src="https://github.com/mdo.png" alt="" width="35" height="35" class="rounded-circle">
          </div>
        </div>
      </div>


      <div class="card border-0 shadow-sm rounded-3 mb-4">
        <div class="card-body">
          <h6 class="fw-bold mb-3 text-secondary">Bộ lọc</h6>
          <div class="d-flex gap-3">
            <input type="text" class="form-control w-50" placeholder="Tìm theo mã hoá đơn, tên khách hàng...">
            <select class="form-select w-25">
              <option>Tất cả trạng thái</option>
              <option>Đã thanh toán</option>
              <option>Chưa thanh toán</option>
            </select>
            <button class="btn btn-outline-secondary">Đặt lại bộ lọc</button>
            <button class="btn btn-outline-success">Xuất Excel</button>
            <button class="btn btn-danger">+ Thêm hoá đơn</button>
          </div>
        </div>
      </div>


      <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body">
          <h6 class="fw-bold mb-3 text-secondary">Danh sách hoá đơn</h6>
          <table class="table table-hover align-middle">
            <thead class="table-light text-muted">
            <tr>
              <th>STT</th>
              <th>Mã HĐ</th>
              <th>Tên khách hàng</th>
              <th>Ngày tạo</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th class="text-center">Hành động</th>
            </tr>
            </thead>
            <tbody>

            <tr v-for="i in 5" :key="i">
              <td>{{ i }}</td>
              <td class="fw-bold">HD00{{ i }}</td>
              <td>Nguyễn Văn A</td>
              <td>28/09/2026</td>
              <td class="fw-bold text-danger">2.500.000đ</td>
              <td>
                  <span class="badge bg-success-subtle text-success border border-success rounded-pill px-3">
                    Đã thanh toán
                  </span>
              </td>
              <td class="text-center">

                <button class="btn btn-sm btn-light border me-1 text-success">O</button>
                <button class="btn btn-sm btn-light border me-1 text-primary">✎</button>
                <button class="btn btn-sm btn-light border text-danger">🗑</button>
              </td>
            </tr>
            </tbody>
          </table>

          <div class="d-flex justify-content-between align-items-center mt-3">
            <span class="text-muted small">Đang hiển thị 1-5 trong tổng số 25 hoá đơn</span>
            <ul class="pagination pagination-sm m-0">
              <li class="page-item disabled"><a class="page-link" href="#">Trước</a></li>
              <li class="page-item active"><a class="page-link" href="#">1</a></li>
              <li class="page-item"><a class="page-link" href="#">2</a></li>
              <li class="page-item"><a class="page-link" href="#">3</a></li>
              <li class="page-item"><a class="page-link" href="#">Sau</a></li>
            </ul>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>

.nav-link {
  border-radius: 8px;
}
.card {
  background-color: #ffffff;
}
.table-light th {
  font-weight: 600;
  border-bottom: 2px solid #eaeaea;
}
</style>