<script setup>
// Hộp hiện mã QR của một giá trị (vd mã biến thể SP01-DEN-4U-G5) kèm nút tải ảnh QR (như video mẫu 1).
// QR tạo ngay trên trình duyệt bằng thư viện qrcode, không cần mạng.
import { ref, watch } from 'vue'
import QRCode from 'qrcode'
import BaseModal from './BaseModal.vue'

const props = defineProps({
  giaTri: { type: String, required: true }, // nội dung mã hóa trong QR
  tieuDe: { type: String, default: 'Mã QR' },
  moTa: { type: String, default: '' }, // dòng chữ phụ dưới QR (vd tên sản phẩm · màu · trọng lượng)
  anh: { type: String, default: '' }, // ảnh biến thể (nếu có)
})
const emit = defineEmits(['close'])

const qrUrl = ref('')
const loi = ref('')
watch(
  () => props.giaTri,
  async (v) => {
    loi.value = ''
    try {
      qrUrl.value = await QRCode.toDataURL(String(v), { width: 320, margin: 1, errorCorrectionLevel: 'M' })
    } catch {
      qrUrl.value = ''
      loi.value = 'Không tạo được mã QR.'
    }
  },
  { immediate: true },
)
const tenFile = () => `QR_${String(props.giaTri).replace(/[^A-Za-z0-9_-]+/g, '_')}.png`
</script>

<template>
  <BaseModal :title="tieuDe" size="sm" @close="emit('close')">
    <div class="qr-box">
      <img v-if="qrUrl" :src="qrUrl" :alt="`Mã QR ${giaTri}`" width="220" height="220" data-testid="qr-img" />
      <span v-else class="text-muted small">{{ loi || 'Đang tạo mã QR…' }}</span>
      <div class="qr-ma ad-code">{{ giaTri }}</div>
      <div v-if="moTa" class="qr-mo-ta">
        <img v-if="anh" :src="anh" alt="" class="qr-thumb" />
        <span>{{ moTa }}</span>
      </div>
    </div>
    <template #footer>
      <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
      <a v-if="qrUrl" class="ad-btn ad-btn-primary text-decoration-none" :href="qrUrl" :download="tenFile()">
        <i class="bi bi-download" aria-hidden="true"></i> Tải QR
      </a>
    </template>
  </BaseModal>
</template>

<style scoped>
.qr-box { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
.qr-box > img { border: 1px solid var(--fs-line, #e3eaf3); border-radius: 10px; padding: 6px; background: #fff; }
.qr-ma { font-size: 0.95rem; }
.qr-mo-ta { display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; color: var(--fs-muted, #7b8794); text-align: center; }
.qr-thumb { width: 32px; height: 32px; object-fit: cover; border-radius: 6px; border: 1px solid var(--fs-line, #e3eaf3); }
</style>
