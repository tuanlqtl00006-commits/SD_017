<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { taoMa } from '../../constants/thuocTinh'
import { layLuaChon } from '../../utils/text'
import { NHOM_DANH_MUC, chatLieuHopDanhMuc, idKhongApDung, laDanhMucVot, laKhongApDung, nhomDanhMuc } from '../../utils/danhMuc'
import { tuDanhSach } from '../../utils/luaChon'
import {
  CHE_DO_HIEN_THI_MAC_DINH,
  TOI_DA_ANH_MOI_MAU,
  apDungMacDinh as apMacDinh,
  congThucSoBienThe,
  kiemTraAnhBatBuoc,
  kiemTraBienThe,
  nhomTheoMau,
  payloadAnhTheoMau,
  payloadBienThe,
  taoBienThe as taoToHop,
} from '../../utils/bienThe'
import { mauHex } from '../../utils/mau'
import { formatMoney } from '../../utils/format'
import { bienTheService, sanPhamService } from '../../services/sanPhamService'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import ConfirmQuestion from '../common/ConfirmQuestion.vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import { useThemNhanh } from '../../composables/useThemNhanh'
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
const THIEU_KHONG_AP_DUNG = 'CSDL chưa có mục "Không áp dụng" (độ cứng, điểm cân bằng, trọng lượng, chu vi). Hãy khởi động lại backend (hoặc chạy database/SD_17_du_lieu_va_nang_cap.sql) rồi tải lại trang.'

const TOI_DA_ANH = 10 // giống backend (gồm cả ảnh chính)
const TOI_DA_MB = 5 // giống backend
const KIEU_ANH = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const thuocTinh = reactive({})
for (const s of SELECTS) thuocTinh[s.slug] = []
for (const slug of ['mau-sac', 'trong-luong', 'chu-vi']) thuocTinh[slug] = [] // dùng để tạo biến thể
const tatCaSanPham = ref([]) // để sinh mã mới và kiểm tra trùng mã
const spDangSua = ref(null) // sản phẩm đang sửa (null khi thêm mới)
const mauCuaSpDangSua = ref([]) // khi sửa: các màu mà sản phẩm đã có biến thể (để sửa ảnh theo màu)

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
  anh: [], // ảnh CHUNG của sản phẩm, phần tử đầu tiên là ảnh chính (khi gửi sẽ tách thành anhChinh + anhPhu)
  moTa: '',
})
// Ảnh RIÊNG từng màu: { idMàu: [url...] }, phần tử đầu là ảnh chính của màu. Ảnh này dùng cho mọi biến thể cùng màu.
const anhTheoMau = reactive({})

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

// Các lựa chọn của một ô. Chất liệu chỉ hiện chất liệu hợp với danh mục đã chọn (vd Quần áo thể thao không có Nhôm / Titanium);
// khi sửa, chất liệu đang dùng vẫn được giữ lại để không mất giá trị cũ.
function luaChonCua(s) {
  const hienTai = spDangSua.value ? spDangSua.value[s.field] : null
  const ds = layLuaChon(thuocTinh[s.slug], hienTai)
  if (s.field !== 'idChatLieu') return ds
  if (!daChonDanhMuc.value) return []
  // Chất liệu cũ chỉ được giữ nguyên khi danh mục không đổi (đổi danh mục thì phải chọn chất liệu hợp danh mục mới)
  const giuCu = spDangSua.value && spDangSua.value.idDanhMuc === Number(form.idDanhMuc)
  return ds.filter((o) => (giuCu && o.id === hienTai) || chatLieuHopDanhMuc(o, tenDanhMuc.value))
}
const khoaOChon = (s) => s.field === 'idChatLieu' && !daChonDanhMuc.value
const dsChon = (s) => tuDanhSach(luaChonCua(s))

/* ----- "Thêm nhanh" thuộc tính ngay trong ô chọn (Select2) ----- */
// Chất liệu mới gắn với nhóm của danh mục đang chọn (danh mục lạ thì dùng cho mọi nhóm)
const nhomChatLieuMoi = () => {
  const nhom = nhomDanhMuc(tenDanhMuc.value)
  return nhom ? [nhom] : NHOM_DANH_MUC.map((n) => n.ma)
}
const { themNhanh, dangThem } = useThemNhanh({
  layDanhSach: (slug) => thuocTinh[slug],
  themVaoDanhSach: (slug, muc) => {
    thuocTinh[slug] = [muc, ...thuocTinh[slug].filter((x) => x.id !== muc.id)]
  },
  toast,
  nhomChatLieu: nhomChatLieuMoi,
})
// Ô chọn đơn (danh mục, thương hiệu, xuất xứ, chất liệu, độ cứng, điểm cân bằng): tạo xong chọn luôn mục mới
async function themNhanhOChon(s, ten) {
  const moi = await themNhanh(s.slug, ten)
  if (moi) form[s.field] = moi.id
}

// Đổi danh mục: xóa giá trị riêng của vợt khi chuyển sang danh mục khác (và ngược lại bắt chọn lại)
watch(
  () => form.idDanhMuc,
  () => {
    // Chất liệu đã chọn không hợp danh mục mới (vd từ Vợt sang Quần áo) thì bắt chọn lại
    if (form.idChatLieu !== '' && !luaChonCua(SELECTS.find((x) => x.field === 'idChatLieu')).some((o) => o.id === Number(form.idChatLieu))) {
      form.idChatLieu = ''
    }
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
    const [ketQuaThuocTinh, dsSanPham, spCanSua, dsBienThe] = await Promise.all([
      Promise.all(cacLoai.map((loai) => thuocTinhService.getAll(loai))),
      sanPhamService.getAll(),
      isEdit ? sanPhamService.getById(idSua) : Promise.resolve(null),
      isEdit ? bienTheService.getAll().catch(() => []) : Promise.resolve([]),
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
      // Ảnh theo màu: màu đã có ảnh + màu mà sản phẩm đã có biến thể (để thêm ảnh cho màu đó)
      for (const x of spCanSua.anhTheoMau || []) anhTheoMau[x.idMauSac] = [...(x.anh || [])]
      const mau = new Set((spCanSua.anhTheoMau || []).map((x) => x.idMauSac))
      for (const b of dsBienThe || []) if (b.idSanPham === idSua) mau.add(b.idMauSac)
      mauCuaSpDangSua.value = [...mau]
      for (const id of mau) if (!anhTheoMau[id]) anhTheoMau[id] = []
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

/* ----- Tạo biến thể: màu sắc x trọng lượng x chu vi (chỉ khi thêm mới) ----- */

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
const cheDo = ref(CHE_DO_HIEN_THI_MAC_DINH) // 'mau': nhóm theo màu (như video mẫu) | 'chung': một bảng chung
const hoiLuu = ref(false) // hiện hộp "Bạn có muốn lưu sản phẩm không?"
const hoiCapNhat = ref(null) // sản phẩm đã tồn tại: { message, ... } từ backend -> hỏi "bạn muốn cập nhật?"

const dangHoatDong = (slug) => thuocTinh[slug].filter((x) => x.hoatDong && !laKhongApDung(x)) // bỏ mục "Không áp dụng" (hệ thống tự gán)
const tenCua = (slug, id) => thuocTinh[slug].find((x) => x.id === id)?.ten ?? '—'
const baMau = (arr, id) => { const i = arr.indexOf(id); i === -1 ? arr.push(id) : arr.splice(i, 1) }
// Ô "Tìm hoặc thêm nhanh" cạnh các nút chọn màu / trọng lượng / chu vi: chọn một mục (hoặc tạo mới) thì tích luôn
const dsThemBienThe = (g) => tuDanhSach(dangHoatDong(g.slug).filter((o) => !g.sel.includes(o.id)))
function tichThem(g, id) {
  if (id !== '' && !g.sel.includes(id)) g.sel.push(id)
}
async function themNhanhBienThe(g, ten) {
  const moi = await themNhanh(g.slug, ten)
  if (moi) tichThem(g, moi.id)
}

// Số biến thể sẽ tạo + công thức (để tự đếm lại: 4 màu x 3 trọng lượng x 2 chu vi = 24)
const duKien = computed(() => congThucSoBienThe(chonMau.value.length, chonTl.value.length, chonCv.value.length, laVot.value))

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
  // Giữ số lượng / giá đã nhập của các dòng cũ khi bấm tạo lại
  bienThe.value = taoToHop(chonMau.value, dsTl, dsCv, bienThe.value, macDinh)
  for (const m of chonMau.value) if (!anhTheoMau[m]) anhTheoMau[m] = []
  toast.success(`Đã tạo ${bienThe.value.length} biến thể (${duKien.value.congThuc}).`)
}

const tatCaChon = computed({
  get: () => bienThe.value.length > 0 && bienThe.value.every((r) => r.chon),
  set: (v) => bienThe.value.forEach((r) => { r.chon = v }),
})
const soChon = computed(() => bienThe.value.filter((r) => r.chon).length)
function apDungMacDinh() {
  const kq = apMacDinh(bienThe.value, macDinh)
  if (!kq.ok) return toast.error(kq.loi)
  toast.success(`Đã áp dụng cho ${kq.soDong} biến thể đang chọn.`)
}
const xoaBienThe = (key) => { bienThe.value = bienThe.value.filter((r) => r.key !== key) }

// Nhóm theo màu (thứ tự màu = thứ tự đã chọn)
const nhomMau = computed(() => nhomTheoMau(bienThe.value, chonMau.value))
const nhomChon = (g) => g.rows.length > 0 && g.rows.every((r) => r.chon)
const doiChonNhom = (g, v) => g.rows.forEach((r) => { r.chon = v })
// Thông số của một dòng trong nhóm màu: "4U (80-84g) · G5" (danh mục khác chỉ có màu)
const thongSoDong = (r) => [tenCua('trong-luong', r.idTrongLuong), tenCua('chu-vi', r.idChuVi)].filter((x) => x && x !== '—' && x !== 'Không áp dụng').join(' · ')

/* ----- Ảnh theo màu ----- */
// Thêm mới: các màu đang có biến thể; sửa: các màu sản phẩm đã có biến thể / đã có ảnh
const mauCoAnh = computed(() => (isEdit ? mauCuaSpDangSua.value : nhomMau.value.map((g) => g.idMauSac)))
// Các màu cần có ảnh: thêm mới = màu của biến thể đang tích; sửa = màu sản phẩm đã có biến thể / đã có ảnh
const mauCanAnh = computed(() =>
  isEdit ? mauCuaSpDangSua.value : nhomMau.value.filter((g) => g.rows.some((r) => r.chon)).map((g) => g.idMauSac),
)
const soBienTheCuaMau = (idMau) => bienThe.value.filter((r) => r.idMauSac === idMau).length

/* ----- Kiểm tra dữ liệu (validate) ----- */

const errors = reactive({}) // lưu lỗi của từng ô, ví dụ errors.ten = 'Nhập tên sản phẩm.'
const daBamLuu = ref(false) // chỉ hiện lỗi sau khi người dùng bấm Lưu lần đầu
const loiDong = ref({}) // lỗi từng dòng biến thể: { key: { soLuong, giaBan } }

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

  // Ảnh BẮT BUỘC (cô: chưa có ảnh mà vẫn lưu thành công là sai): có ít nhất 1 ảnh chung, hoặc mọi màu đều có ảnh riêng.
  // Tối đa 10 ảnh gồm cả ảnh chính (đường dẫn ảnh do backend kiểm tra lại khi lưu)
  if (form.anh.length > TOI_DA_ANH) {
    errors.anh = `Mỗi sản phẩm tối đa ${TOI_DA_ANH} ảnh (gồm cả ảnh chính).`
  } else {
    const anh = kiemTraAnhBatBuoc({ anhChung: form.anh, anhTheoMau, mauCanAnh: mauCanAnh.value })
    if (!anh.ok) {
      errors.anh = anh.mauThieu.length && anh.mauThieu.length < mauCanAnh.value.length
        ? `Chưa có ảnh sản phẩm. Thêm ít nhất 1 ảnh chung, hoặc thêm ảnh cho đủ các màu còn thiếu: ${anh.mauThieu.map((m) => tenCua('mau-sac', m)).join(', ')}.`
        : 'Chưa có ảnh sản phẩm. Thêm ít nhất 1 ảnh chung của sản phẩm, hoặc thêm ảnh cho từng màu.'
    }
  }

  // Biến thể (nếu có): phải tích ít nhất 1 dòng; mỗi dòng đang tích cần số lượng nguyên >= 0 và giá bán >= 1.000 ₫
  const kq = kiemTraBienThe(bienThe.value)
  loiDong.value = kq.loiTheoKey
  if (bienThe.value.length && !soChon.value) {
    errors.bienThe = 'Chưa tích chọn biến thể nào. Tích các biến thể cần lưu (hoặc xóa hết biến thể nếu chỉ muốn lưu sản phẩm).'
  } else if (kq.soLoi) {
    errors.bienThe = kq.thongBao
  }

  return Object.keys(errors).length === 0
}

// Sau khi đã bấm Lưu một lần, người dùng sửa ô nào thì kiểm tra lại ngay để lỗi biến mất
watch([form, bienThe], () => {
  if (daBamLuu.value) kiemTra()
}, { deep: true })

/* ----- Hình ảnh: tải từ máy lên ----- */

const inputFile = ref(null) // ô chọn file (ẩn), mở bằng nút "Thêm ảnh"
const dangTaiAnh = ref(false) // đang tải ảnh lên thì khóa nút Lưu
const loiAnh = ref('') // lỗi của lần tải gần nhất (ảnh chung)
const loiAnhMau = reactive({}) // lỗi tải ảnh của từng màu
const mauDangTai = ref(null) // màu đang chờ chọn file / đang tải

function moChonFile() {
  if (inputFile.value) inputFile.value.click()
}

// Kiểm tra nhanh rồi tải lần lượt từng file lên backend (backend kiểm tra lại). dich: mảng url nhận ảnh.
async function taiAnhLen(files, dich, toiDa, baoLoi) {
  for (const f of files) {
    if (dich.length >= toiDa) {
      baoLoi(`Tối đa ${toiDa} ảnh.`)
      break
    }
    if (!KIEU_ANH.includes(f.type)) {
      baoLoi(`"${f.name}": chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.`)
      continue
    }
    if (f.size > TOI_DA_MB * 1024 * 1024) {
      baoLoi(`"${f.name}" lớn hơn ${TOI_DA_MB} MB.`)
      continue
    }
    try {
      const url = await hinhAnhService.upload(f)
      if (url && !dich.includes(url)) dich.push(url)
    } catch (loi) {
      baoLoi(loi.message || 'Không tải được ảnh.')
    }
  }
}

// Ảnh chung: admin chọn 1 hoặc nhiều file
async function chonFile(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = '' // để lần sau chọn lại đúng file đó vẫn nhận
  if (files.length === 0) return
  loiAnh.value = ''
  dangTaiAnh.value = true
  try {
    await taiAnhLen(files, form.anh, TOI_DA_ANH, (m) => { loiAnh.value = m })
  } finally {
    dangTaiAnh.value = false
  }
}

// Ảnh theo màu: mỗi màu có ô chọn file riêng
async function chonFileMau(idMau, e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (files.length === 0) return
  if (!anhTheoMau[idMau]) anhTheoMau[idMau] = []
  loiAnhMau[idMau] = ''
  dangTaiAnh.value = true
  mauDangTai.value = idMau
  try {
    await taiAnhLen(files, anhTheoMau[idMau], TOI_DA_ANH_MOI_MAU, (m) => { loiAnhMau[idMau] = m })
  } finally {
    dangTaiAnh.value = false
    mauDangTai.value = null
  }
}
function moChonFileMau(idMau) {
  document.getElementById(`sp-anh-mau-${idMau}`)?.click()
}

// Bỏ ảnh / đặt ảnh chính (dùng chung cho ảnh chung và ảnh theo màu)
const xoaAnhTrong = (ds, i) => ds.splice(i, 1)
function datAnhChinhTrong(ds, i) {
  const [a] = ds.splice(i, 1)
  ds.unshift(a)
}
// Giữ tên cũ cho ảnh chung (đã dùng ở nhiều chỗ)
const xoaAnh = (i) => xoaAnhTrong(form.anh, i)
const datAnhChinh = (i) => datAnhChinhTrong(form.anh, i)

/* ----- Lưu ----- */

const tieuDeNut = computed(() => (isEdit ? 'Lưu thay đổi' : bienThe.value.length ? 'Lưu sản phẩm và biến thể' : 'Thêm sản phẩm'))

function guiForm() {
  if (dangTaiAnh.value || dangLuu.value) return // còn ảnh đang tải lên hoặc đang lưu thì chờ
  daBamLuu.value = true
  if (!kiemTra()) {
    if (errors.bienThe) toast.error(errors.bienThe)
    else if (errors.anh) toast.error(errors.anh)
    // Kéo tới ô lỗi đầu tiên để người dùng thấy ngay chỗ cần sửa
    nextTick(() => {
      const o = document.querySelector('#sp-form .is-invalid') || document.getElementById('sp-anh-chung')
      o?.scrollIntoView?.({ block: 'center' })
    })
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
      // Sửa: gửi ảnh của mọi màu đang hiện (màu bỏ hết ảnh thì gửi danh sách rỗng để backend bỏ ảnh của màu đó)
      duLieu.anhTheoMau = payloadAnhTheoMau(anhTheoMau, mauCoAnh.value, false)
      await sanPhamService.update(idSua, duLieu)
      toast.success('Đã lưu thay đổi sản phẩm.')
    } else {
      // Sản phẩm + biến thể + ảnh theo màu lưu trong MỘT lần gọi (cùng thành công hoặc cùng hủy).
      // Mã biến thể do backend tự sinh theo mã sản phẩm + màu + trọng lượng + chu vi.
      duLieu.bienThes = payloadBienThe(bienThe.value)
      const mauDuocChon = [...new Set(duLieu.bienThes.map((b) => b.idMauSac))]
      duLieu.anhTheoMau = payloadAnhTheoMau(anhTheoMau, mauDuocChon)
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
            <Select2
              :id="`sp-${s.field}`"
              v-model="form[s.field]"
              :options="dsChon(s)"
              class="ad-control"
              :class="{ 'is-invalid': errors[s.field] }"
              :disabled="khoaOChon(s)"
              :aria-invalid="!!errors[s.field]"
              :placeholder="khoaOChon(s) ? 'Chọn danh mục trước…' : `Chọn ${s.label.toLowerCase()}…`"
              :allow-create="!khoaOChon(s)"
              :creating="dangThem"
              @create="themNhanhOChon(s, $event)"
            />
            <p v-if="errors[s.field]" class="ad-error">{{ errors[s.field] }}</p>
            <p v-else-if="s.field === 'idDanhMuc' && daChonDanhMuc && !laVot" class="ad-hint">Danh mục này không có độ cứng, điểm cân bằng, trọng lượng, chu vi cán nên các mục đó được ẩn.</p>
          </div>
        </div>
      </section>

      <!-- Tạo biến thể (chỉ khi thêm mới; khi sửa dùng trang Biến thể sản phẩm) -->
      <section v-if="!isEdit" class="ad-card">
        <div class="ad-card-head">
          <span class="ad-icon-box"><i class="bi bi-layers" aria-hidden="true"></i></span>
          <div>
            <h2 class="ad-card-title">Biến thể sản phẩm</h2>
            <p v-if="!daChonDanhMuc" class="ad-card-sub">Chọn danh mục ở trên để hiện các thuộc tính biến thể phù hợp.</p>
            <p v-else-if="laVot" class="ad-card-sub">Chọn màu sắc, trọng lượng, chu vi cán vợt rồi bấm "Tạo biến thể".</p>
            <p v-else class="ad-card-sub">Chọn màu sắc rồi bấm "Tạo biến thể".</p>
          </div>
        </div>
        <div v-for="g in (daChonDanhMuc ? nhomTaoBienThe : [])" :key="g.slug" class="ad-gen-row">
          <span class="ad-label mb-0">{{ g.label }}</span>
          <div class="d-flex flex-wrap gap-2">
            <button v-for="o in dangHoatDong(g.slug)" :key="o.id" type="button" class="ad-gen-chip" :class="{ on: g.sel.includes(o.id) }" :aria-pressed="g.sel.includes(o.id)" @click="baMau(g.sel, o.id)">
              <span v-if="g.slug === 'mau-sac'" class="bt-dot" :style="{ background: mauHex(o.ten) }" aria-hidden="true"></span>
              <i v-if="g.sel.includes(o.id)" class="bi bi-check2" aria-hidden="true"></i> {{ o.ten }}
            </button>
            <span v-if="!dangHoatDong(g.slug).length" class="text-muted small">Chưa có {{ g.label.toLowerCase() }} nào đang hoạt động.</span>
            <div class="bt-them-nhanh">
              <Select2
                :id="`sp-them-${g.slug}`"
                :model-value="''"
                :options="dsThemBienThe(g)"
                class="ad-control bt-them-nhanh-chon"
                :placeholder="`Tìm hoặc thêm nhanh ${g.label.toLowerCase()}…`"
                allow-create
                :creating="dangThem"
                @update:model-value="tichThem(g, $event)"
                @create="themNhanhBienThe(g, $event)"
              />
            </div>
          </div>
        </div>
        <div v-if="daChonDanhMuc" class="d-flex flex-wrap justify-content-end align-items-center gap-3 mt-3">
          <span v-if="duKien.so" class="bt-du-kien" data-testid="du-kien">Sẽ tạo {{ duKien.congThuc }}</span>
          <button type="button" class="ad-btn ad-btn-primary" @click="taoBienThe"><i class="bi bi-plus-circle" aria-hidden="true"></i> Tạo biến thể</button>
        </div>

        <template v-if="bienThe.length">
          <hr />
          <p class="bt-tong" data-testid="tong-bien-the">
            Tổng cộng <strong>{{ bienThe.length }}</strong> biến thể<template v-if="duKien.so === bienThe.length"> ({{ duKien.congThuc.replace(/ = .*$/, '') }})</template>
          </p>
          <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <div class="form-check mb-0">
              <input id="bt-all" v-model="tatCaChon" type="checkbox" class="form-check-input" />
              <label class="form-check-label fw-medium" for="bt-all">Chọn tất cả biến thể ({{ soChon }}/{{ bienThe.length }})</label>
            </div>
            <div class="ad-segment bt-view" role="radiogroup" aria-label="Cách hiển thị biến thể">
              <label class="ad-segment-item" :class="{ active: cheDo === 'mau' }">
                <input v-model="cheDo" type="radio" class="visually-hidden" name="bt-che-do" value="mau" /> <i class="bi bi-palette" aria-hidden="true"></i> Theo màu
              </label>
              <label class="ad-segment-item" :class="{ active: cheDo === 'chung' }">
                <input v-model="cheDo" type="radio" class="visually-hidden" name="bt-che-do" value="chung" /> <i class="bi bi-table" aria-hidden="true"></i> Bảng chung
              </label>
            </div>
          </div>
          <div class="row g-2 align-items-end mb-3">
            <div class="col-md-5"><label class="ad-label" for="bt-sl">Số lượng mặc định</label><input id="bt-sl" v-model="macDinh.soLuong" type="number" min="0" step="1" class="form-control ad-control" placeholder="0" /></div>
            <div class="col-md-5"><label class="ad-label" for="bt-gia">Giá bán mặc định <span class="ad-required">*</span></label><MoneyInput id="bt-gia" v-model="macDinh.giaBan" aria-label="Giá bán mặc định" /></div>
            <div class="col-md-2"><button type="button" class="ad-btn w-100" @click="apDungMacDinh">Áp dụng</button></div>
          </div>
          <p class="ad-hint mb-3">Nhập số lượng / giá bán mặc định rồi bấm "Áp dụng": giá trị được điền cho mọi biến thể đang tích.</p>

          <!-- Cách 1 (mặc định, như 2 video mẫu): mỗi màu một nhóm, bên dưới là các dòng trọng lượng x chu vi của màu đó -->
          <div v-if="cheDo === 'mau'" class="bt-groups">
            <div v-for="g in nhomMau" :key="g.idMauSac" class="ad-gen-group" :data-mau="g.idMauSac">
              <div class="bt-group-head">
                <label class="d-flex align-items-center gap-2 mb-0">
                  <input type="checkbox" class="form-check-input mt-0" :checked="nhomChon(g)" :aria-label="`Chọn mọi biến thể màu ${tenCua('mau-sac', g.idMauSac)}`" @change="doiChonNhom(g, $event.target.checked)" />
                  <span class="bt-dot bt-dot-lg" :style="{ background: mauHex(tenCua('mau-sac', g.idMauSac)) }" aria-hidden="true"></span>
                  <strong>{{ tenCua('mau-sac', g.idMauSac) }}</strong>
                </label>
                <span class="text-muted small">{{ g.rows.length }} biến thể</span>
              </div>
              <div class="table-responsive">
                <table class="table align-middle mb-0 bt-table">
                  <thead>
                    <tr>
                      <th style="width: 40px"></th>
                      <th style="width: 56px">STT</th>
                      <th v-if="laVot">Trọng lượng</th>
                      <th v-if="laVot">Chu vi</th>
                      <th v-else>Biến thể</th>
                      <th style="width: 150px">Số lượng</th>
                      <th style="width: 210px">Giá bán</th>
                      <th style="width: 60px" class="text-end">Xóa</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(r, i) in g.rows" :key="r.key" :class="{ 'bt-row-off': !r.chon }">
                      <td><input v-model="r.chon" type="checkbox" class="form-check-input" :aria-label="`Chọn biến thể ${tenCua('mau-sac', r.idMauSac)} ${thongSoDong(r)}`" /></td>
                      <td>{{ i + 1 }}</td>
                      <td v-if="laVot">{{ tenCua('trong-luong', r.idTrongLuong) }}</td>
                      <td v-if="laVot">{{ tenCua('chu-vi', r.idChuVi) }}</td>
                      <td v-else>{{ tenCua('mau-sac', r.idMauSac) }}</td>
                      <td>
                        <input v-model="r.soLuong" type="number" min="0" step="1" class="form-control form-control-sm" :class="{ 'is-invalid': loiDong[r.key]?.soLuong }" :disabled="!r.chon" aria-label="Số lượng" :title="loiDong[r.key]?.soLuong || ''" />
                      </td>
                      <td><MoneyInput v-model="r.giaBan" small :invalid="!!loiDong[r.key]?.giaBan" :disabled="!r.chon" aria-label="Giá bán" /></td>
                      <td class="text-end"><button type="button" class="btn btn-sm btn-light border text-danger" title="Xóa biến thể này" :aria-label="`Xóa biến thể ${tenCua('mau-sac', r.idMauSac)} ${thongSoDong(r)}`" @click="xoaBienThe(r.key)"><i class="bi bi-trash" aria-hidden="true"></i></button></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Cách 2: một bảng chung cho mọi biến thể (mỗi dòng là một tổ hợp Màu x Trọng lượng x Chu vi) -->
          <template v-else>
            <div class="d-flex justify-content-between align-items-center mb-2">
              <strong>Danh sách biến thể</strong>
              <span class="text-muted small">{{ bienThe.length }} biến thể</span>
            </div>
            <div class="table-responsive">
              <table class="table align-middle mb-0 bt-table">
                <thead>
                  <tr>
                    <th style="width: 40px"></th>
                    <th style="width: 60px">STT</th>
                    <th>Màu sắc</th>
                    <th v-if="laVot">Trọng lượng</th>
                    <th v-if="laVot">Chu vi</th>
                    <th style="width: 150px">Số lượng</th>
                    <th style="width: 210px">Giá bán</th>
                    <th style="width: 60px" class="text-end">Xóa</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(r, i) in bienThe" :key="r.key" :class="{ 'bt-row-off': !r.chon }">
                    <td><input v-model="r.chon" type="checkbox" class="form-check-input" :aria-label="`Chọn biến thể ${i + 1}`" /></td>
                    <td>{{ i + 1 }}</td>
                    <td class="ad-nowrap"><span class="bt-dot" :style="{ background: mauHex(tenCua('mau-sac', r.idMauSac)) }" aria-hidden="true"></span> {{ tenCua('mau-sac', r.idMauSac) }}</td>
                    <td v-if="laVot">{{ tenCua('trong-luong', r.idTrongLuong) }}</td>
                    <td v-if="laVot">{{ tenCua('chu-vi', r.idChuVi) }}</td>
                    <td><input v-model="r.soLuong" type="number" min="0" step="1" class="form-control form-control-sm" :class="{ 'is-invalid': loiDong[r.key]?.soLuong }" :disabled="!r.chon" aria-label="Số lượng" /></td>
                    <td><MoneyInput v-model="r.giaBan" small :invalid="!!loiDong[r.key]?.giaBan" :disabled="!r.chon" aria-label="Giá bán" /></td>
                    <td class="text-end"><button type="button" class="btn btn-sm btn-light border text-danger" title="Xóa biến thể này" @click="xoaBienThe(r.key)"><i class="bi bi-trash" aria-hidden="true"></i></button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
          <p v-if="errors.bienThe" class="ad-error mt-2" role="alert">{{ errors.bienThe }}</p>
        </template>
      </section>

      <!-- Ảnh theo từng màu (như 2 video mẫu): ảnh của màu áp dụng cho mọi biến thể cùng màu -->
      <section v-if="mauCoAnh.length" class="ad-card" data-testid="anh-theo-mau">
        <div class="ad-card-head">
          <span class="ad-icon-box"><i class="bi bi-images" aria-hidden="true"></i></span>
          <div>
            <h2 class="ad-card-title">Ảnh sản phẩm theo màu</h2>
            <p class="ad-card-sub">Thêm ảnh cho từng màu sắc (biến thể đại diện), ảnh tự áp dụng cho mọi biến thể cùng màu. Ảnh đầu tiên là ảnh chính của màu.</p>
          </div>
        </div>
        <div class="bt-anh-grid">
          <div v-for="idMau in mauCoAnh" :key="idMau" class="bt-anh-card" :data-mau="idMau">
            <div class="bt-anh-title">
              <span class="bt-dot" :style="{ background: mauHex(tenCua('mau-sac', idMau)) }" aria-hidden="true"></span>
              Ảnh sản phẩm màu {{ tenCua('mau-sac', idMau) }}
            </div>
            <div class="d-flex justify-content-between align-items-center gap-2 mb-2">
              <div class="small text-muted">
                <template v-if="!isEdit">Áp dụng cho {{ soBienTheCuaMau(idMau) }} biến thể cùng màu</template>
                <template v-else>Áp dụng cho mọi biến thể màu {{ tenCua('mau-sac', idMau) }}</template>
              </div>
              <button
                type="button"
                class="ad-btn ad-btn-primary bt-anh-btn"
                :disabled="dangTaiAnh || (anhTheoMau[idMau]?.length ?? 0) >= TOI_DA_ANH_MOI_MAU"
                @click="moChonFileMau(idMau)"
              >
                <span v-if="mauDangTai === idMau" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <i v-else class="bi bi-plus-lg" aria-hidden="true"></i> Thêm ảnh
              </button>
              <input :id="`sp-anh-mau-${idMau}`" type="file" class="d-none" accept="image/jpeg,image/png,image/webp,image/gif" multiple @change="chonFileMau(idMau, $event)" />
            </div>
            <div v-if="anhTheoMau[idMau]?.length" class="d-flex flex-wrap gap-2">
              <div v-for="(url, i) in anhTheoMau[idMau]" :key="`${i}-${url}`" class="position-relative" style="width: 84px">
                <img :src="url" :alt="`Ảnh ${i + 1} màu ${tenCua('mau-sac', idMau)}`" class="rounded-3 border" style="width: 84px; height: 84px; object-fit: cover" />
                <span v-if="i === 0" class="badge bg-primary position-absolute bottom-0 start-0 ms-1 mb-2">Ảnh chính</span>
                <button type="button" class="btn btn-sm btn-light border position-absolute top-0 end-0 m-1 py-0 px-1 lh-sm" :aria-label="`Bỏ ảnh ${i + 1} màu ${tenCua('mau-sac', idMau)}`" @click="xoaAnhTrong(anhTheoMau[idMau], i)">
                  <i class="bi bi-x-lg"></i>
                </button>
                <button v-if="i > 0" type="button" class="btn btn-link btn-sm p-0 w-100 text-decoration-none" @click="datAnhChinhTrong(anhTheoMau[idMau], i)">Đặt làm ảnh chính</button>
              </div>
            </div>
            <div v-else class="bt-anh-empty">
              <i class="bi bi-image" aria-hidden="true"></i>
              <span>Nhóm màu này chưa có ảnh</span>
            </div>
            <p v-if="loiAnhMau[idMau]" class="ad-error mb-0">{{ loiAnhMau[idMau] }}</p>
          </div>
        </div>
      </section>

      <section id="sp-anh-chung" class="ad-card">
        <label class="ad-label">Ảnh chung của sản phẩm <span class="ad-required">*</span></label>
        <div class="d-flex flex-wrap gap-2">
          <div v-for="(url, i) in form.anh" :key="`${i}-${url}`" class="position-relative" style="width: 92px">
            <img :src="url" :alt="`Ảnh ${i + 1} của sản phẩm`" class="rounded-3 border" style="width: 92px; height: 92px; object-fit: cover" />
            <span v-if="i === 0" class="badge bg-primary position-absolute bottom-0 start-0 ms-1 mb-2">Ảnh chính</span>
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
            <span v-if="dangTaiAnh && mauDangTai === null" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <i v-else class="bi bi-image fs-4"></i>
            <span class="small">{{ dangTaiAnh && mauDangTai === null ? 'Đang tải…' : 'Thêm ảnh' }}</span>
          </button>
        </div>
        <input id="sp-file-anh-chung" ref="inputFile" type="file" class="d-none" accept="image/jpeg,image/png,image/webp,image/gif" multiple @change="chonFile" />
        <p v-if="loiAnh" class="ad-error">{{ loiAnh }}</p>
        <p v-else-if="errors.anh" class="ad-error" role="alert" data-testid="loi-anh">{{ errors.anh }}</p>
        <p v-else class="ad-hint">Bắt buộc có ảnh: thêm ít nhất 1 ảnh chung, hoặc ảnh riêng cho từng màu ở phần trên. Chọn ảnh JPG, PNG, WEBP, GIF (tối đa {{ TOI_DA_MB }} MB mỗi ảnh, {{ TOI_DA_ANH }} ảnh). Ảnh đầu tiên là ảnh chính.</p>

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
.ad-gen-chip { display: inline-flex; align-items: center; gap: 0.35rem; border: 1px solid #d5e0ef; background: #fff; border-radius: 999px; padding: 0.3rem 0.9rem; font-size: 0.85rem; color: #1c2530; }
.ad-gen-chip:hover { border-color: var(--fs-primary, #0977ec); }
.ad-gen-chip.on { background: var(--fs-primary-soft, #e6f1fe); border-color: var(--fs-primary, #0977ec); color: var(--fs-primary-dark, #075fc0); font-weight: 600; }
.bt-dot { display: inline-block; width: 12px; height: 12px; border-radius: 50%; border: 1px solid rgba(15, 23, 42, 0.2); vertical-align: -1px; flex: none; }
.bt-dot-lg { width: 14px; height: 14px; }
.bt-du-kien { font-size: 0.85rem; color: var(--fs-text-2, #4b5563); background: var(--fs-surface, #f5f8fc); border: 1px dashed #c9d6e8; border-radius: 999px; padding: 0.3rem 0.85rem; }
.bt-view { width: auto; }
.bt-them-nhanh { flex: 0 1 17rem; min-width: 12rem; }
.bt-them-nhanh :deep(.s2-trigger) { height: 36px; border-radius: 999px; font-size: 0.85rem; padding-top: 0.25rem; padding-bottom: 0.25rem; }
.bt-tong { margin: 0 0 0.6rem; font-size: 0.9rem; color: var(--fs-text-2, #4b5563); }
.bt-view .ad-segment-item { white-space: nowrap; }
.bt-anh-title { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; margin-bottom: 0.15rem; }
.bt-anh-btn { padding: 0.3rem 0.75rem; font-size: 0.82rem; white-space: nowrap; }
.bt-groups { display: flex; flex-direction: column; gap: 1rem; }
.ad-gen-group { border: 1px solid #e3eaf3; border-radius: 12px; overflow: hidden; }
.bt-group-head { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; padding: 0.6rem 0.9rem; background: var(--fs-surface, #f5f8fc); border-bottom: 1px solid #e3eaf3; }
.ad-gen-group .bt-table th:first-child, .ad-gen-group .bt-table td:first-child { padding-left: 0.9rem; }
.bt-table thead th { font-size: 0.8rem; color: var(--fs-text-2, #4b5563); font-weight: 600; background: #fff; }
.bt-row-off td { opacity: 0.55; }
.bt-anh-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem; }
.bt-anh-card { border: 1px solid #e3eaf3; border-radius: 12px; padding: 0.85rem 1rem; background: #fff; }
.bt-anh-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.25rem; min-height: 96px; border: 2px dashed #d5e0ef; border-radius: 10px; color: #8a97a8; font-size: 0.85rem; }
.bt-anh-empty .bi { font-size: 1.4rem; }
@media (max-width: 576px) { .ad-gen-row { grid-template-columns: 1fr; } }
</style>
