package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.exception.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Cài đặt vận chuyển: phí giao hàng mỗi đơn + miễn phí theo giá trị đơn (Cài đặt → Cài đặt free ship). */
class CaiDatVanChuyenServiceTest {

    @Test
    void datNguong_thiMienPhi_duoiNguong_thiGiuPhiThuong() {
        CaiDatVanChuyenService s = CaiDatVanChuyenService.trongBoNho(true, 2_000_000L);
        assertFalse(s.mienPhi(1_999_999L), "thiếu 1 đồng thì chưa miễn phí");
        assertTrue(s.mienPhi(2_000_000L), "đúng bằng ngưỡng là được miễn phí");
        assertTrue(s.mienPhi(160_000_000L));
        assertEquals(40_000L, s.phi(1_999_999L, 40_000L));
        assertEquals(0L, s.phi(2_000_000L, 40_000L));
        assertEquals(0L, s.phi(160_000_000L, 70_000L), "đạt ngưỡng thì không thu, kể cả phí nhân viên nhập");
    }

    @Test
    void dangTat_thiKhongBaoGioMienPhiTheoGiaTriDon() {
        CaiDatVanChuyenService s = CaiDatVanChuyenService.trongBoNho(false, 2_000_000L);
        assertFalse(s.mienPhi(160_000_000L));
        assertEquals(40_000L, s.phi(160_000_000L, 40_000L));
    }

    @Test
    void chuaCoTep_layMacDinh_luuXong_coHieuLucNgay_vaConSauKhiKhoiDongLai(@TempDir Path thuMuc) throws Exception {
        CaiDatVanChuyenService s = new CaiDatVanChuyenService(thuMuc.toString(), true, 2_000_000L, 25_000L);
        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 2_000_000L, "phiGiaoHang", 25_000L), s.moTa(), "chưa có tệp thì lấy mặc định ở application.properties");
        assertFalse(Files.exists(thuMuc.resolve("van-chuyen.json")), "chỉ đọc thì không tạo tệp");

        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 5_000_000L, "phiGiaoHang", 25_000L), s.luu(true, 5_000_000L));
        assertFalse(s.mienPhi(4_999_999L));
        assertTrue(s.mienPhi(5_000_000L), "lưu xong dùng ngay, không phải khởi động lại");
        assertTrue(Files.exists(thuMuc.resolve("van-chuyen.json")));

        assertEquals(null, new CaiDatVanChuyenService(thuMuc.resolve("chua-co").toString(), true, 2_000_000L, 25_000L).moTaQuanLy().get("capNhatLuc"),
                "chưa ai sửa thì chưa có thời điểm cập nhật");
        String luc = (String) s.moTaQuanLy().get("capNhatLuc");
        assertTrue(luc != null && luc.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?"), "ghi lại thời điểm quản lý lưu: " + luc);

        CaiDatVanChuyenService moLai = new CaiDatVanChuyenService(thuMuc.toString(), false, 1L, 25_000L);
        assertEquals(luc, moLai.moTaQuanLy().get("capNhatLuc"), "thời điểm cập nhật cũng còn sau khi khởi động lại");
        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 5_000_000L, "phiGiaoHang", 25_000L), moLai.moTa(), "khởi động lại vẫn còn cài đặt đã lưu");
    }

    @Test
    void tat_khongGuiNguong_giuNguongCu(@TempDir Path thuMuc) {
        CaiDatVanChuyenService s = new CaiDatVanChuyenService(thuMuc.toString(), true, 2_000_000L, 25_000L);
        assertEquals(Map.of("mienPhiBat", false, "mienPhiTu", 2_000_000L, "phiGiaoHang", 25_000L), s.luu(false, null));
        assertFalse(s.mienPhi(160_000_000L));
    }

    @Test
    void kiemTraDuLieu_loiThiGiuCaiDatCu(@TempDir Path thuMuc) {
        CaiDatVanChuyenService s = new CaiDatVanChuyenService(thuMuc.toString(), true, 2_000_000L, 25_000L);
        ApiException thieu = assertThrows(ApiException.class, () -> s.luu(true, null));
        assertEquals("Vui lòng nhập giá trị đơn tối thiểu để được miễn phí vận chuyển.", thieu.getLoiTheoTruong().get("mienPhiTu"));
        ApiException khong = assertThrows(ApiException.class, () -> s.luu(true, 0L));
        assertEquals("Giá trị đơn tối thiểu phải lớn hơn 0.", khong.getLoiTheoTruong().get("mienPhiTu"));
        assertThrows(ApiException.class, () -> s.luu(true, -5L));
        ApiException lon = assertThrows(ApiException.class, () -> s.luu(true, CaiDatVanChuyenService.NGUONG_TOI_DA + 1));
        assertEquals("Số tiền quá lớn.", lon.getLoiTheoTruong().get("mienPhiTu"));
        ApiException chuaChon = assertThrows(ApiException.class, () -> s.luu(null, 3_000_000L));
        assertTrue(chuaChon.getLoiTheoTruong().containsKey("mienPhiBat"));
        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 2_000_000L, "phiGiaoHang", 25_000L), s.moTa(), "gửi sai thì cài đặt cũ vẫn nguyên");
    }

    @Test
    void tepHong_hoacNguongSai_thiDungMacDinh(@TempDir Path thuMuc) throws Exception {
        Files.writeString(thuMuc.resolve("van-chuyen.json"), "{ day khong phai json");
        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 2_000_000L, "phiGiaoHang", 25_000L), new CaiDatVanChuyenService(thuMuc.toString(), true, 2_000_000L, 25_000L).moTa());
        Files.writeString(thuMuc.resolve("van-chuyen.json"), "{\"mienPhiBat\": true, \"mienPhiTu\": -10}");
        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 2_000_000L, "phiGiaoHang", 25_000L), new CaiDatVanChuyenService(thuMuc.toString(), false, 2_000_000L, 25_000L).moTa(),
                "ngưỡng âm trong tệp bị bỏ, lấy ngưỡng mặc định; bật / tắt vẫn theo tệp");
    }

    /** Góp ý 16/11: bỏ bảng phí theo tỉnh, phí giao là một mức quản lý cài, lưu cùng tệp, sai thì giữ phí cũ. */
    @Test
    void phiGiaoHang_quanLyCai_luuDuoc_kiemTraDuLieu(@TempDir Path thuMuc) {
        CaiDatVanChuyenService s = new CaiDatVanChuyenService(thuMuc.toString(), true, 2_000_000L, 25_000L);
        assertEquals(25_000L, s.phiGiaoHang());
        assertEquals(25_000L, s.phi(1_999_999L));
        assertEquals(0L, s.phi(2_000_000L), "đạt ngưỡng thì miễn phí");
        assertEquals(Map.of("mienPhiBat", true, "mienPhiTu", 2_000_000L, "phiGiaoHang", 40_000L), s.luu(true, 2_000_000L, 40_000L));
        assertEquals(40_000L, s.phi(500_000L), "lưu xong dùng ngay");
        assertEquals(40_000L, new CaiDatVanChuyenService(thuMuc.toString(), false, 1L, 1L).phiGiaoHang(), "khởi động lại vẫn còn phí đã lưu");
        assertEquals(40_000L, s.luu(false, null).get("phiGiaoHang"), "lưu không gửi phí thì giữ phí cũ");
        assertEquals(0L, s.luu(false, null, 0L).get("phiGiaoHang"), "phí 0 nhận được (giao miễn phí mọi đơn)");
        ApiException am = assertThrows(ApiException.class, () -> s.luu(true, 2_000_000L, -1L));
        assertEquals("Phí giao hàng không được âm.", am.getLoiTheoTruong().get("phiGiaoHang"));
        ApiException lon = assertThrows(ApiException.class, () -> s.luu(true, 2_000_000L, CaiDatVanChuyenService.NGUONG_TOI_DA + 1));
        assertEquals("Số tiền quá lớn.", lon.getLoiTheoTruong().get("phiGiaoHang"));
        assertEquals(0L, s.phiGiaoHang(), "gửi sai thì phí cũ vẫn nguyên");
    }
}
