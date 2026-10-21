package com.footstyle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** Bảng ca_lam_viec: định nghĩa ca (Ca sáng 07:00 - 12:00...). Giờ bắt đầu luôn trước giờ kết thúc (không có ca qua đêm). */
@Entity
@Getter
@Setter
@Table(name = "ca_lam_viec")
public class CaLamViec {

    public static final int HOAT_DONG = 1;
    public static final int NGUNG_HOAT_DONG = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ma_ca")
    private String maCa;

    @Column(name = "ten_ca")
    private String tenCa;

    @Column(name = "gio_bat_dau")
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc")
    private LocalTime gioKetThuc;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    void taoMoi() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = ngayTao;
    }

    @PreUpdate
    void capNhat() {
        ngayCapNhat = LocalDateTime.now();
    }
}
