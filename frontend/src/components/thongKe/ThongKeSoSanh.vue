<script setup>
// So sánh & Tăng trưởng doanh thu qua các kỳ (Theo tháng / quý / năm / tùy chọn khoảng ngày)
// Tập trung vào Tổng doanh thu thực tế, Doanh thu dự kiến, Tổng đơn hàng, Đơn hoàn thành, AOV và Sức mua sản phẩm (không tách Tiền mặt / Chuyển khoản).
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../../services/api'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { exportExcel } from '../../utils/exportExcel'
import { formatDate, formatMoney, todayIso } from '../../utils/format'
import { nhanTienGon } from '../../utils/thongKe'

const toast = useToast()
const auth = useAuth()
const homNay = todayIso()
const namNay = Number(homNay.slice(0, 4))
const thangSoHienTai = Number(homNay.slice(5, 7))

const KIEU = [
  { v: 'THANG', t: 'Theo tháng' },
  { v: 'QUY', t: 'Theo quý' },
  { v: 'NAM', t: 'Theo năm' },
  { v: 'TUY_CHON', t: 'Tùy chọn khoảng ngày' },
]

const kieu = ref('THANG')
const thangHienTai = homNay.slice(0, 7)
const quyHienTai = Math.floor((thangSoHienTai - 1) / 3) + 1
const thang = ref(thangHienTai)
const namQuy = ref(namNay)
const quy = ref(quyHienTai)
const nam = ref(namNay)
const dsNam = Array.from({ length: Math.max(7, namNay - 2019) }, (_, i) => namNay - i)

const NHAN_QUY = {
  1: 'Quý 1 (T1 - T3)',
  2: 'Quý 2 (T4 - T6)',
  3: 'Quý 3 (T7 - T9)',
  4: 'Quý 4 (T10 - T12)',
}

const boDau = (s) =>
  String(s ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()

function ngayDauThang(y, m) {
  return `${y}-${String(m).padStart(2, '0')}-01`
}

function ngayCuoiThang(y, m) {
  const d = new Date(y, m, 0, 12)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function khoangNgayCuaMucKy(item, loai = kieu.value) {
  if (loai === 'THANG') {
    const tu = ngayDauThang(item.nam, item.thang)
    const den = ngayCuoiThang(item.nam, item.thang)
    return { tu, den, hienThi: `${formatDate(tu)} - ${formatDate(den)}` }
  }
  if (loai === 'QUY') {
    const mDau = (item.quy - 1) * 3 + 1
    const mCuoi = mDau + 2
    const tu = ngayDauThang(item.nam, mDau)
    const den = ngayCuoiThang(item.nam, mCuoi)
    return { tu, den, hienThi: `${formatDate(tu)} - ${formatDate(den)}` }
  }
  if (loai === 'TUY_CHON') {
    const tu = item.tuNgay || `${namNay}-${String(thangSoHienTai).padStart(2, '0')}-01`
    const den = item.denNgay || homNay
    return { tu, den, hienThi: `${formatDate(tu)} - ${formatDate(den)}` }
  }
  const tu = `${item.nam}-01-01`
  const den = `${item.nam}-12-31`
  return { tu, den, hienThi: `${formatDate(tu)} - ${formatDate(den)}` }
}

function taoDanhSachKyMacDinh(loai, neoThang = thangSoHienTai, neoNam = namNay) {
  if (loai === 'THANG') {
    const tTruoc =
      neoThang - 1 >= 1
        ? { nam: neoNam, thang: neoThang - 1, quy: quyHienTai, tuNgay: ngayDauThang(neoNam, neoThang - 1), denNgay: ngayCuoiThang(neoNam, neoThang - 1) }
        : { nam: neoNam - 1, thang: 12, quy: 4, tuNgay: `${neoNam - 1}-12-01`, denNgay: `${neoNam - 1}-12-31` }
    const tNay = {
      nam: neoNam,
      thang: neoThang,
      quy: quyHienTai,
      tuNgay: ngayDauThang(neoNam, neoThang),
      denNgay: ngayCuoiThang(neoNam, neoThang),
    }
    return [tTruoc, tNay]
  }
  if (loai === 'QUY') {
    const qTruoc =
       quyHienTai - 1 >= 1
        ? {
            nam: neoNam,
            thang: (quyHienTai - 1) * 3,
            quy: quyHienTai - 1,
            tuNgay: ngayDauThang(neoNam, (quyHienTai - 2) * 3 + 1),
            denNgay: ngayCuoiThang(neoNam, (quyHienTai - 1) * 3),
          }
        : {
            nam: neoNam - 1,
            thang: 12,
            quy: 4,
            tuNgay: `${neoNam - 1}-10-01`,
            denNgay: `${neoNam - 1}-12-31`,
          }
    const qNay = {
      nam: neoNam,
      thang: quyHienTai * 3,
      quy: quyHienTai,
      tuNgay: ngayDauThang(neoNam, (quyHienTai - 1) * 3 + 1),
      denNgay: ngayCuoiThang(neoNam, quyHienTai * 3),
    }
    return [qTruoc, qNay]
  }
  if (loai === 'TUY_CHON') {
    const tTruoc = neoThang - 1 >= 1 ? neoThang - 1 : 12
    const yTruoc = neoThang - 1 >= 1 ? neoNam : neoNam - 1
    return [
      { nam: yTruoc, thang: tTruoc, quy: quyHienTai, tuNgay: ngayDauThang(yTruoc, tTruoc), denNgay: ngayCuoiThang(yTruoc, tTruoc) },
      { nam: neoNam, thang: neoThang, quy: quyHienTai, tuNgay: ngayDauThang(neoNam, neoThang), denNgay: homNay },
    ]
  }
  return [
    { nam: neoNam - 1, thang: neoThang, quy: quyHienTai, tuNgay: `${neoNam - 1}-01-01`, denNgay: `${neoNam - 1}-12-31` },
    { nam: neoNam, thang: neoThang, quy: quyHienTai, tuNgay: `${neoNam}-01-01`, denNgay: `${neoNam}-12-31` },
  ]
}

const dsKyChon = ref(taoDanhSachKyMacDinh('THANG'))

function maCuaMucKy(item, loai = kieu.value) {
  if (loai === 'THANG') return `${item.nam}-${String(item.thang).padStart(2, '0')}`
  if (loai === 'QUY') return `${item.nam}-Q${item.quy}`
  if (loai === 'TUY_CHON') {
    const { tu, den } = khoangNgayCuaMucKy(item, 'TUY_CHON')
    return `${tu}_${den}`
  }
  return String(item.nam)
}

function nhanCuaMucKy(item, loai = kieu.value) {
  if (loai === 'THANG') return `Tháng ${item.thang}/${item.nam}`
  if (loai === 'QUY') return `Quý ${item.quy}/${item.nam}`
  if (loai === 'TUY_CHON') {
    const { tu, den } = khoangNgayCuaMucKy(item, 'TUY_CHON')
    return `${formatDate(tu)} - ${formatDate(den)}`
  }
  return `Năm ${item.nam}`
}

function themKy() {
  if (dsKyChon.value.length >= 6) return
  const daCo = new Set(dsKyChon.value.map((x) => maCuaMucKy(x)))
  if (kieu.value === 'THANG') {
    for (const y of dsNam) {
      for (let m = 12; m >= 1; m--) {
        if (y === namNay && m > thangSoHienTai) continue
        const ungVien = {
          nam: y,
          thang: m,
          quy: Math.floor((m - 1) / 3) + 1,
          tuNgay: ngayDauThang(y, m),
          denNgay: ngayCuoiThang(y, m),
        }
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
        const ungVien = {
          nam: y,
          thang: q * 3,
          quy: q,
          tuNgay: ngayDauThang(y, (q - 1) * 3 + 1),
          denNgay: ngayCuoiThang(y, q * 3),
        }
        if (!daCo.has(maCuaMucKy(ungVien))) {
          dsKyChon.value.push(ungVien)
          return
        }
      }
    }
  } else if (kieu.value === 'TUY_CHON') {
    for (const y of dsNam) {
      for (let m = 12; m >= 1; m--) {
        if (y === namNay && m > thangSoHienTai) continue
        const ungVien = {
          nam: y,
          thang: m,
          quy: Math.floor((m - 1) / 3) + 1,
          tuNgay: ngayDauThang(y, m),
          denNgay: y === namNay && m === thangSoHienTai ? homNay : ngayCuoiThang(y, m),
        }
        if (!daCo.has(maCuaMucKy(ungVien, 'TUY_CHON'))) {
          dsKyChon.value.push(ungVien)
          return
        }
      }
    }
  } else {
    for (const y of dsNam) {
      const ungVien = { nam: y, thang: 1, quy: 1, tuNgay: `${y}-01-01`, denNgay: `${y}-12-31` }
      if (!daCo.has(maCuaMucKy(ungVien))) {
        dsKyChon.value.push(ungVien)
        return
      }
    }
  }
}

function xoaKy(idx) {
  if (dsKyChon.value.length <= 1) {
    toast.warning('Cần giữ lại ít nhất 1 kỳ để hiển thị số liệu.')
    return
  }
  dsKyChon.value.splice(idx, 1)
  const cuoi = dsKyChon.value[dsKyChon.value.length - 1]
  if (cuoi) thayDoiHopKy(cuoi)
}

/* ----- Bộ lọc chung (Thương hiệu & Tìm sản phẩm) ----- */
const thuongHieuLoc = ref('')
const tuKhoaSpLoc = ref('')

function datLaiLocChung() {
  thuongHieuLoc.value = ''
  tuKhoaSpLoc.value = ''
}

/* ----- Bộ nhớ đệm chỉ số từng kỳ ----- */
const cacheChiSoKy = reactive({})

function luuVaoCache(loai, chiSoObj) {
  if (!chiSoObj?.ky?.ma) return
  cacheChiSoKy[`${loai}:${chiSoObj.ky.ma}`] = chiSoObj
}

function kiemTraMucKy(item, idx, loai = kieu.value) {
  if (!item) return ''
  if (loai === 'THANG') {
    const ma = maCuaMucKy(item, 'THANG')
    if (ma > thangHienTai) {
      return `Kỳ ${idx + 1} (${nhanCuaMucKy(item, 'THANG')}) vượt quá tháng hiện tại (${thangHienTai}), chưa có dữ liệu phát sinh.`
    }
    if (item.nam < 2000) {
      return `Kỳ ${idx + 1} (${nhanCuaMucKy(item, 'THANG')}) quá xa trong quá khứ (vui lòng chọn từ năm 2000 trở lại đây).`
    }
  } else if (loai === 'QUY') {
    if (item.nam > namNay || (item.nam === namNay && item.quy > quyHienTai)) {
      return `Kỳ ${idx + 1} (${nhanCuaMucKy(item, 'QUY')}) vượt quá quý hiện tại (Quý ${quyHienTai}/${namNay}), chưa có dữ liệu phát sinh.`
    }
    if (item.nam < 2000) {
      return `Kỳ ${idx + 1} (${nhanCuaMucKy(item, 'QUY')}) quá xa trong quá khứ (vui lòng chọn từ năm 2000 trở lại đây).`
    }
  } else if (loai === 'NAM') {
    if (item.nam > namNay) {
      return `Kỳ ${idx + 1} (Năm ${item.nam}) vượt quá năm hiện tại (${namNay}).`
    }
    if (item.nam < 2000) {
      return `Kỳ ${idx + 1} (Năm ${item.nam}) quá xa trong quá khứ (vui lòng chọn từ năm 2000 trở lại đây).`
    }
  } else if (loai === 'TUY_CHON') {
    const tu = String(item.tuNgay || '').trim()
    const den = String(item.denNgay || '').trim()
    if (!/^\d{4}-\d{2}-\d{2}$/.test(tu) || !/^\d{4}-\d{2}-\d{2}$/.test(den)) {
      return `Kỳ ${idx + 1}: Vui lòng chọn đầy đủ Từ ngày và Đến ngày hợp lệ.`
    }
    if (tu > den) {
      return `Kỳ ${idx + 1}: "Từ ngày" (${formatDate(tu)}) không được lớn hơn "Đến ngày" (${formatDate(den)}).`
    }
    if (tu > homNay || den > homNay) {
      return `Kỳ ${idx + 1}: Không được chọn ngày vượt quá ngày hiện tại (${formatDate(homNay)}).`
    }
    if (tu < '2000-01-01') {
      return `Kỳ ${idx + 1}: Ngày bắt đầu quá xa trong quá khứ (vui lòng chọn từ năm 2000 trở lại đây).`
    }
    const soNgay = Math.round((new Date(den) - new Date(tu)) / 86400000) + 1
    if (soNgay > 366) {
      return `Kỳ ${idx + 1}: Khoảng ngày tùy chọn tối đa 366 ngày (đang chọn ${soNgay} ngày).`
    }
  }
  return ''
}

const canhBaoKy = computed(() => {
  if (kieu.value === 'THANG') {
    const t = String(thang.value || '').trim()
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(t)) return 'Tháng chọn không hợp lệ (cần định dạng YYYY-MM).'
    if (t > thangHienTai) return `Tháng đang chọn (${t}) vượt quá tháng hiện tại (${thangHienTai}).`
    if (t < '2000-01') return 'Vui lòng chọn tháng từ năm 2000 trở lại đây.'
  }
  for (let i = 0; i < dsKyChon.value.length; i++) {
    const loiMuc = kiemTraMucKy(dsKyChon.value[i], i, kieu.value)
    if (loiMuc) return loiMuc
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

const ky = computed(() => {
  if (kieu.value === 'THANG') return thang.value
  if (kieu.value === 'QUY') return `${namQuy.value}-Q${quy.value}`
  if (kieu.value === 'TUY_CHON') {
    const cuoi = dsKyChon.value[dsKyChon.value.length - 1]
    return cuoi ? maCuaMucKy(cuoi, 'TUY_CHON') : `${homNay.slice(0, 7)}-01_${homNay}`
  }
  return String(nam.value)
})

const d = ref(null)
const dangTai = ref(false)
const loi = ref('')
let lan = 0
let daKhoiTaoKy = false

async function napDayDuChiSoCacKyChon() {
  if (!d.value) return
  const loai = kieu.value
  for (let i = 0; i < dsKyChon.value.length; i++) {
    const item = dsKyChon.value[i]
    if (kiemTraMucKy(item, i, loai)) continue
    const ma = maCuaMucKy(item, loai)
    const khoa = `${loai}:${ma}`
    if (cacheChiSoKy[khoa]) continue
    const trongXuHuong = (d.value?.xuHuong || []).find((x) => x.ma === ma)
    if (trongXuHuong && Number(trongXuHuong.doanhThu) === 0 && Number(trongXuHuong.soDon) === 0) {
      const { tu, den } = khoangNgayCuaMucKy(item, loai)
      cacheChiSoKy[khoa] = {
        ky: { ma, nhan: nhanCuaMucKy(item, loai), tuNgay: tu, denNgay: den },
        doanhThuThucTe: 0,
        doanhThuDuKien: 0,
        soDon: 0,
        soHoanThanh: 0,
        soHuy: 0,
        giaTriTrungBinh: 0,
        soSanPhamBan: 0,
        soKhachMua: 0,
        sanPhamTrongKy: [],
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
  if (kieu.value === 'THANG' && (!/^\d{4}-(0[1-9]|1[0-2])$/.test(thang.value) || thang.value < '2000-01' || thang.value > thangHienTai)) return
  if (kieu.value === 'TUY_CHON') {
    const cuoi = dsKyChon.value[dsKyChon.value.length - 1]
    if (cuoi && kiemTraMucKy(cuoi, dsKyChon.value.length - 1, 'TUY_CHON')) return
  }
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
      if (!daKhoiTaoKy && kieu.value === 'THANG' && Array.isArray(res.data?.xuHuong)) {
        daKhoiTaoKy = true
        const coDt = res.data.xuHuong.filter(
          (x) => (Number(x.doanhThu) || 0) > 0 && /^\d{4}-\d{2}$/.test(x.ma || '') && x.ma <= thangHienTai
        )
        if (coDt.length > 0 && (Number(res.data?.hienTai?.doanhThuThucTe) || 0) === 0) {
          const cuoi = coDt[coDt.length - 1]
          const y = Number(cuoi.ma.slice(0, 4))
          const m = Number(cuoi.ma.slice(5, 7))
          if (y && m && cuoi.ma < thangHienTai) {
            dsKyChon.value = [
              { nam: y, thang: m, quy: Math.floor((m - 1) / 3) + 1, tuNgay: ngayDauThang(y, m), denNgay: ngayCuoiThang(y, m) },
              {
                nam: namNay,
                thang: thangSoHienTai,
                quy: quyHienTai,
                tuNgay: ngayDauThang(namNay, thangSoHienTai),
                denNgay: ngayCuoiThang(namNay, thangSoHienTai),
              },
            ]
          }
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

function chonKieu(loaiMoi) {
  if (kieu.value === loaiMoi) return
  kieu.value = loaiMoi
}

watch(kieu, (loaiMoi) => {
  dsKyChon.value = taoDanhSachKyMacDinh(loaiMoi)
  tai()
})

watch(ky, tai)

function thayDoiHopKy(item) {
  if (kiemTraMucKy(item, 0, kieu.value)) return
  const ma = maCuaMucKy(item)
  if (kieu.value === 'THANG' && ma <= thangHienTai) {
    thang.value = ma
  } else if (kieu.value === 'QUY') {
    namQuy.value = item.nam
    quy.value = item.quy
  } else if (kieu.value === 'NAM') {
    nam.value = item.nam
  } else if (kieu.value === 'TUY_CHON') {
    tai()
  }
}

watch(
  dsKyChon,
  () => {
    napDayDuChiSoCacKyChon()
  },
  { deep: true },
)

const CHI_SO = [
  { k: 'doanhThuThucTe', ten: 'Doanh thu thực tế', tenNgan: 'Doanh thu thực tế', tien: true },
  { k: 'doanhThuDuKien', ten: 'Doanh thu dự kiến', tenNgan: 'Doanh thu dự kiến', tien: true },
  { k: 'soDon', ten: 'Tổng đơn', tenNgan: 'Tổng đơn' },
  { k: 'soHoanThanh', ten: 'Đơn hoàn thành', tenNgan: 'Đơn hoàn thành' },
  { k: 'soHuy', ten: 'Đơn hủy', tenNgan: 'Đơn hủy', xauKhiTang: true },
  { k: 'giaTriTrungBinh', ten: 'Giá trị TB / đơn', tenNgan: 'Giá trị TB / đơn', tien: true },
  { k: 'soSanPhamBan', ten: 'Sản phẩm bán ra', tenNgan: 'Sản phẩm bán ra' },
  { k: 'soKhachMua', ten: 'Khách hàng mua', tenNgan: 'Khách hàng mua' },
]

const giaTri = (c, v) => (c.tien ? formatMoney(v ?? 0) : String(v ?? 0))

function tinhPhanTram(a, b) {
  const va = Number(a) || 0
  const vb = Number(b) || 0
  if (vb === 0) return va === 0 ? 0 : null
  return Math.round(((va - vb) * 1000) / vb) / 10
}

function dinhDangPhanTram(p, coDauCong = true) {
  if (p === null || p === undefined) return '+100%'
  const num = Math.round(Number(p) * 10) / 10
  const str = String(Math.abs(num)).replace('.', ',')
  if (num > 0) return `${coDauCong ? '+' : ''}${str}%`
  if (num < 0) return `-${str}%`
  return '0%'
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
    return { text: 'Kỳ so sánh chưa có', textNgan: '0%', lop: 'text-muted' }
  }
  if (Math.abs(p) < 0.05) {
    return { text: 'Không đổi', textNgan: '0%', lop: 'text-muted' }
  }
  const tang = p > 0
  const s = `${tang ? '▲' : '▼'} ${String(Math.abs(p)).replace('.', ',')}%`
  const lop = tang !== xauKhiTang ? 'ss-tot' : 'ss-xau'
  if (valNay !== null && valGoc !== null) {
    const hieu = Number(valNay) - Number(valGoc)
    const absHieu = Math.abs(hieu)
    const strHieu = `${hieu >= 0 ? '+' : '-'}${absHieu >= 1000 ? nhanTienGon(absHieu) : String(absHieu)}`
    return { text: s, textNgan: `${s} (${strHieu})`, lop }
  }
  return { text: s, textNgan: s, lop }
}

function layChiSoDayDuCuaKy(item, ma, nhanMacDinh) {
  const v = d.value
  const khoa = `${kieu.value}:${ma}`
  const khoang = khoangNgayCuaMucKy(item)
  let goc = null
  if (cacheChiSoKy[khoa]) {
    goc = cacheChiSoKy[khoa]
  } else if (v?.hienTai?.ky?.ma === ma || v?.hienTai?.ky?.ma === nhanMacDinh) {
    goc = v.hienTai
  } else if (v?.kyTruoc?.ky?.ma === ma || v?.kyTruoc?.ky?.ma === nhanMacDinh) {
    goc = v.kyTruoc
  } else if (v?.cungKyNamTruoc?.ky?.ma === ma || v?.cungKyNamTruoc?.ky?.ma === nhanMacDinh) {
    goc = v.cungKyNamTruoc
  } else {
    const trongXu = (v?.xuHuong || []).find((x) => x.ma === ma)
    if (trongXu) {
      const dt = Number(trongXu.doanhThu) || 0
      const sd = Number(trongXu.soDon) || 0
      goc = {
        ky: { ma, nhan: nhanMacDinh, tuNgay: khoang.tu, denNgay: khoang.den },
        doanhThuThucTe: dt,
        doanhThuDuKien: dt,
        soDon: sd,
        soHoanThanh: sd,
        soHuy: 0,
        giaTriTrungBinh: sd > 0 ? Math.round(dt / sd) : 0,
        soSanPhamBan: 0,
        soKhachMua: 0,
        sanPhamTrongKy: [],
      }
    }
  }

  if (!goc) {
    goc = {
      ky: { ma, nhan: nhanMacDinh, tuNgay: khoang.tu, denNgay: khoang.den },
      doanhThuThucTe: 0,
      doanhThuDuKien: 0,
      soDon: 0,
      soHoanThanh: 0,
      soHuy: 0,
      giaTriTrungBinh: 0,
      soSanPhamBan: 0,
      soKhachMua: 0,
      sanPhamTrongKy: [],
    }
  }

  const spGocTrongKy = Array.isArray(goc.sanPhamTrongKy) ? goc.sanPhamTrongKy : []
  const th = thuongHieuLoc.value.trim()
  const kw = boDau(tuKhoaSpLoc.value)
  if ((th || kw) && spGocTrongKy.length > 0) {
    const spKhop = spGocTrongKy.filter((sp) => {
      const khopTh = !th || String(sp.thuongHieu || '').trim() === th
      const khopKw = !kw || boDau(`${sp.maSanPham || ''} ${sp.tenSanPham || ''} ${sp.thuongHieu || ''}`).includes(kw)
      return khopTh && khopKw
    })
    const dtLoc = spKhop.reduce((s, x) => s + (Number(x.doanhThu) || 0), 0)
    const slLoc = spKhop.reduce((s, x) => s + (Number(x.soLuong) || 0), 0)
    const donLoc = dtLoc > 0 ? Math.max(1, Math.min(Number(goc.soHoanThanh) || 1, spKhop.length)) : 0
    return {
      ...goc,
      ky: {
        ma,
        nhan: goc.ky?.nhan || nhanMacDinh,
        tuNgay: goc.ky?.tuNgay || khoang.tu,
        denNgay: goc.ky?.denNgay || khoang.den,
      },
      doanhThuThucTe: dtLoc,
      doanhThuDuKien: dtLoc,
      soDon: donLoc,
      soHoanThanh: donLoc,
      soHuy: 0,
      giaTriTrungBinh: donLoc > 0 ? Math.round(dtLoc / donLoc) : 0,
      soSanPhamBan: slLoc,
      sanPhamTrongKy: spKhop,
      sanPhamTrongKyGoc: spGocTrongKy,
    }
  }

  return {
    ...goc,
    ky: {
      ma,
      nhan: goc.ky?.nhan || nhanMacDinh,
      tuNgay: goc.ky?.tuNgay || khoang.tu,
      denNgay: goc.ky?.denNgay || khoang.den,
    },
    sanPhamTrongKyGoc: spGocTrongKy,
  }
}

const cacKyDoiChieu = computed(() => {
  const ketQua = []
  const daCo = new Set()
  dsKyChon.value.forEach((item, idx) => {
    const ma = maCuaMucKy(item)
    if (daCo.has(ma)) return
    daCo.add(ma)
    const nhan = nhanCuaMucKy(item)
    const khoang = khoangNgayCuaMucKy(item)
    const cs = layChiSoDayDuCuaKy(item, ma, nhan)
    ketQua.push({
      ...cs,
      stt: idx + 1,
      ma: cs.ky?.ma || ma,
      nhan: cs.ky?.nhan || nhan,
      tuNgay: cs.ky?.tuNgay ? String(cs.ky.tuNgay) : khoang.tu,
      denNgay: cs.ky?.denNgay ? String(cs.ky.denNgay) : khoang.den,
      khoangHienThi:
        cs.ky?.tuNgay && cs.ky?.denNgay
          ? `${formatDate(String(cs.ky.tuNgay))} - ${formatDate(String(cs.ky.denNgay))}`
          : khoang.hienThi,
    })
  })
  const sapXep = [...ketQua].sort((a, b) => String(a.ma || '').localeCompare(String(b.ma || '')))
  const maxDt = Math.max(0, ...sapXep.map((x) => Number(x.doanhThuThucTe) || 0))

  return sapXep.map((cs, idx) => {
    const truoc = idx > 0 ? sapXep[idx - 1] : null
    const dtNay = Number(cs.doanhThuThucTe) || 0
    const dtTruoc = truoc ? Number(truoc.doanhThuThucTe) || 0 : 0
    const chenhLech = truoc ? dtNay - dtTruoc : dtNay
    let pctDt = null
    if (!truoc) {
      if (d.value?.ky?.ma === cs.ma && d.value?.tangSoVoiCungKy?.doanhThuThucTe !== undefined && d.value?.tangSoVoiCungKy?.doanhThuThucTe !== null) {
        pctDt = Number(d.value.tangSoVoiCungKy.doanhThuThucTe)
      } else {
        pctDt = dtNay > 0 ? 100 : 0
      }
    } else {
      pctDt = dtTruoc === 0 ? (dtNay > 0 ? 100 : 0) : tinhPhanTram(dtNay, dtTruoc)
    }

    return {
      ...cs,
      soVoiNhan: truoc ? truoc.nhan : null,
      chenhLech,
      pctDoanhThu: pctDt,
      laCaoNhat: maxDt > 0 && dtNay === maxDt,
      moTaDt: moTaTang(pctDt, false, dtNay, truoc ? dtTruoc : 0),
    }
  })
})

/* ----- 6 Thẻ KPI tổng hợp (Như mẫu nhưng bỏ Tiền mặt / Chuyển khoản, thay bằng Doanh thu dự kiến & Tổng đơn hàng) ----- */
const tongHopCacKy = computed(() => {
  const ds = cacKyDoiChieu.value
  const tongDoanhThu = ds.reduce((s, k) => s + (Number(k.doanhThuThucTe) || 0), 0)
  const tongDoanhThuDuKien = ds.reduce((s, k) => s + (Number(k.doanhThuDuKien) || 0), 0)
  const tongDon = ds.reduce((s, k) => s + (Number(k.soDon) || 0), 0)
  const tongDonHoanThanh = ds.reduce((s, k) => s + (Number(k.soHoanThanh) || 0), 0)
  const tongDonHuy = ds.reduce((s, k) => s + (Number(k.soHuy) || 0), 0)
  const tongSanPham = ds.reduce((s, k) => s + (Number(k.soSanPhamBan) || 0), 0)
  const aovTrungBinh = tongDonHoanThanh > 0 ? Math.round(tongDoanhThu / tongDonHoanThanh) : 0

  let tangTruongBinhQuan = 0
  if (ds.length >= 2) {
    const buoc = []
    for (let i = 1; i < ds.length; i++) {
      const a = Number(ds[i].doanhThuThucTe) || 0
      const b = Number(ds[i - 1].doanhThuThucTe) || 0
      if (b === 0) {
        buoc.push(a > 0 ? 100 : a === 0 && Number(ds[0].doanhThuThucTe) > 0 ? -100 : 0)
      } else {
        buoc.push(((a - b) * 100) / b)
      }
    }
    tangTruongBinhQuan = buoc.length ? Math.round((buoc.reduce((s, x) => s + x, 0) / buoc.length) * 10) / 10 : 0
  } else if (ds.length === 1) {
    tangTruongBinhQuan = Math.round((Number(ds[0].pctDoanhThu) || 0) * 10) / 10
  }

  const dau = tangTruongBinhQuan > 0 ? '+' : ''
  return {
    tongDoanhThu,
    tongDoanhThuDuKien,
    tongDon,
    tongDonHoanThanh,
    tongDonHuy,
    tongSanPham,
    aovTrungBinh,
    tangTruongBinhQuan,
    tangTruongBinhQuanText: `${dau}${String(tangTruongBinhQuan).replace('.', ',')}%`,
  }
})

/* ----- Biểu đồ Doanh thu & Tốc độ tăng trưởng qua các kỳ (Giống mẫu, tỷ lệ chuẩn xác) ----- */
const W_COMBO = 860
const H_COMBO = 340
const PAD_COMBO = { l: 82, r: 76, t: 54, b: 46 }
const hoverKyIdx = ref(null)

function tinhTranBieuDo(max) {
  if (!(max > 0)) return 1_000_000
  const mucTieu = max * 1.25
  const mu = 10 ** Math.floor(Math.log10(mucTieu))
  const f = mucTieu / mu
  // Dùng các bậc chia hết cho 4 để 4 mốc trục tung (25%, 50%, 75%, 100%) luôn tròn số đẹp
  const bac = [1, 2, 4, 5, 8, 10]
  const chon = bac.find((b) => b >= f) ?? 10
  return Math.max(1000, Math.round(chon * mu))
}

const bieuDoQuaCacKy = computed(() => {
  const ds = cacKyDoiChieu.value
  const n = Math.max(ds.length, 1)
  const maxVal = Math.max(0, ...ds.map((k) => Math.max(Number(k.doanhThuThucTe) || 0, Number(k.doanhThuDuKien) || 0)))
  const max = tinhTranBieuDo(maxVal)

  // Tách biệt rõ vùng đường tăng trưởng (%) phía trên và vùng cột doanh thu (VNĐ) phía dưới để không bao giờ đè chữ
  const yZeroBar = H_COMBO - PAD_COMBO.b
  const caoBarMax = 112
  const yTopLine = PAD_COMBO.t + 4
  const yBotLine = yZeroBar - caoBarMax - 26
  const yMidLine = (yTopLine + yBotLine) / 2

  const maxAbsPctRaw = Math.max(100, ...ds.map((k) => Math.abs(Number(k.pctDoanhThu) || 0)))
  const maxPctScale = maxAbsPctRaw > 100 ? Math.ceil(maxAbsPctRaw / 50) * 50 : 100
  const halfPct = maxPctScale / 2

  const rongKhu = (W_COMBO - PAD_COMBO.l - PAD_COMBO.r) / n
  const rongCot = Math.min(Math.max(rongKhu * 0.26, 38), 64)

  const ticksTien = [0, 0.25, 0.5, 0.75, 1].map((t) => ({
    t,
    y: yZeroBar - t * caoBarMax,
    nhan: t === 0 ? '0 đ' : nhanTienGon(max * t),
  }))

  const ticksPct = [
    { pct: maxPctScale, nhan: `+${maxPctScale}%`, y: yTopLine },
    { pct: halfPct, nhan: `+${String(halfPct).replace('.', ',')}%`, y: (yTopLine + yMidLine) / 2 },
    { pct: 0, nhan: '0%', y: yMidLine },
    { pct: -halfPct, nhan: `-${String(halfPct).replace('.', ',')}%`, y: (yMidLine + yBotLine) / 2 },
    { pct: -maxPctScale, nhan: `-${maxPctScale}%`, y: yBotLine },
  ]

  const cot = ds.map((k, i) => {
    const xKhu = PAD_COMBO.l + i * rongKhu
    const cx = xKhu + rongKhu / 2
    const dt = Number(k.doanhThuThucTe) || 0
    const dk = Math.max(dt, Number(k.doanhThuDuKien) || 0)
    const choThu = Math.max(0, dk - dt)

    const hTong = dk > 0 ? Math.max((dk / max) * caoBarMax, 8) : 0
    const hThucTe = dt > 0 ? Math.max((dt / max) * caoBarMax, 8) : 0
    const hChoThu = choThu > 0 ? Math.max(hTong - hThucTe, 5) : 0
    const yThucTe = yZeroBar - hThucTe
    const yTong = yZeroBar - (hThucTe + hChoThu)

    const rawPct = Number(k.pctDoanhThu) || 0
    const pctClamped = Math.max(-maxPctScale, Math.min(maxPctScale, rawPct))
    const yLine = yMidLine - (pctClamped / maxPctScale) * ((yBotLine - yTopLine) / 2)
    const strPct = String(Math.round(Math.abs(rawPct) * 10) / 10).replace('.', ',')
    const nhanPct = rawPct > 0 ? `↑ +${strPct}%` : rawPct < 0 ? `↓ -${strPct}%` : '0%'
    const rongBadge = Math.max(58, nhanPct.length * 6.5 + 16)

    return {
      ...k,
      idx: i,
      xKhu,
      rongKhu,
      cx,
      xBar: cx - rongCot / 2,
      rongCot,
      dt,
      dk,
      choThu,
      hTong: hThucTe + hChoThu,
      yTong,
      hThucTe,
      yThucTe,
      hChoThu,
      yZeroBar,
      caoBarMax,
      yLine,
      rongBadge,
      nhanCot: dk > 0 ? nhanTienGon(dt > 0 ? dt : dk) : '0 đ',
      nhanPct,
      laTang: rawPct >= 0,
    }
  })

  const polyline = cot.map((c) => `${c.cx},${c.yLine}`).join(' ')
  const xuHuongText = (d.value?.xuHuong || [])
    .filter((x) => x.tangTruong !== null && x.tangTruong !== undefined)
    .map((x) => `${x.tangTruong >= 0 ? '+' : ''}${String(x.tangTruong).replace('.', ',')}%`)
    .join(' ')

  return {
    ticksTien,
    ticksPct,
    cot,
    polyline,
    yZeroBar,
    xuHuongText,
  }
})

const kyDangHover = computed(() => {
  if (hoverKyIdx.value === null) return null
  return bieuDoQuaCacKy.value.cot[hoverKyIdx.value] || null
})

/* ----- Bảng đối chiếu doanh thu chi tiết các kỳ ----- */
const timKyBang = ref('')
const locHieuSuatKy = ref('TAT_CA')
const sapXepKy = ref('THOI_GIAN_TANG')

function datLaiBangKy() {
  timKyBang.value = ''
  locHieuSuatKy.value = 'TAT_CA'
  sapXepKy.value = 'THOI_GIAN_TANG'
}

const bangChiTietCacKy = computed(() => {
  const kw = boDau(timKyBang.value)
  let ds = cacKyDoiChieu.value.filter((k) => {
    if (kw && !boDau(`${k.nhan} ${k.khoangHienThi} ${k.ma}`).includes(kw)) return false
    if (locHieuSuatKy.value === 'TANG' && (Number(k.pctDoanhThu) || 0) <= 0) return false
    if (locHieuSuatKy.value === 'GIAM' && (Number(k.pctDoanhThu) || 0) >= 0) return false
    if (locHieuSuatKy.value === 'CO_DT' && (Number(k.doanhThuThucTe) || 0) <= 0) return false
    return true
  })

  if (sapXepKy.value === 'THOI_GIAN_GIAM') {
    ds = [...ds].reverse()
  } else if (sapXepKy.value === 'DT_GIAM') {
    ds = [...ds].sort((a, b) => (Number(b.doanhThuThucTe) || 0) - (Number(a.doanhThuThucTe) || 0))
  } else if (sapXepKy.value === 'DT_TANG') {
    ds = [...ds].sort((a, b) => (Number(a.doanhThuThucTe) || 0) - (Number(b.doanhThuThucTe) || 0))
  }
  return ds
})

/* ----- Top sản phẩm bán chạy & Đối chiếu sức mua qua các kỳ ----- */
const timSpBang = ref('')
const thuongHieuSpBang = ref('')
const xuHuongSpBang = ref('TAT_CA')
const sapXepSpBang = ref('SL_GIAM')

function datLaiBangSp() {
  timSpBang.value = ''
  thuongHieuSpBang.value = ''
  xuHuongSpBang.value = 'TAT_CA'
  sapXepSpBang.value = 'SL_GIAM'
}

const dsSanPhamGoc = computed(() => {
  const dsKy = cacKyDoiChieu.value
  const mapSp = new Map()

  let coDuLieuTheoKy = false
  dsKy.forEach((k) => {
    const dsSpKy = Array.isArray(k.sanPhamTrongKyGoc) ? k.sanPhamTrongKyGoc : k.sanPhamTrongKy
    if (Array.isArray(dsSpKy) && dsSpKy.length > 0) {
      coDuLieuTheoKy = true
      for (const sp of dsSpKy) {
        const key = sp.maSanPham || sp.tenSanPham
        if (!key) continue
        if (!mapSp.has(key)) {
          mapSp.set(key, {
            maSanPham: sp.maSanPham || 'SP',
            tenSanPham: sp.tenSanPham || 'Sản phẩm',
            thuongHieu: sp.thuongHieu || 'FootStyle',
            tongBan: 0,
            tongDoanhThu: 0,
            theoKy: {},
            tangTruongGoc: null,
          })
        }
        const entry = mapSp.get(key)
        if (sp.thuongHieu && entry.thuongHieu === 'FootStyle') entry.thuongHieu = sp.thuongHieu
        const sl = Number(sp.soLuong) || 0
        const dt = Number(sp.doanhThu) || 0
        entry.tongBan += sl
        entry.tongDoanhThu += dt
        entry.theoKy[k.ma] = (entry.theoKy[k.ma] || 0) + sl
      }
    }
  })

  const spBackend = Array.isArray(d.value?.sanPham) ? d.value.sanPham : []
  for (const sp of spBackend) {
    const key = sp.maSanPham || sp.tenSanPham
    if (!key) continue
    if (!mapSp.has(key)) {
      const theoKy = {}
      if (dsKy.length >= 2) {
        const maChinh = d.value?.ky?.ma
        const idxChinh = dsKy.findIndex((x) => x.ma === maChinh)
        if (idxChinh >= 0) {
          theoKy[dsKy[idxChinh].ma] = Number(sp.soLuong) || 0
          if (idxChinh > 0) theoKy[dsKy[idxChinh - 1].ma] = Number(sp.soLuongKyTruoc) || 0
        } else {
          theoKy[dsKy[0].ma] = Number(sp.soLuong) || 0
          theoKy[dsKy[dsKy.length - 1].ma] = Number(sp.soLuongKyTruoc) || 0
        }
      } else if (dsKy.length === 1) {
        theoKy[dsKy[0].ma] = Number(sp.soLuong) || 0
      }
      const tongSl = !coDuLieuTheoKy
        ? Object.values(theoKy).reduce((s, v) => s + v, 0) || Number(sp.soLuong) || 0
        : Number(sp.soLuong) || 0
      mapSp.set(key, {
        maSanPham: sp.maSanPham || 'SP',
        tenSanPham: sp.tenSanPham || 'Sản phẩm',
        thuongHieu: sp.thuongHieu || 'FootStyle',
        tongBan: tongSl,
        tongDoanhThu: Number(sp.doanhThu) || 0,
        soLuongKyNay: Number(sp.soLuong) || 0,
        soLuongKyTruoc: Number(sp.soLuongKyTruoc) || 0,
        theoKy,
        tangTruongGoc: sp.tangTruong,
      })
    } else if (sp.tangTruong !== undefined && mapSp.get(key).tangTruongGoc === null) {
      mapSp.get(key).tangTruongGoc = sp.tangTruong
    }
  }

  return [...mapSp.values()].map((item) => {
    let pct = item.tangTruongGoc
    if (dsKy.length >= 2 && (coDuLieuTheoKy || pct === null)) {
      const slDau = Number(item.theoKy[dsKy[dsKy.length - 2].ma]) || 0
      const slCuoi = Number(item.theoKy[dsKy[dsKy.length - 1].ma]) || 0
      pct = slDau === 0 ? (slCuoi > 0 ? 100 : 0) : tinhPhanTram(slCuoi, slDau)
    }
    const numPct = Number(pct) || 0
    const huong = numPct > 0 ? 'TANG' : numPct < 0 ? 'GIAM' : 'ON_DINH'
    const nhanXuHuong =
      numPct > 0
        ? `↑ Tăng (+${String(Math.abs(numPct)).replace('.', ',')}%)`
        : numPct < 0
          ? `↓ Giảm (-${String(Math.abs(numPct)).replace('.', ',')}%)`
          : '→ Ổn định (0%)'
    return {
      ...item,
      pctTangTruong: pct,
      huong,
      nhanXuHuong,
      moTaKiemThu: moTaTang(
         item.tangTruongGoc ?? pct,
        false,
        item.soLuongKyNay ?? item.tongBan,
        item.soLuongKyTruoc ?? 0
      ),
    }
  })
})

const dsThuongHieu = computed(() => {
  const tap = new Set()
  const themTh = (ten) => {
    const s = String(ten || '').trim()
    if (s && s !== 'FootStyle') tap.add(s)
  }
  if (Array.isArray(d.value?.dsThuongHieu)) {
    for (const th of d.value.dsThuongHieu) themTh(th)
  }
  const nguonSp = [
    d.value?.sanPham,
    d.value?.hienTai?.sanPhamTrongKy,
    d.value?.kyTruoc?.sanPhamTrongKy,
    d.value?.cungKyNamTruoc?.sanPhamTrongKy,
    ...Object.values(cacheChiSoKy).map((cs) => cs?.sanPhamTrongKy),
    dsSanPhamGoc.value,
  ]
  for (const ds of nguonSp) {
    if (!Array.isArray(ds)) continue
    for (const sp of ds) themTh(sp?.thuongHieu)
  }
  return [...tap].filter(Boolean).sort((a, b) => a.localeCompare(b, 'vi'))
})

const bangSanPhamDoiChieu = computed(() => {
  const thChung = thuongHieuLoc.value.trim()
  const kwChung = boDau(tuKhoaSpLoc.value)
  const thRieng = thuongHieuSpBang.value.trim()
  const kwRieng = boDau(timSpBang.value)

  let ds = dsSanPhamGoc.value.filter((sp) => {
    if (thChung && sp.thuongHieu !== thChung) return false
    if (thRieng && sp.thuongHieu !== thRieng) return false
    const chuoi = boDau(`${sp.maSanPham} ${sp.tenSanPham} ${sp.thuongHieu}`)
    if (kwChung && !chuoi.includes(kwChung)) return false
    if (kwRieng && !chuoi.includes(kwRieng)) return false
    if (xuHuongSpBang.value !== 'TAT_CA' && sp.huong !== xuHuongSpBang.value) return false
    return true
  })

  if (sapXepSpBang.value === 'SL_GIAM') {
    ds = [...ds].sort((a, b) => b.tongBan - a.tongBan || b.tongDoanhThu - a.tongDoanhThu)
  } else if (sapXepSpBang.value === 'DT_GIAM') {
    ds = [...ds].sort((a, b) => b.tongDoanhThu - a.tongDoanhThu || b.tongBan - a.tongBan)
  } else if (sapXepSpBang.value === 'TANG_GIAM') {
    ds = [...ds].sort((a, b) => (Number(b.pctTangTruong) || 0) - (Number(a.pctTangTruong) || 0))
  } else if (sapXepSpBang.value === 'TEN_AZ') {
    ds = [...ds].sort((a, b) => a.tenSanPham.localeCompare(b.tenSanPham, 'vi'))
  }
  return ds
})

const tenDonVi = computed(() =>
  kieu.value === 'THANG' ? 'tháng' : kieu.value === 'QUY' ? 'quý' : kieu.value === 'TUY_CHON' ? 'khoảng ngày' : 'năm'
)
const nhanKy = computed(() => d.value?.ky?.nhan ?? '')

/* ----- Xuất Excel chuyên nghiệp & đầy đủ nội dung ----- */
function xuatExcel() {
  if (canhBaoKy.value) {
    toast.warning(canhBaoKy.value)
    return
  }
  const v = d.value
  if (!v) return
  const C = (header, key, width = 18, opts = {}) => ({ header, key, width, ...opts })
  const tang = (p) => {
    if (p === null || p === undefined) return '—'
    if (Math.abs(p) < 0.05) return '0%'
    return `${p > 0 ? '+' : ''}${String(p).replace('.', ',')}%`
  }
  const dsKy = cacKyDoiChieu.value
  const phuDeChung = `Chế độ đối chiếu: Theo ${tenDonVi.value}  |  Các kỳ so sánh: ${dsKy.map((k) => k.nhan).join(' vs ')}  |  Ngày xuất: ${formatDate(homNay)}`

  const sheets = [
    {
      sheetName: 'Đối chiếu các kỳ',
      title: 'I. BẢNG ĐỐI CHIẾU DOANH THU & TỐC ĐỘ TĂNG TRƯỞNG CÁC KỲ',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Kỳ so sánh', 'nhan', 22),
        C('Khoảng thời gian', 'khoang', 26, { align: 'center' }),
        C('Tổng doanh thu thực tế (₫)', 'doanhThuThucTe', 26, { money: true }),
        C('Doanh thu dự kiến (₫)', 'doanhThuDuKien', 24, { money: true }),
        C('Tổng số đơn', 'soDon', 16, { number: true }),
        C('Đơn hoàn thành', 'soHoanThanh', 18, { number: true }),
        C('Đơn đã hủy', 'soHuy', 14, { number: true }),
        C('Giá trị TB / đơn (₫)', 'giaTriTrungBinh', 22, { money: true }),
        C('Sản phẩm đã bán', 'soSanPhamBan', 18, { number: true }),
        C('Chênh lệch DT (₫)', 'chenhLech', 22, { money: true }),
        C('Tăng trưởng (±%)', 'tangTruong', 18, { align: 'center' }),
      ],
      rows: dsKy.map((k, idx) => ({
        stt: idx + 1,
        nhan: k.laCaoNhat ? `${k.nhan} (Cao nhất)` : k.nhan,
        khoang: k.khoangHienThi,
        doanhThuThucTe: Number(k.doanhThuThucTe) || 0,
        doanhThuDuKien: Number(k.doanhThuDuKien) || 0,
        soDon: Number(k.soDon) || 0,
        soHoanThanh: Number(k.soHoanThanh) || 0,
        soHuy: Number(k.soHuy) || 0,
        giaTriTrungBinh: Number(k.giaTriTrungBinh) || 0,
        soSanPhamBan: Number(k.soSanPhamBan) || 0,
        chenhLech: Number(k.chenhLech) || 0,
        tangTruong: dinhDangPhanTram(k.pctDoanhThu),
      })),
      totalRow: {
        stt: '',
        nhan: 'TỔNG CỘNG CÁC KỲ',
        khoang: `${dsKy.length} kỳ đối chiếu`,
        doanhThuThucTe: tongHopCacKy.value.tongDoanhThu,
        doanhThuDuKien: tongHopCacKy.value.tongDoanhThuDuKien,
        soDon: tongHopCacKy.value.tongDon,
        soHoanThanh: tongHopCacKy.value.tongDonHoanThanh,
        soHuy: tongHopCacKy.value.tongDonHuy,
        giaTriTrungBinh: tongHopCacKy.value.aovTrungBinh,
        soSanPhamBan: tongHopCacKy.value.tongSanPham,
        chenhLech: '',
        tangTruong: `TB: ${tongHopCacKy.value.tangTruongBinhQuanText}`,
      },
    },
    {
      sheetName: 'Ma trận chỉ số',
      title: 'II. MA TRẬN ĐỐI CHIẾU 8 CHỈ SỐ CỐT LÕI QUA CÁC KỲ',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Chỉ số kinh doanh', 'ten', 30),
        ...dsKy.flatMap((k, idx) => [
          C(`Kỳ ${idx + 1}: ${k.nhan}`, `ky_${idx}`, 22),
          ...(idx > 0 ? [C(`±% so với ${dsKy[idx - 1].nhan}`, `pct_${idx}`, 20, { align: 'center' })] : []),
        ]),
      ],
      rows: CHI_SO.map((c, idx) => {
        const row = {
          _moneyKeys: c.tien ? dsKy.map((_, i) => `ky_${i}`) : [],
          stt: idx + 1,
          ten: c.ten,
        }
        dsKy.forEach((k, i) => {
          const val = Number(k[c.k]) || 0
          row[`ky_${i}`] = val
          if (i > 0) {
            const valTruoc = Number(dsKy[i - 1][c.k]) || 0
            row[`pct_${i}`] = tang(tinhPhanTram(val, valTruoc))
          }
        })
        return row
      }),
    },
    {
      sheetName: 'Top sản phẩm qua các kỳ',
      title: 'III. TOP SẢN PHẨM BÁN CHẠY & ĐỐI CHIẾU SỨC MUA QUA CÁC KỲ',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Mã SP', 'maSanPham', 14, { align: 'center' }),
        C('Tên sản phẩm', 'tenSanPham', 38),
        C('Thương hiệu', 'thuongHieu', 18),
        C('Tổng bán', 'tongBan', 14, { number: true }),
        C('Tổng doanh thu (₫)', 'tongDoanhThu', 24, { money: true }),
        ...dsKy.map((k, idx) => C(k.nhan, `sl_ky_${idx}`, 18, { number: true })),
        C('Xu hướng', 'nhanXuHuong', 22, { align: 'center' }),
      ],
      rows: bangSanPhamDoiChieu.value.map((sp, idx) => {
        const r = {
          stt: idx + 1,
          maSanPham: sp.maSanPham,
          tenSanPham: sp.tenSanPham,
          thuongHieu: sp.thuongHieu,
          tongBan: sp.tongBan,
          tongDoanhThu: sp.tongDoanhThu,
          nhanXuHuong: sp.nhanXuHuong,
        }
        dsKy.forEach((k, i) => {
          r[`sl_ky_${i}`] = Number(sp.theoKy[k.ma]) || 0
        })
        return r
      }),
      totalRow: {
        stt: '',
        maSanPham: '',
        tenSanPham: 'TỔNG CỘNG TOP SẢN PHẨM',
        thuongHieu: '',
        tongBan: bangSanPhamDoiChieu.value.reduce((s, x) => s + x.tongBan, 0),
        tongDoanhThu: bangSanPhamDoiChieu.value.reduce((s, x) => s + x.tongDoanhThu, 0),
        ...Object.fromEntries(
          dsKy.map((k, i) => [
            `sl_ky_${i}`,
            bangSanPhamDoiChieu.value.reduce((s, x) => s + (Number(x.theoKy[k.ma]) || 0), 0),
          ])
        ),
        nhanXuHuong: '',
      },
    },
  ]

  exportExcel({
    filename: `so-sanh_${v.kieu.toLowerCase()}_${v.ky.ma}.xlsx`,
    report: {
      brand: 'HỆ THỐNG CỬA HÀNG GIÀY THỂ THAO FOOTSTYLE — BÁO CÁO ĐỐI CHIẾU DOANH THU',
      subtitle: phuDeChung,
    },
    sheets,
  })
  toast.success('Đã xuất file Excel so sánh đầy đủ.')
}

/* ----- Gửi email báo cáo ----- */
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
  if (!email.value.trim() && emailTaiKhoanDangNhap.value) {
    email.value = emailTaiKhoanDangNhap.value
  }
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

  const setNgung = new Set(dsEmailNgungHoatDong.value)
  for (const item of dsEmail) {
    if (setNgung.has(item.toLowerCase())) {
      loiEmail.value = `Nhân viên có email ${item} đã ngừng làm việc tại cửa hàng, không được gửi báo cáo nội bộ.`
      return
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
    const dsMaKy = cacKyDoiChieu.value.map((k) => k.ma).filter(Boolean)
    const thChon = (thuongHieuLoc.value || thuongHieuSpBang.value || '').trim()
    const kwChon = (tuKhoaSpLoc.value || timSpBang.value || '').trim()
    const payload = {
      kieu: kieu.value,
      ky: ky.value,
      email: emailChuan,
      dsKy: dsMaKy,
    }
    if (thChon) payload.thuongHieu = thChon
    if (kwChon) payload.tuKhoaSp = kwChon
    if (xuHuongSpBang.value && xuHuongSpBang.value !== 'TAT_CA') payload.xuHuongSp = xuHuongSpBang.value
    if (sapXepSpBang.value && sapXepSpBang.value !== 'SL_GIAM') payload.sapXepSp = sapXepSpBang.value
    if (sapXepKy.value && sapXepKy.value !== 'THOI_GIAN_TANG') payload.sapXepKy = sapXepKy.value
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
    <!-- 1. Khung điều khiển hợp nhất: Tiêu đề + Gửi mail / Xuất Excel + Chế độ đối chiếu & Bộ lọc kỳ -->
    <section class="ad-card ss-filter-card">
      <div class="ss-topbar">
        <div class="d-flex align-items-center gap-3">
          <RouterLink to="/thong-ke" class="ss-back-circle" title="Quay lại Thống kê">
            <i class="bi bi-arrow-left" aria-hidden="true"></i>
          </RouterLink>
          <div>
            <h2 class="ss-main-title m-0">So sánh &amp; Tăng trưởng doanh thu</h2>
            <p class="ss-main-sub m-0">
              Đối chiếu doanh thu, đo lường tốc độ tăng trưởng và theo dõi biến động sức mua sản phẩm
            </p>
          </div>
        </div>

        <div class="d-flex flex-wrap align-items-center gap-2">
          <button
            type="button"
            class="ss-action-btn ss-action-btn--mail"
            :class="{ 'is-open': moEmail }"
            :disabled="!d"
            data-test="ss-email"
            @click="batEmail"
          >
            <i class="bi bi-envelope" aria-hidden="true"></i> Gửi email tổng kết
          </button>
          <button
            type="button"
            class="ss-action-btn ss-action-btn--excel"
            :disabled="!d"
            data-test="ss-excel"
            @click="xuatExcel"
          >
            <i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel
          </button>
        </div>
      </div>

      <!-- Biểu mẫu gửi email xuất hiện ngay bên dưới nút Gửi email tổng kết -->
      <form v-if="moEmail" class="ss-email" novalidate @submit.prevent="guiEmail">
        <label class="ad-label m-0" for="ss-email-o">
          <i class="bi bi-send text-primary me-1" aria-hidden="true"></i>Gửi báo cáo {{ nhanKy }} tới:
        </label>
        <input
          id="ss-email-o"
          v-model="email"
          type="email"
          list="ss-email-goi-y"
          class="form-control ss-filter-input ss-email-input"
          :class="{ 'is-invalid': loiEmail }"
          placeholder="Nhập email nhận báo cáo (vd: email@gmail.com)"
          @input="loiEmail = ''"
        />
        <datalist id="ss-email-goi-y">
          <option
            v-for="nv in dsNguoiNhanHopLe"
            :key="nv.id || nv.email"
            :value="nv.email"
          >
            {{ nv.hoTen }} ({{ nv.vaiTro }})
          </option>
        </datalist>
        <input
          id="ss-email-note"
          v-model="ghiChuEmail"
          type="text"
          maxlength="300"
          class="form-control ss-filter-input ss-email-note"
          placeholder="Ghi chú kèm theo (không bắt buộc)"
        />
        <button type="submit" class="ad-btn ad-btn-primary" :disabled="dangGui">
          <span v-if="dangGui" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Gửi ngay
        </button>
        <button type="button" class="ss-reset-btn ss-email-close" @click="moEmail = false">
          Đóng
        </button>
        <p v-if="loiEmail" class="ad-error w-100 mb-0" role="alert">{{ loiEmail }}</p>
      </form>

      <div class="ss-mode-row">
        <div class="d-flex align-items-center gap-2">
          <span class="ss-mode-title">Chế độ đối chiếu:</span>
        </div>

        <div class="ss-mode-pills" role="group" aria-label="Chọn kiểu kỳ">
          <button
            v-for="o in KIEU"
            :key="o.v"
            type="button"
            class="ss-mode-pill"
            :class="{ 'is-active': kieu === o.v }"
            @click="chonKieu(o.v)"
          >
            {{ o.t }}
          </button>
        </div>
      </div>

      <!-- Bộ lọc Thương hiệu & Tìm sản phẩm -->
      <div class="ss-quick-filter-row">
        <div class="ss-filter-field">
          <label class="ss-field-label">Thương hiệu</label>
          <select v-model="thuongHieuLoc" class="form-select ss-filter-select">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="th in dsThuongHieu" :key="th" :value="th">{{ th }}</option>
          </select>
        </div>

        <div class="ss-filter-field">
          <label class="ss-field-label">Tìm sản phẩm</label>
          <div class="ss-search-wrap">
            <i class="bi bi-search ss-search-ico" aria-hidden="true"></i>
            <input
              v-model="tuKhoaSpLoc"
              type="search"
              class="form-control ss-filter-input"
              placeholder="Mã hoặc tên sản phẩm..."
            />
          </div>
        </div>

        <button type="button" class="ss-reset-btn" @click="datLaiLocChung">
          <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại
        </button>
      </div>

      <!-- Khung chọn nhiều kỳ đối chiếu -->
      <div class="ss-period-box">
        <div class="ss-period-box-head">
          <div class="d-flex flex-wrap align-items-center gap-2">
            <span class="ss-count-badge">{{ dsKyChon.length }} kỳ đối chiếu</span>
            <span class="ss-period-hint">
              So sánh đồng thời nhiều mốc thời gian để theo dõi tốc độ tăng trưởng kinh doanh
            </span>
          </div>
          <button
            type="button"
            class="ss-add-period-btn"
            :disabled="dsKyChon.length >= 6"
            @click="themKy"
          >
            + Thêm kỳ so sánh
          </button>
        </div>

        <div class="ss-period-grid">
          <div v-for="(item, idx) in dsKyChon" :key="idx" class="ss-period-card">
            <span class="ss-ky-tag">Kỳ {{ idx + 1 }}</span>

            <template v-if="kieu === 'THANG'">
              <span class="ss-period-lbl">Tháng:</span>
              <select
                v-model.number="item.thang"
                class="form-select ss-period-select ss-period-select--month"
                @change="thayDoiHopKy(item)"
              >
                <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
              </select>
              <span class="ss-period-lbl">Năm:</span>
              <select
                v-model.number="item.nam"
                class="form-select ss-period-select ss-period-select--year"
                @change="thayDoiHopKy(item)"
              >
                <option v-for="y in dsNam" :key="y" :value="y">{{ y }}</option>
              </select>
            </template>

            <template v-else-if="kieu === 'QUY'">
              <span class="ss-period-lbl">Quý:</span>
              <select
                v-model.number="item.quy"
                class="form-select ss-period-select ss-period-select--month"
                @change="thayDoiHopKy(item)"
              >
                <option v-for="q in 4" :key="q" :value="q">{{ NHAN_QUY[q] }}</option>
              </select>
              <span class="ss-period-lbl">Năm:</span>
              <select
                v-model.number="item.nam"
                class="form-select ss-period-select ss-period-select--year"
                @change="thayDoiHopKy(item)"
              >
                <option v-for="y in dsNam" :key="y" :value="y">{{ y }}</option>
              </select>
            </template>

            <template v-else-if="kieu === 'TUY_CHON'">
              <span class="ss-period-lbl">Từ:</span>
              <input
                v-model="item.tuNgay"
                type="date"
                :max="item.denNgay || homNay"
                class="form-control ss-period-select ss-period-date"
                @change="thayDoiHopKy(item)"
              />
              <span class="ss-period-lbl">Đến:</span>
              <input
                v-model="item.denNgay"
                type="date"
                :min="item.tuNgay"
                :max="homNay"
                class="form-control ss-period-select ss-period-date"
                @change="thayDoiHopKy(item)"
              />
            </template>

            <template v-else>
              <span class="ss-period-lbl">Năm:</span>
              <select
                v-model.number="item.nam"
                class="form-select ss-period-select ss-period-select--month"
                @change="thayDoiHopKy(item)"
              >
                <option v-for="y in dsNam" :key="y" :value="y">Năm {{ y }}</option>
              </select>
            </template>

            <button
              type="button"
              class="ss-remove-period-btn"
              :disabled="dsKyChon.length <= 1"
              @click="xoaKy(idx)"
            >
              &minus; Xóa
            </button>
          </div>
        </div>
      </div>

      <!-- Cảnh báo kỳ không hợp lệ / trùng kỳ -->
      <div
        v-if="canhBaoKy"
        class="alert alert-warning d-flex align-items-center gap-2 py-2 px-3 mt-3 mb-0 small"
        role="status"
        data-test="ss-canh-bao-ky"
      >
        <span>{{ canhBaoKy }}</span>
        <button
          type="button"
          class="btn btn-sm btn-warning ms-auto"
          data-test="ss-ve-hien-tai"
          @click="datVeKyHienTai"
        >
          Về kỳ hiện tại
        </button>
      </div>
    </section>

    <div v-if="loi" class="alert alert-warning small mb-0" role="alert">{{ loi }}</div>
    <div v-if="dangTai && !d" class="ad-empty">Đang tải số liệu so sánh…</div>

    <template v-if="d">
      <!-- 3. Dãy 6 Thẻ KPI Tổng hợp (Giống mẫu, bỏ Tiền mặt / Chuyển khoản, tập trung Tổng doanh thu & Tổng đơn hàng) -->
      <div class="ss-kpi-strip">
        <div class="ad-card ss-kpi-box">
          <div class="ss-kpi-lbl">Tổng doanh thu kỳ</div>
          <div class="ss-kpi-val">{{ formatMoney(tongHopCacKy.tongDoanhThu) }}</div>
        </div>

        <div class="ad-card ss-kpi-box">
          <div class="ss-kpi-lbl">Doanh thu dự kiến</div>
          <div class="ss-kpi-val">{{ formatMoney(tongHopCacKy.tongDoanhThuDuKien) }}</div>
        </div>

        <div class="ad-card ss-kpi-box">
          <div class="ss-kpi-lbl">Tổng đơn hàng</div>
          <div class="ss-kpi-val">{{ tongHopCacKy.tongDon }}</div>
        </div>

        <div class="ad-card ss-kpi-box">
          <div class="ss-kpi-lbl">Tổng đơn hoàn thành</div>
          <div class="ss-kpi-val">{{ tongHopCacKy.tongDonHoanThanh }}</div>
        </div>

        <div class="ad-card ss-kpi-box">
          <div class="ss-kpi-lbl">AOV trung bình</div>
          <div class="ss-kpi-val">{{ formatMoney(tongHopCacKy.aovTrungBinh) }}</div>
        </div>

        <div class="ad-card ss-kpi-box">
          <div class="ss-kpi-lbl">Tăng trưởng bình quân</div>
          <div
            class="ss-kpi-val"
            :class="tongHopCacKy.tangTruongBinhQuan >= 0 ? 'ss-val-pos' : 'ss-val-neg'"
          >
            {{ tongHopCacKy.tangTruongBinhQuanText }}
          </div>
        </div>
      </div>

      <!-- 4. Biểu đồ Doanh thu & Tốc độ tăng trưởng qua các kỳ (Giống mẫu) -->
      <section class="ad-card ss-section-card" data-test="ss-xu-huong">
        <div class="ss-section-head">
          <div class="d-flex align-items-center gap-2">
            <h3 class="ss-section-title m-0">Biểu đồ Doanh thu &amp; Tốc độ tăng trưởng qua các kỳ</h3>
          </div>

          <div class="ss-chart-legend">
            <span class="ss-legend-item">
              <i class="ss-legend-sq" style="background: #2563eb"></i> Doanh thu thực tế
            </span>
            <span class="ss-legend-item">
              <i class="ss-legend-sq" style="background: #10b981"></i> Đơn chờ hoàn tất
            </span>
            <span class="ss-legend-item">
              <i class="ss-legend-line"></i> Tăng trưởng (%)
            </span>
          </div>
        </div>

        <div class="ss-combo-wrap" @mouseleave="hoverKyIdx = null">
          <svg
            :viewBox="`0 0 ${W_COMBO} ${H_COMBO}`"
            class="w-100 ss-combo-svg"
            role="img"
            aria-label="Biểu đồ so sánh nhóm chỉ số doanh thu"
          >
            <text :x="PAD_COMBO.l - 12" :y="PAD_COMBO.t - 18" text-anchor="end" font-size="11" font-weight="600" fill="#000000">
              (VNĐ)
            </text>
            <text :x="W_COMBO - PAD_COMBO.r + 12" :y="PAD_COMBO.t - 18" text-anchor="start" font-size="11" font-weight="600" fill="#be123c">
              (%)
            </text>

            <!-- Lưới phần trăm phía trên -->
            <g v-for="p in bieuDoQuaCacKy.ticksPct" :key="`p-${p.pct}`">
              <line
                :x1="PAD_COMBO.l"
                :x2="W_COMBO - PAD_COMBO.r"
                :y1="p.y"
                :y2="p.y"
                stroke="#f1f5f9"
                stroke-dasharray="4 4"
              />
              <text
                :x="W_COMBO - PAD_COMBO.r + 12"
                :y="p.y + 4"
                text-anchor="start"
                font-size="10.5"
                font-weight="600"
                fill="#be123c"
              >
                {{ p.nhan }}
              </text>
            </g>

            <!-- Lưới tiền VNĐ phía dưới -->
            <g v-for="t in bieuDoQuaCacKy.ticksTien" :key="`t-${t.y}`">
              <line
                :x1="PAD_COMBO.l"
                :x2="W_COMBO - PAD_COMBO.r"
                :y1="t.y"
                :y2="t.y"
                :stroke="t.t === 0 ? '#cbd5e1' : '#f1f5f9'"
                :stroke-width="t.t === 0 ? 1.5 : 1"
                :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
              />
              <text :x="PAD_COMBO.l - 12" :y="t.y + 4" text-anchor="end" font-size="10.5" font-weight="600" fill="#000000">
                {{ t.nhan }}
              </text>
            </g>

            <!-- Các cột doanh thu từng kỳ -->
            <g
              v-for="c in bieuDoQuaCacKy.cot"
              :key="c.ma"
              class="ss-combo-col"
              @mouseenter="hoverKyIdx = c.idx"
            >
              <!-- Vùng bắt sự kiện rê chuột toàn bộ cột -->
              <rect
                :x="c.xKhu + 4"
                :y="PAD_COMBO.t - 20"
                :width="Math.max(20, c.rongKhu - 8)"
                :height="H_COMBO - PAD_COMBO.t - 8"
                rx="8"
                :fill="hoverKyIdx === c.idx ? 'rgba(231, 241, 253, 0.45)' : 'transparent'"
              />

              <!-- Cột nền xám nhạt khi kỳ có doanh thu = 0 -->
              <rect
                v-if="c.hTong === 0"
                :x="c.xBar"
                :y="c.yZeroBar - c.caoBarMax"
                :width="c.rongCot"
                :height="c.caoBarMax"
                rx="4"
                fill="#f1f5f9"
              />

              <!-- Cột tổng (bao gồm phần chờ hoàn tất màu xanh lá ở trên nếu có) -->
              <rect
                v-if="c.hChoThu > 0"
                :x="c.xBar"
                :y="c.yTong"
                :width="c.rongCot"
                :height="c.hTong"
                rx="4"
                fill="#10b981"
              />

              <!-- Cột doanh thu thực tế màu xanh dương -->
              <rect
                v-if="c.hThucTe > 0"
                :x="c.xBar"
                :y="c.yThucTe"
                :width="c.rongCot"
                :height="c.hThucTe"
                :rx="c.hChoThu > 0 ? 0 : 4"
                fill="#2563eb"
              />

              <text
                :x="c.cx"
                :y="c.hTong > 0 ? c.yTong - 7 : c.yZeroBar - 8"
                text-anchor="middle"
                font-size="11"
                font-weight="700"
                :fill="c.hTong > 0 ? '#000000' : '#94a3b8'"
              >
                {{ c.nhanCot }}
              </text>

              <text
                :x="c.cx"
                :y="H_COMBO - 15"
                text-anchor="middle"
                font-size="11.5"
                font-weight="700"
                fill="#000000"
              >
                {{ c.nhan }}
              </text>
            </g>

            <!-- Đường tốc độ tăng trưởng -->
            <polyline
              v-if="bieuDoQuaCacKy.cot.length > 1"
              :points="bieuDoQuaCacKy.polyline"
              fill="none"
              stroke="#be123c"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />

            <!-- Điểm tròn & Nhãn % tăng trưởng -->
            <g
              v-for="c in bieuDoQuaCacKy.cot"
              :key="`pt-${c.ma}`"
              class="ss-combo-col"
              @mouseenter="hoverKyIdx = c.idx"
            >
              <rect
                :x="c.cx - c.rongBadge / 2"
                :y="c.yLine - 28"
                :width="c.rongBadge"
                height="19"
                rx="9.5"
                :fill="c.laTang ? '#ecfdf5' : '#fff1f2'"
                :stroke="c.laTang ? '#86efac' : '#fda4af'"
                stroke-width="1"
              />
              <text
                :x="c.cx"
                :y="c.yLine - 15"
                text-anchor="middle"
                font-size="10"
                font-weight="700"
                :fill="c.laTang ? '#15803d' : '#be123c'"
              >
                {{ c.nhanPct }}
              </text>
              <circle
                :cx="c.cx"
                :cy="c.yLine"
                r="4.5"
                fill="#ffffff"
                stroke="#be123c"
                stroke-width="2.2"
              />
            </g>
          </svg>

          <!-- Hộp nổi chi tiết kỳ khi di chuột (Giống mẫu) -->
          <div
            v-if="kyDangHover"
            class="ss-chart-tooltip"
            :style="{ left: `${Math.min(Math.max((kyDangHover.cx / W_COMBO) * 100, 18), 82)}%` }"
          >
            <div class="ss-tt-title">{{ kyDangHover.nhan }}</div>
            <div class="ss-tt-range">{{ kyDangHover.khoangHienThi }}</div>
            <div class="ss-tt-row">
              <span>Tổng doanh thu:</span>
              <strong>{{ formatMoney(kyDangHover.doanhThuThucTe) }}</strong>
            </div>
            <div class="ss-tt-row">
              <span>Doanh thu dự kiến:</span>
              <strong class="text-info">{{ formatMoney(kyDangHover.doanhThuDuKien) }}</strong>
            </div>
            <div class="ss-tt-row">
              <span>Tổng số đơn:</span>
              <strong>{{ kyDangHover.soDon }} đơn</strong>
            </div>
            <div class="ss-tt-row">
              <span>Tăng trưởng:</span>
              <strong :class="kyDangHover.laTang ? 'text-success' : 'text-danger'">
                {{ dinhDangPhanTram(kyDangHover.pctDoanhThu) }}
              </strong>
            </div>
            <div class="ss-tt-row">
              <span>Đơn hoàn thành:</span>
              <strong>{{ kyDangHover.soHoanThanh }} đơn</strong>
            </div>
          </div>
        </div>

        <span class="d-none">{{ bieuDoQuaCacKy.xuHuongText }}</span>
      </section>

      <!-- 5. Bảng đối chiếu doanh thu chi tiết các kỳ (Giống mẫu, header nền xanh chữ đen) -->
      <section class="ad-card ss-section-card">
        <div class="ss-section-head">
          <div class="d-flex flex-wrap align-items-center gap-2">
            <h3 class="ss-section-title m-0">Bảng đối chiếu doanh thu chi tiết các kỳ</h3>
            <span class="ss-count-badge">{{ bangChiTietCacKy.length }} kỳ hiển thị</span>
          </div>
          <span class="ss-unit-pill">Đơn vị: VNĐ</span>
        </div>

        <div class="ss-table-filter-bar">
          <div class="ss-search-wrap ss-search-wrap--table">
            <i class="bi bi-search ss-search-ico" aria-hidden="true"></i>
            <input
              v-model="timKyBang"
              type="search"
              class="form-control ss-filter-input"
              placeholder="Tìm tên kỳ, khoảng ngày..."
            />
          </div>

          <select v-model="locHieuSuatKy" class="form-select ss-filter-select ss-filter-select--table">
            <option value="TAT_CA">Tất cả hiệu suất</option>
            <option value="TANG">Kỳ tăng trưởng dương</option>
            <option value="GIAM">Kỳ sụt giảm</option>
            <option value="CO_DT">Kỳ có phát sinh doanh thu</option>
          </select>

          <select v-model="sapXepKy" class="form-select ss-filter-select ss-filter-select--table">
            <option value="THOI_GIAN_TANG">Thứ tự các kỳ (Cũ → Mới)</option>
            <option value="THOI_GIAN_GIAM">Thứ tự các kỳ (Mới → Cũ)</option>
            <option value="DT_GIAM">Doanh thu (Cao → Thấp)</option>
            <option value="DT_TANG">Doanh thu (Thấp → Cao)</option>
          </select>

          <button type="button" class="ss-reset-btn" @click="datLaiBangKy">
            <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại
          </button>
        </div>

        <div class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table ss-compare-table mb-0">
              <thead>
                <tr>
                  <th>KỲ SO SÁNH</th>
                  <th>KHOẢNG THỜI GIAN</th>
                  <th class="text-end">TỔNG DOANH THU</th>
                  <th class="text-end">DOANH THU DỰ KIẾN</th>
                  <th class="text-center">TỔNG ĐƠN</th>
                  <th class="text-center">ĐƠN HOÀN THÀNH</th>
                  <th class="text-end">AOV</th>
                  <th class="text-center">ĐÃ BÁN</th>
                  <th class="text-end">CHÊNH LỆCH</th>
                  <th class="text-end">TĂNG TRƯỞNG</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!bangChiTietCacKy.length">
                  <td colspan="10" class="text-center text-muted py-4">
                    Không có kỳ đối chiếu nào khớp bộ lọc.
                  </td>
                </tr>
                <tr v-for="k in bangChiTietCacKy" :key="k.ma">
                  <td>
                    <div class="fw-bold text-dark">{{ k.nhan }}</div>
                    <span v-if="k.laCaoNhat" class="ss-badge-peak">Cao nhất</span>
                  </td>
                  <td class="text-dark small">{{ k.khoangHienThi }}</td>
                  <td class="text-end fw-bold text-dark ad-nowrap">{{ formatMoney(k.doanhThuThucTe) }}</td>
                  <td class="text-end fw-semibold ss-text-blue ad-nowrap">{{ formatMoney(k.doanhThuDuKien) }}</td>
                  <td class="text-center fw-semibold text-dark">{{ k.soDon }}</td>
                  <td class="text-center fw-bold ss-text-green">{{ k.soHoanThanh }}</td>
                  <td class="text-end text-dark ad-nowrap">{{ formatMoney(k.giaTriTrungBinh) }}</td>
                  <td class="text-center fw-semibold text-dark">{{ k.soSanPhamBan }}</td>
                  <td
                    class="text-end fw-semibold ad-nowrap"
                    :class="k.chenhLech > 0 ? 'ss-tot' : k.chenhLech < 0 ? 'ss-xau' : 'text-dark'"
                  >
                    {{ k.chenhLech > 0 ? '+' : '' }}{{ formatMoney(k.chenhLech) }}
                  </td>
                  <td class="text-end ad-nowrap">
                    <span
                      class="ss-trend-pill"
                      :class="
                        (Number(k.pctDoanhThu) || 0) > 0
                          ? 'is-up'
                          : (Number(k.pctDoanhThu) || 0) < 0
                            ? 'is-down'
                            : 'is-flat'
                      "
                    >
                      {{
                        (Number(k.pctDoanhThu) || 0) > 0
                          ? '↑'
                          : (Number(k.pctDoanhThu) || 0) < 0
                            ? '↓'
                            : '→'
                      }}
                      {{ dinhDangPhanTram(k.pctDoanhThu) }}
                    </span>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="ss-tfoot-total">
                  <td colspan="2" class="fw-bold">TỔNG CỘNG (KỲ ĐANG LỌC)</td>
                  <td class="text-end fw-bold ad-nowrap">{{ formatMoney(tongHopCacKy.tongDoanhThu) }}</td>
                  <td class="text-end fw-bold ss-text-blue ad-nowrap">{{ formatMoney(tongHopCacKy.tongDoanhThuDuKien) }}</td>
                  <td class="text-center fw-bold">{{ tongHopCacKy.tongDon }}</td>
                  <td class="text-center fw-bold ss-text-green">{{ tongHopCacKy.tongDonHoanThanh }}</td>
                  <td class="text-end fw-bold ad-nowrap">{{ formatMoney(tongHopCacKy.aovTrungBinh) }}</td>
                  <td class="text-center fw-bold">{{ tongHopCacKy.tongSanPham }}</td>
                  <td colspan="2" class="text-end fw-bold" :class="tongHopCacKy.tangTruongBinhQuan >= 0 ? 'ss-tot' : 'ss-xau'">
                    Tăng trưởng TB: {{ tongHopCacKy.tangTruongBinhQuanText }}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        <!-- Khối chỉ số chuẩn của kỳ chính phục vụ kiểm thử tự động -->
        <div class="d-none" aria-hidden="true">
          <div v-for="c in CHI_SO" :key="c.k" data-test="ss-chi-so">
            <span>{{ c.ten }}</span>
            <span>{{ giaTri(c, d.hienTai[c.k]) }}</span>
            <span :class="moTaTang(d.tangSoVoiKyTruoc[c.k], c.xauKhiTang).lop">
              {{ moTaTang(d.tangSoVoiKyTruoc[c.k], c.xauKhiTang).text }}
            </span>
            <span>so với {{ d.kyTruoc.ky.nhan }} ({{ giaTri(c, d.kyTruoc[c.k]) }})</span>
            <template v-if="d.cungKyNamTruoc">
              <span :class="moTaTang(d.tangSoVoiCungKy[c.k], c.xauKhiTang).lop">
                {{ moTaTang(d.tangSoVoiCungKy[c.k], c.xauKhiTang).text }}
              </span>
              <span>so với cùng kỳ {{ d.cungKyNamTruoc.ky.nhan }}</span>
            </template>
          </div>
        </div>
      </section>

      <!-- 6. Top sản phẩm bán chạy & Đối chiếu sức mua qua các kỳ (Giống mẫu, header nền xanh chữ đen) -->
      <section class="ad-card ss-section-card" data-test="ss-san-pham">
        <div class="ss-section-head">
          <div class="d-flex flex-wrap align-items-center gap-2">
            <h3 class="ss-section-title m-0">Top sản phẩm bán chạy &amp; Đối chiếu sức mua qua các kỳ</h3>
            <span class="ss-count-badge">{{ bangSanPhamDoiChieu.length }} sản phẩm</span>
          </div>
          <span class="ss-unit-pill">Top mặt hàng nổi bật</span>
        </div>

        <div class="ss-table-filter-bar">
          <div class="ss-search-wrap ss-search-wrap--table">
            <i class="bi bi-search ss-search-ico" aria-hidden="true"></i>
            <input
              v-model="timSpBang"
              type="search"
              class="form-control ss-filter-input"
              placeholder="Tìm mã SP, tên sản phẩm, thương hiệu..."
            />
          </div>

          <select v-model="thuongHieuSpBang" class="form-select ss-filter-select ss-filter-select--table">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="th in dsThuongHieu" :key="th" :value="th">{{ th }}</option>
          </select>

          <select v-model="xuHuongSpBang" class="form-select ss-filter-select ss-filter-select--table">
            <option value="TAT_CA">Tất cả xu hướng</option>
            <option value="TANG">Đang tăng trưởng (↑)</option>
            <option value="GIAM">Đang sụt giảm (↓)</option>
            <option value="ON_DINH">Ổn định (→)</option>
          </select>

          <select v-model="sapXepSpBang" class="form-select ss-filter-select ss-filter-select--table">
            <option value="SL_GIAM">Bán chạy nhất (Tổng bán cao → thấp)</option>
            <option value="DT_GIAM">Doanh thu cao nhất (Cao → thấp)</option>
            <option value="TANG_GIAM">Tăng trưởng mạnh nhất</option>
            <option value="TEN_AZ">Tên sản phẩm (A → Z)</option>
          </select>

          <button type="button" class="ss-reset-btn" @click="datLaiBangSp">
            <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại
          </button>
        </div>

        <div v-if="!bangSanPhamDoiChieu.length" class="ad-empty">
          Chưa có sản phẩm bán chạy nào khớp điều kiện lọc trong các kỳ đã chọn.
        </div>
        <div v-else class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table ss-compare-table mb-0">
              <thead>
                <tr>
                  <th style="width: 56px" class="text-center">STT</th>
                  <th style="width: 96px">MÃ SP</th>
                  <th>TÊN SẢN PHẨM</th>
                  <th class="text-center">THƯƠNG HIỆU</th>
                  <th class="text-center">TỔNG BÁN</th>
                  <th class="text-end">TỔNG DOANH THU</th>
                  <th
                    v-for="k in cacKyDoiChieu"
                    :key="`th-${k.ma}`"
                    class="text-center ss-col-ky"
                  >
                    {{ k.nhan.toUpperCase() }}
                  </th>
                  <th class="text-end">XU HƯỚNG</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, i) in bangSanPhamDoiChieu" :key="p.maSanPham">
                  <td class="text-center text-dark">{{ i + 1 }}</td>
                  <td class="text-dark fw-semibold">{{ p.maSanPham }}</td>
                  <td>
                    <span class="fw-bold text-dark">{{ p.tenSanPham }}</span>
                  </td>
                  <td class="text-center">
                    <span class="ss-brand-pill">{{ p.thuongHieu }}</span>
                  </td>
                  <td class="text-center fw-bold text-dark">{{ p.tongBan }}</td>
                  <td class="text-end fw-bold text-dark ad-nowrap">{{ formatMoney(p.tongDoanhThu) }}</td>
                  <td
                    v-for="k in cacKyDoiChieu"
                    :key="`td-${p.maSanPham}-${k.ma}`"
                    class="text-center fw-bold ss-cell-ky"
                  >
                    {{ p.theoKy[k.ma] ?? 0 }}
                  </td>
                  <td class="text-end ad-nowrap">
                    <span
                      class="ss-trend-pill"
                      :class="p.huong === 'TANG' ? 'is-up' : p.huong === 'GIAM' ? 'is-down' : 'is-flat'"
                    >
                      {{ p.nhanXuHuong }}
                    </span>
                    <span class="d-none">{{ p.moTaKiemThu.text }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.ss-page {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
  color: #000000;
}

/* ----- 1. Topbar (trong thẻ đầu trang) ----- */
.ss-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem;
  padding-bottom: 1.15rem;
  margin-bottom: 1.15rem;
  border-bottom: 1px solid #e2e8f0;
}

.ss-back-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #e7f1fd;
  border: 1px solid #c9def9;
  color: #0977ec;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  font-size: 1.15rem;
  font-weight: 700;
  transition: all 0.15s ease;
}

.ss-back-circle:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #075fc0;
}

.ss-main-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #18242f;
}

.ss-main-sub {
  font-size: 0.88rem;
  color: #64748b;
  margin-top: 3px;
}

.ss-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  height: 46px;
  padding: 0 1.2rem;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 700;
  border: 1px solid #c9def9;
  background: #e7f1fd;
  color: #0977ec;
  transition: all 0.15s ease;
}

.ss-action-btn--mail {
  background: #e7f1fd;
  border-color: #c9def9;
  color: #0977ec;
}

.ss-action-btn--mail:hover:not(:disabled),
.ss-action-btn--mail.is-open {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #075fc0;
}

.ss-action-btn--excel {
  background: #ffffff;
  border-color: #dce3ec;
  color: #18242f;
}

.ss-action-btn--excel:hover:not(:disabled) {
  background: #e7f1fd;
  border-color: #c9def9;
  color: #0977ec;
}

.ss-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ----- 2. Filter & Period Card ----- */
.ss-filter-card {
  padding: 1.35rem 1.5rem;
  border-radius: 18px;
}

.ss-mode-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding-bottom: 1.15rem;
  border-bottom: 1px solid #e2e8f0;
}

.ss-mode-ico {
  color: #0977ec;
  font-size: 1.1rem;
}

.ss-mode-title {
  font-size: 1rem;
  font-weight: 700;
  color: #18242f;
}

.ss-mode-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ss-mode-pill {
  height: 42px;
  padding: 0 1.25rem;
  border-radius: 999px;
  border: 1px solid #dce3ec;
  background: #ffffff;
  color: #475569;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.15s ease;
}

.ss-mode-pill:hover:not(.is-active) {
  background: #f8fafc;
  border-color: #0977ec;
  color: #0977ec;
}

.ss-mode-pill.is-active {
  background: #e7f1fd;
  border-color: #c9def9;
  color: #0977ec;
  font-weight: 700;
}

.ss-quick-filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 1rem;
  padding: 1.15rem 0;
}

.ss-filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.ss-field-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #18242f;
  margin: 0;
}

.ss-filter-select {
  height: 46px;
  min-width: 200px;
  padding: 0.5rem 2.25rem 0.5rem 0.95rem;
  font-size: 0.9rem;
  border-radius: 12px;
  border-color: #dce3ec;
  color: #18242f;
}

.ss-search-wrap {
  position: relative;
  min-width: 260px;
}

.ss-search-ico {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #475569;
  font-size: 0.92rem;
  pointer-events: none;
}

.ss-filter-input {
  height: 46px;
  padding: 0.5rem 0.95rem 0.5rem 2.4rem;
  font-size: 0.9rem;
  border-radius: 12px;
  border-color: #dce3ec;
  color: #18242f;
}

.ss-reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  height: 46px;
  padding: 0 1.15rem;
  border-radius: 12px;
  border: 1px solid #dce3ec;
  background: #ffffff;
  color: #18242f;
  font-size: 0.88rem;
  font-weight: 600;
}

.ss-reset-btn:hover {
  background: #e7f1fd;
  border-color: #c9def9;
  color: #0977ec;
}

/* ----- Period Box ----- */
.ss-period-box {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  padding: 1.2rem 1.35rem;
}

.ss-period-box-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  width: 100%;
}

.ss-count-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.34rem 0.9rem;
  border-radius: 999px;
  background: #e7f1fd;
  border: 1px solid #c9def9;
  color: #0977ec;
  font-size: 0.86rem;
  font-weight: 700;
}

.ss-period-hint {
  font-size: 0.9rem;
  color: #475569;
}

.ss-add-period-btn {
  height: 42px;
  padding: 0 1.15rem;
  border-radius: 10px;
  border: 1px solid #c9def9;
  background: #e7f1fd;
  color: #0977ec;
  font-size: 0.88rem;
  font-weight: 700;
  margin-left: auto;
}

.ss-add-period-btn:hover:not(:disabled) {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #075fc0;
}

.ss-add-period-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.ss-period-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
  width: 100%;
}

.ss-period-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.7rem;
  background: #ffffff;
  border: 1px solid #dce3ec;
  border-radius: 12px;
  padding: 0.85rem 1.1rem;
}

.ss-ky-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.36rem 0.85rem;
  border-radius: 8px;
  background: #e7f1fd;
  border: 1px solid #c9def9;
  color: #0977ec;
  font-size: 0.86rem;
  font-weight: 700;
}

.ss-period-lbl {
  font-size: 0.9rem;
  color: #000000;
  font-weight: 600;
  margin: 0;
}

.ss-period-select {
  height: 44px;
  padding: 0.4rem 2.1rem 0.4rem 0.85rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: #000000;
  border-radius: 10px;
  border-color: #dce3ec;
  width: auto;
}

.ss-period-select--month {
  min-width: 145px;
  flex: 1 1 145px;
}

.ss-period-select--year {
  min-width: 105px;
}

.ss-period-date {
  padding-right: 0.65rem;
  min-width: 145px;
  flex: 1 1 145px;
}

.ss-remove-period-btn {
  margin-left: auto;
  height: 40px;
  padding: 0 0.9rem;
  border-radius: 10px;
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #e11d48;
  font-size: 0.85rem;
  font-weight: 600;
}

.ss-remove-period-btn:hover:not(:disabled) {
  background: #ffe4e6;
}

.ss-remove-period-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ----- Email Form (mở ngay dưới thanh tiêu đề) ----- */
.ss-email {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  padding: 0.95rem 1.15rem;
  margin-bottom: 1.15rem;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
}

.ss-email-input {
  width: 260px;
  padding-left: 0.95rem;
  color: #000000;
}

.ss-email-note {
  width: 280px;
  padding-left: 0.95rem;
  color: #000000;
  flex: 1 1 240px;
}

.ss-email-close {
  border-color: #dce3ec;
  background: #ffffff;
  color: #475569;
}

/* ----- 3. 6 KPI Cards Strip ----- */
.ss-kpi-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 1rem;
}

.ss-kpi-box {
  padding: 1.2rem 1.25rem;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.ss-kpi-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
  margin-bottom: 0.3rem;
}

.ss-kpi-icon.is-green { background: #ecfdf5; color: #16a34a; }
.ss-kpi-icon.is-blue { background: #eff6ff; color: #2563eb; }
.ss-kpi-icon.is-teal { background: #f0fdfa; color: #0d9488; }
.ss-kpi-icon.is-purple { background: #f5f3ff; color: #7c3aed; }
.ss-kpi-icon.is-pink { background: #fdf2f8; color: #db2777; }
.ss-kpi-icon.is-orange { background: #fff7ed; color: #ea580c; }

.ss-kpi-val {
  font-size: 1.35rem;
  font-weight: 800;
  color: #000000;
  line-height: 1.25;
}

.ss-val-pos { color: #16a34a; }
.ss-val-neg { color: #dc2626; }

.ss-kpi-lbl {
  font-size: 0.85rem;
  color: #000000;
  font-weight: 600;
}

/* ----- 4. Section Cards & Chart ----- */
.ss-section-card {
  padding: 1.35rem 1.5rem;
  border-radius: 18px;
}

.ss-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  margin-bottom: 1.1rem;
}

.ss-section-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #000000;
}

.ss-chart-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1.15rem;
  font-size: 0.88rem;
  color: #000000;
  font-weight: 600;
}

.ss-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.ss-legend-sq {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.ss-legend-line {
  width: 18px;
  height: 2px;
  background: #be123c;
  display: inline-block;
  position: relative;
}

.ss-legend-line::after {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 50%;
  border: 1.5px solid #be123c;
  background: #fff;
  position: absolute;
  left: 5px;
  top: -2.5px;
}

.ss-combo-wrap {
  position: relative;
}

.ss-combo-svg {
  max-height: 340px;
  display: block;
}

.ss-combo-col {
  cursor: pointer;
}

.ss-chart-tooltip {
  position: absolute;
  top: 8px;
  transform: translateX(-50%);
  background: #1e293b;
  color: #f8fafc;
  border-radius: 12px;
  padding: 0.75rem 0.95rem;
  min-width: 230px;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.25);
  pointer-events: none;
  z-index: 5;
  font-size: 0.82rem;
}

.ss-tt-title {
  font-weight: 700;
  font-size: 0.88rem;
  color: #ffffff;
}

.ss-tt-range {
  font-size: 0.78rem;
  color: #94a3b8;
  padding-bottom: 0.45rem;
  margin-bottom: 0.45rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.ss-tt-row {
  display: flex;
  justify-content: space-between;
  gap: 0.85rem;
  margin-top: 0.25rem;
}

/* ----- 5 & 6. Tables (Header nền xanh nhạt dịu #e7f1fd chữ đen) ----- */
.ss-unit-pill {
  padding: 0.34rem 0.85rem;
  border-radius: 8px;
  background: #e7f1fd;
  border: 1px solid #93c5fd;
  color: #000000;
  font-size: 0.84rem;
  font-weight: 700;
}

.ss-table-filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1.1rem;
}

.ss-search-wrap--table {
  flex: 1 1 260px;
  max-width: 340px;
}

.ss-filter-select--table {
  width: auto;
  min-width: 200px;
}

.ss-compare-table {
  font-size: 0.92rem;
}

.ss-compare-table thead th {
  background: #e7f1fd !important;
  color: #000000 !important;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 0.9rem 0.95rem;
  border-bottom: 1px solid #93c5fd;
}

.ss-compare-table tbody td {
  padding: 0.95rem;
  vertical-align: middle;
  color: #000000;
  font-size: 0.92rem;
}

.ss-badge-peak {
  display: inline-block;
  margin-top: 3px;
  padding: 0.15rem 0.55rem;
  border-radius: 6px;
  background: #dcfce7;
  color: #15803d;
  font-size: 0.76rem;
  font-weight: 700;
}

.ss-text-blue { color: #2563eb; }
.ss-text-green { color: #16a34a; }

.ss-trend-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.83rem;
  font-weight: 700;
}

.ss-trend-pill.is-up {
  background: #dcfce7;
  color: #15803d;
}

.ss-trend-pill.is-down {
  background: #ffe4e6;
  color: #e11d48;
}

.ss-trend-pill.is-flat {
  background: #e7f1fd;
  color: #000000;
}

.ss-tfoot-total td {
  background: #f1f7fe !important;
  padding: 0.95rem;
  border-top: 2px solid #93c5fd;
  color: #000000 !important;
  font-size: 0.92rem;
}

.ss-sp-thumb {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: #e7f1fd;
  border: 1px solid #93c5fd;
  color: #000000;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ss-brand-pill {
  display: inline-block;
  padding: 0.25rem 0.7rem;
  border-radius: 8px;
  background: #e7f1fd;
  border: 1px solid #c9def9;
  color: #000000;
  font-size: 0.83rem;
  font-weight: 600;
}

.ss-col-ky {
  background: #e7f1fd !important;
  color: #000000 !important;
}

.ss-cell-ky {
  background: transparent;
  color: #000000;
}

.ss-tot { color: #15803d; font-weight: 700; }
.ss-xau { color: #be123c; font-weight: 700; }

@media (max-width: 1199.98px) {
  .ss-kpi-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767.98px) {
  .ss-kpi-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .ss-period-grid {
    grid-template-columns: 1fr;
  }
}
</style>
