package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DiaChiKhachHangRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Sổ địa chỉ của CHÍNH khách hàng đang đăng nhập (trang "Tài khoản" ở cửa hàng online).
 * Tài khoản khách không được dùng /api/dia-chi (của nhân viên) nên có đường riêng /api/khach/dia-chi,
 * luôn lấy id khách từ phiên đăng nhập - khách không thể xem/sửa địa chỉ của người khác.
 * Dùng lại cùng quy tắc kiểm tra dữ liệu với DiaChiKhachHangController.
 */
@RestController
@RequestMapping("/api/khach/dia-chi")
@RequiredArgsConstructor
public class KhachDiaChiController {

    private final DiaChiKhachHangRepository diaChiRepository;
    private final KhachHangRepository khachHangRepository;

    private Integer idKhachHienTai() {
        NguoiDung nd = XacThuc.hienTai()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập.", null, null, null));
        if (!nd.laKhachHang()) throw ApiException.badRequest("Chỉ tài khoản khách hàng mới dùng được sổ địa chỉ này.");
        return nd.id();
    }

    private DiaChiKhachHang timCuaToi(Integer id, Integer idKhach) {
        DiaChiKhachHang dc = diaChiRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ."));
        if (dc.getKhachHang() == null || !idKhach.equals(dc.getKhachHang().getId())) {
            throw ApiException.notFound("Không tìm thấy địa chỉ.");
        }
        return dc;
    }

    @GetMapping
    public List<DiaChiKhachHang> danhSach() {
        return diaChiRepository.findByKhachHangId(idKhachHienTai());
    }

    @PostMapping
    @Transactional
    public DiaChiKhachHang them(@RequestBody DiaChiKhachHang diaChi) {
        Integer idKhach = idKhachHienTai();
        var kh = khachHangRepository.findById(idKhach)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng."));
        DiaChiKhachHangController.chuanHoa(diaChi);
        List<DiaChiKhachHang> hienCo = diaChiRepository.findByKhachHangId(idKhach);
        DiaChiKhachHangController.kiemTraSoLuong(hienCo.size());
        DiaChiKhachHangController.kiemTraTrung(diaChi, hienCo, null);
        diaChi.setId(null);
        diaChi.setKhachHang(kh);
        if (diaChi.getTrangThai() == null) diaChi.setTrangThai(1);
        if (hienCo.isEmpty()) diaChi.setLaDiaChiMacDinh(true);
        if (Boolean.TRUE.equals(diaChi.getLaDiaChiMacDinh())) boMacDinhCuaCacDiaChiKhac(idKhach, null);
        return diaChiRepository.save(diaChi);
    }

    @PutMapping("/{id}/mac-dinh")
    @Transactional
    public DiaChiKhachHang datMacDinh(@PathVariable Integer id) {
        Integer idKhach = idKhachHienTai();
        DiaChiKhachHang dc = timCuaToi(id, idKhach);
        dc.setLaDiaChiMacDinh(true);
        boMacDinhCuaCacDiaChiKhac(idKhach, dc.getId());
        return diaChiRepository.save(dc);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> xoa(@PathVariable Integer id) {
        Integer idKhach = idKhachHienTai();
        DiaChiKhachHang dc = timCuaToi(id, idKhach);
        boolean laMacDinh = Boolean.TRUE.equals(dc.getLaDiaChiMacDinh());
        dc.getKhachHang().getDiaChiList().remove(dc);
        diaChiRepository.delete(dc);
        diaChiRepository.flush();
        if (laMacDinh) {
            diaChiRepository.findByKhachHangId(idKhach).stream().findFirst().ifPresent(d -> {
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
