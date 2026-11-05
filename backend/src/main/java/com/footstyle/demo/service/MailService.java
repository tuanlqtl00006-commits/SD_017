package com.footstyle.demo.service;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.service.PhieuMailNoiDung.Loai;
import com.footstyle.demo.service.PhieuMailNoiDung.ThuMail;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Gửi mail phiếu giảm giá cho khách hàng (chỉ phiếu CÁ NHÂN). Có 3 loại mail (xem PhieuMailNoiDung.Loai):
 *  - MOI      : khách mới được tặng phiếu;
 *  - CAP_NHAT : khách vẫn được tặng nhưng thông tin phiếu đã đổi (giá trị giảm, đơn tối thiểu, ngày...);
 *  - HUY      : khách bị bỏ khỏi danh sách nhận phiếu -> phiếu bị thu hồi.
 *
 * Thư gửi dạng HTML (kèm bản chữ cho máy đọc không hiển thị được HTML), nội dung do PhieuMailNoiDung dựng.
 * Chưa cấu hình Gmail (host + tài khoản + mật khẩu ứng dụng, xem mail.properties) thì không gửi mail thật mà chỉ ghi nội dung vào log,
 * để chạy thử không bị lỗi; khi đó giao diện được báo rõ là CHƯA gửi (xem duocCauHinh()).
 * Mail gửi ở luồng riêng nên không làm chậm / làm hỏng thao tác lưu phiếu.
 *
 * Để giao diện BÁO RÕ khi gửi lỗi (sai mật khẩu ứng dụng, mất mạng...): mỗi lần lưu phiếu là một "lô" thư (batDauLo),
 * từng thư gửi xong / lỗi được đếm lại theo mã phiếu (ketQuaLo); trang phiếu giảm giá hỏi lại vài giây sau khi lưu
 * (GET /api/phieu-giam-gia/{id}/ket-qua-mail) và hiện "Đã gửi 3/3 mail" hoặc "Gửi mail lỗi: <lý do>".
 * Kết quả đăng nhập thử Gmail lúc khởi động cũng được giữ lại (trangThai) để form phiếu cá nhân cảnh báo trước khi lưu.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String DUONG_DAN_LOGO = "static/logo.jpg";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String username;
    private final String password;
    private final String tenCuaHang;
    private final String linkCuaHang;
    private final PhieuMailNoiDung noiDung;
    private final NhanVienMailNoiDung noiDungNhanVien;
    // Nội dung ảnh logo đọc sẵn vào bộ nhớ lúc khởi động; null: không tìm thấy / không đọc được -> thư không có logo
    byte[] logo;
    /** Kết quả đăng nhập thử Gmail lúc khởi động: CHUA_CAU_HINH / DANG_KIEM_TRA / THANH_CONG / THAT_BAI. */
    private volatile String dangNhap = "DANG_KIEM_TRA";
    private volatile String lyDoDangNhap = "";
    /** Lô thư gần nhất của từng phiếu (theo mã phiếu). */
    private final Map<String, LoThu> loThu = new ConcurrentHashMap<>();
    private final ExecutorService luongGui = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r, "gui-mail");
        t.setDaemon(true);
        return t;
    });

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.from:}") String from,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${spring.mail.password:}") String password,
                       @Value("${app.mail.ten-cua-hang:FootStyle}") String tenCuaHang,
                       @Value("${app.mail.link-cua-hang:}") String linkCuaHang) {
        this.mailSender = mailSender;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password.trim();
        this.from = (from == null || from.isBlank()) ? this.username : from;
        this.tenCuaHang = (tenCuaHang == null || tenCuaHang.isBlank()) ? "FootStyle" : tenCuaHang.trim();
        this.linkCuaHang = (linkCuaHang != null && (linkCuaHang.trim().startsWith("http://") || linkCuaHang.trim().startsWith("https://")))
                ? linkCuaHang.trim() : null;
        this.noiDung = new PhieuMailNoiDung(this.tenCuaHang, linkCuaHang);
        this.noiDungNhanVien = new NhanVienMailNoiDung(this.tenCuaHang, linkCuaHang);
        this.logo = docLogo(new ClassPathResource(DUONG_DAN_LOGO));
    }

    /** Đọc ảnh logo; không có hoặc đọc lỗi thì trả null (thư vẫn gửi bình thường, chỉ không có logo). */
    static byte[] docLogo(Resource r) {
        try {
            if (!r.exists()) {
                log.info("Không tìm thấy logo {} nên mail sẽ gửi không kèm logo.", r);
                return null;
            }
            try (InputStream in = r.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("Không đọc được logo, mail sẽ gửi không kèm logo: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Đã cấu hình đủ để gửi mail THẬT chưa: có bộ gửi SMTP (spring.mail.host) và đã điền cả email lẫn mật khẩu ứng dụng.
     * Thiếu một trong các mục này thì mail chỉ được ghi vào log (xem gui()).
     */
    public boolean duocCauHinh() {
        return mailSender != null && mailSender.getIfAvailable() != null && !username.isBlank() && !password.isBlank();
    }

    /** Ghi vào log lúc khởi động để biết ngay mail thật có bật không (đỡ phải đoán vì sao không thấy thư). */
    @PostConstruct
    void baoTrangThai() {
        if (duocCauHinh()) {
            chuanHoaTaiKhoan();
            log.info("Gửi mail: ĐÃ cấu hình, thư sẽ gửi thật từ tài khoản {}.", username);
            thuDangNhapNen();
        } else {
            dangNhap = "CHUA_CAU_HINH";
            log.warn("Gửi mail: CHƯA cấu hình Gmail nên phiếu giảm giá cá nhân sẽ KHÔNG gửi mail thật (chỉ ghi nội dung vào log). "
                    + "Điền email + mật khẩu ứng dụng vào backend/src/main/resources/mail.properties rồi chạy lại backend.");
        }
    }

    /** Mật khẩu ứng dụng Google hiển thị dạng "abcd efgh ijkl mnop"; dán thừa dấu cách / xuống dòng thì Gmail báo 535, nên bỏ hết khoảng trắng. */
    static String lamSachMatKhau(String matKhau) {
        return matKhau == null ? "" : matKhau.replaceAll("\\s+", "");
    }

    /** Ghi lại email / mật khẩu đã làm sạch vào bộ gửi SMTP của Spring (bộ gửi tự nạp từ file giữ nguyên khoảng trắng thừa). */
    void chuanHoaTaiKhoan() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender instanceof JavaMailSenderImpl impl) {
            impl.setUsername(username);
            impl.setPassword(lamSachMatKhau(password));
        }
    }

    /** Lúc khởi động thử đăng nhập Gmail ở luồng nền (không làm chậm backend) và ghi kết quả thật vào log. */
    private void thuDangNhapNen() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!(sender instanceof JavaMailSenderImpl impl)) {
            return;
        }
        luongGui.submit(() -> {
            try {
                impl.testConnection();
                dangNhap = "THANH_CONG";
                lyDoDangNhap = "";
                log.info("Gmail: đăng nhập thử THÀNH CÔNG, sẵn sàng gửi thư thật.");
            } catch (Exception e) {
                dangNhap = "THAT_BAI";
                lyDoDangNhap = loiThat(e) + gopY(e);
                log.error("Gmail: đăng nhập thử THẤT BẠI, thư sẽ KHÔNG gửi được: {}{}", loiThat(e), gopY(e));
            }
        });
    }

    /**
     * Gửi NGAY (không qua hàng đợi) một thư thử đến chính tài khoản gửi, trả về câu báo kết quả kèm lỗi thật nếu có.
     * Dùng để kiểm tra cấu hình Gmail mà không cần tạo phiếu.
     */
    public String guiThuThu() {
        if (!duocCauHinh()) {
            return "CHƯA cấu hình Gmail: mở backend/src/main/resources/mail.properties, điền spring.mail.username (email gửi) "
                    + "và spring.mail.password (mật khẩu ứng dụng 16 ký tự) rồi chạy lại backend.";
        }
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            MimeMessage m = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(m, false, "UTF-8");
            helper.setFrom(from, tenCuaHang);
            helper.setTo(username);
            helper.setSubject("[" + tenCuaHang + "] Thư thử gửi mail");
            helper.setText("Nếu bạn đọc được thư này thì cấu hình Gmail của " + tenCuaHang + " đã đúng.");
            sender.send(m);
            return "Đã gửi thư thử đến " + username + ". Hãy kiểm tra hộp thư (cả mục Spam).";
        } catch (Exception e) {
            log.warn("Gửi thư thử lỗi: {}", loiThat(e), e);
            return "Gửi thư thử THẤT BẠI: " + loiThat(e) + gopY(e);
        }
    }

    /** Lấy câu lỗi gốc (Gmail trả về trong nguyên nhân sâu nhất) thay vì câu bọc ngoài của Spring. */
    static String loiThat(Throwable e) {
        Throwable goc = e;
        while (goc.getCause() != null && goc.getCause() != goc) {
            goc = goc.getCause();
        }
        String chu = goc.getMessage() == null ? goc.getClass().getSimpleName() : goc.getMessage();
        return chu.replace('\r', ' ').replace('\n', ' ').trim();
    }

    /** Gợi ý cách sửa cho vài lỗi hay gặp khi dùng Gmail. Xét cả tên lỗi lẫn nội dung của MỌI nguyên nhân bên trong. */
    static String gopY(Throwable e) {
        StringBuilder gom = new StringBuilder();
        for (Throwable t = e; t != null && gom.length() < 4000; t = t.getCause() == t ? null : t.getCause()) {
            gom.append(t.getClass().getSimpleName()).append(' ').append(t.getMessage()).append(' ');
        }
        String chu = gom.toString().toLowerCase(Locale.ROOT);
        if (chu.contains("535") || chu.contains("username and password not accepted") || chu.contains("authenticationfailed")
                || chu.contains("mailauthentication") || chu.contains("invalid credentials")) {
            return " -> Sai email / mật khẩu: Gmail KHÔNG nhận mật khẩu đăng nhập thường, phải dùng \"Mật khẩu ứng dụng\" 16 ký tự "
                    + "(bật xác minh 2 bước rồi tạo tại myaccount.google.com/apppasswords).";
        }
        if (chu.contains("unknownhost") || chu.contains("could not connect") || chu.contains("connectexception")
                || chu.contains("timed out") || chu.contains("timeout")) {
            return " -> Không kết nối được smtp.gmail.com:587: kiểm tra mạng / tường lửa / diệt virus đang chặn cổng 587.";
        }
        if (chu.contains("pkix") || chu.contains("certpath") || chu.contains("unable to find valid certification")) {
            return " -> Lỗi chứng chỉ SSL: thường do phần mềm diệt virus (Avast, Kaspersky, ESET...) đang quét / chèn vào kết nối mail. "
                    + "Tắt tính năng quét email / quét HTTPS của phần mềm đó (hoặc đổi mạng) rồi chạy lại backend.";
        }
        return "";
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
        final LoThu lo = loThu.computeIfAbsent(khoa(phieu.getMaPhieu()), k -> new LoThu());
        lo.themThuNeuChuaDatTruoc();
        // Dựng nội dung ngay bây giờ (chụp lại dữ liệu phiếu lúc này), gửi thì để luồng nền làm
        final byte[] logoGan = logo;
        final ThuMail thu;
        try {
            thu = noiDung.soan(loai, phieu, kh, logoGan != null);
        } catch (Exception e) {
            // Dựng thư lỗi thì chỉ ghi log, không làm hỏng thao tác lưu phiếu đã xong
            log.error("Không soạn được mail phiếu {} cho {}: {}", phieu.getMaPhieu(), den, e.toString(), e);
            lo.ghiLoi("Không soạn được thư cho " + den + ": " + loiThat(e));
            return;
        }
        luongGui.submit(() -> gui(den, thu, logoGan, lo));
    }

    /**
     * Xếp vào hàng đợi mail THÔNG BÁO TÀI KHOẢN cho một nhân viên mới (nhập hàng loạt từ Excel). Kết quả gửi được đếm vào lô `maLo`
     * (bắt đầu bằng batDauLo) để giao diện hỏi lại bằng ketQuaLo(maLo), giống mail phiếu giảm giá.
     */
    public void guiTaiKhoanNhanVien(String hoTen, String email, String maNhanVien, String vaiTro, String matKhauTam, String maLo) {
        final LoThu lo = loThu.computeIfAbsent(khoa(maLo), k -> new LoThu());
        lo.themThuNeuChuaDatTruoc();
        if (email == null || !email.contains("@")) {
            lo.ghiLoi("Nhân viên " + maNhanVien + " không có email hợp lệ.");
            return;
        }
        final String den = email.trim();
        final byte[] logoGan = logo;
        final ThuMail thu;
        try {
            thu = noiDungNhanVien.soan(hoTen, den, maNhanVien, vaiTro, matKhauTam, logoGan != null);
        } catch (Exception e) {
            log.error("Không soạn được mail tài khoản cho {}: {}", den, e.toString(), e);
            lo.ghiLoi("Không soạn được thư cho " + den + ": " + loiThat(e));
            return;
        }
        luongGui.submit(() -> gui(den, thu, logoGan, lo));
    }

    /**
     * Thư mật khẩu tạm khi nhân viên bấm "Quên mật khẩu" ở trang đăng nhập (gửi nền, không bắt người dùng chờ).
     * Chưa cấu hình Gmail thì nội dung thư (kèm mật khẩu tạm) được ghi vào log như các thư khác.
     */
    public void guiMatKhauTam(String hoTen, String email, String matKhauTam) {
        if (email == null || !email.contains("@")) return;
        final String den = email.trim();
        String ten = hoTen == null || hoTen.isBlank() ? "bạn" : hoTen.trim();
        String tieuDe = "[" + tenCuaHang + "] Mật khẩu tạm để đăng nhập";
        String banChu = "Xin chào " + ten + ",\n\n"
                + "Bạn (hoặc ai đó) vừa yêu cầu cấp lại mật khẩu tài khoản " + tenCuaHang + ".\n"
                + "Mật khẩu tạm: " + matKhauTam + "\n\n"
                + "Hãy đăng nhập bằng mật khẩu này rồi đổi mật khẩu ngay (menu tài khoản > Đổi mật khẩu).\n"
                + "Nếu không phải bạn yêu cầu, hãy báo cho quản lý cửa hàng.\n";
        final byte[] logoGan = logo;
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        String ten2 = esc(tenCuaHang);
        StringBuilder h = new StringBuilder(3500);
        moKhungHtml(h, tieuDe, "Mật khẩu tạm tài khoản " + tenCuaHang, "#0977ec", logoGan != null, "KHÔI PHỤC MẬT KHẨU", "Mật khẩu tạm để đăng nhập");
        h.append("<tr><td style=\"padding:16px 28px 8px 28px;").append(phong).append("font-size:15px;line-height:24px;color:#374151;\">")
                .append("Xin chào <strong>").append(esc(ten)).append("</strong>,<br>")
                .append("Bạn vừa yêu cầu cấp lại mật khẩu đăng nhập hệ thống <strong>").append(ten2).append("</strong>. Vui lòng sử dụng mật khẩu tạm bên dưới:</td></tr>\n");
        h.append("<tr><td style=\"padding:12px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#eff6ff;border:2px dashed #0977ec;border-radius:12px;\">")
                .append("<tr><td align=\"center\" style=\"padding:20px 16px;\">")
                .append("<div style=\"").append(phong).append("font-size:12px;color:#6b7280;letter-spacing:1px;font-weight:bold;\">MẬT KHẨU TẠM</div>")
                .append("<div style=\"font-family:'Courier New',Courier,monospace;font-size:28px;line-height:36px;letter-spacing:3px;font-weight:bold;color:#0977ec;margin-top:6px;\">")
                .append(esc(matKhauTam)).append("</div>")
                .append("</td></tr></table></td></tr>\n");
        h.append("<tr><td style=\"padding:8px 28px 16px 28px;").append(phong).append("font-size:14px;line-height:22px;color:#374151;\">")
                .append("Hãy đăng nhập bằng mật khẩu này rồi <strong>đổi mật khẩu ngay</strong> tại mục <em>Tài khoản &gt; Đổi mật khẩu</em>.<br>")
                .append("<span style=\"color:#b45309;\">Nếu không phải bạn yêu cầu, vui lòng báo ngay cho quản lý cửa hàng.</span></td></tr>\n");
        dongKhungHtml(h);
        final ThuMail thu = new ThuMail(tieuDe, banChu, h.toString());
        luongGui.submit(() -> gui(den, thu, logoGan));
    }

    /**
     * Gửi NGAY (chờ kết quả) một thư báo cáo HTML kèm ảnh nhúng kiểu CID (biểu đồ thống kê): anh = { cid -> PNG }.
     * Trả về true nếu gửi thật, false nếu chưa cấu hình Gmail (chỉ ghi log). Lỗi gửi thì ném ra để giao diện báo.
     */
    public boolean guiBaoCao(String den, String tieuDe, String banChu, String html, Map<String, byte[]> anh) throws Exception {
        JavaMailSender sender = mailSender != null ? mailSender.getIfAvailable() : null;
        if (sender == null || !duocCauHinh()) {
            log.warn("[MAIL GIẢ LẬP - CHƯA cấu hình Gmail nên KHÔNG gửi thật] Đến: {} | Tiêu đề: {}\n{}", den, tieuDe, banChu);
            return false;
        }
        return guiBaoCao(den, tieuDe, banChu, html, anh, null, null);
    }

    /**
     * Gửi thư báo cáo HTML kèm biểu đồ nhúng CID và tệp Excel (.xlsx) đính kèm đầy đủ nội dung.
     */
    public boolean guiBaoCao(String den, String tieuDe, String banChu, String html, Map<String, byte[]> anh,
                             String tenFileDinhKem, byte[] fileDinhKem) throws Exception {
        JavaMailSender sender = mailSender != null ? mailSender.getIfAvailable() : null;
        if (sender == null || !duocCauHinh()) {
            return guiBaoCao(den, tieuDe, banChu, html, anh);
        }
        MimeMessage m = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(m, true, "UTF-8");
        if (from != null && !from.isBlank()) helper.setFrom(from, tenCuaHang);
        helper.setTo(den);
        helper.setSubject(tieuDe);
        helper.setText(banChu, html); // phải gọi trước addInline
        for (Map.Entry<String, byte[]> e : anh.entrySet()) {
            helper.addInline(e.getKey(), new ByteArrayResource(e.getValue()), "image/png");
        }
        if (tenFileDinhKem != null && !tenFileDinhKem.isBlank() && fileDinhKem != null && fileDinhKem.length > 0) {
            helper.addAttachment(tenFileDinhKem, new ByteArrayResource(fileDinhKem),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        sender.send(m);
        log.info("Đã gửi báo cáo '{}' đến {}", tieuDe, den);
        return true;
    }

    void gui(String den, ThuMail thu, byte[] logoGan) {
        gui(den, thu, logoGan, null);
    }

    void gui(String den, ThuMail thu, byte[] logoGan, LoThu lo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !duocCauHinh()) {
            log.warn("[MAIL GIẢ LẬP - CHƯA cấu hình Gmail nên KHÔNG gửi thật] Đến: {} | Tiêu đề: {}\n{}", den, thu.tieuDe(), thu.banChu());
            if (lo != null) lo.ghiGiaLap();
            return;
        }
        try {
            sender.send(soanThu(sender, den, thu, logoGan));
            log.info("Đã gửi mail '{}' đến {}", thu.tieuDe(), den);
            if (lo != null) lo.ghiDaGui();
        } catch (Exception e) {
            // Gửi lỗi (sai mật khẩu, mất mạng...) thì chỉ ghi log, không ảnh hưởng dữ liệu phiếu đã lưu
            log.error("KHÔNG gửi được mail '{}' đến {}: {}{}", thu.tieuDe(), den, loiThat(e), gopY(e));
            if (lo != null) lo.ghiLoi(loiThat(e) + gopY(e));
        }
    }

    /* ===================== Theo dõi kết quả gửi (để giao diện báo rõ) ===================== */

    private static String khoa(String maPhieu) {
        return maPhieu == null ? "" : maPhieu.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Bắt đầu một lô thư mới của phiếu (mỗi lần lưu phiếu), xóa kết quả của lần lưu trước.
     * soThu = số thư sẽ xếp gửi trong lô (biết trước nên giao diện không báo "xong" khi thư chưa xếp hết).
     */
    public void batDauLo(String maPhieu, int soThu) {
        LoThu lo = new LoThu();
        lo.tong = Math.max(0, soThu);
        lo.datTruoc = true;
        loThu.put(khoa(maPhieu), lo);
    }

    /**
     * Kết quả lô thư gần nhất của phiếu: tong (số thư đã xếp), daGui, loi, giaLap (chưa cấu hình nên không gửi thật),
     * xong (mọi thư đã có kết quả), lyDo (lý do lỗi gần nhất). Chưa có lô nào thì tong = 0, xong = true.
     */
    public Map<String, Object> ketQuaLo(String maPhieu) {
        LoThu lo = loThu.get(khoa(maPhieu));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maPhieu", maPhieu);
        if (lo == null) {
            m.put("tong", 0);
            m.put("daGui", 0);
            m.put("loi", 0);
            m.put("giaLap", 0);
            m.put("xong", true);
            m.put("lyDo", "");
            return m;
        }
        synchronized (lo) {
            m.put("tong", lo.tong);
            m.put("daGui", lo.daGui);
            m.put("loi", lo.loi);
            m.put("giaLap", lo.giaLap);
            m.put("xong", lo.daGui + lo.loi + lo.giaLap >= lo.tong);
            m.put("lyDo", lo.lyDo);
            m.put("capNhatLuc", lo.capNhatLuc);
        }
        return m;
    }

    /** Trạng thái gửi mail để form phiếu cá nhân cảnh báo trước: daCauHinh, dangNhap (CHUA_CAU_HINH / DANG_KIEM_TRA / THANH_CONG / THAT_BAI), lyDo. */
    public Map<String, Object> trangThai() {
        Map<String, Object> m = new LinkedHashMap<>();
        boolean cauHinh = duocCauHinh();
        m.put("daCauHinh", cauHinh);
        m.put("dangNhap", cauHinh ? dangNhap : "CHUA_CAU_HINH");
        m.put("lyDo", cauHinh ? lyDoDangNhap : "");
        return m;
    }

    /** Đếm kết quả của một lô thư (các luồng gửi cùng ghi nên khóa theo đối tượng). */
    static final class LoThu {
        int tong;
        int daGui;
        int loi;
        int giaLap;
        boolean datTruoc; // tong đã biết trước (batDauLo) thì không tự cộng khi xếp thư
        String lyDo = "";
        LocalDateTime capNhatLuc = LocalDateTime.now();

        synchronized void themThuNeuChuaDatTruoc() {
            if (!datTruoc) tong++;
        }

        synchronized void ghiDaGui() {
            daGui++;
            capNhatLuc = LocalDateTime.now();
        }

        synchronized void ghiLoi(String lyDoLoi) {
            loi++;
            lyDo = lyDoLoi;
            capNhatLuc = LocalDateTime.now();
        }

        synchronized void ghiGiaLap() {
            giaLap++;
            capNhatLuc = LocalDateTime.now();
        }
    }

    /**
     * Xác nhận cho khách biết đã đặt hàng thành công: mã đơn, các món, tổng thanh toán, địa chỉ, cách thanh toán.
     * Gửi ở luồng riêng (không làm chậm / làm hỏng việc đặt hàng); không có email hợp lệ thì bỏ qua;
     * chưa cấu hình Gmail thì chỉ ghi log. `monHang` mỗi phần tử là một dòng chữ, vd "Vợt X (Đen) x2".
     */
    public void guiXacNhanDatHang(String hoTen, String email, String maHoaDon, java.util.List<String> monHang,
                                  String thanhTienChu, String diaChi, String cachThanhToan) {
        guiXacNhanDatHang(hoTen, email, maHoaDon, monHang, thanhTienChu, diaChi, cachThanhToan, null);
    }

    /** Các dòng tiền của đơn (đã định dạng "1.000 ₫") để thư đặt hàng ghi giống hóa đơn; giam / maPhieu / sdt có thể rỗng. */
    public record TienDon(String tamTinh, String giam, String maPhieu, String phiVanChuyen, String sdt) {
    }

    /** Như trên, kèm bảng tiền (tạm tính, giảm giá, phí vận chuyển) và số điện thoại người nhận; `tien` = null thì chỉ ghi tổng. */
    public void guiXacNhanDatHang(String hoTen, String email, String maHoaDon, java.util.List<String> monHang,
                                  String thanhTienChu, String diaChi, String cachThanhToan, TienDon tien) {
        if (email == null || !email.contains("@")) return;
        final String den = email.trim();
        String ten = hoTen == null || hoTen.isBlank() ? "bạn" : hoTen.trim();
        String ma = String.valueOf(maHoaDon);
        String tieuDe = "[" + tenCuaHang + "] Đặt hàng thành công - đơn " + ma;
        StringBuilder chu = new StringBuilder("Xin chào " + ten + ",\n\n"
                + "Cảm ơn bạn đã đặt hàng tại " + tenCuaHang + ". Đơn " + ma + " đã được ghi nhận và đang chờ cửa hàng xác nhận.\n\n");
        if (monHang != null) {
            for (String m : monHang) {
                chu.append("- ").append(m).append('\n');
            }
        }
        chu.append("\nTổng thanh toán: ").append(thanhTienChu)
                .append("\nGiao tới: ").append(diaChi)
                .append("\nThanh toán: ").append(cachThanhToan)
                .append("\n\nBạn có thể tra cứu đơn hàng bằng mã ").append(ma).append(" trên website.\n");

        final byte[] logoGan = logo;
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        StringBuilder h = new StringBuilder(6500);
        moKhungHtml(h, tieuDe,
                "Đơn hàng " + ma + " đã được ghi nhận — Tổng thanh toán: " + thanhTienChu,
                "#16a34a", logoGan != null, "ĐẶT HÀNG THÀNH CÔNG", "Cảm ơn bạn đã đặt hàng!");

        // Lời chào + giới thiệu
        h.append("<tr><td style=\"padding:16px 28px 8px 28px;").append(phong).append("font-size:15px;line-height:24px;color:#374151;\">")
                .append("Xin chào <strong>").append(esc(ten)).append("</strong>,<br>")
                .append("Đơn hàng của bạn tại <strong>").append(esc(tenCuaHang))
                .append("</strong> đã được tiếp nhận thành công và đang chờ cửa hàng xác nhận.</td></tr>\n");

        // Thẻ mã đơn hàng nổi bật
        h.append("<tr><td style=\"padding:12px 28px 8px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f0fdf4;border:2px dashed #16a34a;border-radius:12px;\">")
                .append("<tr><td align=\"center\" style=\"padding:18px 16px;\">")
                .append("<div style=\"").append(phong).append("font-size:12px;color:#6b7280;letter-spacing:1px;font-weight:bold;\">MÃ ĐƠN HÀNG</div>")
                .append("<div style=\"font-family:'Courier New',Courier,monospace;font-size:28px;line-height:36px;letter-spacing:3px;font-weight:bold;color:#15803d;margin-top:4px;\">")
                .append(esc(ma)).append("</div>")
                .append("</td></tr></table></td></tr>\n");

        // Thanh tiến trình: đang ở bước "Đặt hàng" (giống dòng thời gian ở màn tra cứu đơn)
        themTienTrinh(h, 1);

        // Bảng chi tiết sản phẩm
        boolean coCotGia = false;
        java.util.List<String[]> dsMonTach = new java.util.ArrayList<>();
        if (monHang != null) {
            for (String m : monHang) {
                String[] tach = tachMonHang(m);
                if (!tach[3].isEmpty()) coCotGia = true;
                dsMonTach.add(tach);
            }
        }

        h.append("<tr><td style=\"padding:14px 28px 6px 28px;\">")
                .append("<div style=\"").append(phong).append("font-size:13px;font-weight:bold;color:#111827;letter-spacing:0.5px;margin-bottom:8px;\">CHI TIẾT SẢN PHẨM</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;\">")
                .append("<tr style=\"background-color:#f9fafb;\">")
                .append("<td style=\"padding:10px 14px;border-bottom:1px solid #e5e7eb;").append(phong).append("font-size:12px;font-weight:bold;color:#6b7280;\">SẢN PHẨM</td>")
                .append("<td align=\"center\" style=\"padding:10px 10px;border-bottom:1px solid #e5e7eb;white-space:nowrap;").append(phong).append("font-size:12px;font-weight:bold;color:#6b7280;\">SL</td>");
        if (coCotGia) {
            h.append("<td align=\"right\" style=\"padding:10px 14px;border-bottom:1px solid #e5e7eb;white-space:nowrap;").append(phong).append("font-size:12px;font-weight:bold;color:#6b7280;\">THÀNH TIỀN</td>");
        }
        h.append("</tr>\n");

        for (int i = 0; i < dsMonTach.size(); i++) {
            String[] t = dsMonTach.get(i);
            String borderBottom = (i < dsMonTach.size() - 1) ? "border-bottom:1px solid #f3f4f6;" : "";
            h.append("<tr>")
                    .append("<td style=\"padding:12px 14px;").append(borderBottom).append(phong).append("font-size:14px;line-height:20px;color:#111827;\">")
                    .append("<strong>").append(esc(t[0])).append("</strong>");
            if (!t[1].isEmpty()) {
                h.append("<div style=\"font-size:12px;color:#6b7280;margin-top:2px;\">Phân loại: ").append(esc(t[1])).append("</div>");
            }
            h.append("</td>")
                    .append("<td align=\"center\" style=\"padding:12px 10px;white-space:nowrap;").append(borderBottom).append(phong).append("\">")
                    .append("<span style=\"display:inline-block;background-color:#eff6ff;color:#1d4ed8;font-size:13px;font-weight:bold;padding:3px 10px;border-radius:999px;\">")
                    .append(esc(t[2].isEmpty() ? "x1" : t[2])).append("</span></td>");
            if (coCotGia) {
                h.append("<td align=\"right\" style=\"padding:12px 14px;white-space:nowrap;").append(borderBottom).append(phong).append("font-size:14px;font-weight:bold;color:#111827;\">")
                        .append(esc(t[3])).append("</td>");
            }
            h.append("</tr>\n");
        }

        // Các dòng tiền như trên hóa đơn: tạm tính, giảm giá (mã phiếu), phí vận chuyển
        if (tien != null) {
            String cot = coCotGia ? "2" : "1";
            dongTien(h, cot, "Tạm tính", tien.tamTinh(), "#111827", true);
            if (tien.giam() != null && !tien.giam().isBlank()) {
                String nhan = tien.maPhieu() == null || tien.maPhieu().isBlank() ? "Giảm giá" : "Giảm giá (mã " + tien.maPhieu() + ")";
                dongTien(h, cot, nhan, "-" + tien.giam(), "#16a34a", false);
            }
            dongTien(h, cot, "Phí vận chuyển", tien.phiVanChuyen() == null || tien.phiVanChuyen().startsWith("0 ") ? "Miễn phí" : tien.phiVanChuyen(),
                    "#111827", false);
        }

        // Dòng tổng thanh toán
        h.append("<tr style=\"background-color:#f8fafc;\">")
                .append("<td colspan=\"").append(coCotGia ? "2" : "1").append("\" style=\"padding:14px;border-top:2px solid #e5e7eb;").append(phong).append("font-size:15px;font-weight:bold;color:#111827;\">Tổng thanh toán</td>")
                .append("<td align=\"right\" style=\"padding:14px;border-top:2px solid #e5e7eb;white-space:nowrap;").append(phong).append("font-size:18px;font-weight:bold;color:#dc2626;\">")
                .append(esc(thanhTienChu)).append("</td></tr>")
                .append("</table></td></tr>\n");

        // Thông tin nhận hàng & thanh toán
        h.append("<tr><td style=\"padding:12px 28px 8px 28px;\">")
                .append("<div style=\"").append(phong).append("font-size:13px;font-weight:bold;color:#111827;letter-spacing:0.5px;margin-bottom:8px;\">THÔNG TIN GIAO NHẬN</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;background-color:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;\">")
                .append("<tr><td style=\"padding:14px 18px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;\">");
        dongThongTinDon(h, "Người nhận", ten, true);
        if (tien != null && tien.sdt() != null && !tien.sdt().isBlank()) dongThongTinDon(h, "Số điện thoại", tien.sdt(), false);
        dongThongTinDon(h, "Địa chỉ giao tới", diaChi, false);
        dongThongTinDon(h, "Phương thức thanh toán", cachThanhToan, false);
        h.append("</table></td></tr></table></td></tr>\n");

        // Hộp hướng dẫn tra cứu đơn hàng
        h.append("<tr><td style=\"padding:12px 28px 8px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;background-color:#eff6ff;border:1px solid #bfdbfe;border-radius:10px;\">")
                .append("<tr><td style=\"padding:14px 18px;").append(phong).append("font-size:14px;line-height:22px;color:#1e3a8a;\">")
                .append("Bạn có thể kiểm tra tiến độ đơn hàng bất cứ lúc nào tại mục <strong>Tra cứu đơn hàng</strong> trên website bằng mã đơn <strong>")
                .append(esc(ma)).append("</strong>.")
                .append("</td></tr></table></td></tr>\n");

        if (linkCuaHang != null) {
            h.append("<tr><td align=\"center\" style=\"padding:14px 28px 4px 28px;\">")
                    .append("<a href=\"").append(esc(linkCuaHang)).append("\" target=\"_blank\" style=\"").append(phong)
                    .append("display:inline-block;background-color:#16a34a;color:#ffffff;font-size:15px;font-weight:bold;text-decoration:none;padding:12px 28px;border-radius:8px;\">")
                    .append("Tra cứu đơn hàng</a></td></tr>\n");
        }

        dongKhungHtml(h);
        final ThuMail thu = new ThuMail(tieuDe, chu.toString(), h.toString());
        luongGui.submit(() -> gui(den, thu, logoGan));
    }

    /**
     * Báo cho khách biết đơn bị hủy (vd. hết hàng khi xác nhận đơn). Gửi ở luồng riêng nên không làm chậm thao tác đổi trạng thái;
     * không có email hợp lệ thì bỏ qua; chưa cấu hình Gmail thì chỉ ghi log (như các thư khác).
     */
    public void guiThongBaoHuyDon(String hoTen, String email, String maHoaDon, String lyDo) {
        if (email == null || !email.contains("@")) return;
        final String den = email.trim();
        String ten = hoTen == null || hoTen.isBlank() ? "bạn" : hoTen.trim();
        String ly = lyDo == null || lyDo.isBlank() ? "Cửa hàng không thể đáp ứng đơn hàng vào lúc này" : lyDo.trim();
        String ma = String.valueOf(maHoaDon);
        String tieuDe = "[" + tenCuaHang + "] Đơn hàng " + ma + " đã bị hủy";
        String banChu = "Xin chào " + ten + ",\n\n"
                + "Rất tiếc, đơn hàng " + ma + " của bạn tại " + tenCuaHang + " đã bị hủy.\n"
                + "Lý do: " + ly + "\n\n"
                + "Nếu bạn đã thanh toán trước, cửa hàng sẽ hoàn tiền lại cho bạn. Xin lỗi vì sự bất tiện này.\n";

        final byte[] logoGan = logo;
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        StringBuilder h = new StringBuilder(4000);
        moKhungHtml(h, tieuDe, "Đơn hàng " + ma + " tại " + tenCuaHang + " đã bị hủy", "#dc2626", logoGan != null, "ĐƠN HÀNG ĐÃ HỦY", "Thông báo hủy đơn hàng");
        h.append("<tr><td style=\"padding:16px 28px 8px 28px;").append(phong).append("font-size:15px;line-height:24px;color:#374151;\">")
                .append("Xin chào <strong>").append(esc(ten)).append("</strong>,<br>")
                .append("Rất tiếc, đơn hàng <strong>").append(esc(ma)).append("</strong> của bạn tại <strong>")
                .append(esc(tenCuaHang)).append("</strong> đã bị hủy.</td></tr>\n");
        h.append("<tr><td style=\"padding:10px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#fef2f2;border:1.5px dashed #dc2626;border-radius:12px;\">")
                .append("<tr><td style=\"padding:16px 20px;").append(phong).append("font-size:14px;line-height:22px;color:#991b1b;\">")
                .append("<strong>Mã đơn hàng:</strong> ").append(esc(ma)).append("<br>")
                .append("<strong>Lý do hủy:</strong> ").append(esc(ly))
                .append("</td></tr></table></td></tr>\n");
        h.append("<tr><td style=\"padding:8px 28px 12px 28px;").append(phong).append("font-size:14px;line-height:22px;color:#4b5563;\">")
                .append("Nếu bạn đã thanh toán trước, cửa hàng sẽ liên hệ để hoàn tiền lại cho bạn. Rất mong bạn thông cảm vì sự bất tiện này.</td></tr>\n");
        dongKhungHtml(h);

        final ThuMail thu = new ThuMail(tieuDe, banChu, h.toString());
        luongGui.submit(() -> gui(den, thu, logoGan));
    }

    /* ===================== Thư báo đổi trạng thái đơn online ===================== */

    /** Đầu dòng ghi email nhận thông báo trong lịch sử "Đặt hàng trực tuyến" (hóa đơn không có cột email; không đổi SQL). */
    public static final String NHAN_EMAIL_DON = "Email nhận thông báo: ";

    /** Lấy email đã ghi trong mô tả lịch sử đặt hàng (null nếu không có). */
    public static String emailTrongMoTa(String moTa) {
        if (moTa == null) return null;
        int i = moTa.indexOf(NHAN_EMAIL_DON);
        if (i < 0) return null;
        String e = moTa.substring(i + NHAN_EMAIL_DON.length()).trim();
        int het = e.indexOf(' ');
        if (het > 0) e = e.substring(0, het);
        return e.contains("@") ? e : null;
    }

    /** Màu nhấn, nhãn, tiêu đề, lời nhắn của thư theo trạng thái mới (mã như entity HoaDon). */
    private record NoiDungTrangThai(String mau, String nhan, String tieuDe, String loiNhan) {
    }

    private static NoiDungTrangThai noiDungTrangThai(int trangThai) {
        return switch (trangThai) {
            case 2 -> new NoiDungTrangThai("#2563eb", "ĐÃ XÁC NHẬN", "Đơn hàng đã được xác nhận",
                    "Cửa hàng đã xác nhận đơn hàng của bạn và đang chuẩn bị hàng.");
            case 3 -> new NoiDungTrangThai("#7c3aed", "ĐANG CHUẨN BỊ HÀNG", "Đơn hàng đã đóng gói xong",
                    "Đơn hàng đã được đóng gói và đang chờ đơn vị vận chuyển đến lấy.");
            case 4 -> new NoiDungTrangThai("#0284c7", "ĐANG GIAO HÀNG", "Đơn hàng đang được giao",
                    "Đơn hàng đã được giao cho đơn vị vận chuyển và đang trên đường đến bạn. Vui lòng để ý điện thoại để nhận hàng.");
            case 5 -> new NoiDungTrangThai("#16a34a", "ĐÃ GIAO HÀNG", "Đơn hàng đã giao thành công",
                    "Đơn hàng đã được giao đến bạn. Cảm ơn bạn đã mua sắm tại cửa hàng!");
            case 6 -> new NoiDungTrangThai("#15803d", "HOÀN THÀNH", "Đơn hàng đã hoàn thành",
                    "Đơn hàng đã hoàn tất. Rất mong được phục vụ bạn trong những lần mua sắm tiếp theo!");
            case 8 -> new NoiDungTrangThai("#ea580c", "GIAO HÀNG KHÔNG THÀNH CÔNG", "Giao hàng chưa thành công",
                    "Đơn vị vận chuyển chưa giao được đơn hàng. Cửa hàng sẽ liên hệ với bạn để sắp xếp giao lại.");
            default -> new NoiDungTrangThai("#2563eb", "CẬP NHẬT ĐƠN HÀNG", "Đơn hàng có cập nhật mới",
                    "Trạng thái đơn hàng của bạn vừa được cập nhật.");
        };
    }

    /**
     * Báo cho khách khi đơn online đổi trạng thái (đã xác nhận, đang chuẩn bị, đang giao, đã giao, hoàn thành, giao thất bại).
     * Đơn bị hủy dùng {@link #guiThongBaoHuyDon}. Gửi ở luồng riêng; không có email hợp lệ thì bỏ qua.
     */
    public void guiCapNhatTrangThai(String hoTen, String email, String maHoaDon, int trangThai, String ghiChu,
                                   String thanhTienChu, String diaChi) {
        if (email == null || !email.contains("@")) return;
        final String den = email.trim();
        String ten = hoTen == null || hoTen.isBlank() ? "bạn" : hoTen.trim();
        String ma = String.valueOf(maHoaDon);
        NoiDungTrangThai nd = noiDungTrangThai(trangThai);
        boolean thatBai = trangThai == 8;
        String lyDo = thatBai && ghiChu != null && !ghiChu.isBlank() ? ghiChu.trim() : null;
        String tieuDe = "[" + tenCuaHang + "] Đơn hàng " + ma + ": " + nd.tieuDe().toLowerCase(Locale.forLanguageTag("vi-VN"));
        StringBuilder chu = new StringBuilder("Xin chào " + ten + ",\n\n" + nd.loiNhan() + "\n\nMã đơn hàng: " + ma);
        if (lyDo != null) chu.append("\nLý do: ").append(lyDo);
        if (thanhTienChu != null) chu.append("\nTổng thanh toán: ").append(thanhTienChu);
        if (diaChi != null && !diaChi.isBlank()) chu.append("\nGiao tới: ").append(diaChi);
        chu.append("\n\nBạn có thể tra cứu đơn hàng bằng mã ").append(ma).append(" trên website.\n");

        final byte[] logoGan = logo;
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        StringBuilder h = new StringBuilder(5000);
        moKhungHtml(h, tieuDe, "Đơn hàng " + ma + ": " + nd.tieuDe(), nd.mau(), logoGan != null, nd.nhan(), nd.tieuDe());
        h.append("<tr><td style=\"padding:16px 28px 8px 28px;").append(phong).append("font-size:15px;line-height:24px;color:#374151;\">")
                .append("Xin chào <strong>").append(esc(ten)).append("</strong>,<br>").append(esc(nd.loiNhan())).append("</td></tr>\n");
        themTienTrinh(h, trangThai);
        h.append("<tr><td style=\"padding:12px 28px 8px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;background-color:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;\">")
                .append("<tr><td style=\"padding:14px 18px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;\">");
        dongThongTinDon(h, "Mã đơn hàng", ma, true);
        dongThongTinDon(h, "Trạng thái", nd.tieuDe(), false);
        if (lyDo != null) dongThongTinDon(h, "Lý do", lyDo, false);
        if (thanhTienChu != null) dongThongTinDon(h, "Tổng thanh toán", thanhTienChu, false);
        if (diaChi != null && !diaChi.isBlank()) dongThongTinDon(h, "Giao tới", diaChi, false);
        h.append("</table></td></tr></table></td></tr>\n");
        if (linkCuaHang != null) {
            h.append("<tr><td align=\"center\" style=\"padding:14px 28px 4px 28px;\">")
                    .append("<a href=\"").append(esc(linkCuaHang)).append("\" target=\"_blank\" style=\"").append(phong)
                    .append("display:inline-block;background-color:").append(nd.mau())
                    .append(";color:#ffffff;font-size:15px;font-weight:bold;text-decoration:none;padding:12px 28px;border-radius:8px;\">")
                    .append("Tra cứu đơn hàng</a></td></tr>\n");
        }
        dongKhungHtml(h);
        final ThuMail thu = new ThuMail(tieuDe, chu.toString(), h.toString());
        luongGui.submit(() -> gui(den, thu, logoGan));
    }

    /**
     * Thanh tiến trình 5 bước của đơn online (Đặt hàng → Xác nhận → Đang giao → Đã giao → Hoàn thành), dựng bằng bảng để
     * hiện đúng trên Gmail / Outlook. `trangThai` là mã trạng thái của hóa đơn; giao thất bại tô cam ở bước "Đang giao".
     */
    private static void themTienTrinh(StringBuilder h, int trangThai) {
        String[] nhan = {"Đặt hàng", "Xác nhận", "Đang giao", "Đã giao", "Hoàn thành"};
        int buoc = switch (trangThai) {
            case 2, 3 -> 1;
            case 4, 8 -> 2;
            case 5 -> 3;
            case 6 -> 4;
            default -> 0;
        };
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        h.append("<tr><td style=\"padding:14px 28px 6px 28px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;table-layout:fixed;\"><tr>");
        for (int i = 0; i < nhan.length; i++) {
            if (i > 0) {
                // Mũi tên nối hai bước: tô màu khi đơn đã đi qua đoạn này
                String mauMuiTen = i <= buoc ? (trangThai == 8 && i == buoc ? "#ea580c" : "#2563eb") : "#d1d5db";
                h.append("<td width=\"22\" align=\"center\" valign=\"top\" style=\"width:22px;padding:0;\">")
                        .append("<div style=\"height:34px;line-height:34px;").append(phong).append("font-size:18px;font-weight:bold;color:")
                        .append(mauMuiTen).append(";\">&#8594;</div></td>");
            }
            boolean xong = i < buoc || (i == buoc && trangThai == 6);
            boolean dangO = i == buoc && !xong;
            String mau = trangThai == 8 && i == buoc ? "#ea580c" : "#2563eb";
            String nen = xong ? mau : dangO ? "#ffffff" : "#ffffff";
            String vien = xong || dangO ? mau : "#d1d5db";
            String chuSo = xong ? "#ffffff" : dangO ? mau : "#9ca3af";
            String kyHieu = xong ? "&#10003;" : String.valueOf(i + 1);
            h.append("<td align=\"center\" valign=\"top\" style=\"padding:0 2px;\">")
                    .append("<div style=\"width:30px;height:30px;line-height:30px;margin:0 auto;border-radius:50%;border:2px solid ").append(vien)
                    .append(";background-color:").append(nen).append(";color:").append(chuSo).append(";").append(phong)
                    .append("font-size:13px;font-weight:bold;text-align:center;\">").append(kyHieu).append("</div>")
                    .append("<div style=\"").append(phong).append("margin-top:6px;font-size:11px;line-height:14px;font-weight:")
                    .append(xong || dangO ? "bold;color:#111827" : "normal;color:#9ca3af").append(";\">")
                    .append(esc(trangThai == 8 && i == buoc ? "Giao thất bại" : nhan[i])).append("</div></td>");
        }
        h.append("</tr></table></td></tr>\n");
    }

    /** Một dòng tiền (nhãn - giá trị) trong bảng sản phẩm của thư đặt hàng. */
    private static void dongTien(StringBuilder h, String colspan, String nhan, String giaTri, String mau, boolean dongDau) {
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        String vien = dongDau ? "border-top:1px solid #e5e7eb;" : "";
        h.append("<tr><td colspan=\"").append(colspan).append("\" style=\"padding:8px 14px;").append(vien).append(phong)
                .append("font-size:14px;color:#6b7280;\">").append(esc(nhan)).append("</td>")
                .append("<td align=\"right\" style=\"padding:8px 14px;white-space:nowrap;").append(vien).append(phong)
                .append("font-size:14px;font-weight:bold;color:").append(mau).append(";\">").append(esc(giaTri)).append("</td></tr>\n");
    }

    /** Dựng phần mở đầu chuẩn cho thư HTML (dải màu nhấn, logo cửa hàng, nhãn trạng thái, tiêu đề lớn). */
    private void moKhungHtml(StringBuilder h, String tieuDeTrang, String xemTruoc, String mauNhan,
                             boolean coLogo, String nhanTrangThai, String tieuDeLon) {
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        String ten2 = esc(tenCuaHang);
        h.append("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(esc(tieuDeTrang)).append("</title>\n</head>\n")
                .append("<body style=\"margin:0;padding:0;background-color:#f3f4f6;\">\n")
                .append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:#f3f4f6;font-size:1px;line-height:1px;\">")
                .append(esc(xemTruoc)).append("</div>\n")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f3f4f6;\">\n")
                .append("<tr><td align=\"center\" style=\"padding:28px 12px;\">\n")
                .append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:600px;background-color:#ffffff;border-radius:14px;border:1px solid #e5e7eb;\">\n")
                .append("<tr><td style=\"background-color:").append(mauNhan)
                .append(";height:6px;font-size:0;line-height:0;border-radius:14px 14px 0 0;\">&nbsp;</td></tr>\n")
                .append("<tr><td align=\"center\" style=\"padding:24px 28px 6px 28px;\">");
        if (coLogo) {
            h.append("<img src=\"cid:").append(PhieuMailNoiDung.CID_LOGO).append("\" alt=\"").append(ten2)
                    .append("\" width=\"145\" style=\"display:block;margin:0 auto;border:0;outline:none;width:145px;max-width:100%;height:auto;\">");
        } else {
            h.append("<div style=\"").append(phong).append("font-size:22px;font-weight:bold;color:#111827;\">").append(ten2).append("</div>");
        }
        h.append("</td></tr>\n")
                .append("<tr><td align=\"center\" style=\"padding:14px 28px 0 28px;\">")
                .append("<span style=\"").append(phong).append("display:inline-block;background-color:").append(mauNhan)
                .append(";color:#ffffff;font-size:11px;font-weight:bold;letter-spacing:1px;padding:5px 14px;border-radius:999px;\">")
                .append(esc(nhanTrangThai)).append("</span></td></tr>\n")
                .append("<tr><td align=\"center\" style=\"padding:10px 28px 0 28px;\">")
                .append("<h1 style=\"").append(phong).append("margin:0;font-size:23px;line-height:30px;color:#111827;\">")
                .append(esc(tieuDeLon)).append("</h1></td></tr>\n");
    }

    /** Dựng phần chân thư và đóng bảng HTML. */
    private void dongKhungHtml(StringBuilder h) {
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        h.append("<tr><td align=\"center\" style=\"padding:20px 28px 28px 28px;border-top:1px solid #f3f4f6;margin-top:12px;").append(phong)
                .append("font-size:13px;line-height:20px;color:#6b7280;\">")
                .append("Cảm ơn bạn đã tin tưởng và đồng hành cùng <strong>").append(esc(tenCuaHang)).append("</strong>.<br>")
                .append("Đây là thư tự động, vui lòng không trả lời thư này.</td></tr>\n")
                .append("</table>\n</td></tr>\n</table>\n</body>\n</html>\n");
    }

    private static void dongThongTinDon(StringBuilder h, String nhan, String giaTri, boolean dongDau) {
        String phong = "font-family:Arial,Helvetica,sans-serif;";
        String padTop = dongDau ? "0" : "10px";
        h.append("<tr>")
                .append("<td valign=\"top\" style=\"padding:").append(padTop).append(" 12px 0 0;white-space:nowrap;").append(phong).append("font-size:14px;color:#6b7280;\">")
                .append(esc(nhan)).append("</td>")
                .append("<td valign=\"top\" align=\"right\" style=\"padding:").append(padTop).append(" 0 0 0;").append(phong).append("font-size:14px;font-weight:bold;color:#111827;\">")
                .append(esc(giaTri)).append("</td></tr>\n");
    }

    /**
     * Tách chuỗi món hàng (vd: "Yonex Astrox 99 Pro (Đen) x1 - 3.500.000 ₫") thành 4 phần:
     * [0] Tên sản phẩm, [1] Phân loại (màu), [2] Số lượng ("x1"), [3] Thành tiền ("3.500.000 ₫" hoặc "").
     */
    static String[] tachMonHang(String raw) {
        if (raw == null) return new String[] {"", "", "", ""};
        String s = raw.trim();
        String gia = "";
        int idxGia = s.lastIndexOf(" - ");
        if (idxGia > 0 && s.substring(idxGia + 3).contains("₫")) {
            gia = s.substring(idxGia + 3).trim();
            s = s.substring(0, idxGia).trim();
        }
        String sl = "";
        int idxSl = s.lastIndexOf(" x");
        if (idxSl > 0 && s.substring(idxSl + 2).matches("\\d+")) {
            sl = "x" + s.substring(idxSl + 2);
            s = s.substring(0, idxSl).trim();
        }
        String phanLoai = "";
        if (s.endsWith(")") && s.contains(" (")) {
            int idxMo = s.lastIndexOf(" (");
            phanLoai = s.substring(idxMo + 2, s.length() - 1).trim();
            s = s.substring(0, idxMo).trim();
        }
        return new String[] {s, phanLoai, sl, gia};
    }

    private static String esc(String s) {
        return s == null ? "" : org.springframework.web.util.HtmlUtils.htmlEscape(s, "UTF-8");
    }

    /** Ghép thư HTML (kèm bản chữ) và logo gắn kiểu CID. logoGan = null: không gắn logo. */
    MimeMessage soanThu(JavaMailSender sender, String den, ThuMail thu, byte[] logoGan) throws Exception {
        MimeMessage m = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(m, true, "UTF-8");
        if (from != null && !from.isBlank()) {
            helper.setFrom(from, tenCuaHang);
        }
        helper.setTo(den);
        helper.setSubject(thu.tieuDe());
        helper.setText(thu.banChu(), thu.html()); // phải gọi trước addInline
        if (logoGan != null) {
            helper.addInline(PhieuMailNoiDung.CID_LOGO, new ByteArrayResource(logoGan), "image/jpeg");
        }
        return m;
    }
}
