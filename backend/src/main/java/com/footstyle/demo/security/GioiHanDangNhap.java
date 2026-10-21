package com.footstyle.demo.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Chống dò mật khẩu: nhập sai SO_LAN_SAI_TOI_DA lần liên tiếp thì khóa đăng nhập tài khoản đó THOI_GIAN_KHOA.
 * Đếm theo tài khoản gõ vào (không phân biệt hoa thường). Đăng nhập đúng thì xóa bộ đếm. Lưu trong bộ nhớ (khởi động lại backend là hết khóa).
 */
@Component
public class GioiHanDangNhap {

    public static final int SO_LAN_SAI_TOI_DA = 5;
    public static final Duration THOI_GIAN_KHOA = Duration.ofMinutes(5);
    /** Không giữ quá nhiều tài khoản trong bộ nhớ (bị dò hàng loạt tài khoản lạ). */
    private static final int SO_TAI_KHOAN_TOI_DA = 10_000;

    private static final class TrangThai {
        int soLanSai;
        Instant khoaDen;
        Instant lanCuoi;
    }

    private final Map<String, TrangThai> theoTaiKhoan = new ConcurrentHashMap<>();
    private Clock dongHo = Clock.systemDefaultZone();

    /** Dùng trong test để chỉnh giờ. */
    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private static String khoa(String taiKhoan) {
        return taiKhoan == null ? "" : taiKhoan.trim().toLowerCase(Locale.ROOT);
    }

    /** Số giây còn bị khóa (0 = không bị khóa). */
    public long conKhoaGiay(String taiKhoan) {
        TrangThai t = theoTaiKhoan.get(khoa(taiKhoan));
        if (t == null || t.khoaDen == null) return 0;
        long giay = Duration.between(dongHo.instant(), t.khoaDen).getSeconds();
        if (giay <= 0) {
            theoTaiKhoan.remove(khoa(taiKhoan)); // hết khóa: đếm lại từ đầu
            return 0;
        }
        return giay;
    }

    /** Ghi một lần sai; trả về số lần còn được thử (0 = vừa bị khóa). */
    public synchronized int ghiSai(String taiKhoan) {
        if (theoTaiKhoan.size() > SO_TAI_KHOAN_TOI_DA) donDep();
        TrangThai t = theoTaiKhoan.computeIfAbsent(khoa(taiKhoan), k -> new TrangThai());
        t.soLanSai++;
        t.lanCuoi = dongHo.instant();
        if (t.soLanSai >= SO_LAN_SAI_TOI_DA) {
            t.khoaDen = dongHo.instant().plus(THOI_GIAN_KHOA);
            return 0;
        }
        return SO_LAN_SAI_TOI_DA - t.soLanSai;
    }

    public void ghiDung(String taiKhoan) {
        theoTaiKhoan.remove(khoa(taiKhoan));
    }

    /** Bỏ các mục đã quá 1 giờ không thử lại. */
    private void donDep() {
        Instant moc = dongHo.instant().minus(Duration.ofHours(1));
        theoTaiKhoan.values().removeIf(t -> t.lanCuoi != null && t.lanCuoi.isBefore(moc) && (t.khoaDen == null || t.khoaDen.isBefore(dongHo.instant())));
    }
}
