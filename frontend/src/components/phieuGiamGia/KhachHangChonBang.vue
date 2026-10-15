<script setup>
// Bảng "Danh sách khách hàng nhận phiếu" của phiếu giảm giá CÁ NHÂN (bố cục theo form mẫu của nhóm đầu, cô đã khen):
// cột Mã KH / Họ và tên / Giới tính / Ngày sinh / Số điện thoại / Email / Tổng đơn / Mua gần nhất, ô tìm kiếm, bộ lọc
// (giới tính, tháng sinh, sắp sinh nhật, số đơn đã mua, ngày mua gần nhất), chọn tất cả / bỏ chọn theo kết quả đang lọc.
// "Tổng đơn" = số đơn ĐÃ HOÀN THÀNH của khách; "Mua gần nhất" = ngày giờ của đơn hoàn thành gần nhất.
// v-model: mảng id khách đã chọn. daDung: id các khách đã dùng phiếu (khóa, không bỏ chọn được).
import { computed, reactive, watch, ref } from 'vue'
import Select2 from '../common/Select2.vue'
import DateInput from '../common/DateInput.vue'
import { dinhDangNgayGio } from '../../utils/ngayGio'
import {
  LOC_GIOI_TINH,
  LOC_SINH_NHAT,
  LOC_SO_DON,
  LOC_THANG_SINH,
  dangLoc,
  ghiChuSinhNhat,
  locKhach,
  locMacDinh,
  ngaySinhVn,
  tenGioiTinh,
} from '../../utils/khachChonPhieu'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  khachHangs: { type: Array, default: () => [] },
  daDung: { type: Object, default: () => new Set() }, // Set id khách đã dùng phiếu
  dangTai: { type: Boolean, default: false },
  loi: { type: String, default: '' },
  invalid: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const loc = reactive(locMacDinh())
const homNay = ref(new Date())
const dsLoc = computed(() => locKhach(props.khachHangs, loc, homNay.value))
const dangLocKhong = computed(() => dangLoc(loc))
const daChon = computed(() => new Set(props.modelValue))
const biKhoa = (id) => props.daDung?.has?.(id) ?? false

const chonDuoc = computed(() => dsLoc.value.filter((k) => !biKhoa(k.id)))
const soChonTrongLoc = computed(() => chonDuoc.value.filter((k) => daChon.value.has(k.id)).length)
const tatCaDaChon = computed(() => chonDuoc.value.length > 0 && soChonTrongLoc.value === chonDuoc.value.length)
const motPhanDaChon = computed(() => soChonTrongLoc.value > 0 && !tatCaDaChon.value)

function datChon(ids) {
  emit('update:modelValue', ids)
}
function bat(id) {
  if (biKhoa(id)) return
  datChon(daChon.value.has(id) ? props.modelValue.filter((x) => x !== id) : [...props.modelValue, id])
}
function chonTatCaTrongLoc() {
  const them = chonDuoc.value.map((k) => k.id).filter((id) => !daChon.value.has(id))
  if (them.length) datChon([...props.modelValue, ...them])
}
function boChonTrongLoc() {
  const trongLoc = new Set(chonDuoc.value.map((k) => k.id))
  datChon(props.modelValue.filter((id) => !trongLoc.has(id)))
}
function batTatCa() {
  if (tatCaDaChon.value) boChonTrongLoc()
  else chonTatCaTrongLoc()
}
function datLaiLoc() {
  Object.assign(loc, locMacDinh())
}

// Ô đầu bảng ở trạng thái "một phần" (indeterminate) khi chỉ chọn một số khách trong kết quả lọc
const oTatCa = ref(null)
watch([motPhanDaChon, oTatCa], () => {
  if (oTatCa.value) oTatCa.value.indeterminate = motPhanDaChon.value
}, { immediate: true, flush: 'post' })

const GIOI_TINH_CLS = { Nam: 'ad-pill-blue', 'Nữ': 'ad-pill-pink' }
</script>

<template>
  <div class="pgg-khach" :class="{ 'is-invalid': invalid }" data-testid="pgg-bang-khach">
    <div class="pgg-khach-head">
      <h3 class="pgg-khach-title">Danh sách khách hàng nhận phiếu</h3>
      <span class="pgg-badge" data-testid="pgg-da-chon" role="status">Đã chọn {{ modelValue.length }} khách hàng</span>
    </div>

    <div class="pgg-khach-loc">
      <div class="pgg-loc-tim">
        <div class="ad-affix">
          <i class="bi bi-search" aria-hidden="true"></i>
          <input
            id="pgg-khach-tim"
            v-model.trim="loc.tuKhoa"
            type="text"
            class="form-control ad-control"
            placeholder="Tìm theo mã, tên, SĐT, email…"
            autocomplete="off"
            aria-label="Tìm khách hàng"
          />
        </div>
      </div>
      <Select2 id="pgg-khach-gioi-tinh" v-model="loc.gioiTinh" :options="LOC_GIOI_TINH" class="ad-control" aria-label="Lọc theo giới tính" />
      <Select2 id="pgg-khach-thang-sinh" v-model="loc.thangSinh" :options="LOC_THANG_SINH" class="ad-control" aria-label="Lọc theo tháng sinh" />
      <Select2 id="pgg-khach-sinh-nhat" v-model="loc.sinhNhat" :options="LOC_SINH_NHAT" class="ad-control" aria-label="Lọc theo sắp sinh nhật" />
      <Select2 id="pgg-khach-so-don" v-model="loc.soDon" :options="LOC_SO_DON" class="ad-control" aria-label="Lọc theo số đơn đã mua" />
      <div class="pgg-loc-ngay">
        <label class="pgg-loc-nhan" for="pgg-khach-mua-tu">Mua gần nhất từ</label>
        <DateInput id="pgg-khach-mua-tu" v-model="loc.muaTu" :max="loc.muaDen || undefined" class="form-control ad-control" />
      </div>
      <div class="pgg-loc-ngay">
        <label class="pgg-loc-nhan" for="pgg-khach-mua-den">đến ngày</label>
        <DateInput id="pgg-khach-mua-den" v-model="loc.muaDen" :min="loc.muaTu || undefined" class="form-control ad-control" />
      </div>
    </div>

    <div class="pgg-khach-tools">
      <span class="pgg-khach-dem" data-testid="pgg-khach-dem">Hiển thị {{ dsLoc.length }} / {{ khachHangs.length }} khách hàng</span>
      <span class="pgg-khach-nut">
        <button type="button" class="ad-btn ad-btn-sm" data-testid="pgg-chon-tat-ca" @click="chonTatCaTrongLoc">Chọn tất cả kết quả lọc</button>
        <button type="button" class="ad-btn ad-btn-sm" data-testid="pgg-bo-chon" @click="boChonTrongLoc">Bỏ chọn kết quả lọc</button>
        <button v-if="dangLocKhong" type="button" class="ad-btn ad-btn-sm" data-testid="pgg-dat-lai-loc" @click="datLaiLoc">
          <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại lọc
        </button>
      </span>
    </div>

    <div class="pgg-khach-cuon">
      <table class="ad-table pgg-khach-bang">
        <thead>
          <tr>
            <th class="pgg-c-chon">
              <input ref="oTatCa" type="checkbox" :checked="tatCaDaChon" :disabled="!chonDuoc.length" aria-label="Chọn tất cả khách hàng đang hiển thị" data-testid="pgg-chon-dau-bang" @change="batTatCa" />
            </th>
            <th>Mã KH</th>
            <th>Họ và tên</th>
            <th>Giới tính</th>
            <th>Ngày sinh</th>
            <th>Số điện thoại</th>
            <th>Email</th>
            <th class="pgg-c-so">Tổng đơn</th>
            <th>Mua gần nhất</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="dangTai"><td colspan="9" class="pgg-khach-ghi">Đang tải danh sách khách hàng…</td></tr>
          <tr v-else-if="loi"><td colspan="9" class="pgg-khach-ghi text-danger" role="alert">{{ loi }}</td></tr>
          <tr v-else-if="!khachHangs.length"><td colspan="9" class="pgg-khach-ghi">Chưa có khách hàng nào đang hoạt động.</td></tr>
          <tr v-else-if="!dsLoc.length"><td colspan="9" class="pgg-khach-ghi">Không có khách hàng nào phù hợp bộ lọc.</td></tr>
          <template v-else>
            <tr
              v-for="k in dsLoc"
              :key="k.id"
              class="pgg-khach-dong"
              :class="{ 'is-khoa': biKhoa(k.id), 'is-chon': daChon.has(k.id) }"
              data-testid="pgg-khach-dong"
              @click="bat(k.id)"
            >
              <td class="pgg-c-chon" @click.stop>
                <input
                  type="checkbox"
                  :checked="daChon.has(k.id)"
                  :disabled="biKhoa(k.id)"
                  :aria-label="`Chọn khách hàng ${k.hoTen}`"
                  @change="bat(k.id)"
                />
              </td>
              <td><span class="ad-code ad-nowrap">{{ k.ma }}</span></td>
              <td class="pgg-c-ten">
                {{ k.hoTen }}
                <span v-if="biKhoa(k.id)" class="ad-pill ad-pill-gray">Đã dùng</span>
              </td>
              <td>
                <span v-if="tenGioiTinh(k.gioiTinh)" class="ad-pill" :class="GIOI_TINH_CLS[tenGioiTinh(k.gioiTinh)]">{{ tenGioiTinh(k.gioiTinh) }}</span>
                <span v-else class="pgg-trong">---</span>
              </td>
              <td class="ad-nowrap">
                <template v-if="ngaySinhVn(k.ngaySinh)">
                  {{ ngaySinhVn(k.ngaySinh) }}
                  <span v-if="ghiChuSinhNhat(k.ngaySinh, homNay)" class="pgg-sinh-nhat" data-testid="pgg-sinh-nhat"><i class="bi bi-cake2" aria-hidden="true"></i> {{ ghiChuSinhNhat(k.ngaySinh, homNay) }}</span>
                </template>
                <span v-else class="pgg-trong">---</span>
              </td>
              <td class="ad-nowrap">{{ k.soDienThoai || '' }}</td>
              <td class="pgg-c-mail">
                <span v-if="String(k.email ?? '').trim()" :title="k.email">{{ k.email }}</span>
                <span v-else class="ad-pill ad-pill-amber">Chưa có email</span>
              </td>
              <td class="pgg-c-so ad-nowrap" data-testid="pgg-tong-don">{{ k.tongDon ?? 0 }} đơn</td>
              <td class="ad-nowrap" data-testid="pgg-mua-gan-nhat">
                <template v-if="k.muaGanNhat">{{ dinhDangNgayGio(k.muaGanNhat) }}</template>
                <span v-else class="pgg-trong">Chưa mua</span>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.pgg-khach {
  border: 1px solid #dfe5ec;
  border-radius: 14px;
  background: #fff;
  padding: 1rem;
}
.pgg-khach.is-invalid {
  border-color: var(--fs-danger);
}
.pgg-khach-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
}
.pgg-khach-title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 600;
  color: var(--fs-text);
}
.pgg-badge {
  display: inline-block;
  padding: 0.3rem 0.8rem;
  border-radius: 999px;
  background: var(--fs-navy);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
}
.pgg-khach-loc {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 0.6rem;
  align-items: end;
}
.pgg-loc-tim {
  grid-column: 1 / -1;
}
.pgg-loc-nhan {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--fs-text-2);
}
.pgg-khach-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin: 0.8rem 0 0.5rem;
}
.pgg-khach-dem {
  font-size: 0.8rem;
  color: var(--fs-muted);
}
.pgg-khach-nut {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.pgg-khach-cuon {
  max-height: 340px;
  overflow: auto;
  border: 1px solid var(--fs-line-soft);
  border-radius: 12px;
}
.pgg-khach-bang {
  min-width: 860px;
  font-size: 0.85rem;
}
.pgg-khach-bang thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 0.65rem 0.6rem;
}
.pgg-khach-bang tbody td {
  padding: 0.55rem 0.6rem;
}
.pgg-c-chon {
  width: 42px;
  text-align: center;
}
.pgg-c-so {
  text-align: right;
}
.pgg-c-ten {
  font-weight: 600;
}
.pgg-c-mail {
  max-width: 230px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pgg-khach-dong {
  cursor: pointer;
}
.pgg-khach-dong.is-chon > td {
  background: var(--fs-primary-soft);
}
.pgg-khach-dong.is-khoa {
  cursor: not-allowed;
  opacity: 0.72;
}
.pgg-khach-ghi {
  padding: 1.2rem 1rem;
  text-align: center;
  color: var(--fs-text-2);
}
.pgg-trong {
  color: var(--fs-muted);
}
.pgg-sinh-nhat {
  display: block;
  font-size: 0.72rem;
  color: #b4541a;
}
</style>
