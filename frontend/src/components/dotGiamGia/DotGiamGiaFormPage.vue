<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BasePagination from '../common/BasePagination.vue'
import DotGiamGiaLichCard from './DotGiamGiaLichCard.vue'
import ConfirmQuestion from '../common/ConfirmQuestion.vue'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { bienTheService, sanPhamService } from '../../services/sanPhamService'
import { thuocTinhService } from '../../services/thuocTinhService'
import { usePagination } from '../../composables/usePagination'
import { useToast } from '../../composables/useToast'
import { TRANG_THAI, dangHoatDong, tinhTrangThai } from '../../constants/dotGiamGia'
import { formatDate, formatMoney, todayIso } from '../../utils/format'
import { formatPhanTram, lichGiamGia, nhomTheoBienThe } from '../../utils/giamGia'
import { includesText, normalizeText } from '../../utils/text'
import { laKhongApDung } from '../../utils/danhMuc'

/*
 * Trang THÊM đợt giảm giá (/dot-giam-gia/them) và CHI TIẾT / SỬA đợt giảm giá (/dot-giam-gia/:id), giống video:
 *  - Thẻ trái "Thông tin đợt giảm": mã đợt (tự sinh, bấm nút xoay để đổi), tên, giá trị giảm (%), từ ngày, đến ngày, mô tả.
 *  - Thẻ phải "Chọn sản phẩm áp dụng": tìm theo tên / mã, lọc màu sắc + trọng lượng, tích chọn sản phẩm (chọn hết biến thể)
 *    hoặc bấm "+" để chọn từng biến thể.
 *  - Thẻ dưới "Sản phẩm & biến thể đã chọn áp dụng": bảng biến thể đã chọn kèm giá bán và giá sau giảm.
 *  - Bấm "Tạo đợt giảm giá" thì hỏi xác nhận rồi mới lưu; lưu xong quay về danh sách.
 *  - Chỉ sản phẩm đang kinh doanh, biến thể đang bán mới chọn được (danh mục "giày" của CSDL cũ bị ẩn vì cửa hàng chỉ bán vợt).
 *
 * QUY ĐỊNH KHI SỬA ĐỢT GIẢM GIÁ
 *  - Được sửa: tên, giá trị giảm, từ ngày, đến ngày, mô tả, danh sách biến thể áp dụng.
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
// Mã dự phòng sinh ngay trên trình duyệt (DGG + số thứ tự, vd DGG009, không trùng mã đã có) khi chưa hỏi được backend
function taoMaDot() {
  let max = 0
  for (const m of maDaCo.value) {
    const r = /^DGG(\d{1,6})$/.exec(m)
    if (r) max = Math.max(max, Number(r[1]))
  }
  let ma
  do {
    max += 1
    ma = 'DGG' + String(max).padStart(3, '0')
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
  tuNgay: todayIso(),
  denNgay: '',
  moTa: '',
})
const errors = reactive({ tenDot: '', phanTram: '', tuNgay: '', denNgay: '', bienThe: '' })
const xoaLoi = () => Object.keys(errors).forEach((k) => (errors[k] = ''))
// Người dùng sửa lại ô nào thì xóa thông báo lỗi của ô đó
watch(() => form.tenDot, () => (errors.tenDot = ''))
watch(() => form.phanTram, () => (errors.phanTram = ''))
watch(() => form.tuNgay, () => (errors.tuNgay = ''))
watch(() => form.denNgay, () => (errors.denNgay = ''))

/* ----- Biến thể đã chọn ----- */
const daChon = ref(new Set()) // id biến thể
const soDaChon = computed(() => daChon.value.size)
watch(soDaChon, (n) => {
  if (n) errors.bienThe = ''
})

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
const bo = reactive({ tuKhoa: '', mau: '', tl: '', cv: '' }) // giá trị đang nhập
const loc = ref({ tuKhoa: '', mau: '', tl: '', cv: '' }) // giá trị đang áp dụng (bấm Tìm kiếm mới đổi)
function timKiem() {
  loc.value = { tuKhoa: bo.tuKhoa.trim(), mau: bo.mau, tl: bo.tl, cv: bo.cv }
}

const mauChon = computed(() => [...new Set(chonDuoc.value.map((v) => v.idMauSac))].map((id) => ({ id, ten: mauMap.value.get(id) ?? '' })))
const tlChon = computed(() => [...new Set(chonDuoc.value.map((v) => v.idTrongLuong))].map((id) => ({ id, ten: tlMap.value.get(id) ?? '' })).filter((x) => x.ten !== '—'))
const cvChon = computed(() => [...new Set(chonDuoc.value.map((v) => v.idChuVi))].map((id) => ({ id, ten: cvMap.value.get(id) ?? '' })).filter((x) => x.ten !== '—'))

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
watch(loc, () => {
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
const locChon = reactive({ tuKhoa: '', mau: '', tl: '', cv: '' })
const dsChon = computed(() => {
  const ds = [...daChon.value].map((id) => bienTheById.value.get(id)).filter(Boolean)
  ds.sort((a, b) => (a.sp?.ten ?? '').localeCompare(b.sp?.ten ?? '', 'vi') || String(a.ma).localeCompare(String(b.ma)))
  return ds
})
const mauTrongChon = computed(() => [...new Set(dsChon.value.map((v) => v.idMauSac))].map((id) => ({ id, ten: mauMap.value.get(id) ?? '' })))
const tlTrongChon = computed(() => [...new Set(dsChon.value.map((v) => v.idTrongLuong))].map((id) => ({ id, ten: tlMap.value.get(id) ?? '' })).filter((x) => x.ten !== '—'))
const cvTrongChon = computed(() => [...new Set(dsChon.value.map((v) => v.idChuVi))].map((id) => ({ id, ten: cvMap.value.get(id) ?? '' })).filter((x) => x.ten !== '—'))
const dsChonLoc = computed(() =>
  dsChon.value.filter(
    (v) =>
      (!locChon.mau || String(v.idMauSac) === locChon.mau) &&
      (!locChon.tl || String(v.idTrongLuong) === locChon.tl) &&
      (!locChon.cv || String(v.idChuVi) === locChon.cv) &&
      (!locChon.tuKhoa || includesText(`${v.sp?.ma ?? ''} ${v.sp?.ten ?? ''} ${v.ma}`, locChon.tuKhoa)),
  ),
)
const { page: pageChon, pageSize: pageSizeChon, total: totalChon, items: dongChon } = usePagination(dsChonLoc, 10)
watch(locChon, () => {
  pageChon.value = 1
})

// Giá sau giảm theo % đang nhập (chưa nhập đúng thì chưa tính)
const phanTramHopLe = computed(() => {
  const n = Number(form.phanTram)
  return form.phanTram !== '' && Number.isInteger(n) && n >= 1 && n <= 100 ? n : null
})
const giaSauGiam = (v) => (phanTramHopLe.value === null ? null : Math.round((v.giaBan * (100 - phanTramHopLe.value)) / 100))

/* ----- Lịch giảm giá khi đợt chồng nhau (xem trước theo dữ liệu đang nhập) -----
 * Biến thể có thể nằm trong nhiều đợt cùng lúc. Ví dụ đợt A 10% (1/10-10/10) và đợt B 15% (4/10-20/10) cùng có SPCT1:
 *   1/10-3/10: 10% | 4/10-10/10: chồng nhau -> cao nhất 15% hoặc trung bình cộng 12,5% | 11/10-20/10: 15%.
 * Cách tính thật do backend quy định (app.giam-gia.che-do-chong-dot), ở đây hiện cả hai con số và đánh dấu số đang áp dụng. */
const apDungKhac = ref({ chinhSach: 'MAX', muc: [] }) // các đợt KHÁC (đang bật, chưa kết thúc)
const hienTatCaLich = ref(false)
const mucKhacTheoBt = computed(() => nhomTheoBienThe(apDungKhac.value.muc.filter((m) => m.idDot !== idXem)))

// Đợt đang soạn (chỉ tính khi đã nhập đủ % và khoảng ngày hợp lệ)
const dotDangSoan = computed(() => {
  if (phanTramHopLe.value === null || !form.tuNgay || !form.denNgay || form.denNgay < form.tuNgay) return null
  return {
    idDot: idXem ?? 0,
    maDot: form.maDot || 'Đợt này',
    tenDot: form.tenDot || 'Đợt đang soạn',
    phanTram: phanTramHopLe.value,
    ngayBatDau: form.tuNgay,
    ngayKetThuc: form.denNgay,
  }
})

// Chỉ liệt kê biến thể đã chọn mà có khoảng ngày chồng với đợt khác
const lichChongNhau = computed(() => {
  const dot = dotDangSoan.value
  if (!dot) return []
  const ketQua = []
  for (const v of dsChon.value) {
    const khoang = lichGiamGia([...(mucKhacTheoBt.value.get(v.id) ?? []), dot], apDungKhac.value.chinhSach)
    if (khoang.some((k) => k.chongNhau)) ketQua.push({ v, khoang })
  }
  return ketQua
})
const lichHienThi = computed(() => (hienTatCaLich.value ? lichChongNhau.value : lichChongNhau.value.slice(0, 6)))
const tenChinhSach = computed(() => (apDungKhac.value.chinhSach === 'TRUNG_BINH' ? 'trung bình cộng' : 'mức cao nhất'))

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
    // Các đợt khác để dựng lịch giảm giá; lỗi thì thôi, không làm hỏng trang
    dotGiamGiaService.apDung().then((r) => (apDungKhac.value = r)).catch(() => {})
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
      form.tuNgay = String(dot.ngayBatDau).substring(0, 10)
      form.denNgay = String(dot.ngayKetThuc).substring(0, 10)
      form.moTa = dot.moTa ?? ''
      daChon.value = new Set(chiTiet.map((c) => c.idSanPhamChiTiet))
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

function kiemTra() {
  xoaLoi()
  let ok = true
  if (!form.tenDot.trim()) {
    errors.tenDot = 'Vui lòng nhập tên đợt giảm giá.'
    ok = false
  } else if (form.tenDot.trim().length > 255) {
    errors.tenDot = 'Tên đợt giảm giá tối đa 255 ký tự.'
    ok = false
  }
  if (form.phanTram === '' || form.phanTram === null) {
    errors.phanTram = 'Vui lòng nhập giá trị giảm.'
    ok = false
  } else if (phanTramHopLe.value === null) {
    errors.phanTram = 'Giá trị giảm phải là số nguyên từ 1 đến 100.'
    ok = false
  }
  if (!form.tuNgay) {
    errors.tuNgay = 'Vui lòng chọn từ ngày.'
    ok = false
  }
  if (!form.denNgay) {
    errors.denNgay = 'Vui lòng chọn đến ngày.'
    ok = false
  } else if (form.tuNgay && form.denNgay < form.tuNgay) {
    errors.denNgay = 'Đến ngày phải sau hoặc bằng từ ngày.'
    ok = false
  }
  if (form.moTa.length > 500) {
    ok = false
    toast.error('Mô tả tối đa 500 ký tự.')
  }
  if (!soDaChon.value) {
    errors.bienThe = 'Vui lòng chọn ít nhất một sản phẩm / biến thể áp dụng.'
    toast.error(errors.bienThe)
    ok = false
  }
  return ok
}

function batDauLuu() {
  if (kiemTra()) hoiXacNhan.value = true
}

async function luu() {
  dangLuu.value = true
  const payload = {
    tenDot: form.tenDot.trim(),
    phanTramGiamDot: phanTramHopLe.value,
    ngayBatDau: `${form.tuNgay}T00:00:00`,
    ngayKetThuc: `${form.denNgay}T23:59:59`,
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
      <span v-if="isEdit && item" class="ad-pill" :class="TRANG_THAI[tinhTrangThai(item)].cls">{{ TRANG_THAI[tinhTrangThai(item)].label }}</span>
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
              />
              <p v-if="errors.tenDot" class="ad-error">{{ errors.tenDot }}</p>
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
                  placeholder="0"
                  inputmode="numeric"
                />
                <span class="ad-suffix" aria-hidden="true">%</span>
              </div>
              <p v-if="errors.phanTram" class="ad-error">{{ errors.phanTram }}</p>
            </div>

            <div class="dgg-two">
              <div>
                <label class="ad-label" for="dgg-tu-ngay">Từ ngày <span class="ad-required">*</span></label>
                <input id="dgg-tu-ngay" v-model="form.tuNgay" type="date" min="2000-01-01" max="2099-12-31" class="form-control ad-control" :class="{ 'is-invalid': errors.tuNgay }" />
                <p v-if="errors.tuNgay" class="ad-error">{{ errors.tuNgay }}</p>
              </div>
              <div>
                <label class="ad-label" for="dgg-den-ngay">Đến ngày <span class="ad-required">*</span></label>
                <input
                  id="dgg-den-ngay"
                  v-model="form.denNgay"
                  type="date"
                  :min="form.tuNgay || '2000-01-01'"
                  max="2099-12-31"
                  class="form-control ad-control"
                  :class="{ 'is-invalid': errors.denNgay }"
                />
                <p v-if="errors.denNgay" class="ad-error">{{ errors.denNgay }}</p>
              </div>
            </div>

            <div>
              <label class="ad-label" for="dgg-mo-ta">Mô tả</label>
              <textarea id="dgg-mo-ta" v-model="form.moTa" class="form-control ad-control" rows="4" maxlength="500" placeholder="Nhập mô tả..."></textarea>
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

          <div class="dgg-search" @keyup.enter="timKiem">
            <div class="dgg-search-text">
              <div class="ad-affix">
                <i class="bi bi-search" aria-hidden="true"></i>
                <input
                  v-model="bo.tuKhoa"
                  type="text"
                  class="form-control ad-control"
                  placeholder="Tìm theo tên hoặc mã sản phẩm..."
                  aria-label="Tìm theo tên hoặc mã sản phẩm"
                  autocomplete="off"
                />
              </div>
            </div>
            <div>
              <label class="ad-label" for="dgg-loc-mau">Màu sắc</label>
              <select id="dgg-loc-mau" v-model="bo.mau" class="form-select ad-control">
                <option value="">Tất cả màu sắc</option>
                <option v-for="m in mauChon" :key="m.id" :value="String(m.id)">{{ m.ten }}</option>
              </select>
            </div>
            <div>
              <label class="ad-label" for="dgg-loc-tl">Trọng lượng</label>
              <select id="dgg-loc-tl" v-model="bo.tl" class="form-select ad-control">
                <option value="">Tất cả trọng lượng</option>
                <option v-for="t in tlChon" :key="t.id" :value="String(t.id)">{{ t.ten }}</option>
              </select>
            </div>
            <div>
              <label class="ad-label" for="dgg-loc-cv">Chu vi cán</label>
              <select id="dgg-loc-cv" v-model="bo.cv" class="form-select ad-control">
                <option value="">Tất cả chu vi</option>
                <option v-for="c in cvChon" :key="c.id" :value="String(c.id)">{{ c.ten }}</option>
              </select>
            </div>
            <button type="button" class="ad-btn ad-btn-primary" @click="timKiem">
              <i class="bi bi-search" aria-hidden="true"></i> Tìm kiếm
            </button>
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
                      <td class="ad-cell-wrap">{{ row.sp.ten }}</td>
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
                        <ul class="dgg-variants">
                          <li v-for="v in row.variants" :key="v.id">
                            <label class="dgg-variant">
                              <input type="checkbox" class="form-check-input dgg-check" :checked="daChon.has(v.id)" @change="doiBienThe(v)" />
                              <span class="ad-code ad-nowrap">{{ v.ma }}</span>
                              <span class="dgg-chip">{{ v.mau }}</span>
                              <span v-if="v.tl !== '—'" class="dgg-chip">{{ v.tl }}</span>
                              <span v-if="v.cv !== '—'" class="dgg-chip">{{ v.cv }}</span>
                              <span class="dgg-variant-price">{{ formatMoney(v.giaBan) }}</span>
                            </label>
                          </li>
                        </ul>
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

      <!-- Lịch giảm giá khi biến thể nằm trong nhiều đợt chồng nhau -->
      <section v-if="soDaChon && dotDangSoan" class="ad-card dgg-lich">
        <header class="ad-card-head">
          <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-calendar-range"></i></span>
          <div>
            <h2 class="ad-card-title">Lịch giảm giá khi đợt chồng nhau</h2>
            <p v-if="lichChongNhau.length" class="ad-card-sub">
              {{ lichChongNhau.length }} biến thể đã chọn đang nằm trong đợt giảm giá khác cùng thời gian. Hệ thống áp dụng
              <strong>{{ tenChinhSach }}</strong> cho các ngày chồng nhau.
            </p>
            <p v-else class="ad-card-sub">Không có biến thể nào trùng thời gian với đợt giảm giá khác, mỗi biến thể chỉ áp dụng mức giảm của đợt này.</p>
          </div>
        </header>

        <div v-if="lichChongNhau.length" class="dgg-lich-list">
          <div v-for="it in lichHienThi" :key="it.v.id" class="dgg-lich-item">
            <div class="dgg-lich-head">
              <strong>{{ it.v.sp?.ten }}</strong>
              <span class="dgg-muted">{{ [it.v.ma, it.v.mau, it.v.tl, it.v.cv].filter((x) => x && x !== '—').join(' · ') }}</span>
            </div>
            <ul class="dgg-lich-rows">
              <li v-for="k in it.khoang" :key="k.tuNgay" :class="{ 'is-overlap': k.chongNhau }">
                <span class="dgg-lich-range">{{ formatDate(k.tuNgay) }} → {{ formatDate(k.denNgay) }}</span>
                <span class="dgg-lich-dots">{{ k.cacDot.map((d) => `${d.maDot} (${d.phanTram}%)`).join(' + ') }}</span>
                <span v-if="k.chongNhau" class="dgg-lich-vals">
                  <span class="dgg-lich-val" :class="{ on: apDungKhac.chinhSach === 'MAX' }">Cao nhất {{ formatPhanTram(k.caoNhat) }}</span>
                  <span class="dgg-lich-val" :class="{ on: apDungKhac.chinhSach === 'TRUNG_BINH' }">Trung bình {{ formatPhanTram(k.trungBinh) }}</span>
                </span>
                <span v-else class="dgg-lich-vals"><span class="dgg-lich-val on">{{ formatPhanTram(k.apDung) }}</span></span>
              </li>
            </ul>
          </div>
          <button v-if="lichChongNhau.length > 6" type="button" class="ad-btn dgg-lich-more" @click="hienTatCaLich = !hienTatCaLich">
            {{ hienTatCaLich ? 'Thu gọn' : `Xem thêm ${lichChongNhau.length - 6} biến thể` }}
          </button>
          <p class="ad-hint mb-0">Ô màu xanh là mức đang được áp dụng. Đổi cách tính trong <code>application.properties</code> (app.giam-gia.che-do-chong-dot = MAX hoặc TRUNG_BINH).</p>
        </div>
      </section>

      <!-- Thẻ dưới: sản phẩm & biến thể đã chọn -->
      <section class="ad-card" :class="{ 'dgg-card-error': errors.bienThe }">
        <div class="dgg-chosen-head">
          <header class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-check2-square"></i></span>
            <div>
              <h2 class="ad-card-title">Sản phẩm &amp; biến thể đã chọn áp dụng</h2>
              <p class="ad-card-sub">Danh sách chi tiết gồm {{ soDaChon }} biến thể đã chọn</p>
            </div>
          </header>
          <div class="dgg-chosen-filters">
            <div class="dgg-chosen-key">
              <label class="ad-label" for="dgg-chon-key">Tìm kiếm</label>
              <input
                id="dgg-chon-key"
                v-model="locChon.tuKhoa"
                type="text"
                class="form-control ad-control"
                placeholder="Tên, mã sản phẩm / biến thể..."
                autocomplete="off"
              />
            </div>
            <div>
              <label class="ad-label" for="dgg-chon-mau">Màu sắc</label>
              <select id="dgg-chon-mau" v-model="locChon.mau" class="form-select ad-control">
                <option value="">Tất cả màu sắc</option>
                <option v-for="m in mauTrongChon" :key="m.id" :value="String(m.id)">{{ m.ten }}</option>
              </select>
            </div>
            <div>
              <label class="ad-label" for="dgg-chon-tl">Trọng lượng</label>
              <select id="dgg-chon-tl" v-model="locChon.tl" class="form-select ad-control">
                <option value="">Tất cả trọng lượng</option>
                <option v-for="t in tlTrongChon" :key="t.id" :value="String(t.id)">{{ t.ten }}</option>
              </select>
            </div>
            <div>
              <label class="ad-label" for="dgg-chon-cv">Chu vi cán</label>
              <select id="dgg-chon-cv" v-model="locChon.cv" class="form-select ad-control">
                <option value="">Tất cả chu vi</option>
                <option v-for="c in cvTrongChon" :key="c.id" :value="String(c.id)">{{ c.ten }}</option>
              </select>
            </div>
          </div>
        </div>
        <p v-if="errors.bienThe" class="ad-error">{{ errors.bienThe }}</p>

        <div class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table">
              <thead>
                <tr>
                  <th class="ad-col-stt">STT</th>
                  <th>Sản phẩm</th>
                  <th>Biến thể</th>
                  <th>Giá bán</th>
                  <th>Giá sau giảm</th>
                  <th class="ad-col-actions">Gỡ</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!dongChon.length">
                  <td colspan="6">
                    <div class="ad-empty">
                      <i class="bi bi-inbox" aria-hidden="true"></i>
                      <strong>{{ soDaChon ? 'Không có biến thể khớp bộ lọc' : 'Chưa chọn biến thể nào' }}</strong>
                      <span v-if="!soDaChon">Tích chọn sản phẩm ở khung “Chọn sản phẩm áp dụng” phía trên.</span>
                    </div>
                  </td>
                </tr>
                <tr v-for="(v, i) in dongChon" :key="v.id">
                  <td class="ad-col-stt">{{ (pageChon - 1) * pageSizeChon + i + 1 }}</td>
                  <td>
                    <div class="dgg-product">
                      <img v-if="v.sp?.anhChinh" :src="v.sp.anhChinh" :alt="v.sp.ten" class="dgg-thumb" />
                      <span v-else class="dgg-thumb dgg-thumb-empty" aria-hidden="true"><i class="bi bi-image"></i></span>
                      <div>
                        <div class="dgg-product-name">{{ v.sp?.ten }}</div>
                        <div class="dgg-muted">{{ v.sp?.ma }}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="dgg-muted dgg-variant-code">{{ v.ma }}</div>
                    <div class="dgg-chips">
                      <span class="dgg-chip">{{ v.mau }}</span>
                      <span v-if="v.tl !== '—'" class="dgg-chip">{{ v.tl }}</span>
                      <span v-if="v.cv !== '—'" class="dgg-chip">{{ v.cv }}</span>
                    </div>
                  </td>
                  <td class="ad-nowrap">{{ formatMoney(v.giaBan) }}</td>
                  <td class="ad-nowrap">
                    <strong v-if="giaSauGiam(v) !== null" class="dgg-sale">{{ formatMoney(giaSauGiam(v)) }}</strong>
                    <span v-else class="dgg-muted">—</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn is-danger"
                        title="Gỡ khỏi đợt giảm giá"
                        :aria-label="'Gỡ biến thể ' + v.ma"
                        @click="boKhoiChon([v.id])"
                      >
                        <i class="bi bi-x-lg" aria-hidden="true"></i>
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
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
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
.dgg-two {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0.75rem;
}
.dgg-two > div,
.dgg-two input {
  min-width: 0;
  width: 100%;
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
.dgg-search-text {
  flex: 1 1 220px;
  min-width: 0;
}
.dgg-search > div:not(.dgg-search-text) {
  flex: 0 1 200px;
}
.dgg-search .ad-btn {
  flex: 0 0 auto;
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
.dgg-variants {
  list-style: none;
  margin: 0;
  padding: 0 0 0 2.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.dgg-variant {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.55rem;
  cursor: pointer;
  font-size: 0.88rem;
}
.dgg-variant-price {
  margin-left: auto;
  color: var(--fs-text-2);
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
.dgg-chosen-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1.1rem;
}
.dgg-chosen-filters > div {
  width: 200px;
}
.dgg-chosen-filters > .dgg-chosen-key {
  width: 260px;
}
.dgg-card-error {
  border-color: var(--fs-danger);
}
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
.dgg-lich-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.dgg-lich-item {
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  padding: 0.75rem 1rem;
}
.dgg-lich-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.25rem 0.75rem;
  margin-bottom: 0.5rem;
}
.dgg-lich-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.dgg-lich-rows li {
  display: grid;
  grid-template-columns: 14rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.25rem 1rem;
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  font-size: 0.875rem;
}
.dgg-lich-rows li.is-overlap {
  background: #fff7e6;
}
.dgg-lich-range {
  font-weight: 600;
  white-space: nowrap;
}
.dgg-lich-dots {
  color: var(--fs-text-2);
  overflow-wrap: anywhere;
}
.dgg-lich-vals {
  display: inline-flex;
  gap: 0.4rem;
  justify-content: flex-end;
}
.dgg-lich-val {
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  background: #eef1f5;
  color: var(--fs-muted);
  font-size: 0.8rem;
  white-space: nowrap;
}
.dgg-lich-val.on {
  background: var(--fs-success, #198754);
  color: #fff;
  font-weight: 600;
}
.dgg-lich-more {
  align-self: flex-start;
}
@media (max-width: 767.98px) {
  .dgg-lich-rows li {
    grid-template-columns: minmax(0, 1fr);
  }
  .dgg-lich-vals {
    justify-content: flex-start;
  }
}
</style>
