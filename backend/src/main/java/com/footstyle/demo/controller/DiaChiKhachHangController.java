package com.footstyle.demo.controller;

import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.service.QuyTacDuLieu;
import com.footstyle.demo.repository.DiaChiKhachHangRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.service.KhachHangRules;
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
    public DiaChiKhachHang addDiaChi(@PathVariable Integer idKhachHang, @RequestBody DiaChiKhachHang payload) {
        KhachHang kh = khachHangRepository.findById(idKhachHang)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng."));
        List<DiaChiKhachHang> hienCo = diaChiRepository.findByKhachHangId(idKhachHang);
        if (hienCo.size() >= KhachHangRules.SO_DIA_CHI_MAX) {
            throw ApiException.badRequest("Mỗi khách hàng chỉ lưu tối đa " + KhachHangRules.SO_DIA_CHI_MAX
                    + " địa chỉ. Hãy xóa bớt địa chỉ không dùng.");
        }

        DiaChiKhachHang diaChi = new DiaChiKhachHang();
        diaChi.setKhachHang(kh);
        ganDuLieuDaKiemTra(diaChi, payload, kh);
        kiemTraTrung(diaChi, hienCo, null);
        diaChi.setTrangThai(1);
        // Địa chỉ đầu tiên luôn là mặc định
        boolean macDinh = hienCo.isEmpty() || Boolean.TRUE.equals(payload.getLaDiaChiMacDinh());
        diaChi.setLaDiaChiMacDinh(macDinh);
        if (macDinh) boMacDinhCuaCacDiaChiKhac(idKhachHang, null);
        return diaChiRepository.save(diaChi);
    }

    @PutMapping("/{id}")
    @Transactional
    public DiaChiKhachHang updateDiaChi(@PathVariable Integer id, @RequestBody DiaChiKhachHang payload) {
        DiaChiKhachHang existing = timDiaChi(id);
        KhachHang kh = existing.getKhachHang();
        ganDuLieuDaKiemTra(existing, payload, kh);
        kiemTraTrung(existing, diaChiRepository.findByKhachHangId(kh.getId()), existing.getId());
        if (Boolean.TRUE.equals(payload.getLaDiaChiMacDinh())) {
            existing.setLaDiaChiMacDinh(true);
            boMacDinhCuaCacDiaChiKhac(kh.getId(), existing.getId());
        }
        // Bỏ chọn mặc định ở địa chỉ đang là mặc định không có ý nghĩa (khách phải luôn có 1 mặc định): giữ nguyên.
        return diaChiRepository.save(existing);
    }

    /** Chỉ đặt địa chỉ làm mặc định, không đụng tới các trường khác (địa chỉ cũ nhập thiếu vẫn đặt được). */
    @PutMapping("/{id}/mac-dinh")
    @Transactional
    public DiaChiKhachHang datMacDinh(@PathVariable Integer id) {
        DiaChiKhachHang dc = timDiaChi(id);
        dc.setLaDiaChiMacDinh(true);
        boMacDinhCuaCacDiaChiKhac(dc.getKhachHang().getId(), dc.getId());
        return diaChiRepository.save(dc);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> deleteDiaChi(@PathVariable Integer id) {
        DiaChiKhachHang dc = timDiaChi(id);
        if (Boolean.TRUE.equals(dc.getLaDiaChiMacDinh())) {
            throw ApiException.badRequest("Không thể xóa địa chỉ mặc định. Hãy đặt địa chỉ khác làm mặc định trước.");
        }
        // Địa chỉ nằm trong collection cascade ALL của khách hàng nên phải gỡ khỏi collection thì mới xóa được.
        dc.getKhachHang().getDiaChiList().remove(dc);
        diaChiRepository.delete(dc);
        diaChiRepository.flush();
        return Map.of("message", "Xóa thành công");
    }

    private DiaChiKhachHang timDiaChi(Integer id) {
        return diaChiRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy địa chỉ."));
    }

    /** Kiểm tra + chuẩn hóa dữ liệu gửi lên rồi gán vào địa chỉ. Tên / SĐT người nhận để trống thì lấy của khách hàng. */
    private void ganDuLieuDaKiemTra(DiaChiKhachHang dich, DiaChiKhachHang payload, KhachHang kh) {
        String ten = KhachHangRules.chuanHoaKhoangTrang(payload.getTenNguoiNhan());
        String sdt = KhachHangRules.chuanHoaSdt(payload.getSdtNguoiNhan());
        dich.setTenNguoiNhan(KhachHangRules.hoTen(ten != null ? ten : kh.getHoTen(), "Tên người nhận"));
        dich.setSdtNguoiNhan(KhachHangRules.sdt(sdt != null ? sdt : kh.getSdt(), "Số điện thoại người nhận"));
        dich.setTinhThanhPho(KhachHangRules.tinhThanh(payload.getTinhThanhPho()));
        dich.setPhuongXa(KhachHangRules.phuongXa(payload.getPhuongXa()));
        dich.setQuanHuyen(KhachHangRules.chuanHoaKhoangTrang(payload.getQuanHuyen()));
        dich.setDiaChiCuThe(KhachHangRules.diaChiCuThe(payload.getDiaChiCuThe()));
    }

    /** Cùng địa chỉ (tỉnh, phường, địa chỉ cụ thể - không phân biệt hoa thường) và cùng SĐT người nhận -> trùng. */
    private void kiemTraTrung(DiaChiKhachHang moi, List<DiaChiKhachHang> hienCo, Integer boQuaId) {
        for (DiaChiKhachHang d : hienCo) {
            if (boQuaId != null && boQuaId.equals(d.getId())) continue;
            if (bang(d.getTinhThanhPho(), moi.getTinhThanhPho()) && bang(d.getPhuongXa(), moi.getPhuongXa())
                    && bang(d.getDiaChiCuThe(), moi.getDiaChiCuThe())
                    && bang(KhachHangRules.chuanHoaSdt(d.getSdtNguoiNhan()), moi.getSdtNguoiNhan())) {
                throw ApiException.conflict("Địa chỉ này đã có trong sổ địa chỉ của khách hàng.");
            }
        }
    }

    private static boolean bang(String a, String b) {
        String x = KhachHangRules.chuanHoaKhoangTrang(a);
        String y = KhachHangRules.chuanHoaKhoangTrang(b);
        return x == null ? y == null : x.equalsIgnoreCase(y);
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

        if (d.getQuanHuyen() != null && d.getQuanHuyen().length() > 100) loi.them("quanHuyen", "Tên quận/huyện tối đa 100 ký tự.");
        loi.nemNeuCoLoi();
    }

    private static String gon(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }
}
