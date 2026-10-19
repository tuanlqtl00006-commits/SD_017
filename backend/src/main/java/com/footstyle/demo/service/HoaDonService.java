package com.footstyle.demo.service;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
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

    private static final String KHACH_VANG_LAI = "Khách vãng lai";

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final HinhAnhSanPhamRepository hinhAnhRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    /* ===================== Danh sách ===================== */

    /** Điều kiện lọc dùng chung cho danh sách và bộ đếm theo trạng thái (trangThai = null: không lọc theo trạng thái). */
    private Specification<HoaDon> boLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                        Integer loaiDon, Integer trangThai) {
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            throw ApiException.badRequest("Từ ngày phải trước hoặc cùng ngày với Đến ngày.");
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (maHoaDon != null && !maHoaDon.trim().isEmpty()) {
                // Thoát % và _ để gõ "HD_1" không bị hiểu là ký tự đại diện của LIKE
                predicates.add(cb.like(root.get("maHoaDon"), "%" + thoatLike(maHoaDon.trim()) + "%", '\\'));
            }
            if (tuNgay != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ngayTao"), tuNgay.atStartOfDay()));
            }
            if (denNgay != null) {
                // Lấy trọn ngày "đến": < 0h ngày hôm sau (không dùng <= 23:59:59 vì sẽ sót hóa đơn tạo lúc 23:59:59.xxx)
                predicates.add(cb.lessThan(root.get("ngayTao"), denNgay.plusDays(1).atStartOfDay()));
            }
            if (loaiDon != null) {
                predicates.add(cb.equal(root.get("loaiDon"), loaiDon));
            }
            if (trangThai != null) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Số hóa đơn theo từng trạng thái (đã áp các bộ lọc mã / ngày / loại đơn), để hiện số trên các tab. Khóa "tatCa" = tổng. */
    @Transactional(readOnly = true)
    public Map<String, Long> demTheoTrangThai(String maHoaDon, LocalDate tuNgay, LocalDate denNgay, Integer loaiDon) {
        Map<String, Long> ketQua = new LinkedHashMap<>();
        ketQua.put("tatCa", hoaDonRepository.count(boLoc(maHoaDon, tuNgay, denNgay, loaiDon, null)));
        // Mã 0..8, gồm cả 7 (tab "Hoàn tiền", chưa có đơn nào) và 8 (Giao hàng thất bại) để tab nào cũng có số
        for (int tt = HoaDon.DA_HUY; tt <= HoaDon.GIAO_THAT_BAI; tt++) {
            ketQua.put(String.valueOf(tt), hoaDonRepository.count(boLoc(maHoaDon, tuNgay, denNgay, loaiDon, tt)));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public Page<HoaDonRequest> getDanhSachCoLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                                Integer loaiDon, Integer trangThai, Pageable pageable) {
        Specification<HoaDon> spec = boLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai);

        return hoaDonRepository.findAll(spec, pageable).map(hd -> {
            HoaDonRequest request = new HoaDonRequest();
            request.setId(hd.getId());
            request.setMaHoaDon(hd.getMaHoaDon());
            request.setTenKhachHang(tenKhach(hd));
            request.setSdtKhachHang(sdtKhach(hd));
            request.setMaNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getMaNhanVien() : null);
            request.setTenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Không xác định");
            // Số tiền khách phải trả (đã trừ giảm giá, cộng phí ship - đơn tại quầy không có phí ship)
            request.setTongTien(hd.getThanhTien() != null ? thanhTien(hd) : hd.getTongTien());
            request.setNgayTao(hd.getNgayTao());
            request.setTrangThai(hd.getTrangThai());
            request.setLoaiDon(tenLoaiDon(hd.getLoaiDon()));
            return request;
        });
    }

    /**
     * Tên khách: tên trong hồ sơ khách hàng. Đơn tại quầy không gắn hồ sơ là KHÁCH LẺ -> "Khách vãng lai" (cô: "khách lẻ tại sao có số
     * điện thoại"). Đơn giao hàng không gắn hồ sơ thì lấy tên người nhận ghi trên hóa đơn, không có nữa thì "Khách vãng lai".
     */
    private String tenKhach(HoaDon hd) {
        KhachHang kh = hd.getKhachHang();
        if (kh != null && !trong(kh.getHoTen())) {
            return kh.getHoTen().trim();
        }
        if (kh == null && laTaiQuay(hd)) {
            return KHACH_VANG_LAI;
        }
        return trong(hd.getHoTenNguoiNhan()) ? KHACH_VANG_LAI : hd.getHoTenNguoiNhan().trim();
    }

    /**
     * SĐT khách: SĐT trong hồ sơ khách hàng. Khách lẻ mua tại quầy KHÔNG có SĐT (trả null, giao diện hiện "—" hoặc ghi chú khách vãng lai).
     * Chỉ đơn giao hàng mà hồ sơ không có SĐT mới lấy SĐT người nhận ghi trên hóa đơn (cần để liên hệ giao hàng).
     */
    private String sdtKhach(HoaDon hd) {
        KhachHang kh = hd.getKhachHang();
        String sdt = kh != null ? kh.getSdt() : null;
        if (trong(sdt) && !laTaiQuay(hd)) {
            sdt = hd.getSdtNguoiNhan();
        }
        return trong(sdt) ? null : sdt.trim();
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
        int trangThai = hd.getTrangThai() == null ? HoaDon.CHO_XAC_NHAN : hd.getTrangThai();
        info.put("trangThai", hd.getTrangThai());
        info.put("tenTrangThai", HoaDonLuongTrangThai.ten(hd.getTrangThai()));
        // Nút đổi trạng thái giao diện được phép hiện: lấy từ đúng bảng luật mà doiTrangThai dùng để kiểm tra
        info.put("laTrangThaiCuoi", HoaDonLuongTrangThai.laTrangThaiCuoi(trangThai));
        List<Map<String, Object>> chuyenDuoc = new ArrayList<>();
        for (int sang : HoaDonLuongTrangThai.chuyenDuoc(hd.getLoaiDon(), trangThai)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("trangThai", sang);
            m.put("ten", HoaDonLuongTrangThai.ten(sang));
            m.put("batBuocLyDo", HoaDonLuongTrangThai.batBuocLyDo(sang));
            chuyenDuoc.add(m);
        }
        info.put("chuyenDuoc", chuyenDuoc);
        info.put("ngayTao", hd.getNgayTao());
        info.put("ngayCapNhat", hd.getNgayCapNhat());
        info.put("tongTien", soTien(hd.getTongTien()));
        info.put("soTienGiam", soTien(hd.getSoTienGiam()));
        info.put("phiVanChuyen", phiVanChuyen(hd));
        info.put("thanhTien", thanhTien(hd));
        info.put("ghiChu", hd.getGhiChu());
        // Đơn tại quầy không có vận chuyển: không đơn vị vận chuyển, phí 0 (cô: "tại quầy sao lại có phí vận chuyển 30 nghìn")
        info.put("donViVanChuyen", laTaiQuay(hd) ? null : hd.getDonViVanChuyen());
        info.put("ngayGiaoDuKien", hd.getNgayGiaoDuKien());
        info.put("ngayGiaoThucTe", hd.getNgayGiaoThucTe());
        info.put("tenNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : null);
        info.put("maNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getMaNhanVien() : null);
        // Phiếu giảm giá đã dùng: mã + loại + giá trị (để in "mã X, giảm 10%" / "giảm 50.000 ₫") - cô: "giảm dùng mã nào, giảm bao nhiêu phần trăm"
        PhieuGiamGia phieu = hd.getPhieuGiamGia();
        info.put("maPhieuGiamGia", phieu != null ? phieu.getMaPhieu() : null);
        info.put("tenPhieuGiamGia", phieu != null ? phieu.getTenPhieu() : null);
        info.put("loaiPhieuGiamGia", phieu != null ? phieu.getLoaiGiam() : null);
        info.put("giaTriPhieuGiamGia", phieu != null ? phieu.getGiaTriGiam() : null);
        info.put("giamToiDaPhieuGiamGia", phieu != null ? phieu.getGiamToiDa() : null);
        String phuongThuc = hd.getIdPhuongThucThanhToan() == null ? null
                : hoaDonRepository.findTenPhuongThucThanhToan(hd.getIdPhuongThucThanhToan());
        info.put("phuongThucThanhToan", phuongThuc);
        response.put("hoaDon", info);

        // Khách hàng
        KhachHang kh = hd.getKhachHang();
        Map<String, Object> khach = new LinkedHashMap<>();
        khach.put("maKhachHang", kh != null ? kh.getMaKhachHang() : null);
        khach.put("khachLe", kh == null); // không gắn hồ sơ khách hàng
        khach.put("ten", tenKhach(hd));
        khach.put("sdt", sdtKhach(hd)); // cùng cách lấy với danh sách: hồ sơ khách; khách lẻ tại quầy thì không có SĐT
        khach.put("email", kh != null ? kh.getEmail() : null);
        response.put("khachHang", khach);

        // Giao hàng (đơn tại quầy nhận hàng ngay tại cửa hàng: không có người nhận / địa chỉ giao)
        Map<String, Object> giao = new LinkedHashMap<>();
        giao.put("hoTenNguoiNhan", laTaiQuay(hd) ? null : hd.getHoTenNguoiNhan());
        giao.put("sdtNguoiNhan", laTaiQuay(hd) ? null : hd.getSdtNguoiNhan());
        giao.put("diaChi", laTaiQuay(hd) ? null : hd.getDiaChiGiaoHang());
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
        for (LichSuHoaDon ls : lichSuHoaDonRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ls.getId());
            m.put("hanhDong", ls.getHanhDong());
            m.put("trangThai", maTrangThaiTuHanhDong(ls.getHanhDong()));
            m.put("nguoiThaoTac", ls.getNguoiThaoTac());
            m.put("thoiGian", ls.getThoiGian());
            m.put("moTa", ls.getMoTa());
            lichSu.add(m);
        }
        response.put("lichSu", lichSu);
        return response;
    }

    /**
     * Tra cứu hóa đơn theo mã (phục vụ quét QR / mã vạch):
     * Tìm chính xác theo mã (không phân biệt hoa thường), hoặc tìm theo id nếu chuỗi chỉ gồm số.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> traCuuTheoMa(String ma) {
        if (ma == null || ma.trim().isEmpty()) {
            throw ApiException.badRequest("Mã hóa đơn không được để trống.");
        }
        String trimmed = ma.trim();
        HoaDon hd = hoaDonRepository.findFirstByMaHoaDonIgnoreCase(trimmed)
                .orElse(null);
        if (hd == null && trimmed.matches("^\\d+$")) {
            try {
                hd = hoaDonRepository.findById(Integer.valueOf(trimmed)).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (hd == null) {
            throw ApiException.notFound("Không tìm thấy hóa đơn có mã \"" + trimmed + "\".");
        }
        return getChiTietHoaDon(hd.getId());
    }

    private List<Map<String, Object>> danhSachSanPham(Integer idHoaDon) {
        // Ảnh của từng sản phẩm (1 câu truy vấn cho tất cả), gom theo sản phẩm. Mỗi dòng hóa đơn lấy ảnh theo MÀU của biến thể,
        // màu chưa có ảnh thì lấy ảnh chính chung của sản phẩm (xem AnhSanPham.anhCuaBienThe).
        Map<Integer, List<HinhAnhSanPham>> anhTheoSp = new HashMap<>();
        for (HinhAnhSanPham a : hinhAnhRepository.findByTrangThaiOrderByLaAnhChinhDescIdAsc(1)) {
            anhTheoSp.computeIfAbsent(a.getSanPham().getId(), k -> new ArrayList<>()).add(a);
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
            String anh = sp == null ? "" : AnhSanPham.anhCuaBienThe(anhTheoSp.getOrDefault(sp.getId(), List.of()),
                    spct.getMauSac() != null ? spct.getMauSac().getId() : null);
            item.put("hinhAnh", anh.isEmpty() ? null : anh);
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
     * Đổi trạng thái theo bảng luật trong {@link HoaDonLuongTrangThai}: đơn giao hàng đi 1 -> 2 -> 3 -> 4 -> 5 -> 6, đơn tại quầy đi thẳng 1 -> 6;
     * hủy được khi chưa giao cho vận chuyển; Đang giao hàng báo được "Giao hàng thất bại" rồi giao lại hoặc hủy.
     * Hủy đơn và Giao hàng thất bại bắt buộc ghi lý do. Đã hủy / Đã hoàn thành là trạng thái cuối, không đổi nữa.
     */
    @Transactional
    public Map<String, Object> doiTrangThai(Integer idHoaDon, HoaDonTrangThaiRequest req) {
        HoaDon hd = timHoaDon(idHoaDon);
        Integer moi = req == null ? null : req.trangThaiMoi();
        if (!HoaDonLuongTrangThai.hopLe(moi)) {
            throw ApiException.badRequest("Trạng thái mới không hợp lệ.");
        }
        int hienTai = hd.getTrangThai() == null ? HoaDon.CHO_XAC_NHAN : hd.getTrangThai();
        if (HoaDonLuongTrangThai.laTrangThaiCuoi(hienTai)) {
            throw ApiException.badRequest(HoaDonLuongTrangThai.lyDoDaKetThuc(hienTai));
        }
        if (!HoaDonLuongTrangThai.chuyenDuoc(hd.getLoaiDon(), hienTai).contains(moi)) {
            throw ApiException.badRequest(HoaDonLuongTrangThai.lyDoKhongChuyenDuoc(hienTai, moi));
        }

        String ghiChu = req.ghiChu() == null ? "" : req.ghiChu().trim();
        if (ghiChu.isEmpty() && HoaDonLuongTrangThai.batBuocLyDo(moi)) {
            throw ApiException.badRequest(moi == HoaDon.DA_HUY
                    ? "Vui lòng nhập lý do hủy đơn."
                    : "Vui lòng nhập lý do giao hàng thất bại.");
        }
        if (ghiChu.length() > 500) {
            throw ApiException.badRequest("Ghi chú tối đa 500 ký tự.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (moi == HoaDon.DA_HUY) {
            hoanTonKho(hd); // cả khi hủy sau giao hàng thất bại: hàng đã về cửa hàng
        }
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
            BigDecimal conLai = thanhTien(hd).subtract(daTra);
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
        ls.setHanhDong(HoaDonLuongTrangThai.ten(moi));
        ls.setThoiGian(now);
        ls.setMoTa(ghiChu.isEmpty() ? HoaDonLuongTrangThai.moTaMacDinh(hienTai, moi) : ghiChu);
        String nguoi = req.nguoiThaoTac() == null ? "" : req.nguoiThaoTac().trim();
        ls.setNguoiThaoTac(nguoi.isEmpty() ? "Quản lý" : nguoi);
        lichSuHoaDonRepository.save(ls);

        return getChiTietHoaDon(hd.getId());
    }

    /* ===================== Hàm phụ ===================== */

    /** Hủy đơn: trả số lượng của từng dòng hóa đơn về tồn kho biến thể (gộp các dòng cùng biến thể). */
    private void hoanTonKho(HoaDon hd) {
        Map<Integer, SanPhamChiTiet> bienThe = new LinkedHashMap<>();
        Map<Integer, Integer> soLuongHoan = new HashMap<>();
        for (HoaDonChiTiet ct : hoaDonChiTietRepository.findByHoaDon_IdOrderByIdAsc(hd.getId())) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            int sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            if (spct == null || spct.getId() == null || sl <= 0) {
                continue;
            }
            bienThe.putIfAbsent(spct.getId(), spct);
            soLuongHoan.merge(spct.getId(), sl, Integer::sum);
        }
        for (Map.Entry<Integer, SanPhamChiTiet> e : bienThe.entrySet()) {
            SanPhamChiTiet spct = e.getValue();
            int ton = spct.getSoLuongTon() == null ? 0 : spct.getSoLuongTon();
            spct.setSoLuongTon(ton + soLuongHoan.get(e.getKey()));
            spct.setNgayCapNhat(LocalDateTime.now());
            sanPhamChiTietRepository.save(spct);
        }
    }

    private static String thoatLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private HoaDon timHoaDon(Integer id) {
        if (id == null) {
            throw ApiException.badRequest("Thiếu mã hóa đơn.");
        }
        return hoaDonRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hóa đơn. Có thể hóa đơn đã bị xóa."));
    }

    /** Đơn tại quầy (kể cả đơn chưa ghi loại): khách nhận hàng ngay tại cửa hàng, không qua đơn vị vận chuyển. */
    private static boolean laTaiQuay(HoaDon hd) {
        return !Integer.valueOf(HoaDon.GIAO_HANG).equals(hd.getLoaiDon());
    }

    private static boolean trong(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Phí vận chuyển của hóa đơn: đơn tại quầy luôn 0 dù cột phi_van_chuyen còn giá trị cũ (vd 30.000) trong dữ liệu nhập sai. */
    private static BigDecimal phiVanChuyen(HoaDon hd) {
        return laTaiQuay(hd) ? BigDecimal.ZERO : soTien(hd.getPhiVanChuyen());
    }

    /** Thành tiền phải trả: đơn tại quầy bỏ phần phí vận chuyển còn sót trong dữ liệu cũ để ba con số tổng - giảm + phí = thành tiền luôn khớp. */
    private static BigDecimal thanhTien(HoaDon hd) {
        BigDecimal tt = soTien(hd.getThanhTien());
        BigDecimal phiLuu = soTien(hd.getPhiVanChuyen());
        if (laTaiQuay(hd) && phiLuu.signum() > 0) {
            tt = tt.subtract(phiLuu).max(BigDecimal.ZERO);
        }
        return tt;
    }

    private static String tenLoaiDon(Integer loaiDon) {
        return Integer.valueOf(HoaDon.GIAO_HANG).equals(loaiDon) ? "Trực tuyến" : "Tại quầy";
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
        // Lịch sử ghi trước khi đổi tên trạng thái vẫn dùng chữ cũ
        if (h.equalsIgnoreCase("Chờ lấy hàng")) {
            return HoaDon.CHO_LAY_HANG;
        }
        if (h.equalsIgnoreCase("Hoàn thành")) {
            return HoaDon.HOAN_THANH;
        }
        for (Map.Entry<Integer, String> e : HoaDonLuongTrangThai.tatCaTen().entrySet()) {
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
