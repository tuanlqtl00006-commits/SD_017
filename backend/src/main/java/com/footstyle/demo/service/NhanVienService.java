package com.footstyle.demo.service;

import com.footstyle.demo.dto.DiaChiNhanVienDto;
import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.dto.VaiTroResponse;
import com.footstyle.demo.entity.DiaChiNhanVien;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.DiaChiNhanVienRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.VaiTroRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NhanVienService {

    private final NhanVienRepository nhanVienRepo;
    private final DiaChiNhanVienRepository diaChiRepo;
    private final VaiTroRepository vaiTroRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Autowired
    public NhanVienService(NhanVienRepository nhanVienRepo,
                           DiaChiNhanVienRepository diaChiRepo,
                           VaiTroRepository vaiTroRepo,
                           BCryptPasswordEncoder passwordEncoder,
                           @Autowired(required = false) MailService mailService) {
        this.nhanVienRepo = nhanVienRepo;
        this.diaChiRepo = diaChiRepo;
        this.vaiTroRepo = vaiTroRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public NhanVienService(NhanVienRepository nhanVienRepo,
                           DiaChiNhanVienRepository diaChiRepo,
                           VaiTroRepository vaiTroRepo,
                           BCryptPasswordEncoder passwordEncoder) {
        this(nhanVienRepo, diaChiRepo, vaiTroRepo, passwordEncoder, null);
    }

    /* ===================== Đọc ===================== */

    @Transactional(readOnly = true)
    public List<NhanVienResponse> getAll() {
        List<NhanVienResponse> ketQua = new ArrayList<>();
        for (NhanVien nv : nhanVienRepo.findAllByOrderByIdDesc()) {
            ketQua.add(toResponse(nv, diaChiRepo.findByNhanVienIdOrderByMacDinhDescIdAsc(nv.getId())));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public NhanVienResponse getById(Integer id) {
        NhanVien nv = timNhanVien(id);
        return toResponse(nv, diaChiRepo.findByNhanVienIdOrderByMacDinhDescIdAsc(nv.getId()));
    }

    /** Mọi mã nhân viên đang có (để xem trước mã sẽ cấp khi nhập Excel). */
    @Transactional(readOnly = true)
    public List<String> tatCaMaNhanVien() {
        return nhanVienRepo.findAllMaNhanVien();
    }

    // Danh sách vai trò đang dùng, đổ vào ô chọn trên form
    @Transactional(readOnly = true)
    public List<VaiTroResponse> getVaiTro() {
        List<VaiTroResponse> ketQua = new ArrayList<>();
        for (VaiTro v : vaiTroRepo.findByTrangThaiOrderByIdAsc(1)) {
            if (VaiTro.laVaiTroHopLe(v.getTenVaiTro())) { // chỉ có hai quyền: Quản lý và Nhân viên
                ketQua.add(new VaiTroResponse(v.getId(), v.getTenVaiTro()));
            }
        }
        return ketQua;
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public NhanVienResponse them(NhanVienRequest req) {
        NhanVien nv = new NhanVien();
        nv.setTrangThai(NhanVien.HOAT_DONG);
        List<DiaChiNhanVienDto> diaChis = napDuLieu(nv, req, true);
        // Mã tạo SAU khi đã kiểm tra hết thông tin: nếu có mã gửi lên (tự sinh NVxx từ form) thì dùng, không thì tự tạo theo họ tên
        if (req.maNhanVien() != null && !req.maNhanVien().isBlank()) {
            nv.setMaNhanVien(req.maNhanVien().trim().toUpperCase(Locale.ROOT));
        } else {
            nv.setMaNhanVien(MaNhanVienUtil.taoMa(nv.getHoTen(), nhanVienRepo.findAllMaNhanVien()));
        }
        NhanVien daLuu = nhanVienRepo.save(nv);
        if (mailService != null && daLuu.getEmail() != null && !daLuu.getEmail().isBlank() && req.matKhau() != null && !req.matKhau().isBlank()) {
            mailService.guiTaiKhoanNhanVien(
                    daLuu.getHoTen(),
                    daLuu.getEmail(),
                    daLuu.getMaNhanVien(),
                    daLuu.getVaiTro() == null ? "" : daLuu.getVaiTro().getTenVaiTro(),
                    req.matKhau(),
                    "NV-" + daLuu.getMaNhanVien()
            );
        }
        return toResponse(daLuu, luuDiaChi(daLuu, diaChis));
    }

    @Transactional
    public NhanVienResponse sua(Integer id, NhanVienRequest req) {
        NhanVien nv = timNhanVien(id);
        VaiTro vaiTroCu = nv.getVaiTro();
        boolean laQuanLyDangHoatDong = dangHoatDong(nv) && laQuanLy(vaiTroCu);
        // Đếm số quản lý đang hoạt động TRƯỚC khi sửa (lúc này nv chưa bị đổi nên đã được tính trong số đếm)
        long soQuanLy = laQuanLyDangHoatDong
                ? nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroCu.getId(), NhanVien.HOAT_DONG)
                : 0;
        List<DiaChiNhanVienDto> diaChis = napDuLieu(nv, req, false);
        // Quản lý đang hoạt động cuối cùng thì không được đổi sang vai trò khác
        if (laQuanLyDangHoatDong && !laQuanLy(nv.getVaiTro()) && soQuanLy <= 1) {
            throw ApiException.conflict("Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
        }
        NhanVien daLuu = nhanVienRepo.save(nv);
        return toResponse(daLuu, luuDiaChi(daLuu, diaChis));
    }

    // Không xóa nhân viên, chỉ đổi cột trạng thái: Còn làm (1) <-> Đã nghỉ (0)
    @Transactional
    public NhanVienResponse doiTrangThai(Integer id) {
        NhanVien nv = timNhanVien(id);
        if (dangHoatDong(nv)) {
            if (laQuanLy(nv.getVaiTro())) {
                kiemTraConQuanLyKhac(nv.getVaiTro(), "Không thể cho quản lý cuối cùng của hệ thống nghỉ việc.");
            }
            nv.setTrangThai(NhanVien.NGUNG_HOAT_DONG);
        } else {
            nv.setTrangThai(NhanVien.HOAT_DONG);
        }
        NhanVien daLuu = nhanVienRepo.save(nv);
        return toResponse(daLuu, diaChiRepo.findByNhanVienIdOrderByMacDinhDescIdAsc(daLuu.getId()));
    }

    /** Đổi mật khẩu cho nhân viên: kiểm tra độ dài 8-50, có cả chữ và số, không khoảng trắng, rồi băm lưu lại. */
    @Transactional
    public void doiMatKhau(Integer id, String matKhauMoi) {
        NhanVien nv = timNhanVien(id);
        String mk = matKhauMoi == null ? "" : matKhauMoi.trim();
        if (mk.isEmpty()) {
            throw ApiException.badRequest("Vui lòng nhập mật khẩu mới.");
        }
        if (mk.length() < 8 || mk.length() > 50) {
            throw ApiException.badRequest("Mật khẩu từ 8 đến 50 ký tự.");
        }
        if (!CO_CHU.matcher(mk).matches() || !CO_SO.matcher(mk).matches() || mk.chars().anyMatch(Character::isWhitespace)) {
            throw ApiException.badRequest("Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.");
        }
        nv.setMatKhau(passwordEncoder.encode(mk));
        nhanVienRepo.save(nv);
    }

    /* ===================== Hàm phụ ===================== */

    private NhanVien timNhanVien(Integer id) {
        return nhanVienRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên."));
    }

    private boolean dangHoatDong(NhanVien nv) {
        return nv.getTrangThai() != null && nv.getTrangThai() == NhanVien.HOAT_DONG;
    }

    private boolean laQuanLy(VaiTro v) {
        return v != null && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(v.getTenVaiTro());
    }

    // Phải còn ít nhất 1 quản lý khác còn làm thì mới được cho nghỉ / đổi vai trò quản lý này
    private void kiemTraConQuanLyKhac(VaiTro vaiTroQuanLy, String thongBao) {
        long soQuanLy = nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroQuanLy.getId(), NhanVien.HOAT_DONG);
        if (soQuanLy <= 1) {
            throw ApiException.conflict(thongBao);
        }
    }

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo (để mỗi lần kiểm tra chỉ cần 1 dòng)
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    /** Dữ liệu một nhân viên đã kiểm tra và chuẩn hóa (chưa ghi xuống CSDL). */
    public record DuLieuNhanVien(String email, LocalDate ngayVaoLam, String matKhau, String hoTen, String sdt, Integer gioiTinh,
            LocalDate ngaySinh, String canCuoc, List<DiaChiNhanVienDto> diaChis, VaiTro vaiTro) {
    }

    /** Mật khẩu phải có cả chữ và số, không khoảng trắng. */
    private static final Pattern CO_CHU = Pattern.compile(".*\\p{L}.*");
    private static final Pattern CO_SO = Pattern.compile(".*\\d.*");

    // Kiểm tra dữ liệu người dùng gửi lên, hợp lệ thì ghi vào nv (chưa lưu xuống DB)
    //  - THÊM MỚI (taoMoi = true): nhập đủ các trường.
    //  - SỬA (taoMoi = false): chỉ sửa họ tên, giới tính, ngày sinh, số điện thoại, căn cước, địa chỉ, vai trò.
    //    Email, ngày vào làm, mật khẩu giữ nguyên, dù request có gửi lên cũng bỏ qua.
    // THÊM và SỬA dùng chung một bộ luật (kiemTra). Lỗi được báo cho TỪNG Ô cùng lúc (xem LoiForm).
    // Trả về danh sách địa chỉ đã kiểm tra (đúng 1 địa chỉ chính) để lưu sau khi nhân viên đã có id.
    private List<DiaChiNhanVienDto> napDuLieu(NhanVien nv, NhanVienRequest req, boolean taoMoi) {
        LoiForm loi = new LoiForm();
        DuLieuNhanVien du = kiemTra(req, taoMoi, taoMoi ? null : nv, loi);
        loi.nemNeuCoLoi();
        apDung(nv, du, taoMoi);
        return du.diaChis();
    }

    // Dữ liệu hợp lệ hết -> ghi vào entity
    private void apDung(NhanVien nv, DuLieuNhanVien du, boolean taoMoi) {
        nv.setHoTen(du.hoTen());
        nv.setSdt(du.sdt());
        nv.setGioiTinh(du.gioiTinh());
        nv.setNgaySinh(du.ngaySinh());
        nv.setCanCuocCongDan(du.canCuoc().isEmpty() ? null : du.canCuoc());
        nv.setDiaChi(ghepDiaChi(diaChiChinh(du.diaChis())));
        nv.setVaiTro(du.vaiTro());
        if (taoMoi) { // chỉ thêm mới mới được ghi 3 trường này
            nv.setEmail(du.email());
            nv.setNgayVaoLam(du.ngayVaoLam());
            nv.setMatKhau(passwordEncoder.encode(du.matKhau())); // lưu dạng băm BCrypt, không lưu mật khẩu gốc
        }
    }

    /**
     * Bộ luật kiểm tra MỘT nhân viên (dùng cho thêm, sửa và từng dòng của file Excel). Ghi lỗi theo ô vào `loi`.
     * nvCu = nhân viên đang sửa (null khi thêm mới). Tên ô trùng tên trường của NhanVienRequest để giao diện gắn đúng ô.
     */
    public DuLieuNhanVien kiemTra(NhanVienRequest req, boolean taoMoi, NhanVien nvCu, LoiForm loi) {
        int idHienTai = nvCu == null || nvCu.getId() == null ? -1 : nvCu.getId(); // thêm mới chưa có id nên dùng -1
        LocalDate homNay = LocalDate.now();

        // ===== Phần CHỈ có khi THÊM MỚI: email, ngày vào làm, mật khẩu =====
        // Mặc định lấy giá trị cũ của nhân viên (khi sửa thì giữ nguyên)
        String email = nvCu == null || nvCu.getEmail() == null ? "" : nvCu.getEmail();
        LocalDate ngayVaoLam = nvCu == null ? null : nvCu.getNgayVaoLam();
        String matKhau = "";
        if (taoMoi) {
            // Email: đúng định dạng và không trùng người khác
            email = QuyTacDuLieu.cat(req.email()).toLowerCase(Locale.ROOT);
            if (email.isEmpty()) {
                loi.them("email", "Nhập email.");
            } else if (email.length() > 255 || !QuyTacDuLieu.EMAIL.matcher(email).matches()) {
                loi.them("email", "Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.");
            } else if (nhanVienRepo.existsByEmailIgnoreCaseAndIdNot(email, idHienTai)) {
                loi.trung("email", "Email đã được sử dụng.");
            }

            // Ngày vào làm: không nhập thì lấy hôm nay; không được ở tương lai
            ngayVaoLam = req.ngayVaoLam() == null ? homNay : req.ngayVaoLam();
            if (ngayVaoLam.isAfter(homNay)) {
                loi.them("ngayVaoLam", "Ngày vào làm không được ở tương lai.");
            }

            // Mật khẩu: bắt buộc, 8 - 50 ký tự, có cả chữ và số, không khoảng trắng
            matKhau = req.matKhau() == null ? "" : req.matKhau();
            if (matKhau.isEmpty()) {
                loi.them("matKhau", "Nhập mật khẩu cho nhân viên mới.");
            } else if (matKhau.length() < 8 || matKhau.length() > 50) {
                loi.them("matKhau", "Mật khẩu từ 8 đến 50 ký tự.");
            } else if (!CO_CHU.matcher(matKhau).matches() || !CO_SO.matcher(matKhau).matches() || matKhau.chars().anyMatch(Character::isWhitespace)) {
                loi.them("matKhau", "Mật khẩu phải có cả chữ và số, không chứa khoảng trắng.");
            }
        }

        // ===== Phần dùng cho cả THÊM MỚI và SỬA =====
        // Họ tên
        String hoTen = QuyTacDuLieu.chuanHoaHoTen(req.hoTen());
        if (hoTen.isEmpty()) {
            loi.them("hoTen", "Nhập họ tên.");
        } else if (!QuyTacDuLieu.HO_TEN.matcher(hoTen).matches()) {
            loi.them("hoTen", QuyTacDuLieu.LOI_HO_TEN_KY_TU);
        }

        // Số điện thoại: 10 số, đầu số 03/05/07/08/09, không trùng người khác
        String sdt = QuyTacDuLieu.cat(req.soDienThoai());
        if (sdt.isEmpty()) {
            loi.them("soDienThoai", "Nhập số điện thoại.");
        } else if (!QuyTacDuLieu.SDT.matcher(sdt).matches()) {
            loi.them("soDienThoai", QuyTacDuLieu.LOI_SDT);
        } else if (nhanVienRepo.existsBySdtAndIdNot(sdt, idHienTai)) {
            loi.trung("soDienThoai", "Số điện thoại đã được sử dụng.");
        }

        // Giới tính: chữ -> số lưu trong DB
        Integer gioiTinh = null;
        String gt = QuyTacDuLieu.cat(req.gioiTinh());
        if (gt.isEmpty()) {
            loi.them("gioiTinh", "Chọn giới tính.");
        } else if (!gt.equals("Nam") && !gt.equals("Nữ")) {
            loi.them("gioiTinh", "Giới tính không hợp lệ.");
        } else {
            gioiTinh = gt.equals("Nam") ? NhanVien.GIOI_TINH_NAM : NhanVien.GIOI_TINH_NU;
        }

        // Ngày sinh: bắt buộc (là căn cứ để biết nhân viên đã đủ 18 tuổi), phải từ đủ 18 tuổi (BLLĐ 2019, Điều 143)
        LocalDate ngaySinh = req.ngaySinh();
        if (ngaySinh == null) {
            loi.them("ngaySinh", "Nhập ngày sinh.");
        } else {
            String loiNgaySinh = QuyTacDuLieu.kiemTraNgaySinh(ngaySinh, NhanVien.TUOI_TOI_THIEU, "Nhân viên", homNay);
            if (loiNgaySinh != null) loi.them("ngaySinh", loiNgaySinh);
        }

        // Ngày sinh và ngày vào làm phải khớp nhau (vào làm khi đã đủ 18 tuổi)
        if (ngayVaoLam != null && ngaySinh != null && !loi.co("ngaySinh") && !loi.co("ngayVaoLam")
                && ngayVaoLam.isBefore(ngaySinh.plusYears(NhanVien.TUOI_TOI_THIEU))) {
            if (taoMoi) {
                loi.them("ngayVaoLam", "Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ 18 tuổi).");
            } else {
                // Khi sửa, ngày vào làm không đổi được nên lỗi nằm ở ngày sinh
                String ngay = ngayVaoLam.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                loi.them("ngaySinh", "Ngày sinh không hợp lệ: nhân viên phải đủ 18 tuổi vào ngày vào làm (" + ngay + ").");
            }
        }

        // Số căn cước công dân (không bắt buộc): đúng 12 chữ số, không trùng người khác
        String canCuoc = QuyTacDuLieu.cat(req.canCuoc());
        if (!canCuoc.isEmpty()) {
            if (!QuyTacDuLieu.CCCD.matcher(canCuoc).matches()) {
                loi.them("canCuoc", "Số căn cước công dân gồm đúng 12 chữ số.");
            } else if (nhanVienRepo.existsByCanCuocCongDanAndIdNot(canCuoc, idHienTai)) {
                loi.trung("canCuoc", "Số căn cước công dân đã được sử dụng.");
            }
        }

        // Địa chỉ: nhiều địa chỉ, đúng 1 địa chỉ chính
        List<DiaChiNhanVienDto> diaChis = new ArrayList<>();
        try {
            diaChis = chuanHoaDiaChi(req);
        } catch (ApiException e) {
            loi.them("diaChis", e.getMessage());
        }

        // Vai trò: chỉ có hai quyền (Quản lý, Nhân viên), phải tồn tại và đang được sử dụng
        VaiTro vaiTro = null;
        if (req.idVaiTro() == null) {
            loi.them("idVaiTro", "Chọn vai trò.");
        } else {
            vaiTro = vaiTroRepo.findById(req.idVaiTro()).orElse(null);
            if (vaiTro == null) {
                loi.them("idVaiTro", "Vai trò không tồn tại.");
            } else if (vaiTro.getTrangThai() == null || vaiTro.getTrangThai() != 1) {
                loi.them("idVaiTro", "Vai trò này đang ngưng sử dụng.");
            } else if (!VaiTro.laVaiTroHopLe(vaiTro.getTenVaiTro())) {
                loi.them("idVaiTro", "Chỉ có hai vai trò: " + VaiTro.TEN_QUAN_LY + " và " + VaiTro.TEN_NHAN_VIEN + ".");
            }
        }

        return new DuLieuNhanVien(email, ngayVaoLam, matKhau, hoTen, sdt, gioiTinh, ngaySinh, canCuoc, diaChis, vaiTro);
    }

    /**
     * Thêm HÀNG LOẠT (nhập Excel): mọi dòng đã được kiểm tra trước, ở đây ghi tất cả hoặc không ghi gì (một giao dịch).
     * Mật khẩu trong từng DuLieuNhanVien là mật khẩu tạm (chưa băm); trả về danh sách nhân viên mới theo đúng thứ tự dòng.
     */
    @Transactional
    public List<NhanVienResponse> themLo(List<DuLieuNhanVien> ds) {
        List<String> maDangCo = new ArrayList<>(nhanVienRepo.findAllMaNhanVien());
        List<NhanVienResponse> ketQua = new ArrayList<>();
        for (DuLieuNhanVien du : ds) {
            NhanVien nv = new NhanVien();
            nv.setTrangThai(NhanVien.HOAT_DONG);
            apDung(nv, du, true);
            // Mã theo họ tên; tính cả mã vừa cấp cho các dòng trước trong cùng file (hai người cùng chữ cái đầu không trùng mã)
            nv.setMaNhanVien(MaNhanVienUtil.taoMa(nv.getHoTen(), maDangCo));
            maDangCo.add(nv.getMaNhanVien());
            NhanVien daLuu = nhanVienRepo.save(nv);
            ketQua.add(toResponse(daLuu, luuDiaChi(daLuu, du.diaChis())));
        }
        return ketQua;
    }

    /* ===================== Địa chỉ nhiều nơi ===================== */

    // Kiểm tra danh sách địa chỉ gửi lên: bắt buộc có ít nhất 1 địa chỉ, đúng 1 địa chỉ chính.
    // Bản cũ chỉ gửi một chuỗi diaChi thì coi như 1 địa chỉ chính.
    private List<DiaChiNhanVienDto> chuanHoaDiaChi(NhanVienRequest req) {
        List<DiaChiNhanVienDto> nguon = new ArrayList<>();
        if (req.diaChis() != null && !req.diaChis().isEmpty()) {
            nguon.addAll(req.diaChis());
        } else if (req.diaChi() != null && !req.diaChi().isBlank()) {
            nguon.add(new DiaChiNhanVienDto(null, null, null, req.diaChi(), true));
        }
        loi(nguon.isEmpty(), "Nhập ít nhất một địa chỉ.");
        loi(nguon.size() > 20, "Mỗi nhân viên tối đa 20 địa chỉ.");

        List<DiaChiNhanVienDto> ketQua = new ArrayList<>();
        List<Integer> idDaGap = new ArrayList<>();
        int viTriChinh = -1;
        for (int i = 0; i < nguon.size(); i++) {
            DiaChiNhanVienDto d = nguon.get(i);
            loi(d == null, "Địa chỉ thứ " + (i + 1) + " không hợp lệ.");
            // Hai dòng cùng id sẽ cùng ghi vào một địa chỉ trong DB (dòng sau đè dòng trước, cờ "chính" có thể mất)
            if (d.id() != null) {
                loi(idDaGap.contains(d.id()), "Danh sách địa chỉ bị trùng (cùng một địa chỉ gửi hai lần).");
                idDaGap.add(d.id());
            }
            String chiTiet = d.diaChiCuThe() == null ? "" : d.diaChiCuThe().trim().replaceAll("\\s+", " ");
            String tinh = d.tinhThanh() == null ? "" : d.tinhThanh().trim();
            String phuong = d.phuongXa() == null ? "" : d.phuongXa().trim();
            loi(chiTiet.isEmpty(), "Nhập địa chỉ cụ thể (số nhà, đường...) cho địa chỉ thứ " + (i + 1) + ".");
            loi(chiTiet.length() > 255, "Địa chỉ cụ thể tối đa 255 ký tự.");
            loi(tinh.length() > 100 || phuong.length() > 100, "Tên tỉnh/thành phố hoặc phường/xã tối đa 100 ký tự.");
            if (d.macDinh() && viTriChinh < 0) viTriChinh = i;
            ketQua.add(new DiaChiNhanVienDto(d.id(), tinh.isEmpty() ? null : tinh, phuong.isEmpty() ? null : phuong, chiTiet, false));
        }
        if (viTriChinh < 0) viTriChinh = 0; // không chọn địa chỉ chính thì lấy địa chỉ đầu tiên
        DiaChiNhanVienDto chinh = ketQua.get(viTriChinh);
        ketQua.set(viTriChinh, new DiaChiNhanVienDto(chinh.id(), chinh.tinhThanh(), chinh.phuongXa(), chinh.diaChiCuThe(), true));
        return ketQua;
    }

    private DiaChiNhanVienDto diaChiChinh(List<DiaChiNhanVienDto> ds) {
        for (DiaChiNhanVienDto d : ds) {
            if (d.macDinh()) return d;
        }
        return ds.get(0);
    }

    // 'Số 1 Trần Phú, Phường Hà Đông, Thành phố Hà Nội' (bỏ phần trống)
    static String ghepDiaChi(DiaChiNhanVienDto d) {
        List<String> phan = new ArrayList<>();
        if (d.diaChiCuThe() != null && !d.diaChiCuThe().isBlank()) phan.add(d.diaChiCuThe().trim());
        if (d.phuongXa() != null && !d.phuongXa().isBlank()) phan.add(d.phuongXa().trim());
        if (d.tinhThanh() != null && !d.tinhThanh().isBlank()) phan.add(d.tinhThanh().trim());
        String s = String.join(", ", phan);
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    // Lưu địa chỉ: có id thì sửa, không có id thì thêm. KHÔNG xóa địa chỉ nào (địa chỉ không gửi lên vẫn được giữ).
    private List<DiaChiNhanVien> luuDiaChi(NhanVien nv, List<DiaChiNhanVienDto> moi) {
        List<DiaChiNhanVien> hienCo = diaChiRepo.findByNhanVienIdOrderByMacDinhDescIdAsc(nv.getId());
        List<DiaChiNhanVien> daXuLy = new ArrayList<>();
        DiaChiNhanVien chinh = null;
        for (DiaChiNhanVienDto d : moi) {
            DiaChiNhanVien dc = null;
            if (d.id() != null) {
                for (DiaChiNhanVien x : hienCo) {
                    if (d.id().equals(x.getId())) dc = x;
                }
                loi(dc == null, "Địa chỉ không thuộc nhân viên này.");
            } else {
                dc = new DiaChiNhanVien();
                dc.setNhanVien(nv);
            }
            dc.setTinhThanh(d.tinhThanh());
            dc.setPhuongXa(d.phuongXa());
            dc.setDiaChiCuThe(d.diaChiCuThe());
            dc.setMacDinh(false);
            daXuLy.add(dc);
            if (d.macDinh()) chinh = dc;
        }
        if (chinh != null) chinh.setMacDinh(true);
        // Địa chỉ cũ không có trong danh sách gửi lên: giữ nguyên, chỉ bỏ cờ "chính" khi đã có địa chỉ chính mới
        for (DiaChiNhanVien x : hienCo) {
            if (!daXuLy.contains(x)) {
                x.setMacDinh(false);
                daXuLy.add(x);
            }
        }
        List<DiaChiNhanVien> daLuu = new ArrayList<>(diaChiRepo.saveAll(daXuLy));
        daLuu.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getMacDinh()) != Boolean.TRUE.equals(b.getMacDinh())) {
                return Boolean.TRUE.equals(a.getMacDinh()) ? -1 : 1;
            }
            return Integer.compare(a.getId() == null ? Integer.MAX_VALUE : a.getId(), b.getId() == null ? Integer.MAX_VALUE : b.getId());
        });
        return daLuu;
    }

    // Đổi entity thành dữ liệu trả về cho frontend (không trả mật khẩu)
    private NhanVienResponse toResponse(NhanVien nv, List<DiaChiNhanVien> diaChis) {
        String gioiTinh = null;
        if (nv.getGioiTinh() != null) {
            gioiTinh = nv.getGioiTinh() == NhanVien.GIOI_TINH_NAM ? "Nam" : "Nữ";
        }
        VaiTro v = nv.getVaiTro();
        return new NhanVienResponse(
                nv.getId(),
                nv.getMaNhanVien(),
                nv.getHoTen(),
                gioiTinh,
                nv.getNgaySinh(),
                nv.getSdt(),
                nv.getEmail(),
                nv.getDiaChi(),
                nv.getNgayVaoLam(),
                v == null ? null : v.getId(),
                v == null ? null : v.getTenVaiTro(),
                dangHoatDong(nv),
                nv.getCanCuocCongDan(),
                toDiaChiDto(diaChis));
    }

    private List<DiaChiNhanVienDto> toDiaChiDto(List<DiaChiNhanVien> ds) {
        List<DiaChiNhanVienDto> ketQua = new ArrayList<>();
        if (ds == null) return ketQua;
        for (DiaChiNhanVien d : ds) {
            ketQua.add(new DiaChiNhanVienDto(d.getId(), d.getTinhThanh(), d.getPhuongXa(), d.getDiaChiCuThe(), Boolean.TRUE.equals(d.getMacDinh())));
        }
        return ketQua;
    }
}
