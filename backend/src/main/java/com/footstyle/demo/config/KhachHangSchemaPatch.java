package com.footstyle.demo.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dự án đặt spring.jpa.hibernate.ddl-auto=none nên Hibernate không tự thêm cột.
 * Lớp này chạy 1 lần khi backend khởi động: nếu bảng khach_hang chưa có cột anh_dai_dien (ảnh đại diện)
 * thì thêm vào. Nhờ vậy database của mọi thành viên trong nhóm đều tự cập nhật, không phải chạy SQL bằng tay.
 */
@Component
@RequiredArgsConstructor
public class KhachHangSchemaPatch implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KhachHangSchemaPatch.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "IF COL_LENGTH('khach_hang', 'anh_dai_dien') IS NULL "
                            + "ALTER TABLE khach_hang ADD anh_dai_dien NVARCHAR(500) NULL");
        } catch (Exception e) {
            log.warn("Không thêm được cột khach_hang.anh_dai_dien: {}", e.getMessage());
        }
    }
}
