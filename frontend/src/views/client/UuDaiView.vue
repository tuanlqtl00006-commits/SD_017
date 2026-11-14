<script setup>
// Trang Ưu đãi (cô review 14/11: trang cũ giống hệt màn sản phẩm). Chỉ hiện các chương trình ĐANG có hiệu lực, dạng thẻ:
//  - Phiếu giảm giá: mã công khai; khách đã đăng nhập thì thêm cả phiếu tặng riêng cho mình. Bấm "Sao chép mã" để dán ở trang thanh toán.
//  - Đợt giảm giá đang diễn ra: bấm vào để xem đúng các sản phẩm của đợt đó (màn Sản phẩm, ?dot=<id>).
// Danh sách sản phẩm đang giảm giá nằm ở màn Sản phẩm (bộ lọc "Đang giảm giá").
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import { useAuth } from '../../composables/useAuth'
import { useAutoRefresh } from '../../composables/useAutoRefresh'
import { formatMoney, formatNgayGio } from '../../utils/format'

const auth = useAuth()
const dangTai = ref(true)
const phieu = ref([])
const dot = ref([])
const maVuaChep = ref('')
let henChep = null

// Số tiền rất lớn để bỏ qua điều kiện "đơn tối thiểu" khi hỏi máy chủ: còn lại chỉ những phiếu đang dùng được (chưa hết hạn, còn lượt)
const TONG_TIEN_LON = 1_000_000_000_000

async function taiPhieu() {
  if (auth.laKhachHang.value) {
    try {
      const ds = (await api.get('/khach/phieu-giam-gia', { params: { tongTien: TONG_TIEN_LON } })).data
      return (Array.isArray(ds) ? ds : []).filter((p) => p.dungDuoc)
    } catch {
      // phiên hết hạn...: rơi xuống lấy phiếu công khai
    }
  }
  const ds = (await api.get('/cua-hang/phieu-giam-gia')).data
  return (Array.isArray(ds) ? ds : []).map((p) => ({
    ma: p.maPhieu,
    ten: p.tenPhieu,
    caNhan: false,
    loaiGiam: p.hinhThucGiam,
    giaTriGiam: p.giaTriGiam,
    giamToiDa: p.giamToiDa,
    donToiThieu: p.donToiThieu,
    ngayKetThuc: p.ngayKetThuc,
  }))
}

async function tai() {
  const [p, d] = await Promise.allSettled([taiPhieu(), api.get('/cua-hang/dot-giam-gia')])
  if (p.status === 'fulfilled') phieu.value = p.value
  if (d.status === 'fulfilled') dot.value = Array.isArray(d.value?.data) ? d.value.data : []
  dangTai.value = false
}
onMounted(tai)
useAutoRefresh(tai, 3000, { chuKyMs: 60_000, chuDe: ['giam-gia', 'phieu-giam-gia', 'san-pham'] })

// Phiếu tặng riêng lên trước, rồi phiếu sắp hết hạn
const phieuSapXep = computed(() =>
  [...phieu.value].sort((a, b) => Number(!!b.caNhan) - Number(!!a.caNhan) || String(a.ngayKetThuc || '9').localeCompare(String(b.ngayKetThuc || '9'))),
)

const mucGiam = (p) => (Number(p.loaiGiam) === 1 ? `${Number(p.giaTriGiam) || 0}%` : formatMoney(p.giaTriGiam))

async function chepMa(ma) {
  try {
    await navigator.clipboard.writeText(ma)
  } catch {
    // trình duyệt không cho ghi bộ nhớ tạm (mở bằng http://IP): chép bằng ô nhập tạm
    const o = document.createElement('textarea')
    o.value = ma
    document.body.appendChild(o)
    o.select()
    document.execCommand('copy')
    o.remove()
  }
  maVuaChep.value = ma
  clearTimeout(henChep)
  henChep = setTimeout(() => (maVuaChep.value = ''), 2500)
}
</script>

<template>
  <div class="uu-dai-view py-4">
    <div class="vnb-container">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Ưu đãi</li>
        </ol>
      </nav>

      <header class="ud-dau rounded-4 mb-4">
        <div>
          <h1 class="ud-tieu-de">Ưu đãi đang diễn ra</h1>
          <p class="ud-phu mb-0">Phiếu giảm giá và các đợt giảm giá đang có hiệu lực tại FootStyle.</p>
        </div>
        <RouterLink :to="{ path: '/shop/san-pham', query: { khuyenMai: '1' } }" class="btn ud-nut-trang">
          Xem sản phẩm đang giảm giá <i class="bi bi-arrow-right ms-1"></i>
        </RouterLink>
      </header>

      <div v-if="dangTai" class="card border-0 shadow-sm rounded-4 p-5 text-center text-muted">Đang tải ưu đãi…</div>

      <template v-else>
        <!-- ===== Phiếu giảm giá ===== -->
        <section class="mb-5" data-testid="ud-phieu">
          <div class="ud-muc">
            <h2 class="ud-muc-ten"><i class="bi bi-ticket-perforated me-2"></i>Phiếu giảm giá</h2>
            <span class="ud-dem">{{ phieuSapXep.length }} phiếu</span>
          </div>
          <p class="ud-ghi-chu">Sao chép mã rồi dán vào ô mã giảm giá ở trang thanh toán.</p>

          <div v-if="phieuSapXep.length === 0" class="ud-trong">Hiện chưa có phiếu giảm giá nào đang áp dụng.</div>
          <div v-else class="ud-luoi">
            <article v-for="p in phieuSapXep" :key="p.ma" class="ud-phieu" data-testid="ud-the-phieu">
              <div class="ud-phieu-trai">
                <span class="ud-phieu-nhan">GIẢM</span>
                <span class="ud-phieu-muc">{{ mucGiam(p) }}</span>
              </div>
              <div class="ud-phieu-phai">
                <div class="d-flex align-items-start justify-content-between gap-2">
                  <h3 class="ud-phieu-ten">{{ p.ten }}</h3>
                  <span v-if="p.caNhan" class="ud-nhan-rieng">Dành riêng cho bạn</span>
                </div>
                <ul class="ud-phieu-dk">
                  <li v-if="Number(p.loaiGiam) === 1 && p.giamToiDa">Giảm tối đa {{ formatMoney(p.giamToiDa) }}</li>
                  <li>{{ Number(p.donToiThieu) > 0 ? `Đơn từ ${formatMoney(p.donToiThieu)}` : 'Không yêu cầu giá trị đơn tối thiểu' }}</li>
                  <li v-if="p.ngayKetThuc">Hết hạn {{ formatNgayGio(p.ngayKetThuc) }}</li>
                </ul>
                <div class="ud-phieu-chan">
                  <code class="ud-ma">{{ p.ma }}</code>
                  <button type="button" class="btn btn-sm ud-nut" :data-ma="p.ma" @click="chepMa(p.ma)">
                    <i class="bi" :class="maVuaChep === p.ma ? 'bi-check2' : 'bi-clipboard'"></i>
                    {{ maVuaChep === p.ma ? 'Đã sao chép' : 'Sao chép mã' }}
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <!-- ===== Đợt giảm giá ===== -->
        <section data-testid="ud-dot">
          <div class="ud-muc">
            <h2 class="ud-muc-ten"><i class="bi bi-tags me-2"></i>Đợt giảm giá đang diễn ra</h2>
            <span class="ud-dem">{{ dot.length }} đợt</span>
          </div>
          <p class="ud-ghi-chu">Giá sản phẩm trong đợt đã được giảm sẵn, không cần nhập mã.</p>

          <div v-if="dot.length === 0" class="ud-trong">Hiện chưa có đợt giảm giá nào đang diễn ra.</div>
          <div v-else class="ud-luoi">
            <RouterLink v-for="d in dot" :key="d.id" :to="{ path: '/shop/san-pham', query: { dot: d.id } }" class="ud-dot" data-testid="ud-the-dot">
              <div class="ud-dot-muc">
                <span>Giảm đến</span>
                <strong>{{ d.phanTramGiamMax }}%</strong>
              </div>
              <div class="ud-dot-than">
                <h3 class="ud-phieu-ten">{{ d.tenDot }}</h3>
                <p v-if="d.moTa" class="ud-dot-mo-ta">{{ d.moTa }}</p>
                <ul class="ud-phieu-dk">
                  <li>{{ d.soSanPham }} sản phẩm</li>
                  <li>Đến {{ formatNgayGio(d.ngayKetThuc) }}</li>
                </ul>
                <span class="ud-dot-xem">Xem sản phẩm <i class="bi bi-arrow-right"></i></span>
              </div>
            </RouterLink>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.ud-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1.75rem 2rem;
  color: #fff;
  background: linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 50%, #60a5fa 100%);
}
.ud-tieu-de {
  margin: 0 0 0.25rem;
  font-size: 1.7rem;
  font-weight: 800;
}
.ud-phu {
  color: #dbeafe;
}
.ud-nut-trang {
  background: #fff;
  color: var(--vnb-orange);
  font-weight: 700;
  border-radius: 999px;
  padding: 0.55rem 1.2rem;
}
.ud-nut-trang:hover {
  background: var(--vnb-orange-light);
  color: var(--vnb-orange-hover);
}
.ud-muc {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
}
.ud-muc-ten {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--vnb-dark);
}
.ud-muc-ten .bi {
  color: var(--vnb-orange);
}
.ud-dem {
  font-size: 0.85rem;
  color: #64748b;
}
.ud-ghi-chu {
  margin: 0.25rem 0 1rem;
  font-size: 0.88rem;
  color: #64748b;
}
.ud-trong {
  padding: 2rem;
  text-align: center;
  color: #64748b;
  background: #fff;
  border: 1px dashed var(--vnb-border);
  border-radius: 14px;
}
.ud-luoi {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 1rem;
}
.ud-phieu,
.ud-dot {
  display: flex;
  background: #fff;
  border: 1px solid var(--vnb-border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}
.ud-phieu-trai,
.ud-dot-muc {
  flex: 0 0 112px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  padding: 0.75rem 0.5rem;
  color: #fff;
  text-align: center;
}
.ud-phieu-trai {
  background: var(--vnb-orange);
  border-right: 2px dashed rgba(255, 255, 255, 0.6);
}
.ud-phieu-nhan {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  opacity: 0.9;
}
.ud-phieu-muc {
  font-size: 1.15rem;
  font-weight: 800;
  line-height: 1.2;
  word-break: break-word;
}
.ud-phieu-phai,
.ud-dot-than {
  flex: 1;
  min-width: 0;
  padding: 0.85rem 1rem;
}
.ud-phieu-ten {
  margin: 0 0 0.35rem;
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--vnb-dark);
}
.ud-nhan-rieng {
  flex: 0 0 auto;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  color: var(--vnb-orange-hover);
  background: var(--vnb-orange-light);
  border: 1px solid #bfdbfe;
}
.ud-phieu-dk {
  margin: 0 0 0.6rem;
  padding: 0;
  list-style: none;
  font-size: 0.84rem;
  color: #475569;
}
.ud-phieu-chan {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.ud-ma {
  padding: 0.2rem 0.55rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--vnb-dark);
  background: #f1f5f9;
  border: 1px dashed #94a3b8;
  border-radius: 6px;
}
.ud-nut {
  color: var(--vnb-orange);
  border: 1px solid var(--vnb-orange);
  background: #fff;
  font-weight: 600;
  border-radius: 999px;
  white-space: nowrap;
}
.ud-nut:hover {
  background: var(--vnb-orange);
  color: #fff;
}
.ud-dot {
  text-decoration: none;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.ud-dot:hover {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.1);
  transform: translateY(-2px);
}
.ud-dot-muc {
  background: var(--vnb-red);
  font-size: 0.78rem;
  font-weight: 600;
}
.ud-dot-muc strong {
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1.1;
}
.ud-dot-mo-ta {
  margin: 0 0 0.35rem;
  font-size: 0.84rem;
  color: #64748b;
}
.ud-dot-xem {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--vnb-orange);
}
</style>
