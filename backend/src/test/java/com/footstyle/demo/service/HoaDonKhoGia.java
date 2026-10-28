package com.footstyle.demo.service;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.repository.HinhAnhSanPhamRepository;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;

/**
 * Bộ kho giả cho HoaDonService dùng chung cho các test hóa đơn: MỘT hóa đơn (id = 1) cùng các danh sách dòng / thanh toán / lịch sử
 * để test sửa trước rồi kiểm tra sau khi gọi service. Không cần CSDL.
 */
final class HoaDonKhoGia {

    final HoaDon hd = new HoaDon();
    final List<HoaDonChiTiet> dong = new ArrayList<>();
    final List<LichSuThanhToan> thanhToan = new ArrayList<>();
    final List<LichSuHoaDon> lichSu = new ArrayList<>();
    final List<SanPhamChiTiet> daLuuBienThe = new ArrayList<>();
    /** Phiếu cá nhân đã tặng cho khách (bảng phieu_giam_gia_khach_hang). */
    final List<PhieuGiamGiaKhachHang> phieuKhach = new ArrayList<>();
    /** Số đơn KHÁC (ngoài hd) chưa hủy của cùng khách đang dùng cùng phiếu. */
    long donKhacDungPhieu = 0;
    final HoaDonService service;

    HoaDonKhoGia() {
        hd.setId(1);
        hd.setMaHoaDon("HD01");
        hd.setLoaiDon(HoaDon.GIAO_HANG);
        hd.setTrangThai(HoaDon.CHO_XAC_NHAN);
        hd.setTongTien(new BigDecimal("500000"));
        hd.setSoTienGiam(BigDecimal.ZERO);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setThanhTien(new BigDecimal("500000"));

        HoaDonRepository hdRepo = KhoGia.tao(HoaDonRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> Integer.valueOf(1).equals(a[0]) ? Optional.of(hd) : Optional.empty();
            case "findFirstByMaHoaDonIgnoreCase" -> a[0] != null && hd.getMaHoaDon() != null && String.valueOf(a[0]).equalsIgnoreCase(hd.getMaHoaDon()) ? Optional.of(hd) : Optional.empty();
            case "save" -> a[0];
            case "findAll" -> new PageImpl<>(List.of(hd));
            case "count" -> 0L;
            case "countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot" -> donKhacDungPhieu
                    + (hd.getKhachHang() != null && hd.getKhachHang().getId().equals(a[0]) && hd.getPhieuGiamGia() != null
                    && hd.getPhieuGiamGia().getId().equals(a[1]) && !a[2].equals(hd.getTrangThai()) ? 1 : 0);
            default -> KhoGia.KHONG_CO;
        });
        HoaDonChiTietRepository ctRepo = KhoGia.tao(HoaDonChiTietRepository.class, (ten, a) ->
                "findByHoaDon_IdOrderByIdAsc".equals(ten) ? dong : KhoGia.KHONG_CO);
        LichSuHoaDonRepository lsRepo = KhoGia.tao(LichSuHoaDonRepository.class, (ten, a) -> switch (ten) {
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> lichSu;
            case "save" -> {
                lichSu.add((LichSuHoaDon) a[0]);
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        LichSuThanhToanRepository ttRepo = KhoGia.tao(LichSuThanhToanRepository.class, (ten, a) -> switch (ten) {
            case "findByHoaDon_IdOrderByThoiGianAscIdAsc" -> new ArrayList<>(thanhToan);
            case "save" -> {
                thanhToan.add((LichSuThanhToan) a[0]);
                yield a[0];
            }
            default -> KhoGia.KHONG_CO;
        });
        HinhAnhSanPhamRepository anhRepo = KhoGia.tao(HinhAnhSanPhamRepository.class, (ten, a) ->
                "findByTrangThaiOrderByLaAnhChinhDescIdAsc".equals(ten) ? List.of() : KhoGia.KHONG_CO);
        SanPhamChiTietRepository spctRepo = KhoGia.tao(SanPhamChiTietRepository.class, (ten, a) -> {
            if ("save".equals(ten)) {
                daLuuBienThe.add((SanPhamChiTiet) a[0]);
                return a[0];
            }
            return KhoGia.KHONG_CO;
        });
        // Giống câu UPDATE có điều kiện: chỉ giảm khi lượt đã dùng > 0
        PhieuGiamGiaRepository phieuRepo = KhoGia.tao(PhieuGiamGiaRepository.class, (ten, a) -> {
            if (!"giamLuotDung".equals(ten)) return KhoGia.KHONG_CO;
            PhieuGiamGia p = hd.getPhieuGiamGia();
            if (p == null || !p.getId().equals(a[0]) || p.getSoLuongDaDung() == null || p.getSoLuongDaDung() <= 0) return 0;
            p.setSoLuongDaDung(p.getSoLuongDaDung() - 1);
            return 1;
        });
        PhieuGiamGiaKhachHangRepository phieuKhachRepo = KhoGia.tao(PhieuGiamGiaKhachHangRepository.class, (ten, a) -> switch (ten) {
            case "findByIdKhachHang" -> phieuKhach.stream().filter(x -> x.getIdKhachHang().equals(a[0])).toList();
            case "save" -> a[0];
            default -> KhoGia.KHONG_CO;
        });
        service = new HoaDonService(hdRepo, ctRepo, lsRepo, ttRepo, anhRepo, spctRepo, phieuRepo, phieuKhachRepo);
    }

    SanPhamChiTiet bienThe(int id, int ton) {
        SanPhamChiTiet b = new SanPhamChiTiet();
        b.setId(id);
        b.setMaSpct("SP01-DEN-" + id);
        b.setSoLuongTon(ton);
        return b;
    }

    void themDong(SanPhamChiTiet b, int soLuong) {
        HoaDonChiTiet ct = new HoaDonChiTiet();
        ct.setId(dong.size() + 1);
        ct.setHoaDon(hd);
        ct.setSanPhamChiTiet(b);
        ct.setSoLuong(soLuong);
        ct.setDonGia(new BigDecimal("100000"));
        dong.add(ct);
    }
}
