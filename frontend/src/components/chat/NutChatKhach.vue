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
      title="Tư vấn trực tuyến FootStyle"
    >
      <div class="nck-icon-wrap">
        <i class="bi" :class="mo ? 'bi-x-lg' : 'bi-chat-dots-fill'" aria-hidden="true"></i>
        <span v-if="!mo" class="nck-cham" aria-hidden="true"></span>
      </div>
      <span class="nck-chu">{{ mo ? 'Đóng' : 'Tư vấn trực tuyến' }}</span>
    </button>
  </div>
</template>

<style scoped>
.nck {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1050;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}
.nck-nut {
  display: inline-flex;
  align-items: center;
  height: 48px;
  width: 48px;
  padding: 0;
  justify-content: center;
  border: 2px solid #fff;
  border-radius: 999px;
  background: var(--vnb-orange, #1d4ed8);
  color: #fff;
  font-weight: 700;
  font-size: 0.88rem;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.25);
  cursor: pointer;
  outline: none;
  overflow: hidden;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              padding 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              background-color 0.2s ease,
              transform 0.2s ease;
  white-space: nowrap;
}
.nck-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}
.nck-icon-wrap .bi {
  font-size: 1.25rem;
}
.nck-cham {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 2px #fff;
}
.nck-chu {
  max-width: 0;
  opacity: 0;
  overflow: hidden;
  transition: max-width 0.25s ease, opacity 0.2s ease, margin-right 0.25s ease;
  margin-right: 0;
}
/* Khi hover hoặc khi mở khung chat: mở rộng thành pill có chữ */
.nck-nut:hover,
.nck-nut.is-mo {
  width: auto;
  padding-right: 18px;
  background: var(--vnb-orange-hover, #1e40af);
  transform: translateY(-2px);
}
.nck-nut:hover .nck-chu,
.nck-nut.is-mo .nck-chu {
  max-width: 160px;
  opacity: 1;
  margin-left: 2px;
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
}
</style>
