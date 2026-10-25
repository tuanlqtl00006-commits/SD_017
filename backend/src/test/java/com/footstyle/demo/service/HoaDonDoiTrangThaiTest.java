package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Đổi trạng thái hóa đơn: các bước hợp lệ, hủy đơn (hoàn tồn kho), hoàn thành (ghi phần tiền còn lại). */
class HoaDonDoiTrangThaiTest {

    private final HoaDon hd = new HoaDon();
    private final List<HoaDonChiTiet> dong = new ArrayList<>();
    private final List<LichSuThanhToan> thanhToan = new ArrayList<>();
    private final List<LichSuHoaDon> lichSu = new ArrayList<>();
    private final List<SanPhamChiTiet> daLuuBienThe = new ArrayList<>();
    private final HoaDonService service;

    HoaDonDoiTrangThaiTest() {
        hd.setId(1);
        hd.setMaHoaDon("HD01");
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        hd.setThanhTien(new BigDecimal("500000"));

        HoaDonRepository hdRepo = KhoGia.tao(HoaDonRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> Integer.valueOf(1).equals(a[0]) ? Optional.of(hd) : Optional.empty();
            case "save" -> a[0];
            default -> KhoGia.KHONG_CO;
        });
        HoaDonChiTietRepository ctRepo = KhoGia.tao(HoaDonChiTietRepository.class, (ten, a) ->
                "findByHoaDon_IdOrderByIdAsc".equals(ten) ? dong : KhoGia.KHONG_CO);
        LichSuHoaDonRepository lsRepo = KhoGia.tao(LichSuHoaDonRepository.class, (ten, a) -> switch (ten) {
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> lichSu;
            case "save" -> {
                lichSu.add((LichSuHoaDon) a[0]);
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        LichSuThanhToanRepository ttRepo = KhoGia.tao(LichSuThanhToanRepository.class, (ten, a) -> switch (ten) {
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> new ArrayList<>(thanhToan);
            case "save" -> {
                thanhToan.add((LichSuThanhToan) a[0]);
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        HinhAnhSanPhamRepository anhRepo = KhoGia.tao(HinhAnhSanPhamRepository.class, (ten, a) ->
                "findByTrangThaiOrderByLaAnhChinhDescIdAsc".equals(ten) ? List.of() : KhoGia.KHONG_CO);
        SanPhamChiTietRepository spctRepo = KhoGia.tao(SanPhamChiTietRepository.class, (ten, a) -> {
            if ("save".equals(ten)) {
                daLuuBienThe.add((SanPhamChiTiet) a[0]);
                return a[0];
            }
            if ("truTonKho".equals(ten)) { // giống câu UPDATE có điều kiện: chỉ trừ khi còn đủ
                SanPhamChiTiet b = dong.stream().map(HoaDonChiTiet::getSanPhamChiTiet).filter(x -> x != null && x.getId().equals(a[0])).findFirst().orElseThrow();
                int sl = (Integer) a[1];
                if (b.getSoLuongTon() < sl) return 0;
                b.setSoLuongTon(b.getSoLuongTon() - sl);
                return 1;
            }
            return KhoGia.KHONG_CO;
        });
        service = new HoaDonService(hdRepo, ctRepo, lsRepo, ttRepo, anhRepo, spctRepo);
    }

    private SanPhamChiTiet bienThe(int id, int ton) {
        SanPhamChiTiet b = new SanPhamChiTiet();
        b.setId(id);
        b.setMaSpct("SP01-DEN-" + id);
        b.setSoLuongTon(ton);
        return b;
    }

    private void themDong(SanPhamChiTiet b, int soLuong) {
        HoaDonChiTiet ct = new HoaDonChiTiet();
        ct.setId(dong.size() + 1);
        ct.setHoaDon(hd);
        ct.setSanPhamChiTiet(b);
        ct.setSoLuong(soLuong);
        ct.setDonGia(new BigDecimal("100000"));
        dong.add(ct);
    }

    private static HoaDonTrangThaiRequest sang(int tt, String ghiChu) {
        return new HoaDonTrangThaiRequest(tt, ghiChu, null);
    }

    @Test
    void donOnline_xacNhanMoiTruTonKho_gopCacDongCungBienThe() {
        SanPhamChiTiet a = bienThe(10, 6);
        SanPhamChiTiet b = bienThe(11, 1);
        themDong(a, 2);
        themDong(b, 1);
        themDong(a, 3);
        service.doiTrangThai(1, sang(HoaDon.DA_XAC_NHAN, null));
        assertEquals(HoaDon.DA_XAC_NHAN, hd.getTrangThai());
        assertEquals(1, a.getSoLuongTon(), "6 - 2 - 3");
        assertEquals(0, b.getSoLuongTon());
    }

    @Test
    void donOnline_xacNhanKhiKhongDuHang_baoHetCacDongThieu_khongDoiTrangThai_khongTruKho() {
        SanPhamChiTiet a = bienThe(10, 4);
        SanPhamChiTiet b = bienThe(11, 0);
        themDong(a, 2);
        themDong(b, 1);
        themDong(a, 3);
        ApiException e = loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_XAC_NHAN, null)));
        assertTrue(e.getMessage().contains("SP01-DEN-10 (còn 4, đơn cần 5)"), e.getMessage());
        assertTrue(e.getMessage().contains("SP01-DEN-11 (còn 0, đơn cần 1)"));
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai());
        assertEquals(4, a.getSoLuongTon(), "chưa trừ gì");
    }

    @Test
    void donOnline_huyKhiConChoXacNhan_khongCongLaiKho() {
        SanPhamChiTiet a = bienThe(10, 5);
        themDong(a, 2);
        service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Khách đổi ý"));
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertEquals(5, a.getSoLuongTon(), "đơn chưa xác nhận thì chưa trừ kho nên hủy không cộng lại");
        assertTrue(daLuuBienThe.isEmpty());
    }

    @Test
    void huyDonKhachDaChuyenKhoanTruoc_ghiKhoanHoanPhi() {
        LichSuThanhToan ck = new LichSuThanhToan();
        ck.setSoTien(new BigDecimal("500000"));
        ck.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        thanhToan.add(ck);
        service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Khách đổi ý"));
        assertEquals(2, thanhToan.size());
        assertEquals(0, new BigDecimal("-500000").compareTo(thanhToan.get(1).getSoTien()), "hoàn lại toàn bộ tiền khách đã trả");
        assertTrue(lichSu.get(lichSu.size() - 1).getMoTa().contains("Hoàn phí cho khách 500.000 ₫"));
    }

    @Test
    void huyDon_hoanLaiTonKho_gopCacDongCungBienThe() {
        hd.setTrangThai(HoaDon.GIAO_THAT_BAI); // đã xác nhận (đã trừ kho), giao thất bại, hàng về kho
        SanPhamChiTiet a = bienThe(10, 5);
        SanPhamChiTiet b = bienThe(11, 0);
        themDong(a, 2);
        themDong(b, 1);
        themDong(a, 3); // cùng biến thể a nằm ở 2 dòng
        service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Khách đổi ý"));
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertEquals(10, a.getSoLuongTon(), "5 + 2 + 3");
        assertEquals(1, b.getSoLuongTon());
        assertEquals(2, daLuuBienThe.size(), "mỗi biến thể chỉ lưu một lần");
        assertEquals("Khách đổi ý", lichSu.get(lichSu.size() - 1).getMoTa());
    }

    @Test
    void huyDon_dongThieuBienTheHoacSoLuongAm_boQuaKhongLoi() {
        hd.setTrangThai(HoaDon.GIAO_THAT_BAI);
        themDong(null, 2);
        SanPhamChiTiet a = bienThe(10, 0);
        a.setSoLuongTon(null); // tồn kho null coi như 0
        themDong(a, 0);
        themDong(a, 3);
        service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Hết hàng"));
        assertEquals(3, a.getSoLuongTon());
    }

    @Test
    void huyDon_batBuocLyDo_vaKhongHuyKhiDaGiaoChoVanChuyen() {
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_HUY, "   "))).getMessage().contains("lý do"));
        hd.setTrangThai(HoaDon.DANG_GIAO);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Khách hủy"))).getMessage().contains("không thể hủy"));
    }

    @Test
    void khongNhayCoc_vaKhongDoiSauKhiDaHuyHoacHoanThanh() {
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.CHO_LAY_HANG, null))).getMessage().contains("Không thể chuyển"));
        hd.setTrangThai(HoaDon.DA_HUY);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.CHO_XAC_NHAN, null))).getMessage().contains("đã bị hủy"));
        hd.setTrangThai(HoaDon.HOAN_THANH);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_HUY, "x"))).getMessage().contains("đã hoàn thành"));
    }

    @Test
    void trangThaiLaHoacThieu_baoLoi400() {
        assertTrue(loi(() -> service.doiTrangThai(1, sang(9, null))).getMessage().contains("không hợp lệ"));
        assertTrue(loi(() -> service.doiTrangThai(1, null)).getMessage().contains("không hợp lệ"));
        assertTrue(loi(() -> service.doiTrangThai(99, sang(2, null))).getMessage().contains("Không tìm thấy"));
    }

    @Test
    void donTaiQuay_diThangSangHoanThanh_vaGhiPhanTienConLai() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        LichSuThanhToan coc = new LichSuThanhToan();
        coc.setSoTien(new BigDecimal("200000"));
        coc.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        thanhToan.add(coc);
        LichSuThanhToan loi = new LichSuThanhToan(); // giao dịch lỗi không được tính là đã trả
        loi.setSoTien(new BigDecimal("999999"));
        loi.setTrangThai(0);
        thanhToan.add(loi);

        service.doiTrangThai(1, sang(HoaDon.HOAN_THANH, null));
        assertEquals(HoaDon.HOAN_THANH, hd.getTrangThai());
        assertEquals(3, thanhToan.size());
        assertEquals(0, new BigDecimal("300000").compareTo(thanhToan.get(2).getSoTien()), "500.000 - 200.000 đã trả");
    }

    @Test
    void daTraDu_hoanThanhKhongGhiThemThanhToan() {
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        LichSuThanhToan du = new LichSuThanhToan();
        du.setSoTien(new BigDecimal("500000"));
        du.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        thanhToan.add(du);
        service.doiTrangThai(1, sang(HoaDon.HOAN_THANH, null));
        assertEquals(1, thanhToan.size());
    }

    @Test
    void ghiChuQua500KyTu_baoLoi() {
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_XAC_NHAN, "x".repeat(501)))).getMessage().contains("500"));
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai(), "lỗi thì chưa đổi trạng thái");
    }

    @Test
    void tuNgaySauDenNgay_baoLoi400() {
        LocalDate homNay = LocalDate.of(2026, 10, 13);
        assertTrue(loi(() -> service.demTheoTrangThai(null, homNay, homNay.minusDays(1), null)).getMessage().contains("Từ ngày"));
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }
}
