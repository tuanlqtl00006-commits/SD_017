package com.footstyle.demo.service;

import com.footstyle.demo.dto.SanPhamRequest;
import com.footstyle.demo.dto.SanPhamResponse;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ChatLieuRepository;
import com.footstyle.demo.repository.DanhMucRepository;
import com.footstyle.demo.repository.DiemCanBangRepository;
import com.footstyle.demo.repository.DoCungRepository;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.SanPhamRepository;
import com.footstyle.demo.repository.ThuongHieuRepository;
import com.footstyle.demo.repository.XuatXuRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final SanPhamRepository sanPhamRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final DanhMucRepository danhMucRepo;
    private final ThuongHieuRepository thuongHieuRepo;
    private final XuatXuRepository xuatXuRepo;
    private final ChatLieuRepository chatLieuRepo;
    private final DoCungRepository doCungRepo;
    private final DiemCanBangRepository diemCanBangRepo;

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
        return toResponse(sp, anhDangDung(id));
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public SanPhamResponse them(SanPhamRequest req) {
        List<String> anh = layDanhSachAnh(req); // kiểm tra ảnh trước khi lưu bất cứ thứ gì
        SanPham sp = new SanPham();
        sp.setTrangThai(SanPham.HOAT_DONG);
        sp.setNgayTao(LocalDateTime.now());
        napDuLieu(sp, req, true);
        sp.setNgayCapNhat(LocalDateTime.now());
        sp = sanPhamRepo.save(sp);
        dongBoAnh(sp, anh);
        return toResponse(sp, anhDangDung(sp.getId()));
    }

    @Transactional
    public SanPhamResponse sua(Integer id, SanPhamRequest req) {
        SanPham sp = timSanPham(id);
        List<String> anh = layDanhSachAnh(req);
        napDuLieu(sp, req, false);
        sp.setNgayCapNhat(LocalDateTime.now());
        sp = sanPhamRepo.save(sp);
        dongBoAnh(sp, anh);
        return toResponse(sp, anhDangDung(sp.getId()));
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
        return toResponse(sp, anhDangDung(sp.getId()));
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

    // Mã sản phẩm kế tiếp: SP001, SP002, ...
    private String taoMaMoi() {
        int max = 0;
        for (String ma : sanPhamRepo.findAllMaSanPham()) {
            String so = ma == null ? "" : ma.replaceAll("\\D", "");
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return String.format("SP%03d", max + 1);
    }

    // Kiểm tra dữ liệu người dùng gửi lên, hợp lệ thì ghi vào sp (chưa lưu xuống DB)
    private void napDuLieu(SanPham sp, SanPhamRequest req, boolean taoMoi) {
        // Mã: chỉ xử lý khi thêm mới (sửa thì giữ mã cũ). Để trống thì tự sinh.
        if (taoMoi) {
            String ma = req.ma() == null ? "" : req.ma().trim().toUpperCase();
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

        // 6 thuộc tính: phải chọn, phải tồn tại, mục mới chọn phải đang hoạt động
        sp.setDanhMuc(ThuocTinhChon.chon(danhMucRepo, req.idDanhMuc(), sp.getDanhMuc(), "danh mục"));
        sp.setThuongHieu(ThuocTinhChon.chon(thuongHieuRepo, req.idThuongHieu(), sp.getThuongHieu(), "thương hiệu"));
        sp.setXuatXu(ThuocTinhChon.chon(xuatXuRepo, req.idXuatXu(), sp.getXuatXu(), "xuất xứ"));
        sp.setChatLieu(ThuocTinhChon.chon(chatLieuRepo, req.idChatLieu(), sp.getChatLieu(), "chất liệu"));
        sp.setDoCung(ThuocTinhChon.chon(doCungRepo, req.idDoCung(), sp.getDoCung(), "độ cứng"));
        sp.setDiemCanBang(ThuocTinhChon.chon(diemCanBangRepo, req.idDiemCanBang(), sp.getDiemCanBang(), "điểm cân bằng"));
    }

    /* ----- Hình ảnh (bảng hinh_anh_san_pham) ----- */

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

    // Đồng bộ bảng ảnh với danh sách mong muốn: ảnh còn trong danh sách thì bật, ảnh bị bỏ thì chuyển trạng thái 0
    // (không xóa dòng), ảnh mới thì thêm dòng. Đúng 1 ảnh (phần tử đầu) có la_anh_chinh = 1.
    private void dongBoAnh(SanPham sp, List<String> anh) {
        List<HinhAnhSanPham> hienCo = hinhAnhRepo.findBySanPhamId(sp.getId());
        Set<String> daXuLy = new HashSet<>();
        List<HinhAnhSanPham> canLuu = new ArrayList<>();

        for (HinhAnhSanPham h : hienCo) {
            String url = h.getDuongDanHinhAnh();
            boolean giu = anh.contains(url) && daXuLy.add(url); // add trả false nếu url này đã có dòng khác dùng rồi
            h.setTrangThai(giu ? HinhAnhSanPham.HOAT_DONG : HinhAnhSanPham.NGUNG_HOAT_DONG);
            h.setLaAnhChinh(giu && url.equals(anh.get(0)));
            canLuu.add(h);
        }
        for (String url : anh) {
            if (!daXuLy.contains(url)) {
                HinhAnhSanPham moi = new HinhAnhSanPham();
                moi.setSanPham(sp);
                moi.setDuongDanHinhAnh(url);
                moi.setLaAnhChinh(url.equals(anh.get(0)));
                moi.setTrangThai(HinhAnhSanPham.HOAT_DONG);
                canLuu.add(moi);
            }
        }
        hinhAnhRepo.saveAll(canLuu);
    }

    // Ảnh đang dùng của 1 sản phẩm, ảnh chính xếp trước
    private List<HinhAnhSanPham> anhDangDung(Integer idSanPham) {
        List<HinhAnhSanPham> chinh = new ArrayList<>();
        List<HinhAnhSanPham> phu = new ArrayList<>();
        for (HinhAnhSanPham h : hinhAnhRepo.findBySanPhamId(idSanPham)) {
            if (h.getTrangThai() == null || h.getTrangThai() != HinhAnhSanPham.HOAT_DONG) {
                continue;
            }
            if (Boolean.TRUE.equals(h.getLaAnhChinh())) {
                chinh.add(h);
            } else {
                phu.add(h);
            }
        }
        chinh.addAll(phu);
        return chinh;
    }

    private SanPhamResponse toResponse(SanPham sp, List<HinhAnhSanPham> anh) {
        String anhChinh = "";
        List<String> anhPhu = new ArrayList<>();
        for (HinhAnhSanPham h : anh) {
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
                dangHoatDong(sp));
    }
}
