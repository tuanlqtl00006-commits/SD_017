<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PhieuGiamGiaFormModal from './PhieuGiamGiaFormModal.vue'
import { phieuGiamGiaService } from '../../services/phieuGiamGiaService'
import { useToast } from '../../composables/useToast'

// Trang riêng để thêm (/phieu-giam-gia/them) hoặc sửa (/phieu-giam-gia/:id/sua) phiếu giảm giá, giống video.
const route = useRoute()
const router = useRouter()
const toast = useToast()

const id = computed(() => route.params.id)
const item = ref(null)
const all = ref([])
const loading = ref(true)
const saving = ref(false)

const back = () => router.push('/phieu-giam-gia')

onMounted(async () => {
  try {
    all.value = await phieuGiamGiaService.getAll()
    if (id.value) item.value = await phieuGiamGiaService.getById(id.value)
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu phiếu giảm giá.')
    if (id.value) return back()
  } finally {
    loading.value = false
  }
})

async function save(payload) {
  saving.value = true
  try {
    // Phiếu cá nhân: backend gửi mail (phiếu mới / cập nhật / hủy) và trả về câu tóm tắt trong ketQuaMail
    let daLuu
    if (id.value) {
      daLuu = await phieuGiamGiaService.update(id.value, payload)
      toast.success(['Đã lưu thay đổi phiếu giảm giá.', daLuu?.ketQuaMail].filter(Boolean).join(' '))
    } else {
      daLuu = await phieuGiamGiaService.create(payload)
      toast.success(['Tạo phiếu giảm giá thành công.', daLuu?.ketQuaMail].filter(Boolean).join(' '))
    }
    back()
  } catch (e) {
    toast.error(e.message || 'Không thể lưu phiếu giảm giá. Vui lòng thử lại.')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <button type="button" class="ad-back" aria-label="Quay lại danh sách" @click="back">
      <i class="bi bi-arrow-left" aria-hidden="true"></i>
    </button>
    <PhieuGiamGiaFormModal v-if="!loading" as-page :item="item" :all="all" :saving="saving" @save="save" @close="back" />
  </div>
</template>
