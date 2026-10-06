package com.footstyle.demo.service;

import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.dto.VaiTroResponse;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.VaiTroRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NhanVienService {

    private final NhanVienRepository nhanVienRepo;
    private final VaiTroRepository vaiTroRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    /* ===================== Đọc ===================== */

    @Transactional(readOnly = true)
    public List<NhanVienResponse> getAll() {
        List<NhanVienResponse> ketQua = new ArrayList<>();
        for (NhanVien nv : nhanVienRepo.findAllByOrderByIdDesc()) {
            ketQua.add(toResponse(nv));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public NhanVienResponse getById(Integer id) {
        return toResponse(timNhanVien(id));
    }

    // Danh sách vai trò đang dùng, đổ vào ô chọn trên form
    @Transactional(readOnly = true)
    public List<VaiTroResponse> getVaiTro() {
        List<VaiTroResponse> ketQua = new ArrayList<>();
        for (VaiTro v : vaiTroRepo.findByTrangThaiOrderByIdAsc(1)) {
            ketQua.add(new VaiTroResponse(v.getId(), v.getTenVaiTro()));
        }
        return ketQua;
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public NhanVienResponse them(NhanVienRequest req) {
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(taoMaMoi());
        nv.setTrangThai(NhanVien.HOAT_DONG);
        napDuLieu(nv, req, true);
        return toResponse(nhanVienRepo.save(nv));
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
        napDuLieu(nv, req, false);
        // Quản lý đang hoạt động cuối cùng thì không được đổi sang vai trò khác
        if (laQuanLyDangHoatDong && !laQuanLy(nv.getVaiTro()) && soQuanLy <= 1) {
            throw ApiException.conflict("Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
        }
        return toResponse(nhanVienRepo.save(nv));
    }

    // Không xóa nhân viên, chỉ ẩn / hiện bằng cột trạng thái
    @Transactional
    public NhanVienResponse doiTrangThai(Integer id) {
        NhanVien nv = timNhanVien(id);
        if (dangHoatDong(nv)) {
            if (laQuanLy(nv.getVaiTro())) {
                kiemTraConQuanLyKhac(nv.getVaiTro(), "Không thể ngưng hoạt động quản lý cuối cùng của hệ thống.");
            }
            nv.setTrangThai(NhanVien.NGUNG_HOAT_DONG);
        } else {
            nv.setTrangThai(NhanVien.HOAT_DONG);
        }
        return toResponse(nhanVienRepo.save(nv));
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

    // Phải còn ít nhất 1 quản lý khác đang hoạt động thì mới được ẩn / đổi vai trò quản lý này
    private void kiemTraConQuanLyKhac(VaiTro vaiTroQuanLy, String thongBao) {
        long soQuanLy = nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroQuanLy.getId(), NhanVien.HOAT_DONG);
        if (soQuanLy <= 1) {
            throw ApiException.conflict(thongBao);
        }
    }

    // Mã nhân viên kế tiếp: NV0001, NV0002, ...
    private String taoMaMoi() {
        int max = 0;
        for (String ma : nhanVienRepo.findAllMaNhanVien()) {
            String so = ma == null ? "" : ma.replaceAll("\\D", ""); // chỉ giữ chữ số
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return String.format("NV%04d", max + 1);
    }

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo (để mỗi lần kiểm tra chỉ cần 1 dòng)
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    // Kiểm tra dữ liệu người dùng gửi lên, hợp lệ thì ghi vào nv (chưa lưu xuống DB)
    //  - THÊM MỚI (taoMoi = true): nhập đủ các trường.
    //  - SỬA (taoMoi = false): chỉ sửa họ tên, giới tính, ngày sinh, số điện thoại, địa chỉ, vai trò.
    //    Email, ngày vào làm, mật khẩu giữ nguyên, dù request có gửi lên cũng bỏ qua.
    private void napDuLieu(NhanVien nv, NhanVienRequest req, boolean taoMoi) {
        Integer idHienTai = nv.getId() == null ? -1 : nv.getId(); // thêm mới chưa có id nên dùng -1

        // ===== Phần CHỈ có khi THÊM MỚI: email, ngày vào làm, mật khẩu =====
        // Mặc định lấy giá trị cũ của nhân viên (khi sửa thì giữ nguyên)
        String email = nv.getEmail();
        LocalDate ngayVaoLam = nv.getNgayVaoLam();
        String matKhau = "";
        if (taoMoi) {
            // Email: đúng định dạng và không trùng người khác
            email = req.email() == null ? "" : req.email().trim().toLowerCase();
            loi(email.isEmpty(), "Nhập email.");
            loi(email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"), "Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.");
            if (nhanVienRepo.existsByEmailIgnoreCaseAndIdNot(email, idHienTai)) {
                throw ApiException.conflict("Email đã được sử dụng.");
            }

            // Ngày vào làm: không nhập thì lấy hôm nay
            ngayVaoLam = req.ngayVaoLam() == null ? LocalDate.now() : req.ngayVaoLam();

            // Mật khẩu: bắt buộc, 8 - 50 ký tự
            matKhau = req.matKhau() == null ? "" : req.matKhau();
            loi(matKhau.isEmpty(), "Nhập mật khẩu cho nhân viên mới.");
            loi(matKhau.length() < 8 || matKhau.length() > 50, "Mật khẩu từ 8 đến 50 ký tự.");
        }

        // ===== Phần dùng cho cả THÊM MỚI và SỬA =====
        // Họ tên
        String hoTen = req.hoTen() == null ? "" : req.hoTen().trim().replaceAll("\\s+", " ");
        loi(hoTen.isEmpty(), "Nhập họ tên.");
        loi(hoTen.length() < 2 || hoTen.length() > 60, "Họ tên từ 2 đến 60 ký tự.");

        // Số điện thoại: 10 số, đầu số 03/05/07/08/09, không trùng người khác
        String sdt = req.soDienThoai() == null ? "" : req.soDienThoai().trim();
        loi(sdt.isEmpty(), "Nhập số điện thoại.");
        loi(!sdt.matches("^0[35789]\\d{8}$"), "Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        if (nhanVienRepo.existsBySdtAndIdNot(sdt, idHienTai)) {
            throw ApiException.conflict("Số điện thoại đã được sử dụng.");
        }

        // Giới tính (không bắt buộc): chữ -> số lưu trong DB
        Integer gioiTinh = null;
        if (req.gioiTinh() != null && !req.gioiTinh().isBlank()) {
            loi(!req.gioiTinh().equals("Nam") && !req.gioiTinh().equals("Nữ"), "Giới tính không hợp lệ.");
            gioiTinh = req.gioiTinh().equals("Nam") ? NhanVien.GIOI_TINH_NAM : NhanVien.GIOI_TINH_NU;
        }

        // Ngày sinh: phải đủ 18 tuổi
        LocalDate ngaySinh = req.ngaySinh();
        loi(ngaySinh != null && ngaySinh.isAfter(LocalDate.now().minusYears(18)), "Nhân viên phải từ đủ 18 tuổi.");

        // Ngày sinh và ngày vào làm phải khớp nhau (vào làm khi đã đủ 18 tuổi)
        if (ngayVaoLam != null && ngaySinh != null && ngayVaoLam.isBefore(ngaySinh.plusYears(18))) {
            if (taoMoi) {
                throw ApiException.badRequest("Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ 18 tuổi).");
            }
            // Khi sửa, ngày vào làm không đổi được nên lỗi nằm ở ngày sinh
            String ngay = ngayVaoLam.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            throw ApiException.badRequest("Ngày sinh không hợp lệ: nhân viên phải đủ 18 tuổi vào ngày vào làm (" + ngay + ").");
        }

        // Địa chỉ
        String diaChi = req.diaChi() == null ? "" : req.diaChi().trim();
        loi(diaChi.isEmpty(), "Nhập địa chỉ.");
        loi(diaChi.length() > 255, "Địa chỉ tối đa 255 ký tự.");

        // Vai trò: phải tồn tại và đang được sử dụng
        loi(req.idVaiTro() == null, "Chọn vai trò.");
        VaiTro vaiTro = vaiTroRepo.findById(req.idVaiTro())
                .orElseThrow(() -> ApiException.badRequest("Vai trò không tồn tại."));
        loi(vaiTro.getTrangThai() == null || vaiTro.getTrangThai() != 1, "Vai trò này đang ngưng sử dụng.");

        // Dữ liệu hợp lệ hết -> ghi vào entity
        nv.setHoTen(hoTen);
        nv.setSdt(sdt);
        nv.setGioiTinh(gioiTinh);
        nv.setNgaySinh(ngaySinh);
        nv.setDiaChi(diaChi);
        nv.setVaiTro(vaiTro);
        if (taoMoi) { // chỉ thêm mới mới được ghi 3 trường này
            nv.setEmail(email);
            nv.setNgayVaoLam(ngayVaoLam);
            nv.setMatKhau(passwordEncoder.encode(matKhau)); // lưu dạng băm BCrypt, không lưu mật khẩu gốc
        }
    }

    // Đổi entity thành dữ liệu trả về cho frontend (không trả mật khẩu)
    private NhanVienResponse toResponse(NhanVien nv) {
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
                dangHoatDong(nv));
    }
}
