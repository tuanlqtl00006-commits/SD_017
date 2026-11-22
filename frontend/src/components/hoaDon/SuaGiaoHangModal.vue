<script setup>
// Hộp thoại "Sửa thông tin giao hàng" (góp ý 14/11: tách khỏi "Chỉnh sửa đơn"). Dùng chung cho màn chi tiết hóa đơn (nhân viên)
// và màn đơn hàng của khách: đổi người nhận / SĐT / địa chỉ, mỗi đơn MỘT lần, trước khi giao cho đơn vị vận chuyển.
// Phí giao theo quãng đường tới nơi nhận (máy chủ tính bằng API bản đồ): đổi địa chỉ thì hiện phí mới; máy chủ chốt lại khi lưu.
import { computed, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import { hoiXacNhan } from '../../composables/useXacNhan'
import BaseModal from '../common/BaseModal.vue'
import DiaChiHanhChinhSelect from '../common/DiaChiHanhChinhSelect.vue'
import { dungCaiDatVanChuyen } from '../../services/caiDatVanChuyen'
import { formatMoney } from '../../utils/format'
import { phiSauKhiSuaDon } from '../../utils/phiVanChuyenTinh'

const props = defineProps({
  data: { type: Object, required: true }, // dữ liệu chi tiết hóa đơn (dạng GET /hoa-don/{id})
  url: { type: String, required: true }, // PUT tới đây
  laKhach: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'saved'])

const hd = computed(() => props.data?.hoaDon ?? {})
const giao = computed(() => props.data?.giaoHang ?? {})
const caiDat = dungCaiDatVanChuyen()

// Tách địa chỉ đang có "cụ thể, phường/xã, tỉnh" (dạng ô chọn địa chỉ ghép ra) để điền sẵn
const phan = String(giao.value.diaChi || '').split(',').map((x) => x.trim())
const coTinh = phan.length >= 3 && /^(tỉnh|thành phố)\s/i.test(phan[phan.length - 1]) && /^(phường|xã|đặc khu)\s/i.test(phan[phan.length - 2])
const form = reactive({
  hoTen: giao.value.hoTenNguoiNhan || '',
  sdt: giao.value.sdtNguoiNhan || '',
  tinhThanh: coTinh ? phan[phan.length - 1] : '',
  phuongXa: coTinh ? phan[phan.length - 2] : '',
  diaChiCuThe: coTinh ? phan.slice(0, -2).join(', ') : String(giao.value.diaChi || ''),
  lyDo: '',
})
const loi = ref({})
const loiChung = ref('')
const dangLuu = ref(false)

const diaChiMoi = computed(() => [form.diaChiCuThe.trim(), form.phuongXa, form.tinhThanh].filter(Boolean).join(', '))
const tienHang = computed(() => Number(hd.value.tongTien) || 0)
const phiCu = computed(() => Number(hd.value.phiVanChuyen) || 0)
// Phí tới địa chỉ mới do máy chủ tính theo quãng đường; lỗi mạng thì để undefined (tạm tính theo bảng tỉnh)
const phiTheoDiaChiMoi = ref(undefined)
let lanHoiPhi = 0
let henPhi = null
watch(
  () => [form.tinhThanh, form.phuongXa, form.diaChiCuThe],
  () => {
    clearTimeout(henPhi)
    henPhi = setTimeout(async () => {
      const lan = ++lanHoiPhi
      if (!form.tinhThanh || !form.phuongXa) return
      try {
        const { data } = await api.get('/cua-hang/phi-van-chuyen', {
          params: { tinhThanh: form.tinhThanh, phuongXa: form.phuongXa, diaChi: form.diaChiCuThe.trim() || undefined },
        })
        if (lan === lanHoiPhi) phiTheoDiaChiMoi.value = data?.phiVanChuyen ?? undefined
      } catch {
        if (lan === lanHoiPhi) phiTheoDiaChiMoi.value = undefined
      }
    }, 400)
  },
  { immediate: true },
)
const phiMoi = computed(() =>
  form.tinhThanh && form.phuongXa
    ? phiSauKhiSuaDon({
        caiDat, diaChiMoi: diaChiMoi.value, diaChiCu: giao.value.diaChi, tienHang: tienHang.value, tienHangCu: tienHang.value,
        phiCu: phiCu.value, phiTheoDiaChiMoi: phiTheoDiaChiMoi.value,
      })
    : null,
)

// Đơn đã thanh toán: chỉ cho phép đổi địa chỉ trong phạm vi phí ship bằng nhau (Requirement 3)
const daThanhToan = computed(() => Number(props.data?.daThanhToan ?? props.data?.hoaDon?.daThanhToan ?? 0))
const phiShipKhongDoi = computed(() => phiMoi.value === null || Math.round(Number(phiMoi.value)) === Math.round(Number(phiCu.value)))
const khongTheSuaDoLechPhi = computed(() => daThanhToan.value > 0 && phiMoi.value !== null && !phiShipKhongDoi.value)

async function luu() {
  if (dangLuu.value) return
  if (khongTheSuaDoLechPhi.value) {
    loiChung.value = 'Đơn hàng đã thanh toán chỉ cho phép sửa thông tin người nhận và địa chỉ trong phạm vi phí ship bằng nhau.'
    return
  }
  loi.value = {}
  loiChung.value = ''
  if (!(await hoiXacNhan({ title: 'Lưu thông tin giao hàng', message: 'Thông tin giao hàng của mỗi đơn chỉ đổi được một lần. Bạn có chắc muốn lưu?', confirmText: 'Lưu thay đổi' }))) return
  dangLuu.value = true
  try {
    const { data } = await api.put(props.url, {
      hoTenNguoiNhan: form.hoTen,
      sdtNguoiNhan: form.sdt,
      diaChiCuThe: form.diaChiCuThe,
      phuongXa: form.phuongXa,
      tinhThanh: form.tinhThanh,
      lyDo: form.lyDo,
    })
    emit('saved', data)
  } catch (e) {
    loi.value = e.loiTheoTruong || {}
    loiChung.value = e.loiTheoTruong ? '' : e.message || 'Không lưu được thông tin giao hàng.'
  } finally {
    dangLuu.value = false
  }
}
</script>

<template>
  <BaseModal title="Sửa thông tin giao hàng" size="md" static-backdrop @close="emit('close')">
    <div data-testid="sua-giao-hang">
      <p class="sgh-luu-y">
        <i class="bi bi-info-circle" aria-hidden="true"></i>
        Mỗi đơn chỉ đổi thông tin giao hàng <strong>một lần</strong>, trước khi cửa hàng giao cho đơn vị vận chuyển. Sản phẩm trong đơn giữ nguyên.
      </p>
      <div class="row g-3">
        <div class="col-md-6">
          <label class="form-label small fw-semibold" for="sgh-ten">Người nhận <span class="text-danger">*</span></label>
          <input id="sgh-ten" v-model="form.hoTen" class="form-control" :class="{ 'is-invalid': loi.hoTenNguoiNhan }" />
          <div v-if="loi.hoTenNguoiNhan" class="invalid-feedback">{{ loi.hoTenNguoiNhan }}</div>
        </div>
        <div class="col-md-6">
          <label class="form-label small fw-semibold" for="sgh-sdt">Số điện thoại <span class="text-danger">*</span></label>
          <input id="sgh-sdt" v-model="form.sdt" type="tel" class="form-control" :class="{ 'is-invalid': loi.sdtNguoiNhan }" />
          <div v-if="loi.sdtNguoiNhan" class="invalid-feedback">{{ loi.sdtNguoiNhan }}</div>
        </div>
        <DiaChiHanhChinhSelect
          v-model:tinh-thanh="form.tinhThanh"
          v-model:phuong-xa="form.phuongXa"
          id-prefix="sgh-dc"
          col-class="col-md-6"
          required
          :error-tinh="loi.tinhThanh || ''"
          :error-phuong="loi.phuongXa || ''"
        />
        <div class="col-12">
          <label class="form-label small fw-semibold" for="sgh-cu-the">Số nhà, tên đường <span class="text-danger">*</span></label>
          <input id="sgh-cu-the" v-model="form.diaChiCuThe" class="form-control" :class="{ 'is-invalid': loi.diaChiCuThe }" />
          <div v-if="loi.diaChiCuThe" class="invalid-feedback">{{ loi.diaChiCuThe }}</div>
        </div>
        <div class="col-12">
          <label class="form-label small fw-semibold" for="sgh-ly-do">
            {{ laKhach ? 'Ghi chú cho cửa hàng (không bắt buộc)' : 'Lý do' }} <span v-if="!laKhach" class="text-danger">*</span>
          </label>
          <input id="sgh-ly-do" v-model="form.lyDo" class="form-control" :class="{ 'is-invalid': loi.lyDo }" :placeholder="laKhach ? 'vd: giao giờ hành chính' : 'vd: khách gọi đổi địa chỉ'" />
          <div v-if="loi.lyDo" class="invalid-feedback">{{ loi.lyDo }}</div>
        </div>
      </div>
      <div class="sgh-phi" data-testid="sgh-phi">
        <span>Phí giao hàng</span>
        <span>
          <template v-if="phiMoi !== phiCu"><s class="text-muted me-1">{{ formatMoney(phiCu) }}</s></template>
          <strong>{{ phiMoi === 0 ? 'Miễn phí' : formatMoney(phiMoi) }}</strong>
        </span>
      </div>
      <div v-if="khongTheSuaDoLechPhi" class="alert alert-warning small mt-3 mb-0" role="alert" data-testid="sgh-lech-phi-warning">
        <i class="bi bi-exclamation-triangle-fill me-1"></i>
        <strong>Đơn hàng đã thanh toán trước ({{ formatMoney(daThanhToan) }}):</strong>
        Hệ thống chỉ cho phép đổi địa chỉ trong phạm vi phí vận chuyển bằng nhau. Phí hiện tại là <strong>{{ formatMoney(phiCu) }}</strong>, phí địa chỉ mới là <strong>{{ formatMoney(phiMoi) }}</strong>.
      </div>
      <p v-if="loiChung" class="text-danger small mt-2 mb-0" data-testid="sgh-loi">{{ loiChung }}</p>
    </div>
    <template #footer>
      <button type="button" class="btn btn-outline-secondary" @click="emit('close')">Hủy</button>
      <button type="button" class="btn btn-primary" :disabled="dangLuu || khongTheSuaDoLechPhi" data-testid="sgh-luu" @click="luu">
        <span v-if="dangLuu" class="spinner-border spinner-border-sm me-1"></span> Lưu thông tin giao hàng
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.sgh-luu-y {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 12px;
  font-size: 0.86rem;
  color: #1e3a8a;
  background: #eff6ff;
  border-radius: 10px;
}
.sgh-phi {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 12px;
  font-size: 0.92rem;
  border-top: 1px dashed #cbd5e1;
}
</style>
