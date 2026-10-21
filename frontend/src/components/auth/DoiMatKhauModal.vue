<script setup>
// Đổi mật khẩu của tài khoản đang đăng nhập. Backend cấp token mới (token cũ hết hiệu lực) nên lưu lại phiên ngay.
import { reactive, ref } from 'vue'
import api from '../../services/api'
import BaseModal from '../common/BaseModal.vue'
import { useAuth } from '../../composables/useAuth'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { loiMatKhau } from '../../utils/kiemTra'

const emit = defineEmits(['close'])
const auth = useAuth()
const toast = useToast()

const f = reactive({ matKhauCu: '', matKhauMoi: '', nhapLai: '' })
const hien = ref(false)
const dangLuu = ref(false)

const { loi, cham, sua, batDauGui, datLoiMayChu, focusLoiDau } = useLoiForm(() => {
  const e = {}
  if (!f.matKhauCu) e.matKhauCu = 'Vui lòng nhập mật khẩu hiện tại.'
  const moi = loiMatKhau(f.matKhauMoi, 'Vui lòng nhập mật khẩu mới.')
  if (moi) e.matKhauMoi = moi
  else if (f.matKhauMoi === f.matKhauCu) e.matKhauMoi = 'Mật khẩu mới phải khác mật khẩu hiện tại.'
  if (!f.nhapLai) e.nhapLai = 'Vui lòng nhập lại mật khẩu mới.'
  else if (f.nhapLai !== f.matKhauMoi) e.nhapLai = 'Mật khẩu nhập lại không khớp.'
  return e
})

async function luu() {
  if (dangLuu.value) return
  if (!batDauGui()) {
    focusLoiDau('#dmk-form')
    return
  }
  dangLuu.value = true
  try {
    const res = await api.put('/auth/doi-mat-khau', { ...f })
    auth.ghiPhien(res.data)
    toast.success('Đã đổi mật khẩu. Các phiên đăng nhập cũ trên máy khác đã hết hiệu lực.')
    emit('close')
  } catch (e) {
    if (datLoiMayChu(e)) focusLoiDau('#dmk-form')
    else toast.error(e.message || 'Không đổi được mật khẩu.')
  } finally {
    dangLuu.value = false
  }
}

const O = [
  { khoa: 'matKhauCu', nhan: 'Mật khẩu hiện tại', auto: 'current-password' },
  { khoa: 'matKhauMoi', nhan: 'Mật khẩu mới', auto: 'new-password', goiY: '8-50 ký tự, có cả chữ và số, không có khoảng trắng.' },
  { khoa: 'nhapLai', nhan: 'Nhập lại mật khẩu mới', auto: 'new-password' },
]
</script>

<template>
  <BaseModal title="Đổi mật khẩu" size="sm" static-backdrop @close="emit('close')">
    <form id="dmk-form" novalidate @submit.prevent="luu">
      <div v-for="o in O" :key="o.khoa" class="mb-3">
        <label class="ad-label" :for="`dmk-${o.khoa}`">{{ o.nhan }} <span class="ad-required">*</span></label>
        <input
          :id="`dmk-${o.khoa}`"
          v-model="f[o.khoa]"
          :type="hien ? 'text' : 'password'"
          class="form-control ad-control"
          :class="{ 'is-invalid': loi[o.khoa] }"
          :autocomplete="o.auto"
          maxlength="50"
          @input="sua(o.khoa)"
          @blur="cham(o.khoa)"
        />
        <p v-if="loi[o.khoa]" class="ad-error">{{ loi[o.khoa] }}</p>
        <p v-else-if="o.goiY" class="ad-hint">{{ o.goiY }}</p>
      </div>
      <label class="form-check">
        <input v-model="hien" type="checkbox" class="form-check-input" />
        <span class="form-check-label">Hiện mật khẩu</span>
      </label>
    </form>
    <template #footer>
      <button type="button" class="ad-btn" @click="emit('close')">Hủy</button>
      <button type="submit" form="dmk-form" class="ad-btn ad-btn-primary" :disabled="dangLuu">Đổi mật khẩu</button>
    </template>
  </BaseModal>
</template>
