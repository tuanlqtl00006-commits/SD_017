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
        boolean laQuanLyDangHoatDong = dangHoatDong(nv) && laQuanLy(vaiTroCu);


        long soQuanLy = laQuanLyDangHoatDong
                ? nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroCu.getId(), NhanVien.HOAT_DONG)
                : 0;
        napDuLieu(nv, req, false);
        if (laQuanLyDangHoatDong && !laQuanLy(nv.getVaiTro()) && soQuanLy <= 1) {
            throw ApiException.conflict("Loi");
        }
        return toResponse(nhanVienRepo.save(nv));
    }


    @Transactional
    public NhanVienResponse doiTrangThai(Integer id) {
        NhanVien nv = timNhanVien(id);
        if (dangHoatDong(nv)) {
            if (laQuanLy(nv.getVaiTro())) {
                kiemTraConQuanLyKhac(nv.getVaiTro(), "Loi");
            }
            nv.setTrangThai(NhanVien.NGUNG_HOAT_DONG);
        } else {
            nv.setTrangThai(NhanVien.HOAT_DONG);
        }
        return toResponse(nhanVienRepo.save(nv));
    }

    

    private NhanVien timNhanVien(Integer id) {
        return nhanVienRepo.findById(id).orElseThrow(() -> ApiException.notFound("KhĂ´ng tĂ¬m tháº¥y nhĂ¢n viĂªn."));
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
        Integer idHienTai = nv.getId() == null ? -1 : nv.getId(); 

        String email = nv.getEmail();
        LocalDate ngayVaoLam = nv.getNgayVaoLam();
        String matKhau = "";
        if (taoMoi) {
            email = req.email() == null ? "" : req.email().trim().toLowerCase();
            loi(email.isEmpty(), "Nháº­p email.");
            loi(email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"), "Email chÆ°a Ä‘Ăºng Ä‘á»‹nh dáº¡ng, vĂ­ dá»¥: ten@footstyle.vn.");
            if (nhanVienRepo.existsByEmailIgnoreCaseAndIdNot(email, idHienTai)) {
                throw ApiException.conflict("Loi");
            }

            
            ngayVaoLam = req.ngayVaoLam() == null ? LocalDate.now() : req.ngayVaoLam();

            
            matKhau = req.matKhau() == null ? "" : req.matKhau();
            loi(matKhau.isEmpty(), "Nháº­p máº­t kháº©u cho nhĂ¢n viĂªn má»›i.");
            loi(matKhau.length() < 8 || matKhau.length() > 50, "Máº­t kháº©u tá»« 8 Ä‘áº¿n 50 kĂ½ tá»±.");
        }

        
        
        String hoTen = req.hoTen() == null ? "" : req.hoTen().trim().replaceAll("\\s+", " ");
        loi(hoTen.isEmpty(), "Nháº­p há»� tĂªn.");
        loi(hoTen.length() < 2 || hoTen.length() > 60, "Há»� tĂªn tá»« 2 Ä‘áº¿n 60 kĂ½ tá»±.");

        
        String sdt = req.soDienThoai() == null ? "" : req.soDienThoai().trim();
        loi(sdt.isEmpty(), "Nháº­p sá»‘ Ä‘iá»‡n thoáº¡i.");
        loi(!sdt.matches("^0[35789]\\d{8}$"), "Sá»‘ Ä‘iá»‡n thoáº¡i gá»“m 10 chá»¯ sá»‘, báº¯t Ä‘áº§u báº±ng 03, 05, 07, 08 hoáº·c 09.");
        if (nhanVienRepo.existsBySdtAndIdNot(sdt, idHienTai)) {
            throw ApiException.conflict("Loi");
        }

        
        Integer gioiTinh = null;
        if (req.gioiTinh() != null && !req.gioiTinh().isBlank()) {
            loi(!req.gioiTinh().equals("Nam") && !req.gioiTinh().equals("Ná»¯"), "Giá»›i tĂ­nh khĂ´ng há»£p lá»‡.");
            gioiTinh = req.gioiTinh().equals("Nam") ? NhanVien.GIOI_TINH_NAM : NhanVien.GIOI_TINH_NU;
        }

        
        LocalDate ngaySinh = req.ngaySinh();
        loi(ngaySinh != null && ngaySinh.isAfter(LocalDate.now().minusYears(18)), "NhĂ¢n viĂªn pháº£i tá»« Ä‘á»§ 18 tuá»•i.");

        
        if (ngayVaoLam != null && ngaySinh != null && ngayVaoLam.isBefore(ngaySinh.plusYears(18))) {
            if (taoMoi) {
                throw ApiException.badRequest("NgĂ y vĂ o lĂ m khĂ´ng há»£p lá»‡ so vá»›i ngĂ y sinh (pháº£i tá»« Ä‘á»§ 18 tuá»•i).");
            }
            
            String ngay = ngayVaoLam.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            throw ApiException.badRequest("NgĂ y sinh khĂ´ng há»£p lá»‡: nhĂ¢n viĂªn pháº£i Ä‘á»§ 18 tuá»•i vĂ o ngĂ y vĂ o lĂ m (" + ngay + ").");
        }

        
        String diaChi = req.diaChi() == null ? "" : req.diaChi().trim();
        loi(diaChi.isEmpty(), "Nháº­p Ä‘á»‹a chá»‰.");
        loi(diaChi.length() > 255, "Ä�á»‹a chá»‰ tá»‘i Ä‘a 255 kĂ½ tá»±.");

        
        loi(req.idVaiTro() == null, "Chá»�n vai trĂ².");
        VaiTro vaiTro = vaiTroRepo.findById(req.idVaiTro()).orElseThrow(() -> ApiException.badRequest("Vai trĂ² khĂ´ng tá»“n táº¡i."));
        loi(vaiTro.getTrangThai() == null || vaiTro.getTrangThai() != 1, "Vai trĂ² nĂ y Ä‘ang ngÆ°ng sá»­ dá»¥ng.");

        nv.setHoTen(hoTen);
        nv.setSdt(sdt);
        nv.setGioiTinh(gioiTinh);
        nv.setNgaySinh(ngaySinh);
        nv.setDiaChi(diaChi);
        nv.setVaiTro(vaiTro);

        if (taoMoi) { 
            nv.setEmail(email);
            nv.setNgayVaoLam(ngayVaoLam);
            nv.setMatKhau(passwordEncoder.encode(matKhau)); 
        }
    }

    private NhanVienResponse toResponse(NhanVien nv) {
        String gioiTinh = null;
        if (nv.getGioiTinh() != null) {
            gioiTinh = nv.getGioiTinh() == NhanVien.GIOI_TINH_NAM ? "Nam" : "Ná»¯";
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
