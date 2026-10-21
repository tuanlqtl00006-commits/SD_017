package com.footstyle.demo.service;

import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.PhuongThucThanhToan;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.GiaoCaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichLamViecRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.PhuongThucThanhToanRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giao ca: nhận ca (tiền đầu ca) -> bán hàng tại quầy (hóa đơn gắn id_giao_ca) -> kết ca (hệ thống tổng kết, nhân viên đếm tiền,
 * để lại tiền bàn giao cho ca sau).
 *  - Mỗi nhân viên chỉ 1 ca đang mở (kiểm tra ở đây + chỉ mục ux_giao_ca_dang_mo ở CSDL).
 *  - Gợi ý tiền đầu ca = tiền bàn giao của lần giao ca gần nhất; nhập khác thì bắt buộc ghi chú.
 *  - Tự gắn với ca trong lịch làm hôm nay (đến sớm tối đa 60 phút), ghi số phút đến muộn. Không có lịch vẫn nhận được ("ngoài lịch").
 *  - Kết ca: tiền mặt hệ thống = tiền đầu ca + tiền mặt thu trong ca; chênh lệch = thực tế - hệ thống; lệch thì bắt buộc ghi lý do.
 */
@Service
@RequiredArgsConstructor
public class GiaoCaService {

    public static final long TIEN_TOI_DA = 1_000_000_000L;
    /** Đến muộn quá bấy nhiêu phút thì đánh dấu "Đi muộn" trên giao diện. */
    public static final int PHUT_MUON_CHO_PHEP = 15;
    /** Nhận ca sớm hơn giờ bắt đầu tối đa bấy nhiêu phút vẫn tính là ca đó. */
    static final int PHUT_DEN_SOM = 60;

    private final GiaoCaRepository giaoCaRepo;
    private final NhanVienRepository nhanVienRepo;
    private final LichLamViecRepository lichRepo;
    private final HoaDonRepository hoaDonRepo;
    private final LichSuThanhToanRepository thanhToanRepo;
    private final PhuongThucThanhToanRepository phuongThucRepo;

    private Clock dongHo = Clock.systemDefaultZone();

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
    }

    public record NhanCaRequest(BigDecimal tienDauCa, String ghiChu, Integer idLichLamViec) {
    }

    public record KetCaRequest(BigDecimal tienMatThucTe, BigDecimal tienBanGiao, Integer idNhanVienNhanBanGiao, String ghiChu) {
    }

    /* ===================== Ca hiện tại ===================== */

    /** { dangMo: ca đang mở | null, tongKet (nếu đang mở), goiYTienDauCa, nguoiBanGiao, lichHomNay: [...] } của người đang đăng nhập. */
    @Transactional(readOnly = true)
    public Map<String, Object> hienTai() {
        NguoiDung nd = XacThuc.nhanVien();
        Map<String, Object> kq = new LinkedHashMap<>();
        Optional<GiaoCa> mo = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA);
        kq.put("dangMo", mo.map(this::dto).orElse(null));
        kq.put("tongKet", mo.map(this::tongKet).orElse(null));
        Optional<GiaoCa> truoc = giaoCaRepo.findFirstByTrangThaiOrderByThoiGianKetCaDescIdDesc(GiaoCa.DA_GIAO_CA);
        kq.put("goiYTienDauCa", truoc.map(g -> soTien(g.getTienBanGiao())).orElse(null));
        kq.put("nguoiBanGiao", truoc.map(g -> g.getNhanVien().getHoTen()).orElse(null));
        kq.put("thoiGianBanGiao", truoc.map(GiaoCa::getThoiGianKetCa).orElse(null));
        List<Map<String, Object>> lich = new ArrayList<>();
        for (LichLamViec l : lichHomNay(nd.id())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("tenCa", l.getCaLamViec().getTenCa());
            m.put("gioBatDau", l.getCaLamViec().getGioBatDau().format(LichLamViecService.GIO));
            m.put("gioKetThuc", l.getCaLamViec().getGioKetThuc().format(LichLamViecService.GIO));
            lich.add(m);
        }
        kq.put("lichHomNay", lich);
        return kq;
    }

    /** Ca đang mở của nhân viên (bán hàng tại quầy bắt buộc có). Chưa nhận ca thì 409 mã CHUA_NHAN_CA. */
    @Transactional(readOnly = true)
    public GiaoCa caDangMoBatBuoc(Integer idNhanVien) {
        return giaoCaRepo.findFirstByNhanVienIdAndTrangThai(idNhanVien, GiaoCa.DANG_TRONG_CA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "Bạn chưa nhận ca. Hãy bấm \"Ca làm việc\" > Nhận ca trước khi bán hàng.", "CHUA_NHAN_CA", null));
    }

    /* ===================== Nhận ca ===================== */

    @Transactional
    public Map<String, Object> nhanCa(NhanCaRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        Optional<GiaoCa> dangMo = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA);
        if (dangMo.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bạn đang trong ca " + dangMo.get().getMaGiaoCa() + ", hãy giao ca trước khi nhận ca mới.",
                    "DANG_TRONG_CA", null);
        }
        NhanVien nv = nhanVienRepo.findById(nd.id()).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên."));
        LocalDateTime now = bayGio();

        LoiForm loi = new LoiForm();
        BigDecimal tien = req == null ? null : req.tienDauCa();
        kiemTraTien(tien, "tienDauCa", "tiền đầu ca", loi);
        String ghiChu = gon(req == null ? null : req.ghiChu());
        if (ghiChu != null && ghiChu.length() > 500) loi.them("ghiChu", "Ghi chú tối đa 500 ký tự.");
        BigDecimal goiY = giaoCaRepo.findFirstByTrangThaiOrderByThoiGianKetCaDescIdDesc(GiaoCa.DA_GIAO_CA)
                .map(g -> soTien(g.getTienBanGiao())).orElse(null);
        if (!loi.co("tienDauCa") && goiY != null && tien.compareTo(goiY) != 0 && ghiChu == null) {
            loi.them("ghiChu", "Tiền đầu ca khác tiền ca trước bàn giao (" + dinhDangTien(goiY) + "). Vui lòng ghi lý do.");
        }
        LichLamViec lich = null;
        if (req != null && req.idLichLamViec() != null) {
            lich = lichRepo.findById(req.idLichLamViec()).orElse(null);
            if (lich == null || !lich.getNhanVien().getId().equals(nd.id()) || !lich.getNgayLam().equals(now.toLocalDate())) {
                loi.them("idLichLamViec", "Ca được chọn không phải lịch làm hôm nay của bạn.");
                lich = null;
            }
        }
        loi.nemNeuCoLoi();
        if (lich == null) lich = chonLichTuDong(nd.id(), now);

        GiaoCa gc = new GiaoCa();
        gc.setMaGiaoCa(MaUtil.tiepTheo("GC", giaoCaRepo.findAllMaGiaoCa()));
        gc.setNhanVien(nv);
        gc.setLichLamViec(lich);
        gc.setThoiGianNhanCa(now);
        gc.setTienDauCa(tien.setScale(0));
        gc.setGhiChuNhanCa(ghiChu);
        gc.setSoPhutDiMuon(lich == null ? null : soPhutMuon(lich, now));
        gc.setTrangThai(GiaoCa.DANG_TRONG_CA);
        try {
            gc = giaoCaRepo.saveAndFlush(gc);
        } catch (DataIntegrityViolationException e) {
            // 2 tab cùng bấm nhận ca: chỉ mục ux_giao_ca_dang_mo chặn ca thứ hai
            throw new ApiException(HttpStatus.CONFLICT, "Bạn vừa nhận ca ở một cửa sổ khác. Hãy tải lại trang.", "DANG_TRONG_CA", null);
        }
        return dto(gc);
    }

    /** Lịch hôm nay của nhân viên, sắp theo giờ bắt đầu. */
    List<LichLamViec> lichHomNay(Integer idNhanVien) {
        List<LichLamViec> ds = new ArrayList<>(lichRepo.findByNhanVienIdAndNgayLam(idNhanVien, bayGio().toLocalDate()));
        ds.sort(Comparator.comparing(l -> l.getCaLamViec().getGioBatDau()));
        return ds;
    }

    /** Ca trong lịch hôm nay đang diễn ra (cho phép đến sớm 60 phút); không có thì null (nhận ca ngoài lịch). */
    LichLamViec chonLichTuDong(Integer idNhanVien, LocalDateTime now) {
        for (LichLamViec l : lichHomNay(idNhanVien)) {
            CaLamViec ca = l.getCaLamViec();
            LocalDateTime bd = l.getNgayLam().atTime(ca.getGioBatDau());
            LocalDateTime kt = l.getNgayLam().atTime(ca.getGioKetThuc());
            if (!now.isBefore(bd.minusMinutes(PHUT_DEN_SOM)) && now.isBefore(kt)) return l;
        }
        return null;
    }

    static int soPhutMuon(LichLamViec l, LocalDateTime nhanCa) {
        LocalDateTime bd = l.getNgayLam().atTime(l.getCaLamViec().getGioBatDau());
        long phut = Duration.between(bd, nhanCa).toMinutes();
        return (int) Math.max(0, phut);
    }

    /* ===================== Kết ca / giao ca ===================== */

    @Transactional
    public Map<String, Object> ketCa(KetCaRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        GiaoCa gc = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Bạn không có ca nào đang mở để giao.", "CHUA_NHAN_CA", null));
        Map<String, Object> tk = tongKet(gc);
        BigDecimal heThong = (BigDecimal) tk.get("tienMatHeThong");

        LoiForm loi = new LoiForm();
        BigDecimal thucTe = req == null ? null : req.tienMatThucTe();
        BigDecimal banGiao = req == null ? null : req.tienBanGiao();
        kiemTraTien(thucTe, "tienMatThucTe", "tiền mặt thực tế", loi);
        kiemTraTien(banGiao, "tienBanGiao", "tiền bàn giao cho ca sau", loi);
        if (!loi.co("tienMatThucTe") && !loi.co("tienBanGiao") && banGiao.compareTo(thucTe) > 0) {
            loi.them("tienBanGiao", "Tiền bàn giao không được lớn hơn tiền mặt thực tế.");
        }
        String ghiChu = gon(req == null ? null : req.ghiChu());
        if (ghiChu != null && ghiChu.length() > 500) loi.them("ghiChu", "Ghi chú tối đa 500 ký tự.");
        BigDecimal chenhLech = loi.co("tienMatThucTe") ? null : thucTe.subtract(heThong);
        if (chenhLech != null && chenhLech.signum() != 0 && ghiChu == null) {
            loi.them("ghiChu", "Tiền mặt thực tế " + (chenhLech.signum() < 0 ? "thiếu " : "thừa ") + dinhDangTien(chenhLech.abs())
                    + " so với hệ thống. Vui lòng ghi lý do.");
        }
        NhanVien nguoiNhan = null;
        Integer idNhan = req == null ? null : req.idNhanVienNhanBanGiao();
        if (idNhan != null) {
            nguoiNhan = nhanVienRepo.findById(idNhan).orElse(null);
            if (nguoiNhan == null || !Integer.valueOf(NhanVien.HOAT_DONG).equals(nguoiNhan.getTrangThai())) {
                loi.them("idNhanVienNhanBanGiao", "Người nhận bàn giao không hợp lệ hoặc đã ngừng hoạt động.");
            } else if (nguoiNhan.getId().equals(nd.id())) {
                loi.them("idNhanVienNhanBanGiao", "Không bàn giao cho chính mình.");
            }
        }
        loi.nemNeuCoLoi();

        gc.setThoiGianKetCa(bayGio());
        gc.setSoHoaDon((Integer) tk.get("soHoaDon"));
        gc.setDoanhThu((BigDecimal) tk.get("doanhThu"));
        gc.setTienMatThu((BigDecimal) tk.get("tienMat"));
        gc.setChuyenKhoanThu((BigDecimal) tk.get("chuyenKhoan"));
        gc.setTienMatHeThong(heThong);
        gc.setTienMatThucTe(thucTe.setScale(0));
        gc.setChenhLech(chenhLech.setScale(0));
        gc.setTienBanGiao(banGiao.setScale(0));
        gc.setNhanVienNhanBanGiao(nguoiNhan);
        gc.setGhiChuKetCa(ghiChu);
        gc.setTrangThai(GiaoCa.DA_GIAO_CA);
        return chiTietDto(giaoCaRepo.save(gc));
    }

    /**
     * Tổng kết một ca từ các hóa đơn tại quầy gắn với ca (bỏ đơn đã hủy):
     * { soHoaDon, doanhThu, tienMat, chuyenKhoan, tienDauCa, tienMatHeThong }.
     * Tiền mặt / chuyển khoản tách theo phương thức của từng lần trả tiền (lich_su_thanh_toan.id_phuong_thuc_thanh_toan).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> tongKet(GiaoCa gc) {
        Map<Integer, String> maPhuongThuc = new HashMap<>();
        for (PhuongThucThanhToan pt : phuongThucRepo.findAll()) maPhuongThuc.put(pt.getId(), pt.getMaPhuongThuc());
        int soHoaDon = 0;
        BigDecimal doanhThu = BigDecimal.ZERO;
        for (HoaDon hd : hoaDonRepo.findByIdGiaoCaOrderByNgayTaoAsc(gc.getId())) {
            if (Integer.valueOf(HoaDon.DA_HUY).equals(hd.getTrangThai())) continue;
            soHoaDon++;
            doanhThu = doanhThu.add(soTien(hd.getThanhTien()));
        }
        BigDecimal tienMat = BigDecimal.ZERO;
        BigDecimal chuyenKhoan = BigDecimal.ZERO;
        for (LichSuThanhToan tt : thanhToanRepo.findByHoaDon_IdGiaoCa(gc.getId())) {
            if (tt.getTrangThai() == null || tt.getTrangThai() != LichSuThanhToan.DA_THANH_TOAN) continue;
            if (tt.getHoaDon() != null && Integer.valueOf(HoaDon.DA_HUY).equals(tt.getHoaDon().getTrangThai())) continue;
            Integer idPt = tt.getIdPhuongThucThanhToan() != null ? tt.getIdPhuongThucThanhToan()
                    : tt.getHoaDon() == null ? null : tt.getHoaDon().getIdPhuongThucThanhToan();
            if (PhuongThucThanhToan.TIEN_MAT.equals(maPhuongThuc.get(idPt))) tienMat = tienMat.add(soTien(tt.getSoTien()));
            else chuyenKhoan = chuyenKhoan.add(soTien(tt.getSoTien()));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("soHoaDon", soHoaDon);
        m.put("doanhThu", doanhThu);
        m.put("tienMat", tienMat);
        m.put("chuyenKhoan", chuyenKhoan);
        m.put("tienDauCa", soTien(gc.getTienDauCa()));
        m.put("tienMatHeThong", soTien(gc.getTienDauCa()).add(tienMat));
        return m;
    }

    /* ===================== Lịch sử ===================== */

    /** Lịch sử giao ca trong khoảng ngày. Nhân viên chỉ xem được ca của mình; quản lý xem tất cả (lọc theo nhân viên nếu chọn). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSach(LocalDate tuNgay, LocalDate denNgay, Integer idNhanVien) {
        NguoiDung nd = XacThuc.nhanVien();
        LocalDate den = denNgay == null ? bayGio().toLocalDate() : denNgay;
        LocalDate tu = tuNgay == null ? den.minusDays(29) : tuNgay;
        if (tu.isAfter(den)) throw ApiException.badRequest("Từ ngày phải trước hoặc cùng ngày với Đến ngày.");
        if (ChronoUnit.DAYS.between(tu, den) > 366) throw ApiException.badRequest("Mỗi lần xem tối đa 1 năm.");
        Integer loc = nd.laQuanLy() ? idNhanVien : nd.id();
        LocalDateTime a = tu.atStartOfDay();
        LocalDateTime b = den.plusDays(1).atStartOfDay().minusNanos(1);
        List<GiaoCa> ds = loc == null ? giaoCaRepo.findByThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc(a, b)
                : giaoCaRepo.findByNhanVienIdAndThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc(loc, a, b);
        return ds.stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> chiTiet(Integer id) {
        NguoiDung nd = XacThuc.nhanVien();
        GiaoCa gc = giaoCaRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy ca."));
        if (!nd.laQuanLy() && !gc.getNhanVien().getId().equals(nd.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ xem được ca của chính mình.");
        }
        return chiTietDto(gc);
    }

    /** Nhân viên đang hoạt động (chọn người nhận bàn giao). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> nhanVienNhanBanGiao() {
        Integer toi = XacThuc.nhanVien().id();
        List<Map<String, Object>> kq = new ArrayList<>();
        for (NhanVien n : nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.HOAT_DONG)) {
            if (n.getId().equals(toi)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("maNhanVien", n.getMaNhanVien());
            m.put("hoTen", n.getHoTen());
            kq.add(m);
        }
        return kq;
    }

    /* ===================== DTO / hàm phụ ===================== */

    Map<String, Object> chiTietDto(GiaoCa gc) {
        Map<String, Object> m = dto(gc);
        if (Integer.valueOf(GiaoCa.DANG_TRONG_CA).equals(gc.getTrangThai())) m.put("tongKet", tongKet(gc));
        Map<Integer, String> tenPt = new HashMap<>();
        for (PhuongThucThanhToan pt : phuongThucRepo.findAll()) tenPt.put(pt.getId(), pt.getTenPhuongThuc());
        List<Map<String, Object>> hds = new ArrayList<>();
        for (HoaDon hd : hoaDonRepo.findByIdGiaoCaOrderByNgayTaoAsc(gc.getId())) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("id", hd.getId());
            h.put("maHoaDon", hd.getMaHoaDon());
            h.put("ngayTao", hd.getNgayTao());
            h.put("tenKhach", hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : "Khách lẻ");
            h.put("thanhTien", soTien(hd.getThanhTien()));
            h.put("phuongThuc", tenPt.get(hd.getIdPhuongThucThanhToan()));
            h.put("trangThai", hd.getTrangThai());
            hds.add(h);
        }
        m.put("hoaDons", hds);
        return m;
    }

    Map<String, Object> dto(GiaoCa gc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", gc.getId());
        m.put("maGiaoCa", gc.getMaGiaoCa());
        NhanVien nv = gc.getNhanVien();
        m.put("idNhanVien", nv.getId());
        m.put("maNhanVien", nv.getMaNhanVien());
        m.put("tenNhanVien", nv.getHoTen());
        LichLamViec l = gc.getLichLamViec();
        m.put("ngoaiLich", l == null);
        m.put("tenCa", l == null ? null : l.getCaLamViec().getTenCa());
        m.put("gioCa", l == null ? null
                : l.getCaLamViec().getGioBatDau().format(LichLamViecService.GIO) + " - " + l.getCaLamViec().getGioKetThuc().format(LichLamViecService.GIO));
        m.put("thoiGianNhanCa", gc.getThoiGianNhanCa());
        m.put("tienDauCa", soTien(gc.getTienDauCa()));
        m.put("ghiChuNhanCa", gc.getGhiChuNhanCa());
        m.put("soPhutDiMuon", gc.getSoPhutDiMuon());
        m.put("diMuon", gc.getSoPhutDiMuon() != null && gc.getSoPhutDiMuon() > PHUT_MUON_CHO_PHEP);
        m.put("thoiGianKetCa", gc.getThoiGianKetCa());
        m.put("soHoaDon", gc.getSoHoaDon());
        m.put("doanhThu", gc.getDoanhThu());
        m.put("tienMatThu", gc.getTienMatThu());
        m.put("chuyenKhoanThu", gc.getChuyenKhoanThu());
        m.put("tienMatHeThong", gc.getTienMatHeThong());
        m.put("tienMatThucTe", gc.getTienMatThucTe());
        m.put("chenhLech", gc.getChenhLech());
        m.put("tienBanGiao", gc.getTienBanGiao());
        m.put("tenNguoiNhanBanGiao", gc.getNhanVienNhanBanGiao() == null ? null : gc.getNhanVienNhanBanGiao().getHoTen());
        m.put("ghiChuKetCa", gc.getGhiChuKetCa());
        m.put("trangThai", gc.getTrangThai());
        m.put("dangTrongCa", Integer.valueOf(GiaoCa.DANG_TRONG_CA).equals(gc.getTrangThai()));
        return m;
    }

    private static void kiemTraTien(BigDecimal tien, String truong, String ten, LoiForm loi) {
        if (tien == null) loi.them(truong, "Vui lòng nhập " + ten + ".");
        else if (tien.signum() < 0) loi.them(truong, "Số tiền không được âm.");
        else if (tien.stripTrailingZeros().scale() > 0) loi.them(truong, "Số tiền phải là số nguyên (đồng).");
        else if (tien.compareTo(BigDecimal.valueOf(TIEN_TOI_DA)) > 0) loi.them(truong, "Số tiền tối đa 1.000.000.000 ₫.");
    }

    private static String gon(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    static BigDecimal soTien(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 1500000 -> "1.500.000 ₫" (không phụ thuộc ngôn ngữ của máy). */
    static String dinhDangTien(BigDecimal v) {
        String so = v.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
        boolean am = so.startsWith("-");
        if (am) so = so.substring(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < so.length(); i++) {
            if (i > 0 && (so.length() - i) % 3 == 0) sb.append('.');
            sb.append(so.charAt(i));
        }
        return (am ? "-" : "") + sb + " ₫";
    }
}
