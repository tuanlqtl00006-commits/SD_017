package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.XlsxDoc.Dong;
import com.footstyle.demo.service.XlsxDoc.O;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Đọc file .xlsx chỉ bằng JDK (ZIP + StAX): kiểm tra với file thật do openpyxl / xlsxwriter tạo (hai cách lưu chữ khác nhau)
 * và với những file "lạ" ghép tay: thực thể XML độc hại, file bom giải nén, sheet đảo thứ tự, ô không ghi vị trí...
 */
class XlsxDocTest {

    private static ApiException loi(byte[] data) {
        return assertThrows(ApiException.class, () -> XlsxDoc.doc(data));
    }

    private static List<String> chu(Dong d, int tuCot, int denCot) {
        return java.util.stream.IntStream.range(tuCot, denCot).mapToObj(i -> d.o(i).giaTri()).toList();
    }

    // ---------- file thật ----------

    @Test
    void docFileOpenpyxl_chuNoiTuyenSoVaNgaySerial() {
        List<Dong> ds = XlsxDoc.doc(XlsxMau.nap("nhan-vien-hop-le.xlsx"));
        assertEquals(4, ds.size());
        assertEquals(List.of(1, 2, 3, 4), ds.stream().map(Dong::soDong).toList());
        assertEquals("Họ và tên (*)", ds.get(0).o(0).giaTri());
        assertEquals("Tỉnh/Thành phố (*)", ds.get(0).o(10).giaTri());

        Dong an = ds.get(1);
        assertEquals("Nguyễn Văn An", an.o(0).giaTri());
        assertFalse(an.o(0).laSo());
        assertEquals(new O("36300", true), an.o(2), "ngày 20/05/1999 là số serial 36300");
        assertEquals(new O("912345678", true), an.o(3), "Excel làm mất số 0 đầu của số điện thoại: vẫn là ô SỐ");
        assertEquals("001099012345", an.o(5).giaTri(), "căn cước gõ dạng chữ giữ nguyên số 0 đầu");
        assertFalse(an.o(5).laSo());
        assertEquals(new O("45663", true), an.o(6));
        assertTrue(an.o(7).giaTri().equals("Nhân viên"));

        Dong anh = ds.get(3);
        assertEquals("098 765 4322", anh.o(3).giaTri(), "giữ nguyên khoảng trắng, việc làm sạch thuộc về bước kiểm tra");
        assertEquals(new O("79203001299", true), anh.o(5));
        assertTrue(anh.o(6).trong(), "ô để trống");
        assertTrue(anh.o(7).trong());
    }

    @Test
    void docFileXlsxwriter_chuoiDungChungBoQuaDongTrongVaChiDocSheetDau() {
        List<Dong> ds = XlsxDoc.doc(XlsxMau.nap("nhan-vien-hop-le-shared.xlsx"));
        // Dòng 2 và 4 hoàn toàn trống nên bị bỏ qua, nhưng số dòng THẬT của Excel vẫn được giữ (để báo lỗi đúng dòng)
        assertEquals(List.of(1, 3, 5), ds.stream().map(Dong::soDong).toList());
        assertEquals("Email (*)", ds.get(0).o(4).giaTri());
        Dong an = ds.get(1);
        assertEquals("Nguyễn Văn An", an.o(0).giaTri());
        assertEquals(new O("36300", true), an.o(2));
        assertEquals(new O("912345678", true), an.o(3));
        Dong binh = ds.get(2);
        assertEquals("Lê Thị Bình", binh.o(0).giaTri());
        assertEquals("25/12/2001", binh.o(2).giaTri());
        assertFalse(binh.o(2).laSo());
        assertEquals("0987654321", binh.o(3).giaTri());
        assertEquals("Quản lý", binh.o(7).giaTri());
        assertTrue(ds.stream().noneMatch(d -> d.o(0).giaTri().contains("không đọc")), "sheet thứ hai không được đọc");
    }

    @Test
    void oNgoaiPhamViVaDongTrongTraVeOTrong() {
        List<Dong> ds = XlsxDoc.doc(XlsxMau.tao(Arrays.asList("A", null, "C")));
        Dong d = ds.get(0);
        assertTrue(d.o(1).trong());
        assertTrue(d.o(50).trong());
        assertTrue(d.o(-1).trong());
        assertEquals("C", d.o(2).giaTri());
        assertFalse(d.trong());
    }

    // ---------- dựng tay ----------

    @Test
    void sheetDauTienTheoThuTuTrongWorkbook_khongPhaiTheoTenTep() {
        // Workbook liệt kê "Thật" (rId2 -> sheet2.xml) TRƯỚC "Phụ" (rId1 -> sheet1.xml): sheet đầu tiên là sheet2.xml
        Map<String, String> tep = new LinkedHashMap<>();
        tep.put("xl/workbook.xml", "<workbook " + XlsxMau.NS + "><sheets><sheet name=\"Thật\" sheetId=\"2\" r:id=\"rId2\"/><sheet name=\"Phụ\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        tep.put("xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Target=\"/xl/worksheets/sheet2.xml\"/></Relationships>");
        tep.put("xl/worksheets/sheet1.xml", XlsxMau.sheetXml(List.of(List.of("PHU")), 1));
        tep.put("xl/worksheets/sheet2.xml", XlsxMau.sheetXml(List.of(List.of("THAT")), 1));
        List<Dong> ds = XlsxDoc.doc(XlsxMau.zip(tep));
        assertEquals("THAT", ds.get(0).o(0).giaTri());
    }

    @Test
    void chuoiDungChungCoDinhDangRichText_ghepCacDoan() {
        Map<String, String> tep = XlsxMau.sheetMotTep("<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\">"
                + "<c r=\"A1\" t=\"s\"><v>1</v></c><c r=\"B1\" t=\"s\"><v>0</v></c><c r=\"C1\" t=\"s\"><v>9</v></c></row></sheetData></worksheet>");
        tep.put("xl/sharedStrings.xml", "<sst " + XlsxMau.NS + " count=\"2\" uniqueCount=\"2\">"
                + "<si><t>Đơn giản</t></si>"
                + "<si><r><rPr><b/></rPr><t xml:space=\"preserve\">Họ </t></r><r><t>tên</t></r></si></sst>");
        Dong d = XlsxDoc.doc(XlsxMau.zip(tep)).get(0);
        assertEquals("Họ tên", d.o(0).giaTri());
        assertEquals("Đơn giản", d.o(1).giaTri());
        assertEquals("", d.o(2).giaTri(), "chỉ số chuỗi ngoài phạm vi thì coi như ô trống, không lỗi");
    }

    @Test
    void cacKieuOKhac_congThucLogicLoiVaSoMu() {
        String sheet = "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\">"
                + "<c r=\"A1\" t=\"str\"><f>CONCAT(\"a\",\"b\")</f><v>ab</v></c>"
                + "<c r=\"B1\" t=\"b\"><v>1</v></c>"
                + "<c r=\"C1\" t=\"b\"><v>0</v></c>"
                + "<c r=\"D1\" t=\"e\"><v>#N/A</v></c>"
                + "<c r=\"E1\"><v>4.5E7</v></c>"
                + "<c r=\"F1\"><v>912345678.0</v></c>"
                + "<c r=\"G1\"><v>0.25</v></c>"
                + "<c r=\"H1\" t=\"d\"><v>2000-02-03T00:00:00</v></c>"
                + "</row></sheetData></worksheet>";
        Dong d = XlsxDoc.doc(XlsxMau.zip(XlsxMau.sheetMotTep(sheet))).get(0);
        assertEquals(new O("ab", false), d.o(0));
        assertEquals(new O("TRUE", false), d.o(1));
        assertEquals(new O("FALSE", false), d.o(2));
        assertTrue(d.o(3).trong(), "ô lỗi #N/A coi như trống");
        assertEquals(new O("45000000", true), d.o(4), "không để dạng 4.5E7");
        assertEquals(new O("912345678", true), d.o(5), "bỏ phần .0");
        assertEquals(new O("0.25", true), d.o(6));
        assertEquals(new O("2000-02-03T00:00:00", false), d.o(7));
    }

    @Test
    void oKhongGhiViTri_demTuCotADauMoiDong() {
        // Một số phần mềm xuất file không ghi r="A1": phải đếm lại từ cột A ở MỖI dòng
        String sheet = "<worksheet " + XlsxMau.NS + "><sheetData>"
                + "<row><c t=\"inlineStr\"><is><t>a1</t></is></c><c t=\"inlineStr\"><is><t>b1</t></is></c></row>"
                + "<row><c t=\"inlineStr\"><is><t>a2</t></is></c><c t=\"inlineStr\"><is><t>b2</t></is></c><c t=\"inlineStr\"><is><t>c2</t></is></c></row>"
                + "</sheetData></worksheet>";
        List<Dong> ds = XlsxDoc.doc(XlsxMau.zip(XlsxMau.sheetMotTep(sheet)));
        assertEquals(List.of(1, 2), ds.stream().map(Dong::soDong).toList());
        assertEquals(List.of("a1", "b1"), chu(ds.get(0), 0, 2));
        assertEquals(List.of("a2", "b2", "c2"), chu(ds.get(1), 0, 3));
    }

    @Test
    void oBoCotGiuaChung_cacOSauVanDungCot() {
        String sheet = "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"3\">"
                + "<c r=\"B3\" t=\"inlineStr\"><is><t>b</t></is></c><c r=\"E3\" t=\"inlineStr\"><is><t>e</t></is></c></row></sheetData></worksheet>";
        Dong d = XlsxDoc.doc(XlsxMau.zip(XlsxMau.sheetMotTep(sheet))).get(0);
        assertEquals(3, d.soDong());
        assertTrue(d.o(0).trong());
        assertEquals("b", d.o(1).giaTri());
        assertTrue(d.o(2).trong());
        assertTrue(d.o(3).trong());
        assertEquals("e", d.o(4).giaTri());
    }

    @Test
    void kyTuDacBietVaChuTrongThe_tIsRichText() {
        String sheet = "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\">"
                + "<c r=\"A1\" t=\"inlineStr\"><is><r><t>Lê </t></r><r><t>Văn &amp; Sơn</t></r></is></c></row></sheetData></worksheet>";
        Dong d = XlsxDoc.doc(XlsxMau.zip(XlsxMau.sheetMotTep(sheet))).get(0);
        assertEquals("Lê Văn & Sơn", d.o(0).giaTri());
    }

    @Test
    void chiSoCot() {
        assertEquals(0, XlsxDoc.chiSoCot("A1"));
        assertEquals(1, XlsxDoc.chiSoCot("B2"));
        assertEquals(25, XlsxDoc.chiSoCot("Z9"));
        assertEquals(26, XlsxDoc.chiSoCot("AA1"));
        assertEquals(27, XlsxDoc.chiSoCot("AB10"));
        assertEquals(16383, XlsxDoc.chiSoCot("XFD1"));
        assertEquals(2, XlsxDoc.chiSoCot("c5"));
    }

    @Test
    void xlsxMauTuDungTuongThichVoiXlsxDoc() {
        byte[] f = XlsxMau.tao(XlsxMau.TIEU_DE, XlsxMau.nhanVien("Lê Văn Sơn", "son@x.vn", "0912345678"),
                Arrays.asList("Chữ & <đặc biệt>", 912345678, LocalDate.of(1999, 5, 20)));
        List<Dong> ds = XlsxDoc.doc(f);
        assertEquals(3, ds.size());
        assertEquals("Lê Văn Sơn", ds.get(1).o(0).giaTri());
        assertEquals("Chữ & <đặc biệt>", ds.get(2).o(0).giaTri());
        assertEquals(new O("912345678", true), ds.get(2).o(1));
        assertEquals(new O("36300", true), ds.get(2).o(2));
    }

    // ---------- file sai / độc hại ----------

    @Test
    void khongPhaiFileXlsx_baoLoi400DeHieu() {
        for (byte[] data : new byte[][] {null, new byte[0], new byte[] {1, 2, 3}, "Họ tên,Email\nAn,a@x.vn".getBytes(StandardCharsets.UTF_8),
                "%PDF-1.4 ...".getBytes(StandardCharsets.UTF_8)}) {
            ApiException e = loi(data);
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            assertTrue(e.getMessage().contains(".xlsx"), e.getMessage());
        }
    }

    @Test
    void zipNhungKhongCoWorkbook_laFileKhac() {
        byte[] zip = XlsxMau.zip(Map.of("hello.txt", "xin chào"));
        ApiException e = loi(zip);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains(".xlsx"));
    }

    @Test
    void fileXlsxBiCutDo_khongBaoGioNemLoiLa() {
        byte[] tot = XlsxMau.nap("nhan-vien-hop-le.xlsx");
        int soLanBaoLoi = 0;
        // Cắt ở mọi vị trí (bước 37 byte): hoặc đọc được đủ dữ liệu (cắt phần đuôi không cần), hoặc báo lỗi 400 dễ hiểu - không bao giờ ném lỗi lạ
        for (int dai = 0; dai < tot.length; dai += 37) {
            byte[] hong = Arrays.copyOf(tot, dai);
            try {
                XlsxDoc.doc(hong);
            } catch (ApiException e) {
                soLanBaoLoi++;
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatus(), "độ dài " + dai);
            }
        }
        assertTrue(soLanBaoLoi > 5, "file cụt phải bị từ chối: " + soLanBaoLoi);
        // Cắt ngay giữa file thì chắc chắn thiếu dữ liệu
        assertEquals(HttpStatus.BAD_REQUEST, loi(Arrays.copyOf(tot, tot.length / 3)).getStatus());
    }

    @Test
    void xmlHong_baoLoi400() {
        byte[] f = XlsxMau.zip(XlsxMau.sheetMotTep("<worksheet><sheetData><row r=\"1\"><c r=\"A1\"><v>1</v></row></sheetData></worksheet>"));
        ApiException e = loi(f);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("hỏng"), e.getMessage());
    }

    @Test
    void thucTheXmlNgoai_XXE_khongDocDuocTepTrenMay(@org.junit.jupiter.api.io.TempDir Path thuMuc) throws IOException {
        Path bimat = thuMuc.resolve("bi-mat.txt");
        Files.writeString(bimat, "MAT-KHAU-BI-MAT-TREN-MAY-CHU");
        String dtd = "<?xml version=\"1.0\"?><!DOCTYPE sst [<!ENTITY xxe SYSTEM \"" + bimat.toUri() + "\">]>";
        String sheet = dtd + "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>&xxe;</t></is></c></row></sheetData></worksheet>";
        byte[] f = XlsxMau.zip(XlsxMau.sheetMotTep(sheet));
        try {
            List<Dong> ds = XlsxDoc.doc(f);
            // Nếu bộ đọc bỏ qua thực thể thì ô phải KHÔNG chứa nội dung tệp bí mật
            assertTrue(ds.stream().flatMap(d -> d.o().stream()).noneMatch(o -> o.giaTri().contains("MAT-KHAU")), "lộ nội dung tệp trên máy chủ!");
        } catch (ApiException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus()); // hoặc bị từ chối hẳn: cũng an toàn
        }
    }

    @Test
    void thucTheXmlNoCapSoNhan_billionLaughs_khongTreoMay() {
        String dtd = "<?xml version=\"1.0\"?><!DOCTYPE lolz [<!ENTITY lol \"lol\"><!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">"
                + "<!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\"><!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">]>";
        String sheet = dtd + "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>&lol4;</t></is></c></row></sheetData></worksheet>";
        byte[] f = XlsxMau.zip(XlsxMau.sheetMotTep(sheet));
        long bat = System.nanoTime();
        try {
            List<Dong> ds = XlsxDoc.doc(f);
            assertTrue(ds.stream().flatMap(d -> d.o().stream()).allMatch(o -> o.giaTri().length() < 10_000));
        } catch (ApiException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
        assertTrue((System.nanoTime() - bat) / 1_000_000 < 5_000, "xử lý quá lâu");
    }

    @Test
    void fileBomGiaiNen_biChanSomVaBaoLoi400() {
        // ~31 MB toàn số 0 nén lại chỉ vài chục KB: nếu không giới hạn sẽ ăn hết bộ nhớ
        String phanDau = "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>";
        StringBuilder sb = new StringBuilder(phanDau);
        sb.append("0".repeat(31 * 1024 * 1024)).append("</t></is></c></row></sheetData></worksheet>");
        byte[] f = XlsxMau.zip(XlsxMau.sheetMotTep(sb.toString()));
        assertTrue(f.length < 200_000, "file nén nhỏ: " + f.length);
        ApiException e = loi(f);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("quá lớn"), e.getMessage());
    }

    @Test
    void quaNhieuDongTrongSheet_biTuChoi() {
        StringBuilder sb = new StringBuilder("<worksheet " + XlsxMau.NS + "><sheetData>");
        for (int i = 1; i <= 5001; i++) {
            sb.append("<row r=\"").append(i).append("\"><c r=\"A").append(i).append("\"><v>").append(i).append("</v></c></row>");
        }
        sb.append("</sheetData></worksheet>");
        ApiException e = loi(XlsxMau.zip(XlsxMau.sheetMotTep(sb.toString())));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("quá nhiều dòng"));
        // đúng 5000 dòng thì còn đọc được
        StringBuilder vua = new StringBuilder("<worksheet " + XlsxMau.NS + "><sheetData>");
        for (int i = 1; i <= 5000; i++) {
            vua.append("<row r=\"").append(i).append("\"><c r=\"A").append(i).append("\"><v>").append(i).append("</v></c></row>");
        }
        vua.append("</sheetData></worksheet>");
        assertEquals(5000, XlsxDoc.doc(XlsxMau.zip(XlsxMau.sheetMotTep(vua.toString()))).size());
    }

    @Test
    void cotQua100_boQuaKhongLoi() {
        String sheet = "<worksheet " + XlsxMau.NS + "><sheetData><row r=\"1\">"
                + "<c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c><c r=\"DZ1\" t=\"inlineStr\"><is><t>xa</t></is></c></row></sheetData></worksheet>";
        Dong d = XlsxDoc.doc(XlsxMau.zip(XlsxMau.sheetMotTep(sheet))).get(0);
        assertEquals("a", d.o(0).giaTri());
        assertTrue(d.o().size() <= 100);
    }

    @Test
    void khongCoSheetNaoTrongRels_vanThuSheet1() {
        // workbook.xml không có rels: rơi về xl/worksheets/sheet1.xml
        Map<String, String> tep = new LinkedHashMap<>();
        tep.put("xl/workbook.xml", "<workbook " + XlsxMau.NS + "><sheets><sheet name=\"S\" sheetId=\"1\" r:id=\"rId9\"/></sheets></workbook>");
        tep.put("xl/worksheets/sheet1.xml", XlsxMau.sheetXml(List.of(List.of("ok")), 1));
        assertEquals("ok", XlsxDoc.doc(XlsxMau.zip(tep)).get(0).o(0).giaTri());
    }

    @Test
    void workbookTroToSheetKhongCo_baoLoiRoRang() {
        Map<String, String> tep = XlsxMau.sheetMotTep("<worksheet/>");
        tep.remove("xl/worksheets/sheet1.xml");
        ApiException e = loi(XlsxMau.zip(tep));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertTrue(e.getMessage().contains("sheet"));
    }
}
