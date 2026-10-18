<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { CHE_DO_CHONG_DOT } from '../../constants/dotGiamGia'
import { formatMoney } from '../../utils/format'
import { formatPhanTram } from '../../utils/giamGia'
import { dinhDangNgayGio } from '../../utils/ngayGio'

/*
 * Lịch giảm giá theo KHOẢNG NGÀY GIỜ của từng biến thể trong một đợt (dữ liệu tính ở backend: LichGiamGiaService).
 * Khi nhiều đợt cùng áp dụng cho một biến thể trong cùng một khoảng thời gian thì backend gộp theo cách đã cấu hình
 * (app.giam-gia.che-do-chong-dot: MAX = mức cao nhất, mặc định; CONG_DON = cộng dồn, tối đa 100%; TRUNG_BINH = trung bình cộng);
 * mức của cả ba cách đều hiển thị bên cạnh để đối chiếu, cách đang dùng được tô xanh.
 * Thay đổi gì ở đợt (ngày giờ, %, biến thể) thì bấm "Tải lại" hoặc lưu xong vào lại trang để thấy lịch mới.
 */
const props = defineProps({
  idDot: { type: Number, required: true },
  dotDangTat: { type: Boolean, default: false },
})

const lich = ref([])
const dangTai = ref(true)
const loi = ref('')
const cheDo = ref('MAX') // cách gộp đang dùng (backend cấu hình)
const moTaCheDo = ref('')

async function tai() {
  dangTai.value = true
  loi.value = ''
  try {
    lich.value = await dotGiamGiaService.getLichGiamGia(props.idDot)
  } catch (e) {
    loi.value = e.message || 'Không tải được lịch giảm giá.'
  } finally {
    dangTai.value = false
  }
  try {
    const kq = await dotGiamGiaService.cheDo()
    if (kq?.cheDo) {
      cheDo.value = kq.cheDo
      moTaCheDo.value = kq.moTa || ''
    }
  } catch {
    // Backend cũ chưa có API này: chỉ không hiện dòng cách tính
  }
}
onMounted(tai)
watch(() => props.idDot, tai)

const soBienThe = computed(() => lich.value.length)
const coChong = (doan) => doan.cacDot.length > 1
const thongTinCheDo = computed(() => CHE_DO_CHONG_DOT[cheDo.value] ?? CHE_DO_CHONG_DOT.MAX)
// Ba mức của đoạn chồng nhau; cách đang dùng được tô xanh
const cacMuc = (d) => [
  { khoa: 'MAX', ngan: 'Cao nhất', giaTri: d.phanTramCaoNhat },
  { khoa: 'CONG_DON', ngan: 'Cộng dồn', giaTri: d.phanTramCongDon },
  { khoa: 'TRUNG_BINH', ngan: 'Trung bình', giaTri: d.phanTramTrungBinh },
].filter((m) => m.giaTri !== undefined && m.giaTri !== null)
</script>

<template>
  <section class="ad-card">
    <header class="ad-card-head">
      <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-calendar2-range"></i></span>
      <h2 class="ad-card-title flex-grow-1">Lịch giảm giá theo ngày giờ</h2>
      <button type="button" class="ad-btn" :disabled="dangTai" @click="tai">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Tải lại
      </button>
    </header>

    <p class="lich-che-do" data-testid="lich-che-do">
      Cách tính khi chồng đợt: <span class="ad-pill ad-pill-blue" :data-che-do="cheDo">{{ thongTinCheDo.label }}</span>
      <span v-if="moTaCheDo" class="lich-che-do-mo-ta">{{ moTaCheDo }}</span>
    </p>
    <p v-if="dotDangTat" class="ad-hint lich-note">
      <i class="bi bi-info-circle" aria-hidden="true"></i>
      Đợt này đang tắt nên không được tính vào lịch (chỉ các đợt đang bật mới giảm giá).
    </p>
    <div v-if="dangTai" class="ad-empty">Đang tải lịch giảm giá…</div>
    <div v-else-if="loi" class="ad-empty">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <strong>{{ loi }}</strong>
    </div>
    <div v-else-if="!soBienThe" class="ad-empty">
      <i class="bi bi-inbox" aria-hidden="true"></i>
      <strong>Đợt chưa có biến thể nào đang áp dụng</strong>
    </div>

    <div v-else class="lich-list">
      <article v-for="b in lich" :key="b.idBienThe" class="lich-item">
        <header class="lich-head">
          <span class="ad-code">{{ b.maSpct }}</span>
          <span class="lich-name">{{ b.tenSanPham }}</span>
          <span class="lich-price">Giá gốc {{ formatMoney(b.giaBan) }}</span>
        </header>
        <div class="ad-table-scroll">
          <table class="ad-table lich-table">
            <thead>
              <tr>
                <th>Từ lúc</th>
                <th>Đến lúc</th>
                <th>Đợt áp dụng</th>
                <th>Mức giảm</th>
                <th>Giá sau giảm</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!b.doan.length">
                <td colspan="5" class="lich-empty">Không có đoạn giảm giá nào (đợt đang tắt hoặc đã bị gỡ).</td>
              </tr>
              <tr v-for="d in b.doan" :key="d.tuLuc">
                <td class="ad-nowrap">{{ dinhDangNgayGio(d.tuLuc) }}</td>
                <td class="ad-nowrap">{{ dinhDangNgayGio(d.denLuc) }}</td>
                <td>
                  <div v-for="x in d.cacDot" :key="x.idDot" class="lich-dot">
                    <span class="ad-code">{{ x.maDot }}</span> {{ x.phanTram }}%
                  </div>
                </td>
                <td class="ad-nowrap">
                  <strong class="lich-pct">{{ formatPhanTram(d.phanTramApDung) }}</strong>
                  <div v-if="coChong(d)" class="lich-sub">
                    chồng {{ d.cacDot.length }} đợt:
                    <span v-for="m in cacMuc(d)" :key="m.khoa" class="lich-muc" :class="{ on: m.khoa === cheDo }" :data-che-do="m.khoa">{{ m.ngan }} {{ formatPhanTram(m.giaTri) }}</span>
                  </div>
                </td>
                <td class="ad-nowrap"><strong class="lich-sale">{{ formatMoney(d.giaSauGiam) }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.lich-list {
  display: grid;
  gap: 0.9rem;
}
.lich-item {
  border: 1px solid var(--fs-line);
  border-radius: 14px;
  overflow: hidden;
}
.lich-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.9rem;
  background: var(--fs-surface);
  border-bottom: 1px solid var(--fs-line);
}
.lich-name {
  font-weight: 600;
  color: var(--fs-navy);
}
.lich-price {
  margin-left: auto;
  color: var(--fs-text-2);
  font-size: 0.85rem;
}
.lich-table {
  margin: 0;
}
.lich-dot {
  font-size: 0.85rem;
  white-space: nowrap;
}
.lich-pct {
  color: var(--fs-primary-dark);
}
.lich-sale {
  color: var(--fs-success);
}
.lich-sub {
  font-size: 0.75rem;
  color: var(--fs-muted);
}
.lich-empty {
  text-align: center;
  color: var(--fs-muted);
  padding: 0.9rem;
}
.lich-note {
  margin: 0 0 0.75rem;
}
.lich-che-do {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem 0.6rem;
  margin: 0 0 0.75rem;
  font-size: 0.9rem;
}
.lich-che-do-mo-ta {
  color: var(--fs-text-2);
}
.lich-muc {
  display: inline-block;
  margin-right: 0.35rem;
  padding: 0 0.45rem;
  border-radius: 999px;
  background: #eef1f5;
}
.lich-muc.on {
  background: var(--fs-success, #198754);
  color: #fff;
  font-weight: 600;
}
</style>
