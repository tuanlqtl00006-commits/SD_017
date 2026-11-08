import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

import { layPhien } from '../services/phien'
import { phiVanChuyenTheoTinh } from '../utils/phiVanChuyenTinh'
import { tienGiamTheoDotCuaDong } from '../utils/tienDon'
import api from '../services/api'

const KHOA_KHACH_VANG_LAI = 'footstyle_cart_khach'

// Giỏ hàng lưu theo tài khoản; nếu khách thêm hàng khi chưa đăng nhập (footstyle_cart_khach) rồi mới đăng nhập/đăng ký thì tự gộp sang giỏ của tài khoản
function khoaGioHang() {
  const nd = layPhien()?.nguoiDung
  return nd?.id ? `footstyle_cart_${nd.vaiTro}_${nd.id}` : KHOA_KHACH_VANG_LAI
}

function docTheoKhoa(khoa) {
  try {
    const raw = localStorage.getItem(khoa)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    console.error('Không thể đọc giỏ hàng từ localStorage:', e)
    return []
  }
}

function gopGioHang(dsTaiKhoan, dsKhachVangLai) {
  const map = new Map()
  for (const item of dsTaiKhoan) {
    if (item && item.bienTheId != null) {
      map.set(item.bienTheId, { ...item })
    }
  }
  for (const item of dsKhachVangLai) {
    if (!item || item.bienTheId == null) continue
    const maxStock = item.soLuongTon ?? 999
    const qtyToAdd = Number(item.soLuong) || 1
    if (map.has(item.bienTheId)) {
      const existing = map.get(item.bienTheId)
      const newQty = (Number(existing.soLuong) || 0) + qtyToAdd
      existing.soLuong = Math.min(newQty, maxStock)
    } else {
      map.set(item.bienTheId, { ...item, soLuong: Math.min(qtyToAdd, maxStock) })
    }
  }
  return Array.from(map.values())
}

function loadFromStorage() {
  const khoa = khoaGioHang()
  const dsHienTai = docTheoKhoa(khoa)

  // Khi khách đã đăng nhập: nếu trước đó lúc chưa đăng nhập có sản phẩm trong giỏ vãng lai thì giữ nguyên / gộp vào giỏ tài khoản
  if (khoa !== KHOA_KHACH_VANG_LAI) {
    const dsKhachVangLai = docTheoKhoa(KHOA_KHACH_VANG_LAI)
    if (dsKhachVangLai.length > 0) {
      const daGop = gopGioHang(dsHienTai, dsKhachVangLai)
      try {
        localStorage.setItem(khoa, JSON.stringify(daGop))
        localStorage.removeItem(KHOA_KHACH_VANG_LAI)
      } catch (e) {
        console.error('Không thể lưu giỏ hàng đã gộp vào localStorage:', e)
      }
      return daGop
    }
  }

  return dsHienTai
}

function saveToStorage(items) {
  try {
    localStorage.setItem(khoaGioHang(), JSON.stringify(items))
  } catch (e) {
    console.error('Không thể lưu giỏ hàng vào localStorage:', e)
  }
}

export const useCartStore = defineStore('cart', () => {
  const items = ref(loadFromStorage())
  const appliedVoucher = ref(null) // { ma, ten, loaiGiam, giaTriGiam, giamToiDa, donToiThieu }

  /* ----- Số do máy chủ tính (review 05/11) -----
   * Giỏ lưu giá trên trình duyệt nên có thể đã cũ (đợt giảm giá hết hạn, giá đổi) và phiếu giảm giá đang tính ở cả 2 nơi.
   * taiBaoGia() hỏi máy chủ (POST /cua-hang/tinh-tien: cùng cách tính với lúc đặt hàng), cập nhật giá trong giỏ, và các số
   * tiền bên dưới lấy theo máy chủ. Báo giá chỉ dùng khi đúng với giỏ hiện tại (cùng món, cùng số lượng, cùng mã phiếu);
   * giỏ vừa đổi mà chưa có báo giá mới thì tạm dùng số tính trên máy. */
  // Tỉnh/Thành khách chọn ở trang thanh toán (giỏ hàng chưa biết tỉnh thì chưa tính được phí ship)
  const tinhGiao = ref('')

  const baoGia = ref(null)
  const khoaBaoGia = computed(() =>
    JSON.stringify({ i: items.value.map((i) => [i.bienTheId, Number(i.soLuong) || 0]), v: appliedVoucher.value?.ma || '', t: tinhGiao.value || '' }),
  )
  const baoGiaHopLe = computed(() => (baoGia.value && baoGia.value.khoa === khoaBaoGia.value ? baoGia.value : null))
  let lanBaoGia = 0
  async function taiBaoGia() {
    if (!items.value.length) {
      baoGia.value = null
      return null
    }
    const khoa = khoaBaoGia.value
    const lan = ++lanBaoGia
    const nd = layPhien()?.nguoiDung
    try {
      const { data } = await api.post('/cua-hang/tinh-tien', {
        chiTiet: items.value.map((i) => ({ idSanPhamChiTiet: i.bienTheId, soLuong: Number(i.soLuong) || 0 })),
        maPhieuGiamGia: appliedVoucher.value?.ma || null,
        idKhachHang: nd?.vaiTro === 'KHACH_HANG' ? (nd.id ?? null) : null,
        tinhThanh: tinhGiao.value || null,
      })
      // lần hỏi cũ về chậm, hoặc giỏ đã đổi trong lúc chờ: bỏ, không đè số mới
      if (lan !== lanBaoGia || khoa !== khoaBaoGia.value || !data) return null
      let doiGia = false
      for (const d of data.dong || []) {
        const it = items.value.find((x) => x.bienTheId === d.idSanPhamChiTiet)
        if (!it || !d.conBan) continue
        if (Number(it.giaBan) !== Number(d.donGia) || Number(it.giaGoc) !== Number(d.giaGoc)) {
          it.giaBan = Number(d.donGia) || 0
          it.giaGoc = Number(d.giaGoc) || it.giaBan
          doiGia = true
        }
        if (d.soLuongTon !== undefined) it.soLuongTon = d.soLuongTon
      }
      if (doiGia) saveToStorage(items.value)
      baoGia.value = { ...data, khoa, doiGia }
      return baoGia.value
    } catch {
      return null // mất kết nối: giữ số tính trên máy, máy chủ vẫn tính lại khi đặt hàng
    }
  }

  const totalCount = computed(() => {
    return items.value.reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0)
  })

  // Tiền hàng đã trừ giảm theo đợt (giá bán x số lượng)
  const subTotal = computed(() => {
    if (baoGiaHopLe.value) return Number(baoGiaHopLe.value.tongTien) || 0
    return items.value.reduce((sum, item) => sum + (Number(item.giaBan) || 0) * (Number(item.soLuong) || 0), 0)
  })

  // Tiền khách được giảm theo đợt giảm giá (giá gốc - giá bán)
  const giamTheoDot = computed(() => {
    if (baoGiaHopLe.value) return Number(baoGiaHopLe.value.tienGiamTheoDot) || 0
    return tienGiamTheoDotCuaDong(items.value, { donGia: 'giaBan' })
  })

  // Phiếu không dùng được (máy chủ báo lý do)
  const loiPhieu = computed(() => baoGiaHopLe.value?.loiPhieu || '')

  const discountAmount = computed(() => {
    if (!appliedVoucher.value) return 0
    if (baoGiaHopLe.value) return Number(baoGiaHopLe.value.soTienGiam) || 0
    const v = appliedVoucher.value
    if (v.donToiThieu && subTotal.value < Number(v.donToiThieu)) {
      return 0
    }
    // loaiGiam: 1 = % (phan tram), 2 = so tien mat
    let giam = 0
    if (v.loaiGiam === 1 || v.hinhThucGiam === 1 || v.loai === 'PHAN_TRAM') {
      giam = (subTotal.value * Number(v.giaTriGiam || 0)) / 100
      if (v.giamToiDa && giam > Number(v.giamToiDa)) {
        giam = Number(v.giamToiDa)
      }
    } else {
      giam = Number(v.giaTriGiam || 0)
    }
    return Math.min(giam, subTotal.value)
  })

  const chuaCoTinh = computed(() => phiVanChuyenTheoTinh(tinhGiao.value) === null)

  const shippingFee = computed(() => {
    if (subTotal.value === 0) return 0
    // Số máy chủ tính (cùng tỉnh đang chọn) nếu có; không thì cùng luật với máy chủ (CuaHangService.datHangOnline):
    // Hà Nội miễn phí, tỉnh khác mất phí tùy tỉnh (không còn mốc miễn phí theo giá trị đơn).
    // Chưa chọn tỉnh thì chưa tính phí (0), giao diện hiện "chọn Tỉnh/Thành để tính".
    const pvc = baoGiaHopLe.value?.phiVanChuyen
    if (pvc !== null && pvc !== undefined) return Number(pvc) || 0
    return phiVanChuyenTheoTinh(tinhGiao.value) ?? 0
  })

  const finalTotal = computed(() => {
    if (baoGiaHopLe.value) return Number(baoGiaHopLe.value.thanhTien) || 0
    return Math.max(0, subTotal.value - discountAmount.value + shippingFee.value)
  })

  function addItem(itemData) {
    const existingIndex = items.value.findIndex(
      (i) => i.bienTheId === itemData.bienTheId
    )

    const maxStock = itemData.soLuongTon ?? 999
    const qtyToAdd = Number(itemData.soLuong) || 1

    if (existingIndex > -1) {
      const currentQty = items.value[existingIndex].soLuong
      if (currentQty >= maxStock) {
        // Đã đạt tối đa tồn kho, không thêm được nữa
        return { status: 'maxed', maxStock }
      }
      const newQty = currentQty + qtyToAdd
      items.value[existingIndex].soLuong = Math.min(newQty, maxStock)
      const actualAdded = items.value[existingIndex].soLuong - currentQty
      saveToStorage(items.value)
      return { status: actualAdded < qtyToAdd ? 'partial' : 'added', actualAdded, maxStock }
    } else {
      items.value.push({
        sanPhamId: itemData.sanPhamId,
        sanPhamTen: itemData.sanPhamTen,
        sanPhamMa: itemData.sanPhamMa || '',
        bienTheId: itemData.bienTheId,
        bienTheMa: itemData.bienTheMa || '',
        mauSac: itemData.mauSac || '',
        trongLuong: itemData.trongLuong || '',
        chuVi: itemData.chuVi || '',
        giaGoc: Number(itemData.giaGoc) || Number(itemData.giaBan) || 0,
        giaBan: Number(itemData.giaBan) || 0,
        anh: itemData.anh || '/logo.png',
        soLuong: Math.min(qtyToAdd, maxStock),
        soLuongTon: maxStock,
      })
      saveToStorage(items.value)
      return { status: 'added', actualAdded: Math.min(qtyToAdd, maxStock), maxStock }
    }
  }

  function updateQuantity(bienTheId, newQty) {
    const item = items.value.find((i) => i.bienTheId === bienTheId)
    if (!item) return
    const maxStock = item.soLuongTon ?? 999
    item.soLuong = Math.max(1, Math.min(Number(newQty) || 1, maxStock))
    saveToStorage(items.value)
  }

  function removeItem(bienTheId) {
    items.value = items.value.filter((i) => i.bienTheId !== bienTheId)
    saveToStorage(items.value)
    if (items.value.length === 0) {
      appliedVoucher.value = null
    }
  }

  function clearCart() {
    items.value = []
    appliedVoucher.value = null
    tinhGiao.value = ''
    saveToStorage(items.value)
  }

  // Gọi khi đăng nhập / đăng xuất / đổi tài khoản: nạp lại giỏ của tài khoản hiện tại (tự gộp giỏ khách vãng lai nếu có)
  function taiLai() {
    items.value = loadFromStorage()
    if (items.value.length === 0) {
      appliedVoucher.value = null
    }
  }

  function setVoucher(voucher) {
    appliedVoucher.value = voucher
  }

  function removeVoucher() {
    appliedVoucher.value = null
  }

  return {
    items,
    appliedVoucher,
    totalCount,
    subTotal,
    giamTheoDot,
    loiPhieu,
    baoGia: baoGiaHopLe,
    taiBaoGia,
    discountAmount,
    tinhGiao,
    chuaCoTinh,
    shippingFee,
    finalTotal,
    addItem,
    updateQuantity,
    removeItem,
    clearCart,
    taiLai,
    setVoucher,
    removeVoucher,
  }
})
