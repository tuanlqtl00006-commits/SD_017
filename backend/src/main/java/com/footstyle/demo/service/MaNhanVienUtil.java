package com.footstyle.demo.service;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

/**
 * Tạo mã nhân viên theo HỌ TÊN ĐẦY ĐỦ: Tên + chữ cái đầu của họ và tên đệm + số thứ tự (2 chữ số).
 * Ví dụ:  Nguyễn Văn An  -> AnNV01   (người thứ hai trùng tiền tố -> AnNV02)
 *         Trần Thị Mai Lan -> LanTTM01
 *         Lan -> Lan01
 * Bỏ dấu tiếng Việt, chỉ giữ chữ cái. Số thứ tự = số lớn nhất đang dùng cho cùng tiền tố + 1.
 */
public final class MaNhanVienUtil {

    private MaNhanVienUtil() {
    }

    /** Bỏ dấu tiếng Việt (đ -> d) và mọi ký tự không phải chữ cái / khoảng trắng. */
    static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return t.replaceAll("[^A-Za-z\\s]", " ").trim().replaceAll("\\s+", " ");
    }

    /** Phần chữ của mã (chưa có số thứ tự): 'Nguyễn Văn An' -> 'AnNV'. Họ tên không có chữ cái nào -> chuỗi rỗng. */
    public static String tienTo(String hoTen) {
        String sach = boDau(hoTen);
        if (sach.isEmpty()) return "";
        String[] tu = sach.split(" ");
        String ten = tu[tu.length - 1];
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(ten.charAt(0)));
        sb.append(ten.substring(1).toLowerCase(Locale.ROOT));
        for (int i = 0; i < tu.length - 1; i++) {
            sb.append(Character.toUpperCase(tu[i].charAt(0)));
        }
        return sb.toString();
    }

    /** Mã mới không trùng mã nào trong maDangCo (so sánh không phân biệt hoa thường). */
    public static String taoMa(String hoTen, Collection<String> maDangCo) {
        String tienTo = tienTo(hoTen);
        if (tienTo.isEmpty()) tienTo = "NV";
        String tienToThuong = tienTo.toLowerCase(Locale.ROOT);
        int max = 0;
        if (maDangCo != null) {
            for (String ma : maDangCo) {
                if (ma == null) continue;
                String m = ma.trim().toLowerCase(Locale.ROOT);
                if (m.startsWith(tienToThuong) && m.length() > tienToThuong.length()) {
                    String duoi = m.substring(tienToThuong.length());
                    if (duoi.matches("\\d{1,9}")) {
                        max = Math.max(max, Integer.parseInt(duoi));
                    }
                }
            }
        }
        return String.format("%s%02d", tienTo, max + 1);
    }
}
