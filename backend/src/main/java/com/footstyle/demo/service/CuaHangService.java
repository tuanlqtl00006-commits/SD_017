package com.footstyle.demo.service;

import com.footstyle.demo.entity.*;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuaHangService {

    private final SanPhamRepository sanPhamRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final DanhMucRepository danhMucRepo;
    private final ThuongHieuRepository thuongHieuRepo;
    private final XuatXuRepository xuatXuRepo;
    private final ChatLieuRepository chatLieuRepo;
    private final DoCungRepository doCungRepo;
    private final DiemCanBangRepository diemCanBangRepo;
    private final MauSacRepository mauSacRepo;
    private final TrongLuongRepository trongLuongRepo;
    private final ChuViRepository chuViRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;
    private final LichSuHoaDonRepository lichSuHoaDonRepo;
    private final LichSuThanhToanRepository thanhToanRepo;
    private final PhieuGiamGiaRepository phieuRepo;
    private final PhieuGiamGiaKhachHangRepository phieuKhachRepo;
    private final KhachHangRepository khachHangRepo;
    private final GiamGiaApDungService giamGiaService;

    /** Gửi mail xác nhận đặt hàng. Nạp bằng setter (không bắt buộc) để không đổi hàm khởi tạo của lớp. */
    private MailService mailService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public record DatHangOnlineRequest(
            String hoTenNguoiNhan,
            String sdtNguoiNhan,
            String emailNguoiNhan,
            String diaChiGiaoHang,
            String tinhThanh,
            String phuongXa,
            String ghiChu,
            Integer idKhachHang,
            Integer idPhuongThucThanhToan,
            String maPhieuGiamGia,
            List<ItemRequest> chiTiet
    ) {
        public record ItemRequest(Integer idSanPhamChiTiet, Integer soLuong) {}
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private String boDau(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /** Danh sách sản phẩm bán online kèm thông số, giá min/max và % giảm giá */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSachSanPham(Integer idDanhMuc, Integer idThuongHieu, String tuKhoa, Long giaTu, Long giaDen, String sapXep) {
        LocalDateTime now = bayGio();
        Map<Long, Double> giamMap = giamGiaService.phanTramTheoBienThe(now);
        List<SanPham> dsSanPham = sanPhamRepo.findAll();
        List<SanPhamChiTiet> dsChiTiet = spctRepo.findAll();
        List<HinhAnhSanPham> dsAnh = hinhAnhRepo.findByTrangThaiOrderByLaAnhChinhDescIdAsc(HinhAnhSanPham.HOAT_DONG);

        // Map idSanPham -> list anh
        Map<Integer, List<HinhAnhSanPham>> mapAnh = new HashMap<>();
        for (HinhAnhSanPham a : dsAnh) {
            if (a.getSanPham() != null) {
                mapAnh.computeIfAbsent(a.getSanPham().getId(), k -> new ArrayList<>()).add(a);
            }
        }

        // Map idSanPham -> list bien the
        Map<Integer, List<SanPhamChiTiet>> mapBienThe = new HashMap<>();
        for (SanPhamChiTiet bt : dsChiTiet) {
            if (bt.getSanPham() != null && Integer.valueOf(SanPhamChiTiet.HOAT_DONG).equals(bt.getTrangThai())) {
                mapBienThe.computeIfAbsent(bt.getSanPham().getId(), k -> new ArrayList<>()).add(bt);
            }
        }

        String kw = boDau(tuKhoa);
        List<Map<String, Object>> ketQua = new ArrayList<>();

        for (SanPham sp : dsSanPham) {
            if (!Integer.valueOf(1).equals(sp.getTrangThai())) continue;

            if (idDanhMuc != null && (sp.getDanhMuc() == null || !idDanhMuc.equals(sp.getDanhMuc().getId()))) continue;
            if (idThuongHieu != null && (sp.getThuongHieu() == null || !idThuongHieu.equals(sp.getThuongHieu().getId()))) continue;

            if (!kw.isEmpty()) {
                String full = boDau(sp.getTenSanPham() + " " + sp.getMaSanPham() + " "
                        + (sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : "") + " "
                        + (sp.getDanhMuc() != null ? sp.getDanhMuc().getTen() : ""));
                if (!full.contains(kw)) continue;
            }

            List<SanPhamChiTiet> bts = mapBienThe.getOrDefault(sp.getId(), List.of());
            if (bts.isEmpty()) continue;

            long giaGocMin = Long.MAX_VALUE;
            long giaGocMax = 0;
            long giaBanMin = Long.MAX_VALUE;
            long giaBanMax = 0;
            double phanTramMax = 0;
            int tongTon = 0;

            List<Map<String, Object>> btsData = new ArrayList<>();
            for (SanPhamChiTiet bt : bts) {
                long g = bt.getGiaBan() == null ? 0 : bt.getGiaBan();
                double pt = giamMap.getOrDefault(bt.getId().longValue(), 0.0);
                long gSauGiam = pt > 0 ? Math.round(g * (100.0 - pt) / 100.0) : g;
                int ton = bt.getSoLuongTon() == null ? 0 : bt.getSoLuongTon();

                giaGocMin = Math.min(giaGocMin, g);
                giaGocMax = Math.max(giaGocMax, g);
                giaBanMin = Math.min(giaBanMin, gSauGiam);
                giaBanMax = Math.max(giaBanMax, gSauGiam);
                phanTramMax = Math.max(phanTramMax, pt);
                tongTon += ton;

                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("id", bt.getId());
                bm.put("maSpct", bt.getMaSpct());
                bm.put("idMauSac", bt.getMauSac() != null ? bt.getMauSac().getId() : null);
                bm.put("tenMau", bt.getMauSac() != null ? bt.getMauSac().getTen() : "");
                bm.put("trongLuong", bt.getTrongLuong() != null ? bt.getTrongLuong().getTen() : "");
                bm.put("chuVi", bt.getChuVi() != null ? bt.getChuVi().getTen() : "");
                bm.put("giaBanGoc", g);
                bm.put("giaBan", gSauGiam);
                bm.put("phanTramGiam", (int) Math.round(pt));
                bm.put("soLuongTon", ton);
                btsData.add(bm);
            }

            if (giaTu != null && giaBanMax < giaTu) continue;
            if (giaDen != null && giaBanMin > giaDen) continue;

            // Ảnh chính
            List<HinhAnhSanPham> spAnhs = mapAnh.getOrDefault(sp.getId(), List.of());
            String anhChinh = spAnhs.isEmpty() ? "/images/san-pham/sp01_astrox99_den.svg" : spAnhs.get(0).getDuongDanHinhAnh();
            List<String> listAnh = spAnhs.stream().map(HinhAnhSanPham::getDuongDanHinhAnh).toList();

            Map<String, Object> spData = new LinkedHashMap<>();
            spData.put("id", sp.getId());
            spData.put("maSanPham", sp.getMaSanPham());
            spData.put("tenSanPham", sp.getTenSanPham());
            spData.put("moTa", sp.getMoTa());
            spData.put("idDanhMuc", sp.getDanhMuc() != null ? sp.getDanhMuc().getId() : null);
            spData.put("tenDanhMuc", sp.getDanhMuc() != null ? sp.getDanhMuc().getTen() : "");
            spData.put("idThuongHieu", sp.getThuongHieu() != null ? sp.getThuongHieu().getId() : null);
            spData.put("tenThuongHieu", sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : "");
            spData.put("xuatXu", sp.getXuatXu() != null ? sp.getXuatXu().getTen() : "");
            spData.put("chatLieu", sp.getChatLieu() != null ? sp.getChatLieu().getTen() : "");
            spData.put("doCung", sp.getDoCung() != null ? sp.getDoCung().getTen() : "");
            spData.put("diemCanBang", sp.getDiemCanBang() != null ? sp.getDiemCanBang().getTen() : "");
            spData.put("anhChinh", anhChinh);
            spData.put("danhSachAnh", listAnh);
            spData.put("giaGocMin", giaGocMin == Long.MAX_VALUE ? 0 : giaGocMin);
            spData.put("giaGocMax", giaGocMax);
            spData.put("giaBanMin", giaBanMin == Long.MAX_VALUE ? 0 : giaBanMin);
            spData.put("giaBanMax", giaBanMax);
            spData.put("phanTramGiamMax", (int) Math.round(phanTramMax));
            spData.put("tongSoLuongTon", tongTon);
            spData.put("bienThe", btsData);

            ketQua.add(spData);
        }

        // Sắp xếp
        if ("gia-tang".equalsIgnoreCase(sapXep)) {
            ketQua.sort(Comparator.comparingLong(m -> (Long) m.get("giaBanMin")));
        } else if ("gia-giam".equalsIgnoreCase(sapXep)) {
            ketQua.sort((a, b) -> Long.compare((Long) b.get("giaBanMin"), (Long) a.get("giaBanMin")));
        } else if ("giam-nhieu".equalsIgnoreCase(sapXep)) {
            ketQua.sort((a, b) -> Integer.compare((Integer) b.get("phanTramGiamMax"), (Integer) a.get("phanTramGiamMax")));
        } else {
            // Mặc định: id giảm dần (sản phẩm mới nhất)
            ketQua.sort((a, b) -> Integer.compare((Integer) b.get("id"), (Integer) a.get("id")));
        }

        return ketQua;
    }

    /** Chi tiết 1 sản phẩm */
    @Transactional(readOnly = true)
    public Map<String, Object> chiTietSanPham(Integer id) {
        List<Map<String, Object>> ds = danhSachSanPham(null, null, null, null, null, null);
        return ds.stream()
                .filter(m -> id.equals(m.get("id")))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm #" + id, null, null, null));
    }

    /** Menu danh mục & thương hiệu kèm số lượng sản phẩm */
    @Transactional(readOnly = true)
    public Map<String, Object> danhMucVaThuongHieu() {
        List<DanhMuc> dms = danhMucRepo.findAll();
        List<ThuongHieu> ths = thuongHieuRepo.findAll();

        List<Map<String, Object>> dmList = dms.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("ten", d.getTen());
            return m;
        }).toList();

        List<Map<String, Object>> thList = ths.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("ten", t.getTen());
            return m;
        }).toList();

        // Danh sách thuộc tính (công khai) để cửa hàng làm bộ lọc theo từng danh mục, kể cả khi danh mục chưa có sản phẩm
        Map<String, Object> thuocTinh = new LinkedHashMap<>();
        thuocTinh.put("xuatXu", tenThuocTinh(xuatXuRepo.findAll()));
        List<Map<String, Object>> clList = new ArrayList<>();
        for (ChatLieu c : chatLieuRepo.findAll()) {
            if (!dangDung(c)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ten", c.getTen());
            m.put("nhom", c.getNhomApDung() == null ? "" : c.getNhomApDung());
            clList.add(m);
        }
        thuocTinh.put("chatLieu", clList);
        thuocTinh.put("doCung", tenThuocTinh(doCungRepo.findAll()));
        thuocTinh.put("diemCanBang", tenThuocTinh(diemCanBangRepo.findAll()));
        thuocTinh.put("mauSac", tenThuocTinh(mauSacRepo.findAll()));
        thuocTinh.put("trongLuong", tenThuocTinh(trongLuongRepo.findAll()));
        thuocTinh.put("chuVi", tenThuocTinh(chuViRepo.findAll()));

        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("danhMuc", dmList);
        kq.put("thuongHieu", thList);
        kq.put("thuocTinh", thuocTinh);
        return kq;
    }

    private static boolean dangDung(ThuocTinh t) {
        return t.getTen() != null && (t.getTrangThai() == null || t.getTrangThai() != 0)
                && !"Không áp dụng".equalsIgnoreCase(t.getTen().trim());
    }

    private static List<String> tenThuocTinh(List<? extends ThuocTinh> ds) {
        return ds.stream().filter(CuaHangService::dangDung).map(ThuocTinh::getTen).toList();
    }

    /** Sản phẩm đang có khuyến mãi Flash Sale */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> sanPhamKhuyenMai() {
        List<Map<String, Object>> ds = danhSachSanPham(null, null, null, null, null, "giam-nhieu");
        return ds.stream().filter(m -> ((Integer) m.get("phanTramGiamMax")) > 0).limit(8).toList();
    }

    /** Phiếu giảm giá công khai hợp lệ */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> phieuGiamGiaCongKhai(Long tongTien) {
        LocalDateTime now = bayGio();
        List<PhieuGiamGia> ds = phieuRepo.findAll();
        List<Map<String, Object>> kq = new ArrayList<>();
        for (PhieuGiamGia p : ds) {
            if (p.getTrangThai() == null || p.getTrangThai() != 1) continue;
            if (p.getDoiTuongApDung() == null || p.getDoiTuongApDung() != 1) continue; // 1 = Công khai
            if (p.getNgayBatDau() != null && now.isBefore(p.getNgayBatDau())) continue;
            if (p.getNgayKetThuc() != null && now.isAfter(p.getNgayKetThuc())) continue;
            if (p.getSoLuong() != null && p.getSoLuongDaDung() != null && p.getSoLuongDaDung() >= p.getSoLuong()) continue;

            long donToiThieu = p.getGiaTriDonHangToiThieu() == null ? 0L : p.getGiaTriDonHangToiThieu();
            boolean duDieuKien = tongTien == null || tongTien >= donToiThieu;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("maPhieu", p.getMaPhieu());
            m.put("tenPhieu", p.getTenPhieu());
            m.put("hinhThucGiam", p.getLoaiGiam()); // 1 = %, 2 = Tiền mặt
            m.put("giaTriGiam", p.getGiaTriGiam());
            m.put("giamToiDa", p.getGiamToiDa());
            m.put("donToiThieu", donToiThieu);
            m.put("ngayKetThuc", p.getNgayKetThuc());
            m.put("duDieuKien", duDieuKien);
            kq.add(m);
        }
        return kq;
    }

    /** Lý do khách `idKhach` (có thể null) không dùng được phiếu `p` cho đơn `tongTien` đồng; null = dùng được. */
    private String lyDoPhieuKhongDung(PhieuGiamGia p, long tongTien, Integer idKhach) {
        boolean laCuaKhach = false;
        long soLanDaDung = 0;
        if (idKhach != null) {
            for (PhieuGiamGiaKhachHang x : phieuKhachRepo.findByIdKhachHang(idKhach)) {
                if (p.getId().equals(x.getIdPhieuGiamGia()) && x.getNgaySuDung() == null) laCuaKhach = true;
            }
            soLanDaDung = hoaDonRepo.countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot(idKhach, p.getId(), HoaDon.DA_HUY);
        }
        return PhieuApDung.lyDoKhongDung(p, tongTien, bayGio(), idKhach != null, laCuaKhach, soLanDaDung);
    }

    /**
     * Khách nhập mã ở giỏ hàng: kiểm tra mã có dùng được cho đơn `tongTien` không (phiếu công khai, hoặc phiếu cá nhân được tặng
     * cho đúng khách này) và trả về mức giảm. Không dùng được thì báo lý do cụ thể.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> kiemTraPhieuChoKhach(String maPhieu, long tongTien, Integer idKhach) {
        String ma = maPhieu == null ? "" : maPhieu.trim();
        if (ma.isEmpty()) throw ApiException.badRequest("Vui lòng nhập mã phiếu giảm giá.");
        PhieuGiamGia p = phieuRepo.findFirstByMaPhieuIgnoreCase(ma)
                .orElseThrow(() -> ApiException.badRequest("Mã phiếu giảm giá không tồn tại."));
        String lyDo = lyDoPhieuKhongDung(p, tongTien, idKhach);
        if (lyDo != null) throw ApiException.badRequest(lyDo);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ma", p.getMaPhieu());
        m.put("ten", p.getTenPhieu());
        m.put("loaiGiam", p.getLoaiGiam()); // 1 = %, 2 = tiền mặt
        m.put("giaTriGiam", p.getGiaTriGiam());
        m.put("giamToiDa", p.getGiamToiDa());
        m.put("donToiThieu", p.getGiaTriDonHangToiThieu() == null ? 0L : p.getGiaTriDonHangToiThieu());
        m.put("soTienGiam", PhieuApDung.soTienGiam(p, tongTien));
        return m;
    }

    /**
     * Danh sách mã khách đang xem được ở giỏ hàng: phiếu công khai + phiếu cá nhân được tặng đúng cho khách này (chưa dùng).
     * Mã chưa đủ điều kiện cho đơn hiện tại vẫn hiện nhưng kèm lý do (vd "còn thiếu ..."), để khách biết cần mua thêm.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSachPhieuChoKhach(long tongTien, Integer idKhach) {
        Set<Integer> cuaKhach = new HashSet<>();
        if (idKhach != null) {
            for (PhieuGiamGiaKhachHang x : phieuKhachRepo.findByIdKhachHang(idKhach)) {
                if (x.getNgaySuDung() == null) cuaKhach.add(x.getIdPhieuGiamGia());
            }
        }
        List<Map<String, Object>> kq = new ArrayList<>();
        for (PhieuGiamGia p : phieuRepo.findAll()) {
            boolean caNhan = Integer.valueOf(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE).equals(p.getDoiTuongApDung());
            if (caNhan && !cuaKhach.contains(p.getId())) continue; // phiếu tặng riêng cho người khác: không hiện
            // Hiện TẤT CẢ các phiếu còn lại (kể cả hết hạn / hết lượt / ngừng hoạt động): phiếu không dùng được kèm lý do, giao diện làm xám
            String lyDo = lyDoPhieuKhongDung(p, tongTien, idKhach);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma", p.getMaPhieu());
            m.put("ten", p.getTenPhieu());
            m.put("caNhan", caNhan);
            m.put("loaiGiam", p.getLoaiGiam());
            m.put("giaTriGiam", p.getGiaTriGiam());
            m.put("giamToiDa", p.getGiamToiDa());
            m.put("donToiThieu", p.getGiaTriDonHangToiThieu() == null ? 0L : p.getGiaTriDonHangToiThieu());
            m.put("ngayKetThuc", p.getNgayKetThuc());
            m.put("dungDuoc", lyDo == null);
            m.put("lyDo", lyDo);
            m.put("soTienGiam", lyDo == null ? PhieuApDung.soTienGiam(p, tongTien) : 0L);
            kq.add(m);
        }
        return kq;
    }

    /** Khách hàng đặt mua online */
    @Transactional
    public Map<String, Object> datHangOnline(DatHangOnlineRequest req) {
        if (req == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Yêu cầu đặt hàng không hợp lệ.", null, null, null);
        if (req.hoTenNguoiNhan() == null || req.hoTenNguoiNhan().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng nhập họ và tên người nhận hàng.", null, null, Map.of("hoTenNguoiNhan", "Họ tên không được để trống"));
        }
        String sdt = req.sdtNguoiNhan() == null ? "" : req.sdtNguoiNhan().trim();
        if (!sdt.matches("^0\\d{9}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ (cần 10 chữ số, bắt đầu bằng 0).", null, null, Map.of("sdtNguoiNhan", "Số điện thoại 10 chữ số"));
        }
        if (req.diaChiGiaoHang() == null || req.diaChiGiaoHang().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng nhập địa chỉ giao hàng.", null, null, Map.of("diaChiGiaoHang", "Địa chỉ giao hàng không được để trống"));
        }
        if (req.chiTiet() == null || req.chiTiet().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Giỏ hàng rỗng. Vui lòng chọn ít nhất 1 sản phẩm.", null, null, null);
        }

        LocalDateTime now = bayGio();
        Map<Long, Double> giamMap = giamGiaService.phanTramTheoBienThe(now);

        long tongTien = 0;
        List<HoaDonChiTiet> dsChiTiet = new ArrayList<>();

        for (DatHangOnlineRequest.ItemRequest item : req.chiTiet()) {
            if (item.idSanPhamChiTiet() == null || item.soLuong() == null || item.soLuong() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Sản phẩm hoặc số lượng trong giỏ không hợp lệ.", null, null, null);
            }
            SanPhamChiTiet spct = spctRepo.findById(item.idSanPhamChiTiet())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Sản phẩm không tồn tại.", null, null, null));

            int slTon = spct.getSoLuongTon() == null ? 0 : spct.getSoLuongTon();
            if (slTon < item.soLuong()) {
                String ten = (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "") + " (" + (spct.getMauSac() != null ? spct.getMauSac().getTen() : "") + ")";
                throw new ApiException(HttpStatus.BAD_REQUEST, "Sản phẩm \"" + ten + "\" chỉ còn " + slTon + " sản phẩm, không đủ đáp ứng.", null, null, null);
            }

            // Theo yêu cầu của cô: CHƯA trừ tồn kho lúc khách đặt; trừ khi nhân viên chuyển đơn Chờ xác nhận -> Đã xác nhận (HoaDonService)

            long giaGoc = spct.getGiaBan() == null ? 0 : spct.getGiaBan();
            double pt = giamMap.getOrDefault(spct.getId().longValue(), 0.0);
            long giaBan = pt > 0 ? Math.round(giaGoc * (100.0 - pt) / 100.0) : giaGoc;
            long thanhTienMon = giaBan * item.soLuong();
            tongTien += thanhTienMon;

            HoaDonChiTiet ct = new HoaDonChiTiet();
            ct.setSanPhamChiTiet(spct);
            ct.setSoLuong(item.soLuong());
            ct.setDonGia(BigDecimal.valueOf(giaBan));
            ct.setThanhTien(BigDecimal.valueOf(thanhTienMon));
            ct.setTrangThai(1);
            dsChiTiet.add(ct);
        }

        // Tính giảm giá phiếu voucher
        PhieuGiamGia phieu = null;
        long soTienGiam = 0;
        if (req.maPhieuGiamGia() != null && !req.maPhieuGiamGia().trim().isEmpty()) {
            // Mã không dùng được (hết hạn / sai đối tượng / chưa đủ đơn tối thiểu...) thì báo lỗi rõ ràng, không bỏ qua âm thầm
            PhieuGiamGia p = phieuRepo.findFirstByMaPhieuIgnoreCase(req.maPhieuGiamGia().trim())
                    .orElseThrow(() -> ApiException.badRequest("Mã phiếu giảm giá không tồn tại."));
            String lyDoPhieu = lyDoPhieuKhongDung(p, tongTien, req.idKhachHang());
            if (lyDoPhieu != null) throw ApiException.badRequest("Không áp dụng được mã " + p.getMaPhieu() + ": " + lyDoPhieu);
            phieu = p;
            soTienGiam = PhieuApDung.soTienGiam(p, tongTien);
            phieuRepo.tangLuotDung(p.getId());
            // Phiếu cá nhân: đánh dấu khách đã dùng để không dùng lại
            if (req.idKhachHang() != null && Integer.valueOf(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE).equals(p.getDoiTuongApDung())) {
                for (PhieuGiamGiaKhachHang x : phieuKhachRepo.findByIdKhachHang(req.idKhachHang())) {
                    if (p.getId().equals(x.getIdPhieuGiamGia()) && x.getNgaySuDung() == null) {
                        x.setNgaySuDung(now);
                        phieuKhachRepo.save(x);
                        break;
                    }
                }
            }
        }

        // Phí vận chuyển: >= 1.000.000đ freeship, ngược lại 30.000đ
        long phiVanChuyen = tongTien >= 1_000_000L ? 0L : 30_000L;
        long thanhTien = Math.max(0L, tongTien - soTienGiam + phiVanChuyen);

        // Tạo hóa đơn
        String maHd = MaUtil.tiepTheo("HD", hoaDonRepo.findAllMaHoaDon());
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(maHd);
        hd.setLoaiDon(HoaDon.GIAO_HANG); // 2 = Trực tuyến / Giao hàng
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN); // 1 = Chờ xác nhận
        hd.setTongTien(BigDecimal.valueOf(tongTien));
        hd.setSoTienGiam(BigDecimal.valueOf(soTienGiam));
        hd.setPhiVanChuyen(BigDecimal.valueOf(phiVanChuyen));
        hd.setThanhTien(BigDecimal.valueOf(thanhTien));
        hd.setDonViVanChuyen("Giao Hàng Nhanh");
        hd.setHoTenNguoiNhan(req.hoTenNguoiNhan().trim());
        hd.setSdtNguoiNhan(sdt);
        String diaChiFull = req.diaChiGiaoHang().trim()
                + (req.phuongXa() != null && !req.phuongXa().isBlank() ? ", " + req.phuongXa().trim() : "")
                + (req.tinhThanh() != null && !req.tinhThanh().isBlank() ? ", " + req.tinhThanh().trim() : "");
        hd.setDiaChiGiaoHang(diaChiFull);
        hd.setGhiChu(req.ghiChu());
        hd.setPhieuGiamGia(phieu);
        hd.setIdPhuongThucThanhToan(req.idPhuongThucThanhToan() == null ? 1 : req.idPhuongThucThanhToan());
        hd.setNgayTao(now);
        hd.setNgayCapNhat(now);

        // Khách hàng
        if (req.idKhachHang() != null) {
            khachHangRepo.findById(req.idKhachHang()).ifPresent(hd::setKhachHang);
        } else {
            // Tìm theo SĐT xem đã có trong hệ thống chưa
            khachHangRepo.findFirstBySdt(sdt).ifPresent(hd::setKhachHang);
        }

        hd = hoaDonRepo.save(hd);

        // Lưu chi tiết
        for (HoaDonChiTiet ct : dsChiTiet) {
            ct.setHoaDon(hd);
            hoaDonChiTietRepo.save(ct);
        }

        // Lịch sử hóa đơn
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong("Đặt hàng trực tuyến");
        ls.setNguoiThaoTac(req.hoTenNguoiNhan().trim());
        ls.setThoiGian(now);
        ls.setMoTa("Khách đặt online đơn " + maHd + " - Giao tới: " + diaChiFull + " (SĐT: " + sdt + ")");
        lichSuHoaDonRepo.save(ls);

        // Nếu chuyển khoản, chuẩn bị mã VietQR
        String vietQrUrl = null;
        if (Integer.valueOf(2).equals(req.idPhuongThucThanhToan())) {
            vietQrUrl = "https://img.vietqr.io/image/MB-0348888899-compact2.png?amount="
                    + thanhTien + "&addInfo=" + maHd + "%20FOOTSTYLE&accountName=SHOP%20CAU%20LONG%20FOOTSTYLE";
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("idHoaDon", hd.getId());
        res.put("maHoaDon", hd.getMaHoaDon());
        res.put("tongTien", tongTien);
        res.put("soTienGiam", soTienGiam);
        res.put("phiVanChuyen", phiVanChuyen);
        res.put("thanhTien", thanhTien);
        res.put("hoTenNguoiNhan", hd.getHoTenNguoiNhan());
        res.put("sdtNguoiNhan", hd.getSdtNguoiNhan());
        res.put("diaChiGiaoHang", hd.getDiaChiGiaoHang());
        res.put("idPhuongThucThanhToan", hd.getIdPhuongThucThanhToan());
        res.put("tenPhuongThuc", Integer.valueOf(2).equals(hd.getIdPhuongThucThanhToan()) ? "Chuyển khoản VietQR" : "Thanh toán khi nhận hàng (COD)");
        res.put("vietQrUrl", vietQrUrl);
        res.put("soLuongMon", dsChiTiet.size());
        res.put("ngayTao", now.toString());
        res.put("trangThai", "Chờ xác nhận");

        // Gửi mail xác nhận đặt hàng: ưu tiên email khách nhập lúc đặt, không có thì dùng email tài khoản khách hàng
        if (mailService != null) {
            String email = req.emailNguoiNhan() == null ? "" : req.emailNguoiNhan().trim();
            if (email.isEmpty() && hd.getKhachHang() != null && hd.getKhachHang().getEmail() != null) {
                email = hd.getKhachHang().getEmail();
            }
            List<String> mon = new ArrayList<>();
            for (HoaDonChiTiet ct : dsChiTiet) {
                SanPhamChiTiet bt = ct.getSanPhamChiTiet();
                String tenSp = bt != null && bt.getSanPham() != null ? bt.getSanPham().getTenSanPham() : "Sản phẩm";
                String mau = bt != null && bt.getMauSac() != null ? " (" + bt.getMauSac().getTen() + ")" : "";
                mon.add(tenSp + mau + " x" + ct.getSoLuong());
            }
            mailService.guiXacNhanDatHang(hd.getHoTenNguoiNhan(), email, maHd, mon,
                    String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", thanhTien),
                    diaChiFull, (String) res.get("tenPhuongThuc"));
        }

        return res;
    }

    /**
     * Tra cứu đơn của CHÍNH khách đang đăng nhập: đơn không thuộc khách này thì báo "không tìm thấy"
     * (không cho biết đơn người khác có tồn tại hay không).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> traCuuDonHangCuaKhach(String maHoaDon, Integer idKhach) {
        String ma = maHoaDon == null ? "" : maHoaDon.trim();
        HoaDon hd = ma.isEmpty() ? null : hoaDonRepo.findFirstByMaHoaDonIgnoreCase(ma).orElse(null);
        if (hd == null || hd.getKhachHang() == null || !idKhach.equals(hd.getKhachHang().getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng " + ma + " trong tài khoản của bạn.", null, null, null);
        }
        return traCuuDonHang(ma, null);
    }

    /** Tra cứu đơn hàng online */
    @Transactional(readOnly = true)
    public Map<String, Object> traCuuDonHang(String maHoaDon, String sdt) {
        if (maHoaDon == null || maHoaDon.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng nhập mã đơn hàng để tra cứu.", null, null, null);
        }
        HoaDon hd = hoaDonRepo.findFirstByMaHoaDonIgnoreCase(maHoaDon.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng " + maHoaDon, null, null, null));

        if (sdt != null && !sdt.trim().isEmpty()) {
            if (hd.getSdtNguoiNhan() != null && !hd.getSdtNguoiNhan().trim().equals(sdt.trim())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Số điện thoại không khớp với đơn hàng " + maHoaDon, null, null, null);
            }
        }

        List<HoaDonChiTiet> cts = hoaDonChiTietRepo.findByHoaDon_IdOrderByIdAsc(hd.getId());
        List<Map<String, Object>> monHangs = new ArrayList<>();
        for (HoaDonChiTiet c : cts) {
            SanPhamChiTiet bt = c.getSanPhamChiTiet();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idSanPham", bt != null && bt.getSanPham() != null ? bt.getSanPham().getId() : null);
            m.put("tenSanPham", bt != null && bt.getSanPham() != null ? bt.getSanPham().getTenSanPham() : "");
            m.put("mauSac", bt != null && bt.getMauSac() != null ? bt.getMauSac().getTen() : "");
            m.put("trongLuong", bt != null && bt.getTrongLuong() != null ? bt.getTrongLuong().getTen() : "");
            m.put("chuVi", bt != null && bt.getChuVi() != null ? bt.getChuVi().getTen() : "");
            m.put("soLuong", c.getSoLuong());
            m.put("donGia", c.getDonGia());
            m.put("thanhTien", c.getThanhTien());
            monHangs.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", hd.getId());
        res.put("maHoaDon", hd.getMaHoaDon());
        res.put("trangThai", hd.getTrangThai());
        res.put("tenTrangThai", tenTrangThai(hd.getTrangThai()));
        res.put("tongTien", hd.getTongTien());
        res.put("soTienGiam", hd.getSoTienGiam());
        res.put("phiVanChuyen", hd.getPhiVanChuyen());
        res.put("thanhTien", hd.getThanhTien());
        res.put("hoTenNguoiNhan", hd.getHoTenNguoiNhan());
        res.put("sdtNguoiNhan", hd.getSdtNguoiNhan());
        res.put("diaChiGiaoHang", hd.getDiaChiGiaoHang());
        res.put("donViVanChuyen", hd.getDonViVanChuyen());
        res.put("ngayTao", hd.getNgayTao());
        res.put("chiTiet", monHangs);
        BigDecimal daTra = BigDecimal.ZERO;
        for (var tt : thanhToanRepo.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
            if (tt.getTrangThai() != null && tt.getTrangThai() == com.footstyle.demo.entity.LichSuThanhToan.DA_THANH_TOAN && tt.getSoTien() != null) {
                daTra = daTra.add(tt.getSoTien());
            }
        }
        res.put("daTra", daTra);
        res.put("daThanhToanDu", hd.getThanhTien() != null && daTra.compareTo(hd.getThanhTien()) >= 0);
        res.put("idPhuongThucThanhToan", hd.getIdPhuongThucThanhToan());

        return res;
    }

    private String tenTrangThai(Integer code) {
        if (code == null) return "Chờ xác nhận";
        return switch (code) {
            case 0 -> "Đã hủy";
            case 1 -> "Chờ xác nhận";
            case 2 -> "Đã xác nhận";
            case 3 -> "Chờ lấy hàng";
            case 4 -> "Đang giao hàng";
            case 5 -> "Đã giao hàng";
            case 6 -> "Hoàn thành";
            case 8 -> "Giao thất bại";
            default -> "Đang xử lý";
        };
    }
}
