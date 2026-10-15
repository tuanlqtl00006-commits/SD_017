import axios from 'axios';

const api = axios.create({
    baseURL: '/api', // Dùng proxy trong vite.config.js để chuyển hướng sang http://localhost:8081/api
    headers: {
        'Content-Type': 'application/json',
    }
});

// Lỗi từ backend có dạng { message: "...", loiTheoTruong?: { tenO: "câu báo lỗi" }, code?, chiTiet? }:
//  - error.message      : câu tiếng Việt của backend (để các trang chỉ cần toast.error(e.message)); mất kết nối thì báo riêng cho dễ hiểu.
//  - error.status       : mã HTTP (400, 409, 422...) hoặc null khi không kết nối được máy chủ.
//  - error.loiTheoTruong: lỗi theo TỪNG Ô của form (gắn xuống dưới đúng ô nhập), null nếu backend không trả.
//  - error.code / error.chiTiet: mã lỗi nghiệp vụ và dữ liệu kèm theo (vd FILE_KHONG_HOP_LE + danh sách dòng sai khi nhập Excel).
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const data = error.response?.data;
        if (data?.message) {
            error.message = data.message;
        } else if (!error.response) {
            error.message = 'Không kết nối được máy chủ. Hãy kiểm tra backend (cổng 8081) đã chạy chưa.';
        }
        error.status = error.response?.status ?? null;
        error.code = data?.code ?? null;
        error.loiTheoTruong = data?.loiTheoTruong && typeof data.loiTheoTruong === 'object' ? data.loiTheoTruong : null;
        error.chiTiet = data?.chiTiet ?? null;
        return Promise.reject(error);
    }
);

export default api;
