<script setup>
// Thẻ sản phẩm trong khung chat (cô review 14/11 [20:24]): trợ lý ảo gợi ý sản phẩm thì hiện từng thẻ ảnh, tên, giá;
// bấm vào thẻ sang trang chi tiết. Ở màn nhân viên (không phải trang /shop) thì mở trang sản phẩm ở tab mới.
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { taiSanPhamChat } from './khoSanPhamChat'
import { formatMoney } from '../../utils/format'

const props = defineProps({
  ids: { type: Array, default: () => [] },
})

const ds = ref([])
const dangTai = ref(true)

const theoId = computed(() => new Map(ds.value.map((p) => [Number(p.id), p])))
const cacThe = computed(() => props.ids.map((id) => theoId.value.get(Number(id))).filter(Boolean))

const route = useRoute()
const router = useRouter()
const trongCuaHang = computed(() => String(route?.path || '').startsWith('/shop'))

function mo(p) {
  const duongDan = `/shop/san-pham/${p.id}`
  if (trongCuaHang.value) router.push(duongDan)
  else window.open(duongDan, '_blank', 'noopener')
}

onMounted(async () => {
  try {
    ds.value = await taiSanPhamChat()
  } catch {
    ds.value = [] // không tải được: không hiện thẻ, chữ trong tin vẫn còn
  } finally {
    dangTai.value = false
  }
})
</script>

<template>
  <div v-if="ids.length" class="tspc" data-testid="the-san-pham-chat">
    <div v-if="dangTai && !cacThe.length" class="tspc-tai">Đang tải sản phẩm…</div>
    <button v-for="p in cacThe" :key="p.id" type="button" class="tspc-the" :title="`Xem ${p.ten}`" @click="mo(p)">
      <span class="tspc-anh">
        <img v-if="p.anhChinh" :src="p.anhChinh" :alt="p.ten" loading="lazy" />
        <i v-else class="bi bi-image" aria-hidden="true"></i>
        <span v-if="Math.round(Number(p.maxDiscountPercent) || 0) > 0" class="tspc-giam">-{{ Math.round(Number(p.maxDiscountPercent)) }}%</span>
      </span>
      <span class="tspc-than">
        <span class="tspc-ten">{{ p.ten }}</span>
        <span class="tspc-gia">
          <strong :class="{ 'is-giam': Number(p.originalPrice) > Number(p.minPrice) }">{{ Number(p.minPrice) > 0 ? formatMoney(p.minPrice) : 'Liên hệ' }}</strong>
          <s v-if="Number(p.originalPrice) > Number(p.minPrice)">{{ formatMoney(p.originalPrice) }}</s>
        </span>
        <span class="tspc-xem">Xem chi tiết <i class="bi bi-chevron-right" aria-hidden="true"></i></span>
      </span>
    </button>
  </div>
</template>

<style scoped>
.tspc {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 6px;
  max-width: 280px;
}
.tspc-tai {
  font-size: 0.8rem;
  color: var(--fs-muted, #64748b);
}
.tspc-the {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 8px;
  text-align: left;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.tspc-the:hover {
  border-color: #93c5fd;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}
.tspc-anh {
  position: relative;
  display: grid;
  flex: 0 0 56px;
  place-items: center;
  width: 56px;
  height: 56px;
  padding: 4px;
  overflow: hidden;
  color: #cbd5e1;
  background: #f3f4f6;
  border-radius: 10px;
}
.tspc-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  mix-blend-mode: multiply;
}
.tspc-giam {
  position: absolute;
  top: 2px;
  left: 2px;
  padding: 0 5px;
  font-size: 0.62rem;
  font-weight: 700;
  color: #fff;
  background: #e11d48;
  border-radius: 999px;
}
.tspc-than {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}
.tspc-ten {
  display: -webkit-box;
  overflow: hidden;
  font-size: 0.84rem;
  font-weight: 600;
  line-height: 1.3;
  color: #0f172a;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.tspc-gia {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 6px;
  align-items: baseline;
  font-size: 0.82rem;
}
.tspc-gia strong {
  color: #0f172a;
}
.tspc-gia strong.is-giam {
  color: #e11d48;
}
.tspc-gia s {
  font-size: 0.74rem;
  color: #94a3b8;
}
.tspc-xem {
  font-size: 0.74rem;
  font-weight: 600;
  color: #1d4ed8;
}
</style>
