<template>
  <div class="modal fade show d-block" tabindex="-1" style="background-color: rgba(0,0,0,0.5);">
    <div class="modal-dialog modal-xl modal-dialog-centered">
      <div class="modal-content border-0 shadow-lg rounded-4">
        <div class="modal-header border-bottom-0 pb-0">
          <h5 class="modal-title fw-bold text-primary"><i class="bi bi-list-check me-2"></i>Chi tiết đợt giảm giá: {{ campaign?.tenDot }}</h5>
          <button type="button" class="btn-close" @click="$emit('close')"></button>
        </div>
        <div class="modal-body">
          <div class="row">
            <!-- Form Add Product -->
            <div class="col-md-4">
              <div class="card border-0 shadow-sm rounded-3 bg-light">
                <div class="card-body">
                  <h6 class="fw-bold mb-3">Thêm sản phẩm áp dụng</h6>
                  <!-- Cho biết biến thể thêm vào đây khi nào sẽ được giảm: giá sau giảm + giá gốc gạch ngang hiện ở cột "Giá bán" của trang Biến thể sản phẩm -->
                  <div class="alert py-2 px-3 small" :class="trangThaiDot === 'DANG_DIEN_RA' ? 'alert-success' : 'alert-warning'" role="status" data-testid="ghi-chu-dot">{{ ghiChuDot }}</div>
                  <form @submit.prevent="addProduct">
                    <div class="mb-3">
                      <label class="form-label small text-dark fw-medium">Chọn sản phẩm <span class="text-danger">*</span></label>
                      <Select2 id="dgg-ct-bien-the" v-model="selectedProduct" :options="dsBienThe" :disabled="daKetThuc" placeholder="-- Chọn sản phẩm --" class="ad-control" />
                    </div>
                    <div class="mb-3">
                      <label class="form-label small text-dark fw-medium">Phần trăm giảm (%) <span class="text-danger">*</span></label>
                      <input type="number" class="form-control" v-model.number="percent" min="1" max="100" required :disabled="daKetThuc">
                    </div>
                    <!-- Cảnh báo trùng đợt: biến thể đang chọn cũng nằm trong đợt khác chạy cùng thời gian -->
                    <div v-if="dotTrung.length" class="alert alert-warning py-2 px-3 small" role="status" data-testid="ct-canh-bao-trung">
                      <i class="bi bi-exclamation-triangle me-1"></i>
                      Biến thể này cũng nằm trong {{ dotTrung.length }} đợt khác trùng thời gian:
                      <ul class="mb-1 ps-3">
                        <li v-for="m in dotTrung" :key="m.idDot">{{ m.maDot }} giảm {{ m.phanTram }}% ({{ dinhDangNgayGio(m.ngayBatDau) }} → {{ dinhDangNgayGio(m.ngayKetThuc) }})</li>
                      </ul>
                      <span data-testid="ct-ket-qua-trung">Khi chồng nhau hệ thống áp dụng <strong>{{ tenCheDo }}</strong>: <strong>{{ formatPhanTram(phanTramGop) }}</strong>.</span>
                    </div>
                    <button type="submit" class="btn btn-primary w-100 fw-medium" :disabled="daKetThuc">Thêm vào đợt giảm giá</button>
                    <div v-if="errorMsg" class="text-danger small mt-2 fw-medium text-center"><i class="bi bi-exclamation-triangle me-1"></i>{{ errorMsg }}</div>
                  </form>
                </div>
              </div>
            </div>

            <!-- List Products -->
            <div class="col-md-8">
              <h6 class="fw-bold mb-3">Danh sách sản phẩm đang áp dụng</h6>
              <div class="table-responsive bg-white rounded-3 shadow-sm border">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light text-secondary">
                    <tr>
                      <th class="fw-medium">Mã SPCT</th>
                      <th class="fw-medium">Giá gốc</th>
                      <th class="fw-medium">Mức giảm</th>
                      <th class="fw-medium">Giá sau giảm</th>
                      <th class="fw-medium text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in details" :key="item.id">
                      <td><span class="fw-medium text-dark">{{ getProductCode(item.idSanPhamChiTiet) }}</span></td>
                      <td>{{ formatCurrency(getProductPrice(item.idSanPhamChiTiet)) }}</td>
                      <td><span class="badge bg-danger bg-opacity-10 text-danger">-{{ item.phanTramGiamBienThe }}%</span></td>
                      <td class="fw-bold text-success">{{ formatCurrency(calculatePrice(item.idSanPhamChiTiet, item.phanTramGiamBienThe)) }}</td>
                      <td class="text-center">
                        <button class="btn btn-sm btn-light text-danger rounded-3" @click="removeProduct(item.id)" title="Gỡ sản phẩm khỏi đợt">
                          <i class="bi bi-trash-fill"></i> Gỡ
                        </button>
                      </td>
                    </tr>
                    <tr v-if="details.length === 0">
                      <td colspan="5" class="text-center py-4 text-muted">Chưa có sản phẩm nào được áp dụng giảm giá.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../services/api'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { CHE_DO_CHONG_DOT, tinhTrangThai, daKetThuc as quaNgayKetThuc } from '../../constants/dotGiamGia'
import { formatPhanTram, gopPhanTram } from '../../utils/giamGia'
import { dinhDangNgayGio, giaoNhau } from '../../utils/ngayGio'
import { useToast } from '../../composables/useToast'
import Select2 from '../common/Select2.vue'

const props = defineProps({
  campaign: Object
})
const emit = defineEmits(['close'])

const products = ref([])
const details = ref([])
const selectedProduct = ref('')
const percent = ref(props.campaign?.phanTramGiamDot || 10)
const errorMsg = ref('')
const toast = useToast()

// Trạng thái của đợt quyết định khi nào mức giảm hiện ở cột "Giá bán" (trang Biến thể sản phẩm)
const trangThaiDot = computed(() => tinhTrangThai(props.campaign || {}))
// Quá ngày kết thúc thì khóa thêm sản phẩm (kể cả khi công tắc đang tắt)
const daKetThuc = computed(() => quaNgayKetThuc(props.campaign || {}))
const ghiChuDot = computed(() => {
  if (daKetThuc.value) return 'Đợt đã kết thúc: không thể thêm sản phẩm mới.'
  switch (trangThaiDot.value) {
    case 'DANG_DIEN_RA':
      return 'Đợt đang diễn ra: biến thể thêm vào sẽ được giảm giá ngay; trang Biến thể sản phẩm tự hiện giá sau giảm và giá gốc gạch ngang ở cột "Giá bán".'
    case 'SAP_DIEN_RA':
      return `Đợt sắp diễn ra: biến thể thêm vào sẽ bắt đầu được giảm từ ${dinhDangNgayGio(props.campaign?.ngayBatDau)} (trước đó cột "Giá bán" vẫn là giá gốc và chỉ báo "Sắp giảm").`
    case 'NGUNG_HOAT_DONG':
      return 'Đợt đang ngừng hoạt động: phải bật đợt lên thì mức giảm mới được áp dụng và hiện ở cột "Giá bán".'
    default:
      return 'Đợt đã kết thúc: không thể thêm sản phẩm mới.'
  }
})

const fetchData = async () => {
  try {
    const [prodRes, detailRes] = await Promise.all([
      api.get('/bien-the-san-pham'),
      api.get(`/dot-giam-gia-chi-tiet/campaign/${props.campaign.id}`)
    ])
    products.value = prodRes.data
    details.value = detailRes.data
  } catch (error) {
    console.error(error)
    errorMsg.value = error.message || 'Không tải được dữ liệu.'
  }
}

// Các đợt khác đang bật, chưa kết thúc + cách gộp khi chồng nhau (để cảnh báo trùng đợt khi chọn biến thể)
const apDungKhac = ref({ chinhSach: 'MAX', muc: [] })
const cheDoCauHinh = ref(null)
async function taiDotKhac() {
  try {
    const r = await dotGiamGiaService.apDung()
    apDungKhac.value = r && Array.isArray(r.muc) ? r : { chinhSach: 'MAX', muc: [] }
  } catch {
    // Không có thông tin các đợt khác thì thôi, chỉ không cảnh báo trùng đợt
  }
  try {
    const c = await dotGiamGiaService.cheDo()
    cheDoCauHinh.value = c?.cheDo ? c : null
  } catch {
    cheDoCauHinh.value = null
  }
}

onMounted(() => {
  fetchData()
  taiDotKhac()
})

const cheDoDung = computed(() => {
  const khoa = cheDoCauHinh.value?.cheDo || apDungKhac.value.chinhSach
  return CHE_DO_CHONG_DOT[khoa] ? khoa : 'MAX'
})
const tenCheDo = computed(() => CHE_DO_CHONG_DOT[cheDoDung.value].label.toLowerCase())
// Đợt KHÁC trùng thời gian với đợt này trên biến thể đang chọn
const dotTrung = computed(() => {
  const id = selectedProduct.value
  if (id === '' || id === null || id === undefined || !props.campaign) return []
  const khung = { batDau: props.campaign.ngayBatDau, ketThuc: props.campaign.ngayKetThuc }
  return apDungKhac.value.muc.filter(
    (m) => m.idBienThe === id && m.idDot !== props.campaign.id && giaoNhau(khung, { batDau: m.ngayBatDau, ketThuc: m.ngayKetThuc }),
  )
})
// Mức giảm sau khi gộp đợt này (với % đang nhập) và các đợt trùng, theo cách tính đang dùng
const phanTramGop = computed(() => gopPhanTram([Number(percent.value) || 0, ...dotTrung.value.map((m) => Number(m.phanTram) || 0)], cheDoDung.value))

// Chỉ cho chọn biến thể đang hoạt động và chưa nằm trong đợt này
const selectableProducts = computed(() =>
  products.value.filter(p => p.hoatDong && !details.value.some(d => d.idSanPhamChiTiet === p.id))
)

const getProduct = (id) => products.value.find(p => p.id === id)

const getProductCode = (id) => {
  const p = getProduct(id)
  return p ? p.ma : 'Không xác định'
}

const getProductPrice = (id) => {
  const p = getProduct(id)
  return p ? p.giaBan : 0
}

const calculatePrice = (id, percentDrop) => {
  const price = getProductPrice(id)
  return price - (price * percentDrop / 100)
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

// Mục cho ô chọn biến thể: mã biến thể + giá hiện tại
const dsBienThe = computed(() =>
  selectableProducts.value.map((sp) => ({ value: sp.id, label: sp.ma, sub: `Giá: ${formatCurrency(sp.giaBan)}` })),
)

const addProduct = async () => {
  errorMsg.value = ''
  if (selectedProduct.value === '' || selectedProduct.value === null) {
    errorMsg.value = 'Chọn biến thể cần thêm vào đợt giảm giá.'
    return
  }
  try {
    await api.post(`/dot-giam-gia-chi-tiet/campaign/${props.campaign.id}`, {
      idSanPhamChiTiet: selectedProduct.value,
      phanTramGiamBienThe: percent.value
    })
    selectedProduct.value = ''
    toast.success('Đã thêm biến thể vào đợt giảm giá.')
    fetchData()
  } catch (error) {
    errorMsg.value = error.message || 'Có lỗi xảy ra'
  }
}

const removeProduct = async (id) => {
  try {
    await api.delete(`/dot-giam-gia-chi-tiet/${id}`)
    fetchData()
  } catch (error) {
    console.error(error)
    errorMsg.value = error.message || 'Không gỡ được sản phẩm khỏi đợt.'
  }
}
</script>
