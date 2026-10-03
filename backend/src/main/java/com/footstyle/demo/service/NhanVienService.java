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

    
    @Transactional(readOnly = true)
    public List<VaiTroResponse> getVaiTro() {
        List<VaiTroResponse> ketQua = new ArrayList<>();
        for (VaiTro v : vaiTroRepo.findByTrangThaiOrderByIdAsc(1)) {
            ketQua.add(new VaiTroResponse(v.getId(), v.getTenVaiTro()));
        }
        return ketQua;
    }

    

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
        napDuLieu(nv, req, false);
        
        if (dangHoatDong(nv) && laQuanLy(vaiTroCu) && !laQuanLy(nv.getVaiTro())) {
            kiemTraConQuanLyKhac(vaiTroCu, "Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
        }
        return toResponse(nhanVienRepo.save(nv));
    }

    
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

    

    private NhanVien timNhanVien(Integer id) {
        return nhanVienRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên."));
    }

    private boolean dangHoatDong(NhanVien nv) {
        return nv.getTrangThai() != null && nv.getTrangThai() == NhanVien.HOAT_DONG;
    }

    private boolean laQuanLy(VaiTro v) {
        return v != null && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(v.getTenVaiTro());
    }

    
    private void kiemTraConQuanLyKhac(VaiTro vaiTroQuanLy, String thongBao) {
        long soQuanLy = nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroQuanLy.getId(), NhanVien.HOAT_DONG);
        if (soQuanLy <= 1) {
            throw ApiException.conflict(thongBao);
        }
    }

    
    private String taoMaMoi() {
        int max = 0;
        for (String ma : nhanVienRepo.findAllMaNhanVien()) {
            String so = ma == null ? "" : ma.replaceAll("\\D", ""); 
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return String.format("NV%04d", max + 1);
    }

    
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    
    private void napDuLieu(NhanVien nv, NhanVienRequest req, boolean taoMoi) {
        
        String hoTen = req.hoTen() == null ? "" : req.hoTen().trim().replaceAll("\\s+", " ");
        loi(hoTen.isEmpty(), "Nhập họ tên.");
        loi(hoTen.length() < 2 || hoTen.length() > 60, "Họ tên từ 2 đến 60 ký tự.");

        
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        loi(email.isEmpty(), "Nhập email.");
        loi(email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"), "Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.");
        Integer idHienTai = nv.getId() == null ? -1 : nv.getId(); 
        if (nhanVienRepo.existsByEmailIgnoreCaseAndIdNot(email, idHienTai)) {
            throw ApiException.conflict("Email đã được sử dụng.");
        }

        
        String sdt = req.soDienThoai() == null ? "" : req.soDienThoai().trim();
        loi(sdt.isEmpty(), "Nhập số điện thoại.");
        loi(!sdt.matches("^0[35789]\\d{8}$"), "Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        if (nhanVienRepo.existsBySdtAndIdNot(sdt, idHienTai)) {
            throw ApiException.conflict("Số điện thoại đã được sử dụng.");
        }

        
        Integer gioiTinh = null;
        if (req.gioiTinh() != null && !req.gioiTinh().isBlank()) {
            loi(!req.gioiTinh().equals("Nam") && !req.gioiTinh().equals("Nữ"), "Giới tính không hợp lệ.");
            gioiTinh = req.gioiTinh().equals("Nam") ? NhanVien.GIOI_TINH_NAM : NhanVien.GIOI_TINH_NU;
        }

        
        LocalDate ngaySinh = req.ngaySinh();
        loi(ngaySinh != null && ngaySinh.isAfter(LocalDate.now().minusYears(18)), "Nhân viên phải từ đủ 18 tuổi.");

        
        String diaChi = req.diaChi() == null ? "" : req.diaChi().trim();
        loi(diaChi.isEmpty(), "Nhập địa chỉ.");
        loi(diaChi.length() > 255, "Địa chỉ tối đa 255 ký tự.");

        
        LocalDate ngayVaoLam = req.ngayVaoLam();
        if (ngayVaoLam == null) {
            ngayVaoLam = taoMoi ? LocalDate.now() : nv.getNgayVaoLam();
        }
        loi(ngayVaoLam != null && ngaySinh != null && ngayVaoLam.isBefore(ngaySinh.plusYears(18)),
                "Ngày vào làm không hợp lệ so với ngày sinh (phải từ đủ 18 tuổi).");

        
        loi(req.idVaiTro() == null, "Chọn vai trò.");
        VaiTro vaiTro = vaiTroRepo.findById(req.idVaiTro())
                .orElseThrow(() -> ApiException.badRequest("Vai trò không tồn tại."));
        loi(vaiTro.getTrangThai() == null || vaiTro.getTrangThai() != 1, "Vai trò này đang ngưng sử dụng.");

        
        String matKhau = req.matKhau() == null ? "" : req.matKhau();
        loi(taoMoi && matKhau.isEmpty(), "Nhập mật khẩu cho nhân viên mới.");
        if (!matKhau.isEmpty()) {
            loi(matKhau.length() < 8 || matKhau.length() > 50, "Mật khẩu từ 8 đến 50 ký tự.");
            nv.setMatKhau(passwordEncoder.encode(matKhau)); 
        }

        
        nv.setHoTen(hoTen);
        nv.setEmail(email);
        nv.setSdt(sdt);
        nv.setGioiTinh(gioiTinh);
        nv.setNgaySinh(ngaySinh);
        nv.setDiaChi(diaChi);
        nv.setNgayVaoLam(ngayVaoLam);
        nv.setVaiTro(vaiTro);
    }

    
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
