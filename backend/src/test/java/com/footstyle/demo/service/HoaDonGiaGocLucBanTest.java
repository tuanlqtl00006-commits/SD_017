package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Review 05/11 (đồng bộ phần tiền): giá gốc LÚC BÁN của dòng hóa đơn. Bảng chi tiết chỉ lưu đơn giá; giá sản phẩm có thể đổi
 * sau khi bán, nên không được lấy thẳng giá hiện tại để tính "giảm theo đợt" cho đơn cũ.
 */
class HoaDonGiaGocLucBanTest {

    @Test
    void giaChuaDoi_coDotLucBan_layGiaHienTai() {
        assertEquals(450_000, HoaDonService.giaGocLucBan(292_500, 450_000, List.of(35.0)));
    }

    @Test
    void giaDaTangSauKhiBan_coDotLucBan_tinhNguocTuDonGia() {
        // bán 292.500 khi đợt 35% (giá gốc 450.000); sau đó giá tăng lên 500.000
        assertEquals(450_000, HoaDonService.giaGocLucBan(292_500, 500_000, List.of(35.0)));
    }

    @Test
    void giaDaTangSauKhiBan_khongCoDotLucBan_khongHienGiamAo() {
        assertEquals(450_000, HoaDonService.giaGocLucBan(450_000, 500_000, List.of()));
    }

    @Test
    void banKhongGiam_giaChuaDoi() {
        assertEquals(234_000, HoaDonService.giaGocLucBan(234_000, 234_000, List.of()));
    }

    @Test
    void dotDaBiTatSauNay_vanNhanRaNhoMucRiengCuaDot() {
        // mức gộp lúc này = 0 (đợt đã tắt) nhưng % riêng của đợt chứa ngày bán vẫn có trong danh sách
        assertEquals(4_000_000, HoaDonService.giaGocLucBan(3_400_000, 4_000_000, List.of(15.0)));
    }

    @Test
    void lamTronToiDong_lechMotDongVanKhop() {
        long ban = PhieuApDung.giaSauDot(5_000, 39.67);
        assertEquals(5_000, HoaDonService.giaGocLucBan(ban, 5_000, List.of(39.67)));
        assertEquals(5_000, HoaDonService.giaGocLucBan(ban + 1, 5_000, List.of(39.67)));
    }
}
