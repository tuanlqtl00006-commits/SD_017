<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const maHoaDon = computed(() => route.query.ma || 'HD0000')
</script>

<template>
  <div class="order-success-view py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-7 col-md-9 text-center">
          <div class="card border-0 shadow-sm rounded-4 p-5">
            <div class="mb-4">
              <div
                class="rounded-circle bg-success-subtle text-success d-inline-flex align-items-center justify-content-center"
                style="width: 80px; height: 80px;"
              >
                <i class="bi bi-check-lg display-4"></i>
              </div>
            </div>

            <h3 class="fw-bold text-dark mb-2">Đặt Hàng Thành Công!</h3>
            <p class="text-muted mb-4">
              Cảm ơn bạn đã tin tưởng và mua sắm tại <strong>FootStyle Sports</strong>.
            </p>

            <div class="alert alert-light border p-3 rounded-3 mb-4 text-start">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted small">Mã đơn hàng:</span>
                <strong class="fs-5 text-primary">{{ maHoaDon }}</strong>
              </div>
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted small">Trạng thái:</span>
                <span class="badge bg-warning text-dark">Chờ xác nhận</span>
              </div>
              <div class="d-flex justify-content-between align-items-center">
                <span class="text-muted small">Thời gian giao dự kiến:</span>
                <span class="fw-semibold">2 - 3 ngày làm việc</span>
              </div>
            </div>

            <p class="small text-muted mb-4">
              Nhân viên tư vấn của FootStyle sẽ liên hệ qua số điện thoại của bạn để xác nhận đơn hàng trước khi gửi cho đơn vị vận chuyển.
            </p>

            <div class="d-flex flex-wrap justify-content-center gap-3">
              <RouterLink
                :to="{ path: '/shop/tra-cuu', query: { ma: maHoaDon } }"
                class="btn btn-outline-primary rounded-pill px-4 fw-semibold"
              >
                <i class="bi bi-search me-1"></i> Tra cứu đơn hàng này
              </RouterLink>

              <RouterLink
                to="/shop/san-pham"
                class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm"
              >
                <i class="bi bi-arrow-left me-1"></i> Tiếp tục mua sắm
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
