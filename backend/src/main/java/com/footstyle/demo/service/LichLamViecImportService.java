package com.footstyle.demo.service;

import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.CaLamViecRepository;
import com.footstyle.demo.repository.LichLamViecRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.service.XlsxDoc.Dong;
import com.footstyle.demo.service.XlsxDoc.O;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhập lịch làm việc từ file Excel (.xlsx), mỗi dòng = một nhân viên làm một ca trong một ngày.
 * Cùng luật với xếp lịch trên màn hình (LichLamViecService.lyDoKhongXepDuoc) và cùng cách làm với nhập nhân viên:
 * sai dù một dòng thì cả file bị từ chối (422, chưa ghi gì), file đúng thì ghi tất cả trong một giao dịch.
 */
@Service
@RequiredArgsConstructor
public class LichLamViecImportService {

    public static final String MA_LOI_FILE = "FILE_KHONG_HOP_LE";

    /** Cột của file mẫu: tiêu đề hiển thị, các tên chấp nhận (đã bỏ dấu, chữ thường), có bắt buộc không. */
    enum Cot {
        MA_NV("maNhanVien", "Mã nhân viên (*)", true, "ma nhan vien", "ma nv", "ma"),
        HO_TEN("hoTen", "Họ tên", false, "ho ten", "ho va ten", "ten nhan vien"),
        NGAY("ngayLam", "Ngày làm (*)", true, "ngay lam", "ngay"),
        CA("ca", "Ca làm (*)", true, "ca lam", "ca", "ten ca", "ma ca"),
        GHI_CHU("ghiChu", "Ghi chú", false, "ghi chu");

        final String truong;
        final String tieuDe;
        final boolean batBuoc;
        final List<String> tenKhac;

        Cot(String truong, String tieuDe, boolean batBuoc, String... tenKhac) {
            this.truong = truong;
            this.tieuDe = tieuDe;
            this.batBuoc = batBuoc;
            this.tenKhac = List.of(tenKhac);
        }
    }

    public record LoiDong(int dong, String truong, String cot, String thongBao) {
    }

    public record XemTruoc(int dong, String maNhanVien, String hoTen, String ngayLam, String tenCa, String gio) {
    }

    public record KetQua(boolean daLuu, int soLich, List<XemTruoc> xemTruoc) {
    }

    private record DongHopLe(int soDong, NhanVien nv, CaLamViec ca, LocalDate ngay, String ghiChu) {
    }

    private static final List<DateTimeFormatter> KIEU_NGAY = List.of("d/M/uuuu", "d-M-uuuu", "d.M.uuuu", "uuuu-M-d", "uuuu/M/d").stream()
            .map(p -> DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT)).toList();

    private final LichLamViecService lichService;
    private final NhanVienRepository nhanVienRepo;
    private final CaLamViecRepository caRepo;
    private final LichLamViecRepository lichRepo;

    @Transactional
    public KetQua nhap(byte[] file, boolean chiKiemTra) {
        List<Dong> dong = XlsxDoc.doc(file);
        if (dong.isEmpty()) throw ApiException.badRequest("File Excel đang trống. Hãy dùng file mẫu của hệ thống.");
        Map<Cot, Integer> viTri = nhanDienCot(dong.get(0));
        List<Dong> duLieu = new ArrayList<>();
        for (Dong d : dong.subList(1, dong.size())) if (!d.trong()) duLieu.add(d);
        if (duLieu.isEmpty()) throw ApiException.badRequest("File chưa có dòng lịch nào (chỉ có dòng tiêu đề).");
        if (duLieu.size() > LichLamViecService.SO_O_TOI_DA) {
            throw ApiException.badRequest("File có " + duLieu.size() + " dòng, mỗi lần chỉ nhập tối đa " + LichLamViecService.SO_O_TOI_DA
                    + " ca. Hãy tách thành nhiều file.");
        }

        List<CaLamViec> cas = caRepo.findAllByOrderByGioBatDauAscIdAsc();
        List<LoiDong> loi = new ArrayList<>();
        List<DongHopLe> hopLe = new ArrayList<>();
        Map<String, Integer> daGap = new HashMap<>(); // "idNv|ngay|idCa" -> dòng
        Map<String, List<DongHopLe>> theoNguoiNgay = new HashMap<>();
        int soDongLoi = 0;
        for (Dong d : duLieu) {
            Map<Cot, String> loiDong = new LinkedHashMap<>();
            DongHopLe h = docDong(d, viTri, cas, loiDong);
            if (h != null) {
                String khoa = h.nv().getId() + "|" + h.ngay() + "|" + h.ca().getId();
                Integer truoc = daGap.putIfAbsent(khoa, d.soDong());
                if (truoc != null) {
                    loiDong.put(Cot.CA, "Trùng với dòng " + truoc + " (cùng nhân viên, cùng ngày, cùng ca).");
                } else {
                    for (DongHopLe khac : theoNguoiNgay.getOrDefault(h.nv().getId() + "|" + h.ngay(), List.of())) {
                        if (LichLamViecService.chongGio(h.ca(), khac.ca())) {
                            loiDong.put(Cot.CA, "Chồng giờ với " + khac.ca().getTenCa() + " ở dòng " + khac.soDong() + ".");
                            break;
                        }
                    }
                }
                if (loiDong.isEmpty()) {
                    String lyDo = lichService.lyDoKhongXepDuoc(h.nv(), h.ca(), h.ngay());
                    if (lyDo != null) loiDong.put(lyDo.startsWith("Ngày") ? Cot.NGAY : lyDo.startsWith("Nhân viên") ? Cot.MA_NV : Cot.CA, lyDo);
                }
            }
            if (loiDong.isEmpty()) {
                hopLe.add(h);
                theoNguoiNgay.computeIfAbsent(h.nv().getId() + "|" + h.ngay(), k -> new ArrayList<>()).add(h);
                continue;
            }
            soDongLoi++;
            loiDong.forEach((c, tb) -> loi.add(new LoiDong(d.soDong(), c.truong, c.tieuDe, tb)));
        }
        if (!loi.isEmpty()) throw loiFile(loi, duLieu.size(), soDongLoi);

        List<XemTruoc> xem = new ArrayList<>();
        for (DongHopLe h : hopLe) {
            xem.add(new XemTruoc(h.soDong(), h.nv().getMaNhanVien(), h.nv().getHoTen(), h.ngay().toString(), h.ca().getTenCa(),
                    h.ca().getGioBatDau().format(LichLamViecService.GIO) + " - " + h.ca().getGioKetThuc().format(LichLamViecService.GIO)));
        }
        if (chiKiemTra) return new KetQua(false, hopLe.size(), xem);

        for (DongHopLe h : hopLe) {
            LichLamViec l = new LichLamViec();
            l.setNhanVien(h.nv());
            l.setCaLamViec(h.ca());
            l.setNgayLam(h.ngay());
            l.setGhiChu(h.ghiChu());
            l.setTrangThai(1);
            lichRepo.save(l);
        }
        return new KetQua(true, hopLe.size(), xem);
    }

    private DongHopLe docDong(Dong d, Map<Cot, Integer> viTri, List<CaLamViec> cas, Map<Cot, String> loi) {
        NhanVien nv = null;
        String ma = chu(d, viTri, Cot.MA_NV);
        if (ma.isEmpty()) {
            loi.put(Cot.MA_NV, "Nhập mã nhân viên.");
        } else {
            nv = nhanVienRepo.findFirstByMaNhanVienIgnoreCase(ma).orElse(null);
            if (nv == null) loi.put(Cot.MA_NV, "Không có nhân viên mã " + ma + ".");
            else if (!Integer.valueOf(NhanVien.HOAT_DONG).equals(nv.getTrangThai())) loi.put(Cot.MA_NV, "Nhân viên " + nv.getHoTen() + " đang ngừng hoạt động.");
        }
        String hoTen = chu(d, viTri, Cot.HO_TEN);
        if (nv != null && !hoTen.isEmpty() && !NhanVienImportService.chuanHoaTen(hoTen).equals(NhanVienImportService.chuanHoaTen(nv.getHoTen()))) {
            loi.put(Cot.HO_TEN, "Họ tên không khớp với mã " + nv.getMaNhanVien() + " (" + nv.getHoTen() + ").");
        }

        LocalDate ngay = docNgay(d.o(viTri.get(Cot.NGAY)), loi);
        if (ngay != null && ngay.isBefore(lichService.homNay())) loi.put(Cot.NGAY, "Ngày đã qua, không xếp lịch được.");

        CaLamViec ca = null;
        String tenCa = chu(d, viTri, Cot.CA);
        if (tenCa.isEmpty()) {
            loi.put(Cot.CA, "Nhập ca làm (tên ca hoặc mã ca).");
        } else {
            ca = timCa(cas, tenCa);
            if (ca == null) loi.put(Cot.CA, "Không có ca \"" + tenCa + "\". Các ca: " + String.join(", ", cas.stream().map(CaLamViec::getTenCa).toList()) + ".");
            else if (!Integer.valueOf(CaLamViec.HOAT_DONG).equals(ca.getTrangThai())) loi.put(Cot.CA, "Ca \"" + ca.getTenCa() + "\" đang ngừng sử dụng.");
        }

        String ghiChu = chu(d, viTri, Cot.GHI_CHU);
        if (ghiChu.length() > 255) loi.put(Cot.GHI_CHU, "Ghi chú tối đa 255 ký tự.");
        if (!loi.isEmpty()) return null;
        return new DongHopLe(d.soDong(), nv, ca, ngay, ghiChu.isEmpty() ? null : ghiChu);
    }

    /** Ca theo mã (CA01) hoặc tên (Ca sáng, "ca sang", "Sáng"), không phân biệt hoa thường / dấu. */
    static CaLamViec timCa(List<CaLamViec> cas, String s) {
        String t = NhanVienImportService.chuanHoaTen(s);
        for (CaLamViec c : cas) {
            if (t.equals(NhanVienImportService.chuanHoaTen(c.getMaCa())) || t.equals(NhanVienImportService.chuanHoaTen(c.getTenCa()))) return c;
        }
        for (CaLamViec c : cas) {
            if (("ca " + t).equals(NhanVienImportService.chuanHoaTen(c.getTenCa()))) return c;
        }
        return null;
    }

    /** Ô ngày: số serial của Excel hoặc chữ dd/mm/yyyy (cũng nhận yyyy-mm-dd). */
    static LocalDate docNgay(O o, Map<Cot, String> loi) {
        String s = QuyTacDuLieu.cat(o.giaTri());
        if (s.isEmpty()) {
            loi.put(Cot.NGAY, "Nhập ngày làm.");
            return null;
        }
        String baoLoi = "Ngày làm không đúng định dạng (viết dd/mm/yyyy, ví dụ 27/10/2026).";
        if (o.laSo()) {
            try {
                long serial = new BigDecimal(s).longValue();
                if (serial >= 1 && serial <= 2958465) return LocalDate.of(1899, 12, 30).plusDays(serial);
            } catch (NumberFormatException e) {
                // báo lỗi ở dưới
            }
            loi.put(Cot.NGAY, baoLoi);
            return null;
        }
        if (s.matches("\\d{4}-\\d{2}-\\d{2}T.*")) s = s.substring(0, 10);
        for (DateTimeFormatter f : KIEU_NGAY) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException e) {
                // thử kiểu tiếp theo
            }
        }
        loi.put(Cot.NGAY, baoLoi);
        return null;
    }

    private static String chu(Dong d, Map<Cot, Integer> viTri, Cot c) {
        Integer i = viTri.get(c);
        return i == null ? "" : QuyTacDuLieu.cat(d.o(i).giaTri()).replaceAll("\\s+", " ");
    }

    private static Map<Cot, Integer> nhanDienCot(Dong tieuDe) {
        Map<Cot, Integer> kq = new LinkedHashMap<>();
        for (int i = 0; i < tieuDe.o().size(); i++) {
            String ten = NhanVienImportService.chuanHoaTen(tieuDe.o(i).giaTri());
            for (Cot c : Cot.values()) {
                if (!kq.containsKey(c) && c.tenKhac.contains(ten)) {
                    kq.put(c, i);
                    break;
                }
            }
        }
        List<String> thieu = new ArrayList<>();
        for (Cot c : Cot.values()) if (c.batBuoc && !kq.containsKey(c)) thieu.add(c.tieuDe);
        if (!thieu.isEmpty()) {
            throw ApiException.badRequest("File thiếu cột bắt buộc: " + String.join(", ", thieu)
                    + ". Hãy dùng đúng file mẫu của hệ thống (dòng đầu tiên là tiêu đề cột).");
        }
        return kq;
    }

    private static ApiException loiFile(List<LoiDong> loi, int tongDong, int soDongLoi) {
        List<Map<String, Object>> ds = new ArrayList<>();
        for (LoiDong l : loi) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dong", l.dong());
            m.put("truong", l.truong());
            m.put("cot", l.cot());
            m.put("thongBao", l.thongBao());
            ds.add(m);
        }
        Map<String, Object> chiTiet = new LinkedHashMap<>();
        chiTiet.put("loi", ds);
        chiTiet.put("tongDong", tongDong);
        chiTiet.put("soDongLoi", soDongLoi);
        String thongBao = "File có " + loi.size() + " lỗi ở " + soDongLoi + "/" + tongDong
                + " dòng. Cả file bị từ chối, chưa có ca nào được xếp. Hãy sửa các dòng báo lỗi rồi tải lên lại.";
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, thongBao, MA_LOI_FILE, chiTiet);
    }
}
