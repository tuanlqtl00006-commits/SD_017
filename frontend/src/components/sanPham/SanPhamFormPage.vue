<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { taoMa } from '../../constants/thuocTinh'
import { layLuaChon } from '../../utils/text'
import { sanPhamService } from '../../services/sanPhamService'
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
  { field: 'idDoCung', slug: 'do-cung', label: 'Độ cứng' },
  { field: 'idDiemCanBang', slug: 'diem-can-bang', label: 'Điểm cân bằng' },
]

const TOI_DA_ANH = 10 // giống backend (gồm cả ảnh chính)
const TOI_DA_MB = 5 // giống backend
const KIEU_ANH = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const thuocTinh = reactive({})
for (const s of SELECTS) thuocTinh[s.slug] = []
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

onMounted(async () => {
  try {
    const cacLoai = SELECTS.map((s) => s.slug)
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

  // 6 ô chọn: bắt buộc phải chọn (giá trị '' nghĩa là chưa chọn)
  for (const s of SELECTS) {
    if (form[s.field] === '') {
      errors[s.field] = 'Chọn ' + s.label.toLowerCase() + '.'
    }
  }

  // Ảnh: không bắt buộc, tối đa 10 ảnh gồm cả ảnh chính (đường dẫn ảnh do backend kiểm tra khi lưu)
  if (form.anh.length > TOI_DA_ANH) {
    errors.anh = `Mỗi sản phẩm tối đa ${TOI_DA_ANH} ảnh (gồm cả ảnh chính).`
  }

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

const tieuDeNut = computed(() => (isEdit ? 'Lưu thay đổi' : 'Thêm sản phẩm'))

async function guiForm() {
  if (dangTaiAnh.value || dangLuu.value) return // còn ảnh đang tải lên hoặc đang lưu thì chờ
  daBamLuu.value = true
  if (!kiemTra()) {
    return // có lỗi thì dừng
  }
  // Hợp lệ: chuẩn hóa dữ liệu rồi gọi API.
  // Number(...) vì giá trị lấy từ dropdown có thể là chuỗi, còn id trong dữ liệu là số.
  const duLieu = {
    ma: String(form.ma).trim().toUpperCase(),
    ten: String(form.ten).trim(),
    idDanhMuc: Number(form.idDanhMuc),
    idThuongHieu: Number(form.idThuongHieu),
    idXuatXu: Number(form.idXuatXu),
    idChatLieu: Number(form.idChatLieu),
    idDoCung: Number(form.idDoCung),
    idDiemCanBang: Number(form.idDiemCanBang),
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
      await sanPhamService.create(duLieu)
      toast.success('Đã thêm sản phẩm.')
    }
    router.push('/san-pham') // về danh sách, sản phẩm mới nằm ở đầu danh sách
  } catch (loi) {
    toast.error(loi.message || 'Không thể lưu sản phẩm. Vui lòng thử lại.')
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

          <div v-for="s in SELECTS" :key="s.field" class="col-md-6">
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
          </div>
        </div>
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
  </div>
</template>
