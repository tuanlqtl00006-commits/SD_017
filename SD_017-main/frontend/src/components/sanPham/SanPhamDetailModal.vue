<script setup>
import BaseModal from '../common/BaseModal.vue'
import { TRANG_THAI, trangThaiOf } from '../../constants/thuocTinh'
import { formatDate, formatMoney } from '../../utils/format'

defineProps({
  item: { type: Object, required: true },
  bienThes: { type: Array, default: () => [] }, // biến thể của sản phẩm này
  tenOf: { type: Function, required: true }, // (slug, id) => tên
})
const emit = defineEmits(['close', 'edit'])
</script>

<template>
  <BaseModal title="Chi tiết sản phẩm" size="lg" @close="emit('close')">
    <div class="ad-detail-head">
      <img v-if="item.anhChinh" :src="item.anhChinh" :alt="item.ten" class="rounded-3" style="width: 64px; height: 64px; object-fit: cover" />
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
      <div><dt>Danh mục</dt><dd>{{ tenOf('danh-muc', item.idDanhMuc) }}</dd></div>
      <div><dt>Thương hiệu</dt><dd>{{ tenOf('thuong-hieu', item.idThuongHieu) }}</dd></div>
      <div><dt>Xuất xứ</dt><dd>{{ tenOf('xuat-xu', item.idXuatXu) }}</dd></div>
      <div><dt>Chất liệu</dt><dd>{{ tenOf('chat-lieu', item.idChatLieu) }}</dd></div>
      <div><dt>Độ cứng</dt><dd>{{ tenOf('do-cung', item.idDoCung) }}</dd></div>
      <div><dt>Điểm cân bằng</dt><dd>{{ tenOf('diem-can-bang', item.idDiemCanBang) }}</dd></div>
      <div><dt>Ngày tạo</dt><dd>{{ formatDate(item.ngayTao) }}</dd></div>
      <div><dt>Cập nhật gần nhất</dt><dd>{{ formatDate(item.ngayCapNhat) }}</dd></div>
      <div class="ad-dl-full"><dt>Mô tả</dt><dd>{{ item.moTa || 'Chưa có mô tả.' }}</dd></div>
    </dl>

    <h4 class="ad-card-title mt-4 mb-2">Biến thể ({{ bienThes.length }})</h4>
    <div v-if="!bienThes.length" class="ad-hint">Sản phẩm chưa có biến thể. Thêm trong mục “Biến thể sản phẩm”.</div>
    <div v-else class="ad-table-scroll">
      <table class="ad-table">
        <thead>
          <tr><th>Mã</th><th>Màu sắc</th><th>Trọng lượng</th><th>Chu vi</th><th>Giá bán</th><th>Tồn</th></tr>
        </thead>
        <tbody>
          <tr v-for="b in bienThes" :key="b.id">
            <td><span class="ad-code ad-nowrap">{{ b.ma }}</span></td>
            <td>{{ tenOf('mau-sac', b.idMauSac) }}</td>
            <td>{{ tenOf('trong-luong', b.idTrongLuong) }}</td>
            <td>{{ tenOf('chu-vi', b.idChuVi) }}</td>
            <td class="ad-nowrap">{{ formatMoney(b.giaBan) }}</td>
            <td>{{ b.soLuongTon }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <template #footer>
      <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
      <button type="button" class="ad-btn ad-btn-primary" @click="emit('edit')">
        <i class="bi bi-pencil-square" aria-hidden="true"></i> Chỉnh sửa
      </button>
    </template>
  </BaseModal>
</template>
