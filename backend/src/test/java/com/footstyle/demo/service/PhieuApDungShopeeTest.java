package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.footstyle.demo.entity.PhieuGiamGia;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 24/11 (Task 2.2): công thức tiền giảm của phiếu kiểu Shopee - tính trên tiền hàng sau đợt giảm, không gồm phí ship. */
class PhieuApDungShopeeTest {

    private static final LocalDateTime LUC = LocalDateTime.of(2026, 11, 24, 20, 0);

    private static PhieuGiamGia phieu(int loai, long giaTri, Long toiDa, long donToiThieu) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setLoaiGiam(loai);
        p.setGiaTriGiam(giaTri);
        p.setGiamToiDa(toiDa);
        p.setGiaTriDonHangToiThieu(donToiThieu);
        p.setTrangThai(PhieuGiamGia.HOAT_DONG);
        p.setNgayBatDau(LUC.minusDays(1));
        p.setNgayKetThuc(LUC.plusDays(1));
        p.setSoLuong(10);
        p.setSoLuongDaDung(0);
        return p;
    }

    @Test
    void phanTram20_toiDa50k_donTu100k_dungBangTrongDacTa() {
        PhieuGiamGia p = phieu(PhieuGiamGia.LOAI_PHAN_TRAM, 20, 50_000L, 100_000);
        // 80.000: chưa đủ đơn tối thiểu -> 0 và có lý do
        assertEquals(0, PhieuApDung.soTienGiam(p, 80_000));
        assertNotNull(PhieuApDung.lyDoKhongDung(p, 80_000, LUC, false, false, 0));
        assertNull(PhieuApDung.lyDoKhongDung(p, 100_000, LUC, false, false, 0));
        assertEquals(20_000, PhieuApDung.soTienGiam(p, 100_000));
        assertEquals(50_000, PhieuApDung.soTienGiam(p, 250_000));
        assertEquals(50_000, PhieuApDung.soTienGiam(p, 300_000));
    }

    @Test
    void phanTramKhongCoToiDa_lamTronXuong_vaKhongVuotTienHang() {
        PhieuGiamGia p = phieu(PhieuGiamGia.LOAI_PHAN_TRAM, 15, null, 0);
        assertEquals(15_014, PhieuApDung.soTienGiam(p, 100_099)); // 15.014,85 -> 15.014
        assertEquals(1_000, PhieuApDung.soTienGiam(phieu(PhieuGiamGia.LOAI_PHAN_TRAM, 100, null, 0), 1_000));
    }

    @Test
    void giamTien_khongVuotTienHang_khongAm() {
        PhieuGiamGia p = phieu(2, 70_000, null, 0);
        assertEquals(70_000, PhieuApDung.soTienGiam(p, 300_000));
        assertEquals(50_000, PhieuApDung.soTienGiam(p, 50_000));
        assertEquals(0, PhieuApDung.soTienGiam(p, -5));
    }

    @Test
    void phieuNgungHoatDong_khongDungDuoc() {
        PhieuGiamGia p = phieu(PhieuGiamGia.LOAI_PHAN_TRAM, 20, 50_000L, 100_000);
        p.setTrangThai(PhieuGiamGia.NGUNG_HOAT_DONG);
        assertEquals("Phiếu đang ngừng hoạt động.", PhieuApDung.lyDoKhongDung(p, 300_000, LUC, false, false, 0));
    }
}
