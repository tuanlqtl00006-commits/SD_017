package com.footstyle.demo.service;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoaDonService {

    /** Tên hiển thị của từng trạng thái; cũng là chữ ghi vào cột lich_su_hoa_don.hanh_dong. */
    private static final Map<Integer, String> TEN_TRANG_THAI = Map.of(
            HoaDon.DA_HUY, "Đã hủy",
            HoaDon.CHO_XAC_NHAN, "Chờ xác nhận",
            HoaDon.DA_XAC_NHAN, "Đã xác nhận",
            HoaDon.CHO_LAY_HANG, "Chờ lấy hàng",
            HoaDon.DANG_GIAO, "Đang giao hàng",
            HoaDon.DA_GIAO, "Đã giao hàng",
            HoaDon.HOAN_THANH, "Hoàn thành");

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final HinhAnhSanPhamRepository hinhAnhRepository;

    /* ===================== Danh sách ===================== */

    @Transactional(readOnly = true)
    public Page<HoaDonRequest> getDanhSachCoLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                                Integer loaiDon, Integer trangThai, Pageable pageable) {
        Specification<HoaDon> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (maHoaDon != null && !maHoaDon.trim().isEmpty()) {
                predicates.add(cb.like(root.get("maHoaDon"), "%" + maHoaDon.trim() + "%"));
            }
            if (tuNgay != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ngayTao"), tuNgay.atStartOfDay()));
            }
            if (denNgay != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("ngayTao"), denNgay.atTime(23, 59, 59)));
            }
            if (loaiDon != null) {
                predicates.add(cb.equal(root.get("loaiDon"), loaiDon));
            }
            if (trangThai != null) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return hoaDonRepository.findAll(spec, pageable).map(hd -> {
            HoaDonRequest request = new HoaDonRequest();
            request.setId(hd.getId());
            request.setMaHoaDon(hd.getMaHoaDon());
            request.setTenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : "Khách lẻ");
            request.setTenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Không xác định");
            // Số tiền khách phải trả (đã trừ giảm giá, cộng phí ship)
            request.setTongTien(hd.getThanhTien() != null ? hd.getThanhTien() : hd.getTongTien());
            request.setNgayTao(hd.getNgayTao());
            request.setTrangThai(hd.getTrangThai());
            request.setLoaiDon(tenLoaiDon(hd.getLoaiDon()));
            return request;
        });
    }

    /* ===================== Chi tiết ===================== */

    @Transactional(readOnly = true)
    public Map<String, Object> getChiTietHoaDon(Integer idHoaDon) {
        HoaDon hd = timHoaDon(idHoaDon);
        Map<String, Object> response = new LinkedHashMap<>();

        // Thông tin chung
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", hd.getId());
        info.put("maHoaDon", hd.getMaHoaDon());
        info.put("loaiDon", hd.getLoaiDon() == null ? HoaDon.TAI_QUAY : hd.getLoaiDon());
        info.put("tenLoaiDon", tenLoaiDon(hd.getLoaiDon()));
        info.put("trangThai", hd.getTrangThai());
        info.put("tenTrangThai", TEN_TRANG_THAI.getOrDefault(hd.getTrangThai(), "Không xác định"));
        info.put("ngayTao", hd.getNgayTao());
        info.put("ngayCapNhat", hd.getNgayCapNhat());
        info.put("tongTien", soTien(hd.getTongTien()));
        info.put("soTienGiam", soTien(hd.getSoTienGiam()));
        info.put("phiVanChuyen", soTien(hd.getPhiVanChuyen()));
        info.put("thanhTien", soTien(hd.getThanhTien()));
        info.put("ghiChu", hd.getGhiChu());
        info.put("donViVanChuyen", hd.getDonViVanChuyen());
        info.put("ngayGiaoDuKien", hd.getNgayGiaoDuKien());
        info.put("ngayGiaoThucTe", hd.getNgayGiaoThucTe());
        info.put("tenNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : null);
        info.put("maNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getMaNhanVien() : null);
        info.put("maPhieuGiamGia", hd.getPhieuGiamGia() != null ? hd.getPhieuGiamGia().getMaPhieu() : null);
        info.put("tenPhieuGiamGia", hd.getPhieuGiamGia() != null ? hd.getPhieuGiamGia().getTenPhieu() : null);
        String phuongThuc = hd.getIdPhuongThucThanhToan() == null ? null
                : hoaDonRepository.findTenPhuongThucThanhToan(hd.getIdPhuongThucThanhToan());
        info.put("phuongThucThanhToan", phuongThuc);
        response.put("hoaDon", info);

        // Khách hàng
        KhachHang kh = hd.getKhachHang();
        Map<String, Object> khach = new LinkedHashMap<>();
        khach.put("maKhachHang", kh != null ? kh.getMaKhachHang() : null);
        khach.put("ten", kh != null ? kh.getHoTen() : "Khách vãng lai");
        khach.put("sdt", kh != null ? kh.getSdt() : null);
        khach.put("email", kh != null ? kh.getEmail() : null);
        response.put("khachHang", khach);

        // Giao hàng
        Map<String, Object> giao = new LinkedHashMap<>();
        giao.put("hoTenNguoiNhan", hd.getHoTenNguoiNhan());
        giao.put("sdtNguoiNhan", hd.getSdtNguoiNhan());
        giao.put("diaChi", hd.getDiaChiGiaoHang());
        response.put("giaoHang", giao);

        response.put("sanPham", danhSachSanPham(hd.getId()));

        // Thanh toán
        List<Map<String, Object>> thanhToan = new ArrayList<>();
        BigDecimal daThanhToan = BigDecimal.ZERO;
        for (LichSuThanhToan tt : lichSuThanhToanRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tt.getId());
            m.put("soTien", soTien(tt.getSoTien()));
            m.put("maGiaoDich", tt.getMaGiaoDich());
            m.put("thoiGian", tt.getThoiGian());
            m.put("trangThai", tt.getTrangThai());
            m.put("moTa", tt.getMoTa());
            m.put("phuongThuc", phuongThuc);
            thanhToan.add(m);
            if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN) {
                daThanhToan = daThanhToan.add(soTien(tt.getSoTien()));
            }
        }
        response.put("thanhToan", thanhToan);
        response.put("daThanhToan", daThanhToan);

        // Lịch sử thao tác (cũ -> mới)
        List<Map<String, Object>> lichSu = new ArrayList<>();
        for (LichSuHoaDon ls : lichSuHoaDonRepository.findByHoaDon_IdOrderByNgayTaoAscIdAsc(hd.getId())) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ls.getId());
            m.put("hanhDong", ls.getTrangThai());
            m.put("trangThai", maTrangThaiTuHanhDong(ls.getTrangThai()));
            m.put("nguoiThaoTac", ls.getNguoiTao());
            m.put("thoiGian", ls.getNgayTao());
            m.put("moTa", ls.getGhiChu());
            lichSu.add(m);
        }
        response.put("lichSu", lichSu);
        return response;
    }

    private List<Map<String, Object>> danhSachSanPham(Integer idHoaDon) {
        // Ảnh chính của từng sản phẩm (1 câu truy vấn cho tất cả)
        Map<Integer, String> anhChinh = new HashMap<>();
        for (HinhAnhSanPham a : hinhAnhRepository.findByTrangThaiOrderByLaAnhChinhDescIdAsc(1)) {
            anhChinh.putIfAbsent(a.getSanPham().getId(), a.getDuongDanHinhAnh());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (HoaDonChiTiet ct : hoaDonChiTietRepository.findByHoaDon_IdOrderByIdAsc(idHoaDon)) {
            Map<String, Object> item = new HashMap<>();
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            SanPham sp = spct != null ? spct.getSanPham() : null;

            BigDecimal donGia = soTien(ct.getDonGia());
            int soLuong = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            BigDecimal giaGoc = spct != null && spct.getGiaBan() != null ? BigDecimal.valueOf(spct.getGiaBan()) : donGia;
            int phanTramGiam = 0;
            if (giaGoc.signum() > 0 && giaGoc.compareTo(donGia) > 0) {
                phanTramGiam = giaGoc.subtract(donGia).multiply(BigDecimal.valueOf(100))
                        .divide(giaGoc, 0, RoundingMode.HALF_UP).intValue();
            }

            item.put("id", ct.getId());
            item.put("maSpct", spct != null ? spct.getMaSpct() : "---");
            item.put("tenSanPham", sp != null ? sp.getTenSanPham() : "Sản phẩm không xác định");
            item.put("tenDanhMuc", sp != null && sp.getDanhMuc() != null ? sp.getDanhMuc().getTen() : null);
            item.put("tenThuongHieu", sp != null && sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : null);
            item.put("mauSac", spct != null && spct.getMauSac() != null ? spct.getMauSac().getTen() : null);
            item.put("trongLuong", spct != null && spct.getTrongLuong() != null ? spct.getTrongLuong().getTen() : null);
            item.put("chuVi", spct != null && spct.getChuVi() != null ? spct.getChuVi().getTen() : null);
            item.put("hinhAnh", sp != null ? anhChinh.get(sp.getId()) : null);
            item.put("soLuong", soLuong);
            item.put("donGia", donGia);
            item.put("giaGoc", giaGoc);
            item.put("phanTramGiam", phanTramGiam);
            item.put("thanhTien", ct.getThanhTien() != null ? ct.getThanhTien() : donGia.multiply(BigDecimal.valueOf(soLuong)));
            list.add(item);
        }
        return list;
    }

    /* ===================== Đổi trạng thái ===================== */

    /**
     * Đơn giao hàng đi lần lượt 1 -> 2 -> 3 -> 4 -> 5 -> 6; đơn tại quầy đi thẳng 1 -> 6.
     * Chỉ hủy (0) được khi đơn chưa giao cho vận chuyển (trạng thái 1, 2, 3).
     */
    @Transactional
    public Map<String, Object> doiTrangThai(Integer idHoaDon, HoaDonTrangThaiRequest req) {
        HoaDon hd = timHoaDon(idHoaDon);
        Integer moi = req == null ? null : req.trangThaiMoi();
        if (moi == null || !TEN_TRANG_THAI.containsKey(moi)) {
            throw ApiException.badRequest("Trạng thái mới không hợp lệ.");
        }
        int hienTai = hd.getTrangThai() == null ? HoaDon.CHO_XAC_NHAN : hd.getTrangThai();
        if (hienTai == HoaDon.DA_HUY || hienTai == HoaDon.HOAN_THANH) {
            throw ApiException.badRequest(hienTai == HoaDon.DA_HUY
                    ? "Hóa đơn đã bị hủy, không thể đổi trạng thái nữa."
                    : "Hóa đơn đã hoàn thành, không thể đổi trạng thái nữa.");
        }

        String ghiChu = req.ghiChu() == null ? "" : req.ghiChu().trim();
        if (moi == HoaDon.DA_HUY) {
            if (hienTai > HoaDon.CHO_LAY_HANG) {
                throw ApiException.badRequest("Đơn đã giao cho đơn vị vận chuyển, không thể hủy.");
            }
            if (ghiChu.isEmpty()) {
                throw ApiException.badRequest("Vui lòng nhập lý do hủy đơn.");
            }
        } else if (moi != trangThaiTiepTheo(hd.getLoaiDon(), hienTai)) {
            throw ApiException.badRequest("Không thể chuyển từ \"" + TEN_TRANG_THAI.get(hienTai) + "\" sang \""
                    + TEN_TRANG_THAI.get(moi) + "\". Vui lòng tải lại trang.");
        }
        if (ghiChu.length() > 500) {
            throw ApiException.badRequest("Ghi chú tối đa 500 ký tự.");
        }

        LocalDateTime now = LocalDateTime.now();
        hd.setTrangThai(moi);
        hd.setNgayCapNhat(now);
        if (moi == HoaDon.DA_GIAO) {
            hd.setNgayGiaoThucTe(now);
        }
        hoaDonRepository.save(hd);

        // Hoàn thành mà khách chưa trả đủ (đơn COD / tại quầy): ghi nhận phần tiền còn lại
        if (moi == HoaDon.HOAN_THANH) {
            BigDecimal daTra = BigDecimal.ZERO;
            for (LichSuThanhToan tt : lichSuThanhToanRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
                if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN) {
                    daTra = daTra.add(soTien(tt.getSoTien()));
                }
            }
            BigDecimal conLai = soTien(hd.getThanhTien()).subtract(daTra);
            if (conLai.signum() > 0) {
                LichSuThanhToan tt = new LichSuThanhToan();
                tt.setHoaDon(hd);
                tt.setSoTien(conLai);
                tt.setThoiGian(now);
                tt.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
                tt.setMoTa(Integer.valueOf(HoaDon.GIAO_HANG).equals(hd.getLoaiDon())
                        ? "Thu tiền khi giao hàng (COD)" : "Thanh toán đơn tại cửa hàng");
                lichSuThanhToanRepository.save(tt);
            }
        }

        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setTrangThai(TEN_TRANG_THAI.get(moi));
        ls.setNgayTao(now);
        ls.setGhiChu(ghiChu.isEmpty() ? "Chuyển sang \"" + TEN_TRANG_THAI.get(moi) + "\"" : ghiChu);
        String nguoi = req.nguoiThaoTac() == null ? "" : req.nguoiThaoTac().trim();
        ls.setNguoiTao(nguoi.isEmpty() ? "Quản lý" : nguoi);
        lichSuHoaDonRepository.save(ls);

        return getChiTietHoaDon(hd.getId());
    }

    /* ===================== Hàm phụ ===================== */

    private HoaDon timHoaDon(Integer id) {
        if (id == null) {
            throw ApiException.badRequest("Thiếu mã hóa đơn.");
        }
        return hoaDonRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hóa đơn. Có thể hóa đơn đã bị xóa."));
    }

    private static int trangThaiTiepTheo(Integer loaiDon, int hienTai) {
        if (Integer.valueOf(HoaDon.GIAO_HANG).equals(loaiDon)) {
            return hienTai + 1;
        }
        return HoaDon.HOAN_THANH; // tại quầy: thanh toán xong là hoàn thành
    }

    private static String tenLoaiDon(Integer loaiDon) {
        return Integer.valueOf(HoaDon.GIAO_HANG).equals(loaiDon) ? "Giao hàng" : "Tại quầy";
    }

    /** "Chờ xác nhận" -> 1 ... để frontend vẽ dòng thời gian; hành động khác (vd ghi chú) trả null. */
    private static Integer maTrangThaiTuHanhDong(String hanhDong) {
        if (hanhDong == null) {
            return null;
        }
        String h = hanhDong.trim();
        if (h.equalsIgnoreCase("Tạo đơn hàng")) {
            return HoaDon.CHO_XAC_NHAN;
        }
        if (h.equalsIgnoreCase("Hủy đơn hàng")) {
            return HoaDon.DA_HUY;
        }
        for (Map.Entry<Integer, String> e : TEN_TRANG_THAI.entrySet()) {
            if (e.getValue().equalsIgnoreCase(h)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static BigDecimal soTien(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
