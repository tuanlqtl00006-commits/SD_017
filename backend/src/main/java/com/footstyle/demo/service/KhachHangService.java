package com.footstyle.demo.service;

import com.footstyle.demo.dto.KhachHangRequest;
import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.KhachHangRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KhachHangService {

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
        LoiForm loi = new LoiForm();
        String hoTen = kiemTraThongTin(req, null, loi);

        String ma = QuyTacDuLieu.cat(req.getMaKhachHang());
        if (!ma.isEmpty()) {
            if (!ma.matches("[A-Za-z0-9_-]{2,20}")) {
                loi.them("maKhachHang", "Mã khách hàng gồm 2-20 ký tự chữ, số, gạch ngang hoặc gạch dưới, không dấu, không khoảng trắng.");
            } else if (repository.existsByMaKhachHang(ma)) {
                loi.trung("maKhachHang", "Mã khách hàng đã tồn tại.");
            }
        }

        // Địa chỉ giao hàng (không bắt buộc ở backend, nhưng đã nhập thì phải đủ Tỉnh / Phường / Địa chỉ cụ thể)
        String tinh = QuyTacDuLieu.cat(req.getTinhThanh());
        String phuong = QuyTacDuLieu.cat(req.getPhuongXa());
        String chiTiet = QuyTacDuLieu.cat(req.getDiaChiCuThe());
        String tenNguoiNhan = QuyTacDuLieu.chuanHoaHoTen(req.getTenNguoiNhan());
        String sdtNguoiNhan = QuyTacDuLieu.cat(req.getSdtNguoiNhan());
        boolean coDiaChi = !tinh.isEmpty() || !phuong.isEmpty() || !chiTiet.isEmpty();
        if (coDiaChi) {
            loi.neu(tinh.isEmpty(), "tinhThanh", "Chọn Tỉnh/Thành phố.");
            loi.neu(phuong.isEmpty(), "phuongXa", "Chọn Phường/Xã.");
            loi.neu(chiTiet.isEmpty(), "diaChiCuThe", "Nhập địa chỉ cụ thể (số nhà, đường...).");
            loi.neu(chiTiet.length() > 255, "diaChiCuThe", "Địa chỉ cụ thể tối đa 255 ký tự.");
            if (!chiTiet.isEmpty() && chiTiet.length() <= 255 && chiTiet.codePoints().noneMatch(Character::isLetterOrDigit)) {
                loi.them("diaChiCuThe", "Địa chỉ cụ thể phải có chữ hoặc số (ví dụ: 12 Trần Phú).");
            }
            // Người nhận: để trống thì lấy theo khách hàng; đã nhập thì phải đúng quy tắc như họ tên / số điện thoại khách hàng
            if (!tenNguoiNhan.isEmpty()) {
                if (tenNguoiNhan.length() < 2 || tenNguoiNhan.length() > 100) loi.them("tenNguoiNhan", "Họ tên từ 2 đến 100 ký tự.");
                else if (!QuyTacDuLieu.HO_TEN.matcher(tenNguoiNhan).matches()) loi.them("tenNguoiNhan", QuyTacDuLieu.LOI_HO_TEN_KY_TU);
            }
            if (!sdtNguoiNhan.isEmpty() && !QuyTacDuLieu.SDT.matcher(sdtNguoiNhan).matches()) {
                loi.them("sdtNguoiNhan", QuyTacDuLieu.LOI_SDT);
            }
        }
        loi.nemNeuCoLoi();

        String sdt = QuyTacDuLieu.cat(req.getSdt());
        String email = QuyTacDuLieu.cat(req.getEmail()).toLowerCase(Locale.ROOT);
        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(!ma.isEmpty() ? ma : taoMaMoi());
        kh.setHoTen(hoTen);
        kh.setSdt(sdt);
        kh.setEmail(email);
        kh.setNgaySinh(req.getNgaySinh());
        kh.setGioiTinh(req.getGioiTinh() != null ? req.getGioiTinh() : 1);
        kh.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
        String anh = QuyTacDuLieu.cat(req.getAnhDaiDien());
        kh.setAnhDaiDien(anh.isEmpty() ? null : anh);
        // Mật khẩu tạm ngẫu nhiên (lưu dạng băm). Chưa có chức năng đăng nhập cho khách hàng.
        kh.setMatKhau(passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 8)));

        if (coDiaChi) {
            DiaChiKhachHang dc = new DiaChiKhachHang();
            dc.setKhachHang(kh);
            dc.setTenNguoiNhan(!tenNguoiNhan.isEmpty() ? tenNguoiNhan : hoTen);
            dc.setSdtNguoiNhan(!sdtNguoiNhan.isEmpty() ? sdtNguoiNhan : sdt);
            dc.setTinhThanhPho(tinh);
            dc.setQuanHuyen(QuyTacDuLieu.cat(req.getQuanHuyen()).isEmpty() ? null : QuyTacDuLieu.cat(req.getQuanHuyen()));
            dc.setPhuongXa(phuong);
            dc.setDiaChiCuThe(chiTiet);
            dc.setLaDiaChiMacDinh(true);
            dc.setTrangThai(1);
            kh.getDiaChiList().add(dc);
        }
        return repository.save(kh);
    }

    /** Sửa khách hàng: dùng đúng bộ luật như khi thêm (họ tên, SĐT, email bắt buộc; trùng thì không tính chính khách này). */
    @Transactional
    public KhachHang sua(Integer id, KhachHangRequest req) {
        KhachHang kh = layTheoId(id);
        LoiForm loi = new LoiForm();
        String hoTen = kiemTraThongTin(req, kh.getId(), loi);
        loi.nemNeuCoLoi();

        kh.setHoTen(hoTen);
        kh.setSdt(QuyTacDuLieu.cat(req.getSdt()));
        kh.setEmail(QuyTacDuLieu.cat(req.getEmail()).toLowerCase(Locale.ROOT));
        kh.setNgaySinh(req.getNgaySinh());
        if (req.getGioiTinh() != null) kh.setGioiTinh(req.getGioiTinh());
        if (req.getTrangThai() != null) kh.setTrangThai(req.getTrangThai());
        // Ảnh đại diện: không gửi (null) = giữ ảnh cũ; gửi chuỗi rỗng = bỏ ảnh
        if (req.getAnhDaiDien() != null) {
            String anh = QuyTacDuLieu.cat(req.getAnhDaiDien());
            kh.setAnhDaiDien(anh.isEmpty() ? null : anh);
        }
        return repository.save(kh);
    }

    /** Bật / tắt hoạt động (không xóa dữ liệu). */
    @Transactional
    public KhachHang doiTrangThai(Integer id) {
        KhachHang kh = layTheoId(id);
        kh.setTrangThai(Integer.valueOf(1).equals(kh.getTrangThai()) ? 0 : 1);
        return repository.save(kh);
    }

    /**
     * Xóa khách hàng. Khách đã phát sinh hóa đơn / phiếu giảm giá thì cơ sở dữ liệu chặn xóa (khóa ngoại): báo lỗi dễ hiểu
     * (409) và gợi ý chuyển sang "Ngừng hoạt động" thay vì để lộ lỗi SQL.
     */
    @Transactional
    public void xoa(Integer id) {
        repository.delete(layTheoId(id));
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(LOI_XOA_DA_PHAT_SINH);
        }
    }

    public static final String LOI_XOA_DA_PHAT_SINH =
            "Khách hàng đã phát sinh hóa đơn hoặc phiếu giảm giá nên không thể xóa. Hãy chuyển sang trạng thái \"Ngừng hoạt động\".";

    /**
     * Kiểm tra chung cho THÊM và SỬA, ghi lỗi theo từng ô vào `loi`. Trả về họ tên đã chuẩn hóa.
     * idDangSua = id khách đang sửa (không tính là trùng với chính mình), null khi thêm mới.
     * Thứ tự ô: họ tên -> số điện thoại -> email -> ngày sinh -> giới tính -> trạng thái -> ảnh đại diện.
     */
    private String kiemTraThongTin(KhachHangRequest req, Integer idDangSua, LoiForm loi) {
        int idBoQua = idDangSua == null ? -1 : idDangSua;
        LocalDate homNay = LocalDate.now();

        String hoTen = QuyTacDuLieu.chuanHoaHoTen(req.getHoTen());
        if (hoTen.isEmpty()) loi.them("hoTen", "Vui lòng nhập họ tên khách hàng.");
        else if (hoTen.length() < 2 || hoTen.length() > 100) loi.them("hoTen", "Họ tên từ 2 đến 100 ký tự.");
        else if (!QuyTacDuLieu.HO_TEN.matcher(hoTen).matches()) loi.them("hoTen", QuyTacDuLieu.LOI_HO_TEN_KY_TU);

        String sdt = QuyTacDuLieu.cat(req.getSdt());
        if (sdt.isEmpty()) loi.them("sdt", "Vui lòng nhập số điện thoại.");
        else if (!QuyTacDuLieu.SDT.matcher(sdt).matches()) loi.them("sdt", QuyTacDuLieu.LOI_SDT);
        else if (repository.existsBySdtAndIdNot(sdt, idBoQua)) loi.trung("sdt", "Số điện thoại đã tồn tại.");

        String email = QuyTacDuLieu.cat(req.getEmail());
        if (email.isEmpty()) loi.them("email", "Vui lòng nhập email.");
        else if (email.length() > 255 || !QuyTacDuLieu.EMAIL.matcher(email).matches()) loi.them("email", QuyTacDuLieu.LOI_EMAIL);
        else if (repository.existsByEmailIgnoreCaseAndIdNot(email, idBoQua)) loi.trung("email", "Email đã tồn tại.");

        // Ngày sinh không bắt buộc, nhưng đã khai thì phải từ đủ 15 tuổi (BLDS 2015, Điều 21)
        String loiNgaySinh = QuyTacDuLieu.kiemTraNgaySinh(req.getNgaySinh(), KhachHang.TUOI_TOI_THIEU, "Khách hàng", homNay);
        if (loiNgaySinh != null) loi.them("ngaySinh", loiNgaySinh);

        if (req.getGioiTinh() != null && req.getGioiTinh() != 0 && req.getGioiTinh() != 1) {
            loi.them("gioiTinh", "Giới tính không hợp lệ.");
        }
        if (req.getTrangThai() != null && req.getTrangThai() != 0 && req.getTrangThai() != 1) {
            loi.them("trangThai", "Trạng thái không hợp lệ.");
        }
        String loiAnh = QuyTacDuLieu.kiemTraAnhDaiDien(req.getAnhDaiDien());
        if (loiAnh != null) loi.them("anhDaiDien", loiAnh);
        return hoTen;
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
}
