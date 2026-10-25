<script setup>
// Nút "Trợ lý AI" trên header + khung chat bên phải. Component nằm trong MainLayout nên lịch sử chat giữ nguyên khi chuyển trang
// và tự xóa khi đăng xuất.
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { troLyService } from '../../services/troLyService'

const MAX_TIN = 60
const tin = ref([]) // { id, cuaToi, noiDung, goiY, lienKet, loi }
let soTin = 0

const mo = ref(false)
const noiDung = ref('')
const dangGui = ref(false)
const khungTin = ref(null)
const oNhap = ref(null)

function them(t) {
  tin.value = [...tin.value, { id: ++soTin, goiY: [], lienKet: [], ...t }].slice(-MAX_TIN)
  nextTick(() => {
    if (khungTin.value) khungTin.value.scrollTop = khungTin.value.scrollHeight
  })
}

async function moKhung() {
  mo.value = true
  nextTick(() => oNhap.value?.focus())
  if (tin.value.length) return
  dangGui.value = true
  try {
    them(await troLyService.chao())
  } catch (e) {
    them({ noiDung: e.message || 'Chưa kết nối được trợ lý.', loi: true })
  } finally {
    dangGui.value = false
  }
}

async function hoi(cau) {
  const c = String(cau ?? noiDung.value).trim()
  if (!c || dangGui.value) return
  if (c.length > 500) {
    them({ noiDung: 'Câu hỏi tối đa 500 ký tự.', loi: true })
    return
  }
  them({ cuaToi: true, noiDung: c })
  noiDung.value = ''
  dangGui.value = true
  try {
    them(await troLyService.hoi(c))
  } catch (e) {
    them({ noiDung: e.message || 'Trợ lý chưa trả lời được, bạn thử lại nhé.', loi: true })
  } finally {
    dangGui.value = false
    nextTick(() => oNhap.value?.focus())
  }
}

function phim(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    hoi()
  }
}

function dongKhiEsc(e) {
  if (e.key === 'Escape' && mo.value) mo.value = false
}
onMounted(() => document.addEventListener('keydown', dongKhiEsc))
onBeforeUnmount(() => document.removeEventListener('keydown', dongKhiEsc))

const tinCuoi = () => tin.value[tin.value.length - 1]
</script>

<template>
  <button type="button" class="btn btn-light btn-sm border rounded-3 px-3 fw-medium d-flex align-items-center gap-2 tl-nut" data-testid="tro-ly-nut" :aria-expanded="mo" @click="mo ? (mo = false) : moKhung()">
    <i class="bi bi-stars" aria-hidden="true"></i>
    <span class="d-none d-lg-inline">Trợ lý AI</span>
  </button>

  <Teleport to="body">
    <aside v-if="mo" class="tl-khung" role="dialog" aria-label="Trợ lý AI nội bộ" data-testid="tro-ly-khung">
      <header class="tl-dau">
        <span class="tl-bieu-tuong" aria-hidden="true"><i class="bi bi-stars"></i></span>
        <div class="me-auto">
          <strong>Trợ lý AI FootStyle</strong>
          <small>Trả lời từ dữ liệu thật của cửa hàng</small>
        </div>
        <button type="button" class="tl-dong" aria-label="Đóng trợ lý" @click="mo = false"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
      </header>

      <div ref="khungTin" class="tl-tin" aria-live="polite">
        <div v-for="t in tin" :key="t.id" class="tl-dong-tin" :class="{ 'is-toi': t.cuaToi }">
          <div class="tl-bong" :class="{ 'is-loi': t.loi }">{{ t.noiDung }}</div>
          <div v-if="!t.cuaToi && t.lienKet?.length" class="tl-lien-ket">
            <RouterLink v-for="l in t.lienKet" :key="l.duongDan" :to="l.duongDan" class="tl-chip is-link" @click="mo = false">
              <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i> {{ l.nhan }}
            </RouterLink>
          </div>
        </div>
        <div v-if="dangGui" class="tl-dong-tin"><div class="tl-bong tl-dang-go"><span></span><span></span><span></span></div></div>
      </div>

      <div v-if="!dangGui && tinCuoi()?.goiY?.length" class="tl-goi-y" aria-label="Câu hỏi gợi ý">
        <button v-for="g in tinCuoi().goiY" :key="g" type="button" class="tl-chip" @click="hoi(g)">{{ g }}</button>
      </div>

      <form class="tl-soan" @submit.prevent="hoi()">
        <textarea ref="oNhap" v-model="noiDung" rows="1" class="form-control ad-control" placeholder="Hỏi trợ lý, vd: tồn kho Astrox 99…" aria-label="Câu hỏi cho trợ lý" maxlength="500" @keydown="phim"></textarea>
        <button type="submit" class="tl-gui" :disabled="dangGui || !noiDung.trim()" aria-label="Gửi câu hỏi"><i class="bi bi-send-fill" aria-hidden="true"></i></button>
      </form>
    </aside>
  </Teleport>
</template>

<style scoped>
.tl-nut .bi-stars { color: #f59e0b; }
.tl-khung { position: fixed; top: 0; right: 0; z-index: 1060; width: min(420px, calc(100vw / var(--ui-zoom, 1))); height: calc(100vh / var(--ui-zoom, 1)); display: flex; flex-direction: column; background: #fff; border-left: 1px solid var(--fs-line, #e6eaf0); box-shadow: -12px 0 32px rgba(24, 36, 47, 0.14); }
.tl-dau { display: flex; align-items: center; gap: 0.7rem; padding: 0.9rem 1rem; background: var(--fs-primary, #0977ec); color: #fff; }
.tl-dau strong { display: block; font-size: 0.98rem; }
.tl-dau small { opacity: 0.85; font-size: 0.78rem; }
.tl-bieu-tuong { width: 36px; height: 36px; border-radius: 50%; background: rgba(255, 255, 255, 0.18); display: flex; align-items: center; justify-content: center; font-size: 1.1rem; }
.tl-dong { border: 0; background: transparent; color: #fff; font-size: 1.05rem; padding: 0.25rem; }
.tl-tin { flex: 1; overflow-y: auto; padding: 1rem; display: flex; flex-direction: column; gap: 0.6rem; background: #f6f8fb; }
.tl-dong-tin { display: flex; flex-direction: column; align-items: flex-start; gap: 0.35rem; }
.tl-dong-tin.is-toi { align-items: flex-end; }
.tl-bong { max-width: 88%; padding: 0.6rem 0.8rem; border-radius: 14px; background: #fff; border: 1px solid var(--fs-line, #e6eaf0); white-space: pre-line; font-size: 0.9rem; line-height: 1.45; }
.is-toi .tl-bong { background: var(--fs-primary, #0977ec); color: #fff; border-color: transparent; }
.tl-bong.is-loi { background: #fdecec; color: #b42318; border-color: #f5c2c0; }
.tl-lien-ket, .tl-goi-y { display: flex; flex-wrap: wrap; gap: 0.4rem; }
.tl-goi-y { padding: 0.6rem 1rem 0; border-top: 1px solid var(--fs-line-soft, #eef1f5); }
.tl-chip { border: 1px solid var(--fs-primary-line, #bcd8fb); background: var(--fs-primary-soft, #eaf3fe); color: var(--fs-primary-dark, #0759b3); border-radius: 999px; padding: 0.3rem 0.75rem; font-size: 0.8rem; text-decoration: none; }
.tl-chip:hover { background: var(--fs-hover-bg, #dcecfd); }
.tl-soan { display: flex; gap: 0.5rem; padding: 0.75rem 1rem 1rem; }
.tl-soan textarea { resize: none; }
.tl-gui { width: 42px; flex-shrink: 0; border: 0; border-radius: 10px; background: var(--fs-primary, #0977ec); color: #fff; }
.tl-gui:disabled { opacity: 0.5; }
.tl-dang-go { display: flex; gap: 4px; }
.tl-dang-go span { width: 6px; height: 6px; border-radius: 50%; background: #9aa6b2; animation: tl-nhay 1s infinite ease-in-out; }
.tl-dang-go span:nth-child(2) { animation-delay: 0.15s; }
.tl-dang-go span:nth-child(3) { animation-delay: 0.3s; }
@keyframes tl-nhay { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }
@media (prefers-reduced-motion: reduce) { .tl-dang-go span { animation: none; } }
</style>
