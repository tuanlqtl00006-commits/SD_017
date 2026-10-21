<script setup>
// Chat nội bộ giữa nhân viên: nhóm chung "Cả cửa hàng" (ai cũng ở trong), chat riêng 1-1 và nhóm tự tạo (vd nhóm ca tối).
// Hỏi lại máy chủ định kỳ: danh sách 5 giây, cuộc đang mở 3 giây.
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import KhungTinNhan from './KhungTinNhan.vue'
import OSoanTin from './OSoanTin.vue'
import { chatNoiBoApi } from '../../services/chatService'
import { useAuth } from '../../composables/useAuth'
import { useChatChuaDoc } from '../../composables/useChatChuaDoc'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { avatarColors, getInitials } from '../../utils/format'
import { normalizeText as boDau } from '../../utils/text'
import { gioNgan, gopTin, idCuoi, locHoiThoai, NGUOI_GUI, xemTruocTinCuoi } from '../../utils/chat'

const LOAI_RIENG = 2

const auth = useAuth()
const toast = useToast()
const chuaDoc = useChatChuaDoc()

const ds = ref([])
const dangTaiDs = ref(true)
const tuKhoa = ref('')
const dangMo = ref(null)
const tin = ref([])
const dangTaiTin = ref(false)
const noiDung = ref('')
const dangGui = ref(false)
const loiGui = ref('')
const oSoan = ref(null)

const idToi = computed(() => auth.trangThai.nguoiDung?.id ?? null)
const dsHien = computed(() => locHoiThoai(ds.value, tuKhoa.value, boDau))
const laCuaToi = (t) => t.loaiNguoiGui === NGUOI_GUI.NHAN_VIEN && t.idNhanVien === idToi.value
const laRieng = (h) => h?.loai === LOAI_RIENG

function nguoiKia(h) {
  return (h?.thanhVien || []).find((n) => n.id !== idToi.value) ?? null
}
function moTaThanhVien(h) {
  if (!h) return ''
  if (laRieng(h)) return nguoiKia(h)?.vaiTro || ''
  const tv = h.thanhVien || []
  const ten = tv.map((n) => (n.id === idToi.value ? 'Bạn' : n.hoTen.split(' ').slice(-1)[0]))
  return `${tv.length} thành viên: ${ten.slice(0, 6).join(', ')}${ten.length > 6 ? '…' : ''}`
}

/* ---------- danh sách ---------- */

let dangHoiDs = false
async function taiDs() {
  if (dangHoiDs) return
  dangHoiDs = true
  try {
    const moi = await chatNoiBoApi.danhSach()
    // cuộc đang mở coi như đã đọc (tin mới sẽ được đánh dấu ở lần hỏi tin kế tiếp)
    ds.value = moi.map((h) => (h.id === dangMo.value?.id ? { ...h, chuaDoc: 0 } : h))
    if (!dangMo.value && ds.value.length && window.innerWidth >= 992) mo(ds.value[0])
  } catch (e) {
    if (dangTaiDs.value) toast.error(e.message || 'Không tải được danh sách chat.')
  } finally {
    dangTaiDs.value = false
    dangHoiDs = false
  }
}

/* ---------- cuộc đang mở ---------- */

function apDung(kq, thay = false) {
  const { tinNhan, ...thongTin } = kq
  dangMo.value = thongTin
  tin.value = thay ? tinNhan || [] : gopTin(tin.value, tinNhan || [])
  const coTrongDs = ds.value.some((h) => h.id === thongTin.id)
  ds.value = coTrongDs ? ds.value.map((h) => (h.id === thongTin.id ? { ...h, ...thongTin, chuaDoc: 0 } : h)) : [...ds.value, { ...thongTin, chuaDoc: 0 }]
}

async function mo(h) {
  if (dangMo.value?.id === h.id) return
  dangMo.value = { ...h }
  tin.value = []
  noiDung.value = ''
  loiGui.value = ''
  dangTaiTin.value = true
  try {
    apDung(await chatNoiBoApi.xem(h.id), true)
    chuaDoc.taiLai()
    setTimeout(() => oSoan.value?.focus(), 50)
  } catch (e) {
    toast.error(e.message || 'Không mở được cuộc trò chuyện.')
  } finally {
    dangTaiTin.value = false
  }
}

let dangHoiTin = false
async function taiTinMoi() {
  const h = dangMo.value
  if (!h || dangHoiTin || dangGui.value || dangTaiTin.value) return
  dangHoiTin = true
  try {
    const truoc = tin.value.length
    const kq = await chatNoiBoApi.xem(h.id, idCuoi(tin.value))
    if (dangMo.value?.id !== h.id) return
    apDung(kq)
    if (tin.value.length > truoc) chuaDoc.taiLai()
  } catch {
    // thử lại lần sau
  } finally {
    dangHoiTin = false
  }
}

async function gui(text) {
  const h = dangMo.value
  if (!h || dangGui.value) return
  dangGui.value = true
  loiGui.value = ''
  try {
    const t = await chatNoiBoApi.gui(h.id, text)
    noiDung.value = ''
    tin.value = gopTin(tin.value, [t])
    ds.value = ds.value.map((x) => (x.id === h.id ? { ...x, tinCuoi: t, ngayCapNhat: t.thoiGian } : x))
  } catch (e) {
    loiGui.value = e.loiTheoTruong?.noiDung || e.message || 'Chưa gửi được tin nhắn.'
  } finally {
    dangGui.value = false
    oSoan.value?.focus()
  }
}

/* ---------- chat riêng / tạo nhóm ---------- */

const nhanViens = ref([])
const timNv = ref('')
const nvHien = computed(() => {
  const k = boDau(timNv.value.trim())
  return k ? nhanViens.value.filter((n) => boDau(`${n.hoTen} ${n.ma} ${n.vaiTro || ''}`).includes(k)) : nhanViens.value
})
async function taiNhanVien() {
  if (nhanViens.value.length) return
  try {
    nhanViens.value = await chatNoiBoApi.nhanVien()
  } catch (e) {
    toast.error(e.message || 'Không tải được danh sách nhân viên.')
  }
}

const moChonRieng = ref(false)
async function batDauRieng() {
  timNv.value = ''
  moChonRieng.value = true
  await taiNhanVien()
}
const dangMoRieng = ref(false)
async function chatRieng(nv) {
  if (dangMoRieng.value) return
  dangMoRieng.value = true
  try {
    const kq = await chatNoiBoApi.moRieng(nv.id)
    moChonRieng.value = false
    tin.value = []
    apDung(kq, true)
    taiDs()
    setTimeout(() => oSoan.value?.focus(), 50)
  } catch (e) {
    toast.error(e.message || 'Không mở được chat riêng.')
  } finally {
    dangMoRieng.value = false
  }
}

const moTaoNhom = ref(false)
const nhom = reactive({ tieuDe: '', chon: [] })
const dangTaoNhom = ref(false)
const loiNhom = useLoiForm(() => {
  const e = {}
  const t = nhom.tieuDe.trim()
  if (!t) e.tieuDe = 'Hãy đặt tên nhóm.'
  else if (t.length > 100) e.tieuDe = 'Tên nhóm tối đa 100 ký tự.'
  if (!nhom.chon.length) e.idNhanViens = 'Hãy chọn ít nhất 1 nhân viên khác.'
  return e
})
async function batDauTaoNhom() {
  nhom.tieuDe = ''
  nhom.chon = []
  timNv.value = ''
  loiNhom.datLai()
  moTaoNhom.value = true
  await taiNhanVien()
}
function doiChon(id) {
  nhom.chon = nhom.chon.includes(id) ? nhom.chon.filter((x) => x !== id) : [...nhom.chon, id]
  loiNhom.sua('idNhanViens')
}
async function taoNhom() {
  if (dangTaoNhom.value || !loiNhom.batDauGui()) return
  dangTaoNhom.value = true
  try {
    const kq = await chatNoiBoApi.taoNhom(nhom.tieuDe.trim(), nhom.chon)
    moTaoNhom.value = false
    tin.value = []
    apDung(kq, true)
    toast.success(`Đã tạo nhóm "${kq.tieuDe}".`)
    taiDs()
  } catch (e) {
    if (!loiNhom.datLoiMayChu(e)) toast.error(e.message || 'Không tạo được nhóm.')
  } finally {
    dangTaoNhom.value = false
  }
}

let henDs = null
let henTin = null
const an = () => typeof document !== 'undefined' && document.visibilityState === 'hidden'
onMounted(() => {
  taiDs()
  henDs = setInterval(() => !an() && taiDs(), 5000)
  henTin = setInterval(() => !an() && taiTinMoi(), 3000)
})
onBeforeUnmount(() => {
  clearInterval(henDs)
  clearInterval(henTin)
})
</script>

<template>
  <div class="ad-page">
    <section class="ad-card cnb" :class="{ 'is-dang-mo': dangMo }">
      <aside class="cnb-ds" aria-label="Danh sách cuộc trò chuyện">
        <header class="cnb-ds-dau">
          <div class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-chat-dots"></i></span>
            <h2 class="ad-card-title">Chat nội bộ</h2>
          </div>
          <div class="cnb-ds-nut">
            <button type="button" class="ad-btn ad-btn-sm" title="Chat riêng với một nhân viên" @click="batDauRieng"><i class="bi bi-person-plus" aria-hidden="true"></i> Chat riêng</button>
            <button type="button" class="ad-btn ad-btn-sm" title="Tạo nhóm chat" @click="batDauTaoNhom"><i class="bi bi-people" aria-hidden="true"></i> Tạo nhóm</button>
          </div>
        </header>
        <div class="cnb-tim ad-affix">
          <i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="tuKhoa" class="form-control ad-control" type="search" placeholder="Tìm cuộc trò chuyện…" aria-label="Tìm cuộc trò chuyện" />
        </div>
        <ul class="cnb-muc-ds">
          <li v-if="dangTaiDs" class="cnb-trong">Đang tải…</li>
          <li v-else-if="!dsHien.length" class="cnb-trong">Không có cuộc trò chuyện nào.</li>
          <li v-for="h in dsHien" :key="h.id">
            <button type="button" class="cnb-muc" :class="{ active: dangMo?.id === h.id, 'is-chua-doc': h.chuaDoc > 0 }" @click="mo(h)">
              <span v-if="laRieng(h)" class="cnb-avatar" :style="avatarColors(h.tieuDe || '')" aria-hidden="true">{{ getInitials(h.tieuDe || '?') }}</span>
              <span v-else class="cnb-avatar is-nhom" :class="{ 'is-chung': h.nhomChung }" aria-hidden="true"><i class="bi" :class="h.nhomChung ? 'bi-shop' : 'bi-people-fill'"></i></span>
              <span class="cnb-muc-than">
                <span class="cnb-dong1">
                  <strong class="cnb-ten">{{ h.tieuDe }}</strong>
                  <small class="cnb-gio">{{ gioNgan(h.tinCuoi?.thoiGian || h.ngayCapNhat) }}</small>
                </span>
                <span class="cnb-dong2">
                  <span class="cnb-xem-truoc">{{ xemTruocTinCuoi(h.tinCuoi, idToi, !laRieng(h)) }}</span>
                  <span v-if="h.chuaDoc > 0" class="cnb-so" :aria-label="`${h.chuaDoc} tin chưa đọc`">{{ h.chuaDoc > 99 ? '99+' : h.chuaDoc }}</span>
                </span>
              </span>
            </button>
          </li>
        </ul>
      </aside>

      <div class="cnb-chat">
        <div v-if="!dangMo" class="cnb-chua-chon">
          <i class="bi bi-chat-square-dots" aria-hidden="true"></i>
          <strong>Chọn một cuộc trò chuyện</strong>
          <span>Nhắn cả cửa hàng ở nhóm chung, hoặc bấm <b>Chat riêng</b> / <b>Tạo nhóm</b>.</span>
        </div>
        <template v-else>
          <header class="cnb-chat-dau">
            <button type="button" class="ad-icon-btn cnb-quay-lai" aria-label="Quay lại danh sách" @click="dangMo = null"><i class="bi bi-arrow-left" aria-hidden="true"></i></button>
            <span v-if="laRieng(dangMo)" class="cnb-avatar" :style="avatarColors(dangMo.tieuDe || '')" aria-hidden="true">{{ getInitials(dangMo.tieuDe || '?') }}</span>
            <span v-else class="cnb-avatar is-nhom" :class="{ 'is-chung': dangMo.nhomChung }" aria-hidden="true"><i class="bi" :class="dangMo.nhomChung ? 'bi-shop' : 'bi-people-fill'"></i></span>
            <div class="cnb-chat-ten">
              <strong>{{ dangMo.tieuDe }}</strong>
              <small :title="(dangMo.thanhVien || []).map((n) => n.hoTen).join(', ')">{{ moTaThanhVien(dangMo) }}</small>
            </div>
          </header>
          <KhungTinNhan :tin-nhan="tin" :la-cua-toi="laCuaToi" :hien-ten="!laRieng(dangMo)" :dang-tai="dangTaiTin" rong="Chưa có tin nhắn. Gửi lời chào đầu tiên nhé!" />
          <OSoanTin ref="oSoan" v-model="noiDung" :dang-gui="dangGui" :loi="loiGui" :placeholder="`Nhắn ${dangMo.tieuDe}…`" @gui="gui" @go="loiGui = ''" />
        </template>
      </div>
    </section>

    <!-- Chọn người chat riêng -->
    <BaseModal v-if="moChonRieng" title="Chat riêng với nhân viên" @close="moChonRieng = false">
      <div class="ad-affix mb-2"><i class="bi bi-search" aria-hidden="true"></i><input v-model="timNv" class="form-control ad-control" type="search" placeholder="Tìm theo tên, mã nhân viên…" aria-label="Tìm nhân viên" /></div>
      <ul class="cnb-chon-ds">
        <li v-if="!nvHien.length" class="cnb-trong">Không có nhân viên phù hợp.</li>
        <li v-for="n in nvHien" :key="n.id">
          <button type="button" class="cnb-chon" :disabled="dangMoRieng" @click="chatRieng(n)">
            <span class="cnb-avatar" :style="avatarColors(n.hoTen)" aria-hidden="true">{{ getInitials(n.hoTen) }}</span>
            <span><strong>{{ n.hoTen }}</strong><br /><small class="text-muted">{{ n.ma }} · {{ n.vaiTro }}</small></span>
            <i class="bi bi-chat-dots ms-auto" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
    </BaseModal>

    <!-- Tạo nhóm -->
    <BaseModal v-if="moTaoNhom" title="Tạo nhóm chat" @close="moTaoNhom = false">
      <form id="cnb-form-nhom" novalidate @submit.prevent="taoNhom">
        <div class="mb-3">
          <label class="ad-label" for="cnb-ten-nhom">Tên nhóm <span class="text-danger">*</span></label>
          <input id="cnb-ten-nhom" v-model="nhom.tieuDe" class="form-control ad-control" :class="{ 'is-invalid': loiNhom.loi.value.tieuDe }" maxlength="100"
                 placeholder="Ví dụ: Ca tối, Kho hàng…" @input="loiNhom.sua('tieuDe')" @blur="loiNhom.cham('tieuDe')" />
          <div v-if="loiNhom.loi.value.tieuDe" class="ad-error">{{ loiNhom.loi.value.tieuDe }}</div>
        </div>
        <label class="ad-label">Thành viên <span class="text-danger">*</span> <small class="text-muted">(đã chọn {{ nhom.chon.length }}, bạn tự có trong nhóm)</small></label>
        <div class="ad-affix mb-2"><i class="bi bi-search" aria-hidden="true"></i><input v-model="timNv" class="form-control ad-control" type="search" placeholder="Tìm nhân viên…" aria-label="Tìm nhân viên" /></div>
        <ul class="cnb-chon-ds" :class="{ 'is-invalid': loiNhom.loi.value.idNhanViens }">
          <li v-if="!nvHien.length" class="cnb-trong">Không có nhân viên phù hợp.</li>
          <li v-for="n in nvHien" :key="n.id">
            <label class="cnb-chon">
              <input type="checkbox" class="form-check-input" :checked="nhom.chon.includes(n.id)" @change="doiChon(n.id)" />
              <span class="cnb-avatar" :style="avatarColors(n.hoTen)" aria-hidden="true">{{ getInitials(n.hoTen) }}</span>
              <span><strong>{{ n.hoTen }}</strong><br /><small class="text-muted">{{ n.ma }} · {{ n.vaiTro }}</small></span>
            </label>
          </li>
        </ul>
        <div v-if="loiNhom.loi.value.idNhanViens" class="ad-error">{{ loiNhom.loi.value.idNhanViens }}</div>
      </form>
      <template #footer>
        <button type="button" class="ad-btn" @click="moTaoNhom = false">Hủy</button>
        <button type="submit" form="cnb-form-nhom" class="ad-btn ad-btn-primary" :disabled="dangTaoNhom">
          <span v-if="dangTaoNhom" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          <i v-else class="bi bi-people" aria-hidden="true"></i> Tạo nhóm
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.cnb {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  height: max(560px, calc(100vh / var(--ui-zoom, 1) - 150px));
  padding: 0;
  overflow: hidden;
}
.cnb-ds {
  border-right: 1px solid var(--fs-line);
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.cnb-ds-dau {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 16px 10px;
}
.cnb-ds-nut {
  display: flex;
  gap: 6px;
}
.cnb-ds-nut .ad-btn {
  flex: 1 1 0;
  justify-content: center;
}
.cnb-tim {
  margin: 0 16px 10px;
}
.cnb-tim .ad-control {
  height: 40px;
}
.cnb-muc-ds,
.cnb-chon-ds {
  list-style: none;
  margin: 0;
  padding: 0 8px 8px;
  overflow-y: auto;
  flex: 1 1 auto;
  min-height: 0;
}
.cnb-chon-ds {
  max-height: 360px;
  padding: 0;
  border: 1px solid var(--fs-line);
  border-radius: 10px;
}
.cnb-chon-ds.is-invalid {
  border-color: var(--fs-danger);
}
.cnb-trong {
  color: var(--fs-muted);
  text-align: center;
  padding: 24px 12px;
}
.cnb-muc,
.cnb-chon {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  text-align: left;
  border: 0;
  background: transparent;
  border-radius: 10px;
  padding: 10px 8px;
  cursor: pointer;
  margin: 0;
}
.cnb-muc:hover,
.cnb-chon:hover {
  background: var(--fs-hover-row);
}
.cnb-muc.active {
  background: var(--fs-primary-soft);
}
.cnb-avatar {
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 0.85rem;
}
.cnb-avatar.is-nhom {
  background: #e0f2fe;
  color: #0369a1;
  font-size: 1.05rem;
}
.cnb-avatar.is-chung {
  background: var(--fs-primary);
  color: #fff;
}
.cnb-muc-than {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1 1 auto;
}
.cnb-dong1,
.cnb-dong2 {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.cnb-ten {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cnb-gio {
  margin-left: auto;
  color: var(--fs-muted);
  white-space: nowrap;
}
.cnb-xem-truoc {
  color: var(--fs-text-2);
  font-size: 0.86rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1 1 auto;
}
.is-chua-doc .cnb-xem-truoc {
  color: var(--fs-text);
  font-weight: 600;
}
.cnb-so {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #e5484d;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}
.cnb-chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
}
.cnb-chua-chon {
  margin: auto;
  max-width: 360px;
  text-align: center;
  color: var(--fs-text-2);
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  padding: 24px;
}
.cnb-chua-chon .bi {
  font-size: 2.4rem;
  color: var(--fs-primary);
  opacity: 0.7;
}
.cnb-chat-dau {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--fs-line);
}
.cnb-quay-lai {
  display: none;
}
.cnb-chat-ten {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.cnb-chat-ten small {
  color: var(--fs-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 991.98px) {
  .cnb {
    grid-template-columns: minmax(0, 1fr);
  }
  .cnb.is-dang-mo .cnb-ds,
  .cnb:not(.is-dang-mo) .cnb-chat {
    display: none;
  }
  .cnb-quay-lai {
    display: inline-flex;
  }
}
</style>
