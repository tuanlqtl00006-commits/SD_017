package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Review 05/11 (thầy bấm ra): chạy máy thầy không có biến môi trường thì trang thanh toán chuyển khoản báo "đang tắt" và nhân viên
 * không có nút xác nhận đã nhận tiền. Thanh toán giả lập phải bật sẵn trong application.properties khi không đặt APP_THANH_TOAN_GIA_LAP.
 */
@SpringBootTest
class ThanhToanGiaLapMacDinhTest {

    @Autowired
    private Environment env;

    @Test
    void thanhToanGiaLapBatSanKhiKhongDatBienMoiTruong() {
        if (System.getenv("APP_THANH_TOAN_GIA_LAP") != null) return; // máy này đã chủ động đặt giá trị khác
        assertEquals("true", env.getProperty("app.thanh-toan.gia-lap"));
    }
}
