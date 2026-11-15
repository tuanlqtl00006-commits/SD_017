<script setup>
// Thẻ sản phẩm dùng chung ở trang khách (trang chủ, trang Sản phẩm): ảnh trên khung xám nhạt, hãng, tên, giá; giảm giá thì giá đỏ + giá gốc gạch.
// sp: { id, ten, brandName, anhChinh, minPrice, originalPrice, maxDiscountPercent }
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatMoney } from '../../utils/format'

const props = defineProps({
  sp: { type: Object, required: true },
  // Chữ nhỏ dưới tên (vd "Đã bán 10"), không có thì thôi
  nhan: { type: String, default: '' },
  // Thứ hạng (bán chạy): hiện "#1" ở góc
  hang: { type: Number, default: 0 },
})
const router = useRouter()
const giam = computed(() => Math.round(Number(props.sp.maxDiscountPercent) || 0))
const gia = computed(() => Number(props.sp.minPrice) || 0)
const giaGoc = computed(() => Number(props.sp.originalPrice) || 0)
const anh = computed(() => (props.sp.anhChinh && props.sp.anhChinh !== '/logo.png' ? props.sp.anhChinh : ''))

function moChiTiet() {
  router.push(`/shop/san-pham/${props.sp.id}`)
}
</script>

<template>
  <article class="tsp" role="link" tabindex="0" data-testid="the-san-pham" @click="moChiTiet" @keydown.enter="moChiTiet">
    <div class="tsp-anh">
      <span v-if="giam > 0" class="tsp-giam">-{{ giam }}%</span>
      <span v-if="hang" class="tsp-hang">#{{ hang }}</span>
      <img v-if="anh" :src="anh" :alt="sp.ten" loading="lazy" />
      <i v-else class="bi bi-image tsp-khong-anh" aria-hidden="true"></i>
      <span class="tsp-xem">Xem chi tiết <i class="bi bi-arrow-right"></i></span>
    </div>
    <div class="tsp-than">
      <div v-if="sp.brandName" class="tsp-hang-sx">{{ sp.brandName }}</div>
      <h3 class="tsp-ten" :title="sp.ten">{{ sp.ten }}</h3>
      <div class="tsp-gia">
        <template v-if="gia > 0">
          <span class="tsp-gia-ban" :class="{ 'is-giam': giam > 0 }">{{ formatMoney(gia) }}</span>
          <span v-if="giaGoc > gia" class="tsp-gia-goc">{{ formatMoney(giaGoc) }}</span>
        </template>
        <span v-else class="tsp-gia-ban">Liên hệ</span>
      </div>
      <div v-if="nhan" class="tsp-nhan" data-testid="the-san-pham-nhan">{{ nhan }}</div>
    </div>
  </article>
</template>

<style scoped>
.tsp {
  display: flex;
  flex-direction: column;
  height: 100%;
  cursor: pointer;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 18px;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  outline: none;
}
.tsp:hover,
.tsp:focus-visible {
  transform: translateY(-4px);
  border-color: transparent;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.05), 0 20px 44px rgba(15, 23, 42, 0.1);
}
.tsp-anh {
  position: relative;
  margin: 10px 10px 0;
  aspect-ratio: 1 / 1;
  display: grid;
  place-items: center;
  padding: 20px;
  background: #f3f4f6;
  border-radius: 14px;
  overflow: hidden;
}
.tsp-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  mix-blend-mode: multiply;
  transition: transform 0.4s ease;
}
.tsp:hover .tsp-anh img {
  transform: scale(1.06);
}
.tsp-khong-anh {
  font-size: 2.2rem;
  color: #cbd5e1;
}
.tsp-giam,
.tsp-hang {
  position: absolute;
  top: 10px;
  z-index: 1;
  padding: 3px 9px;
  font-size: 0.74rem;
  font-weight: 700;
  border-radius: 999px;
}
.tsp-giam {
  left: 10px;
  color: #fff;
  background: #e11d48;
}
.tsp-hang {
  right: 10px;
  color: #0f172a;
  background: #fff;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
}
.tsp-xem {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  padding: 9px 12px;
  font-size: 0.85rem;
  font-weight: 600;
  text-align: center;
  color: #fff;
  background: #0f172a;
  border-radius: 999px;
  opacity: 0;
  transform: translateY(8px);
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.tsp:hover .tsp-xem,
.tsp:focus-visible .tsp-xem {
  opacity: 1;
  transform: translateY(0);
}
.tsp-than {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 14px 16px 16px;
}
.tsp-hang-sx {
  margin-bottom: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}
.tsp-ten {
  display: -webkit-box;
  min-height: 2.7em;
  margin: 0 0 10px;
  overflow: hidden;
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.35;
  color: #0f172a;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.tsp-gia {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 8px;
  margin-top: auto;
}
.tsp-gia-ban {
  font-size: 1.05rem;
  font-weight: 800;
  color: #0f172a;
}
.tsp-gia-ban.is-giam {
  color: #e11d48;
}
.tsp-gia-goc {
  font-size: 0.82rem;
  color: #94a3b8;
  text-decoration: line-through;
}
.tsp-nhan {
  margin-top: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #1d4ed8;
}
@media (hover: none) {
  .tsp-xem {
    display: none;
  }
}
</style>
