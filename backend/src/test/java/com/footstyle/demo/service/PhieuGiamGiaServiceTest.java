package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.KhachHangTomTatResponse;
import com.footstyle.demo.dto.PhieuGiamGiaRequest;
import com.footstyle.demo.dto.PhieuGiamGiaResponse;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaKhachHangRepository;
import com.footstyle.demo.repository.PhieuGiamGiaRepository;
import com.footstyle.demo.service.PhieuMailNoiDung.Loai;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    /** Kết quả giả của HoaDonRepository.thongKeMuaHangTheoKhach: { idKhach, số đơn hoàn thành, ngày tạo đơn gần nhất }. */
    private final List<Object[]> thongKeMua = new ArrayList<>();
    private int soLanTinhThongKe = 0;
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
        HoaDonRepository hdRepo = proxy(HoaDonRepository.class, (ten, a) -> switch (ten) {
            case "thongKeMuaHangTheoKhach" -> {
                assertEquals(com.footstyle.demo.entity.HoaDon.HOAN_THANH, a[0], "chỉ đếm đơn Đã hoàn thành");
                soLanTinhThongKe++;
                yield new ArrayList<>(thongKeMua);
            }
            default -> NONE;
        });
        service = new PhieuGiamGiaService(phieuRepo, lkRepo, khRepo, hdRepo, mail);
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
    /** Bây giờ, cắt về phút (phiếu tính theo phút). */
    private static final LocalDateTime BAY_GIO = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    /** Ví dụ của cô: giảm 10%, tối đa 200.000, đơn tối thiểu 500.000; chưa tới giờ bắt đầu (sắp diễn ra). */
    private static PhieuGiamGiaRequest caNhan(String ma, long giaTri, List<Integer> khach) {
        return new PhieuGiamGiaRequest(ma, "Tri ân khách", "CA_NHAN", "PHAN_TRAM", giaTri, 200_000L, 500_000L,
                null, null, BAY_GIO.plusDays(5), BAY_GIO.plusDays(11), khach);
    }

    private static PhieuGiamGiaRequest caNhanTuLuc(String ma, long giaTri, List<Integer> khach, LocalDateTime batDau, LocalDateTime ketThuc) {
        return new PhieuGiamGiaRequest(ma, "Tri ân khách", "CA_NHAN", "PHAN_TRAM", giaTri, 200_000L, 500_000L,
                null, null, batDau, ketThuc, khach);
    }

    private static PhieuGiamGiaRequest congKhai(String ma, long giaTri) {
        return new PhieuGiamGiaRequest(ma, "Khuyến mãi chung", "CONG_KHAI", "PHAN_TRAM", giaTri, 200_000L, 500_000L,
                100, null, BAY_GIO.plusDays(5), BAY_GIO.plusDays(11), null);
    }

    /** Chèn thẳng một phiếu vào kho giả (tạo qua service thì giờ bắt đầu ở quá khứ bị chặn). */
    private PhieuGiamGia chenPhieu(String ma, int doiTuong, LocalDateTime batDau, LocalDateTime ketThuc) {
        PhieuGiamGia cu = new PhieuGiamGia();
        cu.setId(++idPhieu);
        cu.setMaPhieu(ma);
        cu.setTenPhieu("Phiếu có sẵn");
        cu.setDoiTuongApDung(doiTuong);
        cu.setLoaiGiam(PhieuGiamGia.LOAI_PHAN_TRAM);
        cu.setGiaTriGiam(10L);
        cu.setGiamToiDa(200_000L);
        cu.setGiaTriDonHangToiThieu(500_000L);
        cu.setSoLuong(doiTuong == PhieuGiamGia.DOI_TUONG_KHACH_CU_THE ? 1 : 10);
        cu.setSoLuongDaDung(0);
        cu.setTrangThai(PhieuGiamGia.HOAT_DONG);
        cu.setNgayBatDau(batDau);
        cu.setNgayKetThuc(ketThuc);
        phieus.add(cu);
        return cu;
    }

    private void tangKhach(PhieuGiamGia p, int idKhach) {
        PhieuGiamGiaKhachHang l = new PhieuGiamGiaKhachHang();
        l.setId(++idLienKet);
        l.setIdPhieuGiamGia(p.getId());
        l.setIdKhachHang(idKhach);
        l.setTrangThai(1);
        lienKets.add(l);
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
                99, 5, BAY_GIO.plusDays(5), BAY_GIO.plusDays(11), List.of(1, 3));
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
                50, 3, BAY_GIO.plusDays(5), BAY_GIO.plusDays(11), List.of(2, 4, 5));
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
        PhieuGiamGia dangChay = chenPhieu("VCH0001", PhieuGiamGia.DOI_TUONG_KHACH_CU_THE, BAY_GIO.minusHours(2), BAY_GIO.plusDays(5));
        for (int id : new int[] {2, 4, 5}) tangKhach(dangChay, id);
        PhieuGiamGiaResponse r = service.sua(dangChay.getId(), caNhanTuLuc("VCH0001", 15, List.of(1, 3, 5), BAY_GIO.minusHours(2), BAY_GIO.plusDays(5)));
        assertTrue(mail.thu.isEmpty(), "đổi cả thông tin lẫn khách nhưng phiếu đang diễn ra");
        assertNull(r.ketQuaMail());
        assertEquals(List.of(1, 3, 5), khachCuaPhieu(dangChay.getId()), "dữ liệu vẫn được lưu");
    }

    @Test
    void phieuBatDauBayGio_tinhLaDangDienRa_taoVanGuiMailMoi_suaKhongGuiMail() {
        PhieuGiamGiaResponse goc = service.them(caNhanTuLuc("VCH0001", 10, List.of(2, 4, 5), BAY_GIO, BAY_GIO.plusDays(5)));
        assertEquals(3, mail.thu.size(), "tạo phiếu vẫn gửi mail mới");
        mail.thu.clear();
        service.sua(goc.id(), caNhanTuLuc("VCH0001", 15, List.of(2, 4, 5), BAY_GIO, BAY_GIO.plusDays(5)));
        assertTrue(mail.thu.isEmpty());
    }

    @Test
    void phieuDaKetThuc_suaKhongGuiMail() {
        PhieuGiamGia cu = chenPhieu("VCHCU01", PhieuGiamGia.DOI_TUONG_KHACH_CU_THE, BAY_GIO.minusDays(20), BAY_GIO.minusDays(10).withHour(23).withMinute(59).withSecond(59));
        tangKhach(cu, 2);
        service.sua(cu.getId(), caNhanTuLuc("VCHCU01", 15, List.of(2), BAY_GIO.minusDays(20), BAY_GIO.minusDays(10).withHour(23).withMinute(59)));
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

    // ---------- case khó ----------

    @Test
    void tangPhieuChoKhachDaNgungHoatDong_baoLoiVaChuaLuuGi() {
        khachHangs.get(2).setTrangThai(0); // KH3 ngừng hoạt động
        ApiException e = assertThrows(ApiException.class, () -> service.them(caNhan("VCHNGUNG", 10, List.of(1, 3))));
        assertTrue(e.getMessage().contains("ngừng hoạt động"), e.getMessage());
        assertTrue(phieus.isEmpty(), "lỗi thì chưa lưu phiếu");
        assertTrue(lienKets.isEmpty());
        assertTrue(mail.thu.isEmpty());
    }

    @Test
    void suaPhieu_khachCuNayNgungHoatDongVanGiuDuoc_nhungKhongThemKhachNgungMoi() {
        PhieuGiamGiaResponse goc = taoPhieuCaNhan123();          // khách 2, 4, 5
        khachHangs.get(1).setTrangThai(0);                        // KH2 (đã được tặng) nay ngừng hoạt động
        service.sua(goc.id(), caNhan("VCH0001", 10, List.of(2, 4, 5, 1)));
        assertEquals(List.of(1, 2, 4, 5), khachCuaPhieu(goc.id()));

        khachHangs.get(2).setTrangThai(0);                        // KH3 chưa được tặng, đang ngừng
        ApiException e = assertThrows(ApiException.class, () -> service.sua(goc.id(), caNhan("VCH0001", 10, List.of(1, 2, 3, 4, 5))));
        assertTrue(e.getMessage().contains("Khách 3"), e.getMessage());
    }

    @Test
    void suaPhieuDaHetHan_khongDuocDoiGioKetThucSangThoiDiemKhacTrongQuaKhu() {
        PhieuGiamGia cu = chenPhieu("VCHCU99", PhieuGiamGia.DOI_TUONG_TAT_CA, BAY_GIO.minusDays(20), BAY_GIO.minusDays(10).withHour(23).withMinute(59).withSecond(59));

        PhieuGiamGiaRequest giuNguyen = new PhieuGiamGiaRequest("VCHCU99", "Phiếu cũ (đổi tên)", "CONG_KHAI", "PHAN_TRAM", 10L, null, 0L,
                10, null, BAY_GIO.minusDays(20), BAY_GIO.minusDays(10).withHour(23).withMinute(59), null);
        assertEquals("Phiếu cũ (đổi tên)", service.sua(cu.getId(), giuNguyen).ten(), "giữ nguyên giờ kết thúc cũ thì vẫn sửa được");

        PhieuGiamGiaRequest doiQuaKhu = new PhieuGiamGiaRequest("VCHCU99", "Phiếu cũ", "CONG_KHAI", "PHAN_TRAM", 10L, null, 0L,
                10, null, BAY_GIO.minusDays(20), BAY_GIO.minusDays(5), null);
        assertTrue(assertThrows(ApiException.class, () -> service.sua(cu.getId(), doiQuaKhu)).getMessage().contains("quá khứ"));

        PhieuGiamGiaRequest giaHan = new PhieuGiamGiaRequest("VCHCU99", "Phiếu cũ", "CONG_KHAI", "PHAN_TRAM", 10L, null, 0L,
                10, null, BAY_GIO.minusDays(20), BAY_GIO.plusDays(3), null);
        assertEquals(BAY_GIO.plusDays(3).withSecond(59), service.sua(cu.getId(), giaHan).ngayKetThuc(), "gia hạn tới tương lai thì được");
    }

    @Test
    void phieuTienMat_donToiThieuNhoHonSoTienGiam_baoLoi() {
        PhieuGiamGiaRequest r = new PhieuGiamGiaRequest("VCHTIEN1", "Giảm 100k", "CONG_KHAI", "TIEN_MAT", 100_000L, null, 50_000L,
                10, null, BAY_GIO, BAY_GIO.plusDays(3), null);
        assertTrue(assertThrows(ApiException.class, () -> service.them(r)).getMessage().contains("Đơn tối thiểu"));

        PhieuGiamGiaRequest r2 = new PhieuGiamGiaRequest("VCHTIEN2", "Giảm 100k đơn 0đ", "CONG_KHAI", "TIEN_MAT", 100_000L, null, 0L,
                10, null, BAY_GIO, BAY_GIO.plusDays(3), null);
        assertTrue(assertThrows(ApiException.class, () -> service.them(r2)).getMessage().contains("Đơn tối thiểu"));
    }

    @Test
    void phieuPhanTram_giamToiDaLonHonDonToiThieu_baoLoi() {
        PhieuGiamGiaRequest r = new PhieuGiamGiaRequest("VCHPT01", "Giảm 50%", "CONG_KHAI", "PHAN_TRAM", 50L, 200_000L, 100_000L,
                10, null, BAY_GIO, BAY_GIO.plusDays(3), null);
        assertTrue(assertThrows(ApiException.class, () -> service.them(r)).getMessage().contains("Giảm tối đa không được lớn hơn đơn tối thiểu"));
    }

    @Test
    void phieuCongKhai_gioiHanMoiKhachLonHonSoLuong_baoLoi() {
        PhieuGiamGiaRequest r = new PhieuGiamGiaRequest("VCHGH1", "Giới hạn", "CONG_KHAI", "PHAN_TRAM", 10L, null, 0L,
                5, 6, BAY_GIO, BAY_GIO.plusDays(3), null);
        assertTrue(assertThrows(ApiException.class, () -> service.them(r)).getMessage().contains("Giới hạn mỗi khách"));
    }

    // ---------- ngày GIỜ (theo phút) ----------

    private static PhieuGiamGiaRequest congKhaiTuLuc(String ma, LocalDateTime batDau, LocalDateTime ketThuc) {
        return new PhieuGiamGiaRequest(ma, "Khuyến mãi chung", "CONG_KHAI", "PHAN_TRAM", 10L, 200_000L, 500_000L,
                100, null, batDau, ketThuc, null);
    }

    @Test
    void ngayGioLuuTheoPhut_batDauTuDauPhut_ketThucDenHetPhut() {
        LocalDateTime bd = BAY_GIO.plusDays(2).withHour(8).withMinute(30).withSecond(45);
        LocalDateTime kt = BAY_GIO.plusDays(3).withHour(17).withMinute(0).withSecond(10);
        PhieuGiamGiaResponse r = service.them(congKhaiTuLuc("VCHGIO1", bd, kt));
        assertEquals(bd.withSecond(0), r.ngayBatDau(), "bắt đầu tính từ đầu phút");
        assertEquals(kt.withSecond(59), r.ngayKetThuc(), "kết thúc 17:00 là dùng được tới hết phút 17:00");
        assertEquals(bd.withSecond(0), phieus.get(0).getNgayBatDau());
        assertEquals(kt.withSecond(59), phieus.get(0).getNgayKetThuc());
    }

    @Test
    void ketThucKhongSauBatDau_baoLoi_ketThucSauMotPhutThiDuoc() {
        LocalDateTime bd = BAY_GIO.plusDays(1).withHour(9).withMinute(0);
        for (LocalDateTime kt : List.of(bd.minusMinutes(1), bd, bd.plusSeconds(30))) {
            ApiException e = assertThrows(ApiException.class, () -> service.them(congKhaiTuLuc("VCHGIO2", bd, kt)), kt.toString());
            assertTrue(e.getMessage().contains("kết thúc phải sau ngày giờ bắt đầu"), e.getMessage());
        }
        assertTrue(phieus.isEmpty());
        PhieuGiamGiaResponse ok = service.them(congKhaiTuLuc("VCHGIO3", bd, bd.plusMinutes(1)));
        assertEquals(bd.plusMinutes(1).withSecond(59), ok.ngayKetThuc());
    }

    @Test
    void thieuNgayGio_baoLoi() {
        ApiException a = assertThrows(ApiException.class, () -> service.them(congKhaiTuLuc("VCHGIO4", null, BAY_GIO.plusDays(1))));
        assertEquals("Chọn ngày giờ bắt đầu.", a.getMessage());
        ApiException b = assertThrows(ApiException.class, () -> service.them(congKhaiTuLuc("VCHGIO4", BAY_GIO.plusDays(1), null)));
        assertEquals("Chọn ngày giờ kết thúc.", b.getMessage());
    }

    @Test
    void taoPhieu_batDauOQuaKhu_baoLoi_nhungLechTrongDungSaiThiDuoc() {
        LocalDateTime bayGio = LocalDateTime.now();
        ApiException e = assertThrows(ApiException.class,
                () -> service.them(congKhaiTuLuc("VCHQK01", bayGio.minusMinutes(ThoiGianPhut.DUNG_SAI_PHUT + 5), bayGio.plusDays(1))));
        assertTrue(e.getMessage().contains("bắt đầu không được ở quá khứ"), e.getMessage());
        assertTrue(phieus.isEmpty());
        // bấm "Bây giờ" rồi lưu chậm vài phút vẫn được
        assertEquals("VCHQK02", service.them(congKhaiTuLuc("VCHQK02", bayGio.minusMinutes(3), bayGio.plusDays(1))).ma());
    }

    @Test
    void taoPhieu_ketThucOQuaKhu_baoLoi() {
        LocalDateTime bayGio = LocalDateTime.now();
        ApiException e = assertThrows(ApiException.class,
                () -> service.them(congKhaiTuLuc("VCHQK03", bayGio.minusMinutes(4), bayGio.minusMinutes(2))));
        assertTrue(e.getMessage().contains("kết thúc không được ở quá khứ"), e.getMessage());
    }

    @Test
    void phieuDaBatDau_khongDuocDoiGioBatDau_nhungDoiGioKetThucSangTuongLaiThiDuoc() {
        PhieuGiamGia dang = chenPhieu("VCHDANG1", PhieuGiamGia.DOI_TUONG_TAT_CA, BAY_GIO.minusHours(3), BAY_GIO.plusDays(2));
        for (LocalDateTime bdMoi : List.of(BAY_GIO.minusHours(2), BAY_GIO.minusHours(4), BAY_GIO.plusHours(1))) {
            ApiException e = assertThrows(ApiException.class,
                    () -> service.sua(dang.getId(), congKhaiTuLuc("VCHDANG1", bdMoi, BAY_GIO.plusDays(2))), bdMoi.toString());
            assertTrue(e.getMessage().contains("đã bắt đầu nên không đổi được"), e.getMessage());
        }
        assertEquals(BAY_GIO.minusHours(3), dang.getNgayBatDau(), "lỗi thì giữ nguyên dữ liệu");

        PhieuGiamGiaResponse giaHan = service.sua(dang.getId(), congKhaiTuLuc("VCHDANG1", BAY_GIO.minusHours(3), BAY_GIO.plusDays(4)));
        assertEquals(BAY_GIO.plusDays(4).withSecond(59), giaHan.ngayKetThuc());

        ApiException quaKhu = assertThrows(ApiException.class,
                () -> service.sua(dang.getId(), congKhaiTuLuc("VCHDANG1", BAY_GIO.minusHours(3), BAY_GIO.minusMinutes(30))));
        assertTrue(quaKhu.getMessage().contains("kết thúc mới không được ở quá khứ"), quaKhu.getMessage());
    }

    @Test
    void phieuSapDienRa_doiGioBatDau_chiDuocSangTuongLai() {
        PhieuGiamGia sap = chenPhieu("VCHSAP1", PhieuGiamGia.DOI_TUONG_TAT_CA, BAY_GIO.plusDays(2), BAY_GIO.plusDays(9));
        ApiException e = assertThrows(ApiException.class,
                () -> service.sua(sap.getId(), congKhaiTuLuc("VCHSAP1", BAY_GIO.minusHours(1), BAY_GIO.plusDays(9))));
        assertTrue(e.getMessage().contains("bắt đầu mới không được ở quá khứ"), e.getMessage());
        PhieuGiamGiaResponse ok = service.sua(sap.getId(), congKhaiTuLuc("VCHSAP1", BAY_GIO.plusDays(3), BAY_GIO.plusDays(9)));
        assertEquals(BAY_GIO.plusDays(3), ok.ngayBatDau());
    }

    @Test
    void phieuCuLuuLucTruocLaNgayDayDu_vanSuaDuocKhiKhongDoiGio() {
        // Dữ liệu cũ: bắt đầu 00:00:00, hết ngày 23:59:59. Gửi lại 00:00 / 23:59 thì không tính là đổi giờ
        LocalDateTime dau = BAY_GIO.minusDays(3).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime cuoi = BAY_GIO.plusDays(3).withHour(23).withMinute(59).withSecond(59);
        PhieuGiamGia cu = chenPhieu("VCHCU77", PhieuGiamGia.DOI_TUONG_TAT_CA, dau, cuoi);
        PhieuGiamGiaResponse r = service.sua(cu.getId(), congKhaiTuLuc("VCHCU77", dau, cuoi.withSecond(0)));
        assertEquals(dau, r.ngayBatDau());
        assertEquals(cuoi, r.ngayKetThuc());
    }

    // ---------- giới hạn các con số ----------

    @Test
    void giaTriVuotTran_baoLoi() {
        LocalDateTime bd = BAY_GIO.plusDays(1);
        LocalDateTime kt = BAY_GIO.plusDays(2);
        record Ca(PhieuGiamGiaRequest req, String loi) {
        }
        List<Ca> ca = List.of(
                new Ca(new PhieuGiamGiaRequest("VCHMAX01", "Tiền", "CONG_KHAI", "TIEN_MAT", 100_000_001L, null, 200_000_000L, 5, null, bd, kt, null), "Số tiền giảm tối đa"),
                new Ca(new PhieuGiamGiaRequest("VCHMAX02", "Phần trăm", "CONG_KHAI", "PHAN_TRAM", 10L, 100_000_001L, 0L, 5, null, bd, kt, null), "Giảm tối đa không quá"),
                new Ca(new PhieuGiamGiaRequest("VCHMAX03", "Đơn", "CONG_KHAI", "PHAN_TRAM", 10L, null, 1_000_000_001L, 5, null, bd, kt, null), "Đơn tối thiểu không quá"),
                new Ca(new PhieuGiamGiaRequest("VCHMAX04", "Số lượng", "CONG_KHAI", "PHAN_TRAM", 10L, null, 0L, 1_000_001, null, bd, kt, null), "Số lượng tối đa"));
        for (Ca c : ca) {
            ApiException e = assertThrows(ApiException.class, () -> service.them(c.req()), c.loi());
            assertTrue(e.getMessage().contains(c.loi()), e.getMessage());
        }
        assertTrue(phieus.isEmpty());
        // đúng ở mức trần thì vẫn được
        service.them(new PhieuGiamGiaRequest("VCHMAX05", "Trần", "CONG_KHAI", "TIEN_MAT", 100_000_000L, null, 1_000_000_000L, 1_000_000, null, bd, kt, null));
    }

    // ---------- bảng chọn khách: ngày sinh, giới tính, tổng đơn, mua gần nhất ----------

    private KhachHangTomTatResponse khachTrongBang(List<KhachHangTomTatResponse> ds, int id) {
        return ds.stream().filter(k -> k.id() == id).findFirst().orElseThrow();
    }

    @Test
    void khachCoTheChon_coNgaySinhGioiTinhTongDonVaMuaGanNhat() {
        khachHangs.get(1).setNgaySinh(LocalDate.of(1999, 9, 2));
        khachHangs.get(1).setGioiTinh(1);
        khachHangs.get(2).setGioiTinh(0);
        LocalDateTime lanCuoi = LocalDateTime.of(2026, 9, 20, 15, 30);
        thongKeMua.add(new Object[] {2, 3L, lanCuoi});
        thongKeMua.add(new Object[] {4, 1L, java.sql.Timestamp.valueOf(lanCuoi.minusDays(40))});

        List<KhachHangTomTatResponse> ds = service.getKhachHangCoTheChon();
        KhachHangTomTatResponse kh2 = khachTrongBang(ds, 2);
        assertEquals(3, kh2.tongDon());
        assertEquals(lanCuoi, kh2.muaGanNhat());
        assertEquals(LocalDate.of(1999, 9, 2), kh2.ngaySinh());
        assertEquals(1, kh2.gioiTinh());
        assertFalse(kh2.daDung());

        KhachHangTomTatResponse kh3 = khachTrongBang(ds, 3);
        assertEquals(0, kh3.gioiTinh());
        assertNull(kh3.ngaySinh(), "chưa khai ngày sinh");

        KhachHangTomTatResponse kh4 = khachTrongBang(ds, 4);
        assertEquals(1, kh4.tongDon());
        assertEquals(lanCuoi.minusDays(40), kh4.muaGanNhat(), "đọc được cả kiểu Timestamp");

        KhachHangTomTatResponse kh1 = khachTrongBang(ds, 1);
        assertEquals(0, kh1.tongDon(), "chưa mua đơn nào");
        assertNull(kh1.muaGanNhat());
    }

    @Test
    void khachCoTheChon_chiLayKhachDangHoatDong() {
        khachHangs.get(2).setTrangThai(0);
        List<Integer> ids = service.getKhachHangCoTheChon().stream().map(KhachHangTomTatResponse::id).toList();
        assertFalse(ids.contains(3));
        assertTrue(ids.containsAll(List.of(1, 2, 4, 5, 6)));
    }

    @Test
    void chiTietPhieuCaNhan_khachDuocTangCungCoThongKe() {
        thongKeMua.add(new Object[] {2, 5L, LocalDateTime.of(2026, 10, 1, 9, 0)});
        PhieuGiamGiaResponse goc = service.them(caNhan("VCHTK001", 10, List.of(2, 4)));
        PhieuGiamGiaResponse chiTiet = service.getById(goc.id());
        assertEquals(5, khachTrongBang(chiTiet.khachHangs(), 2).tongDon());
        assertEquals(0, khachTrongBang(chiTiet.khachHangs(), 4).tongDon());
    }

    @Test
    void phieuCongKhai_khongPhaiTinhThongKeMuaHang() {
        PhieuGiamGiaResponse goc = service.them(congKhai("VCHTK002", 10));
        service.getById(goc.id());
        service.doiTrangThai(goc.id());
        assertEquals(0, soLanTinhThongKe, "phiếu công khai không có khách được tặng nên khỏi truy vấn hóa đơn");
    }
}
