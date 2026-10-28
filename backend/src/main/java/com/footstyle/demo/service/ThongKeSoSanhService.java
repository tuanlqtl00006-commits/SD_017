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
import java.math.BigDecimal;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * Thống kê SO SÁNH (cô: nút "So sánh" - so theo tháng / quý / năm, so với kỳ trước và CÙNG KỲ năm trước, biểu đồ tăng trưởng,
 * tải Excel và gửi email báo cáo kèm biểu đồ; email tự động gửi báo cáo tháng cho quản lý).
 *  - Doanh thu THỰC TẾ: đơn "Đã hoàn thành". Doanh thu DỰ KIẾN: mọi đơn chưa hủy / chưa hoàn tiền (gồm đơn đang xử lý, đang giao).
 *    Cả hai đều KHÔNG tính phí ship (DoanhThu.cua). Hóa đơn chờ tại quầy (chưa thanh toán) không tính.
 *  - kỳ: tháng "2026-10", quý "2026-Q4", năm "2026"; bỏ trống = kỳ hiện tại.
 */
@Service
public class ThongKeSoSanhService {

    private static final Logger log = LoggerFactory.getLogger(ThongKeSoSanhService.class);
    private static final Pattern KY_THANG = Pattern.compile("(\\d{4})-(\\d{1,2})");
    private static final Pattern KY_QUY = Pattern.compile("(\\d{4})-[Qq]([1-4])");
    private static final Pattern KY_NAM = Pattern.compile("(\\d{4})");
    static final int SO_NAM_XU_HUONG = 5;

    public enum Kieu { THANG, QUY, NAM }

    /** Một kỳ thống kê: nhãn hiển thị + khoảng ngày (tính cả 2 đầu). */
    public record Ky(String ma, String nhan, LocalDate tu, LocalDate den) {
    }

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final NhanVienRepository nhanVienRepo;
    private final MailService mailService;
    private Clock dongHo = Clock.systemDefaultZone();

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
            throw ApiException.badRequest("Kiểu so sánh phải là THANG, QUY hoặc NAM.");
        }
    }

    Ky docKy(Kieu kieu, String ky) {
        LocalDate homNay = LocalDate.now(dongHo);
        String s = ky == null ? "" : ky.trim();
        switch (kieu) {
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

    static Ky kyTruoc(Kieu kieu, Ky k) {
        return switch (kieu) {
            case THANG -> thang(YearMonth.from(k.tu()).minusMonths(1));
            case QUY -> {
                LocalDate t = k.tu().minusMonths(3);
                yield quy(t.getYear(), (t.getMonthValue() - 1) / 3 + 1);
            }
            case NAM -> nam(k.tu().getYear() - 1);
        };
    }

    static Ky cungKyNamTruoc(Kieu kieu, Ky k) {
        return switch (kieu) {
            case THANG -> thang(YearMonth.from(k.tu()).minusYears(1));
            case QUY -> quy(k.tu().getYear() - 1, (k.tu().getMonthValue() - 1) / 3 + 1);
            case NAM -> nam(k.tu().getYear() - 1);
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
        for (HoaDonChiTiet ct : chiTietHoanThanh) {
            if (ct.getHoaDon() == null || ct.getHoaDon().getNgayTao() == null || !trongKy(ct.getHoaDon(), k)) continue;
            sanPham += ct.getSoLuong() == null ? 0 : ct.getSoLuong();
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
        kq.put("phanTich", taoPhanTich(kieu, kq));
        return kq;
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
            case THANG -> {
                for (int t = 1; t <= 12; t++) cap.add(new Ky[] {thang(YearMonth.of(nam, t)), thang(YearMonth.of(nam - 1, t))});
            }
            case QUY -> {
                for (int q = 1; q <= 4; q++) cap.add(new Ky[] {quy(nam, q), quy(nam - 1, q)});
            }
            case NAM -> {
                for (int y = nam - (SO_NAM_XU_HUONG - 1); y <= nam; y++) cap.add(new Ky[] {nam(y), null});
            }
        }
        List<Map<String, Object>> ds = new ArrayList<>();
        Long truoc = null;
        for (Ky[] c : cap) {
            long a = doanhThu(c[0], hoaDons);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nhan", kieu == Kieu.THANG ? "T" + c[0].tu().getMonthValue() : kieu == Kieu.QUY ? "Q" + ((c[0].tu().getMonthValue() - 1) / 3 + 1)
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
        if (kieu == Kieu.THANG) {
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
            Map<String, Object> m = theoSp.computeIfAbsent(sp.getId(), k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("maSanPham", sp.getMaSanPham());
                x.put("tenSanPham", sp.getTenSanPham());
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
        String den = email == null ? "" : email.trim();
        if (!den.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) throw ApiException.badRequest("Email nhận báo cáo không hợp lệ.");
        Map<String, Object> d = soSanh(kieu, ky);
        boolean that;
        try {
            that = guiThu(den, d);
        } catch (Exception e) {
            throw ApiException.conflict("Không gửi được email: " + e.getMessage());
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("email", den);
        kq.put("daGui", that);
        kq.put("giaLap", !that);
        return kq;
    }

    /** Email TỰ ĐỘNG HẰNG NGÀY: 21:00 mỗi tối gửi báo cáo doanh thu lũy kế trong tháng & ngày hiện tại cho quản lý. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao-ngay:0 0 21 * * *}")
    public void baoCaoNgayTuDong() {
        String ky = YearMonth.from(LocalDate.now(dongHo)).toString();
        guiBaoCaoChoQuanLy(ky, soSanh("THANG", ky));
    }

    /** Email TỰ ĐỘNG CUỐI THÁNG: 22:30 ngày cuối tháng gửi báo cáo tổng kết tháng hiện tại cho quản lý. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao-cuoi-thang:0 30 22 28-31 * *}")
    public void baoCaoCuoiThangTuDong() {
        LocalDate homNay = LocalDate.now(dongHo);
        if (homNay.getDayOfMonth() != homNay.lengthOfMonth()) return;
        String ky = YearMonth.from(homNay).toString();
        guiBaoCaoChoQuanLy(ky, soSanh("THANG", ky));
    }

    /** Email TỰ ĐỘNG ĐẦU THÁNG: 8 giờ sáng ngày 1 hằng tháng gửi báo cáo chốt tháng trước cho mọi quản lý đang hoạt động có email. */
    @Scheduled(cron = "${app.thong-ke.cron-bao-cao:0 0 8 1 * *}")
    public void baoCaoThangTuDong() {
        String ky = YearMonth.from(LocalDate.now(dongHo)).minusMonths(1).toString();
        guiBaoCaoChoQuanLy(ky, soSanh("THANG", ky));
    }

    private void guiBaoCaoChoQuanLy(String ky, Map<String, Object> d) {
        for (NhanVien nv : nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.HOAT_DONG)) {
            boolean ql = nv.getVaiTro() != null && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(String.valueOf(nv.getVaiTro().getTenVaiTro()).trim());
            if (!ql || nv.getEmail() == null || !nv.getEmail().contains("@")) continue;
            try {
                guiThu(nv.getEmail().trim(), d);
            } catch (Exception e) {
                log.error("Không gửi được báo cáo {} tới {}: {}", ky, nv.getEmail(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    boolean guiThu(String den, Map<String, Object> d) throws Exception {
        Map<String, Object> ky = (Map<String, Object>) d.get("ky");
        Map<String, Object> a = (Map<String, Object>) d.get("hienTai");
        Map<String, Object> kt = (Map<String, Object>) d.get("kyTruoc");
        Map<String, Object> ck = (Map<String, Object>) d.get("cungKyNamTruoc");
        List<String> phanTich = (List<String>) d.get("phanTich");
        String nhanKy = String.valueOf(ky.get("nhan"));
        String tieuDe = "[FootStyle] Báo cáo phân tích & so sánh doanh thu " + nhanKy;

        List<BieuDoPng.Cot> xu = new ArrayList<>();
        for (Map<String, Object> p : (List<Map<String, Object>>) d.get("xuHuong")) {
            Object b = p.get("doanhThuNamTruoc");
            xu.add(new BieuDoPng.Cot(String.valueOf(p.get("nhan")), ((Number) p.get("doanhThu")).longValue(), b == null ? null : ((Number) b).longValue()));
        }
        boolean coNamTruoc = !"NAM".equals(d.get("kieu"));
        byte[] anhXu = BieuDoPng.cotNhom("Doanh thu thực tế theo " + ("THANG".equals(d.get("kieu")) ? "tháng" : "QUY".equals(d.get("kieu")) ? "quý" : "năm"),
                coNamTruoc ? "Năm nay" : "Doanh thu", coNamTruoc ? "Cùng kỳ năm trước" : null, xu);
        List<BieuDoPng.Cot> ct = new ArrayList<>();
        for (Map<String, Object> p : (List<Map<String, Object>>) d.get("chiTietKy")) {
            ct.add(new BieuDoPng.Cot(String.valueOf(p.get("nhan")), ((Number) p.get("doanhThu")).longValue(), ((Number) p.get("doanhThuNamTruoc")).longValue()));
        }
        byte[] anhCt = BieuDoPng.cotNhom("Chi tiết " + nhanKy, nhanKy, "Cùng kỳ năm trước", ct);

        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif;color:#18242f;font-size:14px\">");
        html.append("<h2 style=\"margin:0 0 4px\">Báo cáo phân tích &amp; so sánh ").append(esc(nhanKy)).append("</h2>");
        html.append("<p style=\"color:#5b6b80;margin:0 0 16px\">Từ ").append(ky.get("tuNgay")).append(" đến ").append(ky.get("denNgay"))
                .append(". Doanh thu không gồm phí giao hàng; thực tế = đơn đã hoàn thành, dự kiến = mọi đơn chưa hủy.</p>");

        StringBuilder chu = new StringBuilder("Báo cáo phân tích & so sánh " + nhanKy + "\n\n");
        if (phanTich != null && !phanTich.isEmpty()) {
            html.append("<div style=\"background:#f8fafc;border:1px solid #dbeafe;border-left:4px solid #0977ec;padding:12px 16px;border-radius:8px;margin-bottom:16px\">")
                    .append("<b style=\"color:#0977ec;display:block;margin-bottom:6px\">Nhận xét &amp; Phân tích tổng hợp:</b>")
                    .append("<ul style=\"margin:0;padding-left:18px;line-height:1.55\">");
            chu.append("NHẬN XÉT & PHÂN TÍCH:\n");
            for (String dong : phanTich) {
                html.append("<li>").append(esc(dong)).append("</li>");
                chu.append("* ").append(dong).append("\n");
            }
            html.append("</ul></div>");
            chu.append("\n");
        }

        html.append("<table cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e3eaf3\">")
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
            html.append("<h3 style=\"margin:20px 0 6px\">Sản phẩm bán chạy</h3><table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e3eaf3\">")
                    .append("<tr style=\"background:#f3f6fb\"><th align=\"left\">Sản phẩm</th><th align=\"right\">Số lượng</th><th align=\"right\">Kỳ trước</th><th align=\"right\">±</th></tr>");
            for (Map<String, Object> sp : sps) {
                html.append("<tr style=\"border-top:1px solid #e3eaf3\"><td>").append(esc(String.valueOf(sp.get("tenSanPham")))).append("</td><td align=\"right\">")
                        .append(sp.get("soLuong")).append("</td><td align=\"right\">").append(sp.get("soLuongKyTruoc")).append("</td><td align=\"right\">")
                        .append(phanTram((Double) sp.get("tangTruong"))).append("</td></tr>");
            }
            html.append("</table>");
        }
        html.append("<p style=\"color:#7b8794;margin-top:20px\">Thư gửi từ hệ thống FootStyle (tự động gửi định kỳ hằng ngày lúc 21:00 và cuối mỗi tháng). Xem chi tiết tại trang Thống kê &gt; So sánh.</p></div>");
        Map<String, byte[]> anh = new LinkedHashMap<>();
        anh.put("bieu-do-xu-huong", anhXu);
        anh.put("bieu-do-chi-tiet", anhCt);
        return mailService.guiBaoCao(den, tieuDe, chu.toString(), html.toString(), anh);
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
