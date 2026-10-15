package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import com.footstyle.demo.service.GiamGiaChongDot.Doan;
import com.footstyle.demo.service.GiamGiaChongDot.DotApDung;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Đợt giảm giá chồng nhau theo NGÀY GIỜ. Ví dụ của nhóm:
 * A: 10%, 01/10 08:00 - 10/10 17:00; B: 15%, 04/10 12:00 - 20/10 23:59.
 */
class GiamGiaChongDotTest {

    private static LocalDateTime t(int ngay, int gio, int phut, int giay) {
        return LocalDateTime.of(2026, 10, ngay, gio, phut, giay);
    }

    private static final DotApDung A = new DotApDung(1L, t(1, 8, 0, 0), t(10, 17, 0, 59), 10);
    private static final DotApDung B = new DotApDung(2L, t(4, 12, 0, 0), t(20, 23, 59, 59), 15);

    @Test
    void gopTheoBaCheDo_voi10Va15() {
        assertEquals(15, GiamGiaChongDot.gop(List.of(10, 15), CheDo.MAX));
        assertEquals(25, GiamGiaChongDot.gop(List.of(10, 15), CheDo.CONG_DON));
        assertEquals(12.5, GiamGiaChongDot.gop(List.of(10, 15), CheDo.TRUNG_BINH));
        assertEquals(0, GiamGiaChongDot.gop(List.of(), CheDo.MAX));
        assertEquals(0, GiamGiaChongDot.gop(null, CheDo.CONG_DON));
    }

    @Test
    void congDonKhongBaoGioVuot100() {
        assertEquals(100, GiamGiaChongDot.gop(List.of(60, 70, 30), CheDo.CONG_DON));
        assertEquals(90, GiamGiaChongDot.gop(List.of(60, 30), CheDo.CONG_DON));
    }

    @Test
    void docCheDoTuCauHinh() {
        assertEquals(CheDo.MAX, CheDo.tuChuoi(null));
        assertEquals(CheDo.MAX, CheDo.tuChuoi("  max "));
        assertEquals(CheDo.MAX, CheDo.tuChuoi("khong-biet"));
        assertEquals(CheDo.CONG_DON, CheDo.tuChuoi("cong-don"));
        assertEquals(CheDo.CONG_DON, CheDo.tuChuoi("CONG_DON"));
        assertEquals(CheDo.CONG_DON, CheDo.tuChuoi("sum"));
        assertEquals(CheDo.TRUNG_BINH, CheDo.tuChuoi("trung-binh"));
        assertEquals(CheDo.TRUNG_BINH, CheDo.tuChuoi("TBC"));
    }

    @Test
    void moiCheDoCoCauGiaiThichRieng() {
        assertTrue(CheDo.MAX.moTa().contains("CAO NHẤT"));
        assertTrue(CheDo.CONG_DON.moTa().contains("CỘNG DỒN"));
        assertTrue(CheDo.TRUNG_BINH.moTa().contains("TRUNG BÌNH"));
    }

    @Test
    void phanTramTaiThoiDiem_tinhToiTungGio() {
        List<DotApDung> dots = List.of(A, B);
        assertEquals(0, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(1, 7, 59, 59), CheDo.MAX), "trước giờ bắt đầu của đợt A");
        assertEquals(10, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(1, 8, 0, 0), CheDo.MAX), "đúng giờ bắt đầu");
        assertEquals(10, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(4, 11, 59, 59), CheDo.MAX), "một giây trước khi đợt B bắt đầu");
        assertEquals(15, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(4, 12, 0, 0), CheDo.MAX), "A và B chồng nhau, lấy cao nhất");
        assertEquals(25, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(4, 12, 0, 0), CheDo.CONG_DON));
        assertEquals(12.5, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(7, 0, 0, 0), CheDo.TRUNG_BINH));
        assertEquals(15, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(10, 17, 0, 59), CheDo.MAX), "giây cuối của đợt A vẫn còn chồng");
        assertEquals(15, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(10, 17, 1, 0), CheDo.MAX), "A đã hết, chỉ còn B");
        assertEquals(15, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(10, 17, 1, 0), CheDo.CONG_DON), "chỉ còn B thì cộng dồn cũng là 15");
        assertEquals(15, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(20, 23, 59, 59), CheDo.MAX));
        assertEquals(0, GiamGiaChongDot.phanTramTaiThoiDiem(dots, t(21, 0, 0, 0), CheDo.MAX), "đã hết đợt B");
    }

    @Test
    void lich_baDoanDungGioPhut_theoMax() {
        List<Doan> lich = GiamGiaChongDot.lich(List.of(A, B), CheDo.MAX);
        assertEquals(3, lich.size());
        assertEquals(t(1, 8, 0, 0), lich.get(0).tuLuc());
        assertEquals(t(4, 11, 59, 59), lich.get(0).denLuc());
        assertEquals(10, lich.get(0).phanTram());
        assertFalse(lich.get(0).trungDot());

        assertEquals(t(4, 12, 0, 0), lich.get(1).tuLuc());
        assertEquals(t(10, 17, 0, 59), lich.get(1).denLuc());
        assertEquals(15, lich.get(1).phanTram(), "MAX của 10% và 15%");
        assertTrue(lich.get(1).trungDot());
        assertEquals(List.of(1L, 2L), lich.get(1).idDots());

        assertEquals(t(10, 17, 1, 0), lich.get(2).tuLuc());
        assertEquals(t(20, 23, 59, 59), lich.get(2).denLuc());
        assertEquals(15, lich.get(2).phanTram());
    }

    @Test
    void lich_congDonVaTrungBinhChiKhacOdoanChongNhau() {
        List<Doan> congDon = GiamGiaChongDot.lich(List.of(A, B), CheDo.CONG_DON);
        assertEquals(List.of(10.0, 25.0, 15.0), congDon.stream().map(Doan::phanTram).toList());
        List<Doan> tb = GiamGiaChongDot.lich(List.of(A, B), CheDo.TRUNG_BINH);
        assertEquals(List.of(10.0, 12.5, 15.0), tb.stream().map(Doan::phanTram).toList());
    }

    @Test
    void lich_haiDotLienKeCungMucThiGopThanhMotDoan_khoangTrongThiTachRa() {
        DotApDung d1 = new DotApDung(1L, t(1, 0, 0, 0), t(3, 23, 59, 59), 10);
        DotApDung d2 = new DotApDung(1L, t(4, 0, 0, 0), t(6, 23, 59, 59), 10); // cùng đợt (id), liền kề, cùng mức
        List<Doan> lien = GiamGiaChongDot.lich(List.of(d1, d2), CheDo.MAX);
        assertEquals(1, lien.size());
        assertEquals(t(1, 0, 0, 0), lien.get(0).tuLuc());
        assertEquals(t(6, 23, 59, 59), lien.get(0).denLuc());

        DotApDung cach = new DotApDung(1L, t(8, 0, 0, 0), t(9, 23, 59, 59), 10);
        List<Doan> trong = GiamGiaChongDot.lich(List.of(d1, cach), CheDo.MAX);
        assertEquals(2, trong.size(), "giữa 4/10 và 7/10 không giảm gì");
    }

    @Test
    void lich_haiDotCungNgayNhungKhacGio_khongChongNhau() {
        DotApDung sang = new DotApDung(1L, t(5, 8, 0, 0), t(5, 11, 59, 59), 10);
        DotApDung chieu = new DotApDung(2L, t(5, 14, 0, 0), t(5, 17, 59, 59), 30);
        List<Doan> lich = GiamGiaChongDot.lich(List.of(sang, chieu), CheDo.CONG_DON);
        assertEquals(2, lich.size());
        assertEquals(10, lich.get(0).phanTram());
        assertEquals(30, lich.get(1).phanTram());
        assertEquals(0, GiamGiaChongDot.phanTramTaiThoiDiem(List.of(sang, chieu), t(5, 12, 30, 0), CheDo.MAX), "giữa trưa không có đợt nào");
    }

    @Test
    void lich_khongCoDotThiRong() {
        assertTrue(GiamGiaChongDot.lich(List.of(), CheDo.MAX).isEmpty());
    }
}
