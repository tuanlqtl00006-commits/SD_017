package com.footstyle.demo.service;

import com.footstyle.demo.dto.BienTheMoiRequest;
import com.footstyle.demo.dto.BienTheRequest;
import com.footstyle.demo.dto.BienTheResponse;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ChuViRepository;
import com.footstyle.demo.repository.MauSacRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.repository.SanPhamRepository;
import com.footstyle.demo.repository.TrongLuongRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý biến thể sản phẩm (bảng san_pham_chi_tiet) = sản phẩm + màu sắc + trọng lượng + chu vi, kèm giá bán, tồn kho.
 */
@Service
@RequiredArgsConstructor
public class BienTheService {

    private final SanPhamChiTietRepository bienTheRepo;
    private final SanPhamRepository sanPhamRepo;
    private final MauSacRepository mauSacRepo;
    private final TrongLuongRepository trongLuongRepo;
    private final ChuViRepository chuViRepo;

    /* ===================== Đọc ===================== */

    @Transactional(readOnly = true)
    public List<BienTheResponse> getAll() {
        List<BienTheResponse> ketQua = new ArrayList<>();
        for (SanPhamChiTiet b : bienTheRepo.findAllByOrderByIdDesc()) {
            ketQua.add(toResponse(b));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public BienTheResponse getById(Integer id) {
        return toResponse(timBienThe(id));
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public BienTheResponse them(BienTheRequest req) {
        // Sản phẩm của biến thể: phải tồn tại và đang hoạt động
        loi(req.idSanPham() == null, "Chọn sản phẩm.");
        SanPham sp = sanPhamRepo.findById(req.idSanPham())
                .orElseThrow(() -> ApiException.badRequest("Sản phẩm không tồn tại."));
        loi(sp.getTrangThai() == null || sp.getTrangThai() != SanPham.HOAT_DONG,
                "Sản phẩm đã ngưng hoạt động, không thể thêm biến thể mới.");

        // Sản phẩm đã có biến thể cùng màu + trọng lượng + chu vi: không tạo trùng mà hỏi người dùng có muốn cập nhật không
        SanPhamChiTiet daCo = timBienTheTrung(sp.getId(), req.idMauSac(), req.idTrongLuong(), req.idChuVi());
        if (daCo != null) {
            if (!Boolean.TRUE.equals(req.xacNhanCapNhat())) {
                Map<String, Object> chiTiet = new LinkedHashMap<>();
                chiTiet.put("id", daCo.getId());
                chiTiet.put("ma", daCo.getMaSpct());
                chiTiet.put("giaBan", daCo.getGiaBan());
                chiTiet.put("soLuongTon", daCo.getSoLuongTon());
                throw ApiException.daTonTai("BIEN_THE_DA_TON_TAI",
                        "Biến thể " + daCo.getMaSpct() + " đã tồn tại (cùng màu sắc, trọng lượng, chu vi). Bạn có muốn cập nhật giá bán và số lượng tồn không?",
                        chiTiet);
            }
            return toResponse(capNhatBienTheDaCo(daCo, sp, req));
        }

        SanPhamChiTiet b = new SanPhamChiTiet();
        b.setTrangThai(SanPhamChiTiet.HOAT_DONG);
        b.setNgayTao(LocalDateTime.now());

        // Mã biến thể: đúng định dạng và chưa ai dùng
        String ma = docMa(req);
        if (bienTheRepo.existsByMaSpctIgnoreCase(ma)) {
            throw ApiException.conflict("Mã biến thể đã tồn tại.");
        }
        b.setMaSpct(ma);

        napDuLieu(b, sp, req);
        b.setNgayCapNhat(LocalDateTime.now());
        return toResponse(bienTheRepo.save(b));
    }

    /* ===================== Thêm nhiều biến thể cùng sản phẩm ===================== */

    /** Kết quả lưu nhiều biến thể: moi = số biến thể tạo mới, capNhat = số biến thể đã có được cập nhật giá / tồn. */
    public record KetQuaLuu(int moi, int capNhat) {
    }

    /** Có bao nhiêu biến thể trong danh sách TRÙNG (cùng màu + trọng lượng + chu vi) với biến thể đã có của sản phẩm. */
    @Transactional(readOnly = true)
    public int demTrung(Integer idSanPham, List<BienTheMoiRequest> ds) {
        int dem = 0;
        if (ds != null) {
            for (BienTheMoiRequest x : ds) {
                if (timBienTheTrung(idSanPham, x.idMauSac(), x.idTrongLuong(), x.idChuVi()) != null) {
                    dem++;
                }
            }
        }
        return dem;
    }

    /**
     * Lưu danh sách biến thể của một sản phẩm: bộ (màu + trọng lượng + chu vi) chưa có thì tạo mới (mã tự sinh),
     * đã có thì CẬP NHẬT giá bán + số lượng tồn (và bật lại nếu đang ngưng). Cùng thành công hoặc cùng hủy.
     */
    @Transactional
    public KetQuaLuu luuNhieu(SanPham sp, List<BienTheMoiRequest> ds) {
        if (ds == null || ds.isEmpty()) {
            return new KetQuaLuu(0, 0);
        }
        loi(sp.getTrangThai() == null || sp.getTrangThai() != SanPham.HOAT_DONG,
                "Sản phẩm đang ngưng hoạt động, hãy kích hoạt sản phẩm trước khi thêm biến thể.");

        // Một danh sách không được có 2 dòng cùng màu + trọng lượng + chu vi
        Set<String> daThay = new HashSet<>();
        for (BienTheMoiRequest x : ds) {
            loi(x == null || x.idMauSac() == null || x.idTrongLuong() == null || x.idChuVi() == null,
                    "Mỗi biến thể cần chọn đủ màu sắc, trọng lượng và chu vi.");
            loi(!daThay.add(x.idMauSac() + "-" + x.idTrongLuong() + "-" + x.idChuVi()),
                    "Danh sách biến thể có hai dòng cùng màu sắc, trọng lượng và chu vi.");
        }

        int moi = 0;
        int capNhat = 0;
        for (BienTheMoiRequest x : ds) {
            BienTheRequest req = new BienTheRequest(sp.getId(), x.idMauSac(), x.idTrongLuong(), x.idChuVi(), null,
                    x.giaBan(), x.soLuongTon(), true);
            SanPhamChiTiet daCo = timBienTheTrung(sp.getId(), x.idMauSac(), x.idTrongLuong(), x.idChuVi());
            if (daCo != null) {
                capNhatBienTheDaCo(daCo, sp, req);
                capNhat++;
            } else {
                SanPhamChiTiet b = new SanPhamChiTiet();
                b.setTrangThai(SanPhamChiTiet.HOAT_DONG);
                b.setNgayTao(LocalDateTime.now());
                napDuLieu(b, sp, req);
                b.setMaSpct(taoMaBienThe(sp, b));
                b.setNgayCapNhat(LocalDateTime.now());
                bienTheRepo.save(b);
                moi++;
            }
        }
        return new KetQuaLuu(moi, capNhat);
    }

    // Sửa: được đổi sản phẩm (vd SP008 -> SP007), mã biến thể không sửa tay (chỉ đổi phần đầu theo sản phẩm mới)
    @Transactional
    public BienTheResponse sua(Integer id, BienTheRequest req) {
        SanPhamChiTiet b = timBienThe(id);

        // Sản phẩm: mặc định giữ nguyên; nếu đổi sang sản phẩm khác thì sản phẩm mới phải tồn tại và đang hoạt động
        loi(req.idSanPham() == null, "Chọn sản phẩm.");
        SanPham sp = b.getSanPham();
        String ma = b.getMaSpct();
        if (!req.idSanPham().equals(sp.getId())) {
            SanPham spMoi = sanPhamRepo.findById(req.idSanPham())
                    .orElseThrow(() -> ApiException.badRequest("Sản phẩm không tồn tại."));
            loi(spMoi.getTrangThai() == null || spMoi.getTrangThai() != SanPham.HOAT_DONG,
                    "Sản phẩm đã ngưng hoạt động, không thể chuyển biến thể sang sản phẩm này.");

            // Mã biến thể KHÔNG cho sửa tay (bỏ qua req.ma()). Chỉ khi đổi sản phẩm mà mã đang bắt đầu bằng
            // mã sản phẩm cũ (vd SP008-DEN-4U-G5) thì phần đầu mã tự đổi theo (SP007-DEN-4U-G5).
            String tienToCu = sp.getMaSanPham() + "-";
            if (ma.toUpperCase().startsWith(tienToCu.toUpperCase())) {
                ma = (spMoi.getMaSanPham() + ma.substring(sp.getMaSanPham().length())).toUpperCase();
                loi(ma.length() > 50, "Mã biến thể mới dài quá 50 ký tự.");
                // Kiểm tra trước khi sửa b để Hibernate chưa ghi gì xuống DB
                if (bienTheRepo.existsByMaSpctIgnoreCaseAndIdNot(ma, id)) {
                    throw ApiException.conflict("Mã biến thể " + ma + " đã tồn tại.");
                }
            }
            sp = spMoi;
        }

        // napDuLieu kiểm tra "sản phẩm này đã có biến thể cùng màu + trọng lượng + chu vi chưa" rồi mới ghi vào b
        napDuLieu(b, sp, req);
        b.setMaSpct(ma);
        b.setNgayCapNhat(LocalDateTime.now());
        return toResponse(bienTheRepo.save(b));
    }

    // Không xóa biến thể (có thể đã nằm trong hóa đơn), chỉ ẩn / hiện bằng cột trạng thái
    @Transactional
    public BienTheResponse doiTrangThai(Integer id) {
        SanPhamChiTiet b = timBienThe(id);
        if (dangHoatDong(b)) {
            b.setTrangThai(SanPhamChiTiet.NGUNG_HOAT_DONG);
        } else {
            SanPham sp = b.getSanPham();
            loi(sp.getTrangThai() == null || sp.getTrangThai() != SanPham.HOAT_DONG,
                    "Sản phẩm đang ngưng hoạt động, hãy kích hoạt sản phẩm trước khi kích hoạt biến thể.");
            b.setTrangThai(SanPhamChiTiet.HOAT_DONG);
        }
        b.setNgayCapNhat(LocalDateTime.now());
        return toResponse(bienTheRepo.save(b));
    }

    /* ===================== Hàm phụ ===================== */

    private SanPhamChiTiet timBienThe(Integer id) {
        return bienTheRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy biến thể."));
    }

    private boolean dangHoatDong(SanPhamChiTiet b) {
        return b.getTrangThai() != null && b.getTrangThai() == SanPhamChiTiet.HOAT_DONG;
    }

    // Lấy mã từ request: bỏ khoảng trắng, viết hoa, kiểm tra định dạng. Dùng chung cho thêm và sửa.
    private String docMa(BienTheRequest req) {
        String ma = req.ma() == null ? "" : req.ma().trim().toUpperCase();
        loi(ma.isEmpty(), "Nhập mã biến thể.");
        loi(!ma.matches("[A-Z0-9-]{3,50}"), "Mã gồm 3-50 ký tự chữ, số hoặc dấu gạch ngang, không dấu, không khoảng trắng.");
        return ma;
    }

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    // Kiểm tra màu / trọng lượng / chu vi / giá / tồn, hợp lệ thì ghi sản phẩm + các giá trị đó vào b (chưa lưu xuống DB).
    // Dùng chung cho thêm và sửa. Phải kiểm tra trùng TRƯỚC khi gán vào b, nếu không Hibernate sẽ ghi thay đổi
    // xuống DB trước khi chạy câu kiểm tra và DB báo lỗi trùng thay vì câu thông báo dễ hiểu bên dưới.
    private void napDuLieu(SanPhamChiTiet b, SanPham sp, BienTheRequest req) {
        MauSac mauSac = ThuocTinhChon.chon(mauSacRepo, req.idMauSac(), b.getMauSac(), "màu sắc");
        TrongLuong trongLuong = ThuocTinhChon.chon(trongLuongRepo, req.idTrongLuong(), b.getTrongLuong(), "trọng lượng");
        ChuVi chuVi = ThuocTinhChon.chon(chuViRepo, req.idChuVi(), b.getChuVi(), "chu vi");

        // Một sản phẩm không có 2 biến thể cùng màu + trọng lượng + chu vi
        Integer idHienTai = b.getId() == null ? -1 : b.getId(); // thêm mới chưa có id nên dùng -1
        if (bienTheRepo.existsBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViIdAndIdNot(
                sp.getId(), mauSac.getId(), trongLuong.getId(), chuVi.getId(), idHienTai)) {
            throw ApiException.conflict("Sản phẩm đã có biến thể với màu sắc, trọng lượng và chu vi này.");
        }
        b.setSanPham(sp);
        b.setMauSac(mauSac);
        b.setTrongLuong(trongLuong);
        b.setChuVi(chuVi);

        loi(req.giaBan() == null, "Nhập giá bán.");
        loi(req.giaBan() < 1000, "Giá bán tối thiểu 1.000 ₫.");
        loi(req.giaBan() > 1_000_000_000L, "Giá bán tối đa 1.000.000.000 ₫.");
        b.setGiaBan(req.giaBan());

        loi(req.soLuongTon() == null, "Nhập số lượng tồn.");
        loi(req.soLuongTon() < 0, "Số lượng tồn là số nguyên từ 0 trở lên.");
        loi(req.soLuongTon() > 1_000_000, "Số lượng tồn tối đa 1.000.000.");
        b.setSoLuongTon(req.soLuongTon());
    }

    private SanPhamChiTiet timBienTheTrung(Integer idSanPham, Integer idMau, Integer idTl, Integer idCv) {
        if (idSanPham == null || idMau == null || idTl == null || idCv == null) {
            return null;
        }
        List<SanPhamChiTiet> ds = bienTheRepo.findBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViId(idSanPham, idMau, idTl, idCv);
        return ds.isEmpty() ? null : ds.get(0);
    }

    // Cập nhật biến thể đã có theo dữ liệu mới (giá bán, số lượng tồn); biến thể đang ngưng thì bật lại vì người dùng vừa chủ động thêm nó
    private SanPhamChiTiet capNhatBienTheDaCo(SanPhamChiTiet b, SanPham sp, BienTheRequest req) {
        napDuLieu(b, sp, req);
        b.setTrangThai(SanPhamChiTiet.HOAT_DONG);
        b.setNgayCapNhat(LocalDateTime.now());
        return bienTheRepo.save(b);
    }

    private static boolean laKhongApDung(String ten) {
        return ten != null && "Không áp dụng".equalsIgnoreCase(ten.trim());
    }

    // Mã tự sinh: <MÃ SP>-<MÀU>-<TRỌNG LƯỢNG>-<CHU VI>, thêm -2, -3... nếu bị trùng; tối đa 50 ký tự
    private String taoMaBienThe(SanPham sp, SanPhamChiTiet b) {
        String tl = b.getTrongLuong().getTen() == null ? "" : b.getTrongLuong().getTen().trim().split("\\s+")[0];
        String goc = sp.getMaSanPham().toUpperCase() + "-" + slug(b.getMauSac().getTen());
        // Danh mục không phải vợt không có trọng lượng / chu vi (được gán "Không áp dụng") nên không đưa vào mã
        if (!laKhongApDung(b.getTrongLuong().getTen())) goc += "-" + slug(tl);
        if (!laKhongApDung(b.getChuVi().getTen())) goc += "-" + slug(b.getChuVi().getTen());
        if (goc.length() > 46) {
            goc = goc.substring(0, 46);
        }
        String ma = goc;
        int so = 2;
        while (bienTheRepo.existsByMaSpctIgnoreCase(ma)) {
            ma = goc + "-" + so++;
        }
        return ma;
    }

    // "Xanh dương" -> "XANHDUONG" (bỏ dấu, chỉ giữ chữ và số)
    private static String slug(String s) {
        if (s == null) {
            return "";
        }
        String x = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D');
        return x.replaceAll("[^A-Za-z0-9]+", "").toUpperCase();
    }

    private BienTheResponse toResponse(SanPhamChiTiet b) {
        return new BienTheResponse(
                b.getId(),
                b.getMaSpct(),
                b.getSanPham().getId(),
                b.getMauSac().getId(),
                b.getTrongLuong().getId(),
                b.getChuVi().getId(),
                b.getGiaBan(),
                b.getSoLuongTon(),
                dangHoatDong(b));
    }
}
