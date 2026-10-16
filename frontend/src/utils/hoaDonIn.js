// Bản in / PDF hóa đơn (mở bằng cửa sổ in của trình duyệt, chọn "Lưu dưới dạng PDF" để có file PDF).
// Viết thành hàm thuần (nhận dữ liệu trả về HTML) để test được mà không cần mở cửa sổ.
//
// Theo lời cô (video review GĐ2, 11:49 - 11:53):
//  - KHÔNG in khối "Lịch sử thanh toán";
//  - in mã phiếu giảm giá đã dùng và giảm bao nhiêu %, phí vận chuyển, logo đơn vị giao hàng (GHN, GHTK...), có mã QR;
//  - đơn tại quầy không có vận chuyển (không in phí / đơn vị vận chuyển), khách lẻ không in số điện thoại.
import { TT, trangThaiOf } from '../constants/hoaDon'
import { formatMoney, formatNgayGio } from './format'
import { nhanDienVanChuyen } from './vanChuyen'

/** Thoát ký tự HTML: dữ liệu khách nhập (tên, địa chỉ, ghi chú...) không được chèn được thẻ vào bản in. */
export const esc = (v) =>
    String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c])

const money = (v) => formatMoney(v ?? 0)
const LOAI_PHAN_TRAM = 1 // khớp PhieuGiamGia.LOAI_PHAN_TRAM bên backend

/** Tách mức giảm của phiếu: { giam: 'giảm 10%', toiDa: '100.000 ₫' }; null nếu không dùng phiếu hoặc thiếu giá trị. */
function phanMucGiam(hd) {
  const giaTri = Number(hd?.giaTriPhieuGiamGia)
  if (!hd?.maPhieuGiamGia || !(giaTri > 0)) return null
  if (Number(hd.loaiPhieuGiamGia) === LOAI_PHAN_TRAM) {
    const toiDa = Number(hd.giamToiDaPhieuGiamGia)
    return { giam: `giảm ${giaTri}%`, toiDa: toiDa > 0 ? money(toiDa) : '' }
  }
  return { giam: `giảm ${money(giaTri)}`, toiDa: '' }
}

/** "giảm 10% (tối đa 100.000 ₫)" | "giảm 50.000 ₫" | '' (không dùng phiếu hoặc thiếu giá trị). */
export function moTaMucGiam(hd) {
  const p = phanMucGiam(hd)
  return p ? `${p.giam}${p.toiDa ? ` (tối đa ${p.toiDa})` : ''}` : ''
}

/** Nhãn dòng giảm giá trên bản in, chữ thuần: "Giảm giá (mã VCH7QX2YN – giảm 10%, tối đa 100.000 ₫)" (không lồng ngoặc trong ngoặc). */
export function nhanGiamGia(hd) {
  if (!hd?.maPhieuGiamGia) return 'Giảm giá'
  const p = phanMucGiam(hd)
  return `Giảm giá (mã ${hd.maPhieuGiamGia}${p ? ` – ${p.giam}${p.toiDa ? `, tối đa ${p.toiDa}` : ''}` : ''})`
}

const thongSo = (x) => [x.trongLuong, x.chuVi].filter(Boolean).join(' · ') || '—'

/**
 * Logo đơn vị vận chuyển cho bản in: có file logo thì in logo (chưa có file / lỗi tải thì tự đổi sang nhãn chữ),
 * đơn vị khác chỉ in nhãn chữ. Cửa sổ in là trang riêng nên cần địa chỉ đầy đủ (goc) để tải được file logo.
 */
export function htmlVanChuyen(tenDonVi, goc = '') {
  const dv = nhanDienVanChuyen(tenDonVi)
  if (!dv) return '—'
  if (!dv.logo) return `<span class="vc-badge">${esc(dv.ten)}</span>`
  return `<span class="vc-logo-wrap"><img class="vc-logo" src="${esc(goc + dv.logo)}" alt="Logo ${esc(dv.ma)}" `
      + `onerror="this.style.display='none';this.nextElementSibling.style.display='inline-block'">`
      + `<span class="vc-badge" style="display:none">${esc(dv.ma)}</span></span> <span class="vc-ten">${esc(dv.ten)}</span>`
}

const CSS = `
    body{font-family:"Segoe UI",Roboto,Arial,sans-serif;color:#18242f;margin:32px;font-size:13px}
    .top{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #0977ec;padding-bottom:12px;margin-bottom:16px}
    .top img{height:56px}
    h1{font-size:20px;margin:0 0 4px;color:#0977ec}
    p{margin:3px 0}
    table{width:100%;border-collapse:collapse;margin-top:14px}
    th{background:#e7f1fd;text-align:left;padding:8px;font-size:12px}
    td{padding:8px;border-bottom:1px solid #e6eaf0;vertical-align:top}
    .c{text-align:center}.r{text-align:right;white-space:nowrap}.m{color:#7b8794;font-size:11px;margin-top:2px}
    .sum{margin-left:auto;width:360px;margin-top:14px}
    .sum div{display:flex;justify-content:space-between;gap:16px;padding:4px 0}
    .sum div span:last-child{white-space:nowrap}
    .sum .t{border-top:1px solid #e6eaf0;margin-top:4px;padding-top:8px;font-weight:700;font-size:15px;color:#0977ec}
    .sum .no{color:#c62836;font-weight:700}
    .gg{color:#c62836;font-size:11px;font-weight:600}
    s{color:#7b8794;font-size:11px}
    .foot{text-align:center;color:#7b8794;margin-top:28px}
    h2{font-size:13px;text-transform:uppercase;letter-spacing:.04em;color:#0977ec;margin:0 0 6px;padding-bottom:4px;border-bottom:1px solid #cfe0f7}
    .sec{margin-top:18px;break-inside:avoid}
    .two{display:grid;grid-template-columns:1fr 1fr;gap:16px}
    .head-r{display:flex;align-items:flex-start;gap:14px}
    .qr{width:96px;height:96px;display:block}
    .vc-dong{display:flex;align-items:center;gap:8px;flex-wrap:wrap}
    .vc-logo{height:28px;max-width:110px;object-fit:contain;vertical-align:middle}
    .vc-badge{display:inline-block;padding:2px 9px;border:1.5px solid #0977ec;border-radius:6px;font-weight:800;letter-spacing:.04em;color:#075bb5;background:#e7f1fd}
    .vc-ten{font-weight:600}
    @media print{body{margin:14mm}}`

/**
 * HTML bản in hóa đơn.
 * ctx: { hd, khach, giao, sanPham, daThanhToan, qrUrl, goc }
 *  - hd/khach/giao/sanPham: các mục "hoaDon", "khachHang", "giaoHang", "sanPham" của GET /api/hoa-don/{id}
 *  - daThanhToan: tổng đã thanh toán; qrUrl: ảnh QR dạng data URL; goc: địa chỉ gốc của trang (location.origin) để tải logo
 */
export function taoHtmlHoaDon({ hd = {}, khach = {}, giao = {}, sanPham = [], daThanhToan = 0, qrUrl = '', goc = '' } = {}) {
  const laGiaoHang = hd.loaiDon === 2
  const trangThai = trangThaiOf(hd.trangThai)

  const dongSanPham = sanPham.map((x, i) => {
    const giam = Number(x.phanTramGiam) > 0
    return `
    <tr>
      <td class="c">${i + 1}</td>
      <td>${esc(x.tenSanPham)}<div class="m">${esc(x.maSpct)} · ${esc(x.mauSac || '')} · ${esc(thongSo(x))}</div></td>
      <td class="c">${esc(x.soLuong)}</td>
      <td class="r">${giam ? `<s>${esc(money(x.giaGoc))}</s><br>` : ''}${esc(money(x.donGia))}${giam ? ` <span class="gg">-${esc(x.phanTramGiam)}%</span>` : ''}</td>
      <td class="r">${esc(money(x.thanhTien))}</td>
    </tr>`
  }).join('')

  // Khách lẻ (không gắn hồ sơ khách hàng) chỉ ghi "Khách vãng lai": không SĐT, không email
  const thongTinKhach = khach.khachLe && !khach.sdt
      ? `<p><b>Tên:</b> ${esc(khach.ten || 'Khách vãng lai')}</p><p class="m">Khách lẻ, không lưu số điện thoại.</p>`
      : `<p><b>Tên:</b> ${esc(khach.ten || '—')}</p>
        <p><b>SĐT:</b> ${esc(khach.sdt || '—')}</p>
        <p><b>Email:</b> ${esc(khach.email || '—')}</p>`

  // Đơn tại quầy: nhận hàng ngay tại cửa hàng, không có người nhận / địa chỉ / đơn vị vận chuyển
  const phanGiaoHang = laGiaoHang
      ? `<p><b>Người nhận:</b> ${esc(giao.hoTenNguoiNhan || khach.ten)}</p>
       <p><b>SĐT nhận:</b> ${esc(giao.sdtNguoiNhan || '—')}</p>
       <p><b>Địa chỉ:</b> ${esc(giao.diaChi || '—')}</p>
       <p class="vc-dong"><b>Vận chuyển:</b> ${htmlVanChuyen(hd.donViVanChuyen, goc)}</p>`
      : '<p>Nhận hàng tại cửa hàng</p>'

  const conPhaiTra = Math.max(0, Number(hd.thanhTien ?? 0) - Number(daThanhToan ?? 0))
  const qr = qrUrl ? `<img class="qr" src="${qrUrl}" alt="QR ${esc(hd.maHoaDon)}">` : ''

  return `<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${esc(hd.maHoaDon)}</title>
  <style>${CSS}
  </style></head><body>
    <div class="top">
      <div><img src="${esc(goc)}/logo.png" alt="FootStyle"><p>Shop cầu lông FootStyle</p></div>
      <div class="head-r">
        <div style="text-align:right"><h1>HÓA ĐƠN BÁN HÀNG</h1>
          <p><b>Mã:</b> ${esc(hd.maHoaDon)}</p><p><b>Ngày:</b> ${esc(formatNgayGio(hd.ngayTao))}</p></div>
        ${qr}
      </div>
    </div>

    <div class="sec">
      <h2>Thông tin đơn hàng</h2>
      <div class="two">
        <div><p><b>Mã hóa đơn:</b> ${esc(hd.maHoaDon)}</p><p><b>Loại đơn:</b> ${esc(hd.tenLoaiDon)}</p><p><b>Nhân viên:</b> ${esc(hd.tenNhanVien || '—')}</p></div>
        <div><p><b>Trạng thái:</b> ${esc(trangThai.label)}</p><p><b>Thanh toán:</b> ${esc(hd.phuongThucThanhToan || '—')}</p></div>
      </div>
    </div>

    <div class="two">
      <div class="sec">
        <h2>Khách hàng</h2>
        ${thongTinKhach}
      </div>
      <div class="sec">
        <h2>Giao hàng</h2>
        ${phanGiaoHang}
        ${hd.ghiChu ? `<p><b>Ghi chú:</b> ${esc(hd.ghiChu)}</p>` : ''}
      </div>
    </div>

    <div class="sec">
      <h2>Sản phẩm</h2>
      <table><thead><tr><th class="c">STT</th><th>Sản phẩm</th><th class="c">SL</th><th class="r">Đơn giá</th><th class="r">Thành tiền</th></tr></thead>
        <tbody>${dongSanPham}</tbody></table>
    </div>

    <div class="sec">
      <h2>Tổng tiền</h2>
      <div class="sum">
        <div><span>Tổng tiền hàng</span><span>${esc(money(hd.tongTien))}</span></div>
        <div><span>${esc(nhanGiamGia(hd))}</span><span>- ${esc(money(hd.soTienGiam))}</span></div>
        ${laGiaoHang ? `<div><span>Phí vận chuyển</span><span>${esc(money(hd.phiVanChuyen))}</span></div>` : ''}
        <div class="t"><span>Tổng thanh toán</span><span>${esc(money(hd.thanhTien))}</span></div>
        <div><span>Đã thanh toán</span><span>${esc(money(daThanhToan))}</span></div>
        ${hd.trangThai !== TT.DA_HUY && conPhaiTra > 0 ? `<div class="no"><span>Còn phải trả</span><span>${esc(money(conPhaiTra))}</span></div>` : ''}
      </div>
    </div>
    <p class="foot">Cảm ơn quý khách đã mua hàng tại FootStyle!</p>
    <script>window.onload=function(){window.print()}<\/script>
  </body></html>`
}
