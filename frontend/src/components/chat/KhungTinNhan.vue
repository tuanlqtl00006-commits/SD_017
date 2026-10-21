<script setup>
// Khung hiện tin nhắn dùng chung (chat hỗ trợ khách ở cả 2 phía + chat nội bộ).
// Tin của mình nằm bên phải; tin hệ thống ở giữa; chia đoạn theo ngày; tự cuộn xuống khi có tin mới nếu đang ở cuối.
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { avatarColors, getInitials } from '../../utils/format'
import { chuanBiHienThi, gioPhut, NGUOI_GUI } from '../../utils/chat'

const props = defineProps({
  tinNhan: { type: Array, default: () => [] },
  laCuaToi: { type: Function, default: () => false },
  hienTen: { type: Boolean, default: true },
  dangGo: { type: String, default: '' },
  rong: { type: String, default: 'Chưa có tin nhắn nào.' },
  dangTai: { type: Boolean, default: false },
})

const khung = ref(null)
const oCuoi = ref(true)
const coTinMoi = ref(false)

const dong = computed(() => chuanBiHienThi(props.tinNhan, props.laCuaToi))

function canCuoi() {
  const el = khung.value
  return !el || el.scrollHeight - el.scrollTop - el.clientHeight < 80
}
function onCuon() {
  oCuoi.value = canCuoi()
  if (oCuoi.value) coTinMoi.value = false
}
async function cuonXuong(muot = false) {
  await nextTick()
  const el = khung.value
  if (!el) return
  if (typeof el.scrollTo === 'function') el.scrollTo({ top: el.scrollHeight, behavior: muot ? 'smooth' : 'auto' })
  else el.scrollTop = el.scrollHeight
  oCuoi.value = true
  coTinMoi.value = false
}

watch(
  () => [props.tinNhan.length, props.tinNhan[props.tinNhan.length - 1]?.id, props.dangGo],
  ([dai], [daiCu]) => {
    const cuoi = props.tinNhan[props.tinNhan.length - 1]
    if (!daiCu || oCuoi.value || (cuoi && props.laCuaToi(cuoi))) cuonXuong(!!daiCu)
    else if (dai > daiCu) coTinMoi.value = true
  },
)
onMounted(() => cuonXuong())
defineExpose({ cuonXuong })

function kieuAvatar(t) {
  return t.loaiNguoiGui === NGUOI_GUI.TRO_LY_AO ? {} : avatarColors(t.tenNguoiGui || '')
}
</script>

<template>
  <div class="ktn">
    <div ref="khung" class="ktn-cuon" role="log" aria-live="polite" aria-relevant="additions" @scroll.passive="onCuon">
      <div v-if="dangTai && !tinNhan.length" class="ktn-trong">Đang tải tin nhắn…</div>
      <div v-else-if="!tinNhan.length" class="ktn-trong">
        <i class="bi bi-chat-square-text" aria-hidden="true"></i>
        <span>{{ rong }}</span>
      </div>
      <template v-for="d in dong" :key="d.khoa">
        <div v-if="d.loai === 'ngay'" class="ktn-ngay"><span>{{ d.nhan }}</span></div>
        <div v-else-if="d.loai === 'he-thong'" class="ktn-he-thong">
          <span>{{ d.tin.noiDung }}</span>
          <time :datetime="d.tin.thoiGian">{{ gioPhut(d.tin.thoiGian) }}</time>
        </div>
        <div v-else class="ktn-dong" :class="{ 'is-toi': d.cuaToi, 'is-noi-tiep': d.noiTiep }">
          <div v-if="!d.cuaToi" class="ktn-avatar" :style="kieuAvatar(d.tin)" :class="{ 'is-an': d.noiTiep, 'is-bot': d.tin.loaiNguoiGui === NGUOI_GUI.TRO_LY_AO }" aria-hidden="true">
            <i v-if="d.tin.loaiNguoiGui === NGUOI_GUI.TRO_LY_AO" class="bi bi-robot"></i>
            <template v-else>{{ getInitials(d.tin.tenNguoiGui || '?') }}</template>
          </div>
          <div class="ktn-than">
            <div v-if="hienTen && !d.cuaToi && !d.noiTiep" class="ktn-ten">{{ d.tin.tenNguoiGui || 'Khách' }}</div>
            <div class="ktn-bong" :class="{ 'is-bot': d.tin.loaiNguoiGui === NGUOI_GUI.TRO_LY_AO }">{{ d.tin.noiDung }}</div>
            <time class="ktn-gio" :datetime="d.tin.thoiGian">{{ gioPhut(d.tin.thoiGian) }}</time>
          </div>
        </div>
      </template>
      <div v-if="dangGo" class="ktn-dong">
        <div class="ktn-avatar is-bot" aria-hidden="true"><i class="bi bi-robot"></i></div>
        <div class="ktn-than">
          <div class="ktn-bong is-bot ktn-dang-go"><span class="ktn-cham"></span><span class="ktn-cham"></span><span class="ktn-cham"></span> {{ dangGo }}</div>
        </div>
      </div>
    </div>
    <button v-if="coTinMoi" type="button" class="ktn-moi" @click="cuonXuong(true)"><i class="bi bi-arrow-down" aria-hidden="true"></i> Có tin nhắn mới</button>
  </div>
</template>

<style scoped>
.ktn {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.ktn-cuon {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 8px;
  background: var(--fs-surface);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ktn-trong {
  margin: auto;
  color: var(--fs-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  padding: 24px;
}
.ktn-trong .bi {
  font-size: 2rem;
  opacity: 0.6;
}
.ktn-ngay {
  text-align: center;
  margin: 12px 0 8px;
}
.ktn-ngay span {
  font-size: 0.78rem;
  color: var(--fs-muted);
  background: #fff;
  border: 1px solid var(--fs-line);
  border-radius: 999px;
  padding: 2px 10px;
}
.ktn-he-thong {
  align-self: center;
  max-width: 85%;
  text-align: center;
  font-size: 0.82rem;
  color: var(--fs-text-2);
  background: #eef2f7;
  border-radius: 10px;
  padding: 6px 12px;
  margin: 8px 0;
}
.ktn-he-thong time {
  margin-left: 6px;
  color: var(--fs-muted);
  font-size: 0.75rem;
}
.ktn-dong {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-top: 10px;
  max-width: 100%;
}
.ktn-dong.is-noi-tiep {
  margin-top: 2px;
}
.ktn-dong.is-toi {
  justify-content: flex-end;
}
.ktn-avatar {
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.75rem;
  font-weight: 700;
}
.ktn-avatar.is-bot {
  background: #ede9fe;
  color: #6d28d9;
  font-size: 1rem;
}
.ktn-avatar.is-an {
  visibility: hidden;
}
.ktn-than {
  display: flex;
  flex-direction: column;
  max-width: min(75%, 560px);
  min-width: 0;
}
.is-toi .ktn-than {
  align-items: flex-end;
}
.ktn-ten {
  font-size: 0.78rem;
  color: var(--fs-muted);
  margin: 0 0 2px 4px;
}
.ktn-bong {
  background: #fff;
  border: 1px solid var(--fs-line);
  color: var(--fs-text);
  border-radius: 16px 16px 16px 4px;
  padding: 8px 12px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.45;
}
.ktn-bong.is-bot {
  background: #faf8ff;
  border-color: #e4dcfb;
}
.is-toi .ktn-bong {
  background: var(--fs-primary);
  border-color: var(--fs-primary);
  color: #fff;
  border-radius: 16px 16px 4px 16px;
}
.ktn-gio {
  font-size: 0.72rem;
  color: var(--fs-muted);
  margin: 2px 6px 0;
}
.ktn-dang-go {
  color: var(--fs-text-2);
  font-style: italic;
}
.ktn-cham {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 2px;
  border-radius: 50%;
  background: #a78bfa;
  animation: ktn-nhay 1.2s infinite ease-in-out;
}
.ktn-cham:nth-child(2) {
  animation-delay: 0.15s;
}
.ktn-cham:nth-child(3) {
  animation-delay: 0.3s;
  margin-right: 6px;
}
@keyframes ktn-nhay {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}
@media (prefers-reduced-motion: reduce) {
  .ktn-cham { animation: none; }
}
.ktn-moi {
  position: absolute;
  left: 50%;
  bottom: 12px;
  transform: translateX(-50%);
  border: 0;
  border-radius: 999px;
  background: var(--fs-navy);
  color: #fff;
  font-size: 0.82rem;
  padding: 6px 14px;
  box-shadow: 0 4px 12px rgba(24, 36, 47, 0.25);
}
</style>
