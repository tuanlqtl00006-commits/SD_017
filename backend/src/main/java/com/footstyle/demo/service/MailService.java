package com.footstyle.demo.service;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;

import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
=======

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

=======
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
=======
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


/**
 * Gửi mail phiếu giảm giá cho khách hàng (chỉ phiếu CÁ NHÂN). Có 3 loại mail:
 *  - MOI      : khách mới được tặng phiếu;
 *  - CAP_NHAT : khách vẫn được tặng nhưng thông tin phiếu đã đổi (giá trị giảm, đơn tối thiểu, ngày...);
 *  - HUY      : khách bị bỏ khỏi danh sách nhận phiếu -> phiếu bị thu hồi.
 *

 * Nội dung mail là HTML dựng từ mẫu Thymeleaf trong resources/templates/mail (pgg-moi / pgg-cap-nhat / pgg-huy,
 * phần dùng chung ở fragments.html), có gắn kèm logo FootStyle (resources/mail/logo.png).
 *
 * Chưa cấu hình tài khoản gửi (MAIL_USERNAME) thì không gửi mail thật mà chỉ ghi nội dung vào log, để chạy thử không bị lỗi.
=======
 * Chưa cấu hình SMTP (spring.mail.host) thì không gửi mail thật mà chỉ ghi nội dung vào log, để chạy thử không bị lỗi.

 * Mail gửi ở luồng riêng nên không làm chậm / làm hỏng thao tác lưu phiếu.
 */
@Service
public class MailService {

    public enum Loai { MOI, CAP_NHAT, HUY }

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Logo gắn kèm trong mail; trong mẫu HTML gọi bằng src="cid:footstyleLogo"
    private static final String LOGO_CID = "footstyleLogo";
    private static final ClassPathResource LOGO = new ClassPathResource("mail/logo.png");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ObjectProvider<TemplateEngine> templateEngine;
    private final String from;
    private final String username;
    private final String tenCuaHang;
    private final String shopUrl;
=======

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String tenCuaHang;

    private final ExecutorService luongGui = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "gui-mail");
        t.setDaemon(true);
        return t;
    });

    public MailService(ObjectProvider<JavaMailSender> mailSender,

                       ObjectProvider<TemplateEngine> templateEngine,
                       @Value("${app.mail.from:}") String from,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${app.mail.ten-cua-hang:FootStyle}") String tenCuaHang,
                       @Value("${app.shop-url:http://localhost:5173}") String shopUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.username = username == null ? "" : username.trim();
        this.from = (from == null || from.isBlank()) ? this.username : from.trim();
        this.tenCuaHang = tenCuaHang;
        this.shopUrl = shopUrl;

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

        final String noiDungChu = noiDungChu(loai, phieu, kh);   // bản chữ thường: ghi log khi giả lập
        final String noiDungHtml = noiDungHtml(loai, phieu, kh); // bản HTML có giao diện: gửi thật
        luongGui.submit(() -> gui(den, tieuDe, noiDungChu, noiDungHtml));
    }

    private void gui(String den, String tieuDe, String noiDungChu, String noiDungHtml) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || username.isBlank()) {
            log.info("[MAIL GIẢ LẬP - chưa cấu hình MAIL_USERNAME / MAIL_PASSWORD] Đến: {} | Tiêu đề: {}\n{}", den, tieuDe, noiDungChu);
            return;
        }
        try {
            MimeMessage m = sender.createMimeMessage();
            // multipart = true để gắn được logo vào nội dung mail
            MimeMessageHelper h = new MimeMessageHelper(m, true, StandardCharsets.UTF_8.name());
            if (from != null && !from.isBlank()) {
                h.setFrom(from, tenCuaHang);
            }
            h.setTo(den);
            h.setSubject(tieuDe);
            if (noiDungHtml != null) {
                h.setText(noiDungChu, noiDungHtml); // kèm bản chữ thường cho ứng dụng mail không hiện được HTML
                if (LOGO.exists()) {
                    h.addInline(LOGO_CID, LOGO, "image/png"); // phải gọi SAU setText
                }
            } else {
                h.setText(noiDungChu, false);
            }
=======
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

                return "🎁 Tặng bạn phiếu giảm giá đặc biệt từ " + tenCuaHang + "!";
            case CAP_NHAT:
                return "🔔 Phiếu giảm giá " + p.getMaPhieu() + " của bạn vừa được cập nhật";
            default:
                return "Thông báo về phiếu giảm giá " + p.getMaPhieu() + " từ " + tenCuaHang;
        }
    }

    /** Dựng mail HTML từ mẫu. Lỗi dựng mẫu thì trả về null để vẫn gửi được bản chữ thường. */
    private String noiDungHtml(Loai loai, PhieuGiamGia p, KhachHang kh) {
        TemplateEngine engine = templateEngine.getIfAvailable();
        if (engine == null) {
            return null;
        }
        try {
            Context ctx = new Context(new Locale("vi", "VN"));
            ctx.setVariable("tenKhach", kh.getHoTen() == null ? "quý khách" : kh.getHoTen());
            ctx.setVariable("ma", p.getMaPhieu());
            ctx.setVariable("ten", p.getTenPhieu());
            ctx.setVariable("mucGiam", phanTram(p) ? p.getGiaTriGiam() + "%" : tien(p.getGiaTriGiam()));
            ctx.setVariable("giamToiDa", phanTram(p) && p.getGiamToiDa() != null ? tien(p.getGiamToiDa()) : null);
            ctx.setVariable("donToiThieu", p.getGiaTriDonHangToiThieu() == null || p.getGiaTriDonHangToiThieu() == 0
                    ? "Mọi đơn hàng" : tien(p.getGiaTriDonHangToiThieu()));
            ctx.setVariable("tuNgay", ngay(p.getNgayBatDau(), NGAY_GIO));  // vd 16/08/2026 00:00
            ctx.setVariable("denNgay", ngay(p.getNgayKetThuc(), NGAY));    // "đến hết ngày" 20/08/2026
            ctx.setVariable("shopUrl", shopUrl);
            String mau = switch (loai) {
                case MOI -> "mail/pgg-moi";
                case CAP_NHAT -> "mail/pgg-cap-nhat";
                case HUY -> "mail/pgg-huy";
            };
            return engine.process(mau, ctx);
        } catch (Exception e) {
            log.warn("Không dựng được mẫu mail HTML ({}), gửi bản chữ thường: {}", loai, e.getMessage());
            return null;
        }
    }

    private String noiDungChu(Loai loai, PhieuGiamGia p, KhachHang kh) {
=======
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

                    .append(ngay(p.getNgayBatDau(), NGAY))
                    .append(" - ")
                    .append(ngay(p.getNgayKetThuc(), NGAY)).append('\n');
=======
                    .append(p.getNgayBatDau() == null ? "" : p.getNgayBatDau().format(NGAY))
                    .append(" - ")
                    .append(p.getNgayKetThuc() == null ? "" : p.getNgayKetThuc().format(NGAY)).append('\n');

        }
        sb.append("\nCảm ơn bạn đã đồng hành cùng ").append(tenCuaHang).append(".\n");
        return sb.toString();
    }


    private static boolean phanTram(PhieuGiamGia p) {
        return p.getLoaiGiam() != null && p.getLoaiGiam() == PhieuGiamGia.LOAI_PHAN_TRAM;
    }

    private String mucGiam(PhieuGiamGia p) {
        if (!phanTram(p)) {
=======
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


    private static String ngay(LocalDateTime t, DateTimeFormatter dinhDang) {
        return t == null ? "" : t.format(dinhDang);
    }

    /** 200000 -> "200.000 ₫" */
=======

    private String tien(Long v) {
        long x = v == null ? 0 : v;
        return String.format(new Locale("vi", "VN"), "%,d ₫", x);
    }
}
