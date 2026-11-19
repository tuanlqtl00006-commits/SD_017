package com.footstyle.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phí giao hàng theo QUÃNG ĐƯỜNG từ cửa hàng tới nơi nhận, tính bằng API bản đồ miễn phí (góp ý 18/11 [02:14]: "ship chưa
 * thấy API"; không dùng tài khoản đơn vị vận chuyển):
 * <ul>
 *   <li>Tìm tọa độ địa chỉ bằng Photon / Nominatim, đo quãng đường đi ô tô bằng OSRM (đều dùng dữ liệu OpenStreetMap). Không cần mã khóa.</li>
 *   <li>Phí = phí giao hàng quản lý cài ({@link CaiDatVanChuyenService#phiGiaoHang()}) cho {@code km-co-ban} km đầu,
 *       cộng {@code phi-moi-buoc} cho mỗi {@code buoc-km} km tiếp theo (làm tròn lên), làm tròn tới 1.000 ₫.</li>
 *   <li>Mất mạng / bản đồ không tìm thấy địa chỉ: tạm tính theo bảng phí tỉnh ({@link PhiVanChuyenTinh}), nguồn "BANG_TINH".</li>
 * </ul>
 * Kết quả tọa độ và quãng đường được nhớ trong bộ nhớ (địa chỉ giống nhau không gọi lại), nên màn quầy / giỏ hàng và lúc
 * chốt đơn ra cùng một số. Đạt ngưỡng free ship thì nơi gọi đưa phí về 0 qua {@link CaiDatVanChuyenService}.
 */
@Service
public class PhiVanChuyenService {

    private static final Logger log = LoggerFactory.getLogger(PhiVanChuyenService.class);

    /** Phí đã tính: soKm null khi tạm tính theo bảng tỉnh. nguon: BAN_DO | BANG_TINH. */
    public record KetQua(long phi, Double soKm, String nguon) {
        public Map<String, Object> moTa() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("phiVanChuyen", phi);
            m.put("soKm", soKm == null ? null : Math.round(soKm * 10) / 10.0);
            m.put("nguon", nguon);
            return m;
        }
    }

    public static final String NGUON_BAN_DO = "BAN_DO";
    public static final String NGUON_BANG_TINH = "BANG_TINH";

    /** Bản đồ: tìm tọa độ và đo quãng đường (tách riêng để test thay bằng bản đồ giả, không gọi mạng). */
    public interface BanDo {
        /** {lat, lon} của địa chỉ, không thấy thì empty. */
        Optional<double[]> toaDo(String diaChi);

        /** Quãng đường đi (km) giữa hai tọa độ, không đo được thì empty. */
        Optional<Double> soKm(double[] tu, double[] den);
    }

    private final CaiDatVanChuyenService caiDat;
    private final BanDo banDo;
    private final String diaChiCuaHang;
    private final double kmCoBan;
    private final double buocKm;
    private final long phiMoiBuoc;
    private final boolean bat;
    private final Map<String, Optional<double[]>> nhoToaDo = new ConcurrentHashMap<>();
    private final Map<String, Optional<Double>> nhoSoKm = new ConcurrentHashMap<>();

    @Autowired
    public PhiVanChuyenService(CaiDatVanChuyenService caiDat,
                               @Value("${app.cua-hang.dia-chi:Hà Nội}") String diaChiCuaHang,
                               @Value("${app.van-chuyen.km-co-ban:10}") double kmCoBan,
                               @Value("${app.van-chuyen.buoc-km:50}") double buocKm,
                               @Value("${app.van-chuyen.phi-moi-buoc:1000}") long phiMoiBuoc,
                               @Value("${app.van-chuyen.ban-do-bat:true}") boolean bat) {
        this(caiDat, new BanDoOpenStreetMap(), diaChiCuaHang, kmCoBan, buocKm, phiMoiBuoc, bat);
    }

    public PhiVanChuyenService(CaiDatVanChuyenService caiDat, BanDo banDo, String diaChiCuaHang,
                               double kmCoBan, double buocKm, long phiMoiBuoc, boolean bat) {
        this.caiDat = caiDat;
        this.banDo = banDo;
        this.diaChiCuaHang = diaChiCuaHang == null || diaChiCuaHang.isBlank() ? "Hà Nội" : diaChiCuaHang.trim();
        this.kmCoBan = Math.max(0, kmCoBan);
        this.buocKm = buocKm > 0 ? buocKm : 50;
        this.phiMoiBuoc = Math.max(0, phiMoiBuoc);
        this.bat = bat;
    }

    public double kmCoBan() {
        return kmCoBan;
    }

    public double buocKm() {
        return buocKm;
    }

    public long phiMoiBuoc() {
        return phiMoiBuoc;
    }

    /** Thêm cách tính theo km vào dữ liệu cài đặt gửi giao diện (trang Chính sách, Hướng dẫn, Cài đặt ghi đúng cách tính). */
    public Map<String, Object> themCachTinh(Map<String, Object> m) {
        m.put("tinhTheoKm", bat);
        m.put("kmCoBan", kmCoBan);
        m.put("buocKm", buocKm);
        m.put("phiMoiBuoc", phiMoiBuoc);
        return m;
    }

    /** Phí theo số km đường đi: phí cài đặt cho km cơ bản đầu, mỗi bước km tiếp theo cộng thêm, làm tròn lên 1.000 ₫. */
    public long phiTheoKm(double km) {
        long phi = caiDat.phiGiaoHang();
        if (km > kmCoBan) phi += (long) Math.ceil((km - kmCoBan) / buocKm) * phiMoiBuoc;
        return (long) Math.ceil(phi / 1000.0) * 1000L;
    }

    /** Phí giao tới phường/xã của tỉnh/thành (diaChiCuThe: số nhà, đường - chỉ dùng khi bản đồ không tìm được theo phường). */
    public KetQua tinh(String tinhThanh, String phuongXa, String diaChiCuThe) {
        String tinh = gon(tinhThanh);
        String phuong = gon(phuongXa);
        if (bat && !tinh.isEmpty()) {
            try {
                Optional<double[]> den = timToaDo(ungVienNoiNhan(tinh, phuong, gon(diaChiCuThe)));
                Optional<double[]> tu = timToaDo(ungVienDiaChi(diaChiCuaHang));
                if (den.isPresent() && tu.isPresent()) {
                    Optional<Double> km = soKm(tu.get(), den.get());
                    if (km.isPresent()) return new KetQua(phiTheoKm(km.get()), km.get(), NGUON_BAN_DO);
                }
            } catch (RuntimeException e) {
                log.warn("Không tính được phí theo bản đồ cho {}, {}: {}", phuong, tinh, e.getMessage());
            }
        }
        return new KetQua(PhiVanChuyenTinh.phi(tinh, phuong), null, NGUON_BANG_TINH);
    }

    /**
     * Phí theo địa chỉ ghi liền một chuỗi ("Số 15 Trần Phú, Phường Hà Đông, Thành phố Hà Nội"): phần cuối là tỉnh/thành,
     * phần trước đó là phường/xã. Không nhận ra tỉnh thì null (nơi gọi giữ phí cũ / phí mặc định).
     */
    public KetQua tinhTheoDiaChi(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) return null;
        String[] phan = diaChi.split(",");
        String tinh = phan[phan.length - 1].trim();
        if (!PhiVanChuyenTinh.nhanRa(tinh)) return null;
        String phuong = phan.length >= 2 ? phan[phan.length - 2].trim() : "";
        StringBuilder cuThe = new StringBuilder();
        for (int i = 0; i < phan.length - 2; i++) cuThe.append(i > 0 ? ", " : "").append(phan[i].trim());
        return tinh(tinh, phuong, cuThe.toString());
    }

    private Optional<double[]> timToaDo(List<String> ungVien) {
        for (String q : ungVien) {
            Optional<double[]> td = nho(nhoToaDo, q.toLowerCase(Locale.ROOT), () -> banDo.toaDo(q));
            if (td.isPresent()) return td;
        }
        return Optional.empty();
    }

    private Optional<Double> soKm(double[] tu, double[] den) {
        String khoa = String.format(Locale.ROOT, "%.5f,%.5f>%.5f,%.5f", tu[0], tu[1], den[0], den[1]);
        return nho(nhoSoKm, khoa, () -> banDo.soKm(tu, den));
    }

    /** Kết quả tìm được thì nhớ luôn; không tìm được / lỗi mạng chỉ nhớ {@link #NHO_RONG_MS} để mạng có lại thì tính lại. */
    private <T> Optional<T> nho(Map<String, Optional<T>> bang, String khoa, java.util.function.Supplier<Optional<T>> tim) {
        Optional<T> co = bang.get(khoa);
        if (co != null && (co.isPresent() || System.currentTimeMillis() < hetHanRong.getOrDefault(khoa, 0L))) return co;
        Optional<T> kq = tim.get();
        bang.put(khoa, kq);
        if (kq.isEmpty()) hetHanRong.put(khoa, System.currentTimeMillis() + NHO_RONG_MS);
        return kq;
    }

    private static final long NHO_RONG_MS = 10 * 60_000L;
    private final Map<String, Long> hetHanRong = new ConcurrentHashMap<>();

    /** Thứ tự thử tìm nơi nhận: phường + tỉnh (đủ chính xác để tính phí), rồi địa chỉ cụ thể, cuối cùng chỉ tỉnh. */
    static List<String> ungVienNoiNhan(String tinh, String phuong, String cuThe) {
        List<String> ds = new ArrayList<>();
        if (!phuong.isEmpty()) ds.add(phuong + ", " + tinh);
        if (!cuThe.isEmpty()) ds.add(boSoNha(cuThe) + (phuong.isEmpty() ? "" : ", " + phuong) + ", " + tinh);
        ds.add(tinh);
        return ds;
    }

    /** "123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội": thử cả chuỗi, bỏ số nhà, ghép đường + tỉnh, rồi bớt dần phần đầu. */
    static List<String> ungVienDiaChi(String diaChi) {
        String[] phan = diaChi.split(",");
        List<String> ds = new ArrayList<>();
        ds.add(diaChi.trim());
        String dau = boSoNha(phan[0].trim());
        String cuoi = phan[phan.length - 1].trim();
        if (phan.length > 1) {
            List<String> conLai = new ArrayList<>();
            for (int i = 1; i < phan.length; i++) conLai.add(phan[i].trim());
            ds.add(dau + ", " + String.join(", ", conLai));
            ds.add(dau + ", " + cuoi);
            for (int i = 1; i < phan.length; i++) {
                List<String> duoi = new ArrayList<>();
                for (int j = i; j < phan.length; j++) duoi.add(phan[j].trim());
                ds.add(String.join(", ", duoi));
            }
        }
        return ds.stream().filter(s -> !s.isBlank()).distinct().toList();
    }

    private static String boSoNha(String s) {
        return s.replaceFirst("^(số|so|sn)?\\s*[\\d/\\-]+[a-zA-Z]?\\s*,?\\s*", "").trim();
    }

    private static String gon(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    /**
     * Bản đồ OpenStreetMap miễn phí: Photon / Nominatim (tọa độ) + OSRM (quãng đường). Các dịch vụ yêu cầu tên ứng dụng ở User-Agent
     * và không gọi dồn dập, nên các lần tìm tọa độ cách nhau ít nhất 1 giây. Lỗi mạng trả empty để nơi gọi tạm tính.
     */
    static final class BanDoOpenStreetMap implements BanDo {
        private static final String UA = "FootStyle-DoAnBanVot/1.0";
        private static final List<String> MAY_DUONG_DI = List.of(
                "https://router.project-osrm.org/route/v1/driving/",
                "https://routing.openstreetmap.de/routed-car/route/v1/driving/");
        private final ObjectMapper json = new ObjectMapper();
        private long lanCuoi = 0;

        /**
         * Tìm tọa độ: Photon (komoot, dữ liệu OpenStreetMap, giới hạn trong khung Việt Nam) trước, không được thì Nominatim.
         * Hai nơi đều miễn phí, không cần mã khóa; dùng hai nơi để một nơi tạm chặn / lỗi thì vẫn tính được.
         */
        @Override
        public Optional<double[]> toaDo(String diaChi) {
            String q = URLEncoder.encode(diaChi, StandardCharsets.UTF_8);
            try {
                cho1Giay();
                JsonNode n = goi("https://photon.komoot.io/api/?limit=1&bbox=102.1,8.2,109.5,23.4&q=" + q);
                JsonNode f = n == null ? null : n.path("features").path(0);
                if (f != null && f.path("geometry").path("coordinates").size() == 2) {
                    JsonNode c = f.path("geometry").path("coordinates");
                    return Optional.of(new double[] {c.get(1).asDouble(), c.get(0).asDouble()});
                }
            } catch (Exception e) {
                log.warn("Photon lỗi với \"{}\": {}", diaChi, e.toString());
            }
            try {
                cho1Giay();
                JsonNode n = goi("https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=vn&accept-language=vi&q=" + q);
                if (n == null || !n.isArray() || n.isEmpty()) return Optional.empty();
                return Optional.of(new double[] {n.get(0).path("lat").asDouble(), n.get(0).path("lon").asDouble()});
            } catch (Exception e) {
                log.warn("Nominatim lỗi với \"{}\": {}", diaChi, e.toString());
                return Optional.empty();
            }
        }

        @Override
        public Optional<Double> soKm(double[] tu, double[] den) {
            String toaDo = String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f?overview=false", tu[1], tu[0], den[1], den[0]);
            for (String may : MAY_DUONG_DI) {
                try {
                    JsonNode n = goi(may + toaDo);
                    if (n != null && "Ok".equals(n.path("code").asText()) && n.path("routes").size() > 0) {
                        return Optional.of(n.path("routes").get(0).path("distance").asDouble() / 1000.0);
                    }
                } catch (Exception e) {
                    log.warn("OSRM {} lỗi: {}", may, e.toString());
                }
            }
            return Optional.empty();
        }

        private synchronized void cho1Giay() throws InterruptedException {
            long cho = lanCuoi + 1100 - System.currentTimeMillis();
            if (cho > 0) Thread.sleep(cho);
            lanCuoi = System.currentTimeMillis();
        }

        /**
         * Dùng HttpURLConnection (không dùng java.net.http.HttpClient): HttpClient lỗi "ClosedChannelException" khi lần nối
         * đầu qua IPv6 hỏng rồi thử lại, còn HttpURLConnection tự thử lần lượt từng địa chỉ IP của máy chủ bản đồ.
         */
        private JsonNode goi(String url) throws Exception {
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
            try {
                c.setConnectTimeout(4000);
                c.setReadTimeout(6000);
                c.setRequestProperty("User-Agent", UA);
                c.setRequestProperty("Accept", "application/json");
                if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
                try (java.io.InputStream in = c.getInputStream()) {
                    return json.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            } finally {
                c.disconnect();
            }
        }
    }
}
