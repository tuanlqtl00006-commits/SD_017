package com.footstyle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Bảng thanh_vien_hoi_thoai: nhân viên thuộc hội thoại nào + đã đọc tới tin nhắn nào (để đếm tin chưa đọc). */
@Entity
@Getter
@Setter
@Table(name = "thanh_vien_hoi_thoai")
public class ThanhVienHoiThoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_hoi_thoai")
    private Integer idHoiThoai;

    @Column(name = "id_nhan_vien")
    private Integer idNhanVien;

    @Column(name = "id_tin_nhan_da_doc")
    private Integer idTinNhanDaDoc;

    @Column(name = "ngay_tham_gia")
    private LocalDateTime ngayThamGia;
}
