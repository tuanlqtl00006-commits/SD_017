package com.footstyle.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Tính mức giảm khi MỘT biến thể nằm trong nhiều đợt giảm giá chạy chồng thời gian nhau.
 * Lớp thuần Java (không phụ thuộc Spring) nên kiểm tra được độc lập.
 *
 * <p>Thời gian tính tới từng GIÂY: đợt có hiệu lực từ giờ bắt đầu tới giờ kết thúc (đều tính, kết thúc 17:00 là
 * hết phút 17:00 = 17:00:59 vì giờ kết thúc được chuẩn hóa như vậy khi lưu). Đợt cũ lưu theo ngày
 * (00:00:00 - 23:59:59) vẫn chạy đúng.
 *
 * <p>Ví dụ (theo ghi chú):
 * <pre>
 *   Đợt A: 10%, 1/10 - 10/10, biến thể SPCT1, SPCT5
 *   Đợt B: 15%, 4/10 - 20/10, biến thể SPCT1, SPCT3, SPCT4
 *   SPCT1 -> 1/10-3/10: 10% | 4/10-10/10: trùng 2 đợt (MAX = 15%, CONG_DON = 25%, TRUNG_BINH = 12,5%) | 11/10-20/10: 15%
 * </pre>
 */
public final class GiamGiaChongDot {

    private GiamGiaChongDot() {
    }

    /**
     * Cách tính khi trùng đợt:
     * MAX = lấy mức giảm cao nhất (mặc định), CONG_DON = cộng dồn các đợt (tối đa 100%), TRUNG_BINH = trung bình cộng.
     */
    public enum CheDo {
        MAX, CONG_DON, TRUNG_BINH;

        /** Câu giải thích ngắn hiện trên giao diện (form phiếu / đợt giảm giá). */
        public String moTa() {
            return switch (this) {
                case MAX -> "Lấy mức giảm CAO NHẤT trong các đợt đang chồng nhau (không cộng dồn).";
                case CONG_DON -> "CỘNG DỒN mức giảm của các đợt đang chồng nhau (tối đa 100%).";
                case TRUNG_BINH -> "Lấy TRUNG BÌNH CỘNG mức giảm của các đợt đang chồng nhau.";
            };
        }

        /** Đọc từ cấu hình; không nhận ra thì dùng MAX. */
        public static CheDo tuChuoi(String s) {
            if (s == null) return MAX;
            String v = s.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if (v.equals("CONG_DON") || v.equals("CONGDON") || v.equals("SUM") || v.equals("STACK")) return CONG_DON;
            if (v.equals("TRUNG_BINH") || v.equals("TBC") || v.equals("AVERAGE")) return TRUNG_BINH;
            return MAX;
        }
    }

    /** Một đợt áp dụng cho biến thể: khoảng thời gian (gồm cả hai đầu) + % giảm của biến thể trong đợt đó. */
    public record DotApDung(Long idDot, LocalDateTime batDau, LocalDateTime ketThuc, int phanTram) {
        boolean chua(LocalDateTime thoiDiem) {
            return !thoiDiem.isBefore(batDau) && !thoiDiem.isAfter(ketThuc);
        }
    }

    /** Một đoạn thời gian liên tiếp có cùng mức giảm và cùng các đợt đang chạy (tuLuc, denLuc đều tính). */
    public record Doan(LocalDateTime tuLuc, LocalDateTime denLuc, double phanTram, List<Long> idDots) {
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
        if (cheDo == CheDo.CONG_DON) {
            int tong = 0;
            for (int p : phanTrams) tong += p;
            return Math.min(100, tong); // cộng dồn nhưng không bao giờ giảm quá 100%
        }
        int max = 0;
        for (int p : phanTrams) max = Math.max(max, p);
        return max;
    }

    /** Mức giảm của biến thể tại một thời điểm cụ thể. */
    public static double phanTramTaiThoiDiem(List<DotApDung> dots, LocalDateTime thoiDiem, CheDo cheDo) {
        List<Integer> dangChay = new ArrayList<>();
        for (DotApDung d : dots) {
            if (d.chua(thoiDiem)) dangChay.add(d.phanTram());
        }
        return gop(dangChay, cheDo);
    }

    /** Lịch giảm giá của biến thể: các đoạn thời gian liên tiếp, không chồng nhau, theo thứ tự thời gian. */
    public static List<Doan> lich(List<DotApDung> dots, CheDo cheDo) {
        TreeSet<LocalDateTime> moc = new TreeSet<>();
        for (DotApDung d : dots) {
            moc.add(d.batDau());
            moc.add(d.ketThuc().plusSeconds(1));
        }
        List<LocalDateTime> cacMoc = new ArrayList<>(moc);

        List<Doan> ketQua = new ArrayList<>();
        for (int i = 0; i + 1 < cacMoc.size(); i++) {
            LocalDateTime tu = cacMoc.get(i);
            LocalDateTime den = cacMoc.get(i + 1).minusSeconds(1);
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
                if (truoc.denLuc().plusSeconds(1).equals(tu) && truoc.phanTram() == pt && truoc.idDots().equals(ids)) {
                    ketQua.set(ketQua.size() - 1, new Doan(truoc.tuLuc(), den, pt, ids));
                    continue;
                }
            }
            ketQua.add(new Doan(tu, den, pt, ids));
        }
        return ketQua;
    }
}
