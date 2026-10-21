package com.footstyle.demo.service;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoiThoai;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.ThanhVienHoiThoai;
import com.footstyle.demo.entity.TinNhan;
import com.footstyle.demo.repository.HoiThoaiRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.ThanhVienHoiThoaiRepository;
import com.footstyle.demo.repository.TinNhanRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Kho giả (trong bộ nhớ) cho test chat: hội thoại, thành viên, tin nhắn + nhân viên của CaLamMau. */
class ChatMau {

    final CaLamMau m = new CaLamMau();
    final List<HoiThoai> hoiThoais = new ArrayList<>();
    final List<ThanhVienHoiThoai> thanhViens = new ArrayList<>();
    final List<TinNhan> tinNhans = new ArrayList<>();
    final List<KhachHang> khachs = new ArrayList<>();

    HoiThoaiRepository hoiThoaiRepo() {
        return KhoGia.tao(HoiThoaiRepository.class, (ten, a) -> switch (ten) {
            case "save" -> {
                HoiThoai h = (HoiThoai) a[0];
                if (h.getId() == null) {
                    if (h.getKhoaRieng() != null && hoiThoais.stream().anyMatch(x -> h.getKhoaRieng().equals(x.getKhoaRieng()))) {
                        throw new IllegalStateException("trùng khoa_rieng " + h.getKhoaRieng());
                    }
                    h.setId(hoiThoais.size() + 1);
                    hoiThoais.add(h);
                }
                yield h;
            }
            case "findById" -> hoiThoais.stream().filter(h -> h.getId().equals(a[0])).findFirst();
            case "findFirstByKhoaRieng" -> hoiThoais.stream().filter(h -> Objects.equals(h.getKhoaRieng(), a[0])).findFirst();
            case "findByIdIn" -> hoiThoais.stream().filter(h -> ((List<?>) a[0]).contains(h.getId())).toList();
            case "findTop200ByLoaiOrderByNgayCapNhatDesc" -> hoiThoais.stream().filter(h -> h.getLoai().equals(a[0]))
                    .sorted(Comparator.comparing(HoiThoai::getNgayCapNhat).reversed().thenComparing(HoiThoai::getId, Comparator.reverseOrder()))
                    .limit(200).toList();
            case "countByLoaiAndSdtKhachAndNgayTaoAfter" -> hoiThoais.stream().filter(h -> h.getLoai().equals(a[0])
                    && Objects.equals(h.getSdtKhach(), a[1]) && h.getNgayTao().isAfter((LocalDateTime) a[2])).count();
            default -> KhoGia.KHONG_CO;
        });
    }

    ThanhVienHoiThoaiRepository thanhVienRepo() {
        return KhoGia.tao(ThanhVienHoiThoaiRepository.class, (ten, a) -> switch (ten) {
            case "save" -> {
                ThanhVienHoiThoai tv = (ThanhVienHoiThoai) a[0];
                if (tv.getId() == null) {
                    if (thanhViens.stream().anyMatch(x -> x.getIdHoiThoai().equals(tv.getIdHoiThoai()) && x.getIdNhanVien().equals(tv.getIdNhanVien()))) {
                        throw new IllegalStateException("trùng thành viên");
                    }
                    tv.setId(thanhViens.size() + 1);
                    thanhViens.add(tv);
                }
                yield tv;
            }
            case "findByIdNhanVien" -> thanhViens.stream().filter(t -> t.getIdNhanVien().equals(a[0])).toList();
            case "findByIdHoiThoai" -> thanhViens.stream().filter(t -> t.getIdHoiThoai().equals(a[0])).toList();
            case "findFirstByIdHoiThoaiAndIdNhanVien" -> thanhViens.stream()
                    .filter(t -> t.getIdHoiThoai().equals(a[0]) && t.getIdNhanVien().equals(a[1])).findFirst();
            default -> KhoGia.KHONG_CO;
        });
    }

    TinNhanRepository tinNhanRepo() {
        return KhoGia.tao(TinNhanRepository.class, (ten, a) -> switch (ten) {
            case "save" -> {
                TinNhan t = (TinNhan) a[0];
                if (t.getId() == null) {
                    t.setId(tinNhans.size() + 1);
                    tinNhans.add(t);
                }
                yield t;
            }
            case "findTop200ByIdHoiThoaiAndIdGreaterThanOrderByIdAsc" -> tinNhans.stream()
                    .filter(t -> t.getIdHoiThoai().equals(a[0]) && t.getId() > (Integer) a[1]).limit(200).toList();
            case "findTop100ByIdHoiThoaiOrderByIdDesc" -> tinNhans.stream().filter(t -> t.getIdHoiThoai().equals(a[0]))
                    .sorted(Comparator.comparing(TinNhan::getId).reversed()).limit(100).toList();
            case "findFirstByIdHoiThoaiOrderByIdDesc" -> tinNhans.stream().filter(t -> t.getIdHoiThoai().equals(a[0]))
                    .max(Comparator.comparing(TinNhan::getId));
            case "demChuaDoc" -> tinNhans.stream().filter(t -> t.getIdHoiThoai().equals(a[0]) && t.getId() > (Integer) a[1]
                    && (t.getIdNhanVien() == null || !t.getIdNhanVien().equals(a[2]))).count();
            case "countByIdHoiThoaiAndLoaiNguoiGuiAndThoiGianAfter" -> tinNhans.stream().filter(t -> t.getIdHoiThoai().equals(a[0])
                    && t.getLoaiNguoiGui().equals(a[1]) && t.getThoiGian().isAfter((LocalDateTime) a[2])).count();
            default -> KhoGia.KHONG_CO;
        });
    }

    NhanVienRepository nhanVienRepo() {
        NhanVienRepository goc = m.nhanVienRepo();
        return KhoGia.tao(NhanVienRepository.class, (ten, a) -> switch (ten) {
            case "findAll" -> a.length == 0 ? m.nhanViens : KhoGia.KHONG_CO;
            case "findById" -> goc.findById((Integer) a[0]);
            case "findByTrangThaiOrderByHoTenAsc" -> goc.findByTrangThaiOrderByHoTenAsc((Integer) a[0]);
            default -> KhoGia.KHONG_CO;
        });
    }

    KhachHangRepository khachRepo() {
        return KhoGia.tao(KhachHangRepository.class, (ten, a) -> switch (ten) {
            case "findFirstBySdt" -> khachs.stream().filter(k -> Objects.equals(k.getSdt(), a[0])).findFirst();
            case "findById" -> khachs.stream().filter(k -> k.getId().equals(a[0])).findFirst();
            default -> KhoGia.KHONG_CO;
        });
    }

    KhachHang khach(int id, String ma, String ten, String sdt) {
        KhachHang k = new KhachHang();
        k.setId(id);
        k.setMaKhachHang(ma);
        k.setHoTen(ten);
        k.setSdt(sdt);
        khachs.add(k);
        return k;
    }

    static void dangNhap(NhanVien nv) {
        CaLamMau.dangNhap(nv);
    }
}
