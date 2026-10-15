package com.footstyle.demo.service;

import com.footstyle.demo.service.PhieuMailNoiDung.ThuMail;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

/**
 * Dựng nội dung mail THÔNG BÁO TÀI KHOẢN gửi cho nhân viên mới sau khi nhập hàng loạt từ Excel (tiêu đề, bản chữ, bản HTML).
 * Chỉ là các phép dựng chuỗi, không cần Spring chạy nên kiểm tra được độc lập, giống PhieuMailNoiDung (cùng quy tắc để Gmail hiển thị đúng:
 * bố cục bảng, CSS viết thẳng vào thẻ, rộng tối đa 600px, logo gắn kiểu CID).
 */
public final class NhanVienMailNoiDung {

    private static final String PHONG = "font-family:Arial,Helvetica,sans-serif;";
    private static final String NHAN = "#0977ec";

    private final String tenCuaHang;
    private final String linkCuaHang;

    public NhanVienMailNoiDung(String tenCuaHang, String linkCuaHang) {
        this.tenCuaHang = (tenCuaHang == null || tenCuaHang.isBlank()) ? "FootStyle" : tenCuaHang.trim();
        this.linkCuaHang = chuanHoaLink(linkCuaHang);
    }

    public ThuMail soan(String hoTen, String email, String maNhanVien, String vaiTro, String matKhauTam, boolean coLogo) {
        return new ThuMail(tieuDe(), banChu(hoTen, email, maNhanVien, vaiTro, matKhauTam), html(hoTen, email, maNhanVien, vaiTro, matKhauTam, coLogo));
    }

    public String tieuDe() {
        return "[" + tenCuaHang + "] Tài khoản nhân viên của bạn đã được tạo";
    }

    public String banChu(String hoTen, String email, String maNhanVien, String vaiTro, String matKhauTam) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chào ").append(ten(hoTen)).append(",\n\n");
        sb.append("Cửa hàng ").append(tenCuaHang).append(" vừa tạo tài khoản nhân viên cho bạn. Thông tin đăng nhập:\n\n");
        sb.append("- Mã nhân viên: ").append(maNhanVien).append('\n');
        sb.append("- Vai trò: ").append(vaiTro).append('\n');
        sb.append("- Tài khoản (email): ").append(email).append('\n');
        sb.append("- Mật khẩu tạm: ").append(matKhauTam).append('\n');
        if (linkCuaHang != null) sb.append("- Địa chỉ đăng nhập: ").append(linkCuaHang).append('\n');
        sb.append("\nVì lý do bảo mật, hãy đổi mật khẩu ngay sau lần đăng nhập đầu tiên và không chuyển tiếp thư này cho người khác.\n");
        sb.append("Nếu bạn không phải là người nhận, vui lòng bỏ qua và xóa thư này.\n");
        return sb.toString();
    }

    public String html(String hoTen, String email, String maNhanVien, String vaiTro, String matKhauTam, boolean coLogo) {
        String ten2 = e(tenCuaHang);
        StringBuilder h = new StringBuilder(4500);
        h.append("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(e(tieuDe())).append("</title>\n</head>\n")
                .append("<body style=\"margin:0;padding:0;background-color:#f3f4f6;\">\n");
        h.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:#f3f4f6;font-size:1px;line-height:1px;\">")
                .append(e("Tài khoản nhân viên " + maNhanVien + " đã được tạo")).append("</div>\n");
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f3f4f6;\">\n")
                .append("<tr><td align=\"center\" style=\"padding:24px 12px;\">\n")
                .append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:600px;background-color:#ffffff;border-radius:12px;\">\n");
        h.append("<tr><td style=\"background-color:").append(NHAN)
                .append(";height:6px;font-size:0;line-height:0;border-radius:12px 12px 0 0;\">&nbsp;</td></tr>\n");
        h.append("<tr><td align=\"center\" style=\"padding:24px 24px 4px 24px;\">");
        if (coLogo) {
            h.append("<img src=\"cid:").append(PhieuMailNoiDung.CID_LOGO).append("\" alt=\"").append(ten2)
                    .append("\" width=\"150\" style=\"display:block;margin:0 auto;border:0;outline:none;width:150px;max-width:100%;height:auto;\">");
        } else {
            h.append("<div style=\"").append(PHONG).append("font-size:22px;font-weight:bold;color:#111827;\">").append(ten2).append("</div>");
        }
        h.append("</td></tr>\n");
        h.append("<tr><td align=\"center\" style=\"padding:16px 24px 0 24px;\">")
                .append("<span style=\"").append(PHONG).append("display:inline-block;background-color:").append(NHAN)
                .append(";color:#ffffff;font-size:12px;font-weight:bold;letter-spacing:1px;padding:5px 14px;border-radius:999px;\">TÀI KHOẢN MỚI</span></td></tr>\n");
        h.append("<tr><td align=\"center\" style=\"padding:12px 24px 0 24px;\">")
                .append("<h1 style=\"").append(PHONG).append("margin:0;font-size:24px;line-height:32px;color:#111827;\">Tài khoản nhân viên đã được tạo</h1></td></tr>\n");
        h.append("<tr><td style=\"padding:16px 24px 8px 24px;").append(PHONG).append("font-size:15px;line-height:24px;color:#374151;\">")
                .append("Xin chào <strong>").append(e(ten(hoTen))).append("</strong>,<br>")
                .append("Cửa hàng ").append(ten2).append(" vừa tạo tài khoản nhân viên cho bạn. Thông tin đăng nhập như sau:</td></tr>\n");

        h.append("<tr><td style=\"padding:8px 24px 8px 24px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;background-color:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;\">")
                .append("<tr><td style=\"padding:8px 20px 16px 20px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;\">");
        dong(h, "Mã nhân viên", maNhanVien, false);
        dong(h, "Vai trò", vaiTro, false);
        dong(h, "Tài khoản (email)", email, false);
        dong(h, "Mật khẩu tạm", matKhauTam, true);
        h.append("</table></td></tr></table></td></tr>\n");

        if (linkCuaHang != null) {
            h.append("<tr><td align=\"center\" style=\"padding:16px 24px 8px 24px;\">")
                    .append("<a href=\"").append(e(linkCuaHang)).append("\" target=\"_blank\" style=\"").append(PHONG)
                    .append("display:inline-block;background-color:").append(NHAN)
                    .append(";color:#ffffff;font-size:16px;font-weight:bold;text-decoration:none;padding:13px 32px;border-radius:8px;\">Đăng nhập</a></td></tr>\n");
        }
        h.append("<tr><td style=\"padding:16px 24px 8px 24px;").append(PHONG).append("font-size:14px;line-height:22px;color:#b45309;\">")
                .append("<strong>Lưu ý bảo mật:</strong> hãy đổi mật khẩu ngay sau lần đăng nhập đầu tiên và không chuyển tiếp thư này cho người khác.</td></tr>\n");
        h.append("<tr><td align=\"center\" style=\"padding:20px 24px 28px 24px;").append(PHONG)
                .append("font-size:13px;line-height:20px;color:#6b7280;\">")
                .append("Nếu bạn không phải là người nhận, vui lòng bỏ qua và xóa thư này.<br>")
                .append("Đây là thư tự động, vui lòng không trả lời thư này.</td></tr>\n");
        h.append("</table>\n</td></tr>\n</table>\n</body>\n</html>\n");
        return h.toString();
    }

    private void dong(StringBuilder h, String nhan, String giaTri, boolean dacBiet) {
        h.append("<tr><td style=\"padding:10px 0 0 0;white-space:nowrap;").append(PHONG).append("font-size:14px;color:#6b7280;\">").append(e(nhan)).append("</td>")
                .append("<td align=\"right\" style=\"padding:10px 0 0 12px;")
                .append(dacBiet ? "font-family:'Courier New',Courier,monospace;font-size:18px;letter-spacing:2px;" : PHONG + "font-size:14px;")
                .append("font-weight:bold;color:#111827;word-break:break-all;\">").append(e(giaTri)).append("</td></tr>");
    }

    private static String ten(String hoTen) {
        return (hoTen == null || hoTen.isBlank()) ? "bạn" : hoTen.trim();
    }

    private static String chuanHoaLink(String link) {
        if (link == null) return null;
        String l = link.trim();
        String thap = l.toLowerCase(Locale.ROOT);
        return (thap.startsWith("http://") || thap.startsWith("https://")) && l.length() > 8 ? l : null;
    }

    private static String e(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }
}
