package com.footstyle.demo.service;

import com.footstyle.demo.dto.DiaChiNhanVienDto;
import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.dto.VaiTroResponse;
import com.footstyle.demo.dto.ViTriRequest;
import com.footstyle.demo.dto.ViTriResponse;
import com.footstyle.demo.entity.DiaChiNhanVien;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.entity.ViTri;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DiaChiNhanVienRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.VaiTroRepository;
import com.footstyle.demo.repository.ViTriRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NhanVienService {

    private final NhanVienRepository nhanVienRepo;
    private final DiaChiNhanVienRepository diaChiRepo;
    private final VaiTroRepository vaiTroRepo;
    private final ViTriRepository viTriRepo;
    private final BCryptPasswordEncoder passwordEncoder;

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

    // Danh sách vai trò đang dùng, đổ vào ô chọn trên form
    @Transactional(readOnly = true)
    public List<VaiTroResponse> getVaiTro() {
        List<VaiTroResponse> ketQua = new ArrayList<>();
        for (VaiTro v : vaiTroRepo.findByTrangThaiOrderByIdAsc(1)) {
            ketQua.add(new VaiTroResponse(v.getId(), v.getTenVaiTro()));
        }
        return ketQua;
    }

    // Danh sách vị trí làm việc đã thêm, đổ vào ô chọn nhiều vị trí trên form
    @Transactional(readOnly = true)
    public List<ViTriResponse> getViTri() {
        List<ViTriResponse> ketQua = new ArrayList<>();
        for (ViTri v : viTriRepo.findByTrangThaiOrderByIdAsc(ViTri.DANG_DUNG)) {
            ketQua.add(new ViTriResponse(v.getId(), v.getTenViTri()));
        }
        return ketQua;
    }

    // Thêm vị trí làm việc mới (không có chức năng xóa vị trí)
    @Transactional
    public ViTriResponse themViTri(ViTriRequest req) {
        String ten = req == null || req.ten() == null ? "" : req.ten().trim().replaceAll("\\s+", " ");
        loi(ten.isEmpty(), "Nhập tên vị trí.");
        loi(ten.length() < 2 || ten.length() > 50, "Tên vị trí từ 2 đến 50 ký tự.");
        if (viTriRepo.existsByTenViTriIgnoreCase(ten)) {
            throw ApiException.conflict("Vị trí này đã được thêm, hãy chọn trong danh sách.");
        }
        ViTri v = new ViTri();
        v.setTenViTri(ten);
        v.setTrangThai(ViTri.DANG_DUNG);
        v = viTriRepo.save(v);
        return new ViTriResponse(v.getId(), v.getTenViTri());
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public NhanVienResponse them(NhanVienRequest req) {
        NhanVien nv = new NhanVien();
        nv.setTrangThai(NhanVien.HOAT_DONG);
        List<DiaChiNhanVienDto> diaChis = napDuLieu(nv, req, true);
        // Mã tạo SAU khi đã kiểm tra hết thông tin: theo họ tên đầy đủ (vd Nguyễn Văn An -> AnNV01)
        nv.setMaNhanVien(MaNhanVienUtil.taoMa(nv.getHoTen(), nhanVienRepo.findAllMaNhanVien()));
        NhanVien daLuu = nhanVienRepo.save(nv);
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
        NhanVien daLuu = nhanVienRepo.save(nv);
        return toResponse(daLuu, diaChiRepo.findByNhanVienIdOrderByMacDinhDescIdAsc(daLuu.getId()));
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

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo (để mỗi lần kiểm tra chỉ cần 1 dòng)
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    // Kiểm tra dữ liệu người dùng gửi lên, hợp lệ thì ghi vào nv (chưa lưu xuống DB)
    //  - THÊM MỚI (taoMoi = true): nhập đủ các trường.
    //  - SỬA (taoMoi = false): chỉ sửa họ tên, giới tính, ngày sinh, số điện thoại, địa chỉ, vai trò, CCCD, vị trí.
    //    Email, ngày vào làm, mật khẩu giữ nguyên, dù request có gửi lên cũng bỏ qua.
    // Trả về danh sách địa chỉ đã kiểm tra (đúng 1 địa chỉ chính) để lưu sau khi nhân viên đã có id.
    private List<DiaChiNhanVienDto> napDuLieu(NhanVien nv, NhanVienRequest req, boolean taoMoi) {
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

        // Địa chỉ: nhiều địa chỉ, đúng 1 địa chỉ chính
        List<DiaChiNhanVienDto> diaChis = chuanHoaDiaChi(req);

        // Số căn cước công dân (không bắt buộc): đúng 12 chữ số, không trùng người khác
        String cccd = req.cccd() == null ? "" : req.cccd().trim();
        if (!cccd.isEmpty()) {
            loi(!cccd.matches("^\\d{12}$"), "Số căn cước công dân gồm đúng 12 chữ số.");
            if (nhanVienRepo.existsByCccdAndIdNot(cccd, idHienTai)) {
                throw ApiException.conflict("Số căn cước công dân đã được sử dụng.");
            }
        }

        // Vị trí làm việc (không bắt buộc, chọn được nhiều): phải là vị trí đã thêm và đang dùng
        Set<ViTri> viTriChon = new LinkedHashSet<>();
        if (req.idViTri() != null) {
            for (Integer idViTri : new LinkedHashSet<>(req.idViTri())) {
                if (idViTri == null) continue;
                ViTri v = viTriRepo.findById(idViTri).orElseThrow(() -> ApiException.badRequest("Vị trí không tồn tại."));
                loi(v.getTrangThai() == null || v.getTrangThai() != ViTri.DANG_DUNG, "Vị trí \"" + v.getTenViTri() + "\" đang ngưng sử dụng.");
                viTriChon.add(v);
            }
        }

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
        nv.setDiaChi(ghepDiaChi(diaChiChinh(diaChis)));
        nv.setVaiTro(vaiTro);
        nv.setCccd(cccd.isEmpty() ? null : cccd);
        nv.getViTriList().clear();
        nv.getViTriList().addAll(viTriChon);
        if (taoMoi) { // chỉ thêm mới mới được ghi 3 trường này
            nv.setEmail(email);
            nv.setNgayVaoLam(ngayVaoLam);
            nv.setMatKhau(passwordEncoder.encode(matKhau)); // lưu dạng băm BCrypt, không lưu mật khẩu gốc
        }
        return diaChis;
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
        int viTriChinh = -1;
        for (int i = 0; i < nguon.size(); i++) {
            DiaChiNhanVienDto d = nguon.get(i);
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
        List<ViTriResponse> danhSachViTri = new ArrayList<>();
        for (ViTri vt : nv.getViTriList()) {
            danhSachViTri.add(new ViTriResponse(vt.getId(), vt.getTenViTri()));
        }
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
                nv.getCccd(),
                danhSachViTri,
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
