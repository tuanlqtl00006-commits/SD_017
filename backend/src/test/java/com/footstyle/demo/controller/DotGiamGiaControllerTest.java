package com.footstyle.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.dto.DotGiamGiaRequest;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.service.ThoiGianPhut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Thêm / sửa đợt giảm giá với kho giả: ngày GIỜ (tính theo phút), bắt đầu / kết thúc ở quá khứ, đợt đã bắt đầu,
 * mã, trạng thái, biến thể ngừng bán, danh sách trùng.
 */
class DotGiamGiaControllerTest {

    private static final DateTimeFormatter GUI = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final LocalDateTime BAY_GIO = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    private final List<DotGiamGia> dots = new ArrayList<>();
    private final List<DotGiamGiaChiTiet> chiTiets = new ArrayList<>();
    private final List<SanPhamChiTiet> bienThes = new ArrayList<>();
    private final DotGiamGiaController controller;

    DotGiamGiaControllerTest() {
        bienThes.add(bienThe(1, SanPhamChiTiet.HOAT_DONG));
        bienThes.add(bienThe(2, SanPhamChiTiet.HOAT_DONG));
        bienThes.add(bienThe(3, SanPhamChiTiet.NGUNG_HOAT_DONG));

        DotGiamGiaRepository dotRepo = KhoGia.tao(DotGiamGiaRepository.class, (ten, a) -> switch (ten) {
            case "findAll" -> new ArrayList<>(dots);
            case "existsByMaDot" -> dots.stream().anyMatch(d -> d.getMaDot().equalsIgnoreCase((String) a[0]));
            case "findById" -> dots.stream().filter(d -> d.getId().equals(a[0])).findFirst();
            case "save" -> {
                DotGiamGia d = (DotGiamGia) a[0];
                if (d.getId() == null) {
                    d.setId((long) dots.size() + 1);
                    dots.add(d);
                }
                yield d;
            }
            default -> KhoGia.KHONG_CO;
        });
        DotGiamGiaChiTietRepository ctRepo = KhoGia.tao(DotGiamGiaChiTietRepository.class, (ten, a) -> switch (ten) {
            case "findByDotGiamGiaId" -> chiTiets.stream().filter(c -> c.getDotGiamGia().getId().equals(a[0])).toList();
            case "save" -> {
                DotGiamGiaChiTiet c = (DotGiamGiaChiTiet) a[0];
                if (c.getId() == null) {
                    c.setId((long) chiTiets.size() + 1);
                    chiTiets.add(c);
                }
                yield c;
            }
            case "deleteAll" -> {
                for (Object o : (Iterable<?>) a[0]) chiTiets.remove(o);
                yield null;
            }
            default -> KhoGia.KHONG_CO;
        });
        SanPhamChiTietRepository btRepo = KhoGia.tao(SanPhamChiTietRepository.class, (ten, a) ->
                "findById".equals(ten) ? bienThes.stream().filter(b -> b.getId().equals(a[0])).findFirst() : KhoGia.KHONG_CO);
        controller = new DotGiamGiaController(dotRepo, ctRepo, btRepo, null, null);
    }

    private static SanPhamChiTiet bienThe(int id, int trangThai) {
        SanPhamChiTiet b = new SanPhamChiTiet();
        b.setId(id);
        b.setMaSpct("SP01-" + id);
        b.setTrangThai(trangThai);
        return b;
    }

    /** Request theo NGÀY GIỜ (yyyy-MM-ddTHH:mm) như giao diện gửi lên. */
    private static DotGiamGiaRequest req(String ma, LocalDateTime tu, LocalDateTime den, Integer trangThai, List<Long> ids) {
        return new DotGiamGiaRequest(ma, "Sale cuối tuần", 10, tu.format(GUI), den.format(GUI), null, trangThai, ids);
    }

    private static String loi(Runnable r) {
        return assertThrows(ApiException.class, r::run).getMessage();
    }

    private DotGiamGia dotCoSan(LocalDateTime batDau, LocalDateTime ketThuc) {
        DotGiamGia cu = new DotGiamGia();
        cu.setId((long) dots.size() + 1);
        cu.setMaDot("DGG0" + cu.getId());
        cu.setTenDot("Đợt có sẵn");
        cu.setPhanTramGiamDot(10);
        cu.setNgayBatDau(batDau);
        cu.setNgayKetThuc(ketThuc);
        cu.setTrangThai(1);
        dots.add(cu);
        return cu;
    }

    // ---------- ngày giờ ----------

    @Test
    void taoDotBatDauHoacKetThucOQuaKhu_baoLoi_batDauBayGioVanDuoc() {
        LocalDateTime bayGio = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        assertTrue(loi(() -> controller.create(req("", bayGio.minusDays(5), bayGio.plusDays(1), null, List.of(1L)))).contains("bắt đầu không được ở quá khứ"));
        assertTrue(loi(() -> controller.create(req("", bayGio.minusMinutes(4), bayGio.minusMinutes(2), null, List.of(1L)))).contains("kết thúc không được ở quá khứ"));
        assertTrue(dots.isEmpty());
        DotGiamGia d = controller.create(req("", bayGio.minusMinutes(2), bayGio.plusDays(1), null, List.of(1L)));
        assertEquals("DGG01", d.getMaDot());
        assertEquals(1, d.getTrangThai());
    }

    @Test
    void ngayGioLuuTheoPhut_batDauDauPhut_ketThucHetPhut() {
        LocalDateTime bd = BAY_GIO.plusDays(1).withHour(8).withMinute(30);
        LocalDateTime kt = BAY_GIO.plusDays(3).withHour(17).withMinute(0);
        DotGiamGia d = controller.create(new DotGiamGiaRequest("", "Sale", 10, bd.format(GUI) + ":45", kt.format(GUI) + ":10", null, null, List.of(1L)));
        assertEquals(bd, d.getNgayBatDau());
        assertEquals(kt.withSecond(59), d.getNgayKetThuc());
    }

    @Test
    void chiCoNgay_batDauLaDauNgay_ketThucLaCuoiNgay() {
        LocalDate ngayMai = LocalDate.now().plusDays(1);
        DotGiamGia d = controller.create(new DotGiamGiaRequest("", "Sale", 10, ngayMai.toString(), ngayMai.plusDays(2).toString(), null, null, List.of(1L)));
        assertEquals(ngayMai.atStartOfDay(), d.getNgayBatDau());
        assertEquals(ngayMai.plusDays(2).atTime(23, 59, 59), d.getNgayKetThuc());
    }

    @Test
    void ketThucKhongSauBatDau_baoLoi() {
        LocalDateTime bd = BAY_GIO.plusDays(1).withHour(9).withMinute(0);
        for (LocalDateTime kt : List.of(bd.minusMinutes(1), bd)) {
            assertTrue(loi(() -> controller.create(req("", bd, kt, null, List.of(1L)))).contains("kết thúc phải sau ngày giờ bắt đầu"), kt.toString());
        }
        assertTrue(dots.isEmpty());
        controller.create(req("", bd, bd.plusMinutes(1), null, List.of(1L)));
        assertEquals(1, dots.size());
    }

    @Test
    void suaDotDangChay_khongDuocDoiGioBatDau_nhungGiaHanVaDoiTenDuoc() {
        DotGiamGia dang = dotCoSan(BAY_GIO.minusHours(3), BAY_GIO.plusDays(2));
        for (LocalDateTime bdMoi : List.of(BAY_GIO.minusHours(2), BAY_GIO.minusHours(5), BAY_GIO.plusHours(1))) {
            assertTrue(loi(() -> controller.update(dang.getId(), req(null, bdMoi, BAY_GIO.plusDays(2), null, null))).contains("đã bắt đầu nên không đổi được"), bdMoi.toString());
        }
        assertEquals(BAY_GIO.minusHours(3), dang.getNgayBatDau());

        DotGiamGia giaHan = controller.update(dang.getId(), new DotGiamGiaRequest(null, "Đợt đang chạy (đổi tên)", 10,
                BAY_GIO.minusHours(3).format(GUI), BAY_GIO.plusDays(5).format(GUI), null, null, null));
        assertEquals("Đợt đang chạy (đổi tên)", giaHan.getTenDot());
        assertEquals(BAY_GIO.plusDays(5).withSecond(59), giaHan.getNgayKetThuc());

        assertTrue(loi(() -> controller.update(dang.getId(), req(null, BAY_GIO.minusHours(3), BAY_GIO.minusMinutes(20), null, null))).contains("kết thúc mới không được ở quá khứ"));
    }

    @Test
    void suaDotSapDienRa_doiGioBatDauChiDuocSangTuongLai() {
        DotGiamGia sap = dotCoSan(BAY_GIO.plusDays(2), BAY_GIO.plusDays(8));
        assertTrue(loi(() -> controller.update(sap.getId(), req(null, BAY_GIO.minusHours(1), BAY_GIO.plusDays(8), null, null))).contains("bắt đầu mới không được ở quá khứ"));
        DotGiamGia ok = controller.update(sap.getId(), req(null, BAY_GIO.plusDays(3), BAY_GIO.plusDays(8), null, null));
        assertEquals(BAY_GIO.plusDays(3), ok.getNgayBatDau());
    }

    @Test
    void suaDotDaKetThuc_giuGioCuDuoc_doiSangGioQuaKhuKhacThiKhong_giaHanDuoc() {
        DotGiamGia cu = dotCoSan(BAY_GIO.minusDays(20), BAY_GIO.minusDays(10).withHour(23).withMinute(59).withSecond(59));

        DotGiamGia giu = controller.update(cu.getId(), new DotGiamGiaRequest(null, "Đợt cũ (sửa tên)", 10,
                BAY_GIO.minusDays(20).format(GUI), BAY_GIO.minusDays(10).withHour(23).withMinute(59).format(GUI), null, null, null));
        assertEquals("Đợt cũ (sửa tên)", giu.getTenDot());

        assertTrue(loi(() -> controller.update(cu.getId(), new DotGiamGiaRequest(null, "Đợt cũ", 10,
                BAY_GIO.minusDays(20).format(GUI), BAY_GIO.minusDays(3).format(GUI), null, null, null))).contains("quá khứ"));

        DotGiamGia giaHan = controller.update(cu.getId(), new DotGiamGiaRequest(null, "Đợt cũ", 10,
                BAY_GIO.minusDays(20).format(GUI), BAY_GIO.plusDays(3).format(GUI), null, null, null));
        assertEquals(BAY_GIO.plusDays(3).withSecond(59), giaHan.getNgayKetThuc());
    }

    @Test
    void dotCuLuuTheoNgay_guiLaiNguyenGio_khongBiTinhLaDoiGio() {
        // Dữ liệu cũ 00:00:00 -> 23:59:59; giao diện gửi lại 00:00 / 23:59 thì không tính là đổi giờ
        DotGiamGia cu = dotCoSan(BAY_GIO.minusDays(2).withHour(0).withMinute(0).withSecond(0), BAY_GIO.plusDays(2).withHour(23).withMinute(59).withSecond(59));
        DotGiamGia kq = controller.update(cu.getId(), new DotGiamGiaRequest(null, "Đợt cũ", 10,
                cu.getNgayBatDau().format(GUI), cu.getNgayKetThuc().format(GUI), null, null, null));
        assertEquals(cu.getNgayBatDau(), kq.getNgayBatDau());
        assertEquals(cu.getNgayKetThuc(), kq.getNgayKetThuc());
    }

    @Test
    void duSaiPhutLaHangSoChungVoiPhieuGiamGia() {
        assertEquals(5, ThoiGianPhut.DUNG_SAI_PHUT);
    }

    // ---------- mã, trạng thái, biến thể ----------

    @Test
    void maDotCoDauHoacKhoangTrang_vaTrangThaiLa_baoLoi() {
        LocalDateTime tu = BAY_GIO.plusHours(1);
        assertTrue(loi(() -> controller.create(req("DGG 01", tu, tu.plusDays(3), null, List.of(1L)))).contains("Mã đợt"));
        assertTrue(loi(() -> controller.create(req("ĐỢT1", tu, tu.plusDays(3), null, List.of(1L)))).contains("Mã đợt"));
        assertTrue(loi(() -> controller.create(req("", tu, tu.plusDays(3), 5, List.of(1L)))).contains("Trạng thái"));
        assertEquals("SALE-10_10", controller.create(req("sale-10_10", tu, tu.plusDays(3), 0, List.of(1L))).getMaDot());
    }

    @Test
    void bienTheNgungBanHoacKhongTonTai_baoLoi_danhSachTrungChiLuuMotLan() {
        LocalDateTime tu = BAY_GIO.plusHours(1);
        assertTrue(loi(() -> controller.create(req("", tu, tu.plusDays(3), null, List.of(1L, 3L)))).contains("ngừng bán"));
        assertTrue(loi(() -> controller.create(req("DGG50", tu, tu.plusDays(3), null, List.of(99L)))).contains("không còn tồn tại"));
        chiTiets.clear();
        controller.create(req("DGG60", tu, tu.plusDays(3), null, List.of(2L, 2L, 1L)));
        assertEquals(2, chiTiets.size());
    }

    @Test
    void suaGuiDanhSachRong_baoLoi_doiPhanTramThiApLaiChoMoiBienThe() {
        LocalDateTime tu = BAY_GIO.plusHours(1);
        DotGiamGia d = controller.create(req("", tu, tu.plusDays(3), null, List.of(1L, 2L)));
        chiTiets.get(0).setPhanTramGiamBienThe(25); // mức riêng của biến thể 1
        assertTrue(loi(() -> controller.update(d.getId(), req(null, tu, tu.plusDays(3), null, List.of()))).contains("ít nhất một"));

        controller.update(d.getId(), req(null, tu, tu.plusDays(3), null, null)); // giữ 10%: giữ mức riêng
        assertEquals(25, chiTiets.get(0).getPhanTramGiamBienThe());

        controller.update(d.getId(), new DotGiamGiaRequest(null, "Sale", 30, tu.format(GUI), tu.plusDays(3).format(GUI), null, null, null));
        assertTrue(chiTiets.stream().allMatch(c -> c.getPhanTramGiamBienThe() == 30));
    }

    @Test
    void ngaySaiDinhDangThieuHoacPhanTramVaTenSai_baoLoi() {
        LocalDateTime tu = BAY_GIO.plusHours(1);
        assertTrue(loi(() -> controller.create(new DotGiamGiaRequest("", "Sale", 10, "13/10/2026", "2026-10-20", null, null, List.of(1L)))).contains("Ngày"));
        assertTrue(loi(() -> controller.create(new DotGiamGiaRequest("", "Sale", 10, null, "2026-10-20", null, null, List.of(1L)))).contains("chọn ngày"));
        assertTrue(loi(() -> controller.create(new DotGiamGiaRequest("", "Sale", 0, tu.format(GUI), tu.plusDays(1).format(GUI), null, null, List.of(1L)))).contains("1 đến 100"));
        assertTrue(loi(() -> controller.create(new DotGiamGiaRequest("", "Sale", 101, tu.format(GUI), tu.plusDays(1).format(GUI), null, null, List.of(1L)))).contains("1 đến 100"));
        assertTrue(loi(() -> controller.create(new DotGiamGiaRequest("", "   ", 10, tu.format(GUI), tu.plusDays(1).format(GUI), null, null, List.of(1L)))).contains("tên"));
        assertTrue(loi(() -> controller.create(req("", tu, tu.plusDays(1), null, null))).contains("ít nhất một biến thể"));
        assertTrue(dots.isEmpty());
    }
}
