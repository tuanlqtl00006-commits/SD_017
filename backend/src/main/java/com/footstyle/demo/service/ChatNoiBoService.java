package com.footstyle.demo.service;

import com.footstyle.demo.entity.HoiThoai;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.ThanhVienHoiThoai;
import com.footstyle.demo.entity.TinNhan;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.HoiThoaiRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.ThanhVienHoiThoaiRepository;
import com.footstyle.demo.repository.TinNhanRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat nội bộ giữa nhân viên: nhóm chung "Cả cửa hàng" (mọi nhân viên tự vào), chat riêng 1-1, và nhóm tự tạo.
 * Chỉ thành viên mới xem / gửi được tin của một cuộc chat. Đánh dấu đã đọc theo từng người (thanh_vien_hoi_thoai.id_tin_nhan_da_doc).
 */
@Service
public class ChatNoiBoService {

    public record MoRiengRequest(Integer idNhanVien) {
    }

    public record TaoNhomRequest(String tieuDe, List<Integer> idNhanViens) {
    }

    public record NoiDungRequest(String noiDung) {
    }

    static final String TEN_NHOM_CHUNG = "Cả cửa hàng";

    private final HoiThoaiRepository hoiThoaiRepo;
    private final ThanhVienHoiThoaiRepository thanhVienRepo;
    private final TinNhanRepository tinNhanRepo;
    private final NhanVienRepository nhanVienRepo;
    private Clock dongHo = Clock.systemDefaultZone();

    public ChatNoiBoService(HoiThoaiRepository hoiThoaiRepo, ThanhVienHoiThoaiRepository thanhVienRepo, TinNhanRepository tinNhanRepo,
                            NhanVienRepository nhanVienRepo) {
        this.hoiThoaiRepo = hoiThoaiRepo;
        this.thanhVienRepo = thanhVienRepo;
        this.tinNhanRepo = tinNhanRepo;
        this.nhanVienRepo = nhanVienRepo;
    }

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now(dongHo).withNano(0);
    }

    /** Các cuộc chat nội bộ của mình (mới hoạt động lên đầu), kèm tin cuối và số tin chưa đọc. */
    @Transactional
    public List<Map<String, Object>> danhSach() {
        NguoiDung nd = XacThuc.nhanVien();
        damBaoNhomChung(nd.id());
        List<ThanhVienHoiThoai> cuaToi = thanhVienRepo.findByIdNhanVien(nd.id());
        Map<Integer, ThanhVienHoiThoai> theoHoiThoai = new HashMap<>();
        for (ThanhVienHoiThoai tv : cuaToi) theoHoiThoai.put(tv.getIdHoiThoai(), tv);
        if (theoHoiThoai.isEmpty()) return List.of();
        Map<Integer, NhanVien> nhanVien = new HashMap<>();
        for (NhanVien nv : nhanVienRepo.findAll()) nhanVien.put(nv.getId(), nv);
        List<Map<String, Object>> kq = new ArrayList<>();
        for (HoiThoai h : hoiThoaiRepo.findByIdIn(new ArrayList<>(theoHoiThoai.keySet()))) {
            if (Integer.valueOf(HoiThoai.HO_TRO_KHACH).equals(h.getLoai())) continue; // mốc đã đọc của chat hỗ trợ khách
            ThanhVienHoiThoai tv = theoHoiThoai.get(h.getId());
            Map<String, Object> m = tomTat(h, nd.id(), nhanVien);
            m.put("chuaDoc", tinNhanRepo.demChuaDoc(h.getId(), moc(tv), nd.id()));
            kq.add(m);
        }
        // Nhóm chung luôn đứng đầu, sau đó cuộc nào có tin mới nhất lên trước
        kq.sort(Comparator.comparing((Map<String, Object> m) -> !Boolean.TRUE.equals(m.get("nhomChung")))
                .thenComparing(m -> (LocalDateTime) m.get("ngayCapNhat"), Comparator.nullsLast(Comparator.reverseOrder())));
        return kq;
    }

    /** Nhân viên đang hoạt động (trừ mình) để chọn chat riêng / thêm vào nhóm. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> nhanVien() {
        NguoiDung nd = XacThuc.nhanVien();
        List<Map<String, Object>> kq = new ArrayList<>();
        for (NhanVien nv : nhanVienRepo.findByTrangThaiOrderByHoTenAsc(NhanVien.HOAT_DONG)) {
            if (nv.getId().equals(nd.id())) continue;
            kq.add(nhanVienDto(nv));
        }
        return kq;
    }

    /** Mở (hoặc tạo mới) cuộc chat riêng với một nhân viên. Mỗi cặp chỉ có 1 cuộc (khóa "R:idNhỏ-idLớn"). */
    @Transactional
    public Map<String, Object> moRieng(MoRiengRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        Integer idKia = req == null ? null : req.idNhanVien();
        if (idKia == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Hãy chọn nhân viên.", null, null, Map.of("idNhanVien", "Hãy chọn nhân viên."));
        if (idKia.equals(nd.id())) throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể tự chat với chính mình.", null, null,
                Map.of("idNhanVien", "Không thể tự chat với chính mình."));
        NhanVien kia = nhanVienRepo.findById(idKia).filter(nv -> Integer.valueOf(NhanVien.HOAT_DONG).equals(nv.getTrangThai()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Nhân viên không tồn tại hoặc đã nghỉ.", null, null,
                        Map.of("idNhanVien", "Nhân viên không tồn tại hoặc đã nghỉ.")));
        String khoa = khoaRieng(nd.id(), kia.getId());
        HoiThoai h = hoiThoaiRepo.findFirstByKhoaRieng(khoa).orElseGet(() -> {
            HoiThoai moi = new HoiThoai();
            moi.setLoai(HoiThoai.NOI_BO_RIENG);
            moi.setCheDo(HoiThoai.TRO_LY_AO);
            moi.setKhoaRieng(khoa);
            moi.setNgayTao(bayGio());
            moi.setNgayCapNhat(bayGio());
            return hoiThoaiRepo.save(moi);
        });
        themThanhVien(h.getId(), nd.id(), 0);
        themThanhVien(h.getId(), kia.getId(), 0);
        return xem(h.getId(), null);
    }

    static String khoaRieng(Integer a, Integer b) {
        return "R:" + Math.min(a, b) + "-" + Math.max(a, b);
    }

    @Transactional
    public Map<String, Object> taoNhom(TaoNhomRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        String tieuDe = QuyTacDuLieu.cat(req == null ? null : req.tieuDe()).replaceAll("\\s+", " ");
        Set<Integer> ids = new LinkedHashSet<>();
        if (req != null && req.idNhanViens() != null) {
            for (Integer id : req.idNhanViens()) if (id != null && !id.equals(nd.id())) ids.add(id);
        }
        LoiForm loi = new LoiForm();
        loi.neu(tieuDe.isEmpty(), "tieuDe", "Hãy đặt tên nhóm.");
        loi.neu(tieuDe.length() > 100, "tieuDe", "Tên nhóm tối đa 100 ký tự.");
        loi.neu(ids.isEmpty(), "idNhanViens", "Hãy chọn ít nhất 1 nhân viên khác.");
        List<NhanVien> thanhVien = new ArrayList<>();
        for (Integer id : ids) {
            Optional<NhanVien> nv = nhanVienRepo.findById(id).filter(x -> Integer.valueOf(NhanVien.HOAT_DONG).equals(x.getTrangThai()));
            if (nv.isEmpty()) {
                loi.them("idNhanViens", "Có nhân viên không tồn tại hoặc đã nghỉ, hãy chọn lại.");
                break;
            }
            thanhVien.add(nv.get());
        }
        loi.nemNeuCoLoi();
        LocalDateTime now = bayGio();
        HoiThoai h = new HoiThoai();
        h.setLoai(HoiThoai.NOI_BO_NHOM);
        h.setCheDo(HoiThoai.TRO_LY_AO);
        h.setTieuDe(tieuDe);
        h.setNgayTao(now);
        h.setNgayCapNhat(now);
        h = hoiThoaiRepo.save(h);
        themThanhVien(h.getId(), nd.id(), 0);
        for (NhanVien nv : thanhVien) themThanhVien(h.getId(), nv.getId(), 0);
        luuTin(h.getId(), TinNhan.HE_THONG, null, null, nd.hoTen() + " đã tạo nhóm \"" + tieuDe + "\".", now);
        return xem(h.getId(), null);
    }

    /** Tin nhắn của một cuộc chat (lần đầu 100 tin gần nhất, sau đó chỉ tin mới hơn sauId) + đánh dấu đã đọc. */
    @Transactional
    public Map<String, Object> xem(Integer id, Integer sauId) {
        NguoiDung nd = XacThuc.nhanVien();
        HoiThoai h = hoiThoaiNoiBo(id);
        ThanhVienHoiThoai tv = laThanhVien(h, nd.id());
        List<TinNhan> tin = ChatChung.tin(tinNhanRepo, h.getId(), sauId);
        if (!tin.isEmpty() && ChatChung.daDocToi(tv, tin.get(tin.size() - 1).getId())) thanhVienRepo.save(tv);
        Map<Integer, NhanVien> nhanVien = new HashMap<>();
        for (NhanVien nv : nhanVienRepo.findAll()) nhanVien.put(nv.getId(), nv);
        Map<String, Object> m = tomTat(h, nd.id(), nhanVien);
        m.put("tinNhan", ChatChung.dto(tin, false));
        return m;
    }

    @Transactional
    public Map<String, Object> gui(Integer id, NoiDungRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        HoiThoai h = hoiThoaiNoiBo(id);
        ThanhVienHoiThoai tv = laThanhVien(h, nd.id());
        String noiDung = ChatChung.noiDung(req == null ? null : req.noiDung());
        LocalDateTime now = bayGio();
        TinNhan t = luuTin(h.getId(), TinNhan.NHAN_VIEN, nd.id(), nd.hoTen(), noiDung, now);
        h.setNgayCapNhat(now);
        hoiThoaiRepo.save(h);
        if (ChatChung.daDocToi(tv, t.getId())) thanhVienRepo.save(tv);
        return ChatChung.dto(t, false);
    }

    /** Tổng số tin nội bộ chưa đọc của nhân viên (hiện trên menu / chuông). */
    @Transactional
    public long soChuaDoc(Integer idNv) {
        damBaoNhomChung(idNv);
        long n = 0;
        List<ThanhVienHoiThoai> ds = thanhVienRepo.findByIdNhanVien(idNv);
        if (ds.isEmpty()) return 0;
        Map<Integer, HoiThoai> hoiThoai = new HashMap<>();
        for (HoiThoai h : hoiThoaiRepo.findByIdIn(ds.stream().map(ThanhVienHoiThoai::getIdHoiThoai).toList())) hoiThoai.put(h.getId(), h);
        for (ThanhVienHoiThoai tv : ds) {
            HoiThoai h = hoiThoai.get(tv.getIdHoiThoai());
            if (h == null || Integer.valueOf(HoiThoai.HO_TRO_KHACH).equals(h.getLoai())) continue;
            n += tinNhanRepo.demChuaDoc(h.getId(), moc(tv), idNv);
        }
        return n;
    }

    /** Nhóm chung "Cả cửa hàng": chưa có thì tạo; nhân viên chưa trong nhóm thì thêm vào, coi như đã đọc các tin cũ. */
    void damBaoNhomChung(Integer idNv) {
        HoiThoai h = hoiThoaiRepo.findFirstByKhoaRieng(HoiThoai.KHOA_NHOM_CHUNG).orElseGet(() -> {
            HoiThoai moi = new HoiThoai();
            moi.setLoai(HoiThoai.NOI_BO_NHOM);
            moi.setCheDo(HoiThoai.TRO_LY_AO);
            moi.setTieuDe(TEN_NHOM_CHUNG);
            moi.setKhoaRieng(HoiThoai.KHOA_NHOM_CHUNG);
            moi.setNgayTao(bayGio());
            moi.setNgayCapNhat(bayGio());
            return hoiThoaiRepo.save(moi);
        });
        if (thanhVienRepo.findFirstByIdHoiThoaiAndIdNhanVien(h.getId(), idNv).isEmpty()) {
            int tinCuoi = tinNhanRepo.findFirstByIdHoiThoaiOrderByIdDesc(h.getId()).map(TinNhan::getId).orElse(0);
            themThanhVien(h.getId(), idNv, tinCuoi);
        }
    }

    private void themThanhVien(Integer idHoiThoai, Integer idNv, int daDocToi) {
        if (thanhVienRepo.findFirstByIdHoiThoaiAndIdNhanVien(idHoiThoai, idNv).isPresent()) return;
        ThanhVienHoiThoai tv = new ThanhVienHoiThoai();
        tv.setIdHoiThoai(idHoiThoai);
        tv.setIdNhanVien(idNv);
        tv.setIdTinNhanDaDoc(daDocToi);
        tv.setNgayThamGia(bayGio());
        thanhVienRepo.save(tv);
    }

    private HoiThoai hoiThoaiNoiBo(Integer id) {
        return hoiThoaiRepo.findById(id == null ? -1 : id)
                .filter(h -> !Integer.valueOf(HoiThoai.HO_TRO_KHACH).equals(h.getLoai()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cuộc trò chuyện."));
    }

    private ThanhVienHoiThoai laThanhVien(HoiThoai h, Integer idNv) {
        if (HoiThoai.KHOA_NHOM_CHUNG.equals(h.getKhoaRieng())) damBaoNhomChung(idNv);
        return thanhVienRepo.findFirstByIdHoiThoaiAndIdNhanVien(h.getId(), idNv)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Bạn không ở trong cuộc trò chuyện này."));
    }

    private static int moc(ThanhVienHoiThoai tv) {
        return tv.getIdTinNhanDaDoc() == null ? 0 : tv.getIdTinNhanDaDoc();
    }

    private Map<String, Object> tomTat(HoiThoai h, Integer idToi, Map<Integer, NhanVien> nhanVien) {
        List<Map<String, Object>> tv = new ArrayList<>();
        String tenKia = null;
        for (ThanhVienHoiThoai t : thanhVienRepo.findByIdHoiThoai(h.getId())) {
            NhanVien nv = nhanVien.get(t.getIdNhanVien());
            if (nv == null) continue;
            tv.add(nhanVienDto(nv));
            if (!nv.getId().equals(idToi)) tenKia = nv.getHoTen();
        }
        tv.sort(Comparator.comparing(m -> String.valueOf(m.get("hoTen"))));
        boolean rieng = Integer.valueOf(HoiThoai.NOI_BO_RIENG).equals(h.getLoai());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("loai", h.getLoai());
        m.put("nhomChung", HoiThoai.KHOA_NHOM_CHUNG.equals(h.getKhoaRieng()));
        m.put("tieuDe", rieng ? (tenKia == null ? "(Nhân viên đã bị xóa)" : tenKia) : h.getTieuDe());
        m.put("thanhVien", tv);
        m.put("ngayCapNhat", h.getNgayCapNhat());
        m.put("tinCuoi", ChatChung.tinCuoi(tinNhanRepo.findFirstByIdHoiThoaiOrderByIdDesc(h.getId()).orElse(null)));
        return m;
    }

    private static Map<String, Object> nhanVienDto(NhanVien nv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", nv.getId());
        m.put("ma", nv.getMaNhanVien());
        m.put("hoTen", nv.getHoTen());
        m.put("vaiTro", nv.getVaiTro() == null ? null : nv.getVaiTro().getTenVaiTro());
        m.put("dangLam", Integer.valueOf(NhanVien.HOAT_DONG).equals(nv.getTrangThai()));
        return m;
    }

    private TinNhan luuTin(Integer idHoiThoai, int loai, Integer idNv, String ten, String noiDung, LocalDateTime luc) {
        TinNhan t = new TinNhan();
        t.setIdHoiThoai(idHoiThoai);
        t.setLoaiNguoiGui(loai);
        t.setIdNhanVien(idNv);
        t.setTenNguoiGui(ten);
        t.setNoiDung(noiDung);
        t.setThoiGian(luc);
        return tinNhanRepo.save(t);
    }
}
