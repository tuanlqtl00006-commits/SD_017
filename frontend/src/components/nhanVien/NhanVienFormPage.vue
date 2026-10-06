<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import { GIOI_TINH, TRANG_THAI, taoMaNhanVien, tinhTuoi } from '../../constants/nhanVien'
import { nhanVienService } from '../../services/nhanVienService'
import { useToast } from '../../composables/useToast'
import { avatarColors, formatDate, getInitials, toIso, todayIso } from '../../utils/format'

/*
 * Trang THÊM nhân viên (/nhan-vien/them) và CHI TIẾT / SỬA nhân viên (/nhan-vien/:id), giống video:
 * nút quay lại, thẻ hồ sơ bên trái, các ô nhập bên phải, nút lưu ở cuối.
 *
 * QUY ĐỊNH KHI SỬA NHÂN VIÊN
 *  - Được sửa : họ tên, giới tính, ngày sinh, số điện thoại, địa chỉ, vai trò.
 *  - Không sửa: mã nhân viên (hệ thống cấp), email (là tài khoản đăng nhập),
 *               ngày vào làm (mốc tuyển dụng), mật khẩu (nhập lúc thêm mới, không hiện ở trang sửa).
 *  - Khóa / mở khóa tài khoản: nút ở thẻ "Trạng thái tài khoản" (đổi cột trạng thái, không xóa dữ liệu).
 * Khi thêm mới thì nhập đầy đủ tất cả các ô.
 */
const route = useRoute()
const router = useRouter()
const toast = useToast()

const idXem = route.params.id ? Number(route.params.id) : null // null: thêm mới; có số: xem / sửa nhân viên này
const isEdit = idXem !== null

const tatCa = ref([]) // toàn bộ nhân viên, dùng để kiểm tra trùng và sinh mã
const vaiTro = ref([]) // [{ id, ten }] lấy từ bảng vai_tro
const item = ref(null) // nhân viên đang xem / sửa (null khi thêm mới)

const dangTai = ref(true)
const loiTai = ref('') // có chữ: tải lỗi hoặc không thấy nhân viên
const dangLuu = ref(false)

const form = reactive({
  hoTen: '',
  email: '',
  soDienThoai: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  diaChi: '',
  ngayVaoLam: todayIso(),
  idVaiTro: null,
  matKhau: '',
})

const maNhanVien = computed(() => (item.value ? item.value.ma : taoMaNhanVien(tatCa.value)))

// Chép dữ liệu nhân viên vào form (dùng lúc mở trang và sau khi khóa / mở khóa)
function napForm(nv) {
  form.hoTen = nv.hoTen
  form.email = nv.email
  form.soDienThoai = nv.soDienThoai
  form.gioiTinh = nv.gioiTinh ?? 'Nam'
  form.ngaySinh = nv.ngaySinh ?? ''
  form.diaChi = nv.diaChi
  form.ngayVaoLam = nv.ngayVaoLam ?? ''
  form.idVaiTro = nv.idVaiTro
}

async function taiDuLieu() {
  try {
    const [ds, dsVaiTro] = await Promise.all([nhanVienService.getAll(), nhanVienService.getVaiTro()])
    tatCa.value = ds
    vaiTro.value = dsVaiTro
    if (isEdit) {
      const nv = ds.find((x) => x.id === idXem)
      if (!nv) {
        loiTai.value = 'Không tìm thấy nhân viên này.'
        return
      }
      item.value = nv
      napForm(nv)
    } else {
      // Mặc định vai trò "Nhân viên" (vai trò không phải quản lý); nếu không có thì lấy vai trò đầu tiên.
      form.idVaiTro = (dsVaiTro.find((v) => v.ten !== 'Quản lý') ?? dsVaiTro[0])?.id ?? null
    }
  } catch (loi) {
    loiTai.value = loi.message || 'Không tải được dữ liệu. Vui lòng thử lại.'
    toast.error(loiTai.value)
  }
}

onMounted(async () => {
  await taiDuLieu()
  dangTai.value = false
  await nextTick()
  document.getElementById('nv-ho-ten')?.focus()
})

/* ----- Kiểm tra dữ liệu (validate) ----- */

const errors = reactive({})
const daBamLuu = ref(false) // chỉ hiện lỗi sau khi bấm Lưu lần đầu
const hienMatKhau = ref(false)

// Ngày sinh tối đa: đủ 18 tuổi tính đến hôm nay
const maxBirthDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 18)
  return toIso(d)
})

function daDung(field, value) {
  const needle = String(value).trim().toLowerCase()
  return tatCa.value.some((e) => e.id !== item.value?.id && String(e[field]).toLowerCase() === needle)
}

function kiemTraForm() {
  const e = {}

  const hoTen = String(form.hoTen).trim()
  if (!hoTen) e.hoTen = 'Nhập họ tên.'
  else if (hoTen.length < 2 || hoTen.length > 60) e.hoTen = 'Họ tên từ 2 đến 60 ký tự.'

  // Email chỉ nhập lúc thêm mới; khi sửa email bị khóa nên không cần kiểm tra
  if (!isEdit) {
    const email = String(form.email).trim()
    if (!email) e.email = 'Nhập email.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) e.email = 'Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.'
    else if (daDung('email', email)) e.email = 'Email đã được sử dụng.'
  }

  const sdt = String(form.soDienThoai).trim()
  if (!sdt) e.soDienThoai = 'Nhập số điện thoại.'
  else if (!/^0[35789]\d{8}$/.test(sdt)) e.soDienThoai = 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.'
  else if (daDung('soDienThoai', sdt)) e.soDienThoai = 'Số điện thoại đã được sử dụng.'

  if (form.ngaySinh && tinhTuoi(form.ngaySinh) < 18) e.ngaySinh = 'Nhân viên phải từ đủ 18 tuổi.'

  if (!form.idVaiTro) e.idVaiTro = 'Chọn vai trò.'

  // Ngày vào làm phải sau khi đủ 18 tuổi.
  // Khi sửa, ngày vào làm bị khóa nên lỗi được báo ở ô ngày sinh (ô duy nhất người dùng sửa được).
  if (form.ngayVaoLam && form.ngaySinh && !e.ngaySinh) {
    const [y, m, d] = form.ngaySinh.split('-').map(Number)
    const dau18 = toIso(new Date(y + 18, m - 1, d))
    if (form.ngayVaoLam < dau18) {
      if (isEdit) e.ngaySinh = `Ngày sinh không hợp lệ: nhân viên phải đủ 18 tuổi vào ngày vào làm (${formatDate(form.ngayVaoLam)}).`
      else e.ngayVaoLam = 'Ngày vào làm phải sau khi nhân viên đủ 18 tuổi.'
    }
  }

  const diaChi = String(form.diaChi).trim()
  if (!diaChi) e.diaChi = 'Nhập địa chỉ.'
  else if (diaChi.length > 255) e.diaChi = 'Địa chỉ tối đa 255 ký tự.'

  // Mật khẩu chỉ nhập lúc thêm mới (khi sửa không có ô mật khẩu).
  if (!isEdit) {
    if (!form.matKhau) e.matKhau = 'Nhập mật khẩu cho nhân viên mới.'
    else if (form.matKhau.length < 8 || form.matKhau.length > 50) e.matKhau = 'Mật khẩu từ 8 đến 50 ký tự.'
  }

  return e
}

function capNhatLoi() {
  const e = kiemTraForm()
  Object.keys(errors).forEach((k) => delete errors[k])
  Object.assign(errors, e)
  return e
}

// Sau lần bấm lưu đầu tiên, báo lỗi theo thời gian thực để người dùng thấy ngay khi đã sửa đúng.
watch(form, () => {
  if (daBamLuu.value) capNhatLoi()
})

/* ----- Lưu ----- */

async function guiForm() {
  if (dangLuu.value) return
  daBamLuu.value = true
  const e = capNhatLoi()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#nv-form .is-invalid')?.focus())
    return
  }
  const payload = {
    hoTen: String(form.hoTen).trim().replace(/\s+/g, ' '),
    soDienThoai: String(form.soDienThoai).trim(),
    gioiTinh: form.gioiTinh,
    ngaySinh: form.ngaySinh || null,
    diaChi: String(form.diaChi).trim(),
    idVaiTro: form.idVaiTro,
  }
  // Email, ngày vào làm, mật khẩu: chỉ gửi khi THÊM MỚI. Khi sửa thì không gửi (giữ nguyên giá trị cũ).
  if (!isEdit) {
    payload.email = String(form.email).trim().toLowerCase()
    payload.ngayVaoLam = form.ngayVaoLam || null
    payload.matKhau = form.matKhau
  }
  dangLuu.value = true
  try {
    if (isEdit) {
      await nhanVienService.update(idXem, payload)
      toast.success('Đã lưu thay đổi nhân viên.')
    } else {
      await nhanVienService.create(payload)
      toast.success('Đã thêm nhân viên.')
    }
    router.push('/nhan-vien') // về danh sách, nhân viên mới nằm ở đầu danh sách
  } catch (loi) {
    toast.error(loi.message || 'Không thể lưu nhân viên. Vui lòng thử lại.')
  } finally {
    dangLuu.value = false
  }
}

/* ----- Khóa / mở khóa tài khoản (chỉ ở trang chi tiết) ----- */

const hoiKhoa = ref(false) // đang hiện hộp xác nhận
const dangDoiTrangThai = ref(false)

const noiDungXacNhan = computed(() => {
  const e = item.value
  if (!e) return {}
  return e.hoatDong
    ? {
        title: 'Khóa tài khoản?',
        message: `${e.hoTen} (${e.ma}) sẽ bị ngưng hoạt động và không thể đăng nhập vào hệ thống. Hồ sơ và lịch sử vẫn được giữ lại, bạn có thể mở khóa bất cứ lúc nào.`,
        confirmText: 'Khóa tài khoản',
        variant: 'danger',
      }
    : {
        title: 'Mở khóa tài khoản?',
        message: `${e.hoTen} (${e.ma}) sẽ có thể đăng nhập và làm việc trở lại.`,
        confirmText: 'Mở khóa',
        variant: 'primary',
      }
})

// Backend chặn việc khóa / đổi vai trò quản lý đang hoạt động cuối cùng và báo lỗi bằng toast.
async function doiTrangThai() {
  const e = item.value
  dangDoiTrangThai.value = true
  try {
    await nhanVienService.toggleActive(e.id)
    toast.success(e.hoatDong ? `Đã khóa tài khoản ${e.hoTen}.` : `Đã mở khóa tài khoản ${e.hoTen}.`)
    hoiKhoa.value = false
    const ds = await nhanVienService.getAll()
    tatCa.value = ds
    item.value = ds.find((x) => x.id === idXem) ?? item.value
  } catch (loi) {
    toast.error(loi.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    dangDoiTrangThai.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <div class="d-flex align-items-center gap-3">
      <RouterLink to="/nhan-vien" class="ad-icon-btn text-decoration-none" title="Quay lại danh sách" aria-label="Quay lại danh sách nhân viên">
        <i class="bi bi-arrow-left" aria-hidden="true"></i>
      </RouterLink>
      <span v-if="isEdit && item" class="ad-hint m-0">Mã nhân viên: <strong>{{ item.ma }}</strong></span>
    </div>

    <section v-if="dangTai" class="ad-card"><div class="ad-empty">Đang tải dữ liệu…</div></section>
    <section v-else-if="loiTai" class="ad-card">
      <div class="ad-empty">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>{{ loiTai }}</strong>
        <RouterLink to="/nhan-vien">Quay lại danh sách</RouterLink>
      </div>
    </section>

    <form v-else id="nv-form" novalidate @submit.prevent="guiForm">
      <div class="row g-4 align-items-start">
        <!-- Cột trái: thẻ hồ sơ + trạng thái tài khoản -->
        <div class="col-lg-4">
          <section class="ad-card text-center">
            <span class="ad-avatar ad-avatar-lg mx-auto" :style="avatarColors(form.hoTen || '?')">{{ getInitials(form.hoTen) }}</span>
            <h2 class="ad-card-title mt-3 mb-1">{{ form.hoTen || 'Nhân viên mới' }}</h2>
            <p class="ad-hint m-0">{{ form.email || 'Chưa cập nhật email' }}</p>
            <span v-if="isEdit && item" class="ad-pill mt-2" :class="TRANG_THAI[item.hoatDong].cls">{{ TRANG_THAI[item.hoatDong].label }}</span>
            <p v-else class="ad-hint mt-2 mb-0">Ảnh đại diện hiển thị chữ cái đầu của họ tên.</p>
          </section>

          <section v-if="isEdit && item" class="ad-card mt-4">
            <h2 class="ad-card-title mb-2">Trạng thái tài khoản</h2>
            <button type="button" class="ad-btn w-100" :class="item.hoatDong ? 'text-danger' : 'text-success'" @click="hoiKhoa = true">
              <i class="bi" :class="item.hoatDong ? 'bi-lock' : 'bi-unlock'" aria-hidden="true"></i>
              {{ item.hoatDong ? 'Khóa tài khoản' : 'Mở khóa tài khoản' }}
            </button>
          </section>
        </div>

        <!-- Cột phải: thông tin cơ bản + địa chỉ -->
        <div class="col-lg-8">
          <section class="ad-card">
            <header class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-person"></i></span>
              <div>
                <h2 class="ad-card-title">Thông tin cơ bản</h2>
                <p class="ad-card-sub">Họ tên, email, liên hệ và tài khoản.</p>
              </div>
            </header>

            <p v-if="isEdit" class="ad-hint mt-3 mb-0">
              <i class="bi bi-info-circle" aria-hidden="true"></i>
              Có thể sửa: họ tên, số điện thoại, vai trò, ngày sinh, giới tính, địa chỉ. Các ô màu xám (mã nhân viên, email, ngày vào làm) không thể sửa.
            </p>

            <div class="row g-3 mt-1">
              <div class="col-md-6">
                <label class="ad-label" for="nv-ma">Mã nhân viên</label>
                <input id="nv-ma" type="text" class="form-control ad-control" :value="maNhanVien" readonly />
                <p class="ad-hint">{{ isEdit ? 'Không thể đổi mã sau khi tạo.' : 'Hệ thống tự cấp mã.' }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-ho-ten">Họ và tên <span class="ad-required">*</span></label>
                <input
                  id="nv-ho-ten"
                  v-model="form.hoTen"
                  type="text"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.hoTen }"
                  maxlength="60"
                  placeholder="Ví dụ: Nguyễn Văn An"
                  autocomplete="off"
                  :aria-invalid="!!errors.hoTen"
                />
                <p v-if="errors.hoTen" class="ad-error">{{ errors.hoTen }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-email">Email (dùng để đăng nhập) <span v-if="!isEdit" class="ad-required">*</span></label>
                <input
                  id="nv-email"
                  v-model="form.email"
                  type="email"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.email }"
                  :readonly="isEdit"
                  placeholder="ten@footstyle.vn"
                  autocomplete="off"
                  :aria-invalid="!!errors.email"
                />
                <p v-if="isEdit" class="ad-hint">Email là tài khoản đăng nhập nên không thể đổi.</p>
                <p v-if="errors.email" class="ad-error">{{ errors.email }}</p>
              </div>

              <!-- Mật khẩu chỉ nhập khi thêm mới; trang sửa không có ô này -->
              <div v-if="!isEdit" class="col-md-6">
                <label class="ad-label" for="nv-mat-khau">Mật khẩu <span class="ad-required">*</span></label>
                <div class="ad-affix">
                  <input
                    id="nv-mat-khau"
                    v-model="form.matKhau"
                    :type="hienMatKhau ? 'text' : 'password'"
                    class="form-control ad-control has-btn"
                    :class="{ 'is-invalid': errors.matKhau }"
                    maxlength="50"
                    placeholder="Từ 8 ký tự"
                    autocomplete="new-password"
                    :aria-invalid="!!errors.matKhau"
                  />
                  <button
                    type="button"
                    class="ad-affix-btn"
                    :title="hienMatKhau ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    :aria-label="hienMatKhau ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    @click="hienMatKhau = !hienMatKhau"
                  >
                    <i class="bi" :class="hienMatKhau ? 'bi-eye-slash' : 'bi-eye'" aria-hidden="true"></i>
                  </button>
                </div>
                <p v-if="errors.matKhau" class="ad-error">{{ errors.matKhau }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-sdt">Số điện thoại <span class="ad-required">*</span></label>
                <input
                  id="nv-sdt"
                  v-model="form.soDienThoai"
                  type="tel"
                  inputmode="numeric"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.soDienThoai }"
                  maxlength="10"
                  placeholder="0912345678"
                  autocomplete="off"
                  :aria-invalid="!!errors.soDienThoai"
                />
                <p v-if="errors.soDienThoai" class="ad-error">{{ errors.soDienThoai }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-vai-tro-form">Vai trò <span class="ad-required">*</span></label>
                <select
                  id="nv-vai-tro-form"
                  v-model="form.idVaiTro"
                  class="form-select ad-control"
                  :class="{ 'is-invalid': errors.idVaiTro }"
                  :aria-invalid="!!errors.idVaiTro"
                >
                  <option v-for="v in vaiTro" :key="v.id" :value="v.id">{{ v.ten }}</option>
                </select>
                <p v-if="errors.idVaiTro" class="ad-error">{{ errors.idVaiTro }}</p>
              </div>

              <div class="col-md-6">
                <span id="nv-gioi-tinh-label" class="ad-label">Giới tính <span class="ad-required">*</span></span>
                <div class="ad-segment" role="radiogroup" aria-labelledby="nv-gioi-tinh-label">
                  <label v-for="g in GIOI_TINH" :key="g" class="ad-segment-item" :class="{ active: form.gioiTinh === g }">
                    <input v-model="form.gioiTinh" type="radio" class="visually-hidden" name="gioiTinh" :value="g" />
                    {{ g }}
                  </label>
                </div>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-ngay-sinh">Ngày sinh</label>
                <input
                  id="nv-ngay-sinh"
                  v-model="form.ngaySinh"
                  type="date"
                  :max="maxBirthDate"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.ngaySinh }"
                  :aria-invalid="!!errors.ngaySinh"
                />
                <p v-if="errors.ngaySinh" class="ad-error">{{ errors.ngaySinh }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-ngay-vao-lam">Ngày vào làm</label>
                <input
                  id="nv-ngay-vao-lam"
                  v-model="form.ngayVaoLam"
                  type="date"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.ngayVaoLam }"
                  :readonly="isEdit"
                  :aria-invalid="!!errors.ngayVaoLam"
                />
                <p v-if="isEdit" class="ad-hint">Ngày vào làm là mốc tuyển dụng nên không thể đổi.</p>
                <p v-if="errors.ngayVaoLam" class="ad-error">{{ errors.ngayVaoLam }}</p>
              </div>
            </div>
          </section>

          <section class="ad-card mt-4">
            <header class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
              <div>
                <h2 class="ad-card-title">Địa chỉ</h2>
              </div>
            </header>
            <div class="mt-3">
              <label class="ad-label" for="nv-dia-chi">Địa chỉ <span class="ad-required">*</span></label>
              <textarea
                id="nv-dia-chi"
                v-model="form.diaChi"
                rows="2"
                maxlength="255"
                class="form-control ad-control"
                :class="{ 'is-invalid': errors.diaChi }"
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
                :aria-invalid="!!errors.diaChi"
              ></textarea>
              <p v-if="errors.diaChi" class="ad-error">{{ errors.diaChi }}</p>
            </div>
          </section>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <RouterLink to="/nhan-vien" class="ad-btn text-decoration-none">Hủy</RouterLink>
            <button type="submit" class="ad-btn ad-btn-primary" :disabled="dangLuu">
              <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
              {{ isEdit ? 'Lưu thay đổi' : 'Tạo nhân viên' }}
            </button>
          </div>
        </div>
      </div>
    </form>

    <ConfirmDialog v-if="hoiKhoa" v-bind="noiDungXacNhan" :loading="dangDoiTrangThai" @confirm="doiTrangThai" @cancel="hoiKhoa = false" />
  </div>
</template>
