package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import com.footstyle.demo.service.LichGiamGiaService.DoanGiam;
import com.footstyle.demo.service.LichGiamGiaService.LichBienThe;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Lịch giảm giá theo ngày GIỜ khi nhiều đợt chồng nhau, và mức giảm "ngay lúc này" (đợt bắt đầu 18:00 thì trước 18:00 chưa giảm).
 * Ví dụ của nhóm: A giảm 10% 01/10 08:00 - 10/10 17:00; B giảm 15% 04/10 12:00 - 20/10 23:59; SPCT1 nằm trong cả hai.
 */
class LichGiamGiaServiceTest {

    private final List<DotGiamGia> dots = new ArrayList<>();
    private final List<DotGiamGiaChiTiet> chiTiets = new ArrayList<>();
    private final Map<Integer, SanPhamChiTiet> bienThes = new HashMap<>();
    private CheDo cheDo = CheDo.MAX;

    private static LocalDateTime t(int ngay, int gio, int phut, int giay) {
        return LocalDateTime.of(2026, 10, ngay, gio, phut, giay);
    }

    private DotGiamGia dot(long id, int phanTram, LocalDateTime tu, LocalDateTime den, int trangThai) {
        DotGiamGia d = new DotGiamGia();
        d.setId(id);
        d.setMaDot("DGG0" + id);
        d.setTenDot("Đợt " + id);
        d.setPhanTramGiamDot(phanTram);
        d.setNgayBatDau(tu);
        d.setNgayKetThuc(den);
        d.setTrangThai(trangThai);
        dots.add(d);
        return d;
    }

    private void chiTiet(DotGiamGia d, long idBienThe, int phanTram) {
        DotGiamGiaChiTiet c = new DotGiamGiaChiTiet();
        c.setId((long) chiTiets.size() + 1);
        c.setDotGiamGia(d);
        c.setIdSanPhamChiTiet(idBienThe);
        c.setPhanTramGiamBienThe(phanTram);
        c.setTrangThai(1);
        chiTiets.add(c);
    }

    private SanPhamChiTiet bienThe(int id, long giaBan) {
        SanPhamChiTiet b = new SanPhamChiTiet();
        b.setId(id);
        b.setMaSpct("SPCT" + id);
        b.setGiaBan(giaBan);
        bienThes.put(id, b);
        return b;
    }

    private LichGiamGiaService service() {
        DotGiamGiaRepository dotRepo = KhoGia.tao(DotGiamGiaRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> dots.stream().filter(d -> d.getId().equals(a[0])).findFirst();
            case "findAll" -> new ArrayList<>(dots);
            default -> KhoGia.KHONG_CO;
        });
        DotGiamGiaChiTietRepository ctRepo = KhoGia.tao(DotGiamGiaChiTietRepository.class, (ten, a) -> switch (ten) {
            case "findByDotGiamGiaId" -> chiTiets.stream().filter(c -> c.getDotGiamGia().getId().equals(a[0])).toList();
            case "findAll" -> new ArrayList<>(chiTiets);
            default -> KhoGia.KHONG_CO;
        });
        SanPhamChiTietRepository btRepo = KhoGia.tao(SanPhamChiTietRepository.class, (ten, a) ->
                "findById".equals(ten) ? Optional.ofNullable(bienThes.get(a[0])) : KhoGia.KHONG_CO);
        GiamGiaApDungService apDung = new GiamGiaApDungService(ctRepo) {
            @Override
            public CheDo cheDo() {
                return cheDo;
            }
        };
        return new LichGiamGiaService(dotRepo, ctRepo, btRepo, apDung);
    }

    private DotGiamGia dotA;
    private DotGiamGia dotB;

    private void dungKichBanCuaNhom() {
        bienThe(1, 1_000_000);
        bienThe(3, 500_000);
        bienThe(5, 200_000);
        dotA = dot(1, 10, t(1, 8, 0, 0), t(10, 17, 0, 59), 1);
        dotB = dot(2, 15, t(4, 12, 0, 0), t(20, 23, 59, 59), 1);
        chiTiet(dotA, 1, 10);
        chiTiet(dotA, 5, 10);
        chiTiet(dotB, 1, 15);
        chiTiet(dotB, 3, 15);
    }

    private LichBienThe cua(List<LichBienThe> ds, long idBienThe) {
        return ds.stream().filter(l -> l.idBienThe() == idBienThe).findFirst().orElseThrow();
    }

    @Test
    void lichCuaDotA_bienTheChongVoiDotB_chiaDoanDungGio() {
        dungKichBanCuaNhom();
        List<LichBienThe> lich = service().lichCuaDot(1L);
        assertEquals(2, lich.size(), "đợt A có SPCT1 và SPCT5");

        List<DoanGiam> d1 = cua(lich, 1).doan();
        assertEquals(2, d1.size());
        assertEquals(t(1, 8, 0, 0), d1.get(0).tuLuc());
        assertEquals(t(4, 11, 59, 59), d1.get(0).denLuc());
        assertEquals(10, d1.get(0).phanTramApDung());
        assertEquals(1, d1.get(0).cacDot().size());

        assertEquals(t(4, 12, 0, 0), d1.get(1).tuLuc());
        assertEquals(t(10, 17, 0, 59), d1.get(1).denLuc(), "cắt đúng ở hết đợt A");
        assertEquals(2, d1.get(1).cacDot().size());
        assertEquals(15, d1.get(1).phanTramCaoNhat());
        assertEquals(25, d1.get(1).phanTramCongDon());
        assertEquals(12.5, d1.get(1).phanTramTrungBinh());
        assertEquals(15, d1.get(1).phanTramApDung(), "MAX");
        assertEquals(850_000L, d1.get(1).giaSauGiam());

        List<DoanGiam> d5 = cua(lich, 5).doan();
        assertEquals(1, d5.size());
        assertEquals(10, d5.get(0).phanTramApDung());
        assertEquals(180_000L, d5.get(0).giaSauGiam());
    }

    @Test
    void cheDoCongDonVaTrungBinh_doiMucApDungOdoanChongNhau() {
        dungKichBanCuaNhom();
        cheDo = CheDo.CONG_DON;
        DoanGiam congDon = cua(service().lichCuaDot(1L), 1).doan().get(1);
        assertEquals(25, congDon.phanTramApDung());
        assertEquals(750_000L, congDon.giaSauGiam());

        cheDo = CheDo.TRUNG_BINH;
        DoanGiam tb = cua(service().lichCuaDot(1L), 1).doan().get(1);
        assertEquals(12.5, tb.phanTramApDung());
        assertEquals(875_000L, tb.giaSauGiam());
    }

    @Test
    void lichCuaDotB_catTrongKhoangCuaB() {
        dungKichBanCuaNhom();
        List<LichBienThe> lich = service().lichCuaDot(2L);
        List<DoanGiam> d1 = cua(lich, 1).doan();
        assertEquals(2, d1.size());
        assertEquals(t(4, 12, 0, 0), d1.get(0).tuLuc());
        assertEquals(t(10, 17, 0, 59), d1.get(0).denLuc());
        assertEquals(2, d1.get(0).cacDot().size());
        assertEquals(t(10, 17, 1, 0), d1.get(1).tuLuc());
        assertEquals(t(20, 23, 59, 59), d1.get(1).denLuc());
        assertEquals(15, d1.get(1).phanTramApDung());

        List<DoanGiam> d3 = cua(lich, 3).doan();
        assertEquals(1, d3.size());
        assertEquals(t(4, 12, 0, 0), d3.get(0).tuLuc());
        assertEquals(t(20, 23, 59, 59), d3.get(0).denLuc());
    }

    @Test
    void dotDangTat_khongTinhVaoLichCuaDotKhac() {
        dungKichBanCuaNhom();
        dotB.setTrangThai(0);
        List<DoanGiam> d1 = cua(service().lichCuaDot(1L), 1).doan();
        assertEquals(1, d1.size());
        assertEquals(10, d1.get(0).phanTramApDung());
        assertEquals(t(1, 8, 0, 0), d1.get(0).tuLuc());
        assertEquals(t(10, 17, 0, 59), d1.get(0).denLuc());
    }

    @Test
    void giamHomNay_chiTinhDotDangChayToiTungGio() {
        LocalDateTime bayGio = LocalDateTime.now();
        bienThe(1, 1_000_000);
        bienThe(2, 1_000_000);
        bienThe(3, 1_000_000);
        bienThe(4, 1_000_000);
        DotGiamGia dangChay = dot(1, 10, bayGio.minusHours(1), bayGio.plusHours(2), 1);
        DotGiamGia sapChay = dot(2, 15, bayGio.plusMinutes(30), bayGio.plusHours(3), 1);
        DotGiamGia vuaHet = dot(3, 20, bayGio.minusHours(3), bayGio.minusMinutes(1), 1);
        DotGiamGia daTat = dot(4, 50, bayGio.minusHours(1), bayGio.plusHours(1), 0);
        chiTiet(dangChay, 1, 10);   // biến thể 1: đang giảm 10%, đợt 15% sắp bắt đầu sau 30 phút
        chiTiet(sapChay, 1, 15);
        chiTiet(sapChay, 2, 15);    // biến thể 2: chỉ có đợt sắp chạy -> chưa giảm
        chiTiet(vuaHet, 3, 20);     // biến thể 3: đợt vừa hết 1 phút trước -> không giảm
        chiTiet(daTat, 4, 50);      // biến thể 4: đợt đang tắt -> không giảm

        Map<Long, Double> homNay = service().giamHomNay();
        assertEquals(Map.of(1L, 10.0), homNay);
        assertNull(homNay.get(2L), "đợt bắt đầu sau 30 phút thì bây giờ chưa giảm (trước đây tính theo ngày nên giảm luôn từ 0h)");
        assertTrue(!homNay.containsKey(3L) && !homNay.containsKey(4L));
    }

    @Test
    void giamHomNay_haiDotDangChayCungLuc_theoCheDo() {
        LocalDateTime bayGio = LocalDateTime.now();
        bienThe(1, 1_000_000);
        DotGiamGia a = dot(1, 10, bayGio.minusHours(1), bayGio.plusHours(2), 1);
        DotGiamGia b = dot(2, 15, bayGio.minusMinutes(10), bayGio.plusHours(3), 1);
        chiTiet(a, 1, 10);
        chiTiet(b, 1, 15);
        assertEquals(15.0, service().giamHomNay().get(1L));
        cheDo = CheDo.CONG_DON;
        assertEquals(25.0, service().giamHomNay().get(1L));
        cheDo = CheDo.TRUNG_BINH;
        assertEquals(12.5, service().giamHomNay().get(1L));
    }

    @Test
    void giaSauGiamTrenLich_khopGiaSauGiamOFrontend() {
        // 3 đợt chồng nhau 39% + 40% + 40%, trung bình 39,67%: 5.000 x 60,33% = 3.016,5 -> 3.017 (giống tinhGiaSauGiam ở frontend;
        // công thức cũ gia * (1 - pt / 100) bị sai số dấu phẩy động nên ra 3.016)
        bienThe(7, 5_000);
        DotGiamGia d1 = dot(11, 39, t(1, 0, 0, 0), t(5, 23, 59, 59), 1);
        DotGiamGia d2 = dot(12, 40, t(1, 0, 0, 0), t(5, 23, 59, 59), 1);
        DotGiamGia d3 = dot(13, 40, t(1, 0, 0, 0), t(5, 23, 59, 59), 1);
        chiTiet(d1, 7, 39);
        chiTiet(d2, 7, 40);
        chiTiet(d3, 7, 40);
        cheDo = CheDo.TRUNG_BINH;
        DoanGiam d = cua(service().lichCuaDot(11L), 7).doan().get(0);
        assertEquals(39.67, d.phanTramApDung());
        assertEquals(3_017L, d.giaSauGiam());

        // mọi đoạn, mọi cách tính: giá trên lịch = công thức chung giaSauGiam
        dungKichBanCuaNhom();
        for (CheDo c : CheDo.values()) {
            cheDo = c;
            for (long idDot : new long[] {1L, 2L, 11L}) {
                for (LichBienThe b : service().lichCuaDot(idDot)) {
                    for (DoanGiam x : b.doan()) {
                        assertEquals(LichGiamGiaService.giaSauGiam(b.giaBan(), x.phanTramApDung()), x.giaSauGiam(), c + " " + b.maSpct() + " " + x);
                    }
                }
            }
        }
    }
}
