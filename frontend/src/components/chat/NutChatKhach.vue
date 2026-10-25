<script setup>
// Nút chat nổi ở góc trang bán hàng online: bấm để mở khung chat với trợ lý ảo (trả lời từ dữ liệu cửa hàng),
// trong khung có nút "Gặp nhân viên". Đóng khung không kết thúc cuộc chat (KhungChatKhach lưu phiên trong trình duyệt).
import { ref } from 'vue'
import KhungChatKhach from './KhungChatKhach.vue'

defineProps({
  tenCuaHang: { type: String, default: 'FootStyle' },
})

const mo = ref(false)
</script>

<template>
  <div class="nck">
    <div v-if="mo" id="nck-khung" class="nck-khung" role="dialog" :aria-label="`Chat hỗ trợ ${tenCuaHang}`">
      <KhungChatKhach :ten-cua-hang="tenCuaHang" />
    </div>
    <button
      type="button"
      class="nck-nut"
      :class="{ 'is-mo': mo }"
      :aria-expanded="mo"
      aria-controls="nck-khung"
      @click="mo = !mo"
    >
      <i class="bi" :class="mo ? 'bi-x-lg' : 'bi-chat-dots-fill'" aria-hidden="true"></i>
      <span class="nck-chu">{{ mo ? 'Đóng' : 'Tư vấn trực tuyến' }}</span>
      <span v-if="!mo" class="nck-cham" aria-hidden="true"></span>
    </button>
  </div>
</template>

<style scoped>
.nck {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1050;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}
.nck-nut {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 2px solid #fff;
  border-radius: 999px;
  padding: 10px 20px;
  background: var(--vnb-orange, #1d4ed8);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.25);
  transition: transform 0.2s ease, background-color 0.2s ease;
}
.nck-nut:hover,
.nck-nut.is-mo {
  background: var(--vnb-orange-hover, #1e40af);
  transform: translateY(-2px);
}
.nck-nut .bi {
  font-size: 1.15rem;
}
.nck-cham {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 2px #fff;
}
.nck-khung {
  width: 380px;
  height: min(560px, calc(100vh - 120px));
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--vnb-border, #e2e8f0);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.25);
}
@media (max-width: 575.98px) {
  .nck {
    right: 12px;
    bottom: 12px;
  }
  .nck-khung {
    width: calc(100vw - 24px);
  }
  .nck-chu {
    display: none;
  }
}
</style>
