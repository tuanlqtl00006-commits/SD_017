<script setup>
// Nút "Ca làm việc" trên header: chấm xanh khi đang trong ca, cam khi đã quá giờ kết thúc ca (nhắc giao ca), xám khi chưa nhận ca.
// Bấm để nhận ca / xem tổng kết / giao ca.
import { onMounted, ref } from 'vue'
import GiaoCaModal from './GiaoCaModal.vue'
import { useGiaoCa } from '../../composables/useGiaoCa'

const { trangThai, dangTrongCa, quaGioCa, taiLai } = useGiaoCa()
const mo = ref(false)

onMounted(() => {
  if (!trangThai.daTai) taiLai()
})
</script>

<template>
  <button
    type="button"
    class="btn btn-light btn-sm border rounded-3 px-3 fw-medium d-flex align-items-center gap-2"
    :class="{ 'gc-qua-gio': quaGioCa }"
    :title="quaGioCa ? 'Đã quá giờ kết thúc ca, bấm để giao ca' : dangTrongCa ? `Đang trong ca ${trangThai.dangMo.maGiaoCa}` : 'Chưa nhận ca'"
    data-testid="gc-nut"
    @click="mo = true"
  >
    <i class="bi bi-arrow-left-right text-secondary" aria-hidden="true"></i>
    Ca làm việc
    <span v-if="quaGioCa" class="gc-nhan-qua-gio">Quá giờ</span>
    <span v-else :class="dangTrongCa ? 'ad-live-dot' : 'gc-cham-xam'" aria-hidden="true"></span>
    <span class="visually-hidden">{{ quaGioCa ? 'đã quá giờ kết thúc ca' : dangTrongCa ? 'đang trong ca' : 'chưa nhận ca' }}</span>
  </button>
  <GiaoCaModal v-if="mo" @close="mo = false" />
</template>

<style scoped>
.gc-qua-gio {
  border-color: #fdba74 !important;
  background: #fff7ed !important;
}
.gc-nhan-qua-gio {
  padding: 0 0.4rem;
  border-radius: 999px;
  background: #f97316;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
}
.gc-cham-xam {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #b8c0cc;
  display: inline-block;
}
</style>
