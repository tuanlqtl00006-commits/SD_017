package com.footstyle.demo.security;

import java.util.List;
import java.util.Locale;

/**
 * Bảng phân quyền API (một chỗ duy nhất, có test PhanQuyenTest).
 *  - Quản lý: mọi chức năng.
 *  - Nhân viên: bán hàng tại quầy, hóa đơn, khách hàng + địa chỉ, giao ca, xem lịch làm của mình, hỗ trợ khách, chat nội bộ;
 *    chỉ XEM sản phẩm / thuộc tính / giảm giá; không vào thống kê, quản lý nhân viên, xếp lịch, sửa sản phẩm / giảm giá.
 *  - Khách hàng (trang bán online sau này): chỉ API tài khoản của chính mình.
 * Đường dẫn công khai (không cần đăng nhập): đăng nhập, quên mật khẩu, cấu hình đăng nhập, ảnh đã tải lên, chat của khách.
 */
public final class PhanQuyen {

    private PhanQuyen() {
    }

    /** Không cần đăng nhập (mọi phương thức). */
    private static final List<String> CONG_KHAI = List.of(
            "/api/uploads",            // ảnh sản phẩm / ảnh đại diện (thẻ <img> không gửi kèm token)
            "/api/ho-tro/khach",       // khung chat của khách (bảo vệ bằng mã truy cập riêng của từng hội thoại)
            "/api/cua-hang",           // Bán hàng online (khách xem sản phẩm, đặt hàng, tra cứu)
            "/api/test-connection");

    /** Không cần đăng nhập, chỉ đúng phương thức. */
    private static final List<String[]> CONG_KHAI_THEO_PHUONG_THUC = List.of(
            new String[] {"POST", "/api/auth/dang-nhap"},
            new String[] {"POST", "/api/auth/khach-hang/dang-nhap"},
            new String[] {"POST", "/api/auth/khach-hang/dang-ky"},
            new String[] {"POST", "/api/auth/quen-mat-khau"},
            new String[] {"GET", "/api/auth/cau-hinh"},
            // Công cụ kiểm tra cấu hình Gmail mở trực tiếp trên trình duyệt (HUONG_DAN_CHAY.md); chỉ gửi tới email của chính cửa hàng
            new String[] {"GET", "/api/phieu-giam-gia/thu-gui-mail"});

    /** Nhân viên dùng được với MỌI phương thức. */
    private static final List<String> NHAN_VIEN_MOI_THAO_TAC = List.of(
            "/api/auth",
            "/api/ban-hang",
            "/api/hoa-don",
            "/api/khach-hang",
            "/api/dia-chi",
            "/api/giao-ca",
            "/api/ho-tro",
            "/api/chat",
            "/api/tro-ly",
            "/api/hinh-anh/upload",
            "/api/lich-lam-viec/cua-toi");

    /** Nhân viên chỉ được XEM (GET). */
    private static final List<String> NHAN_VIEN_CHI_XEM = List.of(
            "/api/san-pham",
            "/api/bien-the-san-pham",
            "/api/thuoc-tinh",
            "/api/giam-gia",
            "/api/dot-giam-gia",
            "/api/dot-giam-gia-chi-tiet",
            "/api/phieu-giam-gia",
            "/api/vai-tro",
            "/api/ca-lam-viec");

    /** Ngoại lệ chỉ Quản lý (dù nằm trong nhóm nhân viên dùng được ở trên). */
    private static final List<String[]> CHI_QUAN_LY = List.<String[]>of(
            new String[] {"DELETE", "/api/khach-hang"});

    /**
     * Nhân viên CHƯA VÀO CA chỉ gọi được các API này (đăng nhập / đăng xuất, vào ca, xem lịch của mình).
     * Mọi chức năng khác bắt buộc vào ca trước (cô: "nhân viên không bấm vào ca thì vẫn làm được à?").
     */
    private static final List<String> KHONG_CAN_VAO_CA = List.of(
            "/api/auth",
            "/api/giao-ca",
            "/api/lich-lam-viec/cua-toi",
            "/api/ca-lam-viec");

    /** Khách hàng đăng nhập (trang bán online) dùng được. */
    private static final List<String> KHACH_HANG = List.of("/api/auth", "/api/khach");

    /** true nếu path là `goc` hoặc nằm dưới `goc/` (không nhầm /api/hoa-don với /api/hoa-donxyz). */
    static boolean thuoc(String path, String goc) {
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

    public static boolean congKhai(String method, String path) {
        String p = chuanHoa(path);
        String m = method == null ? "" : method.toUpperCase(Locale.ROOT);
        if (!p.startsWith("/api/")) return true; // ngoài API (trang tĩnh...) không thuộc phạm vi kiểm tra
        for (String g : CONG_KHAI) if (thuoc(p, g)) return true;
        for (String[] r : CONG_KHAI_THEO_PHUONG_THUC) if (r[0].equals(m) && thuoc(p, r[1])) return true;
        return false;
    }

    /** Người dùng `nd` có được gọi API này không. */
    public static boolean duocPhep(NguoiDung nd, String method, String path) {
        if (nd == null) return false;
        String p = chuanHoa(path);
        String m = method == null ? "" : method.toUpperCase(Locale.ROOT);
        if (nd.laKhachHang()) {
            for (String g : KHACH_HANG) if (thuoc(p, g)) return true;
            return false;
        }
        if (nd.laQuanLy()) return true;
        if (!nd.laNhanVien()) return false;
        for (String[] r : CHI_QUAN_LY) if (r[0].equals(m) && thuoc(p, r[1])) return false;
        for (String g : NHAN_VIEN_MOI_THAO_TAC) if (thuoc(p, g)) return true;
        if ("GET".equals(m)) {
            for (String g : NHAN_VIEN_CHI_XEM) if (thuoc(p, g)) return true;
        }
        return false;
    }

    /** true nếu `nd` là nhân viên (không phải quản lý) và API này đòi phải đang trong ca. */
    public static boolean canVaoCa(NguoiDung nd, String method, String path) {
        if (nd == null || !nd.laNhanVien() || nd.laQuanLy()) return false;
        if (congKhai(method, path)) return false;
        String p = chuanHoa(path);
        for (String g : KHONG_CAN_VAO_CA) if (thuoc(p, g)) return false;
        return true;
    }

    /** Câu báo khi không đủ quyền. */
    public static String lyDoTuChoi(NguoiDung nd) {
        if (nd != null && nd.laKhachHang()) return "Tài khoản khách hàng không dùng được chức năng quản trị.";
        return "Chức năng này chỉ dành cho Quản lý. Tài khoản của bạn không đủ quyền.";
    }
}
