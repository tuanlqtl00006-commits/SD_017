<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { diaChiHanhChinhService } from '../../services/diaChiHanhChinhService'
import { chuanHoaTenDonVi } from '../../utils/diaChi'

/*
 * Hai ô chọn Tỉnh/Thành phố -> Phường/Xã theo đơn vị hành chính SAU SÁP NHẬP (provinces.open-api.vn, phiên bản 2).
 * Không còn ô Quận/Huyện. Giá trị là TÊN đầy đủ (vd 'Thành phố Hà Nội', 'Phường Hồng Hà') để lưu thẳng vào CSDL.
 * Dùng bên trong một <div class="row">; mỗi ô là một cột (colClass).
 * Địa chỉ cũ (trước sáp nhập) không còn trong danh sách mới vẫn hiện được, kèm lời nhắc chọn lại.
 */
const props = defineProps({
  tinhThanh: { type: String, default: '' },
  phuongXa: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
  idPrefix: { type: String, default: 'dc' },
  colClass: { type: String, default: 'col-md-6' },
  selectClass: { type: String, default: 'form-select' },
  labelClass: { type: String, default: 'form-label small text-muted mb-1' },
  invalidTinh: { type: Boolean, default: false },
  invalidPhuong: { type: Boolean, default: false },
})
const emit = defineEmits(['update:tinhThanh', 'update:phuongXa'])

const tinhList = ref([])
const phuongList = ref([])
const dangTaiTinh = ref(false)
const dangTaiPhuong = ref(false)
const loiTai = ref('')

async function taiTinh() {
  dangTaiTinh.value = true
  loiTai.value = ''
  try {
    tinhList.value = await diaChiHanhChinhService.layTinhThanh()
  } catch {
    loiTai.value = 'Không tải được danh sách Tỉnh/Thành phố. Kiểm tra kết nối mạng rồi bấm “Tải lại”.'
  } finally {
    dangTaiTinh.value = false
  }
}

// Tìm tỉnh trong danh sách mới theo tên (so khớp không phân biệt dấu / tiền tố)
const tinhDangChon = computed(() => {
  if (!props.tinhThanh) return null
  const key = chuanHoaTenDonVi(props.tinhThanh)
  return tinhList.value.find((t) => t.name === props.tinhThanh) ?? tinhList.value.find((t) => chuanHoaTenDonVi(t.name) === key) ?? null
})

let seq = 0
async function taiPhuong() {
  const my = ++seq
  const t = tinhDangChon.value
  if (!t) {
    phuongList.value = []
    return
  }
  dangTaiPhuong.value = true
  try {
    const ds = await diaChiHanhChinhService.layPhuongXa(t.code)
    if (my === seq) phuongList.value = ds
  } catch {
    if (my === seq) {
      phuongList.value = []
      loiTai.value = 'Không tải được danh sách Phường/Xã. Hãy chọn lại Tỉnh/Thành phố hoặc bấm “Tải lại”.'
    }
  } finally {
    if (my === seq) dangTaiPhuong.value = false
  }
}

onMounted(taiTinh)
// Tên tỉnh đổi (người dùng chọn hoặc form nạp dữ liệu sửa) / danh sách tỉnh vừa tải xong -> tải phường xã tương ứng
watch([() => tinhDangChon.value?.code, tinhList], taiPhuong, { immediate: true })

// Giá trị cũ (trước sáp nhập) không có trong danh sách mới thì vẫn hiện để không mất dữ liệu
const tinhLaDiaChiCu = computed(() => !!props.tinhThanh && !dangTaiTinh.value && tinhList.value.length > 0 && !tinhDangChon.value)
const phuongLaDiaChiCu = computed(() => !!props.phuongXa && !dangTaiPhuong.value && !!tinhDangChon.value && phuongList.value.length > 0 && !phuongList.value.some((w) => w.name === props.phuongXa))

function chonTinh(e) {
  emit('update:tinhThanh', e.target.value)
  emit('update:phuongXa', '') // đổi tỉnh thì phải chọn lại phường/xã
}
function chonPhuong(e) {
  emit('update:phuongXa', e.target.value)
}
function taiLai() {
  loiTai.value = ''
  taiTinh().then(taiPhuong)
}
</script>

<template>
  <div :class="colClass">
    <label class="d-block" :class="labelClass" :for="`${idPrefix}-tinh`">Tỉnh/Thành phố <span v-if="required" class="text-danger">*</span></label>
    <select
      :id="`${idPrefix}-tinh`"
      :value="tinhThanh"
      :class="[selectClass, { 'is-invalid': invalidTinh }]"
      :disabled="disabled || dangTaiTinh"
      @change="chonTinh"
    >
      <option value="">{{ dangTaiTinh ? 'Đang tải…' : 'Chọn Tỉnh/Thành phố' }}</option>
      <option v-if="tinhLaDiaChiCu" :value="tinhThanh">{{ tinhThanh }} (địa chỉ cũ)</option>
      <option v-for="t in tinhList" :key="t.code" :value="t.name">{{ t.name }}</option>
    </select>
  </div>
  <div :class="colClass">
    <label class="d-block" :class="labelClass" :for="`${idPrefix}-phuong`">Phường/Xã <span v-if="required" class="text-danger">*</span></label>
    <select
      :id="`${idPrefix}-phuong`"
      :value="phuongXa"
      :class="[selectClass, { 'is-invalid': invalidPhuong }]"
      :disabled="disabled || !tinhDangChon || dangTaiPhuong"
      @change="chonPhuong"
    >
      <option value="">{{ dangTaiPhuong ? 'Đang tải…' : 'Chọn Phường/Xã' }}</option>
      <option v-if="phuongLaDiaChiCu" :value="phuongXa">{{ phuongXa }} (địa chỉ cũ)</option>
      <option v-for="w in phuongList" :key="w.code" :value="w.name">{{ w.name }}</option>
    </select>
  </div>
  <div v-if="loiTai" class="col-12">
    <small class="text-danger">{{ loiTai }}</small>
    <button type="button" class="btn btn-link btn-sm py-0" @click="taiLai">Tải lại</button>
  </div>
  <div v-else-if="(tinhLaDiaChiCu || phuongLaDiaChiCu) && !disabled" class="col-12">
    <small class="text-warning-emphasis">Địa chỉ này theo đơn vị hành chính cũ (trước sáp nhập). Hãy chọn lại {{ tinhLaDiaChiCu ? 'Tỉnh/Thành phố và ' : '' }}Phường/Xã theo danh sách mới.</small>
  </div>
</template>
