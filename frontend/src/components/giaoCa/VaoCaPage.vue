<script setup>
// Trang VÀO CA (bắt buộc với nhân viên): đăng nhập xong mà chưa vào ca thì chỉ ở trang này, không mở được chức năng nào khác
// (backend cũng chặn API, mã CHUA_NHAN_CA). Cô: "nhân viên không bấm vào ca thì sao? vẫn làm được à?".
//  - Đối soát ca trước: tiền mặt ca trước để lại trong két + tiền chuyển khoản ca trước thu được (theo hệ thống).
//  - Người vào ca đếm két / kiểm tra sao kê rồi nhập lại 2 số này; khác hệ thống thì bắt buộc ghi lý do.
//  - "Bắt đầu ca làm việc" xong thì vào trang đang định mở (hoặc Bán hàng tại quầy).
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MoneyInput from '../common/MoneyInput.vue'
import { useAuth } from '../../composables/useAuth'
import { useGiaoCa } from '../../composables/useGiaoCa'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { duongDanQuayLaiAnToan } from '../../services/phien'
import { formatMoney, formatNgayGio } from '../../utils/format'

const TIEN_TOI_DA = 1_000_000_000
const route = useRoute()
const router = useRouter()
const toast = useToast()
const auth = useAuth()
const { trangThai, dangTrongCa, taiLai, nhanCa } = useGiaoCa()

const f = reactive({ tienDauCa: '', chuyenKhoanCaTruoc: '', ghiChu: '', idLichLamViec: null })
const dangGui = ref(false)

const coCaTruoc = computed(() => trangThai.goiYTienDauCa !== null)
const lechTienMat = computed(() => coCaTruoc.value && f.tienDauCa !== '' && Number(f.tienDauCa) !== Number(trangThai.goiYTienDauCa))
const lechChuyenKhoan = computed(
  () => coCaTruoc.value && f.chuyenKhoanCaTruoc !== '' && Number(f.chuyenKhoanCaTruoc) !== Number(trangThai.chuyenKhoanCaTruoc ?? 0),
)
const canGhiChu = computed(() => lechTienMat.value || lechChuyenKhoan.value)

function kiemTien(v, ten) {
  if (v === '' || v === null) return `Vui lòng nhập ${ten}.`
  if (Number(v) < 0) return 'Số tiền không được âm.'
  if (Number(v) > TIEN_TOI_DA) return 'Số tiền tối đa 1.000.000.000 ₫.'
  return ''
}

const { loi, sua, cham, batDauGui, datLoiMayChu, focusLoiDau } = useLoiForm(() => {
  const e = {}
  const a = kiemTien(f.tienDauCa, 'tiền mặt đầu ca')
  if (a) e.tienDauCa = a
  if (coCaTruoc.value) {
    const b = kiemTien(f.chuyenKhoanCaTruoc, 'số tiền chuyển khoản của ca trước')
    if (b) e.chuyenKhoanCaTruoc = b
  }
  const gc = f.ghiChu.trim()
  if (gc.length > 500) e.ghiChu = 'Ghi chú tối đa 500 ký tự.'
  else if (!gc && lechTienMat.value) e.ghiChu = `Tiền mặt đầu ca khác tiền ca trước bàn giao (${formatMoney(trangThai.goiYTienDauCa)}). Vui lòng ghi lý do.`
  else if (!gc && lechChuyenKhoan.value) {
    e.ghiChu = `Chuyển khoản ca trước khác số hệ thống ghi nhận (${formatMoney(trangThai.chuyenKhoanCaTruoc ?? 0)}). Vui lòng ghi lý do.`
  }
  return e
})

function trangTiepTheo() {
  const q = duongDanQuayLaiAnToan(route.query.quayLai)
  return q && !q.startsWith('/vao-ca') ? q : auth.trangChu()
}

onMounted(async () => {
  await taiLai()
  if (dangTrongCa.value) {
    router.replace(trangTiepTheo())
    return
  }
})

function dungSoCaTruoc() {
  f.tienDauCa = Number(trangThai.goiYTienDauCa ?? 0)
  f.chuyenKhoanCaTruoc = Number(trangThai.chuyenKhoanCaTruoc ?? 0)
  sua('tienDauCa')
  sua('chuyenKhoanCaTruoc')
  sua('ghiChu')
}

async function batDauCa() {
  if (dangGui.value) return
  if (!batDauGui()) {
    focusLoiDau('#vc-form')
    return
  }
  dangGui.value = true
  try {
    const gc = await nhanCa({
      tienDauCa: Number(f.tienDauCa),
      chuyenKhoanCaTruoc: coCaTruoc.value ? Number(f.chuyenKhoanCaTruoc) : null,
      ghiChu: f.ghiChu.trim(),
      idLichLamViec: f.idLichLamViec,
    })
    toast.success(`Đã vào ca ${gc.maGiaoCa}${gc.ngoaiLich ? ' (ngoài lịch)' : ` - ${gc.tenCa}`}. Chúc bạn một ca làm việc thuận lợi!`)
    await router.replace(trangTiepTheo())
  } catch (e) {
    if (e.code === 'DANG_TRONG_CA') {
      await taiLai()
      router.replace(trangTiepTheo())
      return
    }
    if (datLoiMayChu(e)) focusLoiDau('#vc-form')
    else toast.error(e.message || 'Không vào ca được.')
  } finally {
    dangGui.value = false
  }
}

function dangXuat() {
  auth.dangXuat()
  router.replace('/dang-nhap')
}
</script>

<template>
  <div class="vc-trang">
    <header class="vc-dau">
      <img src="/logo.png" alt="FootStyle" class="vc-logo" />
      <div class="vc-nguoi">
        <span class="vc-avatar" aria-hidden="true">{{ auth.vietTat.value }}</span>
        <span class="d-none d-sm-inline">{{ auth.trangThai.nguoiDung?.hoTen }}</span>
        <button type="button" class="ad-btn ad-btn-sm" data-testid="vc-dang-xuat" @click="dangXuat">
          <i class="bi bi-box-arrow-right" aria-hidden="true"></i> Đăng xuất
        </button>
      </div>
    </header>

    <main class="vc-giua">
      <section class="vc-the" aria-labelledby="vc-tieu-de">
        <div class="vc-bieu-tuong" aria-hidden="true"><i class="bi bi-door-open"></i></div>
        <h1 id="vc-tieu-de" class="vc-tieu-de">Vào ca làm việc</h1>
        <p class="vc-phu">
          Xin chào <b>{{ auth.trangThai.nguoiDung?.hoTen }}</b>. Bạn cần vào ca trước khi bắt đầu bán hàng và dùng các chức năng khác.
        </p>

        <div v-if="!trangThai.daTai && trangThai.loi" class="vc-loi-tai" role="alert">
          {{ trangThai.loi }} <button type="button" class="ad-btn ad-btn-sm ms-2" @click="taiLai">Thử lại</button>
        </div>
        <div v-else-if="!trangThai.daTai" class="ad-empty">Đang tải thông tin ca…</div>

        <form v-else id="vc-form" novalidate @submit.prevent="batDauCa">
          <!-- Bàn giao từ ca trước -->
          <div class="vc-khoi">
            <div class="vc-khoi-dau">
              <h2 class="vc-khoi-ten"><i class="bi bi-arrow-left-right" aria-hidden="true"></i> Bàn giao từ ca trước</h2>
              <button v-if="coCaTruoc" type="button" class="ad-btn ad-btn-sm" data-testid="vc-dung-so" @click="dungSoCaTruoc">
                <i class="bi bi-check2-all" aria-hidden="true"></i> Dùng số tiền này
              </button>
            </div>
            <template v-if="coCaTruoc">
              <p class="vc-ghi-nho">
                Ca <b>{{ trangThai.maCaTruoc }}</b> do <b>{{ trangThai.nguoiBanGiao }}</b> kết ca lúc {{ formatNgayGio(trangThai.thoiGianBanGiao) }}.
              </p>
              <dl class="vc-luoi">
                <div><dt>Tiền mặt để lại trong két</dt><dd data-testid="vc-tien-mat-truoc">{{ formatMoney(trangThai.goiYTienDauCa) }}</dd></div>
                <div><dt>Chuyển khoản ca trước thu</dt><dd data-testid="vc-ck-truoc">{{ formatMoney(trangThai.chuyenKhoanCaTruoc ?? 0) }}</dd></div>
              </dl>
              <p class="ad-hint mb-0">Hãy đếm lại tiền trong két và kiểm tra sao kê tài khoản trước khi xác nhận.</p>
            </template>
            <p v-else class="vc-ghi-nho mb-0">Chưa có ca nào bàn giao trước đó. Nhập số tiền mặt đang có trong két.</p>
          </div>

          <!-- Ca trong lịch hôm nay -->
          <fieldset class="vc-khoi">
            <legend class="vc-khoi-ten"><i class="bi bi-calendar-week" aria-hidden="true"></i> Ca làm việc</legend>
            <p v-if="!trangThai.lichHomNay.length" class="ad-hint mb-0">Hôm nay bạn không có lịch làm. Ca sẽ được ghi là <b>ngoài lịch</b>.</p>
            <div v-else class="vc-ca">
              <label class="vc-ca-muc">
                <input v-model="f.idLichLamViec" type="radio" :value="null" class="form-check-input" />
                <span>Tự chọn theo giờ hiện tại</span>
              </label>
              <label v-for="l in trangThai.lichHomNay" :key="l.id" class="vc-ca-muc">
                <input v-model="f.idLichLamViec" type="radio" :value="l.id" class="form-check-input" />
                <span><b>{{ l.tenCa }}</b> {{ l.gioBatDau }} - {{ l.gioKetThuc }}</span>
              </label>
            </div>
          </fieldset>

          <div class="row g-3">
            <div class="col-sm-6">
              <label class="ad-label" for="vc-tien-mat">Tiền mặt đầu ca <span class="ad-required">*</span></label>
              <MoneyInput
                id="vc-tien-mat"
                v-model="f.tienDauCa"
                aria-label="Tiền mặt đầu ca"
                placeholder="Tiền mặt đếm được trong két"
                :invalid="!!loi.tienDauCa"
                @update:model-value="sua('tienDauCa'); sua('ghiChu')"
              />
              <p v-if="loi.tienDauCa" class="ad-error">{{ loi.tienDauCa }}</p>
              <p v-else-if="lechTienMat" class="vc-lech">Khác số ca trước bàn giao</p>
            </div>
            <div v-if="coCaTruoc" class="col-sm-6">
              <label class="ad-label" for="vc-chuyen-khoan">Chuyển khoản ca trước <span class="ad-required">*</span></label>
              <MoneyInput
                id="vc-chuyen-khoan"
                v-model="f.chuyenKhoanCaTruoc"
                aria-label="Chuyển khoản ca trước"
                placeholder="Theo sao kê ngân hàng"
                :invalid="!!loi.chuyenKhoanCaTruoc"
                @update:model-value="sua('chuyenKhoanCaTruoc'); sua('ghiChu')"
              />
              <p v-if="loi.chuyenKhoanCaTruoc" class="ad-error">{{ loi.chuyenKhoanCaTruoc }}</p>
              <p v-else-if="lechChuyenKhoan" class="vc-lech">Khác số hệ thống ghi nhận</p>
            </div>
            <div class="col-12">
              <label class="ad-label" for="vc-ghi-chu">Ghi chú <span v-if="canGhiChu" class="ad-required">*</span></label>
              <textarea
                id="vc-ghi-chu"
                v-model="f.ghiChu"
                class="form-control ad-control"
                :class="{ 'is-invalid': loi.ghiChu }"
                rows="2"
                maxlength="500"
                placeholder="Bắt buộc khi số tiền khác với ca trước bàn giao"
                @input="sua('ghiChu')"
                @blur="cham('ghiChu')"
              ></textarea>
              <p v-if="loi.ghiChu" class="ad-error">{{ loi.ghiChu }}</p>
            </div>
          </div>

          <button type="submit" class="ad-btn ad-btn-primary vc-nut" :disabled="dangGui" data-testid="vc-bat-dau">
            <span v-if="dangGui" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
            <i v-else class="bi bi-play-circle" aria-hidden="true"></i>
            {{ dangGui ? 'Đang vào ca…' : 'Bắt đầu ca làm việc' }}
          </button>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.vc-trang {
  min-height: 100vh;
  background: #f3f6fb;
  display: flex;
  flex-direction: column;
}
.vc-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  background: #fff;
  border-bottom: 1px solid #e5e9f0;
}
.vc-logo {
  height: 40px;
  object-fit: contain;
}
.vc-nguoi {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-weight: 600;
  color: #1e293b;
}
.vc-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-grid;
  place-items: center;
  background: #e0edff;
  color: #0b5ed7;
  font-size: 0.8rem;
  font-weight: 700;
}
.vc-giua {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 2rem 1rem 3rem;
}
.vc-the {
  width: 100%;
  max-width: 680px;
  background: #fff;
  border: 1px solid #e5e9f0;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 12px 32px rgba(15, 27, 45, 0.06);
}
.vc-bieu-tuong {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: #e0edff;
  color: #0b5ed7;
  font-size: 1.5rem;
  margin-bottom: 0.75rem;
}
.vc-tieu-de {
  font-size: 1.5rem;
  font-weight: 800;
  margin: 0 0 0.35rem;
}
.vc-phu {
  color: #5b6b80;
  margin: 0 0 1.5rem;
}
.vc-khoi {
  border: 1px solid #e5e9f0;
  border-radius: 12px;
  padding: 1rem 1.1rem;
  margin: 0 0 1.25rem;
}
.vc-khoi-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}
.vc-khoi-ten {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
  float: none;
  width: auto;
  padding: 0;
}
.vc-khoi-dau .vc-khoi-ten {
  margin: 0;
}
.vc-ghi-nho {
  color: #475569;
  font-size: 0.92rem;
  margin: 0 0 0.75rem;
}
.vc-luoi {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0 0 0.6rem;
}
.vc-luoi > div {
  background: #f8fafc;
  border-radius: 10px;
  padding: 0.65rem 0.8rem;
}
.vc-luoi dt {
  font-size: 0.82rem;
  font-weight: 500;
  color: #64748b;
}
.vc-luoi dd {
  margin: 0.15rem 0 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
}
.vc-ca {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.vc-ca-muc {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  cursor: pointer;
}
.vc-ca-muc:has(input:checked) {
  border-color: #0b5ed7;
  background: #f0f6ff;
}
.vc-ca-muc input {
  margin: 0;
}
.vc-lech {
  margin: 0.3rem 0 0;
  font-size: 0.85rem;
  color: #b45309;
}
.vc-loi-tai {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  background: #fef2f2;
  color: #991b1b;
}
.vc-nut {
  width: 100%;
  height: 48px;
  margin-top: 1.5rem;
  justify-content: center;
  font-size: 1rem;
}
@media (max-width: 575.98px) {
  .vc-the {
    padding: 1.25rem;
  }
  .vc-luoi {
    grid-template-columns: 1fr;
  }
  .vc-dau {
    padding: 0.6rem 1rem;
  }
}
</style>
