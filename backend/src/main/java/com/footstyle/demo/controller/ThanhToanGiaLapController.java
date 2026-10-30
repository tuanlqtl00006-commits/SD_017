package com.footstyle.demo.controller;

import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.CuaHangService;
import com.footstyle.demo.service.HoaDonSuaService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * THANH TOÁN GIẢ LẬP (dùng cho demo / đồ án, không có tiền thật):
 * mã QR trên trang đặt hàng trỏ tới trang "ngân hàng giả"; khách bấm "Xác nhận chuyển tiền" thì đơn được ghi nhận đã thanh toán.
 * Chỉ hoạt động khi app.thanh-toan.gia-lap=true. KHÔNG bật khi chạy thật vì ai biết mã đơn cũng xác nhận được.
 */
@RestController
@RequestMapping("/api/cua-hang/thanh-toan-gia-lap")
@RequiredArgsConstructor
public class ThanhToanGiaLapController {

    private final CuaHangService cuaHangService;
    private final HoaDonSuaService hoaDonSuaService;

    @Value("${app.thanh-toan.gia-lap:false}")
    private boolean giaLap;

    /** Tự điền IP mạng LAN của máy chạy backend (để điện thoại quét QR); để trống thì tự dò. */
    @Value("${app.thanh-toan.dia-chi-lan:}")
    private String diaChiLanCauHinh;

    /** IPv4 mạng nội bộ của máy này (ưu tiên 192.168.x.x, bỏ card ảo như VMware/VirtualBox/WSL/Docker). */
    private String diaChiLan() {
        if (diaChiLanCauHinh != null && !diaChiLanCauHinh.isBlank()) return diaChiLanCauHinh.trim();
        String tot = null;
        String du = null;
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                String ten = (ni.getDisplayName() + " " + ni.getName()).toLowerCase();
                boolean ao = ten.contains("vmware") || ten.contains("virtual") || ten.contains("vbox") || ten.contains("wsl")
                        || ten.contains("docker") || ten.contains("hyper-v") || ten.contains("vethernet") || ten.contains("tap");
                for (java.net.InetAddress a : java.util.Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof java.net.Inet4Address) || !a.isSiteLocalAddress()) continue;
                    String ip = a.getHostAddress();
                    if (!ao && ip.startsWith("192.168.") && tot == null) tot = ip;
                    else if (!ao && du == null) du = ip;
                }
            }
        } catch (java.net.SocketException ignored) {
            // không dò được: trả về null
        }
        return tot != null ? tot : du;
    }

    private void kiemTraBat() {
        if (!giaLap) throw ApiException.notFound("Tính năng thanh toán giả lập đang tắt.");
    }

    private Map<String, Object> thongTin(String ma) {
        Map<String, Object> don = cuaHangService.traCuuDonHang(ma, null);
        BigDecimal thanhTien = don.get("thanhTien") instanceof BigDecimal b ? b : BigDecimal.ZERO;
        BigDecimal daTra = don.get("daTra") instanceof BigDecimal b ? b : BigDecimal.ZERO;
        long conLai = Math.max(0, thanhTien.subtract(daTra).longValue());
        int tt = don.get("trangThai") instanceof Number n ? n.intValue() : 1;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maHoaDon", don.get("maHoaDon"));
        m.put("thanhTien", thanhTien);
        m.put("conLai", conLai);
        m.put("daThanhToanDu", Boolean.TRUE.equals(don.get("daThanhToanDu")));
        m.put("laChuyenKhoan", Integer.valueOf(2).equals(don.get("idPhuongThucThanhToan")));
        m.put("daHuy", tt == 0);
        return m;
    }

    @GetMapping("/{maHoaDon}")
    public Map<String, Object> xem(@PathVariable String maHoaDon) {
        kiemTraBat();
        Map<String, Object> kq = thongTin(maHoaDon);
        kq.put("diaChiLan", diaChiLan());
        return kq;
    }

    @PostMapping("/{maHoaDon}/xac-nhan")
    public Map<String, Object> xacNhan(@PathVariable String maHoaDon) {
        kiemTraBat();
        Map<String, Object> tt = thongTin(maHoaDon);
        if (Boolean.TRUE.equals(tt.get("daHuy"))) throw ApiException.conflict("Đơn hàng đã bị hủy.");
        if (!Boolean.TRUE.equals(tt.get("laChuyenKhoan"))) throw ApiException.conflict("Đơn này không thanh toán bằng chuyển khoản.");
        long conLai = ((Number) tt.get("conLai")).longValue();
        if (conLai <= 0) throw ApiException.conflict("Đơn hàng đã được thanh toán đủ.");
        String ma = String.valueOf(tt.get("maHoaDon"));
        hoaDonSuaService.ghiNhanChuyenKhoanTuDong(ma, conLai, "GIALAP-" + ma + "-" + System.currentTimeMillis(), "Thanh toán giả lập");
        return thongTin(ma);
    }
}
