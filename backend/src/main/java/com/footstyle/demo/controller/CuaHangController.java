package com.footstyle.demo.controller;

import com.footstyle.demo.service.CaiDatVanChuyenService;
import com.footstyle.demo.service.CuaHangService;
import com.footstyle.demo.service.PhiVanChuyenService;
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
    private final PhiVanChuyenService phiVanChuyenService;

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

    /** Sản phẩm bán chạy cho trang chủ: tính từ đơn đã hoàn thành trong 30 ngày gần nhất. */
    @GetMapping("/ban-chay")
    public Map<String, Object> sanPhamBanChay() {
        return service.sanPhamBanChay();
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
        // 24/11 (Task 4.2): trang thanh toán hỏi trạng thái mỗi 4 giây - quá hạn 5 phút chưa trả thì hủy ngay (không chờ lượt quét)
        service.tuHuyNeuQuaHan(maHoaDon);
        return service.traCuuDonHang(maHoaDon, sdt);
    }

    /** Đơn chuyển khoản chưa trả tiền (thường là quá 5 phút, đơn treo): khách đổi sang thanh toán khi nhận hàng. Body: { sdt }. */
    @PostMapping("/don-hang/{maHoaDon}/doi-sang-cod")
    public Map<String, Object> doiSangCod(@PathVariable String maHoaDon, @RequestBody(required = false) Map<String, String> body) {
        return service.doiSangCod(maHoaDon, body == null ? null : body.get("sdt"));
    }

    /** Gửi lại email thông báo đơn chưa thanh toán kèm link quay lại màn hình thanh toán. Body: { sdt }. */
    @PostMapping("/don-hang/{maHoaDon}/gui-mail-thanh-toan")
    public Map<String, Object> guiMailThanhToan(@PathVariable String maHoaDon, @RequestBody(required = false) Map<String, String> body) {
        return service.guiMailThanhToan(maHoaDon, body == null ? null : body.get("sdt"));
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
        return phiVanChuyenService.themCachTinh(caiDatVanChuyen.moTa());
    }

    /**
     * Phí giao hàng tới nơi nhận theo quãng đường (API bản đồ miễn phí OpenStreetMap), dùng cho màn quầy và sửa giao hàng
     * hiện đúng số máy chủ sẽ thu. tienHang (tùy chọn): đạt ngưỡng free ship thì phí 0.
     */
    @GetMapping("/phi-van-chuyen")
    public Map<String, Object> phiVanChuyen(@RequestParam(required = false) String tinhThanh,
                                            @RequestParam(required = false) String phuongXa,
                                            @RequestParam(required = false) String diaChi,
                                            @RequestParam(required = false) Long tienHang) {
        // Không gửi tỉnh: diaChi là địa chỉ ghi liền ("số nhà, Phường X, Tỉnh Y" - màn sửa đơn); không nhận ra tỉnh thì chưa tính
        PhiVanChuyenService.KetQua kq = tinhThanh == null || tinhThanh.isBlank()
                ? phiVanChuyenService.tinhTheoDiaChi(diaChi)
                : phiVanChuyenService.tinh(tinhThanh, phuongXa, diaChi);
        if (kq == null) {
            Map<String, Object> rong = new LinkedHashMap<>();
            rong.put("phiVanChuyen", null);
            rong.put("soKm", null);
            rong.put("nguon", null);
            rong.put("mienPhi", false);
            return rong;
        }
        Map<String, Object> m = kq.moTa();
        boolean mienPhi = tienHang != null && caiDatVanChuyen.mienPhi(tienHang);
        m.put("mienPhi", mienPhi);
        if (mienPhi) m.put("phiVanChuyen", 0L);
        return m;
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
