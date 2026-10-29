package com.footstyle.demo.service;

import com.footstyle.demo.dto.DangNhapRequest;
import com.footstyle.demo.dto.DoiMatKhauRequest;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.security.GioiHanDangNhap;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.TokenXacThuc;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.security.XacThucInterceptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng nhập / đổi mật khẩu / quên mật khẩu.
 * - Tài khoản nhân viên: email, số điện thoại hoặc mã nhân viên; mật khẩu so bằng BCrypt.
 * - Sai mật khẩu 5 lần liên tiếp: khóa 5 phút (GioiHanDangNhap). Câu báo sai luôn chung chung ("tài khoản hoặc mật khẩu không đúng")
 *   để người ngoài không dò được tài khoản nào có tồn tại.
 * - Tài khoản ngừng hoạt động: chỉ báo sau khi đã nhập ĐÚNG mật khẩu (cũng để không lộ tài khoản).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    static final String LOI_SAI = "Tài khoản hoặc mật khẩu không đúng.";
    /** Mã băm giả: tài khoản không tồn tại vẫn so BCrypt một lần để thời gian trả lời như nhau (không dò được tài khoản). */
    private static final String MA_BAM_GIA = "$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m";
    private static final Pattern MA_BAM_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    /** Khoảng cách tối thiểu giữa 2 lần cấp lại mật khẩu tạm cho cùng một tài khoản. */
    static final Duration CHO_CAP_LAI = Duration.ofMinutes(5);

    private final NhanVienRepository nhanVienRepo;
    private final KhachHangRepository khachHangRepo;
    private final BCryptPasswordEncoder encoder;
    private final TokenXacThuc token;
    private final GioiHanDangNhap gioiHan;
    private final MailService mailService;
    /** id nhân viên -> lần cuối được cấp mật khẩu tạm (trong bộ nhớ, giống GioiHanDangNhap). */
    private final Map<Integer, Instant> lanCapLai = new ConcurrentHashMap<>();

    /* ===================== Nhân viên ===================== */

    @Transactional
    public Map<String, Object> dangNhapNhanVien(DangNhapRequest req) {
        String taiKhoan = kiemTraNhap(req);
        String matKhau = req.matKhau();
        Optional<NhanVien> timThay = timNhanVien(taiKhoan);
        // Đếm sai theo TÀI KHOẢN THẬT: mã NV, email, SĐT của cùng một người dùng chung bộ đếm (không thành 3 x 5 lần thử)
        String khoaDem = timThay.map(nv -> "nv#" + nv.getId()).orElse("nv:" + taiKhoan);
        chanNeuDangKhoa(khoaDem);

        if (timThay.isEmpty()) dungMatKhau(matKhau, MA_BAM_GIA);
        if (timThay.isEmpty() || !dungMatKhau(matKhau, timThay.get().getMatKhau())) {
            throw saiMatKhau(khoaDem);
        }
        NhanVien nv = timThay.get();
        if (!Integer.valueOf(NhanVien.HOAT_DONG).equals(nv.getTrangThai())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản lý cửa hàng.", "TAI_KHOAN_NGUNG", null);
        }
        gioiHan.ghiDung(khoaDem);
        if (!laMaBamBCrypt(nv.getMatKhau())) {
            // Mật khẩu dữ liệu mẫu / bản cũ còn lưu dạng thường: băm lại ngay khi đăng nhập đúng
            nv.setMatKhau(encoder.encode(matKhau));
            nhanVienRepo.save(nv);
        }
        return ketQuaDangNhap(token.tao(NguoiDung.LOAI_NHAN_VIEN, nv.getId(), nv.getMatKhau()), XacThucInterceptor.tuNhanVien(nv));
    }

    /** Email chứa @ -> email; 10 chữ số -> SĐT; còn lại -> mã nhân viên. */
    Optional<NhanVien> timNhanVien(String taiKhoan) {
        if (taiKhoan.contains("@")) return nhanVienRepo.findFirstByEmailIgnoreCase(taiKhoan);
        if (taiKhoan.matches("\\d{10}")) return nhanVienRepo.findFirstBySdt(taiKhoan);
        return nhanVienRepo.findFirstByMaNhanVienIgnoreCase(taiKhoan);
    }

    /* ===================== Khách hàng (trang bán online) ===================== */

    @Transactional
    public Map<String, Object> dangNhapKhachHang(DangNhapRequest req) {
        String taiKhoan = kiemTraNhap(req);
        Optional<KhachHang> timThay = taiKhoan.contains("@")
                ? khachHangRepo.findFirstByEmailIgnoreCase(taiKhoan)
                : khachHangRepo.findFirstBySdt(taiKhoan);
        String khoaDem = timThay.map(kh -> "kh#" + kh.getId()).orElse("kh:" + taiKhoan);
        chanNeuDangKhoa(khoaDem);
        if (timThay.isEmpty()) dungMatKhau(req.matKhau(), MA_BAM_GIA);
        if (timThay.isEmpty() || !dungMatKhau(req.matKhau(), timThay.get().getMatKhau())) {
            throw saiMatKhau(khoaDem);
        }
        KhachHang kh = timThay.get();
        if (!Integer.valueOf(1).equals(kh.getTrangThai())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã ngừng hoạt động. Vui lòng liên hệ cửa hàng.", "TAI_KHOAN_NGUNG", null);
        }
        gioiHan.ghiDung(khoaDem);
        if (!laMaBamBCrypt(kh.getMatKhau())) {
            kh.setMatKhau(encoder.encode(req.matKhau()));
            khachHangRepo.save(kh);
        }
        return ketQuaDangNhap(token.tao(NguoiDung.LOAI_KHACH_HANG, kh.getId(), kh.getMatKhau()), XacThucInterceptor.tuKhachHang(kh));
    }

    /* ===================== Đổi / quên mật khẩu ===================== */

    /** Đổi mật khẩu của nhân viên đang đăng nhập; trả về token mới (token cũ hết hiệu lực vì mật khẩu đã đổi). */
    @Transactional
    public Map<String, Object> doiMatKhau(DoiMatKhauRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        NhanVien nv = nhanVienRepo.findById(nd.id()).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản."));
        LoiForm loi = new LoiForm();
        String cu = req == null ? null : req.matKhauCu();
        String moi = req == null ? null : req.matKhauMoi();
        String nhapLai = req == null ? null : req.nhapLai();
        if (cu == null || cu.isEmpty()) loi.them("matKhauCu", "Vui lòng nhập mật khẩu hiện tại.");
        else if (!dungMatKhau(cu, nv.getMatKhau())) loi.them("matKhauCu", "Mật khẩu hiện tại không đúng.");
        String loiMoi = QuyTacDuLieu.loiMatKhau(moi);
        if (loiMoi != null) loi.them("matKhauMoi", loiMoi);
        else if (cu != null && cu.equals(moi)) loi.them("matKhauMoi", "Mật khẩu mới phải khác mật khẩu hiện tại.");
        if (nhapLai == null || nhapLai.isEmpty()) loi.them("nhapLai", "Vui lòng nhập lại mật khẩu mới.");
        else if (moi != null && !moi.equals(nhapLai)) loi.them("nhapLai", "Mật khẩu nhập lại không khớp.");
        loi.nemNeuCoLoi();

        nv.setMatKhau(encoder.encode(moi));
        nhanVienRepo.save(nv);
        return ketQuaDangNhap(token.tao(NguoiDung.LOAI_NHAN_VIEN, nv.getId(), nv.getMatKhau()), XacThucInterceptor.tuNhanVien(nv));
    }

    /** Đổi mật khẩu của khách hàng đang đăng nhập (trang Tài khoản online); trả về token mới như nhân viên. */
    @Transactional
    public Map<String, Object> doiMatKhauKhach(DoiMatKhauRequest req) {
        NguoiDung nd = XacThuc.hienTai()
                .filter(NguoiDung::laKhachHang)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập bằng tài khoản khách hàng."));
        KhachHang kh = khachHangRepo.findById(nd.id()).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản."));
        LoiForm loi = new LoiForm();
        String cu = req == null ? null : req.matKhauCu();
        String moi = req == null ? null : req.matKhauMoi();
        String nhapLai = req == null ? null : req.nhapLai();
        if (cu == null || cu.isEmpty()) loi.them("matKhauCu", "Vui lòng nhập mật khẩu hiện tại.");
        else if (!dungMatKhau(cu, kh.getMatKhau())) loi.them("matKhauCu", "Mật khẩu hiện tại không đúng.");
        String loiMoi = QuyTacDuLieu.loiMatKhau(moi);
        if (loiMoi != null) loi.them("matKhauMoi", loiMoi);
        else if (cu != null && cu.equals(moi)) loi.them("matKhauMoi", "Mật khẩu mới phải khác mật khẩu hiện tại.");
        if (nhapLai == null || nhapLai.isEmpty()) loi.them("nhapLai", "Vui lòng nhập lại mật khẩu mới.");
        else if (moi != null && !moi.equals(nhapLai)) loi.them("nhapLai", "Mật khẩu nhập lại không khớp.");
        loi.nemNeuCoLoi();

        kh.setMatKhau(encoder.encode(moi));
        khachHangRepo.save(kh);
        return phienMoiCuaKhach(kh);
    }

    /** Phiên (token + thông tin hiển thị) cho khách hàng: dùng sau khi đổi mật khẩu hoặc sửa hồ sơ. */
    public Map<String, Object> phienMoiCuaKhach(KhachHang kh) {
        return ketQuaDangNhap(token.tao(NguoiDung.LOAI_KHACH_HANG, kh.getId(), kh.getMatKhau()), XacThucInterceptor.tuKhachHang(kh));
    }

    /**
     * Quên mật khẩu: email đúng của nhân viên đang hoạt động thì đặt mật khẩu tạm mới và gửi qua email.
     * Luôn trả cùng một câu (dù email có tồn tại hay không) để không lộ email nào là tài khoản của cửa hàng.
     */
    @Transactional
    public Map<String, Object> quenMatKhau(String email) {
        String e = email == null ? "" : email.trim();
        LoiForm loi = new LoiForm();
        if (e.isEmpty()) loi.them("email", "Vui lòng nhập email tài khoản.");
        else if (!QuyTacDuLieu.EMAIL.matcher(e).matches()) loi.them("email", QuyTacDuLieu.LOI_EMAIL);
        loi.nemNeuCoLoi();
        nhanVienRepo.findFirstByEmailIgnoreCase(e)
                .filter(nv -> Integer.valueOf(NhanVien.HOAT_DONG).equals(nv.getTrangThai()))
                .filter(nv -> duocCapLai(nv.getId()))
                .ifPresent(nv -> {
                    String tam = MatKhauTam.tao();
                    nv.setMatKhau(encoder.encode(tam));
                    nhanVienRepo.save(nv);
                    mailService.guiMatKhauTam(nv.getHoTen(), nv.getEmail(), tam);
                });
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("message", "Nếu email đúng là tài khoản đang hoạt động, mật khẩu tạm đã được gửi tới email đó. Hãy kiểm tra hộp thư (cả mục Spam).");
        kq.put("daCauHinhMail", mailService.duocCauHinh());
        return kq;
    }

    /**
     * Quên mật khẩu đổi mật khẩu NGAY nên ai biết email cũng bấm được: mỗi tài khoản chỉ cấp lại 1 lần / CHO_CAP_LAI,
     * tránh bị người khác bấm liên tục làm nhân viên không đăng nhập được (vẫn trả cùng một câu báo).
     */
    private synchronized boolean duocCapLai(Integer idNhanVien) {
        Instant bayGio = Instant.now();
        Instant lanTruoc = lanCapLai.get(idNhanVien);
        if (lanTruoc != null && Duration.between(lanTruoc, bayGio).compareTo(CHO_CAP_LAI) < 0) return false;
        lanCapLai.put(idNhanVien, bayGio);
        return true;
    }

    /* ===================== Hàm phụ ===================== */

    /** Bắt buộc nhập đủ; trả về tài khoản đã bỏ khoảng trắng. */
    private static String kiemTraNhap(DangNhapRequest req) {
        String taiKhoan = req == null || req.taiKhoan() == null ? "" : req.taiKhoan().trim();
        String matKhau = req == null || req.matKhau() == null ? "" : req.matKhau();
        LoiForm loi = new LoiForm();
        if (taiKhoan.isEmpty()) loi.them("taiKhoan", "Vui lòng nhập email, số điện thoại hoặc mã nhân viên.");
        else if (taiKhoan.length() > 255) loi.them("taiKhoan", "Tài khoản quá dài.");
        if (matKhau.isEmpty()) loi.them("matKhau", "Vui lòng nhập mật khẩu.");
        else if (matKhau.length() > 100) loi.them("matKhau", "Mật khẩu quá dài.");
        loi.nemNeuCoLoi();
        return taiKhoan;
    }

    private void chanNeuDangKhoa(String taiKhoan) {
        long giay = gioiHan.conKhoaGiay(taiKhoan);
        if (giay > 0) {
            long phut = (giay + 59) / 60;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai mật khẩu " + GioiHanDangNhap.SO_LAN_SAI_TOI_DA
                    + " lần. Vui lòng thử lại sau " + phut + " phút.", "TAM_KHOA", Map.of("conGiay", giay));
        }
    }

    private ApiException saiMatKhau(String taiKhoan) {
        int conLai = gioiHan.ghiSai(taiKhoan);
        if (conLai <= 0) {
            long phut = GioiHanDangNhap.THOI_GIAN_KHOA.toMinutes();
            return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai mật khẩu " + GioiHanDangNhap.SO_LAN_SAI_TOI_DA
                    + " lần. Vui lòng thử lại sau " + phut + " phút.", "TAM_KHOA", Map.of("conGiay", GioiHanDangNhap.THOI_GIAN_KHOA.getSeconds()));
        }
        String them = conLai <= 2 ? " Còn " + conLai + " lần thử trước khi bị tạm khóa." : "";
        return new ApiException(HttpStatus.UNAUTHORIZED, LOI_SAI + them, "SAI_TAI_KHOAN", Map.of("conLai", conLai));
    }

    private boolean dungMatKhau(String nhap, String daBam) {
        if (nhap == null || daBam == null || daBam.isBlank()) return false;
        // Mật khẩu dữ liệu mẫu / phiên bản trước (chưa băm BCrypt) thì so trực tiếp. Đã băm thì CHỈ so bằng BCrypt:
        // không được nhận chính chuỗi băm làm mật khẩu (ai lấy được chuỗi băm trong CSDL sẽ đăng nhập được).
        if (!laMaBamBCrypt(daBam)) {
            return MessageDigest.isEqual(nhap.getBytes(StandardCharsets.UTF_8), daBam.getBytes(StandardCharsets.UTF_8));
        }
        try {
            return encoder.matches(nhap, daBam);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Chuỗi băm BCrypt: $2a$ / $2b$ / $2y$ + 2 chữ số độ khó + $ + 53 ký tự. */
    static boolean laMaBamBCrypt(String s) {
        return s != null && MA_BAM_BCRYPT.matcher(s).matches();
    }

    private Map<String, Object> ketQuaDangNhap(String t, NguoiDung nd) {
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("token", t);
        kq.put("hetHan", LocalDateTime.ofInstant(token.hetHanNeuTaoBayGio(), ZoneId.systemDefault()));
        kq.put("nguoiDung", thongTin(nd));
        return kq;
    }

    /** Thông tin người dùng trả cho giao diện (không có mật khẩu). */
    public static Map<String, Object> thongTin(NguoiDung nd) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loai", nd.loai());
        m.put("id", nd.id());
        m.put("ma", nd.ma());
        m.put("hoTen", nd.hoTen());
        m.put("email", nd.email());
        m.put("vaiTro", nd.vaiTro());
        m.put("tenVaiTro", nd.tenVaiTro());
        return m;
    }
}
