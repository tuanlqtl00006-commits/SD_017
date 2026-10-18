package com.footstyle.demo.service;

import com.footstyle.demo.dto.AnhTheoMauRequest;
import com.footstyle.demo.dto.BienTheMoiRequest;
import com.footstyle.demo.dto.SanPhamRequest;
import com.footstyle.demo.dto.SanPhamResponse;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ChatLieuRepository;
import com.footstyle.demo.repository.DanhMucRepository;
import com.footstyle.demo.repository.DiemCanBangRepository;
import com.footstyle.demo.repository.DoCungRepository;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.MauSacRepository;
import com.footstyle.demo.repository.SanPhamRepository;
import com.footstyle.demo.repository.ThuongHieuRepository;
import com.footstyle.demo.repository.XuatXuRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý sản phẩm (bảng san_pham + hinh_anh_san_pham).
 * Luồng: Controller -> Service kiểm tra dữ liệu (napDuLieu) -> Repository lưu DB -> đổi entity thành Response.
 */
@Service
@RequiredArgsConstructor
public class SanPhamService {

    private static final int TOI_DA_ANH = 10;
    private static final int TOI_DA_ANH_MOI_MAU = 6;

    private final SanPhamRepository sanPhamRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final DanhMucRepository danhMucRepo;
    private final ThuongHieuRepository thuongHieuRepo;
    private final XuatXuRepository xuatXuRepo;
    private final ChatLieuRepository chatLieuRepo;
    private final DoCungRepository doCungRepo;
    private final DiemCanBangRepository diemCanBangRepo;
    private final MauSacRepository mauSacRepo;
    private final BienTheService bienTheService;

    /* ===================== Đọc ===================== */

    @Transactional(readOnly = true)
    public List<SanPhamResponse> getAll() {
        // Lấy ảnh của tất cả sản phẩm bằng 1 câu truy vấn rồi gom theo id sản phẩm (tránh gọi DB cho từng dòng)
        Map<Integer, List<HinhAnhSanPham>> anhTheoSanPham = new HashMap<>();
        for (HinhAnhSanPham h : hinhAnhRepo.findByTrangThaiOrderByLaAnhChinhDescIdAsc(HinhAnhSanPham.HOAT_DONG)) {
            anhTheoSanPham.computeIfAbsent(h.getSanPham().getId(), k -> new ArrayList<>()).add(h);
        }
        List<SanPhamResponse> ketQua = new ArrayList<>();
        for (SanPham sp : sanPhamRepo.findAllByOrderByIdDesc()) {
            List<HinhAnhSanPham> anh = anhTheoSanPham.getOrDefault(sp.getId(), new ArrayList<>());
            ketQua.add(toResponse(sp, anh));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public SanPhamResponse getById(Integer id) {
        SanPham sp = timSanPham(id);
        return toResponse(sp, hinhAnhRepo.findBySanPhamId(id));
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    /*
     * Thêm sản phẩm (kèm các biến thể nếu có). Sản phẩm "đã tồn tại" = cùng TÊN + DANH MỤC + THƯƠNG HIỆU với một sản phẩm đã có.
     *  - Chưa tồn tại: tạo sản phẩm mới + tạo các biến thể.
     *  - Đã tồn tại, chưa xác nhận: trả 409 mã SAN_PHAM_DA_TON_TAI (kèm số biến thể mới / số biến thể trùng) để giao diện hỏi
     *    "Sản phẩm đã tồn tại, bạn muốn cập nhật?". Chưa ghi gì xuống DB.
     *  - Đã tồn tại, đã xác nhận (xacNhanCapNhat = true): CẬP NHẬT sản phẩm cũ (thuộc tính, mô tả, ảnh) và với từng biến thể:
     *    chưa có thì thêm mới, đã có (cùng màu + trọng lượng + chu vi) thì cập nhật giá bán + số lượng tồn.
     */
    @Transactional
    public SanPhamResponse them(SanPhamRequest req) {
        List<String> anh = layDanhSachAnh(req); // kiểm tra ảnh trước khi lưu bất cứ thứ gì
        Map<Integer, List<String>> anhMau = layAnhTheoMau(req);

        SanPham daCo = timSanPhamTrung(req);
        if (daCo != null) {
            int tongBienThe = req.bienThes() == null ? 0 : req.bienThes().size();
            int bienTheTrung = bienTheService.demTrung(daCo.getId(), req.bienThes());
            if (!Boolean.TRUE.equals(req.xacNhanCapNhat())) {
                Map<String, Object> chiTiet = new LinkedHashMap<>();
                chiTiet.put("id", daCo.getId());
                chiTiet.put("ma", daCo.getMaSanPham());
                chiTiet.put("ten", daCo.getTenSanPham());
                chiTiet.put("soBienTheMoi", tongBienThe - bienTheTrung);
                chiTiet.put("soBienTheCapNhat", bienTheTrung);
                throw ApiException.daTonTai("SAN_PHAM_DA_TON_TAI",
                        "Sản phẩm \"" + daCo.getTenSanPham() + "\" (" + daCo.getMaSanPham() + ") đã tồn tại. Bạn có muốn cập nhật không?",
                        chiTiet);
            }
            return capNhatSanPhamDaCo(daCo, req, anh, anhMau);
        }

        SanPham sp = new SanPham();
        sp.setTrangThai(SanPham.HOAT_DONG);
        sp.setNgayTao(LocalDateTime.now());
        napDuLieu(sp, req, true);
        batBuocCoAnh(List.of(), anh, true, anhMau, true, () -> mauCuaBienTheMoi(req));
        sp.setNgayCapNhat(LocalDateTime.now());
        sp = sanPhamRepo.save(sp);
        dongBoAnh(sp, anh);
        dongBoAnhTheoMau(sp, anhMau);
        bienTheService.luuNhieu(sp, req.bienThes());
        return toResponse(sp, hinhAnhRepo.findBySanPhamId(sp.getId()));
    }

    // Người dùng đã đồng ý cập nhật sản phẩm đã có: ghi thông tin mới vào sản phẩm cũ (giữ mã), rồi thêm / cập nhật biến thể
    private SanPhamResponse capNhatSanPhamDaCo(SanPham sp, SanPhamRequest req, List<String> anh, Map<Integer, List<String>> anhMau) {
        String moTaCu = sp.getMoTa();
        napDuLieu(sp, req, false);
        if (sp.getMoTa() == null) {
            sp.setMoTa(moTaCu); // form để trống mô tả thì giữ mô tả cũ, không xóa
        }
        // Ảnh cũ vẫn tính: không gửi ảnh mới thì giữ ảnh cũ, nhưng sau khi cập nhật sản phẩm vẫn phải có ảnh
        final Integer idSp = sp.getId();
        batBuocCoAnh(hinhAnhRepo.findBySanPhamId(idSp), anh, false, anhMau, false, () -> {
            Set<Integer> mauCan = new LinkedHashSet<>(bienTheService.mauCuaSanPham(idSp));
            mauCan.addAll(mauCuaBienTheMoi(req));
            return mauCan;
        });
        sp.setNgayCapNhat(LocalDateTime.now());
        sp = sanPhamRepo.save(sp);
        if (!anh.isEmpty()) {
            dongBoAnh(sp, anh); // không gửi ảnh nào thì giữ ảnh cũ
        }
        // Ảnh theo màu: chỉ cập nhật các màu có gửi ảnh, màu khác giữ nguyên ảnh cũ
        Map<Integer, List<String>> coAnh = new LinkedHashMap<>();
        anhMau.forEach((idMau, ds) -> {
            if (!ds.isEmpty()) coAnh.put(idMau, ds);
        });
        dongBoAnhTheoMau(sp, coAnh);
        bienTheService.luuNhieu(sp, req.bienThes());
        return toResponse(sp, hinhAnhRepo.findBySanPhamId(sp.getId()));
    }

    // Tìm sản phẩm đã có cùng tên (bỏ qua hoa thường, khoảng trắng thừa) + danh mục + thương hiệu; không có thì null
    private SanPham timSanPhamTrung(SanPhamRequest req) {
        if (req.idDanhMuc() == null || req.idThuongHieu() == null) {
            return null; // thiếu thì để napDuLieu báo lỗi "Chọn danh mục / thương hiệu"
        }
        String ten = chuanHoaTen(req.ten());
        if (ten.isEmpty()) {
            return null;
        }
        for (SanPham sp : sanPhamRepo.findByDanhMucIdAndThuongHieuId(req.idDanhMuc(), req.idThuongHieu())) {
            if (chuanHoaTen(sp.getTenSanPham()).equalsIgnoreCase(ten)) {
                return sp;
            }
        }
        return null;
    }

    private String chuanHoaTen(String ten) {
        return ten == null ? "" : ten.trim().replaceAll("\\s+", " ");
    }

    @Transactional
    public SanPhamResponse sua(Integer id, SanPhamRequest req) {
        SanPham sp = timSanPham(id);
        List<String> anh = layDanhSachAnh(req);
        Map<Integer, List<String>> anhMau = layAnhTheoMau(req); // null trong request = giữ nguyên ảnh theo màu
        // Đổi tên / danh mục / thương hiệu thành đúng bộ của một sản phẩm KHÁC thì thành 2 sản phẩm trùng nhau
        SanPham trung = timSanPhamTrung(req);
        if (trung != null && !trung.getId().equals(sp.getId())) {
            throw ApiException.conflict("Đã có sản phẩm \"" + trung.getTenSanPham() + "\" (" + trung.getMaSanPham()
                    + ") cùng tên, danh mục và thương hiệu. Hãy đặt tên khác.");
        }
        napDuLieu(sp, req, false);
        // Sửa sản phẩm cũng phải còn ảnh: ảnh chung gửi lên thay hoàn toàn ảnh chung cũ, ảnh theo màu gửi lên thì thay ảnh của màu đó
        batBuocCoAnh(hinhAnhRepo.findBySanPhamId(id), anh, true, anhMau, true, () -> bienTheService.mauCuaSanPham(id));
        sp.setNgayCapNhat(LocalDateTime.now());
        sp = sanPhamRepo.save(sp);
        dongBoAnh(sp, anh);
        dongBoAnhTheoMau(sp, anhMau);
        if (req.bienThes() != null && !req.bienThes().isEmpty()) {
            bienTheService.luuNhieu(sp, req.bienThes());
        }
        return toResponse(sp, hinhAnhRepo.findBySanPhamId(sp.getId()));
    }

    // Không xóa sản phẩm (đã có biến thể, hóa đơn), chỉ ẩn / hiện bằng cột trạng thái.
    // Biến thể giữ nguyên; khi sản phẩm ngưng thì không thể thêm biến thể mới hay kích hoạt lại biến thể (xem BienTheService).
    @Transactional
    public SanPhamResponse doiTrangThai(Integer id) {
        SanPham sp = timSanPham(id);
        if (dangHoatDong(sp)) {
            sp.setTrangThai(SanPham.NGUNG_HOAT_DONG);
        } else {
            sp.setTrangThai(SanPham.HOAT_DONG);
        }
        sp.setNgayCapNhat(LocalDateTime.now());
        sp = sanPhamRepo.save(sp);
        return toResponse(sp, hinhAnhRepo.findBySanPhamId(sp.getId()));
    }

    /* ===================== Hàm phụ ===================== */

    private SanPham timSanPham(Integer id) {
        return sanPhamRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm."));
    }

    private boolean dangHoatDong(SanPham sp) {
        return sp.getTrangThai() != null && sp.getTrangThai() == SanPham.HOAT_DONG;
    }

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo (để mỗi lần kiểm tra chỉ cần 1 dòng)
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    // Mã sản phẩm kế tiếp: SP01, SP02, ... SP99, SP100 (số thứ tự tối thiểu 2 chữ số, không đệm thêm số 0)
    private String taoMaMoi() {
        int max = 0;
        for (String ma : sanPhamRepo.findAllMaSanPham()) {
            String so = ma == null ? "" : ma.replaceAll("\\D", "");
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return MaUtil.dinhDang("SP", max + 1L);
    }

    // Kiểm tra dữ liệu người dùng gửi lên, hợp lệ thì ghi vào sp (chưa lưu xuống DB)
    private void napDuLieu(SanPham sp, SanPhamRequest req, boolean taoMoi) {
        // Mã: chỉ xử lý khi thêm mới (sửa thì giữ mã cũ). Để trống thì tự sinh.
        if (taoMoi) {
            String ma = req.ma() == null ? "" : req.ma().trim().toUpperCase(Locale.ROOT);
            if (ma.isEmpty()) {
                ma = taoMaMoi();
            }
            loi(!ma.matches("[A-Z0-9]{3,20}"), "Mã gồm 3-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.");
            if (sanPhamRepo.existsByMaSanPhamIgnoreCase(ma)) {
                throw ApiException.conflict("Mã sản phẩm đã tồn tại.");
            }
            sp.setMaSanPham(ma);
        }

        // Tên (cột SQL tối đa 255 ký tự)
        String ten = req.ten() == null ? "" : req.ten().trim().replaceAll("\\s+", " ");
        loi(ten.isEmpty(), "Nhập tên sản phẩm.");
        loi(ten.length() > 255, "Tên sản phẩm tối đa 255 ký tự.");
        sp.setTenSanPham(ten);

        // Mô tả: không bắt buộc
        String moTa = req.moTa() == null ? "" : req.moTa().trim();
        loi(moTa.length() > 4000, "Mô tả tối đa 4000 ký tự.");
        sp.setMoTa(moTa.isEmpty() ? null : moTa);

        // Danh mục + chất liệu cũ (null khi thêm mới), để biết người dùng có đổi hay không
        Integer idDanhMucCu = sp.getDanhMuc() == null ? null : sp.getDanhMuc().getId();
        Integer idChatLieuCu = sp.getChatLieu() == null ? null : sp.getChatLieu().getId();
        Integer idDoCungCu = sp.getDoCung() == null ? null : sp.getDoCung().getId();
        Integer idDiemCanBangCu = sp.getDiemCanBang() == null ? null : sp.getDiemCanBang().getId();
        Boolean laVotCu = sp.getDanhMuc() == null ? null : NhomDanhMuc.laVot(sp.getDanhMuc().getTen());

        // 6 thuộc tính: phải chọn, phải tồn tại, mục mới chọn phải đang hoạt động
        sp.setDanhMuc(ThuocTinhChon.chon(danhMucRepo, req.idDanhMuc(), sp.getDanhMuc(), "danh mục"));
        sp.setThuongHieu(ThuocTinhChon.chon(thuongHieuRepo, req.idThuongHieu(), sp.getThuongHieu(), "thương hiệu"));
        sp.setXuatXu(ThuocTinhChon.chon(xuatXuRepo, req.idXuatXu(), sp.getXuatXu(), "xuất xứ"));
        sp.setChatLieu(ThuocTinhChon.chon(chatLieuRepo, req.idChatLieu(), sp.getChatLieu(), "chất liệu"));
        // Chất liệu phải hợp với danh mục (vd Quần áo thể thao không dùng Nhôm / Titanium). Sản phẩm cũ giữ nguyên danh mục + chất liệu thì cho qua.
        boolean giuNguyen = idDanhMucCu != null && idDanhMucCu.equals(sp.getDanhMuc().getId())
                && idChatLieuCu != null && idChatLieuCu.equals(sp.getChatLieu().getId());
        if (!giuNguyen) {
            loi(!NhomDanhMuc.hop(NhomDanhMuc.tach(sp.getChatLieu().getNhomApDung()), sp.getDanhMuc().getTen()),
                    "Chất liệu \"" + sp.getChatLieu().getTen() + "\" không dùng cho danh mục \"" + sp.getDanhMuc().getTen() + "\". Hãy chọn chất liệu khác.");
        }
        sp.setDoCung(ThuocTinhChon.chon(doCungRepo, req.idDoCung(), sp.getDoCung(), "độ cứng"));
        sp.setDiemCanBang(ThuocTinhChon.chon(diemCanBangRepo, req.idDiemCanBang(), sp.getDiemCanBang(), "điểm cân bằng"));

        // Độ cứng / điểm cân bằng phải khớp danh mục (vợt: giá trị thật; khác: "Không áp dụng").
        // Dữ liệu cũ giữ nguyên danh mục + giá trị thì cho qua, giống chất liệu ở trên.
        String tenDanhMuc = sp.getDanhMuc().getTen();
        boolean doiDanhMuc = !sp.getDanhMuc().getId().equals(idDanhMucCu);
        if (doiDanhMuc || !sp.getDoCung().getId().equals(idDoCungCu)) {
            String loiDoCung = NhomDanhMuc.loiThuocTinhVot(tenDanhMuc, sp.getDoCung().getTen(), "độ cứng");
            loi(loiDoCung != null, loiDoCung);
        }
        if (doiDanhMuc || !sp.getDiemCanBang().getId().equals(idDiemCanBangCu)) {
            String loiCanBang = NhomDanhMuc.loiThuocTinhVot(tenDanhMuc, sp.getDiemCanBang().getTen(), "điểm cân bằng");
            loi(loiCanBang != null, loiCanBang);
        }
        // Đã có biến thể (theo trọng lượng + chu vi nếu là vợt, chỉ theo màu nếu không) thì không đổi qua lại giữa vợt và loại khác,
        // nếu không các biến thể cũ sẽ sai bộ thuộc tính
        if (!taoMoi && laVotCu != null && laVotCu != NhomDanhMuc.laVot(tenDanhMuc) && sp.getId() != null
                && bienTheService.coBienThe(sp.getId())) {
            throw ApiException.conflict("Sản phẩm đã có biến thể nên không thể đổi giữa danh mục vợt và danh mục khác. "
                    + "Hãy tạo sản phẩm mới cho danh mục \"" + tenDanhMuc + "\".");
        }
    }

    /* ----- Hình ảnh (bảng hinh_anh_san_pham) ----- */

    // Màu của các biến thể gửi kèm trong request (không trùng, đúng thứ tự gửi)
    private Set<Integer> mauCuaBienTheMoi(SanPhamRequest req) {
        Set<Integer> mau = new LinkedHashSet<>();
        if (req.bienThes() != null) {
            for (BienTheMoiRequest b : req.bienThes()) {
                if (b != null && b.idMauSac() != null) {
                    mau.add(b.idMauSac());
                }
            }
        }
        return mau;
    }

    /*
     * Ảnh là BẮT BUỘC (cô yêu cầu: "chưa có ảnh mà tại sao vẫn thành công"): sau khi lưu, sản phẩm phải có ít nhất 1 ảnh CHUNG,
     * hoặc mọi màu của biến thể đều có ảnh riêng (chưa có biến thể nào thì cần ít nhất 1 ảnh theo màu bất kỳ).
     *  - hienCo        : các dòng ảnh đang có của sản phẩm (sản phẩm mới: rỗng).
     *  - anhChung      : ảnh chung gửi lên; thayAnhChung = true thì danh sách này thay hẳn ảnh chung cũ (kể cả rỗng = bỏ hết),
     *                    false thì rỗng nghĩa là giữ ảnh chung cũ.
     *  - anhMau        : ảnh theo màu gửi lên; choPhepBoHet = true thì màu gửi danh sách rỗng bị bỏ hết ảnh.
     *  - layMauCan     : lấy các màu bắt buộc có ảnh (chỉ gọi khi không có ảnh chung, đỡ truy vấn CSDL).
     */
    private void batBuocCoAnh(List<HinhAnhSanPham> hienCo, List<String> anhChung, boolean thayAnhChung,
                              Map<Integer, List<String>> anhMau, boolean choPhepBoHet, Supplier<Collection<Integer>> layMauCan) {
        boolean coAnhChung = !anhChung.isEmpty() || (!thayAnhChung && !AnhSanPham.anhChung(hienCo).isEmpty());
        if (coAnhChung) {
            return;
        }
        Map<Integer, List<String>> anhMauCuoi = AnhSanPham.anhTheoMauSauKhiLuu(hienCo, anhMau, choPhepBoHet);
        Collection<Integer> mauCan = layMauCan.get();
        if (mauCan.isEmpty()) {
            loi(!AnhSanPham.coAnhTheoMau(anhMauCuoi), "Sản phẩm phải có ít nhất 1 ảnh. Hãy thêm ảnh chung của sản phẩm.");
            return;
        }
        List<Integer> thieu = AnhSanPham.mauChuaCoAnh(mauCan, anhMauCuoi);
        if (!thieu.isEmpty()) {
            List<String> ten = new ArrayList<>();
            for (Integer id : thieu) {
                ten.add(mauSacRepo.findById(id).map(MauSac::getTen).orElse("#" + id));
            }
            throw ApiException.badRequest("Chưa có ảnh cho màu " + String.join(", ", ten)
                    + ". Hãy thêm ảnh chung của sản phẩm hoặc ảnh riêng cho từng màu.");
        }
    }

    // Gộp ảnh chính + ảnh phụ thành 1 danh sách không trùng, phần tử đầu tiên là ảnh chính.
    private List<String> layDanhSachAnh(SanPhamRequest req) {
        String chinh = req.anhChinh() == null ? "" : req.anhChinh().trim();
        List<String> ds = new ArrayList<>();
        if (!chinh.isEmpty()) {
            ds.add(kiemTraUrl(chinh));
        }
        boolean coAnhPhu = false;
        if (req.anhPhu() != null) {
            for (String u : req.anhPhu()) {
                String x = u == null ? "" : u.trim();
                if (x.isEmpty()) {
                    continue;
                }
                coAnhPhu = true;
                kiemTraUrl(x);
                if (!ds.contains(x)) {
                    ds.add(x);
                }
            }
        }
        loi(chinh.isEmpty() && coAnhPhu, "Nhập ảnh chính trước khi thêm ảnh phụ.");
        loi(ds.size() > TOI_DA_ANH, "Mỗi sản phẩm tối đa " + TOI_DA_ANH + " ảnh.");
        return ds;
    }

    private String kiemTraUrl(String url) {
        loi(url.length() > 500, "Đường dẫn ảnh tối đa 500 ký tự.");
        // Ảnh hợp lệ: ảnh tải lên từ máy (/api/uploads/...) hoặc link http(s)
        boolean laLinkNgoai = url.matches("(?i)^https?://\\S+$");
        boolean laAnhNoiBo = url.matches("^/api/uploads/[A-Za-z0-9._-]+$") && !url.contains("..");
        loi(!laLinkNgoai && !laAnhNoiBo, "Ảnh phải được tải lên từ máy hoặc là đường dẫn http:// hoặc https://.");
        return url;
    }

    /*
     * Ảnh theo màu: [{ idMauSac, anh: [url...] }] -> {idMàu -> [url...]} đã kiểm tra (màu tồn tại, không trùng màu,
     * tối đa TOI_DA_ANH_MOI_MAU ảnh / màu, đường dẫn hợp lệ, bỏ url trùng). request không gửi (null) thì trả về map rỗng = không đổi.
     */
    private Map<Integer, List<String>> layAnhTheoMau(SanPhamRequest req) {
        Map<Integer, List<String>> ketQua = new LinkedHashMap<>();
        if (req.anhTheoMau() == null) {
            return ketQua;
        }
        for (AnhTheoMauRequest x : req.anhTheoMau()) {
            loi(x == null || x.idMauSac() == null, "Ảnh theo màu thiếu màu sắc.");
            loi(ketQua.containsKey(x.idMauSac()), "Mỗi màu chỉ gửi một danh sách ảnh.");
            loi(!mauSacRepo.existsById(x.idMauSac()), "Màu sắc của ảnh không tồn tại.");
            List<String> ds = new ArrayList<>();
            if (x.anh() != null) {
                for (String u : x.anh()) {
                    String url = u == null ? "" : u.trim();
                    if (url.isEmpty()) continue;
                    kiemTraUrl(url);
                    if (!ds.contains(url)) ds.add(url);
                }
            }
            loi(ds.size() > TOI_DA_ANH_MOI_MAU, "Mỗi màu tối đa " + TOI_DA_ANH_MOI_MAU + " ảnh.");
            ketQua.put(x.idMauSac(), ds);
        }
        return ketQua;
    }

    // Đồng bộ ảnh CHUNG (id_mau_sac = NULL) với danh sách mong muốn: ảnh còn trong danh sách thì bật, ảnh bị bỏ thì
    // chuyển trạng thái 0 (không xóa dòng), ảnh mới thì thêm dòng. Đúng 1 ảnh (phần tử đầu) có la_anh_chinh = 1. Ảnh theo màu giữ nguyên.
    private void dongBoAnh(SanPham sp, List<String> anh) {
        hinhAnhRepo.saveAll(AnhSanPham.dongBo(sp, null, hinhAnhRepo.findBySanPhamId(sp.getId()), anh));
    }

    // Đồng bộ ảnh của từng màu có trong map (màu không có trong map giữ nguyên)
    private void dongBoAnhTheoMau(SanPham sp, Map<Integer, List<String>> anhMau) {
        if (anhMau.isEmpty()) {
            return;
        }
        List<HinhAnhSanPham> hienCo = hinhAnhRepo.findBySanPhamId(sp.getId());
        List<HinhAnhSanPham> canLuu = new ArrayList<>();
        anhMau.forEach((idMau, ds) -> canLuu.addAll(AnhSanPham.dongBo(sp, idMau, hienCo, ds)));
        hinhAnhRepo.saveAll(canLuu);
    }

    // anh = mọi dòng ảnh của sản phẩm (kể cả đã bỏ, kể cả ảnh theo màu); hàm tự lọc ảnh đang dùng
    private SanPhamResponse toResponse(SanPham sp, List<HinhAnhSanPham> anh) {
        String anhChinh = "";
        List<String> anhPhu = new ArrayList<>();
        for (HinhAnhSanPham h : AnhSanPham.anhChung(anh)) {
            if (Boolean.TRUE.equals(h.getLaAnhChinh()) && anhChinh.isEmpty()) {
                anhChinh = h.getDuongDanHinhAnh();
            } else {
                anhPhu.add(h.getDuongDanHinhAnh());
            }
        }
        return new SanPhamResponse(
                sp.getId(),
                sp.getMaSanPham(),
                sp.getTenSanPham(),
                sp.getDanhMuc().getId(),
                sp.getThuongHieu().getId(),
                sp.getXuatXu().getId(),
                sp.getChatLieu().getId(),
                sp.getDoCung().getId(),
                sp.getDiemCanBang().getId(),
                sp.getMoTa() == null ? "" : sp.getMoTa(),
                anhChinh,
                anhPhu,
                sp.getNgayTao() == null ? null : sp.getNgayTao().toLocalDate(),
                sp.getNgayCapNhat() == null ? null : sp.getNgayCapNhat().toLocalDate(),
                dangHoatDong(sp),
                AnhSanPham.anhTheoMauResponse(anh),
                AnhSanPham.anhDaiDien(anh));
    }
}
