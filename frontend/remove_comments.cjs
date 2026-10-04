const fs = require('fs');

const files_with_comments = [
    'src/components/DotGiamGiaManager.vue',
    'src/components/Header.vue',
    'src/components/HoaDonChiTiet.vue',
    'src/components/HoaDonManager.vue',
    'src/components/KhachHangAdd.vue',
    'src/components/KhachHangManager.vue',
    'src/components/NhanVienManager.vue',
    'src/components/Sidebar.vue',
    'src/components/nhanVien/NhanVienFormModal.vue',
    'src/services/api.js'
];

const html_comment_pattern = /<!--[\s\S]*?-->/g;
const js_multi_comment_pattern = /\/\*[\s\S]*?\*\//g;
const js_single_comment_pattern = /(?<!:)\/\/.*$/gm;

files_with_comments.forEach(file_path => {
    try {
        if (!fs.existsSync(file_path)) return;
        let content = fs.readFileSync(file_path, 'utf8');
        
        content = content.replace(html_comment_pattern, '');
        content = content.replace(js_multi_comment_pattern, '');
        content = content.replace(js_single_comment_pattern, '');
        
        fs.writeFileSync(file_path, content, 'utf8');
        console.log(`Processed ${file_path}`);
    } catch (err) {
        console.error(`Error processing ${file_path}:`, err);
    }
});
console.log('Done');
