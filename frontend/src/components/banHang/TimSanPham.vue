<script setup>
// Bảng chọn sản phẩm bán tại quầy (cô: dạng BẢNG có phân trang, sản phẩm mới nhất trước, bộ lọc giống trang sản phẩm + lọc khoảng giá).
//  - Chỉ hiện biến thể đang bán và CÒN HÀNG (hết hàng thì tự biến mất - kho đã trừ ngay khi thêm vào hóa đơn chờ).
//  - Nhập số lượng rồi bấm "Thêm"; số lượng lớn hơn tồn kho thì báo ngay, không gửi.
//  - Vẫn quét được mã: máy quét cầm tay (gõ mã + Enter vào ô "Quét mã") hoặc camera.
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import BasePagination from '../common/BasePagination.vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import QuetMaModal from './QuetMaModal.vue'
import { formatMoney } from '../../utils/format'
import { formatPhanTram } from '../../utils/giamGia'

// trongHop: hiện trong hộp thoại 'Chọn sản phẩm' của màn bán hàng (không có khung thẻ / tiêu đề / ô quét mã riêng)
const props = defineProps({ trongHop: { type: Boolean, default: false } })
const emit = defineEmits(['chon'])
const KICH_THUOC = 10

const loc = reactive({ q: '', idDanhMuc: '', idThuongHieu: '', idMauSac: '', idTrongLuong: '', idChuVi: '', giaTu: '', giaDen: '' })
const trang = ref(1)
const ds = ref([])
const tongSo = ref(0)
const boLoc = ref({ danhMuc: [], thuongHieu: [], mauSac: [], trongLuong: [], chuVi: [] })
const dangTai = ref(false)
const loi = ref('')
const soLuong = reactive({}) // id biến thể -> số lượng đang nhập
const loiDong = reactive({}) // id biến thể -> lỗi số lượng
const ma = ref('')
const moCamera = ref(false)
const oTim = ref(null)
const oMa = ref(null)
let hen = null

const loiGia = computed(() =>
  loc.giaTu !== '' && loc.giaDen !== '' && Number(loc.giaTu) > Number(loc.giaDen) ? 'Giá từ phải nhỏ hơn hoặc bằng giá đến.' : '',
)
const luaChon = (ds, tatCa) => [{ value: '', label: tatCa }, ...(ds || []).map((x) => ({ value: x.id, label: x.ten }))]
const coLoc = computed(() => Object.values(loc).some((v) => v !== ''))

async function tai() {
  if (loiGia.value) return
  dangTai.value = true
  loi.value = ''
  try {
    const params = { trang: trang.value - 1, kichThuoc: KICH_THUOC }
    for (const [k, v] of Object.entries(loc)) if (v !== '' && v !== null) params[k] = typeof v === 'string' ? v.trim() || undefined : v
    const res = (await api.get('/ban-hang/san-pham/loc', { params })).data
    ds.value = res.items || []
    tongSo.value = res.tongSo || 0
    if (res.boLoc) boLoc.value = res.boLoc
    if (res.trang !== undefined && res.trang + 1 !== trang.value) trang.value = res.trang + 1
  } catch (e) {
    loi.value = e.message || 'Không tải được sản phẩm.'
  } finally {
    dangTai.value = false
  }
}
onMounted(() => {
  tai()
  if (props.trongHop) setTimeout(() => oTim.value?.focus(), 50)
})
watch(
  () => ({ ...loc }),
  () => {
    clearTimeout(hen)
    hen = setTimeout(() => {
      trang.value = 1
      tai()
    }, 300)
  },
)
watch(trang, (v, cu) => {
  if (v !== cu) tai()
})

function xoaLoc() {
  Object.assign(loc, { q: '', idDanhMuc: '', idThuongHieu: '', idMauSac: '', idTrongLuong: '', idChuVi: '', giaTu: '', giaDen: '' })
}

function nhapSl(sp, v) {
  soLuong[sp.id] = v
  delete loiDong[sp.id]
}
function slCua(sp) {
  return soLuong[sp.id] ?? 1
}
function them(sp) {
  const raw = slCua(sp)
  const sl = Number(raw)
  if (!Number.isInteger(sl) || sl < 1) loiDong[sp.id] = 'Số lượng phải là số nguyên từ 1.'
  else if (sl > sp.tonKho) loiDong[sp.id] = `Chỉ còn ${sp.tonKho} sản phẩm trong kho.`
  else {
    delete loiDong[sp.id]
    emit('chon', sp, null, sl)
    soLuong[sp.id] = 1
    return
  }
}

async function quetMa(giaTri) {
  const m = String(giaTri ?? ma.value).trim()
  if (!m) return
  try {
    const sp = (await api.get(`/ban-hang/san-pham/ma/${encodeURIComponent(m)}`)).data
    emit('chon', sp, null, 1)
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

/** Sau khi thêm / bỏ sản phẩm, thanh toán: tải lại để tồn kho mới nhất. */
defineExpose({ taiLai: tai, focusTim: () => oTim.value?.focus() })
</script>

<template>
  <section class="bh-tim" :class="{ 'ad-card': !props.trongHop, 'is-trong-hop': props.trongHop }">
    <header v-if="!props.trongHop" class="ad-list-head">
      <div class="ad-card-head">
        <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-box-seam"></i></span>
        <h2 class="ad-card-title">Chọn sản phẩm</h2>
      </div>
      <div class="bh-tim-cong-cu">
        <form class="ad-affix bh-o-ma" @submit.prevent="quetMa()">
          <i class="bi bi-upc-scan" aria-hidden="true"></i>
          <input ref="oMa" v-model="ma" type="text" class="form-control ad-control" placeholder="Quét / nhập mã + Enter" aria-label="Quét mã sản phẩm" autocomplete="off" />
        </form>
        <button type="button" class="ad-btn" @click="moCamera = true"><i class="bi bi-camera" aria-hidden="true"></i> Camera</button>
      </div>
    </header>

    <div class="bh-loc">
      <div class="ad-affix bh-loc-tim">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input ref="oTim" v-model="loc.q" type="search" class="form-control ad-control" placeholder="Tên, mã, thương hiệu, màu… (F2)" aria-label="Tìm sản phẩm" autocomplete="off" />
      </div>
      <Select2 id="bh-loc-dm" v-model="loc.idDanhMuc" :options="luaChon(boLoc.danhMuc, 'Tất cả danh mục')" class="ad-control" aria-label="Lọc danh mục" />
      <Select2 id="bh-loc-th" v-model="loc.idThuongHieu" :options="luaChon(boLoc.thuongHieu, 'Tất cả thương hiệu')" class="ad-control" aria-label="Lọc thương hiệu" />
      <Select2 id="bh-loc-mau" v-model="loc.idMauSac" :options="luaChon(boLoc.mauSac, 'Tất cả màu')" class="ad-control" aria-label="Lọc màu sắc" />
      <Select2 id="bh-loc-tl" v-model="loc.idTrongLuong" :options="luaChon(boLoc.trongLuong, 'Mọi trọng lượng')" class="ad-control" aria-label="Lọc trọng lượng" />
      <Select2 id="bh-loc-cv" v-model="loc.idChuVi" :options="luaChon(boLoc.chuVi, 'Mọi chu vi cán')" class="ad-control" aria-label="Lọc chu vi cán" />
      <div class="bh-loc-gia">
        <MoneyInput id="bh-gia-tu" v-model="loc.giaTu" small aria-label="Giá từ" placeholder="Giá từ" :invalid="!!loiGia" />
        <span aria-hidden="true">–</span>
        <MoneyInput id="bh-gia-den" v-model="loc.giaDen" small aria-label="Giá đến" placeholder="Giá đến" :invalid="!!loiGia" />
      </div>
      <button type="button" class="ad-btn ad-btn-sm" :disabled="!coLoc" @click="xoaLoc"><i class="bi bi-x-circle" aria-hidden="true"></i> Xóa lọc</button>
    </div>
    <p v-if="loiGia" class="ad-error px-1 mb-2">{{ loiGia }}</p>
    <p v-if="loi" class="ad-error px-1">{{ loi }}</p>

    <div class="ad-table-wrap">
      <div class="ad-table-scroll">
        <table class="ad-table bh-bang-sp" :class="{ 'is-tai': dangTai }">
          <thead>
            <tr>
              <th class="ad-col-stt">STT</th>
              <th class="text-center">Ảnh</th>
              <th>Sản phẩm</th>
              <th>Phân loại</th>
              <th class="text-end">Giá bán</th>
              <th class="text-center">Tồn kho</th>
              <th class="text-center">Số lượng</th>
              <th class="ad-col-actions"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!dangTai && !ds.length">
              <td colspan="8"><div class="ad-empty">Không tìm thấy sản phẩm còn hàng phù hợp.</div></td>
            </tr>
            <tr v-for="(sp, i) in ds" :key="sp.id" class="bh-dong-chon">
              <td class="ad-col-stt">{{ (trang - 1) * KICH_THUOC + i + 1 }}</td>
              <td class="text-center">
                <span class="bh-anh-nho">
                  <img v-if="sp.anh" :src="sp.anh" :alt="sp.tenSanPham" />
                  <i v-else class="bi bi-image" aria-hidden="true"></i>
                </span>
              </td>
              <td>
                <span class="fw-semibold d-block">{{ sp.tenSanPham }}</span>
                <small class="text-muted"><span class="ad-code">{{ sp.ma }}</span><template v-if="sp.danhMuc"> · {{ sp.danhMuc }}</template></small>
              </td>
              <td>{{ sp.phanLoai || '—' }}</td>
              <td class="text-end ad-nowrap">
                <b class="d-block">{{ formatMoney(sp.donGia) }}</b>
                <small v-if="sp.donGia < sp.giaGoc" class="text-muted"><s>{{ formatMoney(sp.giaGoc) }}</s> -{{ formatPhanTram(sp.phanTramGiam) }}</small>
              </td>
              <td class="text-center">
                <span class="ad-pill" :class="sp.tonKho <= 5 ? 'ad-pill-orange' : 'ad-pill-green'">{{ sp.tonKho }}</span>
              </td>
              <td class="text-center">
                <input
                  :value="slCua(sp)"
                  type="number"
                  min="1"
                  :max="sp.tonKho"
                  class="form-control ad-control bh-o-sl"
                  :class="{ 'is-invalid': loiDong[sp.id] }"
                  :aria-label="`Số lượng ${sp.tenSanPham}`"
                  @input="nhapSl(sp, $event.target.value)"
                  @keydown.enter.prevent="them(sp)"
                />
                <small v-if="loiDong[sp.id]" class="ad-error d-block">{{ loiDong[sp.id] }}</small>
              </td>
              <td class="ad-col-actions">
                <button type="button" class="ad-btn ad-btn-primary ad-btn-sm bh-nut-them" :aria-label="`Thêm ${sp.tenSanPham} vào hóa đơn`" @click="them(sp)">
                  <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="bh-chan-bang">
      <span class="ad-count">{{ tongSo }} sản phẩm còn hàng</span>
      <BasePagination v-if="tongSo > KICH_THUOC" v-model:page="trang" :page-size="KICH_THUOC" :total="tongSo" />
    </div>
  </section>
  <QuetMaModal v-if="moCamera" @close="moCamera = false" @ma="onMaCamera" />
</template>

<style scoped>
.bh-tim {
  padding: 1rem;
}
.bh-tim.is-trong-hop {
  padding: 0;
}
.bh-tim.is-trong-hop .bh-loc {
  margin-top: 0;
}
.bh-tim-cong-cu {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.bh-o-ma {
  width: 220px;
}
.bh-loc {
  display: grid;
  grid-template-columns: minmax(200px, 2fr) repeat(5, minmax(130px, 1fr));
  gap: 0.5rem;
  margin: 0.75rem 0 0.5rem;
}
.bh-loc-gia {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  grid-column: span 2;
}
.bh-loc > .ad-btn {
  justify-self: start;
}
.bh-bang-sp.is-tai {
  opacity: 0.6;
}
.bh-bang-sp td {
  vertical-align: middle;
}
.bh-anh-nho {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #f1f5f9;
  display: inline-grid;
  place-items: center;
  overflow: hidden;
  color: #94a3b8;
}
.bh-anh-nho img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bh-o-sl {
  width: 76px;
  margin: 0 auto;
  text-align: center;
}
.bh-chan-bang {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 0.5rem;
}
@media (max-width: 1400px) {
  .bh-loc {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .bh-loc-tim {
    grid-column: span 3;
  }
  .bh-loc-gia {
    grid-column: span 2;
  }
}
@media (max-width: 640px) {
  .bh-loc {
    grid-template-columns: 1fr;
  }
  .bh-loc-tim,
  .bh-loc-gia {
    grid-column: auto;
  }
}
</style>
