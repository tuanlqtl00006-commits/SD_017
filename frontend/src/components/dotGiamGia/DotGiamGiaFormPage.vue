<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BasePagination from '../common/BasePagination.vue'
import DotGiamGiaLichCard from './DotGiamGiaLichCard.vue'
import DotGiamGiaChongDotCard from './DotGiamGiaChongDotCard.vue'
import ConfirmQuestion from '../common/ConfirmQuestion.vue'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { bienTheService, sanPhamService } from '../../services/sanPhamService'
import { thuocTinhService } from '../../services/thuocTinhService'
import { usePagination } from '../../composables/usePagination'
import { useToast } from '../../composables/useToast'
import { TRANG_THAI, dangHoatDong, tinhTrangThai } from '../../constants/dotGiamGia'
import { formatMoney } from '../../utils/format'
import { dinhDangMa } from '../../utils/ma'
import { tinhGiaSauGiam } from '../../utils/giamGia'
import { mauHex } from '../../utils/mau'
import { includesText, normalizeText } from '../../utils/text'
import { laKhongApDung } from '../../utils/danhMuc'
import {
  bayGioDayDu,
  bayGioIso,
  dinhDangKhoangPhut,
  dinhDangNgayGio,
  kiemTraKhoangThoiGian,
  mocBatDau,
  mocKetThuc,
  ngayGioGuiApi,
  soPhutGiua,
  tachNgayGio,
} from '../../utils/ngayGio'
import DateTimeInput from '../common/DateTimeInput.vue'
import Select2 from '../common/Select2.vue'
import { tuDanhSach } from '../../utils/luaChon'

/*
 * Trang THÊM đợt giảm giá (/dot-giam-gia/them) và CHI TIẾT / SỬA đợt giảm giá (/dot-giam-gia/:id), giống video:
 *  - Thẻ trái "Thông tin đợt giảm": mã đợt (tự sinh, bấm nút xoay để đổi), tên, giá trị giảm (%), NGÀY GIỜ bắt đầu / kết thúc,
 *    "Trạng thái dự kiến" (cập nhật ngay khi chọn giờ), mô tả. Lỗi hiện ngay dưới từng ô.
 *  - Thẻ phải "Chọn sản phẩm áp dụng": tìm theo tên / mã, lọc màu sắc + trọng lượng, tích chọn sản phẩm (chọn hết biến thể)
 *    hoặc bấm "+" để chọn từng biến thể.
 *  - Thẻ "Tổng kết đợt giảm giá": mức giảm, số sản phẩm, số biến thể, ngày giờ bắt đầu, ngày giờ kết thúc, thời lượng.
 *  - Thẻ dưới "Sản phẩm & biến thể đã chọn áp dụng": MỖI DÒNG MỘT BIẾN THỂ (ảnh, sản phẩm, biến thể, giá bán, giá sau giảm),
 *    lọc theo màu sắc / trọng lượng / chu vi, xóa từng dòng hoặc gỡ cả nhóm đang lọc.
 *  - Thẻ "Khi nhiều đợt giảm giá chồng nhau": cách tính đang dùng (cao nhất / cộng dồn / trung bình), ví dụ chạy thử và CẢNH BÁO
 *    TRÙNG ĐỢT với các đợt khác trên những biến thể đã chọn (theo ngày giờ).
 *  - Bấm "Tạo đợt giảm giá" thì hỏi xác nhận rồi mới lưu; lưu xong quay về danh sách.
 *  - Chỉ sản phẩm đang kinh doanh, biến thể đang bán mới chọn được (danh mục "giày" của CSDL cũ bị ẩn vì cửa hàng chỉ bán vợt).
 *
 * THỜI GIAN (cùng quy ước với phiếu giảm giá, backend: ThoiGianPhut)
 *  - Tính theo PHÚT: bắt đầu từ đầu phút bắt đầu, kết thúc tới hết phút kết thúc (kết thúc 17:00 là dùng được tới 17:00:59).
 *  - Thêm mới: giờ bắt đầu không ở quá khứ (cho lệch tối đa 5 phút để bấm "Bây giờ" rồi lưu vẫn được), giờ kết thúc không ở quá khứ.
 *  - Sửa: đợt ĐÃ bắt đầu thì không đổi được ngày giờ bắt đầu; ngày giờ mới (bắt đầu / kết thúc) không được ở quá khứ.
 *
 * QUY ĐỊNH KHI SỬA ĐỢT GIẢM GIÁ
 *  - Được sửa: tên, giá trị giảm, ngày giờ kết thúc (và ngày giờ bắt đầu nếu đợt chưa bắt đầu), mô tả, danh sách biến thể áp dụng.
 *  - Không sửa: mã đợt (hệ thống cấp). Bật / tắt hoạt động bằng công tắc ở trang danh sách (không xóa dữ liệu, công tắc biến mất khi đợt đã kết thúc).
 */
const route = useRoute()
const router = useRouter()
const toast = useToast()

const idXem = route.params.id ? Number(route.params.id) : null // null: thêm mới; có số: xem / sửa đợt này
const isEdit = idXem !== null

const dangTai = ref(true)
const loiTai = ref('')
const dangLuu = ref(false)
const item = ref(null) // đợt đang xem / sửa (null khi thêm mới)

/* ----- Dữ liệu nguồn ----- */
const sanPhams = ref([])
const bienThes = ref([])
const mauSacs = ref([])
const trongLuongs = ref([])
const chuVis = ref([])
const danhMucs = ref([])
const maDaCo = ref(new Set()) // mã các đợt hiện có (viết hoa), để sinh mã không trùng

const spMap = computed(() => new Map(sanPhams.value.map((s) => [s.id, s])))
const mauMap = computed(() => new Map(mauSacs.value.map((x) => [x.id, x.ten])))
const tenHienThi = (x) => (laKhongApDung(x) ? '—' : x.ten) // mục "Không áp dụng" của danh mục không phải vợt
const tlMap = computed(() => new Map(trongLuongs.value.map((x) => [x.id, tenHienThi(x)])))
const cvMap = computed(() => new Map(chuVis.value.map((x) => [x.id, tenHienThi(x)])))
const dmMap = computed(() => new Map(danhMucs.value.map((x) => [x.id, x.ten])))

// Biến thể kèm tên sản phẩm / màu / trọng lượng / chu vi để hiển thị
const bienTheDayDu = computed(() =>
  bienThes.value.map((v) => ({
    ...v,
    sp: spMap.value.get(v.idSanPham) ?? null,
    mau: mauMap.value.get(v.idMauSac) ?? '',
    tl: tlMap.value.get(v.idTrongLuong) ?? '',
    cv: cvMap.value.get(v.idChuVi) ?? '',
  })),
)
const bienTheById = computed(() => new Map(bienTheDayDu.value.map((v) => [v.id, v])))

// Cửa hàng chỉ bán vợt cầu lông: sản phẩm thuộc danh mục giày (dữ liệu cũ) không cho chọn
const laGiay = (sp) => normalizeText(dmMap.value.get(sp?.idDanhMuc) ?? '').includes('giay')

// Chỉ cho chọn biến thể đang bán của sản phẩm đang kinh doanh
const chonDuoc = computed(() => bienTheDayDu.value.filter((v) => v.hoatDong && v.sp?.hoatDong && !laGiay(v.sp)))

/* ----- Form thông tin đợt ----- */
// 'yyyy-MM-ddTHH:mm:ss' (hoặc chỉ ngày, đợt cũ) từ backend -> 'yyyy-MM-ddTHH:mm' cho ô ngày giờ
const veGioPhut = (v, gioMacDinh) => {
  const { ngay, gio } = tachNgayGio(v)
  return ngay ? `${ngay}T${gio || gioMacDinh}` : ''
}

// Mã dự phòng sinh ngay trên trình duyệt (DGG + số thứ tự, vd DGG09, không trùng mã đã có) khi chưa hỏi được backend
function taoMaDot() {
  let max = 0
  for (const m of maDaCo.value) {
    const r = /^DGG(\d{1,6})$/.exec(m)
    if (r) max = Math.max(max, Number(r[1]))
  }
  let ma
  do {
    max += 1
    ma = dinhDangMa('DGG', max)
  } while (maDaCo.value.has(ma))
  return ma
}
// Lấy mã mới từ backend (đảm bảo không trùng trong CSDL); backend chưa trả lời được thì dùng mã dự phòng
async function lamMoiMa() {
  try {
    const ma = await dotGiamGiaService.maMoi()
    form.maDot = ma ? String(ma).toUpperCase() : taoMaDot()
  } catch {
    form.maDot = taoMaDot()
  }
}

const form = reactive({
  maDot: '',
  tenDot: '',
  phanTram: '',
  batDau: bayGioIso(), // 'yyyy-MM-ddTHH:mm' - mặc định: bắt đầu ngay bây giờ (đổi được)
  ketThuc: '', // 'yyyy-MM-ddTHH:mm'
  moTa: '',
})

/* ----- "Bây giờ" chạy theo đồng hồ (trạng thái dự kiến, khóa giờ bắt đầu, cảnh báo quá khứ cập nhật theo) ----- */
const bayGio = ref(bayGioDayDu())
let dongHo = null
onMounted(() => {
  dongHo = setInterval(() => (bayGio.value = bayGioDayDu()), 20000)
})
onBeforeUnmount(() => clearInterval(dongHo))

// Đợt đã bắt đầu (khi SỬA) thì không đổi được ngày giờ bắt đầu
const khoaBatDau = computed(() => isEdit && !!item.value?.ngayBatDau && mocBatDau(item.value.ngayBatDau) <= bayGio.value)

/* ----- Biến thể đã chọn ----- */
const daChon = ref(new Set()) // id biến thể
const soDaChon = computed(() => daChon.value.size)

function themVaoChon(ids) {
  const s = new Set(daChon.value)
  ids.forEach((id) => s.add(id))
  daChon.value = s
}
function boKhoiChon(ids) {
  const s = new Set(daChon.value)
  ids.forEach((id) => s.delete(id))
  daChon.value = s
}
const idsDangChon = (ids) => ids.filter((id) => daChon.value.has(id)).length

/* ----- Thẻ phải: chọn sản phẩm ----- */
const loc = ref({ tuKhoa: '', mau: '', tl: '', cv: '' }) // bộ lọc đang áp dụng: chỉ còn ô tìm kiếm, gõ tới đâu lọc tới đó

// Mỗi dòng = một sản phẩm kèm các biến thể khớp bộ lọc màu / trọng lượng
const dongSanPham = computed(() => {
  const nhom = new Map()
  for (const v of chonDuoc.value) {
    if (loc.value.mau && String(v.idMauSac) !== loc.value.mau) continue
    if (loc.value.tl && String(v.idTrongLuong) !== loc.value.tl) continue
    if (loc.value.cv && String(v.idChuVi) !== loc.value.cv) continue
    if (!nhom.has(v.idSanPham)) nhom.set(v.idSanPham, [])
    nhom.get(v.idSanPham).push(v)
  }
  const rows = []
  for (const sp of sanPhams.value) {
    const variants = nhom.get(sp.id)
    if (!variants) continue
    if (loc.value.tuKhoa && !includesText(`${sp.ma} ${sp.ten}`, loc.value.tuKhoa)) continue
    rows.push({ sp, variants, ids: variants.map((v) => v.id) })
  }
  return rows
})

const { page, pageSize, total, items: dongTrang } = usePagination(dongSanPham, 5)
watch(() => loc.value.tuKhoa, () => {
  page.value = 1
})

function trangThaiDong(row) {
  const n = idsDangChon(row.ids)
  return { checked: n > 0 && n === row.ids.length, partial: n > 0 && n < row.ids.length }
}
function doiSanPham(row) {
  if (trangThaiDong(row).checked) boKhoiChon(row.ids)
  else themVaoChon(row.ids)
}
function doiBienThe(v) {
  if (daChon.value.has(v.id)) boKhoiChon([v.id])
  else themVaoChon([v.id])
}

// Ô tích ở tiêu đề: chọn / bỏ chọn mọi biến thể của các sản phẩm đang hiển thị ở trang này
const idsTrang = computed(() => dongTrang.value.flatMap((r) => r.ids))
const tieuDe = computed(() => {
  const n = idsDangChon(idsTrang.value)
  return { checked: idsTrang.value.length > 0 && n === idsTrang.value.length, partial: n > 0 && n < idsTrang.value.length }
})
function doiTatCaTrang() {
  if (tieuDe.value.checked) boKhoiChon(idsTrang.value)
  else themVaoChon(idsTrang.value)
}

const moRong = ref(new Set()) // id sản phẩm đang mở danh sách biến thể
function doiMoRong(id) {
  const s = new Set(moRong.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  moRong.value = s
}

/* ----- Thẻ dưới: biến thể đã chọn ----- */
const dsChon = computed(() => {
  const ds = [...daChon.value].map((id) => bienTheById.value.get(id)).filter(Boolean)
  ds.sort((a, b) => (a.sp?.ten ?? '').localeCompare(b.sp?.ten ?? '', 'vi') || String(a.ma).localeCompare(String(b.ma)))
  return ds
})
// Bảng biến thể đã chọn: lọc theo màu sắc / kích cỡ (trọng lượng, chu vi cán)
const locChon = reactive({ mau: '', tl: '', cv: '' })
const giaTriLoc = (field, ten) => {
  const map = new Map()
  for (const v of dsChon.value) {
    const t = ten(v)
    if (t && t !== '—') map.set(v[field], t)
  }
  return [...map].map(([id, t]) => ({ id, ten: t })).sort((a, b) => a.ten.localeCompare(b.ten, 'vi'))
}
const locMau = computed(() => giaTriLoc('idMauSac', (v) => v.mau))
const locTl = computed(() => giaTriLoc('idTrongLuong', (v) => v.tl))
const locCv = computed(() => giaTriLoc('idChuVi', (v) => v.cv))
const dsLocMau = computed(() => tuDanhSach(locMau.value, { tatCa: 'Tất cả màu sắc' }))
const dsLocTl = computed(() => tuDanhSach(locTl.value, { tatCa: 'Tất cả trọng lượng' }))
const dsLocCv = computed(() => tuDanhSach(locCv.value, { tatCa: 'Tất cả chu vi' }))
const dsChonLoc = computed(() =>
  dsChon.value.filter(
    (v) =>
      (locChon.mau === '' || v.idMauSac === locChon.mau) &&
      (locChon.tl === '' || v.idTrongLuong === locChon.tl) &&
      (locChon.cv === '' || v.idChuVi === locChon.cv),
  ),
)
const dangLocChon = computed(() => locChon.mau !== '' || locChon.tl !== '' || locChon.cv !== '')
const datLaiLocChon = () => Object.assign(locChon, { mau: '', tl: '', cv: '' })
const { page: pageChon, pageSize: pageSizeChon, total: totalChon, items: dongChon } = usePagination(dsChonLoc, 8)
watch(locChon, () => { pageChon.value = 1 })
// Bỏ hết lọc khi lựa chọn đang lọc không còn biến thể nào (vd vừa gỡ hết màu Đỏ)
watch(dsChon, () => {
  if (locChon.mau !== '' && !locMau.value.some((o) => o.id === locChon.mau)) locChon.mau = ''
  if (locChon.tl !== '' && !locTl.value.some((o) => o.id === locChon.tl)) locChon.tl = ''
  if (locChon.cv !== '' && !locCv.value.some((o) => o.id === locChon.cv)) locChon.cv = ''
})
const goDangLoc = () => boKhoiChon(dsChonLoc.value.map((v) => v.id))

const nhanBienThe = (v) => [v.mau, v.tl, v.cv].filter((x) => x && x !== '—').join(' · ') || v.ma
const anhBienThe = (v) => v.hinhAnh || v.sp?.anhDaiDien || v.sp?.anhChinh || ''

// Giá sau giảm theo % đang nhập (chưa nhập đúng thì chưa tính)
const phanTramHopLe = computed(() => {
  const n = Number(form.phanTram)
  return form.phanTram !== '' && Number.isInteger(n) && n >= 1 && n <= 100 ? n : null
})
const giaSauGiam = (v) => (phanTramHopLe.value === null ? null : tinhGiaSauGiam(v.giaBan, phanTramHopLe.value))

/* ----- Kiểm tra dữ liệu (cùng một bộ luật cho THÊM và SỬA; lỗi hiện ngay dưới từng ô) ----- */
const chamVao = reactive({}) // ô người dùng đã nhập / rời khỏi -> mới hiện lỗi của ô đó
const submitted = ref(false)
const cham = (khoa) => {
  chamVao[khoa] = true
}
watch(() => form.batDau, () => cham('batDau'))
watch(() => form.ketThuc, () => cham('ketThuc'))
watch(() => form.phanTram, () => cham('phanTram'))
watch(() => form.moTa, () => cham('moTa'))
watch(
  () => daChon.value.size,
  () => cham('bienThe'),
)

function validate() {
  const e = {}
  const ten = form.tenDot.trim()
  if (!ten) e.tenDot = 'Vui lòng nhập tên đợt giảm giá.'
  else if (ten.length > 255) e.tenDot = 'Tên đợt giảm giá tối đa 255 ký tự.'

  if (form.phanTram === '' || form.phanTram === null) e.phanTram = 'Vui lòng nhập giá trị giảm.'
  else if (phanTramHopLe.value === null) e.phanTram = 'Giá trị giảm phải là số nguyên từ 1 đến 100.'

  const loiGio = kiemTraKhoangThoiGian({
    batDau: form.batDau,
    ketThuc: form.ketThuc,
    batDauCu: item.value?.ngayBatDau ?? null,
    ketThucCu: item.value?.ngayKetThuc ?? null,
    taoMoi: !isEdit,
    ten: 'Đợt',
    bayGio: bayGio.value,
  })
  if (loiGio.batDau) e.batDau = loiGio.batDau
  if (loiGio.ketThuc) e.ketThuc = loiGio.ketThuc

  if (form.moTa.length > 500) e.moTa = 'Mô tả tối đa 500 ký tự.'
  if (!daChon.value.size) e.bienThe = 'Vui lòng chọn ít nhất một sản phẩm / biến thể áp dụng.'
  return e
}
const loiTatCa = computed(validate)
// Chỉ hiện lỗi của ô đã chạm vào (hoặc tất cả sau khi bấm lưu)
const errors = computed(() => {
  const kq = {}
  for (const [khoa, noiDung] of Object.entries(loiTatCa.value)) {
    if (submitted.value || chamVao[khoa]) kq[khoa] = noiDung
  }
  return kq
})

/* ----- Trạng thái dự kiến (xem trước ngay khi chọn giờ) ----- */
const khoangHopLe = computed(() => {
  const bd = mocBatDau(form.batDau)
  const kt = mocKetThuc(form.ketThuc)
  return !!bd && !!kt && kt.slice(0, 16) > bd.slice(0, 16)
})
const trangThaiDuKien = computed(() => {
  if (!khoangHopLe.value) return null
  const hoatDong = isEdit ? dangHoatDong(item.value ?? {}) : true
  return tinhTrangThai({ trangThai: hoatDong ? 1 : 0, ngayBatDau: form.batDau, ngayKetThuc: form.ketThuc }, bayGio.value)
})
const moTaTrangThai = computed(() => {
  const tt = trangThaiDuKien.value
  if (!tt) return ''
  if (tt === 'SAP_DIEN_RA') return `Bắt đầu lúc ${dinhDangNgayGio(form.batDau)}, còn ${dinhDangKhoangPhut(soPhutGiua(bayGio.value, form.batDau))} nữa.`
  if (tt === 'DANG_DIEN_RA') return `Đang giảm giá, kết thúc lúc ${dinhDangNgayGio(form.ketThuc)} (còn ${dinhDangKhoangPhut(soPhutGiua(bayGio.value, mocKetThuc(form.ketThuc)))}).`
  if (tt === 'DA_KET_THUC') return `Đã quá giờ kết thúc ${dinhDangNgayGio(form.ketThuc)}.`
  return 'Công tắc của đợt đang tắt nên đợt chưa áp dụng (bật lại ở danh sách đợt giảm giá).'
})

/* ----- Tổng kết đợt giảm giá ----- */
const soSanPhamChon = computed(() => new Set(dsChon.value.map((v) => v.idSanPham)).size)
// Đợt trọn các ngày (00:00 -> 23:59) thì đếm số ngày; còn lại hiện số giờ / phút
const thoiLuongDot = computed(() => {
  if (!khoangHopLe.value) return ''
  const bd = tachNgayGio(form.batDau)
  const kt = tachNgayGio(form.ketThuc)
  if ((bd.gio || '00:00') === '00:00' && (kt.gio || '23:59') === '23:59') {
    const soNgay = Math.round((Date.parse(`${kt.ngay}T00:00:00Z`) - Date.parse(`${bd.ngay}T00:00:00Z`)) / 86400000) + 1
    return `${soNgay} ngày`
  }
  return dinhDangKhoangPhut(soPhutGiua(form.batDau, form.ketThuc))
})
const tongGiaGoc = computed(() => dsChon.value.reduce((s, v) => s + (Number(v.giaBan) || 0), 0))
const tongGiaSau = computed(() => (phanTramHopLe.value === null ? null : dsChon.value.reduce((s, v) => s + giaSauGiam(v), 0)))

/* ----- Trùng đợt: các đợt KHÁC (đang bật, chưa kết thúc) + cách gộp khi chồng nhau (cấu hình ở backend) ----- */
const apDungKhac = ref({ chinhSach: 'MAX', muc: [] })
const cheDoChongDot = ref(null) // { cheDo, moTa } từ /dot-giam-gia/che-do (backend cũ chưa có thì dùng chinhSach của /ap-dung)

// Đợt đang soạn (chỉ tính khi đã nhập đủ % và khoảng ngày giờ hợp lệ)
const dotDangSoan = computed(() => {
  if (phanTramHopLe.value === null || !khoangHopLe.value) return null
  return {
    idDot: idXem ?? 0,
    maDot: form.maDot || 'Đợt này',
    tenDot: form.tenDot.trim() || 'Đợt đang soạn',
    phanTram: phanTramHopLe.value,
    ngayBatDau: form.batDau,
    ngayKetThuc: form.ketThuc,
  }
})
const cheDoDangDung = computed(() => cheDoChongDot.value?.cheDo || apDungKhac.value.chinhSach || 'MAX')

/* ----- Tải dữ liệu ----- */
async function taiDuLieu() {
  try {
    const [dsSp, dsBt, dsMau, dsTl, dsCv, dsDm, dsDot, dot, chiTiet] = await Promise.all([
      sanPhamService.getAll(),
      bienTheService.getAll(),
      thuocTinhService.getAll('mau-sac'),
      thuocTinhService.getAll('trong-luong'),
      thuocTinhService.getAll('chu-vi'),
      thuocTinhService.getAll('danh-muc'),
      dotGiamGiaService.getAll(),
      isEdit ? dotGiamGiaService.getById(idXem) : Promise.resolve(null),
      isEdit ? dotGiamGiaService.getBienThe(idXem) : Promise.resolve([]),
    ])
    // Các đợt khác để dựng lịch giảm giá / cảnh báo trùng đợt; lỗi thì thôi, không làm hỏng trang
    // (dữ liệu trả về sai dạng thì coi như không có đợt khác, tránh làm hỏng trang)
    dotGiamGiaService.apDung().then((r) => (apDungKhac.value = r && Array.isArray(r.muc) ? r : { chinhSach: 'MAX', muc: [] })).catch(() => {})
    dotGiamGiaService.cheDo().then((r) => (cheDoChongDot.value = r?.cheDo ? r : null)).catch(() => {})
    sanPhams.value = dsSp
    bienThes.value = dsBt
    mauSacs.value = dsMau
    trongLuongs.value = dsTl
    chuVis.value = dsCv
    danhMucs.value = dsDm
    maDaCo.value = new Set(dsDot.map((d) => String(d.maDot).toUpperCase()))

    if (isEdit) {
      item.value = dot
      form.maDot = dot.maDot
      form.tenDot = dot.tenDot
      form.phanTram = String(dot.phanTramGiamDot)
      form.batDau = veGioPhut(dot.ngayBatDau, '00:00')
      form.ketThuc = veGioPhut(dot.ngayKetThuc, '23:59')
      form.moTa = dot.moTa ?? ''
      daChon.value = new Set(chiTiet.map((c) => c.idSanPhamChiTiet))
      // Nạp dữ liệu cũ xong: lỗi chỉ hiện khi người dùng chạm vào ô (không báo lỗi ngay khi mở trang sửa)
      await nextTick()
      Object.keys(chamVao).forEach((k) => delete chamVao[k])
    } else {
      await lamMoiMa()
    }
  } catch (e) {
    loiTai.value = e.response?.status === 404 ? 'Không tìm thấy đợt giảm giá này.' : e.message || 'Không tải được dữ liệu. Vui lòng thử lại.'
  } finally {
    dangTai.value = false
  }
}
onMounted(taiDuLieu)

/* ----- Lưu ----- */
const hoiXacNhan = ref(false)

function batDauLuu() {
  // Ô bắt đầu còn là giờ mặc định (chưa ai đụng tới): lấy lại "bây giờ" mới nhất để không bị báo quá khứ khi điền form lâu
  if (!isEdit && !chamVao.batDau) form.batDau = bayGioIso()
  submitted.value = true
  bayGio.value = bayGioDayDu()
  const e = validate()
  if (Object.keys(e).length) {
    if (e.bienThe) toast.error(e.bienThe)
    nextTick(() => {
      const o = document.querySelector('.dgg-page .is-invalid, .dgg-page [aria-invalid="true"]')
      o?.scrollIntoView?.({ block: 'center' })
      o?.focus?.()
    })
    return
  }
  hoiXacNhan.value = true
}

async function luu() {
  dangLuu.value = true
  const payload = {
    tenDot: form.tenDot.trim(),
    phanTramGiamDot: phanTramHopLe.value,
    ngayBatDau: ngayGioGuiApi(form.batDau, '00:00'),
    ngayKetThuc: ngayGioGuiApi(form.ketThuc, '23:59'),
    moTa: form.moTa.trim(),
    idBienThe: [...daChon.value],
  }
  try {
    if (isEdit) {
      await dotGiamGiaService.update(idXem, payload)
      toast.success('Đã cập nhật đợt giảm giá thành công.')
    } else {
      await dotGiamGiaService.create({ ...payload, maDot: form.maDot })
      toast.success('Đã tạo mới đợt giảm giá thành công.')
    }
    hoiXacNhan.value = false
    router.push('/dot-giam-gia')
  } catch (e) {
    hoiXacNhan.value = false
    toast.error(e.message || 'Không thể lưu đợt giảm giá. Vui lòng thử lại.')
    // Trùng mã (người khác vừa tạo cùng mã): sinh mã mới để bấm lưu lại
    if (!isEdit && e.response?.status === 409) lamMoiMa()
  } finally {
    dangLuu.value = false
  }
}

const huy = () => router.push('/dot-giam-gia')
</script>

<template>
  <div class="ad-page dgg-page">
    <div class="dgg-topbar">
      <RouterLink to="/dot-giam-gia" class="ad-icon-btn" title="Quay lại danh sách" aria-label="Quay lại danh sách đợt giảm giá">
        <i class="bi bi-arrow-left" aria-hidden="true"></i>
      </RouterLink>
      <span v-if="isEdit && item" class="ad-pill" :class="TRANG_THAI[tinhTrangThai(item, bayGio)].cls" data-testid="dgg-trang-thai-hien-tai">{{ TRANG_THAI[tinhTrangThai(item, bayGio)].label }}</span>
    </div>

    <section v-if="dangTai" class="ad-card"><div class="ad-empty">Đang tải dữ liệu…</div></section>

    <section v-else-if="loiTai" class="ad-card">
      <div class="ad-empty">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>{{ loiTai }}</strong>
        <RouterLink to="/dot-giam-gia" class="ad-btn">Về danh sách đợt giảm giá</RouterLink>
      </div>
    </section>

    <template v-else>
      <div class="dgg-grid">
        <!-- Thẻ trái: thông tin đợt giảm -->
        <section class="ad-card">
          <header class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-tag"></i></span>
            <div>
              <h2 class="ad-card-title">Thông tin đợt giảm</h2>
            </div>
          </header>

          <form class="dgg-form" novalidate @submit.prevent="batDauLuu">
            <div>
              <label class="ad-label" for="dgg-ma">Mã đợt <span class="ad-required">*</span></label>
              <div class="ad-affix">
                <input id="dgg-ma" v-model="form.maDot" type="text" class="form-control ad-control ad-input-code" :class="{ 'has-btn': !isEdit }" readonly />
                <button
                  v-if="!isEdit"
                  type="button"
                  class="ad-affix-btn"
                  title="Tạo mã khác"
                  aria-label="Tạo mã đợt khác"
                  @click="lamMoiMa"
                >
                  <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
                </button>
              </div>
              <p v-if="isEdit" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
            </div>

            <div>
              <label class="ad-label" for="dgg-ten">Tên đợt <span class="ad-required">*</span></label>
              <input
                id="dgg-ten"
                v-model="form.tenDot"
                type="text"
                class="form-control ad-control"
                :class="{ 'is-invalid': errors.tenDot }"
                placeholder="Ví dụ: Yonex Astrox Festival"
                maxlength="255"
                autocomplete="off"
                :aria-invalid="!!errors.tenDot"
                aria-describedby="dgg-ten-msg"
                @input="cham('tenDot')"
                @blur="cham('tenDot')"
              />
              <p v-if="errors.tenDot" id="dgg-ten-msg" class="ad-error">{{ errors.tenDot }}</p>
            </div>

            <div>
              <label class="ad-label" for="dgg-gia-tri">Giá trị giảm (%) <span class="ad-required">*</span></label>
              <div class="ad-affix">
                <input
                  id="dgg-gia-tri"
                  v-model="form.phanTram"
                  type="number"
                  min="1"
                  max="100"
                  step="1"
                  class="form-control ad-control has-suffix"
                  :class="{ 'is-invalid': errors.phanTram }"
                  placeholder="Từ 1 đến 100"
                  inputmode="numeric"
                  :aria-invalid="!!errors.phanTram"
                  aria-describedby="dgg-gia-tri-msg"
                  @blur="cham('phanTram')"
                />
                <span class="ad-suffix" aria-hidden="true">%</span>
              </div>
              <p v-if="errors.phanTram" id="dgg-gia-tri-msg" class="ad-error">{{ errors.phanTram }}</p>
            </div>

            <div>
              <label class="ad-label" for="dgg-tu-ngay">Ngày giờ bắt đầu <span class="ad-required">*</span></label>
              <DateTimeInput id="dgg-tu-ngay" v-model="form.batDau" :invalid="!!errors.batDau" :disabled="khoaBatDau" default-time="00:00" />
              <p v-if="errors.batDau" id="dgg-tu-ngay-msg" class="ad-error">{{ errors.batDau }}</p>
              <p v-else-if="khoaBatDau" class="ad-hint" data-testid="dgg-khoa-bat-dau">Đợt đã bắt đầu nên không đổi được ngày giờ bắt đầu.</p>
            </div>

            <div>
              <label class="ad-label" for="dgg-den-ngay">Ngày giờ kết thúc <span class="ad-required">*</span></label>
              <DateTimeInput id="dgg-den-ngay" v-model="form.ketThuc" :min="form.batDau || undefined" :invalid="!!errors.ketThuc" :show-now="false" default-time="23:59" />
              <p v-if="errors.ketThuc" id="dgg-den-ngay-msg" class="ad-error">{{ errors.ketThuc }}</p>
            </div>

            <div class="dgg-du-kien" data-testid="dgg-trang-thai-du-kien" role="status">
              <span class="dgg-du-kien-nhan">Trạng thái dự kiến:</span>
              <template v-if="trangThaiDuKien">
                <span class="ad-pill" :class="TRANG_THAI[trangThaiDuKien].cls" data-testid="dgg-trang-thai-nhan">{{ TRANG_THAI[trangThaiDuKien].label }}</span>
                <span class="dgg-du-kien-mo-ta">{{ moTaTrangThai }}</span>
              </template>
              <span v-else class="dgg-du-kien-mo-ta">Chọn ngày giờ bắt đầu và kết thúc hợp lệ để xem trạng thái dự kiến.</span>
            </div>
            <p class="ad-hint dgg-quy-uoc-gio">
              Thời gian tính theo phút: bắt đầu từ đầu phút bắt đầu, kết thúc tới hết phút kết thúc (kết thúc 17:00 là còn giảm tới 17:00:59).
            </p>

            <div>
              <label class="ad-label" for="dgg-mo-ta">Mô tả</label>
              <textarea
                id="dgg-mo-ta"
                v-model="form.moTa"
                class="form-control ad-control"
                :class="{ 'is-invalid': errors.moTa }"
                rows="4"
                maxlength="500"
                placeholder="Nhập mô tả..."
                :aria-invalid="!!errors.moTa"
                aria-describedby="dgg-mo-ta-msg"
              ></textarea>
              <p v-if="errors.moTa" id="dgg-mo-ta-msg" class="ad-error">{{ errors.moTa }}</p>
              <p v-else class="ad-hint dgg-dem-ky-tu">{{ form.moTa.length }}/500 ký tự</p>
            </div>

            <div class="dgg-form-actions">
              <button type="submit" class="ad-btn ad-btn-primary" :disabled="dangLuu">
                <i class="bi bi-save" aria-hidden="true"></i> {{ isEdit ? 'Lưu thay đổi' : 'Tạo đợt giảm giá' }}
              </button>
              <button type="button" class="ad-btn" :disabled="dangLuu" @click="huy">Hủy</button>
            </div>
          </form>
        </section>

        <!-- Thẻ phải: chọn sản phẩm áp dụng -->
        <section class="ad-card">
          <header class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-search"></i></span>
            <div>
              <h2 class="ad-card-title">Chọn sản phẩm áp dụng</h2>
              <p class="ad-card-sub">Đã chọn {{ soDaChon }} biến thể</p>
            </div>
          </header>

          <div class="dgg-search">
            <div class="ad-affix">
              <i class="bi bi-search" aria-hidden="true"></i>
              <input
                v-model="loc.tuKhoa"
                type="text"
                class="form-control ad-control"
                placeholder="Tìm sản phẩm theo tên hoặc mã..."
                aria-label="Tìm sản phẩm theo tên hoặc mã"
                autocomplete="off"
              />
            </div>
          </div>

          <div class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table dgg-table">
                <thead>
                  <tr>
                    <th class="dgg-col-check">
                      <input
                        type="checkbox"
                        class="form-check-input dgg-check"
                        :checked="tieuDe.checked"
                        :indeterminate="tieuDe.partial"
                        :disabled="!idsTrang.length"
                        aria-label="Chọn tất cả sản phẩm ở trang này"
                        @change="doiTatCaTrang"
                      />
                    </th>
                    <th class="ad-col-stt">STT</th>
                    <th>Mã SP</th>
                    <th>Tên sản phẩm</th>
                    <th class="dgg-col-expand"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!dongTrang.length">
                    <td colspan="5">
                      <div class="ad-empty">
                        <i class="bi bi-inbox" aria-hidden="true"></i>
                        <strong>Không có sản phẩm phù hợp</strong>
                        <span>Chỉ hiện sản phẩm đang kinh doanh có biến thể đang bán.</span>
                      </div>
                    </td>
                  </tr>
                  <template v-for="(row, i) in dongTrang" :key="row.sp.id">
                    <tr>
                      <td class="dgg-col-check">
                        <input
                          type="checkbox"
                          class="form-check-input dgg-check"
                          :checked="trangThaiDong(row).checked"
                          :indeterminate="trangThaiDong(row).partial"
                          :aria-label="'Chọn mọi biến thể của ' + row.sp.ten"
                          @change="doiSanPham(row)"
                        />
                      </td>
                      <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                      <td><span class="ad-code ad-nowrap">{{ row.sp.ma }}</span></td>
                      <td class="ad-cell-wrap">
                        {{ row.sp.ten }}
                        <span v-if="idsDangChon(row.ids)" class="dgg-count">Đã chọn {{ idsDangChon(row.ids) }}/{{ row.ids.length }} biến thể</span>
                      </td>
                      <td class="dgg-col-expand">
                        <button
                          type="button"
                          class="dgg-expand"
                          :aria-expanded="moRong.has(row.sp.id)"
                          :title="moRong.has(row.sp.id) ? 'Thu gọn biến thể' : 'Chọn từng biến thể'"
                          :aria-label="(moRong.has(row.sp.id) ? 'Thu gọn' : 'Xem') + ' biến thể của ' + row.sp.ten"
                          @click="doiMoRong(row.sp.id)"
                        >
                          <i class="bi" :class="moRong.has(row.sp.id) ? 'bi-dash-lg' : 'bi-plus-lg'" aria-hidden="true"></i>
                        </button>
                      </td>
                    </tr>
                    <tr v-if="moRong.has(row.sp.id)" class="dgg-sub-row">
                      <td colspan="5">
                        <div class="dgg-variants">
                          <button
                            v-for="v in row.variants"
                            :key="v.id"
                            type="button"
                            class="dgg-vchip"
                            :class="{ on: daChon.has(v.id) }"
                            :aria-pressed="daChon.has(v.id)"
                            @click="doiBienThe(v)"
                          >
                            <i v-if="daChon.has(v.id)" class="bi bi-check2" aria-hidden="true"></i>
                            {{ nhanBienThe(v) }}
                            <span class="dgg-vchip-price">{{ formatMoney(v.giaBan) }}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>

          <BasePagination v-if="total" v-model:page="page" :page-size="pageSize" :total="total" />
        </section>
      </div>

      <!-- Tổng kết đợt giảm giá -->
      <section class="ad-card" data-testid="tong-ket">
        <header class="ad-card-head">
          <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-clipboard-data"></i></span>
          <div>
            <h2 class="ad-card-title">Tổng kết đợt giảm giá</h2>
            <p class="ad-card-sub">{{ form.tenDot.trim() || 'Đợt đang soạn' }} · {{ form.maDot }}</p>
          </div>
        </header>
        <div class="dgg-stats">
          <div class="dgg-stat">
            <span class="dgg-stat-label">Mức giảm</span>
            <strong class="dgg-stat-value dgg-sale">{{ phanTramHopLe !== null ? `${phanTramHopLe}%` : '—' }}</strong>
          </div>
          <div class="dgg-stat">
            <span class="dgg-stat-label">Sản phẩm</span>
            <strong class="dgg-stat-value">{{ soSanPhamChon }}</strong>
          </div>
          <div class="dgg-stat">
            <span class="dgg-stat-label">Biến thể</span>
            <strong class="dgg-stat-value">{{ soDaChon }}</strong>
          </div>
          <div class="dgg-stat">
            <span class="dgg-stat-label">Ngày giờ bắt đầu</span>
            <strong class="dgg-stat-value dgg-stat-gio">{{ form.batDau ? dinhDangNgayGio(form.batDau) : '—' }}</strong>
          </div>
          <div class="dgg-stat">
            <span class="dgg-stat-label">Ngày giờ kết thúc</span>
            <strong class="dgg-stat-value dgg-stat-gio">{{ form.ketThuc ? dinhDangNgayGio(form.ketThuc) : '—' }}</strong>
            <span v-if="thoiLuongDot" class="dgg-stat-sub">{{ thoiLuongDot }}</span>
          </div>
        </div>
        <p v-if="soDaChon && tongGiaSau !== null" class="ad-hint mb-0 mt-2">
          Mỗi biến thể 1 sản phẩm: tổng giá bán {{ formatMoney(tongGiaGoc) }} → sau giảm {{ formatMoney(tongGiaSau) }} (giảm {{ formatMoney(tongGiaGoc - tongGiaSau) }}).
        </p>
      </section>

      <!-- Thẻ dưới: sản phẩm & biến thể đã chọn (mỗi dòng một biến thể) -->
      <section class="ad-card" :class="{ 'dgg-card-error': errors.bienThe }">
        <div class="dgg-chosen-head">
          <header class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-check2-square"></i></span>
            <div>
              <h2 class="ad-card-title">Sản phẩm &amp; biến thể đã chọn áp dụng</h2>
              <p class="ad-card-sub">{{ soSanPhamChon }} sản phẩm · {{ soDaChon }} biến thể</p>
            </div>
          </header>
          <div v-if="soDaChon" class="dgg-chosen-filters" data-testid="loc-da-chon">
            <Select2 id="dgg-loc-mau" v-model="locChon.mau" :options="dsLocMau" aria-label="Lọc biến thể đã chọn theo màu sắc" class="ad-control" />
            <Select2 v-if="locTl.length" id="dgg-loc-tl" v-model="locChon.tl" :options="dsLocTl" aria-label="Lọc theo trọng lượng" class="ad-control" />
            <Select2 v-if="locCv.length" id="dgg-loc-cv" v-model="locChon.cv" :options="dsLocCv" aria-label="Lọc theo chu vi cán" class="ad-control" />
            <button v-if="dangLocChon" type="button" class="ad-btn" @click="datLaiLocChon"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại</button>
            <button v-if="dangLocChon && dsChonLoc.length" type="button" class="ad-btn dgg-btn-go" @click="goDangLoc">
              <i class="bi bi-x-circle" aria-hidden="true"></i> Gỡ {{ dsChonLoc.length }} biến thể đang lọc
            </button>
          </div>
        </div>
        <p v-if="errors.bienThe" class="ad-error">{{ errors.bienThe }}</p>

        <div class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table dgg-chosen-table">
              <thead>
                <tr>
                  <th class="ad-col-stt">STT</th>
                  <th class="text-center">Ảnh</th>
                  <th>Sản phẩm</th>
                  <th>Biến thể</th>
                  <th>Giá bán</th>
                  <th>Giá sau giảm</th>
                  <th class="ad-col-actions">Xóa</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!dongChon.length">
                  <td colspan="7">
                    <div class="ad-empty">
                      <i class="bi bi-inbox" aria-hidden="true"></i>
                      <strong>{{ soDaChon ? 'Không có biến thể phù hợp bộ lọc' : 'Chưa chọn sản phẩm nào' }}</strong>
                      <span v-if="!soDaChon">Tích chọn sản phẩm ở khung “Chọn sản phẩm áp dụng” phía trên.</span>
                    </div>
                  </td>
                </tr>
                <tr v-for="(v, i) in dongChon" :key="v.id" class="dgg-chosen-row">
                  <td class="ad-col-stt">{{ (pageChon - 1) * pageSizeChon + i + 1 }}</td>
                  <td class="text-center">
                    <img v-if="anhBienThe(v)" :src="anhBienThe(v)" :alt="v.sp?.ten" class="dgg-thumb" />
                    <span v-else class="dgg-thumb dgg-thumb-empty" aria-hidden="true"><i class="bi bi-image"></i></span>
                  </td>
                  <td>
                    <div class="dgg-product-name">{{ v.sp?.ten }}</div>
                    <div class="dgg-muted">{{ v.sp?.ma }}</div>
                  </td>
                  <td>
                    <div class="ad-nowrap"><span class="dgg-dot" :style="{ background: mauHex(v.mau) }" aria-hidden="true"></span>{{ nhanBienThe(v) }}</div>
                    <div class="dgg-muted ad-code">{{ v.ma }}</div>
                  </td>
                  <td class="ad-nowrap">{{ formatMoney(v.giaBan) }}</td>
                  <td class="ad-nowrap">
                    <strong v-if="giaSauGiam(v) !== null" class="dgg-sale">{{ formatMoney(giaSauGiam(v)) }}</strong>
                    <span v-else class="dgg-muted" title="Nhập giá trị giảm (%) để tính">—</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn is-danger"
                        title="Xóa biến thể này khỏi đợt giảm giá"
                        :aria-label="'Xóa biến thể ' + v.ma"
                        @click="boKhoiChon([v.id])"
                      >
                        <i class="bi bi-trash" aria-hidden="true"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <BasePagination v-if="totalChon" v-model:page="pageChon" :page-size="pageSizeChon" :total="totalChon" />
      </section>

      <!-- Quy tắc + ví dụ chạy thử + cảnh báo trùng đợt (cộng dồn hay lấy mức cao nhất) -->
      <DotGiamGiaChongDotCard
        :dot="dotDangSoan"
        :bien-thes="dsChon"
        :muc-khac="apDungKhac.muc"
        :che-do="cheDoDangDung"
        :mo-ta-che-do="cheDoChongDot?.moTa ?? ''"
      />

      <!-- Lịch giảm giá theo ngày: chỉ khi xem / sửa đợt đã lưu (cần dữ liệu từ backend) -->
      <DotGiamGiaLichCard v-if="isEdit && item" :id-dot="idXem" :dot-dang-tat="!dangHoatDong(item)" />
    </template>

    <ConfirmQuestion
      v-if="hoiXacNhan"
      :message="isEdit ? 'Bạn có chắc chắn muốn cập nhật đợt giảm giá này không?' : 'Bạn có chắc chắn muốn thêm mới đợt giảm giá này không?'"
      :loading="dangLuu"
      @confirm="luu"
      @cancel="hoiXacNhan = false"
    />
  </div>
</template>

<style scoped>
.dgg-page {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}
.dgg-topbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* Hai thẻ trên: thông tin (trái) + chọn sản phẩm (phải) */
.dgg-grid {
  display: grid;
  grid-template-columns: minmax(340px, 420px) minmax(0, 1fr);
  gap: 1.15rem;
  align-items: start;
}
@media (max-width: 991.98px) {
  .dgg-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.dgg-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
/* Trạng thái dự kiến (xem trước ngay khi chọn ngày giờ) */
.dgg-du-kien {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.6rem;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--fs-line);
  border-radius: 10px;
  background: var(--fs-surface, #f5f8fc);
  font-size: 0.875rem;
}
.dgg-du-kien-nhan {
  font-weight: 600;
  color: var(--fs-text);
}
.dgg-du-kien-mo-ta {
  color: var(--fs-text-2);
}
.dgg-quy-uoc-gio {
  margin: -0.35rem 0 0;
}
.dgg-dem-ky-tu {
  text-align: right;
  margin-bottom: 0;
}
.dgg-form-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.4rem;
}
.dgg-form-actions .ad-btn {
  width: 100%;
  justify-content: center;
}
textarea.ad-control {
  resize: vertical;
}

/* Ô tìm kiếm + bộ lọc + nút tìm */
.dgg-search {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.dgg-search > div {
  flex: 1 1 260px;
  min-width: 0;
}

/* Bảng chọn sản phẩm */
.dgg-check {
  accent-color: var(--fs-primary);
  cursor: pointer;
  width: 1.05rem;
  height: 1.05rem;
}
.dgg-check:checked,
.dgg-check:indeterminate {
  background-color: var(--fs-primary);
  border-color: var(--fs-primary);
}
.dgg-col-check {
  width: 44px;
}
.dgg-col-expand {
  width: 52px;
  text-align: center;
}
.dgg-expand {
  width: 30px;
  height: 30px;
  display: inline-grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--fs-muted);
}
.dgg-expand:hover {
  background: var(--fs-primary-soft);
  color: var(--fs-primary);
}
.dgg-sub-row > td {
  background: var(--fs-surface);
  padding-top: 0.4rem !important;
  padding-bottom: 0.6rem !important;
}

/* Nhãn nhỏ (màu sắc / trọng lượng / chu vi) */
.dgg-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.3rem;
}
.dgg-chip {
  display: inline-block;
  padding: 0.12rem 0.6rem;
  border-radius: 999px;
  background: #f1f4f8;
  color: var(--fs-text-2);
  font-size: 0.78rem;
  font-weight: 500;
}

/* Thẻ biến thể đã chọn */
.dgg-chosen-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}
.dgg-count { display: inline-block; margin-left: 0.5rem; padding: 0.05rem 0.5rem; border-radius: 999px; background: var(--fs-primary-soft, #e6f1fe); color: var(--fs-primary-dark, #075fc0); font-size: 0.75rem; font-weight: 600; }
.dgg-variants { display: flex; flex-wrap: wrap; gap: 0.4rem; }
.dgg-vchip { display: inline-flex; align-items: center; gap: 0.35rem; border: 1px solid #d5e0ef; background: #fff; border-radius: 999px; padding: 0.25rem 0.75rem; font-size: 0.82rem; color: #1c2530; }
.dgg-vchip:hover { border-color: var(--fs-primary, #0977ec); }
.dgg-vchip.on { background: var(--fs-primary-soft, #e6f1fe); border-color: var(--fs-primary, #0977ec); color: var(--fs-primary-dark, #075fc0); font-weight: 600; }
.dgg-vchip-price { font-weight: 400; color: #7b8794; }
.dgg-card-error {
  border-color: var(--fs-danger);
}
/* Tổng kết đợt giảm giá */
.dgg-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.75rem;
}
@media (max-width: 991.98px) {
  .dgg-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
.dgg-stat {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--fs-line-soft, #eef1f5);
  border-radius: 12px;
  background: var(--fs-surface, #f5f8fc);
}
.dgg-stat-label { font-size: 0.78rem; color: var(--fs-muted); }
.dgg-stat-value { font-size: 1.15rem; color: var(--fs-text); }
.dgg-stat-sub { font-size: 0.75rem; color: var(--fs-muted); }
.dgg-stat-gio { font-size: 1rem; }
/* Bảng biến thể đã chọn */
.dgg-chosen-filters { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
.dgg-chosen-filters .form-select { width: auto; min-width: 150px; }
.dgg-btn-go { color: var(--fs-danger); border-color: #f3c2c8; }
.dgg-dot { display: inline-block; width: 12px; height: 12px; margin-right: 0.4rem; border-radius: 50%; border: 1px solid rgba(15, 23, 42, 0.18); vertical-align: -1px; }
.dgg-product {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.dgg-thumb {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  flex: 0 0 auto;
  border: 1px solid var(--fs-line);
}
.dgg-thumb-empty {
  display: inline-grid;
  place-items: center;
  background: var(--fs-surface);
  color: var(--fs-muted);
}
.dgg-product-name {
  font-weight: 500;
  color: var(--fs-text);
}
.dgg-muted {
  color: var(--fs-muted);
  font-size: 0.85rem;
}
.dgg-variant-code {
  font-size: 0.88rem;
}
.dgg-sale {
  color: var(--fs-primary-dark);
}
</style>
