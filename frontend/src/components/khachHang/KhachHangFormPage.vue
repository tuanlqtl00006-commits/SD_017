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
import { hinhAnhService } from '../../services/hinhAnhService'
import { loiFileAnhDaiDien } from '../../utils/anhDaiDien'
import { ghepDiaChi } from '../../utils/diaChi'
import { getInitials } from '../../utils/format'
import { ngayTuApi } from '../../utils/ngay'
import {
  HUONG_DAN_TUOI_KHACH,
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
  anhDaiDien: '', // đường dẫn ảnh đã tải lên (/api/uploads/...), để trống = chưa có ảnh
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
    const tenNhan = loiHoTen(form.tenNguoiNhan, { thieu: 'Vui lòng nhập họ tên người nhận.', toiDa: 100 })
    if (tenNhan) e.tenNguoiNhan = tenNhan
    const sdtNhan = loiSdt(form.sdtNguoiNhan, 'Vui lòng nhập số điện thoại người nhận.')
    if (sdtNhan) e.sdtNguoiNhan = sdtNhan
    if (!cat(form.tinhThanh)) e.tinhThanh = 'Chọn Tỉnh/Thành phố.'
    if (!cat(form.phuongXa)) e.phuongXa = 'Chọn Phường/Xã.'
    const chiTiet = cat(form.diaChiCuThe)
    if (!chiTiet) e.diaChiCuThe = 'Nhập địa chỉ cụ thể (số nhà, đường...).'
    else if (chiTiet.length > 255) e.diaChiCuThe = 'Địa chỉ cụ thể tối đa 255 ký tự.'
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
  form.anhDaiDien = k.anhDaiDien ?? ''
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

const chuAvatar = computed(() => (cat(form.hoTen) ? getInitials(chuanHoaHoTen(form.hoTen)) : 'KH'))

/* ----- Ảnh đại diện: chọn file -> tải lên ngay (POST /api/hinh-anh/upload) -> giữ đường dẫn trong form, lưu cùng khách hàng ----- */
const inputAnh = ref(null)
const dangTaiAnh = ref(false)
const loiAnh = ref('')
const anhHong = ref(false) // đường dẫn ảnh không mở được -> quay về hiện chữ cái đầu
watch(
  () => form.anhDaiDien,
  () => {
    anhHong.value = false
  },
)
const hienAnh = computed(() => !!form.anhDaiDien && !anhHong.value)

function moChonAnh() {
  if (inputAnh.value && !dangTaiAnh.value) inputAnh.value.click()
}

async function chonAnh(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // để lần sau chọn lại đúng file đó vẫn nhận
  if (!file) return
  loiAnh.value = loiFileAnhDaiDien(file)
  if (loiAnh.value) return
  dangTaiAnh.value = true
  try {
    form.anhDaiDien = await hinhAnhService.upload(file)
  } catch (err) {
    loiAnh.value = err.message || 'Không tải được ảnh. Vui lòng thử lại.'
  } finally {
    dangTaiAnh.value = false
  }
}

function boAnh() {
  form.anhDaiDien = ''
  loiAnh.value = ''
}

function quayLai() {
  router.push('/khach-hang')
}

/* ----- Lưu ----- */
async function luu() {
  if (dangLuu.value || dangTaiAnh.value) return
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
    // Luôn gửi: chuỗi rỗng = không có ảnh (khi sửa nghĩa là bỏ ảnh cũ)
    anhDaiDien: cat(form.anhDaiDien),
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
    <div>
      <button type="button" class="ad-back" aria-label="Quay lại danh sách khách hàng" @click="quayLai">
        <i class="bi bi-arrow-left" aria-hidden="true"></i>
      </button>
    </div>

    <section v-if="dangTai" class="ad-card"><div class="ad-empty">Đang tải dữ liệu…</div></section>

    <form v-else id="kh-form" novalidate @submit.prevent="luu">
      <div class="row g-3 align-items-start">
        <!-- Thẻ hồ sơ: chữ cái đầu + tên + email đang nhập -->
        <div class="col-12 col-xl-3">
          <aside class="ad-card text-center ad-profile-card" data-testid="kh-ho-so">
            <span class="ad-avatar ad-avatar-xl ad-avatar-soft" :aria-hidden="hienAnh ? null : 'true'">
              <img v-if="hienAnh" :src="form.anhDaiDien" :alt="`Ảnh đại diện của ${cat(form.hoTen) || 'khách hàng'}`" data-testid="kh-anh-dai-dien" @error="anhHong = true" />
              <template v-else>{{ chuAvatar }}</template>
            </span>
            <div class="mt-2 d-flex justify-content-center flex-wrap gap-2">
              <button type="button" class="ad-btn" :disabled="dangTaiAnh || dangLuu" data-testid="kh-chon-anh" @click="moChonAnh">
                <span v-if="dangTaiAnh" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <i v-else class="bi bi-camera" aria-hidden="true"></i>
                {{ dangTaiAnh ? 'Đang tải ảnh…' : form.anhDaiDien ? 'Đổi ảnh' : 'Tải ảnh lên' }}
              </button>
              <button v-if="form.anhDaiDien && !dangTaiAnh" type="button" class="ad-btn" data-testid="kh-bo-anh" @click="boAnh">
                <i class="bi bi-x-lg" aria-hidden="true"></i> Bỏ ảnh
              </button>
            </div>
            <input
              id="kh-file-anh"
              ref="inputAnh"
              type="file"
              class="d-none"
              accept="image/jpeg,image/png,image/webp,image/gif"
              data-testid="kh-file-anh"
              @change="chonAnh"
            />
            <p v-if="loiAnh" class="ad-error mt-2" role="alert" data-testid="kh-loi-anh">{{ loiAnh }}</p>
            <p v-else class="ad-hint mt-2">Ảnh JPG, PNG, WEBP hoặc GIF, tối đa 5 MB (không bắt buộc).</p>
            <h2 class="ad-card-title mt-3">{{ cat(form.hoTen) || 'Khách hàng mới' }}</h2>
            <p class="ad-card-sub">{{ cat(form.email) || 'Chưa nhập email' }}</p>
            <template v-if="isEdit && khach">
              <p class="ad-card-sub mt-3">Mã khách hàng</p>
              <span class="ad-code">{{ khach.maKhachHang }}</span>
              <div class="mt-3">
                <span class="ad-pill" :class="Number(form.trangThai) === 1 ? 'ad-pill-green' : 'ad-pill-red'">
                  {{ Number(form.trangThai) === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
                </span>
              </div>
            </template>
            <p v-else class="ad-card-sub mt-3">Mã khách hàng do hệ thống tự sinh.</p>
          </aside>
        </div>

        <div class="col-12 col-xl-9 d-grid gap-3">
          <!-- ===== Thông tin cơ bản ===== -->
          <section class="ad-card" data-testid="kh-thong-tin">
            <div class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-person"></i></span>
              <div>
                <h2 class="ad-card-title">{{ isEdit ? 'Chỉnh sửa khách hàng' : 'Thêm khách hàng' }}</h2>
                <p class="ad-card-sub">Họ tên, liên hệ và thông tin cá nhân. Các ô có dấu <span class="ad-required">*</span> là bắt buộc.</p>
              </div>
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
                <p v-else id="kh-sdt-msg" class="ad-hint">10 chữ số, đầu số 03, 05, 07, 08 hoặc 09.</p>
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

              <div class="col-md-6">
                <label class="ad-label" for="kh-gioi-tinh">Giới tính</label>
                <Select2
                  id="kh-gioi-tinh"
                  v-model="form.gioiTinh"
                  :options="dsGioiTinh"
                  :min-search="99"
                  class="ad-control"
                  :invalid="!!loi.gioiTinh"
                  @update:model-value="sua('gioiTinh')"
                />
                <p v-if="loi.gioiTinh" class="ad-error">{{ loi.gioiTinh }}</p>
              </div>

              <div class="col-md-6">
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
                <p v-else id="kh-ngay-sinh-msg" class="ad-hint" data-testid="kh-huong-dan-tuoi">{{ HUONG_DAN_TUOI_KHACH }}</p>
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
              <div>
                <h2 class="ad-card-title">Địa chỉ giao hàng <span class="ad-required">*</span></h2>
                <p class="ad-card-sub">Địa chỉ mặc định của khách hàng mới (có thể thêm địa chỉ khác sau khi tạo).</p>
              </div>
            </div>

            <div class="row g-3">
              <div class="col-md-6">
                <label class="ad-label" for="kh-nguoi-nhan">Họ tên người nhận <span class="ad-required">*</span></label>
                <input
                  id="kh-nguoi-nhan"
                  v-model="form.tenNguoiNhan"
                  type="text"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.tenNguoiNhan }"
                  maxlength="100"
                  placeholder="Mặc định là tên khách hàng"
                  autocomplete="off"
                  :aria-invalid="!!loi.tenNguoiNhan"
                  aria-describedby="kh-nguoi-nhan-msg"
                  @input="tuSuaNguoiNhan.ten = true; sua('tenNguoiNhan')"
                  @blur="cham('tenNguoiNhan')"
                />
                <p v-if="loi.tenNguoiNhan" id="kh-nguoi-nhan-msg" class="ad-error">{{ loi.tenNguoiNhan }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="kh-sdt-nguoi-nhan">Số điện thoại người nhận <span class="ad-required">*</span></label>
                <input
                  id="kh-sdt-nguoi-nhan"
                  v-model="form.sdtNguoiNhan"
                  type="text"
                  inputmode="numeric"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.sdtNguoiNhan }"
                  maxlength="15"
                  placeholder="Mặc định là số điện thoại khách hàng"
                  autocomplete="off"
                  :aria-invalid="!!loi.sdtNguoiNhan"
                  aria-describedby="kh-sdt-nguoi-nhan-msg"
                  @input="tuSuaNguoiNhan.sdt = true; sua('sdtNguoiNhan')"
                  @blur="cham('sdtNguoiNhan')"
                />
                <p v-if="loi.sdtNguoiNhan" id="kh-sdt-nguoi-nhan-msg" class="ad-error">{{ loi.sdtNguoiNhan }}</p>
              </div>

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
          </section>

          <section v-else class="ad-card" data-testid="kh-dia-chi-hien-co">
            <div class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
              <div>
                <h2 class="ad-card-title">Địa chỉ giao hàng</h2>
                <p class="ad-card-sub">{{ khach?.diaChiList?.length || 0 }} địa chỉ. Thêm, sửa, xóa và đặt địa chỉ mặc định trong cửa sổ quản lý địa chỉ.</p>
              </div>
            </div>
            <p v-if="diaChiMacDinh" class="kh-dia-chi-text">
              <strong>{{ diaChiMacDinh.tenNguoiNhan }}</strong> · {{ diaChiMacDinh.sdtNguoiNhan }}<br />
              {{ ghepDiaChi(diaChiMacDinh) }}
            </p>
            <p v-else class="ad-hint">Khách hàng chưa có địa chỉ nào.</p>
            <button type="button" class="ad-btn mt-2" @click="moDiaChi = true">
              <i class="bi bi-geo-alt-fill" aria-hidden="true"></i> Quản lý địa chỉ
            </button>
          </section>

          <div class="kh-footer">
            <button type="button" class="ad-btn" :disabled="dangLuu" @click="quayLai">Hủy</button>
            <button type="submit" class="ad-btn ad-btn-primary" :disabled="dangLuu || dangTaiAnh">
              <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
              <i v-else class="bi bi-save" aria-hidden="true"></i>
              {{ isEdit ? 'Lưu thay đổi' : 'Tạo khách hàng' }}
            </button>
          </div>
        </div>
      </div>
    </form>

    <KhachHangAddressModal v-if="moDiaChi && khach" :customer="khach" @close="moDiaChi = false" @address-updated="taiLaiDiaChi" />
  </div>
</template>

<style scoped>
.kh-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
.kh-dia-chi-text {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.55;
}
</style>
