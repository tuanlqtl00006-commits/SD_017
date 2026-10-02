<script setup>
import BaseModal from '../common/BaseModal.vue'
import { HINH_THUC, LOAI_GIAM, TRANG_THAI, formatGiaTri } from '../../constants/phieuGiamGia'
import { formatDate, formatMoney } from '../../utils/format'

defineProps({
  // Phiếu kèm trường trangThai đã được tính sẵn ở trang danh sách
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
        <dt>Thời gian áp dụng</dt>
        <dd>{{ formatDate(item.ngayBatDau) }} - {{ formatDate(item.ngayKetThuc) }}</dd>
      </div>
      <div class="ad-dl-full">
        <dt>Mô tả</dt>
        <dd>{{ item.moTa || 'Chưa có mô tả.' }}</dd>
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
