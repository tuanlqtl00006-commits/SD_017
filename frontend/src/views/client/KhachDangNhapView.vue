<script setup>
// Trang đăng nhập RIÊNG của khách hàng trên web bán hàng (cô: trang đăng nhập khách hàng là một trang khác với trang quản trị).
import { nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth'
import { useLoiForm } from '../../composables/useLoiForm'
import { duongDanQuayLaiAnToan } from '../../services/phien'

const router = useRouter()
const route = useRoute()
const auth = useAuth()

const f = reactive({ taiKhoan: '', matKhau: '', nho: false })
const hienMatKhau = ref(false)
const dangGui = ref(false)
const loiChung = ref('')
const oMatKhau = ref(null)

const { loi, cham, sua, batDauGui, datLoiMayChu, focusLoiDau, datLai } = useLoiForm(() => {
  const e = {}
  if (!f.taiKhoan.trim()) e.taiKhoan = 'Vui lòng nhập email hoặc số điện thoại.'
  if (!f.matKhau) e.matKhau = 'Vui lòng nhập mật khẩu.'
  return e
})

async function dangNhap() {
  loiChung.value = ''
  if (dangGui.value) return
  if (!batDauGui()) {
    focusLoiDau('#kh-form')
    return
  }
  dangGui.value = true
  try {
    await auth.dangNhapKhachHang({ taiKhoan: f.taiKhoan.trim(), matKhau: f.matKhau, nho: f.nho })
    const quayLai = duongDanQuayLaiAnToan(route.query.quayLai)
    await router.replace(quayLai && quayLai.startsWith('/shop') ? quayLai : '/shop')
  } catch (e) {
    f.matKhau = ''
    if (!datLoiMayChu(e)) {
      datLai()
      loiChung.value = e.message || 'Đăng nhập không thành công.'
    }
    nextTick(() => oMatKhau.value?.focus())
  } finally {
    dangGui.value = false
  }
}
</script>

<template>
  <div class="container py-5 kh-dn">
    <div class="kh-the">
      <h1 class="kh-tieu-de">Đăng nhập tài khoản</h1>
      <p class="kh-phu">Đăng nhập để theo dõi đơn hàng và nhận ưu đãi dành riêng cho bạn.</p>

      <div v-if="loiChung" class="kh-loi-chung" role="alert"><i class="bi bi-x-circle-fill" aria-hidden="true"></i> {{ loiChung }}</div>

      <form id="kh-form" novalidate @submit.prevent="dangNhap">
        <div class="mb-3">
          <label class="form-label fw-semibold" for="kh-tai-khoan">Email hoặc số điện thoại <span class="text-danger">*</span></label>
          <input
            id="kh-tai-khoan"
            v-model="f.taiKhoan"
            type="text"
            name="username"
            class="form-control kh-o"
            :class="{ 'is-invalid': loi.taiKhoan }"
            autocomplete="username"
            autocapitalize="none"
            spellcheck="false"
            required
            maxlength="255"
            :aria-invalid="!!loi.taiKhoan"
            :aria-describedby="loi.taiKhoan ? 'kh-tai-khoan-loi' : undefined"
            placeholder="vd: ten@gmail.com hoặc 0901234567"
            @input="sua('taiKhoan')"
            @blur="cham('taiKhoan')"
          />
          <p v-if="loi.taiKhoan" id="kh-tai-khoan-loi" class="kh-loi">{{ loi.taiKhoan }}</p>
        </div>
        <div class="mb-3">
          <label class="form-label fw-semibold" for="kh-mat-khau">Mật khẩu <span class="text-danger">*</span></label>
          <div class="position-relative">
            <input
              id="kh-mat-khau"
              ref="oMatKhau"
              v-model="f.matKhau"
              :type="hienMatKhau ? 'text' : 'password'"
              name="password"
              class="form-control kh-o pe-5"
              :class="{ 'is-invalid': loi.matKhau }"
              autocomplete="current-password"
              required
              maxlength="100"
              :aria-invalid="!!loi.matKhau"
              :aria-describedby="loi.matKhau ? 'kh-mat-khau-loi' : undefined"
              placeholder="Nhập mật khẩu"
              @input="sua('matKhau')"
              @blur="cham('matKhau')"
            />
            <button type="button" class="kh-mat" :aria-label="hienMatKhau ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'" @click="hienMatKhau = !hienMatKhau">
              <i class="bi" :class="hienMatKhau ? 'bi-eye-slash' : 'bi-eye'" aria-hidden="true"></i>
            </button>
          </div>
          <p v-if="loi.matKhau" id="kh-mat-khau-loi" class="kh-loi">{{ loi.matKhau }}</p>
        </div>
        <label class="d-inline-flex align-items-center gap-2 mb-3 kh-nho">
          <input v-model="f.nho" type="checkbox" name="remember" class="form-check-input m-0" /> Ghi nhớ đăng nhập
        </label>
        <button type="submit" class="btn kh-nut w-100" :disabled="dangGui">
          <span v-if="dangGui" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          {{ dangGui ? 'Đang đăng nhập…' : 'Đăng nhập' }}
        </button>
      </form>

      <p class="kh-chan">
        Bạn là nhân viên cửa hàng?
        <button type="button" class="kh-lien-ket" @click="router.push('/dang-nhap')">Đăng nhập trang quản trị</button>
      </p>
    </div>
  </div>
</template>

<style scoped>
.kh-dn {
  display: flex;
  justify-content: center;
}
.kh-the {
  width: 100%;
  max-width: 440px;
  padding: 2rem;
  border: 1px solid #e5e9f0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 27, 45, 0.06);
}
.kh-tieu-de {
  margin: 0 0 0.35rem;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: normal;
}
.kh-phu {
  margin: 0 0 1.25rem;
  color: #5b6b80;
}
.kh-o {
  height: 48px;
  font-size: 1rem;
  border-radius: 10px;
}
.kh-mat {
  position: absolute;
  top: 50%;
  right: 0.5rem;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  color: #64748b;
  padding: 0.35rem;
}
.kh-loi {
  margin: 0.3rem 0 0;
  font-size: 0.86rem;
  color: #dc2626;
}
.kh-loi-chung {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding: 0.7rem 0.9rem;
  border-left: 4px solid #dc2626;
  border-radius: 10px;
  background: #fef2f2;
  color: #991b1b;
  font-size: 0.9rem;
}
.kh-nho {
  font-size: 0.92rem;
  color: #475569;
  cursor: pointer;
}
.kh-nut {
  height: 50px;
  border-radius: 10px;
  background: var(--vnb-blue, #0977ec);
  color: #fff;
  font-weight: 700;
}
.kh-nut:hover:not(:disabled) {
  filter: brightness(1.08);
  color: #fff;
}
.kh-chan {
  margin: 1.25rem 0 0;
  text-align: center;
  font-size: 0.9rem;
  color: #5b6b80;
}
.kh-lien-ket {
  border: 0;
  background: none;
  padding: 0;
  color: #0977ec;
  font-weight: 600;
}
.kh-lien-ket:hover {
  text-decoration: underline;
}
</style>
