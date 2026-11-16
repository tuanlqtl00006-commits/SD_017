<script setup>
// Trang "Đánh giá hỗ trợ" (chỉ quản lý): tổng hợp đánh giá tốt / xấu của khách sau khi chat (góp ý 16/11 [12:30–12:59]).
// "Xem chat" mở đúng cuộc trò chuyện ở màn Hỗ trợ trực tuyến.
import { useRouter } from 'vue-router'
import DanhGiaHoTro from './DanhGiaHoTro.vue'

const router = useRouter()
const moChat = (id) => router.push({ path: '/ho-tro-truc-tuyen', query: { mo: id } })
</script>

<template>
  <div class="ad-page">
    <DanhGiaHoTro @mo="moChat" />
  </div>
</template>
