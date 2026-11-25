package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Góp ý 25/11: tên phương thức thanh toán ghi rõ kênh, giống nhau ở chi tiết hóa đơn, lịch sử thanh toán, bản in và Excel. */
class TenHinhThucThanhToanTest {

    @Test
    void chuyenKhoan_online_laVietQR_taiQuay_laNganHang() {
        assertEquals("Chuyển khoản VietQR", HoaDonService.tenHinhThucThanhToan(2, false));
        assertEquals("Chuyển khoản ngân hàng", HoaDonService.tenHinhThucThanhToan(2, true));
    }

    @Test
    void cacPhuongThucKhac() {
        assertEquals("Tiền mặt", HoaDonService.tenHinhThucThanhToan(1, true));
        assertEquals("Thanh toán khi nhận hàng (COD)", HoaDonService.tenHinhThucThanhToan(1, false));
        assertEquals("Thanh toán khi nhận hàng (COD)", HoaDonService.tenHinhThucThanhToan(3, false));
        assertEquals("Cổng thanh toán VNPay", HoaDonService.tenHinhThucThanhToan(4, false));
        assertEquals("Tiền mặt + chuyển khoản ngân hàng", HoaDonService.tenHinhThucThanhToan(5, true));
        assertEquals("Ví điện tử MoMo", HoaDonService.tenHinhThucThanhToan(6, false));
        assertNull(HoaDonService.tenHinhThucThanhToan(null, true));
    }
}
