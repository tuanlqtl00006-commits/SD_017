package com.footstyle.demo.controller;

import com.footstyle.demo.dto.DoiMatKhauRequest;
import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.dto.KhachHoSoRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.service.AuthService;
import com.footstyle.demo.service.CuaHangService;
import com.footstyle.demo.service.HoaDonService;
import com.footstyle.demo.service.KhachHangService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Hồ sơ + mật khẩu của CHÍNH khách hàng đang đăng nhập (trang "Tài khoản" ở cửa hàng online).
 * Luôn lấy id khách từ phiên đăng nhập nên khách không thể xem / sửa tài khoản của người khác.
 */
@RestController
@RequestMapping("/api/khach")
@RequiredArgsConstructor
public class KhachTaiKhoanController {

    private final KhachHangService khachHangService;
    private final AuthService authService;
    private final CuaHangService cuaHangService;
    private final HoaDonService hoaDonService;

    private Integer idKhachHienTai() {
        NguoiDung nd = XacThuc.hienTai()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập.", null, null, null));
        if (!nd.laKhachHang()) throw ApiException.badRequest("Chỉ tài khoản khách hàng mới dùng được chức năng này.");
        return nd.id();
    }

    private static Map<String, Object> hoSo(KhachHang kh) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hoTen", kh.getHoTen());
        m.put("sdt", kh.getSdt());
        m.put("email", kh.getEmail());
        m.put("ngaySinh", kh.getNgaySinh());
        m.put("gioiTinh", kh.getGioiTinh());
        return m;
    }

    @GetMapping("/tai-khoan")
    public Map<String, Object> xem() {
        return hoSo(khachHangService.layTheoId(idKhachHienTai()));
    }

    /** Lưu hồ sơ; trả về hồ sơ mới + phiên mới (để giao diện cập nhật tên hiển thị). */
    @PutMapping("/tai-khoan")
    public Map<String, Object> sua(@RequestBody(required = false) KhachHoSoRequest req) {
        KhachHang kh = khachHangService.suaHoSoCuaToi(idKhachHienTai(), req);
        Map<String, Object> kq = new LinkedHashMap<>(authService.phienMoiCuaKhach(kh));
        kq.put("hoSo", hoSo(kh));
        return kq;
    }

    /** Tra cứu đơn hàng của chính khách đang đăng nhập (đơn của người khác báo không tìm thấy). */
    @GetMapping("/don-hang/{maHoaDon}")
    public Map<String, Object> traCuuDon(@PathVariable String maHoaDon) {
        return cuaHangService.traCuuDonHangCuaKhach(maHoaDon, idKhachHienTai());
    }

    /** Khách tự hủy đơn của mình, chỉ khi đơn còn "Chờ xác nhận" (sau đó phải liên hệ cửa hàng). */
    @PutMapping("/don-hang/{maHoaDon}/huy")
    public Map<String, Object> huyDon(@PathVariable String maHoaDon,
                                      @RequestBody(required = false) Map<String, String> body) {
        String lyDo = body == null || body.get("lyDo") == null ? "" : body.get("lyDo").trim();
        if (lyDo.isEmpty()) throw ApiException.badRequest("Vui lòng chọn lý do hủy đơn.");
        if (lyDo.length() > 450) throw ApiException.badRequest("Lý do hủy tối đa 450 ký tự.");
        Map<String, Object> don = cuaHangService.traCuuDonHangCuaKhach(maHoaDon, idKhachHienTai());
        Object tt = don.get("trangThai");
        if (!(tt instanceof Number n) || n.intValue() != HoaDon.CHO_XAC_NHAN) {
            throw ApiException.badRequest("Đơn hàng đã được cửa hàng xử lý nên không thể tự hủy. Vui lòng liên hệ cửa hàng.");
        }
        hoaDonService.doiTrangThai((Integer) don.get("id"),
                new HoaDonTrangThaiRequest(HoaDon.DA_HUY, "Khách hủy đơn - Lý do: " + lyDo, "Khách hàng"));
        return cuaHangService.traCuuDonHangCuaKhach(maHoaDon, idKhachHienTai());
    }

    /** Danh sách mã giảm giá khách đang dùng được / sắp dùng được ở giỏ hàng (công khai + tặng riêng cho khách này). */
    @GetMapping("/phieu-giam-gia")
    public java.util.List<Map<String, Object>> danhSachPhieu(@RequestParam(defaultValue = "0") long tongTien) {
        return cuaHangService.danhSachPhieuChoKhach(tongTien, idKhachHienTai());
    }

    /** Khách nhập mã giảm giá ở giỏ hàng: kiểm tra có dùng được cho đơn `tongTien` không và trả mức giảm. */
    @GetMapping("/phieu-giam-gia/{maPhieu}")
    public Map<String, Object> kiemTraPhieu(@PathVariable String maPhieu, @RequestParam(defaultValue = "0") long tongTien) {
        return cuaHangService.kiemTraPhieuChoKhach(maPhieu, tongTien, idKhachHienTai());
    }

    @PutMapping("/doi-mat-khau")
    public Map<String, Object> doiMatKhau(@RequestBody(required = false) DoiMatKhauRequest req) {
        idKhachHienTai();
        return authService.doiMatKhauKhach(req);
    }
}
