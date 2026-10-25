package com.footstyle.demo.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Giá trị thuộc tính phải là giá trị THẬT dùng ở thị trường cầu lông Việt Nam (cô: "đừng nhập size, màu linh tinh... web làm cho khu vực
 * nào thì xem giá trị phù hợp ở khu vực đó", tất cả các nhóm đều phải sửa):
 *  - Màu sắc, độ cứng, điểm cân bằng, xuất xứ: chỉ chữ (có dấu tiếng Việt), khoảng trắng, gạch nối / gạch chéo - không có số, ký tự lạ.
 *  - Trọng lượng vợt: chuẩn "nU" (1U - 8U), có thể kèm khoảng gram ĐÚNG chuẩn của nU, vd "4U (80-84g)" (4U = 80 - 84 g).
 *  - Chu vi cán: G1 - G6 (chuẩn Yonex / Lining bán ở châu Á).
 *  - Danh mục, thương hiệu, chất liệu: phải có chữ, chỉ gồm chữ / số / khoảng trắng và vài dấu thường gặp (- & . ' / ( ) %).
 *  - "Không áp dụng" (mục hệ thống của 4 bảng chỉ vợt có) luôn hợp lệ.
 */
final class ThuocTinhHopLe {

    private static final String CHU = "\\p{L}\\p{M}";
    private static final Pattern CHI_CHU = Pattern.compile("[" + CHU + "]+(( ?[-/] ?| )[" + CHU + "]+)*");
    private static final Pattern CO_CHU = Pattern.compile(".*[" + CHU + "].*");
    private static final Pattern TEN_CHUNG = Pattern.compile("[" + CHU + "0-9 &.'/()%+\\-]+");
    private static final Pattern TRONG_LUONG = Pattern.compile("([1-8])U(?: \\((\\d{2,3}) ?- ?(\\d{2,3}) ?g\\))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHU_VI = Pattern.compile("G([1-6])", Pattern.CASE_INSENSITIVE);

    private ThuocTinhHopLe() {
    }

    /** Chuẩn hóa cách viết (4u -> 4U, g5 -> G5, "4U(80-84 g)" -> "4U (80-84g)"); loại khác giữ nguyên. */
    static String chuanHoa(String loai, String ten) {
        if (ten == null) return "";
        String t = ten.trim().replaceAll("\\s+", " ");
        if ("trong-luong".equals(loai)) {
            String g = t.replaceAll("\\s*\\(\\s*", " (").replaceAll("\\s*\\)", ")").replaceAll("\\s*-\\s*", "-").replaceAll("(\\d)\\s*[gG]\\)", "$1g)");
            Matcher m = TRONG_LUONG.matcher(g);
            if (m.matches()) return m.group(1) + "U" + (m.group(2) == null ? "" : " (" + m.group(2) + "-" + m.group(3) + "g)");
        }
        if ("chu-vi".equals(loai) && CHU_VI.matcher(t).matches()) return t.toUpperCase(Locale.ROOT);
        return t;
    }

    /** Câu báo lỗi nếu tên không hợp lệ với loại thuộc tính; hợp lệ thì null. ten đã qua chuanHoa. */
    static String loi(String loai, String ten) {
        if (ten == null || ten.isEmpty() || NhomDanhMuc.laKhongApDung(ten)) return null;
        switch (loai) {
            case "mau-sac":
                return CHI_CHU.matcher(ten).matches() ? null
                        : "Tên màu chỉ gồm chữ (vd: Đen, Xanh dương, Trắng - Đỏ), không chứa số hay ký tự đặc biệt.";
            case "do-cung":
                return CHI_CHU.matcher(ten).matches() ? null : "Độ cứng chỉ gồm chữ (vd: Mềm, Trung bình, Cứng, Siêu cứng).";
            case "diem-can-bang":
                return CHI_CHU.matcher(ten).matches() ? null : "Điểm cân bằng chỉ gồm chữ (vd: Nặng đầu, Cân bằng, Nhẹ đầu).";
            case "xuat-xu":
                return CHI_CHU.matcher(ten).matches() ? null : "Xuất xứ là tên quốc gia / vùng lãnh thổ, chỉ gồm chữ (vd: Nhật Bản, Đài Loan).";
            case "trong-luong": {
                Matcher m = TRONG_LUONG.matcher(ten);
                if (!m.matches()) return "Trọng lượng vợt theo chuẩn U: 1U - 8U, có thể kèm khoảng gram, vd: 4U hoặc 4U (80-84g).";
                if (m.group(2) == null) return null;
                int u = Integer.parseInt(m.group(1));
                int tu = 100 - 5 * u;
                int den = tu + 4;
                if (Integer.parseInt(m.group(2)) != tu || Integer.parseInt(m.group(3)) != den) {
                    return u + "U là " + tu + "-" + den + "g. Hãy nhập " + u + "U (" + tu + "-" + den + "g).";
                }
                return null;
            }
            case "chu-vi":
                return CHU_VI.matcher(ten).matches() ? null : "Chu vi cán theo chuẩn G1 - G6 (vd: G4, G5). Số càng lớn cán càng nhỏ.";
            default:
                if (!CO_CHU.matcher(ten).matches()) return "Tên phải có chữ, không chỉ gồm số hay ký tự đặc biệt.";
                return TEN_CHUNG.matcher(ten).matches() ? null
                        : "Tên chỉ gồm chữ, số, khoảng trắng và các dấu - & . ' / ( ) %.";
        }
    }
}
