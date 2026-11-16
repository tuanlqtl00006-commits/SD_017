package com.footstyle.demo.service;

import com.footstyle.demo.entity.HoiThoai;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.ThanhVienHoiThoai;
import com.footstyle.demo.entity.TinNhan;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.HoiThoaiRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.ThanhVienHoiThoaiRepository;
import com.footstyle.demo.repository.TinNhanRepository;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat hỗ trợ khách hàng. Khách KHÔNG cần đăng nhập: nhập họ tên + SĐT để bắt đầu, nhận về mã truy cập bí mật
 * (giữ trong trình duyệt) - mọi lần xem / gửi tin sau đều phải kèm mã này, nên người khác không đọc được cuộc chat.
 *
 * Luồng: trợ lý ảo (TroLyAoService) trả lời trước -> khách bấm "Gặp nhân viên" (hoặc trợ lý tự chuyển) -> CHỜ NHÂN VIÊN
 * -> một nhân viên bấm "Nhận hỗ trợ" (hoặc trả lời luôn) -> NHÂN VIÊN HỖ TRỢ -> nhân viên / khách kết thúc -> ĐÃ ĐÓNG.
 */
@Service
public class HoTroService {

    public record BatDauRequest(String hoTen, String sdt) {
    }

    public record TinKhachRequest(String ma, String noiDung) {
    }

    public record MaRequest(String ma) {
    }

    /** Đánh giá sau phiên chat có nhân viên: mucDo TOT / BINH_THUONG / CHUA_TOT, đã giải quyết được yêu cầu chưa, góp ý (không bắt buộc). */
    public record DanhGiaRequest(String ma, String mucDo, Boolean daGiaiQuyet, String gopY) {
    }

    public record NoiDungRequest(String noiDung) {
    }

    /** Mỗi SĐT mở tối đa 5 cuộc chat / giờ; khách gửi tối đa 20 tin / phút (chống spam vì trang chat công khai). */
    static final int SO_HOI_THOAI_MOI_GIO = 5;
    static final int SO_TIN_KHACH_MOI_PHUT = 20;
    static final String TEN_TRO_LY = "Trợ lý ảo";
    /** Cô review 14/11 [20:24–23:17]: phiên chat tự đóng sau khoảng 30 phút không có tin nhắn mới. */
    public static final int TU_DONG_DONG_PHUT = 30;
    /** Tin hệ thống ghi kết quả đánh giá bắt đầu bằng chữ này (dùng để biết khách đã đánh giá chưa, không cần thêm cột). */
    public static final String MO_DAU_DANH_GIA = "Khách đánh giá: ";
    static final Map<String, String> MUC_DO = Map.of("TOT", "Tốt", "BINH_THUONG", "Bình thường", "CHUA_TOT", "Chưa tốt");
    private static final List<String> THU_TU_MUC = List.of("TOT", "BINH_THUONG", "CHUA_TOT");

    private final HoiThoaiRepository hoiThoaiRepo;
    private final TinNhanRepository tinNhanRepo;
    private final ThanhVienHoiThoaiRepository thanhVienRepo;
    private final NhanVienRepository nhanVienRepo;
    private final KhachHangRepository khachHangRepo;
    private final TroLyAoService troLy;
    private final SecureRandom ngauNhien = new SecureRandom();
    private Clock dongHo = Clock.systemDefaultZone();

    public HoTroService(HoiThoaiRepository hoiThoaiRepo, TinNhanRepository tinNhanRepo, ThanhVienHoiThoaiRepository thanhVienRepo,
                        NhanVienRepository nhanVienRepo, KhachHangRepository khachHangRepo, TroLyAoService troLy) {
        this.hoiThoaiRepo = hoiThoaiRepo;
        this.tinNhanRepo = tinNhanRepo;
        this.thanhVienRepo = thanhVienRepo;
        this.nhanVienRepo = nhanVienRepo;
        this.khachHangRepo = khachHangRepo;
        this.troLy = troLy;
    }

    void datDongHo(Clock dongHo) {
        this.dongHo = dongHo;
    }

    private LocalDateTime bayGio() {
        return LocalDateTime.now(dongHo).withNano(0);
    }

    /* ============================ PHÍA KHÁCH (công khai) ============================ */

    @Transactional
    public Map<String, Object> batDau(BatDauRequest req) {
        String hoTen = QuyTacDuLieu.chuanHoaHoTen(req == null ? null : req.hoTen());
        String sdt = QuyTacDuLieu.cat(req == null ? null : req.sdt()).replaceAll("[\\s.]", "");
        LoiForm loi = new LoiForm();
        loi.neu(hoTen.isEmpty(), "hoTen", "Hãy nhập họ tên.");
        loi.neu(hoTen.length() > 100, "hoTen", "Họ tên tối đa 100 ký tự.");
        loi.neu(!hoTen.isEmpty() && !QuyTacDuLieu.HO_TEN.matcher(hoTen).matches(), "hoTen", QuyTacDuLieu.LOI_HO_TEN_KY_TU);
        loi.neu(sdt.isEmpty(), "sdt", "Hãy nhập số điện thoại để nhân viên liên hệ lại khi cần.");
        loi.neu(!sdt.isEmpty() && !QuyTacDuLieu.SDT.matcher(sdt).matches(), "sdt", QuyTacDuLieu.LOI_SDT);
        loi.nemNeuCoLoi();

        LocalDateTime now = bayGio();
        if (hoiThoaiRepo.countByLoaiAndSdtKhachAndNgayTaoAfter(HoiThoai.HO_TRO_KHACH, sdt, now.minusHours(1)) >= SO_HOI_THOAI_MOI_GIO) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Số điện thoại này đã mở nhiều cuộc trò chuyện trong 1 giờ qua. Bạn vui lòng tiếp tục cuộc trò chuyện cũ hoặc thử lại sau.",
                    "QUA_NHIEU_YEU_CAU", null);
        }
        HoiThoai h = new HoiThoai();
        h.setLoai(HoiThoai.HO_TRO_KHACH);
        h.setTieuDe(hoTen);
        h.setTenKhach(hoTen);
        h.setSdtKhach(sdt);
        h.setMaTruyCap(taoMa());
        h.setCheDo(HoiThoai.TRO_LY_AO);
        h.setIdKhachHang(timKhachHang(sdt));
        h.setNgayTao(now);
        h.setNgayCapNhat(now);
        h = hoiThoaiRepo.save(h);

        TroLyAoService.TraLoi chao = troLy.loiChao(hoTen);
        TinNhan t = luuTin(h, TinNhan.TRO_LY_AO, null, TEN_TRO_LY, chao.noiDung(), now);
        Map<String, Object> kq = phienKhach(h, List.of(t), chao.goiY());
        kq.put("ma", h.getMaTruyCap()); // chỉ trả mã truy cập đúng 1 lần, lúc bắt đầu
        return kq;
    }

    /** Khách đã đăng nhập (trang bán hàng online) thì gắn đúng tài khoản; chưa thì dò theo SĐT. */
    private Integer timKhachHang(String sdt) {
        Optional<NguoiDung> nd = XacThuc.hienTai();
        if (nd.isPresent() && nd.get().laKhachHang()) return nd.get().id();
        return khachHangRepo.findFirstBySdt(sdt).map(KhachHang::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> xemKhach(Integer id, String ma, Integer sauId) {
        HoiThoai h = hoiThoaiCuaKhach(id, ma);
        return phienKhach(h, ChatChung.tin(tinNhanRepo, h.getId(), sauId),
                Integer.valueOf(HoiThoai.TRO_LY_AO).equals(h.getCheDo()) && (sauId == null || sauId <= 0) ? TroLyAoService.GOI_Y_MAC_DINH : List.of());
    }

    @Transactional
    public Map<String, Object> guiKhach(Integer id, TinKhachRequest req) {
        HoiThoai h = hoiThoaiCuaKhach(id, req == null ? null : req.ma());
        chuaDong(h);
        String noiDung = ChatChung.noiDung(req.noiDung());
        LocalDateTime now = bayGio();
        if (tinNhanRepo.countByIdHoiThoaiAndLoaiNguoiGuiAndThoiGianAfter(h.getId(), TinNhan.KHACH, now.minusMinutes(1)) >= SO_TIN_KHACH_MOI_PHUT) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn gửi tin nhanh quá, vui lòng chờ một chút rồi gửi tiếp.", "QUA_NHIEU_YEU_CAU", null);
        }
        List<TinNhan> moi = new ArrayList<>();
        moi.add(luuTin(h, TinNhan.KHACH, null, h.getTenKhach(), noiDung, now));
        List<String> goiY = List.of();
        if (Integer.valueOf(HoiThoai.TRO_LY_AO).equals(h.getCheDo())) {
            TroLyAoService.TraLoi tl = troLy.traLoi(noiDung, new TroLyAoService.NguCanh(h.getTenKhach(), h.getSdtKhach()));
            moi.add(luuTin(h, TinNhan.TRO_LY_AO, null, TEN_TRO_LY, tl.noiDungLuu(), now));
            if (tl.chuyenNhanVien()) h.setCheDo(HoiThoai.CHO_NHAN_VIEN);
            goiY = tl.goiY();
        }
        h.setNgayCapNhat(now);
        hoiThoaiRepo.save(h);
        return phienKhach(h, moi, goiY);
    }

    @Transactional
    public Map<String, Object> gapNhanVien(Integer id, MaRequest req) {
        HoiThoai h = hoiThoaiCuaKhach(id, req == null ? null : req.ma());
        chuaDong(h);
        List<TinNhan> moi = new ArrayList<>();
        if (Integer.valueOf(HoiThoai.TRO_LY_AO).equals(h.getCheDo())) {
            LocalDateTime now = bayGio();
            h.setCheDo(HoiThoai.CHO_NHAN_VIEN);
            h.setNgayCapNhat(now);
            moi.add(luuTin(h, TinNhan.HE_THONG, null, null,
                    "Đang kết nối với nhân viên. Bạn cứ nhắn câu hỏi, nhân viên sẽ trả lời ngay tại đây.", now));
            hoiThoaiRepo.save(h);
        }
        return phienKhach(h, moi, List.of());
    }

    @Transactional
    public Map<String, Object> ketThucKhach(Integer id, MaRequest req) {
        HoiThoai h = hoiThoaiCuaKhach(id, req == null ? null : req.ma());
        List<TinNhan> moi = new ArrayList<>();
        if (!Integer.valueOf(HoiThoai.DA_DONG).equals(h.getCheDo())) {
            LocalDateTime now = bayGio();
            h.setCheDo(HoiThoai.DA_DONG);
            h.setNgayCapNhat(now);
            moi.add(luuTin(h, TinNhan.HE_THONG, null, null, "Khách đã kết thúc cuộc trò chuyện.", now));
            hoiThoaiRepo.save(h);
        }
        return phienKhach(h, moi, List.of());
    }

    /**
     * Khách đánh giá cuộc trò chuyện ĐÃ KẾT THÚC có nhân viên tham gia (chỉ trợ lý ảo thì không cần đánh giá), mỗi cuộc 1 lần.
     * Kết quả lưu thành tin hệ thống "Khách đánh giá: ..." nên nhân viên / quản lý xem lại được ngay trong cuộc chat.
     */
    @Transactional
    public Map<String, Object> danhGia(Integer id, DanhGiaRequest req) {
        HoiThoai h = hoiThoaiCuaKhach(id, req == null ? null : req.ma());
        if (!Integer.valueOf(HoiThoai.DA_DONG).equals(h.getCheDo())) throw ApiException.conflict("Cuộc trò chuyện chưa kết thúc.");
        if (h.getIdNhanVienPhuTrach() == null) throw ApiException.conflict("Cuộc trò chuyện chỉ có trợ lý ảo, không cần đánh giá.");
        if (daDanhGia(h)) throw ApiException.conflict("Bạn đã đánh giá cuộc trò chuyện này rồi. Cảm ơn bạn!");
        LoiForm loi = new LoiForm();
        String muc = req.mucDo() == null ? null : MUC_DO.get(req.mucDo().trim().toUpperCase(java.util.Locale.ROOT));
        loi.neu(muc == null, "mucDo", "Hãy chọn mức độ hài lòng.");
        loi.neu(req.daGiaiQuyet() == null, "daGiaiQuyet", "Hãy cho biết yêu cầu của bạn đã được giải quyết chưa.");
        String gopY = QuyTacDuLieu.cat(req.gopY());
        loi.neu(gopY.length() > 300, "gopY", "Góp ý tối đa 300 ký tự.");
        loi.nemNeuCoLoi();
        String noiDung = MO_DAU_DANH_GIA + muc + " · " + (req.daGiaiQuyet() ? "Đã giải quyết yêu cầu" : "Chưa giải quyết yêu cầu")
                + (gopY.isEmpty() ? "" : " · Góp ý: " + gopY);
        TinNhan t = luuTin(h, TinNhan.HE_THONG, null, null, noiDung, bayGio());
        return phienKhach(h, List.of(t), List.of());
    }

    private boolean daDanhGia(HoiThoai h) {
        return ChatChung.tin(tinNhanRepo, h.getId(), null).stream()
                .anyMatch(t -> Integer.valueOf(TinNhan.HE_THONG).equals(t.getLoaiNguoiGui()) && t.getNoiDung() != null && t.getNoiDung().startsWith(MO_DAU_DANH_GIA));
    }

    /**
     * Tự đóng các cuộc chat không có tin nhắn mới trong {@link #TU_DONG_DONG_PHUT} phút (chạy mỗi phút).
     * Có nhân viên tham gia thì khách được mời đánh giá; chỉ trợ lý ảo thì đóng luôn, không cần đánh giá.
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    @Transactional
    public int tuDongDongPhienRanh() {
        LocalDateTime now = bayGio();
        int dem = 0;
        for (HoiThoai h : hoiThoaiRepo.findByLoaiAndCheDoNotAndNgayCapNhatBefore(HoiThoai.HO_TRO_KHACH, HoiThoai.DA_DONG, now.minusMinutes(TU_DONG_DONG_PHUT).plusSeconds(1))) {
            h.setCheDo(HoiThoai.DA_DONG);
            h.setNgayCapNhat(now);
            luuTin(h, TinNhan.HE_THONG, null, null, "Cuộc trò chuyện tự kết thúc vì không có tin nhắn mới trong " + TU_DONG_DONG_PHUT + " phút."
                    + (h.getIdNhanVienPhuTrach() != null ? " Bạn đánh giá giúp cửa hàng chất lượng hỗ trợ nhé." : ""), now);
            hoiThoaiRepo.save(h);
            dem++;
        }
        return dem;
    }

    /** Sai id hay sai mã đều báo "không tìm thấy" như nhau (không cho dò xem id nào đang tồn tại). */
    HoiThoai hoiThoaiCuaKhach(Integer id, String ma) {
        Optional<HoiThoai> h = id == null ? Optional.empty() : hoiThoaiRepo.findById(id);
        if (h.isEmpty() || !Integer.valueOf(HoiThoai.HO_TRO_KHACH).equals(h.get().getLoai()) || !dungMa(h.get().getMaTruyCap(), ma)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc trò chuyện. Bạn hãy bắt đầu cuộc trò chuyện mới.",
                    "KHONG_TIM_THAY_HOI_THOAI", null);
        }
        return h.get();
    }

    static boolean dungMa(String dung, String ma) {
        if (dung == null || ma == null) return false;
        return MessageDigest.isEqual(dung.getBytes(StandardCharsets.UTF_8), ma.getBytes(StandardCharsets.UTF_8));
    }

    private static void chuaDong(HoiThoai h) {
        if (Integer.valueOf(HoiThoai.DA_DONG).equals(h.getCheDo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Cuộc trò chuyện đã kết thúc. Bạn hãy bắt đầu cuộc trò chuyện mới.", "DA_KET_THUC", null);
        }
    }

    private Map<String, Object> phienKhach(HoiThoai h, List<TinNhan> tin, List<String> goiY) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("cheDo", h.getCheDo());
        m.put("tenKhach", h.getTenKhach());
        m.put("tenNhanVien", tenNhanVien(h.getIdNhanVienPhuTrach()));
        m.put("tinNhan", ChatChung.dto(tin, true));
        m.put("goiY", goiY == null ? List.of() : goiY);
        // Đã kết thúc + có nhân viên tham gia: mời khách đánh giá (1 lần)
        boolean dong = Integer.valueOf(HoiThoai.DA_DONG).equals(h.getCheDo());
        boolean daDg = dong && h.getIdNhanVienPhuTrach() != null && daDanhGia(h);
        m.put("daDanhGia", daDg);
        m.put("canDanhGia", dong && h.getIdNhanVienPhuTrach() != null && !daDg);
        return m;
    }

    private String taoMa() {
        byte[] b = new byte[24];
        ngauNhien.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /* ============================ PHÍA NHÂN VIÊN ============================ */

    /**
     * Danh sách cuộc chat hỗ trợ (200 cuộc mới nhất) kèm số lượng theo bộ lọc.
     * loc: "cho" (đang chờ nhân viên), "cua-toi" (mình đang hỗ trợ), "tro-ly" (trợ lý ảo đang trả lời), "da-dong"; trống = tất cả.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> danhSach(String loc) {
        NguoiDung nd = XacThuc.nhanVien();
        List<HoiThoai> tatCa = hoiThoaiRepo.findTop200ByLoaiOrderByNgayCapNhatDesc(HoiThoai.HO_TRO_KHACH);
        Map<Integer, String> tenNv = new HashMap<>();
        Map<String, Integer> dem = new LinkedHashMap<>();
        for (String k : List.of("tatCa", "cho", "cuaToi", "troLy", "daDong")) dem.put(k, 0);
        List<Map<String, Object>> ds = new ArrayList<>();
        for (HoiThoai h : tatCa) {
            String nhom = nhom(h, nd.id());
            dem.merge("tatCa", 1, Integer::sum);
            if (nhom != null) dem.merge(nhom, 1, Integer::sum);
            if (loc != null && !loc.isBlank() && !khopLoc(loc, nhom)) continue;
            Map<String, Object> m = tomTat(h, tenNv);
            m.put("chuaDoc", canTraLoi(h, nd.id()) ? chuaDoc(h, nd.id()) : 0);
            ds.add(m);
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("hoiThoai", ds);
        kq.put("dem", dem);
        return kq;
    }

    private static String nhom(HoiThoai h, Integer idNv) {
        int cd = h.getCheDo() == null ? HoiThoai.TRO_LY_AO : h.getCheDo();
        return switch (cd) {
            case HoiThoai.CHO_NHAN_VIEN -> "cho";
            case HoiThoai.NHAN_VIEN_HO_TRO -> idNv.equals(h.getIdNhanVienPhuTrach()) ? "cuaToi" : null;
            case HoiThoai.DA_DONG -> "daDong";
            default -> "troLy";
        };
    }

    private static boolean khopLoc(String loc, String nhom) {
        return switch (loc) {
            case "cho" -> "cho".equals(nhom);
            case "cua-toi" -> "cuaToi".equals(nhom);
            case "tro-ly" -> "troLy".equals(nhom);
            case "da-dong" -> "daDong".equals(nhom);
            default -> true;
        };
    }

    /** Cuộc chat mình cần để ý: đang chờ nhân viên, hoặc mình đang hỗ trợ. */
    private static boolean canTraLoi(HoiThoai h, Integer idNv) {
        return Integer.valueOf(HoiThoai.CHO_NHAN_VIEN).equals(h.getCheDo())
                || (Integer.valueOf(HoiThoai.NHAN_VIEN_HO_TRO).equals(h.getCheDo()) && idNv.equals(h.getIdNhanVienPhuTrach()));
    }

    private long chuaDoc(HoiThoai h, Integer idNv) {
        int moc = thanhVienRepo.findFirstByIdHoiThoaiAndIdNhanVien(h.getId(), idNv).map(ThanhVienHoiThoai::getIdTinNhanDaDoc).orElse(0);
        return tinNhanRepo.demChuaDoc(h.getId(), moc, idNv);
    }

    /** Số cuộc chat cần nhân viên này để ý: đang chờ nhân viên + cuộc mình hỗ trợ có tin chưa đọc. */
    @Transactional(readOnly = true)
    public int soCanXuLy(Integer idNv) {
        int n = 0;
        for (HoiThoai h : hoiThoaiRepo.findTop200ByLoaiOrderByNgayCapNhatDesc(HoiThoai.HO_TRO_KHACH)) {
            if (Integer.valueOf(HoiThoai.CHO_NHAN_VIEN).equals(h.getCheDo())) n++;
            else if (canTraLoi(h, idNv) && chuaDoc(h, idNv) > 0) n++;
        }
        return n;
    }

    @Transactional
    public Map<String, Object> xem(Integer id, Integer sauId) {
        NguoiDung nd = XacThuc.nhanVien();
        HoiThoai h = hoiThoaiHoTro(id);
        List<TinNhan> tin = ChatChung.tin(tinNhanRepo, h.getId(), sauId);
        if (!tin.isEmpty()) {
            ThanhVienHoiThoai tv = thanhVien(h.getId(), nd.id());
            if (ChatChung.daDocToi(tv, tin.get(tin.size() - 1).getId())) thanhVienRepo.save(tv);
        }
        Map<String, Object> m = tomTat(h, new HashMap<>());
        m.put("tinNhan", ChatChung.dto(tin, false));
        m.put("khachHang", khachHang(h.getIdKhachHang()));
        return m;
    }

    /** Nhận hỗ trợ cuộc chat: chuyển sang "Nhân viên hỗ trợ", trợ lý ảo ngừng trả lời. Quản lý được nhận lại cuộc của nhân viên khác. */
    @Transactional
    public Map<String, Object> nhan(Integer id) {
        NguoiDung nd = XacThuc.nhanVien();
        HoiThoai h = hoiThoaiHoTro(id);
        nhanNeuCan(h, nd, true);
        return xem(id, null);
    }

    @Transactional
    public Map<String, Object> gui(Integer id, NoiDungRequest req) {
        NguoiDung nd = XacThuc.nhanVien();
        HoiThoai h = hoiThoaiHoTro(id);
        String noiDung = ChatChung.noiDung(req == null ? null : req.noiDung());
        nhanNeuCan(h, nd, false);
        LocalDateTime now = bayGio();
        TinNhan t = luuTin(h, TinNhan.NHAN_VIEN, nd.id(), nd.hoTen(), noiDung, now);
        h.setNgayCapNhat(now);
        hoiThoaiRepo.save(h);
        ThanhVienHoiThoai tv = thanhVien(h.getId(), nd.id());
        if (ChatChung.daDocToi(tv, t.getId())) thanhVienRepo.save(tv);
        return ChatChung.dto(t, false);
    }

    @Transactional
    public Map<String, Object> dong(Integer id) {
        NguoiDung nd = XacThuc.nhanVien();
        HoiThoai h = hoiThoaiHoTro(id);
        if (!Integer.valueOf(HoiThoai.DA_DONG).equals(h.getCheDo())) {
            if (Integer.valueOf(HoiThoai.NHAN_VIEN_HO_TRO).equals(h.getCheDo()) && !nd.id().equals(h.getIdNhanVienPhuTrach()) && !nd.laQuanLy()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Cuộc trò chuyện đang do " + tenNhanVien(h.getIdNhanVienPhuTrach())
                        + " hỗ trợ, bạn không kết thúc được.", "NGUOI_KHAC_HO_TRO", null);
            }
            LocalDateTime now = bayGio();
            h.setCheDo(HoiThoai.DA_DONG);
            h.setNgayCapNhat(now);
            luuTin(h, TinNhan.HE_THONG, null, null, "Nhân viên " + nd.hoTen() + " đã kết thúc cuộc trò chuyện. Cảm ơn bạn đã liên hệ!", now);
            hoiThoaiRepo.save(h);
        }
        return xem(id, null);
    }

    /**
     * Cuộc đang ở trợ lý ảo / chờ nhân viên: nhận về mình (+ tin hệ thống). Đang do người khác hỗ trợ: chỉ quản lý được
     * nhận lại (`batBuoc` = bấm nút "Nhận hỗ trợ"), nhân viên thì báo lỗi 409. Đã đóng: 409.
     */
    private void nhanNeuCan(HoiThoai h, NguoiDung nd, boolean batBuoc) {
        chuaDong(h);
        boolean dangCuaNguoiKhac = Integer.valueOf(HoiThoai.NHAN_VIEN_HO_TRO).equals(h.getCheDo()) && !nd.id().equals(h.getIdNhanVienPhuTrach());
        if (Integer.valueOf(HoiThoai.NHAN_VIEN_HO_TRO).equals(h.getCheDo()) && !dangCuaNguoiKhac) return; // mình đang hỗ trợ
        if (dangCuaNguoiKhac && !(nd.laQuanLy() && batBuoc)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cuộc trò chuyện đang do " + tenNhanVien(h.getIdNhanVienPhuTrach()) + " hỗ trợ."
                    + (nd.laQuanLy() ? " Bấm \"Nhận hỗ trợ\" nếu bạn muốn tiếp quản." : ""), "NGUOI_KHAC_HO_TRO", null);
        }
        LocalDateTime now = bayGio();
        String cu = dangCuaNguoiKhac ? tenNhanVien(h.getIdNhanVienPhuTrach()) : null;
        h.setCheDo(HoiThoai.NHAN_VIEN_HO_TRO);
        h.setIdNhanVienPhuTrach(nd.id());
        h.setNgayCapNhat(now);
        luuTin(h, TinNhan.HE_THONG, null, null, cu == null
                ? "Nhân viên " + nd.hoTen() + " đã tham gia hỗ trợ bạn."
                : "Nhân viên " + nd.hoTen() + " đã tiếp quản cuộc trò chuyện từ " + cu + ".", now);
        hoiThoaiRepo.save(h);
    }

    private HoiThoai hoiThoaiHoTro(Integer id) {
        return hoiThoaiRepo.findById(id == null ? -1 : id)
                .filter(h -> Integer.valueOf(HoiThoai.HO_TRO_KHACH).equals(h.getLoai()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cuộc trò chuyện."));
    }

    private ThanhVienHoiThoai thanhVien(Integer idHoiThoai, Integer idNv) {
        return thanhVienRepo.findFirstByIdHoiThoaiAndIdNhanVien(idHoiThoai, idNv).orElseGet(() -> {
            ThanhVienHoiThoai tv = new ThanhVienHoiThoai();
            tv.setIdHoiThoai(idHoiThoai);
            tv.setIdNhanVien(idNv);
            tv.setIdTinNhanDaDoc(0);
            tv.setNgayThamGia(bayGio());
            return thanhVienRepo.save(tv);
        });
    }

    private Map<String, Object> tomTat(HoiThoai h, Map<Integer, String> tenNv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("tenKhach", h.getTenKhach());
        m.put("sdtKhach", h.getSdtKhach());
        m.put("idKhachHang", h.getIdKhachHang());
        m.put("cheDo", h.getCheDo());
        m.put("idNhanVienPhuTrach", h.getIdNhanVienPhuTrach());
        m.put("tenNhanVienPhuTrach", h.getIdNhanVienPhuTrach() == null ? null
                : tenNv.computeIfAbsent(h.getIdNhanVienPhuTrach(), this::tenNhanVien));
        m.put("ngayTao", h.getNgayTao());
        m.put("ngayCapNhat", h.getNgayCapNhat());
        m.put("tinCuoi", ChatChung.tinCuoi(tinNhanRepo.findFirstByIdHoiThoaiOrderByIdDesc(h.getId()).orElse(null)));
        return m;
    }

    private Map<String, Object> khachHang(Integer id) {
        if (id == null) return null;
        return khachHangRepo.findById(id).map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", k.getId());
            m.put("ma", k.getMaKhachHang());
            m.put("hoTen", k.getHoTen());
            m.put("sdt", k.getSdt());
            m.put("email", k.getEmail());
            return m;
        }).orElse(null);
    }

    /**
     * Tổng hợp đánh giá của khách sau khi chat với nhân viên (góp ý 16/11 [12:30–12:59]): đếm Tốt / Bình thường / Chưa tốt,
     * đã / chưa giải quyết, theo từng nhân viên; lọc theo khoảng ngày đánh giá, mức độ, nhân viên.
     * Đọc từ tin hệ thống "Khách đánh giá: ..." đang lưu trong từng cuộc chat (không thêm bảng, không đổi SQL).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> tongHopDanhGia(java.time.LocalDate tuNgay, java.time.LocalDate denNgay, String mucDo, Integer idNhanVien) {
        XacThuc.nhanVien();
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) throw ApiException.badRequest("\"Từ ngày\" phải trước hoặc bằng \"Đến ngày\".");
        String locMuc = mucDo == null || mucDo.isBlank() ? null : mucDo.trim().toUpperCase(java.util.Locale.ROOT);
        if (locMuc != null && !MUC_DO.containsKey(locMuc)) throw ApiException.badRequest("Mức độ đánh giá không hợp lệ.");

        List<TinNhan> tinDanhGia = tinNhanRepo.findByLoaiNguoiGuiAndNoiDungStartingWithOrderByThoiGianDescIdDesc(TinNhan.HE_THONG, MO_DAU_DANH_GIA);
        Map<Integer, HoiThoai> hoiThoai = new HashMap<>();
        List<Integer> ids = tinDanhGia.stream().map(TinNhan::getIdHoiThoai).distinct().toList();
        if (!ids.isEmpty()) for (HoiThoai h : hoiThoaiRepo.findByIdIn(ids)) hoiThoai.put(h.getId(), h);
        Map<Integer, String> tenNv = new HashMap<>();

        List<Map<String, Object>> ds = new ArrayList<>();
        Map<Integer, Map<String, Object>> theoNv = new LinkedHashMap<>();
        Map<Integer, String> dsNhanVien = new LinkedHashMap<>(); // mọi nhân viên từng được đánh giá (cho ô lọc)
        Map<String, Integer> dem = new LinkedHashMap<>();
        for (String k : THU_TU_MUC) dem.put(k, 0);
        int daGiaiQuyet = 0;
        for (TinNhan t : tinDanhGia) {
            HoiThoai h = hoiThoai.get(t.getIdHoiThoai());
            if (h == null || !Integer.valueOf(HoiThoai.HO_TRO_KHACH).equals(h.getLoai())) continue;
            DanhGiaDoc dg = docDanhGia(t.getNoiDung());
            if (dg == null) continue;
            Integer idNv = h.getIdNhanVienPhuTrach();
            String ten = idNv == null ? null : tenNv.computeIfAbsent(idNv, this::tenNhanVien);
            if (idNv != null) dsNhanVien.putIfAbsent(idNv, ten);
            java.time.LocalDate ngay = t.getThoiGian() == null ? null : t.getThoiGian().toLocalDate();
            if (tuNgay != null && (ngay == null || ngay.isBefore(tuNgay))) continue;
            if (denNgay != null && (ngay == null || ngay.isAfter(denNgay))) continue;
            if (locMuc != null && !locMuc.equals(dg.muc())) continue;
            if (idNhanVien != null && !idNhanVien.equals(idNv)) continue;

            dem.merge(dg.muc(), 1, Integer::sum);
            if (dg.daGiaiQuyet()) daGiaiQuyet++;
            Map<String, Object> nv = theoNv.computeIfAbsent(idNv == null ? -1 : idNv, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idNhanVien", idNv);
                m.put("tenNhanVien", ten == null ? "Không rõ" : ten);
                m.put("tong", 0);
                for (String muc : THU_TU_MUC) m.put(muc, 0);
                return m;
            });
            nv.merge("tong", 1, (a, b) -> (Integer) a + (Integer) b);
            nv.merge(dg.muc(), 1, (a, b) -> (Integer) a + (Integer) b);

            Map<String, Object> d = new LinkedHashMap<>();
            d.put("idTinNhan", t.getId());
            d.put("idHoiThoai", h.getId());
            d.put("thoiGian", t.getThoiGian());
            d.put("tenKhach", h.getTenKhach());
            d.put("sdtKhach", h.getSdtKhach());
            d.put("idNhanVien", idNv);
            d.put("tenNhanVien", ten);
            d.put("mucDo", dg.muc());
            d.put("nhanMucDo", MUC_DO.get(dg.muc()));
            d.put("daGiaiQuyet", dg.daGiaiQuyet());
            d.put("gopY", dg.gopY());
            ds.add(d);
        }
        List<Map<String, Object>> nhanVien = new ArrayList<>();
        dsNhanVien.forEach((id, ten) -> nhanVien.add(Map.of("id", id, "hoTen", ten == null ? "Không rõ" : ten)));

        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("tong", ds.size());
        kq.put("dem", dem);
        kq.put("daGiaiQuyet", daGiaiQuyet);
        kq.put("chuaGiaiQuyet", ds.size() - daGiaiQuyet);
        kq.put("theoNhanVien", new ArrayList<>(theoNv.values()));
        kq.put("nhanVien", nhanVien);
        kq.put("danhGia", ds);
        return kq;
    }

    /** Một đánh giá đọc lại từ tin hệ thống: muc là khóa TOT / BINH_THUONG / CHUA_TOT. */
    record DanhGiaDoc(String muc, boolean daGiaiQuyet, String gopY) {
    }

    /** "Khách đánh giá: Tốt · Đã giải quyết yêu cầu · Góp ý: ..." -> DanhGiaDoc; sai dạng thì null. */
    static DanhGiaDoc docDanhGia(String noiDung) {
        if (noiDung == null || !noiDung.startsWith(MO_DAU_DANH_GIA)) return null;
        String[] phan = noiDung.substring(MO_DAU_DANH_GIA.length()).split(" · ", 3);
        String muc = null;
        for (Map.Entry<String, String> e : MUC_DO.entrySet()) if (e.getValue().equals(phan[0].trim())) muc = e.getKey();
        if (muc == null || phan.length < 2) return null;
        boolean daGiaiQuyet = phan[1].trim().startsWith("Đã giải quyết");
        String gopY = phan.length > 2 && phan[2].startsWith("Góp ý: ") ? phan[2].substring("Góp ý: ".length()) : null;
        return new DanhGiaDoc(muc, daGiaiQuyet, gopY);
    }

    private String tenNhanVien(Integer id) {
        if (id == null) return null;
        return nhanVienRepo.findById(id).map(NhanVien::getHoTen).orElse("nhân viên khác");
    }

    private TinNhan luuTin(HoiThoai h, int loai, Integer idNv, String ten, String noiDung, LocalDateTime luc) {
        TinNhan t = new TinNhan();
        t.setIdHoiThoai(h.getId());
        t.setLoaiNguoiGui(loai);
        t.setIdNhanVien(idNv);
        t.setTenNguoiGui(ten);
        t.setNoiDung(noiDung.length() > 2000 ? noiDung.substring(0, 2000) : noiDung);
        t.setThoiGian(luc);
        return tinNhanRepo.save(t);
    }
}
