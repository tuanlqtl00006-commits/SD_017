<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import { sanPhamService } from '../../services/sanPhamService'
import { thuocTinhService } from '../../services/thuocTinhService'
import { useToast } from '../../composables/useToast'
import { tuDanhSach } from '../../utils/luaChon'
import { cat, timBanTrung } from '../../utils/kiemTra'
import { chatLieuHopDanhMuc, idKhongApDung, laDanhMucVot, laKhongApDung } from '../../utils/danhMuc'

const props = defineProps({
  tenKhoiTao: { type: String, default: '' },
  danhSachSanPham: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'saved'])

const toast = useToast()
const dangLuu = ref(false)
const dangTai = ref(true)

const thuocTinh = reactive({
  'danh-muc': [],
  'thuong-hieu': [],
  'xuat-xu': [],
  'chat-lieu': [],
  'do-cung': [],
  'diem-can-bang': [],
  'mau-sac': [],
  'trong-luong': [],
  'chu-vi': [],
})

const form = reactive({
  ten: props.tenKhoiTao || '',
  idDanhMuc: '',
  idThuongHieu: '',
  idXuatXu: '',
  idChatLieu: '',
  idDoCung: '',
  idDiemCanBang: '',
  idMauSac: '',
  giaBan: 100000,
  soLuongTon: 10,
})

const errors = reactive({})
const daBamLuu = ref(false)

const daChonDanhMuc = computed(() => form.idDanhMuc !== '' && form.idDanhMuc != null)
const tenDanhMuc = computed(() => thuocTinh['danh-muc'].find((x) => x.id === Number(form.idDanhMuc))?.ten ?? '')
const laVot = computed(() => daChonDanhMuc.value && laDanhMucVot(tenDanhMuc.value))

const idKad = (slug) => idKhongApDung(thuocTinh[slug])

function luaChonChatLieu() {
  const ds = thuocTinh['chat-lieu'].filter((x) => x.hoatDong)
  if (!daChonDanhMuc.value) return ds
  return ds.filter((o) => chatLieuHopDanhMuc(o, tenDanhMuc.value))
}

const dsDanhMuc = computed(() => tuDanhSach(thuocTinh['danh-muc'].filter((x) => x.hoatDong)))
const dsThuongHieu = computed(() => tuDanhSach(thuocTinh['thuong-hieu'].filter((x) => x.hoatDong)))
const dsXuatXu = computed(() => tuDanhSach(thuocTinh['xuat-xu'].filter((x) => x.hoatDong)))
const dsChatLieu = computed(() => tuDanhSach(luaChonChatLieu()))
const dsDoCung = computed(() => tuDanhSach(thuocTinh['do-cung'].filter((x) => x.hoatDong && !laKhongApDung(x))))
const dsDiemCanBang = computed(() => tuDanhSach(thuocTinh['diem-can-bang'].filter((x) => x.hoatDong && !laKhongApDung(x))))
const dsMauSac = computed(() => tuDanhSach(thuocTinh['mau-sac'].filter((x) => x.hoatDong)))

watch(
  () => form.idDanhMuc,
  () => {
    if (!form.idDanhMuc) return
    if (form.idChatLieu && !luaChonChatLieu().some((o) => o.id === Number(form.idChatLieu))) {
      form.idChatLieu = ''
    }
    if (laVot.value) {
      if (form.idDoCung === idKad('do-cung')) form.idDoCung = ''
      if (form.idDiemCanBang === idKad('diem-can-bang')) form.idDiemCanBang = ''
    } else {
      form.idDoCung = idKad('do-cung') ?? ''
      form.idDiemCanBang = idKad('diem-can-bang') ?? ''
    }
  },
)

const khoaTen = (s) => cat(s).toLowerCase().replace(/\s+/g, ' ')

function kiemTraTen() {
  const ten = cat(form.ten)
  if (!ten) {
    if (daBamLuu.value) errors.ten = 'Nhập tên sản phẩm.'
    else delete errors.ten
  } else if (ten.length > 255) {
    errors.ten = 'Tên sản phẩm tối đa 255 ký tự.'
  } else if (timBanTrung(props.danhSachSanPham, ten, (x) => x.ten, khoaTen, null)) {
    errors.ten = 'Tên sản phẩm đã tồn tại.'
  } else {
    delete errors.ten
  }
}

function validate() {
  for (const k of Object.keys(errors)) delete errors[k]
  kiemTraTen()
  if (!form.idDanhMuc) errors.idDanhMuc = 'Chọn danh mục.'
  if (!form.idThuongHieu) errors.idThuongHieu = 'Chọn thương hiệu.'
  if (!form.idXuatXu) errors.idXuatXu = 'Chọn xuất xứ.'
  if (!form.idChatLieu) errors.idChatLieu = 'Chọn chất liệu.'
  if (laVot.value) {
    if (!form.idDoCung) errors.idDoCung = 'Chọn độ cứng.'
    if (!form.idDiemCanBang) errors.idDiemCanBang = 'Chọn điểm cân bằng.'
  }
  const gia = Number(form.giaBan)
  if (form.giaBan === '' || form.giaBan === null || Number.isNaN(gia) || gia < 1000) {
    errors.giaBan = 'Giá bán tối thiểu 1.000 ₫.'
  }
  const sl = Number(form.soLuongTon)
  if (form.soLuongTon === '' || form.soLuongTon === null || Number.isNaN(sl) || sl < 0) {
    errors.soLuongTon = 'Số lượng không được âm.'
  }
  return Object.keys(errors).length === 0
}

async function submit() {
  daBamLuu.value = true
  if (!validate()) {
    nextTick(() => document.querySelector('#sp-them-nhanh-form .is-invalid')?.focus())
    return
  }

  dangLuu.value = true
  try {
    const idDoCung = laVot.value ? Number(form.idDoCung) : idKad('do-cung')
    const idDiemCanBang = laVot.value ? Number(form.idDiemCanBang) : idKad('diem-can-bang')

    // Chọn màu mặc định: màu người dùng chọn, hoặc màu đầu tiên trong DB
    const mauMacDinh = form.idMauSac ? Number(form.idMauSac) : thuocTinh['mau-sac'].find((x) => x.hoatDong)?.id
    const tlMacDinh = laVot.value ? thuocTinh['trong-luong'].find((x) => x.hoatDong && !laKhongApDung(x))?.id : idKad('trong-luong')
    const cvMacDinh = laVot.value ? thuocTinh['chu-vi'].find((x) => x.hoatDong && !laKhongApDung(x))?.id : idKad('chu-vi')

    const bienThes = []
    if (mauMacDinh) {
      bienThes.push({
        idMauSac: mauMacDinh,
        idTrongLuong: tlMacDinh,
        idChuVi: cvMacDinh,
        giaBan: Number(form.giaBan) || 100000,
        soLuongTon: Number(form.soLuongTon) || 0,
      })
    }

    const payload = {
      ten: cat(form.ten),
      idDanhMuc: Number(form.idDanhMuc),
      idThuongHieu: Number(form.idThuongHieu),
      idXuatXu: Number(form.idXuatXu),
      idChatLieu: Number(form.idChatLieu),
      idDoCung,
      idDiemCanBang,
      anhChinh: '',
      anhPhu: [],
      moTa: '',
      bienThes,
    }

    const moi = await sanPhamService.create(payload)
    toast.success(`Đã thêm nhanh sản phẩm "${moi.ten}".`)
    emit('saved', moi)
    emit('close')
  } catch (err) {
    toast.error(err.message || 'Không thể thêm nhanh sản phẩm. Vui lòng thử lại.')
  } finally {
    dangLuu.value = false
  }
}

onMounted(async () => {
  try {
    const cacLoai = Object.keys(thuocTinh)
    const ketQua = await Promise.all(cacLoai.map((loai) => thuocTinhService.getAll(loai)))
    cacLoai.forEach((loai, i) => {
      thuocTinh[loai] = ketQua[i]
    })
    // Tự điền mặc định mục đầu tiên cho các thuộc tính phổ biến
    const dm = thuocTinh['danh-muc'].find((x) => x.hoatDong)
    if (dm) form.idDanhMuc = dm.id
    const th = thuocTinh['thuong-hieu'].find((x) => x.hoatDong)
    if (th) form.idThuongHieu = th.id
    const xx = thuocTinh['xuat-xu'].find((x) => x.hoatDong)
    if (xx) form.idXuatXu = xx.id
    const cl = thuocTinh['chat-lieu'].find((x) => x.hoatDong)
    if (cl) form.idChatLieu = cl.id
    const ms = thuocTinh['mau-sac'].find((x) => x.hoatDong)
    if (ms) form.idMauSac = ms.id
  } catch (err) {
    toast.error('Không tải được thuộc tính sản phẩm.')
  } finally {
    dangTai.value = false
  }
  await nextTick()
  document.getElementById('sp-tn-ten')?.focus()
})
</script>

<template>
  <BaseModal title="Thêm nhanh sản phẩm" size="lg" static-backdrop @close="emit('close')">
    <div v-if="dangTai" class="ad-empty">Đang tải dữ liệu…</div>
    <form v-else id="sp-them-nhanh-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-12">
          <label class="ad-label" for="sp-tn-ten">Tên sản phẩm <span class="ad-required">*</span></label>
          <input
            id="sp-tn-ten"
            v-model="form.ten"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ten }"
            placeholder="Ví dụ: Vợt cầu lông Yonex Astrox 99 Pro…"
            autocomplete="off"
            @input="kiemTraTen"
            @blur="kiemTraTen"
          />
          <p v-if="errors.ten" class="ad-error">{{ errors.ten }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="sp-tn-dm">Danh mục <span class="ad-required">*</span></label>
          <Select2 id="sp-tn-dm" v-model="form.idDanhMuc" :options="dsDanhMuc" class="ad-control" :class="{ 'is-invalid': errors.idDanhMuc }" placeholder="Chọn danh mục" />
          <p v-if="errors.idDanhMuc" class="ad-error">{{ errors.idDanhMuc }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="sp-tn-th">Thương hiệu <span class="ad-required">*</span></label>
          <Select2 id="sp-tn-th" v-model="form.idThuongHieu" :options="dsThuongHieu" class="ad-control" :class="{ 'is-invalid': errors.idThuongHieu }" placeholder="Chọn thương hiệu" />
          <p v-if="errors.idThuongHieu" class="ad-error">{{ errors.idThuongHieu }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="sp-tn-xx">Xuất xứ <span class="ad-required">*</span></label>
          <Select2 id="sp-tn-xx" v-model="form.idXuatXu" :options="dsXuatXu" class="ad-control" :class="{ 'is-invalid': errors.idXuatXu }" placeholder="Chọn xuất xứ" />
          <p v-if="errors.idXuatXu" class="ad-error">{{ errors.idXuatXu }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="sp-tn-cl">Chất liệu <span class="ad-required">*</span></label>
          <Select2 id="sp-tn-cl" v-model="form.idChatLieu" :options="dsChatLieu" class="ad-control" :class="{ 'is-invalid': errors.idChatLieu }" placeholder="Chọn chất liệu" />
          <p v-if="errors.idChatLieu" class="ad-error">{{ errors.idChatLieu }}</p>
        </div>

        <template v-if="laVot">
          <div class="col-md-6">
            <label class="ad-label" for="sp-tn-dc">Độ cứng <span class="ad-required">*</span></label>
            <Select2 id="sp-tn-dc" v-model="form.idDoCung" :options="dsDoCung" class="ad-control" :class="{ 'is-invalid': errors.idDoCung }" placeholder="Chọn độ cứng" />
            <p v-if="errors.idDoCung" class="ad-error">{{ errors.idDoCung }}</p>
          </div>

          <div class="col-md-6">
            <label class="ad-label" for="sp-tn-cb">Điểm cân bằng <span class="ad-required">*</span></label>
            <Select2 id="sp-tn-cb" v-model="form.idDiemCanBang" :options="dsDiemCanBang" class="ad-control" :class="{ 'is-invalid': errors.idDiemCanBang }" placeholder="Chọn điểm cân bằng" />
            <p v-if="errors.idDiemCanBang" class="ad-error">{{ errors.idDiemCanBang }}</p>
          </div>
        </template>

        <div class="col-12"><hr class="my-1" /><span class="small fw-semibold text-muted">Biến thể khởi tạo</span></div>

        <div class="col-md-4">
          <label class="ad-label" for="sp-tn-ms">Màu sắc</label>
          <Select2 id="sp-tn-ms" v-model="form.idMauSac" :options="dsMauSac" class="ad-control" placeholder="Màu mặc định" />
        </div>

        <div class="col-md-4">
          <label class="ad-label" for="sp-tn-gia">Giá bán <span class="ad-required">*</span></label>
          <MoneyInput id="sp-tn-gia" v-model="form.giaBan" :invalid="!!errors.giaBan" />
          <p v-if="errors.giaBan" class="ad-error">{{ errors.giaBan }}</p>
        </div>

        <div class="col-md-4">
          <label class="ad-label" for="sp-tn-sl">Số lượng tồn</label>
          <input id="sp-tn-sl" v-model="form.soLuongTon" type="number" min="0" step="1" class="form-control ad-control" :class="{ 'is-invalid': errors.soLuongTon }" />
          <p v-if="errors.soLuongTon" class="ad-error">{{ errors.soLuongTon }}</p>
        </div>
      </div>
    </form>

    <template #footer>
      <div class="d-flex justify-content-end gap-2 w-100">
        <button type="button" class="ad-btn" :disabled="dangLuu" @click="emit('close')">Hủy</button>
        <button type="submit" form="sp-them-nhanh-form" class="ad-btn ad-btn-primary" :disabled="dangLuu || dangTai">
          <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          Thêm sản phẩm
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
#sp-them-nhanh-form :deep(input),
#sp-them-nhanh-form :deep(.form-control),
#sp-them-nhanh-form :deep(.money-input input) {
  text-align: left !important;
}
</style>
