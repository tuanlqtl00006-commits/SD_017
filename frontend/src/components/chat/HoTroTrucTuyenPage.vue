<script setup>
// Hỗ trợ khách hàng trực tuyến (nhân viên). Bên trái: các cuộc chat của khách (đang chờ nhân viên, của tôi, trợ lý ảo đang
// trả lời, đã kết thúc). Bên phải: nội dung chat. Trả lời một cuộc đang chờ = tự nhận hỗ trợ; quản lý được tiếp quản cuộc
// của nhân viên khác. Hỏi lại máy chủ định kỳ (danh sách 5 giây, cuộc đang mở 3 giây) nên không cần tải lại trang.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import KhungTinNhan from './KhungTinNhan.vue'
import OSoanTin from './OSoanTin.vue'
import { hoTroApi } from '../../services/chatService'
import { useAuth } from '../../composables/useAuth'
import { useChatChuaDoc } from '../../composables/useChatChuaDoc'
import { useToast } from '../../composables/useToast'
import { avatarColors, getInitials } from '../../utils/format'
import { normalizeText as boDau } from '../../utils/text'
import { CHE_DO, gioNgan, gopTin, idCuoi, locHoiThoai, moTaCheDo, NGUOI_GUI, TRA_LOI_NHANH, xemTruocTinCuoi } from '../../utils/chat'

const auth = useAuth()
const toast = useToast()
const chuaDoc = useChatChuaDoc()

const BO_LOC = [
  { khoa: 'cho', nhan: 'Chờ nhân viên', dem: 'cho' },
  { khoa: 'cua-toi', nhan: 'Của tôi', dem: 'cuaToi' },
  { khoa: 'tro-ly', nhan: 'Trợ lý ảo', dem: 'troLy' },
  { khoa: 'da-dong', nhan: 'Đã kết thúc', dem: 'daDong' },
  { khoa: '', nhan: 'Tất cả', dem: 'tatCa' },
]
const loc = ref('cho')
const tuKhoa = ref('')
const ds = ref([])
const dem = ref({})
const dangTaiDs = ref(true)

const dangMo = ref(null) // thông tin cuộc đang mở (không gồm tin)
const tin = ref([])
const dangTaiTin = ref(false)
const noiDung = ref('')
const dangGui = ref(false)
const loiGui = ref('')
const dangXuLy = ref(false)
const xacNhanDong = ref(false)
const moTraLoiNhanh = ref(false)
const oSoan = ref(null)

const idToi = computed(() => auth.trangThai.nguoiDung?.id ?? null)
const dsHien = computed(() => locHoiThoai(ds.value, tuKhoa.value, boDau))

const cuaNguoiKhac = computed(() => dangMo.value?.cheDo === CHE_DO.NHAN_VIEN_HO_TRO && dangMo.value.idNhanVienPhuTrach !== idToi.value)
const nhanDuoc = computed(() => {
  const h = dangMo.value
  if (!h || h.cheDo === CHE_DO.DA_DONG) return false
  if (h.cheDo === CHE_DO.NHAN_VIEN_HO_TRO) return cuaNguoiKhac.value && auth.laQuanLy.value
  return true
})
const dongDuoc = computed(() => {
  const h = dangMo.value
  return !!h && h.cheDo !== CHE_DO.DA_DONG && (!cuaNguoiKhac.value || auth.laQuanLy.value)
})
const lyDoKhoa = computed(() => {
  const h = dangMo.value
  if (!h) return ''
  if (h.cheDo === CHE_DO.DA_DONG) return 'Cuộc trò chuyện đã kết thúc.'
  if (cuaNguoiKhac.value) {
    return `Đang do ${h.tenNhanVienPhuTrach || 'nhân viên khác'} hỗ trợ.${auth.laQuanLy.value ? ' Bấm "Nhận hỗ trợ" để tiếp quản.' : ''}`
  }
  return ''
})
const goiYSoan = computed(() => {
  const h = dangMo.value
  if (!h) return ''
  if (h.cheDo === CHE_DO.TRO_LY_AO) return 'Trả lời khách (trợ lý ảo sẽ dừng, bạn nhận hỗ trợ cuộc này)…'
  if (h.cheDo === CHE_DO.CHO_NHAN_VIEN) return 'Trả lời để nhận hỗ trợ khách…'
  return `Trả lời ${h.tenKhach}…`
})

const laCuaToi = (t) => t.loaiNguoiGui === NGUOI_GUI.NHAN_VIEN && t.idNhanVien === idToi.value

/* ---------- danh sách ---------- */

let dangHoiDs = false
async function taiDs() {
  if (dangHoiDs) return
  dangHoiDs = true
  try {
    const kq = await hoTroApi.danhSach(loc.value)
    ds.value = kq.hoiThoai || []
    dem.value = kq.dem || {}
  } catch (e) {
    if (dangTaiDs.value) toast.error(e.message || 'Không tải được danh sách hỗ trợ.')
  } finally {
    dangTaiDs.value = false
    dangHoiDs = false
  }
}
function doiLoc(k) {
  loc.value = k
  dangTaiDs.value = true
  ds.value = []
  taiDs()
}

/* ---------- cuộc đang mở ---------- */

function apDung(kq, thay = false) {
  const { tinNhan, ...thongTin } = kq
  dangMo.value = thongTin
  tin.value = thay ? tinNhan || [] : gopTin(tin.value, tinNhan || [])
  // cập nhật dòng tương ứng trong danh sách (bỏ chấm chưa đọc)
  ds.value = ds.value.map((h) => (h.id === thongTin.id ? { ...h, ...thongTin, chuaDoc: 0 } : h))
}

async function mo(h) {
  if (dangMo.value?.id === h.id) return
  dangMo.value = { ...h }
  tin.value = []
  noiDung.value = ''
  loiGui.value = ''
  moTraLoiNhanh.value = false
  dangTaiTin.value = true
  try {
    apDung(await hoTroApi.xem(h.id), true)
    chuaDoc.taiLai()
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
    const kq = await hoTroApi.xem(h.id, idCuoi(tin.value))
    if (dangMo.value?.id !== h.id) return
    apDung(kq)
    if (tin.value.length > truoc) chuaDoc.taiLai()
  } catch {
    // mất kết nối tạm thời: lần sau hỏi lại
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
    const t = await hoTroApi.gui(h.id, text)
    noiDung.value = ''
    tin.value = gopTin(tin.value, [t])
    await taiTinMoi() // lấy tin hệ thống "đã tham gia hỗ trợ" + trạng thái mới
    taiDs()
  } catch (e) {
    loiGui.value = e.loiTheoTruong?.noiDung || e.message || 'Chưa gửi được tin nhắn.'
    if (e.code === 'NGUOI_KHAC_HO_TRO' || e.code === 'DA_KET_THUC') taiTinMoi()
  } finally {
    dangGui.value = false
    oSoan.value?.focus()
  }
}

async function nhan() {
  if (!dangMo.value || dangXuLy.value) return
  dangXuLy.value = true
  try {
    apDung(await hoTroApi.nhan(dangMo.value.id))
    toast.success('Bạn đã nhận hỗ trợ cuộc trò chuyện này.')
    taiDs()
    chuaDoc.taiLai()
    setTimeout(() => oSoan.value?.focus(), 50)
  } catch (e) {
    toast.error(e.message || 'Không nhận được cuộc trò chuyện.')
    taiTinMoi()
  } finally {
    dangXuLy.value = false
  }
}

async function dong() {
  if (!dangMo.value) return
  dangXuLy.value = true
  try {
    apDung(await hoTroApi.dong(dangMo.value.id))
    xacNhanDong.value = false
    toast.success('Đã kết thúc cuộc trò chuyện.')
    taiDs()
    chuaDoc.taiLai()
  } catch (e) {
    toast.error(e.message || 'Không kết thúc được cuộc trò chuyện.')
    xacNhanDong.value = false
  } finally {
    dangXuLy.value = false
  }
}

function chonTraLoiNhanh(c) {
  noiDung.value = noiDung.value ? `${noiDung.value} ${c}` : c
  moTraLoiNhanh.value = false
  oSoan.value?.focus()
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
    <section class="ad-card htt" :class="{ 'is-dang-mo': dangMo }">
      <!-- Danh sách cuộc chat -->
      <aside class="htt-ds" aria-label="Danh sách cuộc trò chuyện">
        <header class="htt-ds-dau">
          <div class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-headset"></i></span>
            <h2 class="ad-card-title">Hỗ trợ khách hàng</h2>
          </div>
          <a href="/ho-tro" target="_blank" rel="noopener" class="htt-link-khach" title="Mở trang chat của khách (để thử)">
            <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i> Trang của khách
          </a>
        </header>
        <div class="htt-loc" role="tablist" aria-label="Lọc cuộc trò chuyện">
          <button v-for="b in BO_LOC" :key="b.khoa" type="button" role="tab" class="htt-loc-nut" :class="{ active: loc === b.khoa }"
                  :aria-selected="loc === b.khoa" @click="doiLoc(b.khoa)">
            {{ b.nhan }}
            <span v-if="dem[b.dem]" class="htt-loc-dem" :class="{ 'is-gap': b.khoa === 'cho' }">{{ dem[b.dem] }}</span>
          </button>
        </div>
        <div class="htt-tim ad-affix">
          <i class="bi bi-search" aria-hidden="true"></i>
          <input v-model="tuKhoa" class="form-control ad-control" type="search" placeholder="Tìm tên, số điện thoại, nội dung…" aria-label="Tìm cuộc trò chuyện" />
        </div>
        <ul class="htt-muc-ds">
          <li v-if="dangTaiDs" class="htt-trong">Đang tải…</li>
          <li v-else-if="!dsHien.length" class="htt-trong">
            <i class="bi bi-inbox" aria-hidden="true"></i>
            {{ tuKhoa ? 'Không có cuộc trò chuyện khớp từ khóa.' : loc === 'cho' ? 'Không có khách nào đang chờ. Tốt lắm!' : 'Chưa có cuộc trò chuyện nào.' }}
          </li>
          <li v-for="h in dsHien" :key="h.id">
            <button type="button" class="htt-muc" :class="{ active: dangMo?.id === h.id, 'is-chua-doc': h.chuaDoc > 0 }" @click="mo(h)">
              <span class="htt-avatar" :style="avatarColors(h.tenKhach || '')" aria-hidden="true">{{ getInitials(h.tenKhach || '?') }}</span>
              <span class="htt-muc-than">
                <span class="htt-muc-dong1">
                  <strong class="htt-ten">{{ h.tenKhach }}</strong>
                  <small class="htt-gio">{{ gioNgan(h.tinCuoi?.thoiGian || h.ngayCapNhat) }}</small>
                </span>
                <span class="htt-muc-dong2">
                  <span class="htt-xem-truoc">{{ xemTruocTinCuoi(h.tinCuoi, idToi) }}</span>
                  <span v-if="h.chuaDoc > 0" class="htt-so" :aria-label="`${h.chuaDoc} tin chưa đọc`">{{ h.chuaDoc > 99 ? '99+' : h.chuaDoc }}</span>
                </span>
                <span class="htt-muc-dong3">
                  <span class="ad-pill" :class="moTaCheDo(h.cheDo).pill">{{ moTaCheDo(h.cheDo).nhan }}</span>
                  <small v-if="h.tenNhanVienPhuTrach" class="text-muted text-truncate">{{ h.idNhanVienPhuTrach === idToi ? 'Bạn' : h.tenNhanVienPhuTrach }}</small>
                  <small class="text-muted ms-auto">{{ h.sdtKhach }}</small>
                </span>
              </span>
            </button>
          </li>
        </ul>
      </aside>

      <!-- Cuộc đang mở -->
      <div class="htt-chat">
        <div v-if="!dangMo" class="htt-chua-chon">
          <i class="bi bi-chat-left-text" aria-hidden="true"></i>
          <strong>Chọn một cuộc trò chuyện</strong>
          <span>Khách bấm "Gặp nhân viên" sẽ hiện ở mục <b>Chờ nhân viên</b>. Trả lời là tự nhận hỗ trợ.</span>
        </div>
        <template v-else>
          <header class="htt-chat-dau">
            <button type="button" class="ad-icon-btn htt-quay-lai" aria-label="Quay lại danh sách" @click="dangMo = null"><i class="bi bi-arrow-left" aria-hidden="true"></i></button>
            <span class="htt-avatar" :style="avatarColors(dangMo.tenKhach || '')" aria-hidden="true">{{ getInitials(dangMo.tenKhach || '?') }}</span>
            <div class="htt-chat-ten">
              <strong>{{ dangMo.tenKhach }}</strong>
              <small>
                <a :href="`tel:${dangMo.sdtKhach}`">{{ dangMo.sdtKhach }}</a>
                <template v-if="dangMo.khachHang"> · <RouterLink :to="`/khach-hang/${dangMo.khachHang.id}/sua`">{{ dangMo.khachHang.ma }}</RouterLink></template>
                <template v-else> · Khách chưa có tài khoản</template>
              </small>
            </div>
            <span class="ad-pill" :class="moTaCheDo(dangMo.cheDo).pill"><i class="bi" :class="moTaCheDo(dangMo.cheDo).icon" aria-hidden="true"></i> {{ moTaCheDo(dangMo.cheDo).nhan }}<template v-if="dangMo.cheDo === CHE_DO.NHAN_VIEN_HO_TRO">: {{ dangMo.idNhanVienPhuTrach === idToi ? 'bạn' : dangMo.tenNhanVienPhuTrach }}</template></span>
            <div class="htt-chat-nut">
              <button v-if="nhanDuoc" type="button" class="ad-btn ad-btn-primary ad-btn-sm" :disabled="dangXuLy" @click="nhan">
                <i class="bi bi-person-check" aria-hidden="true"></i> Nhận hỗ trợ
              </button>
              <button v-if="dongDuoc" type="button" class="ad-btn ad-btn-sm" :disabled="dangXuLy" @click="xacNhanDong = true">
                <i class="bi bi-check2-circle" aria-hidden="true"></i> Kết thúc
              </button>
            </div>
          </header>
          <KhungTinNhan :tin-nhan="tin" :la-cua-toi="laCuaToi" :dang-tai="dangTaiTin" rong="Chưa có tin nhắn." />
          <div v-if="!lyDoKhoa" class="htt-nhanh">
            <button type="button" class="htt-nhanh-nut" :aria-expanded="moTraLoiNhanh" @click="moTraLoiNhanh = !moTraLoiNhanh">
              <i class="bi bi-lightning-charge" aria-hidden="true"></i> Trả lời nhanh
            </button>
            <div v-if="moTraLoiNhanh" class="htt-nhanh-ds">
              <button v-for="c in TRA_LOI_NHANH" :key="c" type="button" class="htt-nhanh-muc" @click="chonTraLoiNhanh(c)">{{ c }}</button>
            </div>
          </div>
          <OSoanTin ref="oSoan" v-model="noiDung" :dang-gui="dangGui" :loi="loiGui" :ly-do-khoa="lyDoKhoa" :placeholder="goiYSoan" @gui="gui" @go="loiGui = ''" />
        </template>
      </div>
    </section>

    <ConfirmDialog
      v-if="xacNhanDong"
      title="Kết thúc hỗ trợ"
      :message="`Kết thúc cuộc trò chuyện với ${dangMo?.tenKhach}? Khách sẽ thấy thông báo đã kết thúc và có thể mở cuộc mới khi cần.`"
      confirm-text="Kết thúc"
      :loading="dangXuLy"
      @confirm="dong"
      @cancel="xacNhanDong = false"
    />
  </div>
</template>

<style scoped>
.htt {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  height: max(560px, calc(100vh / var(--ui-zoom, 1) - 150px));
  padding: 0;
  overflow: hidden;
}
.htt-ds {
  border-right: 1px solid var(--fs-line);
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.htt-ds-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 4px 8px;
  padding: 16px 16px 10px;
}
.htt-ds-dau .ad-card-title {
  white-space: nowrap;
}
.htt-link-khach {
  font-size: 0.82rem;
  white-space: nowrap;
  text-decoration: none;
}
.htt-loc {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 16px 10px;
}
.htt-loc-nut {
  border: 1px solid var(--fs-line);
  background: #fff;
  color: var(--fs-text-2);
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 0.82rem;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.htt-loc-nut.active {
  background: var(--fs-primary-soft);
  border-color: var(--fs-primary-line);
  color: var(--fs-primary-dark);
  font-weight: 600;
}
.htt-loc-dem {
  min-width: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #e5e7eb;
  color: var(--fs-text);
  font-size: 0.72rem;
  line-height: 18px;
  text-align: center;
}
.htt-loc-dem.is-gap {
  background: #e5484d;
  color: #fff;
}
.htt-tim {
  margin: 0 16px 10px;
}
.htt-tim .ad-control {
  height: 40px;
}
.htt-muc-ds {
  list-style: none;
  margin: 0;
  padding: 0 8px 8px;
  overflow-y: auto;
  flex: 1 1 auto;
  min-height: 0;
}
.htt-trong {
  color: var(--fs-muted);
  text-align: center;
  padding: 32px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}
.htt-trong .bi {
  font-size: 1.6rem;
}
.htt-muc {
  display: flex;
  gap: 10px;
  width: 100%;
  text-align: left;
  border: 0;
  background: transparent;
  border-radius: 10px;
  padding: 10px 8px;
}
.htt-muc:hover {
  background: var(--fs-hover-row);
}
.htt-muc.active {
  background: var(--fs-primary-soft);
}
.htt-avatar {
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 0.85rem;
}
.htt-muc-than {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1 1 auto;
}
.htt-muc-dong1,
.htt-muc-dong2,
.htt-muc-dong3 {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.htt-ten {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.htt-gio {
  margin-left: auto;
  color: var(--fs-muted);
  white-space: nowrap;
}
.htt-xem-truoc {
  color: var(--fs-text-2);
  font-size: 0.86rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1 1 auto;
}
.is-chua-doc .htt-xem-truoc {
  color: var(--fs-text);
  font-weight: 600;
}
.htt-so {
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
.htt-muc-dong3 .ad-pill {
  font-size: 0.72rem;
}
.htt-chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
}
.htt-chua-chon {
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
.htt-chua-chon .bi {
  font-size: 2.4rem;
  color: var(--fs-primary);
  opacity: 0.7;
}
.htt-chat-dau {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--fs-line);
  flex-wrap: wrap;
}
.htt-quay-lai {
  display: none;
}
.htt-chat-ten {
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-right: auto;
}
.htt-chat-ten small {
  color: var(--fs-muted);
}
.htt-chat-nut {
  display: flex;
  gap: 6px;
}
.htt-nhanh {
  position: relative;
  padding: 6px 12px 0;
  background: #fff;
  border-top: 1px solid var(--fs-line-soft);
}
.htt-nhanh-nut {
  border: 0;
  background: transparent;
  color: var(--fs-primary-dark);
  font-size: 0.82rem;
  padding: 2px 4px;
}
.htt-nhanh-ds {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 12px;
  right: 12px;
  z-index: 5;
  background: #fff;
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(24, 36, 47, 0.12);
  padding: 6px;
  display: flex;
  flex-direction: column;
}
.htt-nhanh-muc {
  border: 0;
  background: transparent;
  text-align: left;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 0.88rem;
}
.htt-nhanh-muc:hover {
  background: var(--fs-hover-row);
}
.htt-nhanh + :deep(.ost) {
  border-top: 0;
}
@media (max-width: 991.98px) {
  .htt {
    grid-template-columns: minmax(0, 1fr);
  }
  .htt.is-dang-mo .htt-ds,
  .htt:not(.is-dang-mo) .htt-chat {
    display: none;
  }
  .htt-quay-lai {
    display: inline-flex;
  }
}
</style>
