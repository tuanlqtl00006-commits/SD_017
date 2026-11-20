<script setup>
// Trang Liên hệ: chỉ ghi các kênh có thật (hotline, địa chỉ trụ sở như chân trang, chat tư vấn trực tuyến).
// 15/11 bỏ form "Gửi tin nhắn" (bấm chỉ báo đã gửi, không gửi đi đâu) và danh sách chi nhánh viết sẵn trong code.
// 20/11 (góp ý 19/11 [12:48–13:54]): bỏ những gì cửa hàng không có (đổi trả, email cskh@footstyle.vn không có hộp thư thật),
// thêm "Quy định cửa hàng". Mỗi quy định đều đúng với code đang chạy:
//  - COD / chuyển khoản VietQR, hạn chuyển khoản 5 phút, quá hạn đổi sang COD (CuaHangService.HAN_CHUYEN_KHOAN_PHUT);
//  - giá / phiếu đổi thì báo trước khi đặt (CheckoutView.kiemTraTruocKhiDat); mua tại quầy tiền mặt / chuyển khoản / kết hợp;
//  - phiếu: một phiếu mỗi đơn, đơn tối thiểu, giới hạn lượt mỗi khách, phiếu tặng riêng (PhieuApDung.lyDoKhongDung),
//    hủy đơn trả lại lượt phiếu (HoaDonService.hoanLuotPhieu);
//  - phí giao theo cài đặt của quản lý (dungCaiDatVanChuyen); đổi thông tin nhận 1 lần trước khi giao (HoaDonSuaService);
//  - khách tự hủy khi Chờ xác nhận (KhachTaiKhoanController); hủy đơn đã chuyển khoản thì hoàn tiền + email;
//    hết hàng thì giữ đơn, gọi khách đổi sản phẩm / chờ hàng / hủy (MailService);
//  - sai mật khẩu 5 lần khóa 5 phút (GioiHanDangNhap, cả tài khoản khách).
import { ref } from 'vue'
import BaseModal from '../../components/common/BaseModal.vue'
import { dungCaiDatVanChuyen } from '../../services/caiDatVanChuyen'
import { formatMoney } from '../../utils/format'

const caiDatShip = dungCaiDatVanChuyen()
// "Quy định cửa hàng" là một dòng trong "Có thể bạn đang cần", bấm vào mở hộp thoại (20/11)
const moQuyDinh = ref(false)

function moChat() {
  window.dispatchEvent(new Event('mo-chat-khach'))
}
</script>

<template>
  <div class="lh-trang">
    <div class="vnb-container">
      <nav aria-label="breadcrumb" class="lh-duong-dan">
        <ol class="breadcrumb small mb-0">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Liên hệ</li>
        </ol>
      </nav>

      <section class="lh-banner">
        <h1>Liên hệ với FootStyle</h1>
        <p>Cần tư vấn chọn vợt hay hỏi về đơn hàng? Chọn một cách liên hệ bên dưới.</p>
      </section>

      <div class="lh-luoi">
        <a href="tel:0977508430" class="lh-the" data-testid="lh-hotline">
          <span class="lh-icon is-do"><i class="bi bi-telephone-fill"></i></span>
          <h2>Gọi hotline</h2>
          <p class="lh-gia-tri">0977 508 430</p>
          <p class="lh-phu">8:00 - 21:30 hằng ngày</p>
          <span class="lh-hanh-dong">Gọi ngay <i class="bi bi-arrow-right"></i></span>
        </a>

        <button type="button" class="lh-the" data-testid="lh-chat" @click="moChat">
          <span class="lh-icon"><i class="bi bi-chat-dots-fill"></i></span>
          <h2>Chat tư vấn</h2>
          <p class="lh-gia-tri">Trợ lý ảo trả lời ngay</p>
          <p class="lh-phu">Trong khung chat có nút gặp nhân viên</p>
          <span class="lh-hanh-dong">Mở khung chat <i class="bi bi-arrow-right"></i></span>
        </button>

        <div class="lh-the" data-testid="lh-tru-so">
          <span class="lh-icon is-xanh-la"><i class="bi bi-geo-alt-fill"></i></span>
          <h2>Trụ sở</h2>
          <p class="lh-gia-tri">123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội</p>
          <p class="lh-phu mb-0">Mở cửa 8:00 - 21:30 tất cả các ngày trong tuần</p>
        </div>
      </div>

      <div class="lh-khoi">
        <h2>Có thể bạn đang cần</h2>
        <div class="lh-lien-ket">
          <RouterLink to="/shop/tra-cuu"><i class="bi bi-search"></i> Tra cứu đơn hàng</RouterLink>
          <RouterLink to="/shop/uu-dai"><i class="bi bi-ticket-perforated"></i> Ưu đãi và mã giảm giá</RouterLink>
          <button type="button" data-testid="lh-mo-quy-dinh" @click="moQuyDinh = true">
            <i class="bi bi-journal-text"></i> Quy định cửa hàng
          </button>
        </div>
      </div>

      <BaseModal v-if="moQuyDinh" title="Quy định cửa hàng" size="xl" @close="moQuyDinh = false">
        <div data-testid="lh-quy-dinh">
          <p class="lh-phu">Áp dụng khi mua vợt tại FootStyle, cả đặt online và mua tại cửa hàng.</p>
          <div class="lh-quy-dinh">
            <div class="lh-nhom">
              <h3>Đặt hàng và thanh toán</h3>
              <ul>
                <li>Đặt online: thanh toán khi nhận hàng (COD) hoặc chuyển khoản VietQR.</li>
                <li>Chuyển khoản có <strong>5 phút</strong> để quét mã QR; quá hạn chưa trả thì bạn đổi được sang COD.</li>
                <li>Giá theo đợt giảm giá đang chạy lúc đặt. Giá hoặc phiếu giảm giá vừa đổi thì trang thanh toán báo trước khi đặt.</li>
                <li>Mua tại cửa hàng: tiền mặt, chuyển khoản hoặc kết hợp cả hai.</li>
              </ul>
            </div>

            <div class="lh-nhom">
              <h3>Phiếu giảm giá</h3>
              <ul>
                <li>Mỗi đơn dùng một phiếu, tiền hàng phải đạt mức tối thiểu ghi trên phiếu.</li>
                <li>Có phiếu giới hạn số lần dùng cho mỗi khách. Phiếu tặng riêng chỉ dùng được khi đăng nhập đúng tài khoản được tặng.</li>
                <li>Đơn bị hủy thì lượt dùng phiếu được trả lại.</li>
              </ul>
            </div>

            <div class="lh-nhom" data-testid="lh-qd-giao-hang">
              <h3>Giao hàng</h3>
              <ul>
                <li v-if="caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0">
                  Miễn phí giao hàng cho đơn có tiền hàng từ <strong>{{ formatMoney(caiDatShip.mienPhiTu) }}</strong>.
                </li>
                <li v-if="caiDatShip.daTai">
                  <template v-if="caiDatShip.phiGiaoHang > 0 && caiDatShip.tinhTheoKm">
                    {{ caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0 ? 'Chưa đạt mức miễn phí thì phí' : 'Phí' }} giao tính theo quãng đường từ
                    cửa hàng: {{ formatMoney(caiDatShip.phiGiaoHang) }} cho {{ caiDatShip.kmCoBan }} km đầu, thêm {{ formatMoney(caiDatShip.phiMoiBuoc) }}
                    mỗi {{ caiDatShip.buocKm }} km tiếp theo.
                  </template>
                  <template v-else-if="caiDatShip.phiGiaoHang > 0">
                    {{ caiDatShip.mienPhiBat && caiDatShip.mienPhiTu > 0 ? 'Chưa đạt mức miễn phí thì phí' : 'Phí' }} giao tính theo nơi nhận.
                  </template>
                  <template v-else>Giao hàng miễn phí cho mọi đơn.</template>
                  Phí hiện ở giỏ hàng và trang thanh toán trước khi đặt.
                </li>
                <li>Đổi người nhận, số điện thoại hoặc địa chỉ được <strong>1 lần</strong>, trước khi cửa hàng giao đơn cho đơn vị vận chuyển.</li>
              </ul>
            </div>

            <div class="lh-nhom">
              <h3>Hủy đơn và hoàn tiền</h3>
              <ul>
                <li>Bạn tự hủy đơn khi đơn còn <strong>Chờ xác nhận</strong>. Đơn đã xác nhận muốn hủy thì liên hệ cửa hàng.</li>
                <li>Đơn đã chuyển khoản mà bị hủy: cửa hàng hoàn lại tiền và gửi email báo cho bạn.</li>
                <li>Sản phẩm hết hàng sau khi bạn đặt: cửa hàng giữ đơn và gọi cho bạn để đổi sản phẩm khác, chờ hàng về hoặc hủy đơn.</li>
              </ul>
            </div>

            <div class="lh-nhom">
              <h3>Tài khoản</h3>
              <ul>
                <li>Nhập sai mật khẩu <strong>5 lần</strong> liên tiếp thì tài khoản tạm khóa đăng nhập <strong>5 phút</strong>.</li>
              </ul>
            </div>
          </div>
        </div>
        <template #footer>
          <button type="button" class="btn btn-primary rounded-pill px-4" @click="moQuyDinh = false">Đã hiểu</button>
        </template>
      </BaseModal>
    </div>
  </div>
</template>

<style scoped>
.lh-trang {
  padding: 18px 0 72px;
  background: var(--ft-bg, #f6f7f9);
}
.lh-duong-dan {
  margin-bottom: 14px;
}
.lh-banner {
  margin-bottom: 24px;
  padding: 40px;
  color: #fff;
  background: radial-gradient(120% 160% at 85% 15%, #2547b8 0%, #0b1b3f 70%);
  border-radius: 24px;
}
.lh-banner h1 {
  margin: 0 0 8px;
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #fff;
}
.lh-banner p {
  margin: 0;
  color: rgba(255, 255, 255, 0.75);
}
.lh-luoi {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}
.lh-the {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 26px;
  text-align: left;
  color: inherit;
  text-decoration: none;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.lh-the:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}
.lh-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin-bottom: 18px;
  font-size: 1.35rem;
  color: var(--ft-primary, #1d4ed8);
  background: #eff6ff;
  border-radius: 16px;
}
.lh-icon.is-do {
  color: #e11d48;
  background: #fff1f2;
}
.lh-icon.is-xanh-la {
  color: #15803d;
  background: #f0fdf4;
}
.lh-the h2,
.lh-khoi h2 {
  margin: 0 0 6px;
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--ft-ink, #0f172a);
}
.lh-gia-tri {
  margin: 0 0 4px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ft-ink-2, #334155);
}
.lh-phu {
  margin: 0 0 18px;
  font-size: 0.88rem;
  color: #64748b;
}
.lh-hanh-dong {
  margin-top: auto;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ft-primary, #1d4ed8);
}
.lh-quy-dinh {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.lh-nhom {
  padding: 20px 22px;
  background: #f8fafc;
  border-radius: 16px;
}
.lh-nhom h3 {
  margin: 0 0 10px;
  font-size: 1.12rem;
  font-weight: 800;
  color: var(--ft-ink, #0f172a);
}
.lh-nhom ul {
  margin: 0;
  padding-left: 1.2rem;
  font-size: 1rem;
  line-height: 1.6;
  color: var(--ft-ink-2, #334155);
}
.lh-nhom li + li {
  margin-top: 6px;
}
.lh-khoi {
  padding: 26px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.lh-lien-ket {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}
.lh-lien-ket a,
.lh-lien-ket button {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  font: inherit;
  font-weight: 600;
  text-align: left;
  color: var(--ft-ink, #0f172a);
  text-decoration: none;
  background: #f8fafc;
  border: 0;
  border-radius: 12px;
}
.lh-lien-ket a:hover,
.lh-lien-ket button:hover {
  color: var(--ft-primary, #1d4ed8);
  background: #eff6ff;
}
@media (max-width: 991px) {
  .lh-luoi,
  .lh-lien-ket,
  .lh-quy-dinh {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 575px) {
  .lh-banner {
    padding: 26px 22px;
    border-radius: 20px;
  }
  .lh-the,
  .lh-khoi {
    padding: 20px;
    border-radius: 20px;
  }
}
</style>
