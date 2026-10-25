package com.footstyle.demo.service;

import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hóa đơn chờ tại quầy LƯU Ở CSDL (cô: thêm sản phẩm vào hóa đơn chờ là phải trừ tồn kho và tạo hóa đơn chi tiết ngay).
 * Hóa đơn chờ = hoa_don loại 1 (tại quầy), trạng thái 1 (chưa thanh toán), gắn nhân viên + ca đang mở.
 *  - Thêm sản phẩm: trừ tồn kho có điều kiện (2 máy cùng bán món cuối thì máy sau báo hết hàng), ghi hoa_don_chi_tiet theo giá lúc thêm.
 *    Cùng biến thể mà giá đã khác (đợt giảm giá vừa bắt đầu / kết thúc, sửa giá bán) thì tách thành DÒNG MỚI, không gộp.
 *  - Đổi số lượng: tăng thì trừ thêm kho, giảm thì trả lại kho. Xóa dòng / hủy hóa đơn chờ: trả lại toàn bộ.
 *  - Sản phẩm hết hàng tự biến mất khỏi danh sách chọn (tồn kho trong CSDL đã giảm).
 *  - Thanh toán: BanHangService.thanhToanHoaDonCho (tính lại giá, phiếu, tiền khách trả).
 */
@Service
@RequiredArgsConstructor
public class HoaDonChoService {

    /** Mỗi nhân viên giữ tối đa bấy nhiêu hóa đơn chờ. */
    public static final int SO_HOA_DON_CHO_TOI_DA = 10;

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final KhachHangRepository khachHangRepo;
    private final NhanVienRepository nhanVienRepo;
    private final LichSuHoaDonRepository lichSuRepo;
    private final GiaoCaService giaoCaService;
    private final GiamGiaApDungService giamGiaService;
    private final BanHangService banHangService;

    private Clock dongHo = Clock.systemDefaultZone();

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
    }

    public record DoiKhachRequest(Integer idKhachHang) {
    }

    public record ThemSanPhamRequest(Integer idBienThe, Integer soLuong) {
    }

    public record SoLuongRequest(Integer soLuong) {
    }

    /** Hóa đơn chờ tại quầy (chưa thanh toán)? */
    public static boolean laHoaDonCho(HoaDon h) {
        return h != null && Integer.valueOf(HoaDon.TAI_QUAY).equals(h.getLoaiDon()) && Integer.valueOf(HoaDon.CHO_XAC_NHAN).equals(h.getTrangThai());
    }

    /* ===================== Danh sách / tạo ===================== */

    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSach() {
        NguoiDung nd = XacThuc.nhanVien();
        GiaKhiXem gia = giaHienTai();
        List<Map<String, Object>> kq = new ArrayList<>();
        for (HoaDon h : hoaDonRepo.findByNhanVien_IdAndLoaiDonAndTrangThaiOrderByNgayTaoAscIdAsc(nd.id(), HoaDon.TAI_QUAY, HoaDon.CHO_XAC_NHAN)) {
            kq.add(dto(h, gia));
        }
        return kq;
    }

    @Transactional
    public Map<String, Object> tao() {
        NguoiDung nd = XacThuc.nhanVien();
        GiaoCa ca = giaoCaService.caDangMoBatBuoc(nd.id());
        int dangCo = hoaDonRepo.findByNhanVien_IdAndLoaiDonAndTrangThaiOrderByNgayTaoAscIdAsc(nd.id(), HoaDon.TAI_QUAY, HoaDon.CHO_XAC_NHAN).size();
        if (dangCo >= SO_HOA_DON_CHO_TOI_DA) {
            throw ApiException.conflict("Tối đa " + SO_HOA_DON_CHO_TOI_DA + " hóa đơn chờ. Hãy thanh toán hoặc hủy bớt.");
        }
        NhanVien nv = nhanVienRepo.findById(nd.id()).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên."));
        LocalDateTime luc = bayGio();
        HoaDon h = new HoaDon();
        h.setMaHoaDon(MaUtil.tiepTheo("HD", hoaDonRepo.findAllMaHoaDon()));
        h.setLoaiDon(HoaDon.TAI_QUAY);
        h.setTrangThai(HoaDon.CHO_XAC_NHAN);
        h.setNhanVien(nv);
        h.setTongTien(BigDecimal.ZERO);
        h.setSoTienGiam(BigDecimal.ZERO);
        h.setPhiVanChuyen(BigDecimal.ZERO);
        h.setThanhTien(BigDecimal.ZERO);
        h.setNgayTao(luc);
        h.setNgayCapNhat(luc);
        h.setIdGiaoCa(ca.getId());
        try {
            h = hoaDonRepo.saveAndFlush(h);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Mã hóa đơn vừa bị trùng với máy khác. Vui lòng bấm lại.", "TRUNG_MA", null);
        }
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(h);
        ls.setHanhDong("Tạo đơn hàng");
        ls.setMoTa("Tạo hóa đơn chờ tại quầy");
        ls.setNguoiThaoTac(nv.getHoTen());
        ls.setThoiGian(luc);
        lichSuRepo.save(ls);
        return dto(h, giaHienTai());
    }

    /* ===================== Khách hàng ===================== */

    @Transactional
    public Map<String, Object> doiKhach(Integer id, DoiKhachRequest req) {
        HoaDon h = cuaToi(id);
        KhachHang kh = null;
        if (req != null && req.idKhachHang() != null) {
            kh = khachHangRepo.findById(req.idKhachHang()).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng."));
            if (!Integer.valueOf(1).equals(kh.getTrangThai())) throw ApiException.conflict("Khách hàng đang ngừng hoạt động.");
        }
        h.setKhachHang(kh);
        h.setNgayCapNhat(bayGio());
        return dto(hoaDonRepo.save(h), giaHienTai());
    }

    /* ===================== Sản phẩm ===================== */

    @Transactional
    public Map<String, Object> themSanPham(Integer id, ThemSanPhamRequest req) {
        HoaDon h = cuaToi(id);
        if (req == null || req.idBienThe() == null) throw ApiException.badRequest("Chưa chọn sản phẩm.");
        int sl = req.soLuong() == null ? 1 : req.soLuong();
        if (sl < 1 || sl > BanHangService.SO_LUONG_TOI_DA) throw ApiException.badRequest("Số lượng từ 1 đến " + BanHangService.SO_LUONG_TOI_DA + ".");
        SanPhamChiTiet c = spctRepo.findById(req.idBienThe()).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm."));
        if (!BanHangService.dangBan(c)) throw ApiException.conflict("Sản phẩm " + c.getMaSpct() + " đang ngừng bán.");
        GiaKhiXem gia = giaHienTai();
        long donGia = gia.cua(c);

        List<HoaDonChiTiet> dong = chiTietRepo.findByHoaDon_IdOrderByIdAsc(h.getId());
        HoaDonChiTiet cungGia = null;
        boolean coDongKhac = false;
        for (HoaDonChiTiet ct : dong) {
            if (!ct.getSanPhamChiTiet().getId().equals(c.getId())) continue;
            if (long0(ct.getDonGia()) == donGia) cungGia = ct;
            else coDongKhac = true;
        }
        if (cungGia == null && dong.size() >= BanHangService.SO_DONG_TOI_DA) {
            throw ApiException.badRequest("Một hóa đơn tối đa " + BanHangService.SO_DONG_TOI_DA + " dòng sản phẩm.");
        }
        int tongSauKhiThem = sl + (cungGia == null ? 0 : cungGia.getSoLuong());
        if (tongSauKhiThem > BanHangService.SO_LUONG_TOI_DA) throw ApiException.badRequest("Mỗi dòng tối đa " + BanHangService.SO_LUONG_TOI_DA + " sản phẩm.");
        truKho(c, sl);
        if (cungGia != null) {
            cungGia.setSoLuong(tongSauKhiThem);
            cungGia.setThanhTien(BigDecimal.valueOf(donGia * tongSauKhiThem));
            chiTietRepo.save(cungGia);
        } else {
            HoaDonChiTiet ct = new HoaDonChiTiet();
            ct.setHoaDon(h);
            ct.setSanPhamChiTiet(c);
            ct.setSoLuong(sl);
            ct.setDonGia(BigDecimal.valueOf(donGia));
            ct.setThanhTien(BigDecimal.valueOf(donGia * sl));
            ct.setTrangThai(1);
            chiTietRepo.save(ct);
        }
        Map<String, Object> kq = capNhatTong(h, gia);
        if (coDongKhac) {
            kq.put("thongBao", "Giá " + c.getMaSpct() + " đã đổi thành " + GiaoCaService.dinhDangTien(BigDecimal.valueOf(donGia))
                    + " nên được tách thành dòng mới (dòng cũ giữ giá lúc thêm).");
        }
        return kq;
    }

    @Transactional
    public Map<String, Object> doiSoLuong(Integer id, Integer idChiTiet, SoLuongRequest req) {
        HoaDon h = cuaToi(id);
        HoaDonChiTiet ct = dongCua(h, idChiTiet);
        int moi = req == null || req.soLuong() == null ? 0 : req.soLuong();
        if (moi < 1 || moi > BanHangService.SO_LUONG_TOI_DA) throw ApiException.badRequest("Số lượng từ 1 đến " + BanHangService.SO_LUONG_TOI_DA + ".");
        int cu = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
        SanPhamChiTiet c = ct.getSanPhamChiTiet();
        if (moi > cu) truKho(c, moi - cu);
        else if (moi < cu) spctRepo.congTonKho(c.getId(), cu - moi);
        ct.setSoLuong(moi);
        ct.setThanhTien(BigDecimal.valueOf(long0(ct.getDonGia()) * moi));
        chiTietRepo.save(ct);
        return capNhatTong(h, giaHienTai());
    }

    @Transactional
    public Map<String, Object> xoaDong(Integer id, Integer idChiTiet) {
        HoaDon h = cuaToi(id);
        HoaDonChiTiet ct = dongCua(h, idChiTiet);
        int sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
        if (sl > 0) spctRepo.congTonKho(ct.getSanPhamChiTiet().getId(), sl);
        chiTietRepo.delete(ct);
        return capNhatTong(h, giaHienTai());
    }

    /** Đưa các dòng có giá đã đổi về giá hiện tại (nhân viên đã báo khách); dòng trùng biến thể + trùng giá thì gộp lại. */
    @Transactional
    public Map<String, Object> capNhatGia(Integer id) {
        HoaDon h = cuaToi(id);
        GiaKhiXem gia = giaHienTai();
        Map<String, HoaDonChiTiet> theoBienTheGia = new LinkedHashMap<>();
        for (HoaDonChiTiet ct : chiTietRepo.findByHoaDon_IdOrderByIdAsc(h.getId())) {
            long moi = gia.cua(ct.getSanPhamChiTiet());
            String khoa = ct.getSanPhamChiTiet().getId() + "@" + moi;
            HoaDonChiTiet gop = theoBienTheGia.get(khoa);
            if (gop != null) {
                gop.setSoLuong(gop.getSoLuong() + ct.getSoLuong());
                gop.setThanhTien(BigDecimal.valueOf(moi * gop.getSoLuong()));
                chiTietRepo.save(gop);
                chiTietRepo.delete(ct);
                continue;
            }
            ct.setDonGia(BigDecimal.valueOf(moi));
            ct.setThanhTien(BigDecimal.valueOf(moi * ct.getSoLuong()));
            chiTietRepo.save(ct);
            theoBienTheGia.put(khoa, ct);
        }
        return capNhatTong(h, gia);
    }

    /** Hủy hóa đơn chờ: trả hàng về kho. Chưa có sản phẩm thì xóa hẳn (không để lại hóa đơn rỗng trong danh sách). */
    @Transactional
    public void huy(Integer id) {
        HoaDon h = cuaToi(id);
        List<HoaDonChiTiet> dong = chiTietRepo.findByHoaDon_IdOrderByIdAsc(h.getId());
        for (HoaDonChiTiet ct : dong) {
            int sl = ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            if (sl > 0) spctRepo.congTonKho(ct.getSanPhamChiTiet().getId(), sl);
        }
        NguoiDung nd = XacThuc.nhanVien();
        LocalDateTime luc = bayGio();
        if (dong.isEmpty()) {
            lichSuRepo.deleteAll(lichSuRepo.findByHoaDon_IdOrderByThoiGianAscIdAsc(h.getId()));
            hoaDonRepo.delete(h);
            return;
        }
        h.setTrangThai(HoaDon.DA_HUY);
        h.setNgayCapNhat(luc);
        hoaDonRepo.save(h);
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(h);
        ls.setHanhDong("Đã hủy");
        ls.setMoTa("Hủy hóa đơn chờ tại quầy, đã trả " + dong.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum() + " sản phẩm về kho");
        ls.setNguoiThaoTac(nd.hoTen());
        ls.setThoiGian(luc);
        lichSuRepo.save(ls);
    }

    /* ===================== Hàm phụ ===================== */

    /** Hóa đơn chờ của chính người đang đăng nhập (người khác / đã thanh toán / đã hủy thì báo lỗi). */
    HoaDon cuaToi(Integer id) {
        NguoiDung nd = XacThuc.nhanVien();
        HoaDon h = id == null ? null : hoaDonRepo.findById(id).orElse(null);
        if (h == null) throw ApiException.notFound("Không tìm thấy hóa đơn chờ. Có thể đã bị hủy ở cửa sổ khác.");
        if (h.getNhanVien() == null || !h.getNhanVien().getId().equals(nd.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Đây là hóa đơn chờ của nhân viên khác.");
        }
        if (!laHoaDonCho(h)) {
            throw new ApiException(HttpStatus.CONFLICT, "Hóa đơn " + h.getMaHoaDon() + " đã thanh toán hoặc đã hủy. Hãy tải lại trang.",
                    "HOA_DON_CHO_DA_DONG", null);
        }
        return h;
    }

    private HoaDonChiTiet dongCua(HoaDon h, Integer idChiTiet) {
        return chiTietRepo.findByHoaDon_IdOrderByIdAsc(h.getId()).stream()
                .filter(ct -> ct.getId().equals(idChiTiet)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy dòng sản phẩm trong hóa đơn. Hãy tải lại trang."));
    }

    private void truKho(SanPhamChiTiet c, int sl) {
        if (spctRepo.truTonKho(c.getId(), sl) == 0) {
            int con = spctRepo.findById(c.getId()).map(x -> x.getSoLuongTon() == null ? 0 : x.getSoLuongTon()).orElse(0);
            throw new ApiException(HttpStatus.CONFLICT, con <= 0 ? "Sản phẩm " + c.getMaSpct() + " đã hết hàng."
                    : "Sản phẩm " + c.getMaSpct() + " chỉ còn " + con + " trong kho.", "HET_HANG", Map.of("idBienThe", c.getId(), "tonKho", con));
        }
    }

    /** Tính lại tổng tiền hàng của hóa đơn chờ và trả về DTO mới nhất. */
    private Map<String, Object> capNhatTong(HoaDon h, GiaKhiXem gia) {
        long tong = 0;
        for (HoaDonChiTiet ct : chiTietRepo.findByHoaDon_IdOrderByIdAsc(h.getId())) tong += long0(ct.getThanhTien());
        h.setTongTien(BigDecimal.valueOf(tong));
        h.setThanhTien(BigDecimal.valueOf(tong));
        h.setNgayCapNhat(bayGio());
        return dto(hoaDonRepo.save(h), gia);
    }

    /** Giá theo đợt giảm giá + ảnh tại thời điểm xem (dùng chung cho cả danh sách). */
    record GiaKhiXem(Map<Long, Double> phanTram, Map<Integer, List<HinhAnhSanPham>> anh) {
        long cua(SanPhamChiTiet c) {
            return PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), phanTram.getOrDefault(c.getId().longValue(), 0.0));
        }
    }

    GiaKhiXem giaHienTai() {
        return new GiaKhiXem(giamGiaService.phanTramTheoBienThe(bayGio()), banHangService.anhTheoSanPham());
    }

    Map<String, Object> dto(HoaDon h, GiaKhiXem gia) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("maHoaDon", h.getMaHoaDon());
        m.put("ngayTao", h.getNgayTao());
        m.put("khach", h.getKhachHang() == null ? null : BanHangService.khachDto(h.getKhachHang()));
        List<Map<String, Object>> items = new ArrayList<>();
        long tong = 0;
        int soLuong = 0;
        boolean coGiaDoi = false;
        for (HoaDonChiTiet ct : chiTietRepo.findByHoaDon_IdOrderByIdAsc(h.getId())) {
            SanPhamChiTiet c = ct.getSanPhamChiTiet();
            Map<String, Object> d = new LinkedHashMap<>(banHangService.bienTheDto(c, gia.phanTram(), gia.anh()));
            long donGia = long0(ct.getDonGia());
            long giaMoi = gia.cua(c);
            d.put("idBienThe", c.getId());
            d.put("id", ct.getId());
            d.put("ten", c.getSanPham().getTenSanPham());
            d.put("giaHienTai", giaMoi);
            d.put("donGia", donGia);
            d.put("giaDoi", giaMoi != donGia);
            // Tồn kho đọc lại từ CSDL: lệnh trừ kho có điều kiện chạy bằng câu UPDATE nên bản ghi đang giữ có thể là số cũ
            d.put("tonKho", spctRepo.findById(c.getId()).map(x -> x.getSoLuongTon() == null ? 0 : x.getSoLuongTon()).orElse(0));
            d.put("soLuong", ct.getSoLuong());
            d.put("thanhTien", long0(ct.getThanhTien()));
            items.add(d);
            tong += long0(ct.getThanhTien());
            soLuong += ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            coGiaDoi |= giaMoi != donGia;
        }
        m.put("items", items);
        m.put("tongTienHang", tong);
        m.put("soLuong", soLuong);
        m.put("coGiaDoi", coGiaDoi);
        return m;
    }

    static long long0(BigDecimal v) {
        return v == null ? 0 : v.longValue();
    }
}
