// Hàm phụ cho nhận ca / giao ca.
import { esc } from './hoaDonIn'
import { formatMoney, formatNgayGio } from './format'

/** Khoảng thời gian giữa 2 mốc -> '4 giờ 05 phút' (dưới 1 giờ: '35 phút'). */
export function thoiLuong(tu, den = new Date()) {
  const a = new Date(tu).getTime()
  const b = new Date(den).getTime()
  if (Number.isNaN(a) || Number.isNaN(b) || b < a) return ''
  const phut = Math.floor((b - a) / 60000)
  const gio = Math.floor(phut / 60)
  const du = phut % 60
  if (gio === 0) return `${du} phút`
  return `${gio} giờ ${String(du).padStart(2, '0')} phút`
}

/** Chênh lệch = thực tế - hệ thống (null khi chưa nhập thực tế). */
export function tinhChenhLech(thucTe, heThong) {
  if (thucTe === '' || thucTe === null || thucTe === undefined) return null
  return Number(thucTe) - Number(heThong || 0)
}

/** Nhãn + kiểu của chênh lệch: 'Khớp' / 'Thiếu 50.000 ₫' / 'Thừa 20.000 ₫'. */
export function moTaChenhLech(cl) {
  if (cl === null || cl === undefined) return { nhan: '—', kieu: '' }
  if (Number(cl) === 0) return { nhan: 'Khớp', kieu: 'ok' }
  return Number(cl) < 0 ? { nhan: `Thiếu ${formatMoney(-cl)}`, kieu: 'thieu' } : { nhan: `Thừa ${formatMoney(cl)}`, kieu: 'thua' }
}

/** HTML biên bản giao ca để in (mở cửa sổ in của trình duyệt). */
export function htmlBienBanGiaoCa(gc, tenCuaHang = 'FootStyle') {
  const dong = (nhan, giaTri) => `<tr><th>${esc(nhan)}</th><td>${esc(giaTri ?? '—')}</td></tr>`
  const cl = moTaChenhLech(gc.chenhLech)
  return `<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>Biên bản giao ca ${esc(gc.maGiaoCa)}</title>
<style>
body{font-family:Arial,sans-serif;color:#18242f;margin:32px;font-size:14px}
h1{font-size:20px;margin:0 0 4px}.phu{color:#555;margin:0 0 18px}
table{width:100%;border-collapse:collapse;margin-bottom:18px}th,td{border:1px solid #cfd6df;padding:7px 10px;text-align:left}
th{width:42%;background:#f3f6fb;font-weight:600}.ky{display:flex;justify-content:space-around;margin-top:48px;text-align:center}
.ky div{width:40%}.ky small{color:#666}
</style></head><body>
<h1>BIÊN BẢN GIAO CA - ${esc(tenCuaHang)}</h1>
<p class="phu">Mã ca: <b>${esc(gc.maGiaoCa)}</b> · In lúc ${esc(formatNgayGio(new Date()))}</p>
<table>
${dong('Nhân viên', `${gc.tenNhanVien} (${gc.maNhanVien})`)}
${dong('Ca theo lịch', gc.ngoaiLich ? 'Ngoài lịch' : `${gc.tenCa} (${gc.gioCa})`)}
${dong('Nhận ca lúc', formatNgayGio(gc.thoiGianNhanCa))}
${dong('Giao ca lúc', formatNgayGio(gc.thoiGianKetCa))}
${dong('Số hóa đơn', gc.soHoaDon ?? 0)}
${dong('Doanh thu', formatMoney(gc.doanhThu ?? 0))}
${dong('Tiền đầu ca', formatMoney(gc.tienDauCa ?? 0))}
${dong('Tiền mặt thu trong ca', formatMoney(gc.tienMatThu ?? 0))}
${dong('Chuyển khoản thu trong ca', formatMoney(gc.chuyenKhoanThu ?? 0))}
${dong('Tiền mặt theo hệ thống', formatMoney(gc.tienMatHeThong ?? 0))}
${dong('Tiền mặt thực tế', formatMoney(gc.tienMatThucTe ?? 0))}
${dong('Chênh lệch', cl.nhan)}
${dong('Tiền bàn giao cho ca sau', formatMoney(gc.tienBanGiao ?? 0))}
${dong('Người nhận bàn giao', gc.tenNguoiNhanBanGiao || '—')}
${dong('Ghi chú', gc.ghiChuKetCa || '—')}
</table>
<div class="ky"><div><b>Người giao ca</b><br><small>(ký, ghi rõ họ tên)</small><br><br><br>${esc(gc.tenNhanVien)}</div>
<div><b>Người nhận ca</b><br><small>(ký, ghi rõ họ tên)</small><br><br><br>${esc(gc.tenNguoiNhanBanGiao || '')}</div></div>
</body></html>`
}

/** Mở cửa sổ in biên bản. */
export function inBienBanGiaoCa(gc) {
  const w = window.open('', '_blank', 'width=760,height=900')
  if (!w) return false
  w.document.open()
  w.document.write(htmlBienBanGiaoCa(gc))
  w.document.close()
  w.focus()
  setTimeout(() => w.print(), 300)
  return true
}
