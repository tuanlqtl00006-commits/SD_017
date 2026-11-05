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
import java.util.LinkedHashSet;
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
                String.format(Locale.ROOT, "Ngày %02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear()),
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
        String nhan = String.format(Locale.ROOT, "%02d/%02d/%d - %02d/%02d/%d",
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
    @SuppressWarnings("unchecked")
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
                    x.put("hoaDonIds", new LinkedHashSet<Integer>());
                    x.put("khachHangIds", new LinkedHashSet<Integer>());
                    return x;
                });
                spItem.put("soLuong", (Long) spItem.get("soLuong") + sl);
                spItem.put("doanhThu", (Long) spItem.get("doanhThu") + (ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue()));
                if (ct.getHoaDon().getId() != null) {
                    ((Set<Integer>) spItem.get("hoaDonIds")).add(ct.getHoaDon().getId());
                }
                if (ct.getHoaDon().getKhachHang() != null && ct.getHoaDon().getKhachHang().getId() != null) {
                    ((Set<Integer>) spItem.get("khachHangIds")).add(ct.getHoaDon().getKhachHang().getId());
                }
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
        m.put("tyLeHuy", tyLeHuy(huy, soDon));
        m.put("sanPhamTrongKy", new ArrayList<>(spTrongKyMap.values()));
        return m;
    }

    /** Tỷ lệ đơn hủy trên tổng đơn phát sinh (%), làm tròn 1 chữ số. */
    static double tyLeHuy(long huy, long soDon) {
        return soDon <= 0 ? 0.0 : Math.round(huy * 1000.0 / soDon) / 10.0;
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

    /**
     * Khi lọc theo thương hiệu / sản phẩm: chỉ tính phần doanh thu của các sản phẩm khớp lọc.
     * Số đơn = số hóa đơn hoàn thành có chứa sản phẩm khớp lọc, số khách = khách của các hóa đơn đó.
     * Đơn hủy, tổng đơn phát sinh và tiền giảm giá tính theo cả hóa đơn nên không áp dụng khi lọc.
     */
    @SuppressWarnings("unchecked")
    private static void apDungLocSanPhamChoChiSo(Map<String, Object> cs) {
        List<Map<String, Object>> spKhop = (List<Map<String, Object>>) cs.getOrDefault("sanPhamTrongKy", List.of());
        long dtLoc = 0;
        long slLoc = 0;
        Set<Object> hoaDonLoc = new HashSet<>();
        Set<Object> khachLoc = new HashSet<>();
        for (Map<String, Object> sp : spKhop) {
            dtLoc += ((Number) sp.get("doanhThu")).longValue();
            slLoc += ((Number) sp.get("soLuong")).longValue();
            Object ids = sp.get("hoaDonIds");
            if (ids instanceof java.util.Collection<?> c) hoaDonLoc.addAll(c);
            Object kh = sp.get("khachHangIds");
            if (kh instanceof java.util.Collection<?> c) khachLoc.addAll(c);
        }
        int donLoc = hoaDonLoc.size();
        cs.put("doanhThuThucTe", dtLoc);
        cs.put("doanhThuDuKien", dtLoc);
        cs.put("soDon", donLoc);
        cs.put("soHoanThanh", donLoc);
        cs.put("soHuy", 0);
        cs.put("tyLeHuy", 0.0);
        cs.put("tongGiamGia", 0L);
        cs.put("giaTriTrungBinh", donLoc > 0 ? Math.round((double) dtLoc / donLoc) : 0L);
        cs.put("soSanPhamBan", slLoc);
        cs.put("soKhachMua", khachLoc.size());
        cs.put("locSanPham", true);
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
        kq.put("locSanPham", coLocSp);
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

    /** Chỉ số hiển thị trong báo cáo so sánh, cùng thứ tự với bảng trên trang So sánh. */
    record ChiSoBaoCao(String khoa, String ten, boolean tien, boolean phanTram, boolean theoHoaDon, boolean xauKhiTang) {
    }

    static final List<ChiSoBaoCao> CHI_SO_BAO_CAO = List.of(
            new ChiSoBaoCao("doanhThuThucTe", "Doanh thu", true, false, false, false),
            new ChiSoBaoCao("soHoanThanh", "Đơn hoàn thành", false, false, false, false),
            new ChiSoBaoCao("giaTriTrungBinh", "Giá trị TB / đơn", true, false, false, false),
            new ChiSoBaoCao("soSanPhamBan", "Sản phẩm bán ra", false, false, false, false),
            new ChiSoBaoCao("soKhachMua", "Khách hàng mua", false, false, false, false),
            new ChiSoBaoCao("tongGiamGia", "Tiền giảm giá", true, false, true, false),
            new ChiSoBaoCao("soDon", "Tổng đơn phát sinh", false, false, true, false),
            new ChiSoBaoCao("soHuy", "Đơn hủy", false, false, true, true),
            new ChiSoBaoCao("tyLeHuy", "Tỷ lệ hủy đơn", false, true, true, true));

    /** Các chỉ số đưa lên hàng thẻ tóm tắt đầu báo cáo. */
    static final List<String> CHI_SO_THE = List.of("doanhThuThucTe", "soHoanThanh", "giaTriTrungBinh", "soSanPhamBan", "soKhachMua", "tyLeHuy");

    static long soNguyen(Map<String, Object> m, String khoa) {
        Object v = m == null ? null : m.get(khoa);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    static double giaTriChiSo(Map<String, Object> cs, String khoa) {
        if ("tyLeHuy".equals(khoa)) {
            Object v = cs.get("tyLeHuy");
            return v instanceof Number n ? n.doubleValue() : tyLeHuy(soNguyen(cs, "soHuy"), soNguyen(cs, "soDon"));
        }
        return soNguyen(cs, khoa);
    }

    static String soThapPhan(double v) {
        double r = Math.round(v * 10.0) / 10.0;
        String s = r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
        return s.replace('.', ',');
    }

    static String hienThiChiSo(ChiSoBaoCao c, double v) {
        return c.phanTram() ? soThapPhan(v) + "%" : so(Math.round(v), c.tien());
    }

    static String chenhLechChiSo(ChiSoBaoCao c, double nay, double truoc) {
        if (c.phanTram()) {
            double r = Math.round((nay - truoc) * 10.0) / 10.0;
            return r == 0 ? "0 điểm %" : (r > 0 ? "+" : "-") + soThapPhan(Math.abs(r)) + " điểm %";
        }
        long h = Math.round(nay) - Math.round(truoc);
        return h == 0 ? so(0, c.tien()) : (h > 0 ? "+" : "-") + so(Math.abs(h), c.tien());
    }

    static String tangGiamChiSo(ChiSoBaoCao c, double nay, double truoc) {
        if (c.phanTram()) return "—";
        long a = Math.round(nay);
        long b = Math.round(truoc);
        if (b == 0) return a == 0 ? "0%" : "Mới phát sinh";
        return phanTram(tangTruong(a, b));
    }

    static String mauChiSo(ChiSoBaoCao c, double nay, double truoc) {
        double h = nay - truoc;
        if (Math.abs(h) < 1e-9 || "tongGiamGia".equals(c.khoa())) return "#475569";
        return (h > 0) != c.xauKhiTang() ? "#15803d" : "#be123c";
    }

    @SuppressWarnings("unchecked")
    private static String nhanCuaKy(Map<String, Object> cs) {
        Map<String, Object> k = (Map<String, Object>) cs.get("ky");
        return k == null ? "" : String.valueOf(k.get("nhan"));
    }

    @SuppressWarnings("unchecked")
    private static String maCuaKy(Map<String, Object> cs) {
        Map<String, Object> k = (Map<String, Object>) cs.get("ky");
        return k == null ? "" : String.valueOf(k.get("ma"));
    }

    @SuppressWarnings("unchecked")
    private static LocalDate ngayCuaKy(Map<String, Object> cs, String truong) {
        Map<String, Object> k = (Map<String, Object>) cs.get("ky");
        Object v = k == null ? null : k.get(truong);
        if (v instanceof LocalDate ld) return ld;
        try {
            return v == null ? null : LocalDate.parse(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static String ngayVn(LocalDate d) {
        return d == null ? "" : String.format(Locale.ROOT, "%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    /** Danh sách kỳ trong báo cáo theo thứ tự thời gian; kỳ cuối là kỳ đang xét. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> dsKyBaoCao(Map<String, Object> d) {
        List<Map<String, Object>> cacKy = (List<Map<String, Object>>) d.get("cacKyDoiChieu");
        if (cacKy != null && !cacKy.isEmpty()) return new ArrayList<>(cacKy);
        List<Map<String, Object>> ds = new ArrayList<>();
        if (d.get("kyTruoc") != null) ds.add((Map<String, Object>) d.get("kyTruoc"));
        ds.add((Map<String, Object>) d.get("hienTai"));
        return ds;
    }

    static List<ChiSoBaoCao> chiSoHienThi(Map<String, Object> d) {
        boolean locSp = Boolean.TRUE.equals(d.get("locSanPham"));
        return CHI_SO_BAO_CAO.stream().filter(c -> !locSp || !c.theoHoaDon()).toList();
    }

    /** Doanh thu theo thương hiệu của từng kỳ, sắp theo doanh thu kỳ đang xét giảm dần. */
    @SuppressWarnings("unchecked")
    static List<Map.Entry<String, Map<String, Long>>> doanhThuTheoThuongHieu(List<Map<String, Object>> dsKy) {
        Map<String, Map<String, Long>> theoTh = new LinkedHashMap<>();
        for (Map<String, Object> cs : dsKy) {
            String maK = maCuaKy(cs);
            for (Map<String, Object> sp : (List<Map<String, Object>>) cs.getOrDefault("sanPhamTrongKy", List.of())) {
                String th = sp.get("thuongHieu") == null ? "" : String.valueOf(sp.get("thuongHieu")).trim();
                if (th.isEmpty()) th = "Khác";
                theoTh.computeIfAbsent(th, x -> new LinkedHashMap<>()).merge(maK, soNguyen(sp, "doanhThu"), Long::sum);
            }
        }
        String maCuoi = dsKy.isEmpty() ? "" : maCuaKy(dsKy.get(dsKy.size() - 1));
        List<Map.Entry<String, Map<String, Long>>> ds = new ArrayList<>(theoTh.entrySet());
        ds.sort((x, y) -> {
            int c = Long.compare(y.getValue().getOrDefault(maCuoi, 0L), x.getValue().getOrDefault(maCuoi, 0L));
            if (c != 0) return c;
            long tx = x.getValue().values().stream().mapToLong(Long::longValue).sum();
            long ty = y.getValue().values().stream().mapToLong(Long::longValue).sum();
            return tx != ty ? Long.compare(ty, tx) : x.getKey().compareToIgnoreCase(y.getKey());
        });
        return ds;
    }

    /**
     * Tách chênh lệch doanh thu giữa hai kỳ: { tổng chênh lệch, phần do số đơn thay đổi, phần do giá trị TB / đơn thay đổi }.
     * Phần do số đơn = (số đơn kỳ này - số đơn kỳ trước) x giá trị TB / đơn kỳ trước.
     */
    static long[] tachChenhLechDoanhThu(Map<String, Object> nay, Map<String, Object> truoc) {
        long dt1 = soNguyen(truoc, "doanhThuThucTe");
        long dt2 = soNguyen(nay, "doanhThuThucTe");
        long n1 = soNguyen(truoc, "soHoanThanh");
        long n2 = soNguyen(nay, "soHoanThanh");
        long tong = dt2 - dt1;
        long doSoDon = n1 == 0 ? tong : Math.round((n2 - n1) * ((double) dt1 / n1));
        return new long[] { tong, doSoDon, tong - doSoDon };
    }

    /** Các sản phẩm có doanh thu thay đổi nhiều nhất giữa hai kỳ: mỗi phần tử { tên, chênh lệch }. */
    @SuppressWarnings("unchecked")
    static List<Object[]> sanPhamBienDong(Map<String, Object> nay, Map<String, Object> truoc, boolean tang, int gioiHan) {
        Map<String, Object[]> theoSp = new LinkedHashMap<>();
        for (int i = 0; i < 2; i++) {
            Map<String, Object> cs = i == 0 ? truoc : nay;
            for (Map<String, Object> sp : (List<Map<String, Object>>) cs.getOrDefault("sanPhamTrongKy", List.of())) {
                String khoa = String.valueOf(sp.getOrDefault("maSanPham", sp.get("tenSanPham")));
                Object[] x = theoSp.computeIfAbsent(khoa, k -> new Object[] { String.valueOf(sp.get("tenSanPham")), 0L });
                long dt = soNguyen(sp, "doanhThu");
                x[1] = (Long) x[1] + (i == 0 ? -dt : dt);
            }
        }
        return theoSp.values().stream()
                .filter(x -> tang ? (Long) x[1] > 0 : (Long) x[1] < 0)
                .sorted((x, y) -> tang ? Long.compare((Long) y[1], (Long) x[1]) : Long.compare((Long) x[1], (Long) y[1]))
                .limit(gioiHan)
                .toList();
    }

    private static String tenXuHuongSp(String ma) {
        if (ma == null) return "";
        return switch (ma.trim().toUpperCase(Locale.ROOT)) {
            case "TANG" -> "Đang tăng";
            case "GIAM" -> "Đang giảm";
            case "ON_DINH" -> "Ổn định";
            default -> ma;
        };
    }

    private static final String O_TIEU_DE = "style=\"background:#e7f1fd;border:1px solid #c9def9;padding:8px 10px;font-size:13px\"";
    private static final String O_DU_LIEU = "style=\"border:1px solid #e3eaf3;padding:8px 10px\"";

    @SuppressWarnings("unchecked")
    boolean guiThu(String den, Map<String, Object> d) throws Exception {
        List<Map<String, Object>> dsKy = dsKyBaoCao(d);
        Map<String, Object> kyChinh = dsKy.get(dsKy.size() - 1);
        Map<String, Object> kySoSanh = dsKy.size() >= 2 ? dsKy.get(dsKy.size() - 2) : null;
        String nhanChinh = nhanCuaKy(kyChinh);
        String nhanSoSanh = kySoSanh == null ? null : nhanCuaKy(kySoSanh);
        List<ChiSoBaoCao> dsChiSo = chiSoHienThi(d);
        boolean locSp = Boolean.TRUE.equals(d.get("locSanPham"));

        String tieuDeTuyChon = (String) d.get("tieuDeTuyChon");
        String tieuDe = (tieuDeTuyChon != null && !tieuDeTuyChon.isBlank())
                ? tieuDeTuyChon
                : "[FootStyle] Báo cáo so sánh doanh thu " + nhanChinh + (nhanSoSanh != null ? " - " + nhanSoSanh : "");
        String ghiChuNguoiGui = (String) d.get("ghiChuNguoiGui");
        String thLoc = (String) d.get("thuongHieuLoc");
        String kwLoc = (String) d.get("tuKhoaSpLoc");
        String xhSpLoc = (String) d.get("xuHuongSpLoc");

        String tieuDeThu = "Báo cáo so sánh doanh thu " + nhanChinh + (nhanSoSanh != null ? " so với " + nhanSoSanh : "");
        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif;color:#18242f;font-size:14px;max-width:860px\">");
        StringBuilder chu = new StringBuilder(tieuDeThu).append("\n\n");
        html.append("<h2 style=\"margin:0 0 4px\">").append(esc(tieuDeThu)).append("</h2>");
        LocalDate tuChinh = ngayCuaKy(kyChinh, "tuNgay");
        LocalDate denChinh = ngayCuaKy(kyChinh, "denNgay");
        html.append("<p style=\"color:#5b6b80;margin:0 0 12px\">").append(esc(nhanChinh)).append(": ")
                .append(ngayVn(tuChinh)).append(" - ").append(ngayVn(denChinh))
                .append(". Doanh thu tính trên đơn đã hoàn thành, không gồm phí giao hàng.</p>");

        LocalDate homNay = LocalDate.now(dongHo);
        if (tuChinh != null && denChinh != null && !homNay.isBefore(tuChinh) && homNay.isBefore(denChinh)) {
            long daQua = java.time.temporal.ChronoUnit.DAYS.between(tuChinh, homNay) + 1;
            long tongNgay = java.time.temporal.ChronoUnit.DAYS.between(tuChinh, denChinh) + 1;
            String luuY = nhanChinh + " chưa kết thúc (đã qua " + daQua + "/" + tongNgay + " ngày), số liệu tính đến ngày " + ngayVn(homNay) + ".";
            html.append("<p style=\"background:#fff7ed;border:1px solid #fed7aa;padding:8px 12px;border-radius:6px;margin:0 0 12px;color:#9a3412;font-size:13px\">")
                    .append(esc(luuY)).append("</p>");
            chu.append(luuY).append("\n\n");
        }

        List<String> moTaLoc = new ArrayList<>();
        if (dsKy.size() >= 2) {
            List<String> tenCacKy = new ArrayList<>();
            for (Map<String, Object> item : dsKy) tenCacKy.add(nhanCuaKy(item));
            moTaLoc.add("Các kỳ đối chiếu: " + String.join(", ", tenCacKy));
        }
        if (thLoc != null && !thLoc.isBlank()) moTaLoc.add("Thương hiệu: " + thLoc);
        if (kwLoc != null && !kwLoc.isBlank()) moTaLoc.add("Từ khóa SP: " + kwLoc);
        if (xhSpLoc != null && !xhSpLoc.isBlank()) moTaLoc.add("Xu hướng SP: " + tenXuHuongSp(xhSpLoc));
        if (!moTaLoc.isEmpty()) {
            html.append("<p style=\"background:#f8fafc;border:1px solid #e2e8f0;padding:8px 12px;border-radius:6px;margin:0 0 14px;color:#334155;font-size:13px\"><b>Bộ lọc áp dụng:</b> ")
                    .append(esc(String.join(" | ", moTaLoc))).append("</p>");
            chu.append("Bộ lọc áp dụng: ").append(String.join(" | ", moTaLoc)).append("\n\n");
        }
        if (ghiChuNguoiGui != null && !ghiChuNguoiGui.isBlank()) {
            html.append("<div style=\"background:#fffbeb;border:1px solid #fde68a;border-left:4px solid #d97706;padding:12px 16px;border-radius:8px;margin-bottom:14px\">")
                    .append("<b style=\"color:#b45309;display:block;margin-bottom:4px\">Ghi chú từ người gửi:</b>")
                    .append("<div style=\"line-height:1.5;color:#334155\">").append(esc(ghiChuNguoiGui)).append("</div></div>");
            chu.append("Ghi chú từ người gửi:\n").append(ghiChuNguoiGui).append("\n\n");
        }

        // Thẻ tóm tắt: giá trị kỳ đang xét và mức tăng / giảm so với kỳ liền trước
        html.append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%;margin-bottom:16px\"><tr>");
        for (ChiSoBaoCao c : dsChiSo) {
            if (!CHI_SO_THE.contains(c.khoa())) continue;
            double vNay = giaTriChiSo(kyChinh, c.khoa());
            html.append("<td style=\"border:1px solid #e3eaf3;padding:10px;vertical-align:top;background:#f8fafc\">")
                    .append("<div style=\"color:#475569;font-size:12px\">").append(esc(c.ten())).append("</div>")
                    .append("<div style=\"font-size:17px;font-weight:bold;color:#0f172a;margin:2px 0\">").append(hienThiChiSo(c, vNay)).append("</div>");
            if (kySoSanh != null) {
                double vTruoc = giaTriChiSo(kySoSanh, c.khoa());
                String bienDong = c.phanTram() ? chenhLechChiSo(c, vNay, vTruoc) : tangGiamChiSo(c, vNay, vTruoc);
                html.append("<div style=\"font-size:12px\"><b style=\"color:").append(mauChiSo(c, vNay, vTruoc)).append("\">")
                        .append(esc(bienDong)).append("</b> <span style=\"color:#64748b\">so với ").append(esc(nhanSoSanh)).append("</span></div>");
            }
            html.append("</td>");
        }
        html.append("</tr></table>");

        // I. Bảng đối chiếu chỉ số
        html.append("<h3 style=\"margin:18px 0 6px\">I. Bảng đối chiếu chỉ số</h3>")
                .append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\"><tr>")
                .append("<th align=\"left\" ").append(O_TIEU_DE).append(">Chỉ số</th>");
        for (Map<String, Object> item : dsKy) {
            html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">").append(esc(nhanCuaKy(item))).append("</th>");
        }
        if (kySoSanh != null) {
            html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">Chênh lệch</th>")
                    .append("<th align=\"right\" ").append(O_TIEU_DE).append(">Tăng / giảm</th>");
        }
        html.append("</tr>");
        for (ChiSoBaoCao c : dsChiSo) {
            html.append("<tr><td ").append(O_DU_LIEU).append("><b>").append(esc(c.ten())).append("</b></td>");
            for (int i = 0; i < dsKy.size(); i++) {
                double v = giaTriChiSo(dsKy.get(i), c.khoa());
                html.append("<td align=\"right\" ").append(O_DU_LIEU).append(">").append(hienThiChiSo(c, v));
                if (dsKy.size() > 2 && i > 0) {
                    double vt = giaTriChiSo(dsKy.get(i - 1), c.khoa());
                    String bd = c.phanTram() ? chenhLechChiSo(c, v, vt) : tangGiamChiSo(c, v, vt);
                    html.append("<div style=\"font-size:11px;color:").append(mauChiSo(c, v, vt)).append("\">").append(esc(bd)).append("</div>");
                }
                html.append("</td>");
            }
            double vNay = giaTriChiSo(kyChinh, c.khoa());
            chu.append("- ").append(c.ten()).append(": ").append(hienThiChiSo(c, vNay));
            if (kySoSanh != null) {
                double vTruoc = giaTriChiSo(kySoSanh, c.khoa());
                String mau = mauChiSo(c, vNay, vTruoc);
                html.append("<td align=\"right\" ").append(O_DU_LIEU).append("><b style=\"color:").append(mau).append("\">")
                        .append(esc(chenhLechChiSo(c, vNay, vTruoc))).append("</b></td>")
                        .append("<td align=\"right\" ").append(O_DU_LIEU).append("><b style=\"color:").append(mau).append("\">")
                        .append(esc(tangGiamChiSo(c, vNay, vTruoc))).append("</b></td>");
                chu.append(" (").append(nhanSoSanh).append(": ").append(hienThiChiSo(c, vTruoc)).append(", ")
                        .append(c.phanTram() ? chenhLechChiSo(c, vNay, vTruoc) : tangGiamChiSo(c, vNay, vTruoc)).append(")");
            }
            chu.append("\n");
            html.append("</tr>");
        }
        html.append("</table>");
        if (locSp) {
            html.append("<p style=\"color:#64748b;font-size:12px;margin:6px 0 0\">Đang lọc theo sản phẩm nên chỉ tính phần doanh thu của sản phẩm khớp lọc; số đơn là số đơn hoàn thành có chứa các sản phẩm đó.</p>");
        }

        // Phân tích chênh lệch doanh thu giữa kỳ đang xét và kỳ liền trước
        if (kySoSanh != null) {
            long[] tach = tachChenhLechDoanhThu(kyChinh, kySoSanh);
            String moDau = tach[0] == 0
                    ? "Doanh thu " + nhanChinh + " không đổi so với " + nhanSoSanh + "."
                    : "Doanh thu " + nhanChinh + (tach[0] > 0 ? " tăng " : " giảm ") + so(Math.abs(tach[0]), true) + " so với " + nhanSoSanh + ".";
            html.append("<h3 style=\"margin:18px 0 6px\">Doanh thu thay đổi do đâu</h3>")
                    .append("<p style=\"margin:0 0 6px\"><b>").append(esc(moDau)).append("</b></p>")
                    .append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\">");
            long n1 = soNguyen(kySoSanh, "soHoanThanh");
            long n2 = soNguyen(kyChinh, "soHoanThanh");
            long g1 = soNguyen(kySoSanh, "giaTriTrungBinh");
            long g2 = soNguyen(kyChinh, "giaTriTrungBinh");
            html.append("<tr><td ").append(O_DU_LIEU).append(">Số đơn hoàn thành: ").append(n1).append(" → ").append(n2).append(" đơn</td>")
                    .append("<td align=\"right\" ").append(O_DU_LIEU).append("><b style=\"color:").append(tach[1] >= 0 ? "#15803d" : "#be123c").append("\">")
                    .append(tach[1] > 0 ? "+" : tach[1] < 0 ? "-" : "").append(so(Math.abs(tach[1]), true)).append("</b></td></tr>");
            html.append("<tr><td ").append(O_DU_LIEU).append(">Giá trị TB / đơn: ").append(so(g1, true)).append(" → ").append(so(g2, true)).append("</td>")
                    .append("<td align=\"right\" ").append(O_DU_LIEU).append("><b style=\"color:").append(tach[2] >= 0 ? "#15803d" : "#be123c").append("\">")
                    .append(tach[2] > 0 ? "+" : tach[2] < 0 ? "-" : "").append(so(Math.abs(tach[2]), true)).append("</b></td></tr>");
            html.append("</table>");
            chu.append("\n").append(moDau).append("\n");
            List<Object[]> spTang = sanPhamBienDong(kyChinh, kySoSanh, true, 3);
            List<Object[]> spGiam = sanPhamBienDong(kyChinh, kySoSanh, false, 3);
            if (!spTang.isEmpty() || !spGiam.isEmpty()) {
                html.append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%;margin-top:8px\"><tr>")
                        .append("<th align=\"left\" ").append(O_TIEU_DE).append(">Sản phẩm tăng doanh thu nhiều nhất</th>")
                        .append("<th align=\"left\" ").append(O_TIEU_DE).append(">Sản phẩm giảm doanh thu nhiều nhất</th></tr><tr>");
                for (List<Object[]> ds : List.of(spTang, spGiam)) {
                    html.append("<td valign=\"top\" ").append(O_DU_LIEU).append(">");
                    if (ds.isEmpty()) html.append("<span style=\"color:#94a3b8\">Không có</span>");
                    for (Object[] x : ds) {
                        long h = (Long) x[1];
                        html.append("<div style=\"margin:2px 0\">").append(esc(String.valueOf(x[0]))).append(": <b style=\"color:")
                                .append(h > 0 ? "#15803d" : "#be123c").append("\">").append(h > 0 ? "+" : "-").append(so(Math.abs(h), true)).append("</b></div>");
                    }
                    html.append("</td>");
                }
                html.append("</tr></table>");
            }
        }

        html.append("<p style=\"margin:18px 0 6px\"><img src=\"cid:bieu-do-xu-huong\" alt=\"Biểu đồ doanh thu các kỳ\" style=\"max-width:100%\"></p>");

        // II. Doanh thu theo thương hiệu
        List<Map.Entry<String, Map<String, Long>>> dsTh = doanhThuTheoThuongHieu(dsKy);
        if (!dsTh.isEmpty()) {
            String maChinh = maCuaKy(kyChinh);
            String maSoSanh = kySoSanh == null ? null : maCuaKy(kySoSanh);
            long tongChinh = dsTh.stream().mapToLong(e -> e.getValue().getOrDefault(maChinh, 0L)).sum();
            html.append("<h3 style=\"margin:18px 0 6px\">II. Doanh thu theo thương hiệu</h3>")
                    .append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\"><tr>")
                    .append("<th align=\"left\" ").append(O_TIEU_DE).append(">Thương hiệu</th>");
            for (Map<String, Object> item : dsKy) {
                html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">").append(esc(nhanCuaKy(item))).append("</th>");
            }
            if (maSoSanh != null) {
                html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">Chênh lệch</th>")
                        .append("<th align=\"right\" ").append(O_TIEU_DE).append(">Tăng / giảm</th>");
            }
            html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">Tỷ trọng ").append(esc(nhanChinh)).append("</th></tr>");
            ChiSoBaoCao cDt = CHI_SO_BAO_CAO.get(0);
            for (Map.Entry<String, Map<String, Long>> e : dsTh) {
                html.append("<tr><td ").append(O_DU_LIEU).append("><b>").append(esc(e.getKey())).append("</b></td>");
                for (Map<String, Object> item : dsKy) {
                    html.append("<td align=\"right\" ").append(O_DU_LIEU).append(">").append(so(e.getValue().getOrDefault(maCuaKy(item), 0L), true)).append("</td>");
                }
                long vNay = e.getValue().getOrDefault(maChinh, 0L);
                if (maSoSanh != null) {
                    long vTruoc = e.getValue().getOrDefault(maSoSanh, 0L);
                    String mau = mauChiSo(cDt, vNay, vTruoc);
                    html.append("<td align=\"right\" ").append(O_DU_LIEU).append("><b style=\"color:").append(mau).append("\">")
                            .append(esc(chenhLechChiSo(cDt, vNay, vTruoc))).append("</b></td>")
                            .append("<td align=\"right\" ").append(O_DU_LIEU).append("><b style=\"color:").append(mau).append("\">")
                            .append(esc(tangGiamChiSo(cDt, vNay, vTruoc))).append("</b></td>");
                }
                html.append("<td align=\"right\" ").append(O_DU_LIEU).append(">")
                        .append(tongChinh > 0 ? soThapPhan(vNay * 100.0 / tongChinh) + "%" : "0%").append("</td></tr>");
            }
            html.append("</table>");
        }

        // III. Sản phẩm bán chạy qua các kỳ
        List<Map<String, Object>> sps = (List<Map<String, Object>>) d.getOrDefault("sanPham", List.of());
        if (!sps.isEmpty()) {
            html.append("<h3 style=\"margin:18px 0 6px\">III. Sản phẩm bán chạy qua các kỳ (số lượng)</h3>")
                    .append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\"><tr>")
                    .append("<th align=\"center\" ").append(O_TIEU_DE).append(">#</th>")
                    .append("<th align=\"left\" ").append(O_TIEU_DE).append(">Sản phẩm</th>")
                    .append("<th align=\"left\" ").append(O_TIEU_DE).append(">Thương hiệu</th>");
            for (Map<String, Object> item : dsKy) {
                html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">").append(esc(nhanCuaKy(item))).append("</th>");
            }
            html.append("<th align=\"right\" ").append(O_TIEU_DE).append(">Tổng bán</th>")
                    .append("<th align=\"right\" ").append(O_TIEU_DE).append(">Doanh thu</th>")
                    .append("<th align=\"right\" ").append(O_TIEU_DE).append(">Xu hướng</th></tr>");
            int stt = 1;
            for (Map<String, Object> sp : sps) {
                Map<String, Long> theoKy = (Map<String, Long>) sp.get("theoKy");
                html.append("<tr><td align=\"center\" ").append(O_DU_LIEU).append(">").append(stt++).append("</td>")
                        .append("<td ").append(O_DU_LIEU).append("><b>").append(esc(String.valueOf(sp.get("tenSanPham")))).append("</b>")
                        .append("<div style=\"color:#64748b;font-size:12px\">").append(esc(String.valueOf(sp.getOrDefault("maSanPham", "")))).append("</div></td>")
                        .append("<td ").append(O_DU_LIEU).append(">").append(esc(String.valueOf(sp.getOrDefault("thuongHieu", "")))).append("</td>");
                for (Map<String, Object> item : dsKy) {
                    long sl = theoKy != null ? theoKy.getOrDefault(maCuaKy(item), 0L) : 0L;
                    html.append("<td align=\"right\" ").append(O_DU_LIEU).append(">").append(sl).append("</td>");
                }
                long tongBan = sp.get("tongBan") instanceof Number n ? n.longValue() : soNguyen(sp, "soLuong");
                long tongDt = sp.get("tongDoanhThu") instanceof Number n ? n.longValue() : soNguyen(sp, "doanhThu");
                String xh = sp.get("nhanXuHuong") != null ? String.valueOf(sp.get("nhanXuHuong")) : phanTram((Double) sp.get("tangTruong"));
                html.append("<td align=\"right\" ").append(O_DU_LIEU).append("><b>").append(tongBan).append("</b></td>")
                        .append("<td align=\"right\" ").append(O_DU_LIEU).append(">").append(so(tongDt, true)).append("</td>")
                        .append("<td align=\"right\" ").append(O_DU_LIEU).append(">").append(esc(xh)).append("</td></tr>");
            }
            html.append("</table>");
        }
        html.append("<p style=\"color:#7b8794;margin-top:20px;font-size:12px\">Thư gửi từ hệ thống quản lý FootStyle, kèm tệp Excel chi tiết.</p></div>");

        List<BieuDoPng.Cot> cot = new ArrayList<>();
        for (Map<String, Object> item : dsKy) {
            cot.add(new BieuDoPng.Cot(nhanCuaKy(item), soNguyen(item, "doanhThuThucTe"), null));
        }
        Map<String, byte[]> anh = new LinkedHashMap<>();
        anh.put("bieu-do-xu-huong", BieuDoPng.cotNhom("Doanh thu các kỳ so sánh", "Doanh thu", null, cot));

        String maKy = maCuaKy(kyChinh).replaceAll("[^A-Za-z0-9_-]", "_");
        String tenFileXlsx = "Bao-cao-so-sanh_" + String.valueOf(d.get("kieu")).toLowerCase(Locale.ROOT) + "_" + maKy + ".xlsx";
        byte[] fileXlsx = taoFileExcelBaoCao(d);
        return mailService.guiBaoCao(den, tieuDe, chu.toString(), html.toString(), anh, tenFileXlsx, fileXlsx);
    }

    /** Tạo tệp Excel (.xlsx) đính kèm email báo cáo so sánh: chỉ số các kỳ, thương hiệu, sản phẩm. */
    @SuppressWarnings("unchecked")
    static byte[] taoFileExcelBaoCao(Map<String, Object> d) throws Exception {
        List<Map<String, Object>> dsKy = dsKyBaoCao(d);
        Map<String, Object> kyChinh = dsKy.get(dsKy.size() - 1);
        Map<String, Object> kySoSanh = dsKy.size() >= 2 ? dsKy.get(dsKy.size() - 2) : null;
        String nhanChinh = nhanCuaKy(kyChinh);
        String thLoc = (String) d.get("thuongHieuLoc");
        String kwLoc = (String) d.get("tuKhoaSpLoc");
        List<String> tenCacKy = new ArrayList<>();
        for (Map<String, Object> item : dsKy) tenCacKy.add(nhanCuaKy(item));
        String phuDe = (kySoSanh != null ? "So sánh " + nhanChinh + " với " + nhanCuaKy(kySoSanh) : nhanChinh)
                + " | Các kỳ: " + String.join(", ", tenCacKy)
                + (thLoc != null && !thLoc.isBlank() ? " | Thương hiệu: " + thLoc : "")
                + (kwLoc != null && !kwLoc.isBlank() ? " | Từ khóa SP: " + kwLoc : "");

        // Sheet 1: Chỉ số các kỳ
        List<String> h1 = new ArrayList<>(List.of("STT", "Chỉ số"));
        h1.addAll(tenCacKy);
        if (kySoSanh != null) h1.addAll(List.of("Chênh lệch", "Tăng / giảm"));
        List<List<Object>> r1 = new ArrayList<>();
        int stt = 1;
        for (ChiSoBaoCao c : chiSoHienThi(d)) {
            List<Object> row = new ArrayList<>(List.of(stt++, c.ten()));
            for (Map<String, Object> item : dsKy) {
                double v = giaTriChiSo(item, c.khoa());
                row.add(c.phanTram() ? (Object) (soThapPhan(v) + "%") : (Object) Math.round(v));
            }
            if (kySoSanh != null) {
                double vNay = giaTriChiSo(kyChinh, c.khoa());
                double vTruoc = giaTriChiSo(kySoSanh, c.khoa());
                row.add(c.phanTram() ? (Object) chenhLechChiSo(c, vNay, vTruoc) : (Object) (Math.round(vNay) - Math.round(vTruoc)));
                row.add(tangGiamChiSo(c, vNay, vTruoc));
            }
            r1.add(row);
        }

        // Sheet 2: Doanh thu theo thương hiệu
        List<String> h2 = new ArrayList<>(List.of("STT", "Thương hiệu"));
        h2.addAll(tenCacKy);
        if (kySoSanh != null) h2.addAll(List.of("Chênh lệch", "Tăng / giảm"));
        h2.add("Tỷ trọng " + nhanChinh);
        List<List<Object>> r2 = new ArrayList<>();
        List<Map.Entry<String, Map<String, Long>>> dsTh = doanhThuTheoThuongHieu(dsKy);
        String maChinh = maCuaKy(kyChinh);
        long tongChinh = dsTh.stream().mapToLong(e -> e.getValue().getOrDefault(maChinh, 0L)).sum();
        ChiSoBaoCao cDt = CHI_SO_BAO_CAO.get(0);
        int sttTh = 1;
        for (Map.Entry<String, Map<String, Long>> e : dsTh) {
            List<Object> row = new ArrayList<>(List.of(sttTh++, e.getKey()));
            for (Map<String, Object> item : dsKy) row.add(e.getValue().getOrDefault(maCuaKy(item), 0L));
            long vNay = e.getValue().getOrDefault(maChinh, 0L);
            if (kySoSanh != null) {
                long vTruoc = e.getValue().getOrDefault(maCuaKy(kySoSanh), 0L);
                row.add(vNay - vTruoc);
                row.add(tangGiamChiSo(cDt, vNay, vTruoc));
            }
            row.add(tongChinh > 0 ? soThapPhan(vNay * 100.0 / tongChinh) + "%" : "0%");
            r2.add(row);
        }

        // Sheet 3: Sản phẩm qua các kỳ
        List<String> h3 = new ArrayList<>(List.of("STT", "Mã SP", "Tên sản phẩm", "Thương hiệu"));
        h3.addAll(tenCacKy);
        h3.addAll(List.of("Tổng bán", "Doanh thu (₫)", "Xu hướng"));
        List<List<Object>> r3 = new ArrayList<>();
        int sttSp = 1;
        for (Map<String, Object> sp : (List<Map<String, Object>>) d.getOrDefault("sanPham", List.of())) {
            Map<String, Long> theoKy = (Map<String, Long>) sp.get("theoKy");
            List<Object> row = new ArrayList<>(List.of(
                    sttSp++,
                    String.valueOf(sp.getOrDefault("maSanPham", "")),
                    String.valueOf(sp.get("tenSanPham")),
                    String.valueOf(sp.getOrDefault("thuongHieu", ""))));
            for (Map<String, Object> item : dsKy) row.add(theoKy != null ? theoKy.getOrDefault(maCuaKy(item), 0L) : 0L);
            row.add(sp.get("tongBan") instanceof Number n ? n.longValue() : soNguyen(sp, "soLuong"));
            row.add(sp.get("tongDoanhThu") instanceof Number n ? n.longValue() : soNguyen(sp, "doanhThu"));
            row.add(sp.get("nhanXuHuong") != null ? String.valueOf(sp.get("nhanXuHuong")) : phanTram((Double) sp.get("tangTruong")));
            r3.add(row);
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
                        + "<sheet name=\"Chỉ số các kỳ\" sheetId=\"1\" r:id=\"rId1\"/>"
                        + "<sheet name=\"Thương hiệu\" sheetId=\"2\" r:id=\"rId2\"/>"
                        + "<sheet name=\"Sản phẩm\" sheetId=\"3\" r:id=\"rId3\"/>"
                        + "</sheets></workbook>");
        files.put("xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                        + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>"
                        + "<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
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
        files.put("xl/worksheets/sheet1.xml", taoSheetXml("I. BẢNG ĐỐI CHIẾU CHỈ SỐ CÁC KỲ", phuDe, h1, r1));
        files.put("xl/worksheets/sheet2.xml", taoSheetXml("II. DOANH THU THEO THƯƠNG HIỆU (₫)", phuDe, h2, r2));
        files.put("xl/worksheets/sheet3.xml", taoSheetXml("III. SẢN PHẨM BÁN CHẠY QUA CÁC KỲ", phuDe, h3, r3));

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

    /** Chỉ thoát các ký tự đặc biệt của HTML, giữ nguyên chữ tiếng Việt để dùng được cho cả nội dung thư lẫn XML của tệp Excel. */
    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s, "UTF-8");
    }

    private static String so(long v, boolean tien) {
        return tien ? GiaoCaService.dinhDangTien(BigDecimal.valueOf(v)) : String.valueOf(v);
    }

    static String phanTram(Double p) {
        if (p == null) return "—";
        if (p == 0) return "0%";
        return (p > 0 ? "▲ " : "▼ ") + soThapPhan(Math.abs(p)) + "%";
    }

}
