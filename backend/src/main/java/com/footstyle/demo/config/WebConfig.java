package com.footstyle.demo.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Cho phép xem ảnh đã tải lên: GET /api/uploads/ten-file -> file trong thư mục upload. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path thuMuc;

    public WebConfig(@Value("${app.upload-dir:uploads}") String thuMucUpload) {
        this.thuMuc = Paths.get(thuMucUpload).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String viTri = thuMuc.toUri().toString();
        if (!viTri.endsWith("/")) {
            viTri += "/"; // Spring bắt buộc vị trí thư mục kết thúc bằng dấu /
        }
        registry.addResourceHandler("/api/uploads/**").addResourceLocations(viTri);
    }
}
