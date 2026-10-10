package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Bảng lich_su_hoa_don: mỗi lần đổi trạng thái / thao tác trên hóa đơn ghi một dòng. */
@Entity
@Table(name = "lich_su_hoa_don")
@Getter
@Setter
public class LichSuHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private HoaDon hoaDon;

    @Column(name = "nguoi_thao_tac")
    private String nguoiThaoTac;

    /** Tên hành động dạng chữ, vd "Chờ xác nhận", "Đã xác nhận", "Hủy đơn hàng" (cột NVARCHAR). */
    @Column(name = "hanh_dong")
    private String hanhDong;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Column(name = "mo_ta")
    private String moTa;
}
