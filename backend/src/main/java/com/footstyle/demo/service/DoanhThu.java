package com.footstyle.demo.service;

import com.footstyle.demo.entity.HoaDon;
import java.math.BigDecimal;

/**
 * Doanh thu của một hóa đơn = số tiền khách trả cho HÀNG (đã trừ giảm giá), KHÔNG tính phí vận chuyển
 * (cô: đơn 500.000 giảm 10% còn 450.000, phí ship 30.000 thì doanh thu là 450.000, không phải 480.000).
 * Đơn tại quầy không có phí ship; phí ship còn sót trong dữ liệu cũ của đơn tại quầy cũng được bỏ ra.
 */
public final class DoanhThu {

    private DoanhThu() {
    }

    public static BigDecimal cua(HoaDon h) {
        BigDecimal thanhTien = h.getThanhTien() != null ? h.getThanhTien() : BigDecimal.ZERO;
        BigDecimal phi = h.getPhiVanChuyen() != null ? h.getPhiVanChuyen() : BigDecimal.ZERO;
        return thanhTien.subtract(phi).max(BigDecimal.ZERO);
    }
}
