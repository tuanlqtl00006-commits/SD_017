<script setup>
import BaseModal from '../common/BaseModal.vue'
import { VAI_TRO, TRANG_THAI } from '../../constants/nhanVien'
import { avatarColors, formatDate, getInitials } from '../../utils/format'

defineProps({
  item: { type: Object, required: true },
})
const emit = defineEmits(['close', 'edit'])
</script>

<template>
  <BaseModal title="Chi tiết nhân viên" size="lg" @close="emit('close')">
    <div class="ad-detail-head">
      <span class="ad-avatar ad-avatar-lg" :style="item.anh ? undefined : avatarColors(item.hoTen)">
        <img v-if="item.anh" :src="item.anh" :alt="`Ảnh của ${item.hoTen}`" />
        <template v-else>{{ getInitials(item.hoTen) }}</template>
      </span>
      <div>
        <h3 class="ad-detail-name">{{ item.hoTen }}</h3>
        <div class="ad-detail-sub">
          <span class="ad-code">{{ item.ma }}</span>
          <span class="ad-pill ad-pill-blue">{{ VAI_TRO[item.vaiTro].label }}</span>
          <span class="ad-pill" :class="TRANG_THAI[item.hoatDong].cls">{{ TRANG_THAI[item.hoatDong].label }}</span>
        </div>
      </div>
    </div>

    <dl class="ad-dl">
      <div>
        <dt>Tài khoản</dt>
        <dd>{{ item.taiKhoan }}</dd>
      </div>
      <div>
        <dt>Email</dt>
        <dd>{{ item.email }}</dd>
      </div>
      <div>
        <dt>Số điện thoại</dt>
        <dd>{{ item.soDienThoai }}</dd>
      </div>
      <div>
        <dt>Giới tính</dt>
        <dd>{{ item.gioiTinh }}</dd>
      </div>
      <div>
        <dt>Ngày sinh</dt>
        <dd>{{ item.ngaySinh ? formatDate(item.ngaySinh) : 'Chưa cập nhật' }}</dd>
      </div>
      <div>
        <dt>Vai trò</dt>
        <dd>{{ VAI_TRO[item.vaiTro].label }}</dd>
      </div>
      <div class="ad-dl-full">
        <dt>Địa chỉ</dt>
        <dd>{{ item.diaChi }}</dd>
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
