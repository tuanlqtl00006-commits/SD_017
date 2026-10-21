<script setup>
// Chọn khách cho hóa đơn: khách lẻ (mặc định) hoặc tìm theo SĐT / tên / email; thêm nhanh khách mới (dùng lại POST /api/khach-hang,
// cùng luật kiểm tra với trang Khách hàng: họ tên, SĐT 10 số đúng đầu số, email không trùng).
import { reactive, ref, watch } from 'vue'
import api from '../../services/api'
import BaseModal from '../common/BaseModal.vue'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { EMAIL, cat, chuanHoaHoTen, loiHoTen, loiSdt } from '../../utils/kiemTra'

const props = defineProps({ khach: { type: Object, default: null } })
const emit = defineEmits(['chon'])
const toast = useToast()

const tuKhoa = ref('')
const ketQua = ref([])
const dangTim = ref(false)
const moDs = ref(false)
let hen = null

watch(tuKhoa, (v) => {
  clearTimeout(hen)
  if (!v.trim()) {
    ketQua.value = []
    return
  }
  hen = setTimeout(() => tim(v), 300)
})

async function tim(v) {
  dangTim.value = true
  try {
    ketQua.value = (await api.get('/ban-hang/khach-hang', { params: { q: v.trim() } })).data
    moDs.value = true
  } catch (e) {
    toast.error(e.message || 'Không tìm được khách hàng.')
  } finally {
    dangTim.value = false
  }
}

function chon(k) {
  emit('chon', k)
  tuKhoa.value = ''
  ketQua.value = []
  moDs.value = false
}

/* ----- Thêm nhanh ----- */
const moThem = ref(false)
const dangLuu = ref(false)
const f = reactive({ hoTen: '', sdt: '', email: '', gioiTinh: 1 })
const { loi, sua, cham, batDauGui, datLoiMayChu, datLai, focusLoiDau } = useLoiForm(() => {
  if (!moThem.value) return {}
  const e = {}
  const ten = loiHoTen(f.hoTen, { thieu: 'Vui lòng nhập họ tên khách hàng.' })
  if (ten) e.hoTen = ten
  const sdt = loiSdt(f.sdt)
  if (sdt) e.sdt = sdt
  const email = cat(f.email)
  if (!email) e.email = 'Vui lòng nhập email.'
  else if (!EMAIL.test(email)) e.email = 'Email chưa đúng định dạng, ví dụ: ten@gmail.com.'
  return e
})
function moThemNhanh() {
  const v = tuKhoa.value.trim()
  Object.assign(f, { hoTen: '', sdt: /^\d+$/.test(v) ? v : '', email: v.includes('@') ? v : '', gioiTinh: 1 })
  if (!/^\d+$/.test(v) && !v.includes('@')) f.hoTen = v
  datLai()
  moThem.value = true
}
async function luuKhach() {
  if (dangLuu.value) return
  if (!batDauGui()) {
    focusLoiDau('#kh-nhanh-form')
    return
  }
  dangLuu.value = true
  try {
    const res = await api.post('/khach-hang', { hoTen: chuanHoaHoTen(f.hoTen), sdt: cat(f.sdt), email: cat(f.email), gioiTinh: Number(f.gioiTinh) })
    const k = res.data
    toast.success(`Đã thêm khách hàng ${k.hoTen}.`)
    moThem.value = false
    chon({ id: k.id, ma: k.maKhachHang, hoTen: k.hoTen, sdt: k.sdt, email: k.email })
  } catch (e) {
    if (datLoiMayChu(e)) focusLoiDau('#kh-nhanh-form')
    else toast.error(e.message || 'Không thêm được khách hàng.')
  } finally {
    dangLuu.value = false
  }
}
</script>

<template>
  <div class="ck">
    <div v-if="props.khach" class="ck-da-chon">
      <span class="ck-avatar"><i class="bi bi-person" aria-hidden="true"></i></span>
      <div class="flex-grow-1 min-w-0">
        <div class="fw-semibold text-truncate">{{ props.khach.hoTen }}</div>
        <small class="text-muted">{{ props.khach.sdt }} · {{ props.khach.ma }}</small>
      </div>
      <button type="button" class="ad-btn ad-btn-sm" @click="emit('chon', null)">Khách lẻ</button>
    </div>
    <div v-else class="ck-tim">
      <div class="ad-affix">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input
          v-model="tuKhoa"
          type="search"
          class="form-control ad-control"
          placeholder="Khách lẻ - tìm khách theo SĐT, tên, email…"
          aria-label="Tìm khách hàng"
          autocomplete="off"
          @focus="moDs = ketQua.length > 0"
          @keydown.esc="moDs = false"
        />
      </div>
      <button type="button" class="ad-icon-btn" title="Thêm khách mới" aria-label="Thêm khách mới" @click="moThemNhanh">
        <i class="bi bi-person-plus" aria-hidden="true"></i>
      </button>
      <ul v-if="moDs && tuKhoa.trim()" class="ck-ds" role="listbox">
        <li v-if="dangTim" class="ck-trong">Đang tìm…</li>
        <li v-else-if="!ketQua.length" class="ck-trong">
          Không có khách phù hợp. <button type="button" class="ck-lk" @click="moThemNhanh">Thêm khách mới</button>
        </li>
        <li v-for="k in ketQua" :key="k.id">
          <button type="button" class="ck-muc" role="option" @click="chon(k)">
            <b>{{ k.hoTen }}</b> <span class="text-muted">{{ k.sdt }} · {{ k.ma }}</span>
          </button>
        </li>
      </ul>
    </div>
  </div>

  <BaseModal v-if="moThem" title="Thêm nhanh khách hàng" size="sm" static-backdrop @close="moThem = false">
    <form id="kh-nhanh-form" novalidate @submit.prevent="luuKhach">
      <label class="ad-label" for="khn-ten">Họ tên <span class="ad-required">*</span></label>
      <input id="khn-ten" v-model="f.hoTen" class="form-control ad-control" :class="{ 'is-invalid': loi.hoTen }" maxlength="100" @input="sua('hoTen')" @blur="cham('hoTen')" />
      <p v-if="loi.hoTen" class="ad-error">{{ loi.hoTen }}</p>
      <label class="ad-label mt-2" for="khn-sdt">Số điện thoại <span class="ad-required">*</span></label>
      <input id="khn-sdt" v-model="f.sdt" class="form-control ad-control" :class="{ 'is-invalid': loi.sdt }" maxlength="10" inputmode="numeric" @input="sua('sdt')" @blur="cham('sdt')" />
      <p v-if="loi.sdt" class="ad-error">{{ loi.sdt }}</p>
      <label class="ad-label mt-2" for="khn-email">Email <span class="ad-required">*</span></label>
      <input id="khn-email" v-model="f.email" type="email" class="form-control ad-control" :class="{ 'is-invalid': loi.email }" maxlength="255" @input="sua('email')" @blur="cham('email')" />
      <p v-if="loi.email" class="ad-error">{{ loi.email }}</p>
      <div class="d-flex gap-3 mt-2" role="radiogroup" aria-label="Giới tính">
        <label class="form-check"><input v-model="f.gioiTinh" class="form-check-input" type="radio" :value="1" /> Nam</label>
        <label class="form-check"><input v-model="f.gioiTinh" class="form-check-input" type="radio" :value="0" /> Nữ</label>
      </div>
    </form>
    <template #footer>
      <button type="button" class="ad-btn" @click="moThem = false">Hủy</button>
      <button type="submit" form="kh-nhanh-form" class="ad-btn ad-btn-primary" :disabled="dangLuu">Thêm và chọn</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.ck {
  position: relative;
}
.ck-tim {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}
.ck-tim .ad-affix {
  flex: 1;
}
.ck-da-chon {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  border: 1px solid var(--fs-primary-line, #c9def9);
  background: var(--fs-primary-soft, #e7f1fd);
  border-radius: 12px;
  padding: 0.5rem 0.65rem;
}
.ck-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #fff;
  color: var(--fs-primary, #0977ec);
  display: flex;
  align-items: center;
  justify-content: center;
}
.ck-ds {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 30;
  background: #fff;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(24, 36, 47, 0.12);
  list-style: none;
  margin: 0;
  padding: 0.3rem;
  max-height: 280px;
  overflow: auto;
}
.ck-muc {
  width: 100%;
  text-align: left;
  border: 0;
  background: transparent;
  border-radius: 8px;
  padding: 0.5rem 0.6rem;
}
.ck-muc:hover {
  background: var(--fs-hover-row, #f1f7fe);
}
.ck-trong {
  padding: 0.6rem;
  color: var(--fs-muted, #7b8794);
}
.ck-lk {
  border: 0;
  background: none;
  color: var(--fs-primary, #0977ec);
  padding: 0;
  font-weight: 600;
}
.min-w-0 {
  min-width: 0;
}
</style>
