<script setup>
// Khung chat của KHÁCH: nhập họ tên + SĐT -> trợ lý ảo trả lời ngay từ dữ liệu cửa hàng -> bấm "Gặp nhân viên" khi cần.
// Cuộc chat (id + mã truy cập) lưu trong trình duyệt nên tải lại trang vẫn tiếp tục. Trang bán hàng online nhúng component này.
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import KhungTinNhan from './KhungTinNhan.vue'
import OSoanTin from './OSoanTin.vue'
import { hoTroKhachApi } from '../../services/chatService'
import { useLoiForm } from '../../composables/useLoiForm'
import { chuanHoaHoTen, HO_TEN, LOI_HO_TEN_KY_TU, LOI_SDT, SDT } from '../../utils/kiemTra'
import { CHE_DO, docNguoiKhachCu, docPhienKhach, gopTin, idCuoi, luuPhienKhach, NGUOI_GUI, xoaPhienKhach } from '../../utils/chat'

const props = defineProps({
  tenCuaHang: { type: String, default: 'FootStyle' },
  chuKyMs: { type: Number, default: 3000 },
})

const phien = ref(docPhienKhach())
const cheDo = ref(CHE_DO.TRO_LY_AO)
const tenNhanVien = ref('')
const tin = ref([])
const tamThoi = ref([]) // tin khách vừa gửi, chờ máy chủ xác nhận
const goiY = ref([])
const noiDung = ref('')
const dangGui = ref(false)
const dangTai = ref(false)
const loiGui = ref('')
const loiChung = ref('')
const xacNhanKetThuc = ref(false)
const khungTin = ref(null)
const oSoan = ref(null)

const f = reactive(docNguoiKhachCu())
const dangBatDau = ref(false)
const { loi, cham, sua, batDauGui, datLoiMayChu } = useLoiForm(() => {
  const e = {}
  const ten = chuanHoaHoTen(f.hoTen)
  if (!ten) e.hoTen = 'Hãy nhập họ tên.'
  else if (ten.length > 100) e.hoTen = 'Họ tên tối đa 100 ký tự.'
  else if (!HO_TEN.test(ten)) e.hoTen = LOI_HO_TEN_KY_TU
  const sdt = String(f.sdt ?? '').replace(/[\s.]/g, '')
  if (!sdt) e.sdt = 'Hãy nhập số điện thoại để nhân viên liên hệ lại khi cần.'
  else if (!SDT.test(sdt)) e.sdt = LOI_SDT
  return e
})

const tatCaTin = computed(() => [...tin.value, ...tamThoi.value])
const daDong = computed(() => cheDo.value === CHE_DO.DA_DONG)
const trangThai = computed(() => {
  switch (cheDo.value) {
    case CHE_DO.CHO_NHAN_VIEN:
      return { icon: 'bi-hourglass-split', nhan: 'Đang chờ nhân viên…', phu: 'Bạn cứ nhắn, nhân viên sẽ trả lời ngay tại đây' }
    case CHE_DO.NHAN_VIEN_HO_TRO:
      return { icon: 'bi-headset', nhan: tenNhanVien.value ? `Nhân viên ${tenNhanVien.value}` : 'Nhân viên hỗ trợ', phu: 'Đang hỗ trợ bạn' }
    case CHE_DO.DA_DONG:
      return { icon: 'bi-check2-circle', nhan: 'Đã kết thúc', phu: 'Cảm ơn bạn đã liên hệ' }
    default:
      return { icon: 'bi-robot', nhan: `Trợ lý ảo ${props.tenCuaHang}`, phu: 'Trả lời tự động từ dữ liệu cửa hàng' }
  }
})
const dangGo = computed(() => (dangGui.value && cheDo.value === CHE_DO.TRO_LY_AO ? 'Trợ lý đang trả lời…' : ''))

function apDung(kq, thayGoiY = true) {
  if (!kq) return
  if (kq.cheDo) cheDo.value = kq.cheDo
  tenNhanVien.value = kq.tenNhanVien || ''
  tin.value = gopTin(tin.value, kq.tinNhan || [])
  if (thayGoiY) goiY.value = kq.goiY || []
}

function hetPhien() {
  xoaPhienKhach()
  phien.value = null
  tin.value = []
  tamThoi.value = []
  goiY.value = []
  cheDo.value = CHE_DO.TRO_LY_AO
  Object.assign(f, docNguoiKhachCu())
}

function xuLyLoi(e) {
  if (e?.code === 'KHONG_TIM_THAY_HOI_THOAI') {
    hetPhien()
    loiChung.value = 'Cuộc trò chuyện cũ không còn, bạn hãy bắt đầu cuộc trò chuyện mới.'
    return true
  }
  if (e?.code === 'DA_KET_THUC') {
    cheDo.value = CHE_DO.DA_DONG
    taiMoi()
    return true
  }
  return false
}

async function taiDau() {
  if (!phien.value) return
  dangTai.value = true
  try {
    apDung(await hoTroKhachApi.xem(phien.value.id, phien.value.ma))
  } catch (e) {
    if (!xuLyLoi(e)) loiChung.value = e.message || 'Không tải được cuộc trò chuyện.'
  } finally {
    dangTai.value = false
  }
}

let dangHoi = false
async function taiMoi() {
  if (!phien.value || dangHoi) return
  dangHoi = true
  try {
    apDung(await hoTroKhachApi.xem(phien.value.id, phien.value.ma, idCuoi(tin.value)), false)
    loiChung.value = ''
  } catch (e) {
    xuLyLoi(e)
  } finally {
    dangHoi = false
  }
}

let hen = null
let demNhip = 0
onMounted(() => {
  taiDau()
  hen = setInterval(() => {
    if (!phien.value || daDong.value || dangGui.value) return
    if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return
    // Đang ở trợ lý ảo thì hỏi thưa hơn (câu trả lời đã về ngay khi gửi), chỉ để biết nhân viên có vào hay không
    demNhip++
    if (cheDo.value === CHE_DO.TRO_LY_AO && demNhip % 3 !== 0) return
    taiMoi()
  }, props.chuKyMs)
})
onBeforeUnmount(() => clearInterval(hen))

async function batDau() {
  loiChung.value = ''
  if (dangBatDau.value || !batDauGui()) return
  dangBatDau.value = true
  try {
    const hoTen = chuanHoaHoTen(f.hoTen)
    const sdt = String(f.sdt).replace(/[\s.]/g, '')
    const kq = await hoTroKhachApi.batDau(hoTen, sdt)
    phien.value = { id: kq.id, ma: kq.ma, hoTen, sdt }
    luuPhienKhach(phien.value)
    tin.value = []
    apDung(kq)
    setTimeout(() => oSoan.value?.focus(), 50)
  } catch (e) {
    if (!datLoiMayChu(e)) loiChung.value = e.message || 'Không bắt đầu được cuộc trò chuyện.'
  } finally {
    dangBatDau.value = false
  }
}

let soTam = 0
async function gui(text) {
  if (!phien.value || dangGui.value) return
  const noi = String(text ?? '').trim()
  if (!noi) return
  loiGui.value = ''
  dangGui.value = true
  const tam = { id: -(++soTam), loaiNguoiGui: NGUOI_GUI.KHACH, tenNguoiGui: phien.value.hoTen, noiDung: noi, thoiGian: new Date().toISOString(), tam: true }
  tamThoi.value = [...tamThoi.value, tam]
  if (text === noiDung.value.trim()) noiDung.value = ''
  goiY.value = []
  try {
    apDung(await hoTroKhachApi.gui(phien.value.id, phien.value.ma, noi))
  } catch (e) {
    if (!xuLyLoi(e)) {
      loiGui.value = e.loiTheoTruong?.noiDung || e.message || 'Chưa gửi được tin nhắn, bạn thử lại nhé.'
      if (!noiDung.value) noiDung.value = noi
    }
  } finally {
    tamThoi.value = tamThoi.value.filter((t) => t.id !== tam.id)
    dangGui.value = false
    oSoan.value?.focus()
  }
}

function chonGoiY(g) {
  if (/gặp nhân viên/i.test(g)) gapNhanVien()
  else gui(g)
}

async function gapNhanVien() {
  if (!phien.value) return
  try {
    apDung(await hoTroKhachApi.gapNhanVien(phien.value.id, phien.value.ma))
  } catch (e) {
    if (!xuLyLoi(e)) loiGui.value = e.message
  }
}

const dangKetThuc = ref(false)
async function ketThuc() {
  dangKetThuc.value = true
  try {
    apDung(await hoTroKhachApi.ketThuc(phien.value.id, phien.value.ma))
    xacNhanKetThuc.value = false
  } catch (e) {
    if (!xuLyLoi(e)) loiGui.value = e.message
    xacNhanKetThuc.value = false
  } finally {
    dangKetThuc.value = false
  }
}

const laCuaToi = (t) => t.loaiNguoiGui === NGUOI_GUI.KHACH
</script>

<template>
  <div class="kck">
    <header class="kck-dau">
      <span class="kck-bieu-tuong" :class="{ 'is-bot': cheDo === CHE_DO.TRO_LY_AO }" aria-hidden="true"><i class="bi" :class="phien ? trangThai.icon : 'bi-chat-heart'"></i></span>
      <div class="kck-tieu-de">
        <strong>{{ phien ? trangThai.nhan : `Hỗ trợ ${tenCuaHang}` }}</strong>
        <small>{{ phien ? trangThai.phu : 'Trợ lý ảo trả lời ngay, nhân viên hỗ trợ khi bạn cần' }}</small>
      </div>
      <div v-if="phien && !daDong" class="kck-nut">
        <button v-if="cheDo === CHE_DO.TRO_LY_AO" type="button" class="ad-btn ad-btn-sm" @click="gapNhanVien">
          <i class="bi bi-person-raised-hand" aria-hidden="true"></i> Gặp nhân viên
        </button>
        <button type="button" class="ad-icon-btn" title="Kết thúc trò chuyện" aria-label="Kết thúc trò chuyện" @click="xacNhanKetThuc = true">
          <i class="bi bi-x-lg" aria-hidden="true"></i>
        </button>
      </div>
    </header>

    <!-- Chưa có cuộc chat: nhập thông tin -->
    <form v-if="!phien" class="kck-form" novalidate @submit.prevent="batDau">
      <p class="kck-moi">Xin chào! Để lại tên và số điện thoại, trợ lý ảo sẽ tư vấn sản phẩm, khuyến mãi, kiểm tra đơn hàng cho bạn ngay. Cần người thật? Nhân viên luôn sẵn sàng.</p>
      <div class="mb-3">
        <label class="ad-label" for="kck-ten">Họ tên <span class="text-danger">*</span></label>
        <input id="kck-ten" v-model="f.hoTen" class="form-control ad-control" :class="{ 'is-invalid': loi.hoTen }" maxlength="100" autocomplete="name"
               :aria-invalid="!!loi.hoTen" aria-describedby="kck-ten-loi" @input="sua('hoTen')" @blur="cham('hoTen')" />
        <div v-if="loi.hoTen" id="kck-ten-loi" class="ad-error">{{ loi.hoTen }}</div>
      </div>
      <div class="mb-3">
        <label class="ad-label" for="kck-sdt">Số điện thoại <span class="text-danger">*</span></label>
        <input id="kck-sdt" v-model="f.sdt" class="form-control ad-control" :class="{ 'is-invalid': loi.sdt }" inputmode="tel" maxlength="13" autocomplete="tel"
               :aria-invalid="!!loi.sdt" aria-describedby="kck-sdt-loi" @input="sua('sdt')" @blur="cham('sdt')" />
        <div v-if="loi.sdt" id="kck-sdt-loi" class="ad-error">{{ loi.sdt }}</div>
        <div v-else class="ad-hint">Dùng để tra cứu đơn hàng của bạn và để nhân viên gọi lại khi cần.</div>
      </div>
      <div v-if="loiChung" class="alert alert-warning py-2 small" role="alert">{{ loiChung }}</div>
      <button type="submit" class="ad-btn ad-btn-primary w-100 justify-content-center" :disabled="dangBatDau">
        <span v-if="dangBatDau" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <i v-else class="bi bi-chat-dots" aria-hidden="true"></i> Bắt đầu trò chuyện
      </button>
    </form>

    <!-- Đang chat -->
    <template v-else>
      <div v-if="loiChung" class="kck-loi" role="alert">{{ loiChung }}</div>
      <KhungTinNhan ref="khungTin" :tin-nhan="tatCaTin" :la-cua-toi="laCuaToi" :dang-go="dangGo" :dang-tai="dangTai" />
      <div v-if="goiY.length && cheDo === CHE_DO.TRO_LY_AO && !dangGui" class="kck-goi-y" aria-label="Câu hỏi gợi ý">
        <button v-for="g in goiY" :key="g" type="button" class="kck-chip" @click="chonGoiY(g)">{{ g }}</button>
      </div>
      <div v-if="daDong" class="kck-ket-thuc">
        <span>Cuộc trò chuyện đã kết thúc.</span>
        <button type="button" class="ad-btn ad-btn-primary ad-btn-sm" @click="hetPhien"><i class="bi bi-plus-lg" aria-hidden="true"></i> Trò chuyện mới</button>
      </div>
      <OSoanTin v-else ref="oSoan" v-model="noiDung" :dang-gui="dangGui" :loi="loiGui" :placeholder="cheDo === CHE_DO.TRO_LY_AO ? 'Hỏi trợ lý ảo, vd: vợt Yonex dưới 3 triệu…' : 'Nhập tin nhắn cho nhân viên…'"
                @gui="gui" @go="loiGui = ''" />
    </template>

    <ConfirmDialog
      v-if="xacNhanKetThuc"
      title="Kết thúc trò chuyện"
      message="Bạn muốn kết thúc cuộc trò chuyện này? Bạn vẫn có thể bắt đầu cuộc trò chuyện mới bất cứ lúc nào."
      confirm-text="Kết thúc"
      variant="danger"
      :loading="dangKetThuc"
      @confirm="ketThuc"
      @cancel="xacNhanKetThuc = false"
    />
  </div>
</template>

<style scoped>
.kck {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #fff;
}
.kck-dau {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: var(--fs-navy);
  color: #fff;
}
.kck-bieu-tuong {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.14);
  font-size: 1.15rem;
  flex: 0 0 auto;
}
.kck-bieu-tuong.is-bot {
  background: #ede9fe;
  color: #6d28d9;
}
.kck-tieu-de {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1 1 auto;
}
.kck-tieu-de strong {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kck-tieu-de small {
  opacity: 0.75;
  font-size: 0.78rem;
}
.kck-nut {
  display: flex;
  gap: 6px;
  align-items: center;
}
.kck-nut .ad-btn {
  white-space: nowrap;
}
.kck-nut .ad-icon-btn {
  color: #fff;
  background: transparent;
  border-color: rgba(255, 255, 255, 0.3);
}
.kck-form {
  padding: 18px 16px;
  overflow-y: auto;
}
.kck-moi {
  margin: 0 0 16px;
  color: var(--fs-text-2);
  background: var(--fs-primary-soft);
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 0.92rem;
}
.kck-loi {
  background: #fff7ed;
  color: #9a3412;
  font-size: 0.85rem;
  padding: 6px 14px;
}
.kck-goi-y {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 8px 12px;
  background: var(--fs-surface);
  border-top: 1px solid var(--fs-line-soft);
}
.kck-chip {
  border: 1px solid var(--fs-primary-line);
  background: #fff;
  color: var(--fs-primary-dark);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 0.85rem;
}
.kck-chip:hover {
  background: var(--fs-primary-soft);
}
.kck-ket-thuc {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid var(--fs-line);
  color: var(--fs-text-2);
}
</style>
