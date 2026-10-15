package com.footstyle.demo.service;

import com.footstyle.demo.dto.ThuocTinhRequest;
import com.footstyle.demo.dto.ThuocTinhResponse;
import com.footstyle.demo.entity.ChatLieu;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.ThuocTinh;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.entity.XuatXu;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ChatLieuRepository;
import com.footstyle.demo.repository.ChuViRepository;
import com.footstyle.demo.repository.DanhMucRepository;
import com.footstyle.demo.repository.DiemCanBangRepository;
import com.footstyle.demo.repository.DoCungRepository;
import com.footstyle.demo.repository.MauSacRepository;
import com.footstyle.demo.repository.SanPhamRepository;
import com.footstyle.demo.repository.ThuocTinhRepository;
import com.footstyle.demo.repository.ThuongHieuRepository;
import com.footstyle.demo.repository.TrongLuongRepository;
import com.footstyle.demo.repository.XuatXuRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD chung cho 9 bảng thuộc tính của sản phẩm (cùng cấu trúc id, mã, tên, trạng thái).
 * "loai" là đường dẫn trên API: danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang,
 * mau-sac, trong-luong, chu-vi.
 */
@Service
public class ThuocTinhService {

    // loai -> repository của bảng đó
    private final Map<String, ThuocTinhRepository<ThuocTinh>> repos = new HashMap<>();
    // loai -> cách tạo entity rỗng của bảng đó (new DanhMuc(), new MauSac(), ...)
    private final Map<String, Supplier<? extends ThuocTinh>> taoMoi = new HashMap<>();
    // loai -> tên tiếng Việt, dùng trong câu thông báo lỗi
    private final Map<String, String> nhan = new HashMap<>();
    // loai -> tiền tố mã tự cấp (DM01, TH01, ...)
    private final Map<String, String> tienTo = new HashMap<>();

    private final SanPhamRepository sanPhamRepo;

    public ThuocTinhService(DanhMucRepository danhMuc, ThuongHieuRepository thuongHieu, XuatXuRepository xuatXu,
                            ChatLieuRepository chatLieu, DoCungRepository doCung, DiemCanBangRepository diemCanBang,
                            MauSacRepository mauSac, TrongLuongRepository trongLuong, ChuViRepository chuVi,
                            SanPhamRepository sanPhamRepo) {
        this.sanPhamRepo = sanPhamRepo;
        dangKy("danh-muc", "danh mục", "DM", danhMuc, DanhMuc::new);
        dangKy("thuong-hieu", "thương hiệu", "TH", thuongHieu, ThuongHieu::new);
        dangKy("xuat-xu", "xuất xứ", "XX", xuatXu, XuatXu::new);
        dangKy("chat-lieu", "chất liệu", "CL", chatLieu, ChatLieu::new);
        dangKy("do-cung", "độ cứng", "DC", doCung, DoCung::new);
        dangKy("diem-can-bang", "điểm cân bằng", "CB", diemCanBang, DiemCanBang::new);
        dangKy("mau-sac", "màu sắc", "MS", mauSac, MauSac::new);
        dangKy("trong-luong", "trọng lượng", "TL", trongLuong, TrongLuong::new);
        dangKy("chu-vi", "chu vi", "CV", chuVi, ChuVi::new);
    }

    // Ép kiểu repository riêng của từng bảng về kiểu chung ThuocTinhRepository<ThuocTinh>.
    // An toàn vì mỗi repository chỉ nhận đúng loại entity của nó (taoMoi cùng loai tạo đúng entity đó).
    @SuppressWarnings("unchecked")
    private void dangKy(String loai, String ten, String prefix, ThuocTinhRepository<? extends ThuocTinh> repo,
                        Supplier<? extends ThuocTinh> tao) {
        repos.put(loai, (ThuocTinhRepository<ThuocTinh>) repo);
        taoMoi.put(loai, tao);
        nhan.put(loai, ten);
        tienTo.put(loai, prefix);
    }

    /* ===================== Đọc ===================== */

    @Transactional(readOnly = true)
    public List<ThuocTinhResponse> getAll(String loai) {
        List<ThuocTinhResponse> ketQua = new ArrayList<>();
        for (ThuocTinh t : repo(loai).findAllByOrderByIdDesc()) {
            ketQua.add(toResponse(t));
        }
        return ketQua;
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public ThuocTinhResponse them(String loai, ThuocTinhRequest req) {
        ThuocTinhRepository<ThuocTinh> repo = repo(loai);
        String ten = kiemTraTen(loai, req.ten(), -1);

        // Mã do hệ thống tự cấp (DM01, TH02, ...), không nhận mã từ người dùng
        ThuocTinh t = taoMoi.get(loai).get();
        t.setMa(taoMaMoi(loai));
        t.setTen(ten);
        t.setTrangThai(ThuocTinh.HOAT_DONG);
        if (t instanceof ChatLieu cl) {
            cl.setNhomApDung(kiemTraNhom(req.nhom(), true));
        }
        return toResponse(repo.save(t));
    }

    // Sửa chỉ đổi tên, mã giữ nguyên
    @Transactional
    public ThuocTinhResponse sua(String loai, Integer id, ThuocTinhRequest req) {
        ThuocTinhRepository<ThuocTinh> repo = repo(loai);
        ThuocTinh t = tim(loai, id);
        String tenMoi = kiemTraTen(loai, req.ten(), id);
        loi(laMucHeThong(loai, t) && !NhomDanhMuc.laKhongApDung(tenMoi),
                "Mục \"Không áp dụng\" do hệ thống dùng cho danh mục không phải vợt, không đổi tên được.");
        // Đổi tên danh mục làm nó đổi từ vợt sang loại khác (hoặc ngược lại), vd "Vợt cầu lông" -> "Phụ kiện vợt"... thì
        // các sản phẩm đang thuộc danh mục sẽ sai bộ thuộc tính (độ cứng, trọng lượng...)
        if ("danh-muc".equals(loai) && NhomDanhMuc.laVot(t.getTen()) != NhomDanhMuc.laVot(tenMoi)
                && sanPhamRepo.existsByDanhMucId(id)) {
            throw ApiException.conflict("Danh mục đang có sản phẩm, không thể đổi tên làm danh mục chuyển giữa loại vợt và loại khác.");
        }
        t.setTen(tenMoi);
        // Chất liệu: có gửi nhóm danh mục thì cập nhật; không gửi thì giữ nguyên nhóm cũ
        if (t instanceof ChatLieu cl && req.nhom() != null) {
            cl.setNhomApDung(kiemTraNhom(req.nhom(), true));
        }
        return toResponse(repo.save(t));
    }

    // Không xóa thuộc tính (đã có sản phẩm dùng), chỉ ẩn / hiện bằng cột trạng thái
    @Transactional
    public ThuocTinhResponse doiTrangThai(String loai, Integer id) {
        ThuocTinhRepository<ThuocTinh> repo = repo(loai);
        ThuocTinh t = tim(loai, id);
        if (dangHoatDong(t)) {
            loi(laMucHeThong(loai, t), "Mục \"Không áp dụng\" do hệ thống dùng cho danh mục không phải vợt, không ngưng được.");
            t.setTrangThai(ThuocTinh.NGUNG_HOAT_DONG);
        } else {
            t.setTrangThai(ThuocTinh.HOAT_DONG);
        }
        return toResponse(repo.save(t));
    }

    /* ===================== Hàm phụ ===================== */

    private ThuocTinhRepository<ThuocTinh> repo(String loai) {
        ThuocTinhRepository<ThuocTinh> repo = repos.get(loai);
        if (repo == null) {
            throw ApiException.notFound("Loại thuộc tính không hợp lệ.");
        }
        return repo;
    }

    private ThuocTinh tim(String loai, Integer id) {
        return repo(loai).findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy " + nhan.get(loai) + "."));
    }

    // 4 bảng chỉ vợt mới có (độ cứng, điểm cân bằng, trọng lượng, chu vi) có mục "Không áp dụng" gán tự động cho danh mục khác
    private static final List<String> LOAI_CO_MUC_KHONG_AP_DUNG = List.of("do-cung", "diem-can-bang", "trong-luong", "chu-vi");

    private boolean laMucHeThong(String loai, ThuocTinh t) {
        return LOAI_CO_MUC_KHONG_AP_DUNG.contains(loai) && NhomDanhMuc.laKhongApDung(t.getTen());
    }

    private boolean dangHoatDong(ThuocTinh t) {
        return t.getTrangThai() != null && t.getTrangThai() == ThuocTinh.HOAT_DONG;
    }

    // Tên: bắt buộc, tối đa 100 ký tự, không trùng bản ghi khác. Trả về tên đã chuẩn hóa khoảng trắng.
    private String kiemTraTen(String loai, String tenGui, Integer idBoQua) {
        String ten = tenGui == null ? "" : tenGui.trim().replaceAll("\\s+", " ");
        loi(ten.isEmpty(), "Nhập tên " + nhan.get(loai) + ".");
        loi(ten.length() > 100, "Tên tối đa 100 ký tự.");
        if (repo(loai).existsByTenIgnoreCaseAndIdNot(ten, idBoQua)) {
            throw ApiException.conflict("Tên " + nhan.get(loai) + " đã tồn tại.");
        }
        return ten;
    }

    // Nhóm danh mục của chất liệu: mỗi nhóm phải hợp lệ, không trùng; thêm mới thì bắt buộc chọn ít nhất một nhóm.
    // Trả về chuỗi lưu DB, ví dụ "VOT,QUAN_AO".
    private String kiemTraNhom(List<String> nhom, boolean batBuoc) {
        List<String> ds = new ArrayList<>();
        if (nhom != null) {
            for (String n : nhom) {
                String v = n == null ? "" : n.trim().toUpperCase(Locale.ROOT);
                loi(!NhomDanhMuc.TAT_CA.contains(v), "Danh mục sử dụng chất liệu không hợp lệ.");
                if (!ds.contains(v)) {
                    ds.add(v);
                }
            }
        }
        loi(batBuoc && ds.isEmpty(), "Chọn ít nhất một danh mục sử dụng chất liệu này.");
        return String.join(",", ds);
    }

    // Mã kế tiếp của từng loại: tiền tố + số lớn nhất hiện có + 1, ví dụ CB03 -> CB04 (đọc cả mã cũ CB003)
    private String taoMaMoi(String loai) {
        int max = 0;
        for (ThuocTinh t : repo(loai).findAll()) {
            String so = t.getMa() == null ? "" : t.getMa().replaceAll("\\D", ""); // chỉ giữ chữ số
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return MaUtil.dinhDang(tienTo.get(loai), max + 1L);
    }

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    private ThuocTinhResponse toResponse(ThuocTinh t) {
        List<String> nhom = t instanceof ChatLieu cl ? NhomDanhMuc.tach(cl.getNhomApDung()) : null;
        return new ThuocTinhResponse(t.getId(), t.getMa(), t.getTen(), dangHoatDong(t), nhom);
    }
}
