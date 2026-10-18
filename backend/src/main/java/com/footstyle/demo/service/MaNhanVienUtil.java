package com.footstyle.demo.service;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

/**
 * Tạo mã nhân viên theo HỌ TÊN: chữ cái đầu của MỌI từ trong họ tên (viết hoa, bỏ dấu) + số thứ tự (tối thiểu 2 chữ số).
 * Ví dụ:  Lê Quốc Tuấn      -> LQT01   (người thứ hai trùng chữ cái đầu -> LQT02)
 *         Nguyễn Văn An     -> NVA01
 *         Trần Thị Mai Lan  -> TTML01
 *         Lan               -> L01
 * Số thứ tự = số lớn nhất đang dùng cho cùng tiền tố + 1, nên mã không bao giờ trùng mã đã cấp.
 * Quá 99 thì viết thẳng LQT100 (không đệm số 0). Mã mẫu NV01...NV10 giữ nguyên (người có tiền tố NV đánh số tiếp, ví dụ NV11).
 * Mã cũ còn nhiều số 0 (NV0001, LQT0001) vẫn được đọc đúng khi tính số lớn nhất.
 */
public final class MaNhanVienUtil {

    /** Số chữ số tối thiểu của phần số thứ tự (xem MaUtil). */
    public static final int SO_CHU_SO = MaUtil.SO_CHU_SO_TOI_THIEU;

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

    /** Phần chữ của mã (chưa có số thứ tự): 'Lê Quốc Tuấn' -> 'LQT'. Họ tên không có chữ cái nào -> chuỗi rỗng. */
    public static String tienTo(String hoTen) {
        String sach = boDau(hoTen);
        if (sach.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String tu : sach.split(" ")) {
            sb.append(Character.toUpperCase(tu.charAt(0)));
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
        return MaUtil.dinhDang(tienTo, max + 1L);
    }

    /**
     * Tạo mã nhân viên chuẩn theo định dạng NV + số thứ tự (tối thiểu 2 chữ số, ví dụ NV11, NV12...).
     * Lấy số lớn nhất từ các mã đang có bắt đầu bằng tiền tố "NV" + 1.
     */
    public static String taoMaNV(Collection<String> maDangCo) {
        int max = 0;
        if (maDangCo != null) {
            for (String ma : maDangCo) {
                if (ma == null) continue;
                String m = ma.trim().toUpperCase(Locale.ROOT);
                if (m.startsWith("NV") && m.length() > 2) {
                    String duoi = m.substring(2);
                    if (duoi.matches("\\d{1,9}")) {
                        max = Math.max(max, Integer.parseInt(duoi));
                    }
                }
            }
        }
        return MaUtil.dinhDang("NV", max + 1L);
    }
}

