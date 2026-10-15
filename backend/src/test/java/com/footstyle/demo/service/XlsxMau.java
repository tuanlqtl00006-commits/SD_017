package com.footstyle.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dựng file .xlsx nhỏ cho test ngay trong bộ nhớ (chỉ dùng JDK, không cần thư viện Excel).
 *  - tao(dong...): mỗi ô là String (ô chữ nội tuyến), Number (ô số), LocalDate (ô ngày = số serial của Excel) hoặc null (ô trống).
 *  - zip(tep): ghép tay từng tệp XML để dựng những file "lạ" (chuỗi dùng chung, thực thể XML, sheet đảo thứ tự...).
 */
final class XlsxMau {

    static final String NS = "xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"";

    private XlsxMau() {
    }

    /** Tiêu đề đúng như file mẫu của hệ thống. */
    static final List<Object> TIEU_DE = List.of("Họ và tên (*)", "Giới tính (*)", "Ngày sinh (*)", "Số điện thoại (*)", "Email (*)",
            "Số căn cước công dân", "Ngày vào làm", "Vai trò", "Địa chỉ cụ thể (*)", "Phường/Xã (*)", "Tỉnh/Thành phố (*)");

    /** Một dòng nhân viên hợp lệ theo thứ tự cột của file mẫu. */
    static List<Object> nhanVien(String hoTen, String email, String sdt) {
        return java.util.Arrays.asList(hoTen, "Nam", "20/05/1999", sdt, email, null, null, "Nhân viên",
                "Số 15 ngõ 120 Trần Phú", "Phường Hà Đông", "Thành phố Hà Nội");
    }

    /** Số serial ngày của Excel (gốc 30/12/1899). */
    static long serial(LocalDate ngay) {
        return ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), ngay);
    }

    /** Nạp file mẫu trong src/test/resources/import. */
    static byte[] nap(String ten) {
        try (InputStream in = XlsxMau.class.getResourceAsStream("/import/" + ten)) {
            if (in == null) throw new IllegalStateException("Không thấy file mẫu /import/" + ten);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SafeVarargs
    static byte[] tao(List<Object>... dong) {
        return tao(List.of(dong));
    }

    static byte[] tao(List<List<Object>> dong) {
        return zip(sheetMotTep(sheetXml(dong, 1)));
    }

    /** Sheet XML từ các dòng; dòng thứ i nằm ở dòng Excel (batDau + i). */
    static String sheetXml(List<List<Object>> dong, int batDau) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<worksheet ").append(NS).append("><sheetData>");
        for (int i = 0; i < dong.size(); i++) {
            int soDong = batDau + i;
            sb.append("<row r=\"").append(soDong).append("\">");
            List<Object> o = dong.get(i);
            for (int c = 0; c < o.size(); c++) {
                Object v = o.get(c);
                if (v == null || (v instanceof String s && s.isEmpty())) continue;
                String ref = cot(c) + soDong;
                if (v instanceof Number n) {
                    sb.append("<c r=\"").append(ref).append("\"><v>").append(n).append("</v></c>");
                } else if (v instanceof LocalDate d) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"1\"><v>").append(serial(d)).append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(thoat(v.toString()))
                            .append("</t></is></c>");
                }
            }
            sb.append("</row>");
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    /** Bộ tệp tối thiểu của một workbook có đúng một sheet (xl/worksheets/sheet1.xml). */
    static Map<String, String> sheetMotTep(String sheetXml) {
        Map<String, String> tep = new LinkedHashMap<>();
        tep.put("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>");
        tep.put("xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook " + NS + "><sheets><sheet name=\"Nhân viên\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        tep.put("xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        tep.put("xl/worksheets/sheet1.xml", sheetXml);
        return tep;
    }

    static byte[] zip(Map<String, String> tep) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> e : tep.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** Cột 0 -> A, 25 -> Z, 26 -> AA. */
    static String cot(int i) {
        StringBuilder sb = new StringBuilder();
        for (int n = i + 1; n > 0; n = (n - 1) / 26) sb.insert(0, (char) ('A' + (n - 1) % 26));
        return sb.toString();
    }

    private static String thoat(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
