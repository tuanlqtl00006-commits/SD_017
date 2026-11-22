package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichSuHoaDon;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Góp ý 22/11: QR chuyển khoản quá 5 phút mà khách muốn chuyển khoản lại thì gửi thư có link thanh toán;
 * mã QR đếm lại 5 phút từ lúc gửi (mốc lấy từ lịch sử "Gửi lại link thanh toán", không thêm cột CSDL).
 */
class HanChuyenKhoanGuiLaiTest {

    private static final LocalDateTime DAT = LocalDateTime.of(2026, 11, 22, 10, 0);

    private static HoaDon donChuyenKhoan() {
        HoaDon hd = new HoaDon();
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        hd.setIdPhuongThucThanhToan(HoaDonSuaService.CHUYEN_KHOAN_ONLINE);
        hd.setNgayTao(DAT);
        return hd;
    }

    private static LichSuHoaDon ls(String hanhDong, LocalDateTime luc) {
        LichSuHoaDon l = new LichSuHoaDon();
        l.setHanhDong(hanhDong);
        l.setThoiGian(luc);
        return l;
    }

    @Test
    void chuaGuiLai_demTuLucDat_guiLaiThiDemTuLanGuiGanNhat() {
        HoaDon hd = donChuyenKhoan();
        assertEquals(DAT, CuaHangService.batDauQr(hd, List.of()));
        List<LichSuHoaDon> lichSu = List.of(
                ls("Đặt hàng", DAT),
                ls(CuaHangService.GUI_LAI_LINK_THANH_TOAN, DAT.plusMinutes(8)),
                ls("Ghi chú khác", DAT.plusMinutes(9)),
                ls(CuaHangService.GUI_LAI_LINK_THANH_TOAN, DAT.plusMinutes(20)));
        assertEquals(DAT.plusMinutes(20), CuaHangService.batDauQr(hd, lichSu));
    }

    @Test
    void donTreo_tinhTheoMocGuiLai() {
        HoaDon hd = donChuyenKhoan();
        LocalDateTime guiLai = DAT.plusMinutes(10);
        assertTrue(CuaHangService.laDonTreo(hd, 0, DAT.plusMinutes(6)), "quá 5 phút từ lúc đặt");
        assertFalse(CuaHangService.laDonTreo(hd, 0, DAT.plusMinutes(12), guiLai), "gửi lại link: còn trong 5 phút mới");
        assertTrue(CuaHangService.laDonTreo(hd, 0, DAT.plusMinutes(15), guiLai), "hết 5 phút mới lại thành đơn treo");
        assertFalse(CuaHangService.laDonTreo(hd, 100_000, DAT.plusMinutes(30), guiLai), "đã nhận tiền thì không treo");
        hd.setIdPhuongThucThanhToan(CuaHangService.COD);
        assertFalse(CuaHangService.laDonTreo(hd, 0, DAT.plusMinutes(30), guiLai), "đã đổi sang COD");
    }

    @Test
    void anEmail_cheTruocDauA() {
        assertEquals("tu***@gmail.com", CuaHangService.anEmail("tuan.nguyen@gmail.com"));
        assertEquals("a***@x.vn", CuaHangService.anEmail("a@x.vn"));
    }
}
