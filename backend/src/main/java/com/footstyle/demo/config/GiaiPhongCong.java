package com.footstyle.demo.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chạy TRƯỚC khi Spring Boot khởi động (gọi đầu tiên trong {@code BackendApplication.main}).
 *
 * <p>Tình huống hay gặp: backend FootStyle đã chạy sẵn (bấm Run trong IntelliJ, hoặc một cửa sổ terminal chưa tắt) rồi lại chạy
 * thêm một bản nữa ({@code ./mvnw spring-boot:run} hoặc bấm Run lần nữa) -> "Port 8081 was already in use" -> BUILD FAILURE.
 *
 * <p>Lớp này kiểm tra cổng và KHÔNG BAO GIỜ tự tắt backend đang chạy (bản cũ từng bị tắt ngầm khi chạy thêm một bản,
 * nên backend "tự nhiên biến mất"):
 * <ul>
 *   <li>cổng trống -> chạy bình thường;</li>
 *   <li>cổng đang bị backend FootStyle khác giữ (tiến trình Java VÀ trả lời đúng API {@value #API_NHAN_DIEN}) -> bản đang chạy
 *       được GIỮ NGUYÊN, bản mới in thông báo "đã chạy sẵn, dùng luôn bản đó" rồi thoát êm (mã thoát 0, không phải lỗi);</li>
 *   <li>chương trình khác chiếm cổng -> không tắt, in hướng dẫn rồi dừng (mã thoát 1).</li>
 * </ul>
 *
 * <p>Muốn bản mới THAY bản cũ (vd vừa sửa code Java): bấm Stop ở bản cũ, hoặc chạy {@code giai-phong-cong-8081.bat},
 * hoặc bật {@code -Dfootstyle.thay-ban-cu=true} / biến môi trường {@code FOOTSTYLE_THAY_BAN_CU=true} để bản mới tự tắt bản FootStyle cũ.
 * Không muốn kiểm tra cổng nữa: {@code -Dfootstyle.tu-giai-phong-cong=false} hoặc {@code FOOTSTYLE_TU_GIAI_PHONG_CONG=false}.
 *
 * <p>Khi backend tắt bình thường (Stop, Ctrl+C) sẽ in một dòng "[FootStyle] Backend (PID ...) da tat luc ...". Backend biến mất
 * mà KHÔNG có dòng này nghĩa là tiến trình bị tắt cưỡng bức từ bên ngoài (đóng cửa sổ terminal, Task Manager, bản khác tắt...).
 *
 * <p>Thông báo in ra không dấu vì cửa sổ PowerShell / cmd trên Windows thường không hiện được tiếng Việt có dấu.
 */
public final class GiaiPhongCong {

    static final int CONG_MAC_DINH = 8081;
    static final String API_NHAN_DIEN = "/api/phieu-giam-gia/trang-thai-mail";
    private static final String TIEN_TO = "[FootStyle] ";
    private static final String DA_KIEM_TRA = "footstyle.da-kiem-tra-cong";

    /**
     * Kết quả kiểm tra cổng. CONG_TRONG: chạy bình thường. DA_CHAY_SAN: đã có backend FootStyle chạy sẵn, giữ nguyên nó và thoát êm.
     * DA_GIAI_PHONG: (chỉ khi bật thay bản cũ) đã tắt bản cũ. KHONG_GIAI_PHONG_DUOC: cổng bị chương trình khác giữ, dừng.
     */
    enum KetQua { CONG_TRONG, DA_CHAY_SAN, DA_GIAI_PHONG, KHONG_GIAI_PHONG_DUOC }

    /** Các bước phụ thuộc hệ điều hành / mạng, tách ra để test được. */
    interface HeDieuHanh {
        /** PID các tiến trình đang LẮNG NGHE trên cổng. */
        List<Long> timPid(int cong);

        /** Đường dẫn chương trình của tiến trình (vd C:\...\bin\java.exe), rỗng nếu không đọc được. */
        Optional<String> chuongTrinh(long pid);

        /** Cổng có trả lời đúng API nhận diện của FootStyle không. */
        boolean laFootStyle(int cong);

        /** Tắt tiến trình, trả về true nếu lệnh tắt được chấp nhận. */
        boolean tat(long pid);

        /** Giờ tiến trình được khởi động (chỉ để in thông báo), rỗng nếu không đọc được. */
        default Optional<Instant> khoiDongLuc(long pid) {
            return Optional.empty();
        }
    }

    private GiaiPhongCong() {
    }

    /** Gọi trong main(). Lỗi bất ngờ ở đây không được chặn backend khởi động. */
    public static void truocKhiChay(String[] args) {
        KetQua kq;
        try {
            // Chỉ kiểm tra lần khởi động đầu của tiến trình: DevTools tự khởi động lại (đổi code) sẽ gọi lại main()
            if (System.getProperty(DA_KIEM_TRA) != null) {
                return;
            }
            System.setProperty(DA_KIEM_TRA, "true");
            if (!batTinhNang(System.getProperty("footstyle.tu-giai-phong-cong"), System.getenv("FOOTSTYLE_TU_GIAI_PHONG_CONG"))) {
                return;
            }
            int cong = congCauHinh(args, System.getProperty("server.port"), System.getenv("SERVER_PORT"));
            if (cong <= 0) {
                return; // server.port=0 (cổng ngẫu nhiên) thì không cần kiểm tra
            }
            boolean thayBanCu = batThayBanCu(System.getProperty("footstyle.thay-ban-cu"), System.getenv("FOOTSTYLE_THAY_BAN_CU"));
            kq = giaiPhong(cong, thayBanCu, new HeDieuHanhThat(), Duration.ofSeconds(15), System.out);
        } catch (RuntimeException e) {
            return;
        }
        if (kq == KetQua.KHONG_GIAI_PHONG_DUOC) {
            System.exit(1);
        }
        if (kq == KetQua.DA_CHAY_SAN) {
            System.exit(0); // đã có backend chạy sẵn và vẫn đang chạy bình thường: không phải lỗi
        }
        ghiLogKhiTat();
    }

    /** In 1 dòng khi backend tắt bình thường (Stop / Ctrl+C), để phân biệt với bị tắt cưỡng bức từ bên ngoài (không có dòng này). */
    private static void ghiLogKhiTat() {
        try {
            long pid = ProcessHandle.current().pid();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println(thongBaoTat(pid, LocalTime.now()));
                    System.out.flush();
                } catch (RuntimeException ignored) {
                    // đang tắt: không làm gì thêm
                }
            }, "footstyle-ghi-log-tat"));
        } catch (RuntimeException ignored) {
            // không đăng ký được thì thôi, không ảnh hưởng backend
        }
    }

    static String thongBaoTat(long pid, LocalTime luc) {
        return TIEN_TO + "Backend (PID " + pid + ") da tat luc " + luc.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + " (tat binh thuong: Stop / Ctrl+C / dong chuong trinh).";
    }

    /**
     * Kiểm tra cổng. Backend FootStyle đang chạy sẵn thì mặc định được GIỮ NGUYÊN (DA_CHAY_SAN); chỉ khi {@code thayBanCu}
     * (bật chủ động) mới tắt bản cũ để chạy bản mới. Chương trình khác chiếm cổng thì không bao giờ bị tắt.
     */
    static KetQua giaiPhong(int cong, boolean thayBanCu, HeDieuHanh hdh, Duration choToiDa, PrintStream out) {
        if (!dangBan(cong)) {
            return KetQua.CONG_TRONG;
        }
        out.println(TIEN_TO + "Cong " + cong + " dang bi chiem. Kiem tra xem co phai backend FootStyle dang chay san khong...");
        long toi = ProcessHandle.current().pid();
        List<Long> tatCa = hdh.timPid(cong);
        List<Long> pids = tatCa.stream().filter(p -> p != toi).distinct().toList();
        if (pids.isEmpty() && (!tatCa.isEmpty() || !dangBan(cong))) {
            return KetQua.CONG_TRONG; // chính tiến trình này đang giữ cổng, hoặc cổng vừa được nhả ra
        }
        boolean footStyle = hdh.laFootStyle(cong);
        List<Long> javaPids = new ArrayList<>();
        List<String> khac = new ArrayList<>();
        for (long pid : pids) {
            Optional<String> ct = hdh.chuongTrinh(pid);
            if (ct.map(GiaiPhongCong::laJava).orElse(false)) {
                javaPids.add(pid);
            } else {
                khac.add("PID " + pid + (ct.isPresent() ? " (" + ct.get() + ")" : ""));
            }
        }

        if (footStyle && !thayBanCu) {
            // Đã có backend FootStyle chạy sẵn (API trả lời đúng): giữ nguyên, KHÔNG tắt. Không cần biết PID mới kết luận được.
            baoDaChaySan(cong, pids, hdh, out);
            return KetQua.DA_CHAY_SAN;
        }

        if (!footStyle || javaPids.isEmpty()) {
            huongDan(cong, out, pids.isEmpty()
                    ? "Khong tim duoc chuong trinh nao dang chiem cong " + cong + "."
                    : "Chuong trinh dang chiem cong " + cong + " KHONG phai backend FootStyle nen khong tu tat: "
                        + String.join(", ", khac.isEmpty() ? pids.stream().map(p -> "PID " + p).toList() : khac) + ".");
            return KetQua.KHONG_GIAI_PHONG_DUOC;
        }

        for (long pid : javaPids) {
            out.println(TIEN_TO + "Dang tat backend FootStyle cu (PID " + pid + ") de chay ban moi (do FOOTSTYLE_THAY_BAN_CU=true)...");
            hdh.tat(pid);
        }
        long han = System.nanoTime() + choToiDa.toNanos();
        while (System.nanoTime() < han) {
            if (!dangBan(cong)) {
                out.println(TIEN_TO + "Da tat ban cu, cong " + cong + " da trong. Tiep tuc khoi dong backend...");
                return KetQua.DA_GIAI_PHONG;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        huongDan(cong, out, "Da gui lenh tat backend cu (PID " + javaPids + ") nhung cong " + cong + " van chua trong.");
        return KetQua.KHONG_GIAI_PHONG_DUOC;
    }

    private static void baoDaChaySan(int cong, List<Long> pids, HeDieuHanh hdh, PrintStream out) {
        String ban = pids.isEmpty() ? "" : " (" + pids.stream().map(p -> {
            String gio = hdh.khoiDongLuc(p)
                    .map(i -> ", khoi dong luc " + DateTimeFormatter.ofPattern("HH:mm:ss").format(i.atZone(ZoneId.systemDefault())))
                    .orElse("");
            return "PID " + p + gio;
        }).collect(java.util.stream.Collectors.joining("; ")) + ")";
        out.println(TIEN_TO + "==================================================================");
        out.println(TIEN_TO + "Backend FootStyle DA CHAY SAN o cong " + cong + ban + ".");
        out.println(TIEN_TO + "Khong tat ban dang chay: cu dung luon ban do (web van goi duoc http://localhost:" + cong + ").");
        out.println(TIEN_TO + "Ban chay them nay thoat ngay, day KHONG phai loi.");
        out.println(TIEN_TO + "Muon chay lai ban moi (vd vua sua code Java): bam Stop o ban cu (IntelliJ / Ctrl+C o cua so terminal) roi chay lai,");
        out.println(TIEN_TO + "  hoac chay giai-phong-cong-8081.bat, hoac cho ban moi tu tat ban cu:");
        out.println(TIEN_TO + "  $env:FOOTSTYLE_THAY_BAN_CU='true'; ./mvnw spring-boot:run");
        out.println(TIEN_TO + "==================================================================");
    }

    private static void huongDan(int cong, PrintStream out, String lyDo) {
        out.println(TIEN_TO + "==================================================================");
        out.println(TIEN_TO + "KHONG KHOI DONG DUOC: " + lyDo);
        out.println(TIEN_TO + "Cach xu ly:");
        out.println(TIEN_TO + "  1. Tat chuong trinh dang dung cong " + cong + " (vd backend dang chay trong IntelliJ: bam nut Stop),");
        out.println(TIEN_TO + "     hoac chay file giai-phong-cong-8081.bat o thu muc goc project, roi chay lai.");
        out.println(TIEN_TO + "  2. Hoac chay cong khac:  $env:SERVER_PORT=8082; ./mvnw spring-boot:run");
        out.println(TIEN_TO + "     (frontend: $env:VITE_API_TARGET='http://localhost:8082'; npm run dev)");
        out.println(TIEN_TO + "==================================================================");
    }

    /** Cổng có đang bị chương trình khác giữ không (thử mở đúng cổng đó như Tomcat sẽ làm). */
    static boolean dangBan(int cong) {
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress(cong));
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /** Cổng backend sẽ dùng: --server.port=... > -Dserver.port=... > biến môi trường SERVER_PORT > 8081. */
    static int congCauHinh(String[] args, String thuocTinh, String bienMoiTruong) {
        String giaTri = null;
        if (args != null) {
            for (String a : args) {
                if (a != null && a.startsWith("--server.port=")) {
                    giaTri = a.substring("--server.port=".length());
                }
            }
        }
        if (giaTri == null) {
            giaTri = thuocTinh;
        }
        if (giaTri == null || giaTri.isBlank()) {
            giaTri = bienMoiTruong;
        }
        if (giaTri == null || giaTri.isBlank()) {
            return CONG_MAC_DINH;
        }
        try {
            return Integer.parseInt(giaTri.trim());
        } catch (NumberFormatException e) {
            return CONG_MAC_DINH;
        }
    }

    static boolean batTinhNang(String thuocTinh, String bienMoiTruong) {
        return !"false".equalsIgnoreCase(String.valueOf(thuocTinh).trim())
                && !"false".equalsIgnoreCase(String.valueOf(bienMoiTruong).trim());
    }

    /** Chỉ khi đặt rõ "true" mới cho bản mới tự tắt backend FootStyle cũ (mặc định KHÔNG tắt). */
    static boolean batThayBanCu(String thuocTinh, String bienMoiTruong) {
        return "true".equalsIgnoreCase(String.valueOf(thuocTinh).trim())
                || "true".equalsIgnoreCase(String.valueOf(bienMoiTruong).trim());
    }

    /** java / java.exe / javaw.exe (IntelliJ và Maven đều chạy backend bằng chương trình java). */
    static boolean laJava(String chuongTrinh) {
        if (chuongTrinh == null || chuongTrinh.isBlank()) {
            return false;
        }
        String duongDan = chuongTrinh.trim();
        String ten = duongDan.substring(Math.max(duongDan.lastIndexOf('/'), duongDan.lastIndexOf('\\')) + 1);
        return ten.toLowerCase(Locale.ROOT).matches("javaw?(\\.exe)?");
    }

    /** Kết quả "netstat -ano" (Windows): PID các dòng TCP đang lắng nghe trên cổng (địa chỉ ngoài 0.0.0.0:0 / [::]:0). */
    static List<Long> phanTichNetstat(String ketQua, int cong) {
        Set<Long> pids = new LinkedHashSet<>();
        for (String dong : ketQua.split("\\R")) {
            String[] t = dong.trim().split("\\s+");
            if (t.length < 5 || !t[0].toUpperCase(Locale.ROOT).startsWith("TCP")) {
                continue;
            }
            boolean dungCong = t[1].endsWith(":" + cong);
            boolean langNghe = t[2].equals("0.0.0.0:0") || t[2].equals("[::]:0") || t[2].equals("*:*");
            if (dungCong && langNghe) {
                try {
                    pids.add(Long.parseLong(t[t.length - 1]));
                } catch (NumberFormatException ignored) {
                    // dòng tiêu đề / dòng lạ
                }
            }
        }
        return new ArrayList<>(pids);
    }

    /** Kết quả "lsof -t" (macOS / Linux): mỗi dòng một PID. */
    static List<Long> phanTichLsof(String ketQua) {
        Set<Long> pids = new LinkedHashSet<>();
        for (String dong : ketQua.split("\\R")) {
            if (dong.trim().matches("\\d+")) {
                pids.add(Long.parseLong(dong.trim()));
            }
        }
        return new ArrayList<>(pids);
    }

    /** Kết quả "ss -ltnp" (Linux không có lsof): lấy pid=... của dòng lắng nghe trên cổng. */
    static List<Long> phanTichSs(String ketQua, int cong) {
        Set<Long> pids = new LinkedHashSet<>();
        Pattern pid = Pattern.compile("pid=(\\d+)");
        for (String dong : ketQua.split("\\R")) {
            String[] t = dong.trim().split("\\s+");
            boolean dungCong = false;
            for (String o : t) {
                if (o.endsWith(":" + cong)) {
                    dungCong = true;
                    break;
                }
            }
            if (!dungCong) {
                continue;
            }
            Matcher m = pid.matcher(dong);
            while (m.find()) {
                pids.add(Long.parseLong(m.group(1)));
            }
        }
        return new ArrayList<>(pids);
    }

    /** Cài đặt thật: netstat (Windows) / lsof, ss (macOS, Linux), ProcessHandle, HTTP tới 127.0.0.1. */
    static final class HeDieuHanhThat implements HeDieuHanh {

        private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");

        @Override
        public List<Long> timPid(int cong) {
            if (WINDOWS) {
                return phanTichNetstat(chay("netstat", "-ano"), cong);
            }
            List<Long> pids = phanTichLsof(chay("lsof", "-nP", "-iTCP:" + cong, "-sTCP:LISTEN", "-t"));
            return pids.isEmpty() ? phanTichSs(chay("ss", "-ltnpH"), cong) : pids;
        }

        @Override
        public Optional<String> chuongTrinh(long pid) {
            return ProcessHandle.of(pid).flatMap(h -> h.info().command());
        }

        @Override
        public boolean laFootStyle(int cong) {
            try {
                HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).connectTimeout(Duration.ofSeconds(2)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + cong + API_NHAN_DIEN))
                        .timeout(Duration.ofSeconds(3)).GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                String body = res.body() == null ? "" : res.body();
                return res.statusCode() == 200 && body.contains("\"daCauHinh\"") && body.contains("\"dangNhap\"");
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public Optional<Instant> khoiDongLuc(long pid) {
            try {
                return ProcessHandle.of(pid).flatMap(h -> h.info().startInstant());
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }

        @Override
        public boolean tat(long pid) {
            boolean ok = ProcessHandle.of(pid).map(h -> {
                try {
                    return h.destroyForcibly();
                } catch (RuntimeException e) {
                    return false;
                }
            }).orElse(false);
            if (!ok && WINDOWS) {
                chay("taskkill", "/PID", String.valueOf(pid), "/F");
                ok = ProcessHandle.of(pid).map(h -> !h.isAlive()).orElse(true);
            }
            return ok;
        }

        /** Chạy lệnh hệ thống, trả về toàn bộ chữ in ra ("" nếu lỗi / không có lệnh). */
        private static String chay(String... lenh) {
            try {
                Process p = new ProcessBuilder(lenh).redirectErrorStream(true).start();
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                // Đọc ở luồng riêng để lệnh bị treo cũng không làm backend đứng mãi (tối đa 10 giây)
                Thread doc = new Thread(() -> {
                    try (InputStream in = p.getInputStream()) {
                        in.transferTo(buf);
                    } catch (IOException ignored) {
                        // lệnh bị tắt giữa chừng
                    }
                }, "doc-lenh-he-thong");
                doc.setDaemon(true);
                doc.start();
                if (!p.waitFor(10, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
                doc.join(2000);
                return buf.toString(Charset.defaultCharset());
            } catch (IOException e) {
                return "";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }
    }
}
