<script setup>
// Xếp lịch nhanh: nhiều nhân viên x một ca x nhiều ngày trong tuần đang xem. Ô không hợp lệ (ngày đã qua, trùng / chồng giờ...)
// được backend bỏ qua và trả lý do; hộp này hiện kết quả để quản lý biết ô nào chưa xếp được.
import { computed, reactive, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import Select2 from '../common/Select2.vue'
import { lichLamViecService } from '../../services/lichLamViecService'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { formatDate, todayIso } from '../../utils/format'
import { normalizeText as boDau } from '../../utils/text'

const props = defineProps({
  nhanViens: { type: Array, default: () => [] },
  cas: { type: Array, default: () => [] },
  ngays: { type: Array, required: true }, // 7 ngày của tuần đang xem [{ iso, thu, ngay }]
  chonSan: { type: Object, default: null }, // { idNhanVien, ngay } khi mở từ một ô của bảng
})
const emit = defineEmits(['close', 'xong'])
const toast = useToast()
const homNay = todayIso()

const f = reactive({
  idNhanViens: props.chonSan ? [props.chonSan.idNhanVien] : [],
  idCa: '',
  ngays: props.chonSan ? [props.chonSan.ngay] : [],
  ghiChu: '',
})
const timNv = ref('')
const dangGui = ref(false)
const ketQua = ref(null)

const caDangDung = computed(() => props.cas.filter((c) => c.hoatDong))
const luaChonCa = computed(() => [
  { value: '', label: 'Chọn ca…' },
  ...caDangDung.value.map((c) => ({ value: String(c.id), label: `${c.tenCa} (${c.gioBatDau} - ${c.gioKetThuc})` })),
])
const nvLoc = computed(() => {
  const k = boDau(timNv.value)
  return props.nhanViens.filter((n) => !k || boDau(`${n.hoTen} ${n.maNhanVien}`).includes(k))
})
const ngayCon = computed(() => props.ngays.filter((d) => d.iso >= homNay))

function chonTatCaNv(chon) {
  f.idNhanViens = chon ? nvLoc.value.map((n) => n.id) : []
  sua('idNhanViens')
}
function chonNgay(kieu) {
  const ds = kieu === 'thuong' ? ngayCon.value.filter((d) => !['Thứ 7', 'Chủ nhật'].includes(d.thu)) : ngayCon.value
  f.ngays = ds.map((d) => d.iso)
  sua('ngays')
}

const { loi, sua, batDauGui, datLoiMayChu } = useLoiForm(() => {
  const e = {}
  if (!f.idNhanViens.length) e.idNhanViens = 'Chọn ít nhất một nhân viên.'
  if (!f.idCa) e.idCa = 'Chọn ca làm việc.'
  if (!f.ngays.length) e.ngays = 'Chọn ít nhất một ngày.'
  if (f.ghiChu.trim().length > 255) e.ghiChu = 'Ghi chú tối đa 255 ký tự.'
  return e
})
const soO = computed(() => f.idNhanViens.length * f.ngays.length)

async function gui() {
  if (dangGui.value || !batDauGui()) return
  dangGui.value = true
  try {
    ketQua.value = await lichLamViecService.xepLich({
      idNhanViens: f.idNhanViens,
      idCa: Number(f.idCa),
      ngays: [...f.ngays].sort(),
      ghiChu: f.ghiChu.trim(),
    })
    if (ketQua.value.daThem > 0) toast.success(`Đã xếp ${ketQua.value.daThem} ca.`)
    emit('xong')
    if (!ketQua.value.boQua?.length) emit('close')
  } catch (e) {
    if (!datLoiMayChu(e)) toast.error(e.message || 'Không xếp được lịch.')
  } finally {
    dangGui.value = false
  }
}
</script>

<template>
  <BaseModal title="Xếp lịch làm việc" size="lg" :static-backdrop="dangGui" @close="emit('close')">
    <div v-if="ketQua" class="xl-ket-qua">
      <p><b>Đã xếp {{ ketQua.daThem }} ca.</b> Có {{ ketQua.boQua.length }} ô không xếp được:</p>
      <ul class="xl-bo-qua">
        <li v-for="(b, i) in ketQua.boQua" :key="i"><b>{{ b.hoTen }}</b> · {{ formatDate(b.ngay) }}: {{ b.lyDo }}</li>
      </ul>
    </div>
    <form v-else id="xl-form" novalidate @submit.prevent="gui">
      <div class="row g-3">
        <div class="col-md-6">
          <div class="d-flex justify-content-between align-items-end">
            <span class="ad-label">Nhân viên <span class="ad-required">*</span></span>
            <span class="small">
              <button type="button" class="xl-lk" @click="chonTatCaNv(true)">Chọn tất cả</button> ·
              <button type="button" class="xl-lk" @click="chonTatCaNv(false)">Bỏ chọn</button>
            </span>
          </div>
          <input v-model="timNv" type="search" class="form-control ad-control mb-2" placeholder="Tìm nhân viên…" aria-label="Tìm nhân viên" />
          <div class="xl-ds" :class="{ 'is-invalid': loi.idNhanViens }" role="group" aria-label="Chọn nhân viên">
            <label v-for="n in nvLoc" :key="n.id" class="xl-muc">
              <input v-model="f.idNhanViens" type="checkbox" class="form-check-input" :value="n.id" @change="sua('idNhanViens')" />
              <span>{{ n.hoTen }} <small class="text-muted">{{ n.maNhanVien }}</small></span>
            </label>
            <p v-if="!nvLoc.length" class="ad-hint m-2">Không có nhân viên phù hợp.</p>
          </div>
          <p v-if="loi.idNhanViens" class="ad-error">{{ loi.idNhanViens }}</p>
        </div>
        <div class="col-md-6">
          <label class="ad-label" for="xl-ca">Ca làm việc <span class="ad-required">*</span></label>
          <Select2 id="xl-ca" v-model="f.idCa" :options="luaChonCa" :min-search="99" class="ad-control" :invalid="!!loi.idCa" @change="sua('idCa')" />
          <p v-if="loi.idCa" class="ad-error">{{ loi.idCa }}</p>

          <div class="d-flex justify-content-between align-items-end mt-3">
            <span class="ad-label">Ngày <span class="ad-required">*</span></span>
            <span class="small">
              <button type="button" class="xl-lk" @click="chonNgay('thuong')">Thứ 2 - Thứ 6</button> ·
              <button type="button" class="xl-lk" @click="chonNgay('ca-tuan')">Cả tuần</button>
            </span>
          </div>
          <div class="xl-ngay" role="group" aria-label="Chọn ngày">
            <label v-for="d in ngays" :key="d.iso" class="xl-ngay-muc" :class="{ 'is-qua': d.iso < homNay }" :title="d.iso < homNay ? 'Ngày đã qua' : ''">
              <input v-model="f.ngays" type="checkbox" class="form-check-input" :value="d.iso" :disabled="d.iso < homNay" @change="sua('ngays')" />
              <span>{{ d.thu }}<br /><small>{{ d.ngay }}</small></span>
            </label>
          </div>
          <p v-if="loi.ngays" class="ad-error">{{ loi.ngays }}</p>

          <label class="ad-label mt-3" for="xl-ghi-chu">Ghi chú</label>
          <input id="xl-ghi-chu" v-model="f.ghiChu" type="text" class="form-control ad-control" maxlength="255" placeholder="vd: Trực quầy, kiểm kho…" @input="sua('ghiChu')" />
          <p v-if="loi.ghiChu" class="ad-error">{{ loi.ghiChu }}</p>
          <p class="ad-hint mt-2">Sẽ xếp {{ soO }} ô (nhân viên × ngày). Ô trùng ca hoặc chồng giờ với ca khác sẽ được bỏ qua.</p>
        </div>
      </div>
    </form>
    <template #footer>
      <template v-if="ketQua">
        <button type="button" class="ad-btn ad-btn-primary" @click="emit('close')">Đóng</button>
      </template>
      <template v-else>
        <button type="button" class="ad-btn" @click="emit('close')">Hủy</button>
        <button type="submit" form="xl-form" class="ad-btn ad-btn-primary" :disabled="dangGui">Xếp lịch</button>
      </template>
    </template>
  </BaseModal>
</template>

<style scoped>
.xl-ds {
  max-height: 260px;
  overflow: auto;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  padding: 0.25rem;
}
.xl-ds.is-invalid {
  border-color: var(--fs-danger, #dc3545);
}
.xl-muc {
  display: flex;
  gap: 0.6rem;
  align-items: center;
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  cursor: pointer;
}
.xl-muc:hover {
  background: var(--fs-hover-row, #f1f7fe);
}
.xl-ngay {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.35rem;
}
.xl-ngay-muc {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  padding: 0.4rem 0.2rem;
  font-size: 0.82rem;
  text-align: center;
  cursor: pointer;
}
.xl-ngay-muc.is-qua {
  opacity: 0.45;
  cursor: not-allowed;
}
.xl-lk {
  border: 0;
  background: none;
  color: var(--fs-primary, #0977ec);
  padding: 0;
}
.xl-bo-qua {
  max-height: 300px;
  overflow: auto;
  padding-left: 1.2rem;
}
</style>
