package com.footstyle.demo.service;

import com.footstyle.demo.controller.ThanhToanGiaLapController;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Chuyển khoản tại quầy bằng QR giả lập (giống trang đặt hàng online, chỉ khi app.thanh-toan.gia-lap=true).
 * Nhân viên tạo yêu cầu với số tiền đang hiện -> khách quét QR mở trang ngân hàng giả, bấm xác nhận
 * -> màn bán hàng thấy đã nhận tiền và thanh toán hóa đơn. Yêu cầu chỉ giữ trong bộ nhớ, không lưu CSDL.
 */
@Service
@RequiredArgsConstructor
public class ChuyenKhoanQuayService {

    /** Yêu cầu chuyển khoản của một hóa đơn chờ. */
    public record YeuCau(String maHoaDon, long soTien, boolean daNhan, String maGiaoDich) {
    }

    private final HoaDonRepository hoaDonRepo;
    private final Map<String, YeuCau> dsYeuCau = new ConcurrentHashMap<>();

    @Value("${app.thanh-toan.gia-lap:false}")
    private boolean giaLap;

    @Value("${app.thanh-toan.dia-chi-lan:}")
    private String diaChiLanCauHinh;

    void datGiaLap(boolean bat) {
        this.giaLap = bat;
    }

    private void kiemTraBat() {
        if (!giaLap) throw ApiException.notFound("Tính năng thanh toán giả lập đang tắt.");
    }

    /** Hóa đơn chờ của nhân viên đang đăng nhập. */
    private HoaDon hoaDonChoCuaToi(Integer idHoaDon) {
        NguoiDung nd = XacThuc.nhanVien();
        HoaDon hd = idHoaDon == null ? null : hoaDonRepo.findById(idHoaDon).orElse(null);
        if (hd == null || !HoaDonChoService.laHoaDonCho(hd)) throw ApiException.notFound("Không tìm thấy hóa đơn chờ.");
        if (hd.getNhanVien() == null || !hd.getNhanVien().getId().equals(nd.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Đây là hóa đơn chờ của nhân viên khác.");
        }
        return hd;
    }

    /** Tạo (hoặc giữ nguyên nếu cùng số tiền) yêu cầu chuyển khoản; đổi số tiền thì phải quét lại. */
    public Map<String, Object> taoYeuCau(Integer idHoaDon, Long soTien) {
        kiemTraBat();
        HoaDon hd = hoaDonChoCuaToi(idHoaDon);
        if (soTien == null || soTien <= 0) throw ApiException.badRequest("Số tiền chuyển khoản phải lớn hơn 0.");
        if (soTien > GiaoCaService.TIEN_TOI_DA) throw ApiException.badRequest("Số tiền tối đa 1.000.000.000 ₫.");
        String ma = hd.getMaHoaDon();
        YeuCau cu = dsYeuCau.get(ma);
        YeuCau yc = cu != null && cu.soTien() == soTien ? cu : new YeuCau(ma, soTien, false, null);
        dsYeuCau.put(ma, yc);
        Map<String, Object> kq = dto(yc);
        kq.put("diaChiLan", ThanhToanGiaLapController.timDiaChiLan(diaChiLanCauHinh));
        return kq;
    }

    public Map<String, Object> trangThai(Integer idHoaDon) {
        kiemTraBat();
        HoaDon hd = hoaDonChoCuaToi(idHoaDon);
        YeuCau yc = dsYeuCau.get(hd.getMaHoaDon());
        if (yc == null) throw ApiException.notFound("Chưa tạo mã QR chuyển khoản cho hóa đơn này.");
        return dto(yc);
    }

    /* ----- Trang ngân hàng giả (công khai, khách mở bằng cách quét QR) ----- */

    /** Cùng dạng dữ liệu với thanh toán giả lập của đơn online để dùng chung trang ngân hàng giả. */
    public Map<String, Object> xemChoNganHang(String maHoaDon) {
        kiemTraBat();
        YeuCau yc = timYeuCau(maHoaDon);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maHoaDon", yc.maHoaDon());
        m.put("thanhTien", yc.soTien());
        m.put("conLai", yc.daNhan() ? 0 : yc.soTien());
        m.put("daThanhToanDu", yc.daNhan());
        m.put("laChuyenKhoan", true);
        m.put("daHuy", false);
        return m;
    }

    public Map<String, Object> xacNhanTuNganHang(String maHoaDon) {
        kiemTraBat();
        YeuCau yc = timYeuCau(maHoaDon);
        if (!yc.daNhan()) {
            dsYeuCau.put(yc.maHoaDon(), new YeuCau(yc.maHoaDon(), yc.soTien(), true, "GIALAP-" + yc.maHoaDon() + "-" + System.currentTimeMillis()));
        }
        return xemChoNganHang(maHoaDon);
    }

    /** Hóa đơn đã thanh toán / hủy: bỏ yêu cầu. */
    public void xoa(String maHoaDon) {
        if (maHoaDon != null) dsYeuCau.remove(maHoaDon);
    }

    private YeuCau timYeuCau(String maHoaDon) {
        YeuCau yc = maHoaDon == null ? null : dsYeuCau.get(maHoaDon.trim());
        if (yc == null) throw ApiException.notFound("Không tìm thấy yêu cầu chuyển khoản " + maHoaDon + ".");
        return yc;
    }

    private static Map<String, Object> dto(YeuCau yc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maHoaDon", yc.maHoaDon());
        m.put("soTien", yc.soTien());
        m.put("daNhan", yc.daNhan());
        m.put("maGiaoDich", yc.maGiaoDich());
        return m;
    }
}
