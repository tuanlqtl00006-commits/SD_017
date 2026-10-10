package com.footstyle.demo.service;

import com.footstyle.demo.dto.KhachHangRequest;
import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.KhachHangRepository;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KhachHangService {

    private static final Pattern SDT = Pattern.compile("^[0-9]{10,11}$");

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
        String hoTen = trim(req.getHoTen());
        String sdt = trim(req.getSdt());
        String email = trim(req.getEmail());
        if (hoTen == null) throw ApiException.badRequest("Vui lòng nhập họ tên khách hàng.");
        if (sdt != null && !SDT.matcher(sdt).matches()) {
            throw ApiException.badRequest("Số điện thoại không hợp lệ (chỉ gồm 10-11 chữ số).");
        }
        if (email != null && repository.existsByEmail(email)) throw ApiException.conflict("Email đã tồn tại.");
        if (sdt != null && repository.existsBySdt(sdt)) throw ApiException.conflict("Số điện thoại đã tồn tại.");

        String ma = trim(req.getMaKhachHang());
        if (ma != null && repository.existsByMaKhachHang(ma)) throw ApiException.conflict("Mã khách hàng đã tồn tại.");

        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(ma != null ? ma : taoMaMoi());
        kh.setHoTen(hoTen);
        kh.setSdt(sdt);
        kh.setEmail(email);
        kh.setNgaySinh(req.getNgaySinh());
        kh.setGioiTinh(req.getGioiTinh() != null ? req.getGioiTinh() : 1);
        kh.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
        // Mật khẩu tạm ngẫu nhiên (lưu dạng băm). Chưa có chức năng đăng nhập cho khách hàng.
        kh.setMatKhau(passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 8)));

        if (trim(req.getTinhThanh()) != null || trim(req.getDiaChiCuThe()) != null) {
            DiaChiKhachHang dc = new DiaChiKhachHang();
            dc.setKhachHang(kh);
            dc.setTenNguoiNhan(trim(req.getTenNguoiNhan()) != null ? trim(req.getTenNguoiNhan()) : hoTen);
            dc.setSdtNguoiNhan(trim(req.getSdtNguoiNhan()) != null ? trim(req.getSdtNguoiNhan()) : (sdt != null ? sdt : ""));
            dc.setTinhThanhPho(trim(req.getTinhThanh()));
            dc.setQuanHuyen(trim(req.getQuanHuyen()));
            dc.setPhuongXa(trim(req.getPhuongXa()));
            dc.setDiaChiCuThe(trim(req.getDiaChiCuThe()) != null ? trim(req.getDiaChiCuThe()) : "");
            dc.setLaDiaChiMacDinh(true);
            dc.setTrangThai(1);
            kh.getDiaChiList().add(dc);
        }
        return repository.save(kh);
    }

    @Transactional
    public KhachHang sua(Integer id, KhachHangRequest req) {
        KhachHang kh = layTheoId(id);
        String sdt = trim(req.getSdt());
        String email = trim(req.getEmail());
        if (sdt != null && !SDT.matcher(sdt).matches()) {
            throw ApiException.badRequest("Số điện thoại không hợp lệ (chỉ gồm 10-11 chữ số).");
        }
        if (email != null && !email.equals(kh.getEmail()) && repository.existsByEmail(email)) {
            throw ApiException.conflict("Email đã tồn tại.");
        }
        if (sdt != null && !sdt.equals(kh.getSdt()) && repository.existsBySdt(sdt)) {
            throw ApiException.conflict("Số điện thoại đã tồn tại.");
        }
        if (trim(req.getHoTen()) != null) kh.setHoTen(trim(req.getHoTen()));
        kh.setSdt(sdt);
        kh.setEmail(email);
        kh.setNgaySinh(req.getNgaySinh());
        if (req.getGioiTinh() != null) kh.setGioiTinh(req.getGioiTinh());
        if (req.getTrangThai() != null) kh.setTrangThai(req.getTrangThai());
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
        repository.delete(layTheoId(id));
        repository.flush();
    }

    private String taoMaMoi() {
        long n = repository.count() + 1;
        String ma = String.format("KH%03d", n);
        while (repository.existsByMaKhachHang(ma)) {
            n++;
            ma = String.format("KH%03d", n);
        }
        return ma;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
