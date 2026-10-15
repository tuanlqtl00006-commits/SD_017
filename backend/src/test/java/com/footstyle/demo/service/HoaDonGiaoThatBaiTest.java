package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * "Trường hợp giao hàng thất bại chưa thể hiện" (cô, 07:24): đơn Đang giao hàng báo được Giao hàng thất bại (bắt buộc lý do),
 * từ đó giao lại hoặc hủy (hàng về kho nên cộng lại tồn); cùng với việc trạng thái cuối bị khóa hẳn (cô, 10:36).
 */
class HoaDonGiaoThatBaiTest {

    private final HoaDonKhoGia kho = new HoaDonKhoGia();
    private final HoaDon hd = kho.hd;
    private final HoaDonService service = kho.service;

    private static HoaDonTrangThaiRequest sang(int tt, String ghiChu) {
        return new HoaDonTrangThaiRequest(tt, ghiChu, null);
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    private String hanhDongCuoi() {
        return kho.lichSu.get(kho.lichSu.size() - 1).getHanhDong();
    }

    private String moTaCuoi() {
        return kho.lichSu.get(kho.lichSu.size() - 1).getMoTa();
    }

    @Test
    void baoGiaoThatBai_tuDangGiao_batBuocLyDo_vaGhiVaoLichSu() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "  "))).getMessage().contains("lý do giao hàng thất bại"));
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, null))).getMessage().contains("lý do"));
        assertEquals(HoaDon.DANG_GIAO, hd.getTrangThai(), "thiếu lý do thì chưa đổi");
        assertTrue(kho.lichSu.isEmpty());

        Map<String, Object> ketQua = service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "Khách không nghe máy 3 lần"));
        assertEquals(HoaDon.GIAO_THAT_BAI, hd.getTrangThai());
        assertEquals("Giao hàng thất bại", hanhDongCuoi());
        assertEquals("Khách không nghe máy 3 lần", moTaCuoi());
        assertNull(hd.getNgayGiaoThucTe(), "giao thất bại thì chưa có ngày giao thực tế");
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) ketQua.get("hoaDon");
        assertEquals("Giao hàng thất bại", info.get("tenTrangThai"));
        assertEquals(HoaDon.GIAO_THAT_BAI, info.get("trangThai"));
    }

    @Test
    void baoGiaoThatBai_khongTraLaiTonKho() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        SanPhamChiTiet a = kho.bienThe(10, 5);
        kho.themDong(a, 2);
        service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "Sai địa chỉ"));
        assertEquals(5, a.getSoLuongTon(), "hàng còn ở chỗ vận chuyển, chưa về kho");
        assertTrue(kho.daLuuBienThe.isEmpty());
    }

    @Test
    void giaoThatBaiChiBaoDuocKhiDangGiaoHang_vaChiVoiDonGiaoHang() {
        for (int dangO : new int[] {HoaDon.CHO_XAC_NHAN, HoaDon.DA_XAC_NHAN, HoaDon.CHO_LAY_HANG, HoaDon.DA_GIAO}) {
            hd.setTrangThai(dangO);
            ApiException e = loi(() -> service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "lý do")));
            assertTrue(e.getMessage().contains("Đang giao hàng"), "từ " + dangO + ": " + e.getMessage());
            assertEquals(dangO, hd.getTrangThai());
        }
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        loi(() -> service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "lý do")));
        assertEquals(HoaDon.CHO_XAC_NHAN, hd.getTrangThai());
    }

    @Test
    void giaoLai_veDangGiaoHang_khongCanLyDo_vaKhongDongVaoTonKho() {
        hd.setTrangThai(HoaDon.GIAO_THAT_BAI);
        SanPhamChiTiet a = kho.bienThe(10, 5);
        kho.themDong(a, 2);
        service.doiTrangThai(1, sang(HoaDon.DANG_GIAO, null));
        assertEquals(HoaDon.DANG_GIAO, hd.getTrangThai());
        assertEquals("Đang giao hàng", hanhDongCuoi());
        assertEquals("Giao lại sau khi giao hàng thất bại", moTaCuoi());
        assertEquals(5, a.getSoLuongTon());
    }

    @Test
    void giaoLai_coGhiChuThiGhiDungGhiChu() {
        hd.setTrangThai(HoaDon.GIAO_THAT_BAI);
        service.doiTrangThai(1, sang(HoaDon.DANG_GIAO, "Khách hẹn nhận chiều mai"));
        assertEquals("Khách hẹn nhận chiều mai", moTaCuoi());
    }

    @Test
    void huyDonSauGiaoThatBai_batBuocLyDo_vaTraHangVeKho() {
        hd.setTrangThai(HoaDon.GIAO_THAT_BAI);
        SanPhamChiTiet a = kho.bienThe(10, 5);
        SanPhamChiTiet b = kho.bienThe(11, 0);
        kho.themDong(a, 2);
        kho.themDong(b, 1);
        kho.themDong(a, 1);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_HUY, " "))).getMessage().contains("lý do hủy"));
        assertEquals(HoaDon.GIAO_THAT_BAI, hd.getTrangThai());
        assertEquals(5, a.getSoLuongTon(), "chưa hủy thì chưa cộng tồn");

        service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Khách từ chối nhận hàng"));
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertEquals(8, a.getSoLuongTon(), "5 + 2 + 1 trả về kho");
        assertEquals(1, b.getSoLuongTon());
        assertEquals(2, kho.daLuuBienThe.size());
        assertEquals("Đã hủy", hanhDongCuoi());
        assertEquals("Khách từ chối nhận hàng", moTaCuoi());
    }

    @Test
    void tuGiaoThatBai_khongNhayThangSangDaGiaoHoacHoanThanh() {
        for (int dich : new int[] {HoaDon.DA_GIAO, HoaDon.HOAN_THANH, HoaDon.CHO_LAY_HANG, HoaDon.CHO_XAC_NHAN}) {
            hd.setTrangThai(HoaDon.GIAO_THAT_BAI);
            ApiException e = loi(() -> service.doiTrangThai(1, sang(dich, "x")));
            assertTrue(e.getMessage().contains("Không thể chuyển"), dich + ": " + e.getMessage());
            assertEquals(HoaDon.GIAO_THAT_BAI, hd.getTrangThai());
        }
    }

    @Test
    void baoGiaoThatBaiLanThuHai_haiNguoiCungBam_nguoiSauNhanThongBaoDaORoi() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "Khách vắng nhà"));
        ApiException e = loi(() -> service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "Khách vắng nhà")));
        assertTrue(e.getMessage().contains("đã ở trạng thái \"Giao hàng thất bại\""), e.getMessage());
        assertEquals(1, kho.lichSu.size(), "lần thứ hai không ghi thêm lịch sử");
    }

    @Test
    void thatBaiRoiGiaoLaiRoiGiaoThanhCong_diHetDuongDenHoanThanh_vaThuTienCod() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        hd.setThanhTien(new BigDecimal("530000"));
        service.doiTrangThai(1, sang(HoaDon.GIAO_THAT_BAI, "Khách vắng nhà"));
        service.doiTrangThai(1, sang(HoaDon.DANG_GIAO, null));
        service.doiTrangThai(1, sang(HoaDon.DA_GIAO, null));
        assertNotNull(hd.getNgayGiaoThucTe());
        service.doiTrangThai(1, sang(HoaDon.HOAN_THANH, null));
        assertEquals(HoaDon.HOAN_THANH, hd.getTrangThai());
        assertEquals(1, kho.thanhToan.size());
        assertEquals(0, new BigDecimal("530000").compareTo(kho.thanhToan.get(0).getSoTien()));
        assertEquals(LichSuThanhToan.DA_THANH_TOAN, kho.thanhToan.get(0).getTrangThai());
        assertEquals(List.of("Giao hàng thất bại", "Đang giao hàng", "Đã giao hàng", "Đã hoàn thành"),
                kho.lichSu.stream().map(l -> l.getHanhDong()).toList());
    }

    @Test
    void dangGiaoHang_vanKhongHuyThang_vaGiaoThatBaiLaLoiDiDuyNhat() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.DA_HUY, "Khách hủy"))).getMessage().contains("không thể hủy"));
        assertEquals(HoaDon.DANG_GIAO, hd.getTrangThai());
    }

    @Test
    void trangThaiCuoi_khoaHan_moiDichDenDeuBiTuChoi() {
        for (int cuoi : new int[] {HoaDon.DA_HUY, HoaDon.HOAN_THANH, HoaDon.HOAN_TIEN}) {
            for (int dich = 0; dich <= HoaDon.GIAO_THAT_BAI; dich++) {
                hd.setTrangThai(cuoi);
                int d = dich;
                ApiException e = loi(() -> service.doiTrangThai(1, sang(d, "lý do")));
                assertTrue(e.getMessage().contains("không thể đổi trạng thái nữa"), cuoi + " -> " + d + ": " + e.getMessage());
                assertEquals(cuoi, hd.getTrangThai());
            }
        }
        assertTrue(kho.lichSu.isEmpty(), "không ghi lịch sử nào cho các lần bị từ chối");
        assertTrue(kho.thanhToan.isEmpty());
        assertTrue(kho.daLuuBienThe.isEmpty());
    }

    @Test
    void hoanTienChuaCoLuong_khongChuyenVaoDuoc() {
        hd.setTrangThai(HoaDon.DANG_GIAO);
        assertTrue(loi(() -> service.doiTrangThai(1, sang(HoaDon.HOAN_TIEN, "x"))).getMessage().contains("Không thể chuyển"));
    }

    @Test
    void demTheoTrangThai_coDuCacKhoaTu0Den8_vaTatCa() {
        Map<String, Long> dem = service.demTheoTrangThai(null, null, null, null);
        assertEquals(List.of("tatCa", "0", "1", "2", "3", "4", "5", "6", "7", "8"), List.copyOf(dem.keySet()));
    }
}
