<script setup>
// Ô chọn NGÀY + GIỜ dùng chung (phiếu / đợt giảm giá): ngày bấm lịch hoặc gõ dd/mm/yyyy, giờ bấm mở bảng chọn Giờ : Phút hoặc gõ HH:mm (tự chèn dấu ':').
// v-model là 'yyyy-MM-ddTHH:mm' (hoặc '' khi chưa chọn ngày). Chọn ngày mà chưa có giờ thì lấy giờ mặc định (defaultTime):
// '00:00' cho ngày bắt đầu, '23:59' cho ngày kết thúc. min / max chỉ khóa phần ngày trên lịch; giờ do form kiểm tra.
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import DateInput from './DateInput.vue'
import { bayGioIso, dinhDangGioKhiGo, gioHopLe, gopNgayGio, tachNgayGio } from '../../utils/ngayGio'

const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: undefined },
  min: { type: String, default: undefined },
  max: { type: String, default: undefined },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
  defaultTime: { type: String, default: '00:00' },
  showNow: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue'])

const DANH_SACH_GIO = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const DANH_SACH_PHUT = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'))
const MOC_NHANH = ['00:00', '08:00', '12:00', '18:00', '23:59']

const tach = computed(() => tachNgayGio(props.modelValue))
const gio = ref(tach.value.gio)
const minTach = computed(() => (props.min ? tachNgayGio(props.min) : null))
const maxTach = computed(() => (props.max ? tachNgayGio(props.max) : null))
const minNgay = computed(() => minTach.value?.ngay || undefined)
const maxNgay = computed(() => maxTach.value?.ngay || undefined)

const gioDangChon = computed(() => {
  const g = gioHopLe(gio.value) ? gio.value : tach.value.gio || props.defaultTime || '00:00'
  const [hh = '00', mm = '00'] = g.split(':')
  return { hh, mm, full: `${hh}:${mm}` }
})

function isGioDisabled(h) {
  const hh = Number(h)
  const ngay = tach.value.ngay
  if (!ngay) return false
  if (minTach.value?.ngay) {
    if (ngay < minTach.value.ngay) return true
    if (ngay === minTach.value.ngay && minTach.value.gio) {
      const minH = Number(minTach.value.gio.split(':')[0])
      if (hh < minH) return true
    }
  }
  if (maxTach.value?.ngay) {
    if (ngay > maxTach.value.ngay) return true
    if (ngay === maxTach.value.ngay && maxTach.value.gio) {
      const maxH = Number(maxTach.value.gio.split(':')[0])
      if (hh > maxH) return true
    }
  }
  return false
}

function isPhutDisabled(m, forHour = null) {
  const mm = Number(m)
  const hh = Number(forHour !== null ? forHour : gioDangChon.value.hh)
  const ngay = tach.value.ngay
  if (!ngay) return false
  if (minTach.value?.ngay) {
    if (ngay < minTach.value.ngay) return true
    if (ngay === minTach.value.ngay && minTach.value.gio) {
      const [minH, minM] = minTach.value.gio.split(':').map(Number)
      if (hh < minH) return true
      if (hh === minH && mm < minM) return true
    }
  }
  if (maxTach.value?.ngay) {
    if (ngay > maxTach.value.ngay) return true
    if (ngay === maxTach.value.ngay && maxTach.value.gio) {
      const [maxH, maxM] = maxTach.value.gio.split(':').map(Number)
      if (hh > maxH) return true
      if (hh === maxH && mm > maxM) return true
    }
  }
  return false
}

function isMocDisabled(hhmm) {
  const ngay = tach.value.ngay
  if (!ngay) return false
  if (minTach.value?.ngay) {
    if (ngay < minTach.value.ngay) return true
    if (ngay === minTach.value.ngay && minTach.value.gio && hhmm < minTach.value.gio) return true
  }
  if (maxTach.value?.ngay) {
    if (ngay > maxTach.value.ngay) return true
    if (ngay === maxTach.value.ngay && maxTach.value.gio && hhmm > maxTach.value.gio) return true
  }
  return false
}

watch(
  () => props.modelValue,
  (v) => {
    const t = tachNgayGio(v)
    if (!t.ngay) {
      gio.value = ''
    } else if (t.gio && t.gio !== gio.value) {
      gio.value = t.gio
    }
  },
)

function phat(v) {
  if (v !== (props.modelValue || '')) emit('update:modelValue', v)
}

function doiNgay(iso) {
  if (!iso) {
    gio.value = ''
    phat('')
    return
  }
  let g = gioHopLe(gio.value) ? gio.value : tach.value.gio || props.defaultTime
  if (props.min && iso) {
    const minT = tachNgayGio(props.min)
    if (minT.ngay && iso === minT.ngay && minT.gio && g < minT.gio) {
      g = minT.gio
    }
  }
  if (props.max && iso) {
    const maxT = tachNgayGio(props.max)
    if (maxT.ngay && iso === maxT.ngay && maxT.gio && g > maxT.gio) {
      g = maxT.gio
    }
  }
  gio.value = g
  phat(gopNgayGio(iso, g, props.defaultTime))
}

function onGioInput(e) {
  const dangXoa = String(e.inputType || '').startsWith('delete')
  const txt = dinhDangGioKhiGo(e.target.value, dangXoa)
  if (e.target.value !== txt) e.target.value = txt
  gio.value = txt
  if (gioHopLe(txt) && tach.value.ngay) phat(gopNgayGio(tach.value.ngay, txt, props.defaultTime))
}

// Rời ô giờ khi gõ dở thì trả lại giờ đang chọn (hoặc giờ mặc định nếu đã có ngày)
function onGioBlur() {
  if (gioHopLe(gio.value)) return
  gio.value = tach.value.ngay ? tach.value.gio || props.defaultTime : ''
  if (tach.value.ngay && !tach.value.gio) phat(gopNgayGio(tach.value.ngay, gio.value, props.defaultTime))
}

function datBayGio() {
  if (props.disabled || props.readonly) return
  const v = bayGioIso()
  gio.value = tachNgayGio(v).gio
  phat(v)
}

/* ----- Bảng chọn Giờ : Phút ----- */
const moGio = ref(false)
const daDatViTri = ref(false)
const khungGio = ref(null)
const hopGio = ref(null)
const oGio = ref(null)
const cotGio = ref(null)
const cotPhut = ref(null)

function datGioTuBang(hhmm) {
  gio.value = hhmm
  if (tach.value.ngay) {
    phat(gopNgayGio(tach.value.ngay, hhmm, props.defaultTime))
  }
}

function chonGio(hh) {
  if (isGioDisabled(hh)) return
  let mm = gioDangChon.value.mm
  if (isPhutDisabled(mm, hh)) {
    const hopLe = DANH_SACH_PHUT.find((m) => !isPhutDisabled(m, hh))
    if (hopLe) mm = hopLe
  }
  datGioTuBang(`${hh}:${mm}`)
}

function chonPhut(mm) {
  if (isPhutDisabled(mm)) return
  datGioTuBang(`${gioDangChon.value.hh}:${mm}`)
}

function chonMocNhanh(hhmm) {
  if (isMocDisabled(hhmm)) return
  datGioTuBang(hhmm)
  dongGio(true)
}

function chonBayGioTuBang() {
  const v = bayGioIso()
  const g = tachNgayGio(v).gio
  if (props.min && v < props.min) return
  if (props.max && v > props.max) return
  if (tach.value.ngay) {
    datGioTuBang(g)
  } else {
    gio.value = g
    phat(v)
  }
  dongGio(true)
}

function cuonToiMucDangChon() {
  const gEl = cotGio.value?.querySelector('.is-chon') || cotGio.value?.querySelector('.dti-item:not(:disabled)')
  const pEl = cotPhut.value?.querySelector('.is-chon') || cotPhut.value?.querySelector('.dti-item:not(:disabled)')
  if (gEl && cotGio.value) {
    cotGio.value.scrollTop = Math.max(0, gEl.offsetTop - cotGio.value.clientHeight / 2 + gEl.offsetHeight / 2)
  }
  if (pEl && cotPhut.value) {
    cotPhut.value.scrollTop = Math.max(0, pEl.offsetTop - cotPhut.value.clientHeight / 2 + pEl.offsetHeight / 2)
  }
}

function datViTriGio() {
  const k = khungGio.value
  const p = hopGio.value
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
  daDatViTri.value = true
}

function moBangGio() {
  if (props.disabled || props.readonly || moGio.value) return
  if (!gioHopLe(gio.value) && tach.value.ngay) {
    let macDinh = tach.value.gio || props.defaultTime
    if (props.min && minTach.value?.ngay && tach.value.ngay === minTach.value.ngay && minTach.value.gio && macDinh < minTach.value.gio) {
      macDinh = minTach.value.gio
    }
    gio.value = macDinh
  }
  daDatViTri.value = false
  moGio.value = true
  document.addEventListener('mousedown', onNgoaiGio, true)
  window.addEventListener('resize', datViTriGio)
  window.addEventListener('scroll', datViTriGio, true)
  nextTick(() => {
    datViTriGio()
    cuonToiMucDangChon()
  })
}

function dongGio(traFocus = false) {
  if (!moGio.value) return
  moGio.value = false
  document.removeEventListener('mousedown', onNgoaiGio, true)
  window.removeEventListener('resize', datViTriGio)
  window.removeEventListener('scroll', datViTriGio, true)
  if (traFocus) oGio.value?.focus()
}

onBeforeUnmount(() => dongGio())

function onNgoaiGio(e) {
  if (khungGio.value && !khungGio.value.contains(e.target)) dongGio()
}

function onFocusOutGio(e) {
  if (moGio.value && e.relatedTarget && khungGio.value && !khungGio.value.contains(e.relatedTarget)) dongGio()
}

function onGioKey(e) {
  if (e.key === 'ArrowDown' && !moGio.value) {
    e.preventDefault()
    moBangGio()
  } else if (e.key === 'Escape' && moGio.value) {
    e.preventDefault()
    e.stopPropagation()
    dongGio()
  } else if (e.key === 'Enter' && moGio.value) {
    dongGio()
  }
}
</script>

<template>
  <div class="dti" data-testid="o-ngay-gio">
    <DateInput
      :id="id"
      class="form-control ad-control dti-date"
      :class="{ 'is-invalid': invalid }"
      :model-value="tach.ngay"
      :min="minNgay"
      :max="maxNgay"
      :disabled="disabled"
      :readonly="readonly"
      @update:model-value="doiNgay"
    />
    <div ref="khungGio" class="dti-gio" :class="{ 'is-open': moGio }" @focusout="onFocusOutGio">
      <input
        :id="id ? `${id}-gio` : undefined"
        ref="oGio"
        type="text"
        inputmode="numeric"
        autocomplete="off"
        maxlength="5"
        class="form-control ad-control dti-time"
        :class="{ 'is-invalid': invalid }"
        placeholder="HH:mm"
        aria-label="Giờ (HH:mm)"
        aria-haspopup="dialog"
        :aria-expanded="moGio"
        data-testid="o-gio"
        :value="gio"
        :disabled="disabled"
        :readonly="readonly"
        @input="onGioInput"
        @blur="onGioBlur"
        @keydown="onGioKey"
        @click="moBangGio"
      />
      <button
        type="button"
        class="dti-gio-btn"
        tabindex="-1"
        :disabled="disabled || readonly"
        aria-label="Mở bảng chọn giờ"
        @click="moGio ? dongGio(true) : moBangGio()"
      >
        <i class="bi bi-clock" aria-hidden="true"></i>
      </button>

      <div
        v-if="moGio"
        ref="hopGio"
        class="dti-pop"
        :style="{ visibility: daDatViTri ? 'visible' : 'hidden' }"
        role="dialog"
        aria-label="Chọn giờ"
        data-testid="bang-chon-gio"
        @mousedown.prevent
        @keydown.esc.stop.prevent="dongGio(true)"
      >
        <div class="dti-pop-head">
          <span class="dti-pop-title">Chọn giờ : phút</span>
          <span class="dti-pop-val">{{ gioDangChon.full }}</span>
        </div>

        <div class="dti-presets">
          <button
            v-for="m in MOC_NHANH"
            :key="m"
            type="button"
            class="dti-preset-btn"
            :class="{ 'is-chon': gio === m }"
            :disabled="isMocDisabled(m)"
            :data-moc="m"
            @click="chonMocNhanh(m)"
          >
            {{ m }}
          </button>
        </div>

        <div class="dti-cols">
          <div class="dti-col-wrap">
            <div class="dti-col-label">Giờ</div>
            <div ref="cotGio" class="dti-col">
              <button
                v-for="h in DANH_SACH_GIO"
                :key="h"
                type="button"
                class="dti-item"
                :class="{ 'is-chon': gioDangChon.hh === h }"
                :disabled="isGioDisabled(h)"
                :data-gio="h"
                @click="chonGio(h)"
              >
                {{ h }}
              </button>
            </div>
          </div>
          <div class="dti-col-wrap">
            <div class="dti-col-label">Phút</div>
            <div ref="cotPhut" class="dti-col">
              <button
                v-for="m in DANH_SACH_PHUT"
                :key="m"
                type="button"
                class="dti-item"
                :class="{ 'is-chon': gioDangChon.mm === m }"
                :disabled="isPhutDisabled(m)"
                :data-phut="m"
                @click="chonPhut(m)"
              >
                {{ m }}
              </button>
            </div>
          </div>
        </div>

        <div class="dti-foot">
          <button type="button" class="dti-link" data-testid="nut-bay-gio" @click="chonBayGioTuBang">Bây giờ</button>
          <button type="button" class="dti-done" @click="dongGio(true)">Xong</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dti {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.dti :deep(.di-wrap) {
  flex: 1 1 auto;
  min-width: 0;
}
.dti-gio {
  position: relative;
  flex: 0 0 7rem;
}
.dti-time {
  padding-left: 0.75rem;
  padding-right: 2.15rem;
  text-align: left;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
}
.dti-gio-btn {
  position: absolute;
  top: 50%;
  right: 0.3rem;
  transform: translateY(-50%);
  width: 1.85rem;
  height: 1.85rem;
  display: inline-grid;
  place-items: center;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #5b6573;
  font-size: 0.9rem;
  cursor: pointer;
}
.dti-gio-btn:hover:not(:disabled),
.dti-gio.is-open .dti-gio-btn {
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-primary, #0977ec);
}
.dti-gio-btn:disabled {
  cursor: default;
  opacity: 0.5;
}

/* Bảng chọn Giờ : Phút */
.dti-pop {
  position: fixed;
  z-index: 2000;
  width: 15.5rem;
  padding: 0.7rem;
  background: #fff;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(16, 24, 40, 0.16);
  font-size: 0.85rem;
  color: var(--fs-text, #18242f);
}
.dti-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid var(--fs-line-soft, #eef1f5);
}
.dti-pop-title {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--fs-muted, #64748b);
}
.dti-pop-val {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--fs-primary, #0977ec);
  background: var(--fs-primary-soft, #e7f1fd);
  padding: 0.12rem 0.5rem;
  border-radius: 6px;
  font-variant-numeric: tabular-nums;
}
.dti-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.28rem;
  margin-bottom: 0.55rem;
}
.dti-preset-btn {
  flex: 1 1 auto;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #334155;
  border-radius: 6px;
  padding: 0.2rem 0.35rem;
  font-size: 0.76rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition: all 0.12s ease;
}
.dti-preset-btn:hover {
  border-color: var(--fs-primary, #0977ec);
  background: var(--fs-hover-bg, #e7f1fd);
  color: var(--fs-primary, #0977ec);
}
.dti-preset-btn.is-chon {
  border-color: var(--fs-primary, #0977ec);
  background: var(--fs-primary, #0977ec);
  color: #fff;
}
.dti-preset-btn:disabled,
.dti-item:disabled {
  opacity: 0.35;
  cursor: not-allowed !important;
  pointer-events: none;
}
.dti-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.45rem;
}
.dti-col-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.dti-col-label {
  text-align: center;
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--fs-muted, #7b8794);
  margin-bottom: 0.25rem;
}
.dti-col {
  height: 10.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0.2rem;
  border: 1px solid #edf1f7;
  border-radius: 9px;
  background: #fafbfc;
  scrollbar-width: thin;
}
.dti-item {
  border: 0;
  border-radius: 6px;
  background: transparent;
  padding: 0.3rem 0;
  text-align: center;
  font-size: 0.84rem;
  font-variant-numeric: tabular-nums;
  color: #1e293b;
  cursor: pointer;
}
.dti-item:hover {
  background: var(--fs-hover-bg, #e7f1fd);
  color: var(--fs-primary, #0977ec);
}
.dti-item.is-chon {
  background: var(--fs-primary, #0977ec);
  color: #fff;
  font-weight: 600;
}
.dti-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.55rem;
  padding-top: 0.45rem;
  border-top: 1px solid var(--fs-line-soft, #eef1f5);
}
.dti-link {
  border: 0;
  background: transparent;
  padding: 0.22rem 0.5rem;
  border-radius: 7px;
  color: var(--fs-primary, #0977ec);
  font-weight: 600;
  font-size: 0.82rem;
  cursor: pointer;
}
.dti-link:hover {
  background: var(--fs-hover-bg, #e7f1fd);
}
.dti-done {
  border: 0;
  background: var(--fs-primary, #0977ec);
  color: #fff;
  padding: 0.24rem 0.7rem;
  border-radius: 7px;
  font-weight: 600;
  font-size: 0.82rem;
  cursor: pointer;
}
.dti-done:hover {
  filter: brightness(0.95);
}
</style>
