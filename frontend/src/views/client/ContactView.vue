<script setup>
// Trang Liên hệ: chỉ ghi các kênh có thật (hotline, email, địa chỉ trụ sở như chân trang, chat tư vấn trực tuyến).
// 15/11 bỏ form "Gửi tin nhắn" (bấm chỉ báo đã gửi, không gửi đi đâu) và danh sách chi nhánh viết sẵn trong code.
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
        <p>Cần tư vấn chọn vợt, hỏi về đơn hàng hay đổi trả? Chọn một cách liên hệ bên dưới.</p>
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

        <a href="mailto:cskh@footstyle.vn" class="lh-the" data-testid="lh-email">
          <span class="lh-icon is-xanh-la"><i class="bi bi-envelope-fill"></i></span>
          <h2>Gửi email</h2>
          <p class="lh-gia-tri">cskh@footstyle.vn</p>
          <p class="lh-phu">Mở ứng dụng email trên máy bạn</p>
          <span class="lh-hanh-dong">Viết email <i class="bi bi-arrow-right"></i></span>
        </a>
      </div>

      <div class="lh-duoi">
        <div class="lh-khoi">
          <h2>Trụ sở</h2>
          <p class="lh-dia-chi mb-0"><i class="bi bi-geo-alt-fill"></i> 123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội</p>
        </div>
        <div class="lh-khoi">
          <h2>Có thể bạn đang cần</h2>
          <div class="lh-lien-ket">
            <RouterLink to="/shop/tra-cuu"><i class="bi bi-search"></i> Tra cứu đơn hàng</RouterLink>
            <RouterLink to="/shop/chinh-sach#doi-tra"><i class="bi bi-arrow-repeat"></i> Chính sách đổi trả</RouterLink>
            <RouterLink to="/shop/chinh-sach#giao-hang"><i class="bi bi-truck"></i> Phí và thời gian giao hàng</RouterLink>
          </div>
        </div>
      </div>
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
.lh-duoi {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.lh-khoi {
  padding: 26px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.lh-dia-chi {
  margin: 10px 0 6px;
  font-weight: 600;
  color: var(--ft-ink-2, #334155);
}
.lh-dia-chi i {
  margin-right: 6px;
  color: #e11d48;
}
.lh-lien-ket {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}
.lh-lien-ket a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  font-weight: 600;
  color: var(--ft-ink, #0f172a);
  text-decoration: none;
  background: #f8fafc;
  border-radius: 12px;
}
.lh-lien-ket a:hover {
  color: var(--ft-primary, #1d4ed8);
  background: #eff6ff;
}
@media (max-width: 991px) {
  .lh-luoi,
  .lh-duoi {
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
