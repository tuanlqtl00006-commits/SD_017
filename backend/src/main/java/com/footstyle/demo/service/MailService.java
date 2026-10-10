package com.footstyle.demo.service;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gửi mail phiếu giảm giá cho khách hàng (chỉ phiếu CÁ NHÂN). Có 3 loại mail:
 *  - MOI      : khách mới được tặng phiếu;
 *  - CAP_NHAT : khách vẫn được tặng nhưng thông tin phiếu đã đổi (giá trị giảm, đơn tối thiểu, ngày...);
 *  - HUY      : khách bị bỏ khỏi danh sách nhận phiếu -> phiếu bị thu hồi.
 *
 * Chưa cấu hình SMTP (spring.mail.host) thì không gửi mail thật mà chỉ ghi nội dung vào log, để chạy thử không bị lỗi.
 * Mail gửi ở luồng riêng nên không làm chậm / làm hỏng thao tác lưu phiếu.
 */
@Service
public class MailService {

    public enum Loai { MOI, CAP_NHAT, HUY }

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String tenCuaHang;
    private final ExecutorService luongGui = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "gui-mail");
        t.setDaemon(true);
        return t;
    });

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.from:}") String from,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${app.mail.ten-cua-hang:FootStyle}") String tenCuaHang) {
        this.mailSender = mailSender;
        this.from = (from == null || from.isBlank()) ? username : from;
        this.tenCuaHang = tenCuaHang;
    }

    @PreDestroy
    void dong() {
        luongGui.shutdown();
    }

    /** Khách có email hợp lệ thì mới gửi được. */
    public static boolean coEmail(KhachHang kh) {
        return kh != null && kh.getEmail() != null && kh.getEmail().contains("@");
    }

    /** Xếp mail vào hàng đợi gửi (không chờ gửi xong). */
    public void guiPhieuGiamGia(Loai loai, PhieuGiamGia phieu, KhachHang kh) {
        if (!coEmail(kh)) {
            return;
        }
        final String den = kh.getEmail().trim();
        final String tieuDe = tieuDe(loai, phieu);
        final String noiDung = noiDung(loai, phieu, kh);
        luongGui.submit(() -> gui(den, tieuDe, noiDung));
    }

    private void gui(String den, String tieuDe, String noiDung) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("[MAIL GIẢ LẬP - chưa cấu hình SMTP] Đến: {} | Tiêu đề: {}\n{}", den, tieuDe, noiDung);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                m.setFrom(from);
            }
            m.setTo(den);
            m.setSubject(tieuDe);
            m.setText(noiDung);
            sender.send(m);
            log.info("Đã gửi mail '{}' đến {}", tieuDe, den);
        } catch (Exception e) {
            // Gửi lỗi (sai mật khẩu, mất mạng...) thì chỉ ghi log, không ảnh hưởng dữ liệu phiếu đã lưu
            log.warn("Không gửi được mail '{}' đến {}: {}", tieuDe, den, e.getMessage());
        }
    }

    /* ===================== Nội dung mail ===================== */

    private String tieuDe(Loai loai, PhieuGiamGia p) {
        switch (loai) {
            case MOI:
                return "[" + tenCuaHang + "] Bạn nhận được phiếu giảm giá " + p.getMaPhieu();
            case CAP_NHAT:
                return "[" + tenCuaHang + "] Phiếu giảm giá " + p.getMaPhieu() + " đã được cập nhật";
            default:
                return "[" + tenCuaHang + "] Phiếu giảm giá " + p.getMaPhieu() + " đã bị hủy";
        }
    }

    private String noiDung(Loai loai, PhieuGiamGia p, KhachHang kh) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chào ").append(kh.getHoTen() == null ? "quý khách" : kh.getHoTen()).append(",\n\n");
        switch (loai) {
            case MOI:
                sb.append("Cửa hàng ").append(tenCuaHang).append(" tặng bạn một phiếu giảm giá. Thông tin phiếu:\n\n");
                break;
            case CAP_NHAT:
                sb.append("Thông tin phiếu giảm giá của bạn đã được cập nhật. Vui lòng xem thông tin MỚI bên dưới (thay cho thông tin cũ):\n\n");
                break;
            default:
                sb.append("Rất tiếc, phiếu giảm giá dưới đây của bạn đã bị hủy và không còn sử dụng được:\n\n");
                break;
        }
        sb.append("- Mã phiếu: ").append(p.getMaPhieu()).append('\n');
        sb.append("- Tên phiếu: ").append(p.getTenPhieu()).append('\n');
        if (loai != Loai.HUY) {
            sb.append("- Mức giảm: ").append(mucGiam(p)).append('\n');
            sb.append("- Đơn tối thiểu: ").append(tien(p.getGiaTriDonHangToiThieu())).append('\n');
            sb.append("- Hiệu lực: ")
                    .append(p.getNgayBatDau() == null ? "" : p.getNgayBatDau().format(NGAY))
                    .append(" - ")
                    .append(p.getNgayKetThuc() == null ? "" : p.getNgayKetThuc().format(NGAY)).append('\n');
        }
        sb.append("\nCảm ơn bạn đã đồng hành cùng ").append(tenCuaHang).append(".\n");
        return sb.toString();
    }

    private String mucGiam(PhieuGiamGia p) {
        boolean phanTram = p.getLoaiGiam() != null && p.getLoaiGiam() == PhieuGiamGia.LOAI_PHAN_TRAM;
        if (!phanTram) {
            return "giảm " + tien(p.getGiaTriGiam());
        }
        String s = "giảm " + p.getGiaTriGiam() + "%";
        if (p.getGiamToiDa() != null) {
            s += " (tối đa " + tien(p.getGiamToiDa()) + ")";
        }
        return s;
    }

    private String tien(Long v) {
        long x = v == null ? 0 : v;
        return String.format(new Locale("vi", "VN"), "%,d ₫", x);
    }
}
