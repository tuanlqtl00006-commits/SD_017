<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { formatMoney } from '../utils/format'

const route = useRoute()

// ---- Quy ước trạng thái / loại (theo database/SD_17.sql) ----
// hoa_don.loai_hoa_don: 1 = Tại quầy, 2 = Giao hàng
const LOAI_HOA_DON = { 1: 'Tại quầy', 2: 'Giao hàng' }
// hoa_don.trang_thai: 0 = Hủy, 1 = Chờ xác nhận, 2 = Đã xác nhận, 3 = Chờ lấy hàng
const TRANG_THAI = { 0: 'Đã hủy', 1: 'Chờ xác nhận', 2: 'Đã xác nhận', 3: 'Chờ lấy hàng' }
const BUOC = [
  { tt: 1, ten: 'Chờ Xác Nhận', hanhDong: 'Chờ xác nhận', icon: 'bi-hourglass-split' },
  { tt: 2, ten: 'Đã Xác Nhận', hanhDong: 'Đã xác nhận', icon: 'bi-clipboard-check' },
  { tt: 3, ten: 'Chờ Lấy Hàng', hanhDong: 'Chờ lấy hàng', icon: 'bi-box-seam' },
]

// ---- Dữ liệu mẫu, đặt tên cột đúng như bảng SQL ----
// Khi có backend: const { data } = await api.get(`/hoa-don/${route.params.id}`)
const hoaDon = ref({
  id: Number(route.params.id) || 1,
  ma_hoa_don: 'HD0037',
  loai_hoa_don: 2,
  trang_thai: 2,
  tong_tien: 3032000,
  so_tien_giam: 1500000,
  phi_van_chuyen: 31000,
  thanh_tien: 1563000,
  don_vi_van_chuyen: 'GHN',
  dia_chi_giao_hang: 'ưiuefhweiufhwfehe',
  ghi_chu: null,
  khach_hang: { ho_ten: 'nguyễn van a', sdt: '0383854485', email: 'sđf@gmail.com' },
  phieu_giam_gia: { ma_phieu: 'VCH65FFTO' },
  phuong_thuc_thanh_toan: { ten_phuong_thuc: 'Chuyển khoản' },
  lich_su_hoa_don: [
    { hanh_dong: 'Chờ xác nhận', nguoi_thao_tac: 'ADMIN: TA', thoi_gian: '2026-05-10T17:05:25', mo_ta: 'Tạo hóa đơn' },
    { hanh_dong: 'Đã xác nhận', nguoi_thao_tac: 'ADMIN: TA', thoi_gian: '2026-05-10T17:08:03', mo_ta: 'Xác nhận đơn hàng' },
  ],
  lich_su_thanh_toan: [
    { so_tien: 1563000, ma_giao_dich: null, thoi_gian: '2026-05-10T17:08:03', trang_thai: 1, mo_ta: 'Thanh toán đơn hàng' },
  ],
  hoa_don_chi_tiet: [
    { ten_san_pham: 'vợt cầu lông', mau_sac: 'Trắng', trong_luong: '4U', chu_vi: 'G5', so_luong: 1, don_gia: 3032000, thanh_tien: 3032000 },
  ],
})

const pad = (n) => String(n).padStart(2, '0')
const gio = (iso) => { const d = new Date(iso); return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` }
const ngay = (iso) => { const d = new Date(iso); return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}` }

const daHuy = computed(() => hoaDon.value.trang_thai === 0)
const buoc = computed(() =>
  BUOC.map((b) => ({
    ...b,
    xong: hoaDon.value.trang_thai >= b.tt,
    log: hoaDon.value.lich_su_hoa_don.find((l) => l.hanh_dong === b.hanhDong),
  })),
)
const soSanPham = computed(() => hoaDon.value.hoa_don_chi_tiet.length)

const hienLichSu = ref(false)
const imHoaDon = () => window.print()
function huyDon() {
  if (!window.confirm(`Hủy hóa đơn ${hoaDon.value.ma_hoa_don}?`)) return
  hoaDon.value.trang_thai = 0
  hoaDon.value.lich_su_hoa_don.push({
    hanh_dong: 'Hủy đơn', nguoi_thao_tac: 'ADMIN', thoi_gian: new Date().toISOString(), mo_ta: 'Hủy hóa đơn',
  })
}
</script>

<template>
  <div>
    <!-- Hàng 1: Trạng thái + Tổng kết -->
    <div class="row g-4 mb-4">
      <div class="col-xl-8">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4 d-flex flex-column">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h6 class="fw-bold m-0 d-flex align-items-center"><i class="bi bi-file-earmark-check text-primary me-2"></i> Trạng Thái Đơn Hàng</h6>
              <span
                class="badge px-3 py-2 rounded-pill fw-semibold"
                :class="daHuy ? 'bg-danger bg-opacity-10 text-danger' : 'bg-success bg-opacity-10 text-success'"
              >{{ TRANG_THAI[hoaDon.trang_thai] }}</span>
            </div>

            <div class="timeline-container my-4 flex-grow-1">
              <div class="timeline-line"></div>
              <div class="d-flex justify-content-between position-relative">
                <div v-for="b in buoc" :key="b.tt" class="timeline-step" :class="{ active: b.xong }">
                  <div class="timeline-icon" :class="b.xong ? 'bg-primary text-white' : 'bg-white text-muted border'">
                    <i class="bi" :class="b.icon"></i>
                  </div>
                  <div class="mt-2" :class="b.xong ? 'fw-bold text-dark' : 'text-muted fw-medium'">{{ b.ten }}</div>
                  <div v-if="b.log" class="text-muted small">{{ gio(b.log.thoi_gian) }}<br>{{ ngay(b.log.thoi_gian) }}<br>{{ b.log.nguoi_thao_tac }}</div>
                </div>
              </div>
            </div>

            <div class="d-flex justify-content-end gap-3 mt-3">
              <button class="btn btn-outline-danger px-4 rounded-pill fw-medium" :disabled="daHuy" @click="huyDon">
                <i class="bi bi-x-circle me-1"></i> Hủy Đơn Hàng
              </button>
              <button class="btn btn-primary px-4 rounded-pill fw-medium" @click="hienLichSu = !hienLichSu">
                <i class="bi bi-clock-history me-1"></i> Lịch Sử Thao Tác
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="col-xl-4">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-wallet2 text-primary me-2"></i> Tổng Kết Thanh Toán</h6>
            <div class="d-flex justify-content-between mb-3 text-muted">
              <span>Tổng Tiền Hàng</span>
              <span class="text-dark fw-medium text-nowrap">{{ formatMoney(hoaDon.tong_tien) }}</span>
            </div>
            <div class="d-flex justify-content-between mb-3 text-muted">
              <span>Phiếu Giảm Giá
                <span v-if="hoaDon.phieu_giam_gia" class="badge bg-primary bg-opacity-10 text-primary ms-1">{{ hoaDon.phieu_giam_gia.ma_phieu }}</span>
              </span>
              <span class="text-success fw-medium text-nowrap">- {{ formatMoney(hoaDon.so_tien_giam) }}</span>
            </div>
            <div class="d-flex justify-content-between mb-3 text-muted border-bottom pb-3">
              <span>Phí Vận Chuyển
                <span v-if="hoaDon.don_vi_van_chuyen" class="badge bg-warning text-dark ms-1">{{ hoaDon.don_vi_van_chuyen }}</span>
              </span>
              <span class="text-dark fw-medium text-nowrap">+ {{ formatMoney(hoaDon.phi_van_chuyen) }}</span>
            </div>
            <div class="d-flex justify-content-between align-items-center mt-3">
              <span class="fw-bold text-dark fs-5">Tổng Tiền</span>
              <span class="fw-bold text-primary fs-4">{{ formatMoney(hoaDon.thanh_tien) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Lịch sử thao tác (bảng lich_su_hoa_don) -->
    <div v-if="hienLichSu" class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4">
        <h6 class="fw-bold mb-3 d-flex align-items-center"><i class="bi bi-clock-history text-primary me-2"></i> Lịch Sử Thao Tác</h6>
        <table class="table align-middle m-0">
          <thead class="table-light">
            <tr><th class="border-0">Thời gian</th><th class="border-0">Người thao tác</th><th class="border-0">Hành động</th><th class="border-0">Mô tả</th></tr>
          </thead>
          <tbody>
            <tr v-for="(l, i) in hoaDon.lich_su_hoa_don" :key="i">
              <td>{{ gio(l.thoi_gian) }} {{ ngay(l.thoi_gian) }}</td>
              <td>{{ l.nguoi_thao_tac }}</td>
              <td class="fw-medium">{{ l.hanh_dong }}</td>
              <td class="text-muted">{{ l.mo_ta }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Hàng 2: Khách hàng / Giao hàng / Thanh toán -->
    <div class="row g-4 mb-4">
      <div class="col-xl-4">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-person-lines-fill text-primary me-2"></i> Thông Tin Khách Hàng</h6>
            <div class="row mb-3"><div class="col-5 text-muted small text-nowrap">Tên Khách Hàng</div><div class="col-7 text-end fw-medium text-dark">{{ hoaDon.khach_hang.ho_ten }}</div></div>
            <div class="row mb-3"><div class="col-5 text-muted small text-nowrap">Số Điện Thoại</div><div class="col-7 text-end fw-medium text-dark">{{ hoaDon.khach_hang.sdt }}</div></div>
            <div class="row"><div class="col-4 text-muted small">Email</div><div class="col-8 text-end fw-medium text-dark text-break">{{ hoaDon.khach_hang.email }}</div></div>
          </div>
        </div>
      </div>

      <div class="col-xl-4">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-geo-alt text-primary me-2"></i> Thông Tin Giao Hàng</h6>
            <div class="row mb-3"><div class="col-3 text-muted small">Địa Chỉ</div><div class="col-9 text-end fw-medium text-dark">{{ hoaDon.dia_chi_giao_hang || '—' }}</div></div>
            <div class="row mb-3"><div class="col-4 text-muted small">Loại Đơn</div><div class="col-8 text-end fw-medium text-dark">{{ LOAI_HOA_DON[hoaDon.loai_hoa_don] }}</div></div>
            <div class="row"><div class="col-4 text-muted small">Ghi Chú</div><div class="col-8 text-end fw-medium text-muted fst-italic">{{ hoaDon.ghi_chu || 'Không có' }}</div></div>
          </div>
        </div>
      </div>

      <div class="col-xl-4">
        <div class="card border-0 shadow-sm rounded-4 h-100">
          <div class="card-body p-4 d-flex flex-column">
            <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-credit-card text-primary me-2"></i> Lịch Sử Thanh Toán</h6>
            <div v-for="(t, i) in hoaDon.lich_su_thanh_toan" :key="i" class="d-flex justify-content-between align-items-center mb-4 flex-grow-1">
              <div>
                <div class="fw-bold text-dark">{{ hoaDon.phuong_thuc_thanh_toan?.ten_phuong_thuc }}</div>
                <div class="small text-muted">{{ t.mo_ta }}</div>
                <div class="text-muted" style="font-size: 0.75rem;">{{ gio(t.thoi_gian) }} {{ ngay(t.thoi_gian) }}</div>
              </div>
              <div class="text-end">
                <div class="badge mb-1" :class="t.trang_thai === 1 ? 'bg-success bg-opacity-10 text-success' : 'bg-secondary bg-opacity-10 text-secondary'">
                  {{ t.trang_thai === 1 ? 'Đã thanh toán' : 'Chưa thanh toán' }}
                </div>
                <div class="fw-bold text-dark">{{ formatMoney(t.so_tien) }}</div>
              </div>
            </div>
            <div class="d-flex flex-column gap-2 mt-auto">
              <button class="btn btn-outline-primary rounded-3 fw-medium py-2" @click="imHoaDon"><i class="bi bi-printer me-1"></i> In Hóa Đơn</button>
              <button class="btn btn-primary rounded-3 fw-medium py-2"><i class="bi bi-pencil-square me-1"></i> Chỉnh Sửa Đơn Hàng</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Hàng 3: Danh sách sản phẩm (hoa_don_chi_tiet) -->
    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4">
        <h6 class="fw-bold mb-4 d-flex align-items-center"><i class="bi bi-box text-primary me-2"></i> Danh Sách Sản Phẩm ({{ soSanPham }})</h6>
        <div class="table-responsive">
          <table class="table align-middle m-0">
            <thead class="table-light">
              <tr>
                <th class="py-3 text-muted fw-semibold border-0">Sản phẩm</th>
                <th class="py-3 text-muted fw-semibold border-0 text-center">Số lượng</th>
                <th class="py-3 text-muted fw-semibold border-0 text-end">Đơn giá</th>
                <th class="py-3 text-muted fw-semibold border-0 text-end">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(sp, i) in hoaDon.hoa_don_chi_tiet" :key="i">
                <td class="py-3 border-0">
                  <div class="d-flex align-items-center gap-3">
                    <div class="bg-light rounded-3 d-flex align-items-center justify-content-center" style="width: 60px; height: 60px;">
                      <i class="bi bi-image text-muted fs-4"></i>
                    </div>
                    <div>
                      <div class="fw-bold text-dark">{{ sp.ten_san_pham }}</div>
                      <div class="small text-muted">Màu: {{ sp.mau_sac }} | Trọng lượng: {{ sp.trong_luong }} | Chu vi: {{ sp.chu_vi }}</div>
                    </div>
                  </div>
                </td>
                <td class="py-3 border-0 text-center fw-medium text-dark">{{ sp.so_luong }}</td>
                <td class="py-3 border-0 text-end fw-medium text-dark">{{ formatMoney(sp.don_gia) }}</td>
                <td class="py-3 border-0 text-end fw-bold text-primary">{{ formatMoney(sp.thanh_tien) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline-container { position: relative; padding: 0 20px; }
.timeline-line { position: absolute; top: 26px; left: 10%; right: 10%; height: 3px; background-color: #e9ecef; }
.timeline-step { position: relative; text-align: center; width: 150px; }
.timeline-icon {
  width: 52px; height: 52px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto; font-size: 1.4rem;
}
.timeline-step.active .timeline-icon { box-shadow: 0 0 0 5px rgba(13, 110, 253, 0.15); }
</style>
