package com.footstyle.demo.service;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        SanPhamChiTiet b = new SanPhamChiTiet();
        b.setTrangThai(SanPhamChiTiet.HOAT_DONG);
        b.setNgayTao(LocalDateTime.now());

        // Sản phẩm của biến thể: phải tồn tại và đang hoạt động
        loi(req.idSanPham() == null, "Chọn sản phẩm.");
        SanPham sp = sanPhamRepo.findById(req.idSanPham())
                .orElseThrow(() -> ApiException.badRequest("Sản phẩm không tồn tại."));
        loi(sp.getTrangThai() == null || sp.getTrangThai() != SanPham.HOAT_DONG,
                "Sản phẩm đã ngưng hoạt động, không thể thêm biến thể mới.");

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
