package com.footstyle.demo.service;

import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.CaLamViecRepository;
import com.footstyle.demo.repository.GiaoCaRepository;
import com.footstyle.demo.repository.LichLamViecRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.security.XacThuc;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ca làm việc (định nghĩa ca) + lịch làm việc (xếp ca cho nhân viên theo ngày).
 * Luật xếp lịch: chỉ nhân viên đang hoạt động, chỉ ca đang dùng; không xếp / xóa lịch của ngày đã qua;
 * một người không có 2 ca chồng giờ trong cùng ngày; không trùng (người, ca, ngày).
 */
@Service
@RequiredArgsConstructor
public class LichLamViecService {

    static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    /** Một lần xếp lịch tối đa bấy nhiêu ô (nhân viên x ngày), tránh gửi nhầm cả năm. */
    static final int SO_O_TOI_DA = 500;
    /** Xem lịch tối đa 62 ngày một lần (2 tháng). */
    static final int SO_NGAY_XEM_TOI_DA = 62;
    static final Duration CA_NGAN_NHAT = Duration.ofMinutes(30);

    private final CaLamViecRepository caRepo;
    private final LichLamViecRepository lichRepo;
    private final NhanVienRepository nhanVienRepo;
    private final GiaoCaRepository giaoCaRepo;

    private Clock dongHo = Clock.systemDefaultZone();

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    LocalDate homNay() {
        return LocalDate.now(dongHo);
    }

    /* ===================== Ca làm việc ===================== */

    public record CaRequest(String tenCa, String gioBatDau, String gioKetThuc) {
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSachCa() {
        return caRepo.findAllByOrderByGioBatDauAscIdAsc().stream().map(LichLamViecService::caDto).toList();
    }

    @Transactional
    public Map<String, Object> themCa(CaRequest req) {
        CaLamViec ca = new CaLamViec();
        napCa(ca, req, -1);
        ca.setMaCa(MaUtil.tiepTheo("CA", caRepo.findAllMaCa()));
        ca.setTrangThai(CaLamViec.HOAT_DONG);
        return caDto(caRepo.save(ca));
    }

    @Transactional
    public Map<String, Object> suaCa(Integer id, CaRequest req) {
        CaLamViec ca = timCa(id);
        napCa(ca, req, ca.getId());
        return caDto(caRepo.save(ca));
    }

    /** Bật / tắt ca (ca ngừng dùng không xếp lịch mới được, lịch cũ vẫn giữ). */
    @Transactional
    public Map<String, Object> doiTrangThaiCa(Integer id) {
        CaLamViec ca = timCa(id);
        ca.setTrangThai(Integer.valueOf(CaLamViec.HOAT_DONG).equals(ca.getTrangThai()) ? CaLamViec.NGUNG_HOAT_DONG : CaLamViec.HOAT_DONG);
        return caDto(caRepo.save(ca));
    }

    private void napCa(CaLamViec ca, CaRequest req, Integer idBoQua) {
        LoiForm loi = new LoiForm();
        String ten = req == null || req.tenCa() == null ? "" : req.tenCa().trim().replaceAll("\\s+", " ");
        if (ten.isEmpty()) loi.them("tenCa", "Vui lòng nhập tên ca.");
        else if (ten.length() > 100) loi.them("tenCa", "Tên ca tối đa 100 ký tự.");
        else if (caRepo.existsByTenCaIgnoreCaseAndIdNot(ten, idBoQua)) loi.trung("tenCa", "Tên ca đã tồn tại.");
        LocalTime bd = docGio(req == null ? null : req.gioBatDau(), "gioBatDau", "giờ bắt đầu", loi);
        LocalTime kt = docGio(req == null ? null : req.gioKetThuc(), "gioKetThuc", "giờ kết thúc", loi);
        if (bd != null && kt != null) {
            if (!bd.isBefore(kt)) loi.them("gioKetThuc", "Giờ kết thúc phải sau giờ bắt đầu (không có ca qua đêm).");
            else if (Duration.between(bd, kt).compareTo(CA_NGAN_NHAT) < 0) loi.them("gioKetThuc", "Một ca dài ít nhất 30 phút.");
        }
        loi.nemNeuCoLoi();
        ca.setTenCa(ten);
        ca.setGioBatDau(bd);
        ca.setGioKetThuc(kt);
    }

    static LocalTime docGio(String s, String truong, String ten, LoiForm loi) {
        if (s == null || s.isBlank()) {
            loi.them(truong, "Vui lòng nhập " + ten + ".");
            return null;
        }
        try {
            return LocalTime.parse(s.trim()).truncatedTo(ChronoUnit.MINUTES); // nhận "07:30" hoặc "07:30:00"
        } catch (DateTimeParseException e) {
            loi.them(truong, "Giờ không hợp lệ, định dạng HH:mm (vd 07:30).");
            return null;
        }
    }

    private CaLamViec timCa(Integer id) {
        return caRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy ca làm việc."));
    }

    static Map<String, Object> caDto(CaLamViec c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("maCa", c.getMaCa());
        m.put("tenCa", c.getTenCa());
        m.put("gioBatDau", c.getGioBatDau() == null ? null : c.getGioBatDau().format(GIO));
        m.put("gioKetThuc", c.getGioKetThuc() == null ? null : c.getGioKetThuc().format(GIO));
        m.put("hoatDong", Integer.valueOf(CaLamViec.HOAT_DONG).equals(c.getTrangThai()));
        return m;
    }

    /* ===================== Lịch làm việc ===================== */

    public record XepLichRequest(List<Integer> idNhanViens, Integer idCa, List<String> ngays, String ghiChu) {
    }

    /** Lịch trong khoảng ngày (idNhanVien = null: mọi nhân viên). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> lich(LocalDate tuNgay, LocalDate denNgay, Integer idNhanVien) {
        kiemTraKhoang(tuNgay, denNgay);
        List<LichLamViec> ds = idNhanVien == null
                ? lichRepo.findByNgayLamBetween(tuNgay, denNgay)
                : lichRepo.findByNhanVienIdAndNgayLamBetween(idNhanVien, tuNgay, denNgay);
        return ds.stream()
                .sorted(Comparator.comparing(LichLamViec::getNgayLam)
                        .thenComparing(l -> l.getCaLamViec().getGioBatDau())
                        .thenComparing(l -> l.getNhanVien().getHoTen()))
                .map(this::lichDto)
                .toList();
    }

    /** Lịch của nhân viên đang đăng nhập. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> lichCuaToi(LocalDate tuNgay, LocalDate denNgay) {
        return lich(tuNgay, denNgay, XacThuc.nhanVien().id());
    }

    /** Nhân viên đang hoạt động (các hàng của bảng xếp lịch). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> nhanVienDangLam() {
        List<Map<String, Object>> kq = new ArrayList<>();
        for (NhanVien n : nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.HOAT_DONG)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("maNhanVien", n.getMaNhanVien());
            m.put("hoTen", n.getHoTen());
            m.put("vaiTro", n.getVaiTro() == null ? null : n.getVaiTro().getTenVaiTro());
            kq.add(m);
        }
        return kq;
    }

    /**
     * Xếp lịch: mỗi nhân viên x mỗi ngày được chọn x một ca. Ô nào không hợp lệ thì bỏ qua và ghi lý do (không làm hỏng cả lần xếp).
     * Trả về { daThem, boQua: [{ hoTen, ngay, lyDo }] }.
     */
    @Transactional
    public Map<String, Object> xepLich(XepLichRequest req) {
        LoiForm loi = new LoiForm();
        List<Integer> idNvs = req == null || req.idNhanViens() == null ? List.of()
                : req.idNhanViens().stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (idNvs.isEmpty()) loi.them("idNhanViens", "Chọn ít nhất một nhân viên.");
        if (req == null || req.idCa() == null) loi.them("idCa", "Chọn ca làm việc.");
        Set<LocalDate> ngays = new LinkedHashSet<>();
        if (req == null || req.ngays() == null || req.ngays().isEmpty()) {
            loi.them("ngays", "Chọn ít nhất một ngày.");
        } else {
            for (String s : req.ngays()) {
                try {
                    ngays.add(LocalDate.parse(s.trim()));
                } catch (RuntimeException e) {
                    loi.them("ngays", "Ngày không hợp lệ: " + s + " (định dạng yyyy-MM-dd).");
                }
            }
        }
        String ghiChu = req == null || req.ghiChu() == null ? null : req.ghiChu().trim();
        if (ghiChu != null && ghiChu.length() > 255) loi.them("ghiChu", "Ghi chú tối đa 255 ký tự.");
        if (!loi.coLoi() && idNvs.size() * ngays.size() > SO_O_TOI_DA) {
            loi.them("ngays", "Mỗi lần xếp tối đa " + SO_O_TOI_DA + " ô (số nhân viên x số ngày).");
        }
        loi.nemNeuCoLoi();

        CaLamViec ca = timCa(req.idCa());
        if (!Integer.valueOf(CaLamViec.HOAT_DONG).equals(ca.getTrangThai())) {
            throw ApiException.badRequest("Ca \"" + ca.getTenCa() + "\" đang ngừng sử dụng, không xếp lịch mới được.");
        }
        int daThem = 0;
        List<Map<String, Object>> boQua = new ArrayList<>();
        for (Integer idNv : idNvs) {
            NhanVien nv = nhanVienRepo.findById(idNv).orElse(null);
            for (LocalDate ngay : ngays) {
                String lyDo = lyDoKhongXepDuoc(nv, ca, ngay);
                if (lyDo != null) {
                    boQua.add(Map.of("hoTen", nv == null ? "Nhân viên #" + idNv : nv.getHoTen(), "ngay", ngay.toString(), "lyDo", lyDo));
                    continue;
                }
                LichLamViec l = new LichLamViec();
                l.setNhanVien(nv);
                l.setCaLamViec(ca);
                l.setNgayLam(ngay);
                l.setGhiChu(ghiChu == null || ghiChu.isEmpty() ? null : ghiChu);
                l.setTrangThai(1);
                lichRepo.save(l);
                daThem++;
            }
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("daThem", daThem);
        kq.put("boQua", boQua);
        return kq;
    }

    /** Lý do không xếp được một ô (null = xếp được). Dùng chung cho xếp lịch và sao chép tuần. */
    String lyDoKhongXepDuoc(NhanVien nv, CaLamViec ca, LocalDate ngay) {
        if (nv == null) return "Không tìm thấy nhân viên.";
        if (!Integer.valueOf(NhanVien.HOAT_DONG).equals(nv.getTrangThai())) return "Nhân viên đang ngừng hoạt động.";
        if (ngay.isBefore(homNay())) return "Ngày đã qua, không xếp lịch được.";
        for (LichLamViec co : lichRepo.findByNhanVienIdAndNgayLam(nv.getId(), ngay)) {
            CaLamViec khac = co.getCaLamViec();
            if (khac.getId().equals(ca.getId())) return "Đã có lịch " + ca.getTenCa() + " ngày này.";
            if (chongGio(ca, khac)) {
                return "Trùng giờ với " + khac.getTenCa() + " (" + khac.getGioBatDau().format(GIO) + " - " + khac.getGioKetThuc().format(GIO) + ").";
            }
        }
        return null;
    }

    /** Hai ca chồng giờ nhau (07:00-12:00 và 12:00-17:00 KHÔNG chồng: ca trước hết đúng lúc ca sau bắt đầu). */
    static boolean chongGio(CaLamViec a, CaLamViec b) {
        return a.getGioBatDau().isBefore(b.getGioKetThuc()) && b.getGioBatDau().isBefore(a.getGioKetThuc());
    }

    /** Xóa một ô lịch (chỉ ngày hôm nay trở đi, và chưa có ai nhận ca theo ô này). */
    @Transactional
    public void xoaLich(Integer id) {
        LichLamViec l = lichRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy lịch làm việc."));
        if (l.getNgayLam().isBefore(homNay())) throw ApiException.badRequest("Không xóa được lịch của ngày đã qua.");
        if (giaoCaRepo.existsByLichLamViecId(l.getId())) {
            throw ApiException.conflict("Nhân viên đã nhận ca theo lịch này nên không xóa được.");
        }
        lichRepo.delete(l);
    }

    /**
     * Sao chép lịch tuần nguồn sang tuần đích (cả hai là ngày thứ 2). Ô của ngày đã qua, trùng hoặc chồng giờ, nhân viên đã nghỉ thì bỏ qua.
     * Trả về { daThem, boQua: [...] }.
     */
    @Transactional
    public Map<String, Object> saoChepTuan(LocalDate tuanNguon, LocalDate tuanDich) {
        if (tuanNguon == null || tuanDich == null) throw ApiException.badRequest("Thiếu tuần nguồn / tuần đích.");
        if (tuanNguon.getDayOfWeek() != DayOfWeek.MONDAY || tuanDich.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw ApiException.badRequest("Tuần phải bắt đầu từ thứ 2.");
        }
        if (tuanNguon.equals(tuanDich)) throw ApiException.badRequest("Tuần nguồn và tuần đích phải khác nhau.");
        if (tuanDich.plusDays(6).isBefore(homNay())) throw ApiException.badRequest("Tuần đích đã qua, không sao chép được.");
        long lech = ChronoUnit.DAYS.between(tuanNguon, tuanDich);
        int daThem = 0;
        List<Map<String, Object>> boQua = new ArrayList<>();
        List<LichLamViec> nguon = new ArrayList<>(lichRepo.findByNgayLamBetween(tuanNguon, tuanNguon.plusDays(6)));
        nguon.sort(Comparator.comparing(LichLamViec::getNgayLam).thenComparing(l -> l.getCaLamViec().getGioBatDau()));
        if (nguon.isEmpty()) throw ApiException.badRequest("Tuần nguồn chưa có lịch nào để sao chép.");
        for (LichLamViec l : nguon) {
            LocalDate ngay = l.getNgayLam().plusDays(lech);
            CaLamViec ca = l.getCaLamViec();
            String lyDo = !Integer.valueOf(CaLamViec.HOAT_DONG).equals(ca.getTrangThai())
                    ? "Ca " + ca.getTenCa() + " đang ngừng sử dụng."
                    : lyDoKhongXepDuoc(l.getNhanVien(), ca, ngay);
            if (lyDo != null) {
                boQua.add(Map.of("hoTen", l.getNhanVien().getHoTen(), "ngay", ngay.toString(), "lyDo", lyDo));
                continue;
            }
            LichLamViec moi = new LichLamViec();
            moi.setNhanVien(l.getNhanVien());
            moi.setCaLamViec(ca);
            moi.setNgayLam(ngay);
            moi.setGhiChu(l.getGhiChu());
            moi.setTrangThai(1);
            lichRepo.save(moi);
            daThem++;
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("daThem", daThem);
        kq.put("boQua", boQua);
        return kq;
    }

    private void kiemTraKhoang(LocalDate tu, LocalDate den) {
        if (tu == null || den == null) throw ApiException.badRequest("Chọn khoảng ngày cần xem.");
        if (tu.isAfter(den)) throw ApiException.badRequest("Từ ngày phải trước hoặc cùng ngày với Đến ngày.");
        if (ChronoUnit.DAYS.between(tu, den) >= SO_NGAY_XEM_TOI_DA) {
            throw ApiException.badRequest("Mỗi lần xem tối đa " + SO_NGAY_XEM_TOI_DA + " ngày.");
        }
    }

    Map<String, Object> lichDto(LichLamViec l) {
        Map<String, Object> m = new LinkedHashMap<>();
        CaLamViec ca = l.getCaLamViec();
        NhanVien nv = l.getNhanVien();
        m.put("id", l.getId());
        m.put("ngayLam", l.getNgayLam().toString());
        m.put("idNhanVien", nv.getId());
        m.put("maNhanVien", nv.getMaNhanVien());
        m.put("hoTen", nv.getHoTen());
        m.put("idCa", ca.getId());
        m.put("maCa", ca.getMaCa());
        m.put("tenCa", ca.getTenCa());
        m.put("gioBatDau", ca.getGioBatDau().format(GIO));
        m.put("gioKetThuc", ca.getGioKetThuc().format(GIO));
        m.put("ghiChu", l.getGhiChu());
        m.put("daQua", l.getNgayLam().isBefore(homNay()));
        return m;
    }
}
