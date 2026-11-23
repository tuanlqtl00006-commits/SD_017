<script setup>
// Trang "Gợi ý" (góp ý 23/11 Task 3.1): bộ câu hỏi trắc nghiệm từng bước (trình độ, lối chơi, ngân sách, trọng lượng, thương hiệu),
// có thanh tiến trình và nút Quay lại / Tiếp tục; hết câu hỏi thì lọc sản phẩm đang bán và đề xuất các mẫu phù hợp nhất.
// Thay cho bản "gợi ý AI từ ảnh" cũ (làm sai hướng). Logic chấm điểm ở utils/goiYTracNghiem.js.
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import TheSanPham from '../../components/shop/TheSanPham.vue'
import { CAU_HOI, dsThuongHieu, goiYSanPham } from '../../utils/goiYTracNghiem'

const sanPhams = ref([])
const dangTai = ref(true)
const loiTai = ref('')
const buoc = ref(0) // 0..CAU_HOI.length-1 là câu hỏi, = CAU_HOI.length là kết quả
const traLoi = reactive({ trinhDo: '', loiChoi: '', nganSach: '', trongLuong: '', thuongHieu: [] })

const cauHoi = computed(() => {
  const c = CAU_HOI[buoc.value]
  if (!c || c.khoa !== 'thuongHieu') return c
  return { ...c, luaChon: dsThuongHieu(sanPhams.value).map((t) => ({ gt: t, nhan: t, icon: 'bi-award' })) }
})
const xongCauHoi = computed(() => buoc.value >= CAU_HOI.length)
const tienTrinh = computed(() => Math.round((Math.min(buoc.value, CAU_HOI.length) / CAU_HOI.length) * 100))
const daChon = (gt) => (cauHoi.value?.nhieu ? traLoi[cauHoi.value.khoa].includes(gt) : traLoi[cauHoi.value?.khoa] === gt)
// Câu chọn nhiều (thương hiệu) được bỏ trống = không quan trọng
const choTiep = computed(() => !cauHoi.value || cauHoi.value.nhieu || !!traLoi[cauHoi.value.khoa])

function chon(gt) {
  const c = cauHoi.value
  if (c.nhieu) {
    const ds = traLoi[c.khoa]
    const i = ds.indexOf(gt)
    if (i >= 0) ds.splice(i, 1)
    else ds.push(gt)
    return
  }
  traLoi[c.khoa] = gt
}
function tiep() {
  if (choTiep.value) buoc.value++
}
function lui() {
  if (buoc.value > 0) buoc.value--
}
function lamLai() {
  Object.assign(traLoi, { trinhDo: '', loiChoi: '', nganSach: '', trongLuong: '', thuongHieu: [] })
  buoc.value = 0
}

const ketQua = computed(() => (xongCauHoi.value ? goiYSanPham(sanPhams.value, traLoi) : { ds: [], ngoaiNganSach: false }))
// Tóm tắt lựa chọn ở trang kết quả
const tomTat = computed(() =>
  CAU_HOI.map((c) => {
    if (c.nhieu) return traLoi[c.khoa].length ? traLoi[c.khoa].join(', ') : 'Hãng nào cũng được'
    return c.luaChon.find((x) => x.gt === traLoi[c.khoa])?.nhan || ''
  }).filter(Boolean),
)
const theSp = (k) => ({
  id: k.sp.id,
  ten: k.sp.tenSanPham,
  brandName: k.sp.tenThuongHieu,
  anhChinh: k.sp.anhChinh,
  minPrice: k.gia,
  originalPrice: Number(k.sp.phanTramGiamMax) > 0 ? k.sp.giaGocMin : k.gia,
  maxDiscountPercent: k.sp.phanTramGiamMax,
})

onMounted(async () => {
  try {
    const ds = (await api.get('/cua-hang/san-pham')).data
    sanPhams.value = Array.isArray(ds) ? ds : []
  } catch (e) {
    loiTai.value = e?.message || 'Không tải được danh sách sản phẩm.'
  } finally {
    dangTai.value = false
  }
})
</script>

<template>
  <div class="gy py-4" data-testid="goi-y">
    <div class="container">
      <div class="gy-dau">
        <h1 class="gy-tieu-de"><i class="bi bi-stars" aria-hidden="true"></i> Gợi ý vợt cho bạn</h1>
        <p class="gy-mo-ta">Trả lời vài câu hỏi ngắn, cửa hàng gợi ý những mẫu đang bán phù hợp nhất.</p>
      </div>

      <div class="gy-the">
        <!-- Thanh tiến trình -->
        <div class="gy-tien-trinh" aria-label="Tiến trình">
          <div class="gy-tien-trinh-chu">
            <span v-if="!xongCauHoi" data-testid="gy-buoc">Câu {{ buoc + 1 }} / {{ CAU_HOI.length }}</span>
            <span v-else>Hoàn tất</span>
            <span>{{ tienTrinh }}%</span>
          </div>
          <div class="gy-thanh" role="progressbar" :aria-valuenow="tienTrinh" aria-valuemin="0" aria-valuemax="100">
            <div class="gy-thanh-chay" :style="{ width: tienTrinh + '%' }"></div>
          </div>
          <ol class="gy-cac-buoc">
            <li v-for="(c, i) in CAU_HOI" :key="c.khoa" :class="{ 'is-xong': i < buoc, 'is-dang': i === buoc }">
              <span class="gy-so">{{ i < buoc ? '✓' : i + 1 }}</span>
            </li>
          </ol>
        </div>

        <div v-if="dangTai" class="gy-cho">Đang tải sản phẩm…</div>
        <div v-else-if="loiTai" class="alert alert-danger mb-0">{{ loiTai }}</div>

        <!-- Câu hỏi -->
        <template v-else-if="!xongCauHoi">
          <h2 class="gy-hoi" data-testid="gy-cau-hoi">{{ cauHoi.hoi }}</h2>
          <p class="gy-hoi-phu">{{ cauHoi.moTa }}</p>
          <div class="gy-lua-chon" :class="{ 'is-nhieu': cauHoi.nhieu }">
            <button
              v-for="l in cauHoi.luaChon"
              :key="l.gt"
              type="button"
              class="gy-chon"
              :class="{ 'is-chon': daChon(l.gt) }"
              :aria-pressed="daChon(l.gt)"
              data-testid="gy-lua-chon"
              @click="chon(l.gt)"
            >
              <i class="bi" :class="l.icon" aria-hidden="true"></i>
              <span class="gy-chon-chu">
                <strong>{{ l.nhan }}</strong>
                <small v-if="l.phu">{{ l.phu }}</small>
              </span>
              <i v-if="daChon(l.gt)" class="bi bi-check-circle-fill gy-chon-dau" aria-hidden="true"></i>
            </button>
          </div>
          <div class="gy-nut">
            <button type="button" class="btn btn-outline-secondary rounded-pill px-4" :disabled="buoc === 0" data-testid="gy-quay-lai" @click="lui">
              <i class="bi bi-arrow-left me-1"></i> Quay lại
            </button>
            <button type="button" class="btn btn-primary rounded-pill px-4" :disabled="!choTiep" data-testid="gy-tiep" @click="tiep">
              {{ buoc === CAU_HOI.length - 1 ? 'Xem gợi ý' : 'Tiếp tục' }} <i class="bi bi-arrow-right ms-1"></i>
            </button>
          </div>
        </template>

        <!-- Kết quả -->
        <template v-else>
          <div class="gy-tom-tat">
            <span v-for="t in tomTat" :key="t" class="gy-chip">{{ t }}</span>
          </div>
          <p v-if="ketQua.ngoaiNganSach && ketQua.ds.length" class="alert alert-warning small" data-testid="gy-ngoai-ngan-sach">
            Chưa có mẫu còn hàng trong ngân sách bạn chọn. Dưới đây là các mẫu có giá gần nhất.
          </p>
          <p v-if="!ketQua.ds.length" class="gy-cho" data-testid="gy-khong-co">Hiện cửa hàng chưa có sản phẩm còn hàng phù hợp.</p>
          <div v-else class="gy-ket-qua" data-testid="gy-ket-qua">
            <div v-for="k in ketQua.ds" :key="k.sp.id" class="gy-mau">
              <TheSanPham :sp="theSp(k)" />
              <ul v-if="k.lyDo.length" class="gy-ly-do">
                <li v-for="l in k.lyDo" :key="l"><i class="bi bi-check2" aria-hidden="true"></i> {{ l }}</li>
              </ul>
            </div>
          </div>
          <div class="gy-nut">
            <button type="button" class="btn btn-outline-secondary rounded-pill px-4" data-testid="gy-sua" @click="lui">
              <i class="bi bi-arrow-left me-1"></i> Sửa lựa chọn
            </button>
            <button type="button" class="btn btn-primary rounded-pill px-4" data-testid="gy-lam-lai" @click="lamLai">
              <i class="bi bi-arrow-counterclockwise me-1"></i> Làm lại
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gy-dau {
  margin-bottom: 18px;
  text-align: center;
}
.gy-tieu-de {
  font-size: 1.6rem;
  font-weight: 800;
  color: #0f172a;
}
.gy-tieu-de .bi {
  color: #2563eb;
}
.gy-mo-ta {
  margin: 4px 0 0;
  color: #64748b;
}
.gy-the {
  max-width: 920px;
  margin: 0 auto;
  padding: 24px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}
.gy-tien-trinh {
  margin-bottom: 22px;
}
.gy-tien-trinh-chu {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
}
.gy-thanh {
  height: 8px;
  overflow: hidden;
  background: #e2e8f0;
  border-radius: 999px;
}
.gy-thanh-chay {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
  border-radius: 999px;
  transition: width 0.25s ease;
}
.gy-cac-buoc {
  display: flex;
  justify-content: space-between;
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
}
.gy-so {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  font-size: 0.78rem;
  font-weight: 700;
  color: #94a3b8;
  background: #fff;
  border: 2px solid #cbd5e1;
  border-radius: 50%;
}
.is-xong .gy-so {
  color: #fff;
  background: #16a34a;
  border-color: #16a34a;
}
.is-dang .gy-so {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}
.gy-hoi {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}
.gy-hoi-phu {
  margin: 4px 0 16px;
  color: #64748b;
}
.gy-lua-chon {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px;
}
.gy-chon {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  text-align: left;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.gy-chon > .bi:first-child {
  font-size: 1.4rem;
  color: #2563eb;
}
.gy-chon:hover {
  border-color: #93c5fd;
}
.gy-chon.is-chon {
  background: #eff6ff;
  border-color: #2563eb;
}
.gy-chon-chu {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.gy-chon-chu small {
  color: #64748b;
}
.gy-chon-dau {
  position: absolute;
  top: 8px;
  right: 10px;
  color: #2563eb;
}
.gy-nut {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 22px;
}
.gy-cho {
  padding: 30px 0;
  text-align: center;
  color: #64748b;
}
.gy-tom-tat {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.gy-chip {
  padding: 4px 12px;
  font-size: 0.82rem;
  font-weight: 600;
  color: #1d4ed8;
  background: #eff6ff;
  border-radius: 999px;
}
.gy-ket-qua {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.gy-ly-do {
  margin: 8px 0 0;
  padding: 0;
  font-size: 0.8rem;
  color: #15803d;
  list-style: none;
}
@media (max-width: 576px) {
  .gy-the {
    padding: 16px;
  }
  .gy-nut .btn {
    flex: 1;
  }
}
</style>
