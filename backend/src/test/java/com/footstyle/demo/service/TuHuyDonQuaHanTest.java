package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 24/11 (Task 4): đơn online chuyển khoản quá lúc đặt + 5 phút chưa nhận tiền thì TỰ HỦY (quét định kỳ + kiểm tra lười khi tra cứu);
 * không còn gửi lại link / tạo mã QR mới / gia hạn. Thư nhắc thanh toán của nhân viên (Task 3.3) không đổi hạn 5 phút.
 */
class TuHuyDonQuaHanTest {

    private final HoaDonKhoGia kho = new HoaDonKhoGia();
    private final HoaDon hd = kho.hd;
    private final List<HoaDon> quet = new ArrayList<>();

    private CuaHangService service() {
        hd.setIdPhuongThucThanhToan(HoaDonSuaService.CHUYEN_KHOAN_ONLINE);
        LichSuHoaDon dat = new LichSuHoaDon();
        dat.setHoaDon(hd);
        dat.setHanhDong("Đặt hàng");
        dat.setMoTa("Khách đặt online. " + MailService.NHAN_EMAIL_DON + "khach@gmail.com");
        dat.setThoiGian(hd.getNgayTao());
        kho.lichSu.add(dat);
        quet.add(hd);
        CuaHangService s = new CuaHangService(
                KhoGia.khongDung(SanPhamRepository.class), KhoGia.khongDung(SanPhamChiTietRepository.class),
                KhoGia.khongDung(HinhAnhSanPhamRepository.class), KhoGia.khongDung(DanhMucRepository.class),
                KhoGia.khongDung(ThuongHieuRepository.class), KhoGia.khongDung(XuatXuRepository.class),
                KhoGia.khongDung(ChatLieuRepository.class), KhoGia.khongDung(DoCungRepository.class),
                KhoGia.khongDung(DiemCanBangRepository.class), KhoGia.khongDung(MauSacRepository.class),
                KhoGia.khongDung(TrongLuongRepository.class), KhoGia.khongDung(ChuViRepository.class),
                KhoGia.tao(HoaDonRepository.class, (t, a) -> switch (t) {
                    case "findByTrangThaiAndNgayTaoBetween" -> quet.stream()
                            .filter(h -> a[0].equals(h.getTrangThai()) && !h.getNgayTao().isAfter((LocalDateTime) a[2])).toList();
                    case "findFirstByMaHoaDonIgnoreCase" -> String.valueOf(a[0]).equalsIgnoreCase(hd.getMaHoaDon()) ? Optional.of(hd) : Optional.empty();
                    case "findById" -> Integer.valueOf(1).equals(a[0]) ? Optional.of(hd) : Optional.empty();
                    default -> KhoGia.KHONG_CO;
                }),
                KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> "findByHoaDon_IdOrderByIdAsc".equals(t) ? List.of() : KhoGia.KHONG_CO),
                KhoGia.tao(LichSuHoaDonRepository.class, (t, a) -> switch (t) {
                    case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> new ArrayList<>(kho.lichSu);
                    case "save" -> {
                        kho.lichSu.add((LichSuHoaDon) a[0]);
                        yield a[0];
                    }
                    default -> KhoGia.KHONG_CO;
                }),
                KhoGia.tao(LichSuThanhToanRepository.class, (t, a) -> "findByHoaDon_IdOrderByThoiGianAscIdAsc".equals(t)
                        ? new ArrayList<>(kho.thanhToan) : KhoGia.KHONG_CO),
                KhoGia.khongDung(PhieuGiamGiaRepository.class), KhoGia.khongDung(PhieuGiamGiaKhachHangRepository.class),
                KhoGia.khongDung(KhachHangRepository.class), null);
        s.setHoaDonService(kho.service);
        return s;
    }

    private LichSuHoaDon cuoi() {
        return kho.lichSu.get(kho.lichSu.size() - 1);
    }

    @Test
    void quaNamPhut_chuaTra_tuHuy_ghiLyDo_nguoiThaoTacHeThong() {
        hd.setNgayTao(LocalDateTime.now().minusMinutes(5).minusSeconds(1));
        CuaHangService s = service();
        s.tuHuyDonQuaHanThanhToan();
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertEquals("Tự động hủy đơn do quá thời gian thanh toán online (5 phút)", cuoi().getMoTa());
        assertEquals("Hệ thống", cuoi().getNguoiThaoTac());
        // Không có dòng gửi link mới / mã QR mới nào
        assertFalse(kho.lichSu.stream().anyMatch(l -> l.getHanhDong() != null && l.getHanhDong().contains("link")));
    }

    @Test
    void chuaToiNamPhut_daTra_hoacCod_khongHuy() {
        hd.setNgayTao(LocalDateTime.now().minusMinutes(4));
        CuaHangService s = service();
        s.tuHuyDonQuaHanThanhToan();
        s.tuHuyNeuQuaHan("HD01");
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai(), "còn trong 5 phút");

        hd.setNgayTao(LocalDateTime.now().minusMinutes(10));
        LichSuThanhToan tien = new LichSuThanhToan();
        tien.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        tien.setSoTien(BigDecimal.valueOf(500_000));
        kho.thanhToan.add(tien);
        s.tuHuyDonQuaHanThanhToan();
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai(), "đã nhận tiền");

        kho.thanhToan.clear();
        hd.setIdPhuongThucThanhToan(CuaHangService.COD);
        s.tuHuyDonQuaHanThanhToan();
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai(), "đơn COD");
    }

    @Test
    void kiemTraLuoi_khiTraCuu_quaHanThiHuyNgay() {
        hd.setNgayTao(LocalDateTime.now().minusMinutes(6));
        CuaHangService s = service();
        quet.clear(); // lượt quét chưa chạy tới
        s.tuHuyNeuQuaHan("hd01");
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
    }

    @Test
    void hanThanhToan_coDinhLucDatCongNamPhut() {
        hd.setNgayTao(LocalDateTime.of(2026, 11, 24, 19, 58, 4));
        hd.setIdPhuongThucThanhToan(HoaDonSuaService.CHUYEN_KHOAN_ONLINE);
        assertEquals(LocalDateTime.of(2026, 11, 24, 20, 3, 4), CuaHangService.hanThanhToan(hd));
        assertFalse(CuaHangService.laDonTreo(hd, 0, LocalDateTime.of(2026, 11, 24, 20, 3, 3)));
        assertTrue(CuaHangService.laDonTreo(hd, 0, LocalDateTime.of(2026, 11, 24, 20, 3, 4)));
        assertFalse(CuaHangService.laDonTreo(hd, 100_000, LocalDateTime.of(2026, 11, 24, 20, 30)), "đã nhận tiền");
    }

    @Test
    void mailNhacThanhToan_khongGiaHan_toiHanVanTuHuy() {
        hd.setNgayTao(LocalDateTime.now().minusMinutes(3));
        CuaHangService s = service();
        LocalDateTime han = CuaHangService.hanThanhToan(hd);
        Map<String, Object> kq = s.guiMailNhacThanhToan(1, "Nguyễn Văn A");
        assertEquals(han, kq.get("hanChuyenKhoan"), "hạn giữ nguyên lúc đặt + 5 phút");
        assertEquals(CuaHangService.GUI_MAIL_NHAC_THANH_TOAN, cuoi().getHanhDong());
        assertEquals("Nguyễn Văn A", cuoi().getNguoiThaoTac());
        assertTrue(cuoi().getMoTa().startsWith("Gửi mail nhắc thanh toán tới kh***@gmail.com"), cuoi().getMoTa());

        // Sang phút thứ 6: thư nhắc không gia hạn, đơn vẫn tự hủy; gửi nhắc nữa thì báo lỗi
        hd.setNgayTao(LocalDateTime.now().minusMinutes(6));
        assertThrows(ApiException.class, () -> s.guiMailNhacThanhToan(1, "Nguyễn Văn A"));
        s.tuHuyDonQuaHanThanhToan();
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertThrows(ApiException.class, () -> s.guiMailNhacThanhToan(1, "Nguyễn Văn A"));
    }

    @Test
    void anEmail_cheTruocDauA() {
        assertEquals("tu***@gmail.com", CuaHangService.anEmail("tuan.nguyen@gmail.com"));
        assertEquals("a***@x.vn", CuaHangService.anEmail("a@x.vn"));
    }
}
