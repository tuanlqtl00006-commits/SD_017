package com.footstyle.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật các việc chạy theo lịch (vd email báo cáo thống kê tháng tự động gửi cho quản lý - ThongKeSoSanhService). */
@Configuration
@EnableScheduling
public class LichTuDongConfig {
}
