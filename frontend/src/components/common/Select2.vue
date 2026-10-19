<script setup>
// Ô chọn kiểu Select2 dùng chung cho MỌI combobox của hệ thống: bấm vào mở danh sách có ô tìm kiếm (không phân biệt hoa thường
// và dấu), chọn bằng chuột hoặc bàn phím (mũi tên, Enter, Esc), có thể bỏ chọn.
// Bật allowCreate thì gõ tên chưa có sẽ hiện dòng "Thêm nhanh “tên”": bấm vào là phát sự kiện `create` (kèm tên đã gõ) để trang
// cha tạo mục mới ngay tại chỗ, khỏi phải sang trang thuộc tính. Trang cha tạo xong tự gán giá trị mới vào v-model.
//
//   <Select2 id="sp-thuong-hieu" v-model="form.idThuongHieu" :options="dsThuongHieu" placeholder="Chọn thương hiệu"
//            allow-create @create="themNhanhThuongHieu" :class="{ 'is-invalid': errors.idThuongHieu }" />
//
// options: [{ value, label, sub?, search?, disabled? }]; value giữ nguyên kiểu (số / chữ). class, id, aria-*... truyền vào sẽ đặt
// lên nút bấm (nhãn <label for> vẫn trỏ đúng ô; class="ad-control" cho đúng kiểu ô nhập của trang quản trị).
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { normalizeText } from '../../utils/text'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: { type: [String, Number, Boolean], default: '' },
  options: { type: Array, default: () => [] },
  id: { type: String, default: undefined },
  placeholder: { type: String, default: 'Chọn…' },
  searchPlaceholder: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  emptyValue: { type: [String, Number, Boolean], default: '' },
  allowCreate: { type: Boolean, default: false },
  createLabel: { type: String, default: 'Thêm nhanh' },
  creating: { type: Boolean, default: false },
  emptyText: { type: String, default: 'Không có kết quả' },
  // Danh sách ít hơn số này thì ẩn ô tìm kiếm (giống minimumResultsForSearch của Select2). 0 = luôn có ô tìm kiếm.
  minSearch: { type: Number, default: 0 },
  maxLength: { type: Number, default: 100 },
})
const emit = defineEmits(['update:modelValue', 'change', 'create', 'open', 'close'])

const uid = `s2-${useId()}`

const chuoi = (v) => (v === null || v === undefined ? '' : String(v))
const dangChon = computed(() => props.options.find((o) => chuoi(o.value) === chuoi(props.modelValue)) ?? null)
const coGiaTri = computed(() => chuoi(props.modelValue) !== '')
const nhanHienThi = computed(() => dangChon.value?.label ?? '')

/* ----- Danh sách ----- */
const mo = ref(false)
const daDat = ref(false)
const tuKhoa = ref('')
const viTri = ref(0)
const khung = ref(null)
const nut = ref(null)
const hop = ref(null)
const oTim = ref(null)
const danhSach = ref(null)

const hienTimKiem = computed(() => props.allowCreate || props.options.length >= props.minSearch)
const tuKhoaSach = computed(() => tuKhoa.value.trim())

const dsLoc = computed(() => {
  const k = normalizeText(tuKhoaSach.value)
  if (!k) return props.options
  return props.options.filter((o) => normalizeText(`${o.label ?? ''} ${o.search ?? ''}`).includes(k))
})
// Có gõ chữ mà chưa có mục nào trùng tên (không phân biệt hoa thường / dấu) thì mới cho "Thêm nhanh"
const hienThemNhanh = computed(
  () =>
    props.allowCreate &&
    tuKhoaSach.value !== '' &&
    !props.options.some((o) => normalizeText(o.label ?? '') === normalizeText(tuKhoaSach.value)),
)
const tongDong = computed(() => dsLoc.value.length + (hienThemNhanh.value ? 1 : 0))
const placeholderTimKiem = computed(
  () => props.searchPlaceholder || (props.allowCreate ? 'Tìm kiếm hoặc tạo mới…' : 'Tìm kiếm…'),
)

function moDanhSach() {
  if (props.disabled || mo.value) return
  tuKhoa.value = ''
  const i = props.options.findIndex((o) => chuoi(o.value) === chuoi(props.modelValue))
  viTri.value = Math.max(0, i)
  daDat.value = false
  mo.value = true
  document.addEventListener('mousedown', onNgoai, true)
  window.addEventListener('resize', datViTri)
  window.addEventListener('scroll', onCuon, true)
  emit('open')
  nextTick(() => {
    datViTri()
    oTim.value?.focus()
    cuonToiMucDangTro()
  })
}

function dong(traFocus = false) {
  if (!mo.value) return
  mo.value = false
  document.removeEventListener('mousedown', onNgoai, true)
  window.removeEventListener('resize', datViTri)
  window.removeEventListener('scroll', onCuon, true)
  emit('close')
  if (traFocus) nut.value?.focus()
}
onBeforeUnmount(() => dong())

function onNgoai(e) {
  if (khung.value && !khung.value.contains(e.target)) dong()
}
// Cuộn bên trong chính danh sách thì không cần đặt lại vị trí; cuộn trang / hộp thoại thì đặt lại cho đi theo ô
function onCuon(e) {
  if (hop.value && hop.value.contains(e.target)) return
  datViTri()
}
function onFocusOut(e) {
  if (mo.value && e.relatedTarget && khung.value && !khung.value.contains(e.relatedTarget)) dong()
}

function chon(o) {
  if (!o || o.disabled) return
  emit('update:modelValue', o.value)
  if (chuoi(o.value) !== chuoi(props.modelValue)) emit('change', o.value)
  dong(true)
}
function xoa() {
  if (props.disabled) return
  emit('update:modelValue', props.emptyValue)
  emit('change', props.emptyValue)
  nut.value?.focus()
}
function taoMoi() {
  if (!hienThemNhanh.value || props.creating) return
  const ten = tuKhoaSach.value.replace(/\s+/g, ' ').slice(0, props.maxLength)
  emit('create', ten)
  dong(true)
}

/* ----- Bàn phím ----- */
function dichChuyen(buoc) {
  const n = tongDong.value
  if (!n) return
  viTri.value = (viTri.value + buoc + n) % n
  cuonToiMucDangTro()
}
function cuonToiMucDangTro() {
  nextTick(() => danhSach.value?.querySelector('[data-dang-tro="true"]')?.scrollIntoView?.({ block: 'nearest' }))
}
function onTimKey(e) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    dichChuyen(1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    dichChuyen(-1)
  } else if (e.key === 'Home' && !tuKhoa.value) {
    e.preventDefault()
    viTri.value = 0
    cuonToiMucDangTro()
  } else if (e.key === 'End' && !tuKhoa.value) {
    e.preventDefault()
    viTri.value = Math.max(0, tongDong.value - 1)
    cuonToiMucDangTro()
  } else if (e.key === 'Enter') {
    e.preventDefault()
    chonTheoViTri()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    dong(true)
  } else if (e.key === 'Tab') {
    dong()
  }
}
function chonTheoViTri() {
  if (viTri.value < dsLoc.value.length) chon(dsLoc.value[viTri.value])
  else if (hienThemNhanh.value) taoMoi()
}
function onNutKey(e) {
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    moDanhSach()
  } else if (e.key === 'Escape' && mo.value) {
    e.preventDefault()
    e.stopPropagation()
    dong(true)
  }
}

watch(tuKhoa, () => {
  viTri.value = 0
  nextTick(datViTri)
})
watch(
  () => props.options,
  () => nextTick(datViTri),
)

/**
 * Danh sách dùng position: fixed để không bị khung cuộn của hộp thoại cắt mất. Trang đang thu nhỏ (zoom) nên đo tỉ lệ thật
 * bằng cách đặt thử 2 vị trí rồi quy đổi; mở lên trên nếu bên dưới không đủ chỗ.
 */
function datViTri() {
  const k = nut.value ?? khung.value
  const p = hop.value
  if (!k || !p) return
  const r = k.getBoundingClientRect()
  p.style.left = '0px'
  p.style.top = '0px'
  const r0 = p.getBoundingClientRect()
  p.style.left = '100px'
  p.style.top = '100px'
  const r1 = p.getBoundingClientRect()
  const sx = (r1.left - r0.left) / 100 || 1
  const sy = (r1.top - r0.top) / 100 || 1
  p.style.minWidth = `${r.width / sx}px`
  const w = p.getBoundingClientRect().width
  const h = p.getBoundingClientRect().height
  const vw = window.innerWidth
  const vh = window.innerHeight
  let x = r.left
  if (x + w > vw - 8) x = Math.max(8, r.right - w)
  let y = r.bottom + 4
  if (y + h > vh - 8 && r.top - 4 - h >= 8) y = r.top - 4 - h
  p.style.left = `${(x - r0.left) / sx}px`
  p.style.top = `${(y - r0.top) / sy}px`
  daDat.value = true
}
</script>

<template>
  <div ref="khung" class="s2-wrap" :class="{ 'is-open': mo }" @focusout="onFocusOut">
    <button
      :id="id"
      ref="nut"
      v-bind="$attrs"
      type="button"
      class="form-select s2-trigger"
      :class="{ 'is-invalid': invalid, 's2-co-xoa': clearable && coGiaTri && !disabled }"
      role="combobox"
      :data-gia-tri="chuoi(modelValue)"
      aria-haspopup="listbox"
      :aria-expanded="mo"
      :aria-controls="mo ? uid : undefined"
      :disabled="disabled"
      @click="mo ? dong() : moDanhSach()"
      @keydown="onNutKey"
    >
      <span class="s2-value" :class="{ 's2-placeholder': !dangChon }">{{ dangChon ? nhanHienThi : placeholder }}</span>
    </button>
    <button
      v-if="clearable && coGiaTri && !disabled"
      type="button"
      class="s2-clear"
      aria-label="Bỏ chọn"
      @click.stop="xoa"
    >
      <i class="bi bi-x-lg" aria-hidden="true"></i>
    </button>

    <div
      v-if="mo"
      ref="hop"
      class="s2-pop"
      :style="{ visibility: daDat ? 'visible' : 'hidden' }"
      data-testid="select2-popup"
    >
      <div v-if="hienTimKiem" class="s2-search">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input
          ref="oTim"
          v-model="tuKhoa"
          type="text"
          class="s2-search-input"
          autocomplete="off"
          :placeholder="placeholderTimKiem"
          :aria-label="placeholderTimKiem"
          :aria-controls="uid"
          @keydown="onTimKey"
        />
      </div>
      <ul :id="uid" ref="danhSach" class="s2-list" role="listbox">
        <li
          v-for="(o, i) in dsLoc"
          :key="`${chuoi(o.value)}|${i}`"
          class="s2-opt"
          :class="{
            'is-active': i === viTri,
            'is-chon': dangChon && chuoi(o.value) === chuoi(modelValue),
            'is-disabled': o.disabled,
          }"
          role="option"
          :data-value="chuoi(o.value)"
          :aria-selected="!!dangChon && chuoi(o.value) === chuoi(modelValue)"
          :aria-disabled="o.disabled ? 'true' : undefined"
          :data-dang-tro="i === viTri"
          @mousedown.prevent
          @mousemove="viTri = i"
          @click="chon(o)"
        >
          <span class="s2-opt-label">{{ o.label }}</span>
          <small v-if="o.sub" class="s2-opt-sub">{{ o.sub }}</small>
          <i v-if="dangChon && chuoi(o.value) === chuoi(modelValue)" class="bi bi-check2 s2-tick" aria-hidden="true"></i>
        </li>
        <li
          v-if="hienThemNhanh"
          class="s2-opt s2-create"
          :class="{ 'is-active': viTri === dsLoc.length, 'is-disabled': creating }"
          role="option"
          data-testid="select2-them-nhanh"
          :data-dang-tro="viTri === dsLoc.length"
          @mousedown.prevent
          @mousemove="viTri = dsLoc.length"
          @click="taoMoi"
        >
          <i class="bi bi-plus-circle" aria-hidden="true"></i>
          <span>{{ createLabel }} “{{ tuKhoaSach }}”</span>
        </li>
        <li v-if="!dsLoc.length && !hienThemNhanh" class="s2-empty" role="presentation">{{ emptyText }}</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.s2-wrap {
  position: relative;
  width: 100%;
}
.s2-trigger {
  display: block;
  width: 100%;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}
.s2-trigger.s2-co-xoa {
  padding-right: 3.6rem;
}
.s2-trigger:disabled {
  cursor: not-allowed;
}
.s2-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}
.s2-placeholder {
  color: #a3acb9;
}
.s2-wrap.is-open .s2-trigger {
  border-color: var(--fs-primary, #0977ec);
  box-shadow: 0 0 0 4px rgba(9, 119, 236, 0.14);
}
.s2-clear {
  position: absolute;
  top: 50%;
  right: 2.1rem;
  transform: translateY(-50%);
  width: 1.6rem;
  height: 1.6rem;
  display: inline-grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #8a94a3;
  font-size: 0.7rem;
  cursor: pointer;
}
.s2-clear:hover {
  background: #eef1f5;
  color: var(--fs-danger, #dc3545);
}

.s2-pop {
  position: fixed;
  z-index: 2000;
  max-width: min(92vw, 34rem);
  background: #fff;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(16, 24, 40, 0.16);
  font-size: 0.88rem;
  color: var(--fs-text, #18242f);
  overflow: hidden;
}
.s2-search {
  position: relative;
  padding: 0.55rem 0.55rem 0.35rem;
}
.s2-search > .bi {
  position: absolute;
  left: 1.2rem;
  top: 50%;
  transform: translateY(-35%);
  color: var(--fs-muted, #7b8794);
  font-size: 0.8rem;
  pointer-events: none;
}
.s2-search-input {
  width: 100%;
  height: 2.2rem;
  padding: 0 0.7rem 0 2rem;
  border: 1px solid #dce3ec;
  border-radius: 9px;
  font-size: 0.86rem;
  color: var(--fs-text, #18242f);
  background: #fff;
}
.s2-search-input:focus {
  outline: none;
  border-color: var(--fs-primary, #0977ec);
  box-shadow: 0 0 0 3px rgba(9, 119, 236, 0.12);
}
.s2-list {
  max-height: 15rem;
  margin: 0;
  padding: 0.3rem;
  overflow-y: auto;
  list-style: none;
}
.s2-opt {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.7rem;
  border-radius: 8px;
  cursor: pointer;
}
.s2-opt-label {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.s2-opt-sub {
  flex: none;
  color: var(--fs-muted, #7b8794);
}
.s2-opt.is-active {
  background: var(--fs-hover-bg, #e7f1fd);
}
.s2-opt.is-chon {
  color: var(--fs-primary, #0977ec);
  font-weight: 600;
}
.s2-opt.is-disabled {
  color: #b3bcc8;
  cursor: not-allowed;
}
.s2-tick {
  flex: none;
}
.s2-create {
  color: var(--fs-primary, #0977ec);
  font-weight: 600;
}
.s2-empty {
  padding: 0.7rem;
  text-align: center;
  color: var(--fs-muted, #7b8794);
}
</style>
