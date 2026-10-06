import axios from 'axios';

const api = axios.create({
    baseURL: '/api', // Dùng proxy trong vite.config.js để chuyển hướng sang http://localhost:8081/api
    headers: {
        'Content-Type': 'application/json',
    }
});

// Lỗi từ backend có dạng { message: "..." }: đưa message tiếng Việt đó vào error.message
// để các trang chỉ cần toast.error(e.message). Mất kết nối thì báo riêng cho dễ hiểu.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.data?.message) {
            error.message = error.response.data.message;
        } else if (!error.response) {
            error.message = 'Không kết nối được máy chủ. Hãy kiểm tra backend (cổng 8081) đã chạy chưa.';
        }
        return Promise.reject(error);
    }
);

export default api;
