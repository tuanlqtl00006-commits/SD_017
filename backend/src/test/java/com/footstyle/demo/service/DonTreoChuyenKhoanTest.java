package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.HoaDon;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Góp ý 14/11 (video 22:22 [04:52–08:06]): chuyển khoản có hạn 5 phút; quá hạn chưa nhận đồng nào thì là "đơn treo". */
class DonTreoChuyenKhoanTest {

    private static final LocalDateTime DAT = LocalDateTime.of(2026, 11, 15, 10, 0, 0);

    private static HoaDon don(Integer phuongThuc, int trangThai) {
        HoaDon hd = new HoaDon();
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setIdPhuongThucThanhToan(phuongThuc);
        hd.setTrangThai(trangThai);
        hd.setNgayTao(DAT);
        return hd;
    }

    @Test
    void quaNamPhutChuaTraTien_laDonTreo() {
        HoaDon hd = don(HoaDonSuaService.CHUYEN_KHOAN_ONLINE, HoaDon.CHO_XAC_NHAN);
        assertFalse(CuaHangService.laDonTreo(hd, 0, DAT.plusMinutes(4).plusSeconds(59)), "còn trong 5 phút");
        assertTrue(CuaHangService.laDonTreo(hd, 0, DAT.plusMinutes(5)));
        assertFalse(CuaHangService.laDonTreo(hd, 100_000, DAT.plusMinutes(30)), "đã nhận tiền");
    }

    @Test
    void codHoacDaXuLy_khongPhaiDonTreo() {
        assertFalse(CuaHangService.laDonTreo(don(CuaHangService.COD, HoaDon.CHO_XAC_NHAN), 0, DAT.plusHours(1)));
        assertFalse(CuaHangService.laDonTreo(don(HoaDonSuaService.CHUYEN_KHOAN_ONLINE, HoaDon.DA_HUY), 0, DAT.plusHours(1)));
        HoaDon taiQuay = don(HoaDonSuaService.CHUYEN_KHOAN_ONLINE, HoaDon.CHO_XAC_NHAN);
        taiQuay.setLoaiDon(HoaDon.TAI_QUAY);
        assertFalse(CuaHangService.laDonTreo(taiQuay, 0, DAT.plusHours(1)));
    }
}
