package com.footstyle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Bảng lich_lam_viec: nhân viên làm ca nào vào ngày nào (duy nhất theo nhân viên + ca + ngày). */
@Entity
@Getter
@Setter
@Table(name = "lich_lam_viec")
public class LichLamViec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_ca_lam_viec")
    private CaLamViec caLamViec;

    @Column(name = "ngay_lam")
    private LocalDate ngayLam;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    void taoMoi() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
        if (trangThai == null) trangThai = 1;
    }
}
