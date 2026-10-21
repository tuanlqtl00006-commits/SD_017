<script setup>
// Bán hàng tại quầy (giai đoạn 3).
//  - Tối đa 5 hóa đơn chờ cùng lúc (tab), lưu trên trình duyệt theo từng nhân viên: tải lại trang không mất giỏ hàng.
//  - Thêm sản phẩm: tìm theo tên / mã, máy quét mã hoặc camera. Không thêm quá tồn kho.
//  - Khách lẻ hoặc khách hàng (tìm / thêm nhanh); phiếu giảm giá tự chọn phiếu giảm nhiều nhất (đổi được).
//  - Thanh toán tiền mặt / chuyển khoản (mã QR VietQR) / kết hợp. Bắt buộc đã nhận ca.
//  - Backend tính lại toàn bộ giá, phiếu, tồn kho khi thanh toán (xem BanHangService).
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import api from '../../services/api'
import ChonKhach from './ChonKhach.vue'
import TimSanPham from './TimSanPham.vue'
import BaseModal from '../common/BaseModal.vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import MoneyInput from '../common/MoneyInput.vue'
import GiaoCaModal from '../giaoCa/GiaoCaModal.vue'
import { useAuth } from '../../composables/useAuth'
import { useGiaoCa } from '../../composables/useGiaoCa'
import { useToast } from '../../composables/useToast'
import { NGAN_HANG, PHUONG_THUC, daCauHinhNganHang } from '../../constants/thanhToan'
import { formatMoney } from '../../utils/format'
import { formatPhanTram } from '../../utils/giamGia'
import { taoHtmlHoaDon } from '../../utils/hoaDonIn'
import {
  SO_HOA_DON_CHO_TOI_DA, capNhatGia, chuoiVietQR, docHoaDonCho, doiSoLuong, ghiHoaDonCho, goiYTienKhachDua, taoHoaDonMoi,
  themVaoGio, tienGiamDot, tinhThanhToan, tongSoLuong, tongTienHang, xoaKhoiGio,
} from '../../utils/banHang'

const router = useRouter()
const toast = useToast()
const auth = useAuth()
const giaoCa = useGiaoCa()
const idNv = computed(() => auth.trangThai.nguoiDung?.id ?? null)

/* ===================== Hóa đơn chờ ===================== */
const kho = reactive(docHoaDonCho(idNv.value))
if (!kho.ds.length) kho.ds.push(taoHoaDonMoi([]))
if (!kho.ds.some((h) => h.id === kho.dangChon)) kho.dangChon = kho.ds[0].id
watch(kho, (v) => ghiHoaDonCho(idNv.value, v), { deep: true })

const hd = computed(() => kho.ds.find((h) => h.id === kho.dangChon) ?? kho.ds[0])
function themHoaDon() {
  if (kho.ds.length >= SO_HOA_DON_CHO_TOI_DA) {
    toast.error(`Tối đa ${SO_HOA_DON_CHO_TOI_DA} hóa đơn chờ. Hãy thanh toán hoặc xóa bớt.`)
    return
  }
  const moi = taoHoaDonMoi(kho.ds)
  kho.ds.push(moi)
  kho.dangChon = moi.id
}
const xoaHd = ref(null)
function hoiXoaHoaDon(h) {
  if (h.gio.length) xoaHd.value = h
  else xoaHoaDon(h)
}
function xoaHoaDon(h) {
  const i = kho.ds.findIndex((x) => x.id === h.id)
  kho.ds.splice(i, 1)
  if (!kho.ds.length) kho.ds.push(taoHoaDonMoi([]))
  if (kho.dangChon === h.id) kho.dangChon = kho.ds[Math.max(0, i - 1)].id
  xoaHd.value = null
}

/* ===================== Giỏ hàng ===================== */
const timSp = ref(null)
function themSanPham(sp, loiQuet) {
  if (!sp) {
    toast.error(loiQuet)
    return
  }
  const kq = themVaoGio(hd.value.gio, sp)
  if (kq.loi) toast.error(kq.loi)
  else hd.value.gio = kq.gio
}
function doiSl(d, sl) {
  const kq = doiSoLuong(hd.value.gio, d.idBienThe, sl)
  hd.value.gio = kq.gio
  if (kq.loi) toast.error(kq.loi)
}
function xoaDong(d) {
  hd.value.gio = xoaKhoiGio(hd.value.gio, d.idBienThe)
}
const tienHang = computed(() => tongTienHang(hd.value.gio))
const giamDot = computed(() => tienGiamDot(hd.value.gio))

/* ===================== Phiếu giảm giá ===================== */
const phieus = ref([])
const dangTaiPhieu = ref(false)
let henPhieu = null
async function taiPhieu() {
  const h = hd.value
  if (!h.gio.length) {
    phieus.value = []
    if (h.tuDongPhieu) h.maPhieu = ''
    return
  }
  dangTaiPhieu.value = true
  try {
    phieus.value = (await api.get('/ban-hang/phieu-giam-gia', { params: { idKhachHang: h.khach?.id, tongTien: tienHang.value } })).data
    if (h.tuDongPhieu) h.maPhieu = phieus.value.find((p) => p.apDungDuoc)?.ma ?? ''
  } catch (e) {
    phieus.value = []
    toast.error(e.message || 'Không tải được phiếu giảm giá.')
  } finally {
    dangTaiPhieu.value = false
  }
}
watch(
  () => [hd.value.id, hd.value.khach?.id, tienHang.value],
  () => {
    clearTimeout(henPhieu)
    henPhieu = setTimeout(taiPhieu, 300)
  },
  { immediate: true },
)
const phieuChon = computed(() => phieus.value.find((p) => p.ma === hd.value.maPhieu) ?? null)
const giamPhieu = computed(() => (phieuChon.value?.apDungDuoc ? Number(phieuChon.value.soTienGiam) : 0))
const loiPhieu = computed(() => {
  if (!hd.value.maPhieu || dangTaiPhieu.value) return ''
  if (!phieuChon.value) return `Phiếu ${hd.value.maPhieu} không còn dùng được cho đơn này.`
  return phieuChon.value.apDungDuoc ? '' : phieuChon.value.lyDo
})
function chonPhieu(ma) {
  hd.value.maPhieu = ma
  hd.value.tuDongPhieu = false
}
function moTaPhieu(p) {
  const giam = p.loaiGiam === 'PHAN_TRAM' ? `Giảm ${formatPhanTram(p.giaTri)}${p.giamToiDa ? `, tối đa ${formatMoney(p.giamToiDa)}` : ''}` : `Giảm ${formatMoney(p.giaTri)}`
  return `${giam}${p.donToiThieu ? ` · đơn từ ${formatMoney(p.donToiThieu)}` : ''}`
}

/* ===================== Khách ===================== */
function chonKhach(k) {
  hd.value.khach = k
  if (!hd.value.tuDongPhieu && phieuChon.value?.caNhan) {
    hd.value.maPhieu = ''
    hd.value.tuDongPhieu = true
  }
}

/* ===================== Thanh toán ===================== */
const tt = computed(() =>
  tinhThanhToan({
    tongTien: tienHang.value,
    giam: giamPhieu.value,
    phuongThuc: hd.value.phuongThuc,
    tienMat: hd.value.tienMat,
    chuyenKhoan: hd.value.chuyenKhoan,
  }),
)
const goiY = computed(() => goiYTienKhachDua(tt.value.phaiTra - (hd.value.phuongThuc === 'KET_HOP' ? tt.value.chuyenKhoan : 0)))
const coQr = daCauHinhNganHang()
const qrUrl = ref('')
const noiDungCk = computed(() => `FOOTSTYLE ${hd.value.id.slice(-6).toUpperCase()}`)
watch(
  () => [tt.value.chuyenKhoan, noiDungCk.value, hd.value.phuongThuc],
  async ([ck]) => {
    qrUrl.value = ''
    if (!coQr || !ck || hd.value.phuongThuc === 'TIEN_MAT') return
    try {
      qrUrl.value = await QRCode.toDataURL(chuoiVietQR({ bin: NGAN_HANG.bin, soTaiKhoan: NGAN_HANG.soTaiKhoan, soTien: ck, noiDung: noiDungCk.value }), { width: 220, margin: 1 })
    } catch {
      qrUrl.value = ''
    }
  },
  { immediate: true },
)
function chonPhuongThuc(ma) {
  hd.value.phuongThuc = ma
  if (ma === 'CHUYEN_KHOAN') hd.value.tienMat = ''
}

const lyDoKhongThanhToan = computed(() => {
  if (!giaoCa.dangTrongCa.value) return 'Bạn chưa nhận ca.'
  if (!hd.value.gio.length) return 'Hóa đơn chưa có sản phẩm.'
  if (loiPhieu.value) return loiPhieu.value
  if (tt.value.loi === 'Nhập tiền khách đưa.') return ''
  return tt.value.loi
})
const xacNhan = ref(false)
const dangGui = ref(false)
const xong = ref(null)

function moXacNhan() {
  if (lyDoKhongThanhToan.value) {
    toast.error(lyDoKhongThanhToan.value)
    if (!giaoCa.dangTrongCa.value) moCa.value = true
    return
  }
  xacNhan.value = true
}

async function thanhToan() {
  if (dangGui.value) return
  dangGui.value = true
  const h = hd.value
  const tienMatGui =
    h.phuongThuc === 'CHUYEN_KHOAN'
      ? 0
      : h.tienMat !== '' && h.tienMat !== null && h.tienMat !== undefined
        ? tt.value.tienMat
        : Math.max(0, tt.value.phaiTra - (h.phuongThuc === 'KET_HOP' ? tt.value.chuyenKhoan : 0))
  try {
    const res = await api.post('/ban-hang/thanh-toan', {
      idKhachHang: h.khach?.id ?? null,
      maPhieu: h.maPhieu || null,
      ghiChu: h.ghiChu?.trim() || null,
      items: h.gio.map((d) => ({ idBienThe: d.idBienThe, soLuong: d.soLuong, donGia: d.donGia })),
      tienMat: tienMatGui,
      chuyenKhoan: tt.value.chuyenKhoan,
      maGiaoDich: h.phuongThuc === 'TIEN_MAT' ? null : h.maGiaoDich?.trim() || null,
    })
    xacNhan.value = false
    xong.value = res.data
    xoaHoaDon(h)
    giaoCa.taiLai()
    timSp.value?.taiLai()
  } catch (e) {
    xacNhan.value = false
    if (e.code === 'GIA_THAY_DOI') {
      h.gio = capNhatGia(h.gio, e.chiTiet?.items)
      toast.error(`${e.message} Giá trong hóa đơn đã được cập nhật.`)
    } else if (e.code === 'CHUA_NHAN_CA') {
      toast.error(e.message)
      await giaoCa.taiLai()
      moCa.value = true
    } else if (e.code === 'PHIEU_KHONG_HOP_LE') {
      toast.error(e.message)
      h.tuDongPhieu = true
      taiPhieu()
    } else {
      toast.error(e.message || 'Thanh toán không thành công.')
      if (e.code === 'HET_HANG') timSp.value?.taiLai()
    }
  } finally {
    dangGui.value = false
  }
}

/* ----- In hóa đơn vừa bán (dùng mẫu in của trang Hóa đơn) ----- */
async function inHoaDon() {
  const w = window.open('', '_blank', 'width=820,height=900')
  if (!w) {
    toast.error('Trình duyệt đã chặn cửa sổ in. Hãy cho phép cửa sổ bật lên rồi thử lại.')
    return
  }
  try {
    const ct = (await api.get(`/hoa-don/${xong.value.id}`)).data
    const qr = await QRCode.toDataURL(String(ct.hoaDon.maHoaDon), { width: 240, margin: 1 })
    w.document.write(taoHtmlHoaDon({ hd: ct.hoaDon, khach: ct.khachHang, giao: ct.giaoHang, sanPham: ct.sanPham, daThanhToan: ct.daThanhToan, qrUrl: qr, goc: location.origin }))
    w.document.close()
  } catch (e) {
    w.close()
    toast.error(e.message || 'Không in được hóa đơn.')
  }
}

/* ----- Ca làm việc ----- */
const moCa = ref(false)
onMounted(() => {
  if (!giaoCa.trangThai.daTai) giaoCa.taiLai()
  window.addEventListener('keydown', phimTat)
})
onBeforeUnmount(() => window.removeEventListener('keydown', phimTat))
function phimTat(e) {
  if (e.key === 'F2') {
    e.preventDefault()
    timSp.value?.focusTim()
  } else if (e.key === 'F9') {
    e.preventDefault()
    moXacNhan()
  }
}
const soLuongTab = (h) => tongSoLuong(h.gio)
</script>

<template>
  <div class="ad-page bh">
    <div v-if="giaoCa.trangThai.daTai && !giaoCa.dangTrongCa.value" class="bh-chua-ca" role="alert">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <span>Bạn chưa nhận ca. Vẫn chuẩn bị được hóa đơn nhưng phải <b>nhận ca</b> trước khi thanh toán.</span>
      <button type="button" class="ad-btn ad-btn-primary ad-btn-sm" @click="moCa = true">Nhận ca</button>
    </div>

    <!-- Tab hóa đơn chờ -->
    <div class="bh-tabs" role="tablist" aria-label="Hóa đơn chờ">
      <div v-for="h in kho.ds" :key="h.id" class="bh-tab" :class="{ active: h.id === kho.dangChon }">
        <button type="button" role="tab" :aria-selected="h.id === kho.dangChon" class="bh-tab-ten" @click="kho.dangChon = h.id">
          <i class="bi bi-receipt" aria-hidden="true"></i> {{ h.ten }}
          <span v-if="soLuongTab(h)" class="bh-tab-so">{{ soLuongTab(h) }}</span>
        </button>
        <button type="button" class="bh-tab-xoa" :aria-label="`Xóa ${h.ten}`" title="Xóa hóa đơn chờ" @click="hoiXoaHoaDon(h)"><i class="bi bi-x" aria-hidden="true"></i></button>
      </div>
      <button type="button" class="bh-tab-them" :disabled="kho.ds.length >= SO_HOA_DON_CHO_TOI_DA" @click="themHoaDon">
        <i class="bi bi-plus-lg" aria-hidden="true"></i> Hóa đơn mới
      </button>
      <span class="bh-phim">F2: tìm sản phẩm · F9: thanh toán</span>
    </div>

    <div class="bh-luoi">
      <!-- Trái: sản phẩm + giỏ -->
      <div class="bh-trai">
        <TimSanPham ref="timSp" @chon="themSanPham" />

        <section class="ad-card">
          <header class="ad-list-head">
            <div class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-cart3"></i></span>
              <h2 class="ad-card-title">Giỏ hàng · {{ hd.ten }}</h2>
            </div>
            <span class="ad-count">{{ tongSoLuong(hd.gio) }} sản phẩm</span>
          </header>
          <div class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table bh-gio">
                <thead>
                  <tr>
                    <th class="ad-col-stt">STT</th>
                    <th>Sản phẩm</th>
                    <th class="text-end">Đơn giá</th>
                    <th class="text-center">Số lượng</th>
                    <th class="text-end">Thành tiền</th>
                    <th class="ad-col-actions"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!hd.gio.length">
                    <td colspan="6">
                      <div class="ad-empty">
                        <i class="bi bi-cart" aria-hidden="true"></i>
                        <strong>Chưa có sản phẩm</strong>
                        <span>Chọn sản phẩm ở trên hoặc quét mã để thêm vào hóa đơn.</span>
                      </div>
                    </td>
                  </tr>
                  <tr v-for="(d, i) in hd.gio" :key="d.idBienThe">
                    <td class="ad-col-stt">{{ i + 1 }}</td>
                    <td>
                      <div class="bh-dong-sp">
                        <span class="bh-dong-anh"><img v-if="d.anh" :src="d.anh" alt="" /><i v-else class="bi bi-image" aria-hidden="true"></i></span>
                        <span>
                          <span class="fw-semibold d-block">{{ d.ten }}</span>
                          <small class="text-muted">{{ d.phanLoai }} · <span class="ad-code">{{ d.ma }}</span></small>
                        </span>
                      </div>
                    </td>
                    <td class="text-end ad-nowrap">
                      <div>{{ formatMoney(d.donGia) }}</div>
                      <small v-if="d.donGia < d.giaGoc" class="text-muted"><s>{{ formatMoney(d.giaGoc) }}</s> -{{ formatPhanTram(d.phanTramGiam) }}</small>
                    </td>
                    <td class="text-center">
                      <div class="bh-sl">
                        <button type="button" :aria-label="`Giảm số lượng ${d.ten}`" :disabled="d.soLuong <= 1" @click="doiSl(d, d.soLuong - 1)"><i class="bi bi-dash"></i></button>
                        <input :value="d.soLuong" type="number" min="1" :max="d.tonKho" :aria-label="`Số lượng ${d.ten}`" @change="doiSl(d, $event.target.value)" />
                        <button type="button" :aria-label="`Tăng số lượng ${d.ten}`" :disabled="d.soLuong >= d.tonKho" @click="doiSl(d, d.soLuong + 1)"><i class="bi bi-plus"></i></button>
                      </div>
                      <small class="text-muted">Còn {{ d.tonKho }}</small>
                    </td>
                    <td class="text-end fw-semibold ad-nowrap">{{ formatMoney(d.donGia * d.soLuong) }}</td>
                    <td class="ad-col-actions">
                      <button type="button" class="ad-icon-btn is-danger" :aria-label="`Bỏ ${d.ten} khỏi hóa đơn`" title="Bỏ khỏi hóa đơn" @click="xoaDong(d)"><i class="bi bi-trash" aria-hidden="true"></i></button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      <!-- Phải: khách, phiếu, thanh toán -->
      <aside class="bh-phai">
        <section class="ad-card bh-khoi">
          <h3 class="bh-tieu-de"><i class="bi bi-person" aria-hidden="true"></i> Khách hàng</h3>
          <ChonKhach :khach="hd.khach" @chon="chonKhach" />
        </section>

        <section class="ad-card bh-khoi">
          <h3 class="bh-tieu-de"><i class="bi bi-ticket-perforated" aria-hidden="true"></i> Phiếu giảm giá</h3>
          <p v-if="!hd.gio.length" class="ad-hint m-0">Thêm sản phẩm để xem phiếu dùng được.</p>
          <p v-else-if="dangTaiPhieu && !phieus.length" class="ad-hint m-0">Đang tải phiếu…</p>
          <div v-else class="bh-phieu-ds" role="radiogroup" aria-label="Chọn phiếu giảm giá">
            <label class="bh-phieu" :class="{ active: !hd.maPhieu }">
              <input type="radio" class="form-check-input" :checked="!hd.maPhieu" @change="chonPhieu('')" />
              <span>Không dùng phiếu</span>
            </label>
            <label v-for="p in phieus" :key="p.id" class="bh-phieu" :class="{ active: hd.maPhieu === p.ma, 'is-khong': !p.apDungDuoc }">
              <input type="radio" class="form-check-input" :checked="hd.maPhieu === p.ma" :disabled="!p.apDungDuoc" @change="chonPhieu(p.ma)" />
              <span class="flex-grow-1">
                <span class="d-flex justify-content-between gap-2">
                  <b>{{ p.ma }}</b>
                  <b v-if="p.apDungDuoc" class="text-success">-{{ formatMoney(p.soTienGiam) }}</b>
                </span>
                <small class="d-block">{{ p.ten }}<span v-if="p.caNhan" class="ad-pill ad-pill-purple ms-1">Cá nhân</span></small>
                <small class="d-block text-muted">{{ moTaPhieu(p) }}</small>
                <small v-if="!p.apDungDuoc" class="d-block text-danger">{{ p.lyDo }}</small>
              </span>
            </label>
          </div>
          <p v-if="loiPhieu" class="ad-error mb-0">{{ loiPhieu }}</p>
        </section>

        <section class="ad-card bh-khoi">
          <h3 class="bh-tieu-de"><i class="bi bi-cash-stack" aria-hidden="true"></i> Thanh toán</h3>
          <dl class="bh-tong">
            <div><dt>Tiền hàng</dt><dd>{{ formatMoney(tienHang) }}</dd></div>
            <div v-if="giamDot > 0" class="text-muted"><dt>Đã giảm theo đợt</dt><dd>{{ formatMoney(giamDot) }}</dd></div>
            <div><dt>Phiếu giảm giá{{ phieuChon && giamPhieu ? ` (${phieuChon.ma})` : '' }}</dt><dd>-{{ formatMoney(giamPhieu) }}</dd></div>
            <div class="bh-phai-tra"><dt>Khách phải trả</dt><dd>{{ formatMoney(tt.phaiTra) }}</dd></div>
          </dl>

          <div class="ad-segment bh-pt" role="radiogroup" aria-label="Phương thức thanh toán">
            <button v-for="p in PHUONG_THUC" :key="p.ma" type="button" class="ad-segment-item" :class="{ active: hd.phuongThuc === p.ma }" role="radio" :aria-checked="hd.phuongThuc === p.ma" @click="chonPhuongThuc(p.ma)">
              <i class="bi" :class="p.icon" aria-hidden="true"></i> {{ p.ten }}
            </button>
          </div>

          <div v-if="hd.phuongThuc === 'KET_HOP'" class="mt-2">
            <label class="ad-label" for="bh-ck">Chuyển khoản</label>
            <MoneyInput id="bh-ck" v-model="hd.chuyenKhoan" aria-label="Số tiền chuyển khoản" placeholder="Số tiền khách chuyển khoản" />
          </div>
          <div v-if="hd.phuongThuc !== 'CHUYEN_KHOAN'" class="mt-2">
            <div class="d-flex align-items-center justify-content-between mb-1">
              <label class="ad-label m-0" for="bh-tien-mat">Tiền khách đưa (tiền mặt)</label>
              <button
                v-if="hd.tienMat"
                type="button"
                class="btn btn-link btn-sm p-0 text-decoration-none small text-primary"
                @click="hd.tienMat = ''"
              >
                Đúng số tiền
              </button>
            </div>
            <MoneyInput id="bh-tien-mat" v-model="hd.tienMat" aria-label="Tiền khách đưa" :placeholder="`Mặc định: ${formatMoney(tt.phaiTra)}`" />
            <div class="bh-goi-y">
              <button
                v-for="g in goiY"
                :key="g"
                type="button"
                class="bh-chip"
                :class="{ active: (!hd.tienMat && g === tt.phaiTra) || hd.tienMat === g }"
                @click="hd.tienMat = (g === tt.phaiTra && hd.tienMat === g ? '' : g)"
              >
                {{ g === tt.phaiTra ? 'Đúng số tiền' : formatMoney(g) }}
              </button>
            </div>
          </div>
          <div v-if="hd.phuongThuc !== 'TIEN_MAT' && tt.chuyenKhoan > 0" class="bh-qr">
            <img v-if="qrUrl" :src="qrUrl" :alt="`Mã QR chuyển khoản ${formatMoney(tt.chuyenKhoan)}`" width="150" height="150" />
            <div>
              <template v-if="coQr">
                <div class="fw-semibold">{{ NGAN_HANG.tenNganHang || 'Ngân hàng' }} · {{ NGAN_HANG.soTaiKhoan }}</div>
                <div class="small">{{ NGAN_HANG.chuTaiKhoan }}</div>
                <div class="small">Số tiền: <b>{{ formatMoney(tt.chuyenKhoan) }}</b></div>
                <div class="small">Nội dung: <b>{{ noiDungCk }}</b></div>
              </template>
              <div v-else class="small text-muted">Chưa cấu hình tài khoản nhận tiền nên không hiện mã QR (xem frontend/src/constants/thanhToan.js).</div>
              <label class="ad-label mt-2" for="bh-ma-gd">Mã giao dịch</label>
              <input id="bh-ma-gd" v-model="hd.maGiaoDich" type="text" class="form-control ad-control" maxlength="100" placeholder="Mã giao dịch ngân hàng (nếu có)" />
            </div>
          </div>

          <dl class="bh-tong mt-2">
            <div v-if="tt.conThieu > 0 && hd.tienMat" class="text-danger fw-semibold"><dt>Còn thiếu</dt><dd>{{ formatMoney(tt.conThieu) }}</dd></div>
            <div v-if="tt.tienThua > 0" class="bh-thua"><dt>Tiền thừa trả khách</dt><dd>{{ formatMoney(tt.tienThua) }}</dd></div>
          </dl>
          <label class="ad-label" for="bh-ghi-chu">Ghi chú</label>
          <input id="bh-ghi-chu" v-model="hd.ghiChu" type="text" class="form-control ad-control" maxlength="500" placeholder="Ghi chú cho hóa đơn (không bắt buộc)" />

          <button type="button" class="ad-btn ad-btn-primary bh-nut-tt" :disabled="dangGui" :title="lyDoKhongThanhToan" @click="moXacNhan">
            <i class="bi bi-check2-circle" aria-hidden="true"></i> Thanh toán {{ formatMoney(tt.phaiTra) }}
          </button>
          <p v-if="lyDoKhongThanhToan && hd.gio.length" class="ad-hint text-center mb-0 mt-1">{{ lyDoKhongThanhToan }}</p>
        </section>
      </aside>
    </div>

    <!-- Hộp thoại xác nhận thanh toán chuẩn nghiệp vụ POS -->
    <BaseModal v-if="xacNhan" title="Xác nhận thanh toán" size="md" @close="xacNhan = false">
      <div class="bh-xn-modal">
        <!-- Khối hiển thị số tiền thanh toán chính nổi bật -->
        <div class="bh-xn-tong">
          <div class="bh-xn-tong-nhan">TỔNG TIỀN THANH TOÁN</div>
          <div class="bh-xn-tong-so">{{ formatMoney(tt.phaiTra) }}</div>
        </div>

        <!-- Bảng chi tiết tóm tắt đơn thanh toán -->
        <div class="bh-xn-thong-tin">
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-receipt me-1 text-primary"></i>Hóa đơn:</span>
            <span class="bh-xn-gia-tri fw-semibold">{{ hd.ten }} ({{ tongSoLuong(hd.gio) }} sản phẩm)</span>
          </div>
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-person me-1 text-primary"></i>Khách hàng:</span>
            <span class="bh-xn-gia-tri">{{ hd.khach?.hoTen ? `${hd.khach.hoTen} (${hd.khach.sdt || '—'})` : 'Khách lẻ (Khách vãng lai)' }}</span>
          </div>
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-credit-card me-1 text-primary"></i>Phương thức:</span>
            <span class="bh-xn-gia-tri fw-medium">
              <span class="ad-pill" :class="hd.phuongThuc === 'CHUYEN_KHOAN' ? 'ad-pill-blue' : hd.phuongThuc === 'KET_HOP' ? 'ad-pill-purple' : 'ad-pill-green'">
                {{ hd.phuongThuc === 'CHUYEN_KHOAN' ? 'Chuyển khoản' : hd.phuongThuc === 'KET_HOP' ? 'Kết hợp' : 'Tiền mặt' }}
              </span>
            </span>
          </div>
          <div v-if="tt.chuyenKhoan > 0" class="bh-xn-dong">
            <span class="bh-xn-nhan">Tiền chuyển khoản:</span>
            <span class="bh-xn-gia-tri fw-semibold text-primary">{{ formatMoney(tt.chuyenKhoan) }}</span>
          </div>
          <div v-if="tt.tienMat > 0 && hd.phuongThuc !== 'CHUYEN_KHOAN'" class="bh-xn-dong">
            <span class="bh-xn-nhan">Tiền khách đưa:</span>
            <span class="bh-xn-gia-tri fw-semibold text-dark">{{ formatMoney(tt.tienMat) }}</span>
          </div>
          <div v-if="tt.tienThua > 0" class="bh-xn-dong bh-xn-dong-thua">
            <span class="bh-xn-nhan text-success fw-semibold"><i class="bi bi-cash-stack me-1"></i>Tiền thừa trả khách:</span>
            <span class="bh-xn-gia-tri text-success fw-bold fs-6">{{ formatMoney(tt.tienThua) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <button type="button" class="ad-btn" :disabled="dangGui" @click="xacNhan = false">
          Hủy
        </button>
        <button
          type="button"
          class="ad-btn ad-btn-primary px-4 fw-medium"
          :disabled="dangGui"
          @click="thanhToan"
        ><span v-if="dangGui" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Thanh toán</button>
      </template>
    </BaseModal>
    <ConfirmDialog
      v-if="xoaHd"
      title="Xóa hóa đơn chờ"
      :message="`${xoaHd.ten} đang có ${tongSoLuong(xoaHd.gio)} sản phẩm. Xóa hóa đơn chờ này?`"
      confirm-text="Xóa"
      variant="danger"
      @confirm="xoaHoaDon(xoaHd)"
      @cancel="xoaHd = null"
    />
    <BaseModal v-if="xong" :title="`Đã thanh toán ${xong.maHoaDon}`" size="sm" @close="xong = null">
      <div class="bh-xong py-2">
        <i class="bi bi-check-circle-fill text-success fs-1" aria-hidden="true"></i>
        <div class="bh-xong-tien text-primary fw-bold fs-3 mt-1">{{ formatMoney(xong.thanhTien) }}</div>
        <div v-if="xong.tienThua > 0" class="bh-thua mt-1">Trả lại khách: <b>{{ formatMoney(xong.tienThua) }}</b></div>
        <div class="text-muted small mt-2">Thanh toán hoàn tất thành công.</div>
      </div>
      <template #footer>
        <button type="button" class="ad-btn" @click="router.push(`/hoa-don/${xong.id}`)">Xem hóa đơn</button>
        <button type="button" class="ad-btn ad-btn-primary" @click="inHoaDon"><i class="bi bi-printer" aria-hidden="true"></i> In hóa đơn</button>
      </template>
    </BaseModal>
    <GiaoCaModal v-if="moCa" @close="moCa = false" />
  </div>
</template>

<style scoped>
.bh-chua-ca {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #fff4e5;
  color: #92400e;
  border-radius: 12px;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
}
.bh-chua-ca span {
  flex: 1;
}
.bh-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 1rem;
}
.bh-tab {
  display: flex;
  align-items: center;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}
.bh-tab.active {
  border-color: var(--fs-primary, #0977ec);
  background: var(--fs-primary-soft, #e7f1fd);
}
.bh-tab-ten {
  border: 0;
  background: transparent;
  padding: 0.45rem 0.4rem 0.45rem 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.bh-tab.active .bh-tab-ten {
  color: var(--fs-primary-dark, #075fc0);
}
.bh-tab-so {
  min-width: 1.3rem;
  border-radius: 999px;
  background: var(--fs-primary, #0977ec);
  color: #fff;
  font-size: 0.72rem;
  padding: 0 0.35rem;
}
.bh-tab-xoa {
  border: 0;
  background: transparent;
  padding: 0.45rem 0.55rem;
  color: var(--fs-muted, #7b8794);
}
.bh-tab-xoa:hover {
  color: #c62828;
}
.bh-tab-them {
  border: 1px dashed #c9d3df;
  background: transparent;
  border-radius: 10px;
  padding: 0.45rem 0.75rem;
  font-weight: 600;
  color: var(--fs-text-2, #4b5563);
}
.bh-tab-them:disabled {
  opacity: 0.5;
}
.bh-phim {
  margin-left: auto;
  font-size: 0.8rem;
  color: var(--fs-muted, #7b8794);
}
.bh-luoi {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 1rem;
  align-items: start;
}
.bh-trai {
  display: grid;
  gap: 1rem;
  min-width: 0;
}
.bh-phai {
  display: grid;
  gap: 1rem;
  position: sticky;
  top: 1rem;
}
.bh-khoi {
  padding: 1rem;
}
.bh-tieu-de {
  font-size: 0.98rem;
  font-weight: 700;
  margin: 0 0 0.75rem;
  display: flex;
  gap: 0.45rem;
  align-items: center;
}
.bh-dong-sp {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}
.bh-dong-anh {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: var(--fs-surface, #f7f9fc);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #c3ccd7;
  flex-shrink: 0;
}
.bh-dong-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.bh-sl {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 9px;
  overflow: hidden;
}
.bh-sl button {
  border: 0;
  background: var(--fs-surface, #f7f9fc);
  width: 30px;
  height: 32px;
}
.bh-sl button:disabled {
  opacity: 0.4;
}
.bh-sl input {
  width: 52px;
  border: 0;
  text-align: center;
  height: 32px;
  -moz-appearance: textfield;
}
.bh-sl input::-webkit-outer-spin-button,
.bh-sl input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.bh-phieu-ds {
  display: grid;
  gap: 0.4rem;
  max-height: 260px;
  overflow: auto;
}
.bh-phieu {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  padding: 0.5rem 0.65rem;
  cursor: pointer;
}
.bh-phieu.active {
  border-color: var(--fs-primary, #0977ec);
  background: var(--fs-primary-soft, #e7f1fd);
}
.bh-phieu.is-khong {
  opacity: 0.7;
  cursor: not-allowed;
}
.bh-tong {
  margin: 0 0 0.75rem;
  display: grid;
  gap: 0.35rem;
}
.bh-tong > div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}
.bh-tong dt {
  font-weight: 500;
}
.bh-tong dd {
  margin: 0;
  font-weight: 600;
  white-space: nowrap;
}
.bh-phai-tra {
  border-top: 1px dashed var(--fs-line, #e6eaf0);
  padding-top: 0.45rem;
  font-size: 1.15rem;
  color: var(--fs-primary-dark, #075fc0);
}
.bh-thua {
  color: #15803d;
  font-weight: 700;
  font-size: 1.05rem;
}
.bh-pt .ad-segment-item {
  border: 0;
  background: transparent;
  white-space: nowrap;
  font-size: 0.82rem;
  padding: 0 0.35rem;
}
.bh-pt .ad-segment-item.active {
  background: #fff;
}
.bh-goi-y {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.4rem;
}
.bh-chip {
  border: 1px solid var(--fs-line, #e6eaf0);
  background: #fff;
  border-radius: 999px;
  padding: 0.2rem 0.6rem;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.bh-chip:hover {
  border-color: var(--fs-primary, #0977ec);
  color: var(--fs-primary, #0977ec);
}
.bh-chip.active {
  background: var(--fs-primary, #0977ec);
  border-color: var(--fs-primary, #0977ec);
  color: #fff;
  font-weight: 600;
}
.bh-qr {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  margin-top: 0.75rem;
  padding: 0.65rem;
  border-radius: 12px;
  background: var(--fs-surface, #f7f9fc);
}
.bh-nut-tt {
  width: 100%;
  justify-content: center;
  padding: 0.8rem;
  font-size: 1.05rem;
  margin-top: 1rem;
}
.bh-xong {
  text-align: center;
  display: grid;
  gap: 0.35rem;
}
.bh-xong > i {
  font-size: 2.6rem;
  color: #17a34a;
}
.bh-xong-tien {
  font-size: 1.6rem;
  font-weight: 700;
}
/* Modal xác nhận thanh toán */
.bh-xn-modal {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.bh-xn-tong {
  text-align: center;
  padding: 1rem;
  background: #f0f7ff;
  border: 1px solid #cbe2fe;
  border-radius: 12px;
}
.bh-xn-tong-nhan {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--fs-primary-dark, #075fc0);
  margin-bottom: 0.25rem;
}
.bh-xn-tong-so {
  font-size: 1.85rem;
  font-weight: 800;
  color: var(--fs-primary, #0977ec);
  line-height: 1.1;
}
.bh-xn-thong-tin {
  background: var(--fs-surface, #f8fafc);
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  padding: 0.4rem 0.9rem;
}
.bh-xn-dong {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--fs-line-soft, #edf1f6);
  font-size: 0.92rem;
}
.bh-xn-dong:last-child {
  border-bottom: 0;
}
.bh-xn-nhan {
  color: #64748b;
}
.bh-xn-gia-tri {
  color: #1e293b;
  text-align: right;
}
.bh-xn-dong-thua {
  background: #f0fdf4;
  margin: 0.2rem -0.9rem -0.4rem;
  padding: 0.6rem 0.9rem;
  border-radius: 0 0 11px 11px;
  border-top: 1px solid #bbf7d0;
}
@media (max-width: 1200px) {
  .bh-luoi {
    grid-template-columns: 1fr;
  }
  .bh-phai {
    position: static;
  }
}
</style>
