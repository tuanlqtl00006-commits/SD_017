<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth'
import api from '../../services/api'
import HoaDonNoiDung from '../../components/hoaDon/HoaDonNoiDung.vue'
import { formatMoney, formatNgayGio } from '../../utils/format'
import { docDonHang } from '../../utils/donHangLocal'
import { loiSdt } from '../../utils/kiemTra'
import { useAutoRefresh } from '../../composables/useAutoRefresh'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const orderCode = ref('')
// Khách chưa đăng nhập phải nhập SĐT đặt hàng: mã đơn chạy tăng dần, chỉ mã đơn thì ai cũng xem được thông tin của nhau
const sdt = ref('')
const loading = ref(false)
const orderResult = ref(null)
const searchError = ref('')

// Hai ô nhập xếp hai dòng: Mã đơn hàng và Số điện thoại, một nút Tra cứu chung:
//  - có mã đơn (+ SĐT): xem chi tiết đúng một đơn; - chỉ có SĐT: ra danh sách đơn của số đó, bấm vào để xem chi tiết
const dsDon = ref(null) // { sdt, don: [...] } khi tra chỉ bằng số điện thoại

// Tra theo SĐT: máy chủ chỉ trả danh sách gọn (mã đơn, ngày, trạng thái, thành tiền); họ tên, địa chỉ, sản phẩm chỉ xem khi bấm vào từng đơn
async function traCuuSdt() {
  lanTra++
  daTra = null
  searchError.value = ''
  orderResult.value = null
  dsDon.value = null
  const so = sdt.value.trim()
  const loi = loiSdt(so, 'Vui lòng nhập số điện thoại đặt hàng để tra cứu.')
  if (loi) {
    searchError.value = loi
    return
  }
  loading.value = true
  try {
    dsDon.value = (await api.get('/cua-hang/tra-cuu-sdt', { params: { sdt: so } })).data
  } catch (err) {
    searchError.value = err?.status == null ? err?.message || 'Không kết nối được máy chủ.' : err.message || 'Không tìm thấy đơn hàng nào với số điện thoại này.'
  } finally {
    loading.value = false
  }
}

// Bấm một đơn trong danh sách: xem chi tiết (đi qua tra cứu mã đơn + SĐT đang nhập)
async function xemDon(d) {
  const so = sdt.value.trim()
  const code = d.maHoaDon
  lanTra++
  daTra = null
  searchError.value = ''
  orderCode.value = code
  loading.value = true
  try {
    orderResult.value = await traCuuBackend(code, so)
    daTra = { code, so }
    await nextTick()
    document.querySelector('.ol-ket-qua')?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  } catch (err) {
    searchError.value = err?.status == null ? err?.message || 'Không kết nối được máy chủ.' : `Không xem được đơn ${code}. Vui lòng thử lại.`
  } finally {
    loading.value = false
  }
}

function guiForm() {
  const co = orderCode.value.trim()
  const so = sdt.value.trim()
  if (!co && !so) {
    lanTra++
    daTra = null
    orderResult.value = null
    dsDon.value = null
    searchError.value = 'Vui lòng nhập mã đơn hàng hoặc số điện thoại đặt hàng để tra cứu.'
    return
  }
  // Chỉ có số điện thoại: xem danh sách đơn; có mã đơn: xem chi tiết đơn đó
  return co ? handleSearch() : traCuuSdt()
}

// Khách đã đăng nhập xem đơn của chính mình bằng mã đơn; khách vãng lai cần mã đơn + SĐT đặt hàng
function laKhachDangNhap() {
  return auth.trangThai.nguoiDung?.vaiTro === 'KHACH_HANG'
}

onMounted(async () => {
  await auth.khoiTao()
  if (route.query.ma) {
    orderCode.value = String(route.query.ma).trim()
    // Khách vãng lai còn phải nhập SĐT nên chưa tra ngay (không đưa SĐT lên đường dẫn)
    if (laKhachDangNhap()) handleSearch()
  }
})

// Đăng xuất khi đang xem: xóa kết quả đang mở của tài khoản cũ
watch(
  () => auth.trangThai.nguoiDung,
  (nd, cu) => {
    if (cu && !nd) {
      orderResult.value = null
    }
  },
)

// Lần tra cứu thành công gần nhất: tự cập nhật trạng thái đơn đang xem (15 giây / lần, không cần bấm Tra cứu lại)
let daTra = null
let lanTra = 0
async function taiLaiNgam() {
  if (!daTra || loading.value) return
  const lan = lanTra
  try {
    const kq = await traCuuBackend(daTra.code, daTra.so)
    if (lan === lanTra && orderResult.value) orderResult.value = kq
  } catch {
    // lỗi tạm thời: giữ kết quả đang hiện
  }
}
useAutoRefresh(taiLaiNgam, 3000, { chuKyMs: 15_000, coTheTai: () => !!orderResult.value })

async function handleSearch() {
  lanTra++
  daTra = null
  const code = orderCode.value.trim()
  searchError.value = ''
  orderResult.value = null
  dsDon.value = null

  if (!code) {
    searchError.value = 'Vui lòng nhập mã đơn hàng (Ví dụ: HD01).'
    return
  }
  const so = sdt.value.trim()
  // Đã đăng nhập: SĐT chỉ cần khi tra đơn không thuộc tài khoản này
  if (!laKhachDangNhap() || so) {
    const loi = loiSdt(so, 'Vui lòng nhập số điện thoại đặt hàng để tra cứu.')
    if (loi) {
      searchError.value = loi
      return
    }
  }

  loading.value = true

  // Ưu tiên trạng thái THẬT từ backend (cửa hàng cập nhật khi xác nhận / giao hàng); chỉ khi mất kết nối máy chủ mới dùng bản lưu trên máy
  try {
    orderResult.value = await traCuuBackend(code, so)
    daTra = { code, so }
  } catch (err) {
    const local = err?.status == null && so ? traCuuLocal(code, so) : null
    if (local) orderResult.value = local
    else if (err?.status == null) searchError.value = err?.message || 'Không kết nối được máy chủ.'
    else if (laKhachDangNhap() && !so) searchError.value = `Không tìm thấy đơn hàng "${code}" trong tài khoản của bạn. Nếu là đơn đặt bằng số điện thoại khác, hãy nhập số điện thoại đó để tra cứu.`
    else searchError.value = `Không tìm thấy đơn hàng "${code}" với số điện thoại này. Vui lòng kiểm tra lại mã đơn và số điện thoại đặt hàng.`
  } finally {
    loading.value = false
  }
}

// Tra cứu từ backend: khách đăng nhập xem đơn của mình, còn lại (hoặc đơn khác) phải khớp mã đơn + SĐT
async function traCuuBackend(code, so) {
  const traCongKhai = () => api.get('/cua-hang/tra-cuu', { params: { maHoaDon: code, sdt: so } })
  const data = laKhachDangNhap()
    ? (await api.get(`/khach/don-hang/${encodeURIComponent(code)}`).catch((e) => (so ? traCongKhai() : Promise.reject(e)))).data
    : (await traCongKhai()).data
  // Backend trả sẵn "chiTietDon" cùng dạng với màn Chi tiết hóa đơn admin (đã bỏ phần nội bộ) -> vẽ y hệt màn admin
  return data.chiTietDon || chiTietTuDonGon(data)
}

/** Máy chủ cũ / đơn lưu trên máy chỉ có dữ liệu gọn: đổi sang cùng dạng với chi tiết hóa đơn admin. */
function chiTietTuDonGon(d) {
  const ten = d.hoTenNguoiNhan || 'Khách mua hàng'
  return {
    hoaDon: {
      maHoaDon: d.maHoaDon,
      loaiDon: 2,
      tenLoaiDon: 'Trực tuyến',
      trangThai: Number(d.trangThai ?? 1),
      ngayTao: d.ngayTao,
      tongTien: Number(d.tongTien) || 0,
      soTienGiam: Number(d.soTienGiam) || 0,
      phiVanChuyen: Number(d.phiVanChuyen) || 0,
      thanhTien: Number(d.thanhTien) || 0,
      donViVanChuyen: d.donViVanChuyen || null,
      ghiChu: d.ghiChu || null,
    },
    khachHang: { ten, sdt: d.sdtNguoiNhan || null, email: d.email || null },
    giaoHang: { hoTenNguoiNhan: ten, sdtNguoiNhan: d.sdtNguoiNhan || null, diaChi: d.diaChiGiaoHang || null },
    sanPham: (d.chiTiet || []).map((i, idx) => ({
      id: idx,
      maSpct: i.maSpct || '',
      tenSanPham: i.tenSanPham || 'Sản phẩm',
      mauSac: i.mauSac || '',
      trongLuong: i.trongLuong || '',
      chuVi: i.chuVi || '',
      soLuong: i.soLuong,
      donGia: Number(i.donGia) || 0,
      thanhTien: Number(i.thanhTien) || (Number(i.donGia) || 0) * (Number(i.soLuong) || 0),
    })),
    thanhToan: [],
    lichSu: [],
    daThanhToan: Number(d.daTra) || 0,
  }
}

// Mã trạng thái của đơn lưu trên máy (0 chờ xác nhận, 1 đã xác nhận, 2 đang giao, 3 hoàn thành, 6/7 hủy) -> mã của backend
const TRANG_THAI_LOCAL = { 0: 1, 1: 2, 2: 4, 3: 6, 6: 0, 7: 0 }

// Đơn lưu trên máy (khi mất kết nối máy chủ); cũng phải khớp SĐT vì máy dùng chung có thể còn đơn của khách trước
function traCuuLocal(code, so) {
  try {
    const f = docDonHang().find((o) => o.maHoaDon?.toUpperCase() === code.toUpperCase() && String(o.sdtKhachHang || '').replace(/\D/g, '') === so.replace(/\D/g, ''))
    if (!f) return null
    return chiTietTuDonGon({
      maHoaDon: f.maHoaDon,
      trangThai: TRANG_THAI_LOCAL[Number(f.trangThai) || 0] ?? 1,
      ngayTao: f.ngayTao,
      hoTenNguoiNhan: f.tenKhachHang,
      sdtNguoiNhan: f.sdtKhachHang,
      diaChiGiaoHang: f.diaChiGiaoHang,
      chiTiet: (f.items || []).map((i) => ({
        tenSanPham: i.sanPhamTen || i.tenSanPham,
        mauSac: i.mauSac,
        trongLuong: i.trongLuong,
        soLuong: i.soLuong,
        donGia: i.giaBan || i.donGia || 0,
      })),
      tongTien: f.tongTienHang,
      soTienGiam: f.tienGiam,
      phiVanChuyen: f.phiVanChuyen,
      thanhTien: f.tongThanhToan,
    })
  } catch (e) {
    console.error('Lỗi khi đọc local orders:', e)
    return null
  }
}
</script>

<template>
  <div class="order-lookup-view py-4">
    <div class="container ol-container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Tra cứu đơn hàng</li>
        </ol>
      </nav>

      <!-- Form tra cứu -->
      <div class="ol-search card border-0 shadow-sm rounded-4 p-4 mb-4">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="ol-search-icon"><i class="bi bi-search"></i></div>
          <div>
            <h4 class="fw-bold text-dark mb-0">Tra cứu đơn hàng</h4>
            <p class="text-muted small mb-0">Nhập mã đơn hàng và số điện thoại để xem một đơn, hoặc chỉ nhập số điện thoại để xem các đơn gần đây của số đó.</p>
          </div>
        </div>
        <form @submit.prevent="guiForm" class="ol-form" novalidate>
          <div class="ol-field">
            <label class="ol-label" for="ol-ma">Mã đơn hàng</label>
            <div class="ol-input-wrap">
              <i class="bi bi-receipt ol-input-icon" aria-hidden="true"></i>
              <input
                id="ol-ma"
                type="text"
                autocomplete="off"
                class="form-control ol-input ol-input-ma"
                placeholder="Nhập mã đơn hàng"
                aria-label="Mã đơn hàng"
                v-model="orderCode"
              />
            </div>
          </div>
          <div class="ol-field">
            <label class="ol-label" for="ol-sdt">
              Số điện thoại đặt hàng<span v-if="laKhachDangNhap()" class="ol-khong-bat-buoc"> (không bắt buộc khi tra đơn của tài khoản)</span>
            </label>
            <div class="ol-input-wrap">
              <i class="bi bi-telephone ol-input-icon" aria-hidden="true"></i>
              <input
                id="ol-sdt"
                type="tel"
                inputmode="numeric"
                maxlength="10"
                autocomplete="tel"
                class="form-control ol-input"
                placeholder="Nhập số điện thoại"
                aria-label="Số điện thoại đặt hàng"
                data-testid="ol-sdt"
                v-model="sdt"
              />
            </div>
          </div>
          <button class="btn btn-primary ol-nut" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <i v-else class="bi bi-search" aria-hidden="true"></i>
            <span>Tra cứu</span>
          </button>
        </form>
        <div v-if="searchError" class="alert alert-danger py-2 small mt-3 mb-0" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-1"></i> {{ searchError }}
        </div>
      </div>

      <!-- Tra theo số điện thoại: danh sách đơn gọn, bấm "Xem chi tiết" để mở đơn -->
      <div v-if="dsDon" class="card border-0 shadow-sm rounded-4 p-4 mb-4" data-testid="ol-ds-don">
        <h5 class="fw-bold mb-3">Đơn của số {{ dsDon.sdt }} <span class="text-muted fw-normal small">({{ dsDon.don.length }} đơn gần nhất)</span></h5>
        <div class="table-responsive">
          <table class="table align-middle mb-0">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Ngày đặt</th>
                <th>Loại</th>
                <th>Trạng thái</th>
                <th class="text-end">Thành tiền</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dsDon.don" :key="d.maHoaDon" :class="{ 'table-active': orderResult && orderCode === d.maHoaDon }">
                <td class="fw-bold">{{ d.maHoaDon }}</td>
                <td class="text-nowrap">{{ formatNgayGio(d.ngayTao) }}</td>
                <td>{{ d.loaiDon }}</td>
                <td><span class="badge bg-primary-subtle text-primary">{{ d.tenTrangThai }}</span></td>
                <td class="text-end fw-semibold text-nowrap">{{ formatMoney(d.thanhTien) }}</td>
                <td class="text-end">
                  <button type="button" class="btn btn-outline-primary btn-sm" :disabled="loading" data-testid="ol-xem-don" @click="xemDon(d)">Xem chi tiết</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Kết quả tra cứu: cùng một component với màn Chi tiết hóa đơn admin (cô review 04/11: hai màn y hệt nhau) -->
      <HoaDonNoiDung v-if="orderResult" :data="orderResult" che-do-khach class="ol-ket-qua" />
    </div>
  </div>
</template>

<style scoped>
.ol-container {
  max-width: 1280px;
}
.ol-search {
  position: relative;
  overflow: hidden;
  border: 1px solid #e6eefb !important;
}
.ol-search::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 4px;
  background: linear-gradient(90deg, #0d6efd, #4dabf7);
}
.ol-search-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0d6efd, #4dabf7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  flex-shrink: 0;
  box-shadow: 0 6px 16px rgba(13, 110, 253, 0.28);
}

/* Form tra cứu: mỗi ô có nhãn + biểu tượng, nút nằm cùng hàng */
/* Hai ô nhập xếp hai dòng (mã đơn / số điện thoại), nút Tra cứu ở dòng dưới */
.ol-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
.ol-form .ol-nut {
  justify-self: start;
}
.ol-label {
  display: block;
  margin-bottom: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  color: #475569;
}
.ol-khong-bat-buoc {
  font-weight: 400;
  color: #94a3b8;
}
.ol-input-wrap {
  position: relative;
}
.ol-input-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 1.05rem;
  pointer-events: none;
  transition: color 0.15s ease;
}
.ol-input-wrap:focus-within .ol-input-icon {
  color: #0d6efd;
}
.ol-input {
  height: 52px;
  padding-left: 44px;
  border: 1.5px solid #dbe3ee;
  border-radius: 12px;
  background: #f8fafc;
  font-size: 1rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}
.ol-input:hover {
  border-color: #b6c6dc;
}
.ol-input:focus {
  border-color: #0d6efd;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(13, 110, 253, 0.12);
}
.ol-input::placeholder {
  color: #9aa7b8;
}
/* Mã đơn tự viết hoa khi gõ, còn chữ gợi ý giữ chữ thường */
.ol-input-ma {
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 600;
}
.ol-input-ma::placeholder {
  text-transform: none;
  letter-spacing: 0;
  font-weight: 400;
}
.ol-nut {
  height: 52px;
  padding: 0 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #0d6efd, #3b8cff);
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 6px 16px rgba(13, 110, 253, 0.28);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.ol-nut:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(13, 110, 253, 0.36);
}
/* Kết quả dùng bố cục rộng như màn admin */
.ol-ket-qua {
  margin-top: 0.5rem;
}
</style>
