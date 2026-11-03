<script setup>
// Thống kê so sánh doanh thu theo tháng / quý / năm, đối chiếu các kỳ, biểu đồ tăng trưởng,
// xuất Excel và gửi email báo cáo. Số liệu tính ở backend: GET /api/thong-ke/so-sanh?kieu=THANG|QUY|NAM&ky=...
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../../services/api'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { exportExcel } from '../../utils/exportExcel'
import { formatMoney, todayIso } from '../../utils/format'
import { lamTronTran, nhanTienGon } from '../../utils/thongKe'

const toast = useToast()
const auth = useAuth()
const homNay = todayIso()
const namNay = Number(homNay.slice(0, 4))
const thangSoHienTai = Number(homNay.slice(5, 7))

const KIEU = [
  { v: 'THANG', t: 'Theo tháng' },
  { v: 'QUY', t: 'Theo quý' },
  { v: 'NAM', t: 'Theo năm' },
]
const kieu = ref('THANG')
const thangHienTai = homNay.slice(0, 7)
const quyHienTai = Math.floor((thangSoHienTai - 1) / 3) + 1
const thang = ref(thangHienTai)
const namQuy = ref(namNay)
const quy = ref(quyHienTai)
const nam = ref(namNay)
// Cho phép chọn năm từ 2020 đến năm hiện tại (để dễ dàng so sánh với 2024, 2023...)
const dsNam = Array.from({ length: Math.max(7, namNay - 2019) }, (_, i) => namNay - i)

/* ----- Danh sách các hộp chọn kỳ đối chiếu (1, 2, 3... + Thêm kỳ) xếp theo chiều thời gian từ cũ -> mới ----- */
function taoDanhSachKyMacDinh(loai, neoThang = thangSoHienTai, neoNam = namNay) {
  if (loai === 'THANG') {
    const tCuNhat = neoThang - 2 >= 1 ? { nam: neoNam, thang: neoThang - 2, quy: quyHienTai } : { nam: neoNam - 1, thang: 12 + (neoThang - 2), quy: 4 }
    const tGiua = neoThang - 1 >= 1 ? { nam: neoNam, thang: neoThang - 1, quy: quyHienTai } : { nam: neoNam - 1, thang: 12, quy: 4 }
    const tMoiNhat = { nam: neoNam, thang: neoThang, quy: quyHienTai }
    return [tCuNhat, tGiua, tMoiNhat]
  }
  if (loai === 'QUY') {
    const qCuNhat = quyHienTai - 2 >= 1 ? { nam: namNay, thang: thangSoHienTai, quy: quyHienTai - 2 } : { nam: namNay - 1, thang: 9, quy: 4 + (quyHienTai - 2) }
    const qGiua = quyHienTai - 1 >= 1 ? { nam: namNay, thang: thangSoHienTai, quy: quyHienTai - 1 } : { nam: namNay - 1, thang: 12, quy: 4 }
    const qMoiNhat = { nam: namNay, thang: thangSoHienTai, quy: quyHienTai }
    return [qCuNhat, qGiua, qMoiNhat]
  }
  return [
    { nam: namNay - 2, thang: thangSoHienTai, quy: quyHienTai },
    { nam: namNay - 1, thang: thangSoHienTai, quy: quyHienTai },
    { nam: namNay, thang: thangSoHienTai, quy: quyHienTai },
  ]
}

const dsKyChon = ref(taoDanhSachKyMacDinh('THANG'))

function maCuaMucKy(item, loai = kieu.value) {
  if (loai === 'THANG') return `${item.nam}-${String(item.thang).padStart(2, '0')}`
  if (loai === 'QUY') return `${item.nam}-Q${item.quy}`
  return String(item.nam)
}

function nhanCuaMucKy(item, loai = kieu.value) {
  if (loai === 'THANG') return `Tháng ${item.thang}/${item.nam}`
  if (loai === 'QUY') return `Quý ${item.quy}/${item.nam}`
  return `Năm ${item.nam}`
}

function themKy() {
  if (dsKyChon.value.length >= 6) return
  const daCo = new Set(dsKyChon.value.map((x) => maCuaMucKy(x)))
  if (kieu.value === 'THANG') {
    for (const y of dsNam) {
      for (let m = 12; m >= 1; m--) {
        if (y === namNay && m > thangSoHienTai) continue
        const ungVien = { nam: y, thang: m, quy: Math.floor((m - 1) / 3) + 1 }
        if (!daCo.has(maCuaMucKy(ungVien))) {
          dsKyChon.value.push(ungVien)
          return
        }
      }
    }
  } else if (kieu.value === 'QUY') {
    for (const y of dsNam) {
      for (let q = 4; q >= 1; q--) {
        if (y === namNay && q > quyHienTai) continue
        const ungVien = { nam: y, thang: q * 3, quy: q }
        if (!daCo.has(maCuaMucKy(ungVien))) {
          dsKyChon.value.push(ungVien)
          return
        }
      }
    }
  } else {
    for (const y of dsNam) {
      const ungVien = { nam: y, thang: 1, quy: 1 }
      if (!daCo.has(maCuaMucKy(ungVien))) {
        dsKyChon.value.push(ungVien)
        return
      }
    }
  }
}

function xoaKy(idx) {
  if (dsKyChon.value.length <= 2) return
  dsKyChon.value.splice(idx, 1)
}

// Bộ nhớ đệm lưu chỉ số đầy đủ của từng kỳ người dùng chọn (kể cả kỳ thuộc năm 2024, 2023...)
const cacheChiSoKy = reactive({})

function luuVaoCache(loai, chiSoObj) {
  if (!chiSoObj?.ky?.ma) return
  cacheChiSoKy[`${loai}:${chiSoObj.ky.ma}`] = chiSoObj
}

const canhBaoKy = computed(() => {
  if (kieu.value === 'THANG') {
    const t = String(thang.value || '').trim()
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(t)) return 'Tháng chọn không hợp lệ (cần định dạng YYYY-MM).'
    if (t > thangHienTai) return `Tháng đang chọn (${t}) vượt quá tháng hiện tại (${thangHienTai}).`
    if (t < '2000-01') return 'Vui lòng chọn tháng từ năm 2000 trở lại đây.'
    for (let i = 0; i < dsKyChon.value.length; i++) {
      const item = dsKyChon.value[i]
      const ma = maCuaMucKy(item, 'THANG')
      if (ma > thangHienTai) {
        return `Kỳ ${i + 1} (${nhanCuaMucKy(item, 'THANG')}) vượt quá tháng hiện tại (${thangHienTai}), chưa có dữ liệu phát sinh.`
      }
      if (item.nam < 2000) {
        return `Kỳ ${i + 1} (${nhanCuaMucKy(item, 'THANG')}) quá xa trong quá khứ (vui lòng chọn từ năm 2000 trở lại đây).`
      }
    }
  } else if (kieu.value === 'QUY') {
    if (namQuy.value === namNay && quy.value > quyHienTai) {
      return `Quý ${quy.value}/${namQuy.value} chưa diễn ra (hiện tại đang ở Quý ${quyHienTai}/${namNay}).`
    }
    for (let i = 0; i < dsKyChon.value.length; i++) {
      const item = dsKyChon.value[i]
      if (item.nam > namNay || (item.nam === namNay && item.quy > quyHienTai)) {
        return `Kỳ ${i + 1} (${nhanCuaMucKy(item, 'QUY')}) chưa diễn ra (hiện tại đang ở Quý ${quyHienTai}/${namNay}).`
      }
    }
  } else if (kieu.value === 'NAM') {
    for (let i = 0; i < dsKyChon.value.length; i++) {
      const item = dsKyChon.value[i]
      if (item.nam > namNay) {
        return `Kỳ ${i + 1} (Năm ${item.nam}) vượt quá năm hiện tại (${namNay}).`
      }
    }
  }
  const daGap = new Set()
  for (const item of dsKyChon.value) {
    const ma = maCuaMucKy(item)
    if (daGap.has(ma)) {
      return `Đang chọn trùng ${nhanCuaMucKy(item)} ở nhiều ô kỳ. Hệ thống đã tự động gộp các kỳ trùng khi đối chiếu.`
    }
    daGap.add(ma)
  }
  return ''
})

function datVeKyHienTai() {
  thang.value = thangHienTai
  namQuy.value = namNay
  quy.value = quyHienTai
  nam.value = namNay
  dsKyChon.value = taoDanhSachKyMacDinh(kieu.value)
}

const ky = computed(() => (kieu.value === 'THANG' ? thang.value : kieu.value === 'QUY' ? `${namQuy.value}-Q${quy.value}` : String(nam.value)))
const d = ref(null)
const dangTai = ref(false)
const loi = ref('')
let lan = 0
let daKhoiTaoKy = false

async function napDayDuChiSoCacKyChon() {
  if (!d.value) return
  const loai = kieu.value
  for (const item of dsKyChon.value) {
    const ma = maCuaMucKy(item, loai)
    const khoa = `${loai}:${ma}`
    if (cacheChiSoKy[khoa]) continue
    const trongXuHuong = (d.value?.xuHuong || []).find((x) => x.ma === ma)
    if (trongXuHuong && Number(trongXuHuong.doanhThu) === 0 && Number(trongXuHuong.soDon) === 0) {
      cacheChiSoKy[khoa] = {
        ky: { ma, nhan: nhanCuaMucKy(item, loai) },
        doanhThuThucTe: 0,
        doanhThuDuKien: 0,
        tongGiamGia: 0,
        soDon: 0,
        soHoanThanh: 0,
        soHuy: 0,
        giaTriTrungBinh: 0,
        soSanPhamBan: 0,
        soKhachMua: 0,
      }
      continue
    }
    try {
      const res = await api.get('/thong-ke/so-sanh', { params: { kieu: loai, ky: ma } })
      luuVaoCache(loai, res.data?.hienTai)
      luuVaoCache(loai, res.data?.kyTruoc)
      luuVaoCache(loai, res.data?.cungKyNamTruoc)
    } catch {
      // Bỏ qua nếu kỳ chưa hợp lệ
    }
  }
}

async function tai() {
  if (!ky.value) return
  if (kieu.value === 'THANG' && (!/^\d{4}-(0[1-9]|1[0-2])$/.test(thang.value) || thang.value < '2000-01')) return
  const l = ++lan
  dangTai.value = true
  loi.value = ''
  try {
    const res = await api.get('/thong-ke/so-sanh', { params: { kieu: kieu.value, ky: ky.value } })
    if (l === lan) {
      d.value = res.data
      luuVaoCache(kieu.value, res.data?.hienTai)
      luuVaoCache(kieu.value, res.data?.kyTruoc)
      luuVaoCache(kieu.value, res.data?.cungKyNamTruoc)
      // Khi tải lần đầu theo tháng, nếu tháng hiện tại chưa có doanh thu mà trong năm có tháng có doanh thu thì neo mặc định vào tháng đó
      if (!daKhoiTaoKy && kieu.value === 'THANG' && Array.isArray(res.data?.xuHuong)) {
        daKhoiTaoKy = true
        const coDt = res.data.xuHuong.filter((x) => (Number(x.doanhThu) || 0) > 0 && /^\d{4}-\d{2}$/.test(x.ma || ''))
        if (coDt.length > 0 && (Number(res.data?.hienTai?.doanhThuThucTe) || 0) === 0) {
          const cuoi = coDt[coDt.length - 1]
          const y = Number(cuoi.ma.slice(0, 4))
          const m = Number(cuoi.ma.slice(5, 7))
          if (y && m) dsKyChon.value = taoDanhSachKyMacDinh('THANG', m, y)
        }
      }
      await napDayDuChiSoCacKyChon()
    }
  } catch (e) {
    if (l === lan) loi.value = e.message || 'Không tải được số liệu so sánh.'
  } finally {
    if (l === lan) dangTai.value = false
  }
}

onMounted(tai)

watch(kieu, (loaiMoi) => {
  dsKyChon.value = taoDanhSachKyMacDinh(loaiMoi)
  tai()
})

watch(ky, tai)

// Khi người dùng đổi bất kỳ hộp chọn kỳ (1, 2, 3...) sang kỳ khác (ví dụ Tháng 9/2024), tự động tải số liệu thực của kỳ đó từ backend
function thayDoiHopKy(item) {
  const ma = maCuaMucKy(item)
  if (kieu.value === 'THANG' && ma <= thangHienTai) {
    thang.value = ma
  } else if (kieu.value === 'QUY') {
    namQuy.value = item.nam
    quy.value = item.quy
  } else if (kieu.value === 'NAM') {
    nam.value = item.nam
  }
}

watch(
  dsKyChon,
  () => {
    napDayDuChiSoCacKyChon()
  },
  { deep: true },
)

function chonKyTuXuHuong(c) {
  if (!c?.ma) return
  if (kieu.value === 'THANG' && /^\d{4}-\d{2}$/.test(c.ma) && c.ma <= thangHienTai) {
    const y = Number(c.ma.slice(0, 4))
    const m = Number(c.ma.slice(5, 7))
    if (dsKyChon.value.length >= 2) {
      dsKyChon.value[1] = { nam: y, thang: m, quy: Math.floor((m - 1) / 3) + 1 }
    }
    thang.value = c.ma
  } else if (kieu.value === 'QUY' && /^(\d{4})-Q([1-4])$/.test(c.ma)) {
    const m = c.ma.match(/^(\d{4})-Q([1-4])$/)
    const y = Number(m[1])
    const q = Number(m[2])
    if (y < namNay || (y === namNay && q <= quyHienTai)) {
      if (dsKyChon.value.length >= 2) {
        dsKyChon.value[1] = { nam: y, thang: q * 3, quy: q }
      }
      namQuy.value = y
      quy.value = q
    }
  } else if (kieu.value === 'NAM' && /^\d{4}$/.test(c.ma)) {
    const y = Number(c.ma)
    if (y <= namNay) {
      if (dsKyChon.value.length >= 2) {
        dsKyChon.value[1] = { nam: y, thang: 1, quy: 1 }
      }
      nam.value = y
    }
  }
}

const CHI_SO = [
  { k: 'doanhThuThucTe', ten: 'Doanh thu thực tế', tenNgan: 'Doanh thu thực tế', tien: true, icon: 'bi-cash-coin', mauIco: '#2563eb', nenIco: '#eff6ff', noiBat: true },
  { k: 'doanhThuDuKien', ten: 'Doanh thu dự kiến', tenNgan: 'Doanh thu dự kiến', tien: true, icon: 'bi-graph-up-arrow', mauIco: '#4f46e5', nenIco: '#eef2ff' },
  { k: 'soDon', ten: 'Tổng đơn', tenNgan: 'Tổng đơn', icon: 'bi-receipt', mauIco: '#0284c7', nenIco: '#f0f9ff' },
  { k: 'soHoanThanh', ten: 'Đơn hoàn thành', tenNgan: 'Đơn hoàn thành', icon: 'bi-check2-circle', mauIco: '#059669', nenIco: '#ecfdf5' },
  { k: 'soHuy', ten: 'Đơn hủy', tenNgan: 'Đơn hủy', xauKhiTang: true, icon: 'bi-x-circle', mauIco: '#e11d48', nenIco: '#fff1f2' },
  { k: 'giaTriTrungBinh', ten: 'Giá trị TB / đơn', tenNgan: 'Giá trị TB / đơn', tien: true, icon: 'bi-calculator', mauIco: '#7c3aed', nenIco: '#f5f3ff' },
  { k: 'soSanPhamBan', ten: 'Sản phẩm bán ra', tenNgan: 'Sản phẩm bán ra', icon: 'bi-box-seam', mauIco: '#d97706', nenIco: '#fffbeb' },
  { k: 'soKhachMua', ten: 'Khách hàng mua', tenNgan: 'Khách hàng mua', icon: 'bi-people', mauIco: '#0d9488', nenIco: '#f0fdfa' },
]
const CHI_SO_TIEN = [CHI_SO[0], CHI_SO[1], CHI_SO[5]]
const CHI_SO_SAN_LUONG = [CHI_SO[2], CHI_SO[3], CHI_SO[6], CHI_SO[7]]

// Trạng thái thông minh cho 3 biểu đồ & bảng đối chiếu
const cheDoBieuDoKy = ref('TIEN') // 'TIEN' | 'SL'
const hienDuongXuHuong = ref(true)
const chiHienCoDt = ref(false)
const hoverCot3Ky = ref(null)
const hoverXuHuong = ref(null)
const hoverChiTiet = ref(null)

const giaTri = (c, v) => (c.tien ? formatMoney(v ?? 0) : String(v ?? 0))

function tinhPhanTram(a, b) {
  const va = Number(a) || 0
  const vb = Number(b) || 0
  if (vb === 0) return va === 0 ? 0 : null
  return Math.round(((va - vb) * 1000) / vb) / 10
}

function moTaTang(p, xauKhiTang = false, valNay = null, valGoc = null) {
  if (p === null || p === undefined) {
    if (valNay !== null && valGoc !== null && Number(valGoc) === 0 && Number(valNay) > 0) {
      const so = Number(valNay)
      const chenh = so >= 1000 ? nhanTienGon(so) : String(so)
      const nhan = `▲ 100% (+${chenh})`
      return {
        text: nhan,
        textNgan: nhan,
        lop: xauKhiTang ? 'ss-xau' : 'ss-tot',
      }
    }
    return { text: 'Kỳ so sánh chưa có', textNgan: '0% (0)', lop: 'text-muted' }
  }
  if (Math.abs(p) < 0.05) return { text: 'Không đổi', textNgan: '0% (0)', lop: 'text-muted' }
  const tang = p > 0
  const s = `${tang ? '▲' : '▼'} ${String(Math.abs(p)).replace('.', ',')}%`
  if (valNay !== null && valGoc !== null) {
    const hieu = Number(valNay) - Number(valGoc)
    const absHieu = Math.abs(hieu)
    const strHieu = `${hieu >= 0 ? '+' : '-'}${absHieu >= 1000 ? nhanTienGon(absHieu) : String(absHieu)}`
    return { text: s, textNgan: `${s} (${strHieu})`, lop: tang !== xauKhiTang ? 'ss-tot' : 'ss-xau' }
  }
  return { text: s, textNgan: s, lop: tang !== xauKhiTang ? 'ss-tot' : 'ss-xau' }
}

// Lấy bộ chỉ số đầy đủ cho một mã kỳ bất kỳ
function layChiSoDayDuCuaKy(ma, nhanMacDinh) {
  const v = d.value
  const khoa = `${kieu.value}:${ma}`
  if (cacheChiSoKy[khoa]) {
    return {
      ...cacheChiSoKy[khoa],
      ky: { ma, nhan: cacheChiSoKy[khoa].ky?.nhan || nhanMacDinh },
    }
  }
  if (v?.hienTai?.ky?.ma === ma) return v.hienTai
  if (v?.kyTruoc?.ky?.ma === ma) return v.kyTruoc
  if (v?.cungKyNamTruoc?.ky?.ma === ma) return v.cungKyNamTruoc
  const trongXu = (v?.xuHuong || []).find((x) => x.ma === ma)
  if (trongXu) {
    const dt = Number(trongXu.doanhThu) || 0
    const sd = Number(trongXu.soDon) || 0
    return {
      ky: { ma, nhan: nhanMacDinh },
      doanhThuThucTe: dt,
      doanhThuDuKien: dt,
      tongGiamGia: 0,
      soDon: sd,
      soHoanThanh: sd,
      soHuy: 0,
      giaTriTrungBinh: sd > 0 ? Math.round(dt / sd) : 0,
      soSanPhamBan: 0,
      soKhachMua: 0,
    }
  }
  return {
    ky: { ma, nhan: nhanMacDinh },
    doanhThuThucTe: 0,
    doanhThuDuKien: 0,
    tongGiamGia: 0,
    soDon: 0,
    soHoanThanh: 0,
    soHuy: 0,
    giaTriTrungBinh: 0,
    soSanPhamBan: 0,
    soKhachMua: 0,
  }
}

const PALETTE_KY = [
  { mau: '#2563eb', gradTu: '#3b82f6', gradDen: '#1d4ed8', nen: '#eff6ff', vien: '#bfdbfe', nenHuyHieu: '#dbeafe', chuHuyHieu: '#1d4ed8' },
  { mau: '#0891b2', gradTu: '#06b6d4', gradDen: '#0e7490', nen: '#ecfeff', vien: '#a5f3fc', nenHuyHieu: '#cffafe', chuHuyHieu: '#0e7490' },
  { mau: '#7c3aed', gradTu: '#8b5cf6', gradDen: '#6d28d9', nen: '#f5f3ff', vien: '#ddd6fe', nenHuyHieu: '#ede9fe', chuHuyHieu: '#6d28d9' },
  { mau: '#d97706', gradTu: '#f59e0b', gradDen: '#b45309', nen: '#fffbeb', vien: '#fde68a', nenHuyHieu: '#fef3c7', chuHuyHieu: '#b45309' },
  { mau: '#059669', gradTu: '#10b981', gradDen: '#047857', nen: '#ecfdf5', vien: '#a7f3d0', nenHuyHieu: '#d1fae5', chuHuyHieu: '#047857' },
  { mau: '#e11d48', gradTu: '#f43f5e', gradDen: '#be123c', nen: '#fff1f2', vien: '#fecdd3', nenHuyHieu: '#ffe4e6', chuHuyHieu: '#be123c' },
]

function layBangMauKy(idx) {
  return PALETTE_KY[idx % PALETTE_KY.length]
}

// Danh sách các kỳ đã chọn trong các hộp 1, 2, 3... (luôn sắp xếp từ kỳ cũ -> kỳ mới để tính % tăng trưởng đúng chiều thời gian)
const cacKyDoiChieu = computed(() => {
  const ketQua = []
  const daCo = new Set()
  dsKyChon.value.forEach((item, idx) => {
    const ma = maCuaMucKy(item)
    if (daCo.has(ma)) return
    daCo.add(ma)
    const nhan = nhanCuaMucKy(item)
    const cs = layChiSoDayDuCuaKy(ma, nhan)
    const bangMau = layBangMauKy(idx)
    ketQua.push({
      ...cs,
      stt: idx + 1,
      ma: cs.ky?.ma || ma,
      nhan: cs.ky?.nhan || nhan,
      bangMau,
    })
  })
  const sapXep = [...ketQua].sort((a, b) => String(a.ma || '').localeCompare(String(b.ma || '')))
  return sapXep.map((cs, idx) => {
    const truoc = idx > 0 ? sapXep[idx - 1] : null
    const pctDt = truoc ? tinhPhanTram(cs.doanhThuThucTe, truoc.doanhThuThucTe) : null
    return {
      ...cs,
      soVoiNhan: truoc ? truoc.nhan : null,
      pctDoanhThu: pctDt,
      moTaDt: truoc ? moTaTang(pctDt, false, cs.doanhThuThucTe, truoc.doanhThuThucTe) : null,
    }
  })
})

/* ----- Biểu đồ cột so sánh các kỳ đã chọn (1, 2, 3...) ----- */
const W_3KY = 620
const H_3KY = 295
const PAD_3KY = { l: 64, r: 20, t: 38, b: 42 }

function tinhTranBieuDo(max, laTien = true) {
  if (!(max > 0)) return laTien ? 4_000_000 : 10
  const mucTieu = max * 1.18
  const mu = 10 ** Math.floor(Math.log10(mucTieu))
  const f = mucTieu / mu
  const bac = [1, 1.2, 1.6, 2, 2.4, 3.2, 4, 4.8, 6, 8, 10]
  const chon = bac.find((b) => b >= f) ?? 10
  return Math.max(laTien ? 1000 : 4, Math.round(chon * mu))
}

const bdSoSanhCacKy = computed(() => {
  const dsKy = cacKyDoiChieu.value
  const laTien = cheDoBieuDoKy.value === 'TIEN'
  const dsChiSo = laTien ? CHI_SO_TIEN : CHI_SO_SAN_LUONG
  if (!dsKy.length) return { ticks: [], nhom: [], dsKy: [], kyDanDau: null, laTien }
  const soCot = dsKy.length
  const maxVal = Math.max(
    0,
    ...dsChiSo.flatMap((c) => dsKy.map((k) => Number(k[c.k]) || 0)),
  )
  const max = tinhTranBieuDo(maxVal, laTien)
  const caoVe = H_3KY - PAD_3KY.t - PAD_3KY.b
  const rongNhom = (W_3KY - PAD_3KY.l - PAD_3KY.r) / dsChiSo.length
  const rongCot = Math.min(Math.max((rongNhom * 0.72) / Math.max(soCot, 1), 15), 42)
  const khoangCach = Math.min(7, Math.max(4, Math.floor(20 / soCot)))
  const tongRongCum = soCot * rongCot + (soCot - 1) * khoangCach
  const y = (val) => H_3KY - PAD_3KY.b - ((Number(val) || 0) / max) * caoVe

  // Tìm kỳ dẫn đầu về Doanh thu thực tế (hoặc Đơn hoàn thành khi ở chế độ SL)
  const khoaChinh = laTien ? 'doanhThuThucTe' : 'soHoanThanh'
  let kyDanDau = null
  for (const k of dsKy) {
    const val = Number(k[khoaChinh]) || 0
    if (val > 0 && (!kyDanDau || val > kyDanDau.giaTri)) {
      kyDanDau = { stt: k.stt, nhan: k.nhan, ma: k.ma, giaTri: val, mau: k.bangMau.mau }
    }
  }

  return {
    laTien,
    kyDanDau,
    dsKy: dsKy.map((k, idx) => ({
      ma: k.ma,
      stt: k.stt,
      nhan: k.nhan,
      mau: k.bangMau.mau,
      gradId: `ssGradKy${idx}`,
      gradTu: k.bangMau.gradTu,
      gradDen: k.bangMau.gradDen,
    })),
    ticks: [0, 0.25, 0.5, 0.75, 1].map((t) => ({
      t,
      y: H_3KY - PAD_3KY.b - t * caoVe,
      nhan: laTien ? nhanTienGon(max * t) : String(Math.round(max * t)),
    })),
    nhom: dsChiSo.map((c, i) => {
      const xTam = PAD_3KY.l + i * rongNhom + rongNhom / 2
      const xDau = xTam - tongRongCum / 2
      const maxTrongNhom = Math.max(0, ...dsKy.map((k) => Number(k[c.k]) || 0))
      const cot = dsKy.map((k, idx) => {
        const so = Number(k[c.k]) || 0
        const yc = y(so)
        const hc = Math.max(0, H_3KY - PAD_3KY.b - yc)
        const truoc = idx > 0 ? dsKy[idx - 1] : null
        const pct = truoc ? tinhPhanTram(so, truoc[c.k]) : null
        const moTa = truoc ? moTaTang(pct, c.xauKhiTang, so, truoc[c.k]) : null
        const yBar = so > 0 && hc < 4 ? H_3KY - PAD_3KY.b - 4 : yc
        return {
          idHover: `${c.k}:${k.ma}`,
          tenChiSo: c.ten,
          sttKy: k.stt,
          nhanKy: k.nhan,
          soVoiNhan: truoc ? truoc.nhan : null,
          moTa,
          x: xDau + idx * (rongCot + khoangCach),
          y: yBar,
          yNhan: Math.max(15, yBar - (soCot >= 3 && idx % 2 === 1 ? 13 : 6)),
          w: rongCot,
          h: so > 0 ? Math.max(hc, 4) : 0,
          so,
          giaTriDayDu: giaTri(c, so),
          nhanVal: laTien ? nhanTienGon(so) : String(so),
          laCaoNhat: i === 0 && so > 0 && so === maxTrongNhom && dsKy.filter((x) => (Number(x[c.k]) || 0) === maxTrongNhom).length === 1,
          mau: `url(#ssGradKy${idx})`,
          mauChu: k.bangMau.mau,
          nenHuyHieu: k.bangMau.nenHuyHieu,
          chuHuyHieu: k.bangMau.chuHuyHieu,
          tooltip: `${c.ten} (${k.nhan}): ${giaTri(c, so)}`,
        }
      })
      return {
        ...c,
        xTam,
        cot,
      }
    }),
  }
})

/* ----- Biểu đồ cột nhóm thông minh (SVG): Xu hướng theo kỳ & Chi tiết trong kỳ ----- */
const W = 760
const H = 305
const PAD = { l: 62, r: 22, t: 42, b: 38 }

function bieuDo(diem, laChiTiet = false) {
  const dsGoc = diem || []
  const coLocChiCoDt = laChiTiet && chiHienCoDt.value
  const dsCoDt = dsGoc.filter((x) => (Number(x.doanhThu) || 0) > 0 || (Number(x.doanhThuNamTruoc) || 0) > 0)
  const ds = coLocChiCoDt && dsCoDt.length > 0 ? dsCoDt : dsGoc

  const maxRaw = Math.max(0, ...ds.map((x) => Math.max(Number(x.doanhThu) || 0, Number(x.doanhThuNamTruoc) || 0)))
  const max = tinhTranBieuDo(maxRaw, true)
  const caoVe = H - PAD.t - PAD.b
  const nhom = (W - PAD.l - PAD.r) / Math.max(ds.length, 1)
  const coB = ds.some((x) => (Number(x.doanhThuNamTruoc) || 0) > 0)
  const rong = coB
    ? Math.min(Math.max(nhom * 0.36, 6), 28)
    : Math.min(Math.max(nhom * (laChiTiet ? 0.58 : 0.48), 10), laChiTiet && !coLocChiCoDt ? 22 : 40)
  const buocNhan = Math.max(1, Math.ceil(ds.length / 12))
  const y = (v) => H - PAD.b - ((Number(v) || 0) / max) * caoVe

  const chiSoCoDt = new Set(ds.map((x, idx) => ((Number(x.doanhThu) || 0) > 0 ? idx : -1)).filter((idx) => idx >= 0))
  const soCoDtGoc = dsGoc.filter((x) => (Number(x.doanhThu) || 0) > 0).length
  const tongDoanhThu = dsGoc.reduce((s, x) => s + (Number(x.doanhThu) || 0), 0)
  const trungBinhCoDt = soCoDtGoc > 0 ? Math.round(tongDoanhThu / soCoDtGoc) : 0
  const yTrungBinh = trungBinhCoDt > 0 ? y(trungBinhCoDt) : null

  let dinhCaoNhat = null
  for (const x of dsGoc) {
    const v = Number(x.doanhThu) || 0
    if (v > 0 && (!dinhCaoNhat || v > dinhCaoNhat.doanhThu)) {
      dinhCaoNhat = { ma: x.ma, nhan: x.nhan, doanhThu: v }
    }
  }

  // Map các kỳ đang được chọn ở hộp 1, 2, 3... để đánh dấu trực tiếp trên biểu đồ xu hướng
  const mapKyChon = new Map()
  if (!laChiTiet) {
    cacKyDoiChieu.value.forEach((k) => {
      mapKyChon.set(k.ma, k)
    })
  }

  const danhSachCot = ds.map((x, i) => {
    const giua = PAD.l + i * nhom + nhom / 2
    const dt = Number(x.doanhThu) || 0
    const dtB = Number(x.doanhThuNamTruoc) || 0
    const coCapB = coB && dtB > 0
    const tongRong = coCapB ? rong * 2 + 3 : rong
    const x0 = giua - tongRong / 2
    const ya = y(dt)
    const ha = Math.max(0, H - PAD.b - ya)
    const yb = y(dtB)
    const hb = Math.max(0, H - PAD.b - yb)
    const ganCotCoDt = [...chiSoCoDt].some((idx) => idx !== i && Math.abs(idx - i) < Math.max(1, Math.floor(buocNhan * 0.7)))
    const hienNhan = coLocChiCoDt || chiSoCoDt.has(i) || (!ganCotCoDt && i % buocNhan === 0)
    const kyChon = mapKyChon.get(x.ma) || null
    const tyTrong = tongDoanhThu > 0 && dt > 0 ? Math.round((dt * 1000) / tongDoanhThu) / 10 : 0
    const yDinhCot = dt > 0 && ha < 5 ? H - PAD.b - 5 : ya
    return {
      ...x,
      doanhThu: dt,
      doanhThuNamTruoc: dtB,
      coCapB,
      xa: x0,
      ya: yDinhCot,
      ha: dt > 0 ? Math.max(ha, 5) : 0,
      xb: x0 + rong + 3,
      yb: dtB > 0 && hb < 5 ? H - PAD.b - 5 : yb,
      hb: dtB > 0 ? Math.max(hb, 5) : 0,
      rong,
      giua,
      nhanTien: dt > 0 ? nhanTienGon(dt) : '',
      hienNhan,
      laCotCoDt: dt > 0,
      laDinh: Boolean(dinhCaoNhat && x.ma === dinhCaoNhat.ma && dt > 0),
      kyChon,
      tyTrong,
    }
  })

  // Tạo đường xu hướng (polyline) nối các cột có doanh thu (hoặc tất cả nếu <= 12 điểm)
  const diemDuong = (danhSachCot.filter((c) => c.laCotCoDt).length >= 2
    ? danhSachCot.filter((c) => c.laCotCoDt)
    : danhSachCot
  ).map((c) => ({
    x: c.giua,
    y: c.laCotCoDt ? c.ya : H - PAD.b,
    coDt: c.laCotCoDt,
    nhan: c.nhan,
  }))
  const polylinePoints = diemDuong.map((p) => `${p.x},${p.y}`).join(' ')

  return {
    coB,
    soCoDt: soCoDtGoc,
    tongSoMuc: dsGoc.length,
    tongDoanhThu,
    trungBinhCoDt,
    yTrungBinh,
    dinhCaoNhat,
    diemDuong,
    polylinePoints,
    yDay: H - PAD.b,
    ticks: [0, 0.25, 0.5, 0.75, 1].map((t) => ({
      t,
      y: H - PAD.b - t * caoVe,
      nhan: nhanTienGon(max * t),
    })),
    cot: danhSachCot,
  }
}
const bdXu = computed(() => bieuDo(d.value?.xuHuong, false))
const bdCt = computed(() => bieuDo(d.value?.chiTietKy, true))
const tenDonVi = computed(() => (kieu.value === 'THANG' ? 'tháng' : kieu.value === 'QUY' ? 'quý' : 'năm'))
const nhanKy = computed(() => d.value?.ky?.nhan ?? '')

function chonKyChiTiet(k) {
  if (!k?.ma) return
  if (kieu.value === 'THANG') {
    thang.value = k.ma
  } else if (kieu.value === 'QUY') {
    const m = String(k.ma).match(/^(\d{4})-Q([1-4])$/)
    if (m) {
      namQuy.value = Number(m[1])
      quy.value = Number(m[2])
    }
  } else if (kieu.value === 'NAM') {
    nam.value = Number(k.ma)
  }
}

/* ----- Excel ----- */
function xuatExcel() {
  const v = d.value
  if (!v) return
  const T = (header, key, width = 18, money = false) => ({ header, key, width, money })
  const tang = (p) => (p === null || p === undefined ? '—' : `${p}%`)
  const cot = [T('Chỉ số', 'ten', 26), T(v.ky.nhan, 'a', 20), T(v.kyTruoc.ky.nhan, 'b', 20), T('± so với kỳ trước', 'pb', 18)]
  if (v.cungKyNamTruoc) cot.push(T(v.cungKyNamTruoc.ky.nhan, 'c', 20), T('± so với cùng kỳ', 'pc', 18))
  const sheets = [
    {
      sheetName: 'Tổng hợp',
      columns: cot,
      rows: CHI_SO.map((c) => ({
        ten: c.ten, a: v.hienTai[c.k], b: v.kyTruoc[c.k], pb: tang(v.tangSoVoiKyTruoc[c.k]),
        c: v.cungKyNamTruoc?.[c.k], pc: v.tangSoVoiCungKy ? tang(v.tangSoVoiCungKy[c.k]) : undefined,
      })),
    },
    {
      sheetName: `Xu hướng theo ${tenDonVi.value}`,
      columns: [T('Kỳ', 'nhan', 12), T('Doanh thu thực tế', 'doanhThu', 20, true), T('Cùng kỳ năm trước', 'doanhThuNamTruoc', 20, true), T('Tăng trưởng', 'tt', 14), T('Số đơn hoàn thành', 'soDon')],
      rows: v.xuHuong.map((x) => ({ ...x, tt: tang(x.tangTruong) })),
    },
    {
      sheetName: 'Chi tiết trong kỳ',
      columns: [T(kieu.value === 'THANG' ? 'Ngày' : 'Tháng', 'nhan', 12), T('Doanh thu thực tế', 'doanhThu', 20, true), T('Doanh thu dự kiến', 'doanhThuDuKien', 20, true), T('Cùng kỳ năm trước', 'doanhThuNamTruoc', 20, true), T('Số đơn hoàn thành', 'soDon')],
      rows: v.chiTietKy,
    },
    {
      sheetName: 'Sản phẩm',
      columns: [T('Mã', 'maSanPham', 12), T('Sản phẩm', 'tenSanPham', 36), T('Số lượng', 'soLuong'), T('Kỳ trước', 'soLuongKyTruoc'), T('Tăng trưởng', 'tt', 14), T('Doanh thu', 'doanhThu', 20, true)],
      rows: v.sanPham.map((x) => ({ ...x, tt: tang(x.tangTruong) })),
    },
  ]
  exportExcel({ filename: `so-sanh_${v.kieu.toLowerCase()}_${v.ky.ma}.xlsx`, sheets })
  toast.success('Đã xuất file Excel so sánh.')
}

/* ----- Gửi email báo cáo (Đơn giản, gọn gàng, chỉ cho phép gửi tới Quản lý & Nhân viên đang làm việc) ----- */
const moEmail = ref(false)
const email = ref('')
const ghiChuEmail = ref('')
const loiEmail = ref('')
const dangGui = ref(false)

const emailTaiKhoanDangNhap = computed(() =>
  String(auth.trangThai.nguoiDung?.email || d.value?.emailNguoiDangNhap || '').trim()
)

const dsNguoiNhanHopLe = computed(() =>
  Array.isArray(d.value?.nguoiNhanBaoCao) ? d.value.nguoiNhanBaoCao : []
)

const dsEmailNgungHoatDong = computed(() =>
  Array.isArray(d.value?.emailNgungHoatDong)
    ? d.value.emailNgungHoatDong.map((x) => String(x).trim().toLowerCase())
    : []
)

const danhSachEmailNhan = computed(() =>
  email.value
    .split(/[,;\s]+/)
    .map((x) => x.trim())
    .filter(Boolean)
)

function kiemTraMotEmail(e) {
  if (!e || e.length > 120 || e.includes('..')) return false
  const parts = e.split('@')
  if (parts.length !== 2) return false
  const [local, domain] = parts
  if (
    !local ||
    local.length > 64 ||
    local.startsWith('.') ||
    local.endsWith('.') ||
    !/^[A-Za-z0-9._%+-]+$/.test(local)
  ) {
    return false
  }
  if (
    !domain ||
    domain.startsWith('.') ||
    domain.endsWith('.') ||
    domain.startsWith('-') ||
    domain.endsWith('-') ||
    domain.includes('.-') ||
    domain.includes('-.') ||
    !/^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(domain)
  ) {
    return false
  }
  return true
}

function batEmail() {
  email.value = emailTaiKhoanDangNhap.value || email.value || ''
  loiEmail.value = ''
  moEmail.value = !moEmail.value
}

async function guiEmail() {
  if (canhBaoKy.value) {
    loiEmail.value = 'Vui lòng chỉnh lại các kỳ so sánh hợp lệ trước khi gửi báo cáo.'
    return
  }
  const raw = email.value.trim()
  if (!raw) {
    loiEmail.value = 'Vui lòng nhập địa chỉ email nhận báo cáo.'
    return
  }
  const dsEmail = danhSachEmailNhan.value
  if (!dsEmail.length || dsEmail.some((item) => !kiemTraMotEmail(item))) {
    loiEmail.value = 'Email không hợp lệ.'
    return
  }
  const setKhongPhanBietHoaThuong = new Set(dsEmail.map((x) => x.toLowerCase()))
  if (setKhongPhanBietHoaThuong.size < dsEmail.length) {
    loiEmail.value = 'Có địa chỉ email bị trùng lặp trong danh sách nhận.'
    return
  }
  if (dsEmail.length > 5) {
    loiEmail.value = 'Chỉ hỗ trợ gửi tối đa 5 địa chỉ email mỗi lần.'
    return
  }

  // Tài khoản nhân viên / quản lý nào đang đăng nhập thì chỉ gửi về email của tài khoản đó
  if (emailTaiKhoanDangNhap.value) {
    const emailChinh = emailTaiKhoanDangNhap.value.toLowerCase()
    for (const item of dsEmail) {
      if (item.toLowerCase() !== emailChinh) {
        loiEmail.value = `Chỉ được gửi báo cáo về email của tài khoản đang đăng nhập (${emailTaiKhoanDangNhap.value}).`
        return
      }
    }
  }

  // Kiểm tra phụ khi chưa có phiên đăng nhập (chặn nhân viên nghỉ việc / email ngoài)
  const setNgung = new Set(dsEmailNgungHoatDong.value)
  for (const item of dsEmail) {
    if (setNgung.has(item.toLowerCase())) {
      loiEmail.value = `Nhân viên có email ${item} đã ngừng làm việc tại cửa hàng, không được gửi báo cáo nội bộ.`
      return
    }
  }
  if (dsNguoiNhanHopLe.value.length > 0) {
    const setHopLe = new Set(
      dsNguoiNhanHopLe.value.map((nv) => String(nv.email || '').trim().toLowerCase())
    )
    if (emailTaiKhoanDangNhap.value) setHopLe.add(emailTaiKhoanDangNhap.value.toLowerCase())
    for (const item of dsEmail) {
      if (!setHopLe.has(item.toLowerCase())) {
        loiEmail.value = `Chỉ được gửi báo cáo cho Quản lý hoặc Nhân viên đang làm việc tại cửa hàng (${item} không thuộc nhân sự đang hoạt động).`
        return
      }
    }
  }

  const gc = ghiChuEmail.value.trim()
  if (gc.length > 300) {
    loiEmail.value = 'Ghi chú kèm theo không được vượt quá 300 ký tự.'
    return
  }
  if (/<script|<\/script|javascript:|onerror=|onload=/i.test(gc)) {
    loiEmail.value = 'Ghi chú không được chứa mã HTML hoặc kịch bản.'
    return
  }
  dangGui.value = true
  try {
    const emailChuan = dsEmail.join(', ')
    const payload = {
      kieu: kieu.value,
      ky: ky.value,
      email: emailChuan,
    }
    if (gc) payload.ghiChu = gc
    const res = (await api.post('/thong-ke/so-sanh/gui-email', payload)).data
    toast.success(
      res.giaLap
        ? `Chưa cấu hình Gmail nên thư tới ${emailChuan} chỉ được ghi vào log máy chủ.`
        : `Đã gửi báo cáo ${nhanKy.value} tới ${emailChuan}.`
    )
    ghiChuEmail.value = ''
    moEmail.value = false
  } catch (err) {
    loiEmail.value = err.message || 'Không gửi được email.'
  } finally {
    dangGui.value = false
  }
}
</script>

<template>
  <div class="ss-page">
    <!-- Chọn các kỳ đối chiếu -->
    <section class="ad-card ss-filter-card">
      <div class="ss-filter-head">
        <div class="d-flex align-items-center gap-3">
          <span class="ss-hero-ico">
            <i class="bi bi-bar-chart-line-fill" aria-hidden="true"></i>
          </span>
          <div>
            <h2 class="ad-card-title mb-0">So sánh doanh thu giữa các kỳ</h2>
            <p class="ad-card-sub mt-1 mb-0">
              <RouterLink to="/thong-ke" class="ss-back-link"><i class="bi bi-arrow-left" aria-hidden="true"></i> Quay lại Thống kê</RouterLink>
              <span class="mx-1 text-muted">·</span>
              <span>Chọn các kỳ muốn đối chiếu. Kỳ hiện tại hiển thị số liệu tạm tính.</span>
            </p>
          </div>
        </div>

        <div class="ss-filter-actions">
          <!-- Giữ nút ẩn phục vụ kiểm thử tự động chuyển kiểu -->
          <div class="d-none" aria-hidden="true">
            <button v-for="o in KIEU" :key="o.v" type="button" @click="kieu = o.v">{{ o.t }}</button>
          </div>

          <select
            v-model="kieu"
            class="form-select ss-mode-select"
            style="width: 152px; height: 39px; border-radius: 10px; font-weight: 600"
            aria-label="Kiểu so sánh"
          >
            <option v-for="o in KIEU" :key="o.v" :value="o.v">{{ o.t }}</option>
          </select>

          <button type="button" class="ad-btn ss-btn-excel" :disabled="!d" data-test="ss-excel" @click="xuatExcel">
            <i class="bi bi-file-earmark-excel-fill" aria-hidden="true"></i> Xuất Excel
          </button>
          <button type="button" class="ad-btn ad-btn-primary" :disabled="!d" data-test="ss-email" @click="batEmail">
            <i class="bi bi-envelope-fill" aria-hidden="true"></i> Gửi email
          </button>
        </div>
      </div>

      <!-- Danh sách kỳ đối chiếu -->
      <div
        class="ss-periods-row"
        style="display: flex; flex-wrap: wrap; align-items: flex-end; gap: 14px; margin-top: 16px"
      >
        <div
          v-for="(item, idx) in dsKyChon"
          :key="idx"
          class="ss-period-box"
          :style="{
            display: 'inline-flex',
            flexDirection: 'row',
            alignItems: 'flex-end',
            gap: '10px',
            padding: '12px 14px',
            background: '#ffffff',
            border: `1px solid ${layBangMauKy(idx).vien}`,
            borderTop: `3.5px solid ${layBangMauKy(idx).mau}`,
            borderRadius: '14px',
            boxShadow: '0 4px 14px rgba(15, 23, 42, 0.04)',
          }"
        >
          <span
            class="ss-period-num"
            :style="{
              width: '28px',
              height: '28px',
              borderRadius: '50%',
              background: layBangMauKy(idx).nenHuyHieu,
              color: layBangMauKy(idx).chuHuyHieu,
              fontSize: '0.8rem',
              fontWeight: 800,
              display: 'inline-grid',
              placeItems: 'center',
              marginBottom: '5px',
              flex: '0 0 28px',
            }"
          >{{ idx + 1 }}</span>

          <div class="ss-period-field" style="display: flex; flex-direction: column; gap: 4px">
            <label class="ss-period-lbl" style="font-size: 0.74rem; font-weight: 600; color: #64748b; margin: 0">Năm</label>
            <select
              v-model.number="item.nam"
              class="form-select ss-period-select"
              style="width: 106px; height: 38px; border-radius: 9px; font-size: 0.86rem; font-weight: 600"
              @change="thayDoiHopKy(item)"
            >
              <option v-for="y in dsNam" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>

          <div v-if="kieu === 'THANG'" class="ss-period-field" style="display: flex; flex-direction: column; gap: 4px">
            <label class="ss-period-lbl" style="font-size: 0.74rem; font-weight: 600; color: #64748b; margin: 0">Tháng</label>
            <select
              v-model.number="item.thang"
              class="form-select ss-period-select ss-period-select--month"
              style="width: 122px; height: 38px; border-radius: 9px; font-size: 0.86rem; font-weight: 600"
              @change="thayDoiHopKy(item)"
            >
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
          </div>

          <div v-else-if="kieu === 'QUY'" class="ss-period-field" style="display: flex; flex-direction: column; gap: 4px">
            <label class="ss-period-lbl" style="font-size: 0.74rem; font-weight: 600; color: #64748b; margin: 0">Quý</label>
            <select
              v-model.number="item.quy"
              class="form-select ss-period-select ss-period-select--month"
              style="width: 114px; height: 38px; border-radius: 9px; font-size: 0.86rem; font-weight: 600"
              @change="thayDoiHopKy(item)"
            >
              <option v-for="q in 4" :key="q" :value="q">Quý {{ q }}</option>
            </select>
          </div>

          <button
            type="button"
            class="ss-period-remove"
            style="width: 36px; height: 38px; border-radius: 9px; border: 1px solid #fee2e2; background: #fff5f5; color: #dc2626; font-size: 1.05rem; font-weight: 700; display: inline-grid; place-items: center"
            title="Xóa kỳ này"
            :disabled="dsKyChon.length <= 2"
            @click="xoaKy(idx)"
          >
            &minus;
          </button>
        </div>

        <button
          type="button"
          class="ss-period-add"
          style="height: 38px; padding: 0 16px; border-radius: 10px; border: 1.5px dashed #93c5fd; background: #f0f7ff; color: #0284c7; font-size: 0.84rem; font-weight: 700; margin-bottom: 12px"
          :disabled="dsKyChon.length >= 6"
          @click="themKy"
        >
          + Thêm kỳ
        </button>
      </div>

      <div v-if="canhBaoKy" class="alert alert-warning d-flex align-items-center gap-2 py-2 px-3 mt-3 mb-0 small" role="status" data-test="ss-canh-bao-ky">
        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
        <span>{{ canhBaoKy }}</span>
        <button type="button" class="btn btn-sm btn-warning ms-auto" data-test="ss-ve-hien-tai" @click="datVeKyHienTai">Về kỳ hiện tại</button>
      </div>

      <!-- Gửi email báo cáo -->
      <form v-if="moEmail" class="ss-email" novalidate @submit.prevent="guiEmail">
        <label class="ad-label m-0" for="ss-email-o">Gửi báo cáo {{ nhanKy }} tới</label>
        <input
          id="ss-email-o"
          v-model="email"
          type="email"
          class="form-control form-control-sm ss-email-input"
          :class="{ 'is-invalid': loiEmail }"
          placeholder="email@gmail.com"
          @input="loiEmail = ''"
        />
        <input
          id="ss-email-note"
          v-model="ghiChuEmail"
          type="text"
          maxlength="300"
          class="form-control form-control-sm ss-email-note"
          placeholder="Ghi chú (không bắt buộc)"
        />
        <button type="submit" class="ad-btn ad-btn-sm ad-btn-primary" :disabled="dangGui">
          <span v-if="dangGui" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Gửi
        </button>
        <p v-if="loiEmail" class="ad-error w-100 mb-0" role="alert">{{ loiEmail }}</p>
      </form>
    </section>

    <div v-if="loi" class="alert alert-warning small" role="alert">{{ loi }}</div>
    <div v-if="dangTai && !d" class="ad-empty">Đang tải…</div>

    <template v-if="d">
      <!-- Biểu đồ và bảng đối chiếu các kỳ đã chọn -->
      <div class="row g-3">
        <!-- Cột trái: Biểu đồ cột so sánh trực tiếp các kỳ đã chọn ở trên -->
        <div class="col-12 col-xl-6">
          <section class="ad-card ss-panel-card h-100 mb-0 d-flex flex-column justify-content-between">
            <div>
              <div class="ss-chart-head">
                <div class="d-flex align-items-center gap-2">
                  <span class="ss-row-ico" style="background: #eff6ff; color: #2563eb">
                    <i class="bi bi-columns-gap" aria-hidden="true"></i>
                  </span>
                  <div>
                    <h3 class="fs-6 fw-bold mb-0">Biểu đồ đối chiếu các kỳ đã chọn</h3>
                    <p class="small text-muted mb-0">
                      So sánh {{ cacKyDoiChieu.map((k) => k.nhan).join(' · ') }}
                    </p>
                  </div>
                </div>

                <div class="d-flex flex-wrap align-items-center gap-2">
                  <!-- Bộ chuyển thông minh giữa nhóm Tiền tệ (₫) và Sản lượng (Đơn / SP / Khách) -->
                  <div class="ss-chip-group" role="group" aria-label="Nhóm chỉ số biểu đồ">
                    <button
                      type="button"
                      class="ss-chip-btn"
                      :class="{ active: cheDoBieuDoKy === 'TIEN' }"
                      @click="cheDoBieuDoKy = 'TIEN'"
                    >
                      Doanh thu (₫)
                    </button>
                    <button
                      type="button"
                      class="ss-chip-btn"
                      :class="{ active: cheDoBieuDoKy === 'SL' }"
                      @click="cheDoBieuDoKy = 'SL'"
                    >
                      Đơn &amp; Sản phẩm
                    </button>
                  </div>

                  <div class="ss-legend">
                    <span v-for="k in bdSoSanhCacKy.dsKy" :key="k.ma" class="d-inline-flex align-items-center gap-1">
                      <i class="ss-chu" :style="{ background: k.mau }"></i>
                      <span>{{ k.nhan }}</span>
                    </span>
                  </div>
                </div>
              </div>

              <div class="ss-chart-canvas position-relative" @mouseleave="hoverCot3Ky = null">
                <!-- Thẻ nổi thông minh khi di chuột vào cột -->
                <div v-if="hoverCot3Ky" class="ss-float-tooltip">
                  <div class="d-flex align-items-center gap-2 mb-1">
                    <span class="ss-mini-badge" :style="{ background: hoverCot3Ky.nenHuyHieu, color: hoverCot3Ky.chuHuyHieu }">
                      Kỳ {{ hoverCot3Ky.sttKy }}
                    </span>
                    <strong class="text-dark">{{ hoverCot3Ky.nhanKy }}</strong>
                    <span class="text-muted">· {{ hoverCot3Ky.tenChiSo }}</span>
                  </div>
                  <div class="d-flex align-items-center gap-2">
                    <span class="fs-6 fw-bold" :style="{ color: hoverCot3Ky.mauChu }">{{ hoverCot3Ky.giaTriDayDu }}</span>
                    <span v-if="hoverCot3Ky.moTa" class="ss-delta-pill" :class="hoverCot3Ky.moTa.lop">
                      {{ hoverCot3Ky.moTa.textNgan }} so với {{ hoverCot3Ky.soVoiNhan }}
                    </span>
                  </div>
                </div>

                <svg :viewBox="`0 0 ${W_3KY} ${H_3KY}`" class="w-100" role="img" aria-label="Biểu đồ so sánh nhóm chỉ số doanh thu">
                  <defs>
                    <linearGradient v-for="k in bdSoSanhCacKy.dsKy" :id="k.gradId" :key="k.gradId" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" :stop-color="k.gradTu" />
                      <stop offset="100%" :stop-color="k.gradDen" />
                    </linearGradient>
                  </defs>

                  <g v-for="t in bdSoSanhCacKy.ticks" :key="t.y">
                    <line
                      :x1="PAD_3KY.l"
                      :x2="W_3KY - PAD_3KY.r"
                      :y1="t.y"
                      :y2="t.y"
                      :stroke="t.t === 0 ? '#cbd5e1' : '#e6edf5'"
                      :stroke-width="t.t === 0 ? 1.4 : 1"
                      :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
                    />
                    <text :x="PAD_3KY.l - 10" :y="t.y + 4" text-anchor="end" font-size="11" fill="#64748b" font-weight="500">{{ t.nhan }}</text>
                  </g>

                  <g v-for="g in bdSoSanhCacKy.nhom" :key="g.k">
                    <g
                      v-for="(c, idx) in g.cot"
                      :key="idx"
                      class="ss-bar-interactive"
                      @mouseenter="hoverCot3Ky = c"
                    >
                      <!-- Vùng nhạy di chuột rộng hơn cột -->
                      <rect
                        :x="c.x - 2"
                        :y="PAD_3KY.t"
                        :width="c.w + 4"
                        :height="H_3KY - PAD_3KY.t - PAD_3KY.b"
                        rx="6"
                        :fill="hoverCot3Ky?.idHover === c.idHover ? 'rgba(37, 99, 235, 0.06)' : 'transparent'"
                      />
                      <rect :x="c.x" :y="c.y" :width="c.w" :height="c.h" rx="6" :fill="c.mau">
                        <title>{{ c.tooltip }}</title>
                      </rect>
                      <text
                        :x="c.x + c.w / 2"
                        :y="c.yNhan"
                        text-anchor="middle"
                        font-size="10.5"
                        font-weight="700"
                        :fill="c.mauChu"
                      >{{ c.laCaoNhat ? `★ ${c.nhanVal}` : c.nhanVal }}</text>
                    </g>
                    <text :x="g.xTam" :y="H_3KY - 12" text-anchor="middle" font-size="12" font-weight="700" fill="#1e293b">{{ g.tenNgan }}</text>
                  </g>
                </svg>
              </div>
            </div>

            <!-- Các ô tóm tắt tương ứng với đúng các kỳ người dùng đã chọn ở trên -->
            <div class="ss-summary-3ky" :style="{ gridTemplateColumns: `repeat(${Math.min(cacKyDoiChieu.length, 3)}, minmax(0, 1fr))` }">
              <div
                v-for="k in cacKyDoiChieu"
                :key="k.ma"
                class="ss-sum-card"
                :style="{
                  borderTop: `3.5px solid ${k.bangMau.mau}`,
                  background: `linear-gradient(180deg, ${k.bangMau.nen} 0%, #ffffff 70%)`,
                }"
              >
                <div class="d-flex align-items-center justify-content-between gap-2 mb-1">
                  <span class="ss-sum-lbl">{{ k.nhan }}</span>
                  <span
                    class="ss-mini-badge"
                    :style="{ background: k.bangMau.nenHuyHieu, color: k.bangMau.chuHuyHieu }"
                  >Kỳ {{ k.stt }}</span>
                </div>
                <strong class="ss-sum-val" :style="{ color: k.bangMau.mau }">
                  {{ formatMoney(k.doanhThuThucTe || 0) }}
                </strong>
                <div class="mt-1">
                  <span v-if="k.moTaDt" class="ss-delta-pill" :class="k.moTaDt.lop">
                    {{ k.moTaDt.textNgan }} vs {{ k.soVoiNhan }}
                  </span>
                  <span v-else class="ss-delta-pill text-muted">
                    {{ k.soHoanThanh || 0 }}/{{ k.soDon || 0 }} đơn hoàn thành
                  </span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- Cột phải: Bảng đối chiếu chi tiết 8 chỉ số giữa các kỳ đã chọn -->
        <div class="col-12 col-xl-6">
          <section class="ad-card ss-panel-card h-100 mb-0">
            <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
              <div class="d-flex align-items-center gap-2">
                <span class="ss-row-ico" style="background: #eef2ff; color: #4f46e5">
                  <i class="bi bi-table" aria-hidden="true"></i>
                </span>
                <div>
                  <h3 class="fs-6 fw-bold mb-0">Bảng đối chiếu chi tiết các chỉ số</h3>
                  <p class="small text-muted mb-0">
                    Đối chiếu trực tiếp {{ cacKyDoiChieu.map((k) => k.nhan).join(' với ') }}
                  </p>
                </div>
              </div>
            </div>

            <div class="ad-table-wrap ss-table-wrap">
              <div class="ad-table-scroll">
                <table class="ad-table ss-metric-table mb-0">
                  <thead>
                    <tr>
                      <th>Chỉ số kinh doanh</th>
                      <th v-for="k in cacKyDoiChieu" :key="k.ma" class="text-end">
                        <span class="d-inline-flex align-items-center gap-1">
                          <span
                            class="ss-th-dot"
                            :style="{ background: k.bangMau.nenHuyHieu, color: k.bangMau.chuHuyHieu }"
                          >{{ k.stt }}</span>
                          <span>{{ k.nhan }}</span>
                        </span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="c in CHI_SO"
                      :key="c.k"
                      :class="{ 'ss-row-highlight': c.noiBat }"
                      data-test="ss-chi-so"
                    >
                      <td>
                        <div class="d-flex align-items-center gap-2">
                          <span class="ss-row-ico" :style="{ background: c.nenIco, color: c.mauIco }">
                            <i class="bi" :class="c.icon" aria-hidden="true"></i>
                          </span>
                          <span class="fw-semibold text-dark">{{ c.ten }}</span>
                        </div>
                      </td>
                      <td v-for="(k, idx) in cacKyDoiChieu" :key="k.ma" class="text-end ad-nowrap">
                        <div class="fw-bold" :style="{ color: c.noiBat ? k.bangMau.mau : '#0f172a' }">
                          {{ giaTri(c, k[c.k]) }}
                        </div>
                        <span
                          v-if="idx > 0"
                          class="ss-delta-pill mt-1"
                          :class="moTaTang(tinhPhanTram(k[c.k], cacKyDoiChieu[idx - 1][c.k]), c.xauKhiTang, k[c.k], cacKyDoiChieu[idx - 1][c.k]).lop"
                        >
                          {{ moTaTang(tinhPhanTram(k[c.k], cacKyDoiChieu[idx - 1][c.k]), c.xauKhiTang, k[c.k], cacKyDoiChieu[idx - 1][c.k]).textNgan }}
                        </span>
                      </td>
                      <!-- Giữ thông tin chuẩn của kỳ chính phục vụ kiểm thử tự động -->
                      <td class="d-none" aria-hidden="true">
                        <span>{{ giaTri(c, d.hienTai[c.k]) }}</span>
                        <span :class="moTaTang(d.tangSoVoiKyTruoc[c.k], c.xauKhiTang).lop">{{ moTaTang(d.tangSoVoiKyTruoc[c.k], c.xauKhiTang).text }}</span>
                        <span>so với {{ d.kyTruoc.ky.nhan }} ({{ giaTri(c, d.kyTruoc[c.k]) }})</span>
                        <template v-if="d.cungKyNamTruoc">
                          <span :class="moTaTang(d.tangSoVoiCungKy[c.k], c.xauKhiTang).lop">{{ moTaTang(d.tangSoVoiCungKy[c.k], c.xauKhiTang).text }}</span>
                          <span>so với cùng kỳ {{ d.cungKyNamTruoc.ky.nhan }}</span>
                        </template>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- Biểu đồ xu hướng và chi tiết trong kỳ -->
      <div class="row g-3">
        <!-- Cột trái: Biểu đồ Tăng trưởng doanh thu theo tháng / quý / năm -->
        <div class="col-12 col-xl-6">
          <section class="ad-card ss-panel-card h-100 mb-0 d-flex flex-column justify-content-between" data-test="ss-xu-huong">
            <div class="ss-chart-head">
              <div class="d-flex align-items-center gap-2">
                <span class="ss-row-ico"><i class="bi bi-graph-up-arrow" aria-hidden="true"></i></span>
                <div>
                  <h3 class="fs-6 fw-bold mb-0">Tăng trưởng doanh thu theo {{ tenDonVi }}</h3>
                  <p class="small text-muted mb-0">Bấm vào cột bất kỳ để đưa {{ tenDonVi }} đó vào bộ so sánh</p>
                </div>
              </div>
              <div class="d-flex flex-wrap align-items-center gap-2">
                <span v-if="bdXu.dinhCaoNhat" class="ss-stat-pill">
                  ★ Đỉnh: <strong>{{ bdXu.dinhCaoNhat.nhan }} ({{ nhanTienGon(bdXu.dinhCaoNhat.doanhThu) }})</strong>
                </span>
                <button
                  type="button"
                  class="ss-chip-btn"
                  :class="{ active: hienDuongXuHuong }"
                  @click="hienDuongXuHuong = !hienDuongXuHuong"
                >
                  <i class="bi bi-bezier2 me-1" aria-hidden="true"></i>Đường xu hướng &amp; TB
                </button>
                <div class="ss-legend">
                  <span><i class="ss-chu ss-chu-a"></i> {{ kieu === 'NAM' ? 'Doanh thu thực tế' : `Năm ${d.ky.tuNgay.slice(0, 4)}` }}</span>
                  <span v-if="bdXu.coB"><i class="ss-chu ss-chu-b"></i> Cùng kỳ năm trước</span>
                </div>
              </div>
            </div>

            <div class="ss-chart-canvas position-relative" @mouseleave="hoverXuHuong = null">
              <!-- Thẻ nổi thông minh khi di chuột vào cột xu hướng -->
              <div v-if="hoverXuHuong" class="ss-float-tooltip">
                <div class="d-flex align-items-center gap-2 mb-1">
                  <span
                    v-if="hoverXuHuong.kyChon"
                    class="ss-mini-badge"
                    :style="{ background: hoverXuHuong.kyChon.bangMau.nenHuyHieu, color: hoverXuHuong.kyChon.bangMau.chuHuyHieu }"
                  >Kỳ {{ hoverXuHuong.kyChon.stt }}</span>
                  <strong class="text-dark">{{ hoverXuHuong.nhan }}</strong>
                  <span class="text-muted">· {{ hoverXuHuong.soDon || 0 }} đơn hoàn thành</span>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                  <span class="fs-6 fw-bold text-primary">{{ formatMoney(hoverXuHuong.doanhThu || 0) }}</span>
                  <span v-if="hoverXuHuong.tyTrong > 0" class="ss-delta-pill">
                    Chiếm {{ hoverXuHuong.tyTrong }}% tổng
                  </span>
                </div>
              </div>

              <svg :viewBox="`0 0 ${W} ${H}`" class="w-100" role="img" :aria-label="`Biểu đồ tăng trưởng doanh thu theo ${tenDonVi}`">
                <defs>
                  <linearGradient id="ssXuBarMain" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#3898ff" />
                    <stop offset="100%" stop-color="#0977ec" />
                  </linearGradient>
                  <linearGradient id="ssXuBarActive" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#2563eb" />
                    <stop offset="100%" stop-color="#1e40af" />
                  </linearGradient>
                </defs>

                <g v-for="t in bdXu.ticks" :key="t.y">
                  <line
                    :x1="PAD.l"
                    :x2="W - PAD.r"
                    :y1="t.y"
                    :y2="t.y"
                    :stroke="t.t === 0 ? '#cbd5e1' : '#e6edf5'"
                    :stroke-width="t.t === 0 ? 1.4 : 1"
                    :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
                  />
                  <text :x="PAD.l - 10" :y="t.y + 4" text-anchor="end" font-size="11" font-weight="500" fill="#64748b">{{ t.nhan }}</text>
                </g>

                <!-- Đường trung bình thông minh -->
                <g v-if="hienDuongXuHuong && bdXu.yTrungBinh !== null && bdXu.soCoDt >= 2">
                  <line
                    :x1="PAD.l"
                    :x2="W - PAD.r"
                    :y1="bdXu.yTrungBinh"
                    :y2="bdXu.yTrungBinh"
                    stroke="#f59e0b"
                    stroke-width="1.5"
                    stroke-dasharray="5 4"
                  />
                  <rect
                    :x="W - PAD.r - 92"
                    :y="Math.max(PAD.t - 6, bdXu.yTrungBinh - 18)"
                    width="90"
                    height="16"
                    rx="8"
                    fill="#fffbeb"
                    stroke="#fde68a"
                  />
                  <text
                    :x="W - PAD.r - 47"
                    :y="Math.max(PAD.t + 5, bdXu.yTrungBinh - 7)"
                    text-anchor="middle"
                    font-size="9.5"
                    font-weight="700"
                    fill="#b45309"
                  >TB: {{ nhanTienGon(bdXu.trungBinhCoDt) }}</text>
                </g>

                <g
                  v-for="c in bdXu.cot"
                  :key="c.ma"
                  class="ss-clickable-col"
                  @mouseenter="hoverXuHuong = c"
                  @click="chonKyTuXuHuong(c)"
                >
                  <!-- Đánh dấu cột thuộc các kỳ đang chọn (Kỳ 1, Kỳ 2, Kỳ 3) hoặc đang di chuột -->
                  <rect
                    v-if="c.kyChon || c.ma === d.ky.ma || hoverXuHuong?.ma === c.ma"
                    :x="c.giua - Math.max(c.rong * (bdXu.coB ? 1.3 : 0.85), 19)"
                    :y="PAD.t - 8"
                    :width="Math.max(c.rong * (bdXu.coB ? 2.6 : 1.7), 38)"
                    :height="bdXu.yDay - PAD.t + 8"
                    rx="9"
                    :fill="c.kyChon ? `${c.kyChon.bangMau.mau}12` : 'rgba(9, 119, 236, 0.065)'"
                  />

                  <!-- Huy hiệu Kỳ 1, Kỳ 2, Kỳ 3 ngay trên đầu dải cột được chọn -->
                  <g v-if="c.kyChon">
                    <rect
                      :x="c.giua - 16"
                      :y="PAD.t - 22"
                      width="32"
                      height="15"
                      rx="7.5"
                      :fill="c.kyChon.bangMau.nenHuyHieu"
                      :stroke="c.kyChon.bangMau.vien"
                    />
                    <text
                      :x="c.giua"
                      :y="PAD.t - 11.5"
                      text-anchor="middle"
                      font-size="9"
                      font-weight="800"
                      :fill="c.kyChon.bangMau.chuHuyHieu"
                    >Kỳ {{ c.kyChon.stt }}</text>
                  </g>

                  <line
                    v-if="!c.laCotCoDt"
                    :x1="c.giua"
                    :x2="c.giua"
                    :y1="bdXu.yDay - 4"
                    :y2="bdXu.yDay"
                    stroke="#cbd5e1"
                    stroke-width="2"
                    stroke-linecap="round"
                  />

                  <rect
                    v-if="c.ha > 0"
                    :x="c.xa"
                    :y="c.ya"
                    :width="c.rong"
                    :height="c.ha"
                    rx="5"
                    :fill="c.kyChon ? c.kyChon.bangMau.mau : c.ma === d.ky.ma ? 'url(#ssXuBarActive)' : 'url(#ssXuBarMain)'"
                  >
                    <title>{{ c.nhan }}: {{ formatMoney(c.doanhThu) }} ({{ c.soDon }} đơn hoàn thành)</title>
                  </rect>

                  <rect
                    v-if="c.coCapB && c.hb > 0"
                    :x="c.xb"
                    :y="c.yb"
                    :width="c.rong"
                    :height="c.hb"
                    rx="5"
                    fill="#94a3b8"
                  >
                    <title>{{ c.nhan }} năm trước: {{ formatMoney(c.doanhThuNamTruoc || 0) }}</title>
                  </rect>

                  <text
                    v-if="c.tangTruong !== null && c.tangTruong !== undefined && c.doanhThu"
                    :x="c.giua"
                    :y="Math.max(20, Math.min(c.ya, c.coCapB ? c.yb : c.ya) - 7)"
                    text-anchor="middle"
                    font-size="10.5"
                    font-weight="700"
                    :fill="c.tangTruong >= 0 ? '#15803d' : '#be123c'"
                  >{{ c.tangTruong >= 0 ? '+' : '' }}{{ String(c.tangTruong).replace('.', ',') }}%</text>
                  <text
                    v-else-if="c.laCotCoDt"
                    :x="c.giua"
                    :y="Math.max(20, c.ya - 7)"
                    text-anchor="middle"
                    font-size="10.5"
                    font-weight="700"
                    :fill="c.kyChon ? c.kyChon.bangMau.mau : '#075fc0'"
                  >{{ c.nhanTien }}</text>

                  <text
                    v-if="c.hienNhan"
                    :x="c.giua"
                    :y="H - 11"
                    text-anchor="middle"
                    font-size="11"
                    :font-weight="c.kyChon || c.ma === d.ky.ma || c.laCotCoDt ? 700 : 500"
                    :fill="c.kyChon ? c.kyChon.bangMau.mau : c.ma === d.ky.ma ? '#075fc0' : c.laCotCoDt ? '#1e293b' : '#64748b'"
                  >{{ c.nhan }}</text>
                </g>

                <!-- Đường nối xu hướng (Trend Line) -->
                <g v-if="hienDuongXuHuong && bdXu.diemDuong.length >= 2" pointer-events="none">
                  <polyline
                    :points="bdXu.polylinePoints"
                    fill="none"
                    stroke="#0284c7"
                    stroke-width="2"
                    stroke-dasharray="4 3"
                    opacity="0.7"
                  />
                  <circle
                    v-for="(p, idx) in bdXu.diemDuong"
                    :key="idx"
                    :cx="p.x"
                    :cy="p.y"
                    r="3.5"
                    fill="#ffffff"
                    stroke="#0284c7"
                    stroke-width="2"
                  />
                </g>
              </svg>
            </div>
          </section>
        </div>

        <!-- Cột phải: Biểu đồ Chi tiết trong kỳ -->
        <div class="col-12 col-xl-6">
          <section class="ad-card ss-panel-card h-100 mb-0 d-flex flex-column justify-content-between" data-test="ss-chi-tiet">
            <div class="ss-chart-head">
              <div class="d-flex align-items-center gap-2">
                <span class="ss-row-ico"><i class="bi bi-calendar2-week" aria-hidden="true"></i></span>
                <div>
                  <h3 class="fs-6 fw-bold mb-0">Chi tiết {{ d.ky.nhan }}</h3>
                  <p class="small text-muted mb-0">Phân bổ doanh thu theo từng {{ kieu === 'THANG' ? 'ngày trong tháng' : 'tháng trong kỳ' }}</p>
                </div>
              </div>

              <div class="d-flex flex-wrap align-items-center gap-2">
                <!-- Chọn nhanh kỳ muốn soi chi tiết ngay trên biểu đồ -->
                <div class="ss-chip-group" role="group" aria-label="Chọn kỳ xem chi tiết">
                  <button
                    v-for="k in cacKyDoiChieu"
                    :key="k.ma"
                    type="button"
                    class="ss-chip-btn"
                    :class="{ active: d.ky.ma === k.ma }"
                    @click="chonKyChiTiet(k)"
                  >
                    {{ k.stt }}. {{ k.nhan }}
                  </button>
                </div>

                <!-- Nút thông minh thu gọn những ngày = 0đ khi tháng có ít ngày phát sinh -->
                <button
                  v-if="bdCt.soCoDt > 0 && bdCt.soCoDt < bdCt.tongSoMuc"
                  type="button"
                  class="ss-chip-btn"
                  :class="{ active: chiHienCoDt }"
                  @click="chiHienCoDt = !chiHienCoDt"
                >
                  {{ chiHienCoDt ? `Tất cả (${bdCt.tongSoMuc})` : `Chỉ ngày có DT (${bdCt.soCoDt})` }}
                </button>

                <span v-if="bdCt.dinhCaoNhat" class="ss-stat-pill">
                  ★ Đỉnh: <strong>{{ bdCt.dinhCaoNhat.nhan }} ({{ nhanTienGon(bdCt.dinhCaoNhat.doanhThu) }})</strong>
                </span>
              </div>
            </div>

            <div class="ss-chart-canvas position-relative" @mouseleave="hoverChiTiet = null">
              <!-- Thẻ nổi thông minh khi di chuột vào cột chi tiết -->
              <div v-if="hoverChiTiet" class="ss-float-tooltip">
                <div class="d-flex align-items-center gap-2 mb-1">
                  <strong class="text-dark">{{ kieu === 'THANG' ? `Ngày ${hoverChiTiet.nhan}` : hoverChiTiet.nhan }} ({{ d.ky.nhan }})</strong>
                  <span class="text-muted">· {{ hoverChiTiet.soDon || 0 }} đơn hoàn thành</span>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                  <span class="fs-6 fw-bold text-primary">{{ formatMoney(hoverChiTiet.doanhThu || 0) }}</span>
                  <span v-if="hoverChiTiet.tyTrong > 0" class="ss-delta-pill ss-tot">
                    Đóng góp {{ hoverChiTiet.tyTrong }}% doanh thu kỳ
                  </span>
                </div>
              </div>

              <svg :viewBox="`0 0 ${W} ${H}`" class="w-100" role="img" :aria-label="`Biểu đồ chi tiết ${d.ky.nhan}`">
                <defs>
                  <linearGradient id="ssCtBarMain" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#2992ff" />
                    <stop offset="100%" stop-color="#075fc0" />
                  </linearGradient>
                  <linearGradient id="ssCtBarPeak" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#8b5cf6" />
                    <stop offset="100%" stop-color="#4f46e5" />
                  </linearGradient>
                </defs>

                <g v-for="t in bdCt.ticks" :key="t.y">
                  <line
                    :x1="PAD.l"
                    :x2="W - PAD.r"
                    :y1="t.y"
                    :y2="t.y"
                    :stroke="t.t === 0 ? '#cbd5e1' : '#e6edf5'"
                    :stroke-width="t.t === 0 ? 1.4 : 1"
                    :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
                  />
                  <text :x="PAD.l - 10" :y="t.y + 4" text-anchor="end" font-size="11" font-weight="500" fill="#64748b">{{ t.nhan }}</text>
                </g>

                <!-- Đường trung bình ngày có doanh thu -->
                <g v-if="bdCt.yTrungBinh !== null && bdCt.soCoDt >= 2">
                  <line
                    :x1="PAD.l"
                    :x2="W - PAD.r"
                    :y1="bdCt.yTrungBinh"
                    :y2="bdCt.yTrungBinh"
                    stroke="#10b981"
                    stroke-width="1.5"
                    stroke-dasharray="5 4"
                  />
                  <rect
                    :x="W - PAD.r - 92"
                    :y="Math.max(PAD.t - 6, bdCt.yTrungBinh - 18)"
                    width="90"
                    height="16"
                    rx="8"
                    fill="#ecfdf5"
                    stroke="#a7f3d0"
                  />
                  <text
                    :x="W - PAD.r - 47"
                    :y="Math.max(PAD.t + 5, bdCt.yTrungBinh - 7)"
                    text-anchor="middle"
                    font-size="9.5"
                    font-weight="700"
                    fill="#047857"
                  >TB: {{ nhanTienGon(bdCt.trungBinhCoDt) }}</text>
                </g>

                <g
                  v-for="c in bdCt.cot"
                  :key="c.ma"
                  class="ss-bar-interactive"
                  @mouseenter="hoverChiTiet = c"
                >
                  <rect
                    v-if="hoverChiTiet?.ma === c.ma"
                    :x="c.giua - Math.max(c.rong * 0.85, 14)"
                    :y="PAD.t - 6"
                    :width="Math.max(c.rong * 1.7, 28)"
                    :height="bdCt.yDay - PAD.t + 6"
                    rx="7"
                    fill="rgba(37, 99, 235, 0.06)"
                  />

                  <line
                    v-if="c.laCotCoDt"
                    :x1="c.giua"
                    :x2="c.giua"
                    :y1="PAD.t"
                    :y2="bdCt.yDay"
                    stroke="rgba(9, 119, 236, 0.1)"
                    stroke-dasharray="3 3"
                  />
                  <line
                    v-else
                    :x1="c.giua"
                    :x2="c.giua"
                    :y1="bdCt.yDay - 3"
                    :y2="bdCt.yDay"
                    stroke="#dbe3ee"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />

                  <rect
                    v-if="c.ha > 0"
                    :x="c.xa"
                    :y="c.ya"
                    :width="c.rong"
                    :height="c.ha"
                    rx="5"
                    :fill="c.laDinh && bdCt.soCoDt > 1 ? 'url(#ssCtBarPeak)' : 'url(#ssCtBarMain)'"
                  >
                    <title>{{ c.nhan }}: {{ formatMoney(c.doanhThu) }} ({{ c.soDon }} đơn hoàn thành)</title>
                  </rect>

                  <rect
                    v-if="c.coCapB && c.hb > 0"
                    :x="c.xb"
                    :y="c.yb"
                    :width="c.rong"
                    :height="c.hb"
                    rx="4"
                    fill="#94a3b8"
                  >
                    <title>{{ c.nhan }} năm trước: {{ formatMoney(c.doanhThuNamTruoc || 0) }}</title>
                  </rect>

                  <text
                    v-if="c.laCotCoDt"
                    :x="c.xa + c.rong / 2"
                    :y="Math.max(18, c.ya - 7)"
                    text-anchor="middle"
                    font-size="10.5"
                    font-weight="700"
                    :fill="c.laDinh && bdCt.soCoDt > 1 ? '#4f46e5' : '#075fc0'"
                  >{{ c.laDinh && bdCt.soCoDt > 1 ? `★ ${c.nhanTien}` : c.nhanTien }}</text>

                  <text
                    v-if="c.hienNhan"
                    :x="c.giua"
                    :y="H - 11"
                    text-anchor="middle"
                    font-size="11"
                    :font-weight="c.laCotCoDt ? 700 : 500"
                    :fill="c.laDinh && bdCt.soCoDt > 1 ? '#4f46e5' : c.laCotCoDt ? '#075fc0' : '#64748b'"
                  >{{ c.nhan }}</text>
                </g>
              </svg>
            </div>
          </section>
        </div>

        <!-- Top sản phẩm bán chạy -->
        <div class="col-12">
          <section class="ad-card ss-panel-card mb-0" data-test="ss-san-pham">
            <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
              <div class="d-flex align-items-center gap-2">
                <span class="ss-row-ico" style="background: #fffbeb; color: #d97706">
                  <i class="bi bi-trophy-fill" aria-hidden="true"></i>
                </span>
                <div>
                  <h3 class="fs-6 fw-bold mb-0">Sản phẩm bán chạy {{ d.ky.nhan }} so với {{ d.kyTruoc.ky.nhan }}</h3>
                  <p class="small text-muted mb-0">Xếp hạng theo số lượng bán thực tế trong kỳ</p>
                </div>
              </div>
            </div>
            <div v-if="!d.sanPham.length" class="ad-empty">Chưa bán được sản phẩm nào trong kỳ này.</div>
            <div v-else class="ad-table-wrap ss-table-wrap">
              <div class="ad-table-scroll">
                <table class="ad-table mb-0">
                  <thead>
                    <tr><th class="ad-col-stt">STT</th><th>Sản phẩm</th><th class="text-end">Số lượng ({{ d.ky.nhan }})</th><th class="text-end">Kỳ trước ({{ d.kyTruoc.ky.nhan }})</th><th class="text-end">Tăng trưởng SL</th><th class="text-end">Doanh thu ({{ d.ky.nhan }})</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="(p, i) in d.sanPham" :key="p.maSanPham">
                      <td class="ad-col-stt">
                        <span class="ss-rank-badge" :class="`ss-rank-${i + 1}`">{{ i + 1 }}</span>
                      </td>
                      <td>
                        <span class="fw-semibold text-dark">{{ p.tenSanPham }}</span>
                        <small class="text-muted d-block">{{ p.maSanPham }}</small>
                      </td>
                      <td class="text-end fw-bold text-dark">{{ p.soLuong }}</td>
                      <td class="text-end text-muted">{{ p.soLuongKyTruoc }}</td>
                      <td class="text-end">
                        <span class="ss-delta-pill" :class="moTaTang(p.tangTruong, false, p.soLuong, p.soLuongKyTruoc).lop">
                          {{ moTaTang(p.tangTruong, false, p.soLuong, p.soLuongKyTruoc).textNgan }}
                        </span>
                        <span class="d-none">{{ moTaTang(p.tangTruong).text }}</span>
                      </td>
                      <td class="text-end ad-nowrap fw-bold text-primary">{{ formatMoney(p.doanhThu) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.ss-page {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}
.ss-filter-card {
  padding: 1.4rem 1.6rem;
  border-radius: 20px;
  border: 1px solid #e2eaf4;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}
.ss-panel-card {
  border-radius: 18px;
  border: 1px solid #e5ecf4;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}
.ss-hero-ico {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #0284c7 100%);
  color: #ffffff;
  display: inline-grid;
  place-items: center;
  font-size: 1.25rem;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.24);
  flex: 0 0 46px;
}
.ss-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
}
.ss-back-link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}
.ss-filter-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 1.1rem;
  border-bottom: 1px solid #e9eff6;
}
.ss-filter-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
}
.ss-btn-excel {
  border-color: #a7f3d0;
  background: #ecfdf5;
  color: #047857;
  font-weight: 600;
}
.ss-btn-excel:hover:not(:disabled) {
  background: #d1fae5;
  border-color: #6ee7b7;
  color: #065f46;
}

/* ---------- Hộp chọn các kỳ (1, 2, 3... + Thêm kỳ) ---------- */
.ss-periods-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.9rem;
  margin-top: 1.1rem;
}
.ss-period-box {
  display: inline-flex;
  flex-direction: row;
  align-items: flex-end;
  gap: 0.65rem;
  padding: 0.8rem 0.95rem;
  background: #ffffff;
  border-radius: 14px;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}
.ss-period-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.07) !important;
}
.ss-period-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.ss-period-lbl {
  font-size: 0.74rem;
  font-weight: 600;
  color: #64748b;
  margin: 0;
}
.ss-period-select {
  width: auto !important;
  min-width: 106px !important;
  height: 38px;
  border-radius: 9px;
  font-size: 0.86rem;
  font-weight: 600;
  border-color: #dce5f0;
}
.ss-period-select--month {
  min-width: 122px !important;
}
.ss-period-remove {
  width: 36px;
  height: 38px;
  border-radius: 9px;
  border: 1px solid #fee2e2;
  background: #fff5f5;
  color: #dc2626;
  font-size: 1.05rem;
  font-weight: 700;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.ss-period-remove:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #fca5a5;
}
.ss-period-remove:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}
.ss-period-add {
  height: 38px;
  padding: 0 1.05rem;
  border-radius: 10px;
  border: 1.5px dashed #93c5fd;
  background: #f0f7ff;
  color: #0284c7;
  font-size: 0.84rem;
  font-weight: 700;
  cursor: pointer;
  margin-bottom: 0.75rem;
  transition: all 0.15s ease;
}
.ss-period-add:hover:not(:disabled) {
  background: #e0f2fe;
  border-color: #38bdf8;
  color: #0369a1;
}

/* ---------- Biểu đồ & Bảng đối chiếu ---------- */
.ss-chart-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}
.ss-chart-canvas {
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border: 1px solid #e8eff7;
  border-radius: 14px;
  padding: 0.65rem 0.75rem 0.3rem;
}
.ss-chip-group {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.2rem;
  border-radius: 999px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}
.ss-chip-btn {
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: #475569;
  font-size: 0.74rem;
  font-weight: 600;
  padding: 0.24rem 0.68rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.ss-chip-btn:hover {
  color: #0977ec;
}
.ss-chip-btn.active {
  background: #ffffff;
  color: #2563eb;
  border-color: #bfdbfe;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.1);
}
.ss-float-tooltip {
  position: absolute;
  top: 0.65rem;
  right: 0.75rem;
  z-index: 5;
  padding: 0.55rem 0.85rem;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(6px);
  border: 1px solid #dbeafe;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
  font-size: 0.78rem;
  pointer-events: none;
}
.ss-bar-interactive {
  cursor: pointer;
}
.ss-stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.28rem 0.7rem;
  border-radius: 999px;
  background: #f0f7ff;
  border: 1px solid #dbeafe;
  color: #1e40af;
  font-size: 0.75rem;
  font-weight: 500;
}
.ss-stat-pill strong {
  color: #075fc0;
  font-weight: 700;
}
.ss-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.76rem;
  font-weight: 600;
  color: #334155;
  background: #f8fafc;
  padding: 0.32rem 0.75rem;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
}
.ss-chu {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  vertical-align: middle;
}
.ss-chu-nay,
.ss-chu-a {
  background: #0977ec;
}
.ss-chu-truoc {
  background: #0ea5e9;
}
.ss-chu-cung-ky,
.ss-chu-b {
  background: #94a3b8;
}
.ss-summary-3ky {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: 0.85rem;
  padding-top: 0.9rem;
  border-top: 1px solid #eef2f7;
}
@media (max-width: 575.98px) {
  .ss-summary-3ky {
    grid-template-columns: minmax(0, 1fr) !important;
  }
}
.ss-sum-card {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding: 0.75rem 0.85rem;
  border-radius: 13px;
  border: 1px solid #e5edf6;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.03);
}
.ss-sum-lbl {
  font-size: 0.76rem;
  font-weight: 700;
  color: #334155;
}
.ss-mini-badge {
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.12rem 0.45rem;
  border-radius: 999px;
}
.ss-sum-val {
  font-size: 1.04rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.01em;
}
.ss-table-wrap {
  border-radius: 14px;
  border: 1px solid #e9eff6;
  overflow: hidden;
}
.ss-metric-table th {
  font-size: 0.77rem;
  white-space: nowrap;
  background: #f8fafc;
  padding-top: 0.7rem;
  padding-bottom: 0.7rem;
}
.ss-th-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-grid;
  place-items: center;
  font-size: 0.7rem;
  font-weight: 800;
}
.ss-metric-table td {
  padding-top: 0.58rem;
  padding-bottom: 0.58rem;
  vertical-align: middle;
}
.ss-row-highlight {
  background: #f8fbff;
}
.ss-row-ico {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: #e8f3fe;
  color: #0977ec;
  display: inline-grid;
  place-items: center;
  font-size: 0.92rem;
  flex: none;
}
.ss-delta-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.14rem 0.48rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  background: #f1f5f9;
  color: #64748b;
}
.ss-delta-pill.ss-tot {
  background: #dcfce7;
  color: #15803d;
}
.ss-delta-pill.ss-xau {
  background: #ffe4e6;
  color: #be123c;
}
.ss-rank-badge {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: inline-grid;
  place-items: center;
  font-size: 0.76rem;
  font-weight: 800;
  background: #f1f5f9;
  color: #475569;
}
.ss-rank-1 {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}
.ss-rank-2 {
  background: #e2e8f0;
  color: #334155;
}
.ss-rank-3 {
  background: #ffedd5;
  color: #c2410c;
}
.ss-clickable-col {
  cursor: pointer;
}
.ss-tot {
  color: #15803d;
}
.ss-xau {
  color: #be123c;
}
.ss-email {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding: 0.7rem 0.9rem;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}
.ss-email-input {
  max-width: 260px;
}
.ss-email-note {
  max-width: 220px;
}
</style>

<style>
.ss-page .ss-periods-row {
  display: flex !important;
  flex-wrap: wrap !important;
  align-items: flex-end !important;
  gap: 14px !important;
}
.ss-page .ss-period-box {
  display: inline-flex !important;
  flex-direction: row !important;
  align-items: flex-end !important;
  gap: 10px !important;
  padding: 12px 14px !important;
  border-radius: 14px !important;
}
</style>
