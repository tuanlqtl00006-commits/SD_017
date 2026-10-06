package com.footstyle.demo.service;

import com.footstyle.demo.dto.ThuocTinhRequest;
import com.footstyle.demo.dto.ThuocTinhResponse;
import com.footstyle.demo.entity.ChatLieu;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.ThuocTinh;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.entity.XuatXu;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ChatLieuRepository;
import com.footstyle.demo.repository.ChuViRepository;
import com.footstyle.demo.repository.DanhMucRepository;
import com.footstyle.demo.repository.DiemCanBangRepository;
import com.footstyle.demo.repository.DoCungRepository;
import com.footstyle.demo.repository.MauSacRepository;
import com.footstyle.demo.repository.ThuocTinhRepository;
import com.footstyle.demo.repository.ThuongHieuRepository;
import com.footstyle.demo.repository.TrongLuongRepository;
import com.footstyle.demo.repository.XuatXuRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;






@Service
public class ThuocTinhService {

    
    private final Map<String, ThuocTinhRepository<ThuocTinh>> repos = new HashMap<>();
    
    private final Map<String, Supplier<? extends ThuocTinh>> taoMoi = new HashMap<>();
    
    private final Map<String, String> nhan = new HashMap<>();
    
    private final Map<String, String> tienTo = new HashMap<>();

    public ThuocTinhService(DanhMucRepository danhMuc, ThuongHieuRepository thuongHieu, XuatXuRepository xuatXu,
                            ChatLieuRepository chatLieu, DoCungRepository doCung, DiemCanBangRepository diemCanBang,
                            MauSacRepository mauSac, TrongLuongRepository trongLuong, ChuViRepository chuVi) {
        dangKy("danh-muc", "danh mục", "DM", danhMuc, DanhMuc::new);
        dangKy("thuong-hieu", "thương hiệu", "TH", thuongHieu, ThuongHieu::new);
        dangKy("xuat-xu", "xuất xứ", "XX", xuatXu, XuatXu::new);
        dangKy("chat-lieu", "chất liệu", "CL", chatLieu, ChatLieu::new);
        dangKy("do-cung", "độ cứng", "DC", doCung, DoCung::new);
        dangKy("diem-can-bang", "điểm cân bằng", "CB", diemCanBang, DiemCanBang::new);
        dangKy("mau-sac", "màu sắc", "MS", mauSac, MauSac::new);
        dangKy("trong-luong", "trọng lượng", "TL", trongLuong, TrongLuong::new);
        dangKy("chu-vi", "chu vi", "CV", chuVi, ChuVi::new);
    }

    
    
    @SuppressWarnings("unchecked")
    private void dangKy(String loai, String ten, String prefix, ThuocTinhRepository<? extends ThuocTinh> repo,
                        Supplier<? extends ThuocTinh> tao) {
        repos.put(loai, (ThuocTinhRepository<ThuocTinh>) repo);
        taoMoi.put(loai, tao);
        nhan.put(loai, ten);
        tienTo.put(loai, prefix);
    }

    

    @Transactional(readOnly = true)
    public List<ThuocTinhResponse> getAll(String loai) {
        List<ThuocTinhResponse> ketQua = new ArrayList<>();
        for (ThuocTinh t : repo(loai).findAllByOrderByIdDesc()) {
            ketQua.add(toResponse(t));
        }
        return ketQua;
    }

    

    @Transactional
    public ThuocTinhResponse them(String loai, ThuocTinhRequest req) {
        ThuocTinhRepository<ThuocTinh> repo = repo(loai);
        String ten = kiemTraTen(loai, req.ten(), -1);

        
        ThuocTinh t = taoMoi.get(loai).get();
        t.setMa(taoMaMoi(loai));
        t.setTen(ten);
        t.setTrangThai(ThuocTinh.HOAT_DONG);
        return toResponse(repo.save(t));
    }

    
    @Transactional
    public ThuocTinhResponse sua(String loai, Integer id, ThuocTinhRequest req) {
        ThuocTinhRepository<ThuocTinh> repo = repo(loai);
        ThuocTinh t = tim(loai, id);
        t.setTen(kiemTraTen(loai, req.ten(), id));
        return toResponse(repo.save(t));
    }

    
    @Transactional
    public ThuocTinhResponse doiTrangThai(String loai, Integer id) {
        ThuocTinhRepository<ThuocTinh> repo = repo(loai);
        ThuocTinh t = tim(loai, id);
        if (dangHoatDong(t)) {
            t.setTrangThai(ThuocTinh.NGUNG_HOAT_DONG);
        } else {
            t.setTrangThai(ThuocTinh.HOAT_DONG);
        }
        return toResponse(repo.save(t));
    }

    

    private ThuocTinhRepository<ThuocTinh> repo(String loai) {
        ThuocTinhRepository<ThuocTinh> repo = repos.get(loai);
        if (repo == null) {
            throw ApiException.notFound("Loại thuộc tính không hợp lệ.");
        }
        return repo;
    }

    private ThuocTinh tim(String loai, Integer id) {
        return repo(loai).findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy " + nhan.get(loai) + "."));
    }

    private boolean dangHoatDong(ThuocTinh t) {
        return t.getTrangThai() != null && t.getTrangThai() == ThuocTinh.HOAT_DONG;
    }

    
    private String kiemTraTen(String loai, String tenGui, Integer idBoQua) {
        String ten = tenGui == null ? "" : tenGui.trim().replaceAll("\\s+", " ");
        loi(ten.isEmpty(), "Nhập tên " + nhan.get(loai) + ".");
        loi(ten.length() > 100, "Tên tối đa 100 ký tự.");
        if (repo(loai).existsByTenIgnoreCaseAndIdNot(ten, idBoQua)) {
            throw ApiException.conflict("Tên " + nhan.get(loai) + " đã tồn tại.");
        }
        return ten;
    }

    
    private String taoMaMoi(String loai) {
        int max = 0;
        for (ThuocTinh t : repo(loai).findAll()) {
            String so = t.getMa() == null ? "" : t.getMa().replaceAll("\\D", ""); 
            if (!so.isEmpty() && so.length() <= 9) {
                max = Math.max(max, Integer.parseInt(so));
            }
        }
        return String.format("%s%03d", tienTo.get(loai), max + 1);
    }

    
    private void loi(boolean dieuKien, String thongBao) {
        if (dieuKien) {
            throw ApiException.badRequest(thongBao);
        }
    }

    private ThuocTinhResponse toResponse(ThuocTinh t) {
        return new ThuocTinhResponse(t.getId(), t.getMa(), t.getTen(), dangHoatDong(t));
    }
}
