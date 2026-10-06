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
    private Integer id; 

    
    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore 
    private HoaDon hoaDon;

    
    @Column(name = "nguoi_thao_tac")
    private String nguoiTao;

    
    @Column(name = "hanh_dong")
    private Integer trangThai; 
    
    
    

    
    @Column(name = "thoi_gian")
    private Date ngayTao;

    
    @Column(name = "mo_ta")
    private String ghiChu;
}
