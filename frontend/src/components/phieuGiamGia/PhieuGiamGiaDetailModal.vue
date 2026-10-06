<script setup>
import BaseModal from '../common/BaseModal.vue'
import { HINH_THUC, LOAI_GIAM, TRANG_THAI, formatGiaTri } from '../../constants/phieuGiamGia'
import { formatDate, formatMoney } from '../../utils/format'

defineProps({
  item: { type: Object, required: true },
})
const emit = defineEmits(['close', 'edit'])
</script>

<template>
  <BaseModal title="Chi tiết phiếu giảm giá" size="lg" @close="emit('close')">
    <div class="ad-detail-head">
      <span class="ad-icon-box ad-icon-box-lg" aria-hidden="true"><i class="bi bi-ticket-perforated"></i></span>
      <div>
        <h3 class="ad-detail-name">{{ item.ten }}</h3>
        <div class="ad-detail-sub">
          <span class="ad-code">{{ item.ma }}</span>
          <span class="ad-pill" :class="HINH_THUC[item.hinhThuc].cls">
            <i class="bi" :class="HINH_THUC[item.hinhThuc].icon" aria-hidden="true"></i>
            {{ HINH_THUC[item.hinhThuc].label }}
          </span>
          <span class="ad-pill" :class="TRANG_THAI[item.trangThai].cls">{{ TRANG_THAI[item.trangThai].label }}</span>
        </div>
      </div>
    </div>

    <dl class="ad-dl">
      <div>
        <dt>Loại giảm</dt>
        <dd>{{ LOAI_GIAM[item.loaiGiam].label }}</dd>
      </div>
      <div>
        <dt>Giá trị giảm</dt>
        <dd>{{ formatGiaTri(item) }}</dd>
      </div>
      <div>
        <dt>Giảm tối đa</dt>
        <dd>{{ item.giamToiDa ? formatMoney(item.giamToiDa) : item.loaiGiam === 'PHAN_TRAM' ? 'Không giới hạn' : 'Không áp dụng' }}</dd>
      </div>
      <div>
        <dt>Đơn tối thiểu</dt>
        <dd>{{ item.donToiThieu ? formatMoney(item.donToiThieu) : 'Không yêu cầu' }}</dd>
      </div>
      <div>
        <dt>Số lượng phát hành</dt>
        <dd>{{ item.soLuong }}</dd>
      </div>
      <div>
        <dt>Đã sử dụng</dt>
        <dd>{{ item.soLuongDaDung ?? 0 }} / {{ item.soLuong }}</dd>
      </div>
      <div>
        <dt>Giới hạn mỗi khách</dt>
        <dd>{{ item.hinhThuc === 'CA_NHAN' ? '1 phiếu / khách' : item.gioiHanMoiKhach ? `${item.gioiHanMoiKhach} lần` : 'Không giới hạn' }}</dd>
      </div>
      <div>
        <dt>Thời gian áp dụng</dt>
        <dd>{{ formatDate(item.ngayBatDau) }} - {{ formatDate(item.ngayKetThuc) }}</dd>
      </div>
      <div v-if="item.hinhThuc === 'CA_NHAN'" class="ad-dl-full">
        <dt>Khách hàng được tặng ({{ item.khachHangs?.length ?? 0 }})</dt>
        <dd>
          <ul class="pgg-khach-list">
            <li v-for="k in item.khachHangs" :key="k.id">
              <span>{{ k.hoTen }}</span>
              <span class="ad-code">{{ k.ma }}</span>
              <span class="ad-pill" :class="k.daDung ? 'ad-pill-gray' : 'ad-pill-green'">{{ k.daDung ? 'Đã dùng' : 'Chưa dùng' }}</span>
            </li>
          </ul>
        </dd>
      </div>
    </dl>

    <template #footer>
      <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
      <button type="button" class="ad-btn ad-btn-primary" @click="emit('edit')">
        <i class="bi bi-pencil-square" aria-hidden="true"></i> Chỉnh sửa
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.pgg-khach-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 180px;
  overflow-y: auto;
}
.pgg-khach-list li {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.35rem 0;
  border-bottom: 1px solid #eef1f5;
}
.pgg-khach-list li > span:first-child {
  flex: 1;
  min-width: 0;
}
</style>
