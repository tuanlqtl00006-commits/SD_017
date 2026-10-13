package com.footstyle.demo.service;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

/**
 * Dựng nội dung mail phiếu giảm giá (tiêu đề, bản chữ, bản HTML). Chỉ là các phép dựng chuỗi,
 * không cần Spring chạy (chỉ dùng HtmlUtils.htmlEscape có sẵn) nên kiểm tra được độc lập, giống PhieuMailKeHoach.
 *
 * Ba mẫu thư cùng một bộ khung, khác phần đầu và màu nhấn:
 *  - MOI      : "Bạn nhận được phiếu giảm giá"   (xanh lá)
 *  - CAP_NHAT : "Phiếu giảm giá đã được cập nhật" (cam)
 *  - HUY      : "Phiếu giảm giá đã bị hủy"        (đỏ / xám) - không hiện mức giảm, đơn tối thiểu, hiệu lực
 *
 * Quy tắc để Gmail hiển thị đúng: bố cục bằng bảng, CSS viết thẳng vào từng thẻ, rộng tối đa 600px,
 * không dùng thẻ style, JavaScript, font ngoài, ảnh data: hay ảnh từ địa chỉ ngoài. Logo gắn kiểu CID (cid:logo).
 */
public final class PhieuMailNoiDung {

    public enum Loai { MOI, CAP_NHAT, HUY }

    /** Một lá thư đã dựng xong: tiêu đề + bản chữ (dự phòng) + bản HTML. */
    public record ThuMail(String tieuDe, String banChu, String html) {
    }

    /** Mã định danh của logo gắn kèm trong thư; HTML dùng src="cid:logo". */
    public static final String CID_LOGO = "logo";

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PHONG = "font-family:Arial,Helvetica,sans-serif;";

    private final String tenCuaHang;
    private final String linkCuaHang;

    /** linkCuaHang để trống (hoặc không phải http/https) thì thư không có nút bấm. */
    public PhieuMailNoiDung(String tenCuaHang, String linkCuaHang) {
        this.tenCuaHang = (tenCuaHang == null || tenCuaHang.isBlank()) ? "FootStyle" : tenCuaHang.trim();
        this.linkCuaHang = chuanHoaLink(linkCuaHang);
    }

    /** Dựng cả thư. coLogo = true khi thư có gắn logo (HTML sẽ tham chiếu cid:logo). */
    public ThuMail soan(Loai loai, PhieuGiamGia p, KhachHang kh, boolean coLogo) {
        return new ThuMail(tieuDe(loai, p), banChu(loai, p, kh), html(loai, p, kh, coLogo));
    }

    /* ===================== Tiêu đề ===================== */

    public String tieuDe(Loai loai, PhieuGiamGia p) {
        switch (loai) {
            case MOI:
                return "[" + tenCuaHang + "] Bạn nhận được phiếu giảm giá " + p.getMaPhieu();
            case CAP_NHAT:
                return "[" + tenCuaHang + "] Phiếu giảm giá " + p.getMaPhieu() + " đã được cập nhật";
            default:
                return "[" + tenCuaHang + "] Phiếu giảm giá " + p.getMaPhieu() + " đã bị hủy";
        }
    }

    /* ===================== Bản chữ ===================== */

    public String banChu(Loai loai, PhieuGiamGia p, KhachHang kh) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chào ").append(tenKhach(kh)).append(",\n\n");
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
            sb.append("- Đơn tối thiểu: ").append(donToiThieu(p)).append('\n');
            sb.append("- Hiệu lực: ").append(hieuLuc(p)).append('\n');
            if (linkCuaHang != null) {
                sb.append("\nMua sắm tại: ").append(linkCuaHang).append('\n');
            }
        }
        sb.append("\nCảm ơn bạn đã đồng hành cùng ").append(tenCuaHang).append(".\n");
        return sb.toString();
    }

    /* ===================== Bản HTML ===================== */

    public String html(Loai loai, PhieuGiamGia p, KhachHang kh, boolean coLogo) {
        boolean huy = loai == Loai.HUY;
        String nhan;        // màu nhấn (dải trên cùng, nhãn, nút)
        String nen;         // nền nhạt của thẻ phiếu
        String nhan2;       // nhãn nhỏ phía trên tiêu đề
        String tieuDeLon;
        String moTa;
        switch (loai) {
            case MOI:
                nhan = "#16a34a";
                nen = "#ecfdf5";
                nhan2 = "PHIẾU MỚI";
                tieuDeLon = "Bạn nhận được phiếu giảm giá";
                moTa = "Cửa hàng " + tenCuaHang + " tặng bạn một phiếu giảm giá. Thông tin phiếu của bạn như sau:";
                break;
            case CAP_NHAT:
                nhan = "#ea580c";
                nen = "#fff7ed";
                nhan2 = "ĐÃ CẬP NHẬT";
                tieuDeLon = "Phiếu giảm giá đã được cập nhật";
                moTa = "Vui lòng xem thông tin mới bên dưới, thay cho thông tin cũ.";
                break;
            default:
                nhan = "#dc2626";
                nen = "#f3f4f6";
                nhan2 = "ĐÃ HỦY";
                tieuDeLon = "Phiếu giảm giá đã bị hủy";
                moTa = "Phiếu giảm giá dưới đây đã bị hủy và không còn sử dụng được.";
                break;
        }
        String ma = e(p.getMaPhieu());
        String ten = e(p.getTenPhieu());
        String ten2 = e(tenCuaHang);

        StringBuilder h = new StringBuilder(6000);
        h.append("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(e(tieuDe(loai, p))).append("</title>\n</head>\n")
                .append("<body style=\"margin:0;padding:0;background-color:#f3f4f6;\">\n");

        // Dòng xem trước trong hộp thư (ẩn trong nội dung thư). Thư hủy không nhắc mức giảm.
        String xemTruoc = huy
                ? "Phiếu " + p.getMaPhieu() + " đã bị hủy và không còn sử dụng được."
                : tieuDeLon + ": " + p.getMaPhieu() + " - " + mucGiam(p);
        h.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:#f3f4f6;font-size:1px;line-height:1px;\">")
                .append(e(xemTruoc)).append("</div>\n");

        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f3f4f6;\">\n")
                .append("<tr><td align=\"center\" style=\"padding:24px 12px;\">\n")
                .append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:600px;background-color:#ffffff;border-radius:12px;\">\n");

        // Dải màu nhấn
        h.append("<tr><td style=\"background-color:").append(nhan)
                .append(";height:6px;font-size:0;line-height:0;border-radius:12px 12px 0 0;\">&nbsp;</td></tr>\n");

        // Logo + tên cửa hàng
        h.append("<tr><td align=\"center\" style=\"padding:24px 24px 4px 24px;\">");
        if (coLogo) {
            h.append("<img src=\"cid:").append(CID_LOGO).append("\" alt=\"").append(ten2)
                    .append("\" width=\"150\" style=\"display:block;margin:0 auto;border:0;outline:none;width:150px;max-width:100%;height:auto;\">");
        } else {
            h.append("<div style=\"").append(PHONG).append("font-size:22px;font-weight:bold;color:#111827;\">").append(ten2).append("</div>");
        }
        h.append("</td></tr>\n");

        // Nhãn + tiêu đề
        h.append("<tr><td align=\"center\" style=\"padding:16px 24px 0 24px;\">")
                .append("<span style=\"").append(PHONG).append("display:inline-block;background-color:").append(nhan)
                .append(";color:#ffffff;font-size:12px;font-weight:bold;letter-spacing:1px;padding:5px 14px;border-radius:999px;\">")
                .append(nhan2).append("</span></td></tr>\n");
        h.append("<tr><td align=\"center\" style=\"padding:12px 24px 0 24px;\">")
                .append("<h1 style=\"").append(PHONG).append("margin:0;font-size:24px;line-height:32px;color:#111827;\">")
                .append(e(tieuDeLon)).append("</h1></td></tr>\n");

        // Lời chào + mô tả
        h.append("<tr><td style=\"padding:16px 24px 8px 24px;").append(PHONG).append("font-size:15px;line-height:24px;color:#374151;\">")
                .append("Xin chào <strong>").append(e(tenKhach(kh))).append("</strong>,<br>")
                .append(e(moTa)).append("</td></tr>\n");

        // Thẻ phiếu
        boolean maDai = p.getMaPhieu() != null && p.getMaPhieu().length() > 12; // mã dài thì thu nhỏ chữ cho khỏi xuống dòng
        String vien = huy ? "#9ca3af" : nhan;
        String chuMa = huy ? "#6b7280" : nhan;
        h.append("<tr><td style=\"padding:12px 24px 8px 24px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;background-color:")
                .append(nen).append(";border:2px dashed ").append(vien).append(";border-radius:12px;\">")
                .append("<tr><td align=\"center\" style=\"padding:20px 16px 4px 16px;\">")
                .append("<div style=\"").append(PHONG).append("font-size:12px;color:#6b7280;letter-spacing:1px;\">MÃ PHIẾU</div>")
                .append("<div style=\"font-family:'Courier New',Courier,monospace;").append(maDai ? "font-size:20px;line-height:30px;letter-spacing:1px;" : "font-size:30px;line-height:38px;letter-spacing:3px;")
                .append("font-weight:bold;color:")
                .append(chuMa).append(";word-break:break-all;").append(huy ? "text-decoration:line-through;" : "").append("\">")
                .append(ma).append("</div></td></tr>")
                .append("<tr><td align=\"center\" style=\"padding:4px 16px ").append(huy ? "20px" : "8px").append(" 16px;")
                .append(PHONG).append("font-size:16px;font-weight:bold;color:#111827;\">").append(ten).append("</td></tr>");
        if (!huy) {
            h.append("<tr><td align=\"center\" style=\"padding:4px 16px 12px 16px;\">")
                    .append("<div style=\"").append(PHONG).append("font-size:28px;line-height:34px;font-weight:bold;color:").append(nhan).append(";\">")
                    .append(e(mucGiamChinh(p))).append("</div></td></tr>");
            h.append("<tr><td style=\"padding:0 20px 20px 20px;\">")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;border-top:1px solid #e5e7eb;\">");
            if (laPhanTram(p) && p.getGiamToiDa() != null) {
                dongThongTin(h, "Giảm tối đa", tien(p.getGiamToiDa()));
            }
            dongThongTin(h, "Đơn tối thiểu", donToiThieu(p));
            dongThongTin(h, "Hiệu lực", hieuLuc(p));
            h.append("</table></td></tr>");
        }
        h.append("</table></td></tr>\n");

        // Nút bấm (chỉ khi có địa chỉ web và phiếu còn dùng được)
        if (!huy && linkCuaHang != null) {
            h.append("<tr><td align=\"center\" style=\"padding:16px 24px 8px 24px;\">")
                    .append("<a href=\"").append(e(linkCuaHang)).append("\" target=\"_blank\" style=\"").append(PHONG)
                    .append("display:inline-block;background-color:").append(nhan)
                    .append(";color:#ffffff;font-size:16px;font-weight:bold;text-decoration:none;padding:13px 32px;border-radius:8px;\">Mua sắm ngay</a></td></tr>\n");
        }

        // Chân thư
        h.append("<tr><td align=\"center\" style=\"padding:20px 24px 28px 24px;").append(PHONG)
                .append("font-size:13px;line-height:20px;color:#6b7280;\">")
                .append("Cảm ơn bạn đã đồng hành cùng <strong>").append(ten2).append("</strong>.<br>")
                .append("Đây là thư tự động, vui lòng không trả lời thư này.</td></tr>\n");

        h.append("</table>\n</td></tr>\n</table>\n</body>\n</html>\n");
        return h.toString();
    }

    private void dongThongTin(StringBuilder h, String nhan, String giaTri) {
        h.append("<tr>")
                .append("<td style=\"padding:10px 0 0 0;white-space:nowrap;").append(PHONG).append("font-size:14px;color:#6b7280;\">").append(e(nhan)).append("</td>")
                .append("<td align=\"right\" style=\"padding:10px 0 0 12px;").append(PHONG).append("font-size:14px;font-weight:bold;color:#111827;\">")
                .append(e(giaTri)).append("</td></tr>");
    }

    /* ===================== Định dạng ===================== */

    private String tenKhach(KhachHang kh) {
        String ten = kh == null ? null : kh.getHoTen();
        return (ten == null || ten.isBlank()) ? "quý khách" : ten.trim();
    }

    private boolean laPhanTram(PhieuGiamGia p) {
        return p.getLoaiGiam() != null && p.getLoaiGiam() == PhieuGiamGia.LOAI_PHAN_TRAM;
    }

    /** Ví dụ "giảm 10% (tối đa 200.000 ₫)" hoặc "giảm 50.000 ₫". */
    public static String mucGiam(PhieuGiamGia p) {
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

    /** Dòng chữ to trên thẻ phiếu: "Giảm 10%" hoặc "Giảm 50.000 ₫" (phần tối đa nằm ở dòng riêng bên dưới). */
    private String mucGiamChinh(PhieuGiamGia p) {
        return laPhanTram(p) ? "Giảm " + p.getGiaTriGiam() + "%" : "Giảm " + tien(p.getGiaTriGiam());
    }

    private String donToiThieu(PhieuGiamGia p) {
        Long v = p.getGiaTriDonHangToiThieu();
        return (v == null || v <= 0) ? "Không yêu cầu" : tien(v);
    }

    private String hieuLuc(PhieuGiamGia p) {
        return (p.getNgayBatDau() == null ? "" : p.getNgayBatDau().format(NGAY))
                + " – "
                + (p.getNgayKetThuc() == null ? "" : p.getNgayKetThuc().format(NGAY));
    }

    public static String tien(Long v) {
        long x = v == null ? 0 : v;
        return String.format(new Locale("vi", "VN"), "%,d ₫", x);
    }

    /** Chỉ nhận địa chỉ http / https, còn lại coi như không có. */
    private static String chuanHoaLink(String link) {
        if (link == null) {
            return null;
        }
        String l = link.trim();
        String thap = l.toLowerCase(Locale.ROOT);
        return (thap.startsWith("http://") || thap.startsWith("https://")) && l.length() > 8 ? l : null;
    }

    /** Mọi chuỗi từ dữ liệu phải đi qua đây trước khi đưa vào HTML. */
    private static String e(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }
}
