package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.footstyle.demo.entity.HoaDon;
import org.junit.jupiter.api.Test;

/** Cô review 14/11 [11:32]: tab Đơn tại quầy ghi rõ hình thức mua (suy ra từ dữ liệu sẵn có, không thêm cột). */
class HinhThucMuaTest {

    @Test
    void suyRaTuLoaiDonVaNoiTao() {
        assertEquals("Mua tại quầy", HoaDonService.hinhThucMua(HoaDon.TAI_QUAY, false));
        assertEquals("Mua tại quầy", HoaDonService.hinhThucMua(null, false));
        assertEquals("Giao tận nơi", HoaDonService.hinhThucMua(HoaDon.GIAO_HANG, true), "bán quầy có bật giao hàng");
        assertEquals("Đặt online", HoaDonService.hinhThucMua(HoaDon.GIAO_HANG, false));
    }
}
