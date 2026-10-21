package com.footstyle.demo.security;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn mọi /api/** (trừ đường dẫn công khai trong PhanQuyen): đọc token "Authorization: Bearer ...", kiểm tra chữ ký / hạn dùng,
 * nạp lại người dùng từ CSDL (đã ngừng hoạt động hoặc đổi mật khẩu thì token cũ bị từ chối ngay), rồi kiểm tra quyền.
 * Thiếu / sai token -> 401; đủ token nhưng không đủ quyền -> 403. Lỗi trả về dạng { message, code } như các lỗi khác.
 *
 * app.auth.bat=false: bỏ qua đăng nhập (mọi request chạy với quyền của quản lý đầu tiên đang hoạt động) - dùng khi đang phát triển phần khác.
 */
@Component
public class XacThucInterceptor implements HandlerInterceptor {

    /** Kết quả xác thực một request: status 0 = cho qua (nguoiDung có thể null với đường dẫn công khai). */
    public record KetQua(int status, String code, String message, NguoiDung nguoiDung) {
        static KetQua choQua(NguoiDung nd) {
            return new KetQua(0, null, null, nd);
        }
    }

    private final TokenXacThuc token;
    private final NhanVienRepository nhanVienRepo;
    private final KhachHangRepository khachHangRepo;
    private final boolean batDangNhap;

    public XacThucInterceptor(TokenXacThuc token, NhanVienRepository nhanVienRepo, KhachHangRepository khachHangRepo,
                              @Value("${app.auth.bat:true}") boolean batDangNhap) {
        this.token = token;
        this.nhanVienRepo = nhanVienRepo;
        this.khachHangRepo = khachHangRepo;
        this.batDangNhap = batDangNhap;
    }

    public boolean batDangNhap() {
        return batDangNhap;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) path = path.substring(ctx.length());
        KetQua kq = xacThuc(request.getMethod(), path, request.getHeader("Authorization"));
        if (kq.status() != 0) {
            response.setStatus(kq.status());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter w = response.getWriter();
            w.write("{\"message\":\"" + json(kq.message()) + "\",\"code\":\"" + kq.code() + "\"}");
            w.flush();
            return false;
        }
        XacThuc.dat(kq.nguoiDung());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        XacThuc.xoa();
    }

    /** Toàn bộ logic xác thực + phân quyền (tách riêng để test không cần servlet). */
    public KetQua xacThuc(String method, String path, String authorization) {
        if ("OPTIONS".equalsIgnoreCase(method)) return KetQua.choQua(null); // CORS preflight
        boolean congKhai = PhanQuyen.congKhai(method, path);
        if (!batDangNhap) {
            return KetQua.choQua(quanLyMacDinh());
        }
        NguoiDung nd = null;
        String loi = null;
        String code = "CHUA_DANG_NHAP";
        String chuoi = layToken(authorization);
        if (chuoi != null) {
            TokenXacThuc.KetQua doc = token.doc(chuoi);
            if (!doc.hopLe()) {
                code = doc.loi() == TokenXacThuc.Loi.HET_HAN ? "HET_PHIEN" : "TOKEN_KHONG_HOP_LE";
                loi = doc.loi() == TokenXacThuc.Loi.HET_HAN
                        ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                        : "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.";
            } else {
                nd = napNguoiDung(doc.thongTin());
                if (nd == null) {
                    code = "HET_PHIEN";
                    loi = "Tài khoản đã ngừng hoạt động hoặc vừa đổi mật khẩu. Vui lòng đăng nhập lại.";
                }
            }
        }
        if (congKhai) return KetQua.choQua(nd); // công khai: có token hợp lệ thì vẫn biết ai đang gọi
        if (nd == null) {
            return new KetQua(401, code, loi != null ? loi : "Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.", null);
        }
        if (!PhanQuyen.duocPhep(nd, method, path)) {
            return new KetQua(403, "KHONG_DU_QUYEN", PhanQuyen.lyDoTuChoi(nd), nd);
        }
        return KetQua.choQua(nd);
    }

    static String layToken(String authorization) {
        if (authorization == null) return null;
        String a = authorization.trim();
        if (a.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String t = a.substring(7).trim();
            return t.isEmpty() ? null : t;
        }
        return null;
    }

    /** Nạp lại người dùng của token; null nếu không còn hợp lệ (bị xóa, ngừng hoạt động, đổi mật khẩu). */
    NguoiDung napNguoiDung(TokenXacThuc.ThongTin tt) {
        if (NguoiDung.LOAI_NHAN_VIEN.equals(tt.loai())) {
            Optional<NhanVien> nv = nhanVienRepo.findById(tt.id());
            if (nv.isEmpty()) return null;
            NhanVien n = nv.get();
            if (!Integer.valueOf(NhanVien.HOAT_DONG).equals(n.getTrangThai())) return null;
            if (!TokenXacThuc.vanTay(n.getMatKhau()).equals(tt.vanTay())) return null;
            return tuNhanVien(n);
        }
        if (NguoiDung.LOAI_KHACH_HANG.equals(tt.loai())) {
            Optional<KhachHang> kh = khachHangRepo.findById(tt.id());
            if (kh.isEmpty()) return null;
            KhachHang k = kh.get();
            if (!Integer.valueOf(1).equals(k.getTrangThai())) return null;
            if (!TokenXacThuc.vanTay(k.getMatKhau()).equals(tt.vanTay())) return null;
            return tuKhachHang(k);
        }
        return null;
    }

    public static NguoiDung tuNhanVien(NhanVien n) {
        boolean ql = n.getVaiTro() != null && n.getVaiTro().getTenVaiTro() != null
                && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(n.getVaiTro().getTenVaiTro().trim());
        return new NguoiDung(NguoiDung.LOAI_NHAN_VIEN, n.getId(), n.getMaNhanVien(), n.getHoTen(),
                ql ? NguoiDung.QUAN_LY : NguoiDung.NHAN_VIEN, n.getEmail());
    }

    public static NguoiDung tuKhachHang(KhachHang k) {
        return new NguoiDung(NguoiDung.LOAI_KHACH_HANG, k.getId(), k.getMaKhachHang(), k.getHoTen(), NguoiDung.KHACH_HANG, k.getEmail());
    }

    /** Khi tắt đăng nhập: dùng quản lý đang hoạt động có id nhỏ nhất (để giao ca, bán hàng vẫn ghi đúng người). */
    private NguoiDung quanLyMacDinh() {
        return nhanVienRepo.findAll().stream()
                .filter(n -> Integer.valueOf(NhanVien.HOAT_DONG).equals(n.getTrangThai()))
                .filter(n -> n.getVaiTro() != null && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(String.valueOf(n.getVaiTro().getTenVaiTro()).trim()))
                .min(Comparator.comparing(NhanVien::getId))
                .map(XacThucInterceptor::tuNhanVien)
                .orElse(new NguoiDung(NguoiDung.LOAI_NHAN_VIEN, null, "QL", "Quản lý", NguoiDung.QUAN_LY, null));
    }

    private static String json(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
