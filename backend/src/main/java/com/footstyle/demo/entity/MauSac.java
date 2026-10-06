package com.footstyle.demo.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "mau_sac")
@AttributeOverride(name = "ma", column = @Column(name = "ma_mau_sac"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_mau_sac"))
public class MauSac extends ThuocTinh {
}
