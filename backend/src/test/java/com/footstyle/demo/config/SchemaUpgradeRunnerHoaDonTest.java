package com.footstyle.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Đơn tại quầy không có phí vận chuyển / đơn vị vận chuyển (cô: "tại quầy sao lại có phí vận chuyển").
 * Dữ liệu cũ còn sót được dọn ở HAI nơi làm cùng một việc: PHẦN 8.1 của database/SD_17_du_lieu_va_nang_cap.sql (chạy tay)
 * và SchemaUpgradeRunner (backend tự chạy khi khởi động). Test này giữ cho hai nơi không bao giờ lệch nhau.
 */
class SchemaUpgradeRunnerHoaDonTest {

    private static String gon(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Tìm file SQL: chạy từ thư mục backend (Maven), từ thư mục gốc dự án (IDE) hoặc theo biến môi trường FOOTSTYLE_GOC. */
    private static Path timFileSql() {
        String ten = "SD_17_du_lieu_va_nang_cap.sql";
        String goc = System.getenv("FOOTSTYLE_GOC");
        List<Path> ungVien = goc != null && !goc.isBlank()
                ? List.of(Path.of(goc, "database", ten))
                : List.of(Path.of("..", "database", ten), Path.of("database", ten));
        for (Path p : ungVien) {
            if (Files.isRegularFile(p)) return p;
        }
        throw new AssertionError("Không tìm thấy " + ten + " (đã thử " + ungVien + "). Chạy test từ thư mục backend hoặc đặt FOOTSTYLE_GOC.");
    }

    private static String lenhDonTaiQuay() {
        List<String> ds = SchemaUpgradeRunner.LENH.stream().filter(s -> s.startsWith("UPDATE hoa_don SET")).toList();
        assertEquals(1, ds.size(), "SchemaUpgradeRunner phải có đúng 1 lệnh dọn đơn tại quầy");
        return ds.get(0);
    }

    @Test
    void lenhDonTaiQuay_giongHetPhan8_1TrongFileSql() throws IOException {
        String sql = gon(Files.readString(timFileSql(), StandardCharsets.UTF_8));
        String phan8 = sql.substring(sql.indexOf("-- PHẦN 8:"));
        assertTrue(phan8.contains(gon(lenhDonTaiQuay()) + ";"),
                "Lệnh trong SchemaUpgradeRunner khác PHẦN 8.1 của file SQL - hãy sửa cho giống nhau:\n" + lenhDonTaiQuay());
    }

    @Test
    void lenhDonTaiQuay_chiDungDonTaiQuay_boPhiVaDonViVanChuyen_khongLamAmThanhTien() {
        String lenh = gon(lenhDonTaiQuay());
        assertTrue(lenh.contains("WHERE loai_hoa_don = 1"), "chỉ đụng tới đơn tại quầy (loai_hoa_don = 1), không đụng đơn giao hàng");
        assertTrue(lenh.contains("phi_van_chuyen = 0"));
        assertTrue(lenh.contains("don_vi_van_chuyen = NULL"));
        assertTrue(lenh.contains("thanh_tien - ISNULL(phi_van_chuyen, 0) ELSE 0 END"), "thành tiền trừ lại phí đã cộng và không bao giờ âm");
        assertTrue(lenh.contains("ISNULL(phi_van_chuyen, 0) <> 0 OR don_vi_van_chuyen IS NOT NULL"), "đơn đã sạch thì không động tới (chạy lại nhiều lần an toàn)");
    }

    @Test
    void phan8_themHoaDonMauGiaoHangThatBai_coDayDuChiTietVaLichSu() throws IOException {
        String sql = Files.readString(timFileSql(), StandardCharsets.UTF_8);
        String phan8 = sql.substring(sql.indexOf("-- PHẦN 8:"));
        assertTrue(phan8.contains("NOT EXISTS (SELECT 1 FROM hoa_don WHERE ma_hoa_don = 'HD19')"), "không tạo lại HD19 khi chạy lần hai");
        assertTrue(phan8.contains("'HD19', 2,"), "HD19 là đơn giao hàng (loai_hoa_don = 2)");
        assertTrue(phan8.contains("DATEADD(MINUTE, 2000, @tao), 8"), "HD19 ở trạng thái 8 = Giao hàng thất bại");
        // chữ ở cột hanh_dong phải khớp bảng tên của backend để dòng thời gian nhận ra từng bước
        for (String ten : List.of("Tạo đơn hàng", "Đã xác nhận", "Chờ giao hàng", "Đang giao hàng", "Giao hàng thất bại")) {
            assertTrue(phan8.contains("N'" + ten + "'"), "thiếu bước lịch sử: " + ten);
        }
        assertTrue(phan8.contains("INSERT INTO hoa_don_chi_tiet"), "HD19 phải có dòng sản phẩm");
    }
}
