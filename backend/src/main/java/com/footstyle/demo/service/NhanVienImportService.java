package com.footstyle.demo.service;

import com.footstyle.demo.dto.DiaChiNhanVienDto;
import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.VaiTroRepository;
import com.footstyle.demo.service.NhanVienService.DuLieuNhanVien;
import com.footstyle.demo.service.XlsxDoc.Dong;
import com.footstyle.demo.service.XlsxDoc.O;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * NHẬP HÀNG LOẠT nhân viên từ file Excel (.xlsx) - theo yêu cầu của cô: "một dòng sai thì CẢ FILE không được đẩy đi",
 * kiểm tra định dạng, kiểm tra trùng (trong file và với dữ liệu đã có), nhập xong tự gửi mail thông báo tài khoản.
 *
 * Quy trình (nhap):
 *  1. Đọc file và nhận diện cột theo tiêu đề (không phụ thuộc thứ tự cột; thiếu cột bắt buộc -> 400).
 *  2. Kiểm tra TỪNG dòng bằng đúng bộ luật của form thêm nhân viên (NhanVienService.kiemTra) + trùng email / SĐT / CCCD trong chính file.
 *  3. Có bất kỳ lỗi nào -> 422 kèm danh sách lỗi (dòng, cột, nội dung); KHÔNG ghi gì xuống CSDL.
 *  4. chiKiemTra = true: chỉ báo "file hợp lệ" kèm bản xem trước. Ngược lại ghi TẤT CẢ trong một giao dịch (NhanVienService.themLo)
 *     rồi xếp mail tài khoản (mật khẩu tạm ngẫu nhiên) cho từng nhân viên.
 */
@Service
@RequiredArgsConstructor
public class NhanVienImportService {

    /** Tối đa số nhân viên mỗi file (băm mật khẩu BCrypt từng người khá chậm, và cửa hàng không tuyển quá nhiều người một lúc). */
    public static final int TOI_DA_DONG = 100;
    public static final String MA_LOI_FILE = "FILE_KHONG_HOP_LE";

    private final NhanVienService nhanVienService;
    private final VaiTroRepository vaiTroRepo;
    private final MailService mailService;

    /** Cột của file mẫu: tên ô trong NhanVienRequest, tiêu đề trong file mẫu, bắt buộc hay không, các cách viết tiêu đề chấp nhận. */
    enum Cot {
        HO_TEN("hoTen", "Họ và tên", true, "ho va ten", "ho ten", "ten nhan vien"),
        GIOI_TINH("gioiTinh", "Giới tính", true, "gioi tinh"),
        NGAY_SINH("ngaySinh", "Ngày sinh", true, "ngay sinh"),
        SDT("soDienThoai", "Số điện thoại", true, "so dien thoai", "sdt", "dien thoai"),
        EMAIL("email", "Email", true, "email", "e mail", "thu dien tu"),
        CAN_CUOC("canCuoc", "Số căn cước công dân", false, "so can cuoc cong dan", "can cuoc cong dan", "so cccd", "cccd", "can cuoc"),
        NGAY_VAO_LAM("ngayVaoLam", "Ngày vào làm", false, "ngay vao lam"),
        VAI_TRO("idVaiTro", "Vai trò", false, "vai tro", "chuc vu", "quyen"),
        DIA_CHI("diaChiCuThe", "Địa chỉ cụ thể", true, "dia chi cu the", "dia chi", "so nha duong"),
        PHUONG_XA("phuongXa", "Phường/Xã", true, "phuong xa", "phuong", "xa"),
        TINH("tinhThanh", "Tỉnh/Thành phố", true, "tinh thanh pho", "tinh thanh", "tinh", "thanh pho");

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

    /** Một lỗi trong file: dòng THẬT trong Excel, tên ô (trùng tên trường của form), tiêu đề cột, nội dung. */
    public record LoiDong(int dong, String truong, String cot, String thongBao) {
    }

    public record XemTruoc(int dong, String ma, String hoTen, String email, String vaiTro) {
    }

    /** Tài khoản vừa tạo. matKhauTam chỉ trả về MỘT lần này để quản lý lưu lại phòng khi mail chưa gửi được. */
    public record TaiKhoanMoi(String ma, String hoTen, String email, String vaiTro, String matKhauTam) {
    }

    /**
     * Kết quả: daLuu (false = mới chỉ kiểm tra), soNhanVien, xemTruoc (mã dự kiến), taiKhoan + maLo + soMail + mailDaCauHinh (khi đã lưu).
     */
    public record KetQua(boolean daLuu, int soNhanVien, List<XemTruoc> xemTruoc, List<TaiKhoanMoi> taiKhoan, String maLo, int soMail,
            boolean mailDaCauHinh) {
    }

    private record DongNhap(int soDong, NhanVienRequest req, Map<String, String> loiDinhDang, String tenVaiTro) {
    }

    public KetQua nhap(byte[] file, boolean chiKiemTra) {
        List<Dong> dong = XlsxDoc.doc(file);
        if (dong.isEmpty()) {
            throw ApiException.badRequest("File Excel đang trống. Hãy dùng file mẫu của hệ thống.");
        }
        Map<Cot, Integer> viTriCot = nhanDienCot(dong.get(0));
        List<Dong> duLieu = dong.subList(1, dong.size());
        if (duLieu.isEmpty()) {
            throw ApiException.badRequest("File chưa có dòng nhân viên nào (chỉ có dòng tiêu đề).");
        }
        if (duLieu.size() > TOI_DA_DONG) {
            throw ApiException.badRequest("File có " + duLieu.size() + " dòng, mỗi lần chỉ nhập tối đa " + TOI_DA_DONG + " nhân viên. Hãy tách thành nhiều file.");
        }

        Map<String, VaiTro> vaiTroTheoTen = vaiTroHopLe();
        VaiTro macDinh = vaiTroTheoTen.get(chuanHoaTen(VaiTro.TEN_NHAN_VIEN));
        List<DongNhap> ds = new ArrayList<>();
        for (Dong d : duLieu) {
            ds.add(docDong(d, viTriCot, vaiTroTheoTen, macDinh));
        }

        // ---- Kiểm tra từng dòng + trùng trong file ----
        List<LoiDong> loiTatCa = new ArrayList<>();
        List<DuLieuNhanVien> hopLe = new ArrayList<>();
        Map<String, Integer> emailDaGap = new HashMap<>();
        Map<String, Integer> sdtDaGap = new HashMap<>();
        Map<String, Integer> cccdDaGap = new HashMap<>();
        int soDongLoi = 0;
        for (DongNhap n : ds) {
            LoiForm lf = new LoiForm();
            n.loiDinhDang().forEach(lf::them); // lỗi định dạng ô (ngày sai...) đứng trước lỗi chung
            if (QuyTacDuLieu.cat(n.req().diaChis().get(0).phuongXa()).isEmpty()) lf.them("phuongXa", "Nhập Phường/Xã.");
            if (QuyTacDuLieu.cat(n.req().diaChis().get(0).tinhThanh()).isEmpty()) lf.them("tinhThanh", "Nhập Tỉnh/Thành phố.");
            if (QuyTacDuLieu.cat(n.req().diaChis().get(0).diaChiCuThe()).isEmpty()) lf.them("diaChiCuThe", "Nhập địa chỉ cụ thể (số nhà, đường...).");
            DuLieuNhanVien du = nhanVienService.kiemTra(n.req(), true, null, lf);

            trungTrongFile(lf, "email", du.email(), emailDaGap, n.soDong(), "Email");
            trungTrongFile(lf, "soDienThoai", du.sdt(), sdtDaGap, n.soDong(), "Số điện thoại");
            trungTrongFile(lf, "canCuoc", du.canCuoc(), cccdDaGap, n.soDong(), "Số căn cước công dân");

            Map<String, String> loi = lf.theoTruong();
            if (loi.containsKey("phuongXa") || loi.containsKey("tinhThanh") || loi.containsKey("diaChiCuThe")) loi.remove("diaChis");
            if (loi.isEmpty()) {
                hopLe.add(du);
                continue;
            }
            soDongLoi++;
            for (Map.Entry<String, String> e : loi.entrySet()) {
                loiTatCa.add(new LoiDong(n.soDong(), e.getKey(), tieuDeCot(e.getKey()), e.getValue()));
            }
        }
        if (!loiTatCa.isEmpty()) {
            throw loiFile(loiTatCa, ds.size(), soDongLoi);
        }

        // ---- File hợp lệ ----
        if (chiKiemTra) {
            List<String> maDangCo = new ArrayList<>(nhanVienService.tatCaMaNhanVien());
            List<XemTruoc> xem = new ArrayList<>();
            for (int i = 0; i < hopLe.size(); i++) {
                DuLieuNhanVien du = hopLe.get(i);
                String ma = MaNhanVienUtil.taoMa(du.hoTen(), maDangCo);
                maDangCo.add(ma);
                xem.add(new XemTruoc(ds.get(i).soDong(), ma, du.hoTen(), du.email(), du.vaiTro().getTenVaiTro()));
            }
            return new KetQua(false, hopLe.size(), xem, List.of(), null, 0, mailService.duocCauHinh());
        }

        // Mật khẩu tạm đã nằm sẵn trong từng DuLieuNhanVien (xem docDong) nên themLo băm và lưu cùng lúc
        List<NhanVienResponse> moi = nhanVienService.themLo(hopLe);
        String maLo = "NVIMP-" + System.currentTimeMillis();
        mailService.batDauLo(maLo, moi.size());
        List<TaiKhoanMoi> taiKhoan = new ArrayList<>();
        for (int i = 0; i < moi.size(); i++) {
            NhanVienResponse nv = moi.get(i);
            String matKhauTam = hopLe.get(i).matKhau();
            taiKhoan.add(new TaiKhoanMoi(nv.ma(), nv.hoTen(), nv.email(), nv.vaiTro(), matKhauTam));
            mailService.guiTaiKhoanNhanVien(nv.hoTen(), nv.email(), nv.ma(), nv.vaiTro(), matKhauTam, maLo);
        }
        return new KetQua(true, moi.size(), List.of(), taiKhoan, maLo, moi.size(), mailService.duocCauHinh());
    }

    /** Kết quả gửi mail của một lần nhập (tong, daGui, loi, giaLap, xong, lyDo). */
    public Map<String, Object> ketQuaMail(String maLo) {
        return mailService.ketQuaLo(maLo);
    }

    /* ===================== Đọc file ===================== */

    private Map<Cot, Integer> nhanDienCot(Dong tieuDe) {
        Map<Cot, Integer> kq = new LinkedHashMap<>();
        for (int i = 0; i < tieuDe.o().size(); i++) {
            String ten = chuanHoaTen(tieuDe.o(i).giaTri()).replaceAll("\\bbat buoc\\b", "").trim().replaceAll("\\s+", " ");
            for (Cot c : Cot.values()) {
                if (!kq.containsKey(c) && c.tenKhac.contains(ten)) {
                    kq.put(c, i);
                    break;
                }
            }
        }
        List<String> thieu = new ArrayList<>();
        for (Cot c : Cot.values()) {
            if (c.batBuoc && !kq.containsKey(c)) thieu.add(c.tieuDe);
        }
        if (!thieu.isEmpty()) {
            throw ApiException.badRequest("File thiếu cột bắt buộc: " + String.join(", ", thieu) + ". Hãy dùng đúng file mẫu của hệ thống (dòng đầu tiên là tiêu đề cột).");
        }
        return kq;
    }

    private Map<String, VaiTro> vaiTroHopLe() {
        Map<String, VaiTro> kq = new HashMap<>();
        for (VaiTro v : vaiTroRepo.findByTrangThaiOrderByIdAsc(1)) {
            if (VaiTro.laVaiTroHopLe(v.getTenVaiTro())) kq.put(chuanHoaTen(v.getTenVaiTro()), v);
        }
        return kq;
    }

    private DongNhap docDong(Dong d, Map<Cot, Integer> viTri, Map<String, VaiTro> vaiTroTheoTen, VaiTro macDinh) {
        Map<String, String> loiDinhDang = new LinkedHashMap<>();
        String hoTen = chu(d, viTri, Cot.HO_TEN);
        String gioiTinh = chuanHoaGioiTinh(chu(d, viTri, Cot.GIOI_TINH));
        String sdt = soTuO(d, viTri, Cot.SDT, 10, 1);
        String email = chu(d, viTri, Cot.EMAIL);
        String canCuoc = soTuO(d, viTri, Cot.CAN_CUOC, 12, 2);
        LocalDate ngaySinh = docNgay(d, viTri, Cot.NGAY_SINH, loiDinhDang);
        LocalDate ngayVaoLam = docNgay(d, viTri, Cot.NGAY_VAO_LAM, loiDinhDang);

        String tenVaiTro = chu(d, viTri, Cot.VAI_TRO);
        Integer idVaiTro = null;
        if (tenVaiTro.isEmpty()) {
            if (macDinh != null) idVaiTro = macDinh.getId();
        } else {
            VaiTro v = vaiTroTheoTen.get(chuanHoaTen(tenVaiTro));
            if (v != null) idVaiTro = v.getId();
            else loiDinhDang.put("idVaiTro", "Vai trò chỉ gồm " + VaiTro.TEN_QUAN_LY + " hoặc " + VaiTro.TEN_NHAN_VIEN + ".");
        }
        DiaChiNhanVienDto diaChi = new DiaChiNhanVienDto(null, chu(d, viTri, Cot.TINH), chu(d, viTri, Cot.PHUONG_XA), chu(d, viTri, Cot.DIA_CHI), true);
        // Mật khẩu tạm: sinh ngẫu nhiên, đạt luật mật khẩu; được băm khi lưu và gửi cho nhân viên qua mail
        NhanVienRequest req = new NhanVienRequest(hoTen, gioiTinh, ngaySinh, sdt, email, null, ngayVaoLam, idVaiTro, MatKhauTam.tao(), canCuoc,
                List.of(diaChi));
        return new DongNhap(d.soDong(), req, loiDinhDang, tenVaiTro);
    }

    private static String chu(Dong d, Map<Cot, Integer> viTri, Cot c) {
        Integer i = viTri.get(c);
        return i == null ? "" : QuyTacDuLieu.cat(d.o(i).giaTri()).replaceAll("\\s+", " ");
    }

    /**
     * Số điện thoại / căn cước: bỏ khoảng trắng, dấu chấm, gạch ngang. Excel hay đổi "0912345678" thành SỐ 912345678 (mất số 0 đầu)
     * nên với ô SỐ thì bù lại các số 0 bị mất cho đủ độ dài. Chỉ bù tối đa `toiDaBu` số 0: số điện thoại mất nhiều nhất 1 số 0
     * ("0..."), căn cước mất nhiều nhất 2 ("001..." của Hà Nội); thiếu nhiều hơn là gõ sai chứ không phải Excel làm mất,
     * nên để nguyên cho bước kiểm tra báo lỗi (không biến "123" thành "000000000123").
     */
    private static String soTuO(Dong d, Map<Cot, Integer> viTri, Cot c, int doDai, int toiDaBu) {
        Integer i = viTri.get(c);
        if (i == null) return "";
        O o = d.o(i);
        String s = QuyTacDuLieu.cat(o.giaTri()).replaceAll("[\\s.\\-]", "");
        if (o.laSo() && s.matches("\\d+") && s.length() < doDai && doDai - s.length() <= toiDaBu) {
            s = "0".repeat(doDai - s.length()) + s;
        }
        return s;
    }

    private static final List<DateTimeFormatter> KIEU_NGAY = List.of("d/M/uuuu", "d-M-uuuu", "d.M.uuuu", "uuuu-M-d", "uuuu/M/d").stream()
            .map(p -> DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT)).toList();

    /** Ô ngày: số serial của Excel hoặc chữ dd/mm/yyyy (cũng nhận yyyy-mm-dd). Sai thì ghi lỗi vào loi và trả null. */
    private static LocalDate docNgay(Dong d, Map<Cot, Integer> viTri, Cot c, Map<String, String> loi) {
        Integer i = viTri.get(c);
        if (i == null) return null;
        O o = d.o(i);
        String s = QuyTacDuLieu.cat(o.giaTri());
        if (s.isEmpty()) return null;
        String truong = c.truong;
        String baoLoi = c.tieuDe + " không đúng định dạng (viết dd/mm/yyyy, ví dụ 25/12/2000).";
        if (o.laSo()) {
            try {
                long serial = new BigDecimal(s).longValue();
                if (serial < 1 || serial > 2958465) {
                    loi.put(truong, baoLoi);
                    return null;
                }
                return LocalDate.of(1899, 12, 30).plusDays(serial);
            } catch (NumberFormatException e) {
                loi.put(truong, baoLoi);
                return null;
            }
        }
        if (s.matches("\\d{4}-\\d{2}-\\d{2}T.*")) s = s.substring(0, 10);
        for (DateTimeFormatter f : KIEU_NGAY) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException e) {
                // thử kiểu tiếp theo
            }
        }
        loi.put(truong, baoLoi);
        return null;
    }

    private static String chuanHoaGioiTinh(String s) {
        String t = chuanHoaTen(s);
        if (t.equals("nam")) return "Nam";
        if (t.equals("nu")) return "Nữ";
        return s; // sai thì giữ nguyên để bộ luật báo "Giới tính không hợp lệ"
    }

    /** Bỏ dấu, chữ thường, chỉ giữ chữ / số, gộp khoảng trắng: "Phường/Xã (*)" -> "phuong xa". */
    static String chuanHoaTen(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D');
        return t.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /* ===================== Lỗi ===================== */

    private static void trungTrongFile(LoiForm lf, String truong, String giaTri, Map<String, Integer> daGap, int soDong, String ten) {
        if (giaTri == null || giaTri.isEmpty() || lf.co(truong)) return;
        Integer truoc = daGap.putIfAbsent(giaTri.toLowerCase(Locale.ROOT), soDong);
        if (truoc != null) {
            lf.trung(truong, ten + " trùng với dòng " + truoc + " trong file (mỗi " + ten.toLowerCase(Locale.ROOT) + " chỉ được dùng một lần).");
        }
    }

    private static String tieuDeCot(String truong) {
        for (Cot c : Cot.values()) {
            if (c.truong.equals(truong)) return c.tieuDe;
        }
        return switch (truong) {
            case "diaChis" -> Cot.DIA_CHI.tieuDe;
            case "matKhau" -> "Mật khẩu";
            default -> truong;
        };
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
                + " dòng. Cả file bị từ chối, chưa có nhân viên nào được thêm. Hãy sửa các dòng báo lỗi rồi tải lên lại.";
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, thongBao, MA_LOI_FILE, chiTiet);
    }
}
