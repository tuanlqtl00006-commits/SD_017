<script setup>
// Lịch làm của nhân viên đang đăng nhập theo tuần (chỉ xem). Ca đang diễn ra được đánh dấu; nhận ca ở nút "Ca làm việc" trên header.
import { computed, onMounted, ref } from 'vue'
import { lichLamViecService } from '../../services/lichLamViecService'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { todayIso } from '../../utils/format'
import { congNgayIso, ngayTrongTuan, nhanTuan, thu2CuaTuan } from '../../utils/lichTuan'

const toast = useToast()
const auth = useAuth()
const homNay = todayIso()
const thu2 = ref(thu2CuaTuan(homNay))
const ngays = computed(() => ngayTrongTuan(thu2.value))
const lich = ref([])
const dangTai = ref(true)

const theoNgay = computed(() => {
  const kq = {}
  for (const l of lich.value) (kq[l.ngayLam] ||= []).push(l)
  for (const k of Object.keys(kq)) kq[k].sort((a, b) => a.gioBatDau.localeCompare(b.gioBatDau))
  return kq
})
const tongGio = computed(() => {
  let phut = 0
  for (const l of lich.value) {
    const [h1, m1] = l.gioBatDau.split(':').map(Number)
    const [h2, m2] = l.gioKetThuc.split(':').map(Number)
    phut += h2 * 60 + m2 - (h1 * 60 + m1)
  }
  return Math.round((phut / 60) * 10) / 10
})

function dangDienRa(l) {
  if (l.ngayLam !== homNay) return false
  const now = new Date()
  const hm = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  return l.gioBatDau <= hm && hm < l.gioKetThuc
}

async function tai() {
  dangTai.value = true
  try {
    lich.value = await lichLamViecService.lichCuaToi(thu2.value, congNgayIso(thu2.value, 6))
  } catch (e) {
    toast.error(e.message || 'Không tải được lịch làm.')
  } finally {
    dangTai.value = false
  }
}
onMounted(tai)
function doiTuan(n) {
  thu2.value = n === 0 ? thu2CuaTuan(todayIso()) : congNgayIso(thu2.value, n * 7)
  tai()
}
</script>

<template>
  <div class="ad-page">
    <section class="ad-card">
      <header class="ad-list-head">
        <div class="ll-tuan">
          <button type="button" class="ad-icon-btn" aria-label="Tuần trước" @click="doiTuan(-1)"><i class="bi bi-chevron-left"></i></button>
          <h2 class="ad-card-title m-0">{{ nhanTuan(thu2) }}</h2>
          <button type="button" class="ad-icon-btn" aria-label="Tuần sau" @click="doiTuan(1)"><i class="bi bi-chevron-right"></i></button>
          <button type="button" class="ad-btn ad-btn-sm" @click="doiTuan(0)">Tuần này</button>
        </div>
        <span class="ad-count">{{ auth.trangThai.nguoiDung?.hoTen }} · {{ lich.length }} ca · {{ tongGio }} giờ</span>
      </header>
      <div v-if="dangTai" class="ad-empty">Đang tải lịch…</div>
      <div v-else class="lt-luoi">
        <article v-for="d in ngays" :key="d.iso" class="lt-ngay" :class="{ 'is-hom-nay': d.iso === homNay, 'is-qua': d.iso < homNay }">
          <header class="lt-ngay-dau">
            <b>{{ d.thu }}</b>
            <span>{{ d.ngay }}</span>
            <span v-if="d.iso === homNay" class="ad-pill ad-pill-blue">Hôm nay</span>
          </header>
          <div v-if="!(theoNgay[d.iso] || []).length" class="lt-nghi">Nghỉ</div>
          <div v-for="l in theoNgay[d.iso] || []" :key="l.id" class="lt-ca" :class="{ 'is-dang': dangDienRa(l) }">
            <div class="fw-semibold">{{ l.tenCa }}</div>
            <div>{{ l.gioBatDau }} - {{ l.gioKetThuc }}</div>
            <small v-if="l.ghiChu" class="text-muted">{{ l.ghiChu }}</small>
            <span v-if="dangDienRa(l)" class="ad-pill ad-pill-green mt-1">Đang diễn ra</span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.ll-tuan {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.lt-luoi {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.75rem;
  padding: 0 1.25rem 1.25rem;
}
.lt-ngay {
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 14px;
  padding: 0.75rem;
  min-height: 160px;
  background: #fff;
}
.lt-ngay.is-hom-nay {
  border-color: var(--fs-primary, #0977ec);
  box-shadow: 0 0 0 3px var(--fs-primary-soft, #e7f1fd);
}
.lt-ngay.is-qua {
  background: #fafbfc;
  opacity: 0.8;
}
.lt-ngay-dau {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  align-items: center;
  margin-bottom: 0.6rem;
}
.lt-nghi {
  color: var(--fs-muted, #7b8794);
  font-size: 0.9rem;
}
.lt-ca {
  border-radius: 10px;
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-primary-dark, #075fc0);
  padding: 0.5rem 0.6rem;
  margin-bottom: 0.5rem;
  display: flex;
  flex-direction: column;
}
.lt-ca.is-dang {
  background: #e8f6ee;
  color: #146c43;
}
@media (max-width: 1100px) {
  .lt-luoi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
