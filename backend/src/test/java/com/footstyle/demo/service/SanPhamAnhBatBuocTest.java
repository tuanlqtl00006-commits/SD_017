package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.dto.AnhTheoMauRequest;
import com.footstyle.demo.dto.BienTheMoiRequest;
import com.footstyle.demo.dto.SanPhamRequest;
import com.footstyle.demo.dto.SanPhamResponse;
import com.footstyle.demo.entity.ChatLieu;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.HinhAnhSanPham;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Cô: "Ảnh đang chưa có mà tại sao vẫn thành công" -> sản phẩm KHÔNG có ảnh thì không được lưu (cả thêm lẫn sửa).
 * Hợp lệ khi có ít nhất 1 ảnh chung, hoặc mọi màu của biến thể đều có ảnh riêng. Dùng SanPhamService + BienTheService thật,
 * kho giả trong bộ nhớ.
 */
class SanPhamAnhBatBuocTest {

    private static final int PK = 1, KAD = 1, DEN = 1, DO = 2;
    private static final String ANH_CHUNG = "/api/uploads/chung.jpg";

    private final List<SanPham> sanPhams = new ArrayList<>();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final List<HinhAnhSanPham> anhs = new ArrayList<>();
    private int idAnh = 0;

    private final SanPhamService service;

    private static <T extends ThuocTinh> T tt(Supplier<T> tao, int id, String ten) {
        T x = tao.get();
        x.setId(id);
        x.setMa("M" + id);
        x.setTen(ten);
        x.setTrangThai(ThuocTinh.HOAT_DONG);
        return x;
    }

    private static <R> R kho(Class<R> kieu, List<? extends ThuocTinh> ds) {
        return KhoGia.tao(kieu, (ten, a) -> switch (ten) {
            case "findById" -> ds.stream().filter(x -> x.getId().equals(a[0])).findFirst();
            case "existsById" -> ds.stream().anyMatch(x -> x.getId().equals(a[0]));
            default -> KhoGia.KHONG_CO;
        });
    }

    SanPhamAnhBatBuocTest() {
        SanPhamRepository spRepo = KhoGia.tao(SanPhamRepository.class, (ten, a) -> switch (ten) {
            case "findByDanhMucIdAndThuongHieuId" -> sanPhams.stream()
                    .filter(s -> s.getDanhMuc().getId().equals(a[0]) && s.getThuongHieu().getId().equals(a[1])).toList();
            case "findAllMaSanPham" -> sanPhams.stream().map(SanPham::getMaSanPham).toList();
            case "existsByMaSanPhamIgnoreCase" -> sanPhams.stream().anyMatch(s -> s.getMaSanPham().equalsIgnoreCase((String) a[0]));
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
            case "findBySanPhamIdOrderByIdDesc" -> bienThes.stream().filter(b -> b.getSanPham().getId().equals(a[0])).toList();
            case "existsByMaSpctIgnoreCase" -> bienThes.stream().anyMatch(b -> b.getMaSpct().equalsIgnoreCase((String) a[0]));
            case "existsByMaSpctIgnoreCaseAndIdNot" ->
                    bienThes.stream().anyMatch(b -> b.getMaSpct().equalsIgnoreCase((String) a[0]) && !b.getId().equals(a[1]));
            case "findBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViId" -> bienThes.stream().filter(b -> trung(b, a)).toList();
            case "existsBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViIdAndIdNot" ->
                    bienThes.stream().anyMatch(b -> trung(b, a) && !b.getId().equals(a[4]));
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
            case "findBySanPhamId" -> anhs.stream().filter(h -> h.getSanPham().getId().equals(a[0])).toList();
            case "saveAll" -> {
                for (Object o : (Iterable<?>) a[0]) {
                    HinhAnhSanPham h = (HinhAnhSanPham) o;
                    if (h.getId() == null) {
                        h.setId(++idAnh);
                        anhs.add(h);
                    }
                }
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        MauSacRepository msRepo = kho(MauSacRepository.class, List.of(tt(MauSac::new, DEN, "Đen"), tt(MauSac::new, DO, "Đỏ")));
        BienTheService bienThe = new BienTheService(btRepo, spRepo, msRepo,
                kho(TrongLuongRepository.class, List.of(tt(TrongLuong::new, KAD, "Không áp dụng"))),
                kho(ChuViRepository.class, List.of(tt(ChuVi::new, KAD, "Không áp dụng"))), anhRepo);
        service = new SanPhamService(spRepo, anhRepo,
                kho(DanhMucRepository.class, List.of(tt(DanhMuc::new, PK, "Phụ kiện"))),
                kho(ThuongHieuRepository.class, List.of(tt(ThuongHieu::new, 1, "Yonex"))),
                kho(XuatXuRepository.class, List.of(tt(XuatXu::new, 1, "Nhật Bản"))),
                kho(ChatLieuRepository.class, List.of(tt(ChatLieu::new, 1, "Cotton"))),
                kho(DoCungRepository.class, List.of(tt(DoCung::new, KAD, "Không áp dụng"))),
                kho(DiemCanBangRepository.class, List.of(tt(DiemCanBang::new, KAD, "Không áp dụng"))),
                msRepo, bienThe);
    }

    private static boolean trung(SanPhamChiTiet b, Object[] a) {
        return b.getSanPham().getId().equals(a[0]) && b.getMauSac().getId().equals(a[1])
                && b.getTrongLuong().getId().equals(a[2]) && b.getChuVi().getId().equals(a[3]);
    }

    private static BienTheMoiRequest bt(int mau) {
        return new BienTheMoiRequest(mau, KAD, KAD, 50_000L, 5);
    }

    private static AnhTheoMauRequest anhMau(int mau, String... urls) {
        return new AnhTheoMauRequest(mau, List.of(urls));
    }

    private static SanPhamRequest req(String ten, String anhChinh, List<BienTheMoiRequest> bienThes, List<AnhTheoMauRequest> anhTheoMau, Boolean xacNhan) {
        return new SanPhamRequest("", ten, PK, 1, 1, 1, KAD, KAD, "", anhChinh, List.of(), bienThes, xacNhan, anhTheoMau);
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    private long soAnhDangDung() {
        return anhs.stream().filter(h -> h.getTrangThai() == HinhAnhSanPham.HOAT_DONG).count();
    }

    /* ---------- THÊM ---------- */

    @Test
    void themKhongCoAnhNaoCa_luuThanhCongVaKhongLoi() {
        SanPhamResponse r = service.them(req("Cuốn cán", "", null, null, null));
        assertEquals(1, sanPhams.size());
        assertEquals("", r.anhChinh());
        assertTrue(anhs.isEmpty());
    }

    @Test
    void themCoBienTheNhungKhongCoAnh_luuThanhCong() {
        SanPhamResponse r = service.them(req("Cuốn cán", "", List.of(bt(DEN), bt(DO)), null, null));
        assertEquals(1, sanPhams.size());
        assertEquals(2, bienThes.size());
        assertEquals("", r.anhChinh());
    }

    @Test
    void themThieuAnhCuaMotMau_luuThanhCong() {
        service.them(req("Cuốn cán", "", List.of(bt(DEN), bt(DO)),
                List.of(anhMau(DEN, "/api/uploads/den.jpg"), anhMau(DO)), null));
        assertEquals(1, sanPhams.size());
        assertEquals(1, soAnhDangDung());
    }

    @Test
    void themCoAnhChung_duocDuKhongCoAnhTheoMau() {
        SanPhamResponse r = service.them(req("Cuốn cán", ANH_CHUNG, List.of(bt(DEN), bt(DO)), null, null));
        assertEquals(1, sanPhams.size());
        assertEquals(2, bienThes.size());
        assertEquals(ANH_CHUNG, r.anhChinh());
    }

    @Test
    void themKhongCoAnhChungNhungMoiMauDeuCoAnhRieng_duoc() {
        service.them(req("Cuốn cán", "", List.of(bt(DEN), bt(DO)),
                List.of(anhMau(DEN, "/api/uploads/den.jpg"), anhMau(DO, "/api/uploads/do.jpg")), null));
        assertEquals(1, sanPhams.size());
        assertEquals(2, soAnhDangDung());
    }

    @Test
    void themKhongCoBienTheNhungCoAnhRiengCuaMotMau_duocVaCoAnhThiLuuDuoc() {
        service.them(req("Cuốn cán", "", null, List.of(anhMau(DEN, "/api/uploads/den.jpg")), null));
        assertEquals(1, sanPhams.size());
    }

    @Test
    void anhChiLaKhoangTrangThiTinhLaAnhRong_luuThanhCong() {
        SanPhamResponse r = service.them(req("Cuốn cán", "   ", null, null, null));
        assertEquals(1, sanPhams.size());
        assertEquals("", r.anhChinh());
    }

    /* ---------- SỬA ---------- */

    @Test
    void suaBoHetAnh_luuThanhCong_anhCuBiNgungHoatDong() {
        SanPhamResponse r = service.them(req("Cuốn cán", ANH_CHUNG, List.of(bt(DEN)), null, null));
        assertEquals(1, soAnhDangDung());
        service.sua(r.id(), req("Cuốn cán", "", null, null, null));
        assertEquals(0, soAnhDangDung(), "ảnh cũ được tắt hoạt động");
    }

    @Test
    void suaDoiAnhChungSangAnhKhacVanDuoc() {
        SanPhamResponse r = service.them(req("Cuốn cán", ANH_CHUNG, null, null, null));
        SanPhamResponse moi = service.sua(r.id(), req("Cuốn cán", "/api/uploads/moi.jpg", null, null, null));
        assertEquals("/api/uploads/moi.jpg", moi.anhChinh());
    }

    @Test
    void suaBoAnhRiengCuaMotMau_luuThanhCong() {
        // sản phẩm không có ảnh chung, mỗi màu có ảnh riêng
        SanPhamResponse r = service.them(req("Cuốn cán", "", List.of(bt(DEN), bt(DO)),
                List.of(anhMau(DEN, "/api/uploads/den.jpg"), anhMau(DO, "/api/uploads/do.jpg")), null));
        assertEquals(2, soAnhDangDung());
        // bỏ ảnh màu Đen thì được lưu thành công
        service.sua(r.id(), req("Cuốn cán", "", null, List.of(anhMau(DEN)), null));
        assertEquals(1, soAnhDangDung(), "còn ảnh màu Đỏ");
        // thêm ảnh chung rồi bỏ ảnh màu Đen thì được
        service.sua(r.id(), req("Cuốn cán", ANH_CHUNG, null, List.of(anhMau(DEN)), null));
        assertEquals(2, soAnhDangDung(), "ảnh chung mới + ảnh màu Đỏ");
    }

    @Test
    void suaKhongGuiAnhTheoMau_giuAnhRiengCuaCacMau_nenKhongBaoLoi() {
        SanPhamResponse r = service.them(req("Cuốn cán", "", List.of(bt(DEN)), List.of(anhMau(DEN, "/api/uploads/den.jpg")), null));
        // gửi ảnh chung rỗng nhưng không gửi anhTheoMau (null = giữ nguyên): màu Đen vẫn còn ảnh riêng -> hợp lệ
        service.sua(r.id(), req("Cuốn cán đổi tên", "", null, null, null));
        assertEquals("Cuốn cán đổi tên", sanPhams.get(0).getTenSanPham());
    }

    /* ---------- CẬP NHẬT SẢN PHẨM ĐÃ TỒN TẠI ---------- */

    @Test
    void capNhatSanPhamDaTonTaiKhongGuiAnh_giuAnhCu_duoc() {
        service.them(req("Cuốn cán", ANH_CHUNG, List.of(bt(DEN)), null, null));
        ApiException hoi = loi(() -> service.them(req("Cuốn cán", "", List.of(bt(DO)), null, null)));
        assertEquals(HttpStatus.CONFLICT, hoi.getStatus(), "sản phẩm đã tồn tại thì hỏi xác nhận");
        service.them(req("Cuốn cán", "", List.of(bt(DO)), null, true));
        assertEquals(2, bienThes.size());
        assertEquals(1, soAnhDangDung(), "ảnh cũ được giữ");
    }

    @Test
    void capNhatSanPhamDaTonTaiThemMauMoiKhongCoAnhRieng_vanDuocNhoAnhChungCu() {
        service.them(req("Cuốn cán", ANH_CHUNG, List.of(bt(DEN)), null, null));
        service.them(req("Cuốn cán", "", List.of(bt(DO)), null, true));
        assertEquals(2, bienThes.size());
    }

    @Test
    void capNhatSanPhamCuKhongCoAnhNaoVaThemMauKhongAnh_luuThanhCong() {
        // sản phẩm cũ (dữ liệu trước khi có luật ảnh bắt buộc) không có ảnh nào
        SanPham cu = new SanPham();
        cu.setId(1);
        cu.setMaSanPham("SP01");
        cu.setTenSanPham("Cuốn cán");
        cu.setDanhMuc(tt(DanhMuc::new, PK, "Phụ kiện"));
        cu.setThuongHieu(tt(ThuongHieu::new, 1, "Yonex"));
        cu.setXuatXu(tt(XuatXu::new, 1, "Nhật Bản"));
        cu.setChatLieu(tt(ChatLieu::new, 1, "Cotton"));
        cu.setDoCung(tt(DoCung::new, KAD, "Không áp dụng"));
        cu.setDiemCanBang(tt(DiemCanBang::new, KAD, "Không áp dụng"));
        cu.setTrangThai(SanPham.HOAT_DONG);
        sanPhams.add(cu);
        service.them(req("Cuốn cán", "", List.of(bt(DEN)), null, true));
        assertEquals(1, bienThes.size());
    }

    /* ---------- hàm tính thuần ---------- */

    @Test
    void anhSanPham_mauChuaCoAnh_vaAnhTheoMauSauKhiLuu() {
        Map<Integer, List<String>> anh = new LinkedHashMap<>();
        anh.put(1, List.of("a.jpg"));
        anh.put(2, List.of());
        assertEquals(List.of(2, 3), AnhSanPham.mauChuaCoAnh(List.of(1, 2, 3), anh));
        assertTrue(AnhSanPham.coAnhTheoMau(anh));
        assertFalse(AnhSanPham.coAnhTheoMau(Map.of(1, List.<String>of())));

        SanPham sp = new SanPham();
        sp.setId(7);
        HinhAnhSanPham h = new HinhAnhSanPham();
        h.setSanPham(sp);
        h.setIdMauSac(1);
        h.setDuongDanHinhAnh("cu.jpg");
        h.setLaAnhChinh(true);
        h.setTrangThai(HinhAnhSanPham.HOAT_DONG);
        Map<Integer, List<String>> moi = new LinkedHashMap<>();
        moi.put(1, List.of());
        moi.put(2, List.of("moi.jpg"));
        assertEquals(Map.of(2, List.of("moi.jpg")), AnhSanPham.anhTheoMauSauKhiLuu(List.of(h), moi, true), "danh sách rỗng = bỏ hết ảnh màu 1");
        assertEquals(Map.of(1, List.of("cu.jpg"), 2, List.of("moi.jpg")), AnhSanPham.anhTheoMauSauKhiLuu(List.of(h), moi, false), "không cho bỏ: giữ ảnh cũ");
    }
}
