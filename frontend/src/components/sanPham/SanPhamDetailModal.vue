<script setup>
import { computed, reactive, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import QrModal from '../common/QrModal.vue'
import { TRANG_THAI, trangThaiOf } from '../../constants/thuocTinh'
import { formatDate, formatMoney } from '../../utils/format'
import { layTen } from '../../utils/text'
import { mauHex } from '../../utils/mau'

// Màn này chỉ HIỂN THỊ dữ liệu cha truyền xuống, không tự tải dữ liệu
const props = defineProps({
  item: { type: Object, required: true }, // sản phẩm đang xem
  bienThes: { type: Array, default: () => [] }, // các biến thể của sản phẩm này
  thuocTinh: { type: Object, required: true }, // các bảng thuộc tính để đổi id thành tên
})
const emit = defineEmits(['close', 'edit'])

// "Không áp dụng" (danh mục không phải vợt) hiện thành dấu gạch cho gọn
const ten = (slug, id) => { const t = layTen(props.thuocTinh[slug], id); return t === 'Không áp dụng' ? '—' : t }

/* ----- Bảng biến thể phẳng có lọc (như video mẫu): màu sắc, trọng lượng, chu vi ----- */
const loc = reactive({ mau: '', tl: '', cv: '' })
const cacGiaTri = (field, slug) => {
  const ids = [...new Set(props.bienThes.map((b) => b[field]))]
  return ids.map((id) => ({ id, ten: ten(slug, id) })).filter((x) => x.ten !== '—')
}
const dsMau = computed(() => cacGiaTri('idMauSac', 'mau-sac'))
const dsTl = computed(() => cacGiaTri('idTrongLuong', 'trong-luong'))
const dsCv = computed(() => cacGiaTri('idChuVi', 'chu-vi'))
const bienTheLoc = computed(() =>
  props.bienThes.filter(
    (b) => (loc.mau === '' || b.idMauSac === loc.mau) && (loc.tl === '' || b.idTrongLuong === loc.tl) && (loc.cv === '' || b.idChuVi === loc.cv),
  ),
)
const dangLoc = computed(() => loc.mau !== '' || loc.tl !== '' || loc.cv !== '')
const datLai = () => Object.assign(loc, { mau: '', tl: '', cv: '' })

/* ----- Mã QR từng biến thể ----- */
const qr = ref(null)
const moTaQr = (b) => [props.item.ten, ten('mau-sac', b.idMauSac), ten('trong-luong', b.idTrongLuong), ten('chu-vi', b.idChuVi)].filter((x) => x && x !== '—').join(' · ')

const anhDaiDien = computed(() => props.item.anhChinh || props.item.anhDaiDien || '')
</script>

<template>
  <BaseModal title="Chi tiết sản phẩm" size="xl" @close="emit('close')">
    <div class="ad-detail-head">
      <img v-if="anhDaiDien" :src="anhDaiDien" :alt="item.ten" class="rounded-3" style="width: 64px; height: 64px; object-fit: cover" />
      <span v-else class="ad-icon-box ad-icon-box-lg" aria-hidden="true"><i class="bi bi-box"></i></span>
      <div>
        <h3 class="ad-detail-name">{{ item.ten }}</h3>
        <div class="ad-detail-sub">
          <span class="ad-code">{{ item.ma }}</span>
          <span class="ad-pill" :class="TRANG_THAI[trangThaiOf(item)].cls">{{ TRANG_THAI[trangThaiOf(item)].label }}</span>
        </div>
      </div>
    </div>

    <dl class="ad-dl">
      <div><dt>Danh mục</dt><dd>{{ layTen(thuocTinh['danh-muc'], item.idDanhMuc) }}</dd></div>
      <div><dt>Thương hiệu</dt><dd>{{ layTen(thuocTinh['thuong-hieu'], item.idThuongHieu) }}</dd></div>
      <div><dt>Xuất xứ</dt><dd>{{ layTen(thuocTinh['xuat-xu'], item.idXuatXu) }}</dd></div>
      <div><dt>Chất liệu</dt><dd>{{ layTen(thuocTinh['chat-lieu'], item.idChatLieu) }}</dd></div>
      <div v-if="layTen(thuocTinh['do-cung'], item.idDoCung) !== '—'"><dt>Độ cứng</dt><dd>{{ layTen(thuocTinh['do-cung'], item.idDoCung) }}</dd></div>
      <div v-if="layTen(thuocTinh['diem-can-bang'], item.idDiemCanBang) !== '—'"><dt>Điểm cân bằng</dt><dd>{{ layTen(thuocTinh['diem-can-bang'], item.idDiemCanBang) }}</dd></div>
      <div><dt>Ngày tạo</dt><dd>{{ formatDate(item.ngayTao) }}</dd></div>
      <div><dt>Cập nhật gần nhất</dt><dd>{{ formatDate(item.ngayCapNhat) }}</dd></div>
      <div class="ad-dl-full"><dt>Mô tả</dt><dd>{{ item.moTa || 'Chưa có mô tả.' }}</dd></div>
    </dl>

    <div v-if="item.anhPhu && item.anhPhu.length" class="d-flex flex-wrap gap-2 mt-3">
      <img v-for="(url, i) in item.anhPhu" :key="i" :src="url" :alt="`${item.ten} - ảnh phụ ${i + 1}`" class="rounded-3" style="width: 72px; height: 72px; object-fit: cover" />
    </div>

    <!-- Ảnh theo màu -->
    <div v-if="item.anhTheoMau && item.anhTheoMau.length" class="mt-3 d-flex flex-column gap-2" data-testid="anh-mau">
      <div v-for="g in item.anhTheoMau" :key="g.idMauSac" class="d-flex align-items-center gap-2 flex-wrap">
        <span class="sp-mau-ten"><span class="sp-dot" :style="{ background: mauHex(ten('mau-sac', g.idMauSac)) }" aria-hidden="true"></span>{{ ten('mau-sac', g.idMauSac) }}</span>
        <img v-for="(url, i) in g.anh" :key="i" :src="url" :alt="`${item.ten} màu ${ten('mau-sac', g.idMauSac)} ${i + 1}`" class="rounded-3 border" style="width: 56px; height: 56px; object-fit: cover" />
      </div>
    </div>

    <h4 class="ad-card-title mt-4 mb-2">Biến thể ({{ bienThes.length }})</h4>
    <div v-if="!bienThes.length" class="ad-hint">Sản phẩm chưa có biến thể. Thêm trong mục “Biến thể sản phẩm”.</div>
    <template v-else>
      <div class="sp-loc" data-testid="loc-bien-the">
        <select v-model="loc.mau" class="form-select ad-control" aria-label="Lọc theo màu sắc">
          <option value="">Tất cả màu sắc</option>
          <option v-for="o in dsMau" :key="o.id" :value="o.id">{{ o.ten }}</option>
        </select>
        <select v-if="dsTl.length" v-model="loc.tl" class="form-select ad-control" aria-label="Lọc theo trọng lượng">
          <option value="">Tất cả trọng lượng</option>
          <option v-for="o in dsTl" :key="o.id" :value="o.id">{{ o.ten }}</option>
        </select>
        <select v-if="dsCv.length" v-model="loc.cv" class="form-select ad-control" aria-label="Lọc theo chu vi">
          <option value="">Tất cả chu vi</option>
          <option v-for="o in dsCv" :key="o.id" :value="o.id">{{ o.ten }}</option>
        </select>
        <button v-if="dangLoc" type="button" class="ad-btn" @click="datLai"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại</button>
        <span class="text-muted small ms-auto">{{ bienTheLoc.length }}/{{ bienThes.length }} biến thể</span>
      </div>
      <div class="ad-table-scroll">
        <table class="ad-table">
          <thead>
            <tr><th>STT</th><th>Mã</th><th class="text-center">Ảnh</th><th>Màu sắc</th><th>Trọng lượng</th><th>Chu vi</th><th>Giá bán</th><th>Tồn</th><th class="text-center">QR</th></tr>
          </thead>
          <tbody>
            <tr v-if="!bienTheLoc.length"><td colspan="9"><div class="ad-empty py-3">Không có biến thể phù hợp bộ lọc.</div></td></tr>
            <tr v-for="(b, i) in bienTheLoc" :key="b.id">
              <td>{{ i + 1 }}</td>
              <td><span class="ad-code ad-nowrap">{{ b.ma }}</span></td>
              <td class="text-center">
                <span class="sp-thumb"><img v-if="b.hinhAnh" :src="b.hinhAnh" :alt="`Ảnh ${b.ma}`" /><i v-else class="bi bi-image" aria-hidden="true"></i></span>
              </td>
              <td class="ad-nowrap"><span class="sp-dot" :style="{ background: mauHex(ten('mau-sac', b.idMauSac)) }" aria-hidden="true"></span>{{ ten('mau-sac', b.idMauSac) }}</td>
              <td class="ad-nowrap">{{ ten('trong-luong', b.idTrongLuong) }}</td>
              <td>{{ ten('chu-vi', b.idChuVi) }}</td>
              <td class="ad-nowrap">{{ formatMoney(b.giaBan) }}</td>
              <td>{{ b.soLuongTon }}</td>
              <td class="text-center">
                <button type="button" class="ad-icon-btn" :title="`Mã QR ${b.ma}`" :aria-label="`Mã QR biến thể ${b.ma}`" @click="qr = b"><i class="bi bi-qr-code" aria-hidden="true"></i></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <template #footer>
      <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
      <button type="button" class="ad-btn ad-btn-primary" @click="emit('edit')">
        <i class="bi bi-pencil-square" aria-hidden="true"></i> Chỉnh sửa
      </button>
    </template>
  </BaseModal>
  <QrModal v-if="qr" :gia-tri="qr.ma" :tieu-de="`Mã QR biến thể ${qr.ma}`" :mo-ta="moTaQr(qr)" :anh="qr.hinhAnh || ''" @close="qr = null" />
</template>

<style scoped>
.sp-loc { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; margin-bottom: 0.75rem; }
.sp-loc .form-select { width: auto; min-width: 160px; }
.sp-dot { display: inline-block; width: 12px; height: 12px; margin-right: 0.4rem; border-radius: 50%; border: 1px solid rgba(15, 23, 42, 0.18); vertical-align: -1px; }
.sp-mau-ten { display: inline-flex; align-items: center; min-width: 90px; font-size: 0.85rem; font-weight: 600; }
.sp-thumb { width: 36px; height: 36px; display: inline-grid; place-items: center; overflow: hidden; border-radius: 8px; border: 1px solid var(--fs-line-soft, #eef1f5); background: var(--fs-surface, #f5f8fc); color: #b8c0cc; }
.sp-thumb img { width: 100%; height: 100%; object-fit: cover; }
</style>
