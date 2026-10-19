<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '../common/BaseModal.vue'
import api from '../../services/api'
import { quetMaTuAnh, trichXuatMaHoaDon } from '../../utils/qrHoaDon'

const emit = defineEmits(['close'])
const router = useRouter()

const CACH_QUET = [
  { key: 'camera', label: 'Camera', icon: 'bi-camera' },
  { key: 'anh', label: 'Tải ảnh mã', icon: 'bi-image' },
  { key: 'may-quet', label: 'Máy quét / Nhập mã', icon: 'bi-upc-scan' },
]

const tab = ref('camera') // camera | anh | may-quet
const loi = ref('')
const dangMoCamera = ref(false)
const cameraSan = ref(false)
const dangDocAnh = ref(false)
const dangTraCuu = ref(false)
const noiDungDan = ref('')
const maDangTraCuu = ref('')

// Quản lý thiết bị camera
const videoRef = ref(null)
const oDan = ref(null)
const dsThietBiCamera = ref([])
const idCameraDangDung = ref('')
let stream = null
let timer = 0
let dangHoatDong = true
let jsQR = null
const canvas = document.createElement('canvas')

async function layJsQR() {
  if (!jsQR) jsQR = (await import('jsqr')).default
  return jsQR
}

/* ===================== 1. Xử lý tra cứu & mở ngay chi tiết ===================== */
async function xuLyMa(chuoiRaw) {
  const ma = trichXuatMaHoaDon(chuoiRaw)
  if (!ma) {
    loi.value = 'Không tìm thấy nội dung mã hóa đơn hợp lệ.'
    return
  }

  dungCamera()
  loi.value = ''
  dangTraCuu.value = true
  maDangTraCuu.value = ma

  try {
    let idHoaDon = null

    // 1. Thử gọi endpoint tra cứu nhanh chuyên biệt /hoa-don/tra-cuu
    try {
      const res = await api.get('/hoa-don/tra-cuu', { params: { ma } })
      idHoaDon = res.data?.hoaDon?.id
    } catch {
      // 2. Fallback tìm qua danh sách /hoa-don?maHoaDon=...
      const resList = await api.get('/hoa-don', { params: { maHoaDon: ma, size: 5 } })
      const list = resList.data?.content || []
      const match = list.find((h) => String(h.maHoaDon).toLowerCase() === ma.toLowerCase())
        || (list.length === 1 && String(list[0].maHoaDon).toLowerCase().includes(ma.toLowerCase()) ? list[0] : null)
      if (match?.id) {
        idHoaDon = match.id
      }
    }

    if (!idHoaDon) {
      loi.value = `Không tìm thấy hóa đơn có mã "${ma}". Vui lòng kiểm tra lại mã hoặc quét lại.`
      return
    }

    // Tìm thấy hóa đơn -> Đóng modal và mở ngay trang chi tiết hóa đơn
    emit('close')
    router.push(`/hoa-don/${idHoaDon}`)
  } catch (err) {
    loi.value = err?.response?.data?.message || `Không tìm thấy hóa đơn có mã "${ma}". Vui lòng thử lại.`
  } finally {
    dangTraCuu.value = false
  }
}

function thuLai() {
  loi.value = ''
  maDangTraCuu.value = ''
  noiDungDan.value = ''
  if (tab.value === 'camera') {
    moCamera()
  } else if (tab.value === 'may-quet') {
    nextTick(() => oDan.value?.focus())
  }
}

/* ===================== 2. Quét bằng Camera ===================== */
function dungCamera() {
  clearTimeout(timer)
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    stream = null
  }
  cameraSan.value = false
  if (videoRef.value) videoRef.value.srcObject = null
}

async function layDanhSachCamera() {
  if (!navigator.mediaDevices?.enumerateDevices) return
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    dsThietBiCamera.value = devices.filter((d) => d.kind === 'videoinput')
  } catch {
    // bỏ qua
  }
}

async function moCamera() {
  dungCamera()
  loi.value = ''
  if (!navigator.mediaDevices?.getUserMedia) {
    loi.value = 'Trình duyệt không mở được camera (cần https hoặc http://localhost). Hãy dùng “Tải ảnh mã” hoặc “Máy quét”.'
    return
  }

  dangMoCamera.value = true
  try {
    const videoConstraints = idCameraDangDung.value
      ? { deviceId: { exact: idCameraDangDung.value } }
      : { facingMode: { ideal: 'environment' }, width: { ideal: 1280 } }

    const s = await navigator.mediaDevices.getUserMedia({ video: videoConstraints, audio: false })
    if (!dangHoatDong || tab.value !== 'camera') {
      s.getTracks().forEach((t) => t.stop())
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
    layDanhSachCamera()
    quetVong()
  } catch (e) {
    if (e?.name === 'NotAllowedError') {
      loi.value = 'Bạn chưa cho phép truy cập camera. Vui lòng cho phép quyền trong trình duyệt rồi bấm "Mở lại camera".'
    } else if (e?.name === 'NotFoundError' || e?.name === 'OverconstrainedError') {
      loi.value = 'Không tìm thấy camera trên thiết bị. Hãy dùng "Tải ảnh mã" hoặc "Máy quét".'
    } else {
      loi.value = 'Không thể mở camera: ' + (e?.message || 'lỗi không xác định') + '.'
    }
  } finally {
    dangMoCamera.value = false
  }
}

async function doiCamera() {
  if (dsThietBiCamera.value.length < 2) return
  const hienTaiIdx = dsThietBiCamera.value.findIndex((d) => d.deviceId === idCameraDangDung.value)
  const tiepTheo = dsThietBiCamera.value[(hienTaiIdx + 1) % dsThietBiCamera.value.length]
  idCameraDangDung.value = tiepTheo.deviceId
  await moCamera()
}

async function quetVong() {
  const v = videoRef.value
  if (!dangHoatDong || !v || !stream || dangTraCuu.value) return

  if (v.readyState >= 2 && v.videoWidth) {
    let maDocDuoc = ''

    // Thử BarcodeDetector native trước (nếu trình duyệt có sẵn)
    if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
      try {
        const detector = new window.BarcodeDetector({
          formats: ['qr_code', 'code_128', 'code_39', 'ean_13', 'ean_8', 'upc_a', 'upc_e'],
        })
        const barcodes = await detector.detect(v)
        if (barcodes && barcodes.length > 0 && barcodes[0].rawValue) {
          maDocDuoc = barcodes[0].rawValue
        }
      } catch {
        // Fallback sang jsQR
      }
    }

    // Nếu BarcodeDetector chưa đọc được -> dùng jsQR
    if (!maDocDuoc && jsQR) {
      const ti = Math.min(1, 800 / v.videoWidth)
      const w = Math.round(v.videoWidth * ti)
      const h = Math.round(v.videoHeight * ti)
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d', { willReadFrequently: true })
      ctx.drawImage(v, 0, 0, w, h)
      const kq = jsQR(ctx.getImageData(0, 0, w, h).data, w, h, { inversionAttempts: 'dontInvert' })
      if (kq?.data) maDocDuoc = kq.data
    }

    if (maDocDuoc) {
      await xuLyMa(maDocDuoc)
      return
    }
  }

  timer = setTimeout(quetVong, 120)
}

/* ===================== 3. Quét từ ảnh ===================== */
async function chonAnh(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  loi.value = ''
  dangDocAnh.value = true
  try {
    const maDocDuoc = await quetMaTuAnh(file, await layJsQR())
    if (!maDocDuoc) {
      loi.value = 'Không tìm thấy mã QR hoặc mã vạch trong ảnh. Hãy chụp rõ nét, đủ sáng và không bị lóa.'
      return
    }
    await xuLyMa(maDocDuoc)
  } catch (err) {
    loi.value = 'Không đọc được file ảnh: ' + (err?.message || 'file không hợp lệ') + '.'
  } finally {
    dangDocAnh.value = false
  }
}

/* ===================== 4. Máy quét cầm tay / Nhập mã ===================== */
function docNoiDungDan() {
  if (!noiDungDan.value.trim()) return
  xuLyMa(noiDungDan.value)
}

watch(tab, async (t) => {
  loi.value = ''
  dungCamera()
  await nextTick()
  if (t === 'camera') moCamera()
  if (t === 'may-quet') oDan.value?.focus()
})

onMounted(() => {
  if (tab.value === 'camera') moCamera()
})

onBeforeUnmount(() => {
  dangHoatDong = false
  dungCamera()
})
</script>

<template>
  <BaseModal title="Quét mã hóa đơn" size="lg" @close="emit('close')">
    <!-- Bộ chuyển tab phương thức quét -->
    <div class="ad-segment mb-3" role="radiogroup" aria-label="Cách quét">
      <label v-for="t in CACH_QUET" :key="t.key" class="ad-segment-item" :class="{ active: tab === t.key }">
        <input v-model="tab" type="radio" class="visually-hidden" name="hd-cach-quet" :value="t.key" />
        <i class="bi" :class="t.icon" aria-hidden="true"></i> {{ t.label }}
      </label>
    </div>

    <!-- Trạng thái đang tra cứu -->
    <div v-if="dangTraCuu" class="hd-scan-loading py-5 text-center">
      <div class="spinner-border text-primary mb-3" role="status">
        <span class="visually-hidden">Đang tìm...</span>
      </div>
      <p class="fs-6 fw-semibold text-dark mb-1">Đang tìm hóa đơn: <span class="ad-code">{{ maDangTraCuu }}</span></p>
      <p class="text-muted small">Hệ thống sẽ mở ngay chi tiết hóa đơn khi tìm thấy…</p>
    </div>

    <!-- KHU VỰC CÁC TAB QUÉT KHI CHƯA HOÀN TẤT -->
    <div v-else>
      <!-- 1. Tab Camera -->
      <div v-show="tab === 'camera'">
        <div class="hd-cam-box">
          <video ref="videoRef" class="hd-video" muted playsinline></video>
          <div v-if="cameraSan" class="hd-khung-quet" aria-hidden="true">
            <div class="hd-laser-line"></div>
          </div>
          <div v-if="dangMoCamera" class="hd-cho-cam">
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span> Đang mở camera…
          </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-2">
          <p class="ad-hint mb-0">Đưa mã QR hoặc mã vạch trên hóa đơn vào giữa khung hình. Hệ thống sẽ tự động quét và mở hóa đơn.</p>
          <button
            v-if="dsThietBiCamera.length > 1"
            type="button"
            class="btn btn-sm btn-outline-secondary"
            title="Đổi camera trước / sau"
            @click="doiCamera"
          >
            <i class="bi bi-arrow-repeat me-1"></i> Đổi camera
          </button>
        </div>

        <div v-if="!cameraSan && !dangMoCamera" class="mt-2 text-center">
          <button type="button" class="ad-btn" @click="moCamera">
            <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Mở lại camera
          </button>
        </div>
      </div>

      <!-- 2. Tab Tải ảnh -->
      <div v-if="tab === 'anh'">
        <label class="ad-label" for="hd-file-qr">Chọn ảnh chụp hóa đơn (chứa mã QR hoặc mã vạch)</label>
        <input
          id="hd-file-qr"
          type="file"
          accept="image/*"
          class="form-control ad-control"
          :disabled="dangDocAnh"
          @change="chonAnh"
        />
        <p v-if="dangDocAnh" class="ad-hint text-primary mt-2">
          <span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span> Đang quét mã trong ảnh…
        </p>
        <p v-else class="ad-hint mt-2">
          Ảnh nên chụp rõ nét, không bị mờ nhòe hoặc bóng lóa để nhận diện nhanh nhất.
        </p>
      </div>

      <!-- 3. Tab Máy quét / Nhập mã -->
      <div v-if="tab === 'may-quet'">
        <label class="ad-label" for="hd-input-scan">Mã hóa đơn</label>
        <div class="input-group">
          <input
            id="hd-input-scan"
            ref="oDan"
            v-model="noiDungDan"
            type="text"
            class="form-control ad-control"
            placeholder="Dùng máy quét cầm tay quét vào mã, hoặc nhập mã (vd: HD16)..."
            autocomplete="off"
            @keydown.enter.prevent="docNoiDungDan"
          />
          <button
            type="button"
            class="ad-btn ad-btn-primary"
            :disabled="!noiDungDan.trim()"
            @click="docNoiDungDan"
          >
            <i class="bi bi-search me-1" aria-hidden="true"></i> Tìm & mở hóa đơn
          </button>
        </div>
        <p class="ad-hint mt-2">
          Hỗ trợ máy quét mã vạch / QR cầm tay (USB hoặc Bluetooth). Máy quét sẽ tự gõ và mở ngay hóa đơn.
        </p>
      </div>
    </div>

    <!-- Thông báo lỗi khi không tìm thấy hoặc quét lỗi -->
    <div v-if="loi" class="alert alert-danger mt-3 mb-0 d-flex align-items-center justify-content-between" role="alert">
      <div>
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <span>{{ loi }}</span>
      </div>
      <button type="button" class="btn btn-sm btn-outline-danger ms-2" @click="thuLai">
        Thử lại
      </button>
    </div>
  </BaseModal>
</template>

<style scoped>
.hd-cam-box {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 280px;
  max-height: 55vh;
  overflow: hidden;
  border-radius: 14px;
  background: #0f1820;
}

.hd-video {
  width: 100%;
  max-height: 55vh;
  object-fit: cover;
}

.hd-khung-quet {
  position: absolute;
  inset: 15% 20%;
  border: 3px solid rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  box-shadow: 0 0 0 999px rgba(15, 24, 32, 0.45);
  pointer-events: none;
  overflow: hidden;
}

/* Hiệu ứng tia quét laser */
.hd-laser-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #0977ec 20%, #38bdf8 50%, #0977ec 80%, transparent);
  box-shadow: 0 0 10px 2px rgba(56, 189, 248, 0.7);
  animation: hdLaserScan 2s ease-in-out infinite alternate;
}

@keyframes hdLaserScan {
  0% {
    top: 5%;
  }
  100% {
    top: 95%;
  }
}

.hd-cho-cam {
  position: absolute;
  color: #fff;
  font-size: 0.95rem;
  background: rgba(0, 0, 0, 0.6);
  padding: 8px 16px;
  border-radius: 8px;
}
</style>
