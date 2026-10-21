package com.footstyle.demo.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Token đăng nhập tự ký bằng HMAC-SHA256 (thư viện có sẵn của JDK, không cần thêm thư viện JWT):
 *   base64url("v1|loai|id|vanTay|hetHan") + "." + base64url(HMAC(phần trước))
 * - loai: NV (nhân viên) / KH (khách hàng); id: id trong bảng; hetHan: giây (epoch).
 * - vanTay: 12 ký tự đầu của SHA-256(mật khẩu đã băm) -> đổi mật khẩu thì mọi token cũ tự hết hiệu lực.
 * Ai sửa một ký tự trong token thì chữ ký không còn khớp nên bị từ chối.
 */
public class TokenXacThuc {

    /** Lý do token không dùng được. */
    public enum Loi { THIEU, SAI_DINH_DANG, SAI_CHU_KY, HET_HAN }

    public record ThongTin(String loai, int id, String vanTay, long hetHan) {
    }

    /** Kết quả đọc token: thongTin != null khi hợp lệ, ngược lại loi cho biết vì sao. */
    public record KetQua(ThongTin thongTin, Loi loi) {
        public boolean hopLe() {
            return thongTin != null;
        }
    }

    private static final String PHIEN_BAN = "v1";
    private static final Base64.Encoder MA_HOA = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder GIAI_MA = Base64.getUrlDecoder();

    private final byte[] khoa;
    private final Duration thoiHan;
    private final Clock dongHo;

    public TokenXacThuc(String biMat, Duration thoiHan, Clock dongHo) {
        if (biMat == null || biMat.length() < 16) {
            throw new IllegalArgumentException("Chuỗi bí mật ký token (app.auth.bi-mat) phải dài ít nhất 16 ký tự.");
        }
        this.khoa = biMat.getBytes(StandardCharsets.UTF_8);
        this.thoiHan = thoiHan;
        this.dongHo = dongHo;
    }

    public Duration thoiHan() {
        return thoiHan;
    }

    /** Thời điểm hết hạn của token tạo ra ngay bây giờ. */
    public Instant hetHanNeuTaoBayGio() {
        return dongHo.instant().plus(thoiHan);
    }

    public String tao(String loai, int id, String matKhauDaBam) {
        long hetHan = dongHo.instant().plus(thoiHan).getEpochSecond();
        String duLieu = PHIEN_BAN + "|" + loai + "|" + id + "|" + vanTay(matKhauDaBam) + "|" + hetHan;
        String phan1 = MA_HOA.encodeToString(duLieu.getBytes(StandardCharsets.UTF_8));
        return phan1 + "." + MA_HOA.encodeToString(ky(phan1));
    }

    public KetQua doc(String token) {
        if (token == null || token.isBlank()) return new KetQua(null, Loi.THIEU);
        String t = token.trim();
        int cham = t.indexOf('.');
        if (cham <= 0 || cham != t.lastIndexOf('.') || cham == t.length() - 1) return new KetQua(null, Loi.SAI_DINH_DANG);
        String phan1 = t.substring(0, cham);
        byte[] chuKy;
        String duLieu;
        try {
            chuKy = GIAI_MA.decode(t.substring(cham + 1));
            duLieu = new String(GIAI_MA.decode(phan1), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return new KetQua(null, Loi.SAI_DINH_DANG);
        }
        // So chữ ký với thời gian không đổi (không để lộ đúng được bao nhiêu byte)
        if (!MessageDigest.isEqual(ky(phan1), chuKy)) return new KetQua(null, Loi.SAI_CHU_KY);
        String[] p = duLieu.split("\\|", -1);
        if (p.length != 5 || !PHIEN_BAN.equals(p[0])) return new KetQua(null, Loi.SAI_DINH_DANG);
        long hetHan;
        int id;
        try {
            id = Integer.parseInt(p[2]);
            hetHan = Long.parseLong(p[4]);
        } catch (NumberFormatException e) {
            return new KetQua(null, Loi.SAI_DINH_DANG);
        }
        if (dongHo.instant().getEpochSecond() >= hetHan) return new KetQua(null, Loi.HET_HAN);
        return new KetQua(new ThongTin(p[1], id, p[3], hetHan), null);
    }

    private byte[] ky(String noiDung) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(khoa, "HmacSHA256"));
            return mac.doFinal(noiDung.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Không ký được token", e);
        }
    }

    /** 12 ký tự hex đầu của SHA-256(mật khẩu đã băm); chưa có mật khẩu thì "-". */
    public static String vanTay(String matKhauDaBam) {
        if (matKhauDaBam == null || matKhauDaBam.isEmpty()) return "-";
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(matKhauDaBam.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(h, 0, 6); // 6 byte đầu = 12 ký tự hex
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
