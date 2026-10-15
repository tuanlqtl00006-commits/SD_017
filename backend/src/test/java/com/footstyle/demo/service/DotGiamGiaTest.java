package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import com.footstyle.demo.service.DotCapNhatKeHoach.KeHoach;
import com.footstyle.demo.service.DotCapNhatKeHoach.TrangThaiHienThi;
import com.footstyle.demo.service.DotCapNhatKeHoach.TruongHop;
import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import com.footstyle.demo.service.GiamGiaChongDot.Doan;
import com.footstyle.demo.service.GiamGiaChongDot.DotApDung;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử các bài toán nghiệp vụ của Đợt giảm giá theo yêu cầu của giảng viên:
 *  1. Nhiều đợt giảm giá chồng ngày trên cùng biến thể (SPCT1..SPCT5): chia khoảng ngày, tính theo MAX (15%) hoặc TRUNG_BINH (12,5%).
 *  2. Switch-case khi sửa đợt giảm giá (TH1 / TH2 / TH3 tương tự PGG nhưng không gửi mail).
 *  3. Switch-case trạng thái hiển thị của đợt theo thời gian thực và công tắc bật/tắt.
 */
class DotGiamGiaTest {

    private static final LocalDate N_01_10 = LocalDate.of(2026, 10, 1);
    private static final LocalDate N_03_10 = LocalDate.of(2026, 10, 3);
    private static final LocalDate N_04_10 = LocalDate.of(2026, 10, 4);
    private static final LocalDate N_10_10 = LocalDate.of(2026, 10, 10);
    private static final LocalDate N_11_10 = LocalDate.of(2026, 10, 11);
    private static final LocalDate N_20_10 = LocalDate.of(2026, 10, 20);

    // Đợt 1: 10%, 1/10 -> 10/10 (áp dụng cho SPCT1, SPCT5)
    private static final DotApDung DOT_1 = new DotApDung(1L, N_01_10, N_10_10, 10);
    // Đợt 2: 15%, 4/10 -> 20/10 (áp dụng cho SPCT1, SPCT3, SPCT4)
    private static final DotApDung DOT_2 = new DotApDung(2L, N_04_10, N_20_10, 15);

    @Test
    @DisplayName("Kịch bản của cô - SPCT1 trùng 2 đợt: chia 3 khoảng ngày (1/10-3/10: 10%, 4/10-10/10: MAX=15%, 11/10-20/10: 15%)")
    void spct1_trungHaiDot_cheDoMax() {
        List<Doan> lich = GiamGiaChongDot.lich(List.of(DOT_1, DOT_2), CheDo.MAX);
        assertEquals(3, lich.size());

        // 1/10 -> 3/10: chỉ Đợt 1 -> 10%
        assertEquals(new Doan(N_01_10, N_03_10, 10.0, List.of(1L)), lich.get(0));
        assertFalse(lich.get(0).trungDot());

        // 4/10 -> 10/10: trùng Đợt 1 (10%) và Đợt 2 (15%) -> lấy MAX = 15%
        assertEquals(new Doan(N_04_10, N_10_10, 15.0, List.of(1L, 2L)), lich.get(1));
        assertTrue(lich.get(1).trungDot());

        // 11/10 -> 20/10: chỉ Đợt 2 -> 15%
        assertEquals(new Doan(N_11_10, N_20_10, 15.0, List.of(2L)), lich.get(2));
        assertFalse(lich.get(2).trungDot());
    }

    @Test
    @DisplayName("Kịch bản của cô - SPCT1 trùng 2 đợt: chế độ TRUNG_BINH ra 12,5% ở đoạn 4/10-10/10")
    void spct1_trungHaiDot_cheDoTrungBinh() {
        List<Doan> lich = GiamGiaChongDot.lich(List.of(DOT_1, DOT_2), CheDo.TRUNG_BINH);
        assertEquals(3, lich.size());
        assertEquals(10.0, lich.get(0).phanTram());
        assertEquals(12.5, lich.get(1).phanTram());
        assertEquals(15.0, lich.get(2).phanTram());
    }

    @Test
    @DisplayName("Kịch bản của cô - SPCT5 (chỉ Đợt 1: 10%), SPCT3/SPCT4 (chỉ Đợt 2: 15%), SPCT2 (không giảm: 0%)")
    void cacBienTheKhacTrongKichBan() {
        // SPCT5 chỉ thuộc Đợt 1: 1/10 -> 10/10 giảm 10%, từ 11/10 về 0%
        List<Doan> lichSpct5 = GiamGiaChongDot.lich(List.of(DOT_1), CheDo.MAX);
        assertEquals(1, lichSpct5.size());
        assertEquals(new Doan(N_01_10, N_10_10, 10.0, List.of(1L)), lichSpct5.get(0));
        assertEquals(10.0, GiamGiaChongDot.phanTramTaiNgay(List.of(DOT_1), LocalDate.of(2026, 10, 6), CheDo.MAX));
        assertEquals(0.0, GiamGiaChongDot.phanTramTaiNgay(List.of(DOT_1), LocalDate.of(2026, 10, 12), CheDo.MAX));

        // SPCT3, SPCT4 chỉ thuộc Đợt 2: 4/10 -> 20/10 giảm 15%, ngày 2/10 chưa giảm (0%)
        List<Doan> lichSpct3 = GiamGiaChongDot.lich(List.of(DOT_2), CheDo.MAX);
        assertEquals(1, lichSpct3.size());
        assertEquals(new Doan(N_04_10, N_20_10, 15.0, List.of(2L)), lichSpct3.get(0));
        assertEquals(0.0, GiamGiaChongDot.phanTramTaiNgay(List.of(DOT_2), LocalDate.of(2026, 10, 2), CheDo.MAX));
        assertEquals(15.0, GiamGiaChongDot.phanTramTaiNgay(List.of(DOT_2), LocalDate.of(2026, 10, 6), CheDo.MAX));

        // SPCT2 không nằm trong đợt nào -> 0%
        assertTrue(GiamGiaChongDot.lich(List.of(), CheDo.MAX).isEmpty());
        assertEquals(0.0, GiamGiaChongDot.phanTramTaiNgay(List.of(), LocalDate.of(2026, 10, 6), CheDo.MAX));
    }

    @Test
    @DisplayName("Switch-case sửa đợt - TH1: đổi thông tin đợt, giữ nguyên danh sách biến thể -> cập nhật tất cả")
    void suaDot_th1_doiThongTin_giuNguyenBienThe() {
        KeHoach kh = DotCapNhatKeHoach.lap(List.of(1L, 5L), List.of(1L, 5L), true);
        assertEquals(TruongHop.TH1_DOI_THONG_TIN, kh.truongHop());
        assertTrue(kh.moi().isEmpty());
        assertTrue(kh.goBo().isEmpty());
        assertEquals(List.of(1L, 5L), kh.capNhat());
        assertTrue(kh.giuNguyen().isEmpty());
    }

    @Test
    @DisplayName("Switch-case sửa đợt - TH2: giữ nguyên thông tin, đổi danh sách biến thể -> thêm mới, gỡ bỏ, giữ nguyên")
    void suaDot_th2_giuThongTin_doiBienThe() {
        KeHoach kh = DotCapNhatKeHoach.lap(List.of(1L, 5L), List.of(1L, 3L, 4L), false);
        assertEquals(TruongHop.TH2_DOI_BIEN_THE, kh.truongHop());
        assertEquals(List.of(3L, 4L), kh.moi());
        assertEquals(List.of(5L), kh.goBo());
        assertTrue(kh.capNhat().isEmpty());
        assertEquals(List.of(1L), kh.giuNguyen());
    }

    @Test
    @DisplayName("Switch-case sửa đợt - TH3: đổi cả thông tin đợt lẫn danh sách biến thể -> thêm mới, gỡ bỏ, cập nhật phần giữ lại")
    void suaDot_th3_doiCaHai() {
        KeHoach kh = DotCapNhatKeHoach.lap(List.of(1L, 5L), List.of(1L, 3L, 4L), true);
        assertEquals(TruongHop.TH3_DOI_CA_HAI, kh.truongHop());
        assertEquals(List.of(3L, 4L), kh.moi());
        assertEquals(List.of(5L), kh.goBo());
        assertEquals(List.of(1L), kh.capNhat());
        assertTrue(kh.giuNguyen().isEmpty());
    }

    @Test
    @DisplayName("Switch-case sửa đợt - Không đổi gì -> giữ nguyên toàn bộ")
    void suaDot_khongDoi() {
        KeHoach kh = DotCapNhatKeHoach.lap(List.of(1L, 5L), List.of(1L, 5L), false);
        assertEquals(TruongHop.KHONG_DOI, kh.truongHop());
        assertTrue(kh.moi().isEmpty());
        assertTrue(kh.goBo().isEmpty());
        assertTrue(kh.capNhat().isEmpty());
        assertEquals(List.of(1L, 5L), kh.giuNguyen());
    }

    @Test
    @DisplayName("Trạng thái đợt giảm giá theo ngày và công tắc bật/tắt")
    void trangThaiDot_theoNgayVaCongTac() {
        assertEquals(TrangThaiHienThi.SAP_DIEN_RA,
                DotCapNhatKeHoach.tinhTrangThai(1, N_04_10, N_10_10, LocalDate.of(2026, 10, 3)));
        assertEquals(TrangThaiHienThi.DANG_DIEN_RA,
                DotCapNhatKeHoach.tinhTrangThai(1, N_04_10, N_10_10, N_04_10));
        assertEquals(TrangThaiHienThi.DANG_DIEN_RA,
                DotCapNhatKeHoach.tinhTrangThai(1, N_04_10, N_10_10, N_10_10));
        assertEquals(TrangThaiHienThi.DA_KET_THUC,
                DotCapNhatKeHoach.tinhTrangThai(1, N_04_10, N_10_10, N_11_10));
        assertEquals(TrangThaiHienThi.NGUNG_HOAT_DONG,
                DotCapNhatKeHoach.tinhTrangThai(0, N_04_10, N_10_10, LocalDate.of(2026, 10, 6)));
    }
}
