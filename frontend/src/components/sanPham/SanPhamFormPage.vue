<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { taoMa } from '../../constants/thuocTinh'
import { layLuaChon } from '../../utils/text'
import { idKhongApDung, laDanhMucVot, laKhongApDung } from '../../utils/danhMuc'
import { sanPhamService } from '../../services/sanPhamService'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import ConfirmQuestion from '../common/ConfirmQuestion.vue'
import { thuocTinhService } from '../../services/thuocTinhService'
import { hinhAnhService } from '../../services/hinhAnhService'
import { useToast } from '../../composables/useToast'

// Trang THÊM / SỬA sản phẩm (đường dẫn /san-pham/them và /san-pham/:id/sua), giống video: có nút "Quay lại danh sách".
// Trang tự tải dữ liệu cần dùng (các bảng thuộc tính, danh sách sản phẩm để sinh mã, sản phẩm đang sửa).
const route = useRoute()
const router = useRouter()
const toast = useToast()

const idSua = route.params.id ? Number(route.params.id) : null // null: thêm mới; có số: đang sửa sản phẩm này
const isEdit = idSua !== null

// 6 ô chọn của sản phẩm. Dùng v-for để khỏi lặp 6 đoạn HTML giống nhau.
//   field: tên trường trong form (cũng là cột khóa ngoại trong bảng san_pham)
//   slug : tên bảng thuộc tính để lấy danh sách cho dropdown
const SELECTS = [
  { field: 'idDanhMuc', slug: 'danh-muc', label: 'Danh mục' },
  { field: 'idThuongHieu', slug: 'thuong-hieu', label: 'Thương hiệu' },
  { field: 'idXuatXu', slug: 'xuat-xu', label: 'Xuất xứ' },
  { field: 'idChatLieu', slug: 'chat-lieu', label: 'Chất liệu' },
  { field: 'idDoCung', slug: 'do-cung', label: 'Độ cứng', chiVot: true }, // chỉ danh mục vợt mới có
  { field: 'idDiemCanBang', slug: 'diem-can-bang', label: 'Điểm cân bằng', chiVot: true }, // chỉ danh mục vợt mới có
]
const THIEU_KHONG_AP_DUNG = 'CSDL chưa có mục "Không áp dụng" (độ cứng, điểm cân bằng, trọng lượng, chu vi). Hãy khởi động lại backend (hoặc chạy phần 6 "Nâng cấp CSDL cũ" của database/SD_17_tong.sql) rồi tải lại trang.'

const TOI_DA_ANH = 10 // giống backend (gồm cả ảnh chính)
const TOI_DA_MB = 5 // giống backend
const KIEU_ANH = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const thuocTinh = reactive({})
for (const s of SELECTS) thuocTinh[s.slug] = []
for (const slug of ['mau-sac', 'trong-luong', 'chu-vi']) thuocTinh[slug] = [] // dùng để tạo biến thể tự động
const tatCaSanPham = ref([]) // để sinh mã mới và kiểm tra trùng mã
const spDangSua = ref(null) // sản phẩm đang sửa (null khi thêm mới)

const dangTai = ref(true) // đang tải dữ liệu ban đầu
const loiTai = ref(false) // tải dữ liệu thất bại
const dangLuu = ref(false) // đang lưu thì khóa nút

const form = reactive({
  ma: '',
  ten: '',
  idDanhMuc: '',
  idThuongHieu: '',
  idXuatXu: '',
  idChatLieu: '',
  idDoCung: '',
  idDiemCanBang: '',
  anh: [], // danh sách đường dẫn ảnh, phần tử đầu tiên là ảnh chính (khi gửi sẽ tách thành anhChinh + anhPhu)
  moTa: '',
})

/* ----- Thuộc tính theo danh mục -----
   Vợt: có thêm Độ cứng, Điểm cân bằng; biến thể theo Màu sắc + Trọng lượng + Chu vi cán.
   Danh mục khác (Phụ kiện, Túi - balo, Quần áo thể thao, Quả cầu lông...): chỉ Thương hiệu, Xuất xứ, Chất liệu; biến thể chỉ theo Màu sắc.
   Phần không dùng được hệ thống tự gán "Không áp dụng" (CSDL vẫn bắt buộc có giá trị). */
const daChonDanhMuc = computed(() => form.idDanhMuc !== '' && form.idDanhMuc != null)
const tenDanhMuc = computed(() => thuocTinh['danh-muc'].find((x) => x.id === Number(form.idDanhMuc))?.ten ?? '')
const laVot = computed(() => daChonDanhMuc.value && laDanhMucVot(tenDanhMuc.value))
const selectsHienThi = computed(() => SELECTS.filter((s) => !s.chiVot || laVot.value))
// Id của mục "Không áp dụng" ở từng bảng (null nếu CSDL chưa có)
const idKad = (slug) => idKhongApDung(thuocTinh[slug])
// Giá trị gửi lên: vợt dùng giá trị đã chọn, danh mục khác dùng "Không áp dụng"
const idTheoDanhMuc = (s) => (s.chiVot && !laVot.value ? idKad(s.slug) : Number(form[s.field]))

// Đổi danh mục: xóa giá trị riêng của vợt khi chuyển sang danh mục khác (và ngược lại bắt chọn lại)
watch(
  () => form.idDanhMuc,
  () => {
    for (const s of SELECTS) {
      if (!s.chiVot) continue
      const kad = idKad(s.slug)
      if (laVot.value) {
        if (form[s.field] === kad) form[s.field] = ''
      } else if (daChonDanhMuc.value) {
        form[s.field] = kad ?? ''
      }
    }
  },
)

onMounted(async () => {
  try {
    const cacLoai = [...SELECTS.map((s) => s.slug), 'mau-sac', 'trong-luong', 'chu-vi']
    // Các lời gọi API độc lập nhau nên chạy song song cho nhanh
    const [ketQuaThuocTinh, dsSanPham, spCanSua] = await Promise.all([
      Promise.all(cacLoai.map((loai) => thuocTinhService.getAll(loai))),
      sanPhamService.getAll(),
      isEdit ? sanPhamService.getById(idSua) : Promise.resolve(null),
    ])
    cacLoai.forEach((loai, i) => {
      thuocTinh[loai] = ketQuaThuocTinh[i]
    })
    tatCaSanPham.value = dsSanPham

    if (spCanSua) {
      // Đang sửa: chép dữ liệu cũ vào form
      spDangSua.value = spCanSua
      form.ma = spCanSua.ma
      form.ten = spCanSua.ten
      for (const s of SELECTS) form[s.field] = spCanSua[s.field]
      form.anh = [spCanSua.anhChinh, ...(spCanSua.anhPhu || [])].filter(Boolean)
      form.moTa = spCanSua.moTa || ''
    } else {
      // Thêm mới: hệ thống tự tạo mã không trùng mã đã có
      form.ma = taoMa('SP', dsSanPham.map((p) => p.ma))
    }
  } catch (loi) {
    loiTai.value = true
    toast.error(loi.message || 'Không tải được dữ liệu. Vui lòng thử lại.')
  } finally {
    dangTai.value = false
  }
  await nextTick()
  const o = document.getElementById('sp-ten')
  if (o) o.focus()
})

/* ----- Tạo biến thể tự động: màu sắc x trọng lượng x chu vi (chỉ khi thêm mới) ----- */

const chonMau = ref([])
const chonTl = ref([])
const chonCv = ref([])
// Nhóm thuộc tính để tạo biến thể: vợt có đủ 3 nhóm, danh mục khác chỉ có Màu sắc
const nhomTaoBienThe = computed(() => [
  { slug: 'mau-sac', label: 'Màu sắc', sel: chonMau.value },
  ...(laVot.value
    ? [
        { slug: 'trong-luong', label: 'Trọng lượng', sel: chonTl.value },
        { slug: 'chu-vi', label: 'Chu vi cán', sel: chonCv.value },
      ]
    : []),
])
// Đổi sang danh mục có bộ thuộc tính khác (vợt <-> không phải vợt) thì biến thể đã tạo không còn đúng, làm lại từ đầu
watch(laVot, () => {
  chonTl.value = []
  chonCv.value = []
  bienThe.value = []
})
const bienThe = ref([]) // mỗi dòng: { key, idMauSac, idTrongLuong, idChuVi, soLuong, giaBan, chon }
const macDinh = reactive({ soLuong: '', giaBan: '' })
const hoiLuu = ref(false) // hiện hộp "Bạn có muốn lưu sản phẩm không?"
const hoiCapNhat = ref(null) // sản phẩm đã tồn tại: { message, ... } từ backend -> hỏi "bạn muốn cập nhật?"

const dangHoatDong = (slug) => thuocTinh[slug].filter((x) => x.hoatDong && !laKhongApDung(x)) // bỏ mục "Không áp dụng" (hệ thống tự gán)
const tenCua = (slug, id) => thuocTinh[slug].find((x) => x.id === id)?.ten ?? '—'
const baMau = (arr, id) => { const i = arr.indexOf(id); i === -1 ? arr.push(id) : arr.splice(i, 1) }

function taoBienThe() {
  if (!daChonDanhMuc.value) {
    toast.error('Chọn danh mục trước khi tạo biến thể.')
    return
  }
  if (laVot.value && (!chonMau.value.length || !chonTl.value.length || !chonCv.value.length)) {
    toast.error('Chọn ít nhất một màu sắc, một trọng lượng và một chu vi để tạo biến thể.')
    return
  }
  if (!laVot.value && !chonMau.value.length) {
    toast.error('Chọn ít nhất một màu sắc để tạo biến thể.')
    return
  }
  // Danh mục không phải vợt: trọng lượng, chu vi là "Không áp dụng"
  const dsTl = laVot.value ? chonTl.value : [idKad('trong-luong')]
  const dsCv = laVot.value ? chonCv.value : [idKad('chu-vi')]
  if (dsTl[0] == null || dsCv[0] == null) {
    toast.error(THIEU_KHONG_AP_DUNG)
    return
  }
  const cu = new Map(bienThe.value.map((r) => [r.key, r])) // giữ số lượng / giá đã nhập khi tạo lại
  const moi = []
  for (const m of chonMau.value) for (const t of dsTl) for (const c of dsCv) {
    const key = `${m}-${t}-${c}`
    moi.push(cu.get(key) ?? { key, idMauSac: m, idTrongLuong: t, idChuVi: c, soLuong: macDinh.soLuong, giaBan: macDinh.giaBan, chon: true })
  }
  bienThe.value = moi
  toast.success(`Đã tạo ${moi.length} biến thể.`)
}

const nhomMau = computed(() =>
  chonMau.value
    .map((m) => ({ idMauSac: m, ten: tenCua('mau-sac', m), rows: bienThe.value.filter((r) => r.idMauSac === m) }))
    .filter((g) => g.rows.length),
)
const tatCaChon = computed({
  get: () => bienThe.value.length > 0 && bienThe.value.every((r) => r.chon),
  set: (v) => bienThe.value.forEach((r) => { r.chon = v }),
})
const soChon = computed(() => bienThe.value.filter((r) => r.chon).length)
function apDungMacDinh() {
  if (!soChon.value) return toast.error('Chưa có biến thể nào được chọn.')
  for (const r of bienThe.value) if (r.chon) {
    if (macDinh.soLuong !== '') r.soLuong = macDinh.soLuong
    if (macDinh.giaBan !== '') r.giaBan = macDinh.giaBan
  }
}
const xoaBienThe = (key) => { bienThe.value = bienThe.value.filter((r) => r.key !== key) }
const slugify = (x) => String(x).normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').replace(/[^A-Za-z0-9]+/g, '').toUpperCase()

/* ----- Kiểm tra dữ liệu (validate) ----- */

const errors = reactive({}) // lưu lỗi của từng ô, ví dụ errors.ten = 'Nhập tên sản phẩm.'
const daBamLuu = ref(false) // chỉ hiện lỗi sau khi người dùng bấm Lưu lần đầu

// Kiểm tra từng ô. Trả về true nếu hợp lệ, false nếu có lỗi.
function kiemTra() {
  for (const key of Object.keys(errors)) {
    delete errors[key]
  }

  // Mã sản phẩm: do hệ thống tạo, vẫn kiểm tra lại cho chắc
  const ma = String(form.ma).trim().toUpperCase()
  if (ma === '') {
    errors.ma = 'Chưa có mã sản phẩm.'
  } else if (!/^[A-Z0-9]{3,20}$/.test(ma)) {
    errors.ma = 'Mã gồm 3-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  } else if (!isEdit && tatCaSanPham.value.some((p) => p.ma === ma)) {
    errors.ma = 'Mã sản phẩm đã tồn tại. Hãy tải lại trang để lấy mã mới.'
  }

  // Tên sản phẩm: bắt buộc, tối đa 255 ký tự (khớp độ dài cột trong SQL)
  const ten = String(form.ten).trim()
  if (ten === '') {
    errors.ten = 'Nhập tên sản phẩm.'
  } else if (ten.length > 255) {
    errors.ten = 'Tên sản phẩm tối đa 255 ký tự.'
  }

  // Các ô chọn đang hiện: bắt buộc phải chọn (giá trị '' nghĩa là chưa chọn). Độ cứng, điểm cân bằng chỉ cần với vợt
  for (const s of selectsHienThi.value) {
    if (form[s.field] === '') {
      errors[s.field] = 'Chọn ' + s.label.toLowerCase() + '.'
    }
  }
  if (daChonDanhMuc.value && !laVot.value && SELECTS.some((s) => s.chiVot && idKad(s.slug) == null)) {
    errors.idDanhMuc = THIEU_KHONG_AP_DUNG
  }

  // Ảnh: không bắt buộc, tối đa 10 ảnh gồm cả ảnh chính (đường dẫn ảnh do backend kiểm tra khi lưu)
  if (form.anh.length > TOI_DA_ANH) {
    errors.anh = `Mỗi sản phẩm tối đa ${TOI_DA_ANH} ảnh (gồm cả ảnh chính).`
  }

  // Biến thể (nếu có): mỗi dòng đang chọn phải có số lượng nguyên >= 0 và giá bán >= 1.000 ₫
  const loiDong = bienThe.value.filter((r) => r.chon).some((r) => {
    const sl = Number(r.soLuong), gia = Number(r.giaBan)
    return r.soLuong === '' || !Number.isInteger(sl) || sl < 0 || r.giaBan === '' || Number.isNaN(gia) || gia < 1000
  })
  if (loiDong) errors.bienThe = 'Mỗi biến thể đã chọn cần số lượng (số nguyên từ 0) và giá bán tối thiểu 1.000 ₫.'

  return Object.keys(errors).length === 0
}

// Sau khi đã bấm Lưu một lần, người dùng sửa ô nào thì kiểm tra lại ngay để lỗi biến mất
watch(form, () => {
  if (daBamLuu.value) kiemTra()
})

/* ----- Hình ảnh: tải từ máy lên ----- */

const inputFile = ref(null) // ô chọn file (ẩn), mở bằng nút "Thêm ảnh"
const dangTaiAnh = ref(false) // đang tải ảnh lên thì khóa nút Lưu
const loiAnh = ref('') // lỗi của lần tải gần nhất

function moChonFile() {
  if (inputFile.value) inputFile.value.click()
}

// Admin chọn 1 hoặc nhiều file: kiểm tra nhanh ở frontend, rồi tải từng file lên backend (backend kiểm tra lại)
async function chonFile(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = '' // để lần sau chọn lại đúng file đó vẫn nhận
  if (files.length === 0) return

  loiAnh.value = ''
  dangTaiAnh.value = true
  try {
    for (const f of files) {
      if (form.anh.length >= TOI_DA_ANH) {
        loiAnh.value = `Mỗi sản phẩm tối đa ${TOI_DA_ANH} ảnh (gồm cả ảnh chính).`
        break
      }
      if (!KIEU_ANH.includes(f.type)) {
        loiAnh.value = `"${f.name}": chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.`
        continue
      }
      if (f.size > TOI_DA_MB * 1024 * 1024) {
        loiAnh.value = `"${f.name}" lớn hơn ${TOI_DA_MB} MB.`
        continue
      }
      try {
        form.anh.push(await hinhAnhService.upload(f))
      } catch (loi) {
        loiAnh.value = loi.message
      }
    }
  } finally {
    dangTaiAnh.value = false
  }
}

// Bỏ ảnh khỏi sản phẩm (file đã tải vẫn nằm trên server, chỉ không gắn vào sản phẩm)
function xoaAnh(i) {
  form.anh.splice(i, 1)
}

// Đưa ảnh thứ i lên đầu danh sách = đặt làm ảnh chính
function datAnhChinh(i) {
  const [a] = form.anh.splice(i, 1)
  form.anh.unshift(a)
}

/* ----- Lưu ----- */

const tieuDeNut = computed(() => (isEdit ? 'Lưu thay đổi' : bienThe.value.length ? 'Lưu sản phẩm và biến thể' : 'Thêm sản phẩm'))

function guiForm() {
  if (dangTaiAnh.value || dangLuu.value) return // còn ảnh đang tải lên hoặc đang lưu thì chờ
  daBamLuu.value = true
  if (!kiemTra()) {
    if (errors.bienThe) toast.error(errors.bienThe)
    return // có lỗi thì dừng
  }
  if (isEdit) luuSanPham()
  else hoiLuu.value = true // thêm mới: hỏi xác nhận như video
}

// xacNhanCapNhat = true: người dùng đã đồng ý cập nhật sản phẩm đã tồn tại (xem hộp hỏi bên dưới)
async function luuSanPham(xacNhanCapNhat = false) {
  hoiLuu.value = false
  hoiCapNhat.value = null
  // Hợp lệ: chuẩn hóa dữ liệu rồi gọi API.
  // Number(...) vì giá trị lấy từ dropdown có thể là chuỗi, còn id trong dữ liệu là số.
  const duLieu = {
    ma: String(form.ma).trim().toUpperCase(),
    ten: String(form.ten).trim(),
    idDanhMuc: Number(form.idDanhMuc),
    idThuongHieu: Number(form.idThuongHieu),
    idXuatXu: Number(form.idXuatXu),
    idChatLieu: Number(form.idChatLieu),
    idDoCung: idTheoDanhMuc(SELECTS.find((s) => s.field === 'idDoCung')),
    idDiemCanBang: idTheoDanhMuc(SELECTS.find((s) => s.field === 'idDiemCanBang')),
    anhChinh: form.anh[0] || '',
    anhPhu: form.anh.slice(1),
    moTa: String(form.moTa).trim(),
  }
  dangLuu.value = true
  try {
    if (isEdit) {
      await sanPhamService.update(idSua, duLieu)
      toast.success('Đã lưu thay đổi sản phẩm.')
    } else {
      // Sản phẩm + biến thể lưu trong MỘT lần gọi (cùng thành công hoặc cùng hủy).
      // Mã biến thể do backend tự sinh theo mã sản phẩm + màu + trọng lượng + chu vi.
      duLieu.bienThes = bienThe.value
        .filter((r) => r.chon)
        .map((r) => ({
          idMauSac: r.idMauSac,
          idTrongLuong: r.idTrongLuong,
          idChuVi: r.idChuVi,
          giaBan: Number(r.giaBan),
          soLuongTon: Number(r.soLuong),
        }))
      duLieu.xacNhanCapNhat = xacNhanCapNhat
      await sanPhamService.create(duLieu)
      if (xacNhanCapNhat) toast.success('Đã cập nhật sản phẩm và biến thể.')
      else toast.success(bienThe.value.length ? `Đã thêm sản phẩm và ${soChon.value} biến thể.` : 'Đã thêm sản phẩm.')
    }
    router.push('/san-pham') // về danh sách, sản phẩm mới nằm ở đầu danh sách
  } catch (loi) {
    // Sản phẩm (cùng tên + danh mục + thương hiệu) đã tồn tại: chưa lưu gì, hỏi người dùng có muốn cập nhật không
    if (loi.response?.status === 409 && loi.response?.data?.code === 'SAN_PHAM_DA_TON_TAI') {
      hoiCapNhat.value = { message: loi.message, ...(loi.response.data.chiTiet || {}) }
    } else {
      toast.error(loi.message || 'Không thể lưu sản phẩm. Vui lòng thử lại.')
    }
  } finally {
    dangLuu.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <div class="d-flex justify-content-end">
      <RouterLink to="/san-pham" class="ad-btn text-decoration-none"><i class="bi bi-arrow-left" aria-hidden="true"></i> Quay lại danh sách</RouterLink>
    </div>

    <section v-if="dangTai" class="ad-card"><div class="ad-empty">Đang tải dữ liệu…</div></section>
    <section v-else-if="loiTai" class="ad-card">
      <div class="ad-empty">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>Không tải được dữ liệu</strong>
        <span>Hãy kiểm tra backend đã chạy chưa rồi tải lại trang.</span>
      </div>
    </section>

    <form v-else id="sp-form" class="ad-page" novalidate @submit.prevent="guiForm">
      <section class="ad-card">
        <div class="row g-3">
          <div class="col-md-3">
            <label class="ad-label" for="sp-ma">Mã sản phẩm</label>
            <input id="sp-ma" v-model="form.ma" type="text" class="form-control ad-control" :class="{ 'is-invalid': errors.ma }" readonly :aria-invalid="!!errors.ma" />
            <p v-if="errors.ma" class="ad-error">{{ errors.ma }}</p>
            <p v-else class="ad-hint">Hệ thống tự tạo, không đổi được.</p>
          </div>

          <div class="col-md-9">
            <label class="ad-label" for="sp-ten">Sản phẩm <span class="ad-required">*</span></label>
            <input
              id="sp-ten"
              v-model="form.ten"
              type="text"
              class="form-control ad-control"
              :class="{ 'is-invalid': errors.ten }"
              maxlength="255"
              placeholder="Nhập tên sản phẩm…"
              autocomplete="off"
              :aria-invalid="!!errors.ten"
            />
            <p v-if="errors.ten" class="ad-error">{{ errors.ten }}</p>
          </div>

          <div v-for="s in selectsHienThi" :key="s.field" class="col-md-6">
            <label class="ad-label" :for="`sp-${s.field}`">{{ s.label }} <span class="ad-required">*</span></label>
            <select
              :id="`sp-${s.field}`"
              v-model="form[s.field]"
              class="form-select ad-control"
              :class="{ 'is-invalid': errors[s.field] }"
              :aria-invalid="!!errors[s.field]"
            >
              <option value="">Chọn {{ s.label.toLowerCase() }}…</option>
              <option v-for="o in layLuaChon(thuocTinh[s.slug], spDangSua ? spDangSua[s.field] : null)" :key="o.id" :value="o.id">{{ o.ten }}</option>
            </select>
            <p v-if="errors[s.field]" class="ad-error">{{ errors[s.field] }}</p>
            <p v-else-if="s.field === 'idDanhMuc' && daChonDanhMuc && !laVot" class="ad-hint">Danh mục này không có độ cứng, điểm cân bằng, trọng lượng, chu vi cán nên các mục đó được ẩn.</p>
          </div>
        </div>
      </section>

      <!-- Tạo biến thể tự động (chỉ khi thêm mới; khi sửa dùng trang Biến thể sản phẩm) -->
      <section v-if="!isEdit" class="ad-card">
        <div class="ad-card-head">
          <span class="ad-icon-box"><i class="bi bi-layers" aria-hidden="true"></i></span>
          <div>
            <h2 class="ad-card-title">Biến thể sản phẩm</h2>
            <p v-if="!daChonDanhMuc" class="ad-card-sub">Chọn danh mục ở trên để hiện các thuộc tính biến thể phù hợp.</p>
            <p v-else-if="laVot" class="ad-card-sub">Chọn màu sắc, trọng lượng, chu vi cán vợt rồi bấm "Tạo biến thể tự động".</p>
            <p v-else class="ad-card-sub">Chọn màu sắc rồi bấm "Tạo biến thể tự động".</p>
          </div>
        </div>
        <div v-for="g in (daChonDanhMuc ? nhomTaoBienThe : [])" :key="g.slug" class="ad-gen-row">
          <span class="ad-label mb-0">{{ g.label }}</span>
          <div class="d-flex flex-wrap gap-2">
            <button v-for="o in dangHoatDong(g.slug)" :key="o.id" type="button" class="ad-gen-chip" :class="{ on: g.sel.includes(o.id) }" :aria-pressed="g.sel.includes(o.id)" @click="baMau(g.sel, o.id)">
              <i v-if="g.sel.includes(o.id)" class="bi bi-check2" aria-hidden="true"></i> {{ o.ten }}
            </button>
            <span v-if="!dangHoatDong(g.slug).length" class="text-muted small">Chưa có {{ g.label.toLowerCase() }} nào đang hoạt động.</span>
          </div>
        </div>
        <div v-if="daChonDanhMuc" class="d-flex justify-content-end mt-3">
          <button type="button" class="ad-btn ad-btn-primary" @click="taoBienThe"><i class="bi bi-magic" aria-hidden="true"></i> Tạo biến thể tự động</button>
        </div>

        <template v-if="bienThe.length">
          <hr />
          <div class="form-check mb-3">
            <input id="bt-all" v-model="tatCaChon" type="checkbox" class="form-check-input" />
            <label class="form-check-label fw-medium" for="bt-all">Chọn tất cả biến thể ({{ soChon }}/{{ bienThe.length }})</label>
          </div>
          <div class="row g-2 align-items-end mb-3">
            <div class="col-md-5"><label class="ad-label" for="bt-sl">Số lượng mặc định</label><input id="bt-sl" v-model="macDinh.soLuong" type="number" min="0" class="form-control ad-control" placeholder="0" /></div>
            <div class="col-md-5"><label class="ad-label" for="bt-gia">Giá bán mặc định <span class="ad-required">*</span></label><input id="bt-gia" v-model="macDinh.giaBan" type="number" min="0" class="form-control ad-control" placeholder="0" /></div>
            <div class="col-md-2"><button type="button" class="ad-btn w-100" @click="apDungMacDinh">Áp dụng</button></div>
          </div>
          <div v-for="g in nhomMau" :key="g.idMauSac" class="ad-gen-group">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <strong>{{ g.ten }}</strong>
              <span class="text-muted small">{{ g.rows.length }} biến thể</span>
            </div>
            <div class="table-responsive">
              <table class="table align-middle mb-0">
                <thead><tr><th style="width: 40px"></th><th style="width: 60px">STT</th><th v-if="laVot">Trọng lượng</th><th v-if="laVot">Chu vi</th><th style="width: 150px">Số lượng</th><th style="width: 190px">Giá bán</th><th style="width: 60px" class="text-end">Xóa</th></tr></thead>
                <tbody>
                  <tr v-for="(r, i) in g.rows" :key="r.key">
                    <td><input v-model="r.chon" type="checkbox" class="form-check-input" :aria-label="`Chọn biến thể ${i + 1} của ${g.ten}`" /></td>
                    <td>{{ i + 1 }}</td>
                    <td v-if="laVot">{{ tenCua('trong-luong', r.idTrongLuong) }}</td>
                    <td v-if="laVot">{{ tenCua('chu-vi', r.idChuVi) }}</td>
                    <td><input v-model="r.soLuong" type="number" min="0" class="form-control form-control-sm" :disabled="!r.chon" aria-label="Số lượng" /></td>
                    <td><input v-model="r.giaBan" type="number" min="0" class="form-control form-control-sm" :disabled="!r.chon" aria-label="Giá bán" /></td>
                    <td class="text-end"><button type="button" class="btn btn-sm btn-light border text-danger" title="Xóa biến thể này" @click="xoaBienThe(r.key)"><i class="bi bi-trash" aria-hidden="true"></i></button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <p v-if="errors.bienThe" class="ad-error">{{ errors.bienThe }}</p>
        </template>
      </section>

      <section class="ad-card">
        <label class="ad-label">Hình ảnh sản phẩm</label>
        <div class="d-flex flex-wrap gap-2">
          <div v-for="(url, i) in form.anh" :key="`${i}-${url}`" class="position-relative" style="width: 92px">
            <img :src="url" :alt="`Ảnh ${i + 1} của sản phẩm`" class="rounded-3 border" style="width: 92px; height: 92px; object-fit: cover" />
            <span v-if="i === 0" class="badge bg-primary position-absolute top-0 start-0 m-1">Ảnh chính</span>
            <button
              type="button"
              class="btn btn-sm btn-light border position-absolute top-0 end-0 m-1 py-0 px-1 lh-sm"
              title="Bỏ ảnh này"
              :aria-label="`Bỏ ảnh ${i + 1}`"
              @click="xoaAnh(i)"
            >
              <i class="bi bi-x-lg"></i>
            </button>
            <button v-if="i > 0" type="button" class="btn btn-link btn-sm p-0 w-100 text-decoration-none" @click="datAnhChinh(i)">Đặt làm ảnh chính</button>
          </div>

          <button
            v-if="form.anh.length < TOI_DA_ANH"
            type="button"
            class="d-flex flex-column align-items-center justify-content-center rounded-3 bg-white text-secondary"
            style="width: 92px; height: 92px; border: 2px dashed #b8c7de"
            :disabled="dangTaiAnh"
            @click="moChonFile"
          >
            <span v-if="dangTaiAnh" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <i v-else class="bi bi-image fs-4"></i>
            <span class="small">{{ dangTaiAnh ? 'Đang tải…' : 'Thêm ảnh' }}</span>
          </button>
        </div>
        <input ref="inputFile" type="file" class="d-none" accept="image/jpeg,image/png,image/webp,image/gif" multiple @change="chonFile" />
        <p v-if="loiAnh" class="ad-error">{{ loiAnh }}</p>
        <p v-else-if="errors.anh" class="ad-error">{{ errors.anh }}</p>
        <p v-else class="ad-hint">Không bắt buộc. Chọn ảnh JPG, PNG, WEBP, GIF (tối đa {{ TOI_DA_MB }} MB mỗi ảnh, {{ TOI_DA_ANH }} ảnh). Ảnh đầu tiên là ảnh chính.</p>

        <label class="ad-label mt-3" for="sp-mo-ta">Mô tả</label>
        <textarea id="sp-mo-ta" v-model="form.moTa" rows="3" class="form-control ad-control" placeholder="Đặc điểm nổi bật, đối tượng phù hợp…"></textarea>
      </section>

      <div class="d-flex justify-content-end gap-2">
        <RouterLink to="/san-pham" class="ad-btn text-decoration-none">Hủy</RouterLink>
        <button type="submit" class="ad-btn ad-btn-primary" :disabled="dangLuu || dangTaiAnh">
          <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          {{ tieuDeNut }}
        </button>
      </div>
    </form>

    <ConfirmDialog
      v-if="hoiLuu"
      title="Bạn có muốn lưu sản phẩm không?"
      :message="bienThe.length ? `Sản phẩm sẽ được lưu cùng ${soChon} biến thể đã chọn.` : 'Sản phẩm sẽ được lưu (chưa có biến thể).'"
      confirm-text="Xác nhận lưu"
      @confirm="luuSanPham(false)"
      @cancel="hoiLuu = false"
    />

    <!-- Sản phẩm đã tồn tại -> hỏi có muốn cập nhật không (thêm biến thể mới, biến thể trùng thì cập nhật giá / số lượng) -->
    <ConfirmQuestion
      v-if="hoiCapNhat"
      title="Sản phẩm đã tồn tại"
      :message="`${hoiCapNhat.message}${hoiCapNhat.soBienTheMoi || hoiCapNhat.soBienTheCapNhat ? ` Sẽ thêm ${hoiCapNhat.soBienTheMoi || 0} biến thể mới và cập nhật giá bán / số lượng của ${hoiCapNhat.soBienTheCapNhat || 0} biến thể đã có.` : ''}`"
      confirm-text="Cập nhật"
      :loading="dangLuu"
      @confirm="luuSanPham(true)"
      @cancel="hoiCapNhat = null"
    />
  </div>
</template>

<style scoped>
.ad-gen-row { display: grid; grid-template-columns: 110px 1fr; gap: 0.75rem; align-items: start; padding: 0.55rem 0; border-bottom: 1px dashed rgba(0, 0, 0, 0.08); }
.ad-gen-row .ad-label { padding-top: 0.45rem; }
.ad-gen-chip { border: 1px solid #d5e0ef; background: #fff; border-radius: 999px; padding: 0.3rem 0.9rem; font-size: 0.85rem; color: #1c2530; }
.ad-gen-chip:hover { border-color: var(--fs-primary, #0977ec); }
.ad-gen-chip.on { background: var(--fs-primary-soft, #e6f1fe); border-color: var(--fs-primary, #0977ec); color: var(--fs-primary-dark, #075fc0); font-weight: 600; }
.ad-gen-group { border: 1px solid #e3eaf3; border-radius: 14px; padding: 0.9rem 1rem; margin-bottom: 0.9rem; }
@media (max-width: 576px) { .ad-gen-row { grid-template-columns: 1fr; } }
</style>
