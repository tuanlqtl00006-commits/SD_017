import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'footstyle_cart'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    console.error('Không thể đọc giỏ hàng từ localStorage:', e)
    return []
  }
}

function saveToStorage(items) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
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
    // Miễn phí vận chuyển cho đơn hàng từ 500.000₫, dưới 500k thì phí 30.000₫
    return subTotal.value >= 500000 ? 0 : 30000
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
    setVoucher,
    removeVoucher,
  }
})
