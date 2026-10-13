package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.AnhTheoMauResponse;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.SanPham;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ảnh sản phẩm theo màu (hinh_anh_san_pham.id_mau_sac): đọc, chọn ảnh cho biến thể, đồng bộ từng màu. */
class AnhSanPhamTest {

    private static final Integer DEN = 1;
    private static final Integer DO = 2;
    private int id = 0;
    private final SanPham sp = new SanPham();

    private HinhAnhSanPham anh(Integer idMau, String url, boolean chinh, boolean dangDung) {
        HinhAnhSanPham h = new HinhAnhSanPham();
        h.setId(++id);
        h.setSanPham(sp);
        h.setIdMauSac(idMau);
        h.setDuongDanHinhAnh(url);
        h.setLaAnhChinh(chinh);
        h.setTrangThai(dangDung ? HinhAnhSanPham.HOAT_DONG : HinhAnhSanPham.NGUNG_HOAT_DONG);
        return h;
    }

    @Test
    void duLieuCu_chiCoAnhChung_vanDocNhuTruoc() {
        List<HinhAnhSanPham> ds = List.of(anh(null, "/api/uploads/phu.jpg", false, true), anh(null, "/api/uploads/chinh.jpg", true, true));
        List<HinhAnhSanPham> chung = AnhSanPham.anhChung(ds);
        assertEquals("/api/uploads/chinh.jpg", chung.get(0).getDuongDanHinhAnh(), "ảnh chính đứng đầu");
        assertTrue(AnhSanPham.anhTheoMau(ds).isEmpty());
        assertEquals("/api/uploads/chinh.jpg", AnhSanPham.anhDaiDien(ds));
        assertEquals("/api/uploads/chinh.jpg", AnhSanPham.anhCuaBienThe(ds, DEN), "màu chưa có ảnh thì lấy ảnh chung");
    }

    @Test
    void anhTheoMau_gomTheoMau_anhChinhCuaMauDungDau_boAnhDaBo() {
        List<HinhAnhSanPham> ds = List.of(
                anh(DO, "do-phu.jpg", false, true), anh(DO, "do-chinh.jpg", true, true),
                anh(DEN, "den-1.jpg", true, true), anh(DEN, "den-cu.jpg", false, false),
                anh(null, "chung.jpg", true, true));
        Map<Integer, List<String>> m = AnhSanPham.anhTheoMau(ds);
        assertEquals(List.of(DEN, DO), new ArrayList<>(m.keySet()));
        assertEquals(List.of("den-1.jpg"), m.get(DEN));
        assertEquals(List.of("do-chinh.jpg", "do-phu.jpg"), m.get(DO));
        List<AnhTheoMauResponse> r = AnhSanPham.anhTheoMauResponse(ds);
        assertEquals(2, r.size());
        assertEquals(DO, r.get(1).idMauSac());
        // ảnh chung không lẫn ảnh màu
        assertEquals(1, AnhSanPham.anhChung(ds).size());
    }

    @Test
    void anhCuaBienThe_uuTienAnhMau_khongCoThiAnhChung_khongCoGiThiRong() {
        List<HinhAnhSanPham> ds = List.of(anh(DEN, "den.jpg", true, true), anh(null, "chung.jpg", true, true));
        assertEquals("den.jpg", AnhSanPham.anhCuaBienThe(ds, DEN));
        assertEquals("chung.jpg", AnhSanPham.anhCuaBienThe(ds, DO));
        assertEquals("", AnhSanPham.anhCuaBienThe(List.of(), DEN));
        // sản phẩm chỉ có ảnh màu: ảnh đại diện lấy ảnh màu đầu tiên
        assertEquals("den.jpg", AnhSanPham.anhDaiDien(List.of(anh(DEN, "den.jpg", true, true))));
    }

    @Test
    void dongBoMotMau_khongDungToiAnhChungVaMauKhac() {
        HinhAnhSanPham chung = anh(null, "chung.jpg", true, true);
        HinhAnhSanPham doCu = anh(DO, "do.jpg", true, true);
        HinhAnhSanPham denCu = anh(DEN, "den-cu.jpg", true, true);
        List<HinhAnhSanPham> hienCo = List.of(chung, doCu, denCu);

        List<HinhAnhSanPham> luu = AnhSanPham.dongBo(sp, DEN, hienCo, List.of("den-moi.jpg", "den-cu.jpg"));
        assertEquals(2, luu.size(), "1 dòng cũ của màu Đen + 1 dòng mới");
        assertTrue(luu.contains(denCu));
        assertFalse(denCu.getLaAnhChinh(), "ảnh cũ thành ảnh phụ vì ảnh đầu danh sách mới là ảnh chính");
        assertEquals(HinhAnhSanPham.HOAT_DONG, denCu.getTrangThai());
        HinhAnhSanPham moi = luu.stream().filter(h -> h.getId() == null).findFirst().orElseThrow();
        assertEquals(DEN, moi.getIdMauSac());
        assertTrue(moi.getLaAnhChinh());
        assertEquals(sp, moi.getSanPham());
        // dòng của nhóm khác không bị đổi
        assertTrue(chung.getLaAnhChinh());
        assertTrue(doCu.getLaAnhChinh());
        assertEquals(HinhAnhSanPham.HOAT_DONG, doCu.getTrangThai());
    }

    @Test
    void dongBoRong_boHetAnhCuaMau_khongXoaDong() {
        HinhAnhSanPham denCu = anh(DEN, "den.jpg", true, true);
        List<HinhAnhSanPham> luu = AnhSanPham.dongBo(sp, DEN, List.of(denCu), List.of());
        assertEquals(1, luu.size());
        assertEquals(HinhAnhSanPham.NGUNG_HOAT_DONG, denCu.getTrangThai());
        assertFalse(denCu.getLaAnhChinh());
    }

    @Test
    void dongBoAnhChung_boQuaUrlTrung() {
        List<HinhAnhSanPham> luu = AnhSanPham.dongBo(sp, null, List.of(), List.of("a.jpg", "a.jpg", "b.jpg"));
        assertEquals(2, luu.size());
        assertNull(luu.get(0).getIdMauSac());
        assertTrue(luu.get(0).getLaAnhChinh());
        assertFalse(luu.get(1).getLaAnhChinh());
    }
}
