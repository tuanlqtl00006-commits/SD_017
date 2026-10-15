package com.footstyle.demo.controller;

import com.footstyle.demo.dto.DotGiamGiaRequest;
import com.footstyle.demo.dto.GiamGiaApDungResponse;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.service.DotCapNhatKeHoach;
import com.footstyle.demo.service.GiamGiaApDungService;
import com.footstyle.demo.service.LichGiamGiaService;
import com.footstyle.demo.service.MaUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Quản lý đợt giảm giá. Hệ thống không xóa cứng dữ liệu: nút nguồn chỉ bật / tắt cột trang_thai
 * (1 = đang hoạt động, 0 = ngừng hoạt động). Danh sách luôn trả bản ghi mới nhất lên đầu.
 */
@RestController
@RequestMapping("/api/dot-giam-gia")
@RequiredArgsConstructor
public class DotGiamGiaController {


    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final DotGiamGiaChiTietRepository chiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final LichGiamGiaService lichGiamGiaService;
    private final GiamGiaApDungService giamGiaApDungService; // chế độ gộp khi đợt chồng nhau (MAX / TRUNG_BINH)

    /** Mới nhất lên đầu (đợt vừa thêm nằm trên cùng). */
    @GetMapping
    public List<DotGiamGia> getAll() {
        return dotGiamGiaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /** Mã đợt do hệ thống sinh, dạng ngắn DGG + số thứ tự (DGG09, DGG100), không trùng mã đã có. */
    @GetMapping("/ma-moi")
    public Map<String, String> maMoi() {
        return Map.of("ma", sinhMa());
    }

    /**
     * Mức giảm của từng biến thể theo các đợt đang bật và CHƯA kết thúc (đang diễn ra + sắp diễn ra).
     * Trang Thêm / Sửa đợt dùng để xem trước lịch giảm giá khi đợt đang soạn chồng với các đợt khác;
     * "chinhSach" (MAX / TRUNG_BINH) là cách gộp đang cấu hình ở backend.
     */
    @GetMapping("/ap-dung")
    @Transactional(readOnly = true)
    public GiamGiaApDungResponse apDung() {
        List<GiamGiaApDungResponse.Muc> muc = new ArrayList<>();
        for (DotGiamGiaChiTiet ct : chiTietRepository.findApDung(LocalDateTime.now())) {
            DotGiamGia d = ct.getDotGiamGia();
            muc.add(new GiamGiaApDungResponse.Muc(ct.getIdSanPhamChiTiet(), d.getId(), d.getMaDot(), d.getTenDot(),
                    ct.getPhanTramGiamBienThe(), d.getNgayBatDau().toLocalDate(), d.getNgayKetThuc().toLocalDate()));
        }
        return new GiamGiaApDungResponse(giamGiaApDungService.cheDo().name(), muc);
    }

    /** Đọc hoặc đổi cách tính khi biến thể nằm trong nhiều đợt chồng ngày nhau: MAX (lấy mức cao nhất) hoặc TRUNG_BINH (trung bình cộng). */
    @GetMapping("/che-do-chong-dot")
    public Map<String, String> layCheDoChongDot() {
        return Map.of("cheDo", giamGiaApDungService.cheDo().name());
    }

    @PutMapping("/che-do-chong-dot")
    public Map<String, String> doiCheDoChongDot(@RequestBody(required = false) Map<String, String> body) {
        String raw = body == null ? null : body.getOrDefault("cheDo", body.get("chinhSach"));
        return Map.of("cheDo", giamGiaApDungService.doiCheDo(raw).name());
    }

    /** Mức giảm đang áp dụng HÔM NAY của từng biến thể: { "idBienThe": phần trăm }. Nhiều đợt chồng nhau thì gộp theo MAX / TRUNG_BINH (application.properties). */
    @GetMapping("/ap-dung-hom-nay")
    public Map<Long, Double> apDungHomNay() {
        return lichGiamGiaService.giamHomNay();
    }

    /** Lịch giảm giá theo khoảng ngày của từng biến thể trong đợt (có tính các đợt khác chồng lên). */
    @GetMapping("/{id}/lich-giam-gia")
    public List<LichGiamGiaService.LichBienThe> lichGiamGia(@PathVariable Long id) {
        return lichGiamGiaService.lichCuaDot(id);
    }

    @GetMapping("/{id}")
    public DotGiamGia getById(@PathVariable Long id) {
        return dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt giảm giá."));
    }

    /** Thêm đợt giảm giá cùng các biến thể áp dụng trong một lần lưu (cùng thành công hoặc cùng hủy). */
    @PostMapping
    @Transactional
    public DotGiamGia create(@RequestBody DotGiamGiaRequest req) {
        validate(req);
        if (req.idBienThe() == null || req.idBienThe().isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn ít nhất một biến thể sản phẩm áp dụng.");
        }
        String ma = req.maDot() == null ? "" : req.maDot().trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty()) {
            ma = sinhMa();
        } else if (dotGiamGiaRepository.existsByMaDot(ma)) {
            throw ApiException.conflict("Mã đợt giảm giá đã tồn tại. Hãy bấm làm mới để lấy mã khác.");
        }

        DotGiamGia dot = new DotGiamGia();
        dot.setMaDot(ma);
        apDungThongTin(dot, req);
        dot.setTrangThai(req.trangThai() == null ? 1 : req.trangThai());
        dot = dotGiamGiaRepository.save(dot);

        dongBoBienThe(dot, req.idBienThe(), true);
        return dot;
    }

    /**
     * Sửa đợt giảm giá (không đổi mã). Có gửi idBienThe thì đồng bộ luôn danh sách biến thể áp dụng.
     * Xử lý theo switch-case tương tự Phiếu giảm giá (xem DotCapNhatKeHoach, không gửi mail):
     *  - TH1: đổi thông tin đợt (% giảm), danh sách biến thể giữ nguyên -> cập nhật lại % giảm cho mọi biến thể trong đợt.
     *  - TH2: thông tin giữ nguyên, danh sách biến thể thay đổi       -> biến thể mới: thêm; biến thể bị bỏ: gỡ; biến thể giữ lại: giữ nguyên mức giảm riêng.
     *  - TH3: đổi cả hai                                             -> biến thể mới: thêm; biến thể bị bỏ: gỡ; biến thể giữ lại: cập nhật % theo đợt.
     */
    @PutMapping("/{id}")
    @Transactional
    public DotGiamGia update(@PathVariable Long id, @RequestBody DotGiamGiaRequest req) {
        DotGiamGia existing = getById(id);
        validate(req);
        if (req.idBienThe() != null && req.idBienThe().isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn ít nhất một biến thể sản phẩm áp dụng.");
        }
        Integer phanTramCu = existing.getPhanTramGiamDot();
        apDungThongTin(existing, req);
        if (req.trangThai() != null) existing.setTrangThai(req.trangThai());
        DotGiamGia saved = dotGiamGiaRepository.save(existing);
        boolean doiPhanTram = !Objects.equals(phanTramCu, saved.getPhanTramGiamDot());

        if (req.idBienThe() != null) {
            dongBoBienThe(saved, req.idBienThe(), doiPhanTram);
        } else if (doiPhanTram) {
            // Không gửi danh sách biến thể (TH1): giữ nguyên biến thể, chỉ cập nhật lại % giảm cho khớp đợt.
            for (DotGiamGiaChiTiet ct : chiTietRepository.findByDotGiamGiaId(saved.getId())) {
                ct.setPhanTramGiamBienThe(saved.getPhanTramGiamDot());
                chiTietRepository.save(ct);
            }
        }
        return saved;
    }

    /**
     * Bật / tắt hoạt động đợt giảm giá (1: đang hoạt động, 0: ngừng hoạt động). Không xóa dữ liệu.
     * Đợt đã qua ngày kết thúc vẫn bật / tắt được (công tắc ở giao diện luôn hiện; tắt = \"Ngừng hoạt động\").
     */
    @PutMapping("/{id}/trang-thai")
    public DotGiamGia toggleStatus(@PathVariable Long id) {
        DotGiamGia existing = getById(id);
        existing.setTrangThai(Integer.valueOf(1).equals(existing.getTrangThai()) ? 0 : 1);
        return dotGiamGiaRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        if (!dotGiamGiaRepository.existsById(id)) throw ApiException.notFound("Không tìm thấy đợt giảm giá.");
        chiTietRepository.deleteByDotGiamGiaId(id);
        dotGiamGiaRepository.deleteById(id);
    }

    /* ---------------- hàm phụ ---------------- */

    private String sinhMa() {
        // Mã ngắn theo số thứ tự: DGG01, DGG02... (lấy số lớn nhất đang có + 1, đọc được cả mã cũ DGG001), không trùng mã đã có
        int max = 0;
        for (DotGiamGia d : dotGiamGiaRepository.findAll()) {
            String m = d.getMaDot() == null ? "" : d.getMaDot().trim().toUpperCase(Locale.ROOT);
            if (m.matches("DGG\\d{1,6}")) max = Math.max(max, Integer.parseInt(m.substring(3)));
        }
        String ma;
        do {
            max++;
            ma = MaUtil.dinhDang("DGG", max);
        } while (dotGiamGiaRepository.existsByMaDot(ma));
        return ma;
    }

    private void apDungThongTin(DotGiamGia dot, DotGiamGiaRequest req) {
        dot.setTenDot(req.tenDot().trim());
        dot.setPhanTramGiamDot(req.phanTramGiamDot());
        dot.setNgayBatDau(docThoiDiem(req.ngayBatDau(), false));
        dot.setNgayKetThuc(docThoiDiem(req.ngayKetThuc(), true));
        String moTa = req.moTa() == null ? "" : req.moTa().trim();
        dot.setMoTa(moTa.isEmpty() ? null : moTa);
    }

    /**
     * Đọc ngày giờ từ chuỗi gửi lên. "yyyy-MM-dd" -> đầu ngày (00:00:00) hoặc cuối ngày (23:59:59);
     * "yyyy-MM-ddTHH:mm:ss" -> đúng thời điểm đó.
     */
    private LocalDateTime docThoiDiem(String s, boolean cuoiNgay) {
        if (s == null || s.trim().length() < 10) {
            throw ApiException.badRequest("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
        }
        String v = s.trim();
        try {
            if (v.length() == 10) {
                LocalDate ngay = LocalDate.parse(v);
                return cuoiNgay ? ngay.atTime(LocalTime.of(23, 59, 59)) : ngay.atStartOfDay();
            }
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Ngày không hợp lệ.");
        }
    }

    /**
     * Đồng bộ biến thể áp dụng của đợt theo kế hoạch switch-case (DotCapNhatKeHoach):
     *  - Biến thể trong goBo: xóa khỏi đợt.
     *  - Biến thể trong capNhat (TH1, TH3): đặt lại % giảm bằng % mới của đợt.
     *  - Biến thể trong giuNguyen (TH2, KHONG_DOI): giữ nguyên mức giảm riêng.
     *  - Biến thể trong moi (TH2, TH3): kiểm tra đang bán rồi thêm vào đợt với % giảm của đợt.
     */
    private void dongBoBienThe(DotGiamGia dot, List<Long> idMoi, boolean ghiDePhanTram) {
        Set<Long> moi = new LinkedHashSet<>();
        for (Long id : idMoi) {
            if (id == null) throw ApiException.badRequest("Danh sách biến thể áp dụng không hợp lệ.");
            moi.add(id);
        }

        List<DotGiamGiaChiTiet> hienCo = chiTietRepository.findByDotGiamGiaId(dot.getId());
        Map<Long, DotGiamGiaChiTiet> theoIdBt = new HashMap<>();
        List<Long> idCu = new ArrayList<>();
        for (DotGiamGiaChiTiet ct : hienCo) {
            idCu.add(ct.getIdSanPhamChiTiet());
            theoIdBt.put(ct.getIdSanPhamChiTiet(), ct);
        }

        DotCapNhatKeHoach.KeHoach keHoach = DotCapNhatKeHoach.lap(idCu, moi, ghiDePhanTram);

        if (!keHoach.goBo().isEmpty()) {
            Set<Long> canXoa = new HashSet<>(keHoach.goBo());
            List<DotGiamGiaChiTiet> dongXoa = new ArrayList<>();
            for (DotGiamGiaChiTiet ct : hienCo) {
                if (canXoa.contains(ct.getIdSanPhamChiTiet())) dongXoa.add(ct);
            }
            chiTietRepository.deleteAll(dongXoa);
        }

        for (Long idBienThe : keHoach.capNhat()) {
            DotGiamGiaChiTiet ct = theoIdBt.get(idBienThe);
            if (ct != null && !dot.getPhanTramGiamDot().equals(ct.getPhanTramGiamBienThe())) {
                ct.setPhanTramGiamBienThe(dot.getPhanTramGiamDot());
                chiTietRepository.save(ct);
            }
        }

        for (Long idBienThe : keHoach.moi()) {
            SanPhamChiTiet bienThe = sanPhamChiTietRepository.findById(idBienThe.intValue())
                    .orElseThrow(() -> ApiException.badRequest("Có biến thể được chọn không còn tồn tại."));
            if (!Integer.valueOf(SanPhamChiTiet.HOAT_DONG).equals(bienThe.getTrangThai())) {
                throw ApiException.badRequest("Biến thể " + bienThe.getMaSpct() + " đang ngừng bán nên không thể áp dụng giảm giá.");
            }
            DotGiamGiaChiTiet ct = new DotGiamGiaChiTiet();
            ct.setDotGiamGia(dot);
            ct.setIdSanPhamChiTiet(idBienThe);
            ct.setPhanTramGiamBienThe(dot.getPhanTramGiamDot());
            ct.setTrangThai(1);
            chiTietRepository.save(ct);
        }
    }

    private void validate(DotGiamGiaRequest d) {
        if (d == null || d.tenDot() == null || d.tenDot().trim().isEmpty()) {
            throw ApiException.badRequest("Vui lòng nhập tên đợt giảm giá.");
        }
        if (d.tenDot().trim().length() > 255) {
            throw ApiException.badRequest("Tên đợt giảm giá tối đa 255 ký tự.");
        }
        if (d.moTa() != null && d.moTa().trim().length() > 500) {
            throw ApiException.badRequest("Mô tả tối đa 500 ký tự.");
        }
        Integer pt = d.phanTramGiamDot();
        if (pt == null || pt < 1 || pt > 100) {
            throw ApiException.badRequest("Giá trị giảm phải từ 1 đến 100 (%).");
        }
        LocalDateTime bd = docThoiDiem(d.ngayBatDau(), false);
        LocalDateTime kt = docThoiDiem(d.ngayKetThuc(), true);
        if (kt.isBefore(bd)) {
            throw ApiException.badRequest("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
    }
}
