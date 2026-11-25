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
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
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
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaKhachHangRepository phieuGiamGiaKhachHangRepository;

    /** Gửi mail báo khách khi đơn online đổi trạng thái hoặc bị hủy. Nạp bằng setter (không bắt buộc) để không đổi hàm khởi tạo của lớp. */
    private MailService mailService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    /** Để tìm lại giá gốc lúc bán của dòng hóa đơn (xem giaGocLucBan). Nạp bằng setter; null (test) thì dùng giá hiện tại như cũ. */
    private GiamGiaApDungService giamGiaApDungService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setGiamGiaApDungService(GiamGiaApDungService giamGiaApDungService) {
        this.giamGiaApDungService = giamGiaApDungService;
    }

    /** Kho nhân viên để lưu người xử lý đơn hàng. Nạp bằng setter để tương thích khởi tạo trong test. */
    private NhanVienRepository nhanVienRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setNhanVienRepository(NhanVienRepository nhanVienRepository) {
        this.nhanVienRepository = nhanVienRepository;
    }

    /* ===================== Danh sách ===================== */

    /* ----- Kênh bán: tại quầy / online -----
     * Cột loai_hoa_don chỉ cho biết đơn có giao hàng hay không: đơn bán tại quầy mà bật "Giao hàng" cũng là loại 2 (giao hàng)
     * giống đơn khách đặt online. Không đổi SQL nên kênh bán lấy theo dòng lịch sử lúc tạo đơn: bán quầy ghi "...tại quầy"
     * (HoaDonChoService / BanHangService), đặt online ghi "Đặt hàng trực tuyến" / "Khách đặt hàng online". */
    public static final String KENH_TAI_QUAY = "TAI_QUAY";
    public static final String KENH_ONLINE = "ONLINE";
    static final String DAU_HIEU_TAI_QUAY = "tại quầy";

    private static boolean taoTaiQuay(List<LichSuHoaDon> lichSu) {
        return lichSu.stream().anyMatch(l -> l.getMoTa() != null && l.getMoTa().toLowerCase(java.util.Locale.ROOT).contains(DAU_HIEU_TAI_QUAY));
    }

    /** Điều kiện lọc dùng chung cho danh sách và bộ đếm theo trạng thái (trangThai = null: không lọc theo trạng thái). */
    private Specification<HoaDon> boLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                        Integer loaiDon, Integer trangThai) {
        return boLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, null);
    }

    /** Như trên, thêm `kenh` = TAI_QUAY / ONLINE (null: cả hai). */
    private Specification<HoaDon> boLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                        Integer loaiDon, Integer trangThai, String kenh) {
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            throw ApiException.badRequest("Từ ngày phải trước hoặc cùng ngày với Đến ngày.");
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Hóa đơn chờ tại quầy (nhân viên bấm "Thêm hóa đơn chờ", chưa thanh toán: khách vãng lai, 0 đồng lúc mới tạo) chỉ nằm ở màn
            // Bán hàng tại quầy, không đưa vào Quản lý hóa đơn (góp ý 21/11). Thanh toán xong mới thành hóa đơn thật.
            predicates.add(cb.not(cb.and(cb.equal(root.get("loaiDon"), HoaDon.TAI_QUAY), cb.equal(root.get("trangThai"), HoaDon.CHO_XAC_NHAN))));
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
            if (KENH_TAI_QUAY.equals(kenh) || KENH_ONLINE.equals(kenh)) {
                // Đơn được tạo ở quầy: loại tại quầy, hoặc loại giao hàng có dòng lịch sử "...tại quầy"
                var sq = query.subquery(Integer.class);
                var l = sq.from(LichSuHoaDon.class);
                sq.select(l.get("id")).where(cb.equal(l.get("hoaDon"), root), cb.like(l.get("moTa"), "%" + DAU_HIEU_TAI_QUAY + "%"));
                var taiQuay = cb.or(cb.equal(root.get("loaiDon"), HoaDon.TAI_QUAY), cb.exists(sq));
                predicates.add(KENH_TAI_QUAY.equals(kenh) ? taiQuay : cb.not(taiQuay));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Số hóa đơn theo từng trạng thái (đã áp các bộ lọc mã / ngày / loại đơn), để hiện số trên các tab. Khóa "tatCa" = tổng. */
    @Transactional(readOnly = true)
    public Map<String, Long> demTheoTrangThai(String maHoaDon, LocalDate tuNgay, LocalDate denNgay, Integer loaiDon) {
        return demTheoTrangThai(maHoaDon, tuNgay, denNgay, loaiDon, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> demTheoTrangThai(String maHoaDon, LocalDate tuNgay, LocalDate denNgay, Integer loaiDon, String kenh) {
        Map<String, Long> ketQua = new LinkedHashMap<>();
        ketQua.put("tatCa", hoaDonRepository.count(boLoc(maHoaDon, tuNgay, denNgay, loaiDon, null, kenh)));
        // Mã 0..8, gồm cả 7 (tab "Hoàn tiền", chưa có đơn nào) và 8 (Giao hàng thất bại) để tab nào cũng có số
        for (int tt = HoaDon.DA_HUY; tt <= HoaDon.GIAO_THAT_BAI; tt++) {
            ketQua.put(String.valueOf(tt), hoaDonRepository.count(boLoc(maHoaDon, tuNgay, denNgay, loaiDon, tt, kenh)));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public Page<HoaDonRequest> getDanhSachCoLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                                Integer loaiDon, Integer trangThai, Pageable pageable) {
        return getDanhSachCoLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<HoaDonRequest> getDanhSachCoLoc(String maHoaDon, LocalDate tuNgay, LocalDate denNgay,
                                                Integer loaiDon, Integer trangThai, String kenh, Pageable pageable) {
        Specification<HoaDon> spec = boLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, kenh);
        Page<HoaDon> trang = hoaDonRepository.findAll(spec, pageable);
        // Đơn giao hàng nhưng bán tại quầy (ít): một câu truy vấn cho cả trang, chỉ hỏi khi trang có đơn giao hàng
        java.util.Set<Integer> giaoHangTaiQuay = trang.getContent().stream().anyMatch(h -> Integer.valueOf(HoaDon.GIAO_HANG).equals(h.getLoaiDon()))
                ? new java.util.HashSet<>(lichSuHoaDonRepository.idDonGiaoHangBanTaiQuay("%" + DAU_HIEU_TAI_QUAY + "%"))
                : java.util.Set.of();

        return trang.map(hd -> {
            HoaDonRequest request = new HoaDonRequest();
            request.setId(hd.getId());
            request.setMaHoaDon(hd.getMaHoaDon());
            request.setTenKhachHang(tenKhach(hd));
            request.setSdtKhachHang(sdtKhach(hd));
            request.setMaNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getMaNhanVien() : null);
            request.setTenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Không xác định");
            // Số tiền khách phải trả (đã trừ giảm giá, cộng phí ship - đơn tại quầy không có phí ship)
            request.setTongTien(hd.getThanhTien() != null ? thanhTien(hd) : hd.getTongTien());
            request.setDoanhThu(hd.getThanhTien() != null ? thanhTien(hd).subtract(phiVanChuyen(hd)).max(BigDecimal.ZERO) : hd.getTongTien());
            request.setNgayTao(hd.getNgayTao());
            request.setTrangThai(hd.getTrangThai());
            request.setLoaiDon(tenKenh(hd.getLoaiDon(), giaoHangTaiQuay.contains(hd.getId())));
            request.setHinhThucMua(hinhThucMua(hd.getLoaiDon(), giaoHangTaiQuay.contains(hd.getId())));
            request.setHinhThucThanhToan(tenHinhThucThanhToan(hd.getIdPhuongThucThanhToan(), laTaiQuay(hd) || giaoHangTaiQuay.contains(hd.getId())));
            return request;
        });
    }

    /**
     * Tên khách: tên trong hồ sơ khách hàng. Đơn tại quầy không gắn hồ sơ là KHÁCH LẺ -> "Khách vãng lai".
     * Đơn giao hàng không gắn hồ sơ thì lấy tên người nhận ghi trên hóa đơn, không có nữa thì "Khách vãng lai".
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
        List<LichSuHoaDon> dsLichSu = lichSuHoaDonRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId());
        // Tên kênh bán (đơn bán quầy có giao hàng vẫn là "Tại quầy"); loaiDon (số) vẫn quyết định luồng giao hàng
        info.put("tenLoaiDon", tenKenh(hd.getLoaiDon(), taoTaiQuay(dsLichSu)));
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
        // Quản lý / nhân viên chỉ được sửa đơn MỘT lần, khi đơn giao hàng còn Chờ xác nhận (khách không tự sửa)
        boolean daSua = dsLichSu.stream()
                .anyMatch(l -> HoaDonSuaService.HANH_DONG_SUA.equals(l.getHanhDong()));
        info.put("daChinhSua", daSua);
        // Không cho phép chỉnh sửa số lượng sản phẩm trên đơn online
        info.put("duocChinhSua", false);
        // "Sửa thông tin giao hàng" tách riêng (nhân viên và khách, mỗi đơn 1 lần, trước khi giao cho đơn vị vận chuyển)
        info.put("duocSuaGiaoHang", HoaDonSuaService.duocSuaGiaoHang(hd, dsLichSu));
        info.put("daSuaGiaoHang", dsLichSu.stream().anyMatch(l -> HoaDonSuaService.HANH_DONG_SUA_GIAO.equals(l.getHanhDong())));
        info.put("idPhuongThucThanhToan", hd.getIdPhuongThucThanhToan());
        info.put("ngayTao", hd.getNgayTao());
        info.put("ngayCapNhat", hd.getNgayCapNhat());
        info.put("tongTien", soTien(hd.getTongTien()));
        info.put("soTienGiam", soTien(hd.getSoTienGiam()));
        info.put("phiVanChuyen", phiVanChuyen(hd));
        info.put("thanhTien", thanhTien(hd));
        info.put("ghiChu", hd.getGhiChu());
        // Đơn tại quầy không có vận chuyển: không đơn vị vận chuyển, phí 0
        info.put("donViVanChuyen", laTaiQuay(hd) ? null : hd.getDonViVanChuyen());
        info.put("ngayGiaoDuKien", hd.getNgayGiaoDuKien());
        info.put("ngayGiaoThucTe", hd.getNgayGiaoThucTe());
        info.put("tenNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : null);
        info.put("maNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getMaNhanVien() : null);
        // Phiếu giảm giá đã dùng: mã + loại + giá trị (để in "mã X, giảm 10%" / "giảm 50.000 ₫")
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

        List<Map<String, Object>> dsSanPham = danhSachSanPham(hd);
        response.put("sanPham", dsSanPham);
        // Tiền khách đã được giảm theo đợt (tính một lần ở đây để mọi màn / bản in dùng chung một số).
        // tongTien (tiền hàng ĐÃ trừ đợt) giữ nguyên nghĩa vì Thống kê dùng số này; tổng theo giá gốc = tongTien + tienGiamTheoDot.
        long giamDot = 0;
        for (Map<String, Object> sp : dsSanPham) {
            long goc = ((BigDecimal) sp.get("giaGoc")).longValue();
            long ban = ((BigDecimal) sp.get("donGia")).longValue();
            giamDot += Math.max(0, goc - ban) * ((Integer) sp.get("soLuong"));
        }
        info.put("tienGiamTheoDot", BigDecimal.valueOf(giamDot));

        // Thanh toán. Góp ý 25/11: tên phương thức ghi rõ kênh (Chuyển khoản VietQR / Chuyển khoản ngân hàng...), giống Tổng kết, bản in, Excel
        boolean banTaiQuay = laTaiQuay(hd) || taoTaiQuay(dsLichSu);
        String hinhThucTt = tenHinhThucThanhToan(hd.getIdPhuongThucThanhToan(), banTaiQuay);
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
            String tenDong = tt.getIdPhuongThucThanhToan() != null ? tenHinhThucThanhToan(tt.getIdPhuongThucThanhToan(), banTaiQuay) : null;
            m.put("phuongThuc", tenDong != null ? tenDong : hinhThucTt != null ? hinhThucTt : phuongThuc);
            thanhToan.add(m);
            if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN) {
                daThanhToan = daThanhToan.add(soTien(tt.getSoTien()));
            }
        }
        response.put("thanhToan", thanhToan);
        response.put("daThanhToan", daThanhToan);
        // 24/11 (Task 3.1): tên hình thức thanh toán để hiện ở "Tổng kết thanh toán" / "Lịch sử thanh toán" kể cả khi chưa có giao dịch
        info.put("hinhThucThanhToan", hinhThucTt != null ? hinhThucTt : phuongThuc);
        // Góp ý 25/11: đơn hệ thống tự hủy vì quá 5 phút chưa thanh toán online - Lịch sử thanh toán ghi rõ, không ghi "Không thanh toán"
        info.put("tuHuyQuaHan", trangThai == HoaDon.DA_HUY && dsLichSu.stream()
                .anyMatch(l -> l.getMoTa() != null && l.getMoTa().startsWith(CuaHangService.LY_DO_TU_HUY_QUA_HAN)));
        // 24/11 (Task 3.2 / 3.3): đơn online chuyển khoản còn Chờ xác nhận và chưa nhận đủ tiền -> chưa được "Xác nhận đơn";
        // nhân viên được gửi mail nhắc thanh toán (không tạo QR mới, không gia hạn), tới hạn (lúc đặt + 5 phút) thì đơn tự hủy
        boolean choThanhToanOnline = !laTaiQuay(hd) && HoaDonSuaService.laChuyenKhoanOnline(hd.getIdPhuongThucThanhToan())
                && trangThai == HoaDon.CHO_XAC_NHAN && daThanhToan.compareTo(thanhTien(hd)) < 0;
        response.put("choThanhToanOnline", choThanhToanOnline);
        response.put("duocGuiMailNhacThanhToan", choThanhToanOnline && daThanhToan.signum() <= 0
                && !CuaHangService.laDonTreo(hd, daThanhToan.longValue(), LocalDateTime.now()));
        response.put("hanThanhToan", choThanhToanOnline ? CuaHangService.hanThanhToan(hd) : null);
        // Đơn online khách chọn chuyển khoản: nhân viên kiểm tra sao kê rồi bấm "Xác nhận đã nhận chuyển khoản"
        response.put("duocXacNhanThanhToan", !laTaiQuay(hd) && HoaDonSuaService.laChuyenKhoanOnline(hd.getIdPhuongThucThanhToan())
                && !HoaDonLuongTrangThai.laTrangThaiCuoi(trangThai) && daThanhToan.compareTo(thanhTien(hd)) < 0);
        // Đơn online chuyển khoản chưa thanh toán: admin có quyền đổi sang COD trực tiếp khi khách liên hệ (không phải chờ hết 5 phút)
        response.put("duocDoiSangCod", !laTaiQuay(hd) && HoaDonSuaService.laChuyenKhoanOnline(hd.getIdPhuongThucThanhToan())
                && trangThai == HoaDon.CHO_XAC_NHAN && daThanhToan.compareTo(BigDecimal.ZERO) <= 0);
        // Đơn COD: trước khi chuyển sang Hoàn thành, bắt buộc phải có bước xác nhận đã nhận được tiền
        response.put("duocXacNhanTienCod", !laTaiQuay(hd) && Integer.valueOf(1).equals(hd.getIdPhuongThucThanhToan())
                && (trangThai == HoaDon.DA_GIAO || trangThai == HoaDon.DANG_GIAO) && daThanhToan.compareTo(thanhTien(hd)) < 0);
        // Đơn đã hủy mà khách đã thanh toán tiền (chưa hoàn trả): nhân viên có nút "Xác nhận đã hoàn tiền"
        boolean canHoanTien = (trangThai == HoaDon.DA_HUY) && daThanhToan.compareTo(BigDecimal.ZERO) > 0;
        response.put("duocXacNhanHoanTien", canHoanTien);
        // Đơn online Chờ xác nhận (chưa trừ kho) mà kho không còn đủ: bấm "Xác nhận" thì hiện popup hết hàng
        // (giữ hàng chờ nhập / gửi thư báo hết hàng / hủy đơn và hoàn tiền), góp ý 16/11 [05:20–06:47]
        List<String> dsThieuHang = !laTaiQuay(hd) && trangThai == HoaDon.CHO_XAC_NHAN ? danhSachThieuHang(hd) : List.of();
        response.put("duocBaoHetHang", !dsThieuHang.isEmpty());
        response.put("dsThieuHang", dsThieuHang);
        // Đơn chuyển khoản quá 5 phút chưa nhận tiền: "đơn treo" (chờ khách chuyển hoặc đổi sang COD)
        response.put("donTreo", CuaHangService.laDonTreo(hd, daThanhToan.longValue(), LocalDateTime.now()));
        response.put("canHoanTien", canHoanTien);
        response.put("soTienCanHoan", canHoanTien ? daThanhToan : BigDecimal.ZERO);

        // Lịch sử thao tác (cũ -> mới)
        List<Map<String, Object>> lichSu = new ArrayList<>();
        for (LichSuHoaDon ls : dsLichSu) {
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

    /**
     * Giá gốc LÚC BÁN của một dòng hóa đơn. Bảng chi tiết chỉ lưu đơn giá (không đổi SQL), còn giá niêm yết của sản phẩm có thể
     * đã đổi sau khi bán; dùng thẳng giá hiện tại sẽ làm đơn cũ hiện "giảm theo đợt" sai (tăng giá thì hiện giảm ảo).
     * 1. Giá hiện tại với một mức đợt có thể đã áp lúc bán ra đúng đơn giá (lệch tối đa 1 ₫ do làm tròn) -> giá chưa đổi: lấy giá hiện tại.
     * 2. Giá hiện tại bằng đơn giá -> bán không giảm.
     * 3. Lúc bán có đợt đang bật (mức đầu danh sách) -> tính ngược từ đơn giá.
     * 4. Còn lại (không có đợt nào lúc bán, giá đã đổi) -> coi như không giảm theo đợt, không đoán.
     */
    static long giaGocLucBan(long donGia, long giaHienTai, List<Double> mucCoThe) {
        for (double pt : mucCoThe) {
            if (pt > 0 && Math.abs(PhieuApDung.giaSauDot(giaHienTai, pt) - donGia) <= 1) return giaHienTai;
        }
        if (giaHienTai == donGia) return donGia;
        double pt = mucCoThe.isEmpty() ? 0 : mucCoThe.get(0);
        if (pt > 0 && pt < 100) return Math.round(donGia * 100.0 / (100 - pt));
        return donGia;
    }

    private List<Map<String, Object>> danhSachSanPham(HoaDon hd) {
        Integer idHoaDon = hd.getId();
        // Thời điểm có thể đã chốt giá: lúc tạo (đơn online) và lúc cập nhật gần nhất (đơn quầy chốt giá khi thanh toán)
        List<java.time.LocalDateTime> lucBan = new ArrayList<>();
        if (hd.getNgayTao() != null) lucBan.add(hd.getNgayTao());
        if (hd.getNgayCapNhat() != null && !hd.getNgayCapNhat().equals(hd.getNgayTao())) lucBan.add(hd.getNgayCapNhat());
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
            BigDecimal giaHienTai = spct != null && spct.getGiaBan() != null ? BigDecimal.valueOf(spct.getGiaBan()) : donGia;
            BigDecimal giaGoc = giaHienTai;
            if (giamGiaApDungService != null && spct != null) {
                List<Double> muc = giamGiaApDungService.mucGiamCoThe(spct.getId().longValue(), lucBan);
                giaGoc = BigDecimal.valueOf(giaGocLucBan(donGia.longValue(), giaHienTai.longValue(), muc));
            }
            int phanTramGiam = 0;
            if (giaGoc.signum() > 0 && giaGoc.compareTo(donGia) > 0) {
                phanTramGiam = giaGoc.subtract(donGia).multiply(BigDecimal.valueOf(100))
                        .divide(giaGoc, 0, RoundingMode.HALF_UP).intValue();
            }

            item.put("id", ct.getId());
            item.put("idBienThe", spct != null ? spct.getId() : null);
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
     * chỉ hủy được khi còn Chờ xác nhận (đơn giao hàng: hoặc sau Giao hàng thất bại); Đang giao hàng báo được "Giao hàng thất bại" rồi giao lại hoặc hủy.
     * Hủy đơn và Giao hàng thất bại bắt buộc ghi lý do. Đã hủy / Đã hoàn thành / Hoàn tiền là trạng thái cuối, không đổi nữa.
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

        // Luồng bán hàng: đơn online khách chọn chuyển khoản phải được xác nhận ĐÃ NHẬN TIỀN trước khi xác nhận đơn và trừ kho.
        // (Đơn COD thì xác nhận đơn với khách rồi thu tiền khi giao.)
        // 24/11 (Task 3.2): áp cho mọi cách trả online (chuyển khoản, VNPay, MoMo), không chỉ chuyển khoản ngân hàng
        if (moi == HoaDon.DA_XAC_NHAN && !laTaiQuay(hd)
                && HoaDonSuaService.laChuyenKhoanOnline(hd.getIdPhuongThucThanhToan())) {
            BigDecimal daTra = BigDecimal.ZERO;
            for (LichSuThanhToan tt : lichSuThanhToanRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
                if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN) {
                    daTra = daTra.add(soTien(tt.getSoTien()));
                }
            }
            if (daTra.compareTo(thanhTien(hd)) < 0) {
                throw ApiException.badRequest("Đơn khách chọn chuyển khoản: vui lòng xác nhận đã nhận thanh toán online trước khi xác nhận đơn.");
            }
        }

        // Đơn giao hàng (COD): trước khi chuyển sang Hoàn thành, bắt buộc phải có bước xác nhận đã nhận được tiền
        // ("Xác nhận tiền COD", HoaDonSuaService.xacNhanTienCod). Kiểm tra trước khi đổi trạng thái để báo lỗi thì đơn giữ nguyên.
        if (moi == HoaDon.HOAN_THANH && !laTaiQuay(hd)) {
            BigDecimal daTra = BigDecimal.ZERO;
            for (LichSuThanhToan tt : lichSuThanhToanRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
                if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN) {
                    daTra = daTra.add(soTien(tt.getSoTien()));
                }
            }
            if (daTra.compareTo(thanhTien(hd)) < 0) {
                throw ApiException.badRequest("Đơn COD: Bắt buộc phải xác nhận đã nhận được tiền trước khi chuyển sang trạng thái Hoàn thành.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        // Đơn online chỉ trừ tồn kho khi nhân viên xác nhận (lúc khách đặt chưa trừ) - tránh tồn kho âm khi nhiều đơn cùng chờ
        boolean chuaTruKho = !laTaiQuay(hd) && hienTai == HoaDon.CHO_XAC_NHAN;
        // Xác nhận mà không đủ hàng: KHÔNG tự hủy đơn (review 05/11 và 14/11: "tại sao xác nhận cái là hủy đơn luôn").
        // truTonKhoKhiXacNhan báo rõ từng món thiếu và giữ nguyên đơn ở Chờ xác nhận, nhân viên tự quyết: nhập thêm hàng rồi xác nhận lại,
        // hoặc bấm Hủy đơn (ghi lý do; khách đã chuyển khoản thì nhận thư hoàn tiền).
        if (chuaTruKho && moi == HoaDon.DA_XAC_NHAN) {
            truTonKhoKhiXacNhan(hd);
        }
        if (moi == HoaDon.DA_HUY && !chuaTruKho) {
            hoanTonKho(hd); // cả khi hủy sau giao hàng thất bại: hàng đã về cửa hàng
        }
        // Gọi trước khi đổi trạng thái: lúc này hóa đơn vẫn còn được đếm là "chưa hủy" (xem hoanLuotPhieu)
        String hoanPhieu = moi == HoaDon.DA_HUY ? hoanLuotPhieu(hd) : "";
        // Gán nhân viên xử lý đơn nếu đơn chưa có nhân viên (ví dụ đơn online do khách tự đặt)
        if (hd.getNhanVien() == null && nhanVienRepository != null) {
            com.footstyle.demo.security.XacThuc.hienTai()
                    .filter(com.footstyle.demo.security.NguoiDung::laNhanVien)
                    .flatMap(nd -> nhanVienRepository.findById(nd.id()))
                    .ifPresent(hd::setNhanVien);
        }
        hd.setTrangThai(moi);
        hd.setNgayCapNhat(now);
        if (moi == HoaDon.DA_GIAO) {
            hd.setNgayGiaoThucTe(now);
        }
        hoaDonRepository.save(hd);

        // Hoàn thành mà khách chưa trả đủ (áp dụng cho đơn tại quầy): ghi nhận phần tiền còn lại
        if (moi == HoaDon.HOAN_THANH && laTaiQuay(hd)) {
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
                tt.setMoTa("Thanh toán đơn tại cửa hàng");
                lichSuThanhToanRepository.save(tt);
            }
        }

        // Hủy đơn khách đã trả trước (chuyển khoản) -> ghi khoản HOÀN PHÍ chờ hoàn trả (trangThai = 0)
        String hoanPhi = "";
        BigDecimal tienHoan = BigDecimal.ZERO;
        if (moi == HoaDon.DA_HUY) {
            BigDecimal daTra = BigDecimal.ZERO;
            for (LichSuThanhToan tt : lichSuThanhToanRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
                if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN) daTra = daTra.add(soTien(tt.getSoTien()));
            }
            if (daTra.signum() > 0) {
                tienHoan = daTra;
                LichSuThanhToan tt = new LichSuThanhToan();
                tt.setHoaDon(hd);
                tt.setSoTien(daTra.negate());
                tt.setThoiGian(now);
                tt.setTrangThai(0); // 0 = Chờ hoàn trả (nhân viên xác nhận sau khi chuyển tiền hoàn lại cho khách)
                tt.setIdPhuongThucThanhToan(hd.getIdPhuongThucThanhToan());
                tt.setMoTa("Hoàn phí cho khách khi hủy đơn");
                lichSuThanhToanRepository.save(tt);
                hoanPhi = " - Hoàn phí cho khách " + GiaoCaService.dinhDangTien(daTra);
            }
        }

        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong(HoaDonLuongTrangThai.ten(moi));
        ls.setThoiGian(now);
        ls.setMoTa((ghiChu.isEmpty() ? HoaDonLuongTrangThai.moTaMacDinh(hienTai, moi) : ghiChu) + hoanPhi + hoanPhieu);
        // Ghi tên người đang đăng nhập (lấy từ phiên), không dùng chữ chung "Quản lý" do màn hình gửi lên
        String nguoi = com.footstyle.demo.security.XacThuc.hienTai()
                .filter(com.footstyle.demo.security.NguoiDung::laNhanVien)
                .map(com.footstyle.demo.security.NguoiDung::hoTen)
                .filter(t -> t != null && !t.isBlank())
                .orElse(req.nguoiThaoTac() == null ? "" : req.nguoiThaoTac().trim());
        ls.setNguoiThaoTac(nguoi.isEmpty() ? "Quản lý" : nguoi);
        lichSuHoaDonRepository.save(ls);

        // Đơn online đổi trạng thái: gửi thư kèm thanh tiến độ (góp ý 22/11: khách xem ngay đơn tới bước nào, không phải bấm theo dõi);
        // hủy thì thư hủy kèm lý do, hủy mà đã thanh toán thì thư hoàn tiền. Gửi ở luồng riêng nên không làm chậm thao tác.
        if (mailService != null && !laTaiQuay(hd)) {
            String email = emailNhanThongBao(hd);
            if (email != null) {
                String ten = hd.getHoTenNguoiNhan() != null ? hd.getHoTenNguoiNhan()
                        : hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : null;
                if (moi == HoaDon.DA_HUY) {
                    if (tienHoan.signum() > 0) {
                        mailService.guiThongBaoHoanTien(ten, email, hd.getMaHoaDon(),
                                ghiChu.isEmpty() ? null : ghiChu,
                                GiaoCaService.dinhDangTien(tienHoan),
                                "Chuyển khoản ngân hàng");
                    } else {
                        mailService.guiThongBaoHuyDon(ten, email, hd.getMaHoaDon(), ghiChu.isEmpty() ? null : ghiChu);
                    }
                } else {
                    mailService.guiCapNhatTrangThai(ten, email, hd.getMaHoaDon(), moi, ghiChu,
                            GiaoCaService.dinhDangTien(thanhTien(hd)), hd.getDiaChiGiaoHang());
                }
            }
        }

        return getChiTietHoaDon(hd.getId());
    }

    /* ===================== Hàm phụ ===================== */

    /**
     * Tên hình thức thanh toán ghi giống nhau ở mọi nơi (chi tiết: Tổng kết + Lịch sử thanh toán, bản in, danh sách / Excel) - góp ý 25/11
     * "chuyển bằng gì thì ghi tên cái đó": đơn online chuyển khoản là "Chuyển khoản VietQR" (khách quét mã VietQR), bán tại quầy
     * (kể cả bán quầy có giao hàng) chuyển khoản là "Chuyển khoản ngân hàng". Đơn đặt online dùng id 1 (bản ghi COD cũ) vẫn là COD.
     * @param banTaiQuay đơn bán ở quầy (đơn tại quầy, hoặc đơn giao hàng tạo ở quầy)
     */
    public static String tenHinhThucThanhToan(Integer id, boolean banTaiQuay) {
        if (id == null) return null;
        return switch (id) {
            case 1 -> banTaiQuay ? "Tiền mặt" : "Thanh toán khi nhận hàng (COD)";
            case 2 -> banTaiQuay ? "Chuyển khoản ngân hàng" : "Chuyển khoản VietQR";
            case 3 -> "Thanh toán khi nhận hàng (COD)";
            case 4 -> "Cổng thanh toán VNPay";
            case 5 -> "Tiền mặt + chuyển khoản ngân hàng";
            case 6 -> "Ví điện tử MoMo";
            default -> null;
        };
    }

    /**
     * Email nhận thông báo của đơn online: email ghi lúc đặt hàng (dòng lịch sử "Đặt hàng trực tuyến"), không có thì email
     * tài khoản khách hàng. Không có email hợp lệ thì null.
     */
    String emailNhanThongBao(HoaDon hd) {
        for (LichSuHoaDon ls : lichSuHoaDonRepository.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
            String e = MailService.emailTrongMoTa(ls.getMoTa());
            if (e != null) return e;
        }
        String e = hd.getKhachHang() == null ? null : hd.getKhachHang().getEmail();
        return e != null && e.contains("@") ? e.trim() : null;
    }

    /**
     * Xác nhận đơn online: kiểm tra đủ hàng cho TẤT CẢ dòng trước (báo hết các dòng thiếu một lần), rồi trừ có điều kiện từng biến thể.
     * Hai nhân viên cùng xác nhận 2 đơn tranh món cuối: lệnh trừ có điều kiện của đơn sau trả 0 -> báo lỗi, cả giao dịch bị hủy.
     */
    private void truTonKhoKhiXacNhan(HoaDon hd) {
        Map<Integer, SanPhamChiTiet> bienThe = new LinkedHashMap<>();
        Map<Integer, Integer> canTru = new LinkedHashMap<>();
        for (HoaDonChiTiet ct : hoaDonChiTietRepository.findByHoaDon_IdOrderByIdAsc(hd.getId())) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            int sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            if (spct == null || spct.getId() == null || sl <= 0) continue;
            bienThe.putIfAbsent(spct.getId(), spct);
            canTru.merge(spct.getId(), sl, Integer::sum);
        }
        List<String> thieu = danhSachThieuHang(hd);
        if (!thieu.isEmpty()) {
            // Màn chi tiết hóa đơn bắt mã THIEU_HANG để mở popup báo hết hàng và chỉ cho hủy đơn
            throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "Không đủ hàng để xác nhận đơn: " + String.join("; ", thieu)
                    + ". Đơn vẫn ở Chờ xác nhận. Vui lòng liên hệ khách hàng và thực hiện Hủy đơn.", "THIEU_HANG", Map.of("thieu", thieu));
        }
        for (Map.Entry<Integer, Integer> e : canTru.entrySet()) {
            if (sanPhamChiTietRepository.truTonKho(e.getKey(), e.getValue()) == 0) {
                SanPhamChiTiet fresh = sanPhamChiTietRepository.findById(e.getKey()).orElse(bienThe.get(e.getKey()));
                throw ApiException.conflict("Sản phẩm " + (fresh != null ? fresh.getMaSpct() : "") + " vừa hết hàng (đơn khác vừa được xác nhận). Hãy tải lại rồi thử lại.");
            }
        }
    }

    /** Các sản phẩm trong đơn không đủ hàng trong kho (rỗng nếu đủ): "Tên (MÃ, kho còn X, đơn cần Y)". */
    public List<String> danhSachThieuHang(HoaDon hd) {
        Map<Integer, Integer> canTru = new LinkedHashMap<>();
        Map<Integer, SanPhamChiTiet> bienThe = new LinkedHashMap<>();
        for (HoaDonChiTiet ct : hoaDonChiTietRepository.findByHoaDon_IdOrderByIdAsc(hd.getId())) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            int sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            if (spct == null || spct.getId() == null || sl <= 0) continue;
            bienThe.putIfAbsent(spct.getId(), spct);
            canTru.merge(spct.getId(), sl, Integer::sum);
        }
        List<String> thieu = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : canTru.entrySet()) {
            SanPhamChiTiet fresh = sanPhamChiTietRepository.findById(e.getKey()).orElse(bienThe.get(e.getKey()));
            int ton = fresh == null || fresh.getSoLuongTon() == null ? 0 : fresh.getSoLuongTon();
            if (ton < e.getValue()) {
                String ten = fresh != null && fresh.getSanPham() != null ? fresh.getSanPham().getTenSanPham() : (fresh != null ? fresh.getMaSpct() : "SP");
                thieu.add(ten + " (" + (fresh != null ? fresh.getMaSpct() : "") + ", kho còn " + ton + ", đơn cần " + e.getValue() + ")");
            }
        }
        return thieu;
    }

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

    /**
     * Hủy hóa đơn có dùng phiếu (lượt đã trừ lúc thanh toán tại quầy / lúc khách đặt online): trả lại một lượt cho phiếu, không để âm.
     * Phiếu cá nhân thì xóa ngày sử dụng để khách dùng lại được, trừ khi khách còn đơn khác chưa hủy đang dùng phiếu này.
     * Đã hủy là trạng thái cuối nên mỗi hóa đơn chỉ hoàn một lần. Phải gọi TRƯỚC khi đổi trạng thái (hóa đơn này còn được đếm là chưa hủy).
     * Trả về đoạn ghi thêm vào lịch sử hóa đơn ("" nếu không hoàn gì).
     */
    /** Trả lại lượt phiếu của đơn (hủy đơn; sửa đơn làm đơn không còn đủ điều kiện phiếu). Gọi khi đơn vẫn còn gắn phiếu. */
    String hoanLuotPhieu(HoaDon hd) {
        PhieuGiamGia phieu = hd.getPhieuGiamGia();
        if (phieu == null || phieu.getId() == null) {
            return "";
        }
        boolean daHoan = phieuGiamGiaRepository.giamLuotDung(phieu.getId()) > 0;
        KhachHang kh = hd.getKhachHang();
        if (kh != null && kh.getId() != null && Integer.valueOf(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE).equals(phieu.getDoiTuongApDung())) {
            long donKhacDangDung = hoaDonRepository.countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot(kh.getId(), phieu.getId(), HoaDon.DA_HUY) - 1;
            if (donKhacDangDung <= 0) {
                for (PhieuGiamGiaKhachHang x : phieuGiamGiaKhachHangRepository.findByIdKhachHang(kh.getId())) {
                    if (phieu.getId().equals(x.getIdPhieuGiamGia()) && x.getNgaySuDung() != null) {
                        x.setNgaySuDung(null);
                        phieuGiamGiaKhachHangRepository.save(x);
                        daHoan = true;
                    }
                }
            }
        }
        return daHoan ? " - Trả lại lượt phiếu " + phieu.getMaPhieu() : "";
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

    /** Hình thức mua suy ra từ dữ liệu sẵn có (không thêm cột): đơn tại quầy / đơn quầy có giao hàng / đơn khách đặt online. */
    static String hinhThucMua(Integer loaiDon, boolean giaoHangTaoTaiQuay) {
        if (!Integer.valueOf(HoaDon.GIAO_HANG).equals(loaiDon)) return "Mua tại quầy";
        return giaoHangTaoTaiQuay ? "Giao tận nơi" : "Đặt online";
    }

    /** Tên kênh bán: đơn giao hàng tạo ở quầy vẫn là "Tại quầy". */
    private static String tenKenh(Integer loaiDon, boolean giaoHangTaoTaiQuay) {
        return giaoHangTaoTaiQuay ? "Tại quầy" : tenLoaiDon(loaiDon);
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
