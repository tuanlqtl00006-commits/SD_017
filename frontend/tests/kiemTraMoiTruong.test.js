/**
 * scripts/kiem-tra-moi-truong.cjs: bước kiểm tra Node / Java / SQL Server mà chay-du-an.bat và chay-du-an.sh chạy trước khi khởi động.
 * Test chạy bằng các "Java giả" truyền vào hàm nên không cần cài đủ các phiên bản JDK trên máy test.
 */
import { createRequire } from 'node:module'
import { existsSync } from 'node:fs'
import net from 'node:net'
import path from 'node:path'
import { describe, expect, it, vi } from 'vitest'

// npm test luôn chạy với thư mục hiện tại là frontend (giống các test khác đọc file bằng process.cwd())
const duongDan = path.resolve(process.cwd(), '..', 'scripts', 'kiem-tra-moi-truong.cjs')
const kt = existsSync(duongDan) ? createRequire(duongDan)(duongDan) : null

// Chỉ bỏ qua khi chạy riêng thư mục frontend (không có thư mục scripts bên cạnh); trong project đầy đủ thì luôn chạy.
describe.skipIf(!kt)('Kiểm tra môi trường (Node / Java / SQL Server)', () => {
  describe('Node.js', () => {
    it.each([
      ['20.18.9', false],
      ['20.19.0', true],
      ['21.7.3', false],
      ['22.11.0', false],
      ['22.12.0', true],
      ['23.0.0', true],
      ['24.15.0', true],
      ['26.0.0', true],
      ['v22.12.0', true],
      ['18.20.4', false],
      ['', false],
      ['khong-phai-so', false],
    ])('Node %s -> %s', (phienBan, mongDoi) => {
      expect(kt.nodeDuDung(phienBan)).toBe(mongDoi)
    })

    it('Node 20.19 chạy được giao diện nhưng báo chú ý vì bộ kiểm thử cần Node 22.12 trở lên', () => {
      const kq = kt.kiemTraNode('20.19.0')
      expect(kq.loi).toEqual([])
      expect(kq.canhBao).toHaveLength(1)
      expect(kq.canhBao[0]).toMatch(/22\.12/)
    })

    it('Node 18 bị báo lỗi và chỉ cách cài lại', () => {
      const kq = kt.kiemTraNode('18.20.4')
      expect(kq.loi).toHaveLength(1)
      expect(kq.loi[0]).toMatch(/nodejs\.org/)
    })

    it('Node 22.12 trở lên không có lỗi hay cảnh báo', () => {
      for (const v of ['22.12.0', '24.15.0', '26.0.0']) {
        const kq = kt.kiemTraNode(v)
        expect(kq.loi).toEqual([])
        expect(kq.canhBao).toEqual([])
      }
    })
  })

  describe('Java', () => {
    const dauRaLinux = [
      'Property settings:',
      '    java.home = /usr/lib/jvm/java-21-openjdk-amd64',
      '    java.specification.version = 21',
      '    java.vendor = Ubuntu',
      '    java.version = 21.0.12.1',
      'openjdk version "21.0.12.1" 2026-07-21',
    ].join('\n')

    it('đọc đúng phiên bản từ kết quả của java -XshowSettings:properties (cả xuống dòng kiểu Windows)', () => {
      expect(kt.phanTichJava(dauRaLinux)).toEqual({
        chinh: 21,
        day_du: '21.0.12.1',
        home: '/usr/lib/jvm/java-21-openjdk-amd64',
        nhaCungCap: 'Ubuntu',
      })
      const win = dauRaLinux.replace(/\n/g, '\r\n').replace('/usr/lib/jvm/java-21-openjdk-amd64', 'C:\\Program Files\\Java\\jdk-21')
      const kq = kt.phanTichJava(win)
      expect(kq.chinh).toBe(21)
      expect(kq.home).toBe('C:\\Program Files\\Java\\jdk-21')
      expect(kq.day_du).toBe('21.0.12.1')
    })

    it.each([
      ['1.8', 8],
      ['11', 11],
      ['17', 17],
      ['21', 21],
      ['25', 25],
      ['', null],
      [undefined, null],
      ['abc', null],
    ])('java.specification.version %s -> %s', (spec, mongDoi) => {
      expect(kt.soPhienBanJava(spec)).toBe(mongDoi)
    })

    const homeJdk = (chinh, extra = {}) => ({ chinh, day_du: `${chinh}.0.1`, home: '/jdk', nhaCungCap: 'Test', ...extra })
    const fsCoJavac = { existsSync: () => true }
    const fsKhongJavac = { existsSync: (p) => !/javac/.test(p) }
    const kiemTra = (kqJava, moiTruong = {}, fsDung = fsCoJavac, os = 'linux') => kt.kiemTraJava(moiTruong, () => kqJava, fsDung, os)

    it.each([17, 18, 21, 22, 23, 24, 25, 27])('JDK %s chạy được, không lỗi không cảnh báo', (v) => {
      const kq = kiemTra(homeJdk(v))
      expect(kq.loi).toEqual([])
      expect(kq.canhBao).toEqual([])
    })

    it.each([8, 11, 16])('Java %s quá cũ -> báo lỗi', (v) => {
      const kq = kiemTra(homeJdk(v))
      expect(kq.loi).toHaveLength(1)
      expect(kq.loi[0]).toMatch(new RegExp(`Java ${v} qua cu`))
    })

    it('Java mới hơn các bản đã thử -> chỉ cảnh báo, không chặn', () => {
      const kq = kiemTra(homeJdk(31))
      expect(kq.loi).toEqual([])
      expect(kq.canhBao).toHaveLength(1)
    })

    it('chỉ có JRE (không có javac) -> báo lỗi vì không biên dịch được backend', () => {
      const kq = kiemTra(homeJdk(21), {}, fsKhongJavac)
      expect(kq.loi).toHaveLength(1)
      expect(kq.loi[0]).toMatch(/JRE/)
    })

    it('không chạy được lệnh java -> báo lỗi kèm cách cài', () => {
      const kq = kt.kiemTraJava({}, () => null, fsCoJavac, 'linux')
      expect(kq.loi).toHaveLength(1)
      expect(kq.loi[0]).toMatch(/JAVA_HOME/)
    })

    it('JAVA_HOME trỏ sai chỗ -> báo lỗi, không chạy java nào cả', () => {
      const chay = vi.fn()
      const kq = kt.kiemTraJava({ JAVA_HOME: '/khong/co' }, chay, { existsSync: () => false }, 'linux')
      expect(kq.loi).toHaveLength(1)
      expect(kq.loi[0]).toContain('/khong/co')
      expect(chay).not.toHaveBeenCalled()
    })

    it('ưu tiên JAVA_HOME như mvnw: bỏ dấu nháy, dùng java.exe trên Windows và java trên hệ khác', () => {
      const chay = vi.fn(() => homeJdk(21))
      kt.kiemTraJava({ JAVA_HOME: '"C:\\jdk-21"' }, chay, fsCoJavac, 'win32')
      expect(chay).toHaveBeenLastCalledWith(path.join('C:\\jdk-21', 'bin', 'java.exe'))
      kt.kiemTraJava({ JAVA_HOME: '/opt/jdk' }, chay, fsCoJavac, 'linux')
      expect(chay).toHaveBeenLastCalledWith(path.join('/opt/jdk', 'bin', 'java'))
    })

    it('không có JAVA_HOME thì dùng lệnh java trong PATH', () => {
      const chay = vi.fn(() => homeJdk(21))
      kt.kiemTraJava({}, chay, fsCoJavac, 'linux')
      expect(chay).toHaveBeenLastCalledWith('java')
    })
  })

  describe('SQL Server', () => {
    const properties =
      'spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=shop_cau_long_footstyle;encrypt=true;trustServerCertificate=true;}\n'

    it('lấy địa chỉ mặc định trong application.properties', () => {
      expect(kt.diaChiSqlServer({}, properties)).toEqual({ host: 'localhost', cong: 1433 })
    })

    it('biến môi trường DB_URL được ưu tiên; không ghi cổng thì là 1433', () => {
      expect(kt.diaChiSqlServer({ DB_URL: 'jdbc:sqlserver://192.168.1.20:14330;databaseName=x' }, properties)).toEqual({ host: '192.168.1.20', cong: 14330 })
      expect(kt.diaChiSqlServer({ DB_URL: 'jdbc:sqlserver://may-chu;databaseName=x' }, properties)).toEqual({ host: 'may-chu', cong: 1433 })
    })

    it('instance có tên (cổng động) hoặc URL lạ thì không dò được -> null', () => {
      expect(kt.diaChiSqlServer({ DB_URL: 'jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=x' }, properties)).toBeNull()
      expect(kt.diaChiSqlServer({ DB_URL: 'jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=x' }, properties)).toBeNull()
      expect(kt.diaChiSqlServer({ DB_URL: 'jdbc:h2:mem:test' }, properties)).toBeNull()
      expect(kt.diaChiSqlServer({}, '')).toBeNull()
    })

    it('không kết nối được chỉ là cảnh báo, không bao giờ chặn việc chạy (backend có thể ở máy khác / chưa bật)', async () => {
      const kq = await kt.kiemTraSqlServer({}, properties, async () => ({ ok: false, lyDo: 'ECONNREFUSED' }))
      expect(kq.loi).toEqual([])
      expect(kq.canhBao).toHaveLength(1)
      expect(kq.canhBao[0]).toMatch(/TCP\/IP/)
      expect(kq.thongTin).toMatch(/ECONNREFUSED/)
    })

    it('kết nối được thì sạch', async () => {
      const thu = vi.fn(async () => ({ ok: true }))
      const kq = await kt.kiemTraSqlServer({}, properties, thu)
      expect(thu).toHaveBeenCalledWith('localhost', 1433)
      expect(kq.loi).toEqual([])
      expect(kq.canhBao).toEqual([])
    })

    it('thuNoiTcp thật: cổng đang mở -> ok, cổng đóng -> không ok', async () => {
      const mayChu = net.createServer((o) => o.end())
      await new Promise((xong) => mayChu.listen(0, '127.0.0.1', xong))
      const { port } = mayChu.address()
      try {
        expect((await kt.thuNoiTcp('127.0.0.1', port, 3000)).ok).toBe(true)
      } finally {
        await new Promise((xong) => mayChu.close(xong))
      }
      const daDong = await kt.thuNoiTcp('127.0.0.1', port, 3000)
      expect(daDong.ok).toBe(false)
    })
  })

  describe('main', () => {
    it('đặt FOOTSTYLE_BO_QUA_KIEM_TRA=1 thì bỏ qua hoàn toàn và trả 0', async () => {
      const log = vi.spyOn(console, 'log').mockImplementation(() => {})
      try {
        expect(await kt.main([], { FOOTSTYLE_BO_QUA_KIEM_TRA: '1' })).toBe(0)
        expect(log.mock.calls.flat().join('\n')).toMatch(/Bo qua/)
      } finally {
        log.mockRestore()
      }
    })

    it('thông báo chỉ gồm ký tự ASCII (cửa sổ cmd của Windows hay lỗi font tiếng Việt)', async () => {
      const log = vi.spyOn(console, 'log').mockImplementation(() => {})
      try {
        await kt.main(['--chi-node'], {})
        await kt.kiemTraNode('18.0.0')
        await kt.kiemTraJava({ JAVA_HOME: '/khong/co' }, () => null, { existsSync: () => false }, 'linux')
        expect(log.mock.calls.flat().join('\n')).toMatch(/^[\x20-\x7e\n]*$/)
      } finally {
        log.mockRestore()
      }
    })
  })
})
