<script setup>
// Logo đơn vị vận chuyển (GHN, GHTK, Viettel Post) + tên. Chưa có file logo (frontend/public/van-chuyen/*.png) hoặc đơn vị khác
// thì hiện nhãn chữ thay cho logo. Chưa ghi đơn vị nào thì hiện "—".
import { computed, ref, watch } from 'vue'
import { nhanDienVanChuyen } from '../../utils/vanChuyen'

const props = defineProps({
  ten: { type: String, default: '' },
})

const dv = computed(() => nhanDienVanChuyen(props.ten))
const loiAnh = ref(false) // file logo chưa có / hỏng
watch(() => props.ten, () => { loiAnh.value = false })
</script>

<template>
  <span v-if="dv" class="vc">
    <img v-if="dv.logo && !loiAnh" class="vc-logo" :src="dv.logo" :alt="`Logo ${dv.ma}`" @error="loiAnh = true">
    <span v-else class="vc-badge">{{ dv.ma || dv.ten }}</span>
    <span v-if="dv.ma" class="vc-ten">{{ dv.ten }}</span>
  </span>
  <span v-else>—</span>
</template>

<style scoped>
.vc {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.vc-logo {
  height: 28px;
  max-width: 96px;
  object-fit: contain;
}
.vc-badge {
  display: inline-block;
  padding: 0.12rem 0.55rem;
  border: 1.5px solid var(--fs-primary);
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--fs-primary-dark);
  background: var(--fs-primary-soft);
}
.vc-ten {
  font-size: 0.9rem;
}
</style>
