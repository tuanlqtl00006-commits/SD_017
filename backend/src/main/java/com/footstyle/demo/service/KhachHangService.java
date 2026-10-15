package com.footstyle.demo.service;

import com.footstyle.demo.dto.KhachHangRequest;
import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.KhachHangRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KhachHangService {

    /** Mã khách hàng (nếu tự nhập): chữ, số, gạch ngang, gạch dưới, 2-50 ký tự. Quy tắc các trường khác: KhachHangRules. */
    private static final Pattern MA_KH = Pattern.compile("^[A-Z0-9_-]{2,50}$");

    private final KhachHangRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<KhachHang> timKiem(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.searchKhachHang(search.trim());
        }
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public KhachHang layTheoId(Integer id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng."));
    }

    @Transactional
    public KhachHang them(KhachHangRequest req) {
        // 1. Kiểm tra + chuẩn hóa từng trường (sai là dừng với thông báo rõ ràng)
        String hoTen = KhachHangRules.hoTen(req.getHoTen(), "Họ tên");
        String email = KhachHangRules.email(req.getEmail());
        String sdt = KhachHangRules.sdt(req.getSdt(), "Số điện thoại");
        LocalDate ngaySinh = KhachHangRules.ngaySinh(req.getNgaySinh(), LocalDate.now());
        Integer gioiTinh = KhachHangRules.gioiTinh(req.getGioiTinh());
        Integer trangThai = KhachHangRules.trangThai(req.getTrangThai());
        String anhDaiDien = KhachHangRules.anhDaiDien(req.getAnhDaiDien());

        // Địa chỉ mặc định là bắt buộc khi thêm mới. Tên / SĐT người nhận để trống thì lấy của khách hàng.
        String tenNguoiNhan = KhachHangRules.chuanHoaKhoangTrang(req.getTenNguoiNhan()) == null
                ? hoTen : KhachHangRules.hoTen(req.getTenNguoiNhan(), "Tên người nhận");
        String sdtNguoiNhan = KhachHangRules.chuanHoaSdt(req.getSdtNguoiNhan()) == null
                ? sdt : KhachHangRules.sdt(req.getSdtNguoiNhan(), "Số điện thoại người nhận");
        String tinhThanh = KhachHangRules.tinhThanh(req.getTinhThanh());
        String phuongXa = KhachHangRules.phuongXa(req.getPhuongXa());
        String diaChiCuThe = KhachHangRules.diaChiCuThe(req.getDiaChiCuThe());

        // 2. Kiểm tra trùng (email so không phân biệt hoa / thường, SĐT so sau khi đã chuẩn hóa)
        if (repository.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email đã tồn tại.");
        if (repository.existsBySdt(sdt)) throw ApiException.conflict("Số điện thoại đã tồn tại.");

        String ma = trim(req.getMaKhachHang());
        if (ma != null) {
            ma = ma.toUpperCase();
            if (!MA_KH.matcher(ma).matches()) {
                throw ApiException.badRequest("Mã khách hàng chỉ gồm chữ, số, gạch ngang, gạch dưới (2-50 ký tự).");
            }
            if (repository.existsByMaKhachHang(ma)) throw ApiException.conflict("Mã khách hàng đã tồn tại.");
        }

        // 3. Lưu
        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(ma != null ? ma : taoMaMoi());
        kh.setHoTen(hoTen);
        kh.setSdt(sdt);
        kh.setEmail(email);
        kh.setNgaySinh(ngaySinh);
        kh.setGioiTinh(gioiTinh);
        kh.setAnhDaiDien(anhDaiDien);
        kh.setTrangThai(trangThai);
        // Mật khẩu tạm ngẫu nhiên. Chưa có chức năng đăng nhập cho khách hàng.
        kh.setMatKhau(UUID.randomUUID().toString().substring(0, 8)); // Lưu mật khẩu gốc (không mã hóa) để dễ lấy

        DiaChiKhachHang dc = new DiaChiKhachHang();
        dc.setKhachHang(kh);
        dc.setTenNguoiNhan(tenNguoiNhan);
        dc.setSdtNguoiNhan(sdtNguoiNhan);
        dc.setTinhThanhPho(tinhThanh);
        dc.setQuanHuyen(trim(req.getQuanHuyen()));
        dc.setPhuongXa(phuongXa);
        dc.setDiaChiCuThe(diaChiCuThe);
        dc.setLaDiaChiMacDinh(true);
        dc.setTrangThai(1);
        kh.getDiaChiList().add(dc);
        return repository.save(kh);
    }

    @Transactional
    public KhachHang sua(Integer id, KhachHangRequest req) {
        KhachHang kh = layTheoId(id);
        // Khi sửa vẫn bắt buộc họ tên, email, SĐT: không cho xóa trắng thông tin liên hệ
        String hoTen = KhachHangRules.hoTen(req.getHoTen(), "Họ tên");
        String email = KhachHangRules.email(req.getEmail());
        String sdt = KhachHangRules.sdt(req.getSdt(), "Số điện thoại");
        LocalDate ngaySinh = KhachHangRules.ngaySinh(req.getNgaySinh(), LocalDate.now());

        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("Email đã được khách hàng khác sử dụng.");
        }
        if (repository.existsBySdtAndIdNot(sdt, id)) {
            throw ApiException.conflict("Số điện thoại đã được khách hàng khác sử dụng.");
        }

        kh.setHoTen(hoTen);
        kh.setEmail(email);
        kh.setSdt(sdt);
        kh.setNgaySinh(ngaySinh);
        // Không gửi thì giữ nguyên giá trị cũ
        if (req.getGioiTinh() != null) kh.setGioiTinh(KhachHangRules.gioiTinh(req.getGioiTinh()));
        if (req.getTrangThai() != null) kh.setTrangThai(KhachHangRules.trangThai(req.getTrangThai()));
        if (req.getAnhDaiDien() != null) kh.setAnhDaiDien(KhachHangRules.anhDaiDien(req.getAnhDaiDien()));
        return repository.save(kh);
    }

    /** Bật / tắt hoạt động (không xóa dữ liệu). */
    @Transactional
    public KhachHang doiTrangThai(Integer id) {
        KhachHang kh = layTheoId(id);
        kh.setTrangThai(Integer.valueOf(1).equals(kh.getTrangThai()) ? 0 : 1);
        return repository.save(kh);
    }

    @Transactional
    public void xoa(Integer id) {
        KhachHang kh = layTheoId(id);
        try {
            repository.delete(kh);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            // Khách đã có hóa đơn / được tặng phiếu giảm giá: khóa ngoại chặn xóa -> báo rõ thay vì lỗi 500
            throw ApiException.conflict("Khách hàng đã phát sinh hóa đơn hoặc phiếu giảm giá nên không thể xóa. "
                    + "Hãy chuyển sang trạng thái \"Ngừng hoạt động\".");
        }
    }

    private String taoMaMoi() {
        long n = repository.count() + 1;
        String ma = MaUtil.dinhDang("KH", n);
        while (repository.existsByMaKhachHang(ma)) {
            n++;
            ma = MaUtil.dinhDang("KH", n);
        }
        return ma;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
