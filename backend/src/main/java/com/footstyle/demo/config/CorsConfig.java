package com.footstyle.demo.config;

import com.footstyle.demo.security.XacThucInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Vite tự nhảy sang cổng 5174, 5175... khi 5173 đang bận nên cho phép mọi cổng của localhost / 127.0.0.1.
                // Cho phép thêm IP mạng nội bộ (192.168.x.x, 10.x.x.x, 172.16-31.x.x) để điện thoại cùng Wi-Fi mở web bằng IP máy tính
                // (quét QR thanh toán giả lập) vẫn gọi được API; địa chỉ internet bên ngoài vẫn bị chặn.
                java.util.List<String> goc = new java.util.ArrayList<>(java.util.List.of(
                        "http://localhost:*", "http://127.0.0.1:*", "http://192.168.*:*", "http://10.*:*"));
                for (int i = 16; i <= 31; i++) goc.add("http://172." + i + ".*:*");
                registry.addMapping("/**")
                        .allowedOriginPatterns(goc.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders(XacThucInterceptor.TIEU_DE_VAI_TRO) // giao diện đọc được vai trò mới khi bị chuyển quyền
                        .allowCredentials(true);
            }
        };
    }
}
