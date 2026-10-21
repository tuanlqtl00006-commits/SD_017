<script setup>
// Trang chat hỗ trợ CÔNG KHAI cho khách (/ho-tro): không cần đăng nhập. Trang bán hàng online có thể nhúng KhungChatKhach.vue.
import KhungChatKhach from './KhungChatKhach.vue'
</script>

<template>
  <div class="htk">
    <div class="htk-hop">
      <div class="htk-logo">
        <i class="bi bi-shop" aria-hidden="true"></i>
        <span><strong>FootStyle</strong> · Cầu lông chính hãng</span>
      </div>
      <div class="htk-khung">
        <KhungChatKhach ten-cua-hang="FootStyle" />
      </div>
      <p class="htk-chu-thich">Trợ lý ảo trả lời tự động dựa trên dữ liệu sản phẩm, khuyến mãi và đơn hàng của cửa hàng.</p>
    </div>
  </div>
</template>

<style scoped>
/* Trang của khách hiển thị cỡ chữ bình thường (100%): bù lại độ thu nhỏ --ui-zoom của trang quản trị */
.htk {
  zoom: calc(1 / var(--ui-zoom, 1));
  min-height: 100vh;
  background: linear-gradient(160deg, #eaf3fe 0%, #f7f9fc 55%);
  display: flex;
  justify-content: center;
  padding: 24px 16px;
}
.htk-hop {
  width: 100%;
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.htk-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--fs-navy);
  font-size: 1.05rem;
}
.htk-logo .bi {
  color: var(--fs-primary);
  font-size: 1.4rem;
}
.htk-khung {
  height: min(720px, calc(100vh - 140px));
  min-height: 460px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--fs-line);
  box-shadow: 0 10px 30px rgba(24, 36, 47, 0.08);
  display: flex;
  flex-direction: column;
}
.htk-chu-thich {
  text-align: center;
  color: var(--fs-muted);
  font-size: 0.8rem;
  margin: 0;
}
@media (max-width: 575.98px) {
  .htk {
    padding: 0;
  }
  .htk-logo,
  .htk-chu-thich {
    display: none;
  }
  .htk-khung {
    height: 100vh;
    border-radius: 0;
    border: 0;
  }
}
</style>
