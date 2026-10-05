import os
import re

files_with_comments = [
    r'src\components\DotGiamGiaManager.vue',
    r'src\components\Header.vue',
    r'src\components\HoaDonChiTiet.vue',
    r'src\components\HoaDonManager.vue',
    r'src\components\KhachHangAdd.vue',
    r'src\components\KhachHangManager.vue',
    r'src\components\NhanVienManager.vue',
    r'src\components\Sidebar.vue',
    r'src\components\nhanVien\NhanVienFormModal.vue',
    r'src\services\api.js'
]

html_comment_pattern = re.compile(r'<!--.*?-->', re.DOTALL)
js_multi_comment_pattern = re.compile(r'/\*.*?\*/', re.DOTALL)
js_single_comment_pattern = re.compile(r'(?<!:)\/\/.*')

for file_path in files_with_comments:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Apply regexes
    new_content = html_comment_pattern.sub('', content)
    new_content = js_multi_comment_pattern.sub('', new_content)
    new_content = js_single_comment_pattern.sub('', new_content)
    
    # Remove lines that became purely whitespace due to comment removal (optional, but good for cleanup)
    lines = new_content.split('\n')
    cleaned_lines = []
    for line in lines:
        if not line.strip() and len(line) > 0 and len(content.split('\n')) > len(lines):
            pass # this might be too aggressive, let's just write back new_content as is
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

print('Done')
