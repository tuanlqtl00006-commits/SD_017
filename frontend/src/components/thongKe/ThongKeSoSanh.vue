<script setup>
// So sánh doanh thu giữa các kỳ (tháng / quý / năm / khoảng ngày tùy chọn).
// Kỳ mới nhất trong danh sách là kỳ đang xét, được so với kỳ liền trước: thẻ tóm tắt, biểu đồ,
// bảng đối chiếu chỉ số, doanh thu theo thương hiệu và sản phẩm bán chạy qua các kỳ.
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

/* ----- Bộ lọc thương hiệu (áp dụng cho toàn trang, Excel và email) ----- */
const thuongHieuLoc = ref('')

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
        tongGiamGia: 0,
        soDon: 0,
        soHoanThanh: 0,
        soHuy: 0,
        tyLeHuy: 0,
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

/* ----- Danh mục chỉ số so sánh ----- */
// the: hiện trên hàng thẻ tóm tắt; theoHoaDon: tính theo cả hóa đơn nên ẩn khi đang lọc thương hiệu / sản phẩm;
// xauKhiTang: tăng là xấu (tô đỏ); trungTinh: không tô màu tốt / xấu.
const CHI_SO = [
  { k: 'doanhThuThucTe', ten: 'Doanh thu', tien: true, the: true, icon: 'bi-cash-coin', tone: 'blue' },
  { k: 'soHoanThanh', ten: 'Đơn hoàn thành', the: true, icon: 'bi-check2-circle', tone: 'green' },
  { k: 'giaTriTrungBinh', ten: 'Giá trị TB / đơn', tien: true, the: true, icon: 'bi-receipt-cutoff', tone: 'sky' },
  { k: 'soSanPhamBan', ten: 'Sản phẩm bán ra', the: true, icon: 'bi-box-seam', tone: 'indigo' },
  { k: 'soKhachMua', ten: 'Khách hàng mua', the: true, icon: 'bi-people', tone: 'amber' },
  { k: 'tongGiamGia', ten: 'Tiền giảm giá', tien: true, theoHoaDon: true, trungTinh: true },
  { k: 'soDon', ten: 'Tổng đơn phát sinh', theoHoaDon: true },
  { k: 'soHuy', ten: 'Đơn hủy', theoHoaDon: true, xauKhiTang: true },
  { k: 'tyLeHuy', ten: 'Tỷ lệ hủy đơn', phanTram: true, theoHoaDon: true, xauKhiTang: true, the: true, icon: 'bi-x-circle', tone: 'rose' },
]

const coLocSanPham = computed(() => Boolean(thuongHieuLoc.value.trim()))
const chiSoHienThi = computed(() => CHI_SO.filter((c) => !coLocSanPham.value || !c.theoHoaDon))

function soThapPhan(v) {
  return String(Math.round((Number(v) || 0) * 10) / 10).replace('.', ',')
}

function tinhPhanTram(a, b) {
  const va = Number(a) || 0
  const vb = Number(b) || 0
  if (vb === 0) return va === 0 ? 0 : null
  return Math.round(((va - vb) * 1000) / vb) / 10
}

function dinhDangPhanTram(p) {
  if (p === null || p === undefined) return 'Mới phát sinh'
  const num = Math.round(Number(p) * 10) / 10
  if (num > 0) return `+${soThapPhan(num)}%`
  if (num < 0) return `-${soThapPhan(Math.abs(num))}%`
  return '0%'
}

function layGiaTri(cs, c) {
  if (!cs) return 0
  if (c.k === 'tyLeHuy') {
    if (cs.tyLeHuy !== undefined && cs.tyLeHuy !== null) return Number(cs.tyLeHuy) || 0
    const don = Number(cs.soDon) || 0
    return don > 0 ? Math.round(((Number(cs.soHuy) || 0) * 1000) / don) / 10 : 0
  }
  return Number(cs[c.k]) || 0
}

function hienThiGiaTri(c, v) {
  if (c.phanTram) return `${soThapPhan(v)}%`
  return c.tien ? formatMoney(Math.round(v)) : String(Math.round(v))
}

/** So sánh giá trị một chỉ số giữa 2 kỳ: chênh lệch, % tăng / giảm và màu tốt / xấu. */
function soSanhGiaTri(c, nay, truoc) {
  const hieu = c.phanTram ? Math.round((nay - truoc) * 10) / 10 : Math.round(nay) - Math.round(truoc)
  const dau = hieu > 0 ? '+' : hieu < 0 ? '-' : ''
  const abs = Math.abs(hieu)
  const chenhText = c.phanTram ? `${dau}${soThapPhan(abs)} điểm %` : `${dau}${c.tien ? formatMoney(abs) : abs}`
  let pct = null
  let pctText
  if (c.phanTram) {
    pctText = '—'
  } else if (Math.round(truoc) === 0) {
    pctText = Math.round(nay) === 0 ? '0%' : 'Mới phát sinh'
  } else {
    pct = tinhPhanTram(nay, truoc)
    pctText = pct === 0 ? '0%' : `${pct > 0 ? '▲' : '▼'} ${soThapPhan(Math.abs(pct))}%`
  }
  const lop = hieu === 0 || c.trungTinh ? 'is-flat' : (hieu > 0) !== Boolean(c.xauKhiTang) ? 'is-tot' : 'is-xau'
  return { hieu, chenhText, pct, pctText, bienDong: c.phanTram ? chenhText : pctText, lop }
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
        doanhThuDuKien: Number(trongXu.doanhThuDuKien) || dt,
        tongGiamGia: 0,
        soDon: sd,
        soHoanThanh: sd,
        soHuy: 0,
        tyLeHuy: 0,
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
      tongGiamGia: 0,
      soDon: 0,
      soHoanThanh: 0,
      soHuy: 0,
      tyLeHuy: 0,
      giaTriTrungBinh: 0,
      soSanPhamBan: 0,
      soKhachMua: 0,
      sanPhamTrongKy: [],
    }
  }

  const spGocTrongKy = Array.isArray(goc.sanPhamTrongKy) ? goc.sanPhamTrongKy : []
  const kyChuan = {
    ma,
    nhan: goc.ky?.nhan || nhanMacDinh,
    tuNgay: goc.ky?.tuNgay || khoang.tu,
    denNgay: goc.ky?.denNgay || khoang.den,
  }
  const th = thuongHieuLoc.value.trim()
  if (th) {
    // Chỉ tính phần doanh thu của sản phẩm thuộc thương hiệu; số đơn = số hóa đơn hoàn thành có chứa các sản phẩm đó
    const spKhop = spGocTrongKy.filter((sp) => String(sp.thuongHieu || '').trim() === th)
    const dtLoc = spKhop.reduce((s, x) => s + (Number(x.doanhThu) || 0), 0)
    const slLoc = spKhop.reduce((s, x) => s + (Number(x.soLuong) || 0), 0)
    const coMaDon = spKhop.every((sp) => Array.isArray(sp.hoaDonIds))
    const donLoc = coMaDon
      ? new Set(spKhop.flatMap((sp) => sp.hoaDonIds)).size
      : dtLoc > 0
        ? Math.max(1, Math.min(Number(goc.soHoanThanh) || 1, spKhop.length))
        : 0
    const khachLoc = coMaDon ? new Set(spKhop.flatMap((sp) => sp.khachHangIds || [])).size : 0
    return {
      ...goc,
      ky: kyChuan,
      doanhThuThucTe: dtLoc,
      doanhThuDuKien: dtLoc,
      tongGiamGia: 0,
      soDon: donLoc,
      soHoanThanh: donLoc,
      soHuy: 0,
      tyLeHuy: 0,
      giaTriTrungBinh: donLoc > 0 ? Math.round(dtLoc / donLoc) : 0,
      soSanPhamBan: slLoc,
      soKhachMua: khachLoc,
      sanPhamTrongKy: spKhop,
      sanPhamTrongKyGoc: spGocTrongKy,
    }
  }

  return { ...goc, ky: kyChuan, sanPhamTrongKyGoc: spGocTrongKy }
}

/** Các kỳ đang so sánh, sắp theo thời gian (cũ → mới), kèm doanh thu tăng / giảm so với kỳ liền trước. */
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

  return sapXep.map((cs, idx) => {
    const truoc = idx > 0 ? sapXep[idx - 1] : null
    const dtNay = Number(cs.doanhThuThucTe) || 0
    const dtTruoc = truoc ? Number(truoc.doanhThuThucTe) || 0 : 0
    return {
      ...cs,
      laKyDau: !truoc,
      soVoiNhan: truoc ? truoc.nhan : null,
      chenhLech: truoc ? dtNay - dtTruoc : 0,
      pctDoanhThu: truoc ? tinhPhanTram(dtNay, dtTruoc) : null,
    }
  })
})

const kyChinh = computed(() => cacKyDoiChieu.value[cacKyDoiChieu.value.length - 1] || null)
const kySoSanh = computed(() =>
  cacKyDoiChieu.value.length >= 2 ? cacKyDoiChieu.value[cacKyDoiChieu.value.length - 2] : null
)

/* ----- Thẻ tóm tắt: kỳ đang xét so với kỳ liền trước ----- */
const theKpi = computed(() => {
  const nay = kyChinh.value
  if (!nay) return []
  const truoc = kySoSanh.value
  return chiSoHienThi.value
    .filter((c) => c.the)
    .map((c) => {
      const vNay = layGiaTri(nay, c)
      const vTruoc = truoc ? layGiaTri(truoc, c) : 0
      return {
        ...c,
        giaTriText: hienThiGiaTri(c, vNay),
        ss: truoc ? soSanhGiaTri(c, vNay, vTruoc) : null,
      }
    })
})

function soNgayGiua(tu, den) {
  return Math.round((new Date(`${den}T12:00:00`) - new Date(`${tu}T12:00:00`)) / 86400000) + 1
}

/** Lưu ý khi 2 kỳ không cùng độ dài (kỳ đang diễn ra hoặc khoảng ngày tùy chọn lệch nhau). */
const luuYKy = computed(() => {
  const nay = kyChinh.value
  if (!nay?.tuNgay || !nay?.denNgay) return ''
  const tu = String(nay.tuNgay).slice(0, 10)
  const den = String(nay.denNgay).slice(0, 10)
  if (tu <= homNay && homNay < den) {
    return `${nay.nhan} chưa kết thúc (đã qua ${soNgayGiua(tu, homNay)}/${soNgayGiua(tu, den)} ngày), số liệu sẽ còn thay đổi đến hết kỳ.`
  }
  const truoc = kySoSanh.value
  if (kieu.value === 'TUY_CHON' && truoc?.tuNgay && truoc?.denNgay) {
    const a = soNgayGiua(tu, den)
    const b = soNgayGiua(String(truoc.tuNgay).slice(0, 10), String(truoc.denNgay).slice(0, 10))
    if (a !== b) return `Hai kỳ có độ dài khác nhau: ${nay.nhan} có ${a} ngày, ${truoc.nhan} có ${b} ngày.`
  }
  return ''
})

/* ----- Bảng đối chiếu chỉ số (dòng = chỉ số, cột = kỳ) ----- */
const bangChiSo = computed(() => {
  const ds = cacKyDoiChieu.value
  const nay = kyChinh.value
  const truoc = kySoSanh.value
  return chiSoHienThi.value.map((c) => ({
    ...c,
    cot: ds.map((k, i) => {
      const v = layGiaTri(k, c)
      return {
        ma: k.ma,
        text: hienThiGiaTri(c, v),
        ss: ds.length > 2 && i > 0 ? soSanhGiaTri(c, v, layGiaTri(ds[i - 1], c)) : null,
      }
    }),
    ss: nay && truoc ? soSanhGiaTri(c, layGiaTri(nay, c), layGiaTri(truoc, c)) : null,
  }))
})

/* ----- Doanh thu theo thương hiệu ----- */
const bangThuongHieu = computed(() => {
  const ds = cacKyDoiChieu.value
  const theoTh = new Map()
  ds.forEach((k) => {
    for (const sp of k.sanPhamTrongKy || []) {
      const ten = String(sp.thuongHieu || '').trim() || 'Khác'
      if (!theoTh.has(ten)) theoTh.set(ten, {})
      const e = theoTh.get(ten)
      e[k.ma] = (e[k.ma] || 0) + (Number(sp.doanhThu) || 0)
    }
  })
  const nay = kyChinh.value
  const truoc = kySoSanh.value
  const tongNay = nay ? [...theoTh.values()].reduce((s, e) => s + (e[nay.ma] || 0), 0) : 0
  return [...theoTh.entries()]
    .map(([ten, theoKy]) => {
      const vNay = nay ? theoKy[nay.ma] || 0 : 0
      const vTruoc = truoc ? theoKy[truoc.ma] || 0 : 0
      return {
        ten,
        theoKy,
        vNay,
        tongCacKy: Object.values(theoKy).reduce((s, x) => s + x, 0),
        tyTrong: tongNay > 0 ? `${soThapPhan((vNay * 100) / tongNay)}%` : '0%',
        ss: truoc ? soSanhGiaTri(CHI_SO[0], vNay, vTruoc) : null,
      }
    })
    .sort((a, b) => b.vNay - a.vNay || b.tongCacKy - a.tongCacKy || a.ten.localeCompare(b.ten, 'vi'))
})

/* ----- Biểu đồ doanh thu & tăng / giảm qua các kỳ ----- */
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

  // Tách biệt vùng đường tăng / giảm (%) phía trên và vùng cột doanh thu (VNĐ) phía dưới để không đè chữ
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
    { pct: halfPct, nhan: `+${soThapPhan(halfPct)}%`, y: (yTopLine + yMidLine) / 2 },
    { pct: 0, nhan: '0%', y: yMidLine },
    { pct: -halfPct, nhan: `-${soThapPhan(halfPct)}%`, y: (yMidLine + yBotLine) / 2 },
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

    // Kỳ đầu tiên không có kỳ trước để so; kỳ trước bằng 0 mà kỳ này có doanh thu thì ghi "Mới"
    const coDiem = !k.laKyDau
    const laMoi = coDiem && (k.pctDoanhThu === null || k.pctDoanhThu === undefined)
    const rawPct = laMoi ? maxPctScale : Number(k.pctDoanhThu) || 0
    const pctClamped = Math.max(-maxPctScale, Math.min(maxPctScale, rawPct))
    const yLine = yMidLine - (pctClamped / maxPctScale) * ((yBotLine - yTopLine) / 2)
    const strPct = soThapPhan(Math.abs(rawPct))
    const nhanPct = laMoi ? 'Mới' : rawPct > 0 ? `↑ +${strPct}%` : rawPct < 0 ? `↓ -${strPct}%` : '0%'
    const rongBadge = Math.max(46, nhanPct.length * 6.5 + 16)

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
      coDiem,
      yLine,
      rongBadge,
      nhanCot: dk > 0 ? nhanTienGon(dt > 0 ? dt : dk) : '0 đ',
      nhanPct,
      laTang: rawPct >= 0,
    }
  })

  const diem = cot.filter((c) => c.coDiem)
  const polyline = diem.map((c) => `${c.cx},${c.yLine}`).join(' ')
  const xuHuongText = (d.value?.xuHuong || [])
    .filter((x) => x.tangTruong !== null && x.tangTruong !== undefined)
    .map((x) => `${x.tangTruong >= 0 ? '+' : ''}${soThapPhan(x.tangTruong)}%`)
    .join(' ')

  return {
    ticksTien,
    ticksPct,
    cot,
    diem,
    polyline,
    yZeroBar,
    xuHuongText,
  }
})

const kyDangHover = computed(() => {
  if (hoverKyIdx.value === null) return null
  return bieuDoQuaCacKy.value.cot[hoverKyIdx.value] || null
})

/* ----- Sản phẩm bán chạy qua các kỳ ----- */

const dsSanPhamGoc = computed(() => {
  const dsKy = cacKyDoiChieu.value
  const mapSp = new Map()

  dsKy.forEach((k) => {
    const dsSpKy = Array.isArray(k.sanPhamTrongKyGoc) ? k.sanPhamTrongKyGoc : k.sanPhamTrongKy
    if (Array.isArray(dsSpKy) && dsSpKy.length > 0) {
      for (const sp of dsSpKy) {
        const key = sp.maSanPham || sp.tenSanPham
        if (!key) continue
        if (!mapSp.has(key)) {
          mapSp.set(key, {
            maSanPham: sp.maSanPham || 'SP',
            tenSanPham: sp.tenSanPham || 'Sản phẩm',
            thuongHieu: sp.thuongHieu || '',
            tongBan: 0,
            tongDoanhThu: 0,
            theoKy: {},
            tangTruongGoc: null,
          })
        }
        const entry = mapSp.get(key)
        if (sp.thuongHieu && !entry.thuongHieu) entry.thuongHieu = sp.thuongHieu
        const sl = Number(sp.soLuong) || 0
        entry.tongBan += sl
        entry.tongDoanhThu += Number(sp.doanhThu) || 0
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
          theoKy[dsKy[dsKy.length - 1].ma] = Number(sp.soLuong) || 0
          theoKy[dsKy[dsKy.length - 2].ma] = Number(sp.soLuongKyTruoc) || 0
        }
      } else if (dsKy.length === 1) {
        theoKy[dsKy[0].ma] = Number(sp.soLuong) || 0
      }
      mapSp.set(key, {
        maSanPham: sp.maSanPham || 'SP',
        tenSanPham: sp.tenSanPham || 'Sản phẩm',
        thuongHieu: sp.thuongHieu || '',
        tongBan: Object.values(theoKy).reduce((s, x) => s + x, 0) || Number(sp.soLuong) || 0,
        tongDoanhThu: Number(sp.doanhThu) || 0,
        theoKy,
        tangTruongGoc: sp.tangTruong ?? null,
      })
    }
  }

  return [...mapSp.values()].map((item) => {
    let pct = item.tangTruongGoc
    if (dsKy.length >= 2) {
      const slTruoc = Number(item.theoKy[dsKy[dsKy.length - 2].ma]) || 0
      const slNay = Number(item.theoKy[dsKy[dsKy.length - 1].ma]) || 0
      pct = slTruoc === 0 ? (slNay > 0 ? 100 : 0) : tinhPhanTram(slNay, slTruoc)
    }
    const numPct = Number(pct) || 0
    const huong = numPct > 0 ? 'TANG' : numPct < 0 ? 'GIAM' : 'ON_DINH'
    const nhanXuHuong =
      numPct > 0
        ? `↑ Tăng (+${soThapPhan(Math.abs(numPct))}%)`
        : numPct < 0
          ? `↓ Giảm (-${soThapPhan(Math.abs(numPct))}%)`
          : '→ Ổn định (0%)'
    return { ...item, pctTangTruong: pct, huong, nhanXuHuong }
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
  const th = thuongHieuLoc.value.trim()
  return dsSanPhamGoc.value
    .filter((sp) => sp.tongBan > 0 && (!th || String(sp.thuongHieu || '').trim() === th))
    .sort((a, b) => b.tongBan - a.tongBan || b.tongDoanhThu - a.tongDoanhThu)
    .slice(0, 20)
})

const tenDonVi = computed(() =>
  kieu.value === 'THANG' ? 'tháng' : kieu.value === 'QUY' ? 'quý' : kieu.value === 'TUY_CHON' ? 'khoảng ngày' : 'năm'
)
const nhanKy = computed(() => kyChinh.value?.nhan || d.value?.ky?.nhan || '')

/* ----- Xuất Excel: cùng nội dung và bộ lọc với trang ----- */
function xuatExcel() {
  if (canhBaoKy.value) {
    toast.warning(canhBaoKy.value)
    return
  }
  const v = d.value
  const nay = kyChinh.value
  if (!v || !nay) return
  const truoc = kySoSanh.value
  const C = (header, key, width = 18, opts = {}) => ({ header, key, width, ...opts })
  const dsKy = cacKyDoiChieu.value
  const moTaLoc = []
  if (thuongHieuLoc.value.trim()) moTaLoc.push(`Thương hiệu: ${thuongHieuLoc.value.trim()}`)
  const phuDe = [
    truoc ? `So sánh ${nay.nhan} với ${truoc.nhan}` : nay.nhan,
    `Theo ${tenDonVi.value}`,
    `Các kỳ: ${dsKy.map((k) => k.nhan).join(', ')}`,
    ...moTaLoc,
    `Ngày xuất: ${formatDate(homNay)}`,
  ].join('  |  ')
  const cotSoSanh = truoc
    ? [C('Chênh lệch', 'chenh', 20, { align: 'right' }), C('Tăng / giảm', 'tangGiam', 16, { align: 'center' })]
    : []

  const sheets = [
    {
      sheetName: 'Chỉ số các kỳ',
      title: 'I. BẢNG ĐỐI CHIẾU CHỈ SỐ CÁC KỲ',
      subtitle: phuDe,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Chỉ số', 'ten', 26),
        ...dsKy.map((k, i) => C(k.nhan, `ky_${i}`, 20, { align: 'right' })),
        ...cotSoSanh,
      ],
      rows: bangChiSo.value.map((c, idx) => {
        const row = {
          _moneyKeys: c.tien ? [...dsKy.map((_, i) => `ky_${i}`), 'chenh'] : [],
          stt: idx + 1,
          ten: c.ten,
        }
        dsKy.forEach((k, i) => {
          const val = layGiaTri(k, c)
          row[`ky_${i}`] = c.phanTram ? `${soThapPhan(val)}%` : Math.round(val)
        })
        if (c.ss) {
          row.chenh = c.phanTram ? c.ss.chenhText : c.ss.hieu
          row.tangGiam = c.ss.pctText
        }
        return row
      }),
    },
    {
      sheetName: 'Thương hiệu',
      title: 'II. DOANH THU THEO THƯƠNG HIỆU',
      subtitle: phuDe,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Thương hiệu', 'ten', 22),
        ...dsKy.map((k, i) => C(k.nhan, `ky_${i}`, 20, { money: true })),
        ...(truoc ? [C('Chênh lệch', 'chenh', 20, { money: true }), C('Tăng / giảm', 'tangGiam', 16, { align: 'center' })] : []),
        C(`Tỷ trọng ${nay.nhan}`, 'tyTrong', 18, { align: 'center' }),
      ],
      rows: bangThuongHieu.value.map((th, idx) => {
        const row = { stt: idx + 1, ten: th.ten, tyTrong: th.tyTrong }
        dsKy.forEach((k, i) => {
          row[`ky_${i}`] = th.theoKy[k.ma] || 0
        })
        if (th.ss) {
          row.chenh = th.ss.hieu
          row.tangGiam = th.ss.pctText
        }
        return row
      }),
    },
    {
      sheetName: 'Sản phẩm',
      title: 'III. SẢN PHẨM BÁN CHẠY QUA CÁC KỲ',
      subtitle: phuDe,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Mã SP', 'maSanPham', 14, { align: 'center' }),
        C('Tên sản phẩm', 'tenSanPham', 38),
        C('Thương hiệu', 'thuongHieu', 18),
        ...dsKy.map((k, idx) => C(k.nhan, `sl_ky_${idx}`, 16, { number: true })),
        C('Tổng bán', 'tongBan', 14, { number: true }),
        C('Doanh thu (₫)', 'tongDoanhThu', 22, { money: true }),
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
    },
  ]

  exportExcel({
    filename: `so-sanh_${v.kieu.toLowerCase()}_${String(nay.ma).replace(/[^A-Za-z0-9_-]/g, '_')}.xlsx`,
    report: {
      brand: 'FOOTSTYLE — BÁO CÁO SO SÁNH DOANH THU',
      subtitle: phuDe,
    },
    sheets,
  })
  toast.success('Đã xuất file Excel so sánh.')
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
    const thChon = thuongHieuLoc.value.trim()
    const payload = {
      kieu: kieu.value,
      ky: ky.value,
      email: emailChuan,
      dsKy: dsMaKy,
    }
    if (thChon) payload.thuongHieu = thChon
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
            <i class="bi bi-file-earmark-excel text-success" aria-hidden="true"></i> Xuất Excel
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

        <select
          v-model="thuongHieuLoc"
          class="form-select ss-filter-select ss-brand-select"
          aria-label="Lọc theo thương hiệu"
        >
          <option value="">Tất cả thương hiệu</option>
          <option v-for="th in dsThuongHieu" :key="th" :value="th">{{ th }}</option>
        </select>
      </div>

      <!-- Khung chọn các kỳ so sánh -->
      <div class="ss-period-box">
        <div class="ss-period-box-head">
          <span class="ss-count-badge">{{ dsKyChon.length }} kỳ so sánh</span>
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

    <template v-if="d && kyChinh">
      <!-- 2. Đang so sánh kỳ nào với kỳ nào -->
      <div class="ss-context" data-test="ss-dang-so-sanh">
        <div class="ss-context-main">
          <template v-if="kySoSanh">
            Đang so sánh <strong>{{ kyChinh.nhan }}</strong> với <strong>{{ kySoSanh.nhan }}</strong>
          </template>
          <template v-else>
            Đang xem <strong>{{ kyChinh.nhan }}</strong>. Thêm một kỳ nữa để so sánh.
          </template>
          <span v-if="coLocSanPham" class="ss-context-loc">Thương hiệu: {{ thuongHieuLoc.trim() }}</span>
        </div>
        <div v-if="luuYKy" class="ss-context-warn" data-test="ss-luu-y-ky">
          <i class="bi bi-info-circle" aria-hidden="true"></i> {{ luuYKy }}
        </div>
      </div>

      <!-- 3. Thẻ tóm tắt: kỳ đang xét so với kỳ liền trước -->
      <div class="ss-kpi-strip">
        <div
          v-for="t in theKpi"
          :key="t.k"
          class="ad-card ss-kpi-box"
          :class="`is-${t.tone}`"
          data-test="ss-kpi"
        >
          <div class="ss-kpi-top">
            <span class="ss-kpi-lbl">{{ t.ten }}</span>
            <span class="ss-kpi-icon" :class="`is-${t.tone}`" aria-hidden="true">
              <i :class="['bi', t.icon]"></i>
            </span>
          </div>
          <div class="ss-kpi-val">{{ t.giaTriText }}</div>
          <div v-if="t.ss" class="ss-kpi-cmp">
            <span class="ss-trend-pill" :class="t.ss.lop">{{ t.ss.bienDong }}</span>
          </div>
        </div>
      </div>

      <!-- 4. Biểu đồ doanh thu & tăng / giảm qua các kỳ -->
      <section class="ad-card ss-section-card" data-test="ss-xu-huong">
        <div class="ss-section-head">
          <div class="d-flex align-items-center gap-2">
            <h3 class="ss-section-title m-0">Doanh thu qua các kỳ</h3>
          </div>

          <div class="ss-chart-legend">
            <span class="ss-legend-item">
              <i class="ss-legend-sq" style="background: #0977ec"></i> Doanh thu
            </span>
            <span class="ss-legend-item">
              <i class="ss-legend-sq" style="background: #10b981"></i> Đơn đang xử lý
            </span>
            <span class="ss-legend-item">
              <i class="ss-legend-line"></i> Tăng / giảm so với kỳ trước
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
            <defs>
              <linearGradient id="ssBarBlue" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#3b82f6" />
                <stop offset="100%" stop-color="#0977ec" />
              </linearGradient>
              <linearGradient id="ssBarGreen" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#34d399" />
                <stop offset="100%" stop-color="#10b981" />
              </linearGradient>
            </defs>
            <text :x="PAD_COMBO.l - 12" :y="PAD_COMBO.t - 18" text-anchor="end" font-size="11" font-weight="600" fill="#64748b">
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
                :stroke="t.t === 0 ? '#cbd5e1' : '#e2e8f0'"
                :stroke-width="t.t === 0 ? 1.5 : 1"
                :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
              />
              <text :x="PAD_COMBO.l - 12" :y="t.y + 4" text-anchor="end" font-size="10.5" font-weight="600" fill="#64748b">
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
              <rect
                :x="c.xKhu + 4"
                :y="PAD_COMBO.t - 20"
                :width="Math.max(20, c.rongKhu - 8)"
                :height="H_COMBO - PAD_COMBO.t - 8"
                rx="8"
                :fill="hoverKyIdx === c.idx ? 'rgba(231, 241, 253, 0.5)' : 'transparent'"
              />

              <!-- Cột nền xám nhạt khi kỳ không có doanh thu -->
              <rect
                v-if="c.hTong === 0"
                :x="c.xBar"
                :y="c.yZeroBar - c.caoBarMax"
                :width="c.rongCot"
                :height="c.caoBarMax"
                rx="5"
                fill="#f1f5f9"
              />

              <!-- Phần đơn đang xử lý (chưa hoàn thành) nằm trên cột doanh thu -->
              <rect
                v-if="c.hChoThu > 0"
                :x="c.xBar"
                :y="c.yTong"
                :width="c.rongCot"
                :height="c.hTong"
                rx="5"
                fill="url(#ssBarGreen)"
              />

              <rect
                v-if="c.hThucTe > 0"
                :x="c.xBar"
                :y="c.yThucTe"
                :width="c.rongCot"
                :height="c.hThucTe"
                :rx="c.hChoThu > 0 ? 0 : 5"
                fill="url(#ssBarBlue)"
              />

              <text
                :x="c.cx"
                :y="c.hTong > 0 ? c.yTong - 7 : c.yZeroBar - 8"
                text-anchor="middle"
                font-size="11"
                font-weight="700"
                :fill="c.hTong > 0 ? '#18242f' : '#94a3b8'"
              >
                {{ c.nhanCot }}
              </text>

              <text
                :x="c.cx"
                :y="H_COMBO - 15"
                text-anchor="middle"
                font-size="11.5"
                font-weight="600"
                fill="#334155"
              >
                {{ c.nhan }}
              </text>
            </g>

            <!-- Đường tăng / giảm so với kỳ liền trước -->
            <polyline
              v-if="bieuDoQuaCacKy.diem.length > 1"
              :points="bieuDoQuaCacKy.polyline"
              fill="none"
              stroke="#be123c"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />

            <g
              v-for="c in bieuDoQuaCacKy.diem"
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

          <!-- Chi tiết kỳ khi di chuột -->
          <div
            v-if="kyDangHover"
            class="ss-chart-tooltip"
            :style="{ left: `${Math.min(Math.max((kyDangHover.cx / W_COMBO) * 100, 18), 82)}%` }"
          >
            <div class="ss-tt-title">{{ kyDangHover.nhan }}</div>
            <div class="ss-tt-range">{{ kyDangHover.khoangHienThi }}</div>
            <div class="ss-tt-row">
              <span>Doanh thu:</span>
              <strong>{{ formatMoney(kyDangHover.doanhThuThucTe) }}</strong>
            </div>
            <div class="ss-tt-row">
              <span>Đơn hoàn thành:</span>
              <strong>{{ kyDangHover.soHoanThanh }} đơn</strong>
            </div>
            <div class="ss-tt-row">
              <span>Giá trị TB / đơn:</span>
              <strong>{{ formatMoney(kyDangHover.giaTriTrungBinh) }}</strong>
            </div>
            <div v-if="kyDangHover.choThu > 0" class="ss-tt-row">
              <span>Đơn đang xử lý:</span>
              <strong class="text-info">{{ formatMoney(kyDangHover.choThu) }}</strong>
            </div>
            <div v-if="!kyDangHover.laKyDau" class="ss-tt-row">
              <span>So với {{ kyDangHover.soVoiNhan }}:</span>
              <strong :class="kyDangHover.laTang ? 'text-success' : 'text-danger'">
                {{ dinhDangPhanTram(kyDangHover.pctDoanhThu) }}
              </strong>
            </div>
          </div>
        </div>

        <span class="d-none">{{ bieuDoQuaCacKy.xuHuongText }}</span>
      </section>

      <!-- 5. Bảng đối chiếu chỉ số -->
      <section class="ad-card ss-section-card" data-test="ss-bang-chi-so">
        <div class="ss-section-head">
          <div class="d-flex flex-wrap align-items-center gap-2">
            <h3 class="ss-section-title m-0">Bảng đối chiếu chỉ số</h3>
            <span class="ss-count-badge">{{ cacKyDoiChieu.length }} kỳ</span>
          </div>
          <span v-if="kySoSanh" class="ss-unit-pill">{{ kyChinh.nhan }} so với {{ kySoSanh.nhan }}</span>
        </div>

        <div class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table ss-compare-table mb-0">
              <thead>
                <tr>
                  <th>CHỈ SỐ</th>
                  <th v-for="k in cacKyDoiChieu" :key="`h-${k.ma}`" class="text-end ss-col-ky">
                    {{ k.nhan.toUpperCase() }}
                  </th>
                  <template v-if="kySoSanh">
                    <th class="text-end">CHÊNH LỆCH</th>
                    <th class="text-end">TĂNG / GIẢM</th>
                  </template>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in bangChiSo" :key="c.k" data-test="ss-chi-so">
                  <td class="fw-bold text-dark">{{ c.ten }}</td>
                  <td v-for="o in c.cot" :key="`${c.k}-${o.ma}`" class="text-end ad-nowrap">
                    <div class="fw-semibold text-dark">{{ o.text }}</div>
                    <div v-if="o.ss" class="ss-cell-sub" :class="o.ss.lop">{{ o.ss.bienDong }}</div>
                  </td>
                  <template v-if="c.ss">
                    <td class="text-end ad-nowrap ss-delta" :class="c.ss.lop">{{ c.ss.chenhText }}</td>
                    <td class="text-end ad-nowrap">
                      <span class="ss-trend-pill" :class="c.ss.lop">{{ c.ss.pctText }}</span>
                    </td>
                  </template>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <p v-if="coLocSanPham" class="ss-table-note mb-0">
          Đang lọc theo sản phẩm nên chỉ tính phần doanh thu của sản phẩm khớp lọc; số đơn là số đơn hoàn thành có chứa các sản phẩm đó.
        </p>
      </section>

      <!-- 6. Doanh thu theo thương hiệu -->
      <section class="ad-card ss-section-card" data-test="ss-thuong-hieu">
        <div class="ss-section-head">
          <div class="d-flex flex-wrap align-items-center gap-2">
            <h3 class="ss-section-title m-0">Doanh thu theo thương hiệu</h3>
            <span class="ss-count-badge">{{ bangThuongHieu.length }} thương hiệu</span>
          </div>
          <span class="ss-unit-pill">Đơn vị: VNĐ</span>
        </div>

        <div v-if="!bangThuongHieu.length" class="ad-empty">Các kỳ đã chọn chưa có sản phẩm nào được bán.</div>
        <div v-else class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table ss-compare-table mb-0">
              <thead>
                <tr>
                  <th>THƯƠNG HIỆU</th>
                  <th v-for="k in cacKyDoiChieu" :key="`th-h-${k.ma}`" class="text-end ss-col-ky">
                    {{ k.nhan.toUpperCase() }}
                  </th>
                  <template v-if="kySoSanh">
                    <th class="text-end">CHÊNH LỆCH</th>
                    <th class="text-end">TĂNG / GIẢM</th>
                  </template>
                  <th class="text-end">TỶ TRỌNG {{ kyChinh.nhan.toUpperCase() }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="th in bangThuongHieu" :key="th.ten">
                  <td><span class="ss-brand-pill">{{ th.ten }}</span></td>
                  <td v-for="k in cacKyDoiChieu" :key="`th-${th.ten}-${k.ma}`" class="text-end ad-nowrap text-dark">
                    {{ formatMoney(th.theoKy[k.ma] || 0) }}
                  </td>
                  <template v-if="th.ss">
                    <td class="text-end ad-nowrap ss-delta" :class="th.ss.lop">{{ th.ss.chenhText }}</td>
                    <td class="text-end ad-nowrap">
                      <span class="ss-trend-pill" :class="th.ss.lop">{{ th.ss.pctText }}</span>
                    </td>
                  </template>
                  <td class="text-end fw-semibold text-dark">{{ th.tyTrong }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 7. Sản phẩm bán chạy qua các kỳ -->
      <section class="ad-card ss-section-card" data-test="ss-san-pham">
        <div class="ss-section-head">
          <div class="d-flex flex-wrap align-items-center gap-2">
            <h3 class="ss-section-title m-0">Sản phẩm bán chạy qua các kỳ</h3>
            <span class="ss-count-badge">{{ bangSanPhamDoiChieu.length }} sản phẩm</span>
          </div>
          <span class="ss-unit-pill">Số lượng bán theo kỳ</span>
        </div>

        <div v-if="!bangSanPhamDoiChieu.length" class="ad-empty">
          Chưa có sản phẩm nào khớp điều kiện lọc trong các kỳ đã chọn.
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
                  <th
                    v-for="k in cacKyDoiChieu"
                    :key="`sp-h-${k.ma}`"
                    class="text-center ss-col-ky"
                  >
                    {{ k.nhan.toUpperCase() }}
                  </th>
                  <th class="text-center">TỔNG BÁN</th>
                  <th class="text-end">DOANH THU</th>
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
                    <span v-if="p.thuongHieu" class="ss-brand-pill">{{ p.thuongHieu }}</span>
                  </td>
                  <td
                    v-for="k in cacKyDoiChieu"
                    :key="`td-${p.maSanPham}-${k.ma}`"
                    class="text-center fw-bold ss-cell-ky"
                  >
                    {{ p.theoKy[k.ma] ?? 0 }}
                  </td>
                  <td class="text-center fw-bold text-dark">{{ p.tongBan }}</td>
                  <td class="text-end fw-bold text-dark ad-nowrap">{{ formatMoney(p.tongDoanhThu) }}</td>
                  <td class="text-end ad-nowrap">
                    <span
                      class="ss-trend-pill"
                      :class="p.huong === 'TANG' ? 'is-tot' : p.huong === 'GIAM' ? 'is-xau' : 'is-flat'"
                    >
                      {{ p.nhanXuHuong }}
                    </span>
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
  gap: 1.05rem;
  color: var(--fs-text, #18242f);
}

/* ----- 1. Topbar & Bộ lọc kỳ ----- */
.ss-filter-card {
  padding: 1.15rem 1.35rem;
  border-radius: 18px;
  border: 1px solid #e6eaf0;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.04);
}

.ss-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding-bottom: 0.9rem;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #edf2f7;
}

.ss-back-circle {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #e7f1fd;
  border: 1px solid #d5e5fb;
  color: #0977ec;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  font-size: 1rem;
  font-weight: 700;
  flex: none;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.ss-back-circle:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #075fc0;
}

.ss-main-title {
  font-size: 1.08rem;
  font-weight: 700;
  color: var(--fs-text, #18242f);
}

.ss-main-sub {
  font-size: 0.81rem;
  color: #64748b;
  margin-top: 2px;
}

.ss-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  height: 38px;
  padding: 0 1rem;
  border-radius: 10px;
  font-size: 0.82rem;
  font-weight: 600;
  border: 1px solid #d5e5fb;
  background: #e7f1fd;
  color: #0977ec;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
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
  border-color: #e2e8f0;
  color: var(--fs-text, #18242f);
}

.ss-action-btn--excel:hover:not(:disabled) {
  background: #e7f1fd;
  border-color: #c9def9;
  color: #0977ec;
}

.ss-action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* ----- Email Form (mở ngay dưới topbar) ----- */
.ss-email {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
  padding: 0.85rem 1rem;
  margin-bottom: 0.95rem;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
}

.ss-filter-input {
  height: 38px;
  padding: 0.4rem 0.85rem;
  font-size: 0.84rem;
  border-radius: 10px;
  border-color: #dce3ec;
  color: var(--fs-text, #18242f);
}

.ss-email-input {
  width: 250px;
  color: var(--fs-text, #18242f);
}

.ss-email-note {
  width: 260px;
  color: var(--fs-text, #18242f);
  flex: 1 1 220px;
}

.ss-email .ad-btn {
  height: 38px;
  padding: 0 1rem;
  border-radius: 10px;
  font-size: 0.83rem;
}

.ss-reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  height: 38px;
  padding: 0 0.95rem;
  border-radius: 10px;
  border: 1px solid #dce3ec;
  background: #ffffff;
  color: #475569;
  font-size: 0.83rem;
  font-weight: 600;
}

.ss-reset-btn:hover {
  background: #e7f1fd;
  border-color: #c9def9;
  color: #0977ec;
}

.ss-email-close {
  border-color: #dce3ec;
  background: #ffffff;
  color: #475569;
}

/* ----- Chế độ & chọn thương hiệu ----- */
.ss-mode-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding-bottom: 0.9rem;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #edf2f7;
}

.ss-mode-pills {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.22rem;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 0.2rem;
  border-radius: 10px;
}

.ss-mode-pill {
  height: 32px;
  padding: 0 0.9rem;
  border-radius: 7px;
  border: 0;
  background: transparent;
  color: #475569;
  font-size: 0.81rem;
  font-weight: 600;
  transition: background-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.ss-mode-pill:hover:not(.is-active) {
  color: #0977ec;
  background: rgba(255, 255, 255, 0.7);
}

.ss-mode-pill.is-active {
  background: #ffffff;
  color: #0977ec;
  font-weight: 700;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.ss-filter-select {
  height: 38px;
  min-width: 195px;
  padding: 0.35rem 2.1rem 0.35rem 0.85rem;
  font-size: 0.84rem;
  border-radius: 10px;
  border-color: #dce3ec;
  color: var(--fs-text, #18242f);
}

.ss-brand-select {
  width: auto;
}

/* ----- Khung chọn các kỳ ----- */
.ss-period-box {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 0.9rem 1.05rem;
}

.ss-period-box-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  width: 100%;
}

.ss-count-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.22rem 0.68rem;
  border-radius: 999px;
  background: #e7f1fd;
  border: 1px solid #d5e5fb;
  color: #0977ec;
  font-size: 0.76rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ss-add-period-btn {
  height: 34px;
  padding: 0 0.9rem;
  border-radius: 9px;
  border: 1px solid #c9def9;
  background: #ffffff;
  color: #0977ec;
  font-size: 0.8rem;
  font-weight: 600;
  margin-left: auto;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.ss-add-period-btn:hover:not(:disabled) {
  background: #e7f1fd;
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
  gap: 0.7rem;
  width: 100%;
}

.ss-period-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.55rem;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 11px;
  padding: 0.6rem 0.85rem;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.ss-ky-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.22rem 0.62rem;
  border-radius: 7px;
  background: #e7f1fd;
  border: 1px solid #d5e5fb;
  color: #0977ec;
  font-size: 0.76rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ss-period-lbl {
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 600;
  margin: 0;
}

.ss-period-select {
  height: 36px;
  padding: 0.25rem 2rem 0.25rem 0.72rem;
  font-size: 0.83rem;
  font-weight: 600;
  color: var(--fs-text, #18242f);
  border-radius: 9px;
  border-color: #dce3ec;
  width: auto;
}

.ss-period-select--month {
  min-width: 130px;
  flex: 1 1 130px;
}

.ss-period-select--year {
  min-width: 92px;
}

.ss-period-date {
  padding-right: 0.6rem;
  min-width: 138px;
  flex: 1 1 138px;
}

.ss-remove-period-btn {
  margin-left: auto;
  height: 34px;
  padding: 0 0.72rem;
  border-radius: 9px;
  border: 1px solid #fecdd3;
  background: #fff1f2;
  color: #e11d48;
  font-size: 0.78rem;
  font-weight: 600;
}

.ss-remove-period-btn:hover:not(:disabled) {
  background: #ffe4e6;
}

.ss-remove-period-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ----- 2. Dòng "Đang so sánh ..." ----- */
.ss-context {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.72rem 1.1rem;
  border-radius: 12px;
  background: #f0f7ff;
  border: 1px solid #c9def9;
  color: var(--fs-text, #18242f);
}

.ss-context-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.65rem;
  font-size: 0.9rem;
}

.ss-context-loc {
  padding: 0.14rem 0.6rem;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  color: #075fc0;
  font-size: 0.78rem;
  font-weight: 600;
}

.ss-context-warn {
  font-size: 0.81rem;
  color: #b45309;
}

/* ----- 3. Thẻ tóm tắt (2 hàng x 3 thẻ cân đối) ----- */
.ss-kpi-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.ss-kpi-box {
  padding: 0.95rem 1.15rem;
  border-radius: 16px;
  border: 1px solid #e6eaf0;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  gap: 0.42rem;
}

.ss-kpi-box.is-blue { border-top: 3px solid #0977ec; }
.ss-kpi-box.is-green { border-top: 3px solid #16a34a; }
.ss-kpi-box.is-sky { border-top: 3px solid #0284c7; }
.ss-kpi-box.is-indigo { border-top: 3px solid #4f46e5; }
.ss-kpi-box.is-amber { border-top: 3px solid #d97706; }
.ss-kpi-box.is-rose { border-top: 3px solid #e11d48; }

.ss-kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
}

.ss-kpi-lbl {
  font-size: 0.81rem;
  color: #64748b;
  font-weight: 600;
}

.ss-kpi-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: inline-grid;
  place-items: center;
  font-size: 0.95rem;
  flex: none;
}

.ss-kpi-icon.is-blue { background: #e7f1fd; color: #0977ec; }
.ss-kpi-icon.is-green { background: #dcfce7; color: #16a34a; }
.ss-kpi-icon.is-sky { background: #e0f2fe; color: #0284c7; }
.ss-kpi-icon.is-indigo { background: #eef2ff; color: #4f46e5; }
.ss-kpi-icon.is-amber { background: #fef3c7; color: #d97706; }
.ss-kpi-icon.is-rose { background: #ffe4e6; color: #e11d48; }

.ss-kpi-val {
  font-size: 1.38rem;
  font-weight: 700;
  color: var(--fs-text, #18242f);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.015em;
}

.ss-kpi-cmp {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.5rem;
  margin-top: 0.1rem;
}

/* ----- 4. Section Cards & Chart ----- */
.ss-section-card {
  padding: 1.15rem 1.35rem;
  border-radius: 16px;
  border: 1px solid #e6eaf0;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.ss-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
  margin-bottom: 0.9rem;
  padding-bottom: 0.65rem;
  border-bottom: 1px solid #f1f5f9;
}

.ss-section-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--fs-text, #18242f);
}

.ss-chart-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
  font-size: 0.81rem;
  color: #475569;
  font-weight: 600;
}

.ss-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
}

.ss-legend-sq {
  width: 11px;
  height: 11px;
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
  max-height: 335px;
  display: block;
}

.ss-combo-col {
  cursor: pointer;
}

.ss-chart-tooltip {
  position: absolute;
  top: 8px;
  transform: translateX(-50%);
  background: rgba(24, 36, 47, 0.96);
  color: #f8fafc;
  border-radius: 10px;
  padding: 0.65rem 0.85rem;
  min-width: 220px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.24);
  pointer-events: none;
  z-index: 5;
  font-size: 0.79rem;
}

.ss-tt-title {
  font-weight: 700;
  font-size: 0.85rem;
  color: #ffffff;
}

.ss-tt-range {
  font-size: 0.75rem;
  color: #94a3b8;
  padding-bottom: 0.38rem;
  margin-bottom: 0.38rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.ss-tt-row {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  margin-top: 0.22rem;
  color: #cbd5e1;
}

.ss-tt-row strong {
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}

/* ----- 5 & 6. Tables ----- */
.ss-unit-pill {
  padding: 0.22rem 0.68rem;
  border-radius: 999px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 0.76rem;
  font-weight: 600;
}

.ss-section-card .ad-table-wrap {
  border-radius: 12px;
  border: 1px solid #eef2f7;
  overflow: hidden;
}

.ss-compare-table {
  font-size: 0.86rem;
  font-variant-numeric: tabular-nums;
}

.ss-compare-table thead th {
  background: #f8fafc !important;
  color: #475569 !important;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 0.72rem 0.88rem;
  border-bottom: 1px solid #e2e8f0;
}

.ss-compare-table thead th.ss-col-ky {
  background: #eff6ff !important;
  color: #075fc0 !important;
}

.ss-compare-table tbody td {
  padding: 0.75rem 0.88rem;
  vertical-align: middle;
  color: var(--fs-text, #18242f);
  font-size: 0.86rem;
  border-bottom: 1px solid #f1f5f9;
}

.ss-compare-table tbody tr:last-child td {
  border-bottom: 0;
}

.ss-compare-table tbody tr:hover td {
  background: #f8fbff;
}

.ss-trend-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.22rem;
  padding: 0.22rem 0.62rem;
  border-radius: 999px;
  font-size: 0.77rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ss-trend-pill.is-tot {
  background: #dcfce7;
  color: #15803d;
}

.ss-trend-pill.is-xau {
  background: #ffe4e6;
  color: #be123c;
}

.ss-trend-pill.is-flat {
  background: #f1f5f9;
  color: #475569;
}

.ss-delta {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ss-delta.is-tot,
.ss-cell-sub.is-tot {
  color: #15803d;
}

.ss-delta.is-xau,
.ss-cell-sub.is-xau {
  color: #be123c;
}

.ss-delta.is-flat,
.ss-cell-sub.is-flat {
  color: #64748b;
}

.ss-cell-sub {
  font-size: 0.75rem;
  font-weight: 600;
  margin-top: 2px;
}

.ss-table-note {
  margin-top: 0.75rem;
  font-size: 0.8rem;
  color: #64748b;
}

.ss-brand-pill {
  display: inline-block;
  padding: 0.2rem 0.62rem;
  border-radius: 7px;
  background: #f0f7ff;
  border: 1px solid #d5e5fb;
  color: #075fc0;
  font-size: 0.78rem;
  font-weight: 600;
}

.ss-cell-ky {
  background: transparent;
  color: var(--fs-text, #18242f);
}

@media (max-width: 991.98px) {
  .ss-kpi-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767.98px) {
  .ss-period-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 575.98px) {
  .ss-kpi-strip {
    grid-template-columns: 1fr;
  }
}
</style>
