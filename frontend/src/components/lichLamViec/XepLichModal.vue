<script setup>
// Xếp lịch làm việc: chọn ca, chọn ngày, chọn nhân viên và phân công Thu ngân giữ két ngay trong ca.
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
  idThuNgan: null,
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

const nhanViensDaChon = computed(() => {
  return props.nhanViens.filter((n) => f.idNhanViens.includes(n.id))
})

const ngayCon = computed(() => props.ngays.filter((d) => d.iso >= homNay))

function chonTatCaNv(chon) {
  f.idNhanViens = chon ? nvLoc.value.map((n) => n.id) : []
  // Nếu người đang giữ két bị bỏ chọn, reset idThuNgan
  if (!f.idNhanViens.includes(f.idThuNgan)) {
    f.idThuNgan = f.idNhanViens.length > 0 ? f.idNhanViens[0] : null
  }
  sua('idNhanViens')
}

function toggleChonNv(id) {
  const idx = f.idNhanViens.indexOf(id)
  if (idx >= 0) {
    f.idNhanViens.splice(idx, 1)
    if (f.idThuNgan === id) {
      f.idThuNgan = f.idNhanViens.length > 0 ? f.idNhanViens[0] : null
    }
  } else {
    f.idNhanViens.push(id)
    if (f.idThuNgan === null) {
      f.idThuNgan = id
    }
  }
  sua('idNhanViens')
}

function chonNgay(kieu) {
  const ds = kieu === 'thuong' ? ngayCon.value.filter((d) => !['Thứ 7', 'Chủ nhật'].includes(d.thu)) : ngayCon.value
  f.ngays = ds.map((d) => d.iso)
  sua('ngays')
}

const { loi, sua, batDauGui, datLoiMayChu } = useLoiForm(() => {
  const e = {}
  if (!f.idNhanViens.length) e.idNhanViens = 'Vui lòng chọn ít nhất một nhân viên.'
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
      idThuNgan: f.idThuNgan ? Number(f.idThuNgan) : null,
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
              Nhân viên đi làm <span class="ad-required">*</span>
              <span v-if="f.idNhanViens.length" class="badge bg-primary ms-1">{{ f.idNhanViens.length }}</span>
            </span>
            <span class="small">
              <button type="button" class="xl-lk" @click="chonTatCaNv(true)">Chọn tất cả</button> ·
              <button type="button" class="xl-lk" @click="chonTatCaNv(false)">Bỏ chọn</button>
            </span>
          </div>

          <input
            v-model="timNv"
            type="search"
            class="form-control form-control-sm mb-2"
            placeholder="Tìm theo tên hoặc mã NV…"
            aria-label="Tìm nhân viên"
          />

          <div class="xl-ds" :class="{ 'is-invalid': loi.idNhanViens }" role="group" aria-label="Chọn nhân viên">
            <div
              v-for="n in nvLoc"
              :key="n.id"
              class="xl-muc"
              :class="{ 'is-active': f.idNhanViens.includes(n.id) }"
              @click="toggleChonNv(n.id)"
            >
              <input
                type="checkbox"
                class="form-check-input mt-0 me-2"
                :checked="f.idNhanViens.includes(n.id)"
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

        <!-- Cột phải: Ca, Ngày & Phân công Thu ngân -->
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

          <!-- Phân công Thu ngân giữ két cho ca -->
          <div v-if="nhanViensDaChon.length > 0" class="xl-role-assign mt-3">
            <div class="xl-role-assign-head">
              <span class="fw-semibold text-dark">Chỉ định Thu ngân giữ két:</span>
              <small class="text-muted">Chọn 1 người giữ két cho ca này</small>
            </div>
            <div class="xl-role-options">
              <label class="xl-role-item" :class="{ 'is-selected': f.idThuNgan === null }">
                <input
                  v-model="f.idThuNgan"
                  type="radio"
                  class="form-check-input me-2"
                  :value="null"
                />
                <span class="flex-fill">Chưa chỉ định (tất cả là Bán hàng)</span>
              </label>

              <label
                v-for="nv in nhanViensDaChon"
                :key="nv.id"
                class="xl-role-item"
                :class="{ 'is-selected': f.idThuNgan === nv.id }"
              >
                <input
                  v-model="f.idThuNgan"
                  type="radio"
                  class="form-check-input me-2"
                  :value="nv.id"
                />
                <span class="flex-fill fw-medium">{{ nv.hoTen }}</span>
                <span v-if="f.idThuNgan === nv.id" class="badge bg-success">Thu ngân giữ két</span>
                <span v-else class="badge bg-light text-secondary">Bán hàng</span>
              </label>
            </div>
            <p class="xl-role-hint mt-2 mb-0">
              Nhân viên được chọn sẽ phụ trách mở ca và giữ két tiền; các nhân viên còn lại trong ca là Nhân viên bán hàng.
            </p>
          </div>

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
            Sẽ xếp <b>{{ soO }}</b> ca ({{ f.idNhanViens.length }} nhân viên × {{ f.ngays.length }} ngày). Ô trùng ca sẽ được tự động bỏ qua.
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

/* Card phân công Thu ngân giữ két */
.xl-role-assign {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem;
}
.xl-role-assign-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-size: 0.82rem;
}
.xl-role-options {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-height: 140px;
  overflow-y: auto;
}
.xl-role-item {
  display: flex;
  align-items: center;
  padding: 0.35rem 0.5rem;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  margin: 0;
}
.xl-role-item:hover {
  border-color: #94a3b8;
}
.xl-role-item.is-selected {
  border-color: #059669;
  background: #f0fdf4;
}
.xl-role-hint {
  font-size: 0.74rem;
  color: #64748b;
  line-height: 1.35;
}

.xl-bo-qua {
  max-height: 200px;
  overflow-y: auto;
}
</style>
