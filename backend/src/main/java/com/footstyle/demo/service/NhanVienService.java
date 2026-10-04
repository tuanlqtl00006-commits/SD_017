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

    /* ===================== Äá»c ===================== */

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

    // Danh sĂ¡ch vai trĂ² Ä‘ang dĂ¹ng, Ä‘á»• vĂ o Ă´ chá»n trĂªn form
    @Transactional(readOnly = true)
    public List<VaiTroResponse> getVaiTro() {
        List<VaiTroResponse> ketQua = new ArrayList<>();
        for (VaiTro v : vaiTroRepo.findByTrangThaiOrderByIdAsc(1)) {
            ketQua.add(new VaiTroResponse(v.getId(), v.getTenVaiTro()));
        }
        return ketQua;
    }

    /* ===================== ThĂªm / sá»­a / Ä‘á»•i tráº¡ng thĂ¡i ===================== */

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
        // Äáº¿m sá»‘ quáº£n lĂ½ Ä‘ang hoáº¡t Ä‘á»™ng TRÆ¯á»C khi sá»­a (lĂºc nĂ y nv chÆ°a bá»‹ Ä‘á»•i nĂªn Ä‘Ă£ Ä‘Æ°á»£c tĂ­nh trong sá»‘ Ä‘áº¿m)
        long soQuanLy = laQuanLyDangHoatDong
                ? nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroCu.getId(), NhanVien.HOAT_DONG)
                : 0;
        napDuLieu(nv, req, false);
        // Quáº£n lĂ½ Ä‘ang hoáº¡t Ä‘á»™ng cuá»‘i cĂ¹ng thĂ¬ khĂ´ng Ä‘Æ°á»£c Ä‘á»•i sang vai trĂ² khĂ¡c
        if (laQuanLyDangHoatDong && !laQuanLy(nv.getVaiTro()) && soQuanLy <= 1) {
            throw ApiException.conflict("KhĂ´ng thá»ƒ Ä‘á»•i vai trĂ² cá»§a quáº£n lĂ½ Ä‘ang hoáº¡t Ä‘á»™ng cuá»‘i cĂ¹ng.");
        }
        return toResponse(nhanVienRepo.save(nv));
    }

    // KhĂ´ng xĂ³a nhĂ¢n viĂªn, chá»‰ áº©n / hiá»‡n báº±ng cá»™t tráº¡ng thĂ¡i
    @Transactional
    public NhanVienResponse doiTrangThai(Integer id) {
        NhanVien nv = timNhanVien(id);
        if (dangHoatDong(nv)) {
            if (laQuanLy(nv.getVaiTro())) {
                kiemTraConQuanLyKhac(nv.getVaiTro(), "KhĂ´ng thá»ƒ ngÆ°ng hoáº¡t Ä‘á»™ng quáº£n lĂ½ cuá»‘i cĂ¹ng cá»§a há»‡ thá»‘ng.");
            }
            nv.setTrangThai(NhanVien.NGUNG_HOAT_DONG);
        } else {
            nv.setTrangThai(NhanVien.HOAT_DONG);
        }
        return toResponse(nhanVienRepo.save(nv));
    }

    /* ===================== HĂ m phá»¥ ===================== */

    private NhanVien timNhanVien(Integer id) {
        return nhanVienRepo.findById(id).orElseThrow(() -> ApiException.notFound("KhĂ´ng tĂ¬m tháº¥y nhĂ¢n viĂªn."));
    }

    private boolean dangHoatDong(NhanVien nv) {
        return nv.getTrangThai() != null && nv.getTrangThai() == NhanVien.HOAT_DONG;
    }

    private boolean laQuanLy(VaiTro v) {
        return v != null && VaiTro.TEN_QUAN_LY.equalsIgnoreCase(v.getTenVaiTro());
    }

    // Pháº£i cĂ²n Ă­t nháº¥t 1 quáº£n lĂ½ khĂ¡c Ä‘ang hoáº¡t Ä‘á»™ng thĂ¬ má»›i Ä‘Æ°á»£c áº©n / Ä‘á»•i vai trĂ² quáº£n lĂ½ nĂ y
    private void kiemTraConQuanLyKhac(VaiTro vaiTroQuanLy, String thongBao) {
        long soQuanLy = nhanVienRepo.countByVaiTroIdAndTrangThai(vaiTroQuanLy.getId(), NhanVien.HOAT_DONG);
        if (soQuanLy <= 1) {
            throw ApiException.conflict(thongBao);
        }
    }

    // MĂ£ nhĂ¢n viĂªn káº¿ tiáº¿p: NV0001, NV0002, ...
    private String taoMaMoi() {
        int max = 0;
        for (String ma : nhanVienRepo.findAllMaNhanVien()) {
            String so = ma == null ? "" : ma.replaceAll("\\D", ""); // chá»‰ giá»¯ chá»¯ sá»‘
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return String.format("NV%04d", max + 1);
    }

    // Náº¿u Ä‘iá»u kiá»‡n Ä‘Ăºng thĂ¬ bĂ¡o lá»—i 400 kĂ¨m cĂ¢u thĂ´ng bĂ¡o (Ä‘á»ƒ má»—i láº§n kiá»ƒm tra chá»‰ cáº§n 1 dĂ²ng)
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    // Kiá»ƒm tra dá»¯ liá»‡u ngÆ°á»i dĂ¹ng gá»­i lĂªn, há»£p lá»‡ thĂ¬ ghi vĂ o nv (chÆ°a lÆ°u xuá»‘ng DB)
    //  - THĂM Má»I (taoMoi = true): nháº­p Ä‘á»§ cĂ¡c trÆ°á»ng.
    //  - Sá»¬A (taoMoi = false): chá»‰ sá»­a há» tĂªn, giá»›i tĂ­nh, ngĂ y sinh, sá»‘ Ä‘iá»‡n thoáº¡i, Ä‘á»‹a chá»‰, vai trĂ².
    //    Email, ngĂ y vĂ o lĂ m, máº­t kháº©u giá»¯ nguyĂªn, dĂ¹ request cĂ³ gá»­i lĂªn cÅ©ng bá» qua.
    private void napDuLieu(NhanVien nv, NhanVienRequest req, boolean taoMoi) {
        Integer idHienTai = nv.getId() == null ? -1 : nv.getId(); // thĂªm má»›i chÆ°a cĂ³ id nĂªn dĂ¹ng -1

        // ===== Pháº§n CHá»ˆ cĂ³ khi THĂM Má»I: email, ngĂ y vĂ o lĂ m, máº­t kháº©u =====
        // Máº·c Ä‘á»‹nh láº¥y giĂ¡ trá»‹ cÅ© cá»§a nhĂ¢n viĂªn (khi sá»­a thĂ¬ giá»¯ nguyĂªn)
        String email = nv.getEmail();
        LocalDate ngayVaoLam = nv.getNgayVaoLam();
        String matKhau = "";
        if (taoMoi) {
            // Email: Ä‘Ăºng Ä‘á»‹nh dáº¡ng vĂ  khĂ´ng trĂ¹ng ngÆ°á»i khĂ¡c
            email = req.email() == null ? "" : req.email().trim().toLowerCase();
            loi(email.isEmpty(), "Nháº­p email.");
            loi(email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"), "Email chÆ°a Ä‘Ăºng Ä‘á»‹nh dáº¡ng, vĂ­ dá»¥: ten@footstyle.vn.");
            if (nhanVienRepo.existsByEmailIgnoreCaseAndIdNot(email, idHienTai)) {
                throw ApiException.conflict("Email Ä‘Ă£ Ä‘Æ°á»£c sá»­ dá»¥ng.");
            }

            // NgĂ y vĂ o lĂ m: khĂ´ng nháº­p thĂ¬ láº¥y hĂ´m nay
            ngayVaoLam = req.ngayVaoLam() == null ? LocalDate.now() : req.ngayVaoLam();

            // Máº­t kháº©u: báº¯t buá»™c, 8 - 50 kĂ½ tá»±
            matKhau = req.matKhau() == null ? "" : req.matKhau();
            loi(matKhau.isEmpty(), "Nháº­p máº­t kháº©u cho nhĂ¢n viĂªn má»›i.");
            loi(matKhau.length() < 8 || matKhau.length() > 50, "Máº­t kháº©u tá»« 8 Ä‘áº¿n 50 kĂ½ tá»±.");
        }

        // ===== Pháº§n dĂ¹ng cho cáº£ THĂM Má»I vĂ  Sá»¬A =====
        // Há» tĂªn
        String hoTen = req.hoTen() == null ? "" : req.hoTen().trim().replaceAll("\\s+", " ");
        loi(hoTen.isEmpty(), "Nháº­p há» tĂªn.");
        loi(hoTen.length() < 2 || hoTen.length() > 60, "Há» tĂªn tá»« 2 Ä‘áº¿n 60 kĂ½ tá»±.");

        // Sá»‘ Ä‘iá»‡n thoáº¡i: 10 sá»‘, Ä‘áº§u sá»‘ 03/05/07/08/09, khĂ´ng trĂ¹ng ngÆ°á»i khĂ¡c
        String sdt = req.soDienThoai() == null ? "" : req.soDienThoai().trim();
        loi(sdt.isEmpty(), "Nháº­p sá»‘ Ä‘iá»‡n thoáº¡i.");
        loi(!sdt.matches("^0[35789]\\d{8}$"), "Sá»‘ Ä‘iá»‡n thoáº¡i gá»“m 10 chá»¯ sá»‘, báº¯t Ä‘áº§u báº±ng 03, 05, 07, 08 hoáº·c 09.");
        if (nhanVienRepo.existsBySdtAndIdNot(sdt, idHienTai)) {
            throw ApiException.conflict("Sá»‘ Ä‘iá»‡n thoáº¡i Ä‘Ă£ Ä‘Æ°á»£c sá»­ dá»¥ng.");
        }

        // Giá»›i tĂ­nh (khĂ´ng báº¯t buá»™c): chá»¯ -> sá»‘ lÆ°u trong DB
        Integer gioiTinh = null;
        if (req.gioiTinh() != null && !req.gioiTinh().isBlank()) {
            loi(!req.gioiTinh().equals("Nam") && !req.gioiTinh().equals("Ná»¯"), "Giá»›i tĂ­nh khĂ´ng há»£p lá»‡.");
            gioiTinh = req.gioiTinh().equals("Nam") ? NhanVien.GIOI_TINH_NAM : NhanVien.GIOI_TINH_NU;
        }

        // NgĂ y sinh: pháº£i Ä‘á»§ 18 tuá»•i
        LocalDate ngaySinh = req.ngaySinh();
        loi(ngaySinh != null && ngaySinh.isAfter(LocalDate.now().minusYears(18)), "NhĂ¢n viĂªn pháº£i tá»« Ä‘á»§ 18 tuá»•i.");

        // NgĂ y sinh vĂ  ngĂ y vĂ o lĂ m pháº£i khá»›p nhau (vĂ o lĂ m khi Ä‘Ă£ Ä‘á»§ 18 tuá»•i)
        if (ngayVaoLam != null && ngaySinh != null && ngayVaoLam.isBefore(ngaySinh.plusYears(18))) {
            if (taoMoi) {
                throw ApiException.badRequest("NgĂ y vĂ o lĂ m khĂ´ng há»£p lá»‡ so vá»›i ngĂ y sinh (pháº£i tá»« Ä‘á»§ 18 tuá»•i).");
            }
            // Khi sá»­a, ngĂ y vĂ o lĂ m khĂ´ng Ä‘á»•i Ä‘Æ°á»£c nĂªn lá»—i náº±m á»Ÿ ngĂ y sinh
            String ngay = ngayVaoLam.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            throw ApiException.badRequest("NgĂ y sinh khĂ´ng há»£p lá»‡: nhĂ¢n viĂªn pháº£i Ä‘á»§ 18 tuá»•i vĂ o ngĂ y vĂ o lĂ m (" + ngay + ").");
        }

        // Äá»‹a chá»‰
        String diaChi = req.diaChi() == null ? "" : req.diaChi().trim();
        loi(diaChi.isEmpty(), "Nháº­p Ä‘á»‹a chá»‰.");
        loi(diaChi.length() > 255, "Äá»‹a chá»‰ tá»‘i Ä‘a 255 kĂ½ tá»±.");

        // Vai trĂ²: pháº£i tá»“n táº¡i vĂ  Ä‘ang Ä‘Æ°á»£c sá»­ dá»¥ng
        loi(req.idVaiTro() == null, "Chá»n vai trĂ².");
        VaiTro vaiTro = vaiTroRepo.findById(req.idVaiTro())
                .orElseThrow(() -> ApiException.badRequest("Vai trĂ² khĂ´ng tá»“n táº¡i."));
        loi(vaiTro.getTrangThai() == null || vaiTro.getTrangThai() != 1, "Vai trĂ² nĂ y Ä‘ang ngÆ°ng sá»­ dá»¥ng.");

        // Dá»¯ liá»‡u há»£p lá»‡ háº¿t -> ghi vĂ o entity
        nv.setHoTen(hoTen);
        nv.setSdt(sdt);
        nv.setGioiTinh(gioiTinh);
        nv.setNgaySinh(ngaySinh);
        nv.setDiaChi(diaChi);
        nv.setVaiTro(vaiTro);
        if (taoMoi) { // chá»‰ thĂªm má»›i má»›i Ä‘Æ°á»£c ghi 3 trÆ°á»ng nĂ y
            nv.setEmail(email);
            nv.setNgayVaoLam(ngayVaoLam);
            nv.setMatKhau(passwordEncoder.encode(matKhau)); // lÆ°u dáº¡ng bÄƒm BCrypt, khĂ´ng lÆ°u máº­t kháº©u gá»‘c
        }
    }

    // Äá»•i entity thĂ nh dá»¯ liá»‡u tráº£ vá» cho frontend (khĂ´ng tráº£ máº­t kháº©u)
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
