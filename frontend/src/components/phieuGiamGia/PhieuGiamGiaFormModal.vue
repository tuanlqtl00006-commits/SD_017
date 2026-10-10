<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import FormPageShell from '../common/FormPageShell.vue'
import { HINH_THUC, LOAI_GIAM, taoMaPhieu } from '../../constants/phieuGiamGia'
import { phieuGiamGiaService } from '../../services/phieuGiamGiaService'
import { includesText } from '../../utils/text'
import { todayIso } from '../../utils/format'

const props = defineProps({
  item: { type: Object, default: null }, // null: tạo mới; khi sửa có kèm khachHangs (lấy từ API chi tiết)
  all: { type: Array, default: () => [] }, // toàn bộ phiếu, dùng để kiểm tra trùng mã
  saving: { type: Boolean, default: false },
  asPage: { type: Boolean, default: false }, // true: hiển thị như một trang (không phải cửa sổ nổi)
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)
const titleText = computed(() => (isEdit.value ? 'Chỉnh sửa phiếu giảm giá' : props.asPage ? 'Thông tin phiếu' : 'Tạo phiếu giảm giá'))
// Phiếu đã có người dùng thì không đổi hình thức (công khai / cá nhân) và không bỏ được khách đã dùng.
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

/* ----- Chọn khách hàng (chỉ phiếu cá nhân) ----- */
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

// Ô chọn ở tiêu đề bảng: tick khi tất cả khách đang hiển thị đều đã được chọn
const tatCaDangLocDaChon = computed(
  () => khachFiltered.value.length > 0 && khachFiltered.value.every((k) => form.khachHangIds.includes(k.id)),
)
const motPhanDangLocDaChon = computed(
  () => !tatCaDangLocDaChon.value && khachFiltered.value.some((k) => form.khachHangIds.includes(k.id)),
)
function toggleTatCaDangLoc() {
  if (tatCaDangLocDaChon.value) boChonDangLoc()
  else chonTatCaDangLoc()
}

/** '2000-01-15' -> '15/01/2000' */
function hienNgay(iso) {
  if (!iso) return ''
  const [y, m, d] = String(iso).slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}
/** '2026-08-12T17:07:30' -> '12/08/2026 17:07' */
function hienNgayGio(iso) {
  if (!iso) return ''
  return `${hienNgay(iso)} ${String(iso).slice(11, 16)}`
}

// Phiếu cá nhân: mỗi khách được tặng 1 phiếu nên số lượng = số khách được chọn.
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

// Đổi sang giảm theo số tiền thì bỏ "giảm tối đa" (chỉ dùng cho giảm theo %).
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

// Sau lần bấm lưu đầu tiên, báo lỗi theo thời gian thực để người dùng thấy ngay khi đã sửa đúng.
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
  <component
    :is="asPage ? FormPageShell : BaseModal"

    v-bind="asPage ? { title: titleText } : { title: titleText, size: isCaNhan ? 'xl' : 'lg', staticBackdrop: true, onClose: () => emit('close') }"
=======
    v-bind="asPage ? { title: titleText } : { title: titleText, size: 'lg', staticBackdrop: true, onClose: () => emit('close') }"
  >
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
          <section class="pgg-kh" :class="{ 'is-invalid': errors.khachHang }" aria-labelledby="pgg-khach-label">
            <header class="pgg-kh-head">
              <h3 id="pgg-khach-label" class="pgg-kh-title">
                Danh sách khách hàng nhận phiếu <span class="ad-required">*</span>
              </h3>
              <span class="pgg-kh-badge">Đã chọn {{ form.khachHangIds.length }}</span>
            </header>

            <div class="ad-affix pgg-kh-search">
              <i class="bi bi-search" aria-hidden="true"></i>
              <input
                v-model.trim="khachKeyword"
                type="text"
                class="form-control ad-control"
                placeholder="Tìm kiếm theo mã, tên, SĐT…"
                autocomplete="off"
                aria-label="Tìm khách hàng"
              />
            </div>

            <div class="pgg-kh-table-wrap">
              <table class="pgg-kh-table">
                <thead>
                  <tr>
                    <th class="pgg-kh-check">
                      <input
                        type="checkbox"
                        class="form-check-input"
                        :checked="tatCaDangLocDaChon"
                        :indeterminate="motPhanDangLocDaChon"
                        :disabled="!khachFiltered.length"
                        aria-label="Chọn tất cả khách hàng đang hiển thị"
                        @change="toggleTatCaDangLoc"
                      />
                    </th>
                    <th>Mã KH</th>
                    <th>Tên khách hàng</th>
                    <th>Ngày sinh</th>
                    <th>Số điện thoại</th>
                    <th>Email</th>
                    <th class="text-center">Đã mua</th>
                    <th class="text-center">Gần nhất</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="khachLoading">
                    <td colspan="8" class="pgg-kh-note">Đang tải danh sách khách hàng…</td>
                  </tr>
                  <tr v-else-if="khachError">
                    <td colspan="8" class="pgg-kh-note text-danger" role="alert">{{ khachError }}</td>
                  </tr>
                  <tr v-else-if="!khachHangAll.length">
                    <td colspan="8" class="pgg-kh-note">Chưa có khách hàng nào đang hoạt động.</td>
                  </tr>
                  <tr v-else-if="!khachFiltered.length">
                    <td colspan="8" class="pgg-kh-note">Không tìm thấy khách hàng phù hợp.</td>
                  </tr>
                  <template v-else>
                    <tr
                      v-for="k in khachFiltered"
                      :key="k.id"
                      :class="{ 'is-selected': form.khachHangIds.includes(k.id), 'is-locked': khachDaDung.has(k.id) }"
                      @click="toggleKhach(k.id)"
                    >
                      <td class="pgg-kh-check">
                        <input
                          type="checkbox"
                          class="form-check-input"
                          :checked="form.khachHangIds.includes(k.id)"
                          :disabled="khachDaDung.has(k.id)"
                          :aria-label="`Chọn ${k.hoTen}`"
                          @click.stop
                          @change="toggleKhach(k.id)"
                        />
                      </td>
                      <td class="pgg-kh-muted">{{ k.ma }}</td>
                      <td class="pgg-kh-name">
                        {{ k.hoTen }}
                        <span v-if="khachDaDung.has(k.id)" class="ad-pill ad-pill-gray ms-1">Đã dùng</span>
                      </td>
                      <td class="pgg-kh-muted">{{ hienNgay(k.ngaySinh) || '—' }}</td>
                      <td>{{ k.soDienThoai || '—' }}</td>
                      <td class="pgg-kh-email">{{ k.email || '—' }}</td>
                      <td class="text-center pgg-kh-strong">{{ k.soDonDaMua ?? 0 }} đơn</td>
                      <td class="text-center pgg-kh-muted">{{ hienNgayGio(k.lanMuaGanNhat) || '---' }}</td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </section>
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
  </component>
</template>

<style scoped>
/* ---------- Bảng chọn khách hàng nhận phiếu (phiếu cá nhân) ---------- */
.pgg-kh {
  padding: 1rem;
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  background: #fff;
}
.pgg-kh.is-invalid {
  border-color: var(--fs-danger);
}
.pgg-kh-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.pgg-kh-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--fs-text);
}
.pgg-kh-badge {
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  background: var(--fs-primary);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}
.pgg-kh-search {
  max-width: 420px;
  margin-bottom: 0.75rem;
}
.pgg-kh-search .ad-control {
  border-radius: 999px;
}
.pgg-kh-table-wrap {
  min-height: 200px;
  max-height: 320px;
  overflow: auto;
  border: 1px solid var(--fs-line);
  border-radius: 10px;
}
.pgg-kh-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}
.pgg-kh-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 0.75rem;
  background: #fff;
  border-bottom: 1px solid var(--fs-line);
  color: var(--fs-text-2);
  font-weight: 500;
  white-space: nowrap;
}
.pgg-kh-table td {
  padding: 0.7rem 0.75rem;
  border-bottom: 1px solid var(--fs-line-soft);
  white-space: nowrap;
  vertical-align: middle;
}
.pgg-kh-table tbody tr {
  cursor: pointer;
  transition: background-color 0.12s ease;
}
.pgg-kh-table tbody tr:hover {
  background: var(--fs-hover-row);
}
.pgg-kh-table tbody tr.is-selected {
  background: var(--fs-primary-soft);
}
.pgg-kh-table tbody tr.is-locked {
  cursor: not-allowed;
}
.pgg-kh-check {
  width: 44px;
  text-align: center;
}
.pgg-kh-check .form-check-input {
  margin: 0;
  cursor: pointer;
}
.pgg-kh-name {
  font-weight: 600;
  color: var(--fs-text);
}
.pgg-kh-strong {
  font-weight: 500;
}
.pgg-kh-muted {
  color: var(--fs-muted);
}
.pgg-kh-email {
  color: var(--fs-text-2);
}
.pgg-kh-note {
  padding: 2rem 1rem !important;
  text-align: center;
  color: var(--fs-text-2);
  cursor: default;
}
</style>
