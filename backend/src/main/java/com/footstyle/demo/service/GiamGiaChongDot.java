package com.footstyle.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Tính mức giảm khi MỘT biến thể nằm trong nhiều đợt giảm giá chạy chồng ngày nhau.
 * Lớp thuần Java (không phụ thuộc Spring) nên kiểm tra được độc lập.
 *
 * Ví dụ (theo ghi chú):
 *   Đợt A: 10%, 1/10 - 10/10, biến thể SPCT1, SPCT5
 *   Đợt B: 15%, 4/10 - 20/10, biến thể SPCT1, SPCT3, SPCT4
 *   SPCT1 -> 1/10-3/10: 10% | 4/10-10/10: trùng 2 đợt (MAX = 15%, TRUNG_BINH = 12,5%) | 11/10-20/10: 15%
 *
 * Ngày bắt đầu và ngày kết thúc đều tính là ngày áp dụng (ngày kết thúc áp dụng hết ngày).
 */
public final class GiamGiaChongDot {

    private GiamGiaChongDot() {
    }

    /** Cách tính khi trùng đợt: MAX = lấy mức giảm cao nhất, TRUNG_BINH = trung bình cộng các đợt. */
    public enum CheDo {
        MAX, TRUNG_BINH;

        /** Đọc từ cấu hình; không nhận ra thì dùng MAX. */
        public static CheDo tuChuoi(String s) {
            if (s == null) return MAX;
            String v = s.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            return v.equals("TRUNG_BINH") || v.equals("TBC") || v.equals("AVERAGE") ? TRUNG_BINH : MAX;
        }
    }

    /** Một đợt áp dụng cho biến thể: khoảng ngày + % giảm của biến thể trong đợt đó. */
    public record DotApDung(Long idDot, LocalDate batDau, LocalDate ketThuc, int phanTram) {
        boolean chua(LocalDate ngay) {
            return !ngay.isBefore(batDau) && !ngay.isAfter(ketThuc);
        }
    }

    /** Một đoạn ngày liên tiếp có cùng mức giảm và cùng các đợt đang chạy. */
    public record Doan(LocalDate tuNgay, LocalDate denNgay, double phanTram, List<Long> idDots) {
        public boolean trungDot() {
            return idDots.size() > 1;
        }
    }

    /** Gộp danh sách % của các đợt đang chạy thành một mức giảm. Rỗng thì 0. */
    public static double gop(List<Integer> phanTrams, CheDo cheDo) {
        if (phanTrams == null || phanTrams.isEmpty()) return 0;
        if (cheDo == CheDo.TRUNG_BINH) {
            double tong = 0;
            for (int p : phanTrams) tong += p;
            return Math.round(tong / phanTrams.size() * 100.0) / 100.0;
        }
        int max = 0;
        for (int p : phanTrams) max = Math.max(max, p);
        return max;
    }

    /** Mức giảm của biến thể vào một ngày cụ thể. */
    public static double phanTramTaiNgay(List<DotApDung> dots, LocalDate ngay, CheDo cheDo) {
        List<Integer> dangChay = new ArrayList<>();
        for (DotApDung d : dots) {
            if (d.chua(ngay)) dangChay.add(d.phanTram());
        }
        return gop(dangChay, cheDo);
    }

    /** Lịch giảm giá của biến thể: các đoạn ngày liên tiếp, không chồng nhau, theo thứ tự thời gian. */
    public static List<Doan> lich(List<DotApDung> dots, CheDo cheDo) {
        TreeSet<LocalDate> moc = new TreeSet<>();
        for (DotApDung d : dots) {
            moc.add(d.batDau());
            moc.add(d.ketThuc().plusDays(1));
        }
        List<LocalDate> cacMoc = new ArrayList<>(moc);

        List<Doan> ketQua = new ArrayList<>();
        for (int i = 0; i + 1 < cacMoc.size(); i++) {
            LocalDate tu = cacMoc.get(i);
            LocalDate den = cacMoc.get(i + 1).minusDays(1);
            List<Long> ids = new ArrayList<>();
            List<Integer> pts = new ArrayList<>();
            for (DotApDung d : dots) {
                if (d.chua(tu)) {
                    ids.add(d.idDot());
                    pts.add(d.phanTram());
                }
            }
            if (ids.isEmpty()) continue; // khoảng trống giữa hai đợt
            double pt = gop(pts, cheDo);

            // Đoạn liền kề có cùng mức giảm và cùng các đợt thì gộp lại
            if (!ketQua.isEmpty()) {
                Doan truoc = ketQua.get(ketQua.size() - 1);
                if (truoc.denNgay().plusDays(1).equals(tu) && truoc.phanTram() == pt && truoc.idDots().equals(ids)) {
                    ketQua.set(ketQua.size() - 1, new Doan(truoc.tuNgay(), den, pt, ids));
                    continue;
                }
            }
            ketQua.add(new Doan(tu, den, pt, ids));
        }
        return ketQua;
    }
}
