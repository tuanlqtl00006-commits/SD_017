package com.footstyle.demo.config;

import com.footstyle.demo.service.ChuDeSuKien;
import com.footstyle.demo.service.SuKienService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sau mỗi yêu cầu GHI (POST / PUT / PATCH / DELETE) chạy THÀNH CÔNG (mã 2xx, giao dịch đã lưu xong) thì báo chủ đề bị ảnh hưởng
 * qua SuKienService. Lỗi (4xx / 5xx) thì không báo vì dữ liệu không đổi. Bảng đường dẫn -> chủ đề nằm ở ChuDeSuKien.
 */
@Component
public class SuKienFilter extends OncePerRequestFilter {

    /** Header giao diện gửi kèm: mã tab đang ghi (tab đó không cần tải lại vì đã tự cập nhật). */
    public static final String TIEU_DE_TAB = "X-Tab-Id";

    private final SuKienService suKien;

    public SuKienFilter(SuKienService suKien) {
        this.suKien = suKien;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !ChuDeSuKien.laGhi(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(req, res);
        if (req.isAsyncStarted()) return;
        int ma = res.getStatus();
        if (ma < 200 || ma >= 300) return;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        Set<String> chuDe = ChuDeSuKien.theoYeuCau(req.getMethod(), path);
        if (!chuDe.isEmpty()) suKien.bao(chuDe, req.getHeader(TIEU_DE_TAB));
    }
}
