package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Các đợt giảm giá đang diễn ra cho trang Ưu đãi của khách (GiamGiaApDungService.dotDangChay). */
class DotGiamGiaDangChayTest {

    private static final LocalDateTime LUC = LocalDateTime.of(2026, 11, 14, 10, 0);

    private final List<DotGiamGiaChiTiet> chiTiets = new ArrayList<>();
    private final GiamGiaApDungService service = new GiamGiaApDungService(
            KhoGia.tao(DotGiamGiaChiTietRepository.class, (t, a) -> "findAll".equals(t) && a.length == 0 ? chiTiets : KhoGia.KHONG_CO));

    private DotGiamGia dot(long id, String ten, LocalDateTime tu, LocalDateTime den, int trangThai) {
        DotGiamGia d = new DotGiamGia();
        d.setId(id);
        d.setMaDot("DGG" + id);
        d.setTenDot(ten);
        d.setPhanTramGiamDot(10);
        d.setNgayBatDau(tu);
        d.setNgayKetThuc(den);
        d.setTrangThai(trangThai);
        return d;
    }

    private void dong(DotGiamGia d, long idBienThe, Integer phanTram, int trangThai) {
        DotGiamGiaChiTiet ct = new DotGiamGiaChiTiet();
        ct.setDotGiamGia(d);
        ct.setIdSanPhamChiTiet(idBienThe);
        ct.setPhanTramGiamBienThe(phanTram);
        ct.setTrangThai(trangThai);
        chiTiets.add(ct);
    }

    @Test
    void chiLayDotDangBat_trongKhoangNgay_conDongGiamLonHon0() {
        DotGiamGia dangChay = dot(1, "Sale giữa tháng", LUC.minusDays(2), LUC.plusDays(5), 1);
        dong(dangChay, 11, 12, 1);
        dong(dangChay, 12, 8, 1);
        dong(dangChay, 13, 20, 0); // dòng đã tắt: không tính
        dong(dangChay, 14, 0, 1); // dòng 0%: không tính
        DotGiamGia daTat = dot(2, "Đợt đã tắt", LUC.minusDays(2), LUC.plusDays(5), 0);
        dong(daTat, 21, 30, 1);
        DotGiamGia chuaToi = dot(3, "Black Friday sớm", LUC.plusDays(3), LUC.plusDays(9), 1);
        dong(chuaToi, 31, 25, 1);
        DotGiamGia daHet = dot(4, "Hè xanh", LUC.minusDays(30), LUC.minusDays(1), 1);
        dong(daHet, 41, 15, 1);
        DotGiamGia toanDongTat = dot(5, "Đợt không còn dòng nào", LUC.minusDays(1), LUC.plusDays(1), 1);
        dong(toanDongTat, 51, 10, 0);

        List<GiamGiaApDungService.DotDangChay> kq = service.dotDangChay(LUC);
        assertEquals(1, kq.size(), "chỉ đợt đang bật, đang trong khoảng ngày và còn dòng đang bật > 0%");
        GiamGiaApDungService.DotDangChay d = kq.get(0);
        assertEquals(1L, d.id());
        assertEquals("Sale giữa tháng", d.ten());
        assertEquals(Map.of(11L, 12, 12L, 8), d.phanTramTheoBienThe());
    }

    @Test
    void dungLucBatDauVaKetThucVanTinh_dotSapHetXepTruoc_motBienTheNhieuDongLayMucCaoNhat() {
        DotGiamGia hetSau = dot(1, "Kết thúc sau", LUC.minusDays(1), LUC.plusDays(9), 1);
        dong(hetSau, 11, 5, 1);
        dong(hetSau, 11, 9, 1);
        DotGiamGia hetDungLuc = dot(2, "Kết thúc đúng lúc hỏi", LUC.minusDays(1), LUC, 1);
        dong(hetDungLuc, 21, 10, 1);
        DotGiamGia batDauDungLuc = dot(3, "Bắt đầu đúng lúc hỏi", LUC, LUC.plusDays(2), 1);
        dong(batDauDungLuc, 31, 10, 1);

        List<GiamGiaApDungService.DotDangChay> kq = service.dotDangChay(LUC);
        assertEquals(List.of(2L, 3L, 1L), kq.stream().map(GiamGiaApDungService.DotDangChay::id).toList(), "đợt sắp kết thúc xếp trước");
        assertEquals(9, kq.get(2).phanTramTheoBienThe().get(11L), "một biến thể có nhiều dòng trong cùng đợt thì lấy mức cao nhất");
        assertTrue(service.dotDangChay(LUC.plusSeconds(1)).stream().noneMatch(d -> d.id() == 2L), "qua giờ kết thúc thì không còn");
    }
}
