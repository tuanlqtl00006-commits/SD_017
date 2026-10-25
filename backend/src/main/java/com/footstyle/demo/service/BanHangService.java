package com.footstyle.demo.service;

import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.entity.PhuongThucThanhToan;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.PhuongThucThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bán hàng tại quầy. Giao diện giữ các hóa đơn chờ (giỏ hàng) trên trình duyệt; khi thanh toán backend TÍNH LẠI TOÀN BỘ:
 *  - giá theo đợt giảm giá tại thời điểm thanh toán (khác giá đang hiện thì báo GIA_THAY_DOI để nhân viên xác nhận lại);
 *  - phiếu giảm giá (còn hạn, còn lượt, đủ đơn tối thiểu, đúng khách với phiếu cá nhân, giới hạn số lần mỗi khách);
 *  - trừ tồn kho có điều kiện (không bán quá số còn, kể cả 2 máy cùng bán);
 *  - tiền khách trả (tiền mặt + chuyển khoản) phải đủ, tiền thừa chỉ trả bằng tiền mặt.
 * Hóa đơn tạo ra ở trạng thái "Đã hoàn thành", loại "Tại quầy", gắn với ca đang mở của nhân viên (bắt buộc đã nhận ca).
 */
@Service
@RequiredArgsConstructor
public class BanHangService {

    static final int SO_DONG_TOI_DA = 100;
    static final int SO_LUONG_TOI_DA = 999;
    static final long TIEN_TOI_DA = 10_000_000_000L;

    private final SanPhamChiTietRepository spctRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final KhachHangRepository khachHangRepo;
    private final NhanVienRepository nhanVienRepo;
    private final PhieuGiamGiaRepository phieuRepo;
    private final PhieuGiamGiaKhachHangRepository phieuKhachRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final LichSuHoaDonRepository lichSuRepo;
    private final LichSuThanhToanRepository thanhToanRepo;
    private final PhuongThucThanhToanRepository phuongThucRepo;
    private final GiamGiaApDungService giamGiaService;
    private final GiaoCaService giaoCaService;

    private Clock dongHo = Clock.systemDefaultZone();

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
    }

    /* ===================== Sản phẩm ===================== */

    /** Biến thể đang bán (sản phẩm + biến thể đang hoạt động) khớp từ khóa (tên, mã, thương hiệu, màu - không phân biệt dấu). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> timSanPham(String tuKhoa, Integer gioiHan) {
        String k = boDau(tuKhoa);
        int toiDa = gioiHan == null ? 40 : Math.max(1, Math.min(100, gioiHan));
        Map<Long, Double> giam = giamGiaService.phanTramTheoBienThe(bayGio());
        Map<Integer, List<HinhAnhSanPham>> anh = anhTheoSanPham();
        List<Map<String, Object>> kq = new ArrayList<>();
        List<SanPhamChiTiet> ds = new ArrayList<>(spctRepo.findAll());
        ds.sort(Comparator.comparing((SanPhamChiTiet c) -> (c.getSoLuongTon() == null || c.getSoLuongTon() <= 0) ? 1 : 0)
                .thenComparing(c -> c.getSanPham().getTenSanPham(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SanPhamChiTiet::getMaSpct, Comparator.nullsLast(Comparator.naturalOrder())));
        for (SanPhamChiTiet c : ds) {
            if (!dangBan(c)) continue;
            if (!k.isEmpty() && !boDau(chuoiTim(c)).contains(k)) continue;
            kq.add(bienTheDto(c, giam, anh));
            if (kq.size() >= toiDa) break;
        }
        return kq;
    }

    /**
     * Bộ lọc bảng chọn sản phẩm tại quầy (cô: dạng bảng có phân trang, mới nhất trước, lọc giống trang sản phẩm + khoảng giá).
     * trang bắt đầu từ 0; giá lọc theo giá đang bán (đã trừ đợt giảm giá).
     */
    public record LocSanPham(String q, Integer idDanhMuc, Integer idThuongHieu, Integer idMauSac, Integer idTrongLuong, Integer idChuVi,
                             Long giaTu, Long giaDen, Integer trang, Integer kichThuoc) {
    }

    static final int KICH_THUOC_TRANG_MAC_DINH = 10;

    /**
     * Bảng chọn sản phẩm: chỉ biến thể đang bán VÀ còn hàng (hết hàng thì tự biến mất), mới nhất trước.
     * Trả về { items, tongSo, trang, soTrang, kichThuoc, boLoc: { danhMuc, thuongHieu, mauSac, trongLuong, chuVi } }.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> locSanPham(LocSanPham loc) {
        LocSanPham l = loc == null ? new LocSanPham(null, null, null, null, null, null, null, null, null, null) : loc;
        if (l.giaTu() != null && l.giaTu() < 0 || l.giaDen() != null && l.giaDen() < 0) throw ApiException.badRequest("Giá lọc không được âm.");
        if (l.giaTu() != null && l.giaDen() != null && l.giaTu() > l.giaDen()) throw ApiException.badRequest("Giá từ phải nhỏ hơn hoặc bằng giá đến.");
        int kt = l.kichThuoc() == null ? KICH_THUOC_TRANG_MAC_DINH : Math.max(1, Math.min(50, l.kichThuoc()));
        String k = boDau(l.q());
        Map<Long, Double> giam = giamGiaService.phanTramTheoBienThe(bayGio());
        List<SanPhamChiTiet> ds = new ArrayList<>();
        Map<String, Map<Integer, String>> boLoc = new LinkedHashMap<>();
        for (String ten : List.of("danhMuc", "thuongHieu", "mauSac", "trongLuong", "chuVi")) boLoc.put(ten, new LinkedHashMap<>());
        for (SanPhamChiTiet c : spctRepo.findAllByOrderByIdDesc()) {
            if (!dangBan(c) || c.getSoLuongTon() == null || c.getSoLuongTon() <= 0) continue;
            SanPham sp = c.getSanPham();
            if (sp.getDanhMuc() != null) boLoc.get("danhMuc").put(sp.getDanhMuc().getId(), sp.getDanhMuc().getTen());
            if (sp.getThuongHieu() != null) boLoc.get("thuongHieu").put(sp.getThuongHieu().getId(), sp.getThuongHieu().getTen());
            if (c.getMauSac() != null) boLoc.get("mauSac").put(c.getMauSac().getId(), c.getMauSac().getTen());
            if (c.getTrongLuong() != null && !NhomDanhMuc.laKhongApDung(c.getTrongLuong().getTen())) boLoc.get("trongLuong").put(c.getTrongLuong().getId(), c.getTrongLuong().getTen());
            if (c.getChuVi() != null && !NhomDanhMuc.laKhongApDung(c.getChuVi().getTen())) boLoc.get("chuVi").put(c.getChuVi().getId(), c.getChuVi().getTen());
            if (!k.isEmpty() && !boDau(chuoiTim(c)).contains(k)) continue;
            if (l.idDanhMuc() != null && (sp.getDanhMuc() == null || !l.idDanhMuc().equals(sp.getDanhMuc().getId()))) continue;
            if (l.idThuongHieu() != null && (sp.getThuongHieu() == null || !l.idThuongHieu().equals(sp.getThuongHieu().getId()))) continue;
            if (l.idMauSac() != null && (c.getMauSac() == null || !l.idMauSac().equals(c.getMauSac().getId()))) continue;
            if (l.idTrongLuong() != null && (c.getTrongLuong() == null || !l.idTrongLuong().equals(c.getTrongLuong().getId()))) continue;
            if (l.idChuVi() != null && (c.getChuVi() == null || !l.idChuVi().equals(c.getChuVi().getId()))) continue;
            long gia = PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), giam.getOrDefault(c.getId().longValue(), 0.0));
            if (l.giaTu() != null && gia < l.giaTu()) continue;
            if (l.giaDen() != null && gia > l.giaDen()) continue;
            ds.add(c);
        }
        int soTrang = Math.max(1, (ds.size() + kt - 1) / kt);
        int trang = l.trang() == null ? 0 : Math.max(0, Math.min(soTrang - 1, l.trang()));
        Map<Integer, List<HinhAnhSanPham>> anh = anhTheoSanPham();
        List<Map<String, Object>> items = new ArrayList<>();
        for (SanPhamChiTiet c : ds.subList(Math.min(ds.size(), trang * kt), Math.min(ds.size(), trang * kt + kt))) {
            Map<String, Object> m = bienTheDto(c, giam, anh);
            m.put("danhMuc", c.getSanPham().getDanhMuc() == null ? null : c.getSanPham().getDanhMuc().getTen());
            items.add(m);
        }
        Map<String, Object> luaChon = new LinkedHashMap<>();
        boLoc.forEach((ten, m) -> luaChon.put(ten, m.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .map(e -> Map.<String, Object>of("id", e.getKey(), "ten", e.getValue())).toList()));
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("items", items);
        kq.put("tongSo", ds.size());
        kq.put("trang", trang);
        kq.put("soTrang", soTrang);
        kq.put("kichThuoc", kt);
        kq.put("boLoc", luaChon);
        return kq;
    }

    /** Quét mã QR / mã vạch: đúng mã biến thể (không phân biệt hoa thường). */
    @Transactional(readOnly = true)
    public Map<String, Object> timTheoMa(String ma) {
        String m = ma == null ? "" : ma.trim();
        if (m.isEmpty()) throw ApiException.badRequest("Thiếu mã sản phẩm.");
        SanPhamChiTiet c = spctRepo.findFirstByMaSpctIgnoreCase(m)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm có mã " + m + "."));
        if (!dangBan(c)) throw ApiException.conflict("Sản phẩm " + m + " đang ngừng bán.");
        return bienTheDto(c, giamGiaService.phanTramTheoBienThe(bayGio()), anhTheoSanPham());
    }

    static boolean dangBan(SanPhamChiTiet c) {
        SanPham sp = c.getSanPham();
        return Integer.valueOf(SanPhamChiTiet.HOAT_DONG).equals(c.getTrangThai())
                && sp != null && Integer.valueOf(1).equals(sp.getTrangThai());
    }

    private static String chuoiTim(SanPhamChiTiet c) {
        SanPham sp = c.getSanPham();
        StringBuilder sb = new StringBuilder();
        sb.append(c.getMaSpct()).append(' ').append(sp.getTenSanPham()).append(' ').append(sp.getMaSanPham());
        if (sp.getThuongHieu() != null) sb.append(' ').append(sp.getThuongHieu().getTen());
        if (sp.getDanhMuc() != null) sb.append(' ').append(sp.getDanhMuc().getTen());
        if (c.getMauSac() != null) sb.append(' ').append(c.getMauSac().getTen());
        if (c.getTrongLuong() != null) sb.append(' ').append(c.getTrongLuong().getTen());
        return sb.toString();
    }

    Map<Integer, List<HinhAnhSanPham>> anhTheoSanPham() {
        Map<Integer, List<HinhAnhSanPham>> m = new HashMap<>();
        for (HinhAnhSanPham a : hinhAnhRepo.findByTrangThaiOrderByLaAnhChinhDescIdAsc(1)) {
            m.computeIfAbsent(a.getSanPham().getId(), k -> new ArrayList<>()).add(a);
        }
        return m;
    }

    Map<String, Object> bienTheDto(SanPhamChiTiet c, Map<Long, Double> giam, Map<Integer, List<HinhAnhSanPham>> anh) {
        SanPham sp = c.getSanPham();
        long giaGoc = c.getGiaBan() == null ? 0 : c.getGiaBan();
        double pt = giam.getOrDefault(c.getId().longValue(), 0.0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("ma", c.getMaSpct());
        m.put("tenSanPham", sp.getTenSanPham());
        m.put("thuongHieu", sp.getThuongHieu() == null ? null : sp.getThuongHieu().getTen());
        m.put("mauSac", c.getMauSac() == null ? null : c.getMauSac().getTen());
        m.put("trongLuong", c.getTrongLuong() == null ? null : c.getTrongLuong().getTen());
        m.put("chuVi", c.getChuVi() == null ? null : c.getChuVi().getTen());
        m.put("phanLoai", phanLoai(c));
        String a = AnhSanPham.anhCuaBienThe(anh.getOrDefault(sp.getId(), List.of()), c.getMauSac() == null ? null : c.getMauSac().getId());
        m.put("anh", a.isEmpty() ? null : a);
        m.put("giaGoc", giaGoc);
        m.put("phanTramGiam", pt);
        m.put("donGia", PhieuApDung.giaSauDot(giaGoc, pt));
        m.put("tonKho", c.getSoLuongTon() == null ? 0 : c.getSoLuongTon());
        return m;
    }

    /** "Đen · 4U · G5" (bỏ các thuộc tính "Không áp dụng" của sản phẩm không phải vợt). */
    static String phanLoai(SanPhamChiTiet c) {
        List<String> p = new ArrayList<>();
        if (c.getMauSac() != null) p.add(c.getMauSac().getTen());
        if (c.getTrongLuong() != null && !NhomDanhMuc.laKhongApDung(c.getTrongLuong().getTen())) p.add(c.getTrongLuong().getTen());
        if (c.getChuVi() != null && !NhomDanhMuc.laKhongApDung(c.getChuVi().getTen())) p.add(c.getChuVi().getTen());
        return String.join(" · ", p);
    }

    /* ===================== Khách hàng ===================== */

    /** Tối đa 10 khách đang hoạt động khớp tên / SĐT / email / mã (không phân biệt dấu). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> timKhach(String tuKhoa) {
        String k = boDau(tuKhoa);
        if (k.isEmpty()) return List.of();
        List<Map<String, Object>> kq = new ArrayList<>();
        for (KhachHang kh : khachHangRepo.findByTrangThaiOrderByHoTenAsc(1)) {
            String chuoi = boDau(kh.getHoTen() + " " + kh.getSdt() + " " + kh.getEmail() + " " + kh.getMaKhachHang());
            if (!chuoi.contains(k)) continue;
            kq.add(khachDto(kh));
            if (kq.size() >= 10) break;
        }
        return kq;
    }

    static Map<String, Object> khachDto(KhachHang kh) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", kh.getId());
        m.put("ma", kh.getMaKhachHang());
        m.put("hoTen", kh.getHoTen());
        m.put("sdt", kh.getSdt());
        m.put("email", kh.getEmail());
        return m;
    }

    /* ===================== Phiếu giảm giá ===================== */

    /**
     * Phiếu có thể dùng cho đơn (khách + tổng tiền hàng): phiếu công khai đang chạy + phiếu cá nhân khách được tặng mà chưa dùng.
     * Mỗi phiếu kèm apDungDuoc / lyDo / soTienGiam; phiếu dùng được xếp trước, giảm nhiều nhất lên đầu.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> phieuChoDon(Integer idKhachHang, Long tongTien) {
        long tong = tongTien == null ? 0 : Math.max(0, tongTien);
        LocalDateTime luc = bayGio();
        KhachHang kh = idKhachHang == null ? null : khachHangRepo.findById(idKhachHang).orElse(null);
        Map<Integer, PhieuGiamGiaKhachHang> cuaKhach = new HashMap<>();
        if (kh != null) {
            for (PhieuGiamGiaKhachHang x : phieuKhachRepo.findByIdKhachHang(kh.getId())) {
                if (x.getNgaySuDung() == null) cuaKhach.put(x.getIdPhieuGiamGia(), x);
            }
        }
        List<Map<String, Object>> kq = new ArrayList<>();
        for (PhieuGiamGia p : phieuRepo.findAll()) {
            boolean caNhan = Integer.valueOf(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE).equals(p.getDoiTuongApDung());
            if (caNhan && !cuaKhach.containsKey(p.getId())) continue; // phiếu cá nhân của người khác: không hiện
            if (!Integer.valueOf(PhieuGiamGia.HOAT_DONG).equals(p.getTrangThai())) continue;
            if (p.getNgayBatDau() != null && luc.isBefore(p.getNgayBatDau())) continue;
            if (p.getNgayKetThuc() != null && luc.isAfter(p.getNgayKetThuc())) continue;
            long daDung = kh == null || caNhan ? 0
                    : hoaDonRepo.countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot(kh.getId(), p.getId(), HoaDon.DA_HUY);
            String lyDo = PhieuApDung.lyDoKhongDung(p, tong, luc, kh != null, cuaKhach.containsKey(p.getId()), daDung);
            kq.add(phieuDto(p, lyDo, lyDo == null ? PhieuApDung.soTienGiam(p, tong) : 0));
        }
        kq.sort(Comparator.comparing((Map<String, Object> m) -> !(Boolean) m.get("apDungDuoc"))
                .thenComparing(m -> -((Long) m.get("soTienGiam")))
                .thenComparing(m -> (String) m.get("ma")));
        return kq;
    }

    static Map<String, Object> phieuDto(PhieuGiamGia p, String lyDo, long soTienGiam) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("ma", p.getMaPhieu());
        m.put("ten", p.getTenPhieu());
        m.put("caNhan", Integer.valueOf(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE).equals(p.getDoiTuongApDung()));
        m.put("loaiGiam", Integer.valueOf(PhieuGiamGia.LOAI_PHAN_TRAM).equals(p.getLoaiGiam()) ? "PHAN_TRAM" : "TIEN_MAT");
        m.put("giaTri", p.getGiaTriGiam());
        m.put("giamToiDa", p.getGiamToiDa());
        m.put("donToiThieu", p.getGiaTriDonHangToiThieu());
        m.put("ngayKetThuc", p.getNgayKetThuc());
        m.put("conLai", p.getSoLuong() == null ? null : Math.max(0, p.getSoLuong() - (p.getSoLuongDaDung() == null ? 0 : p.getSoLuongDaDung())));
        m.put("apDungDuoc", lyDo == null);
        m.put("lyDo", lyDo);
        m.put("soTienGiam", soTienGiam);
        return m;
    }

    /* ===================== Thanh toán ===================== */

    public record DongHang(Integer idBienThe, Integer soLuong, Long donGia) {
    }

    /** soTienGiam: mức giảm của phiếu đang hiện trên màn hình (null = không so); lệch với mức giảm lúc thanh toán thì báo PHIEU_THAY_DOI. */
    public record ThanhToanRequest(Integer idKhachHang, String maPhieu, String ghiChu, List<DongHang> items,
                                   Long tienMat, Long chuyenKhoan, String maGiaoDich, Boolean xacNhanGiaMoi, Long soTienGiam) {
    }

    @Transactional
    public Map<String, Object> thanhToan(ThanhToanRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        GiaoCa ca = giaoCaService.caDangMoBatBuoc(nd.id());
        NhanVien nv = nhanVienRepo.findById(nd.id()).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên."));
        if (req == null || req.items() == null || req.items().isEmpty()) throw ApiException.badRequest("Hóa đơn chưa có sản phẩm nào.");
        String ghiChu = req.ghiChu() == null ? null : req.ghiChu().trim();
        if (ghiChu != null && ghiChu.length() > 500) throw ApiException.badRequest("Ghi chú tối đa 500 ký tự.");
        if (ghiChu != null && ghiChu.isEmpty()) ghiChu = null;
        LocalDateTime luc = bayGio();

        // 1. Gộp dòng trùng biến thể, kiểm tra số lượng
        Map<Integer, Integer> soLuong = new LinkedHashMap<>();
        Map<Integer, Long> giaGiaoDien = new HashMap<>();
        for (DongHang d : req.items()) {
            if (d == null || d.idBienThe() == null) throw ApiException.badRequest("Dòng sản phẩm không hợp lệ.");
            int sl = d.soLuong() == null ? 0 : d.soLuong();
            if (sl < 1 || sl > SO_LUONG_TOI_DA) throw ApiException.badRequest("Số lượng mỗi sản phẩm từ 1 đến " + SO_LUONG_TOI_DA + ".");
            soLuong.merge(d.idBienThe(), sl, Integer::sum);
            if (d.donGia() != null) giaGiaoDien.put(d.idBienThe(), d.donGia());
        }
        if (soLuong.size() > SO_DONG_TOI_DA) throw ApiException.badRequest("Một hóa đơn tối đa " + SO_DONG_TOI_DA + " sản phẩm khác nhau.");

        // 2. Giá theo đợt giảm giá ngay lúc thanh toán
        Map<Long, Double> phanTram = giamGiaService.phanTramTheoBienThe(luc);
        Map<Integer, SanPhamChiTiet> bienThe = new LinkedHashMap<>();
        Map<Integer, Long> donGia = new LinkedHashMap<>();
        List<Map<String, Object>> giaDoi = new ArrayList<>();
        long tongTienHang = 0;
        for (Map.Entry<Integer, Integer> e : soLuong.entrySet()) {
            SanPhamChiTiet c = spctRepo.findById(e.getKey())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm (mã biến thể #" + e.getKey() + "). Hãy xóa dòng đó khỏi hóa đơn."));
            if (!dangBan(c)) throw ApiException.conflict("Sản phẩm " + c.getMaSpct() + " đã ngừng bán. Hãy xóa khỏi hóa đơn.");
            long gia = PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), phanTram.getOrDefault(c.getId().longValue(), 0.0));
            Long giaCu = giaGiaoDien.get(c.getId());
            if (giaCu != null && giaCu != gia) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idBienThe", c.getId());
                m.put("ma", c.getMaSpct());
                m.put("donGiaCu", giaCu);
                m.put("donGiaMoi", gia);
                giaDoi.add(m);
            }
            bienThe.put(c.getId(), c);
            donGia.put(c.getId(), gia);
            tongTienHang += gia * e.getValue();
        }
        if (!giaDoi.isEmpty() && !Boolean.TRUE.equals(req.xacNhanGiaMoi())) {
            throw new ApiException(HttpStatus.CONFLICT, "Giá của " + giaDoi.size() + " sản phẩm đã thay đổi (đợt giảm giá vừa bắt đầu / kết thúc). "
                    + "Hãy kiểm tra lại giá rồi thanh toán.", "GIA_THAY_DOI", Map.of("items", giaDoi));
        }

        // 3. Khách hàng + phiếu giảm giá
        KhachHang kh = null;
        if (req.idKhachHang() != null) {
            kh = khachHangRepo.findById(req.idKhachHang()).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng."));
            if (!Integer.valueOf(1).equals(kh.getTrangThai())) throw ApiException.conflict("Khách hàng đang ngừng hoạt động.");
        }
        KetQuaPhieu kp = kiemTraPhieu(req.maPhieu(), kh, tongTienHang, luc, req.soTienGiam());
        PhieuGiamGia phieu = kp.phieu();
        long giam = kp.giam();
        long thanhTien = tongTienHang - giam;

        // 4. Tiền khách trả
        KetQuaTien t = kiemTraTien(req.tienMat(), req.chuyenKhoan(), req.maGiaoDich(), thanhTien);
        long tienMat = t.tienMat();
        long chuyenKhoan = t.chuyenKhoan();
        long tienMatThu = t.tienMatThu();
        long tienThua = t.tienThua();

        // 5. Trừ tồn kho có điều kiện
        for (Map.Entry<Integer, Integer> e : soLuong.entrySet()) {
            if (spctRepo.truTonKho(e.getKey(), e.getValue()) == 0) {
                SanPhamChiTiet c = bienThe.get(e.getKey());
                throw new ApiException(HttpStatus.CONFLICT, "Sản phẩm " + c.getMaSpct() + " không đủ hàng (cần " + e.getValue()
                        + ", trong kho còn " + (c.getSoLuongTon() == null ? 0 : c.getSoLuongTon()) + ").", "HET_HANG",
                        Map.of("idBienThe", c.getId()));
            }
        }

        // 6. Phiếu: tăng lượt dùng có điều kiện, phiếu cá nhân đánh dấu đã dùng
        dungPhieu(kp, luc);

        // 7. Hóa đơn + chi tiết + lịch sử + thanh toán
        String maPt = chuyenKhoan == 0 ? PhuongThucThanhToan.TIEN_MAT
                : tienMatThu == 0 ? PhuongThucThanhToan.CHUYEN_KHOAN : PhuongThucThanhToan.KET_HOP;
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(MaUtil.tiepTheo("HD", hoaDonRepo.findAllMaHoaDon()));
        hd.setLoaiDon(HoaDon.TAI_QUAY);
        hd.setTrangThai(HoaDon.HOAN_THANH);
        hd.setNhanVien(nv);
        hd.setKhachHang(kh);
        hd.setPhieuGiamGia(phieu);
        hd.setIdPhuongThucThanhToan(idPhuongThuc(maPt));
        hd.setTongTien(BigDecimal.valueOf(tongTienHang));
        hd.setSoTienGiam(BigDecimal.valueOf(giam));
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setThanhTien(BigDecimal.valueOf(thanhTien));
        hd.setGhiChu(ghiChu);
        hd.setNgayTao(luc);
        hd.setNgayCapNhat(luc);
        hd.setIdGiaoCa(ca.getId());
        try {
            hd = hoaDonRepo.saveAndFlush(hd);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Mã hóa đơn vừa bị trùng với máy khác. Vui lòng bấm thanh toán lại.", "TRUNG_MA", null);
        }
        for (Map.Entry<Integer, Integer> e : soLuong.entrySet()) {
            HoaDonChiTiet ct = new HoaDonChiTiet();
            ct.setHoaDon(hd);
            ct.setSanPhamChiTiet(bienThe.get(e.getKey()));
            ct.setSoLuong(e.getValue());
            ct.setDonGia(BigDecimal.valueOf(donGia.get(e.getKey())));
            ct.setThanhTien(BigDecimal.valueOf(donGia.get(e.getKey()) * e.getValue()));
            ct.setTrangThai(1);
            chiTietRepo.save(ct);
        }
        ghiLichSu(hd, "Tạo đơn hàng", "Tạo hóa đơn tại quầy", nv.getHoTen(), luc);
        ghiLichSu(hd, "Đã hoàn thành", "Khách đã thanh toán, nhận hàng tại cửa hàng", nv.getHoTen(), luc);
        ghiThanhToanQuay(hd, t, luc);
        return ketQua(hd, t, ca);
    }

    /* ===================== Thanh toán hóa đơn chờ (lưu ở CSDL) ===================== */

    /** Bán tại quầy nhưng giao hàng cho khách: khách lẻ chỉ nhập MỘT địa chỉ nhận hàng (cô: khách lẻ có mỗi một địa chỉ). */
    public record GiaoHangRequest(String hoTen, String sdt, String diaChi) {
    }

    public record ThanhToanChoRequest(String maPhieu, String ghiChu, Long tienMat, Long chuyenKhoan, String maGiaoDich,
                                      Boolean xacNhanGiaMoi, Long soTienGiam, GiaoHangRequest giaoHang) {
    }

    /** Phí giao hàng giống web bán online: đơn từ 1.000.000 ₫ miễn phí, còn lại 30.000 ₫. */
    public static long phiGiaoHang(long tongTienHang) {
        return tongTienHang >= 1_000_000L ? 0L : 30_000L;
    }

    /**
     * Thanh toán một hóa đơn chờ (HoaDonChoService): sản phẩm + tồn kho đã có sẵn trong CSDL nên chỉ cần
     *  - kiểm tra giá từng dòng còn đúng giá hiện tại không (khác thì GIA_THAY_DOI; nhân viên xác nhận thì cập nhật giá mới);
     *  - phiếu giảm giá, tiền khách trả như thanh toán thường;
     *  - giao hàng (nếu chọn): phí ship, người nhận; KHÔNG cần bước "xác nhận" nữa vì khách đã trả tiền tại quầy -> "Chờ giao hàng".
     * Không giao hàng thì hóa đơn "Đã hoàn thành" ngay (in hóa đơn luôn ở bước này).
     */
    @Transactional
    public Map<String, Object> thanhToanHoaDonCho(Integer idHoaDon, ThanhToanChoRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        GiaoCa ca = giaoCaService.caDangMoBatBuoc(nd.id());
        HoaDon hd = idHoaDon == null ? null : hoaDonRepo.findById(idHoaDon).orElse(null);
        if (hd == null) throw ApiException.notFound("Không tìm thấy hóa đơn chờ.");
        if (hd.getNhanVien() == null || !hd.getNhanVien().getId().equals(nd.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Đây là hóa đơn chờ của nhân viên khác.");
        }
        if (!HoaDonChoService.laHoaDonCho(hd)) {
            throw new ApiException(HttpStatus.CONFLICT, "Hóa đơn " + hd.getMaHoaDon() + " đã thanh toán hoặc đã hủy. Hãy tải lại trang.",
                    "HOA_DON_CHO_DA_DONG", null);
        }
        if (req == null) throw ApiException.badRequest("Thiếu thông tin thanh toán.");
        String ghiChu = req.ghiChu() == null ? null : req.ghiChu().trim();
        if (ghiChu != null && ghiChu.length() > 500) throw ApiException.badRequest("Ghi chú tối đa 500 ký tự.");
        if (ghiChu != null && ghiChu.isEmpty()) ghiChu = null;
        LocalDateTime luc = bayGio();

        // 1. Giá từng dòng
        List<HoaDonChiTiet> dong = chiTietRepo.findByHoaDon_IdOrderByIdAsc(hd.getId());
        if (dong.isEmpty()) throw ApiException.badRequest("Hóa đơn chưa có sản phẩm nào.");
        Map<Long, Double> phanTram = giamGiaService.phanTramTheoBienThe(luc);
        List<Map<String, Object>> giaDoi = new ArrayList<>();
        for (HoaDonChiTiet ct : dong) {
            SanPhamChiTiet c = ct.getSanPhamChiTiet();
            if (!dangBan(c)) throw ApiException.conflict("Sản phẩm " + c.getMaSpct() + " đã ngừng bán. Hãy xóa khỏi hóa đơn.");
            long gia = PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), phanTram.getOrDefault(c.getId().longValue(), 0.0));
            long cu = ct.getDonGia() == null ? 0 : ct.getDonGia().longValue();
            if (gia != cu) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idChiTiet", ct.getId());
                m.put("idBienThe", c.getId());
                m.put("ma", c.getMaSpct());
                m.put("donGiaCu", cu);
                m.put("donGiaMoi", gia);
                giaDoi.add(m);
            }
        }
        if (!giaDoi.isEmpty()) {
            if (!Boolean.TRUE.equals(req.xacNhanGiaMoi())) {
                throw new ApiException(HttpStatus.CONFLICT, "Giá của " + giaDoi.size() + " sản phẩm trong hóa đơn đã thay đổi. "
                        + "Hãy báo khách giá mới và bấm \"Cập nhật giá\" trước khi thanh toán.", "GIA_THAY_DOI", Map.of("items", giaDoi));
            }
            for (HoaDonChiTiet ct : dong) {
                for (Map<String, Object> m : giaDoi) {
                    if (!ct.getId().equals(m.get("idChiTiet"))) continue;
                    long moi = (Long) m.get("donGiaMoi");
                    ct.setDonGia(BigDecimal.valueOf(moi));
                    ct.setThanhTien(BigDecimal.valueOf(moi * ct.getSoLuong()));
                    chiTietRepo.save(ct);
                }
            }
        }
        long tongTienHang = 0;
        for (HoaDonChiTiet ct : dong) tongTienHang += ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue();

        // 2. Khách + phiếu
        KhachHang kh = hd.getKhachHang();
        if (kh != null && !Integer.valueOf(1).equals(kh.getTrangThai())) throw ApiException.conflict("Khách hàng đang ngừng hoạt động.");
        KetQuaPhieu kp = kiemTraPhieu(req.maPhieu(), kh, tongTienHang, luc, req.soTienGiam());

        // 3. Giao hàng
        GiaoHangRequest gh = req.giaoHang();
        boolean giao = gh != null;
        String hoTen = null;
        String sdt = null;
        String diaChi = null;
        if (giao) {
            com.footstyle.demo.exception.LoiForm loi = new com.footstyle.demo.exception.LoiForm();
            hoTen = gh.hoTen() == null ? "" : gh.hoTen().trim().replaceAll("\\s+", " ");
            sdt = gh.sdt() == null ? "" : gh.sdt().trim().replaceAll("[\\s.-]", "");
            diaChi = gh.diaChi() == null ? "" : gh.diaChi().trim().replaceAll("\\s+", " ");
            if (hoTen.isEmpty()) loi.them("hoTen", "Vui lòng nhập tên người nhận.");
            else if (hoTen.length() > 255) loi.them("hoTen", "Tên người nhận tối đa 255 ký tự.");
            if (sdt.isEmpty()) loi.them("sdt", "Vui lòng nhập số điện thoại người nhận.");
            else if (!sdt.matches("0[35789]\\d{8}")) loi.them("sdt", "Số điện thoại gồm 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
            if (diaChi.isEmpty()) loi.them("diaChi", "Vui lòng nhập địa chỉ giao hàng.");
            else if (diaChi.length() < 10) loi.them("diaChi", "Địa chỉ quá ngắn, hãy ghi rõ số nhà, đường, phường/xã, tỉnh/thành.");
            else if (diaChi.length() > 500) loi.them("diaChi", "Địa chỉ tối đa 500 ký tự.");
            loi.nemNeuCoLoi();
        }
        long phi = giao ? phiGiaoHang(tongTienHang) : 0;
        long thanhTien = tongTienHang - kp.giam() + phi;

        // 4. Tiền khách trả, phiếu
        KetQuaTien t = kiemTraTien(req.tienMat(), req.chuyenKhoan(), req.maGiaoDich(), thanhTien);
        dungPhieu(kp, luc);

        // 5. Chốt hóa đơn
        String maPt = t.chuyenKhoan() == 0 ? PhuongThucThanhToan.TIEN_MAT
                : t.tienMatThu() == 0 ? PhuongThucThanhToan.CHUYEN_KHOAN : PhuongThucThanhToan.KET_HOP;
        hd.setLoaiDon(giao ? HoaDon.GIAO_HANG : HoaDon.TAI_QUAY);
        hd.setTrangThai(giao ? HoaDon.CHO_LAY_HANG : HoaDon.HOAN_THANH);
        hd.setPhieuGiamGia(kp.phieu());
        hd.setIdPhuongThucThanhToan(idPhuongThuc(maPt));
        hd.setTongTien(BigDecimal.valueOf(tongTienHang));
        hd.setSoTienGiam(BigDecimal.valueOf(kp.giam()));
        hd.setPhiVanChuyen(BigDecimal.valueOf(phi));
        hd.setThanhTien(BigDecimal.valueOf(thanhTien));
        hd.setGhiChu(ghiChu);
        hd.setHoTenNguoiNhan(hoTen);
        hd.setSdtNguoiNhan(sdt);
        hd.setDiaChiGiaoHang(diaChi);
        hd.setNgayCapNhat(luc);
        hd.setIdGiaoCa(ca.getId());
        hd = hoaDonRepo.save(hd);
        String nguoi = hd.getNhanVien().getHoTen();
        if (giao) {
            ghiLichSu(hd, "Đã xác nhận", "Khách đặt giao hàng và đã thanh toán tại quầy", nguoi, luc);
            ghiLichSu(hd, "Chờ giao hàng", "Đóng gói, chờ giao cho đơn vị vận chuyển (phí giao hàng " + tien(phi) + ")", nguoi, luc);
        } else {
            ghiLichSu(hd, "Đã hoàn thành", "Khách đã thanh toán, nhận hàng tại cửa hàng", nguoi, luc);
        }
        ghiThanhToanQuay(hd, t, luc);
        Map<String, Object> kq = ketQua(hd, t, ca);
        kq.put("giaoHang", giao);
        kq.put("phiVanChuyen", phi);
        return kq;
    }

    /* ----- Phần dùng chung của 2 kiểu thanh toán ----- */

    private record KetQuaPhieu(PhieuGiamGia phieu, PhieuGiamGiaKhachHang cuaKhach, long giam) {
    }

    private record KetQuaTien(long tienMat, long chuyenKhoan, long tienMatThu, long tienThua, String maGiaoDich) {
    }

    /** Phiếu còn dùng được cho khách + tổng tiền hàng không; soTienGiamGiaoDien khác mức giảm hiện tại thì báo PHIEU_THAY_DOI. */
    private KetQuaPhieu kiemTraPhieu(String maPhieuGui, KhachHang kh, long tongTienHang, LocalDateTime luc, Long soTienGiamGiaoDien) {
        String maPhieu = maPhieuGui == null ? "" : maPhieuGui.trim();
        if (maPhieu.isEmpty()) return new KetQuaPhieu(null, null, 0);
        PhieuGiamGia phieu = phieuRepo.findFirstByMaPhieuIgnoreCase(maPhieu).orElse(null);
        PhieuGiamGiaKhachHang phieuCuaKhach = null;
        if (phieu != null && kh != null) {
            for (PhieuGiamGiaKhachHang x : phieuKhachRepo.findByIdKhachHang(kh.getId())) {
                if (x.getIdPhieuGiamGia().equals(phieu.getId()) && x.getNgaySuDung() == null) phieuCuaKhach = x;
            }
        }
        long daDung = kh == null || phieu == null ? 0
                : hoaDonRepo.countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot(kh.getId(), phieu.getId(), HoaDon.DA_HUY);
        String lyDo = PhieuApDung.lyDoKhongDung(phieu, tongTienHang, luc, kh != null, phieuCuaKhach != null, daDung);
        if (lyDo != null) throw new ApiException(HttpStatus.CONFLICT, "Không dùng được phiếu " + maPhieu + ": " + lyDo, "PHIEU_KHONG_HOP_LE", null);
        long giam = PhieuApDung.soTienGiam(phieu, tongTienHang);
        // Phiếu bị quản lý sửa mức giảm trong lúc hóa đơn đang chờ: không tự đổi số tiền khách phải trả mà báo lại để nhân viên xem
        if (soTienGiamGiaoDien != null && soTienGiamGiaoDien != giam) {
            Map<String, Object> ct = new LinkedHashMap<>();
            ct.put("maPhieu", phieu.getMaPhieu());
            ct.put("soTienGiamCu", soTienGiamGiaoDien);
            ct.put("soTienGiamMoi", giam);
            throw new ApiException(HttpStatus.CONFLICT, "Phiếu " + phieu.getMaPhieu() + " vừa được cập nhật: mức giảm đổi từ " + tien(soTienGiamGiaoDien)
                    + " thành " + tien(giam) + ". Hãy kiểm tra lại số tiền khách phải trả rồi thanh toán.", "PHIEU_THAY_DOI", ct);
        }
        return new KetQuaPhieu(phieu, phieuCuaKhach, giam);
    }

    /** Tiền mặt + chuyển khoản phải đủ; chuyển khoản không quá số phải trả (tiền thừa chỉ trả bằng tiền mặt). */
    private static KetQuaTien kiemTraTien(Long tienMatGui, Long chuyenKhoanGui, String maGiaoDichGui, long thanhTien) {
        long tienMat = tienMatGui == null ? 0 : tienMatGui;
        long chuyenKhoan = chuyenKhoanGui == null ? 0 : chuyenKhoanGui;
        if (tienMat < 0 || chuyenKhoan < 0) throw ApiException.badRequest("Số tiền không được âm.");
        if (tienMat > TIEN_TOI_DA || chuyenKhoan > TIEN_TOI_DA) throw ApiException.badRequest("Số tiền quá lớn.");
        if (chuyenKhoan > thanhTien) {
            throw ApiException.badRequest("Tiền chuyển khoản (" + tien(chuyenKhoan) + ") không được lớn hơn số tiền phải trả (" + tien(thanhTien) + ").");
        }
        if (tienMat + chuyenKhoan < thanhTien) {
            throw ApiException.badRequest("Khách trả chưa đủ, còn thiếu " + tien(thanhTien - tienMat - chuyenKhoan) + ".");
        }
        String maGiaoDich = maGiaoDichGui == null ? null : maGiaoDichGui.trim();
        if (maGiaoDich != null && maGiaoDich.length() > 100) throw ApiException.badRequest("Mã giao dịch tối đa 100 ký tự.");
        if (maGiaoDich != null && maGiaoDich.isEmpty()) maGiaoDich = null;
        long tienMatThu = thanhTien - chuyenKhoan; // phần tiền mặt thực giữ lại (đã trừ tiền thừa trả khách)
        return new KetQuaTien(tienMat, chuyenKhoan, tienMatThu, tienMat - tienMatThu, maGiaoDich);
    }

    private void dungPhieu(KetQuaPhieu kp, LocalDateTime luc) {
        if (kp.phieu() == null) return;
        if (phieuRepo.tangLuotDung(kp.phieu().getId()) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Phiếu " + kp.phieu().getMaPhieu() + " vừa hết lượt sử dụng.", "PHIEU_KHONG_HOP_LE", null);
        }
        if (kp.cuaKhach() != null) {
            kp.cuaKhach().setNgaySuDung(luc);
            phieuKhachRepo.save(kp.cuaKhach());
        }
    }

    private void ghiThanhToanQuay(HoaDon hd, KetQuaTien t, LocalDateTime luc) {
        if (t.tienMatThu() > 0) {
            ghiThanhToan(hd, t.tienMatThu(), null, PhuongThucThanhToan.TIEN_MAT,
                    "Tiền mặt tại quầy (khách đưa " + tien(t.tienMat()) + (t.tienThua() > 0 ? ", trả lại " + tien(t.tienThua()) : "") + ")", luc);
        }
        if (t.chuyenKhoan() > 0) {
            ghiThanhToan(hd, t.chuyenKhoan(), t.maGiaoDich(), PhuongThucThanhToan.CHUYEN_KHOAN, "Chuyển khoản tại quầy", luc);
        }
    }

    private static Map<String, Object> ketQua(HoaDon hd, KetQuaTien t, GiaoCa ca) {
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("id", hd.getId());
        kq.put("maHoaDon", hd.getMaHoaDon());
        kq.put("tongTien", hd.getTongTien() == null ? 0 : hd.getTongTien().longValue());
        kq.put("soTienGiam", hd.getSoTienGiam() == null ? 0 : hd.getSoTienGiam().longValue());
        kq.put("thanhTien", hd.getThanhTien() == null ? 0 : hd.getThanhTien().longValue());
        kq.put("tienMat", t.tienMat());
        kq.put("chuyenKhoan", t.chuyenKhoan());
        kq.put("tienThua", t.tienThua());
        kq.put("maGiaoCa", ca.getMaGiaoCa());
        return kq;
    }

    private Integer idPhuongThuc(String ma) {
        Optional<PhuongThucThanhToan> pt = phuongThucRepo.findFirstByMaPhuongThuc(ma);
        if (pt.isEmpty() && PhuongThucThanhToan.KET_HOP.equals(ma)) pt = phuongThucRepo.findFirstByMaPhuongThuc(PhuongThucThanhToan.TIEN_MAT);
        return pt.map(PhuongThucThanhToan::getId).orElse(null);
    }

    private void ghiLichSu(HoaDon hd, String hanhDong, String moTa, String nguoi, LocalDateTime luc) {
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong(hanhDong);
        ls.setMoTa(moTa);
        ls.setNguoiThaoTac(nguoi);
        ls.setThoiGian(luc);
        lichSuRepo.save(ls);
    }

    private void ghiThanhToan(HoaDon hd, long soTien, String maGiaoDich, String maPt, String moTa, LocalDateTime luc) {
        LichSuThanhToan tt = new LichSuThanhToan();
        tt.setHoaDon(hd);
        tt.setSoTien(BigDecimal.valueOf(soTien));
        tt.setMaGiaoDich(maGiaoDich);
        tt.setThoiGian(luc);
        tt.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        tt.setMoTa(moTa);
        tt.setIdPhuongThucThanhToan(idPhuongThuc(maPt));
        thanhToanRepo.save(tt);
    }

    private static String tien(long v) {
        return GiaoCaService.dinhDangTien(BigDecimal.valueOf(v));
    }

    /** Bỏ dấu tiếng Việt + chữ thường + gọn khoảng trắng (tìm "vot yonex" ra "Vợt Yonex"). */
    public static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return t.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
