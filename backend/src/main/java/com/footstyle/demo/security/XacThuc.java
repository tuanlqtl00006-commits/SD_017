package com.footstyle.demo.security;

import com.footstyle.demo.exception.ApiException;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Người dùng của request đang xử lý (XacThucInterceptor đặt vào đầu request, xóa khi xong).
 * Dùng ở controller / service: XacThuc.nhanVien() lấy nhân viên đang đăng nhập, không có thì báo 401.
 */
public final class XacThuc {

    private static final ThreadLocal<NguoiDung> HIEN_TAI = new ThreadLocal<>();

    private XacThuc() {
    }

    public static void dat(NguoiDung nguoiDung) {
        HIEN_TAI.set(nguoiDung);
    }

    public static void xoa() {
        HIEN_TAI.remove();
    }

    public static Optional<NguoiDung> hienTai() {
        return Optional.ofNullable(HIEN_TAI.get());
    }

    /** Nhân viên / quản lý đang đăng nhập (có id thật trong bảng nhan_vien). Không có thì 401. */
    public static NguoiDung nhanVien() {
        NguoiDung nd = HIEN_TAI.get();
        if (nd == null || !nd.laNhanVien() || nd.id() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập bằng tài khoản nhân viên để dùng chức năng này.");
        }
        return nd;
    }

    public static boolean laQuanLy() {
        NguoiDung nd = HIEN_TAI.get();
        return nd != null && nd.laQuanLy();
    }

    /** Tên ghi vào lịch sử thao tác: người đang đăng nhập; chưa có (vd test) thì dùng tên truyền vào. */
    public static String tenNguoiThaoTac(String macDinh) {
        NguoiDung nd = HIEN_TAI.get();
        if (nd != null && nd.hoTen() != null && !nd.hoTen().isBlank()) return nd.hoTen().trim();
        return macDinh;
    }
}
