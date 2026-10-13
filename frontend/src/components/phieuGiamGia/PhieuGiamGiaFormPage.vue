<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PhieuGiamGiaFormModal from './PhieuGiamGiaFormModal.vue'
import { cauBaoKetQuaMail, phieuGiamGiaService, theoDoiKetQuaMail } from '../../services/phieuGiamGiaService'
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

// Câu tóm tắt của backend bắt đầu bằng "Chưa gửi" / "Không gửi được" nghĩa là KHÔNG có thư nào đi
function mailChuaGui(ketQuaMail) {
  return /^(Chưa gửi|Không gửi được)/.test(ketQuaMail || '')
}

// "Đang gửi N mail..." = thư đang gửi ở nền: hỏi lại backend vài giây sau để báo rõ ĐÃ GỬI hay GỬI LỖI (kèm lý do thật).
// Chạy ngầm (không chặn việc quay về danh sách); thông báo hiện ở góc màn hình dù đang ở trang nào.
async function baoKetQuaMail(phieu) {
  const kq = await theoDoiKetQuaMail(phieu.id)
  const bao = cauBaoKetQuaMail(kq, phieu.ma)
  if (!bao) return
  if (bao.loai === 'error') toast.error(bao.noiDung, bao.tieuDe)
  else toast.success(bao.noiDung)
}

async function save(payload) {
  saving.value = true
  try {
    // Phiếu cá nhân: backend gửi mail (phiếu mới / cập nhật / hủy) và trả về câu tóm tắt trong ketQuaMail.
    // Phiếu đã lưu thành công; nếu mail chưa gửi được (chưa cấu hình Gmail) thì báo bằng thông báo ĐỎ để không tưởng thư đã đi.
    let daLuu
    let loiLuu
    if (id.value) {
      daLuu = await phieuGiamGiaService.update(id.value, payload)
      loiLuu = 'Đã lưu thay đổi phiếu giảm giá.'
    } else {
      daLuu = await phieuGiamGiaService.create(payload)
      loiLuu = 'Tạo phiếu giảm giá thành công.'
    }
    const ketQuaMail = daLuu?.ketQuaMail
    if (mailChuaGui(ketQuaMail)) {
      toast.error([loiLuu, ketQuaMail].join(' '), 'Phiếu đã lưu nhưng mail chưa gửi')
    } else {
      toast.success([loiLuu, ketQuaMail].filter(Boolean).join(' '))
      if (daLuu?.id && /^Đang gửi/.test(ketQuaMail || '')) baoKetQuaMail(daLuu)
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
