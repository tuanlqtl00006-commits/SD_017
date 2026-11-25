<script setup>
// Màn hình khách hàng ở quầy (góp ý 25/11): cửa sổ riêng quay về phía khách, chỉ hiển thị hóa đơn nhân viên đang bán.
// Bố cục theo mẫu người xem chiếu trong video 25/11 [03:15]: đầu trang tên cửa hàng + mã hóa đơn; trái "Sản phẩm đã chọn";
// phải khung tối "Tổng kết hóa đơn" (phiếu đang áp, các dòng tiền, khách cần thanh toán, phương thức dự kiến).
// Dữ liệu do màn bán hàng gửi sang (utils/manHinhKhach.js), cập nhật ngay khi thêm / xóa / đổi số lượng / đổi phiếu / chuyển hóa đơn chờ.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { docManHinhKhach, ngheManHinhKhach } from '../utils/manHinhKhach'
import { formatMoney } from '../utils/format'

const du = ref(docManHinhKhach())
let huyNghe = null
onMounted(() => {
  document.title = 'Màn hình khách hàng - FootStyle'
  huyNghe = ngheManHinhKhach((b) => {
    if (b) du.value = b
  })
})
onBeforeUnmount(() => huyNghe?.())

const dong = computed(() => du.value?.dong ?? [])
const coDon = computed(() => !!du.value?.maHoaDon && dong.value.length > 0)
const tongSoLuong = computed(() => dong.value.reduce((t, d) => t + d.soLuong, 0))
// Các dòng tiền như khối "Thông tin đơn hàng" ở quầy (bỏ dòng tổng, dòng tổng hiện to riêng)
const dongTien = computed(() => (du.value?.tienDon ?? []).filter((d) => d.khoa !== 'tong-thanh-toan'))
function chuTien(d) {
  if (d.khoa === 'phi-van-chuyen') return d.so === null ? 'Chưa tính' : d.so ? `+${formatMoney(d.so)}` : 'Miễn phí'
  if (d.khoa === 'phieu') return d.so ? `-${formatMoney(d.so)}` : 'Không dùng phiếu'
  return `${d.dau === '-' ? '-' : ''}${formatMoney(d.so)}`
}
</script>

<template>
  <div class="mk">
    <header class="mk-dau">
      <div class="mk-hieu">
        <img src="/logo.png" :alt="du?.tenCuaHang || 'FootStyle'" class="mk-logo" />
        <div class="mk-khau-hieu">Cửa hàng cầu lông</div>
      </div>
      <div v-if="du?.maHoaDon" class="mk-ma" data-testid="mk-ma">
        <span>HÓA ĐƠN</span>
        <b>{{ du.maHoaDon }}</b>
      </div>
    </header>

    <main v-if="coDon" class="mk-than">
      <section class="mk-ds" aria-label="Sản phẩm trong hóa đơn">
        <div class="mk-nhan">SẢN PHẨM ĐÃ CHỌN<template v-if="du.tenKhach"> · {{ du.tenKhach }}</template></div>
        <h2 class="mk-tieu-de">{{ dong.length }} sản phẩm · {{ tongSoLuong }} món</h2>
        <ul class="mk-ds-list">
          <li v-for="d in dong" :key="d.id" class="mk-sp" data-testid="mk-dong">
            <div class="mk-anh">
              <img v-if="d.anh" :src="d.anh" :alt="d.ten" />
              <span v-else class="mk-anh-trong">{{ (d.ten || '?').charAt(0) }}</span>
            </div>
            <div class="mk-sp-ten">
              <div class="mk-sp-chinh">{{ d.ten }}</div>
              <div class="mk-sp-phu">
                <template v-if="d.phanLoai">{{ d.phanLoai }} · </template>{{ d.soLuong }} × {{ formatMoney(d.donGia) }}
              </div>
              <span v-if="d.phanTramGiam > 0" class="mk-sp-giam">-{{ d.phanTramGiam }}%</span>
            </div>
            <div class="mk-sp-tien">
              <b>{{ formatMoney(d.donGia * d.soLuong) }}</b>
              <s v-if="d.giaGoc > d.donGia">{{ formatMoney(d.giaGoc * d.soLuong) }}</s>
            </div>
          </li>
        </ul>
      </section>

      <aside class="mk-tong" aria-label="Tổng kết hóa đơn">
        <div class="mk-nhan mk-nhan-vang">THÔNG TIN THANH TOÁN</div>
        <h2 class="mk-tieu-de mk-trang">Tổng kết hóa đơn</h2>

        <div v-if="du.phieu" class="mk-phieu" data-testid="mk-phieu">
          <span class="mk-phieu-icon">%</span>
          <div class="mk-phieu-chu">
            <div class="mk-phieu-nhan">Phiếu giảm giá đang áp dụng</div>
            <b>{{ du.phieu.ma }}</b>
            <div class="mk-phieu-tt"><template v-for="(t, i) in String(du.phieu.thongTin || '').split(' · ')" :key="i">{{ i ? ' · ' : '' }}<span class="mk-lien">{{ t }}</span></template></div>
          </div>
          <div class="mk-phieu-giam">-{{ formatMoney(du.phieu.giam) }}</div>
        </div>

        <dl class="mk-dong-tien">
          <div v-for="d in dongTien" :key="d.khoa">
            <dt>{{ d.nhan }}</dt>
            <dd :class="{ 'mk-giam': d.dau === '-' && d.so }">{{ chuTien(d) }}</dd>
          </div>
        </dl>

        <div class="mk-phai-tra">
          <div class="mk-nhan mk-nhan-vang">KHÁCH CẦN THANH TOÁN</div>
          <strong data-testid="mk-tong">{{ formatMoney(du.tongThanhToan) }}</strong>
        </div>
        <div v-if="du.phuongThuc" class="mk-pt" data-testid="mk-phuong-thuc">
          <span>Phương thức dự kiến</span>
          <b>{{ du.phuongThuc }}</b>
        </div>
      </aside>
    </main>

    <main v-else class="mk-trong">
      <div class="mk-trong-icon"><i class="bi bi-bag-heart" aria-hidden="true"></i></div>
      <div class="mk-chao">Xin chào quý khách!</div>
      <div class="mk-chao-phu">{{ du?.maHoaDon ? 'Nhân viên đang thêm sản phẩm vào hóa đơn ' + du.maHoaDon + '…' : 'Sản phẩm và tổng tiền sẽ hiện ở đây khi nhân viên lên đơn.' }}</div>
    </main>
  </div>
</template>

<style scoped>
.mk {
  position: fixed;
  inset: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: #eef2f7;
  color: #0f172a;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
}
.mk-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 2rem;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}
.mk-hieu {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.mk-logo {
  height: 44px;
  width: auto;
}
.mk-ten {
  font-size: 1.35rem;
  font-weight: 800;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.mk-khau-hieu {
  font-size: 0.8rem;
  color: #64748b;
}
.mk-ma {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
}
.mk-ma span {
  font-size: 0.72rem;
  letter-spacing: 1px;
  color: #64748b;
}
.mk-ma b {
  font-size: 1.3rem;
}
.mk-than {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(340px, 1fr);
  gap: 1.25rem;
  padding: 1.25rem 2rem 2rem;
  align-items: start;
}
.mk-nhan {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 1px;
  color: #d97706;
}
.mk-nhan-vang {
  color: #fbbf24;
}
.mk-tieu-de {
  margin: 0.2rem 0 0.75rem;
  font-size: 1.4rem;
  font-weight: 800;
}
.mk-trang {
  color: #fff;
}
.mk-ds {
  background: #fff;
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
}
.mk-ds-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.mk-sp {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 0;
  border-top: 1px solid #eef2f7;
}
/* Ảnh vợt dài và hẹp: đặt trong ô vuông có viền, giữ trọn ảnh (không cắt) */
.mk-anh {
  width: 76px;
  height: 76px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}
.mk-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.mk-anh-trong {
  font-size: 1.4rem;
  font-weight: 700;
  color: #94a3b8;
}
.mk-sp-chinh {
  font-weight: 700;
  font-size: 1.05rem;
}
.mk-sp-phu {
  color: #64748b;
  font-size: 0.9rem;
  margin-top: 2px;
}
.mk-sp-giam {
  display: inline-block;
  margin-top: 4px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #fef3c7;
  color: #b45309;
  font-size: 0.75rem;
  font-weight: 700;
}
.mk-sp-tien {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 130px;
}
.mk-sp-tien b {
  font-size: 1.1rem;
}
.mk-sp-tien s {
  color: #94a3b8;
  font-size: 0.85rem;
}
.mk-tong {
  background: linear-gradient(160deg, #1e3a5f, #0f2440);
  color: #e2e8f0;
  border-radius: 16px;
  padding: 1.25rem 1.4rem 1.4rem;
  box-shadow: 0 8px 24px rgba(15, 36, 64, 0.35);
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.mk-phieu {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.9rem;
  margin-bottom: 0.75rem;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}
.mk-phieu-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #fbbf24;
  color: #78350f;
  font-weight: 800;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mk-phieu-nhan {
  font-size: 0.75rem;
  color: #cbd5e1;
}
.mk-phieu-chu b {
  color: #fff;
}
.mk-phieu-tt {
  font-size: 0.78rem;
  color: #cbd5e1;
}
.mk-phieu-giam {
  font-weight: 800;
  color: #4ade80;
  white-space: nowrap;
}
.mk-lien {
  white-space: nowrap;
}
.mk-dong-tien {
  margin: 0;
  padding: 0.5rem 0 0.75rem;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.mk-dong-tien div {
  display: flex;
  justify-content: space-between;
  padding: 0.35rem 0;
  font-size: 1rem;
}
.mk-dong-tien dt {
  font-weight: 500;
  color: #cbd5e1;
}
.mk-dong-tien dd {
  margin: 0;
  font-weight: 700;
  color: #fff;
}
.mk-dong-tien dd.mk-giam {
  color: #4ade80;
}
.mk-phai-tra {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  padding-top: 0.9rem;
}
.mk-phai-tra strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 2.4rem;
  font-weight: 800;
  color: #fbbf24;
}
.mk-pt {
  display: flex;
  justify-content: space-between;
  margin-top: 0.75rem;
  padding: 0.6rem 0.9rem;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 0.9rem;
}
.mk-pt b {
  color: #fff;
}
.mk-trong {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  text-align: center;
  padding: 2rem;
}
.mk-trong-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: #fef3c7;
  color: #b45309;
  font-size: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}
.mk-chao {
  font-size: 2.2rem;
  font-weight: 800;
  color: #0f172a;
}
.mk-chao-phu {
  font-size: 1.05rem;
  color: #64748b;
}
@media (max-width: 900px) {
  .mk-than {
    grid-template-columns: 1fr;
    padding: 1rem;
  }
}
</style>
