package com.footstyle.demo.service;

import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import com.footstyle.demo.repository.LichSuThanhToanRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Case khó của bán hàng online (bảng cô gửi): khách KHÔNG tự sửa đơn; nhân viên gọi xác nhận rồi sửa giúp, mỗi đơn chỉ sửa MỘT lần,
 * và chỉ khi đơn giao hàng còn "Chờ xác nhận" (chưa trừ kho).
 *  2. Sửa người nhận / địa chỉ -> tính lại phí giao hàng, chênh lệch thành HOÀN PHÍ (trả lại khách) hoặc PHỤ PHÍ (khách trả thêm).
 *  3. Thêm / sửa / xóa sản phẩm -> tính lại tiền hàng, phiếu giảm giá, phí ship -> hoàn phí / phụ phí.
 *  4. Thêm lại cùng sản phẩm mà giá đã đổi (đợt giảm giá) -> DÒNG MỚI theo giá mới; dòng cũ giữ giá lúc khách đặt.
 *  (1. Hủy đơn đã trả trước -> hoàn phí: HoaDonService.doiTrangThai.)
 * Hoàn phí ghi vào lich_su_thanh_toan với số tiền ÂM; phụ phí thu khi giao hàng (bước Hoàn thành ghi phần còn lại).
 */
@Service
@RequiredArgsConstructor
public class HoaDonSuaService {

    public static final String HANH_DONG_SUA = "Chỉnh sửa đơn hàng";
    /** Đơn online khách chọn chuyển khoản (id phương thức 2 - "Chuyển khoản ngân hàng"). */
    public static final Integer CHUYEN_KHOAN_ONLINE = 2;
    static final int SO_LUONG_TOI_DA = 999;

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final LichSuHoaDonRepository lichSuRepo;
    private final LichSuThanhToanRepository thanhToanRepo;
    private final GiamGiaApDungService giamGiaService;
    private final HoaDonService hoaDonService;

    private Clock dongHo = Clock.systemDefaultZone();

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
    }

    /** Một dòng sau khi sửa: idChiTiet = dòng đang có (null = thêm mới), idBienThe, soLuong. */
    public record DongSua(Integer idChiTiet, Integer idBienThe, Integer soLuong) {
    }

    public record SuaDonRequest(String hoTenNguoiNhan, String sdtNguoiNhan, String diaChiGiaoHang, List<DongSua> items, String lyDo) {
    }

    public record XacNhanThanhToanRequest(String maGiaoDich) {
    }

    @Transactional
    public Map<String, Object> suaDon(Integer id, SuaDonRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hóa đơn."));
        if (!Integer.valueOf(HoaDon.GIAO_HANG).equals(hd.getLoaiDon()) || !Integer.valueOf(HoaDon.CHO_XAC_NHAN).equals(hd.getTrangThai())) {
            throw ApiException.conflict("Chỉ sửa được đơn giao hàng đang \"Chờ xác nhận\".");
        }
        List<LichSuHoaDon> lichSu = lichSuRepo.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId());
        if (lichSu.stream().anyMatch(l -> HANH_DONG_SUA.equals(l.getHanhDong()))) {
            throw new ApiException(HttpStatus.CONFLICT, "Đơn " + hd.getMaHoaDon() + " đã được chỉnh sửa 1 lần, không sửa thêm được.", "DA_SUA_DON", null);
        }
        if (req == null) throw ApiException.badRequest("Thiếu thông tin chỉnh sửa.");

        // 1. Người nhận
        LoiForm loi = new LoiForm();
        String hoTen = gon(req.hoTenNguoiNhan());
        String sdt = req.sdtNguoiNhan() == null ? "" : req.sdtNguoiNhan().trim().replaceAll("[\\s.-]", "");
        String diaChi = gon(req.diaChiGiaoHang());
        if (hoTen.isEmpty()) loi.them("hoTenNguoiNhan", "Vui lòng nhập tên người nhận.");
        else if (hoTen.length() > 255) loi.them("hoTenNguoiNhan", "Tên người nhận tối đa 255 ký tự.");
        if (!sdt.matches("0[35789]\\d{8}")) loi.them("sdtNguoiNhan", "Số điện thoại gồm 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        if (diaChi.length() < 10) loi.them("diaChiGiaoHang", "Địa chỉ quá ngắn, hãy ghi rõ số nhà, đường, phường/xã, tỉnh/thành.");
        else if (diaChi.length() > 500) loi.them("diaChiGiaoHang", "Địa chỉ tối đa 500 ký tự.");
        String lyDo = gon(req.lyDo());
        if (lyDo.isEmpty()) loi.them("lyDo", "Vui lòng ghi lý do chỉnh sửa (vd: khách gọi đổi địa chỉ).");
        else if (lyDo.length() > 500) loi.them("lyDo", "Lý do tối đa 500 ký tự.");
        if (req.items() == null || req.items().isEmpty()) loi.them("items", "Đơn phải còn ít nhất 1 sản phẩm. Muốn bỏ hết thì hủy đơn.");
        loi.nemNeuCoLoi();

        // 2. Sản phẩm
        LocalDateTime luc = bayGio();
        Map<Long, Double> phanTram = giamGiaService.phanTramTheoBienThe(luc);
        List<HoaDonChiTiet> cu = chiTietRepo.findByHoaDon_IdOrderByIdAsc(hd.getId());
        Map<Integer, HoaDonChiTiet> theoId = new HashMap<>();
        for (HoaDonChiTiet ct : cu) theoId.put(ct.getId(), ct);
        Map<Integer, Integer> giuLai = new LinkedHashMap<>(); // id dòng cũ -> số lượng mới
        Map<String, int[]> them = new LinkedHashMap<>(); // "idBienThe@gia" -> [idBienThe, soLuong]
        Map<Integer, Integer> canTheoBienThe = new HashMap<>();
        List<String> moTa = new ArrayList<>();
        for (DongSua d : req.items()) {
            int sl = d == null || d.soLuong() == null ? 0 : d.soLuong();
            if (sl < 1 || sl > SO_LUONG_TOI_DA) throw ApiException.badRequest("Số lượng mỗi sản phẩm từ 1 đến " + SO_LUONG_TOI_DA + ".");
            if (d.idChiTiet() != null) {
                HoaDonChiTiet ct = theoId.get(d.idChiTiet());
                if (ct == null) throw ApiException.badRequest("Dòng sản phẩm không thuộc hóa đơn này. Hãy tải lại trang.");
                giuLai.merge(ct.getId(), sl, Integer::sum);
                canTheoBienThe.merge(ct.getSanPhamChiTiet().getId(), sl, Integer::sum);
                continue;
            }
            SanPhamChiTiet c = spctRepo.findById(d.idBienThe() == null ? -1 : d.idBienThe())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm cần thêm."));
            if (!BanHangService.dangBan(c)) throw ApiException.conflict("Sản phẩm " + c.getMaSpct() + " đang ngừng bán.");
            long gia = PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), phanTram.getOrDefault(c.getId().longValue(), 0.0));
            // Cùng biến thể, cùng giá với dòng đang có thì cộng vào dòng đó; giá đã khác thì thành dòng mới
            HoaDonChiTiet cungGia = cu.stream().filter(x -> x.getSanPhamChiTiet().getId().equals(c.getId())
                    && x.getDonGia() != null && x.getDonGia().longValue() == gia && giuLai.containsKey(x.getId())).findFirst().orElse(null);
            if (cungGia != null) giuLai.merge(cungGia.getId(), sl, Integer::sum);
            else them.computeIfAbsent(c.getId() + "@" + gia, k -> new int[] {c.getId(), 0})[1] += sl;
            canTheoBienThe.merge(c.getId(), sl, Integer::sum);
        }
        // Đơn chưa xác nhận nên chưa trừ kho: chỉ cần kho còn đủ cho số lượng mới
        for (Map.Entry<Integer, Integer> e : canTheoBienThe.entrySet()) {
            SanPhamChiTiet c = spctRepo.findById(e.getKey()).orElseThrow();
            int ton = c.getSoLuongTon() == null ? 0 : c.getSoLuongTon();
            if (ton < e.getValue()) throw ApiException.conflict("Sản phẩm " + c.getMaSpct() + " chỉ còn " + ton + " trong kho (đơn cần " + e.getValue() + ").");
        }
        long tienHangCu = 0;
        for (HoaDonChiTiet ct : cu) tienHangCu += ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue();
        for (HoaDonChiTiet ct : cu) {
            Integer sl = giuLai.get(ct.getId());
            String ten = ct.getSanPhamChiTiet().getMaSpct();
            if (sl == null) {
                moTa.add("Bỏ " + ten + " x" + ct.getSoLuong());
                chiTietRepo.delete(ct);
            } else if (!sl.equals(ct.getSoLuong())) {
                moTa.add(ten + ": " + ct.getSoLuong() + " → " + sl);
                ct.setSoLuong(sl);
                ct.setThanhTien(ct.getDonGia().multiply(BigDecimal.valueOf(sl)));
                chiTietRepo.save(ct);
            }
        }
        for (int[] t : them.values()) {
            SanPhamChiTiet c = spctRepo.findById(t[0]).orElseThrow();
            long gia = PhieuApDung.giaSauDot(c.getGiaBan() == null ? 0 : c.getGiaBan(), phanTram.getOrDefault(c.getId().longValue(), 0.0));
            HoaDonChiTiet ct = new HoaDonChiTiet();
            ct.setHoaDon(hd);
            ct.setSanPhamChiTiet(c);
            ct.setSoLuong(t[1]);
            ct.setDonGia(BigDecimal.valueOf(gia));
            ct.setThanhTien(BigDecimal.valueOf(gia * t[1]));
            ct.setTrangThai(1);
            chiTietRepo.save(ct);
            boolean coDongCu = cu.stream().anyMatch(x -> x.getSanPhamChiTiet().getId().equals(c.getId()) && giuLai.containsKey(x.getId()));
            moTa.add("Thêm " + c.getMaSpct() + " x" + t[1] + " giá " + tien(gia) + (coDongCu ? " (giá đã đổi nên tách dòng mới)" : ""));
        }
        long tienHang = 0;
        for (HoaDonChiTiet ct : chiTietRepo.findByHoaDon_IdOrderByIdAsc(hd.getId())) tienHang += ct.getThanhTien() == null ? 0 : ct.getThanhTien().longValue();

        // 3. Phiếu giảm giá + phí giao hàng
        long giamCu = hd.getSoTienGiam() == null ? 0 : hd.getSoTienGiam().longValue();
        long phiCu = hd.getPhiVanChuyen() == null ? 0 : hd.getPhiVanChuyen().longValue();
        long thanhTienCu = hd.getThanhTien() == null ? 0 : hd.getThanhTien().longValue();
        PhieuGiamGia phieu = hd.getPhieuGiamGia();
        long giam = 0;
        if (phieu != null) {
            long donMin = phieu.getGiaTriDonHangToiThieu() == null ? 0 : phieu.getGiaTriDonHangToiThieu();
            if (tienHang < donMin) {
                moTa.add("Bỏ phiếu " + phieu.getMaPhieu() + " vì đơn chưa đạt tối thiểu " + tien(donMin));
                hd.setPhieuGiamGia(null);
            } else {
                giam = PhieuApDung.soTienGiam(phieu, tienHang);
            }
        }
        long phi = BanHangService.phiGiaoHang(tienHang);
        long thanhTien = Math.max(0, tienHang - giam + phi);
        boolean doiNguoiNhan = !hoTen.equals(gon(hd.getHoTenNguoiNhan())) || !sdt.equals(gon(hd.getSdtNguoiNhan())) || !diaChi.equals(gon(hd.getDiaChiGiaoHang()));
        if (doiNguoiNhan) moTa.add(0, "Giao tới: " + hoTen + " · " + sdt + " · " + diaChi);
        if (phi != phiCu) moTa.add("Phí giao hàng " + tien(phiCu) + " → " + tien(phi));
        if (giam != giamCu) moTa.add("Giảm giá " + tien(giamCu) + " → " + tien(giam));
        if (moTa.isEmpty()) throw ApiException.badRequest("Chưa có thay đổi nào so với đơn hiện tại.");

        hd.setHoTenNguoiNhan(hoTen);
        hd.setSdtNguoiNhan(sdt);
        hd.setDiaChiGiaoHang(diaChi);
        hd.setTongTien(BigDecimal.valueOf(tienHang));
        hd.setSoTienGiam(BigDecimal.valueOf(giam));
        hd.setPhiVanChuyen(BigDecimal.valueOf(phi));
        hd.setThanhTien(BigDecimal.valueOf(thanhTien));
        hd.setNgayCapNhat(luc);
        hoaDonRepo.save(hd);

        // 4. Hoàn phí / phụ phí theo số khách đã trả trước
        long daTra = daTra(hd);
        long chenh = thanhTien - thanhTienCu;
        String tienTe;
        if (daTra > 0 && thanhTien < daTra) {
            long hoan = daTra - thanhTien;
            LichSuThanhToan tt = new LichSuThanhToan();
            tt.setHoaDon(hd);
            tt.setSoTien(BigDecimal.valueOf(-hoan));
            tt.setThoiGian(luc);
            tt.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
            tt.setIdPhuongThucThanhToan(hd.getIdPhuongThucThanhToan());
            tt.setMoTa("Hoàn phí cho khách sau khi chỉnh sửa đơn");
            thanhToanRepo.save(tt);
            tienTe = "Hoàn phí " + tien(hoan) + " cho khách (đã trả trước " + tien(daTra) + ")";
        } else if (daTra > 0 && thanhTien > daTra) {
            tienTe = "Phụ phí " + tien(thanhTien - daTra) + ": khách trả thêm khi nhận hàng";
        } else if (chenh > 0) {
            tienTe = "Khách trả thêm " + tien(chenh) + " khi nhận hàng (COD " + tien(thanhTien) + ")";
        } else if (chenh < 0) {
            tienTe = "Khách trả ít hơn " + tien(-chenh) + " khi nhận hàng (COD " + tien(thanhTien) + ")";
        } else {
            tienTe = "Tổng tiền không đổi";
        }
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong(HANH_DONG_SUA);
        ls.setNguoiThaoTac(nd.hoTen() == null ? "Nhân viên" : nd.hoTen());
        ls.setThoiGian(luc);
        String noiDung = lyDo + ". " + String.join("; ", moTa) + ". Tổng " + tien(thanhTienCu) + " → " + tien(thanhTien) + ". " + tienTe + ".";
        ls.setMoTa(noiDung.length() > 4000 ? noiDung.substring(0, 4000) : noiDung);
        lichSuRepo.save(ls);

        Map<String, Object> kq = hoaDonService.getChiTietHoaDon(hd.getId());
        Map<String, Object> ketQuaSua = new LinkedHashMap<>();
        ketQuaSua.put("tienHangCu", tienHangCu);
        ketQuaSua.put("thanhTienCu", thanhTienCu);
        ketQuaSua.put("thanhTienMoi", thanhTien);
        ketQuaSua.put("daTra", daTra);
        ketQuaSua.put("hoanPhi", daTra > thanhTien ? daTra - thanhTien : 0);
        ketQuaSua.put("phuPhi", daTra > 0 && thanhTien > daTra ? thanhTien - daTra : 0);
        ketQuaSua.put("moTa", tienTe);
        kq.put("ketQuaSua", ketQuaSua);
        return kq;
    }

    /** Đơn online chuyển khoản: nhân viên đối chiếu sao kê rồi xác nhận đã nhận đủ tiền (ghi khoản đã thanh toán). */
    @Transactional
    public Map<String, Object> xacNhanThanhToan(Integer id, XacNhanThanhToanRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hóa đơn."));
        if (!Integer.valueOf(HoaDon.GIAO_HANG).equals(hd.getLoaiDon()) || !CHUYEN_KHOAN_ONLINE.equals(hd.getIdPhuongThucThanhToan())) {
            throw ApiException.conflict("Chỉ xác nhận chuyển khoản cho đơn online khách chọn chuyển khoản.");
        }
        int tt = hd.getTrangThai() == null ? HoaDon.CHO_XAC_NHAN : hd.getTrangThai();
        if (tt == HoaDon.DA_HUY || tt == HoaDon.HOAN_THANH || tt == HoaDon.HOAN_TIEN) throw ApiException.conflict("Đơn đã kết thúc.");
        long con = (hd.getThanhTien() == null ? 0 : hd.getThanhTien().longValue()) - daTra(hd);
        if (con <= 0) throw ApiException.conflict("Đơn đã được thanh toán đủ.");
        String ma = req == null || req.maGiaoDich() == null ? null : req.maGiaoDich().trim();
        if (ma != null && ma.length() > 100) throw ApiException.badRequest("Mã giao dịch tối đa 100 ký tự.");
        LocalDateTime luc = bayGio();
        LichSuThanhToan t = new LichSuThanhToan();
        t.setHoaDon(hd);
        t.setSoTien(BigDecimal.valueOf(con));
        t.setMaGiaoDich(ma == null || ma.isEmpty() ? null : ma);
        t.setThoiGian(luc);
        t.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        t.setIdPhuongThucThanhToan(hd.getIdPhuongThucThanhToan());
        t.setMoTa("Khách chuyển khoản trước (nhân viên đã đối chiếu sao kê)");
        thanhToanRepo.save(t);
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong("Xác nhận thanh toán");
        ls.setNguoiThaoTac(nd.hoTen() == null ? "Nhân viên" : nd.hoTen());
        ls.setThoiGian(luc);
        ls.setMoTa("Đã nhận chuyển khoản " + tien(con) + (t.getMaGiaoDich() == null ? "" : " (mã GD " + t.getMaGiaoDich() + ")"));
        lichSuRepo.save(ls);
        return hoaDonService.getChiTietHoaDon(hd.getId());
    }

    /**
     * Ngân hàng / cổng thanh toán báo "tiền đã vào tài khoản" (webhook): tự ghi khoản đã thanh toán cho đơn online chuyển khoản.
     * Không cần nhân viên đối chiếu sao kê. Idempotent theo `maGiaoDich` (webhook gửi lại thì không ghi trùng).
     *
     * @return mô tả kết quả để trả lại cho bên gọi (log), không bao giờ ném lỗi vì đơn không hợp lệ
     */
    @Transactional
    public String ghiNhanChuyenKhoanTuDong(String maHoaDon, long soTien, String maGiaoDich, String noiDung) {
        if (maHoaDon == null || soTien <= 0) return "Bỏ qua: thiếu mã đơn hoặc số tiền.";
        HoaDon hd = hoaDonRepo.findFirstByMaHoaDonIgnoreCase(maHoaDon.trim()).orElse(null);
        if (hd == null) return "Bỏ qua: không có đơn " + maHoaDon;
        if (!Integer.valueOf(HoaDon.GIAO_HANG).equals(hd.getLoaiDon()) || !CHUYEN_KHOAN_ONLINE.equals(hd.getIdPhuongThucThanhToan())) {
            return "Bỏ qua: đơn " + hd.getMaHoaDon() + " không phải đơn chuyển khoản online.";
        }
        int tt = hd.getTrangThai() == null ? HoaDon.CHO_XAC_NHAN : hd.getTrangThai();
        if (tt == HoaDon.DA_HUY || tt == HoaDon.HOAN_THANH || tt == HoaDon.HOAN_TIEN) {
            return "Bỏ qua: đơn " + hd.getMaHoaDon() + " đã kết thúc (cần hoàn tiền thủ công nếu khách đã chuyển).";
        }
        String ma = maGiaoDich == null ? null : maGiaoDich.trim();
        if (ma != null && ma.length() > 100) ma = ma.substring(0, 100);
        if (ma != null && !ma.isEmpty()) {
            for (LichSuThanhToan x : thanhToanRepo.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
                if (ma.equals(x.getMaGiaoDich())) return "Bỏ qua: giao dịch " + ma + " đã ghi nhận trước đó.";
            }
        }
        LocalDateTime luc = bayGio();
        LichSuThanhToan t = new LichSuThanhToan();
        t.setHoaDon(hd);
        t.setSoTien(BigDecimal.valueOf(soTien));
        t.setMaGiaoDich(ma == null || ma.isEmpty() ? null : ma);
        t.setThoiGian(luc);
        t.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        t.setIdPhuongThucThanhToan(hd.getIdPhuongThucThanhToan());
        t.setMoTa("Khách chuyển khoản VietQR (hệ thống tự đối chiếu)");
        thanhToanRepo.save(t);
        long conLai = (hd.getThanhTien() == null ? 0 : hd.getThanhTien().longValue()) - daTra(hd);
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setHanhDong("Xác nhận thanh toán");
        ls.setNguoiThaoTac("Hệ thống");
        ls.setThoiGian(luc);
        String ghiChu = "Tự động ghi nhận chuyển khoản " + tien(soTien) + (t.getMaGiaoDich() == null ? "" : " (mã GD " + t.getMaGiaoDich() + ")")
                + (conLai > 0 ? " - còn thiếu " + tien(conLai) : conLai < 0 ? " - khách chuyển dư " + tien(-conLai) : "");
        ls.setMoTa(ghiChu.length() > 480 ? ghiChu.substring(0, 480) : ghiChu);
        lichSuRepo.save(ls);
        return "Đã ghi nhận " + tien(soTien) + " cho đơn " + hd.getMaHoaDon() + (conLai > 0 ? " (còn thiếu " + tien(conLai) + ")" : "");
    }

    private long daTra(HoaDon hd) {
        long s = 0;
        for (LichSuThanhToan tt : thanhToanRepo.findByHoaDon_IdOrderByThoiGianAscIdAsc(hd.getId())) {
            if (tt.getTrangThai() != null && tt.getTrangThai() == LichSuThanhToan.DA_THANH_TOAN && tt.getSoTien() != null) s += tt.getSoTien().longValue();
        }
        return s;
    }

    private static String gon(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String tien(long v) {
        return GiaoCaService.dinhDangTien(BigDecimal.valueOf(v));
    }
}
