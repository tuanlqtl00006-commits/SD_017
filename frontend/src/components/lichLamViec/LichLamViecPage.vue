<script setup>
// Xếp lịch làm việc theo tuần (Quản lý): hàng = nhân viên đang hoạt động, cột = thứ 2 -> chủ nhật, ô = các ca của người đó ngày đó.
// Bấm "+" ở ô để xếp ca cho đúng người / ngày; "Xếp lịch nhanh" cho nhiều người nhiều ngày; "Sao chép tuần trước" để lặp lịch.
// Ngày đã qua chỉ xem (không thêm / xóa). Hàng cuối đếm số người mỗi ca trong ngày để thấy ngay ca nào đang thiếu người.
import { computed, onMounted, ref } from 'vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import CaLamViecTab from './CaLamViecTab.vue'
import XepLichModal from './XepLichModal.vue'
import { lichLamViecService } from '../../services/lichLamViecService'
import { useToast } from '../../composables/useToast'
import { formatDate, todayIso } from '../../utils/format'
import { congNgayIso, demTheoCa, gomLich, mauCa, ngayTrongTuan, nhanTuan, thu2CuaTuan } from '../../utils/lichTuan'

const toast = useToast()
const tab = ref('lich')
const homNay = ref(todayIso())
const thu2 = ref(thu2CuaTuan(homNay.value))
const ngays = computed(() => ngayTrongTuan(thu2.value))
const cas = ref([])
const nhanViens = ref([])
const lich = ref([])
const dangTai = ref(true)

const theoO = computed(() => gomLich(lich.value))
const dem = computed(() => demTheoCa(lich.value))
const viTriCa = computed(() => Object.fromEntries(cas.value.map((c, i) => [c.id, i])))
const caDangDung = computed(() => cas.value.filter((c) => c.hoatDong))

async function taiCa() {
  cas.value = await lichLamViecService.dsCa()
}
async function taiLich() {
  dangTai.value = true
  try {
    lich.value = await lichLamViecService.lich(thu2.value, congNgayIso(thu2.value, 6))
  } catch (e) {
    toast.error(e.message || 'Không tải được lịch làm việc.')
  } finally {
    dangTai.value = false
  }
}
onMounted(async () => {
  try {
    const [, nv] = await Promise.all([taiCa(), lichLamViecService.nhanVien()])
    nhanViens.value = nv
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu.')
  }
  await taiLich()
})

function doiTuan(soTuan) {
  thu2.value = soTuan === 0 ? thu2CuaTuan(todayIso()) : congNgayIso(thu2.value, soTuan * 7)
  taiLich()
}

/* ----- Xếp lịch ----- */
const moXep = ref(false)
const chonSan = ref(null)
function moXepO(nv, ngay) {
  chonSan.value = { idNhanVien: nv.id, ngay }
  moXep.value = true
}
function moXepNhanh() {
  chonSan.value = null
  moXep.value = true
}

/* ----- Xóa ----- */
const xoa = ref(null)
const dangXoa = ref(false)
async function xacNhanXoa() {
  dangXoa.value = true
  try {
    await lichLamViecService.xoaLich(xoa.value.id)
    toast.success('Đã bỏ ca khỏi lịch.')
    xoa.value = null
    await taiLich()
  } catch (e) {
    toast.error(e.message || 'Không xóa được.')
  } finally {
    dangXoa.value = false
  }
}

/* ----- Sao chép tuần trước ----- */
const moSaoChep = ref(false)
const dangSaoChep = ref(false)
async function saoChep() {
  dangSaoChep.value = true
  try {
    const kq = await lichLamViecService.saoChepTuan(congNgayIso(thu2.value, -7), thu2.value)
    moSaoChep.value = false
    toast.success(`Đã sao chép ${kq.daThem} ca${kq.boQua.length ? `, bỏ qua ${kq.boQua.length} ca (ngày đã qua / trùng / chồng giờ)` : ''}.`)
    await taiLich()
  } catch (e) {
    moSaoChep.value = false
    toast.error(e.message || 'Không sao chép được.')
  } finally {
    dangSaoChep.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <div class="ad-segment ll-tab" role="tablist" aria-label="Lịch làm việc">
      <button type="button" class="ad-segment-item" :class="{ active: tab === 'lich' }" role="tab" :aria-selected="tab === 'lich'" @click="tab = 'lich'">
        <i class="bi bi-calendar-week" aria-hidden="true"></i> Lịch tuần
      </button>
      <button type="button" class="ad-segment-item" :class="{ active: tab === 'ca' }" role="tab" :aria-selected="tab === 'ca'" @click="tab = 'ca'">
        <i class="bi bi-clock" aria-hidden="true"></i> Ca làm việc
      </button>
    </div>

    <CaLamViecTab v-if="tab === 'ca'" :cas="cas" @doi="taiCa" />

    <section v-else class="ad-card">
      <header class="ad-list-head ll-dau">
        <div class="ll-tuan">
          <button type="button" class="ad-icon-btn" aria-label="Tuần trước" title="Tuần trước" @click="doiTuan(-1)"><i class="bi bi-chevron-left"></i></button>
          <h2 class="ad-card-title m-0">{{ nhanTuan(thu2) }}</h2>
          <button type="button" class="ad-icon-btn" aria-label="Tuần sau" title="Tuần sau" @click="doiTuan(1)"><i class="bi bi-chevron-right"></i></button>
          <button type="button" class="ad-btn ad-btn-sm" @click="doiTuan(0)">Tuần này</button>
        </div>
        <div class="d-flex flex-wrap gap-2">
          <button type="button" class="ad-btn" @click="moSaoChep = true"><i class="bi bi-files" aria-hidden="true"></i> Sao chép tuần trước</button>
          <button type="button" class="ad-btn ad-btn-primary" :disabled="!caDangDung.length" @click="moXepNhanh"><i class="bi bi-calendar-plus" aria-hidden="true"></i> Xếp lịch nhanh</button>
        </div>
      </header>

      <div class="ll-chu-giai">
        <span v-for="c in cas" :key="c.id" class="ll-chip" :class="{ 'is-tat': !c.hoatDong }" :style="mauCa(viTriCa[c.id])">{{ c.tenCa }} {{ c.gioBatDau }}-{{ c.gioKetThuc }}</span>
      </div>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table ll-bang">
            <thead>
              <tr>
                <th class="ll-cot-nv">Nhân viên</th>
                <th v-for="d in ngays" :key="d.iso" :class="{ 'is-hom-nay': d.iso === homNay }">
                  <div>{{ d.thu }}</div>
                  <small>{{ d.ngay }}</small>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="dangTai">
                <td :colspan="8"><div class="ad-empty">Đang tải lịch…</div></td>
              </tr>
              <tr v-else-if="!nhanViens.length">
                <td :colspan="8"><div class="ad-empty">Chưa có nhân viên đang hoạt động.</div></td>
              </tr>
              <template v-else>
                <tr v-for="nv in nhanViens" :key="nv.id">
                  <td class="ll-cot-nv">
                    <div class="fw-semibold">{{ nv.hoTen }}</div>
                    <small class="text-muted">{{ nv.maNhanVien }} · {{ nv.vaiTro }}</small>
                  </td>
                  <td v-for="d in ngays" :key="d.iso" class="ll-o" :class="{ 'is-hom-nay': d.iso === homNay, 'is-qua': d.iso < homNay }">
                    <div v-for="l in theoO[`${nv.id}|${d.iso}`] || []" :key="l.id" class="ll-chip ll-chip-o" :style="mauCa(viTriCa[l.idCa])" :title="l.ghiChu || `${l.tenCa} ${l.gioBatDau}-${l.gioKetThuc}`">
                      <span>{{ l.tenCa }}</span>
                      <button v-if="!l.daQua" type="button" class="ll-bo" :aria-label="`Bỏ ${l.tenCa} của ${nv.hoTen} ngày ${formatDate(d.iso)}`" @click="xoa = l">
                        <i class="bi bi-x" aria-hidden="true"></i>
                      </button>
                    </div>
                    <button v-if="d.iso >= homNay && caDangDung.length" type="button" class="ll-them" :aria-label="`Xếp ca cho ${nv.hoTen} ngày ${formatDate(d.iso)}`" @click="moXepO(nv, d.iso)">
                      <i class="bi bi-plus" aria-hidden="true"></i>
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
            <tfoot v-if="!dangTai && nhanViens.length">
              <tr>
                <th class="ll-cot-nv">Số người / ca</th>
                <td v-for="d in ngays" :key="d.iso" class="ll-dem" :class="{ 'is-hom-nay': d.iso === homNay }">
                  <div v-for="c in caDangDung" :key="c.id" :class="{ 'is-thieu': !(dem[d.iso] || {})[c.id] }">
                    {{ c.tenCa }}: <b>{{ (dem[d.iso] || {})[c.id] || 0 }}</b>
                  </div>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </section>

    <XepLichModal v-if="moXep" :nhan-viens="nhanViens" :cas="cas" :ngays="ngays" :chon-san="chonSan" @close="moXep = false" @xong="taiLich" />
    <ConfirmDialog
      v-if="xoa"
      title="Bỏ ca khỏi lịch"
      :message="`Bỏ ${xoa.tenCa} (${xoa.gioBatDau} - ${xoa.gioKetThuc}) của ${xoa.hoTen} ngày ${formatDate(xoa.ngayLam)}?`"
      confirm-text="Bỏ ca"
      variant="danger"
      :loading="dangXoa"
      @confirm="xacNhanXoa"
      @cancel="xoa = null"
    />
    <ConfirmDialog
      v-if="moSaoChep"
      title="Sao chép lịch tuần trước"
      :message="`Chép toàn bộ lịch của ${nhanTuan(congNgayIso(thu2, -7)).toLowerCase()} sang ${nhanTuan(thu2).toLowerCase()}? Ô của ngày đã qua, ca trùng hoặc chồng giờ sẽ được bỏ qua.`"
      confirm-text="Sao chép"
      :loading="dangSaoChep"
      @confirm="saoChep"
      @cancel="moSaoChep = false"
    />
  </div>
</template>

<style scoped>
.ll-tab {
  margin-bottom: 1rem;
  max-width: 380px;
}
.ll-tab .ad-segment-item {
  border: 0;
  background: transparent;
}
.ll-tab .ad-segment-item.active {
  background: #fff;
}
.ll-dau {
  flex-wrap: wrap;
  gap: 0.75rem;
}
.ll-tuan {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.ll-chu-giai {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  padding: 0 1.25rem 0.75rem;
}
.ll-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  border: 1px solid;
  border-radius: 999px;
  padding: 0.1rem 0.6rem;
  font-weight: 600;
  font-size: 0.82rem;
  white-space: nowrap;
}
.ll-chip.is-tat {
  opacity: 0.45;
  text-decoration: line-through;
}
.ll-bang th,
.ll-bang td {
  vertical-align: top;
  min-width: 118px;
}
.ll-bang th.is-hom-nay,
.ll-bang td.is-hom-nay {
  background: #f1f7fe;
}
.ll-cot-nv {
  min-width: 190px !important;
  position: sticky;
  left: 0;
  background: #fff;
  z-index: 1;
}
.ll-o.is-qua {
  background: #fafbfc;
}
.ll-chip-o {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.3rem;
}
.ll-bo {
  border: 0;
  background: transparent;
  color: inherit;
  padding: 0;
  line-height: 1;
}
.ll-them {
  width: 100%;
  border: 1px dashed #c9d3df;
  background: transparent;
  border-radius: 8px;
  color: var(--fs-muted, #7b8794);
  padding: 0.1rem 0;
  opacity: 0.6;
}
.ll-o:hover .ll-them {
  opacity: 1;
  border-color: var(--fs-primary, #0977ec);
  color: var(--fs-primary, #0977ec);
}
.ll-dem {
  font-size: 0.8rem;
  color: var(--fs-text-2, #4b5563);
}
.ll-dem .is-thieu {
  color: #b42318;
}
</style>
