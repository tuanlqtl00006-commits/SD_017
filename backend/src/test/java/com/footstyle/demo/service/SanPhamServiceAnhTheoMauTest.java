package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.AnhTheoMauRequest;
import com.footstyle.demo.dto.SanPhamRequest;
import com.footstyle.demo.dto.SanPhamResponse;
import com.footstyle.demo.entity.ChatLieu;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.ThuocTinh;
import com.footstyle.demo.entity.ThuongHieu;
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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** SanPhamService thật với kho giả trong bộ nhớ: lưu / sửa ảnh theo màu (hinh_anh_san_pham.id_mau_sac). */
class SanPhamServiceAnhTheoMauTest {

    private static final Object NONE = new Object();
    private final List<SanPham> sanPhams = new ArrayList<>();
    private final List<HinhAnhSanPham> anhs = new ArrayList<>();
    private int idAnh = 0;
    private final SanPhamService service;

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> xuLy) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            Object kq = xuLy.apply(m.getName(), args == null ? new Object[0] : args);
            if (kq == NONE) throw new UnsupportedOperationException(m.getName());
            return kq;
        });
    }

    private static <T extends ThuocTinh> T tt(Supplier<T> tao, String ten) {
        T x = tao.get();
        x.setId(1);
        x.setTen(ten);
        x.setTrangThai(ThuocTinh.HOAT_DONG);
        return x;
    }

    /** Kho thuộc tính chỉ có 1 dòng id = 1. */
    private static <R> R khoThuocTinh(Class<R> type, Object dong) {
        return proxy(type, (ten, a) -> "findById".equals(ten) ? (Integer.valueOf(1).equals(a[0]) ? Optional.of(dong) : Optional.empty()) : NONE);
    }

    SanPhamServiceAnhTheoMauTest() {
        SanPhamRepository spRepo = proxy(SanPhamRepository.class, (ten, a) -> switch (ten) {
            case "findByDanhMucIdAndThuongHieuId" -> List.of();
            case "findAllMaSanPham" -> sanPhams.stream().map(SanPham::getMaSanPham).toList();
            case "existsByMaSanPhamIgnoreCase" -> sanPhams.stream().anyMatch(x -> x.getMaSanPham().equalsIgnoreCase((String) a[0]));
            case "findById" -> sanPhams.stream().filter(x -> x.getId().equals(a[0])).findFirst();
            case "save" -> {
                SanPham x = (SanPham) a[0];
                if (x.getId() == null) {
                    x.setId(sanPhams.size() + 1);
                    sanPhams.add(x);
                }
                yield x;
            }
            default -> NONE;
        });
        HinhAnhSanPhamRepository anhRepo = proxy(HinhAnhSanPhamRepository.class, (ten, a) -> switch (ten) {
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
            default -> NONE;
        });
        MauSacRepository mauRepo = proxy(MauSacRepository.class, (ten, a) -> "existsById".equals(ten) ? List.of(1, 2).contains(a[0]) : NONE);
        BienTheService bienThe = new BienTheService(proxy(SanPhamChiTietRepository.class, (t, a) -> NONE), spRepo, mauRepo,
                proxy(TrongLuongRepository.class, (t, a) -> NONE), proxy(ChuViRepository.class, (t, a) -> NONE), anhRepo);
        service = new SanPhamService(spRepo, anhRepo,
                khoThuocTinh(DanhMucRepository.class, tt(DanhMuc::new, "Vợt cầu lông")),
                khoThuocTinh(ThuongHieuRepository.class, tt(ThuongHieu::new, "Yonex")),
                khoThuocTinh(XuatXuRepository.class, tt(XuatXu::new, "Nhật Bản")),
                khoThuocTinh(ChatLieuRepository.class, tt(ChatLieu::new, "Carbon")),
                khoThuocTinh(DoCungRepository.class, tt(DoCung::new, "Cứng")),
                khoThuocTinh(DiemCanBangRepository.class, tt(DiemCanBang::new, "Nặng đầu")),
                mauRepo, bienThe);
    }

    private static SanPhamRequest req(String anhChinh, List<AnhTheoMauRequest> anhTheoMau) {
        return new SanPhamRequest("", "Astrox 99", 1, 1, 1, 1, 1, 1, "", anhChinh, List.of(), null, null, anhTheoMau);
    }

    @Test
    void themSanPham_luuAnhTheoTungMau_traVeAnhTheoMauVaAnhDaiDien() {
        SanPhamResponse r = service.them(req("", List.of(
                new AnhTheoMauRequest(1, List.of("/api/uploads/den-1.jpg", "/api/uploads/den-2.jpg")),
                new AnhTheoMauRequest(2, List.of("/api/uploads/do-1.jpg")))));
        assertEquals(3, anhs.size());
        assertTrue(anhs.stream().allMatch(h -> h.getIdMauSac() != null));
        assertEquals(2, r.anhTheoMau().size());
        assertEquals(List.of("/api/uploads/den-1.jpg", "/api/uploads/den-2.jpg"), r.anhTheoMau().get(0).anh());
        assertEquals("", r.anhChinh(), "không có ảnh chung");
        assertEquals("/api/uploads/den-1.jpg", r.anhDaiDien(), "ảnh đại diện lấy ảnh chính của màu đầu tiên");
    }

    @Test
    void suaKhongGuiAnhTheoMau_giuNguyen_guiDanhSachRong_thiBoAnhCuaMauDo() {
        SanPhamResponse r = service.them(req("/api/uploads/chung.jpg", List.of(new AnhTheoMauRequest(1, List.of("/api/uploads/den.jpg")))));
        SanPhamResponse giu = service.sua(r.id(), req("/api/uploads/chung.jpg", null));
        assertEquals(1, giu.anhTheoMau().size(), "null = giữ nguyên ảnh theo màu");
        assertEquals("/api/uploads/chung.jpg", giu.anhChinh());

        SanPhamResponse bo = service.sua(r.id(), req("/api/uploads/chung.jpg", List.of(new AnhTheoMauRequest(1, List.of()))));
        assertEquals(0, bo.anhTheoMau().size());
        assertEquals("/api/uploads/chung.jpg", bo.anhDaiDien());
        assertEquals(2, anhs.size(), "không xóa dòng, chỉ đổi trạng thái");
    }

    @Test
    void mauKhongTonTaiHoacQuaNhieuAnh_baoLoiRo() {
        ApiException e1 = assertThrows(ApiException.class, () -> service.them(req("", List.of(new AnhTheoMauRequest(99, List.of("/api/uploads/x.jpg"))))));
        assertTrue(e1.getMessage().contains("Màu sắc của ảnh không tồn tại"));
        List<String> bayAnh = List.of("/api/uploads/1.jpg", "/api/uploads/2.jpg", "/api/uploads/3.jpg", "/api/uploads/4.jpg",
                "/api/uploads/5.jpg", "/api/uploads/6.jpg", "/api/uploads/7.jpg");
        ApiException e2 = assertThrows(ApiException.class, () -> service.them(req("", List.of(new AnhTheoMauRequest(1, bayAnh)))));
        assertTrue(e2.getMessage().contains("tối đa 6 ảnh"));
        ApiException e3 = assertThrows(ApiException.class, () -> service.them(req("", List.of(new AnhTheoMauRequest(1, List.of("javascript:alert(1)"))))));
        assertTrue(e3.getMessage().contains("Ảnh phải được tải lên"));
        assertEquals(0, sanPhams.size(), "lỗi ảnh thì chưa lưu sản phẩm");
    }
}
