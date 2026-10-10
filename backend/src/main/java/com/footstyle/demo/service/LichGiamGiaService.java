package com.footstyle.demo.service;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính mức giảm giá của từng biến thể theo KHOẢNG NGÀY khi nhiều đợt giảm giá chồng lên nhau.
 *
 * Ví dụ: đợt A giảm 10% từ 1/10 đến 10/10 cho SPCT1, SPCT5; đợt B giảm 15% từ 4/10 đến 20/10 cho SPCT1, SPCT3, SPCT4.
 * Với SPCT1 kết quả là 3 đoạn:
 *   1/10 - 3/10  : chỉ đợt A            -> 10%
 *   4/10 - 10/10 : đợt A và B chồng nhau -> lấy mức CAO NHẤT = 15% (trung bình cộng của các đợt là 12,5%)
 *   11/10 - 20/10: chỉ đợt B            -> 15%
 *
 * Quy ước: khi nhiều đợt cùng áp dụng cho một biến thể trong một ngày thì mức áp dụng (phanTramApDung) tính theo
 * cấu hình app.giam-gia.che-do-chong-dot: MAX (mặc định) = lấy mức cao nhất, TRUNG_BINH = trung bình cộng các đợt.
 * Cả mức cao nhất và trung bình cộng đều được trả kèm để đối chiếu.
 * Chỉ tính các đợt đang bật (trang_thai = 1) và các dòng chi tiết đang bật.
 */
@Service
@RequiredArgsConstructor
public class LichGiamGiaService {

    public record DotTrongDoan(Long idDot, String maDot, String tenDot, int phanTram) {
    }

    public record DoanGiam(LocalDate tuNgay, LocalDate denNgay, double phanTramApDung, int phanTramCaoNhat,
                           double phanTramTrungBinh, List<DotTrongDoan> cacDot, Long giaSauGiam) {
    }

    public record LichBienThe(Long idBienThe, String maSpct, String tenSanPham, Long giaBan, List<DoanGiam> doan) {
    }

    private final DotGiamGiaRepository dotRepo;
    private final DotGiamGiaChiTietRepository chiTietRepo;
    private final SanPhamChiTietRepository bienTheRepo;
    private final GiamGiaApDungService apDungService; // giữ cấu hình MAX / TRUNG_BINH và tính giảm giá hôm nay

    /** Một dòng "đợt X giảm p% cho biến thể này từ ngày a đến ngày b". */
    private record MucGiam(DotGiamGia dot, int phanTram, LocalDate tu, LocalDate den) {
    }

    /* ===================== Lịch giảm giá của một đợt ===================== */

    /**
     * Với mỗi biến thể của đợt: chia khoảng ngày của đợt thành các đoạn có mức giảm khác nhau
     * (có tính cả các đợt khác đang chồng lên).
     */
    @Transactional(readOnly = true)
    public List<LichBienThe> lichCuaDot(Long idDot) {
        DotGiamGia dot = dotRepo.findById(idDot)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt giảm giá."));
        LocalDate tu = dot.getNgayBatDau().toLocalDate();
        LocalDate den = dot.getNgayKetThuc().toLocalDate();

        Set<Long> idBienThes = new HashSet<>();
        List<Long> thuTu = new ArrayList<>();
        for (DotGiamGiaChiTiet ct : chiTietRepo.findByDotGiamGiaId(idDot)) {
            if (dangBat(ct.getTrangThai()) && idBienThes.add(ct.getIdSanPhamChiTiet())) {
                thuTu.add(ct.getIdSanPhamChiTiet());
            }
        }

        Map<Long, List<MucGiam>> mucTheoBienThe = gomMucGiam(idBienThes);
        CheDo cheDo = apDungService.cheDo();
        List<LichBienThe> ketQua = new ArrayList<>();
        for (Long idBt : thuTu) {
            SanPhamChiTiet bt = bienTheRepo.findById(idBt.intValue()).orElse(null);
            if (bt == null) {
                continue;
            }
            List<DoanGiam> doan = chiaDoan(mucTheoBienThe.getOrDefault(idBt, new ArrayList<>()), tu, den, bt.getGiaBan(), cheDo);
            ketQua.add(new LichBienThe(idBt, bt.getMaSpct(),
                    bt.getSanPham() == null ? null : bt.getSanPham().getTenSanPham(), bt.getGiaBan(), doan));
        }
        return ketQua;
    }

    /* ===================== Mức giảm đang áp dụng hôm nay ===================== */

    /**
     * { idBienThe -> % giảm hôm nay } (nhiều đợt cùng áp dụng thì gộp theo MAX / TRUNG_BINH đã cấu hình).
     * Biến thể không giảm thì không có trong map.
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> giamHomNay() {
        return apDungService.phanTramTheoBienThe(LocalDate.now());
    }

    /* ===================== Hàm phụ ===================== */

    private boolean dangBat(Integer trangThai) {
        return trangThai == null || trangThai == 1;
    }

    // Gom mọi dòng giảm giá (của các đợt đang bật) theo từng biến thể. locIdBienThe = null: lấy tất cả biến thể.
    private Map<Long, List<MucGiam>> gomMucGiam(Set<Long> locIdBienThe) {
        Map<Long, DotGiamGia> dotDangBat = new HashMap<>();
        for (DotGiamGia d : dotRepo.findAll()) {
            if (d.getTrangThai() != null && d.getTrangThai() == 1) {
                dotDangBat.put(d.getId(), d);
            }
        }
        Map<Long, List<MucGiam>> ketQua = new LinkedHashMap<>();
        for (DotGiamGiaChiTiet ct : chiTietRepo.findAll()) {
            if (!dangBat(ct.getTrangThai()) || ct.getDotGiamGia() == null) {
                continue;
            }
            DotGiamGia d = dotDangBat.get(ct.getDotGiamGia().getId());
            if (d == null || (locIdBienThe != null && !locIdBienThe.contains(ct.getIdSanPhamChiTiet()))) {
                continue;
            }
            int pt = ct.getPhanTramGiamBienThe() == null ? 0 : ct.getPhanTramGiamBienThe();
            ketQua.computeIfAbsent(ct.getIdSanPhamChiTiet(), k -> new ArrayList<>())
                    .add(new MucGiam(d, pt, d.getNgayBatDau().toLocalDate(), d.getNgayKetThuc().toLocalDate()));
        }
        return ketQua;
    }

    // Chia [tu, den] thành các đoạn liên tiếp mà trong mỗi đoạn tập các đợt áp dụng không đổi.
    private List<DoanGiam> chiaDoan(List<MucGiam> mucs, LocalDate tu, LocalDate den, Long giaBan, CheDo cheDo) {
        // Các mốc đổi mức giảm: ngày bắt đầu mỗi đợt và ngày liền sau ngày kết thúc mỗi đợt (cắt trong [tu, den])
        TreeSet<LocalDate> moc = new TreeSet<>();
        moc.add(tu);
        moc.add(den.plusDays(1));
        for (MucGiam m : mucs) {
            LocalDate s = m.tu().isBefore(tu) ? tu : m.tu();
            LocalDate e = m.den().isAfter(den) ? den : m.den();
            if (s.isAfter(e)) {
                continue;
            }
            moc.add(s);
            moc.add(e.plusDays(1));
        }

        List<DoanGiam> ketQua = new ArrayList<>();
        List<LocalDate> ds = new ArrayList<>(moc);
        for (int i = 0; i + 1 < ds.size(); i++) {
            LocalDate dau = ds.get(i);
            LocalDate cuoi = ds.get(i + 1).minusDays(1);
            List<MucGiam> ap = new ArrayList<>();
            for (MucGiam m : mucs) {
                if (!m.tu().isAfter(dau) && !m.den().isBefore(cuoi)) {
                    ap.add(m);
                }
            }
            if (ap.isEmpty()) {
                continue;
            }
            int cao = 0;
            double tong = 0;
            List<DotTrongDoan> cacDot = new ArrayList<>();
            for (MucGiam m : ap) {
                cao = Math.max(cao, m.phanTram());
                tong += m.phanTram();
                cacDot.add(new DotTrongDoan(m.dot().getId(), m.dot().getMaDot(), m.dot().getTenDot(), m.phanTram()));
            }
            double trungBinh = Math.round(tong / ap.size() * 100.0) / 100.0;
            double apDung = cheDo == CheDo.TRUNG_BINH ? trungBinh : cao;
            long gia = giaBan == null ? 0 : giaBan;
            long giaSau = Math.round(gia * (1 - apDung / 100.0));

            // Đoạn liền kề có cùng tập đợt (và cùng mức giảm) thì gộp lại cho gọn
            DoanGiam truoc = ketQua.isEmpty() ? null : ketQua.get(ketQua.size() - 1);
            if (truoc != null && truoc.denNgay().plusDays(1).equals(dau) && cungTapDot(truoc.cacDot(), cacDot)) {
                ketQua.set(ketQua.size() - 1, new DoanGiam(truoc.tuNgay(), cuoi, apDung, cao, trungBinh, cacDot, giaSau));
            } else {
                ketQua.add(new DoanGiam(dau, cuoi, apDung, cao, trungBinh, cacDot, giaSau));
            }
        }
        return ketQua;
    }

    private boolean cungTapDot(List<DotTrongDoan> a, List<DotTrongDoan> b) {
        if (a.size() != b.size()) {
            return false;
        }
        Set<Long> ida = new HashSet<>();
        for (DotTrongDoan x : a) {
            ida.add(x.idDot());
        }
        for (DotTrongDoan x : b) {
            if (!ida.contains(x.idDot())) {
                return false;
            }
        }
        return true;
    }
}
