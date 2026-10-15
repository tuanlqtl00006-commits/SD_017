package com.footstyle.demo.service;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.DiaChiNhanVien;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.repository.DiaChiNhanVienRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import com.footstyle.demo.repository.VaiTroRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Kho dữ liệu nhân viên GIẢ trong bộ nhớ cho test (không cần CSDL, Spring hay Mockito), dùng chung cho các test nhập Excel.
 * Có sẵn: vai trò Quản lý (id 1), Nhân viên (id 2), "Kế toán" (id 3, ĐANG DÙNG nhưng không thuộc hai quyền hợp lệ),
 * "Đã ngưng" (id 4, ngưng dùng) và hai nhân viên mẫu NV01 (Quản lý, CCCD 079203001234) và NV02 (Nhân viên).
 */
final class KhoNhanVienGia {

    final List<NhanVien> nhanViens = new ArrayList<>();
    final List<DiaChiNhanVien> diaChis = new ArrayList<>();
    final List<VaiTro> vaiTros = new ArrayList<>();
    /** Số lần gọi save (để test chứng minh "file sai thì không ghi gì"). */
    int soLanLuu;
    final BCryptPasswordEncoder bam = new BCryptPasswordEncoder();
    final NhanVienService service;
    final VaiTroRepository vaiTroRepo;

    private int idNv = 100;
    private int idDc = 500;

    KhoNhanVienGia() {
        VaiTro quanLy = vaiTro(1, VaiTro.TEN_QUAN_LY, 1);
        VaiTro nhanVien = vaiTro(2, VaiTro.TEN_NHAN_VIEN, 1);
        vaiTro(3, "Kế toán", 1);
        vaiTro(4, "Đã ngưng", 0);
        NhanVien a = nv("NV01", "Nguyễn Hoàng Long", "0912000001", "long@footstyle.vn", quanLy);
        a.setCanCuocCongDan("079203001234");
        nv("NV02", "Trần Thị Mai", "0912000002", "mai@footstyle.vn", nhanVien);

        NhanVienRepository nvRepo = KhoGia.tao(NhanVienRepository.class, (ten, a1) -> switch (ten) {
            case "findAllByOrderByIdDesc" -> nhanViens.stream().sorted(Comparator.comparing(NhanVien::getId).reversed()).toList();
            case "findAll" -> new ArrayList<>(nhanViens);
            case "findById" -> nhanViens.stream().filter(x -> x.getId().equals(a1[0])).findFirst();
            case "existsByEmailIgnoreCaseAndIdNot" -> nhanViens.stream()
                    .anyMatch(x -> x.getEmail() != null && x.getEmail().equalsIgnoreCase((String) a1[0]) && !x.getId().equals(a1[1]));
            case "existsBySdtAndIdNot" -> nhanViens.stream().anyMatch(x -> a1[0].equals(x.getSdt()) && !x.getId().equals(a1[1]));
            case "existsByCanCuocCongDanAndIdNot" -> nhanViens.stream()
                    .anyMatch(x -> a1[0].equals(x.getCanCuocCongDan()) && !x.getId().equals(a1[1]));
            case "countByVaiTroIdAndTrangThai" -> nhanViens.stream()
                    .filter(x -> x.getVaiTro().getId().equals(a1[0]) && a1[1].equals(x.getTrangThai())).count();
            case "findAllMaNhanVien" -> nhanViens.stream().map(NhanVien::getMaNhanVien).toList();
            case "save" -> {
                NhanVien x = (NhanVien) a1[0];
                soLanLuu++;
                if (x.getId() == null) {
                    x.setId(++idNv);
                    nhanViens.add(x);
                }
                yield x;
            }
            default -> KhoGia.KHONG_CO;
        });
        DiaChiNhanVienRepository dcRepo = KhoGia.tao(DiaChiNhanVienRepository.class, (ten, a1) -> switch (ten) {
            case "findByNhanVienIdOrderByMacDinhDescIdAsc" -> diaChis.stream().filter(d -> d.getNhanVien().getId().equals(a1[0]))
                    .sorted(Comparator.comparing((DiaChiNhanVien d) -> !Boolean.TRUE.equals(d.getMacDinh())).thenComparing(DiaChiNhanVien::getId))
                    .toList();
            case "saveAll" -> {
                List<DiaChiNhanVien> kq = new ArrayList<>();
                for (Object o : (Iterable<?>) a1[0]) {
                    DiaChiNhanVien d = (DiaChiNhanVien) o;
                    if (d.getId() == null) {
                        d.setId(++idDc);
                        diaChis.add(d);
                    }
                    kq.add(d);
                }
                yield kq;
            }
            default -> KhoGia.KHONG_CO;
        });
        VaiTroRepository vtRepo = KhoGia.tao(VaiTroRepository.class, (ten, a1) -> switch (ten) {
            case "findById" -> vaiTros.stream().filter(x -> x.getId().equals(a1[0])).findFirst();
            case "findByTrangThaiOrderByIdAsc" -> vaiTros.stream().filter(x -> x.getTrangThai().equals(a1[0])).toList();
            default -> KhoGia.KHONG_CO;
        });
        this.vaiTroRepo = vtRepo;
        this.service = new NhanVienService(nvRepo, dcRepo, vtRepo, bam);
    }

    private VaiTro vaiTro(int id, String ten, int trangThai) {
        VaiTro v = new VaiTro();
        v.setId(id);
        v.setTenVaiTro(ten);
        v.setTrangThai(trangThai);
        vaiTros.add(v);
        return v;
    }

    private NhanVien nv(String ma, String hoTen, String sdt, String email, VaiTro vaiTro) {
        NhanVien n = new NhanVien();
        n.setId(nhanViens.size() + 1);
        n.setMaNhanVien(ma);
        n.setHoTen(hoTen);
        n.setSdt(sdt);
        n.setEmail(email);
        n.setVaiTro(vaiTro);
        n.setTrangThai(NhanVien.HOAT_DONG);
        n.setDiaChi("Hà Nội");
        n.setNgayVaoLam(LocalDate.of(2022, 1, 10));
        nhanViens.add(n);
        return n;
    }

    /** Tìm nhân viên theo email (không phân biệt hoa thường). */
    NhanVien theoEmail(String email) {
        return nhanViens.stream().filter(x -> email.equalsIgnoreCase(x.getEmail())).findFirst().orElse(null);
    }

    /** Địa chỉ đã lưu của một nhân viên. */
    List<DiaChiNhanVien> diaChiCua(NhanVien nv) {
        return diaChis.stream().filter(d -> d.getNhanVien() == nv).toList();
    }
}
