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
        this.tenCuaHang = tenCuaHang;
        this.noiDung = new PhieuMailNoiDung(tenCuaHang, linkCuaHang);
        this.noiDungNhanVien = new NhanVienMailNoiDung(tenCuaHang, linkCuaHang);
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
        return mailSender.getIfAvailable() != null && !username.isBlank() && !password.isBlank();
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
        String html = "<div style=\"font-family:Arial,sans-serif;font-size:15px;color:#18242f\">"
                + "<p>Xin chào <b>" + org.springframework.web.util.HtmlUtils.htmlEscape(ten) + "</b>,</p>"
                + "<p>Bạn (hoặc ai đó) vừa yêu cầu cấp lại mật khẩu tài khoản " + org.springframework.web.util.HtmlUtils.htmlEscape(tenCuaHang) + ".</p>"
                + "<p>Mật khẩu tạm: <b style=\"font-size:18px;letter-spacing:1px\">" + org.springframework.web.util.HtmlUtils.htmlEscape(matKhauTam) + "</b></p>"
                + "<p>Hãy đăng nhập bằng mật khẩu này rồi <b>đổi mật khẩu ngay</b> (menu tài khoản &gt; Đổi mật khẩu).</p>"
                + "<p style=\"color:#7b8794\">Nếu không phải bạn yêu cầu, hãy báo cho quản lý cửa hàng.</p></div>";
        final ThuMail thu = new ThuMail(tieuDe, banChu, html);
        luongGui.submit(() -> gui(den, thu, null));
    }

    /**
     * Gửi NGAY (chờ kết quả) một thư báo cáo HTML kèm ảnh nhúng kiểu CID (biểu đồ thống kê): anh = { cid -> PNG }.
     * Trả về true nếu gửi thật, false nếu chưa cấu hình Gmail (chỉ ghi log). Lỗi gửi thì ném ra để giao diện báo.
     */
    public boolean guiBaoCao(String den, String tieuDe, String banChu, String html, Map<String, byte[]> anh) throws Exception {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !duocCauHinh()) {
            log.warn("[MAIL GIẢ LẬP - CHƯA cấu hình Gmail nên KHÔNG gửi thật] Đến: {} | Tiêu đề: {}\n{}", den, tieuDe, banChu);
            return false;
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
