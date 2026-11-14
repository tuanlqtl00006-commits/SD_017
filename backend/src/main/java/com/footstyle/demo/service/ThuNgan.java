package com.footstyle.demo.service;

import com.footstyle.demo.entity.LichLamViec;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Thu ngân theo ca (review 05/11, cách B: không đổi SQL, không thêm vai trò). Quản lý chỉ định một nhân viên làm thu ngân của một ô lịch
 * (nhân viên x ca x ngày) bằng chữ "Thu ngân" ở đầu ghi chú của ô lịch đó (cột ghi_chu có sẵn). Chỉ thu ngân (và quản lý) mới mở ca bán
 * hàng và chốt ca; nhân viên khác chỉ bán trong ca đang mở.
 */
public final class ThuNgan {

    public static final String NHAN = "Thu ngân";
    private static final String DAU_NOI = " · ";
    static final int GHI_CHU_TOI_DA = 255;

    private ThuNgan() {
    }

    public static boolean la(LichLamViec l) {
        return l != null && la(l.getGhiChu());
    }

    /** Ghi chú bắt đầu bằng "Thu ngân" (không phân biệt hoa thường / dấu) và đứng riêng thành một từ. */
    public static boolean la(String ghiChu) {
        String g = chuanHoa(ghiChu);
        if (!g.startsWith("thu ngan")) return false;
        return g.length() == 8 || !Character.isLetterOrDigit(g.charAt(8));
    }

    /** Thêm / bỏ chữ "Thu ngân" ở đầu ghi chú, giữ nguyên phần ghi chú khác. Kết quả rỗng thì trả null. */
    public static String gan(String ghiChu, boolean thuNgan) {
        String cu = ghiChu == null ? "" : ghiChu.trim();
        boolean dang = la(cu);
        if (thuNgan == dang) return cu.isEmpty() ? null : cu;
        if (thuNgan) return cu.isEmpty() ? NHAN : NHAN + DAU_NOI + cu;
        // bỏ chữ Thu ngân và dấu nối đứng liền sau
        String con = cu.length() > NHAN.length() ? cu.substring(NHAN.length()) : "";
        int i = 0;
        while (i < con.length() && (Character.isWhitespace(con.charAt(i)) || "·-:,;".indexOf(con.charAt(i)) >= 0)) i++;
        String ketQua = con.substring(i).trim();
        return ketQua.isEmpty() ? null : ketQua;
    }

    /** Phần ghi chú sau khi bỏ chữ "Thu ngân" (để hiện riêng cạnh nhãn); null nếu không còn gì. */
    public static String ghiChuKhac(String ghiChu) {
        return gan(ghiChu, false);
    }

    private static String chuanHoa(String s) {
        if (s == null) return "";
        String d = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return d.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
