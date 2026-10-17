package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.service.QuyTacDuLieu;
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

    /** Mỗi khách hàng lưu tối đa bấy nhiêu địa chỉ (tránh sổ địa chỉ phình to, giao diện chọn địa chỉ khó dùng). */
    public static final int SO_DIA_CHI_MAX = 10;
    public static final String LOI_TRUNG = "Địa chỉ này đã có trong sổ địa chỉ của khách hàng.";
    public static final String LOI_DIA_CHI_CHI_TIET_CHU_SO = "Địa chỉ cụ thể phải có chữ hoặc số (ví dụ: 12 Trần Phú).";

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
        chuanHoa(diaChi);
        List<DiaChiKhachHang> hienCo = diaChiRepository.findByKhachHangId(idKhachHang);
        kiemTraSoLuong(hienCo.size());
        kiemTraTrung(diaChi, hienCo, null);
        diaChi.setId(null);
        diaChi.setKhachHang(kh);
        if (diaChi.getTrangThai() == null) diaChi.setTrangThai(1);
        boolean laDauTien = hienCo.isEmpty();
        if (laDauTien) diaChi.setLaDiaChiMacDinh(true);
        if (Boolean.TRUE.equals(diaChi.getLaDiaChiMacDinh())) boMacDinhCuaCacDiaChiKhac(idKhachHang, null);
        return diaChiRepository.save(diaChi);
    }

    @PutMapping("/{id}")
    @Transactional
    public DiaChiKhachHang updateDiaChi(@PathVariable Integer id, @RequestBody DiaChiKhachHang payload) {
        DiaChiKhachHang existing = diaChiRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ."));
        chuanHoa(payload);
        kiemTraTrung(payload, diaChiRepository.findByKhachHangId(existing.getKhachHang().getId()), existing.getId());
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

    /**
     * Chỉ đặt địa chỉ này làm mặc định, không đụng tới các trường khác. Dùng cho nút "Đặt mặc định" ở danh sách:
     * địa chỉ cũ nhập thiếu thông tin vẫn đặt được (gọi PUT đầy đủ sẽ bị chặn vì thiếu dữ liệu).
     */
    @PutMapping("/{id}/mac-dinh")
    @Transactional
    public DiaChiKhachHang datMacDinh(@PathVariable Integer id) {
        DiaChiKhachHang dc = diaChiRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ."));
        dc.setLaDiaChiMacDinh(true);
        boMacDinhCuaCacDiaChiKhac(dc.getKhachHang().getId(), dc.getId());
        return diaChiRepository.save(dc);
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

    /**
     * Bỏ khoảng trắng thừa rồi kiểm tra TỪNG Ô (lỗi trả về theo ô để giao diện hiện ngay dưới ô đó, xem LoiForm):
     * tên người nhận (chỉ chữ cái), SĐT (10 số, đầu số 03/05/07/08/09), tỉnh/thành phố, phường/xã, địa chỉ cụ thể đều bắt buộc
     * (giống form ở giao diện; chặn cả trường hợp gọi thẳng API hoặc chỉ gõ dấu cách).
     */
    static void chuanHoa(DiaChiKhachHang d) {
        if (d == null) throw ApiException.badRequest("Thiếu thông tin địa chỉ.");
        d.setTenNguoiNhan(gon(d.getTenNguoiNhan()));
        d.setSdtNguoiNhan(gon(d.getSdtNguoiNhan()));
        d.setTinhThanhPho(gon(d.getTinhThanhPho()));
        d.setQuanHuyen(gon(d.getQuanHuyen()));
        d.setPhuongXa(gon(d.getPhuongXa()));
        d.setDiaChiCuThe(gon(d.getDiaChiCuThe()));

        LoiForm loi = new LoiForm();
        String ten = d.getTenNguoiNhan();
        if (ten == null) loi.them("tenNguoiNhan", "Vui lòng nhập tên người nhận.");
        else if (ten.length() < 2 || ten.length() > 100) loi.them("tenNguoiNhan", "Họ tên từ 2 đến 100 ký tự.");
        else if (!QuyTacDuLieu.HO_TEN.matcher(ten).matches()) loi.them("tenNguoiNhan", QuyTacDuLieu.LOI_HO_TEN_KY_TU);

        String sdt = d.getSdtNguoiNhan();
        if (sdt == null) loi.them("sdtNguoiNhan", "Vui lòng nhập số điện thoại người nhận.");
        else if (!QuyTacDuLieu.SDT.matcher(sdt).matches()) loi.them("sdtNguoiNhan", QuyTacDuLieu.LOI_SDT);

        if (d.getTinhThanhPho() == null) loi.them("tinhThanhPho", "Vui lòng chọn Tỉnh/Thành phố.");
        else if (d.getTinhThanhPho().length() > 100) loi.them("tinhThanhPho", "Tên tỉnh/thành phố tối đa 100 ký tự.");

        if (d.getPhuongXa() == null) loi.them("phuongXa", "Vui lòng chọn Phường/Xã.");
        else if (d.getPhuongXa().length() > 100) loi.them("phuongXa", "Tên phường/xã tối đa 100 ký tự.");

        if (d.getDiaChiCuThe() == null) loi.them("diaChiCuThe", "Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).");
        else if (d.getDiaChiCuThe().length() > 255) loi.them("diaChiCuThe", "Địa chỉ cụ thể tối đa 255 ký tự.");
        else if (d.getDiaChiCuThe().codePoints().noneMatch(Character::isLetterOrDigit)) loi.them("diaChiCuThe", LOI_DIA_CHI_CHI_TIET_CHU_SO);

        if (d.getQuanHuyen() != null && d.getQuanHuyen().length() > 100) loi.them("quanHuyen", "Tên quận/huyện tối đa 100 ký tự.");
        loi.nemNeuCoLoi();
    }

    /** Đã đủ số địa chỉ tối đa thì không cho thêm nữa. */
    static void kiemTraSoLuong(int soDiaChiHienCo) {
        if (soDiaChiHienCo >= SO_DIA_CHI_MAX) {
            throw ApiException.badRequest("Mỗi khách hàng chỉ lưu tối đa " + SO_DIA_CHI_MAX
                    + " địa chỉ. Hãy xóa bớt địa chỉ không dùng.");
        }
    }

    /**
     * Cùng Tỉnh/Thành phố + Phường/Xã + địa chỉ cụ thể (không phân biệt hoa thường) + cùng SĐT người nhận với một địa chỉ khác
     * của khách thì coi là trùng (409). boQuaId = id địa chỉ đang sửa (không tính là trùng với chính nó), null khi thêm mới.
     * Gọi sau chuanHoa nên các chuỗi đã bỏ khoảng trắng thừa.
     */
    static void kiemTraTrung(DiaChiKhachHang moi, List<DiaChiKhachHang> hienCo, Integer boQuaId) {
        for (DiaChiKhachHang d : hienCo) {
            if (boQuaId != null && boQuaId.equals(d.getId())) continue;
            if (bang(d.getTinhThanhPho(), moi.getTinhThanhPho()) && bang(d.getPhuongXa(), moi.getPhuongXa())
                    && bang(d.getDiaChiCuThe(), moi.getDiaChiCuThe()) && bang(d.getSdtNguoiNhan(), moi.getSdtNguoiNhan())) {
                throw ApiException.conflict(LOI_TRUNG);
            }
        }
    }

    private static boolean bang(String a, String b) {
        String x = gon(a);
        String y = gon(b);
        return x == null ? y == null : x.equalsIgnoreCase(y);
    }

    private static String gon(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
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
