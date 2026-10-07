package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bảng lich_su_thanh_toan: các lần khách trả tiền cho một hóa đơn. */
@Entity
@Table(name = "lich_su_thanh_toan")
@Getter
@Setter
public class LichSuThanhToan {

    public static final int DA_THANH_TOAN = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private HoaDon hoaDon;

    @Column(name = "so_tien")
    private BigDecimal soTien;

    @Column(name = "ma_giao_dich")
    private String maGiaoDich;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "mo_ta")
    private String moTa;
}
