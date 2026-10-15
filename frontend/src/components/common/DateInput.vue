<script setup>
// Ô chọn ngày dùng chung: LUÔN hiện dạng ngày/tháng/năm (13/10/2026) dù trình duyệt để tiếng Anh hay tiếng Việt,
// gõ tay được (tự chèn dấu '/') hoặc bấm mở lịch tiếng Việt (tuần bắt đầu thứ Hai, chọn nhanh tháng / năm, Hôm nay, Xóa).
// v-model vẫn là 'yyyy-mm-dd' (hoặc '') giống <input type="date"> cũ nên API và kiểm tra dữ liệu không đổi.
// id, class, aria-*... truyền vào sẽ đặt lên ô nhập (nhãn <label for> vẫn trỏ đúng ô).
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { congNgay, dinhDangKhiGo, homNayIso, isoSangVn, oLich, vnSangIso } from '../../utils/ngay'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: undefined },
  min: { type: String, default: undefined },
  max: { type: String, default: undefined },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  placeholder: { type: String, default: 'dd/mm/yyyy' },
})
const emit = defineEmits(['update:modelValue'])

const THU = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
// Ngày đang chọn dạng 'yyyy-mm-dd' (API có thể trả kèm giờ)
const ngayChon = computed(() => String(props.modelValue || '').slice(0, 10))

/* ----- Ô nhập ----- */
const chu = ref(isoSangVn(props.modelValue))
watch(
  () => props.modelValue,
  (v) => {
    if (vnSangIso(chu.value) !== (v || '')) chu.value = isoSangVn(v)
  },
)

function phat(iso) {
  if (iso !== (props.modelValue || '')) emit('update:modelValue', iso)
}

function onInput(e) {
  const txt = dinhDangKhiGo(e.target.value)
  if (e.target.value !== txt) e.target.value = txt
  chu.value = txt
  const iso = vnSangIso(txt)
  if (iso === null) return // chưa gõ xong: chưa đổi giá trị
  phat(iso)
  if (iso && mo.value) xemTheo(iso)
}

// Rời ô khi đang gõ dở / gõ ngày không có thật (31/02) thì trả lại ngày đang chọn
function onBlur() {
  if (vnSangIso(chu.value) === null) chu.value = isoSangVn(props.modelValue)
}

function onNhapKey(e) {
  if (e.key === 'ArrowDown' && !mo.value) {
    e.preventDefault()
    moLich(true)
  } else if (e.key === 'Escape' && mo.value) {
    e.preventDefault()
    e.stopPropagation()
    dong()
  }
}

/* ----- Lịch ----- */
const mo = ref(false)
const daDat = ref(false) // đã tính xong vị trí (tránh nháy ở góc màn hình)
const xemNam = ref(2000)
const xemThang = ref(0)
const oFocus = ref('')
const homNay = ref(homNayIso())
const khung = ref(null)
const hop = ref(null)
const oNhap = ref(null)

const biKhoa = (iso) => !!((props.min && iso < props.min) || (props.max && iso > props.max))
const ganNhat = (iso) => (props.min && iso < props.min ? props.min : props.max && iso > props.max ? props.max : iso)

const cacO = computed(() =>
  oLich(xemNam.value, xemThang.value).map((o) => ({ ...o, khoa: biKhoa(o.iso), vn: isoSangVn(o.iso) })),
)
const cacNam = computed(() => {
  const nay = Number(homNay.value.slice(0, 4))
  const yMin = Number(String(props.min || '').slice(0, 4))
  const yMax = Number(String(props.max || '').slice(0, 4))
  let tu = yMin >= 1900 ? yMin : 1900
  let den = yMax ? Math.min(yMax, nay + 30) : nay + 30
  tu = Math.min(tu, xemNam.value)
  den = Math.max(den, xemNam.value, tu)
  const kq = []
  for (let y = tu; y <= den; y++) kq.push(y)
  return kq
})
const homNayBiKhoa = computed(() => biKhoa(homNay.value))

function xemTheo(iso) {
  const [y, m] = iso.split('-').map(Number)
  xemNam.value = y
  xemThang.value = m - 1
  oFocus.value = iso
}

// Đổi tháng / năm bằng nút hoặc ô chọn: giữ ngày đang trỏ trong tháng đang xem
watch([xemNam, xemThang], ([y, m]) => {
  if (!mo.value || cacO.value.some((o) => o.trongThang && o.iso === oFocus.value)) return
  const ngay = Math.min(Number(String(oFocus.value).slice(8, 10)) || 1, new Date(y, m + 1, 0).getDate())
  oFocus.value = `${y}-${String(m + 1).padStart(2, '0')}-${String(ngay).padStart(2, '0')}`
})

function doiThang(n) {
  const d = new Date(xemNam.value, xemThang.value + n, 1)
  xemNam.value = d.getFullYear()
  xemThang.value = d.getMonth()
}

function moLich(focusLich = false) {
  if (props.disabled || props.readonly || mo.value) return
  homNay.value = homNayIso()
  xemTheo(vnSangIso(chu.value) || vnSangIso(ngayChon.value) || ganNhat(homNay.value))
  daDat.value = false
  mo.value = true
  document.addEventListener('mousedown', onNgoai, true)
  window.addEventListener('resize', datViTri)
  window.addEventListener('scroll', datViTri, true)
  nextTick(() => {
    datViTri()
    if (focusLich) focusO()
  })
}

function dong(traFocus = false) {
  if (!mo.value) return
  mo.value = false
  document.removeEventListener('mousedown', onNgoai, true)
  window.removeEventListener('resize', datViTri)
  window.removeEventListener('scroll', datViTri, true)
  if (traFocus) oNhap.value?.focus()
}
onBeforeUnmount(() => dong())

function onNgoai(e) {
  if (khung.value && !khung.value.contains(e.target)) dong()
}
// Tab ra khỏi ô (bàn phím) thì đóng lịch
function onFocusOut(e) {
  if (mo.value && e.relatedTarget && khung.value && !khung.value.contains(e.relatedTarget)) dong()
}

function chon(iso) {
  if (biKhoa(iso)) return
  chu.value = isoSangVn(iso)
  phat(iso)
  dong(true)
}
function xoa() {
  chu.value = ''
  phat('')
  dong(true)
}

function focusO() {
  nextTick(() => hop.value?.querySelector(`[data-iso="${oFocus.value}"]`)?.focus())
}
function onLuoiKey(e) {
  const buoc = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }[e.key]
  if (buoc) {
    e.preventDefault()
    xemTheo(congNgay(oFocus.value, buoc))
    focusO()
  } else if (e.key === 'PageUp' || e.key === 'PageDown') {
    e.preventDefault()
    doiThang(e.key === 'PageUp' ? -1 : 1)
    focusO()
  }
}

/**
 * Lịch dùng position: fixed để không bị khung cuộn của popup cắt mất. Trang đang thu nhỏ (body zoom 75%)
 * nên đo tỉ lệ thật bằng cách đặt thử 2 vị trí rồi quy đổi, mở lên trên nếu bên dưới không đủ chỗ.
 */
function datViTri() {
  const k = khung.value
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
  const w = r1.width
  const h = r1.height
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
  <div ref="khung" class="di-wrap" :class="{ 'is-open': mo }" @focusout="onFocusOut">
    <input
      :id="id"
      ref="oNhap"
      v-bind="$attrs"
      type="text"
      inputmode="numeric"
      autocomplete="off"
      maxlength="10"
      class="di-input"
      :value="chu"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      aria-haspopup="dialog"
      :aria-expanded="mo"
      @input="onInput"
      @blur="onBlur"
      @keydown="onNhapKey"
      @click="moLich(false)"
    />
    <button
      type="button"
      class="di-btn"
      tabindex="-1"
      :disabled="disabled || readonly"
      aria-label="Mở lịch chọn ngày"
      @click="mo ? dong(true) : moLich(true)"
    >
      <i class="bi bi-calendar3" aria-hidden="true"></i>
    </button>

    <div
      v-if="mo"
      ref="hop"
      class="di-pop"
      :style="{ visibility: daDat ? 'visible' : 'hidden' }"
      role="dialog"
      aria-label="Chọn ngày"
      data-testid="lich-chon-ngay"
      @keydown.esc.stop.prevent="dong(true)"
    >
      <div class="di-head">
        <button type="button" class="di-nav" aria-label="Tháng trước" @click="doiThang(-1)">
          <i class="bi bi-chevron-left" aria-hidden="true"></i>
        </button>
        <select v-model.number="xemThang" class="di-sel" aria-label="Tháng">
          <option v-for="t in 12" :key="t" :value="t - 1">Tháng {{ t }}</option>
        </select>
        <select v-model.number="xemNam" class="di-sel" aria-label="Năm">
          <option v-for="y in cacNam" :key="y" :value="y">{{ y }}</option>
        </select>
        <button type="button" class="di-nav" aria-label="Tháng sau" @click="doiThang(1)">
          <i class="bi bi-chevron-right" aria-hidden="true"></i>
        </button>
      </div>
      <div class="di-week" aria-hidden="true">
        <span v-for="t in THU" :key="t" :class="{ 'is-cn': t === 'CN' }">{{ t }}</span>
      </div>
      <div class="di-grid" role="grid" @keydown="onLuoiKey">
        <button
          v-for="o in cacO"
          :key="o.iso"
          type="button"
          class="di-day"
          :class="{ 'is-ngoai': !o.trongThang, 'is-chon': o.iso === ngayChon, 'is-homnay': o.iso === homNay }"
          :data-iso="o.iso"
          :disabled="o.khoa"
          :tabindex="o.iso === oFocus ? 0 : -1"
          :aria-label="o.vn"
          :aria-pressed="o.iso === ngayChon"
          @click="chon(o.iso)"
        >
          {{ o.ngay }}
        </button>
      </div>
      <div class="di-foot">
        <button type="button" class="di-link" @click="xoa">Xóa</button>
        <button type="button" class="di-link" :disabled="homNayBiKhoa" @click="chon(homNay)">Hôm nay</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.di-wrap {
  position: relative;
}
.di-wrap .di-input {
  padding-right: 2.4rem;
}
.di-btn {
  position: absolute;
  top: 50%;
  right: 0.35rem;
  transform: translateY(-50%);
  width: 2rem;
  height: 2rem;
  display: inline-grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #5b6573;
  font-size: 0.95rem;
  cursor: pointer;
}
.di-btn:hover:not(:disabled),
.di-wrap.is-open .di-btn {
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-primary, #0977ec);
}
.di-btn:disabled {
  cursor: default;
  opacity: 0.5;
}

.di-pop {
  position: fixed;
  z-index: 2000;
  width: 18.5rem;
  padding: 0.75rem;
  background: #fff;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(16, 24, 40, 0.16);
  font-size: 0.88rem;
  color: var(--fs-text, #18242f);
}
.di-head {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.5rem;
}
.di-nav {
  flex: none;
  width: 2rem;
  height: 2rem;
  display: inline-grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #3d4652;
}
.di-nav:hover {
  background: var(--fs-hover-bg, #e7f1fd);
  color: var(--fs-primary, #0977ec);
}
.di-sel {
  flex: 1 1 auto;
  min-width: 0;
  height: 2rem;
  padding: 0 0.4rem;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--fs-text, #18242f);
  cursor: pointer;
}
.di-sel:focus {
  outline: none;
  border-color: var(--fs-primary, #0977ec);
}
.di-week,
.di-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}
.di-week span {
  padding: 0.25rem 0;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--fs-muted, #7b8794);
}
.di-week span.is-cn {
  color: #d9534f;
}
.di-day {
  height: 2.2rem;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  font-size: 0.85rem;
}
.di-day:hover:not(:disabled) {
  background: var(--fs-hover-bg, #e7f1fd);
}
.di-day:focus-visible {
  outline: 2px solid var(--fs-primary, #0977ec);
  outline-offset: -2px;
}
.di-day.is-ngoai {
  color: #b3bcc8;
}
.di-day.is-homnay {
  box-shadow: inset 0 0 0 1.5px var(--fs-primary, #0977ec);
  font-weight: 600;
}
.di-day.is-chon {
  background: var(--fs-primary, #0977ec);
  color: #fff;
  font-weight: 600;
}
.di-day:disabled {
  color: #d3d9e1;
  background: transparent;
  cursor: not-allowed;
  text-decoration: line-through;
}
.di-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--fs-line-soft, #eef1f5);
}
.di-link {
  border: 0;
  background: transparent;
  padding: 0.25rem 0.5rem;
  border-radius: 8px;
  color: var(--fs-primary, #0977ec);
  font-weight: 600;
  font-size: 0.85rem;
}
.di-link:hover:not(:disabled) {
  background: var(--fs-hover-bg, #e7f1fd);
}
.di-link:disabled {
  color: #b3bcc8;
  cursor: not-allowed;
}
</style>
