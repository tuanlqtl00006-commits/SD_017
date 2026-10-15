package com.footstyle.demo.service;

import com.footstyle.demo.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Đọc sheet đầu tiên của file .xlsx (Excel 2007+) mà KHÔNG cần thư viện ngoài (chỉ dùng JDK: ZIP + StAX).
 * Đủ cho file nhập danh sách: ô chữ (chuỗi dùng chung, chuỗi nội tuyến), ô số, ô ngày (số serial), ô công thức trả chữ.
 *
 * An toàn khi nhận file người dùng tải lên: tắt DTD và thực thể ngoài (chống XXE), giới hạn dung lượng giải nén (chống "zip bomb")
 * và số dòng / cột đọc vào.
 */
public final class XlsxDoc {

    /** Một ô đã đọc: giá trị chữ + cho biết ô gốc là SỐ (Excel lưu số điện thoại, CCCD, ngày thành số nên cần phân biệt). */
    public record O(String giaTri, boolean laSo) {
        public static final O TRONG = new O("", false);

        public boolean trong() {
            return giaTri == null || giaTri.isBlank();
        }
    }

    /** Một dòng của sheet: số dòng THẬT trong Excel (bắt đầu từ 1) và các ô theo cột (0 = cột A). */
    public record Dong(int soDong, List<O> o) {
        public O o(int cot) {
            return cot >= 0 && cot < o.size() ? o.get(cot) : O.TRONG;
        }

        public boolean trong() {
            return o.stream().allMatch(O::trong);
        }
    }

    private static final long GIOI_HAN_BYTE_MOI_FILE = 30L * 1024 * 1024; // sau khi giải nén
    private static final int GIOI_HAN_DONG = 5000;
    private static final int GIOI_HAN_COT = 100;

    private XlsxDoc() {
    }

    /** Đọc các dòng của sheet đầu tiên (theo thứ tự trong workbook), bỏ qua dòng hoàn toàn trống. */
    public static List<Dong> doc(byte[] data) {
        Map<String, byte[]> tep = giaiNen(data);
        byte[] workbook = tep.get("xl/workbook.xml");
        if (workbook == null) {
            throw ApiException.badRequest("Đây không phải file Excel (.xlsx) hợp lệ. Hãy dùng file mẫu của hệ thống hoặc lưu lại file ở định dạng .xlsx.");
        }
        try {
            String duongDanSheet = timSheetDau(workbook, tep.get("xl/_rels/workbook.xml.rels"));
            byte[] sheet = tep.get(duongDanSheet);
            if (sheet == null) {
                throw ApiException.badRequest("File Excel không có sheet nào để đọc.");
            }
            List<String> chuoiChung = tep.containsKey("xl/sharedStrings.xml") ? docChuoiChung(tep.get("xl/sharedStrings.xml")) : List.of();
            return docSheet(sheet, chuoiChung);
        } catch (XMLStreamException e) {
            throw ApiException.badRequest("Không đọc được file Excel (file hỏng hoặc sai định dạng .xlsx).");
        }
    }

    /* ===================== Giải nén ===================== */

    private static Map<String, byte[]> giaiNen(byte[] data) {
        if (data == null || data.length < 4 || data[0] != 'P' || data[1] != 'K') {
            throw ApiException.badRequest("Đây không phải file Excel (.xlsx) hợp lệ. Hãy dùng file mẫu của hệ thống hoặc lưu lại file ở định dạng .xlsx.");
        }
        Map<String, byte[]> tep = new HashMap<>();
        long tong = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry e;
            int dem = 0;
            while ((e = zip.getNextEntry()) != null) {
                if (++dem > 200) throw ApiException.badRequest("File Excel không hợp lệ (quá nhiều thành phần).");
                String ten = e.getName();
                boolean can = ten.equals("xl/workbook.xml") || ten.equals("xl/_rels/workbook.xml.rels")
                        || ten.equals("xl/sharedStrings.xml") || ten.startsWith("xl/worksheets/");
                if (!can || e.isDirectory()) continue;
                byte[] noiDung = docToiDa(zip, GIOI_HAN_BYTE_MOI_FILE - tong);
                tong += noiDung.length;
                tep.put(ten, noiDung);
            }
        } catch (IOException e) {
            throw ApiException.badRequest("Không đọc được file Excel (file hỏng hoặc sai định dạng .xlsx).");
        }
        return tep;
    }

    private static byte[] docToiDa(InputStream in, long toiDa) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] dem = new byte[8192];
        long da = 0;
        int n;
        while ((n = in.read(dem)) > 0) {
            da += n;
            if (da > toiDa) throw ApiException.badRequest("File Excel quá lớn sau khi giải nén. Mỗi lần chỉ nhập danh sách nhân viên cỡ vài chục dòng.");
            out.write(dem, 0, n);
        }
        return out.toByteArray();
    }

    /* ===================== XML ===================== */

    private static XMLStreamReader moXml(byte[] noiDung) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        return f.createXMLStreamReader(new ByteArrayInputStream(noiDung), "UTF-8");
    }

    /** Sheet đầu tiên trong workbook.xml, đổi từ r:id sang đường dẫn file qua workbook.xml.rels. */
    private static String timSheetDau(byte[] workbook, byte[] rels) throws XMLStreamException {
        String rId = null;
        XMLStreamReader r = moXml(workbook);
        try {
            while (r.hasNext()) {
                if (r.next() == XMLStreamConstants.START_ELEMENT && r.getLocalName().equals("sheet")) {
                    rId = thuocTinh(r, "id"); // r:id (bỏ tiền tố khoa tên)
                    break;
                }
            }
        } finally {
            r.close();
        }
        if (rId != null && rels != null) {
            XMLStreamReader rr = moXml(rels);
            try {
                while (rr.hasNext()) {
                    if (rr.next() == XMLStreamConstants.START_ELEMENT && rr.getLocalName().equals("Relationship") && rId.equals(rr.getAttributeValue(null, "Id"))) {
                        String dich = rr.getAttributeValue(null, "Target");
                        if (dich != null) {
                            dich = dich.startsWith("/") ? dich.substring(1) : "xl/" + dich;
                            return dich;
                        }
                    }
                }
            } finally {
                rr.close();
            }
        }
        return "xl/worksheets/sheet1.xml";
    }

    /** Giá trị thuộc tính theo tên cục bộ (bỏ tiền tố namespace như r:id). */
    private static String thuocTinh(XMLStreamReader r, String tenCucBo) {
        for (int i = 0; i < r.getAttributeCount(); i++) {
            if (r.getAttributeLocalName(i).equals(tenCucBo)) return r.getAttributeValue(i);
        }
        return null;
    }

    private static List<String> docChuoiChung(byte[] noiDung) throws XMLStreamException {
        List<String> ds = new ArrayList<>();
        XMLStreamReader r = moXml(noiDung);
        try {
            StringBuilder sb = null;
            boolean trongT = false;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String ten = r.getLocalName();
                    if (ten.equals("si")) sb = new StringBuilder();
                    else if (ten.equals("t") && sb != null) trongT = true;
                } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    if (trongT && sb != null) sb.append(r.getText());
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String ten = r.getLocalName();
                    if (ten.equals("t")) trongT = false;
                    else if (ten.equals("si") && sb != null) {
                        ds.add(sb.toString());
                        sb = null;
                    }
                }
            }
        } finally {
            r.close();
        }
        return ds;
    }

    private static List<Dong> docSheet(byte[] noiDung, List<String> chuoiChung) throws XMLStreamException {
        TreeMap<Integer, List<O>> dong = new TreeMap<>();
        XMLStreamReader r = moXml(noiDung);
        try {
            int soDongHienTai = 0;
            int soDongTuDong = 0;
            // trạng thái của ô đang đọc
            boolean trongO = false;
            String kieu = null;
            int cot = -1;
            StringBuilder v = new StringBuilder();
            StringBuilder chuoiNoiTuyen = new StringBuilder();
            boolean trongV = false;
            boolean trongT = false;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String ten = r.getLocalName();
                    switch (ten) {
                        case "row" -> {
                            String rr = r.getAttributeValue(null, "r");
                            soDongTuDong++;
                            soDongHienTai = rr != null && rr.matches("\\d+") ? Integer.parseInt(rr) : soDongTuDong;
                            cot = -1; // ô không ghi vị trí (r="B2") thì đếm từ cột A của dòng mới
                            if (dong.size() >= GIOI_HAN_DONG) throw ApiException.badRequest("File Excel có quá nhiều dòng.");
                        }
                        case "c" -> {
                            trongO = true;
                            kieu = r.getAttributeValue(null, "t");
                            String ref = r.getAttributeValue(null, "r");
                            cot = ref == null ? cot + 1 : chiSoCot(ref);
                            v.setLength(0);
                            chuoiNoiTuyen.setLength(0);
                        }
                        case "v" -> trongV = trongO;
                        case "t" -> trongT = trongO; // <is><t> hoặc <is><r><t>
                        default -> { }
                    }
                } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    if (trongV) v.append(r.getText());
                    else if (trongT) chuoiNoiTuyen.append(r.getText());
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String ten = r.getLocalName();
                    switch (ten) {
                        case "v" -> trongV = false;
                        case "t" -> trongT = false;
                        case "c" -> {
                            trongO = false;
                            if (cot >= GIOI_HAN_COT) break;
                            O o = taoO(kieu, v.toString(), chuoiNoiTuyen.toString(), chuoiChung);
                            List<O> ds = dong.computeIfAbsent(soDongHienTai, k -> new ArrayList<>());
                            while (ds.size() < cot) ds.add(O.TRONG);
                            if (ds.size() == cot) ds.add(o);
                            else ds.set(cot, o);
                        }
                        default -> { }
                    }
                }
            }
        } finally {
            r.close();
        }
        List<Dong> ketQua = new ArrayList<>();
        for (Map.Entry<Integer, List<O>> e : dong.entrySet()) {
            Dong d = new Dong(e.getKey(), e.getValue());
            if (!d.trong()) ketQua.add(d);
        }
        return ketQua;
    }

    private static O taoO(String kieu, String v, String noiTuyen, List<String> chuoiChung) {
        if ("inlineStr".equals(kieu)) return new O(noiTuyen, false);
        if (v.isEmpty()) return O.TRONG;
        if ("s".equals(kieu)) {
            try {
                int i = Integer.parseInt(v.trim());
                return new O(i >= 0 && i < chuoiChung.size() ? chuoiChung.get(i) : "", false);
            } catch (NumberFormatException e) {
                return O.TRONG;
            }
        }
        if ("str".equals(kieu) || "d".equals(kieu)) return new O(v, false); // công thức trả chữ / ngày ISO
        if ("b".equals(kieu)) return new O("1".equals(v.trim()) ? "TRUE" : "FALSE", false);
        if ("e".equals(kieu)) return new O("", false); // ô lỗi (#N/A...) coi như trống
        // Ô số: viết ra dạng số thường (không có 4.5E7), bỏ phần ".0"
        try {
            String s = new BigDecimal(v.trim()).stripTrailingZeros().toPlainString();
            return new O(s, true);
        } catch (NumberFormatException e) {
            return new O(v, false);
        }
    }

    /** "C5" -> 2 (cột A = 0). */
    static int chiSoCot(String ref) {
        int n = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (ch >= 'A' && ch <= 'Z') n = n * 26 + (ch - 'A' + 1);
            else if (ch >= 'a' && ch <= 'z') n = n * 26 + (ch - 'a' + 1);
            else break;
        }
        return n - 1;
    }
}
