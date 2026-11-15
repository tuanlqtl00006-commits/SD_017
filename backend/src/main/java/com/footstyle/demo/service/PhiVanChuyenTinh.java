package com.footstyle.demo.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Phí vận chuyển theo nơi nhận hàng (cửa hàng ở Hà Nội).
 * <ul>
 *   <li>Hà Nội: CÓ tính phí (góp ý 15/11, video 22:22 [14:39–15:55]: "nội thành 20-25 nghìn, ngoại thành trên 30 nghìn"):
 *       giao tới phường {@link #PHI_HA_NOI_PHUONG}, giao tới xã {@link #PHI_HA_NOI_XA}; chưa biết phường/xã thì tính như phường.</li>
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
    /** Giao trong Hà Nội tới phường (nội thành). */
    public static final long PHI_HA_NOI_PHUONG = 25_000L;
    /** Giao trong Hà Nội tới xã (ngoại thành). */
    public static final long PHI_HA_NOI_XA = 30_000L;
    private static final String KHOA_HA_NOI = khoa("Hà Nội");

    private static final Map<String, Long> BANG_PHI = new LinkedHashMap<>();

    private static void them(long phi, String... tenTinh) {
        for (String ten : tenTinh) BANG_PHI.put(khoa(ten), phi);
    }

    static {
        them(PHI_HA_NOI_PHUONG, "Hà Nội"); // phường; giao tới xã xem phi(tinh, phuongXa)
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

    /** Phí giao hàng tới tỉnh/thành này (Hà Nội tính như phường); trống hoặc không nhận ra tên thì {@link #PHI_MAC_DINH}. */
    public static long phi(String tinhThanh) {
        return phi(tinhThanh, null);
    }

    /** Phí giao hàng tới phường/xã của tỉnh/thành này: Hà Nội phân biệt phường / xã, tỉnh khác chỉ theo tỉnh. */
    public static long phi(String tinhThanh, String phuongXa) {
        String k = khoa(tinhThanh);
        if (KHOA_HA_NOI.equals(k)) return laXa(phuongXa) ? PHI_HA_NOI_XA : PHI_HA_NOI_PHUONG;
        Long phi = BANG_PHI.get(k);
        return phi != null ? phi : PHI_MAC_DINH;
    }

    /** "Xã Sóc Sơn" -> true; "Phường Hà Đông", trống -> false. */
    static boolean laXa(String phuongXa) {
        if (phuongXa == null) return false;
        String t = Normalizer.normalize(phuongXa.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return t.startsWith("xa ");
    }

    /** Mức phí thấp nhất trong bảng (Hà Nội tới phường). */
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
        String phuong = phan.length >= 2 ? phan[phan.length - 2] : null; // "..., Xã Sóc Sơn, Thành phố Hà Nội"
        return nhanRa(tinh) ? phi(tinh, phuong) : null;
    }
}
