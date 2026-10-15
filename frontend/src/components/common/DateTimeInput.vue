<script setup>
// Ô chọn NGÀY + GIỜ dùng chung (phiếu / đợt giảm giá): ngày bấm lịch hoặc gõ dd/mm/yyyy, giờ gõ HH:mm (tự chèn dấu ':').
// v-model là 'yyyy-MM-ddTHH:mm' (hoặc '' khi chưa chọn ngày). Chọn ngày mà chưa có giờ thì lấy giờ mặc định (defaultTime):
// '00:00' cho ngày bắt đầu, '23:59' cho ngày kết thúc. min / max chỉ khóa phần ngày trên lịch; giờ do form kiểm tra.
import { computed, ref, watch } from 'vue'
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

const tach = computed(() => tachNgayGio(props.modelValue))
const gio = ref(tach.value.gio)
const minNgay = computed(() => (props.min ? tachNgayGio(props.min).ngay || undefined : undefined))
const maxNgay = computed(() => (props.max ? tachNgayGio(props.max).ngay || undefined : undefined))

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
  const g = gioHopLe(gio.value) ? gio.value : tach.value.gio || props.defaultTime
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
    <div class="dti-gio">
      <input
        :id="id ? `${id}-gio` : undefined"
        type="text"
        inputmode="numeric"
        autocomplete="off"
        maxlength="5"
        class="form-control ad-control dti-time"
        :class="{ 'is-invalid': invalid }"
        placeholder="HH:mm"
        aria-label="Giờ (HH:mm)"
        data-testid="o-gio"
        :value="gio"
        :disabled="disabled"
        :readonly="readonly"
        @input="onGioInput"
        @blur="onGioBlur"
      />
    </div>
    <button
      v-if="showNow && !disabled && !readonly"
      type="button"
      class="dti-now"
      title="Đặt bằng ngày giờ hiện tại"
      data-testid="nut-bay-gio"
      @click="datBayGio"
    >
      Bây giờ
    </button>
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
  flex: 0 0 5.6rem;
}
.dti-time {
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.dti-now {
  flex: none;
  border: 0;
  background: transparent;
  padding: 0.25rem 0.4rem;
  border-radius: 8px;
  color: var(--fs-primary, #0977ec);
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
}
.dti-now:hover {
  background: var(--fs-hover-bg, #e7f1fd);
}
</style>
