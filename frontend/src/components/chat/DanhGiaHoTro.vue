<script setup>
// Tổng hợp đánh giá của khách sau khi chat với nhân viên (góp ý 16/11 [12:30–12:59]): đếm Tốt / Bình thường / Chưa tốt,
// đã / chưa giải quyết, theo từng nhân viên; lọc theo ngày, mức độ, nhân viên; bấm "Xem chat" mở đúng cuộc trò chuyện.
// Dùng chung cho trang "Đánh giá hỗ trợ" của quản lý và tab "Đánh giá" trong màn Hỗ trợ trực tuyến.
import { onMounted, reactive, ref, watch } from 'vue'
import DateInput from '../common/DateInput.vue'
import { hoTroApi } from '../../services/chatService'
import { formatNgayGio } from '../../utils/format'

const emit = defineEmits(['mo'])

const MUC = [
  { ma: 'TOT', nhan: 'Tốt', icon: 'bi-emoji-smile', lop: 'is-tot' },
  { ma: 'BINH_THUONG', nhan: 'Bình thường', icon: 'bi-emoji-neutral', lop: 'is-vua' },
  { ma: 'CHUA_TOT', nhan: 'Chưa tốt', icon: 'bi-emoji-frown', lop: 'is-xau' },
]
const lopMuc = (ma) => MUC.find((m) => m.ma === ma)?.lop ?? ''

const loc = reactive({ tuNgay: '', denNgay: '', mucDo: '', idNhanVien: '' })
const kq = ref(null)
const dangTai = ref(true)
const loi = ref('')

let lan = 0
async function tai() {
  if (loc.tuNgay && loc.denNgay && loc.tuNgay > loc.denNgay) {
    loi.value = '"Từ ngày" phải trước hoặc bằng "Đến ngày".'
    return
  }
  const lanNay = ++lan
  dangTai.value = true
  loi.value = ''
  try {
    const params = {}
    for (const [k, v] of Object.entries(loc)) if (v !== '' && v !== null) params[k] = v
    const d = await hoTroApi.danhGia(params)
    if (lanNay === lan) kq.value = d
  } catch (e) {
    if (lanNay === lan) loi.value = e.message || 'Không tải được đánh giá.'
  } finally {
    if (lanNay === lan) dangTai.value = false
  }
}
onMounted(tai)
watch(() => ({ ...loc }), tai)

function datLai() {
  Object.assign(loc, { tuNgay: '', denNgay: '', mucDo: '', idNhanVien: '' })
}
const phanTram = (n) => (kq.value?.tong ? Math.round((n / kq.value.tong) * 100) : 0)
</script>

<template>
  <section class="ad-card dgh" data-testid="dgh">
    <header class="dgh-dau">
      <div class="ad-card-head">
        <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-star-half"></i></span>
        <h2 class="ad-card-title">Đánh giá của khách sau khi chat</h2>
      </div>
      <p class="dgh-mo-ta mb-0">Khách chấm sau mỗi cuộc trò chuyện có nhân viên hỗ trợ.</p>
    </header>

    <div class="dgh-loc">
      <div>
        <label class="ad-label" for="dgh-tu">Từ ngày</label>
        <DateInput id="dgh-tu" v-model="loc.tuNgay" class="form-control ad-control" :max="loc.denNgay || undefined" />
      </div>
      <div>
        <label class="ad-label" for="dgh-den">Đến ngày</label>
        <DateInput id="dgh-den" v-model="loc.denNgay" class="form-control ad-control" :min="loc.tuNgay || undefined" />
      </div>
      <div>
        <label class="ad-label" for="dgh-muc">Mức độ</label>
        <select id="dgh-muc" v-model="loc.mucDo" class="form-select ad-control" data-testid="dgh-loc-muc">
          <option value="">Tất cả mức</option>
          <option v-for="m in MUC" :key="m.ma" :value="m.ma">{{ m.nhan }}</option>
        </select>
      </div>
      <div>
        <label class="ad-label" for="dgh-nv">Nhân viên</label>
        <select id="dgh-nv" v-model="loc.idNhanVien" class="form-select ad-control" data-testid="dgh-loc-nv">
          <option value="">Tất cả nhân viên</option>
          <option v-for="n in kq?.nhanVien ?? []" :key="n.id" :value="n.id">{{ n.hoTen }}</option>
        </select>
      </div>
      <button type="button" class="ad-btn dgh-dat-lai" @click="datLai">
        <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại
      </button>
    </div>
    <p v-if="loi" class="ad-error mb-0" role="alert">{{ loi }}</p>

    <div v-if="kq" class="dgh-the" data-testid="dgh-dem">
      <button
        v-for="m in MUC"
        :key="m.ma"
        type="button"
        class="dgh-o"
        :class="[m.lop, { 'is-chon': loc.mucDo === m.ma }]"
        :aria-pressed="loc.mucDo === m.ma"
        @click="loc.mucDo = loc.mucDo === m.ma ? '' : m.ma"
      >
        <i class="bi" :class="m.icon" aria-hidden="true"></i>
        <span class="dgh-o-nhan">{{ m.nhan }}</span>
        <b class="dgh-o-so">{{ kq.dem?.[m.ma] ?? 0 }}</b>
        <small>{{ phanTram(kq.dem?.[m.ma] ?? 0) }}%</small>
      </button>
      <div class="dgh-o is-giai-quyet">
        <i class="bi bi-check2-circle" aria-hidden="true"></i>
        <span class="dgh-o-nhan">Đã giải quyết</span>
        <b class="dgh-o-so">{{ kq.daGiaiQuyet }}/{{ kq.tong }}</b>
        <small>Chưa giải quyết: {{ kq.chuaGiaiQuyet }}</small>
      </div>
    </div>

    <div v-if="kq?.theoNhanVien?.length" class="ad-table-wrap dgh-bang-nv">
      <table class="ad-table" data-testid="dgh-theo-nv">
        <thead>
          <tr>
            <th>Nhân viên</th>
            <th class="text-center">Số đánh giá</th>
            <th v-for="m in MUC" :key="m.ma" class="text-center">{{ m.nhan }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="n in kq.theoNhanVien" :key="n.idNhanVien ?? 'khong-ro'">
            <td class="fw-semibold">{{ n.tenNhanVien }}</td>
            <td class="text-center">{{ n.tong }}</td>
            <td v-for="m in MUC" :key="m.ma" class="text-center"><span :class="n[m.ma] ? m.lop : 'text-muted'">{{ n[m.ma] }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="ad-table-wrap">
      <div class="ad-table-scroll">
        <table class="ad-table" data-testid="dgh-ds">
          <thead>
            <tr>
              <th>Thời gian</th>
              <th>Khách</th>
              <th>Nhân viên</th>
              <th class="text-center">Mức độ</th>
              <th class="text-center">Giải quyết</th>
              <th>Góp ý</th>
              <th class="text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="dangTai && !kq">
              <td colspan="7"><div class="ad-empty">Đang tải…</div></td>
            </tr>
            <tr v-else-if="!kq?.danhGia?.length">
              <td colspan="7">
                <div class="ad-empty">
                  <i class="bi bi-chat-square-heart" aria-hidden="true"></i>
                  <strong>Chưa có đánh giá</strong>
                  <span>Không có đánh giá nào khớp bộ lọc.</span>
                </div>
              </td>
            </tr>
            <tr v-for="d in kq?.danhGia ?? []" :key="d.idTinNhan">
              <td class="ad-nowrap">{{ formatNgayGio(d.thoiGian) }}</td>
              <td>
                <span class="d-block fw-semibold">{{ d.tenKhach }}</span>
                <small class="text-muted">{{ d.sdtKhach }}</small>
              </td>
              <td>{{ d.tenNhanVien || '—' }}</td>
              <td class="text-center"><span class="dgh-pill" :class="lopMuc(d.mucDo)">{{ d.nhanMucDo }}</span></td>
              <td class="text-center">
                <span v-if="d.daGiaiQuyet" class="text-success"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Đã giải quyết</span>
                <span v-else class="text-danger"><i class="bi bi-x-circle-fill" aria-hidden="true"></i> Chưa</span>
              </td>
              <td class="dgh-gop-y">{{ d.gopY || '—' }}</td>
              <td class="text-center">
                <button type="button" class="ad-btn ad-btn-sm" data-testid="dgh-xem-chat" @click="emit('mo', d.idHoiThoai)">
                  <i class="bi bi-chat-text" aria-hidden="true"></i> Xem chat
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.dgh {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.dgh-dau {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.dgh-mo-ta {
  font-size: 0.82rem;
  color: #64748b;
}
.dgh-loc {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr)) auto;
  gap: 0.75rem;
  align-items: end;
}
.dgh-loc .ad-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.3rem;
}
.dgh-dat-lai {
  height: 38px;
}
.dgh-the {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}
.dgh-o {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  column-gap: 0.6rem;
  align-items: center;
  text-align: left;
  padding: 0.8rem 1rem;
  border-radius: 14px;
  border: 1px solid #e6eaf0;
  background: #fff;
}
button.dgh-o {
  cursor: pointer;
}
.dgh-o > i {
  grid-row: 1 / 3;
  font-size: 1.5rem;
}
.dgh-o-nhan {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
}
.dgh-o-so {
  font-size: 1.35rem;
  font-variant-numeric: tabular-nums;
}
.dgh-o small {
  grid-column: 2;
  font-size: 0.74rem;
  color: #64748b;
}
.dgh-o.is-chon {
  box-shadow: 0 0 0 2px currentColor inset;
}
.is-tot {
  color: #15803d;
}
.is-vua {
  color: #b45309;
}
.is-xau {
  color: #be123c;
}
.is-giai-quyet {
  color: #0977ec;
}
.dgh-pill {
  display: inline-block;
  padding: 0.18rem 0.6rem;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
}
.dgh-pill.is-tot {
  background: #dcfce7;
}
.dgh-pill.is-vua {
  background: #fef3c7;
}
.dgh-pill.is-xau {
  background: #ffe4e6;
}
.dgh-gop-y {
  max-width: 280px;
  white-space: pre-line;
}
@media (max-width: 991px) {
  .dgh-loc,
  .dgh-the {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
