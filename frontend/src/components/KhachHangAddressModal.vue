<script setup>
// Cửa sổ quản lý địa chỉ giao hàng của một khách hàng: xem danh sách, xem chi tiết, thêm, sửa, xóa, đặt địa chỉ mặc định.
// Form địa chỉ kiểm tra từng ô và báo lỗi ngay dưới ô (tên người nhận, SĐT đúng đầu số, tỉnh, phường/xã, địa chỉ cụ thể),
// khớp backend DiaChiKhachHangController.chuanHoa. Địa chỉ theo đơn vị hành chính SAU sáp nhập (Tỉnh/Thành phố -> Phường/Xã).
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import BaseModal from './common/BaseModal.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import DiaChiHanhChinhSelect from './common/DiaChiHanhChinhSelect.vue'
import { ghepDiaChi } from '../utils/diaChi'
import { cat, chuanHoaHoTen, loiHoTen, loiSdt } from '../utils/kiemTra'
import { useLoiForm } from '../composables/useLoiForm'
import { useToast } from '../composables/useToast'

const toast = useToast()

const props = defineProps({
  customer: { type: Object, required: true },
})
const emit = defineEmits(['close', 'address-updated'])

const addresses = ref([])
const dangTai = ref(true)
const showForm = ref(false)
const formTitle = ref('')
const xemDiaChi = ref(null) // địa chỉ đang xem chi tiết (chỉ đọc), null: đang ở danh sách
const saving = ref(false)

const f = reactive({
  id: null,
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: '',
  phuongXa: '',
  quanHuyen: '',
  diaChiCuThe: '',
  laDiaChiMacDinh: false,
})

/* ----- Luật kiểm tra từng ô (cùng câu chữ với backend) ----- */
function tinhLoi() {
  if (!showForm.value) return {}
  const e = {}
  const ten = loiHoTen(f.tenNguoiNhan, { thieu: 'Vui lòng nhập tên người nhận.', toiDa: 100 })
  if (ten) e.tenNguoiNhan = ten
  const sdt = loiSdt(f.sdtNguoiNhan, 'Vui lòng nhập số điện thoại người nhận.')
  if (sdt) e.sdtNguoiNhan = sdt
  if (!cat(f.tinhThanhPho)) e.tinhThanhPho = 'Vui lòng chọn Tỉnh/Thành phố.'
  if (!cat(f.phuongXa)) e.phuongXa = 'Vui lòng chọn Phường/Xã.'
  const chiTiet = cat(f.diaChiCuThe)
  if (!chiTiet) e.diaChiCuThe = 'Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).'
  else if (chiTiet.length > 255) e.diaChiCuThe = 'Địa chỉ cụ thể tối đa 255 ký tự.'
  return e
}
const { loi, cham, sua, batDauGui, datLoiMayChu, focusLoiDau, datLai } = useLoiForm(tinhLoi)

/* ----- Danh sách ----- */
async function fetchAddresses() {
  try {
    const response = await api.get(`/dia-chi/khach-hang/${props.customer.id}`)
    const ds = [...(response.data || [])]
    ds.sort((a, b) => Number(!!b.laDiaChiMacDinh) - Number(!!a.laDiaChiMacDinh)) // địa chỉ mặc định lên đầu
    addresses.value = ds
  } catch (error) {
    toast.error(error.message || 'Không tải được danh sách địa chỉ.')
  } finally {
    dangTai.value = false
  }
}
onMounted(fetchAddresses)

function moForm(tieuDe, du) {
  Object.assign(f, du)
  datLai()
  formTitle.value = tieuDe
  xemDiaChi.value = null
  showForm.value = true
}

function addNewAddress() {
  moForm('Thêm địa chỉ mới', {
    id: null,
    tenNguoiNhan: props.customer.hoTen || '',
    sdtNguoiNhan: props.customer.sdt || '',
    tinhThanhPho: '',
    phuongXa: '',
    quanHuyen: '',
    diaChiCuThe: '',
    laDiaChiMacDinh: addresses.value.length === 0,
  })
}

function viewAddress(addr) {
  xemDiaChi.value = addr
}

// Tên tỉnh / phường xã lấy nguyên từ địa chỉ đã lưu. Địa chỉ cũ (trước sáp nhập) không còn trong danh sách mới thì ô chọn vẫn
// hiện giá trị cũ kèm lời nhắc chọn lại; địa chỉ có Quận/Huyện cũ vẫn được giữ nguyên khi lưu.
function editAddress(addr) {
  moForm('Cập nhật địa chỉ', {
    id: addr.id,
    tenNguoiNhan: addr.tenNguoiNhan || '',
    sdtNguoiNhan: addr.sdtNguoiNhan || '',
    tinhThanhPho: addr.tinhThanhPho || '',
    phuongXa: addr.phuongXa || '',
    quanHuyen: addr.quanHuyen || '',
    diaChiCuThe: addr.diaChiCuThe || '',
    laDiaChiMacDinh: !!addr.laDiaChiMacDinh,
  })
}

function huyForm() {
  showForm.value = false
}

async function saveAddress() {
  if (saving.value) return
  if (!batDauGui()) {
    focusLoiDau('#kh-dc-form')
    return
  }
  // Địa chỉ mới (sau sáp nhập) không có Quận/Huyện: chỉ giữ lại quận/huyện cũ khi tỉnh / phường xã vẫn là giá trị cũ chưa đổi
  const goc = addresses.value.find((a) => a.id === f.id)
  const giuQuanHuyenCu = goc && goc.tinhThanhPho === f.tinhThanhPho && goc.phuongXa === f.phuongXa
  const payload = {
    id: f.id,
    tenNguoiNhan: chuanHoaHoTen(f.tenNguoiNhan),
    sdtNguoiNhan: cat(f.sdtNguoiNhan),
    tinhThanhPho: cat(f.tinhThanhPho),
    phuongXa: cat(f.phuongXa),
    quanHuyen: giuQuanHuyenCu ? goc.quanHuyen || '' : '',
    diaChiCuThe: cat(f.diaChiCuThe),
    laDiaChiMacDinh: f.laDiaChiMacDinh,
  }
  saving.value = true
  try {
    if (f.id) {
      await api.put(`/dia-chi/${f.id}`, payload)
      toast.success('Đã cập nhật địa chỉ.')
    } else {
      await api.post(`/dia-chi/khach-hang/${props.customer.id}`, payload)
      toast.success('Đã thêm địa chỉ mới.')
    }
    showForm.value = false
    await fetchAddresses()
    emit('address-updated')
  } catch (error) {
    // Backend báo lỗi theo từng ô thì gắn xuống đúng ô; không có thì báo chung
    if (datLoiMayChu(error)) focusLoiDau('#kh-dc-form')
    else toast.error(error.message || 'Có lỗi xảy ra khi lưu địa chỉ.')
  } finally {
    saving.value = false
  }
}

/* ----- Xóa ----- */
const confirmDelete = ref(null)
const deleting = ref(false)

async function doDeleteAddress() {
  const addr = confirmDelete.value
  if (!addr) return
  deleting.value = true
  try {
    await api.delete(`/dia-chi/${addr.id}`)
    toast.success('Đã xóa địa chỉ.')
    confirmDelete.value = null
    await fetchAddresses()
    emit('address-updated')
  } catch (error) {
    toast.error(error.message || 'Không thể xóa địa chỉ này.')
  } finally {
    deleting.value = false
  }
}

const tieuDe = computed(() => `Địa chỉ của ${props.customer?.hoTen || 'khách hàng'}`)
</script>

<template>
  <BaseModal :title="tieuDe" size="lg" :static-backdrop="showForm" @close="emit('close')">
    <!-- ===== Form thêm / sửa ===== -->
    <form v-if="showForm" id="kh-dc-form" class="kh-dc-form" novalidate @submit.prevent="saveAddress">
      <h3 class="kh-dc-form-title">{{ formTitle }}</h3>
      <div class="row g-3">
        <div class="col-md-6">
          <label class="ad-label" for="kh-dc-ten">Tên người nhận <span class="ad-required">*</span></label>
          <input
            id="kh-dc-ten"
            v-model="f.tenNguoiNhan"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': loi.tenNguoiNhan }"
            maxlength="100"
            autocomplete="off"
            :aria-invalid="!!loi.tenNguoiNhan"
            aria-describedby="kh-dc-ten-msg"
            @input="sua('tenNguoiNhan')"
            @blur="cham('tenNguoiNhan')"
          />
          <p v-if="loi.tenNguoiNhan" id="kh-dc-ten-msg" class="ad-error">{{ loi.tenNguoiNhan }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="kh-dc-sdt">Số điện thoại <span class="ad-required">*</span></label>
          <input
            id="kh-dc-sdt"
            v-model="f.sdtNguoiNhan"
            type="text"
            inputmode="numeric"
            class="form-control ad-control"
            :class="{ 'is-invalid': loi.sdtNguoiNhan }"
            maxlength="15"
            autocomplete="off"
            :aria-invalid="!!loi.sdtNguoiNhan"
            aria-describedby="kh-dc-sdt-msg"
            @input="sua('sdtNguoiNhan')"
            @blur="cham('sdtNguoiNhan')"
          />
          <p v-if="loi.sdtNguoiNhan" id="kh-dc-sdt-msg" class="ad-error">{{ loi.sdtNguoiNhan }}</p>
        </div>

        <!-- Tỉnh/Thành phố -> Phường/Xã theo đơn vị hành chính sau sáp nhập (không còn Quận/Huyện) -->
        <DiaChiHanhChinhSelect
          v-model:tinh-thanh="f.tinhThanhPho"
          v-model:phuong-xa="f.phuongXa"
          id-prefix="kh-dc"
          required
          col-class="col-md-6"
          select-class="ad-control"
          label-class="ad-label"
          :error-tinh="loi.tinhThanhPho"
          :error-phuong="loi.phuongXa"
          @update:tinh-thanh="sua('tinhThanhPho')"
          @update:phuong-xa="sua('phuongXa')"
        />

        <div class="col-12">
          <label class="ad-label" for="kh-dc-chi-tiet">Địa chỉ cụ thể <span class="ad-required">*</span></label>
          <input
            id="kh-dc-chi-tiet"
            v-model="f.diaChiCuThe"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': loi.diaChiCuThe }"
            maxlength="255"
            placeholder="Số nhà, tên đường..."
            autocomplete="off"
            :aria-invalid="!!loi.diaChiCuThe"
            aria-describedby="kh-dc-chi-tiet-msg"
            @input="sua('diaChiCuThe')"
            @blur="cham('diaChiCuThe')"
          />
          <p v-if="loi.diaChiCuThe" id="kh-dc-chi-tiet-msg" class="ad-error">{{ loi.diaChiCuThe }}</p>
        </div>

        <div class="col-12">
          <div class="form-check">
            <input id="kh-dc-mac-dinh" v-model="f.laDiaChiMacDinh" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="kh-dc-mac-dinh">Đặt làm địa chỉ mặc định</label>
          </div>
        </div>
      </div>

      <div class="kh-dc-form-actions">
        <button type="button" class="ad-btn" :disabled="saving" @click="huyForm">Hủy</button>
        <button type="submit" class="ad-btn ad-btn-primary" :disabled="saving">
          <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          Lưu địa chỉ
        </button>
      </div>
    </form>

    <!-- ===== Xem chi tiết một địa chỉ (chỉ đọc) ===== -->
    <div v-else-if="xemDiaChi" class="kh-dc-form" data-testid="xem-dia-chi">
      <h3 class="kh-dc-form-title">
        Chi tiết địa chỉ
        <span v-if="xemDiaChi.laDiaChiMacDinh" class="ad-pill ad-pill-green">Mặc định</span>
      </h3>
      <dl class="ad-dl">
        <div><dt>Tên người nhận</dt><dd>{{ xemDiaChi.tenNguoiNhan || '—' }}</dd></div>
        <div><dt>Số điện thoại</dt><dd>{{ xemDiaChi.sdtNguoiNhan || '—' }}</dd></div>
        <div><dt>Tỉnh/Thành phố</dt><dd>{{ xemDiaChi.tinhThanhPho || '—' }}</dd></div>
        <div v-if="xemDiaChi.quanHuyen"><dt>Quận/Huyện (cũ)</dt><dd>{{ xemDiaChi.quanHuyen }}</dd></div>
        <div><dt>Phường/Xã</dt><dd>{{ xemDiaChi.phuongXa || '—' }}</dd></div>
        <div><dt>Địa chỉ cụ thể</dt><dd>{{ xemDiaChi.diaChiCuThe || '—' }}</dd></div>
        <div class="ad-dl-full"><dt>Địa chỉ đầy đủ</dt><dd>{{ ghepDiaChi(xemDiaChi) || '—' }}</dd></div>
      </dl>
      <div class="kh-dc-form-actions">
        <button type="button" class="ad-btn" @click="xemDiaChi = null">Quay lại</button>
        <button type="button" class="ad-btn ad-btn-primary" @click="editAddress(xemDiaChi)">Sửa địa chỉ</button>
      </div>
    </div>

    <!-- ===== Danh sách ===== -->
    <div v-else>
      <div v-if="dangTai" class="ad-empty">Đang tải địa chỉ…</div>
      <template v-else>
        <div v-if="addresses.length" class="kh-dc-list">
          <article v-for="addr in addresses" :key="addr.id" class="kh-dc-item" :class="{ 'is-mac-dinh': addr.laDiaChiMacDinh }">
            <div class="kh-dc-item-head">
              <h3 class="kh-dc-item-ten">
                {{ addr.tenNguoiNhan }}
                <span v-if="addr.laDiaChiMacDinh" class="ad-pill ad-pill-green">Mặc định</span>
              </h3>
              <div class="ad-actions">
                <button type="button" class="ad-icon-btn" title="Xem chi tiết" :aria-label="`Xem địa chỉ của ${addr.tenNguoiNhan}`" @click="viewAddress(addr)">
                  <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
                <button type="button" class="ad-icon-btn" title="Sửa địa chỉ" :aria-label="`Sửa địa chỉ của ${addr.tenNguoiNhan}`" @click="editAddress(addr)">
                  <i class="bi bi-pencil-square" aria-hidden="true"></i>
                </button>
                <button type="button" class="ad-icon-btn is-danger" title="Xóa địa chỉ" :aria-label="`Xóa địa chỉ của ${addr.tenNguoiNhan}`" @click="confirmDelete = addr">
                  <i class="bi bi-trash" aria-hidden="true"></i>
                </button>
              </div>
            </div>
            <p class="kh-dc-item-dong">{{ addr.sdtNguoiNhan }}</p>
            <p class="kh-dc-item-dong">{{ ghepDiaChi(addr) }}</p>
          </article>
        </div>
        <p v-else class="kh-dc-trong">Khách hàng chưa có địa chỉ nào.</p>

        <button type="button" class="kh-dc-them" @click="addNewAddress">
          <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm địa chỉ mới
        </button>
      </template>
    </div>
  </BaseModal>

  <ConfirmDialog
    v-if="confirmDelete"
    title="Xóa địa chỉ"
    message="Bạn có chắc chắn muốn xóa địa chỉ này không?"
    confirm-text="Xóa"
    variant="danger"
    :loading="deleting"
    @confirm="doDeleteAddress"
    @cancel="confirmDelete = null"
  />
</template>

<style scoped>
.kh-dc-list {
  display: grid;
  gap: 0.75rem;
}
.kh-dc-item {
  padding: 0.9rem 1rem;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 14px;
  background: #fff;
}
.kh-dc-item.is-mac-dinh {
  border-color: #86d3a3;
  background: #f6fcf8;
}
.kh-dc-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}
.kh-dc-item-ten {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}
.kh-dc-item-dong {
  margin: 0.25rem 0 0;
  font-size: 0.88rem;
  color: var(--fs-muted, #5b6573);
}
.kh-dc-trong {
  margin: 0 0 1rem;
  padding: 1.25rem 0;
  text-align: center;
  color: var(--fs-muted, #5b6573);
}
.kh-dc-them {
  width: 100%;
  margin-top: 0.75rem;
  padding: 0.8rem;
  border: 2px dashed #d5dce6;
  border-radius: 14px;
  background: #f8fafc;
  font-weight: 600;
  color: #4b5563;
}
.kh-dc-them:hover {
  border-color: var(--fs-primary, #0977ec);
  color: var(--fs-primary-dark, #075fc0);
  background: var(--fs-primary-soft, #e7f1fd);
}
.kh-dc-form-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0 0 1rem;
  font-size: 1rem;
  font-weight: 700;
}
.kh-dc-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
}
</style>
