<script setup>
// Trang "ngân hàng giả" (demo đồ án): mở bằng cách quét mã QR trên trang đặt hàng.
// Bấm "Xác nhận chuyển tiền" -> backend ghi nhận đơn đã thanh toán, KHÔNG có tiền thật bị trừ.
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { formatMoney } from '../utils/format'

const route = useRoute()
const ma = computed(() => String(route.query.ma || '').trim())
const thongTin = ref(null)
const loi = ref('')
const dangGui = ref(false)
const xong = ref(false)

async function tai() {
  if (!ma.value) {
    loi.value = 'Mã QR không hợp lệ (thiếu mã đơn hàng).'
    return
  }
  try {
    thongTin.value = (await api.get(`/cua-hang/thanh-toan-gia-lap/${encodeURIComponent(ma.value)}`)).data
    xong.value = thongTin.value.daThanhToanDu
  } catch (e) {
    loi.value = e?.message || 'Không tải được thông tin thanh toán.'
  }
}

async function xacNhan() {
  dangGui.value = true
  loi.value = ''
  try {
    thongTin.value = (await api.post(`/cua-hang/thanh-toan-gia-lap/${encodeURIComponent(ma.value)}/xac-nhan`)).data
    xong.value = true
  } catch (e) {
    loi.value = e?.message || 'Không xác nhận được. Vui lòng thử lại.'
  } finally {
    dangGui.value = false
  }
}

onMounted(tai)
</script>

<template>
  <div class="gl-page">
    <div class="gl-phone">
      <div class="gl-bank">
        <i class="bi bi-bank2"></i>
        <div>
          <div class="gl-bank-name">Ngân hàng Demo</div>
          <div class="gl-bank-sub">Xác nhận chuyển tiền (giả lập)</div>
        </div>
      </div>

      <div v-if="loi && !thongTin" class="gl-card text-center">
        <i class="bi bi-exclamation-circle-fill gl-icon-err"></i>
        <p class="mb-0">{{ loi }}</p>
      </div>

      <div v-else-if="!thongTin" class="gl-card text-center text-muted">
        <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Đang tải...
      </div>

      <div v-else-if="xong" class="gl-card text-center">
        <div class="gl-ok"><i class="bi bi-check-lg"></i></div>
        <h5 class="fw-bold mt-3">Chuyển tiền thành công</h5>
        <div class="gl-amount">{{ formatMoney(thongTin.thanhTien) }}</div>
        <p class="text-muted small mb-1">Nội dung: <strong>{{ thongTin.maHoaDon }}</strong></p>
        <p class="text-muted small mb-0">Bạn có thể đóng trang này. Màn hình đặt hàng sẽ tự cập nhật.</p>
      </div>

      <div v-else-if="thongTin.daHuy" class="gl-card text-center">
        <i class="bi bi-x-circle-fill gl-icon-err"></i>
        <p class="mb-0">Đơn hàng <strong>{{ thongTin.maHoaDon }}</strong> đã bị hủy nên không thể thanh toán.</p>
      </div>

      <div v-else class="gl-card">
        <div class="text-center text-muted small">Bạn sắp chuyển</div>
        <div class="gl-amount text-center">{{ formatMoney(thongTin.conLai) }}</div>

        <div class="gl-row"><span>Người nhận</span><strong>FootStyle Sports</strong></div>
        <div class="gl-row"><span>Nội dung</span><strong class="text-primary">{{ thongTin.maHoaDon }}</strong></div>
        <div class="gl-row"><span>Phí chuyển</span><strong>Miễn phí</strong></div>

        <div v-if="loi" class="gl-error"><i class="bi bi-exclamation-circle-fill me-1"></i>{{ loi }}</div>

        <button type="button" class="gl-btn" :disabled="dangGui" @click="xacNhan">
          <span v-if="dangGui" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          Xác nhận chuyển tiền
        </button>
        <p class="gl-note">Đây là giao diện mô phỏng cho đồ án, không có tiền thật bị trừ.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gl-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  background: linear-gradient(160deg, #e8f1ff 0%, #f6f8fc 60%);
  padding: 1.25rem 1rem;
}
.gl-phone {
  width: 100%;
  max-width: 420px;
}
.gl-bank {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.9rem 1.1rem;
  margin-bottom: 1rem;
  color: #fff;
  background: linear-gradient(135deg, #0d6efd, #0a4fc0);
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(13, 110, 253, 0.3);
}
.gl-bank i {
  font-size: 1.8rem;
}
.gl-bank-name {
  font-weight: 700;
}
.gl-bank-sub {
  font-size: 0.8rem;
  opacity: 0.85;
}
.gl-card {
  background: #fff;
  border-radius: 18px;
  padding: 1.5rem 1.25rem;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}
.gl-amount {
  font-size: 2rem;
  font-weight: 800;
  color: #dc2626;
  margin: 0.2rem 0 1rem;
}
.gl-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.6rem 0;
  font-size: 0.92rem;
  border-top: 1px dashed #e9ecf1;
}
.gl-row span {
  color: #8a94a6;
}
.gl-btn {
  width: 100%;
  margin-top: 1.1rem;
  padding: 0.85rem;
  border: 0;
  border-radius: 999px;
  font-weight: 700;
  color: #fff;
  background: #16a34a;
  box-shadow: 0 8px 18px rgba(22, 163, 74, 0.3);
}
.gl-btn:disabled {
  opacity: 0.7;
}
.gl-note {
  margin: 0.8rem 0 0;
  text-align: center;
  font-size: 0.75rem;
  color: #9aa3b2;
}
.gl-error {
  margin-top: 0.8rem;
  font-size: 0.85rem;
  color: #b91c1c;
}
.gl-icon-err {
  display: block;
  font-size: 2.2rem;
  color: #dc2626;
  margin-bottom: 0.5rem;
}
.gl-ok {
  width: 76px;
  height: 76px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.6rem;
  color: #16a34a;
  background: #dcfce7;
  box-shadow: 0 0 0 10px #f0fdf4;
}
</style>
