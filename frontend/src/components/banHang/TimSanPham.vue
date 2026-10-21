<script setup>
// Tìm sản phẩm để thêm vào hóa đơn: gõ tên / mã / thương hiệu / màu (không cần dấu), hoặc quét mã:
//  - máy quét mã vạch / QR cầm tay: quét vào ô "Quét mã" (máy tự gõ mã + Enter);
//  - camera: nút "Quét bằng camera".
// Giá hiện đã trừ đợt giảm giá đang chạy; sản phẩm hết hàng không chọn được.
import { onMounted, ref, watch } from 'vue'
import api from '../../services/api'
import QuetMaModal from './QuetMaModal.vue'
import { formatMoney } from '../../utils/format'
import { formatPhanTram } from '../../utils/giamGia'

const emit = defineEmits(['chon'])
const tuKhoa = ref('')
const ma = ref('')
const ds = ref([])
const dangTai = ref(false)
const loi = ref('')
const moCamera = ref(false)
const oTim = ref(null)
const oMa = ref(null)
let hen = null

async function tai() {
  dangTai.value = true
  loi.value = ''
  try {
    ds.value = (await api.get('/ban-hang/san-pham', { params: { q: tuKhoa.value.trim() || undefined, gioiHan: 40 } })).data
  } catch (e) {
    loi.value = e.message || 'Không tải được sản phẩm.'
  } finally {
    dangTai.value = false
  }
}
onMounted(tai)
watch(tuKhoa, () => {
  clearTimeout(hen)
  hen = setTimeout(tai, 300)
})

async function quetMa(giaTri) {
  const m = String(giaTri ?? ma.value).trim()
  if (!m) return
  try {
    const sp = (await api.get(`/ban-hang/san-pham/ma/${encodeURIComponent(m)}`)).data
    emit('chon', sp)
    ma.value = ''
  } catch (e) {
    emit('chon', null, e.message || `Không tìm thấy sản phẩm có mã ${m}.`)
  }
  oMa.value?.focus()
}

function onMaCamera(giaTri) {
  moCamera.value = false
  quetMa(giaTri)
}

/** Sau khi thanh toán xong: tải lại để tồn kho mới nhất. */
defineExpose({ taiLai: tai, focusTim: () => oTim.value?.focus() })
</script>

<template>
  <section class="ad-card bh-tim">
    <div class="bh-tim-hang">
      <div class="ad-affix flex-grow-1">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input ref="oTim" v-model="tuKhoa" type="search" class="form-control ad-control" placeholder="Tìm sản phẩm theo tên, mã, thương hiệu, màu… (F2)" aria-label="Tìm sản phẩm" autocomplete="off" />
      </div>
      <form class="ad-affix bh-o-ma" @submit.prevent="quetMa()">
        <i class="bi bi-upc-scan" aria-hidden="true"></i>
        <input ref="oMa" v-model="ma" type="text" class="form-control ad-control" placeholder="Quét / nhập mã + Enter" aria-label="Quét mã sản phẩm" autocomplete="off" />
      </form>
      <button type="button" class="ad-btn" @click="moCamera = true"><i class="bi bi-camera" aria-hidden="true"></i> Quét bằng camera</button>
    </div>

    <p v-if="loi" class="ad-error px-1">{{ loi }}</p>
    <div class="bh-luoi" :class="{ 'is-tai': dangTai }">
      <p v-if="!dangTai && !ds.length" class="ad-empty w-100">Không tìm thấy sản phẩm phù hợp.</p>
      <button
        v-for="sp in ds"
        :key="sp.id"
        type="button"
        class="bh-sp"
        :disabled="sp.tonKho <= 0"
        :title="sp.tonKho <= 0 ? 'Hết hàng' : `Thêm ${sp.tenSanPham} vào hóa đơn`"
        @click="emit('chon', sp)"
      >
        <span class="bh-sp-anh">
          <img v-if="sp.anh" :src="sp.anh" :alt="sp.tenSanPham" loading="lazy" />
          <i v-else class="bi bi-image" aria-hidden="true"></i>
          <span v-if="sp.phanTramGiam > 0" class="bh-sp-giam">-{{ formatPhanTram(sp.phanTramGiam) }}</span>
        </span>
        <span class="bh-sp-ten">{{ sp.tenSanPham }}</span>
        <span class="bh-sp-loai">{{ sp.phanLoai || sp.ma }}</span>
        <span class="bh-sp-gia">
          <b>{{ formatMoney(sp.donGia) }}</b>
          <s v-if="sp.donGia < sp.giaGoc">{{ formatMoney(sp.giaGoc) }}</s>
        </span>
        <span class="bh-sp-ton" :class="{ 'is-het': sp.tonKho <= 0, 'is-it': sp.tonKho > 0 && sp.tonKho <= 5 }">
          {{ sp.tonKho <= 0 ? 'Hết hàng' : `Còn ${sp.tonKho}` }}
        </span>
      </button>
    </div>
  </section>
  <QuetMaModal v-if="moCamera" @close="moCamera = false" @ma="onMaCamera" />
</template>

<style scoped>
.bh-tim {
  padding: 1rem;
}
.bh-tim-hang {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}
.bh-o-ma {
  width: 230px;
}
.bh-luoi {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 0.6rem;
  max-height: 360px;
  overflow: auto;
  padding: 0.1rem;
}
.bh-luoi.is-tai {
  opacity: 0.6;
}
.bh-sp {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  text-align: left;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  background: #fff;
  padding: 0.5rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.bh-sp:not(:disabled):hover {
  border-color: var(--fs-primary, #0977ec);
  box-shadow: 0 4px 14px rgba(9, 119, 236, 0.12);
}
.bh-sp:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.bh-sp-anh {
  position: relative;
  height: 92px;
  border-radius: 8px;
  background: var(--fs-surface, #f7f9fc);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #c3ccd7;
  font-size: 1.6rem;
}
.bh-sp-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.bh-sp-giam {
  position: absolute;
  top: 4px;
  left: 4px;
  background: #e5484d;
  color: #fff;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0 0.3rem;
}
.bh-sp-ten {
  font-weight: 600;
  font-size: 0.88rem;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.bh-sp-loai {
  font-size: 0.78rem;
  color: var(--fs-muted, #7b8794);
}
.bh-sp-gia {
  display: flex;
  gap: 0.35rem;
  align-items: baseline;
  flex-wrap: wrap;
  font-size: 0.88rem;
  color: var(--fs-primary-dark, #075fc0);
}
.bh-sp-gia s {
  color: var(--fs-muted, #7b8794);
  font-size: 0.75rem;
}
.bh-sp-ton {
  font-size: 0.75rem;
  color: #146c43;
}
.bh-sp-ton.is-it {
  color: #b45309;
}
.bh-sp-ton.is-het {
  color: #b42318;
}
</style>
