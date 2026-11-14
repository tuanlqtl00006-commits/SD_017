package com.footstyle.demo.controller;

import com.footstyle.demo.service.CaiDatVanChuyenService;
import com.footstyle.demo.service.CuaHangService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cua-hang")
@RequiredArgsConstructor
public class CuaHangController {

    private final CuaHangService service;
    private final CaiDatVanChuyenService caiDatVanChuyen;

    @GetMapping("/san-pham")
    public List<Map<String, Object>> danhSachSanPham(
            @RequestParam(required = false) Integer idDanhMuc,
            @RequestParam(required = false) Integer idThuongHieu,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Long giaTu,
            @RequestParam(required = false) Long giaDen,
            @RequestParam(required = false) String sapXep
    ) {
        return service.danhSachSanPham(idDanhMuc, idThuongHieu, tuKhoa, giaTu, giaDen, sapXep);
    }

    @GetMapping("/san-pham/{id}")
    public Map<String, Object> chiTietSanPham(@PathVariable Integer id) {
        return service.chiTietSanPham(id);
    }

    @GetMapping("/danh-muc-va-thuong-hieu")
    public Map<String, Object> danhMucVaThuongHieu() {
        return service.danhMucVaThuongHieu();
    }

    @GetMapping("/khuyen-mai")
    public List<Map<String, Object>> sanPhamKhuyenMai() {
        return service.sanPhamKhuyenMai();
    }

    /** Các đợt giảm giá đang diễn ra (trang Ưu đãi của khách). */
    @GetMapping("/dot-giam-gia")
    public List<Map<String, Object>> dotGiamGiaDangChay() {
        return service.dotGiamGiaDangChay();
    }

    @GetMapping("/phieu-giam-gia")
    public List<Map<String, Object>> phieuGiamGia(@RequestParam(required = false) Long tongTien) {
        return service.phieuGiamGiaCongKhai(tongTien);
    }

    @PostMapping("/dat-hang")
    public Map<String, Object> datHangOnline(@RequestBody CuaHangService.DatHangOnlineRequest req) {
        return service.datHangOnline(req);
    }

    /** Tính tiền giỏ hàng như lúc đặt (giá đợt hiện tại, phiếu, phí giao) để giỏ hàng / thanh toán hiện đúng số máy chủ thu. */
    @PostMapping("/tinh-tien")
    public Map<String, Object> tinhTien(@RequestBody(required = false) CuaHangService.TinhTienRequest req) {
        return service.tinhTien(req);
    }

    @GetMapping("/tra-cuu")
    public Map<String, Object> traCuuDonHang(
            @RequestParam String maHoaDon,
            @RequestParam(required = false) String sdt
    ) {
        return service.traCuuDonHang(maHoaDon, sdt);
    }

    /** Tra cứu theo số điện thoại: danh sách đơn gọn của số đó; xem chi tiết từng đơn thì dùng /tra-cuu (mã đơn + SĐT). */
    @GetMapping("/tra-cuu-sdt")
    public Map<String, Object> traCuuTheoSdt(@RequestParam(required = false) String sdt) {
        return service.traCuuTheoSdt(sdt);
    }

    // Tài khoản nhận tiền hiển thị trong mã VietQR (đổi bằng biến môi trường / application.properties).
    // Mặc định là tài khoản MẪU để thử giao diện, KHÔNG nhận tiền thật.
    @Value("${app.thanh-toan.ngan-hang:MB}")
    private String nganHang;
    @Value("${app.thanh-toan.so-tai-khoan:0123456789}")
    private String soTaiKhoan;
    @Value("${app.thanh-toan.ten-chu-tai-khoan:FOOTSTYLE DEMO}")
    private String tenChuTaiKhoan;

    /** Cài đặt miễn phí vận chuyển theo giá trị đơn ({ mienPhiBat, mienPhiTu }) để giỏ hàng, màn quầy, sửa đơn hiện đúng trước khi gửi lên. */
    @GetMapping("/cai-dat-van-chuyen")
    public Map<String, Object> caiDatVanChuyen() {
        return caiDatVanChuyen.moTa();
    }

    @GetMapping("/thong-tin-thanh-toan")
    public Map<String, Object> thongTinThanhToan() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nganHang", nganHang);
        m.put("soTaiKhoan", soTaiKhoan);
        m.put("tenChuTaiKhoan", tenChuTaiKhoan);
        return m;
    }
}
