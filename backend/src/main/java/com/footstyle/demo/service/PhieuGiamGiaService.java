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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    
    private static final String CONG_KHAI = "CONG_KHAI"; 
    private static final String CA_NHAN = "CA_NHAN";     
    private static final String PHAN_TRAM = "PHAN_TRAM"; 
    private static final String TIEN_MAT = "TIEN_MAT";   

    private final PhieuGiamGiaRepository phieuRepo;
    private final PhieuGiamGiaKhachHangRepository phieuKhRepo;
    private final KhachHangRepository khachHangRepo;

    

    
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

    
    @Transactional(readOnly = true)
    public List<KhachHangTomTatResponse> getKhachHangCoTheChon() {
        List<KhachHangTomTatResponse> ketQua = new ArrayList<>();

        for (KhachHang k : khachHangRepo.findByTrangThaiOrderByTenAsc(1)) {
            ketQua.add(toKhachHang(k, false));
        }
        return ketQua;
    }

    

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

        
        if (CA_NHAN.equals(req.hinhThuc())) {
            for (Long idKh : layDanhSachKhachHang(req)) {
                phieuKhRepo.save(taoLienKet(p.getId(), idKh));
            }
        }
        return toResponse(p, getKhachHangDuocTang(p.getId()));
    }

    @Transactional
    public PhieuGiamGiaResponse sua(Integer id, PhieuGiamGiaRequest req) {
        PhieuGiamGia p = timPhieu(id);
        List<PhieuGiamGiaKhachHang> lienKetCu = phieuKhRepo.findByIdPhieuGiamGia(id);

        
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

        
        napDuLieu(p, req, false);
        phieuRepo.save(p);
        capNhatKhachHang(p, req, lienKetCu);
        return toResponse(p, getKhachHangDuocTang(p.getId()));
    }

    
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

    

    private PhieuGiamGia timPhieu(Integer id) {
        return phieuRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu giảm giá."));
    }

    
    private int soLuongDaDung(PhieuGiamGia p) {
        return p.getSoLuongDaDung() == null ? 0 : p.getSoLuongDaDung();
    }

    private boolean dangHoatDong(PhieuGiamGia p) {
        return p.getTrangThai() != null && p.getTrangThai() == PhieuGiamGia.HOAT_DONG;
    }

    
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

    
    private void napDuLieu(PhieuGiamGia p, PhieuGiamGiaRequest req, boolean taoMoi) {
        
        String ten = req.ten() == null ? "" : req.ten().trim();
        loi(ten.isEmpty(), "Nhập tên phiếu.");
        loi(ten.length() > 100, "Tên phiếu tối đa 100 ký tự.");

        
        String hinhThuc = req.hinhThuc();
        loi(!CONG_KHAI.equals(hinhThuc) && !CA_NHAN.equals(hinhThuc), "Hình thức phiếu không hợp lệ.");
        String loai = req.loaiGiam();
        loi(!PHAN_TRAM.equals(loai) && !TIEN_MAT.equals(loai), "Loại giảm không hợp lệ.");
        boolean phanTram = PHAN_TRAM.equals(loai);

        
        Long giaTri = req.giaTri();
        loi(giaTri == null, "Nhập giá trị giảm.");
        loi(phanTram && (giaTri < 1 || giaTri > 100), "Phần trăm giảm từ 1 đến 100.");
        loi(!phanTram && giaTri < 1000, "Số tiền giảm tối thiểu 1.000 ₫.");

        
        Long giamToiDa = phanTram ? req.giamToiDa() : null;
        loi(giamToiDa != null && giamToiDa <= 0, "Giảm tối đa phải lớn hơn 0, hoặc để trống nếu không giới hạn.");

        
        long donToiThieu = req.donToiThieu() == null ? 0 : req.donToiThieu();
        loi(donToiThieu < 0, "Đơn tối thiểu không được âm.");
        loi(!phanTram && donToiThieu > 0 && giaTri > donToiThieu, "Đơn tối thiểu phải lớn hơn hoặc bằng số tiền giảm.");

        
        LocalDate batDau = req.ngayBatDau();
        LocalDate ketThuc = req.ngayKetThuc();
        loi(batDau == null, "Chọn ngày bắt đầu.");
        loi(ketThuc == null, "Chọn ngày kết thúc.");
        loi(ketThuc.isBefore(batDau), "Ngày kết thúc phải sau hoặc cùng ngày bắt đầu.");
        loi(taoMoi && ketThuc.isBefore(LocalDate.now()), "Ngày kết thúc không được ở quá khứ.");

        
        int soLuong;
        Integer gioiHan;
        if (CA_NHAN.equals(hinhThuc)) {
            
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

        
        p.setTenPhieu(ten);
        p.setDoiTuongApDung(CA_NHAN.equals(hinhThuc) ? PhieuGiamGia.DOI_TUONG_KHACH_CU_THE : PhieuGiamGia.DOI_TUONG_TAT_CA);
        p.setLoaiGiam(phanTram ? PhieuGiamGia.LOAI_PHAN_TRAM : PhieuGiamGia.LOAI_SO_TIEN);
        p.setGiaTriGiam(giaTri);
        p.setGiamToiDa(giamToiDa);
        p.setGiaTriDonHangToiThieu(donToiThieu);
        p.setSoLuong(soLuong);
        p.setGioiHanMoiKhach(gioiHan);
        p.setNgayBatDau(batDau.atStartOfDay());                      
        p.setNgayKetThuc(ketThuc.atTime(LocalTime.of(23, 59, 59)));  
    }

    

    private List<Long> layDanhSachKhachHang(PhieuGiamGiaRequest req) {
        List<Long> ids = new ArrayList<>();
        if (req.khachHangIds() != null) {
            for (Long id : req.khachHangIds()) {
                loi(id == null, "Danh sách khách hàng không hợp lệ.");
                if (!ids.contains(id)) ids.add(id); 
            }
        }
        loi(ids.isEmpty(), "Phiếu cá nhân cần chọn ít nhất một khách hàng.");
        loi(khachHangRepo.findAllById(ids).size() != ids.size(), "Có khách hàng không tồn tại trong hệ thống.");
        return ids;
    }


    private PhieuGiamGiaKhachHang taoLienKet(Integer idPhieu, Long idKhachHang) {

        PhieuGiamGiaKhachHang l = new PhieuGiamGiaKhachHang();
        l.setIdPhieuGiamGia(idPhieu);
        l.setIdKhachHang(idKhachHang);
        l.setTrangThai(1);
        return l;
    }

    
    
    private void capNhatKhachHang(PhieuGiamGia p, PhieuGiamGiaRequest req, List<PhieuGiamGiaKhachHang> lienKetCu) {
        if (!CA_NHAN.equals(req.hinhThuc())) {
            phieuKhRepo.deleteAll(lienKetCu); 
            return;
        }

        List<Long> idMoi = layDanhSachKhachHang(req);

        
        List<PhieuGiamGiaKhachHang> canXoa = new ArrayList<>();
        List<Long> idDaCo = new ArrayList<>();

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

        

        for (Long idKh : idMoi) {

            if (!idDaCo.contains(idKh)) {
                phieuKhRepo.save(taoLienKet(p.getId(), idKh));
            }
        }
    }

    
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

        return new KhachHangTomTatResponse(k.getId(), k.getMaKH(), k.getTen(), k.getSdt(), k.getEmail(), daDung);

    }

    
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
                khachHangs);
    }
}
