package com.footstyle.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Chạy thêm một backend khi backend FootStyle khác đang giữ cổng 8081: mặc định bản đang chạy được GIỮ NGUYÊN
 * (bản mới thoát êm), chỉ khi bật FOOTSTYLE_THAY_BAN_CU=true mới tắt bản cũ; chương trình khác thì không bao giờ bị đụng tới.
 */
class GiaiPhongCongTest {

    /** Hệ điều hành giả: ghi lại PID bị tắt, "tắt" = đóng ServerSocket đang giữ cổng trong test. */
    static final class HdhGia implements GiaiPhongCong.HeDieuHanh {
        List<Long> pids = List.of(18608L);
        String chuongTrinh = "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe";
        boolean footStyle = true;
        boolean tatDuoc = true;
        ServerSocket dangGiu;
        final List<Long> daTat = new ArrayList<>();

        @Override
        public List<Long> timPid(int cong) {
            return pids;
        }

        @Override
        public Optional<String> chuongTrinh(long pid) {
            return Optional.ofNullable(chuongTrinh);
        }

        @Override
        public boolean laFootStyle(int cong) {
            return footStyle;
        }

        @Override
        public Optional<Instant> khoiDongLuc(long pid) {
            return Optional.of(Instant.parse("2026-10-13T15:20:11Z"));
        }

        @Override
        public boolean tat(long pid) {
            daTat.add(pid);
            if (tatDuoc && dangGiu != null) {
                try {
                    dangGiu.close();
                } catch (IOException ignored) {
                    // bỏ qua
                }
            }
            return tatDuoc;
        }
    }

    private static PrintStream ghi(ByteArrayOutputStream buf) {
        return new PrintStream(buf, true, StandardCharsets.UTF_8);
    }

    @Test
    void congTrong_khongLamGiCa() throws IOException {
        int cong;
        try (ServerSocket s = new ServerSocket(0)) {
            cong = s.getLocalPort();
        }
        HdhGia hdh = new HdhGia();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        assertEquals(GiaiPhongCong.KetQua.CONG_TRONG, GiaiPhongCong.giaiPhong(cong, false, hdh, Duration.ofSeconds(1), ghi(buf)));
        assertTrue(hdh.daTat.isEmpty());
        assertEquals("", buf.toString(StandardCharsets.UTF_8));
    }

    @Test
    void backendFootStyleDangChay_macDinhGiuNguyen_khongTat() throws IOException {
        try (ServerSocket dangChay = new ServerSocket(0)) {
            int cong = dangChay.getLocalPort();
            HdhGia hdh = new HdhGia();
            hdh.dangGiu = dangChay;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            assertEquals(GiaiPhongCong.KetQua.DA_CHAY_SAN, GiaiPhongCong.giaiPhong(cong, false, hdh, Duration.ofSeconds(3), ghi(buf)));
            assertTrue(hdh.daTat.isEmpty(), "backend dang chay KHONG duoc bi tat");
            assertTrue(GiaiPhongCong.dangBan(cong), "ban dang chay van giu cong");
            String log = buf.toString(StandardCharsets.UTF_8);
            assertTrue(log.contains("Backend FootStyle DA CHAY SAN o cong " + cong), log);
            assertTrue(log.contains("PID 18608"), log);
            assertTrue(log.contains("khoi dong luc"), log);
            assertTrue(log.contains("KHONG phai loi"), log);
            assertTrue(log.contains("FOOTSTYLE_THAY_BAN_CU"), log);
            assertFalse(log.contains("Dang tat"), log);
        }
    }

    @Test
    void apiFootStyleTraLoiNhungKhongTimDuocPid_vanGiuNguyenVaKhongTat() throws IOException {
        try (ServerSocket dangChay = new ServerSocket(0)) {
            HdhGia hdh = new HdhGia();
            hdh.pids = List.of(); // máy không có lệnh netstat / lsof
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            assertEquals(GiaiPhongCong.KetQua.DA_CHAY_SAN,
                    GiaiPhongCong.giaiPhong(dangChay.getLocalPort(), false, hdh, Duration.ofSeconds(1), ghi(buf)));
            assertTrue(hdh.daTat.isEmpty());
            assertTrue(buf.toString(StandardCharsets.UTF_8).contains("DA CHAY SAN"));
        }
    }

    @Test
    void thayBanCuDuocBat_tuTatBanFootStyleCuRoiChoChayTiep() throws IOException {
        ServerSocket cu = new ServerSocket(0);
        int cong = cu.getLocalPort();
        assertTrue(GiaiPhongCong.dangBan(cong));
        HdhGia hdh = new HdhGia();
        hdh.dangGiu = cu;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        assertEquals(GiaiPhongCong.KetQua.DA_GIAI_PHONG, GiaiPhongCong.giaiPhong(cong, true, hdh, Duration.ofSeconds(3), ghi(buf)));
        assertEquals(List.of(18608L), hdh.daTat);
        assertFalse(GiaiPhongCong.dangBan(cong));
        String log = buf.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("Dang tat backend FootStyle cu (PID 18608)"), log);
        assertTrue(log.contains("Tiep tuc khoi dong backend"), log);
    }

    @Test
    void thayBanCuDuocBat_nhungChuongTrinhKhacGiuCong_van_khongTat() throws IOException {
        try (ServerSocket khac = new ServerSocket(0)) {
            HdhGia hdh = new HdhGia();
            hdh.dangGiu = khac;
            hdh.footStyle = false;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            assertEquals(GiaiPhongCong.KetQua.KHONG_GIAI_PHONG_DUOC,
                    GiaiPhongCong.giaiPhong(khac.getLocalPort(), true, hdh, Duration.ofSeconds(1), ghi(buf)));
            assertTrue(hdh.daTat.isEmpty());
        }
    }

    @Test
    void thayBanCuChiBatKhiDatRoLaTrue() {
        assertFalse(GiaiPhongCong.batThayBanCu(null, null));
        assertFalse(GiaiPhongCong.batThayBanCu("", ""));
        assertFalse(GiaiPhongCong.batThayBanCu("false", "no"));
        assertTrue(GiaiPhongCong.batThayBanCu("true", null));
        assertTrue(GiaiPhongCong.batThayBanCu(null, "TRUE"));
    }

    @Test
    void thongBaoKhiTatCoPidVaGio() {
        String t = GiaiPhongCong.thongBaoTat(4321L, LocalTime.of(22, 5, 9));
        assertTrue(t.contains("Backend (PID 4321) da tat luc 22:05:09"), t);
    }

    @Test
    void chuongTrinhKhacGiuCong_khongTat_inHuongDan() throws IOException {
        try (ServerSocket khac = new ServerSocket(0)) {
            int cong = khac.getLocalPort();
            HdhGia hdh = new HdhGia();
            hdh.dangGiu = khac;
            hdh.footStyle = false; // không trả lời API của FootStyle
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            assertEquals(GiaiPhongCong.KetQua.KHONG_GIAI_PHONG_DUOC, GiaiPhongCong.giaiPhong(cong, false, hdh, Duration.ofSeconds(1), ghi(buf)));
            assertTrue(hdh.daTat.isEmpty());
            assertTrue(GiaiPhongCong.dangBan(cong));
            String log = buf.toString(StandardCharsets.UTF_8);
            assertTrue(log.contains("KHONG phai backend FootStyle nen khong tu tat"), log);
            assertTrue(log.contains("giai-phong-cong-8081.bat"), log);
            assertTrue(log.contains("SERVER_PORT=8082"), log);
        }
    }

    @Test
    void khongPhaiTienTrinhJava_khongBaoGioBiTat() throws IOException {
        try (ServerSocket khac = new ServerSocket(0)) {
            HdhGia hdh = new HdhGia();
            hdh.dangGiu = khac;
            hdh.chuongTrinh = "C:\\Program Files\\nodejs\\node.exe";
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            // Bật thay bản cũ: node.exe không phải Java nên vẫn không bị tắt, chỉ in hướng dẫn
            assertEquals(GiaiPhongCong.KetQua.KHONG_GIAI_PHONG_DUOC,
                    GiaiPhongCong.giaiPhong(khac.getLocalPort(), true, hdh, Duration.ofSeconds(1), ghi(buf)));
            assertTrue(hdh.daTat.isEmpty());
            assertTrue(buf.toString(StandardCharsets.UTF_8).contains("node.exe"));
            // Không bật thay bản cũ: không tắt gì cả
            assertEquals(GiaiPhongCong.KetQua.DA_CHAY_SAN,
                    GiaiPhongCong.giaiPhong(khac.getLocalPort(), false, hdh, Duration.ofSeconds(1), ghi(new ByteArrayOutputStream())));
            assertTrue(hdh.daTat.isEmpty());
        }
    }

    @Test
    void tatKhongDuoc_baoLoiSauKhiCho() throws IOException {
        try (ServerSocket cu = new ServerSocket(0)) {
            HdhGia hdh = new HdhGia();
            hdh.dangGiu = cu;
            hdh.tatDuoc = false; // vd tiến trình chạy bằng quyền Administrator
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            assertEquals(GiaiPhongCong.KetQua.KHONG_GIAI_PHONG_DUOC,
                    GiaiPhongCong.giaiPhong(cu.getLocalPort(), true, hdh, Duration.ofMillis(500), ghi(buf)));
            assertEquals(List.of(18608L), hdh.daTat);
            assertTrue(buf.toString(StandardCharsets.UTF_8).contains("van chua trong"));
        }
    }

    @Test
    void chinhTienTrinhNayGiuCong_khongTuTatMinh() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            HdhGia hdh = new HdhGia();
            hdh.pids = List.of(ProcessHandle.current().pid());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            assertEquals(GiaiPhongCong.KetQua.CONG_TRONG, GiaiPhongCong.giaiPhong(s.getLocalPort(), false, hdh, Duration.ofSeconds(1), ghi(buf)));
            assertTrue(hdh.daTat.isEmpty());
        }
    }

    @Test
    void docKetQuaNetstatCuaWindows() {
        String netstat = String.join("\r\n",
                "",
                "Active Connections",
                "",
                "  Proto  Local Address          Foreign Address        State           PID",
                "  TCP    0.0.0.0:135            0.0.0.0:0              LISTENING       1100",
                "  TCP    0.0.0.0:8081           0.0.0.0:0              LISTENING       18608",
                "  TCP    0.0.0.0:18081          0.0.0.0:0              LISTENING       777",
                "  TCP    127.0.0.1:8081         127.0.0.1:52344        ESTABLISHED     18608",
                "  TCP    127.0.0.1:52344        127.0.0.1:8081         ESTABLISHED     9900",
                "  TCP    [::]:8081              [::]:0                 LISTENING       18608",
                "  UDP    0.0.0.0:8081           *:*                                    4444");
        assertEquals(List.of(18608L), GiaiPhongCong.phanTichNetstat(netstat, 8081));
        // Windows tiếng khác: chữ trạng thái khác "LISTENING" vẫn nhận ra nhờ địa chỉ ngoài 0.0.0.0:0
        assertEquals(List.of(5000L), GiaiPhongCong.phanTichNetstat("  TCP    0.0.0.0:8081   0.0.0.0:0   ABHÖREN   5000", 8081));
        assertEquals(List.of(), GiaiPhongCong.phanTichNetstat("", 8081));
    }

    @Test
    void docKetQuaLsofVaSs() {
        assertEquals(List.of(1234L, 5678L), GiaiPhongCong.phanTichLsof("1234\n5678\n1234\nlsof: loi\n"));
        String ss = "LISTEN 0      100    *:8081    *:*    users:((\"java\",pid=4321,fd=45))\n"
                + "LISTEN 0      100    *:18081   *:*    users:((\"java\",pid=99,fd=12))\n";
        assertEquals(List.of(4321L), GiaiPhongCong.phanTichSs(ss, 8081));
    }

    @Test
    void congTheoThuTuUuTienCuaSpring() {
        assertEquals(8081, GiaiPhongCong.congCauHinh(new String[0], null, null));
        assertEquals(8082, GiaiPhongCong.congCauHinh(new String[0], null, "8082"));
        assertEquals(8083, GiaiPhongCong.congCauHinh(new String[0], "8083", "8082"));
        assertEquals(8084, GiaiPhongCong.congCauHinh(new String[] {"--server.port=8084"}, "8083", "8082"));
        assertEquals(8081, GiaiPhongCong.congCauHinh(null, " ", "abc"));
        assertEquals(0, GiaiPhongCong.congCauHinh(new String[] {"--server.port=0"}, null, null));
    }

    @Test
    void nhanRaChuongTrinhJava() {
        assertTrue(GiaiPhongCong.laJava("C:\\Program Files\\Java\\jdk-21\\bin\\java.exe"));
        assertTrue(GiaiPhongCong.laJava("C:\\Users\\a\\.jdks\\corretto-17\\bin\\javaw.exe"));
        assertTrue(GiaiPhongCong.laJava("/usr/lib/jvm/java-17/bin/java"));
        assertFalse(GiaiPhongCong.laJava("C:\\Program Files\\nodejs\\node.exe"));
        assertFalse(GiaiPhongCong.laJava("/usr/bin/javascript"));
        assertFalse(GiaiPhongCong.laJava(""));
        assertFalse(GiaiPhongCong.laJava(null));
    }

    @Test
    void tatTinhNangBangThuocTinhHoacBienMoiTruong() {
        assertTrue(GiaiPhongCong.batTinhNang(null, null));
        assertTrue(GiaiPhongCong.batTinhNang("true", null));
        assertFalse(GiaiPhongCong.batTinhNang("false", null));
        assertFalse(GiaiPhongCong.batTinhNang(null, "FALSE"));
    }

    @Test
    void nhanDienBackendFootStyleQuaApi() throws IOException {
        HttpServer fs = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fs.createContext(GiaiPhongCong.API_NHAN_DIEN, ex -> {
            byte[] b = "{\"daCauHinh\":true,\"dangNhap\":\"THANH_CONG\",\"lyDo\":\"\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        HttpServer khac = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        khac.createContext("/", ex -> {
            byte[] b = "<html>ung dung khac</html>".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        fs.start();
        khac.start();
        try {
            GiaiPhongCong.HeDieuHanhThat that = new GiaiPhongCong.HeDieuHanhThat();
            assertTrue(that.laFootStyle(fs.getAddress().getPort()));
            assertFalse(that.laFootStyle(khac.getAddress().getPort()));
            int trong;
            try (ServerSocket s = new ServerSocket(0)) {
                trong = s.getLocalPort();
            }
            assertFalse(that.laFootStyle(trong)); // không có gì lắng nghe
        } finally {
            fs.stop(0);
            khac.stop(0);
        }
    }

    @Test
    void timDuocPidCuaTienTrinhDangLangNghe_neuMayCoLenhHeThong() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            List<Long> pids = new GiaiPhongCong.HeDieuHanhThat().timPid(s.getLocalPort());
            // Máy không có netstat / lsof / ss thì danh sách rỗng; có thì phải là chính tiến trình test này
            assertTrue(pids.isEmpty() || pids.contains(ProcessHandle.current().pid()), pids.toString());
            assertTrue(new GiaiPhongCong.HeDieuHanhThat().chuongTrinh(ProcessHandle.current().pid())
                    .map(GiaiPhongCong::laJava).orElse(true));
        }
    }
}
