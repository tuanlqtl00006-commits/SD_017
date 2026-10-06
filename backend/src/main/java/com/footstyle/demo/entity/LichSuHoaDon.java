package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Entity
@Table(name = "lich_su_hoa_don")
@Getter
@Setter
public class LichSuHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Giữ nguyên Integer để đồng bộ với LichSuHoaDonRepository nhé

    // Cột khóa ngoại giữ nguyên tên id_hoa_don vì đã khớp
    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore // THÊM ĐÚNG DÒNG NÀY VÀO ĐÂY!
    private HoaDon hoaDon;

    // 1. Ánh xạ nguoiTao thành nguoi_thao_tac
    @Column(name = "nguoi_thao_tac")
    private String nguoiTao;

    // 2. Ánh xạ trangThai thành hanh_dong
    @Column(name = "hanh_dong")
    private Integer trangThai; 
    // (Lưu ý: Nếu trong SQL cột hanh_dong của bạn là kiểu chuỗi nvarchar chứa chữ "Đã xác nhận", 
    // thì biến này bạn phải đổi thành String và lúc lưu ở Controller phải truyền chữ vào nhé. 
    // Còn nếu SQL là kiểu INT thì để nguyên Integer).

    // 3. Ánh xạ ngayTao thành thoi_gian
    @Column(name = "thoi_gian")
    private Date ngayTao;

    // 4. Ánh xạ ghiChu thành mo_ta
    @Column(name = "mo_ta")
    private String ghiChu;
}
