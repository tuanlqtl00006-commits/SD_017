<script setup>
// Nhập lịch làm việc từ Excel: chọn file -> kiểm tra (bảng lỗi theo dòng / cột, hoặc xem trước) -> nhập.
// Sai một dòng là cả file bị từ chối, không ca nào được xếp (backend LichLamViecImportService).
import { computed, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { lichLamViecService } from '../../services/lichLamViecService'
import { useToast } from '../../composables/useToast'
import { formatDate } from '../../utils/format'
import { TOI_DA_DONG_NHAP_LICH, taiFileMauLich } from '../../utils/mauLichLamViec'

const props = defineProps({
  nhanViens: { type: Array, default: () => [] },
  cas: { type: Array, default: () => [] },
  thu2: { type: String, required: true },
  homNay: { type: String, required: true },
})
const emit = defineEmits(['close', 'imported'])
const toast = useToast()

const DUNG_LUONG_TOI_DA = 5 * 1024 * 1024
const MA_LOI_FILE = 'FILE_KHONG_HOP_LE'

const buoc = ref('chon') // chon | loi | xem | xong
const file = ref(null)
const khoaOChonFile = ref(0)
const dangXuLy = ref(false)
const loiChung = ref('')
const loiFile = ref(null)
const xemTruoc = ref(null)
const ketQua = ref(null)
const tenCaDangDung = computed(() => props.cas.filter((c) => c.hoatDong).map((c) => c.tenCa).join(', '))

function veBuocChon() {
  buoc.value = 'chon'
  file.value = null
  khoaOChonFile.value++
  loiChung.value = ''
  loiFile.value = null
  xemTruoc.value = null
}

function chonFile(e) {
  const f = e.target.files?.[0] ?? null
  loiChung.value = ''
  loiFile.value = null
  file.value = null
  if (!f) return
  if (!/\.xlsx$/i.test(f.name)) {
    loiChung.value = 'Chỉ nhận file Excel định dạng .xlsx.'
    e.target.value = ''
    return
  }
  if (f.size > DUNG_LUONG_TOI_DA) {
    loiChung.value = 'File quá lớn (tối đa 5 MB).'
    e.target.value = ''
    return
  }
  file.value = f
}

function xuLyLoi(err, buocHienTai) {
  if (err?.code === MA_LOI_FILE && Array.isArray(err.chiTiet?.loi)) {
    loiFile.value = { loi: err.chiTiet.loi, tongDong: err.chiTiet.tongDong, soDongLoi: err.chiTiet.soDongLoi }
    loiChung.value = ''
    buoc.value = 'loi'
    return
  }
  loiChung.value = err?.message || 'Không xử lý được file. Vui lòng thử lại.'
  buoc.value = buocHienTai
}

async function kiemTraFile() {
  if (!file.value || dangXuLy.value) return
  dangXuLy.value = true
  loiChung.value = ''
  try {
    xemTruoc.value = await lichLamViecService.importExcel(file.value, { chiKiemTra: true })
    buoc.value = 'xem'
  } catch (err) {
    xuLyLoi(err, 'chon')
  } finally {
    dangXuLy.value = false
  }
}

async function nhapVaoHeThong() {
  if (!file.value || dangXuLy.value) return
  dangXuLy.value = true
  loiChung.value = ''
  try {
    ketQua.value = await lichLamViecService.importExcel(file.value, { chiKiemTra: false })
    buoc.value = 'xong'
    toast.success(`Đã xếp ${ketQua.value.soLich} ca từ file Excel.`)
    emit('imported', ketQua.value)
  } catch (err) {
    xuLyLoi(err, 'xem')
  } finally {
    dangXuLy.value = false
  }
}

function taiMau() {
  taiFileMauLich({ nhanViens: props.nhanViens, cas: props.cas, thu2: props.thu2, homNay: props.homNay })
}
</script>

<template>
  <BaseModal title="Nhập lịch làm việc từ Excel" size="lg" static-backdrop @close="emit('close')">
    <section v-if="buoc === 'chon'" data-testid="ll-import-chon">
      <h3 class="ll-imp-h">1. Tải file mẫu và điền lịch</h3>
      <p class="ad-hint">Mỗi dòng là một nhân viên làm một ca trong một ngày. Có thể dùng file "Xuất Excel" của tuần trước, sửa ngày rồi nhập lại.</p>
      <button type="button" class="ad-btn" data-testid="ll-import-tai-mau" @click="taiMau">
        <i class="bi bi-download" aria-hidden="true"></i> Tải file mẫu (.xlsx)
      </button>

      <h3 class="ll-imp-h mt-4">2. Chọn file đã điền</h3>
      <input
        :key="khoaOChonFile"
        type="file"
        class="form-control ad-control"
        aria-label="File Excel lịch làm việc"
        accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        :disabled="dangXuLy"
        data-testid="ll-import-file"
        @change="chonFile"
      />
      <p v-if="loiChung" class="ad-error" role="alert" data-testid="ll-import-loi-chung">{{ loiChung }}</p>

      <ul class="ll-imp-rules">
        <li><strong>Sai dù một dòng thì cả file bị từ chối</strong>, chưa ca nào được xếp; hệ thống chỉ rõ dòng và cột sai.</li>
        <li>Mã nhân viên phải đúng và đang hoạt động; nếu điền Họ tên thì phải khớp với mã.</li>
        <li>Ngày làm dạng dd/mm/yyyy, không được là ngày đã qua.</li>
        <li>Ca làm ghi tên hoặc mã ca đang dùng<template v-if="tenCaDangDung">: {{ tenCaDangDung }}</template>.</li>
        <li>Một người không có hai ca trùng hoặc chồng giờ trong cùng ngày (kể cả với lịch đã xếp). Tối đa {{ TOI_DA_DONG_NHAP_LICH }} dòng.</li>
      </ul>
    </section>

    <section v-else-if="buoc === 'loi' && loiFile" data-testid="ll-import-loi">
      <div class="alert alert-danger py-2 px-3 small" role="alert">
        <strong>Cả file bị từ chối, chưa ca nào được xếp.</strong>
        Có {{ loiFile.loi.length }} lỗi ở {{ loiFile.soDongLoi }}/{{ loiFile.tongDong }} dòng.
      </div>
      <div class="ad-table-wrap ll-imp-scroll">
        <div class="ad-table-scroll">
          <table class="ad-table" aria-label="Danh sách lỗi trong file">
            <thead><tr><th class="ad-col-stt">Dòng</th><th>Cột</th><th>Lỗi</th></tr></thead>
            <tbody>
              <tr v-for="(l, i) in loiFile.loi" :key="i">
                <td class="ad-col-stt"><span class="ad-code">{{ l.dong }}</span></td>
                <td class="ad-nowrap">{{ l.cot }}</td>
                <td class="text-danger">{{ l.thongBao }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p class="ad-hint mt-2">"Dòng" là số dòng trong Excel (dòng 1 là tiêu đề).</p>
    </section>

    <section v-else-if="(buoc === 'xem' && xemTruoc) || (buoc === 'xong' && ketQua)" :data-testid="buoc === 'xem' ? 'll-import-xem' : 'll-import-xong'">
      <div class="alert alert-success py-2 px-3 small" role="status">
        <template v-if="buoc === 'xem'"><strong>File hợp lệ.</strong> Sẽ xếp {{ xemTruoc.soLich }} ca. Kiểm tra rồi bấm "Nhập vào lịch".</template>
        <template v-else><strong>Đã xếp {{ ketQua.soLich }} ca</strong> vào lịch làm việc.</template>
      </div>
      <div class="ad-table-wrap ll-imp-scroll">
        <div class="ad-table-scroll">
          <table class="ad-table" aria-label="Các ca sẽ được xếp">
            <thead><tr><th class="ad-col-stt">Dòng</th><th>Nhân viên</th><th>Ngày làm</th><th>Ca</th></tr></thead>
            <tbody>
              <tr v-for="x in (buoc === 'xem' ? xemTruoc : ketQua).xemTruoc" :key="x.dong">
                <td class="ad-col-stt">{{ x.dong }}</td>
                <td class="ad-nowrap">{{ x.hoTen }} <small class="text-muted">{{ x.maNhanVien }}</small></td>
                <td class="ad-nowrap">{{ formatDate(x.ngayLam) }}</td>
                <td class="ad-nowrap">{{ x.tenCa }} <small class="text-muted">{{ x.gio }}</small></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p v-if="loiChung" class="ad-error mt-2" role="alert">{{ loiChung }}</p>
    </section>

    <template #footer>
      <template v-if="buoc === 'chon'">
        <button type="button" class="ad-btn" @click="emit('close')">Hủy</button>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="ll-import-kiem-tra" :disabled="!file || dangXuLy" @click="kiemTraFile">
          <span v-if="dangXuLy" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          <i v-else class="bi bi-check2-circle" aria-hidden="true"></i> Kiểm tra file
        </button>
      </template>
      <template v-else-if="buoc === 'loi'">
        <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="ll-import-chon-lai" @click="veBuocChon"><i class="bi bi-arrow-repeat" aria-hidden="true"></i> Chọn file khác</button>
      </template>
      <template v-else-if="buoc === 'xem'">
        <button type="button" class="ad-btn" :disabled="dangXuLy" @click="veBuocChon">Chọn file khác</button>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="ll-import-nhap" :disabled="dangXuLy" @click="nhapVaoHeThong">
          <span v-if="dangXuLy" class="spinner-border spinner-border-sm" aria-hidden="true"></span> Nhập {{ xemTruoc?.soLich }} ca vào lịch
        </button>
      </template>
      <button v-else type="button" class="ad-btn ad-btn-primary" data-testid="ll-import-dong" @click="emit('close')">Đóng</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.ll-imp-h { margin: 0 0 0.4rem; font-size: 0.95rem; font-weight: 600; color: var(--fs-text); }
.ll-imp-rules { margin: 1.1rem 0 0; padding: 0.9rem 1rem 0.9rem 2rem; border: 1px solid var(--fs-primary-line); border-radius: 14px; background: var(--fs-primary-soft); font-size: 0.85rem; color: var(--fs-text-2); display: grid; gap: 0.3rem; }
.ll-imp-scroll { max-height: 46vh; overflow: auto; }
.ll-imp-scroll .ad-table thead th { position: sticky; top: 0; z-index: 1; }
</style>
