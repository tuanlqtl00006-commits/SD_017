<script setup>
// Quản lý Lịch làm việc & Chấm công tính lương: phân chia ca trực theo tuần cho Thu ngân và Nhân viên bán hàng.
import { computed, onMounted, ref } from 'vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import CaLamViecTab from './CaLamViecTab.vue'
import XepLichModal from './XepLichModal.vue'
import LichImportModal from './LichImportModal.vue'
import { lichLamViecService } from '../../services/lichLamViecService'
import { nhanVienService } from '../../services/nhanVienService'
import { useToast } from '../../composables/useToast'
import { formatDate, todayIso } from '../../utils/format'
import { xuatLichTuan } from '../../utils/mauLichLamViec'
import { congNgayIso, demTheoCa, gomLich, mauCa, ngayTrongTuan, nhanTuan, thu2CuaTuan } from '../../utils/lichTuan'
import { exportExcel } from '../../utils/exportExcel'

const toast = useToast()
const tab = ref('lich') // 'lich' | 'chamCong' | 'ca'
const homNay = ref(todayIso())
const thu2 = ref(thu2CuaTuan(homNay.value))
const ngays = computed(() => ngayTrongTuan(thu2.value))
const cas = ref([])
const nhanViens = ref([])
const lich = ref([])
const dangTai = ref(true)

// Tìm kiếm & Lọc vai trò
const timKiemNv = ref('')
const boLocVaiTro = ref('tatCa') // 'tatCa' | 'nhanVien' | 'quanLy'

function boDau(s) {
  return String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()
}

const laQuanLy = (nv) => /quản lý/i.test(nv?.vaiTro || '')
const dsNhanVien = computed(() => nhanViens.value.filter((nv) => !laQuanLy(nv)))
const dsQuanLy = computed(() => nhanViens.value.filter(laQuanLy))

const nhanViensHienThi = computed(() => {
  let list = nhanViens.value
  if (boLocVaiTro.value === 'nhanVien') list = dsNhanVien.value
  else if (boLocVaiTro.value === 'quanLy') list = dsQuanLy.value

  const q = boDau(timKiemNv.value)
  if (!q) return list
  return list.filter((nv) => boDau(`${nv.hoTen} ${nv.maNhanVien}`).includes(q))
})

// Bảng chấm công theo tuần để phục vụ tính lương
const timKiemChamCong = ref('')
const dsChamCong = computed(() => {
  const ds = []
  let stt = 1
  for (const item of lich.value) {
    const nv = nhanViens.value.find((n) => n.id === item.idNhanVien) || { hoTen: item.hoTen, maNhanVien: item.maNhanVien, vaiTro: '' }
    const role = (item.laThuNgan || /thu ngân/i.test(item.ghiChu || '')) ? 'Thu ngân' : laQuanLy(nv) ? 'Quản lý' : 'Nhân viên bán hàng'
    if (boLocVaiTro.value === 'nhanVien' && laQuanLy(nv)) continue
    if (boLocVaiTro.value === 'quanLy' && !laQuanLy(nv)) continue

    const homNayIso = todayIso()
    const daLam = item.ngayLam < homNayIso || (item.ngayLam === homNayIso && item.daQua)
    const gioVao = daLam ? `${item.gioBatDau}` : item.ngayLam === homNayIso ? `${item.gioBatDau}` : '—'
    const gioRa = daLam ? `${item.gioKetThuc}` : '—'
    const trangThai = daLam ? 'Hoàn thành' : item.ngayLam === homNayIso ? 'Đang trong ca' : 'Chưa tới'

    ds.push({
      stt: stt++,
      id: item.id,
      idNhanVien: item.idNhanVien,
      maNhanVien: item.maNhanVien,
      hoTen: item.hoTen,
      vaiTro: role,
      ngayLam: item.ngayLam,
      tenCa: item.tenCa,
      gioCa: `${item.gioBatDau} - ${item.gioKetThuc}`,
      gioVao,
      gioRa,
      trangThai,
      gioCong: daLam ? 7 : item.ngayLam === homNayIso ? 4 : 0,
    })
  }
  return ds
})

const dsChamCongHienThi = computed(() => {
  const q = boDau(timKiemChamCong.value)
  if (!q) return dsChamCong.value
  return dsChamCong.value.filter((cc) => boDau(`${cc.hoTen} ${cc.maNhanVien} ${cc.tenCa}`).includes(q))
})

function xuatChamCongExcel() {
  if (!dsChamCong.value.length) {
    toast.error('Chưa có dữ liệu chấm công để xuất.')
    return
  }
  exportExcel({
    fileName: `Bang_Cham_Cong_${thu2.value}`,
    sheets: [
      {
        sheetName: 'Chấm công',
        title: 'BẢNG TỔNG HỢP CHẤM CÔNG & GIỜ LÀM NHÂN VIÊN',
        columns: [
          { header: 'STT', key: 'stt', width: 8, align: 'center' },
          { header: 'Mã NV', key: 'maNhanVien', width: 14, align: 'center' },
          { header: 'Họ và tên', key: 'hoTen', width: 26 },
          { header: 'Vị trí / Vai trò', key: 'vaiTro', width: 22 },
          { header: 'Ngày làm việc', key: 'ngayLam', width: 16, align: 'center' },
          { header: 'Ca làm việc', key: 'tenCa', width: 16 },
          { header: 'Khung giờ', key: 'gioCa', width: 18, align: 'center' },
          { header: 'Giờ vào', key: 'gioVao', width: 14, align: 'center' },
          { header: 'Giờ ra', key: 'gioRa', width: 14, align: 'center' },
          { header: 'Số giờ công', key: 'gioCong', width: 14, number: true },
          { header: 'Trạng thái', key: 'trangThai', width: 18, align: 'center' },
        ],
        rows: dsChamCong.value,
      },
    ],
  })
  toast.success('Đã xuất bảng chấm công Excel.')
}

const theoO = computed(() => gomLich(lich.value))
const dem = computed(() => demTheoCa(lich.value))
const viTriCa = computed(() => Object.fromEntries(cas.value.map((c, i) => [c.id, i])))
const caDangDung = computed(() => cas.value.filter((c) => c.hoatDong))

function tongCaCuaNv(idNv) {
  let cnt = 0
  for (const d of ngays.value) {
    cnt += (theoO.value[`${idNv}|${d.iso}`] || []).length
  }
  return cnt
}

function demThuNganTheoNgay(iso) {
  let cnt = 0
  for (const item of lich.value) {
    if (item.ngayLam === iso && (item.laThuNgan || /thu ngân/i.test(item.ghiChu || ''))) {
      cnt++
    }
  }
  return cnt
}

function layVietTat(ten) {
  const parts = String(ten || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'NV'
  return ((parts[0][0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase()
}

const MAU_AVATAR = ['#2563eb', '#16a34a', '#d97706', '#9333ea', '#0891b2', '#e11d48']
function layMauAvatar(ten) {
  let hash = 0
  for (let i = 0; i < (ten || '').length; i++) hash = ten.charCodeAt(i) + ((hash << 5) - hash)
  return MAU_AVATAR[Math.abs(hash) % MAU_AVATAR.length]
}

async function taiCa() {
  cas.value = await lichLamViecService.dsCa()
}
async function taiLich() {
  dangTai.value = true
  try {
    lich.value = await lichLamViecService.lich(thu2.value, congNgayIso(thu2.value, 6))
  } catch (e) {
    toast.error(e.message || 'Không tải được lịch làm việc.')
  } finally {
    dangTai.value = false
  }
}
onMounted(async () => {
  try {
    const [, nv] = await Promise.all([taiCa(), lichLamViecService.nhanVien()])
    nhanViens.value = nv
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu.')
  }
  await taiLich()
})

function doiTuan(soTuan) {
  thu2.value = soTuan === 0 ? thu2CuaTuan(todayIso()) : congNgayIso(thu2.value, soTuan * 7)
  taiLich()
}

/* ----- Đổi vị trí Thu ngân giữ két cho ca làm việc ----- */
async function toggleThuNganLich(l) {
  if (l.daQua) return
  try {
    const updated = await lichLamViecService.doiThuNgan(l.id)
    l.laThuNgan = updated.laThuNgan
    l.ghiChu = updated.ghiChu
    if (updated.laThuNgan) {
      lich.value.forEach((item) => {
        if (item.id !== l.id && item.ngayLam === l.ngayLam && item.idCa === l.idCa) {
          item.laThuNgan = false
          if (item.ghiChu) {
            item.ghiChu = item.ghiChu.replace('[Thu ngân]', '').replace('Thu ngân', '').trim()
          }
        }
      })
      toast.success(`Đã chỉ định ${l.hoTen} làm Thu ngân cho ${l.tenCa} ngày ${formatDate(l.ngayLam)}.`)
    } else {
      toast.success(`Đã chuyển ${l.hoTen} sang vị trí Bán hàng cho ${l.tenCa}.`)
    }
  } catch (e) {
    toast.error(e.message || 'Không thể đổi vị trí Thu ngân.')
  }
}

/* ----- Xếp lịch ----- */
const moXep = ref(false)
const chonSan = ref(null)
function moXepO(nv, ngay) {
  chonSan.value = { idNhanVien: nv.id, ngay }
  moXep.value = true
}
function moXepNhanh() {
  chonSan.value = null
  moXep.value = true
}

/* ----- Xóa ca ----- */
const xoa = ref(null)
const dangXoa = ref(false)
async function xacNhanXoa() {
  dangXoa.value = true
  try {
    await lichLamViecService.xoaLich(xoa.value.id)
    toast.success('Đã bỏ ca khỏi lịch.')
    xoa.value = null
    await taiLich()
  } catch (e) {
    toast.error(e.message || 'Không xóa được.')
  } finally {
    dangXoa.value = false
  }
}

/* ----- Excel xuất lịch tuần ----- */
const moNhapExcel = ref(false)
function xuatExcel() {
  if (!lich.value.length) {
    toast.error('Tuần này chưa có lịch nào để xuất.')
    return
  }
  xuatLichTuan(lich.value, thu2.value)
}

/* ----- Sao chép tuần trước ----- */
const moSaoChep = ref(false)
const dangSaoChep = ref(false)
async function saoChep() {
  dangSaoChep.value = true
  try {
    const kq = await lichLamViecService.saoChepTuan(congNgayIso(thu2.value, -7), thu2.value)
    moSaoChep.value = false
    toast.success(`Đã sao chép ${kq.daThem} ca${kq.boQua.length ? `, bỏ qua ${kq.boQua.length} ca (ngày đã qua / trùng / chồng giờ)` : ''}.`)
    await taiLich()
  } catch (e) {
    moSaoChep.value = false
    toast.error(e.message || 'Không sao chép được.')
  } finally {
    dangSaoChep.value = false
  }
}
</script>

<template>
  <div class="ad-page ll-page">
    <!-- Header chính với tiêu đề và Tab Navigation hiện đại -->
    <header class="ll-head-wrap mb-3">
      <div class="d-flex align-items-center justify-content-between flex-wrap gap-3">
        <div>
          <h1 class="ll-main-title m-0">Quản lý Lịch làm việc &amp; Chấm công</h1>
          <p class="ll-main-sub m-0 text-muted">
            Phân bổ ca trực riêng cho Thu ngân &amp; Nhân viên bán hàng, kiểm soát mở két tiền và theo dõi chấm công tính lương
          </p>
        </div>

        <!-- 3 Tab chuyển đổi hiện đại -->
        <nav class="ll-nav-tabs" role="tablist" aria-label="Quản lý lịch làm">
          <button
            type="button"
            class="ll-nav-tab"
            :class="{ 'is-active': tab === 'lich' }"
            role="tab"
            :aria-selected="tab === 'lich'"
            @click="tab = 'lich'"
          >
            Lịch làm việc
          </button>
          <button
            type="button"
            class="ll-nav-tab"
            :class="{ 'is-active': tab === 'chamCong' }"
            role="tab"
            :aria-selected="tab === 'chamCong'"
            @click="tab = 'chamCong'"
          >
            Chấm công &amp; Tính lương
          </button>
          <button
            type="button"
            class="ll-nav-tab"
            :class="{ 'is-active': tab === 'ca' }"
            role="tab"
            :aria-selected="tab === 'ca'"
            @click="tab = 'ca'"
          >
            Ca làm việc
          </button>
        </nav>
      </div>
    </header>

    <!-- TAB 3: Cấu hình ca làm việc -->
    <CaLamViecTab v-if="tab === 'ca'" :cas="cas" @doi="taiCa" />

    <!-- TAB 2: Chấm công & Tính lương -->
    <section v-else-if="tab === 'chamCong'" class="ad-card ll-card">
      <header class="ad-list-head ll-card-head">
        <div class="ll-week-ctrl">
          <button type="button" class="ad-icon-btn fw-bold" aria-label="Tuần trước" title="Tuần trước" @click="doiTuan(-1)">&lsaquo;</button>
          <div class="ll-week-title">
            <span class="badge bg-primary-subtle text-primary fw-bold me-1">BẢNG CÔNG</span>
            <b>{{ nhanTuan(thu2) }}</b>
          </div>
          <button type="button" class="ad-icon-btn fw-bold" aria-label="Tuần sau" title="Tuần sau" @click="doiTuan(1)">&rsaquo;</button>
          <button type="button" class="ad-btn ad-btn-sm" @click="doiTuan(0)">Tuần này</button>
        </div>

        <div class="d-flex align-items-center gap-2">
          <button type="button" class="ad-btn ad-btn-success text-white" @click="xuatChamCongExcel">
            Xuất bảng chấm công (Excel)
          </button>
        </div>
      </header>

      <!-- 4 thẻ KPI tóm tắt chấm công tuần -->
      <div class="row g-3 p-3 pt-0">
        <div class="col-12 col-sm-6 col-xl-3">
          <div class="ll-kpi-box is-blue">
            <div>
              <div class="ll-kpi-lbl">Tổng ca phân công</div>
              <div class="ll-kpi-val">{{ dsChamCong.length }} <small class="fw-normal text-muted fs-6">ca</small></div>
            </div>
          </div>
        </div>
        <div class="col-12 col-sm-6 col-xl-3">
          <div class="ll-kpi-box is-green">
            <div>
              <div class="ll-kpi-lbl">Ca đã hoàn thành</div>
              <div class="ll-kpi-val text-success">
                {{ dsChamCong.filter((x) => x.trangThai === 'Hoàn thành').length }}
                <small class="fw-normal text-muted fs-6">ca</small>
              </div>
            </div>
          </div>
        </div>
        <div class="col-12 col-sm-6 col-xl-3">
          <div class="ll-kpi-box is-purple">
            <div>
              <div class="ll-kpi-lbl">Tổng giờ công thực tế</div>
              <div class="ll-kpi-val text-primary">
                {{ dsChamCong.reduce((s, x) => s + x.gioCong, 0) }}
                <small class="fw-normal text-muted fs-6">giờ</small>
              </div>
            </div>
          </div>
        </div>
        <div class="col-12 col-sm-6 col-xl-3">
          <div class="ll-kpi-box is-amber">
            <div>
              <div class="ll-kpi-lbl">Tỷ lệ đi làm</div>
              <div class="ll-kpi-val">
                {{
                  dsChamCong.length
                    ? Math.round((dsChamCong.filter((x) => x.trangThai === 'Hoàn thành').length / dsChamCong.length) * 100)
                    : 100
                }}%
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Thanh công cụ tìm kiếm và lọc vai trò -->
      <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 px-3 pb-3 border-bottom">
        <div class="d-flex align-items-center gap-2">
          <span class="small fw-semibold text-secondary">Vị trí:</span>
          <div class="btn-group btn-group-sm">
            <button
              type="button"
              class="btn"
              :class="boLocVaiTro === 'tatCa' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="boLocVaiTro = 'tatCa'"
            >
              Tất cả ({{ nhanViens.length }})
            </button>
            <button
              type="button"
              class="btn"
              :class="boLocVaiTro === 'thuNgan' ? 'btn-success text-white' : 'btn-outline-success'"
              @click="boLocVaiTro = 'thuNgan'"
            >
              Thu ngân ({{ dsThuNgan.length }})
            </button>
            <button
              type="button"
              class="btn"
              :class="boLocVaiTro === 'banHang' ? 'btn-primary text-white' : 'btn-outline-primary'"
              @click="boLocVaiTro = 'banHang'"
            >
              Bán hàng ({{ dsBanHang.length }})
            </button>
          </div>
        </div>

        <div class="ll-search-wrap">
          <input
            v-model="timKiemChamCong"
            type="search"
            class="form-control form-control-sm ll-search-input"
            placeholder="Tìm theo tên, mã NV hoặc ca…"
            aria-label="Tìm kiếm chấm công"
          />
        </div>
      </div>

      <!-- Bảng dữ liệu chấm công -->
      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table ll-attendance-table">
            <thead>
              <tr>
                <th style="width: 50px" class="text-center">STT</th>
                <th>Nhân viên</th>
                <th>Vai trò</th>
                <th class="text-center">Ngày làm</th>
                <th>Ca làm việc</th>
                <th class="text-center">Khung giờ ca</th>
                <th class="text-center">Giờ vào (Check-in)</th>
                <th class="text-center">Giờ ra (Check-out)</th>
                <th class="text-center">Giờ công</th>
                <th class="text-center">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!dsChamCongHienThi.length">
                <td colspan="10">
                  <div class="ad-empty py-4">Chưa có dữ liệu chấm công phù hợp.</div>
                </td>
              </tr>
              <tr v-for="(cc, idx) in dsChamCongHienThi" :key="cc.id">
                <td class="text-center text-muted">{{ idx + 1 }}</td>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <span class="ll-avatar-sm" :style="{ background: layMauAvatar(cc.hoTen) }">
                      {{ layVietTat(cc.hoTen) }}
                    </span>
                    <div>
                      <div class="fw-semibold text-dark">{{ cc.hoTen }}</div>
                      <small class="text-muted">{{ cc.maNhanVien }}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <span
                    class="badge"
                    :class="cc.vaiTro === 'Thu ngân' ? 'bg-success-subtle text-success' : 'bg-primary-subtle text-primary'"
                  >
                    {{ cc.vaiTro }}
                  </span>
                </td>
                <td class="text-center">{{ formatDate(cc.ngayLam) }}</td>
                <td class="fw-semibold">{{ cc.tenCa }}</td>
                <td class="text-center text-muted">{{ cc.gioCa }}</td>
                <td class="text-center">
                  <span v-if="cc.gioVao !== '—'" class="fw-semibold text-success">{{ cc.gioVao }}</span>
                  <span v-else class="text-muted">—</span>
                </td>
                <td class="text-center">
                  <span v-if="cc.gioRa !== '—'" class="fw-semibold text-primary">{{ cc.gioRa }}</span>
                  <span v-else class="text-muted">—</span>
                </td>
                <td class="text-center fw-bold fs-6">{{ cc.gioCong ? `${cc.gioCong}h` : '—' }}</td>
                <td class="text-center">
                  <span
                    class="badge"
                    :class="
                      cc.trangThai === 'Hoàn thành'
                        ? 'bg-success'
                        : cc.trangThai === 'Đang trong ca'
                          ? 'bg-warning text-dark'
                          : 'bg-secondary'
                    "
                  >
                    {{ cc.trangThai }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- TAB 1: Lịch làm việc theo tuần -->
    <section v-else class="ad-card ll-card">
      <header class="ad-list-head ll-card-head">
        <!-- Chuyển tuần -->
        <div class="ll-week-ctrl">
          <button type="button" class="ad-icon-btn fw-bold" aria-label="Tuần trước" title="Tuần trước" @click="doiTuan(-1)">&lsaquo;</button>
          <div class="ll-week-title">
            <span class="badge bg-primary-subtle text-primary fw-bold me-1">LỊCH TUẦN</span>
            <b>{{ nhanTuan(thu2) }}</b>
          </div>
          <button type="button" class="ad-icon-btn fw-bold" aria-label="Tuần sau" title="Tuần sau" @click="doiTuan(1)">&rsaquo;</button>
          <button type="button" class="ad-btn ad-btn-sm" @click="doiTuan(0)">Tuần này</button>
        </div>

        <!-- Các thao tác lịch làm -->
        <div class="d-flex flex-wrap align-items-center gap-2">
          <button type="button" class="ad-btn" data-testid="ll-xuat-excel" @click="xuatExcel">
            Xuất Excel
          </button>
          <button type="button" class="ad-btn" data-testid="ll-nhap-excel" @click="moNhapExcel = true">
            Nhập Excel
          </button>
          <button type="button" class="ad-btn" @click="moSaoChep = true">
            Sao chép tuần trước
          </button>
          <button
            type="button"
            class="ad-btn ad-btn-primary"
            :disabled="!caDangDung.length"
            @click="moXepNhanh"
          >
            + Xếp lịch nhanh
          </button>
        </div>
      </header>

      <!-- Phân loại nhân viên + Tìm kiếm -->
      <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 px-3 pt-3 pb-2 border-bottom">
        <div class="d-flex align-items-center gap-2">
          <span class="small fw-semibold text-secondary">Phân loại:</span>
          <div class="btn-group btn-group-sm">
            <button
              type="button"
              class="btn"
              :class="boLocVaiTro === 'tatCa' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="boLocVaiTro = 'tatCa'"
            >
              Tất cả ({{ nhanViens.length }})
            </button>
            <button
              type="button"
              class="btn"
              :class="boLocVaiTro === 'nhanVien' ? 'btn-primary text-white' : 'btn-outline-primary'"
              @click="boLocVaiTro = 'nhanVien'"
            >
              Nhân viên ({{ dsNhanVien.length }})
            </button>
            <button
              v-if="dsQuanLy.length"
              type="button"
              class="btn"
              :class="boLocVaiTro === 'quanLy' ? 'btn-secondary text-white' : 'btn-outline-secondary'"
              @click="boLocVaiTro = 'quanLy'"
            >
              Quản lý ({{ dsQuanLy.length }})
            </button>
          </div>
        </div>

        <div class="ll-search-wrap">
          <input
            v-model="timKiemNv"
            type="search"
            class="form-control form-control-sm ll-search-input"
            placeholder="Tìm theo tên hoặc mã NV…"
            aria-label="Tìm kiếm nhân viên"
          />
        </div>
      </div>

      <!-- Chú thích ca & quy định ca thu ngân -->
      <div class="ll-legend-row">
        <div class="d-flex align-items-center flex-wrap gap-2">
          <span class="small fw-semibold text-muted">Ca trực:</span>
          <span
            v-for="c in cas"
            :key="c.id"
            class="ll-chip"
            :class="{ 'is-tat': !c.hoatDong }"
            :style="mauCa(viTriCa[c.id])"
          >
            {{ c.tenCa }} ({{ c.gioBatDau }} - {{ c.gioKetThuc }})
          </span>
        </div>
        <div class="ll-rule-hint">
          <span>Quy định: Mỗi ca bán hàng bắt buộc có ít nhất <b>1 Thu ngân</b> mở ca để kiểm soát két tiền.</span>
        </div>
      </div>

      <!-- Bảng ma trận lịch làm việc tuần -->
      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table ll-matrix-table">
            <thead>
              <tr>
                <th class="ll-col-nv">Nhân viên &amp; Vị trí</th>
                <th
                  v-for="d in ngays"
                  :key="d.iso"
                  class="ll-col-day"
                  :class="{ 'is-today': d.iso === homNay, 'is-past': d.iso < homNay }"
                >
                  <div class="ll-day-head">
                    <span class="ll-day-thu">{{ d.thu }}</span>
                    <span class="ll-day-num">{{ d.ngay }}</span>
                    <span v-if="d.iso === homNay" class="ll-today-pill">Hôm nay</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="dangTai">
                <td :colspan="8">
                  <div class="ad-empty py-4">Đang tải số liệu lịch làm việc…</div>
                </td>
              </tr>
              <tr v-else-if="!nhanViensHienThi.length">
                <td :colspan="8">
                  <div class="ad-empty py-4">Không tìm thấy nhân viên phù hợp.</div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="nv in nhanViensHienThi" :key="nv.id" class="ll-row">
                  <!-- Cột nhân viên (Sticky) -->
                  <td class="ll-col-nv">
                    <div class="d-flex align-items-center gap-2">
                      <span class="ll-avatar" :style="{ background: layMauAvatar(nv.hoTen) }">
                        {{ layVietTat(nv.hoTen) }}
                      </span>
                      <div class="ll-nv-meta">
                        <div class="fw-semibold text-dark">{{ nv.hoTen }}</div>
                        <div class="d-flex align-items-center gap-1 mt-0.5">
                          <small class="text-muted">{{ nv.maNhanVien }}</small>
                          <span
                            class="badge"
                            :class="laQuanLy(nv) ? 'bg-warning-subtle text-warning-emphasis' : 'bg-secondary-subtle text-secondary'"
                            style="font-size: 0.68rem"
                          >
                            {{ laQuanLy(nv) ? 'Quản lý' : 'Nhân viên' }}
                          </span>
                        </div>
                        <span class="ll-nv-count">{{ tongCaCuaNv(nv.id) }} ca/tuần</span>
                      </div>
                    </div>
                  </td>

                  <!-- 7 Cột Ngày làm việc -->
                  <td
                    v-for="d in ngays"
                    :key="d.iso"
                    class="ll-cell"
                    :class="{ 'is-today': d.iso === homNay, 'is-past': d.iso < homNay }"
                  >
                    <!-- Danh sách các ca đã xếp -->
                    <div
                      v-for="l in theoO[`${nv.id}|${d.iso}`] || []"
                      :key="l.id"
                      class="ll-shift-card"
                      :class="{ 'is-cashier': l.laThuNgan || /thu ngân/i.test(l.ghiChu || '') }"
                      :style="mauCa(viTriCa[l.idCa])"
                      :title="l.ghiChu || `${l.tenCa}: ${l.gioBatDau} - ${l.gioKetThuc}`"
                    >
                      <div class="ll-shift-info flex-fill">
                        <span class="ll-shift-name d-flex align-items-center justify-content-between gap-1">
                          <span>{{ l.tenCa }}</span>
                          <span
                            v-if="l.laThuNgan || /thu ngân/i.test(l.ghiChu || '')"
                            class="ll-badge-cashier"
                            title="Thu ngân giữ két trong ca này (bấm để đổi sang Bán hàng)"
                            @click.stop="toggleThuNganLich(l)"
                          >
                            Thu ngân
                          </span>
                          <button
                            v-else-if="!l.daQua"
                            type="button"
                            class="ll-btn-make-cashier"
                            title="Chỉ định làm Thu ngân giữ két cho ca này"
                            @click.stop="toggleThuNganLich(l)"
                          >
                            Giao két
                          </button>
                        </span>
                        <small class="ll-shift-time">{{ l.gioBatDau }} - {{ l.gioKetThuc }}</small>
                      </div>
                      <button
                        v-if="!l.daQua"
                        type="button"
                        class="ll-shift-remove ms-1"
                        :aria-label="`Bỏ ${l.tenCa} của ${nv.hoTen} ngày ${formatDate(d.iso)}`"
                        title="Bỏ ca này"
                        @click="xoa = l"
                      >
                        &times;
                      </button>
                    </div>

                    <!-- Nút xếp thêm ca nhanh vào ô này -->
                    <button
                      v-if="d.iso >= homNay && caDangDung.length"
                      type="button"
                      class="ll-add-btn"
                      :aria-label="`Xếp ca cho ${nv.hoTen} ngày ${formatDate(d.iso)}`"
                      title="Xếp thêm ca"
                      @click="moXepO(nv, d.iso)"
                    >
                      +
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>

            <!-- Tổng kết nhân sự theo ca mỗi ngày -->
            <tfoot v-if="!dangTai && nhanViensHienThi.length">
              <tr class="ll-footer-row">
                <th class="ll-col-nv">
                  <div class="fw-bold">Số lượng nhân sự / ca</div>
                  <small class="text-muted">Tổng nhân sự trực theo ngày</small>
                </th>
                <td
                  v-for="d in ngays"
                  :key="d.iso"
                  class="ll-summary-cell"
                  :class="{ 'is-today': d.iso === homNay }"
                >
                  <div v-for="c in caDangDung" :key="c.id" class="ll-sum-line" :class="{ 'is-zero': !(dem[d.iso] || {})[c.id] }">
                    <span class="text-muted">{{ c.tenCa }}:</span>
                    <b class="ms-1">{{ (dem[d.iso] || {})[c.id] || 0 }}</b>
                  </div>
                  <!-- Cảnh báo thiếu thu ngân -->
                  <div v-if="d.iso >= homNay && demThuNganTheoNgay(d.iso) === 0" class="ll-cashier-warn" title="Ngày này chưa có Thu ngân giữ két">
                    Chưa có Thu ngân
                  </div>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </section>

    <!-- Modal Xếp lịch -->
    <XepLichModal
      v-if="moXep"
      :nhan-viens="nhanViens"
      :cas="cas"
      :ngays="ngays"
      :chon-san="chonSan"
      @close="moXep = false"
      @xong="taiLich"
    />

    <!-- Modal Nhập Excel -->
    <LichImportModal
      v-if="moNhapExcel"
      :nhan-viens="nhanViens"
      :cas="cas"
      :thu2="thu2"
      :hom-nay="homNay"
      @close="moNhapExcel = false"
      @imported="taiLich"
    />

    <!-- Xác nhận xóa ca -->
    <ConfirmDialog
      v-if="xoa"
      title="Bỏ ca khỏi lịch"
      :message="`Bỏ ${xoa.tenCa} (${xoa.gioBatDau} - ${xoa.gioKetThuc}) của ${xoa.hoTen} ngày ${formatDate(xoa.ngayLam)}?`"
      confirm-text="Bỏ ca"
      variant="danger"
      :loading="dangXoa"
      @confirm="xacNhanXoa"
      @cancel="xoa = null"
    >
      <p class="mb-2">Bạn có chắc muốn bỏ ca làm việc này khỏi lịch phân công?</p>
      <div class="ll-hop-info">
        <div><span>Nhân viên:</span> <b>{{ xoa.hoTen }}</b></div>
        <div><span>Ca làm việc:</span> <b>{{ xoa.tenCa }} ({{ xoa.gioBatDau }} - {{ xoa.gioKetThuc }})</b></div>
        <div><span>Ngày làm:</span> <b>{{ formatDate(xoa.ngayLam) }}</b></div>
      </div>
    </ConfirmDialog>

    <!-- Xác nhận sao chép tuần trước -->
    <ConfirmDialog
      v-if="moSaoChep"
      title="Sao chép lịch tuần trước"
      :message="`Chép toàn bộ lịch của ${nhanTuan(congNgayIso(thu2, -7)).toLowerCase()} sang ${nhanTuan(thu2).toLowerCase()}? Ô của ngày đã qua, ca trùng hoặc chồng giờ sẽ được bỏ qua.`"
      confirm-text="Sao chép"
      :loading="dangSaoChep"
      @confirm="saoChep"
      @cancel="moSaoChep = false"
    >
      <p class="mb-2">Sao chép toàn bộ lịch làm việc từ tuần trước sang tuần đang xem:</p>
      <div class="ll-hop-info">
        <div><span>Tuần nguồn:</span> <b>{{ nhanTuan(congNgayIso(thu2, -7)) }}</b></div>
        <div><span>Tuần đích:</span> <b>{{ nhanTuan(thu2) }}</b></div>
      </div>
      <p class="ll-hop-luu-y">Lưu ý: Ô thuộc ngày đã qua, ca trùng hoặc bị chồng giờ sẽ tự động được bỏ qua.</p>
    </ConfirmDialog>
  </div>
</template>

<style scoped>
.ll-page {
  font-family: inherit;
}

/* Header & Tab navigation */
.ll-head-wrap {
  padding-bottom: 0.5rem;
}
.ll-main-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.01em;
}
.ll-main-sub {
  font-size: 0.85rem;
  margin-top: 0.2rem;
}
.ll-nav-tabs {
  display: inline-flex;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 0.25rem;
  gap: 0.25rem;
}
.ll-nav-tab {
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 0.86rem;
  font-weight: 600;
  padding: 0.45rem 0.95rem;
  border-radius: 8px;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.ll-nav-tab:hover {
  color: #1e293b;
}
.ll-nav-tab.is-active {
  background: #ffffff;
  color: #0977ec;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

/* Card layout */
.ll-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}
.ll-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

/* Week Navigator */
.ll-week-ctrl {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.ll-week-title {
  font-size: 0.95rem;
  color: #1e293b;
  padding: 0 0.25rem;
}

/* Search input */
.ll-search-wrap {
  min-width: 240px;
}
.ll-search-input {
  border-radius: 8px;
  font-size: 0.84rem;
}

/* KPI cards */
.ll-kpi-box {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.85rem 1.15rem;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #cbd5e1;
  transition: transform 0.15s ease;
}
.ll-kpi-box:hover {
  transform: translateY(-1px);
}
.ll-kpi-box.is-blue { border-left-color: #2563eb; }
.ll-kpi-box.is-green { border-left-color: #16a34a; }
.ll-kpi-box.is-purple { border-left-color: #9333ea; }
.ll-kpi-box.is-amber { border-left-color: #d97706; }
.ll-kpi-lbl {
  font-size: 0.78rem;
  color: #64748b;
  font-weight: 500;
}
.ll-kpi-val {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
}

/* Legend row */
.ll-legend-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.6rem;
  padding: 0.65rem 1.25rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.ll-chip {
  display: inline-flex;
  align-items: center;
  border: 1px solid;
  border-radius: 999px;
  padding: 0.15rem 0.65rem;
  font-weight: 600;
  font-size: 0.78rem;
  white-space: nowrap;
}
.ll-chip.is-tat {
  opacity: 0.45;
  text-decoration: line-through;
}
.ll-rule-hint {
  font-size: 0.8rem;
  color: #475569;
}

/* Matrix Table */
.ll-matrix-table th,
.ll-matrix-table td {
  vertical-align: top;
  min-width: 130px;
}
.ll-col-nv {
  min-width: 220px !important;
  position: sticky;
  left: 0;
  background: #ffffff;
  z-index: 2;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.02);
}
.ll-col-day {
  text-align: center;
  background: #f8fafc;
}
.ll-day-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
}
.ll-day-thu {
  font-weight: 700;
  font-size: 0.86rem;
  color: #1e293b;
}
.ll-day-num {
  font-size: 0.78rem;
  color: #64748b;
}
.ll-today-pill {
  font-size: 0.68rem;
  font-weight: 700;
  color: #0977ec;
  background: #e0f2fe;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
}
.ll-matrix-table td.is-today,
.ll-matrix-table th.is-today {
  background: #f0f7ff !important;
  border-left: 2px solid #bae6fd;
  border-right: 2px solid #bae6fd;
}
.ll-matrix-table td.is-past {
  background: #fafbfc;
}

/* Employee cell info */
.ll-avatar {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  color: #ffffff;
  font-weight: 700;
  font-size: 0.82rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ll-avatar-sm {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #ffffff;
  font-weight: 700;
  font-size: 0.72rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ll-nv-meta {
  line-height: 1.25;
}
.ll-nv-count {
  display: inline-block;
  font-size: 0.72rem;
  color: #64748b;
  margin-top: 0.2rem;
}

/* Shift card in matrix */
.ll-shift-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid;
  border-radius: 8px;
  padding: 0.35rem 0.5rem;
  margin-bottom: 0.35rem;
  transition: box-shadow 0.15s ease;
}
.ll-shift-card:hover {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}
.ll-shift-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.ll-shift-name {
  font-weight: 700;
  font-size: 0.82rem;
}
.ll-badge-cashier {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.12rem 0.35rem;
  border-radius: 4px;
  background: #047857;
  color: #ffffff;
  cursor: pointer;
  letter-spacing: 0.02em;
  transition: opacity 0.15s ease;
  user-select: none;
  white-space: nowrap;
}
.ll-badge-cashier:hover {
  opacity: 0.85;
}
.ll-btn-make-cashier {
  display: none;
  font-size: 0.6rem;
  font-weight: 600;
  padding: 0.08rem 0.28rem;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.07);
  color: #334155;
  border: 0;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}
.ll-shift-card:hover .ll-btn-make-cashier {
  display: inline-block;
}
.ll-btn-make-cashier:hover {
  background: #047857;
  color: #ffffff;
}
.ll-shift-time {
  font-size: 0.72rem;
  opacity: 0.85;
}
.ll-shift-remove {
  border: 0;
  background: transparent;
  color: inherit;
  opacity: 0.45;
  font-size: 0.75rem;
  padding: 0.15rem 0.25rem;
  border-radius: 4px;
  transition: all 0.15s ease;
}
.ll-shift-remove:hover {
  opacity: 1;
  background: rgba(220, 38, 38, 0.15);
  color: #dc2626;
}

/* Quick add button */
.ll-add-btn {
  width: 100%;
  border: 1px dashed #cbd5e1;
  background: transparent;
  border-radius: 6px;
  color: #94a3b8;
  padding: 0.2rem 0;
  font-size: 0.95rem;
  line-height: 1;
  transition: all 0.15s ease;
  display: block;
}
.ll-cell:hover .ll-add-btn {
  border-color: #0977ec;
  color: #0977ec;
  background: #f0f7ff;
}

/* Footer summary */
.ll-footer-row th,
.ll-footer-row td {
  background: #f8fafc;
  border-top: 2px solid #e2e8f0;
  padding: 0.75rem 0.85rem;
}
.ll-summary-cell {
  font-size: 0.78rem;
  line-height: 1.35;
}
.ll-sum-line.is-zero {
  opacity: 0.45;
}
.ll-cashier-warn {
  margin-top: 0.35rem;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 0.72rem;
  font-weight: 600;
}

/* Dialog info */
.ll-hop-info {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-size: 0.88rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}
.ll-hop-info span {
  color: #64748b;
  display: inline-block;
  min-width: 90px;
}
.ll-hop-luu-y {
  font-size: 0.82rem;
  color: #64748b;
  margin: 0;
}
</style>
