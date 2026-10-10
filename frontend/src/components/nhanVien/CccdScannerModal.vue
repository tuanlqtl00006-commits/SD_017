<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { docMaQrCccd, quetQrTuAnh } from '../../utils/cccd'

/*
 * Quét căn cước công dân (mã QR trên thẻ) để điền sẵn họ tên, ngày sinh, giới tính, số CCCD.
 * 3 cách: (1) camera / webcam, (2) tải ảnh chụp mã QR, (3) máy quét mã vạch cầm tay hoặc dán nội dung mã.
 * Thành công thì phát sự kiện 'scanned' (kèm thông tin đã tách) rồi tự đóng.
 * Camera chỉ chạy được khi trang mở bằng https hoặc http://localhost.
 */
const emit = defineEmits(['close', 'scanned'])

const CACH_QUET = [
  { key: 'camera', label: 'Camera', icon: 'bi-camera' },
  { key: 'anh', label: 'Tải ảnh QR', icon: 'bi-image' },
  { key: 'may-quet', label: 'Máy quét', icon: 'bi-upc-scan' },
]
const tab = ref('camera') // camera | anh | may-quet
const loi = ref('')
const nhac = ref('') // nhắc nhẹ khi camera thấy mã QR không phải của CCCD
const dangMoCamera = ref(false)
const cameraSan = ref(false)
const dangDocAnh = ref(false)
const noiDungDan = ref('')

const videoRef = ref(null)
const oDan = ref(null)
let stream = null
let timer = 0
let dang = true // false khi đã đóng / đã quét xong
let jsQR = null
const canvas = document.createElement('canvas')

async function layJsQR() {
  if (!jsQR) jsQR = (await import('jsqr')).default
  return jsQR
}

function xuLy(noiDung) {
  try {
    const info = docMaQrCccd(noiDung)
    dang = false
    dungCamera()
    emit('scanned', info)
    emit('close')
    return true
  } catch (e) {
    return e.message
  }
}

/* ----- 1. Camera ----- */
function dungCamera() {
  clearTimeout(timer)
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    stream = null
  }
  cameraSan.value = false
  if (videoRef.value) videoRef.value.srcObject = null
}

async function moCamera() {
  loi.value = ''
  nhac.value = ''
  if (!navigator.mediaDevices?.getUserMedia) {
    loi.value = 'Trình duyệt không mở được camera (cần https hoặc http://localhost). Hãy dùng “Tải ảnh QR” hoặc “Máy quét”.'
    return
  }
  dangMoCamera.value = true
  try {
    const s = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 } }, audio: false })
    if (!dang || tab.value !== 'camera') {
      s.getTracks().forEach((t) => t.stop()) // người dùng đã đóng / đổi tab trong lúc chờ cấp quyền
      return
    }
    stream = s
    await nextTick()
    const v = videoRef.value
    if (!v) return dungCamera()
    v.srcObject = s
    await v.play()
    cameraSan.value = true
    await layJsQR()
    quetVong()
  } catch (e) {
    if (e?.name === 'NotAllowedError') loi.value = 'Bạn chưa cho phép dùng camera. Hãy cho phép camera trong trình duyệt rồi bấm “Mở lại camera”.'
    else if (e?.name === 'NotFoundError' || e?.name === 'OverconstrainedError') loi.value = 'Không tìm thấy camera trên thiết bị này. Hãy dùng “Tải ảnh QR” hoặc “Máy quét”.'
    else loi.value = 'Không mở được camera: ' + (e?.message || 'lỗi không xác định') + '.'
  } finally {
    dangMoCamera.value = false
  }
}

function quetVong() {
  const v = videoRef.value
  if (!dang || !v || !stream) return
  if (v.readyState >= 2 && v.videoWidth) {
    const ti = Math.min(1, 800 / v.videoWidth)
    const w = Math.round(v.videoWidth * ti)
    const h = Math.round(v.videoHeight * ti)
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    ctx.drawImage(v, 0, 0, w, h)
    const kq = jsQR(ctx.getImageData(0, 0, w, h).data, w, h, { inversionAttempts: 'dontInvert' })
    if (kq?.data) {
      const r = xuLy(kq.data)
      if (r === true) return
      nhac.value = r // QR khác (không phải CCCD): báo nhẹ và quét tiếp
    }
  }
  timer = setTimeout(quetVong, 120)
}

/* ----- 2. Ảnh ----- */
async function chonAnh(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  loi.value = ''
  dangDocAnh.value = true
  try {
    const noiDung = await quetQrTuAnh(file, await layJsQR())
    if (!noiDung) {
      loi.value = 'Không tìm thấy mã QR trong ảnh. Hãy chụp rõ nét, sát mã QR trên thẻ, đủ sáng và không bị lóa.'
      return
    }
    const r = xuLy(noiDung)
    if (r !== true) loi.value = r
  } catch (err) {
    loi.value = 'Không đọc được ảnh: ' + (err?.message || 'file không hợp lệ') + '.'
  } finally {
    dangDocAnh.value = false
  }
}

/* ----- 3. Máy quét cầm tay / dán nội dung ----- */
function docNoiDungDan() {
  loi.value = ''
  const r = xuLy(noiDungDan.value)
  if (r !== true) loi.value = r
}

watch(tab, async (t) => {
  loi.value = ''
  nhac.value = ''
  dungCamera()
  await nextTick()
  if (t === 'camera') moCamera()
  if (t === 'may-quet') oDan.value?.focus()
})

onMounted(moCamera)
onBeforeUnmount(() => {
  dang = false
  dungCamera()
})
</script>

<template>
  <BaseModal title="Quét căn cước công dân" size="lg" @close="emit('close')">
    <div class="ad-segment mb-3" role="radiogroup" aria-label="Cách quét">
      <label v-for="t in CACH_QUET" :key="t.key" class="ad-segment-item" :class="{ active: tab === t.key }">
        <input v-model="tab" type="radio" class="visually-hidden" name="cccd-cach-quet" :value="t.key" />
        <i class="bi" :class="t.icon" aria-hidden="true"></i> {{ t.label }}
      </label>
    </div>

    <!-- Camera -->
    <div v-show="tab === 'camera'">
      <div class="cccd-cam">
        <video ref="videoRef" class="cccd-video" muted playsinline></video>
        <div v-if="cameraSan" class="cccd-khung" aria-hidden="true"></div>
        <div v-if="dangMoCamera" class="cccd-cho">Đang mở camera…</div>
      </div>
      <p class="ad-hint">Đưa mã QR trên thẻ căn cước vào giữa khung, giữ thẻ thẳng, đủ sáng. Hệ thống tự nhận khi quét được.</p>
      <p v-if="nhac" class="ad-error">{{ nhac }}</p>
      <button v-if="!cameraSan && !dangMoCamera" type="button" class="ad-btn mt-2" @click="moCamera">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Mở lại camera
      </button>
    </div>

    <!-- Ảnh -->
    <div v-if="tab === 'anh'">
      <label class="ad-label" for="cccd-file">Chọn ảnh chụp mã QR (hoặc ảnh chụp thẻ căn cước)</label>
      <input id="cccd-file" type="file" accept="image/*" class="form-control ad-control" :disabled="dangDocAnh" @change="chonAnh" />
      <p v-if="dangDocAnh" class="ad-hint">Đang đọc ảnh…</p>
      <p v-else class="ad-hint">Ảnh nên rõ nét, mã QR đủ lớn và không bị lóa.</p>
    </div>

    <!-- Máy quét -->
    <div v-if="tab === 'may-quet'">
      <label class="ad-label" for="cccd-dan">Nội dung mã QR</label>
      <input
        id="cccd-dan"
        ref="oDan"
        v-model="noiDungDan"
        type="text"
        class="form-control ad-control"
        placeholder="Bấm vào ô này rồi quét bằng máy quét mã vạch, hoặc dán nội dung mã"
        autocomplete="off"
        @keydown.enter.prevent="docNoiDungDan"
      />
      <p class="ad-hint">Máy quét cầm tay sẽ tự gõ nội dung và nhấn Enter. Có thể dán nội dung rồi bấm “Đọc mã”.</p>
      <button type="button" class="ad-btn ad-btn-primary mt-2" :disabled="!noiDungDan.trim()" @click="docNoiDungDan">Đọc mã</button>
    </div>

    <p v-if="loi" class="ad-error mt-3" role="alert">{{ loi }}</p>
  </BaseModal>
</template>

<style scoped>
.cccd-cam {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 260px;
  max-height: 60vh;
  overflow: hidden;
  border-radius: 14px;
  background: #0f1820;
}
.cccd-video {
  width: 100%;
  max-height: 60vh;
  object-fit: cover;
}
.cccd-khung {
  position: absolute;
  inset: 12% 22%;
  border: 3px solid rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  box-shadow: 0 0 0 999px rgba(15, 24, 32, 0.35);
  pointer-events: none;
}
.cccd-cho {
  position: absolute;
  color: #fff;
  font-size: 0.9rem;
}
</style>
