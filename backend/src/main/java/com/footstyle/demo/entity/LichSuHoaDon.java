package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Date;

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
    private String nguoiTao;

    // Ép biến trangThai trỏ đúng vào cột hanh_dong trong SQL
    @Column(name = "hanh_dong")
    private String trangThai; 

    // Ép biến ngayTao trỏ đúng vào cột thoi_gian trong SQL
    @Column(name = "thoi_gian")
    private LocalDateTime ngayTao; // (Hoặc LocalDateTime tùy bạn đang dùng kiểu gì)

    @Column(name = "mo_ta")
    private String ghiChu;

}
