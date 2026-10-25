package com.footstyle.demo.service;

import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.repository.GiaoCaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichLamViecRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trợ lý AI cho nhân viên (nút "Trợ lý AI" trên header trang quản trị). Giống trợ lý của khách: trả lời TỪ DỮ LIỆU THẬT,
 * không gọi dịch vụ AI bên ngoài. Phân quyền theo vai trò: doanh thu toàn cửa hàng chỉ Quản lý xem được.
 * Thứ tự nhận biết: tra mã đơn -> đơn chờ xử lý -> doanh thu -> sắp hết hàng -> tồn kho -> lịch / ca -> hướng dẫn thao tác
 * -> sản phẩm, khuyến mãi, chính sách (dùng lại TroLyAoService) -> chào hỏi / không hiểu.
 */
@Service
public class TroLyNoiBoService {

    public record LienKet(String nhan, String duongDan) {
    }

    public record TraLoi(String noiDung, List<String> goiY, List<LienKet> lienKet) {
    }

    static final int NGUONG_SAP_HET = 5;
    static final List<String> GOI_Y_QUAN_LY = List.of("Doanh thu hôm nay", "Đơn chờ xác nhận", "Sản phẩm sắp hết hàng",
            "Ai làm ca hôm nay?", "Cách giao ca");
    static final List<String> GOI_Y_NHAN_VIEN = List.of("Đơn chờ xác nhận", "Ca của tôi", "Lịch làm của tôi", "Sản phẩm sắp hết hàng",
            "Cách bán hàng tại quầy");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> TU_BO_TON_KHO = Set.of("ton", "kho", "con", "bao", "nhieu", "hang", "khong", "kiem", "tra", "xem",
            "so", "luong", "cua", "san", "pham", "mau", "cai", "chiec", "may", "trong", "the", "nao", "hien", "tai", "la", "gi", "nhe", "a", "oi");

    private final HoaDonRepository hoaDonRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final LichLamViecRepository lichRepo;
    private final GiaoCaRepository giaoCaRepo;
    private final TroLyAoService troLyKhach;
    private Clock dongHo = Clock.systemDefaultZone();

    public TroLyNoiBoService(HoaDonRepository hoaDonRepo, SanPhamChiTietRepository spctRepo, LichLamViecRepository lichRepo,
                             GiaoCaRepository giaoCaRepo, TroLyAoService troLyKhach) {
        this.hoaDonRepo = hoaDonRepo;
        this.spctRepo = spctRepo;
        this.lichRepo = lichRepo;
        this.giaoCaRepo = giaoCaRepo;
        this.troLyKhach = troLyKhach;
    }

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    public TraLoi loiChao(NguoiDung nd) {
        String ten = nd == null || nd.hoTen() == null ? "bạn" : nd.hoTen();
        String doanhThu = nd != null && nd.laQuanLy() ? "• Doanh thu hôm nay / tuần này / tháng này\n" : "";
        return new TraLoi("Chào " + ten + "! Mình là trợ lý AI nội bộ, trả lời từ dữ liệu thật của cửa hàng:\n" + doanhThu
                + "• Đơn chờ xác nhận, tra nhanh đơn theo mã (vd: HD12)\n"
                + "• Tồn kho một sản phẩm, sản phẩm sắp hết hàng\n"
                + "• Lịch làm hôm nay, lịch của bạn, ca đang làm\n"
                + "• Hướng dẫn thao tác: bán tại quầy, giao ca, nhập Excel...\n"
                + "• Sản phẩm theo giá / hãng, khuyến mãi đang chạy", goiY(nd), List.of());
    }

    @Transactional(readOnly = true)
    public TraLoi traLoi(String cauHoi, NguoiDung nd) {
        String t = " " + BanHangService.boDau(cauHoi).replaceAll("[^a-z0-9.,/\\- ]", " ").replaceAll("\\s+", " ").trim() + " ";
        if (t.isBlank()) return loiChao(nd);

        TraLoi kq = traCuuDon(t);
        if (kq == null && TroLyAoService.coMot(t, "don cho", "cho xac nhan", "don moi", "can xu ly", "chua xac nhan", "don dang cho")) kq = donChoXuLy();
        if (kq == null && TroLyAoService.coMot(t, "doanh thu", "doanh so", "ban duoc bao nhieu", "thu duoc")) kq = doanhThu(t, nd);
        if (kq == null && TroLyAoService.coMot(t, "sap het", "het hang", "ton kho thap", "can nhap", "nhap them")) kq = sapHetHang();
        if (kq == null && TroLyAoService.coMot(t, "ton kho", "con bao nhieu", "con hang", "con khong", "so luong ton")) kq = tonKho(t);
        if (kq == null && TroLyAoService.coMot(t, "ca cua toi", "dang trong ca", "ca hien tai", "toi da nhan ca", "ca dang lam")) kq = caCuaToi(nd);
        if (kq == null && TroLyAoService.coMot(t, "lich cua toi", "toi lam ca", "toi lam ngay", "lich lam cua toi", "khi nao toi lam")) kq = lichCuaToi(nd);
        if (kq == null && TroLyAoService.coMot(t, "ca hom nay", "lam hom nay", "ai lam", "lich hom nay", "lich lam hom nay", "ai truc")) kq = lichHomNay(nd);
        if (kq == null) kq = huongDan(t, nd);
        if (kq != null) return kq;

        TroLyAoService.TraLoi khach = troLyKhach.traLoi(cauHoi, null);
        if (!khach.chuyenNhanVien() && !khach.noiDung().startsWith("Xin lỗi") && !khach.noiDung().startsWith("Xin chào")) {
            return new TraLoi(khach.noiDung().replace("Cần hỗ trợ thêm về đơn này, bạn bấm \"Gặp nhân viên\" nhé.", "").trim(),
                    goiY(nd), List.of());
        }
        if (TroLyAoService.coMot(t, " xin chao ", " chao ", " hello ", " hi ", " alo ", " giup ", " lam duoc gi ")) return loiChao(nd);
        return new TraLoi("Mình chưa hiểu câu hỏi. Bạn thử hỏi theo các gợi ý bên dưới, ví dụ \"tồn kho Astrox 99\", \"đơn HD12\", "
                + "\"ai làm ca hôm nay\" hoặc \"cách giao ca\".", goiY(nd), List.of());
    }

    /* ===================== Đơn hàng ===================== */

    TraLoi traCuuDon(String t) {
        Matcher m = TroLyAoService.MA_DON.matcher(t);
        if (!m.find()) return null;
        String maGo = "HD" + m.group(1);
        String ma = MaUtil.dinhDang("HD", Long.parseLong(m.group(1)));
        Optional<HoaDon> hd = hoaDonRepo.findFirstByMaHoaDonIgnoreCase(maGo);
        if (hd.isEmpty() && !ma.equals(maGo)) hd = hoaDonRepo.findFirstByMaHoaDonIgnoreCase(ma);
        if (hd.isEmpty()) return new TraLoi("Không có hóa đơn " + maGo + ".", List.of("Đơn chờ xác nhận"), List.of(new LienKet("Danh sách hóa đơn", "/hoa-don")));
        HoaDon h = hd.get();
        StringBuilder sb = new StringBuilder("Hóa đơn ").append(h.getMaHoaDon()).append(" (")
                .append(Integer.valueOf(HoaDon.GIAO_HANG).equals(h.getLoaiDon()) ? "giao hàng" : "tại quầy").append("):\n");
        sb.append("• Trạng thái: ").append(HoaDonLuongTrangThai.ten(h.getTrangThai())).append('\n');
        String khach = h.getKhachHang() != null ? h.getKhachHang().getHoTen() + " - " + h.getKhachHang().getSdt()
                : h.getHoTenNguoiNhan() != null ? h.getHoTenNguoiNhan() + (h.getSdtNguoiNhan() != null ? " - " + h.getSdtNguoiNhan() : "") : "Khách vãng lai";
        sb.append("• Khách: ").append(khach).append('\n');
        if (h.getNgayTao() != null) sb.append("• Ngày tạo: ").append(h.getNgayTao().format(GIO)).append(' ').append(h.getNgayTao().format(NGAY)).append('\n');
        if (h.getNhanVien() != null) sb.append("• Nhân viên: ").append(h.getNhanVien().getHoTen()).append('\n');
        sb.append("• Thành tiền: ").append(tien(h.getThanhTien()));
        return new TraLoi(sb.toString(), List.of("Đơn chờ xác nhận"), List.of(new LienKet("Mở " + h.getMaHoaDon(), "/hoa-don/" + h.getId())));
    }

    TraLoi donChoXuLy() {
        var trang = hoaDonRepo.findByTrangThai(HoaDon.CHO_XAC_NHAN, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "ngayTao")));
        long tong = trang.getTotalElements();
        if (tong == 0) return new TraLoi("Không có đơn nào đang chờ xác nhận. Tốt lắm!", List.of("Sản phẩm sắp hết hàng"), List.of());
        StringBuilder sb = new StringBuilder("Có ").append(tong).append(" đơn đang chờ xác nhận").append(tong > 5 ? ", 5 đơn mới nhất:" : ":").append('\n');
        for (HoaDon h : trang.getContent()) {
            sb.append("• ").append(h.getMaHoaDon()).append(" · ").append(tien(h.getThanhTien()));
            if (h.getNgayTao() != null) sb.append(" · ").append(h.getNgayTao().format(GIO)).append(' ').append(h.getNgayTao().format(NGAY));
            sb.append('\n');
        }
        return new TraLoi(sb.toString().trim(), List.of(), List.of(new LienKet("Mở danh sách hóa đơn", "/hoa-don")));
    }

    /* ===================== Doanh thu ===================== */

    TraLoi doanhThu(String t, NguoiDung nd) {
        if (nd == null || !nd.laQuanLy()) {
            GiaoCa ca = nd == null ? null : giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA).orElse(null);
            String phu = ca == null ? " Bạn chưa nhận ca nên chưa có doanh thu ca." : " " + tomTatCa(ca);
            return new TraLoi("Doanh thu toàn cửa hàng chỉ Quản lý xem được." + phu, List.of("Ca của tôi"), List.of());
        }
        LocalDate homNay = LocalDate.now(dongHo);
        LocalDate tu = homNay;
        LocalDate den = homNay;
        String nhan = "hôm nay";
        if (TroLyAoService.coMot(t, "hom qua")) {
            tu = den = homNay.minusDays(1);
            nhan = "hôm qua";
        } else if (TroLyAoService.coMot(t, "tuan nay", "tuan")) {
            tu = homNay.with(DayOfWeek.MONDAY);
            nhan = "tuần này";
        } else if (TroLyAoService.coMot(t, "thang nay", "thang")) {
            tu = homNay.withDayOfMonth(1);
            nhan = "tháng này";
        }
        List<HoaDon> ds = hoaDonRepo.findByTrangThaiAndNgayTaoBetween(HoaDon.HOAN_THANH, tu.atStartOfDay(), den.atTime(LocalTime.MAX));
        BigDecimal tong = BigDecimal.ZERO;
        int taiQuay = 0;
        for (HoaDon h : ds) {
            tong = tong.add(DoanhThu.cua(h));
            if (Integer.valueOf(HoaDon.TAI_QUAY).equals(h.getLoaiDon())) taiQuay++;
        }
        String khoang = tu.equals(den) ? tu.format(NGAY) : tu.format(NGAY) + " - " + den.format(NGAY);
        String noiDung = "Doanh thu " + nhan + " (" + khoang + "): " + tien(tong) + "\n• " + ds.size() + " đơn hoàn thành ("
                + taiQuay + " tại quầy, " + (ds.size() - taiQuay) + " giao hàng)"
                + (ds.isEmpty() ? "" : "\n• Trung bình " + tien(tong.divide(BigDecimal.valueOf(ds.size()), 0, java.math.RoundingMode.HALF_UP)) + "/đơn");
        return new TraLoi(noiDung, List.of("Doanh thu tuần này", "Doanh thu tháng này"), List.of(new LienKet("Xem thống kê", "/thong-ke")));
    }

    /* ===================== Kho ===================== */

    TraLoi sapHetHang() {
        List<SanPhamChiTiet> ds = spctRepo.findByTrangThaiAndSoLuongTonLessThanEqualOrderBySoLuongTonAscIdAsc(1, NGUONG_SAP_HET);
        if (ds.isEmpty()) return new TraLoi("Không có phân loại nào còn từ " + NGUONG_SAP_HET + " sản phẩm trở xuống.", List.of(), List.of());
        StringBuilder sb = new StringBuilder(ds.size() + " phân loại còn ≤ " + NGUONG_SAP_HET + ":\n");
        ds.stream().limit(8).forEach(c -> sb.append("• ").append(tenBienThe(c)).append(": còn ").append(c.getSoLuongTon()).append('\n'));
        if (ds.size() > 8) sb.append("… và ").append(ds.size() - 8).append(" phân loại khác.");
        return new TraLoi(sb.toString().trim(), List.of(), List.of(new LienKet("Biến thể sản phẩm", "/san-pham/bien-the")));
    }

    TraLoi tonKho(String t) {
        List<String> tu = new ArrayList<>();
        for (String w : t.trim().split(" ")) if (w.length() >= 2 && !TU_BO_TON_KHO.contains(w)) tu.add(w);
        if (tu.isEmpty()) return new TraLoi("Bạn muốn xem tồn kho sản phẩm nào? Ví dụ: \"tồn kho Astrox 99\" hoặc \"còn bao nhiêu SP01\".",
                List.of("Sản phẩm sắp hết hàng"), List.of());
        List<SanPhamChiTiet> khop = new ArrayList<>();
        for (SanPhamChiTiet c : spctRepo.findAllByOrderByIdDesc()) {
            String chuoi = " " + BanHangService.boDau(tenBienThe(c) + " " + c.getMaSpct()).replaceAll("[^a-z0-9]+", " ") + " ";
            if (tu.stream().allMatch(w -> chuoi.contains(" " + w))) khop.add(c);
        }
        if (khop.isEmpty()) return null;
        khop.sort(Comparator.comparing(SanPhamChiTiet::getMaSpct));
        int tong = khop.stream().mapToInt(c -> c.getSoLuongTon() == null ? 0 : c.getSoLuongTon()).sum();
        StringBuilder sb = new StringBuilder("Tồn kho (" + khop.size() + " phân loại, tổng " + tong + "):\n");
        khop.stream().limit(8).forEach(c -> sb.append("• ").append(tenBienThe(c)).append(" [").append(c.getMaSpct()).append("]: ")
                .append(c.getSoLuongTon()).append(Integer.valueOf(1).equals(c.getTrangThai()) ? "" : " (ngừng bán)").append('\n'));
        if (khop.size() > 8) sb.append("… và ").append(khop.size() - 8).append(" phân loại khác.");
        return new TraLoi(sb.toString().trim(), List.of("Sản phẩm sắp hết hàng"), List.of(new LienKet("Biến thể sản phẩm", "/san-pham/bien-the")));
    }

    static String tenBienThe(SanPhamChiTiet c) {
        List<String> phan = new ArrayList<>();
        if (c.getMauSac() != null) phan.add(c.getMauSac().getTen());
        if (c.getTrongLuong() != null && !"TL000".equalsIgnoreCase(c.getTrongLuong().getMa())) phan.add(c.getTrongLuong().getTen());
        if (c.getChuVi() != null && !"CV000".equalsIgnoreCase(c.getChuVi().getMa())) phan.add(c.getChuVi().getTen());
        String ten = c.getSanPham() == null ? c.getMaSpct() : c.getSanPham().getTenSanPham();
        return phan.isEmpty() ? ten : ten + " · " + String.join(" · ", phan);
    }

    /* ===================== Lịch / ca ===================== */

    TraLoi lichHomNay(NguoiDung nd) {
        LocalDate homNay = LocalDate.now(dongHo);
        List<LichLamViec> ds = lichRepo.findByNgayLamBetween(homNay, homNay);
        List<LienKet> lk = nd != null && nd.laQuanLy() ? List.of(new LienKet("Xếp lịch làm việc", "/lich-lam-viec")) : List.of(new LienKet("Lịch làm của tôi", "/lich-cua-toi"));
        if (ds.isEmpty()) return new TraLoi("Hôm nay (" + homNay.format(NGAY) + ") chưa xếp lịch cho ai.", List.of(), lk);
        Map<CaLamViec, List<String>> theoCa = new LinkedHashMap<>();
        ds.stream().sorted(Comparator.comparing((LichLamViec l) -> l.getCaLamViec().getGioBatDau()).thenComparing(l -> l.getNhanVien().getHoTen()))
                .forEach(l -> theoCa.computeIfAbsent(l.getCaLamViec(), k -> new ArrayList<>()).add(l.getNhanVien().getHoTen()));
        StringBuilder sb = new StringBuilder("Lịch làm hôm nay (" + homNay.format(NGAY) + "):\n");
        theoCa.forEach((ca, ten) -> sb.append("• ").append(ca.getTenCa()).append(" (").append(ca.getGioBatDau().format(GIO)).append(" - ")
                .append(ca.getGioKetThuc().format(GIO)).append("): ").append(String.join(", ", ten)).append('\n'));
        return new TraLoi(sb.toString().trim(), List.of("Lịch làm của tôi"), lk);
    }

    TraLoi lichCuaToi(NguoiDung nd) {
        if (nd == null) return null;
        LocalDate homNay = LocalDate.now(dongHo);
        List<LichLamViec> ds = new ArrayList<>(lichRepo.findByNhanVienIdAndNgayLamBetween(nd.id(), homNay, homNay.plusDays(6)));
        List<LienKet> lk = List.of(new LienKet("Lịch làm của tôi", "/lich-cua-toi"));
        if (ds.isEmpty()) return new TraLoi("7 ngày tới bạn chưa được xếp ca nào.", List.of("Ai làm ca hôm nay?"), lk);
        ds.sort(Comparator.comparing(LichLamViec::getNgayLam).thenComparing(l -> l.getCaLamViec().getGioBatDau()));
        StringBuilder sb = new StringBuilder("Lịch của bạn 7 ngày tới:\n");
        for (LichLamViec l : ds) {
            CaLamViec ca = l.getCaLamViec();
            sb.append("• ").append(thu(l.getNgayLam())).append(' ').append(l.getNgayLam().format(NGAY)).append(": ").append(ca.getTenCa())
                    .append(" (").append(ca.getGioBatDau().format(GIO)).append(" - ").append(ca.getGioKetThuc().format(GIO)).append(")\n");
        }
        return new TraLoi(sb.toString().trim(), List.of("Ca của tôi"), lk);
    }

    TraLoi caCuaToi(NguoiDung nd) {
        if (nd == null) return null;
        GiaoCa ca = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA).orElse(null);
        if (ca == null) {
            return new TraLoi("Bạn chưa nhận ca. Bấm nút \"Ca làm việc\" trên thanh trên cùng → nhập tiền đầu ca → \"Nhận ca\" để bắt đầu bán hàng.",
                    List.of("Cách giao ca", "Lịch làm của tôi"), List.of());
        }
        return new TraLoi(tomTatCa(ca), List.of("Cách giao ca"), List.of(new LienKet("Lịch sử giao ca", "/giao-ca")));
    }

    String tomTatCa(GiaoCa ca) {
        List<HoaDon> ds = hoaDonRepo.findByIdGiaoCaOrderByNgayTaoAsc(ca.getId());
        BigDecimal tong = BigDecimal.ZERO;
        int soDon = 0;
        for (HoaDon h : ds) {
            if (!Integer.valueOf(HoaDon.HOAN_THANH).equals(h.getTrangThai())) continue;
            soDon++;
            tong = tong.add(DoanhThu.cua(h));
        }
        return "Bạn đang trong ca " + ca.getMaGiaoCa() + " từ " + ca.getThoiGianNhanCa().format(GIO) + " " + ca.getThoiGianNhanCa().format(NGAY)
                + ": " + soDon + " đơn hoàn thành, doanh thu " + tien(tong) + ", tiền đầu ca " + tien(ca.getTienDauCa()) + ".";
    }

    private static String thu(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? "Chủ nhật" : "Thứ " + (d.getDayOfWeek().getValue() + 1);
    }

    /* ===================== Hướng dẫn thao tác ===================== */

    private record HuongDan(String[] tuKhoa, boolean chiQuanLy, String noiDung, LienKet lienKet) {
    }

    private static final List<HuongDan> HUONG_DAN = List.of(
            new HuongDan(new String[] {"ban tai quay", "ban hang", "tao hoa don", "thanh toan tai quay", "pos"}, false,
                    "Bán hàng tại quầy:\n1. Nhận ca trước (nút \"Ca làm việc\").\n2. Vào Bán hàng tại quầy, mỗi khách một tab hóa đơn (\"+ Hóa đơn mới\").\n"
                            + "3. Tìm sản phẩm (F2) hoặc quét mã / camera để thêm vào giỏ.\n4. Chọn khách (bỏ trống = khách lẻ), hệ thống gợi ý phiếu giảm giá tốt nhất.\n"
                            + "5. Chọn tiền mặt / chuyển khoản / kết hợp, nhập tiền khách đưa rồi bấm Thanh toán (F9).",
                    new LienKet("Bán hàng tại quầy", "/ban-hang")),
            new HuongDan(new String[] {"giao ca", "ket ca", "nhan ca", "dong ca", "mo ca", "ban giao"}, false,
                    "Nhận / giao ca:\n• Nhận ca: nút \"Ca làm việc\" → nhập tiền đầu ca → Nhận ca.\n"
                            + "• Giao ca: nút \"Ca làm việc\" → xem tổng kết (số đơn, doanh thu, tiền mặt hệ thống) → đếm và nhập tiền mặt thực tế "
                            + "→ ghi chú nếu lệch → chọn người nhận bàn giao → Giao ca. Biên bản giao ca in được ngay.",
                    new LienKet("Lịch sử giao ca", "/giao-ca")),
            new HuongDan(new String[] {"huy don", "doi trang thai", "xac nhan don", "giao that bai", "giao lai"}, false,
                    "Đổi trạng thái hóa đơn: mở chi tiết hóa đơn, các nút chuyển trạng thái chỉ hiện đúng bước được phép. "
                            + "Hủy đơn và Giao hàng thất bại bắt buộc ghi lý do; hủy thì hàng tự cộng lại tồn kho. Đơn Đã hoàn thành / Đã hủy không chuyển tiếp được.",
                    new LienKet("Danh sách hóa đơn", "/hoa-don")),
            new HuongDan(new String[] {"them khach", "tao khach", "khach hang moi"}, false,
                    "Thêm khách hàng: Quản lý khách hàng → Thêm khách hàng. SĐT 10 số bắt đầu bằng 0, email và SĐT không trùng; ngày sinh nếu nhập thì phải đủ 15 tuổi. "
                            + "Ở màn bán hàng có thể thêm nhanh khách bằng nút cạnh ô tìm khách.",
                    new LienKet("Thêm khách hàng", "/khach-hang/them")),
            new HuongDan(new String[] {"nhap excel lich", "excel lich", "xep lich", "sao chep tuan"}, true,
                    "Xếp lịch: Lịch làm việc → Xếp lịch làm việc. Bấm \"+\" ở ô để xếp một người, \"Xếp lịch nhanh\" cho nhiều người nhiều ngày, "
                            + "\"Sao chép tuần trước\" để lặp lịch. \"Xuất Excel\" / \"Nhập Excel\" để làm lịch trên Excel; sai một dòng thì cả file bị từ chối.",
                    new LienKet("Xếp lịch làm việc", "/lich-lam-viec")),
            new HuongDan(new String[] {"nhap excel nhan vien", "them nhan vien", "import nhan vien"}, true,
                    "Thêm nhân viên: Quản lý nhân viên → Thêm (có quét CCCD) hoặc Nhập Excel theo file mẫu. Hệ thống tự tạo mật khẩu tạm và gửi mail tài khoản.",
                    new LienKet("Quản lý nhân viên", "/nhan-vien")),
            new HuongDan(new String[] {"phieu giam gia", "tao phieu", "voucher ca nhan", "tang phieu"}, true,
                    "Tạo phiếu giảm giá: Quản lý giảm giá → Phiếu giảm giá → Thêm. Phiếu Cá nhân: chọn khách (lọc theo tổng đơn, sinh nhật...), "
                            + "số lượng tự bằng số khách và hệ thống gửi mail mã phiếu cho từng khách.",
                    new LienKet("Phiếu giảm giá", "/phieu-giam-gia")),
            new HuongDan(new String[] {"doi mat khau", "quen mat khau"}, false,
                    "Đổi mật khẩu: bấm tên của bạn ở góc phải trên cùng → Đổi mật khẩu. Quên mật khẩu thì bấm \"Quên mật khẩu\" ở trang đăng nhập.",
                    null),
            new HuongDan(new String[] {"chat khach", "ho tro khach", "tra loi khach", "gap nhan vien"}, false,
                    "Hỗ trợ khách: Quản lý chat → Hỗ trợ khách hàng. Khách bấm \"Gặp nhân viên\" sẽ nằm ở mục Chờ nhân viên; mở cuộc chat và trả lời để nhận hỗ trợ.",
                    new LienKet("Hỗ trợ khách hàng", "/ho-tro-truc-tuyen")));

    TraLoi huongDan(String t, NguoiDung nd) {
        if (!TroLyAoService.coMot(t, "cach", "huong dan", "lam sao", "lam the nao", "the nao", "o dau", "nhu the nao")) return null;
        boolean ql = nd != null && nd.laQuanLy();
        for (HuongDan h : HUONG_DAN) {
            if (!TroLyAoService.coMot(t, h.tuKhoa())) continue;
            if (h.chiQuanLy() && !ql) return new TraLoi("Thao tác này chỉ Quản lý làm được. Bạn nhờ Quản lý hỗ trợ nhé.", goiY(nd), List.of());
            return new TraLoi(h.noiDung(), goiY(nd), h.lienKet() == null ? List.of() : List.of(h.lienKet()));
        }
        return null;
    }

    /* ===================== Tiện ích ===================== */

    static List<String> goiY(NguoiDung nd) {
        return nd != null && nd.laQuanLy() ? GOI_Y_QUAN_LY : GOI_Y_NHAN_VIEN;
    }

    static String tien(BigDecimal v) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(v == null ? 0 : v.longValue()) + " ₫";
    }
}
