package com.footstyle.demo.service;

import com.footstyle.demo.dto.KhachHangTomTatResponse;
import com.footstyle.demo.dto.PhieuGiamGiaRequest;
import com.footstyle.demo.dto.PhieuGiamGiaResponse;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    // Chữ mà frontend dùng, trong DB lưu bằng số (xem hằng số trong entity PhieuGiamGia)
    private static final String CONG_KHAI = "CONG_KHAI"; // doi_tuong_ap_dung = 1
    private static final String CA_NHAN = "CA_NHAN";     // doi_tuong_ap_dung = 2
    private static final String PHAN_TRAM = "PHAN_TRAM"; // loai_giam = 1
    private static final String TIEN_MAT = "TIEN_MAT";   // loai_giam = 2

    private final PhieuGiamGiaRepository phieuRepo;
    private final PhieuGiamGiaKhachHangRepository phieuKhRepo;
    private final KhachHangRepository khachHangRepo;
    private final MailService mailService;

    /* ===================== Đọc ===================== */

    // Danh sách phiếu (không kèm khách hàng được tặng, muốn xem thì mở chi tiết)
    @Transactional(readOnly = true)
    public List<PhieuGiamGiaResponse> getAll() {
        List<PhieuGiamGiaResponse> ketQua = new ArrayList<>();
        for (PhieuGiamGia p : phieuRepo.findAllByOrderByIdDesc()) {
            ketQua.add(toResponse(p, new ArrayList<>()));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public PhieuGiamGiaResponse getById(Integer id) {
        PhieuGiamGia p = timPhieu(id);
        return toResponse(p, getKhachHangDuocTang(p.getId()));
    }

    // Khách hàng đang hoạt động, dùng cho ô chọn khi tặng phiếu cá nhân
    @Transactional(readOnly = true)
    public List<KhachHangTomTatResponse> getKhachHangCoTheChon() {
        List<KhachHangTomTatResponse> ketQua = new ArrayList<>();
        for (KhachHang k : khachHangRepo.findByTrangThaiOrderByHoTenAsc(1)) {
            ketQua.add(toKhachHang(k, false));
        }
        return ketQua;
    }

    /* ===================== Thêm / sửa / đổi trạng thái ===================== */

    @Transactional
    public PhieuGiamGiaResponse them(PhieuGiamGiaRequest req) {
        String ma = chuanHoaMa(req.ma());
        if (phieuRepo.existsByMaPhieuIgnoreCase(ma)) {
            throw ApiException.conflict("Mã phiếu đã tồn tại.");
        }
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu(ma);
        p.setSoLuongDaDung(0);
        p.setTrangThai(PhieuGiamGia.HOAT_DONG);
        napDuLieu(p, req, true);
        p = phieuRepo.save(p);

        // Phiếu cá nhân: mỗi khách được tặng là 1 dòng trong bảng phieu_giam_gia_khach_hang
        String ketQuaMail = null;
        if (CA_NHAN.equals(req.hinhThuc())) {
            List<Integer> idKhachHang = layDanhSachKhachHang(req);
            for (Integer idKh : idKhachHang) {
                phieuKhRepo.save(taoLienKet(p.getId(), idKh));
            }
            // Tạo phiếu cá nhân: mọi khách được tặng đều nhận "mail phiếu mới"
            ketQuaMail = guiMail(p, idKhachHang, new ArrayList<>(), new ArrayList<>());
        }
        return toResponse(p, getKhachHangDuocTang(p.getId())).voiKetQuaMail(ketQuaMail);
    }

    @Transactional
    public PhieuGiamGiaResponse sua(Integer id, PhieuGiamGiaRequest req) {
        PhieuGiamGia p = timPhieu(id);
        List<PhieuGiamGiaKhachHang> lienKetCu = phieuKhRepo.findByIdPhieuGiamGia(id);

        // Chỉ phiếu còn "Sắp diễn ra" (chưa tới ngày bắt đầu) mới gửi mail khi sửa; xét TRƯỚC khi nạp dữ liệu mới
        boolean sapDienRa = p.getNgayBatDau() != null && p.getNgayBatDau().isAfter(LocalDateTime.now());

        // Phiếu đã có người dùng thì không được đổi giữa công khai <-> cá nhân
        boolean daCoNguoiDung = soLuongDaDung(p) > 0;
        for (PhieuGiamGiaKhachHang l : lienKetCu) {
            if (l.getNgaySuDung() != null) daCoNguoiDung = true;
        }
        int doiTuongMoi = CA_NHAN.equals(req.hinhThuc())
                ? PhieuGiamGia.DOI_TUONG_KHACH_CU_THE : PhieuGiamGia.DOI_TUONG_TAT_CA;
        boolean doiHinhThuc = p.getDoiTuongApDung() != null && p.getDoiTuongApDung() != doiTuongMoi;
        if (doiHinhThuc && daCoNguoiDung) {
            throw ApiException.conflict("Phiếu đã được sử dụng nên không thể đổi hình thức (công khai / cá nhân).");
        }

        // Chụp lại thông tin phiếu TRƯỚC khi sửa để biết sau đó thông tin có thật sự đổi không
        String thongTinTruoc = chuKyThongTin(p);
        List<Integer> idCu = new ArrayList<>();
        for (PhieuGiamGiaKhachHang l : lienKetCu) {
            idCu.add(l.getIdKhachHang());
        }

        // Mã không đổi sau khi tạo nên bỏ qua req.ma()
        napDuLieu(p, req, false);
        phieuRepo.save(p);
        capNhatKhachHang(p, req, lienKetCu);
        boolean doiThongTin = !thongTinTruoc.equals(chuKyThongTin(p));

        /*
         * Gửi mail khi sửa phiếu cá nhân còn "Sắp diễn ra" (xem PhieuMailKeHoach):
         *  TH1 - đổi thông tin phiếu, danh sách khách giữ nguyên      -> mọi khách nhận "mail cập nhật"
         *  TH2 - thông tin giữ nguyên, danh sách khách thay đổi       -> khách giữ lại: KHÔNG gửi;
         *                                                                khách mới: "mail phiếu mới"; khách bị bỏ: "mail hủy"
         *  TH3 - đổi cả hai                                            -> khách giữ lại: "mail cập nhật"; khách mới: "mail mới"; khách bị bỏ: "mail hủy"
         * Phiếu đã / đang diễn ra thì không gửi mail.
         */
        String ketQuaMail = null;
        if (sapDienRa) {
            List<Integer> idMoiDanhSach = CA_NHAN.equals(req.hinhThuc()) ? layDanhSachKhachHang(req) : new ArrayList<>();
            PhieuMailKeHoach.KeHoach keHoach = PhieuMailKeHoach.lap(idCu, idMoiDanhSach, doiThongTin);
            ketQuaMail = guiMail(p, keHoach.moi(), keHoach.capNhat(), keHoach.huy());
        }
        return toResponse(p, getKhachHangDuocTang(p.getId())).voiKetQuaMail(ketQuaMail);
    }

    // Không xóa phiếu, chỉ ẩn / hiện bằng cột trạng thái. Phiếu đã hết hạn thì không đổi nữa.
    @Transactional
    public PhieuGiamGiaResponse doiTrangThai(Integer id) {
        PhieuGiamGia p = timPhieu(id);
        if (p.getNgayKetThuc() != null && p.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw ApiException.conflict("Phiếu đã kết thúc, không thể thay đổi trạng thái.");
        }
        if (dangHoatDong(p)) {
            p.setTrangThai(PhieuGiamGia.NGUNG_HOAT_DONG);
        } else {
            p.setTrangThai(PhieuGiamGia.HOAT_DONG);
        }
        phieuRepo.save(p);
        return toResponse(p, getKhachHangDuocTang(p.getId()));
    }

    /* ===================== Hàm phụ ===================== */

    private PhieuGiamGia timPhieu(Integer id) {
        return phieuRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu giảm giá."));
    }

    // Số phiếu đã dùng (null thì coi là 0)
    private int soLuongDaDung(PhieuGiamGia p) {
        return p.getSoLuongDaDung() == null ? 0 : p.getSoLuongDaDung();
    }

    private boolean dangHoatDong(PhieuGiamGia p) {
        return p.getTrangThai() != null && p.getTrangThai() == PhieuGiamGia.HOAT_DONG;
    }

    // Nếu điều kiện đúng thì báo lỗi 400 kèm câu thông báo (để mỗi lần kiểm tra chỉ cần 1 dòng)
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    private String chuanHoaMa(String ma) {
        String m = ma == null ? "" : ma.trim().toUpperCase();
        loi(m.isEmpty(), "Nhập mã phiếu.");
        loi(!m.matches("[A-Z0-9]{4,20}"), "Mã gồm 4-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.");
        return m;
    }

    // Kiểm tra dữ liệu người dùng gửi lên, hợp lệ thì ghi vào p (chưa lưu xuống DB)
    private void napDuLieu(PhieuGiamGia p, PhieuGiamGiaRequest req, boolean taoMoi) {
        // Tên phiếu
        String ten = req.ten() == null ? "" : req.ten().trim();
        loi(ten.isEmpty(), "Nhập tên phiếu.");
        loi(ten.length() > 100, "Tên phiếu tối đa 100 ký tự.");

        // Hình thức và loại giảm chỉ nhận đúng các giá trị đã quy định
        String hinhThuc = req.hinhThuc();
        loi(!CONG_KHAI.equals(hinhThuc) && !CA_NHAN.equals(hinhThuc), "Hình thức phiếu không hợp lệ.");
        String loai = req.loaiGiam();
        loi(!PHAN_TRAM.equals(loai) && !TIEN_MAT.equals(loai), "Loại giảm không hợp lệ.");
        boolean phanTram = PHAN_TRAM.equals(loai);

        // Giá trị giảm: phần trăm từ 1-100, tiền mặt tối thiểu 1.000 ₫
        Long giaTri = req.giaTri();
        loi(giaTri == null, "Nhập giá trị giảm.");
        loi(phanTram && (giaTri < 1 || giaTri > 100), "Phần trăm giảm từ 1 đến 100.");
        loi(!phanTram && giaTri < 1000, "Số tiền giảm tối thiểu 1.000 ₫.");

        // Giảm tối đa chỉ dùng cho giảm theo %
        Long giamToiDa = phanTram ? req.giamToiDa() : null;
        loi(giamToiDa != null && giamToiDa <= 0, "Giảm tối đa phải lớn hơn 0, hoặc để trống nếu không giới hạn.");

        // Đơn tối thiểu (không nhập thì là 0)
        long donToiThieu = req.donToiThieu() == null ? 0 : req.donToiThieu();
        loi(donToiThieu < 0, "Đơn tối thiểu không được âm.");
        loi(!phanTram && donToiThieu > 0 && giaTri > donToiThieu, "Đơn tối thiểu phải lớn hơn hoặc bằng số tiền giảm.");

        // Ngày bắt đầu / kết thúc
        LocalDate batDau = req.ngayBatDau();
        LocalDate ketThuc = req.ngayKetThuc();
        loi(batDau == null, "Chọn ngày bắt đầu.");
        loi(ketThuc == null, "Chọn ngày kết thúc.");
        loi(ketThuc.isBefore(batDau), "Ngày kết thúc phải sau hoặc cùng ngày bắt đầu.");
        loi(taoMoi && ketThuc.isBefore(LocalDate.now()), "Ngày kết thúc không được ở quá khứ.");

        // Số lượng và giới hạn mỗi khách
        int soLuong;
        Integer gioiHan;
        if (CA_NHAN.equals(hinhThuc)) {
            // Phiếu cá nhân: mỗi khách được tặng đúng 1 phiếu nên số lượng = số khách được chọn
            soLuong = layDanhSachKhachHang(req).size();
            gioiHan = 1;
        } else {
            loi(req.soLuong() == null || req.soLuong() < 1, "Số lượng là số nguyên từ 1 trở lên.");
            soLuong = req.soLuong();
            loi(soLuong < soLuongDaDung(p), "Số lượng không được nhỏ hơn số phiếu đã dùng (" + soLuongDaDung(p) + ").");
            gioiHan = req.gioiHanMoiKhach();
            loi(gioiHan != null && (gioiHan < 1 || gioiHan > soLuong),
                    "Giới hạn mỗi khách từ 1 đến số lượng phát hành, hoặc để trống nếu không giới hạn.");
        }

        // Dữ liệu hợp lệ hết -> ghi vào entity
        p.setTenPhieu(ten);
        p.setDoiTuongApDung(CA_NHAN.equals(hinhThuc) ? PhieuGiamGia.DOI_TUONG_KHACH_CU_THE : PhieuGiamGia.DOI_TUONG_TAT_CA);
        p.setLoaiGiam(phanTram ? PhieuGiamGia.LOAI_PHAN_TRAM : PhieuGiamGia.LOAI_SO_TIEN);
        p.setGiaTriGiam(giaTri);
        p.setGiamToiDa(giamToiDa);
        p.setGiaTriDonHangToiThieu(donToiThieu);
        p.setSoLuong(soLuong);
        p.setGioiHanMoiKhach(gioiHan);
        p.setNgayBatDau(batDau.atStartOfDay());                      // 00:00:00
        p.setNgayKetThuc(ketThuc.atTime(LocalTime.of(23, 59, 59)));  // hết ngày kết thúc
    }

    // Gộp các thông tin khách hàng nhìn thấy trong mail thành 1 chuỗi, để so sánh trước / sau khi sửa
    private String chuKyThongTin(PhieuGiamGia p) {
        return p.getTenPhieu() + "|" + p.getLoaiGiam() + "|" + p.getGiaTriGiam() + "|" + p.getGiamToiDa() + "|"
                + p.getGiaTriDonHangToiThieu() + "|" + p.getNgayBatDau() + "|" + p.getNgayKetThuc();
    }

    // Xếp mail cho 3 nhóm khách (mới / cập nhật / hủy). Mail chỉ thật sự gửi SAU KHI lưu DB thành công.
    // Trả về câu tóm tắt cho giao diện (null nếu không có mail nào).
    private String guiMail(PhieuGiamGia p, List<Integer> idMoi, List<Integer> idCapNhat, List<Integer> idHuy) {
        List<Integer> tatCa = new ArrayList<>();
        tatCa.addAll(idMoi);
        tatCa.addAll(idCapNhat);
        tatCa.addAll(idHuy);
        if (tatCa.isEmpty()) {
            return null;
        }
        Map<Integer, KhachHang> khachTheoId = new HashMap<>();
        for (KhachHang k : khachHangRepo.findAllById(tatCa)) {
            khachTheoId.put(k.getId(), k);
        }

        List<Runnable> viec = new ArrayList<>();
        int[] daXep = new int[3];      // số mail mới / cập nhật / hủy sẽ gửi
        int khongCoEmail = 0;
        MailService.Loai[] loai = {MailService.Loai.MOI, MailService.Loai.CAP_NHAT, MailService.Loai.HUY};
        List<List<Integer>> nhom = List.of(idMoi, idCapNhat, idHuy);
        for (int i = 0; i < 3; i++) {
            for (Integer idKh : nhom.get(i)) {
                KhachHang k = khachTheoId.get(idKh);
                if (!MailService.coEmail(k)) {
                    khongCoEmail++;
                    continue;
                }
                final MailService.Loai loaiMail = loai[i];
                viec.add(() -> mailService.guiPhieuGiamGia(loaiMail, p, k));
                daXep[i]++;
            }
        }

        if (!viec.isEmpty()) {
            Runnable chay = () -> viec.forEach(Runnable::run);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        chay.run();
                    }
                });
            } else {
                chay.run();
            }
        }

        List<String> phan = new ArrayList<>();
        if (daXep[0] > 0) phan.add(daXep[0] + " mail phiếu mới");
        if (daXep[1] > 0) phan.add(daXep[1] + " mail cập nhật");
        if (daXep[2] > 0) phan.add(daXep[2] + " mail hủy phiếu");
        String ketQua = phan.isEmpty() ? "Không gửi được mail nào" : "Đã gửi " + String.join(", ", phan);
        if (khongCoEmail > 0) {
            ketQua += " (" + khongCoEmail + " khách chưa có email nên không gửi được)";
        }
        return ketQua + ".";
    }

    // Danh sách id khách hàng của phiếu cá nhân: không rỗng, không trùng, phải tồn tại
    private List<Integer> layDanhSachKhachHang(PhieuGiamGiaRequest req) {
        List<Integer> ids = new ArrayList<>();
        if (req.khachHangIds() != null) {
            for (Integer id : req.khachHangIds()) {
                loi(id == null, "Danh sách khách hàng không hợp lệ.");
                if (!ids.contains(id)) ids.add(id); // bỏ id trùng
            }
        }
        loi(ids.isEmpty(), "Phiếu cá nhân cần chọn ít nhất một khách hàng.");
        loi(khachHangRepo.findAllById(ids).size() != ids.size(), "Có khách hàng không tồn tại trong hệ thống.");
        return ids;
    }

    private PhieuGiamGiaKhachHang taoLienKet(Integer idPhieu, Integer idKhachHang) {
        PhieuGiamGiaKhachHang l = new PhieuGiamGiaKhachHang();
        l.setIdPhieuGiamGia(idPhieu);
        l.setIdKhachHang(idKhachHang);
        l.setTrangThai(1);
        return l;
    }

    // Cập nhật danh sách khách được tặng khi sửa: xóa khách bị bỏ, thêm khách mới.
    // Khách đã dùng phiếu thì không được bỏ.
    private void capNhatKhachHang(PhieuGiamGia p, PhieuGiamGiaRequest req, List<PhieuGiamGiaKhachHang> lienKetCu) {
        if (!CA_NHAN.equals(req.hinhThuc())) {
            phieuKhRepo.deleteAll(lienKetCu); // phiếu công khai không gắn khách hàng nào
            return;
        }
        List<Integer> idMoi = layDanhSachKhachHang(req);

        // Duyệt các khách đang được tặng: ai không còn trong danh sách mới thì xóa
        List<PhieuGiamGiaKhachHang> canXoa = new ArrayList<>();
        List<Integer> idDaCo = new ArrayList<>();
        for (PhieuGiamGiaKhachHang l : lienKetCu) {
            if (idMoi.contains(l.getIdKhachHang())) {
                idDaCo.add(l.getIdKhachHang());
            } else if (l.getNgaySuDung() != null) {
                throw ApiException.conflict("Không thể bỏ khách hàng đã dùng phiếu này.");
            } else {
                canXoa.add(l);
            }
        }
        phieuKhRepo.deleteAll(canXoa);

        // Khách mới chưa có trong bảng thì thêm vào
        for (Integer idKh : idMoi) {
            if (!idDaCo.contains(idKh)) {
                phieuKhRepo.save(taoLienKet(p.getId(), idKh));
            }
        }
    }

    // Khách hàng được tặng phiếu (kèm cờ đã dùng hay chưa)
    private List<KhachHangTomTatResponse> getKhachHangDuocTang(Integer idPhieu) {
        List<KhachHangTomTatResponse> ketQua = new ArrayList<>();
        for (PhieuGiamGiaKhachHang l : phieuKhRepo.findByIdPhieuGiamGia(idPhieu)) {
            KhachHang k = khachHangRepo.findById(l.getIdKhachHang()).orElse(null);
            if (k != null) {
                ketQua.add(toKhachHang(k, l.getNgaySuDung() != null));
            }
        }
        return ketQua;
    }

    private KhachHangTomTatResponse toKhachHang(KhachHang k, boolean daDung) {
        return new KhachHangTomTatResponse(k.getId(), k.getMaKhachHang(), k.getHoTen(), k.getSdt(), k.getEmail(), daDung);
    }

    // Đổi entity thành dữ liệu trả về cho frontend (số trong DB -> chữ)
    private PhieuGiamGiaResponse toResponse(PhieuGiamGia p, List<KhachHangTomTatResponse> khachHangs) {
        boolean caNhan = p.getDoiTuongApDung() != null && p.getDoiTuongApDung() == PhieuGiamGia.DOI_TUONG_KHACH_CU_THE;
        boolean phanTram = p.getLoaiGiam() != null && p.getLoaiGiam() == PhieuGiamGia.LOAI_PHAN_TRAM;
        return new PhieuGiamGiaResponse(
                p.getId(),
                p.getMaPhieu(),
                p.getTenPhieu(),
                caNhan ? CA_NHAN : CONG_KHAI,
                phanTram ? PHAN_TRAM : TIEN_MAT,
                p.getGiaTriGiam(),
                p.getGiamToiDa(),
                p.getGiaTriDonHangToiThieu(),
                p.getSoLuong(),
                soLuongDaDung(p),
                p.getGioiHanMoiKhach(),
                p.getNgayBatDau() == null ? null : p.getNgayBatDau().toLocalDate(),
                p.getNgayKetThuc() == null ? null : p.getNgayKetThuc().toLocalDate(),
                dangHoatDong(p),
                khachHangs,
                null);
    }
}
