<script setup>
// Quy định / chính sách cửa hàng (cô review 14/11: tất cả các nhóm phải có màn quy định của cửa hàng: đổi trả, thông tin mua hàng,
// bảo hành...). Nội dung viết theo đúng cách hệ thống đang chạy:
//  - Phí giao hàng và ngưỡng miễn phí vận chuyển lấy từ cài đặt của quản lý (Cài đặt -> Cài đặt free ship), không theo tỉnh;
//  - Thanh toán: COD và chuyển khoản VietQR khi đặt online; tiền mặt / chuyển khoản / kết hợp khi mua tại cửa hàng;
//  - Khách tự hủy đơn khi đơn còn "Chờ xác nhận" (KhachTaiKhoanController PUT /khach/don-hang/{ma}/huy).
import { nextTick, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { dungCaiDatVanChuyen } from '../../services/caiDatVanChuyen'
import { formatMoney } from '../../utils/format'

const route = useRoute()
const caiDatShip = dungCaiDatVanChuyen()

const MUC = [
  { id: 'giao-hang', ten: 'Giao hàng', icon: 'bi-truck' },
  { id: 'thanh-toan', ten: 'Thanh toán', icon: 'bi-credit-card' },
  { id: 'doi-tra', ten: 'Đổi trả & bảo hành', icon: 'bi-arrow-repeat' },
  { id: 'dat-hang', ten: 'Đặt hàng & hủy đơn', icon: 'bi-bag-check' },
  { id: 'bao-mat', ten: 'Bảo mật thông tin', icon: 'bi-shield-lock' },
]

function cuonToi(id) {
  const el = id && document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
onMounted(() => nextTick(() => cuonToi(route.hash?.slice(1))))
watch(() => route.hash, (h) => nextTick(() => cuonToi(h?.slice(1))))
</script>

<template>
  <div class="cs-view py-4">
    <div class="vnb-container">
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Chính sách cửa hàng</li>
        </ol>
      </nav>

      <header class="cs-dau rounded-4 mb-4">
        <h1 class="cs-tieu-de">Chính sách cửa hàng</h1>
        <p class="mb-0">Quy định về giao hàng, thanh toán, đổi trả, đặt hàng và bảo mật khi mua vợt tại FootStyle.</p>
      </header>

      <div class="row g-4">
        <aside class="col-lg-3">
          <nav class="cs-muc-luc" aria-label="Mục lục chính sách">
            <a v-for="m in MUC" :key="m.id" :href="`#${m.id}`" class="cs-muc" @click.prevent="cuonToi(m.id)">
              <i class="bi" :class="m.icon"></i> {{ m.ten }}
            </a>
          </nav>
        </aside>

        <div class="col-lg-9 d-grid gap-4">
          <!-- Giao hàng -->
          <section id="giao-hang" class="cs-the" data-testid="cs-giao-hang">
            <h2 class="cs-muc-ten"><i class="bi bi-truck"></i> Giao hàng</h2>
            <ul class="cs-ds">
              <li>Giao hàng toàn quốc qua <strong>Giao Hàng Nhanh (GHN)</strong>. Nội thành Hà Nội 1-2 ngày, tỉnh khác 3-5 ngày.</li>
              <li v-if="caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0" data-testid="cs-mien-phi">
                <strong>Miễn phí vận chuyển</strong> cho đơn có tiền hàng từ <strong>{{ formatMoney(caiDatShip.mienPhiTu) }}</strong>
                (tiền hàng sau giảm theo đợt, chưa trừ phiếu giảm giá).
              </li>
              <li v-if="caiDatShip.daTai" data-testid="cs-phi-giao">
                <template v-if="caiDatShip.phiGiaoHang > 0 && caiDatShip.tinhTheoKm">
                  {{ caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0 ? 'Chưa đạt mức miễn phí thì phí' : 'Phí' }} giao hàng tính theo quãng đường
                  từ cửa hàng: <strong>{{ formatMoney(caiDatShip.phiGiaoHang) }}</strong> cho {{ caiDatShip.kmCoBan }} km đầu, thêm
                  {{ formatMoney(caiDatShip.phiMoiBuoc) }} mỗi {{ caiDatShip.buocKm }} km tiếp theo.
                </template>
                <template v-else-if="caiDatShip.phiGiaoHang > 0">
                  {{ caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0 ? 'Chưa đạt mức miễn phí thì phí' : 'Phí' }} giao hàng tính theo nơi nhận.
                </template>
                <template v-else>Giao hàng miễn phí cho mọi đơn.</template>
                Phí hiện rõ ở giỏ hàng và trang thanh toán trước khi đặt.
              </li>
              <li>Bạn được kiểm tra hàng trước khi nhận.</li>
            </ul>
          </section>

          <!-- Thanh toán -->
          <section id="thanh-toan" class="cs-the">
            <h2 class="cs-muc-ten"><i class="bi bi-credit-card"></i> Thanh toán</h2>
            <div class="row g-3">
              <div class="col-md-6">
                <div class="cs-o">
                  <h3>Đặt hàng online</h3>
                  <ul class="cs-ds mb-0">
                    <li><strong>Thanh toán khi nhận hàng (COD)</strong>: trả tiền mặt cho nhân viên giao hàng.</li>
                    <li><strong>Chuyển khoản VietQR</strong>: quét mã QR ở trang đặt hàng thành công, số tiền và nội dung tự điền. Cửa hàng xác nhận đơn sau khi nhận được tiền.</li>
                  </ul>
                </div>
              </div>
              <div class="col-md-6">
                <div class="cs-o">
                  <h3>Mua tại cửa hàng</h3>
                  <ul class="cs-ds mb-0">
                    <li>Tiền mặt, chuyển khoản, hoặc kết hợp cả hai.</li>
                    <li>Mua tại cửa hàng vẫn có thể chọn giao hàng tận nơi.</li>
                  </ul>
                </div>
              </div>
            </div>
          </section>

          <!-- Đổi trả & bảo hành -->
          <section id="doi-tra" class="cs-the">
            <h2 class="cs-muc-ten"><i class="bi bi-arrow-repeat"></i> Đổi trả & bảo hành</h2>
            <ul class="cs-ds">
              <li>Đổi sản phẩm trong <strong>7 ngày</strong> nếu lỗi do nhà sản xuất, sản phẩm còn nguyên tem mác và có hóa đơn.</li>
              <li>Vợt chính hãng được bảo hành khung theo chính sách của từng hãng.</li>
              <li>Liên hệ hotline hoặc khung chat trên website để được hướng dẫn đổi trả, bảo hành.</li>
            </ul>
          </section>

          <!-- Đặt hàng & hủy đơn -->
          <section id="dat-hang" class="cs-the">
            <h2 class="cs-muc-ten"><i class="bi bi-bag-check"></i> Đặt hàng & hủy đơn</h2>
            <ul class="cs-ds">
              <li>Sau khi đặt, đơn ở trạng thái <strong>Chờ xác nhận</strong>. Nhân viên kiểm tra hàng trong kho rồi xác nhận và giao cho đơn vị vận chuyển.</li>
              <li>Bạn tự <strong>hủy đơn</strong> được khi đơn còn Chờ xác nhận (trong "Đơn hàng của tôi"). Đơn đã xác nhận muốn hủy thì liên hệ cửa hàng.</li>
              <li>Cần đổi người nhận, địa chỉ hay sản phẩm: liên hệ cửa hàng khi đơn còn Chờ xác nhận để nhân viên chỉnh giúp.</li>
              <li>Đơn đã chuyển khoản mà bị hủy: cửa hàng hoàn lại tiền và gửi email thông báo cho bạn.</li>
              <li>Theo dõi đơn ở <RouterLink :to="{ path: '/shop/tai-khoan', query: { tab: 'orders' } }">Đơn hàng của tôi</RouterLink> (khi đã đăng nhập) hoặc <RouterLink to="/shop/tra-cuu">Tra cứu đơn hàng</RouterLink>.</li>
            </ul>
          </section>

          <!-- Bảo mật -->
          <section id="bao-mat" class="cs-the">
            <h2 class="cs-muc-ten"><i class="bi bi-shield-lock"></i> Bảo mật thông tin</h2>
            <ul class="cs-ds">
              <li>Thông tin họ tên, số điện thoại, địa chỉ chỉ dùng để giao hàng và liên hệ về đơn hàng.</li>
              <li>Tra cứu đơn chỉ bằng mã đơn thì người nhận, số điện thoại, địa chỉ được che; nhập thêm số điện thoại đặt hàng mới xem đủ.</li>
              <li>Mật khẩu tài khoản được mã hóa, cửa hàng không xem được.</li>
            </ul>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-dau {
  padding: 1.75rem 2rem;
  color: #fff;
  background: linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 50%, #60a5fa 100%);
}
.cs-dau p {
  color: #dbeafe;
}
.cs-tieu-de {
  margin: 0 0 0.25rem;
  font-size: 1.7rem;
  font-weight: 800;
}
.cs-muc-luc {
  position: sticky;
  top: 16px;
  display: grid;
  gap: 4px;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--vnb-border);
  border-radius: 14px;
}
.cs-muc {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 10px;
  font-weight: 600;
  color: #334155;
  text-decoration: none;
}
.cs-muc:hover {
  color: var(--vnb-orange);
  background: var(--vnb-orange-light);
}
.cs-muc .bi {
  color: var(--vnb-orange);
}
.cs-the {
  scroll-margin-top: 16px;
  padding: 1.5rem;
  background: #fff;
  border: 1px solid var(--vnb-border);
  border-radius: 16px;
}
.cs-muc-ten {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 1rem;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--vnb-dark);
}
.cs-muc-ten .bi {
  color: var(--vnb-orange);
}
.cs-ds {
  margin: 0 0 1rem;
  padding-left: 1.2rem;
  color: #334155;
  line-height: 1.7;
}
.cs-o {
  height: 100%;
  padding: 1rem 1.1rem;
  background: var(--vnb-orange-light);
  border: 1px solid #dbeafe;
  border-radius: 12px;
}
.cs-o h3 {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  font-weight: 800;
  color: var(--vnb-dark);
}
@media (max-width: 991px) {
  .cs-muc-luc {
    position: static;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}
</style>
