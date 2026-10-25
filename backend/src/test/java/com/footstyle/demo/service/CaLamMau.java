package com.footstyle.demo.service;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.PhuongThucThanhToan;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.repository.CaLamViecRepository;
import com.footstyle.demo.repository.GiaoCaRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichLamViecRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.PhuongThucThanhToanRepository;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.security.XacThucInterceptor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;

/** Dữ liệu mẫu + kho giả cho test lịch làm việc / giao ca (không cần CSDL). */
class CaLamMau {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Thứ 6, 16/10/2026. */
    static final LocalDate HOM_NAY = LocalDate.of(2026, 10, 16);

    final List<NhanVien> nhanViens = new ArrayList<>();
    final List<CaLamViec> cas = new ArrayList<>();
    final List<LichLamViec> lichs = new ArrayList<>();
    final List<GiaoCa> giaoCas = new ArrayList<>();
    final List<HoaDon> hoaDons = new ArrayList<>();
    final List<LichSuThanhToan> thanhToans = new ArrayList<>();
    final List<PhuongThucThanhToan> phuongThucs = new ArrayList<>();

    final NhanVien quanLy;
    final NhanVien bao;   // NV03
    final NhanVien anh;   // NV04
    final NhanVien nghi;  // NV10 ngừng hoạt động
    final CaLamViec sang;
    final CaLamViec chieu;
    final CaLamViec toi;
    final CaLamViec caGay; // 10:00 - 14:00, chồng cả sáng lẫn chiều

    CaLamMau() {
        VaiTro ql = new VaiTro();
        ql.setTenVaiTro(VaiTro.TEN_QUAN_LY);
        VaiTro nv = new VaiTro();
        nv.setTenVaiTro(VaiTro.TEN_NHAN_VIEN);
        quanLy = nv(1, "NV01", "Nguyễn Hoàng Long", ql, 1);
        bao = nv(3, "NV03", "Lê Quốc Bảo", nv, 1);
        anh = nv(4, "NV04", "Phạm Minh Anh", nv, 1);
        nghi = nv(10, "NV10", "Đặng Mai Hương", nv, 0);
        sang = ca(1, "CA01", "Ca sáng", "07:00", "12:00", 1);
        chieu = ca(2, "CA02", "Ca chiều", "12:00", "17:00", 1);
        toi = ca(3, "CA03", "Ca tối", "17:00", "22:00", 1);
        caGay = ca(4, "CA04", "Ca gãy", "10:00", "14:00", 1);
        String[][] pt = {{"PT01", "Tiền mặt"}, {"PT02", "Chuyển khoản ngân hàng"}, {"PT03", "COD"}, {"PT04", "VNPay"}, {"PT05", "Tiền mặt + chuyển khoản"}};
        for (int i = 0; i < pt.length; i++) {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setId(i + 1);
            p.setMaPhuongThuc(pt[i][0]);
            p.setTenPhuongThuc(pt[i][1]);
            p.setTrangThai(1);
            phuongThucs.add(p);
        }
    }

    private NhanVien nv(int id, String ma, String ten, VaiTro vt, int tt) {
        NhanVien n = new NhanVien();
        n.setId(id);
        n.setMaNhanVien(ma);
        n.setHoTen(ten);
        n.setVaiTro(vt);
        n.setTrangThai(tt);
        nhanViens.add(n);
        return n;
    }

    CaLamViec ca(int id, String ma, String ten, String bd, String kt, int tt) {
        CaLamViec c = new CaLamViec();
        c.setId(id);
        c.setMaCa(ma);
        c.setTenCa(ten);
        c.setGioBatDau(LocalTime.parse(bd));
        c.setGioKetThuc(LocalTime.parse(kt));
        c.setTrangThai(tt);
        cas.add(c);
        return c;
    }

    LichLamViec lich(NhanVien n, CaLamViec c, LocalDate ngay) {
        LichLamViec l = new LichLamViec();
        l.setId(lichs.size() + 1);
        l.setNhanVien(n);
        l.setCaLamViec(c);
        l.setNgayLam(ngay);
        l.setTrangThai(1);
        lichs.add(l);
        return l;
    }

    static Clock luc(LocalDate ngay, String gio) {
        return Clock.fixed(ngay.atTime(LocalTime.parse(gio)).atZone(VN).toInstant(), VN);
    }

    static void dangNhap(NhanVien n) {
        XacThuc.dat(XacThucInterceptor.tuNhanVien(n));
    }

    /* ---------- kho giả ---------- */

    NhanVienRepository nhanVienRepo() {
        return KhoGia.tao(NhanVienRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> nhanViens.stream().filter(n -> n.getId().equals(a[0])).findFirst();
            case "findFirstByMaNhanVienIgnoreCase" -> nhanViens.stream().filter(n -> n.getMaNhanVien().equalsIgnoreCase((String) a[0])).findFirst();
            case "findByTrangThaiOrderByHoTenAsc" -> nhanViens.stream().filter(n -> n.getTrangThai().equals(a[0]))
                    .sorted(Comparator.comparing(NhanVien::getHoTen)).toList();
            default -> KhoGia.KHONG_CO;
        });
    }

    CaLamViecRepository caRepo() {
        return KhoGia.tao(CaLamViecRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> cas.stream().filter(c -> c.getId().equals(a[0])).findFirst();
            case "findAllByOrderByGioBatDauAscIdAsc" -> cas.stream().sorted(Comparator.comparing(CaLamViec::getGioBatDau)).toList();
            case "existsByTenCaIgnoreCaseAndIdNot" -> cas.stream().anyMatch(c -> c.getTenCa().equalsIgnoreCase((String) a[0]) && !c.getId().equals(a[1]));
            case "findAllMaCa" -> cas.stream().map(CaLamViec::getMaCa).toList();
            case "save" -> {
                CaLamViec c = (CaLamViec) a[0];
                if (c.getId() == null) {
                    c.setId(cas.size() + 1);
                    cas.add(c);
                }
                yield c;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    LichLamViecRepository lichRepo() {
        return KhoGia.tao(LichLamViecRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> lichs.stream().filter(l -> l.getId().equals(a[0])).findFirst();
            case "findByNgayLamBetween" -> lichs.stream()
                    .filter(l -> !l.getNgayLam().isBefore((LocalDate) a[0]) && !l.getNgayLam().isAfter((LocalDate) a[1])).toList();
            case "findByNhanVienIdAndNgayLamBetween" -> lichs.stream().filter(l -> l.getNhanVien().getId().equals(a[0]))
                    .filter(l -> !l.getNgayLam().isBefore((LocalDate) a[1]) && !l.getNgayLam().isAfter((LocalDate) a[2])).toList();
            case "findByNhanVienIdAndNgayLam" -> lichs.stream()
                    .filter(l -> l.getNhanVien().getId().equals(a[0]) && l.getNgayLam().equals(a[1])).toList();
            case "save" -> {
                LichLamViec l = (LichLamViec) a[0];
                if (l.getId() == null) {
                    l.setId(lichs.stream().mapToInt(LichLamViec::getId).max().orElse(0) + 1);
                    lichs.add(l);
                }
                yield l;
            }
            case "delete" -> {
                lichs.remove(a[0]);
                yield null;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    GiaoCaRepository giaoCaRepo() {
        return KhoGia.tao(GiaoCaRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> giaoCas.stream().filter(g -> g.getId().equals(a[0])).findFirst();
            case "findFirstByNhanVienIdAndTrangThai" -> giaoCas.stream()
                    .filter(g -> g.getNhanVien().getId().equals(a[0]) && g.getTrangThai().equals(a[1])).findFirst();
            case "findFirstByTrangThaiOrderByThoiGianKetCaDescIdDesc" -> giaoCas.stream().filter(g -> g.getTrangThai().equals(a[0]))
                    .max(Comparator.comparing(GiaoCa::getThoiGianKetCa).thenComparing(GiaoCa::getId));
            case "findByThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc" -> giaoCas.stream()
                    .filter(g -> !g.getThoiGianNhanCa().isBefore((LocalDateTime) a[0]) && !g.getThoiGianNhanCa().isAfter((LocalDateTime) a[1]))
                    .sorted(Comparator.comparing(GiaoCa::getThoiGianNhanCa).reversed()).toList();
            case "findByNhanVienIdAndThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc" -> giaoCas.stream()
                    .filter(g -> g.getNhanVien().getId().equals(a[0]))
                    .filter(g -> !g.getThoiGianNhanCa().isBefore((LocalDateTime) a[1]) && !g.getThoiGianNhanCa().isAfter((LocalDateTime) a[2]))
                    .sorted(Comparator.comparing(GiaoCa::getThoiGianNhanCa).reversed()).toList();
            case "existsByLichLamViecId" -> giaoCas.stream().anyMatch(g -> g.getLichLamViec() != null && g.getLichLamViec().getId().equals(a[0]));
            case "findAllMaGiaoCa" -> giaoCas.stream().map(GiaoCa::getMaGiaoCa).toList();
            case "save", "saveAndFlush" -> {
                GiaoCa g = (GiaoCa) a[0];
                if (g.getId() == null) {
                    // giống chỉ mục ux_giao_ca_dang_mo: mỗi nhân viên chỉ 1 ca đang mở
                    if (giaoCas.stream().anyMatch(x -> x.getTrangThai() == 1 && x.getNhanVien().getId().equals(g.getNhanVien().getId()))) {
                        throw new DataIntegrityViolationException("ux_giao_ca_dang_mo");
                    }
                    g.setId(giaoCas.size() + 1);
                    giaoCas.add(g);
                }
                yield g;
            }
            default -> KhoGia.KHONG_CO;
        });
    }

    HoaDonRepository hoaDonRepo() {
        return KhoGia.tao(HoaDonRepository.class, (ten, a) -> switch (ten) {
            case "findByIdGiaoCaOrderByNgayTaoAsc" -> hoaDons.stream().filter(h -> Objects.equals(h.getIdGiaoCa(), a[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
    }

    LichSuThanhToanRepository thanhToanRepo() {
        return KhoGia.tao(LichSuThanhToanRepository.class, (ten, a) -> switch (ten) {
            case "findByHoaDon_IdGiaoCa" -> thanhToans.stream().filter(t -> Objects.equals(t.getHoaDon().getIdGiaoCa(), a[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
    }

    PhuongThucThanhToanRepository phuongThucRepo() {
        return KhoGia.tao(PhuongThucThanhToanRepository.class, (ten, a) -> switch (ten) {
            case "findAll" -> new ArrayList<>(phuongThucs);
            case "findFirstByMaPhuongThuc" -> phuongThucs.stream().filter(p -> p.getMaPhuongThuc().equals(a[0])).findFirst();
            default -> KhoGia.KHONG_CO;
        });
    }

    /** Hóa đơn tại quầy trong ca, kèm các lần trả tiền {mã phương thức, số tiền}. */
    HoaDon hoaDon(Integer idGiaoCa, long thanhTien, int trangThai, Object... traTien) {
        HoaDon h = new HoaDon();
        h.setId(hoaDons.size() + 1);
        h.setMaHoaDon("HD" + (50 + hoaDons.size()));
        h.setIdGiaoCa(idGiaoCa);
        h.setThanhTien(BigDecimal.valueOf(thanhTien));
        h.setTrangThai(trangThai);
        h.setLoaiDon(HoaDon.TAI_QUAY);
        h.setNgayTao(HOM_NAY.atTime(8, hoaDons.size()));
        hoaDons.add(h);
        for (int i = 0; i < traTien.length; i += 2) {
            LichSuThanhToan t = new LichSuThanhToan();
            t.setHoaDon(h);
            String ma = (String) traTien[i];
            t.setIdPhuongThucThanhToan(phuongThucs.stream().filter(p -> p.getMaPhuongThuc().equals(ma)).findFirst().orElseThrow().getId());
            t.setSoTien(BigDecimal.valueOf(((Number) traTien[i + 1]).longValue()));
            t.setTrangThai(1);
            thanhToans.add(t);
        }
        return h;
    }
}
