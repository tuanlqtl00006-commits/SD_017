<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BaseModal from '../common/BaseModal.vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import DiaChiHanhChinhSelect from '../common/DiaChiHanhChinhSelect.vue'
import CccdScannerModal from './CccdScannerModal.vue'
import { GIOI_TINH, TRANG_THAI, taoMaNhanVienMoi, tinhTuoi } from '../../constants/nhanVien'
import { diaChiMoiTuCccd, ghepDiaChi } from '../../utils/diaChi'
import { diaChiHanhChinhService } from '../../services/diaChiHanhChinhService'
import { nhanVienService } from '../../services/nhanVienService'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { avatarColors, formatDate, getInitials, todayIso } from '../../utils/format'
import {
  HUONG_DAN_TUOI_NHAN_VIEN,
  TUOI_NHAN_VIEN_TOI_THIEU,
  cat,
  chuanHoaHoTen,
  congNam,
  khoaEmail,
  khoaSdt,
  loiCccd,
  loiEmail,
  loiHoTen,
  loiMatKhau,
  loiNgaySinh,
  loiSdt,
  ngaySinhMuonNhat,
  ngaySinhSomNhat,
  sinhMatKhauTam,
  timBanTrung,
  vaoLamKhiDuTuoi,
} from '../../utils/kiemTra'
import DateInput from '../common/DateInput.vue'
import Select2 from '../common/Select2.vue'
import { tuDanhSach } from '../../utils/luaChon'

/*
 * Trang THÊM nhân viên (/nhan-vien/them) và CHI TIẾT / SỬA nhân viên (/nhan-vien/:id), giống video:
 * nút quay lại, thẻ hồ sơ bên trái, các ô nhập bên phải, nút lưu ở cuối.
 *
 * MÃ NHÂN VIÊN: chữ cái đầu của họ tên + số thứ tự 4 chữ số (Lê Quốc Tuấn -> LQT0001). Trang này hiện mã dự kiến ngay khi nhập tên;
 *   mã chính thức do backend cấp khi bấm "Tạo nhân viên".
 * QUÉT CĂN CƯỚC (chỉ khi thêm mới): quét mã QR trên thẻ CCCD để điền sẵn họ tên, ngày sinh, giới tính, số CCCD và TỰ THÊM địa chỉ trên thẻ
 *   (địa chỉ cũ có Quận/Huyện) vào danh sách địa chỉ mới (Tỉnh/Thành phố -> Phường/Xã sau sáp nhập), đối chiếu với danh sách hành chính mới.
 * ĐỊA CHỈ: nhiều địa chỉ (Tỉnh/Thành phố -> Phường/Xã sau sáp nhập), chọn 1 địa chỉ chính. Chỉ thêm / sửa / chọn lại, KHÔNG có nút xóa.
 *
 * QUY ĐỊNH KHI SỬA NHÂN VIÊN
 *  - Được sửa : họ tên, giới tính, ngày sinh, số điện thoại, số CCCD, các địa chỉ, vai trò.
 *  - Không sửa: mã nhân viên (hệ thống cấp), email (là tài khoản đăng nhập),
 *               ngày vào làm (mốc tuyển dụng), mật khẩu (nhập lúc thêm mới, không hiện ở trang sửa).
 *  - Trạng thái: "Còn làm" / "Đã nghỉ". Nút "Cho nghỉ việc" / "Cho làm lại" ở thẻ "Trạng thái nhân viên" (đổi cột trạng thái, không xóa dữ liệu).
 * Khi thêm mới thì nhập đầy đủ tất cả các ô.
 *
 * KIỂM TRA DỮ LIỆU (theo góp ý của cô: "form sửa cũng validate như form thêm", "lỗi dưới từng ô", "tuổi phải có căn cứ"):
 *  - Lỗi hiện NGAY DƯỚI TỪNG Ô khi rời ô / gõ, và hiện hết sau khi bấm lưu; ô lỗi đầu tiên được đưa con trỏ tới.
 *  - Form THÊM và form SỬA dùng chung một bộ luật (useLoiForm + utils/kiemTra.js), khớp backend NhanVienService.kiemTra từng câu.
 *  - Ngày sinh BẮT BUỘC và phải từ đủ 18 tuổi (Bộ luật Lao động 2019, Điều 143) - ghi rõ ngay dưới ô.
 *  - Họ tên chỉ gồm chữ cái; số điện thoại đúng đầu số; mật khẩu có cả chữ và số; email / số điện thoại / CCCD không trùng.
 *  - Lỗi do máy chủ trả về (loiTheoTruong) được gắn vào đúng ô và tự mất khi người dùng sửa ô đó.
 */
const route = useRoute()
const router = useRouter()
const toast = useToast()

const idXem = route.params.id ? Number(route.params.id) : null // null: thêm mới; có số: xem / sửa nhân viên này
const isEdit = idXem !== null

const tatCa = ref([]) // toàn bộ nhân viên, dùng để kiểm tra trùng và sinh mã
const vaiTro = ref([]) // [{ id, ten }] lấy từ bảng vai_tro
const dsChonVaiTro = computed(() => tuDanhSach(vaiTro.value))
const item = ref(null) // nhân viên đang xem / sửa (null khi thêm mới)

const dangTai = ref(true)
const loiTai = ref('') // có chữ: tải lỗi hoặc không thấy nhân viên
const dangLuu = ref(false)

const form = reactive({
  hoTen: '',
  email: '',
  soDienThoai: '',
  canCuoc: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  ngayVaoLam: todayIso(),
  idVaiTro: null,
  matKhau: !isEdit ? sinhMatKhauTam() : '',
  diaChis: [], // [{ key, id (null = mới thêm), tinhThanh, phuongXa, diaChiCuThe }]
  chinhKey: null, // key của địa chỉ chính
})

// Mã nhân viên: sửa -> mã đã cấp; thêm mới -> tự sinh mã NVxx hợp lý ngay khi mở form
const maNhanVien = computed(() => {
  if (item.value) return item.value.ma
  return taoMaNhanVienMoi(tatCa.value.map((e) => e.ma))
})

const tenVaiTroChon = computed(() => {
  const v = vaiTro.value.find((x) => x.id === form.idVaiTro)
  return v ? v.ten : 'Chưa chọn'
})

const chuoiDiaChiChinh = computed(() => {
  if (!diaChiChinh.value) return ''
  return hienDiaChi(diaChiChinh.value)
})

const tuoiHienThi = computed(() => {
  if (!form.ngaySinh || form.ngaySinh.length < 10) return null
  try {
    const age = tinhTuoi(form.ngaySinh)
    return age >= 0 ? age : null
  } catch {
    return null
  }
})

/* ----- Nhiều địa chỉ ----- */
let keySeq = 0
const taoKey = () => `n${++keySeq}`
const hienDiaChi = (d) => ghepDiaChi(d)
const diaChiChinh = computed(() => form.diaChis.find((d) => d.key === form.chinhKey) ?? form.diaChis[0] ?? null)

const dcForm = reactive({ mo: false, key: null, tinhThanh: '', phuongXa: '', diaChiCuThe: '', daBam: false })
const diaChiTrenCccd = ref('') // địa chỉ ghi trên thẻ CCCD vừa quét (để tham khảo khi nhập địa chỉ cụ thể)

function moFormDiaChi(d = null) {
  dcForm.mo = true
  dcForm.key = d ? d.key : null
  dcForm.tinhThanh = d?.tinhThanh ?? ''
  dcForm.phuongXa = d?.phuongXa ?? ''
  dcForm.diaChiCuThe = d?.diaChiCuThe ?? ''
  dcForm.daBam = false
  nextTick(() => document.getElementById('nv-dc-chi-tiet')?.focus())
}
function huyFormDiaChi() {
  dcForm.mo = false
  dcForm.key = null
  dcForm.daBam = false
}
// Lỗi của ô địa chỉ đang nhập: hiện dưới từng ô sau lần bấm "Lưu địa chỉ" đầu tiên, rồi cập nhật theo từng lần gõ / chọn
function kiemTraDiaChi() {
  const e = {}
  const chiTiet = chuanHoaHoTen(dcForm.diaChiCuThe)
  if (!dcForm.tinhThanh) e.tinhThanh = 'Chọn Tỉnh/Thành phố.'
  if (!dcForm.phuongXa) e.phuongXa = 'Chọn Phường/Xã.'
  if (!chiTiet) e.diaChiCuThe = 'Nhập số nhà, tên đường...'
  else if (chiTiet.length > 255) e.diaChiCuThe = 'Địa chỉ cụ thể tối đa 255 ký tự.'
  return e
}
const loiDc = computed(() => (dcForm.daBam ? kiemTraDiaChi() : {}))
function luuFormDiaChi() {
  dcForm.daBam = true
  if (Object.keys(kiemTraDiaChi()).length) return
  const chiTiet = chuanHoaHoTen(dcForm.diaChiCuThe)
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
  form.canCuoc = nv.canCuoc ?? ''
  form.gioiTinh = nv.gioiTinh ?? 'Nam'
  form.ngaySinh = nv.ngaySinh ?? ''
  form.ngayVaoLam = nv.ngayVaoLam ?? ''
  form.idVaiTro = nv.idVaiTro
  // Nhân viên cũ chưa có danh sách địa chỉ thì dùng chuỗi địa chỉ cũ làm một địa chỉ chính
  const ds = nv.diaChis?.length ? nv.diaChis : nv.diaChi ? [{ id: null, tinhThanh: '', phuongXa: '', diaChiCuThe: nv.diaChi, macDinh: true }] : []
  form.diaChis = ds.map((d) => ({ key: d.id ? `d${d.id}` : taoKey(), id: d.id ?? null, tinhThanh: d.tinhThanh ?? '', phuongXa: d.phuongXa ?? '', diaChiCuThe: d.diaChiCuThe ?? '' }))
  form.chinhKey = (ds.findIndex((d) => d.macDinh) >= 0 ? form.diaChis[ds.findIndex((d) => d.macDinh)] : form.diaChis[0])?.key ?? null
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
      if (!form.matKhau) form.matKhau = sinhMatKhauTam()
    }
  } catch (err) {
    loiTai.value = err.message || 'Không tải được dữ liệu. Vui lòng thử lại.'
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
// Địa chỉ trên thẻ (cũ, có Quận/Huyện) được TỰ THÊM vào danh sách địa chỉ mới (Tỉnh/Thành phố -> Phường/Xã):
//  - đủ số nhà / đường -> thêm thẳng vào danh sách (là địa chỉ chính nếu chưa có địa chỉ nào), bấm nút sửa để chọn lại nếu chưa khớp đơn vị mới;
//  - chỉ có Tỉnh/Phường, thiếu số nhà / đường -> mở sẵn ô nhập với Tỉnh/Phường đã điền, người dùng nhập tiếp địa chỉ cụ thể.
async function apDungCccd(info) {
  form.canCuoc = info.soCccd
  if (info.hoTen) {
    form.hoTen = info.hoTen
    cham('hoTen')
  }
  if (info.gioiTinh) form.gioiTinh = info.gioiTinh
  if (info.ngaySinh) {
    form.ngaySinh = info.ngaySinh
    cham('ngaySinh')
  }
  diaChiTrenCccd.value = info.diaChi
  const trung = tatCa.value.find((e) => e.canCuoc === info.soCccd && e.id !== (item.value?.id ?? null))
  const tenNv = info.hoTen || 'nhân viên'
  let ghiChuDiaChi = ''
  if (info.diaChi) ghiChuDiaChi = await themDiaChiTuCccd(info.diaChi)
  if (trung) {
    toast.error(`Số CCCD này đã thuộc nhân viên ${trung.hoTen} (${trung.ma}).`)
    form.canCuoc = ''
  } else {
    toast.success(`Đã điền thông tin từ căn cước của ${tenNv}.${ghiChuDiaChi} Hãy kiểm tra lại rồi nhập email, mật khẩu và số điện thoại.`)
  }
  nextTick(() => document.getElementById('nv-email')?.scrollIntoView?.({ block: 'center', behavior: 'smooth' }))
}

// Thêm địa chỉ trên thẻ vào danh sách địa chỉ; trả về câu ghi chú cho thông báo.
async function themDiaChiTuCccd(diaChiThe) {
  const dc = await diaChiMoiTuCccd(diaChiThe, diaChiHanhChinhService)
  const hienThi = (d) => ghepDiaChi(d).toLowerCase().replace(/\s+/g, ' ')
  if (!dc.diaChiCuThe) {
    // thẻ không ghi số nhà / đường: mở ô nhập với Tỉnh, Phường đã điền sẵn
    if (!dcForm.mo) {
      moFormDiaChi()
      dcForm.tinhThanh = dc.tinhThanh
      dcForm.phuongXa = dc.phuongXa
    }
    return ' Địa chỉ trên thẻ chưa có số nhà / đường: hãy nhập địa chỉ cụ thể ở ô địa chỉ.'
  }
  const moi = { key: taoKey(), id: null, tinhThanh: dc.tinhThanh, phuongXa: dc.phuongXa, diaChiCuThe: dc.diaChiCuThe }
  if (form.diaChis.some((d) => hienThi(d) === hienThi(moi))) return ' Địa chỉ trên thẻ đã có trong danh sách địa chỉ.'
  form.diaChis.push(moi)
  if (!form.chinhKey) form.chinhKey = moi.key // địa chỉ đầu tiên tự là địa chỉ chính
  if (dc.khopTinh && dc.khopPhuong) return ' Đã thêm địa chỉ trên thẻ vào danh sách địa chỉ (theo đơn vị hành chính mới).'
  if (dc.khopTinh) return ` Đã thêm địa chỉ trên thẻ vào danh sách địa chỉ${dc.daDoiTinh ? ` (tỉnh đã sáp nhập: ${dc.tinhThanh})` : ''}; Phường/Xã cũ chưa khớp đơn vị mới, bấm nút sửa địa chỉ để chọn lại.`
  return ' Đã thêm địa chỉ trên thẻ vào danh sách địa chỉ; Tỉnh/Phường chưa khớp đơn vị hành chính mới, bấm nút sửa địa chỉ để chọn lại.'
}

/* ----- Kiểm tra dữ liệu: mỗi ô một câu báo lỗi, khớp backend NhanVienService.kiemTra ----- */

const hienMatKhau = ref(false)
function sinhLaiMatKhau() {
  form.matKhau = sinhMatKhauTam()
  sua('matKhau')
  toast.info('Đã tạo mật khẩu tạm ngẫu nhiên mới.')
}
const LOI_EMAIL_NHAN_VIEN = 'Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.'

// Khóa các ngày không chọn được trên lịch: nhân viên phải từ đủ 18 tuổi và không quá 100 tuổi (chặn gõ nhầm năm).
// Gõ tay ngày ngoài khoảng này vẫn bị báo lỗi ngay dưới ô.
const ngaySinhToiDa = ngaySinhMuonNhat(TUOI_NHAN_VIEN_TOI_THIEU)
const ngaySinhToiThieu = ngaySinhSomNhat()
const ngayHomNay = todayIso()
// Ngày vào làm chọn được từ sinh nhật thứ 18 trở đi (chỉ khi ngày sinh đã hợp lệ)
const ngayVaoLamToiThieu = computed(() => (cat(form.ngaySinh) && !loiNgaySinh(form.ngaySinh, TUOI_NHAN_VIEN_TOI_THIEU, 'Nhân viên') ? congNam(form.ngaySinh, TUOI_NHAN_VIEN_TOI_THIEU) : undefined))

function tinhLoi() {
  const e = {}
  const homNay = todayIso()
  const idDangSua = item.value?.id ?? null

  const ten = loiHoTen(form.hoTen, { thieu: 'Nhập họ tên.', toiDa: 60 })
  if (ten) e.hoTen = ten

  // Email chỉ nhập lúc thêm mới; khi sửa email bị khóa nên không cần kiểm tra
  if (!isEdit) {
    let email = loiEmail(form.email, { thieu: 'Nhập email.', dinhDang: LOI_EMAIL_NHAN_VIEN })
    if (!email && timBanTrung(tatCa.value, form.email, (x) => x.email, khoaEmail, idDangSua)) email = 'Email đã được sử dụng.'
    if (email) e.email = email
  }

  if (form.canCuoc && timBanTrung(tatCa.value, form.canCuoc, (x) => x.canCuoc, cat, idDangSua)) {
    e.canCuoc = 'Số căn cước công dân đã được sử dụng.'
    toast.error('Số căn cước công dân đã được sử dụng.')
  }

  let sdt = loiSdt(form.soDienThoai, 'Nhập số điện thoại.')
  if (!sdt && timBanTrung(tatCa.value, form.soDienThoai, (x) => x.soDienThoai, khoaSdt, idDangSua)) sdt = 'Số điện thoại đã được sử dụng.'
  if (sdt) e.soDienThoai = sdt

  if (!cat(form.gioiTinh)) e.gioiTinh = 'Chọn giới tính.'
  else if (!GIOI_TINH.includes(form.gioiTinh)) e.gioiTinh = 'Giới tính không hợp lệ.'

  // Ngày sinh BẮT BUỘC (là căn cứ biết nhân viên đã đủ 18 tuổi) - Bộ luật Lao động 2019, Điều 143
  if (!cat(form.ngaySinh)) e.ngaySinh = 'Nhập ngày sinh.'
  else {
    const ns = loiNgaySinh(form.ngaySinh, TUOI_NHAN_VIEN_TOI_THIEU, 'Nhân viên', homNay)
    if (ns) e.ngaySinh = ns
  }

  // Ngày vào làm: không nhập thì lấy hôm nay; không được ở tương lai (khi sửa thì ô này bị khóa, giữ ngày cũ)
  const vaoLam = cat(form.ngayVaoLam).slice(0, 10) || (isEdit ? '' : homNay)
  if (!isEdit && vaoLam > homNay) e.ngayVaoLam = 'Ngày vào làm không được ở tương lai.'

  // Ngày sinh và ngày vào làm phải khớp nhau (vào làm khi đã đủ 18 tuổi).
  // Khi sửa, ngày vào làm bị khóa nên lỗi được báo ở ô ngày sinh (ô duy nhất người dùng sửa được).
  if (vaoLam && cat(form.ngaySinh) && !e.ngaySinh && !e.ngayVaoLam && !vaoLamKhiDuTuoi(form.ngaySinh, vaoLam)) {
    if (isEdit) e.ngaySinh = `Ngày sinh không hợp lệ: nhân viên phải đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi vào ngày vào làm (${formatDate(vaoLam)}).`
    else e.ngayVaoLam = `Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ ${TUOI_NHAN_VIEN_TOI_THIEU} tuổi).`
  }

  if (!form.idVaiTro) e.idVaiTro = 'Chọn vai trò.'

  if (dcForm.mo) e.diaChis = 'Bấm “Lưu địa chỉ” hoặc “Hủy” để hoàn tất địa chỉ đang nhập.'
  else if (!form.diaChis.length) e.diaChis = 'Thêm ít nhất một địa chỉ.'

  // Mật khẩu chỉ nhập lúc thêm mới (khi sửa không có ô mật khẩu).
  if (!isEdit) {
    const mk = loiMatKhau(form.matKhau)
    if (mk) e.matKhau = mk
  }

  return e
}

const { loi, cham, sua, batDauGui, datLoiMayChu, focusLoiDau } = useLoiForm(tinhLoi)

/* ----- Lưu ----- */

async function focusLoiDauTien() {
  await focusLoiDau('#nv-form')
  // Lỗi "chưa có địa chỉ" không nằm ở một ô nhập nào: cuộn tới thẻ Địa chỉ
  if (!document.querySelector('#nv-form .is-invalid, #nv-form [aria-invalid="true"]')) {
    document.getElementById('nv-dia-chi-card')?.scrollIntoView?.({ block: 'center', behavior: 'smooth' })
  }
}

async function guiForm() {
  if (dangLuu.value) return
  if (!batDauGui()) {
    focusLoiDauTien()
    return
  }
  const payload = {
    hoTen: chuanHoaHoTen(form.hoTen),
    soDienThoai: cat(form.soDienThoai),
    gioiTinh: form.gioiTinh,
    ngaySinh: form.ngaySinh || null,
    canCuoc: cat(form.canCuoc) || null,
    idVaiTro: form.idVaiTro,
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
    payload.email = khoaEmail(form.email)
    payload.ngayVaoLam = form.ngayVaoLam || null
    payload.matKhau = form.matKhau
    if (maNhanVien.value) payload.maNhanVien = maNhanVien.value
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
  } catch (err) {
    // Backend trả lỗi theo từng ô (vd 409 trùng email) -> gắn xuống đúng ô; không có thì báo bằng thông báo chung
    if (datLoiMayChu(err)) {
      toast.error(err.message || 'Vui lòng kiểm tra lại các ô báo đỏ.')
      focusLoiDauTien()
    } else {
      toast.error(err.message || 'Không thể lưu nhân viên. Vui lòng thử lại.')
    }
  } finally {
    dangLuu.value = false
  }
}

/* ----- Cho nghỉ việc / cho làm lại (chỉ ở trang chi tiết) ----- */

const hoiKhoa = ref(false) // đang hiện hộp xác nhận
const dangDoiTrangThai = ref(false)

const noiDungXacNhan = computed(() => {
  const e = item.value
  if (!e) return {}
  return e.hoatDong
    ? {
        title: 'Cho nhân viên nghỉ việc?',
        message: `${e.hoTen} (${e.ma}) sẽ chuyển sang trạng thái "Đã nghỉ" và không thể đăng nhập vào hệ thống. Hồ sơ và lịch sử vẫn được giữ lại, bạn có thể cho làm lại bất cứ lúc nào.`,
        confirmText: 'Cho nghỉ việc',
        variant: 'danger',
      }
    : {
        title: 'Cho nhân viên làm lại?',
        message: `${e.hoTen} (${e.ma}) sẽ chuyển sang trạng thái "Còn làm" và có thể đăng nhập, làm việc trở lại.`,
        confirmText: 'Cho làm lại',
        variant: 'primary',
      }
})

// Backend chặn việc cho nghỉ / đổi vai trò quản lý còn làm cuối cùng và báo lỗi bằng toast.
async function doiTrangThai() {
  const e = item.value
  dangDoiTrangThai.value = true
  try {
    await nhanVienService.toggleActive(e.id)
    toast.success(e.hoatDong ? `Đã cho nhân viên ${e.hoTen} nghỉ việc.` : `Đã cho nhân viên ${e.hoTen} làm lại.`)
    hoiKhoa.value = false
    const ds = await nhanVienService.getAll()
    tatCa.value = ds
    item.value = ds.find((x) => x.id === idXem) ?? item.value
  } catch (err) {
    toast.error(err.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    dangDoiTrangThai.value = false
  }
}

/* ----- Đổi mật khẩu nhân viên (dành cho xem / sửa nhân viên) ----- */
const hienDoiMk = ref(false)
const matKhauMoi = ref('')
const hienMatKhauMoi = ref(false)
const loiMatKhauMoi = ref('')
const dangDoiMk = ref(false)

function moDoiMatKhau() {
  matKhauMoi.value = sinhMatKhauTam()
  loiMatKhauMoi.value = ''
  hienDoiMk.value = true
}

async function xacNhanDoiMatKhau() {
  const err = loiMatKhau(matKhauMoi.value, 'Vui lòng nhập mật khẩu mới.')
  if (err) {
    loiMatKhauMoi.value = err
    return
  }
  dangDoiMk.value = true
  try {
    await nhanVienService.doiMatKhau(idXem, matKhauMoi.value)
    toast.success('Đã đổi mật khẩu nhân viên thành công.')
    hienDoiMk.value = false
    matKhauMoi.value = ''
  } catch (e) {
    toast.error(e.message || 'Không thể đổi mật khẩu. Vui lòng thử lại.')
  } finally {
    dangDoiMk.value = false
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
        <!-- Cột trái: thẻ hồ sơ + trạng thái nhân viên -->
        <div class="col-lg-4">
          <section class="ad-card text-center" data-testid="nv-the-ho-so">
            <span class="ad-avatar ad-avatar-lg mx-auto" :style="avatarColors(form.hoTen || '?')">{{ getInitials(form.hoTen) }}</span>
            <h2 class="ad-card-title mt-3 mb-1">{{ form.hoTen || 'Nhân viên mới' }}</h2>
            <div class="d-flex align-items-center justify-content-center gap-2 mt-2 flex-wrap">
              <span v-if="maNhanVien" class="badge bg-secondary-subtle text-secondary border border-secondary-subtle font-monospace px-2 py-1">
                {{ maNhanVien }}
              </span>
              <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">
                {{ tenVaiTroChon }}
              </span>
              <span class="ad-pill" :class="isEdit && item ? TRANG_THAI[item.hoatDong].cls : TRANG_THAI[true].cls">
                {{ isEdit && item ? TRANG_THAI[item.hoatDong].label : TRANG_THAI[true].label }}
              </span>
            </div>

            <hr class="my-3 text-secondary-subtle" />

            <div class="nv-preview-list text-start">
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-hash"></i> Mã NV:</span>
                <span class="nv-preview-val font-monospace" data-testid="nv-preview-ma">{{ maNhanVien || '—' }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-person"></i> Họ và tên:</span>
                <span class="nv-preview-val" data-testid="nv-preview-ten">{{ form.hoTen || '—' }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-shield-lock"></i> Vai trò:</span>
                <span class="nv-preview-val" data-testid="nv-preview-vai-tro">{{ tenVaiTroChon }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-envelope"></i> Email:</span>
                <span class="nv-preview-val text-truncate ms-2" style="max-width: 175px;" :title="form.email" data-testid="nv-preview-email">{{ form.email || '—' }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-telephone"></i> SĐT:</span>
                <span class="nv-preview-val" data-testid="nv-preview-sdt">{{ form.soDienThoai || '—' }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-gender-ambiguous"></i> Giới tính:</span>
                <span class="nv-preview-val" data-testid="nv-preview-gioi-tinh">{{ form.gioiTinh || '—' }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-cake2"></i> Ngày sinh:</span>
                <span class="nv-preview-val" data-testid="nv-preview-ngay-sinh">
                  {{ form.ngaySinh ? formatDate(form.ngaySinh) : '—' }}
                  <span v-if="tuoiHienThi" class="text-muted">({{ tuoiHienThi }} tuổi)</span>
                </span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-calendar-check"></i> Vào làm:</span>
                <span class="nv-preview-val" data-testid="nv-preview-vao-lam">{{ form.ngayVaoLam ? formatDate(form.ngayVaoLam) : '—' }}</span>
              </div>
              <div class="nv-preview-item">
                <span class="nv-preview-label"><i class="bi bi-credit-card-2-front"></i> CCCD:</span>
                <span class="nv-preview-val font-monospace" data-testid="nv-preview-cccd">{{ form.canCuoc || '—' }}</span>
              </div>
              <div class="nv-preview-item flex-column align-items-stretch">
                <span class="nv-preview-label mb-1"><i class="bi bi-geo-alt"></i> Địa chỉ chính:</span>
                <div class="nv-preview-val text-start ps-3" style="font-size: 0.84rem;" data-testid="nv-preview-dia-chi">
                  {{ chuoiDiaChiChinh || 'Chưa thêm địa chỉ' }}
                </div>
              </div>
            </div>
          </section>

          <section v-if="isEdit && item" class="ad-card mt-4">
            <h2 class="ad-card-title mb-2">Trạng thái nhân viên</h2>
            <button type="button" class="ad-btn w-100" :class="item.hoatDong ? 'text-danger' : 'text-success'" @click="hoiKhoa = true">
              <i class="bi" :class="item.hoatDong ? 'bi-box-arrow-right' : 'bi-arrow-counterclockwise'" aria-hidden="true"></i>
              {{ item.hoatDong ? 'Cho nghỉ việc' : 'Cho làm lại' }}
            </button>
          </section>

          <section v-if="isEdit && item" class="ad-card mt-4">
            <h2 class="ad-card-title mb-2">Bảo mật tài khoản</h2>
            <button type="button" class="ad-btn w-100" @click="moDoiMatKhau">
              <i class="bi bi-key" aria-hidden="true"></i> Đổi mật khẩu
            </button>
          </section>
        </div>

        <!-- Cột phải: thông tin cơ bản + địa chỉ -->
        <div class="col-lg-8">
          <section class="ad-card">
            <header class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-person"></i></span>
              <h2 class="ad-card-title">Thông tin cơ bản</h2>
              <div class="d-flex align-items-center gap-2 ms-auto">
                <span v-if="form.canCuoc" class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1" data-testid="cccd-da-quet">
                  <i class="bi bi-qr-code-scan me-1" aria-hidden="true"></i>CCCD: {{ form.canCuoc }}
                </span>
                <button v-if="!isEdit" type="button" class="ad-btn" @click="hienQuet = true">
                  <i class="bi bi-qr-code-scan" aria-hidden="true"></i> Quét căn cước
                </button>
              </div>
            </header>

            <div class="row g-3 mt-1">
              <div class="col-md-6">
                <label class="ad-label" for="nv-ma">Mã nhân viên</label>
                <input
                  id="nv-ma"
                  type="text"
                  class="form-control ad-control font-monospace"
                  :value="maNhanVien"
                  readonly
                />
                <p v-if="isEdit" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-ho-ten">Họ và tên <span class="ad-required">*</span></label>
                <input
                  id="nv-ho-ten"
                  v-model="form.hoTen"
                  type="text"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.hoTen }"
                  maxlength="60"
                  placeholder="Ví dụ: Nguyễn Văn An"
                  autocomplete="off"
                  :aria-invalid="!!loi.hoTen"
                  aria-describedby="nv-ho-ten-msg"
                  @input="sua('hoTen')"
                  @blur="cham('hoTen')"
                />
                <p v-if="loi.hoTen" id="nv-ho-ten-msg" class="ad-error">{{ loi.hoTen }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-email">Email (dùng để đăng nhập) <span v-if="!isEdit" class="ad-required">*</span></label>
                <input
                  id="nv-email"
                  v-model="form.email"
                  type="email"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.email }"
                  :readonly="isEdit"
                  placeholder="ten@footstyle.vn"
                  autocomplete="off"
                  :aria-invalid="!!loi.email"
                  aria-describedby="nv-email-msg"
                  @input="sua('email')"
                  @blur="cham('email')"
                />
                <p v-if="isEdit" class="ad-hint">Email là tài khoản đăng nhập nên không thể đổi.</p>
                <p v-if="loi.email" id="nv-email-msg" class="ad-error">{{ loi.email }}</p>
              </div>

              <!-- Mật khẩu chỉ nhập khi thêm mới; trang sửa không có ô này -->
              <div v-if="!isEdit" class="col-md-6">
                <div class="d-flex align-items-center justify-content-between mb-1">
                  <label class="ad-label mb-0" for="nv-mat-khau">Mật khẩu tạm (tự sinh) <span class="ad-required">*</span></label>
                  <button
                    type="button"
                    class="btn btn-link btn-sm p-0 text-decoration-none"
                    title="Sinh mật khẩu ngẫu nhiên mới"
                    @click="sinhLaiMatKhau"
                  >
                    <i class="bi bi-arrow-clockwise me-1" aria-hidden="true"></i>Sinh mật khẩu khác
                  </button>
                </div>
                <div class="ad-affix">
                  <input
                    id="nv-mat-khau"
                    v-model="form.matKhau"
                    :type="hienMatKhau ? 'text' : 'password'"
                    class="form-control ad-control has-btn font-monospace"
                    :class="{ 'is-invalid': loi.matKhau }"
                    maxlength="50"
                    placeholder="Mật khẩu tự sinh"
                    autocomplete="new-password"
                    :aria-invalid="!!loi.matKhau"
                    aria-describedby="nv-mat-khau-msg"
                    @input="sua('matKhau')"
                    @blur="cham('matKhau')"
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
                <p v-if="loi.matKhau" id="nv-mat-khau-msg" class="ad-error">{{ loi.matKhau }}</p>
                <p v-else id="nv-mat-khau-msg" class="ad-hint">
                  <i class="bi bi-info-circle me-1" aria-hidden="true"></i>Mật khẩu tạm tự sinh được gửi qua email cho nhân viên. Nhân viên dùng mật khẩu này để đăng nhập và tự đổi mật khẩu cá nhân.
                </p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-sdt">Số điện thoại <span class="ad-required">*</span></label>
                <input
                  id="nv-sdt"
                  v-model="form.soDienThoai"
                  type="tel"
                  inputmode="numeric"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.soDienThoai }"
                  maxlength="10"
                  placeholder="0912345678"
                  autocomplete="off"
                  :aria-invalid="!!loi.soDienThoai"
                  aria-describedby="nv-sdt-msg"
                  @input="sua('soDienThoai')"
                  @blur="cham('soDienThoai')"
                />
                <p v-if="loi.soDienThoai" id="nv-sdt-msg" class="ad-error">{{ loi.soDienThoai }}</p>
                <p v-else id="nv-sdt-msg" class="ad-hint">10 chữ số, đầu số 03, 05, 07, 08 hoặc 09.</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-vai-tro-form">Vai trò <span class="ad-required">*</span></label>
                <Select2
                  id="nv-vai-tro-form"
                  v-model="form.idVaiTro"
                  :options="dsChonVaiTro"
                  placeholder="Chọn vai trò…"
                  class="ad-control"
                  :invalid="!!loi.idVaiTro"
                  :aria-invalid="!!loi.idVaiTro"
                  aria-describedby="nv-vai-tro-msg"
                  @update:model-value="sua('idVaiTro')"
                />
                <p v-if="loi.idVaiTro" id="nv-vai-tro-msg" class="ad-error">{{ loi.idVaiTro }}</p>
              </div>

              <div class="col-md-6">
                <span id="nv-gioi-tinh-label" class="ad-label">Giới tính <span class="ad-required">*</span></span>
                <div class="ad-segment" role="radiogroup" aria-labelledby="nv-gioi-tinh-label">
                  <label v-for="g in GIOI_TINH" :key="g" class="ad-segment-item" :class="{ active: form.gioiTinh === g }">
                    <input v-model="form.gioiTinh" type="radio" class="visually-hidden" name="gioiTinh" :value="g" @change="sua('gioiTinh')" />
                    {{ g }}
                  </label>
                </div>
                <p v-if="loi.gioiTinh" class="ad-error">{{ loi.gioiTinh }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-ngay-sinh">Ngày sinh <span class="ad-required">*</span></label>
                <DateInput
                  id="nv-ngay-sinh"
                  v-model="form.ngaySinh"
                  :min="ngaySinhToiThieu"
                  :max="ngaySinhToiDa"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.ngaySinh }"
                  :aria-invalid="!!loi.ngaySinh"
                  aria-describedby="nv-ngay-sinh-msg"
                  @update:model-value="sua('ngaySinh')"
                  @blur="cham('ngaySinh')"
                />
                <p v-if="loi.ngaySinh" id="nv-ngay-sinh-msg" class="ad-error">{{ loi.ngaySinh }}</p>
                <p v-else id="nv-ngay-sinh-msg" class="ad-hint" data-testid="nv-huong-dan-tuoi">{{ HUONG_DAN_TUOI_NHAN_VIEN }}</p>
              </div>

              <div class="col-md-6">
                <label class="ad-label" for="nv-ngay-vao-lam">Ngày vào làm</label>
                <DateInput
                  id="nv-ngay-vao-lam"
                  v-model="form.ngayVaoLam"
                  :min="isEdit ? undefined : ngayVaoLamToiThieu"
                  :max="isEdit ? undefined : ngayHomNay"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': loi.ngayVaoLam }"
                  :readonly="isEdit"
                  :aria-invalid="!!loi.ngayVaoLam"
                  aria-describedby="nv-ngay-vao-lam-msg"
                  @update:model-value="sua('ngayVaoLam')"
                  @blur="cham('ngayVaoLam')"
                />
                <p v-if="isEdit" class="ad-hint">Ngày vào làm là mốc tuyển dụng nên không thể đổi.</p>
                <p v-if="loi.ngayVaoLam" id="nv-ngay-vao-lam-msg" class="ad-error">{{ loi.ngayVaoLam }}</p>
              </div>
            </div>
          </section>

          <section id="nv-dia-chi-card" class="ad-card mt-4">
            <header class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
              <h2 class="ad-card-title">Địa chỉ</h2>
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
                  :error-tinh="loiDc.tinhThanh"
                  :error-phuong="loiDc.phuongXa"
                />
                <div class="col-12">
                  <label class="ad-label" for="nv-dc-chi-tiet">Địa chỉ cụ thể <span class="ad-required">*</span></label>
                  <input
                    id="nv-dc-chi-tiet"
                    v-model="dcForm.diaChiCuThe"
                    type="text"
                    maxlength="255"
                    class="form-control ad-control"
                    :class="{ 'is-invalid': loiDc.diaChiCuThe }"
                    placeholder="Số nhà, tên đường, thôn / tổ..."
                    autocomplete="off"
                    :aria-invalid="!!loiDc.diaChiCuThe"
                    aria-describedby="nv-dc-chi-tiet-msg"
                    @keydown.enter.prevent="luuFormDiaChi"
                  />
                  <p v-if="loiDc.diaChiCuThe" id="nv-dc-chi-tiet-msg" class="ad-error">{{ loiDc.diaChiCuThe }}</p>
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

            <p v-if="loi.diaChis" id="nv-dia-chi-msg" class="ad-error mt-2">{{ loi.diaChis }}</p>
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
 
    <!-- Hộp thoại Đổi mật khẩu (chỉ dùng khi sửa / xem chi tiết) -->
    <BaseModal v-if="isEdit && hienDoiMk" title="Đổi mật khẩu nhân viên" @close="hienDoiMk = false">
      <div class="p-3">
        <p class="ad-hint mb-3">
          Nhập mật khẩu mới cho nhân viên <strong>{{ item?.hoTen }}</strong> (mã {{ item?.ma }}):
        </p>
        <div class="mb-3">
          <div class="d-flex align-items-center justify-content-between mb-1">
            <label class="ad-label mb-0" for="nv-mk-moi">Mật khẩu mới <span class="ad-required">*</span></label>
            <button
              type="button"
              class="btn btn-link btn-sm p-0 text-decoration-none"
              title="Sinh mật khẩu ngẫu nhiên"
              @click="matKhauMoi = sinhMatKhauTam(); loiMatKhauMoi = ''"
            >
              <i class="bi bi-arrow-clockwise me-1" aria-hidden="true"></i>Sinh ngẫu nhiên
            </button>
          </div>
          <div class="ad-affix">
            <input
              id="nv-mk-moi"
              v-model="matKhauMoi"
              :type="hienMatKhauMoi ? 'text' : 'password'"
              class="form-control ad-control has-btn font-monospace"
              :class="{ 'is-invalid': !!loiMatKhauMoi }"
              maxlength="50"
              placeholder="Từ 8 ký tự, gồm cả chữ và số"
              @input="loiMatKhauMoi = ''"
            />
            <button
              type="button"
              class="ad-affix-btn"
              :title="hienMatKhauMoi ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              @click="hienMatKhauMoi = !hienMatKhauMoi"
            >
              <i class="bi" :class="hienMatKhauMoi ? 'bi-eye-slash' : 'bi-eye'" aria-hidden="true"></i>
            </button>
          </div>
          <p v-if="loiMatKhauMoi" class="ad-error mt-1">{{ loiMatKhauMoi }}</p>
          <p v-else class="ad-hint mt-1">Từ 8 đến 50 ký tự, có cả chữ và số, không chứa khoảng trắng.</p>
        </div>
      </div>
      <template #footer>
        <button type="button" class="ad-btn" :disabled="dangDoiMk" @click="hienDoiMk = false">Hủy</button>
        <button type="button" class="ad-btn ad-btn-primary" :disabled="dangDoiMk" @click="xacNhanDoiMatKhau">
          <span v-if="dangDoiMk" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
          Lưu mật khẩu mới
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
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
.nv-preview-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  font-size: 0.875rem;
}
.nv-preview-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
  padding-bottom: 0.45rem;
  border-bottom: 1px dashed #e9ecef;
}
.nv-preview-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.nv-preview-label {
  color: #6c757d;
  font-size: 0.825rem;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.nv-preview-val {
  font-weight: 500;
  color: #212529;
  text-align: right;
  overflow-wrap: anywhere;
}
</style>
