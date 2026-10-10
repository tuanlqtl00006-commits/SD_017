package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DiaChiKhachHangRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dia-chi")
@RequiredArgsConstructor
public class DiaChiKhachHangController {

    private final DiaChiKhachHangRepository diaChiRepository;
    private final KhachHangRepository khachHangRepository;

    @GetMapping("/khach-hang/{idKhachHang}")
    public List<DiaChiKhachHang> getDiaChiByKhachHang(@PathVariable Integer idKhachHang) {
        return diaChiRepository.findByKhachHangId(idKhachHang);
    }

    @PostMapping("/khach-hang/{idKhachHang}")
    @Transactional
    public DiaChiKhachHang addDiaChi(@PathVariable Integer idKhachHang, @RequestBody DiaChiKhachHang diaChi) {
        var kh = khachHangRepository.findById(idKhachHang)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng."));
        diaChi.setId(null);
        diaChi.setKhachHang(kh);
        if (diaChi.getTrangThai() == null) diaChi.setTrangThai(1);
        boolean laDauTien = diaChiRepository.findByKhachHangId(idKhachHang).isEmpty();
        if (laDauTien) diaChi.setLaDiaChiMacDinh(true);
        if (Boolean.TRUE.equals(diaChi.getLaDiaChiMacDinh())) boMacDinhCuaCacDiaChiKhac(idKhachHang, null);
        return diaChiRepository.save(diaChi);
    }

    @PutMapping("/{id}")
    @Transactional
    public DiaChiKhachHang updateDiaChi(@PathVariable Integer id, @RequestBody DiaChiKhachHang payload) {
        DiaChiKhachHang existing = diaChiRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ."));
        existing.setTenNguoiNhan(payload.getTenNguoiNhan());
        existing.setSdtNguoiNhan(payload.getSdtNguoiNhan());
        existing.setTinhThanhPho(payload.getTinhThanhPho());
        existing.setQuanHuyen(payload.getQuanHuyen());
        existing.setPhuongXa(payload.getPhuongXa());
        existing.setDiaChiCuThe(payload.getDiaChiCuThe());
        if (Boolean.TRUE.equals(payload.getLaDiaChiMacDinh())) {
            existing.setLaDiaChiMacDinh(true);
            boMacDinhCuaCacDiaChiKhac(existing.getKhachHang().getId(), existing.getId());
        }
        // Bỏ chọn mặc định ở địa chỉ đang là mặc định không có ý nghĩa (khách phải luôn có 1 mặc định): giữ nguyên.
        return diaChiRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> deleteDiaChi(@PathVariable Integer id) {
        DiaChiKhachHang dc = diaChiRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ."));
        Integer idKh = dc.getKhachHang().getId();
        boolean laMacDinh = Boolean.TRUE.equals(dc.getLaDiaChiMacDinh());
        // Địa chỉ nằm trong collection cascade ALL của khách hàng nên phải gỡ khỏi collection thì mới xóa được.
        dc.getKhachHang().getDiaChiList().remove(dc);
        diaChiRepository.delete(dc);
        diaChiRepository.flush();
        if (laMacDinh) {
            // Chuyển mặc định sang địa chỉ còn lại đầu tiên.
            diaChiRepository.findByKhachHangId(idKh).stream().findFirst().ifPresent(d -> {
                d.setLaDiaChiMacDinh(true);
                diaChiRepository.save(d);
            });
        }
        return Map.of("message", "Xóa thành công");
    }

    private void boMacDinhCuaCacDiaChiKhac(Integer idKhachHang, Integer giuLaiId) {
        for (DiaChiKhachHang other : diaChiRepository.findByKhachHangId(idKhachHang)) {
            if (giuLaiId != null && giuLaiId.equals(other.getId())) continue;
            if (Boolean.TRUE.equals(other.getLaDiaChiMacDinh())) {
                other.setLaDiaChiMacDinh(false);
                diaChiRepository.save(other);
            }
        }
    }
}
