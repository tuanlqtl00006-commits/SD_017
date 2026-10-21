<script setup>
// Danh sách ca làm việc (Ca sáng / chiều / tối...) + thêm / sửa / bật tắt ca. Ca ngừng dùng không xếp lịch mới được, lịch cũ vẫn giữ.
import { reactive, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import StatusSwitch from '../common/StatusSwitch.vue'
import { lichLamViecService } from '../../services/lichLamViecService'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { mauCa } from '../../utils/lichTuan'

const props = defineProps({ cas: { type: Array, default: () => [] } })
const emit = defineEmits(['doi'])
const toast = useToast()

const moForm = ref(false)
const dangLuu = ref(false)
const f = reactive({ id: null, tenCa: '', gioBatDau: '', gioKetThuc: '' })

function thoiLuong(c) {
  const [h1, m1] = c.gioBatDau.split(':').map(Number)
  const [h2, m2] = c.gioKetThuc.split(':').map(Number)
  const phut = h2 * 60 + m2 - (h1 * 60 + m1)
  return `${Math.floor(phut / 60)} giờ${phut % 60 ? ` ${phut % 60} phút` : ''}`
}

const { loi, sua, cham, batDauGui, datLoiMayChu, datLai, focusLoiDau } = useLoiForm(() => {
  if (!moForm.value) return {}
  const e = {}
  const ten = f.tenCa.trim()
  if (!ten) e.tenCa = 'Vui lòng nhập tên ca.'
  else if (ten.length > 100) e.tenCa = 'Tên ca tối đa 100 ký tự.'
  if (!f.gioBatDau) e.gioBatDau = 'Vui lòng nhập giờ bắt đầu.'
  if (!f.gioKetThuc) e.gioKetThuc = 'Vui lòng nhập giờ kết thúc.'
  else if (f.gioBatDau && f.gioKetThuc <= f.gioBatDau) e.gioKetThuc = 'Giờ kết thúc phải sau giờ bắt đầu (không có ca qua đêm).'
  return e
})

function moThem() {
  Object.assign(f, { id: null, tenCa: '', gioBatDau: '', gioKetThuc: '' })
  datLai()
  moForm.value = true
}
function moSua(c) {
  Object.assign(f, { id: c.id, tenCa: c.tenCa, gioBatDau: c.gioBatDau, gioKetThuc: c.gioKetThuc })
  datLai()
  moForm.value = true
}

async function luu() {
  if (dangLuu.value) return
  if (!batDauGui()) {
    focusLoiDau('#ca-form')
    return
  }
  dangLuu.value = true
  const body = { tenCa: f.tenCa.trim(), gioBatDau: f.gioBatDau, gioKetThuc: f.gioKetThuc }
  try {
    if (f.id) await lichLamViecService.suaCa(f.id, body)
    else await lichLamViecService.themCa(body)
    toast.success(f.id ? 'Đã cập nhật ca.' : 'Đã thêm ca mới.')
    moForm.value = false
    emit('doi')
  } catch (e) {
    if (datLoiMayChu(e)) focusLoiDau('#ca-form')
    else toast.error(e.message || 'Không lưu được ca.')
  } finally {
    dangLuu.value = false
  }
}

async function doiTrangThai(c) {
  try {
    const kq = await lichLamViecService.doiTrangThaiCa(c.id)
    toast.success(kq.hoatDong ? `Đã bật ${kq.tenCa}.` : `Đã ngừng dùng ${kq.tenCa}.`)
    emit('doi')
  } catch (e) {
    toast.error(e.message || 'Không đổi được trạng thái.')
  }
}
</script>

<template>
  <section class="ad-card">
    <header class="ad-list-head">
      <div class="ad-card-head">
        <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-clock"></i></span>
        <h2 class="ad-card-title">Ca làm việc</h2>
      </div>
      <button type="button" class="ad-btn ad-btn-primary" @click="moThem"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm ca</button>
    </header>
    <div class="ad-table-wrap">
      <div class="ad-table-scroll">
        <table class="ad-table">
          <thead>
            <tr>
              <th class="ad-col-stt">STT</th>
              <th>Mã ca</th>
              <th>Tên ca</th>
              <th>Giờ làm</th>
              <th>Thời lượng</th>
              <th>Trạng thái</th>
              <th class="ad-col-actions">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!props.cas.length">
              <td colspan="7"><div class="ad-empty">Chưa có ca làm việc nào.</div></td>
            </tr>
            <tr v-for="(c, i) in props.cas" :key="c.id">
              <td class="ad-col-stt">{{ i + 1 }}</td>
              <td><span class="ad-code">{{ c.maCa }}</span></td>
              <td><span class="ll-chip" :style="mauCa(i)">{{ c.tenCa }}</span></td>
              <td>{{ c.gioBatDau }} - {{ c.gioKetThuc }}</td>
              <td>{{ thoiLuong(c) }}</td>
              <td>
                <span class="ad-pill" :class="c.hoatDong ? 'ad-pill-green' : 'ad-pill-gray'">{{ c.hoatDong ? 'Đang dùng' : 'Ngừng dùng' }}</span>
              </td>
              <td class="ad-col-actions">
                <div class="ad-actions">
                  <StatusSwitch :on="c.hoatDong" :label="c.hoatDong ? `Ngừng dùng ${c.tenCa}` : `Bật ${c.tenCa}`" @toggle="doiTrangThai(c)" />
                  <button type="button" class="ad-icon-btn" title="Sửa ca" :aria-label="`Sửa ${c.tenCa}`" @click="moSua(c)">
                    <i class="bi bi-pencil-square" aria-hidden="true"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <BaseModal v-if="moForm" :title="f.id ? 'Sửa ca làm việc' : 'Thêm ca làm việc'" size="sm" static-backdrop @close="moForm = false">
    <form id="ca-form" novalidate @submit.prevent="luu">
      <label class="ad-label" for="ca-ten">Tên ca <span class="ad-required">*</span></label>
      <input id="ca-ten" v-model="f.tenCa" type="text" class="form-control ad-control" :class="{ 'is-invalid': loi.tenCa }" maxlength="100" placeholder="vd: Ca hành chính" @input="sua('tenCa')" @blur="cham('tenCa')" />
      <p v-if="loi.tenCa" class="ad-error">{{ loi.tenCa }}</p>
      <div class="row g-2 mt-1">
        <div class="col-6">
          <label class="ad-label" for="ca-bd">Bắt đầu <span class="ad-required">*</span></label>
          <input id="ca-bd" v-model="f.gioBatDau" type="time" class="form-control ad-control" :class="{ 'is-invalid': loi.gioBatDau }" @input="sua('gioBatDau')" />
          <p v-if="loi.gioBatDau" class="ad-error">{{ loi.gioBatDau }}</p>
        </div>
        <div class="col-6">
          <label class="ad-label" for="ca-kt">Kết thúc <span class="ad-required">*</span></label>
          <input id="ca-kt" v-model="f.gioKetThuc" type="time" class="form-control ad-control" :class="{ 'is-invalid': loi.gioKetThuc }" @input="sua('gioKetThuc')" />
          <p v-if="loi.gioKetThuc" class="ad-error">{{ loi.gioKetThuc }}</p>
        </div>
      </div>
    </form>
    <template #footer>
      <button type="button" class="ad-btn" @click="moForm = false">Hủy</button>
      <button type="submit" form="ca-form" class="ad-btn ad-btn-primary" :disabled="dangLuu">Lưu</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.ll-chip {
  display: inline-block;
  border: 1px solid;
  border-radius: 999px;
  padding: 0.1rem 0.65rem;
  font-weight: 600;
  font-size: 0.85rem;
}
</style>
