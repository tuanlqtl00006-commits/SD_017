<script setup>
// Ô soạn tin: Enter để gửi, Shift + Enter xuống dòng, tự cao lên theo nội dung, đếm ký tự khi gần giới hạn.
import { computed, nextTick, ref, watch } from 'vue'
import { DO_DAI_TOI_DA } from '../../utils/chat'

const props = defineProps({
  disabled: { type: Boolean, default: false },
  dangGui: { type: Boolean, default: false },
  placeholder: { type: String, default: 'Nhập tin nhắn…' },
  lyDoKhoa: { type: String, default: '' },
  loi: { type: String, default: '' },
})
const emit = defineEmits(['gui', 'go'])
const noiDung = defineModel({ type: String, default: '' })
const o = ref(null)

const dai = computed(() => noiDung.value.trim().length)
const quaDai = computed(() => dai.value > DO_DAI_TOI_DA)
const guiDuoc = computed(() => !props.disabled && !props.dangGui && dai.value > 0 && !quaDai.value)

function coGian() {
  const el = o.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 160)}px`
}
watch(noiDung, () => nextTick(coGian))

function gui() {
  if (!guiDuoc.value) return
  emit('gui', noiDung.value.trim())
}
function onPhim(e) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    gui()
  }
}
function focus() {
  o.value?.focus()
}
defineExpose({ focus })
</script>

<template>
  <div class="ost">
    <div v-if="lyDoKhoa" class="ost-khoa"><i class="bi bi-lock" aria-hidden="true"></i> {{ lyDoKhoa }}</div>
    <div v-else class="ost-hang">
      <textarea
        ref="o"
        v-model="noiDung"
        class="form-control ad-control ost-o"
        :class="{ 'is-invalid': quaDai || loi }"
        rows="1"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="DO_DAI_TOI_DA + 200"
        aria-label="Nội dung tin nhắn"
        @keydown="onPhim"
        @input="emit('go')"
      ></textarea>
      <button type="button" class="ad-btn ad-btn-primary ost-gui" :disabled="!guiDuoc" title="Gửi (Enter)" aria-label="Gửi tin nhắn" @click="gui">
        <span v-if="dangGui" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <i v-else class="bi bi-send-fill" aria-hidden="true"></i>
      </button>
    </div>
    <div v-if="!lyDoKhoa" class="ost-duoi">
      <span v-if="loi" class="ad-error">{{ loi }}</span>
      <span v-else-if="quaDai" class="ad-error">Tin nhắn tối đa {{ DO_DAI_TOI_DA }} ký tự (đang có {{ dai }}).</span>
      <span v-else class="ost-goi-y">Enter để gửi · Shift + Enter xuống dòng</span>
      <span v-if="dai > DO_DAI_TOI_DA - 200" class="ost-dem" :class="{ 'is-qua': quaDai }">{{ dai }}/{{ DO_DAI_TOI_DA }}</span>
    </div>
  </div>
</template>

<style scoped>
.ost {
  border-top: 1px solid var(--fs-line);
  background: #fff;
  padding: 10px 12px 6px;
}
.ost-hang {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
textarea.ost-o {
  flex: 1 1 auto;
  resize: none;
  min-height: 44px;
  max-height: 160px;
  line-height: 1.4;
  padding-top: 0.6rem;
  padding-bottom: 0.6rem;
}
.ost-gui {
  flex: 0 0 auto;
  width: 46px;
  height: 44px;
  justify-content: center;
  padding: 0;
}
.ost-duoi {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  min-height: 18px;
  margin-top: 4px;
  font-size: 0.75rem;
}
.ost-goi-y {
  color: var(--fs-muted);
}
.ost-dem {
  color: var(--fs-muted);
  margin-left: auto;
}
.ost-dem.is-qua {
  color: var(--fs-danger);
  font-weight: 600;
}
.ost-khoa {
  color: var(--fs-text-2);
  background: var(--fs-surface);
  border: 1px dashed var(--fs-line);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.88rem;
  margin-bottom: 6px;
}
</style>
