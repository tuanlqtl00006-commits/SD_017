<script setup>
// Màn hình khách hàng ở quầy (góp ý 25/11): cửa sổ riêng quay về phía khách, chỉ hiển thị hóa đơn nhân viên đang bán.
// Giao diện theo phong cách FootStyle (cùng màu, thẻ, ô icon với màn quản trị; khối phiếu giống màn bán hàng) - 26/11 người dùng dặn
// "giống nhóm tôi, đừng làm quá sang nhóm kia".
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
// Các dòng tiền cùng tên, cùng thứ tự với khối "Thông tin đơn hàng" ở quầy (dòng tổng hiện to riêng)
const dongTien = computed(() => (du.value?.tienDon ?? []).filter((d) => d.khoa !== 'tong-thanh-toan'))
function chuTien(d) {
  if (d.khoa === 'phi-van-chuyen') return d.so === null ? 'Chưa tính' : d.so ? `+${formatMoney(d.so)}` : 'Miễn phí'
  if (d.khoa === 'phieu') return d.so ? `-${formatMoney(d.so)}` : 'Không dùng phiếu'
  return `${d.dau === '-' ? '-' : ''}${formatMoney(d.so)}`
}
const cumThongTin = (s) => String(s || '').split(' · ')
</script>

<template>
  <div class="mk">
    <header class="mk-dau">
      <img src="/logo.png" :alt="du?.tenCuaHang || 'FootStyle'" class="mk-logo" />
      <div v-if="du?.maHoaDon" class="mk-ma" data-testid="mk-ma">
        <i class="bi bi-receipt" aria-hidden="true"></i> Hóa đơn <b>{{ du.maHoaDon }}</b>
        <template v-if="du.tenKhach"><span class="mk-ma-cham">·</span>{{ du.tenKhach }}</template>
      </div>
    </header>

    <main v-if="coDon" class="mk-than">
      <!-- Trái: giỏ hàng -->
      <section class="mk-the" aria-labelledby="mk-gio">
        <div class="mk-the-dau">
          <span class="mk-icon"><i class="bi bi-cart3" aria-hidden="true"></i></span>
          <div>
            <h2 id="mk-gio" class="mk-tieu-de">Giỏ hàng</h2>
            <div class="mk-phu">{{ dong.length }} sản phẩm · {{ tongSoLuong }} món</div>
          </div>
        </div>
        <ul class="mk-ds">
          <li v-for="d in dong" :key="d.id" class="mk-sp" data-testid="mk-dong">
            <div class="mk-anh">
              <img v-if="d.anh" :src="d.anh" :alt="d.ten" />
              <i v-else class="bi bi-image" aria-hidden="true"></i>
            </div>
            <div class="mk-sp-ten">
              <div class="mk-sp-chinh">{{ d.ten }}</div>
              <div v-if="d.phanLoai" class="mk-sp-phu">{{ d.phanLoai }}</div>
              <div class="mk-sp-gia">
                {{ d.soLuong }} × {{ formatMoney(d.donGia) }}
                <s v-if="d.giaGoc > d.donGia">{{ formatMoney(d.giaGoc) }}</s>
                <span v-if="d.phanTramGiam > 0" class="mk-giam-nhan">-{{ d.phanTramGiam }}%</span>
              </div>
            </div>
            <div class="mk-sp-tien">{{ formatMoney(d.donGia * d.soLuong) }}</div>
          </li>
        </ul>
      </section>

      <!-- Phải: thông tin đơn hàng (cùng tên với màn quầy) -->
      <aside class="mk-the mk-tong" aria-labelledby="mk-tt">
        <div class="mk-the-dau">
          <span class="mk-icon"><i class="bi bi-credit-card" aria-hidden="true"></i></span>
          <h2 id="mk-tt" class="mk-tieu-de">Thông tin đơn hàng</h2>
        </div>

        <!-- Khối phiếu giống màn bán hàng: "Đang áp dụng phiếu X (giảm Y)" + dòng thông tin phiếu -->
        <div v-if="du.phieu" class="mk-phieu" data-testid="mk-phieu">
          <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
          <span>
            Đang áp dụng phiếu <b>{{ du.phieu.ma }}</b> <span class="mk-lien">(giảm <b>{{ formatMoney(du.phieu.giam) }}</b>)</span>
            <small class="mk-phieu-tt"><template v-for="(t, i) in cumThongTin(du.phieu.thongTin)" :key="i">{{ i ? ' · ' : '' }}<span class="mk-lien">{{ t }}</span></template></small>
          </span>
        </div>

        <dl class="mk-dong-tien">
          <div v-for="d in dongTien" :key="d.khoa">
            <dt>{{ d.nhan }}</dt>
            <dd :class="{ 'mk-giam': d.dau === '-' && d.so }">{{ chuTien(d) }}</dd>
          </div>
        </dl>

        <div class="mk-phai-tra">
          <span>Tổng thanh toán</span>
          <strong data-testid="mk-tong">{{ formatMoney(du.tongThanhToan) }}</strong>
        </div>
        <div v-if="du.phuongThuc" class="mk-pt" data-testid="mk-phuong-thuc">
          <span>Hình thức thanh toán</span>
          <b>{{ du.phuongThuc }}</b>
        </div>
      </aside>
    </main>

    <main v-else class="mk-trong">
      <div class="mk-chao">Xin chào quý khách!</div>
      <div class="mk-chao-phu">{{ du?.maHoaDon ? 'Nhân viên đang thêm sản phẩm vào hóa đơn ' + du.maHoaDon + '…' : 'Sản phẩm và tổng tiền sẽ hiện ở đây khi nhân viên lên đơn.' }}</div>
    </main>
  </div>
</template>

<style scoped>
/* Màu, thẻ, ô icon theo màn quản trị FootStyle (assets/admin.css: --fs-*, .ad-card, .ad-icon-box) */
.mk {
  position: fixed;
  inset: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: var(--fs-surface, #f7f9fc);
  color: var(--fs-text, #18242f);
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
}
.mk-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.8rem 2rem;
  background: #fff;
  border-bottom: 1px solid var(--fs-line, #e6eaf0);
}
.mk-logo {
  height: 44px;
  width: auto;
}
.mk-ma {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-primary-dark, #075fc0);
  font-size: 1rem;
}
.mk-ma-cham {
  margin: 0 0.1rem;
}
.mk-than {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(340px, 1fr);
  gap: 1.25rem;
  padding: 1.25rem 2rem 2rem;
  align-items: start;
}
.mk-the {
  background: #fff;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.04);
  box-shadow: 0 8px 30px rgba(31, 45, 61, 0.06);
  padding: 1.25rem 1.5rem;
}
.mk-the-dau {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.mk-icon {
  flex: none;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-primary, #0977ec);
  font-size: 1.2rem;
}
.mk-tieu-de {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}
.mk-phu {
  color: var(--fs-muted, #7b8794);
  font-size: 0.9rem;
}
.mk-ds {
  list-style: none;
  margin: 0;
  padding: 0;
}
.mk-sp {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 0.8rem 0;
  border-top: 1px solid var(--fs-line-soft, #eef1f5);
}
.mk-anh {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--fs-line, #e6eaf0);
  display: grid;
  place-items: center;
  padding: 4px;
  color: var(--fs-muted, #7b8794);
  font-size: 1.4rem;
}
.mk-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.mk-sp-chinh {
  font-weight: 700;
  font-size: 1.05rem;
}
.mk-sp-phu {
  color: var(--fs-muted, #7b8794);
  font-size: 0.88rem;
}
.mk-sp-gia {
  margin-top: 2px;
  font-size: 0.92rem;
  color: #475569;
}
.mk-sp-gia s {
  margin-left: 0.35rem;
  color: #94a3b8;
}
.mk-giam-nhan {
  margin-left: 0.35rem;
  padding: 0 0.45rem;
  border-radius: 999px;
  background: #fdecee;
  color: var(--fs-danger, #dc3545);
  font-size: 0.75rem;
  font-weight: 700;
}
.mk-sp-tien {
  font-weight: 700;
  font-size: 1.1rem;
  min-width: 130px;
  text-align: right;
}
/* Khối phiếu giống .bh-phieu-ap ở màn bán hàng */
.mk-phieu {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.6rem 0.75rem;
  margin-bottom: 0.5rem;
  border-radius: 10px;
  background: #ecfdf5;
  font-size: 0.95rem;
  line-height: 1.45;
}
.mk-phieu > .bi {
  color: #16a34a;
  font-size: 1.1rem;
  line-height: 1.3;
}
.mk-phieu b {
  color: #15803d;
}
.mk-phieu-tt {
  display: block;
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px dashed #bbf7d0;
  font-size: 0.85rem;
  color: #475569;
}
.mk-lien {
  white-space: nowrap;
}
.mk-dong-tien {
  margin: 0;
}
.mk-dong-tien div {
  display: flex;
  justify-content: space-between;
  padding: 0.4rem 0;
  font-size: 1.02rem;
}
.mk-dong-tien dt {
  font-weight: 500;
  color: #475569;
}
.mk-dong-tien dd {
  margin: 0;
  font-weight: 600;
}
.mk-dong-tien dd.mk-giam {
  color: var(--fs-success, #198754);
}
.mk-phai-tra {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 0.4rem;
  padding-top: 0.9rem;
  border-top: 1px dashed var(--fs-line, #e6eaf0);
  font-size: 1.1rem;
  font-weight: 700;
}
.mk-phai-tra strong {
  font-size: 2.1rem;
  color: var(--fs-danger, #dc3545);
}
.mk-pt {
  display: flex;
  justify-content: space-between;
  margin-top: 0.75rem;
  padding: 0.55rem 0.85rem;
  border-radius: 10px;
  background: var(--fs-surface, #f7f9fc);
  font-size: 0.92rem;
  color: #475569;
}
.mk-pt b {
  color: var(--fs-text, #18242f);
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
.mk-chao {
  font-size: 2.1rem;
  font-weight: 800;
  color: var(--fs-primary-dark, #075fc0);
}
.mk-chao-phu {
  font-size: 1.05rem;
  color: var(--fs-muted, #7b8794);
}
@media (max-width: 900px) {
  .mk-than {
    grid-template-columns: 1fr;
    padding: 1rem;
  }
}
</style>
