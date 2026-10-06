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
                  <form @submit.prevent="addProduct">
                    <div class="mb-3">
                      <label class="form-label small text-dark fw-medium">Chọn sản phẩm <span class="text-danger">*</span></label>
                      <select class="form-select" v-model="selectedProduct" required>
                        <option value="">-- Chọn sản phẩm --</option>
                        <option v-for="sp in products" :key="sp.id" :value="sp.id">
                          [{{ sp.maSpct }}] - Giá: {{ sp.giaBan }}đ
                        </option>
                      </select>
                    </div>
                    <div class="mb-3">
                      <label class="form-label small text-dark fw-medium">Phần trăm giảm (%) <span class="text-danger">*</span></label>
                      <input type="number" class="form-control" v-model.number="percent" min="1" max="100" required>
                    </div>
                    <button type="submit" class="btn btn-primary w-100 fw-medium">Thêm vào đợt giảm giá</button>
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
import { ref, onMounted, defineProps, defineEmits } from 'vue'
import api from '../services/api'

const props = defineProps({
  campaign: Object
})
const emit = defineEmits(['close'])

const products = ref([])
const details = ref([])
const selectedProduct = ref('')
const percent = ref(props.campaign?.phanTramGiamDot || 10)
const errorMsg = ref('')

const fetchData = async () => {
  try {
    const [prodRes, detailRes] = await Promise.all([
      api.get('/san-pham-chi-tiet'),
      api.get(`/dot-giam-gia-chi-tiet/campaign/${props.campaign.id}`)
    ])
    products.value = prodRes.data
    details.value = detailRes.data
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchData()
})

const getProduct = (id) => products.value.find(p => p.id === id)

const getProductCode = (id) => {
  const p = getProduct(id)
  return p ? p.maSpct : 'Unknown'
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

const addProduct = async () => {
  errorMsg.value = ''
  try {
    await api.post(`/dot-giam-gia-chi-tiet/campaign/${props.campaign.id}`, {
      idSanPhamChiTiet: selectedProduct.value,
      phanTramGiamBienThe: percent.value
    })
    selectedProduct.value = ''
    fetchData()
  } catch (error) {
    errorMsg.value = error.response?.data?.message || 'Có lỗi xảy ra'
  }
}

const removeProduct = async (id) => {
  try {
    await api.delete(`/dot-giam-gia-chi-tiet/${id}`)
    fetchData()
  } catch (error) {
    console.error(error)
  }
}
</script>
