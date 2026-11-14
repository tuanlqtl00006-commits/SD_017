package com.footstyle.demo.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ghi dữ liệu qua API nào thì báo cho những màn nào tải lại (chủ đề). Một chỗ duy nhất, có test ChuDeSuKienTest.
 * Giao diện (frontend/src/services/suKien.js) dùng đúng các tên chủ đề này.
 */
public final class ChuDeSuKien {

    private ChuDeSuKien() {
    }

    public static final String HOA_DON = "hoa-don";
    public static final String TON_KHO = "ton-kho";
    public static final String SAN_PHAM = "san-pham";
    public static final String GIAM_GIA = "giam-gia";
    public static final String PHIEU_GIAM_GIA = "phieu-giam-gia";
    public static final String KHACH_HANG = "khach-hang";
    public static final String NHAN_VIEN = "nhan-vien";
    public static final String GIAO_CA = "giao-ca";
    public static final String LICH_LAM_VIEC = "lich-lam-viec";
    public static final String CAI_DAT = "cai-dat";

    /**
     * Ghi nhưng không đổi dữ liệu các màn đang xem: đăng nhập, tính tiền thử giỏ hàng, trợ lý, gửi email thống kê,
     * chat (có kênh riêng), tải ảnh lên (chưa lưu vào sản phẩm). Xét trước bảng bên dưới.
     */
    private static final List<String> BO_QUA = List.of(
            "/api/auth",
            "/api/cua-hang/tinh-tien",
            "/api/tro-ly",
            "/api/thong-ke",
            "/api/chat",
            "/api/ho-tro",
            "/api/hinh-anh/upload",
            "/api/uploads",
            "/api/su-kien",
            "/api/test-connection");

    /** Gốc đường dẫn -> chủ đề bị ảnh hưởng. */
    private static final List<Map.Entry<String, Set<String>>> BANG = List.of(
            // bán tại quầy: hóa đơn, tồn kho, số liệu ca, lượt dùng phiếu
            Map.entry("/api/ban-hang", Set.of(HOA_DON, TON_KHO, GIAO_CA, PHIEU_GIAM_GIA)),
            // đổi trạng thái / sửa / hủy đơn: hủy thì trả hàng, xác nhận thì trừ kho
            Map.entry("/api/hoa-don", Set.of(HOA_DON, TON_KHO, GIAO_CA, PHIEU_GIAM_GIA)),
            // cửa hàng online: đặt hàng, xác nhận thanh toán (kể cả QR chuyển khoản ở quầy)
            Map.entry("/api/cua-hang", Set.of(HOA_DON, TON_KHO, GIAO_CA, PHIEU_GIAM_GIA)),
            // khách đăng nhập: sửa tài khoản, địa chỉ, tự hủy đơn
            Map.entry("/api/khach", Set.of(KHACH_HANG, HOA_DON, TON_KHO, PHIEU_GIAM_GIA)),
            Map.entry("/api/san-pham", Set.of(SAN_PHAM, TON_KHO)),
            Map.entry("/api/bien-the-san-pham", Set.of(SAN_PHAM, TON_KHO)),
            Map.entry("/api/hinh-anh", Set.of(SAN_PHAM)),
            Map.entry("/api/thuoc-tinh", Set.of(SAN_PHAM)),
            // đợt giảm giá đổi giá bán của sản phẩm
            Map.entry("/api/dot-giam-gia", Set.of(GIAM_GIA, SAN_PHAM)),
            Map.entry("/api/dot-giam-gia-chi-tiet", Set.of(GIAM_GIA, SAN_PHAM)),
            Map.entry("/api/giam-gia", Set.of(GIAM_GIA, SAN_PHAM)),
            Map.entry("/api/phieu-giam-gia", Set.of(PHIEU_GIAM_GIA)),
            Map.entry("/api/khach-hang", Set.of(KHACH_HANG)),
            Map.entry("/api/dia-chi", Set.of(KHACH_HANG)),
            Map.entry("/api/nhan-vien", Set.of(NHAN_VIEN)),
            Map.entry("/api/vai-tro", Set.of(NHAN_VIEN)),
            Map.entry("/api/giao-ca", Set.of(GIAO_CA)),
            Map.entry("/api/lich-lam-viec", Set.of(LICH_LAM_VIEC, GIAO_CA)),
            Map.entry("/api/ca-lam-viec", Set.of(LICH_LAM_VIEC, GIAO_CA)),
            // cài đặt cửa hàng (ngưỡng miễn phí vận chuyển): màn quầy, giỏ hàng tính lại phí giao
            Map.entry("/api/cai-dat", Set.of(CAI_DAT)));

    /** POST / PUT / PATCH / DELETE mới có thể đổi dữ liệu. */
    public static boolean laGhi(String method) {
        if (method == null) return false;
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    /** Chủ đề cần báo sau khi `method path` chạy thành công; rỗng = không báo. */
    public static Set<String> theoYeuCau(String method, String path) {
        if (!laGhi(method)) return Set.of();
        String p = chuanHoa(path);
        for (String g : BO_QUA) if (thuoc(p, g)) return Set.of();
        for (Map.Entry<String, Set<String>> e : BANG) if (thuoc(p, e.getKey())) return e.getValue();
        return Set.of();
    }

    /** `path` là `goc` hoặc nằm dưới `goc/` (không nhầm /api/khach với /api/khach-hang). */
    private static boolean thuoc(String path, String goc) {
        return path.equals(goc) || path.startsWith(goc + "/");
    }

    private static String chuanHoa(String path) {
        if (path == null || path.isEmpty()) return "/";
        String p = path;
        int hoi = p.indexOf('?');
        if (hoi >= 0) p = p.substring(0, hoi);
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p.toLowerCase(Locale.ROOT);
    }
}
