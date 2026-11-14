package com.footstyle.demo.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Phí vận chuyển theo Tỉnh/Thành phố nhận hàng (cửa hàng ở Hà Nội).
 * <ul>
 *   <li>Hà Nội: miễn phí, bất kể giá trị đơn.</li>
 *   <li>Tỉnh/thành khác: mức phí tùy tỉnh (xa Hà Nội thì phí cao hơn).</li>
 * </ul>
 * Lớp này chỉ tính phí theo tỉnh. Đơn đạt ngưỡng miễn phí vận chuyển theo giá trị đơn (quản lý cài đặt) thì nơi gọi đưa phí về 0
 * qua {@link CaiDatVanChuyenService}.
 * Dùng chung cho đơn online, đơn giao hàng bán tại quầy và sửa đơn. Bảng này phải khớp frontend/src/utils/phiVanChuyenTinh.js.
 * Danh sách theo 34 tỉnh/thành sau sáp nhập 07/2025 (đúng danh sách khách chọn ở form địa chỉ).
 */
public final class PhiVanChuyenTinh {

    /** Phí khi không nhận ra tên tỉnh (vd địa chỉ cũ ghi tay). */
    public static final long PHI_MAC_DINH = 35_000L;

    private static final Map<String, Long> BANG_PHI = new LinkedHashMap<>();

    private static void them(long phi, String... tenTinh) {
        for (String ten : tenTinh) BANG_PHI.put(khoa(ten), phi);
    }

    static {
        them(0L, "Hà Nội");
        // Lân cận Hà Nội
        them(25_000L, "Bắc Ninh", "Hưng Yên", "Phú Thọ", "Thái Nguyên", "Hải Phòng", "Ninh Bình");
        // Miền núi phía Bắc, Quảng Ninh
        them(30_000L, "Quảng Ninh", "Lào Cai", "Tuyên Quang", "Lạng Sơn", "Cao Bằng", "Điện Biên", "Lai Châu", "Sơn La");
        // Bắc Trung Bộ
        them(35_000L, "Thanh Hóa", "Nghệ An", "Hà Tĩnh");
        // Trung Bộ
        them(40_000L, "Quảng Trị", "Huế", "Đà Nẵng", "Quảng Ngãi");
        // Nam Trung Bộ, Tây Nguyên
        them(45_000L, "Gia Lai", "Khánh Hòa", "Đắk Lắk", "Lâm Đồng");
        // Đông Nam Bộ
        them(50_000L, "Hồ Chí Minh", "Đồng Nai", "Tây Ninh");
        // Tây Nam Bộ
        them(55_000L, "Cần Thơ", "Vĩnh Long", "Đồng Tháp", "Cà Mau", "An Giang");
    }

    private PhiVanChuyenTinh() {
    }

    /** Khóa so khớp: bỏ dấu, chữ thường, bỏ tiền tố Tỉnh / Thành phố / TP., bỏ khoảng trắng và dấu câu. */
    static String khoa(String ten) {
        if (ten == null) return "";
        String t = Normalizer.normalize(ten, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).trim()
                .replaceFirst("^(thanh pho|tinh|tp\\.?)\\s+", "");
        return t.replaceAll("[^a-z0-9]", "");
    }

    /** Phí giao hàng tới tỉnh/thành này; trống hoặc không nhận ra tên thì {@link #PHI_MAC_DINH}. */
    public static long phi(String tinhThanh) {
        Long phi = BANG_PHI.get(khoa(tinhThanh));
        return phi != null ? phi : PHI_MAC_DINH;
    }

    /** Mức phí thấp nhất trong các tỉnh/thành CÓ tính phí (không kể nơi miễn phí). */
    public static long phiThapNhat() {
        return BANG_PHI.values().stream().filter(p -> p > 0).mapToLong(Long::longValue).min().orElse(PHI_MAC_DINH);
    }

    /** Mức phí cao nhất trong bảng phí theo tỉnh. */
    public static long phiCaoNhat() {
        return BANG_PHI.values().stream().mapToLong(Long::longValue).max().orElse(PHI_MAC_DINH);
    }

    /** Có nhận ra tên tỉnh/thành trong bảng phí không. */
    public static boolean nhanRa(String tinhThanh) {
        return BANG_PHI.containsKey(khoa(tinhThanh));
    }

    /**
     * Phí theo địa chỉ ghi liền một chuỗi ("Số 15, Phường X, Thành phố Hà Nội"): lấy phần cuối sau dấu phẩy cuối cùng làm tỉnh.
     * Không nhận ra tỉnh thì trả {@code null} để nơi gọi giữ phí cũ.
     */
    public static Long phiTheoDiaChi(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) return null;
        String[] phan = diaChi.split(",");
        String tinh = phan[phan.length - 1];
        return nhanRa(tinh) ? phi(tinh) : null;
    }
}
