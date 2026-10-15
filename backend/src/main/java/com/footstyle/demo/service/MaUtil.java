package com.footstyle.demo.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quy tắc chung cho MỌI mã do hệ thống tự cấp: TIỀN TỐ + số thứ tự, số thứ tự có TỐI THIỂU 2 chữ số, KHÔNG đệm thêm số 0.
 * Ví dụ:  HD01, HD02 ... HD09, HD10 ... HD99, HD100, HD1000   (không còn HD000001)
 *         NV01, KH01, SP01, DGG01, DM01, TH01, LQT01 ...
 * Quá 99 thì cứ tăng bình thường (HD100), không bị cắt hay đổi dạng.
 */
public final class MaUtil {

    /** Số chữ số tối thiểu của phần số thứ tự. */
    public static final int SO_CHU_SO_TOI_THIEU = 2;

    /** Chữ cái đứng đầu + phần số ở cuối, ví dụ HD000001, NV0003, LQT01. Không khớp (VD VCH7QX2YN, SP01-DEN) thì giữ nguyên. */
    private static final Pattern MA_CO_SO = Pattern.compile("^([A-Za-z]+)(\\d{1,9})$");

    private MaUtil() {
    }

    /** 1 -> "01", 9 -> "09", 10 -> "10", 100 -> "100". */
    public static String soThuTu(long n) {
        // Locale.ROOT: luôn là chữ số 0-9, không phụ thuộc ngôn ngữ của máy (máy đặt tiếng Ả Rập / Thái sẽ ra "NV٠٣" nếu để mặc định)
        return String.format(Locale.ROOT, "%0" + SO_CHU_SO_TOI_THIEU + "d", n);
    }

    /** ("HD", 1) -> "HD01"; ("HD", 100) -> "HD100". */
    public static String dinhDang(String tienTo, long n) {
        return (tienTo == null ? "" : tienTo) + soThuTu(n);
    }

    /**
     * Rút gọn mã cũ có quá nhiều số 0: HD000001 -> HD01, NV0010 -> NV10, DM001 -> DM01, DC000 -> DC00, LQT0001 -> LQT01.
     * Mã không có dạng "chữ cái + số" (hoặc null / rỗng) thì trả về nguyên vẹn. Chạy lại trên mã đã gọn thì không đổi.
     */
    public static String rutGon(String ma) {
        if (ma == null) return null;
        String m = ma.trim();
        Matcher mt = MA_CO_SO.matcher(m);
        if (!mt.matches()) return ma;
        return mt.group(1) + soThuTu(Long.parseLong(mt.group(2)));
    }

    /**
     * Mã biến thể dạng "SP001-DEN-4U-G5": chỉ rút gọn phần mã sản phẩm ở đầu (trước dấu gạch ngang đầu tiên)
     * -> "SP01-DEN-4U-G5". Phần màu / trọng lượng / chu vi phía sau giữ nguyên.
     */
    public static String rutGonMaBienThe(String ma) {
        if (ma == null) return null;
        int i = ma.indexOf('-');
        if (i <= 0) return rutGon(ma);
        return rutGon(ma.substring(0, i)) + ma.substring(i);
    }
}
