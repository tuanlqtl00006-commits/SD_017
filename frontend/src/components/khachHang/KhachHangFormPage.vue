<script setup>
// Trang THÊM (/khach-hang/them) và SỬA (/khach-hang/:id/sua) khách hàng - làm lại theo góp ý của cô:
//  - Lỗi hiện NGAY DƯỚI TỪNG Ô: họ tên, số điện thoại (đúng 10 số, đầu số 03/05/07/08/09), email (đúng định dạng), ngày sinh (từ đủ 15 tuổi).
//  - Trùng số điện thoại / email báo ngay khi gõ (so với danh sách khách đã có) và báo lại khi backend trả 409.
//  - Mọi ô chọn (giới tính, trạng thái, tỉnh/thành, phường/xã) đều là Select2.
//  - Form SỬA dùng đúng các luật như form THÊM (cùng useLoiForm + utils/kiemTra.js, khớp backend KhachHangService).
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Select2 from '../common/Select2.vue'
import DateInput from '../common/DateInput.vue'
import DiaChiHanhChinhSelect from '../common/DiaChiHanhChinhSelect.vue'
import KhachHangAddressModal from '../KhachHangAddressModal.vue'
import { khachHangService } from '../../services/khachHangService'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { ghepDiaChi } from '../../utils/diaChi'
import { ngayTuApi } from '../../utils/ngay'
import {
  HUONG_DAN_TUOI_KHACH,
  LOI_DIA_CHI_CHI_TIET_CHU_SO,
  TUOI_KHACH_TOI_THIEU,
  chuanHoaHoTen,
  cat,
  khoaEmail,
  khoaSdt,
  loiEmail,
  loiHoTen,
  loiNgaySinh,
  loiSdt,
  ngaySinhMuonNhat,
  ngaySinhSomNhat,
  timBanTrung,
} from '../../utils/kiemTra'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const id = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit = computed(() => id.value !== null)

const dangTai = ref(true)
const dangLuu = ref(false)
const dsKhach = ref([]) // toàn bộ khách đã có: để báo trùng số điện thoại / email ngay khi gõ
const khach = ref(null) // khách đang sửa (bản ghi đầy đủ từ API)
const moDiaChi = ref(false)

const dsGioiTinh = [
  { value: 1, label: 'Nam' },
  { value: 0, label: 'Nữ' },
]
const dsTrangThai = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Ngừng hoạt động' },
]

const form = reactive({
  hoTen: '',
  sdt: '',
  email: '',
  ngaySinh: '',
  gioiTinh: 1,
  trangThai: 1,
  // Địa chỉ giao hàng đầu tiên (chỉ khi THÊM; khi sửa thì quản lý địa chỉ ở cửa sổ riêng)
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanh: '',
  phuongXa: '',
  diaChiCuThe: '',
})

// Tên / số điện thoại người nhận tự lấy theo khách hàng cho tới khi người dùng tự sửa
const tuSuaNguoiNhan = reactive({ ten: false, sdt: false })
watch(
  () => form.hoTen,
  (v) => {
    if (!tuSuaNguoiNhan.ten) form.tenNguoiNhan = v
  },
)
watch(
  () => form.sdt,
  (v) => {
    if (!tuSuaNguoiNhan.sdt) form.sdtNguoiNhan = v
  },
)

// Khóa các ngày không chọn được trên lịch (tương lai, chưa đủ 15 tuổi, quá 100 tuổi). Gõ tay ngày ngoài khoảng này vẫn báo lỗi dưới ô.
const ngayToiDa = ngaySinhMuonNhat(TUOI_KHACH_TOI_THIEU)
const ngayToiThieu = ngaySinhSomNhat()

/* ----- Luật kiểm tra: mỗi ô một câu, khớp backend KhachHangService.kiemTraThongTin ----- */
function tinhLoi() {
  const e = {}
  const ten = loiHoTen(form.hoTen, { thieu: 'Vui lòng nhập họ tên khách hàng.', toiDa: 100 })
  if (ten) e.hoTen = ten

  let sdt = loiSdt(form.sdt)
  if (!sdt) {
    const trung = timBanTrung(dsKhach.value, form.sdt, (k) => k.sdt, khoaSdt, id.value)
    if (trung) sdt = `Số điện thoại đã tồn tại (khách hàng ${trung.hoTen}).`
  }
  if (sdt) e.sdt = sdt

  let email = loiEmail(form.email)
  if (!email) {
    const trung = timBanTrung(dsKhach.value, form.email, (k) => k.email, khoaEmail, id.value)
    if (trung) email = `Email đã tồn tại (khách hàng ${trung.hoTen}).`
  }
  if (email) e.email = email

  // Ngày sinh không bắt buộc, nhưng đã khai thì phải từ đủ 15 tuổi (Bộ luật Dân sự 2015, Điều 21)
  const ns = loiNgaySinh(form.ngaySinh, TUOI_KHACH_TOI_THIEU, 'Khách hàng')
  if (ns) e.ngaySinh = ns

  if (![0, 1].includes(Number(form.gioiTinh))) e.gioiTinh = 'Giới tính không hợp lệ.'
  if (isEdit.value && ![0, 1].includes(Number(form.trangThai))) e.trangThai = 'Trạng thái không hợp lệ.'

  if (!isEdit.value) {
    if (!cat(form.tinhThanh)) e.tinhThanh = 'Chọn Tỉnh/Thành phố.'
    if (!cat(form.phuongXa)) e.phuongXa = 'Chọn Phường/Xã.'
    const chiTiet = cat(form.diaChiCuThe)
    if (!chiTiet) e.diaChiCuThe = 'Nhập địa chỉ cụ thể (số nhà, đường...).'
    else if (chiTiet.length > 255) e.diaChiCuThe = 'Địa chỉ cụ thể tối đa 255 ký tự.'
    else if (!/[\p{L}\p{Nd}]/u.test(chiTiet)) e.diaChiCuThe = LOI_DIA_CHI_CHI_TIET_CHU_SO
  }
  return e
}

const { loi, cham, sua, batDauGui, datLoiMayChu, focusLoiDau } = useLoiForm(tinhLoi)

/* ----- Tải dữ liệu ----- */
function napForm(k) {
  khach.value = k
  form.hoTen = k.hoTen ?? ''
  form.sdt = k.sdt ?? ''
  form.email = k.email ?? ''
  form.ngaySinh = ngayTuApi(k.ngaySinh)
  form.gioiTinh = k.gioiTinh === 0 ? 0 : 1
  form.trangThai = k.trangThai === 0 ? 0 : 1
}

onMounted(async () => {
  try {
    const [ds, chiTiet] = await Promise.all([khachHangService.getAll(), isEdit.value ? khachHangService.getById(id.value) : null])
    dsKhach.value = ds ?? []
    if (chiTiet) napForm(chiTiet)
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu khách hàng.')
    if (isEdit.value) return quayLai()
  } finally {
    dangTai.value = false
  }
})

async function taiLaiDiaChi() {
  // Sau khi thêm / sửa / xóa địa chỉ ở cửa sổ quản lý: lấy lại khách để thẻ "Địa chỉ" luôn đúng
  try {
    khach.value = await khachHangService.getById(id.value)
  } catch {
    /* giữ bản cũ */
  }
}

const diaChiMacDinh = computed(() => {
  const ds = khach.value?.diaChiList ?? []
  return ds.find((d) => d.laDiaChiMacDinh) ?? ds[0] ?? null
})

function quayLai() {
  router.push('/khach-hang')
}

/* ----- Lưu ----- */
async function luu() {
  if (dangLuu.value) return
  if (!batDauGui()) {
    focusLoiDau('#kh-form')
    return
  }
  const payload = {
    hoTen: chuanHoaHoTen(form.hoTen),
    sdt: cat(form.sdt),
    email: khoaEmail(form.email),
    ngaySinh: form.ngaySinh || null,
    gioiTinh: Number(form.gioiTinh),
    trangThai: isEdit.value ? Number(form.trangThai) : 1,
  }
  if (!isEdit.value) {
    // Địa chỉ giao hàng nhập ở form này được backend lưu thành địa chỉ MẶC ĐỊNH của khách hàng (địa chỉ sau sáp nhập: không có quận/huyện)
    Object.assign(payload, {
      tenNguoiNhan: chuanHoaHoTen(form.tenNguoiNhan),
      sdtNguoiNhan: cat(form.sdtNguoiNhan),
      tinhThanh: cat(form.tinhThanh),
      phuongXa: cat(form.phuongXa),
      diaChiCuThe: cat(form.diaChiCuThe),
    })
  }
  dangLuu.value = true
  try {
    if (isEdit.value) await khachHangService.update(id.value, payload)
    else await khachHangService.create(payload)
    toast.success(isEdit.value ? 'Đã cập nhật khách hàng.' : 'Đã tạo khách hàng mới.')
    quayLai()
  } catch (err) {
    // Backend trả lỗi theo từng ô (vd 409 trùng email) -> gắn xuống đúng ô; không có thì báo bằng thông báo chung
    if (datLoiMayChu(err)) {
      toast.error(err.message || 'Vui lòng kiểm tra lại các ô báo đỏ.')
      focusLoiDau('#kh-form')
    } else {
      toast.error(err.message || 'Không thể lưu khách hàng. Vui lòng thử lại.')
    }
  } finally {
    dangLuu.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <div class="kh-top-bar">
      <div class="d-flex align-items-center gap-3">
        <button type="button" class="ad-back" aria-label="Quay lại danh sách khách hàng" @click="quayLai">
          <i class="bi bi-arrow-left" aria-hidden="true"></i>
        </button>
        <div>
          <h1 class="ad-page-title m-0">{{ isEdit ? 'Chỉnh sửa khách hàng' : 'Thêm khách hàng mới' }}</h1>
          <p class="kh-top-sub">
            {{ isEdit ? 'Cập nhật hồ sơ cá nhân và sổ địa chỉ giao hàng của khách hàng.' : 'Điền thông tin liên hệ và địa chỉ giao hàng mặc định để tạo hồ sơ mới.' }}
          </p>
        </div>
      </div>
      <div v-if="isEdit && khach" class="d-flex align-items-center gap-2">
        <span class="ad-code">{{ khach.maKhachHang }}</span>
        <span class="ad-pill" :class="Number(form.trangThai) === 1 ? 'ad-pill-green' : 'ad-pill-red'">
          {{ Number(form.trangThai) === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
        </span>
      </div>
    </div>

    <section v-if="dangTai" class="ad-card"><div class="ad-empty">Đang tải dữ liệu…</div></section>

    <form v-else id="kh-form" novalidate @submit.prevent="luu">
      <div class="d-grid gap-3">
        <!-- ===== Thông tin cơ bản ===== -->
        <section class="ad-card" data-testid="kh-thong-tin">
          <div class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-person-vcard"></i></span>
            <h2 class="ad-card-title">Thông tin khách hàng</h2>
          </div>

          <div class="row g-3">
            <div class="col-md-6">
              <label class="ad-label" for="kh-ho-ten">Họ và tên <span class="ad-required">*</span></label>
              <input
                id="kh-ho-ten"
                v-model="form.hoTen"
                type="text"
                class="form-control ad-control"
                :class="{ 'is-invalid': loi.hoTen }"
                maxlength="100"
                placeholder="Nhập họ và tên"
                autocomplete="off"
                :aria-invalid="!!loi.hoTen"
                aria-describedby="kh-ho-ten-msg"
                @input="sua('hoTen')"
                @blur="cham('hoTen')"
              />
              <p v-if="loi.hoTen" id="kh-ho-ten-msg" class="ad-error">{{ loi.hoTen }}</p>
            </div>

            <div class="col-md-6">
              <label class="ad-label" for="kh-sdt">Số điện thoại <span class="ad-required">*</span></label>
              <input
                id="kh-sdt"
                v-model="form.sdt"
                type="text"
                inputmode="numeric"
                class="form-control ad-control"
                :class="{ 'is-invalid': loi.sdt }"
                maxlength="15"
                placeholder="Ví dụ: 0912345678"
                autocomplete="off"
                :aria-invalid="!!loi.sdt"
                aria-describedby="kh-sdt-msg"
                @input="sua('sdt')"
                @blur="cham('sdt')"
              />
              <p v-if="loi.sdt" id="kh-sdt-msg" class="ad-error">{{ loi.sdt }}</p>
            </div>

            <div class="col-md-6">
              <label class="ad-label" for="kh-email">Email <span class="ad-required">*</span></label>
              <input
                id="kh-email"
                v-model="form.email"
                type="text"
                inputmode="email"
                class="form-control ad-control"
                :class="{ 'is-invalid': loi.email }"
                maxlength="255"
                placeholder="Ví dụ: ten@gmail.com"
                autocomplete="off"
                :aria-invalid="!!loi.email"
                aria-describedby="kh-email-msg"
                @input="sua('email')"
                @blur="cham('email')"
              />
              <p v-if="loi.email" id="kh-email-msg" class="ad-error">{{ loi.email }}</p>
            </div>

            <div :class="isEdit ? 'col-md-6' : 'col-md-3'">
              <label class="ad-label" for="kh-ngay-sinh">Ngày sinh</label>
              <DateInput
                id="kh-ngay-sinh"
                v-model="form.ngaySinh"
                class="form-control ad-control"
                :class="{ 'is-invalid': loi.ngaySinh }"
                :min="ngayToiThieu"
                :max="ngayToiDa"
                :aria-invalid="!!loi.ngaySinh"
                aria-describedby="kh-ngay-sinh-msg"
                @update:model-value="sua('ngaySinh')"
                @blur="cham('ngaySinh')"
              />
              <p v-if="loi.ngaySinh" id="kh-ngay-sinh-msg" class="ad-error">{{ loi.ngaySinh }}</p>
            </div>

            <div :class="isEdit ? 'col-md-6' : 'col-md-3'">
              <span class="ad-label d-block">Giới tính</span>
              <div id="kh-gioi-tinh" class="kh-gender-radios" role="radiogroup" aria-label="Giới tính">
                <label
                  v-for="gt in dsGioiTinh"
                  :key="gt.value"
                  class="kh-radio-item"
                  :class="{ 'is-checked': Number(form.gioiTinh) === gt.value }"
                >
                  <input
                    :id="gt.value === 1 ? 'kh-gioi-tinh-nam' : 'kh-gioi-tinh-nu'"
                    v-model="form.gioiTinh"
                    type="radio"
                    name="kh-gioi-tinh"
                    :value="gt.value"
                    class="kh-radio-input"
                    @change="sua('gioiTinh')"
                  />
                  <span>{{ gt.label }}</span>
                </label>
              </div>
              <p v-if="loi.gioiTinh" class="ad-error">{{ loi.gioiTinh }}</p>
            </div>

            <div v-if="isEdit" class="col-md-6">
              <label class="ad-label" for="kh-trang-thai">Trạng thái</label>
              <Select2
                id="kh-trang-thai"
                v-model="form.trangThai"
                :options="dsTrangThai"
                :min-search="99"
                class="ad-control"
                :invalid="!!loi.trangThai"
                @update:model-value="sua('trangThai')"
              />
              <p v-if="loi.trangThai" class="ad-error">{{ loi.trangThai }}</p>
            </div>
          </div>
        </section>

        <!-- ===== Địa chỉ giao hàng ===== -->
        <section v-if="!isEdit" class="ad-card" data-testid="kh-dia-chi-moi">
          <div class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
            <h2 class="ad-card-title">Địa chỉ giao hàng <span class="ad-required">*</span></h2>
          </div>

          <div class="row g-3">
            <!-- Địa chỉ hành chính sau sáp nhập (provinces.open-api.vn v2): Tỉnh/Thành phố -> Phường/Xã, không còn Quận/Huyện -->
            <DiaChiHanhChinhSelect
              v-model:tinh-thanh="form.tinhThanh"
              v-model:phuong-xa="form.phuongXa"
              id-prefix="kh-dia-chi"
              required
              col-class="col-md-6"
              select-class="ad-control"
              label-class="ad-label"
              :error-tinh="loi.tinhThanh"
              :error-phuong="loi.phuongXa"
              @update:tinh-thanh="sua('tinhThanh')"
              @update:phuong-xa="sua('phuongXa')"
            />

            <div class="col-12">
              <label class="ad-label" for="kh-dia-chi-cu-the">Địa chỉ cụ thể <span class="ad-required">*</span></label>
              <input
                id="kh-dia-chi-cu-the"
                v-model="form.diaChiCuThe"
                type="text"
                class="form-control ad-control"
                :class="{ 'is-invalid': loi.diaChiCuThe }"
                maxlength="255"
                placeholder="Số nhà, tên đường..."
                autocomplete="off"
                :aria-invalid="!!loi.diaChiCuThe"
                aria-describedby="kh-dia-chi-cu-the-msg"
                @input="sua('diaChiCuThe')"
                @blur="cham('diaChiCuThe')"
              />
              <p v-if="loi.diaChiCuThe" id="kh-dia-chi-cu-the-msg" class="ad-error">{{ loi.diaChiCuThe }}</p>
            </div>
          </div>

          <div class="kh-card-footer">
            <button type="button" class="ad-btn kh-footer-btn" :disabled="dangLuu" @click="quayLai">Hủy</button>
            <button type="submit" class="ad-btn ad-btn-primary kh-footer-btn" :disabled="dangLuu">
              <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
              <i v-else class="bi bi-save" aria-hidden="true"></i>
              Tạo khách hàng
            </button>
          </div>
        </section>

        <section v-else class="ad-card" data-testid="kh-dia-chi-hien-co">
          <div class="kh-dia-chi-head">
            <div class="ad-card-head mb-0">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
              <h2 class="ad-card-title">Địa chỉ giao hàng</h2>
            </div>
            <button type="button" class="ad-btn" @click="moDiaChi = true">
              <i class="bi bi-geo-alt-fill" aria-hidden="true"></i> Quản lý địa chỉ
            </button>
          </div>

          <div v-if="diaChiMacDinh" class="kh-dia-chi-box">
            <div class="kh-dia-chi-meta">
              <strong class="kh-dia-chi-ten">{{ diaChiMacDinh.tenNguoiNhan }}</strong>
              <span class="kh-dia-chi-sep">·</span>
              <span class="kh-dia-chi-sdt">{{ diaChiMacDinh.sdtNguoiNhan }}</span>
              <span class="ad-pill ad-pill-green">Mặc định</span>
            </div>
            <p class="kh-dia-chi-text">
              <i class="bi bi-pin-map me-1 text-muted" aria-hidden="true"></i>{{ ghepDiaChi(diaChiMacDinh) }}
            </p>
          </div>
          <div v-else class="kh-dia-chi-empty">
            <i class="bi bi-geo-alt" aria-hidden="true"></i>
            <span>Khách hàng chưa có địa chỉ nào. Bấm “Quản lý địa chỉ” để thêm địa chỉ giao hàng.</span>
          </div>

          <div class="kh-card-footer">
            <button type="button" class="ad-btn kh-footer-btn" :disabled="dangLuu" @click="quayLai">Hủy</button>
            <button type="submit" class="ad-btn ad-btn-primary kh-footer-btn" :disabled="dangLuu">
              <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
              <i v-else class="bi bi-save" aria-hidden="true"></i>
              Lưu thay đổi
            </button>
          </div>
        </section>
      </div>
    </form>

    <KhachHangAddressModal v-if="moDiaChi && khach" :customer="khach" @close="moDiaChi = false" @address-updated="taiLaiDiaChi" />
  </div>
</template>

<style scoped>
.kh-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.kh-top-sub {
  margin: 0.15rem 0 0;
  font-size: 0.85rem;
  color: var(--fs-muted, #5b6573);
}
.kh-dia-chi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.kh-dia-chi-box {
  padding: 0.9rem 1.1rem;
  border: 1px solid #ccebd7;
  border-radius: 12px;
  background: #f6fcf8;
}
.kh-dia-chi-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-bottom: 0.35rem;
  font-size: 0.92rem;
}
.kh-dia-chi-ten {
  color: var(--fs-ink, #14213d);
}
.kh-dia-chi-sep {
  color: #9ca3af;
}
.kh-dia-chi-sdt {
  color: var(--fs-muted, #5b6573);
  font-weight: 500;
}
.kh-dia-chi-text {
  margin: 0;
  font-size: 0.88rem;
  color: #374151;
  line-height: 1.5;
}
.kh-dia-chi-empty {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.85rem 1rem;
  border: 1px dashed var(--fs-line, #e6eaf0);
  border-radius: 12px;
  background: #f8fafc;
  font-size: 0.88rem;
  color: var(--fs-muted, #5b6573);
}
.kh-gender-radios {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
  min-height: 42px;
}
.kh-radio-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  height: 42px;
  margin: 0;
  padding: 0 0.85rem;
  border: 1px solid var(--fs-line, #dce3ec);
  border-radius: 10px;
  background: #fff;
  font-size: 0.92rem;
  font-weight: 500;
  color: var(--fs-ink, #14213d);
  cursor: pointer;
  user-select: none;
  transition: border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
}
.kh-radio-item:hover {
  border-color: #9dc5f8;
  background: #f8fbff;
}
.kh-radio-item.is-checked {
  border-color: var(--fs-primary, #0977ec);
  background: #f0f7ff;
  color: var(--fs-primary-dark, #075fc0);
  font-weight: 600;
}
.kh-radio-input {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  margin: 0;
  border: 2px solid #b8c4d4;
  border-radius: 50%;
  background: #fff;
  display: inline-grid;
  place-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.kh-radio-input::before {
  content: '';
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--fs-primary, #0977ec);
  transform: scale(0);
  transition: transform 0.15s ease;
}
.kh-radio-item:hover .kh-radio-input {
  border-color: var(--fs-primary, #0977ec);
}
.kh-radio-input:checked {
  border-color: var(--fs-primary, #0977ec);
}
.kh-radio-input:checked::before {
  transform: scale(1);
}
.kh-radio-input:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(9, 119, 236, 0.18);
}
.kh-card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.35rem;
  padding-top: 1.1rem;
  border-top: 1px solid var(--fs-line-soft, #eef2f7);
}
.kh-footer-btn {
  min-width: 145px;
  justify-content: center;
}
</style>
