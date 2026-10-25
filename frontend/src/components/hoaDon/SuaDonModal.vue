<script setup>
// Sửa đơn online (case khó cô gửi): khách không tự sửa; nhân viên gọi xác nhận rồi sửa giúp MỘT lần khi đơn còn "Chờ xác nhận".
//  - Sửa người nhận / địa chỉ; thêm / đổi số lượng / bỏ sản phẩm.
//  - Thêm lại cùng sản phẩm mà giá đã đổi: backend tách thành dòng mới (dòng cũ giữ giá lúc khách đặt).
//  - Backend tính lại phiếu giảm giá + phí giao hàng; khách đã chuyển khoản trước thì ghi HOÀN PHÍ / PHỤ PHÍ.
import { computed, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import BaseModal from '../common/BaseModal.vue'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { formatMoney } from '../../utils/format'
import { phiGiaoHang } from '../../utils/banHang'

const props = defineProps({ data: { type: Object, required: true } })
const emit = defineEmits(['close', 'saved'])
const toast = useToast()

const giao = props.data.giaoHang || {}
const f = reactive({
  hoTenNguoiNhan: giao.hoTenNguoiNhan || '',
  sdtNguoiNhan: giao.sdtNguoiNhan || '',
  diaChiGiaoHang: giao.diaChi || '',
  lyDo: '',
})
// Dòng đang có (giữ giá lúc đặt) + dòng thêm mới (giá hiện tại)
const dong = ref(
  (props.data.sanPham || []).map((x) => ({
    idChiTiet: x.id, idBienThe: x.idBienThe ?? x.idSanPhamChiTiet ?? null, ten: x.tenSanPham, phanLoai: x.phanLoai || x.maSpct || '',
    donGia: Number(x.donGia) || 0, soLuong: Number(x.soLuong) || 1, moi: false,
  })),
)

/* ----- Thêm sản phẩm ----- */
const tuKhoa = ref('')
const ketQua = ref([])
let hen = null
watch(tuKhoa, (q) => {
  clearTimeout(hen)
  if (!q.trim()) {
    ketQua.value = []
    return
  }
  hen = setTimeout(async () => {
    try {
      ketQua.value = (await api.get('/ban-hang/san-pham', { params: { q: q.trim(), gioiHan: 8 } })).data || []
    } catch {
      ketQua.value = []
    }
  }, 300)
})
function them(sp) {
  if (sp.tonKho <= 0) {
    toast.error(`${sp.tenSanPham} đã hết hàng.`)
    return
  }
  const co = dong.value.find((d) => d.moi && d.idBienThe === sp.id)
  if (co) co.soLuong = Math.min(co.soLuong + 1, sp.tonKho)
  else dong.value.push({ idChiTiet: null, idBienThe: sp.id, ten: sp.tenSanPham, phanLoai: sp.phanLoai || sp.ma, donGia: Number(sp.donGia) || 0, soLuong: 1, tonKho: sp.tonKho, moi: true })
  tuKhoa.value = ''
  ketQua.value = []
}
function bo(d) {
  dong.value = dong.value.filter((x) => x !== d)
}

/* ----- Tạm tính (backend tính lại chính xác) ----- */
const tienHang = computed(() => dong.value.reduce((s, d) => s + d.donGia * (Number(d.soLuong) || 0), 0))
const phiShip = computed(() => phiGiaoHang(tienHang.value))
const cu = computed(() => Number(props.data.hoaDon?.thanhTien) || 0)
const daTra = computed(() => Number(props.data.daThanhToan) || 0)

const { loi, sua, cham, batDauGui, datLoiMayChu, focusLoiDau } = useLoiForm(() => {
  const e = {}
  if (!f.hoTenNguoiNhan.trim()) e.hoTenNguoiNhan = 'Vui lòng nhập tên người nhận.'
  if (!/^0[35789]\d{8}$/.test(f.sdtNguoiNhan.replace(/[\s.-]/g, ''))) e.sdtNguoiNhan = 'Số điện thoại gồm 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.'
  if (f.diaChiGiaoHang.trim().length < 10) e.diaChiGiaoHang = 'Địa chỉ quá ngắn, hãy ghi rõ số nhà, đường, phường/xã, tỉnh/thành.'
  if (!f.lyDo.trim()) e.lyDo = 'Vui lòng ghi lý do chỉnh sửa (vd: khách gọi đổi địa chỉ).'
  if (!dong.value.length) e.items = 'Đơn phải còn ít nhất 1 sản phẩm. Muốn bỏ hết thì hủy đơn.'
  else if (dong.value.some((d) => !(Number(d.soLuong) >= 1))) e.items = 'Số lượng mỗi sản phẩm tối thiểu là 1.'
  return e
})

const dangGui = ref(false)
async function luu() {
  if (dangGui.value) return
  if (!batDauGui()) {
    focusLoiDau('#sua-don-form')
    return
  }
  dangGui.value = true
  try {
    const res = await api.put(`/hoa-don/${props.data.hoaDon.id}/sua-don`, {
      hoTenNguoiNhan: f.hoTenNguoiNhan.trim(),
      sdtNguoiNhan: f.sdtNguoiNhan.trim(),
      diaChiGiaoHang: f.diaChiGiaoHang.trim(),
      lyDo: f.lyDo.trim(),
      items: dong.value.map((d) => ({ idChiTiet: d.idChiTiet, idBienThe: d.idBienThe, soLuong: Number(d.soLuong) })),
    })
    toast.success(`Đã sửa đơn. ${res.data?.ketQuaSua?.moTa || ''}`.trim())
    emit('saved', res.data)
  } catch (e) {
    if (datLoiMayChu(e)) focusLoiDau('#sua-don-form')
    else toast.error(e.message || 'Không sửa được đơn.')
  } finally {
    dangGui.value = false
  }
}
</script>

<template>
  <BaseModal title="Chỉnh sửa đơn hàng (chỉ 1 lần)" size="lg" static-backdrop @close="emit('close')">
    <form id="sua-don-form" novalidate @submit.prevent="luu">
      <p class="sd-luu-y" role="note">
        <i class="bi bi-info-circle" aria-hidden="true"></i>
        Chỉ sửa khi đã gọi xác nhận với khách. Mỗi đơn chỉ được sửa <b>một lần</b>; sau khi lưu, hệ thống tính lại phí giao hàng, phiếu giảm giá và
        ghi <b>hoàn phí / phụ phí</b> nếu khách đã chuyển khoản trước.
      </p>
      <div class="row g-3">
        <div class="col-md-6">
          <label class="ad-label" for="sd-ten">Người nhận <span class="ad-required">*</span></label>
          <input id="sd-ten" v-model="f.hoTenNguoiNhan" class="form-control ad-control" :class="{ 'is-invalid': loi.hoTenNguoiNhan }" maxlength="255" @input="sua('hoTenNguoiNhan')" @blur="cham('hoTenNguoiNhan')" />
          <p v-if="loi.hoTenNguoiNhan" class="ad-error">{{ loi.hoTenNguoiNhan }}</p>
        </div>
        <div class="col-md-6">
          <label class="ad-label" for="sd-sdt">Số điện thoại <span class="ad-required">*</span></label>
          <input id="sd-sdt" v-model="f.sdtNguoiNhan" type="tel" class="form-control ad-control" :class="{ 'is-invalid': loi.sdtNguoiNhan }" maxlength="15" @input="sua('sdtNguoiNhan')" @blur="cham('sdtNguoiNhan')" />
          <p v-if="loi.sdtNguoiNhan" class="ad-error">{{ loi.sdtNguoiNhan }}</p>
        </div>
        <div class="col-12">
          <label class="ad-label" for="sd-dc">Địa chỉ giao hàng <span class="ad-required">*</span></label>
          <textarea id="sd-dc" v-model="f.diaChiGiaoHang" rows="2" class="form-control ad-control" :class="{ 'is-invalid': loi.diaChiGiaoHang }" maxlength="500" @input="sua('diaChiGiaoHang')" @blur="cham('diaChiGiaoHang')"></textarea>
          <p v-if="loi.diaChiGiaoHang" class="ad-error">{{ loi.diaChiGiaoHang }}</p>
        </div>
      </div>

      <h3 class="sd-tieu-de">Sản phẩm</h3>
      <div class="ad-affix mb-2 position-relative">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input v-model="tuKhoa" type="search" class="form-control ad-control" placeholder="Tìm sản phẩm để thêm vào đơn…" aria-label="Tìm sản phẩm để thêm" autocomplete="off" />
        <ul v-if="ketQua.length" class="sd-goi-y" role="listbox">
          <li v-for="sp in ketQua" :key="sp.id">
            <button type="button" :disabled="sp.tonKho <= 0" @click="them(sp)">
              <span>{{ sp.tenSanPham }} <small class="text-muted">{{ sp.phanLoai || sp.ma }}</small></span>
              <span class="text-nowrap">{{ formatMoney(sp.donGia) }} · {{ sp.tonKho <= 0 ? 'Hết hàng' : `còn ${sp.tonKho}` }}</span>
            </button>
          </li>
        </ul>
      </div>
      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr><th class="ad-col-stt">STT</th><th>Sản phẩm</th><th class="text-end">Đơn giá</th><th class="text-center">Số lượng</th><th class="text-end">Thành tiền</th><th class="ad-col-actions"></th></tr>
            </thead>
            <tbody>
              <tr v-for="(d, i) in dong" :key="d.idChiTiet ?? `moi-${d.idBienThe}`">
                <td class="ad-col-stt">{{ i + 1 }}</td>
                <td>
                  <span class="fw-semibold d-block">{{ d.ten }}</span>
                  <small class="text-muted">{{ d.phanLoai }}</small>
                  <span v-if="d.moi" class="ad-pill ad-pill-blue ms-1">Thêm mới · giá hiện tại</span>
                </td>
                <td class="text-end ad-nowrap">{{ formatMoney(d.donGia) }}</td>
                <td class="text-center">
                  <input v-model.number="d.soLuong" type="number" min="1" :max="d.tonKho || 999" class="form-control ad-control sd-sl" :aria-label="`Số lượng ${d.ten}`" @input="sua('items')" />
                </td>
                <td class="text-end ad-nowrap fw-semibold">{{ formatMoney(d.donGia * (Number(d.soLuong) || 0)) }}</td>
                <td class="ad-col-actions">
                  <button type="button" class="ad-icon-btn is-danger" :aria-label="`Bỏ ${d.ten}`" @click="bo(d); sua('items')"><i class="bi bi-trash" aria-hidden="true"></i></button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p v-if="loi.items" class="ad-error">{{ loi.items }}</p>

      <dl class="sd-tong">
        <div><dt>Tiền hàng (tạm tính)</dt><dd>{{ formatMoney(tienHang) }}</dd></div>
        <div><dt>Phí giao hàng</dt><dd>{{ phiShip ? formatMoney(phiShip) : 'Miễn phí' }}</dd></div>
        <div><dt>Tổng trước khi sửa</dt><dd>{{ formatMoney(cu) }}</dd></div>
        <div v-if="daTra > 0"><dt>Khách đã chuyển khoản</dt><dd>{{ formatMoney(daTra) }}</dd></div>
      </dl>
      <p class="ad-hint">Phiếu giảm giá được tính lại khi lưu (đơn không còn đạt mức tối thiểu thì bỏ phiếu).</p>

      <label class="ad-label" for="sd-ly-do">Lý do chỉnh sửa <span class="ad-required">*</span></label>
      <textarea id="sd-ly-do" v-model="f.lyDo" rows="2" class="form-control ad-control" :class="{ 'is-invalid': loi.lyDo }" maxlength="500" placeholder="vd: Khách gọi đổi địa chỉ và thêm 1 ống cầu" @input="sua('lyDo')" @blur="cham('lyDo')"></textarea>
      <p v-if="loi.lyDo" class="ad-error">{{ loi.lyDo }}</p>
    </form>
    <template #footer>
      <button type="button" class="ad-btn" :disabled="dangGui" @click="emit('close')">Hủy</button>
      <button type="submit" form="sua-don-form" class="ad-btn ad-btn-primary" :disabled="dangGui" data-testid="sd-luu">
        <span v-if="dangGui" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Lưu chỉnh sửa
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.sd-luu-y {
  display: flex;
  gap: 0.5rem;
  padding: 0.65rem 0.8rem;
  border-radius: 10px;
  background: #eff6ff;
  color: #1e3a8a;
  font-size: 0.9rem;
}
.sd-tieu-de {
  font-size: 1rem;
  font-weight: 700;
  margin: 1.25rem 0 0.5rem;
}
.sd-goi-y {
  position: absolute;
  z-index: 5;
  top: 100%;
  left: 0;
  right: 0;
  margin: 0.25rem 0 0;
  padding: 0.25rem;
  list-style: none;
  background: #fff;
  border: 1px solid #e3eaf3;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(15, 27, 45, 0.12);
  max-height: 260px;
  overflow: auto;
}
.sd-goi-y button {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  border: 0;
  background: none;
  padding: 0.5rem 0.6rem;
  border-radius: 8px;
  text-align: left;
}
.sd-goi-y button:hover:not(:disabled) {
  background: #f1f5f9;
}
.sd-sl {
  width: 80px;
  margin: 0 auto;
  text-align: center;
}
.sd-tong {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.5rem;
  margin: 0.75rem 0 0.25rem;
}
.sd-tong > div {
  background: #f8fafc;
  border-radius: 10px;
  padding: 0.5rem 0.7rem;
}
.sd-tong dt {
  font-size: 0.8rem;
  font-weight: 500;
  color: #64748b;
}
.sd-tong dd {
  margin: 0;
  font-weight: 700;
}
</style>
