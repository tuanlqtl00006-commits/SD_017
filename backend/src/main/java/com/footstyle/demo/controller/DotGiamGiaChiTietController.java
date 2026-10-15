package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dot-giam-gia-chi-tiet")
@RequiredArgsConstructor
public class DotGiamGiaChiTietController {

    private final DotGiamGiaChiTietRepository chiTietRepository;
    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    @GetMapping("/campaign/{campaignId}")
    public List<DotGiamGiaChiTiet> getByCampaign(@PathVariable Long campaignId) {
        return chiTietRepository.findByDotGiamGiaId(campaignId);
    }

    @PostMapping("/campaign/{campaignId}")
    public DotGiamGiaChiTiet addProductToCampaign(@PathVariable Long campaignId, @RequestBody DotGiamGiaChiTiet chiTiet) {
        DotGiamGia campaign = dotGiamGiaRepository.findById(campaignId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt giảm giá."));
        // Đợt đã qua ngày kết thúc thì thêm biến thể cũng không bao giờ được giảm (giống công tắc bật / tắt: so theo NGÀY)
        if (campaign.getNgayKetThuc() != null && campaign.getNgayKetThuc().toLocalDate().isBefore(LocalDate.now())) {
            throw ApiException.conflict("Đợt giảm giá đã kết thúc, không thể thêm sản phẩm.");
        }
        if (chiTiet.getIdSanPhamChiTiet() == null) {
            throw ApiException.badRequest("Vui lòng chọn biến thể sản phẩm hợp lệ.");
        }
        SanPhamChiTiet bienThe = sanPhamChiTietRepository.findById(chiTiet.getIdSanPhamChiTiet().intValue())
                .orElseThrow(() -> ApiException.badRequest("Vui lòng chọn biến thể sản phẩm hợp lệ."));
        // Giống trang Thêm / Sửa đợt: biến thể đang ngừng bán thì không áp dụng giảm giá
        if (!Integer.valueOf(SanPhamChiTiet.HOAT_DONG).equals(bienThe.getTrangThai())) {
            throw ApiException.badRequest("Biến thể " + bienThe.getMaSpct() + " đang ngừng bán nên không thể áp dụng giảm giá.");
        }
        Integer percent = chiTiet.getPhanTramGiamBienThe();
        if (percent == null || percent < 1 || percent > 100) {
            throw ApiException.badRequest("Phần trăm giảm phải từ 1 đến 100.");
        }
        if (chiTietRepository.existsByDotGiamGiaIdAndIdSanPhamChiTiet(campaignId, chiTiet.getIdSanPhamChiTiet())) {
            throw ApiException.conflict("Sản phẩm này đã tồn tại trong đợt giảm giá.");
        }
        chiTiet.setId(null);
        chiTiet.setDotGiamGia(campaign);
        if (chiTiet.getTrangThai() == null) chiTiet.setTrangThai(1);
        return chiTietRepository.save(chiTiet);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!chiTietRepository.existsById(id)) throw ApiException.notFound("Không tìm thấy chi tiết đợt giảm giá.");
        chiTietRepository.deleteById(id);
    }
}
