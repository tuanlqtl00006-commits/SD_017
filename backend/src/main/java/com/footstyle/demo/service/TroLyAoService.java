package com.footstyle.demo.service;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trợ lý ảo (Chat AI) trả lời khách TỪ DỮ LIỆU THẬT của cửa hàng, KHÔNG gọi dịch vụ AI bên ngoài (không cần API key, không tốn phí).
 * Hiểu tiếng Việt có dấu hoặc không dấu. Nhận biết theo thứ tự:
 *  1. muốn gặp nhân viên -> chuyển cho nhân viên;
 *  2. tra cứu đơn (mã HDxx + SĐT đúng của đơn, không lộ đơn của người khác);
 *  3. khuyến mãi (phiếu giảm giá công khai đang chạy, đợt giảm giá);
 *  4. câu hỏi thường gặp (giao hàng, đổi trả, thanh toán, giờ mở cửa, địa chỉ, căng cước);
 *  5. tìm sản phẩm theo thương hiệu, loại, trọng lượng (3U/4U...), khoảng giá, lối chơi (tấn công / phòng thủ / người mới), tên sản phẩm;
 *  6. lời chào; không hiểu thì gợi ý câu hỏi và mời gặp nhân viên.
 */
@Service
public class TroLyAoService {

    /** Câu trả lời: nội dung + có chuyển cho nhân viên không + các câu gợi ý bấm nhanh. */
    public record TraLoi(String noiDung, boolean chuyenNhanVien, List<String> goiY) {
    }

    /** Thông tin cuộc chat để trả lời đúng người (tên, SĐT khách đã nhập khi bắt đầu chat). */
    public record NguCanh(String tenKhach, String sdtKhach) {
    }

    public static final List<String> GOI_Y_MAC_DINH = List.of("Vợt dưới 3 triệu", "Vợt tấn công", "Khuyến mãi hôm nay",
            "Kiểm tra đơn hàng", "Gặp nhân viên");
    static final int DO_DAI_TOI_DA = 1900;
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SanPhamChiTietRepository spctRepo;
    private final GiamGiaApDungService giamGiaService;
    private final PhieuGiamGiaRepository phieuRepo;
    private final DotGiamGiaRepository dotRepo;
    private final HoaDonRepository hoaDonRepo;
    private final String tenCuaHang;
    private final String diaChi;
    private final String hotline;
    private final String gioMoCua;
    private Clock dongHo = Clock.systemDefaultZone();

    public TroLyAoService(SanPhamChiTietRepository spctRepo, GiamGiaApDungService giamGiaService, PhieuGiamGiaRepository phieuRepo,
                          DotGiamGiaRepository dotRepo, HoaDonRepository hoaDonRepo,
                          @Value("${app.cua-hang.ten:FootStyle}") String tenCuaHang,
                          @Value("${app.cua-hang.dia-chi:}") String diaChi,
                          @Value("${app.cua-hang.hotline:}") String hotline,
                          @Value("${app.cua-hang.gio-mo-cua:}") String gioMoCua) {
        this.spctRepo = spctRepo;
        this.giamGiaService = giamGiaService;
        this.phieuRepo = phieuRepo;
        this.dotRepo = dotRepo;
        this.hoaDonRepo = hoaDonRepo;
        this.tenCuaHang = tenCuaHang;
        this.diaChi = diaChi;
        this.hotline = hotline;
        this.gioMoCua = gioMoCua;
    }

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    /** Lời chào khi khách bắt đầu chat. */
    public TraLoi loiChao(String tenKhach) {
        String ten = tenKhach == null || tenKhach.isBlank() ? "bạn" : tenKhach.trim();
        return new TraLoi("Xin chào " + ten + "! Mình là trợ lý ảo của " + tenCuaHang + ". Mình có thể giúp bạn:\n"
                + "• Tìm vợt, phụ kiện theo hãng, giá, lối chơi (vd: \"vợt Yonex dưới 3 triệu\", \"vợt tấn công 4U\")\n"
                + "• Xem khuyến mãi, mã giảm giá đang có\n"
                + "• Kiểm tra đơn hàng (gửi mã đơn, vd: HD12)\n"
                + "• Giải đáp giao hàng, đổi trả, thanh toán\n"
                + "Cần nói chuyện với nhân viên thì bấm \"Gặp nhân viên\" nhé.", false, GOI_Y_MAC_DINH);
    }

    @Transactional(readOnly = true)
    public TraLoi traLoi(String cauHoi, NguCanh nc) {
        String t = " " + BanHangService.boDau(cauHoi).replaceAll("[^a-z0-9.,/\\- ]", " ").replaceAll("\\s+", " ").trim() + " ";
        if (t.isBlank()) return new TraLoi("Bạn cần mình giúp gì ạ?", false, GOI_Y_MAC_DINH);

        if (coMot(t, "nhan vien", "tu van vien", "nguoi that", "gap nguoi", "noi chuyen voi nguoi", "goi lai cho toi", "tu van truc tiep")) {
            return new TraLoi("Mình đã chuyển cuộc trò chuyện cho nhân viên. Bạn vui lòng đợi trong giây lát, nhân viên sẽ trả lời ngay tại đây.",
                    true, List.of());
        }
        TraLoi don = traCuuDon(t, nc);
        if (don != null) return cat(don);
        boolean nhacSanPham = coMot(t, " vot", "san pham", "qua cau", "ong cau", " balo", " tui ", " ao ", " quan ", "phu kien");
        if (!nhacSanPham && coMot(t, "khuyen mai", "giam gia", "voucher", "ma giam", " sale ", "uu dai", "phieu giam", "dang giam")) {
            return cat(khuyenMai());
        }
        TraLoi hoi = cauHoiThuongGap(t);
        if (hoi != null) return hoi;
        TraLoi sp = timSanPham(t);
        if (sp != null) return cat(sp);
        if (coMot(t, " xin chao ", " chao ", " hello ", " hi ", " alo ", " shop oi ")) return loiChao(nc == null ? null : nc.tenKhach());
        if (coMot(t, " cam on ", " thanks ", " thank ", " ok ", " oke ")) {
            return new TraLoi("Cảm ơn bạn đã quan tâm " + tenCuaHang + "! Bạn cần hỗ trợ thêm gì cứ nhắn mình nhé.", false, GOI_Y_MAC_DINH);
        }
        return new TraLoi("Xin lỗi, mình chưa hiểu câu hỏi. Bạn thử hỏi theo các gợi ý bên dưới, hoặc bấm \"Gặp nhân viên\" để được tư vấn trực tiếp.",
                false, GOI_Y_MAC_DINH);
    }

    /* ===================== Tra cứu đơn ===================== */

    static final Pattern MA_DON = Pattern.compile("\\bhd\\s*-?\\s*(\\d{1,9})\\b");
    static final Pattern SDT = Pattern.compile("\\b(0[35789]\\d{8})\\b");

    TraLoi traCuuDon(String t, NguCanh nc) {
        Matcher m = MA_DON.matcher(t);
        boolean hoiDon = coMot(t, "don hang", "kiem tra don", "tra don", "don cua toi", "tinh trang don", "theo doi don", "ma don", "van don");
        if (!m.find()) {
            return hoiDon ? new TraLoi("Bạn gửi giúp mình mã đơn hàng (vd: HD12) để mình kiểm tra nhé. Mã đơn có trong tin nhắn / hóa đơn khi đặt hàng.",
                    false, List.of()) : null;
        }
        String maGo = "HD" + m.group(1);
        String ma = MaUtil.dinhDang("HD", Long.parseLong(m.group(1)));
        Matcher ms = SDT.matcher(t);
        String sdt = ms.find() ? ms.group(1) : nc == null ? null : nc.sdtKhach();
        Optional<HoaDon> hd = hoaDonRepo.findFirstByMaHoaDonIgnoreCase(maGo);
        if (hd.isEmpty() && !ma.equals(maGo)) hd = hoaDonRepo.findFirstByMaHoaDonIgnoreCase(ma);
        if (hd.isEmpty() || sdt == null || !dungSdt(hd.get(), sdt)) {
            // Không nói rõ là sai mã hay sai SĐT để người khác không dò được đơn
            return new TraLoi("Mình không tìm thấy đơn " + ma + " khớp với số điện thoại " + (sdt == null ? "của bạn" : an(sdt))
                    + ". Bạn kiểm tra lại mã đơn và số điện thoại đặt hàng, hoặc bấm \"Gặp nhân viên\" để được hỗ trợ.", false, List.of("Gặp nhân viên"));
        }
        HoaDon h = hd.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Đơn ").append(h.getMaHoaDon()).append(" (")
                .append(Integer.valueOf(HoaDon.GIAO_HANG).equals(h.getLoaiDon()) ? "đặt online" : "mua tại quầy").append("):\n");
        sb.append("• Trạng thái: ").append(HoaDonLuongTrangThai.ten(h.getTrangThai())).append('\n');
        if (h.getNgayTao() != null) sb.append("• Ngày đặt: ").append(h.getNgayTao().format(NGAY)).append('\n');
        sb.append("• Tổng thanh toán: ").append(tien(h.getThanhTien() == null ? 0 : h.getThanhTien().longValue())).append('\n');
        if (Integer.valueOf(HoaDon.GIAO_HANG).equals(h.getLoaiDon()) && h.getDonViVanChuyen() != null) {
            sb.append("• Đơn vị vận chuyển: ").append(h.getDonViVanChuyen()).append('\n');
        }
        if (h.getNgayGiaoDuKien() != null && Integer.valueOf(HoaDon.DANG_GIAO).equals(h.getTrangThai())) {
            sb.append("• Dự kiến giao: ").append(h.getNgayGiaoDuKien().format(NGAY)).append('\n');
        }
        sb.append("Cần hỗ trợ thêm về đơn này, bạn bấm \"Gặp nhân viên\" nhé.");
        return new TraLoi(sb.toString(), false, List.of("Gặp nhân viên"));
    }

    private static boolean dungSdt(HoaDon h, String sdt) {
        KhachHang kh = h.getKhachHang();
        return (kh != null && sdt.equals(kh.getSdt())) || sdt.equals(h.getSdtNguoiNhan());
    }

    /** 0912345678 -> 0912***678 */
    static String an(String sdt) {
        return sdt.length() < 7 ? sdt : sdt.substring(0, 4) + "***" + sdt.substring(sdt.length() - 3);
    }

    /* ===================== Khuyến mãi ===================== */

    TraLoi khuyenMai() {
        LocalDateTime now = LocalDateTime.now(dongHo);
        List<String> dong = new ArrayList<>();
        for (PhieuGiamGia p : phieuRepo.findAll()) {
            if (!Integer.valueOf(PhieuGiamGia.DOI_TUONG_TAT_CA).equals(p.getDoiTuongApDung())) continue; // phiếu cá nhân: không công khai
            if (PhieuApDung.lyDoKhongDung(p, Long.MAX_VALUE / 4, now, false, false, 0) != null) continue;
            String giam = Integer.valueOf(PhieuGiamGia.LOAI_PHAN_TRAM).equals(p.getLoaiGiam())
                    ? "giảm " + p.getGiaTriGiam() + "%" + (p.getGiamToiDa() != null ? " (tối đa " + tien(p.getGiamToiDa()) + ")" : "")
                    : "giảm " + tien(p.getGiaTriGiam() == null ? 0 : p.getGiaTriGiam());
            String dk = p.getGiaTriDonHangToiThieu() != null && p.getGiaTriDonHangToiThieu() > 0
                    ? " cho đơn từ " + tien(p.getGiaTriDonHangToiThieu()) : "";
            dong.add("• Mã " + p.getMaPhieu() + ": " + giam + dk + (p.getNgayKetThuc() != null ? ", đến " + p.getNgayKetThuc().format(NGAY) : ""));
            if (dong.size() >= 5) break;
        }
        List<String> dot = new ArrayList<>();
        for (DotGiamGia d : dotRepo.findAll()) {
            if (!Integer.valueOf(1).equals(d.getTrangThai()) || d.getNgayBatDau() == null || d.getNgayKetThuc() == null) continue;
            if (now.isBefore(d.getNgayBatDau()) || now.isAfter(d.getNgayKetThuc())) continue;
            dot.add("• " + d.getTenDot() + ": giảm đến " + d.getPhanTramGiamDot() + "%, đến " + d.getNgayKetThuc().format(NGAY));
            if (dot.size() >= 5) break;
        }
        if (dong.isEmpty() && dot.isEmpty()) {
            return new TraLoi("Hiện cửa hàng chưa có chương trình khuyến mãi công khai nào. Bạn theo dõi thêm nhé!", false, List.of("Vợt dưới 3 triệu"));
        }
        StringBuilder sb = new StringBuilder();
        if (!dong.isEmpty()) sb.append("Mã giảm giá đang dùng được (nhập khi thanh toán, mỗi đơn 1 mã):\n").append(String.join("\n", dong));
        if (!dot.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Đợt giảm giá đang chạy (giá đã giảm sẵn trên sản phẩm):\n").append(String.join("\n", dot));
        }
        return new TraLoi(sb.toString(), false, List.of("Vợt đang giảm giá", "Vợt dưới 3 triệu"));
    }

    /* ===================== Câu hỏi thường gặp ===================== */

    TraLoi cauHoiThuongGap(String t) {
        if (coMot(t, "dia chi", "o dau", "cua hang nam", "chi nhanh", "den xem", "showroom", "hotline", "so dien thoai cua hang", "lien he")) {
            return new TraLoi("Cửa hàng " + tenCuaHang + ": " + diaChi + ".\nGiờ mở cửa: " + gioMoCua + ".\nHotline: " + hotline + ".",
                    false, List.of("Giờ mở cửa"));
        }
        if (coMot(t, "gio mo cua", "may gio", "mo cua", "dong cua", "lam viec den")) {
            return new TraLoi("Cửa hàng mở cửa " + gioMoCua + ". Bạn có thể nhắn tin ở đây bất cứ lúc nào nhé.", false, List.of("Địa chỉ cửa hàng"));
        }
        if (coMot(t, "giao hang", " ship", "van chuyen", "bao lau nhan", "phi ship", "giao toi", "giao den")) {
            return new TraLoi("Cửa hàng giao hàng toàn quốc qua Giao Hàng Nhanh, Giao Hàng Tiết Kiệm, Viettel Post:\n"
                    + "• Nội thành Hà Nội 1-2 ngày, tỉnh khác 3-5 ngày\n"
                    + "• Phí 30.000 - 35.000 ₫ tùy đơn vị; nhiều chương trình hỗ trợ phí ship (xem \"Khuyến mãi hôm nay\")\n"
                    + "• Được kiểm tra hàng trước khi nhận, thanh toán khi nhận hàng (COD)", false, List.of("Khuyến mãi hôm nay", "Đổi trả"));
        }
        if (coMot(t, "doi tra", "doi hang", "tra hang", "hoan tien", "bao hanh", "loi san pham", "bi loi")) {
            return new TraLoi("Chính sách đổi trả - bảo hành:\n"
                    + "• Đổi sản phẩm trong 7 ngày nếu lỗi do nhà sản xuất, còn nguyên tem mác, hóa đơn\n"
                    + "• Vợt chính hãng bảo hành khung theo chính sách của hãng\n"
                    + "• Không đổi trả sản phẩm đã qua sử dụng không do lỗi nhà sản xuất\n"
                    + "Cần đổi trả, bạn bấm \"Gặp nhân viên\" và gửi mã đơn để được hỗ trợ.", false, List.of("Gặp nhân viên"));
        }
        if (coMot(t, "thanh toan", "chuyen khoan", " cod ", "tra gop", "the ngan hang", "vnpay")) {
            return new TraLoi("Cửa hàng nhận thanh toán bằng tiền mặt, chuyển khoản (quét mã QR tại quầy), ví VNPay, và thanh toán khi nhận hàng (COD) với đơn online.",
                    false, GOI_Y_MAC_DINH);
        }
        if (coMot(t, "cang day", "cang cuoc", "dan vot", "bao nhieu kg", "may kg", "luc cang")) {
            return new TraLoi("Gợi ý mức căng dây:\n• Người mới chơi: 9 - 10 kg\n• Chơi phong trào: 10 - 11 kg\n• Chơi lâu năm / thi đấu: 11 - 13 kg\n"
                    + "Căng càng cao cầu đi càng chính xác nhưng cần lực tay tốt hơn. Cửa hàng nhận đan vợt tại quầy.", false, List.of("Vợt cho người mới"));
        }
        return null;
    }

    /* ===================== Tìm sản phẩm ===================== */

    /** Bộ lọc đọc được từ câu hỏi. nhomDanhMuc là mã NhomDanhMuc (VOT, QUA_CAU...) hoặc "GIAY" (cửa hàng chưa bán giày). */
    record BoLoc(String thuongHieu, String nhomDanhMuc, String trongLuong, Long giaTu, Long giaDen, String diemCanBang,
                 Set<String> doCung, boolean dangGiam) {
        boolean coLoc() {
            return thuongHieu != null || nhomDanhMuc != null || trongLuong != null || giaTu != null || giaDen != null
                    || diemCanBang != null || !doCung.isEmpty() || dangGiam;
        }
    }

    /** Từ chung / từ tiếng Việt hay gặp trong câu hỏi - không dùng để đoán tên sản phẩm. */
    private static final Set<String> TU_CHUNG = Set.of("vot", "cau", "long", "pro", "ao", "quan", "tui", "balo", "giay", "the", "thao",
            "tan", "cong", "thu", "phong", "nhe", "nang", "dau", "can", "bang", "gia", "duoi", "tren", "moi", "nguoi", "choi", "mau",
            "den", "trang", "do", "xanh", "hang", "con", "khong", "co", "mua", "ban", "re", "tot", "dep", "sale", "giam", "ma", "cho",
            "va", "voi", "nam", "nu", "size", "phu", "kien", "qua", "ong", "hop", "loai", "mem", "cung", "trung", "binh", "tre", "em",
            "toi", "minh", "shop", "nao", "gi", "bao", "nhieu", "tien", "chiec", "cai", "doi", "thanh", "danh", "tap", "de", "game");

    TraLoi timSanPham(String t) {
        List<SanPhamChiTiet> ds = spctRepo.findAll().stream().filter(BanHangService::dangBan).toList();
        Set<String> thuongHieus = new LinkedHashSet<>();
        Map<Integer, List<SanPhamChiTiet>> theoSp = new LinkedHashMap<>();
        for (SanPhamChiTiet c : ds) {
            if (c.getSanPham().getThuongHieu() != null) thuongHieus.add(c.getSanPham().getThuongHieu().getTen());
            theoSp.computeIfAbsent(c.getSanPham().getId(), k -> new ArrayList<>()).add(c);
        }
        BoLoc loc = docBoLoc(t, thuongHieus);
        boolean hoiSanPham = coMot(t, " vot", "san pham", " mua ", " gia ", "con hang", "goi y", " tu van", "nen mua", "loai nao",
                "bao nhieu tien", "phu kien", " giay", " balo", " tui ", "qua cau", "ong cau", " ao ", " quan ");

        // Khách gõ tên mẫu (vd "astrox 99", "nanoflare 800"): khớp đúng 1 mẫu thì trả chi tiết, nhiều mẫu thì lọc theo tên
        Map<Integer, Integer> diemTen = new LinkedHashMap<>();
        int caoNhat = 0;
        for (List<SanPhamChiTiet> nhom : theoSp.values()) {
            SanPham sp = nhom.get(0).getSanPham();
            int d = diemTen(t, sp.getTenSanPham(), sp.getThuongHieu() == null ? null : sp.getThuongHieu().getTen());
            if (d > 0) diemTen.put(sp.getId(), d);
            caoNhat = Math.max(caoNhat, d);
        }
        Map<Long, Double> giam = giamGiaService.phanTramTheoBienThe(LocalDateTime.now(dongHo));
        final int max = caoNhat;
        List<Integer> trungNhat = diemTen.entrySet().stream().filter(e -> e.getValue() == max).map(Map.Entry::getKey).toList();
        if (max >= 2 && trungNhat.size() == 1) return chiTietSanPham(theoSp.get(trungNhat.get(0)), giam);
        if (!loc.coLoc() && !hoiSanPham && diemTen.isEmpty()) return null;

        List<GoiYSp> ketQua = new ArrayList<>();
        for (List<SanPhamChiTiet> nhom : theoSp.values()) {
            SanPham sp = nhom.get(0).getSanPham();
            if (!diemTen.isEmpty() && diemTen.getOrDefault(sp.getId(), 0) < max) continue;
            if (loc.thuongHieu() != null && (sp.getThuongHieu() == null || !loc.thuongHieu().equals(sp.getThuongHieu().getTen()))) continue;
            if (loc.nhomDanhMuc() != null && (sp.getDanhMuc() == null || !loc.nhomDanhMuc().equals(NhomDanhMuc.tuTen(sp.getDanhMuc().getTen())))) continue;
            if (loc.diemCanBang() != null && (sp.getDiemCanBang() == null
                    || !BanHangService.boDau(sp.getDiemCanBang().getTen()).equals(loc.diemCanBang()))) continue;
            if (!loc.doCung().isEmpty() && (sp.getDoCung() == null || !loc.doCung().contains(BanHangService.boDau(sp.getDoCung().getTen())))) continue;
            long min = Long.MAX_VALUE;
            long lon = 0;
            double ptMax = 0;
            int ton = 0;
            Set<String> phanLoai = new LinkedHashSet<>();
            for (SanPhamChiTiet c : nhom) {
                if (loc.trongLuong() != null && (c.getTrongLuong() == null
                        || !BanHangService.boDau(c.getTrongLuong().getTen()).startsWith(loc.trongLuong()))) continue;
                double pt = giam.getOrDefault(c.getId().longValue(), 0.0);
                long gia = PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), pt);
                if (loc.giaTu() != null && gia < loc.giaTu()) continue;
                if (loc.giaDen() != null && gia > loc.giaDen()) continue;
                if (loc.dangGiam() && pt <= 0) continue;
                int sl = c.getSoLuongTon() == null ? 0 : c.getSoLuongTon();
                if (sl <= 0) continue;
                ton += sl;
                min = Math.min(min, gia);
                lon = Math.max(lon, gia);
                ptMax = Math.max(ptMax, pt);
                phanLoai.add(BanHangService.phanLoai(c));
            }
            if (ton == 0) continue;
            ketQua.add(new GoiYSp(sp, min, lon, ptMax, new ArrayList<>(phanLoai)));
        }
        if (ketQua.isEmpty()) {
            return new TraLoi("Hiện chưa có sản phẩm còn hàng phù hợp (" + moTaLoc(loc)
                    + "). Bạn thử nới khoảng giá hoặc hỏi hãng khác, hoặc bấm \"Gặp nhân viên\" để được tư vấn.", false, GOI_Y_MAC_DINH);
        }
        ketQua.sort(loc.dangGiam() ? Comparator.comparingDouble(GoiYSp::phanTram).reversed() : Comparator.<GoiYSp>comparingLong(GoiYSp::giaThapNhat));
        StringBuilder sb = new StringBuilder();
        sb.append(loc.coLoc() ? "Sản phẩm còn hàng phù hợp (" + moTaLoc(loc) + "):\n" : "Một số sản phẩm đang có tại cửa hàng:\n");
        for (int i = 0; i < Math.min(5, ketQua.size()); i++) {
            GoiYSp g = ketQua.get(i);
            SanPham sp = g.sp();
            sb.append("• ").append(sp.getTenSanPham());
            List<String> thuocTinh = new ArrayList<>();
            if (sp.getThuongHieu() != null) thuocTinh.add(sp.getThuongHieu().getTen());
            if (sp.getDiemCanBang() != null && !NhomDanhMuc.laKhongApDung(sp.getDiemCanBang().getTen())) thuocTinh.add(sp.getDiemCanBang().getTen().toLowerCase(java.util.Locale.ROOT));
            if (!thuocTinh.isEmpty()) sb.append(" (").append(String.join(", ", thuocTinh)).append(')');
            sb.append(": ").append(g.giaThapNhat() == g.giaCaoNhat() ? tien(g.giaThapNhat()) : tien(g.giaThapNhat()) + " - " + tien(g.giaCaoNhat()));
            if (g.phanTram() > 0) sb.append(" (đang giảm ").append(phanTram(g.phanTram())).append(')');
            List<String> pl = g.phanLoai().stream().filter(x -> !x.isBlank()).toList();
            if (!pl.isEmpty()) {
                sb.append(" · ").append(String.join(", ", pl.subList(0, Math.min(3, pl.size()))));
                if (pl.size() > 3) sb.append("…");
            }
            sb.append('\n');
        }
        if (ketQua.size() > 5) sb.append("…và ").append(ketQua.size() - 5).append(" sản phẩm khác.\n");
        sb.append("Bạn muốn xem kỹ mẫu nào cứ gõ tên mẫu đó nhé.");
        return new TraLoi(sb.toString(), false, List.of("Khuyến mãi hôm nay", "Gặp nhân viên"));
    }

    /** Một sản phẩm gợi ý: khoảng giá (sau đợt giảm) của các biến thể còn hàng khớp bộ lọc. */
    record GoiYSp(SanPham sp, long giaThapNhat, long giaCaoNhat, double phanTram, List<String> phanLoai) {
    }

    private TraLoi chiTietSanPham(List<SanPhamChiTiet> nhom, Map<Long, Double> giam) {
        SanPham sp = nhom.get(0).getSanPham();
        StringBuilder sb = new StringBuilder(sp.getTenSanPham());
        List<String> tt = new ArrayList<>();
        if (sp.getThuongHieu() != null) tt.add("hãng " + sp.getThuongHieu().getTen());
        if (sp.getDiemCanBang() != null && !NhomDanhMuc.laKhongApDung(sp.getDiemCanBang().getTen())) tt.add(sp.getDiemCanBang().getTen().toLowerCase(java.util.Locale.ROOT));
        if (sp.getDoCung() != null && !NhomDanhMuc.laKhongApDung(sp.getDoCung().getTen())) tt.add("thân " + sp.getDoCung().getTen().toLowerCase(java.util.Locale.ROOT));
        if (!tt.isEmpty()) sb.append(" (").append(String.join(", ", tt)).append(')');
        sb.append(":\n");
        boolean conHang = false;
        int dem = 0;
        for (SanPhamChiTiet c : nhom) {
            if (dem++ >= 8) {
                sb.append("• …và ").append(nhom.size() - 8).append(" phân loại khác\n");
                break;
            }
            double pt = giam.getOrDefault(c.getId().longValue(), 0.0);
            long goc = c.getGiaBan() == null ? 0 : c.getGiaBan();
            long gia = PhieuApDung.giaSauDot(goc, pt);
            int ton = c.getSoLuongTon() == null ? 0 : c.getSoLuongTon();
            conHang |= ton > 0;
            String pl = BanHangService.phanLoai(c);
            sb.append("• ").append(pl.isBlank() ? c.getMaSpct() : pl).append(": ").append(tien(gia));
            if (pt > 0) sb.append(" (giảm ").append(phanTram(pt)).append(" từ ").append(tien(goc)).append(')');
            sb.append(ton > 0 ? (ton <= 3 ? " - chỉ còn " + ton : " - còn hàng") : " - hết hàng").append('\n');
        }
        if (sp.getMoTa() != null && !sp.getMoTa().isBlank()) {
            String moTa = sp.getMoTa().trim();
            sb.append(moTa.length() > 300 ? moTa.substring(0, 300) + "…" : moTa).append('\n');
        }
        sb.append(conHang ? "Bạn muốn đặt mẫu này hay cần tư vấn thêm thì bấm \"Gặp nhân viên\" nhé."
                : "Mẫu này đang tạm hết hàng, bạn để lại lời nhắn cho nhân viên nhé.");
        return new TraLoi(sb.toString(), false, List.of("Gặp nhân viên", "Khuyến mãi hôm nay"));
    }

    /**
     * Điểm khớp tên sản phẩm: mỗi từ đặc trưng của tên (bỏ từ chung, tên hãng, trọng lượng 4U, chu vi G5) có trong câu được
     * 1 điểm, từ có chữ số (99, 800) được 2 điểm.
     */
    static int diemTen(String t, String tenSanPham, String thuongHieu) {
        String hang = thuongHieu == null ? "" : BanHangService.boDau(thuongHieu);
        int diem = 0;
        Set<String> daTinh = new LinkedHashSet<>();
        for (String tu : BanHangService.boDau(tenSanPham).split("[^a-z0-9]+")) {
            if (tu.length() < 2 || TU_CHUNG.contains(tu) || tu.equals(hang) || tu.matches("\\d?u|g\\d") || !daTinh.add(tu)) continue;
            if (t.contains(" " + tu + " ")) diem += tu.chars().anyMatch(Character::isDigit) ? 2 : 1;
        }
        return diem;
    }

    static BoLoc docBoLoc(String t, Set<String> thuongHieus) {
        String hang = null;
        for (String th : thuongHieus) {
            String k = BanHangService.boDau(th);
            if (!k.isEmpty() && t.contains(" " + k + " ")) {
                hang = th;
                break;
            }
        }
        String nhom = null;
        if (coMot(t, "qua cau", "ong cau", "hop cau")) nhom = NhomDanhMuc.QUA_CAU;
        else if (coMot(t, " vot")) nhom = NhomDanhMuc.VOT;
        else if (coMot(t, " giay ")) nhom = "GIAY";
        else if (coMot(t, " balo", " tui ", "bao vot")) nhom = NhomDanhMuc.TUI_BALO;
        else if (coMot(t, " ao ", " quan ", "quan ao")) nhom = NhomDanhMuc.QUAN_AO;
        else if (coMot(t, "phu kien", "quan can", " cuoc ", "bang chan", "bang co tay")) nhom = NhomDanhMuc.PHU_KIEN;
        Matcher mu = Pattern.compile("(?<![a-z0-9])([2-6])\\s?u(?![a-z0-9])").matcher(t);
        String tl = mu.find() ? mu.group(1) + "u" : null;
        String dcb = null;
        if (coMot(t, "tan cong", "dap cau", "dap manh", "nang dau", "danh cong", "smash")) dcb = "nang dau";
        else if (coMot(t, "phong thu", "nhe dau", "toc do", "phan tac", "danh doi", "chan cau", "danh doi")) dcb = "nhe dau";
        else if (coMot(t, "can bang", "toan dien", "cong thu")) dcb = "can bang";
        Set<String> doCung = new LinkedHashSet<>();
        if (coMot(t, "nguoi moi", "moi choi", "moi tap", "nhap mon", "luc tay yeu", "de choi", "tre em", "nu choi")) {
            doCung.add("mem");
            doCung.add("trung binh");
        } else if (coMot(t, "cung tay", "luc tay tot", "luc tay khoe", "chuyen nghiep", "thi dau")) {
            doCung.add("cung");
            doCung.add("rat cung");
        }
        long[] gia = khoangGia(t);
        boolean dangGiam = coMot(t, "dang giam", "giam gia", " sale ", "khuyen mai");
        return new BoLoc(hang, nhom, tl, gia == null ? null : gia[0], gia == null || gia[1] == Long.MAX_VALUE ? null : gia[1],
                dcb, doCung, dangGiam);
    }

    /** Một số tiền: số (1.5 / 1,5 / 2.000.000) + đơn vị (triệu, tr, k, nghìn, đồng) + phần lẻ kiểu "1tr5". */
    private static final String SO = "(\\d+(?:[.,]\\d+)*)\\s*(?:(trieu|tr|m|k|nghin|ngan|dong|vnd)(\\d)?\\b)?(?![a-z0-9])";
    private static final Pattern GIA_KHOANG = Pattern.compile("(?:\\btu\\s+)?" + SO + "\\s*(?:-|den|toi)\\s*" + SO);
    private static final Pattern GIA_DUOI = Pattern.compile("\\b(?:duoi|nho hon|it hon|toi da|khong qua|re hon|khong vuot qua)\\s+" + SO);
    private static final Pattern GIA_TREN = Pattern.compile("\\b(?:tren|lon hon|hon|tu)\\s+" + SO);
    private static final Pattern GIA_TAM = Pattern.compile("\\b(?:khoang|tam|gan|chung|gia)\\s+" + SO);
    private static final Pattern GIA_CO_DON_VI = Pattern.compile("(?<![a-z0-9])" + SO);

    /**
     * Khoảng giá trong câu: { từ, đến } (đồng, "đến" = Long.MAX_VALUE nếu không giới hạn); không có thì null.
     * "dưới 2 triệu", "từ 1tr5 đến 3tr", "1-2 triệu", "trên 3 triệu", "khoảng 2 triệu" (±20%), "vợt 2 triệu" (±20%).
     */
    static long[] khoangGia(String t) {
        Matcher m = GIA_KHOANG.matcher(t);
        while (m.find()) {
            String dv2 = m.group(5);
            String dv1 = m.group(2) == null ? dv2 : m.group(2);
            if (dv1 == null && dv2 == null) continue; // "4 - 5" không có đơn vị: không chắc là giá
            Long a = docTien(m.group(1), dv1, m.group(3));
            Long b = docTien(m.group(4), dv2, m.group(6));
            if (a != null && b != null) return new long[] {Math.min(a, b), Math.max(a, b)};
        }
        m = GIA_DUOI.matcher(t);
        if (m.find()) {
            Long a = docTien(m.group(1), m.group(2), m.group(3));
            if (a != null) return new long[] {0, a};
        }
        m = GIA_TREN.matcher(t);
        if (m.find()) {
            Long a = docTien(m.group(1), m.group(2), m.group(3));
            if (a != null) return new long[] {a, Long.MAX_VALUE};
        }
        m = GIA_TAM.matcher(t);
        if (m.find()) {
            Long a = docTien(m.group(1), m.group(2), m.group(3));
            if (a != null) return new long[] {Math.round(a * 0.8), Math.round(a * 1.2)};
        }
        m = GIA_CO_DON_VI.matcher(t);
        while (m.find()) {
            if (m.group(2) == null || m.group(2).equals("m")) continue; // số trơn (vd "2 cây") không phải giá
            Long a = docTien(m.group(1), m.group(2), m.group(3));
            if (a != null) return new long[] {Math.round(a * 0.8), Math.round(a * 1.2)};
        }
        return null;
    }

    /** "2" + "trieu" -> 2.000.000; "1" + "tr" + "5" -> 1.500.000; "500" + "k" -> 500.000; "2" (không đơn vị, nhỏ) -> 2 triệu. */
    static Long docTien(String so, String donVi, String le) {
        if (so == null) return null;
        double v;
        try {
            String s = so;
            if (s.matches("\\d{1,3}([.,]\\d{3})+")) s = s.replaceAll("[.,]", ""); // 2.000.000 / 1,500
            else if (s.matches("\\d+[.,]\\d+")) s = s.replace(',', '.');           // 1,5 -> 1.5
            else if (!s.matches("\\d+")) return null;
            v = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
        String dv = donVi == null ? "" : donVi;
        switch (dv) {
            case "trieu", "tr", "m" -> {
                if (le != null && !so.contains(".") && !so.contains(",")) v += Integer.parseInt(le) / 10.0; // 1tr5
                return Math.round(v * 1_000_000);
            }
            case "k", "nghin", "ngan" -> {
                return Math.round(v * 1_000);
            }
            case "dong", "vnd" -> {
                return Math.round(v);
            }
            default -> {
                if (v < 100) return Math.round(v * 1_000_000); // "dưới 2" hiểu là 2 triệu
                if (v < 100_000) return Math.round(v * 1_000);  // "dưới 500" hiểu là 500 nghìn
                return Math.round(v);
            }
        }
    }

    private static String moTaLoc(BoLoc l) {
        List<String> p = new ArrayList<>();
        if (l.nhomDanhMuc() != null) {
            p.add(switch (l.nhomDanhMuc()) {
                case NhomDanhMuc.VOT -> "vợt";
                case NhomDanhMuc.QUA_CAU -> "quả cầu";
                case "GIAY" -> "giày";
                case NhomDanhMuc.TUI_BALO -> "túi / balo";
                case NhomDanhMuc.QUAN_AO -> "quần áo";
                default -> "phụ kiện";
            });
        }
        if (l.thuongHieu() != null) p.add("hãng " + l.thuongHieu());
        if (l.trongLuong() != null) p.add(l.trongLuong().toUpperCase(java.util.Locale.ROOT));
        if (l.diemCanBang() != null) {
            p.add(switch (l.diemCanBang()) {
                case "nang dau" -> "nặng đầu (tấn công)";
                case "nhe dau" -> "nhẹ đầu (phòng thủ, tốc độ)";
                default -> "cân bằng";
            });
        }
        if (l.doCung().contains("mem")) p.add("thân dẻo, dễ chơi");
        else if (!l.doCung().isEmpty()) p.add("thân cứng");
        if (l.giaTu() != null && l.giaDen() != null && l.giaTu() > 0) p.add("giá " + tien(l.giaTu()) + " - " + tien(l.giaDen()));
        else if (l.giaDen() != null) p.add("giá dưới " + tien(l.giaDen()));
        else if (l.giaTu() != null && l.giaTu() > 0) p.add("giá từ " + tien(l.giaTu()));
        if (l.dangGiam()) p.add("đang giảm giá");
        return p.isEmpty() ? "yêu cầu của bạn" : String.join(", ", p);
    }

    /* ===================== Hàm phụ ===================== */

    static boolean coMot(String t, String... tu) {
        for (String x : tu) if (t.contains(x)) return true;
        return false;
    }

    /** 10.0 -> "10%", 12.5 -> "12,5%". */
    static String phanTram(double pt) {
        long x10 = Math.round(pt * 10);
        return (x10 % 10 == 0 ? String.valueOf(x10 / 10) : (x10 / 10) + "," + (x10 % 10)) + "%";
    }

    private static String tien(long v) {
        return GiaoCaService.dinhDangTien(BigDecimal.valueOf(v));
    }

    private static TraLoi cat(TraLoi tl) {
        if (tl.noiDung().length() <= DO_DAI_TOI_DA) return tl;
        return new TraLoi(tl.noiDung().substring(0, DO_DAI_TOI_DA) + "…", tl.chuyenNhanVien(), tl.goiY());
    }
}
