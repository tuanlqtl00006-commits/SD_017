<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import DiaChiHanhChinhSelect from '../common/DiaChiHanhChinhSelect.vue'
import CccdScannerModal from './CccdScannerModal.vue'
import { GIOI_TINH, TRANG_THAI, taoMaTheoTen, tienToMaNhanVien, tinhTuoi } from '../../constants/nhanVien'
import { ghepDiaChi } from '../../utils/diaChi'
import { nhanVienService } from '../../services/nhanVienService'
import { useToast } from '../../composables/useToast'
import { avatarColors, formatDate, getInitials, toIso, todayIso } from '../../utils/format'

/*
 * Trang THÊM nhân viên (/nhan-vien/them) và CHI TIẾT / SỬA nhân viên (/nhan-vien/:id), giống video:
 * nút quay lại, thẻ hồ sơ bên trái, các ô nhập bên phải, nút lưu ở cuối.
 *
 * MÃ NHÂN VIÊN: tạo theo HỌ TÊN ĐẦY ĐỦ (Nguyễn Văn An -> AnNV01). Trang này hiện mã dự kiến ngay khi nhập tên;
 *   mã chính thức do backend cấp khi bấm "Tạo nhân viên".
 * VỊ TRÍ LÀM VIỆC: chọn nhiều vị trí đã thêm, hoặc thêm vị trí mới; không có chức năng xóa vị trí.
 * QUÉT CĂN CƯỚC (chỉ khi thêm mới): quét mã QR trên thẻ CCCD để điền sẵn họ tên, ngày sinh, giới tính, số CCCD.
 * ĐỊA CHỈ: nhiều địa chỉ (Tỉnh/Thành phố -> Phường/Xã sau sáp nhập), chọn 1 địa chỉ chính. Chỉ thêm / sửa / chọn lại, KHÔNG có nút xóa.
 *
 * QUY ĐỊNH KHI SỬA NHÂN VIÊN
 *  - Được sửa : họ tên, giới tính, ngày sinh, số điện thoại, số CCCD, các địa chỉ, vai trò, vị trí làm việc.
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
const viTriList = ref([]) // [{ id, ten }] các vị trí làm việc đã thêm

const dangTai = ref(true)
const loiTai = ref('') // có chữ: tải lỗi hoặc không thấy nhân viên
const dangLuu = ref(false)

const form = reactive({
  hoTen: '',
  email: '',
  soDienThoai: '',
  cccd: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  ngayVaoLam: todayIso(),
  idVaiTro: null,
  matKhau: '',
  idViTri: [], // các vị trí làm việc đang chọn
  diaChis: [], // [{ key, id (null = mới thêm), tinhThanh, phuongXa, diaChiCuThe }]
  chinhKey: null, // key của địa chỉ chính
})

// Mã nhân viên: sửa -> mã đã cấp; thêm mới -> mã dự kiến theo họ tên đầy đủ (trống khi chưa nhập tên)
const maNhanVien = computed(() => {
  if (item.value) return item.value.ma
  if (!tienToMaNhanVien(form.hoTen)) return ''
  return taoMaTheoTen(form.hoTen, tatCa.value.map((e) => e.ma))
})

/* ----- Nhiều địa chỉ ----- */
let keySeq = 0
const taoKey = () => `n${++keySeq}`
const hienDiaChi = (d) => ghepDiaChi(d)
const diaChiChinh = computed(() => form.diaChis.find((d) => d.key === form.chinhKey) ?? form.diaChis[0] ?? null)

const dcForm = reactive({ mo: false, key: null, tinhThanh: '', phuongXa: '', diaChiCuThe: '', loi: {} })
const diaChiTrenCccd = ref('') // địa chỉ ghi trên thẻ CCCD vừa quét (để tham khảo khi nhập địa chỉ cụ thể)

function moFormDiaChi(d = null) {
  dcForm.mo = true
  dcForm.key = d ? d.key : null
  dcForm.tinhThanh = d?.tinhThanh ?? ''
  dcForm.phuongXa = d?.phuongXa ?? ''
  dcForm.diaChiCuThe = d?.diaChiCuThe ?? ''
  dcForm.loi = {}
  nextTick(() => document.getElementById('nv-dc-chi-tiet')?.focus())
}
function huyFormDiaChi() {
  dcForm.mo = false
  dcForm.key = null
  dcForm.loi = {}
}
function luuFormDiaChi() {
  const loi = {}
  const chiTiet = String(dcForm.diaChiCuThe).trim().replace(/\s+/g, ' ')
  if (!dcForm.tinhThanh) loi.tinhThanh = 'Chọn Tỉnh/Thành phố.'
  if (!dcForm.phuongXa) loi.phuongXa = 'Chọn Phường/Xã.'
  if (!chiTiet) loi.diaChiCuThe = 'Nhập số nhà, tên đường...'
  else if (chiTiet.length > 255) loi.diaChiCuThe = 'Địa chỉ cụ thể tối đa 255 ký tự.'
  dcForm.loi = loi
  if (Object.keys(loi).length) return
  const du = { tinhThanh: dcForm.tinhThanh, phuongXa: dcForm.phuongXa, diaChiCuThe: chiTiet }
  if (dcForm.key) {
    const d = form.diaChis.find((x) => x.key === dcForm.key)
    if (d) Object.assign(d, du)
  } else {
    const moi = { key: taoKey(), id: null, ...du }
    form.diaChis.push(moi)
    if (!form.chinhKey) form.chinhKey = moi.key // địa chỉ đầu tiên tự là địa chỉ chính
  }
  huyFormDiaChi()
}
function dungDiaChiTrenCccd() {
  dcForm.diaChiCuThe = diaChiTrenCccd.value
}

// Chép dữ liệu nhân viên vào form (dùng lúc mở trang và sau khi khóa / mở khóa)
function napForm(nv) {
  form.hoTen = nv.hoTen
  form.email = nv.email
  form.soDienThoai = nv.soDienThoai
  form.cccd = nv.cccd ?? ''
  form.gioiTinh = nv.gioiTinh ?? 'Nam'
  form.ngaySinh = nv.ngaySinh ?? ''
  form.ngayVaoLam = nv.ngayVaoLam ?? ''
  form.idVaiTro = nv.idVaiTro
  form.idViTri = (nv.viTri ?? []).map((v) => v.id)
  // Nhân viên cũ chưa có danh sách địa chỉ thì dùng chuỗi địa chỉ cũ làm một địa chỉ chính
  const ds = nv.diaChis?.length ? nv.diaChis : nv.diaChi ? [{ id: null, tinhThanh: '', phuongXa: '', diaChiCuThe: nv.diaChi, macDinh: true }] : []
  form.diaChis = ds.map((d) => ({ key: d.id ? `d${d.id}` : taoKey(), id: d.id ?? null, tinhThanh: d.tinhThanh ?? '', phuongXa: d.phuongXa ?? '', diaChiCuThe: d.diaChiCuThe ?? '' }))
  form.chinhKey = (ds.findIndex((d) => d.macDinh) >= 0 ? form.diaChis[ds.findIndex((d) => d.macDinh)] : form.diaChis[0])?.key ?? null
}

async function taiDuLieu() {
  try {
    const [ds, dsVaiTro, dsViTri] = await Promise.all([nhanVienService.getAll(), nhanVienService.getVaiTro(), nhanVienService.getViTri()])
    tatCa.value = ds
    vaiTro.value = dsVaiTro
    viTriList.value = dsViTri
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

/* ----- Quét căn cước công dân ----- */
const hienQuet = ref(false)

// Điền các ô từ mã QR trên thẻ. Người dùng vẫn xem lại / sửa được trước khi lưu.
function apDungCccd(info) {
  form.cccd = info.soCccd
  if (info.hoTen) form.hoTen = info.hoTen
  if (info.gioiTinh) form.gioiTinh = info.gioiTinh
  if (info.ngaySinh) form.ngaySinh = info.ngaySinh
  diaChiTrenCccd.value = info.diaChi
  const trung = tatCa.value.find((e) => e.cccd === info.soCccd)
  if (trung) toast.error(`Số CCCD này đã thuộc nhân viên ${trung.hoTen} (${trung.ma}).`)
  else toast.success(`Đã điền thông tin từ căn cước của ${info.hoTen || 'nhân viên'}. Hãy kiểm tra lại rồi nhập email, mật khẩu, số điện thoại và địa chỉ.`)
  if (info.diaChi && !form.diaChis.length && !dcForm.mo) moFormDiaChi() // mở sẵn ô nhập địa chỉ, có gợi ý từ thẻ
  nextTick(() => document.getElementById('nv-email')?.scrollIntoView?.({ block: 'center', behavior: 'smooth' }))
}

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

  const cccd = String(form.cccd).trim()
  if (cccd && !/^\d{12}$/.test(cccd)) e.cccd = 'Số căn cước công dân gồm đúng 12 chữ số.'
  else if (cccd && daDung('cccd', cccd)) e.cccd = 'Số căn cước công dân đã được sử dụng.'

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

  if (dcForm.mo) e.diaChis = 'Bấm “Lưu địa chỉ” hoặc “Hủy” để hoàn tất địa chỉ đang nhập.'
  else if (!form.diaChis.length) e.diaChis = 'Thêm ít nhất một địa chỉ.'

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

/* ----- Vị trí làm việc: chọn nhiều, thêm mới, không xóa ----- */

const tenViTriMoi = ref('')
const dangThemViTri = ref(false)
const loiViTri = ref('')

const daChonViTri = (id) => form.idViTri.includes(id)
function chonViTri(id) {
  const i = form.idViTri.indexOf(id)
  if (i >= 0) form.idViTri.splice(i, 1)
  else form.idViTri.push(id)
}

async function themViTri() {
  loiViTri.value = ''
  const ten = tenViTriMoi.value.trim().replace(/\s+/g, ' ')
  if (!ten) {
    loiViTri.value = 'Nhập tên vị trí.'
    return
  }
  if (ten.length < 2 || ten.length > 50) {
    loiViTri.value = 'Tên vị trí từ 2 đến 50 ký tự.'
    return
  }
  // Đã có sẵn trong danh sách thì chỉ cần chọn lại
  const coSan = viTriList.value.find((v) => v.ten.toLowerCase() === ten.toLowerCase())
  if (coSan) {
    if (!daChonViTri(coSan.id)) form.idViTri.push(coSan.id)
    tenViTriMoi.value = ''
    return
  }
  dangThemViTri.value = true
  try {
    const moi = await nhanVienService.themViTri(ten)
    viTriList.value.push(moi)
    form.idViTri.push(moi.id)
    tenViTriMoi.value = ''
    toast.success(`Đã thêm vị trí "${moi.ten}".`)
  } catch (loi) {
    loiViTri.value = loi.message || 'Không thể thêm vị trí. Vui lòng thử lại.'
  } finally {
    dangThemViTri.value = false
  }
}

/* ----- Lưu ----- */

async function guiForm() {
  if (dangLuu.value) return
  daBamLuu.value = true
  const e = capNhatLoi()
  if (Object.keys(e).length) {
    nextTick(() => {
      const o = document.querySelector('#nv-form .is-invalid')
      if (o) o.focus()
      else document.getElementById('nv-dia-chi-card')?.scrollIntoView?.({ block: 'center', behavior: 'smooth' })
    })
    return
  }
  const payload = {
    hoTen: String(form.hoTen).trim().replace(/\s+/g, ' '),
    soDienThoai: String(form.soDienThoai).trim(),
    gioiTinh: form.gioiTinh,
    ngaySinh: form.ngaySinh || null,
    cccd: String(form.cccd).trim() || null,
    idVaiTro: form.idVaiTro,
    idViTri: [...form.idViTri],
    // Địa chỉ không có nút xóa; gửi cả danh sách, id = null là địa chỉ mới thêm
    diaChis: form.diaChis.map((d) => ({
      id: d.id,
      tinhThanh: d.tinhThanh || null,
      phuongXa: d.phuongXa || null,
      diaChiCuThe: d.diaChiCuThe,
      macDinh: d.key === (diaChiChinh.value?.key ?? null),
    })),
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
      const moi = await nhanVienService.create(payload)
      toast.success(moi?.ma ? `Đã thêm nhân viên ${moi.hoTen} với mã ${moi.ma}.` : 'Đã thêm nhân viên.')
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
              <button v-if="!isEdit" type="button" class="ad-btn ms-auto" @click="hienQuet = true">
                <i class="bi bi-qr-code-scan" aria-hidden="true"></i> Quét căn cước
              </button>
            </header>

            <p v-if="isEdit" class="ad-hint mt-3 mb-0">
              <i class="bi bi-info-circle" aria-hidden="true"></i>
              Có thể sửa: họ tên, số điện thoại, số căn cước, vai trò, vị trí làm việc, ngày sinh, giới tính, địa chỉ. Các ô màu xám (mã nhân viên, email, ngày vào làm) không thể sửa.
            </p>

            <div class="row g-3 mt-1">
              <div class="col-md-6">
                <label class="ad-label" for="nv-ma">Mã nhân viên</label>
                <input
                  id="nv-ma"
                  type="text"
                  class="form-control ad-control"
                  :value="maNhanVien"
                  :placeholder="isEdit ? '' : 'Tự tạo sau khi nhập họ tên'"
                  readonly
                />
                <p v-if="isEdit" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
                <p v-else-if="maNhanVien" class="ad-hint">Mã dự kiến theo họ tên đầy đủ; hệ thống cấp mã chính thức khi bạn bấm “Tạo nhân viên”.</p>
                <p v-else class="ad-hint">Mã được tạo theo họ tên đầy đủ, ví dụ Nguyễn Văn An → AnNV01.</p>
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
                <label class="ad-label" for="nv-cccd">Số căn cước công dân</label>
                <input
                  id="nv-cccd"
                  v-model="form.cccd"
                  type="text"
                  inputmode="numeric"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.cccd }"
                  maxlength="12"
                  placeholder="12 chữ số hoặc bấm “Quét căn cước”"
                  autocomplete="off"
                  :aria-invalid="!!errors.cccd"
                />
                <p v-if="errors.cccd" class="ad-error">{{ errors.cccd }}</p>
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

          <section id="nv-dia-chi-card" class="ad-card mt-4">
            <header class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
              <div>
                <h2 class="ad-card-title">Địa chỉ</h2>
                <p class="ad-card-sub">Thêm nhiều địa chỉ, chọn một địa chỉ chính.</p>
              </div>
              <button type="button" class="ad-btn ms-auto" :disabled="dcForm.mo" @click="moFormDiaChi()">
                <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm địa chỉ
              </button>
            </header>

            <div v-if="form.diaChis.length" class="nv-dc-list" role="radiogroup" aria-label="Chọn địa chỉ chính">
              <label v-for="d in form.diaChis" :key="d.key" class="nv-dc" :class="{ active: form.chinhKey === d.key }">
                <input v-model="form.chinhKey" type="radio" class="nv-dc-radio" name="nv-dia-chi-chinh" :value="d.key" />
                <span class="nv-dc-body">
                  <span class="nv-dc-text">{{ hienDiaChi(d) }}</span>
                  <span v-if="form.chinhKey === d.key" class="ad-pill ad-pill-green nv-dc-tag">Địa chỉ chính</span>
                </span>
                <button type="button" class="ad-icon-btn nv-dc-edit" title="Sửa địa chỉ" aria-label="Sửa địa chỉ này" :disabled="dcForm.mo" @click.prevent="moFormDiaChi(d)">
                  <i class="bi bi-pencil" aria-hidden="true"></i>
                </button>
              </label>
            </div>
            <p v-else-if="!dcForm.mo" class="ad-hint mt-2">Chưa có địa chỉ nào. Bấm “Thêm địa chỉ”.</p>

            <!-- Ô nhập địa chỉ (thêm mới hoặc sửa một địa chỉ) -->
            <div v-if="dcForm.mo" class="nv-dc-form">
              <h3 class="nv-dc-form-title">{{ dcForm.key ? 'Sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h3>
              <div class="row g-3">
                <DiaChiHanhChinhSelect
                  v-model:tinh-thanh="dcForm.tinhThanh"
                  v-model:phuong-xa="dcForm.phuongXa"
                  id-prefix="nv-dc"
                  required
                  col-class="col-md-6"
                  select-class="form-select ad-control"
                  label-class="ad-label"
                  :invalid-tinh="!!dcForm.loi.tinhThanh"
                  :invalid-phuong="!!dcForm.loi.phuongXa"
                />
                <div class="col-12">
                  <label class="ad-label" for="nv-dc-chi-tiet">Địa chỉ cụ thể <span class="ad-required">*</span></label>
                  <input
                    id="nv-dc-chi-tiet"
                    v-model="dcForm.diaChiCuThe"
                    type="text"
                    maxlength="255"
                    class="form-control ad-control"
                    :class="{ 'is-invalid': dcForm.loi.diaChiCuThe }"
                    placeholder="Số nhà, tên đường, thôn / tổ..."
                    autocomplete="off"
                    @keydown.enter.prevent="luuFormDiaChi"
                  />
                  <p v-if="dcForm.loi.tinhThanh || dcForm.loi.phuongXa" class="ad-error">{{ dcForm.loi.tinhThanh || dcForm.loi.phuongXa }}</p>
                  <p v-if="dcForm.loi.diaChiCuThe" class="ad-error">{{ dcForm.loi.diaChiCuThe }}</p>
                  <p v-if="diaChiTrenCccd" class="ad-hint">
                    Địa chỉ trên căn cước: <strong>{{ diaChiTrenCccd }}</strong>
                    <button type="button" class="btn btn-link btn-sm py-0" @click="dungDiaChiTrenCccd">Dùng làm địa chỉ cụ thể</button>
                  </p>
                </div>
              </div>
              <div class="d-flex justify-content-end gap-2 mt-3">
                <button type="button" class="ad-btn" @click="huyFormDiaChi">Hủy</button>
                <button type="button" class="ad-btn ad-btn-primary" @click="luuFormDiaChi">Lưu địa chỉ</button>
              </div>
            </div>

            <p v-if="errors.diaChis" class="ad-error mt-2">{{ errors.diaChis }}</p>
          </section>

          <section class="ad-card mt-4">
            <header class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-briefcase"></i></span>
              <div>
                <h2 class="ad-card-title">Vị trí làm việc</h2>
                <p class="ad-card-sub">Chọn một hoặc nhiều vị trí đã thêm, hoặc thêm vị trí mới.</p>
              </div>
            </header>
            <div class="nv-chips mt-3" role="group" aria-label="Các vị trí làm việc">
              <button
                v-for="v in viTriList"
                :key="v.id"
                type="button"
                class="nv-chip"
                :class="{ active: daChonViTri(v.id) }"
                :aria-pressed="daChonViTri(v.id)"
                @click="chonViTri(v.id)"
              >
                <i class="bi" :class="daChonViTri(v.id) ? 'bi-check-circle-fill' : 'bi-circle'" aria-hidden="true"></i>
                {{ v.ten }}
              </button>
              <span v-if="!viTriList.length" class="ad-hint m-0">Chưa có vị trí nào, hãy thêm vị trí mới ở bên dưới.</span>
            </div>
            <p class="ad-hint mt-2 mb-0">Đã chọn {{ form.idViTri.length }} vị trí. Bấm vào vị trí để chọn hoặc bỏ chọn cho nhân viên này.</p>
            <div class="nv-add mt-3">
              <label class="ad-label" for="nv-vi-tri-moi">Thêm vị trí mới</label>
              <div class="d-flex gap-2">
                <input
                  id="nv-vi-tri-moi"
                  v-model="tenViTriMoi"
                  type="text"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loiViTri }"
                  maxlength="50"
                  placeholder="Ví dụ: Thu ngân"
                  autocomplete="off"
                  @keydown.enter.prevent="themViTri"
                />
                <button type="button" class="ad-btn ad-btn-primary flex-shrink-0" :disabled="dangThemViTri" @click="themViTri">
                  <span v-if="dangThemViTri" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                  <i v-else class="bi bi-plus-lg" aria-hidden="true"></i> Thêm
                </button>
              </div>
              <p v-if="loiViTri" class="ad-error">{{ loiViTri }}</p>
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

    <!-- Quét mã QR trên căn cước công dân -->
    <CccdScannerModal v-if="hienQuet" @scanned="apDungCccd" @close="hienQuet = false" />

    <ConfirmDialog v-if="hoiKhoa" v-bind="noiDungXacNhan" :loading="dangDoiTrangThai" @confirm="doiTrangThai" @cancel="hoiKhoa = false" />
  </div>
</template>

<style scoped>
/* Chọn nhiều vị trí làm việc */
.nv-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.nv-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.9rem;
  border: 1px solid var(--fs-line);
  border-radius: 999px;
  background: #fff;
  color: var(--fs-text-2);
  font-weight: 500;
  transition: border-color 0.15s ease, background-color 0.15s ease, color 0.15s ease;
}
.nv-chip:hover {
  border-color: var(--fs-primary);
  color: var(--fs-primary-dark);
}
.nv-chip.active {
  border-color: var(--fs-primary);
  background: var(--fs-hover-bg);
  color: var(--fs-primary);
  font-weight: 700;
}
.nv-chip:focus-visible {
  outline: 2px solid var(--fs-primary);
  outline-offset: 2px;
}

/* Danh sách địa chỉ: mỗi địa chỉ là một thẻ chọn được (chọn = địa chỉ chính) */
.nv-dc-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.nv-dc {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0;
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}
.nv-dc:hover {
  background: var(--fs-hover-bg);
}
.nv-dc.active {
  border-color: var(--fs-primary);
  background: var(--fs-primary-soft);
}
.nv-dc:has(.nv-dc-radio:focus-visible) {
  outline: 2px solid var(--fs-primary);
  outline-offset: 2px;
}
.nv-dc-radio {
  flex: none;
  width: 1.05rem;
  height: 1.05rem;
  accent-color: var(--fs-primary);
}
.nv-dc-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.6rem;
}
.nv-dc-text {
  font-size: 0.9rem;
  color: var(--fs-text);
  overflow-wrap: anywhere;
}
.nv-dc-tag {
  flex: none;
}
.nv-dc-edit {
  flex: none;
}
.nv-dc-form {
  margin-top: 0.9rem;
  padding: 1rem;
  border: 1px dashed var(--fs-primary);
  border-radius: 14px;
  background: #fbfdff;
}
.nv-dc-form-title {
  margin: 0 0 0.75rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--fs-text);
}
</style>
