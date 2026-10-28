package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/**
 * Bộ lọc ngày của danh sách hóa đơn: trang hóa đơn mặc định lọc Từ ngày = Đến ngày = hôm nay,
 * nên điều kiện phải lấy TRỌN ngày (kể cả hóa đơn tạo lúc 23:59:59.xxx).
 */
class HoaDonLocNgayTest {

    /** Một lời gọi CriteriaBuilder đã ghi lại: tên hàm, cột, giá trị. */
    record Goi(String ham, String cot, Object giaTri) {
    }

    @SuppressWarnings("unchecked")
    private static <T> T gia(Class<T> kieu, java.lang.reflect.InvocationHandler xuLy) {
        return (T) Proxy.newProxyInstance(kieu.getClassLoader(), new Class<?>[] {kieu}, (p, m, a) -> {
            switch (m.getName()) {
                case "toString": return kieu.getSimpleName();
                case "hashCode": return System.identityHashCode(p);
                case "equals": return p == a[0];
                default: return xuLy.invoke(p, m, a);
            }
        });
    }

    private static <T> T khongDung(Class<T> kieu) {
        return gia(kieu, (p, m, a) -> {
            throw new UnsupportedOperationException(kieu.getSimpleName() + "." + m.getName());
        });
    }

    /** Gọi demTheoTrangThai với kho giả, lấy Specification đầu tiên rồi chạy nó trên CriteriaBuilder giả. */
    private static List<Goi> chayBoLoc(LocalDate tu, LocalDate den) {
        List<Specification<HoaDon>> spec = new ArrayList<>();
        HoaDonRepository kho = gia(HoaDonRepository.class, (p, m, a) -> {
            if (m.getName().equals("count") && a != null && a.length == 1 && a[0] instanceof Specification<?> s) {
                @SuppressWarnings("unchecked")
                Specification<HoaDon> sp = (Specification<HoaDon>) s;
                spec.add(sp);
                return 0L;
            }
            throw new UnsupportedOperationException("HoaDonRepository." + m.getName());
        });
        HoaDonService service = new HoaDonService(kho, khongDung(HoaDonChiTietRepository.class),
                khongDung(LichSuHoaDonRepository.class), khongDung(LichSuThanhToanRepository.class),
                khongDung(HinhAnhSanPhamRepository.class), khongDung(SanPhamChiTietRepository.class),
                khongDung(PhieuGiamGiaRepository.class),
                khongDung(PhieuGiamGiaKhachHangRepository.class));
        service.demTheoTrangThai(null, tu, den, null);
        assertFalse(spec.isEmpty());

        List<Goi> goi = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Root<HoaDon> root = gia(Root.class, (p, m, a) -> {
            if (m.getName().equals("get") && a.length == 1 && a[0] instanceof String cot) {
                return gia(Path.class, (pp, mm, aa) -> {
                    if (mm.getName().equals("getAlias")) return cot;
                    throw new UnsupportedOperationException("Path." + mm.getName());
                });
            }
            throw new UnsupportedOperationException("Root." + m.getName());
        });
        CriteriaBuilder cb = gia(CriteriaBuilder.class, (p, m, a) -> {
            if (m.getName().equals("and")) return gia(Predicate.class, (pp, mm, aa) -> null);
            String cot = a != null && a.length > 0 && a[0] instanceof Path<?> path ? path.getAlias() : null;
            goi.add(new Goi(m.getName(), cot, a != null && a.length > 1 ? a[1] : null));
            return gia(Predicate.class, (pp, mm, aa) -> null);
        });
        @SuppressWarnings("unchecked")
        CriteriaQuery<HoaDon> query = khongDung(CriteriaQuery.class);
        assertNotNull(spec.get(0).toPredicate(root, query, cb));
        return goi;
    }

    @Test
    void cungMotNgay_layTronNgayTu0hDenTruoc0hHomSau() {
        LocalDate homNay = LocalDate.of(2026, 10, 13);
        List<Goi> goi = chayBoLoc(homNay, homNay);
        assertTrue(goi.contains(new Goi("greaterThanOrEqualTo", "ngayTao", LocalDateTime.of(2026, 10, 13, 0, 0))), goi.toString());
        assertTrue(goi.contains(new Goi("lessThan", "ngayTao", LocalDateTime.of(2026, 10, 14, 0, 0))), goi.toString());
        assertTrue(goi.stream().noneMatch(g -> g.ham().equals("lessThanOrEqualTo")), goi.toString());

        LocalDateTime cuoiNgay = LocalDateTime.of(2026, 10, 13, 23, 59, 59, 997_000_000);
        assertTrue(cuoiNgay.isBefore(LocalDateTime.of(2026, 10, 14, 0, 0)), "hóa đơn 23:59:59.997 vẫn thuộc ngày 13/10");
        assertTrue(cuoiNgay.isAfter(LocalDateTime.of(2026, 10, 13, 23, 59, 59)), "điều kiện cũ (<= 23:59:59) sẽ bỏ sót hóa đơn này");
    }

    @Test
    void khongChonNgay_khongThemDieuKienNgay() {
        List<Goi> goi = chayBoLoc(null, null);
        assertEquals(0, goi.stream().filter(g -> "ngayTao".equals(g.cot())).count(), goi.toString());
    }

    @Test
    void chiChonDenNgay_vanLayTronNgayDo() {
        List<Goi> goi = chayBoLoc(null, LocalDate.of(2026, 12, 31));
        assertEquals(List.of(new Goi("lessThan", "ngayTao", LocalDateTime.of(2027, 1, 1, 0, 0))),
                goi.stream().filter(g -> "ngayTao".equals(g.cot())).toList());
    }
}
