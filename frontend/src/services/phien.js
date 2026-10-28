// Phiên đăng nhập lưu trên trình duyệt: { token, hetHan, nguoiDung, nho }.
// "Ghi nhớ đăng nhập" -> localStorage (đóng trình duyệt mở lại vẫn còn); không ghi nhớ -> sessionStorage (đóng tab là mất).
// Tách riêng khỏi useAuth để api.js đọc token mà không bị vòng import (api.js <- useAuth <- api.js).
const KHOA = 'fs_phien'

let boNho // undefined = chưa đọc; null = chưa đăng nhập

function docKho() {
  for (const ten of ['localStorage', 'sessionStorage']) {
    try {
      const s = window[ten]?.getItem(KHOA)
      if (s) return JSON.parse(s)
    } catch {
      // trình duyệt chặn bộ nhớ (chế độ riêng tư...) hoặc dữ liệu hỏng: coi như chưa đăng nhập
    }
  }
  return null
}

function conHan(p) {
  if (!p?.token) return false
  if (!p.hetHan) return true
  const t = Date.parse(p.hetHan)
  return Number.isNaN(t) || t > Date.now()
}

/** Phiên hiện tại (đã hết hạn thì coi như không có). */
export function layPhien() {
  if (boNho === undefined) boNho = docKho()
  if (boNho && !conHan(boNho)) {
    xoaPhien()
    return null
  }
  return boNho
}

export function luuPhien(phien) {
  boNho = phien
  try {
    window.localStorage.removeItem(KHOA)
    window.sessionStorage.removeItem(KHOA)
    const kho = phien.nho === false ? window.sessionStorage : window.localStorage
    kho.setItem(KHOA, JSON.stringify(phien))
  } catch {
    // không lưu được thì phiên chỉ sống tới khi tải lại trang
  }
}

export function xoaPhien() {
  boNho = null
  try {
    window.localStorage.removeItem(KHOA)
    window.sessionStorage.removeItem(KHOA)
  } catch {
    // bỏ qua
  }
}

export function layToken() {
  return layPhien()?.token || null
}

// Khi API báo hết phiên (401): MainLayout / main.js đăng ký hàm đưa người dùng về trang đăng nhập.
let xuLyHetPhien = null
export function datXuLyHetPhien(fn) {
  xuLyHetPhien = fn
}
export function baoHetPhien(lyDo) {
  if (xuLyHetPhien) xuLyHetPhien(lyDo)
}

/** Chỉ cho quay lại đường dẫn trong trang (chặn chuyển hướng ra trang ngoài qua ?quayLai=//web-la.com). */
export function duongDanQuayLaiAnToan(p) {
  if (typeof p !== 'string' || !p.startsWith('/') || p.startsWith('//') || p.startsWith('/\\')) return null
  if (p.startsWith('/dang-nhap')) return null
  return p
}

// Khi API báo nhân viên chưa vào ca (409 CHUA_NHAN_CA, vd ca vừa bị kết ở cửa sổ khác): main.js đưa về trang Vào ca.
let xuLyChuaVaoCa = null
export function datXuLyChuaVaoCa(fn) {
  xuLyChuaVaoCa = fn
}
export function baoChuaVaoCa() {
  if (xuLyChuaVaoCa) xuLyChuaVaoCa()
}

// Case khó "chuyển quyền nhân viên": mỗi phản hồi API của nhân viên có header X-Vai-Tro (vai trò đọc lại từ CSDL).
// Khác vai trò đang lưu (quản lý vừa đổi quyền trong lúc người này đang làm việc) thì main.js cập nhật menu / chuyển trang.
let xuLyDoiVaiTro = null
export function datXuLyDoiVaiTro(fn) {
  xuLyDoiVaiTro = fn
}
export function baoVaiTro(vaiTro) {
  const cu = layPhien()?.nguoiDung?.vaiTro
  if (!vaiTro || !cu || cu === 'KHACH_HANG' || cu === vaiTro) return
  if (xuLyDoiVaiTro) xuLyDoiVaiTro(vaiTro, cu)
}
