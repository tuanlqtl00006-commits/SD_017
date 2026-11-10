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
import java.time.format.DateTimeFormatter;
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
 * Giao ca: nhận ca (tiền đầu ca) -> bán hàng -> kết ca (đếm tiền, bàn giao cho ca sau).
 * Mỗi nhân viên chỉ có 1 ca đang mở; tiền lệch so với hệ thống thì bắt buộc ghi lý do.
 */
@Service
@RequiredArgsConstructor
public class GiaoCaService {

    public static final long TIEN_TOI_DA = 1_000_000_000L;
    /** Đến muộn quá bấy nhiêu phút thì đánh dấu "Đi muộn" trên giao diện. */
    public static final int PHUT_MUON_CHO_PHEP = 15;
    /** Nhận ca sớm hơn giờ bắt đầu tối đa bấy nhiêu phút vẫn tính là ca đó. */
    static final int PHUT_DEN_SOM = 15;
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

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

    /**
     * chuyenKhoanCaTruoc: số tiền chuyển khoản của ca trước mà người vào ca xác nhận (đối chiếu sao kê ngân hàng).
     * Có ca trước thì bắt buộc nhập; khác số hệ thống ghi nhận thì bắt buộc ghi lý do (giống tiền mặt).
     */
    public record NhanCaRequest(BigDecimal tienDauCa, String ghiChu, Integer idLichLamViec, BigDecimal chuyenKhoanCaTruoc) {
        public NhanCaRequest(BigDecimal tienDauCa, String ghiChu, Integer idLichLamViec) {
            this(tienDauCa, ghiChu, idLichLamViec, null);
        }
    }

    public record KetCaRequest(BigDecimal tienMatThucTe, BigDecimal tienBanGiao, Integer idNhanVienNhanBanGiao, String ghiChu) {
    }

    /* ===================== Ca hiện tại ===================== */

    /** { dangMo: ca đang mở | null, tongKet (nếu đang mở), goiYTienDauCa, nguoiBanGiao, lichHomNay: [...] } của người đang đăng nhập. */
    @Transactional(readOnly = true)
    public Map<String, Object> hienTai() {
        NguoiDung nd = XacThuc.nhanVien();
        Map<String, Object> kq = new LinkedHashMap<>();
        // Quản lý (admin) có toàn quyền, không nhận ca / giao ca (chốt 04/11): kể cả còn ca cũ mở từ trước (vd GC143) cũng không coi là
        // đang trong ca, nên màn bán hàng không nhắc quản lý giao ca.
        Optional<GiaoCa> mo = nd.laQuanLy() ? Optional.empty() : giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA);
        kq.put("dangMo", mo.map(this::dto).orElse(null));
        kq.put("laNguoiMoCa", mo.isPresent());
        kq.put("tongKet", mo.map(this::tongKet).orElse(null));
        // Chưa vào ca mà đang có ca của người khác mở thì chưa vào ca được (trang Vào ca báo trước); quản lý không cần vào ca
        kq.put("caKhacDangMo", mo.isPresent() || nd.laQuanLy() ? null : giaoCaRepo.findFirstByTrangThaiOrderByThoiGianNhanCaAsc(GiaoCa.DANG_TRONG_CA).map(this::dto).orElse(null));
        Optional<GiaoCa> truoc = caTruocChuaNhan();
        kq.put("goiYTienDauCa", truoc.map(g -> soTien(g.getTienBanGiao())).orElse(null));
        kq.put("nguoiBanGiao", truoc.map(g -> g.getNhanVien().getHoTen()).orElse(null));
        kq.put("thoiGianBanGiao", truoc.map(GiaoCa::getThoiGianKetCa).orElse(null));
        kq.put("maCaTruoc", truoc.map(GiaoCa::getMaGiaoCa).orElse(null));
        kq.put("tienMatCaTruoc", truoc.map(g -> soTien(g.getTienMatThucTe())).orElse(null));
        kq.put("chuyenKhoanCaTruoc", truoc.map(g -> soTien(g.getChuyenKhoanThu())).orElse(null));
        List<Map<String, Object>> lich = new ArrayList<>();
        for (LichLamViec l : lichHomNay(nd.id())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("tenCa", l.getCaLamViec().getTenCa());
            m.put("gioBatDau", l.getCaLamViec().getGioBatDau().format(LichLamViecService.GIO));
            m.put("gioKetThuc", l.getCaLamViec().getGioKetThuc().format(LichLamViecService.GIO));
            m.put("daGiao", giaoCaRepo.existsByLichLamViecId(l.getId())); // đã giao (kể cả quản lý đóng hộ) thì không vào lại
            lich.add(m);
        }
        kq.put("lichHomNay", lich);
        return kq;
    }

    /**
     * Ca đang mở của nhân viên (bán hàng tại quầy bắt buộc có). Chưa nhận ca thì 409 CHUA_NHAN_CA;
     * ca nhận từ hôm trước mà quên giao thì 409 CA_CU_CHUA_GIAO (không gắn hóa đơn hôm nay vào ca cũ).
     */
    @Transactional(readOnly = true)
    public GiaoCa caDangMoBatBuoc(Integer idNhanVien) {
        // Mỗi ca một nhân viên: người bán phải đang giữ ca của chính mình (không bán nhờ ca của người khác)
        GiaoCa gc = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(idNhanVien, GiaoCa.DANG_TRONG_CA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "Bạn chưa nhận ca. Vui lòng nhận ca (nhập tiền đầu ca) trước khi bán hàng.", "CHUA_NHAN_CA", null));
        if (gc.getThoiGianNhanCa() != null && gc.getThoiGianNhanCa().toLocalDate().isBefore(bayGio().toLocalDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ca " + gc.getMaGiaoCa() + " nhận ngày " + gc.getThoiGianNhanCa().format(NGAY)
                    + " chưa giao. Vui lòng giao ca đó rồi vào ca mới trước khi bán hàng.", "CA_CU_CHUA_GIAO", null);
        }
        return gc;
    }

    /**
     * Ca gắn cho hóa đơn bán tại quầy. Quản lý có toàn quyền nên không bắt nhận ca / giao ca:
     * tiền quản lý thu bỏ chung một két nên hóa đơn gắn vào ca đang mở trong hôm nay (để giao ca két vẫn khớp);
     * không có ca nào mở thì hóa đơn không thuộc ca nào (null). Nhân viên vẫn bắt buộc như {@link #caDangMoBatBuoc}.
     */
    @Transactional(readOnly = true)
    public GiaoCa caBanHang(NguoiDung nd) {
        if (!nd.laQuanLy()) return caDangMoBatBuoc(nd.id());
        return giaoCaRepo.findFirstByTrangThaiOrderByThoiGianNhanCaAsc(GiaoCa.DANG_TRONG_CA)
                .filter(gc -> gc.getThoiGianNhanCa() == null || !gc.getThoiGianNhanCa().toLocalDate().isBefore(bayGio().toLocalDate()))
                .orElse(null);
    }

    /** Lần giao ca gần nhất mà tiền bàn giao chưa có ca nào nhận (cửa hàng một két tiền: mỗi lần bàn giao chỉ cho một ca sau). */
    private Optional<GiaoCa> caTruocChuaNhan() {
        Optional<GiaoCa> truoc = giaoCaRepo.findFirstByTrangThaiOrderByThoiGianKetCaDescIdDesc(GiaoCa.DA_GIAO_CA);
        if (truoc.isPresent() && truoc.get().getThoiGianKetCa() != null
                && giaoCaRepo.existsByThoiGianNhanCaGreaterThanEqual(truoc.get().getThoiGianKetCa())) {
            return Optional.empty();
        }
        return truoc;
    }

    /* ===================== Nhận ca ===================== */

    @Transactional
    public Map<String, Object> nhanCa(NhanCaRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
if (nd.laQuanLy()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Quản lý không cần vào ca / giao ca. Ca làm việc chỉ dành cho nhân viên bán hàng.");
        }
        Optional<GiaoCa> dangMo = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA);
        if (dangMo.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bạn đang trong ca " + dangMo.get().getMaGiaoCa() + ", vui lòng giao ca trước khi nhận ca mới.",
                    "DANG_TRONG_CA", null);
        }
        // Cửa hàng một két tiền: ca trước phải giao (hoặc quản lý đóng ca hộ) thì ca sau mới vào được
        Optional<GiaoCa> caKhac = giaoCaRepo.findFirstByTrangThaiOrderByThoiGianNhanCaAsc(GiaoCa.DANG_TRONG_CA);
        if (caKhac.isPresent()) {
            GiaoCa k = caKhac.get();
            throw new ApiException(HttpStatus.CONFLICT, "Ca " + k.getMaGiaoCa() + " của " + k.getNhanVien().getHoTen() + " (nhận lúc "
                    + k.getThoiGianNhanCa().format(GIO_NGAY) + ") chưa giao. Người ca trước giao ca, hoặc quản lý đóng ca hộ, rồi bạn mới vào ca được.",
                    "CA_KHAC_DANG_MO", Map.of("idGiaoCa", k.getId(), "maGiaoCa", k.getMaGiaoCa()));
        }
        NhanVien nv = nhanVienRepo.findById(nd.id()).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên."));
        LocalDateTime now = bayGio();

        LoiForm loi = new LoiForm();
        BigDecimal tien = req == null ? null : req.tienDauCa();
        kiemTraTien(tien, "tienDauCa", "tiền đầu ca", loi);
        String ghiChu = gon(req == null ? null : req.ghiChu());
        if (ghiChu != null && ghiChu.length() > 500) loi.them("ghiChu", "Ghi chú tối đa 500 ký tự.");
        Optional<GiaoCa> caTruoc = caTruocChuaNhan();
        BigDecimal goiY = caTruoc.map(g -> soTien(g.getTienBanGiao())).orElse(null);
        if (!loi.co("tienDauCa") && goiY != null && tien.compareTo(goiY) != 0 && ghiChu == null) {
            loi.them("ghiChu", "Tiền đầu ca khác tiền ca trước bàn giao (" + dinhDangTien(goiY) + "). Vui lòng ghi lý do.");
        }
        // Đối soát chuyển khoản của ca trước
        if (caTruoc.isPresent()) {
            BigDecimal ck = req == null ? null : req.chuyenKhoanCaTruoc();
            BigDecimal ckHeThong = soTien(caTruoc.get().getChuyenKhoanThu());
            kiemTraTien(ck, "chuyenKhoanCaTruoc", "số tiền chuyển khoản của ca trước", loi);
            if (!loi.co("chuyenKhoanCaTruoc") && ck.compareTo(ckHeThong) != 0 && ghiChu == null) {
                loi.them("ghiChu", "Chuyển khoản ca trước khác số hệ thống ghi nhận (" + dinhDangTien(ckHeThong) + "). Vui lòng ghi lý do.");
            }
        }
        LichLamViec lich = null;
        if (req != null && req.idLichLamViec() != null) {
            lich = lichRepo.findById(req.idLichLamViec()).orElse(null);
            if (lich == null || !lich.getNhanVien().getId().equals(nd.id()) || !lich.getNgayLam().equals(now.toLocalDate())) {
                loi.them("idLichLamViec", "Ca được chọn không phải lịch làm hôm nay của bạn.");
                lich = null;
            } else if (giaoCaRepo.existsByLichLamViecId(lich.getId())) {
                // Giao ca rồi (hoặc quản lý đóng hộ) thì không tự vào lại; làm thêm giờ thì quản lý xếp thêm lịch
                loi.them("idLichLamViec", lich.getCaLamViec().getTenCa() + " hôm nay của bạn đã giao rồi, không vào lại được. "
                        + "Cần làm thêm giờ thì nhờ quản lý xếp thêm lịch cho bạn.");
                lich = null;
            } else {
                // Chỉ nhận ca trong khoảng từ 15 phút trước giờ bắt đầu tới giờ kết thúc
                CaLamViec ca = lich.getCaLamViec();
                LocalDateTime tu = lich.getNgayLam().atTime(ca.getGioBatDau()).minusMinutes(PHUT_DEN_SOM);
                LocalDateTime den = lich.getNgayLam().atTime(ca.getGioKetThuc());
                if (now.isBefore(tu) || !now.isBefore(den)) {
                    loi.them("idLichLamViec", ca.getTenCa() + " nhận được từ " + tu.format(LichLamViecService.GIO) + " đến "
                            + den.format(LichLamViecService.GIO) + " (sớm nhất " + PHUT_DEN_SOM + " phút trước giờ bắt đầu).");
                    lich = null;
                }
            }
        }
        loi.nemNeuCoLoi();
        if (lich == null) lich = chonLichTuDong(nd.id(), now);
        if (lich == null) chanNgoaiLichNeuCoLich(nd.id());

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
            throw new ApiException(HttpStatus.CONFLICT, "Bạn vừa nhận ca ở một cửa sổ khác. Vui lòng tải lại trang.", "DANG_TRONG_CA", null);
        }
        return dto(gc);
    }

    /**
     * Hôm nay nhân viên có lịch làm mà không chọn được ca nào thì không cho vào "ngoài lịch":
     * ca đã giao rồi, chưa tới giờ nhận (sớm nhất 15 phút trước giờ bắt đầu) hoặc đã hết giờ.
     * Chỉ người hôm nay không có lịch (làm đột xuất) mới được ghi ca ngoài lịch.
     */
    private void chanNgoaiLichNeuCoLich(Integer idNhanVien) {
        List<LichLamViec> ds = lichHomNay(idNhanVien);
        if (ds.isEmpty()) return;
        List<LichLamViec> chuaGiao = ds.stream().filter(l -> !giaoCaRepo.existsByLichLamViecId(l.getId())).toList();
        String ngoaiLich = "Cần làm thêm giờ hoặc làm ngoài lịch thì nhờ quản lý xếp thêm lịch cho bạn.";
        String msg;
        if (chuaGiao.isEmpty()) {
            msg = "Các ca trong lịch hôm nay của bạn (" + moTaCaLich(ds) + ") đã giao rồi, không vào lại được. " + ngoaiLich;
        } else {
            msg = "Hôm nay bạn có lịch " + moTaCaLich(chuaGiao) + ". Chỉ nhận ca được từ " + PHUT_DEN_SOM
                    + " phút trước giờ bắt đầu tới hết giờ ca, nên lúc này chưa vào ca được. " + ngoaiLich;
        }
        throw new ApiException(HttpStatus.CONFLICT, msg, "NGOAI_LICH_BI_CHAN", null);
    }

    private static String moTaCaLich(List<LichLamViec> ds) {
        return ds.stream().map(l -> l.getCaLamViec().getTenCa() + " " + l.getCaLamViec().getGioBatDau().format(LichLamViecService.GIO)
                + " - " + l.getCaLamViec().getGioKetThuc().format(LichLamViecService.GIO)).collect(java.util.stream.Collectors.joining(", "));
    }

    /** Lịch hôm nay của nhân viên, sắp theo giờ bắt đầu. */
    List<LichLamViec> lichHomNay(Integer idNhanVien) {
        List<LichLamViec> ds = new ArrayList<>(lichRepo.findByNhanVienIdAndNgayLam(idNhanVien, bayGio().toLocalDate()));
        ds.sort(Comparator.comparing(l -> l.getCaLamViec().getGioBatDau()));
        return ds;
    }

    /** Ca chưa giao trong lịch hôm nay đang diễn ra (cho phép nhận sớm 15 phút); không có thì null. */
    LichLamViec chonLichTuDong(Integer idNhanVien, LocalDateTime now) {
        for (LichLamViec l : lichHomNay(idNhanVien)) {
            if (giaoCaRepo.existsByLichLamViecId(l.getId())) continue; // ca này đã giao rồi
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
        // Mỗi ca một nhân viên làm hết: người nhận ca tự bán, giữ két, đếm tiền và giao ca của chính mình
        NguoiDung nd = XacThuc.nhanVien();
        GiaoCa gc = giaoCaRepo.findFirstByNhanVienIdAndTrangThai(nd.id(), GiaoCa.DANG_TRONG_CA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Bạn không có ca nào đang mở để giao.", "CHUA_NHAN_CA", null));
        return ketCaCua(gc, req, null);
    }

    /**
     * Quản lý đóng ca hộ khi người trong ca đã về mà quên giao ca: quản lý đếm két và nhập như khi giao ca.
     * Hóa đơn chờ của ca đó phải hủy trước (POST /api/giao-ca/{id}/huy-hoa-don-cho).
     */
    @Transactional
    public Map<String, Object> dongCaHo(Integer idGiaoCa, KetCaRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        if (!nd.laQuanLy()) throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ quản lý được đóng ca hộ.");
        GiaoCa gc = idGiaoCa == null ? null : giaoCaRepo.findById(idGiaoCa).orElse(null);
        if (gc == null) throw ApiException.notFound("Không tìm thấy ca.");
        if (!Integer.valueOf(GiaoCa.DANG_TRONG_CA).equals(gc.getTrangThai())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ca " + gc.getMaGiaoCa() + " đã giao rồi. Vui lòng tải lại trang.", "CA_DA_GIAO", null);
        }
        return ketCaCua(gc, req, "Quản lý " + nd.hoTen() + " đóng ca hộ lúc " + bayGio().format(GIO_NGAY) + ".");
    }

    /** Giao ca gc (người trong ca tự giao, hoặc quản lý đóng hộ khi ghiChuDongHo khác null). */
    private Map<String, Object> ketCaCua(GiaoCa gc, KetCaRequest req, String ghiChuDongHo) {
        long soCho = hoaDonRepo.findByIdGiaoCaOrderByNgayTaoAsc(gc.getId()).stream().filter(HoaDonChoService::laHoaDonCho).count();
        if (soCho > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Còn " + soCho + " hóa đơn chờ chưa thanh toán (đang giữ hàng trong kho). "
                    + (ghiChuDongHo == null ? "Vui lòng thanh toán hoặc hủy trước khi giao ca." : "Vui lòng hủy các hóa đơn chờ này trước khi đóng ca hộ."),
                    "CON_HOA_DON_CHO", Map.of("soHoaDonCho", soCho));
        }
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
            } else if (nguoiNhan.getId().equals(gc.getNhanVien().getId())) {
                loi.them("idNhanVienNhanBanGiao", "Không được bàn giao cho chính người trong ca.");
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
        gc.setGhiChuKetCa(ghiChuDongHo == null ? ghiChu : ghiChu == null ? ghiChuDongHo : ghiChuDongHo + " " + ghiChu);
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
            if (Integer.valueOf(HoaDon.DA_HUY).equals(hd.getTrangThai()) || HoaDonChoService.laHoaDonCho(hd)) continue; // bỏ đơn hủy + hóa đơn chờ chưa thanh toán
            soHoaDon++;
            // Doanh thu không gồm phí giao hàng (giống Thống kê); tiền mặt / chuyển khoản bên dưới vẫn tính đủ số khách trả
            doanhThu = doanhThu.add(soTien(hd.getThanhTien()).subtract(soTien(hd.getPhiVanChuyen())));
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
        // Mọi ca nhận từ đầu khoảng trở đi (kể cả của người khác / sau khoảng lọc) để tìm ca nối tiếp; một câu truy vấn cho cả danh sách
        List<GiaoCa> tangDan = ds.isEmpty() ? List.of() : caNhanTuLuc(a);
        return ds.stream().map(g -> {
            Map<String, Object> m = dto(g);
            m.put("caSau", caSau(g, tangDan));
            return m;
        }).toList();
    }

    /** Các ca nhận từ thời điểm `luc` trở đi, xếp theo giờ nhận tăng dần. */
    private List<GiaoCa> caNhanTuLuc(LocalDateTime luc) {
        List<GiaoCa> ds = new ArrayList<>(giaoCaRepo.findByThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc(luc, bayGio().plusYears(1)));
        ds.sort(Comparator.comparing(GiaoCa::getThoiGianNhanCa).thenComparing(GiaoCa::getId));
        return ds;
    }

    /**
     * Ca nối tiếp (review: "nhân viên nhận ca đến hết, xong rồi tiếp tục đến ai"): cửa hàng một két tiền nên ca sau là ca nhận
     * sớm nhất kể từ lúc ca này giao. Ca chưa giao, hoặc chưa có ai nhận tiếp thì null.
     */
    private static Map<String, Object> caSau(GiaoCa gc, List<GiaoCa> tangDan) {
        if (gc.getThoiGianKetCa() == null) return null;
        for (GiaoCa g : tangDan) {
            if (g.getId().equals(gc.getId()) || g.getThoiGianNhanCa().isBefore(gc.getThoiGianKetCa())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("maGiaoCa", g.getMaGiaoCa());
            m.put("tenNhanVien", g.getNhanVien().getHoTen());
            m.put("maNhanVien", g.getNhanVien().getMaNhanVien());
            m.put("thoiGianNhanCa", g.getThoiGianNhanCa());
            return m;
        }
        return null;
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
        m.put("caSau", gc.getThoiGianKetCa() == null ? null : caSau(gc, caNhanTuLuc(gc.getThoiGianKetCa())));
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
