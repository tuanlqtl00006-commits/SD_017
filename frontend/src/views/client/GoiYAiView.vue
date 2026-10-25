<script setup>
// Gợi ý AI từ ảnh: khách tải ảnh (vợt, áo, giày... thấy ở đâu đó) -> trình duyệt phân tích màu / hình -> gợi ý sản phẩm giống nhất đang bán.
// Ảnh chỉ xử lý trên máy của khách (utils/goiYAnh.js), không gửi lên máy chủ.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../../services/api'
import { formatMoney } from '../../utils/format'
import { docDacTrung, xepHangGoiY } from '../../utils/goiYAnh'

const KICH_THUOC = 64
const DUNG_LUONG_TOI_DA = 10 * 1024 * 1024

const sanPhams = ref([])
const dacTrungSanPham = ref({})
const dangTaiSanPham = ref(true)
const loiTai = ref('')

const anhXem = ref('')
const dacTrungKhach = ref(null)
const dangPhanTich = ref(false)
const loiAnh = ref('')
const daPhanTich = ref(false)
const danhMuc = ref('')
const keoTha = ref(false)

const dsDanhMuc = computed(() => [...new Set(sanPhams.value.map((s) => s.tenDanhMuc).filter(Boolean))])
const ketQua = computed(() => {
  const ds = danhMuc.value ? sanPhams.value.filter((s) => s.tenDanhMuc === danhMuc.value) : sanPhams.value
  return xepHangGoiY(dacTrungKhach.value, ds, dacTrungSanPham.value)
})

function docAnh(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      try {
        const c = document.createElement('canvas')
        c.width = KICH_THUOC
        c.height = KICH_THUOC
        const ctx = c.getContext('2d', { willReadFrequently: true })
        const ti = Math.min(KICH_THUOC / (img.naturalWidth || KICH_THUOC), KICH_THUOC / (img.naturalHeight || KICH_THUOC))
        const w = Math.max(1, Math.round((img.naturalWidth || KICH_THUOC) * ti))
        const h = Math.max(1, Math.round((img.naturalHeight || KICH_THUOC) * ti))
        ctx.drawImage(img, (KICH_THUOC - w) / 2, (KICH_THUOC - h) / 2, w, h)
        resolve(docDacTrung(ctx.getImageData(0, 0, KICH_THUOC, KICH_THUOC).data, KICH_THUOC, KICH_THUOC))
      } catch (e) {
        reject(e)
      }
    }
    img.onerror = () => reject(new Error('Không đọc được ảnh.'))
    img.src = src
  })
}

onMounted(async () => {
  try {
    sanPhams.value = (await api.get('/cua-hang/san-pham')).data || []
    const kq = {}
    await Promise.all(
      sanPhams.value.map(async (sp) => {
        kq[sp.id] = sp.anhChinh ? await docAnh(sp.anhChinh).catch(() => null) : null
      }),
    )
    dacTrungSanPham.value = kq
  } catch (e) {
    loiTai.value = e.message || 'Không tải được danh sách sản phẩm.'
  } finally {
    dangTaiSanPham.value = false
  }
})

let urlCu = ''
onBeforeUnmount(() => urlCu && URL.revokeObjectURL(urlCu))

async function chonAnh(file) {
  loiAnh.value = ''
  if (!file) return
  if (!/^image\/(jpeg|png|webp|gif|bmp)$/.test(file.type)) {
    loiAnh.value = 'Chỉ nhận ảnh JPG, PNG, WEBP, GIF hoặc BMP.'
    return
  }
  if (file.size > DUNG_LUONG_TOI_DA) {
    loiAnh.value = 'Ảnh quá lớn (tối đa 10 MB).'
    return
  }
  if (urlCu) URL.revokeObjectURL(urlCu)
  urlCu = URL.createObjectURL(file)
  anhXem.value = urlCu
  dangPhanTich.value = true
  daPhanTich.value = false
  try {
    dacTrungKhach.value = await docAnh(urlCu)
    if (!dacTrungKhach.value) loiAnh.value = 'Ảnh gần như chỉ có nền trơn, không nhận ra sản phẩm. Hãy chọn ảnh rõ sản phẩm hơn.'
  } catch (e) {
    dacTrungKhach.value = null
    loiAnh.value = e.message || 'Không phân tích được ảnh.'
  } finally {
    dangPhanTich.value = false
    daPhanTich.value = true
  }
}

function khiChon(e) {
  chonAnh(e.target.files?.[0])
  e.target.value = ''
}
function khiTha(e) {
  keoTha.value = false
  chonAnh(e.dataTransfer?.files?.[0])
}
</script>

<template>
  <div class="container py-4 gy-ai">
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb small mb-3">
        <li class="breadcrumb-item"><RouterLink to="/shop" class="text-decoration-none text-muted">Trang chủ</RouterLink></li>
        <li class="breadcrumb-item active" aria-current="page">Gợi ý AI</li>
      </ol>
    </nav>

    <div class="row g-4">
      <div class="col-lg-4">
        <section class="gy-the">
          <h1 class="gy-tieu-de"><i class="bi bi-stars" aria-hidden="true"></i> Gợi ý AI từ hình ảnh</h1>
          <p class="text-muted small mb-3">Tải ảnh cây vợt, đôi giày, chiếc áo... bạn thích. AI sẽ phân tích màu sắc và gợi ý sản phẩm giống nhất đang có tại FootStyle.</p>

          <label
            class="gy-tha"
            :class="{ 'is-keo': keoTha, 'co-anh': anhXem }"
            data-testid="goi-y-chon-anh"
            @dragover.prevent="keoTha = true"
            @dragleave="keoTha = false"
            @drop.prevent="khiTha"
          >
            <input type="file" accept="image/jpeg,image/png,image/webp,image/gif,image/bmp" class="visually-hidden" aria-label="Chọn ảnh để AI gợi ý" @change="khiChon" />
            <img v-if="anhXem" :src="anhXem" alt="Ảnh bạn đã chọn" />
            <span v-else class="gy-tha-chu">
              <i class="bi bi-cloud-arrow-up" aria-hidden="true"></i>
              <strong>Bấm để chọn ảnh</strong>
              <small>hoặc kéo thả ảnh vào đây (JPG, PNG, WEBP · tối đa 10 MB)</small>
            </span>
          </label>
          <p v-if="loiAnh" class="text-danger small mt-2 mb-0" role="alert">{{ loiAnh }}</p>

          <div v-if="dacTrungKhach?.mauNoiBat?.length" class="mt-3" data-testid="goi-y-mau">
            <div class="small fw-semibold mb-2">Màu nổi bật AI nhận ra</div>
            <div class="d-flex flex-wrap gap-2">
              <span v-for="m in dacTrungKhach.mauNoiBat" :key="m.ten" class="gy-mau">
                <span class="gy-cham" :style="{ background: `rgb(${m.rgb.join(',')})` }" aria-hidden="true"></span>
                {{ m.ten }} {{ Math.round(m.tiLe * 100) }}%
              </span>
            </div>
          </div>

          <div v-if="dsDanhMuc.length" class="mt-3">
            <label class="small fw-semibold mb-1" for="gy-danh-muc">Chỉ tìm trong</label>
            <select id="gy-danh-muc" v-model="danhMuc" class="form-select form-select-sm">
              <option value="">Tất cả loại sản phẩm</option>
              <option v-for="d in dsDanhMuc" :key="d" :value="d">{{ d }}</option>
            </select>
          </div>
          <p class="gy-ghi-chu mt-3 mb-0"><i class="bi bi-shield-check" aria-hidden="true"></i> Ảnh chỉ được xử lý trên trình duyệt của bạn, không tải lên máy chủ.</p>
        </section>
      </div>

      <div class="col-lg-8">
        <section class="gy-the h-100">
          <h2 class="gy-tieu-de-2">Gợi ý từ AI</h2>
          <div v-if="loiTai" class="alert alert-warning small">{{ loiTai }}</div>
          <div v-else-if="dangTaiSanPham || dangPhanTich" class="gy-trong"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span> {{ dangPhanTich ? 'AI đang phân tích ảnh…' : 'Đang chuẩn bị dữ liệu sản phẩm…' }}</div>
          <div v-else-if="!daPhanTich" class="gy-trong">
            <i class="bi bi-image" aria-hidden="true"></i>
            <span>Hãy tải lên một bức ảnh để AI gợi ý sản phẩm phù hợp.</span>
          </div>
          <div v-else-if="!ketQua.length" class="gy-trong" data-testid="goi-y-trong">
            <i class="bi bi-search" aria-hidden="true"></i>
            <span>Chưa tìm thấy sản phẩm đủ giống. Bạn thử ảnh khác hoặc xem <RouterLink to="/shop/san-pham">tất cả sản phẩm</RouterLink>.</span>
          </div>
          <div v-else class="row g-3" data-testid="goi-y-ket-qua">
            <div v-for="k in ketQua" :key="k.sp.id" class="col-sm-6 col-xl-4">
              <RouterLink :to="`/shop/san-pham/${k.sp.id}`" class="gy-sp text-decoration-none">
                <div class="gy-sp-anh"><img :src="k.sp.anhChinh || '/logo.png'" :alt="k.sp.tenSanPham" /></div>
                <div class="gy-sp-ten" :title="k.sp.tenSanPham">{{ k.sp.tenSanPham }}</div>
                <div class="small text-muted">{{ k.sp.tenThuongHieu }} · {{ k.sp.tenDanhMuc }}</div>
                <div class="gy-sp-gia">{{ formatMoney(k.sp.giaBanMin) }}</div>
                <div class="gy-do-giong" :aria-label="`Độ giống ${k.doGiong}%`">
                  <div class="gy-thanh"><span :style="{ width: k.doGiong + '%' }"></span></div>
                  <small>Giống {{ k.doGiong }}%<template v-if="k.mauKhop"> · có màu {{ k.mauKhop }}</template></small>
                </div>
              </RouterLink>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gy-the { background: #fff; border: 1px solid #e8ebf0; border-radius: 14px; padding: 1.25rem; }
.gy-tieu-de { font-size: 1.25rem; font-weight: 800; letter-spacing: normal; margin-bottom: 0.4rem; }
.gy-tieu-de .bi-stars { color: #f59e0b; }
.gy-tieu-de-2 { font-size: 1.05rem; font-weight: 700; margin-bottom: 1rem; }
.gy-tha { display: flex; align-items: center; justify-content: center; width: 100%; aspect-ratio: 4 / 3; border: 2px dashed #c9d3e0; border-radius: 12px; background: #f7f9fc; cursor: pointer; overflow: hidden; transition: border-color 0.2s, background 0.2s; }
.gy-tha:hover, .gy-tha.is-keo { border-color: var(--vnb-blue, #0977ec); background: #eef5ff; }
.gy-tha.co-anh { border-style: solid; }
.gy-tha img { width: 100%; height: 100%; object-fit: contain; }
.gy-tha-chu { display: flex; flex-direction: column; align-items: center; gap: 0.3rem; text-align: center; padding: 1rem; color: #5b6878; }
.gy-tha-chu .bi { font-size: 2.2rem; color: var(--vnb-blue, #0977ec); }
.gy-mau { display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.25rem 0.6rem; border: 1px solid #e3e8ef; border-radius: 999px; font-size: 0.8rem; }
.gy-cham { width: 14px; height: 14px; border-radius: 50%; border: 1px solid rgba(0, 0, 0, 0.15); }
.gy-ghi-chu { font-size: 0.78rem; color: #6b7785; }
.gy-trong { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.5rem; min-height: 260px; color: #6b7785; text-align: center; }
.gy-trong .bi { font-size: 2rem; }
.gy-sp { display: block; height: 100%; padding: 0.75rem; border: 1px solid #e8ebf0; border-radius: 12px; color: inherit; transition: box-shadow 0.2s, transform 0.2s; }
.gy-sp:hover { box-shadow: 0 8px 22px rgba(0, 0, 0, 0.08); transform: translateY(-2px); }
.gy-sp-anh { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; background: #f7f9fc; border-radius: 8px; margin-bottom: 0.5rem; }
.gy-sp-anh img { max-width: 85%; max-height: 85%; object-fit: contain; }
.gy-sp-ten { font-weight: 600; font-size: 0.9rem; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 2.6em; }
.gy-sp-gia { color: #e95211; font-weight: 700; margin: 0.25rem 0; }
.gy-thanh { height: 6px; background: #edf0f4; border-radius: 999px; overflow: hidden; margin-bottom: 0.2rem; }
.gy-thanh span { display: block; height: 100%; background: linear-gradient(90deg, #22c55e, #0977ec); }
</style>
