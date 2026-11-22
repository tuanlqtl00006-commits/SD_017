package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Góp ý 22/11: mã QR chuyển khoản hết 5 phút mà khách chưa trả thì hệ thống TỰ gửi 1 thư có link thanh toán
 * (ghi "Gửi lại link thanh toán" để mã QR đếm lại 5 phút). Mỗi đơn chỉ tự gửi 1 lần; đơn COD / đã trả / không email thì bỏ qua.
 */
class TuGuiLinkQrHetHanTest {

    private final Map<Integer, List<LichSuHoaDon>> lichSu = new HashMap<>();
    private final List<LichSuHoaDon> daGhi = new ArrayList<>();

    private HoaDon don(int id, Integer phuongThuc, String email) {
        HoaDon hd = new HoaDon();
        hd.setId(id);
        hd.setMaHoaDon("HD" + id);
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        hd.setIdPhuongThucThanhToan(phuongThuc);
        hd.setNgayTao(LocalDateTime.now().minusMinutes(6));
        hd.setThanhTien(BigDecimal.valueOf(500_000));
        List<LichSuHoaDon> ls = new ArrayList<>();
        if (email != null) {
            LichSuHoaDon dat = new LichSuHoaDon();
            dat.setHanhDong("Đặt hàng");
            dat.setMoTa("Khách đặt online. " + MailService.NHAN_EMAIL_DON + email);
            dat.setThoiGian(hd.getNgayTao());
            ls.add(dat);
        }
        lichSu.put(id, ls);
        return hd;
    }

    @Test
    void hetHan_tuGuiMotLan_boQuaDonCodDaTraDaGuiVaKhongEmail() {
        HoaDon canGui = don(1, HoaDonSuaService.CHUYEN_KHOAN_ONLINE, "khach@gmail.com");
        HoaDon cod = don(2, CuaHangService.COD, "cod@gmail.com");
        HoaDon daTra = don(3, HoaDonSuaService.CHUYEN_KHOAN_ONLINE, "tra@gmail.com");
        HoaDon daGui = don(4, HoaDonSuaService.CHUYEN_KHOAN_ONLINE, "gui@gmail.com");
        LichSuHoaDon cu = new LichSuHoaDon();
        cu.setHanhDong(CuaHangService.GUI_LAI_LINK_THANH_TOAN);
        cu.setThoiGian(LocalDateTime.now().minusMinutes(1));
        lichSu.get(4).add(cu);
        HoaDon khongEmail = don(5, HoaDonSuaService.CHUYEN_KHOAN_ONLINE, null);
        LichSuThanhToan tien = new LichSuThanhToan();
        tien.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        tien.setSoTien(BigDecimal.valueOf(500_000));
        List<HoaDon> ds = List.of(canGui, cod, daTra, daGui, khongEmail);
        LocalDateTime[] khoangQuet = new LocalDateTime[2];

        CuaHangService service = new CuaHangService(
                KhoGia.khongDung(SanPhamRepository.class), KhoGia.khongDung(SanPhamChiTietRepository.class),
                KhoGia.khongDung(HinhAnhSanPhamRepository.class), KhoGia.khongDung(DanhMucRepository.class),
                KhoGia.khongDung(ThuongHieuRepository.class), KhoGia.khongDung(XuatXuRepository.class),
                KhoGia.khongDung(ChatLieuRepository.class), KhoGia.khongDung(DoCungRepository.class),
                KhoGia.khongDung(DiemCanBangRepository.class), KhoGia.khongDung(MauSacRepository.class),
                KhoGia.khongDung(TrongLuongRepository.class), KhoGia.khongDung(ChuViRepository.class),
                KhoGia.tao(HoaDonRepository.class, (t, a) -> {
                    if ("findByTrangThaiAndNgayTaoBetween".equals(t) && Integer.valueOf(HoaDon.CHO_XAC_NHAN).equals(a[0])) {
                        khoangQuet[0] = (LocalDateTime) a[1];
                        khoangQuet[1] = (LocalDateTime) a[2];
                        return ds;
                    }
                    return KhoGia.KHONG_CO;
                }),
                KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> "findByHoaDon_IdOrderByIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                KhoGia.tao(LichSuHoaDonRepository.class, (t, a) -> switch (t) {
                    case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> new ArrayList<>(lichSu.getOrDefault((Integer) a[0], List.of()));
                    case "save" -> {
                        LichSuHoaDon l = (LichSuHoaDon) a[0];
                        daGhi.add(l);
                        lichSu.get(l.getHoaDon().getId()).add(l);
                        yield l;
                    }
                    default -> KhoGia.KHONG_CO;
                }),
                KhoGia.tao(LichSuThanhToanRepository.class, (t, a) -> "findByHoaDon_IdOrderByThoiGianAscIdAsc".equals(t)
                        ? (Integer.valueOf(3).equals(a[0]) ? List.of(tien) : List.of()) : KhoGia.KHONG_CO),
                KhoGia.khongDung(PhieuGiamGiaRepository.class), KhoGia.khongDung(PhieuGiamGiaKhachHangRepository.class),
                KhoGia.khongDung(KhachHangRepository.class), null);

        service.tuGuiLinkKhiQrHetHan();

        assertEquals(1, daGhi.size(), "chỉ đơn chuyển khoản chưa trả, có email, chưa gửi lại: " + daGhi);
        LichSuHoaDon ghi = daGhi.get(0);
        assertEquals(1, ghi.getHoaDon().getId());
        assertEquals(CuaHangService.GUI_LAI_LINK_THANH_TOAN, ghi.getHanhDong());
        assertEquals("Hệ thống", ghi.getNguoiThaoTac());
        assertTrue(ghi.getMoTa().contains("tự gửi link thanh toán tới kh***@gmail.com"), ghi.getMoTa());
        // Chỉ quét đơn đặt trong 30 phút gần nhất và đã quá 5 phút
        assertEquals(25, java.time.Duration.between(khoangQuet[0], khoangQuet[1]).toMinutes());

        // Lần quét sau: đơn 1 đã có "Gửi lại link thanh toán" nên không gửi thêm
        service.tuGuiLinkKhiQrHetHan();
        assertEquals(1, daGhi.size());
    }
}
