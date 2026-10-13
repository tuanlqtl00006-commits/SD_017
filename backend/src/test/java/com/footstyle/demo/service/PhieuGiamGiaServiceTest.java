package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.PhieuGiamGiaRequest;
import com.footstyle.demo.dto.PhieuGiamGiaResponse;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.service.PhieuMailNoiDung.Loai;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Kiểm tra PhieuGiamGiaService thật (them / sua / doiTrangThai) với kho dữ liệu giả trong bộ nhớ
 * và MailService giả ghi lại những thư đã xếp gửi. Khách KH1..KH5 có id 1..5 (KH6 chưa có email).
 */
class PhieuGiamGiaServiceTest {

    /** Một thư mà service đã yêu cầu gửi (chụp lại giá trị phiếu tại thời điểm gửi). */
    record ThuDaGui(Loai loai, String maPhieu, String tenPhieu, Long giaTri, Integer idKhach) {
    }

    /** MailService giả: ghi lại thay vì gửi. */
    static class MailGhiNho extends MailService {
        final List<ThuDaGui> thu = new ArrayList<>();

        volatile boolean daCauHinh = true;

        MailGhiNho() {
            super(null, "", "", "", "FootStyle", "");
        }

        @Override
        public boolean duocCauHinh() {
            return daCauHinh;
        }

        @Override
        public void guiPhieuGiamGia(Loai loai, PhieuGiamGia p, KhachHang kh) {
            thu.add(new ThuDaGui(loai, p.getMaPhieu(), p.getTenPhieu(), p.getGiaTriGiam(), kh.getId()));
        }
    }

    // ---------- kho dữ liệu giả ----------
    private final List<PhieuGiamGia> phieus = new ArrayList<>();
    private final List<PhieuGiamGiaKhachHang> lienKets = new ArrayList<>();
    private final List<KhachHang> khachHangs = new ArrayList<>();
    private int idPhieu = 0;
    private int idLienKet = 0;
    private final MailGhiNho mail = new MailGhiNho();
    private final PhieuGiamGiaService service;

    private static final Object NONE = new Object();

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> xuLy) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            Object kq = xuLy.apply(m.getName(), args == null ? new Object[0] : args);
            if (kq == NONE) throw new UnsupportedOperationException(m.getName());
            return kq;
        });
    }

    PhieuGiamGiaServiceTest() {
        for (int i = 1; i <= 5; i++) khach(i, "KH" + i, "Khách " + i, "kh" + i + "@mail.vn");
        khach(6, "KH6", "Khách 6", null);

        PhieuGiamGiaRepository phieuRepo = proxy(PhieuGiamGiaRepository.class, (ten, a) -> switch (ten) {
            case "findAllByOrderByIdDesc" -> phieus.stream().sorted(Comparator.comparing(PhieuGiamGia::getId).reversed()).toList();
            case "findById" -> phieus.stream().filter(x -> x.getId().equals(a[0])).findFirst();
            case "existsByMaPhieuIgnoreCase" -> phieus.stream().anyMatch(x -> x.getMaPhieu().equalsIgnoreCase((String) a[0]));
            case "save" -> {
                PhieuGiamGia x = (PhieuGiamGia) a[0];
                if (x.getId() == null) {
                    x.setId(++idPhieu);
                    phieus.add(x);
                }
                yield x;
            }
            default -> NONE;
        });
        PhieuGiamGiaKhachHangRepository lkRepo = proxy(PhieuGiamGiaKhachHangRepository.class, (ten, a) -> switch (ten) {
            case "findByIdPhieuGiamGia" -> lienKets.stream().filter(l -> l.getIdPhieuGiamGia().equals(a[0])).toList();
            case "save" -> {
                PhieuGiamGiaKhachHang l = (PhieuGiamGiaKhachHang) a[0];
                if (l.getId() == null) {
                    l.setId(++idLienKet);
                    lienKets.add(l);
                }
                yield l;
            }
            case "deleteAll" -> {
                for (Object o : (Iterable<?>) a[0]) lienKets.remove(o);
                yield null;
            }
            default -> NONE;
        });
        KhachHangRepository khRepo = proxy(KhachHangRepository.class, (ten, a) -> switch (ten) {
            case "findAllById" -> {
                List<KhachHang> kq = new ArrayList<>();
                for (Object id : (Iterable<?>) a[0]) khachHangs.stream().filter(k -> k.getId().equals(id)).findFirst().ifPresent(kq::add);
                yield kq;
            }
            case "findById" -> khachHangs.stream().filter(k -> k.getId().equals(a[0])).findFirst();
            case "findByTrangThaiOrderByHoTenAsc" -> khachHangs.stream().filter(k -> a[0].equals(k.getTrangThai())).toList();
            default -> NONE;
        });
        service = new PhieuGiamGiaService(phieuRepo, lkRepo, khRepo, mail);
    }

    @AfterEach
    void donGiaoDich() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void khach(int id, String ma, String hoTen, String email) {
        KhachHang k = new KhachHang();
        k.setId(id);
        k.setMaKhachHang(ma);
        k.setHoTen(hoTen);
        k.setEmail(email);
        k.setTrangThai(1);
        khachHangs.add(k);
    }

    // ---------- tiện ích dựng request ----------
    private static final LocalDate HOM_NAY = LocalDate.now();

    /** Ví dụ của cô: giảm 10%, tối đa 200.000, đơn tối thiểu 500.000; chưa tới ngày bắt đầu (sắp diễn ra). */
    private static PhieuGiamGiaRequest caNhan(String ma, long giaTri, List<Integer> khach) {
        return new PhieuGiamGiaRequest(ma, "Tri ân khách", "CA_NHAN", "PHAN_TRAM", giaTri, 200_000L, 500_000L,
                null, null, HOM_NAY.plusDays(5), HOM_NAY.plusDays(11), khach);
    }

    private static PhieuGiamGiaRequest caNhanTuNgay(String ma, long giaTri, List<Integer> khach, LocalDate batDau, LocalDate ketThuc) {
        return new PhieuGiamGiaRequest(ma, "Tri ân khách", "CA_NHAN", "PHAN_TRAM", giaTri, 200_000L, 500_000L,
                null, null, batDau, ketThuc, khach);
    }

    private static PhieuGiamGiaRequest congKhai(String ma, long giaTri) {
        return new PhieuGiamGiaRequest(ma, "Khuyến mãi chung", "CONG_KHAI", "PHAN_TRAM", giaTri, 200_000L, 500_000L,
                100, null, HOM_NAY.plusDays(5), HOM_NAY.plusDays(11), null);
    }

    private PhieuGiamGiaResponse taoPhieuCaNhan123() {
        PhieuGiamGiaResponse r = service.them(caNhan("VCH0001", 10, List.of(2, 4, 5)));
        mail.thu.clear();
        return r;
    }

    private List<Integer> khachCuaPhieu(Integer idPhieu) {
        return lienKets.stream().filter(l -> l.getIdPhieuGiamGia().equals(idPhieu)).map(PhieuGiamGiaKhachHang::getIdKhachHang).sorted().toList();
    }

    private List<Integer> nhan(Loai loai) {
        return mail.thu.stream().filter(t -> t.loai() == loai).map(ThuDaGui::idKhach).sorted().toList();
    }

    // ---------- tạo phiếu ----------

    @Test
    void taoPhieuCaNhan_soLuongBangSoKhach_vaMoiKhachNhanMailMoi() {
        PhieuGiamGiaResponse r = service.them(caNhan("VCH0001", 10, List.of(2, 4, 5)));
        assertEquals(3, r.soLuong());
        assertEquals(1, r.gioiHanMoiKhach());
        assertEquals("CA_NHAN", r.hinhThuc());
        assertEquals(List.of(2, 4, 5), khachCuaPhieu(r.id()));
        assertEquals(List.of(2, 4, 5), nhan(Loai.MOI));
        assertEquals(3, mail.thu.size());
        assertTrue(mail.thu.stream().allMatch(t -> t.maPhieu().equals("VCH0001")));
        assertEquals("Đang gửi 3 mail phiếu mới.", r.ketQuaMail());
        assertTrue(phieus.get(0).getNgayBatDau().isAfter(LocalDateTime.now()), "phiếu ở trạng thái Sắp diễn ra");
    }

    @Test
    void taoPhieuCaNhan_moLoThuVoiDungSoThuDeGiaoDienHoiKetQua() {
        PhieuGiamGiaResponse r = service.them(caNhan("VCHLO1", 10, List.of(1, 2, 6)));
        // KH6 chưa có email nên lô chỉ có 2 thư; MailGhiNho không gửi thật nên kết quả chưa xong
        java.util.Map<String, Object> kq = service.ketQuaMail(r.id());
        assertEquals(2, kq.get("tong"));
        assertEquals(false, kq.get("xong"));
    }

    @Test
    void taoPhieuCaNhan_idTrungChiTinhMotLan() {
        PhieuGiamGiaResponse r = service.them(caNhan("VCH0001", 10, List.of(2, 2, 4, 5, 5)));
        assertEquals(3, r.soLuong());
        assertEquals(List.of(2, 4, 5), khachCuaPhieu(r.id()));
        assertEquals(3, mail.thu.size());
    }

    @Test
    void taoPhieuCaNhan_boQuaSoLuongNguoiDungGuiLen() {
        PhieuGiamGiaRequest req = new PhieuGiamGiaRequest("VCH0001", "Tri ân", "CA_NHAN", "PHAN_TRAM", 10L, 200_000L, 500_000L,
                99, 5, HOM_NAY.plusDays(5), HOM_NAY.plusDays(11), List.of(1, 3));
        PhieuGiamGiaResponse r = service.them(req);
        assertEquals(2, r.soLuong());
        assertEquals(1, r.gioiHanMoiKhach());
    }

    @Test
    void taoPhieuCaNhan_chuaCauHinhGmailThiBaoChuaGuiThat() {
        mail.daCauHinh = false;
        PhieuGiamGiaResponse r = service.them(caNhan("VCH0001", 10, List.of(2, 4, 5)));
        // Phiếu vẫn lưu đủ 3 khách, nhưng câu kết quả phải nói rõ là CHƯA gửi (không được báo "Đang gửi / Đã gửi")
        assertEquals(List.of(2, 4, 5), khachCuaPhieu(r.id()));
        assertTrue(r.ketQuaMail().startsWith("Chưa gửi được mail thật vì chưa cấu hình Gmail"), r.ketQuaMail());
        assertTrue(r.ketQuaMail().contains("mail.properties"));
        assertFalse(r.ketQuaMail().contains("Đang gửi"));
        assertFalse(r.ketQuaMail().contains("Đã gửi"));
    }

    @Test
    void taoPhieuCaNhan_khachChuaCoEmailThiBaoTrongCauKetQua() {
        PhieuGiamGiaResponse r = service.them(caNhan("VCH0001", 10, List.of(2, 6)));
        assertEquals(List.of(2), nhan(Loai.MOI));
        assertEquals("Đang gửi 1 mail phiếu mới (1 khách chưa có email nên không gửi được).", r.ketQuaMail());
        mail.thu.clear();
        PhieuGiamGiaResponse r2 = service.them(caNhan("VCH0002", 10, List.of(6)));
        assertTrue(mail.thu.isEmpty());
        assertEquals("Không gửi được mail nào (1 khách chưa có email nên không gửi được).", r2.ketQuaMail());
    }

    @Test
    void taoPhieuCaNhan_khongChonKhachHoacKhachKhongTonTaiThiBaoLoiVaKhongGuiMail() {
        ApiException a = assertThrows(ApiException.class, () -> service.them(caNhan("VCH0001", 10, List.of())));
        assertEquals(HttpStatus.BAD_REQUEST, a.getStatus());
        ApiException b = assertThrows(ApiException.class, () -> service.them(caNhan("VCH0001", 10, List.of(2, 99))));
        assertEquals(HttpStatus.BAD_REQUEST, b.getStatus());
        assertTrue(mail.thu.isEmpty());
    }

    @Test
    void taoPhieuCongKhai_khongGuiMail() {
        PhieuGiamGiaResponse r = service.them(congKhai("VCH0001", 10));
        assertTrue(mail.thu.isEmpty());
        assertNull(r.ketQuaMail());
        assertEquals(100, r.soLuong());
        assertTrue(khachCuaPhieu(r.id()).isEmpty());
    }

    @Test
    void thuChiDuocXepGuiSauKhiLuuThanhCong_rollbackThiKhongGui() {
        // Mô phỏng giao dịch: đăng ký "afterCommit", chỉ khi commit mới gửi
        TransactionSynchronizationManager.initSynchronization();
        service.them(caNhan("VCH0001", 10, List.of(2, 4, 5)));
        assertTrue(mail.thu.isEmpty(), "chưa commit thì chưa gửi");
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) s.afterCommit();
        assertEquals(List.of(2, 4, 5), nhan(Loai.MOI), "commit xong mới gửi");
        TransactionSynchronizationManager.clearSynchronization();
        mail.thu.clear();

        // Rollback: không bao giờ gọi afterCommit -> không có thư nào
        TransactionSynchronizationManager.initSynchronization();
        service.them(caNhan("VCH0002", 10, List.of(1, 3)));
        TransactionSynchronizationManager.clearSynchronization();
        assertTrue(mail.thu.isEmpty(), "rollback thì không gửi");
    }

    // ---------- sửa phiếu "Sắp diễn ra": đúng 3 trường hợp ----------

    @Test
    void th1_doiThongTinPhieuDanhSachKhachGiuNguyen_moiKhachNhanMailCapNhat() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        PhieuGiamGiaResponse r = service.sua(goc.id(), caNhan("VCH0001", 15, List.of(2, 4, 5))); // 10% -> 15%
        assertEquals(List.of(2, 4, 5), nhan(Loai.CAP_NHAT));
        assertEquals(3, mail.thu.size());
        assertTrue(mail.thu.stream().allMatch(t -> t.giaTri() == 15), "mail mang giá trị MỚI");
        assertEquals("Đang gửi 3 mail cập nhật.", r.ketQuaMail());
        assertEquals(List.of(2, 4, 5), khachCuaPhieu(goc.id()));
        assertEquals(3, r.soLuong());
    }

    @Test
    void th2_thongTinKhongDoiDoiKhachThanh_1_3_5() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        PhieuGiamGiaResponse r = service.sua(goc.id(), caNhan("VCH0001", 10, List.of(1, 3, 5)));
        assertEquals(List.of(1, 3), nhan(Loai.MOI));
        assertEquals(List.of(2, 4), nhan(Loai.HUY));
        assertTrue(nhan(Loai.CAP_NHAT).isEmpty());
        assertFalse(mail.thu.stream().anyMatch(t -> t.idKhach() == 5), "khách 5 giữ nguyên nên không nhận gì");
        assertEquals(4, mail.thu.size());
        assertEquals("Đang gửi 2 mail phiếu mới, 2 mail hủy phiếu.", r.ketQuaMail());
        assertEquals(List.of(1, 3, 5), khachCuaPhieu(goc.id()));
        assertEquals(3, r.soLuong());
    }

    @Test
    void th3_doiCaThongTinVaKhach() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        PhieuGiamGiaResponse r = service.sua(goc.id(), caNhan("VCH0001", 15, List.of(1, 3, 5)));
        assertEquals(List.of(1, 3), nhan(Loai.MOI));
        assertEquals(List.of(2, 4), nhan(Loai.HUY));
        assertEquals(List.of(5), nhan(Loai.CAP_NHAT));
        assertEquals(5, mail.thu.size());
        assertEquals("Đang gửi 2 mail phiếu mới, 1 mail cập nhật, 2 mail hủy phiếu.", r.ketQuaMail());
        assertEquals(List.of(1, 3, 5), khachCuaPhieu(goc.id()));
    }

    @Test
    void khongDoiGi_khongGuiMailNao() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        PhieuGiamGiaResponse r = service.sua(goc.id(), caNhan("VCH0001", 10, List.of(2, 4, 5)));
        assertTrue(mail.thu.isEmpty());
        assertNull(r.ketQuaMail());
    }

    @Test
    void soLuongKhongTinhLaThongTinDoi_chiDoiSoLuongGuiLenThiKhongGuiMail() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        PhieuGiamGiaRequest req = new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "PHAN_TRAM", 10L, 200_000L, 500_000L,
                50, 3, HOM_NAY.plusDays(5), HOM_NAY.plusDays(11), List.of(2, 4, 5));
        PhieuGiamGiaResponse r = service.sua(goc.id(), req);
        assertTrue(mail.thu.isEmpty());
        assertEquals(3, r.soLuong(), "số lượng vẫn bằng số khách");
    }

    @Test
    void tungThongTinDoiDeuLamPhatSinhMailCapNhat() {
        String[] ten = {"tên", "loại giảm", "giá trị", "giảm tối đa", "đơn tối thiểu", "ngày bắt đầu", "ngày kết thúc"};
        PhieuGiamGiaRequest goc = caNhan("VCH0001", 10, List.of(2, 4, 5));
        List<PhieuGiamGiaRequest> doi = List.of(
                new PhieuGiamGiaRequest("VCH0001", "Tên khác", "CA_NHAN", "PHAN_TRAM", 10L, 200_000L, 500_000L, null, null, goc.ngayBatDau(), goc.ngayKetThuc(), goc.khachHangIds()),
                new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "TIEN_MAT", 10_000L, null, 500_000L, null, null, goc.ngayBatDau(), goc.ngayKetThuc(), goc.khachHangIds()),
                new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "PHAN_TRAM", 11L, 200_000L, 500_000L, null, null, goc.ngayBatDau(), goc.ngayKetThuc(), goc.khachHangIds()),
                new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "PHAN_TRAM", 10L, 300_000L, 500_000L, null, null, goc.ngayBatDau(), goc.ngayKetThuc(), goc.khachHangIds()),
                new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "PHAN_TRAM", 10L, 200_000L, 600_000L, null, null, goc.ngayBatDau(), goc.ngayKetThuc(), goc.khachHangIds()),
                new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "PHAN_TRAM", 10L, 200_000L, 500_000L, null, null, goc.ngayBatDau().plusDays(1), goc.ngayKetThuc(), goc.khachHangIds()),
                new PhieuGiamGiaRequest("VCH0001", "Tri ân khách", "CA_NHAN", "PHAN_TRAM", 10L, 200_000L, 500_000L, null, null, goc.ngayBatDau(), goc.ngayKetThuc().plusDays(1), goc.khachHangIds()));
        for (int i = 0; i < doi.size(); i++) {
            PhieuGiamGiaResponse p = service.them(new PhieuGiamGiaRequest("VCHX00" + i, goc.ten(), goc.hinhThuc(), goc.loaiGiam(), goc.giaTri(),
                    goc.giamToiDa(), goc.donToiThieu(), null, null, goc.ngayBatDau(), goc.ngayKetThuc(), goc.khachHangIds()));
            mail.thu.clear();
            PhieuGiamGiaRequest d = doi.get(i);
            service.sua(p.id(), new PhieuGiamGiaRequest("VCHX00" + i, d.ten(), d.hinhThuc(), d.loaiGiam(), d.giaTri(), d.giamToiDa(),
                    d.donToiThieu(), null, null, d.ngayBatDau(), d.ngayKetThuc(), d.khachHangIds()));
            assertEquals(List.of(2, 4, 5), nhan(Loai.CAP_NHAT), "đổi " + ten[i] + " phải gửi mail cập nhật");
        }
    }

    @Test
    void doiTuCaNhanSangCongKhai_moiKhachCuNhanMailHuyVaBiGoKhoiPhieu() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        PhieuGiamGiaResponse r = service.sua(goc.id(), congKhai("VCH0001", 10));
        assertEquals(List.of(2, 4, 5), nhan(Loai.HUY));
        assertEquals(3, mail.thu.size());
        assertTrue(khachCuaPhieu(goc.id()).isEmpty());
        assertEquals("CONG_KHAI", r.hinhThuc());
        assertEquals("Đang gửi 3 mail hủy phiếu.", r.ketQuaMail());
    }

    @Test
    void doiTuCongKhaiSangCaNhan_khachDuocTangNhanMailMoi() {
        PhieuGiamGiaResponse goc = service.them(congKhai("VCH0001", 10));
        PhieuGiamGiaResponse r = service.sua(goc.id(), caNhan("VCH0001", 10, List.of(1, 3)));
        assertEquals(List.of(1, 3), nhan(Loai.MOI));
        assertEquals(2, mail.thu.size());
        assertEquals(2, r.soLuong());
    }

    @Test
    void suaPhieuCongKhai_khongGuiMail() {
        PhieuGiamGiaResponse goc = service.them(congKhai("VCH0001", 10));
        PhieuGiamGiaResponse r = service.sua(goc.id(), congKhai("VCH0001", 20));
        assertTrue(mail.thu.isEmpty());
        assertNull(r.ketQuaMail());
    }

    @Test
    void mailHuyMangMaPhieuVaThuChoKhachBoRa() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        service.sua(goc.id(), caNhan("VCH0001", 10, List.of(5)));
        assertEquals(List.of(2, 4), nhan(Loai.HUY));
        assertTrue(mail.thu.stream().allMatch(t -> t.maPhieu().equals("VCH0001")));
    }

    // ---------- phiếu đang / đã diễn ra: sửa không gửi mail ----------

    @Test
    void phieuDangDienRa_suaKhongGuiMail() {
        PhieuGiamGiaResponse goc = service.them(caNhanTuNgay("VCH0001", 10, List.of(2, 4, 5), HOM_NAY.minusDays(2), HOM_NAY.plusDays(5)));
        assertEquals(3, mail.thu.size(), "tạo phiếu vẫn gửi mail mới");
        mail.thu.clear();
        PhieuGiamGiaResponse r = service.sua(goc.id(), caNhanTuNgay("VCH0001", 15, List.of(1, 3, 5), HOM_NAY.minusDays(2), HOM_NAY.plusDays(5)));
        assertTrue(mail.thu.isEmpty(), "đổi cả thông tin lẫn khách nhưng phiếu đang diễn ra");
        assertNull(r.ketQuaMail());
        assertEquals(List.of(1, 3, 5), khachCuaPhieu(goc.id()), "dữ liệu vẫn được lưu");
    }

    @Test
    void phieuBatDauHomNay_tinhLaDangDienRa_suaKhongGuiMail() {
        PhieuGiamGiaResponse goc = service.them(caNhanTuNgay("VCH0001", 10, List.of(2, 4, 5), HOM_NAY, HOM_NAY.plusDays(5)));
        mail.thu.clear();
        service.sua(goc.id(), caNhanTuNgay("VCH0001", 15, List.of(2, 4, 5), HOM_NAY, HOM_NAY.plusDays(5)));
        assertTrue(mail.thu.isEmpty());
    }

    @Test
    void phieuDaKetThuc_suaKhongGuiMail() {
        PhieuGiamGia cu = new PhieuGiamGia();
        cu.setId(++idPhieu);
        cu.setMaPhieu("VCHCU01");
        cu.setTenPhieu("Phiếu cũ");
        cu.setDoiTuongApDung(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE);
        cu.setLoaiGiam(PhieuGiamGia.LOAI_PHAN_TRAM);
        cu.setGiaTriGiam(10L);
        cu.setGiamToiDa(200_000L);
        cu.setGiaTriDonHangToiThieu(500_000L);
        cu.setSoLuong(1);
        cu.setSoLuongDaDung(0);
        cu.setTrangThai(PhieuGiamGia.HOAT_DONG);
        cu.setNgayBatDau(HOM_NAY.minusDays(20).atStartOfDay());
        cu.setNgayKetThuc(HOM_NAY.minusDays(10).atTime(LocalTime.of(23, 59, 59)));
        phieus.add(cu);
        PhieuGiamGiaKhachHang l = new PhieuGiamGiaKhachHang();
        l.setId(++idLienKet);
        l.setIdPhieuGiamGia(cu.getId());
        l.setIdKhachHang(2);
        l.setTrangThai(1);
        lienKets.add(l);

        service.sua(cu.getId(), caNhanTuNgay("VCHCU01", 15, List.of(2), HOM_NAY.minusDays(20), HOM_NAY.minusDays(10)));
        assertTrue(mail.thu.isEmpty());
    }

    // ---------- các ràng buộc cũ vẫn giữ ----------

    @Test
    void boKhachDaDungPhieu_baoLoiVaKhongGuiMail() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        lienKets.stream().filter(l -> l.getIdKhachHang() == 4).findFirst().orElseThrow().setNgaySuDung(LocalDateTime.now());
        ApiException e = assertThrows(ApiException.class, () -> service.sua(goc.id(), caNhan("VCH0001", 15, List.of(2, 5))));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertTrue(mail.thu.isEmpty());
    }

    @Test
    void trungMaPhieu_baoLoi() {
        service.them(congKhai("VCH0001", 10));
        ApiException e = assertThrows(ApiException.class, () -> service.them(caNhan("vch0001", 10, List.of(2))));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertTrue(mail.thu.isEmpty());
    }

    // ---------- công tắc bật / tắt ----------

    @Test
    void doiTrangThai_conHanThiBatTatDuocVaKhongGuiMail() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        assertTrue(goc.hoatDong());
        assertFalse(service.doiTrangThai(goc.id()).hoatDong());
        assertTrue(service.doiTrangThai(goc.id()).hoatDong());
        assertTrue(mail.thu.isEmpty());
    }

    @Test
    void doiTrangThai_daQuaNgayKetThucVanBatTatDuoc() {
        // Phiếu hết hạn vẫn bật / tắt được (tắt = "Ngưng hoạt động"), đúng như công tắc luôn hiện trên giao diện; không gửi mail
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();
        phieus.get(0).setNgayKetThuc(LocalDateTime.now().minusMinutes(1));
        assertFalse(service.doiTrangThai(goc.id()).hoatDong());
        assertEquals(PhieuGiamGia.NGUNG_HOAT_DONG, phieus.get(0).getTrangThai());
        assertTrue(service.doiTrangThai(goc.id()).hoatDong());
        assertTrue(mail.thu.isEmpty());
    }
}
