package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TroLyAoServiceTest {

    private static final LocalDateTime LUC = CaLamMau.HOM_NAY.atTime(10, 0);
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final List<PhieuGiamGia> phieus = new ArrayList<>();
    private final List<DotGiamGia> dots = new ArrayList<>();
    private final List<HoaDon> hoaDons = new ArrayList<>();
    private final Map<Long, Double> phanTramDot = new HashMap<>();
    private final TroLyAoService troLy;
    private final TroLyAoService.NguCanh lan = new TroLyAoService.NguCanh("Lan", "0901000001");

    TroLyAoServiceTest() {
        SanPham astrox = sanPham(1, "Vợt cầu lông Yonex Astrox 99 Pro", "Yonex", "Vợt cầu lông", "Nặng đầu", "Cứng");
        SanPham nano = sanPham(2, "Vợt cầu lông Yonex Nanoflare 700", "Yonex", "Vợt cầu lông", "Nhẹ đầu", "Trung bình");
        SanPham lining = sanPham(3, "Vợt cầu lông Lining Axforce 20", "Lining", "Vợt cầu lông người mới", "Cân bằng", "Mềm");
        SanPham cau = sanPham(4, "Quả cầu lông Thành Công", "Thành Công", "Quả cầu lông", "Không áp dụng", "Không áp dụng");
        SanPham hetHang = sanPham(5, "Vợt cầu lông Victor Thruster K 9900", "Victor", "Vợt cầu lông", "Nặng đầu", "Cứng");
        bienThe(1, astrox, "Đen", "4U", 4_290_000L, 5);
        bienThe(2, astrox, "Đỏ", "3U", 4_390_000L, 2);
        bienThe(3, nano, "Xanh", "4U", 2_890_000L, 10);
        bienThe(4, lining, "Trắng", "5U", 890_000L, 8);
        bienThe(5, cau, "Trắng", "Không áp dụng", 250_000L, 100);
        bienThe(6, hetHang, "Xanh", "4U", 3_500_000L, 0);
        phanTramDot.put(3L, 10.0); // Nanoflare đang giảm 10% -> 2.601.000

        phieus.add(phieu("PGG01", PhieuGiamGia.DOI_TUONG_TAT_CA, PhieuGiamGia.LOAI_PHAN_TRAM, 10, 200_000L, 1_000_000L, LUC.plusDays(5)));
        phieus.add(phieu("PGG02", PhieuGiamGia.DOI_TUONG_KHACH_CU_THE, PhieuGiamGia.LOAI_SO_TIEN, 100_000, null, 0L, LUC.plusDays(5)));
        phieus.add(phieu("PGG03", PhieuGiamGia.DOI_TUONG_TAT_CA, PhieuGiamGia.LOAI_SO_TIEN, 50_000, null, 0L, LUC.minusDays(1)));
        DotGiamGia d = new DotGiamGia();
        d.setTenDot("Sale tháng 10");
        d.setPhanTramGiamDot(10);
        d.setTrangThai(1);
        d.setNgayBatDau(LUC.minusDays(3));
        d.setNgayKetThuc(LUC.plusDays(10));
        dots.add(d);

        KhachHang kh = new KhachHang();
        kh.setSdt("0901000001");
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon("HD12");
        hd.setKhachHang(kh);
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setTrangThai(HoaDon.DANG_GIAO);
        hd.setThanhTien(BigDecimal.valueOf(2_631_000));
        hd.setNgayTao(LUC.minusDays(2));
        hd.setNgayGiaoDuKien(LUC.plusDays(1));
        hd.setDonViVanChuyen("Giao Hàng Nhanh");
        hoaDons.add(hd);

        GiamGiaApDungService giam = new GiamGiaApDungService(null) {
            @Override
            public Map<Long, Double> phanTramTheoBienThe(LocalDateTime thoiDiem) {
                return phanTramDot;
            }
        };
        troLy = new TroLyAoService(
                KhoGia.tao(SanPhamChiTietRepository.class, (t, a) -> "findAll".equals(t) && a.length == 0 ? bienThes : KhoGia.KHONG_CO),
                giam,
                KhoGia.tao(PhieuGiamGiaRepository.class, (t, a) -> "findAll".equals(t) && a.length == 0 ? phieus : KhoGia.KHONG_CO),
                KhoGia.tao(DotGiamGiaRepository.class, (t, a) -> "findAll".equals(t) && a.length == 0 ? dots : KhoGia.KHONG_CO),
                KhoGia.tao(HoaDonRepository.class, (t, a) -> "findFirstByMaHoaDonIgnoreCase".equals(t)
                        ? hoaDons.stream().filter(h -> h.getMaHoaDon().equalsIgnoreCase((String) a[0])).findFirst() : KhoGia.KHONG_CO),
                "FootStyle", "Số 15 Trần Phú, Hà Đông", "0912 000 001", "7:00 - 22:00");
        troLy.datDongHo(Clock.fixed(LUC.atZone(CaLamMau.VN).toInstant(), CaLamMau.VN));
    }

    private static <T extends com.footstyle.demo.entity.ThuocTinh> T tt(T t, String ten) {
        t.setTen(ten);
        return t;
    }

    private SanPham sanPham(int id, String ten, String hang, String danhMuc, String canBang, String doCung) {
        SanPham sp = new SanPham();
        sp.setId(id);
        sp.setTenSanPham(ten);
        sp.setTrangThai(1);
        sp.setThuongHieu(tt(new ThuongHieu(), hang));
        sp.setDanhMuc(tt(new DanhMuc(), danhMuc));
        sp.setDiemCanBang(tt(new DiemCanBang(), canBang));
        sp.setDoCung(tt(new DoCung(), doCung));
        return sp;
    }

    private void bienThe(int id, SanPham sp, String mau, String tl, long gia, int ton) {
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(id);
        c.setSanPham(sp);
        c.setMaSpct("SPCT" + id);
        c.setMauSac(tt(new MauSac(), mau));
        c.setTrongLuong(tt(new TrongLuong(), tl));
        c.setGiaBan(gia);
        c.setSoLuongTon(ton);
        c.setTrangThai(1);
        bienThes.add(c);
    }

    private static PhieuGiamGia phieu(String ma, int doiTuong, int loai, long giaTri, Long toiDa, Long toiThieu, LocalDateTime ketThuc) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu(ma);
        p.setDoiTuongApDung(doiTuong);
        p.setLoaiGiam(loai);
        p.setGiaTriGiam(giaTri);
        p.setGiamToiDa(toiDa);
        p.setGiaTriDonHangToiThieu(toiThieu);
        p.setSoLuong(100);
        p.setSoLuongDaDung(0);
        p.setTrangThai(PhieuGiamGia.HOAT_DONG);
        p.setNgayBatDau(LUC.minusDays(10));
        p.setNgayKetThuc(ketThuc);
        return p;
    }

    private String hoi(String cau) {
        return troLy.traLoi(cau, lan).noiDung();
    }

    /* ---------- đọc giá, bộ lọc ---------- */

    @Test
    void khoangGia_hieuNhieuCachViet() {
        assertArrayEquals(new long[] {0, 3_000_000}, TroLyAoService.khoangGia(" vot duoi 3 trieu "));
        assertArrayEquals(new long[] {1_500_000, 3_000_000}, TroLyAoService.khoangGia(" tu 1tr5 den 3tr "));
        assertArrayEquals(new long[] {1_000_000, 2_000_000}, TroLyAoService.khoangGia(" vot 1-2 trieu "));
        assertArrayEquals(new long[] {1_000_000, 2_000_000}, TroLyAoService.khoangGia(" tu 1 den 2 trieu "));
        assertArrayEquals(new long[] {2_000_000, Long.MAX_VALUE}, TroLyAoService.khoangGia(" tren 2 trieu "));
        assertArrayEquals(new long[] {1_600_000, 2_400_000}, TroLyAoService.khoangGia(" khoang 2 trieu "));
        assertArrayEquals(new long[] {0, 500_000}, TroLyAoService.khoangGia(" duoi 500k "));
        assertArrayEquals(new long[] {0, 2_000_000}, TroLyAoService.khoangGia(" duoi 2 "));
        assertArrayEquals(new long[] {0, 2_500_000}, TroLyAoService.khoangGia(" duoi 2.500.000 "));
        assertArrayEquals(new long[] {0, 1_500_000}, TroLyAoService.khoangGia(" duoi 1,5 trieu "));
        assertArrayEquals(new long[] {2_400_000, 3_600_000}, TroLyAoService.khoangGia(" vot 3 trieu "));
        // 4U là trọng lượng, không phải giá; số không có đơn vị và không có từ chỉ giá cũng bỏ qua
        assertNull(TroLyAoService.khoangGia(" vot 4u "));
        assertNull(TroLyAoService.khoangGia(" mua 2 cay vot "));
        assertNull(TroLyAoService.khoangGia(" tren 4u "));
    }

    @Test
    void docBoLoc_hangLoaiTrongLuongLoiChoi() {
        TroLyAoService.BoLoc l = TroLyAoService.docBoLoc(" vot yonex tan cong 4u duoi 3 trieu ", Set.of("Yonex", "Lining"));
        assertEquals("Yonex", l.thuongHieu());
        assertEquals(NhomDanhMuc.VOT, l.nhomDanhMuc());
        assertEquals("4u", l.trongLuong());
        assertEquals("nang dau", l.diemCanBang());
        assertEquals(3_000_000L, l.giaDen());
        TroLyAoService.BoLoc moi = TroLyAoService.docBoLoc(" vot cho nguoi moi choi ", new LinkedHashSet<>());
        assertEquals(Set.of("mem", "trung binh"), moi.doCung());
        assertEquals("nhe dau", TroLyAoService.docBoLoc(" vot phong thu ", Set.of()).diemCanBang());
        assertEquals(NhomDanhMuc.QUA_CAU, TroLyAoService.docBoLoc(" ong cau nao tot ", Set.of()).nhomDanhMuc());
        assertFalse(TroLyAoService.docBoLoc(" xin chao ", Set.of()).coLoc());
    }

    @Test
    void hamPhu() {
        assertEquals("10%", TroLyAoService.phanTram(10.0));
        assertEquals("12,5%", TroLyAoService.phanTram(12.5));
        assertEquals("0912***678", TroLyAoService.an("0912345678"));
        assertEquals(3, TroLyAoService.diemTen(" astrox 99 ", "Vợt cầu lông Yonex Astrox 99 Pro", "Yonex"));
        assertEquals(0, TroLyAoService.diemTen(" vot yonex 4u ", "Vợt cầu lông Yonex Astrox 99 Pro 4U", "Yonex"));
        assertEquals(0, TroLyAoService.diemTen(" vot tan cong ", "Quả cầu lông Thành Công", "Thành Công"));
    }

    /* ---------- trả lời ---------- */

    @Test
    void chao_vaGoiYMacDinh() {
        TroLyAoService.TraLoi tl = troLy.traLoi("Xin chào shop", lan);
        assertTrue(tl.noiDung().startsWith("Xin chào Lan!"));
        assertEquals(TroLyAoService.GOI_Y_MAC_DINH, tl.goiY());
        assertTrue(troLy.loiChao(null).noiDung().contains("Xin chào bạn!"));
    }

    @Test
    void gapNhanVien_chuyenChoNhanVien() {
        assertTrue(troLy.traLoi("Cho mình gặp nhân viên tư vấn", lan).chuyenNhanVien());
        assertTrue(troLy.traLoi("cho minh noi chuyen voi nguoi that", lan).chuyenNhanVien());
        assertFalse(troLy.traLoi("vợt yonex", lan).chuyenNhanVien());
    }

    @Test
    void traCuuDon_dungSdtMoiXemDuoc() {
        String dung = hoi("Kiểm tra đơn HD12 giúp mình");
        assertTrue(dung.contains("Đơn HD12 (đặt online)"), dung);
        assertTrue(dung.contains("Đang giao hàng") || dung.contains("Đang giao"), dung);
        assertTrue(dung.contains("2.631.000 ₫"), dung);
        assertTrue(dung.contains("Giao Hàng Nhanh"), dung);
        // Gõ mã kiểu cũ có số 0 đệm vẫn tìm được
        assertTrue(hoi("đơn hd0012").contains("Đơn HD12"));
        // Người khác (SĐT khác) không xem được, và không bị lộ trạng thái
        String sai = troLy.traLoi("đơn HD12", new TroLyAoService.NguCanh("X", "0987654321")).noiDung();
        assertTrue(sai.contains("không tìm thấy đơn HD12"), sai);
        assertTrue(sai.contains("0987***321"), sai);
        assertFalse(sai.contains("Đang giao"), sai);
        // SĐT gõ trong câu được ưu tiên
        assertTrue(troLy.traLoi("đơn HD12 sdt 0901000001", new TroLyAoService.NguCanh("X", "0987654321")).noiDung().contains("Trạng thái"));
        // SĐT theo quy tắc chung (10 số, bắt đầu bằng 0) - kể cả số máy bàn 02x - vẫn nhận ra khi gõ trong câu
        hoaDons.get(0).setSdtNguoiNhan("0243123456");
        assertTrue(troLy.traLoi("đơn HD12 sdt 0243123456", new TroLyAoService.NguCanh("X", "0987654321")).noiDung().contains("Trạng thái"));
        assertTrue(hoi("Đơn HD99").contains("không tìm thấy đơn HD99"));
        TroLyAoService.TraLoi hoiDon = troLy.traLoi("mình muốn kiểm tra đơn hàng", null);
        assertTrue(hoiDon.noiDung().contains("chọn đơn cần tư vấn"), hoiDon.noiDung());
        assertTrue(hoiDon.goiY().contains(TroLyAoService.GOI_Y_CHON_DON), "khung chat mở danh sách đơn để khách chọn");
    }

    @Test
    void khuyenMai_chiPhieuCongKhaiConHan_vaDotDangChay() {
        String km = hoi("Shop có khuyến mãi gì không?");
        assertTrue(km.contains("Mã PGG01: giảm 10% (tối đa 200.000 ₫) cho đơn từ 1.000.000 ₫"), km);
        assertFalse(km.contains("PGG02"), "phiếu cá nhân không công khai");
        assertFalse(km.contains("PGG03"), "phiếu hết hạn");
        assertTrue(km.contains("Sale tháng 10: giảm đến 10%"), km);
        phieus.clear();
        dots.clear();
        assertTrue(hoi("có voucher không").contains("chưa có chương trình khuyến mãi"));
    }

    /** Id các thẻ sản phẩm trợ lý gửi kèm câu trả lời (1 Astrox, 2 Nanoflare, 3 Axforce, 4 quả cầu, 5 Thruster hết hàng). */
    private java.util.List<Integer> the(String cau) {
        return troLy.traLoi(cau, null).sanPham();
    }

    // Cô review 14/11 [20:24]: gợi ý sản phẩm trả về từng THẺ (ảnh, tên, giá) bấm được, không phải đoạn chữ dài
    @Test
    void timSanPham_theoHangGiaLoiChoi_traVeTheSanPham() {
        TroLyAoService.TraLoi duoi3 = troLy.traLoi("vợt yonex dưới 3 triệu", null);
        assertEquals(java.util.List.of(2), duoi3.sanPham(), "chỉ Nanoflare (sau giảm 2.601.000 ₫), không Astrox, không hãng khác");
        assertTrue(duoi3.noiDung().contains("Có 1 sản phẩm còn hàng phù hợp (vợt, hãng Yonex, giá dưới 3.000.000 ₫)"), duoi3.noiDung());
        assertTrue(duoi3.noiDung().contains("Bấm vào thẻ"), duoi3.noiDung());
        assertFalse(duoi3.noiDung().contains("•"), "không còn danh sách chữ dài: " + duoi3.noiDung());
        assertTrue(duoi3.noiDungLuu().endsWith("\n" + TroLyAoService.MO_THE_SP + "2]]"), duoi3.noiDungLuu());

        assertEquals(java.util.List.of(1), the("tư vấn vợt tấn công"), "Astrox; Thruster hết hàng thì không gợi ý");
        assertEquals(java.util.Set.of(2, 3), new java.util.HashSet<>(the("vot cho nguoi moi choi")));
        assertEquals(java.util.Set.of(1, 2), new java.util.HashSet<>(the("vợt 4U")));
        assertEquals(java.util.List.of(2), the("vợt đang giảm giá"));
        assertEquals(java.util.List.of(4), the("ống cầu nào tốt"));
        assertTrue(hoi("vợt dưới 500k").contains("Hiện chưa có sản phẩm còn hàng phù hợp (vợt, giá dưới 500.000 ₫)"));
        assertTrue(the("vợt dưới 500k").isEmpty());
    }

    @Test
    void timSanPham_theoMauSac() {
        TroLyAoService.TraLoi xanhDuong = troLy.traLoi("vợt 3 triệu xanh dương", null);
        assertEquals(java.util.List.of(2), xanhDuong.sanPham(), "chỉ Nanoflare màu xanh (2.601.000 ₫), không lấy Astrox (đen/đỏ)");
        assertTrue(xanhDuong.noiDung().contains("màu xanh dương"), xanhDuong.noiDung());
        assertTrue(xanhDuong.noiDung().contains("giá 2.400.000 ₫ - 3.600.000 ₫"), xanhDuong.noiDung());

        TroLyAoService.TraLoi mauDo = troLy.traLoi("vợt 3 triệu màu đỏ", null);
        assertTrue(mauDo.sanPham().isEmpty(), "không có vợt màu đỏ nào tầm 3 triệu");
        assertTrue(mauDo.noiDung().contains("Hiện chưa có sản phẩm còn hàng phù hợp (vợt, màu đỏ, giá 2.400.000 ₫ - 3.600.000 ₫)"), mauDo.noiDung());

        TroLyAoService.TraLoi mauTrang = troLy.traLoi("vợt màu trắng", null);
        assertEquals(java.util.List.of(3), mauTrang.sanPham(), "chỉ Axforce màu trắng");
    }

    @Test
    void goTenMau_traChiTiet() {
        String ct = hoi("astrox 99 còn hàng không");
        assertTrue(ct.startsWith("Vợt cầu lông Yonex Astrox 99 Pro (hãng Yonex, nặng đầu, thân cứng)"), ct);
        assertTrue(ct.contains("Đen · 4U: 4.290.000 ₫ - còn hàng"), ct);
        assertTrue(ct.contains("Đỏ · 3U: 4.390.000 ₫ - chỉ còn 2"), ct);
        String het = hoi("thruster 9900");
        assertTrue(het.contains("hết hàng") && het.contains("tạm hết hàng"), het);
        String nano = hoi("nanoflare 700 giá bao nhiêu");
        assertTrue(nano.contains("2.601.000 ₫ (giảm 10% từ 2.890.000 ₫)"), nano);
        assertEquals(java.util.List.of(2), the("nanoflare 700 giá bao nhiêu"), "kèm thẻ của đúng mẫu đó");
    }

    @Test
    void cauHoiThuongGap() {
        assertTrue(hoi("ship mất bao lâu vậy").contains("giao hàng toàn quốc"));
        assertTrue(hoi("Địa chỉ shop ở đâu").contains("Số 15 Trần Phú, Hà Đông"));
        assertTrue(hoi("hotline của shop").contains("0912 000 001"));
        assertTrue(hoi("mấy giờ mở cửa").contains("7:00 - 22:00"));
        assertTrue(hoi("chính sách đổi trả thế nào").contains("Đổi sản phẩm trong 7 ngày"));
        assertTrue(hoi("có trả bằng chuyển khoản được không").contains("chuyển khoản"));
        assertTrue(hoi("nên căng dây bao nhiêu kg").contains("9 - 10 kg"));
    }

    @Test
    void khongHieu_goiYVaMoiGapNhanVien() {
        TroLyAoService.TraLoi tl = troLy.traLoi("abc xyz", lan);
        assertTrue(tl.noiDung().contains("chưa hiểu"));
        assertEquals(TroLyAoService.GOI_Y_MAC_DINH, tl.goiY());
        assertEquals("Bạn cần mình giúp gì ạ?", troLy.traLoi("  ?? ", lan).noiDung());
        assertTrue(hoi("cảm ơn shop").startsWith("Cảm ơn bạn"));
    }

    @Test
    void cauTraLoi_khongQuaDoDaiCot() {
        for (int i = 10; i < 40; i++) {
            SanPham sp = sanPham(i, "Vợt cầu lông Yonex Mẫu Thử " + i, "Yonex", "Vợt cầu lông", "Cân bằng", "Cứng");
            for (int k = 0; k < 6; k++) bienThe(i * 100 + k, sp, "Màu rất dài số " + k, "4U", 1_000_000L + k, 3);
        }
        for (String c : List.of("vợt yonex", "vot", "khuyen mai", "yonex mau thu 15")) {
            assertTrue(troLy.traLoi(c, lan).noiDung().length() <= TroLyAoService.DO_DAI_TOI_DA + 1, c);
        }
    }

    @Test
    void hoiPhiShip_traLoiDungCachHeThongTinh_theoTinhVaNguongMienPhi() {
        // Đang tắt miễn phí theo giá trị đơn: chỉ nói phí theo tỉnh, không còn câu "30.000 - 35.000 ₫ tùy đơn vị"
        String tat = hoi("phí ship bao nhiêu vậy shop");
        assertTrue(tat.contains("Giao Hàng Nhanh"), tat);
        assertTrue(tat.contains("Hà Nội 25.000 ₫ (phường) / 30.000 ₫ (xã)"), tat);
        assertTrue(tat.contains("đến 55.000 ₫ tùy tỉnh"), tat);
        assertFalse(tat.contains("30.000 - 35.000"), tat);
        assertFalse(tat.contains("được miễn phí vận chuyển"), tat);
        // Quản lý bật miễn phí cho đơn từ 2.000.000 ₫: trợ lý nói đúng mức đang cài đặt
        troLy.setCaiDatVanChuyen(CaiDatVanChuyenService.trongBoNho(true, 2_000_000L));
        String bat = hoi("giao hàng thế nào");
        assertTrue(bat.contains("Đơn có tiền hàng từ 2.000.000 ₫ được miễn phí vận chuyển"), bat);
        assertEquals(25_000L, PhiVanChuyenTinh.phiThapNhat());
        assertEquals(55_000L, PhiVanChuyenTinh.phiCaoNhat());
    }
}
