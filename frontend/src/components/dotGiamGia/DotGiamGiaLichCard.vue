<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { formatMoney } from '../../utils/format'

/*
 * Lịch giảm giá theo KHOẢNG NGÀY của từng biến thể trong một đợt (dữ liệu tính ở backend: LichGiamGiaService).
 * Khi nhiều đợt cùng áp dụng cho một biến thể trong cùng một ngày thì backend gộp theo cách đã cấu hình
 * (app.giam-gia.che-do-chong-dot: MAX = mức cao nhất, mặc định; TRUNG_BINH = trung bình cộng);
 * cả mức cao nhất và trung bình cộng của các đợt chồng nhau đều hiển thị bên cạnh để đối chiếu.
 * Thay đổi gì ở đợt (ngày, %, biến thể) thì bấm "Tải lại" hoặc lưu xong vào lại trang để thấy lịch mới.
 */
const props = defineProps({
  idDot: { type: Number, required: true },
  dotDangTat: { type: Boolean, default: false },
})

const lich = ref([])
const dangTai = ref(true)
const loi = ref('')

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
}
onMounted(tai)
watch(() => props.idDot, tai)

// 'yyyy-mm-dd' -> 'd/m/yyyy'
const ngay = (iso) => {
  const [y, m, d] = String(iso).split('-')
  return `${Number(d)}/${Number(m)}/${y}`
}
const soBienThe = computed(() => lich.value.length)
const coChong = (doan) => doan.cacDot.length > 1
</script>

<template>
  <section class="ad-card">
    <header class="ad-card-head">
      <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-calendar2-range"></i></span>
      <div class="flex-grow-1">
        <h2 class="ad-card-title">Lịch giảm giá theo ngày</h2>
        <p class="ad-card-sub">
          Mức giảm thật sự của từng biến thể trong khoảng ngày của đợt. Nhiều đợt chồng nhau thì tự gộp theo cấu hình (mặc định lấy mức giảm cao nhất, có thể đổi sang trung bình cộng).
        </p>
      </div>
      <button type="button" class="ad-btn" :disabled="dangTai" @click="tai">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Tải lại
      </button>
    </header>

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
                <th>Từ ngày</th>
                <th>Đến ngày</th>
                <th>Đợt áp dụng</th>
                <th>Mức giảm</th>
                <th>Giá sau giảm</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!b.doan.length">
                <td colspan="5" class="lich-empty">Không có đoạn giảm giá nào (đợt đang tắt hoặc đã bị gỡ).</td>
              </tr>
              <tr v-for="d in b.doan" :key="d.tuNgay">
                <td class="ad-nowrap">{{ ngay(d.tuNgay) }}</td>
                <td class="ad-nowrap">{{ ngay(d.denNgay) }}</td>
                <td>
                  <div v-for="x in d.cacDot" :key="x.idDot" class="lich-dot">
                    <span class="ad-code">{{ x.maDot }}</span> {{ x.phanTram }}%
                  </div>
                </td>
                <td class="ad-nowrap">
                  <strong class="lich-pct">{{ d.phanTramApDung }}%</strong>
                  <div v-if="coChong(d)" class="lich-sub">
                    chồng {{ d.cacDot.length }} đợt · cao nhất {{ d.phanTramCaoNhat }}% · TB cộng {{ d.phanTramTrungBinh }}%
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
</style>
