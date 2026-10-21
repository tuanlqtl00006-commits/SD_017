<script setup>
// Nút "Ca làm việc" trên header: chấm xanh khi đang trong ca, xám khi chưa nhận ca. Bấm để nhận ca / xem tổng kết / giao ca.
import { onMounted, ref } from 'vue'
import GiaoCaModal from './GiaoCaModal.vue'
import { useGiaoCa } from '../../composables/useGiaoCa'

const { trangThai, dangTrongCa, taiLai } = useGiaoCa()
const mo = ref(false)

onMounted(() => {
  if (!trangThai.daTai) taiLai()
})
</script>

<template>
  <button
    type="button"
    class="btn btn-light btn-sm border rounded-3 px-3 fw-medium d-flex align-items-center gap-2"
    :title="dangTrongCa ? `Đang trong ca ${trangThai.dangMo.maGiaoCa}` : 'Chưa nhận ca'"
    @click="mo = true"
  >
    <i class="bi bi-arrow-left-right text-secondary" aria-hidden="true"></i>
    Ca làm việc
    <span :class="dangTrongCa ? 'ad-live-dot' : 'gc-cham-xam'" aria-hidden="true"></span>
    <span class="visually-hidden">{{ dangTrongCa ? 'đang trong ca' : 'chưa nhận ca' }}</span>
  </button>
  <GiaoCaModal v-if="mo" @close="mo = false" />
</template>

<style scoped>
.gc-cham-xam {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #b8c0cc;
  display: inline-block;
}
</style>
