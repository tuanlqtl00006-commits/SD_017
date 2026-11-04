package com.footstyle.demo.service;

import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * Thống kê so sánh doanh thu theo tháng / quý / năm, so với kỳ trước và cùng kỳ năm trước, biểu đồ tăng trưởng,
 * xuất Excel và gửi email báo cáo kèm biểu đồ.
 *  - Doanh thu thực tế: đơn "Đã hoàn thành". Doanh thu dự kiến: mọi đơn chưa hủy / chưa hoàn tiền (gồm đơn đang xử lý, đang giao).
 *    Cả hai đều không tính phí ship (DoanhThu.cua). Hóa đơn chờ tại quầy (chưa thanh toán) không tính.
 *  - kỳ: tháng "2026-10", quý "2026-Q4", năm "2026"; bỏ trống = kỳ hiện tại.
 */
@Service
public class ThongKeSoSanhService {

    private static final Logger log = LoggerFactory.getLogger(ThongKeSoSanhService.class);
    private static final Pattern KY_NGAY = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern KY_THANG = Pattern.compile("(\\d{4})-(\\d{1,2})");
    private static final Pattern KY_QUY = Pattern.compile("(\\d{4})-[Qq]([1-4])");
    private static final Pattern KY_NAM = Pattern.compile("(\\d{4})");
    private static final Pattern KY_TUY_CHON = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})[_~](\\d{4}-\\d{2}-\\d{2})");
    static final int SO_NAM_XU_HUONG = 5;

    public enum Kieu { NGAY, THANG, QUY, NAM, TUY_CHON }

    /** Một kỳ thống kê: nhãn hiển thị + khoảng ngày (tính cả 2 đầu). */
    public record Ky(String ma, String nhan, LocalDate tu, LocalDate den) {
    }

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final NhanVienRepository nhanVienRepo;
    private final MailService mailService;
    private Clock dongHo = Clock.systemDefaultZone();

    private volatile String emailTuDong = "";
    private volatile boolean tuDongHangNgay = true;
    private volatile boolean tuDongThang = true;
    private volatile boolean tuDongNam = true;

    public ThongKeSoSanhService(HoaDonRepository hoaDonRepo, HoaDonChiTietRepository chiTietRepo, NhanVienRepository nhanVienRepo,
                                MailService mailService) {
        this.hoaDonRepo = hoaDonRepo;
        this.chiTietRepo = chiTietRepo;
        this.nhanVienRepo = nhanVienRepo;
        this.mailService = mailService;
    }

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    /* ===================== Kỳ ===================== */

    static Kieu docKieu(String s) {
        if (s == null || s.isBlank()) return Kieu.THANG;
        try {
            return Kieu.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Kiểu so sánh phải là NGAY, THANG, QUY hoặc NAM.");
        }
    }

    Ky docKy(Kieu kieu, String ky) {
        LocalDate homNay = LocalDate.now(dongHo);
        String s = ky == null ? "" : ky.trim();
        switch (kieu) {
            case NGAY -> {
                LocalDate ng = homNay;
                if (!s.isEmpty()) {
                    Matcher m = KY_NGAY.matcher(s);
                    if (!m.matches()) {
                        throw ApiException.badRequest("Ngày không hợp lệ (dạng 2026-10-24).");
                    }
                    try {
                        int y = Integer.parseInt(m.group(1));
                        int mo = Integer.parseInt(m.group(2));
                        int da = Integer.parseInt(m.group(3));
                        if (y < 2000 || y > 2100) throw ApiException.badRequest("Ngày không hợp lệ (dạng 2026-10-24).");
                        ng = LocalDate.of(y, mo, da);
                    } catch (Exception ex) {
                        throw ApiException.badRequest("Ngày không hợp lệ (dạng 2026-10-24).");
                    }
                }
                return ngay(ng);
            }
            case THANG -> {
                YearMonth ym = YearMonth.from(homNay);
                if (!s.isEmpty()) {
                    Matcher m = KY_THANG.matcher(s);
                    if (!m.matches()) {
                        throw ApiException.badRequest("Tháng không hợp lệ (dạng 2026-10).");
                    }
                    int y = Integer.parseInt(m.group(1));
                    int mo = Integer.parseInt(m.group(2));
                    if (y < 2000 || y > 2100 || mo < 1 || mo > 12) {
                        throw ApiException.badRequest("Tháng không hợp lệ (dạng 2026-10).");
                    }
                    ym = YearMonth.of(y, mo);
                }
                return thang(ym);
            }
            case QUY -> {
                int nam = homNay.getYear();
                int quy = (homNay.getMonthValue() - 1) / 3 + 1;
                if (!s.isEmpty()) {
                    Matcher m = KY_QUY.matcher(s);
                    if (!m.matches()) throw ApiException.badRequest("Quý không hợp lệ (dạng 2026-Q4).");
                    nam = Integer.parseInt(m.group(1));
                    quy = Integer.parseInt(m.group(2));
                    if (nam < 2000 || nam > 2100) throw ApiException.badRequest("Quý không hợp lệ (dạng 2026-Q4).");
                }
                return quy(nam, quy);
            }
            case TUY_CHON -> {
                LocalDate tu = homNay.withDayOfMonth(1);
                LocalDate den = homNay;
                if (!s.isEmpty()) {
                    Matcher m = KY_TUY_CHON.matcher(s);
                    if (!m.matches()) throw ApiException.badRequest("Khoảng ngày không hợp lệ (dạng 2026-10-01_2026-10-31).");
                    try {
                        tu = LocalDate.parse(m.group(1));
                        den = LocalDate.parse(m.group(2));
                    } catch (Exception ex) {
                        throw ApiException.badRequest("Khoảng ngày không hợp lệ.");
                    }
                    if (tu.isAfter(den)) {
                        LocalDate tmp = tu;
                        tu = den;
                        den = tmp;
                    }
                }
                return tuyChon(tu, den);
            }
            default -> {
                int nam = homNay.getYear();
                if (!s.isEmpty()) {
                    if (!KY_NAM.matcher(s).matches()) throw ApiException.badRequest("Năm không hợp lệ (dạng 2026).");
                    nam = Integer.parseInt(s);
                    if (nam < 2000 || nam > 2100) throw ApiException.badRequest("Năm không hợp lệ (dạng 2026).");
                }
                return nam(nam);
            }
        }
    }

    static Ky ngay(LocalDate d) {
        return new Ky(
                d.toString(),
                String.format("Ngày %02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear()),
                d,
                d
        );
    }

    static Ky thang(YearMonth ym) {
        return new Ky(ym.toString(), "Tháng " + ym.getMonthValue() + "/" + ym.getYear(), ym.atDay(1), ym.atEndOfMonth());
    }

    static Ky quy(int nam, int quy) {
        LocalDate tu = LocalDate.of(nam, (quy - 1) * 3 + 1, 1);
        return new Ky(nam + "-Q" + quy, "Quý " + quy + "/" + nam, tu, tu.plusMonths(3).minusDays(1));
    }

    static Ky nam(int nam) {
        return new Ky(String.valueOf(nam), "Năm " + nam, LocalDate.of(nam, 1, 1), LocalDate.of(nam, 12, 31));
    }

    static Ky tuyChon(LocalDate tu, LocalDate den) {
        String nhan = String.format("%02d/%02d/%d - %02d/%02d/%d",
                tu.getDayOfMonth(), tu.getMonthValue(), tu.getYear(),
                den.getDayOfMonth(), den.getMonthValue(), den.getYear());
        return new Ky(tu + "_" + den, nhan, tu, den);
    }

    static Ky kyTruoc(Kieu kieu, Ky k) {
        return switch (kieu) {
            case NGAY -> ngay(k.tu().minusDays(1));
            case THANG -> thang(YearMonth.from(k.tu()).minusMonths(1));
            case QUY -> {
                LocalDate t = k.tu().minusMonths(3);
                yield quy(t.getYear(), (t.getMonthValue() - 1) / 3 + 1);
            }
            case NAM -> nam(k.tu().getYear() - 1);
            case TUY_CHON -> {
                long soNgay = java.time.temporal.ChronoUnit.DAYS.between(k.tu(), k.den()) + 1;
                LocalDate denTr = k.tu().minusDays(1);
                yield tuyChon(denTr.minusDays(Math.max(0, soNgay - 1)), denTr);
            }
        };
    }

    static Ky cungKyNamTruoc(Kieu kieu, Ky k) {
        return switch (kieu) {
            case NGAY -> ngay(k.tu().minusYears(1));
            case THANG -> thang(YearMonth.from(k.tu()).minusYears(1));
            case QUY -> quy(k.tu().getYear() - 1, (k.tu().getMonthValue() - 1) / 3 + 1);
            case NAM -> nam(k.tu().getYear() - 1);
            case TUY_CHON -> tuyChon(k.tu().minusYears(1), k.den().minusYears(1));
        };
    }

    /* ===================== Số liệu ===================== */

    /** Hóa đơn được tính vào thống kê (bỏ hóa đơn chờ tại quầy chưa thanh toán). */
    static boolean tinh(HoaDon h) {
        return h.getNgayTao() != null && !HoaDonChoService.laHoaDonCho(h);
    }

    static boolean duKien(HoaDon h) {
        Integer tt = h.getTrangThai();
        return tt != null && tt != HoaDon.DA_HUY && tt != HoaDon.HOAN_TIEN;
    }

    static boolean trongKy(HoaDon h, Ky k) {
        LocalDate d = h.getNgayTao().toLocalDate();
        return !d.isBefore(k.tu()) && !d.isAfter(k.den());
    }

    /** Chỉ số của một kỳ: doanh thu thực tế / dự kiến, tổng giảm giá, số đơn, hoàn thành, hủy, giá trị TB, sản phẩm bán, số khách. */
    static Map<String, Object> chiSo(Ky k, List<HoaDon> hoaDons, List<HoaDonChiTiet> chiTietHoanThanh) {
        long thucTe = 0;
        long duKien = 0;
        long tongGiamGia = 0;
        int soDon = 0;
        int hoanThanh = 0;
        int huy = 0;
        Set<Integer> khach = new HashSet<>();
        for (HoaDon h : hoaDons) {
            if (!tinh(h) || !trongKy(h, k)) continue;
            soDon++;
            long dt = DoanhThu.cua(h).longValue();
            if (duKien(h)) duKien += dt;
            if (Integer.valueOf(HoaDon.HOAN_THANH).equals(h.getTrangThai())) {
                hoanThanh++;
                thucTe += dt;
                if (h.getSoTienGiam() != null && h.getSoTienGiam().signum() > 0) {
                    tongGiamGia += h.getSoTienGiam().longValue();
                }
                if (h.getKhachHang() != null) khach.add(h.getKhachHang().getId());
            }
            if (Integer.valueOf(HoaDon.DA_HUY).equals(h.getTrangThai())) huy++;
        }
        long sanPham = 0;
        Map<Integer, Map<String, Object>> spTrongKyMap = new LinkedHashMap<>();
        for (HoaDonChiTiet ct : chiTietHoanThanh) {
            if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null || !trongKy(ct.getHoaDon(), k)) continue;
            long sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            sanPham += sl;
            if (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getSanPham() != null) {
                SanPham sp = ct.getSanPhamChiTiet().getSanPham();
                String tenTh = sp.getThuongHieu() != null && sp.getThuongHieu().getTen() != null
                        ? sp.getThuongHieu().getTen().trim()
                        : "";
                Map<String, Object> spItem = spTrongKyMap.computeIfAbsent(sp.getId(), id -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("idSanPham", sp.getId());
                    x.put("maSanPham", sp.getMaSanPham());
                    x.put("tenSanPham", sp.getTenSanPham());
                    x.put("thuongHieu", tenTh);
                    x.put("soLuong", 0L);
                    x.put("doanhThu", 0L);
                    return x;
                });
                spItem.put("soLuong", (Long) spItem.get("soLuong") + sl);
                spItem.put("doanhThu", (Long) spItem.get("doanhThu") + (ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue()));
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ky", kyDto(k));
        m.put("doanhThuThucTe", thucTe);
        m.put("doanhThuDuKien", duKien);
        m.put("tongGiamGia", tongGiamGia);
        m.put("soDon", soDon);
        m.put("soHoanThanh", hoanThanh);
        m.put("soHuy", huy);
        m.put("giaTriTrungBinh", hoanThanh == 0 ? 0 : Math.round((double) thucTe / hoanThanh));
        m.put("soSanPhamBan", sanPham);
        m.put("soKhachMua", khach.size());
        m.put("sanPhamTrongKy", new ArrayList<>(spTrongKyMap.values()));
        return m;
    }

    static Map<String, Object> kyDto(Ky k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ma", k.ma());
        m.put("nhan", k.nhan());
        m.put("tuNgay", k.tu());
        m.put("denNgay", k.den());
        return m;
    }

    /** % tăng trưởng so với b; b = 0 thì null (không tính được). */
    static Double tangTruong(long a, long b) {
        if (b == 0) return a == 0 ? 0.0 : null;
        return Math.round((a - b) * 1000.0 / b) / 10.0;
    }

    static final List<String> CHI_SO_SO_SANH = List.of("doanhThuThucTe", "doanhThuDuKien", "tongGiamGia", "soDon", "soHoanThanh", "soHuy",
            "giaTriTrungBinh", "soSanPhamBan", "soKhachMua");
    static final Map<String, String> TEN_CHI_SO = Map.of(
            "doanhThuThucTe", "Doanh thu thực tế", "doanhThuDuKien", "Doanh thu dự kiến", "tongGiamGia", "Tổng tiền giảm giá",
            "soDon", "Tổng đơn", "soHoanThanh", "Đơn hoàn thành",
            "soHuy", "Đơn hủy", "giaTriTrungBinh", "Giá trị TB / đơn", "soSanPhamBan", "Sản phẩm bán ra", "soKhachMua", "Khách hàng mua");

    static Map<String, Object> bangTang(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : CHI_SO_SO_SANH) m.put(k, tangTruong(((Number) a.get(k)).longValue(), ((Number) b.get(k)).longValue()));
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> soSanh(String kieuChuoi, String kyChuoi) {
        Kieu kieu = docKieu(kieuChuoi);
        Ky ky = docKy(kieu, kyChuoi);
        Ky truoc = kyTruoc(kieu, ky);
        Ky cungKy = cungKyNamTruoc(kieu, ky);
        int namDau = kieu == Kieu.NAM ? ky.tu().getYear() - (SO_NAM_XU_HUONG - 1) : ky.tu().getYear() - 1;
        LocalDate tu = LocalDate.of(namDau, 1, 1).isBefore(truoc.tu()) ? LocalDate.of(namDau, 1, 1) : truoc.tu();
        LocalDate den = LocalDate.of(ky.tu().getYear(), 12, 31);
        List<HoaDon> hoaDons = hoaDonRepo.findByNgayTaoBetween(tu.atStartOfDay(), den.atTime(LocalTime.MAX));
        List<HoaDonChiTiet> chiTiets = chiTietRepo.findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(HoaDon.HOAN_THANH, tu.atStartOfDay(), den.atTime(LocalTime.MAX));

        Map<String, Object> hienTai = chiSo(ky, hoaDons, chiTiets);
        Map<String, Object> kt = chiSo(truoc, hoaDons, chiTiets);
        Map<String, Object> ck = chiSo(cungKy, hoaDons, chiTiets);

        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("kieu", kieu.name());
        kq.put("ky", kyDto(ky));
        kq.put("hienTai", hienTai);
        kq.put("kyTruoc", kt);
        kq.put("cungKyNamTruoc", kieu == Kieu.NAM ? null : ck);
        kq.put("tangSoVoiKyTruoc", bangTang(hienTai, kt));
        kq.put("tangSoVoiCungKy", kieu == Kieu.NAM ? null : bangTang(hienTai, ck));
        kq.put("xuHuong", xuHuong(kieu, ky, hoaDons));
        kq.put("chiTietKy", chiTietKy(kieu, ky, hoaDons));
        kq.put("sanPham", sanPham(ky, truoc, chiTiets));
        Set<String> dsThuongHieu = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (HoaDonChiTiet ct : chiTiets) {
            if (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getSanPham() != null
                    && ct.getSanPhamChiTiet().getSanPham().getThuongHieu() != null
                    && ct.getSanPhamChiTiet().getSanPham().getThuongHieu().getTen() != null) {
                String tenTh = ct.getSanPhamChiTiet().getSanPham().getThuongHieu().getTen().trim();
                if (!tenTh.isEmpty()) dsThuongHieu.add(tenTh);
            }
        }
        kq.put("dsThuongHieu", new ArrayList<>(dsThuongHieu));
        kq.put("phanTich", taoPhanTich(kieu, kq));
        kq.put("nguoiNhanBaoCao", dsNguoiNhanDangLamViec());
        kq.put("emailNgungHoatDong", dsEmailNgungHoatDong());
        String emailDangNhap = layEmailNguoiDangNhap();
        if (emailDangNhap != null) {
            kq.put("emailNguoiDangNhap", emailDangNhap);
        }
        return kq;
    }

    private String layEmailNguoiDangNhap() {
        return XacThuc.hienTai()
                .map(NguoiDung::email)
                .filter(em -> em != null && !em.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    /** Danh sách Quản lý & Nhân viên đang làm việc tại cửa hàng có email hợp lệ để nhận báo cáo. */
    private List<Map<String, Object>> dsNguoiNhanDangLamViec() {
        List<Map<String, Object>> ds = new ArrayList<>();
        for (NhanVien nv : nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.HOAT_DONG)) {
            if (nv.getEmail() == null || !nv.getEmail().contains("@")) continue;
            String tenVt = nv.getVaiTro() != null && nv.getVaiTro().getTenVaiTro() != null
                    ? nv.getVaiTro().getTenVaiTro().trim()
                    : "Nhân viên";
            boolean laQl = VaiTro.TEN_QUAN_LY.equalsIgnoreCase(tenVt);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", nv.getId());
            m.put("maNhanVien", nv.getMaNhanVien());
            m.put("hoTen", nv.getHoTen());
            m.put("email", nv.getEmail().trim());
            m.put("vaiTro", tenVt);
            m.put("laQuanLy", laQl);
            ds.add(m);
        }
        ds.sort((a, b) -> Boolean.compare((Boolean) b.get("laQuanLy"), (Boolean) a.get("laQuanLy")));
        return ds;
    }

    private List<String> dsEmailNgungHoatDong() {
        try {
            List<NhanVien> dsNgung = nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.NGUNG_HOAT_DONG);
            if (dsNgung == null) return List.of();
            List<String> kq = new ArrayList<>();
            for (NhanVien nv : dsNgung) {
                if (nv.getEmail() != null && nv.getEmail().contains("@")) {
                    kq.add(nv.getEmail().trim().toLowerCase(Locale.ROOT));
                }
            }
            return kq;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Tạo nội dung nhận xét, phân tích tự động (tổng quan, giảm giá, so với kỳ trước, cùng kỳ năm trước và các kỳ khác trong năm). */
    @SuppressWarnings("unchecked")
    static List<String> taoPhanTich(Kieu kieu, Map<String, Object> d) {
        List<String> ds = new ArrayList<>();
        Map<String, Object> ky = (Map<String, Object>) d.get("ky");
        Map<String, Object> a = (Map<String, Object>) d.get("hienTai");
        Map<String, Object> kt = (Map<String, Object>) d.get("kyTruoc");
        Map<String, Object> ck = (Map<String, Object>) d.get("cungKyNamTruoc");
        String nhanKy = String.valueOf(ky.get("nhan"));
        String nhanKt = String.valueOf(((Map<String, Object>) kt.get("ky")).get("nhan"));

        long dtNay = ((Number) a.get("doanhThuThucTe")).longValue();
        long dtDuKien = ((Number) a.get("doanhThuDuKien")).longValue();
        long giamGia = ((Number) a.get("tongGiamGia")).longValue();
        int soDon = ((Number) a.get("soDon")).intValue();
        int hoanThanh = ((Number) a.get("soHoanThanh")).intValue();
        int huy = ((Number) a.get("soHuy")).intValue();
        long spBan = ((Number) a.get("soSanPhamBan")).longValue();

        ds.add(String.format(Locale.ROOT, "Kết quả %s: Ghi nhận %d đơn hàng (hoàn thành %d đơn, hủy %d đơn); doanh thu thực tế đạt %s, doanh thu dự kiến đạt %s, tổng chiết khấu/giảm giá khuyến mãi %s, bán ra %d sản phẩm.",
                nhanKy, soDon, hoanThanh, huy, so(dtNay, true), so(dtDuKien, true), so(giamGia, true), spBan));

        long dtTruoc = ((Number) kt.get("doanhThuThucTe")).longValue();
        Double tangKt = tangTruong(dtNay, dtTruoc);
        long chenhKt = dtNay - dtTruoc;
        String moTaKt = chenhKt == 0 ? "giữ nguyên (0 đ)" : String.format(Locale.ROOT, "%s (%s%s)", chenhKt > 0 ? "tăng" : "giảm", chenhKt > 0 ? "+" : "", so(chenhKt, true));
        ds.add(String.format(Locale.ROOT, "So với kỳ liền trước (%s đạt %s): Doanh thu thực tế %s, biến động %s.",
                nhanKt, so(dtTruoc, true), moTaKt, phanTram(tangKt)));

        if (ck != null) {
            String nhanCk = String.valueOf(((Map<String, Object>) ck.get("ky")).get("nhan"));
            long dtCk = ((Number) ck.get("doanhThuThucTe")).longValue();
            Double tangCk = tangTruong(dtNay, dtCk);
            long chenhCk = dtNay - dtCk;
            String moTaCk = chenhCk == 0 ? "giữ nguyên (0 đ)" : String.format(Locale.ROOT, "%s (%s%s)", chenhCk > 0 ? "tăng" : "giảm", chenhCk > 0 ? "+" : "", so(chenhCk, true));
            ds.add(String.format(Locale.ROOT, "So với cùng kỳ năm trước (%s đạt %s): Doanh thu thực tế %s, biến động %s.",
                    nhanCk, so(dtCk, true), moTaCk, phanTram(tangCk)));
        }

        List<Map<String, Object>> xu = (List<Map<String, Object>>) d.get("xuHuong");
        if (xu != null && !xu.isEmpty()) {
            Map<String, Object> maxKy = null;
            long tongNam = 0;
            int soKyCoDt = 0;
            for (Map<String, Object> p : xu) {
                long v = ((Number) p.get("doanhThu")).longValue();
                tongNam += v;
                if (v > 0) soKyCoDt++;
                if (maxKy == null || v > ((Number) maxKy.get("doanhThu")).longValue()) maxKy = p;
            }
            long tb = soKyCoDt > 0 ? Math.round((double) tongNam / soKyCoDt) : 0;
            String donVi = kieu == Kieu.THANG ? "các tháng trong năm" : kieu == Kieu.QUY ? "các quý trong năm" : "các năm";
            if (maxKy != null && ((Number) maxKy.get("doanhThu")).longValue() > 0) {
                ds.add(String.format(Locale.ROOT, "So sánh với %s: Tổng doanh thu chuỗi đạt %s (trung bình %s/kỳ có phát sinh); kỳ cao nhất là %s (%s).",
                        donVi, so(tongNam, true), so(tb, true), maxKy.get("nhan"), so(((Number) maxKy.get("doanhThu")).longValue(), true)));
            }
        }

        List<Map<String, Object>> sps = (List<Map<String, Object>>) d.get("sanPham");
        if (sps != null && !sps.isEmpty()) {
            Map<String, Object> top1 = sps.get(0);
            ds.add(String.format(Locale.ROOT, "Sản phẩm chủ lực: %s (%s) dẫn đầu với %s sản phẩm bán ra, mang về %s (so với kỳ trước: %s).",
                    top1.get("tenSanPham"), top1.get("maSanPham"), top1.get("soLuong"),
                    so(((Number) top1.get("doanhThu")).longValue(), true), phanTram((Double) top1.get("tangTruong"))));
        }
        return ds;
    }

    /**
     * Xu hướng để xem tăng trưởng: THÁNG -> 12 tháng của năm (so với cùng tháng năm trước), QUÝ -> 4 quý (so với năm trước),
     * NĂM -> 5 năm gần nhất. Mỗi điểm: { nhan, doanhThu, doanhThuNamTruoc, soDon, tangTruong }.
     */
    static List<Map<String, Object>> xuHuong(Kieu kieu, Ky ky, List<HoaDon> hoaDons) {
        List<Ky[]> cap = new ArrayList<>();
        int nam = ky.tu().getYear();
        switch (kieu) {
            case NGAY -> {
                for (int i = 6; i >= 0; i--) {
                    LocalDate ng = ky.tu().minusDays(i);
                    cap.add(new Ky[] {ngay(ng), ngay(ng.minusYears(1))});
                }
            }
            case THANG -> {
                for (int t = 1; t <= 12; t++) cap.add(new Ky[] {thang(YearMonth.of(nam, t)), thang(YearMonth.of(nam - 1, t))});
            }
            case QUY -> {
                for (int q = 1; q <= 4; q++) cap.add(new Ky[] {quy(nam, q), quy(nam - 1, q)});
            }
            case NAM -> {
                for (int y = nam - (SO_NAM_XU_HUONG - 1); y <= nam; y++) cap.add(new Ky[] {nam(y), null});
            }
            case TUY_CHON -> {
                long soNgay = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(ky.tu(), ky.den()) + 1);
                for (int i = 3; i >= 0; i--) {
                    LocalDate denI = ky.den().minusDays(i * soNgay);
                    LocalDate tuI = denI.minusDays(soNgay - 1);
                    Ky kI = tuyChon(tuI, denI);
                    cap.add(new Ky[] {kI, tuyChon(tuI.minusYears(1), denI.minusYears(1))});
                }
            }
        }
        List<Map<String, Object>> ds = new ArrayList<>();
        Long truoc = null;
        for (Ky[] c : cap) {
            long a = doanhThu(c[0], hoaDons);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nhan", kieu == Kieu.NGAY ? c[0].tu().getDayOfMonth() + "/" + c[0].tu().getMonthValue()
                    : kieu == Kieu.THANG ? "T" + c[0].tu().getMonthValue() : kieu == Kieu.QUY ? "Q" + ((c[0].tu().getMonthValue() - 1) / 3 + 1)
                    : String.valueOf(c[0].tu().getYear()));
            m.put("ma", c[0].ma());
            m.put("doanhThu", a);
            m.put("soDon", soDonHoanThanh(c[0], hoaDons));
            if (c[1] != null) {
                long b = doanhThu(c[1], hoaDons);
                m.put("doanhThuNamTruoc", b);
                m.put("tangTruong", tangTruong(a, b));
            } else {
                m.put("doanhThuNamTruoc", null);
                m.put("tangTruong", truoc == null ? null : tangTruong(a, truoc)); // năm: so với năm liền trước
            }
            truoc = a;
            ds.add(m);
        }
        return ds;
    }

    /** Chi tiết trong kỳ: tháng -> từng ngày, quý / năm -> từng tháng; kèm doanh thu cùng kỳ năm trước. */
    static List<Map<String, Object>> chiTietKy(Kieu kieu, Ky ky, List<HoaDon> hoaDons) {
        List<Map<String, Object>> ds = new ArrayList<>();
        if (kieu == Kieu.NGAY) {
            for (int i = 6; i >= 0; i--) {
                LocalDate d = ky.tu().minusDays(i);
                Ky ng = new Ky(d.toString(), d.getDayOfMonth() + "/" + d.getMonthValue(), d, d);
                LocalDate dTruoc = d.minusYears(1);
                Ky ngTruoc = new Ky(dTruoc.toString(), "", dTruoc, dTruoc);
                ds.add(diem(ng, ngTruoc, hoaDons));
            }
        } else if (kieu == Kieu.THANG) {
            for (LocalDate d = ky.tu(); !d.isAfter(ky.den()); d = d.plusDays(1)) {
                Ky ngay = new Ky(d.toString(), d.getDayOfMonth() + "/" + d.getMonthValue(), d, d);
                LocalDate dTruoc = d.minusYears(1);
                Ky ngayTruoc = new Ky(dTruoc.toString(), "", dTruoc, dTruoc);
                ds.add(diem(ngay, ngayTruoc, hoaDons));
            }
        } else {
            for (LocalDate d = ky.tu(); !d.isAfter(ky.den()); d = d.plusMonths(1)) {
                YearMonth ym = YearMonth.from(d);
                ds.add(diem(thang(ym), thang(ym.minusYears(1)), hoaDons));
            }
        }
        return ds;
    }

    private static Map<String, Object> diem(Ky k, Ky namTruoc, List<HoaDon> hoaDons) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nhan", k.ma().length() == 7 ? "T" + k.tu().getMonthValue() : k.nhan());
        m.put("ma", k.ma());
        m.put("doanhThu", doanhThu(k, hoaDons));
        m.put("doanhThuDuKien", doanhThuDuKien(k, hoaDons));
        m.put("soDon", soDonHoanThanh(k, hoaDons));
        m.put("doanhThuNamTruoc", doanhThu(namTruoc, hoaDons));
        return m;
    }

    static long doanhThu(Ky k, List<HoaDon> hoaDons) {
        long t = 0;
        for (HoaDon h : hoaDons) {
            if (tinh(h) && trongKy(h, k) && Integer.valueOf(HoaDon.HOAN_THANH).equals(h.getTrangThai())) t += DoanhThu.cua(h).longValue();
        }
        return t;
    }

    static long doanhThuDuKien(Ky k, List<HoaDon> hoaDons) {
        long t = 0;
        for (HoaDon h : hoaDons) if (tinh(h) && trongKy(h, k) && duKien(h)) t += DoanhThu.cua(h).longValue();
        return t;
    }

    static int soDonHoanThanh(Ky k, List<HoaDon> hoaDons) {
        int n = 0;
        for (HoaDon h : hoaDons) if (tinh(h) && trongKy(h, k) && Integer.valueOf(HoaDon.HOAN_THANH).equals(h.getTrangThai())) n++;
        return n;
    }

    /** Top 10 sản phẩm của kỳ (số lượng, doanh thu) kèm số lượng kỳ trước để thấy tăng / giảm. */
    static List<Map<String, Object>> sanPham(Ky ky, Ky truoc, List<HoaDonChiTiet> chiTiets) {
        Map<Integer, Map<String, Object>> theoSp = new LinkedHashMap<>();
        for (HoaDonChiTiet ct : chiTiets) {
            if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null || ct.getSanPhamChiTiet() == null) continue;
            boolean nay = trongKy(ct.getHoaDon(), ky);
            boolean kt = trongKy(ct.getHoaDon(), truoc);
            if (!nay && !kt) continue;
            SanPham sp = ct.getSanPhamChiTiet().getSanPham();
            String tenTh = sp.getThuongHieu() != null && sp.getThuongHieu().getTen() != null
                    ? sp.getThuongHieu().getTen().trim()
                    : "";
            Map<String, Object> m = theoSp.computeIfAbsent(sp.getId(), k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("idSanPham", sp.getId());
                x.put("maSanPham", sp.getMaSanPham());
                x.put("tenSanPham", sp.getTenSanPham());
                x.put("thuongHieu", tenTh);
                x.put("soLuong", 0L);
                x.put("doanhThu", 0L);
                x.put("soLuongKyTruoc", 0L);
                return x;
            });
            long sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            if (nay) {
                m.put("soLuong", (Long) m.get("soLuong") + sl);
                m.put("doanhThu", (Long) m.get("doanhThu") + (ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue()));
            } else {
                m.put("soLuongKyTruoc", (Long) m.get("soLuongKyTruoc") + sl);
            }
        }
        return theoSp.values().stream()
                .filter(m -> (Long) m.get("soLuong") > 0)
                .sorted((x, y) -> Long.compare((Long) y.get("soLuong"), (Long) x.get("soLuong")))
                .limit(10)
                .peek(m -> m.put("tangTruong", tangTruong((Long) m.get("soLuong"), (Long) m.get("soLuongKyTruoc"))))
                .toList();
    }

    /* ===================== Email báo cáo ===================== */

    /** Gửi báo cáo so sánh tới email (quản lý bấm "Gửi email"). Trả về { daGui, giaLap, email }. */
    public Map<String, Object> guiEmail(String kieu, String ky, String email) {
        return guiEmail(kieu, ky, email, null, null, null, null, null, null, null, null);
    }

    /** Gửi báo cáo so sánh đầy đủ (hỗ trợ nhiều người nhận, tiêu đề tùy chọn, ghi chú người gửi và danh sách kỳ đối chiếu). */
    public Map<String, Object> guiEmail(String kieu, String ky, String email, String tieuDeTuyChon, String ghiChu, List<String> dsKy) {
        return guiEmail(kieu, ky, email, tieuDeTuyChon, ghiChu, dsKy, null, null, null, null, null);
    }

    /** Gửi báo cáo so sánh khớp hoàn toàn với bộ lọc đang chọn trên giao diện (chế độ kỳ, danh sách kỳ, thương hiệu, từ khóa sản phẩm). */
    public Map<String, Object> guiEmail(String kieu, String ky, String email, String tieuDeTuyChon, String ghiChu,
                                        List<String> dsKy, String thuongHieu, String tuKhoaSp) {
        return guiEmail(kieu, ky, email, tieuDeTuyChon, ghiChu, dsKy, thuongHieu, tuKhoaSp, null, null, null);
    }

    /** Gửi báo cáo so sánh khớp 100% với mọi bộ lọc và chế độ sắp xếp trên giao diện. */
    public Map<String, Object> guiEmail(String kieu, String ky, String email, String tieuDeTuyChon, String ghiChu,
                                        List<String> dsKy, String thuongHieu, String tuKhoaSp,
                                        String xuHuongSp, String sapXepSp, String sapXepKy) {
        String raw = email == null ? "" : email.trim();
        if (raw.isEmpty()) throw ApiException.badRequest("Email nhận báo cáo không hợp lệ.");
        String[] tach = raw.split("[,;\\s]+");
        List<String> dsEmail = new ArrayList<>();
        for (String item : tach) {
            String e = item.trim();
            if (e.isEmpty()) continue;
            if (e.contains("..") || !e.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
                throw ApiException.badRequest("Email nhận báo cáo không hợp lệ: " + e);
            }
            if (!dsEmail.contains(e)) dsEmail.add(e);
        }
        if (dsEmail.isEmpty()) throw ApiException.badRequest("Email nhận báo cáo không hợp lệ.");

        // Chặn gửi báo cáo nội bộ tới nhân viên đã ngừng làm việc tại cửa hàng
        List<String> dsNgung = dsEmailNgungHoatDong();
        if (!dsNgung.isEmpty()) {
            for (String e : dsEmail) {
                String lower = e.toLowerCase(Locale.ROOT);
                if (dsNgung.contains(lower)) {
                    throw ApiException.badRequest("Nhân viên có email " + e + " đã ngừng làm việc tại cửa hàng, không được gửi báo cáo nội bộ.");
                }
            }
        }

        Map<String, Object> d = soSanhTheoBoLoc(kieu, ky, dsKy, thuongHieu, tuKhoaSp, xuHuongSp, sapXepSp, sapXepKy);
        if (tieuDeTuyChon != null && !tieuDeTuyChon.isBlank()) {
            d.put("tieuDeTuyChon", tieuDeTuyChon.trim());
        }
        if (ghiChu != null && !ghiChu.isBlank()) {
            d.put("ghiChuNguoiGui", ghiChu.trim());
        }
        boolean that = false;
        try {
            for (String den : dsEmail) {
                that = guiThu(den, d) || that;
            }
        } catch (Exception e) {
            throw ApiException.conflict("Không gửi được email: " + e.getMessage());
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("email", String.join(", ", dsEmail));
        kq.put("soNguoiNhan", dsEmail.size());
        kq.put("daGui", that);
        kq.put("giaLap", !that);
        return kq;
    }

    private static String boDauTiengViet(String s) {
        if (s == null || s.isBlank()) return "";
        String norm = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).trim();
    }

    private static boolean khopBoLocSanPham(HoaDonChiTiet ct, String thChuan, String kwKhongDau) {
        if (ct == null || ct.getSanPhamChiTiet() == null || ct.getSanPhamChiTiet().getSanPham() == null) {
            return false;
        }
        SanPham sp = ct.getSanPhamChiTiet().getSanPham();
        String tenTh = sp.getThuongHieu() != null && sp.getThuongHieu().getTen() != null
                ? sp.getThuongHieu().getTen().trim()
                : "";
        if (!thChuan.isEmpty() && !tenTh.equalsIgnoreCase(thChuan)) {
            return false;
        }
        if (!kwKhongDau.isEmpty()) {
            String gop = (sp.getMaSanPham() == null ? "" : sp.getMaSanPham()) + " "
                    + (sp.getTenSanPham() == null ? "" : sp.getTenSanPham()) + " "
                    + tenTh;
            return boDauTiengViet(gop).contains(kwKhongDau);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static void apDungLocSanPhamChoChiSo(Map<String, Object> cs) {
        List<Map<String, Object>> spKhop = (List<Map<String, Object>>) cs.getOrDefault("sanPhamTrongKy", List.of());
        long dtLoc = 0;
        long slLoc = 0;
        for (Map<String, Object> sp : spKhop) {
            dtLoc += ((Number) sp.get("doanhThu")).longValue();
            slLoc += ((Number) sp.get("soLuong")).longValue();
        }
        int hoanThanhGoc = ((Number) cs.get("soHoanThanh")).intValue();
        int donLoc = dtLoc > 0 ? Math.max(1, Math.min(Math.max(1, hoanThanhGoc), spKhop.size())) : 0;
        cs.put("doanhThuThucTe", dtLoc);
        cs.put("doanhThuDuKien", dtLoc);
        cs.put("soDon", donLoc);
        cs.put("soHoanThanh", donLoc);
        cs.put("soHuy", 0);
        cs.put("giaTriTrungBinh", donLoc > 0 ? Math.round((double) dtLoc / donLoc) : 0L);
        cs.put("soSanPhamBan", slLoc);
    }

    /** Tổng hợp sản phẩm đối chiếu qua tất cả các kỳ đã chọn, hỗ trợ lọc xu hướng và sắp xếp giống giao diện. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> sanPhamDoiChieu(List<Ky> dsKy, List<HoaDonChiTiet> chiTiets,
                                                     String xuHuongLoc, String sapXepSp) {
        if (dsKy == null || dsKy.isEmpty()) return List.of();
        Ky kyCuoi = dsKy.get(dsKy.size() - 1);
        Ky kyKeCuoi = dsKy.size() >= 2 ? dsKy.get(dsKy.size() - 2) : null;

        Map<Integer, Map<String, Object>> theoSp = new LinkedHashMap<>();
        for (HoaDonChiTiet ct : chiTiets) {
            if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null || ct.getSanPhamChiTiet() == null) continue;
            SanPham sp = ct.getSanPhamChiTiet().getSanPham();
            if (sp == null) continue;
            List<Ky> kyKhop = new ArrayList<>();
            for (Ky k : dsKy) {
                if (trongKy(ct.getHoaDon(), k)) kyKhop.add(k);
            }
            if (kyKhop.isEmpty()) continue;

            String tenTh = sp.getThuongHieu() != null && sp.getThuongHieu().getTen() != null
                    ? sp.getThuongHieu().getTen().trim()
                    : "FootStyle";
            Map<String, Object> m = theoSp.computeIfAbsent(sp.getId(), id -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("idSanPham", sp.getId());
                x.put("maSanPham", sp.getMaSanPham() == null ? "SP" : sp.getMaSanPham());
                x.put("tenSanPham", sp.getTenSanPham() == null ? "Sản phẩm" : sp.getTenSanPham());
                x.put("thuongHieu", tenTh.isEmpty() ? "FootStyle" : tenTh);
                x.put("tongBan", 0L);
                x.put("tongDoanhThu", 0L);
                x.put("soLuong", 0L);
                x.put("doanhThu", 0L);
                x.put("soLuongKyTruoc", 0L);
                x.put("theoKy", new LinkedHashMap<String, Long>());
                return x;
            });
            long sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            long dt = ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue();
            Map<String, Long> theoKyMap = (Map<String, Long>) m.get("theoKy");
            for (Ky k : kyKhop) {
                m.put("tongBan", (Long) m.get("tongBan") + sl);
                m.put("tongDoanhThu", (Long) m.get("tongDoanhThu") + dt);
                theoKyMap.put(k.ma(), theoKyMap.getOrDefault(k.ma(), 0L) + sl);
                if (k.ma().equals(kyCuoi.ma())) {
                    m.put("soLuong", (Long) m.get("soLuong") + sl);
                    m.put("doanhThu", (Long) m.get("doanhThu") + dt);
                }
                if (kyKeCuoi != null && k.ma().equals(kyKeCuoi.ma())) {
                    m.put("soLuongKyTruoc", (Long) m.get("soLuongKyTruoc") + sl);
                }
            }
        }

        List<Map<String, Object>> ds = new ArrayList<>();
        for (Map<String, Object> item : theoSp.values()) {
            long tongBan = (Long) item.get("tongBan");
            if (tongBan <= 0) continue;
            long slCuoi = (Long) item.get("soLuong");
            long slDau = (Long) item.get("soLuongKyTruoc");
            Double pct;
            if (kyKeCuoi != null) {
                pct = slDau == 0 ? (slCuoi > 0 ? 100.0 : 0.0) : tangTruong(slCuoi, slDau);
            } else {
                pct = slCuoi > 0 ? 100.0 : 0.0;
            }
            double numPct = pct == null ? 0.0 : pct;
            String huong = numPct > 0 ? "TANG" : numPct < 0 ? "GIAM" : "ON_DINH";
            String nhanXuHuong = numPct > 0
                    ? "↑ Tăng (+" + String.valueOf(Math.abs(numPct)).replace('.', ',') + "%)"
                    : numPct < 0
                            ? "↓ Giảm (-" + String.valueOf(Math.abs(numPct)).replace('.', ',') + "%)"
                            : "→ Ổn định (0%)";
            item.put("tangTruong", pct);
            item.put("huong", huong);
            item.put("nhanXuHuong", nhanXuHuong);
            if ((Long) item.get("doanhThu") == 0L) {
                item.put("doanhThu", item.get("tongDoanhThu"));
            }
            if (xuHuongLoc != null && !xuHuongLoc.isBlank() && !"TAT_CA".equalsIgnoreCase(xuHuongLoc.trim())) {
                if (!huong.equalsIgnoreCase(xuHuongLoc.trim())) continue;
            }
            ds.add(item);
        }

        String sx = sapXepSp == null ? "SL_GIAM" : sapXepSp.trim().toUpperCase(Locale.ROOT);
        ds.sort((a, b) -> switch (sx) {
            case "DT_GIAM" -> Long.compare((Long) b.get("tongDoanhThu"), (Long) a.get("tongDoanhThu"));
            case "TANG_GIAM" -> Double.compare(
                    b.get("tangTruong") == null ? 0.0 : (Double) b.get("tangTruong"),
                    a.get("tangTruong") == null ? 0.0 : (Double) a.get("tangTruong"));
            case "GIAM_MANH" -> Double.compare(
                    a.get("tangTruong") == null ? 0.0 : (Double) a.get("tangTruong"),
                    b.get("tangTruong") == null ? 0.0 : (Double) b.get("tangTruong"));
            default -> Long.compare((Long) b.get("tongBan"), (Long) a.get("tongBan"));
        });
        return ds.size() > 20 ? ds.subList(0, 20) : ds;
    }

    private Map<String, Object> soSanhTheoBoLoc(String kieuChuoi, String kyChuoi, List<String> dsKyChuoi,
                                                String thuongHieu, String tuKhoaSp,
                                                String xuHuongSp, String sapXepSp, String sapXepKy) {
        String th = thuongHieu == null ? "" : thuongHieu.trim();
        String kwRaw = tuKhoaSp == null ? "" : tuKhoaSp.trim();
        String kw = boDauTiengViet(kwRaw);
        boolean coLocSp = !th.isEmpty() || !kw.isEmpty();
        boolean coLocThem = (xuHuongSp != null && !xuHuongSp.isBlank() && !"TAT_CA".equalsIgnoreCase(xuHuongSp))
                || (sapXepSp != null && !sapXepSp.isBlank() && !"SL_GIAM".equalsIgnoreCase(sapXepSp))
                || (sapXepKy != null && !sapXepKy.isBlank() && !"THOI_GIAN_TANG".equalsIgnoreCase(sapXepKy));

        List<String> dsMa = new ArrayList<>();
        if (dsKyChuoi != null) {
            for (String m : dsKyChuoi) {
                if (m != null && !m.isBlank() && !dsMa.contains(m.trim())) {
                    dsMa.add(m.trim());
                }
            }
        }
        if (dsMa.isEmpty() && !coLocSp && !coLocThem) {
            return new LinkedHashMap<>(soSanh(kieuChuoi, kyChuoi));
        }

        Kieu kieu = docKieu(kieuChuoi);
        List<Ky> dsKyChon = new ArrayList<>();
        for (String ma : dsMa) {
            dsKyChon.add(docKy(kieu, ma));
        }
        dsKyChon.sort((a, b) -> a.tu().compareTo(b.tu()));

        Ky ky = !dsKyChon.isEmpty() ? dsKyChon.get(dsKyChon.size() - 1) : docKy(kieu, kyChuoi);
        Ky truoc = dsKyChon.size() >= 2 ? dsKyChon.get(dsKyChon.size() - 2) : kyTruoc(kieu, ky);
        Ky cungKy = cungKyNamTruoc(kieu, ky);
        if (dsKyChon.isEmpty()) {
            dsKyChon.add(truoc);
            dsKyChon.add(ky);
        } else if (dsKyChon.size() == 1) {
            List<Ky> dayDu = new ArrayList<>();
            dayDu.add(truoc);
            dayDu.add(ky);
            dsKyChon = dayDu;
        }

        int namDau = kieu == Kieu.NAM ? ky.tu().getYear() - (SO_NAM_XU_HUONG - 1) : ky.tu().getYear() - 1;
        LocalDate tu = LocalDate.of(namDau, 1, 1).isBefore(truoc.tu()) ? LocalDate.of(namDau, 1, 1) : truoc.tu();
        LocalDate den = LocalDate.of(ky.tu().getYear(), 12, 31).isAfter(ky.den())
                ? LocalDate.of(ky.tu().getYear(), 12, 31)
                : ky.den();
        for (Ky item : dsKyChon) {
            if (item.tu().isBefore(tu)) tu = item.tu();
            if (item.den().isAfter(den)) den = item.den();
        }

        List<HoaDon> hoaDons = hoaDonRepo.findByNgayTaoBetween(tu.atStartOfDay(), den.atTime(LocalTime.MAX));
        List<HoaDonChiTiet> chiTiets = chiTietRepo.findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(
                HoaDon.HOAN_THANH, tu.atStartOfDay(), den.atTime(LocalTime.MAX));
        if (coLocSp) {
            chiTiets = chiTiets.stream().filter(ct -> khopBoLocSanPham(ct, th, kw)).toList();
        }

        Map<String, Object> hienTai = chiSo(ky, hoaDons, chiTiets);
        Map<String, Object> kt = chiSo(truoc, hoaDons, chiTiets);
        Map<String, Object> ck = chiSo(cungKy, hoaDons, chiTiets);
        if (coLocSp) {
            apDungLocSanPhamChoChiSo(hienTai);
            apDungLocSanPhamChoChiSo(kt);
            apDungLocSanPhamChoChiSo(ck);
        }

        List<Map<String, Object>> cacKyDoiChieu = new ArrayList<>();
        for (Ky item : dsKyChon) {
            Map<String, Object> cs = chiSo(item, hoaDons, chiTiets);
            if (coLocSp) apDungLocSanPhamChoChiSo(cs);
            cacKyDoiChieu.add(cs);
        }

        List<HoaDon> hoaDonsChoBieuDo = hoaDons;
        if (coLocSp) {
            Map<Integer, HoaDon> mapHdLoc = new LinkedHashMap<>();
            for (HoaDonChiTiet ct : chiTiets) {
                if (ct.getHoaDon() == null || ct.getHoaDon().getId() == null) continue;
                HoaDon goc = ct.getHoaDon();
                long tienCt = ct.getThanhTien() == null ? 0L : ct.getThanhTien().longValue();
                HoaDon banSao = mapHdLoc.computeIfAbsent(goc.getId(), id -> {
                    HoaDon h = new HoaDon();
                    h.setId(goc.getId());
                    h.setMaHoaDon(goc.getMaHoaDon());
                    h.setNgayTao(goc.getNgayTao());
                    h.setTrangThai(HoaDon.HOAN_THANH);
                    h.setLoaiDon(goc.getLoaiDon());
                    h.setPhiVanChuyen(BigDecimal.ZERO);
                    h.setThanhTien(BigDecimal.ZERO);
                    h.setTongTien(BigDecimal.ZERO);
                    return h;
                });
                BigDecimal moi = banSao.getThanhTien().add(BigDecimal.valueOf(tienCt));
                banSao.setThanhTien(moi);
                banSao.setTongTien(moi);
            }
            hoaDonsChoBieuDo = new ArrayList<>(mapHdLoc.values());
        }

        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("kieu", kieu.name());
        kq.put("ky", kyDto(ky));
        kq.put("hienTai", hienTai);
        kq.put("kyTruoc", kt);
        kq.put("cungKyNamTruoc", kieu == Kieu.NAM ? null : ck);
        kq.put("tangSoVoiKyTruoc", bangTang(hienTai, kt));
        kq.put("tangSoVoiCungKy", kieu == Kieu.NAM ? null : bangTang(hienTai, ck));
        kq.put("xuHuong", xuHuong(kieu, ky, hoaDonsChoBieuDo));
        kq.put("chiTietKy", chiTietKy(kieu, ky, hoaDonsChoBieuDo));
        kq.put("sanPham", sanPhamDoiChieu(dsKyChon, chiTiets, xuHuongSp, sapXepSp));
        if (!cacKyDoiChieu.isEmpty()) {
            kq.put("cacKyDoiChieu", cacKyDoiChieu);
        }
        if (!th.isEmpty()) kq.put("thuongHieuLoc", th);
        if (!kwRaw.isEmpty()) kq.put("tuKhoaSpLoc", kwRaw);
        if (xuHuongSp != null && !xuHuongSp.isBlank() && !"TAT_CA".equalsIgnoreCase(xuHuongSp)) {
            kq.put("xuHuongSpLoc", xuHuongSp.trim());
        }
        if (sapXepKy != null && !sapXepKy.isBlank()) {
            kq.put("sapXepKyLoc", sapXepKy.trim());
        }
        return kq;
    }

    /** Lấy cấu hình lịch gửi báo cáo tự động. */
    public Map<String, Object> layLichTuDong() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", emailTuDong);
        m.put("hangNgay", tuDongHangNgay);
        m.put("hangThang", tuDongThang);
        m.put("hangNam", tuDongNam);
        return m;
    }

    /** Lưu cấu hình lịch gửi báo cáo tự động. */
    public Map<String, Object> luuLichTuDong(String email, Boolean hangNgay, Boolean hangThang, Boolean hangNam) {
        String raw = email == null ? "" : email.trim();
        if (!raw.isEmpty()) {
            if (raw.contains("..") || !raw.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
                throw ApiException.badRequest("Email nhận tự động không hợp lệ.");
            }
        }
        this.emailTuDong = raw;
        if (hangNgay != null) this.tuDongHangNgay = hangNgay;
        if (hangThang != null) this.tuDongThang = hangThang;
        if (hangNam != null) this.tuDongNam = hangNam;
        return layLichTuDong();
    }

    /** Email TỰ ĐỘNG HẰNG NGÀY: 00:05 mỗi ngày (Giờ Việt Nam) tự gửi báo cáo ngày vừa kết thúc. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao-ngay:0 5 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void baoCaoNgayTuDong() {
        if (!tuDongHangNgay) return;
        String ky = LocalDate.now(dongHo).minusDays(1).toString();
        guiBaoCaoChoQuanLy(ky, soSanh("NGAY", ky));
    }

    /** Email TỰ ĐỘNG CUỐI THÁNG: 22:30 ngày cuối tháng gửi báo cáo tổng kết tháng hiện tại cho quản lý. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao-cuoi-thang:0 30 22 28-31 * *}", zone = "Asia/Ho_Chi_Minh")
    public void baoCaoCuoiThangTuDong() {
        if (!tuDongThang) return;
        LocalDate homNay = LocalDate.now(dongHo);
        if (homNay.getDayOfMonth() != homNay.lengthOfMonth()) return;
        String ky = YearMonth.from(homNay).toString();
        guiBaoCaoChoQuanLy(ky, soSanh("THANG", ky));
    }

    /** Email TỰ ĐỘNG ĐẦU THÁNG: 00:05 ngày 1 tháng sau (Giờ Việt Nam) gửi báo cáo toàn bộ tháng vừa kết thúc. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao:0 5 0 1 * *}", zone = "Asia/Ho_Chi_Minh")
    public void baoCaoThangTuDong() {
        if (!tuDongThang) return;
        String ky = YearMonth.from(LocalDate.now(dongHo)).minusMonths(1).toString();
        guiBaoCaoChoQuanLy(ky, soSanh("THANG", ky));
    }

    /** Email TỰ ĐỘNG ĐẦU NĂM: 00:10 ngày 1/1 (Giờ Việt Nam) gửi báo cáo toàn bộ năm vừa kết thúc. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao-nam:0 10 0 1 1 *}", zone = "Asia/Ho_Chi_Minh")
    public void baoCaoNamTuDong() {
        if (!tuDongNam) return;
        String ky = String.valueOf(LocalDate.now(dongHo).getYear() - 1);
        guiBaoCaoChoQuanLy(ky, soSanh("NAM", ky));
    }

    private void guiBaoCaoChoQuanLy(String ky, Map<String, Object> d) {
        Set<String> daGui = new HashSet<>();
        if (emailTuDong != null && !emailTuDong.isBlank() && emailTuDong.contains("@")) {
            try {
                guiThu(emailTuDong.trim(), d);
                daGui.add(emailTuDong.trim().toLowerCase(Locale.ROOT));
            } catch (Exception e) {
                log.error("Không gửi được báo cáo tự động {} tới {}: {}", ky, emailTuDong, e.getMessage());
            }
        }
        for (NhanVien nv : nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.HOAT_DONG)) {
            boolean ql = nv.getVaiTro() != null && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(String.valueOf(nv.getVaiTro().getTenVaiTro()).trim());
            if (!ql || nv.getEmail() == null || !nv.getEmail().contains("@")) continue;
            String em = nv.getEmail().trim();
            if (!daGui.add(em.toLowerCase(Locale.ROOT))) continue;
            try {
                guiThu(em, d);
            } catch (Exception e) {
                log.error("Không gửi được báo cáo {} tới {}: {}", ky, em, e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    boolean guiThu(String den, Map<String, Object> d) throws Exception {
        Map<String, Object> ky = (Map<String, Object>) d.get("ky");
        Map<String, Object> a = (Map<String, Object>) d.get("hienTai");
        Map<String, Object> kt = (Map<String, Object>) d.get("kyTruoc");
        Map<String, Object> ck = (Map<String, Object>) d.get("cungKyNamTruoc");
        String nhanKy = String.valueOf(ky.get("nhan"));
        String tieuDeTuyChon = (String) d.get("tieuDeTuyChon");
        String tieuDe = (tieuDeTuyChon != null && !tieuDeTuyChon.isBlank())
                ? tieuDeTuyChon
                : "[FootStyle] Báo cáo phân tích & so sánh doanh thu " + nhanKy;
        String ghiChuNguoiGui = (String) d.get("ghiChuNguoiGui");

        String thLoc = (String) d.get("thuongHieuLoc");
        String kwLoc = (String) d.get("tuKhoaSpLoc");
        String xhSpLoc = (String) d.get("xuHuongSpLoc");
        String sxKyLoc = (String) d.get("sapXepKyLoc");
        List<Map<String, Object>> cacKyGoc = (List<Map<String, Object>>) d.get("cacKyDoiChieu");
        List<Map<String, Object>> dsKy = (cacKyGoc != null && !cacKyGoc.isEmpty())
                ? new ArrayList<>(cacKyGoc)
                : List.of(kt, a);

        List<BieuDoPng.Cot> xu = new ArrayList<>();
        if (cacKyGoc != null && !cacKyGoc.isEmpty()) {
            for (Map<String, Object> item : dsKy) {
                Map<String, Object> kMap = (Map<String, Object>) item.get("ky");
                String tenK = kMap != null ? String.valueOf(kMap.get("nhan")) : "";
                long dtTt = ((Number) item.get("doanhThuThucTe")).longValue();
                long dtDk = ((Number) item.get("doanhThuDuKien")).longValue();
                xu.add(new BieuDoPng.Cot(tenK, dtTt, dtDk));
            }
        } else {
            for (Map<String, Object> p : (List<Map<String, Object>>) d.get("xuHuong")) {
                Object b = p.get("doanhThuNamTruoc");
                xu.add(new BieuDoPng.Cot(String.valueOf(p.get("nhan")), ((Number) p.get("doanhThu")).longValue(), b == null ? null : ((Number) b).longValue()));
            }
        }
        boolean coNamTruoc = !"NAM".equals(d.get("kieu"));
        byte[] anhXu = (cacKyGoc != null && !cacKyGoc.isEmpty())
                ? BieuDoPng.cotNhom("Biểu đồ đối chiếu doanh thu qua các kỳ đã chọn", "Doanh thu thực tế", "Doanh thu dự kiến", xu)
                : BieuDoPng.cotNhom("Doanh thu thực tế theo " + ("THANG".equals(d.get("kieu")) ? "tháng" : "QUY".equals(d.get("kieu")) ? "quý" : "năm"),
                        coNamTruoc ? "Năm nay" : "Doanh thu", coNamTruoc ? "Cùng kỳ năm trước" : null, xu);
        List<BieuDoPng.Cot> ct = new ArrayList<>();
        for (Map<String, Object> p : (List<Map<String, Object>>) d.get("chiTietKy")) {
            ct.add(new BieuDoPng.Cot(String.valueOf(p.get("nhan")), ((Number) p.get("doanhThu")).longValue(), ((Number) p.get("doanhThuNamTruoc")).longValue()));
        }
        byte[] anhCt = BieuDoPng.cotNhom("Chi tiết " + nhanKy, nhanKy, "Cùng kỳ năm trước", ct);

        long tongDtCacKy = 0;
        long tongDtDkCacKy = 0;
        long tongDonHtCacKy = 0;
        long tongSpCacKy = 0;
        List<Double> dsPct = new ArrayList<>();
        for (int i = 0; i < dsKy.size(); i++) {
            Map<String, Object> item = dsKy.get(i);
            long dtI = ((Number) item.get("doanhThuThucTe")).longValue();
            tongDtCacKy += dtI;
            tongDtDkCacKy += ((Number) item.get("doanhThuDuKien")).longValue();
            tongDonHtCacKy += ((Number) item.get("soHoanThanh")).longValue();
            tongSpCacKy += ((Number) item.get("soSanPhamBan")).longValue();
            if (i > 0) {
                long dtTr = ((Number) dsKy.get(i - 1).get("doanhThuThucTe")).longValue();
                Double p = dtTr == 0 ? (dtI > 0 ? 100.0 : 0.0) : tangTruong(dtI, dtTr);
                if (p != null) dsPct.add(p);
            }
        }
        long aovTb = tongDonHtCacKy > 0 ? Math.round((double) tongDtCacKy / tongDonHtCacKy) : 0;
        double tangTb = dsPct.isEmpty() ? 0.0 : Math.round(dsPct.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;

        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif;color:#18242f;font-size:14px\">");
        html.append("<h2 style=\"margin:0 0 4px\">Báo cáo phân tích &amp; so sánh ").append(esc(nhanKy)).append("</h2>");
        html.append("<p style=\"color:#5b6b80;margin:0 0 12px\">Từ ").append(ky.get("tuNgay")).append(" đến ").append(ky.get("denNgay"))
                .append(". Doanh thu không gồm phí giao hàng; thực tế = đơn đã hoàn thành, dự kiến = mọi đơn chưa hủy.</p>");
        if ((thLoc != null && !thLoc.isBlank()) || (kwLoc != null && !kwLoc.isBlank())
                || (xhSpLoc != null && !xhSpLoc.isBlank()) || (cacKyGoc != null && cacKyGoc.size() >= 2)) {
            List<String> moTaLoc = new ArrayList<>();
            if (cacKyGoc != null && cacKyGoc.size() >= 2) {
                List<String> tenCacKy = new ArrayList<>();
                for (Map<String, Object> item : cacKyGoc) {
                    Map<String, Object> kMap = (Map<String, Object>) item.get("ky");
                    if (kMap != null && kMap.get("nhan") != null) tenCacKy.add(String.valueOf(kMap.get("nhan")));
                }
                if (!tenCacKy.isEmpty()) moTaLoc.add("Các kỳ đối chiếu: " + String.join(", ", tenCacKy));
            }
            if (thLoc != null && !thLoc.isBlank()) moTaLoc.add("Thương hiệu: " + thLoc);
            if (kwLoc != null && !kwLoc.isBlank()) moTaLoc.add("Từ khóa SP: " + kwLoc);
            if (xhSpLoc != null && !xhSpLoc.isBlank()) moTaLoc.add("Xu hướng SP: " + xhSpLoc);
            if (!moTaLoc.isEmpty()) {
                html.append("<p style=\"background:#f8fafc;border:1px solid #e2e8f0;padding:8px 12px;border-radius:6px;margin:0 0 14px;color:#334155;font-size:13px\"><b>Bộ lọc áp dụng:</b> ")
                        .append(esc(String.join(" | ", moTaLoc))).append("</p>");
            }
        }

        StringBuilder chu = new StringBuilder("Báo cáo phân tích & so sánh " + nhanKy + "\n\n");
        if (ghiChuNguoiGui != null && !ghiChuNguoiGui.isBlank()) {
            html.append("<div style=\"background:#fffbeb;border:1px solid #fde68a;border-left:4px solid #d97706;padding:12px 16px;border-radius:8px;margin-bottom:14px\">")
                    .append("<b style=\"color:#b45309;display:block;margin-bottom:4px\">Ghi chú &amp; Đề xuất từ người gửi:</b>")
                    .append("<div style=\"line-height:1.5;color:#334155\">").append(esc(ghiChuNguoiGui)).append("</div></div>");
            chu.append("GHI CHÚ TỪ NGƯỜI GỬI:\n").append(ghiChuNguoiGui).append("\n\n");
        }

        // 4 thẻ KPI tổng hợp của các kỳ đang đối chiếu (khớp với 4 thẻ KPI trên giao diện So sánh)
        html.append("<table cellpadding=\"10\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e3eaf3;margin-bottom:16px;width:100%;max-width:760px\">")
                .append("<tr style=\"background:#f8fafc\">")
                .append("<td style=\"border:1px solid #e3eaf3\"><div style=\"color:#64748b;font-size:12px\">Tổng doanh thu các kỳ</div><b style=\"font-size:16px;color:#0f172a\">").append(so(tongDtCacKy, true)).append("</b></td>")
                .append("<td style=\"border:1px solid #e3eaf3\"><div style=\"color:#64748b;font-size:12px\">Tổng đơn hoàn thành</div><b style=\"font-size:16px;color:#0f172a\">").append(tongDonHtCacKy).append("</b></td>")
                .append("<td style=\"border:1px solid #e3eaf3\"><div style=\"color:#64748b;font-size:12px\">AOV trung bình</div><b style=\"font-size:16px;color:#0f172a\">").append(so(aovTb, true)).append("</b></td>")
                .append("<td style=\"border:1px solid #e3eaf3\"><div style=\"color:#64748b;font-size:12px\">Tăng trưởng bình quân</div><b style=\"font-size:16px;color:").append(tangTb >= 0 ? "#15803d" : "#be123c").append("\">").append(phanTram(tangTb)).append("</b></td>")
                .append("</tr></table>");

        // Bảng tổng hợp chỉ số kinh doanh theo kỳ (khớp với bảng trên giao diện So sánh)
        List<Map<String, Object>> dsKyBang = new ArrayList<>(dsKy);
        if ("THOI_GIAN_GIAM".equalsIgnoreCase(sxKyLoc)) {
            java.util.Collections.reverse(dsKyBang);
        } else if ("DT_GIAM".equalsIgnoreCase(sxKyLoc)) {
            dsKyBang.sort((x, y) -> Long.compare(((Number) y.get("doanhThuThucTe")).longValue(), ((Number) x.get("doanhThuThucTe")).longValue()));
        } else if ("DT_TANG".equalsIgnoreCase(sxKyLoc)) {
            dsKyBang.sort((x, y) -> Long.compare(((Number) x.get("doanhThuThucTe")).longValue(), ((Number) y.get("doanhThuThucTe")).longValue()));
        }
        html.append("<h3 style=\"margin:16px 0 6px\">I. Bảng tổng hợp chỉ số kinh doanh theo kỳ</h3>")
                .append("<table cellpadding=\"7\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e3eaf3;margin-bottom:16px\">")
                .append("<tr style=\"background:#f3f6fb\"><th align=\"left\">Kỳ báo cáo</th><th align=\"center\">Khoảng ngày</th><th align=\"right\">Đơn hoàn thành</th><th align=\"right\">Sản phẩm bán</th><th align=\"right\">Doanh thu thực tế</th><th align=\"right\">Giá trị TB/đơn (AOV)</th><th align=\"right\">Chênh lệch kỳ trước</th><th align=\"right\">Tốc độ tăng trưởng</th><th align=\"right\">Tỷ trọng</th></tr>");
        for (int i = 0; i < dsKyBang.size(); i++) {
            Map<String, Object> item = dsKyBang.get(i);
            Map<String, Object> kMap = (Map<String, Object>) item.get("ky");
            String tenK = kMap != null ? String.valueOf(kMap.get("nhan")) : "";
            String khoangK = kMap != null ? kMap.get("tuNgay") + " – " + kMap.get("denNgay") : "";
            long dtI = ((Number) item.get("doanhThuThucTe")).longValue();
            long donHt = ((Number) item.get("soHoanThanh")).longValue();
            long spBan = ((Number) item.get("soSanPhamBan")).longValue();
            long aovI = ((Number) item.get("giaTriTrungBinh")).longValue();
            int idxGoc = dsKy.indexOf(item);
            long dtTr = idxGoc > 0 ? ((Number) dsKy.get(idxGoc - 1).get("doanhThuThucTe")).longValue() : 0;
            long chenh = idxGoc > 0 ? dtI - dtTr : dtI;
            Double pctI = idxGoc > 0 ? (dtTr == 0 ? (dtI > 0 ? 100.0 : 0.0) : tangTruong(dtI, dtTr)) : (dtI > 0 ? 100.0 : 0.0);
            double tyTrong = tongDtCacKy > 0 ? Math.round((dtI * 1000.0) / tongDtCacKy) / 10.0 : 0.0;
            html.append("<tr style=\"border-top:1px solid #e3eaf3\"><td><b>").append(esc(tenK)).append("</b></td>")
                    .append("<td align=\"center\" style=\"color:#64748b;font-size:12px\">").append(esc(khoangK)).append("</td>")
                    .append("<td align=\"right\">").append(donHt).append("</td>")
                    .append("<td align=\"right\">").append(spBan).append("</td>")
                    .append("<td align=\"right\"><b>").append(so(dtI, true)).append("</b></td>")
                    .append("<td align=\"right\">").append(so(aovI, true)).append("</td>")
                    .append("<td align=\"right\">").append(chenh > 0 ? "+" + so(chenh, true) : so(chenh, true)).append("</td>")
                    .append("<td align=\"right\">").append(phanTram(pctI)).append("</td>")
                    .append("<td align=\"right\">").append(String.valueOf(tyTrong).replace('.', ',')).append("%</td></tr>");
        }
        html.append("<tr style=\"border-top:2px solid #cbd5e1;background:#f8fafc;font-weight:bold\">")
                .append("<td>TỔNG CỘNG / TRUNG BÌNH</td><td align=\"center\">").append(dsKy.size()).append(" kỳ</td>")
                .append("<td align=\"right\">").append(tongDonHtCacKy).append("</td>")
                .append("<td align=\"right\">").append(tongSpCacKy).append("</td>")
                .append("<td align=\"right\">").append(so(tongDtCacKy, true)).append("</td>")
                .append("<td align=\"right\">").append(so(aovTb, true)).append("</td>")
                .append("<td align=\"right\">—</td>")
                .append("<td align=\"right\">TB: ").append(phanTram(tangTb)).append("</td>")
                .append("<td align=\"right\">").append(tongDtCacKy > 0 ? "100%" : "0%").append("</td></tr></table>");

        // Bảng ma trận chỉ số chi tiết
        html.append("<h3 style=\"margin:16px 0 6px\">II. Ma trận đối chiếu chỉ số chi tiết</h3>")
                .append("<table cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e3eaf3\">")
                .append("<tr style=\"background:#f3f6fb\"><th align=\"left\">Chỉ số</th><th align=\"right\">").append(esc(nhanKy))
                .append("</th><th align=\"right\">").append(esc(String.valueOf(((Map<String, Object>) kt.get("ky")).get("nhan")))).append("</th><th align=\"right\">±</th>");
        if (ck != null) html.append("<th align=\"right\">").append(esc(String.valueOf(((Map<String, Object>) ck.get("ky")).get("nhan")))).append("</th><th align=\"right\">±</th>");
        html.append("</tr>");
        for (String k : CHI_SO_SO_SANH) {
            boolean tien = k.startsWith("doanhThu") || k.equals("giaTriTrungBinh") || k.equals("tongGiamGia");
            long va = ((Number) a.get(k)).longValue();
            long vb = ((Number) kt.get(k)).longValue();
            html.append("<tr style=\"border-top:1px solid #e3eaf3\"><td>").append(esc(TEN_CHI_SO.get(k))).append("</td><td align=\"right\"><b>")
                    .append(so(va, tien)).append("</b></td><td align=\"right\">").append(so(vb, tien)).append("</td><td align=\"right\">")
                    .append(phanTram(tangTruong(va, vb))).append("</td>");
            if (ck != null) {
                long vc = ((Number) ck.get(k)).longValue();
                html.append("<td align=\"right\">").append(so(vc, tien)).append("</td><td align=\"right\">").append(phanTram(tangTruong(va, vc))).append("</td>");
            }
            html.append("</tr>");
            chu.append("- ").append(TEN_CHI_SO.get(k)).append(": ").append(so(va, tien)).append(" (kỳ trước ").append(so(vb, tien)).append(", ")
                    .append(phanTram(tangTruong(va, vb))).append(")\n");
        }
        html.append("</table>");

        html.append("<p style=\"margin:20px 0 6px\"><img src=\"cid:bieu-do-xu-huong\" alt=\"Biểu đồ xu hướng\" style=\"max-width:100%\"></p>");
        html.append("<p style=\"margin:6px 0\"><img src=\"cid:bieu-do-chi-tiet\" alt=\"Biểu đồ chi tiết\" style=\"max-width:100%\"></p>");
        List<Map<String, Object>> sps = (List<Map<String, Object>>) d.get("sanPham");
        if (!sps.isEmpty()) {
            html.append("<h3 style=\"margin:20px 0 6px\">III. Top sản phẩm bán chạy &amp; Đối chiếu sức mua qua các kỳ</h3>")
                    .append("<table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e3eaf3\">")
                    .append("<tr style=\"background:#f3f6fb\"><th align=\"center\">#</th><th align=\"left\">Mã SP</th><th align=\"left\">Sản phẩm</th><th align=\"left\">Thương hiệu</th><th align=\"right\">Tổng bán</th><th align=\"right\">Tổng doanh thu</th>");
            for (Map<String, Object> item : dsKy) {
                Map<String, Object> kMap = (Map<String, Object>) item.get("ky");
                html.append("<th align=\"right\">").append(esc(kMap != null ? String.valueOf(kMap.get("nhan")) : "")).append("</th>");
            }
            html.append("<th align=\"right\">Xu hướng (±%)</th></tr>");
            int sttSp = 1;
            for (Map<String, Object> sp : sps) {
                Map<String, Long> theoKyMap = (Map<String, Long>) sp.get("theoKy");
                long tongBanSp = sp.get("tongBan") instanceof Number n ? n.longValue() : ((Number) sp.get("soLuong")).longValue();
                long tongDtSp = sp.get("tongDoanhThu") instanceof Number n ? n.longValue() : ((Number) sp.get("doanhThu")).longValue();
                html.append("<tr style=\"border-top:1px solid #e3eaf3\"><td align=\"center\">").append(sttSp++).append("</td>")
                        .append("<td>").append(esc(String.valueOf(sp.getOrDefault("maSanPham", "SP")))).append("</td>")
                        .append("<td><b>").append(esc(String.valueOf(sp.get("tenSanPham")))).append("</b></td>")
                        .append("<td>").append(esc(String.valueOf(sp.getOrDefault("thuongHieu", "FootStyle")))).append("</td>")
                        .append("<td align=\"right\"><b>").append(tongBanSp).append("</b></td>")
                        .append("<td align=\"right\">").append(so(tongDtSp, true)).append("</td>");
                for (int i = 0; i < dsKy.size(); i++) {
                    Map<String, Object> kMap = (Map<String, Object>) dsKy.get(i).get("ky");
                    String maK = kMap != null ? String.valueOf(kMap.get("ma")) : "";
                    long slK = theoKyMap != null && theoKyMap.containsKey(maK)
                            ? theoKyMap.get(maK)
                            : (i == dsKy.size() - 1 ? ((Number) sp.get("soLuong")).longValue()
                                    : (i == dsKy.size() - 2 ? ((Number) sp.get("soLuongKyTruoc")).longValue() : 0L));
                    html.append("<td align=\"right\">").append(slK).append("</td>");
                }
                String xhText = sp.get("nhanXuHuong") != null ? String.valueOf(sp.get("nhanXuHuong")) : phanTram((Double) sp.get("tangTruong"));
                html.append("<td align=\"right\">").append(esc(xhText)).append("</td></tr>");
            }
            html.append("</table>");
        }
        html.append("<p style=\"color:#7b8794;margin-top:20px\">Thư gửi từ hệ thống FootStyle (kèm tệp Excel báo cáo chi tiết đính kèm). Xem chi tiết tại trang Thống kê &gt; So sánh.</p></div>");
        Map<String, byte[]> anh = new LinkedHashMap<>();
        anh.put("bieu-do-xu-huong", anhXu);
        anh.put("bieu-do-chi-tiet", anhCt);
        String maKy = String.valueOf(ky.get("ma")).replaceAll("[^A-Za-z0-9_-]", "_");
        String tenFileXlsx = "Bao-cao-thong-ke_" + String.valueOf(d.get("kieu")).toLowerCase(Locale.ROOT) + "_" + maKy + ".xlsx";
        byte[] fileXlsx = taoFileExcelBaoCao(d);
        return mailService.guiBaoCao(den, tieuDe, chu.toString(), html.toString(), anh, tenFileXlsx, fileXlsx);
    }

    /** Tạo tệp Excel (.xlsx) chuyên nghiệp nhiều sheet đính kèm theo email báo cáo thống kê. */
    @SuppressWarnings("unchecked")
    static byte[] taoFileExcelBaoCao(Map<String, Object> d) throws Exception {
        Map<String, Object> ky = (Map<String, Object>) d.get("ky");
        Map<String, Object> a = (Map<String, Object>) d.get("hienTai");
        Map<String, Object> kt = (Map<String, Object>) d.get("kyTruoc");
        Map<String, Object> ck = (Map<String, Object>) d.get("cungKyNamTruoc");
        String nhanKy = String.valueOf(ky.get("nhan"));
        String nhanKt = String.valueOf(((Map<String, Object>) kt.get("ky")).get("nhan"));
        String nhanCk = ck != null ? String.valueOf(((Map<String, Object>) ck.get("ky")).get("nhan")) : null;
        String thLoc = (String) d.get("thuongHieuLoc");
        String kwLoc = (String) d.get("tuKhoaSpLoc");
        List<Map<String, Object>> cacKyGoc = (List<Map<String, Object>>) d.get("cacKyDoiChieu");
        List<Map<String, Object>> dsKy = (cacKyGoc != null && !cacKyGoc.isEmpty())
                ? cacKyGoc
                : List.of(kt, a);
        List<String> tenCacKy = new ArrayList<>();
        for (Map<String, Object> item : dsKy) {
            Map<String, Object> kMap = (Map<String, Object>) item.get("ky");
            if (kMap != null && kMap.get("nhan") != null) tenCacKy.add(String.valueOf(kMap.get("nhan")));
        }
        String phuDe = "Các kỳ đối chiếu: " + String.join(" vs ", tenCacKy)
                + (thLoc != null && !thLoc.isBlank() ? " | Thương hiệu: " + thLoc : "")
                + (kwLoc != null && !kwLoc.isBlank() ? " | Từ khóa SP: " + kwLoc : "")
                + " | Đơn vị: FootStyle";

        // Sheet 1: Đối chiếu các kỳ đã chọn
        List<String> h1 = List.of("STT", "Kỳ so sánh", "Khoảng thời gian", "Doanh thu thực tế (₫)", "Doanh thu dự kiến (₫)",
                "Tổng số đơn", "Đơn hoàn thành", "Đơn đã hủy", "Giá trị TB / đơn (₫)", "Sản phẩm đã bán", "Chênh lệch DT (₫)", "Tăng trưởng (±%)");
        List<List<Object>> r1 = new ArrayList<>();
        long tongDt = 0, tongDk = 0, tongDon = 0, tongHt = 0, tongHuy = 0, tongSp = 0;
        for (int i = 0; i < dsKy.size(); i++) {
            Map<String, Object> item = dsKy.get(i);
            Map<String, Object> kMap = (Map<String, Object>) item.get("ky");
            String tenK = kMap != null ? String.valueOf(kMap.get("nhan")) : "";
            String khoangK = kMap != null ? kMap.get("tuNgay") + " - " + kMap.get("denNgay") : "";
            long dtI = ((Number) item.get("doanhThuThucTe")).longValue();
            long dkI = ((Number) item.get("doanhThuDuKien")).longValue();
            long donI = ((Number) item.get("soDon")).longValue();
            long htI = ((Number) item.get("soHoanThanh")).longValue();
            long huyI = ((Number) item.get("soHuy")).longValue();
            long aovI = ((Number) item.get("giaTriTrungBinh")).longValue();
            long spI = ((Number) item.get("soSanPhamBan")).longValue();
            long dtTr = i > 0 ? ((Number) dsKy.get(i - 1).get("doanhThuThucTe")).longValue() : 0;
            long chenh = i > 0 ? dtI - dtTr : dtI;
            Double pctI = i > 0 ? (dtTr == 0 ? (dtI > 0 ? 100.0 : 0.0) : tangTruong(dtI, dtTr)) : (dtI > 0 ? 100.0 : 0.0);
            tongDt += dtI;
            tongDk += dkI;
            tongDon += donI;
            tongHt += htI;
            tongHuy += huyI;
            tongSp += spI;
            r1.add(List.of(i + 1, tenK, khoangK, dtI, dkI, donI, htI, huyI, aovI, spI, chenh, phanTram(pctI)));
        }
        r1.add(List.of("", "TỔNG CỘNG CÁC KỲ", dsKy.size() + " kỳ đối chiếu", tongDt, tongDk, tongDon, tongHt, tongHuy,
                tongHt > 0 ? Math.round((double) tongDt / tongHt) : 0L, tongSp, "—", "—"));

        // Sheet 2: Ma trận chỉ số cốt lõi
        List<String> h2 = new ArrayList<>(List.of("STT", "Chỉ số kinh doanh", nhanKy, nhanKt, "Chênh lệch kỳ trước", "±% kỳ trước"));
        if (ck != null) h2.addAll(List.of(nhanCk, "Chênh lệch cùng kỳ", "±% cùng kỳ"));
        List<List<Object>> r2 = new ArrayList<>();
        int stt = 1;
        for (String k : CHI_SO_SO_SANH) {
            long va = ((Number) a.get(k)).longValue();
            long vb = ((Number) kt.get(k)).longValue();
            List<Object> row = new ArrayList<>(List.of(stt++, TEN_CHI_SO.get(k), va, vb, va - vb, phanTram(tangTruong(va, vb))));
            if (ck != null) {
                long vc = ((Number) ck.get(k)).longValue();
                row.addAll(List.of(vc, va - vc, phanTram(tangTruong(va, vc))));
            }
            r2.add(row);
        }

        // Sheet 3: Chi tiết trong kỳ
        List<String> h3 = List.of("STT", "Mốc thời gian", "Số đơn hoàn thành", "Doanh thu thực tế (₫)", "Doanh thu dự kiến (₫)", "Cùng kỳ năm trước (₫)", "Chênh lệch (₫)");
        List<List<Object>> r3 = new ArrayList<>();
        List<Map<String, Object>> ct = (List<Map<String, Object>>) d.getOrDefault("chiTietKy", List.of());
        int idx3 = 1;
        for (Map<String, Object> x : ct) {
            long dt = ((Number) x.get("doanhThu")).longValue();
            long dk = ((Number) x.get("doanhThuDuKien")).longValue();
            long nt = ((Number) x.get("doanhThuNamTruoc")).longValue();
            r3.add(List.of(idx3++, String.valueOf(x.get("nhan")), ((Number) x.get("soDon")).intValue(), dt, dk, nt, dt - nt));
        }

        // Sheet 4: Top sản phẩm qua các kỳ
        List<String> h4 = new ArrayList<>(List.of("Hạng", "Mã SP", "Tên sản phẩm", "Thương hiệu", "Tổng bán", "Tổng doanh thu (₫)"));
        for (String tenK : tenCacKy) h4.add(tenK);
        h4.add("Xu hướng");
        List<List<Object>> r4 = new ArrayList<>();
        List<Map<String, Object>> sps = (List<Map<String, Object>>) d.getOrDefault("sanPham", List.of());
        int idx4 = 1;
        for (Map<String, Object> sp : sps) {
            Map<String, Long> theoKyMap = (Map<String, Long>) sp.get("theoKy");
            long tongBanSp = sp.get("tongBan") instanceof Number n ? n.longValue() : ((Number) sp.get("soLuong")).longValue();
            long tongDtSp = sp.get("tongDoanhThu") instanceof Number n ? n.longValue() : ((Number) sp.get("doanhThu")).longValue();
            List<Object> rowSp = new ArrayList<>(List.of(
                    idx4++,
                    String.valueOf(sp.getOrDefault("maSanPham", "SP")),
                    String.valueOf(sp.get("tenSanPham")),
                    String.valueOf(sp.getOrDefault("thuongHieu", "FootStyle")),
                    tongBanSp,
                    tongDtSp
            ));
            for (int i = 0; i < dsKy.size(); i++) {
                Map<String, Object> kMap = (Map<String, Object>) dsKy.get(i).get("ky");
                String maK = kMap != null ? String.valueOf(kMap.get("ma")) : "";
                long slK = theoKyMap != null && theoKyMap.containsKey(maK)
                        ? theoKyMap.get(maK)
                        : (i == dsKy.size() - 1 ? ((Number) sp.get("soLuong")).longValue()
                                : (i == dsKy.size() - 2 ? ((Number) sp.get("soLuongKyTruoc")).longValue() : 0L));
                rowSp.add(slK);
            }
            String xhText = sp.get("nhanXuHuong") != null ? String.valueOf(sp.get("nhanXuHuong")) : phanTram((Double) sp.get("tangTruong"));
            rowSp.add(xhText);
            r4.add(rowSp);
        }

        Map<String, String> files = new LinkedHashMap<>();
        files.put("[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet4.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                        + "</Types>");
        files.put("_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                        + "</Relationships>");
        files.put("xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                        + "<sheets>"
                        + "<sheet name=\"Tổng hợp\" sheetId=\"1\" r:id=\"rId1\"/>"
                        + "<sheet name=\"Xu hướng\" sheetId=\"2\" r:id=\"rId2\"/>"
                        + "<sheet name=\"Chi tiết trong kỳ\" sheetId=\"3\" r:id=\"rId3\"/>"
                        + "<sheet name=\"Sản phẩm bán chạy\" sheetId=\"4\" r:id=\"rId4\"/>"
                        + "</sheets></workbook>");
        files.put("xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                        + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>"
                        + "<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet4.xml\"/>"
                        + "<Relationship Id=\"rId5\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                        + "</Relationships>");
        files.put("xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<fonts count=\"4\">"
                        + "<font><sz val=\"11\"/><name val=\"Segoe UI\"/></font>"
                        + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Segoe UI\"/></font>"
                        + "<font><b/><sz val=\"13\"/><color rgb=\"FF0F172A\"/><name val=\"Segoe UI\"/></font>"
                        + "<font><i/><sz val=\"10\"/><color rgb=\"FF475569\"/><name val=\"Segoe UI\"/></font>"
                        + "</fonts>"
                        + "<fills count=\"3\">"
                        + "<fill><patternFill patternType=\"none\"/></fill>"
                        + "<fill><patternFill patternType=\"gray125\"/></fill>"
                        + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1E3A8A\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                        + "</fills>"
                        + "<borders count=\"2\">"
                        + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                        + "<border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/><diagonal/></border>"
                        + "</borders>"
                        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                        + "<cellXfs count=\"5\">"
                        + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"
                        + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                        + "</cellXfs></styleSheet>");
        files.put("xl/worksheets/sheet1.xml", taoSheetXml("I. BÁO CÁO ĐỐI CHIẾU CHỈ SỐ KINH DOANH — " + nhanKy, phuDe, h1, r1));
        files.put("xl/worksheets/sheet2.xml", taoSheetXml("II. XU HƯỚNG DOANH THU CHUỖI KỲ", phuDe, h2, r2));
        files.put("xl/worksheets/sheet3.xml", taoSheetXml("III. CHI TIẾT DOANH THU TRONG KỲ — " + nhanKy, phuDe, h3, r3));
        files.put("xl/worksheets/sheet4.xml", taoSheetXml("IV. TOP SẢN PHẨM BÁN CHẠY TRONG KỲ", phuDe, h4, r4));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    private static String taoSheetXml(String tieuDe, String phuDe, List<String> headers, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<cols>");
        for (int i = 0; i < headers.size(); i++) {
            int w = i == 0 ? 8 : i == 1 ? 30 : 20;
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        sb.append("<row r=\"1\" ht=\"24\" customHeight=\"1\"><c r=\"A1\" s=\"3\" t=\"inlineStr\"><is><t>").append(esc(tieuDe)).append("</t></is></c></row>");
        sb.append("<row r=\"2\" ht=\"19\" customHeight=\"1\"><c r=\"A2\" s=\"4\" t=\"inlineStr\"><is><t>").append(esc(phuDe)).append("</t></is></c></row>");
        sb.append("<row r=\"4\" ht=\"24\" customHeight=\"1\">");
        for (int c = 0; c < headers.size(); c++) {
            String ref = tenCotExcel(c) + "4";
            sb.append("<c r=\"").append(ref).append("\" s=\"1\" t=\"inlineStr\"><is><t>").append(esc(headers.get(c))).append("</t></is></c>");
        }
        sb.append("</row>");
        int rIdx = 5;
        for (List<Object> row : rows) {
            sb.append("<row r=\"").append(rIdx).append("\" ht=\"20\" customHeight=\"1\">");
            for (int c = 0; c < row.size(); c++) {
                String ref = tenCotExcel(c) + rIdx;
                Object val = row.get(c);
                if (val instanceof Number n) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"2\"><v>").append(n).append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(ref).append("\" s=\"0\" t=\"inlineStr\"><is><t>").append(esc(String.valueOf(val))).append("</t></is></c>");
                }
            }
            sb.append("</row>");
            rIdx++;
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static String tenCotExcel(int idx) {
        int n = idx + 1;
        StringBuilder s = new StringBuilder();
        while (n > 0) {
            int rem = (n - 1) % 26;
            s.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return s.toString();
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static String so(long v, boolean tien) {
        return tien ? GiaoCaService.dinhDangTien(BigDecimal.valueOf(v)) : String.valueOf(v);
    }

    static String phanTram(Double p) {
        if (p == null) return "—";
        if (p == 0) return "0%";
        return (p > 0 ? "▲ " : "▼ ") + String.valueOf(Math.abs(p)).replace('.', ',') + "%";
    }

}
