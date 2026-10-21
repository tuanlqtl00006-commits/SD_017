package com.footstyle.demo.security;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Gắn XacThucInterceptor vào mọi /api/** và tạo bộ ký token từ cấu hình app.auth.* (application.properties). */
@Configuration
public class XacThucWebConfig implements WebMvcConfigurer {

    private final XacThucInterceptor interceptor;

    public XacThucWebConfig(XacThucInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**");
    }

    @Bean
    public static TokenXacThuc tokenXacThuc(@Value("${app.auth.bi-mat}") String biMat,
                                            @Value("${app.auth.thoi-han-gio:12}") long thoiHanGio) {
        return new TokenXacThuc(biMat, Duration.ofHours(Math.max(1, thoiHanGio)), Clock.systemDefaultZone());
    }
}
