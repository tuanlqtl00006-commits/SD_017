package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.dto.BienTheMoiRequest;
import com.footstyle.demo.dto.BienTheRequest;
import com.footstyle.demo.dto.SanPhamRequest;
import com.footstyle.demo.dto.SanPhamResponse;
import com.footstyle.demo.dto.ThuocTinhRequest;
import com.footstyle.demo.entity.ChatLieu;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuocTinh;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.entity.XuatXu;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ChatLieuRepository;
import com.footstyle.demo.repository.ChuViRepository;
import com.footstyle.demo.repository.DanhMucRepository;
import com.footstyle.demo.repository.DiemCanBangRepository;
import com.footstyle.demo.repository.DoCungRepository;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.MauSacRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.repository.SanPhamRepository;
import com.footstyle.demo.repository.ThuongHieuRepository;
import com.footstyle.demo.repository.TrongLuongRepository;
import com.footstyle.demo.repository.XuatXuRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Bộ thuộc tính theo danh mục: vợt có Độ cứng / Điểm cân bằng / Trọng lượng / Chu vi thật, danh mục khác dùng "Không áp dụng".
 * Kiểm tra SanPhamService, BienTheService, ThuocTinhService thật với kho giả trong bộ nhớ.
 */
class SanPhamTheoDanhMucTest {

    // id 1 = giá trị thật của vợt, id 9 = "Không áp dụng"
    private static final int VOT = 1, PHU_KIEN = 2, THAT = 1, KAD = 9;

    private final List<SanPham> sanPhams = new ArrayList<>();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final List<DanhMuc> danhMucs = new ArrayList<>(List.of(tt(DanhMuc::new, VOT, "Vợt cầu lông"), tt(DanhMuc::new, PHU_KIEN, "Phụ kiện")));
    private final List<DoCung> doCungs = new ArrayList<>(List.of(tt(DoCung::new, THAT, "Cứng"), tt(DoCung::new, KAD, "Không áp dụng")));
    private final List<DiemCanBang> canBangs = List.of(tt(DiemCanBang::new, THAT, "Nặng đầu"), tt(DiemCanBang::new, KAD, "Không áp dụng"));
    private final List<TrongLuong> trongLuongs = List.of(tt(TrongLuong::new, THAT, "4U (80-84g)"), tt(TrongLuong::new, KAD, "Không áp dụng"), tt(TrongLuong::new, 2, "3U (85-89g)"));
    private final List<ChuVi> chuVis = List.of(tt(ChuVi::new, THAT, "G5"), tt(ChuVi::new, KAD, "Không áp dụng"), tt(ChuVi::new, 2, "G6"));
    private final List<MauSac> maus = List.of(tt(MauSac::new, 1, "Đen"), tt(MauSac::new, 2, "Xanh dương"));

    private final SanPhamService sanPhamService;
    private final BienTheService bienTheService;
    private final ThuocTinhService thuocTinhService;

    private static <T extends ThuocTinh> T tt(Supplier<T> tao, int id, String ten) {
        T x = tao.get();
        x.setId(id);
        x.setMa("M" + id);
        x.setTen(ten);
        x.setTrangThai(ThuocTinh.HOAT_DONG);
        return x;
    }

    /** Kho thuộc tính: findById, existsByTenIgnoreCaseAndIdNot, save, findAll. */
    private static <R> R kho(Class<R> kieu, List<? extends ThuocTinh> ds) {
        return KhoGia.tao(kieu, (ten, a) -> switch (ten) {
            case "findById" -> ds.stream().filter(x -> x.getId().equals(a[0])).findFirst();
            case "existsByTenIgnoreCaseAndIdNot" -> ds.stream().anyMatch(x -> x.getTen().equalsIgnoreCase((String) a[0]) && !x.getId().equals(a[1]));
            case "existsById" -> ds.stream().anyMatch(x -> x.getId().equals(a[0]));
            case "save" -> a[0];
            case "findAll" -> new ArrayList<>(ds);
            default -> KhoGia.KHONG_CO;
        });
    }

    SanPhamTheoDanhMucTest() {
        SanPhamRepository spRepo = KhoGia.tao(SanPhamRepository.class, (ten, a) -> switch (ten) {
            case "findByDanhMucIdAndThuongHieuId" -> sanPhams.stream()
                    .filter(s -> s.getDanhMuc().getId().equals(a[0]) && s.getThuongHieu().getId().equals(a[1])).toList();
            case "findAllMaSanPham" -> sanPhams.stream().map(SanPham::getMaSanPham).toList();
            case "existsByMaSanPhamIgnoreCase" -> sanPhams.stream().anyMatch(s -> s.getMaSanPham().equalsIgnoreCase((String) a[0]));
            case "existsByDanhMucId" -> sanPhams.stream().anyMatch(s -> s.getDanhMuc().getId().equals(a[0]));
            case "findById" -> sanPhams.stream().filter(s -> s.getId().equals(a[0])).findFirst();
            case "save" -> {
                SanPham s = (SanPham) a[0];
                if (s.getId() == null) {
                    s.setId(sanPhams.size() + 1);
                    sanPhams.add(s);
                }
                yield s;
            }
            default -> KhoGia.KHONG_CO;
        });
        SanPhamChiTietRepository btRepo = KhoGia.tao(SanPhamChiTietRepository.class, (ten, a) -> switch (ten) {
            case "existsBySanPhamId" -> bienThes.stream().anyMatch(b -> b.getSanPham().getId().equals(a[0]));
            case "existsByMaSpctIgnoreCase" -> bienThes.stream().anyMatch(b -> b.getMaSpct().equalsIgnoreCase((String) a[0]));
            case "existsByMaSpctIgnoreCaseAndIdNot" -> bienThes.stream().anyMatch(b -> b.getMaSpct().equalsIgnoreCase((String) a[0]) && !b.getId().equals(a[1]));
            case "findBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViId" -> bienThes.stream().filter(b -> trung(b, a)).toList();
            case "existsBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViIdAndIdNot" ->
                    bienThes.stream().anyMatch(b -> trung(b, a) && !b.getId().equals(a[4]));
            case "findById" -> bienThes.stream().filter(b -> b.getId().equals(a[0])).findFirst();
            case "save" -> {
                SanPhamChiTiet b = (SanPhamChiTiet) a[0];
                if (b.getId() == null) {
                    b.setId(bienThes.size() + 1);
                    bienThes.add(b);
                }
                yield b;
            }
            default -> KhoGia.KHONG_CO;
        });
        HinhAnhSanPhamRepository anhRepo = KhoGia.tao(HinhAnhSanPhamRepository.class, (ten, a) -> switch (ten) {
            case "findBySanPhamId" -> List.of();
            case "saveAll" -> a[0];
            default -> KhoGia.KHONG_CO;
        });
        DanhMucRepository dmRepo = kho(DanhMucRepository.class, danhMucs);
        DoCungRepository dcRepo = kho(DoCungRepository.class, doCungs);
        MauSacRepository msRepo = kho(MauSacRepository.class, maus);
        TrongLuongRepository tlRepo = kho(TrongLuongRepository.class, trongLuongs);
        ChuViRepository cvRepo = kho(ChuViRepository.class, chuVis);
        DiemCanBangRepository cbRepo = kho(DiemCanBangRepository.class, canBangs);
        ThuongHieuRepository thRepo = kho(ThuongHieuRepository.class, List.of(tt(ThuongHieu::new, 1, "Yonex")));
        XuatXuRepository xxRepo = kho(XuatXuRepository.class, List.of(tt(XuatXu::new, 1, "Nhật Bản")));
        ChatLieuRepository clRepo = kho(ChatLieuRepository.class, List.of(tt(ChatLieu::new, 1, "Carbon")));

        bienTheService = new BienTheService(btRepo, spRepo, msRepo, tlRepo, cvRepo, anhRepo);
        sanPhamService = new SanPhamService(spRepo, anhRepo, dmRepo, thRepo, xxRepo, clRepo, dcRepo, cbRepo, msRepo, bienTheService);
        thuocTinhService = new ThuocTinhService(dmRepo, thRepo, xxRepo, clRepo, dcRepo, cbRepo, msRepo, tlRepo, cvRepo, spRepo);
    }

    private static boolean trung(SanPhamChiTiet b, Object[] a) {
        return b.getSanPham().getId().equals(a[0]) && b.getMauSac().getId().equals(a[1])
                && b.getTrongLuong().getId().equals(a[2]) && b.getChuVi().getId().equals(a[3]);
    }

    private static SanPhamRequest sp(String ten, int danhMuc, int doCung, int canBang, List<BienTheMoiRequest> bienThes) {
        // ảnh là bắt buộc nên mọi sản phẩm trong test đều có 1 ảnh chung
        return new SanPhamRequest("", ten, danhMuc, 1, 1, 1, doCung, canBang, "", "/api/uploads/anh.jpg", List.of(), bienThes, null, null);
    }

    private static BienTheMoiRequest bt(int mau, int tl, int cv) {
        return new BienTheMoiRequest(mau, tl, cv, 500_000L, 10);
    }

    private static String loi(Runnable r) {
        return assertThrows(ApiException.class, r::run).getMessage();
    }

    /* ---------- sản phẩm ---------- */

    @Test
    void votPhaiCoDoCungDiemCanBangThat_phuKienPhaiLaKhongApDung() {
        assertTrue(loi(() -> sanPhamService.them(sp("Astrox 99", VOT, KAD, THAT, null))).contains("độ cứng"));
        assertTrue(loi(() -> sanPhamService.them(sp("Astrox 99", VOT, THAT, KAD, null))).contains("điểm cân bằng"));
        assertTrue(loi(() -> sanPhamService.them(sp("Quấn cán", PHU_KIEN, THAT, KAD, null))).contains("Không áp dụng"));
        assertEquals(0, sanPhams.size(), "lỗi thì chưa lưu");
        sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, null));
        sanPhamService.them(sp("Quấn cán", PHU_KIEN, KAD, KAD, null));
        assertEquals(2, sanPhams.size());
    }

    @Test
    void phuKien_bienTheChiTheoMau_maKhongCoTrongLuongChuVi() {
        sanPhamService.them(sp("Quấn cán", PHU_KIEN, KAD, KAD, List.of(bt(1, KAD, KAD), bt(2, KAD, KAD))));
        assertEquals(List.of("SP01-DEN", "SP01-XANHDUONG"), bienThes.stream().map(SanPhamChiTiet::getMaSpct).toList());
        // phụ kiện mà gửi trọng lượng thật -> lỗi, không lưu nửa chừng sản phẩm mới
        assertTrue(loi(() -> sanPhamService.them(sp("Bao vợt", PHU_KIEN, KAD, KAD, List.of(bt(1, THAT, KAD))))).contains("trọng lượng"));
    }

    @Test
    void votHaiMauHaiTrongLuongHaiChuVi_taoDung8BienThe() {
        // Cô: "2 x 2 x 2 = 8 thì phải có 8 biến thể"
        List<BienTheMoiRequest> ds = new ArrayList<>();
        for (int mau : new int[] {1, 2}) for (int tl : new int[] {THAT, 2}) for (int cv : new int[] {THAT, 2}) ds.add(bt(mau, tl, cv));
        assertEquals(8, ds.size());
        sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, ds));
        assertEquals(8, bienThes.size(), "2 màu x 2 trọng lượng x 2 chu vi = 8 biến thể");
        assertEquals(8, bienThes.stream().map(SanPhamChiTiet::getMaSpct).distinct().count(), "mã biến thể không trùng nhau");
        assertEquals(4, bienThes.stream().filter(b -> b.getMauSac().getId() == 1).count(), "mỗi màu có 4 biến thể");
    }

    @Test
    void vot_bienTheKhongDuocChonKhongApDung() {
        assertTrue(loi(() -> sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, List.of(bt(1, THAT, KAD))))).contains("chu vi"));
        sanPhamService.them(sp("Nanoflare", VOT, THAT, THAT, List.of(bt(1, THAT, THAT))));
        assertEquals("SP02-DEN-4U-G5", bienThes.get(0).getMaSpct());
    }

    @Test
    void suaDoiTenTrungSanPhamKhacCungDanhMucThuongHieu_baoTrung() {
        sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, null));
        SanPhamResponse b = sanPhamService.them(sp("Astrox 88", VOT, THAT, THAT, null));
        ApiException e = assertThrows(ApiException.class, () -> sanPhamService.sua(b.id(), sp("  astrox   99 ", VOT, THAT, THAT, null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("Astrox 88", sanPhams.get(1).getTenSanPham(), "không đổi gì");
        // sửa chính nó (giữ tên) vẫn được
        assertEquals("Astrox 88 Pro", sanPhamService.sua(b.id(), sp("Astrox 88 Pro", VOT, THAT, THAT, null)).ten());
    }

    @Test
    void doiDanhMucVotSangPhuKien_khiDaCoBienThe_baoLoi_chuaCoBienTheThiDuoc() {
        SanPhamResponse coBt = sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, List.of(bt(1, THAT, THAT))));
        ApiException e = assertThrows(ApiException.class, () -> sanPhamService.sua(coBt.id(), sp("Astrox 99", PHU_KIEN, KAD, KAD, null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertTrue(e.getMessage().contains("đã có biến thể"));

        SanPhamResponse chuaCo = sanPhamService.them(sp("Nanoflare", VOT, THAT, THAT, null));
        assertEquals(PHU_KIEN, sanPhamService.sua(chuaCo.id(), sp("Nanoflare", PHU_KIEN, KAD, KAD, null)).idDanhMuc());
    }

    @Test
    void duLieuCuSaiBoThuocTinh_giuNguyenThiVanSuaDuoc() {
        SanPhamResponse r = sanPhamService.them(sp("Quấn cán", PHU_KIEN, KAD, KAD, null));
        sanPhams.get(0).setDoCung(doCungs.get(0)); // dữ liệu cũ: phụ kiện lại có độ cứng "Cứng"
        assertEquals("Quấn cán mới", sanPhamService.sua(r.id(), sp("Quấn cán mới", PHU_KIEN, THAT, KAD, null)).ten());
    }

    /* ---------- biến thể ---------- */

    @Test
    void themBienTheLe_kiemTraTheoDanhMucCuaSanPham() {
        SanPhamResponse vot = sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, null));
        SanPhamResponse pk = sanPhamService.them(sp("Quấn cán", PHU_KIEN, KAD, KAD, null));
        assertTrue(loi(() -> bienTheService.them(new BienTheRequest(vot.id(), 1, KAD, THAT, "SP01-DEN", 500_000L, 1, null))).contains("trọng lượng"));
        assertTrue(loi(() -> bienTheService.them(new BienTheRequest(pk.id(), 1, KAD, THAT, "SP02-DEN", 50_000L, 1, null))).contains("chu vi"));
        assertEquals("SP02-DEN", bienTheService.them(new BienTheRequest(pk.id(), 1, KAD, KAD, "sp02-den", 50_000L, 1, null)).ma());
    }

    @Test
    void chuyenBienTheSangSanPhamKhacDanhMuc_baoLoi() {
        SanPhamResponse vot = sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, null));
        SanPhamResponse pk = sanPhamService.them(sp("Quấn cán", PHU_KIEN, KAD, KAD, List.of(bt(1, KAD, KAD))));
        Integer idBt = bienThes.get(0).getId();
        assertTrue(loi(() -> bienTheService.sua(idBt, new BienTheRequest(vot.id(), 1, KAD, KAD, null, 50_000L, 1, null))).contains("trọng lượng"));
        assertEquals(pk.id(), bienThes.get(0).getSanPham().getId(), "lỗi thì biến thể vẫn ở sản phẩm cũ");
        // chuyển đúng bộ thuộc tính vợt thì được, mã đổi phần đầu theo sản phẩm mới
        assertEquals("SP01-DEN", bienTheService.sua(idBt, new BienTheRequest(vot.id(), 1, THAT, THAT, null, 500_000L, 1, null)).ma());
    }

    @Test
    void giaBanVaTonKhoBien() {
        SanPhamResponse pk = sanPhamService.them(sp("Quấn cán", PHU_KIEN, KAD, KAD, null));
        assertTrue(loi(() -> bienTheService.them(new BienTheRequest(pk.id(), 1, KAD, KAD, "SP01-DEN", 999L, 1, null))).contains("1.000"));
        assertTrue(loi(() -> bienTheService.them(new BienTheRequest(pk.id(), 1, KAD, KAD, "SP01-DEN", 1_000_000_001L, 1, null))).contains("tối đa"));
        assertTrue(loi(() -> bienTheService.them(new BienTheRequest(pk.id(), 1, KAD, KAD, "SP01-DEN", 1000L, -1, null))).contains("Số lượng"));
        assertEquals(1000L, bienTheService.them(new BienTheRequest(pk.id(), 1, KAD, KAD, "SP01-DEN", 1000L, 0, null)).giaBan());
    }

    @Test
    void danhSachBienTheCoHaiDongTrung_baoLoiTruocKhiLuu() {
        assertTrue(loi(() -> sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, List.of(bt(1, THAT, THAT), bt(1, THAT, THAT))))).contains("hai dòng"));
        assertTrue(bienThes.isEmpty());
    }

    /* ---------- thuộc tính ---------- */

    @Test
    void mucKhongApDung_khongDoiTenKhongNgungDuoc() {
        assertTrue(loi(() -> thuocTinhService.sua("do-cung", KAD, new ThuocTinhRequest("Mềm", null))).contains("Không áp dụng"));
        assertTrue(loi(() -> thuocTinhService.doiTrangThai("do-cung", KAD)).contains("không ngưng"));
        // đổi cách viết hoa vẫn được (vẫn là "Không áp dụng")
        assertEquals("không áp dụng", thuocTinhService.sua("do-cung", KAD, new ThuocTinhRequest(" không  áp dụng ", null)).ten());
        // mục thường thì ngưng được
        assertEquals(false, thuocTinhService.doiTrangThai("do-cung", THAT).hoatDong());
    }

    @Test
    void doiTenDanhMucLamDoiLoaiVot_khiDaCoSanPham_baoLoi() {
        sanPhamService.them(sp("Astrox 99", VOT, THAT, THAT, null));
        ApiException e = assertThrows(ApiException.class, () -> thuocTinhService.sua("danh-muc", VOT, new ThuocTinhRequest("Phụ kiện cầu lông", null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        // đổi tên mà vẫn là vợt thì được
        assertEquals("Vợt cầu lông cao cấp", thuocTinhService.sua("danh-muc", VOT, new ThuocTinhRequest("Vợt cầu lông cao cấp", null)).ten());
        // danh mục chưa có sản phẩm thì đổi thoải mái
        assertEquals("Vợt trẻ em", thuocTinhService.sua("danh-muc", PHU_KIEN, new ThuocTinhRequest("Vợt trẻ em", null)).ten());
    }

    @Test
    void loaiThuocTinhLa_bao404() {
        ApiException e = assertThrows(ApiException.class, () -> thuocTinhService.getAll("kich-co"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void nhomDanhMuc_loiThuocTinhVot() {
        assertNull(NhomDanhMuc.loiThuocTinhVot("Vợt cầu lông người mới", "3U", "trọng lượng"));
        assertNull(NhomDanhMuc.loiThuocTinhVot("Quả cầu lông", "KHÔNG ÁP DỤNG", "trọng lượng"));
        assertTrue(NhomDanhMuc.laKhongApDung("  Khong  ap dung "));
        assertTrue(NhomDanhMuc.loiThuocTinhVot("Danh mục lạ", "3U", "trọng lượng") != null, "danh mục không phải vợt");
    }
}
