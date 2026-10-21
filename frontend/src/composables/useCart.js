import { ref, computed } from 'vue'

const KHOA_LUU = 'fs_gio_hang'

function docTuLocalStorage() {
  try {
    const raw = localStorage.getItem(KHOA_LUU)
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    return []
  }
}

const items = ref(docTuLocalStorage())

function luuVaoLocalStorage() {
  try {
    localStorage.setItem(KHOA_LUU, JSON.stringify(items.value))
  } catch (e) {
    // ignore
  }
}

export function useCart() {
  const tongSoLuong = computed(() =>
    items.value.reduce((sum, item) => sum + (item.soLuong || 1), 0),
  )

  const tongTien = computed(() =>
    items.value.reduce(
      (sum, item) => sum + (item.giaBan || 0) * (item.soLuong || 1),
      0,
    ),
  )

  function them(sanPham, bienThe, soLuong = 1) {
    const idBt = bienThe.id
    const tonTai = items.value.find((i) => i.id === idBt)

    if (tonTai) {
      const slMoi = tonTai.soLuong + soLuong
      const maxTon = bienThe.soLuongTon != null ? bienThe.soLuongTon : 999
      tonTai.soLuong = Math.min(slMoi, maxTon)
    } else {
      items.value.push({
        id: idBt,
        idSanPham: sanPham.id,
        maSpct: bienThe.maSpct,
        tenSanPham: sanPham.tenSanPham,
        anh: bienThe.anh || sanPham.anhChinh,
        tenMau: bienThe.tenMau,
        trongLuong: bienThe.trongLuong,
        chuVi: bienThe.chuVi,
        giaBan: bienThe.giaBan,
        giaGoc: bienThe.giaBanGoc || bienThe.giaBan,
        phanTramGiam: bienThe.phanTramGiam || 0,
        soLuong: Math.max(1, soLuong),
        soLuongTon: bienThe.soLuongTon != null ? bienThe.soLuongTon : 999,
      })
    }
    luuVaoLocalStorage()
  }

  function suaSoLuong(idBt, soLuong) {
    const item = items.value.find((i) => i.id === idBt)
    if (!item) return
    if (soLuong <= 0) {
      xoa(idBt)
      return
    }
    item.soLuong = Math.min(soLuong, item.soLuongTon || 999)
    luuVaoLocalStorage()
  }

  function xoa(idBt) {
    items.value = items.value.filter((i) => i.id !== idBt)
    luuVaoLocalStorage()
  }

  function lamTrong() {
    items.value = []
    luuVaoLocalStorage()
  }

  return {
    items,
    tongSoLuong,
    tongTien,
    them,
    suaSoLuong,
    xoa,
    lamTrong,
  }
}
