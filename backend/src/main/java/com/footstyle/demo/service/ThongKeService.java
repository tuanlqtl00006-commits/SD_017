package com.footstyle.demo.service;

import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhuongThucThanhToan;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.CaLamViecRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.GiaoCaRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.LichLamViecRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.PhuongThucThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.repository.SanPhamRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thống kê giai đoạn 3 (chỉ quản lý): sản phẩm bán chạy, sắp hết hàng, doanh thu theo phương thức thanh toán / khung giờ /
 * ca làm việc, chuyên cần theo lịch làm việc (đúng giờ, đi muộn, vắng) và các ca giao ca lệch tiền.
 * Doanh thu chỉ tính đơn ĐÃ HOÀN THÀNH, theo ngày tạo đơn (giống trang Thống kê đang có).
 * Các phần tổng quan khác (doanh thu theo ngày, trạng thái đơn, nhân viên, khách) vẫn tính ở frontend từ danh sách hóa đơn.
 */
@Service
public class ThongKeService {

    static final int NGUONG_SAP_HET_MAC_DINH = 5;
    static final int SO_SAN_PHAM_BAN_CHAY = 10;
    static final int SO_SAP_HET_HIEN = 20;

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final LichSuThanhToanRepository thanhToanRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final PhuongThucThanhToanRepository phuongThucRepo;
    private final CaLamViecRepository caRepo;
    private final LichLamViecRepository lichRepo;
    private final GiaoCaRepository giaoCaRepo;
    @Autowired(required = false)
    private SanPhamRepository sanPhamRepository;
    @Autowired(required = false)
    private DotGiamGiaRepository dotGiamGiaRepository;
    @Autowired(required = false)
    private PhieuGiamGiaRepository phieuGiamGiaRepository;
    @Autowired(required = false)
    private KhachHangRepository khachHangRepository;
    @Autowired(required = false)
    private NhanVienRepository nhanVienRepository;
    @Autowired(required = false)
    private LichGiamGiaService lichGiamGiaService;
    private Clock dongHo = Clock.systemDefaultZone();

    public ThongKeService(HoaDonRepository hoaDonRepo, HoaDonChiTietRepository chiTietRepo, LichSuThanhToanRepository thanhToanRepo,
                          SanPhamChiTietRepository spctRepo, PhuongThucThanhToanRepository phuongThucRepo, CaLamViecRepository caRepo,
                          LichLamViecRepository lichRepo, GiaoCaRepository giaoCaRepo) {
        this.hoaDonRepo = hoaDonRepo;
        this.chiTietRepo = chiTietRepo;
        this.thanhToanRepo = thanhToanRepo;
        this.spctRepo = spctRepo;
        this.phuongThucRepo = phuongThucRepo;
        this.caRepo = caRepo;
        this.lichRepo = lichRepo;
        this.giaoCaRepo = giaoCaRepo;
    }

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    /** Mặc định 30 ngày gần nhất; tối đa 1 năm mỗi lần xem. nguong: tồn kho từ mức này trở xuống là "sắp hết" (0 - 100). */
    @Transactional(readOnly = true)
    public Map<String, Object> tongHop(LocalDate tuNgay, LocalDate denNgay, Integer nguong) {
        LocalDateTime bayGio = LocalDateTime.now(dongHo);
        LocalDate den = denNgay == null ? bayGio.toLocalDate() : denNgay;
        LocalDate tu = tuNgay == null ? den.minusDays(29) : tuNgay;
        if (tu.isAfter(den)) throw ApiException.badRequest("Từ ngày phải trước hoặc cùng ngày với Đến ngày.");
        if (ChronoUnit.DAYS.between(tu, den) > 366) throw ApiException.badRequest("Mỗi lần xem thống kê tối đa 1 năm.");
        int ng = nguong == null ? NGUONG_SAP_HET_MAC_DINH : nguong;
        if (ng < 0 || ng > 100) throw ApiException.badRequest("Ngưỡng sắp hết hàng phải từ 0 đến 100.");
        LocalDateTime a = tu.atStartOfDay();
        LocalDateTime b = den.atTime(LocalTime.MAX);

        List<HoaDon> hoaDons = hoaDonRepo.findByTrangThaiAndNgayTaoBetween(HoaDon.HOAN_THANH, a, b);
        List<GiaoCa> giaoCas = giaoCaRepo.findByThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc(a, b);

        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("tuNgay", tu);
        kq.put("denNgay", den);
        kq.put("sanPhamBanChay", sanPhamBanChay(chiTietRepo.findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(HoaDon.HOAN_THANH, a, b)));
        List<SanPhamChiTiet> sapHet = spctRepo.findByTrangThaiAndSoLuongTonLessThanEqualOrderBySoLuongTonAscIdAsc(SanPhamChiTiet.HOAT_DONG, ng)
                .stream().filter(BanHangService::dangBan).toList();
        kq.put("nguongSapHet", ng);
        kq.put("soSapHet", sapHet.size());
        kq.put("soHetHang", sapHet.stream().filter(c -> ton(c) <= 0).count());
        kq.put("sapHetHang", sapHet.stream().limit(SO_SAP_HET_HIEN).map(ThongKeService::sapHetDto).toList());
        kq.put("theoPhuongThuc", theoPhuongThuc(hoaDons,
                thanhToanRepo.findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(HoaDon.HOAN_THANH, a, b), phuongThucRepo.findAll()));
        kq.put("theoGio", theoGio(hoaDons));
        kq.put("theoCa", theoCa(hoaDons, caRepo.findAllByOrderByGioBatDauAscIdAsc()));
        kq.put("chuyenCan", chuyenCan(lichRepo.findByNgayLamBetween(tu, den), giaoCas, bayGio));
        kq.put("giaoCa", giaoCa(giaoCas));
        return kq;
    }

    /* ---------------- sản phẩm ---------------- */

    /** Top sản phẩm theo số lượng bán (bằng nhau thì doanh thu cao hơn lên trước), kèm phân loại bán chạy nhất. */
    static List<Map<String, Object>> sanPhamBanChay(List<HoaDonChiTiet> chiTiets) {
        Map<Integer, Map<String, Object>> theoSp = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Integer>> slBienThe = new HashMap<>();
        Map<Integer, Set<Integer>> donCuaSp = new HashMap<>();
        Map<Integer, SanPhamChiTiet> bienThe = new HashMap<>();
        for (HoaDonChiTiet ct : chiTiets) {
            SanPhamChiTiet c = ct.getSanPhamChiTiet();
            if (c == null || c.getSanPham() == null) continue;
            SanPham sp = c.getSanPham();
            int sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            long tien = ct.getThanhTien() != null ? ct.getThanhTien().longValue()
                    : (ct.getDonGia() == null ? 0 : ct.getDonGia().longValue() * sl);
            Map<String, Object> m = theoSp.computeIfAbsent(sp.getId(), k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("idSanPham", sp.getId());
                x.put("maSanPham", sp.getMaSanPham());
                x.put("tenSanPham", sp.getTenSanPham());
                x.put("thuongHieu", sp.getThuongHieu() == null ? null : sp.getThuongHieu().getTen());
                x.put("soLuong", 0L);
                x.put("doanhThu", 0L);
                return x;
            });
            m.put("soLuong", (Long) m.get("soLuong") + sl);
            m.put("doanhThu", (Long) m.get("doanhThu") + tien);
            slBienThe.computeIfAbsent(sp.getId(), k -> new HashMap<>()).merge(c.getId(), sl, Integer::sum);
            bienThe.put(c.getId(), c);
            if (ct.getHoaDon() != null) donCuaSp.computeIfAbsent(sp.getId(), k -> new HashSet<>()).add(ct.getHoaDon().getId());
        }
        for (Map.Entry<Integer, Map<String, Object>> e : theoSp.entrySet()) {
            e.getValue().put("soDon", donCuaSp.getOrDefault(e.getKey(), Set.of()).size());
            slBienThe.get(e.getKey()).entrySet().stream()
                    .max(Map.Entry.<Integer, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                    .ifPresent(b -> e.getValue().put("phanLoaiBanChay", BanHangService.phanLoai(bienThe.get(b.getKey()))));
        }
        return theoSp.values().stream()
                .sorted(Comparator.comparing((Map<String, Object> m) -> (Long) m.get("soLuong")).reversed()
                        .thenComparing(m -> (Long) m.get("doanhThu"), Comparator.reverseOrder()))
                .limit(SO_SAN_PHAM_BAN_CHAY).toList();
    }

    private static int ton(SanPhamChiTiet c) {
        return c.getSoLuongTon() == null ? 0 : c.getSoLuongTon();
    }

    private static Map<String, Object> sapHetDto(SanPhamChiTiet c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("ma", c.getMaSpct());
        m.put("idSanPham", c.getSanPham().getId());
        m.put("tenSanPham", c.getSanPham().getTenSanPham());
        m.put("phanLoai", BanHangService.phanLoai(c));
        m.put("tonKho", ton(c));
        return m;
    }

    /* ---------------- doanh thu theo phương thức / giờ / ca ---------------- */

    /**
     * Tiền thu theo phương thức: lấy từ lịch sử thanh toán (đơn tiền mặt + chuyển khoản tách đúng 2 phần); đơn cũ chưa ghi
     * phương thức trên từng lần thanh toán thì tính cả đơn vào phương thức của hóa đơn.
     */
    static List<Map<String, Object>> theoPhuongThuc(List<HoaDon> hoaDons, List<LichSuThanhToan> thanhToans, List<PhuongThucThanhToan> phuongThucs) {
        Map<Integer, PhuongThucThanhToan> pt = new HashMap<>();
        for (PhuongThucThanhToan p : phuongThucs) pt.put(p.getId(), p);
        Map<Integer, List<LichSuThanhToan>> ttTheoDon = new HashMap<>();
        for (LichSuThanhToan t : thanhToans) {
            if (t.getHoaDon() == null || !Integer.valueOf(LichSuThanhToan.DA_THANH_TOAN).equals(t.getTrangThai())) continue;
            ttTheoDon.computeIfAbsent(t.getHoaDon().getId(), k -> new ArrayList<>()).add(t);
        }
        Map<Integer, long[]> tong = new LinkedHashMap<>(); // idPhuongThuc (-1 = chưa rõ) -> { tiền, số đơn }
        for (HoaDon h : hoaDons) {
            List<LichSuThanhToan> ds = ttTheoDon.getOrDefault(h.getId(), List.of());
            if (!ds.isEmpty() && ds.stream().allMatch(t -> t.getIdPhuongThucThanhToan() != null)) {
                Set<Integer> daDem = new HashSet<>();
                for (LichSuThanhToan t : ds) {
                    long[] x = tong.computeIfAbsent(t.getIdPhuongThucThanhToan(), k -> new long[2]);
                    x[0] += t.getSoTien() == null ? 0 : t.getSoTien().longValue();
                    if (daDem.add(t.getIdPhuongThucThanhToan())) x[1]++;
                }
            } else {
                long[] x = tong.computeIfAbsent(h.getIdPhuongThucThanhToan() == null ? -1 : h.getIdPhuongThucThanhToan(), k -> new long[2]);
                x[0] += tien(h.getThanhTien());
                x[1]++;
            }
        }
        List<Map<String, Object>> kq = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : tong.entrySet()) {
            PhuongThucThanhToan p = pt.get(e.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma", p == null ? null : p.getMaPhuongThuc());
            m.put("ten", p == null ? "Chưa rõ phương thức" : p.getTenPhuongThuc());
            m.put("soTien", e.getValue()[0]);
            m.put("soDon", e.getValue()[1]);
            kq.add(m);
        }
        kq.sort(Comparator.comparing((Map<String, Object> m) -> (Long) m.get("soTien")).reversed()
                .thenComparing(m -> (Long) m.get("soDon"), Comparator.reverseOrder()));
        return kq;
    }

    /** 24 khung giờ (0h - 23h): số đơn hoàn thành + doanh thu theo giờ tạo đơn. */
    static List<Map<String, Object>> theoGio(List<HoaDon> hoaDons) {
        long[] so = new long[24];
        long[] tien = new long[24];
        for (HoaDon h : hoaDons) {
            if (h.getNgayTao() == null) continue;
            int g = h.getNgayTao().getHour();
            so[g]++;
            tien[g] += tien(h.getThanhTien());
        }
        List<Map<String, Object>> kq = new ArrayList<>();
        for (int g = 0; g < 24; g++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gio", g);
            m.put("soDon", so[g]);
            m.put("doanhThu", tien[g]);
            kq.add(m);
        }
        return kq;
    }

    /** Doanh thu theo ca làm việc (giờ tạo đơn rơi vào khung giờ của ca); ngoài mọi ca thì vào "Ngoài giờ ca". */
    static List<Map<String, Object>> theoCa(List<HoaDon> hoaDons, List<CaLamViec> cas) {
        List<CaLamViec> dangDung = cas.stream().filter(c -> Integer.valueOf(CaLamViec.HOAT_DONG).equals(c.getTrangThai())
                && c.getGioBatDau() != null && c.getGioKetThuc() != null).toList();
        Map<String, long[]> tong = new LinkedHashMap<>();
        Map<String, String> gio = new LinkedHashMap<>();
        for (CaLamViec c : dangDung) {
            tong.put(c.getTenCa(), new long[2]);
            gio.put(c.getTenCa(), c.getGioBatDau() + " - " + c.getGioKetThuc());
        }
        long[] ngoai = new long[2];
        for (HoaDon h : hoaDons) {
            if (h.getNgayTao() == null) continue;
            LocalTime t = h.getNgayTao().toLocalTime();
            long[] x = ngoai;
            for (CaLamViec c : dangDung) {
                if (!t.isBefore(c.getGioBatDau()) && t.isBefore(c.getGioKetThuc())) {
                    x = tong.get(c.getTenCa());
                    break;
                }
            }
            x[0]++;
            x[1] += tien(h.getThanhTien());
        }
        List<Map<String, Object>> kq = new ArrayList<>();
        for (Map.Entry<String, long[]> e : tong.entrySet()) kq.add(caDto(e.getKey(), gio.get(e.getKey()), e.getValue()));
        if (ngoai[0] > 0) kq.add(caDto("Ngoài giờ ca", null, ngoai));
        return kq;
    }

    private static Map<String, Object> caDto(String ten, String gio, long[] x) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tenCa", ten);
        m.put("gio", gio);
        m.put("soDon", x[0]);
        m.put("doanhThu", x[1]);
        return m;
    }

    /* ---------------- lịch làm việc + giao ca ---------------- */

    /**
     * Chuyên cần từng nhân viên trong khoảng: số ca theo lịch, đã làm, vắng (ca đã kết thúc mà không nhận ca), chưa tới,
     * số lần đi muộn (quá PHUT_MUON_CHO_PHEP phút), tổng phút muộn, số ca làm ngoài lịch, tổng phút làm (ca đã giao).
     */
    static List<Map<String, Object>> chuyenCan(List<LichLamViec> lichs, List<GiaoCa> giaoCas, LocalDateTime bayGio) {
        Map<Integer, GiaoCa> theoLich = new HashMap<>();
        for (GiaoCa g : giaoCas) if (g.getLichLamViec() != null) theoLich.putIfAbsent(g.getLichLamViec().getId(), g);
        Map<Integer, Map<String, Object>> kq = new LinkedHashMap<>();
        for (LichLamViec l : lichs) {
            if (l.getNhanVien() == null || Integer.valueOf(0).equals(l.getTrangThai())) continue;
            Map<String, Object> m = dongNhanVien(kq, l.getNhanVien());
            cong(m, "soCaLich", 1);
            GiaoCa g = theoLich.get(l.getId());
            if (g != null) {
                cong(m, "soCaDaLam", 1);
            } else if (daQua(l, bayGio)) {
                cong(m, "soCaVang", 1);
            } else {
                cong(m, "soCaChuaToi", 1);
            }
        }
        for (GiaoCa g : giaoCas) {
            if (g.getNhanVien() == null) continue;
            Map<String, Object> m = dongNhanVien(kq, g.getNhanVien());
            if (g.getLichLamViec() == null) cong(m, "soCaNgoaiLich", 1);
            int muon = g.getSoPhutDiMuon() == null ? 0 : g.getSoPhutDiMuon();
            if (muon > GiaoCaService.PHUT_MUON_CHO_PHEP) {
                cong(m, "soLanDiMuon", 1);
                cong(m, "tongPhutMuon", muon);
            }
            if (g.getThoiGianKetCa() != null && g.getThoiGianNhanCa() != null) {
                cong(m, "soPhutLam", Math.max(0, Duration.between(g.getThoiGianNhanCa(), g.getThoiGianKetCa()).toMinutes()));
            }
        }
        return kq.values().stream().sorted(Comparator.comparing(m -> String.valueOf(m.get("hoTen")))).toList();
    }

    /** Ca theo lịch đã kết thúc (ngày đã qua, hoặc hôm nay mà đã quá giờ kết thúc ca). */
    static boolean daQua(LichLamViec l, LocalDateTime bayGio) {
        if (l.getNgayLam() == null) return false;
        if (l.getNgayLam().isBefore(bayGio.toLocalDate())) return true;
        if (l.getNgayLam().isAfter(bayGio.toLocalDate())) return false;
        LocalTime ket = l.getCaLamViec() == null ? null : l.getCaLamViec().getGioKetThuc();
        return ket != null && !bayGio.toLocalTime().isBefore(ket);
    }

    private static Map<String, Object> dongNhanVien(Map<Integer, Map<String, Object>> kq, NhanVien nv) {
        return kq.computeIfAbsent(nv.getId(), k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idNhanVien", nv.getId());
            m.put("maNhanVien", nv.getMaNhanVien());
            m.put("hoTen", nv.getHoTen());
            for (String f : List.of("soCaLich", "soCaDaLam", "soCaVang", "soCaChuaToi", "soLanDiMuon", "tongPhutMuon", "soCaNgoaiLich", "soPhutLam")) {
                m.put(f, 0L);
            }
            return m;
        });
    }

    private static void cong(Map<String, Object> m, String khoa, long n) {
        m.put(khoa, (Long) m.get(khoa) + n);
    }

    /** Tổng hợp giao ca: số ca đã giao, số ca lệch tiền, tổng thiếu / thừa và danh sách ca lệch (lệch nhiều lên trước). */
    static Map<String, Object> giaoCa(List<GiaoCa> giaoCas) {
        long daGiao = 0;
        long thieu = 0;
        long thua = 0;
        List<Map<String, Object>> lech = new ArrayList<>();
        for (GiaoCa g : giaoCas) {
            if (!Integer.valueOf(GiaoCa.DA_GIAO_CA).equals(g.getTrangThai())) continue;
            daGiao++;
            long cl = tien(g.getChenhLech());
            if (cl == 0) continue;
            if (cl < 0) thieu += -cl;
            else thua += cl;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("maGiaoCa", g.getMaGiaoCa());
            m.put("tenNhanVien", g.getNhanVien() == null ? null : g.getNhanVien().getHoTen());
            m.put("thoiGianKetCa", g.getThoiGianKetCa());
            m.put("chenhLech", cl);
            m.put("ghiChu", g.getGhiChuKetCa());
            lech.add(m);
        }
        lech.sort(Comparator.comparing((Map<String, Object> m) -> Math.abs((Long) m.get("chenhLech"))).reversed());
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("soCaDaGiao", daGiao);
        kq.put("soCaLech", lech.size());
        kq.put("tongThieu", thieu);
        kq.put("tongThua", thua);
        kq.put("caLech", lech.size() > 20 ? lech.subList(0, 20) : lech);
        return kq;
    }

    private static long tien(BigDecimal v) {
        return v == null ? 0 : v.longValue();
    }

    private static BigDecimal khongAm(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        return v;
    }

    /* ---------------- Liên kết toàn hệ thống & CSDL (GET /api/thong-ke/tong-quan) ---------------- */

    @Transactional(readOnly = true)
    public Map<String, Object> layTongQuan() {
        LocalDateTime now = LocalDateTime.now(dongHo);
        Map<Long, Double> giamHomNay = lichGiamGiaService != null ? lichGiamGiaService.giamHomNay() : Map.of();

        List<Map<String, Object>> chiTietHoaDon = new ArrayList<>();
        for (HoaDonChiTiet ct : chiTietRepo.findAll()) {
            HoaDon hd = ct.getHoaDon();
            if (hd == null) continue;
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            SanPham sp = spct != null ? spct.getSanPham() : null;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idHoaDon", hd.getId());
            m.put("maHoaDon", hd.getMaHoaDon());
            m.put("ngayTao", hd.getNgayTao());
            m.put("trangThaiHoaDon", hd.getTrangThai());
            m.put("idBienThe", spct != null ? spct.getId() : null);
            m.put("maBienThe", spct != null ? spct.getMaSpct() : null);
            m.put("tenBienThe", moTaBienThe(spct));
            m.put("soLuongTon", spct != null && spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0);
            m.put("idSanPham", sp != null ? sp.getId() : null);
            m.put("maSanPham", sp != null ? sp.getMaSanPham() : null);
            m.put("tenSanPham", sp != null ? sp.getTenSanPham() : "Sản phẩm");
            m.put("tenThuongHieu", sp != null && sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : "Khác");
            m.put("tenDanhMuc", sp != null && sp.getDanhMuc() != null ? sp.getDanhMuc().getTen() : "Khác");
            m.put("soLuong", ct.getSoLuong() != null ? Math.max(0, ct.getSoLuong()) : 0);
            m.put("donGia", khongAm(ct.getDonGia()));
            m.put("thanhTien", khongAm(ct.getThanhTien()));
            chiTietHoaDon.add(m);
        }

        List<Map<String, Object>> uuDaiHoaDon = new ArrayList<>();
        for (HoaDon hd : hoaDonRepo.findAll()) {
            KhachHang kh = hd.getKhachHang();
            NhanVien nv = hd.getNhanVien();
            PhieuGiamGia pgg = hd.getPhieuGiamGia();
            boolean taiQuay = Integer.valueOf(HoaDon.TAI_QUAY).equals(hd.getLoaiDon());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idHoaDon", hd.getId());
            m.put("maHoaDon", hd.getMaHoaDon());
            m.put("ngayTao", hd.getNgayTao());
            m.put("trangThai", hd.getTrangThai());
            m.put("idKhachHang", kh != null ? kh.getId() : null);
            m.put("maKhachHang", kh != null ? kh.getMaKhachHang() : null);
            m.put("tenKhachHang", kh != null ? kh.getHoTen() : null);
            m.put("sdtKhachHang", kh != null ? kh.getSdt() : null);
            m.put("idNhanVien", nv != null ? nv.getId() : null);
            m.put("maNhanVien", nv != null ? nv.getMaNhanVien() : null);
            m.put("tenNhanVien", nv != null ? nv.getHoTen() : null);
            m.put("idPhieuGiamGia", pgg != null ? pgg.getId() : null);
            m.put("maPhieuGiamGia", pgg != null ? pgg.getMaPhieu() : null);
            m.put("tenPhieuGiamGia", pgg != null ? pgg.getTenPhieu() : null);
            m.put("soTienGiam", khongAm(hd.getSoTienGiam()));
            m.put("phiVanChuyen", taiQuay ? BigDecimal.ZERO : khongAm(hd.getPhiVanChuyen()));
            m.put("tongTienHang", khongAm(hd.getTongTien()));
            uuDaiHoaDon.add(m);
        }

        List<SanPhamChiTiet> dsBienThe = spctRepo.findAll();
        long tongBienTheHoatDong = 0;
        long tongTonKho = 0;
        long soBienTheSapHet = 0;
        long soBienTheHetHang = 0;
        List<Map<String, Object>> tonKhoCanhBao = new ArrayList<>();

        List<SanPhamChiTiet> bienTheDangBan = new ArrayList<>();
        for (SanPhamChiTiet b : dsBienThe) {
            SanPham sp = b.getSanPham();
            if (!Integer.valueOf(1).equals(b.getTrangThai()) || sp == null || !Integer.valueOf(1).equals(sp.getTrangThai())) {
                continue;
            }
            tongBienTheHoatDong++;
            int ton = b.getSoLuongTon() != null ? b.getSoLuongTon() : 0;
            tongTonKho += Math.max(ton, 0);
            if (ton <= 0) soBienTheHetHang++;
            else if (ton <= 5) soBienTheSapHet++;
            if (ton <= 10) bienTheDangBan.add(b);
        }
        bienTheDangBan.sort(Comparator.comparingInt(b -> b.getSoLuongTon() != null ? b.getSoLuongTon() : 0));

        for (int i = 0; i < Math.min(bienTheDangBan.size(), 20); i++) {
            SanPhamChiTiet b = bienTheDangBan.get(i);
            SanPham sp = b.getSanPham();
            Double ptGiam = b.getId() != null ? giamHomNay.get(b.getId().longValue()) : null;
            long giaBan = b.getGiaBan() != null ? b.getGiaBan() : 0L;
            long giaSauGiam = ptGiam != null && ptGiam > 0 ? Math.round(giaBan * (100.0 - ptGiam) / 100.0) : giaBan;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idBienThe", b.getId());
            m.put("maBienThe", b.getMaSpct());
            m.put("idSanPham", sp != null ? sp.getId() : null);
            m.put("maSanPham", sp != null ? sp.getMaSanPham() : null);
            m.put("tenSanPham", sp != null ? sp.getTenSanPham() : "");
            m.put("tenThuongHieu", sp != null && sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : "");
            m.put("tenBienThe", moTaBienThe(b));
            m.put("giaBan", giaBan);
            m.put("giaSauGiam", giaSauGiam);
            m.put("phanTramGiam", ptGiam != null ? ptGiam : 0);
            m.put("soLuongTon", b.getSoLuongTon() != null ? b.getSoLuongTon() : 0);
            tonKhoCanhBao.add(m);
        }

        long tongSanPhamHoatDong = sanPhamRepository == null ? 0 : sanPhamRepository.findAll().stream()
                .filter(s -> Integer.valueOf(1).equals(s.getTrangThai()))
                .count();
        long tongKhachHangHoatDong = khachHangRepository == null ? 0 : khachHangRepository.findAll().stream()
                .filter(k -> Integer.valueOf(1).equals(k.getTrangThai()))
                .count();
        long tongNhanVienHoatDong = nhanVienRepository == null ? 0 : nhanVienRepository.findAll().stream()
                .filter(n -> Integer.valueOf(1).equals(n.getTrangThai()))
                .count();

        long soDotDangDienRa = 0;
        long soDotSapDienRa = 0;
        List<Map<String, Object>> dotDangChay = new ArrayList<>();
        if (dotGiamGiaRepository != null) {
            for (DotGiamGia d : dotGiamGiaRepository.findAll()) {
                if (!Integer.valueOf(1).equals(d.getTrangThai())) continue;
                if (d.getNgayKetThuc() != null && d.getNgayKetThuc().isBefore(now)) continue;
                boolean sapDienRa = d.getNgayBatDau() != null && d.getNgayBatDau().isAfter(now);
                if (sapDienRa) soDotSapDienRa++;
                else soDotDangDienRa++;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", d.getId());
                m.put("maDot", d.getMaDot());
                m.put("tenDot", d.getTenDot());
                m.put("phanTramGiamDot", d.getPhanTramGiamDot());
                m.put("ngayBatDau", d.getNgayBatDau());
                m.put("ngayKetThuc", d.getNgayKetThuc());
                m.put("trangThaiText", sapDienRa ? "Sắp diễn ra" : "Đang diễn ra");
                dotDangChay.add(m);
            }
        }

        long soPhieuDangHoatDong = 0;
        List<Map<String, Object>> phieuDangChay = new ArrayList<>();
        if (phieuGiamGiaRepository != null) {
            for (PhieuGiamGia p : phieuGiamGiaRepository.findAllByOrderByIdDesc()) {
                if (!Integer.valueOf(1).equals(p.getTrangThai())) continue;
                if (p.getNgayKetThuc() != null && p.getNgayKetThuc().isBefore(now)) continue;
                boolean sapDienRa = p.getNgayBatDau() != null && p.getNgayBatDau().isAfter(now);
                soPhieuDangHoatDong++;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("maPhieu", p.getMaPhieu());
                m.put("tenPhieu", p.getTenPhieu());
                m.put("loaiGiam", p.getLoaiGiam());
                m.put("giaTriGiam", p.getGiaTriGiam());
                m.put("soLuong", p.getSoLuong());
                m.put("soLuongDaDung", p.getSoLuongDaDung() != null ? p.getSoLuongDaDung() : 0);
                m.put("ngayBatDau", p.getNgayBatDau());
                m.put("ngayKetThuc", p.getNgayKetThuc());
                m.put("trangThaiText", sapDienRa ? "Sắp diễn ra" : "Đang hoạt động");
                phieuDangChay.add(m);
            }
        }

        Map<String, Object> heThong = new LinkedHashMap<>();
        heThong.put("tongSanPhamHoatDong", tongSanPhamHoatDong);
        heThong.put("tongBienTheHoatDong", tongBienTheHoatDong);
        heThong.put("tongTonKho", tongTonKho);
        heThong.put("soBienTheSapHet", soBienTheSapHet);
        heThong.put("soBienTheHetHang", soBienTheHetHang);
        heThong.put("tongKhachHangHoatDong", tongKhachHangHoatDong);
        heThong.put("tongNhanVienHoatDong", tongNhanVienHoatDong);
        heThong.put("soDotDangDienRa", soDotDangDienRa);
        heThong.put("soDotSapDienRa", soDotSapDienRa);
        heThong.put("dotDangChay", dotDangChay);
        heThong.put("soPhieuDangHoatDong", soPhieuDangHoatDong);
        heThong.put("phieuDangChay", phieuDangChay);

        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("chiTietHoaDon", chiTietHoaDon);
        kq.put("uuDaiHoaDon", uuDaiHoaDon);
        kq.put("tonKhoCanhBao", tonKhoCanhBao);
        kq.put("heThong", heThong);
        return kq;
    }

    private String moTaBienThe(SanPhamChiTiet spct) {
        if (spct == null) return "";
        List<String> phan = new ArrayList<>();
        if (spct.getMauSac() != null && hopLeThuocTinh(spct.getMauSac().getTen())) {
            phan.add(spct.getMauSac().getTen().trim());
        }
        if (spct.getTrongLuong() != null && hopLeThuocTinh(spct.getTrongLuong().getTen())) {
            phan.add(spct.getTrongLuong().getTen().trim());
        }
        if (spct.getChuVi() != null && hopLeThuocTinh(spct.getChuVi().getTen())) {
            phan.add(spct.getChuVi().getTen().trim());
        }
        return phan.isEmpty() ? Objects.toString(spct.getMaSpct(), "") : String.join(" · ", phan);
    }

    private boolean hopLeThuocTinh(String ten) {
        if (ten == null || ten.trim().isEmpty()) return false;
        String t = ten.trim().toLowerCase(Locale.ROOT);
        return !t.contains("không áp dụng") && !t.contains("khong ap dung");
    }
}
