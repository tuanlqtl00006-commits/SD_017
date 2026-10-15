<script setup>
import { computed, reactive, ref } from 'vue'
import MoneyInput from '../common/MoneyInput.vue'
import { CHE_DO_CHONG_DOT } from '../../constants/dotGiamGia'
import { formatMoney } from '../../utils/format'
import { formatPhanTram, gopPhanTram, lichGiamGia, nhomTheoBienThe, tinhGiaSauGiam } from '../../utils/giamGia'
import { cungPhut, dinhDangNgayGio, giaoNhau } from '../../utils/ngayGio'

/*
 * Thẻ "Khi nhiều đợt giảm giá chồng nhau thì tính thế nào?" ở trang Thêm / Sửa đợt giảm giá (theo lời cô: phải DEMO được
 * trường hợp trùng đợt - cộng dồn hay lấy mức cao nhất):
 *  1. Cách tính đang dùng (backend cấu hình: MAX / CONG_DON / TRUNG_BINH) + quy tắc chồng với phiếu giảm giá.
 *  2. Ví dụ minh họa chạy được ngay (nhập giá bán + % của hai đợt -> kết quả của cả ba cách, cách đang dùng được tô đậm).
 *  3. CẢNH BÁO TRÙNG ĐỢT thật: liệt kê các đợt khác đang bật, trùng thời gian với đợt đang soạn trên các biến thể đã chọn,
 *     kèm lịch từng biến thể theo ngày giờ (đoạn nào chồng nhau, mức giảm theo từng cách, giá sau giảm).
 */
const props = defineProps({
  // Đợt đang soạn { idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc } - null nếu chưa nhập đủ / sai
  dot: { type: Object, default: null },
  // Biến thể đã chọn [{ id, ma, giaBan, sp: { ten }, mau, tl, cv }]
  bienThes: { type: Array, default: () => [] },
  // Mức giảm của các đợt đang bật, chưa kết thúc (API /dot-giam-gia/ap-dung -> muc), có thể gồm cả chính đợt đang sửa
  mucKhac: { type: Array, default: () => [] },
  cheDo: { type: String, default: 'MAX' },
  moTaCheDo: { type: String, default: '' },
})

const cheDoDung = computed(() => (CHE_DO_CHONG_DOT[props.cheDo] ? props.cheDo : 'MAX'))
const thongTinCheDo = computed(() => CHE_DO_CHONG_DOT[cheDoDung.value])
const moTa = computed(() => props.moTaCheDo || thongTinCheDo.value.moTa)

/* ----- 2. Ví dụ minh họa ----- */
const viDu = reactive({ giaBan: 3000000, dotA: 10, dotB: 15 })
const gioiHan = (x) => Math.min(100, Math.max(0, Number(x) || 0))
const bangViDu = computed(() => {
  const gia = Number(viDu.giaBan) || 0
  const phanTrams = [gioiHan(viDu.dotA), gioiHan(viDu.dotB)]
  return Object.keys(CHE_DO_CHONG_DOT).map((khoa) => {
    const phanTram = gopPhanTram(phanTrams, khoa)
    return { khoa, ...CHE_DO_CHONG_DOT[khoa], phanTram, giaSau: tinhGiaSauGiam(gia, phanTram), dangDung: khoa === cheDoDung.value }
  })
})

/* ----- 3. Trùng đợt thật ----- */
const mucTheoBienThe = computed(() => nhomTheoBienThe(props.mucKhac.filter((m) => m.idDot !== props.dot?.idDot)))
const khungDot = computed(() => (props.dot ? { batDau: props.dot.ngayBatDau, ketThuc: props.dot.ngayKetThuc } : null))

// Các đợt KHÁC trùng thời gian với đợt đang soạn (gom theo đợt, đếm số biến thể đã chọn bị trùng)
const dotTrung = computed(() => {
  const dot = props.dot
  if (!dot) return []
  const theoDot = new Map()
  for (const v of props.bienThes) {
    for (const m of mucTheoBienThe.value.get(v.id) ?? []) {
      if (!giaoNhau(khungDot.value, { batDau: m.ngayBatDau, ketThuc: m.ngayKetThuc })) continue
      let g = theoDot.get(m.idDot)
      if (!g) {
        g = { idDot: m.idDot, maDot: m.maDot, tenDot: m.tenDot, phanTram: m.phanTram, ngayBatDau: m.ngayBatDau, ngayKetThuc: m.ngayKetThuc, soBienThe: 0 }
        theoDot.set(m.idDot, g)
      }
      g.soBienThe += 1
    }
  }
  return [...theoDot.values()].map((g) => ({
    ...g,
    // Cùng mức giảm và cùng khoảng thời gian: đợt này gần như không đem lại gì thêm (trừ khi chọn "cộng dồn")
    giongHet: Number(g.phanTram) === Number(dot.phanTram) && cungPhut(g.ngayBatDau, dot.ngayBatDau) && cungPhut(g.ngayKetThuc, dot.ngayKetThuc),
  }))
})

// Lịch từng biến thể (chỉ những biến thể có đoạn chồng nhau), cắt trong khoảng thời gian của đợt đang soạn
const lichTheoBienThe = computed(() => {
  const dot = props.dot
  if (!dot) return []
  const ketQua = []
  for (const v of props.bienThes) {
    const khoang = lichGiamGia([...(mucTheoBienThe.value.get(v.id) ?? []), dot], cheDoDung.value, khungDot.value)
    if (khoang.some((k) => k.chongNhau)) ketQua.push({ v, khoang })
  }
  return ketQua
})
const hienTatCa = ref(false)
const lichHienThi = computed(() => (hienTatCa.value ? lichTheoBienThe.value : lichTheoBienThe.value.slice(0, 6)))
const soBienTheTrung = computed(() => lichTheoBienThe.value.length)

const nhanBienThe = (v) => [v.ma, v.mau, v.tl, v.cv].filter((x) => x && x !== '—').join(' · ')
const tenDotTrongDoan = (d) => (d.idDot === props.dot?.idDot ? 'Đợt này' : d.maDot)
const cacChip = (k) => [
  { khoa: 'MAX', ngan: 'Cao nhất', giaTri: k.caoNhat },
  { khoa: 'CONG_DON', ngan: 'Cộng dồn', giaTri: k.congDon },
  { khoa: 'TRUNG_BINH', ngan: 'Trung bình', giaTri: k.trungBinh },
].map((c) => ({ ...c, dangDung: c.khoa === cheDoDung.value }))
</script>

<template>
  <section class="ad-card dgg-chong" data-testid="dgg-chong-dot">
    <header class="ad-card-head">
      <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-layers"></i></span>
      <div>
        <h2 class="ad-card-title">Khi nhiều đợt giảm giá chồng nhau thì tính thế nào?</h2>
        <p class="ad-card-sub">Một biến thể có thể nằm trong nhiều đợt cùng lúc. Quy tắc của cửa hàng, ví dụ chạy thử và cảnh báo trùng đợt.</p>
      </div>
    </header>

    <!-- 1. Quy tắc -->
    <div class="dgg-che-do" data-testid="dgg-che-do">
      <span class="dgg-che-do-nhan">Cách tính đang dùng:</span>
      <span class="ad-pill ad-pill-blue" data-testid="dgg-che-do-ten" :data-che-do="cheDoDung">{{ thongTinCheDo.label }}</span>
      <span class="dgg-che-do-mo-ta">{{ moTa }}</span>
    </div>
    <ul class="dgg-quy-tac">
      <li>Đợt giảm giá giảm theo <strong>% trên giá từng biến thể</strong>. Hai đợt chỉ chồng nhau khi cùng áp cho một biến thể trong cùng khoảng ngày giờ.</li>
      <li>
        <strong>Phiếu giảm giá tính sau đợt:</strong> phiếu tính trên tổng tiền hàng đã trừ giảm giá của đợt; mỗi đơn chỉ dùng <strong>1 phiếu</strong> (các phiếu không cộng dồn).
      </li>
      <li>Đổi cách tính ở backend: <code>app.giam-gia.che-do-chong-dot</code> = <code>MAX</code> | <code>CONG_DON</code> | <code>TRUNG_BINH</code>.</li>
    </ul>

    <!-- 2. Ví dụ minh họa -->
    <div class="dgg-vi-du" data-testid="dgg-vi-du">
      <h3 class="dgg-tieu-de-nho">Ví dụ chạy thử: hai đợt cùng áp cho một biến thể</h3>
      <div class="row g-3 align-items-end">
        <div class="col-md-4">
          <label class="ad-label" for="dgg-vd-gia">Giá bán biến thể</label>
          <MoneyInput id="dgg-vd-gia" v-model="viDu.giaBan" aria-label="Giá bán biến thể dùng để thử" />
        </div>
        <div class="col-6 col-md-4">
          <label class="ad-label" for="dgg-vd-dot-a">Đợt A giảm (%)</label>
          <div class="ad-affix">
            <input id="dgg-vd-dot-a" v-model.number="viDu.dotA" type="number" min="0" max="100" step="1" class="form-control ad-control has-suffix" aria-label="Phần trăm giảm của đợt A" />
            <span class="ad-suffix" aria-hidden="true">%</span>
          </div>
        </div>
        <div class="col-6 col-md-4">
          <label class="ad-label" for="dgg-vd-dot-b">Đợt B giảm (%)</label>
          <div class="ad-affix">
            <input id="dgg-vd-dot-b" v-model.number="viDu.dotB" type="number" min="0" max="100" step="1" class="form-control ad-control has-suffix" aria-label="Phần trăm giảm của đợt B" />
            <span class="ad-suffix" aria-hidden="true">%</span>
          </div>
        </div>
      </div>
      <div class="dgg-vd-luoi">
        <div
          v-for="c in bangViDu"
          :key="c.khoa"
          class="dgg-vd-o"
          :class="{ 'is-dung': c.dangDung }"
          :data-testid="'dgg-vd-' + c.khoa"
        >
          <span class="dgg-vd-ten">{{ c.label }} <span v-if="c.dangDung" class="ad-pill ad-pill-green">đang dùng</span></span>
          <strong class="dgg-vd-pt">{{ formatPhanTram(c.phanTram) }}</strong>
          <span class="dgg-vd-gia">{{ formatMoney(c.giaSau) }}</span>
        </div>
      </div>
      <p class="ad-hint mb-0">Hai đợt cùng chạy một lúc: chỉ <strong>một</strong> cách tính được dùng (ô được tô xanh), hai cách còn lại chỉ để so sánh.</p>
    </div>

    <!-- 3. Kiểm tra trùng đợt -->
    <div class="dgg-kiem-tra" data-testid="dgg-kiem-tra-trung">
      <h3 class="dgg-tieu-de-nho">Kiểm tra trùng đợt với đợt đang soạn</h3>
      <p v-if="!dot" class="ad-hint mb-0" data-testid="dgg-trung-chua-du">
        Nhập giá trị giảm (%) và chọn ngày giờ bắt đầu - kết thúc hợp lệ để kiểm tra trùng đợt.
      </p>
      <p v-else-if="!bienThes.length" class="ad-hint mb-0" data-testid="dgg-trung-chua-chon">Chọn biến thể áp dụng để kiểm tra trùng đợt.</p>
      <p v-else-if="!dotTrung.length" class="dgg-khong-trung" data-testid="dgg-khong-trung">
        <i class="bi bi-check-circle" aria-hidden="true"></i>
        Không có biến thể nào trùng thời gian với đợt giảm giá khác - mỗi biến thể chỉ áp dụng mức giảm của đợt này.
      </p>
      <template v-else>
        <div class="dgg-canh-bao" role="status" data-testid="dgg-canh-bao-trung">
          <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
          <div>
            Có <strong>{{ dotTrung.length }}</strong> đợt giảm giá khác trùng thời gian với đợt này trên
            <strong>{{ soBienTheTrung }}</strong> biến thể đã chọn. Ở khoảng thời gian trùng, hệ thống áp dụng:
            <strong data-testid="dgg-canh-bao-che-do">{{ thongTinCheDo.label }}</strong>.
            <ul>
              <li v-for="g in dotTrung" :key="g.idDot" :data-testid="'dgg-dot-trung-' + g.idDot">
                <span class="ad-code">{{ g.maDot }}</span> {{ g.tenDot }} - giảm {{ g.phanTram }}%
                ({{ dinhDangNgayGio(g.ngayBatDau) }} → {{ dinhDangNgayGio(g.ngayKetThuc) }}), trùng {{ g.soBienThe }} biến thể
                <span v-if="g.giongHet" class="ad-pill ad-pill-amber" data-testid="dgg-giong-het">Cùng mức giảm và cùng thời gian</span>
              </li>
            </ul>
          </div>
        </div>

        <div class="dgg-lich-list">
          <div v-for="it in lichHienThi" :key="it.v.id" class="dgg-lich-item" :data-testid="'dgg-lich-' + it.v.id">
            <div class="dgg-lich-head">
              <strong>{{ it.v.sp?.ten }}</strong>
              <span class="dgg-muted">{{ nhanBienThe(it.v) }}</span>
              <span class="dgg-muted">Giá bán {{ formatMoney(it.v.giaBan) }}</span>
            </div>
            <ul class="dgg-lich-rows">
              <li v-for="k in it.khoang" :key="k.tuLuc" :class="{ 'is-overlap': k.chongNhau }" :data-chong-nhau="k.chongNhau">
                <span class="dgg-lich-range">{{ dinhDangNgayGio(k.tuLuc) }} → {{ dinhDangNgayGio(k.denLuc) }}</span>
                <span class="dgg-lich-dots">{{ k.cacDot.map((d) => `${tenDotTrongDoan(d)} (${d.phanTram}%)`).join(' + ') }}</span>
                <span class="dgg-lich-vals">
                  <template v-if="k.chongNhau">
                    <span v-for="c in cacChip(k)" :key="c.khoa" class="dgg-lich-val" :class="{ on: c.dangDung }" :data-che-do="c.khoa">
                      {{ c.ngan }} {{ formatPhanTram(c.giaTri) }}
                    </span>
                  </template>
                  <span v-else class="dgg-lich-val on">{{ formatPhanTram(k.apDung) }}</span>
                </span>
                <span class="dgg-lich-gia" title="Giá sau giảm theo cách đang dùng">{{ formatMoney(tinhGiaSauGiam(it.v.giaBan, k.apDung)) }}</span>
              </li>
            </ul>
          </div>
          <button v-if="lichTheoBienThe.length > 6" type="button" class="ad-btn dgg-lich-more" @click="hienTatCa = !hienTatCa">
            {{ hienTatCa ? 'Thu gọn' : `Xem thêm ${lichTheoBienThe.length - 6} biến thể` }}
          </button>
          <p class="ad-hint mb-0">Hàng nền vàng là đoạn thời gian chồng nhau. Ô màu xanh là mức đang được áp dụng; giá bên phải là giá sau giảm theo cách đó.</p>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.dgg-che-do {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 0.75rem;
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  background: var(--fs-surface, #f5f8fc);
}
.dgg-che-do-nhan {
  font-weight: 600;
  color: var(--fs-text);
}
.dgg-che-do-mo-ta {
  color: var(--fs-text-2);
}
.dgg-quy-tac {
  margin: 0.9rem 0 0;
  padding-left: 1.2rem;
  color: var(--fs-text-2);
  font-size: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.dgg-quy-tac code {
  font-size: 0.85em;
}
.dgg-tieu-de-nho {
  margin: 0 0 0.6rem;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--fs-text);
}
.dgg-vi-du,
.dgg-kiem-tra {
  margin-top: 1.1rem;
  padding-top: 1.1rem;
  border-top: 1px dashed var(--fs-line);
}
.dgg-vd-luoi {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0.9rem 0 0.6rem;
}
@media (max-width: 767.98px) {
  .dgg-vd-luoi {
    grid-template-columns: minmax(0, 1fr);
  }
}
.dgg-vd-o {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  background: #fff;
}
.dgg-vd-o.is-dung {
  border-color: var(--fs-success, #198754);
  background: #f0faf4;
}
.dgg-vd-ten {
  font-size: 0.82rem;
  color: var(--fs-muted);
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.dgg-vd-pt {
  font-size: 1.3rem;
  color: var(--fs-primary-dark);
}
.dgg-vd-gia {
  color: var(--fs-text-2);
  font-size: 0.9rem;
}
.dgg-khong-trung {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--fs-success, #198754);
}
.dgg-canh-bao {
  display: flex;
  gap: 0.7rem;
  padding: 0.8rem 1rem;
  border: 1px solid #f3d9a4;
  border-radius: 12px;
  background: #fff7e6;
  color: #7a5200;
  margin-bottom: 0.9rem;
}
.dgg-canh-bao > i {
  font-size: 1.15rem;
  margin-top: 0.1rem;
}
.dgg-canh-bao ul {
  margin: 0.4rem 0 0;
  padding-left: 1.1rem;
}
.dgg-canh-bao li {
  margin-bottom: 0.2rem;
}
.dgg-muted {
  color: var(--fs-muted);
  font-size: 0.85rem;
}
.dgg-lich-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.dgg-lich-item {
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  padding: 0.75rem 1rem;
}
.dgg-lich-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.25rem 0.75rem;
  margin-bottom: 0.5rem;
}
.dgg-lich-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.dgg-lich-rows li {
  display: grid;
  grid-template-columns: 18rem minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.25rem 1rem;
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  font-size: 0.875rem;
}
.dgg-lich-rows li.is-overlap {
  background: #fff7e6;
}
.dgg-lich-range {
  font-weight: 600;
  white-space: nowrap;
}
.dgg-lich-dots {
  color: var(--fs-text-2);
  overflow-wrap: anywhere;
}
.dgg-lich-vals {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  justify-content: flex-end;
}
.dgg-lich-val {
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  background: #eef1f5;
  color: var(--fs-muted);
  font-size: 0.8rem;
  white-space: nowrap;
}
.dgg-lich-val.on {
  background: var(--fs-success, #198754);
  color: #fff;
  font-weight: 600;
}
.dgg-lich-gia {
  font-weight: 600;
  color: var(--fs-primary-dark);
  white-space: nowrap;
  text-align: right;
}
.dgg-lich-more {
  align-self: flex-start;
}
@media (max-width: 991.98px) {
  .dgg-lich-rows li {
    grid-template-columns: minmax(0, 1fr);
  }
  .dgg-lich-vals {
    justify-content: flex-start;
  }
  .dgg-lich-gia {
    text-align: left;
  }
}
</style>
