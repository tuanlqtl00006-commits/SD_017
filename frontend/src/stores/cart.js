import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

import { layPhien } from '../services/phien'
import { PHI_GIAO } from '../utils/banHang'

const KHOA_KHACH_VANG_LAI = 'footstyle_cart_khach'
/** Phí giao đơn online dưới mức miễn phí (khớp máy chủ). */
export const PHI_SHIP_ONLINE = 30000

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

  const totalCount = computed(() => {
    return items.value.reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0)
  })

  const subTotal = computed(() => {
    return items.value.reduce((sum, item) => sum + (Number(item.giaBan) || 0) * (Number(item.soLuong) || 0), 0)
  })

  const discountAmount = computed(() => {
    if (!appliedVoucher.value) return 0
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

  const shippingFee = computed(() => {
    if (subTotal.value === 0) return 0
    // Cùng luật với máy chủ (CuaHangService.datHangOnline): đơn từ 1.000.000₫ miễn phí, dưới mức đó phí 30.000₫.
    // Trước đây giỏ hàng miễn phí từ 500.000₫ nên khách thấy "Miễn phí" mà đơn lưu lại bị cộng 30.000₫ (review 04/11, đơn HD970).
    return subTotal.value >= PHI_GIAO.mienPhiTu ? 0 : PHI_SHIP_ONLINE
  })

  const finalTotal = computed(() => {
    return Math.max(0, subTotal.value - discountAmount.value + shippingFee.value)
  })

  function addItem(itemData) {
    const existingIndex = items.value.findIndex(
      (i) => i.bienTheId === itemData.bienTheId
    )

    const maxStock = itemData.soLuongTon ?? 999
    const qtyToAdd = Number(itemData.soLuong) || 1

    if (existingIndex > -1) {
      const newQty = items.value[existingIndex].soLuong + qtyToAdd
      items.value[existingIndex].soLuong = Math.min(newQty, maxStock)
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
    }
    saveToStorage(items.value)
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
    discountAmount,
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
