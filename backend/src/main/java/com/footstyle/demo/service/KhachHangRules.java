package com.footstyle.demo.service;

import com.footstyle.demo.exception.ApiException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quy tắc kiểm tra + chuẩn hóa dữ liệu khách hàng và địa chỉ giao hàng.
 * Dùng chung cho thêm / sửa khách hàng và sổ địa chỉ, để gọi thẳng API (Postman...) cũng bị chặn như trên giao diện.
 * Mỗi hàm trả về giá trị đã chuẩn hóa, sai thì ném lỗi 400 kèm thông báo tiếng Việt.
 * Frontend có bản tương tự: frontend/src/utils/khachHangValidate.js (sửa quy tắc thì sửa cả hai).
 */
public final class KhachHangRules {

    public static final int TEN_MIN = 2;
    public static final int TEN_MAX = 100;
    public static final int EMAIL_MAX = 100;
    public static final int TUOI_MIN = 10;
    public static final int TUOI_MAX = 100;
    public static final int DIA_CHI_MIN = 5;
    public static final int DIA_CHI_MAX = 255;
    public static final int DON_VI_HANH_CHINH_MAX = 100;
    public static final int ANH_MAX = 500;
    /** Số địa chỉ tối đa trong sổ địa chỉ của một khách hàng. */
    public static final int SO_DIA_CHI_MAX = 10;

    /** Chỉ chữ cái (kể cả tiếng Việt có dấu), các từ cách nhau đúng 1 khoảng trắng. */
    private static final Pattern HO_TEN = Pattern.compile("^[\\p{L}\\p{M}]+( [\\p{L}\\p{M}]+)*$");
    /** SĐT di động Việt Nam: 10 số, đầu 03 / 05 / 07 / 08 / 09. */
    private static final Pattern SDT_VN = Pattern.compile("^0[35789]\\d{8}$");
    /** Ký tự được phép gõ kèm SĐT (sẽ tự bỏ): khoảng trắng, dấu chấm, gạch ngang, ngoặc. */
    private static final Pattern KY_TU_PHAN_CACH_SDT = Pattern.compile("[\\s.\\-()]");
    /** Phần trước @ không bắt đầu / kết thúc bằng dấu chấm, không có 2 dấu chấm liền nhau; tên miền có đuôi >= 2 chữ. */
    private static final Pattern EMAIL = Pattern.compile(
            "^[a-z0-9]+([._%+-][a-z0-9]+)*@[a-z0-9]+(-[a-z0-9]+)*(\\.[a-z0-9]+(-[a-z0-9]+)*)*\\.[a-z]{2,}$");
    /** Địa chỉ cụ thể phải có ít nhất 1 chữ hoặc số (không chấp nhận chỉ toàn ký tự đặc biệt). */
    private static final Pattern CO_CHU_HOAC_SO = Pattern.compile(".*[\\p{L}\\d].*");
    private static final Pattern ANH_NOI_BO = Pattern.compile("^/api/uploads/[A-Za-z0-9._-]+$");
    private static final Pattern ANH_LINK = Pattern.compile("^https?://\\S+$");
    private static final Locale VI = Locale.forLanguageTag("vi");

    private KhachHangRules() {
    }

    /** Bỏ khoảng trắng đầu/cuối, gộp nhiều khoảng trắng thành 1, đưa chữ có dấu về dạng dựng sẵn (NFC). Rỗng -> null. */
    public static String chuanHoaKhoangTrang(String s) {
        if (s == null) return null;
        String t = Normalizer.normalize(s, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /** Họ tên khách hàng / người nhận. nhan: "Họ tên", "Tên người nhận"... dùng trong thông báo lỗi. */
    public static String hoTen(String raw, String nhan) {
        String v = chuanHoaKhoangTrang(raw);
        if (v == null) throw loi("Vui lòng nhập " + thuong(nhan) + ".");
        if (v.length() < TEN_MIN || v.length() > TEN_MAX) {
            throw loi(nhan + " phải từ " + TEN_MIN + " đến " + TEN_MAX + " ký tự.");
        }
        if (!HO_TEN.matcher(v).matches()) {
            throw loi(nhan + " chỉ được chứa chữ cái và khoảng trắng (không chứa số, ký tự đặc biệt).");
        }
        return v;
    }

    /** Chuẩn hóa SĐT: bỏ khoảng trắng / dấu chấm / gạch ngang, đổi +84 hoặc 84 ở đầu thành 0. Rỗng -> null. */
    public static String chuanHoaSdt(String raw) {
        if (raw == null) return null;
        String v = KY_TU_PHAN_CACH_SDT.matcher(raw).replaceAll("");
        if (v.startsWith("+84")) v = "0" + v.substring(3);
        else if (v.startsWith("84") && v.length() == 11) v = "0" + v.substring(2);
        return v.isEmpty() ? null : v;
    }

    public static String sdt(String raw, String nhan) {
        String v = chuanHoaSdt(raw);
        if (v == null) throw loi("Vui lòng nhập " + thuong(nhan) + ".");
        if (!SDT_VN.matcher(v).matches()) {
            throw loi(nhan + " không hợp lệ (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
        }
        return v;
    }

    /** Email bắt buộc, lưu chữ thường để so trùng không phân biệt hoa / thường. */
    public static String email(String raw) {
        String v = raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
        if (v == null || v.isEmpty()) throw loi("Vui lòng nhập email.");
        if (v.length() > EMAIL_MAX) throw loi("Email tối đa " + EMAIL_MAX + " ký tự.");
        if (!EMAIL.matcher(v).matches()) throw loi("Email không đúng định dạng (ví dụ: ten@gmail.com).");
        return v;
    }

    /** Ngày sinh không bắt buộc; có nhập thì không ở tương lai và tuổi trong khoảng TUOI_MIN..TUOI_MAX. */
    public static LocalDate ngaySinh(LocalDate d, LocalDate homNay) {
        if (d == null) return null;
        if (d.isAfter(homNay)) throw loi("Ngày sinh không được ở tương lai.");
        int tuoi = Period.between(d, homNay).getYears();
        if (tuoi < TUOI_MIN) throw loi("Khách hàng phải từ " + TUOI_MIN + " tuổi trở lên.");
        if (tuoi > TUOI_MAX) throw loi("Ngày sinh không hợp lệ (khách hàng không quá " + TUOI_MAX + " tuổi).");
        return d;
    }

    /** 0: Nữ, 1: Nam. Không gửi -> mặc định Nam. */
    public static Integer gioiTinh(Integer v) {
        if (v == null) return 1;
        if (v != 0 && v != 1) throw loi("Giới tính không hợp lệ.");
        return v;
    }

    /** 0: Ngừng hoạt động, 1: Hoạt động. Không gửi -> mặc định Hoạt động. */
    public static Integer trangThai(Integer v) {
        if (v == null) return 1;
        if (v != 0 && v != 1) throw loi("Trạng thái không hợp lệ.");
        return v;
    }

    /** Ảnh đại diện không bắt buộc; có thì phải là ảnh đã tải lên hệ thống (/api/uploads/...) hoặc link http(s). */
    public static String anhDaiDien(String raw) {
        String v = raw == null ? null : raw.trim();
        if (v == null || v.isEmpty()) return null;
        if (v.length() > ANH_MAX || v.contains("..")
                || !(ANH_NOI_BO.matcher(v).matches() || ANH_LINK.matcher(v).matches())) {
            throw loi("Ảnh đại diện không hợp lệ.");
        }
        return v;
    }

    public static String tinhThanh(String raw) {
        return donViHanhChinh(raw, "Tỉnh/Thành phố");
    }

    public static String phuongXa(String raw) {
        return donViHanhChinh(raw, "Phường/Xã");
    }

    public static String diaChiCuThe(String raw) {
        String v = chuanHoaKhoangTrang(raw);
        if (v == null) throw loi("Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).");
        if (v.length() < DIA_CHI_MIN || v.length() > DIA_CHI_MAX) {
            throw loi("Địa chỉ cụ thể phải từ " + DIA_CHI_MIN + " đến " + DIA_CHI_MAX + " ký tự.");
        }
        if (!CO_CHU_HOAC_SO.matcher(v).matches()) throw loi("Địa chỉ cụ thể phải có chữ hoặc số.");
        return v;
    }

    private static String donViHanhChinh(String raw, String nhan) {
        String v = chuanHoaKhoangTrang(raw);
        if (v == null) throw loi("Vui lòng chọn " + nhan + ".");
        if (v.length() > DON_VI_HANH_CHINH_MAX) throw loi(nhan + " không hợp lệ.");
        return v;
    }

    /** Chữ thường cho phần giữa câu: "Họ tên" -> "họ tên". */
    private static String thuong(String nhan) {
        return nhan.toLowerCase(VI);
    }

    private static ApiException loi(String message) {
        return ApiException.badRequest(message);
    }
}
