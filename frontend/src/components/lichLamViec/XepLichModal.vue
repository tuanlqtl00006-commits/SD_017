<script setup>
// Xếp lịch làm việc: chọn MỘT nhân viên, chọn ca, chọn ngày. Mỗi ca trong ngày chỉ một nhân viên (người đó nhận ca, bán, giữ két, giao ca).
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
const luaChonCa = computed(() => {
  const options = [{ value: '', label: 'Chọn ca…' }]
  for (const c of props.cas) {
    if (c.hoatDong) {
      options.push({
        value: String(c.id),
        label: `${c.tenCa} (${c.gioBatDau} - ${c.gioKetThuc})`,
      })
    } else {
      options.push({
        value: String(c.id),
        label: `${c.tenCa} (${c.gioBatDau} - ${c.gioKetThuc}) [Đang tắt ở tab Ca làm việc]`,
        disabled: true,
      })
    }
  }
  return options
})

const nvLoc = computed(() => {
  const k = boDau(timNv.value)
  return props.nhanViens.filter((n) => {
    if (k && !boDau(`${n.hoTen} ${n.maNhanVien}`).includes(k)) return false
    return true
  })
})

const ngayCon = computed(() => props.ngays.filter((d) => d.iso >= homNay))

// Chỉ chọn một nhân viên: bấm người khác là đổi sang người đó, bấm lại người đang chọn là bỏ chọn
function toggleChonNv(id) {
  f.idNhanViens = f.idNhanViens.includes(id) ? [] : [id]
  sua('idNhanViens')
}

function chonNgay(kieu) {
  const ds = kieu === 'thuong' ? ngayCon.value.filter((d) => !['Thứ 7', 'Chủ nhật'].includes(d.thu)) : ngayCon.value
  f.ngays = ds.map((d) => d.iso)
  sua('ngays')
}

const { loi, sua, batDauGui, datLoiMayChu } = useLoiForm(() => {
  const e = {}
  if (!f.idNhanViens.length) e.idNhanViens = 'Vui lòng chọn nhân viên.'
  if (!f.idCa) e.idCa = 'Vui lòng chọn ca làm việc.'
  if (!f.ngays.length) e.ngays = 'Vui lòng chọn ít nhất một ngày.'
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
    if (ketQua.value.daThem > 0) toast.success(`Đã xếp ${ketQua.value.daThem} ca làm việc.`)
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
      <div class="alert alert-success d-flex align-items-center mb-3">
        <b>Đã xếp thành công {{ ketQua.daThem }} ca làm việc.</b>
      </div>
      <div v-if="ketQua.boQua && ketQua.boQua.length" class="card border-warning mb-3">
        <div class="card-header bg-warning-subtle text-warning-emphasis py-2 fw-semibold">
          Có {{ ketQua.boQua.length }} ô không xếp được do trùng lịch:
        </div>
        <ul class="xl-bo-qua list-group list-group-flush mb-0">
          <li v-for="(b, i) in ketQua.boQua" :key="i" class="list-group-item py-2 small">
            <b>{{ b.hoTen }}</b> · {{ formatDate(b.ngay) }}: {{ b.lyDo }}
          </li>
        </ul>
      </div>
    </div>
    <form v-else id="xl-form" novalidate @submit.prevent="gui">
      <div class="row g-4">
        <!-- Cột trái: Chọn nhân viên -->
        <div class="col-md-6 border-end">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="ad-label mb-0">
              Nhân viên làm ca <span class="ad-required">*</span>
            </span>
            <small class="text-muted">Mỗi ca một nhân viên</small>
          </div>

          <input
            v-model="timNv"
            type="search"
            class="form-control form-control-sm mb-2"
            placeholder="Tìm theo tên hoặc mã NV…"
            aria-label="Tìm nhân viên"
          />

          <div class="xl-ds" :class="{ 'is-invalid': loi.idNhanViens }" role="radiogroup" aria-label="Chọn nhân viên">
            <div
              v-for="n in nvLoc"
              :key="n.id"
              class="xl-muc"
              :class="{ 'is-active': f.idNhanViens.includes(n.id) }"
              @click="toggleChonNv(n.id)"
            >
              <input
                type="radio"
                name="xl-nhan-vien"
                class="form-check-input mt-0 me-2"
                :checked="f.idNhanViens.includes(n.id)"
                :aria-label="n.hoTen"
                @click.stop="toggleChonNv(n.id)"
              />
              <div class="d-flex align-items-center justify-content-between flex-fill">
                <div>
                  <span class="fw-semibold text-dark">{{ n.hoTen }}</span>
                  <small class="text-muted ms-1">({{ n.maNhanVien }})</small>
                </div>
                <span
                  class="badge"
                  :class="/quản lý/i.test(n.vaiTro || '') ? 'bg-warning-subtle text-warning-emphasis' : 'bg-secondary-subtle text-secondary'"
                  style="font-size: 0.7rem"
                >
                  {{ /quản lý/i.test(n.vaiTro || '') ? 'Quản lý' : 'Nhân viên' }}
                </span>
              </div>
            </div>
            <p v-if="!nvLoc.length" class="ad-hint m-2">Không có nhân viên phù hợp.</p>
          </div>
          <p v-if="loi.idNhanViens" class="ad-error mt-1">{{ loi.idNhanViens }}</p>
        </div>

        <!-- Cột phải: Ca, Ngày -->
        <div class="col-md-6">
          <label class="ad-label" for="xl-ca">Ca làm việc <span class="ad-required">*</span></label>
          <Select2 id="xl-ca" v-model="f.idCa" :options="luaChonCa" :min-search="99" class="ad-control" :invalid="!!loi.idCa" @change="sua('idCa')" />
          <p v-if="loi.idCa" class="ad-error">{{ loi.idCa }}</p>

          <div class="d-flex justify-content-between align-items-end mt-3 mb-1">
            <span class="ad-label mb-0">Ngày làm việc <span class="ad-required">*</span></span>
            <span class="small">
              <button type="button" class="xl-lk" @click="chonNgay('thuong')">Thứ 2 - Thứ 6</button> ·
              <button type="button" class="xl-lk" @click="chonNgay('ca-tuan')">Cả tuần</button>
            </span>
          </div>
          <div class="xl-ngay" role="group" aria-label="Chọn ngày">
            <label v-for="d in ngays" :key="d.iso" class="xl-ngay-muc" :class="{ 'is-qua': d.iso < homNay, 'is-checked': f.ngays.includes(d.iso) }" :title="d.iso < homNay ? 'Ngày đã qua' : ''">
              <input v-model="f.ngays" type="checkbox" class="form-check-input" :value="d.iso" :disabled="d.iso < homNay" @change="sua('ngays')" />
              <span>{{ d.thu }}<br /><small>{{ d.ngay }}</small></span>
            </label>
          </div>
          <p v-if="loi.ngays" class="ad-error">{{ loi.ngays }}</p>

          <label class="ad-label mt-3" for="xl-ghi-chu">Ghi chú</label>
          <input
            id="xl-ghi-chu"
            v-model="f.ghiChu"
            type="text"
            class="form-control form-control-sm"
            maxlength="255"
            placeholder="Ghi chú thêm cho ca (không bắt buộc)"
            @input="sua('ghiChu')"
          />
          <p v-if="loi.ghiChu" class="ad-error">{{ loi.ghiChu }}</p>

          <p class="ad-hint mt-2 mb-0">
            Sẽ xếp <b>{{ soO }}</b> ca ({{ f.ngays.length }} ngày). Nhân viên này tự nhận ca, bán hàng, giữ két và giao ca. Ngày mà ca đã có người khác sẽ được bỏ qua.
          </p>
        </div>
      </div>
    </form>
    <template #footer>
      <template v-if="ketQua">
        <button type="button" class="ad-btn ad-btn-primary" @click="emit('close')">Đóng</button>
      </template>
      <template v-else>
        <button type="button" class="ad-btn" @click="emit('close')">Hủy</button>
        <button type="submit" form="xl-form" class="ad-btn ad-btn-primary" :disabled="dangGui">
          {{ dangGui ? 'Đang xếp lịch…' : 'Xếp lịch' }}
        </button>
      </template>
    </template>
  </BaseModal>
</template>

<style scoped>
.xl-ds {
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid var(--fs-line, #e2e8f0);
  border-radius: 8px;
  padding: 0.35rem;
}
.xl-ds.is-invalid {
  border-color: var(--fs-danger, #dc3545);
}
.xl-muc {
  display: flex;
  align-items: center;
  padding: 0.45rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  user-select: none;
}
.xl-muc:hover {
  background: #f1f5f9;
}
.xl-muc.is-active {
  background: #f0fdf4;
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
  gap: 0.2rem;
  border: 1px solid var(--fs-line, #e2e8f0);
  border-radius: 8px;
  padding: 0.35rem 0.15rem;
  font-size: 0.8rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.xl-ngay-muc:hover:not(.is-qua) {
  border-color: #0977ec;
  background: #f0f7ff;
}
.xl-ngay-muc.is-checked {
  border-color: #0977ec;
  background: #eff6ff;
  font-weight: 600;
}
.xl-ngay-muc.is-qua {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f8fafc;
}
.xl-lk {
  border: 0;
  background: none;
  color: var(--fs-primary, #0977ec);
  padding: 0;
  cursor: pointer;
}
.xl-lk:hover {
  text-decoration: underline;
}

.xl-bo-qua {
  max-height: 200px;
  overflow-y: auto;
}
</style>
