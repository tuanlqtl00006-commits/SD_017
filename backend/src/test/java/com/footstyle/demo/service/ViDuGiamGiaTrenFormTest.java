package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Các "ví dụ" trên form Đợt giảm giá ("Ví dụ chạy thử: hai đợt cùng áp cho một biến thể") và form Phiếu giảm giá ("Ví dụ với
 * phiếu này") phải ra đúng số của backend: gộp % khi nhiều đợt chồng nhau (GiamGiaChongDot.gop) và giá sau giảm của biến thể
 * (LichGiamGiaService.giaSauGiam, cùng công thức tinhGiaSauGiam ở frontend). Frontend kiểm tra cùng các số này trong
 * frontend/tests/viDuGiamGia.test.js.
 */
class ViDuGiamGiaTrenFormTest {

    @Test
    void viDuHaiDotChongNhau_tren_formDotGiamGia() {
        // Thẻ "Khi nhiều đợt giảm giá chồng nhau": giá 3.000.000, đợt A 10%, đợt B 15%
        List<Integer> ab = List.of(10, 15);
        assertEquals(15.0, GiamGiaChongDot.gop(ab, CheDo.MAX));
        assertEquals(25.0, GiamGiaChongDot.gop(ab, CheDo.CONG_DON));
        assertEquals(12.5, GiamGiaChongDot.gop(ab, CheDo.TRUNG_BINH));
        assertEquals(2_550_000L, LichGiamGiaService.giaSauGiam(3_000_000, GiamGiaChongDot.gop(ab, CheDo.MAX)));
        assertEquals(2_250_000L, LichGiamGiaService.giaSauGiam(3_000_000, GiamGiaChongDot.gop(ab, CheDo.CONG_DON)));
        assertEquals(2_625_000L, LichGiamGiaService.giaSauGiam(3_000_000, GiamGiaChongDot.gop(ab, CheDo.TRUNG_BINH)));
        // 70% + 60%: cộng dồn chặn ở 100% (giá 0 ₫), trung bình 65%
        List<Integer> lon = List.of(70, 60);
        assertEquals(100.0, GiamGiaChongDot.gop(lon, CheDo.CONG_DON));
        assertEquals(0L, LichGiamGiaService.giaSauGiam(1_000_000, GiamGiaChongDot.gop(lon, CheDo.CONG_DON)));
        assertEquals(65.0, GiamGiaChongDot.gop(lon, CheDo.TRUNG_BINH));
        assertEquals(300_000L, LichGiamGiaService.giaSauGiam(1_000_000, GiamGiaChongDot.gop(lon, CheDo.MAX)));
        // ba đợt: trung bình làm tròn 2 chữ số như frontend (gopPhanTram)
        assertEquals(39.67, GiamGiaChongDot.gop(List.of(39, 40, 40), CheDo.TRUNG_BINH));
        assertEquals(11.67, GiamGiaChongDot.gop(List.of(10, 10, 15), CheDo.TRUNG_BINH));
    }

    @Test
    void giaSauDot_trenFormPhieu_lamTronGiongFrontend() {
        // Cột "Tiền hàng sau đợt giảm giá" trong ví dụ của form phiếu (frontend/tests/viDuGiamGia.test.js, BANG_VI_DU)
        assertEquals(1_800_000L, LichGiamGiaService.giaSauGiam(2_000_000, 10));
        assertEquals(1_440_000L, LichGiamGiaService.giaSauGiam(1_600_000, 10));
        assertEquals(2_625_000L, LichGiamGiaService.giaSauGiam(3_000_000, 12.5));
        assertEquals(1_125_005L, LichGiamGiaService.giaSauGiam(1_250_005, 10), ".5 làm tròn lên");
        assertEquals(3_017L, LichGiamGiaService.giaSauGiam(5_000, 39.67));
        assertEquals(1_040L, LichGiamGiaService.giaSauGiam(1_050, 1));
        assertEquals(2_187_500L, LichGiamGiaService.giaSauGiam(2_500_000, 12.5));
        assertEquals(0L, LichGiamGiaService.giaSauGiam(2_000_000, 100));
        assertEquals(999_990L, LichGiamGiaService.giaSauGiam(999_990, 0));
    }

    @Test
    void giaSauGiam_khongBaoGioAm_vaKhongVuotGiaGoc() {
        long[] gias = {0, 1, 3, 5, 99, 1_050, 5_000, 1_250_005, 3_000_000, 99_999_999};
        double[] pts = {0, 0.5, 1, 12.5, 33.33, 39.67, 50, 66.67, 99.5, 100, 150, -5};
        for (long g : gias) {
            for (double p : pts) {
                long s = LichGiamGiaService.giaSauGiam(g, p);
                assertTrue(s >= 0 && s <= g, g + " giảm " + p + "% -> " + s);
            }
        }
    }
}
