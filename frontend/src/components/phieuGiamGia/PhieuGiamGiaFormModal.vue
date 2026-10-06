<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { HINH_THUC, LOAI_GIAM, taoMaPhieu } from '../../constants/phieuGiamGia'
import { phieuGiamGiaService } from '../../services/phieuGiamGiaService'
import { includesText } from '../../utils/text'
import { todayIso } from '../../utils/format'

const props = defineProps({
  item: { type: Object, default: null },
  all: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)
const daCoNguoiDung = computed(
  () => (props.item?.soLuongDaDung ?? 0) > 0 || (props.item?.khachHangs ?? []).some((k) => k.daDung),
)
const khachDaDung = computed(() => new Set((props.item?.khachHangs ?? []).filter((k) => k.daDung).map((k) => k.id)))

const form = reactive(
  props.item
    ? {
        ma: props.item.ma,
        ten: props.item.ten,
        hinhThuc: props.item.hinhThuc,
        loaiGiam: props.item.loaiGiam,
        giaTri: props.item.giaTri,
        giamToiDa: props.item.giamToiDa ?? '',
        donToiThieu: props.item.donToiThieu ?? 0,
        soLuong: props.item.soLuong,
        gioiHanMoiKhach: props.item.gioiHanMoiKhach ?? '',
        ngayBatDau: props.item.ngayBatDau,
        ngayKetThuc: props.item.ngayKetThuc,
        khachHangIds: (props.item.khachHangs ?? []).map((k) => k.id),
      }
    : {
        ma: taoMaPhieu(props.all.map((p) => p.ma)),
        ten: '',
        hinhThuc: 'CONG_KHAI',
        loaiGiam: 'PHAN_TRAM',
        giaTri: '',
        giamToiDa: '',
        donToiThieu: 0,
        soLuong: 1,
        gioiHanMoiKhach: '',
        ngayBatDau: todayIso(),
        ngayKetThuc: '',
        khachHangIds: [],
      },
)

const isPercent = computed(() => form.loaiGiam === 'PHAN_TRAM')
const isCaNhan = computed(() => form.hinhThuc === 'CA_NHAN')


const khachHangAll = ref([])
const khachLoading = ref(false)
const khachError = ref('')
const khachKeyword = ref('')

async function loadKhachHang() {
  if (khachHangAll.value.length || khachLoading.value) return
  khachLoading.value = true
  khachError.value = ''
  try {
    khachHangAll.value = await phieuGiamGiaService.getKhachHang()
  } catch (e) {
    khachError.value = e.message || 'Không tải được danh sách khách hàng.'
  } finally {
    khachLoading.value = false
  }
}

const khachFiltered = computed(() =>
  khachHangAll.value.filter((k) => !khachKeyword.value || includesText(`${k.ma} ${k.hoTen} ${k.soDienThoai ?? ''} ${k.email ?? ''}`, khachKeyword.value)),
)

function toggleKhach(id) {
  if (khachDaDung.value.has(id)) return
  const i = form.khachHangIds.indexOf(id)
  if (i === -1) form.khachHangIds.push(id)
  else form.khachHangIds.splice(i, 1)
}
function chonTatCaDangLoc() {
  for (const k of khachFiltered.value) if (!form.khachHangIds.includes(k.id)) form.khachHangIds.push(k.id)
}
function boChonDangLoc() {
  const giuLai = new Set(khachFiltered.value.filter((k) => khachDaDung.value.has(k.id)).map((k) => k.id))
  const dangLoc = new Set(khachFiltered.value.map((k) => k.id))
  form.khachHangIds = form.khachHangIds.filter((id) => !dangLoc.has(id) || giuLai.has(id))
}

watch(
  () => [isCaNhan.value, form.khachHangIds.length],
  ([caNhan, n]) => {
    if (caNhan) form.soLuong = n
  },
  { immediate: true },
)
watch(
  isCaNhan,
  (v) => {
    if (v) loadKhachHang()
    else if (form.soLuong < 1) form.soLuong = 1
  },
  { immediate: true },
)
const errors = reactive({})
const submitted = ref(false)

watch(
  () => form.loaiGiam,
  (value) => {
    if (value !== 'PHAN_TRAM') form.giamToiDa = ''
  },
)

function regenerateCode() {
  form.ma = taoMaPhieu(props.all.map((p) => p.ma))
}

function validate() {
  const e = {}
  const ma = String(form.ma).trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã phiếu.'
  else if (!/^[A-Z0-9]{4,20}$/.test(ma)) e.ma = 'Mã gồm 4-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  else if (!isEdit.value && props.all.some((p) => p.ma === ma)) e.ma = 'Mã phiếu đã tồn tại.'

  const ten = String(form.ten).trim()
  if (!ten) e.ten = 'Nhập tên phiếu.'
  else if (ten.length > 100) e.ten = 'Tên phiếu tối đa 100 ký tự.'

  const giaTri = Number(form.giaTri)
  if (form.giaTri === '' || Number.isNaN(giaTri)) e.giaTri = 'Nhập giá trị giảm.'
  else if (isPercent.value && (giaTri < 1 || giaTri > 100)) e.giaTri = 'Phần trăm giảm từ 1 đến 100.'
  else if (!isPercent.value && giaTri < 1000) e.giaTri = 'Số tiền giảm tối thiểu 1.000 ₫.'

  if (isPercent.value && form.giamToiDa !== '' && !(Number(form.giamToiDa) > 0)) {
    e.giamToiDa = 'Giảm tối đa phải lớn hơn 0, hoặc để trống nếu không giới hạn.'
  }

  const donToiThieu = Number(form.donToiThieu === '' ? 0 : form.donToiThieu)
  if (Number.isNaN(donToiThieu) || donToiThieu < 0) e.donToiThieu = 'Đơn tối thiểu không được âm.'
  else if (!isPercent.value && !e.giaTri && donToiThieu > 0 && giaTri > donToiThieu) {
    e.donToiThieu = 'Đơn tối thiểu phải lớn hơn hoặc bằng số tiền giảm.'
  }

  if (isCaNhan.value) {
    if (!form.khachHangIds.length) e.khachHang = 'Chọn ít nhất một khách hàng để tặng phiếu.'
  } else {
    const soLuong = Number(form.soLuong)
    const daDung = props.item?.soLuongDaDung ?? 0
    if (form.soLuong === '' || !Number.isInteger(soLuong) || soLuong < 1) e.soLuong = 'Số lượng là số nguyên từ 1 trở lên.'
    else if (soLuong < daDung) e.soLuong = `Số lượng không được nhỏ hơn số phiếu đã dùng (${daDung}).`

    if (form.gioiHanMoiKhach !== '' && form.gioiHanMoiKhach !== null) {
      const gh = Number(form.gioiHanMoiKhach)
      if (!Number.isInteger(gh) || gh < 1) e.gioiHanMoiKhach = 'Giới hạn mỗi khách là số nguyên từ 1 trở lên, hoặc để trống.'
      else if (!e.soLuong && gh > soLuong) e.gioiHanMoiKhach = 'Giới hạn mỗi khách không được lớn hơn số lượng phát hành.'
    }
  }

  if (!form.ngayBatDau) e.ngayBatDau = 'Chọn ngày bắt đầu.'
  if (!form.ngayKetThuc) e.ngayKetThuc = 'Chọn ngày kết thúc.'
  else if (form.ngayBatDau && form.ngayKetThuc < form.ngayBatDau) e.ngayKetThuc = 'Ngày kết thúc phải sau hoặc cùng ngày bắt đầu.'
  else if (!isEdit.value && form.ngayKetThuc < todayIso()) e.ngayKetThuc = 'Ngày kết thúc không được ở quá khứ.'

  return e
}

function refreshErrors() {
  const e = validate()
  Object.keys(errors).forEach((k) => delete errors[k])
  Object.assign(errors, e)
  return e
}

watch(form, () => {
  if (submitted.value) refreshErrors()
})

function submit() {
  submitted.value = true
  const e = refreshErrors()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#pgg-form .is-invalid')?.focus())
    return
  }
  emit('save', {
    ma: String(form.ma).trim().toUpperCase(),
    ten: String(form.ten).trim(),
    hinhThuc: form.hinhThuc,
    loaiGiam: form.loaiGiam,
    giaTri: Number(form.giaTri),
    giamToiDa: isPercent.value && form.giamToiDa !== '' ? Number(form.giamToiDa) : null,
    donToiThieu: Number(form.donToiThieu === '' ? 0 : form.donToiThieu),
    soLuong: Number(form.soLuong),
    gioiHanMoiKhach: !isCaNhan.value && form.gioiHanMoiKhach !== '' && form.gioiHanMoiKhach !== null ? Number(form.gioiHanMoiKhach) : null,
    ngayBatDau: form.ngayBatDau,
    ngayKetThuc: form.ngayKetThuc,
    khachHangIds: isCaNhan.value ? [...form.khachHangIds] : [],
  })
}

onMounted(() => {
  document.getElementById('pgg-ten')?.focus()
})
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa phiếu giảm giá' : 'Tạo phiếu giảm giá'" size="lg" static-backdrop @close="emit('close')">
    <form id="pgg-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-md-5">
          <label class="ad-label" for="pgg-ma">Mã phiếu <span class="ad-required">*</span></label>
          <div class="ad-affix">
            <input
              id="pgg-ma"
              v-model="form.ma"
              type="text"
              class="form-control ad-control text-uppercase"
              :class="{ 'is-invalid': errors.ma, 'has-btn': !isEdit }"
              :readonly="isEdit"
              maxlength="20"
              autocomplete="off"
              :aria-invalid="!!errors.ma"
              aria-describedby="pgg-ma-msg"
            />
            <button
              v-if="!isEdit"
              type="button"
              class="ad-affix-btn"
              title="Tạo mã khác"
              aria-label="Tạo mã khác"
              @click="regenerateCode"
            >
              <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
            </button>
          </div>
          <p v-if="errors.ma" id="pgg-ma-msg" class="ad-error">{{ errors.ma }}</p>
          <p v-else-if="isEdit" id="pgg-ma-msg" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
        </div>

        <div class="col-md-7">
          <label class="ad-label" for="pgg-ten">Tên phiếu <span class="ad-required">*</span></label>
          <input
            id="pgg-ten"
            v-model="form.ten"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ten }"
            maxlength="100"
            placeholder="Ví dụ: Giảm 10% vợt Yonex"
            autocomplete="off"
            :aria-invalid="!!errors.ten"
            aria-describedby="pgg-ten-msg"
          />
          <p v-if="errors.ten" id="pgg-ten-msg" class="ad-error">{{ errors.ten }}</p>
        </div>

        <div class="col-md-6">
          <span id="pgg-hinh-thuc-label" class="ad-label">Hình thức</span>
          <div class="ad-segment" role="radiogroup" aria-labelledby="pgg-hinh-thuc-label">
            <label v-for="(opt, key) in HINH_THUC" :key="key" class="ad-segment-item" :class="{ active: form.hinhThuc === key }">
              <input v-model="form.hinhThuc" type="radio" class="visually-hidden" name="hinhThuc" :value="key" :disabled="isEdit && daCoNguoiDung" />
              <i class="bi" :class="opt.icon" aria-hidden="true"></i> {{ opt.label }}
            </label>
          </div>
          <p v-if="isEdit && daCoNguoiDung" class="ad-hint">Phiếu đã có người dùng nên không đổi được hình thức.</p>
        </div>

        <div class="col-md-6">
          <span id="pgg-loai-giam-label" class="ad-label">Loại giảm</span>
          <div class="ad-segment" role="radiogroup" aria-labelledby="pgg-loai-giam-label">
            <label v-for="(opt, key) in LOAI_GIAM" :key="key" class="ad-segment-item" :class="{ active: form.loaiGiam === key }">
              <input v-model="form.loaiGiam" type="radio" class="visually-hidden" name="loaiGiam" :value="key" />
              {{ opt.label }}
            </label>
          </div>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-gia-tri">Giá trị giảm <span class="ad-required">*</span></label>
          <div class="ad-affix">
            <input
              id="pgg-gia-tri"
              v-model.number="form.giaTri"
              type="number"
              min="0"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.giaTri }"
              :placeholder="isPercent ? 'Từ 1 đến 100' : 'Ví dụ: 50000'"
              :aria-invalid="!!errors.giaTri"
              aria-describedby="pgg-gia-tri-msg"
            />
            <span class="ad-suffix" aria-hidden="true">{{ isPercent ? '%' : '₫' }}</span>
          </div>
          <p v-if="errors.giaTri" id="pgg-gia-tri-msg" class="ad-error">{{ errors.giaTri }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-giam-toi-da">Giảm tối đa</label>
          <div class="ad-affix">
            <input
              id="pgg-giam-toi-da"
              v-model.number="form.giamToiDa"
              type="number"
              min="0"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.giamToiDa }"
              :disabled="!isPercent"
              :placeholder="isPercent ? 'Để trống nếu không giới hạn' : 'Chỉ áp dụng khi giảm theo %'"
              :aria-invalid="!!errors.giamToiDa"
              aria-describedby="pgg-giam-toi-da-msg"
            />
            <span class="ad-suffix" aria-hidden="true">₫</span>
          </div>
          <p v-if="errors.giamToiDa" id="pgg-giam-toi-da-msg" class="ad-error">{{ errors.giamToiDa }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-don-toi-thieu">Đơn tối thiểu</label>
          <div class="ad-affix">
            <input
              id="pgg-don-toi-thieu"
              v-model.number="form.donToiThieu"
              type="number"
              min="0"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.donToiThieu }"
              placeholder="0 nếu không yêu cầu"
              :aria-invalid="!!errors.donToiThieu"
              aria-describedby="pgg-don-toi-thieu-msg"
            />
            <span class="ad-suffix" aria-hidden="true">₫</span>
          </div>
          <p v-if="errors.donToiThieu" id="pgg-don-toi-thieu-msg" class="ad-error">{{ errors.donToiThieu }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-so-luong">Số lượng phát hành <span class="ad-required">*</span></label>
          <input
            id="pgg-so-luong"
            v-model.number="form.soLuong"
            type="number"
            :min="Math.max(1, item?.soLuongDaDung ?? 1)"
            step="1"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.soLuong }"
            :readonly="isCaNhan"
            :aria-invalid="!!errors.soLuong"
            aria-describedby="pgg-so-luong-msg"
          />
          <p v-if="errors.soLuong" id="pgg-so-luong-msg" class="ad-error">{{ errors.soLuong }}</p>
          <p v-else-if="isCaNhan" id="pgg-so-luong-msg" class="ad-hint">Tự tính theo số khách hàng được chọn (mỗi khách 1 phiếu).</p>
        </div>

        <div v-if="!isCaNhan" class="col-md-6">
          <label class="ad-label" for="pgg-gioi-han">Giới hạn mỗi khách</label>
          <input
            id="pgg-gioi-han"
            v-model.number="form.gioiHanMoiKhach"
            type="number"
            min="1"
            step="1"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.gioiHanMoiKhach }"
            placeholder="Để trống nếu không giới hạn"
            :aria-invalid="!!errors.gioiHanMoiKhach"
            aria-describedby="pgg-gioi-han-msg"
          />
          <p v-if="errors.gioiHanMoiKhach" id="pgg-gioi-han-msg" class="ad-error">{{ errors.gioiHanMoiKhach }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-ngay-bd">Ngày bắt đầu <span class="ad-required">*</span></label>
          <input
            id="pgg-ngay-bd"
            v-model="form.ngayBatDau"
            type="date"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ngayBatDau }"
            :aria-invalid="!!errors.ngayBatDau"
            aria-describedby="pgg-ngay-bd-msg"
          />
          <p v-if="errors.ngayBatDau" id="pgg-ngay-bd-msg" class="ad-error">{{ errors.ngayBatDau }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-ngay-kt">Ngày kết thúc <span class="ad-required">*</span></label>
          <input
            id="pgg-ngay-kt"
            v-model="form.ngayKetThuc"
            type="date"
            :min="form.ngayBatDau || undefined"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ngayKetThuc }"
            :aria-invalid="!!errors.ngayKetThuc"
            aria-describedby="pgg-ngay-kt-msg"
          />
          <p v-if="errors.ngayKetThuc" id="pgg-ngay-kt-msg" class="ad-error">{{ errors.ngayKetThuc }}</p>
        </div>

        <div v-if="isCaNhan" class="col-12">
          <span id="pgg-khach-label" class="ad-label">
            Khách hàng được tặng <span class="ad-required">*</span>
            <span class="pgg-count">Đã chọn {{ form.khachHangIds.length }}</span>
          </span>
          <div class="pgg-picker" :class="{ 'is-invalid': errors.khachHang }" role="group" aria-labelledby="pgg-khach-label">
            <div class="pgg-picker-tools">
              <div class="ad-affix pgg-search">
                <i class="bi bi-search" aria-hidden="true"></i>
                <input
                  v-model.trim="khachKeyword"
                  type="text"
                  class="form-control ad-control"
                  placeholder="Tìm theo mã, tên, SĐT, email"
                  autocomplete="off"
                  aria-label="Tìm khách hàng"
                />
              </div>
              <button type="button" class="ad-btn" @click="chonTatCaDangLoc">Chọn tất cả</button>
              <button type="button" class="ad-btn" @click="boChonDangLoc">Bỏ chọn</button>
            </div>
            <div class="pgg-picker-list">
              <p v-if="khachLoading" class="pgg-picker-note">Đang tải danh sách khách hàng…</p>
              <p v-else-if="khachError" class="pgg-picker-note text-danger" role="alert">{{ khachError }}</p>
              <p v-else-if="!khachHangAll.length" class="pgg-picker-note">Chưa có khách hàng nào đang hoạt động.</p>
              <p v-else-if="!khachFiltered.length" class="pgg-picker-note">Không tìm thấy khách hàng phù hợp.</p>
              <label
                v-for="k in khachFiltered"
                :key="k.id"
                class="pgg-picker-item"
                :class="{ 'is-locked': khachDaDung.has(k.id) }"
              >
                <input
                  type="checkbox"
                  :checked="form.khachHangIds.includes(k.id)"
                  :disabled="khachDaDung.has(k.id)"
                  @change="toggleKhach(k.id)"
                />
                <span class="pgg-picker-name">{{ k.hoTen }}</span>
                <span class="ad-code">{{ k.ma }}</span>
                <span class="pgg-picker-sub">{{ k.soDienThoai || k.email || '' }}</span>
                <span v-if="khachDaDung.has(k.id)" class="ad-pill ad-pill-gray">Đã dùng</span>
              </label>
            </div>
          </div>
          <p v-if="errors.khachHang" class="ad-error">{{ errors.khachHang }}</p>
        </div>
      </div>
    </form>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy</button>
      <button type="submit" form="pgg-form" class="ad-btn ad-btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Tạo phiếu' }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.pgg-count {
  margin-left: 0.5rem;
  font-weight: 500;
  color: var(--fs-text-2);
}
.pgg-picker {
  border: 1px solid #dfe5ec;
  border-radius: 12px;
  overflow: hidden;
}
.pgg-picker.is-invalid {
  border-color: #dc3545;
}
.pgg-picker-tools {
  display: flex;
  gap: 0.5rem;
  padding: 0.6rem;
  background: #f7f9fb;
  border-bottom: 1px solid #dfe5ec;
}
.pgg-search {
  flex: 1;
  min-width: 0;
}
.pgg-picker-list {
  max-height: 220px;
  overflow-y: auto;
}
.pgg-picker-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
  border-bottom: 1px solid #eef1f5;
}
.pgg-picker-item:last-child {
  border-bottom: 0;
}
.pgg-picker-item:hover {
  background: #f4f8ff;
}
.pgg-picker-item.is-locked {
  cursor: not-allowed;
  opacity: 0.7;
}
.pgg-picker-name {
  font-weight: 600;
  flex: 1;
  min-width: 0;
}
.pgg-picker-sub {
  color: var(--fs-text-2);
  white-space: nowrap;
}
.pgg-picker-note {
  margin: 0;
  padding: 1rem;
  text-align: center;
  color: var(--fs-text-2);
  font-size: 0.85rem;
}
</style>
