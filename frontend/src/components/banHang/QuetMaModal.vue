<script setup>
// Quét mã QR sản phẩm bằng camera (máy tính / điện thoại). Mã QR in ở trang Biến thể sản phẩm chứa mã biến thể (vd SP01-DEN-4U-G5).
// Đọc được mã thì báo ra ngoài (emit 'ma') và đóng camera; máy quét mã vạch cầm tay thì dùng ô "Quét mã" trên trang (không cần hộp này).
import { onBeforeUnmount, onMounted, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'

const emit = defineEmits(['close', 'ma'])
const video = ref(null)
const canvas = ref(null)
const loi = ref('')
const dangMo = ref(true)
let luong = null
let hen = null
let jsQR = null

async function batCamera() {
  if (!navigator.mediaDevices?.getUserMedia) {
    loi.value = 'Trình duyệt không hỗ trợ camera (cần mở trang bằng http://localhost hoặc https).'
    dangMo.value = false
    return
  }
  try {
    jsQR = (await import('jsqr')).default
    luong = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false })
    video.value.srcObject = luong
    await video.value.play()
    dangMo.value = false
    hen = setInterval(doc, 250)
  } catch (e) {
    dangMo.value = false
    loi.value = e?.name === 'NotAllowedError'
      ? 'Bạn chưa cho phép dùng camera. Hãy bấm biểu tượng camera trên thanh địa chỉ để cho phép rồi mở lại.'
      : 'Không mở được camera: ' + (e?.message || 'không có camera.')
  }
}

function doc() {
  const v = video.value
  const c = canvas.value
  if (!v || !c || !jsQR || v.readyState < 2) return
  c.width = v.videoWidth
  c.height = v.videoHeight
  const ctx = c.getContext('2d', { willReadFrequently: true })
  ctx.drawImage(v, 0, 0, c.width, c.height)
  const anh = ctx.getImageData(0, 0, c.width, c.height)
  const kq = jsQR(anh.data, anh.width, anh.height, { inversionAttempts: 'dontInvert' })
  if (kq?.data) {
    tatCamera()
    emit('ma', kq.data.trim())
  }
}

function tatCamera() {
  clearInterval(hen)
  hen = null
  luong?.getTracks().forEach((t) => t.stop())
  luong = null
}

onMounted(batCamera)
onBeforeUnmount(tatCamera)
</script>

<template>
  <BaseModal title="Quét mã QR sản phẩm" size="md" @close="emit('close')">
    <div class="qm-khung">
      <video ref="video" class="qm-video" muted playsinline aria-label="Hình từ camera"></video>
      <div class="qm-ngam" aria-hidden="true"></div>
      <div v-if="dangMo" class="qm-thong-bao">Đang mở camera…</div>
    </div>
    <canvas ref="canvas" class="d-none"></canvas>
    <p v-if="loi" class="ad-error mt-2" role="alert">{{ loi }}</p>
    <p v-else class="ad-hint mt-2">Đưa mã QR của sản phẩm vào giữa khung. Đọc được mã là tự thêm vào hóa đơn.</p>
  </BaseModal>
</template>

<style scoped>
.qm-khung {
  position: relative;
  background: #0f172a;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 4 / 3;
}
.qm-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.qm-ngam {
  position: absolute;
  inset: 18%;
  border: 3px solid rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  box-shadow: 0 0 0 999px rgba(15, 23, 42, 0.35);
}
.qm-thong-bao {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
</style>
